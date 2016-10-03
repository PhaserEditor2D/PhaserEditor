/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.Compiler;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.ObjectVector;
import org.eclipse.wst.jsdt.internal.core.Logger;

public abstract class Scope implements TypeConstants, TypeIds {

	/* Scope kinds */
	public final static int BLOCK_SCOPE = 1;
	public final static int CLASS_SCOPE = 3;
	public final static int COMPILATION_UNIT_SCOPE = 4;
	public final static int METHOD_SCOPE = 2;
	public final static int WITH_SCOPE = 5;

	/* Argument Compatibilities */
	public final static int NOT_COMPATIBLE = -1;
	public final static int COMPATIBLE = 0;
	public final static int AUTOBOX_COMPATIBLE = 1;
	public final static int VARARGS_COMPATIBLE = 2;

	/* Type Compatibilities */
	public static final int EQUAL_OR_MORE_SPECIFIC = -1;
	public static final int NOT_RELATED = 0;
	public static final int MORE_GENERIC = 1;

	public int kind;
	public Scope parent;

	/* Answer an int describing the relationship between the given types.
	*
	* 		NOT_RELATED
	* 		EQUAL_OR_MORE_SPECIFIC : left is compatible with right
	* 		MORE_GENERIC : right is compatible with left
	*/
	public static int compareTypes(TypeBinding left, TypeBinding right) {
		if (left.isCompatibleWith(right))
			return Scope.EQUAL_OR_MORE_SPECIFIC;
		if (right.isCompatibleWith(left))
			return Scope.MORE_GENERIC;
		return Scope.NOT_RELATED;
	}
	public static TypeBinding getBaseType(char[] name) {
		// list should be optimized (with most often used first)
		int length = name.length;
		if (length > 2 && length < 8) {
			switch (name[0]) {
				case 'i' :
					if (length == 3 && name[1] == 'n' && name[2] == 't')
						return TypeBinding.INT;
					break;
				case 'v' :
					if (length == 4 && name[1] == 'o' && name[2] == 'i' && name[3] == 'd')
						return TypeBinding.VOID;
					break;
				case 'b' :
					if (length == 7
						&& name[1] == 'o'
						&& name[2] == 'o'
						&& name[3] == 'l'
						&& name[4] == 'e'
						&& name[5] == 'a'
						&& name[6] == 'n')
						return TypeBinding.BOOLEAN;
					break;
				case 'c' :
					if (length == 4 && name[1] == 'h' && name[2] == 'a' && name[3] == 'r')
						return TypeBinding.CHAR;
					break;
				case 'd' :
					if (length == 6
						&& name[1] == 'o'
						&& name[2] == 'u'
						&& name[3] == 'b'
						&& name[4] == 'l'
						&& name[5] == 'e')
						return TypeBinding.DOUBLE;
					break;
				case 'f' :
					if (length == 5
						&& name[1] == 'l'
						&& name[2] == 'o'
						&& name[3] == 'a'
						&& name[4] == 't')
						return TypeBinding.FLOAT;
					break;
				case 'l' :
					if (length == 4 && name[1] == 'o' && name[2] == 'n' && name[3] == 'g')
						return TypeBinding.LONG;
					break;
				case 's' :
					if (length == 5
						&& name[1] == 'h'
						&& name[2] == 'o'
						&& name[3] == 'r'
						&& name[4] == 't')
						return TypeBinding.SHORT;
			}
		}
		return null;
	}

	protected Scope(int kind, Scope parent) {
		this.kind = kind;
		this.parent = parent;
	}

	/*
	 * Boxing primitive
	 */
	public TypeBinding boxing(TypeBinding type) {
		if (type.isBaseType())
			return environment().computeBoxingType(type);
		return type;
	}

	public ClassScope classScope() {
		Scope scope = this;
		do {
			if (scope instanceof ClassScope)
				return (ClassScope) scope;
			scope = scope.parent;
		} while (scope != null);
		return null;
	}

	public final CompilationUnitScope compilationUnitScope() {
		Scope lastScope = null;
		Scope scope = this;
		do {
			lastScope = scope;
			scope = scope.parent;
		} while (scope != null);
		return (CompilationUnitScope) lastScope;
	}

	/**
	 * Finds the most specific compiler options
	 */
	public final CompilerOptions compilerOptions() {

		return compilationUnitScope().environment.globalOptions;
	}

	/**
	 * Internal use only
	 * Given a method, returns null if arguments cannot be converted to parameters.
	 * Will answer a subsituted method in case the method was generic and type inference got triggered;
	 * in case the method was originally compatible, then simply answer it back.
	 */
	protected final MethodBinding computeCompatibleMethod(MethodBinding method, TypeBinding[] arguments, InvocationSite invocationSite) {

		TypeBinding[] parameters = method.parameters;
		if (parameters == arguments
//			&& (method.returnType.tagBits & TagBits.HasTypeVariable) == 0
				)
				return method;

		int argLength = arguments.length;
		int paramLength = parameters.length;
		boolean isVarArgs = method.isVarargs();
		if (argLength != paramLength)
			if (!isVarArgs || argLength < paramLength - 1)
				return null; // incompatible

		if (parameterCompatibilityLevel(method, arguments) > NOT_COMPATIBLE)
			return method;
		return null; // incompatible
	}

	public ArrayBinding createArrayType(TypeBinding type, int dimension) {
		if (type.isValidBinding())
			return environment().createArrayType(type, dimension);
		// do not cache obvious invalid types
		return new ArrayBinding(type, dimension, environment());
	}
	
	public final ClassScope enclosingClassScope() {
		Scope scope = this;
		while ((scope = scope.parent) != null) {
			if (scope instanceof ClassScope) return (ClassScope) scope;
		}
		return null; // may answer null if no type around
	}

	public final MethodScope enclosingMethodScope() {
		Scope scope = this;
		if (scope instanceof MethodScope) return (MethodScope) scope;
		while ((scope = scope.parent) != null) {
			if (scope instanceof MethodScope) return (MethodScope) scope;
		}
		return null; // may answer null if no method around
	}

	/* Answer the scope receiver type (could be parameterized)
	*/
	public final ReferenceBinding enclosingReceiverType() {
		Scope scope = this;
		AbstractMethodDeclaration inMethod =null;
		do {
			if (scope instanceof MethodScope) {
				MethodScope methodScope = (MethodScope) scope;
				inMethod = methodScope.referenceMethod();
				if (inMethod.inferredMethod!=null && inMethod.inferredMethod.inType!=null && inMethod.inferredMethod.inType.binding!=null)
					return inMethod.inferredMethod.inType.binding;
			}
			else if (scope instanceof CompilationUnitScope) {
				CompilationUnitScope compilationUnitScope = (CompilationUnitScope) scope;
				for (int i=0;i<compilationUnitScope.referenceContext.numberInferredTypes;i++)
				{
					InferredType type= compilationUnitScope.referenceContext.inferredTypes[i];
					if (type.containsMethod(inMethod))
						return (ReferenceBinding)compilationUnitScope.getTypeOrPackage(type.getName(),Binding.TYPE);
				}
			}
			if (scope instanceof ClassScope) {
				ClassScope classScope=(ClassScope)scope;
				if (classScope.referenceContext!=null)
					return classScope.referenceContext.binding;
				if (classScope.inferredType!=null)
					return classScope.inferredType.binding;
//				return environment().convertToParameterizedType(((ClassScope) scope).referenceContext.binding);
			}
			scope = scope.parent;
		} while (scope != null);
		return null;
	}
	public final CompilationUnitBinding enclosingCompilationUnit() {
		Scope scope = this;
		do {
			if (scope instanceof CompilationUnitScope) {
				return ((CompilationUnitScope) scope).referenceContext.compilationUnitBinding;
			}
			scope = scope.parent;
		} while (scope != null);
		return null;
	}

	/**
	 * Returns the immediately enclosing reference context, starting from current scope parent.
	 * If starting on a class, it will skip current class. If starting on unitScope, returns null.
	 */
	public ReferenceContext enclosingReferenceContext() {
		Scope current = this;
		while ((current = current.parent) != null) {
			switch(current.kind) {
				case METHOD_SCOPE :
					return ((MethodScope) current).referenceContext;
				case CLASS_SCOPE :
					return ((ClassScope) current).referenceContext;
				case COMPILATION_UNIT_SCOPE :
					return ((CompilationUnitScope) current).referenceContext;
			}
		}
		return null;
	}

	/* Answer the scope enclosing source type (could be generic)
	*/
	public final SourceTypeBinding enclosingSourceType() {
		Scope scope = this;
		do {
			if (scope instanceof ClassScope)
				return ((ClassScope) scope).getReferenceBinding();
			else if(scope instanceof CompilationUnitScope)
				return ((CompilationUnitScope) scope).referenceContext.compilationUnitBinding;
			scope = scope.parent;
		} while (scope != null);
		return null;
	}

	public final SourceTypeBinding enclosingTypeBinding() {
		Scope scope = this;
		do {
			if (scope instanceof ClassScope)
				return ((ClassScope) scope).getReferenceBinding();
			else if (scope instanceof CompilationUnitScope)
				return ((CompilationUnitScope) scope).referenceContext.compilationUnitBinding;
			scope = scope.parent;
		} while (scope != null);
		return null;
	}

	public final LookupEnvironment environment() {
		Scope scope, unitScope = this;
		while ((scope = unitScope.parent) != null)
			unitScope = scope;
		return ((CompilationUnitScope) unitScope).environment;
	}

