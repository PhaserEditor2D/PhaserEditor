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
import org.eclipse.wst.jsdt.core.ast.IThrowStatement;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ThrowStatement extends Statement implements IThrowStatement {

	public Expression exception;
	public TypeBinding exceptionType;

public ThrowStatement(Expression exception, int sourceStart, int sourceEnd) {
	this.exception = exception;
	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	this.exception.analyseCode(currentScope, flowContext, flowInfo);
	// need to check that exception thrown is actually caught somewhere
	//flowContext.checkExceptionHandlers(this.exceptionType, this, flowInfo, currentScope);
	return FlowInfo.DEAD_END;
}

public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output).append("throw "); //$NON-NLS-1$
	this.exception.printExpression(0, output);
	return output.append(';');
}

public void resolve(BlockScope scope) {
	this.exceptionType = this.exception.resolveType(scope);
	if (this.exceptionType == null || !this.exceptionType.isValidBinding()) {
		this.exceptionType = new ProblemReferenceBinding(CharOperation.NO_CHAR_CHAR,null,0);
	}
}

public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope))
		this.exception.traverse(visitor, blockScope);
	visitor.endVisit(this, blockScope);
}
public int getASTType() {
	return IASTNode.THROW_STATEMENT;

}
}
