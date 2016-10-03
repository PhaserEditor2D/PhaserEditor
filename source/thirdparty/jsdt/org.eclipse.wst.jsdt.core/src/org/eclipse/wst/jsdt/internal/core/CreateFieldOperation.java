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
package org.eclipse.wst.jsdt.internal.core;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * <p>
 * This operation creates a field declaration in a type.
 *
 * <p>
 * Required Attributes:
 * <ul>
 * <li>Containing Type
 * <li>The source code for the declaration. No verification of the source is
 * performed.
 * </ul>
 */
public class CreateFieldOperation extends CreateTypeMemberOperation {
	/**
	 * When executed, this operation will create a field with the given name in
	 * the given type with the specified source.
	 *
	 * <p>
	 * By default the new field is positioned after the last existing field
	 * declaration, or as the first member in the type if there are no field
	 * declarations.
	 */
	public CreateFieldOperation(IJavaScriptElement parentElement, String source,
			boolean force) {
		super(parentElement, source, force);
	}

	protected ASTNode generateElementAST(ASTRewrite rewriter,
			IDocument document, IJavaScriptUnit cu) throws JavaScriptModelException {
		ASTNode node = super.generateElementAST(rewriter, document, cu);
		if (node.getNodeType() != ASTNode.FIELD_DECLARATION)
			throw new JavaScriptModelException(new JavaModelStatus(
					IJavaScriptModelStatusConstants.INVALID_CONTENTS));
		return node;
	}

	/**
	 * @see CreateElementInCUOperation#generateResultHandle
	 */
	protected IJavaScriptElement generateResultHandle() {
		if (getType()!=null)
			return getType().getField(getASTNodeName());
		return getCompilationUnit().getField(getASTNodeName());
	}

	/**
	 * @see CreateElementInCUOperation#getMainTaskName()
	 */
	public String getMainTaskName() {
		return Messages.operation_createFieldProgress;
	}

	private VariableDeclarationFragment getFragment(ASTNode node) {
		Iterator fragments = ((FieldDeclaration) node).fragments().iterator();
		if (this.anchorElement != null) {
			VariableDeclarationFragment fragment = null;
			String fragmentName = this.anchorElement.getElementName();
			while (fragments.hasNext()) {
				fragment = (VariableDeclarationFragment) fragments.next();
				if (fragment.getName().getIdentifier().equals(fragmentName)) {
					return fragment;
				}
			}
			return fragment;
		} else {
			return (VariableDeclarationFragment) fragments.next();
		}
	}

	/**
	 * By default the new field is positioned after the last existing field
	 * declaration, or as the first member in the type if there are no field
	 * declarations.
	 */
	protected void initializeDefaultPosition() {
		IField[] fields = null;
		IParent parentElement;
		try {
			if (getType() != null) {
				parentElement = getType();
				fields = getType().getFields();
			} else {
				parentElement = getCompilationUnit();
				fields = getCompilationUnit().getFields();

			}
			if (fields != null && fields.length > 0) {
				final IField lastField = fields[fields.length - 1];
				// if (parentElement.isEnum()) {
				// IField field = lastField;
				// if (!field.isEnumConstant()) {
				// createAfter(lastField);
				// }
				// } else {
				createAfter(lastField);
				// }
			} else {
				IJavaScriptElement[] elements = parentElement.getChildren();
				if (elements != null && elements.length > 0) {
					createBefore(elements[0]);
				}
			}
		} catch (JavaScriptModelException e) {
			// type doesn't exist: ignore
		}
	}

	/**
	 * @see CreateTypeMemberOperation#verifyNameCollision
	 */
	protected IJavaScriptModelStatus verifyNameCollision() {
		if (this.createdNode != null) {
			IType type = getType();
			String fieldName = getASTNodeName();
			if (type != null) {
				if (type.getField(fieldName).exists()) {
					return new JavaModelStatus(
							IJavaScriptModelStatusConstants.NAME_COLLISION, Messages
									.bind(Messages.status_nameCollision,
											fieldName));
				}
			} else {
				if (getCompilationUnit().getField(fieldName).exists()) {
					return new JavaModelStatus(
							IJavaScriptModelStatusConstants.NAME_COLLISION, Messages
									.bind(Messages.status_nameCollision,
											fieldName));
				}

			}
		}
		return JavaModelStatus.VERIFIED_OK;
	}

	private String getASTNodeName() {
		if (this.alteredName != null)
			return this.alteredName;
		return getFragment(this.createdNode).getName().getIdentifier();
	}

	protected SimpleName rename(ASTNode node, SimpleName newName) {
		VariableDeclarationFragment fragment = getFragment(node);
		SimpleName oldName = fragment.getName();
		fragment.setName(newName);
		return oldName;
	}
}
