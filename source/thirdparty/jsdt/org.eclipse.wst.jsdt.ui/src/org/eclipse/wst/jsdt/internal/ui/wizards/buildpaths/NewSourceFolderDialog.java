/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;

public class NewSourceFolderDialog extends StatusDialog {
	
	private SelectionButtonDialogField fUseProjectButton;
	private SelectionButtonDialogField fUseFolderButton;
	
	private StringDialogField fContainerDialogField;
	private StatusInfo fContainerFieldStatus;
	
	private IContainer fFolder;
	private List fExistingFolders;
	private IProject fCurrProject;
		
	public NewSourceFolderDialog(Shell parent, String title, IProject project, List existingFolders, CPListElement entryToEdit) {
		super(parent);
		setTitle(title);
		
		fContainerFieldStatus= new StatusInfo();
	
		SourceContainerAdapter adapter= new SourceContainerAdapter();
		
		fUseProjectButton= new SelectionButtonDialogField(SWT.RADIO);
		fUseProjectButton.setLabelText(NewWizardMessages.NewSourceFolderDialog_useproject_button); 
		fUseProjectButton.setDialogFieldListener(adapter);

		fUseFolderButton= new SelectionButtonDialogField(SWT.RADIO);
		fUseFolderButton.setLabelText(NewWizardMessages.NewSourceFolderDialog_usefolder_button); 
		fUseFolderButton.setDialogFieldListener(adapter);		
		
		fContainerDialogField= new StringDialogField();
		fContainerDialogField.setDialogFieldListener(adapter);
		fContainerDialogField.setLabelText(NewWizardMessages.NewSourceFolderDialog_sourcefolder_label); 
		
		fUseFolderButton.attachDialogField(fContainerDialogField);
		
		fFolder= null;
		fExistingFolders= existingFolders;
		fCurrProject= project;
		
		boolean useFolders= true;
		if (entryToEdit == null) {
			fContainerDialogField.setText(""); //$NON-NLS-1$
		} else {
			IPath editPath= entryToEdit.getPath().removeFirstSegments(1);
			fContainerDialogField.setText(editPath.toString());
			useFolders= !editPath.isEmpty();
		}
		fUseFolderButton.setSelection(useFolders);
		fUseProjectButton.setSelection(!useFolders);
	}
	
	public void setMessage(String message) {
		fContainerDialogField.setLabelText(message);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		inner.setLayout(layout);
		
		int widthHint= convertWidthInCharsToPixels(50);
		
		
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= widthHint;
		
		if (fExistingFolders.contains(fCurrProject)) {
			fContainerDialogField.doFillIntoGrid(inner, 2);
		} else {
			fUseProjectButton.doFillIntoGrid(inner, 1);
			fUseFolderButton.doFillIntoGrid(inner, 1);
			fContainerDialogField.getTextControl(inner);

			int horizontalIndent= convertWidthInCharsToPixels(3);
			data.horizontalIndent= horizontalIndent;
		}
		Text text= fContainerDialogField.getTextControl(null);
		text.setLayoutData(data);
		TextFieldNavigationHandler.install(text);
				
		fContainerDialogField.postSetFocusOnDialogField(parent.getDisplay());
		applyDialogFont(composite);		
		return composite;
	}

		
	// -------- SourceContainerAdapter --------

	private class SourceContainerAdapter implements IDialogFieldListener {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}
	}
	
	protected void doStatusLineUpdate() {
		checkIfPathValid();
		updateStatus(fContainerFieldStatus);
	}		
	
	protected void checkIfPathValid() {
		fFolder= null;
		IContainer folder= null;
		if (fUseFolderButton.isSelected()) {
			String pathStr= fContainerDialogField.getText();
			if (pathStr.length() == 0) {
				fContainerFieldStatus.setError(NewWizardMessages.NewSourceFolderDialog_error_enterpath); 
				return;
			}
			IPath path= fCurrProject.getFullPath().append(pathStr);
			IWorkspace workspace= fCurrProject.getWorkspace();
			
			IStatus pathValidation= workspace.validatePath(path.toString(), IResource.FOLDER);
			if (!pathValidation.isOK()) {
				fContainerFieldStatus.setError(Messages.format(NewWizardMessages.NewSourceFolderDialog_error_invalidpath, pathValidation.getMessage())); 
				return;
			}
			folder= fCurrProject.getFolder(pathStr);
		} else {
			folder= fCurrProject; 
		}
		if (isExisting(folder)) {
			fContainerFieldStatus.setError(NewWizardMessages.NewSourceFolderDialog_error_pathexists); 
			return;
		}
		fContainerFieldStatus.setOK();
		fFolder= folder;
	}
	
	private boolean isExisting(IContainer folder) {
		return fExistingFolders.contains(folder);
	}
		
	
		
	public IContainer getSourceFolder() {
		return fFolder;
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.NEW_CONTAINER_DIALOG);
	}


}
