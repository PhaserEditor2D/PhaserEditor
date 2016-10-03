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
package org.eclipse.wst.jsdt.internal.ui.infoviews;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.ICodeAssist;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;

/**
 * Helper class to convert text selections to Java elements.
 *
 * 
 */
class TextSelectionConverter {

	/** Empty result. */
	private static final IJavaScriptElement[] EMPTY_RESULT= new IJavaScriptElement[0];

	/** Prevent instance creation. */
	private TextSelectionConverter() {
	}

	/**
	 * Finds and returns the Java elements for the given editor selection.
	 *
	 * @param editor the Java editor
	 * @param selection the text selection
	 * @return	the Java elements for the given editor selection
	 * @throws JavaScriptModelException
	 */
	public static IJavaScriptElement[] codeResolve(JavaEditor editor, ITextSelection selection) throws JavaScriptModelException {
		return codeResolve(getInput(editor), selection);
	}

	/**
	 * Finds and returns the Java element that contains the
	 * text selection in the given editor.
	 *
	 * @param editor the Java editor
	 * @param selection the text selection
	 * @return	the Java elements for the given editor selection
	 * @throws JavaScriptModelException
	 */
	public static IJavaScriptElement getElementAtOffset(JavaEditor editor, ITextSelection selection) throws JavaScriptModelException {
		return getElementAtOffset(getInput(editor), selection);
	}

	//-------------------- Helper methods --------------------

	private static IJavaScriptElement getInput(JavaEditor editor) {
		if (editor == null)
			return null;
		IEditorInput input= editor.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
		return manager.getWorkingCopy(input);
	}

	private static IJavaScriptElement[] codeResolve(IJavaScriptElement input, ITextSelection selection) throws JavaScriptModelException {
			if (input instanceof ICodeAssist) {
				if (input instanceof IJavaScriptUnit) {
					IJavaScriptUnit cunit= (IJavaScriptUnit)input;
					if (cunit.isWorkingCopy())
						JavaModelUtil.reconcile(cunit);
				}
				IJavaScriptElement[] elements= ((ICodeAssist)input).codeSelect(selection.getOffset(), selection.getLength());
				if (elements != null && elements.length > 0)
					return elements;
			}
			return EMPTY_RESULT;
	}

	private static IJavaScriptElement getElementAtOffset(IJavaScriptElement input, ITextSelection selection) throws JavaScriptModelException {
		if (input instanceof IJavaScriptUnit) {
			IJavaScriptUnit cunit= (IJavaScriptUnit)input;
			if (cunit.isWorkingCopy())
				JavaModelUtil.reconcile(cunit);
			IJavaScriptElement ref= cunit.getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		} else if (input instanceof IClassFile) {
			IJavaScriptElement ref= ((IClassFile)input).getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		}
		return null;
	}
}
