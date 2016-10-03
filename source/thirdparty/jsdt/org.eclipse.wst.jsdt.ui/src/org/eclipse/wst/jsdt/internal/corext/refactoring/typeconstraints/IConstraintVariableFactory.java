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

import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.Type;

public interface IConstraintVariableFactory {
	ConstraintVariable makeExpressionOrTypeVariable(Expression expression, IContext context);
	DeclaringTypeVariable makeDeclaringTypeVariable(ITypeBinding memberTypeBinding);
	DeclaringTypeVariable makeDeclaringTypeVariable(IVariableBinding fieldBinding);
	DeclaringTypeVariable makeDeclaringTypeVariable(IFunctionBinding methodBinding);
	ParameterTypeVariable makeParameterTypeVariable(IFunctionBinding methodBinding, int parameterIndex);
	RawBindingVariable makeRawBindingVariable(ITypeBinding binding);
	ReturnTypeVariable makeReturnTypeVariable(ReturnStatement returnStatement);
	ReturnTypeVariable makeReturnTypeVariable(IFunctionBinding methodBinding);		
	TypeVariable makeTypeVariable(Type type);
	TypeVariable makeTypeVariable(ITypeBinding binding, String source, CompilationUnitRange range);	
}
