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
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ObjectLiteralField extends Expression {

	/**
	 * The "type" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor FIELD_NAME_PROPERTY =
		new ChildPropertyDescriptor(ObjectLiteralField.class, "fieldName", Expression.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "expression" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor INITIALIZER_PROPERTY =
		new ChildPropertyDescriptor(ObjectLiteralField.class, "initializer", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(ObjectLiteralField.class, properyList);
		addProperty(FIELD_NAME_PROPERTY, properyList);
		addProperty(INITIALIZER_PROPERTY, properyList);
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
	private Expression fieldName = null;

	/**
	 * The expression; lazily initialized; defaults to a unspecified, but legal,
	 * expression.
	 */
	private Expression initializer = null;

	/**
	 * Creates a new AST node for a cast expression owned by the given
	 * AST. By default, the type and expression are unspecified (but legal).
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ObjectLiteralField(AST ast) {
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
		if (property == INITIALIZER_PROPERTY) {
			if (get) {
				return getInitializer();
			} else {
				setInitializer((Expression) child);
				return null;
			}
		}
		if (property == FIELD_NAME_PROPERTY) {
			if (get) {
				return getFieldName();
			} else {
				setFieldName((Expression) child);
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
		return OBJECT_LITERAL_FIELD;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		ObjectLiteralField result = new ObjectLiteralField(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		result.setFieldName( (Expression) getFieldName().clone(target));
		result.setInitializer((Expression) getInitializer().clone(target));
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
			acceptChild(visitor, getFieldName());
			acceptChild(visitor, getInitializer());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the type in this cast expression.
	 *
	 * @return the type
	 */
	public Expression getFieldName() {
		if (this.fieldName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.fieldName == null) {
					preLazyInit();
					this.fieldName =  new SimpleName(this.ast);
					postLazyInit(this.fieldName, FIELD_NAME_PROPERTY);
				}
			}
		}
		return this.fieldName;
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
	public void setFieldName(Expression name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.fieldName;
		preReplaceChild(oldChild, name, FIELD_NAME_PROPERTY);
		this.fieldName = name;
		postReplaceChild(oldChild, name, FIELD_NAME_PROPERTY);
	}

	/**
	 * Returns the expression of this cast expression.
	 *
	 * @return the expression node
	 */
	public Expression getInitializer() {
		if (this.initializer == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.initializer == null) {
					preLazyInit();
					this.initializer = new SimpleName(this.ast);
					postLazyInit(this.initializer, INITIALIZER_PROPERTY);
				}
			}
		}
		return this.initializer;
	}

	/**
	 * Sets the expression of this cast expression.
	 *
	 * @param expression the new expression node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setInitializer(Expression expression) {
		if (expression == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.initializer;
		preReplaceChild(oldChild, expression, INITIALIZER_PROPERTY);
		this.initializer = expression;
		postReplaceChild(oldChild, expression, INITIALIZER_PROPERTY);
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
			+ (this.fieldName == null ? 0 : getFieldName().treeSize())
			+ (this.initializer == null ? 0 : getInitializer().treeSize());
	}
}
