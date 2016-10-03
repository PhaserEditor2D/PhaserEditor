/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;

public class CPListElementSorter extends ViewerComparator {
	
	private static final int SOURCE= 0;
	private static final int PROJECT= 1;
	private static final int LIBRARY= 2;
	private static final int CONTAINER= 4;
	
	private static final int ATTRIBUTE= 5;
	private static final int CONTAINER_ENTRY= 6;
	private static final int JRE_ENTRY = -2;
	private static final int REQUIRED_ENTRY = -1;
	private static final int OTHER= 7;
	
	/*
	 * @see ViewerSorter#category(Object)
	 */
	public int category(Object obj) {
		if (obj instanceof CPListElement) {
			CPListElement element= (CPListElement) obj;
			if (element.getParentContainer() != null) {
				return CONTAINER_ENTRY;
			}
			
			if(element.isJRE()) {
				return JRE_ENTRY;
			}
			
			if(element.isInNonModifiableContainer()) {
				return REQUIRED_ENTRY;
			}	
			
			switch (element.getEntryKind()) {
			case IIncludePathEntry.CPE_LIBRARY:
				return LIBRARY;
			case IIncludePathEntry.CPE_PROJECT:
				return PROJECT;
			case IIncludePathEntry.CPE_SOURCE:
				return SOURCE;
			case IIncludePathEntry.CPE_CONTAINER:
				return CONTAINER;
			}
		} else if (obj instanceof CPListElementAttribute) {
			return ATTRIBUTE;
		} else if (obj instanceof IAccessRule) {
			return ATTRIBUTE;
		}
		return OTHER;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		
        int cat1 = category(e1);
        int cat2 = category(e2);

        if (cat1 != cat2)
            return cat1 - cat2;
        
        if (cat1 == ATTRIBUTE || cat1 == CONTAINER_ENTRY) {
        	return 0; // do not sort attributes or container entries
        }
        
		if (viewer instanceof ContentViewer) {
			IBaseLabelProvider prov = ((ContentViewer) viewer).getLabelProvider();
            if (prov instanceof ILabelProvider) {
                ILabelProvider lprov = (ILabelProvider) prov;
                String name1 = lprov.getText(e1);
                String name2 = lprov.getText(e2);
                return getComparator().compare(name1, name2);
            }
		}
		return 0;
	}
	

}
