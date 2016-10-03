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
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.util.QualifiedTypeNameHistory;

public class AddImportCorrectionProposal extends ASTRewriteCorrectionProposal {
	
	private final String fTypeName;
	private final String fQualifierName;

	public AddImportCorrectionProposal(String name, IJavaScriptUnit cu, int relevance, Image image, String qualifierName, String typeName, SimpleName node) {
		super(name, cu, ASTRewrite.create(node.getAST()), relevance, image);
		fTypeName= typeName;
		fQualifierName= qualifierName;
	}
	
	public String getQualifiedTypeName() {
		return fQualifierName + '.' + fTypeName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeCorrectionProposal#performChange(org.eclipse.ui.IEditorPart, org.eclipse.jface.text.IDocument)
	 */
	protected void performChange(IEditorPart activeEditor, IDocument document) throws CoreException {
		super.performChange(activeEditor, document);
		rememberSelection();
	}
	

	private void rememberSelection() throws CoreException {
		QualifiedTypeNameHistory.remember(getQualifiedTypeName());
	}

}
