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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.SourceType;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.wst.jsdt.internal.core.search.PathCollector;
import org.eclipse.wst.jsdt.internal.core.search.PatternSearchJob;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IndexManager;
import org.eclipse.wst.jsdt.internal.core.util.ASTNodeFinder;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Collects the super type names of a given declaring type.
 * Returns NOT_FOUND_DECLARING_TYPE if the declaring type was not found.
 * Returns null if the declaring type pattern doesn't require an exact match.
 */
public class SuperTypeNamesCollector {

	/**
	 * An ast visitor that visits type declarations and member type declarations
	 * collecting their super type names.
	 */
	public class TypeDeclarationVisitor extends ASTVisitor {
		public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
			ReferenceBinding binding = typeDeclaration.binding;
			if (SuperTypeNamesCollector.this.matches(binding))
				SuperTypeNamesCollector.this.collectSuperTypeNames(binding);
			return true;
		}
		public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
			ReferenceBinding binding = typeDeclaration.binding;
			if (SuperTypeNamesCollector.this.matches(binding))
				SuperTypeNamesCollector.this.collectSuperTypeNames(binding);
			return true;
		}
		public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
			ReferenceBinding binding = memberTypeDeclaration.binding;
			if (SuperTypeNamesCollector.this.matches(binding))
				SuperTypeNamesCollector.this.collectSuperTypeNames(binding);
			return true;
		}
		public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
			return false; // don't visit field declarations
		}
		public boolean visit(Initializer initializer, MethodScope scope) {
			return false; // don't visit initializers
		}
		public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
			return false; // don't visit constructor declarations
		}
		public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
			return false; // don't visit method declarations
		}
	}
SearchPattern pattern;
char[] typeSimpleName;
char[] typeQualification;
MatchLocator locator;
IType type;
IProgressMonitor progressMonitor;
char[][][] result;
int resultIndex;

public SuperTypeNamesCollector(
	SearchPattern pattern,
	char[] typeSimpleName,
	char[] typeQualification,
	MatchLocator locator,
	IType type,
	IProgressMonitor progressMonitor) {

	this.pattern = pattern;
	this.typeSimpleName = typeSimpleName;
	this.typeQualification = typeQualification;
	this.locator = locator;
	this.type = type;
	this.progressMonitor = progressMonitor;
}

protected void addToResult(char[][] compoundName) {
	int resultLength = this.result.length;
	for (int i = 0; i < resultLength; i++)
		if (CharOperation.equals(this.result[i], compoundName)) return; // already known

	if (resultLength == this.resultIndex)
		System.arraycopy(this.result, 0, this.result = new char[resultLength*2][][], 0, resultLength);
	this.result[this.resultIndex++] = compoundName;
}
/*
 * Parse the given compiation unit and build its type bindings.
 */
