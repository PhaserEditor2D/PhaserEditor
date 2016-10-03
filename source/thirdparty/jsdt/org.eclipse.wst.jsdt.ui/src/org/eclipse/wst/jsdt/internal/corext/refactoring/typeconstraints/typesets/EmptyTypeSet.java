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

public class EmptyTypeSet extends TypeSet {

	EmptyTypeSet(TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isUniverse()
	 */
	public boolean isUniverse() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#makeClone()
	 */
	public TypeSet makeClone() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#intersectedWith(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isEmpty()
	 */
	public boolean isEmpty() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#upperBound()
	 */
	public TypeSet upperBound() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#lowerBound()
	 */
	public TypeSet lowerBound() {
		return this;
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
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#contains(TType)
	 */
	public boolean contains(TType t) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#containsAll(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet s) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#iterator()
	 */
	public Iterator iterator() {
		return new Iterator() {
			public void remove() {
				//do nothing
			}
			public boolean hasNext() {
				return false;
			}
			public Object next() {
				return null;
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isSingleton()
	 */
	public boolean isSingleton() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#anyMember()
	 */
	public TType anyMember() {
		return null;
	}

	public String toString() {
		return "{ }"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#enumerate()
	 */
	public EnumeratedTypeSet enumerate() {
		return new EnumeratedTypeSet(getTypeSetEnvironment());
	}
}
