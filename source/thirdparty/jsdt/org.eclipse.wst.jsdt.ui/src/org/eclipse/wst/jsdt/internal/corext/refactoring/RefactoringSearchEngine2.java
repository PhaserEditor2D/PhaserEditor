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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;

/**
 * Helper class to use the search engine in refactorings.
 * 
 * 
 */
public final class RefactoringSearchEngine2 {

	/** Default implementation of a search requestor */
	private static class DefaultSearchRequestor implements IRefactoringSearchRequestor {

		public final SearchMatch acceptSearchMatch(final SearchMatch match) {
			return match;
		}
	}

	/** Search requestor which only collects compilation units */
	private class RefactoringCompilationUnitCollector extends RefactoringSearchCollector {

		/** The collected compilation units */
		private final Set fCollectedUnits= new HashSet();

		/** The inaccurate matches */
		private final Set fInaccurateMatches= new HashSet();

		public final void acceptSearchMatch(final SearchMatch match) throws CoreException {
			final SearchMatch accepted= fRequestor.acceptSearchMatch(match);
			if (accepted != null) {
				final IResource resource= accepted.getResource();
				if (!resource.equals(fLastResource)) {
					final IJavaScriptElement element= JavaScriptCore.create(resource);
					if (element instanceof IJavaScriptUnit)
						fCollectedUnits.add(element);
				}
				if (fInaccurate && accepted.getAccuracy() == SearchMatch.A_INACCURATE && !fInaccurateMatches.contains(accepted)) {
					fStatus.addEntry(fSeverity, Messages.format(RefactoringCoreMessages.RefactoringSearchEngine_inaccurate_match, accepted.getResource().getName()), null, null, RefactoringStatusEntry.NO_CODE); 
					fInaccurateMatches.add(accepted);
				}
			}
		}

		public final void clearResults() {
			super.clearResults();
			fCollectedUnits.clear();
			fInaccurateMatches.clear();
		}

		public final Collection getBinaryResources() {
			return Collections.EMPTY_SET;
		}

		public final Collection getCollectedMatches() {
			return fCollectedUnits;
		}

		public final Collection getInaccurateMatches() {
			return fInaccurateMatches;
		}
	}

	private abstract class RefactoringSearchCollector extends SearchRequestor {

		protected IResource fLastResource= null;

		public void clearResults() {
			fLastResource= null;
		}

		public abstract Collection getBinaryResources();

		public abstract Collection getCollectedMatches();

		public abstract Collection getInaccurateMatches();
	}

	/** Search requestor which collects every search match */
	private class RefactoringSearchMatchCollector extends RefactoringSearchCollector {

		/** The binary resources */
		private final Set fBinaryResources= new HashSet();

		/** The collected matches */
		private final List fCollectedMatches= new ArrayList();

		/** The inaccurate matches */
		private final Set fInaccurateMatches= new HashSet();

		public final void acceptSearchMatch(final SearchMatch match) throws CoreException {
			final SearchMatch accepted= fRequestor.acceptSearchMatch(match);
			if (accepted != null) {
				fCollectedMatches.add(accepted);
				final IResource resource= accepted.getResource();
				if (!resource.equals(fLastResource)) {
					if (fBinary) {
						final IJavaScriptElement element= JavaScriptCore.create(resource);
						if (!(element instanceof IJavaScriptUnit)) {
							final IProject project= resource.getProject();
							if (!fGrouping)
								fStatus.addEntry(fSeverity, Messages.format(RefactoringCoreMessages.RefactoringSearchEngine_binary_match_ungrouped, project.getName()), null, null, RefactoringStatusEntry.NO_CODE); 
							else if (!fBinaryResources.contains(resource))
								fStatus.addEntry(fSeverity, Messages.format(RefactoringCoreMessages.RefactoringSearchEngine_binary_match_grouped, project.getName()), null, null, RefactoringStatusEntry.NO_CODE); 
							fBinaryResources.add(resource);
						}
					}
					if (fInaccurate && accepted.getAccuracy() == SearchMatch.A_INACCURATE && !fInaccurateMatches.contains(accepted)) {
						fStatus.addEntry(fSeverity, Messages.format(RefactoringCoreMessages.RefactoringSearchEngine_inaccurate_match, resource.getName()), null, null, RefactoringStatusEntry.NO_CODE); 
						fInaccurateMatches.add(accepted);
					}
				}
			}
		}

