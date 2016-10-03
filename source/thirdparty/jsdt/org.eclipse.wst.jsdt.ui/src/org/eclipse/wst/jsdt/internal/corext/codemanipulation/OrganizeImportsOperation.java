/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.corext.util.TypeNameMatchCollector;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.wst.jsdt.internal.ui.text.correction.SimilarElementsRequestor;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class OrganizeImportsOperation implements IWorkspaceRunnable {
	public static interface IChooseImportQuery {
		/**
		 * Selects imports from a list of choices.
		 * @param openChoices From each array, a type reference has to be selected
		 * @param ranges For each choice the range of the corresponding  type reference.
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges);
	}
	
	
	private static class TypeReferenceProcessor {
		
		private static class UnresolvedTypeData {
			final SimpleName ref;
			final int typeKinds;
			final List foundInfos;

			public UnresolvedTypeData(SimpleName ref) {
				this.ref= ref;
				this.typeKinds= ASTResolving.getPossibleTypeKinds(ref, true);
				this.foundInfos= new ArrayList(3);
			}
			
			public void addInfo(TypeNameMatch info) {
				for (int i= this.foundInfos.size() - 1; i >= 0; i--) {
					TypeNameMatch curr= (TypeNameMatch) this.foundInfos.get(i);
					if (curr.getTypeContainerName().equals(info.getTypeContainerName())) {
						return; // not added. already contains type with same name
					}
				}
				foundInfos.add(info);
			}
		}
		
		private Set fOldSingleImports;
		private Set fOldDemandImports;
		
		private Set fImplicitImports;
		
		private ImportRewrite fImpStructure;
		
		private boolean fDoIgnoreLowerCaseNames;
		
		private IPackageFragment fCurrPackage;
		
		private ScopeAnalyzer fAnalyzer;
		private boolean fAllowDefaultPackageImports;
		
		private Map fUnresolvedTypes;
		private Set fImportsAdded;
		private TypeNameMatch[][] fOpenChoices;
		private SourceRange[] fSourceRanges;
		
		
		public TypeReferenceProcessor(Set oldSingleImports, Set oldDemandImports, JavaScriptUnit root, ImportRewrite impStructure, boolean ignoreLowerCaseNames) {
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fImpStructure= impStructure;
			fDoIgnoreLowerCaseNames= ignoreLowerCaseNames;
			
			IJavaScriptUnit cu= impStructure.getCompilationUnit();
			
			fImplicitImports= new HashSet(3);
			fImplicitImports.add(""); //$NON-NLS-1$
			fImplicitImports.add("java.lang"); //$NON-NLS-1$
			fImplicitImports.add(cu.getParent().getElementName());	
			
			fAnalyzer= new ScopeAnalyzer(root);
			
			fCurrPackage= (IPackageFragment) cu.getParent();
			
			fAllowDefaultPackageImports= cu.getJavaScriptProject().getOption(JavaScriptCore.COMPILER_COMPLIANCE, true).equals(JavaScriptCore.VERSION_1_3);
			
			fImportsAdded= new HashSet();
			fUnresolvedTypes= new HashMap();
		}
		
		private boolean needsImport(ITypeBinding typeBinding, SimpleName ref) {
			if (!typeBinding.isTopLevel() && !typeBinding.isMember() || typeBinding.isRecovered()) {
				return false; // no imports for anonymous, local, primitive types or parameters types
			}
			int modifiers= typeBinding.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				return false; // imports for privates are not required
			}
			ITypeBinding currTypeBinding= Bindings.getBindingOfParentType(ref);
			if (currTypeBinding == null) {
				return false; // not in a type
			}
			if (!Modifier.isPublic(modifiers)) {
				if (!currTypeBinding.getPackage().getName().equals(typeBinding.getPackage().getName())) {
					return false; // not visible
				}
			}
			
			ASTNode parent= ref.getParent();
			while (parent instanceof Type) {
				parent= parent.getParent();
			}
			if (parent instanceof AbstractTypeDeclaration && parent.getParent() instanceof JavaScriptUnit) {
				return true;
			}
			
			if (typeBinding.isMember()) {
				if (fAnalyzer.isDeclaredInScope(typeBinding, ref, ScopeAnalyzer.TYPES | ScopeAnalyzer.CHECK_VISIBILITY))
					return false;
			}
			return true;				
		}
		
		
		/**
		 * Tries to find the given type name and add it to the import structure.
		 * @param ref the name node
		 */
		public void add(SimpleName ref) {
			String typeName= ref.getIdentifier();
			
			if (fImportsAdded.contains(typeName)) {
				return;
			}
			
			IBinding binding= ref.resolveBinding();
			if (binding != null) {
				if (binding.getKind() != IBinding.TYPE) {
					return;
				}
				ITypeBinding typeBinding= (ITypeBinding) binding;
				if (typeBinding.isArray()) {
					typeBinding= typeBinding.getElementType();
				}
				typeBinding= typeBinding.getTypeDeclaration();
				if (!typeBinding.isRecovered()) {
					if (needsImport(typeBinding, ref)) {
						fImpStructure.addImport(typeBinding);
						fImportsAdded.add(typeName);
					}
					return;
				}
			} else {
				if (fDoIgnoreLowerCaseNames && typeName.length() > 0) {
					char ch= typeName.charAt(0);
					if (Strings.isLowerCase(ch) && Character.isLetter(ch)) {
						return;
					}
				}
			}
			fImportsAdded.add(typeName);			
			fUnresolvedTypes.put(typeName, new UnresolvedTypeData(ref));
		}
			
		public boolean process(IProgressMonitor monitor) throws JavaScriptModelException {
			try {
				int nUnresolved= fUnresolvedTypes.size();
				if (nUnresolved == 0) {
					return false;
				}
				char[][] allTypes= new char[nUnresolved][];
				int i= 0;
				for (Iterator iter= fUnresolvedTypes.keySet().iterator(); iter.hasNext();) {
					allTypes[i++]= ((String) iter.next()).toCharArray();
				}
				final ArrayList typesFound= new ArrayList();
				final IJavaScriptProject project= fCurrPackage.getJavaScriptProject();
				IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { project });
				TypeNameMatchCollector collector= new TypeNameMatchCollector(typesFound);
				new SearchEngine().searchAllTypeNames(null, allTypes, scope, collector, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);

				boolean is50OrHigher= 	JavaModelUtil.is50OrHigher(project);
				
				for (i= 0; i < typesFound.size(); i++) {
					TypeNameMatch curr= (TypeNameMatch) typesFound.get(i);
					UnresolvedTypeData data= (UnresolvedTypeData) fUnresolvedTypes.get(curr.getSimpleTypeName());
					if (data != null && isVisible(curr) && isOfKind(curr, data.typeKinds, is50OrHigher)) {
						if (fAllowDefaultPackageImports || curr.getPackageName().length() > 0) {
							data.addInfo(curr);
						}
					}
				}
				
				ArrayList openChoices= new ArrayList(nUnresolved);
				ArrayList sourceRanges= new ArrayList(nUnresolved);
				for (Iterator iter= fUnresolvedTypes.values().iterator(); iter.hasNext();) {
					UnresolvedTypeData data= (UnresolvedTypeData) iter.next();
					TypeNameMatch[] openChoice= processTypeInfo(data.foundInfos);
					if (openChoice != null) {
						openChoices.add(openChoice);
						sourceRanges.add(new SourceRange(data.ref.getStartPosition(), data.ref.getLength()));
					}
				}
				if (openChoices.isEmpty()) {
					return false;
				}
				fOpenChoices= (TypeNameMatch[][]) openChoices.toArray(new TypeNameMatch[openChoices.size()][]);
				fSourceRanges= (SourceRange[]) sourceRanges.toArray(new SourceRange[sourceRanges.size()]);
				return true;
			} finally {
				monitor.done();
			}
		}
		
		private TypeNameMatch[] processTypeInfo(List typeRefsFound) {
			int nFound= typeRefsFound.size();
			if (nFound == 0) {
				// nothing found
				return null;
			} else if (nFound == 1) {
				TypeNameMatch typeRef= (TypeNameMatch) typeRefsFound.get(0);
				fImpStructure.addImport(typeRef.getFullyQualifiedName());
				return null;
			} else {
				String typeToImport= null;
				boolean ambiguousImports= false;
				
				// multiple found, use old imports to find an entry
				for (int i= 0; i < nFound; i++) {
					TypeNameMatch typeRef= (TypeNameMatch) typeRefsFound.get(i);
					String fullName= typeRef.getFullyQualifiedName();
					String containerName= typeRef.getTypeContainerName();
					if (fOldSingleImports.contains(fullName)) {
						// was single-imported
						fImpStructure.addImport(fullName);
						return null;
					} else if (fOldDemandImports.contains(containerName) || fImplicitImports.contains(containerName)) {
						if (typeToImport == null) {
							typeToImport= fullName;
						} else {  // more than one import-on-demand
							ambiguousImports= true;
						}
					}
				}
				
				if (typeToImport != null && !ambiguousImports) {
					fImpStructure.addImport(typeToImport);
					return null;
				}
				// return the open choices
				return (TypeNameMatch[]) typeRefsFound.toArray(new TypeNameMatch[nFound]);
			}
		}
		
		private boolean isOfKind(TypeNameMatch curr, int typeKinds, boolean is50OrHigher) {
			return (typeKinds & SimilarElementsRequestor.CLASSES) != 0;
		}

		private boolean isVisible(TypeNameMatch curr) {
			int flags= curr.getModifiers();
			if (Flags.isPrivate(flags)) {
				return false;
			}
			if (Flags.isPublic(flags)) {
				return true;
			}
			return curr.getPackageName().equals(fCurrPackage.getElementName());
		}

		public TypeNameMatch[][] getChoices() {
			return fOpenChoices;
		}

		public ISourceRange[] getChoicesSourceRanges() {
			return fSourceRanges;
		}
	}	

	private boolean fDoSave;
	
	private boolean fIgnoreLowerCaseNames;
	
	private IChooseImportQuery fChooseImportQuery;
	
	private int fNumberOfImportsAdded;
	private int fNumberOfImportsRemoved;

	private IProblem fParsingError;
	private IJavaScriptUnit fCompilationUnit;
	
	private JavaScriptUnit fASTRoot;

	private final boolean fAllowSyntaxErrors;
	
	public OrganizeImportsOperation(IJavaScriptUnit cu, JavaScriptUnit astRoot, boolean ignoreLowerCaseNames, boolean save, boolean allowSyntaxErrors, IChooseImportQuery chooseImportQuery) throws CoreException {
		fCompilationUnit= cu;
		fASTRoot= astRoot;

		fDoSave= save;
		fIgnoreLowerCaseNames= ignoreLowerCaseNames;
		fAllowSyntaxErrors= allowSyntaxErrors;
		fChooseImportQuery= chooseImportQuery;

		fNumberOfImportsAdded= 0;
		fNumberOfImportsRemoved= 0;
		
		fParsingError= null;
	}

	/**
	 * Runs the operation.
	 * @param monitor the progress monitor
	 * @throws CoreException thrown when the operation failed
	 * @throws OperationCanceledException Runtime error thrown when operation is canceled.
	 */	
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(Messages.format(CodeGenerationMessages.OrganizeImportsOperation_description, fCompilationUnit.getElementName()), 10);
			
    		TextEdit edit= createTextEdit(new SubProgressMonitor(monitor, 9));
    		if (edit == null)
    			return;
    		
    		JavaModelUtil.applyEdit(fCompilationUnit, edit, fDoSave, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}
	
	public TextEdit createTextEdit(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			fNumberOfImportsAdded= 0;
			fNumberOfImportsRemoved= 0;
			
			monitor.beginTask(Messages.format(CodeGenerationMessages.OrganizeImportsOperation_description, fCompilationUnit.getElementName()), 9);
			
			JavaScriptUnit astRoot= fASTRoot;
			if (astRoot == null) {
				astRoot= ASTProvider.getASTProvider().getAST(fCompilationUnit, ASTProvider.WAIT_YES, new SubProgressMonitor(monitor, 2));
				if (monitor.isCanceled())
					throw new OperationCanceledException();
			} else {
				monitor.worked(2);
			}
			
			ImportRewrite importsRewrite= StubUtility.createImportRewrite(astRoot, false);

			Set/*<String>*/ oldSingleImports= new HashSet();
			Set/*<String>*/  oldDemandImports= new HashSet();
			List/*<SimpleName>*/ typeReferences= new ArrayList();
			List/*<SimpleName>*/ staticReferences= new ArrayList();
			
			if (!collectReferences(astRoot, typeReferences, staticReferences, oldSingleImports, oldDemandImports))
				return null;
						
			monitor.worked(1);
		
			TypeReferenceProcessor processor= new TypeReferenceProcessor(oldSingleImports, oldDemandImports, astRoot, importsRewrite, fIgnoreLowerCaseNames);
			
			Iterator refIterator= typeReferences.iterator();
			while (refIterator.hasNext()) {
				SimpleName typeRef= (SimpleName) refIterator.next();
				processor.add(typeRef);
			}
			
			boolean hasOpenChoices= processor.process(new SubProgressMonitor(monitor, 3));
			addStaticImports(staticReferences, importsRewrite);
			
			if (hasOpenChoices && fChooseImportQuery != null) {
				TypeNameMatch[][] choices= processor.getChoices();
				ISourceRange[] ranges= processor.getChoicesSourceRanges();
				TypeNameMatch[] chosen= fChooseImportQuery.chooseImports(choices, ranges);
				if (chosen == null) {
					// cancel pressed by the user
					throw new OperationCanceledException();
				}
				for (int i= 0; i < chosen.length; i++) {
					TypeNameMatch typeInfo= chosen[i];
					importsRewrite.addImport(typeInfo.getFullyQualifiedName());
				}				
			}

			TextEdit result= importsRewrite.rewriteImports(new SubProgressMonitor(monitor, 3));
			
			determineImportDifferences(importsRewrite, oldSingleImports, oldDemandImports);
			
			return result;
		} finally {
			monitor.done();
		}
	}
	
	private void determineImportDifferences(ImportRewrite importsStructure, Set oldSingleImports, Set oldDemandImports) {
  		ArrayList importsAdded= new ArrayList();
  		importsAdded.addAll(Arrays.asList(importsStructure.getCreatedImports()));
  		importsAdded.addAll(Arrays.asList(importsStructure.getCreatedStaticImports()));
		
		Object[] content= oldSingleImports.toArray();
	    for (int i= 0; i < content.length; i++) {
	        String importName= (String) content[i];
	        if (importsAdded.remove(importName))
	            oldSingleImports.remove(importName);
	    }
	    content= oldDemandImports.toArray();
	    for (int i= 0; i < content.length; i++) {
	        String importName= (String) content[i]; 
	        if (importsAdded.remove(importName + ".*")) //$NON-NLS-1$
	            oldDemandImports.remove(importName);
	    }
	    fNumberOfImportsAdded= importsAdded.size();
	    fNumberOfImportsRemoved= oldSingleImports.size() + oldDemandImports.size();	    
	}
	
	
	private void addStaticImports(List/*<SimpleName>*/ staticReferences, ImportRewrite importsStructure) {
		for (int i= 0; i < staticReferences.size(); i++) {
			Name name= (Name) staticReferences.get(i);
			IBinding binding= name.resolveBinding();
			if (binding != null) { // paranoia check
				importsStructure.addStaticImport(binding);
			}
		}
	}

	
	// find type references in a compilation unit
	private boolean collectReferences(JavaScriptUnit astRoot, List typeReferences, List staticReferences, Set oldSingleImports, Set oldDemandImports) {
		if (!fAllowSyntaxErrors) {
			IProblem[] problems= astRoot.getProblems();
			for (int i= 0; i < problems.length; i++) {
				IProblem curr= problems[i];
				if (curr.isError() && (curr.getID() & IProblem.Syntax) != 0) {
					fParsingError= problems[i];
					return false;
				}
			}
		}
		List imports= astRoot.imports();
		for (int i= 0; i < imports.size(); i++) {
			ImportDeclaration curr= (ImportDeclaration) imports.get(i);
			String id= ASTResolving.getFullName(curr.getName());
			if (curr.isOnDemand()) {
				oldDemandImports.add(id);
			} else {
				oldSingleImports.add(id);
			}
		}
		
		IJavaScriptProject project= fCompilationUnit.getJavaScriptProject();
		ImportReferencesCollector.collect(astRoot, project, null, typeReferences, staticReferences);

		return true;
	}	
	
	/**
	 * After executing the operation, returns <code>null</code> if the operation has been executed successfully or
	 * the range where parsing failed. 
	 * @return returns the parse error
	 */
	public IProblem getParseError() {
		return fParsingError;
	}
	
	public int getNumberOfImportsAdded() {
		return fNumberOfImportsAdded;
	}
	
	public int getNumberOfImportsRemoved() {
		return fNumberOfImportsRemoved;
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
}
