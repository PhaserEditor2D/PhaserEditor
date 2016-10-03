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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
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


public final class ConstraintCollector extends ASTVisitor {

	private final ConstraintCreator fCreator;
	private final Set fConstraints;
	
	public ConstraintCollector() {
		this(new FullConstraintCreator());
	}
	
	public ConstraintCollector(ConstraintCreator creator) {
		Assert.isNotNull(creator);
		fCreator= creator;
		fConstraints= new LinkedHashSet();
	}

	private void add(ITypeConstraint[] constraints){
		fConstraints.addAll(Arrays.asList(constraints));
	}
	
	public void clear(){
		fConstraints.clear();
	}

	public ITypeConstraint[] getConstraints(){
		return (ITypeConstraint[]) fConstraints.toArray(new ITypeConstraint[fConstraints.size()]);
	}
	
	//------------------------- visit methods -------------------------//
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ArrayType)
	 */
	public boolean visit(ArrayType node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Assignment)
	 */
	public boolean visit(Assignment node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Block)
	 */
	public boolean visit(Block node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.CatchClause)
	 */
	public boolean visit(CatchClause node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.CharacterLiteral)
	 */
	public boolean visit(RegularExpressionLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.JavaScriptUnit)
	 */
	public boolean visit(JavaScriptUnit node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.DoStatement)
	 */
	public boolean visit(DoStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ForStatement)
	 */
	public boolean visit(ForStatement node) {
		add(fCreator.create(node));
		return true;
	}
	public boolean visit(FunctionExpression node) {
		add(fCreator.create(node));
		return true;
	}
	public boolean visit(ObjectLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	public boolean visit(ObjectLiteralField node) {
		add(fCreator.create(node));
		return true;
	}
	public boolean visit(ForInStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.IfStatement)
	 */
	public boolean visit(IfStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Initializer)
	 */
	public boolean visit(Initializer node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Javadoc)
	 */
	public boolean visit(JSdoc node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	public boolean visit(ListExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionDeclaration)
	 */
	public boolean visit(FunctionDeclaration node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionInvocation)
	 */
	public boolean visit(FunctionInvocation node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	public boolean visit(UndefinedLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SimpleName)
	 */
	public boolean visit(SimpleName node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SimpleType)
	 */
	public boolean visit(SimpleType node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		add(fCreator.create(node));
		return true;
	}
	

	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TryStatement)
	 */
	public boolean visit(TryStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		add(fCreator.create(node));
		return true;
		
		// TODO account for enums and annotations
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		add(fCreator.create(node));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		add(fCreator.create(node));
		return true;
	}	
	
	public boolean visit(WithStatement node) {
		add(fCreator.create(node));
		return true;
	}
}
