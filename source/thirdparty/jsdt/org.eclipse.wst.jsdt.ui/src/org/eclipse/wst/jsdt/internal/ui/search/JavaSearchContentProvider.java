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
package org.eclipse.wst.jsdt.internal.ui.search;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public abstract class JavaSearchContentProvider implements IStructuredContentProvider {
	protected final Object[] EMPTY_ARR= new Object[0];
	
	private JavaSearchResult fResult;
	private JavaSearchResultPage fPage;

	JavaSearchContentProvider(JavaSearchResultPage page) {
		fPage= page;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		initialize((JavaSearchResult) newInput);
		
	}
	
	protected void initialize(JavaSearchResult result) {
		fResult= result;
	}
	
	public abstract void elementsChanged(Object[] updatedElements);
	public abstract void clear();

	public void dispose() {
		// nothing to do
	}

	JavaSearchResultPage getPage() {
		return fPage;
	}
	
	JavaSearchResult getSearchResult() {
		return fResult;
	}

}
