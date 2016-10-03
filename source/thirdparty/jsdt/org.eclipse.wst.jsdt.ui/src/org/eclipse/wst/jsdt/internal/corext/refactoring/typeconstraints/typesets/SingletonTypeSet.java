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

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;

public class SingletonTypeSet extends TypeSet {
	private final TType fType;

	
	//TODO: encapsulate in factory method and return the same set for known types
	public SingletonTypeSet(TType t, TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment);
		Assert.isNotNull(t);
		fType= t;
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
		return this; //new SingletonTypeSet(fType, getTypeSetEnvironment());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#intersectedWith(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		if (s2.contains(fType))
			return this;
		else
			return getTypeSetEnvironment().getEmptyTypeSet();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isEmpty()
	 */
	public boolean isEmpty() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#upperBound()
	 */
	public TypeSet upperBound() {
		return this; // makeClone();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#lowerBound()
	 */
	public TypeSet lowerBound() {
		return this; // makeClone();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueLowerBound()
	 */
	public boolean hasUniqueLowerBound() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueUpperBound()
	 */
	public boolean hasUniqueUpperBound() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueLowerBound()
	 */
	public TType uniqueLowerBound() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueUpperBound()
	 */
	public TType uniqueUpperBound() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#contains(TType)
	 */
	public boolean contains(TType t) {
		return fType.equals(t);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#containsAll(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet s) {
		if (s.isEmpty())
			return true;
		if (s.isSingleton())
			return s.anyMember().equals(fType);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#iterator()
	 */
	public Iterator iterator() {
		return new Iterator() {
			private boolean done= false;
			public void remove() {
				throw new UnsupportedOperationException();
			}
			public boolean hasNext() {
				return !done;
			}
			public Object next() {
				done= true;
				return fType;
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#anyMember()
	 */
	public TType anyMember() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#enumerate()
	 */
	public EnumeratedTypeSet enumerate() {
		EnumeratedTypeSet enumeratedTypeSet= new EnumeratedTypeSet(fType, getTypeSetEnvironment());
		enumeratedTypeSet.initComplete();
		return enumeratedTypeSet;
	}

	public boolean equals(Object o) {
		if (o instanceof SingletonTypeSet) {
			SingletonTypeSet other= (SingletonTypeSet) o;

			return fType.equals(other.fType);
		} else if (o instanceof TypeSet) {
			TypeSet other= (TypeSet) o;

			return other.isSingleton() && other.anyMember().equals(fType);
		} else
			return false;
	}

	public String toString() {
		return "{" + fID + ": " + fType.getPrettySignature() + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
