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

package org.eclipse.wst.jsdt.internal.corext.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;


public class MethodOverrideTester {
	private static class Substitutions {
		
		public static final Substitutions EMPTY_SUBST= new Substitutions();
		
		private HashMap fMap;
		
		public Substitutions() {
			fMap= null;
		}
		
		private String[] getSubstArray(String typeVariable) {
			if (fMap != null) {
				return (String[]) fMap.get(typeVariable);
			}
			return null;
		}
						
		public String getSubstitution(String typeVariable) {
			String[] subst= getSubstArray(typeVariable);
			if (subst != null) {
				return subst[0];
			}
			return null;
		}
		
		public String getErasure(String typeVariable) {
			String[] subst= getSubstArray(typeVariable);
			if (subst != null) {
				return subst[1];
			}
			return null;
		}
	}	
	
	private final IType fFocusType;
	private final ITypeHierarchy fHierarchy;
	
	private Map /* <IFunction, Substitutions> */ fMethodSubstitutions;
	private Map /* <IType, Substitutions> */ fTypeVariableSubstitutions;
			
	public MethodOverrideTester(IType focusType, ITypeHierarchy hierarchy) {
		if (focusType == null || hierarchy == null) {
			throw new IllegalArgumentException();
		}
		fFocusType= focusType;
		fHierarchy= hierarchy;
		fTypeVariableSubstitutions= null;
		fMethodSubstitutions= null;
	}
	
	public IType getFocusType() {
		return fFocusType;
	}
	
	public ITypeHierarchy getTypeHierarchy() {
		return fHierarchy;
	}
	
	/**
	 * Finds the method that declares the given method. A declaring method is the 'original' method declaration that does
	 * not override nor implement a method. <code>null</code> is returned it the given method does not override
	 * a method. When searching, super class are examined before implemented interfaces.
	 * @param testVisibility If true the result is tested on visibility. Null is returned if the method is not visible.
	 * @throws JavaScriptModelException
	 */
	public IFunction findDeclaringMethod(IFunction overriding, boolean testVisibility) throws JavaScriptModelException {
		IFunction result= null;
		IFunction overridden= findOverriddenMethod(overriding, testVisibility);
		while (overridden != null) {
			result= overridden;
			overridden= findOverriddenMethod(result, testVisibility);
		}
		return result;
	}
	
	/**
	 * Finds the method that is overridden by the given method.
	 * First the super class is examined and then the implemented interfaces.
	 * @param testVisibility If true the result is tested on visibility. Null is returned if the method is not visible.
	 * @throws JavaScriptModelException
	 */
	public IFunction findOverriddenMethod(IFunction overriding, boolean testVisibility) throws JavaScriptModelException {
		int flags= overriding.getFlags();
		if (Flags.isPrivate(flags) || Flags.isStatic(flags) || overriding.isConstructor()) {
			return null;
		}
		
		IType type= overriding.getDeclaringType();
		if (type==null)
			return null;
		IType superClass= fHierarchy.getSuperclass(type);
		if (superClass != null) {
			IFunction res= findOverriddenMethodInHierarchy(superClass, overriding);
			if (res != null && !Flags.isPrivate(res.getFlags())) {
				if (!testVisibility || JavaModelUtil.isVisibleInHierarchy(res, type.getPackageFragment())) {
					return res;
				}
			}
		}
		return null;
	}
	
	/**
	 * Finds the directly overridden method in a type and its super types. First the super class is examined and then the implemented interfaces.
	 * With generics it is possible that 2 methods in the same type are overidden at the same time. In that case, the first overridden method found is returned. 
	 * 	@param type The type to find methods in
	 * @param overriding The overriding method
	 * @return The first overridden method or <code>null</code> if no method is overridden
	 * @throws JavaScriptModelException
	 */
	public IFunction findOverriddenMethodInHierarchy(IType type, IFunction overriding) throws JavaScriptModelException {
		IFunction method= findOverriddenMethodInType(type, overriding);
		if (method != null) {
			return method;
		}
		IType superClass= fHierarchy.getSuperclass(type);
		if (superClass != null) {
			IFunction res=  findOverriddenMethodInHierarchy(superClass, overriding);
			if (res != null) {
				return res;
			}
		}
		return method;		
	}
	
