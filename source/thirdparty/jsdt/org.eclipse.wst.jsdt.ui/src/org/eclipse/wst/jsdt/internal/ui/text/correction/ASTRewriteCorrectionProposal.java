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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;

/**
 * A proposal for quick fixes and quick assists that works on a AST rewriter.
 * Either a rewriter is directly passed in the constructor or method {@link #getRewrite()}
 * is overridden to provide the AST rewriter that is evaluated to the document when the
 * proposal is applied.
 * 
 * 
 */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {

	private ASTRewrite fRewrite;
	private ImportRewrite fImportRewrite;

	/**
	 * Constructs a AST rewrite correction proposal.
	 * 
	 * @param name the display name of the proposal.
	 * @param cu the compilation unit that is modified.
	 * @param rewrite the AST rewrite that is invoked when the proposal is applied or
	 *  <code>null</code> if {@link #getRewrite()} is overridden.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public ASTRewriteCorrectionProposal(String name, IJavaScriptUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, relevance, image);
		fRewrite= rewrite;
	}

	/**
	 * Returns the import rewriter used for this compilation unit. <code>
	 */
	public ImportRewrite getImportRewrite() {
		return fImportRewrite;
	}

	/**
	 * Sets the import rewriter used for this compilation unit.
	 */
	public void setImportRewrite(ImportRewrite rewrite) {
		fImportRewrite= rewrite;
	}
	
	/**
	 * Sets the import rewriter used for this compilation unit.
	 */
	public ImportRewrite createImportRewrite(JavaScriptUnit astRoot) {
		fImportRewrite= StubUtility.createImportRewrite(astRoot, true);
		return fImportRewrite;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(org.eclipse.jface.text.IDocument)
	 */
	protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
		super.addEdits(document, editRoot);
		ASTRewrite rewrite= getRewrite();
		if (rewrite != null) {
			try {
				TextEdit edit= rewrite.rewriteAST();
				editRoot.addChild(edit);
			} catch (IllegalArgumentException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		if (fImportRewrite != null) {
			editRoot.addChild(fImportRewrite.rewriteImports(new NullProgressMonitor()));
		}
	}

	/**
	 * Returns the rewriter that has been passed in the constructor. Implementors can override this
	 * method to create the rewriter lazy. This method will only be called once.
	 * 
	 * @return returns the rewriter to be used.
	 * @throws CoreException an exception is thrown when the rewriter could not be created.
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		if (fRewrite == null) {
			IStatus status= JavaUIStatus.createError(IStatus.ERROR, "Rewriter not initialized", null); //$NON-NLS-1$
			throw new CoreException(status);
		}
		return fRewrite;
	}
}
