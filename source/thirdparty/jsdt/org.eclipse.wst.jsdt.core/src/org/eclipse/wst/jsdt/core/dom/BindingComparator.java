/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import java.util.HashSet;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ImportBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding;

/**
 * Internal helper class for comparing bindings.
 *
 */
class BindingComparator {

	/**
	 * @param declaringElement
	 * @param declaringElement2
	 * @return true if both parameters are equals, false otherwise
	 */
	static boolean isEqual(Binding declaringElement, Binding declaringElement2, HashSet visitedTypes) {
		if (declaringElement instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
			if (!(declaringElement2 instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)){
				return false;
			}
			return isEqual((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) declaringElement,
					(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) declaringElement2,
					visitedTypes);
		} else if (declaringElement instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding) {
			if (!(declaringElement2 instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding)) {
				return false;
			}
			return isEqual((org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding) declaringElement,
					(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding) declaringElement2,
					visitedTypes);
		} else if (declaringElement instanceof VariableBinding) {
			if (!(declaringElement2 instanceof VariableBinding)) {
				return false;
			}
			return isEqual((VariableBinding) declaringElement,
					(VariableBinding) declaringElement2);
		} else if (declaringElement instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) {
			if (!(declaringElement2 instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding)) {
				return false;
			}
			org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding packageBinding = (org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) declaringElement;
			org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding packageBinding2 = (org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) declaringElement2;
			return CharOperation.equals(packageBinding.compoundName, packageBinding2.compoundName);
		} else if (declaringElement instanceof ImportBinding) {
			if (!(declaringElement2 instanceof ImportBinding)) {
				return false;
			}
			ImportBinding importBinding = (ImportBinding) declaringElement;
			ImportBinding importBinding2 = (ImportBinding) declaringElement2;
			return importBinding.onDemand == importBinding2.onDemand
				&& CharOperation.equals(importBinding.compoundName, importBinding2.compoundName);
		}
		return false;
	}

	static boolean isEqual(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding,
			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding2) {
		return isEqual(methodBinding, methodBinding2, new HashSet());
	}

	static boolean isEqual(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding,
			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding2,
			HashSet visitedTypes) {
		if (methodBinding == null) {
			return methodBinding2 == null;
		}
		if (methodBinding2 == null) return false;
		if ( CharOperation.equals(methodBinding.selector, methodBinding2.selector)
				&& isEqual(methodBinding.returnType, methodBinding2.returnType, visitedTypes)
//				&& isEqual(methodBinding.thrownExceptions, methodBinding2.thrownExceptions, visitedTypes)
				&& isEqual(methodBinding.declaringClass, methodBinding2.declaringClass, visitedTypes)
//				&& isEqual(methodBinding.typeVariables, methodBinding2.typeVariables, visitedTypes)
				&& isEqual(methodBinding.parameters, methodBinding2.parameters, visitedTypes))
			 return true;
		org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding constructorBinding =null;
		org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methBinding =null;
		 if (methodBinding.selector==TypeConstants.INIT)
		 {
			 constructorBinding=methodBinding;
			 methBinding=methodBinding2;
		 }
		 else if (methodBinding2.selector==TypeConstants.INIT)
		 {
			 constructorBinding=methodBinding2;
			 methBinding=methodBinding;

		 }
		 return (constructorBinding!=null &&
				 CharOperation.equals(methBinding.selector,constructorBinding.declaringClass.sourceName)
					&& isEqual(methBinding.parameters, constructorBinding.parameters, visitedTypes));

	}

	static boolean isEqual(VariableBinding variableBinding, VariableBinding variableBinding2) {
		return (variableBinding.modifiers & ExtraCompilerModifiers.AccJustFlag) == (variableBinding2.modifiers & ExtraCompilerModifiers.AccJustFlag)
				&& CharOperation.equals(variableBinding.name, variableBinding2.name)
				&& isEqual(variableBinding.type, variableBinding2.type)
				&& (variableBinding.id == variableBinding2.id);
	}

