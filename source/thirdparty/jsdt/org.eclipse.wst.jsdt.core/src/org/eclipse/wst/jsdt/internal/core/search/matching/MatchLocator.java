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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.search.FieldDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.FieldReferenceMatch;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.LocalVariableDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.LocalVariableReferenceMatch;
import org.eclipse.wst.jsdt.core.search.MethodDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.MethodReferenceMatch;
import org.eclipse.wst.jsdt.core.search.PackageReferenceMatch;
import org.eclipse.wst.jsdt.core.search.SearchDocument;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.core.search.TypeDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.TypeReferenceMatch;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.INameEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor2;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfIntValues;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.Messages;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.BinaryMember;
import org.eclipse.wst.jsdt.internal.core.BinaryType;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.CompilationUnit;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.LibraryFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.LocalVariable;
import org.eclipse.wst.jsdt.internal.core.MetadataFile;
import org.eclipse.wst.jsdt.internal.core.NameLookup;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.PackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;
import org.eclipse.wst.jsdt.internal.core.SourceMapper;
import org.eclipse.wst.jsdt.internal.core.SourceMethod;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.hierarchy.HierarchyResolver;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.search.BasicSearchEngine;
import org.eclipse.wst.jsdt.internal.core.search.HierarchyScope;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.IndexSelector;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchDocument;
import org.eclipse.wst.jsdt.internal.core.util.HandleFactory;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;

public class MatchLocator implements ITypeRequestor, ITypeRequestor2 {

public static final int MAX_AT_ONCE;
static {
	long maxMemory = Runtime.getRuntime().maxMemory();
	int ratio = (int) Math.round(((double) maxMemory) / (64 * 0x100000));
	switch (ratio) {
		case 0:
		case 1:
			MAX_AT_ONCE = 100;
			break;
		case 2:
			MAX_AT_ONCE = 200;
			break;
		case 3:
			MAX_AT_ONCE = 300;
			break;
		default:
			MAX_AT_ONCE = 400;
			break;
	}
}

// permanent state
public SearchPattern pattern;
public PatternLocator patternLocator;
public int matchContainer;
public SearchRequestor requestor;
public IJavaScriptSearchScope scope;
public IProgressMonitor progressMonitor;

public org.eclipse.wst.jsdt.core.IJavaScriptUnit[] workingCopies;
public HandleFactory handleFactory;

// cache of all super type names if scope is hierarchy scope
public char[][][] allSuperTypeNames;

// the following is valid for the current project
public MatchLocatorParser parser;
private Parser basicParser;
public INameEnvironment nameEnvironment;
public NameLookup nameLookup;
public LookupEnvironment lookupEnvironment;
public HierarchyResolver hierarchyResolver;

public CompilerOptions options;

// management of PossibleMatch to be processed
public int numberOfMatches; // (numberOfMatches - 1) is the last unit in matchesToProcess
public PossibleMatch[] matchesToProcess;
public PossibleMatch currentPossibleMatch;

/*
 * Time spent in the IJavaScriptSearchResultCollector
 */
public long resultCollectorTime = 0;

// Progress information
int progressStep;
int progressWorked;

// Binding resolution and cache
CompilationUnitScope unitScope;
SimpleLookupTable bindings;

// Cache for method handles
HashSet methodHandles;

private HashtableOfObject parsedUnits;


class ReportMatchingVisitor extends ASTVisitor
{
	MatchingNodeSet nodeSet;
	boolean matchedClassContainer;
	IJavaScriptElement enclosingElement;
	boolean typeInHierarchy;
	CoreException exception;

	public ReportMatchingVisitor(MatchingNodeSet nodeSet, boolean matchedClassContainer, IJavaScriptElement enclosingElement, boolean typeInHierarchy) {
		super();
		this.nodeSet = nodeSet;
		this.matchedClassContainer = matchedClassContainer;
		this.enclosingElement = enclosingElement;
		this.typeInHierarchy = typeInHierarchy;
	}

	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		Integer level = (Integer) nodeSet.matchingNodes.removeKey(localDeclaration);
		int accuracy = (level != null && matchedClassContainer) ? level.intValue() : -1;
		try {
			reportMatching(localDeclaration, null, null, enclosingElement, accuracy, typeInHierarchy, nodeSet);
			
			//check for a method declaration nested under the local declaration to report
			AbstractMethodDeclaration methodDeclaration = AbstractMethodDeclaration.findMethodDeclaration(localDeclaration);
			if(methodDeclaration != null && methodDeclaration instanceof MethodDeclaration) {
				visit((MethodDeclaration)methodDeclaration, scope);
			}
		} catch (CoreException e) {
			exception=e;
		}

		return false;
	}

	public boolean visit(MethodDeclaration methodDeclaration, Scope parentScope) {
		Integer level = (Integer) nodeSet.matchingNodes.removeKey(methodDeclaration);
		int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
		try {
			reportMatching(null, methodDeclaration, enclosingElement, value, typeInHierarchy, nodeSet);
		} catch (CoreException e) {
			exception=e;
		}
		return false;
	}

}


/**
 * An ast visitor that visits local type declarations.
 */
public class LocalDeclarationVisitor extends ASTVisitor {
	IJavaScriptElement enclosingElement;
	Binding enclosingElementBinding;
	MatchingNodeSet nodeSet;
	HashtableOfIntValues occurrencesCounts = new HashtableOfIntValues(); // key = class name (char[]), value = occurrenceCount (int)
	public LocalDeclarationVisitor(IJavaScriptElement enclosingElement, Binding enclosingElementBinding, MatchingNodeSet nodeSet) {
		this.enclosingElement = enclosingElement;
		this.enclosingElementBinding = enclosingElementBinding;
		this.nodeSet = nodeSet;
	}
	public boolean visit(TypeDeclaration typeDeclaration, BlockScope unused) {
		try {
			char[] simpleName;
			if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
				simpleName = CharOperation.NO_CHAR;
			} else {
				simpleName = typeDeclaration.name;
			}
			int occurrenceCount = occurrencesCounts.get(simpleName);
			if (occurrenceCount == HashtableOfIntValues.NO_VALUE) {
				occurrenceCount = 1;
			} else {
				occurrenceCount = occurrenceCount + 1;
			}
			occurrencesCounts.put(simpleName, occurrenceCount);
			if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
				reportMatching(typeDeclaration, this.enclosingElement, -1, nodeSet, occurrenceCount);
			} else {
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(typeDeclaration);
				reportMatching(typeDeclaration, this.enclosingElement, level != null ? level.intValue() : -1, nodeSet, occurrenceCount);
			}
			return false; // don't visit members as this was done during reportMatching(...)
		} catch (CoreException e) {
			throw new WrappedCoreException(e);
		}
	}
}

public static class WorkingCopyDocument extends JavaSearchDocument {
	public org.eclipse.wst.jsdt.core.IJavaScriptUnit workingCopy;
	WorkingCopyDocument(org.eclipse.wst.jsdt.core.IJavaScriptUnit workingCopy, SearchParticipant participant) {
		super(workingCopy.getPath().toString(), participant);
		this.charContents = ((CompilationUnit)workingCopy).getContents();
		this.workingCopy = workingCopy;
	}
	public String toString() {
		return "WorkingCopyDocument for " + getPath(); //$NON-NLS-1$
	}
}

public static class WrappedCoreException extends RuntimeException {
	private static final long serialVersionUID = 8354329870126121212L; // backward compatible
	public CoreException coreException;
	public WrappedCoreException(CoreException coreException) {
		this.coreException = coreException;
	}
}

public static SearchDocument[] addWorkingCopies(InternalSearchPattern pattern, SearchDocument[] indexMatches, org.eclipse.wst.jsdt.core.IJavaScriptUnit[] copies, SearchParticipant participant) {
	// working copies take precedence over corresponding compilation units
	HashMap workingCopyDocuments = workingCopiesThatCanSeeFocus(copies, pattern.focus, pattern.isPolymorphicSearch(), participant);
	SearchDocument[] matches = null;
	int length = indexMatches.length;
	for (int i = 0; i < length; i++) {
		SearchDocument searchDocument = indexMatches[i];
		if (searchDocument.getParticipant() == participant) {
			SearchDocument workingCopyDocument = (SearchDocument) workingCopyDocuments.remove(searchDocument.getPath());
			if (workingCopyDocument != null) {
				if (matches == null) {
					System.arraycopy(indexMatches, 0, matches = new SearchDocument[length], 0, length);
				}
				matches[i] = workingCopyDocument;
			}
		}
	}
	if (matches == null) { // no working copy
		matches = indexMatches;
	}
	int remainingWorkingCopiesSize = workingCopyDocuments.size();
	if (remainingWorkingCopiesSize != 0) {
		System.arraycopy(matches, 0, matches = new SearchDocument[length+remainingWorkingCopiesSize], 0, length);
		Iterator iterator = workingCopyDocuments.values().iterator();
		int index = length;
		while (iterator.hasNext()) {
			matches[index++] = (SearchDocument) iterator.next();
		}
	}
	return matches;
}

public static void setFocus(InternalSearchPattern pattern, IJavaScriptElement focus) {
	pattern.focus = focus;
}

/*
 * Returns the working copies that can see the given focus.
 */
private static HashMap workingCopiesThatCanSeeFocus(org.eclipse.wst.jsdt.core.IJavaScriptUnit[] copies, IJavaScriptElement focus, boolean isPolymorphicSearch, SearchParticipant participant) {
	if (copies == null) return new HashMap();
	if (focus != null) {
		while (!(focus instanceof IJavaScriptProject)) {
			focus = focus.getParent();
		}
	}
	HashMap result = new HashMap();
	for (int i=0, length = copies.length; i<length; i++) {
		org.eclipse.wst.jsdt.core.IJavaScriptUnit workingCopy = copies[i];
		IPath projectOrJar = MatchLocator.getProjectOrJar(workingCopy).getPath();
		if (focus == null || IndexSelector.canSeeFocus(focus, isPolymorphicSearch, projectOrJar)) {
			result.put(
				workingCopy.getPath().toString(),
				new WorkingCopyDocument(workingCopy, participant)
			);
		}
	}
	return result;
}


public static SearchPattern createAndPattern(final SearchPattern leftPattern, final SearchPattern rightPattern) {
	return new AndPattern(0/*no kind*/, 0/*no rule*/) {
		SearchPattern current = leftPattern;
		public SearchPattern currentPattern() {
			return this.current;
		}
		protected boolean hasNextQuery() {
			if (this.current == leftPattern) {
				this.current = rightPattern;
				return true;
			}
			return false;
		}
		protected void resetQuery() {
			this.current = leftPattern;
		}
	};
}

/**
 * Query a given index for matching entries. Assumes the sender has opened the index and will close when finished.
 */
public static void findIndexMatches(InternalSearchPattern pattern, Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaScriptSearchScope scope, IProgressMonitor monitor) throws IOException {
	pattern.findIndexMatches(index, requestor, participant, scope, monitor);
}

public static IJavaScriptElement getProjectOrJar(IJavaScriptElement element) {
	while (!(element instanceof IJavaScriptProject) &&
			!( element instanceof LibraryFragmentRoot) &&
			!( element instanceof PackageFragmentRoot)) {
		element = element.getParent();
	}
	return element;
}

public static IJavaScriptElement projectOrJarFocus(InternalSearchPattern pattern) {
	return pattern == null || pattern.focus == null ? null : getProjectOrJar(pattern.focus);
}

public MatchLocator(
	SearchPattern pattern,
	SearchRequestor requestor,
	IJavaScriptSearchScope scope,
	IProgressMonitor progressMonitor) {

	this.pattern = pattern;
	this.patternLocator = PatternLocator.patternLocator(this.pattern);
	this.matchContainer = this.patternLocator.matchContainer();
	this.requestor = requestor;
	this.scope = scope;
	this.progressMonitor = progressMonitor;
}
/**
 * Add an additional binary type
 */
public void accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
	/* commented out below is the original code with a compilation error */
	System.out.println("Bad call to method-- IMPLEMENT MatchLocator. accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) "); //$NON-NLS-1$
	//this.lookupEnvironment.createBinaryTypeFrom(binaryType., packageBinding, accessRestriction);
}
/**
 * Add an additional compilation unit into the loop
 *  ->  build compilation unit declarations, their bindings and record their results.
 */
