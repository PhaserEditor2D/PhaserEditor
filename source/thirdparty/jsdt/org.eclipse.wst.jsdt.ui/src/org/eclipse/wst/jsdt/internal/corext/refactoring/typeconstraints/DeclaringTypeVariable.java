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
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;

/**
 * Tells the type which declares the member.
 */
public class DeclaringTypeVariable extends ConstraintVariable{
	
	private final IBinding fMemberBinding;
	
	protected DeclaringTypeVariable(ITypeBinding memberTypeBinding) {
		super(memberTypeBinding.getDeclaringClass());
		fMemberBinding= memberTypeBinding;
	}

	protected DeclaringTypeVariable(IVariableBinding fieldBinding) {
		super(fieldBinding.getDeclaringClass());
		Assert.isTrue(fieldBinding.isField());
		fMemberBinding= fieldBinding;
	}

	protected DeclaringTypeVariable(IFunctionBinding methodBinding) {
		super(methodBinding.getDeclaringClass());
		fMemberBinding= methodBinding;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Decl(" + Bindings.asString(fMemberBinding) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IBinding getMemberBinding() {
		return fMemberBinding;
	}
}
