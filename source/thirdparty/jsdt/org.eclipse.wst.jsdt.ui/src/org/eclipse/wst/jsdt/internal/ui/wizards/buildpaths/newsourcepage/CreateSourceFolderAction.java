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
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.AddSourceFolderWizard;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;

//SelectedElements iff enabled: IJavaScriptProject && size==1
public class CreateSourceFolderAction extends BuildpathModifierAction {

	public CreateSourceFolderAction(IWorkbenchSite site) {
		this(site, null, PlatformUI.getWorkbench().getProgressService());
	}
	
	public CreateSourceFolderAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context);
    }
	
	private CreateSourceFolderAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context) {
		super(site, selectionTarget, BuildpathModifierAction.CREATE_FOLDER);
		
		setText(ActionMessages.OpenNewSourceFolderWizardAction_text2); 
		setDescription(ActionMessages.OpenNewSourceFolderWizardAction_description); 
		setToolTipText(ActionMessages.OpenNewSourceFolderWizardAction_tooltip); 
		setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPACKROOT);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_SOURCEFOLDER_WIZARD_ACTION);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
	    return NewWizardMessages.PackageExplorerActionGroup_FormText_createNewSourceFolder;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		Shell shell= getShell();
	
		try {
			IJavaScriptProject javaProject= (IJavaScriptProject)getSelectedElements().get(0);
			
            CPListElement newEntrie= new CPListElement(javaProject, IIncludePathEntry.CPE_SOURCE);
            CPListElement[] existing= CPListElement.createFromExisting(javaProject);
            boolean isProjectSrcFolder= CPListElement.isProjectSourceFolder(existing, javaProject);
			
            AddSourceFolderWizard wizard= new AddSourceFolderWizard(existing, newEntrie, false, false, false, isProjectSrcFolder, isProjectSrcFolder);
			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(javaProject));
			
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

    protected boolean canHandle(IStructuredSelection selection) {
    	if (selection.size() != 1)
    		return false;
    		
    	if (!(selection.getFirstElement() instanceof IJavaScriptProject))
    		return false;
    	
    	return true;
    }	
}
