/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.compiler;

import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblem;

/**
 * Richer description of a JavaScript problem, as detected by the compiler or some of the underlying
 * technology reusing the compiler. With the introduction of <code>validationParticipant</code>,
 * the simpler problem interface <code>IProblem</code> did not carry enough information to better
 * separate and categorize JavaScript problems. In order to minimize impact on existing API, JavaScript problems
 * are still passed around as <code>IProblem</code>, though actual implementations should explicitly
 * extend <code>CategorizedProblem</code>. Participants can produce their own problem definitions,
 * and given these are categorized problems, they can be better handled by clients (such as user
 * interface).
 *
 * A categorized problem provides access to:
 * <ul>
 * <li> its location (originating source file name, source position, line number), </li>
 * <li> its message description and a predicate to check its severity (warning or error). </li>
 * <li> its ID : a number identifying the very nature of this problem. All possible IDs for standard JavaScript
 * problems are listed as constants on <code>IProblem</code>, </li>
 * <li> its marker type : a string identifying the problem creator. It corresponds to the marker type
 * chosen if this problem was to be persisted. Standard JavaScript problems are associated to marker
 * type "org.eclipse.wst.jsdt.core.problem"), </li>
 * <li> its category ID : a number identifying the category this problem belongs to. All possible IDs for
 * standard JavaScript problem categories are listed in this class. </li>
 * </ul>
 *
 * Note: the compiler produces IProblems internally, which are turned into markers by the validator
 * so as to persist problem descriptions. This explains why there is no API allowing to reach IProblem detected
 * when validating. However, the JavaScript problem markers carry equivalent information to IProblem, in particular
 * their ID (attribute "id") is set to one of the IDs defined on this interface.
 *
 * Note: Standard JavaScript problems produced by JavaScript default tooling will be subclasses of this class. Technically, most
 * API methods dealing with problems are referring to <code>IProblem</code> for backward compatibility reason.
 * It is intended that <code>CategorizedProblem</code> will be subclassed for custom problem implementation when
 * participating in compilation operations, so as to allow participant to contribute their own marker types, and thus
 * defining their own domain specific problem/category IDs.
 *
 * Note: standard JavaScript problems produced by JavaScript default tooling will set the
 * marker IMarker#GENERATED_BY attribute to JavaBuilder#GENERATED_BY; compiler
 * participants may specify the IMarker#GENERATED_BY attribute of their markers
 * by adding it to the extra marker attributes of the problems they generate;
 * markers resulting from compiler participants' problems that do not have the
 * IMarker#GENERATED_BY extra attribute set do not have the IMarker#GENERATED_BY
 * attribute set either.
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class CategorizedProblem implements IProblem {

	/**
	 * List of standard category IDs used by JavaScript problems, more categories will be added
	 * in the future.
	 */
	public static final int CAT_UNSPECIFIED = 0;
	/** Category for problems related to buildpath */
	public static final int CAT_BUILDPATH = 10;
	/** Category for fatal problems related to syntax */
	public static final int CAT_SYNTAX = 20;
	/** Category for fatal problems in import statements */
	public static final int CAT_IMPORT = 30;
	/** Category for fatal problems related to types, could be addressed by some type change */
	public static final int CAT_TYPE = 40;
	/** Category for fatal problems related to type members, could be addressed by some field or method change */
	public static final int CAT_MEMBER = 50;
	/** Category for fatal problems which could not be addressed by external changes, but require an edit to be addressed */
	public static final int CAT_INTERNAL = 60;
	/** Category for optional problems in Javadoc */
	public static final int CAT_JAVADOC = 70;
	/** Category for optional problems related to coding style practices */
	public static final int CAT_CODE_STYLE = 80;
	/** Category for optional problems related to potential programming flaws */
	public static final int CAT_POTENTIAL_PROGRAMMING_PROBLEM = 90;
	/** Category for optional problems related to naming conflicts */
	public static final int CAT_NAME_SHADOWING_CONFLICT = 100;
	/** Category for optional problems related to unnecessary code */
	public static final int CAT_UNNECESSARY_CODE = 120;
	/** Category for optional problems related to access restrictions */
	public static final int CAT_RESTRICTION = 150;

/**
 * Returns an integer identifying the category of this problem. Categories, like problem IDs are
 * defined in the context of some marker type. Custom implementations of <code>CategorizedProblem</code>
 * may choose arbitrary values for problem/category IDs, as long as they are associated with a different
 * marker type.
 * Standard JavaScript problem markers (i.e. marker type is "org.eclipse.wst.jsdt.core.problem") carry an
 * attribute "categoryId" persisting the originating problem category ID as defined by this method).
 * @return id - an integer identifying the category of this problem
 */
public abstract int getCategoryID();

/**
 * Returns the marker type associated to this problem, if it gets persisted into a marker by the JavaBuilder
 * Standard JavaScript problems are associated to marker type "org.eclipse.wst.jsdt.core.problem").
 * Note: problem markers are expected to extend "org.eclipse.core.resources.problemmarker" marker type.
 * @return the type of the marker which would be associated to the problem
 */
public abstract String getMarkerType();

/**
 * Returns the names of the extra marker attributes associated to this problem when persisted into a marker
 * by the JavaBuilder. Extra attributes are only optional, and are allowing client customization of generated
 * markers. By default, no EXTRA attributes is persisted, and a categorized problem only persists the following attributes:
 * <ul>
 * <li>	<code>IMarker#MESSAGE</code> -&gt; {@link IProblem#getMessage()}</li>
 * <li>	<code>IMarker#SEVERITY</code> -&gt; <code> IMarker#SEVERITY_ERROR</code> or
 *         <code>IMarker#SEVERITY_WARNING</code> depending on {@link IProblem#isError()} or {@link IProblem#isWarning()}</li>
 * <li>	<code>IJavaScriptModelMarker#ID</code> -&gt; {@link IProblem#getID()}</li>
 * <li>	<code>IMarker#CHAR_START</code>  -&gt; {@link IProblem#getSourceStart()}</li>
 * <li>	<code>IMarker#CHAR_END</code>  -&gt; {@link IProblem#getSourceEnd()}</li>
 * <li>	<code>IMarker#LINE_NUMBER</code>  -&gt; {@link IProblem#getSourceLineNumber()}</li>
 * <li>	<code>IJavaScriptModelMarker#ARGUMENTS</code>  -&gt; some <code>String[]</code> used to compute quickfixes </li>
 * <li>	<code>IJavaScriptModelMarker#CATEGORY_ID</code> -&gt; {@link CategorizedProblem#getCategoryID()}</li>
 * </ul>
 * The names must be eligible for marker creation, as defined by <code>IMarker#setAttributes(String[], Object[])</code>,
 * and there must be as many names as values according to {@link #getExtraMarkerAttributeValues()}.
 * Note that extra marker attributes will be inserted after default ones (as described in {@link CategorizedProblem#getMarkerType()},
 * and thus could be used to override defaults.
 * @return the names of the corresponding marker attributes
 */
public String[] getExtraMarkerAttributeNames() {
	return CharOperation.NO_STRINGS;
}

/**
 * Returns the respective values for the extra marker attributes associated to this problem when persisted into
 * a marker by the JavaBuilder. Each value must correspond to a matching attribute name, as defined by
 * {@link #getExtraMarkerAttributeNames()}.
 * The values must be eligible for marker creation, as defined by <code>IMarker#setAttributes(String[], Object[])</code>.
 * @return the values of the corresponding extra marker attributes
 */
public Object[] getExtraMarkerAttributeValues() {
	return DefaultProblem.EMPTY_VALUES;
}

public void setMessage(String msg)
{
	
}
}
