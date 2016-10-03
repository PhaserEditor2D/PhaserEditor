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
package org.eclipse.wst.jsdt.internal.corext;

import org.eclipse.osgi.util.NLS;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public final class CorextMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.corext.CorextMessages";//$NON-NLS-1$

	private CorextMessages() {
		// Do not instantiate
	}

	public static String Resources_outOfSyncResources;
	public static String Resources_outOfSync;
	public static String Resources_modifiedResources;
	public static String Resources_fileModified;
	
	public static String JavaDocLocations_migrate_operation;
	public static String JavaDocLocations_error_readXML;
	public static String JavaDocLocations_migratejob_name;
	
	
	public static String History_error_serialize;
	public static String History_error_read;
	public static String TypeInfoHistory_consistency_check;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CorextMessages.class);
	}

	public static String JavaModelUtil_applyedit_operation;
}
