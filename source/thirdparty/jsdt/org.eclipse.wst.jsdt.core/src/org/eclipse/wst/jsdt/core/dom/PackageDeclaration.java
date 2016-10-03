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
 * Package declaration AST node type.
 * For JLS2:
 * <pre>
 * PackageDeclaration:
 *    <b>package</b> Name <b>;</b>
 * </pre>
 * For JLS3, annotations and doc comment
 * were added:
 * <pre>
 * PackageDeclaration:
 *    [ jsdoc ] { Annotation } <b>package</b> Name <b>;</b>
 * </pre>
 * Note that the standard AST parser only recognizes a jsdoc comment
 * immediately preceding the package declaration when it occurs in the
 * special <code>package-info.js</code> javaScript unit (JLS3 7.4.1.1).
 * The jsdoc comment in that file contains the package description.
 * 
 * <p><b>Note: This Class only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class PackageDeclaration extends ASTNode {

	/**
	 * The "javadoc" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor JAVADOC_PROPERTY =
		new ChildPropertyDescriptor(PackageDeclaration.class, "javadoc", JSdoc.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "name" structural property of this node type.
	 *  
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(PackageDeclaration.class, "name", Name.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

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
		List propertyList = new ArrayList(2);
		createPropertyList(PackageDeclaration.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_2_0 = reapPropertyList(propertyList);

		propertyList = new ArrayList(4);
		createPropertyList(PackageDeclaration.class, propertyList);
		addProperty(JAVADOC_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_3_0 = reapPropertyList(propertyList);
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
	 * The doc comment, or <code>null</code> if none.
	 * Defaults to none.
	 *  
	 */
	JSdoc optionalDocComment = null;

	/**
	 * The annotations (element type: <code>Annotation</code>).
	 * Null in JLS2. Added in JLS3; defaults to an empty list
	 * (see constructor).
	 *  
	 */
	private ASTNode.NodeList annotations = null;

	/**
	 * The package name; lazily initialized; defaults to a unspecified,
	 * legal JavaScript package identifier.
	 */
	private Name packageName = null;

	/**
	 * Creates a new AST node for a package declaration owned by the
	 * given AST. The package declaration initially has an unspecified,
	 * but legal, JavaScript identifier; and an empty list of annotations.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	PackageDeclaration(AST ast) {
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
		if (property == JAVADOC_PROPERTY) {
			if (get) {
				return getJavadoc();
			} else {
				setJavadoc((JSdoc) child);
				return null;
			}
		}
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((Name) child);
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
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return PACKAGE_DECLARATION;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		PackageDeclaration result = new PackageDeclaration(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		if (this.ast.apiLevel >= AST.JLS3) {
			result.setJavadoc((JSdoc) ASTNode.copySubtree(target, getJavadoc()));
			result.annotations().addAll(ASTNode.copySubtrees(target, annotations()));
		}
		result.setName((Name) getName().clone(target));
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
			if (this.ast.apiLevel >= AST.JLS3) {
				acceptChild(visitor, getJavadoc());
				acceptChildren(visitor, this.annotations);
			}
			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the live ordered list of annotations of this
	 * package declaration (added in JLS3 API).
	 *
	 * @return the live list of annotations
	 *    (element type: <code>Annotation</code>)
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 *  
	 */
	public List annotations() {
		// more efficient than just calling unsupportedIn2() to check
		if (this.annotations == null) {
			unsupportedIn2();
		}
		return this.annotations;
	}

	/**
	 * Returns the doc comment node.
	 *
	 * @return the doc comment node, or <code>null</code> if none
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 *  
	 */
	public JSdoc getJavadoc() {
		// more efficient than just calling unsupportedIn2() to check
		if (this.annotations == null) {
			unsupportedIn2();
		}
		return this.optionalDocComment;
	}

	/**
	 * Sets or clears the doc comment node.
	 *
	 * @param docComment the doc comment node, or <code>null</code> if none
	 * @exception IllegalArgumentException if the doc comment string is invalid
	 * @exception UnsupportedOperationException if this operation is used in
	 * a JLS2 AST
	 *  
	 */
	public void setJavadoc(JSdoc docComment) {
		// more efficient than just calling unsupportedIn2() to check
		if (this.annotations == null) {
			unsupportedIn2();
		}
		ASTNode oldChild = this.optionalDocComment;
		preReplaceChild(oldChild, docComment, JAVADOC_PROPERTY);
		this.optionalDocComment = docComment;
		postReplaceChild(oldChild, docComment, JAVADOC_PROPERTY);
	}

	/**
	 * Returns the package name of this package declaration.
	 *
	 * @return the package name node
	 */
	public Name getName() {
		if (this.packageName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.packageName == null) {
					preLazyInit();
					this.packageName = new SimpleName(this.ast);
					postLazyInit(this.packageName, NAME_PROPERTY);
				}
			}
		}
		return this.packageName;
	}

	/**
	 * Sets the package name of this package declaration to the given name.
	 *
	 * @param name the new package name
	 * @exception IllegalArgumentException if`:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(Name name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.packageName;
		preReplaceChild(oldChild, name, NAME_PROPERTY);
		this.packageName = name;
		postReplaceChild(oldChild, name, NAME_PROPERTY);
	}

	/**
	 * Resolves and returns the binding for the package declared in this package
	 * declaration.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public IPackageBinding resolveBinding() {
		return this.ast.getBindingResolver().resolvePackage(this);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		return BASE_NODE_SIZE + 3 * 4;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return
			memSize()
			+ (this.optionalDocComment == null ? 0 : getJavadoc().treeSize())
			+ (this.annotations == null ? 0 : this.annotations.listSize())
			+ (this.packageName == null ? 0 : getName().treeSize());
	}
}

