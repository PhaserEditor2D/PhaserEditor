/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ICreateTargetQueries;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ICreateTargetQuery;

public class CreateTargetQueries implements ICreateTargetQueries {

	private final Wizard fWizard;
	private final Shell fShell;

	public CreateTargetQueries(Wizard wizard) {
		fWizard= wizard;
		fShell= null;
	}
	
	public CreateTargetQueries(Shell shell) {
		fShell = shell;
		fWizard= null;
	}
	
	public ICreateTargetQuery createNewPackageQuery() {
		return new ICreateTargetQuery() {
			public Object getCreatedTarget(Object selection) {
				return null;
			}
			
			public String getNewButtonLabel() {
				return ReorgMessages.ReorgMoveWizard_newPackage;
			}
		};
	}
}
