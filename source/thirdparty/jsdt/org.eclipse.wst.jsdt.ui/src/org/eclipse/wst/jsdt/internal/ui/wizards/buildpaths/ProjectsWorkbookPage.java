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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.TreeListDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;


public class ProjectsWorkbookPage extends BuildPathBasePage {
	
	private final int IDX_ADDPROJECT= 0;
	private final int IDX_EDIT= 1;
	private final int IDX_REMOVE= 2;
	
	private ListDialogField fClassPathList;
	private IJavaScriptProject fCurrJProject;
	
	private TreeListDialogField fProjectsList;
	
	private Control fSWTControl;

	private final IWorkbenchPreferenceContainer fPageContainer;
	
	public ProjectsWorkbookPage(ListDialogField classPathList, IWorkbenchPreferenceContainer pageContainer) {
		fClassPathList= classPathList;
		fPageContainer= pageContainer;
		fSWTControl= null;	
		
		String[] buttonLabels= new String[] {
			NewWizardMessages.ProjectsWorkbookPage_projects_add_button,
			NewWizardMessages.ProjectsWorkbookPage_projects_edit_button, 
			NewWizardMessages.ProjectsWorkbookPage_projects_remove_button
		};
		
		ProjectsAdapter adapter= new ProjectsAdapter();
		
		fProjectsList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fProjectsList.setDialogFieldListener(adapter);
		fProjectsList.setLabelText(NewWizardMessages.ProjectsWorkbookPage_projects_label); 
		
		fProjectsList.enableButton(IDX_REMOVE, false);
		fProjectsList.enableButton(IDX_EDIT, false);
		
		fProjectsList.setViewerComparator(new CPListElementSorter());
	}
	
