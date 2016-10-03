/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

public class CleanUpConstants {
	
	/**
	 * False value
	 * 
	 * 
	 */
	public static final String FALSE= "false"; //$NON-NLS-1$
	
	/**
	 * True value
	 * 
	 * 
	 */
	public static final String TRUE= "true"; //$NON-NLS-1$
	
	/**
	 * Format Java Source Code <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String FORMAT_SOURCE_CODE= "cleanup.format_source_code"; //$NON-NLS-1$
	
	/**
	 * Format comments. Specify which comment with:<br>
	 * {@link #FORMAT_JAVADOC}<br>
	 * {@link #FORMAT_MULTI_LINE_COMMENT}<br>
	 * {@link #FORMAT_SINGLE_LINE_COMMENT} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 * @deprecated replaced by {@link #FORMAT_SOURCE_CODE}
	 */
	public static final String FORMAT_COMMENT= "cleanup.format_comment"; //$NON-NLS-1$
	
	/**
	 * Format single line comments. Only has an effect if
	 * {@link #FORMAT_COMMENT} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 * @deprecated replaced by {@link org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_LINE_COMMENT}
	 */
	public static final String FORMAT_SINGLE_LINE_COMMENT= "cleanup.format_single_line_comment"; //$NON-NLS-1$
	
	/**
	 * Format multi line comments. Only has an effect if {@link #FORMAT_COMMENT}
	 * is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 * @deprecated replaced by {@link org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT}
	 */
	public static final String FORMAT_MULTI_LINE_COMMENT= "cleanup.format_multi_line_comment"; //$NON-NLS-1$
	
	/**
	 * Format javadoc comments. Only has an effect if {@link #FORMAT_COMMENT} is
	 * TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 * @deprecated replaced by {@link org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT}
	 */
	public static final String FORMAT_JAVADOC= "cleanup.format_javadoc"; //$NON-NLS-1$
	
