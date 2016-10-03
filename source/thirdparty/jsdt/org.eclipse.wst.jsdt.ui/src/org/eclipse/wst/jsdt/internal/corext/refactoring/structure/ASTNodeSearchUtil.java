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
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;

public class ASTNodeSearchUtil {

	private ASTNodeSearchUtil() {
		//no instance
	}

	public static ASTNode[] getAstNodes(SearchMatch[] searchResults, JavaScriptUnit cuNode) {
		List result= new ArrayList(searchResults.length);
		for (int i= 0; i < searchResults.length; i++) {
			ASTNode node= getAstNode(searchResults[i], cuNode);
			if (node != null)
				result.add(node);
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	public static ASTNode getAstNode(SearchMatch searchResult, JavaScriptUnit cuNode) {
		ASTNode selectedNode= getAstNode(cuNode, searchResult.getOffset(), searchResult.getLength());
		if (selectedNode == null)
			return null;
		if (selectedNode.getParent() == null)
			return null;
		return selectedNode;
	}

	public static ASTNode getAstNode(JavaScriptUnit cuNode, int start, int length){
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(start, length), true);
		cuNode.accept(analyzer);
		//XXX workaround for jdt core feature 23527
		ASTNode node= analyzer.getFirstSelectedNode();
		if (node == null && analyzer.getLastCoveringNode() instanceof SuperConstructorInvocation)
			node= analyzer.getLastCoveringNode().getParent();
		else if (node == null && analyzer.getLastCoveringNode() instanceof ConstructorInvocation)
			node= analyzer.getLastCoveringNode().getParent();
		
		if (node == null)	
			return null;
		
		ASTNode parentNode= node.getParent();

		if (parentNode instanceof FunctionDeclaration){
			FunctionDeclaration md= (FunctionDeclaration)parentNode;
			if (!(node instanceof SimpleName)
				&& md.isConstructor()
			    && md.getBody() != null
			    && md.getBody().statements().size() > 0 
			    &&(md.getBody().statements().get(0) instanceof ConstructorInvocation || md.getBody().statements().get(0) instanceof SuperConstructorInvocation)
			    &&((ASTNode)md.getBody().statements().get(0)).getLength() == length + 1)
			return (ASTNode)md.getBody().statements().get(0);
		}

		if (parentNode instanceof SuperConstructorInvocation){
			if (parentNode.getLength() == length + 1)
				return parentNode;
		}
		if (parentNode instanceof ConstructorInvocation){
			if (parentNode.getLength() == length + 1)
				return parentNode;
		}
		return node;
	}

	public static FunctionDeclaration getMethodDeclarationNode(IFunction iMethod, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (FunctionDeclaration)ASTNodes.getParent(getNameNode(iMethod, cuNode), FunctionDeclaration.class);
	}

	public static BodyDeclaration getMethodOrAnnotationTypeMemberDeclarationNode(IFunction iMethod, JavaScriptUnit cuNode) throws JavaScriptModelException {
//		if (JdtFlags.isAnnotation(iMethod.getDeclaringType()))
//			return getAnnotationTypeMemberDeclarationNode(iMethod, cuNode);
//		else
			return getMethodDeclarationNode(iMethod, cuNode);
	}

	public static VariableDeclarationFragment getFieldDeclarationFragmentNode(IField iField, JavaScriptUnit cuNode) throws JavaScriptModelException {
		ASTNode node= getNameNode(iField, cuNode);
		if (node instanceof VariableDeclarationFragment)
			return  (VariableDeclarationFragment)node;
		return (VariableDeclarationFragment)ASTNodes.getParent(node, VariableDeclarationFragment.class);
	}
		
	public static FieldDeclaration getFieldDeclarationNode(IField iField, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (FieldDeclaration) ASTNodes.getParent(getNameNode(iField, cuNode), FieldDeclaration.class);
	}


	public static BodyDeclaration getFieldOrEnumConstantDeclaration(IField iField, JavaScriptUnit cuNode) throws JavaScriptModelException {
			return getFieldDeclarationNode(iField, cuNode);
	}


	public static BodyDeclaration getBodyDeclarationNode(IMember iMember, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (BodyDeclaration) ASTNodes.getParent(getNameNode(iMember, cuNode), BodyDeclaration.class);
	}

	public static AbstractTypeDeclaration getAbstractTypeDeclarationNode(IType iType, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (AbstractTypeDeclaration) ASTNodes.getParent(getNameNode(iType, cuNode), AbstractTypeDeclaration.class);
	}

	public static TypeDeclaration getTypeDeclarationNode(IType iType, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (TypeDeclaration) ASTNodes.getParent(getNameNode(iType, cuNode), TypeDeclaration.class);
	}
	
	public static ClassInstanceCreation getClassInstanceCreationNode(IType iType, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (ClassInstanceCreation) ASTNodes.getParent(getNameNode(iType, cuNode), ClassInstanceCreation.class);
	}
	
	public static List getBodyDeclarationList(IType iType, JavaScriptUnit cuNode) throws JavaScriptModelException {
		if (iType.isAnonymous())
			return getClassInstanceCreationNode(iType, cuNode).getAnonymousClassDeclaration().bodyDeclarations();
		else
			return getAbstractTypeDeclarationNode(iType, cuNode).bodyDeclarations();
	}
	
	//returns an array because of the import container, which does not represent 1 node but many
	//for fields, it returns the whole declaration node
	public static ASTNode[] getDeclarationNodes(IJavaScriptElement element, JavaScriptUnit cuNode) throws JavaScriptModelException {
		switch(element.getElementType()){
			case IJavaScriptElement.FIELD:
				return new ASTNode[]{getFieldOrEnumConstantDeclaration((IField) element, cuNode)};
			case IJavaScriptElement.IMPORT_CONTAINER:
				return getImportNodes((IImportContainer)element, cuNode);
			case IJavaScriptElement.IMPORT_DECLARATION:
				return new ASTNode[]{getImportDeclarationNode((IImportDeclaration)element, cuNode)};
			case IJavaScriptElement.INITIALIZER:
				return new ASTNode[]{getInitializerNode((IInitializer)element, cuNode)};
			case IJavaScriptElement.METHOD:
				return new ASTNode[]{getMethodOrAnnotationTypeMemberDeclarationNode((IFunction) element, cuNode)};
			case IJavaScriptElement.TYPE:
				return new ASTNode[]{getAbstractTypeDeclarationNode((IType) element, cuNode)};
			default:
				Assert.isTrue(false, String.valueOf(element.getElementType()));
				return null;
		}
	}

	private static ASTNode getNameNode(IMember iMember, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return NodeFinder.perform(cuNode, iMember.getNameRange());
	}

	public static ImportDeclaration getImportDeclarationNode(IImportDeclaration reference, JavaScriptUnit cuNode) throws JavaScriptModelException {
		return (ImportDeclaration) findNode(reference.getSourceRange(), cuNode);
	}

	public static ASTNode[] getImportNodes(IImportContainer reference, JavaScriptUnit cuNode) throws JavaScriptModelException {
		IJavaScriptElement[] imps= reference.getChildren();
		ASTNode[] result= new ASTNode[imps.length];
		for (int i= 0; i < imps.length; i++) {
			result[i]= getImportDeclarationNode((IImportDeclaration)imps[i], cuNode);
		}
		return result;
	}

	public static Initializer getInitializerNode(IInitializer initializer, JavaScriptUnit cuNode) throws JavaScriptModelException {
		ASTNode node= findNode(initializer.getSourceRange(), cuNode);
		if (node instanceof Initializer)
			return (Initializer) node;
		if (node instanceof Block && node.getParent() instanceof Initializer)
			return (Initializer) node.getParent();
		return null;
	}
	
	private static ASTNode findNode(ISourceRange range, JavaScriptUnit cuNode){
		NodeFinder nodeFinder= new NodeFinder(range.getOffset(), range.getLength());
		cuNode.accept(nodeFinder);
		ASTNode coveredNode= nodeFinder.getCoveredNode();
		if (coveredNode != null)
			return coveredNode;
		else
			return nodeFinder.getCoveringNode();		
	}
	
	public static ASTNode[] findNodes(SearchMatch[] searchResults, JavaScriptUnit cuNode) {
		List result= new ArrayList(searchResults.length);
		for (int i= 0; i < searchResults.length; i++) {
			ASTNode node= findNode(searchResults[i], cuNode);
			if (node != null)
				result.add(node);
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	public static ASTNode findNode(SearchMatch searchResult, JavaScriptUnit cuNode) {
		ASTNode selectedNode= NodeFinder.perform(cuNode, searchResult.getOffset(), searchResult.getLength());
		if (selectedNode == null)
			return null;
		if (selectedNode.getParent() == null)
			return null;
		return selectedNode;
	}
}
