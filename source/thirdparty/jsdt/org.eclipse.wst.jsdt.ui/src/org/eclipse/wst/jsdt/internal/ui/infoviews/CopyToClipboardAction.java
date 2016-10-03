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
package org.eclipse.wst.jsdt.internal.ui.infoviews;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

class CopyToClipboardAction extends SelectionDispatchAction{

	private final static int MAX_REPEAT_COUNT= 10;

	private Clipboard fClipboard;

	public CopyToClipboardAction(IWorkbenchSite site) {
		super(site);

		setText(InfoViewMessages.CopyAction_label);
		setToolTipText(InfoViewMessages.CopyAction_tooltip);
		setDescription(InfoViewMessages.CopyAction_description);

		ISharedImages workbenchImages= PlatformUI.getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAbstractTextEditorHelpContextIds.COPY_ACTION);

		update(getSelection());
	}

	public void selectionChanged(ITextSelection selection) {
		setEnabled(selection != null && selection.getLength() > 0);
	}

	public void run(ITextSelection selection) {
		fClipboard= new Clipboard(getShell().getDisplay());
		try {
			copyToClipboard(selection, 0);
		} finally {
			fClipboard.dispose();
		}
	}

	private void copyToClipboard(ITextSelection selection, int repeatCount) {
		try{
			fClipboard.setContents(new String[] { selection.getText() }, new Transfer[] { TextTransfer.getInstance() });
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD || repeatCount >= MAX_REPEAT_COUNT)
				throw e;

			if (MessageDialog.openQuestion(getShell(), InfoViewMessages.CopyToClipboard_error_title, InfoViewMessages.CopyToClipboard_error_message))
				copyToClipboard(selection, repeatCount + 1);
		}
	}
}