	/**
	 * Removes trailing whitespace in compilation units<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String FORMAT_REMOVE_TRAILING_WHITESPACES= "cleanup.remove_trailing_whitespaces"; //$NON-NLS-1$
	
	/**
	 * Removes trailing whitespace in compilation units on all lines<br>
	 * Only has an effect if {@link #FORMAT_REMOVE_TRAILING_WHITESPACES} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String FORMAT_REMOVE_TRAILING_WHITESPACES_ALL= "cleanup.remove_trailing_whitespaces_all"; //$NON-NLS-1$
	
	/**
	 * Removes trailing whitespace in compilation units on all lines which
	 * contain an other characters then whitespace<br>
	 * Only has an effect if {@link #FORMAT_REMOVE_TRAILING_WHITESPACES} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY= "cleanup.remove_trailing_whitespaces_ignore_empty"; //$NON-NLS-1$
	
	/**
	 * Controls access qualifiers for instance fields. For detailed settings use<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS}<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS= "cleanup.use_this_for_non_static_field_access"; //$NON-NLS-1$
	
	/**
	 * Adds a 'this' qualifier to field accesses.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                     int fField;
	 *                     void foo() {fField= 10;} -&gt; void foo() {this.fField= 10;}
	 * </pre></code> <br>
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS}
	 * is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS= "cleanup.always_use_this_for_non_static_field_access"; //$NON-NLS-1$
	
	/**
	 * Removes 'this' qualifier to field accesses.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                     int fField;
	 *                     void foo() {this.fField= 10;} -&gt; void foo() {fField= 10;}
	 * </pre></code> <br>
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS}
	 * is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY= "cleanup.use_this_for_non_static_field_access_only_if_necessary"; //$NON-NLS-1$
	
	/**
	 * Controls access qualifiers for instance methods. For detailed settings
	 * use<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS}<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS= "cleanup.use_this_for_non_static_method_access"; //$NON-NLS-1$
	
	/**
	 * Adds a 'this' qualifier to method accesses.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                     int method(){};
	 *                     void foo() {method()} -&gt; void foo() {this.method();}
	 * </pre></code> <br>
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS}
	 * is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS= "cleanup.always_use_this_for_non_static_method_access"; //$NON-NLS-1$
	
	/**
	 * Removes 'this' qualifier to field accesses.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                     int fField;
	 *                     void foo() {this.fField= 10;} -&gt; void foo() {fField= 10;}
	 * </pre></code> <br>
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS}
	 * is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY= "cleanup.use_this_for_non_static_method_access_only_if_necessary"; //$NON-NLS-1$
	
	/**
	 * Controls access qualifiers for static members. For detailed settings use<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD}<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS}<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD}<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS}
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS= "cleanup.qualify_static_member_accesses_with_declaring_class"; //$NON-NLS-1$
	
	/**
	 * Qualify static field accesses with declaring type.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   class E {
	 *                     public static int i;
	 *                     void foo() {i= 10;} -&gt; void foo() {E.i= 10;}
	 *                   }
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD= "cleanup.qualify_static_field_accesses_with_declaring_class"; //$NON-NLS-1$
	
	/**
	 * Qualifies static method accesses with declaring type.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   class E {
	 *                     public static int m();
	 *                     void foo() {m();} -&gt; void foo() {E.m();}
	 *                   }
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD= "cleanup.qualify_static_method_accesses_with_declaring_class"; //$NON-NLS-1$
	
	/**
	 * Changes indirect accesses to static members to direct ones.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   class E {public static int i;}
	 *                   class ESub extends E {
	 *                     void foo() {ESub.i= 10;} -&gt; void foo() {E.i= 10;}
	 *                   }
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS= "cleanup.qualify_static_member_accesses_through_subtypes_with_declaring_class"; //$NON-NLS-1$
	
	/**
	 * Changes non static accesses to static members to static accesses.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   class E {
	 *                     public static int i;
	 *                     void foo() {(new E()).i= 10;} -&gt; void foo() {E.i= 10;}
	 *                   }
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS= "cleanup.qualify_static_member_accesses_through_instances_with_declaring_class"; //$NON-NLS-1$
	
	/**
	 * Controls the usage of blocks around single control statement bodies. For
	 * detailed settings use<br>
	 * {@link #CONTROL_STATMENTS_USE_BLOCKS_ALWAYS}<br>
	 * {@link #CONTROL_STATMENTS_USE_BLOCKS_NEVER}<br>
	 * {@link #CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String CONTROL_STATEMENTS_USE_BLOCKS= "cleanup.use_blocks"; //$NON-NLS-1$
	
	/**
	 * Adds block to control statement body if the body is not a block.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   	 if (b) foo(); -&gt; if (b) {foo();}
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String CONTROL_STATMENTS_USE_BLOCKS_ALWAYS= "cleanup.always_use_blocks"; //$NON-NLS-1$
	
	/**
	 * Remove unnecessary blocks in control statement bodies if they contain a
	 * single return or throw statement.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                     if (b) {return;} -&gt; if (b) return;
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW= "cleanup.use_blocks_only_for_return_and_throw"; //$NON-NLS-1$
	
	/**
	 * Remove unnecessary blocks in control statement bodies.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                     if (b) {foo();} -&gt; if (b) foo();
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String CONTROL_STATMENTS_USE_BLOCKS_NEVER= "cleanup.never_use_blocks"; //$NON-NLS-1$
	
	/**
	 * Convert for loops to enhanced for loops.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   for (int i = 0; i &lt; array.length; i++) {} -&gt; for (int element : array) {}
	 * </code></pre>
	 * 
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED= "cleanup.convert_to_enhanced_for_loop"; //$NON-NLS-1$
	
	/**
	 * Controls the usage of parenthesis in expressions. For detailed settings
	 * use<br>
	 * {@link #EXPRESSIONS_USE_PARENTHESES_ALWAYS}<br>
	 * {@link #EXPRESSIONS_USE_PARENTHESES_NEVER}<br>
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String EXPRESSIONS_USE_PARENTHESES= "cleanup.use_parentheses_in_expressions"; //$NON-NLS-1$
	
	/**
	 * Add paranoic parenthesis around conditional expressions.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   boolean b= i &gt; 10 &amp;&amp; i &lt; 100 || i &gt; 20;
	 *                   -&gt;
	 *                   boolean b= ((i &gt; 10) &amp;&amp; (i &lt; 100)) || (i &gt; 20);
	 * </pre></code> <br>
	 * Only has an effect if {@link #EXPRESSIONS_USE_PARENTHESES} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String EXPRESSIONS_USE_PARENTHESES_ALWAYS= "cleanup.always_use_parentheses_in_expressions"; //$NON-NLS-1$
	
	/**
	 * Remove unnecessary parenthesis around conditional expressions.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   boolean b= ((i &gt; 10) &amp;&amp; (i &lt; 100)) || (i &gt; 20);
	 *                   -&gt;
	 *                   boolean b= i &gt; 10 &amp;&amp; i &lt; 100 || i &gt; 20;
	 * </pre></code> <br>
	 * Only has an effect if {@link #EXPRESSIONS_USE_PARENTHESES} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String EXPRESSIONS_USE_PARENTHESES_NEVER= "cleanup.never_use_parentheses_in_expressions"; //$NON-NLS-1$
	
	/**
	 * Controls the usage of 'final' modifier for variable declarations. For
	 * detailed settings use:<br>
	 * {@link #VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES}<br>
	 * {@link #VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS}<br>
	 * {@link #VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL= "cleanup.make_variable_declarations_final"; //$NON-NLS-1$
	
	/**
	 * Add a final modifier to private fields where possible i.e.:
	 * 
	 * <pre><code>
	 *                   private int field= 0; -&gt; private final int field= 0;
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if {@link #VARIABLE_DECLARATIONS_USE_FINAL} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS= "cleanup.make_private_fields_final"; //$NON-NLS-1$
	
	/**
	 * Add a final modifier to method parameters where possible i.e.:
	 * 
	 * <pre><code>
	 *                   void foo(int i) {} -&gt; void foo(final int i) {}
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if {@link #VARIABLE_DECLARATIONS_USE_FINAL} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS= "cleanup.make_parameters_final"; //$NON-NLS-1$
	
	/**
	 * Add a final modifier to local variables where possible i.e.:
	 * 
	 * <pre><code>
	 *                   int i= 0; -&gt; final int i= 0;
	 * </code></pre>
	 * 
	 * <br>
	 * Only has an effect if {@link #VARIABLE_DECLARATIONS_USE_FINAL} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES= "cleanup.make_local_variable_final"; //$NON-NLS-1$
	
	/**
	 * Adds type parameters to raw type references.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   List l; -&gt; List&lt;Object&gt; l;
	 * </code></pre>
	 * 
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Not set<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES= "cleanup.use_arguments_for_raw_type_references"; //$NON-NLS-1$
	
	/**
	 * Removes unused imports. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_IMPORTS= "cleanup.remove_unused_imports"; //$NON-NLS-1$
	
	/**
	 * Controls the removal of unused private members. For detailed settings
	 * use:<br>
	 * {@link #REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS}<br>
	 * {@link #REMOVE_UNUSED_CODE_PRIVATE_FELDS}<br>
	 * {@link #REMOVE_UNUSED_CODE_PRIVATE_METHODS}<br>
	 * {@link #REMOVE_UNUSED_CODE_PRIVATE_TYPES} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_MEMBERS= "cleanup.remove_unused_private_members"; //$NON-NLS-1$
	
	/**
	 * Removes unused private types. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_TYPES= "cleanup.remove_unused_private_types"; //$NON-NLS-1$
	
	/**
	 * Removes unused private constructors. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS= "cleanup.remove_private_constructors"; //$NON-NLS-1$
	
	/**
	 * Removes unused private fields. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_FELDS= "cleanup.remove_unused_private_fields"; //$NON-NLS-1$
	
	/**
	 * Removes unused private methods. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_METHODS= "cleanup.remove_unused_private_methods"; //$NON-NLS-1$
	
	/**
	 * Removes unused local variables. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNUSED_CODE_LOCAL_VARIABLES= "cleanup.remove_unused_local_variables"; //$NON-NLS-1$
	
	/**
	 * Removes unused casts. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNNECESSARY_CASTS= "cleanup.remove_unnecessary_casts"; //$NON-NLS-1$
	
	/**
	 * Remove unnecessary '$NON-NLS$' tags.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 * String s; //$NON-NLS-1$ -&gt; String s;
	 * </code></pre>
	 * 
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String REMOVE_UNNECESSARY_NLS_TAGS= "cleanup.remove_unnecessary_nls_tags"; //$NON-NLS-1$
	
	/**
	 * Controls whether missing annotations should be added to the code. For
	 * detailed settings use:<br>
	 * {@link #ADD_MISSING_ANNOTATIONS_DEPRECATED}<br>
	 * {@value #ADD_MISSING_ANNOTATIONS_OVERRIDE} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_ANNOTATIONS= "cleanup.add_missing_annotations"; //$NON-NLS-1$
	
	/**
	 * Add '@Override' annotation in front of overriding methods.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   class E1 {void foo();}
	 *                   class E2 extends E1 {
	 *                   	 void foo(); -&gt;  @Override void foo();
	 *                   }
	 * </pre></code> <br>
	 *           Only has an effect if {@link #ADD_MISSING_ANNOTATIONS} is TRUE
	 *           <br>
	 *           <br>
	 *           Possible values: {TRUE, FALSE}<br>
	 *           Default value: Value returned by
	 *           {@link #getEclipseDefaultSettings()}<br>
	 *           <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_ANNOTATIONS_OVERRIDE= "cleanup.add_missing_override_annotations"; //$NON-NLS-1$
	
	/**
	 * Add '@Deprecated' annotation in front of deprecated members.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                         /**@deprecated* /
	 *                        int i;
	 *                    -&gt;
	 *                         /**@deprecated* /
	 *                         @Deprecated
	 *                        int i;
	 * </pre></code> <br>
	 * Only has an effect if {@link #ADD_MISSING_ANNOTATIONS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_ANNOTATIONS_DEPRECATED= "cleanup.add_missing_deprecated_annotations"; //$NON-NLS-1$
	
	/**
	 * Controls whether missing serial version ids should be added to the code.
	 * For detailed settings use:<br>
	 * {@link #ADD_MISSING_SERIAL_VERSION_ID_DEFAULT}<br>
	 * {@link #ADD_MISSING_SERIAL_VERSION_ID_GENERATED} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_SERIAL_VERSION_ID= "cleanup.add_serial_version_id"; //$NON-NLS-1$
	
	/**
	 * Adds a generated serial version id to subtypes of java.io.Serializable
	 * and java.io.Externalizable
	 * 
	 * public class E implements Serializable {} -> public class E implements
	 * Serializable { private static final long serialVersionUID = 4381024239L; }
	 * <br>
	 * Only has an effect if {@link #ADD_MISSING_SERIAL_VERSION_ID} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_SERIAL_VERSION_ID_GENERATED= "cleanup.add_generated_serial_version_id"; //$NON-NLS-1$
	
	/**
	 * Adds a default serial version it to subtypes of java.io.Serializable and
	 * java.io.Externalizable
	 * 
	 * public class E implements Serializable {} -> public class E implements
	 * Serializable { private static final long serialVersionUID = 1L; } <br>
	 * Only has an effect if {@link #ADD_MISSING_SERIAL_VERSION_ID} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_SERIAL_VERSION_ID_DEFAULT= "cleanup.add_default_serial_version_id"; //$NON-NLS-1$
	
	/**
	 * Add '$NON-NLS$' tags to non externalized strings.
	 * <p>
	 * i.e.:
	 * 
	 * <pre><code>
	 *                   	 String s= &quot;&quot;; -&gt; String s= &quot;&quot;; //$NON-NLS-1$
	 * </code></pre>
	 * 
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ADD_MISSING_NLS_TAGS= "cleanup.add_missing_nls_tags"; //$NON-NLS-1$
	
	/**
	 * If true the imports are organized while cleaning up code.
	 * 
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String ORGANIZE_IMPORTS= "cleanup.organize_imports"; //$NON-NLS-1$
	
	/**
	 * Should members be sorted?
	 * <br><br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #SORT_MEMBERS_ALL
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String SORT_MEMBERS = "cleanup.sort_members"; //$NON-NLS-1$

	/**
	 * If sorting members, should fields, enum constants and initializers also be sorted?
	 * <br>
	 * This has only an effect if {@link #SORT_MEMBERS} is also enabled.
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * Default value: Value returned by {@link #getEclipseDefaultSettings()}<br>
	 * <br>
	 * 
	 * @see #SORT_MEMBERS
	 * @see #TRUE
	 * @see #FALSE
	 * 
	 */
	public static final String SORT_MEMBERS_ALL = "cleanup.sort_members_all"; //$NON-NLS-1$

