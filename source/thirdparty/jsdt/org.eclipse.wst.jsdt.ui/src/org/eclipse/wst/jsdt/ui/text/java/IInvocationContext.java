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

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

/**
 * Context information for quick fix and quick assist processors.
 * <p>
 * Note: this interface is not intended to be implemented.
 * </p>
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IInvocationContext {

	/**
	 * @return Returns the current compilation unit.
	 */
	IJavaScriptUnit getCompilationUnit();

	/**
	 * @return Returns the offset of the current selection
	 */
	int getSelectionOffset();

	/**
	 * @return Returns the length of the current selection
	 */
	int getSelectionLength();

	/**
	 * Returns an AST of the compilation unit, possibly only a partial AST focused on the selection
	 * offset (see {@link org.eclipse.wst.jsdt.core.dom.ASTParser#setFocalPosition(int)}).
	 * The returned AST is shared and therefore protected and cannot be modified.
	 * The client must check the AST API level and do nothing if they are given an AST
	 * they can't handle. (see {@link org.eclipse.wst.jsdt.core.dom.AST#apiLevel()}).
	 * @return Returns the root of the AST corresponding to the current compilation unit.
	 */
	JavaScriptUnit getASTRoot();

	/**
	 * Convenience method to evaluate the AST node covering the current selection.
	 * @return Returns the node that covers the location of the problem
	 */
	ASTNode getCoveringNode();

	/**
	 * Convenience method to evaluate the AST node that is covered by the current selection.
	 * @return Returns the node that is covered by the location of the problem
	 */
	ASTNode getCoveredNode();

}
