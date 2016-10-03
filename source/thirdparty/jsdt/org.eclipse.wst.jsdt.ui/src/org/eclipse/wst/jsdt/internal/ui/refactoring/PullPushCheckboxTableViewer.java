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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.IMemberActionInfo;

class PullPushCheckboxTableViewer extends CheckboxTableViewer{
	public PullPushCheckboxTableViewer(Table table) {
		super(table);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#doUpdateItem(org.eclipse.swt.widgets.Widget, java.lang.Object, boolean)
	 */
	protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
		super.doUpdateItem(widget, element, fullMap);
		if (! (widget instanceof TableItem))
			return;
		TableItem item= (TableItem)widget;
		IMemberActionInfo info= (IMemberActionInfo)element;
		item.setChecked(PullPushCheckboxTableViewer.getCheckState(info));
		Assert.isTrue(item.getChecked() == PullPushCheckboxTableViewer.getCheckState(info));
	}

	/*
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object, java.lang.Object)
	 */
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
		// XXX workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=9390
		setCheckState((IMemberActionInfo[])input);
	}

	private void setCheckState(IMemberActionInfo[] infos) {
		if (infos == null)
			return;
		for (int i= 0; i < infos.length; i++) {
			IMemberActionInfo info= infos[i];
			setChecked(info, PullPushCheckboxTableViewer.getCheckState(info));
		}	
	}

	private static boolean getCheckState(IMemberActionInfo info) {
		return info.isActive();
	}		
	
	/*
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	public void refresh() {
		int topIndex = getTable().getTopIndex();
		super.refresh();
		// XXX workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=9390
		setCheckState((IMemberActionInfo[])getInput());
		if (topIndex < getTable().getItemCount())
			getTable().setTopIndex(topIndex); //see bug 31645
	}
}
