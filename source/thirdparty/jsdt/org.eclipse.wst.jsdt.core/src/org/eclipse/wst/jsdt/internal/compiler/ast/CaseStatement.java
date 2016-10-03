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
import org.eclipse.wst.jsdt.core.ast.ICaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class CaseStatement extends Statement implements ICaseStatement {

	public Expression constantExpression;

	public CaseStatement(Expression constantExpression, int sourceEnd, int sourceStart) {
		this.constantExpression = constantExpression;
		this.sourceEnd = sourceEnd;
		this.sourceStart = sourceStart;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		if (constantExpression != null) {
			this.constantExpression.analyseCode(currentScope, flowContext, flowInfo);
		}
		return flowInfo;
	}

	public StringBuffer printStatement(int tab, StringBuffer output) {

		printIndent(tab, output);
		if (constantExpression == null) {
			output.append("default : "); //$NON-NLS-1$
		} else {
			output.append("case "); //$NON-NLS-1$
			constantExpression.printExpression(0, output).append(" : "); //$NON-NLS-1$
		}
		return output.append(';');
	}


	/**
	 * No-op : should use resolveCase(...) instead.
	 */
	public void resolve(BlockScope scope) {
		// no-op : should use resolveCase(...) instead.
	}

	/**
	 * Returns the constant intValue or ordinal for enum constants. If constant is NotAConstant, then answers Float.MIN_VALUE
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.Statement#resolveCase(org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding, org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement)
	 */
	public Constant resolveCase(BlockScope scope, TypeBinding switchExpressionType, SwitchStatement switchStatement) {
		// switchExpressionType maybe null in error case
	    scope.enclosingCase = this; // record entering in a switch case block

		if (constantExpression == null) {
			// remember the default case into the associated switch statement
			if (switchStatement.defaultCase != null)
				scope.problemReporter().duplicateDefaultCase(this);

			// on error the last default will be the selected one ...
			switchStatement.defaultCase = this;
			return Constant.NotAConstant;
		}
		// add into the collection of cases of the associated switch statement
		switchStatement.cases[switchStatement.caseCount++] = this;
		
		TypeBinding caseType = constantExpression.resolveType(scope);
		if (caseType == null || switchExpressionType == null) return Constant.NotAConstant;
		if (constantExpression.isConstantValueOfTypeAssignableToType(caseType, switchExpressionType)
				|| caseType.isCompatibleWith(switchExpressionType)) {
			
			return constantExpression.constant;
			
		} else if (scope.isBoxingCompatibleWith(caseType, switchExpressionType)
						|| (caseType.isBaseType()  // narrowing then boxing ?
								&& scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5 // autoboxing
								&& !switchExpressionType.isBaseType()
								&& constantExpression.isConstantValueOfTypeAssignableToType(caseType, scope.environment().computeBoxingType(switchExpressionType)))) {
			// constantExpression.computeConversion(scope, caseType, switchExpressionType); - do not report boxing/unboxing conversion
			return constantExpression.constant;
		}
		scope.problemReporter().typeMismatchError(caseType, switchExpressionType, constantExpression);
		return Constant.NotAConstant;
	}


	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			if (constantExpression != null) constantExpression.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.CASE_STATEMENT;
	
	}
}
