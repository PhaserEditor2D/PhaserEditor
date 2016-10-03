/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class ProblemSeveritiesConfigurationBlock extends OptionsConfigurationBlock {

	private static final String SETTINGS_SECTION_NAME= "ProblemSeveritiesConfigurationBlock";  //$NON-NLS-1$
	
	private static final Key PREF_PB_SEMANTIC_VALIDATION_ENABLEMENT = getJDTCoreKey(JavaScriptCore.COMPILER_SEMANTIC_VALIDATION);
	private static final Key PREF_PB_STRICT_ON_KEYWORD_USAGE = getJDTCoreKey(JavaScriptCore.COMPILER_STRICT_ON_KEYWORD_USAGE);
	
	// Preference store keys, see JavaScriptCore.getOptions
	private static final Key PREF_PB_UNDEFINED_FIELD= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNDEFINED_FIELD);
//	private static final Key PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME= getJDTCoreKey(JavaScriptCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME);
	private static final Key PREF_PB_DEPRECATION= getJDTCoreKey(JavaScriptCore.COMPILER_PB_DEPRECATION);
	private static final Key PREF_PB_DEPRECATION_IN_DEPRECATED_CODE=getJDTCoreKey(JavaScriptCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE);
	private static final Key PREF_PB_DEPRECATION_WHEN_OVERRIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD);
	
	private static final Key PREF_PB_HIDDEN_CATCH_BLOCK= getJDTCoreKey(JavaScriptCore.COMPILER_PB_HIDDEN_CATCH_BLOCK);
	private static final Key PREF_PB_UNUSED_LOCAL= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_LOCAL);
	private static final Key PREF_PB_UNUSED_PARAMETER= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_PARAMETER);
//	private static final Key PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE);
	private static final Key PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE);
	private static final Key PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT);
//	private static final Key PREF_PB_SYNTHETIC_ACCESS_EMULATION= getJDTCoreKey(JavaScriptCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION);
	private static final Key PREF_PB_NON_EXTERNALIZED_STRINGS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_NON_NLS_STRING_LITERAL);
//	private static final Key PREF_PB_UNUSED_IMPORT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_IMPORT);
	private static final Key PREF_PB_UNUSED_PRIVATE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER);
//	private static final Key PREF_PB_STATIC_ACCESS_RECEIVER= getJDTCoreKey(JavaScriptCore.COMPILER_PB_STATIC_ACCESS_RECEIVER);
	private static final Key PREF_PB_NO_EFFECT_ASSIGNMENT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT);
//	private static final Key PREF_PB_CHAR_ARRAY_IN_CONCAT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION);
	private static final Key PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT);
	private static final Key PREF_PB_LOCAL_VARIABLE_HIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_LOCAL_VARIABLE_HIDING);
	private static final Key PREF_PB_FIELD_HIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_FIELD_HIDING);
//	private static final Key PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD= getJDTCoreKey(JavaScriptCore.COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD);
	private static final Key PREF_PB_INDIRECT_STATIC_ACCESS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INDIRECT_STATIC_ACCESS);
	private static final Key PREF_PB_EMPTY_STATEMENT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_EMPTY_STATEMENT);
	private static final Key PREF_PB_UNNECESSARY_ELSE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNNECESSARY_ELSE);
//	private static final Key PREF_PB_UNNECESSARY_TYPE_CHECK= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK);
//	private static final Key PREF_PB_INCOMPATIBLE_INTERFACE_METHOD= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD);
//	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION);
//	private static final Key PREF_PB_MISSING_SERIAL_VERSION= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_SERIAL_VERSION);
	private static final Key PREF_PB_UNDOCUMENTED_EMPTY_BLOCK= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK);
	private static final Key PREF_PB_FINALLY_BLOCK_NOT_COMPLETING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING);
