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
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

class OverwriteHelper {
	private Object fDestination;
	private IFile[] fFiles= new IFile[0];
	private IFolder[] fFolders= new IFolder[0];
	private IJavaScriptUnit[] fCus= new IJavaScriptUnit[0];
	private IPackageFragmentRoot[] fRoots= new IPackageFragmentRoot[0];
	private IPackageFragment[] fPackageFragments= new IPackageFragment[0];

	public void setFiles(IFile[] files) {
		Assert.isNotNull(files);
		fFiles= files;
	}

	public void setFolders(IFolder[] folders) {
		Assert.isNotNull(folders);
		fFolders= folders;
	}

	public void setCus(IJavaScriptUnit[] cus) {
		Assert.isNotNull(cus);
		fCus= cus;
	}
	
	public void setPackageFragmentRoots(IPackageFragmentRoot[] roots) {
		Assert.isNotNull(roots);
		fRoots= roots;
	}	

	public void setPackages(IPackageFragment[] fragments) {
		Assert.isNotNull(fragments);
		fPackageFragments= fragments;
	}

	public IFile[] getFilesWithoutUnconfirmedOnes() {
		return fFiles;
	}

	public IFolder[] getFoldersWithoutUnconfirmedOnes() {
		return fFolders;
	}

	public IJavaScriptUnit[] getCusWithoutUnconfirmedOnes() {
		return fCus;
	}

	public IPackageFragmentRoot[] getPackageFragmentRootsWithoutUnconfirmedOnes() {
		return fRoots;
	}

	public IPackageFragment[] getPackagesWithoutUnconfirmedOnes() {
		return fPackageFragments;
	}

	public void confirmOverwriting(IReorgQueries reorgQueries, IJavaScriptElement destination) {
		Assert.isNotNull(destination);
		fDestination= destination;
		confirmOverwritting(reorgQueries);
	}

	public void confirmOverwriting(IReorgQueries reorgQueries, IResource destination) {
		Assert.isNotNull(destination);
		Assert.isNotNull(reorgQueries);
		fDestination= destination;
		confirmOverwritting(reorgQueries);
	}
	
	private void confirmOverwritting(IReorgQueries reorgQueries) {
		IConfirmQuery overwriteQuery= reorgQueries.createYesYesToAllNoNoToAllQuery(RefactoringCoreMessages.OverwriteHelper_0, true, IReorgQueries.CONFIRM_OVERWRITING); 
		IConfirmQuery skipQuery= reorgQueries.createSkipQuery(RefactoringCoreMessages.OverwriteHelper_2, IReorgQueries.CONFIRM_SKIPPING); 
		confirmFileOverwritting(overwriteQuery);
		confirmFolderOverwritting(skipQuery);
		confirmCuOverwritting(overwriteQuery);	
		confirmPackageFragmentRootOverwritting(skipQuery);	
		confirmPackageOverwritting(overwriteQuery);	
	}

	private void confirmPackageFragmentRootOverwritting(IConfirmQuery overwriteQuery) {
		List toNotOverwrite= new ArrayList(1);
		for (int i= 0; i < fRoots.length; i++) {
			IPackageFragmentRoot root= fRoots[i];
			if (canOverwrite(root) && ! skip(root.getElementName(), overwriteQuery))
				toNotOverwrite.add(root);
		}
		IPackageFragmentRoot[] roots= (IPackageFragmentRoot[]) toNotOverwrite.toArray(new IPackageFragmentRoot[toNotOverwrite.size()]);
		fRoots= ArrayTypeConverter.toPackageFragmentRootArray(ReorgUtils.setMinus(fRoots, roots));
	}

	private void confirmCuOverwritting(IConfirmQuery overwriteQuery) {
		List cusToNotOverwrite= new ArrayList(1);
		for (int i= 0; i < fCus.length; i++) {
			IJavaScriptUnit cu= fCus[i];
			if (canOverwrite(cu) && ! overwrite(cu, overwriteQuery))
				cusToNotOverwrite.add(cu);
		}
		IJavaScriptUnit[] cus= (IJavaScriptUnit[]) cusToNotOverwrite.toArray(new IJavaScriptUnit[cusToNotOverwrite.size()]);
		fCus= ArrayTypeConverter.toCuArray(ReorgUtils.setMinus(fCus, cus));
	}

