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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.TreeListDialogField;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;

public class LibrariesWorkbookPage extends BuildPathBasePage {
	
	private ListDialogField fClassPathList;
	private IJavaScriptProject fCurrJProject;
	
	private TreeListDialogField fLibrariesList;
	
	private Control fSWTControl;
	private final IWorkbenchPreferenceContainer fPageContainer;

	//private final int IDX_ADDJAR= 0;
	//private final int IDX_ADDEXT= 1;
	//private final int IDX_ADDVAR= 2;
	private final int IDX_ADDLIB= 0;
	private final int IDX_ADDFOL= 1;
	
	private final int IDX_EDIT= 3;
	private final int IDX_REMOVE= 4;

	//private final int IDX_REPLACE= 9;
		
	public LibrariesWorkbookPage(CheckedListDialogField classPathList, IWorkbenchPreferenceContainer pageContainer) {
		fClassPathList= classPathList;
		fPageContainer= pageContainer;
		fSWTControl= null;
		
		String[] buttonLabels= new String[] { 
			//NewWizardMessages.LibrariesWorkbookPage_libraries_addjar_button,	
			//NewWizardMessages.LibrariesWorkbookPage_libraries_addextjar_button, 
			//NewWizardMessages.LibrariesWorkbookPage_libraries_addvariable_button, 
			NewWizardMessages.LibrariesWorkbookPage_libraries_addlibrary_button, 
			NewWizardMessages.LibrariesWorkbookPage_libraries_addclassfolder_button, 
			/* */ null,  
			NewWizardMessages.LibrariesWorkbookPage_libraries_edit_button, 
			NewWizardMessages.LibrariesWorkbookPage_libraries_remove_button,
			/* */ 
			//NewWizardMessages.LibrariesWorkbookPage_libraries_replace_button
		};		
				
		LibrariesAdapter adapter= new LibrariesAdapter();
				
		fLibrariesList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fLibrariesList.setDialogFieldListener(adapter);
		fLibrariesList.setLabelText(NewWizardMessages.LibrariesWorkbookPage_libraries_label); 
		fLibrariesList.enableButton(IDX_EDIT, false);
		fLibrariesList.enableButton(IDX_REMOVE, false);
		
		//fLibrariesList.enableButton(IDX_REPLACE, false);

		fLibrariesList.setViewerComparator(new CPListElementSorter());

	}
		