	// abstract method lookup lookup (since maybe missing default abstract methods)
	protected MethodBinding findDefaultAbstractMethod(
		ReferenceBinding receiverType,
		char[] selector,
		TypeBinding[] argumentTypes,
		InvocationSite invocationSite,
		ReferenceBinding classHierarchyStart,
		ObjectVector found,
		MethodBinding concreteMatch) {

		int startFoundSize = found.size;

		MethodBinding[] candidates = null;
		int candidatesCount = 0;
		MethodBinding problemMethod = null;
		int foundSize = found.size;
		if (foundSize > startFoundSize) {
			// argument type compatibility check
			for (int i = startFoundSize; i < foundSize; i++) {
				MethodBinding methodBinding = (MethodBinding) found.elementAt(i);
				MethodBinding compatibleMethod = computeCompatibleMethod(methodBinding, argumentTypes, invocationSite);
				if (compatibleMethod != null) {
					if (compatibleMethod.isValidBinding()) {
						if (candidatesCount == 0) {
							candidates = new MethodBinding[foundSize - startFoundSize + 1];
							if (concreteMatch != null)
								candidates[candidatesCount++] = concreteMatch;
						}
						candidates[candidatesCount++] = compatibleMethod;
					} else if (problemMethod == null) {
						problemMethod = compatibleMethod;
					}
				}
			}
		}

		if (candidatesCount < 2) {
			if (concreteMatch == null) {
				if (candidatesCount == 0)
					return problemMethod; // can be null
				concreteMatch = candidates != null ? candidates[0] : null;
			}
			return concreteMatch;
		}
		// no need to check for visibility - interface methods are public
		if (compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
			return mostSpecificMethodBinding(candidates, candidatesCount, argumentTypes, invocationSite, receiverType);
		return null;
	}

	// Internal use only
	public ReferenceBinding findDirectMemberType(char[] typeName, ReferenceBinding enclosingType) {
		if ((enclosingType.tagBits & TagBits.HasNoMemberTypes) != 0)
			return null; // know it has no member types (nor inherited member types)

		ReferenceBinding enclosingReceiverType = enclosingReceiverType();
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordReference(enclosingType, typeName);
		ReferenceBinding memberType = enclosingType.getMemberType(typeName);
		if (memberType != null) {
			unitScope.recordTypeReference(memberType);
			if (enclosingReceiverType == null
				? memberType.canBeSeenBy(getCurrentPackage())
				: memberType.canBeSeenBy(enclosingType, enclosingReceiverType))
					return memberType;
			return new ProblemReferenceBinding(typeName, memberType, ProblemReasons.NotVisible);
		}
		return null;
	}

	// Internal use only
	public MethodBinding findExactMethod(
		ReferenceBinding receiverType,
		char[] selector,
		TypeBinding[] argumentTypes,
		InvocationSite invocationSite) {

		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordTypeReferences(argumentTypes);
		MethodBinding exactMethod = (receiverType!=null) ?
			receiverType.getExactMethod(selector, argumentTypes, unitScope) :
				unitScope.referenceContext.compilationUnitBinding.getExactMethod(selector, argumentTypes, unitScope);
		if (exactMethod != null && !exactMethod.isBridge()) {
			// must find both methods for this case: <S extends A> void foo() {}  and  <N extends B> N foo() { return null; }
			// or find an inherited method when the exact match is to a bridge method
			// special treatment for Object.getClass() in 1.5 mode (substitute parameterized return type)
			if (exactMethod.canBeSeenBy(receiverType, invocationSite, this)) {
				return exactMethod;
			}
		}
		return null;
	}

	// Internal use only
	/*	Answer the field binding that corresponds to fieldName.
		Start the lookup at the receiverType.
		InvocationSite implements
			isSuperAccess(); this is used to determine if the discovered field is visible.
		Only fields defined by the receiverType or its supertypes are answered;
		a field of an enclosing type will not be found using this API.

		If no visible field is discovered, null is answered.
	*/
	public FieldBinding findField(TypeBinding receiverType, char[] fieldName, InvocationSite invocationSite, boolean needResolve) {

		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordTypeReference(receiverType);

		checkArrayField: {
			switch (receiverType.kind()) {
				case Binding.BASE_TYPE :
					return null;
				default:
					break checkArrayField;
			}
		}

		ReferenceBinding currentType = (ReferenceBinding) receiverType;
		if (!currentType.canBeSeenBy(this))
			return new ProblemFieldBinding(currentType, fieldName, ProblemReasons.ReceiverTypeNotVisible);

		FieldBinding field = currentType.getField(fieldName, needResolve);
		if (field != null) {
			if (invocationSite == null
				? field.canBeSeenBy(getCurrentPackage())
				: field.canBeSeenBy(currentType, invocationSite, this))
					return field;
			return new ProblemFieldBinding(field /* closest match*/, field.declaringClass, fieldName, ProblemReasons.NotVisible);
		}
		// collect all superinterfaces of receiverType until the field is found in a supertype
		FieldBinding visibleField = null;
		boolean keepLooking = true;
		FieldBinding notVisibleField = null;
		// we could hold onto the not visible field for extra error reporting
		
		Set checkedParents = new HashSet();
		while (keepLooking) {
			if (JavaScriptCore.IS_ECMASCRIPT4)
			{
				((SourceTypeBinding) currentType).classScope.connectTypeHierarchy();
			}
			if ((currentType = currentType.getSuperBinding()) == null) {
				break;
			}
			
			/* if current type is already a parent that was check break to prevent
			 * infinite loop.  This can happen if something gets messed up with
			 * the parentage of a type and there ends up being a parentage loop.
			 * 
			 * else add the current type to the checked parents and continue on
			 */
			if(checkedParents.contains(currentType)) {
				break;
			} else {
				checkedParents.add(currentType);
			}

			unitScope.recordTypeReference(currentType);
			if ((field = currentType.getField(fieldName, needResolve)) != null) {
				keepLooking = false;
				if (field.canBeSeenBy(receiverType, invocationSite, this)) {
					if (visibleField == null)
						visibleField = field;
					else
						return new ProblemFieldBinding(visibleField /* closest match*/, visibleField.declaringClass, fieldName, ProblemReasons.Ambiguous);
				} else {
					if (notVisibleField == null)
						notVisibleField = field;
				}
			}
		}

		if (visibleField != null)
			return visibleField;
		if (notVisibleField != null) {
			return new ProblemFieldBinding(notVisibleField, currentType, fieldName, ProblemReasons.NotVisible);
		}
		return null;
	}

	// Internal use only
	public ReferenceBinding findMemberType(char[] typeName, ReferenceBinding enclosingType) {
		if ((enclosingType.tagBits & TagBits.HasNoMemberTypes) != 0)
			return null; // know it has no member types (nor inherited member types)

		ReferenceBinding enclosingSourceType = enclosingSourceType();
		PackageBinding currentPackage = getCurrentPackage();
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordReference(enclosingType, typeName);
		ReferenceBinding memberType = enclosingType.getMemberType(typeName);
		if (memberType != null) {
			unitScope.recordTypeReference(memberType);
			if (enclosingSourceType == null
				? memberType.canBeSeenBy(currentPackage)
				: memberType.canBeSeenBy(enclosingType, enclosingSourceType))
					return memberType;
			return new ProblemReferenceBinding(typeName, memberType, ProblemReasons.NotVisible);
		}

		// collect all superinterfaces of receiverType until the memberType is found in a supertype
		ReferenceBinding currentType = enclosingType;
		ReferenceBinding[] interfacesToVisit = null;
		int nextPosition = 0;
		ReferenceBinding visibleMemberType = null;
		boolean keepLooking = true;
		ReferenceBinding notVisible = null;
		// we could hold onto the not visible field for extra error reporting
		Set checkedParents = new HashSet();
		while (keepLooking) {
			
			ReferenceBinding sourceType = currentType;
			if (sourceType.isHierarchyBeingConnected())
				return null; // looking for an undefined member type in its own superclass ref
			((SourceTypeBinding) sourceType).classScope.connectTypeHierarchy();
			
			if ((currentType = currentType.getSuperBinding()) == null) {
				break;
			}
			
			/* if current type is already a parent that was check break to prevent
			 * infinite loop.  This can happen if something gets messed up with
			 * the parentage of a type and there ends up being a parentage loop.
			 * 
			 * else add the current type to the checked parents and continue on
			 */
			if(checkedParents.contains(currentType)) {
				break;
			} else {
				checkedParents.add(currentType);
			}

			unitScope.recordReference(currentType, typeName);
			if ((memberType = currentType.getMemberType(typeName)) != null) {
				unitScope.recordTypeReference(memberType);
				keepLooking = false;
				if (enclosingSourceType == null
					? memberType.canBeSeenBy(currentPackage)
					: memberType.canBeSeenBy(enclosingType, enclosingSourceType)) {
						if (visibleMemberType == null)
							visibleMemberType = memberType;
						else
							return new ProblemReferenceBinding(typeName, visibleMemberType, ProblemReasons.Ambiguous);
				} else {
					notVisible = memberType;
				}
			}
		}
		// walk all visible interfaces to find ambiguous references
		if (interfacesToVisit != null) {
			ProblemReferenceBinding ambiguous = null;
			done : for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding anInterface = interfacesToVisit[i];
				unitScope.recordReference(anInterface, typeName);
				if ((memberType = anInterface.getMemberType(typeName)) != null) {
					unitScope.recordTypeReference(memberType);
					if (visibleMemberType == null) {
						visibleMemberType = memberType;
					} else {
						ambiguous = new ProblemReferenceBinding(typeName, visibleMemberType, ProblemReasons.Ambiguous);
						break done;
					}
				}
			}
			if (ambiguous != null)
				return ambiguous;
		}
		if (visibleMemberType != null)
			return visibleMemberType;
		if (notVisible != null)
			return new ProblemReferenceBinding(typeName, notVisible, ProblemReasons.NotVisible);
		return null;
	}

	/**
	 * <b>NOTE: </b> Internal use only - use findMethod()
	 * 
	 * @param receiverType
	 * @param selector
	 * @param argumentTypes <code>null</code> means match on any arguments
	 * @param invocationSite
	 * @return
	 */
	public MethodBinding findMethod(ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
		ReferenceBinding currentType = receiverType;
		ObjectVector found = new ObjectVector(3);
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordTypeReferences(argumentTypes);

		if (receiverType==null)
		{
			MethodBinding methodBinding = unitScope.referenceContext.compilationUnitBinding.getExactMethod(selector,argumentTypes, unitScope);
			if (methodBinding==null)
				methodBinding= new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
			return methodBinding;
		}

		// superclass lookup
		long complianceLevel = compilerOptions().complianceLevel;
		boolean isCompliant14 = complianceLevel >= ClassFileConstants.JDK1_4;
		boolean isCompliant15 = complianceLevel >= ClassFileConstants.JDK1_5;
		ReferenceBinding classHierarchyStart = currentType;
		Set checkedParents = new HashSet();
		while (currentType != null) {
			/* if current type is already a parent that was check break to prevent
			 * infinite loop.  This can happen if something gets messed up with
			 * the parentage of a type and there ends up being a parentage loop.
			 * 
			 * else add the current type to the checked parents and continue on
			 */
			if(checkedParents.contains(currentType)) {
				break;
			} else {
				checkedParents.add(currentType);
			}
			
			unitScope.recordTypeReference(currentType);
			MethodBinding[] currentMethods = currentType.getMethods(selector);
			int currentLength = currentMethods.length;
			if (currentLength > 0) {
				if (isCompliant14 && (found.size > 0)) {
					nextMethod: for (int i = 0, l = currentLength; i < l; i++) { // currentLength can be modified inside the loop
						MethodBinding currentMethod = currentMethods[i];
						if (currentMethod == null) continue nextMethod;

						// if 1.4 compliant, must filter out redundant protected methods from superclasses
						// protected method need to be checked only - default access is already dealt with in #canBeSeen implementation
						// when checking that p.C -> q.B -> p.A cannot see default access members from A through B.
						// if ((currentMethod.modifiers & AccProtected) == 0) continue nextMethod;
						// BUT we can also ignore any overridden method since we already know the better match (fixes 80028)
						for (int j = 0, max = found.size; j < max; j++) {
							MethodBinding matchingMethod = (MethodBinding) found.elementAt(j);
							if (currentMethod.areParametersEqual(matchingMethod)) {
								if (isCompliant15) {
									if (matchingMethod.isBridge() && !currentMethod.isBridge())
										continue nextMethod; // keep inherited methods to find concrete method over a bridge method
								}
								currentLength--;
								currentMethods[i] = null;
								continue nextMethod;
							}
						}
					}
				}

				if (currentLength > 0) {
					// append currentMethods, filtering out null entries
					if (currentMethods.length == currentLength) {
						found.addAll(currentMethods);
					} else {
						for (int i = 0, max = currentMethods.length; i < max; i++) {
							MethodBinding currentMethod = currentMethods[i];
							if (currentMethod != null)
								found.add(currentMethod);
						}
					}
				}
			}
			currentType = currentType.getSuperBinding();
		}

		if (found.size==0 && (receiverType==null || receiverType instanceof CompilationUnitBinding))
		{
			Binding binding = getTypeOrPackage(selector, Binding.METHOD);
			if (binding instanceof MethodBinding)
			{
				((MethodBinding) binding).ensureBindingsAreComplete();
				found.add(binding);
		}
		}
		// if found several candidates, then eliminate those not matching argument types
		int foundSize = found.size;
		MethodBinding[] candidates = null;
		int candidatesCount = 0;
		MethodBinding problemMethod = null;
		if (foundSize > 0) {
			// argument type compatibility check
			for (int i = 0; i < foundSize; i++) {
				MethodBinding methodBinding = (MethodBinding) found.elementAt(i);
				MethodBinding compatibleMethod = methodBinding;//computeCompatibleMethod(methodBinding, argumentTypes, invocationSite);
				if (compatibleMethod != null) {
					if (compatibleMethod.isValidBinding()) {
						if (foundSize == 1 && compatibleMethod.canBeSeenBy(receiverType, invocationSite, this)) {
							// return the single visible match now
							return compatibleMethod;
						}
						if (candidatesCount == 0)
							candidates = new MethodBinding[foundSize];
						candidates[candidatesCount++] = compatibleMethod;
					} else if (problemMethod == null) {
						problemMethod = compatibleMethod;
					}
				}
			}
		}

		// no match was found
		if (candidatesCount == 0) {
			// abstract classes may get a match in interfaces; for non abstract
			// classes, reduces secondary errors since missing interface method
			// error is already reported
			MethodBinding interfaceMethod =
				findDefaultAbstractMethod(receiverType, selector, argumentTypes, invocationSite, classHierarchyStart, found, null);
			if (interfaceMethod != null) return interfaceMethod;
			if (found.size == 0) return null;
			if (problemMethod != null) return problemMethod;

			// still no match; try to find a close match when the parameter
			// order is wrong or missing some parameters


			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=69471
			// bad guesses are foo(), when argument types have been supplied
			// and foo(X, Y), when the argument types are (int, float, Y)
			// so answer the method with the most argType matches and least parameter type mismatches
			int bestArgMatches = -1;
			MethodBinding bestGuess = (MethodBinding) found.elementAt(0); // if no good match so just use the first one found
			int argLength = argumentTypes.length;
			foundSize = found.size;
			nextMethod : for (int i = 0; i < foundSize; i++) {
				MethodBinding methodBinding = (MethodBinding) found.elementAt(i);
				TypeBinding[] params = methodBinding.parameters;
				int paramLength = params.length;
				int argMatches = 0;
				next: for (int a = 0; a < argLength; a++) {
					TypeBinding arg = argumentTypes[a];
					for (int p = a == 0 ? 0 : a - 1; p < paramLength && p < a + 1; p++) { // look one slot before & after to see if the type matches
						if (params[p] == arg) {
							argMatches++;
							continue next;
						}
					}
				}
				if (argMatches < bestArgMatches)
					continue nextMethod;
				if (argMatches == bestArgMatches) {
					int diff1 = paramLength < argLength ? 2 * (argLength - paramLength) : paramLength - argLength;
					int bestLength = bestGuess.parameters.length;
					int diff2 = bestLength < argLength ? 2 * (argLength - bestLength) : bestLength - argLength;
					if (diff1 >= diff2)
						continue nextMethod;
				}
				bestArgMatches = argMatches;
				bestGuess = methodBinding;
			}
			return bestGuess;
//			return new ProblemMethodBinding(bestGuess, bestGuess.selector, argumentTypes, ProblemReasons.NotFound);
		}

		// tiebreak using visibility check
		int visiblesCount = 0;
		
		for (int i = 0; i < candidatesCount; i++) {
			MethodBinding methodBinding = candidates[i];
			if (methodBinding.canBeSeenBy(receiverType, invocationSite,
					this)) {
				if (visiblesCount != i) {
					candidates[i] = null;
					candidates[visiblesCount] = methodBinding;
				}
				visiblesCount++;
			}

		}
		if (visiblesCount == 1) {
			return candidates[0];
		}
		if (visiblesCount == 0) {
			MethodBinding interfaceMethod = findDefaultAbstractMethod(
					receiverType, selector, argumentTypes, invocationSite,
					classHierarchyStart, found, null);
			if (interfaceMethod != null)
				return interfaceMethod;
			return new ProblemMethodBinding(candidates[0],
					candidates[0].selector, candidates[0].parameters,
					ProblemReasons.NotVisible);
		}
		
		if (complianceLevel <= ClassFileConstants.JDK1_3) {
			ReferenceBinding declaringClass = candidates[0].declaringClass;
			return mostSpecificClassMethodBinding(candidates, visiblesCount, invocationSite);
		}

		MethodBinding mostSpecificMethod = mostSpecificMethodBinding(candidates, visiblesCount, argumentTypes, invocationSite, receiverType);
		return mostSpecificMethod;
	}

