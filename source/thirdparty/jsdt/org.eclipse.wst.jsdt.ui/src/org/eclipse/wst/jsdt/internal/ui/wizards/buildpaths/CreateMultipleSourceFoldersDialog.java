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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.AbstractOpenWizardAction;

public class CreateMultipleSourceFoldersDialog extends TrayDialog {

	private final class FakeFolderBaseWorkbenchContentProvider extends BaseWorkbenchContentProvider {
		/**
		 * {@inheritDoc}
		 */
		public Object getParent(Object element) {
			Object object= fNonExistingFolders.get(element);
			if (object != null)
				return object;
			
			return super.getParent(element);
		}

		/**
		 * {@inheritDoc}
		 */
		public Object[] getChildren(Object element) {
			List result= new ArrayList();
			//all keys with value element
			Set keys= fNonExistingFolders.keySet();
			for (Iterator iter= keys.iterator(); iter.hasNext();) {
				Object key= iter.next();
				if (fNonExistingFolders.get(key).equals(element)) {
					result.add(key);
				}
			}
			if (result.size() == 0)
				return super.getChildren(element);
			
			Object[] children= super.getChildren(element);
			for (int i= 0; i < children.length; i++) {
				result.add(children[i]);
			}
			return result.toArray();
		}
	}

	private final IJavaScriptProject fJavaProject;
	private final CPListElement[] fExistingElements;
	private String fOutputLocation;
	private final HashSet fRemovedElements;
	private final HashSet fModifiedElements;
	private final HashSet fInsertedElements;
	private final Hashtable fNonExistingFolders;

	public CreateMultipleSourceFoldersDialog(final IJavaScriptProject javaProject, final CPListElement[] existingElements, final String outputLocation, Shell shell) {
		super(shell);
		fJavaProject= javaProject;
		fExistingElements= existingElements;
		fOutputLocation= outputLocation;
		fRemovedElements= new HashSet();
		fModifiedElements= new HashSet();
		fInsertedElements= new HashSet();
		fNonExistingFolders= new Hashtable();
		
		for (int i= 0; i < existingElements.length; i++) {
			CPListElement cur= existingElements[i];
			if (cur.getResource() == null || !cur.getResource().exists()) {
				addFakeFolder(fJavaProject.getProject(), cur);
			}
		}
	}

	public int open() {
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		List existingContainers= getExistingContainers(fExistingElements);

		IProject[] allProjects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList rejectedElements= new ArrayList(allProjects.length);
		IProject currProject= fJavaProject.getProject();
		for (int i= 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(currProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new FakeFolderBaseWorkbenchContentProvider();

		String title= NewWizardMessages.SourceContainerWorkbookPage_ExistingSourceFolderDialog_new_title; 
		String message= NewWizardMessages.SourceContainerWorkbookPage_ExistingSourceFolderDialog_edit_description; 


		MultipleFolderSelectionDialog dialog= new MultipleFolderSelectionDialog(getShell(), lp, cp) {
			protected Control createDialogArea(Composite parent) {
				Control result= super.createDialogArea(parent);
				PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJavaHelpContextIds.BP_CHOOSE_EXISTING_FOLDER_TO_MAKE_SOURCE_FOLDER);
				return result;
			}
			
			protected Object createFolder(final IContainer container) {
				final Object[] result= new Object[1];
				final CPListElement newElement= new CPListElement(fJavaProject, IIncludePathEntry.CPE_SOURCE);
				final AddSourceFolderWizard wizard= newSourceFolderWizard(newElement, fExistingElements, fOutputLocation, container);
				AbstractOpenWizardAction action= new AbstractOpenWizardAction() {
					protected INewWizard createWizard() throws CoreException {
						return wizard;
					}
				};
				action.addPropertyChangeListener(new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (event.getProperty().equals(IAction.RESULT)) {
							if (event.getNewValue().equals(Boolean.TRUE)) {
								result[0]= addFakeFolder(fJavaProject.getProject(), newElement);
							} else {
								wizard.cancel();
							}
						}
					}
				});
				action.run();
				return result[0];
			}
		};
		dialog.setExisting(existingContainers.toArray());
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(fJavaProject.getProject().getParent());
		dialog.setInitialFocus(fJavaProject.getProject());

		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();	
			for (int i= 0; i < elements.length; i++) {
				IResource res= (IResource)elements[i];
				
				IPath[] defaultExclusions = JavaScriptCore.getJavaScriptCore().getDefaultClasspathExclusionPatterns(); 
				CPListElement cple = new CPListElement(fJavaProject, IIncludePathEntry.CPE_SOURCE, res.getFullPath(), res);
				for (int j = 0; j < defaultExclusions.length; j++) {
					cple.addToExclusions(res.getFullPath().append(defaultExclusions[j]));
				}
				fInsertedElements.add(cple);
			}

