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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.search.ui.ISearchPageScoreComputer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchPageScoreComputer;
import org.eclipse.wst.jsdt.internal.ui.search.SearchUtil;

/**
 * Implements basic UI support for LogicalPackage.
 */
public class LogicalPackageAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[] {
		ResourceMapping.class
	};

	// Must be Object to allow lazy loading	
	private Object fSearchPageScoreComputer;
	
	public Class[] getAdapterList() {
		updateLazyLoadedAdapters();
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		updateLazyLoadedAdapters();
		
		if (fSearchPageScoreComputer != null && ISearchPageScoreComputer.class.equals(key)) {
			return fSearchPageScoreComputer;
		} else if (ResourceMapping.class.equals(key)) {
			if (!(element instanceof LogicalPackage))
				return null;
			return JavaElementResourceMapping.create((LogicalPackage)element);
		}
		return null; 
	}
	
	private void updateLazyLoadedAdapters() {
		if (fSearchPageScoreComputer == null && SearchUtil.isSearchPlugInActivated())
			createSearchPageScoreComputer();
	}

	private void createSearchPageScoreComputer() {
		fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
		PROPERTIES= new Class[] {
			ISearchPageScoreComputer.class,
			ResourceMapping.class
		};
	}
}
