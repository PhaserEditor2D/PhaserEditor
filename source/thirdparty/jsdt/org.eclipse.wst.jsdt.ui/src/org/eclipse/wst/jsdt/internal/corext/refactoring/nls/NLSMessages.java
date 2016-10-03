/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import org.eclipse.osgi.util.NLS;

public final class NLSMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSMessages";//$NON-NLS-1$

	private NLSMessages() {
		// Do not instantiate
	}

	public static String NLSSourceModifier_replace_value;
	public static String NLSSourceModifier_replace_key;
	public static String NLSSourceModifier_externalize;
	public static String NLSSourceModifier_remove_tag;
	public static String NLSSourceModifier_change_description;
	public static String NLSSourceModifier_replace_accessor;
	public static String NLSSourceModifier_remove_accessor;
	public static String NLSSourceModifier_add_tag;
	public static String NLSRefactoring_compilation_unit;
	public static String NLSRefactoring_checking;
	public static String NLSRefactoring_pattern_empty;
	public static String NLSRefactoring_pattern_does_not_contain;
	public static String NLSRefactoring_Only_the_first_occurrence_of;
	public static String NLSRefactoring_null;
	public static String NLSRefactoring_empty;
	public static String NLSRefactoring_should_not_contain;
	public static String NLSRefactoring_nothing_to_do;
	public static String NLSRefactoring_will_be_created;
	public static String NLSRefactoring_no_strings;
	public static String NLSRefactoring_warning;
	public static String NLSRefactoring_change_name;
	public static String AccessorClassModifier_add_entry;
	public static String AccessorClassModifier_remove_entry;
	public static String AccessorClassModifier_replace_entry;
	public static String AccessorClassModifier_missingType;
	public static String NLSPropertyFileModifier_add_entry;
	public static String NLSPropertyFileModifier_change_name;
	public static String NLSPropertyFileModifier_replace_entry;
	public static String NLSPropertyFileModifier_remove_entry;

	static {
		NLS.initializeMessages(BUNDLE_NAME, NLSMessages.class);
	}
}
