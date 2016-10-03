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
package org.eclipse.wst.jsdt.internal.corext.util;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.core.search.TypeNameMatchRequestor;

public class TypeNameMatchCollector extends TypeNameMatchRequestor {

	private final Collection fCollection;

	public TypeNameMatchCollector(Collection collection) {
		Assert.isNotNull(collection);
		fCollection= collection;
	}
	
	private boolean inScope(TypeNameMatch match) {
		return !TypeFilter.isFiltered(match);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.TypeNameMatchRequestor#acceptTypeNameMatch(org.eclipse.wst.jsdt.core.search.TypeNameMatch)
	 */
	public void acceptTypeNameMatch(TypeNameMatch match) {
		if (inScope(match)) {
			fCollection.add(match);
		}
	}

}
