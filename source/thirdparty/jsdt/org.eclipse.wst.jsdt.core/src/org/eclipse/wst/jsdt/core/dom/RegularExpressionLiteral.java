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

import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;

/**
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
*/
public class RegularExpressionLiteral extends Expression {

	/**
	 * The "escapedValue" structural property of this node type.
	 *  
	 */
	public static final SimplePropertyDescriptor REGULAR_EXPRESSION_PROPERTY =
		new SimplePropertyDescriptor(RegularExpressionLiteral.class, "regularExpression", String.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(RegularExpressionLiteral.class, properyList);
		addProperty(REGULAR_EXPRESSION_PROPERTY, properyList);
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
			// includes '/'
	private String regularExpression = "/&/g";//$NON-NLS-1$

	/**
	 * Creates a new unparented character literal node owned by the given AST.
	 * By default, the character literal denotes an unspecified character.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	RegularExpressionLiteral(AST ast) {
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
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == REGULAR_EXPRESSION_PROPERTY) {
			if (get) {
				return getRegularExpression();
			} else {
				setRegularExpression((String) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return REGULAR_EXPRESSION_LITERAL;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		RegularExpressionLiteral result = new RegularExpressionLiteral(target);
		result.setSourceRange(this.getStartPosition(), this.getLength());
		result.setRegularExpression(getRegularExpression());
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
		visitor.visit(this);
		visitor.endVisit(this);
	}

	/**
	 * Returns the string value of this literal node. The value is the sequence
	 * of characters that would appear in the source program, including
	 * enclosing single quotes and embedded escapes.
	 *
	 * @return the escaped string value, including enclosing single quotes
	 *    and embedded escapes
	 */
	public String getRegularExpression() {
		return this.regularExpression;
	}

	/**
	 * Sets the string value of this literal node. The value is the sequence
	 * of characters that would appear in the source program, including
	 * enclosing single quotes and embedded escapes. For example,
	 * <ul>
	 * <li><code>'a'</code> <code>setEscapedValue("\'a\'")</code></li>
	 * <li><code>'\n'</code> <code>setEscapedValue("\'\\n\'")</code></li>
	 * </ul>
	 *
	 * @param value the string value, including enclosing single quotes
	 *    and embedded escapes
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setRegularExpression(String value) {
		// check setInternalEscapedValue(String) if this method is changed
		if (value == null) {
			throw new IllegalArgumentException();
		}
		Scanner scanner = this.ast.scanner;
		char[] source = value.toCharArray();
		scanner.setSource(source);
		scanner.resetTo(0, source.length);
		try {
			int tokenType = scanner.getNextToken();
			switch(tokenType) {
				case TerminalTokens.TokenNameRegExLiteral:
					break;
				default:
					throw new IllegalArgumentException();
			}
		} catch(InvalidInputException e) {
			throw new IllegalArgumentException();
		}
		preValueChange(REGULAR_EXPRESSION_PROPERTY);
		this.regularExpression = value;
		postValueChange(REGULAR_EXPRESSION_PROPERTY);
	}


	/* (omit javadoc for this method)
	 * This method is a copy of setEscapedValue(String) that doesn't do any validation.
	 */
	void internalSetRegularExpression(String value) {
		preValueChange(REGULAR_EXPRESSION_PROPERTY);
		this.regularExpression = value;
		postValueChange(REGULAR_EXPRESSION_PROPERTY);
	}


	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		int size = BASE_NODE_SIZE + 1 * 4 + stringSize(regularExpression);
		return size;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return memSize();
	}
}

