/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class FunctionExpression extends Expression implements IFunctionExpression {


	public MethodDeclaration methodDeclaration;

	public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}
	
	public MethodDeclaration getMethodDeclaration() {
		return this.methodDeclaration;
	}
	
	
	public FunctionExpression(MethodDeclaration methodDeclaration)
	{
		this.methodDeclaration=methodDeclaration;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		return methodDeclaration.print(indent, output);
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope))
			methodDeclaration.traverse(visitor, scope);
	}


	public TypeBinding resolveType(BlockScope scope) {
		constant = Constant.NotAConstant;
		this.methodDeclaration.setScope(new MethodScope(scope,this.methodDeclaration,false));
		
		if(!this.methodDeclaration.hasBinding()) {
			this.methodDeclaration.setBinding(this.methodDeclaration.getScope().createMethod(this.methodDeclaration, this.methodDeclaration.selector, scope.enclosingCompilationUnit(), false, false));
		}
		
		//add binding to scope only if named
		if(this.methodDeclaration.getName() != null) {
			scope.addLocalMethod(this.methodDeclaration.getBinding());
		}
		
		this.methodDeclaration.getBinding().createFunctionTypeBinding(scope);
		this.methodDeclaration.resolve(scope);
		return this.methodDeclaration.getBinding().functionTypeBinding;
	}

	public TypeBinding resolveForAllocation(BlockScope scope, ASTNode location) {
		return this.resolveType(scope);
	}

	public int nullStatus(FlowInfo flowInfo) {
			return FlowInfo.NON_NULL; // constant expression cannot be null
	}

	public FlowInfo analyseCode(
			BlockScope classScope,
			FlowContext initializationContext,
			FlowInfo flowInfo) {
		this.methodDeclaration.analyseCode(classScope, initializationContext, flowInfo.copy());
		return flowInfo;
	}
	public int getASTType() {
		return IASTNode.FUNCTION_EXPRESSION;
	
	}

}
