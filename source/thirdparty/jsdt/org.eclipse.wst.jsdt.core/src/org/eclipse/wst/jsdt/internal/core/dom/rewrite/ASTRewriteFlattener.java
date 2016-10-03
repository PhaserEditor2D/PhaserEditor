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
package org.eclipse.wst.jsdt.internal.core.dom.rewrite;

import java.util.List;

import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BlockComment;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.CharacterLiteral;
import org.eclipse.wst.jsdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
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
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
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
import org.eclipse.wst.jsdt.internal.compiler.util.Util;


public class ASTRewriteFlattener extends ASTVisitor {

	/**
	 * Internal synonynm for deprecated constant AST.JSL2
	 * to alleviate deprecation warnings.
	 * @deprecated
	 */
	/*package*/ static final int JLS2_INTERNAL = AST.JLS2;

	public static String asString(ASTNode node, RewriteEventStore store) {
		ASTRewriteFlattener flattener= new ASTRewriteFlattener(store);
		node.accept(flattener);
		return flattener.getResult();
	}

	protected StringBuffer result;
	private RewriteEventStore store;

	public ASTRewriteFlattener(RewriteEventStore store) {
		this.store= store;
		this.result= new StringBuffer();
	}

	/**
	 * Returns the string accumulated in the visit.
	 *
	 * @return the serialized
	 */
	public String getResult() {
		// convert to a string, but lose any extra space in the string buffer by copying
		return new String(this.result.toString());
	}

	/**
	 * Resets this printer so that it can be used again.
	 */
	public void reset() {
		this.result.setLength(0);
	}

	/**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 *
	 * @param modifiers the modifiers
	 * @param buf The <code>StringBuffer</code> to write the result to.
	 */
	public static void printModifiers(int modifiers, StringBuffer buf) {
//		if (Modifier.isPublic(modifiers)) {
//			buf.append("public "); //$NON-NLS-1$
//		}
//		if (Modifier.isProtected(modifiers)) {
//			buf.append("protected "); //$NON-NLS-1$
//		}
//		if (Modifier.isPrivate(modifiers)) {
//			buf.append("private "); //$NON-NLS-1$
//		}
//		if (Modifier.isStatic(modifiers)) {
//			buf.append("static "); //$NON-NLS-1$
//		}
//		if (Modifier.isAbstract(modifiers)) {
//			buf.append("abstract "); //$NON-NLS-1$
//		}
//		if (Modifier.isFinal(modifiers)) {
//			buf.append("final "); //$NON-NLS-1$
//		}
//		if (Modifier.isSynchronized(modifiers)) {
//			buf.append("synchronized "); //$NON-NLS-1$
//		}
//		if (Modifier.isVolatile(modifiers)) {
//			buf.append("volatile "); //$NON-NLS-1$
//		}
//		if (Modifier.isNative(modifiers)) {
//			buf.append("native "); //$NON-NLS-1$
//		}
//		if (Modifier.isStrictfp(modifiers)) {
//			buf.append("strictfp "); //$NON-NLS-1$
//		}
//		if (Modifier.isTransient(modifiers)) {
//			buf.append("transient "); //$NON-NLS-1$
//		}
	}

	protected List getChildList(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return (List) getAttribute(parent, childProperty);
	}

	protected ASTNode getChildNode(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return (ASTNode) getAttribute(parent, childProperty);
	}

	protected int getIntAttribute(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return ((Integer) getAttribute(parent, childProperty)).intValue();
	}

	protected boolean getBooleanAttribute(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return ((Boolean) getAttribute(parent, childProperty)).booleanValue();
	}

	protected Object getAttribute(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return this.store.getNewValue(parent, childProperty);
	}

	protected void visitList(ASTNode parent, StructuralPropertyDescriptor childProperty, String separator) {
		List list= getChildList(parent, childProperty);
		for (int i= 0; i < list.size(); i++) {
			if (separator != null && i > 0) {
				this.result.append(separator);
			}
			((ASTNode) list.get(i)).accept(this);
		}
	}

