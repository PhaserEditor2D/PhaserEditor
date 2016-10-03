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
package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.search.ui.text.Match;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTMatcher;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;

public class ExceptionOccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {

	public static final String IS_EXCEPTION= "isException"; //$NON-NLS-1$
	
	private AST fAST;
	private Name fSelectedName;
	
	private ITypeBinding fException;
	private ASTNode fStart;
	private List fResult;
	
	public ExceptionOccurrencesFinder() {
		fResult= new ArrayList();
	}
	
	public String initialize(JavaScriptUnit root, int offset, int length) {
		return initialize(root, NodeFinder.perform(root, offset, length));
	}
	
	public String initialize(JavaScriptUnit root, ASTNode node) {
		fAST= root.getAST();
		if (!(node instanceof Name)) {
			return SearchMessages.ExceptionOccurrencesFinder_no_exception;  
		}
		fSelectedName= ASTNodes.getTopMostName((Name)node);
		ASTNode parent= fSelectedName.getParent();
		FunctionDeclaration decl= resolveMethodDeclaration(parent);
		if (decl != null && methodThrowsException(decl, fSelectedName)) {
			fException= fSelectedName.resolveTypeBinding();
			fStart= decl.getBody();
		} else if (parent instanceof Type) {
			parent= parent.getParent();
			if (parent instanceof SingleVariableDeclaration && parent.getParent() instanceof CatchClause) {
				CatchClause catchClause= (CatchClause)parent.getParent();
				TryStatement tryStatement= (TryStatement)catchClause.getParent();
				if (tryStatement != null) {
					IVariableBinding var= catchClause.getException().resolveBinding();
					if (var != null && var.getType() != null) {
						fException= var.getType();
						fStart= tryStatement.getBody();
					}
				}
			}
		}
		if (fException == null || fStart == null)
			return SearchMessages.ExceptionOccurrencesFinder_no_exception;  
		return null;
	}
	
	private FunctionDeclaration resolveMethodDeclaration(ASTNode node) {
		if (node instanceof FunctionDeclaration)
			return (FunctionDeclaration)node;
		JSdoc doc= (JSdoc) ASTNodes.getParent(node, ASTNode.JSDOC);
		if (doc == null)
			return null;
		if (doc.getParent() instanceof FunctionDeclaration)
			return (FunctionDeclaration) doc.getParent();
		return null;
	}
	
	private boolean methodThrowsException(FunctionDeclaration method, Name exception) {
		ASTMatcher matcher = new ASTMatcher();
		for (Iterator iter = method.thrownExceptions().iterator(); iter.hasNext();) {
			Name thrown = (Name)iter.next();
			if (exception.subtreeMatch(matcher, thrown))
				return true;
		}
		return false;
	}
	
	public List perform() {
		fStart.accept(this);
		if (fSelectedName != null) {
			fResult.add(fSelectedName);
		}
		return fResult;
	}
	
	public void collectOccurrenceMatches(IJavaScriptElement element, IDocument document, Collection resultingMatches) {
		HashMap lineToLineElement= new HashMap();
		
		for (Iterator iter= fResult.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			int startPosition= node.getStartPosition();
			int length= node.getLength();
			try {
				boolean isException= node == fSelectedName;
				int line= document.getLineOfOffset(startPosition);
				Integer lineInteger= Integer.valueOf(line);
				ExceptionOccurrencesGroupKey groupKey= (ExceptionOccurrencesGroupKey) lineToLineElement.get(lineInteger);
				if (groupKey == null) {
					IRegion region= document.getLineInformation(line);
					String lineContents= document.get(region.getOffset(), region.getLength()).trim();
					groupKey= new ExceptionOccurrencesGroupKey(element, line, lineContents, isException);
					lineToLineElement.put(lineInteger, groupKey);
				} else if (isException) {
					// the line with the target exception always has the exception icon:
					groupKey.setException(true);
				}
				Match match= new Match(groupKey, startPosition, length);
				resultingMatches.add(match);
			} catch (BadLocationException e) {
				//nothing
			}
		}
	}
		
	public String getJobLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_searchfor ; 
	}
	
	public String getElementName() {
		if (fSelectedName != null) {
			return ASTNodes.asString(fSelectedName);
		}
		return null;
	}
	
	public String getUnformattedPluralLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_label_plural;
	}
	
	public String getUnformattedSingularLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_label_singular;
	}
	
	public boolean visit(AnonymousClassDeclaration node) {
		return false;
	}
	
	public boolean visit(ClassInstanceCreation node) {
		if (matches(node.resolveConstructorBinding())) {
			fResult.add(node.getType());
		}
		return super.visit(node);
	}
	
	public boolean visit(ConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			// mark this
			SimpleName name= fAST.newSimpleName("xxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 4);
			fResult.add(name);
		}
		return super.visit(node);
	}
	
	public boolean visit(FunctionInvocation node) {
		if (matches(node.resolveMethodBinding()))
			fResult.add(node.getName());
		return super.visit(node);
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			SimpleName name= fAST.newSimpleName("xxxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 5);
			fResult.add(name);
		}
		return super.visit(node);
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if (matches(node.resolveMethodBinding())) {
			fResult.add(node.getName());
		}
		return super.visit(node);
	}
	
	public boolean visit(ThrowStatement node) {
		if (matches(node.getExpression().resolveTypeBinding())) {
			SimpleName name= fAST.newSimpleName("xxxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 5);
			fResult.add(name);
			
		}
		return super.visit(node);
	}
	
	public boolean visit(TypeDeclarationStatement node) {
		// don't dive into local type declarations.
		return false;
	}
	
	private boolean matches(IFunctionBinding binding) {
		return false;
	}

	private boolean matches(ITypeBinding exception) {
		if (exception == null)
			return false;
		while (exception != null) {
			if (Bindings.equals(fException, exception))
				return true;
			exception= exception.getSuperclass();
		}
		return false;
	}


}
