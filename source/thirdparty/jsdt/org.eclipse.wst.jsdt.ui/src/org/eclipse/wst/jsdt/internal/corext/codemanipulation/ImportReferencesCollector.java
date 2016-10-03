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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Region;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class ImportReferencesCollector extends GenericVisitor {

	public static void collect(ASTNode node, IJavaScriptProject project, Region rangeLimit, Collection resultingTypeImports, Collection resultingStaticImports) {
		JavaScriptUnit astRoot= (JavaScriptUnit) node.getRoot();
		node.accept(new ImportReferencesCollector(project, astRoot, rangeLimit, resultingTypeImports, resultingStaticImports));
	}
	
	
	private JavaScriptUnit fASTRoot;
	private Region fSubRange;
	private Collection/*<Name>*/ fTypeImports;
	private Collection/*<Name>*/ fStaticImports;

	private ImportReferencesCollector(IJavaScriptProject project, JavaScriptUnit astRoot, Region rangeLimit, Collection resultingTypeImports, Collection resultingStaticImports) {
		super(true);
		fTypeImports= resultingTypeImports;
		fStaticImports= resultingStaticImports;
		fSubRange= rangeLimit;
		if (project == null || !JavaModelUtil.is50OrHigher(project)) {
			fStaticImports= null; // do not collect
		}
		fASTRoot= astRoot;
	}
	
	public ImportReferencesCollector(IJavaScriptProject project, Region rangeLimit, Collection resultingTypeImports, Collection resultingStaticImports) {
		this(project, null, rangeLimit, resultingTypeImports, resultingStaticImports);
	}
	
	public JavaScriptUnit getASTRoot(ASTNode node) {
		if (fASTRoot == null) {
			fASTRoot= (JavaScriptUnit) node.getRoot();
		}
		return fASTRoot;
	}
	
	private boolean isAffected(ASTNode node) {
		if (fSubRange == null) {
			return true;
		}
		int nodeStart= node.getStartPosition();
		int offset= fSubRange.getOffset();
		return nodeStart + node.getLength() > offset && (offset + fSubRange.getLength()) >  nodeStart;
	}
	
	
	private void addReference(SimpleName name) {
		if (isAffected(name)) {
			fTypeImports.add(name);
		}
	}			
	
	private void typeRefFound(Name node) {
		if (node != null) {
			while (node.isQualifiedName()) {
				node= ((QualifiedName) node).getQualifier();
			}
			addReference((SimpleName) node);
		}
	}

	private void possibleTypeRefFound(Name node) {
		while (node.isQualifiedName()) {
			node= ((QualifiedName) node).getQualifier();
		}
		IBinding binding= node.resolveBinding();
		if (binding == null || binding.getKind() == IBinding.TYPE) {
			// if the binding is null, we cannot determine if 
			// we have a type binding or not, so we will assume
			// we do.
			addReference((SimpleName) node);
		}
	}
	
	private void possibleStaticImportFound(Name name) {
		if (fStaticImports == null) {
			return;
		}
		
		while (name.isQualifiedName()) {
			name= ((QualifiedName) name).getQualifier();
		}
		if (!isAffected(name)) {
			return;
		}
		
		IBinding binding= name.resolveBinding();
		if (binding == null || binding instanceof ITypeBinding || !Modifier.isStatic(binding.getModifiers()) || ((SimpleName) name).isDeclaration()) {
			return;
		}
		
		if (binding instanceof IVariableBinding) {
			IVariableBinding varBinding= (IVariableBinding) binding;
			if (varBinding.isField()) {
				varBinding= varBinding.getVariableDeclaration();
				ITypeBinding declaringClass= varBinding.getDeclaringClass();
				if (declaringClass != null && !declaringClass.isLocal()) {
					if (new ScopeAnalyzer(getASTRoot(name)).isDeclaredInScope(varBinding, (SimpleName)name, ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY))
							return;
					fStaticImports.add(name);
				}
			}
		} else if (binding instanceof IFunctionBinding) {
			IFunctionBinding methodBinding= ((IFunctionBinding) binding).getMethodDeclaration();
			ITypeBinding declaringClass= methodBinding.getDeclaringClass();
			if (declaringClass != null && !declaringClass.isLocal()) {
				if (new ScopeAnalyzer(getASTRoot(name)).isDeclaredInScope(methodBinding, (SimpleName)name, ScopeAnalyzer.METHODS | ScopeAnalyzer.CHECK_VISIBILITY))
						return;
				fStaticImports.add(name);
			}
		}

	}
	
	private void doVisitChildren(List elements) {
		int nElements= elements.size();
		for (int i= 0; i < nElements; i++) {
			((ASTNode) elements.get(i)).accept(this);
		}
	}
	
	private void doVisitNode(ASTNode node) {
		if (node != null) {
			node.accept(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.wst.jsdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		return isAffected(node);
	}
	
	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		doVisitNode(node.getElementType());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		typeRefFound(node.getName());
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(QualifiedType)
	 */
	public boolean visit(QualifiedType node) {
		// nothing to do here, let the qualifier be visited
		return true;
	}
	
	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		possibleTypeRefFound(node); // possible ref
		possibleStaticImportFound(node);
		return false;
	}		

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		if (node.getAST().apiLevel() >= AST.JLS3) {
			doVisitChildren(node.annotations());
		}
		return false;
	}				

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		typeRefFound(node.getQualifier());
		return false;
	}

	private void evalQualifyingExpression(Expression expr, Name selector) {
		if (expr != null) {
			if (expr instanceof Name) {
				Name name= (Name) expr;
				possibleTypeRefFound(name);
				possibleStaticImportFound(name);
			} else {
				expr.accept(this);
			}
		} else if (selector != null) {
			possibleStaticImportFound(selector);
		}
	}			

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		doVisitChildren(node.typeArguments());
		doVisitNode(node.getType());
		evalQualifyingExpression(node.getExpression(), null);
		if (node.getAnonymousClassDeclaration() != null) {
			node.getAnonymousClassDeclaration().accept(this);
		}
		doVisitChildren(node.arguments());
		return false;
	}

	/*
	 * @see ASTVisitor#endVisit(FunctionInvocation)
	 */
	public boolean visit(FunctionInvocation node) {
		evalQualifyingExpression(node.getExpression(), node.getName());
		doVisitChildren(node.typeArguments());
		doVisitChildren(node.arguments());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */		
	public boolean visit(SuperConstructorInvocation node) {
		if (!isAffected(node)) {
			return false;
		}
		
		evalQualifyingExpression(node.getExpression(), null);
		doVisitChildren(node.typeArguments());
		doVisitChildren(node.arguments());
		return false;	
	}		

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		evalQualifyingExpression(node.getExpression(), node.getName());
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		// if the call gets here, it can only be a variable reference
		possibleStaticImportFound(node);
		return false;
	}
	


	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (!isAffected(node)) {
			return false;
		}
		return true;
	}
	
	/*
	 * @see ASTVisitor#visit(FunctionDeclaration)
	 */
	public boolean visit(FunctionDeclaration node) {
		if (!isAffected(node)) {
			return false;
		}
		doVisitNode(node.getJavadoc());
		
		if (node.getAST().apiLevel() >= AST.JLS3) {
			doVisitChildren(node.modifiers());
		}
		
		if (!node.isConstructor()) {
			doVisitNode(node.getReturnType2());
		}
		doVisitChildren(node.parameters());
		Iterator iter=node.thrownExceptions().iterator();
		while (iter.hasNext()) {
			typeRefFound((Name) iter.next());
		}
		doVisitNode(node.getBody());
		return false;
	}
	
	public boolean visit(TagElement node) {
		String tagName= node.getTagName();
		List list= node.fragments();
		int idx= 0;
		if (tagName != null && !list.isEmpty()) {
			Object first= list.get(0);
			if (first instanceof Name) {
				if ("@throws".equals(tagName) || "@exception".equals(tagName)) {  //$NON-NLS-1$//$NON-NLS-2$
					typeRefFound((Name) first);
				} else if ("@see".equals(tagName) || "@link".equals(tagName) || "@linkplain".equals(tagName)) {  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					Name name= (Name) first;
					possibleTypeRefFound(name);
				}
				idx++;
			}
		}
		for (int i= idx; i < list.size(); i++) {
			doVisitNode((ASTNode) list.get(i));
		}
		return false;
	}
	
	public boolean visit(MemberRef node) {
		Name qualifier= node.getQualifier();
		if (qualifier != null) {
			typeRefFound(qualifier);
		}
		return false;
	}
	
	public boolean visit(FunctionRef node) {
		Name qualifier= node.getQualifier();
		if (qualifier != null) {
			typeRefFound(qualifier);
		}
		List list= node.parameters();
		if (list != null) {
			doVisitChildren(list); // visit FunctionRefParameter with Type
		}
		return false;
	}
}
