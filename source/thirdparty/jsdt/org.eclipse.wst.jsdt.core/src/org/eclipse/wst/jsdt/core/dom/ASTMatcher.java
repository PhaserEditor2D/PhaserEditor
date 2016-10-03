/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import java.util.Iterator;
import java.util.List;

/**
 * Concrete superclass and default implementation of an AST subtree matcher.
 * <p>
 * For example, to compute whether two ASTs subtrees are structurally
 * isomorphic, use <code>n1.subtreeMatch(new ASTMatcher(), n2)</code> where
 * <code>n1</code> and <code>n2</code> are the AST root nodes of the subtrees.
 * </p>
 * <p>
 * For each different concrete AST node type <i>T</i> there is a
 * <code>public boolean match(<i>T</i> node, Object other)</code> method
 * that matches the given node against another object (typically another
 * AST node, although this is not essential). The default implementations
 * provided by this class tests whether the other object is a node of the
 * same type with structurally isomorphic child subtrees. For nodes with
 * list-valued properties, the child nodes within the list are compared in
 * order. For nodes with multiple properties, the child nodes are compared
 * in the order that most closely corresponds to the lexical reading order
 * of the source program. For instance, for a type declaration node, the
 * child ordering is: name, superclass, superinterfaces, and body
 * declarations.
 * </p>
 * <p>
 * Subclasses may override (extend or reimplement) some or all of the
 * <code>match</code> methods in order to define more specialized subtree
 * matchers.
 * </p>
 *
 * @see org.eclipse.wst.jsdt.core.dom.ASTNode#subtreeMatch(ASTMatcher, Object)
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ASTMatcher {

	/**
	 * Indicates whether doc tags should be matched.
	 */
	private boolean matchDocTags;

	/**
	 * Creates a new AST matcher instance.
	 * <p>
	 * For backwards compatibility, the matcher ignores tag
	 * elements below doc comments by default. Use
	 * {@link #ASTMatcher(boolean) ASTMatcher(true)}
	 * for a matcher that compares doc tags by default.
	 * </p>
	 */
	public ASTMatcher() {
		this(false);
	}

	/**
	 * Creates a new AST matcher instance.
	 *
	 * @param matchDocTags <code>true</code> if doc comment tags are
	 * to be compared by default, and <code>false</code> otherwise
	 * @see #match(JSdoc,Object)
	 */
	public ASTMatcher(boolean matchDocTags) {
		this.matchDocTags = matchDocTags;
	}

	/**
	 * Returns whether the given lists of AST nodes match pair wise according
	 * to <code>ASTNode.subtreeMatch</code>.
	 * <p>
	 * Note that this is a convenience method, useful for writing recursive
	 * subtree matchers.
	 * </p>
	 *
	 * @param list1 the first list of AST nodes
	 *    (element type: <code>ASTNode</code>)
	 * @param list2 the second list of AST nodes
	 *    (element type: <code>ASTNode</code>)
	 * @return <code>true</code> if the lists have the same number of elements
	 *    and match pair-wise according to <code>ASTNode.subtreeMatch</code>
	 * @see ASTNode#subtreeMatch(ASTMatcher matcher, Object other)
	 */
	public final boolean safeSubtreeListMatch(List list1, List list2) {
		int size1 = list1.size();
		int size2 = list2.size();
		if (size1 != size2) {
			return false;
		}
		for (Iterator it1 = list1.iterator(), it2 = list2.iterator(); it1.hasNext();) {
			ASTNode n1 = (ASTNode) it1.next();
			ASTNode n2 = (ASTNode) it2.next();
			if (!n1.subtreeMatch(this, n2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether the given nodes match according to
	 * <code>AST.subtreeMatch</code>. Returns <code>false</code> if one or
	 * the other of the nodes are <code>null</code>. Returns <code>true</code>
	 * if both nodes are <code>null</code>.
	 * <p>
	 * Note that this is a convenience method, useful for writing recursive
	 * subtree matchers.
	 * </p>
	 *
	 * @param node1 the first AST node, or <code>null</code>; must be an
	 *    instance of <code>ASTNode</code>
	 * @param node2 the second AST node, or <code>null</code>; must be an
	 *    instance of <code>ASTNode</code>
	 * @return <code>true</code> if the nodes match according
	 *    to <code>AST.subtreeMatch</code> or both are <code>null</code>, and
	 *    <code>false</code> otherwise
	 * @see ASTNode#subtreeMatch(ASTMatcher, Object)
	 */
	public final boolean safeSubtreeMatch(Object node1, Object node2) {
		if (node1 == null && node2 == null) {
			return true;
		}
		if (node1 == null || node2 == null) {
			return false;
		}
		// N.B. call subtreeMatch even node1==node2!=null
		return ((ASTNode) node1).subtreeMatch(this, node2);
	}

	/**
	 * Returns whether the given objects are equal according to
	 * <code>equals</code>. Returns <code>false</code> if either
	 * node is <code>null</code>.
	 *
	 * @param o1 the first object, or <code>null</code>
	 * @param o2 the second object, or <code>null</code>
	 * @return <code>true</code> if the nodes are equal according to
	 *    <code>equals</code> or both <code>null</code>, and
	 *    <code>false</code> otherwise
	 */
	public static boolean safeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}


	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(AnonymousClassDeclaration node, Object other) {
		if (!(other instanceof AnonymousClassDeclaration)) {
			return false;
		}
		AnonymousClassDeclaration o = (AnonymousClassDeclaration) other;
		return safeSubtreeListMatch(node.bodyDeclarations(), o.bodyDeclarations());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayAccess node, Object other) {
		if (!(other instanceof ArrayAccess)) {
			return false;
		}
		ArrayAccess o = (ArrayAccess) other;
		return (
			safeSubtreeMatch(node.getArray(), o.getArray())
				&& safeSubtreeMatch(node.getIndex(), o.getIndex()));
	}

	/**
	 * Returns whether the given node and the other object object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayCreation node, Object other) {
		if (!(other instanceof ArrayCreation)) {
			return false;
		}
		ArrayCreation o = (ArrayCreation) other;
		return (
			safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeListMatch(node.dimensions(), o.dimensions())
				&& safeSubtreeMatch(node.getInitializer(), o.getInitializer()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayInitializer node, Object other) {
		if (!(other instanceof ArrayInitializer)) {
			return false;
		}
		ArrayInitializer o = (ArrayInitializer) other;
		return safeSubtreeListMatch(node.expressions(), o.expressions());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ArrayType node, Object other) {
		if (!(other instanceof ArrayType)) {
			return false;
		}
		ArrayType o = (ArrayType) other;
		return safeSubtreeMatch(node.getComponentType(), o.getComponentType());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Assignment node, Object other) {
		if (!(other instanceof Assignment)) {
			return false;
		}
		Assignment o = (Assignment) other;
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getLeftHandSide(), o.getLeftHandSide())
				&& safeSubtreeMatch(node.getRightHandSide(), o.getRightHandSide()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Block node, Object other) {
		if (!(other instanceof Block)) {
			return false;
		}
		Block o = (Block) other;
		return safeSubtreeListMatch(node.statements(), o.statements());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type. Subclasses may override
	 * this method as needed.
	 * </p>
	 * <p>Note: {@link LineComment} and {@link BlockComment} nodes are
	 * not considered part of main structure of the AST. This method will
	 * only be called if a client goes out of their way to visit this
	 * kind of node explicitly.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(BlockComment node, Object other) {
		if (!(other instanceof BlockComment)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(BooleanLiteral node, Object other) {
		if (!(other instanceof BooleanLiteral)) {
			return false;
		}
		BooleanLiteral o = (BooleanLiteral) other;
		return node.booleanValue() == o.booleanValue();
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(BreakStatement node, Object other) {
		if (!(other instanceof BreakStatement)) {
			return false;
		}
		BreakStatement o = (BreakStatement) other;
		return safeSubtreeMatch(node.getLabel(), o.getLabel());
	}

	public boolean match(FunctionExpression node, Object other) {
		if (!(other instanceof FunctionExpression)) {
			return false;
		}
		FunctionExpression o = (FunctionExpression) other;
		return
			safeSubtreeMatch(node.getMethod(), o.getMethod());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(CatchClause node, Object other) {
		if (!(other instanceof CatchClause)) {
			return false;
		}
		CatchClause o = (CatchClause) other;
		return (
			safeSubtreeMatch(node.getException(), o.getException())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(CharacterLiteral node, Object other) {
		if (!(other instanceof CharacterLiteral)) {
			return false;
		}
		CharacterLiteral o = (CharacterLiteral) other;
		return safeEquals(node.getEscapedValue(), o.getEscapedValue());
	}

	public boolean match(RegularExpressionLiteral node, Object other) {
		if (!(other instanceof RegularExpressionLiteral)) {
			return false;
		}
		RegularExpressionLiteral o = (RegularExpressionLiteral) other;
		return safeEquals(node.getRegularExpression(), o.getRegularExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ClassInstanceCreation node, Object other) {
		if (!(other instanceof ClassInstanceCreation)) {
			return false;
		}
		ClassInstanceCreation o = (ClassInstanceCreation) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (!safeSubtreeMatch(node.internalGetName(), o.internalGetName())) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
			if (!safeSubtreeMatch(node.getType(), o.getType())) {
				return false;
			}
		}
		return
			safeSubtreeMatch(node.getExpression(), o.getExpression())
			&& safeSubtreeMatch(node.getMember(), o.getMember())
				&& safeSubtreeListMatch(node.arguments(), o.arguments())
				&& safeSubtreeMatch(
					node.getAnonymousClassDeclaration(),
					o.getAnonymousClassDeclaration());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(JavaScriptUnit node, Object other) {
		if (!(other instanceof JavaScriptUnit)) {
			return false;
		}
		JavaScriptUnit o = (JavaScriptUnit) other;
		return (
			safeSubtreeMatch(node.getPackage(), o.getPackage())
				&& safeSubtreeListMatch(node.imports(), o.imports())
				&& safeSubtreeListMatch(node.types(), o.types()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ConditionalExpression node, Object other) {
		if (!(other instanceof ConditionalExpression)) {
			return false;
		}
		ConditionalExpression o = (ConditionalExpression) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getThenExpression(), o.getThenExpression())
				&& safeSubtreeMatch(node.getElseExpression(), o.getElseExpression()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ConstructorInvocation node, Object other) {
		if (!(other instanceof ConstructorInvocation)) {
			return false;
		}
		ConstructorInvocation o = (ConstructorInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return safeSubtreeListMatch(node.arguments(), o.arguments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ContinueStatement node, Object other) {
		if (!(other instanceof ContinueStatement)) {
			return false;
		}
		ContinueStatement o = (ContinueStatement) other;
		return safeSubtreeMatch(node.getLabel(), o.getLabel());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(DoStatement node, Object other) {
		if (!(other instanceof DoStatement)) {
			return false;
		}
		DoStatement o = (DoStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(EmptyStatement node, Object other) {
		if (!(other instanceof EmptyStatement)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(EnhancedForStatement node, Object other) {
		if (!(other instanceof EnhancedForStatement)) {
			return false;
		}
		EnhancedForStatement o = (EnhancedForStatement) other;
		return (
			safeSubtreeMatch(node.getParameter(), o.getParameter())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}



	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ExpressionStatement node, Object other) {
		if (!(other instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement o = (ExpressionStatement) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FieldAccess node, Object other) {
		if (!(other instanceof FieldAccess)) {
			return false;
		}
		FieldAccess o = (FieldAccess) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FieldDeclaration node, Object other) {
		if (!(other instanceof FieldDeclaration)) {
			return false;
		}
		FieldDeclaration o = (FieldDeclaration) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return
			safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
			&& safeSubtreeMatch(node.getType(), o.getType())
			&& safeSubtreeListMatch(node.fragments(), o.fragments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ForStatement node, Object other) {
		if (!(other instanceof ForStatement)) {
			return false;
		}
		ForStatement o = (ForStatement) other;
		return (
			safeSubtreeListMatch(node.initializers(), o.initializers())
				&& safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.updaters(), o.updaters())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	public boolean match(ForInStatement node, Object other) {
		if (!(other instanceof ForInStatement)) {
			return false;
		}
		ForInStatement o = (ForInStatement) other;
		return (
			safeSubtreeMatch(node.getIterationVariable(), o.getIterationVariable())
				&& safeSubtreeMatch(node.getCollection(), o.getCollection())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(IfStatement node, Object other) {
		if (!(other instanceof IfStatement)) {
			return false;
		}
		IfStatement o = (IfStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getThenStatement(), o.getThenStatement())
				&& safeSubtreeMatch(node.getElseStatement(), o.getElseStatement()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ImportDeclaration node, Object other) {
		if (!(other instanceof ImportDeclaration)) {
			return false;
		}
		ImportDeclaration o = (ImportDeclaration) other;
		if (node.getAST().apiLevel >= AST.JLS3) {
			if (node.isStatic() != o.isStatic()) {
				return false;
			}
		}
		if (node.isFileImport() != o.isFileImport()) {
			return false;
		}
		return (
			safeSubtreeMatch(node.getName(), o.getName())
				&& node.isOnDemand() == o.isOnDemand());
	}


	public boolean match(InferredType node, Object other) {
		if (!(other instanceof InferredType)) {
			return false;
		}
		InferredType o = (InferredType) other;
		if (node.type==null || o.type==null)
			return true;
		
		return 	node.type.equals(o.type);
	}


	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(InfixExpression node, Object other) {
		if (!(other instanceof InfixExpression)) {
			return false;
		}
		InfixExpression o = (InfixExpression) other;
		// be careful not to trigger lazy creation of extended operand lists
		if (node.hasExtendedOperands() && o.hasExtendedOperands()) {
			if (!safeSubtreeListMatch(node.extendedOperands(), o.extendedOperands())) {
				return false;
			}
		}
		if (node.hasExtendedOperands() != o.hasExtendedOperands()) {
			return false;
		}
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand())
				&& safeSubtreeMatch(node.getRightOperand(), o.getRightOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(InstanceofExpression node, Object other) {
		if (!(other instanceof InstanceofExpression)) {
			return false;
		}
		InstanceofExpression o = (InstanceofExpression) other;
		return (
				safeSubtreeMatch(node.getLeftOperand(), o.getLeftOperand())
				&& safeSubtreeMatch(node.getRightOperand(), o.getRightOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Initializer node, Object other) {
		if (!(other instanceof Initializer)) {
			return false;
		}
		Initializer o = (Initializer) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return (
				safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * Unlike other node types, the behavior of the default
	 * implementation is controlled by a constructor-supplied
	 * parameter  {@link #ASTMatcher(boolean) ASTMatcher(boolean)}
	 * which is <code>false</code> if not specified.
	 * When this parameter is <code>true</code>, the implementation
	 * tests whether the other object is also a <code>Javadoc</code>
	 * with structurally isomorphic child subtrees; the comment string
	 * (<code>Javadoc.getComment()</code>) is ignored.
	 * Conversely, when the parameter is <code>false</code>, the
	 * implementation tests whether the other object is also a
	 * <code>Javadoc</code> with exactly the same comment string;
	 * the tag elements ({@link JSdoc#tags() Javadoc.tags} are
	 * ignored. Subclasses may reimplement.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 * @see #ASTMatcher()
	 * @see #ASTMatcher(boolean)
	 */
	public boolean match(JSdoc node, Object other) {
		if (!(other instanceof JSdoc)) {
			return false;
		}
		JSdoc o = (JSdoc) other;
		if (this.matchDocTags) {
			return safeSubtreeListMatch(node.tags(), o.tags());
		} else {
			return compareDeprecatedComment(node, o);
		}
	}

	/**
	 * Return whether the deprecated comment strings of the given jsdoc are equals.
	 * <p>
	 * Note the only purpose of this method is to hide deprecated warnings.
	 * @deprecated mark deprecated to hide deprecated usage
	 */
	private boolean compareDeprecatedComment(JSdoc first, JSdoc second) {
		if (first.getAST().apiLevel == AST.JLS2_INTERNAL) {
			return safeEquals(first.getComment(), second.getComment());
		} else {
			return true;
		}
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(LabeledStatement node, Object other) {
		if (!(other instanceof LabeledStatement)) {
			return false;
		}
		LabeledStatement o = (LabeledStatement) other;
		return (
			safeSubtreeMatch(node.getLabel(), o.getLabel())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type. Subclasses may override
	 * this method as needed.
	 * </p>
	 * <p>Note: {@link LineComment} and {@link BlockComment} nodes are
	 * not considered part of main structure of the AST. This method will
	 * only be called if a client goes out of their way to visit this
	 * kind of node explicitly.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(LineComment node, Object other) {
		if (!(other instanceof LineComment)) {
			return false;
		}
		return true;
	}

	public boolean match(ListExpression node, Object other) {
		if (!(other instanceof ListExpression)) {
			return false;
		}
		ListExpression o = (ListExpression) other;
		return safeSubtreeListMatch(node.expressions(), o.expressions());
	}


	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(MemberRef node, Object other) {
		if (!(other instanceof MemberRef)) {
			return false;
		}
		MemberRef o = (MemberRef) other;
		return (
				safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}


	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FunctionRef node, Object other) {
		if (!(other instanceof FunctionRef)) {
			return false;
		}
		FunctionRef o = (FunctionRef) other;
		return (
				safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName())
		        && safeSubtreeListMatch(node.parameters(), o.parameters()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FunctionRefParameter node, Object other) {
		if (!(other instanceof FunctionRefParameter)) {
			return false;
		}
		FunctionRefParameter o = (FunctionRefParameter) other;
		int level = node.getAST().apiLevel;
		if (level >= AST.JLS3) {
			if (node.isVarargs() != o.isVarargs()) {
				return false;
			}
		}
		return (
				safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 * <p>
	 * Note that extra array dimensions are compared since they are an
	 * important part of the method declaration.
	 * </p>
	 * <p>
	 * Note that the method return types are compared even for constructor
	 * declarations.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FunctionDeclaration node, Object other) {
		if (!(other instanceof FunctionDeclaration)) {
			return false;
		}
		FunctionDeclaration o = (FunctionDeclaration) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
			if (!safeSubtreeMatch(node.internalGetReturnType(), o.internalGetReturnType())) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
			if (!safeSubtreeMatch(node.getReturnType2(), o.getReturnType2())) {
				return false;
			}
		}
		return ((node.isConstructor() == o.isConstructor())
				&& safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeMatch(node.getName(), o.getName())
				// n.b. compare return type even for constructors
				&& safeSubtreeListMatch(node.parameters(), o.parameters())
	 			&& node.getExtraDimensions() == o.getExtraDimensions()
				&& safeSubtreeListMatch(node.thrownExceptions(), o.thrownExceptions())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(FunctionInvocation node, Object other) {
		if (!(other instanceof FunctionInvocation)) {
			return false;
		}
		FunctionInvocation o = (FunctionInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.arguments(), o.arguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(Modifier node, Object other) {
		if (!(other instanceof Modifier)) {
			return false;
		}
		Modifier o = (Modifier) other;
		return (node.getKeyword() == o.getKeyword());
	}


	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(NullLiteral node, Object other) {
		if (!(other instanceof NullLiteral)) {
			return false;
		}
		return true;
	}

	public boolean match(UndefinedLiteral node, Object other) {
		if (!(other instanceof UndefinedLiteral)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(NumberLiteral node, Object other) {
		if (!(other instanceof NumberLiteral)) {
			return false;
		}
		NumberLiteral o = (NumberLiteral) other;
		return safeEquals(node.getToken(), o.getToken());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PackageDeclaration node, Object other) {
		if (!(other instanceof PackageDeclaration)) {
			return false;
		}
		PackageDeclaration o = (PackageDeclaration) other;
		if (node.getAST().apiLevel >= AST.JLS3) {
			if (!safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())) {
				return false;
			}
			if (!safeSubtreeListMatch(node.annotations(), o.annotations())) {
				return false;
			}
		}
		return safeSubtreeMatch(node.getName(), o.getName());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ParenthesizedExpression node, Object other) {
		if (!(other instanceof ParenthesizedExpression)) {
			return false;
		}
		ParenthesizedExpression o = (ParenthesizedExpression) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PostfixExpression node, Object other) {
		if (!(other instanceof PostfixExpression)) {
			return false;
		}
		PostfixExpression o = (PostfixExpression) other;
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getOperand(), o.getOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PrefixExpression node, Object other) {
		if (!(other instanceof PrefixExpression)) {
			return false;
		}
		PrefixExpression o = (PrefixExpression) other;
		return (
			node.getOperator().equals(o.getOperator())
				&& safeSubtreeMatch(node.getOperand(), o.getOperand()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(PrimitiveType node, Object other) {
		if (!(other instanceof PrimitiveType)) {
			return false;
		}
		PrimitiveType o = (PrimitiveType) other;
		return (node.getPrimitiveTypeCode() == o.getPrimitiveTypeCode());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(QualifiedName node, Object other) {
		if (!(other instanceof QualifiedName)) {
			return false;
		}
		QualifiedName o = (QualifiedName) other;
		return (
			safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(QualifiedType node, Object other) {
		if (!(other instanceof QualifiedType)) {
			return false;
		}
		QualifiedType o = (QualifiedType) other;
		return (
			safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ReturnStatement node, Object other) {
		if (!(other instanceof ReturnStatement)) {
			return false;
		}
		ReturnStatement o = (ReturnStatement) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SimpleName node, Object other) {
		if (!(other instanceof SimpleName)) {
			return false;
		}
		SimpleName o = (SimpleName) other;
		return node.getIdentifier().equals(o.getIdentifier());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SimpleType node, Object other) {
		if (!(other instanceof SimpleType)) {
			return false;
		}
		SimpleType o = (SimpleType) other;
		return safeSubtreeMatch(node.getName(), o.getName());
	}


	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 * <p>
	 * Note that extra array dimensions and the variable arity flag
	 * are compared since they are both important parts of the declaration.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SingleVariableDeclaration node, Object other) {
		if (!(other instanceof SingleVariableDeclaration)) {
			return false;
		}
		SingleVariableDeclaration o = (SingleVariableDeclaration) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
			if (node.isVarargs() != o.isVarargs()) {
				return false;
			}
		}
		return
		    safeSubtreeMatch(node.getType(), o.getType())
				&& safeSubtreeMatch(node.getName(), o.getName())
	 			&& node.getExtraDimensions() == o.getExtraDimensions()
				&& safeSubtreeMatch(node.getInitializer(), o.getInitializer());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(StringLiteral node, Object other) {
		if (!(other instanceof StringLiteral)) {
			return false;
		}
		StringLiteral o = (StringLiteral) other;
		return safeEquals(node.getEscapedValue(), o.getEscapedValue());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SuperConstructorInvocation node, Object other) {
		if (!(other instanceof SuperConstructorInvocation)) {
			return false;
		}
		SuperConstructorInvocation o = (SuperConstructorInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.arguments(), o.arguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SuperFieldAccess node, Object other) {
		if (!(other instanceof SuperFieldAccess)) {
			return false;
		}
		SuperFieldAccess o = (SuperFieldAccess) other;
		return (
			safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeMatch(node.getQualifier(), o.getQualifier()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SuperMethodInvocation node, Object other) {
		if (!(other instanceof SuperMethodInvocation)) {
			return false;
		}
		SuperMethodInvocation o = (SuperMethodInvocation) other;
		if (node.getAST().apiLevel >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
				return false;
			}
		}
		return (
			safeSubtreeMatch(node.getQualifier(), o.getQualifier())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.arguments(), o.arguments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SwitchCase node, Object other) {
		if (!(other instanceof SwitchCase)) {
			return false;
		}
		SwitchCase o = (SwitchCase) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(SwitchStatement node, Object other) {
		if (!(other instanceof SwitchStatement)) {
			return false;
		}
		SwitchStatement o = (SwitchStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeListMatch(node.statements(), o.statements()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TagElement node, Object other) {
		if (!(other instanceof TagElement)) {
			return false;
		}
		TagElement o = (TagElement) other;
		return (
				safeEquals(node.getTagName(), o.getTagName())
				&& safeSubtreeListMatch(node.fragments(), o.fragments()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TextElement node, Object other) {
		if (!(other instanceof TextElement)) {
			return false;
		}
		TextElement o = (TextElement) other;
		return safeEquals(node.getText(), o.getText());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ThisExpression node, Object other) {
		if (!(other instanceof ThisExpression)) {
			return false;
		}
		ThisExpression o = (ThisExpression) other;
		return safeSubtreeMatch(node.getQualifier(), o.getQualifier());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(ThrowStatement node, Object other) {
		if (!(other instanceof ThrowStatement)) {
			return false;
		}
		ThrowStatement o = (ThrowStatement) other;
		return safeSubtreeMatch(node.getExpression(), o.getExpression());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TryStatement node, Object other) {
		if (!(other instanceof TryStatement)) {
			return false;
		}
		TryStatement o = (TryStatement) other;
		return (
			safeSubtreeMatch(node.getBody(), o.getBody())
				&& safeSubtreeListMatch(node.catchClauses(), o.catchClauses())
				&& safeSubtreeMatch(node.getFinally(), o.getFinally()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TypeDeclaration node, Object other) {
		if (!(other instanceof TypeDeclaration)) {
			return false;
		}
		TypeDeclaration o = (TypeDeclaration) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
			if (!safeSubtreeMatch(node.internalGetSuperclass(), o.internalGetSuperclass())) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
			if (!safeSubtreeMatch(node.getSuperclassType(), o.getSuperclassType())) {
				return false;
			}
		}
		return (
				safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
				&& safeSubtreeMatch(node.getName(), o.getName())
				&& safeSubtreeListMatch(node.bodyDeclarations(), o.bodyDeclarations()));
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TypeDeclarationStatement node, Object other) {
		if (!(other instanceof TypeDeclarationStatement)) {
			return false;
		}
		TypeDeclarationStatement o = (TypeDeclarationStatement) other;
		return safeSubtreeMatch(node.getDeclaration(), o.getDeclaration());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(TypeLiteral node, Object other) {
		if (!(other instanceof TypeLiteral)) {
			return false;
		}
		TypeLiteral o = (TypeLiteral) other;
		return safeSubtreeMatch(node.getType(), o.getType());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(VariableDeclarationExpression node, Object other) {
		if (!(other instanceof VariableDeclarationExpression)) {
			return false;
		}
		VariableDeclarationExpression o = (VariableDeclarationExpression) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return safeSubtreeMatch(node.getType(), o.getType())
			&& safeSubtreeListMatch(node.fragments(), o.fragments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 * <p>
	 * Note that extra array dimensions are compared since they are an
	 * important part of the type of the variable.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(VariableDeclarationFragment node, Object other) {
		if (!(other instanceof VariableDeclarationFragment)) {
			return false;
		}
		VariableDeclarationFragment o = (VariableDeclarationFragment) other;
		return safeSubtreeMatch(node.getName(), o.getName())
			&& node.getExtraDimensions() == o.getExtraDimensions()
			&& safeSubtreeMatch(node.getInitializer(), o.getInitializer());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(VariableDeclarationStatement node, Object other) {
		if (!(other instanceof VariableDeclarationStatement)) {
			return false;
		}
		VariableDeclarationStatement o = (VariableDeclarationStatement) other;
		int level = node.getAST().apiLevel;
		if (level == AST.JLS2_INTERNAL) {
			if (node.getModifiers() != o.getModifiers()) {
				return false;
			}
		}
		if (level >= AST.JLS3) {
			if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
				return false;
			}
		}
		return safeSubtreeMatch(node.getType(), o.getType())
			&& safeSubtreeListMatch(node.fragments(), o.fragments());
	}

	/**
	 * Returns whether the given node and the other object match.
	 * <p>
	 * The default implementation provided by this class tests whether the
	 * other object is a node of the same type with structurally isomorphic
	 * child subtrees. Subclasses may override this method as needed.
	 * </p>
	 *
	 * @param node the node
	 * @param other the other object, or <code>null</code>
	 * @return <code>true</code> if the subtree matches, or
	 *   <code>false</code> if they do not match or the other object has a
	 *   different node type or is <code>null</code>
	 */
	public boolean match(WhileStatement node, Object other) {
		if (!(other instanceof WhileStatement)) {
			return false;
		}
		WhileStatement o = (WhileStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	public boolean match(WithStatement node, Object other) {
		if (!(other instanceof WithStatement)) {
			return false;
		}
		WithStatement o = (WithStatement) other;
		return (
			safeSubtreeMatch(node.getExpression(), o.getExpression())
				&& safeSubtreeMatch(node.getBody(), o.getBody()));
	}

	public boolean match(ObjectLiteral node, Object other) {
		if (!(other instanceof ObjectLiteral)) {
			return false;
		}
		ObjectLiteral o = (ObjectLiteral) other;
		return   safeSubtreeListMatch(node.fields(), o.fields());
	}

	public boolean match(ObjectLiteralField node, Object other) {
		if (!(other instanceof ObjectLiteralField)) {
			return false;
		}
		ObjectLiteralField o = (ObjectLiteralField) other;
		return safeSubtreeMatch(node.getFieldName(), o.getFieldName())
		&& safeSubtreeMatch(node.getInitializer(), o.getInitializer());
	}

}
