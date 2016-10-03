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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import org.eclipse.wst.jsdt.core.IMember;


/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public interface IRequestQuery {
	
	// return codes
	public static final int CANCEL= 0;
	public static final int NO= 1;
	public static final int YES= 2;
	public static final int YES_ALL= 3;
	
	/**
	 * Do the callback. Returns YES, NO, YES_ALL or CANCEL
	 */
	int doQuery(IMember member);
}
