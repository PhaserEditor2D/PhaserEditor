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
 * reduce an allocation expression containing the cursor.
 * If the allocation expression is not qualified, the enclosingInstance field
 * is null.
 * e.g.
 *
 *	class X {
 *    void foo() {
 *      new [start]Bar[end](1, 2)
 *    }
 *  }
 *
 *	---> class X {
 *         void foo() {
 *           <SelectOnAllocationExpression:new Bar(1, 2)>
 *         }
 *       }
 *
 */

import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedAllocationExpression extends QualifiedAllocationExpression {

	public SelectionOnQualifiedAllocationExpression() {
		// constructor without argument
	}

	public SelectionOnQualifiedAllocationExpression(TypeDeclaration anonymous) {
		super(anonymous);
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.enclosingInstance == null)
			output.append("<SelectOnAllocationExpression:");  //$NON-NLS-1$
		else
			output.append("<SelectOnQualifiedAllocationExpression:"); //$NON-NLS-1$

		return super.printExpression(indent, output).append('>');
	}

	public TypeBinding resolveType(BlockScope scope) {
		super.resolveType(scope);

		// tolerate some error cases
		if (binding == null ||
				!(binding.isValidBinding() ||
					binding.problemId() == ProblemReasons.NotVisible))
			throw new SelectionNodeFound();
		if (anonymousType == null)
			throw new SelectionNodeFound(binding);

		// if selecting a type for an anonymous type creation, we have to
		// find its target super constructor (if extending a class) or its target
		// super interface (if extending an interface)
		// find the constructor binding inside the super constructor call
		ConstructorDeclaration constructor = (ConstructorDeclaration) anonymousType.declarationOf(binding.original());
		throw new SelectionNodeFound(constructor.constructorCall.binding);
		
	}
}
