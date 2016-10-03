/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

/**
 * Descriptor for a simple property of an AST node.
 * A simple property is one whose value is a
 * primitive type (such as <code>int</code> or <code>boolean</code>)
 * or some simple value type (such as <code>String</code> or
 * <code>InfixExpression.Operator</code>).
 *
 * @see org.eclipse.wst.jsdt.core.dom.ASTNode#getStructuralProperty(StructuralPropertyDescriptor)
 * @see org.eclipse.wst.jsdt.core.dom.ASTNode#setStructuralProperty(StructuralPropertyDescriptor, Object)
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class SimplePropertyDescriptor extends StructuralPropertyDescriptor {

	/**
	 * Value type. For example, for a node type like
	 * SingleVariableDeclaration, the modifiers property is int.class
	 */
	private final Class valueType;

	/**
	 * Indicates whether a value is mandatory. A property value is allowed
	 * to be <code>null</code> only if it is not mandatory.
	 */
	private final boolean mandatory;

	/**
	 * Creates a new simple property descriptor with the given property id.
	 * Note that this constructor is declared package-private so that
	 * property descriptors can only be created by the AST
	 * implementation.
	 *
	 * @param nodeClass concrete AST node type that owns this property
	 * @param propertyId the property id
	 * @param valueType the value type of this property
	 * @param mandatory <code>true</code> if the property is mandatory,
	 * and <code>false</code> if it is may be <code>null</code>
	 */
	SimplePropertyDescriptor(Class nodeClass, String propertyId, Class valueType, boolean mandatory) {
		super(nodeClass, propertyId);
		if (valueType == null || ASTNode.class.isAssignableFrom(valueType)) {
			throw new IllegalArgumentException();
		}
		this.valueType = valueType;
		this.mandatory = mandatory;
	}

	/**
	 * Returns the value type of this property.
	 * <p>
	 * For example, for a node type like SingleVariableDeclaration,
	 * the "modifiers" property returns <code>int.class</code>.
	 * </p>
	 *
	 * @return the value type of the property
	 */
	public Class getValueType() {
		return this.valueType;
	}

	/**
	 * Returns whether this property is mandatory. A property value
	 * is not allowed to be <code>null</code> if it is mandatory.
	 *
	 * @return <code>true</code> if the property is mandatory,
	 * and <code>false</code> if it is may be <code>null</code>
	 */
	public boolean isMandatory() {
		return this.mandatory;
	}
}
