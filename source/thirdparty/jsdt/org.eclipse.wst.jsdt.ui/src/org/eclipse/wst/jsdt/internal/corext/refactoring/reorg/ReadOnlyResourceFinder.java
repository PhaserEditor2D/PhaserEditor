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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;


class ReadOnlyResourceFinder{
	private ReadOnlyResourceFinder(){
	}

	static boolean confirmDeleteOfReadOnlyElements(IJavaScriptElement[] javaElements, IResource[] resources, IReorgQueries queries) throws CoreException {
		String queryTitle= RefactoringCoreMessages.ReadOnlyResourceFinder_0; 
		String question= RefactoringCoreMessages.ReadOnlyResourceFinder_1; 
		return ReadOnlyResourceFinder.confirmOperationOnReadOnlyElements(queryTitle, question, javaElements, resources, queries);
	}

	static boolean confirmMoveOfReadOnlyElements(IJavaScriptElement[] javaElements, IResource[] resources, IReorgQueries queries) throws CoreException {
		String queryTitle= RefactoringCoreMessages.ReadOnlyResourceFinder_2; 
		String question= RefactoringCoreMessages.ReadOnlyResourceFinder_3; 
		return ReadOnlyResourceFinder.confirmOperationOnReadOnlyElements(queryTitle, question, javaElements, resources, queries);
	}

	private static boolean confirmOperationOnReadOnlyElements(String queryTitle, String question, IJavaScriptElement[] javaElements, IResource[] resources, IReorgQueries queries) throws CoreException {
		boolean hasReadOnlyResources= ReadOnlyResourceFinder.hasReadOnlyResourcesAndSubResources(javaElements, resources);
		if (hasReadOnlyResources) {
			IConfirmQuery query= queries.createYesNoQuery(queryTitle, false, IReorgQueries.CONFIRM_READ_ONLY_ELEMENTS);
			return query.confirm(question);
		}
		return true;
	}

	private static boolean hasReadOnlyResourcesAndSubResources(IJavaScriptElement[] javaElements, IResource[] resources) throws CoreException {
		return (hasReadOnlyResourcesAndSubResources(resources)||
				  hasReadOnlyResourcesAndSubResources(javaElements));
	}

	private static boolean hasReadOnlyResourcesAndSubResources(IJavaScriptElement[] javaElements) throws CoreException {
		for (int i= 0; i < javaElements.length; i++) {
			if (hasReadOnlyResourcesAndSubResources(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean hasReadOnlyResourcesAndSubResources(IJavaScriptElement javaElement) throws CoreException {
		switch(javaElement.getElementType()){
			case IJavaScriptElement.CLASS_FILE:
				//if this assert fails, it means that a precondition is missing
				Assert.isTrue(((IClassFile)javaElement).getResource() instanceof IFile);
				//$FALL-THROUGH$
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				IResource resource= ReorgUtils.getResource(javaElement);
				return (resource != null && Resources.isReadOnly(resource));
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				IResource packResource= ReorgUtils.getResource(javaElement);
				if (packResource == null)
					return false;
				IPackageFragment pack= (IPackageFragment)javaElement;
				if (Resources.isReadOnly(packResource))
					return true;
				Object[] nonJava= pack.getNonJavaScriptResources();
				for (int i= 0; i < nonJava.length; i++) {
					Object object= nonJava[i];
					if (object instanceof IResource && hasReadOnlyResourcesAndSubResources((IResource)object))
						return true;
				}
				return hasReadOnlyResourcesAndSubResources(pack.getChildren());
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement;
				if (root.isArchive())
					return false;
				IResource pfrResource= ReorgUtils.getResource(javaElement);
				if (pfrResource == null)
					return false;
				if (Resources.isReadOnly(pfrResource))
					return true;
				Object[] nonJava1= root.getNonJavaScriptResources();
				for (int i= 0; i < nonJava1.length; i++) {
					Object object= nonJava1[i];
					if (object instanceof IResource && hasReadOnlyResourcesAndSubResources((IResource)object))
						return true;
				}
				return hasReadOnlyResourcesAndSubResources(root.getChildren());

			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.IMPORT_CONTAINER:
			case IJavaScriptElement.IMPORT_DECLARATION:
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.METHOD:
			case IJavaScriptElement.TYPE:
				return false;
			default: 
				Assert.isTrue(false);//not handled here
				return false;
		}
	}

	private static boolean hasReadOnlyResourcesAndSubResources(IResource[] resources) throws CoreException {
		for (int i= 0; i < resources.length; i++) {
			if (hasReadOnlyResourcesAndSubResources(resources[i]))
				return true;
		}
		return false;
	}

	private static boolean hasReadOnlyResourcesAndSubResources(IResource resource) throws CoreException {
		if (resource.isLinked()) //we don't want to count these because we never actually delete linked resources
			return false;
		if (Resources.isReadOnly(resource))
			return true;
		if (resource instanceof IContainer)
			return hasReadOnlyResourcesAndSubResources(((IContainer)resource).members());
		return false;
	}
}
