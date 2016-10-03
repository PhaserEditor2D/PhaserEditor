/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ArrayInitializer extends Expression implements IArrayInitializer {

	public Expression[] expressions;
	public ArrayBinding binding; //the type of the { , , , }

	/**
	 * ArrayInitializer constructor comment.
	 */
	public ArrayInitializer() {

		super();
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

		if (expressions != null) {
			for (int i = 0, max = expressions.length; i < max; i++) {
				flowInfo = expressions[i].analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
			}
		}
		return flowInfo;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append('[');
		if (expressions != null) {
			int j = 20 ;
			for (int i = 0 ; i < expressions.length ; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				if (expressions[i]!=null)
					expressions[i].printExpression(0, output);
				j -- ;
				if (j == 0) {
					output.append('\n');
					printIndent(indent+1, output);
					j = 20;
				}
			}
		}
		return output.append(']');
	}

	public TypeBinding resolveType(BlockScope scope) {
		this.constant = Constant.NotAConstant;
		this.resolvedType = this.binding = new ArrayBinding(TypeBinding.UNKNOWN,1,scope.environment());
		if (this.expressions!=null)
		  for (int i = 0, length = this.expressions.length; i < length; i++) {
			Expression expression = this.expressions[i];
			 expression.resolveType(scope);
		}		
		return this.resolvedType;
	}

	public TypeBinding resolveTypeExpecting(BlockScope scope, TypeBinding expectedType) {
		// Array initializers can only occur on the right hand side of an assignment
		// expression, therefore the expected type contains the valid information
		// concerning the type that must be enforced by the elements of the array initializer.

		// this method is recursive... (the test on isArrayType is the stop case)

		this.constant = Constant.NotAConstant;

		if (expectedType instanceof ArrayBinding) {
			this.resolvedType = this.binding = (ArrayBinding) expectedType;
			if (this.expressions == null)
				return this.binding;
			TypeBinding elementType = this.binding.elementsType();
			for (int i = 0, length = this.expressions.length; i < length; i++) {
				Expression expression = this.expressions[i];
				expression.setExpectedType(elementType);
				TypeBinding exprType = expression instanceof ArrayInitializer
						? expression.resolveTypeExpecting(scope, elementType)
						: expression.resolveType(scope);
				if (exprType == null)
					continue;

				// Compile-time conversion required?
				if (elementType != exprType) // must call before computeConversion() and typeMismatchError()
					scope.compilationUnitScope().recordTypeConversion(elementType, exprType);

				if ((expression.isConstantValueOfTypeAssignableToType(exprType, elementType)
						|| (elementType.isBaseType() && BaseTypeBinding.isWidening(elementType.id, exprType.id)))
						|| exprType.isCompatibleWith(elementType)) {
				} else if (scope.isBoxingCompatibleWith(exprType, elementType)
									|| (exprType.isBaseType()  // narrowing then boxing ?
											&& scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5 // autoboxing
											&& !elementType.isBaseType()
											&& expression.isConstantValueOfTypeAssignableToType(exprType, scope.environment().computeBoxingType(elementType)))) {
				} else {
					scope.problemReporter().typeMismatchError(exprType, elementType, expression);
//					return null;
				}
			}
			return this.binding;
		}

		// infer initializer type for error reporting based on first element
		TypeBinding leafElementType = null;
		int dim = 1;
		if (this.expressions == null) {
			leafElementType = TypeBinding.UNKNOWN;
		} else {
			Expression expression = this.expressions[0];
			while(expression != null && expression instanceof ArrayInitializer) {
				dim++;
				Expression[] subExprs = ((ArrayInitializer) expression).expressions;
				if (subExprs == null){
					leafElementType = scope.getJavaLangObject();
					expression = null;
					break;
				}
				expression = ((ArrayInitializer) expression).expressions[0];
			}
			if (expression != null) {
				leafElementType = expression.resolveType(scope);
			}
			// fault-tolerance - resolve other expressions as well
			for (int i = 1, length = this.expressions.length; i < length; i++) {
				expression = this.expressions[i];
				if (expression != null) {
					expression.resolveType(scope)	;
				}
			}		}
		if (leafElementType != null) {
			this.resolvedType = scope.createArrayType(leafElementType, dim);
			if (expectedType != null )
				scope.problemReporter().typeMismatchError(this.resolvedType, expectedType, this);
		}
		return null;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.expressions != null) {
				int expressionsLength = this.expressions.length;
				for (int i = 0; i < expressionsLength; i++)
					if (this.expressions[i]!=null)
					  this.expressions[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.ARRAY_INITIALIZER;
	
	}
}
