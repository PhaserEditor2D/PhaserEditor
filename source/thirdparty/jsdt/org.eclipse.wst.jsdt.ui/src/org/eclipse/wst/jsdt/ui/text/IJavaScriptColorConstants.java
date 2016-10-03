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
package org.eclipse.wst.jsdt.ui.text;

/**
 * Color keys used for syntax highlighting Java
 * code and Javadoc compliant comments.
 * A <code>IColorManager</code> is responsible for mapping
 * concrete colors to these keys.
 * <p>
 * This interface declares static final fields only; it is not intended to be
 * implemented.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. *
 * @see org.eclipse.wst.jsdt.ui.text.IColorManager
 * @see org.eclipse.wst.jsdt.ui.text.IColorManagerExtension
 */
public interface IJavaScriptColorConstants {

	/**
	 * Note: This constant is for internal use only. Clients should not use this constant.
	 * The prefix all color constants start with
	 * (value <code>"java_"</code>).
	 */
	String PREFIX= "java_"; //$NON-NLS-1$

	/** The color key for multi-line comments in JavaScript code
	 * (value <code>"java_multi_line_comment"</code>).
	 */
	String JAVA_MULTI_LINE_COMMENT= "java_multi_line_comment"; //$NON-NLS-1$

	/** The color key for single-line comments in JavaScript code
	 * (value <code>"java_single_line_comment"</code>).
	 */
	String JAVA_SINGLE_LINE_COMMENT= "java_single_line_comment"; //$NON-NLS-1$

	/** The color key for JavaScript keywords in JavaScript code
	 * (value <code>"java_keyword"</code>).
	 */
	String JAVA_KEYWORD= "java_keyword"; //$NON-NLS-1$

	/** The color key for string and character literals in JavaScript code
	 * (value <code>"java_string"</code>).
	 */
	String JAVA_STRING= "java_string"; //$NON-NLS-1$

	/** The color key for method names in JavaScript code
	 * (value <code>"java_method_name"</code>).
	 *
	 * 
	 * @deprecated replaced as of 3.1 by an equivalent semantic highlighting, see {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.SemanticHighlightings#METHOD}
	 */
	String JAVA_METHOD_NAME= "java_method_name"; //$NON-NLS-1$

	/** The color key for keyword 'return' in JavaScript code
	 * (value <code>"java_keyword_return"</code>).
	 *
	 * 
	 */
	String JAVA_KEYWORD_RETURN= "java_keyword_return"; //$NON-NLS-1$

	/** The color key for operators in JavaScript code
	 * (value <code>"java_operator"</code>).
	 *
	 * 
	 */
	String JAVA_OPERATOR= "java_operator"; //$NON-NLS-1$

	/** The color key for brackets in JavaScript code
	 * (value <code>"java_bracket"</code>).
	 *
	 * 
	 */
	String JAVA_BRACKET= "java_bracket"; //$NON-NLS-1$

	/**
	 * The color key for everything in JavaScript code for which no other color is specified
	 * (value <code>"java_default"</code>).
	 */
	String JAVA_DEFAULT= "java_default"; //$NON-NLS-1$

	/**
	 * The color key for annotations
	 * (value <code>"java_annotation"</code>).
	 *
	 * 
	 * @deprecated replaced as of 3.2 by an equivalent semantic highlighting, see {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.SemanticHighlightings#ANNOTATION}
	 */
	String JAVA_ANNOTATION= "java_annotation"; //$NON-NLS-1$

	/**
	 * The color key for task tags in JavaScript comments
	 * (value <code>"java_comment_task_tag"</code>).
	 *
	 * 
	 */
	String TASK_TAG= "java_comment_task_tag"; //$NON-NLS-1$

	/**
	 * The color key for JavaDoc keywords (<code>@foo</code>) in JavaDoc comments
	 * (value <code>"java_doc_keyword"</code>).
	 */
	String JAVADOC_KEYWORD= "java_doc_keyword"; //$NON-NLS-1$

	/**
	 * The color key for HTML tags (<code>&lt;foo&gt;</code>) in JavaDoc comments
	 * (value <code>"java_doc_tag"</code>).
	 */
	String JAVADOC_TAG= "java_doc_tag"; //$NON-NLS-1$

	/**
	 * The color key for JavaDoc links (<code>{foo}</code>) in JavaDoc comments
	 * (value <code>"java_doc_link"</code>).
	 */
	String JAVADOC_LINK= "java_doc_link"; //$NON-NLS-1$

	/**
	 * The color key for everything in JavaDoc comments for which no other color is specified
	 * (value <code>"java_doc_default"</code>).
	 */
	String JAVADOC_DEFAULT= "java_doc_default"; //$NON-NLS-1$

	//---------- Properties File Editor ----------

	/**
	 * The color key for keys in a properties file
	 * (value <code>"pf_coloring_key"</code>).
	 *
	 * 
	 */
	String PROPERTIES_FILE_COLORING_KEY= "pf_coloring_key"; //$NON-NLS-1$

	/**
	 * The color key for comments in a properties file
	 * (value <code>"pf_coloring_comment"</code>).
	 *
	 * 
	 */

	String PROPERTIES_FILE_COLORING_COMMENT= "pf_coloring_comment"; //$NON-NLS-1$

	/**
	 * The color key for values in a properties file
	 * (value <code>"pf_coloring_value"</code>).
	 *
	 * 
	 */
	String PROPERTIES_FILE_COLORING_VALUE= "pf_coloring_value"; //$NON-NLS-1$

	/**
	 * The color key for assignment in a properties file.
	 * (value <code>"pf_coloring_assignment"</code>).
	 *
	 * 
	 */
	String PROPERTIES_FILE_COLORING_ASSIGNMENT= "pf_coloring_assignment"; //$NON-NLS-1$

	/**
	 * The color key for arguments in values in a properties file.
	 * (value <code>"pf_coloring_argument"</code>).
	 *
	 * 
	 */
	String PROPERTIES_FILE_COLORING_ARGUMENT= "pf_coloring_argument"; //$NON-NLS-1$
}
