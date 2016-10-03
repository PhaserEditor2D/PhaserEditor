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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;

public class ParameterEditDialog extends StatusDialog {
	
	private final ParameterInfo fParameter;
	private final boolean fEditType;
	private final boolean fEditDefault;
	private final StubTypeContext fContext;
	private Text fType;
	private Text fName;
	private Text fDefaultValue;
	
	/**
	 * @param context the <code>IPackageFragment</code> for type ContentAssist.
	 * Can be <code>null</code> if <code>canEditType</code> is <code>false</code>.
	 */
	public ParameterEditDialog(Shell parentShell, ParameterInfo parameter, boolean canEditType, boolean canEditDefault, StubTypeContext context) {
		super(parentShell);
		fParameter= parameter;
		fEditType= canEditType;
		fEditDefault= canEditDefault;
		fContext= context;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(RefactoringMessages.ParameterEditDialog_title); 
	}

	protected Control createDialogArea(Composite parent) {
		Composite result= (Composite)super.createDialogArea(parent);
		GridLayout layout= (GridLayout)result.getLayout();
		layout.numColumns= 2;
		Label label;
		GridData gd;
		
		label= new Label(result, SWT.NONE);
		String newName = fParameter.getNewName();
		if (newName.length() == 0)
			label.setText(RefactoringMessages.ParameterEditDialog_message_new); 
		else
			label.setText(Messages.format(RefactoringMessages.ParameterEditDialog_message, newName)); 
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
		
		if (fEditType) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.ParameterEditDialog_type); 
			fType= new Text(result, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			fType.setLayoutData(gd);
			fType.setText(fParameter.getNewTypeName());
			fType.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validate((Text)e.widget);
					}
				});
			TextFieldNavigationHandler.install(fType);
			JavaTypeCompletionProcessor processor= new JavaTypeCompletionProcessor(true, false);
			processor.setCompletionContext(fContext.getCuHandle(), fContext.getBeforeString(), fContext.getAfterString());
			ControlContentAssistHelper.createTextContentAssistant(fType, processor);
		}

		label= new Label(result, SWT.NONE);
		fName= new Text(result, SWT.BORDER);
		initializeDialogUnits(fName);
		label.setText(RefactoringMessages.ParameterEditDialog_name); 
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(45);
		fName.setLayoutData(gd);
		fName.setText(newName);
		fName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validate((Text)e.widget);
				}
			});
		TextFieldNavigationHandler.install(fName);

		if (fEditDefault && fParameter.isAdded()) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.ParameterEditDialog_defaultValue); 
			fDefaultValue= new Text(result, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			fDefaultValue.setLayoutData(gd);
			fDefaultValue.setText(fParameter.getDefaultValue());
			fDefaultValue.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validate((Text)e.widget);
					}
				});
			TextFieldNavigationHandler.install(fDefaultValue);
		}
		applyDialogFont(result);		
		return result;
	}
	
	protected void okPressed() {
		if (fType != null) {
			fParameter.setNewTypeName(fType.getText());
		}
		fParameter.setNewName(fName.getText());
		if (fDefaultValue != null) {
			fParameter.setDefaultValue(fDefaultValue.getText());
		}
		super.okPressed();
	}
	
	private void validate(Text first) {
		IStatus[] result= new IStatus[3];
		if (first == fType) {
			result[0]= validateType();
			result[1]= validateName();
			result[2]= validateDefaultValue();
		} else if (first == fName) {
			result[0]= validateName();
			result[1]= validateType();
			result[2]= validateDefaultValue();
		} else {
			result[0]= validateDefaultValue();
			result[1]= validateName();
			result[2]= validateType();
		}
		for (int i= 0; i < result.length; i++) {
			IStatus status= result[i];
			if (status != null && !status.isOK()) {
				updateStatus(status);
				return;
			}
		}
		updateStatus(createOkStatus());
	}
	
	private IStatus validateType() {
		if (fType == null)
			return null;
		String type= fType.getText();
		
		RefactoringStatus status= TypeContextChecker.checkParameterTypeSyntax(type, fContext.getCuHandle().getJavaScriptProject());
		if (status == null || status.isOK())
			return createOkStatus();
		if (status.hasError())
			return createErrorStatus(status.getEntryWithHighestSeverity().getMessage());
		else
			return createWarningStatus(status.getEntryWithHighestSeverity().getMessage());
	}
	
	private IStatus validateName() {
		if (fName == null) 
			return null;
		String text= fName.getText();
		if (text.length() == 0)
			return createErrorStatus(RefactoringMessages.ParameterEditDialog_name_error);
		IStatus status= JavaScriptConventions.validateFieldName(text);
		if (status.matches(IStatus.ERROR))
			return status;
		if (! Checks.startsWithLowerCase(text))
			return createWarningStatus(RefactoringCoreMessages.ExtractTempRefactoring_convention); 
		return createOkStatus();
	}
	
	private IStatus validateDefaultValue() {
		if (fDefaultValue == null)
			return null;
		String defaultValue= fDefaultValue.getText();
		if (defaultValue.length() == 0)
			return createErrorStatus(RefactoringMessages.ParameterEditDialog_defaultValue_error);
		if (ChangeSignatureRefactoring.isValidExpression(defaultValue))
			return createOkStatus();
		String msg= Messages.format(RefactoringMessages.ParameterEditDialog_defaultValue_invalid, new String[]{defaultValue}); 
		return createErrorStatus(msg);
		
	}
	
	private Status createOkStatus() {
		return new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	private Status createWarningStatus(String message) {
		return new Status(IStatus.WARNING, JavaScriptPlugin.getPluginId(), IStatus.WARNING, message, null);
	}
	
	private Status createErrorStatus(String message) {
		return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, message, null);
	}
}
