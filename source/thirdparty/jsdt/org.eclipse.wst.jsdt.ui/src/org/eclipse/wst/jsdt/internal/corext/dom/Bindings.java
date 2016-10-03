/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug "inline method - doesn't handle implicit cast" (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.IPackageBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class Bindings {
	
	public static final String ARRAY_LENGTH_FIELD_BINDING_STRING= "(array type):length";//$NON-NLS-1$
	private Bindings() {
		// No instance
	}

	/**
	 * Checks if the two bindings are equals. First an identity check is
	 * made an then the key of the bindings are compared. 
	 * @param b1 first binding treated as <code>this</code>. So it must
	 *  not be <code>null</code>
	 * @param b2 the second binding.
	 * @return boolean
	 */
	public static boolean equals(IBinding b1, IBinding b2) {
		return b1.isEqualTo(b2);
	}

	/**
	 * Checks if the two arrays of bindings have the same length and
	 * their elements are equal. Uses
	 * <code>Bindings.equals(IBinding, IBinding)</code> to compare.
	 * @param b1 the first array of bindings. Must not be <code>null</code>.
	 * @param b2 the second array of bindings.
	 * @return boolean
	 */
	public static boolean equals(IBinding[] b1, IBinding[] b2) {
		Assert.isNotNull(b1);
		if (b1 == b2)
			return true;
		if (b2 == null)
			return false;		
		if (b1.length != b2.length)
			return false;
		for (int i= 0; i < b1.length; i++) {
			if (! Bindings.equals(b1[i], b2[i]))
				return false;
		}
		return true;
	}
	
	public static int hashCode(IBinding binding){
		Assert.isNotNull(binding);
		String key= binding.getKey();
		if (key == null)
			return binding.hashCode();
		return key.hashCode();
	}
	
	/**
	 * Note: this method is for debugging and testing purposes only.
	 * There are tests whose pre-computed test results rely on the returned String's format.
	 * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider
	 */
	public static String asString(IBinding binding) {
		if (binding instanceof IFunctionBinding)
			return asString((IFunctionBinding)binding);
		else if (binding instanceof ITypeBinding)
			return ((ITypeBinding)binding).getQualifiedName();
		else if (binding instanceof IVariableBinding)
			return asString((IVariableBinding)binding);
		return binding.toString();
	}

	private static String asString(IVariableBinding variableBinding) {
		if (! variableBinding.isField())
			return variableBinding.toString();
		if (variableBinding.getDeclaringClass() == null) {
			Assert.isTrue(variableBinding.getName().equals("length"));//$NON-NLS-1$
			return ARRAY_LENGTH_FIELD_BINDING_STRING;
		}
		StringBuffer result= new StringBuffer();
		result.append(variableBinding.getDeclaringClass().getName());
		result.append(':');
		result.append(variableBinding.getName());				
		return result.toString();		
	}

	private static String asString(IFunctionBinding method) {
		StringBuffer result= new StringBuffer();
		result.append(method.getDeclaringClass().getName());
		result.append(':');
		result.append(method.getName());
		result.append('(');
		ITypeBinding[] parameters= method.getParameterTypes();
		int lastComma= parameters.length - 1;
		for (int i= 0; i < parameters.length; i++) {
			ITypeBinding parameter= parameters[i];
			result.append(parameter.getName());
			if (i < lastComma)
				result.append(", "); //$NON-NLS-1$
		}
		result.append(')');
		return result.toString();
	}
	
	public static String getTypeQualifiedName(ITypeBinding type) {
		List result= new ArrayList(5);
		createName(type, false, result);
		
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < result.size(); i++) {
			if (i > 0) {
				buffer.append('.');
			}
			buffer.append(((String) result.get(i)));
		}
		return buffer.toString();
	}

	/**
	 * Returns the fully qualified name of the specified type binding.
	 * <p>
	 * If the binding resolves to a generic type, the fully qualified name of the raw type is returned.
	 * 
	 * @param type the type binding to get its fully qualified name
	 * @return the fully qualified name
	 */
	public static String getFullyQualifiedName(ITypeBinding type) {
		String name= type.getQualifiedName();
		final int index= name.indexOf('<');
		if (index > 0)
			name= name.substring(0, index);
		return name;
	}

	public static String getImportName(IBinding binding) {
		ITypeBinding declaring= null;
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return getRawQualifiedName((ITypeBinding) binding);
			case IBinding.PACKAGE:
				return binding.getName() + ".*"; //$NON-NLS-1$
			case IBinding.METHOD:
				declaring= ((IFunctionBinding) binding).getDeclaringClass();
				break;
			case IBinding.VARIABLE:
				declaring= ((IVariableBinding) binding).getDeclaringClass();
				if (declaring == null) {
					return binding.getName(); // array.length
				}
				
				break;
			default:
				return binding.getName();
		}
		return JavaModelUtil.concatenateName(getRawQualifiedName(declaring), binding.getName());
	}	
	
	
	private static void createName(ITypeBinding type, boolean includePackage, List list) {
		ITypeBinding baseType= type;
		if (type.isArray()) {
			baseType= type.getElementType();
		}
		if (!baseType.isPrimitive() && !baseType.isNullType()) {
			ITypeBinding declaringType= baseType.getDeclaringClass();
			if (declaringType != null) {
				createName(declaringType, includePackage, list);
			} else if (includePackage && !baseType.getPackage().isUnnamed()) {
				String[] components= baseType.getPackage().getNameComponents();
				for (int i= 0; i < components.length; i++) {
					list.add(components[i]);
				}
			}
		}
		if (!baseType.isAnonymous()) {
			list.add(type.getName());
		} else {
			list.add("$local$"); //$NON-NLS-1$
		}		
	}	
	
	
	public static String[] getNameComponents(ITypeBinding type) {
		List result= new ArrayList(5);
		createName(type, false, result);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public static String[] getAllNameComponents(ITypeBinding type) {
		List result= new ArrayList(5);
		createName(type, true, result);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public static ITypeBinding getTopLevelType(ITypeBinding type) {
		ITypeBinding parent= type.getDeclaringClass();
		while (parent != null) {
			type= parent;
			parent= type.getDeclaringClass();
		}
		return type;
	}
	
	/**
	 * Checks whether the passed type binding is a runtime exception.
	 * 
	 * @param thrownException the type binding
	 * 
	 * @return <code>true</code> if the passed type binding is a runtime exception;
	 * 	otherwise <code>false</code> is returned
	 */
	public static boolean isRuntimeException(ITypeBinding thrownException) {
		if (thrownException == null || thrownException.isPrimitive() || thrownException.isArray())
			return false;
		return findTypeInHierarchy(thrownException, "java.lang.RuntimeException") != null; //$NON-NLS-1$
	}
	
	/**
	 * Finds the field specified by <code>fieldName<code> in
	 * the given <code>type</code>. Returns <code>null</code> if no such field exits.
	 * @param type the type to search the field in
	 * @param fieldName the field name
	 * @return the binding representing the field or <code>null</code>
	 */
	public static IVariableBinding findFieldInType(ITypeBinding type, String fieldName) {
		if (type.isPrimitive())
			return null;
		IVariableBinding[] fields= type.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			IVariableBinding field= fields[i];
			if (field.getName().equals(fieldName))
				return field;
		}
		return null;
	}
	
	/**
	 * Finds the field specified by <code>fieldName</code> in
	 * the type hierarchy denoted by the given type. Returns <code>null</code> if no such field
	 * exists. If the field is defined in more than one super type only the first match is 
	 * returned. First the super class is examined and than the implemented interfaces.
	 * @param type The type to search the field in
	 * @param fieldName The name of the field to find
	 * @return the variable binding representing the field
	 */
	public static IVariableBinding findFieldInHierarchy(ITypeBinding type, String fieldName) {
		IVariableBinding field= findFieldInType(type, fieldName);
		if (field != null)
			return field;
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null) {
			field= findFieldInHierarchy(superClass, fieldName);
			if (field != null)
				return field;			
		}
		return null;
	}
		
	/**
	 * Finds the method specified by <code>methodName<code> and </code>parameters</code> in
	 * the given <code>type</code>. Returns <code>null</code> if no such method exits.
	 * @param type The type to search the method in
	 * @param methodName The name of the method to find
	 * @param parameters The parameter types of the method to find. If <code>null</code> is passed, only 
	 *  the name is matched and parameters are ignored.
	 * @return the method binding representing the method
	 */
	public static IFunctionBinding findMethodInType(ITypeBinding type, String methodName, ITypeBinding[] parameters) {
		if (type.isPrimitive())
			return null;
		IFunctionBinding[] methods= type.getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			if (parameters == null) {
				if (methodName.equals(methods[i].getName()))
					return methods[i];
			} else {
				if (isEqualMethod(methods[i], methodName, parameters))
					return methods[i];
			}
		}
		return null;
	}
	
	/**
	 * Finds the method specified by <code>methodName</code> and </code>parameters</code> in
	 * the type hierarchy denoted by the given type. Returns <code>null</code> if no such method
	 * exists. If the method is defined in more than one super type only the first match is 
	 * returned. First the super class is examined and than the implemented interfaces.
	 * @param type The type to search the method in
	 * @param methodName The name of the method to find
	 * @param parameters The parameter types of the method to find. If <code>null</code> is passed, only the name is matched and parameters are ignored.
	 * @return the method binding representing the method
	 */
	public static IFunctionBinding findMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding[] parameters) {
		if (type==null)
			return null;
		IFunctionBinding method= findMethodInType(type, methodName, parameters);
		if (method != null)
			return method;
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null) {
			method= findMethodInHierarchy(superClass, methodName, parameters);
			if (method != null)
				return method;			
		}
		return null;
	}
	
	/**
	 * Finds the method specified by <code>methodName<code> and </code>parameters</code> in
	 * the given <code>type</code>. Returns <code>null</code> if no such method exits.
	 * @param type The type to search the method in
	 * @param methodName The name of the method to find
	 * @param parameters The parameter types of the method to find. If <code>null</code> is passed, only the name is matched and parameters are ignored.
	 * @return the method binding representing the method
	 */
	public static IFunctionBinding findMethodInType(ITypeBinding type, String methodName, String[] parameters) {
		if (type.isPrimitive())
			return null;
		IFunctionBinding[] methods= type.getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			if (parameters == null) {
				if (methodName.equals(methods[i].getName()))
					return methods[i];
			} else {
				if (isEqualMethod(methods[i], methodName, parameters))
					return methods[i];
			}
		}
		return null;
	}
	
	/**
	 * Finds the method specified by <code>methodName</code> and </code>parameters</code> in
	 * the type hierarchy denoted by the given type. Returns <code>null</code> if no such method
	 * exists. If the method is defined in more than one super type only the first match is 
	 * returned. First the super class is examined and than the implemented interfaces.
	 * @param type the type to search the method in
	 * @param methodName The name of the method to find
	 * @param parameters The parameter types of the method to find. If <code>null</code> is passed, only the name is matched and parameters are ignored.
	 * @return the method binding representing the method
	 */
	public static IFunctionBinding findMethodInHierarchy(ITypeBinding type, String methodName, String[] parameters) {
		IFunctionBinding method= findMethodInType(type, methodName, parameters);
		if (method != null)
			return method;
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null) {
			method= findMethodInHierarchy(superClass, methodName, parameters);
			if (method != null)
				return method;			
		}
		return null;
	}
	
	/**
	 * Finds the method in the given <code>type</code> that is overridden by the specified <code>method<code>.
	 * Returns <code>null</code> if no such method exits.
	 * @param type The type to search the method in
	 * @param method The specified method that would override the result
	 * @return the method binding of the method that is overridden by the specified <code>method<code>, or <code>null</code>
	 */
	public static IFunctionBinding findOverriddenMethodInType(ITypeBinding type, IFunctionBinding method) {
		IFunctionBinding[] methods= type.getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			if (isSubsignature(method, methods[i]))
				return methods[i];
		}
		return null;
	}
	
	/**
	 * Finds a method in the hierarchy of <code>type</code> that is overridden by </code>binding</code>.
	 * Returns <code>null</code> if no such method exists. If the method is defined in more than one super type only the first match is 
	 * returned. First the super class is examined and than the implemented interfaces.
	 * @param type The type to search the method in
	 * @param binding The method that overrides
	 * @return the method binding overridden the method
	 */
	public static IFunctionBinding findOverriddenMethodInHierarchy(ITypeBinding type, IFunctionBinding binding) {
		IFunctionBinding method= findOverriddenMethodInType(type, binding);
		if (method != null)
			return method;
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null) {
			method= findOverriddenMethodInHierarchy(superClass, binding);
			if (method != null)
				return method;			
		}
		return null;
	}
	
	
	/**
	 * Finds the method that is overridden by the given method. The search is bottom-up, so this
	 * returns the nearest defining/declaring method.
	 * @param overriding overriding method
	 * @param testVisibility If true the result is tested on visibility. Null is returned if the method is not visible.
	 * @return the method binding representing the method
	 */
	public static IFunctionBinding findOverriddenMethod(IFunctionBinding overriding, boolean testVisibility) {
		int modifiers= overriding.getModifiers();
		if (Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers) || overriding.isConstructor()) {
			return null;
		}
		
		ITypeBinding type= overriding.getDeclaringClass();
		ITypeBinding superType = type != null ? type.getSuperclass() : null;
		if (superType != null) {
			IFunctionBinding res= findOverriddenMethodInHierarchy(superType, overriding);
			if (res != null && !Modifier.isPrivate(res.getModifiers())) {
				if (!testVisibility || isVisibleInHierarchy(res, overriding.getDeclaringClass().getPackage())) {
					return res;
				}
			}
		}
		return null;
	}
	
	
	public static boolean isVisibleInHierarchy(IFunctionBinding member, IPackageBinding pack) {
		int otherflags= member.getModifiers();
		if (Modifier.isPublic(otherflags) || Modifier.isProtected(otherflags)) {
			return true;
		} else if (Modifier.isPrivate(otherflags)) {
			return false;
		}		

		ITypeBinding declaringType= member.getDeclaringClass();
		return declaringType != null && pack == declaringType.getPackage();
	}
		
	/**
	 * Returns all super types (classes and interfaces) for the given type.
	 * @param type The type to get the supertypes of.
	 * @return all super types (excluding <code>type</code>)
	 */
	public static ITypeBinding[] getAllSuperTypes(ITypeBinding type) {
		Set result= new HashSet();
		collectSuperTypes(type, result);
		result.remove(type);
		return (ITypeBinding[]) result.toArray(new ITypeBinding[result.size()]);
	}
	
	private static void collectSuperTypes(ITypeBinding curr, Set collection) {
		if (collection.add(curr)) {
			ITypeBinding superClass= curr.getSuperclass();
			if (superClass != null) {
				collectSuperTypes(superClass, collection);
			}
		}
	}

	/**
	 * Method to visit a type hierarchy defined by a given type.
	 * 
	 * @param type the type which hierarchy is to be visited
	 * @param visitor the visitor
	 * @return <code>false</code> if the visiting got interrupted
	 */
	public static boolean visitHierarchy(ITypeBinding type, TypeBindingVisitor visitor) {
		boolean result= visitSuperclasses(type, visitor);
		return result;
	}

	/**
	 * Method to visit a super class hierarchy defined by a given type.
	 * 
	 * @param type the type which super class hierarchy is to be visited
	 * @param visitor the visitor
	 * @return <code>false</code> if the visiting got interrupted
	 */
	public static boolean visitSuperclasses(ITypeBinding type, TypeBindingVisitor visitor) {
		while ((type= type.getSuperclass()) != null) {
			if (!visitor.visit(type)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests whether the two methods are erasure-equivalent.
	 * @deprecated use {@link #isSubsignature(IFunctionBinding, IFunctionBinding)}
	 */
	//TODO: rename to isErasureEquivalentMethod and change to two IFunctionBinding parameters
	public static boolean isEqualMethod(IFunctionBinding method, String methodName, ITypeBinding[] parameters) {
		if (!method.getName().equals(methodName))
			return false;
			
		ITypeBinding[] methodParameters= method.getParameterTypes();
		if (methodParameters.length != parameters.length)
			return false;
		for (int i= 0; i < parameters.length; i++) {
			if (!equals(methodParameters[i].getErasure(), parameters[i].getErasure()))
				return false;
		}
		//Can't use this fix, since some clients assume that this method tests erasure equivalence:
//		if (method.getTypeParameters().length == 0) {
//			//a method without type parameters cannot be overridden by one that declares type parameters -> can be exact here
//			for (int i= 0; i < parameters.length; i++) {
//				if ( ! (equals(methodParameters[i], parameters[i])
//						|| equals(methodParameters[i].getErasure(), parameters[i]))) // subsignature
//					return false;
//			}
//		} else {
//			//this will find all overridden methods, but may generate false positives in some cases:
//			for (int i= 0; i < parameters.length; i++) {
//				if (!equals(methodParameters[i].getErasure(), parameters[i].getErasure()))
//					return false;
//			}
//		}
		return true;
	}

	/**
	 * @param overriding overriding method (m1)
	 * @param overridden overridden method (m2)
	 * @return <code>true</code> iff the method <code>m1</code> is a subsignature of the method <code>m2</code>.
	 * 		This is one of the requirements for m1 to override m2.
	 * 		Accessibility and return types are not taken into account.
	 * 		Note that subsignature is <em>not</em> symmetric!
	 */
	public static boolean isSubsignature(IFunctionBinding overriding, IFunctionBinding overridden) {
		//TODO: use IFunctionBinding#isSubsignature(..) once it is tested and fixed (only erasure of m1's parameter types, considering type variable counts, doing type variable substitution		
		if (!overriding.getName().equals(overridden.getName()))
			return false;
			
		ITypeBinding[] m1Params= overriding.getParameterTypes();
		ITypeBinding[] m2Params= overridden.getParameterTypes();
		if (m1Params.length != m2Params.length)
			return false;
	
		// m1TypeParams.length == m2TypeParams.length == 0  
		if (equals(m1Params, m2Params))
			return true;
		for (int i= 0; i < m1Params.length; i++) {
			ITypeBinding m1Param= m1Params[i];
			if (! (equals(m1Param, m2Params[i].getErasure()))) // can erase m2
				return false;
		}
		return true;
		
	}

	/**
	 * @param method
	 * @param methodName
	 * @param parameters
	 * @return <code>true</code> iff the method
	 * 		m1 (with name <code>methodName</code> and method parameters <code>parameters</code>)
	 * 		is a subsignature of the method <code>m2</code>. Accessibility and return types are not taken into account.
	 */
	public static boolean isEqualMethod(IFunctionBinding method, String methodName, String[] parameters) {
		if (!method.getName().equals(methodName))
			return false;

		ITypeBinding[] methodParameters= method.getParameterTypes();
		if (methodParameters.length != parameters.length)
			return false;
		String first, second;
		int index;
		for (int i= 0; i < parameters.length; i++) {
			first= parameters[i];
			index= first.indexOf('<');
			if (index > 0)
				first= first.substring(0, index);
			second= methodParameters[i].getErasure().getQualifiedName();
			index= second.indexOf('<');
			if (index > 0)
				second= second.substring(0, index);
			if (!first.equals(second))
				return false;
		}
		return true;
	}

	/**
	 * Finds a type binding for a given fully qualified type in the hierarchy of a type.
	 * Returns <code>null</code> if no type binding is found.
	 * @param hierarchyType the binding representing the hierarchy
	 * @param fullyQualifiedTypeName the fully qualified name to search for
	 * @return the type binding
	 */
	public static ITypeBinding findTypeInHierarchy(ITypeBinding hierarchyType, String fullyQualifiedTypeName) {
		if (hierarchyType.isArray() || hierarchyType.isPrimitive()) {
			return null;
		}
		if (fullyQualifiedTypeName.equals(hierarchyType.getQualifiedName())) {
			return hierarchyType;
		}
		ITypeBinding superClass= hierarchyType.getSuperclass();
		if (superClass != null) {
			ITypeBinding res= findTypeInHierarchy(superClass, fullyQualifiedTypeName);
			if (res != null) {
				return res;
			}
		}
		return null;
	}
	
	/**
	 * Returns the binding of the variable written in an Assignment.
	 * @param assignment The assignment 
	 * @return The binding or <code>null</code> if no bindings are available.
	 */
	public static IVariableBinding getAssignedVariable(Assignment assignment) {
		Expression leftHand = assignment.getLeftHandSide();
		switch (leftHand.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				return (IVariableBinding) ((SimpleName) leftHand).resolveBinding();
			case ASTNode.QUALIFIED_NAME:
				return (IVariableBinding) ((QualifiedName) leftHand).getName().resolveBinding();				
			case ASTNode.FIELD_ACCESS:
				return ((FieldAccess) leftHand).resolveFieldBinding();
			case ASTNode.SUPER_FIELD_ACCESS:
				return ((SuperFieldAccess) leftHand).resolveFieldBinding();
			default:
				return null;
		}
	}
	
	/**
	 * Returns <code>true</code> if the given type is a super type of a candidate.
	 * <code>true</code> is returned if the two type bindings are identical (TODO)
	 * @param possibleSuperType the type to inspect
	 * @param type the type whose super types are looked at
	 * @return <code>true</code> iff <code>possibleSuperType</code> is
	 * 		a super type of <code>type</code> or is equal to it
	 */
	public static boolean isSuperType(ITypeBinding possibleSuperType, ITypeBinding type) {
		if (type.isArray() || type.isPrimitive()) {
			return false;
		}
		if (Bindings.equals(type, possibleSuperType)) {
			return true;
		}
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null) {
			if (isSuperType(possibleSuperType, superClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the compilation unit where the type of the given <code>ITypeBinding</code> is defined,
	 * using the class path defined by the given Java project. Returns <code>null</code>
	 * if no compilation unit is found (e.g. type binding is from a binary type)
	 * @param typeBinding the type binding to search for
	 * @param project the project used as a scope
	 * @return the compilation unit containing the type
	 * @throws JavaScriptModelException if an errors occurs in the Java model
	 */
	public static IJavaScriptUnit findCompilationUnit(ITypeBinding typeBinding, IJavaScriptProject project) throws JavaScriptModelException {
		IJavaScriptElement type= typeBinding.getJavaElement();
		if (type instanceof IType)
			return ((IType) type).getJavaScriptUnit();
		else
			return null;
	}

	/**
	 * Finds a method for the given <code>IFunctionBinding</code>. Returns
	 * <code>null</code> if the type doesn't contain a corresponding method.
	 * @param method the method to find
	 * @param type the type to look in
	 * @return the corresponding IFunction or <code>null</code>
	 * @throws JavaScriptModelException if an error occurs in the Java model
	 * @deprecated Use {@link #findMethodInHierarchy(ITypeBinding, String, String[])} or {@link JavaModelUtil}
	 */
	public static IFunction findMethod(IFunctionBinding method, IType type) throws JavaScriptModelException {
		method= method.getMethodDeclaration();
		
		IFunction[] candidates= type.getFunctions();
		for (int i= 0; i < candidates.length; i++) {
			IFunction candidate= candidates[i];
			if (candidate.getElementName().equals(method.getName()) && sameParameters(method, candidate)) {
				return candidate;
			}
		}
		return null;
	}			

	//---- Helper methods to convert a method ---------------------------------------------
	
	private static boolean sameParameters(IFunctionBinding method, IFunction candidate) throws JavaScriptModelException {
		ITypeBinding[] methodParamters= method.getParameterTypes();
		String[] candidateParameters= candidate.getParameterTypes();
		if (methodParamters.length != candidateParameters.length)
			return false;
		IType scope= candidate.getDeclaringType();
		for (int i= 0; i < methodParamters.length; i++) {
			ITypeBinding methodParameter= methodParamters[i];
			String candidateParameter= candidateParameters[i];
			if (!sameParameter(methodParameter, candidateParameter, scope))
				return false;
		}
		return true;
	}

	private static boolean sameParameter(ITypeBinding type, String candidate, IType scope) throws JavaScriptModelException {
		if (type.getDimensions() != Signature.getArrayCount(candidate))
			return false;
			
		// Normalizes types
		if (type.isArray())
			type= type.getElementType();
		candidate= Signature.getElementType(candidate);
		
		if ((Signature.getTypeSignatureKind(candidate) == Signature.BASE_TYPE_SIGNATURE) != type.isPrimitive()) {
			return false;
		}
			
		if (type.isPrimitive()) {
			return type.getName().equals(Signature.toString(candidate));
		} else {
			type= type.getErasure();
			
			if (candidate.charAt(Signature.getArrayCount(candidate)) == Signature.C_RESOLVED) {
				return Signature.toString(candidate).equals(Bindings.getFullyQualifiedName(type));
			} else {
				String[][] qualifiedCandidates= scope.resolveType(Signature.toString(candidate));
				if (qualifiedCandidates == null || qualifiedCandidates.length == 0)
					return false;
				String packageName= type.getPackage().isUnnamed() ? "" : type.getPackage().getName(); //$NON-NLS-1$
				String typeName= getTypeQualifiedName(type);
				for (int i= 0; i < qualifiedCandidates.length; i++) {
					String[] qualifiedCandidate= qualifiedCandidates[i];
					if (	qualifiedCandidate[0].equals(packageName) &&
							qualifiedCandidate[1].equals(typeName))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Normalizes a type binding received from an expression to a type binding that can be used in a declaration signature. 
	 * Anonymous types are normalized, to the super class or interface. For null or void bindings
	 * <code>null</code> is returned. 
	 * @param binding the binding to normalize
	 * @return the normalized binding
	 */
	public static ITypeBinding normalizeTypeBinding(ITypeBinding binding) {
		if (binding != null && !binding.isNullType() && !isVoidType(binding)) {
			if (binding.isAnonymous()) {
				return binding.getSuperclass();
			}
			return binding;
		}
		return null;
	}
	
	public static boolean isVoidType(ITypeBinding binding) {
		return "void".equals(binding.getName()); //$NON-NLS-1$
	}

	/**
	 * Normalizes the binding so that it can be used as a type inside a declaration
	 * (e.g. variable declaration, method return type, parameter type, ...). For
	 * null bindings Object is returned.
	 * @param binding binding to normalize
	 * @param ast current ast
	 * 
	 * @return the normalized type to be used in declarations
	 */
	public static ITypeBinding normalizeForDeclarationUse(ITypeBinding binding, AST ast) {
		if (binding.isNullType())
			return ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
		if (binding.isPrimitive())
			return binding;
		binding= normalizeTypeBinding(binding);
		return binding;
	}

	/**
	 * Returns the type binding of the node's parent type declaration.
	 * @param node
	 * @return the type binding of the node's parent type declaration
	 */
	public static ITypeBinding getBindingOfParentType(ASTNode node) {
		while (node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				return ((AbstractTypeDeclaration) node).resolveBinding();
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration) node).resolveBinding();
			} else if (node instanceof JavaScriptUnit) {
				return ((JavaScriptUnit) node).resolveBinding();
			}
			node= node.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the type binding of the node's type context or null if the node is an annotation, type parameter or super type declaration of a tope level type.
	 * The result of this method is equal to the result of {@link #getBindingOfParentType(ASTNode)} for nodes in the type's body.
	 * @param node
	 * @return the type binding of the node's parent type context
	 */
	public static ITypeBinding getBindingOfParentTypeContext(ASTNode node) {
		StructuralPropertyDescriptor lastLocation= null;

		while (node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration decl= (AbstractTypeDeclaration) node;
				if (lastLocation == decl.getBodyDeclarationsProperty()) {
					return decl.resolveBinding();
				}
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration) node).resolveBinding();
			}
			lastLocation= node.getLocationInParent();
			node= node.getParent();
		}
		return null;
	}

	
	public static String getRawName(ITypeBinding binding) {
		String name= binding.getName();
		return name;
	}
	

	public static String getRawQualifiedName(ITypeBinding binding) {
		final String EMPTY= ""; //$NON-NLS-1$
		
		if (binding.isAnonymous() || binding.isLocal()) {
			return EMPTY; 
		}
		
		if (binding.isPrimitive() || binding.isNullType()) {
			return binding.getName();
		}
		
		if (binding.isArray()) {
			String elementTypeQualifiedName = getRawQualifiedName(binding.getElementType());
			if (elementTypeQualifiedName.length() != 0) {
				StringBuffer stringBuffer= new StringBuffer(elementTypeQualifiedName);
				stringBuffer.append('[').append(']');
				return stringBuffer.toString();
			} else {
				return EMPTY;
			}
		}
		if (binding.isMember()) {
			String outerName= getRawQualifiedName(binding.getDeclaringClass());
			if (outerName.length() > 0) {
				StringBuffer buffer= new StringBuffer();
				buffer.append(outerName);
				buffer.append('.');
				buffer.append(getRawName(binding));
				return buffer.toString();
			} else {
				return EMPTY;
			}

		} else if (binding.isTopLevel()) {
			IPackageBinding packageBinding= binding.getPackage();
			StringBuffer buffer= new StringBuffer();
			if (packageBinding != null && packageBinding.getName().length() > 0) {
				buffer.append(packageBinding.getName()).append('.');
			}
			buffer.append(getRawName(binding));
			return buffer.toString();
		}
		return EMPTY;
	}
	

	/**
	 * Tests if the given node is a declaration, not a instance of a generic type, method or field.
	 * Declarations can be found in AST with JavaScriptUnit.findDeclaringNode
	 * @param binding binding to test
	 * @return returns <code>true</code> if the binding is a declaration binding
	 */
	public static boolean isDeclarationBinding(IBinding binding) {
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return ((ITypeBinding) binding).getTypeDeclaration() == binding;
			case IBinding.VARIABLE:
				return ((IVariableBinding) binding).getVariableDeclaration() == binding;
			case IBinding.METHOD:
				return ((IFunctionBinding) binding).getMethodDeclaration() == binding;
		}
		return true;
	}
	
	
	public static IBinding getDeclaration(IBinding binding) {
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return ((ITypeBinding) binding).getTypeDeclaration();
			case IBinding.VARIABLE:
				return ((IVariableBinding) binding).getVariableDeclaration();
			case IBinding.METHOD:
				return ((IFunctionBinding) binding).getMethodDeclaration();
		}
		return binding;
	}


	/**
	 * @deprecated Need to review: Use {@link #isSubsignature(IFunctionBinding, IFunctionBinding)} if the two bindings
	 * are in the same hierarchy (directly overrides each other), or {@link #findMethodInHierarchy(ITypeBinding, String, ITypeBinding[])}
	 * else.
	 */
	public static boolean containsSignatureEquivalentConstructor(IFunctionBinding[] candidates, IFunctionBinding overridable) {
		for (int index= 0; index < candidates.length; index++) {
			if (isSignatureEquivalentConstructor(candidates[index], overridable))
				return true;
		}
		return false;
	}

	private static boolean isSignatureEquivalentConstructor(IFunctionBinding overridden, IFunctionBinding overridable) {

		if (!overridden.isConstructor() || !overridable.isConstructor())
			return false;
		
		if (overridden.isDefaultConstructor())
			return false;
		
		return areSubTypeCompatible(overridden, overridable);
	}
	
	/**
	 * @deprecated Need to review: Use {@link #isSubsignature(IFunctionBinding, IFunctionBinding)} if the two bindings
	 * are in the same hierarchy (directly overrides each other), or {@link #findMethodInHierarchy(ITypeBinding, String, ITypeBinding[])}
	 * else.
	 */
	public static boolean areOverriddenMethods(IFunctionBinding overridden, IFunctionBinding overridable) {

		if (!overridden.getName().equals(overridable.getName()))
			return false;

		return areSubTypeCompatible(overridden, overridable);
	}

	private static boolean areSubTypeCompatible(IFunctionBinding overridden, IFunctionBinding overridable) {
		
		if (overridden.getParameterTypes().length != overridable.getParameterTypes().length)
			return false;
		
		ITypeBinding overriddenReturn= overridden.getReturnType();
		ITypeBinding overridableReturn= overridable.getReturnType();
		if (overriddenReturn == null || overridableReturn == null)
			return false;
		
		if (!overriddenReturn.getErasure().isSubTypeCompatible(overridableReturn.getErasure()))
			return false;
		
		ITypeBinding[] overriddenTypes= overridden.getParameterTypes();
		ITypeBinding[] overridableTypes= overridable.getParameterTypes();
		Assert.isTrue(overriddenTypes.length == overridableTypes.length);
		for (int index= 0; index < overriddenTypes.length; index++) {
			final ITypeBinding overridableErasure= overridableTypes[index].getErasure();
			final ITypeBinding overriddenErasure= overriddenTypes[index].getErasure();
			if (!overridableErasure.isSubTypeCompatible(overriddenErasure) || !overridableErasure.getKey().equals(overriddenErasure.getKey()))
				return false;
		}
		return true;
	}
	
}
