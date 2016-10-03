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
package org.eclipse.wst.jsdt.internal.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.dialogs.TypeSelectionExtension;

/**
 * A type selection dialog used for opening types.
 */
public class OpenTypeSelectionDialog extends FilteredTypesSelectionDialog {

	private static final String DIALOG_SETTINGS= "org.eclipse.wst.jsdt.internal.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaScriptSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaScriptSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(parent, multi, context, scope, elementKinds, extension);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog#getDialogSettings()
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaScriptPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings= JavaScriptPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}
}
