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
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IIfStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.impl.StringConstant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class IfStatement extends Statement implements IIfStatement {

	//this class represents the case of only one statement in
	//either else and/or then branches.

	public Expression condition;
	public Statement thenStatement;
	public Statement elseStatement;

	boolean thenExit;

	// for local variables table attributes
	int thenInitStateIndex = -1;
	int elseInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public IfStatement(Expression condition, Statement thenStatement, 	int sourceStart, int sourceEnd) {

		this.condition = condition;
		this.thenStatement = thenStatement;
		// remember useful empty statement
		if (thenStatement instanceof EmptyStatement) thenStatement.bits |= IsUsefulEmptyStatement;
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
	}

	public IfStatement(Expression condition, Statement thenStatement, Statement elseStatement, int sourceStart, int sourceEnd) {

		this.condition = condition;
		this.thenStatement = thenStatement;
		// remember useful empty statement
		if (thenStatement instanceof EmptyStatement) thenStatement.bits |= IsUsefulEmptyStatement;
		this.elseStatement = elseStatement;
		if (elseStatement instanceof IfStatement) elseStatement.bits |= IsElseIfStatement;
		if (elseStatement instanceof EmptyStatement) elseStatement.bits |= IsUsefulEmptyStatement;
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		// process the condition
		FlowInfo conditionFlowInfo =
			condition.analyseCode(currentScope, flowContext, flowInfo);

		Constant cst = this.condition.optimizedBooleanConstant();
		boolean isConditionOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
		boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

		// process the THEN part
		FlowInfo thenFlowInfo = conditionFlowInfo.safeInitsWhenTrue();
		if (isConditionOptimizedFalse) {
			thenFlowInfo.setReachMode(FlowInfo.UNREACHABLE);
		}
		FlowInfo elseFlowInfo = conditionFlowInfo.initsWhenFalse();
		if (isConditionOptimizedTrue) {
			elseFlowInfo.setReachMode(FlowInfo.UNREACHABLE);
		}
		if (this.thenStatement != null) {
			// Save info for code gen
//			thenInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(thenFlowInfo);
			if (!thenStatement.complainIfUnreachable(thenFlowInfo, currentScope, false)) {
				thenFlowInfo =
					thenStatement.analyseCode(currentScope, flowContext, thenFlowInfo);
			}
		}
		// code gen: optimizing the jump around the ELSE part
		this.thenExit = (thenFlowInfo.tagBits & FlowInfo.UNREACHABLE) != 0;

		// process the ELSE part
		if (this.elseStatement != null) {
		    // signal else clause unnecessarily nested, tolerate else-if code pattern
		    if (thenFlowInfo == FlowInfo.DEAD_END
		            && (this.bits & IsElseIfStatement) == 0 	// else of an else-if
		            && !(this.elseStatement instanceof IfStatement)) {
		        currentScope.problemReporter().unnecessaryElse(this.elseStatement);
		    }
			// Save info for code gen
//			elseInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(elseFlowInfo);
			if (!elseStatement.complainIfUnreachable(elseFlowInfo, currentScope, false)) {
				elseFlowInfo =
					elseStatement.analyseCode(currentScope, flowContext, elseFlowInfo);
			}
		}

		// handle cases  where condition is "typeof something== ''", set inits accordingly
		if (this.condition instanceof EqualExpression)
		{
			EqualExpression equalExpression =(EqualExpression) this.condition;
			int operator=(equalExpression.bits & OperatorMASK) >> OperatorSHIFT;
			if (operator==OperatorIds.EQUAL_EQUAL || operator==OperatorIds.NOT_EQUAL)
			{

				boolean isDefined[]={false};
				SingleNameReference snr=getTypeofExpressionVar(equalExpression.left,equalExpression.right,isDefined);
				if (snr==null)
					snr=getTypeofExpressionVar(equalExpression.right, equalExpression.left,isDefined);
				if (snr!=null)
				{
					LocalVariableBinding local = snr.localVariableBinding();
					if (local==null)
						snr.resolveType(currentScope, true,null);
					local = snr.localVariableBinding();
					if (local!=null)
					{
						if (isDefined[0])
							thenFlowInfo.markAsDefinitelyAssigned(local);
						else
							elseFlowInfo.markAsDefinitelyAssigned(local);
					}
				}


			}
		}

		// merge THEN & ELSE initializations
		FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranches(
			thenFlowInfo,
			isConditionOptimizedTrue,
			elseFlowInfo,
			isConditionOptimizedFalse,
			true /*if(true){ return; }  fake-reachable(); */);
//		mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
		return mergedInfo;
	}

	private SingleNameReference getTypeofExpressionVar(Expression expression1,Expression expression2,boolean isDefined[])
	{
		if (expression1 instanceof UnaryExpression && expression2.constant instanceof StringConstant)
		{
			UnaryExpression unaryExpression = (UnaryExpression)expression1;
			if ( unaryExpression.expression instanceof SingleNameReference &&
					(((unaryExpression.bits & OperatorMASK) >> OperatorSHIFT)==OperatorIds.TYPEOF)
					)
		{
				isDefined[0]=!((StringConstant)expression2.constant).stringValue().equals("undefined"); //$NON-NLS-1$
				return (SingleNameReference)unaryExpression.expression ;

		}
		}
		return null;
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		printIndent(indent, output).append("if ("); //$NON-NLS-1$
		condition.printExpression(0, output).append(")\n");	//$NON-NLS-1$
		thenStatement.printStatement(indent + 2, output);
		if (elseStatement != null) {
			output.append('\n');
			printIndent(indent, output);
			output.append("else\n"); //$NON-NLS-1$
			elseStatement.printStatement(indent + 2, output);
		}
		return output;
	}

	public void resolve(BlockScope scope) {

		TypeBinding type = condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
		if (thenStatement != null)
			thenStatement.resolve(scope);
		if (elseStatement != null)
			elseStatement.resolve(scope);
	}

	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			condition.traverse(visitor, blockScope);
			if (thenStatement != null)
				thenStatement.traverse(visitor, blockScope);
			if (elseStatement != null)
				elseStatement.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.IF_STATEMENT;
	
	}
}
