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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.wst.jsdt.ui.actions.GenerateActionGroup;
import org.eclipse.wst.jsdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenViewActionGroup;
import org.eclipse.wst.jsdt.ui.actions.RefactorActionGroup;

class NewSearchViewActionGroup extends CompositeActionGroup {
	private OpenEditorActionGroup fOpenEditorActionGroup;
	
	public NewSearchViewActionGroup(IViewPart part) {
		Assert.isNotNull(part);
		OpenViewActionGroup openViewActionGroup;
		setGroups(new ActionGroup[]{
			fOpenEditorActionGroup= new OpenEditorActionGroup(part),
			openViewActionGroup= new OpenViewActionGroup(part),
			new GenerateActionGroup(part), 
			new RefactorActionGroup(part),
			new JavaSearchActionGroup(part) 
		});
		openViewActionGroup.containsShowInMenu(false);
	}
	
	public void handleOpen(OpenEvent event) {
		IAction openAction= fOpenEditorActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}
	}
}

