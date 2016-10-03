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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelMarker;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;

/**
 * This class defines a set of reusable static checks methods.
 */
public class Checks {
	
	/*
	 * no instances
	 */
	private Checks(){
	}
	
	/* Constants returned by checkExpressionIsRValue */
	public static final int IS_RVALUE= 0;
	public static final int NOT_RVALUE_MISC= 1;
	public static final int NOT_RVALUE_VOID= 2;

	/**
	 * Checks if method will have a constructor name after renaming.
	 * @param method
	 * @param newMethodName
	 * @param newTypeName 
	 * @return <code>RefactoringStatus</code> with <code>WARNING</code> severity if 
	 * the give method will have a constructor name after renaming
	 * <code>null</code> otherwise.
	 */
	public static RefactoringStatus checkIfConstructorName(IFunction method, String newMethodName, String newTypeName){
		if (! newMethodName.equals(newTypeName))
			return null;
		else
			return RefactoringStatus.createWarningStatus(
				Messages.format(RefactoringCoreMessages.Checks_constructor_name,  
				new Object[] {JavaElementUtil.createMethodSignature(method), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType()) } ));
	}
		
	/**
	 * Checks if the given name is a valid Java field name.
	 *
	 * @param name the java field name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java field name.
	 */
	public static RefactoringStatus checkFieldName(String name) {
		return checkName(name, JavaScriptConventions.validateFieldName(name));
	}

	/**
	 * Checks if the given name is a valid Java type parameter name.
	 *
	 * @param name the java type parameter name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java type parameter name.
	 */
	public static RefactoringStatus checkTypeParameterName(String name) {
		return checkName(name, JavaScriptConventions.validateTypeVariableName(name));
	}

	/**
	 * Checks if the given name is a valid Java identifier.
	 *
	 * @param name the java identifier.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java identifier.
	 */
	public static RefactoringStatus checkIdentifier(String name) {
		return checkName(name, JavaScriptConventions.validateIdentifier(name));
	}
	
	/**
	 * Checks if the given name is a valid Java method name.
	 *
	 * @param name the java method name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java method name.
	 */
	public static RefactoringStatus checkMethodName(String name) {
		RefactoringStatus status= checkName(name, JavaScriptConventions.validateFunctionName(name));
		if (status.isOK() && startsWithUpperCase(name))
			return RefactoringStatus.createWarningStatus(RefactoringCoreMessages.Checks_method_names_lowercase); 
		else	
			return status;
	}
		
	/**
	 * Checks if the given name is a valid Java type name.
	 *
	 * @param name the java method name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java type name.
	 */
	public static RefactoringStatus checkTypeName(String name) {
		//fix for: 1GF5Z0Z: ITPJUI:WINNT - assertion failed after renameType refactoring
//		if (name.indexOf(".") != -1) //$NON-NLS-1$
//			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_no_dot);
//		else	
			return checkName(name, JavaScriptConventions.validateJavaScriptTypeName(name));
	}
	
	/**
	 * Checks if the given name is a valid Java package name.
	 *
	 * @param name the java package name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java package name.
	 */
	public static RefactoringStatus checkPackageName(String name) {
		return checkName(name, JavaScriptConventions.validatePackageName(name));
	}
	
	/**
	 * Checks if the given name is a valid compilation unit name.
	 *
	 * @param name the compilation unit name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid compilation unit name.
	 */
	public static RefactoringStatus checkCompilationUnitName(String name) {
		return checkName(name, JavaScriptConventions.validateCompilationUnitName(name));
	}

	/**
	 * Returns ok status if the new name is ok. This is when no other file with that name exists.
	 * @param cu
	 * @param newName 
	 * @return the status
	 */
	public static RefactoringStatus checkCompilationUnitNewName(IJavaScriptUnit cu, String newName) {
		String newCUName= JavaModelUtil.getRenamedCUName(cu, newName);
		if (resourceExists(RenameResourceChange.renamedResourcePath(cu.getResource().getFullPath(), newCUName)))
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.Checks_cu_name_used, newName));
		else
			return new RefactoringStatus();
	}
		
	public static boolean startsWithUpperCase(String s) {
		if (s == null)
			return false;
		else if ("".equals(s)) //$NON-NLS-1$
			return false;
		else
			//workaround for JDK bug (see 26529)
			return s.charAt(0) == Character.toUpperCase(s.charAt(0));
	}
		
	public static boolean startsWithLowerCase(String s){
		if (s == null)
			return false;
		else if ("".equals(s)) //$NON-NLS-1$
			return false;
		else
			//workaround for JDK bug (see 26529)
			return s.charAt(0) == Character.toLowerCase(s.charAt(0));
	}

	public static boolean resourceExists(IPath resourcePath){
		return ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath) != null;
	}
	
	public static boolean isTopLevel(IType type){
		return type.getDeclaringType() == null;
	}

	public static boolean isAnonymous(IType type) throws JavaScriptModelException {
		return type.isAnonymous();
	}

	public static boolean isTopLevelType(IMember member){
		return  member.getElementType() == IJavaScriptElement.TYPE && isTopLevel((IType) member);
	}
	
	public static boolean isInsideLocalType(IType type) throws JavaScriptModelException {
		while (type != null) {
			if (type.isLocal())
				return true;
			type= type.getDeclaringType();
		}
		return false;
	}

	public static boolean isAlreadyNamed(IJavaScriptElement element, String name){
		return name.equals(element.getElementName());
	}

