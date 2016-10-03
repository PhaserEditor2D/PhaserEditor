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
package org.eclipse.wst.jsdt.ui;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewPart;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public interface IPackagesViewPart extends IViewPart {
	/**
	 * Selects and reveals the given element in this packages view.
	 * The tree will be expanded as needed to show the element.
	 *
	 * @param element the element to be revealed
	 */
	void selectAndReveal(Object element);
	
	/**
	 * Returns the TreeViewer shown in the Packages view.
	 * 
	 * @return the tree viewer used in the Packages view
	 * 
	 * 
	 */
	TreeViewer getTreeViewer();
	
    /**
     * Returns whether this Packages view's selection automatically tracks the active editor.
     * 
     * @return <code>true</code> if linking is enabled, <code>false</code> if not
     * 
     */
    boolean isLinkingEnabled();
    
    /**
     * Sets whether this Packages view's selection automatically tracks the active editor.
     * 
     * @param enabled <code>true</code> to enable, <code>false</code> to disable
     * 
     */
    void setLinkingEnabled(boolean enabled);
}
