/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.IDependent;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleLookupTable;

/*
Not all fields defined by this type (& its subclasses) are initialized when it is created.
Some are initialized only when needed.

Accessors have been provided for some public fields so all TypeBindings have the same API...
but access public fields directly whenever possible.
Non-public fields have accessors which should be used everywhere you expect the field to be initialized.

null is NOT a valid value for a non-public field... it just means the field is not initialized.
*/

abstract public class ReferenceBinding extends TypeBinding implements IDependent {

	public char[][] compoundName;
	public char[] sourceName;
	public int modifiers;
	public PackageBinding fPackage;
	char[] fileName;
	char[] constantPoolName;
	char[] signature;

	private SimpleLookupTable compatibleCache;

	private static final Comparator FIELD_COMPARATOR = new Comparator() {
		public int compare(Object o1, Object o2) {
			char[] n1 = ((FieldBinding) o1).name;
			char[] n2 = ((FieldBinding) o2).name;
			return ReferenceBinding.compare(n1, n2, n1.length, n2.length);
		}
	};
	private static final Comparator METHOD_COMPARATOR = new Comparator() {
		public int compare(Object o1, Object o2) {
			MethodBinding m1 = (MethodBinding) o1;
			MethodBinding m2 = (MethodBinding) o2;
			char[] s1 = m1.selector;
			char[] s2 = m2.selector;
			int c = ReferenceBinding.compare(s1, s2, s1 == null ? 0 : s1.length, s2 == null ? 0 : s2.length);
			return c == 0 ? m1.parameters.length - m2.parameters.length : c;
		}
	};

public static FieldBinding binarySearch(char[] name, FieldBinding[] sortedFields) {
	if (sortedFields == null)
		return null;
	int max = sortedFields.length;
	if (max == 0)
		return null;
	int left = 0, right = max - 1, nameLength = name.length;
	int mid = 0;
	char[] midName;
	while (left <= right) {
		mid = left + (right - left) /2;
		int compare = compare(name, midName = sortedFields[mid].name, nameLength, midName.length);
		if (compare < 0) {
			right = mid-1;
		} else if (compare > 0) {
			left = mid+1;
		} else {
			return sortedFields[mid];
		}
	}
	return null;
}

/**
 * Returns a combined range value representing: (start + (end<<32)), where start is the index of the first matching method
 * (remember methods are sorted alphabetically on selectors), and end is the index of last contiguous methods with same
 * selector.
 * -1 means no method got found
 * @param selector
 * @param sortedMethods
 * @return (start + (end<<32)) or -1 if no method found
 */
public static long binarySearch(char[] selector, MethodBinding[] sortedMethods) {
	if (sortedMethods == null || selector == null || selector.length == 0)
		return -1;
	int max = sortedMethods.length;
	if (max == 0)
		return -1;
	int left = 0, right = max - 1, selectorLength = selector.length;
	int mid = 0;
	char[] midSelector;
	while (left <= right) {
		mid = left + (right - left) /2;
		int compare = compare(selector, midSelector = sortedMethods[mid].selector, selectorLength, midSelector.length);
		if (compare < 0) {
			right = mid-1;
		} else if (compare > 0) {
			left = mid+1;
		} else {
			int start = mid, end = mid;
			// find first method with same selector
			while (start > left && CharOperation.equals(sortedMethods[start-1].selector, selector)){ start--; }
			// find last method with same selector
			while (end < right && CharOperation.equals(sortedMethods[end+1].selector, selector)){ end++; }
			return start + ((long)end<< 32);
		}
	}
	return -1;
}

/**
 * Compares two strings lexicographically.
 * The comparison is based on the Unicode value of each character in
 * the strings.
 *
 * @return  the value <code>0</code> if the str1 is equal to str2;
 *          a value less than <code>0</code> if str1
 *          is lexicographically less than str2;
 *          and a value greater than <code>0</code> if str1 is
 *          lexicographically greater than str2.
 */
static int compare(char[] str1, char[] str2, int len1, int len2) {
	int n= Math.min(len1, len2);
	int i= 0;
	while (n-- != 0) {
		char c1= str1[i];
		char c2= str2[i++];
		if (c1 != c2) {
			return c1 - c2;
		}
	}
	return len1 - len2;
}

/**
 * Sort the field array using a quicksort
 */
public static void sortFields(FieldBinding[] sortedFields, int left, int right) {
	Arrays.sort(sortedFields, left, right, FIELD_COMPARATOR);
}

/**
 * Sort the field array using a quicksort
 */
public static void sortMethods(MethodBinding[] sortedMethods, int left, int right) {
	Arrays.sort(sortedMethods, left, right, METHOD_COMPARATOR);
}

public FieldBinding[] availableFields() {
	return fields();
}
public MethodBinding[] availableMethods() {
	return methods();
}
/* Answer true if the receiver can be instantiated
*/
public boolean canBeInstantiated() {
	return (this.modifiers & (ClassFileConstants.AccAbstract)) == 0;
}
/* Answer true if the receiver is visible to the invocationPackage.
*/
public final boolean canBeSeenBy(PackageBinding invocationPackage) {
	if (isPublic()) return true;
	if (isPrivate()) return false;

	// isProtected() or isDefault()
	return invocationPackage == this.fPackage;
}
/* Answer true if the receiver is visible to the receiverType and the invocationType.
*/

public final boolean canBeSeenBy(ReferenceBinding receiverType, ReferenceBinding invocationType) {
	if (isPublic()) return true;

	if (invocationType == this && invocationType == receiverType) return true;

	if (isPrivate()) {
		// answer true if the receiverType is the receiver or its enclosingType
		// AND the invocationType and the receiver have a common enclosingType
		if (!(receiverType == this || receiverType == enclosingType())) {
			// special tolerance for type variable direct bounds
			return false;
		}
		

		if (invocationType != this) {
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = (ReferenceBinding)this;
			temp = outerDeclaringClass.enclosingType();
			while (temp != null) {
				outerDeclaringClass = temp;
				temp = temp.enclosingType();
			}
			if (outerInvocationType != outerDeclaringClass) return false;
		}
		return true;
	}

	// isDefault()
	if (invocationType.fPackage != this.fPackage) return false;

	ReferenceBinding currentType = receiverType;
	ReferenceBinding declaringClass = enclosingType() == null ? this : enclosingType();
	do {
		if (declaringClass == currentType) return true;
		PackageBinding currentPackage = currentType.fPackage;
		// package could be null for wildcards/intersection types, ignore and recurse in superclass
		if (currentPackage != null && currentPackage != this.fPackage) return false;
	} while ((currentType = currentType.getSuperBinding()) != null);
	return false;
}
/*
 * Answer true if the receiver is visible to the type provided by the scope.
 */
public final boolean canBeSeenBy(Scope scope) {
	if (isPublic()) return true;

	SourceTypeBinding invocationType = scope.enclosingSourceType();
	if (invocationType == this) return true;

	if (invocationType == null) // static import call
		return !isPrivate() && scope.getCurrentPackage() == this.fPackage;

	if (isPrivate()) {
		// answer true if the receiver and the invocationType have a common enclosingType
		// already know they are not the identical type
		ReferenceBinding outerInvocationType = invocationType;
		ReferenceBinding temp = outerInvocationType.enclosingType();
		while (temp != null) {
			outerInvocationType = temp;
			temp = temp.enclosingType();
		}

		ReferenceBinding outerDeclaringClass = (ReferenceBinding)this;
		temp = outerDeclaringClass.enclosingType();
		while (temp != null) {
			outerDeclaringClass = temp;
			temp = temp.enclosingType();
		}
		return outerInvocationType == outerDeclaringClass;
	}

	// isDefault()
	return invocationType.fPackage == this.fPackage;
}

/**
 * In case of problems, returns the closest match found. It may not be perfect match, but the
 * result of a best effort to improve fault-tolerance.
*/
public ReferenceBinding closestMatch() {
	return this; // by default, the closest match is the binding itself
}

public void computeId() {
	if (this.compoundName == null) return;
	
	switch (this.compoundName.length) {

		case 1 :
		case 2 :
//			if (!CharOperation.endsWith(fileName, TypeConstants.SYSTEMJS))	return;
//			if (!CharOperation.equals(TypeConstants.SYSTEMJS, this.compoundName[0]))
//				return;

//			// remaining types MUST be in java.*.*
//			if (!CharOperation.equals(TypeConstants.LANG, this.compoundName[1])) {
//				if (CharOperation.equals(TypeConstants.IO, this.compoundName[1])) {
//					if (CharOperation.equals(TypeConstants.JAVA_IO_PRINTSTREAM[2], this.compoundName[2]))
//						this.id = TypeIds.T_JavaIoPrintStream;
//					else if (CharOperation.equals(TypeConstants.JAVA_IO_SERIALIZABLE[2], this.compoundName[2]))
//					    this.id = TypeIds.T_JavaIoSerializable;
//					else if (CharOperation.equals(TypeConstants.JAVA_IO_EXTERNALIZABLE[2], this.compoundName[2]))
//					    this.id = TypeIds.T_JavaIoExternalizable;
//					else if (CharOperation.equals(TypeConstants.JAVA_IO_OBJECTSTREAMEXCEPTION[2], this.compoundName[2]))
//						this.id = TypeIds.T_JavaIoObjectStreamException;
//					else if (CharOperation.equals(TypeConstants.JAVA_IO_IOEXCEPTION[2], this.compoundName[2]))
//						this.id = TypeIds.T_JavaIoException;
//				} else if (CharOperation.equals(TypeConstants.UTIL, this.compoundName[1])
//						&& CharOperation.equals(TypeConstants.JAVA_UTIL_ITERATOR[2], this.compoundName[2])) {
//					this.id = TypeIds.T_JavaUtilIterator;
//				}
//				return;
//			}


			// remaining types MUST be in java.lang.*
			char[] typeName = (compoundName.length>1&&CharOperation.equals(compoundName[0], TypeConstants.SYSTEMJS))
					? this.compoundName[1] : this.compoundName[0];
			if (typeName.length == 0) return; // just to be safe
			switch (typeName[0]) {
//				case 'A' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ASSERTIONERROR[2]))
//						this.id = TypeIds.T_JavaLangAssertionError;
//					return;
				case 'B' :
					if (CharOperation.equals(typeName, TypeConstants.BOOLEAN_OBJECT[0]))
						this.id = TypeIds.T_boolean;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_BYTE[2]))
//						this.id = TypeIds.T_JavaLangByte;
					return;
//				case 'C' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CHARACTER[2]))
//						this.id = TypeIds.T_JavaLangCharacter;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CLASS[2]))
//						this.id = TypeIds.T_JavaLangClass;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CLASSNOTFOUNDEXCEPTION[2]))
//						this.id = TypeIds.T_JavaLangClassNotFoundException;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CLONEABLE[2]))
//					    this.id = TypeIds.T_JavaLangCloneable;
//					return;
//				case 'D' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_DOUBLE[2]))
//						this.id = TypeIds.T_JavaLangDouble;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_DEPRECATED[2]))
//						this.id = TypeIds.T_JavaLangDeprecated;
//					return;
				case 'E' :
					if (CharOperation.equals(typeName, TypeConstants.ERROR[0]))
						this.id = TypeIds.T_JavaLangError;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_EXCEPTION[2]))
