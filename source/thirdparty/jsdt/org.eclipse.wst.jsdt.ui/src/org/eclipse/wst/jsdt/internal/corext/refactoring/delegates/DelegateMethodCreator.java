/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.delegates;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.FunctionRefParameter;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Delegate creator for static and non-static methods.
 * 
 * 
 */
public class DelegateMethodCreator extends DelegateCreator {

	private ASTNode fDelegateInvocation;
	private FunctionRef fDocMethodReference;

	protected void initialize() {

		Assert.isTrue(getDeclaration() instanceof FunctionDeclaration);

		if (getNewElementName() == null)
			setNewElementName(((FunctionDeclaration) getDeclaration()).getName().getIdentifier());
		
		setInsertBefore(true); 
	}

	protected ASTNode createBody(BodyDeclaration bd) throws JavaScriptModelException {

		FunctionDeclaration methodDeclaration= (FunctionDeclaration) bd;

		// interface or abstract method ? => don't create a method body.
		if (methodDeclaration.getBody() == null)
			return null;

		return createDelegateMethodBody(methodDeclaration);
	}

	protected ASTNode createDocReference(final BodyDeclaration declaration) throws JavaScriptModelException {
		fDocMethodReference= getAst().newFunctionRef();
		fDocMethodReference.setName(getAst().newSimpleName(getNewElementName()));
		if (isMoveToAnotherFile())
			fDocMethodReference.setQualifier(createDestinationTypeName());
		createArguments((FunctionDeclaration) declaration, fDocMethodReference.parameters(), false);
		return fDocMethodReference;
	}

	protected ASTNode getBodyHead(BodyDeclaration result) {
		return result;
	}

	protected ChildPropertyDescriptor getJavaDocProperty() {
		return FunctionDeclaration.JAVADOC_PROPERTY;
	}

	protected ChildPropertyDescriptor getBodyProperty() {
		return FunctionDeclaration.BODY_PROPERTY;
	}

	/**
	 * @return the delegate incovation, either a {@link ConstructorInvocation}
	 *         or a {@link FunctionInvocation}. May be null if the delegate
	 *         method is abstract (and therefore has no body at all)
	 */
	public ASTNode getDelegateInvocation() {
		return fDelegateInvocation;
	}

	/**
	 * @return the javadoc reference to the old method in the javadoc comment.
	 * 		   May be null if no comment was created. 
	 */
	public FunctionRef getJavadocReference() {
		return fDocMethodReference;
	}

	/**
	 * Creates the corresponding statement for the method invocation, based on
	 * the return type.
	 * 
	 * @param declaration the method declaration where the invocation statement
	 *            is inserted
	 * @param invocation the method invocation being encapsulated by the
	 *            resulting statement
	 * @return the corresponding statement
	 */
	protected Statement createMethodInvocation(final FunctionDeclaration declaration, final FunctionInvocation invocation) {
		Assert.isNotNull(declaration);
		Assert.isNotNull(invocation);
		Statement statement= null;
		final Type type= declaration.getReturnType2();
		if (type == null)
			statement= createExpressionStatement(invocation);
		else {
			if (type instanceof PrimitiveType) {
				final PrimitiveType primitive= (PrimitiveType) type;
				if (primitive.getPrimitiveTypeCode().equals(PrimitiveType.VOID))
					statement= createExpressionStatement(invocation);
				else
					statement= createReturnStatement(invocation);
			} else
				statement= createReturnStatement(invocation);
		}
		return statement;
	}

	/**
	 * {@inheritDoc}
	 */
	protected IBinding getDeclarationBinding() {
		final FunctionDeclaration declaration= (FunctionDeclaration) getDeclaration();
		return declaration.resolveBinding();
	}

	private void createArguments(final FunctionDeclaration declaration, final List arguments, boolean methodInvocation) throws JavaScriptModelException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(arguments);
		SingleVariableDeclaration variable= null;
		final int size= declaration.parameters().size();
		for (int index= 0; index < size; index++) {
			variable= (SingleVariableDeclaration) declaration.parameters().get(index);

			if (methodInvocation) {
				// we are creating method invocation parameters
				final SimpleName expression= getAst().newSimpleName(variable.getName().getIdentifier());
				arguments.add(expression);
			} else {
				// we are creating type info for the javadoc
				final FunctionRefParameter parameter= getAst().newFunctionRefParameter();
				parameter.setType(ASTNodeFactory.newType(getAst(), variable));
				if ((index == size - 1) && declaration.isVarargs())
					parameter.setVarargs(true);
				arguments.add(parameter);
			}
		}
	}

	private Block createDelegateMethodBody(final FunctionDeclaration declaration) throws JavaScriptModelException {
		Assert.isNotNull(declaration);

		FunctionDeclaration old= (FunctionDeclaration) getDeclaration();
		List arguments;
		Statement call;
		if (old.isConstructor()) {
			ConstructorInvocation invocation= getAst().newConstructorInvocation();
			arguments= invocation.arguments();
			call= invocation;
			fDelegateInvocation= invocation;
		} else {
			FunctionInvocation invocation= getAst().newFunctionInvocation();
			invocation.setName(getAst().newSimpleName(getNewElementName()));
			invocation.setExpression(getAccess());
			arguments= invocation.arguments();
			call= createMethodInvocation(declaration, invocation);
			fDelegateInvocation= invocation;
		}
		createArguments(declaration, arguments, true);

		final Block body= getAst().newBlock();
		body.statements().add(call);

		return body;
	}

	/**
	 * Creates a new expression statement for the method invocation.
	 * 
	 * @param invocation the method invocation
	 * @return the corresponding statement
	 */
	private ExpressionStatement createExpressionStatement(final FunctionInvocation invocation) {
		Assert.isNotNull(invocation);
		return invocation.getAST().newExpressionStatement(invocation);
	}

	/**
	 * Creates a new return statement for the method invocation.
	 * 
	 * @param invocation the method invocation to create a return statement for
	 * @return the corresponding statement
	 */
	private ReturnStatement createReturnStatement(final FunctionInvocation invocation) {
		Assert.isNotNull(invocation);
		final ReturnStatement statement= invocation.getAST().newReturnStatement();
		statement.setExpression(invocation);
		return statement;
	}

	protected String getTextEditGroupLabel() {
		return RefactoringCoreMessages.DelegateMethodCreator_text_edit_group_field;
	}
}