public void accept(ICompilationUnit sourceUnit, char[][] typeNames, AccessRestriction accessRestriction) {
	// Switch the current policy and compilation result for this unit to the requested one.
	CompilationResult unitResult = new CompilationResult(sourceUnit, 1, 1, this.options.maxProblemsPerUnit);
	try {
		if (parsedUnits == null)
			parsedUnits = new HashtableOfObject();
		CompilationUnitDeclaration parsedUnit = (CompilationUnitDeclaration) parsedUnits.get(sourceUnit.getFileName());
		if(parsedUnit == null) {
			Parser parser = basicParser();
			parsedUnit = parser.dietParse(sourceUnit, unitResult);
			parser.inferTypes(parsedUnit, this.options);
			parsedUnits.put(sourceUnit.getFileName(), parsedUnit);
		}
		this.lookupEnvironment.buildTypeBindings(parsedUnit, typeNames, accessRestriction);
		this.lookupEnvironment.completeTypeBindings(parsedUnit, typeNames, true);
	} catch (AbortCompilationUnit e) {
		// at this point, currentCompilationUnitResult may not be sourceUnit, but some other
		// one requested further along to resolve sourceUnit.
		if (unitResult.compilationUnit == sourceUnit) { // only report once
			//requestor.acceptResult(unitResult.tagAsAccepted());
		} else {
			throw e; // want to abort enclosing request to compile
		}
	}
	// Display unit error in debug mode
	if (BasicSearchEngine.VERBOSE) {
		if (unitResult.problemCount > 0) {
			System.out.println(unitResult);
		}
	}
}
public void accept(ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
	accept(sourceUnit, CharOperation.NO_CHAR_CHAR, accessRestriction);
}


public void accept(LibraryAPIs libraryMetaData)
{
	lookupEnvironment.buildTypeBindings(libraryMetaData);

}
/**
 * Add additional source types
 */
public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
	// case of SearchableEnvironment of an IJavaScriptProject is used
	ISourceType sourceType = sourceTypes[0];
	while (sourceType.getEnclosingType() != null)
		sourceType = sourceType.getEnclosingType();
	if (sourceType instanceof SourceTypeElementInfo) {
		// get source
		SourceTypeElementInfo elementInfo = (SourceTypeElementInfo) sourceType;
		IType type = elementInfo.getHandle();
		ICompilationUnit sourceUnit = (ICompilationUnit) type.getJavaScriptUnit();
		accept(sourceUnit, accessRestriction);
	} else {
		CompilationResult result = new CompilationResult(sourceType.getFileName(), sourceType.getPackageName(), 1, 1, 0);
		CompilationUnitDeclaration unit =
			SourceTypeConverter.buildCompilationUnit(
				sourceTypes,
				SourceTypeConverter.FIELD_AND_METHOD // need field and methods
				| SourceTypeConverter.MEMBER_TYPE, // need member types
				// no need for field initialization
				this.lookupEnvironment.problemReporter,
				result);
		this.lookupEnvironment.buildTypeBindings(unit, accessRestriction);
		this.lookupEnvironment.completeTypeBindings(unit, true);
	}
}
protected Parser basicParser() {
	if (this.basicParser == null) {
		ProblemReporter problemReporter =
			new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				this.options,
				new DefaultProblemFactory());
		this.basicParser = new Parser(problemReporter, false);
		this.basicParser.reportOnlyOneSyntaxError = true;
	}
	return this.basicParser;
}
/*
 * Caches the given binary type in the lookup environment and returns it.
 * Returns the existing one if already cached.
 * Returns null if source type binding was cached.
 */
protected BinaryTypeBinding cacheBinaryType(IType type, IBinaryType binaryType) throws JavaScriptModelException {
	IType enclosingType = type.getDeclaringType();
	if (enclosingType != null)
		cacheBinaryType(enclosingType, null); // cache enclosing types first, so that binary type can be found in lookup enviroment
	if (binaryType == null) {
		ClassFile classFile = (ClassFile) type.getClassFile();
		try {
			binaryType = getBinaryInfo(classFile, classFile.getResource());
		} catch (CoreException e) {
			if (e instanceof JavaScriptModelException) {
				throw (JavaScriptModelException) e;
			} else {
				throw new JavaScriptModelException(e);
			}
		}
	}

	BinaryTypeBinding binding = null; //this.lookupEnvironment.cacheBinaryType(type, (AccessRestriction)null /*no access restriction*/);
	if (binding == null) { // it was already cached as a result of a previous query
		char[][] compoundName = CharOperation.splitOn('.', type.getFullyQualifiedName().toCharArray());
		ReferenceBinding referenceBinding = this.lookupEnvironment.getCachedType(compoundName);
		if (referenceBinding != null && (referenceBinding instanceof BinaryTypeBinding))
			binding = (BinaryTypeBinding) referenceBinding; // if the binding could be found and if it comes from a binary type
	}
	return binding;
}
/*
 * Computes the super type names of the focus type if any.
 */
protected char[][][] computeSuperTypeNames(IType focusType) {
	String fullyQualifiedName = focusType.getFullyQualifiedName();
	int lastDot = fullyQualifiedName.lastIndexOf('.');
	char[] qualification = lastDot == -1 ? CharOperation.NO_CHAR : fullyQualifiedName.substring(0, lastDot).toCharArray();
	char[] simpleName = focusType.getElementName().toCharArray();

	SuperTypeNamesCollector superTypeNamesCollector =
		new SuperTypeNamesCollector(
			this.pattern,
			simpleName,
			qualification,
			new MatchLocator(this.pattern, this.requestor, this.scope, this.progressMonitor), // clone MatchLocator so that it has no side effect
			focusType,
			this.progressMonitor);
	try {
		this.allSuperTypeNames = superTypeNamesCollector.collect();
	} catch (JavaScriptModelException e) {
		// problem collecting super type names: leave it null
	}
	return this.allSuperTypeNames;
}
/**
 * Creates an IFunction from the given method declaration and type.
 */
protected IJavaScriptElement createHandle(AbstractMethodDeclaration method, IJavaScriptElement parent) {
	if (!(parent instanceof IType ||
			parent instanceof org.eclipse.wst.jsdt.core.IJavaScriptUnit ||
			parent instanceof org.eclipse.wst.jsdt.core.IClassFile
			)) return parent;

//	IType type = (IType) parent;
	Argument[] arguments = method.arguments;
	int argCount = arguments == null ? 0 : arguments.length;
//	if (type.isBinary()) {
//		// don't cache the methods of the binary type
//		// fall thru if its a constructor with a synthetic argument... find it the slower way
//		ClassFileReader reader = classFileReader(type);
//		if (reader != null) {
//			IBinaryMethod[] methods = reader.getMethods();
//			if (methods != null) {
//				// build arguments names
//				boolean firstIsSynthetic = false;
//				if (reader.isMember() && method.isConstructor() && !Flags.isStatic(reader.getModifiers())) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48261
//					firstIsSynthetic = true;
//					argCount++;
//				}
//				char[][] argumentTypeNames = new char[argCount][];
//				for (int i = 0; i < argCount; i++) {
//					char[] typeName = null;
//					if (i == 0 && firstIsSynthetic) {
//						typeName = type.getDeclaringType().getFullyQualifiedName().toCharArray();
//					} else if (arguments != null) {
//						TypeReference typeRef = arguments[firstIsSynthetic ? i - 1 : i].type;
//						typeName = CharOperation.concatWith(typeRef.getTypeName(), '.');
//						for (int k = 0, dim = typeRef.dimensions(); k < dim; k++)
//							typeName = CharOperation.concat(typeName, new char[] {'[', ']'});
//					}
//					if (typeName == null) {
//						// invalid type name
//						return null;
//					}
//					argumentTypeNames[i] = typeName;
//				}
//
//				// return binary method
//				return createBinaryMethodHandle(type, method.selector, argumentTypeNames);
//			}
//		}
//		return null;
//	}

	String[] parameterTypeSignatures = new String[argCount];
	if (arguments != null) {
		for (int i = 0; i < argCount; i++) {
			TypeReference typeRef = arguments[i].type;
			char[] typeName = typeRef!=null ? CharOperation.concatWith(typeRef.getTypeName(), '.')
					: null;
			parameterTypeSignatures[i] = Signature.createTypeSignature(typeName, false);
		}
	}

	String methodName = null;
	if (method.getName() != null) {
		methodName = new String( method.getName());
	}
	else {
		methodName = "___anonymous"; //$NON-NLS-1$
	}
	return createMethodHandle(parent, methodName, parameterTypeSignatures);
}
/*
 * Create method handle.
 * Store occurences for create handle to retrieve possible duplicate ones.
 */
private IJavaScriptElement createMethodHandle(IJavaScriptElement parent, String methodName, String[] parameterTypeSignatures) {
	IFunction methodHandle = null;
	if (parent instanceof org.eclipse.wst.jsdt.core.IJavaScriptUnit) {
		org.eclipse.wst.jsdt.core.IJavaScriptUnit compUnit = (org.eclipse.wst.jsdt.core.IJavaScriptUnit ) parent;
		 methodHandle = compUnit.getFunction(methodName, parameterTypeSignatures);

	}
	else if (parent instanceof ICompilationUnit) {
		org.eclipse.wst.jsdt.core.IClassFile classFile = (org.eclipse.wst.jsdt.core.IClassFile ) parent;
		 methodHandle = classFile.getFunction(methodName, parameterTypeSignatures);

	}
	else if (parent instanceof IType) {
		IType type = (IType) parent;
		 methodHandle = type.getFunction(methodName, parameterTypeSignatures);

	}
	if (methodHandle instanceof SourceMethod) {
		while (this.methodHandles.contains(methodHandle)) {
			((SourceMethod) methodHandle).occurrenceCount++;
		}
	}
	this.methodHandles.add(methodHandle);
	return methodHandle;
}
protected IJavaScriptElement createHandle(InferredAttribute fieldDeclaration, InferredType typeDeclaration, IJavaScriptElement parent) {
	if (parent instanceof ITypeRoot) {
		ITypeRoot typeRoot = (ITypeRoot) parent;
		parent=typeRoot.getType(new String(typeDeclaration.getName()));
	}
	if (!(parent instanceof IType)) return parent;

	return ((IType) parent).getField(new String(fieldDeclaration.name));
}


/**
 * Creates an IField from the given field declaration and type.
 */
protected IJavaScriptElement createHandle(FieldDeclaration fieldDeclaration, TypeDeclaration typeDeclaration, IJavaScriptElement parent) {
	if (!(parent instanceof IType)) return parent;
	IType type = (IType) parent;

	switch (fieldDeclaration.getKind()) {
		case AbstractVariableDeclaration.FIELD :
			return ((IType) parent).getField(new String(fieldDeclaration.name));
	}
	if (type.isBinary()) {
		// do not return initializer for binary types
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=98378
		return type;
	}
	// find occurence count of the given initializer in its type declaration
	int occurrenceCount = 0;
	FieldDeclaration[] fields = typeDeclaration.fields;
	for (int i = 0, length = fields.length; i < length; i++) {
		if (fields[i].getKind() == AbstractVariableDeclaration.INITIALIZER) {
			occurrenceCount++;
			if (fields[i].equals(fieldDeclaration)) break;
		}
	}
	return ((IType) parent).getInitializer(occurrenceCount);
}
/**
 * Create an handle for a local variable declartion (may be a local variable or type parameter).
 */
protected IJavaScriptElement createHandle(AbstractVariableDeclaration variableDeclaration, IJavaScriptElement parent) {
	switch (variableDeclaration.getKind()) {
		case AbstractVariableDeclaration.LOCAL_VARIABLE:
			String signature = (variableDeclaration.type!=null) ?
					new String(variableDeclaration.type.resolvedType.signature()): Signature.SIG_ANY;
			return new LocalVariable((JavaElement)parent,
				new String(variableDeclaration.name),
				variableDeclaration.declarationSourceStart,
				variableDeclaration.declarationSourceEnd,
				variableDeclaration.sourceStart,
				variableDeclaration.sourceEnd,
				signature
			);
		case AbstractVariableDeclaration.PARAMETER:
			return new LocalVariable((JavaElement)parent,
				new String(variableDeclaration.name),
				variableDeclaration.declarationSourceStart,
				variableDeclaration.declarationSourceEnd,
				variableDeclaration.sourceStart,
				variableDeclaration.sourceEnd,
				new String(variableDeclaration.type.resolvedType.signature())
			);
	}
	return null;
}
/*
 * Creates hierarchy resolver if needed.
 * Returns whether focus is visible.
 */