//	//-------------- main and native method checks ------------------
//	public static RefactoringStatus checkForMainAndNativeMethods(IJavaScriptUnit cu) throws JavaScriptModelException {
//		return checkForMainAndNativeMethods(cu.getTypes());
//	}
//	
//	public static RefactoringStatus checkForMainAndNativeMethods(IType[] types) throws JavaScriptModelException {
//		RefactoringStatus result= new RefactoringStatus();
//		for (int i= 0; i < types.length; i++)
//			result.merge(checkForMainAndNativeMethods(types[i]));
//		return result;
//	}
//	
//	public static RefactoringStatus checkForMainAndNativeMethods(IType type) throws JavaScriptModelException {
//		RefactoringStatus result= new RefactoringStatus();
//		result.merge(checkForMainAndNativeMethods(type.getMethods()));
//		result.merge(checkForMainAndNativeMethods(type.getTypes()));
//		return result;
//	}
//	
//	private static RefactoringStatus checkForMainAndNativeMethods(IFunction[] methods) throws JavaScriptModelException {
//		RefactoringStatus result= new RefactoringStatus();
//		for (int i= 0; i < methods.length; i++) {
//			if (JdtFlags.isNative(methods[i])){
//				String msg= Messages.format(RefactoringCoreMessages.Checks_method_native,  
//								new String[]{JavaModelUtil.getFullyQualifiedName(methods[i].getDeclaringType()), methods[i].getElementName(), "UnsatisfiedLinkError"});//$NON-NLS-1$
//				result.addEntry(RefactoringStatus.ERROR, msg, JavaStatusContext.create(methods[i]), Corext.getPluginId(), RefactoringStatusCodes.NATIVE_METHOD); 
//			}
//			if (methods[i].isMainMethod()) {
//				String msg= Messages.format(RefactoringCoreMessages.Checks_has_main,
//						JavaModelUtil.getFullyQualifiedName(methods[i].getDeclaringType()));
//				result.addEntry(RefactoringStatus.WARNING, msg, JavaStatusContext.create(methods[i]), Corext.getPluginId(), RefactoringStatusCodes.MAIN_METHOD); 
//			}
//		}
//		return result;
//	}
	
	//---- New method name checking -------------------------------------------------------------
	
	/**
	 * Checks if the new method is already used in the given type.
	 * @param type
	 * @param methodName
	 * @param parameters
	 * @return the status
	 */
	public static RefactoringStatus checkMethodInType(ITypeBinding type, String methodName, ITypeBinding[] parameters) {
		RefactoringStatus result= new RefactoringStatus();
		if (methodName.equals(type.getName()))
			result.addWarning(RefactoringCoreMessages.Checks_methodName_constructor); 
		IFunctionBinding method= org.eclipse.wst.jsdt.internal.corext.dom.Bindings.findMethodInType(type, methodName, parameters);
		if (method != null) 
			result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_exists,  
				new Object[] {methodName, type.getName()}),
				JavaStatusContext.create(method));
		return result;
	}
	
	/**
	 * Checks if the new method somehow conflicts with an already existing method in
	 * the hierarchy. The following checks are done:
	 * <ul>
	 *   <li> if the new method overrides a method defined in the given type or in one of its
	 * 		super classes. </li>
	 * </ul>
	 * @param type
	 * @param methodName
	 * @param returnType
	 * @param parameters
	 * @return the status
	 */
	public static RefactoringStatus checkMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding returnType, ITypeBinding[] parameters) {
		RefactoringStatus result= new RefactoringStatus();
		IFunctionBinding method= Bindings.findMethodInHierarchy(type, methodName, parameters);
		if (method != null) {
			boolean returnTypeClash= false;
			ITypeBinding methodReturnType= method.getReturnType();
			if (returnType != null && methodReturnType != null) {
				String returnTypeKey= returnType.getKey();
				String methodReturnTypeKey= methodReturnType.getKey();
				if (returnTypeKey == null && methodReturnTypeKey == null) {
					returnTypeClash= returnType != methodReturnType;	
				} else if (returnTypeKey != null && methodReturnTypeKey != null) {
					returnTypeClash= !returnTypeKey.equals(methodReturnTypeKey);
				}
			}
			ITypeBinding dc= method.getDeclaringClass();
			if (returnTypeClash) {
				result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_returnTypeClash, 
					new Object[] {methodName, dc.getName()}),
					JavaStatusContext.create(method));
			} else {
				result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_overrides, 
					new Object[] {methodName, dc.getName()}),
					JavaStatusContext.create(method));
			}
		}
		return result;
	}
	
	//---- Selection checks --------------------------------------------------------------------

	public static boolean isExtractableExpression(ASTNode[] selectedNodes, ASTNode coveringNode) {
		ASTNode node= coveringNode;
		if (selectedNodes != null && selectedNodes.length == 1)
			node= selectedNodes[0];
		return isExtractableExpression(node);
	}

	public static boolean isExtractableExpression(ASTNode node) {
		if (!(node instanceof Expression))
			return false;
		if (node instanceof Name) {
			IBinding binding= ((Name) node).resolveBinding();
			return !(binding instanceof ITypeBinding);
		}
		return true;
	}

	public static boolean isInsideJavadoc(ASTNode node) {
		do {
			if (node.getNodeType() == ASTNode.JSDOC)
				return true;
			node= node.getParent();
		} while (node != null);
		return false;
	}

	/**
	 * Returns a fatal error in case the name is empty. In all other cases, an
	 * error based on the given status is returned.
	 * 
	 * @param name a name
	 * @param status a status
	 * @return RefactoringStatus based on the given status or the name, if
	 *         empty.
	 */
	public static RefactoringStatus checkName(String name, IStatus status) {
		RefactoringStatus result= new RefactoringStatus();
		if ("".equals(name)) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_Choose_name); 

		if (status.isOK())
			return result;
		
		switch (status.getSeverity()){
			case IStatus.ERROR: 
				return RefactoringStatus.createFatalErrorStatus(status.getMessage());
			case IStatus.WARNING: 
				return RefactoringStatus.createWarningStatus(status.getMessage());
			case IStatus.INFO:
				return RefactoringStatus.createInfoStatus(status.getMessage());
			default: //no nothing
				return new RefactoringStatus();
		}
	}
	
	/**
	 * Finds a method in a type
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @param name
	 * @param parameterCount
	 * @param isConstructor
	 * @param type
	 * @return The first found method or null, if nothing found
	 * @throws JavaScriptModelException
	 */
	public static IFunction findMethod(String name, int parameterCount, boolean isConstructor, IType type) throws JavaScriptModelException {
		return findMethod(name, parameterCount, isConstructor, type.getFunctions());
	}
	
	/**
	 * Finds a method in a type.
	 * Searches for a method with the same name and the same parameter count.
	 * Parameter types are <b>not</b> compared.
	 * @param method
	 * @param type
	 * @return The first found method or null, if nothing found
	 * @throws JavaScriptModelException
	 */
	public static IFunction findMethod(IFunction method, IType type) throws JavaScriptModelException {
		return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), type.getFunctions());
	}

	/**
	 * Finds a method in an array of methods.
	 * Searches for a method with the same name and the same parameter count.
	 * Parameter types are <b>not</b> compared.
	 * @param method
	 * @param methods
	 * @return The first found method or null, if nothing found
	 * @throws JavaScriptModelException
	 */
	public static IFunction findMethod(IFunction method, IFunction[] methods) throws JavaScriptModelException {
		return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), methods);
	}
	
	public static IFunction findMethod(String name, int parameters, boolean isConstructor, IFunction[] methods) throws JavaScriptModelException {	
		for (int i= methods.length-1; i >= 0; i--) {
			IFunction curr= methods[i];
			if (name.equals(curr.getElementName())) {
				if (isConstructor == curr.isConstructor()) {
					if (parameters == curr.getParameterTypes().length) {
						return curr;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Finds a method in a type.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @param method
	 * @param type
	 * @return The first found method or null, if nothing found
	 * @throws JavaScriptModelException
	 */
	public static IFunction findSimilarMethod(IFunction method, IType type) throws JavaScriptModelException {
		return findSimilarMethod(method, type.getFunctions());
	}

	/**
	 * Finds a method in an array of methods.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @param method
	 * @param methods
	 * @return The first found method or null, if nothing found
	 * @throws JavaScriptModelException
	 */
	public static IFunction findSimilarMethod(IFunction method, IFunction[] methods) throws JavaScriptModelException {
		boolean isConstructor= method.isConstructor();
		for (int i= 0; i < methods.length; i++) {
			IFunction otherMethod= methods[i];
			if (otherMethod.isConstructor() == isConstructor && method.isSimilar(otherMethod))
				return otherMethod;
		}
		return null;
	}
				
	/*
	 * Compare two parameter signatures
	 */
	public static boolean compareParamTypes(String[] paramTypes1, String[] paramTypes2) {
		if (paramTypes1.length == paramTypes2.length) {
			int i= 0;
			while (i < paramTypes1.length) {
				String t1= Signature.getSimpleName(Signature.toString(paramTypes1[i]));
				String t2= Signature.getSimpleName(Signature.toString(paramTypes2[i]));
				if (!t1.equals(t2)) {
					return false;
				}
				i++;
			}
			return true;
		}
		return false;
	}
	
	//---------------------
	
	public static RefactoringStatus checkIfCuBroken(IMember member) throws JavaScriptModelException{
		IJavaScriptUnit cu= (IJavaScriptUnit)JavaScriptCore.create(member.getJavaScriptUnit().getResource());
		if (cu == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_cu_not_created);	 
		else if (! cu.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_cu_not_parsed);	 
		return new RefactoringStatus();
	}
	
	/**
	 * From SearchResultGroup[] passed as the parameter
	 * this method removes all those that correspond to a non-parsable IJavaScriptUnit
	 * and returns it as a result.
	 * @param grouped the array of search result groups from which non parsable compilation
	 *  units are to be removed.
	 * @param status a refactoring status to collect errors and problems
	 * @return the array of search result groups 
	 * @throws JavaScriptModelException
	 */	
	public static SearchResultGroup[] excludeCompilationUnits(SearchResultGroup[] grouped, RefactoringStatus status) throws JavaScriptModelException{
		List result= new ArrayList();
		boolean wasEmpty= grouped.length == 0;
		for (int i= 0; i < grouped.length; i++){	
			IResource resource= grouped[i].getResource();
			IJavaScriptElement element= JavaScriptCore.create(resource);
			if (! (element instanceof IJavaScriptUnit))
				continue;
			//XXX this is a workaround 	for a jcore feature that shows errors in cus only when you get the original element
			IJavaScriptUnit cu= (IJavaScriptUnit)JavaScriptCore.create(resource);
			if (! cu.isStructureKnown()){
				String path= Checks.getFullPath(cu);
				status.addError(Messages.format(RefactoringCoreMessages.Checks_cannot_be_parsed, path)); 
				continue; //removed, go to the next one
			}
			result.add(grouped[i]);	
		}
		
		if ((!wasEmpty) && result.isEmpty())
			status.addFatalError(RefactoringCoreMessages.Checks_all_excluded); 
		
		return (SearchResultGroup[])result.toArray(new SearchResultGroup[result.size()]);
	}
	
	private static final String getFullPath(IJavaScriptUnit cu) {
		Assert.isTrue(cu.exists());
		return cu.getResource().getFullPath().toString();
	}
	
	
	public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] grouped) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < grouped.length; i++)
			checkCompileErrorsInAffectedFile(result, grouped[i].getResource());
		return result;
	}
	
	public static void checkCompileErrorsInAffectedFile(RefactoringStatus result, IResource resource) throws JavaScriptModelException {
		if (hasCompileErrors(resource))
			result.addWarning(Messages.format(RefactoringCoreMessages.Checks_cu_has_compile_errors, resource.getFullPath().makeRelative())); 
	}
	
	public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] references, IResource declaring) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < references.length; i++){
			IResource resource= references[i].getResource();
			if (resource.equals(declaring))
				declaring= null;
			checkCompileErrorsInAffectedFile(result, resource);
		}
		if (declaring != null)
			checkCompileErrorsInAffectedFile(result, declaring);
		return result;
	}
	
	private static boolean hasCompileErrors(IResource resource) throws JavaScriptModelException {
		try {
			IMarker[] problemMarkers= resource.findMarkers(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			for (int i= 0; i < problemMarkers.length; i++) {
				if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
					return true;
			}
			return false;
		} catch (JavaScriptModelException e){
			throw e;		
		} catch (CoreException e){
			throw new JavaScriptModelException(e);
		}
	}
	
	//------
	public static boolean isReadOnly(Object element) throws JavaScriptModelException{
		if (element instanceof IResource)
			return isReadOnly((IResource)element);
		
		if (element instanceof IJavaScriptElement) {
			if ((element instanceof IPackageFragmentRoot) && isClasspathDelete((IPackageFragmentRoot)element)) 
				return false;
			return isReadOnly(((IJavaScriptElement)element).getResource());
		}
		
		Assert.isTrue(false, "not expected to get here");	 //$NON-NLS-1$
		return false;
	}
	
	public static boolean isReadOnly(IResource res) throws JavaScriptModelException {
		ResourceAttributes attributes= res.getResourceAttributes();
		if (attributes != null && attributes.isReadOnly())
			return true;
		
		if (! (res instanceof IContainer))	
			return false;
		
		IContainer container= (IContainer)res;
		try {
			IResource[] children= container.members();
			for (int i= 0; i < children.length; i++) {
				if (isReadOnly(children[i]))
					return true;
			}
			return false;
		} catch (JavaScriptModelException e){
			throw e;
		} catch (CoreException e) {
			throw new JavaScriptModelException(e);
		}
	}
	
	public static boolean isClasspathDelete(IPackageFragmentRoot pkgRoot) {
		IResource res= pkgRoot.getResource();
		if (res == null)
			return true;
		IProject definingProject= res.getProject();
		if (res.getParent() != null && pkgRoot.isArchive() && ! res.getParent().equals(definingProject))
			return true;
		
		IProject occurringProject= pkgRoot.getJavaScriptProject().getProject();
		return !definingProject.equals(occurringProject);
	}
	
	//-------- validateEdit checks ----
	
	public static RefactoringStatus validateModifiesFiles(IFile[] filesToModify, Object context) {
		RefactoringStatus result= new RefactoringStatus();
		IStatus status= Resources.checkInSync(filesToModify);
		if (!status.isOK())
			result.merge(RefactoringStatus.create(status));
		status= Resources.makeCommittable(filesToModify, context);
		if (!status.isOK()) {
			result.merge(RefactoringStatus.create(status));
			if (!result.hasFatalError()) {
				result.addFatalError(RefactoringCoreMessages.Checks_validateEdit); 
			}			
		}
		return result;
	}
	
	public static RefactoringStatus validateEdit(IJavaScriptUnit unit, Object context) {
		IResource resource= unit.getPrimary().getResource();
		RefactoringStatus result= new RefactoringStatus();
		if (resource == null)
			return result;
		IStatus status= Resources.checkInSync(resource);
		if (!status.isOK())
			result.merge(RefactoringStatus.create(status));
		status= Resources.makeCommittable(resource, context);
		if (!status.isOK()) {
			result.merge(RefactoringStatus.create(status));
			if (!result.hasFatalError()) {
				result.addFatalError(RefactoringCoreMessages.Checks_validateEdit); 
			}			
		}
		return result;
	}	

	/**
	 * Checks whether it is possible to modify the given <code>IJavaScriptElement</code>.
	 * The <code>IJavaScriptElement</code> must exist and be non read-only to be modifiable.
	 * Moreover, if it is a <code>IMember</code> it must not be binary.
	 * The returned <code>RefactoringStatus</code> has <code>ERROR</code> severity if
	 * it is not possible to modify the element.
	 * @param javaElement
	 * @return the status
	 * @throws JavaScriptModelException
	 *
	 * @see IJavaScriptElement#exists
	 * @see IJavaScriptElement#isReadOnly
	 * @see IMember#isBinary
	 * @see RefactoringStatus
	 */ 
	public static RefactoringStatus checkAvailability(IJavaScriptElement javaElement) throws JavaScriptModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (! javaElement.exists())
			result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_not_in_model, javaElement.getElementName())); 
		if (javaElement.isReadOnly())
			result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_read_only, javaElement.getElementName()));	 
		if (javaElement.exists() && !javaElement.isStructureKnown())
			result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_unknown_structure, javaElement.getElementName()));	 
		if (javaElement instanceof IMember && ((IMember)javaElement).isBinary())
			result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_binary, javaElement.getElementName())); 
		return result;
	}
	
	public static boolean isAvailable(IJavaScriptElement javaElement) throws JavaScriptModelException {
		if (javaElement == null)
			return false;
		if (! javaElement.exists())
			return false;
		if (javaElement.isReadOnly())
			return false;
		// work around for https://bugs.eclipse.org/bugs/show_bug.cgi?id=48422
		// the Java project is now cheating regarding its children so we shouldn't
		// call isStructureKnown if the project isn't open.
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=52474
		if (!(javaElement instanceof IJavaScriptProject) && !(javaElement instanceof ILocalVariable) && !javaElement.isStructureKnown())
			return false;
		if (javaElement instanceof IMember && ((IMember)javaElement).isBinary())
			return false;
		return true;
	}

	public static IType findTypeInPackage(IPackageFragment pack, String name) throws JavaScriptModelException {
		Assert.isTrue(pack.exists());
		Assert.isTrue(!pack.isReadOnly());
		
		/* IJavaScriptUnit.getType expects simple name*/  
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
		IJavaScriptUnit[] cus= pack.getJavaScriptUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return cus[i].getType(name);
		}
		return null;
	}

	public static RefactoringStatus checkTempName(String newName) {
		RefactoringStatus result= Checks.checkIdentifier(newName);
		if (result.hasFatalError())
			return result;
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning(RefactoringCoreMessages.ExtractTempRefactoring_convention); 
		return result;		
	}

	public static RefactoringStatus checkEnumConstantName(String newName) {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (result.hasFatalError())
			return result;
		for (int i= 0; i < newName.length(); i++) {
			char c= newName.charAt(i);
			if (Character.isLetter(c) && !Character.isUpperCase(c)) {
				result.addWarning(RefactoringCoreMessages.RenameEnumConstRefactoring_convention); 
				break;
			}
		}
		return result;
	}

	public static RefactoringStatus checkConstantName(String newName) {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (result.hasFatalError())
			return result;
		for (int i= 0; i < newName.length(); i++) {
			char c= newName.charAt(i);
			if (Character.isLetter(c) && !Character.isUpperCase(c)) {
				result.addWarning(RefactoringCoreMessages.ExtractConstantRefactoring_convention); 
				break;
			}
		}
		return result;
	}

	public static boolean isException(IType iType, IProgressMonitor pm) throws JavaScriptModelException {
		try{
			if (! iType.isClass())
				return false;
			IType[] superTypes= iType.newSupertypeHierarchy(pm).getAllSuperclasses(iType);
			for (int i= 0; i < superTypes.length; i++) {
				if ("java.lang.Throwable".equals(superTypes[i].getFullyQualifiedName())) //$NON-NLS-1$
					return true;
			}
			return false;
		} finally{
			pm.done();
		}	
	}
		
	/**
	 * @param e
	 * @return int
	 *          Checks.IS_RVALUE		if e is an rvalue
	 *          Checks.NOT_RVALUE_VOID  if e is not an rvalue because its type is void
	 *          Checks.NOT_RVALUE_MISC  if e is not an rvalue for some other reason
	 */
	public static int checkExpressionIsRValue(Expression e) {
		if(e instanceof Name) {
			if(!(((Name) e).resolveBinding() instanceof IVariableBinding)) {
				return NOT_RVALUE_MISC;
			}
		}
		
		ITypeBinding tb= e.resolveTypeBinding();
		if (tb == null)
			return NOT_RVALUE_MISC;
		else if (tb.getName().equals("void")) //$NON-NLS-1$
			return NOT_RVALUE_VOID;

		return IS_RVALUE;		
	}

	public static boolean isDeclaredIn(VariableDeclaration tempDeclaration, Class astNodeClass) {
		ASTNode initializer= ASTNodes.getParent(tempDeclaration, astNodeClass);
		if (initializer == null)
			return false;
		ASTNode anonymous= ASTNodes.getParent(tempDeclaration, AnonymousClassDeclaration.class);	
		if (anonymous == null)
			return true;
		// stupid code. Is to find out if the variable declaration isn't a field.
		if (ASTNodes.isParent(anonymous, initializer))
			return false;
		return true;	
	}
}
