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
package org.eclipse.wst.jsdt.internal.ui.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

public class JavaUIHelp {

	public static void setHelp(StructuredViewer viewer, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(viewer, contextId);
		viewer.getControl().addHelpListener(listener);
	}

	public static void setHelp(JavaEditor editor, StyledText text, String contextId) {
		JavaUIHelpListener listener= new JavaUIHelpListener(editor, contextId);
		text.addHelpListener(listener);
	}
	
	/**
	 * Creates and returns a help context provider for the given part.
	 * 
	 * @param part the part for which to create the help context provider
	 * @param contextId	the optional context ID used to retrieve static help
	 * @return the help context provider 
	 */
	public static IContextProvider getHelpContextProvider(IWorkbenchPart part, String contextId) {
		IStructuredSelection selection;
		try {
			selection= SelectionConverter.getStructuredSelection(part);
		} catch (JavaScriptModelException ex) {
			JavaScriptPlugin.log(ex);
			selection= StructuredSelection.EMPTY;
		}
		Object[] elements= selection.toArray();
		return new JavaUIHelpContextProvider(contextId, elements);
	}

	private static class JavaUIHelpListener implements HelpListener {

		private StructuredViewer fViewer;
		private String fContextId;
		private JavaEditor fEditor;

		public JavaUIHelpListener(StructuredViewer viewer, String contextId) {
			fViewer= viewer;
			fContextId= contextId;
		}

		public JavaUIHelpListener(JavaEditor editor, String contextId) {
			fContextId= contextId;
			fEditor= editor;
		}

		/*
		 * @see HelpListener#helpRequested(HelpEvent)
		 * 
		 */
		public void helpRequested(HelpEvent e) {
			try {
				Object[] selected= null;
				if (fViewer != null) {
					ISelection selection= fViewer.getSelection();
					if (selection instanceof IStructuredSelection) {
						selected= ((IStructuredSelection)selection).toArray();
					}
				} else if (fEditor != null) {
					IJavaScriptElement input= SelectionConverter.getInput(fEditor);
					if (ActionUtil.isOnBuildPath(input)) {
						selected= SelectionConverter.codeResolve(fEditor);
					}
				}
				JavadocHelpContext.displayHelp(fContextId, selected);
			} catch (CoreException x) {
				JavaScriptPlugin.log(x);
			}
		}
	}

	private static class JavaUIHelpContextProvider implements IContextProvider {
		private String fId;
		private Object[] fSelected;
		public JavaUIHelpContextProvider(String id, Object[] selected) {
			fId= id;
			fSelected= selected;
		}
		public int getContextChangeMask() {
			return SELECTION;
		}
		public IContext getContext(Object target) {
			IContext context= HelpSystem.getContext(fId);
			if (fSelected != null && fSelected.length > 0) {
				try {
					context= new JavadocHelpContext(context, fSelected);
				} catch (JavaScriptModelException e) {
					// since we are updating the UI with async exec it
					// can happen that the element doesn't exist anymore
					// but we are still showing it in the user interface
					if (!e.isDoesNotExist())
						JavaScriptPlugin.log(e);
				}
			}
			return context;
		}
		public String getSearchExpression(Object target) {
			return null;
		}
	}
}
