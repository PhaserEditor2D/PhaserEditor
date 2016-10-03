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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;


public abstract class EditorTracker implements IWindowListener, IPageListener, IPartListener {
	
	//--- Window listener
	
	public void windowActivated(IWorkbenchWindow window) {
	}
	public void windowDeactivated(IWorkbenchWindow window) {
	}
	public void windowClosed(IWorkbenchWindow window) {
		window.removePageListener(this);
	}
	public void windowOpened(IWorkbenchWindow window) {
		window.addPageListener(this);
	}
	
	//---- IPageListener
	
	public void pageActivated(IWorkbenchPage page) {
	}
	public void pageClosed(IWorkbenchPage page) {
		page.removePartListener(this);
	}
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(this);
	}
	
	//---- Part Listener
	
	public void partActivated(IWorkbenchPart part) {
	}
	public void partBroughtToTop(IWorkbenchPart part) {
	}
	public void partClosed(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorClosed((IEditorPart)part);
		}
	}
	public void partDeactivated(IWorkbenchPart part) {
	}
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorOpened((IEditorPart)part);
		}
	}
	
	public abstract void editorOpened(IEditorPart part);
	
	public abstract void editorClosed(IEditorPart part);
	
}
