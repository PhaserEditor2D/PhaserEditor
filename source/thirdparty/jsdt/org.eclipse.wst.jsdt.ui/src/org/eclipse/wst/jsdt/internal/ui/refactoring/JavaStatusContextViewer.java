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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.TextStatusContextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.InternalClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;


public class JavaStatusContextViewer extends TextStatusContextViewer {

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.IStatusContextViewer#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		final SourceViewer viewer= getSourceViewer();
		viewer.unconfigure();
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		viewer.configure(new JavaScriptSourceViewerConfiguration(JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		viewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
	}
	
	protected SourceViewer createSourceViewer(Composite parent) {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		return new JavaSourceViewer(parent, null, null, false, SWT.LEFT_TO_RIGHT | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, store);
	}

	private IPackageFragmentRoot getPackageFragmentRoot(IClassFile file) {

		IJavaScriptElement element= file.getParent();
		while (element != null && element.getElementType() != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
			element= element.getParent();

		return (IPackageFragmentRoot) element;
	}

	public void setInput(RefactoringStatusContext context) {
		if (context instanceof JavaStatusContext) {
			JavaStatusContext jsc= (JavaStatusContext)context;
			IDocument document= null;
			if (jsc.isBinary()) {
				IClassFile file= jsc.getClassFile();
				IEditorInput editorInput= new InternalClassFileEditorInput(file);
				document= getDocument(JavaScriptPlugin.getDefault().getClassFileDocumentProvider(), editorInput);
				if (document.getLength() == 0)
					document= new Document(Messages.format(RefactoringMessages.JavaStatusContextViewer_no_source_found0, getPackageFragmentRoot(file).getElementName()));
				updateTitle(file);
			} else {
				IJavaScriptUnit cunit= jsc.getCompilationUnit();
				if (cunit.isWorkingCopy()) {
					try {
						document= newJavaDocument(cunit.getSource());
					} catch (JavaScriptModelException e) {
						// document is null which is a valid input.
					}
				} else {
					IEditorInput editorInput= new FileEditorInput((IFile)cunit.getResource());
					document= getDocument(JavaScriptPlugin.getDefault().getCompilationUnitDocumentProvider(), editorInput);
				}
				if (document == null)
					document= new Document(RefactoringMessages.JavaStatusContextViewer_no_source_available);
				updateTitle(cunit);
			}
			setInput(document, createRegion(jsc.getSourceRange()));
		} else if (context instanceof JavaStringStatusContext) {
			updateTitle(null);
			JavaStringStatusContext sc= (JavaStringStatusContext)context;
			setInput(newJavaDocument(sc.getSource()), createRegion(sc.getSourceRange()));
		}
	}
	
	private IDocument newJavaDocument(String source) {
		IDocument result= new Document(source);
		JavaScriptTextTools textTools= JavaScriptPlugin.getDefault().getJavaTextTools();
		textTools.setupJavaDocumentPartitioner(result);
		return result;
	}
	
	private static IRegion createRegion(ISourceRange range) {
		return new Region(range.getOffset(), range.getLength());
	}
	
	private IDocument getDocument(IDocumentProvider provider, IEditorInput input) {
		if (input == null)
			return null;
		IDocument result= null;
		try {
			provider.connect(input);
			result= provider.getDocument(input);
		} catch (CoreException e) {
		} finally {
			provider.disconnect(input);
		}
		return result;
	}	
}
