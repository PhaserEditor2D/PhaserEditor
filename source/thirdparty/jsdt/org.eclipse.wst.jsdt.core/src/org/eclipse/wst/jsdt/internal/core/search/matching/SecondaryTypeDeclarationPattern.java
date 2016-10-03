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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;

/**
 * @deprecated this will be removed at some point because it does not apply to JavaScript
 */
public class SecondaryTypeDeclarationPattern extends TypeDeclarationPattern {

	private final static char[] SECONDARY_PATTERN_KEY = "*/S".toCharArray(); //$NON-NLS-1$

public SecondaryTypeDeclarationPattern() {
	super(R_EXACT_MATCH | R_CASE_SENSITIVE);
}

public SecondaryTypeDeclarationPattern(int matchRule) {
	super(matchRule);
}

public SearchPattern getBlankPattern() {
	return new SecondaryTypeDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
}
protected StringBuffer print(StringBuffer output) {
	output.append("Secondary"); //$NON-NLS-1$
	return super.print(output);
}

/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
 */
EntryResult[] queryIn(Index index) throws IOException {
	return index.query(this.getIndexCategories(), SECONDARY_PATTERN_KEY, R_PATTERN_MATCH | R_CASE_SENSITIVE);
}

}
