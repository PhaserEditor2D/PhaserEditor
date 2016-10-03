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
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BlockComment;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
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
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.FunctionRefParameter;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.LineComment;
import org.eclipse.wst.jsdt.core.dom.ListExpression;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
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
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.RegularExpressionLiteral;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TextElement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
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
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class ASTFlattener extends GenericVisitor {

	/**
	 * The string buffer into which the serialized representation of the AST is
	 * written.
	 */
	protected StringBuffer fBuffer;

	/**
	 * Creates a new AST printer.
	 */
	public ASTFlattener() {
		this.fBuffer= new StringBuffer();
	}

	/**
	 * Returns the string accumulated in the visit.
	 *
	 * @return the serialized 
	 */
	public String getResult() {
		return this.fBuffer.toString();
	}

	/**
	 * Resets this printer so that it can be used again.
	 */
	public void reset() {
		this.fBuffer.setLength(0);
	}

	public static String asString(ASTNode node) {
		ASTFlattener flattener= new ASTFlattener();
		node.accept(flattener);
		return flattener.getResult();
	}


	protected boolean visitNode(ASTNode node) {
		Assert.isTrue(false, "No implementation to flatten node: " + node.toString()); //$NON-NLS-1$
		return false;
	}
	
	/**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 * Used for 3.0 modifiers and annotations.
	 * 
	 * @param ext the list of modifier and annotation nodes
	 * (element type: <code>IExtendedModifiers</code>)
	 */
	private void printModifiers(List ext) {
		for (Iterator it= ext.iterator(); it.hasNext();) {
			ASTNode p= (ASTNode) it.next();
			p.accept(this);
			this.fBuffer.append(" ");//$NON-NLS-1$
		}
	}

	/**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 * Used for JLS2 modifiers.
	 * 
	 * @param modifiers the modifier flags
	 */
	private void printModifiers(int modifiers) {
//		if (Modifier.isPublic(modifiers)) {
//			this.fBuffer.append("public ");//$NON-NLS-1$
//		}
//		if (Modifier.isProtected(modifiers)) {
//			this.fBuffer.append("protected ");//$NON-NLS-1$
//		}
//		if (Modifier.isPrivate(modifiers)) {
//			this.fBuffer.append("private ");//$NON-NLS-1$
//		}
//		if (Modifier.isStatic(modifiers)) {
//			this.fBuffer.append("static ");//$NON-NLS-1$
//		}
//		if (Modifier.isAbstract(modifiers)) {
//			this.fBuffer.append("abstract ");//$NON-NLS-1$
//		}
//		if (Modifier.isFinal(modifiers)) {
//			this.fBuffer.append("final ");//$NON-NLS-1$
//		}
//		if (Modifier.isSynchronized(modifiers)) {
//			this.fBuffer.append("synchronized ");//$NON-NLS-1$
//		}
//		if (Modifier.isVolatile(modifiers)) {
//			this.fBuffer.append("volatile ");//$NON-NLS-1$
//		}
//		if (Modifier.isNative(modifiers)) {
//			this.fBuffer.append("native ");//$NON-NLS-1$
//		}
//		if (Modifier.isStrictfp(modifiers)) {
//			this.fBuffer.append("strictfp ");//$NON-NLS-1$
//		}
//		if (Modifier.isTransient(modifiers)) {
//			this.fBuffer.append("transient ");//$NON-NLS-1$
//		}
	}

	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		this.fBuffer.append("{");//$NON-NLS-1$
		for (Iterator it= node.bodyDeclarations().iterator(); it.hasNext();) {
			BodyDeclaration b= (BodyDeclaration) it.next();
			b.accept(this);
		}
		this.fBuffer.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		node.getArray().accept(this);
		this.fBuffer.append("[");//$NON-NLS-1$
		node.getIndex().accept(this);
		this.fBuffer.append("]");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		this.fBuffer.append("new ");//$NON-NLS-1$
		ArrayType at= node.getType();
		int dims= at.getDimensions();
		Type elementType= at.getElementType();
		elementType.accept(this);
		for (Iterator it= node.dimensions().iterator(); it.hasNext();) {
			this.fBuffer.append("[");//$NON-NLS-1$
			Expression e= (Expression) it.next();
			e.accept(this);
			this.fBuffer.append("]");//$NON-NLS-1$
			dims--;
		}
		// add empty "[]" for each extra array dimension
		for (int i= 0; i < dims; i++) {
			this.fBuffer.append("[]");//$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		this.fBuffer.append("[");//$NON-NLS-1$
		for (Iterator it= node.expressions().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append("]");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		node.getComponentType().accept(this);
		this.fBuffer.append("[]");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		node.getLeftHandSide().accept(this);
		this.fBuffer.append(node.getOperator().toString());
		node.getRightHandSide().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		this.fBuffer.append("{");//$NON-NLS-1$
		for (Iterator it= node.statements().iterator(); it.hasNext();) {
			Statement s= (Statement) it.next();
			s.accept(this);
		}
		this.fBuffer.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BlockComment)
	
	 */
	public boolean visit(BlockComment node) {
		this.fBuffer.append("/* */");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (node.booleanValue() == true) {
			this.fBuffer.append("true");//$NON-NLS-1$
		} else {
			this.fBuffer.append("false");//$NON-NLS-1$
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		this.fBuffer.append("break");//$NON-NLS-1$
		if (node.getLabel() != null) {
			this.fBuffer.append(" ");//$NON-NLS-1$
			node.getLabel().accept(this);
		}
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	public boolean visit(FunctionExpression node) {
		node.getMethod().accept(this);
		return false;
	}

	public boolean visit(ObjectLiteral node) {
		this.fBuffer.append("{");//$NON-NLS-1$
		for (Iterator it= node.fields().iterator(); it.hasNext();) {
			ObjectLiteralField field= (ObjectLiteralField) it.next();
			field.accept(this);
		}
		this.fBuffer.append("}");//$NON-NLS-1$
		return false;
	}

	
	
	public boolean visit(ObjectLiteralField node) {
		node.getFieldName().accept(this);
		this.fBuffer.append(" : ");//$NON-NLS-1$
		node.getInitializer().accept(this);
		return false;
	}

	
	/*
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		this.fBuffer.append("catch (");//$NON-NLS-1$
		node.getException().accept(this);
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		this.fBuffer.append(node.getEscapedValue());
		return false;
	}	
	
	public boolean visit(RegularExpressionLiteral node) {
		this.fBuffer.append(node.getRegularExpression());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			this.fBuffer.append(".");//$NON-NLS-1$
		}
		this.fBuffer.append("new ");//$NON-NLS-1$
//		if (node.getAST().apiLevel() == AST.JLS2) {
//			node.getName().accept(this);
//		}
//		if (node.getAST().apiLevel() >= AST.JLS3) {
//			if (!node.typeArguments().isEmpty()) {
//				this.fBuffer.append("<");//$NON-NLS-1$
//				for (Iterator it= node.typeArguments().iterator(); it.hasNext();) {
//					Type t= (Type) it.next();
//					t.accept(this);
//					if (it.hasNext()) {
//						this.fBuffer.append(",");//$NON-NLS-1$
//					}
//				}
//				this.fBuffer.append(">");//$NON-NLS-1$
//			}
//			node.getType().accept(this);
//		}
		node.getMember().accept(this);
		this.fBuffer.append("(");//$NON-NLS-1$
		for (Iterator it= node.arguments().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(")");//$NON-NLS-1$
		if (node.getAnonymousClassDeclaration() != null) {
			node.getAnonymousClassDeclaration().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(JavaScriptUnit)
	 */
	public boolean visit(JavaScriptUnit node) {
		if (node.getPackage() != null) {
			node.getPackage().accept(this);
		}
		for (Iterator it= node.imports().iterator(); it.hasNext();) {
			ImportDeclaration d= (ImportDeclaration) it.next();
			d.accept(this);
		}
		for (Iterator it= node.types().iterator(); it.hasNext();) {
			AbstractTypeDeclaration d= (AbstractTypeDeclaration) it.next();
			d.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		node.getExpression().accept(this);
		this.fBuffer.append("?");//$NON-NLS-1$
		node.getThenExpression().accept(this);
		this.fBuffer.append(":");//$NON-NLS-1$
		node.getElseExpression().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (!node.typeArguments().isEmpty()) {
				this.fBuffer.append("<");//$NON-NLS-1$
				for (Iterator it= node.typeArguments().iterator(); it.hasNext();) {
					Type t= (Type) it.next();
					t.accept(this);
					if (it.hasNext()) {
						this.fBuffer.append(",");//$NON-NLS-1$
					}
				}
				this.fBuffer.append(">");//$NON-NLS-1$
			}
		}
		this.fBuffer.append("this(");//$NON-NLS-1$
		for (Iterator it= node.arguments().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(");");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		this.fBuffer.append("continue");//$NON-NLS-1$
		if (node.getLabel() != null) {
			this.fBuffer.append(" ");//$NON-NLS-1$
			node.getLabel().accept(this);
		}
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		this.fBuffer.append("do ");//$NON-NLS-1$
		node.getBody().accept(this);
		this.fBuffer.append(" while (");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(");");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(EnhancedForStatement)
	 
	 */
	public boolean visit(EnhancedForStatement node) {
		this.fBuffer.append("for (");//$NON-NLS-1$
		node.getParameter().accept(this);
		this.fBuffer.append(" : ");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}


	/*
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		node.getExpression().accept(this);
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		node.getExpression().accept(this);
		this.fBuffer.append(".");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
		node.getType().accept(this);
		this.fBuffer.append(" ");//$NON-NLS-1$
		for (Iterator it= node.fragments().iterator(); it.hasNext();) {
			VariableDeclarationFragment f= (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(", ");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		this.fBuffer.append("for (");//$NON-NLS-1$
		for (Iterator it= node.initializers().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
		}
		this.fBuffer.append("; ");//$NON-NLS-1$
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
		}
		this.fBuffer.append("; ");//$NON-NLS-1$
		for (Iterator it= node.updaters().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
		}
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	public boolean visit(ForInStatement node) {
		this.fBuffer.append("for (");//$NON-NLS-1$
		if (node.getIterationVariable()  != null) {
			node.getIterationVariable().accept(this);
		}
		this.fBuffer.append(" in ");//$NON-NLS-1$
		if (node.getCollection()  != null) {
			node.getCollection().accept(this);
		}
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		this.fBuffer.append("if (");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getThenStatement().accept(this);
		if (node.getElseStatement() != null) {
			this.fBuffer.append(" else ");//$NON-NLS-1$
			node.getElseStatement().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		if (node.isFileImport())
			return false;
		this.fBuffer.append("import ");//$NON-NLS-1$
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (node.isStatic()) {
				this.fBuffer.append("static ");//$NON-NLS-1$
			}
		}
		node.getName().accept(this);
		if (node.isOnDemand()) {
			this.fBuffer.append(".*");//$NON-NLS-1$
		}
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		node.getLeftOperand().accept(this);
		this.fBuffer.append(' '); // for cases like x= i - -1; or x= i++ + ++i;
		this.fBuffer.append(node.getOperator().toString());
		this.fBuffer.append(' ');
		node.getRightOperand().accept(this);
		final List extendedOperands = node.extendedOperands();
		if (extendedOperands.size() != 0) {
			this.fBuffer.append(' ');
			for (Iterator it = extendedOperands.iterator(); it.hasNext(); ) {
				this.fBuffer.append(node.getOperator().toString()).append(' ');
				Expression e = (Expression) it.next();
				e.accept(this);
			}
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		node.getLeftOperand().accept(this);
		this.fBuffer.append(" instanceof ");//$NON-NLS-1$
		node.getRightOperand().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(JSdoc node) {
		this.fBuffer.append("/** ");//$NON-NLS-1$
		for (Iterator it= node.tags().iterator(); it.hasNext();) {
			ASTNode e= (ASTNode) it.next();
			e.accept(this);
		}
		this.fBuffer.append("\n */");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		node.getLabel().accept(this);
		this.fBuffer.append(": ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	public boolean visit(ListExpression node) {
		for (Iterator it= node.expressions().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		return false;
	}

	
	/*
	 * @see ASTVisitor#visit(LineComment)
	
	 */
	public boolean visit(LineComment node) {
		this.fBuffer.append("//\n");//$NON-NLS-1$
		return false;
	}


	/*
	 * @see ASTVisitor#visit(MemberRef)
	 
	 */
	public boolean visit(MemberRef node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
		}
		this.fBuffer.append("#");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}


	/*
	 * @see ASTVisitor#visit(FunctionRef)
	
	 */
	public boolean visit(FunctionRef node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
		}
		this.fBuffer.append("#");//$NON-NLS-1$
		node.getName().accept(this);
		this.fBuffer.append("(");//$NON-NLS-1$
		for (Iterator it= node.parameters().iterator(); it.hasNext();) {
			FunctionRefParameter e= (FunctionRefParameter) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FunctionRefParameter)
	
	 */
	public boolean visit(FunctionRefParameter node) {
		node.getType().accept(this);
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (node.isVarargs()) {
				this.fBuffer.append("...");//$NON-NLS-1$
			}
		}
		if (node.getName() != null) {
			this.fBuffer.append(" ");//$NON-NLS-1$
			node.getName().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FunctionDeclaration)
	 */
	public boolean visit(FunctionDeclaration node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
//		if (!node.isConstructor()) {
//			if (node.getAST().apiLevel() == AST.JLS2) {
//				node.getReturnType().accept(this);
//			} else {
//				if (node.getReturnType2() != null) {
//					node.getReturnType2().accept(this);
//				} else {
//					// methods really ought to have a return type
//					this.fBuffer.append("void");//$NON-NLS-1$
//				}
//			}
//			this.fBuffer.append(" ");//$NON-NLS-1$
//		}
		if (node.getName()!=null)
			node.getName().accept(this);
		this.fBuffer.append("(");//$NON-NLS-1$
		for (Iterator it= node.parameters().iterator(); it.hasNext();) {
			SingleVariableDeclaration v= (SingleVariableDeclaration) it.next();
			v.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(")");//$NON-NLS-1$
		for (int i= 0; i < node.getExtraDimensions(); i++) {
			this.fBuffer.append("[]"); //$NON-NLS-1$
		}
		if (!node.thrownExceptions().isEmpty()) {
			this.fBuffer.append(" throws ");//$NON-NLS-1$
			for (Iterator it= node.thrownExceptions().iterator(); it.hasNext();) {
				Name n= (Name) it.next();
				n.accept(this);
				if (it.hasNext()) {
					this.fBuffer.append(", ");//$NON-NLS-1$
				}
			}
			this.fBuffer.append(" ");//$NON-NLS-1$
		}
		if (node.getBody() == null) {
			this.fBuffer.append(";");//$NON-NLS-1$
		} else {
			node.getBody().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FunctionInvocation)
	 */
	public boolean visit(FunctionInvocation node) {
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			this.fBuffer.append(".");//$NON-NLS-1$
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (!node.typeArguments().isEmpty()) {
				this.fBuffer.append("<");//$NON-NLS-1$
				for (Iterator it= node.typeArguments().iterator(); it.hasNext();) {
					Type t= (Type) it.next();
					t.accept(this);
					if (it.hasNext()) {
						this.fBuffer.append(",");//$NON-NLS-1$
					}
				}
				this.fBuffer.append(">");//$NON-NLS-1$
			}
		}
		SimpleName name = node.getName();
		if (name!=null)
			name.accept(this);
		this.fBuffer.append("(");//$NON-NLS-1$
		for (Iterator it= node.arguments().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Modifier)
	
	 */
	public boolean visit(Modifier node) {
		this.fBuffer.append(node.getKeyword().toString());
		return false;
	}


	/*
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		this.fBuffer.append("null");//$NON-NLS-1$
		return false;
	}

	public boolean visit(UndefinedLiteral node) {
		this.fBuffer.append("undefined");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		this.fBuffer.append(node.getToken());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (node.getJavadoc() != null) {
				node.getJavadoc().accept(this);
			}
		}
		this.fBuffer.append("package ");//$NON-NLS-1$
		node.getName().accept(this);
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		this.fBuffer.append("(");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		node.getOperand().accept(this);
		this.fBuffer.append(node.getOperator().toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		this.fBuffer.append(node.getOperator().toString());
		node.getOperand().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		this.fBuffer.append(node.getPrimitiveTypeCode().toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		node.getQualifier().accept(this);
		this.fBuffer.append(".");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(QualifiedType)
	
	 */
	public boolean visit(QualifiedType node) {
		node.getQualifier().accept(this);
		this.fBuffer.append(".");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		this.fBuffer.append("return");//$NON-NLS-1$
		if (node.getExpression() != null) {
			this.fBuffer.append(" ");//$NON-NLS-1$
			node.getExpression().accept(this);
		}
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		this.fBuffer.append(node.getIdentifier());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
//		node.getType().accept(this);
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (node.isVarargs()) {
				this.fBuffer.append("...");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(" ");//$NON-NLS-1$
		node.getName().accept(this);
		for (int i= 0; i < node.getExtraDimensions(); i++) {
			this.fBuffer.append("[]"); //$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			this.fBuffer.append("=");//$NON-NLS-1$
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		this.fBuffer.append(node.getEscapedValue());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			this.fBuffer.append(".");//$NON-NLS-1$
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (!node.typeArguments().isEmpty()) {
				this.fBuffer.append("<");//$NON-NLS-1$
				for (Iterator it= node.typeArguments().iterator(); it.hasNext();) {
					Type t= (Type) it.next();
					t.accept(this);
					if (it.hasNext()) {
						this.fBuffer.append(",");//$NON-NLS-1$
					}
				}
				this.fBuffer.append(">");//$NON-NLS-1$
			}
		}
		this.fBuffer.append("super(");//$NON-NLS-1$
		for (Iterator it= node.arguments().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(");");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			this.fBuffer.append(".");//$NON-NLS-1$
		}
		this.fBuffer.append("super.");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			this.fBuffer.append(".");//$NON-NLS-1$
		}
		this.fBuffer.append("super.");//$NON-NLS-1$
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (!node.typeArguments().isEmpty()) {
				this.fBuffer.append("<");//$NON-NLS-1$
				for (Iterator it= node.typeArguments().iterator(); it.hasNext();) {
					Type t= (Type) it.next();
					t.accept(this);
					if (it.hasNext()) {
						this.fBuffer.append(",");//$NON-NLS-1$
					}
				}
				this.fBuffer.append(">");//$NON-NLS-1$
			}
		}
		node.getName().accept(this);
		this.fBuffer.append("(");//$NON-NLS-1$
		for (Iterator it= node.arguments().iterator(); it.hasNext();) {
			Expression e= (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(",");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		if (node.isDefault()) {
			this.fBuffer.append("default :");//$NON-NLS-1$
		} else {
			this.fBuffer.append("case ");//$NON-NLS-1$
			node.getExpression().accept(this);
			this.fBuffer.append(":");//$NON-NLS-1$
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		this.fBuffer.append("switch (");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(") ");//$NON-NLS-1$
		this.fBuffer.append("{");//$NON-NLS-1$
		for (Iterator it= node.statements().iterator(); it.hasNext();) {
			Statement s= (Statement) it.next();
			s.accept(this);
		}
		this.fBuffer.append("}");//$NON-NLS-1$
		return false;
	}


	/*
	 * @see ASTVisitor#visit(TagElement)
	
	 */
	public boolean visit(TagElement node) {
		if (node.isNested()) {
			// nested tags are always enclosed in braces
			this.fBuffer.append("{");//$NON-NLS-1$
		} else {
			// top-level tags always begin on a new line
			this.fBuffer.append("\n * ");//$NON-NLS-1$
		}
		boolean previousRequiresWhiteSpace= false;
		if (node.getTagName() != null) {
			this.fBuffer.append(node.getTagName());
			previousRequiresWhiteSpace= true;
		}
		boolean previousRequiresNewLine= false;
		for (Iterator it= node.fragments().iterator(); it.hasNext();) {
			ASTNode e= (ASTNode) it.next();
			// assume text elements include necessary leading and trailing whitespace
			// but Name, MemberRef, FunctionRef, and nested TagElement do not include white space
			boolean currentIncludesWhiteSpace= (e instanceof TextElement);
			if (previousRequiresNewLine && currentIncludesWhiteSpace) {
				this.fBuffer.append("\n * ");//$NON-NLS-1$
			}
			previousRequiresNewLine= currentIncludesWhiteSpace;
			// add space if required to separate
			if (previousRequiresWhiteSpace && !currentIncludesWhiteSpace) {
				this.fBuffer.append(" "); //$NON-NLS-1$
			}
			e.accept(this);
			previousRequiresWhiteSpace= !currentIncludesWhiteSpace && !(e instanceof TagElement);
		}
		if (node.isNested()) {
			this.fBuffer.append("}");//$NON-NLS-1$
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TextElement)
	
	 */
	public boolean visit(TextElement node) {
		this.fBuffer.append(node.getText());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			this.fBuffer.append(".");//$NON-NLS-1$
		}
		this.fBuffer.append("this");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		this.fBuffer.append("throw ");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		this.fBuffer.append("try ");//$NON-NLS-1$
		node.getBody().accept(this);
		this.fBuffer.append(" ");//$NON-NLS-1$
		for (Iterator it= node.catchClauses().iterator(); it.hasNext();) {
			CatchClause cc= (CatchClause) it.next();
			cc.accept(this);
		}
		if (node.getFinally() != null) {
			this.fBuffer.append("finally ");//$NON-NLS-1$
			node.getFinally().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
		this.fBuffer.append("class ");//$NON-NLS-1$
		node.getName().accept(this);
		this.fBuffer.append(" ");//$NON-NLS-1$
		if (node.getAST().apiLevel() == AST.JLS2) {
			if (node.getSuperclass() != null) {
				this.fBuffer.append("extends ");//$NON-NLS-1$
				node.getSuperclass().accept(this);
				this.fBuffer.append(" ");//$NON-NLS-1$
			}
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (node.getSuperclassType() != null) {
				this.fBuffer.append("extends ");//$NON-NLS-1$
				node.getSuperclassType().accept(this);
				this.fBuffer.append(" ");//$NON-NLS-1$
			}
		}
		this.fBuffer.append("{");//$NON-NLS-1$
		for (Iterator it= node.bodyDeclarations().iterator(); it.hasNext();) {
			BodyDeclaration d= (BodyDeclaration) it.next();
			d.accept(this);
		}
		this.fBuffer.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		if (node.getAST().apiLevel() == AST.JLS2) {
			node.getTypeDeclaration().accept(this);
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			node.getDeclaration().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		node.getType().accept(this);
		this.fBuffer.append(".class");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
//		node.getType().accept(this);
		this.fBuffer.append(" ");//$NON-NLS-1$
		for (Iterator it= node.fragments().iterator(); it.hasNext();) {
			VariableDeclarationFragment f= (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(", ");//$NON-NLS-1$
			}
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		node.getName().accept(this);
		for (int i= 0; i < node.getExtraDimensions(); i++) {
			this.fBuffer.append("[]");//$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			this.fBuffer.append("=");//$NON-NLS-1$
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		if (node.getAST().apiLevel() == AST.JLS2) {
			printModifiers(node.getModifiers());
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			printModifiers(node.modifiers());
		}
		node.getType().accept(this);
		this.fBuffer.append(" ");//$NON-NLS-1$
		for (Iterator it= node.fragments().iterator(); it.hasNext();) {
			VariableDeclarationFragment f= (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				this.fBuffer.append(", ");//$NON-NLS-1$
			}
		}
		this.fBuffer.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		this.fBuffer.append("while (");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	public boolean visit(WithStatement node) {
		this.fBuffer.append("with (");//$NON-NLS-1$
		node.getExpression().accept(this);
		this.fBuffer.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

}
