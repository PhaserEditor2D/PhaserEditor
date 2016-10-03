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
package org.eclipse.wst.jsdt.internal.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.history.ElementLocalHistoryPageSource;
import org.eclipse.team.ui.history.HistoryPageCompareEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;


/**
 * Provides "Replace from local history" for Java elements.
 */
class JavaReplaceWithEditionActionImpl extends JavaHistoryActionImpl {
	
	protected boolean fPrevious= false;

	JavaReplaceWithEditionActionImpl(boolean previous) {
		super(true);
		fPrevious= previous;
	}
	
	public void run(ISelection selection) {
		
		Shell shell= getShell();
		
		final IMember input= getEditionElement(selection);
		if (input == null) {
			MessageDialog.openInformation(shell, CompareMessages.ReplaceFromHistory_title, CompareMessages.ReplaceFromHistory_invalidSelectionMessage);
			return;
		}
		
		final IFile file= getFile(input);
		if (file == null) {
			showError();
			return;
		}
		
		IStatus status= Resources.makeCommittable(file, shell);
		if (!status.isOK()) {
			return;
		}
		
		if (fPrevious) {
			String errorTitle= CompareMessages.ReplaceFromHistory_title; 
			String errorMessage= CompareMessages.ReplaceFromHistory_internalErrorMessage; 
			try {
				ITypedElement ti = ElementLocalHistoryPageSource.getPreviousEdition(file, input);
				if (ti == null) {
					MessageDialog.openInformation(shell, errorTitle, CompareMessages.ReplaceFromHistory_parsingErrorMessage);	
					return;
				}
				replace(input, file, ti);
			} catch (TeamException e) {
				ExceptionHandler.handle(e, shell, errorTitle, errorMessage);
			}
		} else {
			JavaElementHistoryPageSource pageSource = JavaElementHistoryPageSource.getInstance();
			CompareConfiguration cc = new CompareConfiguration();
			cc.setLeftEditable(false);
			cc.setRightEditable(false);
			HistoryPageCompareEditorInput ci = new HistoryPageCompareEditorInput(cc, pageSource, input) {
				protected void performReplace(Object selectedElement) {
					if (selectedElement instanceof ITypedElement) {
						JavaReplaceWithEditionActionImpl.this.replace(input, file, (ITypedElement)selectedElement);
					}
				}
			};
			ci.setReplace(true);
			ci.setTitle(CompareMessages.JavaReplaceWithEditionActionImpl_0);
			ci.setHelpContextId(IJavaHelpContextIds.REPLACE_ELEMENT_WITH_HISTORY_DIALOG);
			CompareUI.openCompareDialog(ci);
		}
	}

	public void replace(IMember input, IFile file, ITypedElement element) {
		
		Shell shell= getShell();
		
		String errorTitle= CompareMessages.ReplaceFromHistory_title; 
		String errorMessage= CompareMessages.ReplaceFromHistory_internalErrorMessage;
		
		// get the document where to insert the text
		IPath path= file.getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		ITextFileBuffer textFileBuffer= null;
		try {
			bufferManager.connect(path, LocationKind.IFILE, null);
			textFileBuffer= bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
			IDocument document= textFileBuffer.getDocument();		
			performReplace(input, file, textFileBuffer, document, element);
	 	} catch(InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
			
		} catch(InterruptedException ex) {
			// shouldn't be called because is not cancelable
			Assert.isTrue(false);
			
		} catch(CoreException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
			
		} finally {
			try {
				if (textFileBuffer != null)
					bufferManager.disconnect(path, LocationKind.IFILE, null);
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			}
		}
	}
	
	private void performReplace(IMember input, IFile file,
			ITextFileBuffer textFileBuffer, IDocument document, ITypedElement ti)
			throws CoreException, JavaScriptModelException,
			InvocationTargetException, InterruptedException {
		
		if (ti instanceof IStreamContentAccessor) {
			
			boolean inEditor= beingEdited(file);
			
			String content= JavaCompareUtilities.readString((IStreamContentAccessor)ti);
			String newContent= trimTextBlock(content, TextUtilities.getDefaultLineDelimiter(document), input.getJavaScriptProject());
			if (newContent == null) {
				showError();
				return;
			}
			
			IJavaScriptUnit compilationUnit= input.getJavaScriptUnit();
			JavaScriptUnit root= parsePartialCompilationUnit(compilationUnit);
			
			
			final ISourceRange nameRange= input.getNameRange();
			// workaround for bug in getNameRange(): for AnnotationMembers length is negative
			int length= nameRange.getLength();
			if (length < 0)
				length= 1;
			ASTNode node2= NodeFinder.perform(root, new SourceRange(nameRange.getOffset(), length));
			ASTNode node= ASTNodes.getParent(node2, BodyDeclaration.class);
			
			//ASTNode node= getBodyContainer(root, input);
			if (node == null) {
				showError();
				return;
			}
			
			ASTRewrite rewriter= ASTRewrite.create(root.getAST());
			rewriter.replace(node, rewriter.createStringPlaceholder(newContent, node.getNodeType()), null);
			
			if (inEditor) {
				JavaEditor je= getEditor(file);
				if (je != null)
					je.setFocus();
			}
			
			Map options= null;
			IJavaScriptProject javaProject= compilationUnit.getJavaScriptProject();
			if (javaProject != null)
				options= javaProject.getOptions(true);
			applyChanges(rewriter, document, textFileBuffer, getShell(), inEditor, options);
			
		}
	}

	private void showError() {
		MessageDialog.openError(getShell(), CompareMessages.ReplaceFromHistory_title, CompareMessages.ReplaceFromHistory_internalErrorMessage);
	}
}
