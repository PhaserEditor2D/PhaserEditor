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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator;

/**
 * Special problem decorator for hierarchical package layout.
 * <p>
 * It only decorates package fragments which are not covered by the
 * <code>ProblemsLabelDecorator</code>.
 * </p>
 * 
 * @see org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator 
 * 
 */
public class TreeHierarchyLayoutProblemsDecorator extends ProblemsLabelDecorator {

	private boolean fIsFlatLayout;
	
	public TreeHierarchyLayoutProblemsDecorator() {
		this(false);
	}
	
	public TreeHierarchyLayoutProblemsDecorator(boolean isFlatLayout) {
		super(null);
		fIsFlatLayout= isFlatLayout;
	}
	
	protected int computePackageAdornmentFlags(IPackageFragment fragment) {
		if (!fIsFlatLayout && !fragment.isDefaultPackage()) {
			return super.computeAdornmentFlags(fragment.getResource());
		}
		return super.computeAdornmentFlags(fragment);
	}		

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator#computeAdornmentFlags(java.lang.Object)
	 */
	protected int computeAdornmentFlags(Object element) {
		if (element instanceof IPackageFragment) {
			return computePackageAdornmentFlags((IPackageFragment) element);
		} else if (element instanceof LogicalPackage) {
			IPackageFragment[] fragments= ((LogicalPackage) element).getFragments();
			int res= 0;
			for (int i= 0; i < fragments.length; i++) {
				int flags= computePackageAdornmentFlags(fragments[i]);
				if (flags == JavaScriptElementImageDescriptor.ERROR) {
					return flags;
				} else if (flags != 0) {
					res= flags;
				}
			}
			return res;
		}
		return super.computeAdornmentFlags(element);
	}
	
	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
	}

}
