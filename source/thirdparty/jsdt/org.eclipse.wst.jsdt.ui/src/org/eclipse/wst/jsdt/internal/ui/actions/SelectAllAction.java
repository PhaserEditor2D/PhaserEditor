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
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
 
/**
 * This action selects all entries currently showing in view.
 */
public class SelectAllAction extends Action {

	private StructuredViewer fViewer;

	/**
	 * Creates the action for a TreeViewer
	 */
	public SelectAllAction(TreeViewer viewer) {
		this((StructuredViewer) viewer);
	}
	
	/**
	 * Creates the action for a TableViewer
	 */
	public SelectAllAction(TableViewer viewer) {
		this((StructuredViewer) viewer);
	}

	private SelectAllAction(StructuredViewer viewer) {
		super("selectAll"); //$NON-NLS-1$
		setText(ActionMessages.SelectAllAction_label); 
		setToolTipText(ActionMessages.SelectAllAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SELECT_ALL_ACTION);
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}
	
	private void collectExpandedAndVisible(TreeItem[] items, List result) {
		for (int i= 0; i < items.length; i++) {
			TreeItem item= items[i];
			if (item.getData() != null) {
				result.add(item);
				if (item.getExpanded()) {
					collectExpandedAndVisible(item.getItems(), result);
				}
			}
		}
	}

	/**
	 * Selects all resources in the view.
	 */
	public void run() {
		if (fViewer instanceof TreeViewer) {
			ArrayList allVisible= new ArrayList();
			Tree tree= ((TreeViewer) fViewer).getTree();
			collectExpandedAndVisible(tree.getItems(), allVisible);
			tree.setSelection((TreeItem[]) allVisible.toArray(new TreeItem[allVisible.size()]));
			fViewer.setSelection(fViewer.getSelection());
		} else if (fViewer instanceof TableViewer) {
			((TableViewer) fViewer).getTable().selectAll();
			// force viewer selection change
			fViewer.setSelection(fViewer.getSelection());
		}
	}
}
