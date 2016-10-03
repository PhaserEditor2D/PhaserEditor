/*******************************************************************************
 * Copyright (c) 2000, 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;

/**
 * JavaScriptCore eclipse preferences initializer.
 * Initially done in JavaScriptCore.initializeDefaultPreferences which was deprecated
 * with new eclipse preferences mechanism.
 */
public class JavaCorePreferenceInitializer extends AbstractPreferenceInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		// If modified, also modify the method JavaModelManager#getDefaultOptionsNoInitialization()
		// Get options names set
		HashSet optionNames = JavaModelManager.getJavaModelManager().optionNames;

		// Compiler settings
		Map defaultOptionsMap = new CompilerOptions().getMap(); // compiler defaults

		// Override some compiler defaults
		defaultOptionsMap.put(JavaScriptCore.COMPILER_LOCAL_VARIABLE_ATTR, JavaScriptCore.GENERATE);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_CODEGEN_UNUSED_LOCAL, JavaScriptCore.PRESERVE);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_TASK_TAGS, JavaScriptCore.DEFAULT_TASK_TAGS);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_TASK_PRIORITIES, JavaScriptCore.DEFAULT_TASK_PRIORITIES);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_TASK_CASE_SENSITIVE, JavaScriptCore.ENABLED);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT, JavaScriptCore.ENABLED);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaScriptCore.ERROR);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_SEMANTIC_VALIDATION, JavaScriptCore.DISABLED);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_STRICT_ON_KEYWORD_USAGE, JavaScriptCore.ENABLED);

		// Builder settings
		defaultOptionsMap.put(JavaScriptCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, JavaScriptCore.ABORT);
		defaultOptionsMap.put(JavaScriptCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE, JavaScriptCore.WARNING);
		defaultOptionsMap.put(JavaScriptCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, JavaScriptCore.CLEAN);
		defaultOptionsMap.put(JavaScriptCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER, JavaScriptCore.IGNORE);

		// JavaScriptCore settings
		defaultOptionsMap.put(JavaScriptCore.CORE_JAVA_BUILD_ORDER, JavaScriptCore.IGNORE);
		defaultOptionsMap.put(JavaScriptCore.CORE_INCOMPLETE_CLASSPATH, JavaScriptCore.ERROR);
		defaultOptionsMap.put(JavaScriptCore.CORE_CIRCULAR_CLASSPATH, JavaScriptCore.ERROR);
		defaultOptionsMap.put(JavaScriptCore.CORE_INCOMPATIBLE_JDK_LEVEL, JavaScriptCore.IGNORE);
		defaultOptionsMap.put(JavaScriptCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS, JavaScriptCore.ENABLED);
		defaultOptionsMap.put(JavaScriptCore.CORE_DEFAULT_CLASSPATH_EXCLUSION_PATTERNS, JavaScriptCore.DEFAULT_EXCLUSION_PATTERNS);
		defaultOptionsMap.put(JavaScriptCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS, JavaScriptCore.ENABLED);

		// encoding setting comes from resource plug-in
		optionNames.add(JavaScriptCore.CORE_ENCODING);

		// Formatter settings
		Map codeFormatterOptionsMap = DefaultCodeFormatterConstants.getEclipseDefaultSettings(); // code formatter defaults
		for (Iterator iter = codeFormatterOptionsMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String optionName = (String) entry.getKey();
			defaultOptionsMap.put(optionName, entry.getValue());
			optionNames.add(optionName);
		}

		// CodeAssist settings
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_VISIBILITY_CHECK, JavaScriptCore.DISABLED);
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_DEPRECATION_CHECK, JavaScriptCore.DISABLED);
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_IMPLICIT_QUALIFICATION, JavaScriptCore.DISABLED);
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_FIELD_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_STATIC_FIELD_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_LOCAL_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_ARGUMENT_PREFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_FIELD_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_STATIC_FIELD_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_LOCAL_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_ARGUMENT_SUFFIXES, ""); //$NON-NLS-1$
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK, JavaScriptCore.ENABLED);
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK, JavaScriptCore.DISABLED);
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_CAMEL_CASE_MATCH, JavaScriptCore.ENABLED);
		defaultOptionsMap.put(JavaScriptCore.CODEASSIST_SUGGEST_STATIC_IMPORTS, JavaScriptCore.ENABLED);

		/* START -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
		/*
		 * Default ERROR for unresolved types/fields/methods
		 */
		defaultOptionsMap.put(JavaScriptCore.UNRESOLVED_TYPE_REFERENCE, JavaScriptCore.ERROR);
		defaultOptionsMap.put(JavaScriptCore.UNRESOLVED_FIELD_REFERENCE, JavaScriptCore.ERROR);
		defaultOptionsMap.put(JavaScriptCore.UNRESOLVED_METHOD_REFERENCE, JavaScriptCore.ERROR);
		/* END -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
		/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
		defaultOptionsMap.put(JavaScriptCore.LOOSE_VAR_DECL, JavaScriptCore.WARNING);
		defaultOptionsMap.put(JavaScriptCore.OPTIONAL_SEMICOLON, JavaScriptCore.WARNING);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_PB_DUPLICATE_LOCAL_VARIABLES, JavaScriptCore.WARNING);
		defaultOptionsMap.put(JavaScriptCore.COMPILER_PB_UNINITIALIZED_LOCAL_VARIABLE, JavaScriptCore.WARNING);
		/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */

		/* START -------------------------------- Bug 417465 - JavaScript Validation reports max 100 problems per .js file --------------------- */
		defaultOptionsMap.put(JavaScriptCore.COMPILER_PB_MAX_PER_UNIT, String.valueOf(100));
		/* END   -------------------------------- Bug 417465 - JavaScript Validation reports max 100 problems per .js file --------------------- */	

		// Time out for parameter names
		defaultOptionsMap.put(JavaScriptCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, "50"); //$NON-NLS-1$

		// Store default values to default preferences
	 	IEclipsePreferences defaultPreferences = ((IScopeContext) new DefaultScope()).getNode(JavaScriptCore.PLUGIN_ID);
		for (Iterator iter = defaultOptionsMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String optionName = (String) entry.getKey();
			defaultPreferences.put(optionName, (String)entry.getValue());
			optionNames.add(optionName);
		}
	}
}
