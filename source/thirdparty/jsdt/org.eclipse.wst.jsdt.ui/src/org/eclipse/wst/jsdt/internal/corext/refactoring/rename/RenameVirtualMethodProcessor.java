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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;


public class RenameVirtualMethodProcessor extends RenameMethodProcessor {
	
	private IFunction fOriginalMethod;
	private boolean fActivationChecked;
	private ITypeHierarchy fCachedHierarchy= null;
	
	/**
	 * Creates a new rename method processor.
	 * @param method the method, or <code>null</code> if invoked by scripting
	 */
	public RenameVirtualMethodProcessor(IFunction method) {
		super(method);
		fOriginalMethod= getMethod();
	}
	
	/*
	 * int. not javadoc'd
	 * 
	 * Protected constructor; only called from RenameTypeProcessor. Initializes
	 * the method processor with an already resolved top level and ripple
	 * methods.
	 * 
	 */
	RenameVirtualMethodProcessor(IFunction topLevel, IFunction[] ripples, TextChangeManager changeManager, ITypeHierarchy hierarchy, GroupCategorySet categorySet) {
		super(topLevel, changeManager, categorySet);
		fOriginalMethod= getMethod();
		fActivationChecked= true; // is top level
		fCachedHierarchy= hierarchy; // may be null
		setMethodsToRename(ripples);
	}

	public IFunction getOriginalMethod() {
		return fOriginalMethod;
	}

	private ITypeHierarchy getCachedHierarchy(IType declaring, IProgressMonitor monitor) throws JavaScriptModelException {
		if (fCachedHierarchy != null && declaring.equals(fCachedHierarchy.getType()))
			return fCachedHierarchy;
		fCachedHierarchy= declaring.newTypeHierarchy(new SubProgressMonitor(monitor, 1));
		return fCachedHierarchy;
	}

	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameVirtualMethodAvailable(getMethod());
	}
	
	//------------ preconditions -------------
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= super.checkInitialConditions(monitor);
		if (result.hasFatalError())
			return result;
		try{
			monitor.beginTask("", 3); //$NON-NLS-1$
			if (!fActivationChecked) {
				// the following code may change the method to be changed.
				IFunction method= getMethod();
				fOriginalMethod= method;
				
				ITypeHierarchy hierarchy= null;
				IType declaringType= method.getDeclaringType();
				if (declaringType!=null)
					hierarchy= getCachedHierarchy(declaringType, new SubProgressMonitor(monitor, 1));

				IFunction topmost= getMethod();
				if (MethodChecks.isVirtual(topmost))
					topmost= MethodChecks.getTopmostMethod(getMethod(), hierarchy, monitor);
				if (topmost != null)
					initialize(topmost);
				fActivationChecked= true;
			}
		} finally{
			monitor.done();
		}
		return result;
	}

	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext) throws CoreException {
		try{
			pm.beginTask("", 9); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();

			result.merge(super.doCheckFinalConditions(new SubProgressMonitor(pm, 7), checkContext));
			if (result.hasFatalError())
				return result;

			final IFunction method= getMethod();
			final IType declaring= method.getDeclaringType();
			final ITypeHierarchy hierarchy= getCachedHierarchy(declaring, new SubProgressMonitor(pm, 1));
			final String name= getNewElementName();
			
			IFunction[] hierarchyMethods= hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), hierarchy, method, name);
			for (int i= 0; i < hierarchyMethods.length; i++) {
				IFunction hierarchyMethod= hierarchyMethods[i];
				RefactoringStatusContext context= JavaStatusContext.create(hierarchyMethod);
				if (Checks.compareParamTypes(method.getParameterTypes(), hierarchyMethod.getParameterTypes())) {
					result.addError(Messages.format(
						RefactoringCoreMessages.RenameVirtualMethodRefactoring_hierarchy_declares2, 
						name), context); 
				} else {
					result.addWarning(Messages.format(
						RefactoringCoreMessages.RenameVirtualMethodRefactoring_hierarchy_declares1, 
						name), context); 
				}					
			}
			
			fCachedHierarchy= null;
			return result;
		} finally{
			pm.done();
		}
	}
	
	//---- Interface checks -------------------------------------
	
	private IFunction[] relatedTypeDeclaresMethodName(IProgressMonitor pm, IFunction method, String newName) throws CoreException {
		try{
			Set result= new HashSet();
			Set types= getRelatedTypes();
			pm.beginTask("", types.size()); //$NON-NLS-1$
			for (Iterator iter= types.iterator(); iter.hasNext(); ) {
				final IFunction found= Checks.findMethod(method, (IType)iter.next());
				final IType declaring= found.getDeclaringType();
				result.addAll(Arrays.asList(hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), declaring.newTypeHierarchy(new SubProgressMonitor(pm, 1)), found, newName)));
			}
			return (IFunction[]) result.toArray(new IFunction[result.size()]);
		} finally {
			pm.done();
		}	
	}

	private boolean isSpecialCase() throws CoreException {
		String[] noParams= new String[0];
		String[] specialNames= new String[]{"toString", "toString", "toString", "toString", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
											"getClass", "getClass", "notify", //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
											"notifyAll", "wait"}; //$NON-NLS-2$ //$NON-NLS-1$
		String[][] specialParamTypes= new String[][]{noParams, noParams, noParams, noParams,
													 noParams, noParams, //$NON-NLS-2$ //$NON-NLS-1$
													 noParams, noParams, noParams};
		String[] specialReturnTypes= new String[]{"QString;", "QString;", "Qjava.lang.String;", "Qjava.lang.String;", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
												   "QClass;", "Qjava.lang.Class;", //$NON-NLS-2$ //$NON-NLS-1$
												   Signature.SIG_VOID, Signature.SIG_VOID, Signature.SIG_VOID};
		Assert.isTrue((specialNames.length == specialParamTypes.length) && (specialParamTypes.length == specialReturnTypes.length));
		for (int i= 0; i < specialNames.length; i++){
			if (specialNames[i].equals(getNewElementName()) 
				&& Checks.compareParamTypes(getMethod().getParameterTypes(), specialParamTypes[i]) 
				&& !specialReturnTypes[i].equals(getMethod().getReturnType())){
					return true;
			}
		}
		return false;		
	}
	
	private Set getRelatedTypes() {
		Set methods= getMethodsToRename();
		Set result= new HashSet(methods.size());
		for (Iterator iter= methods.iterator(); iter.hasNext(); ){
			result.add(((IFunction)iter.next()).getDeclaringType());
		}
		return result;
	}
	
	//---- Class checks -------------------------------------
	

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		final RefactoringStatus status= super.initialize(arguments);
		fOriginalMethod= getMethod();
		return status;
	}

	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_renamed_plural;
		else
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_renamed_singular;
	}
}
