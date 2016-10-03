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

package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper;

public class MethodWrapperWorkbenchAdapter implements IWorkbenchAdapter {

	private final MethodWrapper fMethodWrapper;

	public MethodWrapperWorkbenchAdapter(MethodWrapper methodWrapper) {
		Assert.isNotNull(methodWrapper);
		fMethodWrapper= methodWrapper;
	}

	public MethodWrapper getMethodWrapper() {
		return fMethodWrapper;
	}

	public Object[] getChildren(Object o) { //should not be called
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
        return fMethodWrapper.getMember().getElementName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return fMethodWrapper.getParent();
	}
	
    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        //Note: A MethodWrapperWorkbenchAdapter is equal to its MethodWrapper and vice versa (bug 101677).
        return fMethodWrapper.equals(obj);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        //Note: A MethodWrapperWorkbenchAdapter is equal to its MethodWrapper and vice versa (bug 101677).
        return fMethodWrapper.hashCode();
    }
    
}
