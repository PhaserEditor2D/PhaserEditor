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


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;

/**
 * The factory which is capable of recreating class file editor
 * inputs stored in a memento.
 */
public class ClassFileEditorInputFactory implements IElementFactory {

	public final static String ID=  "org.eclipse.wst.jsdt.ui.ClassFileEditorInputFactory"; //$NON-NLS-1$
	public final static String KEY= "org.eclipse.wst.jsdt.ui.ClassFileIdentifier"; //$NON-NLS-1$

	public ClassFileEditorInputFactory() {
	}

	/**
	 * @see IElementFactory#createElement
	 */
	public IAdaptable createElement(IMemento memento) {
		String identifier= memento.getString(KEY);
		if (identifier == null)
			return null;
			
		IJavaScriptElement element= JavaScriptCore.create(identifier);
		try {
			if (!element.exists() && element instanceof IClassFile) {
				/*
				 * Let's try to find the class file,
				 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=83221
				 */ 
				IClassFile cf= (IClassFile)element;
				IType type= cf.getType(); // this will work, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=154667
				IJavaScriptProject project= element.getJavaScriptProject();
				if (project != null) {
					type= JavaModelUtil.findType(project, type.getFullyQualifiedName());
					if (type == null)
						return null;
					element= type.getParent();
				}
			}
			return EditorUtility.getEditorInput(element);
		} catch (JavaScriptModelException x) {
			// Don't report but simply return null
			return null;
		}
	}

	public static void saveState(IMemento memento, InternalClassFileEditorInput input) {
		IClassFile c= input.getClassFile();
		memento.putString(KEY, c.getHandleIdentifier());
	}
}