	static boolean isEqual(FieldBinding fieldBinding, FieldBinding fieldBinding2) {
		HashSet visitedTypes = new HashSet();
		return (fieldBinding.modifiers & ExtraCompilerModifiers.AccJustFlag) == (fieldBinding2.modifiers & ExtraCompilerModifiers.AccJustFlag)
				&& CharOperation.equals(fieldBinding.name, fieldBinding2.name)
				&& isEqual(fieldBinding.type, fieldBinding2.type, visitedTypes)
				&& isEqual(fieldBinding.declaringClass, fieldBinding2.declaringClass, visitedTypes);
	}

	/**
	 * @param bindings
	 * @param otherBindings
	 * @return true if both parameters are equals, false otherwise
	 */
	static boolean isEqual(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[] bindings, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[] otherBindings) {
		return isEqual(bindings, otherBindings, new HashSet());
	}
	/**
	 * @param bindings
	 * @param otherBindings
	 * @return true if both parameters are equals, false otherwise
	 */
	static boolean isEqual(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[] bindings, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[] otherBindings, HashSet visitedTypes) {
		if (bindings == null) {
			return otherBindings == null;
		}
		if (otherBindings == null) {
			return false;
		}
		int length = bindings.length;
		int otherLength = otherBindings.length;
		if (length != otherLength) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			if (!isEqual(bindings[i], otherBindings[i], visitedTypes)) {
				return false;
			}
		}
		return true;
	}
	static boolean isEqual(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding typeBinding, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding typeBinding2, HashSet visitedTypes) {
		if (typeBinding == typeBinding2)
			return true;
		if (typeBinding == null || typeBinding2 == null)
			return false;

		switch (typeBinding.kind()) {
			case Binding.BASE_TYPE :
				if (!typeBinding2.isBaseType()) {
					return false;
				}
				return typeBinding.id == typeBinding2.id;

			case Binding.ARRAY_TYPE :
				if (!typeBinding2.isArrayType()) {
					return false;
				}
				return typeBinding.dimensions() == typeBinding2.dimensions()
						&& isEqual(typeBinding.leafComponentType(), typeBinding2.leafComponentType(), visitedTypes);

			default :
				if (!(typeBinding2 instanceof ReferenceBinding)) {
					return false;
				}
				ReferenceBinding referenceBinding = (ReferenceBinding) typeBinding;
				ReferenceBinding referenceBinding2 = (ReferenceBinding) typeBinding2;
				char[] constantPoolName = referenceBinding.constantPoolName();
				char[] constantPoolName2 = referenceBinding2.constantPoolName();
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=116833
				if (constantPoolName == null) {
					if (constantPoolName2 != null) {
						return false;
					}
					if (!CharOperation.equals(referenceBinding.computeUniqueKey(), referenceBinding2.computeUniqueKey())) {
						return false;
					}
				} else {
					if (constantPoolName2 == null) {
						return false;
					}
					if (!CharOperation.equals(constantPoolName, constantPoolName2)) {
						return false;
					}
				}
				return CharOperation.equals(referenceBinding.compoundName, referenceBinding2.compoundName)
					&& ((referenceBinding.modifiers & ~ClassFileConstants.AccSuper) & (ExtraCompilerModifiers.AccJustFlag))
							== ((referenceBinding2.modifiers & ~ClassFileConstants.AccSuper) & (ExtraCompilerModifiers.AccJustFlag))
					&& isEqual(referenceBinding.enclosingType(), referenceBinding2.enclosingType(), visitedTypes);
		}
	}
	/**
	 * @param typeBinding
	 * @param typeBinding2
	 * @return true if both parameters are equals, false otherwise
	 */
	static boolean isEqual(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding typeBinding, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding typeBinding2) {
		return isEqual(typeBinding, typeBinding2, new HashSet());
	}
}
