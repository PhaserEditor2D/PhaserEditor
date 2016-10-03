/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;


/**
 * Proposal info that computes the javadoc lazily when it is queried.
 *
 * 
 */
public final class TypeProposalInfo extends MemberProposalInfo {

	/**
	 * Creates a new proposal info.
	 *
	 * @param project the java project to reference when resolving types
	 * @param proposal the proposal to generate information for
	 */
	public TypeProposalInfo(IJavaScriptProject project, CompletionProposal proposal) {
		super(project, proposal);
	}

	/**
	 * Resolves the member described by the receiver and returns it if found.
	 * Returns <code>null</code> if no corresponding member can be found.
	 *
	 * @return the resolved member or <code>null</code> if none is found
	 * @throws JavaScriptModelException if accessing the java model fails
	 */
	protected IMember resolveMember() throws JavaScriptModelException {
		char[] signature= fProposal.getSignature();
		String typeName= SignatureUtil.stripSignatureToFQN(String.valueOf(signature));
		IType type = fJavaProject.findType(typeName);
		if (type == null)
			type = fJavaProject.findType(new String(fProposal.getCompletion()));
		return type;
	}
}
