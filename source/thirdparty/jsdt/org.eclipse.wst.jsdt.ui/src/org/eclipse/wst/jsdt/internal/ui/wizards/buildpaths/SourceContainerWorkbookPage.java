/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.TreeListDialogField;
import org.eclipse.wst.jsdt.ui.actions.AbstractOpenWizardAction;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;

public class SourceContainerWorkbookPage extends BuildPathBasePage {
	
	private class OpenBuildPathWizardAction extends AbstractOpenWizardAction implements IPropertyChangeListener {
		
		private final BuildPathWizard fWizard;
		private final List fSelectedElements;
		
		public OpenBuildPathWizardAction(BuildPathWizard wizard) {
			fWizard= wizard;
			addPropertyChangeListener(this);
			fSelectedElements= fFoldersList.getSelectedElements();
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected INewWizard createWizard() throws CoreException {
			return fWizard;
		}

		/**
		 * {@inheritDoc}
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(IAction.RESULT)) {
				if (event.getNewValue().equals(Boolean.TRUE)) {
					finishWizard();
				} else {
					fWizard.cancel();
				}
			}
		}
		
		protected void finishWizard() {
			List insertedElements= fWizard.getInsertedElements();
			refresh(insertedElements, fWizard.getRemovedElements(), fWizard.getModifiedElements());
			
			if (insertedElements.isEmpty()) {
				fFoldersList.postSetSelection(new StructuredSelection(fSelectedElements));
			}
		}

	}
	
	private static AddSourceFolderWizard newSourceFolderWizard(CPListElement element, List/*<CPListElement>*/ existingElements, String outputLocation, boolean newFolder) {
		CPListElement[] existing= (CPListElement[])existingElements.toArray(new CPListElement[existingElements.size()]);
		AddSourceFolderWizard wizard= new AddSourceFolderWizard(existing, element, false, newFolder, newFolder, newFolder?CPListElement.isProjectSourceFolder(existing, element.getJavaProject()):false, newFolder);
		wizard.setDoFlushChange(false);
		return wizard;
	}
	
	private static AddSourceFolderWizard newLinkedSourceFolderWizard(CPListElement element, List/*<CPListElement>*/ existingElements, String outputLocation, boolean newFolder) {
		CPListElement[] existing= (CPListElement[])existingElements.toArray(new CPListElement[existingElements.size()]);
		AddSourceFolderWizard wizard= new AddSourceFolderWizard(existing, element, true, newFolder, newFolder, newFolder?CPListElement.isProjectSourceFolder(existing, element.getJavaProject()):false, newFolder);
		wizard.setDoFlushChange(false);
		return wizard;
	}
	
	private static EditFilterWizard newEditFilterWizard(CPListElement element, List/*<CPListElement>*/ existingElements, String outputLocation) {
		CPListElement[] existing= (CPListElement[])existingElements.toArray(new CPListElement[existingElements.size()]);
		EditFilterWizard result = new EditFilterWizard(existing, element);
		result.setDoFlushChange(false);
		return result;
	}

	private ListDialogField fClassPathList;
	private IJavaScriptProject fCurrJProject;
	
	private Control fSWTControl;
	private TreeListDialogField fFoldersList;
	
	//private StringDialogField fOutputLocationField;
	
	//private SelectionButtonDialogField fUseFolderOutputs;
	
	private final int IDX_ADD= 0;
	private final int IDX_ADD_LINK= 1;
	
	private final int IDX_ADDEXT= 3;
	private final int IDX_ADDJAR= 4;
	
	private final int IDX_ADDVAR= 5;
	private final int IDX_EDIT=7;
	private final int IDX_REMOVE= 8;	
	
