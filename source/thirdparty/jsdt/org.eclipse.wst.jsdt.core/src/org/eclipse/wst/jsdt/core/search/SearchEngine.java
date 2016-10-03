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
package org.eclipse.wst.jsdt.core.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSetOfCharArray;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.search.BasicSearchEngine;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.wst.jsdt.internal.core.search.PatternSearchJob;
import org.eclipse.wst.jsdt.internal.core.search.TypeNameMatchRequestorWrapper;
import org.eclipse.wst.jsdt.internal.core.search.TypeNameRequestorWrapper;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IndexManager;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeSynonymsPattern;

/**
 * A {@link SearchEngine} searches for JavaScript elements following a search pattern.
 * The search can be limited to a search scope.
 * <p>
 * Various search patterns can be created using the factory methods
 * {@link SearchPattern#createPattern(String, int, int, int)}, {@link SearchPattern#createPattern(IJavaScriptElement, int)},
 * {@link SearchPattern#createOrPattern(SearchPattern, SearchPattern)}.
 * </p>
 * <p>For example, one can search for references to a method in the hierarchy of a type,
 * or one can search for the declarations of types starting with "Abstract" in a project.
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class SearchEngine {

	// Search engine now uses basic engine functionalities
	private BasicSearchEngine basicEngine;

	/**
	 * Creates a new search engine.
	 */
	public SearchEngine() {
		this.basicEngine = new BasicSearchEngine();
	}

	/**
	 * Creates a new search engine with a list of working copies that will take precedence over
	 * their original javascript unit s in the subsequent search operations.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.</p>
	 * <p>
	 * Since 3.0 the given working copies take precedence over primary working copies (if any).
	 *
	 * @param workingCopies the working copies that take precedence over their original javascript unit s
	 *  
	 */
	public SearchEngine(IJavaScriptUnit[] workingCopies) {
		this.basicEngine = new BasicSearchEngine(workingCopies);
	}
	/**
	 * Creates a new search engine with the given working copy owner.
	 * The working copies owned by this owner will take precedence over
	 * the primary javascript unit s in the subsequent search operations.
	 *
	 * @param workingCopyOwner the owner of the working copies that take precedence over their original javascript unit s
	 *  
	 */
	public SearchEngine(WorkingCopyOwner workingCopyOwner) {
		this.basicEngine = new BasicSearchEngine(workingCopyOwner);
	}

	/**
	 * Returns a JavaScript search scope limited to the hierarchy of the given type.
	 * The JavaScript elements resulting from a search with this scope will
	 * be types in this hierarchy, or members of the types in this hierarchy.
	 *
	 * @param type the focus of the hierarchy scope
	 * @return a new hierarchy scope
	 * @exception JavaScriptModelException if the hierarchy could not be computed on the given type
	 */
	public static IJavaScriptSearchScope createHierarchyScope(IType type) throws JavaScriptModelException {
		return BasicSearchEngine.createHierarchyScope(type);
	}

	/**
	 * Returns a JavaScript search scope limited to the hierarchy of the given type.
	 * When the hierarchy is computed, the types defined in the working copies owned
	 * by the given owner take precedence over the original javascript unit s.
	 * The JavaScript elements resulting from a search with this scope will
	 * be types in this hierarchy, or members of the types in this hierarchy.
	 *
	 * @param type the focus of the hierarchy scope
	 * @param owner the owner of working copies that take precedence over original javascript unit s
	 * @return a new hierarchy scope
	 * @exception JavaScriptModelException if the hierarchy could not be computed on the given type
	 *  
	 */
	public static IJavaScriptSearchScope createHierarchyScope(IType type, WorkingCopyOwner owner) throws JavaScriptModelException {
		return BasicSearchEngine.createHierarchyScope(type, owner);
	}

	/**
	 * Returns a JavaScript search scope limited to the given JavaScript elements.
	 * The JavaScript elements resulting from a search with this scope will
	 * be children of the given elements.
	 * <p>
	 * If an element is an IJavaScriptProject, then the project's source folders,
	 * its jars (external and internal) and its referenced projects (with their source
	 * folders and jars, recursively) will be included.
	 * If an element is an IPackageFragmentRoot, then only the package fragments of
	 * this package fragment root will be included.
	 * If an element is an IPackageFragment, then only the javascript unit  and class
	 * files of this package fragment will be included. Subpackages will NOT be
	 * included.</p>
	 * <p>
	 * In other words, this is equivalent to using SearchEngine.createJavaSearchScope(elements, true).</p>
	 *
	 * @param elements the JavaScript elements the scope is limited to
	 * @return a new JavaScript search scope
	 *  
	 */
	public static IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] elements) {
		return BasicSearchEngine.createJavaSearchScope(elements);
	}

	/**
	 * Returns a JavaScript search scope limited to the given JavaScript elements.
	 * The JavaScript elements resulting from a search with this scope will
	 * be children of the given elements.
	 *
	 * If an element is an IJavaScriptProject, then the project's source folders,
	 * its jars (external and internal) and - if specified - its referenced projects
	 * (with their source folders and jars, recursively) will be included.
	 * If an element is an IPackageFragmentRoot, then only the package fragments of
	 * this package fragment root will be included.
	 * If an element is an IPackageFragment, then only the javascript unit  and class
	 * files of this package fragment will be included. Subpackages will NOT be
	 * included.
	 *
	 * @param elements the JavaScript elements the scope is limited to
	 * @param includeReferencedProjects a flag indicating if referenced projects must be
	 * 									 recursively included
	 * @return a new JavaScript search scope
	 *  
	 */
	public static IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] elements, boolean includeReferencedProjects) {
		return BasicSearchEngine.createJavaSearchScope(elements, includeReferencedProjects);
	}

	/**
	 * Returns a JavaScript search scope limited to the given JavaScript elements.
	 * The JavaScript elements resulting from a search with this scope will
	 * be children of the given elements.
	 *
	 * If an element is an IJavaScriptProject, then it includes:
	 * - its source folders if IJavaScriptSearchScope.SOURCES is specified,
	 * - its application libraries (internal and external jars, class folders that are on the raw includepath,
	 *   or the ones that are coming from a includepath path variable,
	 *   or the ones that are coming from a includepath container with the K_APPLICATION kind)
	 *   if IJavaScriptSearchScope.APPLICATION_LIBRARIES is specified
	 * - its system libraries (internal and external jars, class folders that are coming from an
	 *   IJsGlobalScopeContainer with the K_SYSTEM kind)
	 *   if IJavaScriptSearchScope.APPLICATION_LIBRARIES is specified
	 * - its referenced projects (with their source folders and jars, recursively)
	 *   if IJavaScriptSearchScope.REFERENCED_PROJECTS is specified.
	 * If an element is an IPackageFragmentRoot, then only the package fragments of
	 * this package fragment root will be included.
	 * If an element is an IPackageFragment, then only the javascript unit  and class
	 * files of this package fragment will be included. Subpackages will NOT be
	 * included.
	 *
	 * @param elements the JavaScript elements the scope is limited to
	 * @param includeMask the bit-wise OR of all include types of interest
	 * @return a new JavaScript search scope
	 * @see IJavaScriptSearchScope#SOURCES
	 * @see IJavaScriptSearchScope#APPLICATION_LIBRARIES
	 * @see IJavaScriptSearchScope#SYSTEM_LIBRARIES
	 * @see IJavaScriptSearchScope#REFERENCED_PROJECTS
	 *  
	 */
	public static IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] elements, int includeMask) {
		return BasicSearchEngine.createJavaSearchScope(elements, includeMask);
	}

	/**
	 * Create a type name match on a given type with specific modifiers.
	 *
	 * @param type The javascript model handle of the type
	 * @param modifiers Modifiers of the type
	 * @return A non-null match on the given type.
	 *  
	 */
	public static TypeNameMatch createTypeNameMatch(IType type, int modifiers) {
		return BasicSearchEngine.createTypeNameMatch(type, modifiers);
	}

	/**
	 * Returns a JavaScript search scope with the workspace as the only limit.
	 *
	 * @return a new workspace scope
	 */
	public static IJavaScriptSearchScope createWorkspaceScope() {
		return BasicSearchEngine.createWorkspaceScope();
	}
	/**
	 * Returns a new default JavaScript search participant.
	 *
	 * @return a new default JavaScript search participant
	 *  
	 */
	public static SearchParticipant getDefaultSearchParticipant() {
		return BasicSearchEngine.getDefaultSearchParticipant();
	}

	/**
	 * Searches for matches of a given search pattern. Search patterns can be created using helper
	 * methods (from a String pattern or a JavaScript element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @param pattern the pattern to search
	 * @param participants the particpants in the search
	 * @param scope the search scope
	 * @param requestor the requestor to report the matches to
	 * @param monitor the progress monitor used to report progress
	 * @exception CoreException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 * 
	 */
	public void search(SearchPattern pattern, SearchParticipant[] participants, IJavaScriptSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {
		this.basicEngine.search(pattern, participants, scope, requestor, monitor);
	}

	/**
	 * Searches for all top-level types and member types in the given scope.
	 * The search can be selecting specific types (given a package name using specific match mode
	 * and/or a type name using another specific match mode).
	 *
	 * @param packageName the full name of the package of the searched types, or a prefix for this
	 *						package, or a wild-carded string for this package.
	 *						May be <code>null</code>, then any package name is accepted.
	 * @param typeName the dot-separated qualified name of the searched type (the qualification include
	 *					the enclosing types if the searched type is a member type), or a prefix
	 *					for this type, or a wild-carded string for this type.
	 *					May be <code>null</code>, then any type name is accepted.
	 * @param packageMatchRule ignored
	 * @param typeMatchRule one of
	 * <ul>
	 *		<li>{@link SearchPattern#R_EXACT_MATCH} if the package name and type name are the full names
	 *			of the searched types.</li>
	 *		<li>{@link SearchPattern#R_PREFIX_MATCH} if the package name and type name are prefixes of the names
	 *			of the searched types.</li>
	 *		<li>{@link SearchPattern#R_PATTERN_MATCH} if the package name and type name contain wild-cards.</li>
	 *		<li>{@link SearchPattern#R_CAMELCASE_MATCH} if type name are camel case of the names of the searched types.</li>
	 * </ul>
	 * combined with {@link SearchPattern#R_CASE_SENSITIVE},
	 *   e.g. {@link SearchPattern#R_EXACT_MATCH} | {@link SearchPattern#R_CASE_SENSITIVE} if an exact and case sensitive match is requested,
	 *   or {@link SearchPattern#R_PREFIX_MATCH} if a prefix non case sensitive match is requested.
	 * @param searchFor determines the nature of the searched elements
	 *	<ul>
	 * 	<li>{@link IJavaScriptSearchConstants#CLASS}: only look for classes</li>
	 *		<li>{@link IJavaScriptSearchConstants#INTERFACE}: only look for interfaces</li>
	 * 	<li>{@link IJavaScriptSearchConstants#ENUM}: only look for enumeration</li>
	 *		<li>{@link IJavaScriptSearchConstants#ANNOTATION_TYPE}: only look for annotation type</li>
	 * 	<li>{@link IJavaScriptSearchConstants#CLASS_AND_ENUM}: only look for classes and enumerations</li>
	 *		<li>{@link IJavaScriptSearchConstants#CLASS_AND_INTERFACE}: only look for classes and interfaces</li>
	 * 	<li>{@link IJavaScriptSearchConstants#TYPE}: look for all types (ie. classes, interfaces, enum and annotation types)</li>
	 *	</ul>
	 * @param scope the scope to search in
	 * @param nameRequestor the requestor that collects the results of the search
	 * @param waitingPolicy one of
	 * <ul>
	 *		<li>{@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if the search should start immediately</li>
	 *		<li>{@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH} if the search should be cancelled if the
	 *			underlying indexer has not finished indexing the workspace</li>
	 *		<li>{@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH} if the search should wait for the
	 *			underlying indexer to finish indexing the workspace</li>
	 * </ul>
	 * @param progressMonitor the progress monitor to report progress to, or <code>null</code> if no progress
	 *							monitor is provided
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchAllTypeNames(
		final char[] packageName,
		final int packageMatchRule,  //ignored
		final char[] typeName,
		final int typeMatchRule,
		int searchFor,
		IJavaScriptSearchScope scope,
		final TypeNameRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		TypeNameRequestorWrapper requestorWrapper = new TypeNameRequestorWrapper(nameRequestor);
		this.basicEngine.searchAllTypeNames(packageName, typeName, typeMatchRule, scope, requestorWrapper, waitingPolicy, progressMonitor);
	}
	
	/**
	 * Searches for all top-level types and member types in the given scope.
	 * <p>
	 * Provided {@link TypeNameMatchRequestor} requestor will collect {@link TypeNameMatch}
	 * matches found during the search.
	 * </p>
	 *
	 * @param prefix The prefix could be part of the qualification or simple name for a type, 
	 * or it could be a camel case statement for a simple name of a type.
	 * @param typeMatchRule one of
	 * <ul>
	 *		<li>{@link SearchPattern#R_EXACT_MATCH} if the package name and type name are the full names
	 *			of the searched types.</li>
	 *		<li>{@link SearchPattern#R_PREFIX_MATCH} if the package name and type name are prefixes of the names
	 *			of the searched types.</li>
	 *		<li>{@link SearchPattern#R_PATTERN_MATCH} if the package name and type name contain wild-cards.</li>
	 *		<li>{@link SearchPattern#R_CAMELCASE_MATCH} if type name are camel case of the names of the searched types.</li>
	 * </ul>
	 * combined with {@link SearchPattern#R_CASE_SENSITIVE},
	 *   e.g. {@link SearchPattern#R_EXACT_MATCH} | {@link SearchPattern#R_CASE_SENSITIVE} if an exact and case sensitive match is requested,
	 *   or {@link SearchPattern#R_PREFIX_MATCH} if a prefix non case sensitive match is requested.
	 * @param searchFor determines the nature of the searched elements
	 *	<ul>
	 * 	<li>{@link IJavaScriptSearchConstants#CLASS}: only look for classes</li>
	 *		<li>{@link IJavaScriptSearchConstants#INTERFACE}: only look for interfaces</li>
	 * 	<li>{@link IJavaScriptSearchConstants#ENUM}: only look for enumeration</li>
	 *		<li>{@link IJavaScriptSearchConstants#ANNOTATION_TYPE}: only look for annotation type</li>
	 * 	<li>{@link IJavaScriptSearchConstants#CLASS_AND_ENUM}: only look for classes and enumerations</li>
	 *		<li>{@link IJavaScriptSearchConstants#CLASS_AND_INTERFACE}: only look for classes and interfaces</li>
	 * 	<li>{@link IJavaScriptSearchConstants#TYPE}: look for all types (ie. classes, interfaces, enum and annotation types)</li>
	 *	</ul>
	 * @param scope the scope to search in
	 * @param nameMatchRequestor the {@link TypeNameMatchRequestor requestor} that collects
	 * 				{@link TypeNameMatch matches} of the search.
	 * @param waitingPolicy one of
	 * <ul>
	 *		<li>{@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if the search should start immediately</li>
	 *		<li>{@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH} if the search should be cancelled if the
	 *			underlying indexer has not finished indexing the workspace</li>
	 *		<li>{@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH} if the search should wait for the
	 *			underlying indexer to finish indexing the workspace</li>
	 * </ul>
	 * @param progressMonitor the progress monitor to report progress to, or <code>null</code> if no progress
	 *							monitor is provided
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchAllTypeNames(
		final char[] prefix,
		final int typeMatchRule,
		int searchFor,
		IJavaScriptSearchScope scope,
		final TypeNameMatchRequestor nameMatchRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		TypeNameMatchRequestorWrapper requestorWrapper = new TypeNameMatchRequestorWrapper(nameMatchRequestor, scope);
		this.basicEngine.searchAllTypeNames(prefix, typeMatchRule, scope, requestorWrapper, waitingPolicy, progressMonitor);
	}

	/**
	 * Searches for all top-level types and member types in the given scope.
	 * The search can be selecting specific types (given a package name using specific match mode
	 * and/or a type name using another specific match mode).
	 * <p>
	 * Provided {@link TypeNameMatchRequestor} requestor will collect {@link TypeNameMatch}
	 * matches found during the search.
	 * </p>
	 *
	 * @param packageName the full name of the package of the searched types, or a prefix for this
	 *						package, or a wild-carded string for this package.
	 *						May be <code>null</code>, then any package name is accepted.
	 * @param packageMatchRule IGNORED
	 * @param typeName the dot-separated qualified name of the searched type (the qualification include
	 *					the enclosing types if the searched type is a member type), or a prefix
	 *					for this type, or a wild-carded string for this type.
	 *					May be <code>null</code>, then any type name is accepted.
	 * @param typeMatchRule one of
	 * <ul>
	 *		<li>{@link SearchPattern#R_EXACT_MATCH} if the package name and type name are the full names
	 *			of the searched types.</li>
	 *		<li>{@link SearchPattern#R_PREFIX_MATCH} if the package name and type name are prefixes of the names
	 *			of the searched types.</li>
	 *		<li>{@link SearchPattern#R_PATTERN_MATCH} if the package name and type name contain wild-cards.</li>
	 *		<li>{@link SearchPattern#R_CAMELCASE_MATCH} if type name are camel case of the names of the searched types.</li>
	 * </ul>
	 * combined with {@link SearchPattern#R_CASE_SENSITIVE},
	 *   e.g. {@link SearchPattern#R_EXACT_MATCH} | {@link SearchPattern#R_CASE_SENSITIVE} if an exact and case sensitive match is requested,
	 *   or {@link SearchPattern#R_PREFIX_MATCH} if a prefix non case sensitive match is requested.
	 * @param searchFor determines the nature of the searched elements
	 *	<ul>
	 * 	<li>{@link IJavaScriptSearchConstants#CLASS}: only look for classes</li>
	 *		<li>{@link IJavaScriptSearchConstants#INTERFACE}: only look for interfaces</li>
	 * 	<li>{@link IJavaScriptSearchConstants#ENUM}: only look for enumeration</li>
	 *		<li>{@link IJavaScriptSearchConstants#ANNOTATION_TYPE}: only look for annotation type</li>
	 * 	<li>{@link IJavaScriptSearchConstants#CLASS_AND_ENUM}: only look for classes and enumerations</li>
	 *		<li>{@link IJavaScriptSearchConstants#CLASS_AND_INTERFACE}: only look for classes and interfaces</li>
	 * 	<li>{@link IJavaScriptSearchConstants#TYPE}: look for all types (ie. classes, interfaces, enum and annotation types)</li>
	 *	</ul>
	 * @param scope the scope to search in
	 * @param nameMatchRequestor the {@link TypeNameMatchRequestor requestor} that collects
	 * 				{@link TypeNameMatch matches} of the search.
	 * @param waitingPolicy one of
	 * <ul>
	 *		<li>{@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if the search should start immediately</li>
	 *		<li>{@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH} if the search should be cancelled if the
	 *			underlying indexer has not finished indexing the workspace</li>
	 *		<li>{@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH} if the search should wait for the
	 *			underlying indexer to finish indexing the workspace</li>
	 * </ul>
	 * @param progressMonitor the progress monitor to report progress to, or <code>null</code> if no progress
	 *							monitor is provided
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchAllTypeNames(
		final char[] packageName,
		final int packageMatchRule, //ignored
		final char[] typeName,
		final int typeMatchRule,
		int searchFor,
		IJavaScriptSearchScope scope,
		final TypeNameMatchRequestor nameMatchRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		TypeNameMatchRequestorWrapper requestorWrapper = new TypeNameMatchRequestorWrapper(nameMatchRequestor, scope);
		this.basicEngine.searchAllTypeNames(packageName, typeName, typeMatchRule, scope, requestorWrapper, waitingPolicy, progressMonitor);
	}

	/**
	 * Searches for all top-level types and member types in the given scope matching any of the given qualifications
	 * and type names in a case sensitive way.
	 *
	 * @param qualifications the qualified name of the package/enclosing type of the searched types.
	 *					May be <code>null</code>, then any package name is accepted.
	 * @param typeNames the simple names of the searched types.
	 *					If this parameter is <code>null</code>, then no type will be found.
	 * @param scope the scope to search in
	 * @param nameRequestor the requestor that collects the results of the search
	 * @param waitingPolicy one of
	 * <ul>
	 *		<li>{@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if the search should start immediately</li>
	 *		<li>{@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH} if the search should be cancelled if the
	 *			underlying indexer has not finished indexing the workspace</li>
	 *		<li>{@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH} if the search should wait for the
	 *			underlying indexer to finish indexing the workspace</li>
	 * </ul>
	 * @param progressMonitor the progress monitor to report progress to, or <code>null</code> if no progress
	 *							monitor is provided
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchAllTypeNames(
		final char[][] qualifications,
		final char[][] typeNames,
		IJavaScriptSearchScope scope,
		final TypeNameRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		TypeNameRequestorWrapper requestorWrapper = new TypeNameRequestorWrapper(nameRequestor);
		this.basicEngine.searchAllTypeNames(
			qualifications,
			typeNames,
			SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
			scope,
			requestorWrapper,
			waitingPolicy,
			progressMonitor);
	}

	/**
	 * Searches for all top-level types and member types in the given scope matching any of the given qualifications
	 * and type names in a case sensitive way.
	 * <p>
	 * Provided {@link TypeNameMatchRequestor} requestor will collect {@link TypeNameMatch}
	 * matches found during the search.
	 * </p>
	 *
	 * @param qualifications the qualified name of the package/enclosing type of the searched types.
	 *					May be <code>null</code>, then any package name is accepted.
	 * @param typeNames the simple names of the searched types.
	 *					If this parameter is <code>null</code>, then no type will be found.
	 * @param scope the scope to search in
	 * @param nameMatchRequestor the {@link TypeNameMatchRequestor requestor} that collects
	 * 				{@link TypeNameMatch matches} of the search.
	 * @param waitingPolicy one of
	 * <ul>
	 *		<li>{@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if the search should start immediately</li>
	 *		<li>{@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH} if the search should be cancelled if the
	 *			underlying indexer has not finished indexing the workspace</li>
	 *		<li>{@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH} if the search should wait for the
	 *			underlying indexer to finish indexing the workspace</li>
	 * </ul>
	 * @param progressMonitor the progress monitor to report progress to, or <code>null</code> if no progress
	 *							monitor is provided
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchAllTypeNames(
		final char[][] qualifications,
		final char[][] typeNames,
		IJavaScriptSearchScope scope,
		final TypeNameMatchRequestor nameMatchRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		TypeNameMatchRequestorWrapper requestorWrapper = new TypeNameMatchRequestorWrapper(nameMatchRequestor, scope);
		this.basicEngine.searchAllTypeNames(
			qualifications,
			typeNames,
			SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
			scope,
			requestorWrapper,
			waitingPolicy,
			progressMonitor);
	}

	/**
	 * Searches for all declarations of the fields accessed in the given element.
	 * The element can be a javascript unit , a source type, or a source method.
	 * Reports the field declarations using the given requestor.
	 * <p>
	 * Consider the following code:
	 * <code>
	 * <pre>
	 *		class A {
	 *			int field1;
	 *		}
	 *		class B extends A {
	 *			String value;
	 *		}
	 *		class X {
	 *			void test() {
	 *				B b = new B();
	 *				System.out.println(b.value + b.field1);
	 *			};
	 *		}
	 * </pre>
	 * </code>
	 * then searching for declarations of accessed fields in method
	 * <code>X.test()</code> would collect the fields
	 * <code>B.value</code> and <code>A.field1</code>.
	 * </p>
	 *
	 * @param enclosingElement the method, type, or javascript unit  to be searched in
	 * @param requestor a callback object to which each match is reported
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the element doesn't exist</li>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchDeclarationsOfAccessedFields(IJavaScriptElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaScriptModelException {
		this.basicEngine.searchDeclarationsOfAccessedFields(enclosingElement, requestor, monitor);
	}

	/**
	 * Searches for all declarations of the types referenced in the given element.
	 * The element can be a javascript unit , a source type, or a source method.
	 * Reports the type declarations using the given requestor.
	 * <p>
	 * Consider the following code:
	 * <code>
	 * <pre>
	 *		class A {
	 *		}
	 *		class B extends A {
	 *		}
	 *		interface I {
	 *		  int VALUE = 0;
	 *		}
	 *		class X {
	 *			void test() {
	 *				B b = new B();
	 *				this.foo(b, I.VALUE);
	 *			};
	 *		}
	 * </pre>
	 * </code>
	 * then searching for declarations of referenced types in method <code>X.test()</code>
	 * would collect the class <code>B</code> and the interface <code>I</code>.
	 * </p>
	 *
	 * @param enclosingElement the method, type, or javascript unit  to be searched in
	 * @param requestor a callback object to which each match is reported
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the element doesn't exist</li>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchDeclarationsOfReferencedTypes(IJavaScriptElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaScriptModelException {
		this.basicEngine.searchDeclarationsOfReferencedTypes(enclosingElement, requestor, monitor);
	}

	/**
	 * Searches for all declarations of the methods invoked in the given element.
	 * The element can be a javascript unit , a source type, or a source method.
	 * Reports the method declarations using the given requestor.
	 * <p>
	 * Consider the following code:
	 * <code>
	 * <pre>
	 *		class A {
	 *			void foo() {};
	 *			void bar() {};
	 *		}
	 *		class B extends A {
	 *			void foo() {};
	 *		}
	 *		class X {
	 *			void test() {
	 *				A a = new B();
	 *				a.foo();
	 *				B b = (B)a;
	 *				b.bar();
	 *			};
	 *		}
	 * </pre>
	 * </code>
	 * then searching for declarations of sent messages in method
	 * <code>X.test()</code> would collect the methods
	 * <code>A.foo()</code>, <code>B.foo()</code>, and <code>A.bar()</code>.
	 * </p>
	 *
	 * @param enclosingElement the method, type, or javascript unit  to be searched in
	 * @param requestor a callback object to which each match is reported
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaScriptModelException if the search failed. Reasons include:
	 *	<ul>
	 *		<li>the element doesn't exist</li>
	 *		<li>the includepath is incorrectly set</li>
	 *	</ul>
	 *  
	 */
	public void searchDeclarationsOfSentMessages(IJavaScriptElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaScriptModelException {
		this.basicEngine.searchDeclarationsOfSentMessages(enclosingElement, requestor, monitor);
	}
	
	
	/**
	 * <p>
	 * Gets all the names of subtypes of a given type name in the given
	 * scope.
	 * </p>
	 * 
	 * @param typeName
	 *            name of the type whose subtype names will be found
	 * @param scope
	 *            to search in for all the subtypes of the given type name
	 * @param waitingPolicy
	 *            one of
	 *            <ul>
	 *            <li>
	 *            {@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if
	 *            the search should start immediately</li>
	 *            <li>
	 *            {@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH}
	 *            if the search should be cancelled if the underlying indexer
	 *            has not finished indexing the workspace</li>
	 *            <li>
	 *            {@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH}
	 *            if the search should wait for the underlying indexer to
	 *            finish indexing the workspace</li>
	 *            </ul>
	 * @param progressMonitor
	 *            monitor to report progress to
	 * 
	 * @return List of type names that are the subtypes of the given type
	 *         name, if there are no subtypes then the list will only contain
	 *         the given type name. The given type name is ALWAYS the first
	 *         element in the list.
	 */
	public static char[][] getAllSubtypeNames(char[] typeName, IJavaScriptSearchScope scope, int waitingPolicy, IProgressMonitor progressMonitor) {
		final IProgressMonitor monitor = progressMonitor != null ? progressMonitor : new NullProgressMonitor();
		
		//list of found names
		final SimpleSetOfCharArray subtypeNames = new SimpleSetOfCharArray();
		
		// queue of types to search for synonyms for
		final LinkedList searchQueue = new LinkedList();
		searchQueue.add(typeName);

		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		while (!searchQueue.isEmpty()) {
			char[] searchName = (char[]) searchQueue.remove(0);
			if (subtypeNames.includes(searchName))
				continue;
			subtypeNames.add(searchName);
			
			char[][] synonyms = getAllSynonyms(searchName, scope, waitingPolicy, null);
			for (int i = 0; i < synonyms.length; i++) {
				if (!subtypeNames.includes(synonyms[i])) {
					searchQueue.add(synonyms[i]);
				}
			}
			/*
			 * create pattern and job to search for subtypes of the parent
			 * type
			 */
			TypeDeclarationPattern subtypePattern = new TypeDeclarationPattern(IIndexConstants.ONE_STAR, IIndexConstants.ONE_STAR, new char[][]{searchName}, SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE);

			// run the search
			indexManager.performConcurrentJob(new PatternSearchJob(subtypePattern, new JavaSearchParticipant(), scope, new IndexQueryRequestor() {
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					TypeDeclarationPattern record = (TypeDeclarationPattern) indexRecord;
					char[] subtype = CharOperation.concat(record.qualification, record.simpleName, IIndexConstants.DOT);
					if (!subtypeNames.includes(subtype)) {
						searchQueue.add(subtype);
					}
					return true;
				}
			}), waitingPolicy, new NullProgressMonitor() {
				public void setCanceled(boolean value) {
					monitor.setCanceled(value);
				}

				public boolean isCanceled() {
					return monitor.isCanceled();
				}
			});
		}
		char[][] names = new char[subtypeNames.elementSize][];
		subtypeNames.asArray(names);
		return names;
	}
	
	/**
	 * <p>
	 * Gets all the synonyms of a given type, including itself, in the given
	 * scope.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> It is guaranteed that itself will be the first synonym in
	 * the list.
	 * </p>
	 * 
	 * @param typeName
	 *            name of the type to get all the synonyms for
	 * @param scope
	 *            to search in for all the synonyms of the given type
	 * @param waitingPolicy
	 *            one of
	 *            <ul>
	 *            <li>
	 *            {@link IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH} if
	 *            the search should start immediately</li>
	 *            <li>
	 *            {@link IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH}
	 *            if the search should be cancelled if the underlying indexer
	 *            has not finished indexing the workspace</li>
	 *            <li>
	 *            {@link IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH}
	 *            if the search should wait for the underlying indexer to
	 *            finish indexing the workspace</li>
	 *            </ul>
	 * @param progressMonitor
	 *            monitor to report progress to
	 * 
	 * @return List of type names that are the synonyms of the given type
	 *         name, if there are non synonyms then the list will only contain
	 *         the given type name. The given type name is ALWAYS the first
	 *         element in the list.
	 */
	public static char[][] getAllSynonyms(char[] typeName, IJavaScriptSearchScope scope,
				int waitingPolicy, IProgressMonitor progressMonitor) {
		
		final IProgressMonitor monitor = progressMonitor != null ? progressMonitor : new NullProgressMonitor();
		
		//list of found synonyms
		final List allSynonyms = new ArrayList();
		allSynonyms.add(typeName);
		
		//queue of types to search for synonyms for
		final LinkedList searchForSynonyms = new LinkedList();
		searchForSynonyms.add(typeName);
		
		//for each synonyms search of synonyms of that synonym
		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		while (!searchForSynonyms.isEmpty() && !monitor.isCanceled()) {
			char[] needle = (char[])searchForSynonyms.removeFirst();
			
			//create pattern and job to search for type synonyms for the parent type that is being searched for
			TypeSynonymsPattern typeSynonymsPattern = new TypeSynonymsPattern(needle);
		
			//search for the type synonyms
			indexManager.performConcurrentJob(new PatternSearchJob(
						typeSynonymsPattern,
						new JavaSearchParticipant(),
						scope,
						new IndexQueryRequestor() {
							public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
								TypeSynonymsPattern record = (TypeSynonymsPattern)indexRecord;
								char[][] patternSynonyms = record.getSynonyms();
								
								if(patternSynonyms != null && patternSynonyms.length != 0) {
									for(int i = 0; i < patternSynonyms.length; ++i) {
										/* if new synonym add to list of synonyms to return and to
										 * list of synonyms to check for more synonyms */
										if(!listContains(allSynonyms, patternSynonyms[i])) {
											allSynonyms.add(patternSynonyms[i]);
											searchForSynonyms.add(patternSynonyms[i]);
										}
									}
								}
								
								return true;
							}
						}
				),
				waitingPolicy,
				new NullProgressMonitor() {
					public void setCanceled(boolean value) {
						monitor.setCanceled(value);
					}
					public boolean isCanceled() {
						return monitor.isCanceled();
					}
				}
			);
		}
		
		return (char[][])allSynonyms.toArray(new char[allSynonyms.size()][]);
	}
	
	private static boolean listContains(List list, Object elem) {
		boolean contains = false;
		
		//need to do char equals if char array
		if(elem instanceof char[]) {
			char[] needle = (char[])elem;
			for(int i = 0; i < list.size() && !contains; ++i) {
				contains = list.get(i) instanceof char[] && CharOperation.equals((char[])list.get(i), needle);
			}
		} else {
			contains = list.contains(elem);
		}
		
		return contains;
	}
}
