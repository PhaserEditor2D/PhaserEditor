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
package org.eclipse.wst.jsdt.internal.ui.filters;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.util.StringMatcher;

/**
 * The NamePatternFilter selects the elements which
 * match the given string patterns.
 * <p>
 * The following characters have special meaning:
 *   ? => any character
 *   * => any string
 * </p>
 * 
 * 
 */
public class NamePatternFilter extends ViewerFilter {
	private String[] fPatterns;
	private StringMatcher[] fMatchers;
	
	/**
	 * Return the currently configured StringMatchers.
	 * @return returns the matchers
	 */
	private StringMatcher[] getMatchers() {
		return fMatchers;
	}
	
	/**
	 * Gets the patterns for the receiver.
	 * @return returns the patterns to be filtered for
	 */
	public String[] getPatterns() {
		return fPatterns;
	}
	
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fMatchers.length == 0) {
			return true;
		}
		String matchName= null;
		if (element instanceof IJavaScriptElement) {
			matchName= ((IJavaScriptElement) element).getElementName();
		} else if (element instanceof IResource) {
			matchName= ((IResource) element).getName();
		}else if (element instanceof IStorage) {
			matchName= ((IStorage) element).getName();
		} else if (element instanceof IWorkingSet) {
			matchName= ((IWorkingSet) element).getLabel();
		} else if (element instanceof IAdaptable) {
			IWorkbenchAdapter wbadapter= (IWorkbenchAdapter) ((IAdaptable)element).getAdapter(IWorkbenchAdapter.class);
			if (wbadapter != null) {
				matchName= wbadapter.getLabel(element);
			}
		}
		if (matchName != null && matchName.length() > 0) {
			StringMatcher[] testMatchers= getMatchers();
			for (int i = 0; i < testMatchers.length; i++) {
				if (testMatchers[i].match(matchName))
					return false;
			}
			return true;
		}
		return true;
	}
	
	/**
	 * Sets the patterns to filter out for the receiver.
	 * <p>
	 * The following characters have special meaning:
	 *   ? => any character
	 *   * => any string
	 * </p>
	 * @param newPatterns the new patterns
	 */
	public void setPatterns(String[] newPatterns) {
		fPatterns = newPatterns;
		fMatchers = new StringMatcher[newPatterns.length];
		for (int i = 0; i < newPatterns.length; i++) {
			//Reset the matchers to prevent constructor overhead
			fMatchers[i]= new StringMatcher(newPatterns[i], true, false);
		}
	}
}
