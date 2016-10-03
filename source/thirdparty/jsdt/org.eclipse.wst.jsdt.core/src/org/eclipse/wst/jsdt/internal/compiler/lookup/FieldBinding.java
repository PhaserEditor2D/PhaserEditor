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
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;

public class FieldBinding extends VariableBinding {
	public ReferenceBinding declaringClass;
protected FieldBinding() {
	super(null, null, 0);
	// for creating problem field
}
public FieldBinding(char[] name, TypeBinding type, int modifiers, ReferenceBinding declaringClass) {
	super(name, type, modifiers);
	this.declaringClass = declaringClass;
}
public FieldBinding(InferredAttribute field, TypeBinding type, int modifiers, ReferenceBinding declaringClass) {
	this(field.name, type, modifiers, declaringClass);
	field.binding = this; // record binding in declaration
}
// special API used to change field declaring class for runtime visibility check
public FieldBinding(FieldBinding initialFieldBinding, ReferenceBinding declaringClass) {
	super(initialFieldBinding.name, initialFieldBinding.type, initialFieldBinding.modifiers);
	this.declaringClass = declaringClass;
	this.id = initialFieldBinding.id;
}
/* API
* Answer the receiver's binding type from Binding.BindingID.
*/

public final int kind() {
	return FIELD;
}
/* Answer true if the receiver is visible to the invocationPackage.
*/

public final boolean canBeSeenBy(PackageBinding invocationPackage) {
	if (isPublic()) return true;
	if (isPrivate()) return false;

	// isProtected() or isDefault()
	return invocationPackage == declaringClass.getPackage();
}
/* Answer true if the receiver is visible to the type provided by the scope.
* InvocationSite implements isSuperAccess() to provide additional information
* if the receiver is protected.
*
* NOTE: Cannot invoke this method with a compilation unit scope.
*/

public final boolean canBeSeenBy(TypeBinding receiverType, InvocationSite invocationSite, Scope scope) {
	if (isPublic() || !JavaScriptCore.IS_ECMASCRIPT4) {
		return true;
	}

	SourceTypeBinding invocationType = scope.enclosingSourceType();
	if(receiverType instanceof SourceTypeBinding) {
		SourceTypeBinding receiverSourceType = (SourceTypeBinding)receiverType;
		if (receiverSourceType.isLinkedType(declaringClass)  && receiverSourceType.isLinkedType(invocationType)) {
			return true;
		}
	}

	if (invocationType == null) // static import call
		return !isPrivate() && scope.getCurrentPackage() == declaringClass.fPackage;

	if (isProtected()) {
		// answer true if the invocationType is the declaringClass or they are in the same package
		// OR the invocationType is a subclass of the declaringClass
		//    AND the receiverType is the invocationType or its subclass
		//    OR the method is a static method accessed directly through a type
		//    OR previous assertions are true for one of the enclosing type
		if (invocationType == declaringClass) return true;
		if (invocationType.fPackage == declaringClass.fPackage) return true;

		ReferenceBinding currentType = invocationType;
		int depth = 0;
		ReferenceBinding receiverErasure = (ReferenceBinding)receiverType;
		ReferenceBinding declaringErasure = declaringClass;
		do {
			if (currentType.findSuperTypeWithSameErasure(declaringErasure) != null) {
				if (invocationSite.isSuperAccess())
					return true;
				// receiverType can be an array binding in one case... see if you can change it
				if (receiverType instanceof ArrayBinding)
					return false;
				if (isStatic()) {
					if (depth > 0) invocationSite.setDepth(depth);
					return true; // see 1FMEPDL - return invocationSite.isTypeAccess();
				}
				if (currentType == receiverErasure || receiverErasure.findSuperTypeWithSameErasure(currentType) != null) {
					if (depth > 0) invocationSite.setDepth(depth);
					return true;
				}
			}
			depth++;
			currentType = currentType.enclosingType();
		} while (currentType != null);
		return false;
	}

	if (isPrivate()) {
		// answer true if the receiverType is the declaringClass
		// AND the invocationType and the declaringClass have a common enclosingType
		
		if (receiverType != declaringClass) {
			// special tolerance for type variable direct bounds
			return false;
		}
		

		if (invocationType != declaringClass) {
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = (ReferenceBinding) declaringClass;
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
	PackageBinding declaringPackage = declaringClass.fPackage;
	if (invocationType.fPackage != declaringPackage) return false;

	// receiverType can be an array binding in one case... see if you can change it
	if (receiverType instanceof ArrayBinding)
		return false;
	ReferenceBinding currentType = (ReferenceBinding) receiverType;
	do {
		if (declaringClass == currentType) return true;
		PackageBinding currentPackage = currentType.fPackage;
		// package could be null for wildcards/intersection types, ignore and recurse in superclass
		if (currentPackage != null && currentPackage != declaringPackage) return false;
	} while ((currentType = currentType.getSuperBinding()) != null);
	return false;
}
/*
 * declaringUniqueKey dot fieldName ) returnTypeUniqueKey
 * p.X { X<T> x} --> Lp/X;.x)p/X<TT;>;
 */
public char[] computeUniqueKey(boolean isLeaf) {
	// declaring key
	char[] declaringKey =
		this.declaringClass == null /*case of length field for an array*/
			? CharOperation.NO_CHAR
			: this.declaringClass.computeUniqueKey(false/*not a leaf*/);
	int declaringLength = declaringKey.length;

	// name
	int nameLength = this.name.length;

	// return type
	char[] returnTypeKey = this.type == null ? new char[] {'V'} : this.type.computeUniqueKey(false/*not a leaf*/);
	int returnTypeLength = returnTypeKey.length;

	char[] uniqueKey = new char[declaringLength + 1 + nameLength + 1 + returnTypeLength];
	int index = 0;
	System.arraycopy(declaringKey, 0, uniqueKey, index, declaringLength);
	index += declaringLength;
	uniqueKey[index++] = '.';
	System.arraycopy(this.name, 0, uniqueKey, index, nameLength);
	index += nameLength;
	uniqueKey[index++] = ')';
	System.arraycopy(returnTypeKey, 0, uniqueKey, index, returnTypeLength);
	return uniqueKey;
}

public final int getAccessFlags() {
	return modifiers & ExtraCompilerModifiers.AccJustFlag;
}


/* Answer true if the receiver has default visibility
*/

public final boolean isDefault() {
	return !isPublic() && !isProtected() && !isPrivate();
}
/* Answer true if the receiver is a deprecated field
*/

public final boolean isDeprecated() {
	return (modifiers & ClassFileConstants.AccDeprecated) != 0;
}
/* Answer true if the receiver has private visibility
*/

public final boolean isPrivate() {
	return (modifiers & ClassFileConstants.AccPrivate) != 0;
}
/* Answer true if the receiver has private visibility and is used locally
*/

public final boolean isUsed() {
	return (modifiers & ExtraCompilerModifiers.AccLocallyUsed) != 0;
}
/* Answer true if the receiver has protected visibility
*/

public final boolean isProtected() {
	return (modifiers & ClassFileConstants.AccProtected) != 0;
}
/* Answer true if the receiver has public visibility
*/

public final boolean isPublic() {
	return (modifiers & ClassFileConstants.AccPublic) != 0;
}
/* Answer true if the receiver is a static field
*/

public final boolean isStatic() {
	return (modifiers & ClassFileConstants.AccStatic) != 0;
}
/* Answer true if the receiver's declaring type is deprecated (or any of its enclosing types)
*/

public final boolean isViewedAsDeprecated() {
	return (modifiers & (ClassFileConstants.AccDeprecated | ExtraCompilerModifiers.AccDeprecatedImplicitly)) != 0;
}
/**
 * Returns the original field (as opposed to parameterized instances)
 */
public FieldBinding original() {
	return this;
}
public  boolean isFor(AbstractVariableDeclaration variableDeclaration)
{
	return false;
}
//public FieldDeclaration sourceField() {
//	SourceTypeBinding sourceType;
//	try {
//		sourceType = (SourceTypeBinding) declaringClass;
//	} catch (ClassCastException e) {
//		return null;
//	}
//
//	FieldDeclaration[] fields = sourceType.scope.referenceContext.fields;
//	if (fields != null) {
//		for (int i = fields.length; --i >= 0;)
//			if (this == fields[i].binding)
//				return fields[i];
//	}
//	return null;
//}
}
