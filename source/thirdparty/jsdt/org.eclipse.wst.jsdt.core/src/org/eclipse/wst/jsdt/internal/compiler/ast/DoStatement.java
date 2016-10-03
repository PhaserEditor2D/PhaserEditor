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
import org.eclipse.wst.jsdt.core.ast.IDoStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.LoopingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class DoStatement extends Statement implements IDoStatement {

	public Expression condition;
	public Statement action;


	// for local variables table attributes
	int mergedInitStateIndex = -1;

public DoStatement(Expression condition, Statement action, int s, int e) {

	this.sourceStart = s;
	this.sourceEnd = e;
	this.condition = condition;
	this.action = action;
	// remember useful empty statement
	if (action instanceof EmptyStatement) action.bits |= ASTNode.IsUsefulEmptyStatement;
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	LoopingFlowContext loopingContext =
		new LoopingFlowContext(
			flowContext,
			flowInfo,
			this,
			currentScope);

	Constant cst = this.condition.constant;
	boolean isConditionTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
	cst = this.condition.optimizedBooleanConstant();
	boolean isConditionOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
	boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

	int previousMode = flowInfo.reachMode();

	boolean isContinue=true;
	
	
	UnconditionalFlowInfo actionInfo = flowInfo.nullInfoLessUnconditionalCopy();
	// we need to collect the contribution to nulls of the coming paths through the
	// loop, be they falling through normally or branched to break, continue labels
	// or catch blocks
	if ((this.action != null) && !this.action.isEmptyBlock()) {
		actionInfo = this.action.
			analyseCode(currentScope, loopingContext, actionInfo).
			unconditionalInits();

		// code generation can be optimized when no need to continue in the loop
		if ((actionInfo.tagBits &
				loopingContext.initsOnContinue.tagBits &
				FlowInfo.UNREACHABLE) != 0) {
			isContinue = false;
		}
	}
	/* Reset reach mode, to address following scenario.
	 *   final blank;
	 *   do { if (true) break; else blank = 0; } while(false);
	 *   blank = 1; // may be initialized already
	 */
	actionInfo.setReachMode(previousMode);

	LoopingFlowContext condLoopContext;
	FlowInfo condInfo =
		this.condition.analyseCode(
			currentScope,
			(condLoopContext =
				new LoopingFlowContext(flowContext,	flowInfo, this, currentScope)),
			(this.action == null
				? actionInfo
				: (actionInfo.mergedWith(loopingContext.initsOnContinue))).copy());
	if (!isConditionOptimizedFalse && isContinue) {
		loopingContext.complainOnDeferredNullChecks(currentScope,
				flowInfo.unconditionalCopy().addPotentialNullInfoFrom(
					  condInfo.initsWhenTrue().unconditionalInits()));
		condLoopContext.complainOnDeferredNullChecks(currentScope,
				actionInfo.addPotentialNullInfoFrom(
				  condInfo.initsWhenTrue().unconditionalInits()));
	}

	// end of loop
	FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranches(
			(loopingContext.initsOnBreak.tagBits &
				FlowInfo.UNREACHABLE) != 0 ?
				loopingContext.initsOnBreak :
				flowInfo.unconditionalCopy().addInitializationsFrom(loopingContext.initsOnBreak),
					// recover upstream null info
			isConditionOptimizedTrue,
			(condInfo.tagBits & FlowInfo.UNREACHABLE) == 0 ?
					flowInfo.addInitializationsFrom(condInfo.initsWhenFalse()) : condInfo,
				// recover null inits from before condition analysis
			false, // never consider opt false case for DO loop, since break can always occur (47776)
			!isConditionTrue /*do{}while(true); unreachable(); */);
//	this.mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
	return mergedInfo;
}

public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output).append("do"); //$NON-NLS-1$
	if (this.action == null)
		output.append(" ;\n"); //$NON-NLS-1$
	else {
		output.append('\n');
		this.action.printStatement(indent + 1, output).append('\n');
	}
	output.append("while ("); //$NON-NLS-1$
	return this.condition.printExpression(0, output).append(");"); //$NON-NLS-1$
}

public void resolve(BlockScope scope) {
	TypeBinding type = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
	if (this.action != null)
		this.action.resolve(scope);
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		if (this.action != null) {
			this.action.traverse(visitor, scope);
		}
		this.condition.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
public int getASTType() {
	return IASTNode.DOUBLE_LITERAL;

}
}
