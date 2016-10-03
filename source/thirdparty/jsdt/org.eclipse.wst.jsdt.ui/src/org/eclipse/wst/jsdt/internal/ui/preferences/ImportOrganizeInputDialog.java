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
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.preferences.ImportOrganizeConfigurationBlock.ImportOrderEntry;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.wst.jsdt.ui.IJavaScriptElementSearchConstants;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Dialog to enter a new package entry in the organize import preference page.
 */
public class ImportOrganizeInputDialog extends StatusDialog {
	
	private class ImportOrganizeInputAdapter implements IDialogFieldListener, IStringButtonAdapter {
		/**
		 * @see IDialogFieldListener#dialogFieldChanged(DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			doDialogFieldChanged(field);
		}			

		/**
		 * @see IStringButtonAdapter#changeControlPressed(DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			doBrowsePackages();
		}
	}
	
	private StringButtonDialogField fNameDialogField;
	private SelectionButtonDialogField fBrowseTypeButton;
	private List fExistingEntries;
	private final boolean fIsStatic;
		
	public ImportOrganizeInputDialog(Shell parent, List/*<ImportOrderEntry>*/ existingEntries, boolean isStatic) {
		super(parent);
		fIsStatic= isStatic;
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fExistingEntries= existingEntries;
		
		String label, title;
		if (isStatic) {
			title= PreferencesMessages.ImportOrganizeInputDialog_title_static; 
			label= PreferencesMessages.ImportOrganizeInputDialog_name_group_static_label; 
		} else {
			title= PreferencesMessages.ImportOrganizeInputDialog_title; 
			label= PreferencesMessages.ImportOrganizeInputDialog_name_group_label; 
		}
		setTitle(title);

		ImportOrganizeInputAdapter adapter= new ImportOrganizeInputAdapter();

		fNameDialogField= new StringButtonDialogField(adapter);
		fNameDialogField.setLabelText(label); 
		fNameDialogField.setButtonLabel(PreferencesMessages.ImportOrganizeInputDialog_browse_packages_button); 
		fNameDialogField.setDialogFieldListener(adapter);
		fNameDialogField.setText(""); //$NON-NLS-1$
		
		fBrowseTypeButton= new SelectionButtonDialogField(SWT.PUSH);
		fBrowseTypeButton.setLabelText(PreferencesMessages.ImportOrganizeInputDialog_browse_types_label); 
		fBrowseTypeButton.setDialogFieldListener(adapter);
	}
		
	public void setInitialSelection(ImportOrderEntry editedEntry) {
		Assert.isNotNull(editedEntry);
		if (editedEntry.name.length() == 0) {
			fNameDialogField.setText(""); //$NON-NLS-1$
		} else {
			fNameDialogField.setText(editedEntry.name);
		}
	}
	
	public ImportOrderEntry getResult() {
		String val= fNameDialogField.getText();
		if ("*".equals(val)) { //$NON-NLS-1$
			return new ImportOrderEntry("", fIsStatic); //$NON-NLS-1$
		} else {
			return new ImportOrderEntry(val, fIsStatic);
		}
	}
		
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		initializeDialogUnits(parent);
		
		GridLayout layout= (GridLayout) composite.getLayout();
		layout.numColumns= 2;
		
		fNameDialogField.doFillIntoGrid(composite, 3);
		
		LayoutUtil.setHorizontalSpan(fNameDialogField.getLabelControl(null), 2);
		
		int fieldWidthHint= convertWidthInCharsToPixels(60);
		Text text= fNameDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, fieldWidthHint);
		LayoutUtil.setHorizontalGrabbing(text);
		TextFieldNavigationHandler.install(text);
		
		DialogField.createEmptySpace(composite, 1);
		fBrowseTypeButton.doFillIntoGrid(composite, 1);
		
		fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
		
		applyDialogFont(composite);		
		return composite;
	}
	
	final void doBrowsePackages() {
		IRunnableContext context= new BusyIndicatorRunnableContext();
		IJavaScriptSearchScope scope= SearchEngine.createWorkspaceScope();
		int style= PackageSelectionDialog.F_REMOVE_DUPLICATES | PackageSelectionDialog.F_SHOW_PARENTS | PackageSelectionDialog.F_HIDE_DEFAULT_PACKAGE;
		PackageSelectionDialog dialog= new PackageSelectionDialog(getShell(), context, style, scope);
		dialog.setFilter(fNameDialogField.getText());
		dialog.setIgnoreCase(false);
		dialog.setTitle(PreferencesMessages.ImportOrganizeInputDialog_ChoosePackageDialog_title); 
		dialog.setMessage(PreferencesMessages.ImportOrganizeInputDialog_ChoosePackageDialog_description); 
		dialog.setEmptyListMessage(PreferencesMessages.ImportOrganizeInputDialog_ChoosePackageDialog_empty); 
		if (dialog.open() == Window.OK) {
			IPackageFragment res= (IPackageFragment) dialog.getFirstResult();
			fNameDialogField.setText(res.getElementName());
		}
	}
	
	private void doBrowseTypes() {		
		IRunnableContext context= new BusyIndicatorRunnableContext();
		IJavaScriptSearchScope scope= SearchEngine.createWorkspaceScope();
		int style= IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES;
		try {
			SelectionDialog dialog= JavaScriptUI.createTypeDialog(getShell(), context, scope, style, false, fNameDialogField.getText());
			dialog.setTitle(PreferencesMessages.ImportOrganizeInputDialog_ChooseTypeDialog_title); 
			dialog.setMessage(PreferencesMessages.ImportOrganizeInputDialog_ChooseTypeDialog_description); 
			if (dialog.open() == Window.OK) {
				IType res= (IType) dialog.getResult()[0];
				fNameDialogField.setText(res.getFullyQualifiedName('.'));
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), PreferencesMessages.ImportOrganizeInputDialog_ChooseTypeDialog_title, PreferencesMessages.ImportOrganizeInputDialog_ChooseTypeDialog_error_message);  
		}
	}
	
	/**
	 * @param field
	 */
	final void doDialogFieldChanged(DialogField field) {
		if (field == fBrowseTypeButton) {
			doBrowseTypes();
		} else {
			doValidation();
		}
	}
	
	
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String newText= fNameDialogField.getText();
		if (newText.length() == 0) {
			status.setError(""); //$NON-NLS-1$
		} else {
			if (newText.equals("*")) { //$NON-NLS-1$
				if (doesExist("", fIsStatic)) { //$NON-NLS-1$
					status.setError(PreferencesMessages.ImportOrganizeInputDialog_error_entryExists); 
				}
			} else {
				IStatus val= JavaScriptConventions.validateJavaScriptTypeName(newText, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3);
				if (val.matches(IStatus.ERROR)) {
					status.setError(PreferencesMessages.ImportOrganizeInputDialog_error_invalidName); 
				} else {
					if (doesExist(newText, fIsStatic)) {
						status.setError(PreferencesMessages.ImportOrganizeInputDialog_error_entryExists); 
					}
				}
			}
		}
		updateStatus(status);
	}
	
	private boolean doesExist(String name, boolean isStatic) {
		for (int i= 0; i < fExistingEntries.size(); i++) {
			ImportOrderEntry entry= (ImportOrderEntry) fExistingEntries.get(i);
			if (name.equals(entry.name) && isStatic == entry.isStatic) {
				return true;
			}
		}
		return false;
	}
	

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.IMPORT_ORGANIZE_INPUT_DIALOG);
	}


}
