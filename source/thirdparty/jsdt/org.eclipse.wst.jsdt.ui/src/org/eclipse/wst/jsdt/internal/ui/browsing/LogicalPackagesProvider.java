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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

abstract class LogicalPackagesProvider implements IPropertyChangeListener, IElementChangedListener {

	protected static final Object[] NO_CHILDREN= new Object[0];

	protected Map fMapToLogicalPackage;
	protected Map fMapToPackageFragments;
	protected boolean fCompoundState;
	protected StructuredViewer fViewer;
	protected boolean fInputIsProject;

	public LogicalPackagesProvider(StructuredViewer viewer){
		fViewer= viewer;
		fCompoundState= isInCompoundState();
		fInputIsProject= true;
		fMapToLogicalPackage= new HashMap();
		fMapToPackageFragments= new HashMap();
		JavaScriptPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Adds the given fragments to the internal map.
	 * Existing fragments will be replaced by the new ones.
	 *
	 * @param packageFragments the package fragments to add
	 */
	protected void addFragmentsToMap(IPackageFragment[] packageFragments) {
		for (int i= 0; i < packageFragments.length; i++) {
			IPackageFragment fragment= packageFragments[i];
			String key= getKey(fragment);
			fMapToPackageFragments.put(key, fragment);
		}
	}

	protected String getKey(IPackageFragment fragment) {
		return fragment.getElementName() + fragment.getJavaScriptProject().getElementName();
	}

	/**
	 * Returns the logical package for the given package fragment
	 * or <code>null</code> if it is not grouped by logical package.
	 *
	 * @param fragment the package fragment
	 * @return the logical package
	 */
	public LogicalPackage findLogicalPackage(IPackageFragment fragment) {
		Assert.isNotNull(fragment);
		if (isInCompoundState())
			return (LogicalPackage)fMapToLogicalPackage.get(getKey(fragment));
		else
			return null;
	}

	/**
	 * Combines packages with same names into a logical package which will
	 * be added to the resulting array. If a package is not yet in this content
	 * provider then the package fragment is added to the resulting array.
	 *
	 * @param packageFragments the package fragments to combine
	 * @return an array with combined (logical) packages and package fragments
	 */
	protected Object[] combineSamePackagesIntoLogialPackages(IPackageFragment[] packageFragments) {

		if (!fCompoundState)
			return packageFragments;

		List newChildren= new ArrayList();

		for (int i= 0; i < packageFragments.length; i++) {
			IPackageFragment fragment=  packageFragments[i];

			if (fragment == null)
				continue;

			LogicalPackage lp= findLogicalPackage(fragment);

			if (lp != null) {
				if (lp.belongs(fragment)) {
					lp.add(fragment);
				}
				if(!newChildren.contains(lp))
					newChildren.add(lp);

			} else {
				String key= getKey(fragment);
				IPackageFragment frag= (IPackageFragment)fMapToPackageFragments.get(key);
				if (frag != null && !fragment.equals(frag)) {
					lp= new LogicalPackage(frag);
					lp.add(fragment);
					newChildren.remove(frag);
					newChildren.add(lp);
					fMapToLogicalPackage.put(key, lp);
					fMapToPackageFragments.remove(frag);
				} else {
					fMapToPackageFragments.put(key, fragment);
					newChildren.add(fragment);
				}
			}
		}
		return newChildren.toArray();
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (fCompoundState == isInCompoundState())
			return;
		else
			fCompoundState= isInCompoundState();

		if (!isInCompoundState()) {
			fMapToLogicalPackage.clear();
			fMapToPackageFragments.clear();
		}

		if(fViewer instanceof TreeViewer){
			TreeViewer viewer= (TreeViewer) fViewer;
			Object[] expandedObjects= viewer.getExpandedElements();
			viewer.refresh();
			viewer.setExpandedElements(expandedObjects);
		} else
			fViewer.refresh();
	}

	protected boolean isInCompoundState() {
		// XXX: for now we don't offer a preference might become a view menu entry
		//		return AppearancePreferencePage.logicalPackagesInPackagesView();
		return true;
	}

	public void dispose(){
		JavaScriptPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		fMapToLogicalPackage= null;
		fMapToPackageFragments= null;
	}

	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput != null) {
			JavaScriptCore.addElementChangedListener(this);
		} else {
			JavaScriptCore.removeElementChangedListener(this);
		}
		fInputIsProject= (newInput instanceof IJavaScriptProject);

		if(viewer instanceof StructuredViewer)
			fViewer= (StructuredViewer)viewer;
	}

	abstract protected void processDelta(IJavaScriptElementDelta delta) throws JavaScriptModelException;

	/*
	 * 
	 */
	protected boolean isClassPathChange(IJavaScriptElementDelta delta) {

		// need to test the flags only for package fragment roots
		if (delta.getElement().getElementType() != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
			return false;

		int flags= delta.getFlags();
		return (delta.getKind() == IJavaScriptElementDelta.CHANGED &&
			((flags & IJavaScriptElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaScriptElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaScriptElementDelta.F_REORDER) != 0));
	}

	/*
	 * @see org.eclipse.wst.jsdt.core.IElementChangedListener#elementChanged(org.eclipse.wst.jsdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		try {
			processDelta(event.getDelta());
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
	}

}
