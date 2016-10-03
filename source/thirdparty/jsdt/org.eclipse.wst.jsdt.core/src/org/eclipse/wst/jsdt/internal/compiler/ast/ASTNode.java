/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt McCutchen
 *     		Partial fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=122995.
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.DelegateASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;

public abstract class ASTNode implements TypeConstants, TypeIds, IASTNode {

	public int sourceStart, sourceEnd;

	// storage for internal flags (32 bits)						BIT USAGE
	public final static int Bit1 = 0x1;					// return type (operator) | name reference kind (name ref) | add assertion (type decl) | useful empty statement (empty statement)
	public final static int Bit2 = 0x2;					// return type (operator) | name reference kind (name ref) | has local type (type, method, field decl)
	public final static int Bit3 = 0x4;					// return type (operator) | name reference kind (name ref)
	public final static int Bit4 = 0x8;					// return type (operator) | first assignment to local (name ref,local decl) | undocumented empty block (block, type and method decl)
	public final static int Bit5 = 0x10;				// value for return (expression) | has all method bodies (unit) | supertype ref (type ref) | resolved (field decl)
	public final static int Bit6 = 0x20;				// depth (name ref, msg) | ignore need cast check (cast expression) | error in signature (method declaration/ initializer)
	public final static int Bit7 = 0x40;				// depth (name ref, msg) | operator (operator) | need runtime checkcast (cast expression) | label used (labelStatement) | needFreeReturn (AbstractMethodDeclaration)
	public final static int Bit8 = 0x80;				// depth (name ref, msg) | operator (operator) | unsafe cast (cast expression) | is default constructor (constructor declaration)
	public final static int Bit9 = 0x100;				// depth (name ref, msg) | operator (operator) | is local type (type decl)
	public final static int Bit10= 0x200;				// depth (name ref, msg) | operator (operator) | is anonymous type (type decl)
	public final static int Bit11 = 0x400;				// depth (name ref, msg) | operator (operator) | is member type (type decl)
	public final static int Bit12 = 0x800;				// depth (name ref, msg) | operator (operator) | has abstract methods (type decl)
	public final static int Bit13 = 0x1000;				// depth (name ref, msg) | is secondary type (type decl)
	public final static int Bit14 = 0x2000;				// strictly assigned (reference lhs) | discard enclosing instance (explicit constr call) | hasBeenGenerated (type decl)
	public final static int Bit15 = 0x4000;				// is unnecessary cast (expression) | implicit this (this ref) | is varargs (type ref) | isSubRoutineEscaping (try statement) | superAccess (javadoc allocation expression/javadoc message send/javadoc return statement)
	public final static int Bit16 = 0x8000;				// in javadoc comment (name ref, type ref, msg)
	public final static int Bit17 = 0x10000;			// compound assigned (reference lhs)
	public final static int Bit18 = 0x20000;			// non null (expression) | onDemand (import reference)
	public final static int Bit19 = 0x40000;			// didResolve (parameterized qualified type ref/parameterized single type ref)  | empty (javadoc return statement)
	public final static int Bit20 = 0x80000;
	public final static int Bit21 = 0x100000;
	public final static int Bit22 = 0x200000;			// parenthesis count (expression) | used (import reference)
	public final static int Bit23 = 0x400000;			// parenthesis count (expression)
	public final static int Bit24 = 0x800000;			// parenthesis count (expression)
	public final static int Bit25 = 0x1000000;			// parenthesis count (expression)
	public final static int Bit26 = 0x2000000;			// parenthesis count (expression)
	public final static int Bit27 = 0x4000000;			// parenthesis count (expression)
	public final static int Bit28 = 0x8000000;			// parenthesis count (expression)
	public final static int Bit29 = 0x10000000;			// parenthesis count (expression)
	public final static int Bit30 = 0x20000000;			// elseif (if statement) | try block exit (try statement) | fall-through (case statement) | ignore no effect assign (expression ref) | needScope (for statement) | isAnySubRoutineEscaping (return statement) | blockExit (synchronized statement)
	public final static int Bit31 = 0x40000000;			// local declaration reachable (local decl) | ignore raw type check (type ref) | discard entire assignment (assignment) | isSynchronized (return statement) | thenExit (if statement)
	public final static int Bit32 = 0x80000000;			// reachable (statement)

