/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.wst.jsdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.EditFilterWizard;

//SelectedElements iff enabled: (IJavaScriptProject || IPackageFragmentRoot) && size == 1
public class EditFilterAction extends BuildpathModifierAction {
	
	public EditFilterAction(IWorkbenchSite site) {
		this(site, null, PlatformUI.getWorkbench().getProgressService());
	}
	
	public EditFilterAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context);
    }
	
	private EditFilterAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context) {
		super(site, selectionTarget, BuildpathModifierAction.EDIT_FILTERS);
		
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_label);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_CONFIGURE_BUILDPATH_FILTERS);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_tooltip); 
		setDescription(NewWizardMessages.PackageExplorerActionGroup_FormText_Edit);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CONFIGURE_BUILDPATH_FILTERS);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
		if (!isEnabled())
			return null;
		

		return NewWizardMessages.PackageExplorerActionGroup_FormText_Edit;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		Shell shell= getShell();
	
		try {
			EditFilterWizard wizard= createWizard();
			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(getSelectedElements().get(0)));
			
			WizardDialog dialog= new WizardDialog(shell, wizard);
			PixelConverter converter= new PixelConverter(JFaceResources.getDialogFont());
			dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
			dialog.create();
			int res= dialog.open();
			if (res == Window.OK) {
				BuildpathDelta delta= new BuildpathDelta(getToolTipText());
				
				ArrayList newEntries= wizard.getExistingEntries();
				delta.setNewEntries((CPListElement[])newEntries.toArray(new CPListElement[newEntries.size()]));
				
				IResource resource= wizard.getCreatedElement().getCorrespondingResource();
				delta.addCreatedResource(resource);
					
				informListeners(delta);
				
				selectAndReveal(new StructuredSelection(wizard.getCreatedElement()));
			}
			
			notifyResult(res == Window.OK);
		} catch (CoreException e) {
			String title= NewWizardMessages.AbstractOpenWizardAction_createerror_title; 
			String message= NewWizardMessages.AbstractOpenWizardAction_createerror_message; 
			ExceptionHandler.handle(e, shell, title, message);
		}
	}
	
	private EditFilterWizard createWizard() throws CoreException {
		IJavaScriptProject javaProject= null;
		Object firstElement= getSelectedElements().get(0);
		if (firstElement instanceof IJavaScriptProject) {
			javaProject= (IJavaScriptProject)firstElement;
		} else {
			javaProject= ((IPackageFragmentRoot)firstElement).getJavaScriptProject();
		}
		CPListElement[] existingEntries= CPListElement.createFromExisting(javaProject);
		CPListElement elementToEdit= findElement((IJavaScriptElement)firstElement, existingEntries);
		return new EditFilterWizard(existingEntries, elementToEdit);
	}
	
	private static CPListElement findElement(IJavaScriptElement element, CPListElement[] elements) {
		IPath path= element.getPath();
		for (int i= 0; i < elements.length; i++) {
			CPListElement cur= elements[i];
			if (cur.getEntryKind() == IIncludePathEntry.CPE_SOURCE && cur.getPath().equals(path)) {
				return cur;
			}
		}
		return null;
	}

	protected boolean canHandle(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		
		try {
			Object element= selection.getFirstElement();
			if (element instanceof IJavaScriptProject) {
				return ClasspathModifier.isSourceFolder((IJavaScriptProject)element);
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot packageFragmentRoot= ((IPackageFragmentRoot) element);
				if (packageFragmentRoot.getKind() != IPackageFragmentRoot.K_SOURCE)
					return false;
				
				return packageFragmentRoot.getJavaScriptProject() != null;
			}
		} catch (JavaScriptModelException e) {
		}
		return false;
	}
}
