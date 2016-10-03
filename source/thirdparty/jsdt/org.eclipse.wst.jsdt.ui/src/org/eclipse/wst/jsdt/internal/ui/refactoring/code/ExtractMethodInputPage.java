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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.refactoring.ChangeParametersControl;
import org.eclipse.wst.jsdt.internal.ui.refactoring.IParameterListChangeListener;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.util.RowLayouter;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;

public class ExtractMethodInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ExtractMethodInputPage";//$NON-NLS-1$

	private ExtractMethodRefactoring fRefactoring;
	private Text fTextField;
	private boolean fFirstTime;
	private JavaSourceViewer fSignaturePreview;
	private Document fSignaturePreviewDocument;
	private IDialogSettings fSettings;
	
	private static final String DESCRIPTION = RefactoringMessages.ExtractMethodInputPage_description;
	private static final String THROW_RUNTIME_EXCEPTIONS= "ThrowRuntimeExceptions"; //$NON-NLS-1$
	private static final String GENERATE_JAVADOC= "GenerateJavadoc";  //$NON-NLS-1$

	public ExtractMethodInputPage() {
		super(PAGE_NAME);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
		setDescription(DESCRIPTION);
		fFirstTime= true;
		fSignaturePreviewDocument= new Document();
	}

	public void createControl(Composite parent) {
		fRefactoring= (ExtractMethodRefactoring)getRefactoring();
		loadSettings();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		GridData gd= null;
		
		initializeDialogUnits(result);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(getLabelText());
		
		fTextField= createTextInputField(result, SWT.BORDER);
		fTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		layouter.perform(label, fTextField, 1);
		
		ASTNode[] destinations= fRefactoring.getDestinations();
		if (destinations.length > 1) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.ExtractMethodInputPage_destination_type); 
			final Combo combo= new Combo(result, SWT.READ_ONLY | SWT.DROP_DOWN);
			for (int i= 0; i < destinations.length; i++) {
				ASTNode declaration= destinations[i];
				combo.add(getLabel(declaration));
			}
			combo.select(0);
			combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			combo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setDestination(combo.getSelectionIndex());
				}
				public void widgetDefaultSelected(SelectionEvent e) {
					// nothing
				}
			});
		}
		
//		label= new Label(result, SWT.NONE);
//		label.setText(RefactoringMessages.ExtractMethodInputPage_access_Modifiers); 
//		
//		Composite group= new Composite(result, SWT.NONE);
//		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		layout= new GridLayout();
//		layout.numColumns= 4; layout.marginWidth= 0;
//		group.setLayout(layout);
//		
//		String[] labels= new String[] {
//			RefactoringMessages.ExtractMethodInputPage_public,  
//			RefactoringMessages.ExtractMethodInputPage_protected, 
//			RefactoringMessages.ExtractMethodInputPage_default, 
//			RefactoringMessages.ExtractMethodInputPage_private
//		};
//		Integer[] data= Integer.valueOf[] {Integer.valueOf(Modifier.PUBLIC), Integer.valueOf(Modifier.PROTECTED), Integer.valueOf(Modifier.NONE), Integer.valueOf(Modifier.PRIVATE)};
//		Integer visibility= Integer.valueOf(fRefactoring.getVisibility());
//		for (int i= 0; i < labels.length; i++) {
//			Button radio= new Button(group, SWT.RADIO);
//			radio.setText(labels[i]);
//			radio.setData(data[i]);
//			if (data[i].equals(visibility))
//				radio.setSelection(true);
//			radio.addSelectionListener(new SelectionAdapter() {
//				public void widgetSelected(SelectionEvent event) {
//					setVisibility((Integer)event.widget.getData());
//				}
//			});
//		}
//		layouter.perform(label, group, 1);
		
		if (!fRefactoring.getParameterInfos().isEmpty()) {
			ChangeParametersControl cp= new ChangeParametersControl(result, SWT.NONE, 
				RefactoringMessages.ExtractMethodInputPage_parameters, 
				new IParameterListChangeListener() {
				public void parameterChanged(ParameterInfo parameter) {
					parameterModified();
				}
				public void parameterListChanged() {
					parameterModified();
				}
				public void parameterAdded(ParameterInfo parameter) {
					updatePreview(getText());
				}
			}, ChangeParametersControl.Mode.EXTRACT_METHOD);
			gd= new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan= 2;
			cp.setLayoutData(gd);
			cp.setInput(fRefactoring.getParameterInfos());
		}
		
