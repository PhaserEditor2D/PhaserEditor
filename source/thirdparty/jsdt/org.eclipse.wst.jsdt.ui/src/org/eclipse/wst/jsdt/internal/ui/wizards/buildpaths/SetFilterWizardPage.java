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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.ui.wizards.NewElementWizardPage;

public class SetFilterWizardPage extends NewElementWizardPage {
	
	private static final String PAGE_NAME= "SetFilterWizardPage"; //$NON-NLS-1$
	
	private ListDialogField fInclusionPatternList;
	private ListDialogField fExclusionPatternList;
	private CPListElement fCurrElement;
	private IProject fCurrProject;
	
	private IContainer fCurrSourceFolder;
	
	private static final int IDX_ADD= 0;
	private static final int IDX_ADD_MULTIPLE= 1;
	private static final int IDX_EDIT= 2;
	private static final int IDX_REMOVE= 4;

	private final ArrayList fExistingEntries;

	public SetFilterWizardPage(CPListElement entryToEdit, ArrayList existingEntries) {
		super(PAGE_NAME);
		fExistingEntries= existingEntries;
		
		setTitle(NewWizardMessages.ExclusionInclusionDialog_title); 
		setDescription(NewWizardMessages.ExclusionInclusionDialog_description2);
		
		fCurrElement= entryToEdit;
		fCurrProject= entryToEdit.getJavaProject().getProject();
		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		IResource res= root.findMember(entryToEdit.getPath());
		if (res instanceof IContainer) {
			fCurrSourceFolder= (IContainer) res;
		}	
		
		String excLabel= NewWizardMessages.ExclusionInclusionDialog_exclusion_pattern_label; 
		ImageDescriptor excDescriptor= JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB;
		String[] excButtonLabels= new String[] {
				NewWizardMessages.ExclusionInclusionDialog_exclusion_pattern_add, 
				NewWizardMessages.ExclusionInclusionDialog_exclusion_pattern_add_multiple, 
				NewWizardMessages.ExclusionInclusionDialog_exclusion_pattern_edit, 
				null,
				NewWizardMessages.ExclusionInclusionDialog_exclusion_pattern_remove
			};
		
		
		String incLabel= NewWizardMessages.ExclusionInclusionDialog_inclusion_pattern_label; 
		ImageDescriptor incDescriptor= JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB;
		String[] incButtonLabels= new String[] {
				NewWizardMessages.ExclusionInclusionDialog_inclusion_pattern_add, 
				NewWizardMessages.ExclusionInclusionDialog_inclusion_pattern_add_multiple, 
				NewWizardMessages.ExclusionInclusionDialog_inclusion_pattern_edit, 
				null,
				NewWizardMessages.ExclusionInclusionDialog_inclusion_pattern_remove
			};	
		
		fExclusionPatternList= createListContents(entryToEdit, CPListElement.EXCLUSION, excLabel, excDescriptor, excButtonLabels);
		fInclusionPatternList= createListContents(entryToEdit, CPListElement.INCLUSION, incLabel, incDescriptor, incButtonLabels);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite inner= new Composite(parent, SWT.NONE);
		inner.setFont(parent.getFont());
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fInclusionPatternList.doFillIntoGrid(inner, 3);
		LayoutUtil.setHorizontalSpan(fInclusionPatternList.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fInclusionPatternList.getListControl(null));
		
		fExclusionPatternList.doFillIntoGrid(inner, 3);
		LayoutUtil.setHorizontalSpan(fExclusionPatternList.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fExclusionPatternList.getListControl(null));	
		
		setControl(inner);
		Dialog.applyDialogFont(inner);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(inner, IJavaHelpContextIds.INCLUSION_EXCLUSION_WIZARD_PAGE);
	}
	
	
	private static class ExclusionInclusionLabelProvider extends LabelProvider {
		
		private Image fElementImage;

		public ExclusionInclusionLabelProvider(ImageDescriptor descriptor) {
			ImageDescriptorRegistry registry= JavaScriptPlugin.getImageDescriptorRegistry();
			fElementImage= registry.get(descriptor);
		}
		
		public Image getImage(Object element) {
			return fElementImage;
		}

		public String getText(Object element) {
			return (String) element;
		}

	}
	
	private ListDialogField createListContents(CPListElement entryToEdit, String key, String label, ImageDescriptor descriptor, String[] buttonLabels) {
		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();
		
		ListDialogField patternList= new ListDialogField(adapter, buttonLabels, new ExclusionInclusionLabelProvider(descriptor));
		patternList.setDialogFieldListener(adapter);
		patternList.setLabelText(label);
		patternList.enableButton(IDX_EDIT, false);
	
		IPath[] pattern= (IPath[]) entryToEdit.getAttribute(key);
		
		ArrayList elements= new ArrayList(pattern.length);
		for (int i= 0; i < pattern.length; i++) {
			String patternName= pattern[i].toString();
			if (patternName.length() > 0)
				elements.add(patternName);
		}
		patternList.setElements(elements);
		patternList.selectFirstElement();
		patternList.enableButton(IDX_ADD_MULTIPLE, fCurrSourceFolder != null);
		patternList.setViewerComparator(new ViewerComparator());
		return patternList;
	}
	
