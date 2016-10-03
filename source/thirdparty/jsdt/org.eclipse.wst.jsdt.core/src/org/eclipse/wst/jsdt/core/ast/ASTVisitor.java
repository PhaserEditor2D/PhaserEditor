/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.ast;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;


/**
 *  
 * A visitor for iterating through the AST Node tree.
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class ASTVisitor  {

	
	public void acceptProblem(IProblem problem) {
		// do nothing by default
	}
	public void endVisit(		IAllocationExpression allocationExpression) {
		// do nothing by default
	}
	public void endVisit(IAND_AND_Expression and_and_Expression) {
		// do nothing by default
	}
	public void endVisit(IArgument argument) {
		// do nothing by default
	}

	public void endVisit(		IArrayAllocationExpression arrayAllocationExpression) {
		// do nothing by default
	}
	public void endVisit(IArrayInitializer arrayInitializer) {
		// do nothing by default
	}
	public void endVisit(IArrayQualifiedTypeReference arrayQualifiedTypeReference) {
		// do nothing by default
	}
	public void endVisit(IArrayReference arrayReference) {
		// do nothing by default
	}
	public void endVisit(IArrayTypeReference arrayTypeReference) {
		// do nothing by default
	}
	public void endVisit(IAssignment assignment) {
		// do nothing by default
	}
	public void endVisit(IBinaryExpression binaryExpression) {
		// do nothing by default
	}
	public void endVisit(IBlock block) {
		// do nothing by default
	}
	public void endVisit(IBreakStatement breakStatement) {
		// do nothing by default
	}
	public void endVisit(ICaseStatement caseStatement) {
		// do nothing by default
	}
	public void endVisit(IScriptFileDeclaration scriptFileDeclaration) {		
		// do nothing by default
	}
	public void endVisit(ICompoundAssignment compoundAssignment) {
		// do nothing by default
	}
	public void endVisit(IConditionalExpression conditionalExpression) {
		// do nothing by default
	}
	public void endVisit(IConstructorDeclaration constructorDeclaration) {
		// do nothing by default
	}
	public void endVisit(IContinueStatement continueStatement) {
		// do nothing by default
	}
	public void endVisit(IDoStatement doStatement) {
		// do nothing by default
	}
	public void endVisit(IDoubleLiteral doubleLiteral) {
		// do nothing by default
	}
	public void endVisit(IEmptyStatement emptyStatement) {
		// do nothing by default
	}
	public void endVisit(IEqualExpression equalExpression) {
		// do nothing by default
	}
	public void endVisit(IExplicitConstructorCall explicitConstructor) {
		// do nothing by default
	}
	public void endVisit(IExtendedStringLiteral extendedStringLiteral) {
		// do nothing by default
	}
	public void endVisit(IFalseLiteral falseLiteral) {
		// do nothing by default
	}
	public void endVisit(IFieldDeclaration fieldDeclaration) {
		// do nothing by default
	}
	
	public void endVisit(IFieldReference fieldDeclaration) {
		// do nothing by default
	}
	
	public void endVisit(IForeachStatement forStatement) {
		// do nothing by default
	}
	public void endVisit(IForStatement forStatement) {
		// do nothing by default
	}
	public void endVisit(IForInStatement forInStatement) {
		// do nothing by default
	}

	public void endVisit(IFunctionExpression functionExpression) {
	}

	public void endVisit(IIfStatement ifStatement) {
		// do nothing by default
	}
	public void endVisit(IImportReference importRef) {
		// do nothing by default
	}
	public void endVisit(InferredType inferredType) {
		// do nothing by default
	}

	public void endVisit(IInitializer initializer) {
		// do nothing by default
	}
	public void endVisit(IInstanceOfExpression instanceOfExpression) {
		// do nothing by default
	}
	public void endVisit(IIntLiteral intLiteral) {
		// do nothing by default
	}
	public void endVisit(IJsDoc javadoc) {
		// do nothing by default
	}
	public void endVisit(IJsDocAllocationExpression expression) {
		// do nothing by default
	}
	public void endVisit(IJsDocArgumentExpression expression) {
		// do nothing by default
	}
	public void endVisit(IJsDocArrayQualifiedTypeReference typeRef) {
		// do nothing by default
	}
	public void endVisit(IJsDocArraySingleTypeReference typeRef) {
		// do nothing by default
	}
	public void endVisit(IJsDocFieldReference fieldRef) {
		// do nothing by default
	}
	public void endVisit(IJsDocImplicitTypeReference implicitTypeReference) {
		// do nothing by default
	}
	public void endVisit(IJsDocMessageSend messageSend) {
		// do nothing by default
	}
	public void endVisit(IJsDocQualifiedTypeReference typeRef) {
		// do nothing by default
	}
	public void endVisit(IJsDocReturnStatement statement) {
		// do nothing by default
	}
	public void endVisit(IJsDocSingleNameReference argument) {
		// do nothing by default
	}
	public void endVisit(IJsDocSingleTypeReference typeRef) {
		// do nothing by default
	}
	public void endVisit(ILabeledStatement labeledStatement) {
		// do nothing by default
	}
	public void endVisit(ILocalDeclaration localDeclaration) {
		// do nothing by default
	}
	public void endVisit(IListExpression listDeclaration) {
		// do nothing by default
	}
	public void endVisit(IFunctionCall messageSend) {
		// do nothing by default
	}
	public void endVisit(IFunctionDeclaration methodDeclaration) {
		// do nothing by default
	}
	public void endVisit(IStringLiteralConcatenation literal) {
		// do nothing by default
	}
	public void endVisit(INullLiteral nullLiteral) {
		// do nothing by default
	}
	public void endVisit(IOR_OR_Expression or_or_Expression) {
		// do nothing by default
	}
	public void endVisit(IPostfixExpression postfixExpression) {
		// do nothing by default
	}
	public void endVisit(IPrefixExpression prefixExpression) {
		// do nothing by default
	}
	public void endVisit(IQualifiedAllocationExpression qualifiedAllocationExpression) {
		// do nothing by default
	}
	public void endVisit(IQualifiedNameReference qualifiedNameReference) {
		// do nothing by default
	}
	public void endVisit(IQualifiedThisReference qualifiedThisReference) {
		// do nothing by default
	}
	public void endVisit(IQualifiedTypeReference qualifiedTypeReference) {
		// do nothing by default
	}

	public void endVisit(IRegExLiteral stringLiteral) {
		// do nothing by default
	}


	public void endVisit(IReturnStatement returnStatement) {
		// do nothing by default
	}
	public void endVisit(ISingleNameReference singleNameReference) {
		// do nothing by default
	}
	
	public void endVisit(ISingleTypeReference singleTypeReference) {
		// do nothing by default
	}
	public void endVisit(IStringLiteral stringLiteral) {
		// do nothing by default
	}
	public void endVisit(ISuperReference superReference) {
		// do nothing by default
	}
	public void endVisit(ISwitchStatement switchStatement) {
		// do nothing by default
	}

	public void endVisit(IThisReference thisReference) {
		// do nothing by default
	}
	public void endVisit(IThrowStatement throwStatement) {
		// do nothing by default
	}
	public void endVisit(ITrueLiteral trueLiteral) {
		// do nothing by default
	}
	public void endVisit(ITryStatement tryStatement) {
		// do nothing by default
	}
	public void endVisit(ITypeDeclaration memberTypeDeclaration) {
		// do nothing by default
	}
	public void endVisit(IUnaryExpression unaryExpression) {
		// do nothing by default
	}
	public void endVisit(IUndefinedLiteral undefinedLiteral) {
		// do nothing by default
	}

	public void endVisit(IWhileStatement whileStatement) {
		// do nothing by default
	}
	public void endVisit(IWithStatement whileStatement) {
		// do nothing by default
	}
	public boolean visit(IAllocationExpression allocationExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IAND_AND_Expression and_and_Expression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IArgument argument) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IArrayAllocationExpression arrayAllocationExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IArrayInitializer arrayInitializer) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IArrayQualifiedTypeReference arrayQualifiedTypeReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IArrayReference arrayReference) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IArrayTypeReference arrayTypeReference) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IAssignment assignment) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IBinaryExpression binaryExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IBlock block) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IBreakStatement breakStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ICaseStatement caseStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IScriptFileDeclaration compilationUnitDeclaration) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ICompoundAssignment compoundAssignment) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IConditionalExpression conditionalExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IConstructorDeclaration constructorDeclaration) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IContinueStatement continueStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IDoStatement doStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IDoubleLiteral doubleLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IEmptyStatement emptyStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IEqualExpression equalExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IExplicitConstructorCall explicitConstructor) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IExtendedStringLiteral extendedStringLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IFalseLiteral falseLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IFieldDeclaration fieldDeclaration) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IFieldReference fieldReference) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IForeachStatement forStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IForInStatement forInStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IForStatement forStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IFunctionExpression functionExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IIfStatement ifStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IImportReference importRef) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(InferredType inferredType) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(InferredMethod inferredMethod) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(InferredAttribute inferredField) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IInitializer initializer) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IInstanceOfExpression instanceOfExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IIntLiteral intLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IJsDoc javadoc) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocAllocationExpression expression) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocArgumentExpression expression) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocArrayQualifiedTypeReference typeRef) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocArraySingleTypeReference typeRef) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocFieldReference fieldRef) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocImplicitTypeReference implicitTypeReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocMessageSend messageSend) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocQualifiedTypeReference typeRef) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocReturnStatement statement) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocSingleNameReference argument) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IJsDocSingleTypeReference typeRef) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(ILabeledStatement labeledStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ILocalDeclaration localDeclaration) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IListExpression listDeclaration) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IFunctionCall functionCall) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IFunctionDeclaration functionDeclaration) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IStringLiteralConcatenation literal) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(INullLiteral nullLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IOR_OR_Expression or_or_Expression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IPostfixExpression postfixExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IPrefixExpression prefixExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IQualifiedAllocationExpression qualifiedAllocationExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IQualifiedNameReference qualifiedNameReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IQualifiedThisReference qualifiedThisReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IQualifiedTypeReference qualifiedTypeReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IRegExLiteral stringLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IReturnStatement returnStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ISingleNameReference singleNameReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(ISingleTypeReference singleTypeReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IStringLiteral stringLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ISuperReference superReference) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ISwitchStatement switchStatement) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IThisReference thisReference) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IThrowStatement throwStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ITrueLiteral trueLiteral) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ITryStatement tryStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ITypeDeclaration localTypeDeclaration) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IUnaryExpression unaryExpression) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IUndefinedLiteral undefined) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IWhileStatement whileStatement) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IWithStatement whileStatement) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(IObjectLiteral literal) {
		return true; // do nothing by default, keep traversing
	}
	public void endVisit(IObjectLiteral literal) {
	}
	public boolean visit(IObjectLiteralField field) {
		return true; // do nothing by default, keep traversing
	}
	public void endVisit(IObjectLiteralField field) {
	}

	
	
}
