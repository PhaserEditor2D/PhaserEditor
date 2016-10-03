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

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.ISwitchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.SwitchFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SwitchStatement extends Statement implements ISwitchStatement{

	public Expression expression;
	public Statement[] statements;
	public BlockScope scope;
	public int explicitDeclarations;
	public CaseStatement[] cases;
	public CaseStatement defaultCase;
	public int blockStart;
	public int caseCount;
	Constant[] constants;

	// fallthrough
	public final static int CASE = 0;
	public final static int FALLTHROUGH = 1;
	public final static int ESCAPING = 2;

	// for local variables table attributes
	int preSwitchInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public FlowInfo analyseCode(
			BlockScope currentScope,
			FlowContext flowContext,
			FlowInfo flowInfo) {

	    try {
			flowInfo = expression.analyseCode(currentScope, flowContext, flowInfo);
			SwitchFlowContext switchContext =
				new SwitchFlowContext(flowContext, this);

			// analyse the block by considering specially the case/default statements (need to bind them
			// to the entry point)
			FlowInfo caseInits = FlowInfo.DEAD_END;
			// in case of statements before the first case
//			preSwitchInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(flowInfo);
			int caseIndex = 0;
			if (statements != null) {
				boolean didAlreadyComplain = false;
				int fallThroughState = CASE;
				for (int i = 0, max = statements.length; i < max; i++) {
					Statement statement = statements[i];
					if ((caseIndex < caseCount) && (statement == cases[caseIndex])) { // statement is a case
						this.scope.enclosingCase = cases[caseIndex]; // record entering in a switch case block
						caseIndex++;
						if (fallThroughState == FALLTHROUGH
								&& (statement.bits & ASTNode.DocumentedFallthrough) == 0) { // the case is not fall-through protected by a line comment
							scope.problemReporter().possibleFallThroughCase(this.scope.enclosingCase);
						}
						caseInits = caseInits.mergedWith(flowInfo.unconditionalInits());
						didAlreadyComplain = false; // reset complaint
						fallThroughState = CASE;
					} else if (statement == defaultCase) { // statement is the default case
						this.scope.enclosingCase = defaultCase; // record entering in a switch case block
						if (fallThroughState == FALLTHROUGH
								&& (statement.bits & ASTNode.DocumentedFallthrough) == 0) {
							scope.problemReporter().possibleFallThroughCase(this.scope.enclosingCase);
						}
						caseInits = caseInits.mergedWith(flowInfo.unconditionalInits());
						didAlreadyComplain = false; // reset complaint
						fallThroughState = CASE;
					} else {
						fallThroughState = FALLTHROUGH; // reset below if needed
					}
					if (!statement.complainIfUnreachable(caseInits, scope, didAlreadyComplain)) {
						caseInits = statement.analyseCode(scope, switchContext, caseInits);
						if (caseInits == FlowInfo.DEAD_END) {
							fallThroughState = ESCAPING;
						}
					} else {
						didAlreadyComplain = true;
					}
				}
			}

			final TypeBinding resolvedTypeBinding = this.expression.resolvedType;
			// if no default case, then record it may jump over the block directly to the end
			if (defaultCase == null) {
				// only retain the potential initializations
				flowInfo.addPotentialInitializationsFrom(
					caseInits.mergedWith(switchContext.initsOnBreak));
//				mergedInitStateIndex =
//					currentScope.methodScope().recordInitializationStates(flowInfo);
				return flowInfo;
			}

			// merge all branches inits
			FlowInfo mergedInfo = caseInits.mergedWith(switchContext.initsOnBreak);
//			mergedInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(mergedInfo);
			return mergedInfo;
	    } finally {
	        if (this.scope != null) this.scope.enclosingCase = null; // no longer inside switch case block
	    }
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		printIndent(indent, output).append("switch ("); //$NON-NLS-1$
		expression.printExpression(0, output).append(") {"); //$NON-NLS-1$
		if (statements != null) {
			for (int i = 0; i < statements.length; i++) {
				output.append('\n');
				if (statements[i] instanceof CaseStatement) {
					statements[i].printStatement(indent, output);
				} else {
					statements[i].printStatement(indent+2, output);
				}
			}
		}
		output.append("\n"); //$NON-NLS-1$
		return printIndent(indent, output).append('}');
	}

	public void resolve(BlockScope upperScope) {

	    try {
			TypeBinding expressionType = expression.resolveType(upperScope);
			if (statements != null) {
				scope = !JavaScriptCore.IS_ECMASCRIPT4 ? upperScope :  new BlockScope(upperScope);
				int length;
				// collection of cases is too big but we will only iterate until caseCount
				cases = new CaseStatement[length = statements.length];
				this.caseCount = 0;
				this.constants = new Constant[length];
				CaseStatement[] duplicateCaseStatements = null;
				int duplicateCaseStatementsCounter = 0;
				int counter = 0;
				for (int i = 0; i < length; i++) {
					Constant constant;
					final Statement statement = statements[i];
					constant = statement.resolveCase(scope, expressionType, this);
					if (constant == Constant.NotAConstant && statement instanceof CaseStatement) {
						CaseStatement cs = (CaseStatement) statement;
						if (cs.constantExpression != null && cs.constantExpression.constant != null) {
							constant = cs.constantExpression.constant;
						}
					}
					if (constant != Constant.NotAConstant) {
						Constant key = constant;
						if (constant==null)
							continue;
						//----check for duplicate case statement------------
						for (int j = 0; j < counter; j++) {
							if (this.constants[j].equals(key)) {
								final CaseStatement currentCaseStatement = (CaseStatement) statement;
								if (duplicateCaseStatements == null) {
									scope.problemReporter().duplicateCase(cases[j]);
									scope.problemReporter().duplicateCase(currentCaseStatement);
									duplicateCaseStatements = new CaseStatement[length];
									duplicateCaseStatements[duplicateCaseStatementsCounter++] = cases[j];
									duplicateCaseStatements[duplicateCaseStatementsCounter++] = currentCaseStatement;
								} else {
									boolean found = false;
									searchReportedDuplicate: for (int k = 2; k < duplicateCaseStatementsCounter; k++) {
										if (duplicateCaseStatements[k] == statement) {
											found = true;
											break searchReportedDuplicate;
										}
									}
									if (!found) {
										scope.problemReporter().duplicateCase(currentCaseStatement);
										duplicateCaseStatements[duplicateCaseStatementsCounter++] = currentCaseStatement;
									}
								}
							}
						}
						this.constants[counter++] = key;
					}
				}
				if (length != counter) { // resize constants array
					System.arraycopy(this.constants, 0, this.constants = new Constant[counter], 0, counter);
				}
			} else {
				if ((this.bits & UndocumentedEmptyBlock) != 0) {
					upperScope.problemReporter().undocumentedEmptyBlock(this.blockStart, this.sourceEnd);
				}
			}
	    } finally {
	        if (this.scope != null) this.scope.enclosingCase = null; // no longer inside switch case block
	    }
	}

	public void traverse(
			ASTVisitor visitor,
			BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			if (this.scope == null) 
				this.scope = blockScope;
			expression.traverse(visitor, scope);
			if (statements != null) {
				int statementsLength = statements.length;
				for (int i = 0; i < statementsLength; i++)
					statements[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.SWITCH_STATEMENT;
	
	}

}
