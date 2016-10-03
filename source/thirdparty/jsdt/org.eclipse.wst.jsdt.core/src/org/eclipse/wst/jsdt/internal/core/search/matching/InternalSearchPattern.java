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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchScope;

/**
 * Internal search pattern implementation
 */
public abstract class InternalSearchPattern {

	/**
	 *  The focus element (used for reference patterns)
	 */
	IJavaScriptElement focus;

	int kind;
	boolean mustResolve = true;

	void acceptMatch(String relativePath, String containerPath, SearchPattern pattern, IndexQueryRequestor requestor, SearchParticipant participant, IJavaScriptSearchScope scope) {

		if (scope instanceof JavaSearchScope) {
			JavaSearchScope javaSearchScope = (JavaSearchScope) scope;
			// Get document path access restriction from java search scope
			// Note that requestor has to verify if needed whether the document violates the access restriction or not
			AccessRuleSet access = javaSearchScope.getAccessRuleSet(relativePath, containerPath);
			if (access != JavaSearchScope.NOT_ENCLOSED) { // scope encloses the document path
				String documentPath = documentPath(containerPath, relativePath);
				if (!requestor.acceptIndexMatch(documentPath, pattern, participant, access))
					throw new OperationCanceledException();
			}
		} else {
			String documentPath = documentPath(containerPath, relativePath);
			if (scope.encloses(documentPath))
				if (!requestor.acceptIndexMatch(documentPath, pattern, participant, null))
					throw new OperationCanceledException();

		}
	}
	SearchPattern currentPattern() {
		return (SearchPattern) this;
	}
	String documentPath(String containerPath, String relativePath) {
		/* For some library entries containerPath == relativePath */

		if(containerPath!=null && relativePath!=null) {
			IPath container = new Path(containerPath);
			IPath relative = new Path(relativePath);
			if(container.makeAbsolute() . equals(relative.makeAbsolute())) {
				return relativePath;
			}
		}

		String separator = Util.isArchiveFileName(containerPath) ? IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR : "/"; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer(containerPath.length() + separator.length() + relativePath.length());
		buffer.append(new Path(containerPath).toString());
		buffer.append(separator);
		buffer.append(new Path(relativePath).toString());
		return buffer.toString();
	}
	/**
	 * Query a given index for matching entries. Assumes the sender has opened the index and will close when finished.
	 */
	public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaScriptSearchScope scope, IProgressMonitor monitor) throws IOException {
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		try {
			index.startQuery();
			SearchPattern pattern = currentPattern();
			EntryResult[] entries = ((InternalSearchPattern)pattern).queryIn(index);
			if (entries == null) return;

			SearchPattern decodedResult = pattern.getBlankPattern();
			String containerPath = index.containerPath;
			for (int i = 0, l = entries.length; i < l; i++) {
				if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();

				EntryResult entry = entries[i];
				decodedResult.decodeIndexKey(entry.getWord());
				if (pattern.matchesDecodedKey(decodedResult)) {
					// TODO (kent) some clients may not need the document names
					String[] names = entry.getDocumentNames(index);
					for (int j = 0, n = names.length; j < n; j++) {
						if(!scope.shouldExclude(containerPath, names[j]))
							acceptMatch(names[j], containerPath, decodedResult, requestor, participant, scope);

					}
				}
			}
		} finally {
			index.stopQuery();
		}
	}
	boolean isPolymorphicSearch() {
		return false;
	}
	EntryResult[] queryIn(Index index) throws IOException {
		SearchPattern pattern = (SearchPattern) this;
		return index.query(pattern.getIndexCategories(), pattern.getIndexKey(), pattern.getMatchRule());
	}

}
