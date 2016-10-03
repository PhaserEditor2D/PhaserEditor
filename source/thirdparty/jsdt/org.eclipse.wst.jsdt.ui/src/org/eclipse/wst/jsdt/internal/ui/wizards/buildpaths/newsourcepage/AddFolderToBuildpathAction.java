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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.wst.jsdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;

/**
 * The action that is run to add a project or a folder to the the build path as a source folder. Is enabled only if
 * the selected item can be added as a source folder and is not already a source folder.
 *
 */
public class AddFolderToBuildpathAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;

	public AddFolderToBuildpathAction(IWorkbenchSite site) {
		this(site, null, PlatformUI.getWorkbench().getProgressService());
	}
	
	public AddFolderToBuildpathAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context);
    }
	
	private AddFolderToBuildpathAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context) {
		super(site, selectionTarget, BuildpathModifierAction.ADD_SEL_SF_TO_BP);
		
		fContext= context;
		
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_label);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_ADD_AS_SOURCE_FOLDER);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_tooltip);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
		if (!isEnabled())
			return null;
		
		if (getSelectedElements().size() != 1)
			return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_toBuildpath;
		
		Object obj= getSelectedElements().get(0);
		if (obj instanceof IJavaScriptProject) {
			return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ProjectToBuildpath, ((IJavaScriptProject)obj).getElementName());
		} else if (obj instanceof IPackageFragment) {
			return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_PackageToBuildpath, ((IPackageFragment)obj).getElementName());
		} else if (obj instanceof IResource) {
			return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_FolderToBuildpath, ((IResource)obj).getName());
		}
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		try {
			final IJavaScriptProject project;
			Object object= getSelectedElements().get(0);
			if (object instanceof IJavaScriptProject) {
				project= (IJavaScriptProject)object;
			} else if (object instanceof IPackageFragment) {
				project= ((IPackageFragment)object).getJavaScriptProject();
			} else {
				IFolder folder= (IFolder)object;
				project= JavaScriptCore.create(folder.getProject());
				if (project == null)
					return;
			}

			final IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						List result= addToClasspath(getSelectedElements(), project, monitor);
						selectAndReveal(new StructuredSelection(result));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			fContext.run(false, false, runnable);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.AddSourceFolderToBuildpathAction_ErrorTitle);
			} else {
				JavaScriptPlugin.log(e);
			}
		} catch (final InterruptedException e) {
		}
	}

	private List addToClasspath(List elements, IJavaScriptProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (!project.getProject().hasNature(JavaScriptCore.NATURE_ID)) {
			StatusInfo rootStatus= new StatusInfo();
			rootStatus.setError(NewWizardMessages.ClasspathModifier_Error_NoNatures); 
			throw new CoreException(rootStatus);
		}
		
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, elements.size() + 4); 

        	BuildpathDelta delta= new BuildpathDelta(getToolTipText());

			List existingEntries= ClasspathModifier.getExistingEntries(project);

			List newEntries= new ArrayList();
			for (int i= 0; i < elements.size(); i++) {
				Object element= elements.get(i);
				CPListElement entry;
				if (element instanceof IResource)
					entry= ClasspathModifier.addToClasspath((IResource) element, existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
				else
					entry= ClasspathModifier.addToClasspath((IJavaScriptElement) element, existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
				newEntries.add(entry);
			}

			Set modifiedSourceEntries= new HashSet();
			BuildPathBasePage.fixNestingConflicts((CPListElement[])newEntries.toArray(new CPListElement[newEntries.size()]), (CPListElement[])existingEntries.toArray(new CPListElement[existingEntries.size()]), modifiedSourceEntries);

			ClasspathModifier.setNewEntry(existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));

			ClasspathModifier.commitClassPath(existingEntries, project, new SubProgressMonitor(monitor, 1));
			
        	delta.setNewEntries((CPListElement[])existingEntries.toArray(new CPListElement[existingEntries.size()]));
        	informListeners(delta);

			List result= new ArrayList();
			for (int i= 0; i < newEntries.size(); i++) {
				IIncludePathEntry entry= ((CPListElement) newEntries.get(i)).getClasspathEntry();
				IJavaScriptElement root;
				if (entry.getPath().equals(project.getPath()))
					root= project;
				else
					root= project.findPackageFragmentRoot(entry.getPath());
				if (root != null) {
					result.add(root);
				}
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Determines whether this action can be run on the current selection or selections. If any
	 * of the items is not able to run this action then it will not be presented to the user in the wizard.
	 */
	protected boolean canHandle(IStructuredSelection elements) {
		if (elements.size() == 0)
			return false;
		try {
			for (Iterator iter= elements.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof IJavaScriptProject) {
					// if this is a javascript project and its already the source folder then return false
					if (ClasspathModifier.isSourceFolder((IJavaScriptProject)element))
						return false;
				} else if (element instanceof IPackageFragment) {
					IPackageFragment fragment= (IPackageFragment)element;
					if (ClasspathModifier.isDefaultFragment(fragment))
	                    return false;
	                
	                if (((IPackageFragmentRoot)fragment.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)).isArchive())
	                    return false;
				} else if (element instanceof IFolder) {
					IProject project= ((IFolder)element).getProject();
					IJavaScriptProject javaProject= JavaScriptCore.create(project);
					if (javaProject == null || !javaProject.exists())
						return false;
				} else {
					return false;
				}
			}
			return true;
		} catch (CoreException e) {
		}
		return false;
	}
}
