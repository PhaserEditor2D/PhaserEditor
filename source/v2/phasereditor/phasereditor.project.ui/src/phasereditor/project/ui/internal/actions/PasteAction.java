// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.project.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectOperation;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorMessages;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * Copied from org.eclipse.ui.internal.navigator.resources.actions.PasteAction
 *
 */
@SuppressWarnings("all")
public class PasteAction extends SelectionListenerAction {

    /**
     * The id of this action.
     */
    public static final String ID = PlatformUI.PLUGIN_ID + ".PasteAction";//$NON-NLS-1$

    /**
     * The shell in which to show any dialogs.
     */
    private Shell shell;

    /**
     * System clipboard
     */
    private Clipboard clipboard;

    /**
     * Creates a new action.
     *
     * @param shell the shell for any dialogs
     * @param clipboard the clipboard
     */
    public PasteAction(Shell shell, Clipboard clipboard) {
        super(WorkbenchNavigatorMessages.PasteAction_Past_);
        Assert.isNotNull(shell);
        Assert.isNotNull(clipboard);
        this.shell = shell;
        this.clipboard = clipboard;
        setToolTipText(WorkbenchNavigatorMessages.PasteAction_Paste_selected_resource_s_);
        setId(PasteAction.ID);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "HelpId"); //$NON-NLS-1$
				// TODO INavigatorHelpContextIds.PASTE_ACTION);
    }

    /**
     * Returns the actual target of the paste action. Returns null
     * if no valid target is selected.
     *
     * @return the actual target of the paste action
     */
    private IResource getTarget() {
		List<? extends IResource> selectedResources = getSelectedResources();

        for (IResource resource : selectedResources) {
            if (resource instanceof IProject && !((IProject) resource).isOpen()) {
				return null;
			}
            if (resource.getType() == IResource.FILE) {
				resource = resource.getParent();
			}
            if (resource != null) {
				return resource;
			}
        }
        return null;
    }

    /**
     * Returns whether any of the given resources are linked resources.
     *
     * @param resources resource to check for linked type. may be null
     * @return true=one or more resources are linked. false=none of the
     * 	resources are linked
     */
    private boolean isLinked(IResource[] resources) {
        for (IResource resource : resources) {
            if (resource.isLinked()) {
				return true;
			}
        }
        return false;
    }

    /**
     * Implementation of method defined on <code>IAction</code>.
     */
    @Override
	public void run() {
        // try a resource transfer
        ResourceTransfer resTransfer = ResourceTransfer.getInstance();
		IResource[] resourceData = (IResource[]) clipboard.getContents(resTransfer);

        if (resourceData != null && resourceData.length > 0) {
            if (resourceData[0].getType() == IResource.PROJECT) {
                // enablement checks for all projects
                for (IResource resource : resourceData) {
					CopyProjectOperation operation = new CopyProjectOperation(shell);
                    operation.copyProject((IProject) resource);
                }
            } else {
                // enablement should ensure that we always have access to a container
                IContainer container = getContainer();
				CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(shell);
                operation.copyResources(resourceData, container);
            }
            return;
        }

        // try a file transfer
        FileTransfer fileTransfer = FileTransfer.getInstance();
        String[] fileData = (String[]) clipboard.getContents(fileTransfer);

        if (fileData != null) {
            // enablement should ensure that we always have access to a container
            IContainer container = getContainer();
			CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(shell);
            operation.copyFiles(fileData, container);
        }
    }

    /**
     * Returns the container to hold the pasted resources.
     */
    private IContainer getContainer() {
		List<? extends IResource> selection = getSelectedResources();
        if (selection.get(0) instanceof IFile) {
			return ((IFile) selection.get(0)).getParent();
		}
        return (IContainer) selection.get(0);
    }

    /**
     * The <code>PasteAction</code> implementation of this
     * <code>SelectionListenerAction</code> method enables this action if
     * a resource compatible with what is on the clipboard is selected.
     *
     * -Clipboard must have IResource or java.io.File
     * -Projects can always be pasted if they are open
     * -Workspace folder may not be copied into itself
     * -Files and folders may be pasted to a single selected folder in open
     * 	project or multiple selected files in the same folder
     */
    @Override
	protected boolean updateSelection(IStructuredSelection selection) {
        if (!super.updateSelection(selection)) {
			return false;
		}

        final IResource[][] clipboardData = new IResource[1][];
		shell.getDisplay().syncExec(() -> {
			// clipboard must have resources or files
			ResourceTransfer resTransfer = ResourceTransfer.getInstance();
			clipboardData[0] = (IResource[]) clipboard.getContents(resTransfer);
		});
        IResource[] resourceData = clipboardData[0];
        boolean isProjectRes = resourceData != null && resourceData.length > 0
                && resourceData[0].getType() == IResource.PROJECT;

        if (isProjectRes) {
            for (IResource resource : resourceData) {
                // make sure all resource data are open projects
                // can paste open projects regardless of selection
                if (resource.getType() != IResource.PROJECT
                        || ((IProject) resource).isOpen() == false) {
					return false;
				}
            }
            return true;
        }

        if (getSelectedNonResources().size() > 0) {
			return false;
		}

        IResource targetResource = getTarget();
        // targetResource is null if no valid target is selected (e.g., open project)
        // or selection is empty
        if (targetResource == null) {
			return false;
		}

        // can paste files and folders to a single selection (file, folder,
        // open project) or multiple file selection with the same parent
		List<? extends IResource> selectedResources = getSelectedResources();
        if (selectedResources.size() > 1) {
            for (IResource resource : selectedResources) {
                if (resource.getType() != IResource.FILE) {
					return false;
				}
                if (!targetResource.equals(resource.getParent())) {
					return false;
				}
            }
        }
        if (resourceData != null) {
            // linked resources can only be pasted into projects
            if (isLinked(resourceData)
                && targetResource.getType() != IResource.PROJECT
                && targetResource.getType() != IResource.FOLDER) {
				return false;
			}

            if (targetResource.getType() == IResource.FOLDER) {
                // don't try to copy folder to self
                for (IResource resource : resourceData) {
                    if (targetResource.equals(resource)) {
						return false;
					}
                }
            }
            return true;
        }
        TransferData[] transfers = clipboard.getAvailableTypes();
        FileTransfer fileTransfer = FileTransfer.getInstance();
        for (TransferData transfer : transfers) {
            if (fileTransfer.isSupportedType(transfer)) {
				return true;
			}
        }
        return false;
    }
}
