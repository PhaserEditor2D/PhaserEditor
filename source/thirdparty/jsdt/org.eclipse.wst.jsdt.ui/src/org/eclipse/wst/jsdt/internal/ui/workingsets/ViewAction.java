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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;

public class ViewAction extends Action {

	private final ViewActionGroup fActionGroup;
	private final int fMode;

	public ViewAction(ViewActionGroup group, int mode) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		Assert.isNotNull(group);
		fActionGroup= group;
		fMode= mode;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {
		if (isChecked())
			fActionGroup.setMode(fMode);
	}
}