	// Internal use only
	public MethodBinding findMethodForArray(
		ArrayBinding receiverType,
		char[] selector,
		TypeBinding[] argumentTypes,
		InvocationSite invocationSite) {

		TypeBinding leafType = receiverType.leafComponentType();
		if (leafType instanceof ReferenceBinding) {
			if (!((ReferenceBinding) leafType).canBeSeenBy(this))
				return new ProblemMethodBinding(selector, Binding.NO_PARAMETERS, (ReferenceBinding)leafType, ProblemReasons.ReceiverTypeNotVisible);
		}

		ReferenceBinding object = getJavaLangObject();
		MethodBinding methodBinding = object.getExactMethod(selector, argumentTypes, null);
		if (methodBinding != null) {
			// handle the method clone() specially... cannot be protected or throw exceptions
			if (argumentTypes == Binding.NO_PARAMETERS) {
			    switch (selector[0]) {
			        case 'c':
			            break;
			        case 'g':
			            break;
			    }
			}
			if (methodBinding.canBeSeenBy(receiverType, invocationSite, this))
				return methodBinding;
		}
		methodBinding = findMethod(object, selector, argumentTypes, invocationSite);
		if (methodBinding == null)
			return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
		return methodBinding;
	}

	// Internal use only
	public  Binding findBinding(
		char[] typeName,
		int mask,
		PackageBinding declarationPackage,
		PackageBinding invocationPackage, boolean searchEnvironment) {

		compilationUnitScope().recordReference(declarationPackage.compoundName, typeName);
		Binding typeBinding = 
			(searchEnvironment) ? declarationPackage.getBinding(typeName,mask) :
			declarationPackage.getBinding0(typeName, mask);
		if (typeBinding == null)
			return null;

		if (typeBinding.isValidBinding()) {
			if (declarationPackage != invocationPackage && typeBinding instanceof ReferenceBinding
					&& !((ReferenceBinding)typeBinding).canBeSeenBy(invocationPackage))
				return new ProblemReferenceBinding(typeName,(ReferenceBinding) typeBinding, ProblemReasons.NotVisible);
		}
		return typeBinding;
	}

	// Internal use only
	public ReferenceBinding findType(
		char[] typeName,
		PackageBinding declarationPackage,
		PackageBinding invocationPackage) {

		return (ReferenceBinding)findBinding(typeName, Binding.TYPE, declarationPackage, invocationPackage, true);
	}

	public LocalVariableBinding findVariable(char[] variable) {

		return null;
	}

