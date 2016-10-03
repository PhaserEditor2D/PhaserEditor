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
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;

/**
  */
public class AssistContext implements IInvocationContext {

	private IJavaScriptUnit fCompilationUnit;
	private int fOffset;
	private int fLength;

	private JavaScriptUnit fASTRoot;

	/*
	 * Constructor for CorrectionContext.
	 */
	public AssistContext(IJavaScriptUnit cu, int offset, int length) {
		fCompilationUnit= cu;
		fOffset= offset;
		fLength= length;

		fASTRoot= null;
	}

	/**
	 * Returns the compilation unit.
	 * @return Returns a IJavaScriptUnit
	 */
	public IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the length.
	 * @return int
	 */
	public int getSelectionLength() {
		return fLength;
	}

	/**
	 * Returns the offset.
	 * @return int
	 */
	public int getSelectionOffset() {
		return fOffset;
	}

	public JavaScriptUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= ASTProvider.getASTProvider().getAST(fCompilationUnit, ASTProvider.WAIT_YES, null);
			if (fASTRoot == null) {
				// see bug 63554
				fASTRoot= ASTResolving.createQuickFixAST(fCompilationUnit, null);
			}
		}
		return fASTRoot;
	}


	/**
	 * @param root The ASTRoot to set.
	 */
	public void setASTRoot(JavaScriptUnit root) {
		fASTRoot= root;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.text.java.IInvocationContext#getCoveringNode()
	 */
	public ASTNode getCoveringNode() {
		NodeFinder finder= new NodeFinder(fOffset, fLength);
		getASTRoot().accept(finder);
		return finder.getCoveringNode();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.text.java.IInvocationContext#getCoveredNode()
	 */
	public ASTNode getCoveredNode() {
		NodeFinder finder= new NodeFinder(fOffset, fLength);
		getASTRoot().accept(finder);
		return finder.getCoveredNode();
	}

}
