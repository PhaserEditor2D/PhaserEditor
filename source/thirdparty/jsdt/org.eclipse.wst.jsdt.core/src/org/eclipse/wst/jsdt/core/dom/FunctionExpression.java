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

package org.eclipse.wst.jsdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Cast expression AST node type.
 *
 * <pre>
 * CastExpression:
 *    <b>(</b> Type <b>)</b> Expression
 * </pre>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class FunctionExpression extends Expression {

	/**
	 * The "type" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor METHOD_PROPERTY =
		new ChildPropertyDescriptor(FunctionExpression.class, "method", FunctionDeclaration.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$


	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(FunctionExpression.class, properyList);
		addProperty(METHOD_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants
	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 *  
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The type; lazily initialized; defaults to a unspecified,
	 * legal type.
	 */
	private FunctionDeclaration methodDeclaration = null;


	/**
	 * Creates a new AST node for a cast expression owned by the given
	 * AST. By default, the type and expression are unspecified (but legal).
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	FunctionExpression(AST ast) {
		super(ast);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == METHOD_PROPERTY) {
			if (get) {
				return getMethod();
			} else {
				setMethod((FunctionDeclaration) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return FUNCTION_EXPRESSION;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		FunctionExpression result = new FunctionExpression(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		result.setMethod((FunctionDeclaration) getMethod().clone(target));
		return result;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChild(visitor, getMethod());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the type in this cast expression.
	 *
	 * @return the type
	 */
	public FunctionDeclaration getMethod() {
		if (this.methodDeclaration == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.methodDeclaration == null) {
					preLazyInit();
					this.methodDeclaration = this.ast.newFunctionDeclaration();
					postLazyInit(this.methodDeclaration, METHOD_PROPERTY);
				}
			}
		}
		return this.methodDeclaration;
	}

	/**
	 * Sets the type in this cast expression to the given type.
	 *
	 * @param type the new type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setMethod(FunctionDeclaration method) {
		if (method == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.methodDeclaration;
		preReplaceChild(oldChild, method, METHOD_PROPERTY);
		this.methodDeclaration = method;
		postReplaceChild(oldChild, method, METHOD_PROPERTY);
	}




	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 2 * 4;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return
			memSize()
			+ (this.methodDeclaration == null ? 0 : getMethod().treeSize());
	}
}