	public final static long Bit32L = 0x80000000L;
	public final static long Bit33L = 0x100000000L;
	public final static long Bit34L = 0x200000000L;
	public final static long Bit35L = 0x400000000L;
	public final static long Bit36L = 0x800000000L;
	public final static long Bit37L = 0x1000000000L;
	public final static long Bit38L = 0x2000000000L;
	public final static long Bit39L = 0x4000000000L;
	public final static long Bit40L = 0x8000000000L;
	public final static long Bit41L = 0x10000000000L;
	public final static long Bit42L = 0x20000000000L;
	public final static long Bit43L = 0x40000000000L;
	public final static long Bit44L = 0x80000000000L;
	public final static long Bit45L = 0x100000000000L;
	public final static long Bit46L = 0x200000000000L;
	public final static long Bit47L = 0x400000000000L;
	public final static long Bit48L = 0x800000000000L;
	public final static long Bit49L = 0x1000000000000L;
	public final static long Bit50L = 0x2000000000000L;
	public final static long Bit51L = 0x4000000000000L;
	public final static long Bit52L = 0x8000000000000L;
	public final static long Bit53L = 0x10000000000000L;
	public final static long Bit54L = 0x20000000000000L;
	public final static long Bit55L = 0x40000000000000L;
	public final static long Bit56L = 0x80000000000000L;
	public final static long Bit57L = 0x100000000000000L;
	public final static long Bit58L = 0x200000000000000L;
	public final static long Bit59L = 0x400000000000000L;
	public final static long Bit60L = 0x800000000000000L;
	public final static long Bit61L = 0x1000000000000000L;
	public final static long Bit62L = 0x2000000000000000L;
	public final static long Bit63L = 0x4000000000000000L;
	public final static long Bit64L = 0x8000000000000000L;

	public int bits = IsReachable; 				// reachable by default

	// for operators
	public static final int ReturnTypeIDMASK = Bit1|Bit2|Bit3|Bit4;
	public static final int OperatorSHIFT = 6;	// Bit7 -> Bit12
	public static final int OperatorMASK = Bit7|Bit8|Bit9|Bit10|Bit11|Bit12; // 6 bits for operator ID

	// for binary expressions
	public static final int IsReturnedValue = Bit5;

	// for name references
	public static final int RestrictiveFlagMASK = Bit1|Bit2|Bit3|Bit4;

	// for name refs or local decls
	public static final int FirstAssignmentToLocal = Bit4;

	// for this reference
	public static final int IsImplicitThis = Bit15;

	// for single name references
	public static final int DepthSHIFT = 5;	// Bit6 -> Bit13
	public static final int DepthMASK = Bit6|Bit7|Bit8|Bit9|Bit10|Bit11|Bit12|Bit13; // 8 bits for actual depth value (max. 255)

	// for statements
	public static final int IsReachable = Bit32;
	public static final int LabelUsed = Bit7;
	public static final int DocumentedFallthrough = Bit30;

	// local decls
	public static final int IsLocalDeclarationReachable = Bit31;

	// try statements
	public static final int IsSubRoutineEscaping = Bit15;
	public static final int IsTryBlockExiting = Bit30;

	// for type declaration
	public static final int ContainsAssertion = Bit1;
	public static final int IsLocalType = Bit9;
	public static final int IsAnonymousType = Bit10; // used to test for anonymous
	public static final int IsMemberType = Bit11; // local member do not know it is local at parse time (need to look at binding)
	public static final int HasAbstractMethods = Bit12; // used to promote abstract enums
	public static final int IsSecondaryType = Bit13; // used to test for secondary
	public static final int HasBeenGenerated = Bit14;

	// for type, method and field declarations
	public static final int HasLocalType = Bit2; // cannot conflict with AddAssertionMASK
	public static final int HasBeenResolved = Bit5; // field decl only (to handle forward references)

	// for expression
	public static final int ParenthesizedSHIFT = 21; // Bit22 -> Bit29
	public static final int ParenthesizedMASK = Bit22|Bit23|Bit24|Bit25|Bit26|Bit27|Bit28|Bit29; // 8 bits for parenthesis count value (max. 255)
	public static final int IgnoreNoEffectAssignCheck = Bit30;

