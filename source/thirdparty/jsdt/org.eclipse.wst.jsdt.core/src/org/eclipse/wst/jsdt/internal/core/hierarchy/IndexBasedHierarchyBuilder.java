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
package org.eclipse.wst.jsdt.internal.core.hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.IPathRequestor;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.Member;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.PackageFragment;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;
import org.eclipse.wst.jsdt.internal.core.search.BasicSearchEngine;
import org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessTypeRequestor;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.wst.jsdt.internal.core.search.PatternSearchJob;
import org.eclipse.wst.jsdt.internal.core.search.SubTypeSearchJob;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IndexManager;
import org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator;
import org.eclipse.wst.jsdt.internal.core.search.matching.SuperTypeReferencePattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.util.HandleFactory;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class IndexBasedHierarchyBuilder extends HierarchyBuilder implements SuffixConstants {
	/** heuristic so that there still progress for deep hierarchies */
	public static final int MAXTICKS = 800;
	
	/**
	 * A temporary cache of compilation units to handles to speed info to
	 * handle translation - it only contains the entries for the types in the
	 * region (in other words, it contains no supertypes outside the region).
	 */
	protected Map cuToHandle;

	/**
	 * The scope this hierarchy builder should restrain results to.
	 */
	protected IJavaScriptSearchScope scope;

	/**
	 * Collection used to queue subtype index queries
	 */
	static class Queue {
		public char[][] names = new char[10][];
		public int start = 0;
		public int end = -1;

		public void add(char[] name) {
			if (++this.end == this.names.length) {
				this.end -= this.start;
				System.arraycopy(this.names, this.start, this.names = new char[this.end * 2][], 0, this.end);
				this.start = 0;
			}
			this.names[this.end] = name;
		}

		public char[] retrieve() {
			if (this.start > this.end) {
				return null; // none
			}

			char[] name = this.names[this.start++];
			if (this.start > this.end) {
				this.start = 0;
				this.end = -1;
			}
			return name;
		}
		
		/**
		 * <p>
		 * Determines if the queue contains the given needle.
		 * </p>
		 * 
		 * @param needle
		 *            determine if this needle is in the queue
		 * @return <code>true</code> if the given needle is in the queue,
		 *         <code>false</code> otherwise
		 */
		public boolean contains(char[] needle) {
			boolean contains = false;
			
			if(this.start <= this.end) {
				for(int i = this.start; i <= this.end && !contains; ++i) {
					contains = CharOperation.equals(needle, this.names[i]);
				}
			}
			
			return contains;
		}

		public String toString() {
			StringBuffer buffer = new StringBuffer("Queue:\n"); //$NON-NLS-1$
			for (int i = this.start; i <= this.end; i++) {
				buffer.append(this.names[i]).append('\n');
			}
			return buffer.toString();
		}
	}

	public IndexBasedHierarchyBuilder(TypeHierarchy hierarchy, IJavaScriptSearchScope scope) throws JavaScriptModelException {
		super(hierarchy);
		this.cuToHandle = new HashMap(5);
		this.scope = scope;
	}

	public void build(boolean computeSubtypes) {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		try {
			// optimize access to zip files while building hierarchy
			manager.cacheZipFiles();

			if (computeSubtypes) {
				// Note by construction there always is a focus type here
				IType focusType = getType();
				boolean focusIsObject = focusType.getElementName().equals(new String(IIndexConstants.OBJECT));
				
				// percentage of work needed to get possible subtypes
				int amountOfWorkForSubtypes = focusIsObject ? 5 : 80; 
				IProgressMonitor possibleSubtypesMonitor = this.hierarchy.progressMonitor == null ? null : new SubProgressMonitor(this.hierarchy.progressMonitor, amountOfWorkForSubtypes);
				
				String[] allPossibleSubtypesPaths;
				if (((Member) focusType).getOuterMostLocalContext() == null) {
					// top level or member type
					allPossibleSubtypesPaths = this.determinePossibleSubTypesFilePaths(possibleSubtypesMonitor);
				}
				else {
					// local or anonymous type
					allPossibleSubtypesPaths = CharOperation.NO_STRINGS;
				}
				
				if (allPossibleSubtypesPaths != null) {
					IProgressMonitor buildMonitor = this.hierarchy.progressMonitor == null ? null : new SubProgressMonitor(this.hierarchy.progressMonitor, 100 - amountOfWorkForSubtypes);
					this.hierarchy.initialize(allPossibleSubtypesPaths.length);
					this.buildFromPotentialSubtypeFilepaths(allPossibleSubtypesPaths, new HashSet(10), buildMonitor);
				}
			}
			else {
				this.hierarchy.initialize(1);
				this.buildSupertypes();
			}
		}
		finally {
			manager.flushZipFiles();
		}
	}

	private void buildForProject(JavaProject project, ArrayList potentialSubtypes, org.eclipse.wst.jsdt.core.IJavaScriptUnit[] workingCopies, HashSet localTypes, IProgressMonitor monitor) throws JavaScriptModelException {
		// resolve
		int openablesLength = potentialSubtypes.size();
		if (openablesLength > 0) {
			// copy vectors into arrays
			Openable[] openables = new Openable[openablesLength];
			potentialSubtypes.toArray(openables);

			/* sort in the order of roots and in reverse alphabetical order for .class file
			 * since requesting top level types in the process of caching an enclosing type is */
			// not supported by the lookup environment
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			int rootsLength = roots.length;
			final HashtableOfObjectToInt indexes = new HashtableOfObjectToInt(openablesLength);
			for (int i = 0; i < openablesLength; i++) {
				IJavaScriptElement root = openables[i].getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
				int index;
				for (index = 0; index < rootsLength; index++) {
					if (roots[index].equals(root)) {
						break;
					}
				}
				indexes.put(openables[i], index);
			}
			Arrays.sort(openables, new Comparator() {
				public int compare(Object a, Object b) {
					int aIndex = indexes.get(a);
					int bIndex = indexes.get(b);
					if (aIndex != bIndex) {
						return aIndex - bIndex;
					}
					return ((Openable) b).getElementName().compareTo(((Openable) a).getElementName());
				}
			});

			IType focusType = this.getType();
			boolean inProjectOfFocusType = focusType != null && focusType.getJavaScriptProject().equals(project);
			org.eclipse.wst.jsdt.core.IJavaScriptUnit[] unitsToLookInside = null;
			if (inProjectOfFocusType) {
				org.eclipse.wst.jsdt.core.IJavaScriptUnit unitToLookInside = focusType.getJavaScriptUnit();
				if (unitToLookInside != null) {
					int wcLength = workingCopies == null ? 0 : workingCopies.length;
					if (wcLength == 0) {
						unitsToLookInside = new org.eclipse.wst.jsdt.core.IJavaScriptUnit[]{unitToLookInside};
					}
					else {
						unitsToLookInside = new org.eclipse.wst.jsdt.core.IJavaScriptUnit[wcLength + 1];
						unitsToLookInside[0] = unitToLookInside;
						System.arraycopy(workingCopies, 0, unitsToLookInside, 1, wcLength);
					}
				}
				else {
					unitsToLookInside = workingCopies;
				}
			}

			SearchableEnvironment searchableEnvironment = project.newSearchableNameEnvironment(unitsToLookInside);
			this.nameLookup = searchableEnvironment.nameLookup;
			Map options = project.getOptions(true);
			// disable task tags to speed up parsing
			options.put(JavaScriptCore.COMPILER_TASK_TAGS, ""); //$NON-NLS-1$
			this.hierarchyResolver = new HierarchyResolver(searchableEnvironment, options, this, new DefaultProblemFactory());
			if (focusType != null) {
				Member declaringMember = ((Member) focusType).getOuterMostLocalContext();
				if (declaringMember == null) {
					// top level or member type
					if (!inProjectOfFocusType) {
						char[] typeQualifiedName = focusType.getTypeQualifiedName('.').toCharArray();
						String[] packageName = ((PackageFragment) focusType.getPackageFragment()).names;
						if (searchableEnvironment.findType(typeQualifiedName, Util.toCharArrays(packageName), this.hierarchyResolver) == null) {
							// focus type is not visible in this project: no need to go further
							return;
						}
					}
				}
				else {
					// local or anonymous type
					Openable openable;
					if (declaringMember.isBinary()) {
						openable = (Openable) declaringMember.getClassFile();
					}
					else {
						openable = (Openable) declaringMember.getJavaScriptUnit();
					}
					localTypes = new HashSet();
					localTypes.add(openable.getPath().toString());
					this.hierarchyResolver.resolve(new Openable[]{openable}, localTypes, monitor);
					return;
				}
			}
			this.hierarchyResolver.resolve(openables, localTypes, monitor);
		}
	}

	/**
	 * Configure this type hierarchy based on the given potential subtypes.
	 */
	private void buildFromPotentialSubtypeFilepaths(String[] allPotentialSubTypeFilePaths, HashSet localTypes, IProgressMonitor monitor) {
		IType focusType = this.getType();

		// substitute compilation units with working copies
		HashMap wcPaths = new HashMap(); // a map from path to working copies
		int wcLength;
		org.eclipse.wst.jsdt.core.IJavaScriptUnit[] workingCopies = this.hierarchy.workingCopies;
		if (workingCopies != null && (wcLength = workingCopies.length) > 0) {
			String[] newPaths = new String[wcLength];
			for (int i = 0; i < wcLength; i++) {
				org.eclipse.wst.jsdt.core.IJavaScriptUnit workingCopy = workingCopies[i];
				String path = workingCopy.getPath().toString();
				wcPaths.put(path, workingCopy);
				newPaths[i] = path;
			}
			int potentialSubtypesLength = allPotentialSubTypeFilePaths.length;
			System.arraycopy(allPotentialSubTypeFilePaths, 0, allPotentialSubTypeFilePaths = new String[potentialSubtypesLength + wcLength], 0, potentialSubtypesLength);
			System.arraycopy(newPaths, 0, allPotentialSubTypeFilePaths, potentialSubtypesLength, wcLength);
		}

		int length = allPotentialSubTypeFilePaths.length;

		/* inject the compilation unit of the focus type (so that types in
		 * this cu have special visibility permission (this is also usefull
		 * when the cu is a working copy) */
		Openable focusCU = (Openable) focusType.getJavaScriptUnit();
		String focusPath = null;
		if (focusCU != null) {
			focusPath = focusCU.getPath().toString();
			if (length > 0) {
				System.arraycopy(allPotentialSubTypeFilePaths, 0, allPotentialSubTypeFilePaths = new String[length + 1], 0, length);
				allPotentialSubTypeFilePaths[length] = focusPath;
			}
			else {
				allPotentialSubTypeFilePaths = new String[]{focusPath};
			}
			length++;
		}

		// Sort in alphabetical order so that potential subtypes are grouped per project
		Arrays.sort(allPotentialSubTypeFilePaths);

		ArrayList potentialSubtypes = new ArrayList();
		try {
			// create element infos for subtypes
			HandleFactory factory = new HandleFactory();
			IJavaScriptProject currentProject = null;
			if (monitor != null)
				monitor.beginTask("", length * 2 /* 1 for build binding, 1 for connect hierarchy*/); //$NON-NLS-1$
			for (int i = 0; i < length; i++) {
				try {
					String resourcePath = allPotentialSubTypeFilePaths[i];

					/* skip duplicate paths (e.g. if focus path was injected
					 * when it was already a potential subtype) */
					if (i > 0 && resourcePath.equals(allPotentialSubTypeFilePaths[i - 1])) {
						continue;
					}

					Openable handle;
					org.eclipse.wst.jsdt.core.IJavaScriptUnit workingCopy = (org.eclipse.wst.jsdt.core.IJavaScriptUnit) wcPaths.get(resourcePath);
					if (workingCopy != null) {
						handle = (Openable) workingCopy;
					}
					else {
						handle = resourcePath.equals(focusPath) ? focusCU : factory.createOpenable(resourcePath, this.scope);
						if (handle == null) {
							continue; // match is outside classpath
						}
					}

					IJavaScriptProject project = handle.getJavaScriptProject();
					if (currentProject == null) {
						currentProject = project;
						potentialSubtypes = new ArrayList(5);
					}
					else if (!currentProject.equals(project)) {
						// build current project
						this.buildForProject((JavaProject) currentProject, potentialSubtypes, workingCopies, localTypes, monitor);
						currentProject = project;
						potentialSubtypes = new ArrayList(5);
					}

					potentialSubtypes.add(handle);
				}
				catch (JavaScriptModelException e) {
					continue;
				}
			}

			// build last project
			try {
				if (currentProject == null) {
					// case of no potential subtypes
					currentProject = focusType.getJavaScriptProject();
					if (focusType.isBinary()) {
						potentialSubtypes.add(focusType.getClassFile());
					}
					else {
						potentialSubtypes.add(focusType.getJavaScriptUnit());
					}
				}
				this.buildForProject((JavaProject) currentProject, potentialSubtypes, workingCopies, localTypes, monitor);
			}
			catch (JavaScriptModelException e) {
				// ignore
			}

			// Add focus if not already in (case of a type with no explicit super type)
			if (!this.hierarchy.contains(focusType)) {
				this.hierarchy.addRootClass(focusType);
			}
		}
		finally {
			if (monitor != null)
				monitor.done();
		}
	}

	protected ICompilationUnit createCompilationUnitFromPath(Openable handle, IFile file) {
		ICompilationUnit unit = super.createCompilationUnitFromPath(handle, file);
		this.cuToHandle.put(unit, handle);
		return unit;
	}

	/**
	 * @param monitor used to track progress and monitor cancellation
	 * 
	 * @return file paths to all of the files containing subtypes of the current focus type.
	 */
	private String[] determinePossibleSubTypesFilePaths(IProgressMonitor monitor) {
		final HashSet paths = new HashSet(10);
		IPathRequestor collector = new IPathRequestor() {
			public void acceptPath(String path, boolean containsLocalTypes) {
				paths.add(path);
			}
		};

		try {
			if (monitor != null) {
				monitor.beginTask("", MAXTICKS); //$NON-NLS-1$
			}
			searchAllPossibleSubTypes(this.getType(), this.scope, collector, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
		}
		finally {
			if (monitor != null) {
				monitor.done();
			}
		}

		int length = paths.size();
		String[] result = new String[length];
		int count = 0;
		for (Iterator iter = paths.iterator(); iter.hasNext();) {
			result[count++] = (String) iter.next();
		}
		return result;
	}

	/**
	 * Find the set of candidate subtypes of a given type.
	 * 
	 * The requestor is notified of super type references (with actual path of
	 * its occurrence) for all types which are potentially involved inside a
	 * particular hierarchy. The match locator is not used here to narrow down
	 * the results, the type hierarchy resolver is rather used to compute the
	 * whole hierarchy at once.
	 * 
	 * @param type
	 * @param scope
	 * @param pathRequestor
	 * @param waitingPolicy
	 * @param progressMonitor
	 */
	public static void searchAllPossibleSubTypes(IType type, IJavaScriptSearchScope scope,
				 final IPathRequestor pathRequestor,
				int waitingPolicy, IProgressMonitor progressMonitor) {
		
		//set up monitor
		final IProgressMonitor monitor;
		if(progressMonitor == null) {
			monitor = new NullProgressMonitor();
		} else {
			monitor = progressMonitor;
		}

		// embed constructs inside arrays so as to pass them to (inner) collector
		final Queue parentTypeNames = new Queue();
		final HashtableOfObject foundSuperNames = new HashtableOfObject(5);

		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();

		// use a special collector to collect paths and queue new subtype names
		IndexQueryRequestor subTypeSearchRequestor = new IndexQueryRequestor() {
			/**
			 * @see org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor#acceptIndexMatch(java.lang.String, org.eclipse.wst.jsdt.core.search.SearchPattern, org.eclipse.wst.jsdt.core.search.SearchParticipant, org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet)
			 */
			public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
				SuperTypeReferencePattern record = (SuperTypeReferencePattern) indexRecord;
				pathRequestor.acceptPath(documentPath, false);
				char[] typeName = record.typeName;
				
				//add to list of type names to search for children for
				if (!foundSuperNames.containsKey(typeName)) {
					foundSuperNames.put(typeName, typeName);
					parentTypeNames.add(typeName);
				}
				return true;
			}
		};

		//create initial pattern
		SuperTypeReferencePattern superTypeRefPattern = new SuperTypeReferencePattern(null, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
		MatchLocator.setFocus(superTypeRefPattern, type);
		
		//create the job used for searching for the sub types
		SearchParticipant participant = new JavaSearchParticipant();
		SubTypeSearchJob subTypeSearchJob = new SubTypeSearchJob(superTypeRefPattern, participant, scope, subTypeSearchRequestor);

		//queue of parent type names
		parentTypeNames.add(type.getElementName().toCharArray());
		
		//monitor used just to track cancellation
		IProgressMonitor cancelMonitor = new NullProgressMonitor() {
			/**
			 * <p>
			 * don't report progress since this is too costly for deep hierarchies
			 * just handle isCanceled()
			 * </p>
			 * 
			 * @see org.eclipse.core.runtime.NullProgressMonitor#setCanceled(boolean)
			 */
			public void setCanceled(boolean value) {
				monitor.setCanceled(value);
			}

			public boolean isCanceled() {
				return monitor.isCanceled();
			}
		};
	
		try {
			//while there are still parent type names to check and the progress monitor has not been canceled keep processing
			int ticks = 0;
			while (parentTypeNames.start <= parentTypeNames.end && !monitor.isCanceled()) {
				// all subclasses of OBJECT are actually all types
				char[] currentTypeName = parentTypeNames.retrieve();
				if (CharOperation.equals(currentTypeName, IIndexConstants.OBJECT)) {
					currentTypeName = null;
				}
				
				/* if current name then get all its synonyms then search for all of those synonyms children
				 * else in case, we search all sub-types, no need to search further */
				if(currentTypeName != null) {
					//get all the synonyms, including self, to the current type name
					char[][] synonyms = SearchEngine.getAllSynonyms(currentTypeName, scope, waitingPolicy, progressMonitor);
					
					//for each synonym search the index for sub types of that synonym
					for(int i = 0; i < synonyms.length; ++i) {
						char[] synonym = synonyms[i];
						
						//search for synonym type in index so that it can be added to the hierarchy
						indexManager.performConcurrentJob(new PatternSearchJob(
								new TypeDeclarationPattern(synonym, SearchPattern.R_EXACT_MATCH),
								participant, scope,
								new IndexQueryRequestor() {
									/**
									 * @see org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor#acceptIndexMatch(java.lang.String, org.eclipse.wst.jsdt.core.search.SearchPattern, org.eclipse.wst.jsdt.core.search.SearchParticipant, org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet)
									 */
									public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
										pathRequestor.acceptPath(documentPath, false);
										
										return true;
									}
								}), waitingPolicy, cancelMonitor);
						
						//perform super type pattern index search
						superTypeRefPattern.superTypeName = synonym;
						indexManager.performConcurrentJob(subTypeSearchJob, waitingPolicy, cancelMonitor);
						
						//track progress
						if (++ticks <= MAXTICKS) {
							monitor.worked(1);
						}
					}
				} else {
					break;
				}
			}
		}
		finally {
			//always end the job
			subTypeSearchJob.finished();
		}
	}
	
	public static ArrayList findAllSuperTypes(char[] typeName, IJavaScriptSearchScope scope) {
		final ArrayList superTypes = new ArrayList();
		final ArrayList queue = new ArrayList();
		final ArrayList alreadySearched = new ArrayList();

		try {
			queue.add(CharOperation.toLowerCase(typeName));

			IProgressMonitor progressMonitor = new IProgressMonitor() {
				boolean isCanceled = false;

				public void beginTask(String name, int totalWork) {
					// implements interface method
				}

				public void done() {
					// implements interface method
				}

				public void internalWorked(double work) {
					// implements interface method
				}

				public boolean isCanceled() {
					return isCanceled;
				}

				public void setCanceled(boolean value) {
					isCanceled = value;
				}

				public void setTaskName(String name) {
					// implements interface method
				}

				public void subTask(String name) {
					// implements interface method
				}

				public void worked(int work) {
					// implements interface method
				}
			};
			IRestrictedAccessTypeRequestor typeRequestor = new IRestrictedAccessTypeRequestor() {
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] superTypeNames, char[][] enclosingTypeNames, String path, AccessRestriction access) {
					boolean doAdd = true;
					for (int i = 0; i < superTypes.size(); i++) {
						if (CharOperation.equals(superTypeNames[0], (char[]) superTypes.get(i))) {
							doAdd = false;
							break;
						}
					}
					if (doAdd) {
						superTypes.add(superTypeNames[0]);
					}
					queue.add(superTypeNames[0]);
				}
			};
			try {
				while (!queue.isEmpty()) {
					char[] nextSearch = (char[]) queue.get(0);
					boolean doSearch = true;
					if (CharOperation.equals(nextSearch, IIndexConstants.OBJECT)) {
						doSearch = false;
					}

					for (int i = 0; doSearch && i < alreadySearched.size(); i++) {
						if (CharOperation.equals(nextSearch, (char[]) alreadySearched.get(i))) {
							doSearch = false;
						}
					}
					if (doSearch) {
						alreadySearched.add(nextSearch);
						// not case sensitive
						new BasicSearchEngine().searchAllTypeNames(CharOperation.NO_CHAR, nextSearch,
									SearchPattern.R_EXACT_MATCH, 
									scope, typeRequestor, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
									progressMonitor);

					}
					queue.remove(0);
					doSearch = true;
				}
			}
			catch (OperationCanceledException e) {
			}
		}
		catch (JavaScriptModelException e) {
		}
		return superTypes;
	}
}
