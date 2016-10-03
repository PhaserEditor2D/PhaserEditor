/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui;


/**
 * These constants define the set of properties that this plug-in expects to
 * be available via <code>ProductProperties#getProperty(String)</code>. The status of
 * this interface and the facilities offered is highly provisional. 
 * Productization support will be reviewed and possibly modified in future 
 * releases.
 * 
 * @see org.eclipse.core.runtime.IProduct#getProperty(String)
 */

public interface IProductConstants {   
	/**
	 * The "explorer" view to use when creating the JavaScript perspective
	 */
	public static final String PERSPECTIVE_EXPLORER_VIEW = "idPerspectiveHierarchyView"; //$NON-NLS-1$

}
