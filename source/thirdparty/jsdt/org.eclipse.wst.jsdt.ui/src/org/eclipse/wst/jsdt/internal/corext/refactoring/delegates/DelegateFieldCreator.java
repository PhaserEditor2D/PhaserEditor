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
package org.eclipse.wst.jsdt.internal.corext.refactoring.delegates;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Delegate creator for static fields. Note that this implementation assumes a
 * field <strong>with only one fragment</strong>. See
 * {@link org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor#getASTMembers(org.eclipse.ltk.core.refactoring.RefactoringStatus)}
 * for more information.
 * 
 * 
 */
public class DelegateFieldCreator extends DelegateCreator {

	private VariableDeclarationFragment fOldFieldFragment;

	protected void initialize() {
		
		Assert.isTrue(getDeclaration() instanceof FieldDeclaration);
		Assert.isTrue(((FieldDeclaration) getDeclaration()).fragments().size() == 1);
		
		fOldFieldFragment= (VariableDeclarationFragment) ((FieldDeclaration) getDeclaration()).fragments().get(0);
		if (getNewElementName() == null)
			setNewElementName(fOldFieldFragment.getName().getIdentifier());
		
		setInsertBefore(false); // delegate must be inserted after the original field that is referenced in the initializer
	}

	protected ASTNode createBody(BodyDeclaration fd) throws JavaScriptModelException {
		FieldDeclaration result= (FieldDeclaration) fd;
		Expression initializer= createDelegateFieldInitializer(result);
		return initializer;
	}

	protected ASTNode createDocReference(BodyDeclaration declaration) {
		MemberRef ref= getAst().newMemberRef();
		ref.setName(getAst().newSimpleName(getNewElementName()));

		if (isMoveToAnotherFile())
			ref.setQualifier(createDestinationTypeName());
		return ref;
	}
	
	protected ASTNode getBodyHead(BodyDeclaration result) {
		return fOldFieldFragment;
	}
	
	protected ChildPropertyDescriptor getJavaDocProperty() {
		return FieldDeclaration.JAVADOC_PROPERTY;
	}

	protected ChildPropertyDescriptor getBodyProperty() {
		return VariableDeclarationFragment.INITIALIZER_PROPERTY;
	}

	protected IBinding getDeclarationBinding() {
		return fOldFieldFragment.resolveBinding();
	}

	protected String getTextEditGroupLabel() {
		return RefactoringCoreMessages.DelegateFieldCreator_text_edit_group_label;
	}

	// ******************* INTERNAL HELPERS ***************************

	private Expression createDelegateFieldInitializer(final FieldDeclaration declaration) throws JavaScriptModelException {
		Assert.isNotNull(declaration);

		Expression qualification= getAccess();
		if (qualification != null) {
			FieldAccess access= getAst().newFieldAccess();
			access.setExpression(qualification);
			access.setName(getAst().newSimpleName(getNewElementName()));
			return access;
		} else {
			SimpleName access= getAst().newSimpleName(getNewElementName());
			return access;
		}
	}
}
