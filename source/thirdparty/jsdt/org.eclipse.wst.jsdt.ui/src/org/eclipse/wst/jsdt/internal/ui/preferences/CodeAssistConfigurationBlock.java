/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.text.java.ProposalSorterHandle;
import org.eclipse.wst.jsdt.internal.ui.text.java.ProposalSorterRegistry;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * Configures the content assist preferences.
 * 
 * 
 */
class CodeAssistConfigurationBlock extends OptionsConfigurationBlock {
	
	private static final Key PREF_CODEASSIST_AUTOACTIVATION= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION);
	private static final Key PREF_CODEASSIST_AUTOACTIVATION_DELAY= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY);
	private static final Key PREF_CODEASSIST_AUTOINSERT= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOINSERT);
	private static final Key PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
	private static final Key PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC);
	private static final Key PREF_CODEASSIST_SHOW_VISIBLE_PROPOSALS= getJDTUIKey(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS);
	private static final Key PREF_CODEASSIST_SORTER= getJDTUIKey(PreferenceConstants.CODEASSIST_SORTER);
	private static final Key PREF_CODEASSIST_CASE_SENSITIVITY= getJDTUIKey(PreferenceConstants.CODEASSIST_CASE_SENSITIVITY);
	private static final Key PREF_CODEASSIST_ADDIMPORT= getJDTUIKey(PreferenceConstants.CODEASSIST_ADDIMPORT);
	private static final Key PREF_CODEASSIST_SUGGEST_STATIC_IMPORTS= getJDTCoreKey(JavaScriptCore.CODEASSIST_SUGGEST_STATIC_IMPORTS);
	private static final Key PREF_CODEASSIST_INSERT_COMPLETION= getJDTUIKey(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
	private static final Key PREF_CODEASSIST_FILL_ARGUMENT_NAMES= getJDTUIKey(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES);
	private static final Key PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS= getJDTUIKey(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
	private static final Key PREF_CODEASSIST_PREFIX_COMPLETION= getJDTUIKey(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION);
	private static final Key PREF_CODEASSIST_FORBIDDEN_REFERENCE_CHECK= getJDTCoreKey(JavaScriptCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK);
	private static final Key PREF_CODEASSIST_DISCOURAGED_REFERENCE_CHECK= getJDTCoreKey(JavaScriptCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK);
	private static final Key PREF_CODEASSIST_DEPRECATION_CHECK= getJDTCoreKey(JavaScriptCore.CODEASSIST_DEPRECATION_CHECK);
	private static final Key PREF_CODEASSIST_CAMEL_CASE_MATCH= getJDTCoreKey(JavaScriptCore.CODEASSIST_CAMEL_CASE_MATCH);

	private static Key[] getAllKeys() {
		return new Key[] {
				PREF_CODEASSIST_AUTOACTIVATION,
				PREF_CODEASSIST_AUTOACTIVATION_DELAY,
				PREF_CODEASSIST_AUTOINSERT,
				PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA,
				PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC,
				PREF_CODEASSIST_SHOW_VISIBLE_PROPOSALS,
				PREF_CODEASSIST_SORTER,
				PREF_CODEASSIST_CASE_SENSITIVITY,
				PREF_CODEASSIST_ADDIMPORT,
				PREF_CODEASSIST_SUGGEST_STATIC_IMPORTS,
				PREF_CODEASSIST_INSERT_COMPLETION,
				PREF_CODEASSIST_FILL_ARGUMENT_NAMES,
				PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS,
				PREF_CODEASSIST_PREFIX_COMPLETION,
				PREF_CODEASSIST_FORBIDDEN_REFERENCE_CHECK,
				PREF_CODEASSIST_DISCOURAGED_REFERENCE_CHECK,
				PREF_CODEASSIST_DEPRECATION_CHECK,
				PREF_CODEASSIST_CAMEL_CASE_MATCH,
		};	
	}
	
	private static final String[] trueFalse= new String[] { IPreferenceStore.TRUE, IPreferenceStore.FALSE };
	private static final String[] enabledDisabled= new String[] { JavaScriptCore.ENABLED, JavaScriptCore.DISABLED };

	private Button fCompletionInsertsRadioButton;
	private Button fCompletionOverwritesRadioButton;

	public CodeAssistConfigurationBlock(IStatusChangeListener statusListener, IWorkbenchPreferenceContainer workbenchcontainer) {
		super(statusListener, null, getAllKeys(), workbenchcontainer);
	}

	protected Control createContents(Composite parent) {
		ScrolledPageContent scrolled= new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite control= new Composite(scrolled, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		control.setLayout(layout);

		Composite composite;

		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_insertionSection_title); 
		addInsertionSection(composite);
		
		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_sortingSection_title); 
		addSortingSection(composite);
		
		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_autoactivationSection_title); 
		addAutoActivationSection(composite);
		
		initialize();
		
		scrolled.setContent(control);
		final Point size= control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(size.x, size.y);
		return scrolled;
	}

	protected Composite createSubsection(Composite parent, String label) {
		Group group= new Group(parent, SWT.SHADOW_NONE);
		group.setText(label);
		GridData data= new GridData(SWT.FILL, SWT.CENTER, true, false);
		group.setLayoutData(data);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		group.setLayout(layout);

		return group;
	}

	private void addInsertionSection(Composite composite) {
		addCompletionRadioButtons(composite);
		
		String label;		
		label= PreferencesMessages.JavaEditorPreferencePage_insertSingleProposalsAutomatically;
		addCheckBox(composite, label, PREF_CODEASSIST_AUTOINSERT, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_completePrefixes; 
		addCheckBox(composite, label, PREF_CODEASSIST_PREFIX_COMPLETION, trueFalse, 0);
		
//		label= PreferencesMessages.JavaEditorPreferencePage_automaticallyAddImportInsteadOfQualifiedName; 
//		Button master= addCheckBox(composite, label, PREF_CODEASSIST_ADDIMPORT, trueFalse, 0);
		
//		label= PreferencesMessages.JavaEditorPreferencePage_suggestStaticImports; 
//		Button slave= addCheckBox(composite, label, PREF_CODEASSIST_SUGGEST_STATIC_IMPORTS, enabledDisabled, 20);
//		createSelectionDependency(master, slave);
		
		
		label= PreferencesMessages.JavaEditorPreferencePage_fillArgumentNamesOnMethodCompletion; 
		Button master= addCheckBox(composite, label, PREF_CODEASSIST_FILL_ARGUMENT_NAMES, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_guessArgumentNamesOnMethodCompletion; 
		Button slave= addCheckBox(composite, label, PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS, trueFalse, 20);
		createSelectionDependency(master, slave);
	}

	/**
	 * Creates a selection dependency between a master and a slave control.
	 * 
	 * @param master
	 *                   The master button that controls the state of the slave
	 * @param slave
	 *                   The slave control that is enabled only if the master is
	 *                   selected
	 */
	protected static void createSelectionDependency(final Button master, final Control slave) {

		master.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent event) {
				// Do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				slave.setEnabled(master.getSelection());
			}
		});
		slave.setEnabled(master.getSelection());
	}
	
	private void addSortingSection(Composite composite) {
		String label;
		label= PreferencesMessages.JavaEditorPreferencePage_presentProposalsInAlphabeticalOrder;
		ProposalSorterHandle[] sorters= ProposalSorterRegistry.getDefault().getSorters();
		String[] labels= new String[sorters.length];
		String[] values= new String[sorters.length];
		for (int i= 0; i < sorters.length; i++) {
			ProposalSorterHandle handle= sorters[i];
			labels[i]= handle.getName();
			values[i]= handle.getId();
		}
		
		addComboBox(composite, label, PREF_CODEASSIST_SORTER, values, labels, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_showOnlyProposalsVisibleInTheInvocationContext; 
		addCheckBox(composite, label, PREF_CODEASSIST_SHOW_VISIBLE_PROPOSALS, trueFalse, 0);
		
		label= PreferencesMessages.CodeAssistConfigurationBlock_matchCamelCase_label;
		addCheckBox(composite, label, PREF_CODEASSIST_CAMEL_CASE_MATCH, enabledDisabled, 0);

		/*
		label= PreferencesMessages.CodeAssistConfigurationBlock_restricted_link;
		Map targetInfo= new java.util.HashMap(2);
		targetInfo.put(ProblemSeveritiesPreferencePage.DATA_SELECT_OPTION_KEY,	JavaScriptCore.COMPILER_PB_FORBIDDEN_REFERENCE);
		targetInfo.put(ProblemSeveritiesPreferencePage.DATA_SELECT_OPTION_QUALIFIER, JavaScriptCore.PLUGIN_ID);
		createPreferencePageLink(composite, label, targetInfo);
		
		
		label= PreferencesMessages.CodeAssistConfigurationBlock_hideForbidden_label;
		addCheckBox(composite, label, PREF_CODEASSIST_FORBIDDEN_REFERENCE_CHECK, enabledDisabled, 0);
		
		label= PreferencesMessages.CodeAssistConfigurationBlock_hideDiscouraged_label;
		addCheckBox(composite, label, PREF_CODEASSIST_DISCOURAGED_REFERENCE_CHECK, enabledDisabled, 0);
		
		label= PreferencesMessages.CodeAssistConfigurationBlock_hideDeprecated_label;
		addCheckBox(composite, label, PREF_CODEASSIST_DEPRECATION_CHECK, enabledDisabled, 0);
		*/
	}

	private void createPreferencePageLink(Composite composite, String label, final Map targetInfo) {
		final Link link= new Link(composite, SWT.NONE);
		link.setText(label);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, targetInfo); 
			}
		});
	}
	
	private void addAutoActivationSection(Composite composite) {
		String label;
		label= PreferencesMessages.JavaEditorPreferencePage_enableAutoActivation; 
		final Button autoactivation= addCheckBox(composite, label, PREF_CODEASSIST_AUTOACTIVATION, trueFalse, 0);
		autoactivation.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateAutoactivationControls();
			}
		});		
		
		label= PreferencesMessages.JavaEditorPreferencePage_autoActivationDelay; 
		addLabelledTextField(composite, label, PREF_CODEASSIST_AUTOACTIVATION_DELAY, 4, 0, true);
		
		label= PreferencesMessages.JavaEditorPreferencePage_autoActivationTriggersForJava; 
		addLabelledTextField(composite, label, PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, 4, 0, false);
		
		label= PreferencesMessages.JavaEditorPreferencePage_autoActivationTriggersForJavaDoc; 
		addLabelledTextField(composite, label, PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, 4, 0, false);
	}
	
	protected Text addLabelledTextField(Composite parent, String label, Key key, int textlimit, int indent, boolean dummy) {	
		PixelConverter pixelConverter= new PixelConverter(parent);
		
		Label labelControl= new Label(parent, SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(new GridData());
				
		Text textBox= new Text(parent, SWT.BORDER | SWT.SINGLE);
		textBox.setData(key);
		textBox.setLayoutData(new GridData());
		
		fLabels.put(textBox, labelControl);
		
		String currValue= getValue(key);	
		if (currValue != null) {
			textBox.setText(currValue);
		}
		textBox.addModifyListener(getTextModifyListener());

		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		if (textlimit != 0) {
			textBox.setTextLimit(textlimit);
			data.widthHint= pixelConverter.convertWidthInCharsToPixels(textlimit + 1);
		}
		data.horizontalIndent= indent;
		data.horizontalSpan= 2;
		textBox.setLayoutData(data);

		fTextBoxes.add(textBox);
		return textBox;
	}

	private void addCompletionRadioButtons(Composite contentAssistComposite) {
		Composite completionComposite= new Composite(contentAssistComposite, SWT.NONE);
		GridData ccgd= new GridData();
		ccgd.horizontalSpan= 2;
		completionComposite.setLayoutData(ccgd);
		GridLayout ccgl= new GridLayout();
		ccgl.marginWidth= 0;
		ccgl.numColumns= 2;
		completionComposite.setLayout(ccgl);
		
		SelectionListener completionSelectionListener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {				
				boolean insert= fCompletionInsertsRadioButton.getSelection();
				setValue(PREF_CODEASSIST_INSERT_COMPLETION, insert);
			}
		};
		
		fCompletionInsertsRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionInsertsRadioButton.setText(PreferencesMessages.JavaEditorPreferencePage_completionInserts); 
		fCompletionInsertsRadioButton.setLayoutData(new GridData());
		fCompletionInsertsRadioButton.addSelectionListener(completionSelectionListener);
		
		fCompletionOverwritesRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionOverwritesRadioButton.setText(PreferencesMessages.JavaEditorPreferencePage_completionOverwrites); 
		fCompletionOverwritesRadioButton.setLayoutData(new GridData());
		fCompletionOverwritesRadioButton.addSelectionListener(completionSelectionListener);
		
		Label label= new Label(completionComposite, SWT.NONE);
		label.setText(PreferencesMessages.JavaEditorPreferencePage_completionToggleHint);
		GridData gd= new GridData();
		gd.horizontalIndent= 20;
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
	}
	
	public void initialize() {
		initializeFields();
	}

	private void initializeFields() {
		boolean completionInserts= getBooleanValue(PREF_CODEASSIST_INSERT_COMPLETION);
		fCompletionInsertsRadioButton.setSelection(completionInserts);
		fCompletionOverwritesRadioButton.setSelection(!completionInserts);
		
		updateAutoactivationControls();
 	}
	
    private void updateAutoactivationControls() {
        boolean autoactivation= getBooleanValue(PREF_CODEASSIST_AUTOACTIVATION);
        setControlEnabled(PREF_CODEASSIST_AUTOACTIVATION_DELAY, autoactivation);
        setControlEnabled(PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, autoactivation);
        setControlEnabled(PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, autoactivation);
        setControlEnabled(PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS, getBooleanValue(PREF_CODEASSIST_FILL_ARGUMENT_NAMES));
    }

    
	public void performDefaults() {
		super.performDefaults();
		initializeFields();
	}
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}
	
	/**
	 * Validates that the specified number is positive.
	 * 
	 * @param number
	 *                   The number to validate
	 * @return The status of the validation
	 */
	protected static IStatus validatePositiveNumber(final String number) {

		final StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.SpellingPreferencePage_empty_threshold); 
		} else {
			try {
				final int value= Integer.parseInt(number);
				if (value < 0) {
					status.setError(Messages.format(PreferencesMessages.SpellingPreferencePage_invalid_threshold, number)); 
				}
			} catch (NumberFormatException exception) {
				status.setError(Messages.format(PreferencesMessages.SpellingPreferencePage_invalid_threshold, number)); 
			}
		}
		return status;
	}
	
	protected void validateSettings(Key key, String oldValue, String newValue) {
		if (key == null || PREF_CODEASSIST_AUTOACTIVATION_DELAY.equals(key))
			fContext.statusChanged(validatePositiveNumber(getValue(PREF_CODEASSIST_AUTOACTIVATION_DELAY)));
	}

	protected void setControlEnabled(Key key, boolean enabled) {
		Control control= getControl(key);
		control.setEnabled(enabled);
		Label label= (Label) fLabels.get(control);
		if (label != null)
			label.setEnabled(enabled);
	}

	private Control getControl(Key key) {
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fTextBoxes.get(i);
			Key currKey= (Key) curr.getData();
			if (key.equals(currKey)) {
				return curr;
			}
		}
		return null;		
	}
}