	/* API
	 *
	 *	Answer the binding that corresponds to the argument name.
	 *	flag is a mask of the following values VARIABLE (= FIELD or LOCAL), TYPE, PACKAGE.
	 *	Only bindings corresponding to the mask can be answered.
	 *
	 *	For example, getBinding("foo", VARIABLE, site) will answer
	 *	the binding for the field or local named "foo" (or an error binding if none exists).
	 *	If a type named "foo" exists, it will not be detected (and an error binding will be answered)
	 *
	 *	The VARIABLE mask has precedence over the TYPE mask.
	 *
	 *	If the VARIABLE mask is not set, neither fields nor locals will be looked for.
	 *
	 *	InvocationSite implements:
	 *		isSuperAccess(); this is used to determine if the discovered field is visible.
	 *
	 *	Limitations: cannot request FIELD independently of LOCAL, or vice versa
	 */
	public Binding getBinding(char[] name, int mask, InvocationSite invocationSite, boolean needResolve) {
		CompilationUnitScope unitScope = compilationUnitScope();
		LookupEnvironment env = unitScope.environment;
		try {
			env.missingClassFileLocation = invocationSite;
			Binding binding = null;
			FieldBinding problemField = null;
			if ((mask & Binding.VARIABLE) != 0) {
				boolean insideStaticContext = false;
				boolean insideConstructorCall = false;

				FieldBinding foundField = null;
				// can be a problem field which is answered if a valid field is not found
				ProblemFieldBinding foundInsideProblem = null;
				// inside Constructor call or inside static context
				Scope scope = this;
				int depth = 0;
				int foundDepth = 0;
				ReferenceBinding foundActualReceiverType = null;
				done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found
					switch (scope.kind) {
						case METHOD_SCOPE :
							MethodScope methodScope = (MethodScope) scope;
							insideStaticContext |= methodScope.isStatic;
							insideConstructorCall |= methodScope.isConstructorCall;

							// Fall through... could duplicate the code below to save a cast - questionable optimization
						case BLOCK_SCOPE :
							LocalVariableBinding variableBinding = scope.findVariable(name);
							// looks in this scope only
							if (variableBinding != null) {
								if (foundField != null && foundField.isValidBinding())
									return new ProblemFieldBinding(
										foundField, // closest match
										foundField.declaringClass,
										name,
										ProblemReasons.InheritedNameHidesEnclosingName);
								if (depth > 0)
									invocationSite.setDepth(depth);
								return variableBinding;
							}
							break;
						case CLASS_SCOPE :
							ClassScope classScope = (ClassScope) scope;
							ReferenceBinding receiverType = classScope.enclosingReceiverType();
							FieldBinding fieldBinding = classScope.findField(receiverType, name, invocationSite, needResolve);
							// Use next line instead if willing to enable protected access accross inner types
							// FieldBinding fieldBinding = findField(enclosingType, name, invocationSite);

							if (fieldBinding != null) { // skip it if we did not find anything
								if (fieldBinding.problemId() == ProblemReasons.Ambiguous) {
									if (foundField == null || foundField.problemId() == ProblemReasons.NotVisible)
										// supercedes any potential InheritedNameHidesEnclosingName problem
										return fieldBinding;
									// make the user qualify the field, likely wants the first inherited field (javac generates an ambiguous error instead)
									return new ProblemFieldBinding(
										foundField, // closest match
										foundField.declaringClass,
										name,
										ProblemReasons.InheritedNameHidesEnclosingName);
								}

								ProblemFieldBinding insideProblem = null;
								if (fieldBinding.isValidBinding()) {
									if (!fieldBinding.isStatic()) {
										if (insideConstructorCall) {
											insideProblem =
												new ProblemFieldBinding(
													fieldBinding, // closest match
													fieldBinding.declaringClass,
													name,
													ProblemReasons.NonStaticReferenceInConstructorInvocation);
										} else if (insideStaticContext) {
											insideProblem =
												new ProblemFieldBinding(
													fieldBinding, // closest match
													fieldBinding.declaringClass,
													name,
													ProblemReasons.NonStaticReferenceInStaticContext);
										}
									}
									if (receiverType == fieldBinding.declaringClass || compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4) {
										// found a valid field in the 'immediate' scope (ie. not inherited)
										// OR in 1.4 mode (inherited shadows enclosing)
										if (foundField == null) {
											if (depth > 0){
												invocationSite.setDepth(depth);
												invocationSite.setActualReceiverType(receiverType);
											}
											// return the fieldBinding if it is not declared in a superclass of the scope's binding (that is, inherited)
											return insideProblem == null ? fieldBinding : insideProblem;
										}
										if (foundField.isValidBinding())
											// if a valid field was found, complain when another is found in an 'immediate' enclosing type (that is, not inherited)
											if (foundField.declaringClass != fieldBinding.declaringClass)
												// ie. have we found the same field - do not trust field identity yet
												return new ProblemFieldBinding(
													foundField, // closest match
													foundField.declaringClass,
													name,
													ProblemReasons.InheritedNameHidesEnclosingName);
									}
								}

								if (foundField == null || (foundField.problemId() == ProblemReasons.NotVisible && fieldBinding.problemId() != ProblemReasons.NotVisible)) {
									// only remember the fieldBinding if its the first one found or the previous one was not visible & fieldBinding is...
									foundDepth = depth;
									foundActualReceiverType = receiverType;
									foundInsideProblem = insideProblem;
									foundField = fieldBinding;
								}
							}
							
							depth++;
							insideStaticContext |= receiverType.isStatic();
							// 1EX5I8Z - accessing outer fields within a constructor call is permitted
							// in order to do so, we change the flag as we exit from the type, not the method
							// itself, because the class scope is used to retrieve the fields.
							MethodScope enclosingMethodScope = scope.methodScope();
							insideConstructorCall = enclosingMethodScope == null ? false : enclosingMethodScope.isConstructorCall;
							break;
						case WITH_SCOPE :
							WithScope withScope = (WithScope) scope;
							TypeBinding withType = withScope.referenceContext;
							fieldBinding = withScope.findField(withType, name, invocationSite, needResolve);
							// Use next line instead if willing to enable protected access accross inner types
							// FieldBinding fieldBinding = findField(enclosingType, name, invocationSite);

							if (fieldBinding != null) { // skip it if we did not find anything
								if (fieldBinding.isValidBinding()) {
									return fieldBinding;
								}
							}
							break;
							case COMPILATION_UNIT_SCOPE :
							if ( (mask & (Binding.FIELD|Binding.VARIABLE)) >0)
							{
								variableBinding = scope.findVariable(name);
							// looks in this scope only
								if (variableBinding != null) {
									if (foundField != null && foundField.isValidBinding())
										return new ProblemFieldBinding(
										foundField, // closest match
										foundField.declaringClass,
										name,
										ProblemReasons.InheritedNameHidesEnclosingName);
									if (depth > 0)
										invocationSite.setDepth(depth);
									return variableBinding;
								}

								if(unitScope.classScope()!=null) {
									//ReferenceBinding bind = env.getType(new char[][]{unitScope.superTypeName});
									//if(bind==null) break done;
									foundField = (unitScope.classScope()).findField(unitScope.superBinding, name, invocationSite, true);
									if(foundField!=null && foundField.isValidBinding()) {

										return foundField;
									}
								}



							}else if  ( (mask & (Binding.METHOD)) >0){
								MethodBinding methodBinding = (unitScope.classScope()).findMethod(unitScope.superBinding, name, new TypeBinding[0], invocationSite);
								if(methodBinding!=null && methodBinding.isValidBinding()) return methodBinding;

							}

							break done;
					}
					scope = scope.parent;
				}

				if (foundInsideProblem != null)
					return foundInsideProblem;
				if (foundField != null) {
					if (foundField.isValidBinding()) {
						if (foundDepth > 0) {
							invocationSite.setDepth(foundDepth);
							invocationSite.setActualReceiverType(foundActualReceiverType);
						}
						return foundField;
					}
					problemField = foundField;
					foundField = null;
				}
			}

			if ( (mask&Binding.METHOD)!=0)
			{
				
				Scope scope = this;

				done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found
					switch (scope.kind) {
						case METHOD_SCOPE :
							MethodScope methodScope = (MethodScope) scope;
							binding = methodScope.findMethod(name, Binding.NO_PARAMETERS,true);
							if (binding!=null)
								return binding;
							break;
						case WITH_SCOPE :
							WithScope withScope = (WithScope) scope;
							ReferenceBinding withType = withScope.referenceContext;
							// retrieve an exact visible match (if possible)
							// compilationUnitScope().recordTypeReference(receiverType);   not needed since receiver is the source type
							MethodBinding methBinding = withScope.findExactMethod(withType, name, Binding.NO_PARAMETERS, invocationSite);
							if (methBinding == null)
								methBinding = withScope.findMethod(withType,name, Binding.NO_PARAMETERS, invocationSite);
							if (methBinding != null) { // skip it if we did not find anything
									if (methBinding.isValidBinding()) {
											return methBinding;
										}
							}
							break;
						case CLASS_SCOPE :
							ClassScope classScope = (ClassScope) scope;
							ReferenceBinding receiverType = classScope.enclosingReceiverType();
							break;
						case COMPILATION_UNIT_SCOPE :
							CompilationUnitScope compilationUnitScope = (CompilationUnitScope) scope;
							CompilationUnitBinding compilationUnitBinding = compilationUnitScope.enclosingCompilationUnit();
							 receiverType = compilationUnitBinding;
								MethodBinding methodBinding =
										  compilationUnitScope.findExactMethod(receiverType, name, Binding.NO_PARAMETERS, invocationSite);
								if (methodBinding != null) { // skip it if we did not find anything
											return methodBinding;
								}

							break done;
					}
					scope = scope.parent;
				}

			}
			// We did not find a local or instance variable.
			if ((mask & Binding.TYPE|Binding.VARIABLE|Binding.METHOD) != 0) {
				if ((mask & Binding.TYPE) != 0 && (binding = getBaseType(name)) != null)
					return binding;
				binding = getTypeOrPackage(name,  mask);// (mask & Binding.PACKAGE) == 0 ? Binding.TYPE : Binding.TYPE | Binding.PACKAGE);
				if (binding.isValidBinding() || mask == Binding.TYPE)
					return binding;
				// answer the problem type binding if we are only looking for a type
			} else if ((mask & Binding.PACKAGE) != 0) {
				unitScope.recordSimpleReference(name);
				if ((binding = env.getTopLevelPackage(name)) != null)
					return binding;
			}
			if (problemField != null) return problemField;
			if (binding != null && binding.problemId() != ProblemReasons.NotFound)
				return binding; // answer the better problem binding
			return new ProblemBinding(name, enclosingTypeBinding(), ProblemReasons.NotFound);
		} catch (AbortCompilation e) {
			e.updateContext(invocationSite, referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	/* API
	 *
	 *	Answer the binding that corresponds to the argument name.
	 *	flag is a mask of the following values VARIABLE (= FIELD or LOCAL), TYPE, PACKAGE.
	 *	Only bindings corresponding to the mask can be answered.
	 *
	 *	For example, getBinding("foo", VARIABLE, site) will answer
	 *	the binding for the field or local named "foo" (or an error binding if none exists).
	 *	If a type named "foo" exists, it will not be detected (and an error binding will be answered)
	 *
	 *	The VARIABLE mask has precedence over the TYPE mask.
	 *
	 *	If the VARIABLE mask is not set, neither fields nor locals will be looked for.
	 *
	 *	InvocationSite implements:
	 *		isSuperAccess(); this is used to determine if the discovered field is visible.
	 *
	 *	Limitations: cannot request FIELD independently of LOCAL, or vice versa
	 */
	public Binding getLocalBinding(char[] name, int mask, InvocationSite invocationSite, boolean needResolve) {
		CompilationUnitScope unitScope = compilationUnitScope();
		try {
			Binding binding = null;
			FieldBinding problemField = null;
			if ((mask & Binding.VARIABLE) != 0) {
				boolean insideStaticContext = false;
				boolean insideConstructorCall = false;

				FieldBinding foundField = null;
				// can be a problem field which is answered if a valid field is not found
				ProblemFieldBinding foundInsideProblem = null;
				// inside Constructor call or inside static context
				Scope scope = this;
				int depth = 0;
				int foundDepth = 0;
				ReferenceBinding foundActualReceiverType = null;
				done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found
					switch (scope.kind) {
						case METHOD_SCOPE :
							MethodScope methodScope = (MethodScope) scope;
							insideStaticContext |= methodScope.isStatic;
							insideConstructorCall |= methodScope.isConstructorCall;

							// Fall through... could duplicate the code below to save a cast - questionable optimization
						case BLOCK_SCOPE :
							LocalVariableBinding variableBinding = scope.findVariable(name);
							// looks in this scope only
							if (variableBinding != null) {
								if (foundField != null && foundField.isValidBinding())
									return new ProblemFieldBinding(
										foundField, // closest match
										foundField.declaringClass,
										name,
										ProblemReasons.InheritedNameHidesEnclosingName);
								if (depth > 0)
									invocationSite.setDepth(depth);
								return variableBinding;
							}
							break;
						case CLASS_SCOPE :
							ClassScope classScope = (ClassScope) scope;
							ReferenceBinding receiverType = classScope.enclosingReceiverType();
							FieldBinding fieldBinding = classScope.findField(receiverType, name, invocationSite, needResolve);
							// Use next line instead if willing to enable protected access accross inner types
							// FieldBinding fieldBinding = findField(enclosingType, name, invocationSite);

							if (fieldBinding != null) { // skip it if we did not find anything
								if (fieldBinding.problemId() == ProblemReasons.Ambiguous) {
									if (foundField == null || foundField.problemId() == ProblemReasons.NotVisible)
										// supercedes any potential InheritedNameHidesEnclosingName problem
										return fieldBinding;
									// make the user qualify the field, likely wants the first inherited field (javac generates an ambiguous error instead)
									return new ProblemFieldBinding(
										foundField, // closest match
										foundField.declaringClass,
										name,
										ProblemReasons.InheritedNameHidesEnclosingName);
								}

								ProblemFieldBinding insideProblem = null;
								if (fieldBinding.isValidBinding()) {
									if (!fieldBinding.isStatic()) {
										if (insideConstructorCall) {
											insideProblem =
												new ProblemFieldBinding(
													fieldBinding, // closest match
													fieldBinding.declaringClass,
													name,
													ProblemReasons.NonStaticReferenceInConstructorInvocation);
										} else if (insideStaticContext) {
											insideProblem =
												new ProblemFieldBinding(
													fieldBinding, // closest match
													fieldBinding.declaringClass,
													name,
													ProblemReasons.NonStaticReferenceInStaticContext);
										}
									}
									if (receiverType == fieldBinding.declaringClass || compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4) {
										// found a valid field in the 'immediate' scope (ie. not inherited)
										// OR in 1.4 mode (inherited shadows enclosing)
										if (foundField == null) {
											if (depth > 0){
												invocationSite.setDepth(depth);
												invocationSite.setActualReceiverType(receiverType);
											}
											// return the fieldBinding if it is not declared in a superclass of the scope's binding (that is, inherited)
											return insideProblem == null ? fieldBinding : insideProblem;
										}
										if (foundField.isValidBinding())
											// if a valid field was found, complain when another is found in an 'immediate' enclosing type (that is, not inherited)
											if (foundField.declaringClass != fieldBinding.declaringClass)
												// ie. have we found the same field - do not trust field identity yet
												return new ProblemFieldBinding(
													foundField, // closest match
													foundField.declaringClass,
													name,
													ProblemReasons.InheritedNameHidesEnclosingName);
									}
								}

								if (foundField == null || (foundField.problemId() == ProblemReasons.NotVisible && fieldBinding.problemId() != ProblemReasons.NotVisible)) {
									// only remember the fieldBinding if its the first one found or the previous one was not visible & fieldBinding is...
									foundDepth = depth;
									foundActualReceiverType = receiverType;
									foundInsideProblem = insideProblem;
									foundField = fieldBinding;
								}
							}
							depth++;
							insideStaticContext |= receiverType.isStatic();
							// 1EX5I8Z - accessing outer fields within a constructor call is permitted
							// in order to do so, we change the flag as we exit from the type, not the method
							// itself, because the class scope is used to retrieve the fields.
							MethodScope enclosingMethodScope = scope.methodScope();
							insideConstructorCall = enclosingMethodScope == null ? false : enclosingMethodScope.isConstructorCall;
							break;
						case WITH_SCOPE :
						{
							WithScope withScope = (WithScope) scope;
							TypeBinding withType = withScope.referenceContext;
							FieldBinding withBinding = withScope.findField(withType, name, invocationSite, needResolve);
							// Use next line instead if willing to enable protected access accross inner types
							// FieldBinding fieldBinding = findField(enclosingType, name, invocationSite);

							if (withBinding != null) { // skip it if we did not find anything
								if (withBinding.isValidBinding()) {
									return withBinding;
								}
							}
						}
							break;
							case COMPILATION_UNIT_SCOPE :
							if ( (mask & (Binding.FIELD|Binding.VARIABLE)) >0)
							{
								variableBinding = scope.findVariable(name);
							// looks in this scope only
								if (variableBinding != null) {
									if (foundField != null && foundField.isValidBinding())
										return new ProblemFieldBinding(
										foundField, // closest match
										foundField.declaringClass,
										name,
										ProblemReasons.InheritedNameHidesEnclosingName);
									if (depth > 0)
										invocationSite.setDepth(depth);
									return variableBinding;
								}

								if(unitScope.classScope()!=null) {
									//ReferenceBinding bind = env.getType(new char[][]{unitScope.superTypeName});
									//if(bind==null) break done;
									foundField = (unitScope.classScope()).findField(unitScope.superBinding, name, invocationSite, true);
									if(foundField!=null && foundField.isValidBinding()) {

										return foundField;
									}
								}



							}else if  ( (mask & (Binding.METHOD)) >0){
								MethodBinding methodBinding = (unitScope.classScope()).findMethod(unitScope.superBinding, name, new TypeBinding[0], invocationSite);
								if(methodBinding!=null && methodBinding.isValidBinding()) return methodBinding;

							}

							break done;
					}
					scope = scope.parent;
				}

				if (foundInsideProblem != null)
					return foundInsideProblem;
				if (foundField != null) {
					if (foundField.isValidBinding()) {
						if (foundDepth > 0) {
							invocationSite.setDepth(foundDepth);
							invocationSite.setActualReceiverType(foundActualReceiverType);
						}
						return foundField;
					}
					problemField = foundField;
					foundField = null;
				}

			}

			if ( (mask&Binding.METHOD)!=0)
			{
				MethodBinding methodBinding = findMethod(null, name, Binding.NO_PARAMETERS, invocationSite);
				if (methodBinding!=null && methodBinding.isValidBinding())
					return methodBinding;
			}

			if (problemField != null) return problemField;
			if (binding != null && binding.problemId() != ProblemReasons.NotFound)
				return binding; // answer the better problem binding
			return new ProblemBinding(name, enclosingTypeBinding(), ProblemReasons.NotFound);
		} catch (AbortCompilation e) {
			e.updateContext(invocationSite, referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
		}
	}

	/**
	 * <p><b>NOTE:</b> This function does not validate the given argument types because any number of arguments
	 * can be passed to any JavaScript function or constructor.</p>
	 * 
	 * @param receiverType
	 * @param argumentTypes
	 * @param invocationSite
	 * @return The constructor for the given receiver type or a {@link ProblemMethodBinding} if the
	 * constructor is not visible. 
	 */
	public MethodBinding getConstructor(ReferenceBinding receiverType, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
		CompilationUnitScope unitScope = compilationUnitScope();
		LookupEnvironment env = unitScope.environment;
		try {
			env.missingClassFileLocation = invocationSite;
			unitScope.recordTypeReference(receiverType);
			unitScope.recordTypeReferences(argumentTypes);
			MethodBinding methodBinding = receiverType.getExactConstructor(argumentTypes);
			if (methodBinding != null && methodBinding.canBeSeenBy(invocationSite, this)) {
				return methodBinding;
			}
			
			//get the methods
			MethodBinding[] methods = receiverType.sourceName != null ? receiverType.getMethods(receiverType.sourceName) : null;
			MethodBinding constructor = null;
			if (methods == null || methods == Binding.NO_METHODS || methods.length == 0){
				constructor = new MethodBinding(0, receiverType.sourceName, receiverType, null,receiverType);
			} else {
				//log warning about to many constructors
				if(methods.length > 1 && Compiler.DEBUG) {
					Logger.log(Logger.WARNING_DEBUG, "Scope#getConstructor: There should only ever be one match for a" +
							" constructor search but found " + methods.length + " when looking for " +
							new String(receiverType.sourceName) + ". Using the first match.");
				}
				
				//should only ever be one constructor so use the first one in the list
				constructor = methods[0];
			}
			
			//if can't be seen return problem binding
			if(!constructor.canBeSeenBy(invocationSite, this)) {
				constructor = new ProblemMethodBinding(
						methods[0],
						methods[0].selector,
						methods[0].parameters,
						ProblemReasons.NotVisible);
			}
			
			return constructor;
		} catch (AbortCompilation e) {
			e.updateContext(invocationSite, referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	public final PackageBinding getCurrentPackage() {
		Scope scope, unitScope = this;
		while ((scope = unitScope.parent) != null)
			unitScope = scope;
		return ((CompilationUnitScope) unitScope).getDefaultPackage();
	}

	/**
	 * Returns the modifiers of the innermost enclosing declaration.
	 * @return modifiers
	 */
	public int getDeclarationModifiers(){
		switch(this.kind){
			case Scope.BLOCK_SCOPE :
			case Scope.METHOD_SCOPE :
				MethodScope methodScope = methodScope();
				if (!methodScope.isInsideInitializer()){
					// check method modifiers to see if deprecated
					MethodBinding context = ((AbstractMethodDeclaration)methodScope.referenceContext).getBinding();
					if (context != null)
						return context.modifiers;
				} else {
					SourceTypeBinding type = ((BlockScope) this).referenceType().binding;

					// inside field declaration ? check field modifier to see if deprecated
					if (methodScope.initializedField != null)
						return methodScope.initializedField.modifiers;
					if (type != null)
						return type.modifiers;
				}
				break;
			case Scope.CLASS_SCOPE :
				ReferenceBinding context = ((ClassScope)this).referenceType().binding;
				if (context != null)
					return context.modifiers;
				break;
		}
		return -1;
	}

	public FieldBinding getField(TypeBinding receiverType, char[] fieldName, InvocationSite invocationSite) {
		LookupEnvironment env = environment();
		try {
			env.missingClassFileLocation = invocationSite;
			FieldBinding field = findField(receiverType, fieldName, invocationSite, true /*resolve*/);
			if (field != null) return field;

			return new ProblemFieldBinding(
				receiverType instanceof ReferenceBinding ? (ReferenceBinding) receiverType : null,
				fieldName,
				ProblemReasons.NotFound);
		} catch (AbortCompilation e) {
			e.updateContext(invocationSite, referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	public Binding getFieldOrMethod( TypeBinding receiverType, char[] fieldName, InvocationSite invocationSite ) {
		LookupEnvironment env = environment();
		try {
			env.missingClassFileLocation = invocationSite;
			//first look for field
			FieldBinding field = findField(receiverType, fieldName, invocationSite, true /*resolve*/);
			if (field != null) {
				return field;
			}

			/* not sure if this fix is correct, but receiver type is [sometimes] coming in as "BaseTypeBinding" and causing a classcastexception */
			MethodBinding method = findMethod(
						receiverType instanceof ReferenceBinding?(ReferenceBinding)receiverType:null,
						fieldName, null, invocationSite );
			if( method != null )
			{
				if (!method.isValidBinding())
				{
					if (method.problemId()!=ProblemReasons.NotFound)
						return method;
				}
				else
					return method;
			}

			return new ProblemFieldBinding(
				receiverType instanceof ReferenceBinding ? (ReferenceBinding) receiverType : null,
				fieldName,
				ProblemReasons.NotFound);
		} catch (AbortCompilation e) {
			e.updateContext(invocationSite, referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	/* API
	 *
	 *	Answer the method binding that corresponds to selector, argumentTypes.
	 *	Start the lookup at the enclosing type of the receiver.
	 *	InvocationSite implements
	 *		isSuperAccess(); this is used to determine if the discovered method is visible.
	 *		setDepth(int); this is used to record the depth of the discovered method
	 *			relative to the enclosing type of the receiver. (If the method is defined
	 *			in the enclosing type of the receiver, the depth is 0; in the next enclosing
	 *			type, the depth is 1; and so on
	 *
	 *	If no visible method is discovered, an error binding is answered.
	 */
	public MethodBinding getImplicitMethod(char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {

		boolean insideStaticContext = false;
		boolean insideConstructorCall = false;
		MethodBinding foundMethod = null;
		MethodBinding foundProblem = null;
		boolean foundProblemVisible = false;
		Scope scope = this;
		int depth = 0;
		// in 1.4 mode (inherited visible shadows enclosing)
		CompilerOptions options;
		boolean inheritedHasPrecedence = (options = compilerOptions()).complianceLevel >= ClassFileConstants.JDK1_4;

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found
			switch (scope.kind) {
				case METHOD_SCOPE :
					MethodScope methodScope = (MethodScope) scope;
					insideStaticContext |= methodScope.isStatic;
					insideConstructorCall |= methodScope.isConstructorCall;
					MethodBinding binding = methodScope.findMethod(selector,argumentTypes,true);
					if (binding!=null)
						return binding;
					LocalVariableBinding variable = methodScope.findVariable(selector);
					if (variable!=null)
					{
						
					}
					break;
				case WITH_SCOPE :
					WithScope withScope = (WithScope) scope;
					ReferenceBinding withType = withScope.referenceContext;
					// retrieve an exact visible match (if possible)
					// compilationUnitScope().recordTypeReference(receiverType);   not needed since receiver is the source type
					MethodBinding methBinding = withScope.findExactMethod(withType, selector, argumentTypes, invocationSite);
					if (methBinding == null)
						methBinding = withScope.findMethod(withType, selector, argumentTypes, invocationSite);
					if (methBinding != null) { // skip it if we did not find anything
							if (methBinding.isValidBinding()) {
									return methBinding;
								}
					}
					break;
				case CLASS_SCOPE :
					ClassScope classScope = (ClassScope) scope;
					ReferenceBinding receiverType = classScope.enclosingReceiverType();
					// retrieve an exact visible match (if possible)
					// compilationUnitScope().recordTypeReference(receiverType);   not needed since receiver is the source type
					MethodBinding methodBinding = classScope.findExactMethod(receiverType, selector, argumentTypes, invocationSite);
					if (methodBinding == null)
						methodBinding = classScope.findMethod(receiverType, selector, argumentTypes, invocationSite);
					if (methodBinding != null) { // skip it if we did not find anything
						if (foundMethod == null) {
							if (methodBinding.isValidBinding()) {
								if (!methodBinding.isStatic() && (insideConstructorCall || insideStaticContext)) {
									if (foundProblem != null && foundProblem.problemId() != ProblemReasons.NotVisible)
										return foundProblem; // takes precedence
									return new ProblemMethodBinding(
										methodBinding, // closest match
										methodBinding.selector,
										methodBinding.parameters,
										insideConstructorCall
											? ProblemReasons.NonStaticReferenceInConstructorInvocation
											: ProblemReasons.NonStaticReferenceInStaticContext);
								}
								if (inheritedHasPrecedence
										|| receiverType == methodBinding.declaringClass
										|| (receiverType.getMethods(selector)) != Binding.NO_METHODS) {
									// found a valid method in the 'immediate' scope (ie. not inherited)
									// OR in 1.4 mode (inherited visible shadows enclosing)
									// OR the receiverType implemented a method with the correct name
									// return the methodBinding if it is not declared in a superclass of the scope's binding (that is, inherited)
									if (foundProblemVisible) {
										return foundProblem;
									}
									if (depth > 0) {
										invocationSite.setDepth(depth);
										invocationSite.setActualReceiverType(receiverType);
									}
									return methodBinding;
								}

								if (foundProblem == null || foundProblem.problemId() == ProblemReasons.NotVisible) {
									if (foundProblem != null) foundProblem = null;
									// only remember the methodBinding if its the first one found
									// remember that private methods are visible if defined directly by an enclosing class
									if (depth > 0) {
										invocationSite.setDepth(depth);
										invocationSite.setActualReceiverType(receiverType);
									}
									foundMethod = methodBinding;
								}
							} else { // methodBinding is a problem method
								if (methodBinding.problemId() != ProblemReasons.NotVisible && methodBinding.problemId() != ProblemReasons.NotFound)
									return methodBinding; // return the error now
								if (foundProblem == null) {
									foundProblem = methodBinding; // hold onto the first not visible/found error and keep the second not found if first is not visible
								}
								if (! foundProblemVisible && methodBinding.problemId() == ProblemReasons.NotFound) {
									MethodBinding closestMatch = ((ProblemMethodBinding) methodBinding).closestMatch;
									if (closestMatch != null && closestMatch.canBeSeenBy(receiverType, invocationSite, this)) {
										foundProblem = methodBinding; // hold onto the first not visible/found error and keep the second not found if first is not visible
										foundProblemVisible = true;
									}
								}
							}
						} else { // found a valid method so check to see if this is a hiding case
							if (methodBinding.problemId() == ProblemReasons.Ambiguous
								|| (foundMethod.declaringClass != methodBinding.declaringClass
									&& (receiverType == methodBinding.declaringClass || receiverType.getMethods(selector) != Binding.NO_METHODS)))
								// ambiguous case -> must qualify the method (javac generates an ambiguous error instead)
								// otherwise if a method was found, complain when another is found in an 'immediate' enclosing type (that is, not inherited)
								// NOTE: Unlike fields, a non visible method hides a visible method
								return new ProblemMethodBinding(
									methodBinding, // closest match
									selector,
									argumentTypes,
									ProblemReasons.InheritedNameHidesEnclosingName);
						}
					}
					
					depth++;
					insideStaticContext |= receiverType.isStatic();
					// 1EX5I8Z - accessing outer fields within a constructor call is permitted
					// in order to do so, we change the flag as we exit from the type, not the method
					// itself, because the class scope is used to retrieve the fields.
					MethodScope enclosingMethodScope = scope.methodScope();
					insideConstructorCall = enclosingMethodScope == null ? false : enclosingMethodScope.isConstructorCall;
					break;
				case COMPILATION_UNIT_SCOPE :
					CompilationUnitScope compilationUnitScope = (CompilationUnitScope) scope;
					CompilationUnitBinding compilationUnitBinding = compilationUnitScope.enclosingCompilationUnit();
					 receiverType = compilationUnitBinding;
						methodBinding =
							(foundMethod == null)
								? compilationUnitScope.findExactMethod(receiverType, selector, argumentTypes, invocationSite)
								: compilationUnitScope.findExactMethod(receiverType, foundMethod.selector, foundMethod.parameters, invocationSite);
						if (methodBinding == null)
							methodBinding = compilationUnitScope.findMethod(receiverType, selector, argumentTypes, invocationSite);
						if (methodBinding == null)
							methodBinding = compilationUnitScope.findMethod(selector, argumentTypes,true);
						if (methodBinding != null) { // skip it if we did not find anything
							if (methodBinding.problemId() == ProblemReasons.Ambiguous) {
								if (foundMethod == null || foundMethod.problemId() == ProblemReasons.NotVisible) {
									// supercedes any potential InheritedNameHidesEnclosingName problem
									return methodBinding;
								}
								// make the user qualify the method, likely wants the first inherited method (javac generates an ambiguous error instead)
								return new ProblemMethodBinding(
									methodBinding, // closest match
									selector,
									argumentTypes,
									ProblemReasons.InheritedNameHidesEnclosingName);
							}
							MethodBinding fuzzyProblem = null;
							MethodBinding insideProblem = null;

								if (foundMethod == null) {
									if (receiverType == methodBinding.declaringClass
										|| (receiverType.getMethods(selector)) != Binding.NO_METHODS
										|| ((foundProblem == null || foundProblem.problemId() != ProblemReasons.NotVisible) && compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)) {
											// found a valid method in the 'immediate' scope (ie. not inherited)
											// OR the receiverType implemented a method with the correct name
											// OR in 1.4 mode (inherited visible shadows enclosing)
											if (depth > 0) {
												invocationSite.setDepth(depth);
												invocationSite.setActualReceiverType(receiverType);
											}
											// return the methodBinding if it is not declared in a superclass of the scope's binding (that is, inherited)
											if (foundProblem != null && foundProblem.problemId() != ProblemReasons.NotVisible)
												return foundProblem;
											if (insideProblem != null)
												return insideProblem;
											return methodBinding;
										}
									}

							if (foundMethod == null || (foundMethod.problemId() == ProblemReasons.NotVisible && methodBinding.problemId() != ProblemReasons.NotVisible)) {
								// only remember the methodBinding if its the first one found or the previous one was not visible & methodBinding is...
								// remember that private methods are visible if defined directly by an enclosing class
								if (depth > 0) {
									invocationSite.setDepth(depth);
									invocationSite.setActualReceiverType(receiverType);
								}
								foundProblem = fuzzyProblem;
								foundProblem = insideProblem;
								if (fuzzyProblem == null)
									foundMethod = methodBinding; // only keep it if no error was found
							}
						}
					depth++;
					insideStaticContext |= receiverType.isStatic();

					break done;
			}
			scope = scope.parent;
		}

		if (insideStaticContext && options.sourceLevel >= ClassFileConstants.JDK1_5) {
			if (foundProblem != null) {
				if (foundProblem.declaringClass != null && foundProblem.declaringClass.id == TypeIds.T_JavaLangObject)
					return foundProblem; // static imports lose to methods from Object
				if (foundProblem.problemId() == ProblemReasons.NotFound && foundProblemVisible) {
					return foundProblem; // visible method selectors take precedence
				}
			}
		}

		if (foundMethod != null) {
			invocationSite.setActualReceiverType(foundMethod.declaringClass);
			return foundMethod;
		}
		if (foundProblem != null)
			return foundProblem;

		return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
	}

	public final ReferenceBinding getJavaLangAssertionError() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(JAVA_LANG_ASSERTIONERROR);
		return unitScope.environment.getResolvedType(JAVA_LANG_ASSERTIONERROR, this);
	}

	public final ReferenceBinding getJavaLangClass() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(JAVA_LANG_CLASS);
		return unitScope.environment.getResolvedType(JAVA_LANG_CLASS, this);
	}

	public final ReferenceBinding getJavaLangIterable() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(JAVA_LANG_ITERABLE);
		return unitScope.environment.getResolvedType(JAVA_LANG_ITERABLE, this);
	}
	public final ReferenceBinding getJavaLangObject() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(JAVA_LANG_OBJECT);
		return unitScope.environment.getResolvedType(JAVA_LANG_OBJECT, this);
	}

	public final ReferenceBinding getJavaLangArray() {
		compilationUnitScope().recordQualifiedReference(ARRAY);
		return environment().getResolvedType(ARRAY, this);
	}

	public final ReferenceBinding getJavaLangString() {
		compilationUnitScope().recordQualifiedReference(JAVA_LANG_STRING);
		return environment().getResolvedType(JAVA_LANG_STRING, this);
	}

	public final ReferenceBinding getJavaLangNumber() {
		compilationUnitScope().recordQualifiedReference(NUMBER);
		return environment().getResolvedType(NUMBER, this);
	}

	public final ReferenceBinding getJavaLangFunction() {
		compilationUnitScope().recordQualifiedReference(FUNCTION);
		return environment().getResolvedType(FUNCTION, this);
	}

	public final ReferenceBinding getJavaLangBoolean() {
		compilationUnitScope().recordQualifiedReference(BOOLEAN_OBJECT);
		return environment().getResolvedType(BOOLEAN_OBJECT, this);
	}


	public final ReferenceBinding getJavaLangThrowable() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(JAVA_LANG_THROWABLE);
		return unitScope.environment.getResolvedType(JAVA_LANG_THROWABLE, this);
	}
	
	public final ReferenceBinding getJavaLangError() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(ERROR);
		return unitScope.environment.getResolvedType(ERROR, this);
	}
	
	public final ReferenceBinding getJavaLangRegExp() {
		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(REGEXP);
		return unitScope.environment.getResolvedType(REGEXP, this);
	}

	/* Answer the type binding corresponding to the typeName argument, relative to the enclosingType.
	*/
	public final ReferenceBinding getMemberType(char[] typeName, ReferenceBinding enclosingType) {
		ReferenceBinding memberType = findMemberType(typeName, enclosingType);
		if (memberType != null) return memberType;
		return new ProblemReferenceBinding(typeName, null, ProblemReasons.NotFound);
	}

	public MethodBinding getMethod(TypeBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
		CompilationUnitScope unitScope = compilationUnitScope();
		LookupEnvironment env = unitScope.environment;
		try {
			env.missingClassFileLocation = invocationSite;
			if (receiverType==null)
				return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
			switch (receiverType.kind()) {
				case Binding.BASE_TYPE :
					return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
//				case Binding.ARRAY_TYPE :
//					unitScope.recordTypeReference(receiverType);
//					return findMethodForArray((ArrayBinding) receiverType, selector, argumentTypes, invocationSite);
			}
			unitScope.recordTypeReference(receiverType);

			ReferenceBinding currentType = (ReferenceBinding) receiverType;
			if (!currentType.canBeSeenBy(this))
				return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.ReceiverTypeNotVisible);

			// retrieve an exact visible match (if possible)
			MethodBinding methodBinding = findExactMethod(currentType, selector, argumentTypes, invocationSite);
			if (methodBinding != null) return methodBinding;

			methodBinding = findMethod(currentType, selector, argumentTypes, invocationSite);
			if (methodBinding == null)
				return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
			if (!methodBinding.isValidBinding())
				return methodBinding;

			return methodBinding;
		} catch (AbortCompilation e) {
			e.updateContext(invocationSite, referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	/* Answer the package from the compoundName or null if it begins with a type.
	* Intended to be used while resolving a qualified type name.
	*
	* NOTE: If a problem binding is returned, senders should extract the compound name
	* from the binding & not assume the problem applies to the entire compoundName.
	*/
	public final Binding getPackage(char[][] compoundName) {
		compilationUnitScope().recordQualifiedReference(compoundName);
		Binding binding = getTypeOrPackage(compoundName[0], Binding.TYPE | Binding.PACKAGE);
		if (binding == null)
			return new ProblemReferenceBinding(compoundName[0], null, ProblemReasons.NotFound);
		if (!binding.isValidBinding())
			return binding;

		if (!(binding instanceof PackageBinding)) return null; // compoundName does not start with a package

		int currentIndex = 1;
		PackageBinding packageBinding = (PackageBinding) binding;
		while (currentIndex < compoundName.length) {
			binding = packageBinding.getTypeOrPackage(compoundName[currentIndex++],  Binding.PACKAGE);
			if (binding == null)
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					null,
					ProblemReasons.NotFound);
			if (!binding.isValidBinding())
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					binding instanceof ReferenceBinding ? ((ReferenceBinding)binding).closestMatch() : null,
					binding.problemId());
			if (!(binding instanceof PackageBinding))
				return packageBinding;
			packageBinding = (PackageBinding) binding;
		}
		return new ProblemReferenceBinding(compoundName, null, ProblemReasons.NotFound);
	}

	/* Answer the type binding that corresponds the given name, starting the lookup in the receiver.
	* The name provided is a simple source name (e.g., "Object" , "Point", ...)
	*/
	// The return type of this method could be ReferenceBinding if we did not answer base types.
	// NOTE: We could support looking for Base Types last in the search, however any code using
	// this feature would be extraordinarily slow.  Therefore we don't do this
	public final TypeBinding getType(char[] name) {
		// Would like to remove this test and require senders to specially handle base types
		TypeBinding binding = getBaseType(name);
		if (binding != null) return binding;
		return (ReferenceBinding) getTypeOrPackage(name, Binding.TYPE);
	}

	/* Answer the type binding that corresponds to the given name, starting the lookup in the receiver
	* or the packageBinding if provided.
	* The name provided is a simple source name (e.g., "Object" , "Point", ...)
	*/
	public final TypeBinding getType(char[] name, PackageBinding packageBinding) {
		if (packageBinding == null)
			return getType(name);

		Binding binding = packageBinding.getTypeOrPackage(name,  Binding.TYPE);
		if (binding == null)
			return new ProblemReferenceBinding(
				CharOperation.arrayConcat(packageBinding.compoundName, name),
				null,
				ProblemReasons.NotFound);
		if (!binding.isValidBinding())
			return new ProblemReferenceBinding(
				CharOperation.arrayConcat(packageBinding.compoundName, name),
				binding instanceof ReferenceBinding ? ((ReferenceBinding)binding).closestMatch() : null,
				binding.problemId());

		ReferenceBinding typeBinding = (ReferenceBinding) binding;
		if (!typeBinding.canBeSeenBy(this))
			return new ProblemReferenceBinding(
				CharOperation.arrayConcat(packageBinding.compoundName, name),
				typeBinding,
				ProblemReasons.NotVisible);
		return typeBinding;
	}

	/* Answer the type binding corresponding to the compoundName.
	*
	* NOTE: If a problem binding is returned, senders should extract the compound name
	* from the binding & not assume the problem applies to the entire compoundName.
	*/
	public final TypeBinding getType(char[][] compoundName, int typeNameLength) {
		if (typeNameLength == 1) {
			// Would like to remove this test and require senders to specially handle base types
			TypeBinding binding = getBaseType(compoundName[0]);
			if (binding != null) return binding;
		}

		CompilationUnitScope unitScope = compilationUnitScope();
		unitScope.recordQualifiedReference(compoundName);
		Binding binding =
			getTypeOrPackage(compoundName[0], typeNameLength == 1 ? Binding.TYPE : Binding.TYPE | Binding.PACKAGE);
		if (binding == null)
			return new ProblemReferenceBinding(compoundName[0], null, ProblemReasons.NotFound);
		if (!binding.isValidBinding())
			return (ReferenceBinding) binding;

		int currentIndex = 1;
		boolean checkVisibility = false;
		if (binding instanceof PackageBinding) {
			PackageBinding packageBinding = (PackageBinding) binding;
			while (currentIndex < typeNameLength) {
				binding = packageBinding.getTypeOrPackage(compoundName[currentIndex++], Binding.TYPE); // does not check visibility
				if (binding == null)
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						null,
						ProblemReasons.NotFound);
				if (!binding.isValidBinding())
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						binding instanceof ReferenceBinding ? ((ReferenceBinding)binding).closestMatch() : null,
						binding.problemId());
				if (!(binding instanceof PackageBinding))
					break;
				packageBinding = (PackageBinding) binding;
			}
			if (binding instanceof PackageBinding)
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					null,
					ProblemReasons.NotFound);
			checkVisibility = true;
		}

		// binding is now a ReferenceBinding
		ReferenceBinding typeBinding = (ReferenceBinding) binding;
		unitScope.recordTypeReference(typeBinding);
		if (checkVisibility) // handles the fall through case
			if (!typeBinding.canBeSeenBy(this))
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					typeBinding,
					ProblemReasons.NotVisible);

		while (currentIndex < typeNameLength) {
			typeBinding = getMemberType(compoundName[currentIndex++], typeBinding);
			if (!typeBinding.isValidBinding()) {
				if (typeBinding instanceof ProblemReferenceBinding) {
					ProblemReferenceBinding problemBinding = (ProblemReferenceBinding) typeBinding;
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						problemBinding.closestMatch(),
						typeBinding.problemId());
				}
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					((ReferenceBinding)binding).closestMatch(),
					typeBinding.problemId());
			}
		}
		return typeBinding;
	}

	/* Internal use only
	*/
	final Binding getTypeOrPackage(char[] name, int mask) {
		Scope scope = this;
		Binding foundType = null;
		boolean insideStaticContext = false;
		if ((mask & Binding.TYPE) == 0) {
			Scope next = scope;
			while ((next = scope.parent) != null)
				scope = next;
		} else {
			done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found
				switch (scope.kind) {
					case METHOD_SCOPE :
						MethodScope methodScope = (MethodScope) scope;
						insideStaticContext |= methodScope.isStatic;
					case BLOCK_SCOPE :
						ReferenceBinding localType = ((BlockScope) scope).findLocalType(name); // looks in this scope only
						if (localType != null) {
							return localType;
						}
						break;
					case CLASS_SCOPE :
						SourceTypeBinding sourceType = ((ClassScope) scope).getReferenceBinding();
						
						// type variables take precedence over the source type, ex. class X <X> extends X == class X <Y> extends Y
						// but not when we step out to the enclosing type
						if (CharOperation.equals(name, sourceType.sourceName))
							return sourceType;
						insideStaticContext |= sourceType.isStatic();
						break;
					case COMPILATION_UNIT_SCOPE :
						break done;
				}
				scope = scope.parent;
			}
		}

		// at this point the scope is a compilation unit scope
		CompilationUnitScope unitScope = (CompilationUnitScope) scope;
		HashtableOfObject typeOrPackageCache = unitScope.typeOrPackageCache;
		if (typeOrPackageCache != null) {
			Binding binding = (Binding) typeOrPackageCache.get(name);
			if (binding != null) { // can also include NotFound ProblemReferenceBindings if we already know this name is not found
				if (binding instanceof ImportBinding) { // single type import cached in faultInImports(), replace it in the cache with the type
					ImportReference importReference = ((ImportBinding) binding).reference;
					if (importReference != null)
						importReference.bits |= ASTNode.Used;
					if (binding instanceof ImportConflictBinding)
						typeOrPackageCache.put(name, binding = ((ImportConflictBinding) binding).conflictingTypeBinding); // already know its visible
					else
						typeOrPackageCache.put(name, binding = ((ImportBinding) binding).resolvedImport); // already know its visible
				}
				if ((mask & Binding.TYPE) != 0) {
					if (binding instanceof ReferenceBinding)
						return binding; // cached type found in previous walk below
				}
				if ((mask & Binding.PACKAGE) != 0 && binding instanceof PackageBinding)
					return binding; // cached package found in previous walk below
			}
		}

		// ask for the imports + name
		if ((mask & Binding.TYPE|Binding.VARIABLE|Binding.METHOD) != 0) {
			ImportBinding[] imports = unitScope.imports;
			if (imports != null && typeOrPackageCache == null) { // walk single type imports since faultInImports() has not run yet
				nextImport : for (int i = 0, length = imports.length; i < length; i++) {
					ImportBinding importBinding = imports[i];
					if (!importBinding.onDemand) {
						if (CharOperation.equals(importBinding.compoundName[importBinding.compoundName.length - 1], name)) {
							Binding resolvedImport = unitScope.resolveSingleImport(importBinding);
							if (resolvedImport == null) continue nextImport;
							if (resolvedImport instanceof MethodBinding) {
								resolvedImport = getType(importBinding.compoundName, importBinding.compoundName.length);
								if (!resolvedImport.isValidBinding()) continue nextImport;
							}
							if (resolvedImport instanceof TypeBinding) {
								ImportReference importReference = importBinding.reference;
								if (importReference != null)
									importReference.bits |= ASTNode.Used;
								return resolvedImport; // already know its visible
							}
						}
					}
				}
			}


			// check on file imports
			if (imports != null) {
				for (int i = 0, length = imports.length; i < length; i++) {
					ImportBinding someImport = imports[i];
					if (someImport.reference!=null && someImport.reference.isFileImport())
					{
						Binding resolvedImport = someImport.resolvedImport;
						Binding temp = null;

						if (resolvedImport instanceof CompilationUnitBinding) {
							CompilationUnitBinding compilationUnitBinding =(CompilationUnitBinding)resolvedImport;
							
							temp = findBinding(name, mask, compilationUnitBinding.getPackage(), unitScope.getDefaultPackage(), false);
							if (temp!=null && temp.isValidBinding())
							{
								ImportReference importReference = someImport.reference;
								importReference.bits |= ASTNode.Used;
								if (typeOrPackageCache != null)
									typeOrPackageCache.put(name, temp);
								return temp; // type is always visible to its own package
							}
						}
						

					}
				}
			}
			
			// check if the name is in the current package, skip it if its a sub-package
			PackageBinding currentPackage = unitScope.getDefaultPackage();
			unitScope.recordReference(currentPackage.compoundName, name);
			Binding binding=currentPackage.getTypeOrPackage(name, mask);
			if ( (binding instanceof ReferenceBinding || binding instanceof MethodBinding)
					&& !(binding instanceof ProblemReferenceBinding)) {
				if (typeOrPackageCache != null)
					typeOrPackageCache.put(name, binding);
				return binding; // type is always visible to its own package
			}
			else if (binding instanceof LocalVariableBinding && binding.isValidBinding())
			{
				compilationUnitScope().addExternalVar((LocalVariableBinding)binding);
				return binding;
			}

			// check on demand imports
			if (imports != null) {
				boolean foundInImport = false;
				Binding type = null;
				for (int i = 0, length = imports.length; i < length; i++) {
					ImportBinding someImport = imports[i];
					if (someImport.onDemand) {
						Binding resolvedImport = someImport.resolvedImport;
						Binding temp = null;
						if (resolvedImport instanceof PackageBinding) {
							temp = findBinding(name, mask, (PackageBinding) resolvedImport, currentPackage, false);
						} else {
							temp = findDirectMemberType(name, (ReferenceBinding) resolvedImport);
						}
						if (temp != type && temp != null) {
							if (temp.isValidBinding()) {
								ImportReference importReference = someImport.reference;
								if (importReference != null)
									importReference.bits |= ASTNode.Used;
								if (foundInImport) {
									// Answer error binding -- import on demand conflict; name found in two import on demand packages.
									temp = new ProblemReferenceBinding(name, null, ProblemReasons.Ambiguous);
									if (typeOrPackageCache != null)
										typeOrPackageCache.put(name, temp);
									return temp;
								}
								type =  temp;
								foundInImport = true;
							} else if (foundType == null) {
								foundType = temp;
							}
						}
					}
				}
				if (type != null) {
					if (typeOrPackageCache != null)
						typeOrPackageCache.put(name, type);
					return type;
				}
			}
		}

		unitScope.recordSimpleReference(name);
		if ((mask & Binding.PACKAGE) != 0) {
			PackageBinding packageBinding = unitScope.environment.getTopLevelPackage(name);
			if (packageBinding != null) {
				if (typeOrPackageCache != null)
					typeOrPackageCache.put(name, packageBinding);
				return packageBinding;
			}
		}

		// Answer error binding -- could not find name
		if (foundType == null) {
			foundType = new ProblemReferenceBinding(name, null, ProblemReasons.NotFound);
			if (typeOrPackageCache != null && (mask & Binding.PACKAGE) != 0) // only put NotFound type in cache if you know its not a package
				typeOrPackageCache.put(name, foundType);
		}
		return foundType;
	}

	// Added for code assist... NOT Public API
	// DO NOT USE to resolve import references since this method assumes 'A.B' is relative to a single type import of 'p1.A'
	// when it may actually mean the type B in the package A
	// use CompilationUnitScope.getImport(char[][]) instead
	public final Binding getTypeOrPackage(char[][] compoundName) {
		return getTypeOrPackage(compoundName,Binding.TYPE | Binding.PACKAGE);
	}

	public final Binding getTypeOrPackage(char[][] compoundName, int mask) {
		int nameLength = compoundName.length;
		if (nameLength == 1) {
			TypeBinding binding = getBaseType(compoundName[0]);
			if (binding != null) return binding;
		}
		Binding binding = getTypeOrPackage(compoundName[0], Binding.TYPE | Binding.PACKAGE);
		if (!binding.isValidBinding()) return binding;

		int currentIndex = 1;
		boolean checkVisibility = false;
		if (binding instanceof PackageBinding) {
			PackageBinding packageBinding = (PackageBinding) binding;

			while (currentIndex < nameLength) {
				binding = packageBinding.getTypeOrPackage(compoundName[currentIndex++], mask);
				if (binding == null)
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						null,
						ProblemReasons.NotFound);
				if (!binding.isValidBinding())
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						binding instanceof ReferenceBinding ? ((ReferenceBinding)binding).closestMatch() : null,
						binding.problemId());
				if (!(binding instanceof PackageBinding))
					break;
				packageBinding = (PackageBinding) binding;
			}
			if (binding instanceof PackageBinding) return binding;
			checkVisibility = true;
		}
		// binding is now a ReferenceBinding
		ReferenceBinding typeBinding = (ReferenceBinding) binding;
		ReferenceBinding qualifiedType = typeBinding;

		if (checkVisibility) // handles the fall through case
			if (!typeBinding.canBeSeenBy(this))
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					typeBinding,
					ProblemReasons.NotVisible);

		while (currentIndex < nameLength) {
			typeBinding = getMemberType(compoundName[currentIndex++], typeBinding);
			// checks visibility
			if (!typeBinding.isValidBinding())
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					((ReferenceBinding)binding).closestMatch(),
					typeBinding.problemId());

			
			qualifiedType = typeBinding;
			
		}
		return qualifiedType;
	}

	protected boolean hasErasedCandidatesCollisions(TypeBinding one, TypeBinding two, Map invocations, ReferenceBinding type, ASTNode typeRef) {
		invocations.clear();
		TypeBinding[] mecs = minimalErasedCandidates(new TypeBinding[] {one, two}, invocations);
		if (mecs != null) {
			nextCandidate: for (int k = 0, max = mecs.length; k < max; k++) {
				TypeBinding mec = mecs[k];
				if (mec == null) continue nextCandidate;
				Object value = invocations.get(mec);
				if (value instanceof TypeBinding[]) {
					TypeBinding[] invalidInvocations = (TypeBinding[]) value;
					type.tagBits |= TagBits.HierarchyHasProblems;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the immediately enclosing switchCase statement (carried by closest blockScope),
	 */
	public CaseStatement innermostSwitchCase() {
		Scope scope = this;
		do {
			if (scope instanceof BlockScope)
				return ((BlockScope) scope).enclosingCase;
			scope = scope.parent;
		} while (scope != null);
		return null;
	}

	protected boolean isAcceptableMethod(MethodBinding one, MethodBinding two) {
		TypeBinding[] oneParams = one.parameters;
		TypeBinding[] twoParams = two.parameters;
		int oneParamsLength = oneParams.length;
		int twoParamsLength = twoParams.length;
		if (oneParamsLength == twoParamsLength) {
			for (int i = 0; i < oneParamsLength; i++) {
				TypeBinding oneParam = oneParams[i];
				TypeBinding twoParam = twoParams[i];
				if (oneParam == twoParam) {
					continue;
				}
				if (oneParam.isCompatibleWith(twoParam)) {
					
				} else {
					if (i == oneParamsLength - 1 && one.isVarargs() && two.isVarargs()) {
						TypeBinding eType = ((ArrayBinding) twoParam).elementsType();
						if (oneParam == eType || oneParam.isCompatibleWith(eType))
							return true; // special case to choose between 2 varargs methods when the last arg is Object[]
					}
					return false;
				}
			}
			return true;
		}

		if (one.isVarargs() && two.isVarargs()) {
			if (oneParamsLength > twoParamsLength) {
				// special case when autoboxing makes (int, int...) better than (Object...) but not (int...) or (Integer, int...)
				if (((ArrayBinding) twoParams[twoParamsLength - 1]).elementsType().id != TypeIds.T_JavaLangObject)
					return false;
			}
			// check that each parameter before the vararg parameters are compatible (no autoboxing allowed here)
			for (int i = (oneParamsLength > twoParamsLength ? twoParamsLength : oneParamsLength) - 2; i >= 0; i--)
				if (oneParams[i] != twoParams[i] && !oneParams[i].isCompatibleWith(twoParams[i]))
					return false;
			if (parameterCompatibilityLevel(one, twoParams) == NOT_COMPATIBLE
				&& parameterCompatibilityLevel(two, oneParams) == VARARGS_COMPATIBLE)
					return true;
		}
		return false;
	}

	public boolean isBoxingCompatibleWith(TypeBinding expressionType, TypeBinding targetType) {
		LookupEnvironment environment = environment();
		if (environment.globalOptions.sourceLevel < ClassFileConstants.JDK1_5 || expressionType.isBaseType() == targetType.isBaseType())
			return false;

		// check if autoboxed type is compatible
		TypeBinding convertedType = environment.computeBoxingType(expressionType);
		return convertedType == targetType || convertedType.isCompatibleWith(targetType);
	}

	/* Answer true if the scope is nested inside a given field declaration.
	 * Note: it works as long as the scope.fieldDeclarationIndex is reflecting the field being traversed
	 * e.g. during name resolution.
	*/
	public final boolean isDefinedInField(FieldBinding field) {
		Scope scope = this;
		do {
			if (scope instanceof MethodScope) {
				MethodScope methodScope = (MethodScope) scope;
				if (methodScope.initializedField == field) return true;
			}
			scope = scope.parent;
		} while (scope != null);
		return false;
	}

	/* Answer true if the scope is nested inside a given method declaration
	*/
	public final boolean isDefinedInMethod(MethodBinding method) {
		Scope scope = this;
		do {
			if (scope instanceof MethodScope) {
				ReferenceContext refContext = ((MethodScope) scope).referenceContext;
				if (refContext instanceof AbstractMethodDeclaration)
					if (((AbstractMethodDeclaration) refContext).getBinding() == method)
						return true;
			}
			scope = scope.parent;
		} while (scope != null);
		return false;
	}

	/* Answer whether the type is defined in the same compilation unit as the receiver
	*/
	public final boolean isDefinedInSameUnit(ReferenceBinding type) {
		// find the outer most enclosing type
		ReferenceBinding enclosingType = type;
		while ((type = enclosingType.enclosingType()) != null)
			enclosingType = type;

		// find the compilation unit scope
		Scope scope, unitScope = this;
		while ((scope = unitScope.parent) != null)
			unitScope = scope;

		// test that the enclosingType is not part of the compilation unit
		SourceTypeBinding[] topLevelTypes = ((CompilationUnitScope) unitScope).topLevelTypes;
		for (int i = topLevelTypes.length; --i >= 0;)
			if (topLevelTypes[i] == enclosingType)
				return true;
		return false;
	}

	/* Answer true if the scope is nested inside a given type declaration
	*/
	public final boolean isDefinedInType(ReferenceBinding type) {
		Scope scope = this;
		do {
			if (scope instanceof ClassScope)
				if (((ClassScope) scope).getReferenceBinding() == type)
					return true;
			scope = scope.parent;
		} while (scope != null);
		return false;
	}

	/**
	 * Returns true if the scope or one of its parent is associated to a given caseStatement, denoting
	 * being part of a given switch case statement.
	 */
	public boolean isInsideCase(CaseStatement caseStatement) {
		Scope scope = this;
		do {
			switch (scope.kind) {
				case Scope.BLOCK_SCOPE :
					if (((BlockScope) scope).enclosingCase == caseStatement) {
						return true;
					}
			}
			scope = scope.parent;
		} while (scope != null);
		return false;
	}

	public boolean isInsideDeprecatedCode(){
		switch(this.kind){
			case Scope.BLOCK_SCOPE :
			case Scope.METHOD_SCOPE :
				MethodScope methodScope = methodScope();
				if (methodScope != null) {
					if (!methodScope.isInsideInitializer()){
						// check method modifiers to see if deprecated
						MethodBinding context = ((AbstractMethodDeclaration)methodScope.referenceContext).getBinding();
						if (context != null && context.isViewedAsDeprecated())
							return true;
					} else {
						SourceTypeBinding type = ((BlockScope)this).referenceType().binding;
						// inside field declaration ? check field modifier to see if deprecated
						if (methodScope.initializedField != null && methodScope.initializedField.isViewedAsDeprecated())
							return true;
						if (type != null) {
							if (type.isViewedAsDeprecated())
								return true;
						}
					}
				}
				break;
			case Scope.CLASS_SCOPE :
				ReferenceBinding context = ((ClassScope)this).referenceType().binding;
				if (context != null) {
					if (context.isViewedAsDeprecated())
						return true;
				}
				break;
			case Scope.COMPILATION_UNIT_SCOPE :
				// consider import as being deprecated if first type is itself deprecated (123522)
				CompilationUnitDeclaration unit = referenceCompilationUnit();
				if (unit.types != null && unit.types.length > 0) {
					SourceTypeBinding type = unit.types[0].binding;
					if (type != null) {
						if (type.isViewedAsDeprecated())
							return true;
					}
				}
		}
		return false;
	}

	public MethodScope methodScope() {
		Scope scope = this;
		do {
			if (scope instanceof MethodScope)
				return (MethodScope) scope;
			scope = scope.parent;
		} while (scope != null);
		return null;
	}

	/**
	 * Returns the most specific set of types compatible with all given types.
	 * (i.e. most specific common super types)
	 * If no types is given, will return an empty array. If not compatible
	 * reference type is found, returns null. In other cases, will return an array
	 * of minimal erased types, where some nulls may appear (and must simply be
	 * ignored).
	 */
	protected TypeBinding[] minimalErasedCandidates(TypeBinding[] types, Map allInvocations) {
		int length = types.length;
		int indexOfFirst = -1, actualLength = 0;
		for (int i = 0; i < length; i++) {
			TypeBinding type = types[i];
			if (type == null) continue;
			if (type.isBaseType()) return null;
			if (indexOfFirst < 0) indexOfFirst = i;
			actualLength ++;
		}
		switch (actualLength) {
			case 0: return Binding.NO_TYPES;
			case 1: return types;
		}
		TypeBinding firstType = types[indexOfFirst];
		if (firstType.isBaseType()) return null;

		// record all supertypes of type
		// intersect with all supertypes of otherType
		ArrayList typesToVisit = new ArrayList(5);

		int dim = firstType.dimensions();
		TypeBinding leafType = firstType.leafComponentType();
		TypeBinding firstErasure = firstType;
		if (firstErasure != firstType) {
			allInvocations.put(firstErasure, firstType);
		}
		typesToVisit.add(firstType);
		int max = 1;
		ReferenceBinding currentType;
		for (int i = 0; i < max; i++) {
			TypeBinding typeToVisit = (TypeBinding) typesToVisit.get(i);
			dim = typeToVisit.dimensions();
			if (dim > 0) {
				leafType = typeToVisit.leafComponentType();
				switch(leafType.id) {
					case T_JavaLangObject:
						if (dim > 1) { // Object[][] supertype is Object[]
							TypeBinding elementType = ((ArrayBinding)typeToVisit).elementsType();
							if (!typesToVisit.contains(elementType)) {
								typesToVisit.add(elementType);
								max++;
							}
							continue;
						}
						// fallthrough
					case T_short:
					case T_char:
					case T_boolean:
					case T_int:
					case T_long:
					case T_float:
					case T_double:
						TypeBinding superType = getJavaLangObject();
						if (!typesToVisit.contains(superType)) {
							typesToVisit.add(superType);
							max++;
						}
						continue;

					default:
				}
				typeToVisit = leafType;
			}
			currentType = (ReferenceBinding) typeToVisit;
			
			TypeBinding itsSuperclass = currentType.getSuperBinding();
			if (itsSuperclass != null) {
				TypeBinding superType = dim == 0 ? itsSuperclass : (TypeBinding)environment().createArrayType(itsSuperclass, dim); // recreate array if needed
				if (!typesToVisit.contains(superType)) {
					typesToVisit.add(superType);
					max++;
					TypeBinding superTypeErasure = superType;
					if (superTypeErasure != superType) {
						allInvocations.put(superTypeErasure, superType);
					}
				}
			}
		}
		int superLength = typesToVisit.size();
		TypeBinding[] erasedSuperTypes = new TypeBinding[superLength];
		int rank = 0;
		for (Iterator iter = typesToVisit.iterator(); iter.hasNext();) {
			TypeBinding type = (TypeBinding)iter.next();
			leafType = type.leafComponentType();
			erasedSuperTypes[rank++] = type;
		}
		// intersecting first type supertypes with other types' ones, nullifying non matching supertypes
		int remaining = superLength;
		nextOtherType: for (int i = indexOfFirst+1; i < length; i++) {
			TypeBinding otherType = types[i];
			if (otherType == null) continue nextOtherType;
			if (otherType.isArrayType()) {
				nextSuperType: for (int j = 0; j < superLength; j++) {
					TypeBinding erasedSuperType = erasedSuperTypes[j];
					if (erasedSuperType == null || erasedSuperType == otherType) continue nextSuperType;
					TypeBinding match;
					if ((match = otherType.findSuperTypeWithSameErasure(erasedSuperType)) == null) {
						erasedSuperTypes[j] = null;
						if (--remaining == 0) return null;
						continue nextSuperType;
					}
					// record invocation
					Object invocationData = allInvocations.get(erasedSuperType);
					if (invocationData == null) {
						allInvocations.put(erasedSuperType, match); // no array for singleton
					} else if (invocationData instanceof TypeBinding) {
						if (match != invocationData) {
							// using an array to record invocations in order (188103)
							TypeBinding[] someInvocations = { (TypeBinding) invocationData, match, };
							allInvocations.put(erasedSuperType, someInvocations);
						}
					} else { // using an array to record invocations in order (188103)
						TypeBinding[] someInvocations = (TypeBinding[]) invocationData;
						checkExisting: {
							int invocLength = someInvocations.length;
							for (int k = 0; k < invocLength; k++) {
								if (someInvocations[k] == match) break checkExisting;
							}
							System.arraycopy(someInvocations, 0, someInvocations = new TypeBinding[invocLength+1], 0, invocLength);
							allInvocations.put(erasedSuperType, someInvocations);
							someInvocations[invocLength] = match;
						}
					}
				}
				continue nextOtherType;
			}
			nextSuperType: for (int j = 0; j < superLength; j++) {
				TypeBinding erasedSuperType = erasedSuperTypes[j];
				if (erasedSuperType == null) continue nextSuperType;
				TypeBinding match;
				if (erasedSuperType == otherType) {
					match = erasedSuperType;
				} else {
					if (erasedSuperType.isArrayType()) {
						match = null;
					} else {
						match = otherType.findSuperTypeWithSameErasure(erasedSuperType);
					}
					if (match == null) { // incompatible super type
						erasedSuperTypes[j] = null;
						if (--remaining == 0) return null;
						continue nextSuperType;
					}
				}
				// record invocation
				Object invocationData = allInvocations.get(erasedSuperType);
				if (invocationData == null) {
					allInvocations.put(erasedSuperType, match); // no array for singleton
				} else if (invocationData instanceof TypeBinding) {
					if (match != invocationData) {
						// using an array to record invocations in order (188103)
						TypeBinding[] someInvocations = { (TypeBinding) invocationData, match, };
						allInvocations.put(erasedSuperType, someInvocations);
					}
				} else { // using an array to record invocations in order (188103)
					TypeBinding[] someInvocations = (TypeBinding[]) invocationData;
					checkExisting: {
						int invocLength = someInvocations.length;
						for (int k = 0; k < invocLength; k++) {
							if (someInvocations[k] == match) break checkExisting;
						}
						System.arraycopy(someInvocations, 0, someInvocations = new TypeBinding[invocLength+1], 0, invocLength);
						allInvocations.put(erasedSuperType, someInvocations);
						someInvocations[invocLength] = match;
					}
				}
			}
		}
		// eliminate non minimal super types
		if (remaining > 1) {
			nextType: for (int i = 0; i < superLength; i++) {
				TypeBinding erasedSuperType = erasedSuperTypes[i];
				if (erasedSuperType == null) continue nextType;
				nextOtherType: for (int j = 0; j < superLength; j++) {
					if (i == j) continue nextOtherType;
					TypeBinding otherType = erasedSuperTypes[j];
					if (otherType == null) continue nextOtherType;
					if (erasedSuperType instanceof ReferenceBinding) {
						if (erasedSuperType.findSuperTypeWithSameErasure(otherType) != null) {
							erasedSuperTypes[j] = null; // discard non minimal supertype
							remaining--;
						}
					} else if (erasedSuperType.isArrayType()) {
						if (erasedSuperType.findSuperTypeWithSameErasure(otherType) != null) {
							erasedSuperTypes[j] = null; // discard non minimal supertype
							remaining--;
						}
					}
				}
			}
		}
		return erasedSuperTypes;
	}


	// Internal use only
	/* All methods in visible are acceptable matches for the method in question...
	* The methods defined by the receiver type appear before those defined by its
	* superclass and so on. We want to find the one which matches best.
	*
	* Since the receiver type is a class, we know each method's declaring class is
	* either the receiver type or one of its superclasses. It is an error if the best match
	* is defined by a superclass, when a lesser match is defined by the receiver type
	* or a closer superclass.
	*/
	protected final MethodBinding mostSpecificClassMethodBinding(MethodBinding[] visible, int visibleSize, InvocationSite invocationSite) {
		MethodBinding previous = null;
		nextVisible : for (int i = 0; i < visibleSize; i++) {
			MethodBinding method = visible[i];
			if (previous != null && method.declaringClass != previous.declaringClass)
				break; // cannot answer a method farther up the hierarchy than the first method found

			if (!method.isStatic()) previous = method; // no ambiguity for static methods
			for (int j = 0; j < visibleSize; j++) {
				if (i == j) continue;
				if (!visible[j].areParametersCompatibleWith(method.parameters))
					continue nextVisible;
			}
			return method;
		}
			return new ProblemMethodBinding(visible[0], visible[0].selector, visible[0].parameters, ProblemReasons.Ambiguous);
	}

	/**
	 * caveat: this is not a direct implementation of JLS
	 * 
	 * @param visible
	 * @param visibleSize
	 * @param argumentTypes <code>null</code> means match on any arguments
	 * @param invocationSite
	 * @param receiverType
	 * @return
	 */
	protected final MethodBinding mostSpecificMethodBinding(MethodBinding[] visible, int visibleSize, TypeBinding[] argumentTypes, InvocationSite invocationSite, ReferenceBinding receiverType) {
		int[] compatibilityLevels = new int[visibleSize];
		for (int i = 0; i < visibleSize; i++)
			compatibilityLevels[i] = parameterCompatibilityLevel(visible[i], argumentTypes);

		MethodBinding[] moreSpecific = new MethodBinding[visibleSize];
		int count = 0;
		for (int level = 0, max = VARARGS_COMPATIBLE; level <= max; level++) {
			nextVisible : for (int i = 0; i < visibleSize; i++) {
				if (compatibilityLevels[i] != level) continue nextVisible;
				max = level; // do not examine further categories, will either return mostSpecific or report ambiguous case
				MethodBinding current = visible[i];
				MethodBinding original = current.original();
				MethodBinding tiebreakMethod = current.tiebreakMethod();
				for (int j = 0; j < visibleSize; j++) {
					if (i == j || compatibilityLevels[j] != level) continue;
					MethodBinding next = visible[j];
					if (original == next.original()) {
						// parameterized superclasses & interfaces may be walked twice from different paths so skip next from now on
						compatibilityLevels[j] = -1;
						continue;
					}

					MethodBinding methodToTest = next;
					MethodBinding acceptable = computeCompatibleMethod(methodToTest, tiebreakMethod.parameters, invocationSite);
					/* There are 4 choices to consider with current & next :
					 foo(B) & foo(A) where B extends A
					 1. the 2 methods are equal (both accept each others parameters) -> want to continue
					 2. current has more specific parameters than next (so acceptable is a valid method) -> want to continue
					 3. current has less specific parameters than next (so acceptable is null) -> go on to next
					 4. current and next are not compatible with each other (so acceptable is null) -> go on to next
					 */
					if (acceptable == null || !acceptable.isValidBinding())
						continue nextVisible;
					if (!isAcceptableMethod(tiebreakMethod, acceptable))
						continue nextVisible;
					// pick a concrete method over a bridge method when parameters are equal since the return type of the concrete method is more specific
					if (current.isBridge() && !next.isBridge())
						if (tiebreakMethod.areParametersEqual(acceptable))
							continue nextVisible; // skip current so acceptable wins over this bridge method
				}
				moreSpecific[i] = current;
				count++;
			}
		}
		if (count == 1) {
			for (int i = 0; i < visibleSize; i++) {
				if (moreSpecific[i] != null) {
					return visible[i];
				}
			}
		} else if (count == 0) {
			return new ProblemMethodBinding(visible[0], visible[0].selector, visible[0].parameters, ProblemReasons.Ambiguous);
		}

//		// found several methods that are mutually acceptable -> must be equal
//		// so now with the first acceptable method, find the 'correct' inherited method for each other acceptable method AND
//		// see if they are equal after substitution of type variables (do the type variables have to be equal to be considered an override???)
//		nextSpecific : for (int i = 0; i < visibleSize; i++) {
//			MethodBinding current = moreSpecific[i];
//			if (current != null) {
//				MethodBinding original = current.original();
//				for (int j = 0; j < visibleSize; j++) {
//					MethodBinding next = moreSpecific[j];
//					if (next == null || i == j) continue;
//					MethodBinding original2 = next.original();
//					if (original.declaringClass == original2.declaringClass)
//						break nextSpecific; // duplicates thru substitution
//
//					if (!original.isAbstract()) {
//						if (original2.isAbstract())
//							continue; // only compare current against other concrete methods
//						TypeBinding superType = original.declaringClass.findSuperTypeWithSameErasure(original2.declaringClass);
//						if (superType == null)
//							continue nextSpecific; // current's declaringClass is not a subtype of next's declaringClass
//					} else if (receiverType != null) { // should not be null if original isAbstract, but be safe
//						TypeBinding superType = receiverType.findSuperTypeWithSameErasure(original.declaringClass);
//						if (original.declaringClass == superType || !(superType instanceof ReferenceBinding)) {
//							// keep original
//						} else {
//							// must find inherited method with the same substituted variables
//							MethodBinding[] superMethods = ((ReferenceBinding) superType).getMethods(original.selector);
//							for (int m = 0, l = superMethods.length; m < l; m++) {
//								if (superMethods[m].original() == original) {
//									original = superMethods[m];
//									break;
//								}
//							}
//						}
//						superType = receiverType.findSuperTypeWithSameErasure(original2.declaringClass);
//						if (original2.declaringClass == superType || !(superType instanceof ReferenceBinding)) {
//							// keep original2
//						} else {
//							// must find inherited method with the same substituted variables
//							MethodBinding[] superMethods = ((ReferenceBinding) superType).getMethods(original2.selector);
//							for (int m = 0, l = superMethods.length; m < l; m++) {
//								if (superMethods[m].original() == original2) {
//									original2 = superMethods[m];
//									break;
//								}
//							}
//						}
//						if (original2 == null || !original.areParametersEqual(original2))
//							continue nextSpecific; // current does not override next
//						if (!original.returnType.isCompatibleWith(original2.returnType) &&
//								!original.returnType.isCompatibleWith(original2.returnType)) {
//							// 15.12.2
//							continue nextSpecific; // choose original2 instead
//						}
//					}
//				}
//
//				return current;
//			}
//		}

		//if can not figure which one is best, just pick first one
		return moreSpecific[0];
	}

	public final ClassScope outerMostClassScope() {
		ClassScope lastClassScope = null;
		Scope scope = this;
		do {
			if (scope instanceof ClassScope)
				lastClassScope = (ClassScope) scope;
			scope = scope.parent;
		} while (scope != null);
		return lastClassScope; // may answer null if no class around
	}

	public final MethodScope outerMostMethodScope() {
		MethodScope lastMethodScope = null;
		Scope scope = this;
		do {
			if (scope instanceof MethodScope)
				lastMethodScope = (MethodScope) scope;
			scope = scope.parent;
		} while (scope != null);
		return lastMethodScope; // may answer null if no method around
	}

	/**
	 * 
	 * @param method
	 * @param arguments <code>null</code> means match on any arguments
	 * 
	 * @return
	 */
	public int parameterCompatibilityLevel(MethodBinding method, TypeBinding[] arguments) {
		//if not arguments to compare against, assume arguments do not matter, so compatible
		if(arguments == null) {
			return COMPATIBLE;
		}
		
		TypeBinding[] parameters = method.parameters;
		int paramLength = parameters.length;
		int argLength = arguments.length;

		if (compilerOptions().sourceLevel < ClassFileConstants.JDK1_5) {
			if (paramLength != argLength)
				return NOT_COMPATIBLE;
			for (int i = 0; i < argLength; i++) {
				TypeBinding param = parameters[i];
				TypeBinding arg = arguments[i];
				if (arg != param && !arg.isCompatibleWith(param))
					return NOT_COMPATIBLE;
			}
			return COMPATIBLE;
		}

		int level = COMPATIBLE; // no autoboxing or varargs support needed
		int lastIndex = argLength;
		LookupEnvironment env = environment();
		if (method.isVarargs()) {
			lastIndex = paramLength - 1;
			if (paramLength == argLength) { // accept X or X[] but not X[][]
				TypeBinding param = parameters[lastIndex]; // is an ArrayBinding by definition
				TypeBinding arg = arguments[lastIndex];
				if (param != arg) {
					level = parameterCompatibilityLevel(arg, param, env);
					if (level == NOT_COMPATIBLE) {
						// expect X[], is it called with X
						param = ((ArrayBinding) param).elementsType();
						if (parameterCompatibilityLevel(arg, param, env) == NOT_COMPATIBLE)
							return NOT_COMPATIBLE;
						level = VARARGS_COMPATIBLE; // varargs support needed
					}
				}
			} else {
				if (paramLength < argLength) { // all remaining argument types must be compatible with the elementsType of varArgType
					TypeBinding param = ((ArrayBinding) parameters[lastIndex]).elementsType();
					for (int i = lastIndex; i < argLength; i++) {
						TypeBinding arg = arguments[i];
						if (param != arg && parameterCompatibilityLevel(arg, param, env) == NOT_COMPATIBLE)
							return NOT_COMPATIBLE;
					}
				}  else if (lastIndex != argLength) { // can call foo(int i, X ... x) with foo(1) but NOT foo();
					return NOT_COMPATIBLE;
				}
				level = VARARGS_COMPATIBLE; // varargs support needed
			}
		} else if (paramLength != argLength) {
			return NOT_COMPATIBLE;
		}
		// now compare standard arguments from 0 to lastIndex
		for (int i = 0; i < lastIndex; i++) {
			TypeBinding param = parameters[i];
			TypeBinding arg = arguments[i];
			if (arg != param) {
				int newLevel = parameterCompatibilityLevel(arg, param, env);
				if (newLevel == NOT_COMPATIBLE)
					return NOT_COMPATIBLE;
				if (newLevel > level)
					level = newLevel;
			}
		}
		return level;
	}

	private int parameterCompatibilityLevel(TypeBinding arg, TypeBinding param, LookupEnvironment env) {
		// only called if env.options.sourceLevel >= ClassFileConstants.JDK1_5
		if (arg.isCompatibleWith(param))
			return COMPATIBLE;
		if (arg.isBaseType() != param.isBaseType()) {
			TypeBinding convertedType = env.computeBoxingType(arg);
			if (convertedType == param || convertedType.isCompatibleWith(param))
				return AUTOBOX_COMPATIBLE;
		}
		return NOT_COMPATIBLE;
	}

	public abstract ProblemReporter problemReporter();

	public final CompilationUnitDeclaration referenceCompilationUnit() {
		Scope scope, unitScope = this;
		while ((scope = unitScope.parent) != null)
			unitScope = scope;
		return ((CompilationUnitScope) unitScope).referenceContext;
	}

	/**
	 * Returns the nearest reference context, starting from current scope.
	 * If starting on a class, it will return current class. If starting on unitScope, returns unit.
	 */
	public ReferenceContext referenceContext() {
		Scope current = this;
		do {
			switch(current.kind) {
				case METHOD_SCOPE :
					return ((MethodScope) current).referenceContext;
				case CLASS_SCOPE :
					return ((ClassScope) current).referenceContext;
				case COMPILATION_UNIT_SCOPE :
					return ((CompilationUnitScope) current).referenceContext;
			}
		} while ((current = current.parent) != null);
		return null;
	}

	// start position in this scope - for ordering scopes vs. variables
	int startIndex() {
		return 0;
	}
}