	/**
	 * Finds an overridden method in a type. WWith generics it is possible that 2 methods in the same type are overidden at the same time.
	 * In that case the first overridden method found is returned.
	 * @param overriddenType The type to find methods in
	 * @param overriding The overriding method
	 * @return The first overridden method or <code>null</code> if no method is overridden
	 * @throws JavaScriptModelException
	 */
	public IFunction findOverriddenMethodInType(IType overriddenType, IFunction overriding) throws JavaScriptModelException {
		IFunction[] overriddenMethods= overriddenType.getFunctions();
		for (int i= 0; i < overriddenMethods.length; i++) {
			if (isSubsignature(overriding, overriddenMethods[i])) {
				return overriddenMethods[i];
			}
		}
		return null;
	}
	
	/**
	 * Finds an overriding method in a type.
	 * @param overridingType The type to find methods in
	 * @param overridden The overridden method
	 * @return The overriding method or <code>null</code> if no method is overriding.
	 * @throws JavaScriptModelException
	 */
	public IFunction findOverridingMethodInType(IType overridingType, IFunction overridden) throws JavaScriptModelException {
		IFunction[] overridingMethods= overridingType.getFunctions();
		for (int i= 0; i < overridingMethods.length; i++) {
			if (isSubsignature(overridingMethods[i], overridden)) {
				return overridingMethods[i];
			}
		}
		return null;
	}
	
	/**
	 * Tests if a method is a subsignature of another method.
	 * @param overriding overriding method (m1)
	 * @param overridden overridden method (m2)
	 * @return <code>true</code> iff the method <code>m1</code> is a subsignature of the method <code>m2</code>.
	 * 		This is one of the requirements for m1 to override m2.
	 * 		Accessibility and return types are not taken into account.
	 * 		Note that subsignature is <em>not</em> symmetric!
	 * @throws JavaScriptModelException
	 */
	public boolean isSubsignature(IFunction overriding, IFunction overridden) throws JavaScriptModelException {
		if (!overridden.getElementName().equals(overriding.getElementName())) {
			return false;
		}
		int nParameters= overridden.getNumberOfParameters();
		if (nParameters != overriding.getNumberOfParameters()) {
			return false;
		}
		
		return nParameters == 0 || hasCompatibleParameterTypes(overriding, overridden);
	}

	private boolean hasCompatibleParameterTypes(IFunction overriding, IFunction overridden) throws JavaScriptModelException {
		String[] overriddenParamTypes= overridden.getParameterTypes();
		String[] overridingParamTypes= overriding.getParameterTypes();
		
		String[] substitutedOverriding= new String[overridingParamTypes.length];
		boolean testErasure= false;
		
		for (int i= 0; i < overridingParamTypes.length; i++) {
			String overriddenParamSig= overriddenParamTypes[i];
			String overriddenParamName= getSubstitutedTypeName(overriddenParamSig, overridden);
			String overridingParamName= getSubstitutedTypeName(overridingParamTypes[i], overriding);
			substitutedOverriding[i]= overridingParamName;
			if (!overriddenParamName.equals(overridingParamName)) {
				testErasure= true;
				break;
			}
		}
		if (testErasure) {
			for (int i= 0; i < overridingParamTypes.length; i++) {
				String overriddenParamSig= overriddenParamTypes[i];
				String overriddenParamName= getErasedTypeName(overriddenParamSig, overridden);
				String overridingParamName= substitutedOverriding[i];
				if (overridingParamName == null)
					overridingParamName= getSubstitutedTypeName(overridingParamTypes[i], overriding);
				if (!overriddenParamName.equals(overridingParamName)) {
					return false;
				}
			}
		}
		return true;
	}
	
	private String getVariableSubstitution(IMember context, String variableName) throws JavaScriptModelException {
		IType type;
		if (context instanceof IFunction) {
			String subst= getMethodSubstitions((IFunction) context).getSubstitution(variableName);
			if (subst != null) {
				return subst;
			}
			type= context.getDeclaringType();
		} else {
			type= (IType) context;
		}
		String subst= getTypeSubstitions(type).getSubstitution(variableName);
		if (subst != null) {
			return subst;
		}
		return variableName; // not a type variable
	}
	
