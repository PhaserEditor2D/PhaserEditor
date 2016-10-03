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
package org.eclipse.wst.jsdt.internal.compiler;

import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForInStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArraySingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.WithStatement;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;


/**
 * A visitor for iterating through the parse tree.
 */
public class DelegateASTVisitor extends ASTVisitor {
	org.eclipse.wst.jsdt.core.ast.ASTVisitor visitor;	
	public DelegateASTVisitor(org.eclipse.wst.jsdt.core.ast.ASTVisitor visitor)
	{
		this.visitor=visitor;
	}
	
	
	public void acceptProblem(IProblem problem) {
		visitor.acceptProblem(problem);
	}
	public void endVisit(
		AllocationExpression allocationExpression,
		BlockScope scope) {
		visitor.endVisit(allocationExpression);
	}
	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		visitor.endVisit(and_and_Expression);
	}
	public void endVisit(Argument argument, BlockScope scope) {
		visitor.endVisit(argument);
	}
	public void endVisit(Argument argument,ClassScope scope) {
		visitor.endVisit(argument);
	}
	public void endVisit(
    		ArrayAllocationExpression arrayAllocationExpression,
    		BlockScope scope) {
		visitor.endVisit(arrayAllocationExpression);
	}
	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
		visitor.endVisit(arrayInitializer);
	}
	public void endVisit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		BlockScope scope) {
		visitor.endVisit(arrayQualifiedTypeReference);
	}
	public void endVisit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		ClassScope scope) {
		visitor.endVisit(arrayQualifiedTypeReference);
	}
	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
		visitor.endVisit(arrayReference);
	}
	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		visitor.endVisit(arrayTypeReference);
	}
	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		visitor.endVisit(arrayTypeReference);
	}
	public void endVisit(Assignment assignment, BlockScope scope) {
		visitor.endVisit(assignment);
	}
	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		visitor.endVisit(binaryExpression);
	}
	public void endVisit(Block block, BlockScope scope) {
		visitor.endVisit(block);
	}
	public void endVisit(BreakStatement breakStatement, BlockScope scope) {
		visitor.endVisit(breakStatement);
	}
	public void endVisit(CaseStatement caseStatement, BlockScope scope) {
		visitor.endVisit(caseStatement);
	}
 
	public void endVisit(
		CompilationUnitDeclaration compilationUnitDeclaration,
		CompilationUnitScope scope) {
		visitor.endVisit(compilationUnitDeclaration);
	}
	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
		visitor.endVisit(compoundAssignment);
	}
	public void endVisit(
			ConditionalExpression conditionalExpression,
			BlockScope scope) {
		visitor.endVisit(conditionalExpression);
	}
	public void endVisit(
		ConstructorDeclaration constructorDeclaration,
		ClassScope scope) {
		visitor.endVisit(constructorDeclaration);
	}
	public void endVisit(ContinueStatement continueStatement, BlockScope scope) {
		visitor.endVisit(continueStatement);
	}
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		visitor.endVisit(doStatement);
	}
	public void endVisit(DoubleLiteral doubleLiteral, BlockScope scope) {
		visitor.endVisit(doubleLiteral);
	}
	public void endVisit(EmptyStatement emptyStatement, BlockScope scope) {
		visitor.endVisit(emptyStatement);
	}
	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		visitor.endVisit(equalExpression);
	}
	public void endVisit(
		ExplicitConstructorCall explicitConstructor,
		BlockScope scope) {
		visitor.endVisit(explicitConstructor);
	}
	public void endVisit(
		ExtendedStringLiteral extendedStringLiteral,
		BlockScope scope) {
		visitor.endVisit(extendedStringLiteral);
	}
	public void endVisit(FalseLiteral falseLiteral, BlockScope scope) {
		visitor.endVisit(falseLiteral);
	}
	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		visitor.endVisit( fieldDeclaration);
	}
	public void endVisit(FieldReference fieldReference, BlockScope scope) {
		visitor.endVisit( fieldReference); 
	}
	public void endVisit(FieldReference fieldReference, ClassScope scope) {
		visitor.endVisit(fieldReference);
	}
	public void endVisit(ForeachStatement forStatement, BlockScope scope) {
		visitor.endVisit(forStatement);
	}
	public void endVisit(ForStatement forStatement, BlockScope scope) {
		visitor.endVisit(forStatement);
	}
	public void endVisit(ForInStatement forInStatement, BlockScope scope) {
		visitor.endVisit(forInStatement);
	}

	public void endVisit(FunctionExpression functionExpression, BlockScope scope) {
		visitor.endVisit(functionExpression);
	}

	public void endVisit(IfStatement ifStatement, BlockScope scope) {
		visitor.endVisit(ifStatement);
	}
	public void endVisit(ImportReference importRef, CompilationUnitScope scope) {
		visitor.endVisit(importRef);
	}
	public void endVisit(InferredType inferredType, BlockScope scope) {
		visitor.endVisit(inferredType);
	}

	public void endVisit(Initializer initializer, MethodScope scope) {
		visitor.endVisit(initializer);
	}
	public void endVisit(
    		InstanceOfExpression instanceOfExpression,
    		BlockScope scope) {
		visitor.endVisit(instanceOfExpression);
	}
	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
		visitor.endVisit(intLiteral);
	}
	public void endVisit(Javadoc javadoc, BlockScope scope) {
		visitor.endVisit(javadoc);
	}
	public void endVisit(Javadoc javadoc, ClassScope scope) {
		visitor.endVisit(javadoc);
	}
	public void endVisit(JavadocAllocationExpression expression, BlockScope scope) {
		visitor.endVisit(expression);
	}
	public void endVisit(JavadocAllocationExpression expression, ClassScope scope) {
		visitor.endVisit(expression);
	}
	public void endVisit(JavadocArgumentExpression expression, BlockScope scope) {
		visitor.endVisit(expression);
	}
	public void endVisit(JavadocArgumentExpression expression, ClassScope scope) {
		visitor.endVisit(expression);
	}
	public void endVisit(JavadocArrayQualifiedTypeReference typeRef, BlockScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocArrayQualifiedTypeReference typeRef, ClassScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocArraySingleTypeReference typeRef, BlockScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocArraySingleTypeReference typeRef, ClassScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocFieldReference fieldRef, BlockScope scope) {
		visitor.endVisit(fieldRef);
	}
	public void endVisit(JavadocFieldReference fieldRef, ClassScope scope) {
		visitor.endVisit(fieldRef);
	}
	public void endVisit(JavadocImplicitTypeReference implicitTypeReference, BlockScope scope) {
		visitor.endVisit(implicitTypeReference);
	}
	public void endVisit(JavadocImplicitTypeReference implicitTypeReference, ClassScope scope) {
		visitor.endVisit(implicitTypeReference);
	}
	public void endVisit(JavadocMessageSend messageSend, BlockScope scope) {
		visitor.endVisit(messageSend);
	}
	public void endVisit(JavadocMessageSend messageSend, ClassScope scope) {
		visitor.endVisit(messageSend);
	}
	public void endVisit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocReturnStatement statement, BlockScope scope) {
		visitor.endVisit(statement);
	}
	public void endVisit(JavadocReturnStatement statement, ClassScope scope) {
		visitor.endVisit(statement);
	}
	public void endVisit(JavadocSingleNameReference argument, BlockScope scope) {
		visitor.endVisit(argument);
	}
	public void endVisit(JavadocSingleNameReference argument, ClassScope scope) {
		visitor.endVisit(argument);
	}
	public void endVisit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		visitor.endVisit(typeRef);
	}
	public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
		visitor.endVisit(labeledStatement);
	}
	public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
		visitor.endVisit(localDeclaration);
	}
	public void endVisit(ListExpression listDeclaration, BlockScope scope) {
		visitor.endVisit(listDeclaration);
	}
	public void endVisit(MessageSend messageSend, BlockScope scope) {
		visitor.endVisit(messageSend);
	}
	public void endVisit(MethodDeclaration methodDeclaration, Scope scope) {
		visitor.endVisit(methodDeclaration);
	}
	public void endVisit(StringLiteralConcatenation literal, BlockScope scope) {
		visitor.endVisit(literal);
	}
	public void endVisit(NullLiteral nullLiteral, BlockScope scope) {
		visitor.endVisit(nullLiteral);
	}
	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		visitor.endVisit(or_or_Expression);
	}
	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
		visitor.endVisit(postfixExpression);
	}
	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
		visitor.endVisit(prefixExpression);
	}
	public void endVisit(
    		QualifiedAllocationExpression qualifiedAllocationExpression,
    		BlockScope scope) {
		visitor.endVisit(qualifiedAllocationExpression);
	}
	public void endVisit(
		QualifiedNameReference qualifiedNameReference,
		BlockScope scope) {
		visitor.endVisit(qualifiedNameReference);
	}
	public void endVisit(
			QualifiedNameReference qualifiedNameReference,
			ClassScope scope) {
		visitor.endVisit(qualifiedNameReference);
	}
	public void endVisit(
    		QualifiedThisReference qualifiedThisReference,
    		BlockScope scope) {
		visitor.endVisit(qualifiedThisReference);
	}
	public void endVisit(
    		QualifiedThisReference qualifiedThisReference,
    		ClassScope scope) {
		visitor.endVisit(qualifiedThisReference);
	}
	public void endVisit(
    		QualifiedTypeReference qualifiedTypeReference,
    		BlockScope scope) {
		visitor.endVisit(qualifiedTypeReference);
	}
	public void endVisit(
    		QualifiedTypeReference qualifiedTypeReference,
    		ClassScope scope) {
		visitor.endVisit(qualifiedTypeReference);
	}

	public void endVisit(RegExLiteral stringLiteral, BlockScope scope) {
		visitor.endVisit(stringLiteral);
	}


	public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
		visitor.endVisit(returnStatement);
	}
	public void endVisit(
    		SingleNameReference singleNameReference,
    		BlockScope scope) {
		visitor.endVisit(singleNameReference);
	}
	public void endVisit(
			SingleNameReference singleNameReference,
			ClassScope scope) {
		visitor.endVisit(singleNameReference);
	}
	public void endVisit(
    		SingleTypeReference singleTypeReference,
    		BlockScope scope) {
		visitor.endVisit(singleTypeReference);
	}
	public void endVisit(
    		SingleTypeReference singleTypeReference,
    		ClassScope scope) {
		visitor.endVisit(singleTypeReference);
	}
	public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
		visitor.endVisit(stringLiteral);
	}
	public void endVisit(SuperReference superReference, BlockScope scope) {
		visitor.endVisit(superReference);
	}
	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		visitor.endVisit(switchStatement);
	}

	public void endVisit(ThisReference thisReference, BlockScope scope) {
		visitor.endVisit(thisReference);
	}
	public void endVisit(ThisReference thisReference, ClassScope scope) {
		visitor.endVisit(thisReference);
	}
	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
		visitor.endVisit(throwStatement);
	}
	public void endVisit(TrueLiteral trueLiteral, BlockScope scope) {
		visitor.endVisit(trueLiteral);
	}
	public void endVisit(TryStatement tryStatement, BlockScope scope) {
		visitor.endVisit(tryStatement);
	}
	public void endVisit(
		TypeDeclaration localTypeDeclaration,
		BlockScope scope) {
		visitor.endVisit(localTypeDeclaration);
	}
	public void endVisit(
		TypeDeclaration memberTypeDeclaration,
		ClassScope scope) {
		visitor.endVisit(memberTypeDeclaration);
	}
	public void endVisit(
		TypeDeclaration typeDeclaration,
		CompilationUnitScope scope) {
		visitor.endVisit(typeDeclaration);
	}
	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
		visitor.endVisit(unaryExpression);
	}
	public void endVisit(UndefinedLiteral undefinedLiteral, BlockScope scope) {
		visitor.endVisit(undefinedLiteral);
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		visitor.endVisit(whileStatement);
	}
	public void endVisit(WithStatement withStatement, BlockScope scope) {
		visitor.endVisit(withStatement);
	}
	public boolean visit(
    		AllocationExpression allocationExpression,
    		BlockScope scope) {
		return visitor.visit(allocationExpression);
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return visitor.visit(and_and_Expression);
	}
	public boolean visit(Argument argument, BlockScope scope) {
		return visitor.visit(argument);
	}
	public boolean visit(Argument argument, ClassScope scope) {
		return visitor.visit(argument);
	}
	public boolean visit(
		ArrayAllocationExpression arrayAllocationExpression,
		BlockScope scope) {
		return visitor.visit(arrayAllocationExpression);
	}
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return visitor.visit(arrayInitializer);
	}
	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		BlockScope scope) {
		return visitor.visit(arrayQualifiedTypeReference);
	}
	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		ClassScope scope) {
		return visitor.visit(arrayQualifiedTypeReference);
	}
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return visitor.visit(arrayReference);
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return visitor.visit(arrayTypeReference);
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return visitor.visit(arrayTypeReference);
	}
	public boolean visit(Assignment assignment, BlockScope scope) {
		return visitor.visit(assignment);
	}
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return visitor.visit(binaryExpression);
	}
	public boolean visit(Block block, BlockScope scope) {
		return visitor.visit(block);
	}
	public boolean visit(BreakStatement breakStatement, BlockScope scope) {
		return visitor.visit(breakStatement);
	}
	public boolean visit(CaseStatement caseStatement, BlockScope scope) {
		return visitor.visit(caseStatement);
	}
 
	public boolean visit(
		CompilationUnitDeclaration compilationUnitDeclaration,
		CompilationUnitScope scope) {
		return visitor.visit(compilationUnitDeclaration);
	}
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return visitor.visit(compoundAssignment);
	}
	public boolean visit(
    		ConditionalExpression conditionalExpression,
    		BlockScope scope) {
		return visitor.visit(conditionalExpression);
	}
	public boolean visit(
		ConstructorDeclaration constructorDeclaration,
		ClassScope scope) {
		return visitor.visit(constructorDeclaration);
	}
	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {
		return visitor.visit(continueStatement);
	}
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		return visitor.visit(doStatement);
	}
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return visitor.visit(doubleLiteral);
	}
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		return visitor.visit(emptyStatement);
	}
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return visitor.visit(equalExpression);
	}
	public boolean visit(
		ExplicitConstructorCall explicitConstructor,
		BlockScope scope) {
		return visitor.visit(explicitConstructor);
	}
	public boolean visit(
		ExtendedStringLiteral extendedStringLiteral,
		BlockScope scope) {
		return visitor.visit(extendedStringLiteral);
	}
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return visitor.visit(falseLiteral);
	}
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return visitor.visit(fieldDeclaration);
	}
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return visitor.visit(fieldReference);
	}
	public boolean visit(FieldReference fieldReference, ClassScope scope) {
		return visitor.visit(fieldReference);
	}
	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		return visitor.visit(forStatement);
	}
	public boolean visit(ForInStatement forInStatement, BlockScope scope) {
		return visitor.visit(forInStatement);
	}
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		return visitor.visit(forStatement);
	}
	public boolean visit(FunctionExpression functionExpression, BlockScope scope) {
		return visitor.visit(functionExpression);
	}
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		return visitor.visit(ifStatement);
	}
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return visitor.visit(importRef);
	}

	public boolean visit(InferredType inferredType, BlockScope scope) {
		return visitor.visit(inferredType);
	}

	public boolean visit(InferredMethod inferredMethod, BlockScope scope) {
		return visitor.visit(inferredMethod);
	}

	public boolean visit(InferredAttribute inferredField, BlockScope scope) {
		return visitor.visit(inferredField);
	}
	public boolean visit(Initializer initializer, MethodScope scope) {
		return visitor.visit(initializer);
	}
	public boolean visit(
    		InstanceOfExpression instanceOfExpression,
    		BlockScope scope) {
		return visitor.visit(instanceOfExpression);
	}
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return visitor.visit(intLiteral);
	}
	public boolean visit(Javadoc javadoc, BlockScope scope) {
		return visitor.visit(javadoc);
	}
	public boolean visit(Javadoc javadoc, ClassScope scope) {
		return visitor.visit(javadoc);
	}
	public boolean visit(JavadocAllocationExpression expression, BlockScope scope) {
		return visitor.visit(expression);
	}
	public boolean visit(JavadocAllocationExpression expression, ClassScope scope) {
		return visitor.visit(expression);
	}
	public boolean visit(JavadocArgumentExpression expression, BlockScope scope) {
		return visitor.visit(expression);
	}
	public boolean visit(JavadocArgumentExpression expression, ClassScope scope) {
		return visitor.visit(expression);
	}
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, BlockScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, ClassScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocArraySingleTypeReference typeRef, BlockScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocArraySingleTypeReference typeRef, ClassScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocFieldReference fieldRef, BlockScope scope) {
		return visitor.visit(fieldRef);
	}
	public boolean visit(JavadocFieldReference fieldRef, ClassScope scope) {
		return visitor.visit(fieldRef);
	}
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, BlockScope scope) {
		return visitor.visit(implicitTypeReference);
	}
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, ClassScope scope) {
		return visitor.visit(implicitTypeReference);
	}
	public boolean visit(JavadocMessageSend messageSend, BlockScope scope) {
		return visitor.visit(messageSend);
	}
	public boolean visit(JavadocMessageSend messageSend, ClassScope scope) {
		return visitor.visit(messageSend);
	}
	public boolean visit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocReturnStatement statement, BlockScope scope) {
		return visitor.visit(statement);
	}
	public boolean visit(JavadocReturnStatement statement, ClassScope scope) {
		return visitor.visit(statement);
	}
	public boolean visit(JavadocSingleNameReference argument, BlockScope scope) {
		return visitor.visit(argument);
	}
	public boolean visit(JavadocSingleNameReference argument, ClassScope scope) {
		return visitor.visit(argument);
	}
	public boolean visit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		return visitor.visit(typeRef);
	}
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		return visitor.visit(labeledStatement);
	}
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return visitor.visit(localDeclaration);
	}
	public boolean visit(ListExpression listDeclaration, BlockScope scope) {
		return visitor.visit(listDeclaration);
	}
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		return visitor.visit(messageSend);
	}
	public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
		return visitor.visit(methodDeclaration);
	}
	public boolean visit(
			StringLiteralConcatenation literal,
			BlockScope scope) {
		return visitor.visit(literal);
	}
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return visitor.visit(nullLiteral);
	}
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return visitor.visit(or_or_Expression);
	}
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return visitor.visit(postfixExpression);
	}
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return visitor.visit(prefixExpression);
	}
	public boolean visit(
    		QualifiedAllocationExpression qualifiedAllocationExpression,
    		BlockScope scope) {
		return visitor.visit(qualifiedAllocationExpression);
	}
	public boolean visit(
			QualifiedNameReference qualifiedNameReference,
			BlockScope scope) {
		return visitor.visit(qualifiedNameReference);
	}
	public boolean visit(
			QualifiedNameReference qualifiedNameReference,
			ClassScope scope) {
		return visitor.visit(qualifiedNameReference);
	}
	public boolean visit(
			QualifiedThisReference qualifiedThisReference,
			BlockScope scope) {
		return visitor.visit(qualifiedThisReference);
	}
	public boolean visit(
			QualifiedThisReference qualifiedThisReference,
			ClassScope scope) {
		return visitor.visit(qualifiedThisReference);
	}
	public boolean visit(
    		QualifiedTypeReference qualifiedTypeReference,
    		BlockScope scope) {
		return visitor.visit(qualifiedTypeReference);
	}
	public boolean visit(
    		QualifiedTypeReference qualifiedTypeReference,
    		ClassScope scope) {
		return visitor.visit(qualifiedTypeReference);
	}
	public boolean visit(RegExLiteral stringLiteral, BlockScope scope) {
		return visitor.visit(stringLiteral);
	}
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		return visitor.visit(returnStatement);
	}
	public boolean visit(
		SingleNameReference singleNameReference,
		BlockScope scope) {
		return visitor.visit(singleNameReference);
	}
	public boolean visit(
			SingleNameReference singleNameReference,
			ClassScope scope) {
		return visitor.visit(singleNameReference);
	}
	public boolean visit(
    		SingleTypeReference singleTypeReference,
    		BlockScope scope) {
		return visitor.visit(singleTypeReference);
	}
	public boolean visit(
    		SingleTypeReference singleTypeReference,
    		ClassScope scope) {
		return visitor.visit(singleTypeReference);
	}
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return visitor.visit(stringLiteral);
	}
	public boolean visit(SuperReference superReference, BlockScope scope) {
		return visitor.visit(superReference);
	}
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		return visitor.visit(switchStatement);
	}

	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return visitor.visit(thisReference);
	}
	public boolean visit(ThisReference thisReference, ClassScope scope) {
		return visitor.visit(thisReference);
	}
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		return visitor.visit(throwStatement);
	}
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return visitor.visit(trueLiteral);
	}
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		return visitor.visit(tryStatement);
	}
	public boolean visit(
		TypeDeclaration localTypeDeclaration,
		BlockScope scope) {
		return visitor.visit(localTypeDeclaration);
	}
	public boolean visit(
		TypeDeclaration memberTypeDeclaration,
		ClassScope scope) {
		return visitor.visit(memberTypeDeclaration);
	}
	public boolean visit(
		TypeDeclaration typeDeclaration,
		CompilationUnitScope scope) {
		return visitor.visit(typeDeclaration);
	}
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		return visitor.visit(unaryExpression);
	}
	public boolean visit(UndefinedLiteral undefined, BlockScope scope) {
		return visitor.visit(undefined);
	}
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		return visitor.visit(whileStatement);
	}
	public boolean visit(WithStatement whileStatement, BlockScope scope) {
		return visitor.visit(whileStatement);
	}
	public boolean visit(ObjectLiteral literal, BlockScope scope) {
		return visitor.visit(literal);
	}
	public void endVisit(ObjectLiteral literal, BlockScope scope) {
		 visitor.endVisit(literal);
	}
	public boolean visit(ObjectLiteralField field, BlockScope scope) {
		return visitor.visit(field);
	}
	public void endVisit(ObjectLiteralField field, BlockScope scope) {
		 visitor.endVisit(field);
	}
}
