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
package org.eclipse.wst.jsdt.ui.text.java;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;


/**
 * Problem information for quick fix and quick assist processors.
 * <p>
 * Note: this interface is not intended to be implemented.
 * </p>
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IProblemLocation {

	/**
	 * Returns the start offset of the problem.
	 *
	 * @return the start offset of the problem
	 */
	int getOffset();

	/**
	 * Returns the length of the problem.
	 *
	 * @return the length of the problem
	 */
	int getLength();

	/**
	 * Returns the marker type of this problem.
	 *
	 * @return The marker type of the problem.
	 * 
	 */
	String getMarkerType();
	
	/**
	 * Returns the id of problem. Note that problem ids are defined per problem marker type.
	 * See {@link org.eclipse.wst.jsdt.core.compiler.IProblem} for id definitions for problems of type
	 * <code>org.eclipse.wst.jsdt.core.problem</code> and <code>org.eclipse.wst.jsdt.core.task</code>.
	 *
	 * @return The id of the problem.
	 */
	int getProblemId();

	/**
	 * Returns the original arguments recorded into the problem.
	 *
	 * @return String[] Returns the problem arguments.
	 */
	String[] getProblemArguments();

	/**
	 * Returns if the problem has error severity.
	 *
	 * @return <code>true</code> if the problem has error severity
	 */
	boolean isError();

	/**
	 * Convenience method to evaluate the AST node covering this problem.
	 *
	 * @param astRoot The root node of the current AST
	 * @return Returns the node that covers the location of the problem
	 */
	ASTNode getCoveringNode(JavaScriptUnit astRoot);

	/**
	 * Convenience method to evaluate the AST node covered by this problem.
	 *
	 * @param astRoot The root node of the current AST
	 * @return Returns the node that is covered by the location of the problem
	 */
	ASTNode getCoveredNode(JavaScriptUnit astRoot);

}
