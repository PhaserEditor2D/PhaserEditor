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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceManipulation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DeleteFileChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DeleteFolderChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DeleteFromClasspathChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DeletePackageFragmentRootChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DeleteSourceManipulationChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.UndoablePackageDeleteChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;


class DeleteChangeCreator {
	private DeleteChangeCreator() {
		//private
	}
	
	/**
	 * @param packageDeletes a list of {@link IResource}s that will be deleted
	 *        by the delete operation of the {@link IPackageFragment}s in
	 *        <code>javaElements</code>, or <code>null</code> iff
	 *        <code>javaElements</code> does not contain package fragments
	 */
	static Change createDeleteChange(TextChangeManager manager, IResource[] resources,
			IJavaScriptElement[] javaElements, String changeName, List/*<IResource>*/ packageDeletes) throws CoreException {
		/*
		 * Problem: deleting a package and subpackages can result in
		 * multiple package fragments in fJavaElements but only
		 * one folder in packageDeletes. The way to handle this is to make the undo
		 * change of individual package delete changes an empty change, and
		 * add take care of the undo in UndoablePackageDeleteChange.
		 */ 
		DynamicValidationStateChange result;
		if (packageDeletes.size() > 0) {
			result= new UndoablePackageDeleteChange(changeName, packageDeletes);
		} else {
			result= new DynamicValidationStateChange(changeName);
		}
		
		for (int i= 0; i < javaElements.length; i++) {
			IJavaScriptElement element= javaElements[i];
			if (! ReorgUtils.isInsideCompilationUnit(element))
				result.add(createDeleteChange(element));
		}

		for (int i= 0; i < resources.length; i++) {
			result.add(createDeleteChange(resources[i]));
		}
		
		Map grouped= ReorgUtils.groupByCompilationUnit(getElementsSmallerThanCu(javaElements));
		if (grouped.size() != 0 ){
			Assert.isNotNull(manager);
			for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
				IJavaScriptUnit cu= (IJavaScriptUnit) iter.next();
				result.add(createDeleteChange(cu, (List)grouped.get(cu), manager));
			}
		}

		return result;
	}
	
	private static Change createDeleteChange(IResource resource) {
		Assert.isTrue(! (resource instanceof IWorkspaceRoot));//cannot be done
		Assert.isTrue(! (resource instanceof IProject)); //project deletion is handled by the workbench
		if (resource instanceof IFile)
			return new DeleteFileChange((IFile)resource, true);
		if (resource instanceof IFolder)
			return new DeleteFolderChange((IFolder)resource, true);
		Assert.isTrue(false);//there're no more kinds
		return null;
	}

	/*
	 * List<IJavaScriptElement> javaElements
	 */
	private static Change createDeleteChange(IJavaScriptUnit cu, List javaElements, TextChangeManager manager) throws CoreException {
		JavaScriptUnit cuNode= RefactoringASTParser.parseWithASTProvider(cu, false, null);
		CompilationUnitRewrite rewriter= new CompilationUnitRewrite(cu, cuNode);
		IJavaScriptElement[] elements= (IJavaScriptElement[]) javaElements.toArray(new IJavaScriptElement[javaElements.size()]);
		ASTNodeDeleteUtil.markAsDeleted(elements, rewriter, null);
		return addTextEditFromRewrite(manager, cu, rewriter.getASTRewrite());
	}

	private static TextChange addTextEditFromRewrite(TextChangeManager manager, IJavaScriptUnit cu, ASTRewrite rewrite) throws CoreException {
		try {
			ITextFileBuffer buffer= RefactoringFileBuffers.acquire(cu);
			TextEdit resultingEdits= rewrite.rewriteAST(buffer.getDocument(), cu.getJavaScriptProject().getOptions(true));
			TextChange textChange= manager.get(cu);
			if (textChange instanceof TextFileChange) {
				TextFileChange tfc= (TextFileChange) textChange;
				if (cu.isWorkingCopy())
					tfc.setSaveMode(TextFileChange.LEAVE_DIRTY);
			}
			String message= RefactoringCoreMessages.DeleteChangeCreator_1; 
			TextChangeCompatibility.addTextEdit(textChange, message, resultingEdits);
			return textChange;
		} finally {
			RefactoringFileBuffers.release(cu);
		}
	}

	//List<IJavaScriptElement>
	private static List getElementsSmallerThanCu(IJavaScriptElement[] javaElements){
		List result= new ArrayList();
		for (int i= 0; i < javaElements.length; i++) {
			IJavaScriptElement element= javaElements[i];
			if (ReorgUtils.isInsideCompilationUnit(element))
				result.add(element);
		}
		return result;
	}

	private static Change createDeleteChange(IJavaScriptElement javaElement) {
		Assert.isTrue(! ReorgUtils.isInsideCompilationUnit(javaElement));
		
		switch(javaElement.getElementType()){
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return createPackageFragmentRootDeleteChange((IPackageFragmentRoot)javaElement);

			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return createSourceManipulationDeleteChange((IPackageFragment)javaElement);

			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return createSourceManipulationDeleteChange((IJavaScriptUnit)javaElement);

			case IJavaScriptElement.CLASS_FILE:
				//if this assert fails, it means that a precondition is missing
				Assert.isTrue(((IClassFile)javaElement).getResource() instanceof IFile);
				return createDeleteChange(((IClassFile)javaElement).getResource());

			case IJavaScriptElement.JAVASCRIPT_MODEL: //cannot be done
				Assert.isTrue(false);
				return null;

			case IJavaScriptElement.JAVASCRIPT_PROJECT: //handled differently
				Assert.isTrue(false);
				return null;

			case IJavaScriptElement.TYPE:
			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.METHOD:
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.IMPORT_CONTAINER:
			case IJavaScriptElement.IMPORT_DECLARATION:
				Assert.isTrue(false);//not done here
				return null;
			default:
				Assert.isTrue(false);//there's no more kinds
				return new NullChange();
		}
	}

	private static Change createSourceManipulationDeleteChange(ISourceManipulation element) {
		//XXX workaround for bug 31384, in case of linked ISourceManipulation delete the resource
		if (element instanceof IJavaScriptUnit || element instanceof IPackageFragment){
			IResource resource;
			if (element instanceof IJavaScriptUnit)
				resource= ReorgUtils.getResource((IJavaScriptUnit)element);
			else 
				resource= ((IPackageFragment)element).getResource();
			if (resource != null && resource.isLinked())
				return createDeleteChange(resource);
		}
		return new DeleteSourceManipulationChange(element, true);
	}
	
	private static Change createPackageFragmentRootDeleteChange(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		if (resource != null && resource.isLinked()){
			//XXX using this code is a workaround for jcore bug 31998
			//jcore cannot handle linked stuff
			//normally, we should always create DeletePackageFragmentRootChange
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.DeleteRefactoring_delete_package_fragment_root); 
	
			composite.add(new DeleteFromClasspathChange(root));
			Assert.isTrue(! Checks.isClasspathDelete(root));//checked in preconditions
			composite.add(createDeleteChange(resource));
	
			return composite;
		} else {
			Assert.isTrue(! root.isExternal());
			// TODO remove the query argument
			return new DeletePackageFragmentRootChange(root, true, null); 
		}
	}
}
