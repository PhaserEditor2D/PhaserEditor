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

package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;

/**
 * Abstract Factory for the creation of ConstraintVariables. This default factory
 * ensures that no duplicate ConstraintVariables are created, so that Object.equals()
 * can be used to compare ConstraintVariables.
 */
public class ConstraintVariableFactory implements IConstraintVariableFactory {

	private Map/*<IBinding,IBinding>*/ fBindingMap= new HashMap();

	private Map/*<IBinding + CompilationUnitRange,ExpressionVariable>*/ fExpressionMap= new Hashtable();
	private Map/*<Integer,ExpressionVariable>*/ fLiteralMap= new HashMap();	
	private Map/*<CompilationUnitRange,TypeVariable>*/ fTypeVariableMap= new HashMap();
	private Map/*<String,DeclaringTypeVariable>*/ fDeclaringTypeVariableMap= new HashMap();
	private Map/*<String,ParameterTypeVariable>*/ fParameterMap= new HashMap();
	private Map/*<String,RawBindingVariable>*/ fRawBindingMap= new HashMap();
	private Map/*<String,ReturnTypeVariable>*/ fReturnVariableMap= new HashMap();
	
	public static final boolean REPORT= false;
	protected int nrCreated=0;
	protected int nrRetrieved=0;
	