//	private static final Key PREF_PB_UNQUALIFIED_FIELD_ACCESS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS);
	private static final Key PREF_PB_FORBIDDEN_REFERENCE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_FORBIDDEN_REFERENCE);
	private static final Key PREF_PB_DISCOURRAGED_REFERENCE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_DISCOURAGED_REFERENCE);
	private static final Key PREF_PB_UNUSED_LABEL= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNUSED_LABEL);
	private static final Key PREF_PB_PARAMETER_ASSIGNMENT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_PARAMETER_ASSIGNMENT);
	private static final Key PREF_PB_FALLTHROUGH_CASE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_FALLTHROUGH_CASE);
	
	private static final Key PREF_PB_NULL_REFERENCE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_NULL_REFERENCE);
	private static final Key PREF_PB_POTENTIAL_NULL_REFERENCE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE);
	private static final Key PREF_PB_DUPLICATE_LOCAL_VARIABLES= getJDTCoreKey(JavaScriptCore.COMPILER_PB_DUPLICATE_LOCAL_VARIABLES);
	private static final Key PREF_PB_REDUNDANT_NULL_CHECK= getJDTCoreKey(JavaScriptCore.COMPILER_PB_REDUNDANT_NULL_CHECK);
	private static final Key PREF_PB_UNINITIALIZED_LOCAL_VARIABLE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNINITIALIZED_LOCAL_VARIABLE);
	private static final Key PREF_PB_UNINITIALIZED_GLOBAL_VARIABLE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNINITIALIZED_GLOBAL_VARIABLE);
	
//	private static final Key PREF_15_PB_UNCHECKED_TYPE_OPERATION= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION);
//	private static final Key PREF_15_PB_FINAL_PARAM_BOUND= getJDTCoreKey(JavaScriptCore.COMPILER_PB_FINAL_PARAMETER_BOUND);
//	private static final Key PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST= getJDTCoreKey(JavaScriptCore.COMPILER_PB_VARARGS_ARGUMENT_NEED_CAST);
//	private static final Key PREF_15_PB_AUTOBOXING_PROBLEM= getJDTCoreKey(JavaScriptCore.COMPILER_PB_AUTOBOXING);
	
//	private static final Key PREF_15_PB_MISSING_OVERRIDE_ANNOTATION= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION);
//	private static final Key PREF_15_PB_ANNOTATION_SUPER_INTERFACE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_ANNOTATION_SUPER_INTERFACE);
//	private static final Key PREF_15_PB_TYPE_PARAMETER_HIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_TYPE_PARAMETER_HIDING);
//	private static final Key PREF_15_PB_INCOMPLETE_ENUM_SWITCH= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH);
//	private static final Key PREF_15_PB_RAW_TYPE_REFERENCE= getJDTCoreKey(JavaScriptCore.COMPILER_PB_RAW_TYPE_REFERENCE);
	
