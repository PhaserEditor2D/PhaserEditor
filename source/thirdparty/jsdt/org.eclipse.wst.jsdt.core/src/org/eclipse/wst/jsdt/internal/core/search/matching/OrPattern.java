/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;

public class OrPattern extends SearchPattern implements IIndexConstants {

	int matchCompatibility;
	protected SearchPattern[] patterns;

	public OrPattern(SearchPattern leftPattern, SearchPattern rightPattern) {
		super(Math.max(leftPattern.getMatchRule(), rightPattern.getMatchRule()));
		((InternalSearchPattern)this).kind = OR_PATTERN;
		((InternalSearchPattern)this).mustResolve = ((InternalSearchPattern) leftPattern).mustResolve || ((InternalSearchPattern) rightPattern).mustResolve;

		SearchPattern[] leftPatterns = leftPattern instanceof OrPattern ? ((OrPattern) leftPattern).patterns : null;
		SearchPattern[] rightPatterns = rightPattern instanceof OrPattern ? ((OrPattern) rightPattern).patterns : null;
		int leftSize = leftPatterns == null ? 1 : leftPatterns.length;
		int rightSize = rightPatterns == null ? 1 : rightPatterns.length;
		this.patterns = new SearchPattern[leftSize + rightSize];

		if (leftPatterns == null)
			this.patterns[0] = leftPattern;
		else
			System.arraycopy(leftPatterns, 0, this.patterns, 0, leftSize);
		if (rightPatterns == null)
			this.patterns[leftSize] = rightPattern;
		else
			System.arraycopy(rightPatterns, 0, this.patterns, leftSize, rightSize);

		// Store erasure match
		matchCompatibility = 0;
		for (int i = 0, length = this.patterns.length; i < length; i++) {
			matchCompatibility |= ((JavaSearchPattern) this.patterns[i]).matchCompatibility;
		}
	}
	public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaScriptSearchScope scope, IProgressMonitor progressMonitor) throws IOException {
		// per construction, OR pattern can only be used with a PathCollector (which already gather results using a set)
		try {
			index.startQuery();
			for (int i = 0, length = this.patterns.length; i < length; i++)
				((InternalSearchPattern)this.patterns[i]).findIndexMatches(index, requestor, participant, scope, progressMonitor);
		} finally {
			index.stopQuery();
		}
	}

	public SearchPattern getBlankPattern() {
		return null;
	}

	public SearchPattern findPatternKind(int patternKind) {
		for (int i = 0; i < patterns.length; i++) {
			if (((InternalSearchPattern)patterns[i]).kind == patternKind) {
				return patterns[i];
			}
		}
		return null;
	}
	
	/**
	 * Whether this pattern is erasure match.
	 * @return boolean isErasureMatch;
	 */
	boolean isErasureMatch() {
		return (this.matchCompatibility & R_ERASURE_MATCH) != 0;
	}

	boolean isPolymorphicSearch() {
		for (int i = 0, length = this.patterns.length; i < length; i++)
			if (((InternalSearchPattern) this.patterns[i]).isPolymorphicSearch()) return true;
		return false;
	}

	/**
	 * Returns whether the pattern has signatures or not.
	 * @return true if one at least of the stored pattern has signatures.
	 */
	public final boolean hasSignatures() {
		boolean isErasureMatch = isErasureMatch();
		for (int i = 0, length = this.patterns.length; i < length && !isErasureMatch; i++) {
			if (((JavaSearchPattern) this.patterns[i]).hasSignatures()) return true;
		}
		return false;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.patterns[0].toString());
		for (int i = 1, length = this.patterns.length; i < length; i++) {
			buffer.append("\n| "); //$NON-NLS-1$
			buffer.append(this.patterns[i].toString());
		}
		return buffer.toString();
	}
}
