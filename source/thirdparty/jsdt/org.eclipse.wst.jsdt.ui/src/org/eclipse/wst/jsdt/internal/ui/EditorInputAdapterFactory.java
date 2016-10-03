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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.search.ui.ISearchPageScoreComputer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchPageScoreComputer;
import org.eclipse.wst.jsdt.internal.ui.search.SearchUtil;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Adapter factory to support basic UI operations for for editor inputs.
 */
public class EditorInputAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[] {IJavaScriptElement.class};
	
	private Object fSearchPageScoreComputer;

	public Class[] getAdapterList() {
		updateLazyLoadedAdapters();
		return PROPERTIES;
	}

	public Object getAdapter(Object element, Class key) {
		updateLazyLoadedAdapters();
		if (fSearchPageScoreComputer != null && ISearchPageScoreComputer.class.equals(key))
			return fSearchPageScoreComputer;
		
		if (IJavaScriptElement.class.equals(key) && element instanceof IEditorInput) {
			IJavaScriptElement je= JavaScriptUI.getWorkingCopyManager().getWorkingCopy((IEditorInput)element); 
			if (je != null)
				return je;
			if (element instanceof IStorageEditorInput) {
				try {
					return ((IStorageEditorInput)element).getStorage().getAdapter(key);
				} catch (CoreException ex) {
					// Fall through
				}
			}
		}
		return null;
	}

	private void updateLazyLoadedAdapters() {
		if (fSearchPageScoreComputer == null && SearchUtil.isSearchPlugInActivated())
			createSearchPageScoreComputer();
	}
	
	private void createSearchPageScoreComputer() {
		fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
		PROPERTIES= new Class[] {ISearchPageScoreComputer.class, IJavaScriptElement.class};
	}
}
