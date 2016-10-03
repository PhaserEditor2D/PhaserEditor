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
import org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.wst.jsdt.internal.compiler.ast.Clinit;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DebuggerStatement;
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
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectGetterSetterField;
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
public abstract class ASTVisitor {
	public void acceptProblem(IProblem problem) {
		// do nothing by default
	}
	public void endVisit(
		AllocationExpression allocationExpression,
		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(Argument argument, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(Argument argument,ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		ArrayAllocationExpression arrayAllocationExpression,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(Assignment assignment, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(Block block, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(BreakStatement breakStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(CaseStatement caseStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ClassLiteralAccess classLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(Clinit clinit, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(
		CompilationUnitDeclaration compilationUnitDeclaration,
		CompilationUnitScope scope) {
		// do nothing by default
	}
	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
			ConditionalExpression conditionalExpression,
			BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		ConstructorDeclaration constructorDeclaration,
		ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(ContinueStatement continueStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(DoubleLiteral doubleLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(EmptyStatement emptyStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		ExplicitConstructorCall explicitConstructor,
		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		ExtendedStringLiteral extendedStringLiteral,
		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(FalseLiteral falseLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		// do nothing by default
	}
	public void endVisit(FieldReference fieldReference, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(FieldReference fieldReference, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(ForeachStatement forStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ForStatement forStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ForInStatement forInStatement, BlockScope scope) {
		// do nothing by default
	}

	public void endVisit(FunctionExpression functionExpression, BlockScope scope) {
	}

	public void endVisit(IfStatement ifStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ImportReference importRef, CompilationUnitScope scope) {
		// do nothing by default
	}
	public void endVisit(InferredType inferredType, BlockScope scope) {
		// do nothing by default
	}

	public void endVisit(Initializer initializer, MethodScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		InstanceOfExpression instanceOfExpression,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(Javadoc javadoc, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(Javadoc javadoc, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocAllocationExpression expression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocAllocationExpression expression, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocArgumentExpression expression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocArgumentExpression expression, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocArrayQualifiedTypeReference typeRef, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocArrayQualifiedTypeReference typeRef, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocArraySingleTypeReference typeRef, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocArraySingleTypeReference typeRef, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocFieldReference fieldRef, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocFieldReference fieldRef, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocImplicitTypeReference implicitTypeReference, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocImplicitTypeReference implicitTypeReference, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocMessageSend messageSend, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocMessageSend messageSend, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocReturnStatement statement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocReturnStatement statement, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocSingleNameReference argument, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocSingleNameReference argument, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ListExpression listDeclaration, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(MessageSend messageSend, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(MethodDeclaration methodDeclaration, Scope scope) {
		// do nothing by default
	}
	public void endVisit(StringLiteralConcatenation literal, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(NullLiteral nullLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		QualifiedAllocationExpression qualifiedAllocationExpression,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		QualifiedNameReference qualifiedNameReference,
		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
			QualifiedNameReference qualifiedNameReference,
			ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		QualifiedThisReference qualifiedThisReference,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		QualifiedThisReference qualifiedThisReference,
    		ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		QualifiedTypeReference qualifiedTypeReference,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		QualifiedTypeReference qualifiedTypeReference,
    		ClassScope scope) {
		// do nothing by default
	}

	public void endVisit(RegExLiteral stringLiteral, BlockScope scope) {
		// do nothing by default
	}


	public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		SingleNameReference singleNameReference,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
			SingleNameReference singleNameReference,
			ClassScope scope) {
			// do nothing by default
	}
	public void endVisit(
    		SingleTypeReference singleTypeReference,
    		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
    		SingleTypeReference singleTypeReference,
    		ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(SuperReference superReference, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		// do nothing by default
	}

	public void endVisit(ThisReference thisReference, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(ThisReference thisReference, ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(TrueLiteral trueLiteral, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(TryStatement tryStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		TypeDeclaration localTypeDeclaration,
		BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(
		TypeDeclaration memberTypeDeclaration,
		ClassScope scope) {
		// do nothing by default
	}
	public void endVisit(
		TypeDeclaration typeDeclaration,
		CompilationUnitScope scope) {
		// do nothing by default
	}
	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(UndefinedLiteral undefinedLiteral, BlockScope scope) {
		// do nothing by default
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		// do nothing by default
	}
	public void endVisit(WithStatement whileStatement, BlockScope scope) {
		// do nothing by default
	}
	public boolean visit(
    		AllocationExpression allocationExpression,
    		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Argument argument, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Argument argument, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		ArrayAllocationExpression arrayAllocationExpression,
		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Assignment assignment, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Block block, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(BreakStatement breakStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(CaseStatement caseStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Clinit clinit, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		CompilationUnitDeclaration compilationUnitDeclaration,
		CompilationUnitScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		ConditionalExpression conditionalExpression,
    		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		ConstructorDeclaration constructorDeclaration,
		ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		ExplicitConstructorCall explicitConstructor,
		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		ExtendedStringLiteral extendedStringLiteral,
		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(FieldReference fieldReference, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ForInStatement forInStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(FunctionExpression functionExpression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(InferredType inferredType, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(InferredMethod inferredMethod, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(InferredAttribute inferredField, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Initializer initializer, MethodScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		InstanceOfExpression instanceOfExpression,
    		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Javadoc javadoc, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(Javadoc javadoc, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocAllocationExpression expression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocAllocationExpression expression, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocArgumentExpression expression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocArgumentExpression expression, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocArraySingleTypeReference typeRef, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocArraySingleTypeReference typeRef, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocFieldReference fieldRef, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocFieldReference fieldRef, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocMessageSend messageSend, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocMessageSend messageSend, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocReturnStatement statement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocReturnStatement statement, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocSingleNameReference argument, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocSingleNameReference argument, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ListExpression listDeclaration, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
			StringLiteralConcatenation literal,
			BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		QualifiedAllocationExpression qualifiedAllocationExpression,
    		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
			QualifiedNameReference qualifiedNameReference,
			BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
			QualifiedNameReference qualifiedNameReference,
			ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
			QualifiedThisReference qualifiedThisReference,
			BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
			QualifiedThisReference qualifiedThisReference,
			ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		QualifiedTypeReference qualifiedTypeReference,
    		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		QualifiedTypeReference qualifiedTypeReference,
    		ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(RegExLiteral stringLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		SingleNameReference singleNameReference,
		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
			SingleNameReference singleNameReference,
			ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		SingleTypeReference singleTypeReference,
    		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
    		SingleTypeReference singleTypeReference,
    		ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(SuperReference superReference, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}

	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ThisReference thisReference, ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		TypeDeclaration localTypeDeclaration,
		BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		TypeDeclaration memberTypeDeclaration,
		ClassScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(
		TypeDeclaration typeDeclaration,
		CompilationUnitScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(UndefinedLiteral undefined, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(WithStatement whileStatement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public boolean visit(ObjectLiteral literal, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public void endVisit(ObjectLiteral literal, BlockScope scope) {
	}
	public boolean visit(ObjectLiteralField field, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public void endVisit(ObjectLiteralField field, BlockScope scope) {
	}
	public boolean visit(ObjectGetterSetterField field, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public void endVisit(ObjectGetterSetterField field, BlockScope scope) {
	}
	public boolean visit(DebuggerStatement statement, BlockScope scope) {
		return true; // do nothing by default, keep traversing
	}
	public void endVisit(DebuggerStatement statement, BlockScope scope) {
	}
}