		public final void clearResults() {
			super.clearResults();
			fCollectedMatches.clear();
			fInaccurateMatches.clear();
			fBinaryResources.clear();
		}

		public final Collection getBinaryResources() {
			return fBinaryResources;
		}

		public final Collection getCollectedMatches() {
			return fCollectedMatches;
		}

		public final Collection getInaccurateMatches() {
			return fInaccurateMatches;
		}
	}

	/** The compilation unit granularity */
	public static final int GRANULARITY_COMPILATION_UNIT= 2;

	/** The search match granularity */
	public static final int GRANULARITY_SEARCH_MATCH= 1;

	/** Should binary matches be filtered? */
	private boolean fBinary= false;

	/** The refactoring search collector */
	private RefactoringSearchCollector fCollector= null;

	/** The search granularity */
	private int fGranularity= GRANULARITY_SEARCH_MATCH;

	/** Should the matches be grouped by resource? */
	private boolean fGrouping= true;

	/** Should inaccurate matches be filtered? */
	private boolean fInaccurate= true;

	/** The working copy owner, or <code>null</code> */
	private WorkingCopyOwner fOwner= null;

	/** The search pattern, or <code>null</code> */
	private SearchPattern fPattern= null;

	/** The search requestor */
	private IRefactoringSearchRequestor fRequestor= new DefaultSearchRequestor();

	/** The search scope */
	private IJavaScriptSearchScope fScope= SearchEngine.createWorkspaceScope();

	/** The severity */
	private int fSeverity= RefactoringStatus.WARNING;

	/** The search status */
	private RefactoringStatus fStatus= new RefactoringStatus();

	/** The working copies */
	private IJavaScriptUnit[] fWorkingCopies= {};
	
	/**
	 * Creates a new refactoring search engine.
	 */
	public RefactoringSearchEngine2() {
		// Do nothing
	}

	/**
	 * Creates a new refactoring search engine.
	 * 
	 * @param pattern the search pattern
	 */
	public RefactoringSearchEngine2(final SearchPattern pattern) {
		Assert.isNotNull(pattern);
		fPattern= pattern;
	}

	/**
	 * Clears all results found so far, and sets resets the status to {@link RefactoringStatus#OK}.
	 */
	public final void clearResults() {
		getCollector().clearResults();
		fStatus= new RefactoringStatus();
	}

	/**
	 * Returns the affected compilation units of the previous search queries.
	 * <p>
	 * In order to retrieve the compilation units, grouping by resource must have been enabled before searching.
	 * 
	 * @return the compilation units of the previous queries
	 */
	public final IJavaScriptUnit[] getAffectedCompilationUnits() {
		if (fGranularity == GRANULARITY_COMPILATION_UNIT) {
			final Collection collection= getCollector().getCollectedMatches();
			final IJavaScriptUnit[] units= new IJavaScriptUnit[collection.size()];
			int index= 0;
			for (final Iterator iterator= collection.iterator(); iterator.hasNext(); index++)
				units[index]= (IJavaScriptUnit) iterator.next();
			return units;
		} else {
			final SearchResultGroup[] groups= getGroupedMatches();
			final IJavaScriptUnit[] units= new IJavaScriptUnit[groups.length];
			for (int index= 0; index < groups.length; index++)
				units[index]= groups[index].getCompilationUnit();
			return units;
		}
	}

	/**
	 * Returns the affected java projects of the previous search queries.
	 * <p>
	 * In order to retrieve the java projects, grouping by resource must have been enabled before searching.
	 * 
	 * @return the java projects of the previous queries (element type: <code>&ltIJavaProject, Collection&ltSearchResultGroup&gt&gt</code>)
	 */
	public final Map getAffectedProjects() {
		final Map map= new HashMap();
		IJavaScriptProject project= null;
		IJavaScriptUnit unit= null;
		if (fGranularity == GRANULARITY_COMPILATION_UNIT) {
			final IJavaScriptUnit[] units= getAffectedCompilationUnits();
			for (int index= 0; index < units.length; index++) {
				unit= units[index];
				project= unit.getJavaScriptProject();
				if (project != null) {
					Set set= (Set) map.get(project);
					if (set == null) {
						set= new HashSet();
						map.put(project, set);
					}
					set.add(unit);
				}
			}
		} else {
			final SearchResultGroup[] groups= getGroupedMatches();
			SearchResultGroup group= null;
			for (int index= 0; index < groups.length; index++) {
				group= groups[index];
				unit= group.getCompilationUnit();
				if (unit != null) {
					project= unit.getJavaScriptProject();
					if (project != null) {
						Set set= (Set) map.get(project);
						if (set == null) {
							set= new HashSet();
							map.put(project, set);
						}
						set.add(group);
					}
				}
			}
		}
		return map;
	}

