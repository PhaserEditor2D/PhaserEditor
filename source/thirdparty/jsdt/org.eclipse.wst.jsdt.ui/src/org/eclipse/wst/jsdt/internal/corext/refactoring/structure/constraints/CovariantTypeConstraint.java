/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;

/**
 * Type constraint which models covariance-related types.
 */
public final class CovariantTypeConstraint implements ITypeConstraint2 {

	/** The ancestor type */
	private final ConstraintVariable2 fAncestor;

	/** The descendant type */
	private final ConstraintVariable2 fDescendant;

	/**
	 * Creates a new covariant type constraint.
	 * 
	 * @param descendant the descendant type
	 * @param ancestor the ancestor type
	 */
	public CovariantTypeConstraint(final ConstraintVariable2 descendant, final ConstraintVariable2 ancestor) {
		Assert.isNotNull(descendant);
		Assert.isNotNull(ancestor);
		fDescendant= descendant;
		fAncestor= ancestor;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public final boolean equals(final Object object) {
		if (object.getClass() != CovariantTypeConstraint.class)
			return false;
		final ITypeConstraint2 other= (ITypeConstraint2) object;
		return getLeft() == other.getLeft() && getRight() == other.getRight();
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2#getLeft()
	 */
	public final ConstraintVariable2 getLeft() {
		return fDescendant;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2#getRight()
	 */
	public final ConstraintVariable2 getRight() {
		return fAncestor;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	public final int hashCode() {
		return fDescendant.hashCode() ^ 35 * fAncestor.hashCode();
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		return fDescendant.toString() + " <<= " + fAncestor.toString(); //$NON-NLS-1$ 
	}
}