	public SourceContainerWorkbookPage(ListDialogField classPathList) {
		fClassPathList= classPathList;
	
		//fOutputLocationField= outputLocationField;
		
		fSWTControl= null;
				
		SourceContainerAdapter adapter= new SourceContainerAdapter();
					
		String[] buttonLabels;

		buttonLabels= new String[] { 
			NewWizardMessages.SourceContainerWorkbookPage_folders_add_button, 
			NewWizardMessages.SourceContainerWorkbookPage_folders_link_source_button,
			/* 1 */ null,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addextjar_button, 
			NewWizardMessages.LibrariesWorkbookPage_libraries_addjar_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addvariable_button, 
			null,
			NewWizardMessages.SourceContainerWorkbookPage_folders_edit_button, 
			NewWizardMessages.SourceContainerWorkbookPage_folders_remove_button
		};
		
		fFoldersList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fFoldersList.setDialogFieldListener(adapter);
		fFoldersList.setLabelText(NewWizardMessages.SourceContainerWorkbookPage_folders_label); 
		
		fFoldersList.setViewerComparator(new CPListElementSorter());
		fFoldersList.enableButton(IDX_EDIT, false);
		fFoldersList.enableButton(IDX_ADDVAR, false);
		
//		fUseFolderOutputs= new SelectionButtonDialogField(SWT.CHECK);
//		fUseFolderOutputs.setSelection(false);
//		fUseFolderOutputs.setLabelText(NewWizardMessages.SourceContainerWorkbookPage_folders_check); 
//		fUseFolderOutputs.setDialogFieldListener(adapter);
	}
	
