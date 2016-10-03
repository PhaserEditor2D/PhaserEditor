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
package org.eclipse.wst.jsdt.internal.ui.refactoring.code;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ReplaceInvocationsRefactoring;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;

public class ReplaceInvocationsInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ReplaceInvocationsInputPage";//$NON-NLS-1$

	private ReplaceInvocationsRefactoring fRefactoring;

	private static final long LABEL_FLAGS= JavaScriptElementLabels.M_PRE_TYPE_PARAMETERS | JavaScriptElementLabels.M_PRE_RETURNTYPE | JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_PARAMETER_NAMES | JavaScriptElementLabels.M_EXCEPTIONS;
	
	public ReplaceInvocationsInputPage() {
		super(PAGE_NAME);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		fRefactoring= (ReplaceInvocationsRefactoring) getRefactoring();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);

		createMethodSignature(result);
		
		Label separator= new Label(parent, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.heightHint= 5;
		separator.setLayoutData(gridData);
		
		Label bodyLabel= new Label(result, SWT.NONE);
		bodyLabel.setText(RefactoringMessages.ReplaceInvocationsInputPage_replaceInvocationsBy);
		
		createBody(result);
		
		Button replaceAll= new Button(result, SWT.CHECK);
		replaceAll.setText(RefactoringMessages.ReplaceInvocationsInputPage_replaceAll);
		boolean canSingle= fRefactoring.canReplaceSingle();
//		replaceAll.setEnabled(canSingle);
		replaceAll.setEnabled(false); // does not work for now...
		replaceAll.setSelection(! canSingle);
		replaceAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				boolean all= ((Button) event.widget).getSelection();
				changeMode(all ? ReplaceInvocationsRefactoring.Mode.REPLACE_ALL : ReplaceInvocationsRefactoring.Mode.REPLACE_SINGLE);
			}
		});
		
		Dialog.applyDialogFont(result);
	}

	private void createMethodSignature(Composite parent) {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		JavaSourceViewer signatureViewer= new JavaSourceViewer(parent, null, null, false, SWT.READ_ONLY | SWT.WRAP /*| SWT.BORDER*/, store);
		signatureViewer.configure(new JavaScriptSourceViewerConfiguration(JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		signatureViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		signatureViewer.getTextWidget().setBackground(parent.getBackground());
		String signatureLabel= JavaScriptElementLabels.getElementLabel(fRefactoring.getMethod(), LABEL_FLAGS);
		signatureViewer.setDocument(new Document(signatureLabel));
		signatureViewer.setEditable(false);
		
		Control signatureControl= signatureViewer.getControl();
		PixelConverter pixelConverter= new PixelConverter(signatureControl);
		GridData gdata= new GridData(GridData.FILL_HORIZONTAL);
		gdata.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
		signatureControl.setLayoutData(gdata);
	}

	private void createBody(Composite parent) {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		JavaSourceViewer bodyEditor= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.WRAP | SWT.BORDER, store);
		bodyEditor.configure(new JavaScriptSourceViewerConfiguration(JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		bodyEditor.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		Document bodyDocument= new Document(getInitialBody());
		bodyEditor.setDocument(bodyDocument);
		bodyEditor.setEditable(true);
		
		Control bodyControl= bodyEditor.getControl();
		PixelConverter pixelConverter= new PixelConverter(bodyControl);
		GridData gdata= new GridData(GridData.FILL_BOTH);
		gdata.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
		gdata.minimumHeight= pixelConverter.convertHeightInCharsToPixels(5);
		bodyControl.setLayoutData(gdata);
		bodyControl.setFocus();
		
		bodyDocument.addDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
			public void documentChanged(DocumentEvent event) {
				try {
					fRefactoring.setBody(event.getDocument().get(), fRefactoring.getMethod().getParameterNames());
				} catch (JavaScriptModelException ex) {
					// TODO Auto-generated catch block
					JavaScriptPlugin.log(ex);
				}
			}
		});
	}
	
	private String getInitialBody() {
		//TODO
		return ""; //$NON-NLS-1$
		
	}

	private void changeMode(ReplaceInvocationsRefactoring.Mode mode) {
		RefactoringStatus status;
		try {
			status= fRefactoring.setCurrentMode(mode);
		} catch (JavaScriptModelException e) {
			status= RefactoringStatus.createFatalErrorStatus(e.getMessage());
		}
		setPageComplete(status);
	}	
}
