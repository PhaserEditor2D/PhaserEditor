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

package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;

/**
 * A VariableVariable is a ConstraintVariable which stands for
 * the type of a variable, namely a field or a local variable
 * Use {@link ParameterTypeVariable2} for method parameters).
 */
public final class VariableVariable2 extends ConstraintVariable2 implements ISourceConstraintVariable {

	private final String fKey;
	private IJavaScriptUnit fCompilationUnit;
	
	public VariableVariable2(TType type, IVariableBinding binding) {
		super(type);
		fKey= binding.getKey();
	}

	public void setCompilationUnit(IJavaScriptUnit unit) {
		fCompilationUnit= unit;
	}
	
	public IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	public String getKey() {
		return fKey;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fKey.hashCode();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other.getClass() != VariableVariable2.class)
			return false;
		
		return fKey.equals(((VariableVariable2) other).getKey());
	}
}
