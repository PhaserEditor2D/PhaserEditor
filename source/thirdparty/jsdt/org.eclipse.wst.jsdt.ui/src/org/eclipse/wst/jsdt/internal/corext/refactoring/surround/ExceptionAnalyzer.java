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
package org.eclipse.wst.jsdt.internal.corext.refactoring.surround;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.AbstractExceptionAnalyzer;

public class ExceptionAnalyzer extends AbstractExceptionAnalyzer {

	private Selection fSelection;
	
	private static class ExceptionComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			int d1= getDepth((ITypeBinding)o1);
			int d2= getDepth((ITypeBinding)o2);
			if (d1 < d2)
				return 1;
			if (d1 > d2)
				return -1;
			return 0;
		}
		private int getDepth(ITypeBinding binding) {
			int result= 0;
			while (binding != null) {
				binding= binding.getSuperclass();
				result++;
			}
			return result;
		}
	}
	
	private ExceptionAnalyzer(Selection selection) {
		Assert.isNotNull(selection);
		fSelection= selection;
	}
	
	public static ITypeBinding[] perform(BodyDeclaration enclosingNode, Selection selection) {
		ExceptionAnalyzer analyzer= new ExceptionAnalyzer(selection);
		enclosingNode.accept(analyzer);
		List exceptions= analyzer.getCurrentExceptions();
		if (enclosingNode.getNodeType() == ASTNode.FUNCTION_DECLARATION) {
			List thrownExceptions= ((FunctionDeclaration)enclosingNode).thrownExceptions();
			for (Iterator thrown= thrownExceptions.iterator(); thrown.hasNext();) {
				ITypeBinding thrownException= ((Name)thrown.next()).resolveTypeBinding();
				if (thrownException != null) {
					for (Iterator excep= exceptions.iterator(); excep.hasNext();) {
						ITypeBinding exception= (ITypeBinding) excep.next();
						if (exception.isAssignmentCompatible(thrownException))
							excep.remove();
					}
				}
			}
		}
		Collections.sort(exceptions, new ExceptionComparator());
		return (ITypeBinding[]) exceptions.toArray(new ITypeBinding[exceptions.size()]);
	}

	public boolean visit(ThrowStatement node) {
		ITypeBinding exception= node.getExpression().resolveTypeBinding();
		if (!isSelected(node) || exception == null || Bindings.isRuntimeException(exception)) // Safety net for null bindings when compiling fails.
			return true;
		
		addException(exception);
		return true;
	}
	
	public boolean visit(FunctionInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveMethodBinding(), node.getAST());
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveMethodBinding(), node.getAST());
	}
	
	public boolean visit(ClassInstanceCreation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding(), node.getAST());
	}
	
	public boolean visit(ConstructorInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding(), node.getAST());
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding(), node.getAST());
	}	

	private boolean handleExceptions(IFunctionBinding binding, AST ast) {
		return true;
	}
		
	private boolean isSelected(ASTNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.SELECTED;
	}
}
