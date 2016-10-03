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

/*
 * Selection node build by the parser in any case it was intending to
 * reduce a message send containing the cursor.
 * e.g.
 *
 *	class X {
 *    void foo() {
 *      this.[start]bar[end](1, 2)
 *    }
 *  }
 *
 *	---> class X {
 *         void foo() {
 *           <SelectOnMessageSend:this.bar(1, 2)>
 *         }
 *       }
 *
 */

import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnMessageSend extends MessageSend {

	/*
	 * Cannot answer default abstract match, iterate in superinterfaces of declaring class
	 * for a better match (default abstract match came from scope lookups).
	 */
	private MethodBinding findNonDefaultAbstractMethod(MethodBinding methodBinding) {
		return methodBinding;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append("<SelectOnMessageSend:"); //$NON-NLS-1$
		if (receiver!=null && !receiver.isImplicitThis()) receiver.printExpression(0, output).append('.');
		output.append(this.selector).append('(');
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				arguments[i].printExpression(0, output);
			}
		}
		return output.append(")>"); //$NON-NLS-1$
	}

	public TypeBinding resolveType(BlockScope scope) {

		super.resolveType(scope);

		// tolerate some error cases
		if(binding == null ||
					!(binding.isValidBinding() ||
						binding.problemId() == ProblemReasons.NotVisible
						|| binding.problemId() == ProblemReasons.InheritedNameHidesEnclosingName
						|| binding.problemId() == ProblemReasons.NonStaticReferenceInConstructorInvocation
						|| binding.problemId() == ProblemReasons.NonStaticReferenceInStaticContext)) {
			throw new SelectionNodeFound();
		} else {
			if(binding.isDefaultAbstract()) {
				throw new SelectionNodeFound(findNonDefaultAbstractMethod(binding)); // 23594
			} else {
				throw new SelectionNodeFound(binding);
			}
		}
	}
}
