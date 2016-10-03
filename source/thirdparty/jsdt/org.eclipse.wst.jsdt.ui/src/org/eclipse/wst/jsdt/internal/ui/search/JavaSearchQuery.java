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
package org.eclipse.wst.jsdt.internal.ui.search;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.search.ElementQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.IMatchPresentation;
import org.eclipse.wst.jsdt.ui.search.IQueryParticipant;
import org.eclipse.wst.jsdt.ui.search.ISearchRequestor;
import org.eclipse.wst.jsdt.ui.search.PatternQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.QuerySpecification;

public class JavaSearchQuery implements ISearchQuery {

	private static final String PERF_SEARCH_PARTICIPANT= "org.eclipse.wst.jsdt.ui/perf/search/participants"; //$NON-NLS-1$

	private ISearchResult fResult;
	private final QuerySpecification fPatternData;
	
	public JavaSearchQuery(QuerySpecification data) {
		if (data == null) {
			throw new IllegalArgumentException("data must not be null"); //$NON-NLS-1$
		}
		fPatternData= data;
	}
	
	private static class SearchRequestor implements ISearchRequestor {
		private IQueryParticipant fParticipant;
		private JavaSearchResult fSearchResult;
		public void reportMatch(Match match) {
			IMatchPresentation participant= fParticipant.getUIParticipant();
			if (participant == null || match.getElement() instanceof IJavaScriptElement || match.getElement() instanceof IResource) {
				fSearchResult.addMatch(match);
			} else {
				fSearchResult.addMatch(match, participant);
			}
		}
		
		protected SearchRequestor(IQueryParticipant participant, JavaSearchResult result) {
			super();
			fParticipant= participant;
			fSearchResult= result;
		}
	}
	
	public IStatus run(IProgressMonitor monitor) {
		final JavaSearchResult textResult= (JavaSearchResult) getSearchResult();
		textResult.removeAll();
		// Don't need to pass in working copies in 3.0 here
		SearchEngine engine= new SearchEngine();
		try {

			int totalTicks= 1000;
			IProject[] projects= JavaSearchScopeFactory.getInstance().getProjects(fPatternData.getScope());
			final SearchParticipantRecord[] participantDescriptors= SearchParticipantsExtensionPoint.getInstance().getSearchParticipants(projects);
			final int[] ticks= new int[participantDescriptors.length];
			for (int i= 0; i < participantDescriptors.length; i++) {
				final int iPrime= i;
				ISafeRunnable runnable= new ISafeRunnable() {
					public void handleException(Throwable exception) {
						ticks[iPrime]= 0;
						String message= SearchMessages.JavaSearchQuery_error_participant_estimate; 
						JavaScriptPlugin.log(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), 0, message, exception));
					}

					public void run() throws Exception {
						ticks[iPrime]= participantDescriptors[iPrime].getParticipant().estimateTicks(fPatternData);
					}
				};
				
				SafeRunner.run(runnable);
				totalTicks+= ticks[i];
			}
			
			SearchPattern pattern;
			String stringPattern;
			
