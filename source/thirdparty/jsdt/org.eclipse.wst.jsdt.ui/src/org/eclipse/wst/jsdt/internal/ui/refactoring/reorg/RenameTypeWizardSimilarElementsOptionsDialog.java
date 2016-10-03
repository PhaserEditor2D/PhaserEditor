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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * Option dialog for selecting a similarly named element renaming strategy
 * 
 * 
 * 
 */
public class RenameTypeWizardSimilarElementsOptionsDialog extends MessageDialog {

	private SelectionButtonDialogField fExactStrategyRadio;
	private SelectionButtonDialogField fEmbeddedStrategyRadio;
	private SelectionButtonDialogField fSuffixStrategyRadio;

	private Label fWarningLabel;
	private Label fWarningImageLabel;
	private int fSelectedStrategy;

	public RenameTypeWizardSimilarElementsOptionsDialog(Shell parentShell, int defaultStrategy) {
		super(parentShell, RefactoringMessages.RenameTypeWizardSimilarElementsOptionsDialog_title, null, new String(), INFORMATION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fSelectedStrategy= defaultStrategy;
	}

	/*
	 * (non-Javadoc)
	 * 
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

		Label infoLabel= new Label(messageComposite, SWT.WRAP);
		infoLabel.setText(RefactoringMessages.RenameTypeWizardSimilarElementsOptionsDialog_select_strategy);
		GridData gd= new GridData(GridData.FILL, GridData.CENTER, true, false);
		gd.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		infoLabel.setLayoutData(gd);
		infoLabel.setFont(parent.getFont());

		int indent= convertWidthInCharsToPixels(3);

		fExactStrategyRadio= new SelectionButtonDialogField(SWT.RADIO);
		fExactStrategyRadio.setLabelText(RefactoringMessages.RenameTypeWizardSimilarElementsOptionsDialog_strategy_1);
		fExactStrategyRadio.doFillIntoGrid(messageComposite, 1);
		fExactStrategyRadio.setSelection(fSelectedStrategy == RenamingNameSuggestor.STRATEGY_EXACT);
		LayoutUtil.setHorizontalIndent(fExactStrategyRadio.getSelectionButton(null), indent);

		fEmbeddedStrategyRadio= new SelectionButtonDialogField(SWT.RADIO);
		fEmbeddedStrategyRadio.setLabelText(RefactoringMessages.RenameTypeWizardSimilarElementsOptionsDialog_strategy_2);
		fEmbeddedStrategyRadio.doFillIntoGrid(messageComposite, 1);
		fEmbeddedStrategyRadio.setSelection(fSelectedStrategy == RenamingNameSuggestor.STRATEGY_EMBEDDED);
		LayoutUtil.setHorizontalIndent(fEmbeddedStrategyRadio.getSelectionButton(null), indent);

		fSuffixStrategyRadio= new SelectionButtonDialogField(SWT.RADIO);
		fSuffixStrategyRadio.setLabelText(RefactoringMessages.RenameTypeWizardSimilarElementsOptionsDialog_strategy_3);
		fSuffixStrategyRadio.doFillIntoGrid(messageComposite, 1);
		fSuffixStrategyRadio.setSelection(fSelectedStrategy == RenamingNameSuggestor.STRATEGY_SUFFIX);
		LayoutUtil.setHorizontalIndent(fSuffixStrategyRadio.getSelectionButton(null), indent);

		final Composite warningComposite= new Composite(messageComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		warningComposite.setLayout(layout);
		warningComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		warningComposite.setFont(messageComposite.getFont());

		Image image= Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
		fWarningImageLabel= new Label(warningComposite, SWT.LEFT | SWT.WRAP);
		fWarningImageLabel.setImage(image);
		fWarningImageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 1, 1));

		fWarningLabel= new Label(warningComposite, SWT.WRAP);
		fWarningLabel.setText(RefactoringMessages.RenameTypeWizardSimilarElementsOptionsDialog_warning_short_names);
		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1);
		gridData.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		fWarningLabel.setLayoutData(gridData);
		fWarningLabel.setFont(warningComposite.getFont());

		fExactStrategyRadio.setDialogFieldListener(new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				updateLabel();
				fSelectedStrategy= RenamingNameSuggestor.STRATEGY_EXACT;
			}
		});

		fEmbeddedStrategyRadio.setDialogFieldListener(new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				updateLabel();
				fSelectedStrategy= RenamingNameSuggestor.STRATEGY_EMBEDDED;
			}
		});

		fSuffixStrategyRadio.setDialogFieldListener(new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				updateLabel();
				fSelectedStrategy= RenamingNameSuggestor.STRATEGY_SUFFIX;
			}
		});

		updateLabel();

		return messageComposite;
	}
	
	
	protected boolean customShouldTakeFocus() {
		return true;
	}

	private void updateLabel() {
		fWarningImageLabel.setEnabled(!fExactStrategyRadio.isSelected());
		fWarningLabel.setEnabled(!fExactStrategyRadio.isSelected());
	}

	/**
	 * @return one of the STRATEGY_* constants in {@link RenamingNameSuggestor}
	 */
	public int getSelectedStrategy() {
		return fSelectedStrategy;
	}
}