//						this.id = TypeIds.T_JavaLangException;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ENUM[2]))
//						this.id = TypeIds.T_JavaLangEnum;
					return;
				case 'F' :
					if (CharOperation.equals(typeName, TypeConstants.FUNCTION[0]))
						this.id = TypeIds.T_function;
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_FLOAT[2]))
//						this.id = TypeIds.T_JavaLangFloat;
					return;
//				case 'I' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_INTEGER[2]))
//						this.id = TypeIds.T_JavaLangInteger;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ITERABLE[2]))
//						this.id = TypeIds.T_JavaLangIterable;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ILLEGALARGUMENTEXCEPTION[2]))
//						this.id = TypeIds.T_JavaLangIllegalArgumentException;
//					return;
//				case 'L' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_LONG[2]))
//						this.id = TypeIds.T_JavaLangLong;
//					return;
				case 'N' :
					if (CharOperation.equals(typeName, TypeConstants.NUMBER[0]))
						this.id = TypeIds.T_int;
					return;
				case 'O' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_OBJECT[0]))
						this.id = TypeIds.T_JavaLangObject;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_OVERRIDE[2]))
//						this.id = TypeIds.T_JavaLangOverride;
					return;
//				case 'R' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION[2]))
//						this.id = 	TypeIds.T_JavaLangRuntimeException;
//					break;
				case 'S' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_STRING[0]))
						this.id = TypeIds.T_JavaLangString;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_STRINGBUFFER[2]))
