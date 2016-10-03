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
package org.eclipse.wst.jsdt.internal.corext.callhierarchy;

import org.eclipse.osgi.util.NLS;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public final class CallHierarchyMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.corext.callhierarchy.CallHierarchyMessages";//$NON-NLS-1$

	private CallHierarchyMessages() {
		// Do not instantiate
	}

	public static String CallerMethodWrapper_taskname;
	public static String CalleeMethodWrapper_taskname;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CallHierarchyMessages.class);
	}
}