	private String getVariableErasure(IMember context, String variableName) throws JavaScriptModelException {
		IType type;
		if (context instanceof IFunction) {
			String subst= getMethodSubstitions((IFunction) context).getErasure(variableName);
			if (subst != null) {
				return subst;
			}
			type= context.getDeclaringType();
		} else {
			type= (IType) context;
		}
		String subst= getTypeSubstitions(type).getErasure(variableName);
		if (subst != null) {
			return subst;
		}
		return variableName; // not a type variable
	}
	
	/*
	 * Returns the substitutions for a method's type parameters
	 */
	private Substitutions getMethodSubstitions(IFunction method) throws JavaScriptModelException {
		if (fMethodSubstitutions == null) {
			fMethodSubstitutions= new LRUMap(3);
		}
		
		Substitutions s= (Substitutions) fMethodSubstitutions.get(method);
		if (s == null) {
			s= Substitutions.EMPTY_SUBST;
			fMethodSubstitutions.put(method, s);
		}
		return s;
	}
	
	/*
	 * Returns the substitutions for a type's type parameters
	 */
	private Substitutions getTypeSubstitions(IType type) throws JavaScriptModelException {
		if (fTypeVariableSubstitutions == null) {
			fTypeVariableSubstitutions= new HashMap();
			computeSubstitutions(fFocusType, null, null);
		}
		Substitutions subst= (Substitutions) fTypeVariableSubstitutions.get(type);
		if (subst == null) {
			return Substitutions.EMPTY_SUBST;
		}
		return subst;
	}
	
	private void computeSubstitutions(IType instantiatedType, IType instantiatingType, String[] typeArguments) throws JavaScriptModelException {
		Substitutions s= new Substitutions();
		fTypeVariableSubstitutions.put(instantiatedType, s);
		
		String superclassTypeSignature= instantiatedType.getSuperclassTypeSignature();
		if (superclassTypeSignature != null) {
			IType superclass= fHierarchy.getSuperclass(instantiatedType);
			if (superclass != null && !fTypeVariableSubstitutions.containsKey(superclass)) {
				computeSubstitutions(superclass, instantiatedType, new String[0]);
			}
		}
	}

	/**
	 * Translates the type signature to a 'normalized' type name where all variables are substituted for the given type or method context.
	 * The returned name contains only simple names and can be used to compare against other substituted type names
	 * @param typeSig The type signature to translate
	 * @param context The context for the substitution
	 * @return a type name
	 * @throws JavaScriptModelException 
	 */
	private String getSubstitutedTypeName(String typeSig, IMember context) throws JavaScriptModelException {
		return internalGetSubstitutedTypeName(typeSig, context, false, new StringBuffer()).toString();
	}
	
	private String getErasedTypeName(String typeSig, IMember context) throws JavaScriptModelException {
		return internalGetSubstitutedTypeName(typeSig, context, true, new StringBuffer()).toString();
	}
		
	private StringBuffer internalGetSubstitutedTypeName(String typeSig, IMember context, boolean erasure, StringBuffer buf) throws JavaScriptModelException {
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				return buf.append(Signature.toString(typeSig));
			case Signature.ARRAY_TYPE_SIGNATURE:
				internalGetSubstitutedTypeName(Signature.getElementType(typeSig), context, erasure, buf);
				for (int i= Signature.getArrayCount(typeSig); i > 0; i--) {
					buf.append('[').append(']');
				}
				return buf;
			case Signature.CLASS_TYPE_SIGNATURE: {
				String erasureSig= typeSig;
				String erasureName= Signature.getSimpleName(Signature.toString(erasureSig));
				
				char ch= erasureSig.charAt(0);
				if (ch == Signature.C_RESOLVED) {
					buf.append(erasureName);
				} else if (ch == Signature.C_UNRESOLVED) { // could be a type variable
					if (erasure) {
						buf.append(getVariableErasure(context, erasureName));
					} else {
						buf.append(getVariableSubstitution(context, erasureName));
					}
				} else {
					Assert.isTrue(false, "Unknown class type signature"); //$NON-NLS-1$
				}
				return buf;
			}
			default:
				Assert.isTrue(false, "Unhandled type signature kind"); //$NON-NLS-1$
				return buf;
		}
	}
			
}