	private void confirmFolderOverwritting(IConfirmQuery overwriteQuery) {
		List foldersToNotOverwrite= new ArrayList(1);
		for (int i= 0; i < fFolders.length; i++) {
			IFolder folder= fFolders[i];
			if (canOverwrite(folder) && ! skip(folder.getName(), overwriteQuery))
				foldersToNotOverwrite.add(folder);				
		}
		IFolder[] folders= (IFolder[]) foldersToNotOverwrite.toArray(new IFolder[foldersToNotOverwrite.size()]);
		fFolders= ArrayTypeConverter.toFolderArray(ReorgUtils.setMinus(fFolders, folders));
	}

	private void confirmFileOverwritting(IConfirmQuery overwriteQuery) {
		List filesToNotOverwrite= new ArrayList(1);
		for (int i= 0; i < fFiles.length; i++) {
			IFile file= fFiles[i];
			if (canOverwrite(file) && ! overwrite(file, overwriteQuery))
				filesToNotOverwrite.add(file);
		}
		IFile[] files= (IFile[]) filesToNotOverwrite.toArray(new IFile[filesToNotOverwrite.size()]);
		fFiles= ArrayTypeConverter.toFileArray(ReorgUtils.setMinus(fFiles, files));
	}

	private void confirmPackageOverwritting(IConfirmQuery overwriteQuery){
		List toNotOverwrite= new ArrayList(1);
		for (int i= 0; i < fPackageFragments.length; i++) {
			IPackageFragment pack= fPackageFragments[i];
			if (canOverwrite(pack) && ! overwrite(pack, overwriteQuery))
				toNotOverwrite.add(pack);
		}
		IPackageFragment[] packages= (IPackageFragment[]) toNotOverwrite.toArray(new IPackageFragment[toNotOverwrite.size()]);
		fPackageFragments= ArrayTypeConverter.toPackageArray(ReorgUtils.setMinus(fPackageFragments, packages));
	}

	private boolean canOverwrite(IPackageFragment pack) {
		Assert.isTrue(fDestination instanceof IPackageFragmentRoot);
		IPackageFragmentRoot destination= (IPackageFragmentRoot)fDestination;
		return ! destination.equals(pack.getParent()) && destination.getPackageFragment(pack.getElementName()).exists();
	}

	private boolean canOverwrite(IResource resource) {
		if (resource == null)
			return false;
		IResource destinationResource= ResourceUtil.getResource(fDestination);
		if (destinationResource.equals(resource.getParent()))
			return false;
		if (destinationResource instanceof IContainer) {
			IContainer container= (IContainer)destinationResource;
			IResource member=  container.findMember(resource.getName());
			if (member == null || !member.exists())
				return false;
			if (member instanceof IContainer) {
				try {
					if (((IContainer)member).members().length == 0)
						return false;
				} catch (CoreException e) {
					return true;
				}
			}
			return true;
		}
		return false;
	}
	
	private boolean canOverwrite(IPackageFragmentRoot root) {
		Assert.isTrue(fDestination instanceof IJavaScriptProject);
		IJavaScriptProject destination= (IJavaScriptProject)fDestination;
		IFolder conflict= destination.getProject().getFolder(root.getElementName());
		try {
			return !destination.equals(root.getParent()) && conflict.exists() &&  conflict.members().length > 0;
		} catch (CoreException e) {
			return true;
		}
	}

	private boolean canOverwrite(IJavaScriptUnit cu) {
		if (fDestination instanceof IPackageFragment){
			IPackageFragment destination= (IPackageFragment)fDestination;
			return ! destination.equals(cu.getParent()) && destination.getJavaScriptUnit(cu.getElementName()).exists();
		} else {
			return canOverwrite(ReorgUtils.getResource(cu));
		}
	}

	private static boolean overwrite(IResource resource, IConfirmQuery overwriteQuery){
		return overwrite(resource.getName(), overwriteQuery);
	}

	private static boolean overwrite(IJavaScriptElement element, IConfirmQuery overwriteQuery){
		return overwrite(element.getElementName(), overwriteQuery);
	}

	private static boolean overwrite(String name, IConfirmQuery overwriteQuery){
		String question= Messages.format(RefactoringCoreMessages.OverwriteHelper_1, name); 
		return overwriteQuery.confirm(question);
	}
	private static boolean skip(String name, IConfirmQuery overwriteQuery){
		String question= Messages.format(RefactoringCoreMessages.OverwriteHelper_3, name); 
		return overwriteQuery.confirm(question);
	}	
}