//						this.id = TypeIds.T_JavaLangStringBuffer;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_STRINGBUILDER[2]))
//						this.id = TypeIds.T_JavaLangStringBuilder;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SYSTEM[2]))
//						this.id = TypeIds.T_JavaLangSystem;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SHORT[2]))
//						this.id = TypeIds.T_JavaLangShort;
//					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SUPPRESSWARNINGS[2]))
//						this.id = TypeIds.T_JavaLangSuppressWarnings;
					return;
//				case 'T' :
//					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_THROWABLE[2]))
//						this.id = TypeIds.T_JavaLangThrowable;
//					return;
				case 'V' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_VOID[2]))
						this.id = TypeIds.T_JavaLangVoid;
					return;
			}
		break;

		case 4:
			if (!CharOperation.equals(TypeConstants.JAVA, this.compoundName[0]))
				return;
			if (!CharOperation.equals(TypeConstants.LANG, this.compoundName[1]))
				return;
			char[] packageName = this.compoundName[2];
			if (packageName.length == 0) return; // just to be safe
			typeName = this.compoundName[3];
			if (typeName.length == 0) return; // just to be safe
			if (CharOperation.equals(packageName, TypeConstants.REFLECT)) {
				return;
			}
			break;
	}
}
/*
 * p.X<T extends Y & I, U extends Y> {} -> Lp/X<TT;TU;>;
 */
