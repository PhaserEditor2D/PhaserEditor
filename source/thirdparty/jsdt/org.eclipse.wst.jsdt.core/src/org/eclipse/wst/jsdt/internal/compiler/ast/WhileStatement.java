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
import org.eclipse.wst.jsdt.core.ast.IWhileStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.LoopingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class WhileStatement extends Statement implements IWhileStatement {

	public Expression condition;
	public Statement action;

	public WhileStatement(Expression condition, Statement action, int s, int e) {
		this.condition = condition;
		this.action = action;
		// remember useful empty statement
		if (action instanceof EmptyStatement) action.bits |= IsUsefulEmptyStatement;
		sourceStart = s;
		sourceEnd = e;
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext,
		FlowInfo flowInfo) {

		Constant cst = this.condition.constant;
		boolean isConditionTrue = cst == null ? false : cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isConditionFalse = cst == null ? false : cst != Constant.NotAConstant && cst.booleanValue() == false;

		cst = this.condition.optimizedBooleanConstant();
		boolean isConditionOptimizedTrue = cst == null ? false : cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isConditionOptimizedFalse = cst == null ? false : cst != Constant.NotAConstant && cst.booleanValue() == false;

		LoopingFlowContext condLoopContext;
		FlowInfo condInfo =	flowInfo.nullInfoLessUnconditionalCopy();
		// we need to collect the contribution to nulls of the coming paths through the
		// loop, be they falling through normally or branched to break, continue labels
		// or catch blocks
		condInfo = this.condition.analyseCode(
				currentScope,
				(condLoopContext =
					new LoopingFlowContext(flowContext, flowInfo, this, currentScope)),
				condInfo);

		LoopingFlowContext loopingContext;
		FlowInfo actionInfo;
		FlowInfo exitBranch;
		if (action == null
			|| (action.isEmptyBlock() && currentScope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_3)) {
			condLoopContext.complainOnDeferredNullChecks(currentScope,
				condInfo.unconditionalInits());
			if (isConditionTrue) {
				return FlowInfo.DEAD_END;
			} else {
				FlowInfo mergedInfo = flowInfo.copy().addInitializationsFrom(condInfo.initsWhenFalse());
				if (isConditionOptimizedTrue){
					mergedInfo.setReachMode(FlowInfo.UNREACHABLE);
				}
				return mergedInfo;
			}
		} else {
			// in case the condition was inlined to false, record the fact that there is no way to reach any
			// statement inside the looping action
			loopingContext =
				new LoopingFlowContext(
					flowContext,
					flowInfo,
					this,
					currentScope);
			if (isConditionFalse) {
				actionInfo = FlowInfo.DEAD_END;
			} else {
				actionInfo = condInfo.initsWhenTrue().copy();
				if (isConditionOptimizedFalse){
					actionInfo.setReachMode(FlowInfo.UNREACHABLE);
				}
			}

			if (!this.action.complainIfUnreachable(actionInfo, currentScope, false)) {
				actionInfo = this.action.analyseCode(currentScope, loopingContext, actionInfo);
			}

			// code generation can be optimized when no need to continue in the loop
			exitBranch = flowInfo.copy();
			// need to start over from flowInfo so as to get null inits

			if ((actionInfo.tagBits &
					loopingContext.initsOnContinue.tagBits &
					FlowInfo.UNREACHABLE) != 0) {
				exitBranch.addInitializationsFrom(condInfo.initsWhenFalse());
			} else {
				actionInfo = actionInfo.mergedWith(loopingContext.initsOnContinue.unconditionalInits());
				condLoopContext.complainOnDeferredNullChecks(currentScope,
						actionInfo);
				loopingContext.complainOnDeferredNullChecks(currentScope,
						actionInfo);
				exitBranch.
					addPotentialInitializationsFrom(
						actionInfo.unconditionalInits()).
					addInitializationsFrom(condInfo.initsWhenFalse());
			}
		}

		// end of loop
		FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranches(
				(loopingContext.initsOnBreak.tagBits &
					FlowInfo.UNREACHABLE) != 0 ?
					loopingContext.initsOnBreak :
					flowInfo.addInitializationsFrom(loopingContext.initsOnBreak), // recover upstream null info
				isConditionOptimizedTrue,
				exitBranch,
				isConditionOptimizedFalse,
				!isConditionTrue /*while(true); unreachable(); */);
		return mergedInfo;
	}

	public void resolve(BlockScope scope) {
		condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
		if (action != null)
			action.resolve(scope);
	}

	public StringBuffer printStatement(int tab, StringBuffer output) {
		printIndent(tab, output).append("while ("); //$NON-NLS-1$
		condition.printExpression(0, output).append(')');
		if (action == null)
			output.append(';');
		else
			action.printStatement(tab + 1, output);
		return output;
	}

	public void traverse( ASTVisitor visitor, BlockScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			condition.traverse(visitor, blockScope);
			if (action != null)
				action.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	
	public int getASTType() {
		return IASTNode.WHILE_STATEMENT;
	}
}
