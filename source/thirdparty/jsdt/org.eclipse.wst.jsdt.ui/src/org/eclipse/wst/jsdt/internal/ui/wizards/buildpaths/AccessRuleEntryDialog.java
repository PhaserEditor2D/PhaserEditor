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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;

public class AccessRuleEntryDialog extends StatusDialog {
	
	private StringDialogField fPatternDialog;
	private StatusInfo fPatternStatus;
	
	private String fPattern;
	private ComboDialogField fRuleKindCombo;
	private int[] fRuleKinds;
		
	public AccessRuleEntryDialog(Shell parent, IAccessRule ruleToEdit, CPListElement entryToEdit) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		String title, message;
		if (ruleToEdit == null) {
			title= NewWizardMessages.TypeRestrictionEntryDialog_add_title; 
		} else {
			title= NewWizardMessages.TypeRestrictionEntryDialog_edit_title; 
		}
		message= Messages.format(NewWizardMessages.TypeRestrictionEntryDialog_pattern_label, entryToEdit.getPath().makeRelative().toString());  
		setTitle(title);
		
		fPatternStatus= new StatusInfo();
		
		TypeRulesAdapter adapter= new TypeRulesAdapter();
		fPatternDialog= new StringDialogField();
		fPatternDialog.setLabelText(message);
		fPatternDialog.setDialogFieldListener(adapter);
		
		fRuleKindCombo= new ComboDialogField(SWT.READ_ONLY);
		fRuleKindCombo.setLabelText(NewWizardMessages.TypeRestrictionEntryDialog_kind_label); 
		fRuleKindCombo.setDialogFieldListener(adapter);
		String[] items= {
				NewWizardMessages.TypeRestrictionEntryDialog_kind_non_accessible, 
				NewWizardMessages.TypeRestrictionEntryDialog_kind_discourraged, 
				NewWizardMessages.TypeRestrictionEntryDialog_kind_accessible
		};
		fRuleKinds= new int[] {
				IAccessRule.K_NON_ACCESSIBLE,
				IAccessRule.K_DISCOURAGED,
				IAccessRule.K_ACCESSIBLE
		};
		fRuleKindCombo.setItems(items);
		
		
		if (ruleToEdit == null) {
			fPatternDialog.setText(""); //$NON-NLS-1$
			fRuleKindCombo.selectItem(0);
		} else {
			fPatternDialog.setText(ruleToEdit.getPattern().toString());
			for (int i= 0; i < fRuleKinds.length; i++) {
				if (fRuleKinds[i] == ruleToEdit.getKind()) {
					fRuleKindCombo.selectItem(i);
					break;
				}
			}
		}
	}
	
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
				
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		
		Label description= new Label(inner, SWT.WRAP);
		description.setText(NewWizardMessages.TypeRestrictionEntryDialog_description); 

		GridData gd= new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		gd.widthHint= convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
		
		fRuleKindCombo.doFillIntoGrid(inner, 2);
		fPatternDialog.doFillIntoGrid(inner, 2);
				
		Label description2= new Label(inner, SWT.WRAP);
		description2.setText(NewWizardMessages.TypeRestrictionEntryDialog_description2); 

		gd= new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		gd.widthHint= convertWidthInCharsToPixels(60);
		description2.setLayoutData(gd);
		
		fPatternDialog.postSetFocusOnDialogField(parent.getDisplay());
		applyDialogFont(composite);		
		return composite;
	}

		
	// -------- TypeRulesAdapter --------

	private class TypeRulesAdapter implements IDialogFieldListener {
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}
	}
	

	protected void doStatusLineUpdate() {
		checkIfPatternValid();
		updateStatus(fPatternStatus);
	}		
	
	protected void checkIfPatternValid() {
		String pattern= fPatternDialog.getText().trim();
		if (pattern.length() == 0) {
			fPatternStatus.setError(NewWizardMessages.TypeRestrictionEntryDialog_error_empty); 
			return;
		}
		IPath path= new Path(pattern);
		if (path.isAbsolute() || path.getDevice() != null) {
			fPatternStatus.setError(NewWizardMessages.TypeRestrictionEntryDialog_error_notrelative); 
			return;
		}
		
		fPattern= pattern; 
		fPatternStatus.setOK();
	}
	
	public IAccessRule getRule() {
		IPath filePattern= new Path(fPattern);
		int kind= fRuleKinds[fRuleKindCombo.getSelectionIndex()];
		return JavaScriptCore.newAccessRule(filePattern, kind);
	}
	
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.ACCESS_RULES_DIALOG);
	}
}
