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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;

/**
 * A modification collector for Java element delete operations.
 */
public class DeleteModifications extends RefactoringModifications {
	
	private List/*<IJavaScriptElement>*/ fDelete;
	
	/**
	 * Contains the actual packages when executing
	 * <code>handlePackageFragmentDelete</code>. This is part of the
	 * algorithm to check if a parent folder can be deleted.
	 */
	private List/*<IPackageFragment>*/ fPackagesToDelete;
	
	public DeleteModifications() {
		fDelete= new ArrayList();
		fPackagesToDelete= new ArrayList();
	}
	
	public void delete(IResource resource) {
		getResourceModifications().addDelete(resource);
	}
	
	public void delete(IResource[] resources) {
		for (int i= 0; i < resources.length; i++) {
			delete(resources[i]);
		}
	}
	
	public void delete(IJavaScriptElement[] elements) throws CoreException {
		for (int i= 0; i < elements.length; i++) {
			delete(elements[i]);
		}
	}
	
	public void delete(IJavaScriptElement element) throws CoreException {
		switch(element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				return;
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				fDelete.add(element);
				if (element.getResource() != null)
					getResourceModifications().addDelete(element.getResource());
				return;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				fDelete.add(element);
				IResource resource= element.getResource();
				// Flag an resource change even if we have an archive. If it is
				// internal (we have a underlying resource then we have a resource
				// change.
				if (resource != null)
					getResourceModifications().addDelete(resource);
				IJavaScriptProject[] referencingProjects= JavaElementUtil.getReferencingProjects((IPackageFragmentRoot) element);
				for (int i= 0; i < referencingProjects.length; i++) {
					IFile classpath= referencingProjects[i].getJSDTScopeFile(); //$NON-NLS-1$
					getResourceModifications().addChanged(classpath);
				}
				return;
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				fDelete.add(element);
				fPackagesToDelete.add(element);
				return;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				fDelete.add(element);
				IType[] types= ((IJavaScriptUnit)element).getTypes();
				fDelete.addAll(Arrays.asList(types));
				if (element.getResource() != null)
					getResourceModifications().addDelete(element.getResource());
				return;
			default:
				fDelete.add(element);
		}
		
	}
	
	/**
	 * @return a List of IResources that are removed by package deletes
	 */
	public List/*<IResource>*/ postProcess() throws CoreException {
		ArrayList resourcesCollector= new ArrayList();
		for (Iterator iter= fPackagesToDelete.iterator(); iter.hasNext();) {
			IPackageFragment pack= (IPackageFragment) iter.next();
			handlePackageFragmentDelete(pack, resourcesCollector);
		}
		return resourcesCollector;
	}
	
	public void buildDelta(IResourceChangeDescriptionFactory deltaFactory) {
		getResourceModifications().buildDelta(deltaFactory);
	}
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor owner, String[] natures, SharableParticipants shared) {
		List result= new ArrayList();
		for (Iterator iter= fDelete.iterator(); iter.hasNext();) {
			result.addAll(Arrays.asList(ParticipantManager.loadDeleteParticipants(status, 
				owner, iter.next(), 
				new DeleteArguments(), natures, shared)));
		}
		result.addAll(Arrays.asList(getResourceModifications().getParticipants(status, owner, natures, shared)));
		return (RefactoringParticipant[]) result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	/**
	 * This method collects file and folder deletion for notifying
	 * participants. Participants will get notified of 
	 * 
	 * * deletion of the package (in any case)
	 * * deletion of files within the package if only the files are deleted without 
	 *   the package folder ("package cleaning")
	 * * deletion of the package folder if it is not only cleared and if its parent
	 *   is not removed as well.
	 *   
	 * All deleted resources are added to <code>resourcesCollector</code>
	 * 
	 * @param resourcesCollector a collector for IResources to be deleted
	 */
	private void handlePackageFragmentDelete(IPackageFragment pack, ArrayList resourcesCollector) throws CoreException {		
		final IContainer container= (IContainer)pack.getResource();
		if (container == null)
			return;
		
		final IResource[] members= container.members();

		/*
		 * Check whether this package is removed completely or only cleared.
		 * The default package can never be removed completely.
		 */
		if (!pack.isDefaultPackage() && canRemoveCompletely(pack)) {
			// This package is removed completely, which means its folder will be
			// deleted as well. We only notify participants of the folder deletion
			// if the parent folder of this folder will not be deleted as well:
			boolean parentIsMarked= false;
			final IPackageFragment parent= JavaElementUtil.getParentSubpackage(pack);
			if (parent == null) {
				// "Parent" is the default package which will never be
				// deleted physically
				parentIsMarked= false;
			} else {
				// Parent is marked if it is in the list
				parentIsMarked= fPackagesToDelete.contains(parent);
			}
			
			if (parentIsMarked) {
				// Parent is marked, but is it really deleted or only cleared?
				if (canRemoveCompletely(parent)) {
					// Parent can be removed completely, so we do not add
					// this folder to the list.
				} else {
					// Parent cannot be removed completely, but as this folder
					// can be removed, we notify the participant
					resourcesCollector.add(container);
					getResourceModifications().addDelete(container);
				}
			} else {
				// Parent will not be removed, but we will 
				resourcesCollector.add(container);
				getResourceModifications().addDelete(container);
			}
		} else {
			// This package is only cleared because it has subpackages (=subfolders)
			// which are not deleted. As the package is only cleared, its folder
			// will not be removed and so we must notify the participant of the deleted children.
			for (int m= 0; m < members.length; m++) {
				IResource member= members[m];
				if (member instanceof IFile) {
					IFile file= (IFile)member;
					if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
						continue;
					if (pack.isDefaultPackage() && ! JavaScriptCore.isJavaScriptLikeFileName(file.getName()))
						continue;
					resourcesCollector.add(member);
					getResourceModifications().addDelete(member);
				}
				if (!pack.isDefaultPackage() && member instanceof IFolder) {
					// Normally, folder children of packages are packages
					// as well, but in case they have been removed from the build
					// path, notify the participant
					IPackageFragment frag= (IPackageFragment) JavaScriptCore.create(member);
					if (frag == null) {
						resourcesCollector.add(member);
						getResourceModifications().addDelete(member);
					}
				}
			}
		}
	}
	
	/**
	 * Returns true if this initially selected package is really deletable
	 * (if it has non-selected sub packages, it may only be cleared).
	 */
	private boolean canRemoveCompletely(IPackageFragment pack) throws JavaScriptModelException {
		final IPackageFragment[] subPackages= JavaElementUtil.getPackageAndSubpackages(pack);
		for (int i= 0; i < subPackages.length; i++) {
			if (!(subPackages[i].equals(pack)) && !(fPackagesToDelete.contains(subPackages[i])))
				return false;
		}
		return true;
	}
}