			if (fExistingElements.length == 1) {
				CPListElement existingElement= fExistingElements[0];
				if (existingElement.getResource() instanceof IProject) {
					if (!removeProjectFromBP(existingElement)) {
						ArrayList added= new ArrayList(fInsertedElements);
						HashSet updatedEclusionPatterns= new HashSet();
						addExlusionPatterns(added, updatedEclusionPatterns);
						fModifiedElements.addAll(updatedEclusionPatterns);
					}
				}
			} else {
				ArrayList added= new ArrayList(fInsertedElements);
				HashSet updatedEclusionPatterns= new HashSet();
				addExlusionPatterns(added, updatedEclusionPatterns);
				fModifiedElements.addAll(updatedEclusionPatterns);
			}
			return Window.OK;
		} else {
			return Window.CANCEL;
		}
	}

	public List getInsertedElements() {
		return new ArrayList(fInsertedElements);
	}

	public List getRemovedElements() {
		return new ArrayList(fRemovedElements);
	}

	public List getModifiedElements() {
		return new ArrayList(fModifiedElements);
	}

	public IPath getOutputLocation() {
		return new Path(fOutputLocation).makeAbsolute();
	}

	/**
	 * Asks to change the output folder to 'proj/bin' when no source folders were existing
	 */ 
	private boolean removeProjectFromBP(CPListElement existing) {
		IPath outputFolder= new Path(fOutputLocation);

		IPath newOutputFolder= null;
		String message;
		if (outputFolder.segmentCount() == 1) {
			String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
			newOutputFolder= outputFolder.append(outputFolderName);
			message= Messages.format(NewWizardMessages.SourceContainerWorkbookPage_ChangeOutputLocationDialog_project_and_output_message, newOutputFolder); 
		} else {
			message= NewWizardMessages.SourceContainerWorkbookPage_ChangeOutputLocationDialog_project_message; 
		}
		String title= NewWizardMessages.SourceContainerWorkbookPage_ChangeOutputLocationDialog_title; 
		if (MessageDialog.openQuestion(getShell(), title, message)) {
			fRemovedElements.add(existing);
			if (newOutputFolder != null) {
				fOutputLocation= newOutputFolder.toString();
			}
			return true;
		}
		return false;
	}

	private void addExlusionPatterns(List newEntries, Set modifiedEntries) {
		BuildPathBasePage.fixNestingConflicts((CPListElement[])newEntries.toArray(new CPListElement[newEntries.size()]), fExistingElements, modifiedEntries);
		if (!modifiedEntries.isEmpty()) {
			String title= NewWizardMessages.SourceContainerWorkbookPage_exclusion_added_title; 
			String message= NewWizardMessages.SourceContainerWorkbookPage_exclusion_added_message; 
			MessageDialog.openInformation(getShell(), title, message);
		}
	}
	
	private AddSourceFolderWizard newSourceFolderWizard(CPListElement element, CPListElement[] existing, String outputLocation, IContainer parent) {
		AddSourceFolderWizard wizard= new AddSourceFolderWizard(existing, element, false, true, false, false, false, parent);
		wizard.setDoFlushChange(false);
		return wizard;
	}
	
	private List getExistingContainers(CPListElement[] existingElements) {
		List res= new ArrayList();
		for (int i= 0; i < existingElements.length; i++) {
			IResource resource= existingElements[i].getResource();
			if (resource instanceof IContainer) {
				res.add(resource);	
			}
		}
		Set keys= fNonExistingFolders.keySet();
		for (Iterator iter= keys.iterator(); iter.hasNext();) {
			IFolder folder= (IFolder)iter.next();
			res.add(folder);
		}
		return res;
	}
	
	private IFolder addFakeFolder(final IContainer container, final CPListElement element) {
		IFolder result;
		IPath projectPath= fJavaProject.getPath();
		IPath path= element.getPath();
		if (projectPath.isPrefixOf(path)) {
			path= path.removeFirstSegments(projectPath.segmentCount());
		}
		result= container.getFolder(path);
		IFolder folder= result;
		do {
			IContainer parent= folder.getParent();
			fNonExistingFolders.put(folder, parent);
			if (parent instanceof IFolder) {
				folder= (IFolder)parent;
			} else {
				folder= null;
			}
		} while (folder != null && !folder.exists());
		return result;
	}
}