	/**
	 * Returns the refactoring search collector.
	 * 
	 * @return the refactoring search collector
	 */
	private RefactoringSearchCollector getCollector() {
		if (fCollector == null) {
			if (fGranularity == GRANULARITY_COMPILATION_UNIT)
				fCollector= new RefactoringCompilationUnitCollector();
			else if (fGranularity == GRANULARITY_SEARCH_MATCH)
				fCollector= new RefactoringSearchMatchCollector();
			else
				Assert.isTrue(false);
		}
		return fCollector;
	}

	/**
	 * Returns the found search matches in grouped by their containing resource.
	 * 
	 * @return the found search matches
	 */
	private SearchResultGroup[] getGroupedMatches() {
		final Map grouped= new HashMap();
		List matches= null;
		IResource resource= null;
		SearchMatch match= null;
		for (final Iterator iterator= getSearchMatches().iterator(); iterator.hasNext();) {
			match= (SearchMatch) iterator.next();
			resource= match.getResource();
			if (!grouped.containsKey(resource))
				grouped.put(resource, new ArrayList(4));
			matches= (List) grouped.get(resource);
			matches.add(match);
		}
		if (fBinary) {
			final Collection collection= getCollector().getBinaryResources();
			for (final Iterator iterator= grouped.keySet().iterator(); iterator.hasNext();) {
				resource= (IResource) iterator.next();
				if (collection.contains(resource))
					iterator.remove();
			}
		}
		final SearchResultGroup[] result= new SearchResultGroup[grouped.keySet().size()];
		int index= 0;
		for (final Iterator iterator= grouped.keySet().iterator(); iterator.hasNext();) {
			resource= (IResource) iterator.next();
			matches= (List) grouped.get(resource);
			result[index++]= new SearchResultGroup(resource, ((SearchMatch[]) matches.toArray(new SearchMatch[matches.size()])));
		}
		return result;
	}

	/**
	 * Returns the search pattern currently used for searching.
	 * 
	 * @return the search pattern
	 */
	public final SearchPattern getPattern() {
		return fPattern;
	}

	/**
	 * Returns the results of the previous search queries.
	 * <p>
	 * The result depends on the following conditions:
	 * <ul>
	 * <li>If the search granularity is {@link #GRANULARITY_COMPILATION_UNIT}, the results are elements of type {@link IJavaScriptUnit}.</li>
	 * <li>If grouping by resource is enabled, the results are elements of type {@link SearchResultGroup}, otherwise the elements are of type {@link SearchMatch}.</li>
	 * </ul>
	 * 
	 * @return the results of the previous queries
	 */
	public final Object[] getResults() {
		if (fGranularity == GRANULARITY_COMPILATION_UNIT)
			return getAffectedCompilationUnits();
		else {
			if (fGrouping)
				return getGroupedMatches();
			else
				return getUngroupedMatches();
		}
	}

	/**
	 * Returns the search matches filtered by their accuracy.
	 * 
	 * @return the filtered search matches
	 */
	private Collection getSearchMatches() {
		Collection results= null;
		if (fInaccurate) {
			results= new LinkedList(getCollector().getCollectedMatches());
			final Collection collection= getCollector().getInaccurateMatches();
			SearchMatch match= null;
			for (final Iterator iterator= results.iterator(); iterator.hasNext();) {
				match= (SearchMatch) iterator.next();
				if (collection.contains(match))
					iterator.remove();
			}
		} else
			results= getCollector().getCollectedMatches();
		return results;
	}

	/**
	 * Returns the refactoring status of this search engine.
	 * 
	 * @return the refactoring status
	 */
	public final RefactoringStatus getStatus() {
		return fStatus;
	}

