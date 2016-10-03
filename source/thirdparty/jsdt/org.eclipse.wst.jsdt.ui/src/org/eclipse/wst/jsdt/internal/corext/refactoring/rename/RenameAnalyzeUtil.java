/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.search.FieldDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.MethodDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;

class RenameAnalyzeUtil {
	
	private static class ProblemNodeFinder {
		
		private ProblemNodeFinder() {
			//static
		}
		
		public static SimpleName[] getProblemNodes(ASTNode methodNode, VariableDeclaration variableNode, TextEdit[] edits, TextChange change) {
			String key= variableNode.resolveBinding().getKey();
			NameNodeVisitor visitor= new NameNodeVisitor(edits, change, key);
			methodNode.accept(visitor);
			return visitor.getProblemNodes();
		}
		
		private static class NameNodeVisitor extends ASTVisitor {
	
			private Collection fRanges;
			private Collection fProblemNodes;
			private String fKey;
	
			public NameNodeVisitor(TextEdit[] edits, TextChange change, String key) {
				Assert.isNotNull(edits);
				Assert.isNotNull(key);
				
				fRanges= new HashSet(Arrays.asList(RefactoringAnalyzeUtil.getNewRanges(edits, change)));
				fProblemNodes= new ArrayList(0);
				fKey= key;
			}
	
			public SimpleName[] getProblemNodes() {
				return (SimpleName[]) fProblemNodes.toArray(new SimpleName[fProblemNodes.size()]);
			}
	
			//----- visit methods 
	
			public boolean visit(SimpleName node) {
				VariableDeclaration decl= getVariableDeclaration(node);
				if (decl == null)
					return super.visit(node);
				
				IVariableBinding binding= decl.resolveBinding();
				if (binding == null)
					return super.visit(node);
				
				boolean keysEqual= fKey.equals(binding.getKey()); 
				boolean rangeInSet= fRanges.contains(new Region(node.getStartPosition(), node.getLength()));
	
				if (keysEqual && !rangeInSet)
					fProblemNodes.add(node);
	
				if (!keysEqual && rangeInSet)
					fProblemNodes.add(node);
				
				/*
				 * if (!keyEquals && !rangeInSet) 
				 * 		ok, different local variable.
				 * 
				 * if (keyEquals && rangeInSet) 
				 * 		ok, renamed local variable & has been renamed.
				 */
	
				return super.visit(node);
			}
		}
	}

	static class LocalAnalyzePackage {
		public final TextEdit fDeclarationEdit;
		public final TextEdit[] fOccurenceEdits;
		
		public LocalAnalyzePackage(final TextEdit declarationEdit, final TextEdit[] occurenceEdits) {
			fDeclarationEdit = declarationEdit;
			fOccurenceEdits = occurenceEdits;
		}
	}

	private RenameAnalyzeUtil() {
		//no instance
	}
	
