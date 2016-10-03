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

import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;

public class CompletionOnMethodName extends MethodDeclaration {
	public int selectorEnd;

	public CompletionOnMethodName(CompilationResult compilationResult){
		super(compilationResult);
	}

	public StringBuffer print(int indent, StringBuffer output) {

		printIndent(indent, output);
		output.append("<CompletionOnMethodName:"); //$NON-NLS-1$
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

	public void resolve(ClassScope upperScope) {

		super.resolve(upperScope);
		throw new CompletionNodeFound(this, upperScope);
	}
}
