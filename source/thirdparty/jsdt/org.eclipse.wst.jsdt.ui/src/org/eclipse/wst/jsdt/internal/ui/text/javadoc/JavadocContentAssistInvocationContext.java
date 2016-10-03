/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.javadoc;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;


/**
 * 
 * 
 */
public final class JavadocContentAssistInvocationContext extends JavaContentAssistInvocationContext {

	private final int fFlags;
	
	/**
	 * @param viewer
	 * @param offset
	 * @param editor
	 * @param flags see {@link org.eclipse.wst.jsdt.ui.text.java.IJavadocCompletionProcessor#RESTRICT_TO_MATCHING_CASE}
	 */
	public JavadocContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor, int flags) {
		super(viewer, offset, editor);
		fFlags= flags;
	}
	
	/**
	 * Returns the flags for this content assist invocation.
	 * 
	 * @return the flags for this content assist invocation
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavadocCompletionProcessor#RESTRICT_TO_MATCHING_CASE
	 */
	public int getFlags() {
		return fFlags;
	}
	
	/**
	 * Returns the selection length of the viewer.
	 * 
	 * @return the selection length of the viewer
	 */
	public int getSelectionLength() {
		return getViewer().getSelectedRange().y;
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		
		return fFlags == ((JavadocContentAssistInvocationContext) obj).fFlags;
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() << 2 | fFlags;
	}

}
