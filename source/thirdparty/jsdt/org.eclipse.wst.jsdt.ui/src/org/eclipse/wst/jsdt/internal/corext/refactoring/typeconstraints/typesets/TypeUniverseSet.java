/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Robert M. Fuhrer (rfuhrer@watson.ibm.com), IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets;

import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;

public class TypeUniverseSet extends SubTypesOfSingleton {

	TypeUniverseSet(TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment.getJavaLangObject(), typeSetEnvironment);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.SubTypesSet#contains(TType)
	 */
	public boolean contains(TType t) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.SubTypesSet#containsAll(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet s) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#addedTo(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public TypeSet addedTo(TypeSet that) {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.SubTypesOfSingleton#makeClone()
	 */
	public TypeSet makeClone() {
		return this; // new TypeUniverseSet();
	}

	public String toString() {
		return "{ " + fID + ": <universe> }";  //$NON-NLS-1$//$NON-NLS-2$
	}
}