protected boolean createHierarchyResolver(IType focusType, PossibleMatch[] possibleMatches) {
	// cache focus type if not a possible match
	char[][] compoundName = CharOperation.splitOn('.', focusType.getFullyQualifiedName().toCharArray());
	boolean isPossibleMatch = false;
	for (int i = 0, length = possibleMatches.length; i < length; i++) {
		if (CharOperation.equals(possibleMatches[i].compoundName, compoundName)) {
			isPossibleMatch = true;
			break;
		}
	}
	if (!isPossibleMatch) {
		if (focusType.isBinary()) {
			try {
				cacheBinaryType(focusType, null);
			} catch (JavaScriptModelException e) {
				return false;
			}
		} else {
			// cache all types in the focus' compilation unit (even secondary types)
			accept((ICompilationUnit) focusType.getJavaScriptUnit(), null /*TODO no access restriction*/);
		}
	}

	// resolve focus type
	this.hierarchyResolver = new HierarchyResolver(this.lookupEnvironment, null/*hierarchy is not going to be computed*/);
	ReferenceBinding binding = this.hierarchyResolver.setFocusType(compoundName);
	return binding != null && binding.isValidBinding() && (binding.tagBits & TagBits.HierarchyHasProblems) == 0;
}
/**
 * Creates an IImportDeclaration from the given import statement
 */
protected IJavaScriptElement createImportHandle(ImportReference importRef) {
	char[] importName = CharOperation.concatWith(importRef.getImportName(), '.');
	if ((importRef.bits & ASTNode.OnDemand) != 0)
		importName = CharOperation.concat(importName, ".*" .toCharArray()); //$NON-NLS-1$
	Openable openable = this.currentPossibleMatch.openable;
	if (openable instanceof CompilationUnit)
		return ((CompilationUnit) openable).getImport(new String(importName));

	// binary types do not contain import statements so just answer the top-level type as the element
	IType binaryType = ((ClassFile) openable).getType();
	return binaryType;
}
/**
 * Creates an IType from the given simple top level type name.
 */
protected IType createTypeHandle(String simpleTypeName) {
	Openable openable = this.currentPossibleMatch.openable;
	if (openable instanceof CompilationUnit)
		return ((CompilationUnit) openable).getType(simpleTypeName);

	IType binaryType = ((ClassFile) openable).getType(simpleTypeName);
//	String binaryTypeQualifiedName = binaryType.getTypeQualifiedName();
//	if (simpleTypeName.equals(binaryTypeQualifiedName))
		return binaryType; // answer only top-level types, sometimes the classFile is for a member/local type

//		// type name may be null for anonymous (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=164791)
//		String classFileName = simpleTypeName.length() == 0 ? binaryTypeQualifiedName : simpleTypeName;
//		IClassFile classFile = binaryType.getPackageFragment().getClassFile(classFileName + SuffixConstants.SUFFIX_STRING_class);
//		return classFile.getType();
}
protected boolean encloses(IJavaScriptElement element) {
	return element != null && this.scope.encloses(element);
}
/* (non-Javadoc)
 * Return info about last type argument of a parameterized type reference.
 * These info are made of concatenation of 2 int values which are respectively
 *  depth and end position of the last type argument.
 * For example, this method will return 0x300000020 for type ref List<List<List<String>>>
 * if end position of type reference "String" equals 32.
 */
private long findLastTypeArgumentInfo(TypeReference typeRef) {
	// Get last list of type arguments for parameterized qualified type reference
	TypeReference lastTypeArgument = typeRef;
	int depth = 0;
	while (true) {
		TypeReference[] lastTypeArguments = null;
		// Get last type argument for single type reference of last list of argument of parameterized qualified type reference
		TypeReference last = null;
		if (lastTypeArguments != null) {
			for (int i=lastTypeArguments.length-1; i>=0 && last==null; i++) {
				last = lastTypeArguments[i];
			}
		}
		if (last == null) break;
		depth++;
		lastTypeArgument = last;
	}
	// Current type reference is not parameterized. So, it is the last type argument
	return (((long) depth) << 32) + lastTypeArgument.sourceEnd;
}
protected IBinaryType getBinaryInfo(ClassFile classFile, IResource resource) throws CoreException {
	BinaryType binaryType = (BinaryType) classFile.getType();
	if (classFile.isOpen())
		return (IBinaryType) binaryType.getElementInfo(); // reuse the info from the java model cache

	// create a temporary info
//	IBinaryType info;
//	try {
//		PackageFragment pkg = (PackageFragment) classFile.getParent();
//		PackageFragmentRoot root = (PackageFragmentRoot) pkg.getParent();
//		if (root.isArchive()) {
//			// class file in a jar
//			String classFileName = classFile.getElementName();
//			String classFilePath = Util.concatWith(pkg.names, classFileName, '/');
//			ZipFile zipFile = null;
//			try {
//				zipFile = ((JarPackageFragmentRoot) root).getJar();
//				info = ClassFileReader.read(zipFile, classFilePath);
//			} finally {
//				JavaModelManager.getJavaModelManager().closeZipFile(zipFile);
//			}
//		} else {
//			// class file in a directory
//			info = Util.newClassFileReader(resource);
//		}
//		if (info == null) throw binaryType.newNotPresentException();
//		return info;
//	} catch (ClassFormatException e) {
//		//e.printStackTrace();
//		return null;
//	} catch (java.io.IOException e) {
//		throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.IO_EXCEPTION);
//	}
	return null;
}
protected IType getFocusType() {
	return this.scope instanceof HierarchyScope ? ((HierarchyScope) this.scope).focusType : null;
}
protected void getMethodBodies(CompilationUnitDeclaration unit, MatchingNodeSet nodeSet) {
	if (unit.ignoreMethodBodies) {
		unit.ignoreFurtherInvestigation = true;
		return; // if initial diet parse did not work, no need to dig into method bodies.
	}

	// save existing values to restore them at the end of the parsing process
	// see bug 47079 for more details
	int[] oldLineEnds = this.parser.scanner.lineEnds;
	int oldLinePtr = this.parser.scanner.linePtr;

	try {
		CompilationResult compilationResult = unit.compilationResult;
		this.parser.scanner.setSource(compilationResult);

		if (this.parser.javadocParser.checkDocComment) {
			char[] contents = compilationResult.compilationUnit.getContents();
			this.parser.javadocParser.scanner.setSource(contents);
		}
		this.parser.nodeSet = nodeSet;
		this.parser.parseBodies(unit);
	} finally {
		this.parser.nodeSet = null;
		// this is done to prevent any side effects on the compilation unit result
		// line separator positions array.
		this.parser.scanner.lineEnds = oldLineEnds;
		this.parser.scanner.linePtr = oldLinePtr;
	}
}
protected TypeBinding getType(Object typeKey, char[] typeName) {
	if (this.unitScope == null || typeName == null || typeName.length == 0) return null;
	// Try to get binding from cache
	Binding binding = (Binding) this.bindings.get(typeKey);
	if (binding != null) {
		if (binding instanceof TypeBinding && binding.isValidBinding())
			return (TypeBinding) binding;
		return null;
	}
	// Get binding from unit scope
	char[][] compoundName = CharOperation.splitOn('.', typeName);
	TypeBinding typeBinding = this.unitScope.getType(compoundName, compoundName.length);
	this.bindings.put(typeKey, typeBinding);
	return typeBinding.isValidBinding() ? typeBinding : null;
}
public MethodBinding getMethodBinding(MethodPattern methodPattern) {
	if (this.unitScope == null) return null;
	// Try to get binding from cache
	Binding binding = (Binding) this.bindings.get(methodPattern);
	if (binding != null) {
		if (binding instanceof MethodBinding && binding.isValidBinding())
			return (MethodBinding) binding;
		return null;
	}
	//	Get binding from unit scope
	char[] typeName = PatternLocator.qualifiedPattern(methodPattern.getDeclaringSimpleName(), methodPattern.getDeclaringQualification());
	TypeBinding declaringTypeBinding = getType(typeName, typeName);
	if (declaringTypeBinding != null) {
		if (declaringTypeBinding.isArrayType()) {
			declaringTypeBinding = declaringTypeBinding.leafComponentType();
		}
		if (!declaringTypeBinding.isBaseType()) {
			char[][] parameterTypes = methodPattern.parameterSimpleNames;
			if (parameterTypes == null) return null;
			int paramTypeslength = parameterTypes.length;
			ReferenceBinding referenceBinding = (ReferenceBinding) declaringTypeBinding;
			MethodBinding[] methods = referenceBinding.getMethods(methodPattern.selector);
			int methodsLength = methods.length;
			for (int i=0; i<methodsLength; i++) {
				TypeBinding[] methodParameters = methods[i].parameters;
				int paramLength = methodParameters==null ? 0 : methodParameters.length;
				boolean found = false;
				if (methodParameters != null && paramLength == paramTypeslength) {
					for (int p=0; p<paramLength; p++) {
						if (CharOperation.equals(methodParameters[p].sourceName(), parameterTypes[p])) {
							// param erasure match
							found = true;
						} else {
							// type variable
							found = false;
							if (!found) break;
						}
					}
				}
				if (found) {
					this.bindings.put(methodPattern, methods[i]);
					return methods[i];
				}
			}
		}
	}
	this.bindings.put(methodPattern, new ProblemMethodBinding(methodPattern.selector, null, ProblemReasons.NotFound));
	return null;
}
protected boolean hasAlreadyDefinedType(CompilationUnitDeclaration parsedUnit) {
	CompilationResult result = parsedUnit.compilationResult;
	if (result == null) return false;
	for (int i = 0; i < result.problemCount; i++)
		if (result.problems[i].getID() == IProblem.DuplicateTypes)
			return true;
	return false;
}
/**
 * Create a new parser for the given project, as well as a lookup environment.
 */