//	private static final Key PREF_PB_SUPPRESS_WARNINGS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_SUPPRESS_WARNINGS);
//	private static final Key PREF_PB_UNHANDLED_WARNING_TOKEN= getJDTCoreKey(JavaScriptCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN);
	//private static final Key PREF_PB_FATAL_OPTIONAL_ERROR= getJDTCoreKey(JavaScriptCore.COMPILER_PB_FATAL_OPTIONAL_ERROR);

	/* START -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
	private static final Key PREF_UNRESOLVED_TYPE_OPTIONAL_ERROR =  getJDTCoreKey(JavaScriptCore.UNRESOLVED_TYPE_REFERENCE);
	private static final Key PREF_UNRESOLVED_FIELD_OPTIONAL_ERROR =  getJDTCoreKey(JavaScriptCore.UNRESOLVED_FIELD_REFERENCE);
	private static final Key PREF_UNRESOLVED_METHOD_OPTIONAL_ERROR =  getJDTCoreKey(JavaScriptCore.UNRESOLVED_METHOD_REFERENCE);
	/* END -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
	
	
	/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
	private static final Key PREF_LOOSE_VAR =  getJDTCoreKey(JavaScriptCore.LOOSE_VAR_DECL);
	private static final Key PREF_OPTIONAL_SEMICOLON =  getJDTCoreKey(JavaScriptCore.OPTIONAL_SEMICOLON);

	
	/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */	
	
	/* START -------------------------------- Bug 417465 - JavaScript Validation reports max 100 problems per .js file --------------------- */
	private static final Key PREF_PB_MAX_PER_UNIT= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MAX_PER_UNIT);
	/* END   -------------------------------- Bug 417465 - JavaScript Validation reports max 100 problems per .js file --------------------- */	
	
	// values
	private static final String ERROR= JavaScriptCore.ERROR;
	private static final String WARNING= JavaScriptCore.WARNING;
	private static final String IGNORE= JavaScriptCore.IGNORE;

	private static final String ENABLED= JavaScriptCore.ENABLED;
	private static final String DISABLED= JavaScriptCore.DISABLED;
	

	private PixelConverter fPixelConverter;

	private ControlEnableState fBlockEnableState;
	private Composite fControlsComposite;
	private Button semanticCheckBox;
	private Button strictOnKeywordsCheckBox;
	
	public ProblemSeveritiesConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);
		
		// compatibilty code for the merge of the two option PB_SIGNAL_PARAMETER: 
		if (ENABLED.equals(getValue(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT))) {
//			setValue(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, ENABLED);
		}
	}
	
	private static Key[] getKeys() {
		return new Key[] {
				PREF_PB_MAX_PER_UNIT,
				PREF_PB_SEMANTIC_VALIDATION_ENABLEMENT,
				PREF_PB_STRICT_ON_KEYWORD_USAGE,
				PREF_PB_UNDEFINED_FIELD,
				/*PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME,*/ PREF_PB_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
				PREF_PB_UNUSED_PARAMETER, PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE,
				/*PREF_PB_SYNTHETIC_ACCESS_EMULATION,*/ PREF_PB_NON_EXTERNALIZED_STRINGS,
				/*PREF_PB_UNUSED_IMPORT,*/ PREF_PB_UNUSED_LABEL, 
				/*PREF_PB_STATIC_ACCESS_RECEIVER, */PREF_PB_DEPRECATION_IN_DEPRECATED_CODE, 
				PREF_PB_NO_EFFECT_ASSIGNMENT, /*PREF_PB_INCOMPATIBLE_INTERFACE_METHOD,*/
				PREF_PB_UNUSED_PRIVATE,/* PREF_PB_CHAR_ARRAY_IN_CONCAT,*/ PREF_PB_UNNECESSARY_ELSE,
				PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, PREF_PB_LOCAL_VARIABLE_HIDING, PREF_PB_FIELD_HIDING,
				/*PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD,*/ PREF_PB_INDIRECT_STATIC_ACCESS,
				PREF_PB_EMPTY_STATEMENT, /*PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, */PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT,
//				PREF_PB_UNNECESSARY_TYPE_CHECK, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, PREF_PB_UNQUALIFIED_FIELD_ACCESS,
				PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, PREF_PB_DEPRECATION_WHEN_OVERRIDING,
				PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, /*PREF_PB_MISSING_SERIAL_VERSION, */
				PREF_PB_PARAMETER_ASSIGNMENT, PREF_PB_NULL_REFERENCE, PREF_PB_POTENTIAL_NULL_REFERENCE,
				PREF_PB_DUPLICATE_LOCAL_VARIABLES,
				PREF_PB_REDUNDANT_NULL_CHECK, PREF_PB_UNINITIALIZED_LOCAL_VARIABLE, PREF_PB_UNINITIALIZED_GLOBAL_VARIABLE, PREF_PB_FALLTHROUGH_CASE,
//				PREF_15_PB_UNCHECKED_TYPE_OPERATION, PREF_15_PB_FINAL_PARAM_BOUND, PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST,
//				PREF_15_PB_AUTOBOXING_PROBLEM, PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, PREF_15_PB_ANNOTATION_SUPER_INTERFACE,
				/*PREF_15_PB_TYPE_PARAMETER_HIDING, PREF_15_PB_INCOMPLETE_ENUM_SWITCH,*/
				/*PREF_15_PB_RAW_TYPE_REFERENCE,*/ /*PREF_PB_FATAL_OPTIONAL_ERROR,*/
				PREF_PB_FORBIDDEN_REFERENCE, PREF_PB_DISCOURRAGED_REFERENCE/*, PREF_PB_SUPPRESS_WARNINGS, PREF_PB_UNHANDLED_WARNING_TOKEN*/,PREF_UNRESOLVED_TYPE_OPTIONAL_ERROR,PREF_UNRESOLVED_FIELD_OPTIONAL_ERROR,PREF_UNRESOLVED_METHOD_OPTIONAL_ERROR,PREF_LOOSE_VAR,PREF_OPTIONAL_SEMICOLON
			};
	}
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		int nColumns= 3;
		
		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		mainComp.setLayout(layout);
	
		String label= PreferencesMessages.JavaBuildConfigurationBlock_pb_max_per_unit_label; 
		Text text= addTextField(mainComp, label, PREF_PB_MAX_PER_UNIT, 0, 0);
		GridData gd= (GridData) text.getLayoutData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(8);
		gd.horizontalAlignment= GridData.END;
		text.setTextLimit(6);
		
		label = PreferencesMessages.ProblemSeveritiesConfigurationBlock_enableStrictOnKeywordUsageValidation;
		strictOnKeywordsCheckBox = addCheckBox(mainComp, label, PREF_PB_STRICT_ON_KEYWORD_USAGE, new String[]{ENABLED, DISABLED}, 0);
		Label horizontalLine= new Label(mainComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		horizontalLine.setFont(mainComp.getFont());

		label = PreferencesMessages.ProblemSeveritiesConfigurationBlock_enableSemanticValidation;
		semanticCheckBox = addCheckBox(mainComp, label, PREF_PB_SEMANTIC_VALIDATION_ENABLEMENT, new String[]{ENABLED, DISABLED}, 0);
		horizontalLine= new Label(mainComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		horizontalLine.setFont(mainComp.getFont());
		
		Composite commonComposite= createStyleTabContent(mainComp);
		GridData gridData= GridDataFactory.fillDefaults().grab(true, true).span(3, 1).create();
		gridData.heightHint= fPixelConverter.convertHeightInCharsToPixels(20);
		commonComposite.setLayoutData(gridData);

		fControlsComposite = commonComposite;
		
		validateSettings(null, null, null);
	
		return mainComp;
	}
	
	private Composite createStyleTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_error,  
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_warning, 
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		
		int nColumns= 3;
		
		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
		
		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout(nColumns, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		
		Label description= new Label(composite, SWT.LEFT | SWT.WRAP);
		description.setFont(description.getFont());
		description.setText(PreferencesMessages.ProblemSeveritiesConfigurationBlock_common_description); 
		description.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, nColumns - 1, 1));
				
		int indentStep=  fPixelConverter.convertWidthInCharsToPixels(1);
		
		int defaultIndent= indentStep * 0;
		int extraIndent= indentStep * 2;
		String label;
		ExpandableComposite excomposite;
		Composite inner;
		/* START -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_Resolution;
//		excomposite= createStyleSection(composite, label, nColumns);
//		
//		inner= new Composite(excomposite, SWT.NONE);
//		inner.setFont(composite.getFont());
//		inner.setLayout(new GridLayout(nColumns, false));
//		excomposite.setClient(inner);
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_UnresolvedTypes; 
//		addComboBox(inner, label, PREF_UNRESOLVED_TYPE_OPTIONAL_ERROR, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_UnresolvedFields; 
//		addComboBox(inner, label, PREF_UNRESOLVED_FIELD_OPTIONAL_ERROR, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_UnresolvedMethods; 
//		addComboBox(inner, label, PREF_UNRESOLVED_METHOD_OPTIONAL_ERROR, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		/* END -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
		// --- style
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_code_style; 
		excomposite= createStyleSection(composite, label, nColumns);
		
		inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_static_access_receiver_label; 
//		addComboBox(inner, label, PREF_PB_STATIC_ACCESS_RECEIVER, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_indirect_access_to_static_label; 
//		addComboBox(inner, label, PREF_PB_INDIRECT_STATIC_ACCESS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);	
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unqualified_field_access_label; 
//		addComboBox(inner, label, PREF_PB_UNQUALIFIED_FIELD_ACCESS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_undocumented_empty_block_label; 
		addComboBox(inner, label, PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_synth_access_emul_label; 
//		addComboBox(inner, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_method_naming_label; 
//		addComboBox(inner, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);			

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_parameter_assignment; 
		addComboBox(inner, label, PREF_PB_PARAMETER_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);			

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_non_externalized_strings_label; 
		addComboBox(inner, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
		// --- potential_programming_problems
			
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_potential_programming_problems; 
		excomposite= createStyleSection(composite, label, nColumns);
		
		inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
	/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_LooselyDeclaredGlobalVar; 
//		addComboBox(inner, label, PREF_LOOSE_VAR, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);			

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_Optionalsemicolon; 
		addComboBox(inner, label, PREF_OPTIONAL_SEMICOLON, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);			

		/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
		
	
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_undefined_field_label; 
//		addComboBox(inner, label, PREF_PB_UNDEFINED_FIELD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);			
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_serial_version_label; 
//		addComboBox(inner, label, PREF_PB_MISSING_SERIAL_VERSION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
				
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_no_effect_assignment_label; 
		addComboBox(inner, label, PREF_PB_NO_EFFECT_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_accidential_assignement_label; 
//		addComboBox(inner, label, PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_finally_block_not_completing_label; 
		addComboBox(inner, label, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_empty_statement_label; 
		addComboBox(inner, label, PREF_PB_EMPTY_STATEMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_char_array_in_concat_label; 
//		addComboBox(inner, label, PREF_PB_CHAR_ARRAY_IN_CONCAT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_hidden_catchblock_label; 
//		addComboBox(inner, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_inexact_vararg_label; 
//		addComboBox(inner, label, PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_autoboxing_problem_label; 
//		addComboBox(inner, label, PREF_15_PB_AUTOBOXING_PROBLEM, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_incomplete_enum_switch_label; 
//		addComboBox(inner, label, PREF_15_PB_INCOMPLETE_ENUM_SWITCH, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_fall_through_case;
		addComboBox(inner, label, PREF_PB_FALLTHROUGH_CASE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_reference;
//		addComboBox(inner, label, PREF_PB_NULL_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_potential_null_reference;
//		addComboBox(inner, label, PREF_PB_POTENTIAL_NULL_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_duplicate_local_variables;
//		addComboBox(inner, label, PREF_PB_DUPLICATE_LOCAL_VARIABLES, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
		label= PreferencesMessages.ProblemSeveritiesconfigurationBlock_pb_uninitialized_local_variable;
		addComboBox(inner, label, PREF_PB_UNINITIALIZED_LOCAL_VARIABLE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesconfigurationBlock_pb_uninitialized_global_variable;
//		addComboBox(inner, label, PREF_PB_UNINITIALIZED_GLOBAL_VARIABLE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
		// --- name_shadowing
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_name_shadowing; 
//		excomposite= createStyleSection(composite, label, nColumns);
//		
//		inner= new Composite(excomposite, SWT.NONE);
//		inner.setFont(composite.getFont());
//		inner.setLayout(new GridLayout(nColumns, false));
//		excomposite.setClient(inner);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_field_hiding_label; 
//		addComboBox(inner, label, PREF_PB_FIELD_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_local_variable_hiding_label; 
		addComboBox(inner, label, PREF_PB_LOCAL_VARIABLE_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_special_param_hiding_label; 
//		addCheckBox(inner, label, PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, enabledDisabled, extraIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_type_parameter_hiding_label; 
//		addComboBox(inner, label, PREF_15_PB_TYPE_PARAMETER_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_incompatible_interface_method_label; 
//		addComboBox(inner, label, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
		// --- API access rules
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_deprecations; 
//		excomposite= createStyleSection(composite, label, nColumns);
//		
//		inner= new Composite(excomposite, SWT.NONE);
//		inner.setFont(composite.getFont());
//		inner.setLayout(new GridLayout(nColumns, false));
//		excomposite.setClient(inner);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_label; 
//		addComboBox(inner, label, PREF_PB_DEPRECATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_in_deprecation_label; 
//		addCheckBox(inner, label, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE, enabledDisabled, extraIndent);		
//	
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_when_overriding_label; 
//		addCheckBox(inner, label, PREF_PB_DEPRECATION_WHEN_OVERRIDING, enabledDisabled, extraIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_forbidden_reference_label; 
//		addComboBox(inner, label, PREF_PB_FORBIDDEN_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, 0);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_discourraged_reference_label; 
//		addComboBox(inner, label, PREF_PB_DISCOURRAGED_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		
		// --- unnecessary_code
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_unnecessary_code; 
		excomposite= createStyleSection(composite, label, nColumns);
	
		inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_local_label; 
		addComboBox(inner, label, PREF_PB_UNUSED_LOCAL, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_parameter_label; 
//		addComboBox(inner, label, PREF_PB_UNUSED_PARAMETER, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_signal_param_in_overriding_label; 
//		addCheckBox(inner, label, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, enabledDisabled, extraIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore_documented_unused_parameters; 
//		addCheckBox(inner, label, PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, enabledDisabled, extraIndent);
				
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_imports_label; 
//		addComboBox(inner, label, PREF_PB_UNUSED_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_private_label; 
//		addComboBox(inner, label, PREF_PB_UNUSED_PRIVATE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_null_check; 
//		addComboBox(inner, label, PREF_PB_REDUNDANT_NULL_CHECK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unnecessary_else_label; 
		addComboBox(inner, label, PREF_PB_UNNECESSARY_ELSE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unnecessary_type_check_label; 
//		addComboBox(inner, label, PREF_PB_UNNECESSARY_TYPE_CHECK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_label; 
//		addComboBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_when_overriding_label; 
//		addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, enabledDisabled, extraIndent);

//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_label_label; 
//		addComboBox(inner, label, PREF_PB_UNUSED_LABEL, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
	
		
		// --- generics
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_generics;
//		excomposite= createStyleSection(composite, label, nColumns);
//
//		
//		inner= new Composite(excomposite, SWT.NONE);
//		inner.setFont(composite.getFont());
//		inner.setLayout(new GridLayout(nColumns, false));
//		excomposite.setClient(inner);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unsafe_type_op_label; 
//		addComboBox(inner, label, PREF_15_PB_UNCHECKED_TYPE_OPERATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_raw_type_reference; 
//		addComboBox(inner, label, PREF_15_PB_RAW_TYPE_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_final_param_bound_label; 
//		addComboBox(inner, label, PREF_15_PB_FINAL_PARAM_BOUND, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		
//		// --- annotations
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_annotations; 
//		excomposite= createStyleSection(composite, label, nColumns);
//
//		
//		inner= new Composite(excomposite, SWT.NONE);
//		inner.setFont(composite.getFont());
//		inner.setLayout(new GridLayout(nColumns, false));
//		excomposite.setClient(inner);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_override_annotation_label; 
//		addComboBox(inner, label, PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_deprecated_annotation_label; 
//		addComboBox(inner, label, PREF_PB_MISSING_DEPRECATED_ANNOTATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_annotation_super_interface_label; 
//		addComboBox(inner, label, PREF_15_PB_ANNOTATION_SUPER_INTERFACE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unhandled_surpresswarning_tokens;
//		addComboBox(inner, label, PREF_PB_UNHANDLED_WARNING_TOKEN, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
//		
//		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_enable_surpresswarning_annotation; 
//		addCheckBox(inner, label, PREF_PB_SUPPRESS_WARNINGS, enabledDisabled, 0);

		//new Label(composite, SWT.NONE);
		
		//String[] enableDisableValues= new String[] { ENABLED, DISABLED };
		//label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_treat_optional_as_fatal;
		//addCheckBox(composite, label, PREF_PB_FATAL_OPTIONAL_ERROR, enableDisableValues, 0);

		
		IDialogSettings section= JavaScriptPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
		restoreSectionExpansionStates(section);
		
		return sc1;
	}
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}
		
		IStatus maxNumberProblemsStatus = new StatusInfo();
		if (changedKey != null) {
			if ( PREF_PB_MAX_PER_UNIT.equals(changedKey) || 
					PREF_PB_UNUSED_PARAMETER.equals(changedKey) || 
					PREF_PB_SEMANTIC_VALIDATION_ENABLEMENT.equals(changedKey) ||
					PREF_PB_STRICT_ON_KEYWORD_USAGE.equals(changedKey) )
//					PREF_PB_DEPRECATION.equals(changedKey) ||
//					PREF_PB_LOCAL_VARIABLE_HIDING.equals(changedKey) ||
//					PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION.equals(changedKey)) 
			{				
				if (PREF_PB_MAX_PER_UNIT.equals(changedKey)) {
					maxNumberProblemsStatus= validateMaxNumberProblems();
				}
				updateEnableStates();
//			} else if (PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING.equals(changedKey)) {
//				// merging the two options
//				setValue(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT, newValue);
			} else {
				return;
			}
		} else {
			maxNumberProblemsStatus= validateMaxNumberProblems();
			updateEnableStates();
		}
		fContext.statusChanged(maxNumberProblemsStatus);
	}
	
	private void updateEnableStates() {
		boolean notJustParseErrors = checkValue(PREF_PB_SEMANTIC_VALIDATION_ENABLEMENT, ENABLED);
		enableConfigControls(notJustParseErrors);
		
		if (!notJustParseErrors) {
			//boolean enableUnusedParams= !checkValue(PREF_PB_UNUSED_PARAMETER, IGNORE);
	//		getCheckBox(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING).setEnabled(enableUnusedParams);
		//	getCheckBox(PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE).setEnabled(enableUnusedParams);
			
			//boolean enableDeprecation= !checkValue(PREF_PB_DEPRECATION, IGNORE);
			//getCheckBox(PREF_PB_DEPRECATION_IN_DEPRECATED_CODE).setEnabled(enableDeprecation);
			//getCheckBox(PREF_PB_DEPRECATION_WHEN_OVERRIDING).setEnabled(enableDeprecation);
			
	//		boolean enableThrownExceptions= !checkValue(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, IGNORE);
	//		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING).setEnabled(enableThrownExceptions);
	
	//		boolean enableHiding= !checkValue(PREF_PB_LOCAL_VARIABLE_HIDING, IGNORE);
	//		getCheckBox(PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD).setEnabled(enableHiding);
		}
	}
	
	protected void enableConfigControls(boolean enable) {
		if (enable) {
			if (fBlockEnableState != null) {
				fBlockEnableState.restore();
				fBlockEnableState= null;
			}
		} else {
			if (fBlockEnableState == null) {
				fBlockEnableState= ControlEnableState.disable(fControlsComposite);
			}
		}	
	}

	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsbuild_title; 
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsfullbuild_message; 
		} else {
			message= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsprojectbuild_message; 
		}
		return new String[] { title, message };
	}
	
	private IStatus validateMaxNumberProblems() {
		String number= getValue(PREF_PB_MAX_PER_UNIT);
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.JavaBuildConfigurationBlock_empty_input); 
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value <= 0) {
					status.setError(Messages.format(PreferencesMessages.JavaBuildConfigurationBlock_invalid_input, number)); 
				}
			} catch (NumberFormatException e) {
				status.setError(Messages.format(PreferencesMessages.JavaBuildConfigurationBlock_invalid_input, number)); 
			}
		}
		return status;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#dispose()
	 */
	public void dispose() {
		IDialogSettings section= JavaScriptPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION_NAME);
		storeSectionExpansionStates(section);
		super.dispose();
	}
	
	protected void controlChanged(Widget widget) {
		if(widget == semanticCheckBox) {
			String newValue= semanticCheckBox.getSelection() ? ENABLED : DISABLED;
			ControlData data= (ControlData) widget.getData();
			String oldValue= setValue(data.getKey(), newValue);
			validateSettings(data.getKey(), oldValue, newValue);
		} else  {
			super.controlChanged(widget);
		}
	}
	
}