	// for references on lhs of assignment
	public static final int IsStrictlyAssigned = Bit14; // set only for true assignments, as opposed to compound ones
	public static final int IsCompoundAssigned = Bit17; // set only for compound assignments, as opposed to other ones

	// for explicit constructor call
	public static final int DiscardEnclosingInstance = Bit14; // used for codegen

	// for empty statement
	public static final int IsUsefulEmptyStatement = Bit1;

	// for block and method declaration
	public static final int UndocumentedEmptyBlock = Bit4;
	public static final int OverridingMethodWithSupercall = Bit5;

	// for initializer and method declaration
	public static final int ErrorInSignature = Bit6;

	// for abstract method declaration
	public static final int NeedFreeReturn = Bit7; // abstract method declaration

	// for constructor declaration
	public static final int IsDefaultConstructor = Bit8;

	// for compilation unit
	public static final int HasAllMethodBodies = Bit5;
	public static final int IsImplicitUnit = Bit1;

	// for references in Javadoc comments
	public static final int InsideJavadoc = Bit16;

	// for javadoc allocation expression/javadoc message send/javadoc return statement
	public static final int SuperAccess = Bit15;

	// for javadoc return statement
	public static final int Empty = Bit19;

	// for if statement
	public static final int IsElseIfStatement = Bit30;
	public static final int ThenExit = Bit31;

	// for type reference
	public static final int IsSuperType = Bit5;
	public static final int IsVarArgs = Bit15;
	public static final int IgnoreRawTypeCheck = Bit31;

	// for null reference analysis
	public static final int IsNonNull = Bit18;

	// for for statement
	public static final int NeededScope = Bit30;

	// for import reference
	public static final int OnDemand = Bit18;
	public static final int Used = Bit2;
	public static final int IsFileImport = Bit5;

	// for parameterized qualified/single type ref
	public static final int DidResolve = Bit19;

	// for return statement
	public static final int IsAnySubRoutineEscaping = Bit30;

	// for synchronized statement
	public static final int BlockExit = Bit30;

	// for method decls and var decls
	public static final int IsInferredType = Bit14;
	public static final int IsInferredJsDocType = Bit15;
	
	// constants used when checking invocation arguments
	public static final int INVOCATION_ARGUMENT_OK = 0;
	public static final int INVOCATION_ARGUMENT_UNCHECKED = 1;

