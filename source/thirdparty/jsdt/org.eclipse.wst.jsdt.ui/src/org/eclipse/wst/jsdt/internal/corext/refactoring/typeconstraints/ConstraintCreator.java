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
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.CharacterLiteral;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.ListExpression;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteralField;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.RegularExpressionLiteral;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.TypeLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;

/**
 * Empty implementation of a creator - provided to allow subclasses to override only a subset of methods.
 * Subclass to provide constraint creation functionality.
 */
public class ConstraintCreator {

	public static final ITypeConstraint[] EMPTY_ARRAY= new ITypeConstraint[0];
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration)
	 */
	public ITypeConstraint[] create(AnonymousClassDeclaration node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayAccess)
	 */
	public ITypeConstraint[] create(ArrayAccess node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayCreation)
	 */
	public ITypeConstraint[] create(ArrayCreation node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayInitializer)
	 */
	public ITypeConstraint[] create(ArrayInitializer node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayType)
	 */
	public ITypeConstraint[] create(ArrayType node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Assignment)
	 */
	public ITypeConstraint[] create(Assignment node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Block)
	 */
	public ITypeConstraint[] create(Block node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.BooleanLiteral)
	 */
	public ITypeConstraint[] create(BooleanLiteral node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.BreakStatement)
	 */
	public ITypeConstraint[] create(BreakStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.CatchClause)
	 */
	public ITypeConstraint[] create(CatchClause node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.CharacterLiteral)
	 */
	public ITypeConstraint[] create(CharacterLiteral node) {
		return EMPTY_ARRAY;
	}

	public ITypeConstraint[] create(RegularExpressionLiteral node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation)
	 */
	public ITypeConstraint[] create(ClassInstanceCreation node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.JavaScriptUnit)
	 */
	public ITypeConstraint[] create(JavaScriptUnit node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ConditionalExpression)
	 */
	public ITypeConstraint[] create(ConditionalExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ConstructorInvocation)
	 */
	public ITypeConstraint[] create(ConstructorInvocation node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ContinueStatement)
	 */
	public ITypeConstraint[] create(ContinueStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.DoStatement)
	 */
	public ITypeConstraint[] create(DoStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.EmptyStatement)
	 */
	public ITypeConstraint[] create(EmptyStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ExpressionStatement)
	 */
	public ITypeConstraint[] create(ExpressionStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FieldAccess)
	 */
	public ITypeConstraint[] create(FieldAccess node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FieldDeclaration)
	 */
	public ITypeConstraint[] create(FieldDeclaration node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ForStatement)
	 */
	public ITypeConstraint[] create(ForStatement node) {
		return EMPTY_ARRAY;
	}
	public ITypeConstraint[] create(ForInStatement node) {
		return EMPTY_ARRAY;
	}
	public ITypeConstraint[] create(FunctionExpression node) {
		return EMPTY_ARRAY;
	}
	public ITypeConstraint[] create(ObjectLiteral node) {
		return EMPTY_ARRAY;
	}
	public ITypeConstraint[] create(ObjectLiteralField node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.IfStatement)
	 */
	public ITypeConstraint[] create(IfStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ImportDeclaration)
	 */
	public ITypeConstraint[] create(ImportDeclaration node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.InfixExpression)
	 */
	public ITypeConstraint[] create(InfixExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Initializer)
	 */
	public ITypeConstraint[] create(Initializer node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.InstanceofExpression)
	 */
	public ITypeConstraint[] create(InstanceofExpression node) {
		return EMPTY_ARRAY;
	}

	public ITypeConstraint[] create(ListExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Javadoc)
	 */
	public ITypeConstraint[] create(JSdoc node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.LabeledStatement)
	 */
	public ITypeConstraint[] create(LabeledStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionDeclaration)
	 */
	public ITypeConstraint[] create(FunctionDeclaration node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionInvocation)
	 */
	public ITypeConstraint[] create(FunctionInvocation node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.NullLiteral)
	 */
	public ITypeConstraint[] create(NullLiteral node) {
		return EMPTY_ARRAY;
	}

	public ITypeConstraint[] create(UndefinedLiteral node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.NumberLiteral)
	 */
	public ITypeConstraint[] create(NumberLiteral node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PackageDeclaration)
	 */
	public ITypeConstraint[] create(PackageDeclaration node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression)
	 */
	public ITypeConstraint[] create(ParenthesizedExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PostfixExpression)
	 */
	public ITypeConstraint[] create(PostfixExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PrefixExpression)
	 */
	public ITypeConstraint[] create(PrefixExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PrimitiveType)
	 */
	public ITypeConstraint[] create(PrimitiveType node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.QualifiedName)
	 */
	public ITypeConstraint[] create(QualifiedName node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ReturnStatement)
	 */
	public ITypeConstraint[] create(ReturnStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SimpleName)
	 */
	public ITypeConstraint[] create(SimpleName node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SimpleType)
	 */
	public ITypeConstraint[] create(SimpleType node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration)
	 */
	public ITypeConstraint[] create(SingleVariableDeclaration node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.StringLiteral)
	 */
	public ITypeConstraint[] create(StringLiteral node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation)
	 */
	public ITypeConstraint[] create(SuperConstructorInvocation node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperFieldAccess)
	 */
	public ITypeConstraint[] create(SuperFieldAccess node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation)
	 */
	public ITypeConstraint[] create(SuperMethodInvocation node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SwitchCase)
	 */
	public ITypeConstraint[] create(SwitchCase node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SwitchStatement)
	 */
	public ITypeConstraint[] create(SwitchStatement node) {
		return EMPTY_ARRAY;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ThisExpression)
	 */
	public ITypeConstraint[] create(ThisExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ThrowStatement)
	 */
	public ITypeConstraint[] create(ThrowStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TryStatement)
	 */
	public ITypeConstraint[] create(TryStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TypeDeclaration)
	 */
	public ITypeConstraint[] create(TypeDeclaration node) {
		return EMPTY_ARRAY;
		
		// TODO account for enums and annotations
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement)
	 */
	public ITypeConstraint[] create(TypeDeclarationStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TypeLiteral)
	 */
	public ITypeConstraint[] create(TypeLiteral node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression)
	 */
	public ITypeConstraint[] create(VariableDeclarationExpression node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment)
	 */
	public ITypeConstraint[] create(VariableDeclarationFragment node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement)
	 */
	public ITypeConstraint[] create(VariableDeclarationStatement node) {
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.WhileStatement)
	 */
	public ITypeConstraint[] create(WhileStatement node) {
		return EMPTY_ARRAY;
	}

	public ITypeConstraint[] create(WithStatement node) {
		return EMPTY_ARRAY;
	}

}