			if (fPatternData instanceof ElementQuerySpecification) {
				IJavaScriptElement element= ((ElementQuerySpecification) fPatternData).getElement();
				stringPattern= JavaScriptElementLabels.getElementLabel(element, JavaScriptElementLabels.ALL_DEFAULT);
				if (!element.exists()) {
					return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), 0, Messages.format(SearchMessages.JavaSearchQuery_error_element_does_not_exist, stringPattern), null);  
				}
				pattern= SearchPattern.createPattern(element, fPatternData.getLimitTo(), SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
			} else {
				PatternQuerySpecification patternSpec = (PatternQuerySpecification) fPatternData;
				stringPattern= patternSpec.getPattern();
				int matchMode= getMatchMode(stringPattern) | SearchPattern.R_ERASURE_MATCH;
				if (patternSpec.isCaseSensitive())
					matchMode |= SearchPattern.R_CASE_SENSITIVE;
				pattern= SearchPattern.createPattern(patternSpec.getPattern(), patternSpec.getSearchFor(), patternSpec.getLimitTo(), matchMode);
			}
			
			if (pattern == null) {
				return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), 0, Messages.format(SearchMessages.JavaSearchQuery_error_unsupported_pattern, stringPattern), null);  
			}
			monitor.beginTask(Messages.format(SearchMessages.JavaSearchQuery_task_label, stringPattern), totalTicks); 
			IProgressMonitor mainSearchPM= new SubProgressMonitor(monitor, 1000);

			boolean ignorePotentials= NewSearchUI.arePotentialMatchesIgnored();
			NewSearchResultCollector collector= new NewSearchResultCollector(textResult, ignorePotentials);
			
			
			engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, fPatternData.getScope(), collector, mainSearchPM);
			for (int i= 0; i < participantDescriptors.length; i++) {
				final ISearchRequestor requestor= new SearchRequestor(participantDescriptors[i].getParticipant(), textResult);
				final IProgressMonitor participantPM= new SubProgressMonitor(monitor, ticks[i]);

				final int iPrime= i;
				ISafeRunnable runnable= new ISafeRunnable() {
					public void handleException(Throwable exception) {
						participantDescriptors[iPrime].getDescriptor().disable();
						String message= SearchMessages.JavaSearchQuery_error_participant_search; 
						JavaScriptPlugin.log(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), 0, message, exception));
					}

					public void run() throws Exception {

						final IQueryParticipant participant= participantDescriptors[iPrime].getParticipant();

						final PerformanceStats stats= PerformanceStats.getStats(PERF_SEARCH_PARTICIPANT, participant);
						stats.startRun();

						participant.search(requestor, fPatternData, participantPM);

						stats.endRun();
					}
				};
				
				SafeRunner.run(runnable);
			}
			
		} catch (CoreException e) {
			return e.getStatus();
		}
		String message= Messages.format(SearchMessages.JavaSearchQuery_status_ok_message, String.valueOf(textResult.getMatchCount())); 
		return new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), 0, message, null);
	}
	
	private int getMatchMode(String pattern) {
		if (pattern.indexOf('*') != -1 || pattern.indexOf('?') != -1) {
			return SearchPattern.R_PATTERN_MATCH;
		} else if (SearchUtils.isCamelCasePattern(pattern)) {
			return SearchPattern.R_CAMELCASE_MATCH;
		}
		return SearchPattern.R_EXACT_MATCH;
	}

	public String getLabel() {
		return SearchMessages.JavaSearchQuery_label; 
	}

	public String getResultLabel(int nMatches) {
		if (nMatches == 1) {
			String[] args= { getSearchPatternDescription(), fPatternData.getScopeDescription() };
			switch (fPatternData.getLimitTo()) {
				case IJavaScriptSearchConstants.IMPLEMENTORS:
					return Messages.format(SearchMessages.JavaSearchOperation_singularImplementorsPostfix, args); 
				case IJavaScriptSearchConstants.DECLARATIONS:
					return Messages.format(SearchMessages.JavaSearchOperation_singularDeclarationsPostfix, args); 
				case IJavaScriptSearchConstants.REFERENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularReferencesPostfix, args); 
				case IJavaScriptSearchConstants.ALL_OCCURRENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularOccurrencesPostfix, args); 
				case IJavaScriptSearchConstants.READ_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularReadReferencesPostfix, args); 
				case IJavaScriptSearchConstants.WRITE_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularWriteReferencesPostfix, args); 
				default:
					return Messages.format(SearchMessages.JavaSearchOperation_singularOccurrencesPostfix, args); 
			}
		} else {
			Object[] args= { getSearchPatternDescription(), Integer.valueOf(nMatches), fPatternData.getScopeDescription() };
			switch (fPatternData.getLimitTo()) {
				case IJavaScriptSearchConstants.IMPLEMENTORS:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralImplementorsPostfix, args); 
				case IJavaScriptSearchConstants.DECLARATIONS:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralDeclarationsPostfix, args); 
				case IJavaScriptSearchConstants.REFERENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralReferencesPostfix, args); 
				case IJavaScriptSearchConstants.ALL_OCCURRENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralOccurrencesPostfix, args); 
				case IJavaScriptSearchConstants.READ_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralReadReferencesPostfix, args); 
				case IJavaScriptSearchConstants.WRITE_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralWriteReferencesPostfix, args); 
				default:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralOccurrencesPostfix, args); 
			}
		}
	}
	
	private String getSearchPatternDescription() {
		if (fPatternData instanceof ElementQuerySpecification) {
			IJavaScriptElement element= ((ElementQuerySpecification) fPatternData).getElement();
			return JavaScriptElementLabels.getElementLabel(element, JavaScriptElementLabels.ALL_DEFAULT
					| JavaScriptElementLabels.ALL_FULLY_QUALIFIED | JavaScriptElementLabels.USE_RESOLVED);
		} 
		return ((PatternQuerySpecification) fPatternData).getPattern();
	}

	ImageDescriptor getImageDescriptor() {
		if (fPatternData.getLimitTo() == IJavaScriptSearchConstants.IMPLEMENTORS || fPatternData.getLimitTo() == IJavaScriptSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}

	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public ISearchResult getSearchResult() {
		if (fResult == null) {
			JavaSearchResult result= new JavaSearchResult(this);
			new SearchResultUpdater(result);
			fResult= result;
		}
		return fResult;
	}
	
	QuerySpecification getSpecification() {
		return fPatternData;
	}
}
