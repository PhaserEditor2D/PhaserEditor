/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * Defines the constants used in the <code>org.eclipse.ui.themes</code>
 * extension contributed by this plug-in.
 * 
 * 
 */
public interface IJavaThemeConstants {

	String ID_PREFIX= JavaScriptUI.ID_PLUGIN + "."; //$NON-NLS-1$

	/**
	 * Theme constant for the color used to highlight matching brackets.
	 */
	public final String EDITOR_MATCHING_BRACKETS_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;

	/**
	 * Theme constant for the color used to render multi-line comments.
	 */
	public final String EDITOR_MULTI_LINE_COMMENT_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR;

	/**
	 * Theme constant for the color used to render java keywords.
	 */
	public final String EDITOR_JAVA_KEYWORD_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR;

	/**
	 * A theme constant that holds the color used to render string constants.
	 */
	public final String EDITOR_STRING_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_STRING_COLOR;

	/**
	 * A theme constant that holds the color used to render single line comments.
	 */
	public final String EDITOR_SINGLE_LINE_COMMENT_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR;

	/**
	 * A theme constant that holds the color used to render operators.
	 */
	public final String EDITOR_JAVA_OPERATOR_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVA_OPERATOR_COLOR;

	/**
	 * A theme constant that holds the color used to render java default text.
	 */
	public final String EDITOR_JAVA_DEFAULT_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR;

	/**
	 * A theme constant that holds the color used to render the 'return' keyword.
	 */
	public final String EDITOR_JAVA_KEYWORD_RETURN_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_COLOR;

	/**
	 * A theme constant that holds the color used to render javadoc keywords.
	 */
	public final String EDITOR_JAVADOC_KEYWORD_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVADOC_KEYWORD_COLOR;

	/**
	 * A theme constant that holds the color used to render javadoc tags.
	 */
	public final String EDITOR_JAVADOC_TAG_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVADOC_TAG_COLOR;

	/**
	 * A theme constant that holds the color used to render brackets.
	 */
	public final String EDITOR_JAVA_BRACKET_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVA_BRACKET_COLOR;

	/**
	 * A theme constant that holds the color used to render task tags.
	 */
	public final String EDITOR_TASK_TAG_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_TASK_TAG_COLOR;

	/**
	 * A theme constant that holds the color used to render javadoc links.
	 */
	public final String EDITOR_JAVADOC_LINKS_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR;

	/**
	 * A theme constant that holds the color used to render javadoc default text.
	 */
	public final String EDITOR_JAVADOC_DEFAULT_COLOR= ID_PREFIX + PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR;

	/**
	 * A theme constant that holds the background color used in the code assist selection dialog.
	 */
	public final String CODEASSIST_PROPOSALS_BACKGROUND= ID_PREFIX + PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND;

	/**
	 * A theme constant that holds the foreground color used in the code assist selection dialog.
	 */
	public final String CODEASSIST_PROPOSALS_FOREGROUND= ID_PREFIX + PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND;

	/**
	 * A theme constant that holds the background color used for parameter hints.
	 */
	public final String CODEASSIST_PARAMETERS_BACKGROUND= ID_PREFIX + PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND;

	/**
	 * A theme constant that holds the foreground color used in the code assist selection dialog.
	 */
	public final String CODEASSIST_PARAMETERS_FOREGROUND= ID_PREFIX + PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND;

	/**
	 * Theme constant for the background color used in the code assist selection dialog to mark replaced code.
	 */
	public final String CODEASSIST_REPLACEMENT_BACKGROUND= ID_PREFIX + PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND;

	/**
	 * Theme constant for the foreground color used in the code
	 * assist selection dialog to mark replaced code.
	 */
	public final String CODEASSIST_REPLACEMENT_FOREGROUND= ID_PREFIX + PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND;

	/**
	 * Theme constant for the color used to render values in a properties file.
	 */
	String PROPERTIES_FILE_COLORING_VALUE= ID_PREFIX + PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE;

	/**
	 * Theme constant for the color used to render keys in a properties file.
	 */
	String PROPERTIES_FILE_COLORING_KEY= ID_PREFIX + PreferenceConstants.PROPERTIES_FILE_COLORING_KEY;

	/**
	 * Theme constant for the color used to render arguments in a properties file.
	 */
	String PROPERTIES_FILE_COLORING_ARGUMENT= ID_PREFIX + PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT;

	/**
	 * Theme constant for the color used to render assignments in a properties file.
	 */
	String PROPERTIES_FILE_COLORING_ASSIGNMENT= ID_PREFIX +	PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT;

	/**
	 * Theme constant for the color used to render comments in a properties file.
	 */
	String PROPERTIES_FILE_COLORING_COMMENT= ID_PREFIX + PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT;
	
}
