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

import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

public final class PackageSelectionDialogButtonField extends StringButtonDialogField {

	private IPackageFragment fPackageFragment;

	public PackageSelectionDialogButtonField(String label, String button, PackageBrowseAdapter adapter, IJavaScriptProject root) {
		super(adapter);
		setContentAssistProcessor(new JavaPackageCompletionProcessor(new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_ROOT)));

		IPackageFragmentRoot[] roots;
		try {
			roots= root.getAllPackageFragmentRoots();
			((JavaPackageCompletionProcessor)getContentAssistProcessor()).setPackageFragmentRoot(roots[0]);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			// if exception no content assist .. but thats no problem
		}
		setLabelText(label);
		setButtonLabel(button);
		adapter.setReceiver(this);
	}

	public void setPackage(IPackageFragment packageFragment) {
		fPackageFragment= packageFragment;
		if (fPackageFragment != null) {
			setText(fPackageFragment.getElementName());

			JavaPackageCompletionProcessor contentAssist= (JavaPackageCompletionProcessor)getContentAssistProcessor();
			contentAssist.setPackageFragmentRoot((IPackageFragmentRoot)packageFragment.getParent());
		}
	}
}
