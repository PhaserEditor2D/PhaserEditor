/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IArrayAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ArrayAllocationExpression extends Expression implements IArrayAllocationExpression {

	public TypeReference type;

	//dimensions.length gives the number of dimensions, but the
	// last ones may be nulled as in new int[4][5][][]
	public Expression[] dimensions;
	public ArrayInitializer initializer;

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		for (int i = 0, max = this.dimensions.length; i < max; i++) {
			Expression dim;
			if ((dim = this.dimensions[i]) != null) {
				flowInfo = dim.analyseCode(currentScope, flowContext, flowInfo);
			}
		}
		if (this.initializer != null) {
			return this.initializer.analyseCode(currentScope, flowContext, flowInfo);
		}
		return flowInfo;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("new "); //$NON-NLS-1$
		this.type.print(0, output);
		for (int i = 0; i < this.dimensions.length; i++) {
			if (this.dimensions[i] == null)
				output.append("[]"); //$NON-NLS-1$
			else {
				output.append('[');
				this.dimensions[i].printExpression(0, output);
				output.append(']');
			}
		}
		if (this.initializer != null) this.initializer.printExpression(0, output);
		return output;
	}

	public TypeBinding resolveType(BlockScope scope) {
		// Build an array type reference using the current dimensions
		// The parser does not check for the fact that dimension may be null
		// only at the -end- like new int [4][][]. The parser allows new int[][4][]
		// so this must be checked here......(this comes from a reduction to LL1 grammar)

		TypeBinding referenceType = this.type.resolveType(scope, true /* check bounds*/);

		// will check for null after dimensions are checked
		this.constant = Constant.NotAConstant;

		// check the validity of the dimension syntax (and test for all null dimensions)
		int explicitDimIndex = -1;
		loop: for (int i = this.dimensions.length; --i >= 0;) {
			if (this.dimensions[i] != null) {
				if (explicitDimIndex < 0) explicitDimIndex = i;
			} else if (explicitDimIndex > 0) {
				break loop;
			}
		}

		// dimensions resolution
		for (int i = 0; i <= explicitDimIndex; i++) {
			Expression dimExpression;
			if ((dimExpression = this.dimensions[i]) != null) {
				TypeBinding dimensionType = dimExpression.resolveTypeExpecting(scope, TypeBinding.INT);
			}
		}

		// building the array binding
		if (referenceType != null) {
			this.resolvedType = scope.createArrayType(referenceType, this.dimensions.length);

			// check the initializer
			if (this.initializer != null) {
				if ((this.initializer.resolveTypeExpecting(scope, this.resolvedType)) != null)
					this.initializer.binding = (ArrayBinding)this.resolvedType;
			}
		}
		return this.resolvedType;
	}


	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			int dimensionsLength = this.dimensions.length;
			this.type.traverse(visitor, scope);
			for (int i = 0; i < dimensionsLength; i++) {
				if (this.dimensions[i] != null)
					this.dimensions[i].traverse(visitor, scope);
			}
			if (this.initializer != null)
				this.initializer.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.ARRAY_ALLOCATION_EXPRESSION;
	
	}
}