	public void init(IJavaScriptProject jproject) {
		fCurrJProject= jproject;
		if (Display.getCurrent() != null) {
			updateLibrariesList();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					updateLibrariesList();
				}
			});
		}
	}
	
	private void updateLibrariesList() {
		List cpelements= fClassPathList.getElements();
		List libelements= new ArrayList(cpelements.size());
		
		int nElements= cpelements.size();
		for (int i= 0; i < nElements; i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (isEntryKind(cpe.getEntryKind())) {
				libelements.add(cpe);
			}
		}
		fLibrariesList.setElements(libelements);
	}		
		
	// -------- UI creation
	
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibrariesList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fLibrariesList.getTreeControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fLibrariesList.setButtonsMinWidth(buttonBarWidth);
		
		fLibrariesList.setViewerComparator(new CPListElementSorter());
		
		fSWTControl= composite;
				
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaScriptPlugin.getActiveWorkbenchShell();
	}
	
	
	private class LibrariesAdapter implements IDialogFieldListener, ITreeListAdapter {
		
		private final Object[] EMPTY_ARR= new Object[0];
		
		// -------- IListAdapter --------
		public void customButtonPressed(TreeListDialogField field, int index) {
			libaryPageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(TreeListDialogField field) {
			libaryPageSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			libaryPageDoubleClicked(field);
		}
		
		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			libaryPageKeyPressed(field, event);
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren();
			} else if (element instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute) element;
				if (CPListElement.ACCESSRULES.equals(attribute.getKey())) {
					return (IAccessRule[]) attribute.getValue();
				}
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
			return getChildren(field, element).length > 0;
		}		
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			libaryPageDialogFieldChanged(field);
		}
	}
	
	private void libaryPageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] libentries= null;
		switch (index) {
//		case IDX_ADDJAR: /* add jar */
//			libentries= openJarFileDialog(null);
//			break;
//		case IDX_ADDEXT: /* add external jar */
//			libentries= openExtJarFileDialog(null);
//			break;
//		case IDX_ADDVAR: /* add variable */
//			libentries= openVariableSelectionDialog(null);
//			break;
		case IDX_ADDLIB: /* add library */
			libentries= openContainerSelectionDialog(null);
			break;
		case IDX_ADDFOL: /* add folder */
			libentries= openClassFolderDialog(null);
			break;			
		case IDX_EDIT: /* edit */
			editEntry();
			return;
		case IDX_REMOVE: /* remove */
			removeEntry();
			return;
//		case IDX_REPLACE: /* replace */
//			replaceJarFile();
//			return;
		}
		if (libentries != null) {
			int nElementsChosen= libentries.length;					
			// remove duplicates
			List cplist= fLibrariesList.getElements();
			List elementsToAdd= new ArrayList(nElementsChosen);
			
			for (int i= 0; i < nElementsChosen; i++) {
				CPListElement curr= libentries[i];
				if (!cplist.contains(curr) && !elementsToAdd.contains(curr)) {
					elementsToAdd.add(curr);
					curr.setAttribute(CPListElement.JAVADOC, BuildPathSupport.guessJavadocLocation(curr));
				}
			}
//			if (!elementsToAdd.isEmpty() && (index == IDX_ADDFOL)) {
//				askForAddingExclusionPatternsDialog(elementsToAdd);
//			}
			
			fLibrariesList.addElements(elementsToAdd);
			// || index == IDX_ADDVAR
			if (index == IDX_ADDLIB ) {
				fLibrariesList.refresh();
			}
			fLibrariesList.postSetSelection(new StructuredSelection(libentries));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#addElement(org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement)
	 */
	public void addElement(CPListElement element) {
		fLibrariesList.addElement(element);
		fLibrariesList.postSetSelection(new StructuredSelection(element));
	}
	
//	private void askForAddingExclusionPatternsDialog(List newEntries) {
//		HashSet modified= new HashSet();
//		List existing= fClassPathList.getElements();
//		fixNestingConflicts((CPListElement[])newEntries.toArray(new CPListElement[newEntries.size()]), (CPListElement[])existing.toArray(new CPListElement[existing.size()]), modified);
//		if (!modified.isEmpty()) {
//			String title= NewWizardMessages.LibrariesWorkbookPage_exclusion_added_title; 
//			String message= NewWizardMessages.LibrariesWorkbookPage_exclusion_added_message; 
//			MessageDialog.openInformation(getShell(), title, message);
//		}
//	}
	
	protected void libaryPageDoubleClicked(TreeListDialogField field) {
		List selection= fLibrariesList.getSelectedElements();
		if (canEdit(selection)) {
			editEntry();
		}
	}

	protected void libaryPageKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (field == fLibrariesList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}	
	}	

//	private void replaceJarFile() {
//		final IPackageFragmentRoot root= getSelectedPackageFragmentRoot();
//		if (root != null) {
//			final IImportWizard wizard= new JarImportWizard(false);
//			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(root));
//			final WizardDialog dialog= new WizardDialog(getShell(), wizard);
//			dialog.create();
//			dialog.getShell().setSize(Math.max(JarImportWizardAction.SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), JarImportWizardAction.SIZING_WIZARD_HEIGHT);
//			PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IJavaHelpContextIds.JARIMPORT_WIZARD_PAGE);
//			dialog.open();
//		}
//	}

