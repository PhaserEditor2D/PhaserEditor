/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;

/*
 * Not all fields defined by this type (& its subclasses) are initialized when it is created.
 * Some are initialized only when needed.
 *
 * Accessors have been provided for some public fields so all TypeBindings have the same API...
 * but access public fields directly whenever possible.
 * Non-public fields have accessors which should be used everywhere you expect the field to be initialized.
 *
 * null is NOT a valid value for a non-public field... it just means the field is not initialized.
 */
abstract public class TypeBinding extends Binding {

	public int id = TypeIds.NoId;
	public long tagBits = 0; // See values in the interface TagBits below

	/** Base type definitions */
	public final static BaseTypeBinding INT = new BaseTypeBinding(
			TypeIds.T_int, TypeConstants.INT, new char[] { 'I' });

	public final static BaseTypeBinding SHORT = new BaseTypeBinding(
			TypeIds.T_short, TypeConstants.SHORT, new char[] { 'S' });

	public final static BaseTypeBinding CHAR = new BaseTypeBinding(
			TypeIds.T_char, TypeConstants.CHAR, new char[] { 'C' });

	public final static BaseTypeBinding LONG = new BaseTypeBinding(
			TypeIds.T_long, TypeConstants.LONG, new char[] { 'J' });

	public final static BaseTypeBinding FLOAT = new BaseTypeBinding(
			TypeIds.T_float, TypeConstants.FLOAT, new char[] { 'F' });

	public final static BaseTypeBinding DOUBLE = new BaseTypeBinding(
			TypeIds.T_double, TypeConstants.DOUBLE, new char[] { 'D' });

	public final static BaseTypeBinding BOOLEAN = new BaseTypeBinding(
			TypeIds.T_boolean, TypeConstants.BOOLEAN, new char[] { 'Z' });

	public final static BaseTypeBinding NULL = new BaseTypeBinding(
			TypeIds.T_null, TypeConstants.NULL, new char[] { 'N' }); // N stands
																		// for
																		// null
																		// even
																		// if it
																		// is
																		// never
																		// internally
																		// used

	public final static BaseTypeBinding VOID = new BaseTypeBinding(
			TypeIds.T_void, TypeConstants.VOID, new char[] { 'V' });

	public final static BaseTypeBinding UNDEFINED = new BaseTypeBinding(
			TypeIds.T_undefined, TypeConstants.UNDEFINED, new char[] { 'U' }); // N
																				// stands
																				// for
																				// null
																				// even
																				// if
																				// it
																				// is
																				// never
																				// internally
																				// used

	public final static BaseTypeBinding ANY = new BaseTypeBinding(
			TypeIds.T_any, TypeConstants.ANY, new char[] { 'A' });
	public final static BaseTypeBinding UNKNOWN = new BaseTypeBinding(
			TypeIds.T_any, TypeConstants.ANY, new char[] { 'A' });

	/**
	 * Match a well-known type id to its binding
	 */
	public static final TypeBinding wellKnownType(Scope scope, int id) {
		switch (id) {
		case TypeIds.T_boolean:
			return TypeBinding.BOOLEAN;
		case TypeIds.T_char:
			return TypeBinding.CHAR;
		case TypeIds.T_short:
			return TypeBinding.SHORT;
		case TypeIds.T_double:
			return TypeBinding.DOUBLE;
		case TypeIds.T_float:
			return TypeBinding.FLOAT;
		case TypeIds.T_int:
			return TypeBinding.INT;
		case TypeIds.T_long:
			return TypeBinding.LONG;
		case TypeIds.T_JavaLangObject:
			return scope.getJavaLangObject();
		case TypeIds.T_JavaLangString:
			return scope.getJavaLangString();
		default:
			return null;
		}
	}

	/*
	 * Answer true if the receiver can be instantiated
	 */
	public boolean canBeInstantiated() {
		return !isBaseType();
	}

	/**
	 * Answer the receiver's constant pool name. NOTE: This method should only
	 * be used during/after code gen. e.g. 'java/lang/Object'
	 */
	public abstract char[] constantPoolName();

	public String debugName() {
		return new String(readableName());
	}

	/*
	 * Answer the receiver's dimensions - 0 for non-array types
	 */
	public int dimensions() {
		return 0;
	}

	/*
	 * Answer the receiver's enclosing type... null if the receiver is a top
	 * level type.
	 */
	public ReferenceBinding enclosingType() {
		return null;
	}