public void initialize(JavaProject project, int possibleMatchSize) throws JavaScriptModelException {
	// clean up name environment only if there are several possible match as it is reused
	// when only one possible match (bug 58581)
	if (this.nameEnvironment != null && possibleMatchSize != 1)
		this.nameEnvironment.cleanup();

	SearchableEnvironment searchableEnvironment = project.newSearchableNameEnvironment(this.workingCopies);

	// if only one possible match, a file name environment costs too much,
	// so use the existing searchable  environment which will populate the java model
	// only for this possible match and its required types.
	this.nameEnvironment = true//possibleMatchSize == 1
		? (INameEnvironment) searchableEnvironment
		: (INameEnvironment) new JavaSearchNameEnvironment(project, this.workingCopies);

	// create lookup environment
	Map map = project.getOptions(true);
	map.put(CompilerOptions.OPTION_TaskTags, ""); //$NON-NLS-1$
	this.options = new CompilerOptions(map);
	ProblemReporter problemReporter =
		new ProblemReporter(
			DefaultErrorHandlingPolicies.proceedWithAllProblems(),
			this.options,
			new DefaultProblemFactory());
	this.lookupEnvironment = new LookupEnvironment(this, this.options, problemReporter, this.nameEnvironment);

	this.parser = MatchLocatorParser.createParser(problemReporter, this);

	// basic parser needs also to be reset as project options may have changed
	// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=163072
	this.basicParser = null;

	// remember project's name lookup
	this.nameLookup = searchableEnvironment.nameLookup;

	// initialize queue of units
	this.numberOfMatches = 0;
	this.matchesToProcess = new PossibleMatch[possibleMatchSize];
}
protected void locateMatches(JavaProject javaProject, PossibleMatch[] possibleMatches, int start, int length) throws CoreException {
	initialize(javaProject, length);

	// create and resolve binding (equivalent to beginCompilation() in Compiler)
	boolean mustResolvePattern = ((InternalSearchPattern)this.pattern).mustResolve;
	boolean mustResolve = mustResolvePattern;
	this.patternLocator.mayBeGeneric = this.options.sourceLevel >= ClassFileConstants.JDK1_5;
	boolean bindingsWereCreated = mustResolve;
	try {
		for (int i = start, maxUnits = start + length; i < maxUnits; i++) {
			PossibleMatch possibleMatch = possibleMatches[i];
			try {
				if (possibleMatch.openable instanceof MetadataFile)
				{
					this.currentPossibleMatch=possibleMatch;
					processMetadata((MetadataFile)possibleMatch.openable);
				}
				else
				{
				if (!parseAndBuildBindings(possibleMatch, mustResolvePattern)) continue;
				// Currently we only need to resolve over pattern flag if there's potential parameterized types
				if (this.patternLocator.mayBeGeneric) {
					// If pattern does not resolve then rely on possible match node set resolution
					// which may have been modified while locator was adding possible matches to it
					if (!mustResolvePattern && !mustResolve) {
						mustResolve = possibleMatch.nodeSet.mustResolve;
						bindingsWereCreated = mustResolve;
					}
				} else {
					// Reset matching node resolution with pattern one if there's no potential parameterized type
					// to minimize side effect on previous search behavior
					possibleMatch.nodeSet.mustResolve = mustResolvePattern;
				}
				// possible match node resolution has been merged with pattern one, so rely on it to know
				// whether we need to process compilation unit now or later
				if (!possibleMatch.nodeSet.mustResolve) {
					if (this.progressMonitor != null) {
						this.progressWorked++;
						if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
					}
					process(possibleMatch, bindingsWereCreated);
					if (this.numberOfMatches>0 && this.matchesToProcess[this.numberOfMatches-1] == possibleMatch) {
						// forget last possible match as it was processed
						this.numberOfMatches--;
					}
				}
				}
			} finally {
				if (!possibleMatch.nodeSet.mustResolve)
					possibleMatch.cleanUp();
			}
		}
		if (mustResolve)
			this.lookupEnvironment.completeTypeBindings();

		// create hierarchy resolver if needed
		IType focusType = getFocusType();
		if (focusType == null) {
			this.hierarchyResolver = null;
		} else if (!createHierarchyResolver(focusType, possibleMatches)) {
			// focus type is not visible, use the super type names instead of the bindings
			if (computeSuperTypeNames(focusType) == null) return;
		}
	} catch (AbortCompilation e) {
		bindingsWereCreated = false;
	}

	if (!mustResolve) {
		return;
	}

	// possible match resolution
	for (int i = 0; i < this.numberOfMatches; i++) {
		if (this.progressMonitor != null && this.progressMonitor.isCanceled())
			throw new OperationCanceledException();
		PossibleMatch possibleMatch = this.matchesToProcess[i];
		try {
			process(possibleMatch, bindingsWereCreated);
		} catch (AbortCompilation e) {
			// problem with class path: it could not find base classes
			// continue and try next matching openable reporting innacurate matches (since bindings will be null)
			bindingsWereCreated = false;
		} catch (JavaScriptModelException e) {
			// problem with class path: it could not find base classes
			// continue and try next matching openable reporting innacurate matches (since bindings will be null)
			bindingsWereCreated = false;
		} finally {
			if (this.progressMonitor != null) {
				this.progressWorked++;
				if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
			}
			if (this.options.verbose)
				System.out.println(
					Messages.bind(Messages.compilation_done,
						new String[] {
							String.valueOf(i + 1),
							String.valueOf(this.numberOfMatches),
							new String(possibleMatch.parsedUnit.getFileName())
						}));
		}
	}
	for (int i = 0; i < this.numberOfMatches; i++) {
		// cleanup compilation unit result
		this.matchesToProcess[i].cleanUp();
		this.matchesToProcess[i] = null; // release reference to processed possible match
	}
	if (this.progressMonitor != null && this.progressMonitor.isCanceled())
		throw new OperationCanceledException();
}
private void processMetadata(MetadataFile metadataFile) throws CoreException{
	
		IType [] types = metadataFile.getTypes();
		int matchLevel=PatternLocator.IMPOSSIBLE_MATCH;
		for (int typeIndex = 0; typeIndex < types.length; typeIndex++) {
			IType type=types[typeIndex];
			matchLevel=this.patternLocator.matchMetadataElement(type);
			if (matchLevel>=PatternLocator.POSSIBLE_MATCH)
				reportMatching(type, matchLevel, null, 1);
			IFunction[] methods = type.getFunctions();
			for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {
				IFunction method=methods[methodIndex];
				matchLevel=this.patternLocator.matchMetadataElement(method);
				if (matchLevel>=PatternLocator.POSSIBLE_MATCH)
					reportMatching(method, matchLevel, null, 1);
			}
			IField[] fields = type.getFields();
			for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
				IField field=fields[fieldIndex];
				matchLevel=this.patternLocator.matchMetadataElement(field);
				if (matchLevel>=PatternLocator.POSSIBLE_MATCH)
					reportMatching(field, matchLevel, null, 1);
			}
		}
		IFunction[] methods = metadataFile.getFunctions();
		for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {
			IFunction method=methods[methodIndex];
			matchLevel=this.patternLocator.matchMetadataElement(method);
			if (matchLevel>=PatternLocator.POSSIBLE_MATCH)
				reportMatching(method, matchLevel, null, 1);
		}
		IField[] fields = metadataFile.getFields();
		for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
			IField field=fields[fieldIndex];
			matchLevel=this.patternLocator.matchMetadataElement(field);
			if (matchLevel>=PatternLocator.POSSIBLE_MATCH)
				reportMatching(field, matchLevel, null, 1);
		}

}

/**
 * Locate the matches amongst the possible matches.
 */
protected void locateMatches(JavaProject javaProject, PossibleMatchSet matchSet, int expected) throws CoreException {
	PossibleMatch[] possibleMatches = matchSet.getPossibleMatches(javaProject.getPackageFragmentRoots());
	int length = possibleMatches.length;
	// increase progress from duplicate matches not stored in matchSet while adding...
	if (this.progressMonitor != null && expected>length) {
		this.progressWorked += expected-length;
		this.progressMonitor.worked( expected-length);
	}
	// locate matches (processed matches are limited to avoid problem while using VM default memory heap size)
	for (int index = 0; index < length;) {
		int max = Math.min(MAX_AT_ONCE, length - index);
		locateMatches(javaProject, possibleMatches, index, max);
		index += max;
	}
	this.patternLocator.clear();
}
/**
 * Locate the matches in the given files and report them using the search requestor.
 */
public void locateMatches(SearchDocument[] searchDocuments) throws CoreException {
	int docsLength = searchDocuments.length;
	if (BasicSearchEngine.VERBOSE) {
		System.out.println("Locating matches in documents ["); //$NON-NLS-1$
		for (int i = 0; i < docsLength; i++)
			System.out.println("\t" + searchDocuments[i]); //$NON-NLS-1$
		System.out.println("]"); //$NON-NLS-1$
	}

	// init infos for progress increasing
	int n = docsLength<1000 ? Math.min(Math.max(docsLength/200+1, 2),4) : 5 *(docsLength/1000);
	this.progressStep = docsLength < n ? 1 : docsLength / n; // step should not be 0
	this.progressWorked = 0;

	// extract working copies
	ArrayList copies = new ArrayList();
	for (int i = 0; i < docsLength; i++) {
		SearchDocument document = searchDocuments[i];
		if (document instanceof WorkingCopyDocument) {
			copies.add(((WorkingCopyDocument)document).workingCopy);
		}
	}
	int copiesLength = copies.size();
	this.workingCopies = new org.eclipse.wst.jsdt.core.IJavaScriptUnit[copiesLength];
	copies.toArray(this.workingCopies);

	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	this.bindings = new SimpleLookupTable();
	try {
		// optimize access to zip files during search operation
		manager.cacheZipFiles();

		// initialize handle factory (used as a cache of handles so as to optimize space)
		if (this.handleFactory == null)
			this.handleFactory = new HandleFactory();

		if (this.progressMonitor != null) {
			this.progressMonitor.beginTask("", searchDocuments.length); //$NON-NLS-1$
		}

		// initialize pattern for polymorphic search (ie. method reference pattern)
		this.patternLocator.initializePolymorphicSearch(this);

		JavaProject previousJavaProject = null;
		PossibleMatchSet matchSet = new PossibleMatchSet();
		Util.sort(searchDocuments, new Util.Comparer() {
			public int compare(Object a, Object b) {
				return ((SearchDocument)a).getPath().compareTo(((SearchDocument)b).getPath());
			}
		});
		int displayed = 0; // progress worked displayed
		String previousPath = null;
		for (int i = 0; i < docsLength; i++) {
			if (this.progressMonitor != null && this.progressMonitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			// skip duplicate paths
			SearchDocument searchDocument = searchDocuments[i];
			searchDocuments[i] = null; // free current document
			String pathString = searchDocument.getPath();
			if (i > 0 && pathString.equals(previousPath)) {
				if (this.progressMonitor != null) {
					this.progressWorked++;
					if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
				}
				displayed++;
				continue;
			}
			previousPath = pathString;

			Openable openable;
			org.eclipse.wst.jsdt.core.IJavaScriptUnit workingCopy = null;
			if (searchDocument instanceof WorkingCopyDocument) {
				workingCopy = ((WorkingCopyDocument)searchDocument).workingCopy;
				openable = (Openable) workingCopy;
			} else if(searchDocument.isVirtual()) {
				openable = (Openable)searchDocument.getJavaElement();
			}else{
				openable = this.handleFactory.createOpenable(pathString, this.scope);
			}
			if (openable == null) {
				if (this.progressMonitor != null) {
					this.progressWorked++;
					if ((this.progressWorked%this.progressStep)==0) this.progressMonitor.worked(this.progressStep);
				}
				displayed++;
				continue; // match is outside classpath
			}

			// create new parser and lookup environment if this is a new project
			IResource resource = null;
			JavaProject javaProject = (JavaProject) openable.getJavaScriptProject();
			resource = workingCopy != null ? workingCopy.getResource() : openable.getResource();
			if (resource == null)
				resource = javaProject.getProject(); // case of a file in an external jar
			if (!javaProject.equals(previousJavaProject)) {
				// locate matches in previous project
				if (previousJavaProject != null) {
					try {
						locateMatches(previousJavaProject, matchSet, i-displayed);
						displayed = i;
					} catch (JavaScriptModelException e) {
						// problem with classpath in this project -> skip it
					}
					matchSet.reset();
				}
				previousJavaProject = javaProject;
			}
			matchSet.add(new PossibleMatch(this, resource, openable, searchDocument, ((InternalSearchPattern) this.pattern).mustResolve));
		}

		// last project
		if (previousJavaProject != null) {
			try {
				locateMatches(previousJavaProject, matchSet, docsLength-displayed);
			} catch (JavaScriptModelException e) {
				// problem with classpath in last project -> ignore
			}
		}

	} finally {
		if (this.progressMonitor != null)
			this.progressMonitor.done();
		if (this.nameEnvironment != null)
			this.nameEnvironment.cleanup();
		if(this.parsedUnits != null) {
			parsedUnits.clear();
		}
		manager.flushZipFiles();
		this.bindings = null;
	}
}
//*/
protected IType lookupType(ReferenceBinding typeBinding) {
	if (typeBinding == null) return null;

	char[] packageName = typeBinding.qualifiedPackageName();
	IPackageFragment[] pkgs = this.nameLookup.findPackageFragments(
		(packageName == null || packageName.length == 0)
			? IPackageFragment.DEFAULT_PACKAGE_NAME
			: new String(packageName),
		false);

	// iterate type lookup in each package fragment
	char[] sourceName = typeBinding.qualifiedSourceName();
	String typeName = new String(sourceName);
	int acceptFlag = 0;
	if (typeBinding.isClass()) {
		acceptFlag = NameLookup.ACCEPT_CLASSES;
	}
	if (pkgs != null) {
		for (int i = 0, length = pkgs.length; i < length; i++) {
			IType type = this.nameLookup.findType(typeName, pkgs[i],  false,  acceptFlag, true/*consider secondary types*/);
			if (type != null) return type;
		}
	}

	// search inside enclosing element
	char[][] qualifiedName = CharOperation.splitOn('.', sourceName);
	int length = qualifiedName.length;
	if (length == 0) return null;

	IType type = createTypeHandle(new String(qualifiedName[0])); // find the top-level type
	if (type == null) return null;

	for (int i = 1; i < length; i++) {
		type = type.getType(new String(qualifiedName[i]));
		if (type == null) return null;
	}
	if (type.exists()) return type;
	return null;
}
public SearchMatch newDeclarationMatch(
		IJavaScriptElement element,
		Binding binding,
		int accuracy,
		int offset,
		int length) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	return newDeclarationMatch(element, binding, accuracy, offset, length, participant, resource);
}

