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

package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;


public class OccurrencesSearchQuery implements ISearchQuery {

	private final OccurrencesSearchResult fResult;
	private IOccurrencesFinder fFinder;
	private IDocument fDocument;
	private final IJavaScriptElement fElement;
	private final String fJobLabel;
	private final String fSingularLabel;
	private final String fPluralLabel;
	private final String fName;
	
	public OccurrencesSearchQuery(IOccurrencesFinder finder, IDocument document, IJavaScriptElement element) {
		fFinder= finder;
		fDocument= document;
		fElement= element;
		fJobLabel= fFinder.getJobLabel();
		fResult= new OccurrencesSearchResult(this);
		fSingularLabel= fFinder.getUnformattedSingularLabel();
		fPluralLabel= fFinder.getUnformattedPluralLabel();
		fName= fFinder.getElementName();
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		if (fFinder == null) {
			return new StatusInfo(IStatus.ERROR, "Query has already been running"); //$NON-NLS-1$
		}
		try {
			fFinder.perform();
			ArrayList resultingMatches= new ArrayList();
			fFinder.collectOccurrenceMatches(fElement, fDocument, resultingMatches);
			if (!resultingMatches.isEmpty()) {
				fResult.addMatches((Match[]) resultingMatches.toArray(new Match[resultingMatches.size()]));
			}
			//Don't leak AST:
			fFinder= null;
			fDocument= null;
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return fJobLabel;
	}
	
	public String getResultLabel(int nMatches) {
		if (nMatches == 1) {
			return Messages.format(fSingularLabel, new Object[] { fName, fElement.getElementName() });
		} else {
			return Messages.format(fPluralLabel, new Object[] { fName, Integer.valueOf(nMatches), fElement.getElementName() });
		}
	}
		
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return false; // must release finder to not keep AST reference
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	public boolean canRunInBackground() {
		return true;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		return fResult;
	}
}