	/**
	 * Find supertype which erases to a given well-known type, or null if not
	 * found (using id avoids triggering the load of well-known type: 73740)
	 * NOTE: only works for erasures of well-known types, as random other types
	 * may share same id though being distincts.
	 * 
	 */
	public ReferenceBinding findSuperTypeErasingTo(int wellKnownErasureID,
			boolean erasureIsClass) {

		if (!(this instanceof ReferenceBinding))
			return null;
		ReferenceBinding reference = (ReferenceBinding) this;

		// do not allow type variables to match with erasures for free
		if (reference.id == wellKnownErasureID
				|| (this.id == wellKnownErasureID))
			return reference;

		ReferenceBinding currentType = reference;
		// iterate superclass to avoid recording interfaces if searched
		// supertype is class
		if (erasureIsClass) {
			while ((currentType = currentType.getSuperBinding()) != null) {
				if (currentType.id == wellKnownErasureID
						|| (currentType.id == wellKnownErasureID))
					return currentType;
			}
			return null;
		}
//		ReferenceBinding[] interfacesToVisit = null;
//		int nextPosition = 0;
//		do {
//		} while ((currentType = currentType.superclass()) != null);
//
//		for (int i = 0; i < nextPosition; i++) {
//			currentType = interfacesToVisit[i];
//			if (currentType.id == wellKnownErasureID
//					|| (currentType.id == wellKnownErasureID))
//				return currentType;
//		}
		return null;
	}

	/**
	 * Find supertype which erases to a given type, or null if not found
	 */
	public TypeBinding findSuperTypeWithSameErasure(TypeBinding otherType) {
		if (this == otherType)
			return this;
		if (otherType == null)
			return null;
		switch (kind()) {
		case Binding.ARRAY_TYPE:
			ArrayBinding arrayType = (ArrayBinding) this;
			int otherDim = otherType.dimensions();
			if (arrayType.dimensions != otherDim) {
				switch (otherType.id) {
				case TypeIds.T_JavaLangObject:
					return otherType;
				}
				if (otherDim < arrayType.dimensions
						&& otherType.leafComponentType().id == TypeIds.T_JavaLangObject) {
					return otherType; // X[][] has Object[] as an implicit
										// supertype
				}
				return null;
			}
			if (!(arrayType.leafComponentType instanceof ReferenceBinding))
				return null;
			TypeBinding leafSuperType = arrayType.leafComponentType
					.findSuperTypeWithSameErasure(otherType.leafComponentType());
			if (leafSuperType == null)
				return null;
			return arrayType.environment().createArrayType(leafSuperType,
					arrayType.dimensions);

		case Binding.TYPE:
			if (this == otherType || (this == otherType))
				return this;

			ReferenceBinding currentType = (ReferenceBinding) this;
			ReferenceBinding firstSuper = null;

			while ( ((currentType = currentType.getSuperBinding()) != null) && (currentType != firstSuper)) {
				if (currentType == otherType || (currentType == otherType))
					return currentType;
				if ( firstSuper == null ) firstSuper = currentType;
			}
			return null;
		}
		return null;
	}

	/**
	 * Returns the type to use for generic cast, or null if none required
	 */
	public TypeBinding genericCast(TypeBinding otherType) {
		if (this == otherType)
			return null;
		return otherType;
	}

	public abstract PackageBinding getPackage();

	public final boolean isAnonymousType() {
		return (this.tagBits & TagBits.IsAnonymousType) != 0;
	}

	public final boolean isObjectLiteralType() {
		return (this.tagBits & TagBits.IsObjectLiteralType) != 0;
	}

	/*
	 * Answer true if the receiver is an array
	 */
	public final boolean isArrayType() {
		return (this.tagBits & TagBits.IsArrayType) != 0;
	}

	/*
	 * Answer true if the receiver is a base type
	 */
	public final boolean isBaseType() {
		return (this.tagBits & TagBits.IsBaseType) != 0;
	}

	public boolean isBasicType() {
		if ((this.tagBits & TagBits.IsBaseType) != 0)
			return true;
		return id <= TypeIds.T_last_basic;
	}

	public boolean isClass() {
		return false;
	}

	/*
	 * Answer true if the receiver type can be assigned to the argument type
	 * (right)
	 */
	public abstract boolean isCompatibleWith(TypeBinding right);

	/**
	 * Returns true if a type is identical to another one, or for generic types,
	 * true if compared to its raw type.
	 */
	public boolean isEquivalentTo(TypeBinding otherType) {
		if (this == otherType)
			return true;
		if (otherType == null)
			return false;
		return false;
	}

	/*
	 * Answer true if the receiver's hierarchy has problems (always false for
	 * arrays & base types)
	 */
	public final boolean isHierarchyInconsistent() {
		return (this.tagBits & TagBits.HierarchyHasProblems) != 0;
	}

