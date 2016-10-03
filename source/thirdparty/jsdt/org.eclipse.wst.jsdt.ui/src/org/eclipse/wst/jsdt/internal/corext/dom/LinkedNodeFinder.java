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
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.ArrayList;

import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;


/**
 * Find all nodes connected to a given binding or node. e.g. Declaration of a field and all references.
 * For types this includes also the constructor declaration, for methods also overridden methods
 * or methods overriding (if existing in the same AST)  

*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class LinkedNodeFinder  {
	
	private LinkedNodeFinder() {
	}
	
	
	/**
	 * Find all nodes connected to the given binding. e.g. Declaration of a field and all references.
	 * For types this includes also the constructor declaration, for methods also overridden methods
	 * or methods overriding (if existing in the same AST)
	 * @param root The root of the AST tree to search
	 * @param binding The binding of the searched nodes
	 * @return Return 
	 */
	public static SimpleName[] findByBinding(ASTNode root, IBinding binding) {
		ArrayList res= new ArrayList();
		BindingFinder nodeFinder= new BindingFinder(binding, res);
		root.accept(nodeFinder);
		return (SimpleName[]) res.toArray(new SimpleName[res.size()]);
	}
	
	/**
	 * Find all nodes connected to the given name node. If the node has a binding then all nodes connected
	 * to this binding are returned. If the node has no binding, then all nodes that also miss a binding and have
	 * the same name are returned.
	 * @param root The root of the AST tree to search
	 * @param name The node to find linked nodes for
	 * @return Return 
	 */
	public static SimpleName[] findByNode(ASTNode root, SimpleName name) { 
		IBinding binding = name.resolveBinding();
		if (binding != null) {
			return findByBinding(root, binding);
		}
		SimpleName[] names= findByProblems(root, name);
		if (names != null) {
			return names;
		}
		int parentKind= name.getParent().getNodeType();
		if (parentKind == ASTNode.LABELED_STATEMENT || parentKind == ASTNode.BREAK_STATEMENT || parentKind == ASTNode.CONTINUE_STATEMENT) {
			ArrayList res= new ArrayList();
			LabelFinder nodeFinder= new LabelFinder(name, res);
			root.accept(nodeFinder);
			return (SimpleName[]) res.toArray(new SimpleName[res.size()]);
		}
		return new SimpleName[] { name };
	}
	
	
	
	private static final int FIELD= 1;
	private static final int METHOD= 2;
	private static final int TYPE= 4;
	private static final int LABEL= 8;
	private static final int NAME= FIELD | TYPE;
	
	private static int getProblemKind(IProblem problem) {
		switch (problem.getID()) {
			case IProblem.UndefinedField:
				return FIELD;
			case IProblem.UndefinedMethod:
			case IProblem.UndefinedFunction:
				return METHOD;
			case IProblem.UndefinedLabel:
				return LABEL;
			case IProblem.UndefinedName:
				return NAME;
			case IProblem.UndefinedType:
				return TYPE;
		}
		return 0;
	}
	
	private static int getNameNodeProblemKind(IProblem[] problems, SimpleName nameNode) {
		int nameOffset= nameNode.getStartPosition();
		int nameInclEnd= nameOffset + nameNode.getLength() - 1;
		
		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			if (curr.getSourceStart() == nameOffset && curr.getSourceEnd() == nameInclEnd) {
				int kind= getProblemKind(curr);
				if (kind != 0) {
					return kind;
				}
			}
		}
		return 0;
	}
	
	
	public static SimpleName[] findByProblems(ASTNode parent, SimpleName nameNode) {
		ArrayList res= new ArrayList();
		
		ASTNode astRoot = parent.getRoot();
		if (!(astRoot instanceof JavaScriptUnit)) {
			return null;
		}
			
		IProblem[] problems= ((JavaScriptUnit) astRoot).getProblems();
		int nameNodeKind= getNameNodeProblemKind(problems, nameNode);
		if (nameNodeKind == 0) { // no problem on node
			return null;
		}
			
		int bodyStart= parent.getStartPosition();
		int bodyEnd= bodyStart + parent.getLength();
		
		String name= nameNode.getIdentifier();

		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			int probStart= curr.getSourceStart();
			int probEnd= curr.getSourceEnd() + 1;
			
			if (probStart > bodyStart && probEnd < bodyEnd) {
				int currKind= getProblemKind(curr);
				if ((nameNodeKind & currKind) != 0) {
					ASTNode node= NodeFinder.perform(parent, probStart, probEnd - probStart);
					if (node instanceof SimpleName && name.equals(((SimpleName) node).getIdentifier())) {
						res.add(node);
					}
				}
			}
		}
		return (SimpleName[]) res.toArray(new SimpleName[res.size()]);
	}
	
	private static class LabelFinder extends ASTVisitor {
		
		private SimpleName fLabel;
		private ASTNode fDefiningLabel;
		private ArrayList fResult;
		
		public LabelFinder(SimpleName label, ArrayList result) {
			super(true);
			fLabel= label;
			fResult= result;
			fDefiningLabel= null;
		}
		
		private boolean isSameLabel(SimpleName label) {
			return label != null && fLabel.getIdentifier().equals(label.getIdentifier());
		}
		
		public boolean visit(BreakStatement node) {
			SimpleName label= node.getLabel();
			if (fDefiningLabel != null && isSameLabel(label) && ASTNodes.isParent(label, fDefiningLabel)) {
				fResult.add(label);
			}
			return false;
		}
				
		public boolean visit(ContinueStatement node) {
			SimpleName label= node.getLabel();
			if (fDefiningLabel != null && isSameLabel(label) && ASTNodes.isParent(label, fDefiningLabel)) {
				fResult.add(label);
			}
			return false;
		}

		public boolean visit(LabeledStatement node) {
			if (fDefiningLabel == null) {
				SimpleName label= node.getLabel();
				if (fLabel == label || isSameLabel(label) && ASTNodes.isParent(fLabel, node)) {
					fDefiningLabel= node;
					fResult.add(label);
				}
			}
			node.getBody().accept(this);
			return false;
		}
	}
	
	private static class BindingFinder extends ASTVisitor {
	
		private IBinding fBinding;
		private ArrayList fResult;
		
		public BindingFinder(IBinding binding, ArrayList result) {
			super(true);
			fBinding= getDeclaration(binding);
			fResult= result;
		}
		
		public boolean visit(FunctionDeclaration node) {
			if (node.isConstructor() && fBinding.getKind() == IBinding.TYPE) {
				ASTNode typeNode= node.getParent();
				if (typeNode instanceof AbstractTypeDeclaration) {
					if (fBinding == ((AbstractTypeDeclaration) typeNode).resolveBinding()) {
						fResult.add(node.getName());
					}
				}
			}
			return true;
		}
		
		public boolean visit(TypeDeclaration node) {
			if (fBinding.getKind() == IBinding.METHOD) {
				IFunctionBinding binding= (IFunctionBinding) fBinding;
				if (binding.isConstructor() && binding.getDeclaringClass() == node.resolveBinding()) {
					fResult.add(node.getName());
				}
			}
			return true;
		}		


		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding == null || binding.getKind() != fBinding.getKind()) {
				return false;
			}
			binding= getDeclaration(binding);
			
			if (fBinding == binding) {
				fResult.add(node);
			} else if (binding.getKind() == IBinding.METHOD) {
				IFunctionBinding curr= (IFunctionBinding) binding;
				IFunctionBinding methodBinding= (IFunctionBinding) fBinding;
				if (methodBinding.overrides(curr) || curr.overrides(methodBinding)) {
					fResult.add(node);
				}
			}
			return false;
		}
		
		private static IBinding getDeclaration(IBinding binding) {
			if (binding instanceof ITypeBinding) {
				return ((ITypeBinding) binding).getTypeDeclaration();
			} else if (binding instanceof IFunctionBinding) {
				return ((IFunctionBinding) binding).getMethodDeclaration();
			} else if (binding instanceof IVariableBinding) {
				return ((IVariableBinding) binding).getVariableDeclaration();
			}
			return binding;
		}
	}
}
