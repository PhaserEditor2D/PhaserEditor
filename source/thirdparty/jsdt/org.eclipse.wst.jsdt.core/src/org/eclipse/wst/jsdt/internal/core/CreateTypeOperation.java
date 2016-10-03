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

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * <p>This operation creates a class or interface.
 *
 * <p>Required Attributes:<ul>
 *  <li>Parent element - must be a compilation unit, or type.
 *  <li>The source code for the type. No verification of the source is
 *      performed.
 * </ul>
 */
public class CreateTypeOperation extends CreateTypeMemberOperation {
/**
 * When executed, this operation will create a type unit
 * in the given parent element (a compilation unit, type)
 */
public CreateTypeOperation(IJavaScriptElement parentElement, String source, boolean force) {
	super(parentElement, source, force);
}
protected ASTNode generateElementAST(ASTRewrite rewriter, IDocument document, IJavaScriptUnit cu) throws JavaScriptModelException {
	ASTNode node = super.generateElementAST(rewriter, document, cu);
	if (!(node instanceof AbstractTypeDeclaration))
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_CONTENTS));
	return node;
}

/**
 * @see CreateElementInCUOperation#generateResultHandle()
 */
protected IJavaScriptElement generateResultHandle() {
	IJavaScriptElement parent= getParentElement();
	switch (parent.getElementType()) {
		case IJavaScriptElement.JAVASCRIPT_UNIT:
			return ((IJavaScriptUnit)parent).getType(getASTNodeName());
		case IJavaScriptElement.TYPE:
			return ((IType)parent).getType(getASTNodeName());
		// Note: creating local/anonymous type is not supported
	}
	return null;
}
/**
 * @see CreateElementInCUOperation#getMainTaskName()
 */
public String getMainTaskName(){
	return Messages.operation_createTypeProgress;
}
/**
 * Returns the <code>IType</code> the member is to be created in.
 */
protected IType getType() {
	IJavaScriptElement parent = getParentElement();
	if (parent.getElementType() == IJavaScriptElement.TYPE) {
		return (IType) parent;
	}
	return null;
}
/**
 * @see CreateTypeMemberOperation#verifyNameCollision
 */
protected IJavaScriptModelStatus verifyNameCollision() {
	IJavaScriptElement parent = getParentElement();
	switch (parent.getElementType()) {
		case IJavaScriptElement.JAVASCRIPT_UNIT:
			String typeName = getASTNodeName();
			if (((IJavaScriptUnit) parent).getType(typeName).exists()) {
				return new JavaModelStatus(
					IJavaScriptModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, typeName));
			}
			break;
		case IJavaScriptElement.TYPE:
			typeName = getASTNodeName();
			if (((IType) parent).getType(typeName).exists()) {
				return new JavaModelStatus(
					IJavaScriptModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, typeName));
			}
			break;
		// Note: creating local/anonymous type is not supported
	}
	return JavaModelStatus.VERIFIED_OK;
}
private String getASTNodeName() {
	return ((AbstractTypeDeclaration) this.createdNode).getName().getIdentifier();
}
protected SimpleName rename(ASTNode node, SimpleName newName) {
	AbstractTypeDeclaration type = (AbstractTypeDeclaration) node;
	SimpleName oldName = type.getName();
	type.setName(newName);
	return oldName;
}
}
