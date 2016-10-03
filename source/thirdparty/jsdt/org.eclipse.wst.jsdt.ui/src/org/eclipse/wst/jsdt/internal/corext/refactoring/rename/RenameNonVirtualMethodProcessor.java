/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.MethodDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;

public class RenameNonVirtualMethodProcessor extends RenameMethodProcessor {

	/**
	 * Creates a new rename method processor.
	 * <p>
	 * This constructor is only invoked by <code>RenameTypeProcessor</code>.
	 * </p>
	 * 
	 * @param method the method
	 * @param manager the change manager
	 * @param categorySet the group category set
	 */
	RenameNonVirtualMethodProcessor(IFunction method, TextChangeManager manager, GroupCategorySet categorySet) {
		super(method, manager, categorySet);
	}

	/**
	 * Creates a new rename method processor.
	 * @param method the method, or <code>null</code> if invoked by scripting
	 */
	public RenameNonVirtualMethodProcessor(IFunction method) {
		super(method);
	}
	
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameNonVirtualMethodAvailable(getMethod());
	}
	
	//----------- preconditions --------------
	
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext) throws CoreException {
		try{
			pm.beginTask("", 3); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.doCheckFinalConditions(new SubProgressMonitor(pm, 1), checkContext));
			if (result.hasFatalError())
				return result;
			
			final IFunction method= getMethod();
			final IType declaring= method.getDeclaringType();
			final String name= getNewElementName();
			if (declaring!=null)
			{
			  IFunction[] hierarchyMethods= hierarchyDeclaresMethodName(
				new SubProgressMonitor(pm, 1), declaring.newTypeHierarchy(new SubProgressMonitor(pm, 1)), method, name);
			
			  for (int i= 0; i < hierarchyMethods.length; i++) {
				IFunction hierarchyMethod= hierarchyMethods[i];
				RefactoringStatusContext context= JavaStatusContext.create(hierarchyMethod);
				if (Checks.compareParamTypes(method.getParameterTypes(), hierarchyMethod.getParameterTypes())) {
					String message= Messages.format(
						RefactoringCoreMessages.RenamePrivateMethodRefactoring_hierarchy_defines, 
						new String[]{JavaModelUtil.getFullyQualifiedName(
							declaring), name});
					result.addError(message, context);				
				}else {
					String message= Messages.format(
						RefactoringCoreMessages.RenamePrivateMethodRefactoring_hierarchy_defines2, 
						new String[]{JavaModelUtil.getFullyQualifiedName(
							declaring), name});
					result.addWarning(message, context);				
				}
			}
			}
			return result;
		} finally{
			pm.done();
		}
	}
	
	/*
	 * The code below is needed to due bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=39700.
	 * Declaration in hierarchy doesn't take visibility into account. 
	 */

	/*
	 * XXX working around bug 39700
	 */
	protected SearchResultGroup[] getOccurrences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", 2);	 //$NON-NLS-1$
		SearchPattern pattern= createReferenceSearchPattern();
		SearchResultGroup[] groups= RefactoringSearchEngine.search(pattern, createRefactoringScope(),
			new MethodOccurenceCollector(getMethod().getElementName()), new SubProgressMonitor(pm, 1), status);
		//Workaround bug 39700. Manually add declaration match:
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			IJavaScriptUnit cu= group.getCompilationUnit();
			if (cu.equals(getDeclaringCU())) {
				IResource resource= group.getResource();
				int start= getMethod().getNameRange().getOffset();
				int length= getMethod().getNameRange().getLength();
				MethodDeclarationMatch declarationMatch= new MethodDeclarationMatch(getMethod(), SearchMatch.A_ACCURATE, start, length, SearchEngine.getDefaultSearchParticipant(), resource);
				group.add(declarationMatch);
				break;//no need to go further
			}	
		}
		return groups;	
	}
		
	/*
	 * @see RenameMethodProcessor#addOccurrences(org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager, org.eclipse.core.runtime.IProgressMonitor, RefactoringStatus)
	 */
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		// declaration update must be registered first
		addDeclarationUpdate(manager);
		if (getUpdateReferences())
			addReferenceUpdates(manager, pm, status);
		pm.worked(1);
	}
	
	private IJavaScriptUnit getDeclaringCU() {
		return getMethod().getJavaScriptUnit();
	}

	/*
	 * @see RenameMethodProcessor#createOccurrenceSearchPattern(org.eclipse.core.runtime.IProgressMonitor)
	 */
	SearchPattern createOccurrenceSearchPattern(IProgressMonitor pm) {
		pm.beginTask("", 1); //$NON-NLS-1$
		SearchPattern pattern= SearchPattern.createPattern(getMethod(), IJavaScriptSearchConstants.ALL_OCCURRENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		pm.done();
		return pattern;
	}

	private SearchPattern createReferenceSearchPattern() {
		return SearchPattern.createPattern(getMethod(), IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
	}
	
	final void addDeclarationUpdate(TextChangeManager manager) throws CoreException {

		if (getDelegateUpdating()) {
			// create the delegate
			CompilationUnitRewrite rewrite= new CompilationUnitRewrite(getDeclaringCU());
			rewrite.setResolveBindings(true);
			FunctionDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(getMethod(), rewrite.getRoot());
			DelegateMethodCreator creator= new DelegateMethodCreator();
			creator.setDeclaration(methodDeclaration);
			creator.setDeclareDeprecated(getDeprecateDelegates());
			creator.setSourceRewrite(rewrite);
			creator.setCopy(true);
			creator.setNewElementName(getNewElementName());
			creator.prepareDelegate();
			creator.createEdit();
			CompilationUnitChange cuChange= rewrite.createChange();
			if (cuChange != null) {
				cuChange.setKeepPreviewEdits(true);
				manager.manage(getDeclaringCU(), cuChange);
			}
		}

		String editName= RefactoringCoreMessages.RenameMethodRefactoring_update_declaration;
		ISourceRange nameRange= getMethod().getNameRange();
		ReplaceEdit replaceEdit= new ReplaceEdit(nameRange.getOffset(), nameRange.getLength(), getNewElementName());
		addTextEdit(manager.get(getDeclaringCU()), editName, replaceEdit);
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		SearchResultGroup[] grouped= getReferences(pm, status);
		for (int i= 0; i < grouped.length; i++) {
			SearchResultGroup group= grouped[i];
			SearchMatch[] results= group.getSearchResults();
			IJavaScriptUnit cu= group.getCompilationUnit();
			TextChange change= manager.get(cu);
			for (int j= 0; j < results.length; j++){
				String editName= RefactoringCoreMessages.RenamePrivateMethodRefactoring_update; 
				ReplaceEdit replaceEdit= createReplaceEdit(results[j], cu);
				addTextEdit(change, editName, replaceEdit);
				
			}
		}	
	}

	private SearchResultGroup[] getReferences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		//TODO: should not do the search again!
		pm.beginTask("", 2);	 //$NON-NLS-1$
		SearchPattern pattern= createReferenceSearchPattern();
		return RefactoringSearchEngine.search(pattern, createRefactoringScope(),
			new MethodOccurenceCollector(getMethod().getElementName()), new SubProgressMonitor(pm, 1), status);	
	}

	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_renamed_plural;
		else
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_renamed_singular;
	}
}
