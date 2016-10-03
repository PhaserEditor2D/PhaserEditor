/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.IRefactoringSearchRequestor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;

/**
 * A TargetProvider provides all targets that have to be adapted, i.e. all method invocations that should be inlined. 
 */
abstract class TargetProvider {

	public static final boolean BUG_CORE_130317= true; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=130317
	
	protected SourceProvider fSourceProvider;

	//TODO: not used...
	public void setSourceProvider(SourceProvider sourceProvider) {
		Assert.isNotNull(sourceProvider);
		fSourceProvider= sourceProvider;
	}

	public abstract void initialize();

	public abstract IJavaScriptUnit[] getAffectedCompilationUnits(RefactoringStatus status, IProgressMonitor pm)  throws JavaScriptModelException;
	
	public abstract BodyDeclaration[] getAffectedBodyDeclarations(IJavaScriptUnit unit, IProgressMonitor pm);
	
	// constructor invocation is not an expression but a statement
	public abstract ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm);
	
	public abstract RefactoringStatus checkActivation() throws JavaScriptModelException;
	
	public abstract int getStatusSeverity();
	
	public boolean isSingle() {
		return false;
	}
	
	public static TargetProvider create(IJavaScriptUnit cu, FunctionInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(IJavaScriptUnit cu, SuperMethodInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(IJavaScriptUnit cu, ConstructorInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(FunctionDeclaration declaration) {
		IFunctionBinding method= declaration.resolveBinding();
		if (method == null)
			return new ErrorTargetProvider(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.TargetProvider_method_declaration_not_unique));
		ITypeBinding type= method.getDeclaringClass();
		if (type.isLocal()) {
			if (((IType) type.getJavaElement()).isBinary()) {
				return new ErrorTargetProvider(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.TargetProvider_cannot_local_method_in_binary));
			} else {
				IType declaringClassOfLocal= (IType) type.getDeclaringClass().getJavaElement();
				return new LocalTypeTargetProvider(declaringClassOfLocal.getJavaScriptUnit(), declaration);
			}
		} else {
			return new MemberTypeTargetProvider(declaration.resolveBinding());
		}
	}

	public static TargetProvider create(IFunctionBinding methodBinding) {
		return new MemberTypeTargetProvider(methodBinding);
	}
	
	static void fastDone(IProgressMonitor pm) {
		if (pm == null)
			return;
		pm.beginTask("", 1); //$NON-NLS-1$
		pm.worked(1);
		pm.done();
	}
	
	static class ErrorTargetProvider extends TargetProvider {
		private RefactoringStatus fErrorStatus;
		public ErrorTargetProvider(RefactoringStatus status) {
			fErrorStatus= status;
		}
		public RefactoringStatus checkActivation() throws JavaScriptModelException {
			return fErrorStatus;
		}
		public void initialize() {
		}
		public IJavaScriptUnit[] getAffectedCompilationUnits(RefactoringStatus status, IProgressMonitor pm) throws JavaScriptModelException {
			return null;
		}
		public BodyDeclaration[] getAffectedBodyDeclarations(IJavaScriptUnit unit, IProgressMonitor pm) {
			return null;
		}
		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			return null;
		}
		public int getStatusSeverity() {
			return 0;
		}
	}
	
	static class SingleCallTargetProvider extends TargetProvider {
		private IJavaScriptUnit fCUnit;
		private ASTNode fInvocation;
		private boolean fIterated;
		public SingleCallTargetProvider(IJavaScriptUnit cu, ASTNode invocation) {
			Assert.isNotNull(cu);
			Assert.isNotNull(invocation);
			Assert.isTrue(Invocations.isInvocation(invocation));
			fCUnit= cu;
			fInvocation= invocation;
		}
		public void initialize() {
			fIterated= false;
		}
		public IJavaScriptUnit[] getAffectedCompilationUnits(RefactoringStatus status, IProgressMonitor pm) {
			return new IJavaScriptUnit[] { fCUnit };
		}
		public BodyDeclaration[] getAffectedBodyDeclarations(IJavaScriptUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			if (fIterated)
				return new BodyDeclaration[0];
			fastDone(pm);
			return new BodyDeclaration[] { 
				(BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class)
			};
		}
	
		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			fastDone(pm);
			if (fIterated)
				return null;
			fIterated= true;
			return new ASTNode[] { fInvocation };
		}
		public RefactoringStatus checkActivation() throws JavaScriptModelException {
			return new RefactoringStatus();
		}
		public int getStatusSeverity() {
			return RefactoringStatus.FATAL;
		}
		public boolean isSingle() {
			return true;
		}
	}

	private static class BodyData {
		public BodyDeclaration fBody;
		private List fInvocations;
		public BodyData(BodyDeclaration declaration) {
			fBody= declaration;
		}
		public void addInvocation(ASTNode node) {
			if (fInvocations == null)
				fInvocations= new ArrayList(2);
			fInvocations.add(node);
		}
		public ASTNode[] getInvocations() {
			return (ASTNode[])fInvocations.toArray(new ASTNode[fInvocations.size()]);
		}
		public boolean hasInvocations() {
			return fInvocations != null && !fInvocations.isEmpty();
		}
		public BodyDeclaration getDeclaration() {
			return fBody;
		}
	}

	private static class InvocationFinder extends ASTVisitor {
		Map/*<BodyDeclaration, BodyData>*/ result= new HashMap(2);
		Stack/*<BodyData>*/ fBodies= new Stack();
		BodyData fCurrent;
		private IFunctionBinding fBinding;
		public InvocationFinder(IFunctionBinding binding) {
			Assert.isNotNull(binding);
			fBinding= binding.getMethodDeclaration();
			Assert.isNotNull(fBinding);
		}
		public boolean visit(FunctionInvocation node) {
			if (matches(node.getName().resolveBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(SuperMethodInvocation node) {
			if (matches(node.getName().resolveBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(ConstructorInvocation node) {
			if (matches(node.resolveConstructorBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(ClassInstanceCreation node) {
			if (matches(node.resolveConstructorBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			return visitType();
		}
		public void endVisit(TypeDeclaration node) {
			endVisitType();
		}

		private boolean visitType() {
			fBodies.add(fCurrent);
			fCurrent= null;
			return true;
		}
		private void endVisitType() {
			fCurrent= (BodyData)fBodies.remove(fBodies.size() - 1);
		}
		public boolean visit(FieldDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData(node);
			return true;
		}
		public void endVisit(FieldDeclaration node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			endVisitType();
		}
		public boolean visit(FunctionDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData(node);
			return true;
		}
		public void endVisit(FunctionDeclaration node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			endVisitType();
			
		}
		public boolean visit(Initializer node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData(node);
			return true;
		}
		public void endVisit(Initializer node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			endVisitType();
		}
		private boolean matches(IBinding binding) {
			if (!(binding instanceof IFunctionBinding))
				return false;
			if (BUG_CORE_130317)
				return fBinding.getKey().equals(((IFunctionBinding)binding).getMethodDeclaration().getKey());
			else
				return fBinding.isEqualTo(((IFunctionBinding)binding).getMethodDeclaration());
		}
	}
	
	private static class LocalTypeTargetProvider extends TargetProvider {
		private IJavaScriptUnit fCUnit;
		private FunctionDeclaration fDeclaration;
		private Map fBodies;
		public LocalTypeTargetProvider(IJavaScriptUnit unit, FunctionDeclaration declaration) {
			Assert.isNotNull(unit);
			Assert.isNotNull(declaration);
			fCUnit= unit;
			fDeclaration= declaration;
		}
		public void initialize() {
			InvocationFinder finder= new InvocationFinder(fDeclaration.resolveBinding());
			ASTNode type= ASTNodes.getParent(fDeclaration, AbstractTypeDeclaration.class);
			type.accept(finder);
			fBodies= finder.result;
		}
		public IJavaScriptUnit[] getAffectedCompilationUnits(RefactoringStatus status, IProgressMonitor pm) {
			fastDone(pm);
			return new IJavaScriptUnit[] { fCUnit };
		}
	
		public BodyDeclaration[] getAffectedBodyDeclarations(IJavaScriptUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			Set result= fBodies.keySet();
			fastDone(pm);
			return (BodyDeclaration[])result.toArray(new BodyDeclaration[result.size()]);
		}
	
		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			BodyData data= (BodyData)fBodies.get(declaration);
			Assert.isNotNull(data);
			fastDone(pm);
			return data.getInvocations();
		}
	
		public RefactoringStatus checkActivation() throws JavaScriptModelException {
			return new RefactoringStatus();
		}
		
		public int getStatusSeverity() {
			return RefactoringStatus.ERROR;
		}
	}
	
	private static class MemberTypeTargetProvider extends TargetProvider {
		private final IFunctionBinding fMethodBinding;
		private Map fCurrentBodies;
		public MemberTypeTargetProvider(IFunctionBinding methodBinding) {
			Assert.isNotNull(methodBinding);
			fMethodBinding= methodBinding;
		}
		public void initialize() {
			// do nothing.
		}

		public IJavaScriptUnit[] getAffectedCompilationUnits(final RefactoringStatus status, IProgressMonitor pm) throws JavaScriptModelException {
			IFunction method= (IFunction)fMethodBinding.getJavaElement();
			Assert.isTrue(method != null);
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(method, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.setGranularity(RefactoringSearchEngine2.GRANULARITY_COMPILATION_UNIT);
			engine.setFiltering(true, true);
			engine.setScope(RefactoringScopeFactory.create(method));
			engine.setRequestor(new IRefactoringSearchRequestor() {
				public SearchMatch acceptSearchMatch(SearchMatch match) {
					if (match.isInsideDocComment())
						return null;
					if (match.getAccuracy() == SearchMatch.A_INACCURATE) {
						Object element= match.getElement();
						if (element instanceof IJavaScriptElement) {
							IJavaScriptElement jElement= (IJavaScriptElement)element;
							IJavaScriptUnit unit= (IJavaScriptUnit)jElement.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
							if (unit != null) {
								status.addError(RefactoringCoreMessages.TargetProvider_inaccurate_match,
									JavaStatusContext.create(unit, new SourceRange(match.getOffset(), match.getLength())));
								return null;
							}
						}
						status.addError(RefactoringCoreMessages.TargetProvider_inaccurate_match);
						return null;
					} else {
						return match;
					}
				}
			});
			engine.searchPattern(new SubProgressMonitor(pm, 1));
			return engine.getAffectedCompilationUnits();
		}

		public BodyDeclaration[] getAffectedBodyDeclarations(IJavaScriptUnit unit, IProgressMonitor pm) {
			ASTNode root= new RefactoringASTParser(AST.JLS3).parse(unit, true);
			InvocationFinder finder= new InvocationFinder(fMethodBinding);
			root.accept(finder);
			fCurrentBodies= finder.result;
			Set result= fCurrentBodies.keySet();
			fastDone(pm);
			return (BodyDeclaration[])result.toArray(new BodyDeclaration[result.size()]);
		}
	
		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			BodyData data= (BodyData)fCurrentBodies.get(declaration);
			Assert.isNotNull(data);
			fastDone(pm);
			return data.getInvocations();
		}
	
		public RefactoringStatus checkActivation() throws JavaScriptModelException {
			return new RefactoringStatus();
		}
		
		public int getStatusSeverity() {
			return RefactoringStatus.ERROR;
		}
	}
}
