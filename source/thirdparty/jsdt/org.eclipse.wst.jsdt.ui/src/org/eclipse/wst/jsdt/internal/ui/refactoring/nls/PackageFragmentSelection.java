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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;

class PackageFragmentSelection extends StringButtonStatusDialogField implements SourceChangeListener {

	private final SourceFirstPackageSelectionDialogField fDialogField;
	private JavaPackageCompletionProcessor fCurrPackageCompletionProcessor;
	private IDialogFieldListener fUpdateListener;

	public PackageFragmentSelection(SourceFirstPackageSelectionDialogField field, String packageLabel, String browseLabel,
		String statusHint, IStringButtonAdapter adapter) {
		super(adapter);
		fDialogField= field;
		setLabelText(packageLabel);
		setButtonLabel(browseLabel);
		setStatusWidthHint(statusHint);
		fCurrPackageCompletionProcessor= new JavaPackageCompletionProcessor();
	}

	public void setUpdateListener(IDialogFieldListener updateListener) {
		fUpdateListener= updateListener;
	}

	public Control[] doFillIntoGrid(Composite parent, int nColumns, int textWidth) {
		Control[] res= super.doFillIntoGrid(parent, nColumns);

		final Text text= getTextControl(null);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateListener();
			}
		});
		LayoutUtil.setWidthHint(text, textWidth);
		LayoutUtil.setHorizontalGrabbing(text);
		ControlContentAssistHelper.createTextContentAssistant(text, fCurrPackageCompletionProcessor);
		TextFieldNavigationHandler.install(text);

		return res;
	}

	public void setPackageFragment(IPackageFragment fragment) {
		if (fragment != null) {
			setText(fragment.getElementName());
		}

		updateListener();
	}

	private void updateListener() {
		if (fUpdateListener != null) {
			fUpdateListener.dialogFieldChanged(this);
		}
	}

	public IPackageFragment getPackageFragment() {
		return calculateFragment(fDialogField.getSelectedFragmentRoot());
	}

	private IPackageFragment calculateFragment(IPackageFragmentRoot root) {
		if (root == null) {
			return null;
		} else {
			return root.getPackageFragment(getText());
		}
	}

	public void sourceRootChanged(IPackageFragmentRoot newRoot) {
		fCurrPackageCompletionProcessor.setPackageFragmentRoot(newRoot);

		setPackageFragment(calculateFragment(newRoot));
	}
}
