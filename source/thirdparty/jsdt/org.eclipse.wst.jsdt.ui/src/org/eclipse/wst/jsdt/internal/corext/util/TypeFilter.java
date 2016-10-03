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
package org.eclipse.wst.jsdt.internal.corext.util;

import java.util.StringTokenizer;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.StringMatcher;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 *
 */
public class TypeFilter implements IPropertyChangeListener {
	
	public static TypeFilter getDefault() {
		return JavaScriptPlugin.getDefault().getTypeFilter();
	}
	
	public static boolean isFiltered(String fullTypeName) {
		return getDefault().filter(fullTypeName);
	}
	
	public static boolean isFiltered(char[] fullTypeName) {
		return getDefault().filter(new String(fullTypeName));
	}
		
	public static boolean isFiltered(char[] packageName, char[] typeName) {
		return getDefault().filter(JavaModelUtil.concatenateName(packageName, typeName));
	}
	
	public static boolean isFiltered(IType type) {
		TypeFilter typeFilter = getDefault();
		if (typeFilter.hasFilters()) {
			return typeFilter.filter(JavaModelUtil.getFullyQualifiedName(type));
		}
		return false;
	}
	
	public static boolean isFiltered(TypeNameMatch match) {
		return getDefault().filter(match.getFullyQualifiedName());
	}

	private StringMatcher[] fStringMatchers;

	/**
	 * 
	 */
	public TypeFilter() {
		fStringMatchers= null;
		PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
	}
	
	private synchronized StringMatcher[] getStringMatchers() {
		if (fStringMatchers == null) {
			String str= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.TYPEFILTER_ENABLED);
			StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
			int nTokens= tok.countTokens();
			
			fStringMatchers= new StringMatcher[nTokens];
			for (int i= 0; i < nTokens; i++) {
				String curr= tok.nextToken();
				if (curr.length() > 0) { 
					fStringMatchers[i]= new StringMatcher(curr, false, false);
				}
			}
		}
		return fStringMatchers;
	}
	
	public void dispose() {
		PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
		fStringMatchers= null;
	}
	
	
	public boolean hasFilters() {
		return getStringMatchers().length > 0;
	}
	
	public boolean filter(String fullTypeName) {
		StringMatcher[] matchers= getStringMatchers();
		for (int i= 0; i < matchers.length; i++) {
			StringMatcher curr= matchers[i];
			if (curr.match(fullTypeName)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public synchronized void propertyChange(PropertyChangeEvent event) {
		if (PreferenceConstants.TYPEFILTER_ENABLED.equals(event.getProperty())) {
			fStringMatchers= null;
		}
	}


}
