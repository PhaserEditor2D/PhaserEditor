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

import java.util.ArrayList;
import java.util.List;

/**
 * AST node for a parameter within a method reference ({@link FunctionRef}).
 * These nodes only occur within doc comments ({@link JSdoc}).
 * For JLS2:
 * <pre>
 * FunctionRefParameter:
 * 		Type [ Identifier ]
 * </pre>
 * For JLS3, the variable arity indicator was added:
 * <pre>
 * FunctionRefParameter:
 * 		Type [ <b>...</b> ] [ Identifier ]
 * </pre>
 * <p>
 * Note: The 1.5 spec for the jsdoc tool does not mention the possibility
 * of a variable arity indicator in method references. However, the 1.5
 * jsdoc tool itself does indeed support it. Since it makes sense to have
 * a way to explicitly refer to variable arity methods, it seems more likely
 * that the jsdoc spec is wrong in this case.
 * </p>
 *
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class FunctionRefParameter extends ASTNode {

	/**
	 * The "type" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor TYPE_PROPERTY =
		new ChildPropertyDescriptor(FunctionRefParameter.class, "type", Type.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "varargs" structural property of this node type (added in JLS3 API).
	 *  
	 */
	public static final SimplePropertyDescriptor VARARGS_PROPERTY =
		new SimplePropertyDescriptor(FunctionRefParameter.class, "varargs", boolean.class, MANDATORY); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(FunctionRefParameter.class, "name", SimpleName.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

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
		List properyList = new ArrayList(3);
		createPropertyList(FunctionRefParameter.class, properyList);
		addProperty(TYPE_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_2_0 = reapPropertyList(properyList);

		properyList = new ArrayList(3);
		createPropertyList(FunctionRefParameter.class, properyList);
		addProperty(TYPE_PROPERTY, properyList);
		addProperty(VARARGS_PROPERTY, properyList);
		addProperty(NAME_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_3_0 = reapPropertyList(properyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the AST.JLS* constants
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
	 * The type; lazily initialized; defaults to a unspecified,
	 * legal type.
	 */
	private Type type = null;

	/**
	 * Indicates the last parameter of a variable arity method;
	 * defaults to false.
	 *
	 *  
	 */
	private boolean variableArity = false;

	/**
	 * The parameter name, or <code>null</code> if none; none by
	 * default.
	 */
	private SimpleName optionalParameterName = null;

	/**
	 * Creates a new AST node for a method referenece parameter owned by the given
	 * AST. By default, the node has an unspecified (but legal) type,
	 * not variable arity, and no parameter name.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	FunctionRefParameter(AST ast) {
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
		if (property == TYPE_PROPERTY) {
			if (get) {
				return getType();
			} else {
				setType((Type) child);
				return null;
			}
		}
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((SimpleName) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final boolean internalGetSetBooleanProperty(SimplePropertyDescriptor property, boolean get, boolean value) {
		if (property == VARARGS_PROPERTY) {
			if (get) {
				return isVarargs();
			} else {
				setVarargs(value);
				return false;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetBooleanProperty(property, get, value);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return FUNCTION_REF_PARAMETER;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		FunctionRefParameter result = new FunctionRefParameter(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		result.setType((Type) ASTNode.copySubtree(target, getType()));
		if (this.ast.apiLevel >= AST.JLS3) {
			result.setVarargs(isVarargs());
		}
		result.setName((SimpleName) ASTNode.copySubtree(target, getName()));
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
			acceptChild(visitor, getType());
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the paramter type.
	 *
	 * @return the parameter type
	 */
	public Type getType() {
		if (this.type == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.type == null) {
					preLazyInit();
					this.type = this.ast.newInferredType(null);
					postLazyInit(this.type, TYPE_PROPERTY);
				}
			}
		}
		return this.type;
	}

	/**
	 * Sets the paramter type to the given type.
	 *
	 * @param type the new type
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the type is <code>null</code></li>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setType(Type type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.type;
		preReplaceChild(oldChild, type, TYPE_PROPERTY);
		this.type = type;
		postReplaceChild(oldChild, type, TYPE_PROPERTY);
	}

	/**
	 * Returns whether this method reference parameter is for
	 * the last parameter of a variable arity method (added in JLS3 API).
	 * <p>
	 * Note that the binding for the type <code>Foo</code>in the vararg method
	 * reference <code>#fun(Foo...)</code> is always for the type as
	 * written; i.e., the type binding for <code>Foo</code>. However, if you
	 * navigate from the FunctionRef to its method binding to the
	 * type binding for its last parameter, the type binding for the vararg
	 * parameter is always an array type (i.e., <code>Foo[]</code>) reflecting
	 * the way vararg methods get compiled.
	 * </p>
	 *
	 * @return <code>true</code> if this is a variable arity parameter,
	 *    and <code>false</code> otherwise
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 *  
	 */
	public boolean isVarargs() {
		unsupportedIn2();
		return this.variableArity;
	}

	/**
	 * Sets whether this method reference parameter is for the last parameter of
	 * a variable arity method (added in JLS3 API).
	 *
	 * @param variableArity <code>true</code> if this is a variable arity
	 *    parameter, and <code>false</code> otherwise
	 *  
	 */
	public void setVarargs(boolean variableArity) {
		unsupportedIn2();
		preValueChange(VARARGS_PROPERTY);
		this.variableArity = variableArity;
		postValueChange(VARARGS_PROPERTY);
	}

	/**
	 * Returns the parameter name, or <code>null</code> if there is none.
	 *
	 * @return the parameter name node, or <code>null</code> if there is none
	 */
	public SimpleName getName() {
		return this.optionalParameterName;
	}

	/**
	 * Sets or clears the parameter name.
	 *
	 * @param name the parameter name node, or <code>null</code> if
	 *    there is none
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName name) {
		ASTNode oldChild = this.optionalParameterName;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.optionalParameterName = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		return BASE_NODE_SIZE + 2 * 5;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return
			memSize()
			+ (this.type == null ? 0 : getType().treeSize())
			+ (this.optionalParameterName == null ? 0 : getName().treeSize());
	}
}
