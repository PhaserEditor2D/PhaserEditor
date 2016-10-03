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
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;

/**
 * This class is used to find references to constructors.
 */
class ConstructorReferenceFinder {
	private final IType fType;
	private final IFunction[] fConstructors;

	private ConstructorReferenceFinder(IType type) throws JavaScriptModelException{
		fConstructors= JavaElementUtil.getAllConstructors(type);
		fType= type;
	}

	private ConstructorReferenceFinder(IFunction constructor){
		fConstructors= new IFunction[]{constructor};
		fType= constructor.getDeclaringType();
	}

	public static SearchResultGroup[] getConstructorReferences(IType type, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException{
		return new ConstructorReferenceFinder(type).getConstructorReferences(pm, null, IJavaScriptSearchConstants.REFERENCES, status);
	}

	public static SearchResultGroup[] getConstructorReferences(IType type, WorkingCopyOwner owner, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException{
		return new ConstructorReferenceFinder(type).getConstructorReferences(pm, owner, IJavaScriptSearchConstants.REFERENCES, status);
	}

	public static SearchResultGroup[] getConstructorOccurrences(IFunction constructor, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException{
		Assert.isTrue(constructor.isConstructor());
		return new ConstructorReferenceFinder(constructor).getConstructorReferences(pm, null, IJavaScriptSearchConstants.ALL_OCCURRENCES, status);
	}

	private SearchResultGroup[] getConstructorReferences(IProgressMonitor pm, WorkingCopyOwner owner, int limitTo, RefactoringStatus status) throws JavaScriptModelException{
		IJavaScriptSearchScope scope= createSearchScope();
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(fConstructors, limitTo);
		if (pattern == null){
			if (fConstructors.length != 0)
				return new SearchResultGroup[0];
			return getImplicitConstructorReferences(pm, owner, status);	
		}	
		return removeUnrealReferences(RefactoringSearchEngine.search(pattern, owner, scope, pm, status));
	}
	
	//XXX this method is a workaround for jdt core bug 27236
	private SearchResultGroup[] removeUnrealReferences(SearchResultGroup[] groups) {
		List result= new ArrayList(groups.length);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			IJavaScriptUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			JavaScriptUnit cuNode= new RefactoringASTParser(AST.JLS3).parse(cu, false);
			SearchMatch[] allSearchResults= group.getSearchResults();
			List realConstructorReferences= new ArrayList(Arrays.asList(allSearchResults));
			for (int j= 0; j < allSearchResults.length; j++) {
				SearchMatch searchResult= allSearchResults[j];
				if (! isRealConstructorReferenceNode(ASTNodeSearchUtil.getAstNode(searchResult, cuNode)))
					realConstructorReferences.remove(searchResult);
			}
			if (! realConstructorReferences.isEmpty())
				result.add(new SearchResultGroup(group.getResource(), (SearchMatch[]) realConstructorReferences.toArray(new SearchMatch[realConstructorReferences.size()])));
		}
		return (SearchResultGroup[]) result.toArray(new SearchResultGroup[result.size()]);
	}
	
	//XXX this method is a workaround for jdt core bug 27236
	private boolean isRealConstructorReferenceNode(ASTNode node){
		String typeName= fConstructors[0].getDeclaringType().getElementName();
		if (node.getParent() instanceof AbstractTypeDeclaration
				&& ((AbstractTypeDeclaration) node.getParent()).getNameProperty().equals(node.getLocationInParent())) {
			//Example:
			//	class A{
			//	    A(){}
			//	}
			//	class B extends A {}
			//==> "B" is found as reference to A()
			return false;
		}
		if (node.getParent() instanceof FunctionDeclaration
				&& FunctionDeclaration.NAME_PROPERTY.equals(node.getLocationInParent())) {
			FunctionDeclaration md= (FunctionDeclaration)node.getParent();
			if (md.isConstructor() && ! md.getName().getIdentifier().equals(typeName)) {
				//Example:
				//	class A{
				//	    A(){}
				//	}
				//	class B extends A{
				//	    B(){}
				//	}
				//==> "B" in "B(){}" is found as reference to A()
				return false;
			}
		}
		return true;
	}
	
	private IJavaScriptSearchScope createSearchScope() throws JavaScriptModelException{
		if (fConstructors.length == 0)
			return RefactoringScopeFactory.create(fType);
		return RefactoringScopeFactory.create(getMostVisibleConstructor());
	}
	
	private IFunction getMostVisibleConstructor() throws JavaScriptModelException {
		Assert.isTrue(fConstructors.length > 0);
		IFunction candidate= fConstructors[0];
		int visibility= JdtFlags.getVisibilityCode(fConstructors[0]);
		for (int i= 1; i < fConstructors.length; i++) {
			IFunction constructor= fConstructors[i];
			if (JdtFlags.isHigherVisibility(JdtFlags.getVisibilityCode(constructor), visibility))
				candidate= constructor;
		}
		return candidate;
	}

	private SearchResultGroup[] getImplicitConstructorReferences(IProgressMonitor pm, WorkingCopyOwner owner, RefactoringStatus status) throws JavaScriptModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		List searchMatches= new ArrayList();
		searchMatches.addAll(getImplicitConstructorReferencesFromHierarchy(owner, new SubProgressMonitor(pm, 1)));
		searchMatches.addAll(getImplicitConstructorReferencesInClassCreations(owner, new SubProgressMonitor(pm, 1), status));
		pm.done();
		return RefactoringSearchEngine.groupByCu((SearchMatch[]) searchMatches.toArray(new SearchMatch[searchMatches.size()]), status);
	}
		
	//List of SearchResults
	private List getImplicitConstructorReferencesInClassCreations(WorkingCopyOwner owner, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		//XXX workaround for jdt core bug 23112
		SearchPattern pattern= SearchPattern.createPattern(fType, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		IJavaScriptSearchScope scope= RefactoringScopeFactory.create(fType);
		SearchResultGroup[] refs= RefactoringSearchEngine.search(pattern, owner, scope, pm, status);
		List result= new ArrayList();
		for (int i= 0; i < refs.length; i++) {
			SearchResultGroup group= refs[i];
			IJavaScriptUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			JavaScriptUnit cuNode= new RefactoringASTParser(AST.JLS3).parse(cu, false);
			SearchMatch[] results= group.getSearchResults();
			for (int j= 0; j < results.length; j++) {
				SearchMatch searchResult= results[j];
				ASTNode node= ASTNodeSearchUtil.getAstNode(searchResult, cuNode);
				if (isImplicitConstructorReferenceNodeInClassCreations(node))
					result.add(searchResult);
			}
		}
		return result;
	}

	public static boolean isImplicitConstructorReferenceNodeInClassCreations(ASTNode node) {
		if (node instanceof Type) {
			final ASTNode parent= node.getParent();
			if (parent instanceof ClassInstanceCreation) {
				return (node.equals(((ClassInstanceCreation) parent).getType()));
			}
		}
		return false;
	}

	//List of SearchResults
	private List getImplicitConstructorReferencesFromHierarchy(WorkingCopyOwner owner, IProgressMonitor pm) throws JavaScriptModelException{
		IType[] subTypes= getNonBinarySubtypes(owner, fType, pm);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			result.addAll(getAllSuperConstructorInvocations(subTypes[i]));
		}
		return result;
	}

	private static IType[] getNonBinarySubtypes(WorkingCopyOwner owner, IType type, IProgressMonitor monitor) throws JavaScriptModelException{
		ITypeHierarchy hierarchy= null;
		if (owner == null)
			hierarchy= type.newTypeHierarchy(monitor);
		else
			hierarchy= type.newSupertypeHierarchy(owner, monitor);
		IType[] subTypes= hierarchy.getAllSubtypes(type);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			if (! subTypes[i].isBinary()) {
				result.add(subTypes[i]);
			}
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	//Collection of SearchResults
	private static Collection getAllSuperConstructorInvocations(IType type) throws JavaScriptModelException {
		IFunction[] constructors= JavaElementUtil.getAllConstructors(type);
		JavaScriptUnit cuNode= new RefactoringASTParser(AST.JLS3).parse(type.getJavaScriptUnit(), false);
		List result= new ArrayList(constructors.length);
		for (int i= 0; i < constructors.length; i++) {
			ASTNode superCall= getSuperConstructorCallNode(constructors[i], cuNode);
			if (superCall != null)
				result.add(createSearchResult(superCall, constructors[i]));
		}
		return result;
	}

	private static SearchMatch createSearchResult(ASTNode superCall, IFunction constructor) {
		int start= superCall.getStartPosition();
		int end= ASTNodes.getInclusiveEnd(superCall); //TODO: why inclusive?
		IResource resource= constructor.getResource();
		return new SearchMatch(constructor, SearchMatch.A_ACCURATE, start, end - start,
				SearchEngine.getDefaultSearchParticipant(), resource);
	}

	private static SuperConstructorInvocation getSuperConstructorCallNode(IFunction constructor, JavaScriptUnit cuNode) throws JavaScriptModelException {
		Assert.isTrue(constructor.isConstructor());
		FunctionDeclaration constructorNode= ASTNodeSearchUtil.getMethodDeclarationNode(constructor, cuNode);
		Assert.isTrue(constructorNode.isConstructor());
		Block body= constructorNode.getBody();
		Assert.isNotNull(body);
		List statements= body.statements();
		if (! statements.isEmpty() && statements.get(0) instanceof SuperConstructorInvocation)
			return (SuperConstructorInvocation)statements.get(0);
		return null;
	}
}
