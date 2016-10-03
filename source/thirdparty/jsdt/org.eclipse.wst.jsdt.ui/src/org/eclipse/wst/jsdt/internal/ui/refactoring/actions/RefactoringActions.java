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
package org.eclipse.wst.jsdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;

/**
 * Helper class for refactoring actions
 */
public class RefactoringActions {

	/**
	 * Converts the given selection into a type using the following rules:
	 * <ul>
	 *   <li>if the selection is enclosed by a type than that type is returned.</li>
	 *   <li>if the selection is inside a compilation unit or class file than the 
	 *       primary type is returned.</li>
	 *   <li>otherwise <code>null</code> is returned.
	 * </ul>
	 */
	public static IType getEnclosingOrPrimaryType(JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement element= selection.resolveEnclosingElement();
		if (element != null)
			return convertToEnclosingOrPrimaryType(element);
		return null;
	}
	public static IType getEnclosingOrPrimaryType(JavaEditor editor) throws JavaScriptModelException {
		return convertToEnclosingOrPrimaryType(SelectionConverter.resolveEnclosingElement(
			editor, (ITextSelection)editor.getSelectionProvider().getSelection()));
	}

	private static IType convertToEnclosingOrPrimaryType(IJavaScriptElement element) throws JavaScriptModelException {
		if (element instanceof IType)
			return (IType)element;
		IType result= (IType)element.getAncestor(IJavaScriptElement.TYPE);
		if (result != null)
			return result;
		if (element instanceof IJavaScriptUnit)
			return ((IJavaScriptUnit)element).findPrimaryType();
		if (element instanceof IClassFile) 
			return ((IClassFile)element).getType();
		return null;
	}
	
	/**
	 * Converts the given selection into a type using the following rules:
	 * <ul>
	 *   <li>if the selection is enclosed by a type than that type is returned.</li>
	 *   <li>otherwise <code>null</code> is returned.
	 * </ul>
	 */
	public static IType getEnclosingType(JavaTextSelection selection) throws JavaScriptModelException {
		return convertToEnclosingType(selection.resolveEnclosingElement());
	}
	public static IType getEnclosingType(JavaEditor editor) throws JavaScriptModelException {
		return convertToEnclosingType(SelectionConverter.resolveEnclosingElement(
			editor, (ITextSelection)editor.getSelectionProvider().getSelection()));
	}
	
	private static IType convertToEnclosingType(IJavaScriptElement element) {
		if (element == null)
			return null;
		if (! (element instanceof IType))
			element= element.getAncestor(IJavaScriptElement.TYPE);
		return (IType)element;
	}
}