	/**
	 * Returns the found search matches in no particular order.
	 * 
	 * @return the found search matches
	 */
	private SearchMatch[] getUngroupedMatches() {
		Collection results= null;
		if (fBinary) {
			results= new LinkedList(getSearchMatches());
			final Collection collection= getCollector().getBinaryResources();
			SearchMatch match= null;
			for (final Iterator iterator= results.iterator(); iterator.hasNext();) {
				match= (SearchMatch) iterator.next();
				if (collection.contains(match.getResource()))
					iterator.remove();
			}
		} else
			results= getSearchMatches();
		final SearchMatch[] matches= new SearchMatch[results.size()];
		results.toArray(matches);
		return matches;
	}

	/**
	 * Performs the search according to the specified pattern.
	 * 
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void searchPattern(final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(fPattern);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.RefactoringSearchEngine_searching_occurrences); 
			try {
				SearchEngine engine= null;
				if (fOwner != null)
					engine= new SearchEngine(fOwner);
				else
					engine= new SearchEngine(fWorkingCopies);
				engine.search(fPattern, SearchUtils.getDefaultSearchParticipants(), fScope, getCollector(), new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			} catch (CoreException exception) {
				throw new JavaScriptModelException(exception);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Performs the search of referenced fields.
	 * 
	 * @param element the java element whose referenced fields have to be found
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void searchReferencedFields(final IJavaScriptElement element, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(element);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.RefactoringSearchEngine_searching_referenced_fields); 
			try {
				SearchEngine engine= null;
				if (fOwner != null)
					engine= new SearchEngine(fOwner);
				else
					engine= new SearchEngine(fWorkingCopies);
				engine.searchDeclarationsOfAccessedFields(element, getCollector(), new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			} catch (CoreException exception) {
				throw new JavaScriptModelException(exception);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Performs the search of referenced methods.
	 * 
	 * @param element the java element whose referenced methods have to be found
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void searchReferencedMethods(final IJavaScriptElement element, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(element);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.RefactoringSearchEngine_searching_referenced_methods); 
			try {
				SearchEngine engine= null;
				if (fOwner != null)
					engine= new SearchEngine(fOwner);
				else
					engine= new SearchEngine(fWorkingCopies);
				engine.searchDeclarationsOfSentMessages(element, getCollector(), new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			} catch (CoreException exception) {
				throw new JavaScriptModelException(exception);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Performs the search of referenced types.
	 * 
	 * @param element the java element whose referenced types have to be found
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void searchReferencedTypes(final IJavaScriptElement element, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(element);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.RefactoringSearchEngine_searching_referenced_types); 
			try {
				SearchEngine engine= null;
				if (fOwner != null)
					engine= new SearchEngine(fOwner);
				else
					engine= new SearchEngine(fWorkingCopies);
				engine.searchDeclarationsOfReferencedTypes(element, getCollector(), new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			} catch (CoreException exception) {
				throw new JavaScriptModelException(exception);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets the conjunction of search patterns to be used during search.
	 * <p>
	 * This method must be called before {@link RefactoringSearchEngine2#searchPattern(IProgressMonitor)}
	 * 
	 * @param first the first search pattern to set
	 * @param second the second search pattern to set
	 */
	public final void setAndPattern(final SearchPattern first, final SearchPattern second) {
		Assert.isNotNull(first);
		Assert.isNotNull(second);
		fPattern= SearchPattern.createAndPattern(first, second);
	}

	/**
	 * Determines how search matches are filtered.
	 * <p>
	 * This method must be called before start searching. The default is to filter inaccurate matches only.
	 * 
	 * @param inaccurate <code>true</code> to filter inaccurate matches, <code>false</code> otherwise
	 * @param binary <code>true</code> to filter binary matches, <code>false</code> otherwise
	 */
	public final void setFiltering(final boolean inaccurate, final boolean binary) {
		fInaccurate= inaccurate;
		fBinary= binary;
	}

	/**
	 * Sets the granularity to use during the searches.
	 * <p>
	 * This method must be called before start searching. The default is a granularity of {@link #GRANULARITY_SEARCH_MATCH}.
	 * 
	 * @param granularity The granularity to use. Must be one of the <code>GRANULARITY_XXX</code> constants.
	 */
	public final void setGranularity(final int granularity) {
		Assert.isTrue(granularity == GRANULARITY_COMPILATION_UNIT || granularity == GRANULARITY_SEARCH_MATCH);
		fGranularity= granularity;
	}