//		Button checkBox= new Button(result, SWT.CHECK);
//		checkBox.setText(RefactoringMessages.ExtractMethodInputPage_throwRuntimeExceptions); 
//		checkBox.setSelection(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
//		checkBox.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				setRethrowRuntimeException(((Button)e.widget).getSelection());
//			}
//		});
//		layouter.perform(checkBox);
//		
		Button checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.ExtractMethodInputPage_generateJavadocComment); 
		boolean generate= computeGenerateJavadoc();
		setGenerateJavadoc(generate);
		checkBox.setSelection(generate);
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setGenerateJavadoc(((Button)e.widget).getSelection());
			}
		});
		layouter.perform(checkBox);
		
		int duplicates= fRefactoring.getNumberOfDuplicates();
		checkBox= new Button(result, SWT.CHECK);
		if (duplicates == 0) {
			checkBox.setText(RefactoringMessages.ExtractMethodInputPage_duplicates_none); 
		} else  if (duplicates == 1) {
			checkBox.setText(RefactoringMessages.ExtractMethodInputPage_duplicates_single); 
		} else {
			checkBox.setText(Messages.format(
				RefactoringMessages.ExtractMethodInputPage_duplicates_multi, 
				Integer.valueOf(duplicates))); 
		}
		checkBox.setSelection(fRefactoring.getReplaceDuplicates());
		checkBox.setEnabled(duplicates > 0);
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setReplaceDuplicates(((Button)e.widget).getSelection());
			}
		});
		layouter.perform(checkBox);
		
		label= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layouter.perform(label);
		
		createSignaturePreview(result, layouter);
		
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_METHOD_WIZARD_PAGE);		
	}
	
	private String getLabel(ASTNode node) {
		if (node instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)node).getName().getIdentifier();
		} else if (node instanceof AnonymousClassDeclaration) {
			if (node.getLocationInParent() == ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY) {
				ClassInstanceCreation creation= (ClassInstanceCreation)node.getParent();
				return Messages.format(
					RefactoringMessages.ExtractMethodInputPage_anonymous_type_label,  
					ASTNodes.asString(creation.getType()));
			}
		}
		return "UNKNOWN"; //$NON-NLS-1$
	}

	private Text createTextInputField(Composite parent, int style) {
		Text result= new Text(parent, style);
		result.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				textModified(getText());
			}
		});
		TextFieldNavigationHandler.install(result);
		return result;
	}
	
	private String getText() {
		if (fTextField == null)
			return null;
		return fTextField.getText();	
	}
	
	private String getLabelText(){
		return RefactoringMessages.ExtractMethodInputPage_label_text; 
	}
	