	/**
	 * Should the Clean Up Wizard be shown when executing the Clean Up Action?
	 * <br>
	 * <br>
	 * Possible values: {<code><b>true</b></code>,
	 * <code><b>false</b></code>}<br>
	 * Default value: <code><b>true</b></code><br>
	 * <br>
	 * 
	 * 
	 */
	public static final String SHOW_CLEAN_UP_WIZARD= "cleanup.showwizard"; //$NON-NLS-1$
	
	/**
	 * A key to a serialized string in the <code>InstanceScope</code>
	 * containing all the profiles.<br>
	 * Following code snippet can load the profiles:
	 * 
	 * <pre><code>
	 * List profiles= new ProfileStore(CLEANUP_PROFILES, new CleanUpVersioner()).readProfiles(new InstanceScope());
	 * </code></pre>
	 * 
	 * 
	 */
	public static final String CLEANUP_PROFILES= "org.eclipse.wst.jsdt.ui.cleanupprofiles"; //$NON-NLS-1$
	
	/**
	 * Stores the id of the clean up profile used when executing clean up.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #DEFAULT_PROFILE} <br>
	 * 
	 * 
	 */
	public final static String CLEANUP_PROFILE= "cleanup_profile"; //$NON-NLS-1$$
	
	/**
	 * Stores the id of the clean up profile used when executing clean up on
	 * save.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #DEFAULT_SAVE_PARTICIPANT_PROFILE} <br>
	 * 
	 * 
	 */
	public static final String CLEANUP_ON_SAVE_PROFILE= "cleanup.on_save_profile_id"; //$NON-NLS-1$
	
