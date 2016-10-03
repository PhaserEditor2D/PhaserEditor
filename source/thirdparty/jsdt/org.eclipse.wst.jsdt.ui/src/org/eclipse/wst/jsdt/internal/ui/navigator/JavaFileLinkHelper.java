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

package org.eclipse.wst.jsdt.internal.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class JavaFileLinkHelper implements ILinkHelper {

	public void activateEditor(IWorkbenchPage page, IStructuredSelection selection) {
		if (selection == null || selection.isEmpty())
			return;
		Object element= selection.getFirstElement();
		IEditorPart part= EditorUtility.isOpenInEditor(element);
		if (part != null) {
			page.bringToTop(part);
			if (element instanceof IJavaScriptElement)
				EditorUtility.revealInEditor(part, (IJavaScriptElement) element);
		}

	}

	public IStructuredSelection findSelection(IEditorInput input) {
		IJavaScriptElement element= JavaScriptUI.getEditorInputJavaElement(input);
		if (element == null) {
			IFile file = ResourceUtil.getFile(input);
			if (file != null) {
				element= JavaScriptCore.create(file);
			}
		}
		return (element != null) ? new StructuredSelection(element) : StructuredSelection.EMPTY;
	}

}
