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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

class PackageSelectionStringButtonAdapter implements IStringButtonAdapter {

	private final SourceFirstPackageSelectionDialogField fPackageSelectionField;
	private String fEmtpyListMessage;
	private String fMessage;
	private String fTitle;

	PackageSelectionStringButtonAdapter(SourceFirstPackageSelectionDialogField field, String title, String message,
		String emtpyListMessage) {
		fPackageSelectionField= field;
		fTitle= title;
		fMessage= message;
		fEmtpyListMessage= emtpyListMessage;
	}

	public void changeControlPressed(DialogField field) {
		IPackageFragmentRoot root= fPackageSelectionField.getSelectedFragmentRoot();

		IJavaScriptElement[] packages= null;
		try {
			if (root != null && root.exists()) {
				packages= root.getChildren();
			}
		} catch (JavaScriptModelException e) {
			// no need to react
		}

		if (packages == null) {
			packages= new IJavaScriptElement[0];
		}

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(field.getLabelControl(null).getShell(),
			new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(true);

		dialog.setTitle(fTitle);
		dialog.setMessage(fMessage);
		dialog.setEmptyListMessage(fEmtpyListMessage);
		dialog.setElements(packages);

		// TODO initial selection
		//    List selection = new ArrayList();
		//    selection.add(fPackageSelectionField.fPackageSelection.getPackageFragment());
		//    dialog.setInitialElementSelections(selection);

		if (dialog.open() == Window.OK) {
			IPackageFragment fragment= (IPackageFragment)dialog.getFirstResult();
			fPackageSelectionField.setSelected(fragment);
		}
	}
}
