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
package org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

public class StructureSelectHistoryAction extends Action implements IUpdate {
	private JavaEditor fEditor;
	private SelectionHistory fHistory;

	public StructureSelectHistoryAction(JavaEditor editor, SelectionHistory history) {
		super(SelectionActionMessages.StructureSelectHistory_label);
		setToolTipText(SelectionActionMessages.StructureSelectHistory_tooltip);
		setDescription(SelectionActionMessages.StructureSelectHistory_description);
		Assert.isNotNull(history);
		Assert.isNotNull(editor);
		fHistory= history;
		fEditor= editor;
		update();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.STRUCTURED_SELECTION_HISTORY_ACTION);
	}

	public void update() {
		setEnabled(!fHistory.isEmpty());
	}

	public void run() {
		ISourceRange old= fHistory.getLast();
		if (old != null) {
			try {
				fHistory.ignoreSelectionChanges();
				fEditor.selectAndReveal(old.getOffset(), old.getLength());
			} finally {
				fHistory.listenToSelectionChanges();
			}
		}
	}
}