public char[] computeUniqueKey(boolean isLeaf) {
	if (!isLeaf) return signature();
	return signature();
}
/* Answer the receiver's constant pool name.
*
* NOTE: This method should only be used during/after code gen.
*/
public char[] constantPoolName() /* java/lang/Object */ {
	if (this.constantPoolName != null) return this.constantPoolName;

	return this.constantPoolName = CharOperation.concatWith(this.compoundName, '/');
}
public String debugName() {
	return (this.compoundName != null) ? new String(readableName()) : "UNNAMED TYPE"; //$NON-NLS-1$
}
public final int depth() {
	int depth = 0;
	ReferenceBinding current = this;
	while ((current = current.enclosingType()) != null)
		depth++;
	return depth;
}

public final ReferenceBinding enclosingTypeAt(int relativeDepth) {
	ReferenceBinding current = this;
	while (relativeDepth-- > 0 && current != null)
		current = current.enclosingType();
	return current;
}

public int fieldCount() {
	return fields().length;
}

public FieldBinding[] fields() {
	return Binding.NO_FIELDS;
}

public final int getAccessFlags() {
	return this.modifiers & ExtraCompilerModifiers.AccJustFlag;
}
public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
	return null;
}
public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
	return null;
}
public FieldBinding getField(char[] fieldName, boolean needResolve) {
	return null;
}
/**
 * @see org.eclipse.wst.jsdt.internal.compiler.env.IDependent#getFileName()
 */
public char[] getFileName() {
	return this.fileName;
}
public ReferenceBinding getMemberType(char[] typeName) {
	ReferenceBinding[] memberTypes = memberTypes();
	for (int i = memberTypes.length; --i >= 0;)
		if (CharOperation.equals(memberTypes[i].sourceName, typeName))
			return memberTypes[i];
	return null;
}

public MethodBinding[] getMethods(char[] selector) {
	return Binding.NO_METHODS;
}

public PackageBinding getPackage() {
	return this.fPackage;
}

public int hashCode() {
	// ensure ReferenceBindings hash to the same posiiton as UnresolvedReferenceBindings so they can be replaced without rehashing
	// ALL ReferenceBindings are unique when created so equals() is the same as ==
	return (this.compoundName == null || this.compoundName.length == 0)
		? super.hashCode()
		: CharOperation.hashCode(this.compoundName[this.compoundName.length - 1]);
}

/**
 * Returns true if the two types have an incompatible common supertype,
 * e.g. List<String> and List<Integer>
 */
public boolean hasIncompatibleSuperType(ReferenceBinding otherType) {

    if (this == otherType) return false;

	ReferenceBinding[] interfacesToVisit = null;
	int nextPosition = 0;
    ReferenceBinding currentType = this;
	TypeBinding match;
	do {
		match = otherType.findSuperTypeWithSameErasure(currentType);
		if (match != null && !match.isIntersectingWith(currentType))
			return true;
	} while ((currentType = currentType.getSuperBinding()) != null);

//	for (int i = 0; i < nextPosition; i++) {
//		currentType = interfacesToVisit[i];
//		if (currentType == otherType) return false;
//		match = otherType.findSuperTypeWithSameErasure(currentType);
//		if (match != null && !match.isIntersectingWith(currentType))
//			return true;
//	}
	return false;
}
public boolean hasMemberTypes() {
    return false;
}
public final boolean hasRestrictedAccess() {
	return (this.modifiers & ExtraCompilerModifiers.AccRestrictedAccess) != 0;
}

