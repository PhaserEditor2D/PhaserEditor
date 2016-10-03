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
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ITypeSet;

public abstract class TypeSet implements ITypeSet {
	
	public TType chooseSingleType() {
		return null;
	}
	
	public ITypeSet restrictedTo(ITypeSet restrictionSet) {
		throw new UnsupportedOperationException();
	}
	
	protected TType getJavaLangObject() {
		return fTypeSetEnvironment.getJavaLangObject();
	}

	protected TypeSetEnvironment getTypeSetEnvironment() {
		return fTypeSetEnvironment;
	}

	static private int sID= 0;

	static public int getCount() { return sID; }
	static public void resetCount() { sID= 0; }

	/**
	 * An ID unique to this EnumeratedTypeSet instance, to aid in debugging the sharing
	 * of TypeSets across ConstraintVariables in a TypeEstimateEnvironment.
	 */
	protected final int fID;
	private final TypeSetEnvironment fTypeSetEnvironment;
	
	protected TypeSet(TypeSetEnvironment typeSetEnvironment) { 
		fTypeSetEnvironment= typeSetEnvironment;
		fID= sID++;
	}

	/**
	 * @return <code>true</code> iff this set represents the universe of TTypes
	 */
	abstract public boolean isUniverse();

	abstract public TypeSet makeClone();

	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		return null;
	}

	/**
	 * Computes and returns a <em>new</em> TypeSet representing the intersection of the
	 * receiver with s2. Does not modify the receiver or argument sets.
	 * @param s2
	 * @return the new TypeSet
	 */
	public TypeSet intersectedWith(TypeSet s2) {
		if (s2.isUniverse())
			return makeClone();
		else if (isUniverse())
			return s2.makeClone();
		else if (isEmpty() || s2.isEmpty())
			return getTypeSetEnvironment().getEmptyTypeSet();
		else if (isSingleton()) {
			if (s2.contains(anyMember()))
				return makeClone();
			else
				return getTypeSetEnvironment().getEmptyTypeSet();
		} else if (s2.isSingleton()) {
				if (contains(s2.anyMember()))
					return s2.makeClone();
				else
					return getTypeSetEnvironment().getEmptyTypeSet();
		} else if (s2 instanceof TypeSetIntersection) {
			TypeSetIntersection x= (TypeSetIntersection) s2;
			// xsect(A,xsect(A,B)) = xsect(A,B) and
			// xsect(B,xsect(A,B)) = xsect(A,B)
			if (x.getLHS().equals(this) || x.getRHS().equals(this))
				return x;
		}

		TypeSet result= specialCasesIntersectedWith(s2);

		if (result != null)
			return result;
		else
			return new TypeSetIntersection(this, s2);
	}

	/**
	 * Returns the TypeSet resulting from union'ing the receiver with the argument.
	 * Does not modify the receiver or the argument sets.
	 */
	public TypeSet addedTo(TypeSet that) {
		if (isUniverse() || that.isUniverse())
			return getTypeSetEnvironment().getUniverseTypeSet();
		if ((this instanceof EnumeratedTypeSet || this instanceof SingletonTypeSet) &&
			(that instanceof EnumeratedTypeSet || that instanceof SingletonTypeSet)) {
			EnumeratedTypeSet result= enumerate();

			result.addAll(that);
			return result;
		}
		return new TypeSetUnion(this, that);
	}

	/**
	 * Returns a new TypeSet representing the set of all sub-types of the
	 * types in the receiver.
	 */
	public TypeSet subTypes() {
		if (isUniverse() || contains(getJavaLangObject()))
			return getTypeSetEnvironment().getUniverseTypeSet();

		if (isSingleton())
			return possiblyArraySubTypeSetFor(anyMember());

		return getTypeSetEnvironment().createSubTypesSet(this);
	}

	private TypeSet possiblyArraySubTypeSetFor(TType t) {
		// In Java, subTypes(x[]) == (subTypes(x))[]
//		if (t.isArrayType()) {
//			ArrayType at= (ArrayType) t;
//
//			return new ArrayTypeSet(possiblyArraySubTypeSetFor(at.getArrayElementType()));
//		} else
			
		return getTypeSetEnvironment().createSubTypesOfSingleton(t);
	}

	private TypeSet possiblyArraySuperTypeSetFor(TType t) {
		// In Java, superTypes(x[]) == (superTypes(x))[] union {Object}
//		if (t.isArrayType()) {
//			ArrayType at= (ArrayType) t;
//
//			return new ArraySuperTypeSet(possiblyArraySuperTypeSetFor(at.getArrayElementType()));
//		} else
			return getTypeSetEnvironment().createSuperTypesOfSingleton(t);
	}

	/**
	 * Returns a new TypeSet representing the set of all super-types of the
	 * types in the receiver.
	 */
	public TypeSet superTypes() {
		if (isUniverse())
			return getTypeSetEnvironment().getUniverseTypeSet();

		if (isSingleton())
			return possiblyArraySuperTypeSetFor(anyMember());

		return getTypeSetEnvironment().createSuperTypesSet(this);
	}

	/**
	 * Return true iff the type set contains no types.
	 */
	abstract public boolean isEmpty();

	/**
	 * Returns the types in the upper bound of this set.
	 */
	abstract public TypeSet upperBound();

	/**
	 * Returns the types in the lower bound of this set.
	 */
	abstract public TypeSet lowerBound();

	/**
	 * Returns true iff this TypeSet has a unique lower bound.
	 */
	abstract public boolean hasUniqueLowerBound();

	/**
	 * Returns true iff this TypeSet has a unique upper bound other than
	 * java.lang.Object.
	 */
	abstract public boolean hasUniqueUpperBound();

	/**
	 * Returns the unique lower bound of this set of types, if it has one,
	 * or null otherwise.
	 */
	abstract public TType uniqueLowerBound();

	/**
	 * Returns the unique upper bound of this set of types, if it has one,
	 * or null otherwise.
	 */
	abstract public TType uniqueUpperBound();

	/**
	 * Returns true iff the type set contains the given type.
	 */
	abstract public boolean contains(TType t);

	/**
	 * Returns true iff the type set contains all of the types in the given TypeSet.
	 */
	abstract public boolean containsAll(TypeSet s);

	/**
	 * Returns an iterator over the types in the receiver.
	 */
	abstract public Iterator iterator();

	/**
	 * Returns a new TypeSet enumerating the receiver's contents.
	 */
	abstract public EnumeratedTypeSet enumerate();

	/**
	 * Returns true iff the given set has precisely one element
	 */
	abstract public boolean isSingleton();

	/**
	 * Returns an arbitrary member of the given Typeset.
	 */
	abstract public TType anyMember();
}
