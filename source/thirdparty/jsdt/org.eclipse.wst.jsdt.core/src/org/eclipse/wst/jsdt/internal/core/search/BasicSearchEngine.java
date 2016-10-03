/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchDocument;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.core.CompilationUnit;
import org.eclipse.wst.jsdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IndexManager;
import org.eclipse.wst.jsdt.internal.core.search.matching.ConstructorPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.DeclarationOfAccessedFieldsPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.DeclarationOfReferencedMethodsPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.DeclarationOfReferencedTypesPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.FieldPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.LocalVariablePattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator;
import org.eclipse.wst.jsdt.internal.core.search.matching.MethodPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.MultiTypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.OrPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.SecondaryTypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.processing.IJob;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Search basic engine. Public search engine (see {@link org.eclipse.wst.jsdt.core.search.SearchEngine}
 * for detailed comment), now uses basic engine functionalities.
 * Note that search basic engine does not implement depreciated functionalities...
 */
public class BasicSearchEngine {
	private static final String GLOBAL_TYPE_SYMBOL = new String(IIndexConstants.GLOBAL_SYMBOL);

	/*
	 * A default parser to parse non-reconciled working copies
	 */
	private Parser parser;
	private CompilerOptions compilerOptions;

	/*
	 * A list of working copies that take precedence over their original
	 * compilation units.
	 */
	private IJavaScriptUnit[] workingCopies;
	
	/**
	 * <p>Set of all of the working copies paths</p>
	 */
	private HashSet fWorkingCopiesPaths;

	/*
	 * A working copy owner whose working copies will take precedent over
	 * their original compilation units.
	 */
	private WorkingCopyOwner workingCopyOwner;

	/**
	 * For tracing purpose.
	 */
	public static boolean VERBOSE = false;