//	private void setVisibility(Integer visibility) {
//		fRefactoring.setVisibility(visibility.intValue());
//		updatePreview(getText());
//	}
//	
//	private void setRethrowRuntimeException(boolean value) {
//		fSettings.put(THROW_RUNTIME_EXCEPTIONS, value);
//		fRefactoring.setThrowRuntimeExceptions(value);
//		updatePreview(getText());
//	}
	
	private boolean computeGenerateJavadoc() {
		boolean result= fRefactoring.getGenerateJavadoc();
		if (result)
			return result;
		return fSettings.getBoolean(GENERATE_JAVADOC);
	}
	
	private void setGenerateJavadoc(boolean value) {
		fSettings.put(GENERATE_JAVADOC, value);
		fRefactoring.setGenerateJavadoc(value);
	}
	
	private void createSignaturePreview(Composite composite, RowLayouter layouter) {
		Label previewLabel= new Label(composite, SWT.NONE);
		previewLabel.setText(RefactoringMessages.ExtractMethodInputPage_signature_preview); 
		layouter.perform(previewLabel);
		
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		fSignaturePreview= new JavaSourceViewer(composite, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP /*| SWT.BORDER*/, store);
		fSignaturePreview.configure(new JavaScriptSourceViewerConfiguration(JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		fSignaturePreview.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		fSignaturePreview.getTextWidget().setBackground(composite.getBackground());
		fSignaturePreview.setDocument(fSignaturePreviewDocument);
		fSignaturePreview.setEditable(false);
		
		//Layouting problems with wrapped text: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=9866
		Control signaturePreviewControl= fSignaturePreview.getControl();
		PixelConverter pixelConverter= new PixelConverter(signaturePreviewControl);
		GridData gdata= new GridData(GridData.FILL_BOTH);
		gdata.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
		gdata.heightHint= pixelConverter.convertHeightInCharsToPixels(2);
		signaturePreviewControl.setLayoutData(gdata);
		layouter.perform(signaturePreviewControl);
	}
	
	private void updatePreview(String text) {
		if (fSignaturePreview == null)
			return;
			
		if (text.length() == 0)
			text= "someMethodName";			 //$NON-NLS-1$
		
		int top= fSignaturePreview.getTextWidget().getTopPixel();
		String signature;
		try {
			signature= fRefactoring.getSignature(text);
		} catch (IllegalArgumentException e) { 
			signature= ""; //$NON-NLS-1$ 
		}
		fSignaturePreviewDocument.set(signature);
		fSignaturePreview.getTextWidget().setTopPixel(top);
	}
	
	private void loadSettings() {
		fSettings= getDialogSettings().getSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
			fSettings.put(THROW_RUNTIME_EXCEPTIONS, false);
			fSettings.put(GENERATE_JAVADOC, JavaPreferencesSettings.getCodeGenerationSettings(fRefactoring.getCompilationUnit().getJavaScriptProject()).createComments);
		}
		fRefactoring.setThrowRuntimeExceptions(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
	}
	
	//---- Input validation ------------------------------------------------------
	
	public void setVisible(boolean visible) {
		if (visible) {
			if (fFirstTime) {
				fFirstTime= false;
				setPageComplete(false);
				updatePreview(getText());
				fTextField.setFocus();
			} else {
				setPageComplete(validatePage(true));
			}
		}
		super.setVisible(visible);
	}
	
	private void textModified(String text) {
		fRefactoring.setMethodName(text);
		RefactoringStatus status= validatePage(true);
		if (!status.hasFatalError()) {
			updatePreview(text);
		} else {
			fSignaturePreviewDocument.set(""); //$NON-NLS-1$
		}
		setPageComplete(status);
	}
	
	private void parameterModified() {
		updatePreview(getText());
		setPageComplete(validatePage(false));
	}
	
	private RefactoringStatus validatePage(boolean text) {
		RefactoringStatus result= new RefactoringStatus();
		if (text) {
			result.merge(validateMethodName());
			result.merge(validateParameters());
		} else {
			result.merge(validateParameters());
			result.merge(validateMethodName());
		}
		return result;
	}
	
	private RefactoringStatus validateMethodName() {
		RefactoringStatus result= new RefactoringStatus();
		String text= getText();
		if ("".equals(text)) { //$NON-NLS-1$
			result.addFatalError(RefactoringMessages.ExtractMethodInputPage_validation_emptyMethodName); 
			return result;
		}
		result.merge(fRefactoring.checkMethodName());
		return result;
	}
	
	private RefactoringStatus validateParameters() {
		RefactoringStatus result= new RefactoringStatus();
		List parameters= fRefactoring.getParameterInfos();
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if ("".equals(info.getNewName())) { //$NON-NLS-1$
				result.addFatalError(RefactoringMessages.ExtractMethodInputPage_validation_emptyParameterName); 
				return result;
			}
		}
		result.merge(fRefactoring.checkParameterNames());
		result.merge(fRefactoring.checkVarargOrder());
		return result;
	}
}