	protected void doCustomButtonPressed(ListDialogField field, int index) {
		if (index == IDX_ADD) {
			addEntry(field);
		} else if (index == IDX_EDIT) {
			editEntry(field);
		} else if (index == IDX_ADD_MULTIPLE) {
			addMultipleEntries(field);
		} else if (index == IDX_REMOVE) {
			field.removeElements(field.getSelectedElements());
		}
		updateStatus();
	}
	
	private void updateStatus() {
//		fCurrElement.setAttribute(CPListElement.INCLUSION, getInclusionPattern());
//		fCurrElement.setAttribute(CPListElement.EXCLUSION, getExclusionPattern());
//		StatusInfo statusInfo= new StatusInfo();
//		statusInfo.setOK();
//		updateStatus(statusInfo);
		
		fCurrElement.setAttribute(CPListElement.INCLUSION, getInclusionPattern());
		fCurrElement.setAttribute(CPListElement.EXCLUSION, getExclusionPattern());
		IJavaScriptModelStatus status= JavaScriptConventions.validateClasspath(fCurrElement.getJavaProject(), CPListElement.convertToClasspathEntries(fExistingEntries));
		if (!status.isOK()) {
			StatusInfo statusInfo= new StatusInfo();
			statusInfo.setError(status.getMessage());
			updateStatus(statusInfo);
		} else {
			StatusInfo statusInfo= new StatusInfo();
			statusInfo.setOK();
			updateStatus(statusInfo);
		}
	}

	protected void doDoubleClicked(ListDialogField field) {
		editEntry(field);
		updateStatus();
	}
	
	protected void doSelectionChanged(ListDialogField field) {
		List selected= field.getSelectedElements();
		field.enableButton(IDX_EDIT, canEdit(selected));
	}
        	
	private boolean canEdit(List selected) {
		return selected.size() == 1;
	}
	
	private void editEntry(ListDialogField field) {
		List selElements= field.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		List existing= field.getElements();
		String entry= (String) selElements.get(0);
		ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(), isExclusion(field), entry, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			field.replaceElement(entry, dialog.getExclusionPattern());
		}
	}
	
	private boolean isExclusion(ListDialogField field) {
		return field == fExclusionPatternList;
	}


	private void addEntry(ListDialogField field) {
		List existing= field.getElements();
		ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(), isExclusion(field), null, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			field.addElement(dialog.getExclusionPattern());
		}
	}	
	
	// -------- ExclusionPatternAdapter --------

	private class ExclusionPatternAdapter implements IListAdapter, IDialogFieldListener {
		/**
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		public void customButtonPressed(ListDialogField field, int index) {
			doCustomButtonPressed(field, index);
		}

		/**
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void selectionChanged(ListDialogField field) {
			doSelectionChanged(field);
		}
		/**
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void doubleClicked(ListDialogField field) {
			doDoubleClicked(field);
		}

		/**
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
		}
		
	}
	
	protected void doStatusLineUpdate() {
	}		
	
	protected void checkIfPatternValid() {
	}
	
	
	private IPath[] getPattern(ListDialogField field) {
		Object[] arr= field.getElements().toArray();
		Arrays.sort(arr);
		IPath[] res= new IPath[arr.length];
		for (int i= 0; i < res.length; i++) {
			res[i]= new Path((String) arr[i]);
		}
		return res;
	}
	
	public IPath[] getExclusionPattern() {
		return getPattern(fExclusionPatternList);
	}
	
	public IPath[] getInclusionPattern() {
		return getPattern(fInclusionPatternList);
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.EXCLUSION_PATTERN_DIALOG);
	}
	
	private void addMultipleEntries(ListDialogField field) {
		String title, message;
		if (isExclusion(field)) {
			title= NewWizardMessages.ExclusionInclusionDialog_ChooseExclusionPattern_title; 
			message= NewWizardMessages.ExclusionInclusionDialog_ChooseExclusionPattern_description; 
		} else {
			title= NewWizardMessages.ExclusionInclusionDialog_ChooseInclusionPattern_title; 
			message= NewWizardMessages.ExclusionInclusionDialog_ChooseInclusionPattern_description; 
		}
		
		IPath[] res= ExclusionInclusionEntryDialog.chooseExclusionPattern(getShell(), fCurrSourceFolder, title, message, null, true);
		if (res != null) {
			for (int i= 0; i < res.length; i++) {
				field.addElement(res[i].toString());
			}
		}
	}

}