	/**
	 * A key to the version of the profile stored in the preferences.<br>
	 * <br>
	 * Possible values: Integer value<br>
	 * Default value: {@link org.eclipse.wst.jsdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner#CURRENT_VERSION} <br>
	 * 
	 * 
	 */
	public final static String CLEANUP_SETTINGS_VERSION_KEY= "cleanup_settings_version"; //$NON-NLS-1$
	
	/**
	 * Id of the 'Eclipse [built-in]' profile.<br>
	 * <br>
	 * 
	 * 
	 */
	public final static String ECLIPSE_PROFILE= "org.eclipse.wst.jsdt.ui.default.eclipse_clean_up_profile"; //$NON-NLS-1$
	
	/**
	 * Id of the 'Save Participant [built-in]' profile.<br>
	 * <br>
	 * 
	 * 
	 */
	public final static String SAVE_PARTICIPANT_PROFILE= "org.eclipse.wst.jsdt.ui.default.save_participant_clean_up_profile"; //$NON-NLS-1$
	
	public static final String CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS= "cleanup.on_save_use_additional_actions"; //$NON-NLS-1$
	
	/**
	 * The id of the profile used as a default profile when executing clean up.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #ECLIPSE_PROFILE} <br>
	 * 
	 * 
	 */
	public final static String DEFAULT_PROFILE= ECLIPSE_PROFILE;
	
