/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java.hover;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.ui.text.java.hover.IJavaEditorTextHover;


public class JavaTypeHover implements IJavaEditorTextHover {

	private IJavaEditorTextHover fProblemHover;
	private IJavaEditorTextHover fJavadocHover;

	public JavaTypeHover() {
		fProblemHover= new ProblemHover();
		fJavadocHover= new JavadocHover();
	}

	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		fProblemHover.setEditor(editor);
		fJavadocHover.setEditor(editor);
	}

	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return fJavadocHover.getHoverRegion(textViewer, offset);
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		String hoverInfo= fProblemHover.getHoverInfo(textViewer, hoverRegion);
		if (hoverInfo != null)
			return hoverInfo;

		return fJavadocHover.getHoverInfo(textViewer, hoverRegion);
	}
}
