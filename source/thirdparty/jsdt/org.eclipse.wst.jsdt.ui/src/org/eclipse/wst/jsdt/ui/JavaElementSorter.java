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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;


/**
 * Sorter for JavaScript elements. Ordered by element category, then by element name. 
 * Package fragment roots are sorted as ordered on the classpath.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class JavaElementSorter extends ViewerSorter {
	
	private final JavaScriptElementComparator fComparator;
	
	/**
	 * Constructor.
	 */
	public JavaElementSorter() {	
		super(null); // delay initialization of collator
		fComparator= new JavaScriptElementComparator();
	}
		
	/*
	 * @see ViewerSorter#category
	 */
	public int category(Object element) {
		return fComparator.category(element);
	}
	
	/*
	 * @see ViewerSorter#compare
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		return fComparator.compare(viewer, e1, e2);
	}
	
	/**
	 * Overrides {@link org.eclipse.jface.viewers.ViewerSorter#getCollator()}.
	 * @deprecated The method is not intended to be used by clients.
	 */
	public final java.text.Collator getCollator() {
		// kept in for API compatibility
		if (collator == null) {
			collator= java.text.Collator.getInstance();
		}
		return collator;
	}
}
