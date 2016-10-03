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

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IOR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;

//dedicated treatment for the ||
public class OR_OR_Expression extends BinaryExpression implements IOR_OR_Expression {

	int rightInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public OR_OR_Expression(Expression left, Expression right, int operator) {
		super(left, right, operator);
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		Constant cst = this.left.optimizedBooleanConstant();
		boolean isLeftOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isLeftOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

		if (isLeftOptimizedFalse) {
			// FALSE || anything
			 // need to be careful of scenario:
			//		(x || y) || !z, if passing the left info to the right, it would be swapped by the !
			FlowInfo mergedInfo = left.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
			mergedInfo = right.analyseCode(currentScope, flowContext, mergedInfo);
//			mergedInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(mergedInfo);
			return mergedInfo;
		}

		FlowInfo leftInfo = left.analyseCode(currentScope, flowContext, flowInfo);

		 // need to be careful of scenario:
		//		(x || y) || !z, if passing the left info to the right, it would be swapped by the !
		FlowInfo rightInfo = leftInfo.initsWhenFalse().unconditionalCopy();
//		rightInitStateIndex =
//			currentScope.methodScope().recordInitializationStates(rightInfo);

		int previousMode = rightInfo.reachMode();
		if (isLeftOptimizedTrue){
			rightInfo.setReachMode(FlowInfo.UNREACHABLE);
		}
		rightInfo = right.analyseCode(currentScope, flowContext, rightInfo);
		FlowInfo mergedInfo = FlowInfo.conditional(
					// merging two true initInfos for such a negative case: if ((t && (b = t)) || f) r = b; // b may not have been initialized
					leftInfo.initsWhenTrue().unconditionalInits().mergedWith(
						rightInfo.safeInitsWhenTrue().setReachMode(previousMode).unconditionalInits()),
					rightInfo.initsWhenFalse());
//		mergedInitStateIndex =
//			currentScope.methodScope().recordInitializationStates(mergedInfo);
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
		return IASTNode.OR_OR_EXPRESSION;
	
	}
}
