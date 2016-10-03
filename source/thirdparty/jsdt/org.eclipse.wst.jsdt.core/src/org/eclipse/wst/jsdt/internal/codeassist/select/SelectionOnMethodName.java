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
package org.eclipse.wst.jsdt.internal.codeassist.select;

import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;

public class SelectionOnMethodName extends MethodDeclaration {
	public int selectorEnd;

	public SelectionOnMethodName(CompilationResult compilationResult){
		super(compilationResult);
	}

	public StringBuffer print(int indent, StringBuffer output) {

		printIndent(indent, output);
		output.append("<SelectionOnMethodName:"); //$NON-NLS-1$
		printModifiers(this.modifiers, output);
		printReturnType(0, output);
		output.append(selector).append('(');
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				arguments[i].print(0, output);
			}
		}
		output.append(')');
		return output.append('>');
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration#resolve(org.eclipse.wst.jsdt.internal.compiler.lookup.Scope)
	 */
	public void resolve(Scope upperScope) {
		super.resolve(upperScope);
		throw new SelectionNodeFound(binding);
	}
}