	/**
	 * Sets the working copies to take precedence during the searches.
	 * <p>
	 * This method must be called before start searching. The default is to use no working copies
	 * 
	 * @param copies the working copies to use
	 */
	public final void setWorkingCopies(final IJavaScriptUnit[] copies) {
		Assert.isNotNull(copies);
		fWorkingCopies= new IJavaScriptUnit[copies.length];
		System.arraycopy(copies, 0, fWorkingCopies, 0, copies.length);
	}

	/**
	 * Determines how search matches are grouped.
	 * <p>
	 * This method must be called before start searching. The default is to group by containing resource.
	 * 
	 * @param grouping <code>true</code> to group matches by their containing resource, <code>false</code> otherwise
	 */
	public final void setGrouping(final boolean grouping) {
		fGrouping= grouping;
	}

	/**
	 * Sets the disjunction of search patterns to be used during search.
	 * <p>
	 * This method must be called before {@link RefactoringSearchEngine2#searchPattern(IProgressMonitor)}
	 * 
	 * @param first the first search pattern to set
	 * @param second the second search pattern to set
	 */
	public final void setOrPattern(final SearchPattern first, final SearchPattern second) {
		Assert.isNotNull(first);
		Assert.isNotNull(second);
		fPattern= SearchPattern.createOrPattern(first, second);
	}

	/**
	 * Sets the working copy owner to use during search.
	 * <p>
	 * This method must be called before start searching. The default is to use no working copy owner.
	 * 
	 * @param owner the working copy owner to use, or <code>null</code> to use none
	 */
	public final void setOwner(final WorkingCopyOwner owner) {
		fOwner= owner;
	}

	/**
	 * Sets the search pattern to be used during search.
	 * <p>
	 * This method must be called before {@link RefactoringSearchEngine2#searchPattern(IProgressMonitor)}
	 * 
	 * @param elements the set of elements
	 * @param limitTo determines the nature of the expected matches. This is a combination of {@link org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants}.
	 */
	public final void setPattern(final IJavaScriptElement[] elements, final int limitTo) {
		Assert.isNotNull(elements);
		Assert.isTrue(elements.length > 0);
		SearchPattern pattern= SearchPattern.createPattern(elements[0], limitTo, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		IJavaScriptElement element= null;
		for (int index= 1; index < elements.length; index++) {
			element= elements[index];
			pattern= SearchPattern.createOrPattern(pattern, SearchPattern.createPattern(element, limitTo, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		}
		setPattern(pattern);
	}

	/**
	 * Sets the search pattern to be used during search.
	 * <p>
	 * This method must be called before {@link RefactoringSearchEngine2#searchPattern(IProgressMonitor)}
	 * 
	 * @param pattern the search pattern to set
	 */
	public final void setPattern(final SearchPattern pattern) {
		Assert.isNotNull(pattern);
		fPattern= pattern;
	}

	/**
	 * Sets the search requestor for this search engine.
	 * <p>
	 * This method must be called before start searching. The default is a non-filtering search requestor.
	 * 
	 * @param requestor the search requestor to set
	 */
	public final void setRequestor(final IRefactoringSearchRequestor requestor) {
		Assert.isNotNull(requestor);
		fRequestor= requestor;
	}

	/**
	 * Sets the search scope for this search engine.
	 * <p>
	 * This method must be called before start searching. The default is the entire workspace as search scope.
	 * 
	 * @param scope the search scope to set
	 */
	public final void setScope(final IJavaScriptSearchScope scope) {
		Assert.isNotNull(scope);
		fScope= scope;
	}

	/**
	 * Sets the severity of the generated status entries.
	 * <p>
	 * This method must be called before start searching. The default is a severity of {@link RefactoringStatus#OK}.
	 * 
	 * @param severity the severity to set
	 */
	public final void setSeverity(final int severity) {
		Assert.isTrue(severity == RefactoringStatus.WARNING || severity == RefactoringStatus.INFO || severity == RefactoringStatus.FATAL || severity == RefactoringStatus.ERROR);
		fSeverity= severity;
	}

	/**
	 * Sets the refactoring status for this search engine.
	 * <p>
	 * This method must be called before start searching. The default is an empty status with status {@link RefactoringStatus#OK}.
	 * 
	 * @param status the refactoring status to set
	 */
	public final void setStatus(final RefactoringStatus status) {
		Assert.isNotNull(status);
		fStatus= status;
	}
}
