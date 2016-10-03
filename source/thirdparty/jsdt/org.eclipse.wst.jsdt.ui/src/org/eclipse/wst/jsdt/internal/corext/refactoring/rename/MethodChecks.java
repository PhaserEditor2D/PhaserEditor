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
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;

public class MethodChecks {

	//no instances
	private MethodChecks(){
	}
	
	public static boolean isVirtual(IFunction method) throws JavaScriptModelException {
		if (method.isConstructor())
			return false;
		if (JdtFlags.isPrivate(method))	
			return false;
		if (JdtFlags.isStatic(method))	
			return false;
		if (method.getDeclaringType()==null)
			return false;
		return true;	
	}	
	
	public static boolean isVirtual(IFunctionBinding methodBinding){
		if (methodBinding.isConstructor())
			return false;
		if (Modifier.isPrivate(methodBinding.getModifiers()))	//TODO is this enough?
			return false;
		if (Modifier.isStatic(methodBinding.getModifiers()))	//TODO is this enough?
			return false;
		return true;	
	}
	
	public static RefactoringStatus checkIfOverridesAnother(IFunction method, ITypeHierarchy hierarchy) throws JavaScriptModelException {
		IFunction overrides= MethodChecks.overridesAnotherMethod(method, hierarchy);
		if (overrides == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(overrides);
		String message= Messages.format(RefactoringCoreMessages.MethodChecks_overrides, 
				new String[]{JavaElementUtil.createMethodSignature(overrides), JavaModelUtil.getFullyQualifiedName(overrides.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, Corext.getPluginId(), RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD, overrides);
	}

	public static IFunction overridesAnotherMethod(IFunction method, ITypeHierarchy hierarchy) throws JavaScriptModelException {
		MethodOverrideTester tester= new MethodOverrideTester(method.getDeclaringType(), hierarchy);
		IFunction found= tester.findDeclaringMethod(method, true);
		boolean overrides= (found != null && !found.equals(method) && (!JdtFlags.isStatic(found)) && (!JdtFlags.isPrivate(found)));
		if (overrides)
			return found;
		else
			return null;
	}
	
	/**
	 * Locates the topmost method of an override ripple and returns it. If none
	 * is found, null is returned.
	 *
	 * @param method the IFunction which may be part of a ripple
	 * @param typeHierarchy a ITypeHierarchy of the declaring type of the method. May be null
	 * @param monitor an IProgressMonitor
	 * @return the topmost method of the ripple, or null if none
	 * @throws JavaScriptModelException
	 */
	public static IFunction getTopmostMethod(IFunction method, ITypeHierarchy typeHierarchy, IProgressMonitor monitor) throws JavaScriptModelException {

		Assert.isNotNull(method);

		ITypeHierarchy hierarchy= typeHierarchy;
		IFunction topmostMethod= null;
		final IType declaringType= method.getDeclaringType();
		if (declaringType==null)
			return method;
		
		if ((hierarchy == null) || !declaringType.equals(hierarchy.getType()))
			hierarchy= declaringType.newTypeHierarchy(monitor);
		
		if (hierarchy == null)
			hierarchy= declaringType.newSupertypeHierarchy(monitor);
		IFunction overrides= overridesAnotherMethod(method, hierarchy);
		if (overrides != null && !overrides.equals(method))
			topmostMethod= overrides;
		
		return topmostMethod;
	}

	/**
	 * Finds all overridden methods of a certain method.
	 * 
	 */
	public static IFunction[] getOverriddenMethods(IFunction method, IProgressMonitor monitor) throws CoreException {

		Assert.isNotNull(method);
		return RippleMethodFinder2.getRelatedMethods(method, monitor, null);
	}
}