	/**
	 * The id of the profile used as a default profile when executing clean up
	 * on save.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #SAVE_PARTICIPANT_PROFILE} <br>
	 * 
	 * 
	 */
	public final static String DEFAULT_SAVE_PARTICIPANT_PROFILE= SAVE_PARTICIPANT_PROFILE;
	
	public static Map getEclipseDefaultSettings() {
		final HashMap result= new HashMap();
		
		//Member Accesses
		result.put(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY, TRUE);
		
		result.put(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY, TRUE);
		
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, TRUE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, FALSE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, FALSE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, TRUE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, TRUE);
		
		//Control Statements
		result.put(CONTROL_STATEMENTS_USE_BLOCKS, FALSE);
		result.put(CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, TRUE);
		result.put(CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, FALSE);
		result.put(CONTROL_STATMENTS_USE_BLOCKS_NEVER, FALSE);
		
		result.put(CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, FALSE);
		
		//Expressions
		result.put(EXPRESSIONS_USE_PARENTHESES, FALSE);
		result.put(EXPRESSIONS_USE_PARENTHESES_NEVER, TRUE);
		result.put(EXPRESSIONS_USE_PARENTHESES_ALWAYS, FALSE);
		
		//Variable Declarations
		result.put(VARIABLE_DECLARATIONS_USE_FINAL, FALSE);
		result.put(VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, TRUE);
		result.put(VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, FALSE);
		result.put(VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, TRUE);
		
		//Unused Code
		result.put(REMOVE_UNUSED_CODE_IMPORTS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, FALSE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_FELDS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_METHODS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_TYPES, TRUE);
		result.put(REMOVE_UNUSED_CODE_LOCAL_VARIABLES, FALSE);
		
		//Unnecessary Code
		result.put(REMOVE_UNNECESSARY_CASTS, TRUE);
		result.put(REMOVE_UNNECESSARY_NLS_TAGS, TRUE);
		
		//Missing Code
		result.put(ADD_MISSING_ANNOTATIONS, TRUE);
		result.put(ADD_MISSING_ANNOTATIONS_OVERRIDE, TRUE);
		result.put(ADD_MISSING_ANNOTATIONS_DEPRECATED, TRUE);
		
