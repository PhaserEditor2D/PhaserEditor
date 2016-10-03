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
package org.eclipse.wst.jsdt.internal.codeassist.complete;

import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnJavadocParamNameReference extends JavadocSingleNameReference implements CompletionOnJavadoc {
	public int completionFlags = JAVADOC;
	public char[][] missingParams;

	public CompletionOnJavadocParamNameReference(char[] name, long pos, int start, int end) {
		super(name, pos, start, end);
	}

	public CompletionOnJavadocParamNameReference(JavadocSingleNameReference nameRef) {
		super(nameRef.token, (((long)nameRef.sourceStart)<<32)+nameRef.sourceEnd, nameRef.tagSourceStart, nameRef.tagSourceStart);
	}

	/**
	 * @param flags The completionFlags to set.
	 */
	public void addCompletionFlags(int flags) {
		this.completionFlags |= flags;
	}

	/**
	 * Get completion node flags.
	 *
	 * @return int Flags of the javadoc completion node.
	 */
	public int getCompletionFlags() {
		return this.completionFlags;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression#printExpression(int, java.lang.StringBuffer)
	 */
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("<CompletionOnJavadocParamNameReference:"); //$NON-NLS-1$
		if (this.token != null) super.printExpression(indent, output);
		return output.append('>');
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference#reportError(org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public TypeBinding reportError(BlockScope scope) {
		return null;
	}
}
