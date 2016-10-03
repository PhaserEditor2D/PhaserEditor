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
import org.eclipse.wst.jsdt.core.ast.IForStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.LoopingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ForStatement extends Statement implements IForStatement {

	public Statement[] initializations;
	public Expression condition;
	public Statement[] increments;
	public Statement action;

	//when there is no local declaration, there is no need of a new scope
	//scope is positionned either to a new scope, or to the "upper"scope (see resolveType)
	public boolean neededScope;
	public BlockScope scope;


	// for local variables table attributes
	int preCondInitStateIndex = -1;
	int preIncrementsInitStateIndex = -1;
	int condIfTrueInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public ForStatement(
		Statement[] initializations,
		Expression condition,
		Statement[] increments,
		Statement action,
		boolean neededScope,
		int s,
		int e) {

		this.sourceStart = s;
		this.sourceEnd = e;
		this.initializations = initializations;
		this.condition = condition;
		this.increments = increments;
		this.action = action;
		// remember useful empty statement
		if (action instanceof EmptyStatement) action.bits |= IsUsefulEmptyStatement;
		this.neededScope = neededScope;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {


		
		boolean isContinue=true;
		
		// process the initializations
		if (initializations != null) {
			for (int i = 0, count = initializations.length; i < count; i++) {
				flowInfo = initializations[i].analyseCode(scope, flowContext, flowInfo);
			}
		}
//		preCondInitStateIndex =
//			currentScope.methodScope().recordInitializationStates(flowInfo);

		Constant cst = this.condition == null ? null : this.condition.constant;
		boolean isConditionTrue = cst == null || (cst != Constant.NotAConstant && cst.booleanValue() == true);
		boolean isConditionFalse = cst != null && (cst != Constant.NotAConstant && cst.booleanValue() == false);

		cst = this.condition == null ? null : this.condition.optimizedBooleanConstant();
		boolean isConditionOptimizedTrue = cst == null ||  (cst != Constant.NotAConstant && cst.booleanValue() == true);
		boolean isConditionOptimizedFalse = cst != null && (cst != Constant.NotAConstant && cst.booleanValue() == false);

		
		
		// process the condition
		LoopingFlowContext condLoopContext = null;
		FlowInfo condInfo = flowInfo.nullInfoLessUnconditionalCopy();
		if (condition != null) {
			if (!isConditionTrue) {
				condInfo =
					condition.analyseCode(
						scope,
						(condLoopContext =
							new LoopingFlowContext(flowContext, flowInfo, this, scope)),
						condInfo);
			}
		}

		// process the action
		LoopingFlowContext loopingContext;
		UnconditionalFlowInfo actionInfo;
		if (action == null
			|| (action.isEmptyBlock() && currentScope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_3)) {
			if (isConditionTrue) {
				if (condLoopContext != null) {
					condLoopContext.complainOnDeferredNullChecks(currentScope,
						condInfo);
				}
				return FlowInfo.DEAD_END;
			} else {
				if (isConditionFalse){
					isContinue=false; // for(;false;p());
				}
				actionInfo = condInfo.initsWhenTrue().unconditionalCopy();
				loopingContext =
					new LoopingFlowContext(flowContext, flowInfo, this, scope);
			}
		}
		else {
			loopingContext =
				new LoopingFlowContext(flowContext, flowInfo, this, scope);
			FlowInfo initsWhenTrue = condInfo.initsWhenTrue();
//			condIfTrueInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(initsWhenTrue);

				if (isConditionFalse) {
					actionInfo = FlowInfo.DEAD_END;
				} else {
					actionInfo = initsWhenTrue.unconditionalCopy();
					if (isConditionOptimizedFalse){
						actionInfo.setReachMode(FlowInfo.UNREACHABLE);
					}
				}
			if (!this.action.complainIfUnreachable(actionInfo, scope, false)) {
				actionInfo = action.analyseCode(scope, loopingContext, actionInfo).
					unconditionalInits();
			}

			// code generation can be optimized when no need to continue in the loop
			if ((actionInfo.tagBits &
					loopingContext.initsOnContinue.tagBits &
					FlowInfo.UNREACHABLE) != 0) {
				isContinue=false;
			}
			else {
				actionInfo = actionInfo.mergedWith(loopingContext.initsOnContinue);
			}
		}
		// for increments
		FlowInfo exitBranch = flowInfo.copy();
		// recover null inits from before condition analysis
		LoopingFlowContext incrementContext = null;
		if (isContinue) {
			if (increments != null) {
				incrementContext =
					new LoopingFlowContext(flowContext, flowInfo, this, scope);
				FlowInfo incrementInfo = actionInfo;
//				this.preIncrementsInitStateIndex =
//					currentScope.methodScope().recordInitializationStates(incrementInfo);
				for (int i = 0, count = increments.length; i < count; i++) {
					incrementInfo = increments[i].
						analyseCode(scope, incrementContext, incrementInfo);
				}
				actionInfo = incrementInfo.unconditionalInits();
			}
			exitBranch.addPotentialInitializationsFrom(actionInfo).
				addInitializationsFrom(condInfo.initsWhenFalse());
		}
		else {
			exitBranch.addInitializationsFrom(condInfo.initsWhenFalse());
		}
		// nulls checks
		if (condLoopContext != null) {
			condLoopContext.complainOnDeferredNullChecks(currentScope,
				actionInfo);
		}
		loopingContext.complainOnDeferredNullChecks(currentScope,
			actionInfo);
		if (incrementContext != null) {
			incrementContext.complainOnDeferredNullChecks(currentScope,
				actionInfo);
		}

		//end of loop
		FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranches(
				(loopingContext.initsOnBreak.tagBits &
					FlowInfo.UNREACHABLE) != 0 ?
					loopingContext.initsOnBreak :
					flowInfo.addInitializationsFrom(loopingContext.initsOnBreak), // recover upstream null info
				isConditionOptimizedTrue,
				exitBranch,
				isConditionOptimizedFalse,
				!isConditionTrue /*for(;;){}while(true); unreachable(); */);
//		mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
		return mergedInfo;
	}

	public StringBuffer printStatement(int tab, StringBuffer output) {

		printIndent(tab, output).append("for ("); //$NON-NLS-1$
		//inits
		if (initializations != null) {
			for (int i = 0; i < initializations.length; i++) {
				//nice only with expressions
				if (i > 0) output.append(", "); //$NON-NLS-1$
				initializations[i].print(0, output);
			}
		}
		output.append("; "); //$NON-NLS-1$
		//cond
		if (condition != null) condition.printExpression(0, output);
		output.append("; "); //$NON-NLS-1$
		//updates
		if (increments != null) {
			for (int i = 0; i < increments.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				increments[i].print(0, output);
			}
		}
		output.append(") "); //$NON-NLS-1$
		//block
		if (action == null)
			output.append(';');
		else {
			output.append('\n');
			action.printStatement(tab + 1, output);
		}
		return output;
	}

	public void resolve(BlockScope upperScope) {

		// use the scope that will hold the init declarations
		scope = neededScope ? new BlockScope(upperScope) : upperScope;
		if (initializations != null)
			for (int i = 0, length = initializations.length; i < length; i++) {
				initializations[i].resolve(scope);
		/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
		/* check where for variable exists in scope chain, report error if not local */
				if(initializations[i] instanceof Assignment  ) {
					Assignment as = ((Assignment)initializations[i]);
					if (as.getLeftHandSide() instanceof SingleNameReference)
					{
						LocalVariableBinding bind1 = as.localVariableBinding();
						if(bind1==null || bind1.declaringScope instanceof CompilationUnitScope){
							upperScope.problemReporter().looseVariableDecleration(this, as);
						}
					}
				}


			}
		/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */

		if (condition != null) {
			TypeBinding type = condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
		}
		if (increments != null)
			for (int i = 0, length = increments.length; i < length; i++)
				increments[i].resolve(scope);
		if (action != null)
			action.resolve(scope);
	}

	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		BlockScope visitScope= (this.scope!=null)?this.scope :blockScope;
		if (visitor.visit(this, blockScope)) {
			if (initializations != null) {
				int initializationsLength = initializations.length;
				for (int i = 0; i < initializationsLength; i++)
					initializations[i].traverse(visitor, visitScope);
			}

			if (condition != null)
				condition.traverse(visitor, visitScope);

			if (increments != null) {
				int incrementsLength = increments.length;
				for (int i = 0; i < incrementsLength; i++)
					increments[i].traverse(visitor, visitScope);
			}

			if (action != null)
				action.traverse(visitor, visitScope);
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.FOR_STATEMENT;
	
	}
}
