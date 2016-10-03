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

import java.util.Iterator;

import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;

public class TypeSetUnion extends TypeSet {
	private TypeSet fLHS;
	private TypeSet fRHS;

	public TypeSetUnion(TypeSet lhs, TypeSet rhs) {
		super(lhs.getTypeSetEnvironment());
		fLHS= lhs;
		fRHS= rhs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isUniverse()
	 */
	public boolean isUniverse() {
		if (fLHS.isUniverse() || fRHS.isUniverse())
			return true;
		if (fLHS.isSingleton() && fRHS.isSingleton())
			return false;
		throw new IllegalStateException("unimplemented"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#makeClone()
	 */
	public TypeSet makeClone() {
		return new TypeSetUnion(fLHS.makeClone(), fRHS.makeClone());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isEmpty()
	 */
	public boolean isEmpty() {
		return fLHS.isEmpty() && fRHS.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#contains(TType)
	 */
	public boolean contains(TType t) {
		return fLHS.contains(t) || fRHS.contains(t);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#containsAll(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet s) {
		return fLHS.containsAll(s) || fRHS.containsAll(s);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#anyMember()
	 */
	public TType anyMember() {
		return fLHS.anyMember();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o instanceof TypeSetUnion) {
			TypeSetUnion other= (TypeSetUnion) o;
			return other.fLHS.equals(fLHS) && other.fRHS.equals(fRHS);
		} else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#upperBound()
	 */
	public TypeSet upperBound() {
		throw new IllegalStateException("unimplemented"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#lowerBound()
	 */
	public TypeSet lowerBound() {
		throw new IllegalStateException("unimplemented"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#iterator()
	 */
	public Iterator iterator() {
		throw new IllegalStateException("unimplemented"); //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isSingleton()
	 */
	public boolean isSingleton() {
		return fLHS.isSingleton() && fRHS.isSingleton() && fLHS.anyMember().equals(fRHS.anyMember());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueLowerBound()
	 */
	public boolean hasUniqueLowerBound() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueUpperBound()
	 */
	public boolean hasUniqueUpperBound() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueLowerBound()
	 */
	public TType uniqueLowerBound() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueUpperBound()
	 */
	public TType uniqueUpperBound() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#enumerate()
	 */
	public EnumeratedTypeSet enumerate() {
		EnumeratedTypeSet result= fLHS.enumerate();

		result.addAll(fRHS.enumerate());
		return result;
	}

	public String toString() {
		return "<" + fID + ": union(" + fLHS + "," + fRHS + ")>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
