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
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IPackageBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTFlattener;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;

/* package */ class MoveStaticMemberAnalyzer extends ASTVisitor {

	protected RefactoringStatus fStatus;
	
	protected ITypeBinding fSource;
	protected ITypeBinding fTarget;
	protected CompilationUnitRewrite fCuRewrite;
	protected IBinding[] fMembers;

	protected boolean fNeedsImport;

	protected Set fProcessed;
	
	protected static final String REFERENCE_UPDATE= RefactoringCoreMessages.MoveMembersRefactoring_referenceUpdate; 
	
	public MoveStaticMemberAnalyzer(CompilationUnitRewrite cuRewrite, IBinding[] members, ITypeBinding source, ITypeBinding target) {
		super(true);
		fStatus= new RefactoringStatus();
		fCuRewrite= cuRewrite;
		fMembers= members;
		fSource= source;
		fTarget= target;
		fProcessed= new HashSet();
	}
	
	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	protected boolean isProcessed(ASTNode node) {
		return fProcessed.contains(node);
	}
	
	protected void rewrite(SimpleName node, ITypeBinding type) {
		AST ast= node.getAST();
		Type result= fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST());
		fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
		Name dummy= ASTNodeFactory.newName(fCuRewrite.getAST(), ASTFlattener.asString(result));
		QualifiedName name= ast.newQualifiedName(dummy, ast.newSimpleName(node.getIdentifier()));
		fCuRewrite.getASTRewrite().replace(node, name, fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
		fCuRewrite.getImportRemover().registerRemovedNode(node);
		fProcessed.add(node);
		fNeedsImport= true;
	}
	
	protected void rewrite(QualifiedName node, ITypeBinding type) {
		rewriteName(node.getQualifier(), type);
		fProcessed.add(node.getName());
	}
	
	protected void rewrite(FieldAccess node, ITypeBinding type) {
		Expression exp= node.getExpression();
		if (exp == null) {
			Type result= fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST());
			fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
			exp= ASTNodeFactory.newName(fCuRewrite.getAST(), ASTFlattener.asString(result));
			fCuRewrite.getASTRewrite().set(node, FieldAccess.EXPRESSION_PROPERTY, exp,  fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
			fNeedsImport= true;
		} else if (exp instanceof Name) {
			rewriteName((Name)exp, type);
		} else {
			rewriteExpression(node, exp, type);
		}
		fProcessed.add(node.getName());
	}
	
	protected void rewrite(FunctionInvocation node, ITypeBinding type) {
		Expression exp= node.getExpression();
		if (exp == null) {
			Type result= fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST());
			fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
			exp= ASTNodeFactory.newName(fCuRewrite.getAST(), ASTFlattener.asString(result));
			fCuRewrite.getASTRewrite().set(node, FunctionInvocation.EXPRESSION_PROPERTY, exp, fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
			fNeedsImport= true;
		} else if (exp instanceof Name) {
			rewriteName((Name)exp, type);
		} else {
			rewriteExpression(node, exp, type);
		}
		fProcessed.add(node.getName());
	}
	
	protected void rewrite(MemberRef node, ITypeBinding type) {
		Name qualifier= node.getQualifier();
		if (qualifier == null) {
			Type result= fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST());
			fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
			qualifier= ASTNodeFactory.newName(fCuRewrite.getAST(), ASTFlattener.asString(result));
			fCuRewrite.getASTRewrite().set(node, MemberRef.QUALIFIER_PROPERTY, qualifier, fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
			fNeedsImport= true;
		} else {
			rewriteName(qualifier, type);
		}
		fProcessed.add(node.getName());
	}
	
	protected void rewrite(FunctionRef node, ITypeBinding type) {
		Name qualifier= node.getQualifier();
		if (qualifier == null) {
			Type result= fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST());
			fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
			qualifier= ASTNodeFactory.newName(fCuRewrite.getAST(), ASTFlattener.asString(result));
			fCuRewrite.getASTRewrite().set(node, FunctionRef.QUALIFIER_PROPERTY, qualifier, fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
			fNeedsImport= true;
		} else {
			rewriteName(qualifier, type);
		}
		fProcessed.add(node.getName());
	}

	private void rewriteName(Name name, ITypeBinding type) {
		AST creator= name.getAST();
		boolean fullyQualified= false;
		if (name instanceof QualifiedName) {
			SimpleName left= ASTNodes.getLeftMostSimpleName(name);
			if (left.resolveBinding() instanceof IPackageBinding)
				fullyQualified= true;
		}
		if (fullyQualified) {
			fCuRewrite.getASTRewrite().replace(
				name, 
				ASTNodeFactory.newName(creator, type.getQualifiedName()),
				fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
			fCuRewrite.getImportRemover().registerRemovedNode(name);
		} else {
			Type result= fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST());
			fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
			Name n= ASTNodeFactory.newName(fCuRewrite.getAST(), ASTFlattener.asString(result));
			fCuRewrite.getASTRewrite().replace(
				name, 
				n,
				fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
			fCuRewrite.getImportRemover().registerRemovedNode(name);
			fNeedsImport= true;
		}
	}
			
	private void rewriteExpression(ASTNode node, Expression exp, ITypeBinding type) {
		fCuRewrite.getASTRewrite().replace(exp, fCuRewrite.getImportRewrite().addImport(type, fCuRewrite.getAST()), fCuRewrite.createGroupDescription(REFERENCE_UPDATE));
		fCuRewrite.getImportRemover().registerAddedImport(type.getQualifiedName());
		fCuRewrite.getImportRemover().registerRemovedNode(exp);
		fNeedsImport= true;
		nonStaticAccess(node);
	}
	
	protected void nonStaticAccess(ASTNode node) {
		fStatus.addWarning(RefactoringCoreMessages.MoveStaticMemberAnalyzer_nonStatic,  
			JavaStatusContext.create(fCuRewrite.getCu(), node));
	}
	
	protected boolean isStaticAccess(Expression exp, ITypeBinding type) {
		if (!(exp instanceof Name))
			return false;
		return Bindings.equals(type, ((Name)exp).resolveBinding());
	} 
	
	protected boolean isMovedMember(IBinding binding) {
		if (binding == null)
			return false;
		for (int i= 0; i < fMembers.length; i++) {
			if (Bindings.equals(fMembers[i], binding))
				return true;
		}
		return false;
	}
}