	/**
	 * Returns true if a type is intersecting with another one,
	 */
	public boolean isIntersectingWith(TypeBinding otherType) {
		return this == otherType;
	}

	public final boolean isLocalType() {
		return (this.tagBits & TagBits.IsLocalType) != 0;
	}

	public final boolean isMemberType() {
		return (this.tagBits & TagBits.IsMemberType) != 0;
	}

	public final boolean isNestedType() {
		return (this.tagBits & TagBits.IsNestedType) != 0;
	}

	public final boolean isAnyType() {
		return id == TypeIds.T_any;
	}

	public final boolean isNumericType() {
		switch (id) {
		case TypeIds.T_int:
		case TypeIds.T_float:
		case TypeIds.T_double:
		case TypeIds.T_short:
		case TypeIds.T_long:
		case TypeIds.T_char:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns true if the two types are statically known to be different at
	 * compile-time, e.g. a type variable is not provably known to be distinct
	 * from another type
	 */
	public boolean isProvablyDistinctFrom(TypeBinding otherType, int depth) {
		if (this == otherType)
			return false;
		if (depth > 1)
			return true;
		return this != otherType;
	}

	/**
	 * JLS(3) 4.7. Note: Foo<?>.Bar is also reifiable
	 */
	public boolean isReifiable() {

		TypeBinding leafType = leafComponentType();
		if (!(leafType instanceof ReferenceBinding))
			return true;
		ReferenceBinding current = (ReferenceBinding) leafType;
		do {
			if (current.isStatic())
				return true;
			if (current.isLocalType()) {
				// NestedTypeBinding nestedType = (NestedTypeBinding)
				// current.erasure();
				// if (nestedType.scope.methodScope().isStatic) return true;
				return true;
			}
		} while ((current = current.enclosingType()) != null);
		return true;
	}

	/**
	 * Returns true if a given type may be thrown
	 */
	public boolean isThrowable() {
		return false;
	}

	// JLS3: 4.5.1.1
	public boolean isTypeArgumentContainedBy(TypeBinding otherType) {
		if (this == otherType)
			return true;
		return false;
	}

	/**
	 * Returns true if the type is a subclass of java.lang.Error or
	 * java.lang.RuntimeException
	 */
	public boolean isUncheckedException(boolean includeSupertype) {
		return false;
	}

	/*
	 * API Answer the receiver's binding type from Binding.BindingID.
	 */
	public int kind() {
		return Binding.TYPE;
	}

	public TypeBinding leafComponentType() {
		return this;
	}

	/**
	 * Meant to be invoked on compatible types, to figure if unchecked
	 * conversion is necessary
	 */
	public boolean needsUncheckedConversion(TypeBinding targetType) {

		if (this == targetType)
			return false;
		targetType = targetType.leafComponentType();
		if (!(targetType instanceof ReferenceBinding))
			return false;

		TypeBinding currentType = this.leafComponentType();
		TypeBinding match = currentType
				.findSuperTypeWithSameErasure(targetType);
		if (!(match instanceof ReferenceBinding))
			return false;
		return false;
	}

	/**
	 * Answer the qualified name of the receiver's package separated by periods
	 * or an empty string if its the default package.
	 * 
	 * For example, {java.util}.
	 */

	public char[] qualifiedPackageName() {
		PackageBinding packageBinding = getPackage();
		return packageBinding == null
				|| packageBinding.compoundName == CharOperation.NO_CHAR_CHAR ? CharOperation.NO_CHAR
				: packageBinding.readableName();
	}

	/**
	 * Answer the source name for the type. In the case of member types, as the
	 * qualified name from its top level type. For example, for a member type N
	 * defined inside M & A: "A.M.N".
	 */

	public abstract char[] qualifiedSourceName();

	/**
	 * Answer the receiver classfile signature. Arrays & base types do not
	 * distinguish between signature() & constantPoolName(). NOTE: This method
	 * should only be used during/after code gen.
	 */
	public char[] signature() {
		return constantPoolName();
	}

	public abstract char[] sourceName();

	public void swapUnresolved(UnresolvedReferenceBinding unresolvedType,
			ReferenceBinding resolvedType, LookupEnvironment environment) {
		// subclasses must override if they wrap another type binding
	}

	public boolean isFunctionType() {
		return false;
	}

	public char[] getFileName() {
		return new char[] {};
	}

	/**
	 * Compare two type bindings. If all members of the other bindngs are a
	 * member of this type, return this type.
	 * 
	 * @param other
	 * @return
	 */
	public TypeBinding reconcileAnonymous(TypeBinding other) {
		return null;
	}

}
