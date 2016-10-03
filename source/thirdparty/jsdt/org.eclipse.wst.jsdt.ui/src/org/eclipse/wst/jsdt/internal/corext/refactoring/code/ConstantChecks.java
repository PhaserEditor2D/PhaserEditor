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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.IExpressionFragment;

class ConstantChecks {
	private static abstract class ExpressionChecker extends ASTVisitor {

		private final IExpressionFragment fExpression;
		protected boolean fResult= true;

		public ExpressionChecker(IExpressionFragment ex) {
			fExpression= ex;
		}
		public boolean check() {
			fResult= true;
			fExpression.getAssociatedNode().accept(this);
			return fResult;
		}
	}

	private static class LoadTimeConstantChecker extends ExpressionChecker {
		public LoadTimeConstantChecker(IExpressionFragment ex) {
			super(ex);
		}

		public boolean visit(SuperFieldAccess node) {
			fResult= false;
			return false;
		}
		public boolean visit(SuperMethodInvocation node) {
			fResult= false;
			return false;
		}
		public boolean visit(ThisExpression node) {
			fResult= false;
			return false;
		}
		public boolean visit(FieldAccess node) {
			fResult= new LoadTimeConstantChecker((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(node.getExpression())).check();
			return false;
		}
		public boolean visit(FunctionInvocation node) {
			if(node.getExpression() == null) {
				visitName(node.getName());	
			} else {
				fResult= new LoadTimeConstantChecker((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(node.getExpression())).check();
			}
			
			return false;
		}
		public boolean visit(QualifiedName node) {
			return visitName(node);
		}
		public boolean visit(SimpleName node) {
			return visitName(node);
		}
		
		private boolean visitName(Name name) {
			fResult= checkName(name);
			return false; //Do not descend further                 
		}
		
		private boolean checkName(Name name) {
			IBinding binding= name.resolveBinding();
			if (binding == null)
				return true;  /* If the binding is null because of compile errors etc., 
				                  scenarios which may have been deemed unacceptable in
				                  the presence of semantic information will be admitted. */
			
			// If name represents a member:
			if (binding instanceof IVariableBinding || binding instanceof IFunctionBinding)
				return isMemberReferenceValidInClassInitialization(name);
			else if (binding instanceof ITypeBinding)
				return true;
			else {
					/*  IPackageBinding is not expected, as a package name not
					    used as a type name prefix is not expected in such an
					    expression.  Other types are not expected either.
					 */
					Assert.isTrue(false);
					return true;		
			}
		}

		private boolean isMemberReferenceValidInClassInitialization(Name name) {
			IBinding binding= name.resolveBinding();
			Assert.isTrue(binding instanceof IVariableBinding || binding instanceof IFunctionBinding);

			if(name instanceof SimpleName)
				return Modifier.isStatic(binding.getModifiers());
			else {
				Assert.isTrue(name instanceof QualifiedName);
				return checkName(((QualifiedName) name).getQualifier());
			}
		}
	}

	private static class StaticFinalConstantChecker extends ExpressionChecker {
		public StaticFinalConstantChecker(IExpressionFragment ex) {
			super(ex);
		}
		
		public boolean visit(SuperFieldAccess node) {
			fResult= false;
			return false;
		}
		public boolean visit(SuperMethodInvocation node) {
			fResult= false;
			return false;
		}
		public boolean visit(ThisExpression node) {
			fResult= false;
			return false;
		}

		public boolean visit(QualifiedName node) {
			return visitName(node);
		}
		public boolean visit(SimpleName node) {
			return visitName(node);
		}
		private boolean visitName(Name name) {
			IBinding binding= name.resolveBinding();
			if(binding == null) { 
				/* If the binding is null because of compile errors etc., 
				   scenarios which may have been deemed unacceptable in
				   the presence of semantic information will be admitted. 
				   Descend deeper.
				 */
				 return true;
			}
			
			int modifiers= binding.getModifiers();	
			if(binding instanceof IVariableBinding) {
				if (!(Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))) {
					fResult= false;
					return false;
				}		
			} else if(binding instanceof IFunctionBinding) {
				if (!Modifier.isStatic(modifiers)) {
					fResult= false;
					return false;
				}
			} else if(binding instanceof ITypeBinding) {
				return false; // It's o.k.  Don't descend deeper.
		
			} else {
					/*  IPackageBinding is not expected, as a package name not
					    used as a type name prefix is not expected in such an
					    expression.  Other types are not expected either.
					 */
					Assert.isTrue(false);
					return false;		
			}
			
			//Descend deeper:
			return true;
		}
	}
	
	public static boolean isStaticFinalConstant(IExpressionFragment ex) {
		return new StaticFinalConstantChecker(ex).check();
	}

	public static boolean isLoadTimeConstant(IExpressionFragment ex) {
		return new LoadTimeConstantChecker(ex).check();
	}
}
