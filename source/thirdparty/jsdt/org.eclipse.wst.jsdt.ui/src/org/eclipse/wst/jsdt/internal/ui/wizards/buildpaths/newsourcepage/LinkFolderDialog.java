/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.dialogs.PathVariableSelectionDialog;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class LinkFolderDialog extends StatusDialog {
    private final class FolderNameField extends Observable implements IDialogFieldListener {
        private StringDialogField fNameDialogField;
        
        public FolderNameField(Composite parent, int numOfColumns) {
            createControls(parent, numOfColumns);
        }
        
        private void createControls(Composite parent, int numColumns) {
            fNameDialogField= new StringDialogField();
            fNameDialogField.setLabelText(NewWizardMessages.LinkFolderDialog_folderNameGroup_label); 
            fNameDialogField.doFillIntoGrid(parent, 2);
            LayoutUtil.setHorizontalGrabbing(fNameDialogField.getTextControl(null));
			LayoutUtil.setHorizontalSpan(fNameDialogField.getLabelControl(null), numColumns);
			DialogField.createEmptySpace(parent, numColumns - 1);
            
            fNameDialogField.setDialogFieldListener(this);
        }
        
        public StringDialogField getNameDialogField() {
            return fNameDialogField;
        }
        
        public void setText(String text) {
            fNameDialogField.setText(text);
            fNameDialogField.setFocus();
        }
        
        public String getText() {
            return fNameDialogField.getText();
        }
        
        protected void fireEvent() {
            setChanged();
            notifyObservers();
        }

        public void dialogFieldChanged(DialogField field) {
            fireEvent();
        }
    }
    
    private final class LinkFields extends Observable implements IStringButtonAdapter, IDialogFieldListener{
        private StringButtonDialogField fLinkLocation;
        
        private static final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaScriptUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$
        
        public LinkFields(Composite parent, int numColumns) {
            createControls(parent, numColumns);
        }
        
        private void createControls(Composite parent, int numColumns) {
            fLinkLocation= new StringButtonDialogField(this);
            
            fLinkLocation.setLabelText(NewWizardMessages.LinkFolderDialog_dependenciesGroup_locationLabel_desc); 
            fLinkLocation.setButtonLabel(NewWizardMessages.LinkFolderDialog_dependenciesGroup_browseButton_desc); 
            fLinkLocation.setDialogFieldListener(this);
            
            SelectionButtonDialogField variables= new SelectionButtonDialogField(SWT.PUSH);
            variables.setLabelText(NewWizardMessages.LinkFolderDialog_dependenciesGroup_variables_desc); 
            variables.setDialogFieldListener(new IDialogFieldListener() {
                public void dialogFieldChanged(DialogField field) {
                    handleVariablesButtonPressed();
                }
            });
            
            fLinkLocation.doFillIntoGrid(parent, numColumns);

			LayoutUtil.setHorizontalSpan(fLinkLocation.getLabelControl(null), numColumns);
            LayoutUtil.setHorizontalGrabbing(fLinkLocation.getTextControl(null));
            
            variables.doFillIntoGrid(parent, 1);
        }
        
        public String getLinkTarget() {
            return fLinkLocation.getText();
        }
        
		public void setLinkTarget(String text) {
			fLinkLocation.setText(text);
		}

        /*(non-Javadoc)
         * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
         */
        public void changeControlPressed(DialogField field) {
            final DirectoryDialog dialog= new DirectoryDialog(getShell());
            dialog.setMessage(NewWizardMessages.JavaProjectWizardFirstPage_directory_message); 
            String directoryName = getLinkTarget().trim();
            if (directoryName.length() == 0) {
                String prevLocation= JavaScriptPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LAST_EXTERNAL_LOC);
                if (prevLocation != null) {
                    directoryName= prevLocation;
                }
            }
        
            if (directoryName.length() > 0) {
                final File path = new File(directoryName);
                if (path.exists())
                    dialog.setFilterPath(directoryName);
            }
            final String selectedDirectory = dialog.open();
            if (selectedDirectory != null) {
                fLinkLocation.setText(selectedDirectory);
                if (fName == null) {
                	fFolderNameField.setText(selectedDirectory.substring(selectedDirectory.lastIndexOf(File.separatorChar) + 1));
                }
                JavaScriptPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
            }
        }
        
        /**
         * Opens a path variable selection dialog
         */
        private void handleVariablesButtonPressed() {
            int variableTypes = IResource.FOLDER;

            // allow selecting file and folder variables when creating a 
            // linked file
            /*if (type == IResource.FILE)
                variableTypes |= IResource.FILE;*/

            PathVariableSelectionDialog dialog = new PathVariableSelectionDialog(getShell(), variableTypes);
            if (dialog.open() == IDialogConstants.OK_ID) {
                String[] variableNames = (String[]) dialog.getResult();
                if (variableNames != null && variableNames.length == 1) {
                    fLinkLocation.setText(variableNames[0]);
                    if (fName == null) {
                        fFolderNameField.setText(variableNames[0]);	
                    }
                }
            }
        }
        
        public void dialogFieldChanged(DialogField field) {
            fireEvent();
        }
        
		private void fireEvent() {
            setChanged();
            notifyObservers();
        }
    }
    
    /**
     * Validate this page and show appropriate warnings and error NewWizardMessages.
     */
    private final class Validator implements Observer {

        public void update(Observable o, Object arg) {
            String name= fFolderNameField.getText();
            IStatus nameStatus= validateFolderName(name);
            if (nameStatus.matches(IStatus.ERROR)) {
            	updateStatus(nameStatus);
            } else {
	            IStatus dependencyStatus= validateLinkLocation(name);
	            updateStatus(StatusUtil.getMoreSevere(nameStatus, dependencyStatus));
            }
        }
        
        /**
		 * Validates this page's controls.
		 *
		 * @return IStatus indicating the validation result. IStatus.OK if the 
		 *  specified link target is valid given the linkHandle.
		 */
		private IStatus validateLinkLocation(String name) {
			IWorkspace workspace= JavaScriptPlugin.getWorkspace();
			IPath path= Path.fromOSString(fDependenciesGroup.getLinkTarget());

			IStatus locationStatus= workspace.validateLinkLocation(fContainer.getFolder(new Path(name)), path);
			if (locationStatus.matches(IStatus.ERROR))
				return locationStatus;

			// use the resolved link target name
			String resolvedLinkTarget= resolveVariable();
			path= new Path(resolvedLinkTarget);
			File linkTargetFile= new Path(resolvedLinkTarget).toFile();
			if (linkTargetFile.exists()) {
				IStatus fileTypeStatus= validateFileType(linkTargetFile);
				if (!fileTypeStatus.isOK())
					return fileTypeStatus;
			} else
				if (locationStatus.isOK()) {
					// locationStatus takes precedence over missing location warning.
					return new StatusInfo(IStatus.ERROR, NewWizardMessages.NewFolderDialog_linkTargetNonExistent); 
				}
			if (locationStatus.isOK()) {
				return new StatusInfo();
			}
			return new StatusInfo(locationStatus.getSeverity(), locationStatus.getMessage());
		}
        
        /**
         * Validates the type of the given file against the link type specified
         * in the constructor.
         * 
         * @param linkTargetFile file to validate
         * @return IStatus indicating the validation result. IStatus.OK if the 
         *  given file is valid.
         */
        private IStatus validateFileType(File linkTargetFile) {
            if (!linkTargetFile.isDirectory())
                return new StatusInfo(IStatus.ERROR, NewWizardMessages.NewFolderDialog_linkTargetNotFolder); 
            return new StatusInfo();
        }
        
        /**
         * Tries to resolve the value entered in the link target field as 
         * a variable, if the value is a relative path.
         * Displays the resolved value if the entered value is a variable.
         */
        private String resolveVariable() {
            IPathVariableManager pathVariableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
            IPath path= Path.fromOSString(fDependenciesGroup.getLinkTarget());
            IPath resolvedPath= pathVariableManager.resolvePath(path);
            return resolvedPath.toOSString();
        }
                
        /**
         * Checks if the folder name is valid.
         *
         * @return <code>true</code> if validation was
         * correct, <code>false</code> otherwise
         */
        private IStatus validateFolderName(String name) {
            if (name.length() == 0) { 
            	return new StatusInfo(IStatus.ERROR, NewWizardMessages.NewFolderDialog_folderNameEmpty); 
            }
            
            IStatus nameStatus = fContainer.getWorkspace().validateName(name, IResource.FOLDER);
            if (!nameStatus.matches(IStatus.ERROR)) {
                return nameStatus;
            }
            
            IPath path = new Path(name);
            if (fContainer.findMember(path) != null) {
            	return new StatusInfo(IStatus.ERROR, Messages.format(NewWizardMessages.NewFolderDialog_folderNameEmpty_alreadyExists, name)); 
            }
            return nameStatus;
        }
    }
    
    private FolderNameField fFolderNameField;
    private LinkFields fDependenciesGroup;
    private IContainer fContainer;
	private IFolder fCreatedFolder;
	private boolean fCreateLink;
	private String fName;
	private String fTarget;

    /**
     * Creates a NewFolderDialog
     * 
     * @param parentShell parent of the new dialog
     * @param container parent of the new folder
     * 
     * @see HintTextGroup
     */
    public LinkFolderDialog(Shell parentShell, IContainer container) {
    	this(parentShell, container, true);
    }

    public LinkFolderDialog(Shell parentShell, IContainer container, boolean createLink) {
    	super(parentShell);
    	fContainer = container;
		fCreateLink= createLink;
        setTitle(NewWizardMessages.LinkFolderDialog_title); 
        setShellStyle(getShellStyle() | SWT.RESIZE);
        setStatusLineAboveButtons(true);
	}

	/* (non-Javadoc)
     * Method declared in Window.
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
    }

    /**
     * @see org.eclipse.jface.window.Window#create()
     */
    public void create() {
        super.create();
        // initially disable the ok button since we don't preset the
        // folder name field
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
    
    public void setName(String name) {
		if (fFolderNameField != null) {
    		fFolderNameField.setText(name);
    	}
    	fName= name;
    }
    
    public void setLinkTarget(String target) {
		if (fDependenciesGroup != null) {
    		fDependenciesGroup.setLinkTarget(target);
    	}
    	fTarget= target;
    }

    /* (non-Javadoc)
     * Method declared on Dialog.
     */
    protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
		
        int numOfColumns= 3;
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        
        GridLayout layout = new GridLayout(numOfColumns, false);
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        composite.setLayout(layout);
        GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.minimumWidth= convertWidthInCharsToPixels(80);
        composite.setLayoutData(gridData);
        
        Label label= new Label(composite, SWT.NONE);
        label.setFont(composite.getFont());
        label.setText(Messages.format(NewWizardMessages.LinkFolderDialog_createIn, fContainer.getFullPath().makeRelative().toString())); 
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, numOfColumns, 1));
        
        fDependenciesGroup= new LinkFields(composite, numOfColumns);
        if (fTarget != null) {
        	fDependenciesGroup.setLinkTarget(fTarget);
        }
        fFolderNameField= new FolderNameField(composite, numOfColumns);
        if (fName != null) {
        	fFolderNameField.setText(fName);
        }
        
        Validator validator= new Validator();
        fDependenciesGroup.addObserver(validator);
        fFolderNameField.addObserver(validator);

        return composite;
    }

    /**
     * Creates a folder resource handle for the folder with the given name.
     * The folder handle is created relative to the container specified during 
     * object creation. 
     *
     * @param folderName the name of the folder resource to create a handle for
     * @return the new folder resource handle
     */
    private IFolder createFolderHandle(String folderName) {
        IWorkspaceRoot workspaceRoot = fContainer.getWorkspace().getRoot();
        IPath folderPath = fContainer.getFullPath().append(folderName);
        IFolder folderHandle = workspaceRoot.getFolder(folderPath);

        return folderHandle;
    }

    /**
     * Creates a new folder with the given name and optionally linking to
     * the specified link target.
     * 
     * @param folderName name of the new folder
     * @param linkTargetName name of the link target folder. may be null.
     * @return IFolder the new folder
     */
    private IFolder createNewFolder(final String folderName, final String linkTargetName) {
        final IFolder folderHandle = createFolderHandle(folderName);
        
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
            public void execute(IProgressMonitor monitor) throws CoreException {
                try {
                    monitor.beginTask(NewWizardMessages.NewFolderDialog_progress, 2000); 
                    if (monitor.isCanceled())
                        throw new OperationCanceledException();
                    
                        // create link to folder
                    folderHandle.createLink(Path.fromOSString(fDependenciesGroup.getLinkTarget()), IResource.ALLOW_MISSING_LOCAL, monitor);
                    
                    if (monitor.isCanceled())
                        throw new OperationCanceledException();
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                finally {
                    monitor.done();
                }
            }
        };
        
        try {
            new ProgressMonitorDialog(getShell())
                    .run(true, true, operation);
        } catch (InterruptedException exception) {
            return null;
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof CoreException) {
                ErrorDialog.openError(getShell(), NewWizardMessages.NewFolderDialog_errorTitle, 
                        null, // no special message
                        ((CoreException) exception.getTargetException())
                                .getStatus());
            } else {
                // CoreExceptions are handled above, but unexpected runtime exceptions and errors may still occur.
                JavaScriptPlugin.log(new Exception(Messages.format(
                        "Exception in {0}.createNewFolder(): {1}", //$NON-NLS-1$
                        new Object[] { getClass().getName(),
                                exception.getTargetException() })));
                MessageDialog.openError(getShell(), NewWizardMessages.NewFolderDialog_errorTitle, 
                        Messages.format(
                                NewWizardMessages.NewFolderDialog_internalError, 
                                new Object[] { exception.getTargetException()
                                        .getMessage() }));
            }
            return null;
        }
        
        return folderHandle;
    }

    /**
     * Update the dialog's status line to reflect the given status. It is safe to call
     * this method before the dialog has been opened.
     */
    protected void updateStatus(IStatus status) {
        super.updateStatus(status);
    }
   
    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#okPressed()
     */
    protected void okPressed() {
    	if (fCreateLink) {
	        String linkTarget = fDependenciesGroup.getLinkTarget();
	        linkTarget= linkTarget.length() == 0 ? null : linkTarget;
	        fCreatedFolder = createNewFolder(fFolderNameField.getText(), linkTarget);
    	} else {
    		fCreatedFolder = createFolderHandle(fFolderNameField.getText());
    	}
        super.okPressed();
    }
    
    /**
     * Returns the created folder or <code>null</code>
     * if there is none.
     * 
     * @return created folder or <code>null</code>
     */
    public IFolder getCreatedFolder() {
        return fCreatedFolder;
    }

	public IPath getLinkTarget() {
		return Path.fromOSString(fDependenciesGroup.getLinkTarget());
	}
    
}