	public void init(IJavaScriptProject jproject) {
		fCurrJProject= jproject;
		if (Display.getCurrent() != null) {
			updateFoldersList();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					updateFoldersList();
				}
			});
		}
	}
	
	private void updateFoldersList() {
		if (fSWTControl == null || fSWTControl.isDisposed()) {
			return;
		}

		ArrayList folders= new ArrayList();
	
//		boolean useFolderOutputs= false;
		List cpelements= fClassPathList.getElements();
		for (int i= 0; i < cpelements.size(); i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (isEntryKind(cpe.getEntryKind())  ) {
				folders.add(cpe);
			}
		}
		fFoldersList.setElements(folders);
		//fUseFolderOutputs.setSelection(useFolderOutputs);
		
		for (int i= 0; i < folders.size(); i++) {
			CPListElement cpe= (CPListElement) folders.get(i);
			IPath[] ePatterns= (IPath[]) cpe.getAttribute(CPListElement.EXCLUSION);
			IPath[] iPatterns= (IPath[])cpe.getAttribute(CPListElement.INCLUSION);
			if (ePatterns!=null && iPatterns!=null && (ePatterns.length > 0 || iPatterns.length > 0)) {
				fFoldersList.expandElement(cpe, 3);
			}				
		}
	}			
	
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		Composite composite= new Composite(parent, SWT.NONE);
		
		//LayoutUtil.doDefaultLayout(composite, new DialogField[] { fFoldersList, fUseFolderOutputs , fOutputLocationField}, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fFoldersList}, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fFoldersList.getTreeControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fFoldersList.setButtonsMinWidth(buttonBarWidth);
			
		fSWTControl= composite;
		
		// expand
		List elements= fFoldersList.getElements();
		for (int i= 0; i < elements.size(); i++) {
			CPListElement elem= (CPListElement) elements.get(i);
			IPath[] exclusionPatterns= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
			IPath[] inclusionPatterns= (IPath[]) elem.getAttribute(CPListElement.INCLUSION);
			if (exclusionPatterns.length > 0 || inclusionPatterns.length > 0) {
				fFoldersList.expandElement(elem, 3);
			}
		}
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaScriptPlugin.getActiveWorkbenchShell();
	}
	
	
	private class SourceContainerAdapter implements ITreeListAdapter, IDialogFieldListener {
	
		private final Object[] EMPTY_ARR= new Object[0];
		
		// -------- IListAdapter --------
		public void customButtonPressed(TreeListDialogField field, int index) {
			sourcePageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(TreeListDialogField field) {
			sourcePageSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			sourcePageDoubleClicked(field);
		}
		
		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			sourcePageKeyPressed(field, event);
		}	

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren();
			}
			return EMPTY_ARR;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof CPListElementAttribute) {
				return ((CPListElementAttribute) element).getParent();
			}
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return (element instanceof CPListElement);
		}		
		
		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			sourcePageDialogFieldChanged(field);
		}

	}
	
	protected void sourcePageKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (field == fFoldersList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}	
	}	
	
	protected void sourcePageDoubleClicked(TreeListDialogField field) {
		if (field == fFoldersList) {
			List selection= field.getSelectedElements();
			if (canEdit(selection)) {
				editEntry();
			}
		}
	}

	protected void sourcePageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] libentries= null;
		if (field == fFoldersList) {
			if (index == IDX_ADD) {
				IProject project= fCurrJProject.getProject();
				if (project.exists() && hasFolders(project)) {
					List existingElements= fFoldersList.getElements();
					CPListElement[] existing= (CPListElement[])existingElements.toArray(new CPListElement[existingElements.size()]);
					CreateMultipleSourceFoldersDialog dialog= new CreateMultipleSourceFoldersDialog(fCurrJProject, existing, "", getShell()); //$NON-NLS-1$
					if (dialog.open() == Window.OK) {
						refresh(dialog.getInsertedElements(), dialog.getRemovedElements(), dialog.getModifiedElements());
					}
				} else {
					CPListElement newElement= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_SOURCE);
					AddSourceFolderWizard wizard= newSourceFolderWizard(newElement, fFoldersList.getElements(), "", true); //$NON-NLS-1$
					OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
					action.run();
				}
			} else if (index == IDX_ADD_LINK) {
				CPListElement newElement= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_SOURCE);
				AddSourceFolderWizard wizard= newLinkedSourceFolderWizard(newElement, fFoldersList.getElements(), "", true); //$NON-NLS-1$
				OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
				action.run();
			} else if (index==IDX_ADDJAR) {
				libentries= openJarFileDialog(null);
			}else if (index==IDX_ADDEXT) {
				libentries= openExtJarFileDialog(null);
				
			}
			
			else if (index == IDX_EDIT) {
				editEntry();
			} else if (index == IDX_REMOVE) {
				removeEntry();
			}
		}
		
		if (libentries != null) {
			int nElementsChosen= libentries.length;					
			// remove duplicates
			List cplist= fClassPathList.getElements();
			List elementsToAdd= new ArrayList(nElementsChosen);
			
			for (int i= 0; i < nElementsChosen; i++) {
				CPListElement curr= libentries[i];
				if (!cplist.contains(curr) && !elementsToAdd.contains(curr)) {
					elementsToAdd.add(curr);
					curr.setAttribute(CPListElement.JAVADOC, BuildPathSupport.guessJavadocLocation(curr));
				}
			}
			refresh(elementsToAdd,null,null);
			
			fClassPathList.addElements(elementsToAdd);
			// || index == IDX_ADDVAR

			fClassPathList.postSetSelection(new StructuredSelection(libentries));
//			if (index == IDX_ADDJAR || index == IDX_ADDVAR || index==IDX_ADDEXT) {
//				fClassPathList.refresh();
//			}
		}
	}
	private CPListElement[] openJarFileDialog(CPListElement existing) {
		IWorkspaceRoot root= fCurrJProject.getProject().getWorkspace().getRoot();
		
		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseJAREntries(getShell(), fCurrJProject.getPath(), getUsedJARFiles(existing));
			if (selected != null) {
				ArrayList res= new ArrayList();
				
				for (int i= 0; i < selected.length; i++) {
					IPath curr= selected[i];
					IResource resource= root.findMember(curr);
					if (resource instanceof IFile) {
						res.add(newCPLibraryElement(resource));
					}
				}
				return (CPListElement[]) res.toArray(new CPListElement[res.size()]);
			}
		} else {
			IPath configured= BuildPathDialogAccess.configureJAREntry(getShell(), existing.getPath(), getUsedJARFiles(existing));
			if (configured != null) {
				IResource resource= root.findMember(configured);
				if (resource instanceof IFile) {
					return new CPListElement[] { newCPLibraryElement(resource) }; 
				}
			}
		}		
		return null;
	}
	
	
	private CPListElement[] openExtJarFileDialog(CPListElement existing) {
		CPListElement tempElem=null;
		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseExternalJAREntries(getShell());
			if (selected != null) {
				ArrayList res= new ArrayList();
				for (int i= 0; i < selected.length; i++) {
					tempElem = new CPListElement(fCurrJProject, IIncludePathEntry.CPE_LIBRARY, selected[i], null);
					tempElem.setExported(true);
					res.add(tempElem);
				}
				return (CPListElement[]) res.toArray(new CPListElement[res.size()]);
			}
		} else {
			IPath configured= BuildPathDialogAccess.configureExternalJAREntry(getShell(), existing.getPath());
			if (configured != null) {
				tempElem = new CPListElement(fCurrJProject, IIncludePathEntry.CPE_LIBRARY, configured, null);
				tempElem.setExported(true);
				return new CPListElement[] { tempElem };
			}
		}		
		return null;
	}
	
	private IPath[] getUsedJARFiles(CPListElement existing) {
		List res= new ArrayList();
		List cplist= fFoldersList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			// 
			if ( elem.getEntryKind() == IIncludePathEntry.CPE_LIBRARY && (elem != existing)) {
				IResource resource= elem.getResource();
				if (resource instanceof IFile) {
					res.add(resource.getFullPath());
				}else if (resource instanceof IPath) {
					res.add(resource);
				}
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}
	private CPListElement newCPLibraryElement(IResource res) {
		CPListElement elem= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_LIBRARY, res.getFullPath(), res);
		elem.setExported(true);
		return elem;
	}	
	
	private boolean hasFolders(IContainer container) {
		
		try {
			IResource[] members= container.members();
			for (int i= 0; i < members.length; i++) {
				if (members[i] instanceof IContainer) {
					return true;
				}
			}
		} catch (CoreException e) {
			// ignore
		}
		
		List elements= fFoldersList.getElements();
		if (elements.size() > 1)
			return true;
		
		if (elements.size() == 0)
			return false;
		
		CPListElement single= (CPListElement)elements.get(0);
		if (single.getPath().equals(fCurrJProject.getPath()))
			return false;
		
		return true;
	}

	private void editEntry() {
		List selElements= fFoldersList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (fFoldersList.getIndexOfElement(elem) != -1) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}

	private void editElementEntry(CPListElement elem) {
		CPListElement[] res= null;
		
		
		if(elem.getEntryKind()== IIncludePathEntry.CPE_CONTAINER) {
			res= openContainerSelectionDialog(elem);
		}else if(elem.getEntryKind()== IIncludePathEntry.CPE_LIBRARY) {
			IResource resource= elem.getResource();
			if (resource == null) {
				res= openExtJarFileDialog(elem);
			} else if (resource.getType() == IResource.FOLDER) {
				if (resource.exists()) {
					res= openClassFolderDialog(elem);
				} else {
					res= openNewClassFolderDialog(elem);
				} 
			} else if (resource.getType() == IResource.FILE) {
				res= openJarFileDialog(elem);			
			}
		}if (elem.getLinkTarget() != null) {
			AddSourceFolderWizard wizard= newLinkedSourceFolderWizard(elem, fFoldersList.getElements(), "", false); //$NON-NLS-1$
			OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
			action.run();
		} else {
			AddSourceFolderWizard wizard= newSourceFolderWizard(elem, fFoldersList.getElements(), "", false); //$NON-NLS-1$
			OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
			action.run();
		}
		
		if (res != null && res.length > 0) {
			CPListElement curr= res[0];
			curr.setExported(elem.isExported());
			curr.setAttributesFromExisting(elem);
			ArrayList removed = new ArrayList(0);
			ArrayList added = new ArrayList(0);
			removed.add(curr);
			added.add(elem);
			//fLibrariesList.replaceElement(elem, curr);
			refresh(added,removed,null);

		}
	}
	private CPListElement[] openNewClassFolderDialog(CPListElement existing) {
		String title= (existing == null) ? NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_new_title : NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_edit_title; 
		IProject currProject= fCurrJProject.getProject();
		
		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, currProject, getUsedContainers(existing), existing);
		IPath projpath= currProject.getFullPath();
		dialog.setMessage(Messages.format(NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_description, projpath.toString())); 
		if (dialog.open() == Window.OK) {
			IFolder folder= dialog.getFolder();
			return new CPListElement[] { newCPLibraryElement(folder) };
		}
		return null;
	}
	private IPath[] getUsedContainers(CPListElement existing) {
		ArrayList res= new ArrayList();	
			
		List cplist= fFoldersList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			if (elem.getEntryKind() == IIncludePathEntry.CPE_LIBRARY && (elem != existing)) {
				IResource resource= elem.getResource();
				if (resource instanceof IContainer && !resource.equals(existing)) {
					res.add(resource.getFullPath());
				}
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}
	private CPListElement[] openClassFolderDialog(CPListElement existing) {
		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseClassFolderEntries(getShell(), fCurrJProject.getPath(), getUsedContainers(existing));
			if (selected != null) {
				IWorkspaceRoot root= fCurrJProject.getProject().getWorkspace().getRoot();
				ArrayList res= new ArrayList();
				for (int i= 0; i < selected.length; i++) {
					IPath curr= selected[i];
					IResource resource= root.findMember(curr);
					if (resource instanceof IContainer) {
						res.add(newCPLibraryElement(resource));
					}
				}
				return (CPListElement[]) res.toArray(new CPListElement[res.size()]);
			}
		} else {
			// disabled
		}		
		return null;
	}
	
	private CPListElement[] openContainerSelectionDialog(CPListElement existing) {
		if (existing == null) {
			IIncludePathEntry[] created= BuildPathDialogAccess.chooseContainerEntries(getShell(), fCurrJProject, getRawClasspath());
			if (created != null) {
				CPListElement[] res= new CPListElement[created.length];
				for (int i= 0; i < res.length; i++) {
					res[i]= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_CONTAINER, created[i].getPath(), null);
				}
				return res;
			}
		} else {
			IIncludePathEntry created= BuildPathDialogAccess.configureContainerEntry(getShell(), existing.getClasspathEntry(), fCurrJProject, getRawClasspath());
			if (created != null) {
				CPListElement elem= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_CONTAINER, created.getPath(), null);
				return new CPListElement[] { elem };
			}
		}		
		return null;
	}
	
	private IIncludePathEntry[] getRawClasspath() {
		IIncludePathEntry[] currEntries= new IIncludePathEntry[fClassPathList.getSize()];
		for (int i= 0; i < currEntries.length; i++) {
			CPListElement curr= (CPListElement) fClassPathList.getElement(i);
			currEntries[i]= curr.getClasspathEntry();
		}
		return currEntries;
	}
	private void attributeUpdated(CPListElement selElement, String[] changedAttributes) {
		Object parentContainer= selElement.getParentContainer();
		if (parentContainer instanceof CPListElement) { // inside a container: apply changes right away
			IIncludePathEntry updatedEntry= selElement.getClasspathEntry();
			updateContainerEntry(updatedEntry, changedAttributes, fCurrJProject, ((CPListElement) parentContainer).getPath());
		}
	}
	
	private void updateContainerEntry(final IIncludePathEntry newEntry, final String[] changedAttributes, final IJavaScriptProject jproject, final IPath containerPath) {
		try {
			IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {				
					BuildPathSupport.modifyClasspathEntry(null, newEntry, changedAttributes, jproject, containerPath, monitor);
				}
			};
			PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(runnable));

		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.LibrariesWorkbookPage_configurecontainer_error_title; 
			String message= NewWizardMessages.LibrariesWorkbookPage_configurecontainer_error_message; 
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch (InterruptedException e) {
			// 
		}
	}
	
	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		
		CPListElement selElement= elem.getParent();
		
		if (key.equals(CPListElement.ACCESSRULES)) {
			AccessRulesDialog dialog= new AccessRulesDialog(getShell(), selElement, fCurrJProject, false);
			int res= dialog.open();
			if (res == Window.OK || res == AccessRulesDialog.SWITCH_PAGE) {
				selElement.setAttribute(CPListElement.ACCESSRULES, dialog.getAccessRules());
				String[] changedAttributes= { CPListElement.ACCESSRULES };
				attributeUpdated(selElement, changedAttributes);
				
				fFoldersList.refresh(elem);
				fClassPathList.dialogFieldChanged(); // validate
				updateEnabledState();
				
//				if (res == AccessRulesDialog.SWITCH_PAGE) { // switch after updates and validation
//					dialog.performPageSwitch(fPageContainer);
//				}
			}
//		} else 	if (key.equals(CPListElement.OUTPUT)) {
//			//CPListElement selElement=  elem.getParent();
//			OutputLocationDialog dialog= new OutputLocationDialog(getShell(), selElement, fClassPathList.getElements(), new Path("").makeAbsolute(), true); //$NON-NLS-1$
//			if (dialog.open() == Window.OK) {
//				selElement.setAttribute(CPListElement.OUTPUT, dialog.getOutputLocation());
//				fFoldersList.refresh();
//				fClassPathList.dialogFieldChanged(); // validate
//			}
		} else if (key.equals(CPListElement.EXCLUSION) || key.equals(CPListElement.INCLUSION)) {
			EditFilterWizard wizard= newEditFilterWizard(elem.getParent(), fFoldersList.getElements(), ""); //$NON-NLS-1$
			OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
			action.run();
		} else {
			if (editCustomAttribute(getShell(), elem)) {
				fFoldersList.refresh();
				fClassPathList.dialogFieldChanged(); // validate
			}
		}
	}

	protected void sourcePageSelectionChanged(DialogField field) {
		List selected= fFoldersList.getSelectedElements();
		fFoldersList.enableButton(IDX_EDIT, canEdit(selected));
		fFoldersList.enableButton(IDX_REMOVE, canRemove(selected));
		boolean noAttributes= containsOnlyTopLevelEntries(selected);
		fFoldersList.enableButton(IDX_ADD, noAttributes);
	}
	
	private void removeEntry() {
		List selElements= fFoldersList.getSelectedElements();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				if (attrib.isBuiltIn()) {
					Object value= null;
					if (key.equals(CPListElement.EXCLUSION) || key.equals(CPListElement.INCLUSION)) {
						value= new Path[0];
					}
					attrib.getParent().setAttribute(key, value);
				} else {
					removeCustomAttribute(attrib);
				}
				selElements.remove(i);
			}
		}
		if (selElements.isEmpty()) {
			fFoldersList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			for (Iterator iter= selElements.iterator(); iter.hasNext();) {
				CPListElement element= (CPListElement)iter.next();
				if (element.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
					List list= ClasspathModifier.removeFilters(element.getPath(), fCurrJProject, fFoldersList.getElements());
					for (Iterator iterator= list.iterator(); iterator.hasNext();) {
						CPListElement modified= (CPListElement)iterator.next();
						fFoldersList.refresh(modified);
						fFoldersList.expandElement(modified, 3);
					}
				}
			}
			fFoldersList.removeElements(selElements);
		}
	}
	
	private boolean canRemove(List selElements) {
		if (selElements.size() == 0) {
			return false;
		}
		for (int i= 0; i < selElements.size(); i++) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				if (attrib.isBuiltIn()) {
					if (CPListElement.INCLUSION.equals(key)) {
						if (((IPath[]) attrib.getValue()).length == 0) {
							return false;
						}
					} else if (CPListElement.EXCLUSION.equals(key)) {
						if (((IPath[]) attrib.getValue()).length == 0) {
							return false;
						}
					} else if (attrib.getValue() == null) {
						return false;
					}
				} else {
					if  (!canRemoveCustomAttribute(attrib)) {
						return false;
					}
				}
			} else if (elem instanceof CPListElement) {
				CPListElement curr= (CPListElement) elem;
				if (curr.getParentContainer() != null) {
					return false;
				}
			}
		}
		return true;
	}		
	
	private boolean canEdit(List selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			CPListElement cp= ((CPListElement)elem);
			if (cp.getPath().equals(cp.getJavaProject().getPath()))
				return false;
			
			return true;
		}
		if (elem instanceof CPListElementAttribute) {
			CPListElementAttribute attrib= (CPListElementAttribute) elem;
			if (attrib.isBuiltIn()) {
				return true;
			} else {
				return canEditCustomAttribute(attrib);
			}
		}
		return false;
	}	
	
	private void sourcePageDialogFieldChanged(DialogField field) {
		if (fCurrJProject == null) {
			// not initialized
			return;
		}
		
//		if (field == fUseFolderOutputs) {
//			if (!fUseFolderOutputs.isSelected()) {
//				int nFolders= fFoldersList.getSize();
//				for (int i= 0; i < nFolders; i++) {
//					CPListElement cpe= (CPListElement) fFoldersList.getElement(i);
//					cpe.setAttribute(CPListElement.OUTPUT, null);
//				}
//			}
//			fFoldersList.refresh();
//		} else 
			
			if (field == fFoldersList) {
			updateClasspathList();
		}
	}	
	
		
	private void updateClasspathList() {
		List srcelements= fFoldersList.getElements();
		
		List cpelements= fClassPathList.getElements();
		int nEntries= cpelements.size();
		// backwards, as entries will be deleted
		int lastRemovePos= nEntries;
		int afterLastSourcePos= 0;
		for (int i= nEntries - 1; i >= 0; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (isEntryKind(kind)) {
				if (!srcelements.remove(cpe)) {
					cpelements.remove(i);
					lastRemovePos= i;
				} else if (lastRemovePos == nEntries) {
					afterLastSourcePos= i + 1;
				}
			}
		}

		if (!srcelements.isEmpty()) {
			int insertPos= Math.min(afterLastSourcePos, lastRemovePos);
			cpelements.addAll(insertPos, srcelements);
		}
		
		//if (lastRemovePos != nEntries || !srcelements.isEmpty()) {
			fClassPathList.setElements(cpelements);
		//}
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fFoldersList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements, boolean expand) {
		fFoldersList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (int i= 0; i < selElements.size(); i++) {
				fFoldersList.expandElement(selElements.get(i), 1);
			}
		}
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		return (kind == IIncludePathEntry.CPE_SOURCE || kind==IIncludePathEntry.CPE_LIBRARY || kind==IIncludePathEntry.CPE_VARIABLE);
	}

	private void refresh(List insertedElements, List removedElements, List modifiedElements) {
		if (removedElements==null)
			removedElements=new ArrayList();
		if (modifiedElements==null)
			modifiedElements=new ArrayList();
		fFoldersList.addElements(insertedElements);
		for (Iterator iter= insertedElements.iterator(); iter.hasNext();) {
			CPListElement element= (CPListElement)iter.next();
			fFoldersList.expandElement(element, 3);
		}
		
		fFoldersList.removeElements(removedElements);
		
		for (Iterator iter= modifiedElements.iterator(); iter.hasNext();) {
			CPListElement element= (CPListElement)iter.next();
			fFoldersList.refresh(element);
			fFoldersList.expandElement(element, 3);
			fFoldersList.dialogFieldChanged();
		}
		
		fFoldersList.refresh(); //does enforce the order of the entries.
		if (!insertedElements.isEmpty()) {
			fFoldersList.postSetSelection(new StructuredSelection(insertedElements));
		}

		//fOutputLocationField.setText(outputLocation.makeRelative().toOSString());
	}
	private void updateEnabledState() {
		List selElements= fFoldersList.getSelectedElements();
		fFoldersList.enableButton(IDX_EDIT, canEdit(selElements));
		fFoldersList.enableButton(IDX_REMOVE, canRemove(selElements));
		//fFoldersList.enableButton(IDX_REPLACE, getSelectedPackageFragmentRoot() != null);
		
//		boolean noAttributes= containsOnlyTopLevelEntries(selElements);
		//fLibrariesList.enableButton(IDX_ADDEXT, noAttributes);
		//fLibrariesList.enableButton(IDX_ADDFOL, noAttributes);
		//fLibrariesList.enableButton(IDX_ADDJAR, noAttributes);
		//fFoldersList.enableButton(IDX_ADDLIB, noAttributes);
		//fLibrariesList.enableButton(IDX_ADDVAR, noAttributes);
	}
	/**
     * {@inheritDoc}
     */
    public void setFocus() {
    	fFoldersList.setFocus();
    }	

}
