/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class CopyQualifiedNameAction extends SelectionDispatchAction {
	
	private static final long LABEL_FLAGS= JavaScriptElementLabels.F_FULLY_QUALIFIED | JavaScriptElementLabels.M_FULLY_QUALIFIED | JavaScriptElementLabels.I_FULLY_QUALIFIED | JavaScriptElementLabels.T_FULLY_QUALIFIED | JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.USE_RESOLVED | JavaScriptElementLabels.T_TYPE_PARAMETERS | JavaScriptElementLabels.CU_QUALIFIED | JavaScriptElementLabels.CF_QUALIFIED;

    //TODO: Make API
	public static final String ACTION_DEFINITION_ID= "org.eclipse.wst.jsdt.ui.edit.text.java.copy.qualified.name"; //$NON-NLS-1$

	//TODO: Make API
	public static final String ACTION_HANDLER_ID= "org.eclipse.wst.jsdt.ui.actions.CopyQualifiedName"; //$NON-NLS-1$

	private JavaEditor fEditor;

    public CopyQualifiedNameAction(JavaEditor editor) {
    	this(editor.getSite());
		fEditor= editor;
		setEnabled(true);
	}

	public CopyQualifiedNameAction(IWorkbenchSite site) {
		super(site);
		
		setText(ActionMessages.CopyQualifiedNameAction_ActionName);
		setToolTipText(ActionMessages.CopyQualifiedNameAction_ToolTipText);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_COPY_QUALIFIED_NAME);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_COPY_QUALIFIED_NAME);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection.toArray()));
	}
	
	public void selectionChanged(ITextSelection selection) {
		//Must not create an AST
	}

	private boolean canEnable(Object[] objects) {
		for (int i= 0; i < objects.length; i++) {
			Object element= objects[i];
			if (isValideElement(element))
				return true;
		}

		return false;
	}
	
	private boolean isValideElement(Object element) {
		if (element instanceof IMember)
			return true;
		
		if (element instanceof IClassFile)
			return true;
		
		if (element instanceof IJavaScriptUnit)
			return true;
		
		if (element instanceof IImportDeclaration)
			return true;
		
		if (element instanceof IPackageFragment)
			return true;
		
		return false;
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
    	
    	try {
			IJavaScriptElement[] elements= getSelectedElements();
			if (elements == null) {
				MessageDialog.openInformation(getShell(), ActionMessages.CopyQualifiedNameAction_InfoDialogTitel, ActionMessages.CopyQualifiedNameAction_NoElementToQualify);
				return;
			}

			Object[] data= null;
			Transfer[] dataTypes= null;
			
			if (elements.length == 1) {
				String qualifiedName= JavaScriptElementLabels.getElementLabel(elements[0], LABEL_FLAGS);
				IResource resource= elements[0].getCorrespondingResource();
				
				if (resource != null) {
					IPath location= resource.getLocation();
					if (location != null) {
						data= new Object[] {qualifiedName, resource, new String[] {location.toOSString()}};
						dataTypes= new Transfer[] {TextTransfer.getInstance(), ResourceTransfer.getInstance(), FileTransfer.getInstance()};
					} else {
						data= new Object[] {qualifiedName, resource};
						dataTypes= new Transfer[] {TextTransfer.getInstance(), ResourceTransfer.getInstance()};
					}
				} else {
					data= new Object[] {qualifiedName};
					dataTypes= new Transfer[] {TextTransfer.getInstance()};
				}
			} else {
				StringBuffer buf= new StringBuffer();
				buf.append(JavaScriptElementLabels.getElementLabel(elements[0], LABEL_FLAGS));
				for (int i= 1; i < elements.length; i++) {
					IJavaScriptElement element= elements[i];
					
					String qualifiedName= JavaScriptElementLabels.getElementLabel(element, LABEL_FLAGS);
					buf.append('\r').append('\n').append(qualifiedName);
				}
				data= new Object[] {buf.toString()};
				dataTypes= new Transfer[] {TextTransfer.getInstance()};
			}
			
			Clipboard clipboard= new Clipboard(getShell().getDisplay());
			try {
				clipboard.setContents(data, dataTypes);
			} catch (SWTError e) {
				if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
					throw e;
				}
				if (MessageDialog.openQuestion(getShell(), ActionMessages.CopyQualifiedNameAction_ErrorTitle, ActionMessages.CopyQualifiedNameAction_ErrorDescription)) {
					clipboard.setContents(data, dataTypes);
				}
			} finally {
				clipboard.dispose();
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
    }

    private IJavaScriptElement[] getSelectedElements() throws JavaScriptModelException {
    	if (fEditor != null) {
    		IJavaScriptElement element= getSelectedElement(fEditor);
    		if (element == null)
    			return null;
    		
    		return new IJavaScriptElement[] {element}; 
    	}
    	
    	ISelection selection= getSelection();
    	if (!(selection instanceof IStructuredSelection))
    		return null;
    	
    	List result= new ArrayList();
    	for (Iterator iter= ((IStructuredSelection)selection).iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (isValideElement(element))
				result.add(element);
		}
    	if (result.isEmpty())
    		return null;
    	
		return (IJavaScriptElement[])result.toArray(new IJavaScriptElement[result.size()]);
	}

	private IJavaScriptElement getSelectedElement(JavaEditor editor) {
		ISourceViewer viewer= editor.getViewer();
		if (viewer == null)
			return null;
		
		Point selectedRange= viewer.getSelectedRange();
		int length= selectedRange.y;
		int offset= selectedRange.x;
		
		IJavaScriptElement element= JavaScriptUI.getEditorInputJavaElement(editor.getEditorInput());		
		JavaScriptUnit ast= ASTProvider.getASTProvider().getAST(element, ASTProvider.WAIT_YES, null);
		if (ast == null)
			return null;

		NodeFinder finder= new NodeFinder(offset, length);
		ast.accept(finder);
		ASTNode node= finder.getCoveringNode();
		
		IBinding binding= null;
		if (node instanceof Name) {
			binding= ((Name)node).resolveBinding();
		} else if (node instanceof FunctionInvocation) {
			binding= ((FunctionInvocation)node).resolveMethodBinding();
		} else if (node instanceof FunctionDeclaration) {
			binding= ((FunctionDeclaration)node).resolveBinding();
		} else if (node instanceof Type) {
			binding= ((Type)node).resolveBinding();
		} else if (node instanceof AnonymousClassDeclaration) {
			binding= ((AnonymousClassDeclaration)node).resolveBinding();
		} else if (node instanceof TypeDeclaration) {
			binding= ((TypeDeclaration)node).resolveBinding();
		} else if (node instanceof JavaScriptUnit) {
			return ((JavaScriptUnit)node).getJavaElement();
		} else if (node instanceof Expression) {
			binding= ((Expression)node).resolveTypeBinding();
		} else if (node instanceof ImportDeclaration) {
			binding= ((ImportDeclaration)node).resolveBinding();
		} else if (node instanceof MemberRef) {
			binding= ((MemberRef)node).resolveBinding();
		} else if (node instanceof PackageDeclaration) {
			binding= ((PackageDeclaration)node).resolveBinding();
		} else if (node instanceof VariableDeclaration) {
			binding= ((VariableDeclaration)node).resolveBinding();
		} 
			
		if (binding != null)
			return binding.getJavaElement();

		return null;
	}

}
