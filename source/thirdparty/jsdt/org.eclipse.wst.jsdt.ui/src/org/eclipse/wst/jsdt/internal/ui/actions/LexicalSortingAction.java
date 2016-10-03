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
package org.eclipse.wst.jsdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingMessages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.SourcePositionComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;

/*
 * XXX: This class should become part of the MemberFilterActionGroup
 *      which should be renamed to MemberActionsGroup
 */
public class LexicalSortingAction extends Action {
	private JavaScriptElementComparator fComparator= new JavaScriptElementComparator();
	private SourcePositionComparator fSourcePositonComparator= new SourcePositionComparator();
	private StructuredViewer fViewer;
	private String fPreferenceKey;

	public LexicalSortingAction(StructuredViewer viewer, String id) {
		super();
		fViewer= viewer;
		fPreferenceKey= "LexicalSortingAction." + id + ".isChecked"; //$NON-NLS-1$ //$NON-NLS-2$
		setText(JavaBrowsingMessages.LexicalSortingAction_label); 
		JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
		setToolTipText(JavaBrowsingMessages.LexicalSortingAction_tooltip); 
		setDescription(JavaBrowsingMessages.LexicalSortingAction_description); 
		boolean checked= JavaScriptPlugin.getDefault().getPreferenceStore().getBoolean(fPreferenceKey); 
		valueChanged(checked, false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_BROWSING_ACTION);
	}

	public void run() {
		valueChanged(isChecked(), true);
	}

	private void valueChanged(final boolean on, boolean store) {
		setChecked(on);
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				if (on)
					fViewer.setComparator(fComparator);
				else
					fViewer.setComparator(fSourcePositonComparator);
			}
		});
		
		if (store)
			JavaScriptPlugin.getDefault().getPreferenceStore().setValue(fPreferenceKey, on);
	}
}
