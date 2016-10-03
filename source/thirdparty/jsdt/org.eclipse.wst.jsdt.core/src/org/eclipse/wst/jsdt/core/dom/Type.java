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

package org.eclipse.wst.jsdt.core.dom;

/**
 * Abstract base class of all type AST node types. A type node represents a
 * reference to a primitive type (including void), to an array type, or to a
 * simple named type (or type variable), to a qualified type, or to a
 * parameterized type. Note that not all of these
 * are meaningful in all contexts.
 * <p>
 * <pre>
 * Type:
 *    PrimitiveType
 *    ArrayType
 *    SimpleType
 *    QualifiedType
 *    ParameterizedType
 * PrimitiveType:
 *    <b>byte</b>
 *    <b>short</b>
 *    <b>char</b>
 *    <b>int</b>
 *    <b>long</b>
 *    <b>float</b>
 *    <b>double</b>
 *    <b>boolean</b>
 *    <b>void</b>
 * ArrayType:
 *    Type <b>[</b> <b>]</b>
 * SimpleType:
 *    TypeName
 * QualifiedType:
 *    Type <b>.</b> SimpleName
 * </pre>
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class Type extends ASTNode {

	/**
	 * Creates a new AST node for a type owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	Type(AST ast) {
		super(ast);
	}

	/**
	 * Returns whether this type is a primitive type
	 * (<code>PrimitiveType</code>).
	 *
	 * @return <code>true</code> if this is a primitive type, and
	 *    <code>false</code> otherwise
	 */
	public final boolean isPrimitiveType() {
		return (this instanceof PrimitiveType);
	}

	/**
	 * Returns whether this type is a simple type
	 * (<code>SimpleType</code>).
	 *
	 * @return <code>true</code> if this is a simple type, and
	 *    <code>false</code> otherwise
	 */
	public final boolean isSimpleType() {
		return (this instanceof SimpleType);
	}

	/**
	 * Returns whether this type is an array type
	 * (<code>ArrayType</code>).
	 *
	 * @return <code>true</code> if this is an array type, and
	 *    <code>false</code> otherwise
	 */
	public final boolean isArrayType() {
		return (this instanceof ArrayType);
	}

	/**
	 * Returns whether this type is a qualified type
	 * (<code>QualifiedType</code>).
	 * <p>
	 * Note that a type like "A.B" can be represented either of two ways:
	 * <ol>
	 * <li>
	 * <code>QualifiedType(SimpleType(SimpleName("A")),SimpleName("B"))</code>
	 * </li>
	 * <li>
	 * <code>SimpleType(QualifiedName(SimpleName("A"),SimpleName("B")))</code>
	 * </li>
	 * </ol>
	 * The first form is preferred when "A" is known to be a type. However, a
	 * parser cannot always determine this. Clients should be prepared to handle
	 * either rather than make assumptions. (Note also that the first form
	 * became possible as of JLS3; only the second form existed in the
	 * JLS2 API.)
	 * </p>
	 *
	 * @return <code>true</code> if this is a qualified type, and
	 *    <code>false</code> otherwise
	 *  
	 */
	public final boolean isQualifiedType() {
		return (this instanceof QualifiedType);
	}

	/**
	 * Resolves and returns the binding for this type.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the type binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public final ITypeBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveType(this);
	}

	public boolean isInferred()
	{
		return false;
	}

}