// Internal method... assume its only sent to classes NOT interfaces
boolean implementsMethod(MethodBinding method) {
	char[] selector = method.selector;
	ReferenceBinding type = this;
	while (type != null) {
		MethodBinding[] methods = type.methods();
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			for (int i = start; i <= end; i++) {
				if (methods[i].areParametersEqual(method))
					return true;
			}
		}
		type = type.getSuperBinding();
	}
	return false;
}

public final boolean isBinaryBinding() {
	return (this.tagBits & TagBits.IsBinaryBinding) != 0;
}

public boolean isClass() {
	return true;
}

/**
 * Answer true if the receiver type can be assigned to the argument type (right)
 * In addition to improving performance, caching also ensures there is no infinite regression
 * since per nature, the compatibility check is recursive through parameterized type arguments (122775)
 */
public boolean isCompatibleWith(TypeBinding otherType) {

	if (otherType == this)
		return true;
	if (otherType.id == TypeIds.T_JavaLangObject || otherType.id == TypeIds.T_any)
		return true;
	Object result;
	if (this.compatibleCache == null) {
		this.compatibleCache = new SimpleLookupTable(3);
		result = null;
	} else {
		result = this.compatibleCache.get(otherType);
		if (result != null) {
			return result == Boolean.TRUE;
		}
	}
	boolean cacheThisBinding = true;
	if(this instanceof ProblemReferenceBinding) {
		cacheThisBinding = false;
	}
	if(cacheThisBinding) {
		this.compatibleCache.put(otherType, Boolean.FALSE); // protect from recursive call
	}
	if (isCompatibleWith0(otherType)) {
		if(cacheThisBinding) {
			this.compatibleCache.put(otherType, Boolean.TRUE);
		}
		return true;
	}
	return false;
}

/**
 * Answer true if the receiver type can be assigned to the argument type (right)
 */
private boolean isCompatibleWith0(TypeBinding otherType) {
	if (otherType == this)
		return true;
	if (otherType.id == TypeIds.T_JavaLangObject)
		return true;
	// equivalence may allow compatibility with array type through wildcard
	// bound
	if (this.isEquivalentTo(otherType))
		return true;
	switch (otherType.kind()) {
		case Binding.TYPE :
			ReferenceBinding otherReferenceType = (ReferenceBinding) otherType;
			if(Arrays.equals(this.compoundName,otherReferenceType.compoundName) && Arrays.equals(this.fileName, otherReferenceType.fileName)) {
				return true;
			}


			if ( otherReferenceType.isSuperclassOf(this))
				return true;
			return (otherReferenceType.isAnonymousType()  && this.isSuperclassOf(otherReferenceType));
		case Binding.ARRAY_TYPE:
		   return this==((ArrayBinding)otherType).referenceBinding;
		default :
			return false;
	}
}

/**
 * Answer true if the receiver has default visibility
 */
public final boolean isDefault() {
	return (this.modifiers & (ClassFileConstants.AccPublic | ClassFileConstants.AccProtected | ClassFileConstants.AccPrivate)) == 0;
}

/**
 * Answer true if the receiver is a deprecated type
 */
public final boolean isDeprecated() {
	return (this.modifiers & ClassFileConstants.AccDeprecated) != 0;
}

/**
 * Returns true if the type hierarchy is being connected
 */
public boolean isHierarchyBeingConnected() {
	return (this.tagBits & TagBits.EndHierarchyCheck) == 0 && (this.tagBits & TagBits.BeginHierarchyCheck) != 0;
}

/**
 * Answer true if the receiver has private visibility
 */
public final boolean isPrivate() {
	return (this.modifiers & ClassFileConstants.AccPrivate) != 0;
}

/**
 * Answer true if the receiver has public visibility
 */
public final boolean isPublic() {
	return (this.modifiers & ClassFileConstants.AccPublic) != 0;
}

/**
 * Answer true if the receiver is a static member type (or toplevel)
 */
public final boolean isStatic() {
	return (this.modifiers & (ClassFileConstants.AccStatic)) != 0 || (this.tagBits & TagBits.IsNestedType) == 0;
}
/**
 * Answer true if all float operations must adher to IEEE 754 float/double rules
 */
public final boolean isStrictfp() {
	return (this.modifiers & ClassFileConstants.AccStrictfp) != 0;
}

/**
 * Answer true if the receiver is in the superclass hierarchy of aType
 * NOTE: Object.isSuperclassOf(Object) -> false
 */
