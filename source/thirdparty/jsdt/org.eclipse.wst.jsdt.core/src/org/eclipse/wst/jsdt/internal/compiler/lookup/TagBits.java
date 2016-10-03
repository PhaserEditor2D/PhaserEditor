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

import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;

public interface TagBits {

	// Tag bits in the tagBits int of every TypeBinding
	long IsArrayType = ASTNode.Bit1;
	long IsBaseType = ASTNode.Bit2;
	long IsNestedType = ASTNode.Bit3;
	long IsMemberType = ASTNode.Bit4;
	long MemberTypeMask = IsNestedType | IsMemberType;
	long IsLocalType = ASTNode.Bit5;
	long LocalTypeMask = IsNestedType | IsLocalType;
	long IsAnonymousType = ASTNode.Bit6;
	long AnonymousTypeMask = LocalTypeMask | IsAnonymousType;
	long IsBinaryBinding = ASTNode.Bit7;

	long HasInconsistentHierarchy = ASTNode.Bit8; // for binary type binding only

	// for the type cycle hierarchy check used by ClassScope
	long BeginHierarchyCheck = ASTNode.Bit9;  // type
	long EndHierarchyCheck = ASTNode.Bit10; // type
	long ContainsNestedTypesInSignature = ASTNode.Bit10; // method

	// test bit to see if default abstract methods were computed
	long KnowsDefaultAbstractMethods = ASTNode.Bit11; // type

	long IsArgument = ASTNode.Bit11; // local
	long ClearPrivateModifier = ASTNode.Bit11; // constructor binding

	// test bits to see if parts of binary types are faulted
	long AreFieldsSorted = ASTNode.Bit13;
	long AreFieldsComplete = ASTNode.Bit14; // sorted and all resolved
	long AreMethodsSorted = ASTNode.Bit15;
	long AreMethodsComplete = ASTNode.Bit16; // sorted and all resolved

	// test bit to avoid asking a type for a member type (includes inherited member types)
	long HasNoMemberTypes = ASTNode.Bit17;

	// test bit to identify if the type's hierarchy is inconsistent
	long HierarchyHasProblems = ASTNode.Bit18;

	// used by BinaryTypeBinding
	long HasUnresolvedTypeVariables = ASTNode.Bit25;
	long HasUnresolvedSuperclass = ASTNode.Bit26;
	long HasUnresolvedSuperinterfaces = ASTNode.Bit27;
	long HasUnresolvedEnclosingType = ASTNode.Bit28;
	long HasUnresolvedMemberTypes = ASTNode.Bit29;

	long DefaultValueResolved = ASTNode.Bit52L;

	// set when type contains non-private constructor(s)
	long HasNonPrivateConstructor = ASTNode.Bit53L;
	long IsConstructor = ASTNode.Bit54L;
	
	long IsInferredJsDocType = ASTNode.Bit55L;
	long IsInferredType = ASTNode.Bit56L;
	long IsObjectLiteralType = ASTNode.Bit57L;
	
	
}
