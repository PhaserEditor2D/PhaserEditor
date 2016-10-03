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
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

/**
 * Dialog to enter a new entry in the type filter preference page.
 */
public class TypeFilterInputDialog extends StatusDialog {
	
	private class TypeFilterInputAdapter implements IDialogFieldListener, IStringButtonAdapter {
		/*
		 * @see IDialogFieldListener#dialogFieldChanged(DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			doValidation();
		}			
		/*
		 * @see IStringButtonAdapter#changeControlPressed(DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			doButtonPressed();
		}
	}
	
	private StringButtonDialogField fNameDialogField;
	private List fExistingEntries;
		
	public TypeFilterInputDialog(Shell parent, List existingEntries) {
		super(parent);
		
		fExistingEntries= existingEntries;
		
		setTitle(PreferencesMessages.TypeFilterInputDialog_title); 

		TypeFilterInputAdapter adapter= new TypeFilterInputAdapter();

		fNameDialogField= new StringButtonDialogField(adapter);
		fNameDialogField.setLabelText(PreferencesMessages.TypeFilterInputDialog_message); 
		fNameDialogField.setButtonLabel(PreferencesMessages.TypeFilterInputDialog_browse_button); 
		fNameDialogField.setDialogFieldListener(adapter);
		
		fNameDialogField.setText("");		 //$NON-NLS-1$
	}
	
	public void setInitialString(String input) {
		Assert.isNotNull(input);
		fNameDialogField.setText(input);
	}
	
	public Object getResult() {
		return fNameDialogField.getText();
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		LayoutUtil.doDefaultLayout(inner, new DialogField[] { fNameDialogField }, true, 0, 0);
		
		int fieldWidthHint= convertWidthInCharsToPixels(60);
		Text text= fNameDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, fieldWidthHint);
		LayoutUtil.setHorizontalGrabbing(text);
		TextFieldNavigationHandler.install(text);
		
		fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
		
		applyDialogFont(composite);		
		return composite;
	}
	
	private void doButtonPressed() {
		IJavaScriptSearchScope scope= SearchEngine.createWorkspaceScope();
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		int flags= PackageSelectionDialog.F_SHOW_PARENTS | PackageSelectionDialog.F_HIDE_DEFAULT_PACKAGE | PackageSelectionDialog.F_REMOVE_DUPLICATES;
		PackageSelectionDialog dialog = new PackageSelectionDialog(getShell(), context, flags , scope);
		dialog.setTitle(PreferencesMessages.TypeFilterInputDialog_choosepackage_label); 
		dialog.setMessage(PreferencesMessages.TypeFilterInputDialog_choosepackage_description); 
		dialog.setMultipleSelection(false);
		dialog.setFilter(fNameDialogField.getText());
		if (dialog.open() == IDialogConstants.OK_ID) {
			IPackageFragment res= (IPackageFragment) dialog.getFirstResult();
			fNameDialogField.setText(res.getElementName() + "*"); //$NON-NLS-1$
		}
	}
	
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String newText= fNameDialogField.getText();
		if (newText.length() == 0) {
			status.setError(PreferencesMessages.TypeFilterInputDialog_error_enterName); 
		} else {
			newText= newText.replace('*', 'X').replace('?', 'Y');
			IStatus val= JavaScriptConventions.validatePackageName(newText, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3);
			if (val.matches(IStatus.ERROR)) {
				status.setError(Messages.format(PreferencesMessages.TypeFilterInputDialog_error_invalidName, val.getMessage())); 
			} else {
				if (fExistingEntries.contains(newText)) {
					status.setError(PreferencesMessages.TypeFilterInputDialog_error_entryExists); 
				}
			}
		}
		updateStatus(status);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.IMPORT_ORGANIZE_INPUT_DIALOG);
	}
}
