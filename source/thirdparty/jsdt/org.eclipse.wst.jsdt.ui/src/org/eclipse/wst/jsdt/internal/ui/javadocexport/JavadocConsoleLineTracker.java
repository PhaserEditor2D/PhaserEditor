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
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class JavadocConsoleLineTracker implements IConsoleLineTracker {
	
	private static class JavadocConsoleHyperLink implements IHyperlink {
		
		private IPath fExternalPath;
		private int fLineNumber;

		public JavadocConsoleHyperLink(IPath externalPath, int lineNumber) {
			fExternalPath= externalPath;
			fLineNumber= lineNumber;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkEntered()
		 */
		public void linkEntered() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkExited()
		 */
		public void linkExited() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkActivated()
		 */
		public void linkActivated() {
			try {
				IFile[] files= ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(fExternalPath);
				if (files.length > 0) {
					for (int i = 0; i < files.length; i++) {
						IFile curr= files[0];
						IJavaScriptElement element= JavaScriptCore.create(curr);
						if (element != null && element.exists()) {
							IEditorPart part= JavaScriptUI.openInEditor(element, true, false);
							if (part instanceof ITextEditor) {
								revealLine((ITextEditor) part, fLineNumber);
							}
							return;
						}
					}
				}	
			} catch (BadLocationException e) {
				JavaScriptPlugin.log(e);
			} catch (PartInitException e) {
				JavaScriptPlugin.log(e);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		
		private void revealLine(ITextEditor editor, int lineNumber) throws BadLocationException {
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			IRegion region= document.getLineInformation(lineNumber - 1);
			editor.selectAndReveal(region.getOffset(), 0);
		}
		
	}
	

	private IConsole fConsole;

	/**
	 * 
	 */
	public JavadocConsoleLineTracker() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
	 */
	public void init(IConsole console) {
		fConsole= console;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
	 */
	public void lineAppended(IRegion line) {
		try {
			int offset = line.getOffset();
			int length = line.getLength();
			String text = fConsole.getDocument().get(offset, length);
			
			int index1= text.indexOf(':');
			if (index1 == -1) {
				return;
			}
			
			int lineNumber= -1;
			IPath path= null;
			int index2= text.indexOf(':', index1 + 1);
			while ((index2 != -1) && (path == null)) {
				if (index1 < index2) {
					try {
						String substr= text.substring(index1 + 1, index2);
						lineNumber= Integer.parseInt(substr);
						path= Path.fromOSString(text.substring(0, index1));
					} catch (NumberFormatException e) {
						// ignore
					}
				}
				index1= index2;
				index2= text.indexOf(':', index1 + 1);
			}
			
			if (lineNumber != -1) {
				JavadocConsoleHyperLink link= new JavadocConsoleHyperLink(path, lineNumber);
				fConsole.addLink(link, line.getOffset(), index1);

			}
		} catch (BadLocationException e) {
			// ignore
		}
	}



	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
	 */
	public void dispose() {
		fConsole = null;
	}

}
