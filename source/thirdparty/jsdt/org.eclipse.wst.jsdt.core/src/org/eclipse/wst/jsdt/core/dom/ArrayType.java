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
 * Type node for an array type.
 * <p>
 * Array types are expressed in a recursive manner, one dimension at a time.
 * </p>
 * <pre>
 * ArrayType:
 *    Type <b>[</b> <b>]</b>
 * </pre>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ArrayType extends Type {

	/**
	 * The "componentType" structural property of this node type.
	 * <p><b>Note: This Field only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 */
	public static final ChildPropertyDescriptor COMPONENT_TYPE_PROPERTY =
		new ChildPropertyDescriptor(ArrayType.class, "componentType", Type.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(ArrayType.class, properyList);
		addProperty(COMPONENT_TYPE_PROPERTY, properyList);
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
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The component type; lazily initialized; defaults to a simple type with
	 * an unspecfied, but legal, name.
	 */
	private Type componentType = null;

	/**
	 * Creates a new unparented node for an array type owned by the given AST.
	 * By default, a 1-dimensional array of an unspecified simple type.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ArrayType(AST ast) {
		super(ast);
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == COMPONENT_TYPE_PROPERTY) {
			if (get) {
				return getComponentType();
			} else {
				setComponentType((Type) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return ARRAY_TYPE;
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		ArrayType result = new ArrayType(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		result.setComponentType((Type) getComponentType().clone(target));
		return result;
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			acceptChild(visitor, getComponentType());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the component type of this array type. The component type
	 * may be another array type.
	 *
	 * @return the component type node
	 */
	public Type getComponentType() {
		if (this.componentType == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.componentType == null) {
					preLazyInit();
					this.componentType = new SimpleType(this.ast);
					postLazyInit(this.componentType, COMPONENT_TYPE_PROPERTY);
				}
			}
		}
		return this.componentType;
	}

	/**
	 * Sets the component type of this array type. The component type
	 * may be another array type.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param componentType the component type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */
	public void setComponentType(Type componentType) {
		if (componentType == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.componentType;
		preReplaceChild(oldChild, componentType, COMPONENT_TYPE_PROPERTY);
		this.componentType = componentType;
		postReplaceChild(oldChild, componentType, COMPONENT_TYPE_PROPERTY);
	}

	/**
	 * Returns the element type of this array type. The element type is
	 * never an array type.
	 * <p>
	 * This is a convenience method that descends a chain of nested array types
	 * until it reaches a non-array type.
	 * </p>
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @return the component type node
	 */
	public Type getElementType() {
		Type t = getComponentType();
		while (t.isArrayType()) {
			t = ((ArrayType) t).getComponentType();
		}
		return t;
	}

	/**
	 * Returns the number of dimensions in this array type.
	 * <p>
	 * This is a convenience method that descends a chain of nested array types
	 * until it reaches a non-array type.
	 * </p>
	 *
	 * @return the number of dimensions (always positive)
	 */
	public int getDimensions() {
		Type t = getComponentType();
		int dimensions = 1; // always include this array type
		while (t.isArrayType()) {
			dimensions++;
			t = ((ArrayType) t).getComponentType();
		}
		return dimensions;
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		return BASE_NODE_SIZE + 1 * 4;
	}

	/* (omit jsdoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return
			memSize()
			+ (this.componentType == null ? 0 : getComponentType().treeSize());
	}
}

