/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;

public class MethodVerifier {
	SourceTypeBinding type;
	HashtableOfObject inheritedMethods;
	HashtableOfObject currentMethods;
	LookupEnvironment environment;
	private boolean allowCompatibleReturnTypes;
/*
Binding creation is responsible for reporting all problems with types:
	- all modifier problems (duplicates & multiple visibility modifiers + incompatible combinations - abstract/final)
		- plus invalid modifiers given the context (the verifier did not do this before)
	- qualified name collisions between a type and a package (types in default packages are excluded)
	- all type hierarchy problems:
		- cycles in the superclass or superinterface hierarchy
		- an ambiguous, invisible or missing superclass or superinterface
		- extending a final class
		- extending an interface instead of a class
		- implementing a class instead of an interface
		- implementing the same interface more than once (ie. duplicate interfaces)
	- with nested types:
		- shadowing an enclosing type's source name
		- defining a static class or interface inside a non-static nested class
		- defining an interface as a local type (local types can only be classes)
*/
MethodVerifier(LookupEnvironment environment) {
	this.type = null;  // Initialized with the public method verify(SourceTypeBinding)
	this.inheritedMethods = null;
	this.currentMethods = null;
	this.environment = environment;
	this.allowCompatibleReturnTypes =
		environment.globalOptions.complianceLevel >= ClassFileConstants.JDK1_5
			&& environment.globalOptions.sourceLevel < ClassFileConstants.JDK1_5;
}
boolean areMethodsCompatible(MethodBinding one, MethodBinding two) {
	return doesMethodOverride(one, two) && areReturnTypesCompatible(one, two);
}
boolean areParametersEqual(MethodBinding one, MethodBinding two) {
	TypeBinding[] oneArgs = one.parameters;
	TypeBinding[] twoArgs = two.parameters;
	if (oneArgs == twoArgs) return true;

	int length = oneArgs.length;
	if (length != twoArgs.length) return false;

	for (int i = 0; i < length; i++)
		if (!areTypesEqual(oneArgs[i], twoArgs[i])) return false;
	return true;
}
boolean areReturnTypesCompatible(MethodBinding one, MethodBinding two) {
	if (one.returnType == two.returnType) return true;

	if (areTypesEqual(one.returnType, two.returnType)) return true;

	// when sourceLevel < 1.5 but compliance >= 1.5, allow return types in binaries to be compatible instead of just equal
	if (this.allowCompatibleReturnTypes &&
			one.declaringClass instanceof BinaryTypeBinding &&
			two.declaringClass instanceof BinaryTypeBinding) {
		return areReturnTypesCompatible0(one, two);
	}
	return false;
}
boolean areReturnTypesCompatible0(MethodBinding one, MethodBinding two) {
	// short is compatible with int, but as far as covariance is concerned, its not
	if (one.returnType.isBaseType()) return false;

	
	if (one.declaringClass.id == TypeIds.T_JavaLangObject)
		return two.returnType.isCompatibleWith(one.returnType); // interface methods inherit from Object
	return one.returnType.isCompatibleWith(two.returnType);
}
boolean areTypesEqual(TypeBinding one, TypeBinding two) {
	if (one == two) return true;

	// its possible that an UnresolvedReferenceBinding can be compared to its resolved type
	// when they're both UnresolvedReferenceBindings then they must be identical like all other types
	// all wrappers of UnresolvedReferenceBindings are converted as soon as the type is resolved
	// so its not possible to have 2 arrays where one is UnresolvedX[] and the other is X[]
	if (one instanceof UnresolvedReferenceBinding)
		return ((UnresolvedReferenceBinding) one).resolvedType == two;
	if (two instanceof UnresolvedReferenceBinding)
		return ((UnresolvedReferenceBinding) two).resolvedType == one;
	if ( (one!=null && one.id==TypeIds.T_any) || (two!=null && two.id==TypeIds.T_any))
		return true;
	return false; // all other type bindings are identical
}
boolean canSkipInheritedMethods() {
	return true;
}
boolean canSkipInheritedMethods(MethodBinding one, MethodBinding two) {
	return two == null // already know one is not null
		|| one.declaringClass == two.declaringClass;
}
void checkAgainstInheritedMethods(MethodBinding currentMethod, MethodBinding[] methods, int length, MethodBinding[] allInheritedMethods) {
	CompilerOptions options = type.scope.compilerOptions();
	// need to find the overridden methods to avoid blaming this type for issues which are already reported against a supertype
	// but cannot ignore an overridden inherited method completely when it comes to checking for bridge methods
	int[] overriddenInheritedMethods = length > 1 ? findOverriddenInheritedMethods(methods, length) : null;
	nextMethod : for (int i = length; --i >= 0;) {
		MethodBinding inheritedMethod = methods[i];
		if (overriddenInheritedMethods == null || overriddenInheritedMethods[i] == 0) {
			if (currentMethod.isStatic() != inheritedMethod.isStatic() && currentMethod.declaringClass == type) {  // Cannot override a static method or hide an instance method
				problemReporter(currentMethod).staticAndInstanceConflict(currentMethod, inheritedMethod);
				continue nextMethod;
			}

			// want to tag currentMethod even if return types are not equal
			if (inheritedMethod.isAbstract()) {
				currentMethod.modifiers |= ExtraCompilerModifiers.AccImplementing | ExtraCompilerModifiers.AccOverriding;
				
//			with the above change an abstract method is tagged as implementing the inherited abstract method
//			if (!currentMethod.isAbstract() && inheritedMethod.isAbstract()) {
//				if ((currentMethod.modifiers & CompilerModifiers.AccOverriding) == 0)
//					currentMethod.modifiers |= CompilerModifiers.AccImplementing;
			} else {
				currentMethod.modifiers |= ExtraCompilerModifiers.AccOverriding;
			}
 
			if (!areReturnTypesCompatible(currentMethod, inheritedMethod))
			{
				if (!(currentMethod.returnType!=null && currentMethod.returnType.isObjectLiteralType()
					&& inheritedMethod.returnType!=null && inheritedMethod.returnType.isObjectLiteralType()))
				if (reportIncompatibleReturnTypeError(currentMethod, inheritedMethod))
					continue nextMethod;
				
			}

			if (!isAsVisible(currentMethod, inheritedMethod))
				problemReporter(currentMethod).visibilityConflict(currentMethod, inheritedMethod);
			if (options.reportDeprecationWhenOverridingDeprecatedMethod && inheritedMethod.isViewedAsDeprecated()) {
				if (!currentMethod.isViewedAsDeprecated() || options.reportDeprecationInsideDeprecatedCode) {	
					problemReporter(currentMethod).overridesDeprecatedMethod(currentMethod, inheritedMethod);
				}
			}
		}
		checkForBridgeMethod(currentMethod, inheritedMethod, allInheritedMethods);
	}
}
void checkForBridgeMethod(MethodBinding currentMethod, MethodBinding inheritedMethod, MethodBinding[] allInheritedMethods) {
	// no op before 1.5
}
void checkInheritedMethods(MethodBinding[] methods, int length) {
	int[] overriddenInheritedMethods = length > 1 ? findOverriddenInheritedMethods(methods, length) : null;
	if (overriddenInheritedMethods != null) {
		// detected some overridden methods that can be ignored when checking return types
		// but cannot ignore an overridden inherited method completely when it comes to checking for bridge methods
		int index = 0;
		MethodBinding[] closestMethods = new MethodBinding[length];
		for (int i = 0; i < length; i++)
			if (overriddenInheritedMethods[i] == 0)
				closestMethods[index++] = methods[i];
		if (!checkInheritedReturnTypes(closestMethods, index))
			return;
	} else if (!checkInheritedReturnTypes(methods, length)) {
		return;
	}

	MethodBinding concreteMethod = null;
	
	for (int i = length; --i >= 0;) {  // Remember that only one of the methods can be non-abstract
		if (!methods[i].isAbstract()) {
			concreteMethod = methods[i];
			break;
		}
	}
	
	if (concreteMethod == null) {
		
		return;
	}
}
boolean checkInheritedReturnTypes(MethodBinding[] methods, int length) {
	MethodBinding first = methods[0];
	int index = length;
	while (--index > 0 && areReturnTypesCompatible(first, methods[index])){/*empty*/}
	if (index == 0)
		return true;

	problemReporter().inheritedMethodsHaveIncompatibleReturnTypes(this.type, methods, length);
	return false;
}
/*
For each inherited method identifier (message pattern - vm signature minus the return type)
	if current method exists
		if current's vm signature does not match an inherited signature then complain
		else compare current's exceptions & visibility against each inherited method
	else
		if inherited methods = 1
			if inherited is abstract && type is NOT an interface or abstract, complain
		else
			if vm signatures do not match complain
			else
				find the concrete implementation amongst the abstract methods (can only be 1)
				if one exists then
					it must be a public instance method
					compare concrete's exceptions against each abstract method
				else
					complain about missing implementation only if type is NOT an interface or abstract
*/
void checkMethods() {
	char[][] methodSelectors = this.inheritedMethods.keyTable;
	nextSelector : for (int s = methodSelectors.length; --s >= 0;) {
		if (methodSelectors[s] == null) continue nextSelector;

		MethodBinding[] current = (MethodBinding[]) this.currentMethods.get(methodSelectors[s]);

		MethodBinding[] inherited = (MethodBinding[]) this.inheritedMethods.valueTable[s];
		if (inherited.length == 1 && current == null) { // handle the common case
			continue nextSelector;
		}

		int index = -1;
		MethodBinding[] matchingInherited = new MethodBinding[inherited.length];
		if (current != null) {
			for (int i = 0, length1 = current.length; i < length1; i++) {
				MethodBinding currentMethod = current[i];
				for (int j = 0, length2 = inherited.length; j < length2; j++) {
					MethodBinding inheritedMethod = computeSubstituteMethod(inherited[j], currentMethod);
					if (inheritedMethod != null) {
						if (doesMethodOverride(currentMethod, inheritedMethod)) {
							matchingInherited[++index] = inheritedMethod;
							inherited[j] = null; // do not want to find it again
						}
					}
				}
				if (index >= 0) {
					checkAgainstInheritedMethods(currentMethod, matchingInherited, index + 1, inherited); // pass in the length of matching
					while (index >= 0) matchingInherited[index--] = null; // clear the contents of the matching methods
				}
			}
		}

		for (int i = 0, length = inherited.length; i < length; i++) {
			MethodBinding inheritedMethod = inherited[i];
			if (inheritedMethod == null) continue;

			matchingInherited[++index] = inheritedMethod;
			for (int j = i + 1; j < length; j++) {
				MethodBinding otherInheritedMethod = inherited[j];
				if (canSkipInheritedMethods(inheritedMethod, otherInheritedMethod))
					continue;
				otherInheritedMethod = computeSubstituteMethod(otherInheritedMethod, inheritedMethod);
				if (otherInheritedMethod != null) {
					if (doesMethodOverride(inheritedMethod, otherInheritedMethod)) {
						matchingInherited[++index] = otherInheritedMethod;
						inherited[j] = null; // do not want to find it again
					}
				}
			}
			if (index == -1) continue;
			if (index > 0)
				checkInheritedMethods(matchingInherited, index + 1); // pass in the length of matching
			while (index >= 0) matchingInherited[index--] = null; // clear the contents of the matching methods
		}
	}
}
void checkPackagePrivateAbstractMethod(MethodBinding abstractMethod) {
	// check that the inherited abstract method (package private visibility) is implemented within the same package
	PackageBinding necessaryPackage = abstractMethod.declaringClass.fPackage;
	if (necessaryPackage == this.type.fPackage) return; // not a problem

	ReferenceBinding superType = this.type.getSuperBinding();
	do {
		if (!superType.isValidBinding()) return;
		return; // closer non abstract super type will be flagged instead

	} while ((superType = superType.getSuperBinding()) != abstractMethod.declaringClass);
}
void computeInheritedMethods() {
	ReferenceBinding superclass = this.type.getSuperBinding(); // class or enum
	computeInheritedMethods(superclass, null);
}
/*
Binding creation is responsible for reporting:
	- all modifier problems (duplicates & multiple visibility modifiers + incompatible combinations)
		- plus invalid modifiers given the context... examples:
			- interface methods can only be public
			- abstract methods can only be defined by abstract classes
	- collisions... 2 methods with identical vmSelectors
	- multiple methods with the same message pattern but different return types
	- ambiguous, invisible or missing return/argument/exception types
	- check the type of any array is not void
	- check that each exception type is Throwable or a subclass of it
*/
void computeInheritedMethods(ReferenceBinding superclass, ReferenceBinding[] superInterfaces) {
	// only want to remember inheritedMethods that can have an impact on the current type
	// if an inheritedMethod has been 'replaced' by a supertype's method then skip it

	this.inheritedMethods = new HashtableOfObject(51); // maps method selectors to an array of methods... must search to match paramaters & return type
	ReferenceBinding[] interfacesToVisit = null;
	int nextPosition = 0;
	ReferenceBinding[] itsInterfaces = superInterfaces;
	if (itsInterfaces != null) {
		nextPosition = itsInterfaces.length;
		interfacesToVisit = itsInterfaces;
	}

	ReferenceBinding superType = superclass;
	HashtableOfObject nonVisibleDefaultMethods = new HashtableOfObject(3); // maps method selectors to an array of methods

	while (superType != null && superType.isValidBinding()) {

		MethodBinding[] methods = superType.unResolvedMethods();
		nextMethod : for (int m = methods.length; --m >= 0;) {
			MethodBinding inheritedMethod = methods[m];
			if (inheritedMethod.isPrivate() || inheritedMethod.isConstructor() || inheritedMethod.isDefaultAbstract())
				continue nextMethod;
			MethodBinding[] existingMethods = (MethodBinding[]) this.inheritedMethods.get(inheritedMethod.selector);
			if (existingMethods != null) {
				for (int i = 0, length = existingMethods.length; i < length; i++) {
					if (existingMethods[i].declaringClass != inheritedMethod.declaringClass && areMethodsCompatible(existingMethods[i], inheritedMethod)) {
						if (inheritedMethod.isDefault() && inheritedMethod.isAbstract())
							checkPackagePrivateAbstractMethod(inheritedMethod);
						continue nextMethod;
					}
				}
			}
			MethodBinding[] nonVisible = (MethodBinding[]) nonVisibleDefaultMethods.get(inheritedMethod.selector);
			if (nonVisible != null)
				for (int i = 0, l = nonVisible.length; i < l; i++)
					if (areMethodsCompatible(nonVisible[i], inheritedMethod))
						continue nextMethod;

			if (!inheritedMethod.isDefault() || inheritedMethod.declaringClass.fPackage == type.fPackage) {
				if (existingMethods == null) {
					existingMethods = new MethodBinding[] {inheritedMethod};
				} else {
					int length = existingMethods.length;
					System.arraycopy(existingMethods, 0, existingMethods = new MethodBinding[length + 1], 0, length);
					existingMethods[length] = inheritedMethod;
				}
				this.inheritedMethods.put(inheritedMethod.selector, existingMethods);
			} else {
				if (nonVisible == null) {
					nonVisible = new MethodBinding[] {inheritedMethod};
				} else {
					int length = nonVisible.length;
					System.arraycopy(nonVisible, 0, nonVisible = new MethodBinding[length + 1], 0, length);
					nonVisible[length] = inheritedMethod;
				}
				nonVisibleDefaultMethods.put(inheritedMethod.selector, nonVisible);

				MethodBinding[] current = (MethodBinding[]) this.currentMethods.get(inheritedMethod.selector);
				if (current != null) { // non visible methods cannot be overridden so a warning is issued
					foundMatch : for (int i = 0, length = current.length; i < length; i++) {
						if (areMethodsCompatible(current[i], inheritedMethod)) {
							break foundMatch;
						}
					}
				}
			}
		}
		superType = superType.getSuperBinding();
	}
	if (nextPosition == 0) return;

	for (int i = 0; i < nextPosition; i++) {
		superType = interfacesToVisit[i];
		if (superType.isValidBinding()) {
			MethodBinding[] methods = superType.unResolvedMethods();
			for (int m = methods.length; --m >= 0;) { // Interface methods are all abstract public
				MethodBinding inheritedMethod = methods[m];
				MethodBinding[] existingMethods = (MethodBinding[]) this.inheritedMethods.get(inheritedMethod.selector);
				if (existingMethods == null) {
					existingMethods = new MethodBinding[] {inheritedMethod};
				} else {
					int length = existingMethods.length;
						
					System.arraycopy(existingMethods, 0, existingMethods = new MethodBinding[length + 1], 0, length);
					existingMethods[length] = inheritedMethod;
				}
				this.inheritedMethods.put(inheritedMethod.selector, existingMethods);
			}
		}
	}
}
void computeMethods() {
	MethodBinding[] methods = type.methods();
	int size = methods.length;
	this.currentMethods = new HashtableOfObject(size == 0 ? 1 : size); // maps method selectors to an array of methods... must search to match paramaters & return type
	for (int m = size; --m >= 0;) {
		MethodBinding method = methods[m];
		if (!(method.isConstructor() || method.isDefaultAbstract())) { // keep all methods which are NOT constructors or default abstract
			MethodBinding[] existingMethods = (MethodBinding[]) this.currentMethods.get(method.selector);
			if (existingMethods == null)
				existingMethods = new MethodBinding[1];
			else
				System.arraycopy(existingMethods, 0,
					(existingMethods = new MethodBinding[existingMethods.length + 1]), 0, existingMethods.length - 1);
			existingMethods[existingMethods.length - 1] = method;
			this.currentMethods.put(method.selector, existingMethods);
		}
	}
}
MethodBinding computeSubstituteMethod(MethodBinding inheritedMethod, MethodBinding currentMethod) {
	if (inheritedMethod == null) return null;
	if (currentMethod.parameters.length != inheritedMethod.parameters.length) return null; // no match
	return inheritedMethod;
}
public boolean doesMethodOverride(MethodBinding method, MethodBinding inheritedMethod) {
	return areParametersEqual(method, inheritedMethod);
}
int[] findOverriddenInheritedMethods(MethodBinding[] methods, int length) {
	// NOTE assumes length > 1
	// inherited methods are added as we walk up the superclass hierarchy, then each superinterface
	// so method[1] from a class can NOT override method[0], but methods from superinterfaces can
	// since superinterfaces can be added from different superclasses or other superinterfaces
	int[] toSkip = null;
	int i = 0;
	ReferenceBinding declaringClass = methods[i].declaringClass;
	
	// in the first pass, skip overridden methods from superclasses
	// only keep methods from the closest superclass, all others from higher superclasses can be skipped
	// NOTE: methods were added in order by walking up the superclass hierarchy
	ReferenceBinding declaringClass2 = methods[++i].declaringClass;
	while (declaringClass == declaringClass2) {
		if (++i == length) return null;
		declaringClass2 = methods[i].declaringClass;
	}
	
	// skip all methods from different superclasses
	toSkip = new int[length];
	do {
		toSkip[i] = -1;
		if (++i == length) return toSkip;
		declaringClass2 = methods[i].declaringClass;
	} while (true);
}
boolean isAsVisible(MethodBinding newMethod, MethodBinding inheritedMethod) {
	if (inheritedMethod.modifiers == newMethod.modifiers) return true;

	if (newMethod.isPublic()) return true;		// Covers everything
	if (inheritedMethod.isPublic()) return false;

	if (newMethod.isProtected()) return true;
	if (inheritedMethod.isProtected()) return false;

	return !newMethod.isPrivate();		// The inheritedMethod cannot be private since it would not be visible
}
boolean isSameClassOrSubclassOf(ReferenceBinding testClass, ReferenceBinding superclass) {
	do {
		if (testClass == superclass) return true;
	} while ((testClass = testClass.getSuperBinding()) != null);
	return false;
}
ProblemReporter problemReporter() {
	return this.type.scope.problemReporter();
}
ProblemReporter problemReporter(MethodBinding currentMethod) {
	ProblemReporter reporter = problemReporter();
	if (currentMethod.declaringClass == type && currentMethod.sourceMethod() != null)	// only report against the currentMethod if its implemented by the type
		reporter.referenceContext = currentMethod.sourceMethod();
	return reporter;
}
/**
 * Return true and report an incompatibleReturnType error if currentMethod's
 * return type is strictly incompatible with inheritedMethod's, else return
 * false and report an unchecked conversion warning. Do not call when
 * areReturnTypesCompatible(currentMethod, inheritedMethod) returns true.
 * @param currentMethod the (potentially) inheriting method
 * @param inheritedMethod the inherited method
 * @return true if currentMethod's return type is strictly incompatible with
 *         inheritedMethod's
 */
boolean reportIncompatibleReturnTypeError(MethodBinding currentMethod, MethodBinding inheritedMethod) {
	problemReporter(currentMethod).incompatibleReturnType(currentMethod, inheritedMethod);
	return true;
}
void verify(SourceTypeBinding someType) {
	this.type = someType;
	computeMethods();
	computeInheritedMethods();
	checkMethods();
}
public String toString() {
	StringBuffer buffer = new StringBuffer(10);
	buffer.append("MethodVerifier for type: "); //$NON-NLS-1$
	buffer.append(type.readableName());
	buffer.append('\n');
	buffer.append("\t-inherited methods: "); //$NON-NLS-1$
	buffer.append(this.inheritedMethods);
	return buffer.toString();
}
}
