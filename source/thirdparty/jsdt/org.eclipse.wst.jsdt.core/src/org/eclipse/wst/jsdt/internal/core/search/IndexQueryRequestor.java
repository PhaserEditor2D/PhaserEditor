/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;

/**
 * <p>Requester used when searching an index for matches to a pattern.</p>
 */
public abstract class IndexQueryRequestor {

	/**
	 * <p>Accepts an index match when searching an index.</p>
	 * 
	 * @param documentPath
	 * @param indexRecord
	 * @param participant
	 * @param access
	 * 
	 * @return <code>true</code> to continue search, <code>false</code> to request cancel of search
	 */
	public abstract boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord,
			SearchParticipant participant, AccessRuleSet access);

}
