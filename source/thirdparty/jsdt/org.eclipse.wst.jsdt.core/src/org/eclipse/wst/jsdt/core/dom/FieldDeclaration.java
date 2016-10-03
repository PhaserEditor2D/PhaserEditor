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
 * Field declaration node type.
 * <p>
 * This kind of node collects several variable declaration fragments
 * (<code>VariableDeclarationFragment</code>) into a single body declaration
 * (<code>BodyDeclaration</code>), all sharing the same modifiers and base type.
 * </p>
 * <pre>
 * FieldDeclaration:
 *    [Javadoc] { ExtendedModifier } Type VariableDeclarationFragment
 *         { <b>,</b> VariableDeclarationFragment } <b>;</b>
 * </pre>
 * <p>
 * When a jsdoc comment is present, the source range begins with the first
 * character of the "/**" comment delimiter. When there is no jsdoc comment,
 * the source range begins with the first character of the initial modifier or
 * type. The source range extends through the last character of the final ";".
 * </p>
 * 
 * <p><b>Note: This Class only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class FieldDeclaration extends BodyDeclaration {

	/**
	 * The "javadoc" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
		internalJavadocPropertyFactory(FieldDeclaration.class);

	/**
	 * The "modifiers" structural property of this node type (JLS2 API only).
	 *  
	 */
	public static final SimplePropertyDescriptor MODIFIERS_PROPERTY =
		internalModifiersPropertyFactory(FieldDeclaration.class);

	/**
	 * The "modifiers" structural property of this node type (added in JLS3 API).
	 *  
	 */
	public static final ChildListPropertyDescriptor MODIFIERS2_PROPERTY =
		internalModifiers2PropertyFactory(FieldDeclaration.class);

	/**
	 * The "type" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor TYPE_PROPERTY =
		new ChildPropertyDescriptor(FieldDeclaration.class, "type", Type.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "fragments" structural property of this node type).
	 *  
	 */
	public static final ChildListPropertyDescriptor FRAGMENTS_PROPERTY =
		new ChildListPropertyDescriptor(FieldDeclaration.class, "fragments", VariableDeclarationFragment.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 *  
	 */
	private static final List PROPERTY_DESCRIPTORS_2_0;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 *  
	 */
	private static final List PROPERTY_DESCRIPTORS_3_0;

	static {
		List properyList = new ArrayList(5);
		createPropertyList(FieldDeclaration.class, properyList);
		addProperty(JAVADOC_PROPERTY, properyList);
		addProperty(MODIFIERS_PROPERTY, properyList);
		addProperty(TYPE_PROPERTY, properyList);
		addProperty(FRAGMENTS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_2_0 = reapPropertyList(properyList);

		properyList = new ArrayList(5);
		createPropertyList(FieldDeclaration.class, properyList);
		addProperty(JAVADOC_PROPERTY, properyList);
		addProperty(MODIFIERS2_PROPERTY, properyList);
		addProperty(TYPE_PROPERTY, properyList);
		addProperty(FRAGMENTS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_3_0 = reapPropertyList(properyList);
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
		if (apiLevel == AST.JLS2_INTERNAL) {
			return PROPERTY_DESCRIPTORS_2_0;
		} else {
			return PROPERTY_DESCRIPTORS_3_0;
		}
	}

	/**
	 * The base type; lazily initialized; defaults to an unspecified,
	 * legal type.
	 */
	private Type baseType = null;

	/**
	 * The list of variable declaration fragments (element type:
	 * <code VariableDeclarationFragment</code>).  Defaults to an empty list.
	 */
	private ASTNode.NodeList variableDeclarationFragments =
		new ASTNode.NodeList(FRAGMENTS_PROPERTY);

	/**
	 * Creates a new unparented field declaration statement node owned
	 * by the given AST.  By default, the field declaration has: no modifiers,
	 * an unspecified (but legal) type, and an empty list of variable
	 * declaration fragments (which is syntactically illegal).
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	FieldDeclaration(AST ast) {
		super(ast);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 *  
	 */
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int internalGetSetIntProperty(SimplePropertyDescriptor property, boolean get, int value) {
		if (property == MODIFIERS_PROPERTY) {
			if (get) {
				return getModifiers();
			} else {
				internalSetModifiers(value);
				return 0;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetIntProperty(property, get, value);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == JAVADOC_PROPERTY) {
			if (get) {
				return getJavadoc();
			} else {
				setJavadoc((JSdoc) child);
				return null;
			}
		}
		if (property == TYPE_PROPERTY) {
			if (get) {
				return getType();
			} else {
				setType((Type) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == MODIFIERS2_PROPERTY) {
			return modifiers();
		}
		if (property == FRAGMENTS_PROPERTY) {
			return fragments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	/* (omit javadoc for this method)
	 * Method declared on BodyDeclaration.
	 */
	final ChildPropertyDescriptor internalJavadocProperty() {
		return JAVADOC_PROPERTY;
	}

	/* (omit javadoc for this method)
	 * Method declared on BodyDeclaration.
	 */
	final SimplePropertyDescriptor internalModifiersProperty() {
		return MODIFIERS_PROPERTY;
	}

	/* (omit javadoc for this method)
	 * Method declared on BodyDeclaration.
	 */
	final ChildListPropertyDescriptor internalModifiers2Property() {
		return MODIFIERS2_PROPERTY;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return FIELD_DECLARATION;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		FieldDeclaration result = new FieldDeclaration(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		result.setJavadoc(
			(JSdoc) ASTNode.copySubtree(target, getJavadoc()));
		if (this.ast.apiLevel == AST.JLS2_INTERNAL) {
			result.internalSetModifiers(getModifiers());
		}
		if (this.ast.apiLevel >= AST.JLS3) {
			result.modifiers().addAll(ASTNode.copySubtrees(target, modifiers()));
		}
		result.setType((Type) getType().clone(target));
		result.fragments().addAll(
			ASTNode.copySubtrees(target, fragments()));
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
			acceptChild(visitor, getJavadoc());
			if (this.ast.apiLevel >= AST.JLS3) {
				acceptChildren(visitor, this.modifiers);
			}
			acceptChild(visitor, getType());
			acceptChildren(visitor, this.variableDeclarationFragments);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the base type declared in this field declaration.
	 * <p>
	 * N.B. The individual child variable declaration fragments may specify
	 * additional array dimensions. So the type of the variable are not
	 * necessarily exactly this type.
	 * </p>
	 *
	 * @return the base type
	 */
	public Type getType() {
		if (this.baseType == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.baseType == null) {
					preLazyInit();
					this.baseType = this.ast.newInferredType(null);
					postLazyInit(this.baseType, TYPE_PROPERTY);
				}
			}
		}
		return this.baseType;
	}

	/**
	 * Sets the base type declared in this field declaration to the given type.
	 *
	 * @param type the new base type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setType(Type type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.baseType;
		preReplaceChild(oldChild, type, TYPE_PROPERTY);
		this.baseType = type;
		postReplaceChild(oldChild, type, TYPE_PROPERTY);
	}

	/**
	 * Returns the live list of variable declaration fragments in this field
	 * declaration. Adding and removing nodes from this list affects this node
	 * dynamically. All nodes in this list must be
	 * <code>VariableDeclarationFragment</code>s; attempts to add any other
	 * type of node will trigger an exception.
	 *
	 * @return the live list of variable declaration fragments in this
	 *    statement (element type: <code>VariableDeclarationFragment</code>)
	 */
	public List fragments() {
		return this.variableDeclarationFragments;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		return super.memSize() + 2 * 4;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return
			memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ (this.modifiers == null ? 0 : this.modifiers.listSize())
			+ (this.baseType == null ? 0 : getType().treeSize())
			+ this.variableDeclarationFragments.listSize();
	}
}