//	private IPackageFragmentRoot getSelectedPackageFragmentRoot() {
//		final List elements= fLibrariesList.getSelectedElements();
//		if (elements.size() == 1) {
//			final Object object= elements.get(0);
//			if (object instanceof CPListElement) {
//				final CPListElement element= (CPListElement) object;
//				final IIncludePathEntry entry= element.getClasspathEntry();
//				if (JarImportWizard.isValidClassPathEntry(entry)) {
//					final IJavaScriptProject project= element.getJavaProject();
//					if (project != null) {
//						try {
//							final IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
//							for (int index= 0; index < roots.length; index++) {
//								if (entry.equals(roots[index].getRawClasspathEntry()))
//									return roots[index];
//							}
//						} catch (JavaScriptModelException exception) {
//							JavaScriptPlugin.log(exception);
//						}
//					}
//				}
//			}
//		}
//		return null;
//	}

	private void removeEntry() {
		List selElements= fLibrariesList.getSelectedElements();
		HashMap containerEntriesToUpdate= new HashMap();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				if (attrib.isBuiltIn()) {
					Object value= null;
					if (key.equals(CPListElement.ACCESSRULES)) {
						value= new IAccessRule[0];
					}
					attrib.setValue(value);
				}else {
					removeCustomAttribute(attrib);
				}
				selElements.remove(i);
				if (attrib.getParent().getParentContainer() instanceof CPListElement) { // inside a container: apply changes right away
					CPListElement containerEntry= attrib.getParent();
					HashSet changedAttributes= (HashSet) containerEntriesToUpdate.get(containerEntry);
					if (changedAttributes == null) {
						changedAttributes= new HashSet();
						containerEntriesToUpdate.put(containerEntry, changedAttributes);
					}
					changedAttributes.add(key); // collect the changed attributes
				}
			}else if(elem instanceof  CPListElement) {
				CPListElement listElem = (CPListElement)elem;
				if (listElem.getEntryKind()==IIncludePathEntry.CPE_CONTAINER)
				{
					JsGlobalScopeContainerInitializer init = listElem.getContainerInitializer();
					init.removeFromProject(fCurrJProject);
				}
				
				
			}
		
		}
		if (selElements.isEmpty()) {
			fLibrariesList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			fLibrariesList.removeElements(selElements);
		}
		for (Iterator iter= containerEntriesToUpdate.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry= (Entry) iter.next();
			CPListElement curr= (CPListElement) entry.getKey();
			HashSet attribs= (HashSet) entry.getValue();
			String[] changedAttributes= (String[]) attribs.toArray(new String[attribs.size()]);
			IIncludePathEntry changedEntry= curr.getClasspathEntry();
			updateContainerEntry(changedEntry, changedAttributes, fCurrJProject, ((CPListElement) curr.getParentContainer()).getPath());
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
				if (attrib.isNonModifiable()) {
					return false;
				}
				if (attrib.isBuiltIn()) {
					if (attrib.getParent().isInContainer(JavaRuntime.JRE_CONTAINER) && CPListElement.ACCESSRULES.equals(attrib.getKey())) {
						return false; // workaround for 166519 until we have full story
					}
					if (attrib.getKey().equals(CPListElement.ACCESSRULES)) {
						return ((IAccessRule[]) attrib.getValue()).length > 0;
					}
					if (attrib.getValue() == null) {
						return false;
					}
				} else {
					if (!canRemoveCustomAttribute(attrib)) {
						return false;
					}
				}
			} else if (elem instanceof CPListElement) {
				CPListElement curr= (CPListElement) elem;
				if (curr.getEntryKind()==IIncludePathEntry.CPE_CONTAINER)
					return !curr.isInNonModifiableContainer();
				if (curr.getParentContainer() != null) {
					return false;
				}
			} else { // unknown element
				return false;
			}
		}		
		return true;
	}	

	/**
	 * Method editEntry.
	 */
	private void editEntry() {
		List selElements= fLibrariesList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (fLibrariesList.getIndexOfElement(elem) != -1) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}
	
	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		CPListElement selElement= elem.getParent();
		
		if (key.equals(CPListElement.ACCESSRULES)) {
			AccessRulesDialog dialog= new AccessRulesDialog(getShell(), selElement, fCurrJProject, fPageContainer != null);
			int res= dialog.open();
			if (res == Window.OK || res == AccessRulesDialog.SWITCH_PAGE) {
				selElement.setAttribute(CPListElement.ACCESSRULES, dialog.getAccessRules());
				String[] changedAttributes= { CPListElement.ACCESSRULES };
				attributeUpdated(selElement, changedAttributes);
				
				fLibrariesList.refresh(elem);
				fClassPathList.dialogFieldChanged(); // validate
				updateEnabledState();
				
				if (res == AccessRulesDialog.SWITCH_PAGE) { // switch after updates and validation
					dialog.performPageSwitch(fPageContainer);
				}
			}
		} else {
			if (editCustomAttribute(getShell(), elem)) {
				String[] changedAttributes= { key };
				attributeUpdated(selElement, changedAttributes);
				fLibrariesList.refresh(elem);
				fClassPathList.dialogFieldChanged(); // validate
				updateEnabledState();
			}
		}
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
		
	private void editElementEntry(CPListElement elem) {
		CPListElement[] res= null;
		
		switch (elem.getEntryKind()) {
		case IIncludePathEntry.CPE_CONTAINER:
			res= openContainerSelectionDialog(elem);
			break;
		case IIncludePathEntry.CPE_LIBRARY:
			IResource resource= elem.getResource();
//			if (resource == null) {
//				res= openExtJarFileDialog(elem);
//			} else 
				if (resource.getType() == IResource.FOLDER) {
				if (resource.exists()) {
					res= openClassFolderDialog(elem);
				} else {
					//res= openNewClassFolderDialog(elem);
				} 
//			} else if (resource.getType() == IResource.FILE) {
//				res= openJarFileDialog(elem);			
			}
			break;
//		case IIncludePathEntry.CPE_VARIABLE:
//			res= openVariableSelectionDialog(elem);
//			break;
		}
		if (res != null && res.length > 0) {
			CPListElement curr= res[0];
			curr.setExported(elem.isExported());
			curr.setAttributesFromExisting(elem);
			fLibrariesList.replaceElement(elem, curr);
			if (elem.getEntryKind() == IIncludePathEntry.CPE_VARIABLE) {
				fLibrariesList.refresh();
			}
		}		
			
	}

	private void libaryPageSelectionChanged(DialogField field) {
		updateEnabledState();
	}

	private void updateEnabledState() {
		List selElements= fLibrariesList.getSelectedElements();
		fLibrariesList.enableButton(IDX_EDIT, canEdit(selElements));
		fLibrariesList.enableButton(IDX_REMOVE, canRemove(selElements));
		//fLibrariesList.enableButton(IDX_REPLACE, getSelectedPackageFragmentRoot() != null);
		
		boolean noAttributes= containsOnlyTopLevelEntries(selElements);
		//fLibrariesList.enableButton(IDX_ADDEXT, noAttributes);
		//fLibrariesList.enableButton(IDX_ADDFOL, noAttributes);
		//fLibrariesList.enableButton(IDX_ADDJAR, noAttributes);
		fLibrariesList.enableButton(IDX_ADDLIB, noAttributes);
		//fLibrariesList.enableButton(IDX_ADDVAR, noAttributes);
	}
	
	private boolean canEdit(List selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			CPListElement curr= (CPListElement) elem;
			if (curr.getEntryKind()==IIncludePathEntry.CPE_CONTAINER)
				return !curr.isInNonModifiableContainer();
			 return !(curr.getResource() instanceof IFolder) && curr.getParentContainer() == null;
		}
		if (elem instanceof CPListElementAttribute) {
			CPListElementAttribute attrib= (CPListElementAttribute) elem;
//			if (attrib.isInNonModifiableContainer()) {
//				return false;
//			}
			
			
//			if (attrib.getParent().isInContainer(JavaRuntime.JRE_CONTAINER) && CPListElement.ACCESSRULES.equals(attrib.getKey())) {
//				return false; // workaround for 166519 until we have full story
//			}
//			
//			if(CPListElement.ACCESSRULES.equals(attrib.getKey()){
//				return true;
//			}
			
			if (!attrib.isBuiltIn()) {
				return canEditCustomAttribute(attrib);
			}
			return true;
		}
		return false;
	}
	
	private void libaryPageDialogFieldChanged(DialogField field) {
		if (fCurrJProject != null) {
			// already initialized
			updateClasspathList();
		}
	}	
		
	private void updateClasspathList() {
		List projelements= fLibrariesList.getElements();
		
		List cpelements= fClassPathList.getElements();
		int nEntries= cpelements.size();
		// backwards, as entries will be deleted
		int lastRemovePos= nEntries;
		for (int i= nEntries - 1; i >= 0; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (isEntryKind(kind)) {
				if (!projelements.remove(cpe)) {
					cpelements.remove(i);
					lastRemovePos= i;
				}	
			}
		}
		
		cpelements.addAll(lastRemovePos, projelements);

		if (lastRemovePos != nEntries || !projelements.isEmpty()) {
			fClassPathList.setElements(cpelements);
		}
	}
	
		
