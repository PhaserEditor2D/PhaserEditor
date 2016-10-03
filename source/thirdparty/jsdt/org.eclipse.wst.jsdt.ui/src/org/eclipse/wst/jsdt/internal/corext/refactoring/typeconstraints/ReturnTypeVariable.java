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

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;

public class ReturnTypeVariable extends ConstraintVariable{
	
	private final IFunctionBinding fMethodBinding;

	public ReturnTypeVariable(ReturnStatement returnStatement) {
		this(getMethod(returnStatement).resolveBinding());
		Assert.isNotNull(returnStatement);
	}

	public ReturnTypeVariable(IFunctionBinding methodBinding) {
		super(methodBinding.getReturnType());
		fMethodBinding= methodBinding;
	}
	
	public static FunctionDeclaration getMethod(ReturnStatement returnStatement) {
		return (FunctionDeclaration)ASTNodes.getParent(returnStatement, FunctionDeclaration.class);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[" + Bindings.asString(fMethodBinding) + "]_returnType"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public IFunctionBinding getMethodBinding() {
		return fMethodBinding;
	}

}
