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
package org.eclipse.wst.jsdt.internal.ui;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

public class ResourceAdapterFactory implements IAdapterFactory {

	private static Class[] PROPERTIES= new Class[] {
		IJavaScriptElement.class
	};
		
	public Class[] getAdapterList() {
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		if (IJavaScriptElement.class.equals(key)) {
			
			// Performance optimization, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133141
			if (element instanceof IFile) {
				IJavaScriptElement je= JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(new FileEditorInput((IFile)element));
				if (je != null)
					return je;
			}
			
			return JavaScriptCore.create((IResource)element);
		}
		return null;
	}	
}
