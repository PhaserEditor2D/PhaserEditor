/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJarEntryResource;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;


/**
 * An EditorInput for a JarEntryFile.
 */
public class JarEntryEditorInput implements IStorageEditorInput {

	private IStorage fJarEntryFile;

	public JarEntryEditorInput(IStorage jarEntryFile) {
		fJarEntryFile= jarEntryFile;
	}

	/*
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof JarEntryEditorInput))
			return false;
		JarEntryEditorInput other= (JarEntryEditorInput) obj;
		return fJarEntryFile.equals(other.fJarEntryFile);
	}

	/*
	 * @see IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/*
	 * @see IEditorInput#getName()
	 */
	public String getName() {
		return fJarEntryFile.getName();
	}

	/*
	 * @see IEditorInput#getContentType()
	 */
	public String getContentType() {
		return fJarEntryFile.getFullPath().getFileExtension();
	}

	/*
	 * @see IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		if (fJarEntryFile instanceof IJarEntryResource) {
			IJarEntryResource jarEntry= (IJarEntryResource)fJarEntryFile;
			IPackageFragmentRoot root= jarEntry.getPackageFragmentRoot();
			IPath fullPath= root.getPath().append(fJarEntryFile.getFullPath());
			if (root.isExternal())
				return fullPath.toOSString();
			return fullPath.toString();
			
		}
		
		IPath fullPath= fJarEntryFile.getFullPath();
		if (fullPath == null)
			return null;
		return fullPath.toString();
	}

	/*
	 * @see IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		return registry.getImageDescriptor(getContentType());
	}

	/*
	 * @see IEditorInput#exists()
	 */
	public boolean exists() {
		// JAR entries can't be deleted
		return true;
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 * see IStorageEditorInput#getStorage()
	 */
	 public IStorage getStorage() {
	 	return fJarEntryFile;
	 }
}


