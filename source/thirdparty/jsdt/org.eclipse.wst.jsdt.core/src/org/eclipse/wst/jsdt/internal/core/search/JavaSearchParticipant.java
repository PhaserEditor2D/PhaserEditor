/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchDocument;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.search.indexing.SourceIndexer;
import org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator;

/**
 * A search participant describes a particular extension to a generic search mechanism, allowing thus to
 * perform combined search actions which will involve all required participants
 *
 * A search scope defines which participants are involved.
 *
 * A search participant is responsible for holding index files, and selecting the appropriate ones to feed to
 * index queries. It also can map a document path to an actual document (note that documents could live outside
 * the workspace or no exist yet, and thus aren't just resources).
 */
public class JavaSearchParticipant extends SearchParticipant {

	IndexSelector indexSelector;

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.SearchParticipant#beginSearching()
	 */
	public void beginSearching() {
		super.beginSearching();
		this.indexSelector = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.SearchParticipant#doneSearching()
	 */
	public void doneSearching() {
		this.indexSelector = null;
		super.doneSearching();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.SearchParticipant#getName()
	 */
	public String getDescription() {
		return "JavaScript"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.SearchParticipant#getDocument(String)
	 */
	public SearchDocument getDocument(String documentPath) {
		return new JavaSearchDocument(documentPath, this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.SearchParticipant#indexDocument(SearchDocument)
	 */
	public void indexDocument(SearchDocument document, IPath indexPath) {
		// TODO must verify that the document + indexPath match, when this is not called from scheduleDocumentIndexing
		document.removeAllIndexEntries(); // in case the document was already indexed

		String documentPath = document.getPath();
		if (org.eclipse.wst.jsdt.internal.core.util.Util.isJavaLikeFileName(documentPath)) {
			new SourceIndexer(document).indexDocument();
		} 
		else if (org.eclipse.wst.jsdt.internal.core.util.Util.isMetadataFileName(documentPath)) {
			new SourceIndexer(document).indexMetadata();
		} 
		else if (Util.isArchiveFileName(documentPath)) {
			new SourceIndexer(document).indexArchive();
		}
	}

	/* (non-Javadoc)
	 * @see SearchParticipant#locateMatches(SearchDocument[], SearchPattern, IJavaScriptSearchScope, SearchRequestor, IProgressMonitor)
	 */
	public void locateMatches(SearchDocument[] indexMatches, SearchPattern pattern,
			IJavaScriptSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {

		MatchLocator matchLocator =
			new MatchLocator(
				pattern,
				requestor,
				scope,
				monitor
		);

		/* eliminating false matches and locating them */
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		matchLocator.locateMatches(indexMatches);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.search.SearchParticipant#selectIndexes(org.eclipse.wst.jsdt.core.search.SearchQuery, org.eclipse.wst.jsdt.core.search.SearchContext)
	 */
	public IPath[] selectIndexes(
		SearchPattern pattern,
		IJavaScriptSearchScope scope) {

		if (this.indexSelector == null) {
			this.indexSelector = new IndexSelector(scope, pattern);
		}
		return this.indexSelector.getIndexLocations();
	}

}