	static RefactoringStatus analyzeRenameChanges(TextChangeManager manager,  SearchResultGroup[] oldOccurrences, SearchResultGroup[] newOccurrences) {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < oldOccurrences.length; i++) {
			SearchResultGroup oldGroup= oldOccurrences[i];
			SearchMatch[] oldSearchResults= oldGroup.getSearchResults();
			IJavaScriptUnit cunit= oldGroup.getCompilationUnit();
			if (cunit == null)
				continue;
			for (int j= 0; j < oldSearchResults.length; j++) {
				SearchMatch oldSearchResult= oldSearchResults[j];
				if (! RenameAnalyzeUtil.existsInNewOccurrences(oldSearchResult, newOccurrences, manager)){
					addShadowsError(cunit, oldSearchResult, result);
				}	
			}
		}
		return result;
	}

	static IJavaScriptUnit findWorkingCopyForCu(IJavaScriptUnit[] newWorkingCopies, IJavaScriptUnit cu){
		IJavaScriptUnit original= cu == null ? null : cu.getPrimary();
		for (int i= 0; i < newWorkingCopies.length; i++) {
			if (newWorkingCopies[i].getPrimary().equals(original))
				return newWorkingCopies[i];
		}
		return null;
	}

	static IJavaScriptUnit[] createNewWorkingCopies(IJavaScriptUnit[] compilationUnitsToModify, TextChangeManager manager, WorkingCopyOwner owner, SubProgressMonitor pm) throws CoreException {
		pm.beginTask("", compilationUnitsToModify.length); //$NON-NLS-1$
		IJavaScriptUnit[] newWorkingCopies= new IJavaScriptUnit[compilationUnitsToModify.length];
		for (int i= 0; i < compilationUnitsToModify.length; i++) {
			IJavaScriptUnit cu= compilationUnitsToModify[i];
			newWorkingCopies[i]= createNewWorkingCopy(cu, manager, owner, new SubProgressMonitor(pm, 1));
		}
		pm.done();
		return newWorkingCopies;
	}
	
	static IJavaScriptUnit createNewWorkingCopy(IJavaScriptUnit cu, TextChangeManager manager,
			WorkingCopyOwner owner, SubProgressMonitor pm) throws CoreException {
		IJavaScriptUnit newWc= cu.getWorkingCopy(owner, null);
		String previewContent= manager.get(cu).getPreviewContent(new NullProgressMonitor());
		newWc.getBuffer().setContents(previewContent);
		newWc.reconcile(IJavaScriptUnit.NO_AST, false, owner, pm);
		return newWc;
	}
	
	private static boolean existsInNewOccurrences(SearchMatch searchResult, SearchResultGroup[] newOccurrences, TextChangeManager manager) {
		SearchResultGroup newGroup= findOccurrenceGroup(searchResult.getResource(), newOccurrences);
		if (newGroup == null)
			return false;
		
		IRegion oldEditRange= getCorrespondingEditChangeRange(searchResult, manager);
		if (oldEditRange == null)
			return false;
		
		SearchMatch[] newSearchResults= newGroup.getSearchResults();
		int oldRangeOffset = oldEditRange.getOffset();
		for (int i= 0; i < newSearchResults.length; i++) {
			if (newSearchResults[i].getOffset() == oldRangeOffset)
				return true;
		}
		return false;
	}
	
	private static IRegion getCorrespondingEditChangeRange(SearchMatch searchResult, TextChangeManager manager) {
		TextChange change= getTextChange(searchResult, manager);
		if (change == null)
			return null;
		
		IRegion oldMatchRange= createTextRange(searchResult);
		TextEditChangeGroup[] editChanges= change.getTextEditChangeGroups();	
		for (int i= 0; i < editChanges.length; i++) {
			if (oldMatchRange.equals(editChanges[i].getRegion()))
				return TextEdit.getCoverage(change.getPreviewEdits(editChanges[i].getTextEdits()));
		}
		return null;
	}
	
	private static TextChange getTextChange(SearchMatch searchResult, TextChangeManager manager) {
		IJavaScriptUnit cu= SearchUtils.getCompilationUnit(searchResult);
		if (cu == null)
			return null;
		return manager.get(cu);
	}
	
	private static IRegion createTextRange(SearchMatch searchResult) {
		return new Region(searchResult.getOffset(), searchResult.getLength());
	}
	
	private static SearchResultGroup findOccurrenceGroup(IResource resource, SearchResultGroup[] newOccurrences) {
		for (int i= 0; i < newOccurrences.length; i++) {
			if (newOccurrences[i].getResource().equals(resource))
				return newOccurrences[i];
		}
		return null;
	}
	