public boolean isSuperclassOf(ReferenceBinding otherType) {
	while ((otherType = otherType.getSuperBinding()) != null) {
		if (otherType.isEquivalentTo(this)) return true;
	}
	return false;
}
/**
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding#isThrowable()
 */
public boolean isThrowable() {
	ReferenceBinding current = this;
	do {
		switch (current.id) {
			case TypeIds.T_JavaLangThrowable :
			case TypeIds.T_JavaLangError :
			case TypeIds.T_JavaLangRuntimeException :
			case TypeIds.T_JavaLangException :
				return true;
		}
	} while ((current = current.getSuperBinding()) != null);
	return false;
}
/**
 * JLS 11.5 ensures that Throwable, Exception, RuntimeException and Error are directly connected.
 * (Throwable<- Exception <- RumtimeException, Throwable <- Error). Thus no need to check #isCompatibleWith
 * but rather check in type IDs so as to avoid some eager class loading for JCL writers.
 * When 'includeSupertype' is true, answers true if the given type can be a supertype of some unchecked exception
 * type (i.e. Throwable or Exception).
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding#isUncheckedException(boolean)
 */
public boolean isUncheckedException(boolean includeSupertype) {
	if (JavaScriptCore.IS_ECMASCRIPT4)	// no checked exceptions for now
	{
	switch (this.id) {
			case TypeIds.T_JavaLangError :
			case TypeIds.T_JavaLangRuntimeException :
				return true;
			case TypeIds.T_JavaLangThrowable :
			case TypeIds.T_JavaLangException :
				return includeSupertype;
	}
	ReferenceBinding current = this;
	while ((current = current.getSuperBinding()) != null) {
		switch (current.id) {
			case TypeIds.T_JavaLangError :
			case TypeIds.T_JavaLangRuntimeException :
				return true;
			case TypeIds.T_JavaLangThrowable :
			case TypeIds.T_JavaLangException :
				return false;
		}
	}
	return false;
	}
	else
		return true;
}
/**
 * Answer true if the receiver has private visibility and is used locally
 */
public final boolean isUsed() {
	return (this.modifiers & ExtraCompilerModifiers.AccLocallyUsed) != 0;
}

/* Answer true if the receiver is deprecated (or any of its enclosing types)
*/
public boolean isViewedAsDeprecated() {
	return (this.modifiers & (ClassFileConstants.AccDeprecated | ExtraCompilerModifiers.AccDeprecatedImplicitly)) != 0;
}
public ReferenceBinding[] memberTypes() {
	return Binding.NO_MEMBER_TYPES;
}
public MethodBinding[] methods() {
	return Binding.NO_METHODS;
}
public final ReferenceBinding outermostEnclosingType() {
	ReferenceBinding current = this;
	while (true) {
		ReferenceBinding last = current;
		if ((current = current.enclosingType()) == null)
			return last;
	}
}
/**
* Answer the source name for the type.
* In the case of member types, as the qualified name from its top level type.
* For example, for a member type N defined inside M & A: "A.M.N".
*/

public char[] qualifiedSourceName() {
//	if (isMemberType())
//		return CharOperation.concat(enclosingType().qualifiedSourceName(), sourceName(), '.');
	return sourceName();
}

/* Answer the receiver's signature.
*
* NOTE: This method should only be used during/after code gen.
*/

public char[] readableName() /*java.lang.Object,  p.X<T> */ {
    char[] readableName;
	if (isMemberType()) {
		readableName = CharOperation.concat(enclosingType().readableName(), this.sourceName, '.');
	} else {
		readableName = CharOperation.concatWith(this.compoundName, '.');
	}
	return readableName;
}

public char[] shortReadableName() /*Object*/ {
    char[] shortReadableName;
	if (isMemberType()) {
		shortReadableName = CharOperation.concat(enclosingType().shortReadableName(), this.sourceName, '.');
	} else {
		shortReadableName = this.sourceName;
	}
	return shortReadableName;
}
public char[] signature() /* Ljava/lang/Object; */ {
	if (this.signature != null)
		return this.signature;

	return this.signature = CharOperation.concat('L', constantPoolName(), ';');
}
public char[] sourceName() {
	return this.sourceName;
}

public ReferenceBinding getSuperBinding() {
	return null;
}
public InferredType getInferredType() {

	return null;
}

MethodBinding[] unResolvedMethods() { // for the MethodVerifier so it doesn't resolve types
	return methods();
}

public void cleanup()
{

}
}
