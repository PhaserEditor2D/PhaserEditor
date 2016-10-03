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
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;

/**
 * A <code>IRestrictedAccessTypeRequestor</code> collects search results from a <code>searchAllTypeNames</code>
 * query to a <code>SearchBasicEngine</code> providing restricted access information when a type is accepted.
 * @see org.eclipse.wst.jsdt.core.search.TypeNameRequestor
 */
public interface IRestrictedAccessTypeRequestor {

	public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] superTypeNames, char[][] enclosingTypeNames, String path, AccessRestriction access);

}
