/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *   Michael Fraenkel (fraenkel@us.ibm.com) - patch
 *          (report 60714: Call Hierarchy: display search scope in view title)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;


abstract class SearchScopeAction extends Action {
	private final SearchScopeActionGroup fGroup;
	
	public SearchScopeAction(SearchScopeActionGroup group, String text) {
		super(text, AS_RADIO_BUTTON);
		this.fGroup = group;
	}
	
	public abstract IJavaScriptSearchScope getSearchScope();
	
	public abstract int getSearchScopeType();
	
	public void run() {
		this.fGroup.setSelected(this, true);
	}
	
	public abstract String getFullDescription();
}
