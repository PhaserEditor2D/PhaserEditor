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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.compare.EditionSelectionDialog;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;


class JavaAddElementFromHistoryImpl extends JavaHistoryActionImpl {
	
	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.ui.compare.AddFromHistoryAction"; //$NON-NLS-1$
	
	JavaAddElementFromHistoryImpl() {
		super(true);
	}
	
	public void run(ISelection selection) {
		
		String errorTitle= CompareMessages.AddFromHistory_title; 
		String errorMessage= CompareMessages.AddFromHistory_internalErrorMessage; 
		Shell shell= getShell();
		
		IJavaScriptUnit cu= null;
		IParent parent= null;
		IMember input= null;
		
		// analyze selection
		if (selection.isEmpty()) {
			// no selection: we try to use the editor's input
			JavaEditor editor= getEditor();
			if (editor != null) {
				IEditorInput editorInput= editor.getEditorInput();
				IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
				if (manager != null) {
					cu= manager.getWorkingCopy(editorInput);
					parent= cu;
				}
			}
		} else {
			input= getEditionElement(selection);
			if (input != null) {
				cu= input.getJavaScriptUnit();
				parent= input;
				input= null;

			} else {
				if (selection instanceof IStructuredSelection) {
					Object o= ((IStructuredSelection)selection).getFirstElement();
					if (o instanceof IJavaScriptUnit) {
						cu= (IJavaScriptUnit) o;
						parent= cu;
					}
				}
			}
		}
		
		if (parent == null || cu == null) {
			String invalidSelectionMessage= CompareMessages.AddFromHistory_invalidSelectionMessage; 
			MessageDialog.openInformation(shell, errorTitle, invalidSelectionMessage);
			return;
		}
		
		IFile file= getFile(parent);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
				
		boolean inEditor= beingEdited(file);

		IStatus status= Resources.makeCommittable(file, shell);
		if (!status.isOK()) {
			return;
		}
		
		// get the document where to insert the text
		IPath path= file.getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		ITextFileBuffer textFileBuffer= null;
		try {
			bufferManager.connect(path, LocationKind.IFILE, null);
			textFileBuffer= bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
			IDocument document= textFileBuffer.getDocument();
			
			// configure EditionSelectionDialog and let user select an edition
			ITypedElement target= new JavaTextBufferNode(file, document, inEditor);
			ITypedElement[] editions= buildEditions(target, file);
											
			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setAddMode(true);
			d.setHelpContextId(IJavaHelpContextIds.ADD_ELEMENT_FROM_HISTORY_DIALOG);
			ITypedElement selected= d.selectEdition(target, editions, parent);
			if (selected == null)
				return;	// user cancel
								
			IJavaScriptUnit cu2= cu;
			if (parent instanceof IMember)
				cu2= ((IMember)parent).getJavaScriptUnit();
			
			JavaScriptUnit root= parsePartialCompilationUnit(cu2);
			ASTRewrite rewriter= ASTRewrite.create(root.getAST());
			
			ITypedElement[] results= d.getSelection();
			for (int i= 0; i < results.length; i++) {
				
			    // create an AST node
				ASTNode newNode= createASTNode(rewriter, results[i], TextUtilities.getDefaultLineDelimiter(document), cu.getJavaScriptProject());
				if (newNode == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;	
				}
				
				// now determine where to put the new node
				if (newNode instanceof PackageDeclaration) {
				    rewriter.set(root, JavaScriptUnit.PACKAGE_PROPERTY, newNode, null);
				    
				} else if (newNode instanceof ImportDeclaration) {
					ListRewrite lw= rewriter.getListRewrite(root, JavaScriptUnit.IMPORTS_PROPERTY);
					lw.insertFirst(newNode, null);
					
				} else {	// class, interface, enum, annotation, method, field
					
					if (parent instanceof IJavaScriptUnit) {	// top level
						ListRewrite lw= rewriter.getListRewrite(root, JavaScriptUnit.TYPES_PROPERTY);
						int index= ASTNodes.getInsertionIndex((BodyDeclaration)newNode, root.types());
						lw.insertAt(newNode, index, null);
						
					} else if (parent instanceof IType) {
						ASTNode declaration= getBodyContainer(root, (IType)parent);
						if (declaration instanceof TypeDeclaration) {
							List container= ASTNodes.getBodyDeclarations(declaration);
							int index= ASTNodes.getInsertionIndex((BodyDeclaration)newNode, container);
							ListRewrite lw= rewriter.getListRewrite(declaration, ASTNodes.getBodyDeclarationsProperty(declaration));
							lw.insertAt(newNode, index, null);
						}
					} else {
						JavaScriptPlugin.logErrorMessage("JavaAddElementFromHistoryImpl: unknown container " + parent); //$NON-NLS-1$
					}
					
				}
			}
			
			Map options= null;
			IJavaScriptProject javaProject= cu2.getJavaScriptProject();
			if (javaProject != null)
				options= javaProject.getOptions(true);
			applyChanges(rewriter, document, textFileBuffer, shell, inEditor, options);

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
	
	/**
	 * Creates a place holder ASTNode for the given element.
	 * @param rewriter
	 * @param element
	 * @param delimiter the line delimiter
	 * @param project 
	 * @return a ASTNode or null
	 * @throws CoreException
	 */
	private ASTNode createASTNode(ASTRewrite rewriter, ITypedElement element, String delimiter, IJavaScriptProject project) throws CoreException {
		if (element instanceof IStreamContentAccessor) {
			String content= JavaCompareUtilities.readString((IStreamContentAccessor)element);
			if (content != null) {
				content= trimTextBlock(content, delimiter, project);
				if (content != null) {
				    int type= getPlaceHolderType(element);
				    if (type != -1)
				        return rewriter.createStringPlaceholder(content, type);
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the corresponding place holder type for the given element.
	 * @return a place holder type (see ASTRewrite) or -1 if there is no corresponding placeholder
	 */
	private int getPlaceHolderType(ITypedElement element) {
		
		if (element instanceof DocumentRangeNode) {
			JavaNode jn= (JavaNode) element;
			switch (jn.getTypeCode()) {
				
			case JavaNode.PACKAGE:
			    return ASTNode.PACKAGE_DECLARATION;

			case JavaNode.CLASS:
			case JavaNode.INTERFACE:
				return ASTNode.TYPE_DECLARATION;
				
			case JavaNode.CONSTRUCTOR:
			case JavaNode.METHOD:
				return ASTNode.FUNCTION_DECLARATION;
				
			case JavaNode.FIELD:
				return ASTNode.FIELD_DECLARATION;
				
			case JavaNode.INIT:
				return ASTNode.INITIALIZER;

			case JavaNode.IMPORT:
			case JavaNode.IMPORT_CONTAINER:
				return ASTNode.IMPORT_DECLARATION;

			case JavaNode.CU:
			    return ASTNode.JAVASCRIPT_UNIT;
			}
		}
		return -1;
	}

	protected boolean isEnabled(ISelection selection) {
		
		if (selection.isEmpty()) {
			JavaEditor editor= getEditor();
			if (editor != null) {
				// we check whether editor shows JavaScriptUnit
				IEditorInput editorInput= editor.getEditorInput();
				IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
				return manager.getWorkingCopy(editorInput) != null;
			}
			return false;
		}
		
		if (selection instanceof IStructuredSelection) {
			Object o= ((IStructuredSelection)selection).getFirstElement();
			if (o instanceof IJavaScriptUnit)
				return true;
		}
		
		return super.isEnabled(selection);
	}
}
