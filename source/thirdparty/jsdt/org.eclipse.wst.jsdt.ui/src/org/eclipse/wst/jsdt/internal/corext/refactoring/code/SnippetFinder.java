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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTMatcher;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;


/* package */ class SnippetFinder extends GenericVisitor {
	
	public static class Match {
		private List fNodes;
		private Map fLocalMappings;
		
		public Match() {
			fNodes= new ArrayList(10);
			fLocalMappings= new HashMap();
		}
		public void add(ASTNode node) {
			fNodes.add(node);
		}
		public boolean hasCorrectNesting(ASTNode node) {
			if (fNodes.size() == 0)
				return true;
			ASTNode parent= node.getParent();
			if(((ASTNode)fNodes.get(0)).getParent() != parent)
				return false;
			// Here we know that we have two elements. In this case the
			// parent must be a block or a switch statement. Otherwise a 
			// snippet like "if (true) foo(); else foo();" would match 
			// the pattern "foo(); foo();"
			int nodeType= parent.getNodeType();
			return nodeType == ASTNode.BLOCK || nodeType == ASTNode.SWITCH_STATEMENT;
		}
		public ASTNode[] getNodes() {
			return (ASTNode[])fNodes.toArray(new ASTNode[fNodes.size()]);
		}
		public void addLocal(IVariableBinding org, SimpleName local) {
			fLocalMappings.put(org, local);
		}
		public SimpleName getMappedName(IVariableBinding org) {
			return (SimpleName)fLocalMappings.get(org);
		}
		public IVariableBinding getMappedBinding(IVariableBinding org) {
			SimpleName name= (SimpleName) fLocalMappings.get(org);
			return ASTNodes.getVariableBinding(name);
		}
		public boolean isEmpty() {
			return fNodes.isEmpty() && fLocalMappings.isEmpty();
		}
		/**
		 * Tests if the whole duplicate is the full body of a method. If so
		 * don't replace it since we would replace a method body with a new
		 * method body which doesn't make to much sense.
		 * 
		 * @return whether the duplicte is the whole method body
		 */
		public boolean isMethodBody() {
			ASTNode first= (ASTNode)fNodes.get(0);
			if (first.getParent() == null)
				return false;
			ASTNode candidate= first.getParent().getParent();
			if (candidate == null || candidate.getNodeType() != ASTNode.FUNCTION_DECLARATION)
				return false;
			FunctionDeclaration method= (FunctionDeclaration)candidate;
			return method.getBody().statements().size() == fNodes.size();
		}
		public FunctionDeclaration getEnclosingMethod() {
			ASTNode first= (ASTNode)fNodes.get(0);
			return (FunctionDeclaration)ASTNodes.getParent(first, ASTNode.FUNCTION_DECLARATION);
		}
	}
	
	private class Matcher extends ASTMatcher {
		public boolean match(SimpleName candidate, Object s) {
			if (!(s instanceof SimpleName))
				return false;
				
			SimpleName snippet= (SimpleName)s;
			if (candidate.isDeclaration() != snippet.isDeclaration())
				return false;
			
			IBinding cb= candidate.resolveBinding();
			IBinding sb= snippet.resolveBinding();
			if (cb == null || sb == null)
				return false;
			IVariableBinding vcb= ASTNodes.getVariableBinding(candidate);
			IVariableBinding vsb= ASTNodes.getVariableBinding(snippet);
			if (vcb == null || vsb == null)
				return Bindings.equals(cb, sb);
			if (!vcb.isField() && !vsb.isField() && Bindings.equals(vcb.getType(), vsb.getType())) {
				SimpleName mapped= fMatch.getMappedName(vsb);
				if (mapped != null) {
					IVariableBinding mappedBinding= ASTNodes.getVariableBinding(mapped);
					if (!Bindings.equals(vcb, mappedBinding))
						return false;
				}
				fMatch.addLocal(vsb, candidate);
				return true;
			}
			return Bindings.equals(cb, sb);	
		}
	}

	private List fResult= new ArrayList(2);
	private Match fMatch;
	private ASTNode[] fSnippet;
	private int fIndex;
	private Matcher fMatcher;
	private int fTypes;
	
	private SnippetFinder(ASTNode[] snippet) {
		super(true);
		fSnippet= snippet;
		fMatcher= new Matcher();
		reset();
	}
	
	public static Match[] perform(ASTNode start, ASTNode[] snippet) {
		Assert.isTrue(start instanceof JavaScriptUnit || start instanceof AbstractTypeDeclaration || start instanceof AnonymousClassDeclaration);
		SnippetFinder finder= new SnippetFinder(snippet);
		start.accept(finder);
		for (Iterator iter = finder.fResult.iterator(); iter.hasNext();) {
			Match match = (Match)iter.next();
			ASTNode[] nodes= match.getNodes();
			// doesn't match if the candidate is the left hand side of an 
			// assignment and the snippet consists of a single node.
			// Otherwise y= i; i= z; results in y= e(); e()= z;
			if (nodes.length == 1 && isLeftHandSideOfAssignment(nodes[0])) {
				iter.remove();
			}
		}
		return (Match[])finder.fResult.toArray(new Match[finder.fResult.size()]);
	}
	
	private static boolean isLeftHandSideOfAssignment(ASTNode node) {
		ASTNode parent= node.getParent();
		return parent != null && parent.getNodeType() == ASTNode.ASSIGNMENT && ((Assignment)parent).getLeftHandSide() == node;
	}
	
	public boolean visit(TypeDeclaration node) {
		if (++fTypes > 1)
			return false;
		return super.visit(node);
	}
	
	public void endVisit(TypeDeclaration node) {
		--fTypes;
		super.endVisit(node);
	}
	
	protected boolean visitNode(ASTNode node) {
		if (matches(node)) {
			return false;
		} else if (!isResetted()){
			reset();
			if (matches(node))
				return false;
		}
		return true;
	}
	
	private boolean matches(ASTNode node) {
		if (isSnippetNode(node))
			return false;
		if (node.subtreeMatch(fMatcher, fSnippet[fIndex]) && fMatch.hasCorrectNesting(node)) {
			fMatch.add(node);
			fIndex++;
			if (fIndex == fSnippet.length) {
				fResult.add(fMatch);
				reset();
			}
			return true;
		}
		return false;
	}

	private boolean isResetted() {
		return fIndex == 0 && fMatch.isEmpty();
	}

	private void reset() {
		fIndex= 0;
		fMatch= new Match();
	}
	
	private boolean isSnippetNode(ASTNode node) {
		for (int i= 0; i < fSnippet.length; i++) {
			if (node == fSnippet[i])
				return true;
		}
		return false;
	}
}
