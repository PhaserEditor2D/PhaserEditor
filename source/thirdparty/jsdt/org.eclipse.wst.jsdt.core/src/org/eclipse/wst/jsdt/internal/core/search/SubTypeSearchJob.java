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
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.index.Index;

public class SubTypeSearchJob extends PatternSearchJob {

SimpleSet indexes = new SimpleSet(5);

public SubTypeSearchJob(SearchPattern pattern, SearchParticipant participant, IJavaScriptSearchScope scope, IndexQueryRequestor requestor) {
	super(pattern, participant, scope, requestor);
}
public void finished() {
	Object[] values = this.indexes.values;
	for (int i = 0, l = values.length; i < l; i++)
		if (values[i] != null)
			((Index) values[i]).stopQuery();
}
public boolean search(Index index, IProgressMonitor progressMonitor) {
	if (index == null) return COMPLETE;
	if (indexes.addIfNotIncluded(index) == index)
		index.startQuery();
	return super.search(index, progressMonitor);
}
}