	public ASTNode() {

		super();
	}
	private static int checkInvocationArgument(BlockScope scope, Expression argument, TypeBinding parameterType, TypeBinding argumentType, TypeBinding originalParameterType) {
		TypeBinding checkedParameterType = originalParameterType == null ? parameterType : originalParameterType;
		if (argumentType != checkedParameterType && argumentType.needsUncheckedConversion(checkedParameterType)) {
			return INVOCATION_ARGUMENT_UNCHECKED;
		}
		return INVOCATION_ARGUMENT_OK;
	}
	public static void checkInvocationArguments(BlockScope scope, Expression receiver, TypeBinding receiverType, MethodBinding method, Expression[] arguments, TypeBinding[] argumentTypes, boolean argsContainCast, InvocationSite invocationSite) {
		TypeBinding[] params = method.parameters;
		int paramLength = params.length;

		int invocationStatus = INVOCATION_ARGUMENT_OK;
		if (arguments != null) {
			if (method.isVarargs()) {
				// 4 possibilities exist for a call to the vararg method foo(int i, long ... value) : foo(1), foo(1, 2), foo(1, 2, 3, 4) & foo(1, new long[] {1, 2})
				int lastIndex = paramLength - 1;
				for (int i = 0; i < lastIndex; i++) {
					TypeBinding originalRawParam = null;
					invocationStatus |= checkInvocationArgument(scope, arguments[i], params[i] , argumentTypes[i], originalRawParam);
				}
			   int argLength = arguments.length;
			   if (lastIndex < argLength) { // vararg argument was provided
				   	TypeBinding parameterType = params[lastIndex];
					TypeBinding originalRawParam = null;

				    if (paramLength != argLength || parameterType.dimensions() != argumentTypes[lastIndex].dimensions()) {
				    	parameterType = ((ArrayBinding) parameterType).elementsType(); // single element was provided for vararg parameter
				    }
					for (int i = lastIndex; i < argLength; i++) {
						invocationStatus |= checkInvocationArgument(scope, arguments[i], parameterType, argumentTypes[i], originalRawParam);
					}
				}

			   if (paramLength == argumentTypes.length) { // 70056
					int varargsIndex = paramLength - 1;
					ArrayBinding varargsType = (ArrayBinding) params[varargsIndex];
					TypeBinding lastArgType = argumentTypes[varargsIndex];
					int dimensions;
					if (lastArgType != TypeBinding.NULL && (varargsType.dimensions <= (dimensions = lastArgType.dimensions()))) {
						if (lastArgType.leafComponentType().isBaseType()) {
							dimensions--;
						}
					}
				}
			} else {
				int length = (paramLength<arguments.length) ? paramLength : arguments.length;
				for (int i = 0; i < length; i++) {
					TypeBinding originalRawParam = null;
					invocationStatus |= checkInvocationArgument(scope, arguments[i], params[i], argumentTypes[i], originalRawParam);
				}
			}
		}
//		if ((invocationStatus & INVOCATION_ARGUMENT_WILDCARD) != 0) {
//		    scope.problemReporter().wildcardInvocation((ASTNode)invocationSite, receiverType, method, argumentTypes);
//		} else if (!method.isStatic() && !receiverType.isUnboundWildcard() && method.declaringClass.isRawType() && method.hasSubstitutedParameters()) {
//		    scope.problemReporter().unsafeRawInvocation((ASTNode)invocationSite, method);
//		} else if (rawOriginalGenericMethod != null) {
//		    scope.problemReporter().unsafeRawGenericMethodInvocation((ASTNode)invocationSite, method);
//		}
	}
	public ASTNode concreteStatement() {
		return this;
	}

	public final boolean isFieldUseDeprecated(FieldBinding field, Scope scope, boolean isStrictlyAssigned) {

		if (!isStrictlyAssigned && (field.isPrivate() || (field.declaringClass != null && field.declaringClass.isLocalType())) && !scope.isDefinedInField(field)) {
			// ignore cases where field is used from within inside itself
			field.original().modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
		}

		if ((field.modifiers & ExtraCompilerModifiers.AccRestrictedAccess) != 0) {
			AccessRestriction restriction =
				scope.environment().getAccessRestriction(field.declaringClass);
			if (restriction != null) {
				scope.problemReporter().forbiddenReference(field, this,
						restriction.getFieldAccessMessageTemplate(), restriction.getProblemId());
			}
		}

		if (!field.isViewedAsDeprecated()) return false;

		// inside same unit - no report
		if (scope.isDefinedInSameUnit(field.declaringClass)) return false;

		// if context is deprecated, may avoid reporting
		if (!scope.compilerOptions().reportDeprecationInsideDeprecatedCode && scope.isInsideDeprecatedCode()) return false;
		return true;
	}

	public boolean isImplicitThis() {

		return false;
	}

	/* Answer true if the method use is considered deprecated.
	* An access in the same compilation unit is allowed.
	*/
	public final boolean isMethodUseDeprecated(MethodBinding method, Scope scope,
			boolean isExplicitUse) {
		if ((method.isPrivate() /*|| method.declaringClass.isLocalType()*/) && !scope.isDefinedInMethod(method)) {
			// ignore cases where method is used from within inside itself (e.g. direct recursions)
			method.original().modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
		}

		// TODO (maxime) consider separating concerns between deprecation and access restriction.
		// 				 Caveat: this was not the case when access restriction funtion was added.
		if (isExplicitUse && (method.modifiers & ExtraCompilerModifiers.AccRestrictedAccess) != 0) {
			// note: explicit constructors calls warnings are kept despite the 'new C1()' case (two
			//       warnings, one on type, the other on constructor), because of the 'super()' case.
			AccessRestriction restriction =
				scope.environment().getAccessRestriction(method.declaringClass);
			if (restriction != null) {
				if (method.isConstructor()) {
					scope.problemReporter().forbiddenReference(method, this,
							restriction.getConstructorAccessMessageTemplate(),
							restriction.getProblemId());
				}
				else {
					scope.problemReporter().forbiddenReference(method, this,
							restriction.getMethodAccessMessageTemplate(),
							restriction.getProblemId());
				}
			}
		}

		if (!method.isViewedAsDeprecated()) return false;

		// inside same unit - no report
		if (scope.isDefinedInSameUnit(method.declaringClass)) return false;

		// non explicit use and non explicitly deprecated - no report
		if (!isExplicitUse &&
				(method.modifiers & ClassFileConstants.AccDeprecated) == 0) {
			return false;
		}

		// if context is deprecated, may avoid reporting
		if (!scope.compilerOptions().reportDeprecationInsideDeprecatedCode && scope.isInsideDeprecatedCode()) return false;
		return true;
	}