		result.put(ADD_MISSING_SERIAL_VERSION_ID, FALSE);
		result.put(ADD_MISSING_SERIAL_VERSION_ID_GENERATED, FALSE);
		result.put(ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, TRUE);
		
		result.put(ADD_MISSING_NLS_TAGS, FALSE);
		
		//Code Organising
		result.put(FORMAT_SOURCE_CODE, FALSE);
		
		result.put(FORMAT_REMOVE_TRAILING_WHITESPACES, FALSE);
		result.put(FORMAT_REMOVE_TRAILING_WHITESPACES_ALL, TRUE);
		result.put(FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY, FALSE);
		
		result.put(ORGANIZE_IMPORTS, FALSE);

		result.put(SORT_MEMBERS, FALSE);
		result.put(SORT_MEMBERS_ALL, FALSE);
		
		return result;
	}
	
	public static Map getSaveParticipantSettings() {
		final HashMap result= new HashMap();
		
		//Member Accesses
		result.put(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY, TRUE);
		
		result.put(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, FALSE);
		result.put(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY, TRUE);
		
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, FALSE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, FALSE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, FALSE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, TRUE);
		result.put(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, TRUE);
		
		//Control Statements
		result.put(CONTROL_STATEMENTS_USE_BLOCKS, FALSE);
		result.put(CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, TRUE);
		result.put(CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, FALSE);
		result.put(CONTROL_STATMENTS_USE_BLOCKS_NEVER, FALSE);
		
		result.put(CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, FALSE);
		
		//Expressions
		result.put(EXPRESSIONS_USE_PARENTHESES, FALSE);
		result.put(EXPRESSIONS_USE_PARENTHESES_NEVER, TRUE);
		result.put(EXPRESSIONS_USE_PARENTHESES_ALWAYS, FALSE);
		
		//Variable Declarations
		result.put(VARIABLE_DECLARATIONS_USE_FINAL, TRUE);
		result.put(VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, FALSE);
		result.put(VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, FALSE);
		result.put(VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, TRUE);
		
		//Unused Code
		result.put(REMOVE_UNUSED_CODE_IMPORTS, FALSE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, FALSE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_FELDS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_METHODS, TRUE);
		result.put(REMOVE_UNUSED_CODE_PRIVATE_TYPES, TRUE);
		result.put(REMOVE_UNUSED_CODE_LOCAL_VARIABLES, FALSE);
		
		//Unnecessary Code
		result.put(REMOVE_UNNECESSARY_CASTS, TRUE);
		result.put(REMOVE_UNNECESSARY_NLS_TAGS, FALSE);
		
		//Missing Code
		result.put(ADD_MISSING_ANNOTATIONS, TRUE);
		result.put(ADD_MISSING_ANNOTATIONS_OVERRIDE, TRUE);
		result.put(ADD_MISSING_ANNOTATIONS_DEPRECATED, TRUE);
		
		result.put(ADD_MISSING_SERIAL_VERSION_ID, FALSE);
		result.put(ADD_MISSING_SERIAL_VERSION_ID_GENERATED, FALSE);
		result.put(ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, TRUE);
		
		result.put(ADD_MISSING_NLS_TAGS, FALSE);
		
		//Code Organising
		result.put(FORMAT_SOURCE_CODE, FALSE);
		
		result.put(FORMAT_REMOVE_TRAILING_WHITESPACES, FALSE);
		result.put(FORMAT_REMOVE_TRAILING_WHITESPACES_ALL, TRUE);
		result.put(FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY, FALSE);
		
		result.put(ORGANIZE_IMPORTS, TRUE);

		result.put(SORT_MEMBERS, FALSE);
		result.put(SORT_MEMBERS_ALL, FALSE);
		
		result.put(CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, FALSE);
		
		return result;
	}
	
	public static void initDefaults(IPreferenceStore store) {
		final Map settings= getEclipseDefaultSettings();
		for (final Iterator iterator= settings.keySet().iterator(); iterator.hasNext();) {
			final String key= (String)iterator.next();
			store.setDefault(key, (String)settings.get(key));
		}
		
		store.setDefault(SHOW_CLEAN_UP_WIZARD, true);
		store.setDefault(CLEANUP_PROFILE, DEFAULT_PROFILE);
		store.setDefault(CLEANUP_ON_SAVE_PROFILE, DEFAULT_SAVE_PARTICIPANT_PROFILE);
	}
	
}