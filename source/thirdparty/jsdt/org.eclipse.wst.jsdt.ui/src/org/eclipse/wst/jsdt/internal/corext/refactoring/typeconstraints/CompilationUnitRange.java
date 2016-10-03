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
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;

public final class CompilationUnitRange {

	private final IJavaScriptUnit fCompilationUnit;
	private final ISourceRange fSourceRange;

	public CompilationUnitRange(IJavaScriptUnit unit, ISourceRange range) {
		Assert.isNotNull(unit);
		Assert.isNotNull(range);
		fCompilationUnit= unit;
		fSourceRange= range;
	}

	public CompilationUnitRange(IJavaScriptUnit unit, ASTNode node) {
		this(unit, new SourceRange(node));
	}

	public IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	public ISourceRange getSourceRange() {
		return fSourceRange;
	}

	//rootNode must be the ast root for fCompilationUnit
	public ASTNode getNode(JavaScriptUnit rootNode) {
		NodeFinder finder= new NodeFinder(fSourceRange.getOffset(), fSourceRange.getLength());
		rootNode.accept(finder);
		ASTNode result= finder.getCoveringNode();
		if (result != null)
			return result;
		return finder.getCoveredNode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + fSourceRange.toString() + " in " + fCompilationUnit.getElementName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof CompilationUnitRange))
			return false;
		CompilationUnitRange other= (CompilationUnitRange)obj;
		return fCompilationUnit.equals(other.fCompilationUnit) && fSourceRange.equals(other.fSourceRange);
	}

	public int hashCode() {
		return (37 * fCompilationUnit.hashCode()) ^ fSourceRange.hashCode();
	}
}