public SearchMatch newDeclarationMatch(
		IJavaScriptElement element,
		Binding binding,
		int accuracy,
		int offset,
		int length,
		SearchParticipant participant,
		IResource resource) {
	switch (element.getElementType()) {
		case IJavaScriptElement.TYPE:
			return new TypeDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
		case IJavaScriptElement.FIELD:
			return new FieldDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
		case IJavaScriptElement.METHOD:
			return new MethodDeclarationMatch(binding == null ? element : ((JavaElement) element).resolved(binding), accuracy, offset, length, participant, resource);
		case IJavaScriptElement.LOCAL_VARIABLE:
			return new LocalVariableDeclarationMatch(element, accuracy, offset, length, participant, resource);
		default:
			return null;
	}
}

public SearchMatch newFieldReferenceMatch(
		IJavaScriptElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	int bits = reference.bits;
	boolean isCoupoundAssigned = (bits & ASTNode.IsCompoundAssigned) != 0;
	boolean isReadAccess = isCoupoundAssigned || (bits & ASTNode.IsStrictlyAssigned) == 0;
	boolean isWriteAccess = isCoupoundAssigned || (bits & ASTNode.IsStrictlyAssigned) != 0;
	boolean insideDocComment = (bits & ASTNode.InsideJavadoc) != 0;
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	if (enclosingBinding != null)
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	return new FieldReferenceMatch(enclosingElement, accuracy, offset, length, isReadAccess, isWriteAccess, insideDocComment, participant, resource);
}

public SearchMatch newLocalVariableReferenceMatch(
		IJavaScriptElement enclosingElement,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	int bits = reference.bits;
	boolean isCoupoundAssigned = (bits & ASTNode.IsCompoundAssigned) != 0;
	boolean isReadAccess = isCoupoundAssigned || (bits & ASTNode.IsStrictlyAssigned) == 0;
	boolean isWriteAccess = isCoupoundAssigned || (bits & ASTNode.IsStrictlyAssigned) != 0;
	boolean insideDocComment = (bits & ASTNode.InsideJavadoc) != 0;
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	return new LocalVariableReferenceMatch(enclosingElement, accuracy, offset, length, isReadAccess, isWriteAccess, insideDocComment, participant, resource);
}

public SearchMatch newMethodReferenceMatch(
		IJavaScriptElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length,
		boolean isConstructor,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = (reference.bits & ASTNode.InsideJavadoc) != 0;
	if (enclosingBinding != null)
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	boolean isOverridden = (accuracy & PatternLocator.SUPER_INVOCATION_FLAVOR) != 0;
	return new MethodReferenceMatch(enclosingElement, accuracy, offset, length, isConstructor, isOverridden, insideDocComment, participant, resource);
}

public SearchMatch newPackageReferenceMatch(
		IJavaScriptElement enclosingElement,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = (reference.bits & ASTNode.InsideJavadoc) != 0;
	return new PackageReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

public TypeReferenceMatch newTypeReferenceMatch(
		IJavaScriptElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		int offset,
		int length,
		ASTNode reference) {
	SearchParticipant participant = getParticipant();
	IResource resource = this.currentPossibleMatch.resource;
	boolean insideDocComment = (reference.bits & ASTNode.InsideJavadoc) != 0;
	if (enclosingBinding != null)
		enclosingElement = ((JavaElement) enclosingElement).resolved(enclosingBinding);
	return new TypeReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource);
}

public TypeReferenceMatch newTypeReferenceMatch(
		IJavaScriptElement enclosingElement,
		Binding enclosingBinding,
		int accuracy,
		ASTNode reference) {
	return newTypeReferenceMatch(enclosingElement, enclosingBinding, accuracy, reference.sourceStart, reference.sourceEnd-reference.sourceStart+1, reference);
}

/**
 * Add the possibleMatch to the loop
 *  ->  build compilation unit declarations, their bindings and record their results.
 */
