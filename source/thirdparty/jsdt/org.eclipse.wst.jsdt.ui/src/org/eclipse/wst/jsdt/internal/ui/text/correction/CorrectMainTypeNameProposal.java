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
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;

/**
 * Renames the primary type to be compatible with the name of the compilation unit.
 * All constructors and local references to the type are renamed as well.
  */
public class CorrectMainTypeNameProposal extends ASTRewriteCorrectionProposal {

	private final String fOldName;
	private final String fNewName;
	private final IInvocationContext fContext;

	/**
	 * Constructor for CorrectTypeNameProposal.
	 */
	public CorrectMainTypeNameProposal(IJavaScriptUnit cu, IInvocationContext context, String oldTypeName, String newTypeName, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fContext= context;

		setDisplayName(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_renametype_description, newTypeName));
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));

		fOldName= oldTypeName;
		fNewName= newTypeName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		JavaScriptUnit astRoot= fContext.getASTRoot();

		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		AbstractTypeDeclaration decl= findTypeDeclaration(astRoot.types(), fOldName);
		if (decl != null) {
			ASTNode[] sameNodes= LinkedNodeFinder.findByNode(astRoot, decl.getName());
			for (int i= 0; i < sameNodes.length; i++) {
				rewrite.replace(sameNodes[i], ast.newSimpleName(fNewName), null);
			}
		}
		return rewrite;
	}

	private AbstractTypeDeclaration findTypeDeclaration(List types, String name) {
		for (Iterator iter= types.iterator(); iter.hasNext();) {
			AbstractTypeDeclaration decl= (AbstractTypeDeclaration) iter.next();
			if (name.equals(decl.getName().getIdentifier())) {
				return decl;
			}
		}
		return null;
	}

}
