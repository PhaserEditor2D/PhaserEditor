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
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression.Operator;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;

public class OccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {
	
	public static final String IS_WRITEACCESS= "writeAccess"; //$NON-NLS-1$
	public static final String IS_VARIABLE= "variable"; //$NON-NLS-1$
	
	private JavaScriptUnit fRoot;
	private Name fSelectedNode;
	private IBinding fTarget;
	private List fUsages= new ArrayList/*<ASTNode>*/();
	private List fWriteUsages= new ArrayList/*<ASTNode>*/();
	private boolean fTargetIsStaticMethodImport;

	public OccurrencesFinder(IBinding target) {
		super(true);
		fTarget= target;
	}
	
	public OccurrencesFinder() {
		super(true);
	}
	
	public String initialize(JavaScriptUnit root, int offset, int length) {
		return initialize(root, NodeFinder.perform(root, offset, length));
	}
	
	public String initialize(JavaScriptUnit root, ASTNode node) {
		if (!(node instanceof Name))
			return SearchMessages.OccurrencesFinder_no_element; 
		fRoot= root;
		fSelectedNode= (Name)node;
		fTarget= fSelectedNode.resolveBinding();
		if (fTarget == null)
			return SearchMessages.OccurrencesFinder_no_binding; 
		fTarget= getBindingDeclaration(fTarget);
		
		fTargetIsStaticMethodImport= isStaticImport(fSelectedNode.getParent());
		return null;
	}
	
	public List perform() {
		fRoot.accept(this);
		return fUsages;
	}
	
	public void collectOccurrenceMatches(IJavaScriptElement element, IDocument document, Collection resultingMatches) {
		boolean isVariable= fTarget instanceof IVariableBinding;
		HashMap lineToGroup= new HashMap();
		
		for (Iterator iter= fUsages.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			int startPosition= node.getStartPosition();
			int length= node.getLength();
			try {
				boolean isWriteAccess= fWriteUsages.contains(node);
				int line= document.getLineOfOffset(startPosition);
				Integer lineInteger= Integer.valueOf(line);
				OccurrencesGroupKey groupKey= (OccurrencesGroupKey) lineToGroup.get(lineInteger);
				if (groupKey == null) {
					IRegion region= document.getLineInformation(line);
					String lineContents= document.get(region.getOffset(), region.getLength()).trim();
					groupKey= new OccurrencesGroupKey(element, line, lineContents, isWriteAccess, isVariable);
					lineToGroup.put(lineInteger, groupKey);
				} else if (isWriteAccess) {
					// a line with read an write access is considered as write access:
					groupKey.setWriteAccess(true);
				}
				Match match= new Match(groupKey, startPosition, length);
				resultingMatches.add(match);
			} catch (BadLocationException e) {
				//nothing
			}
		}
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.search.IOccurrencesFinder#getJobLabel()
	 */
	public String getJobLabel() {
		return SearchMessages.OccurrencesFinder_searchfor ; 
	}
	
	public String getElementName() {
		if (fSelectedNode != null) {
			return ASTNodes.asString(fSelectedNode);
		}
		return null;
	}
	
	public String getUnformattedPluralLabel() {
		return SearchMessages.OccurrencesFinder_label_plural;
	}
	
	public String getUnformattedSingularLabel() {
		return SearchMessages.OccurrencesFinder_label_singular;
	}
	
	public boolean visit(QualifiedName node) {
		final IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding && ((IVariableBinding)binding).isField()) {
			SimpleName name= node.getName();
			return !match(name, fUsages, name.resolveBinding());
		}
		if (binding instanceof IFunctionBinding) {
			if (isStaticImport(node)) {
				SimpleName name= node.getName();
				return !matchStaticImport(name, fUsages, (IFunctionBinding)binding);
			}
		}
		return !match(node, fUsages, binding);
	}
	
	private static boolean isStaticImport(ASTNode node) {
		if (!(node instanceof QualifiedName))
			return false;
		
		ASTNode parent= ((QualifiedName)node).getParent();
		return parent  instanceof ImportDeclaration && ((ImportDeclaration)parent).isStatic();
	}

	public boolean visit(FunctionInvocation node) {
		if (fTargetIsStaticMethodImport)
			return !matchStaticImport(node.getName(), fUsages, node.resolveMethodBinding());
		
		return true;
	}
	
	public boolean visit(SimpleName node) {
		return !match(node, fUsages, node.resolveBinding());
	}

	/*
	 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ConstructorInvocation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		// match with the constructor and the type.
		Type type= node.getType();
		if (type!=null)
		{
			if (type instanceof SimpleType) {
				Name name= ((SimpleType) type).getName();
				if (name instanceof QualifiedName)
					name= ((QualifiedName)name).getName();
				match(name, fUsages, node.resolveConstructorBinding());
			}
			
		}
		else
		{
			Expression member = node.getMember();
			if (member instanceof SimpleName)
			{
				SimpleName name=(SimpleName)member;
				match(name,fUsages,node.resolveConstructorBinding());
			}
		}
		
		return super.visit(node);
	}
	
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		SimpleName name= getSimpleName(lhs);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());	
		lhs.accept(this);
		node.getRightHandSide().accept(this);
		return false;
	}
	
	public boolean visit(SingleVariableDeclaration node) {
		match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		if (node.getParent().getNodeType() == ASTNode.FIELD_DECLARATION || node.getInitializer() != null)
			match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(PrefixExpression node) {
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator == Operator.INCREMENT || operator == Operator.DECREMENT) {
			Expression operand= node.getOperand();
			SimpleName name= getSimpleName(operand);
			if (name != null) 
				match(name, fWriteUsages, name.resolveBinding());				
		}
		return super.visit(node);
	}

	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		SimpleName name= getSimpleName(operand);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());
		return super.visit(node);
	}
	
	private boolean match(Name node, List result, IBinding binding) {
		if (binding != null && Bindings.equals(getBindingDeclaration(binding), fTarget)) {
			result.add(node);
			return true;
		}
		return false;
	}
	
	private boolean matchStaticImport(Name node, List result, IFunctionBinding binding) {
		if (binding == null || node == null || !(fTarget instanceof IFunctionBinding) || !Modifier.isStatic(binding.getModifiers()))
			return false;
		
		IFunctionBinding targetMethodBinding= (IFunctionBinding)fTarget;
		if ((fTargetIsStaticMethodImport || Modifier.isStatic(targetMethodBinding.getModifiers())) && (targetMethodBinding.getDeclaringClass().getTypeDeclaration() == binding.getDeclaringClass().getTypeDeclaration())) {
			if (node.getFullyQualifiedName().equals(targetMethodBinding.getName())) {
				result.add(node);
				return true;
			}
		}
		return false;
	}

	private SimpleName getSimpleName(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression);
		else if (expression instanceof QualifiedName)
			return (((QualifiedName) expression).getName());
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName();
		return null;
	}
	
	private IBinding getBindingDeclaration(IBinding binding) {
		switch (binding.getKind()) {
			case IBinding.TYPE :
				return ((ITypeBinding)binding).getTypeDeclaration();
			case IBinding.METHOD :
				return ((IFunctionBinding)binding).getMethodDeclaration();
			case IBinding.VARIABLE :
				return ((IVariableBinding)binding).getVariableDeclaration();
			default:
				return binding;
		}
	}
}