protected boolean parseAndBuildBindings(PossibleMatch possibleMatch, boolean mustResolve) throws CoreException {
	if (this.progressMonitor != null && this.progressMonitor.isCanceled())
		throw new OperationCanceledException();

	try {
		if (BasicSearchEngine.VERBOSE)
			System.out.println("Parsing " + possibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$

		this.parser.nodeSet = possibleMatch.nodeSet;
		CompilationResult unitResult = new CompilationResult(possibleMatch, 1, 1, this.options.maxProblemsPerUnit);
		CompilationUnitDeclaration parsedUnit = this.parser.dietParse(possibleMatch, unitResult);
		if (parsedUnit != null) {
			this.parser.inferTypes(parsedUnit,this.options);
			if (!parsedUnit.isEmpty()) {
				if (mustResolve) {
					this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				}
				if (hasAlreadyDefinedType(parsedUnit)) return false; // skip type has it is hidden so not visible
				getMethodBodies(parsedUnit, possibleMatch.nodeSet);
				if (this.patternLocator.mayBeGeneric && !mustResolve && possibleMatch.nodeSet.mustResolve) {
					// special case: possible match node set force resolution although pattern does not
					// => we need to build types for this compilation unit
					this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				}
			}

			// add the possibleMatch with its parsedUnit to matchesToProcess
			possibleMatch.parsedUnit = parsedUnit;
			int size = this.matchesToProcess.length;
			if (this.numberOfMatches == size)
				System.arraycopy(this.matchesToProcess, 0, this.matchesToProcess = new PossibleMatch[size == 0 ? 1 : size * 2], 0, this.numberOfMatches);
			this.matchesToProcess[this.numberOfMatches++] = possibleMatch;
		}
	} finally {
		this.parser.nodeSet = null;
	}
	return true;
}
/*
 * Process a compilation unit already parsed and build.
 */
protected void process(PossibleMatch possibleMatch, boolean bindingsWereCreated) throws CoreException {
	this.currentPossibleMatch = possibleMatch;
	CompilationUnitDeclaration unit = possibleMatch.parsedUnit;
	try {
		if (unit.isEmpty()) {
			if (this.currentPossibleMatch.openable instanceof ClassFile) {
				ClassFile classFile = (ClassFile) this.currentPossibleMatch.openable;
				IBinaryType info = getBinaryInfo(classFile, this.currentPossibleMatch.resource);
				if (info != null) {
					boolean mayBeGeneric = this.patternLocator.mayBeGeneric;
					this.patternLocator.mayBeGeneric = false; // there's no longer generics in class files
					try {
						new ClassFileMatchLocator().locateMatches(this, classFile, info);
					}
					finally {
						this.patternLocator.mayBeGeneric = mayBeGeneric;
					}
				}
			}
			return;
		}
		if (hasAlreadyDefinedType(unit)) return; // skip type has it is hidden so not visible

		// Move getMethodBodies to #parseAndBuildings(...) method to allow possible match resolution management
		//getMethodBodies(unit);

		boolean mustResolve = (((InternalSearchPattern)this.pattern).mustResolve || possibleMatch.nodeSet.mustResolve);
		if (bindingsWereCreated && mustResolve) {
			if (unit.types != null || unit.statements!=null) {
				if (BasicSearchEngine.VERBOSE)
					System.out.println("Resolving " + this.currentPossibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$

				this.lookupEnvironment.unitBeingCompleted = unit;
				reduceParseTree(unit);

				if (unit.scope != null) {
					// fault in fields & methods
					unit.scope.faultInTypes();
				}
				unit.resolve();
			} else if (unit.isPackageInfo()) {
				if (BasicSearchEngine.VERBOSE)
					System.out.println("Resolving " + this.currentPossibleMatch.openable.toStringWithAncestors()); //$NON-NLS-1$
				unit.resolve();
			}
		}
		reportMatching(unit, mustResolve);
	} catch (AbortCompilation e) {
		// could not resolve: report inaccurate matches
		reportMatching(unit, false); // do not resolve when cu has errors
		if (!(e instanceof AbortCompilationUnit)) {
			// problem with class path
			throw e;
		}
	} finally {
		this.lookupEnvironment.unitBeingCompleted = null;
		this.currentPossibleMatch = null;
	}
}
protected void purgeMethodStatements(TypeDeclaration type, boolean checkEachMethod) {
	checkEachMethod = checkEachMethod
		&& this.currentPossibleMatch.nodeSet.hasPossibleNodes(type.declarationSourceStart, type.declarationSourceEnd);
	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		if (checkEachMethod) {
			for (int j = 0, length = methods.length; j < length; j++) {
				AbstractMethodDeclaration method = methods[j];
				purgeMethodStatements(method);
			}
		} else {
			for (int j = 0, length = methods.length; j < length; j++) {
				methods[j].statements = null;
				methods[j].javadoc = null;
			}
		}
	}

	TypeDeclaration[] memberTypes = type.memberTypes;
	if (memberTypes != null)
		for (int i = 0, l = memberTypes.length; i < l; i++)
			purgeMethodStatements(memberTypes[i], checkEachMethod);
}

private void purgeMethodStatements(AbstractMethodDeclaration method) {
	if (!this.currentPossibleMatch.nodeSet.hasPossibleNodes(method.declarationSourceStart, method.declarationSourceEnd)) {
		method.statements = null;
		method.javadoc = null;
	}
}

/**
 * Called prior to the unit being resolved. Reduce the parse tree where possible.
 */
protected void reduceParseTree(CompilationUnitDeclaration unit) {
	// remove statements from methods that have no possible matching nodes
	if (unit.types!=null)
	{
		TypeDeclaration[] types = unit.types;
		for (int i = 0, l = types.length; i < l; i++)
			purgeMethodStatements(types[i], true);
	}
	if (unit.statements!=null)
		for (int i = 0; i < unit.statements.length; i++) {
			if (unit.statements[i] instanceof AbstractMethodDeclaration)
				purgeMethodStatements((AbstractMethodDeclaration)unit.statements[i]);
		}

}
public SearchParticipant getParticipant() {
	return this.currentPossibleMatch.document.getParticipant();
}

protected void report(SearchMatch match) throws CoreException {
	long start = -1;
	if (BasicSearchEngine.VERBOSE) {
		start = System.currentTimeMillis();
		System.out.println("Reporting match"); //$NON-NLS-1$
		System.out.println("\tResource: " + match.getResource());//$NON-NLS-1$
		System.out.println("\tPositions: [offset=" + match.getOffset() + ", length=" + match.getLength() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			if (this.parser != null && match.getOffset() > 0 && match.getLength() > 0 && !(match.getElement() instanceof BinaryMember)) {
				String selection = new String(this.parser.scanner.source, match.getOffset(), match.getLength());
				System.out.println("\tSelection: -->" + selection + "<--"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (Exception e) {
			// it's just for debug purposes... ignore all exceptions in this area
		}
		try {
			JavaElement javaElement = (JavaElement)match.getElement();
			System.out.println("\tJava element: "+ javaElement.toStringWithAncestors()); //$NON-NLS-1$
			if (!javaElement.exists()) {
				System.out.println("\t\tWARNING: this element does NOT exist!"); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// it's just for debug purposes... ignore all exceptions in this area
		}
		if (match instanceof TypeReferenceMatch) {
			try {
				TypeReferenceMatch typeRefMatch = (TypeReferenceMatch) match;
				JavaElement local = (JavaElement) typeRefMatch.getLocalElement();
				if (local != null) {
					System.out.println("\tLocal element: "+ local.toStringWithAncestors()); //$NON-NLS-1$
				}
				IJavaScriptElement[] others = typeRefMatch.getOtherElements();
				if (others != null) {
					int length = others.length;
					if (length > 0) {
						System.out.println("\tOther elements:"); //$NON-NLS-1$
						for (int i=0; i<length; i++) {
							JavaElement other = (JavaElement) others[i];
							System.out.println("\t\t- "+ other.toStringWithAncestors()); //$NON-NLS-1$
						}
					}
				}
			} catch (Exception e) {
				// it's just for debug purposes... ignore all exceptions in this area
			}
		}
		System.out.println(match.getAccuracy() == SearchMatch.A_ACCURATE
			? "\tAccuracy: EXACT_MATCH" //$NON-NLS-1$
			: "\tAccuracy: POTENTIAL_MATCH"); //$NON-NLS-1$
		System.out.print("\tRule: "); //$NON-NLS-1$
		if (match.isExact()) {
			System.out.print("EXACT"); //$NON-NLS-1$
		} else if (match.isEquivalent()) {
			System.out.print("EQUIVALENT"); //$NON-NLS-1$
		} else if (match.isErasure()) {
			System.out.print("ERASURE"); //$NON-NLS-1$
		} else {
			System.out.print("INVALID RULE"); //$NON-NLS-1$
		}
		if (match instanceof MethodReferenceMatch) {
			MethodReferenceMatch methodReferenceMatch = (MethodReferenceMatch) match;
			if (methodReferenceMatch.isSuperInvocation()) {
				System.out.print("+SUPER INVOCATION"); //$NON-NLS-1$
			}
			if (methodReferenceMatch.isImplicit()) {
				System.out.print("+IMPLICIT"); //$NON-NLS-1$
			}
		}
		System.out.println("\n\tRaw: "+match.isRaw()); //$NON-NLS-1$
	}
	this.requestor.acceptSearchMatch(match);
	if (BasicSearchEngine.VERBOSE)
		this.resultCollectorTime += System.currentTimeMillis()-start;
}
/**
 * Finds the accurate positions of the sequence of tokens given by qualifiedName
 * in the source and reports a reference to this this qualified name
 * to the search requestor.
 */
protected void reportAccurateTypeReference(SearchMatch match, ASTNode typeRef, char[] name) throws CoreException {
	if (match.getRule() == 0) return;
	if (!encloses((IJavaScriptElement)match.getElement())) return;

	// Compute source positions of the qualified reference
	int sourceStart = typeRef.sourceStart;
	int sourceEnd = typeRef.sourceEnd;
	Scanner scanner = this.parser.scanner;
	scanner.setSource(this.currentPossibleMatch.getContents());
	scanner.resetTo(sourceStart, sourceEnd);

	int token = -1;
	int currentPosition;
	do {
		currentPosition = scanner.currentPosition;
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			// ignore
		}
		if (token == TerminalTokens.TokenNameIdentifier && this.pattern.matchesName(name, scanner.getCurrentTokenSource())) {
			int length = scanner.currentPosition-currentPosition;
			match.setOffset(currentPosition);
			match.setLength(length);
			report(match);
			return;
		}
	} while (token != TerminalTokens.TokenNameEOF);

	//	Report match
	match.setOffset(sourceStart);
	match.setLength(sourceEnd-sourceStart+1);
	report(match);
}

/**
 * Finds the accurate positions of the sequence of tokens given by qualifiedName
 * in the source and reports a reference to this parameterized type name
 * to the search requestor.
 * @since 3.1
 */
protected void reportAccurateParameterizedMethodReference(SearchMatch match, ASTNode statement, TypeReference[] typeArguments) throws CoreException {
	if (match.getRule() == 0) return;
	if (!encloses((IJavaScriptElement)match.getElement())) return;

	// If there's type arguments, look for end (ie. char '>') of last one.
	int start = match.getOffset();
	if (typeArguments != null && typeArguments.length > 0) {
		boolean isErasureMatch= (pattern instanceof OrPattern) ? ((OrPattern)pattern).isErasureMatch() : ((JavaSearchPattern)pattern).isErasureMatch();
		if (!isErasureMatch) {

			// Initialize scanner
			Scanner scanner = this.parser.scanner;
			char[] source = this.currentPossibleMatch.getContents();
			scanner.setSource(source);

			// Search previous opening '<'
			start = typeArguments[0].sourceStart;
			int end = statement.sourceEnd;
			scanner.resetTo(start, end);
			int lineStart = start;
			try {
				linesUp: while (true) {
					while (scanner.source[scanner.currentPosition] != '\n') {
						scanner.currentPosition--;
						if (scanner.currentPosition == 0) break linesUp;
					}
					lineStart = scanner.currentPosition+1;
					scanner.resetTo(lineStart, end);
					while (!scanner.atEnd()) {
						if (scanner.getNextToken() == TerminalTokens.TokenNameLESS) {
							start = scanner.getCurrentTokenStartPosition();
							break linesUp;
						}
					}
					end = lineStart - 2;
					scanner.currentPosition = end;
				}
			}
			catch (InvalidInputException ex) {
				// give up
			}
	 	}
	}

	// Report match
	match.setOffset(start);
	match.setLength(statement.sourceEnd-start+1);
	report(match);
}

/**
 * Finds the accurate positions of the sequence of tokens given by qualifiedName
 * in the source and reports a reference to this parameterized type name
 * to the search requestor.
 * @since 3.1
 */
protected void reportAccurateParameterizedTypeReference(SearchMatch match, TypeReference typeRef, int index, TypeReference[] typeArguments) throws CoreException {
	if (match.getRule() == 0) return;
	if (!encloses((IJavaScriptElement)match.getElement())) return;

	// If there's type arguments, look for end (ie. char '>') of last one.
	int end = typeRef.sourceEnd;
	if (typeArguments != null) {
		// Initialize scanner
		Scanner scanner = this.parser.scanner;
		char[] source = this.currentPossibleMatch.getContents();
		scanner.setSource(source);

		boolean shouldMatchErasure= (pattern instanceof OrPattern) ? ((OrPattern)pattern).isErasureMatch() : ((JavaSearchPattern)pattern).isErasureMatch();
		boolean hasSignatures = (pattern instanceof OrPattern) ? ((OrPattern)pattern).hasSignatures() : ((JavaSearchPattern)pattern).hasSignatures();
		if (shouldMatchErasure || !hasSignatures) {
			// if pattern is erasure only, then select the end of the reference
			if (typeRef instanceof QualifiedTypeReference && index >= 0) {
				long[] positions = ((QualifiedTypeReference) typeRef).sourcePositions;
				end = (int) positions[index];
			} else if (typeRef instanceof ArrayTypeReference) {
				end = ((ArrayTypeReference) typeRef).originalSourceEnd;
			}
		}  else {
			// Set scanner position at end of last type argument
			scanner.resetTo(end, source.length-1);
			int depth = 0;
			for (int i=typeArguments.length-1; i>=0; i--) {
				if (typeArguments[i] != null) {
					long lastTypeArgInfo = findLastTypeArgumentInfo(typeArguments[i]);
					depth = (int) (lastTypeArgInfo >>> 32)+1;
					scanner.resetTo(((int)lastTypeArgInfo)+1, scanner.eofPosition-1);
					break;
				}
			}

			// Now, scan to search next closing '>'
			while (depth-- > 0) {
				while (!scanner.atEnd()) {
					if (scanner.getNextChar() == '>') {
						end = scanner.currentPosition - 1;
						break;
					}
				}
			}
	 	}
	}

	// Report match
	match.setLength(end-match.getOffset()+1);
	report(match);
}
/**
 * Finds the accurate positions of each valid token in the source and
 * reports a reference to this token to the search requestor.
 * A token is valid if it has an accuracy which is not -1.
 */
protected void reportAccurateEnumConstructorReference(SearchMatch match, FieldDeclaration field, AllocationExpression allocation) throws CoreException {
	report(match);
	return;
}
/**
 * Finds the accurate positions of each valid token in the source and
 * reports a reference to this token to the search requestor.
 * A token is valid if it has an accuracy which is not -1.
 */
protected void reportAccurateFieldReference(SearchMatch[] matches, QualifiedNameReference qNameRef) throws CoreException {
	if (matches == null) return; // there's nothing to accurate in this case
	int matchesLength = matches.length;

	int sourceStart = qNameRef.sourceStart;
	int sourceEnd = qNameRef.sourceEnd;
	char[][] tokens = qNameRef.tokens;

	// compute source positions of the qualified reference
	Scanner scanner = this.parser.scanner;
	scanner.setSource(this.currentPossibleMatch.getContents());
	scanner.resetTo(sourceStart, sourceEnd);
	int sourceLength = sourceEnd-sourceStart+1;

	int refSourceStart = -1, refSourceEnd = -1;
	int length = tokens.length;
	int token = -1;
	int previousValid = -1;
	int i = 0;
	int index = 0;
	do {
		int currentPosition = scanner.currentPosition;
		// read token
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			//ignore
		}
		if (token != TerminalTokens.TokenNameEOF) {
			char[] currentTokenSource = scanner.getCurrentTokenSource();
			boolean equals = false;
			while (i < length && !(equals = this.pattern.matchesName(tokens[i++], currentTokenSource))){/*empty*/}
			if (equals && (previousValid == -1 || previousValid == i - 2)) {
				previousValid = i - 1;
				if (refSourceStart == -1)
					refSourceStart = currentPosition;
				refSourceEnd = scanner.currentPosition - 1;
			} else {
				i = 0;
				refSourceStart = -1;
				previousValid = -1;
			}
			// read '.'
			try {
				token = scanner.getNextToken();
			} catch (InvalidInputException e) {
				// ignore
			}
		}
		SearchMatch match = matches[index];
		if (match != null && match.getRule() != 0) {
			if (!encloses((IJavaScriptElement)match.getElement())) return;
			// accept reference
			if (refSourceStart != -1) {
				match.setOffset(refSourceStart);
				match.setLength(refSourceEnd-refSourceStart+1);
				report(match);
			} else {
				match.setOffset(sourceStart);
				match.setLength(sourceLength);
				report(match);
			}
			i = 0;
		}
		refSourceStart = -1;
		previousValid = -1;
		if (index < matchesLength - 1) {
			index++;
		}
	} while (token != TerminalTokens.TokenNameEOF);

}
protected void reportBinaryMemberDeclaration(IResource resource, IMember binaryMember, Binding binaryMemberBinding, IBinaryType info, int accuracy) throws CoreException {
	ClassFile classFile = (ClassFile) binaryMember.getClassFile();
	ISourceRange range = classFile.isOpen() ? binaryMember.getNameRange() : SourceMapper.UNKNOWN_RANGE;
	if (range.getOffset() == -1) {
		BinaryType type = (BinaryType) classFile.getType();
		String sourceFileName = type.sourceFileName(info);
		if (sourceFileName != null) {
			SourceMapper mapper = classFile.getSourceMapper();
			if (mapper != null) {
				char[] contents = mapper.findSource(type, sourceFileName);
				if (contents != null)
					range = mapper.mapSource(type, contents, info, binaryMember);
			}
		}
	}
	if (resource == null) resource =  this.currentPossibleMatch.resource;
	SearchMatch match = newDeclarationMatch(binaryMember, binaryMemberBinding, accuracy, range.getOffset(), range.getLength(), getParticipant(), resource);
	report(match);
}
/**
 * Visit the given method declaration and report the nodes that match exactly the
 * search pattern (ie. the ones in the matching nodes set)
 * Note that the method declaration has already been checked.
 */
protected void reportMatching(TypeDeclaration type, AbstractMethodDeclaration method, IJavaScriptElement parent, int accuracy, boolean typeInHierarchy, MatchingNodeSet nodeSet) throws CoreException {
	IJavaScriptElement enclosingElement = null;
	if (accuracy > -1) {
		enclosingElement = createHandle(method, parent);
		if (enclosingElement != null) { // skip if unable to find method
			// compute source positions of the selector
			Scanner scanner = parser.scanner;
			int nameSourceStart = method.sourceStart;
			scanner.setSource(this.currentPossibleMatch.getContents());
			scanner.resetTo(nameSourceStart, method.sourceEnd);
			try {
				scanner.getNextToken();
			} catch (InvalidInputException e) {
				// ignore
			}
			if (encloses(enclosingElement)) {
				SearchMatch match = null;
				if (method.isDefaultConstructor()) {
					// Use type for match associated element as default constructor does not exist in source
					int offset = type.sourceStart;
					match = this.patternLocator.newDeclarationMatch(type, parent, type.binding, accuracy, type.sourceEnd-offset+1, this);
				} else {
					int length = scanner.currentPosition - nameSourceStart;
					match = this.patternLocator.newDeclarationMatch(method, enclosingElement, method.getBinding(), accuracy, length, this);
				}
				if (match != null) {
					report(match);
				}
			}
		}
	}

	// handle nodes for the local type first
	if ((method.bits & ASTNode.HasLocalType) != 0) {
		if (enclosingElement == null)
			enclosingElement = createHandle(method, parent);
		LocalDeclarationVisitor localDeclarationVisitor = new LocalDeclarationVisitor(enclosingElement, method.getBinding(), nodeSet);
		try {
			method.traverse(localDeclarationVisitor, (ClassScope) null);
		} catch (WrappedCoreException e) {
			throw e.coreException;
		}
	}

	// references in this method
	if (typeInHierarchy) {
		ASTNode[] nodes = nodeSet.matchingNodes(method.declarationSourceStart, method.declarationSourceEnd);
		if (nodes != null) {
			if ((this.matchContainer & PatternLocator.METHOD_CONTAINER) != 0) {
				if (enclosingElement == null)
					enclosingElement = createHandle(method, parent);
				if (encloses(enclosingElement)) {
					for (int i = 0, l = nodes.length; i < l; i++) {
						ASTNode node = nodes[i];
						Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
						this.patternLocator.matchReportReference(node, enclosingElement, method.getBinding(), method.getScope(), level.intValue(), this);
					}
					return;
				}
			}
			for (int i = 0, l = nodes.length; i < l; i++)
				nodeSet.matchingNodes.removeKey(nodes[i]);
		}
	}
}
/**
 * Visit the given resolved parse tree and report the nodes that match the search pattern.
 */
protected void reportMatching(CompilationUnitDeclaration unit, boolean mustResolve) throws CoreException {
	MatchingNodeSet nodeSet = this.currentPossibleMatch.nodeSet;
	boolean locatorMustResolve = this.patternLocator.mustResolve;
	if (nodeSet.mustResolve) this.patternLocator.mustResolve = true;
	if (BasicSearchEngine.VERBOSE) {
		System.out.println("Report matching: "); //$NON-NLS-1$
		int size = nodeSet.matchingNodes==null ? 0 : nodeSet.matchingNodes.elementSize;
		System.out.print("	- node set: accurate="+ size); //$NON-NLS-1$
		size = nodeSet.possibleMatchingNodesSet==null ? 0 : nodeSet.possibleMatchingNodesSet.elementSize;
		System.out.println(", possible="+size); //$NON-NLS-1$
		System.out.print("	- must resolve: "+mustResolve); //$NON-NLS-1$
		System.out.print(" (locator: "+this.patternLocator.mustResolve); //$NON-NLS-1$
		System.out.println(", nodeSet: "+nodeSet.mustResolve+')'); //$NON-NLS-1$
	}
	if (mustResolve) {
		this.unitScope= unit.scope.compilationUnitScope();
		// move the possible matching nodes that exactly match the search pattern to the matching nodes set
		Object[] nodes = nodeSet.possibleMatchingNodesSet.values;
		for (int i = 0, l = nodes.length; i < l; i++) {
			ASTNode node = (ASTNode) nodes[i];
			if (node == null) continue;
			if (node instanceof ImportReference) {
				// special case for import refs: they don't know their binding
				// import ref cannot be in the hierarchy of a type
				if (this.hierarchyResolver != null) continue;

				ImportReference importRef = (ImportReference) node;
				Binding binding = (importRef.bits & ASTNode.OnDemand) != 0
					? unitScope.getImport(CharOperation.subarray(importRef.tokens, 0, importRef.tokens.length), true)
					: unitScope.getImport(importRef.tokens, false);
				this.patternLocator.matchLevelAndReportImportRef(importRef, binding, this);
			}
			nodeSet.addMatch(node, this.patternLocator.resolveLevel(node));
		}
		nodeSet.possibleMatchingNodesSet = new SimpleSet(3);
		if (BasicSearchEngine.VERBOSE) {
			int size = nodeSet.matchingNodes==null ? 0 : nodeSet.matchingNodes.elementSize;
			System.out.print("	- node set: accurate="+size); //$NON-NLS-1$
			size = nodeSet.possibleMatchingNodesSet==null ? 0 : nodeSet.possibleMatchingNodesSet.elementSize;
			System.out.println(", possible="+size); //$NON-NLS-1$
		}
	} else {
		this.unitScope = null;
	}

	if (nodeSet.matchingNodes.elementSize == 0) return; // no matching nodes were found
	this.methodHandles = new HashSet();

	boolean matchedUnitContainer = (this.matchContainer & PatternLocator.COMPILATION_UNIT_CONTAINER) != 0;

	// report references in javadoc
	if (unit.javadoc != null) {
		ASTNode[] nodes = nodeSet.matchingNodes(unit.javadoc.sourceStart, unit.javadoc.sourceEnd);
		if (nodes != null) {
			if (!matchedUnitContainer) {
				for (int i = 0, l = nodes.length; i < l; i++)
					nodeSet.matchingNodes.removeKey(nodes[i]);
			} else {
				IJavaScriptElement element = null;
				for (int i = 0, l = nodes.length; i < l; i++) {
					ASTNode node = nodes[i];
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
					if (encloses(element))
						this.patternLocator.matchReportReference(node, element, null/*no binding*/, level.intValue(), this);
				}
			}
		}
	}

	if (matchedUnitContainer) {
		ImportReference[] imports = unit.imports;
		if (imports != null) {
			for (int i = 0, l = imports.length; i < l; i++) {
				ImportReference importRef = imports[i];
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(importRef);
				if (level != null)
					this.patternLocator.matchReportImportRef(importRef, null/*no binding*/, createImportHandle(importRef), level.intValue(), this);
			}
		}
	}

	TypeDeclaration[] types = unit.types;
	if (types != null) {
		for (int i = 0, l = types.length; i < l; i++) {
			if (nodeSet.matchingNodes.elementSize == 0) return; // reported all the matching nodes
			TypeDeclaration type = types[i];
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(type);
			int accuracy = (level != null && matchedUnitContainer) ? level.intValue() : -1;
			reportMatching(type, null, accuracy, nodeSet, 1);
		}
	}
	ProgramElement[] statements = unit.statements;

	if (statements!=null)
	{
		IJavaScriptElement enclosingElement = this.currentPossibleMatch.openable;
		if (enclosingElement == null) return;

		boolean typeInHierarchy = true;
		boolean matchedClassContainer=true;
		ReportMatchingVisitor reportMatchingVisitor = new ReportMatchingVisitor(nodeSet,matchedClassContainer,enclosingElement,typeInHierarchy);
		unit.traverse(reportMatchingVisitor, unit.scope);

		for (int i = 0; i < statements.length; i++) {
			if (nodeSet.matchingNodes.elementSize == 0) return; // reported all the matching nodes
			if (statements[i] instanceof AbstractMethodDeclaration) {//already handled
			} else if (statements[i] instanceof LocalDeclaration) {//already handled
			}
			else
			{
				ASTNode[] nodes = nodeSet.matchingNodes(statements[i].sourceStart, statements[i].sourceEnd);
				if (nodes != null) {
					if ((this.matchContainer & PatternLocator.COMPILATION_UNIT_CONTAINER) != 0) {
						if (encloses(enclosingElement)) {
							for (int j = 0, l = nodes.length; j < l; j++) {
								ASTNode node = nodes[j];
								Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
								int accuracy = (level != null && matchedClassContainer) ? level.intValue() : -1;
								this.patternLocator.matchReportReference(node, enclosingElement, unit.compilationUnitBinding, unit.scope, accuracy, this);
							}
//							return;
						}
					}
					for (int j = 0, l = nodes.length; j < l; j++)
						nodeSet.matchingNodes.removeKey(nodes[j]);
				}
			}

		}
	}
	for (int i=0;i<unit.numberInferredTypes;i++) {
		InferredType inferredType = unit.inferredTypes[i];
			IJavaScriptElement enclosingElement = this.currentPossibleMatch.openable;
			if (enclosingElement == null) return;
			boolean typeInHierarchy = true;
			boolean matchedClassContainer=true;
			enclosingElement=((ITypeRoot)enclosingElement).getType(new String(inferredType.getName()));

			Integer level = (Integer) nodeSet.matchingNodes.removeKey(inferredType);
			int accuracy = (level != null && matchedClassContainer) ? level.intValue() : -1;
			reportMatching(inferredType, null, accuracy, nodeSet, 1);

			  for (int attributeInx=0; attributeInx<inferredType.numberAttributes; attributeInx++) {
					InferredAttribute attribute = inferredType.attributes[attributeInx];
					 level = (Integer) nodeSet.matchingNodes.removeKey(attribute);
					 accuracy = (level != null && matchedClassContainer) ? level.intValue() : -1;
						reportMatching(attribute, inferredType, enclosingElement, accuracy, typeInHierarchy, nodeSet);

				}
			ArrayList methods = inferredType.methods;
			if (methods!=null)
				for (Iterator iterator = methods.iterator(); iterator.hasNext();) {
					InferredMethod method = (InferredMethod) iterator.next();
					 level = (Integer) nodeSet.matchingNodes.removeKey(method);
					 accuracy = (level != null && matchedClassContainer) ? level.intValue() : -1;
					int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
					reportMatching(null, (AbstractMethodDeclaration) method.getFunctionDeclaration(), enclosingElement, value, typeInHierarchy, nodeSet);

				}
		}


	// Clear handle cache
	this.methodHandles = null;
	this.bindings.removeKey(this.pattern);
	this.patternLocator.mustResolve = locatorMustResolve;
}
/**
 * Visit the given field declaration and report the nodes that match exactly the
 * search pattern (ie. the ones in the matching nodes set)
 */
protected void reportMatching(FieldDeclaration field, FieldDeclaration[] otherFields, TypeDeclaration type, IJavaScriptElement parent, int accuracy, boolean typeInHierarchy, MatchingNodeSet nodeSet) throws CoreException {
	IJavaScriptElement enclosingElement = null;
	if (accuracy > -1) {
		enclosingElement = createHandle(field, type, parent);
		if (encloses(enclosingElement)) {
			int offset = field.sourceStart;
			SearchMatch match = newDeclarationMatch(enclosingElement, field.binding, accuracy, offset, field.sourceEnd-offset+1);
			if (field.initialization instanceof AllocationExpression) {
				reportAccurateEnumConstructorReference(match, field, (AllocationExpression) field.initialization);
			} else {
				report(match);
			}
		}
	}

	// handle the nodes for the local type first
	if ((field.bits & ASTNode.HasLocalType) != 0) {
		if (enclosingElement == null)
			enclosingElement = createHandle(field, type, parent);
		LocalDeclarationVisitor localDeclarationVisitor = new LocalDeclarationVisitor(enclosingElement, field.binding, nodeSet);
		try {
			field.traverse(localDeclarationVisitor, null);
		} catch (WrappedCoreException e) {
			throw e.coreException;
		}
	}

	if (typeInHierarchy) {
		// Look at field declaration
		if (field.endPart1Position != 0) { // not necessary if field is an initializer
			ASTNode[] nodes = nodeSet.matchingNodes(field.declarationSourceStart, field.endPart1Position);
			if (nodes != null) {
				if ((this.matchContainer & PatternLocator.FIELD_CONTAINER) == 0) {
					for (int i = 0, l = nodes.length; i < l; i++)
						nodeSet.matchingNodes.removeKey(nodes[i]);
				} else {
					if (enclosingElement == null)
						enclosingElement = createHandle(field, type, parent);
					if (encloses(enclosingElement)) {
						for (int i = 0, l = nodes.length; i < l; i++) {
							ASTNode node = nodes[i];
							Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
							IJavaScriptElement[] otherElements = null;
							if (otherFields != null) {
								int length = otherFields.length;
								int size = 0;
								while (size<length && otherFields[size] != null) {
									size++;
								}
								otherElements = new IJavaScriptElement[size];
								for (int j=0; j<size; j++) {
									otherElements[j] = createHandle(otherFields[j], type, parent);
								}
							}
							this.patternLocator.matchReportReference(node, enclosingElement, null, otherElements, field.binding, level.intValue(), this);
						}
					}
				}
			}
		}

		// Look in initializer
		int fieldEnd = field.endPart2Position == 0 ? field.declarationSourceEnd : field.endPart2Position;
		ASTNode[] nodes = nodeSet.matchingNodes(field.sourceStart, fieldEnd);
		if (nodes != null) {
			if ((this.matchContainer & PatternLocator.FIELD_CONTAINER) == 0) {
				for (int i = 0, l = nodes.length; i < l; i++)
					nodeSet.matchingNodes.removeKey(nodes[i]);
			} else {
				if (enclosingElement == null) {
					enclosingElement = createHandle(field, type, parent);
				}
				if (encloses(enclosingElement)) {
					for (int i = 0, l = nodes.length; i < l; i++) {
						ASTNode node = nodes[i];
						Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
						// Set block scope for initializer in case there would have other local and other elements to report
						BlockScope blockScope = null;
						if (field.getKind() == AbstractVariableDeclaration.INITIALIZER) {
							Block block = ((Initializer)field).block;
							if (block != null) blockScope = block.scope;
						}
						this.patternLocator.matchReportReference(node, enclosingElement, field.binding, blockScope, level.intValue(), this);
					}
				}
			}
		}
	}
}


protected void reportMatching(InferredAttribute field,   InferredType type, IJavaScriptElement parent, int accuracy, boolean typeInHierarchy, MatchingNodeSet nodeSet) throws CoreException {
	IJavaScriptElement enclosingElement = null;
	if (accuracy > -1) {
		enclosingElement = createHandle(field, type, parent);
		if (encloses(enclosingElement)) {
			int offset = field.sourceStart;
			SearchMatch match = newDeclarationMatch(enclosingElement, field.binding, accuracy, offset, field.sourceEnd-offset+1);
			report(match);
		}
	}


}

protected void reportMatching(InferredType type, IJavaScriptElement parent, int accuracy, MatchingNodeSet nodeSet, int occurrenceCount) throws CoreException {
	// create type handle
	IJavaScriptElement enclosingElement = parent;
	if (enclosingElement == null) {
		enclosingElement = createTypeHandle(new String(type.getName()));
	} else if (enclosingElement instanceof IType) {
		enclosingElement = ((IType) parent).getType(new String(type.getName()));
	} else if (enclosingElement instanceof IMember) {
	    IMember member = (IMember) parent;
		if (member.isBinary())  {
			enclosingElement = ((IClassFile)this.currentPossibleMatch.openable).getType();
		} else {
			enclosingElement = member.getType(new String(type.getName()), occurrenceCount);
		}
	}
	if (enclosingElement == null) return;
	boolean enclosesElement = encloses(enclosingElement);

	// report the type declaration
	if (accuracy > -1 && enclosesElement) {
		int offset = type.sourceStart;
		SearchMatch match = this.patternLocator.newDeclarationMatch(type, enclosingElement, type.binding, accuracy, type.sourceEnd-offset+1, this);
		report(match);
	}

//	boolean matchedClassContainer = (this.matchContainer & PatternLocator.CLASS_CONTAINER) != 0;

}


protected void reportMatching( IJavaScriptElement enclosingElement, int accuracy, MatchingNodeSet nodeSet, int occurrenceCount) throws CoreException {
	// create type handle
	if (enclosingElement == null) return;
	boolean enclosesElement = encloses(enclosingElement);

	// report the type declaration
	if (accuracy > -1 && enclosesElement) {
		int offset = 0;//element.g;
		int elementLength=0;
		SearchMatch match = this.patternLocator.newDeclarationMatch(null, enclosingElement, null, accuracy, elementLength, this);
		report(match);
	}

//	boolean matchedClassContainer = (this.matchContainer & PatternLocator.CLASS_CONTAINER) != 0;

}


protected void reportMatching(LocalDeclaration field, LocalDeclaration[] otherFields, TypeDeclaration type, IJavaScriptElement parent, int accuracy, boolean typeInHierarchy, MatchingNodeSet nodeSet) throws CoreException {
	IJavaScriptElement enclosingElement = null;
	if (accuracy > -1) {
		enclosingElement = createHandle(field,   parent);
		if (encloses(enclosingElement)) {
			int offset = field.sourceStart;
			SearchMatch match = newDeclarationMatch(enclosingElement, field.binding, accuracy, offset, field.sourceEnd-offset+1);
			report(match);
		}
	}


	if (typeInHierarchy) {
		// Look in initializer
		ASTNode[] nodes = nodeSet.matchingNodes(field.sourceStart, field.declarationSourceEnd);
		if (nodes != null) {
			if ((this.matchContainer & PatternLocator.FIELD_CONTAINER) != 0) {
				if (enclosingElement == null) {
					enclosingElement = createHandle(field,  parent);
				}
				if (encloses(enclosingElement)) {
					for (int i = 0, l = nodes.length; i < l; i++) {
						ASTNode node = nodes[i];
						Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
						// Set block scope for initializer in case there would have other local and other elements to report
						BlockScope blockScope = null;
						this.patternLocator.matchReportReference(node, enclosingElement, field.binding, blockScope, level.intValue(), this);
					}
				}
			}
		}
	}
}


/**
 * Visit the given type declaration and report the nodes that match exactly the
 * search pattern (ie. the ones in the matching nodes set)
 */
protected void reportMatching(TypeDeclaration type, IJavaScriptElement parent, int accuracy, MatchingNodeSet nodeSet, int occurrenceCount) throws CoreException {
	// create type handle
	IJavaScriptElement enclosingElement = parent;
	if (enclosingElement == null) {
		enclosingElement = createTypeHandle(new String(type.name));
	} else if (enclosingElement instanceof IType) {
		enclosingElement = ((IType) parent).getType(new String(type.name));
	} else if (enclosingElement instanceof IMember) {
	    IMember member = (IMember) parent;
		if (member.isBinary())  {
			enclosingElement = ((IClassFile)this.currentPossibleMatch.openable).getType();
		} else {
			enclosingElement = member.getType(new String(type.name), occurrenceCount);
		}
	}
	if (enclosingElement == null) return;
	boolean enclosesElement = encloses(enclosingElement);

	// report the type declaration
	if (accuracy > -1 && enclosesElement) {
		int offset = type.sourceStart;
		SearchMatch match = this.patternLocator.newDeclarationMatch(type, enclosingElement, type.binding, accuracy, type.sourceEnd-offset+1, this);
		report(match);
	}

	boolean matchedClassContainer = (this.matchContainer & PatternLocator.CLASS_CONTAINER) != 0;

	// report references in javadoc
	if (type.javadoc != null) {
		ASTNode[] nodes = nodeSet.matchingNodes(type.declarationSourceStart, type.sourceStart);
		if (nodes != null) {
			if (!matchedClassContainer) {
				for (int i = 0, l = nodes.length; i < l; i++)
					nodeSet.matchingNodes.removeKey(nodes[i]);
			} else {
				for (int i = 0, l = nodes.length; i < l; i++) {
					ASTNode node = nodes[i];
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
					if (enclosesElement) {
						this.patternLocator.matchReportReference(node, enclosingElement, type.binding, level.intValue(), this);
					}
				}
			}
		}
	}

	// super types
	if ((type.bits & ASTNode.IsAnonymousType) != 0) {
		TypeReference superType =type.allocation.type;
		if (superType != null) {
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(superType);
			if (level != null && matchedClassContainer)
				this.patternLocator.matchReportReference(superType, enclosingElement, type.binding, level.intValue(), this);
		}
	} else {
		TypeReference superClass = type.superclass;
		if (superClass != null) {
			reportMatchingSuper(superClass, enclosingElement, type.binding, nodeSet, matchedClassContainer);
		}
	}

	// filter out element not in hierarchy scope
	boolean typeInHierarchy = type.binding == null || typeInHierarchy(type.binding);
	matchedClassContainer = matchedClassContainer && typeInHierarchy;

	// Visit fields
	FieldDeclaration[] fields = type.fields;
	if (fields != null) {
		if (nodeSet.matchingNodes.elementSize == 0) return;	// end as all matching nodes were reported
		FieldDeclaration[] otherFields = null;
		int first = -1;
		int length = fields.length;
		for (int i = 0; i < length; i++) {
			FieldDeclaration field = fields[i];
			boolean last = field.endPart2Position == 0 || field.declarationEnd == field.endPart2Position;
			// Store first index of multiple field declaration
			if (!last) {
				if (first == -1) {
					first = i;
				}
			}
			if (first >= 0) {
				// Store all multiple fields but first one for other elements
				if (i > first) {
					if (otherFields == null) {
						otherFields = new FieldDeclaration[length-i];
					}
					otherFields[i-1-first] = field;
				}
				// On last field, report match with all other elements
				if (last) {
					for (int j=first; j<=i; j++) {
						Integer level = (Integer) nodeSet.matchingNodes.removeKey(fields[j]);
						int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
						reportMatching(fields[j], otherFields, type, enclosingElement, value, typeInHierarchy, nodeSet);
					}
					first = -1;
					otherFields = null;
				}
			} else {
				// Single field, report normally
				Integer level = (Integer) nodeSet.matchingNodes.removeKey(field);
				int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
				reportMatching(field, null, type, enclosingElement, value, typeInHierarchy, nodeSet);
			}
		}
	}

	// Visit methods
	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		if (nodeSet.matchingNodes.elementSize == 0) return;	// end as all matching nodes were reported
		for (int i = 0, l = methods.length; i < l; i++) {
			AbstractMethodDeclaration method = methods[i];
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(method);
			int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
			reportMatching(type, method, enclosingElement, value, typeInHierarchy, nodeSet);
		}
	}

	// Visit types
	TypeDeclaration[] memberTypes = type.memberTypes;
	if (memberTypes != null) {
		for (int i = 0, l = memberTypes.length; i < l; i++) {
			if (nodeSet.matchingNodes.elementSize == 0) return;	// end as all matching nodes were reported
			TypeDeclaration memberType = memberTypes[i];
			Integer level = (Integer) nodeSet.matchingNodes.removeKey(memberType);
			int value = (level != null && matchedClassContainer) ? level.intValue() : -1;
			reportMatching(memberType, enclosingElement, value, nodeSet, 1);
		}
	}
}
protected void reportMatchingSuper(TypeReference superReference, IJavaScriptElement enclosingElement, Binding elementBinding, MatchingNodeSet nodeSet, boolean matchedClassContainer) throws CoreException {
	ASTNode[] nodes = null;
	if (nodes != null) {
		if ((this.matchContainer & PatternLocator.CLASS_CONTAINER) == 0) {
			for (int i = 0, l = nodes.length; i < l; i++)
				nodeSet.matchingNodes.removeKey(nodes[i]);
		} else {
			if (encloses(enclosingElement))
				for (int i = 0, l = nodes.length; i < l; i++) {
					ASTNode node = nodes[i];
					Integer level = (Integer) nodeSet.matchingNodes.removeKey(node);
					this.patternLocator.matchReportReference(node, enclosingElement, elementBinding, level.intValue(), this);
				}
		}
	} else {
		Integer level = (Integer) nodeSet.matchingNodes.removeKey(superReference);
		if (level != null && matchedClassContainer)
			this.patternLocator.matchReportReference(superReference, enclosingElement, elementBinding, level.intValue(), this);
	}
}
protected boolean typeInHierarchy(ReferenceBinding binding) {
	if (this.hierarchyResolver == null) return true; // not a hierarchy scope
	if (this.hierarchyResolver.subOrSuperOfFocus(binding)) return true;

	if (this.allSuperTypeNames != null) {
		char[][] compoundName = binding.compoundName;
		for (int i = 0, length = this.allSuperTypeNames.length; i < length; i++)
			if (CharOperation.equals(compoundName, this.allSuperTypeNames[i]))
				return true;
	}
	return false;
}

public CompilationUnitDeclaration doParse(ICompilationUnit unit, AccessRestriction accessRestriction) {
	CompilationResult unitResult =
		new CompilationResult(unit, 1, 1, this.options.maxProblemsPerUnit);
	try {
		Parser parser=basicParser();
		CompilationUnitDeclaration parsedUnit = parser.parse(unit, unitResult);
		parser.inferTypes(parsedUnit,this.options);
		return parsedUnit;
	} catch (AbortCompilationUnit e) {
//		// at this point, currentCompilationUnitResult may not be sourceUnit, but some other
//		// one requested further along to resolve sourceUnit.
//		if (unitResult.compilationUnit == sourceUnit) { // only report once
//			requestor.acceptResult(unitResult.tagAsAccepted());
//		} else {
			throw e; // want to abort enclosing request to compile
//		}
	}

}
}
