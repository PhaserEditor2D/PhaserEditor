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
import org.eclipse.wst.jsdt.core.ast.IListExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ListExpression extends Expression implements IListExpression {

/* Tracking helpers
 * The following are used to elaborate realistic statistics about binary
 * expressions. This must be neutralized in the released code.
 * Search the keyword BE_INSTRUMENTATION to reenable.
 * An external device must install a suitable probe so as to monitor the
 * emission of events and publish the results.
	public interface Probe {
		public void ping(int depth);
	}
	public int depthTracker;
	public static Probe probe;
 */

	public Expression []expressions;
	public Constant optimizedBooleanConstant;

public ListExpression(Expression expression1, Expression expression2) {
	if (expression1 instanceof ListExpression)
	{
		ListExpression expr1=(ListExpression)expression1;
		this.expressions=new Expression[expr1.expressions.length+1];
		System.arraycopy(expr1.expressions, 0, this.expressions, 0, expr1.expressions.length);
		this.expressions[this.expressions.length-1]=expression2;
	}
	else
		this.expressions=new Expression[]{expression1,expression2};
	this.sourceStart = expressions[0].sourceStart;
	this.sourceEnd = expressions[expressions.length-1].sourceEnd;
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext,
		FlowInfo flowInfo) {
	// keep implementation in sync with CombinedBinaryExpression#analyseCode
	 for (int i=0; i<this.expressions.length; i++)
	 {
		this.expressions[i].checkNPE(currentScope, flowContext, flowInfo);
		flowInfo = this.expressions[i].analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
	}
	 return flowInfo;
}


public boolean isCompactableOperation() {
	return true;
}



public TypeBinding resolveType(BlockScope scope) {
	// keep implementation in sync with CombinedBinaryExpression#resolveType
	// and nonRecursiveResolveTypeUpwards
	
	this.constant = Constant.NotAConstant;
	
	for (int i = 0; i < this.expressions.length; i++) {
		this.resolvedType=this.expressions[i].resolveType(scope);
		this.constant = this.expressions[i].constant;
	}

	return this.resolvedType;
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		for (int i = 0; i < this.expressions.length; i++)
			this.expressions[i].traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}

public StringBuffer printExpression(int indent, StringBuffer output) {
	output.append('(');
	for (int i = 0; i < this.expressions.length; i++) {
		if (i>0)
			output.append(", "); //$NON-NLS-1$
		this.expressions[i].printExpression(indent, output);
	}
	output.append(')');
	return output;
}
public int getASTType() {
	return IASTNode.LIST_EXPRESSION;

}
}
