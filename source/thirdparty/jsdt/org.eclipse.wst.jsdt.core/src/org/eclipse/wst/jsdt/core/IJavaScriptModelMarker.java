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
package org.eclipse.wst.jsdt.core;

/**
 * Markers used by the JavaScript model.
 * <p>
 * This interface declares constants only; it is not intended to be implemented
 * or extended.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptModelMarker {

	/**
	 * JavaScript model problem marker type (value
	 * <code>"org.eclipse.wst.jsdt.core.problem"</code>). This can be used to
	 * recognize those markers in the workspace that flag problems detected by
	 * the JavaScript tooling during validation.
	 */
	String JAVASCRIPT_MODEL_PROBLEM_MARKER = JavaScriptCore.PLUGIN_ID + ".problem"; //$NON-NLS-1$

	/**
	 * JavaScript model transient problem marker type (value
	 * <code>"org.eclipse.wst.jsdt.core.transient_problem"</code>). This can be
	 * used to recognize those markers in the workspace that flag transient
	 * problems detected by the JavaScript tooling (such as a problem detected by the
	 * outliner, or a problem detected during a code completion). Since 1.0,
	 * transient problems are reported as <code>IProblem</code> through
	 * various API. Only the evaluation API is still producing markers for
	 * transient problems.
	 *
	 * @see org.eclipse.wst.jsdt.core.compiler.IProblem
	 * @see org.eclipse.wst.jsdt.core.eval.ICodeSnippetRequestor#acceptProblem(org.eclipse.core.resources.IMarker,String,
	 *      int)
	 */
	String TRANSIENT_PROBLEM = JavaScriptCore.PLUGIN_ID + ".transient_problem"; //$NON-NLS-1$

	/**
	 * JavaScript model task marker type (value
	 * <code>"org.eclipse.wst.jsdt.core.task"</code>). This can be used to
	 * recognize task markers in the workspace that correspond to tasks
	 * specified in JavaScript source comments and detected during compilation (for
	 * example, 'TO-DO: ...'). Tasks are identified by a task tag, which can be
	 * customized through <code>JavaScriptCore</code> option
	 * <code>"org.eclipse.wst.jsdt.core.compiler.taskTag"</code>.
	 *
	 */
	String TASK_MARKER = JavaScriptCore.PLUGIN_ID + ".task"; //$NON-NLS-1$

	/**
	 * Id marker attribute (value <code>"arguments"</code>). Arguments are
	 * concatenated into one String, prefixed with an argument count (followed
	 * with colon separator) and separated with '#' characters. For example: {
	 * "foo", "bar" } is encoded as "2:foo#bar", { } is encoded as "0: "
	 *
	 */
	String ARGUMENTS = "arguments"; //$NON-NLS-1$

	/**
	 * ID marker attribute (value <code>"id"</code>).
	 */
	String ID = "id"; //$NON-NLS-1$

	/**
	 * ID category marker attribute (value <code>"categoryId"</code>)
	 */
	String CATEGORY_ID = "categoryId"; //$NON-NLS-1$

	/**
	 * Flags marker attribute (value <code>"flags"</code>). Reserved for
	 * future use.
	 */
	String FLAGS = "flags"; //$NON-NLS-1$

	/**
	 * Cycle detected marker attribute (value <code>"cycleDetected"</code>).
	 * Used only on buildpath problem markers. The value of this attribute is
	 * either "true" or "false".
	 */
	String CYCLE_DETECTED = "cycleDetected"; //$NON-NLS-1$

	/**
	 * Include path problem marker type (value
	 * <code>"org.eclipse.wst.jsdt.core.buildpath_problem"</code>). This can be
	 * used to recognize those markers in the workspace that flag problems
	 * detected by the JavaScript tooling during includepath setting.
	 */
	String BUILDPATH_PROBLEM_MARKER = JavaScriptCore.PLUGIN_ID
			+ ".buildpath_problem"; //$NON-NLS-1$

	/**
	 * IncludePath file format marker attribute (value
	 * <code>"classpathFileFormat"</code>). Used only on includepath problem
	 * markers. The value of this attribute is either "true" or "false".
	 *
	 */
	String INCLUDEPATH_FILE_FORMAT = "classpathFileFormat"; //$NON-NLS-1$
}
