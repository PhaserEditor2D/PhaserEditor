/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist.complete;

/*
 * Completion node build by the parser in any case it was intending to
 * reduce an access to the literal 'class' containing the cursor.
 * e.g.
 *
 *	class X {
 *    void foo() {
 *      String[].[cursor]
 *    }
 *  }
 *
 *	---> class X {
 *         void foo() {
 *           <CompleteOnClassLiteralAccess:String[].>
 *         }
 *       }
 *
 * The source range of the completion node denotes the source range
 * which should be replaced by the completion.
 */

import org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnClassLiteralAccess extends ClassLiteralAccess {

	public char[] completionIdentifier;
	public int classStart;

	public CompletionOnClassLiteralAccess(long pos, TypeReference t) {

		super((int)pos, t);
		this.classStart = (int) (pos >>> 32);
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append("<CompleteOnClassLiteralAccess:"); //$NON-NLS-1$
		return this.type.print(0, output).append('.').append(this.completionIdentifier).append('>');
	}

	public TypeBinding resolveType(BlockScope scope) {

		if (super.resolveType(scope) == null)
			throw new CompletionNodeFound();
		else
			throw new CompletionNodeFound(this, this.targetType, scope);
	}
}
