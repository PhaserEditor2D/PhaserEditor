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
package org.eclipse.wst.jsdt.ui.search;

import org.eclipse.search.ui.text.Match;

/**
 * A callback interface to report matches against. This class serves as a bottleneck and minimal interface
 * to report matches to the JavaScript search infrastructure. Query participants will be passed an
 * instance of this interface when their <code>search(...)</code> method is called.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface ISearchRequestor {
	/**
	 * Adds a match to the search that issued this particular {@link ISearchRequestor}.
	 * @param match The match to be reported.
	 */
	void reportMatch(Match match);
}
