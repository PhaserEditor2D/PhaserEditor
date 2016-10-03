/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IAbstractVariableDeclaration;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IJsDoc;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public abstract class AbstractVariableDeclaration extends Statement implements  IAbstractVariableDeclaration, InvocationSite {
	public int declarationEnd;
	public int declarationSourceEnd;
	public int declarationSourceStart;
	public int hiddenVariableDepth; // used to diagnose hiding scenarii
	public Expression initialization;
	public int modifiers;
	public int modifiersSourceStart;
	public Javadoc javadoc;


	public InferredType inferredType;
	public char[] name;

	public TypeReference type;
	
	public AbstractVariableDeclaration nextLocal;
	
	/**
	 * <p>
	 * <code>true</code> if this variable declaration is actually a reference to a type,
	 * rather then the instance of a type. <code>false</code> if this variable is
	 * a reference to an instance of a type rather then the type itself.
	 * </p>
	 */
	private boolean fIsType;

	/**
	 * <p>
	 * Default constructor.
	 * </p>
	 */
	public AbstractVariableDeclaration() {
		this.fIsType = false;
	}
	
	public InferredType getInferredType() {
		return this.inferredType;
	}
	
	public void setInferredType(InferredType type) {		
		this.inferredType = type;
	}
	
	public char[] getName() {
		return this.name;
	}
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		return flowInfo;
	}

	public static final int FIELD = 1;
	public static final int INITIALIZER = 2;
	public static final int LOCAL_VARIABLE = 4;
	public static final int PARAMETER = 5;

	/**
	 * Returns the constant kind of this variable declaration
	 */
	public abstract int getKind();

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#isSuperAccess()
	 */
	public boolean isSuperAccess() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#isTypeAccess()
	 */
	public boolean isTypeAccess() {
		return false;
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {
		printAsExpression(indent, output);
		return output.append(';');
	}

	public StringBuffer printAsExpression(int indent, StringBuffer output) {
		printIndent(indent, output);
		printModifiers(this.modifiers, output);
		output.append("var "); //$NON-NLS-1$

		printFragment(indent, output);
		if (this.nextLocal!=null)
		{
			output.append(", "); //$NON-NLS-1$
			this.nextLocal.printFragment(indent, output);
		}
		return output;
	}

	protected void printFragment(int indent, StringBuffer output) {
		if (type != null) {
			type.print(0, output).append(' ');
		}
		output.append(this.name);

		if (initialization != null) {
			output.append(" = "); //$NON-NLS-1$
			initialization.printExpression(indent, output);
		}
	}

	public void resolve(BlockScope scope) {
		// do nothing by default (redefined for local variables)
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#setActualReceiverType(org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding)
	 */
	public void setActualReceiverType(ReferenceBinding receiverType) {
		// do nothing by default
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#setDepth(int)
	 */
	public void setDepth(int depth) {

		this.hiddenVariableDepth = depth;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#setFieldIndex(int)
	 */
	public void setFieldIndex(int depth) {
		// do nothing by default
	}

	public TypeBinding getTypeBinding()
	{
		if (type!=null)
			return type.resolvedType;
		else if (inferredType!=null)
			return inferredType.binding;
		return null;

	}
	public int getASTType() {
		return IASTNode.ABSTRACT_VARIABLE_DECLARATION;
	
	}
	
	public IJsDoc getJsDoc()
	{
		return this.javadoc;
	}
	
	public IExpression getInitialization()
	{
		return this.initialization;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ast.IAssignment#setIsType(boolean)
	 */
	public void setIsType(boolean isType) {
		this.fIsType = isType;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ast.IAssignment#isType()
	 */
	public boolean isType() {
		return this.fIsType;
	}
}
