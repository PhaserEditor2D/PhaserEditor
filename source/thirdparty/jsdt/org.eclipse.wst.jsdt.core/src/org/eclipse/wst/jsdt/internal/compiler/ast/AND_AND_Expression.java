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
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IAND_AND_Expression;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;

//dedicated treatment for the &&
public class AND_AND_Expression extends BinaryExpression implements IAND_AND_Expression {

	int rightInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public AND_AND_Expression(Expression left, Expression right, int operator) {
		super(left, right, operator);
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

		Constant cst = this.left.optimizedBooleanConstant();
		boolean isLeftOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isLeftOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

		if (isLeftOptimizedTrue) {
			// TRUE && anything
			// need to be careful of scenario:
			//  (x && y) && !z, if passing the left info to the right, it would
			// be swapped by the !
			FlowInfo mergedInfo = left.analyseCode(currentScope, flowContext, flowInfo)
					.unconditionalInits();
			mergedInfo = right.analyseCode(currentScope, flowContext, mergedInfo);
//			mergedInitStateIndex = currentScope.methodScope()
//					.recordInitializationStates(mergedInfo);
			return mergedInfo;
		}

		FlowInfo leftInfo = left.analyseCode(currentScope, flowContext, flowInfo);
		// need to be careful of scenario:
		//  (x && y) && !z, if passing the left info to the right, it would be
		// swapped by the !
		FlowInfo rightInfo = leftInfo.initsWhenTrue().unconditionalCopy();
//		rightInitStateIndex = currentScope.methodScope().recordInitializationStates(rightInfo);

		int previousMode = rightInfo.reachMode();
		if (isLeftOptimizedFalse) {
			rightInfo.setReachMode(FlowInfo.UNREACHABLE);
		}
		rightInfo = right.analyseCode(currentScope, flowContext, rightInfo);
		FlowInfo mergedInfo = FlowInfo.conditional(
				rightInfo.safeInitsWhenTrue(),
				leftInfo.initsWhenFalse().unconditionalInits().mergedWith(
						rightInfo.initsWhenFalse().setReachMode(previousMode).unconditionalInits()));
		// reset after trueMergedInfo got extracted
//		mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
		return mergedInfo;
	}

	public boolean isCompactableOperation() {
		return false;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			left.traverse(visitor, scope);
			right.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.AND_AND_EXPRESSION;
	
	}
}