	public boolean isSuper() {

		return false;
	}

	public boolean isThis() {

		return false;
	}

	/* Answer true if the type use is considered deprecated.
	* An access in the same compilation unit is allowed.
	*/
	public final boolean isTypeUseDeprecated(TypeBinding type, Scope scope) {

		if (type.isArrayType())
			return isTypeUseDeprecated(((ArrayBinding) type).leafComponentType, scope);
		if (type.isBaseType())
			return false;


		/* BC - threw an exception-- temp fix */
		ReferenceBinding refType=null;
		try {
			refType = (ReferenceBinding) type;
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}

		if ((refType.isPrivate() || refType.isLocalType()) && !scope.isDefinedInType(refType)) {
			// ignore cases where type is used from within inside itself
			refType.modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
		}

		if (refType.hasRestrictedAccess()) {
			AccessRestriction restriction = scope.environment().getAccessRestriction(type);
			if (restriction != null) {
				scope.problemReporter().forbiddenReference(type, this, restriction.getMessageTemplate(), restriction.getProblemId());
			}
		}

		if (!refType.isViewedAsDeprecated()) return false;

		// inside same unit - no report
		if (scope.isDefinedInSameUnit(refType)) return false;

		// if context is deprecated, may avoid reporting
		if (!scope.compilerOptions().reportDeprecationInsideDeprecatedCode && scope.isInsideDeprecatedCode()) return false;
		return true;
	}

	public abstract StringBuffer print(int indent, StringBuffer output);

	public static StringBuffer printIndent(int indent, StringBuffer output) {

		for (int i = indent; i > 0; i--) output.append("  "); //$NON-NLS-1$
		return output;
	}

	public static StringBuffer printModifiers(int modifiers, StringBuffer output) {

		if ((modifiers & ClassFileConstants.AccPublic) != 0)
			output.append("public "); //$NON-NLS-1$
		if ((modifiers & ClassFileConstants.AccPrivate) != 0)
			output.append("private "); //$NON-NLS-1$
		if ((modifiers & ClassFileConstants.AccProtected) != 0)
			output.append("protected "); //$NON-NLS-1$
		if ((modifiers & ClassFileConstants.AccStatic) != 0)
			output.append("static "); //$NON-NLS-1$
		if ((modifiers & ClassFileConstants.AccFinal) != 0)
			output.append("final "); //$NON-NLS-1$
		if ((modifiers & ClassFileConstants.AccNative) != 0)
			output.append("native "); //$NON-NLS-1$
		if ((modifiers & ClassFileConstants.AccAbstract) != 0)
			output.append("abstract "); //$NON-NLS-1$
		return output;
	}

	public int sourceStart() {
		return this.sourceStart;
	}
	public int sourceEnd() {
		return this.sourceEnd;
	}
	public void setSourceEnd(int pos) {
		this.sourceEnd = pos;
	}
	public String toString() {
		return print(0, new StringBuffer(30)).toString();
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		// do nothing by default
	}

	public boolean isInferred() {
		return false;
	}
	public int getASTType() {
		return IASTNode.AST_NODE;
	
	}
	
	public void traverse(org.eclipse.wst.jsdt.core.ast.ASTVisitor visitor) {
		this.traverse(new DelegateASTVisitor(visitor), null);
	}
}