protected CompilationUnitDeclaration buildBindings(IJavaScriptUnit compilationUnit, boolean isTopLevelOrMember) throws JavaScriptModelException {
	// source unit
	org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit = (org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit) compilationUnit;

	CompilationResult compilationResult = new CompilationResult(sourceUnit, 1, 1, 0);
	CompilationUnitDeclaration unit =
		isTopLevelOrMember ?
			this.locator.basicParser().dietParse(sourceUnit, compilationResult) :
			this.locator.basicParser().parse(sourceUnit, compilationResult);
	if (unit != null) {
		this.locator.lookupEnvironment.buildTypeBindings(unit, null /*no access restriction*/);
		this.locator.lookupEnvironment.completeTypeBindings(unit, !isTopLevelOrMember);
		if (!isTopLevelOrMember) {
			if (unit.scope != null)
				unit.scope.faultInTypes(); // fault in fields & methods
			unit.resolve();
		}
	}
	return unit;
}
public char[][][] collect() throws JavaScriptModelException {
	if (this.type != null) {
		// Collect the paths of the cus that are in the hierarchy of the given type
		this.result = new char[1][][];
		this.resultIndex = 0;
		JavaProject javaProject = (JavaProject) this.type.getJavaScriptProject();
		this.locator.initialize(javaProject, 0);
		try {
			if (this.type.isBinary()) {
				BinaryTypeBinding binding = this.locator.cacheBinaryType(this.type, null);
				if (binding != null)
					collectSuperTypeNames(binding);
			} else {
				IJavaScriptUnit unit = this.type.getJavaScriptUnit();
				SourceType sourceType = (SourceType) this.type;
				boolean isTopLevelOrMember = sourceType.getOuterMostLocalContext() == null;
				CompilationUnitDeclaration parsedUnit = buildBindings(unit, isTopLevelOrMember);
				if (parsedUnit != null) {
					ASTNodeFinder nodeFinder = new ASTNodeFinder(parsedUnit);
					InferredType inferredType=nodeFinder.findInferredType(this.type);
					if (inferredType!=null)
						collectSuperTypeNames(inferredType.binding);
					else
					{
						TypeDeclaration typeDecl = nodeFinder.findType(this.type);
						if (typeDecl != null && typeDecl.binding != null)
							collectSuperTypeNames(typeDecl.binding);
					}
				}
			}
		} catch (AbortCompilation e) {
			// problem with classpath: report inacurrate matches
			return null;
		}
		if (this.result.length > this.resultIndex)
			System.arraycopy(this.result, 0, this.result = new char[this.resultIndex][][], 0, this.resultIndex);
		return this.result;
	}

	// Collect the paths of the cus that declare a type which matches declaringQualification + declaringSimpleName
	String[] paths = this.getPathsOfDeclaringType();
	if (paths == null) return null;

	// Create bindings from source types and binary types and collect super type names of the type declaration
	// that match the given declaring type
	Util.sort(paths); // sort by projects
	JavaProject previousProject = null;
	this.result = new char[1][][];
	this.resultIndex = 0;
	for (int i = 0, length = paths.length; i < length; i++) {
		try {
			Openable openable = this.locator.handleFactory.createOpenable(paths[i], this.locator.scope);
			if (openable == null) continue; // outside classpath

			IJavaScriptProject project = openable.getJavaScriptProject();
			if (!project.equals(previousProject)) {
				previousProject = (JavaProject) project;
				this.locator.initialize(previousProject, 0);
			}
			if (openable instanceof IJavaScriptUnit) {
				IJavaScriptUnit unit = (IJavaScriptUnit) openable;
				CompilationUnitDeclaration parsedUnit = buildBindings(unit, true /*only toplevel and member types are visible to the focus type*/);
				if (parsedUnit != null)
					parsedUnit.traverse(new TypeDeclarationVisitor(), parsedUnit.scope);
			} else if (openable instanceof IClassFile) {
				IClassFile classFile = (IClassFile) openable;
				BinaryTypeBinding binding = this.locator.cacheBinaryType(classFile.getType(), null);
				if (matches(binding))
					collectSuperTypeNames(binding);
			}
		} catch (AbortCompilation e) {
			// ignore: continue with next element
		} catch (JavaScriptModelException e) {
			// ignore: continue with next element
		}
	}
	if (this.result.length > this.resultIndex)
		System.arraycopy(this.result, 0, this.result = new char[this.resultIndex][][], 0, this.resultIndex);
	return this.result;
}
/**
 * Collects the names of all the supertypes of the given type.
 */
protected void collectSuperTypeNames(ReferenceBinding binding) {
	ReferenceBinding superclass = binding.getSuperBinding();
	if (superclass != null) {
		this.addToResult(superclass.compoundName);
		this.collectSuperTypeNames(superclass);
	}
}
protected String[] getPathsOfDeclaringType() {
	if (this.typeQualification == null && this.typeSimpleName == null) return null;

	final PathCollector pathCollector = new PathCollector();
	IJavaScriptSearchScope scope = SearchEngine.createWorkspaceScope();
	IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
	SearchPattern searchPattern = new TypeDeclarationPattern(
		this.typeQualification,
		this.typeSimpleName,
		this.pattern.getMatchRule());
	IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
		public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
			TypeDeclarationPattern record = (TypeDeclarationPattern)indexRecord;
			if (record.enclosingTypeNames != IIndexConstants.ONE_ZERO_CHAR) {  // filter out local and anonymous classes
				pathCollector.acceptIndexMatch(documentPath, indexRecord, participant, access);
			}
			return true;
		}
	};

	indexManager.performConcurrentJob(
		new PatternSearchJob(
			searchPattern,
			new JavaSearchParticipant(),
			scope,
			searchRequestor),
		IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
		progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 100));
	return pathCollector.getPaths();
}
protected boolean matches(char[][] compoundName) {
	int length = compoundName.length;
	if (length == 0) return false;
	char[] simpleName = compoundName[length-1];
	int last = length - 1;
	if (this.typeSimpleName == null || this.pattern.matchesName(simpleName, this.typeSimpleName)) {
		// most frequent case: simple name equals last segment of compoundName
		char[][] qualification = new char[last][];
		System.arraycopy(compoundName, 0, qualification, 0, last);
		return this.pattern.matchesName(this.typeQualification, CharOperation.concatWith(qualification, '.'));
	}

	return false;
}
protected boolean matches(ReferenceBinding binding) {
	return binding != null && binding.compoundName != null && this.matches(binding.compoundName);
}
}
