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
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

public class ReplaceCorrectionProposal extends CUCorrectionProposal {

	private String fReplacementString;
	private int fOffset;
	private int fLength;

	public ReplaceCorrectionProposal(String name, IJavaScriptUnit cu, int offset, int length, String replacementString, int relevance) {
		super(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		fReplacementString= replacementString;
		fOffset= offset;
		fLength= length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(org.eclipse.jface.text.IDocument)
	 */
	protected void addEdits(IDocument doc, TextEdit rootEdit) throws CoreException {
		super.addEdits(doc, rootEdit);

		TextEdit edit= new ReplaceEdit(fOffset, fLength, fReplacementString);
		rootEdit.addChild(edit);
	}

}
