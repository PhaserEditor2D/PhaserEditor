/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;


/**
 * Class file considered as editor input.
 */
public class InternalClassFileEditorInput implements IClassFileEditorInput, IPersistableElement {

	private IClassFile fClassFile;

	public InternalClassFileEditorInput(IClassFile classFile) {
		fClassFile= classFile;
	}

	/*
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof InternalClassFileEditorInput))
			return false;
		InternalClassFileEditorInput other= (InternalClassFileEditorInput) obj;
		return fClassFile.equals(other.fClassFile);
	}

	/*
	 * @see Object#hashCode
	 */
	public int hashCode() {
		return fClassFile.hashCode();
	}

	/*
	 * @see IClassFileEditorInput#getClassFile()
	 */
	public IClassFile getClassFile() {
		return fClassFile;
	}

	/*
	 * @see IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return this;
	}

	/*
	 * @see IEditorInput#getName()
	 */
	public String getName() {
		return fClassFile.getDisplayName();
	}

	/*
	 * @see IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
//		return fClassFile.getType().getFullyQualifiedName();
		StringBuffer s = new StringBuffer();
		JavaScriptElementLabels.getElementLabel(fClassFile, JavaScriptElementLabels.ALL_POST_QUALIFIED | JavaScriptElementLabels.APPEND_ROOT_PATH, s);
		return s.toString();
	}

	/*
	 * @see IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		try {
			if (fClassFile.isClass())
				return JavaPluginImages.DESC_OBJS_CFILECLASS;
			return JavaPluginImages.DESC_OBJS_CFILEINT;
		} catch (JavaScriptModelException e) {
			// fall through
		}
		return JavaPluginImages.DESC_OBJS_CFILE;
	}

	/*
	 * @see IEditorInput#exists()
	 */
	public boolean exists() {
		return fClassFile.exists();
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IClassFile.class)
			return fClassFile;
		else if(adapter == IJavaScriptElement.class)
			return fClassFile;
		return null;
	}

	/*
	 * @see IPersistableElement#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
		ClassFileEditorInputFactory.saveState(memento, this);
	}

	/*
	 * @see IPersistableElement#getFactoryId()
	 */
	public String getFactoryId() {
		return ClassFileEditorInputFactory.ID;
	}
}


