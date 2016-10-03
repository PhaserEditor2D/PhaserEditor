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
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;

public class ParameterTypeVariable extends ConstraintVariable {

	private final IFunctionBinding fMethodBinding;
	private final int fParameterIndex;
	
	public ParameterTypeVariable(IFunctionBinding methodBinding, int parameterIndex) {
		super(methodBinding.getParameterTypes()[parameterIndex]);
		Assert.isNotNull(methodBinding);
		Assert.isTrue(0 <= parameterIndex);
		Assert.isTrue(parameterIndex < methodBinding.getParameterTypes().length);
		fMethodBinding= methodBinding;
		fParameterIndex= parameterIndex;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[Parameter(" + fParameterIndex + "," + Bindings.asString(fMethodBinding) + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public IFunctionBinding getMethodBinding() {
		return fMethodBinding;
	}

	public int getParameterIndex() {
		return fParameterIndex;
	}

}
