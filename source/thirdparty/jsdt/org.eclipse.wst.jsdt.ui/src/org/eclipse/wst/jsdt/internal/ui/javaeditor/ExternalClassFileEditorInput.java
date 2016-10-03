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
package org.eclipse.wst.jsdt.internal.ui.javaeditor;



import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.JavaScriptCore;


/**
 * Editor input for .class files on the file system.
 */
public class ExternalClassFileEditorInput extends FileEditorInput implements IClassFileEditorInput {

	private IClassFile fClassFile;

	ExternalClassFileEditorInput(IFile file) {
		super(file);
		refresh();
	}

	/*
	 * @see IClassFileEditorInput#getClassFile()
	 */
	public IClassFile getClassFile() {
		return fClassFile;
	}

	/**
	 * Refreshes this input element. Workaround for non-updating class file elements.
	 */
	public void refresh() {
		Object element= JavaScriptCore.create(getFile());
		if (element instanceof IClassFile)
			fClassFile= (IClassFile) element;
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IClassFile.class)
			return fClassFile;
		return fClassFile.getAdapter(adapter);
	}

}
