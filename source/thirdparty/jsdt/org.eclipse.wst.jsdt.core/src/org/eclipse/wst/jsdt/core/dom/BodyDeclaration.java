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

import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class of all AST nodes that represent body declarations
 * that may appear in the body of some kind of class or interface declaration,
 * including anonymous class declarations.
 * <p>
 * <pre>
 * BodyDeclaration:
 *		ClassDeclaration
 *		InterfaceDeclaration
 *		FunctionDeclaration
 * 		ConstructorDeclaration
 * 		FieldDeclaration
 * 		Initializer
 * </pre>
 * </p>
 * <p>
 * All types of body declarations carry modifiers, although they differ in
 * which modifiers are allowed. Most types of body declarations can carry a
 * doc comment; Initializer is the only ones that does not. The source range
 * for body declarations always includes the doc comment if present.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class BodyDeclaration extends ProgramElement {

	/**
	 * The doc comment, or <code>null</code> if none.
	 * Defaults to none.
	 */
	JSdoc optionalDocComment = null;

	/**
	 * The modifier flags; bit-wise or of Modifier flags.
	 * Defaults to none. Not used in 3.0.
	 */
	private int modifierFlags = Modifier.NONE;

	/**
	 * The extended modifiers (element type: <code>IExtendedModifier</code>).
	 *  defaults to an empty list
	 * (see constructor).
	 *
	 */
	ASTNode.NodeList modifiers = null;

	/**
	 * Returns structural property descriptor for the "modifiers" property
	 * of this node.
	 *
	 * @return the property descriptor
	 */
	abstract SimplePropertyDescriptor internalModifiersProperty();

	/**
	 * Returns structural property descriptor for the "modifiers" property
	 * of this node.
	 *
	 * @return the property descriptor
	 */
	abstract ChildListPropertyDescriptor internalModifiers2Property();

	/**
	 * Returns structural property descriptor for the "modifiers" property
	 * of this node.
	 *
	 * @return the property descriptor
	 */
	public final ChildListPropertyDescriptor getModifiersProperty() {
		// important: return property for AST.JLS3
		return internalModifiers2Property();
	}

	/**
	 * Returns structural property descriptor for the "jsdoc" property
	 * of this node.
	 *
	 * @return the property descriptor
	 */
	abstract ChildPropertyDescriptor internalJavadocProperty();

	/**
	 * Returns structural property descriptor for the "jsdoc" property
	 * of this node.
	 *
	 * @return the property descriptor
	 */
	public final ChildPropertyDescriptor getJavadocProperty() {
		return internalJavadocProperty();
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "jsdoc" property declared on the given concrete node type.
	 *
	 * @return the property descriptor
	 */
	static final ChildPropertyDescriptor internalJavadocPropertyFactory(Class nodeClass) {
		return new ChildPropertyDescriptor(nodeClass, "javadoc", JSdoc.class, OPTIONAL, NO_CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "modifiers" property declared on the given concrete node type.
	 *
	 * @return the property descriptor
	 */
	static final SimplePropertyDescriptor internalModifiersPropertyFactory(Class nodeClass) {
		return new SimplePropertyDescriptor(nodeClass, "modifiers", int.class, MANDATORY); //$NON-NLS-1$
	}

	/**
	 * Creates and returns a structural property descriptor for the
	 * "modifiers" property declared on the given concrete node type.
	 *
	 * @return the property descriptor
	 */
	static final ChildListPropertyDescriptor internalModifiers2PropertyFactory(Class nodeClass) {
		return new ChildListPropertyDescriptor(nodeClass, "modifiers", IExtendedModifier.class, CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Creates a new AST node for a body declaration node owned by the
	 * given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	BodyDeclaration(AST ast) {
		super(ast);
		if (ast.apiLevel >= AST.JLS3) {
			this.modifiers = new ASTNode.NodeList(internalModifiers2Property());
		}
	}

	/**
	 * Returns the doc comment node.
	 *
	 * @return the doc comment node, or <code>null</code> if none
	 */
	public JSdoc getJavadoc() {
		return this.optionalDocComment;
	}

	/**
	 * Sets or clears the doc comment node.
	 *
	 * @param docComment the doc comment node, or <code>null</code> if none
	 * @exception IllegalArgumentException if the doc comment string is invalid
	 */
	public void setJavadoc(JSdoc docComment) {
		ChildPropertyDescriptor p = internalJavadocProperty();
		ASTNode oldChild = this.optionalDocComment;
		preReplaceChild(oldChild, docComment, p);
		this.optionalDocComment = docComment;
		postReplaceChild(oldChild, docComment, p);
	}

	/**
	 * Returns the modifiers explicitly specified on this declaration.
	 *
	 * @return the bit-wise or of <code>Modifier</code> constants
	 * @see Modifier
	 */
	public int getModifiers() {
		// more efficient than checking getAST().API_LEVEL
		if (this.modifiers == null) {
			// JLS2 behavior - bona fide property
			return this.modifierFlags;
		} else {
			// JLS3 behavior - convenience method
			// performance could be improved by caching computed flags
			// but this would require tracking changes to this.modifiers
			int computedmodifierFlags = Modifier.NONE;
			for (Iterator it = modifiers().iterator(); it.hasNext(); ) {
				Object x = it.next();
				if (x instanceof Modifier) {
					computedmodifierFlags |= ((Modifier) x).getKeyword().toFlagValue();
				}
			}
			return computedmodifierFlags;
		}
	}

	/**
	 * Sets the modifiers explicitly specified on this declaration (JLS2 API only).
	 *
	 * @param modifiers the given modifiers (bit-wise or of <code>Modifier</code> constants)
	 * @see Modifier
	 * @deprecated Rhis method is replaced by
	 * {@link #modifiers()} which contains a list of a <code>Modifier</code> nodes.
	 */
	public void setModifiers(int modifiers) {
		internalSetModifiers(modifiers);
	}

	/**
	 * Internal synonym for deprecated method. Used to avoid
	 * deprecation warnings.
	 */
	/*package*/ final void internalSetModifiers(int pmodifiers) {
		// more efficient than just calling supportedOnlyIn2() to check
		if (this.modifiers != null) {
			supportedOnlyIn2();
		}
		SimplePropertyDescriptor p = internalModifiersProperty();
		preValueChange(p);
		this.modifierFlags = pmodifiers;
		postValueChange(p);
	}

	/**
	 * Returns the live ordered list of modifiers
	 * of this declaration .
	 *
	 * @return the live list of modifiers
	 *    (element type: <code>IExtendedModifier</code>)
	 */
	public List modifiers() {
		// more efficient than just calling unsupportedIn2() to check
		if (this.modifiers == null) {
			unsupportedIn2();
		}
		return this.modifiers;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		return BASE_NODE_SIZE + 3 * 4;
	}
}

