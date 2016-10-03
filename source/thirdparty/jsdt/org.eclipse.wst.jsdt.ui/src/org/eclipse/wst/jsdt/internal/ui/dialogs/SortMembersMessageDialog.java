/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.MembersOrderPreferencePage;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

public class SortMembersMessageDialog extends OptionalMessageDialog {
	
	private final static String OPTIONAL_ID= "SortMembersMessageDialog.optionalDialog.id"; //$NON-NLS-1$
	
	private final static String DIALOG_SETTINGS_SORT_ALL= "SortMembers.sort_all"; //$NON-NLS-1$
	
	private SelectionButtonDialogField fNotSortAllRadio;
	private SelectionButtonDialogField fSortAllRadio;

	private final IDialogSettings fDialogSettings;
	
	public SortMembersMessageDialog(Shell parentShell) {
		super(OPTIONAL_ID, parentShell, DialogsMessages.SortMembersMessageDialog_dialog_title, null, new String(), INFORMATION, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
		
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fDialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		
		boolean isSortAll= fDialogSettings.getBoolean(DIALOG_SETTINGS_SORT_ALL);
		
		fNotSortAllRadio= new SelectionButtonDialogField(SWT.RADIO);
		fNotSortAllRadio.setLabelText(DialogsMessages.SortMembersMessageDialog_do_not_sort_fields_label);
		fNotSortAllRadio.setSelection(!isSortAll);
		
		fSortAllRadio= new SelectionButtonDialogField(SWT.RADIO);
		fSortAllRadio.setLabelText(DialogsMessages.SortMembersMessageDialog_sort_all_label);
		fSortAllRadio.setSelection(isSortAll);
	}
		
	private Control createLinkControl(Composite composite) {
		Link link= new Link(composite, SWT.WRAP | SWT.RIGHT);
		link.setText(DialogsMessages.SortMembersMessageDialog_description); 
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openCodeTempatePage(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID);
			}
		});
		link.setToolTipText(DialogsMessages.SortMembersMessageDialog_link_tooltip); 
		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, true, false);
		gridData.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);//convertWidthInCharsToPixels(60);
		link.setLayoutData(gridData);
		link.setFont(composite.getFont());
		
		return link;
	}
	
	protected void openCodeTempatePage(String id) {
		PreferencesUtil.createPreferenceDialogOn(getShell(), MembersOrderPreferencePage.PREF_ID, null, null).open();
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.IconAndMessageDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control contents= super.createContents(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJavaHelpContextIds.SORT_MEMBERS_DIALOG);
		return contents;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IconAndMessageDialog#createMessageArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createMessageArea(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite messageComposite= new Composite(parent, SWT.NONE);
		messageComposite.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		messageComposite.setLayout(layout);
		messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createLinkControl(messageComposite);
				
		int indent= convertWidthInCharsToPixels(3);
		
		fNotSortAllRadio.doFillIntoGrid(messageComposite, 1);
		LayoutUtil.setHorizontalIndent(fNotSortAllRadio.getSelectionButton(null), indent);
		
		fSortAllRadio.doFillIntoGrid(messageComposite, 1);
		LayoutUtil.setHorizontalIndent(fSortAllRadio.getSelectionButton(null), indent);
		
		final Composite warningComposite= new Composite(messageComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		warningComposite.setLayout(layout);
		warningComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		warningComposite.setFont(messageComposite.getFont());
		
		Image image= Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
		final Label imageLabel1= new Label(warningComposite, SWT.LEFT | SWT.WRAP);
		imageLabel1.setImage(image);
		imageLabel1.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 1, 1));
		
		final Label label= new Label(warningComposite, SWT.WRAP);
		label.setText(DialogsMessages.SortMembersMessageDialog_sort_warning_label);
		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1);
		gridData.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(gridData);
		label.setFont(warningComposite.getFont());
		
		fNotSortAllRadio.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				imageLabel1.setEnabled(!fNotSortAllRadio.isSelected());
				label.setEnabled(!fNotSortAllRadio.isSelected());
			}
		});
		imageLabel1.setEnabled(!fNotSortAllRadio.isSelected());
		label.setEnabled(!fNotSortAllRadio.isSelected());
		
		return messageComposite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		if (isDialogEnabled(OPTIONAL_ID)) {
			int res= super.open();
			if (res != Window.OK) {
				setDialogEnabled(OPTIONAL_ID, true); // don't save state on cancel
			}
			return res;
		}
		return Window.OK;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#close()
	 */
	public boolean close() {
		fDialogSettings.put(DIALOG_SETTINGS_SORT_ALL, fSortAllRadio.isSelected());
		return super.close();
	}
	

	public boolean isNotSortingFieldsEnabled() {
		return fNotSortAllRadio.isSelected();
	}

}
