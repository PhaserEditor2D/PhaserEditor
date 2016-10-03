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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ProblemTableViewer;

/**
 * Special problem table viewer to handle logical packages.
 */
class PackagesViewTableViewer extends ProblemTableViewer implements IPackagesViewViewer {

	public PackagesViewTableViewer(Composite parent, int style) {
		super(parent, style);
		ColoredViewersManager.install(this);
	}

	public void mapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage && item instanceof Item) {
			LogicalPackage cp= (LogicalPackage) element;
			IPackageFragment[] fragments= cp.getFragments();
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				fResourceToItemsMapper.addToMap(fragment, (Item)item);
			}
		}
		super.mapElement(element, item);
	}

	public void unmapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage && item instanceof Item) {
			LogicalPackage cp= (LogicalPackage) element;
			IPackageFragment[] fragments= cp.getFragments();
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				fResourceToItemsMapper.removeFromMap(fragment, (Item)item);
			}
		}
		super.unmapElement(element, item);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#getFilteredChildren(java.
	 * lang.Object)
	 */
	protected Object[] getFilteredChildren(Object parent) {

		Object[] result= getRawChildren(parent);
		List list= new ArrayList();
		if (result != null) {
			Object[] toBeFiltered= new Object[1];
			for (int i= 0; i < result.length; i++) {
				Object object= result[i];
				if(object instanceof LogicalPackage) {
					if(selectLogicalPackage((LogicalPackage)object))
						list.add(object);
				} else {
					toBeFiltered[0]= object;
					if (filter(toBeFiltered).length == 1)
						list.add(object);
				}
			}
		}
		return list.toArray();
	}

	private boolean selectLogicalPackage(LogicalPackage logicalPackage) {
		return filter(logicalPackage.getFragments()).length > 0;
	}

	// --------- see: IPackagesViewViewer ----------

	public Widget doFindItem(Object element){
		return super.doFindItem(element);
	}

	public Widget doFindInputItem(Object element){
		return super.doFindInputItem(element);
	}

	public List getSelectionFromWidget(){
		return super.getSelectionFromWidget();
	}

	public void doUpdateItem(Widget item, Object element, boolean fullMap){
		super.doUpdateItem(item, element, fullMap);
	}

	public void internalRefresh(Object element){
		super.internalRefresh(element);
	}

	public void setSelectionToWidget(List l, boolean reveal){
		super.setSelectionToWidget(l, reveal);
	}
}