	public int getNumCreated(){
		return nrCreated;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeExpressionVariable(org.eclipse.wst.jsdt.core.dom.Expression, org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IContext)
	 */
	public ConstraintVariable makeExpressionOrTypeVariable(Expression expression,
													       IContext context) {
		IBinding binding= ExpressionVariable.resolveBinding(expression);
		
		if (binding instanceof ITypeBinding){
			IJavaScriptUnit cu= ASTCreator.getCu(expression);
			Assert.isNotNull(cu);
			CompilationUnitRange range= new CompilationUnitRange(cu, expression);
			return makeTypeVariable((ITypeBinding)getKey(binding), expression.toString(), range);
		}
		
		if (ASTNodes.isLiteral(expression)){
			Integer nodeType= Integer.valueOf(expression.getNodeType());
			if (! fLiteralMap.containsKey(nodeType)){
				fLiteralMap.put(nodeType, new ExpressionVariable(expression));
				if (REPORT) nrCreated++;
			} else {
				if (REPORT) nrRetrieved++;
			}
			if (REPORT) dumpConstraintStats();
			return (ExpressionVariable) fLiteralMap.get(nodeType);
		}
			
		// For ExpressionVariables, there are two cases. If the expression has a binding
		// we use that as the key. Otherwise, we use the CompilationUnitRange. See
		// also ExpressionVariable.equals()
		ExpressionVariable ev;
		Object key;
		if (binding != null){
			key= getKey(binding);
		} else {
			key= new CompilationUnitRange(ASTCreator.getCu(expression), expression);
		}
		ev= (ExpressionVariable)fExpressionMap.get(key);
		
		if (ev != null){
			if (REPORT) nrRetrieved++;
		} else {
			ev= new ExpressionVariable(expression);
			fExpressionMap.put(key, ev);
			if (REPORT) nrCreated++;
			if (REPORT) dumpConstraintStats();
		}
		return ev;
	}
	
	
	//
	// The method IBinding.equals() does not have the desired behavior, and Bindings.equals()
	// must be used to compare bindings. We use an additional layer of Hashing.
	//
	private IBinding getKey(IBinding binding) {
		if (fBindingMap.containsKey(binding)){
			return (IBinding)fBindingMap.get(binding);
		} else {
			for (Iterator it= fBindingMap.keySet().iterator(); it.hasNext(); ){
				IBinding b2= (IBinding)it.next();
				if (Bindings.equals(binding, b2)){
					fBindingMap.put(binding, b2);
					return b2;
				}
			}
			fBindingMap.put(binding, binding);
			return binding;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeDeclaringTypeVariable(org.eclipse.wst.jsdt.core.dom.ITypeBinding)
	 */
	public DeclaringTypeVariable makeDeclaringTypeVariable(ITypeBinding memberTypeBinding) {
		String key = memberTypeBinding.getKey();
		if (! fDeclaringTypeVariableMap.containsKey(key)){
			fDeclaringTypeVariableMap.put(key, new DeclaringTypeVariable(memberTypeBinding));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (DeclaringTypeVariable)fDeclaringTypeVariableMap.get(key);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeDeclaringTypeVariable(org.eclipse.wst.jsdt.core.dom.IVariableBinding)
	 */
	public DeclaringTypeVariable makeDeclaringTypeVariable(IVariableBinding fieldBinding) {
		String key= fieldBinding.getKey();
		if (! fDeclaringTypeVariableMap.containsKey(key)){
			fDeclaringTypeVariableMap.put(key, new DeclaringTypeVariable(fieldBinding));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (DeclaringTypeVariable)fDeclaringTypeVariableMap.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeDeclaringTypeVariable(org.eclipse.wst.jsdt.core.dom.IFunctionBinding)
	 */
	public DeclaringTypeVariable makeDeclaringTypeVariable(IFunctionBinding methodBinding) {
		String key= methodBinding.getKey();
		if (! fDeclaringTypeVariableMap.containsKey(key)){
			fDeclaringTypeVariableMap.put(key, new DeclaringTypeVariable(methodBinding));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (DeclaringTypeVariable)fDeclaringTypeVariableMap.get(key);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeParameterTypeVariable(org.eclipse.wst.jsdt.core.dom.IFunctionBinding, int)
	 */
	public ParameterTypeVariable makeParameterTypeVariable(IFunctionBinding methodBinding,
														   int parameterIndex) {
		String key= methodBinding.getKey() + parameterIndex;
		if (! fParameterMap.containsKey(key)){
			fParameterMap.put(key, new ParameterTypeVariable(methodBinding, parameterIndex));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (ParameterTypeVariable)fParameterMap.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeRawBindingVariable(org.eclipse.wst.jsdt.core.dom.ITypeBinding)
	 */
	public RawBindingVariable makeRawBindingVariable(ITypeBinding binding) {
		String key = binding.getKey();
		if (! fRawBindingMap.containsKey(key)){
			fRawBindingMap.put(key, new RawBindingVariable(binding));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (RawBindingVariable)fRawBindingMap.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeReturnTypeVariable(org.eclipse.wst.jsdt.core.dom.ReturnStatement)
	 */
	public ReturnTypeVariable makeReturnTypeVariable(ReturnStatement returnStatement) {
		return makeReturnTypeVariable(ReturnTypeVariable.getMethod(returnStatement).resolveBinding());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeReturnTypeVariable(org.eclipse.wst.jsdt.core.dom.IFunctionBinding)
	 */
	public ReturnTypeVariable makeReturnTypeVariable(IFunctionBinding methodBinding) {
		String key= methodBinding.getKey();
		if (!fReturnVariableMap.containsKey(key)){
			fReturnVariableMap.put(key, new ReturnTypeVariable(methodBinding));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (ReturnTypeVariable)fReturnVariableMap.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeTypeVariable(org.eclipse.wst.jsdt.core.dom.Type)
	 */
	public TypeVariable makeTypeVariable(Type type) {
		IJavaScriptUnit cu= ASTCreator.getCu(type);
		Assert.isNotNull(cu);
		CompilationUnitRange range= new CompilationUnitRange(cu, type);
		if (! fTypeVariableMap.containsKey(range)){
			fTypeVariableMap.put(range, new TypeVariable(type));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (TypeVariable) fTypeVariableMap.get(range);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.IConstraintVariableFactory#makeTypeVariable(org.eclipse.wst.jsdt.core.dom.ITypeBinding, java.lang.String, org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange)
	 */
	public TypeVariable makeTypeVariable(ITypeBinding binding, String source, CompilationUnitRange range) {
		if (! fTypeVariableMap.containsKey(range)){
			fTypeVariableMap.put(range, new TypeVariable(binding, source, range));
			if (REPORT) nrCreated++;
		} else {
			if (REPORT) nrRetrieved++;
		}
		if (REPORT) dumpConstraintStats();
		return (TypeVariable) fTypeVariableMap.get(range);
	}

	protected void dumpConstraintStats() {
		System.out.println("created: " + nrCreated + ", retrieved: " + nrRetrieved); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