	/*
	 * Creates a new search basic engine.
	 */
	public BasicSearchEngine() {
		// will use working copies of PRIMARY owner
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#SearchEngine(IJavaScriptUnit[]) for detailed comment.
	 */
	public BasicSearchEngine(IJavaScriptUnit[] workingCopies) {
		this.workingCopies = workingCopies;
	}

	char convertTypeKind(int typeDeclarationKind) {
		switch(typeDeclarationKind) {
			case TypeDeclaration.CLASS_DECL : return IIndexConstants.CLASS_SUFFIX;
			default : return IIndexConstants.TYPE_SUFFIX;
		}
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#SearchEngine(WorkingCopyOwner) for detailed comment.
	 */
	public BasicSearchEngine(WorkingCopyOwner workingCopyOwner) {
		this.workingCopyOwner = workingCopyOwner;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createHierarchyScope(IType) for detailed comment.
	 */
	public static IJavaScriptSearchScope createHierarchyScope(IType type) throws JavaScriptModelException {
		return createHierarchyScope(type, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createHierarchyScope(IType,WorkingCopyOwner) for detailed comment.
	 */
	public static IJavaScriptSearchScope createHierarchyScope(IType type, WorkingCopyOwner owner) throws JavaScriptModelException {
		return new HierarchyScope(type, owner);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createJavaSearchScope(IJavaScriptElement[]) for detailed comment.
	 */
	public static IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] elements) {
		return createJavaSearchScope(elements, true);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createJavaSearchScope(IJavaScriptElement[], boolean) for detailed comment.
	 */
	public static IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] elements, boolean includeReferencedProjects) {
		int includeMask = IJavaScriptSearchScope.SOURCES | IJavaScriptSearchScope.APPLICATION_LIBRARIES | IJavaScriptSearchScope.SYSTEM_LIBRARIES;
		if (includeReferencedProjects) {
			includeMask |= IJavaScriptSearchScope.REFERENCED_PROJECTS;
		}
		return createJavaSearchScope(elements, includeMask);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createJavaSearchScope(IJavaScriptElement[], int) for detailed comment.
	 */
	public static IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] elements, int includeMask) {
		JavaSearchScope scope = new JavaSearchScope();
		HashSet visitedProjects = new HashSet(2);
		for (int i = 0, length = elements.length; i < length; i++) {
			IJavaScriptElement element = elements[i];
			if (element != null) {
				try {
					if (element instanceof JavaProject) {
						scope.add((JavaProject)element, includeMask, visitedProjects);
					} else {
						scope.add(element);
					}
				} catch (JavaScriptModelException e) {
					// ignore
				}
			}
		}
		return scope;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createTypeNameMatch(IType, int) for detailed comment.
	 */
	public static TypeNameMatch createTypeNameMatch(IType type, int modifiers) {
		return new JavaSearchTypeNameMatch(type, modifiers);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#createWorkspaceScope() for detailed comment.
	 */
	public static IJavaScriptSearchScope createWorkspaceScope() {
		return JavaModelManager.getJavaModelManager().getWorkspaceScope();
	}

	/**
	 * Searches for matches to a given query. Search queries can be created using helper
	 * methods (from a String pattern or a Java element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @param scope the search result has to be limited to the given scope
	 * @param requestor a callback object to which each match is reported
	 */
	void findMatches(SearchPattern pattern, SearchParticipant[] participants, IJavaScriptSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		try {
			if (VERBOSE) {
				Util.verbose("Searching for pattern: " + pattern.toString()); //$NON-NLS-1$
				Util.verbose(scope.toString());
			}
			if (participants == null) {
				if (VERBOSE) Util.verbose("No participants => do nothing!"); //$NON-NLS-1$
				return;
			}

			/* initialize progress monitor */
			int length = participants.length;
			if (monitor != null)
				monitor.beginTask(Messages.engine_searching, 100 * length);
			IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
			requestor.beginReporting();
			for (int i = 0; i < length; i++) {
				if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();

				SearchParticipant participant = participants[i];
				try {
					if (monitor != null) monitor.subTask(Messages.bind(Messages.engine_searching_indexing, new String[] {participant.getDescription()}));
					participant.beginSearching();
					requestor.enterParticipant(participant);
					PathCollector pathCollector = new PathCollector();
					indexManager.performConcurrentJob(
						new PatternSearchJob(pattern, participant, scope, pathCollector),
						IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
						monitor==null ? null : new SubProgressMonitor(monitor, 50));
					if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();

					// locate index matches if any (note that all search matches could have been issued during index querying)
					if (monitor != null) monitor.subTask(Messages.bind(Messages.engine_searching_matching, new String[] {participant.getDescription()}));
					String[] indexMatchPaths = pathCollector.getPaths();
					if (indexMatchPaths != null) {
						pathCollector = null; // release
						int indexMatchLength = indexMatchPaths.length;
						SearchDocument[] indexMatches = new SearchDocument[indexMatchLength];
						for (int j = 0; j < indexMatchLength; j++) {
							indexMatches[j] = participant.getDocument(indexMatchPaths[j]);
						}
						SearchDocument[] matches = MatchLocator.addWorkingCopies(pattern, indexMatches, getWorkingCopies(), participant);
						participant.locateMatches(matches, pattern, scope, requestor, monitor==null ? null : new SubProgressMonitor(monitor, 50));
					}
				} finally {
					requestor.exitParticipant(participant);
					participant.doneSearching();
				}
			}
		} finally {
			requestor.endReporting();
			if (monitor != null)
				monitor.done();
		}
	}
	/**
	 * Returns a new default Java search participant.
	 *
	 * @return a new default Java search participant
	 * @since 3.0
	 */
	public static SearchParticipant getDefaultSearchParticipant() {
		return new JavaSearchParticipant();
	}


	/**
	 * @param matchRule
	 */
	public static String getMatchRuleString(final int matchRule) {
		if (matchRule == 0) {
			return "R_EXACT_MATCH"; //$NON-NLS-1$
		}
		StringBuffer buffer = new StringBuffer();
		for (int i=1; i<=8; i++) {
			int bit = matchRule & (1<<(i-1));
			if (bit != 0 && buffer.length()>0) buffer.append(" | "); //$NON-NLS-1$
			switch (bit) {
				case SearchPattern.R_PREFIX_MATCH:
					buffer.append("R_PREFIX_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_CASE_SENSITIVE:
					buffer.append("R_CASE_SENSITIVE"); //$NON-NLS-1$
					break;
				case SearchPattern.R_EQUIVALENT_MATCH:
					buffer.append("R_EQUIVALENT_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_ERASURE_MATCH:
					buffer.append("R_ERASURE_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_FULL_MATCH:
					buffer.append("R_FULL_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_PATTERN_MATCH:
					buffer.append("R_PATTERN_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_REGEXP_MATCH:
					buffer.append("R_REGEXP_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_CAMELCASE_MATCH:
					buffer.append("R_CAMELCASE_MATCH"); //$NON-NLS-1$
					break;
			}
		}
		return buffer.toString();
	}

	/**
	 * Return kind of search corresponding to given value.
	 *
	 * @param searchFor
	 */
	public static String getSearchForString(final int searchFor) {
		switch (searchFor) {
			case IJavaScriptSearchConstants.TYPE:
				return ("TYPE"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.METHOD:
				return ("METHOD"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.PACKAGE:
				return ("PACKAGE"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.CONSTRUCTOR:
				return ("CONSTRUCTOR"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.FIELD:
				return ("FIELD"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.CLASS:
				return ("CLASS"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.VAR:
				return ("VAR"); //$NON-NLS-1$
			case IJavaScriptSearchConstants.FUNCTION:
				return ("FUNCTION"); //$NON-NLS-1$
		}
		return "UNKNOWN"; //$NON-NLS-1$
	}

	private Parser getParser() {
		if (this.parser == null) {
			this.compilerOptions = new CompilerOptions(JavaScriptCore.getOptions());
			ProblemReporter problemReporter =
				new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					this.compilerOptions,
					new DefaultProblemFactory());
			this.parser = new Parser(problemReporter, true);
		}
		return this.parser;
	}

	/**
	 * @return list of working copies used by this search engine,
	 * or <code>null</code> if none.
	 */
	private IJavaScriptUnit[] getWorkingCopies() {
		IJavaScriptUnit[] copies;
		if (this.workingCopies != null) {
			if (this.workingCopyOwner == null) {
				copies = JavaModelManager.getJavaModelManager().getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, false/*don't add primary WCs a second time*/);
				if (copies == null) {
					copies = this.workingCopies;
				} else {
					HashMap pathToCUs = new HashMap();
					for (int i = 0, length = copies.length; i < length; i++) {
						IJavaScriptUnit unit = copies[i];
						pathToCUs.put(unit.getPath(), unit);
					}
					for (int i = 0, length = this.workingCopies.length; i < length; i++) {
						IJavaScriptUnit unit = this.workingCopies[i];
						pathToCUs.put(unit.getPath(), unit);
					}
					int length = pathToCUs.size();
					copies = new IJavaScriptUnit[length];
					pathToCUs.values().toArray(copies);
				}
			} else {
				copies = this.workingCopies;
			}
		} else if (this.workingCopyOwner != null) {
			copies = JavaModelManager.getJavaModelManager().getWorkingCopies(this.workingCopyOwner, true/*add primary WCs*/);
		} else {
			copies = JavaModelManager.getJavaModelManager().getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, false/*don't add primary WCs a second time*/);
		}
		if (copies == null) return null;

		// filter out primary working copies that are saved
		IJavaScriptUnit[] result = null;
		int length = copies.length;
		int index = 0;
		for (int i = 0; i < length; i++) {
			CompilationUnit copy = (CompilationUnit)copies[i];
			try {
				if (!copy.isPrimary()
						|| copy.hasUnsavedChanges()
						|| copy.hasResourceChanged()) {
					if (result == null) {
						result = new IJavaScriptUnit[length];
					}
					result[index++] = copy;
				}
			}  catch (JavaScriptModelException e) {
				// copy doesn't exist: ignore
			}
		}
		if (index != length && result != null) {
			System.arraycopy(result, 0, result = new IJavaScriptUnit[index], 0, index);
		}
		return result;
	}
	
	/**
	 * @return {@link HashSet} of all of the working copy paths
	 */
	private HashSet getWorkingCopiesPaths() {
		if(this.fWorkingCopiesPaths == null) {
			this.fWorkingCopiesPaths = new HashSet();
			
			IJavaScriptUnit[] workingCopies = this.getWorkingCopies();
			for(int i = 0; workingCopies != null && i < workingCopies.length; ++i) {
				this.fWorkingCopiesPaths.add(workingCopies[i].getPath().toString());
			}
		}
		
		return this.fWorkingCopiesPaths;
	}

	/*
	 * Returns the list of working copies used to do the search on the given Java element.
	 */
	private IJavaScriptUnit[] getWorkingCopies(IJavaScriptElement element) {
		if (element instanceof IMember) {
			IJavaScriptUnit cu = ((IMember)element).getJavaScriptUnit();
			if (cu != null && cu.isWorkingCopy()) {
				IJavaScriptUnit[] copies = getWorkingCopies();
				int length = copies == null ? 0 : copies.length;
				if (length > 0) {
					IJavaScriptUnit[] newWorkingCopies = new IJavaScriptUnit[length+1];
					System.arraycopy(copies, 0, newWorkingCopies, 0, length);
					newWorkingCopies[length] = cu;
					return newWorkingCopies;
				}
				return new IJavaScriptUnit[] {cu};
			}
		}
		return getWorkingCopies();
	}

	boolean match(char patternTypeSuffix, int modifiers) {
		switch(patternTypeSuffix) {
			case IIndexConstants.CLASS_SUFFIX :
				return modifiers == 0;
		}
		return true;
	}

	boolean match(char patternTypeSuffix, char[] patternPkg, char[] patternTypeName, int matchRule, int typeKind, char[] pkg, char[] typeName) {
		if (typeName==null)
			typeName=CharOperation.NO_CHAR;
		switch(patternTypeSuffix) {
			case IIndexConstants.CLASS_SUFFIX :
				if (typeKind != TypeDeclaration.CLASS_DECL) return false;
				break;
			case IIndexConstants.TYPE_SUFFIX : // nothing
		}

		boolean isCaseSensitive = (matchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
		if (patternPkg != null && !CharOperation.equals(patternPkg, pkg, isCaseSensitive))
				return false;

		if (patternTypeName != null) {
			boolean isCamelCase = (matchRule & SearchPattern.R_CAMELCASE_MATCH) != 0;
			int matchMode = matchRule & JavaSearchPattern.MATCH_MODE_MASK;
			if (!isCaseSensitive && !isCamelCase) {
				patternTypeName = CharOperation.toLowerCase(patternTypeName);
			}
			boolean matchFirstChar = !isCaseSensitive || patternTypeName[0] == typeName[0];
			if (isCamelCase && matchFirstChar && CharOperation.camelCaseMatch(patternTypeName, typeName)) {
				return true;
			}
			switch(matchMode) {
				case SearchPattern.R_EXACT_MATCH :
					if (!isCamelCase) {
						return matchFirstChar && CharOperation.equals(patternTypeName, typeName, isCaseSensitive);
					}
					// fall through next case to match as prefix if camel case failed
				case SearchPattern.R_PREFIX_MATCH :
					return matchFirstChar && CharOperation.prefixEquals(patternTypeName, typeName, isCaseSensitive);
				case SearchPattern.R_PATTERN_MATCH :
					return CharOperation.match(patternTypeName, typeName, isCaseSensitive);
				case SearchPattern.R_REGEXP_MATCH :
					// TODO (frederic) implement regular expression match
					break;
			}
		}
		return true;

	}

	/**
	 * Searches for matches of a given search pattern. Search patterns can be created using helper
	 * methods (from a String pattern or a Java element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#search(SearchPattern, SearchParticipant[], IJavaScriptSearchScope, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void search(SearchPattern pattern, SearchParticipant[] participants, IJavaScriptSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.search(SearchPattern, SearchParticipant[], IJavaScriptSearchScope, SearchRequestor, IProgressMonitor)"); //$NON-NLS-1$
		}
		findMatches(pattern, participants, scope, requestor, monitor);
	}
	
	public void searchAllBindingNames(
			final char[] packageName,
			final char[] bindingName,
			final int bindingType,
			final int matchRule,
			IJavaScriptSearchScope scope,
			final IRestrictedAccessBindingRequestor nameRequestor,
			int waitingPolicy,
			boolean doParse,
			IProgressMonitor progressMonitor)  throws JavaScriptModelException {

			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllBindingNames(char[], char[], int, int, IJavaScriptSearchScope, IRestrictedAccessTypeRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
				Util.verbose("	- package name: "+(packageName==null?"null":new String(packageName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- type name: "+(bindingName==null?"null":new String(bindingName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- match rule: "+getMatchRuleString(matchRule)); //$NON-NLS-1$
				Util.verbose("	- bindingType for: "+bindingType); //$NON-NLS-1$
				Util.verbose("	- scope: "+scope); //$NON-NLS-1$
			}

			IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
			SearchPattern searchPattern=null;
			char suffix=0;
			switch(bindingType){

				case Binding.TYPE :
				{
					

					suffix = IIndexConstants.CLASS_SUFFIX;
					searchPattern = new TypeDeclarationPattern(
							packageName,
							bindingName,
							matchRule);

					break;
				}
				case Binding.VARIABLE :
				case Binding.LOCAL :
				case Binding.FIELD :
				{
					//searchPattern = new   LocalVariablePattern(true, false, false,bindingName,   matchRule);
					searchPattern = new FieldPattern(true, false, false, true, bindingName, null, IIndexConstants.GLOBAL_SYMBOL, null, null, matchRule, null);
				}
				break;
				case Binding.METHOD:
				{
					searchPattern = new MethodPattern(
							true,false,true,
							bindingName,
							null,null,null,null,
							null,IIndexConstants.GLOBAL_SYMBOL,
							matchRule);

				}
				break;
				default: // some combination
				{
					if ((bindingType & Binding.METHOD) >0)
					{
						searchPattern = new MethodPattern(
								true,false,true,
								bindingName,
								matchRule);

					}
					if ((bindingType & (Binding.VARIABLE |Binding.LOCAL |Binding.FIELD )) >0)
					{
						//LocalVariablePattern localVariablePattern = new   LocalVariablePattern(true, false, false,bindingName,   matchRule);
						FieldPattern fieldPattern = new FieldPattern(true, false, false, true, bindingName, null, IIndexConstants.GLOBAL_SYMBOL, null, null, matchRule, null);
						if (searchPattern==null)
							searchPattern=fieldPattern;
						else
							searchPattern=new OrPattern(searchPattern,fieldPattern);
					}
					if ((bindingType & Binding.TYPE) >0)
					{
						suffix = IIndexConstants.CLASS_SUFFIX;
						TypeDeclarationPattern typeDeclarationPattern = new TypeDeclarationPattern(
								packageName,
								bindingName,
								matchRule);
							if (searchPattern==null)
								searchPattern=typeDeclarationPattern;
							else
								searchPattern=new OrPattern(searchPattern,typeDeclarationPattern);
					}
				}
			}
			final SearchPattern pattern =searchPattern;
			final char typeSuffix=suffix;

			// Index requester
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					// Filter unexpected types
					JavaSearchPattern record = (JavaSearchPattern)indexRecord;

					// Accept document path
					AccessRestriction accessRestriction = null;
					int modifiers=ClassFileConstants.AccPublic;
					char[] packageName=null;
					char[] simpleBindingName=null;
					if (record instanceof MethodPattern) {
						MethodPattern methodPattern = (MethodPattern) record;
						simpleBindingName=methodPattern.selector;
						Path path = new Path(documentPath);
						String string = path.lastSegment();
						if (path.hasTrailingSeparator())	// is library
						{
							packageName=string.toCharArray();
						}
					}
					else if (record instanceof LocalVariablePattern)
					{
						LocalVariablePattern localVariablePattern = (LocalVariablePattern) record;
						simpleBindingName=localVariablePattern.name;
						Path path = new Path(documentPath);
						String string = path.lastSegment();
						if (path.hasTrailingSeparator())	// is library
						{
							packageName=string.toCharArray();
						}

					}else if (record instanceof TypeDeclarationPattern) {
						TypeDeclarationPattern typeDecPattern = (TypeDeclarationPattern)record;
						simpleBindingName=typeDecPattern.simpleName;
						Path path = new Path(documentPath);
						String string = path.lastSegment();
						if (path.hasTrailingSeparator())	// is library
						{
							packageName=string.toCharArray();
						}
					}

					nameRequestor.acceptBinding( bindingType,modifiers, packageName, simpleBindingName,   documentPath, accessRestriction);

					return true;
				}
			};

			try {
				if (progressMonitor != null) {
					progressMonitor.beginTask(Messages.engine_searching, 100);
				}
				// add type names from indexes
				indexManager.performConcurrentJob(
					new PatternSearchJob(
						pattern,
						getDefaultSearchParticipant(), // Java search only
						scope,
						searchRequestor),
					waitingPolicy,
					progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 100));

				// add type names from working copies
				IJavaScriptUnit[] copies = getWorkingCopies();
				final int copiesLength = copies == null ? 0 : copies.length;
				if (copies != null && doParse) {
					for (int i = 0; i < copiesLength; i++) {
						IJavaScriptUnit workingCopy = copies[i];
						if (!scope.encloses(workingCopy)) continue;
						final String path = workingCopy.getPath().toString();
						if (workingCopy.isConsistent()) {
							char[] packageDeclaration = CharOperation.NO_CHAR;
							switch (bindingType)
							{
							case Binding.TYPE:
							{
								IType[] allTypes = workingCopy.getAllTypes();
								for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
									IType type = allTypes[j];
									char[] simpleName = type.getElementName().toCharArray();
									int kind = TypeDeclaration.CLASS_DECL;
									
									if (match(typeSuffix, packageName, bindingName, matchRule, kind, packageDeclaration, simpleName)) {
										nameRequestor.acceptBinding(bindingType,type.getFlags(), packageDeclaration, simpleName,   path, null);
									}
								}
							}
							case Binding.METHOD:
							{
								IFunction[] allMethods = workingCopy.getFunctions();
								for (int j = 0, allMethodsLength = allMethods.length; j < allMethodsLength; j++) {
									IFunction method = allMethods[j];
									char[] simpleName = method.getElementName().toCharArray();
									if (match(typeSuffix, packageName, bindingName, matchRule, 0, packageDeclaration, simpleName)) {
										nameRequestor.acceptBinding(bindingType,method.getFlags(), packageDeclaration, simpleName,   path, null);
									}
								}
							}
							break;
							case Binding.VARIABLE :
							case Binding.LOCAL :
							case Binding.FIELD :
							{
								IField[] allFields = workingCopy.getFields ();
								for (int j = 0, allFieldsLength = allFields.length; j < allFieldsLength; j++) {
									IField field = allFields[j];
									char[] simpleName = field.getElementName().toCharArray();
									if (match(typeSuffix, packageName, bindingName, matchRule, 0, packageDeclaration, simpleName)) {
										nameRequestor.acceptBinding(bindingType,field.getFlags(), packageDeclaration, simpleName,   path, null);
									}
								}
							}
							break;
							}
						} else {
							Parser basicParser = getParser();
							org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit unit = (org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit) workingCopy;
							CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.compilerOptions.maxProblemsPerUnit);
							CompilationUnitDeclaration parsedUnit = basicParser.dietParse(unit, compilationUnitResult);
							if (parsedUnit != null) {
								basicParser.inferTypes(parsedUnit, null);
								final char[] packageDeclaration = parsedUnit.currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(parsedUnit.currentPackage.getImportName(), '.');
								class AllTypeDeclarationsVisitor extends ASTVisitor {
//									public boolean visit(TypeDeclaration typeDeclaration, Scope blockScope) {
//										return false; // no local/anonymous type
//									}
									public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope compilationUnitScope) {
										if (bindingType==Binding.TYPE &&
												match(typeSuffix, packageName, bindingName, matchRule, TypeDeclaration.kind(typeDeclaration.modifiers), packageDeclaration, typeDeclaration.name)) {
											nameRequestor.acceptBinding(bindingType,typeDeclaration.modifiers, packageDeclaration, typeDeclaration.name,  path, null);
										}
										return true;
									}
									public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
										if ((scope instanceof CompilationUnitScope) && (bindingType==Binding.LOCAL || bindingType==Binding.FIELD || bindingType==Binding.VARIABLE )&&
												match(typeSuffix, packageName, bindingName, matchRule,0, packageDeclaration, localDeclaration.name)) {
											nameRequestor.acceptBinding(bindingType,localDeclaration.modifiers, packageDeclaration,  localDeclaration.name,  path, null);
										}
										return true;
									}
									public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
										char[] methName = methodDeclaration.getName();
										if (bindingType==Binding.METHOD && methName!=null &&
												match(typeSuffix, packageName, bindingName, matchRule,0, packageDeclaration, methName)) {
											nameRequestor.acceptBinding(bindingType,methodDeclaration.modifiers, packageDeclaration,  methName,  path, null);
										}
										return true;
									}
									public boolean visit(InferredType inferredType, BlockScope scope) {
										if (bindingType==Binding.TYPE &&
													match(typeSuffix, packageName, bindingName, matchRule, TypeDeclaration.kind(0), packageDeclaration, inferredType.getName())) {
												nameRequestor.acceptBinding(bindingType,0, packageDeclaration, inferredType.getName(),  path, null);
											}
										return true;
									}
									public boolean visit(InferredAttribute inferredField, BlockScope scope) {
										if ((scope instanceof CompilationUnitScope) && (bindingType==Binding.LOCAL || bindingType==Binding.FIELD || bindingType==Binding.VARIABLE )&&
													match(typeSuffix, packageName, bindingName, matchRule,0, packageDeclaration, inferredField.name)) {
												nameRequestor.acceptBinding(bindingType,inferredField.modifiers, packageDeclaration,  inferredField.name,  path, null);
										}
										return true;
									}
									public boolean visit(InferredMethod inferredMethod, BlockScope scope) {
										if (bindingType==Binding.METHOD && inferredMethod.name!=null &&
													match(typeSuffix, packageName, bindingName, matchRule,0, packageDeclaration, inferredMethod.name)) {
												nameRequestor.acceptBinding(bindingType,((MethodDeclaration)inferredMethod.getFunctionDeclaration()).modifiers, packageDeclaration,  inferredMethod.name,  path, null);
											}
										return true;
									}
								}
								parsedUnit.traverse(new AllTypeDeclarationsVisitor(), parsedUnit.scope);
							}
						}
					}
				}
			} finally {
				if (progressMonitor != null) {
					progressMonitor.done();
				}
			}
		}




	/**
	 * Searches for all secondary types in the given scope.
	 * The search can be selecting specific types (given a package or a type name
	 * prefix and match modes).
	 */
	public void searchAllSecondaryTypeNames(
			IPackageFragmentRoot[] sourceFolders,
			final IRestrictedAccessTypeRequestor nameRequestor,
			boolean waitForIndexes,
			IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchAllSecondaryTypeNames(IPackageFragmentRoot[], IRestrictedAccessTypeRequestor, boolean, IProgressMonitor)"); //$NON-NLS-1$
			StringBuffer buffer = new StringBuffer("	- source folders: "); //$NON-NLS-1$
			int length = sourceFolders.length;
			for (int i=0; i<length; i++) {
				if (i==0) {
					buffer.append('[');
				} else {
					buffer.append(',');
				}
				buffer.append(sourceFolders[i].getElementName());
			}
			buffer.append("]\n	- waitForIndexes: "); //$NON-NLS-1$
			buffer.append(waitForIndexes);
			Util.verbose(buffer.toString());
		}

		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		final TypeDeclarationPattern pattern = new SecondaryTypeDeclarationPattern();

		// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requester
		final HashSet workingCopyPaths = this.getWorkingCopiesPaths();

		// Index requester
		IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
			public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
				// Filter unexpected types
				TypeDeclarationPattern record = (TypeDeclarationPattern)indexRecord;

				if (record.enclosingTypeNames == IIndexConstants.ONE_ZERO_CHAR) {
					return true; // filter out local and anonymous classes
				}
				
				if (workingCopyPaths.contains(documentPath)) {
					return true; // filter out working copies
				}

				// Accept document path
				AccessRestriction accessRestriction = null;
				if (access != null) {
					// Compute document relative path
					int pkgLength = (record.qualification==null || record.qualification.length==0) ? 0 : record.qualification.length+1;
					int nameLength = record.simpleName==null ? 0 : record.simpleName.length;
					char[] path = new char[pkgLength+nameLength];
					int pos = 0;
					if (pkgLength > 0) {
						System.arraycopy(record.qualification, 0, path, pos, pkgLength-1);
						CharOperation.replace(path, '.', '/');
						path[pkgLength-1] = '/';
						pos += pkgLength;
					}
					if (nameLength > 0) {
						System.arraycopy(record.simpleName, 0, path, pos, nameLength);
						pos += nameLength;
					}
					// Update access restriction if path is not empty
					if (pos > 0) {
						accessRestriction = access.getViolatedRestriction(path);
					}
				}
				nameRequestor.acceptType(record.modifiers, record.qualification, record.simpleName, record.superTypes, record.enclosingTypeNames, documentPath, accessRestriction);
				return true;
			}
		};

		// add type names from indexes
		try {
			if (progressMonitor != null) {
				progressMonitor.beginTask(Messages.engine_searching, 100);
			}
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					createJavaSearchScope(sourceFolders),
					searchRequestor),
				waitForIndexes
					? IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH
					: IJavaScriptSearchConstants.FORCE_IMMEDIATE_SEARCH,
				progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 100));
		} catch (OperationCanceledException oce) {
			// do nothing
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}
	
	/**
	 * <p>Search for types using the given prefix. The prefix could be part of the
	 * qualification or simple name for  a type, or it could be a camel case
	 * statement for a simple name of a type.</p>
	 * 
	 * @param prefix
	 * @param matchRule
	 * @param scope
	 * @param nameRequestor
	 * @param waitingPolicy
	 * @param progressMonitor
	 * @throws JavaScriptModelException
	 */
	public void searchAllTypeNames(
		final char[] prefix,
		final int matchRule,
		IJavaScriptSearchScope scope,
		final IRestrictedAccessTypeRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		// Create pattern
		TypeDeclarationPattern pattern = new TypeDeclarationPattern(prefix, matchRule);

		this.searchAllTypeNames(pattern, scope, nameRequestor, waitingPolicy, progressMonitor);
	}

	/**
	 * <p>Search for a type with a specific qualification and simple name</p>
	 * 
	 * @param qualification
	 * @param qualificationMatchRule
	 * @param simpleTypeName
	 * @param matchRule
	 * @param scope
	 * @param nameRequestor
	 * @param waitingPolicy
	 * @param progressMonitor
	 * @throws JavaScriptModelException
	 */
	public void searchAllTypeNames(
		final char[] qualification,
		final char[] simpleTypeName,
		final int matchRule,
		IJavaScriptSearchScope scope,
		final IRestrictedAccessTypeRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchAllTypeNames(char[], char[], int, int, IJavaScriptSearchScope, IRestrictedAccessTypeRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
			Util.verbose("	- package name: "+(qualification==null?"null":new String(qualification))); //$NON-NLS-1$ //$NON-NLS-2$
			Util.verbose("	- type name: "+(simpleTypeName==null?"null":new String(simpleTypeName))); //$NON-NLS-1$ //$NON-NLS-2$
			Util.verbose("	- match rule: "+getMatchRuleString(matchRule)); //$NON-NLS-1$
			Util.verbose("	- scope: "+scope); //$NON-NLS-1$
		}

		// Create pattern
		TypeDeclarationPattern pattern = new TypeDeclarationPattern(
				qualification,
				simpleTypeName,
				matchRule);

		this.searchAllTypeNames(pattern, scope, nameRequestor, waitingPolicy, progressMonitor);
	}

	/**
	 * <p>Search for multiple types with specific qualifications and simple type names.</p>
	 * 
	 * @param qualifications
	 * @param simpleTypeNames
	 * @param matchRule
	 * @param scope
	 * @param nameRequestor
	 * @param waitingPolicy
	 * @param progressMonitor
	 * @throws JavaScriptModelException
	 */
	public void searchAllTypeNames(
		final char[][] qualifications,
		final char[][] simpleTypeNames,
		final int matchRule,
		IJavaScriptSearchScope scope,
		final IRestrictedAccessTypeRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchAllTypeNames(char[][], char[][], int, int, IJavaScriptSearchScope, IRestrictedAccessTypeRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
			Util.verbose("	- package name: "+(qualifications==null?"null":new String(CharOperation.concatWith(qualifications, ',')))); //$NON-NLS-1$ //$NON-NLS-2$
			Util.verbose("	- type name: "+(simpleTypeNames==null?"null":new String(CharOperation.concatWith(simpleTypeNames, ',')))); //$NON-NLS-1$ //$NON-NLS-2$
			Util.verbose("	- match rule: "+matchRule); //$NON-NLS-1$
			Util.verbose("	- scope: "+scope); //$NON-NLS-1$
		}

		MultiTypeDeclarationPattern pattern = new MultiTypeDeclarationPattern(
				qualifications, simpleTypeNames, matchRule);
		this.searchAllTypeNames(pattern, scope, nameRequestor, waitingPolicy, progressMonitor);
	}
	
	/**
	 * <p>Used to search for types using a given pattern.</p>
	 * 
	 * @param pattern
	 * @param scope
	 * @param nameRequestor
	 * @param waitingPolicy
	 * @param progressMonitor
	 * @throws JavaScriptModelException
	 */
	private void searchAllTypeNames(
		final TypeDeclarationPattern pattern,
		IJavaScriptSearchScope scope,
		final IRestrictedAccessTypeRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaScriptModelException {

		// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
		final HashSet workingCopyPaths = this.getWorkingCopiesPaths();

		// Index requestor
		IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
			public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
				// Filter unexpected types
				TypeDeclarationPattern record = (TypeDeclarationPattern) indexRecord;
				if (record.enclosingTypeNames == IIndexConstants.ONE_ZERO_CHAR) {
					return true; // filter out local and anonymous classes
				}
				
				if (workingCopyPaths.contains(documentPath)) {
					return true; // filter out working copies
				}

				// Accept document path
				AccessRestriction accessRestriction = null;
				if (access != null) {
					// Compute document relative path
					int qualificationLength = (record.qualification == null || record.qualification.length == 0) ? 0 : record.qualification.length + 1;
					int nameLength = record.simpleName == null ? 0 : record.simpleName.length;
					char[] path = new char[qualificationLength + nameLength];
					int pos = 0;
					if (qualificationLength > 0) {
						System.arraycopy(record.qualification, 0, path, pos, qualificationLength - 1);
						CharOperation.replace(path, '.', '/');
						path[qualificationLength-1] = '/';
						pos += qualificationLength;
					}
					if (nameLength > 0) {
						System.arraycopy(record.simpleName, 0, path, pos, nameLength);
						pos += nameLength;
					}
					// Update access restriction if path is not empty
					if (pos > 0) {
						accessRestriction = access.getViolatedRestriction(path);
					}
				}
				nameRequestor.acceptType(record.modifiers, record.qualification, record.simpleName, record.superTypes, record.enclosingTypeNames, documentPath, accessRestriction);
				return true;
			}
		};

		try {
			if (progressMonitor != null) {
				progressMonitor.beginTask(Messages.engine_searching, 100);
			}
			// add type names from indexes
			IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					scope,
					searchRequestor),
				waitingPolicy,
				progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 100));

			// add type names from working copies
			IJavaScriptUnit[] copies = this.getWorkingCopies();
			if (copies != null) {
				final int matchRule = pattern.getMatchRule();
				for (int i = 0; i < copies.length; i++) {
					final IJavaScriptUnit workingCopy = copies[i];
					if (!scope.encloses(workingCopy)) continue;
					final String path = workingCopy.getPath().toString();
					
					//make the working copy consistent if it is not
					if(!workingCopy.isConsistent()) {
						workingCopy.makeConsistent(progressMonitor);
					}
					
					//search all types in the working copy
					IType[] allTypes = workingCopy.getAllTypes();
					for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
						IType type = allTypes[j];
						
						//get type name
						char[] wcTypeQualification = null;
						char[] wcTypeSimpleName = null;
						char[] wcTypeFullName = type.getTypeQualifiedName().toCharArray();
						if(type.getTypeQualifiedName() != null) {
							char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(wcTypeFullName);
							wcTypeQualification = wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX];
							wcTypeSimpleName = wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX];
						}
						
						if(wcTypeSimpleName != null) {
							TypeDeclarationPattern wcPattern = new TypeDeclarationPattern(wcTypeQualification, wcTypeSimpleName, matchRule);
							if (pattern.matchesDecodedKey(wcPattern)) {
								char[][] superTypes = CharOperation.NO_CHAR_CHAR;
								try {
									String superType = type.getSuperclassName();
									superTypes = new char[][]{superType.toCharArray()};
								}
								catch (JavaScriptModelException e) {
									// stay empty
								}
								nameRequestor.acceptType(type.getFlags(), wcTypeQualification, wcTypeSimpleName, superTypes, null, path, null);
							}
						}
					}
				}
			}
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	public void searchAllModuleDeclarations(SearchPattern pattern, IJavaScriptSearchScope scope, final IModuleRequestor requestor, IProgressMonitor monitor) {
		final HashSet workingCopiesPaths = this.getWorkingCopiesPaths();
		
		// Index requestor
		IndexQueryRequestor queryRequestor = new IndexQueryRequestor() {
			public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
				if(!workingCopiesPaths.contains(documentPath)) {
					if (indexRecord instanceof MethodPattern) {
						MethodPattern record = (MethodPattern) indexRecord;
						requestor.acceptFunction(record.selector,
									record.getDeclaringQualification(), 
									record.getDeclaringSimpleName(),
									documentPath);
					}
					else if (indexRecord instanceof FieldPattern) {
						FieldPattern record = (FieldPattern) indexRecord;
						requestor.acceptField(record.name,
									record.getDeclaringQualification(),
									record.getDeclaringSimpleName(),
									documentPath);
					}
					else if (indexRecord instanceof TypeDeclarationPattern) {
						TypeDeclarationPattern record = (TypeDeclarationPattern) indexRecord;
						requestor.acceptType(record.qualification, 
									record.simpleName, 
									documentPath);
					}
				}
				return true;
			}
		};

		// Find matches from index
		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		try {
			if (monitor != null) {
				monitor.beginTask(Messages.engine_searching, 1000);
			}

			indexManager.performConcurrentJob(new PatternSearchJob(
						pattern,
						getDefaultSearchParticipant(), // JavaScript search only
						scope,
						queryRequestor),
						IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
						monitor == null ? null : new SubProgressMonitor(monitor, 1000));

			SearchPattern typePattern = null;
			SearchPattern fieldPattern = null;
			SearchPattern methodPattern = null;
			
			if (pattern instanceof OrPattern) {
				typePattern = ((OrPattern) pattern).findPatternKind(IIndexConstants.TYPE_DECL_PATTERN);
				methodPattern = ((OrPattern) pattern).findPatternKind(IIndexConstants.METHOD_PATTERN);
				fieldPattern = ((OrPattern) pattern).findPatternKind(IIndexConstants.FIELD_PATTERN);
			} else if (pattern instanceof TypeDeclarationPattern) {
				typePattern = pattern;
			} else if (pattern instanceof FieldPattern) {
				fieldPattern = pattern;
			} else if (pattern instanceof MethodPattern) {
				methodPattern = pattern;
			}
			
			// find matches from working copies
			IJavaScriptUnit[] workingCopies = this.getWorkingCopies();
			for (int w = 0; workingCopies != null && w < workingCopies.length; w++) {
				final IJavaScriptUnit workingCopy = workingCopies[w];

				//skip this working copy if not in the scope
				if (!scope.encloses(workingCopy)) {
					continue;
				}

				try {
					//make the working copy consistent if it is not
					if (!workingCopy.isConsistent()) {
						workingCopy.makeConsistent(monitor);
					}

					//check each each type in the working copy for a match
					IType[] types = workingCopy.getAllTypes();
					for (int t = 0; t < types.length; t++) {
						IType type = types[t];
						if (typePattern != null && !type.isAnonymous()) {
							//get type name
							char[] wcTypeSimpleName = null;
							char[] wcTypeQualification = null;
							if (type.getTypeQualifiedName() != null) {
								char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(type.getTypeQualifiedName().toCharArray());
								wcTypeQualification = wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX];
								wcTypeSimpleName = wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX];
							}

							if (wcTypeSimpleName != null) {
								TypeDeclarationPattern wcPattern = new TypeDeclarationPattern(wcTypeQualification, wcTypeSimpleName, pattern.getMatchRule());
								if (typePattern.matchesDecodedKey(wcPattern)) {
									requestor.acceptType(type);
								}
							}
						}
						if (methodPattern != null) {
							IFunction[] allFunctions = type.getFunctions();
							for (int f = 0; f < allFunctions.length; f++) {
								IFunction function = allFunctions[f];
								//selector must not be null for it to be a match
								char[] wcSelector = null;
								if (function.getElementName() != null) {
									wcSelector = function.getElementName().toCharArray();
								}
								char[] wcDeclaringTypeSimpleName = null;
								char[] wcDeclaringTypeQualification = null;
								if (function.getDeclaringType() != null && function.getDeclaringType().getTypeQualifiedName() != null) {
									char[] wcDeclaringType = function.getDeclaringType().getTypeQualifiedName().toCharArray();
									char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(wcDeclaringType);
									wcDeclaringTypeQualification = wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX];
									wcDeclaringTypeSimpleName = wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX];
								}
								if (wcSelector != null) {
									MethodPattern wcPattern = new MethodPattern(true, false, false, wcSelector, null, null, null, null, 
												wcDeclaringTypeQualification, wcDeclaringTypeSimpleName, pattern.getMatchRule());
									//if working copy function matches the search pattern then accept the function
									if (methodPattern.matchesDecodedKey(wcPattern)) {
										requestor.acceptFunction(function);
									}
								}
							}
						}
						if (fieldPattern != null) {
							IField[] allFields = type.getFields();
							for (int i = 0; i < allFields.length; i++) {
								IField field = allFields[i];
								//selector must not be null for it to be a match
								char[] wcName = null;
								if (field.getElementName() != null) {
									wcName = field.getElementName().toCharArray();
								}
								char[] wcDeclaringTypeSimpleName = null;
								char[] wcDeclaringTypeQualification = null;
								if (field.getDeclaringType() != null && field.getDeclaringType().getTypeQualifiedName() != null) {
									char[] wcDeclaringType = field.getDeclaringType().getTypeQualifiedName().toCharArray();
									char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(wcDeclaringType);
									wcDeclaringTypeQualification = wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX];
									wcDeclaringTypeSimpleName = wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX];
								}
								if (wcName != null) {
									//create a pattern from the working copy field
									FieldPattern wcPattern = new FieldPattern(true, false, false, 
												wcName, wcDeclaringTypeQualification, wcDeclaringTypeSimpleName, pattern.getMatchRule());
									if (fieldPattern.matchesDecodedKey(wcPattern)) {
										requestor.acceptField(field);
									}
								}
							}			
						}
					}
				} catch(JavaScriptModelException e) {
					Logger.logException("Error while processing working copy", e);
				}
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}


	public void searchDeclarations(IJavaScriptElement enclosingElement, SearchRequestor requestor, SearchPattern pattern, IProgressMonitor monitor) throws JavaScriptModelException {
		if (VERBOSE) {
			Util.verbose("	- java element: "+enclosingElement); //$NON-NLS-1$
		}
		IJavaScriptSearchScope scope = createJavaSearchScope(new IJavaScriptElement[] {enclosingElement});
		IResource resource = enclosingElement.getResource();
		if (enclosingElement instanceof IMember) {
			IMember member = (IMember) enclosingElement;
			IJavaScriptUnit cu = member.getJavaScriptUnit();
			if (cu != null) {
				resource = cu.getResource();
			} else if (member.isBinary()) {
				// binary member resource cannot be used as this
				// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=148215
				resource = null;
			}
		}
		try {
			if (resource instanceof IFile) {
				try {
					requestor.beginReporting();
					if (VERBOSE) {
						Util.verbose("Searching for " + pattern + " in " + resource.getFullPath()); //$NON-NLS-1$//$NON-NLS-2$
					}
					SearchParticipant participant = getDefaultSearchParticipant();
					SearchDocument[] documents = MatchLocator.addWorkingCopies(
						pattern,
						new SearchDocument[] {new JavaSearchDocument(enclosingElement.getPath().toString(), participant)},
						getWorkingCopies(enclosingElement),
						participant);
					participant.locateMatches(
						documents,
						pattern,
						scope,
						requestor,
						monitor);
				} finally {
					requestor.endReporting();
				}
			} else {
				search(
					pattern,
					new SearchParticipant[] {getDefaultSearchParticipant()},
					scope,
					requestor,
					monitor);
			}
		} catch (CoreException e) {
			if (e instanceof JavaScriptModelException)
				throw (JavaScriptModelException) e;
			throw new JavaScriptModelException(e);
		}
	}

	/**
	 * Searches for all declarations of the fields accessed in the given element.
	 * The element can be a compilation unit, a source type, or a source method.
	 * Reports the field declarations using the given requestor.
	 *
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#searchDeclarationsOfAccessedFields(IJavaScriptElement, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchDeclarationsOfAccessedFields(IJavaScriptElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaScriptModelException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchDeclarationsOfAccessedFields(IJavaScriptElement, SearchRequestor, SearchPattern, IProgressMonitor)"); //$NON-NLS-1$
		}
		SearchPattern pattern = new DeclarationOfAccessedFieldsPattern(enclosingElement);
		searchDeclarations(enclosingElement, requestor, pattern, monitor);
	}

	/**
	 * Searches for all declarations of the types referenced in the given element.
	 * The element can be a compilation unit, a source type, or a source method.
	 * Reports the type declarations using the given requestor.
	 *
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#searchDeclarationsOfReferencedTypes(IJavaScriptElement, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchDeclarationsOfReferencedTypes(IJavaScriptElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaScriptModelException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchDeclarationsOfReferencedTypes(IJavaScriptElement, SearchRequestor, SearchPattern, IProgressMonitor)"); //$NON-NLS-1$
		}
		SearchPattern pattern = new DeclarationOfReferencedTypesPattern(enclosingElement);
		searchDeclarations(enclosingElement, requestor, pattern, monitor);
	}

	/**
	 * Searches for all declarations of the methods invoked in the given element.
	 * The element can be a compilation unit, a source type, or a source method.
	 * Reports the method declarations using the given requestor.
	 *
	 * @see org.eclipse.wst.jsdt.core.search.SearchEngine#searchDeclarationsOfSentMessages(IJavaScriptElement, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchDeclarationsOfSentMessages(IJavaScriptElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaScriptModelException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchDeclarationsOfSentMessages(IJavaScriptElement, SearchRequestor, SearchPattern, IProgressMonitor)"); //$NON-NLS-1$
		}
		SearchPattern pattern = new DeclarationOfReferencedMethodsPattern(enclosingElement);
		searchDeclarations(enclosingElement, requestor, pattern, monitor);
	}
	
	/**
	 * <p>Used to search all constructor declarations for ones that match the given type name using the given role,
	 * in the given scope, reporting to the given requester.</p>
	 * 
	 * @param prefix constructor prefix to search for
	 * @param typeMatchRule Search pattern matching rule to use with the given <code>typeNamePattern</code>
	 * @param scope scope of the search
	 * @param requestor requester to report findings to
	 * @param waitingPolicy Policy to use when waiting for the index
	 * @param progressMonitor monitor to report index search progress to
	 * 
	 * @see SearchPattern#R_CAMELCASE_MATCH
	 * @see SearchPattern#R_CASE_SENSITIVE
	 * @see SearchPattern#R_EQUIVALENT_MATCH
	 * @see SearchPattern#R_EXACT_MATCH
	 * @see SearchPattern#R_FULL_MATCH
	 * @see SearchPattern#R_PATTERN_MATCH
	 * @see SearchPattern#R_PREFIX_MATCH
	 * @see SearchPattern#R_REGEXP_MATCH
	 * 
	 * @see IJavaScriptSearchConstants#FORCE_IMMEDIATE_SEARCH
	 * @see IJavaScriptSearchConstants#CANCEL_IF_NOT_READY_TO_SEARCH
	 * @see IJavaScriptSearchConstants#WAIT_UNTIL_READY_TO_SEARCH
	 */
	public void searchAllConstructorDeclarations(
			final char[] prefix,
			final int typeMatchRule,
			IJavaScriptSearchScope scope,
			final IConstructorRequestor requestor,
			int waitingPolicy,
			IProgressMonitor progressMonitor) {
		
		// Debug
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchAllConstructorDeclarations(char[], char[], int, IJavaSearchScope, IRestrictedAccessConstructorRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
			Util.verbose("	- type name: "+(prefix==null?"null":new String(prefix))); //$NON-NLS-1$ //$NON-NLS-2$
			Util.verbose("	- type match rule: "+getMatchRuleString(typeMatchRule)); //$NON-NLS-1$
			Util.verbose("	- scope: "+scope); //$NON-NLS-1$
		}

		// Create pattern
		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		final ConstructorPattern pattern = new ConstructorPattern(
				prefix,
				typeMatchRule);

		// Index requester
		final HashSet workingCopiesPaths = this.getWorkingCopiesPaths();
		IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
			public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
				// Filter unexpected types
				ConstructorPattern record = (ConstructorPattern)indexRecord;
				
				//do not accept matches from files that have working copies open
				if(!workingCopiesPaths.contains(documentPath)) {
					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int nameLength = record.declaringSimpleName==null ? 0 : record.declaringSimpleName.length;
						char[] path = new char[nameLength];
						int pos = 0;
						
						if (nameLength > 0) {
							System.arraycopy(record.declaringSimpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					requestor.acceptConstructor(
							record.modifiers,
							QualificationHelpers.createFullyQualifiedName(
									record.declaringQualification, record.declaringSimpleName),
							record.parameterNames == null ? 0 : record.parameterNames.length,
							record.getFullyQualifiedParameterTypeNames(),
							record.parameterNames,
							documentPath,
							accessRestriction);
				}
				return true;
			}
		};

		//find constructor matches from index
		try {
			if (progressMonitor != null) {
				progressMonitor.beginTask(Messages.engine_searching, 1000);
			}
			// Find constructor declarations from index
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // JavaScript search only
					scope,
					searchRequestor),
				waitingPolicy,
				progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 1000));
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
		
		// find constructor matches from working copies
		IJavaScriptUnit[] workingCopies = this.getWorkingCopies();
		for (int w = 0; workingCopies != null && w < workingCopies.length; w++) {
			final IJavaScriptUnit workingCopy = workingCopies[w];
			
			//skip this working copy if not in the scope
			if(!scope.encloses(workingCopy)) {
				continue;
			}
			
			try {
				//make the working copy consistent if it is not
				if(!workingCopy.isConsistent()) {
					workingCopy.makeConsistent(progressMonitor);
				}
				
				//check each constructor in each type in the working copy for a match
				IType[] types = workingCopy.getAllTypes();
				for(int t = 0; t < types.length; ++t) {
					IType type = types[t];
					
					//get type name
					char[] wcTypeQualification = null;
					char[] wcTypeSimpleName = null;
					char[] wcTypeFullName = type.getTypeQualifiedName().toCharArray();
					if(type.getTypeQualifiedName() != null) {
						char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(wcTypeFullName);
						wcTypeQualification = wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX];
						wcTypeSimpleName = wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX];
					}
					
					if(wcTypeSimpleName != null) {
						//if working type matches the pattern then propose its constructor
						ConstructorPattern wcPattern = new ConstructorPattern(
								wcTypeQualification, wcTypeSimpleName, typeMatchRule);
						if(pattern.matchesDecodedKey(wcPattern)) {
							IFunction[] allFunctions = type.getFunctions();
							for (int f = 0; f < allFunctions.length; ++f) {
								IFunction function = allFunctions[f];
								
								//if the function is a constructor propose it
								if(function.isConstructor()) {
									//figure out parameter names and types
									String[] wcParameterNames = function.getParameterNames();
									char[][] wcParameterTypes = QualificationHelpers.stringArrayToCharArray(function.getParameterTypes());
									for(int i = 0; i < wcParameterTypes.length; ++i) {
										try {
											wcParameterTypes[i] = Signature.toCharArray(wcParameterTypes[i]);
										} catch(IllegalArgumentException e) {
											/* ignore, this will happen if a name looking like it maybe a signature gets passed in, but isn't, such as "QName"
											 * the real future fix for this should be to completely stop using signatures
											 */
										}
									}
									
									//accept the constructor
									requestor.acceptConstructor(
											function.getFlags(),
											wcTypeFullName,
											wcParameterNames == null ? 0 : wcParameterNames.length,
											wcParameterTypes,
											QualificationHelpers.stringArrayToCharArray(wcParameterNames),
											workingCopy.getPath().toString(),
											null);
								}
							}
						}
					}
				}
			} catch(JavaScriptModelException e) {
				Logger.logException("Error while processing working copy", e); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * <p>Searches for all methods in the index and working copies.</p>
	 * 
	 * @param functionRequester requester to report results to
	 * @param selectorPattern selector pattern that the results need to match
	 * @param declaringType type that all results must be defined on
	 * @param selectorPatternMatchRule the match rule used with the given <code>selectorPattern</code>
	 * @param scope of the search
	 * @param waitingPolicy policy to use when waiting for the index to index
	 * @param progressMonitor monitor to report status too
	 * 
	 * @see SearchPattern
	 * 
	 * @see IJob#ForceImmediate
	 * @see IJob#CancelIfNotReady
	 * @see IJob#WaitUntilReady
	 */
	public void searchAllFunctions(final IFunctionRequester functionRequester,
			char[] selectorPattern, char[][] declaringTypes, final int selectorPatternMatchRule,
			IJavaScriptSearchScope scope,
			int waitingPolicy, IProgressMonitor progressMonitor) {
		
		//pattern for searching the index and working copies
		final MethodPattern searchPattern = new MethodPattern(true, false,
				selectorPattern, declaringTypes, selectorPatternMatchRule);
		
		//requester used to accept index matches
		final HashSet workingCopiesPaths = this.getWorkingCopiesPaths();
		IndexQueryRequestor queryRequestor = new IndexQueryRequestor() {
			/**
			 * @see org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor#acceptIndexMatch(java.lang.String, org.eclipse.wst.jsdt.core.search.SearchPattern, org.eclipse.wst.jsdt.core.search.SearchParticipant, org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet)
			 */
			public boolean acceptIndexMatch(String documentPath,
					SearchPattern indexRecord, SearchParticipant participant,
					AccessRuleSet access) {
				
				if(!workingCopiesPaths.contains(documentPath)) {
					MethodPattern record = (MethodPattern)indexRecord;
					
					functionRequester.acceptFunction(record.selector,
							QualificationHelpers.createFullyQualifiedNames(record.parameterQualifications, record.parameterSimpleNames),
							record.parameterNames,
							record.returnQualification, record.returnSimpleName,
							record.getDeclaringQualification(), record.getDeclaringSimpleName(),
							record.modifiers,
							documentPath);
				}
				
				return true;
			}
		};
		
		// Find function matches from index
		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		try {
			if (progressMonitor != null) {
				progressMonitor.beginTask(Messages.engine_searching, 1000);
			}
			
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					searchPattern,
					getDefaultSearchParticipant(), // JavaScript search only
					scope,
					queryRequestor),
				waitingPolicy,
				progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 1000));
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
		
		// find function matches from working copies
		IJavaScriptUnit[] workingCopies = this.getWorkingCopies();
		for (int workingCopyIndex = 0; workingCopies != null && workingCopyIndex < workingCopies.length;
				workingCopyIndex++) {
			
			final IJavaScriptUnit workingCopy = workingCopies[workingCopyIndex];
			
			//skip this working copy if not in the scope
			if(!scope.encloses(workingCopy)) {
				continue;
			}
			
			try {
				//make the working copy consistent if it is not
				if(!workingCopy.isConsistent()) {
					workingCopy.makeConsistent(progressMonitor);
				}
				
				//get all functions defined at the compilation unit level
				List allFunctions = new ArrayList();
				allFunctions.addAll(Arrays.asList(workingCopy.getFunctions()));
				
				//get all functions defined on global type
				IType[] types = workingCopy.getTypes();
				if(types != null & types.length > 0) {
					IType globalType = findGlobalType(workingCopy);
					
					if(globalType != null) {
						allFunctions.addAll(Arrays.asList(globalType.getFunctions()));
					}
				}
				
				//check each field in the working copy for a match
				for (int funcIndex = 0; funcIndex < allFunctions.size(); ++funcIndex) {
					IFunction function = (IFunction)allFunctions.get(funcIndex);
					
					//selector must not be null for it to be a match
					char[] wcSelector = null;
					if(function.getElementName() != null) {
						wcSelector = function.getElementName().toCharArray();
					}
					if(wcSelector != null) {
						
						//create a pattern from the working copy method
						char[] wcDeclaringType = null;
						if(function.getDeclaringType() != null && function.getDeclaringType().getTypeQualifiedName() != null) {
							wcDeclaringType = function.getDeclaringType().getTypeQualifiedName().toCharArray();
						}
						wcDeclaringType = wcDeclaringType != null ? wcDeclaringType : IIndexConstants.GLOBAL_SYMBOL;
						MethodPattern wcPattern = new MethodPattern(true, false,
								wcSelector, declaringTypes, selectorPatternMatchRule);
						
						//if working copy function matches the search pattern then accept the function
						if(searchPattern.matchesDecodedKey(wcPattern)) {
							//figure out parameter types
							char[][] wcParameterTypes = QualificationHelpers.stringArrayToCharArray(function.getParameterTypes());
							for(int i = 0; i < wcParameterTypes.length; ++i) {
								try {
									wcParameterTypes[i] = Signature.toCharArray(wcParameterTypes[i]);
								} catch(IllegalArgumentException e) {
									/* ignore, this will happen if a name looking like it maybe a signature gets passed in, but isn't, such as "QName"
									 * the real future fix for this should be to completely stop using signatures
									 */
								}
							}
							
							//figure out the return type parts
							char[] wcReturnQualification = null;
							char[] wcReturnSimpleName = null;
							String wcReturnTypeSig = function.getReturnType();
							if(wcReturnTypeSig != null) {
								char[] wcReturnType = Signature.toString(wcReturnTypeSig).toCharArray();
								char[][] wcSeperatedReturnType = 
										QualificationHelpers.seperateFullyQualifedName(wcReturnType);
								wcReturnQualification = wcSeperatedReturnType[QualificationHelpers.QULIFIERS_INDEX];
								wcReturnSimpleName = wcSeperatedReturnType[QualificationHelpers.SIMPLE_NAMES_INDEX];
							}
							
							//get the declaring type parts
							char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(wcDeclaringType);
							
							//accept the method
							functionRequester.acceptFunction(wcSelector,
									wcParameterTypes,
									QualificationHelpers.stringArrayToCharArray(function.getParameterNames()),
									wcReturnQualification, wcReturnSimpleName,
									wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX],
									wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX],
									function.getFlags(), workingCopy.getPath().toString());
						}
					}
				}
			} catch(JavaScriptModelException e) {
				Logger.logException("Error while processing working copy", e);
			}
		}
	}
	
	
	/**
	 * <p>Searches for all variables in the index and working copies.</p>
	 * 
	 * @param variableRequester requester to report results to
	 * @param variablePattern selector pattern that the results need to match
	 * @param declaringType type that all results must be defined on
	 * @param variablePatternMatchRule the match rule used with the given <code>variablePattern</code>
	 * @param scope of the search
	 * @param waitingPolicy policy to use when waiting for the index to index
	 * @param progressMonitor monitor to report status too
	 * 
	 * @see SearchPattern
	 * 
	 * @see IJob#ForceImmediate
	 * @see IJob#CancelIfNotReady
	 * @see IJob#WaitUntilReady
	 */
	public void searchAllVariables(final IVariableRequester variableRequester,
			char[] variablePattern, char[][] declaringTypes, final int variablePatternMatchRule,
			IJavaScriptSearchScope scope,
			int waitingPolicy, IProgressMonitor progressMonitor) {
		
		//determine the declaring type pattern characters
		char[][] declaringTypePatternChars = null;
		if(declaringTypes != null && declaringTypes.length > 0) {
			declaringTypePatternChars = new char[declaringTypes.length][];
			for(int i = 0; i < declaringTypes.length; i++)
				declaringTypePatternChars[i] = declaringTypes[i];
		}
		
		//pattern for searching the index and working copies
		final FieldPattern searchPattern = new FieldPattern(true, false, false, true,
				variablePattern, declaringTypePatternChars, null, null, variablePatternMatchRule, null);
		
		//requester used to accept index matches
		final HashSet workingCopiesPaths = this.getWorkingCopiesPaths();
		IndexQueryRequestor queryRequestor = new IndexQueryRequestor() {
			/**
			 * @see org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor#acceptIndexMatch(java.lang.String, org.eclipse.wst.jsdt.core.search.SearchPattern, org.eclipse.wst.jsdt.core.search.SearchParticipant, org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet)
			 */
			public boolean acceptIndexMatch(String documentPath,
					SearchPattern indexRecord, SearchParticipant participant,
					AccessRuleSet access) {
				
				if(!workingCopiesPaths.contains(documentPath)) {
					FieldPattern record = (FieldPattern)indexRecord;
					variableRequester.acceptVariable(record.name,
							record.typeQualification, record.typeSimpleName, record.getDeclaringQualification(),
							record.getDeclaringSimpleName(),
							record.modifiers,
							documentPath);
				}
				
				return true;
			}
		};
		
		// Find function matches from index
		IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
		try {
			if (progressMonitor != null) {
				progressMonitor.beginTask(Messages.engine_searching, 1000);
			}
			
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					searchPattern,
					getDefaultSearchParticipant(), // JavaScript search only
					scope,
					queryRequestor),
				waitingPolicy,
				progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 1000));
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
		
		// find function matches from working copies
		IJavaScriptUnit[] workingCopies = this.getWorkingCopies();
		for (int workingCopyIndex = 0; workingCopies != null && workingCopyIndex < workingCopies.length;
				workingCopyIndex++) {
			
			final IJavaScriptUnit workingCopy = workingCopies[workingCopyIndex];
			
			//skip this working copy if not in the scope
			if(!scope.encloses(workingCopy)) {
				continue;
			}
			
			try {
				//make the working copy consistent if it is not
				if(!workingCopy.isConsistent()) {
					workingCopy.makeConsistent(progressMonitor);
				}
				
				//get all fields defined at the compilation unit level
				List allFields = new ArrayList();
				allFields.addAll(Arrays.asList(workingCopy.getFields()));
				
				//get all fields defined on global type defined in this file
				IType[] types = workingCopy.getTypes();
				if(types != null & types.length > 0) {
					IType globalType = findGlobalType(workingCopy);
					
					if(globalType != null) {
						allFields.addAll(Arrays.asList(globalType.getFields()));
					}
				}
				
				//check each field in the working copy for a match
				for (int fieldIndex = 0; fieldIndex < allFields.size(); ++fieldIndex) {
					IField field = (IField)allFields.get(fieldIndex);
					
					//selector must not be null for it to be a match
					char[] wcName = null;
					if(field.getElementName() != null) {
						wcName = field.getElementName().toCharArray();
					}
					if(wcName != null) {
						
						//create a pattern from the working copy method
						char[] wcDeclaringType = null;
						if(field.getDeclaringType() != null && field.getDeclaringType().getTypeQualifiedName() != null) {
							wcDeclaringType = field.getDeclaringType().getTypeQualifiedName().toCharArray();
						}
						
						wcDeclaringType = wcDeclaringType != null ? wcDeclaringType : IIndexConstants.GLOBAL_SYMBOL;
						FieldPattern wcPattern = new FieldPattern(true, false, false, true,
								wcName, declaringTypePatternChars, null, null, variablePatternMatchRule, null);
						
						//if working copy function matches the search pattern then accept the function
						if(searchPattern.matchesDecodedKey(wcPattern)) {
							
							//figure out the return type parts
							char[] wcTypeQualification = null;
							char[] wcTypeSimpleName = null;
							String wcTypeSig = field.getTypeSignature();
							if(wcTypeSig != null) {
								char[] wcType = Signature.toString(wcTypeSig).toCharArray();
								char[][] wcSeperatedType = 
										QualificationHelpers.seperateFullyQualifedName(wcType);
								wcTypeQualification = wcSeperatedType[QualificationHelpers.QULIFIERS_INDEX];
								wcTypeSimpleName = wcSeperatedType[QualificationHelpers.SIMPLE_NAMES_INDEX];
							}
							
							//get the declaring type parts
							char[][] wcSeperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(wcDeclaringType);
							
							//accept the field
							variableRequester.acceptVariable(wcName,
									wcTypeQualification, wcTypeSimpleName,
									wcSeperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX],
									wcSeperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX],
									field.getFlags(), workingCopy.getPath().toString());
						}
					}
				}
			} catch(JavaScriptModelException e) {
				Logger.logException("Error while processing working copy", e);
			}
		}
	}
	
	/**
	 * <p>
	 * Finds the global type on the given unit if there is one.
	 * </p>
	 * 
	 * @param unit
	 *            {@link IJavaScriptUnit} to find the global type on
	 * 
	 * @return global type in the given {@link IJavaScriptUnit}, or
	 *         <code>null</code> if there is none
	 * 
	 * @throws JavaScriptModelException
	 *             getting the types on the unit can throw this
	 */
	private static IType findGlobalType(IJavaScriptUnit unit) throws JavaScriptModelException {
		IType globalType = null;
		
		IType[] types = unit.getTypes();
		if(types != null & types.length > 0) {
			for(int i = 0; i < types.length && globalType == null; ++i) {
				if(types[i].getElementName().equals(GLOBAL_TYPE_SYMBOL)) {
					globalType = types[i];
				}
			}
		}
		
		return globalType;
	}
}
