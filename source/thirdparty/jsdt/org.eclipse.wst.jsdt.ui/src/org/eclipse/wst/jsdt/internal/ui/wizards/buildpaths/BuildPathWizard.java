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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewElementWizard;

public abstract class BuildPathWizard extends NewElementWizard {
	
	private boolean fDoFlushChange;
	private final CPListElement fEntryToEdit;
	private IPackageFragmentRoot fPackageFragmentRoot;
	private final ArrayList fExistingEntries;

	public BuildPathWizard(CPListElement[] existingEntries, CPListElement newEntry, String titel, ImageDescriptor image) {
		if (image != null)
			setDefaultPageImageDescriptor(image);
		
		setDialogSettings(JavaScriptPlugin.getDefault().getDialogSettings());
		setWindowTitle(titel);

		fEntryToEdit= newEntry;
		fExistingEntries= new ArrayList(Arrays.asList(existingEntries));
		fDoFlushChange= true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		if (fDoFlushChange) {
			IJavaScriptProject javaProject= getEntryToEdit().getJavaProject();
			
			BuildPathsBlock.flush(getExistingEntries(),  javaProject,  getSuperType(), monitor);
			
			IProject project= javaProject.getProject();
			IPath path= getEntryToEdit().getPath();
			
			IResource folder= project.getWorkspace().getRoot().findMember(path);
			fPackageFragmentRoot= javaProject.getPackageFragmentRoot(folder);
		}
	}
	public LibrarySuperType getSuperType() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IJavaScriptElement getCreatedElement() {
		return fPackageFragmentRoot;
	}
	
	public void setDoFlushChange(boolean b) {
		fDoFlushChange= b;
	}
	
	public ArrayList getExistingEntries() {
		return fExistingEntries;
	}

	protected CPListElement getEntryToEdit() {
		return fEntryToEdit;
	}

	public List/*<CPListElement>*/ getInsertedElements() {
		return new ArrayList();
	}

	public List/*<CPListElement>*/ getRemovedElements() {
		return new ArrayList();
	}

	public List/*<CPListElement>*/ getModifiedElements() {
		ArrayList result= new ArrayList(1);
		result.add(fEntryToEdit);
		return result;
	}
	
	public abstract void cancel();

}