	public void init(final IJavaScriptProject jproject) {
		fCurrJProject= jproject;
		
		if (Display.getCurrent() != null) {
			updateProjectsList(jproject);
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					updateProjectsList(jproject);
				}
			});
		}
	}
		
	private void updateProjectsList(IJavaScriptProject currJProject) {
		// add the projects-cpentries that are already on the class path
		List cpelements= fClassPathList.getElements();
		
		final List checkedProjects= new ArrayList(cpelements.size());
		
		for (int i= cpelements.size() - 1 ; i >= 0; i--) {
			CPListElement cpelem= (CPListElement)cpelements.get(i);
			if (isEntryKind(cpelem.getEntryKind())) {
				checkedProjects.add(cpelem);
			}
		}
		fProjectsList.setElements(checkedProjects);
	}		
		
	// -------- UI creation ---------
		
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fProjectsList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fProjectsList.getTreeControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fProjectsList.setButtonsMinWidth(buttonBarWidth);
		
		fSWTControl= composite;
				
		return composite;
	}
		
	private void updateClasspathList() {
		List projelements= fProjectsList.getElements();
		
		boolean remove= false;
		List cpelements= fClassPathList.getElements();
		// backwards, as entries will be deleted
		for (int i= cpelements.size() -1; i >= 0 ; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (isEntryKind(cpe.getEntryKind())) {
				if (!projelements.remove(cpe)) {
					cpelements.remove(i);
					remove= true;
				}	
			}
		}
		for (int i= 0; i < projelements.size(); i++) {
			cpelements.add(projelements.get(i));
		}
		if (remove || (projelements.size() > 0)) {
			fClassPathList.setElements(cpelements);
		}
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fProjectsList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements, boolean expand) {
		fProjectsList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (int i= 0; i < selElements.size(); i++) {
				fProjectsList.expandElement(selElements.get(i), 1);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		return kind == IIncludePathEntry.CPE_PROJECT;
	}


	private class ProjectsAdapter implements IDialogFieldListener, ITreeListAdapter {
		
		private final Object[] EMPTY_ARR= new Object[0];
		
		// -------- IListAdapter --------
		public void customButtonPressed(TreeListDialogField field, int index) {
			projectPageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(TreeListDialogField field) {
			projectPageSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			projectPageDoubleClicked(field);
		}
		
		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			projectPageKeyPressed(field, event);
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
			return getChildren(field, element).length > 0;
		}		
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			projectPageDialogFieldChanged(field);
		}
	}
	
	private void projectPageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] entries= null;
		switch (index) {
		case IDX_ADDPROJECT: /* add project */
			entries= openProjectDialog(null);
			break;			
		case IDX_EDIT: /* edit */
			editEntry();
			return;
		case IDX_REMOVE: /* remove */
			removeEntry();
			return;			
		}
		if (entries != null) {
			int nElementsChosen= entries.length;					
			// remove duplicates
			List cplist= fProjectsList.getElements();
			List elementsToAdd= new ArrayList(nElementsChosen);
			for (int i= 0; i < nElementsChosen; i++) {
				CPListElement curr= entries[i];
				if (!cplist.contains(curr) && !elementsToAdd.contains(curr)) {
					elementsToAdd.add(curr);
				}
			}
						
			fProjectsList.addElements(elementsToAdd);
			if (index == IDX_ADDPROJECT) {
				fProjectsList.refresh();
			}
			fProjectsList.postSetSelection(new StructuredSelection(entries));
		}
	}
	
	private void removeEntry() {
		List selElements= fProjectsList.getSelectedElements();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				if (attrib.isBuiltIn()) {
					String key= attrib.getKey();
					Object value= null;
					if (key.equals(CPListElement.ACCESSRULES)) {
						value= new IAccessRule[0];
					}
					attrib.getParent().setAttribute(key, value);
				} else {
					removeCustomAttribute(attrib);
				}
				selElements.remove(i);
			}
		}
		if (selElements.isEmpty()) {
			fProjectsList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			fProjectsList.removeElements(selElements);
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
					if (CPListElement.ACCESSRULES.equals(attrib.getKey())) {
						if (((IAccessRule[]) attrib.getValue()).length == 0) {
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
			return false;
		}
		if (elem instanceof CPListElementAttribute) {
			CPListElementAttribute attrib= (CPListElementAttribute) elem;
			if (attrib.isNonModifiable()) {
				return false;
			}
			if (!attrib.isBuiltIn()) {
				return canEditCustomAttribute(attrib);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Method editEntry.
	 */
	private void editEntry() {
		List selElements= fProjectsList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (fProjectsList.getIndexOfElement(elem) != -1) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}
	
	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		if (key.equals(CPListElement.ACCESSRULES)) {
			showAccessRestrictionDialog(elem.getParent());
		} else {
			if (editCustomAttribute(getShell(), elem)) {
				fProjectsList.refresh();
				fClassPathList.dialogFieldChanged(); // validate
			}
		}
	}
	
	private void showAccessRestrictionDialog(CPListElement selElement) {
		AccessRulesDialog dialog= new AccessRulesDialog(getShell(), selElement, fCurrJProject, fPageContainer != null);
		int res= dialog.open();
		if (res == Window.OK || res == AccessRulesDialog.SWITCH_PAGE) {
			selElement.setAttribute(CPListElement.ACCESSRULES, dialog.getAccessRules());
			selElement.setAttribute(CPListElement.COMBINE_ACCESSRULES, new Boolean(dialog.doCombineAccessRules()));
			fProjectsList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
			
			if (res == AccessRulesDialog.SWITCH_PAGE) {
				dialog.performPageSwitch(fPageContainer);
			}
		}
	}
		
	private void editElementEntry(CPListElement elem) {
		CPListElement[] res= openProjectDialog(elem);
		if (res != null && res.length > 0) {
			CPListElement curr= res[0];
			curr.setExported(elem.isExported());
			fProjectsList.replaceElement(elem, curr);
		}		
			
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaScriptPlugin.getActiveWorkbenchShell();
	}


	private CPListElement[] openProjectDialog(CPListElement elem) {
		
		try {
			ArrayList selectable= new ArrayList();
			selectable.addAll(Arrays.asList(fCurrJProject.getJavaScriptModel().getJavaScriptProjects()));
			selectable.remove(fCurrJProject);
			
			List elements= fProjectsList.getElements();
			for (int i= 0; i < elements.size(); i++) {
				CPListElement curr= (CPListElement) elements.get(0);
				IJavaScriptProject proj= (IJavaScriptProject) JavaScriptCore.create(curr.getResource());
				selectable.remove(proj);
			}
			Object[] selectArr= selectable.toArray();
			new JavaScriptElementComparator().sort(null, selectArr);
					
			ListSelectionDialog dialog= new ListSelectionDialog(getShell(), Arrays.asList(selectArr), new ArrayContentProvider(), new JavaUILabelProvider(), NewWizardMessages.ProjectsWorkbookPage_chooseProjects_message); 
			dialog.setTitle(NewWizardMessages.ProjectsWorkbookPage_chooseProjects_title); 
			dialog.setHelpAvailable(false);
			if (dialog.open() == Window.OK) {
				Object[] result= dialog.getResult();
				CPListElement[] cpElements= new CPListElement[result.length];
				for (int i= 0; i < result.length; i++) {
					IJavaScriptProject curr= (IJavaScriptProject) result[i];
					cpElements[i]= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_PROJECT, curr.getPath(), curr.getResource());
				}
				return cpElements;
			}
		} catch (JavaScriptModelException e) {
			return null;
		}
		return null;
	}

	protected void projectPageDoubleClicked(TreeListDialogField field) {
		List selection= fProjectsList.getSelectedElements();
		if (canEdit(selection)) {
			editEntry();
		}
	}

	protected void projectPageKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (field == fProjectsList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}	
	}
	
	private void projectPageDialogFieldChanged(DialogField field) {
		if (fCurrJProject != null) {
			// already initialized
			updateClasspathList();
		}
	}
	
	private void projectPageSelectionChanged(DialogField field) {
		List selElements= fProjectsList.getSelectedElements();
		fProjectsList.enableButton(IDX_EDIT, canEdit(selElements));
		fProjectsList.enableButton(IDX_REMOVE, canRemove(selElements));
		
		boolean noAttributes= containsOnlyTopLevelEntries(selElements);
		fProjectsList.enableButton(IDX_ADDPROJECT, noAttributes);
	}

	/**
     * {@inheritDoc}
     */
    public void setFocus() {
    	fProjectsList.setFocus();
    }
}