//	private CPListElement[] openNewClassFolderDialog(CPListElement existing) {
//		String title= (existing == null) ? NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_new_title : NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_edit_title; 
//		IProject currProject= fCurrJProject.getProject();
//		
//		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, currProject, getUsedContainers(existing), existing);
//		IPath projpath= currProject.getFullPath();
//		dialog.setMessage(Messages.format(NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_description, projpath.toString())); 
//		if (dialog.open() == Window.OK) {
//			IFolder folder= dialog.getFolder();
//			return new CPListElement[] { newCPLibraryElement(folder) };
//		}
//		return null;
//	}
			
			
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
	
//	private CPListElement[] openJarFileDialog(CPListElement existing) {
//		IWorkspaceRoot root= fCurrJProject.getProject().getWorkspace().getRoot();
//		
//		if (existing == null) {
//			IPath[] selected= BuildPathDialogAccess.chooseJAREntries(getShell(), fCurrJProject.getPath(), getUsedJARFiles(existing));
//			if (selected != null) {
//				ArrayList res= new ArrayList();
//				
//				for (int i= 0; i < selected.length; i++) {
//					IPath curr= selected[i];
//					IResource resource= root.findMember(curr);
//					if (resource instanceof IFile) {
//						res.add(newCPLibraryElement(resource));
//					}
//				}
//				return (CPListElement[]) res.toArray(new CPListElement[res.size()]);
//			}
//		} else {
//			IPath configured= BuildPathDialogAccess.configureJAREntry(getShell(), existing.getPath(), getUsedJARFiles(existing));
//			if (configured != null) {
//				IResource resource= root.findMember(configured);
//				if (resource instanceof IFile) {
//					return new CPListElement[] { newCPLibraryElement(resource) }; 
//				}
//			}
//		}		
//		return null;
//	}
	
	private IPath[] getUsedContainers(CPListElement existing) {
		ArrayList res= new ArrayList();
			
		List cplist= fLibrariesList.getElements();
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
	
//	private IPath[] getUsedJARFiles(CPListElement existing) {
//		List res= new ArrayList();
//		List cplist= fLibrariesList.getElements();
//		for (int i= 0; i < cplist.size(); i++) {
//			CPListElement elem= (CPListElement)cplist.get(i);
//			if (elem.getEntryKind() == IIncludePathEntry.CPE_LIBRARY && (elem != existing)) {
//				IResource resource= elem.getResource();
//				if (resource instanceof IFile) {
//					res.add(resource.getFullPath());
//				}
//			}
//		}
//		return (IPath[]) res.toArray(new IPath[res.size()]);
//	}	
	
	private CPListElement newCPLibraryElement(IResource res) {
		return new CPListElement(fCurrJProject, IIncludePathEntry.CPE_LIBRARY, res.getFullPath(), res);
	}

//	private CPListElement[] openExtJarFileDialog(CPListElement existing) {
//		if (existing == null) {
//			IPath[] selected= BuildPathDialogAccess.chooseExternalJAREntries(getShell());
//			if (selected != null) {
//				ArrayList res= new ArrayList();
//				for (int i= 0; i < selected.length; i++) {
//					res.add(new CPListElement(fCurrJProject, IIncludePathEntry.CPE_LIBRARY, selected[i], null));
//				}
//				return (CPListElement[]) res.toArray(new CPListElement[res.size()]);
//			}
//		} else {
//			IPath configured= BuildPathDialogAccess.configureExternalJAREntry(getShell(), existing.getPath());
//			if (configured != null) {
//				return new CPListElement[] { new CPListElement(fCurrJProject, IIncludePathEntry.CPE_LIBRARY, configured, null) };
//			}
//		}		
//		return null;
//	}
		
//	private CPListElement[] openVariableSelectionDialog(CPListElement existing) {
//		List existingElements= fLibrariesList.getElements();
//		ArrayList existingPaths= new ArrayList(existingElements.size());
//		for (int i= 0; i < existingElements.size(); i++) {
//			CPListElement elem= (CPListElement) existingElements.get(i);
//			if (elem.getEntryKind() == IIncludePathEntry.CPE_VARIABLE) {
//				existingPaths.add(elem.getPath());
//			}
//		}
//		IPath[] existingPathsArray= (IPath[]) existingPaths.toArray(new IPath[existingPaths.size()]);
//		
//		if (existing == null) {
//			IPath[] paths= BuildPathDialogAccess.chooseVariableEntries(getShell(), existingPathsArray);
//			if (paths != null) {
//				ArrayList result= new ArrayList();
//				for (int i = 0; i < paths.length; i++) {
//					IPath path= paths[i];
//					CPListElement elem= createCPVariableElement(path);
//					if (!existingElements.contains(elem)) {
//						result.add(elem);
//					}
//				}
//				return (CPListElement[]) result.toArray(new CPListElement[result.size()]);
//			}
//		} else {
//			IPath path= BuildPathDialogAccess.configureVariableEntry(getShell(), existing.getPath(), existingPathsArray);
//			if (path != null) {
//				return new CPListElement[] { createCPVariableElement(path) };
//			}
//		}
//		return null;
//	}

//	private CPListElement createCPVariableElement(IPath path) {
//		CPListElement elem= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_VARIABLE, path, null);
//		IPath resolvedPath= JavaScriptCore.getResolvedVariablePath(path);
//		elem.setIsMissing((resolvedPath == null) || !resolvedPath.toFile().exists());
//		return elem;
//	}

	private CPListElement[] openContainerSelectionDialog(CPListElement existing) {
		if (existing == null) {
			IIncludePathEntry[] created= BuildPathDialogAccess.chooseContainerEntries(getShell(), fCurrJProject, getRawClasspath());
			if (created != null) {
				CPListElement[] res= new CPListElement[created.length];
				for (int i= 0; i < res.length; i++) {
					//res[i]= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_CONTAINER, created[i].getPath(), null);
					res[i]= new CPListElement(fCurrJProject, created[i].getEntryKind(), created[i].getPath(), null);
				}
				return res;
			}
		} else {
			IIncludePathEntry created= BuildPathDialogAccess.configureContainerEntry(getShell(), existing.getClasspathEntry(), fCurrJProject, getRawClasspath());
			if (created != null) {
				//CPListElement elem= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_CONTAINER, created.getPath(), null);
				CPListElement elem= new CPListElement(fCurrJProject, created.getEntryKind(), created.getPath(), null);
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
		
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		//return true;
		return kind == IIncludePathEntry.CPE_LIBRARY || kind == IIncludePathEntry.CPE_VARIABLE || kind == IIncludePathEntry.CPE_CONTAINER ;
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fLibrariesList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements, boolean expand) {
		fLibrariesList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (int i= 0; i < selElements.size(); i++) {
				fLibrariesList.expandElement(selElements.get(i), 1);
			}
		}
	}

	/**
     * {@inheritDoc}
     */
    public void setFocus() {
    	fLibrariesList.setFocus();
    }	

}
