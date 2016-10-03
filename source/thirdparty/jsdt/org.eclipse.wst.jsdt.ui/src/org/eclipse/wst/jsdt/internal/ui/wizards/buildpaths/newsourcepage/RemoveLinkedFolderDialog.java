/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IRemoveLinkedFolderQuery;

/**
 * Dialog to prompt whether a linked folder should be deleted.
 * 
 */
class RemoveLinkedFolderDialog extends MessageDialog {

	/** The remove status */
	private int fRemoveStatus= IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH_AND_FOLDER;

	/** The remove build path and folder button */
	private Button fRemoveBuildPathAndFolder;

	/** The remove build path button */
	private Button fRemoveBuildPath;

	/**
	 * Creates a new remove linked folder dialog.
	 * 
	 * @param shell the parent shell to use
	 * @param folder the linked folder to remove
	 */
	RemoveLinkedFolderDialog(final Shell shell, final IFolder folder) {
		super(shell, NewWizardMessages.ClasspathModifierQueries_confirm_remove_linked_folder_label, null, Messages.format(NewWizardMessages.ClasspathModifierQueries_confirm_remove_linked_folder_message, new Object[] { folder.getFullPath()}), MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0); // yes is the default
		Assert.isTrue(folder.isLinked());
	}

	protected Control createCustomArea(final Composite parent) {

		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		fRemoveBuildPathAndFolder= new Button(composite, SWT.RADIO);
		fRemoveBuildPathAndFolder.addSelectionListener(selectionListener);

		fRemoveBuildPathAndFolder.setText(NewWizardMessages.ClasspathModifierQueries_delete_linked_folder);
		fRemoveBuildPathAndFolder.setFont(parent.getFont());

		fRemoveBuildPath= new Button(composite, SWT.RADIO);
		fRemoveBuildPath.addSelectionListener(selectionListener);

		fRemoveBuildPath.setText(NewWizardMessages.ClasspathModifierQueries_do_not_delete_linked_folder);
		fRemoveBuildPath.setFont(parent.getFont());

		fRemoveBuildPathAndFolder.setSelection(fRemoveStatus == IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH_AND_FOLDER);
		fRemoveBuildPath.setSelection(fRemoveStatus == IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH);

		return composite;
	}

	private SelectionListener selectionListener= new SelectionAdapter() {

		public final void widgetSelected(final SelectionEvent event) {
			final Button button= (Button) event.widget;
			if (button.getSelection())
				fRemoveStatus= (button == fRemoveBuildPathAndFolder) ? IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH_AND_FOLDER : IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH;
		}
	};

	/**
	 * Returns the remove status.
	 * 
	 * @return the remove status, one of IRemoveLinkedFolderQuery#REMOVE_XXX
	 */
	public final int getRemoveStatus() {
		return fRemoveStatus;
	}
}
