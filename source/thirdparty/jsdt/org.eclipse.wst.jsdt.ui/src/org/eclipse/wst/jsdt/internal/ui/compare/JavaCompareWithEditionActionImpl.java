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
package org.eclipse.wst.jsdt.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.HistoryPageCompareEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;


/**
 * Provides "Replace from local history" for Java elements.
 */
class JavaCompareWithEditionActionImpl extends JavaHistoryActionImpl {
	
	private static boolean USE_MODAL_COMPARE = false;
	
	JavaCompareWithEditionActionImpl() {
		super(false);
	}	
	
	public void run(ISelection selection) {
				
		IMember input= getEditionElement(selection);
		if (input == null) {
			MessageDialog.openInformation(getShell(), CompareMessages.CompareWithHistory_title, CompareMessages.CompareWithHistory_invalidSelectionMessage);
			return;
		}
		
		JavaElementHistoryPageSource pageSource = JavaElementHistoryPageSource.getInstance();
		final IFile file= pageSource.getFile(input);
		if (file == null) {
			MessageDialog.openError(getShell(), CompareMessages.CompareWithHistory_title, CompareMessages.CompareWithHistory_internalErrorMessage);
			return;
		}
		
		if (USE_MODAL_COMPARE) {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setLeftEditable(false);
			cc.setRightEditable(false);
			HistoryPageCompareEditorInput ci = new HistoryPageCompareEditorInput(cc, pageSource, input);
			ci.setTitle(CompareMessages.JavaCompareWithEditionActionImpl_0);
			ci.setHelpContextId(IJavaHelpContextIds.COMPARE_ELEMENT_WITH_HISTORY_DIALOG);
			CompareUI.openCompareDialog(ci);
		} else {
			TeamUI.showHistoryFor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), input, pageSource);
		}
	}
}