//--- find missing changes in BOTH directions
	
	//TODO: Currently filters out declarations (MethodDeclarationMatch, FieldDeclarationMatch).
	//Long term solution: only pass reference search results in.
	static RefactoringStatus analyzeRenameChanges2(TextChangeManager manager,
			SearchResultGroup[] oldReferences, SearchResultGroup[] newReferences, String newElementName) {
		RefactoringStatus result= new RefactoringStatus();
		
		HashMap cuToNewResults= new HashMap(newReferences.length);
		for (int i1= 0; i1 < newReferences.length; i1++) {
			IJavaScriptUnit cu= newReferences[i1].getCompilationUnit();
			if (cu != null)
				cuToNewResults.put(cu.getPrimary(), newReferences[i1].getSearchResults());
		}
		
		for (int i= 0; i < oldReferences.length; i++) {
			SearchResultGroup oldGroup= oldReferences[i];
			SearchMatch[] oldMatches= oldGroup.getSearchResults();
			IJavaScriptUnit cu= oldGroup.getCompilationUnit();
			if (cu == null)
				continue;
			
			SearchMatch[] newSearchMatches= (SearchMatch[]) cuToNewResults.remove(cu);
			if (newSearchMatches == null) {
				for (int j = 0; j < oldMatches.length; j++) {
					SearchMatch oldMatch = oldMatches[j];
					addShadowsError(cu, oldMatch, result);
				}
			} else {
				analyzeChanges(cu, manager.get(cu), oldMatches, newSearchMatches, newElementName, result);
			}
		}
		
		for (Iterator iter= cuToNewResults.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry= (Entry) iter.next();
			IJavaScriptUnit cu= (IJavaScriptUnit) entry.getKey();
			SearchMatch[] newSearchMatches= (SearchMatch[]) entry.getValue();
			for (int i= 0; i < newSearchMatches.length; i++) {
				SearchMatch newMatch= newSearchMatches[i];
				addReferenceShadowedError(cu, newMatch, newElementName, result);
			}
		}
		return result;
	}

	private static void analyzeChanges(IJavaScriptUnit cu, TextChange change,
			SearchMatch[] oldMatches, SearchMatch[] newMatches, String newElementName, RefactoringStatus result) {
		Map updatedOldOffsets= getUpdatedChangeOffsets(change, oldMatches);
		for (int i= 0; i < newMatches.length; i++) {
			SearchMatch newMatch= newMatches[i];
			Integer offsetInNew= Integer.valueOf(newMatch.getOffset());
			SearchMatch oldMatch= (SearchMatch) updatedOldOffsets.remove(offsetInNew);
			if (oldMatch == null) {
				addReferenceShadowedError(cu, newMatch, newElementName, result);
			}
		}
		for (Iterator iter= updatedOldOffsets.values().iterator(); iter.hasNext();) {
			// remaining old matches are not found any more -> they have been shadowed
			SearchMatch oldMatch= (SearchMatch) iter.next();
			addShadowsError(cu, oldMatch, result);
		}
	}
	
	/** @return Map &lt;Integer updatedOffset, SearchMatch oldMatch&gt; */
	private static Map getUpdatedChangeOffsets(TextChange change, SearchMatch[] oldMatches) {
		Map/*<Integer updatedOffset, SearchMatch oldMatch>*/ updatedOffsets= new HashMap();
		Map oldToUpdatedOffsets= getEditChangeOffsetUpdates(change);
		for (int i= 0; i < oldMatches.length; i++) {
			SearchMatch oldMatch= oldMatches[i];
			Integer updatedOffset= (Integer) oldToUpdatedOffsets.get(Integer.valueOf(oldMatch.getOffset()));
			if (updatedOffset == null)
				updatedOffset= Integer.valueOf(-1); //match not updated
			updatedOffsets.put(updatedOffset, oldMatch);
		}
		return updatedOffsets;
	}

	/** @return Map &lt;Integer oldOffset, Integer updatedOffset&gt; */
	private static Map getEditChangeOffsetUpdates(TextChange change) {
		TextEditChangeGroup[] editChanges= change.getTextEditChangeGroups();
		Map/*<oldOffset, newOffset>*/ offsetUpdates= new HashMap(editChanges.length);
		for (int i= 0; i < editChanges.length; i++) {
			TextEditChangeGroup editChange= editChanges[i];
			IRegion oldRegion= editChange.getRegion();
			if (oldRegion == null)
				continue;
			IRegion updatedRegion= TextEdit.getCoverage(change.getPreviewEdits(editChange.getTextEdits()));
			if (updatedRegion == null)
				continue;
			
			offsetUpdates.put(Integer.valueOf(oldRegion.getOffset()), Integer.valueOf(updatedRegion.getOffset()));
		}
		return offsetUpdates;
	}

	private static void addReferenceShadowedError(IJavaScriptUnit cu, SearchMatch newMatch, String newElementName, RefactoringStatus result) {
		//Found a new match with no corresponding old match.
		//-> The new match is a reference which was pointing to another element,
		//but that other element has been shadowed
		
		//TODO: should not have to filter declarations:
		if (newMatch instanceof MethodDeclarationMatch || newMatch instanceof FieldDeclarationMatch)
			return;
		ISourceRange range= getOldSourceRange(newMatch);
		RefactoringStatusContext context= JavaStatusContext.create(cu, range);
		String message= Messages.format(
				RefactoringCoreMessages.RenameAnalyzeUtil_reference_shadowed, 
				new String[] {cu.getElementName(), newElementName});
		result.addError(message, context);
	}

	private static ISourceRange getOldSourceRange(SearchMatch newMatch) {
		// cannot transfom offset in preview to offset in original -> just show enclosing method
		IJavaScriptElement newMatchElement= (IJavaScriptElement) newMatch.getElement();
		IJavaScriptElement primaryElement= newMatchElement.getPrimaryElement();
		ISourceRange range= null;
		if (primaryElement.exists() && primaryElement instanceof ISourceReference) {
			try {
				range= ((ISourceReference) primaryElement).getSourceRange();
			} catch (JavaScriptModelException e) {
				// can live without source range
			}
		}
		return range;
	}

	private static void addShadowsError(IJavaScriptUnit cu, SearchMatch oldMatch, RefactoringStatus result) {
		// Old match not found in new matches -> reference has been shadowed
		
		//TODO: should not have to filter declarations:
		if (oldMatch instanceof MethodDeclarationMatch || oldMatch instanceof FieldDeclarationMatch)
			return;
		ISourceRange range= new SourceRange(oldMatch.getOffset(), oldMatch.getLength());
		RefactoringStatusContext context= JavaStatusContext.create(cu, range);
		String message= Messages.format(RefactoringCoreMessages.RenameAnalyzeUtil_shadows, cu.getElementName()); 
		result.addError(message, context);
	}

	/**
	 * This method analyzes a set of local variable renames inside one cu. It checks whether
	 * any new compile errors have been introduced by the rename(s) and whether the correct
	 * node(s) has/have been renamed.
	 * 
	 * @param analyzePackages the LocalAnalyzePackages containing the information about the local renames
	 * @param cuChange the TextChange containing all local variable changes to be applied.
	 * @param oldCUNode the fully (incl. bindings) resolved AST node of the original compilation unit
	 * @param statementsRecovery whether statements recovery should be performed when parsing the changed CU
	 * @return a RefactoringStatus containing errors if compile errors or wrongly renamed nodes are found
	 * @throws CoreException thrown if there was an error greating the preview content of the change
	 */
	public static RefactoringStatus analyzeLocalRenames(LocalAnalyzePackage[] analyzePackages, TextChange cuChange, JavaScriptUnit oldCUNode, boolean statementsRecovery) throws CoreException {

		RefactoringStatus result= new RefactoringStatus();
		IJavaScriptUnit compilationUnit= (IJavaScriptUnit) oldCUNode.getJavaElement();

		String newCuSource= cuChange.getPreviewContent(new NullProgressMonitor());
		JavaScriptUnit newCUNode= new RefactoringASTParser(AST.JLS3).parse(newCuSource, compilationUnit, true, statementsRecovery, null);

		result.merge(analyzeCompileErrors(newCuSource, newCUNode, oldCUNode));
		if (result.hasError())
			return result;

		for (int i= 0; i < analyzePackages.length; i++) {
			ASTNode enclosing= getEnclosingBlockOrMethod(analyzePackages[i].fDeclarationEdit, cuChange, newCUNode);
			if(enclosing == null)
				enclosing = newCUNode;

			// get new declaration
			IRegion newRegion= RefactoringAnalyzeUtil.getNewTextRange(analyzePackages[i].fDeclarationEdit, cuChange);
			ASTNode newDeclaration= NodeFinder.perform(newCUNode, newRegion.getOffset(), newRegion.getLength());
			Assert.isTrue(newDeclaration instanceof Name);

			VariableDeclaration declaration= getVariableDeclaration((Name) newDeclaration);
			Assert.isNotNull(declaration);

			SimpleName[] problemNodes= ProblemNodeFinder.getProblemNodes(enclosing, declaration, analyzePackages[i].fOccurenceEdits, cuChange);
			result.merge(RefactoringAnalyzeUtil.reportProblemNodes(newCuSource, problemNodes));
		}
		return result;
	}

	private static VariableDeclaration getVariableDeclaration(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding == null && node.getParent() instanceof VariableDeclaration)
			return (VariableDeclaration) node.getParent();

		if (binding != null && binding.getKind() == IBinding.VARIABLE) {
			JavaScriptUnit cu= (JavaScriptUnit) ASTNodes.getParent(node, JavaScriptUnit.class);
			return ASTNodes.findVariableDeclaration( ((IVariableBinding) binding), cu);
		}
		return null;
	}

	private static ASTNode getEnclosingBlockOrMethod(TextEdit declarationEdit, TextChange change, JavaScriptUnit newCUNode) {
		ASTNode enclosing= RefactoringAnalyzeUtil.getBlock(declarationEdit, change, newCUNode);
		if (enclosing == null)
			enclosing= RefactoringAnalyzeUtil.getMethodDeclaration(declarationEdit, change, newCUNode);
		return enclosing;
	}

	private static RefactoringStatus analyzeCompileErrors(String newCuSource, JavaScriptUnit newCUNode, JavaScriptUnit oldCUNode) {
		RefactoringStatus result= new RefactoringStatus();
		IProblem[] newProblems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, oldCUNode);
		for (int i= 0; i < newProblems.length; i++) {
			IProblem problem= newProblems[i];
			if (problem.isError())
				result.addEntry(new RefactoringStatusEntry((problem.isError() ? RefactoringStatus.ERROR : RefactoringStatus.WARNING), problem.getMessage(), new JavaStringStatusContext(newCuSource, new SourceRange(problem))));
		}
		return result;
	}
}