	protected void visitList(ASTNode parent, StructuralPropertyDescriptor childProperty, String separator, String lead, String post) {
		List list= getChildList(parent, childProperty);
		if (!list.isEmpty()) {
			this.result.append(lead);
			for (int i= 0; i < list.size(); i++) {
				if (separator != null && i > 0) {
					this.result.append(separator);
				}
				((ASTNode) list.get(i)).accept(this);
			}
			this.result.append(post);
		}
	}


	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		this.result.append('{');
		visitList(node, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		getChildNode(node, ArrayAccess.ARRAY_PROPERTY).accept(this);
		this.result.append('[');
		getChildNode(node, ArrayAccess.INDEX_PROPERTY).accept(this);
		this.result.append(']');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		this.result.append("new "); //$NON-NLS-1$
		ArrayType arrayType= (ArrayType) getChildNode(node, ArrayCreation.TYPE_PROPERTY);

		// get the element type and count dimensions
		Type elementType= (Type) getChildNode(arrayType, ArrayType.COMPONENT_TYPE_PROPERTY);
		int dimensions= 1; // always include this array type
		while (elementType.isArrayType()) {
			dimensions++;
			elementType = (Type) getChildNode(elementType, ArrayType.COMPONENT_TYPE_PROPERTY);
		}

		elementType.accept(this);

		List list= getChildList(node, ArrayCreation.DIMENSIONS_PROPERTY);
		for (int i= 0; i < list.size(); i++) {
			this.result.append('[');
			((ASTNode) list.get(i)).accept(this);
			this.result.append(']');
			dimensions--;
		}

		// add empty "[]" for each extra array dimension
		for (int i= 0; i < dimensions; i++) {
			this.result.append("[]"); //$NON-NLS-1$
		}
		ASTNode initializer= getChildNode(node, ArrayCreation.INITIALIZER_PROPERTY);
		if (initializer != null) {
			getChildNode(node, ArrayCreation.INITIALIZER_PROPERTY).accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		this.result.append('[');
		visitList(node, ArrayInitializer.EXPRESSIONS_PROPERTY, String.valueOf(','));
		this.result.append(']');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		getChildNode(node, ArrayType.COMPONENT_TYPE_PROPERTY).accept(this);
		this.result.append("[]"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		getChildNode(node, Assignment.LEFT_HAND_SIDE_PROPERTY).accept(this);
		this.result.append(getAttribute(node, Assignment.OPERATOR_PROPERTY).toString());
		getChildNode(node, Assignment.RIGHT_HAND_SIDE_PROPERTY).accept(this);
		return false;
	}



	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		this.result.append('{');
		visitList(node, Block.STATEMENTS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (node.booleanValue() == true) {
			this.result.append("true"); //$NON-NLS-1$
		} else {
			this.result.append("false"); //$NON-NLS-1$
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		this.result.append("break"); //$NON-NLS-1$
		ASTNode label= getChildNode(node, BreakStatement.LABEL_PROPERTY);
		if (label != null) {
			this.result.append(' ');
			label.accept(this);
		}
		this.result.append(';');
		return false;
	}

	public boolean visit(FunctionExpression node) {
		getChildNode(node, FunctionExpression.METHOD_PROPERTY).accept(this);
		return false;
	}

	public boolean visit(ObjectLiteralField node) {
		getChildNode(node, ObjectLiteralField.FIELD_NAME_PROPERTY).accept(this);
		this.result.append(" : "); //$NON-NLS-1$
		getChildNode(node, ObjectLiteralField.INITIALIZER_PROPERTY).accept(this);
		return false;
	}

	public boolean visit(ObjectLiteral node) {
		this.result.append('{');
		visitList(node, ObjectLiteral.FIELDS_PROPERTY, String.valueOf(','));
		this.result.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		this.result.append("catch ("); //$NON-NLS-1$
		getChildNode(node, CatchClause.EXCEPTION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, CatchClause.BODY_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		this.result.append(getAttribute(node, CharacterLiteral.ESCAPED_VALUE_PROPERTY));
		return false;
	}

	public boolean visit(RegularExpressionLiteral node) {
		this.result.append(getAttribute(node, RegularExpressionLiteral.REGULAR_EXPRESSION_PROPERTY));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		ASTNode expression= getChildNode(node, ClassInstanceCreation.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
			this.result.append('.');
		}
		this.result.append("new ");//$NON-NLS-1$
//		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
//			getChildNode(node, ClassInstanceCreation.NAME_PROPERTY).accept(this);
//		} else {
//			visitList(node, ClassInstanceCreation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
//			getChildNode(node, ClassInstanceCreation.TYPE_PROPERTY).accept(this);
//		}
		getChildNode(node, ClassInstanceCreation.MEMBER_PROPERTY).accept(this);


		this.result.append('(');
		visitList(node, ClassInstanceCreation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		ASTNode decl= getChildNode(node, ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY);
		if (decl != null) {
			decl.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(JavaScriptUnit)
	 */
	public boolean visit(JavaScriptUnit node) {
		ASTNode pack= getChildNode(node, JavaScriptUnit.PACKAGE_PROPERTY);
		if (pack != null) {
			pack.accept(this);
		}
		visitList(node, JavaScriptUnit.IMPORTS_PROPERTY, null);
//		visitList(node, JavaScriptUnit.TYPES_PROPERTY, null);
		visitList(node, JavaScriptUnit.STATEMENTS_PROPERTY, null);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		getChildNode(node, ConditionalExpression.EXPRESSION_PROPERTY).accept(this);
		this.result.append('?');
		getChildNode(node, ConditionalExpression.THEN_EXPRESSION_PROPERTY).accept(this);
		this.result.append(':');
		getChildNode(node, ConditionalExpression.ELSE_EXPRESSION_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		if (node.getAST().apiLevel() >= AST.JLS3) {
			visitList(node, ConstructorInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}
		this.result.append("this("); //$NON-NLS-1$
		visitList(node, ConstructorInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		this.result.append("continue"); //$NON-NLS-1$
		ASTNode label= getChildNode(node, ContinueStatement.LABEL_PROPERTY);
		if (label != null) {
			this.result.append(' ');
			label.accept(this);
		}
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		this.result.append("do "); //$NON-NLS-1$
		getChildNode(node, DoStatement.BODY_PROPERTY).accept(this);
		this.result.append(" while ("); //$NON-NLS-1$
		getChildNode(node, DoStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		getChildNode(node, ExpressionStatement.EXPRESSION_PROPERTY).accept(this);
		if (node.getParent() == null || node.getParent().getNodeType()!=ASTNode.FOR_IN_STATEMENT)
			this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		getChildNode(node, FieldAccess.EXPRESSION_PROPERTY).accept(this);
		this.result.append('.');
		getChildNode(node, FieldAccess.NAME_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		ASTNode javadoc= getChildNode(node, FieldDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, FieldDeclaration.MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, FieldDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
//		getChildNode(node, FieldDeclaration.TYPE_PROPERTY).accept(this);
		this.result.append("var "); //$NON-NLS-1$
		visitList(node, FieldDeclaration.FRAGMENTS_PROPERTY, String.valueOf(','));
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		this.result.append("for ("); //$NON-NLS-1$
		visitList(node, ForStatement.INITIALIZERS_PROPERTY, String.valueOf(','));
		this.result.append(';');
		ASTNode expression= getChildNode(node, ForStatement.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
		}
		this.result.append(';');
		visitList(node, ForStatement.UPDATERS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		getChildNode(node, ForStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	public boolean visit(ForInStatement node) {
		this.result.append("for ("); //$NON-NLS-1$
		ASTNode expression= getChildNode(node, ForInStatement.ITERATION_VARIABLE_PROPERTY);
		if (expression != null) {
			expression.accept(this);
		}
		this.result.append(" in "); //$NON-NLS-1$
		expression= getChildNode(node, ForInStatement.COLLECTION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
		}
		this.result.append(')');
		getChildNode(node, ForInStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		this.result.append("if ("); //$NON-NLS-1$
		getChildNode(node, IfStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, IfStatement.THEN_STATEMENT_PROPERTY).accept(this);
		ASTNode elseStatement= getChildNode(node, IfStatement.ELSE_STATEMENT_PROPERTY);
		if (elseStatement != null) {
			this.result.append(" else "); //$NON-NLS-1$
			elseStatement.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		if (node.isFileImport())
			return false;
		this.result.append("import "); //$NON-NLS-1$
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (getBooleanAttribute(node, ImportDeclaration.STATIC_PROPERTY)) {
				this.result.append("static ");//$NON-NLS-1$
			}
		}
		getChildNode(node, ImportDeclaration.NAME_PROPERTY).accept(this);
		if (getBooleanAttribute(node, ImportDeclaration.ON_DEMAND_PROPERTY)) {
			this.result.append(".*"); //$NON-NLS-1$
		}
		this.result.append(';');
		return false;
	}



	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		getChildNode(node, InfixExpression.LEFT_OPERAND_PROPERTY).accept(this);
		this.result.append(' ');
		String operator= getAttribute(node, InfixExpression.OPERATOR_PROPERTY).toString();

		this.result.append(operator);
		this.result.append(' ');
		getChildNode(node, InfixExpression.RIGHT_OPERAND_PROPERTY).accept(this);

		List list= getChildList(node, InfixExpression.EXTENDED_OPERANDS_PROPERTY);
		for (int i= 0; i < list.size(); i++) {
			this.result.append(operator);
			((ASTNode) list.get(i)).accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		getChildNode(node, InstanceofExpression.LEFT_OPERAND_PROPERTY).accept(this);
		this.result.append(" instanceof "); //$NON-NLS-1$
		getChildNode(node, InstanceofExpression.RIGHT_OPERAND_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		ASTNode javadoc= getChildNode(node, Initializer.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, Initializer.MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, Initializer.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, Initializer.BODY_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(JSdoc node) {
		this.result.append("/**"); //$NON-NLS-1$
		List list= getChildList(node, JSdoc.TAGS_PROPERTY);
		for (int i= 0; i < list.size(); i++) {
			this.result.append("\n * "); //$NON-NLS-1$
			((ASTNode) list.get(i)).accept(this);
		}
		this.result.append("\n */"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		getChildNode(node, LabeledStatement.LABEL_PROPERTY).accept(this);
		this.result.append(": "); //$NON-NLS-1$
		getChildNode(node, LabeledStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FunctionDeclaration)
	 */
	public boolean visit(FunctionDeclaration node) {
		ASTNode javadoc= getChildNode(node, FunctionDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, FunctionDeclaration.MODIFIERS_PROPERTY), this.result);
		} else {
//			visitList(node, FunctionDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
//			visitList(node, FunctionDeclaration.TYPE_PARAMETERS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}

		this.result.append("function "); //$NON-NLS-1$

//		if (!getBooleanAttribute(node, FunctionDeclaration.CONSTRUCTOR_PROPERTY)) {
//			if (node.getAST().apiLevel() == JLS2_INTERNAL) {
//				getChildNode(node, FunctionDeclaration.RETURN_TYPE_PROPERTY).accept(this);
//			} else {
//				ASTNode returnType = getChildNode(node, FunctionDeclaration.RETURN_TYPE2_PROPERTY);
//				if (returnType != null) {
//					returnType.accept(this);
//				} else {
//					// methods really ought to have a return type
//					this.result.append("void");//$NON-NLS-1$
//				}
//			}
//			this.result.append(' ');
//		}
		ASTNode childNode = getChildNode(node, FunctionDeclaration.NAME_PROPERTY);
		if (childNode!=null)
			childNode.accept(this);
		this.result.append('(');
		visitList(node, FunctionDeclaration.PARAMETERS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		int extraDims= getIntAttribute(node, FunctionDeclaration.EXTRA_DIMENSIONS_PROPERTY);
		for (int i = 0; i < extraDims; i++) {
			this.result.append("[]"); //$NON-NLS-1$
		}
		visitList(node, FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY, String.valueOf(','), " throws ", Util.EMPTY_STRING); //$NON-NLS-1$
		ASTNode body= getChildNode(node, FunctionDeclaration.BODY_PROPERTY);
		if (body == null) {
			this.result.append("{}"); //$NON-NLS-1$
		} else {
			body.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FunctionInvocation)
	 */
	public boolean visit(FunctionInvocation node) {
		ASTNode expression= getChildNode(node, FunctionInvocation.EXPRESSION_PROPERTY);
		ASTNode nameNode = getChildNode(node, FunctionInvocation.NAME_PROPERTY);
		if (expression != null) {
			expression.accept(this);
			if (nameNode!=null)
				this.result.append('.');
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			visitList(node, FunctionInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}

		if (nameNode!=null)
			nameNode.accept(this);
		this.result.append('(');
		visitList(node, FunctionInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		this.result.append("null"); //$NON-NLS-1$
		return false;
	}

	public boolean visit(UndefinedLiteral node) {
		this.result.append("undefined"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		this.result.append(getAttribute(node, NumberLiteral.TOKEN_PROPERTY).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		if (node.getAST().apiLevel() >= AST.JLS3) {
			ASTNode javadoc = getChildNode(node, PackageDeclaration.JAVADOC_PROPERTY);
			if (javadoc != null) {
				javadoc.accept(this);
			}
		}
		this.result.append("package "); //$NON-NLS-1$
		getChildNode(node, PackageDeclaration.NAME_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		this.result.append('(');
		getChildNode(node, ParenthesizedExpression.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		getChildNode(node, PostfixExpression.OPERAND_PROPERTY).accept(this);
		this.result.append(getAttribute(node, PostfixExpression.OPERATOR_PROPERTY).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		this.result.append(getAttribute(node, PrefixExpression.OPERATOR_PROPERTY).toString());
		getChildNode(node, PrefixExpression.OPERAND_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		this.result.append(getAttribute(node, PrimitiveType.PRIMITIVE_TYPE_CODE_PROPERTY).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		getChildNode(node, QualifiedName.QUALIFIER_PROPERTY).accept(this);
		this.result.append('.');
		getChildNode(node, QualifiedName.NAME_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		this.result.append("return"); //$NON-NLS-1$
		ASTNode expression= getChildNode(node, ReturnStatement.EXPRESSION_PROPERTY);
		if (expression != null) {
			this.result.append(' ');
			expression.accept(this);
		}
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		this.result.append(getAttribute(node, SimpleName.IDENTIFIER_PROPERTY));
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
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, SingleVariableDeclaration.MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, SingleVariableDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
//		getChildNode(node, SingleVariableDeclaration.TYPE_PROPERTY).accept(this);
//		if (node.getAST().apiLevel() >= AST.JLS3) {
//			if (getBooleanAttribute(node, SingleVariableDeclaration.VARARGS_PROPERTY)) {
//				this.result.append("...");//$NON-NLS-1$
//			}
//		}
		if (node.getParent()!=null && node.getParent().getNodeType()!=ASTNode.FUNCTION_DECLARATION)
			this.result.append("var "); //$NON-NLS-1$
		getChildNode(node, SingleVariableDeclaration.NAME_PROPERTY).accept(this);
		int extraDimensions= getIntAttribute(node, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY);
		for (int i = 0; i < extraDimensions; i++) {
			this.result.append("[]"); //$NON-NLS-1$
		}
		ASTNode initializer= getChildNode(node, SingleVariableDeclaration.INITIALIZER_PROPERTY);
		if (initializer != null) {
			this.result.append('=');
			initializer.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		this.result.append(getAttribute(node, StringLiteral.ESCAPED_VALUE_PROPERTY));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		ASTNode expression= getChildNode(node, SuperConstructorInvocation.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
			this.result.append('.');
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			visitList(node, SuperConstructorInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}
		this.result.append("super("); //$NON-NLS-1$
		visitList(node, SuperConstructorInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		ASTNode qualifier= getChildNode(node, SuperFieldAccess.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("super."); //$NON-NLS-1$
		getChildNode(node, SuperFieldAccess.NAME_PROPERTY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		ASTNode qualifier= getChildNode(node, SuperMethodInvocation.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("super."); //$NON-NLS-1$
		if (node.getAST().apiLevel() >= AST.JLS3) {
			visitList(node, SuperMethodInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}
		getChildNode(node, SuperMethodInvocation.NAME_PROPERTY).accept(this);
		this.result.append('(');
		visitList(node, SuperMethodInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		ASTNode expression= getChildNode(node, SwitchCase.EXPRESSION_PROPERTY);
		if (expression == null) {
			this.result.append("default"); //$NON-NLS-1$
		} else {
			this.result.append("case "); //$NON-NLS-1$
			expression.accept(this);
		}
		this.result.append(':');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		this.result.append("switch ("); //$NON-NLS-1$
		getChildNode(node, SwitchStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		this.result.append('{');
		visitList(node, SwitchStatement.STATEMENTS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		ASTNode qualifier= getChildNode(node, ThisExpression.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("this"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		this.result.append("throw "); //$NON-NLS-1$
		getChildNode(node, ThrowStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		this.result.append("try "); //$NON-NLS-1$
		getChildNode(node, TryStatement.BODY_PROPERTY).accept(this);
		this.result.append(' ');
		visitList(node, TryStatement.CATCH_CLAUSES_PROPERTY, null);
		ASTNode finallyClause= getChildNode(node, TryStatement.FINALLY_PROPERTY);
		if (finallyClause != null) {
			this.result.append(" finally "); //$NON-NLS-1$
			finallyClause.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		int apiLevel= node.getAST().apiLevel();

		ASTNode javadoc= getChildNode(node, TypeDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}

		if (apiLevel == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, TypeDeclaration.MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, TypeDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}

		this.result.append("class "); //$NON-NLS-1$
		getChildNode(node, TypeDeclaration.NAME_PROPERTY).accept(this);

		this.result.append(' ');

		ChildPropertyDescriptor superClassProperty= (apiLevel == JLS2_INTERNAL) ? TypeDeclaration.SUPERCLASS_PROPERTY : TypeDeclaration.SUPERCLASS_TYPE_PROPERTY;
		ASTNode superclass= getChildNode(node, superClassProperty);
		if (superclass != null) {
			this.result.append("extends "); //$NON-NLS-1$
			superclass.accept(this);
			this.result.append(' ');
		}

		this.result.append('{');
		visitList(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			getChildNode(node, TypeDeclarationStatement.TYPE_DECLARATION_PROPERTY).accept(this);
		} else {
			getChildNode(node, TypeDeclarationStatement.DECLARATION_PROPERTY).accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		getChildNode(node, TypeLiteral.TYPE_PROPERTY).accept(this);
		this.result.append(".class"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, VariableDeclarationExpression.MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, VariableDeclarationExpression.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
//		getChildNode(node, VariableDeclarationExpression.TYPE_PROPERTY).accept(this);
		this.result.append("var "); //$NON-NLS-1$
		visitList(node, VariableDeclarationExpression.FRAGMENTS_PROPERTY, String.valueOf(','));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		getChildNode(node, VariableDeclarationFragment.NAME_PROPERTY).accept(this);
		int extraDimensions= getIntAttribute(node, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY);
		for (int i = 0; i < extraDimensions; i++) {
			this.result.append("[]"); //$NON-NLS-1$
		}
		ASTNode initializer= getChildNode(node, VariableDeclarationFragment.INITIALIZER_PROPERTY);
		if (initializer != null) {
			this.result.append('=');
			initializer.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, VariableDeclarationStatement.MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, VariableDeclarationStatement.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
//		getChildNode(node, VariableDeclarationStatement.TYPE_PROPERTY).accept(this);
		this.result.append("var "); //$NON-NLS-1$
		visitList(node, VariableDeclarationStatement.FRAGMENTS_PROPERTY, String.valueOf(','));
		this.result.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		this.result.append("while ("); //$NON-NLS-1$
		getChildNode(node, WhileStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, WhileStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	public boolean visit(WithStatement node) {
		this.result.append("with ("); //$NON-NLS-1$
		getChildNode(node, WithStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, WithStatement.BODY_PROPERTY).accept(this);
		return false;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.BlockComment)
	 */
	public boolean visit(BlockComment node) {
		return false; // cant flatten, needs source
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.LineComment)
	 */
	public boolean visit(LineComment node) {
		return false; // cant flatten, needs source
	}

	public boolean visit(ListExpression node) {
		visitList(node, ArrayInitializer.EXPRESSIONS_PROPERTY, String.valueOf(','));
		return false;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.MemberRef)
	 */
	public boolean visit(MemberRef node) {
		ASTNode qualifier= getChildNode(node, MemberRef.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
		}
		this.result.append('#');
		getChildNode(node, MemberRef.NAME_PROPERTY).accept(this);
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionRef)
	 */
	public boolean visit(FunctionRef node) {
		ASTNode qualifier= getChildNode(node, FunctionRef.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
		}
		this.result.append('#');
		getChildNode(node, FunctionRef.NAME_PROPERTY).accept(this);
		this.result.append('(');
		visitList(node, FunctionRef.PARAMETERS_PROPERTY, ","); //$NON-NLS-1$
		this.result.append(')');
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionRefParameter)
	 */
	public boolean visit(FunctionRefParameter node) {
		getChildNode(node, FunctionRefParameter.TYPE_PROPERTY).accept(this);
		if (node.getAST().apiLevel() >= AST.JLS3) {
			if (getBooleanAttribute(node, FunctionRefParameter.VARARGS_PROPERTY)) {
				this.result.append("..."); //$NON-NLS-1$
			}
		}
		ASTNode name= getChildNode(node, FunctionRefParameter.NAME_PROPERTY);
		if (name != null) {
			this.result.append(' ');
			name.accept(this);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TagElement)
	 */
	public boolean visit(TagElement node) {
		Object tagName= getAttribute(node, TagElement.TAG_NAME_PROPERTY);
		if (tagName != null) {
			this.result.append((String) tagName);
		}
		List list= getChildList(node, TagElement.FRAGMENTS_PROPERTY);
		for (int i= 0; i < list.size(); i++) {
			if (i > 0 || tagName != null) {
				this.result.append(' ');
			}
			ASTNode curr= (ASTNode) list.get(i);
			if (curr instanceof TagElement) {
				this.result.append('{');
				curr.accept(this);
				this.result.append('}');
			} else {
				curr.accept(this);
			}
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.TextElement)
	 */
	public boolean visit(TextElement node) {
		this.result.append(getAttribute(node, TextElement.TEXT_PROPERTY));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(EnhancedForStatement)
	 * @since 3.0
	 */
	public boolean visit(EnhancedForStatement node) {
		this.result.append("for (");//$NON-NLS-1$
		getChildNode(node, EnhancedForStatement.PARAMETER_PROPERTY).accept(this);
		this.result.append(':');
		getChildNode(node, EnhancedForStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, EnhancedForStatement.BODY_PROPERTY).accept(this);
		return false;
	}


	/*
	 * @see ASTVisitor#visit(Modifier)
	 * @since 3.0
	 */
	public boolean visit(Modifier node) {
		this.result.append(getAttribute(node, Modifier.KEYWORD_PROPERTY).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(QualifiedType)
	 * @since 3.0
	 */
	public boolean visit(QualifiedType node) {
		getChildNode(node, QualifiedType.QUALIFIER_PROPERTY).accept(this);
		this.result.append('.');
		getChildNode(node, QualifiedType.NAME_PROPERTY).accept(this);
		return false;
	}
}
