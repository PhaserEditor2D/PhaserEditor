/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
 * Descriptor for a child list property of an AST node.
 * A child list property is one whose value is a list of
 * {@link ASTNode}.
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class ChildListPropertyDescriptor extends StructuralPropertyDescriptor {

	/**
	 * Element type. For example, for a node type like
	 * JavaScriptUnit, the "statements" property is Statement.class.
	 * <p>
	 * Field is private, but marked package-visible for fast
	 * access from ASTNode.
	 * </p>
	 */
	final Class elementType;

	/**
	 * Indicates whether a cycle is possible.
	 * <p>
	 * Field is private, but marked package-visible for fast
	 * access from ASTNode.
	 * </p>
	 */
	final boolean cycleRisk;

	/**
	 * Creates a new child list property descriptor with the given property id.
	 * Note that this constructor is declared package-private so that
	 * property descriptors can only be created by the AST
	 * implementation.
	 *
	 * @param nodeClass concrete AST node type that owns this property
	 * @param propertyId the property id
	 * @param elementType the element type of this property
	 * @param cycleRisk <code>true</code> if this property is at
	 * risk of cycles, and <code>false</code> if there is no worry about cycles
	 */
	ChildListPropertyDescriptor(Class nodeClass, String propertyId, Class elementType, boolean cycleRisk) {
		super(nodeClass, propertyId);
		if (elementType == null) {
			throw new IllegalArgumentException();
		}
		this.elementType = elementType;
		this.cycleRisk = cycleRisk;
	}

	/**
	 * Returns the element type of this list property.
	 * <p>
	 * For example, for a node type like JavaScriptUnit,
	 * the "imports" property returns <code>ImportDeclaration.class</code>.
	 * </p>
	 *
	 * @return the element type of the property
	 */
	public final Class getElementType() {
		return this.elementType;
	}

	/**
	 * Returns whether this property is vulnerable to cycles.
	 * <p>
	 * A property is vulnerable to cycles if a node of the owning
	 * type (that is, the type that owns this property) could legally
	 * appear in the AST subtree below this property. For example,
	 * the body property of a
	 * {@link FunctionDeclaration} node
	 * admits a body which might include statement that embeds
	 * another {@link FunctionDeclaration} node.
	 * On the other hand, the name property of a
	 * FunctionDeclaration node admits only names, and thereby excludes
	 * another FunctionDeclaration node.
	 * </p>
	 *
	 * @return <code>true</code> if cycles are possible,
	 * and <code>false</code> if cycles are impossible
	 */
	public final boolean cycleRisk() {
		return this.cycleRisk;
	}
}
