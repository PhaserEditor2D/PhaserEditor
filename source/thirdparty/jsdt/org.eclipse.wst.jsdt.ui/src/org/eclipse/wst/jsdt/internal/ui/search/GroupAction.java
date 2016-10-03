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

import org.eclipse.jface.action.Action;


public class GroupAction extends Action {
	private int fGrouping;
	private JavaSearchResultPage fPage;
	
	public GroupAction(String label, String tooltip, JavaSearchResultPage page, int grouping) {
		super(label);
		setToolTipText(tooltip);
		fPage= page;
		fGrouping= grouping;
	}

	public void run() {
		fPage.setGrouping(fGrouping);
	}

	public int getGrouping() {
		return fGrouping;
	}
}
