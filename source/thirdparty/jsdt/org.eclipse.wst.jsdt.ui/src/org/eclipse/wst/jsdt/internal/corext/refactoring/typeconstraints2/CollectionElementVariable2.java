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

package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;

public final class CollectionElementVariable2 extends ConstraintVariable2 {

	public static final int NOT_DECLARED_TYPE_VARIABLE_INDEX= -1;
	
	private final ConstraintVariable2 fParentCv;
	private final String fTypeVariableKey;
	private final int fDeclarationTypeVariableIndex;

	//TODO: make a 'TypedCollectionElementVariable extends TypeConstraintVariable2'
	// iff Collection reference already has type parameter in source
	/**
	 * @param parentCv the parent constraint variable
	 * @param typeVariable the type variable for this constraint
	 * @param declarationTypeVariableIndex
	 */
	public CollectionElementVariable2(ConstraintVariable2 parentCv, ITypeBinding typeVariable, int declarationTypeVariableIndex) {
		super(null);
		fParentCv= parentCv;
		throw new IllegalArgumentException(typeVariable.toString());
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fParentCv.hashCode() ^ fTypeVariableKey.hashCode();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other.getClass() != CollectionElementVariable2.class)
			return false;
		
		CollectionElementVariable2 other2= (CollectionElementVariable2) other;
		return fParentCv == other2.fParentCv
				&& fTypeVariableKey.equals(other2.fTypeVariableKey);
	}
	
	public int getDeclarationTypeVariableIndex() {
		return fDeclarationTypeVariableIndex;
	}
	
	public ConstraintVariable2 getParentConstraintVariable() {
		return fParentCv;
	}
	
	public IJavaScriptUnit getCompilationUnit() {
		if (fParentCv instanceof ISourceConstraintVariable)
			return ((ISourceConstraintVariable) fParentCv).getCompilationUnit();
		else
			return null;
//			//TODO: assert in constructor(s)
//			return ((CollectionElementVariable2) fElementCv).getCompilationUnit();
	}
	
	public String toString() {
		return "Elem[" + fParentCv.toString() + ", " + fTypeVariableKey + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
