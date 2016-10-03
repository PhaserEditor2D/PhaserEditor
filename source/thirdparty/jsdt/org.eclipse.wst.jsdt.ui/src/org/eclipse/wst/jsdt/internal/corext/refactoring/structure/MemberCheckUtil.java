/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

class MemberCheckUtil {
	
	private MemberCheckUtil(){
		//static only
	}
	
	public static RefactoringStatus checkMembersInDestinationType(IMember[] members, IType destinationType) throws JavaScriptModelException {	
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < members.length; i++) {
			if (members[i].getElementType() == IJavaScriptElement.METHOD)
				checkMethodInType(destinationType, result, (IFunction)members[i]);
			else if (members[i].getElementType() == IJavaScriptElement.FIELD)
				checkFieldInType(destinationType, result, (IField)members[i]);
			else if (members[i].getElementType() == IJavaScriptElement.TYPE)
				checkTypeInType(destinationType, result, (IType)members[i]);
		}
		return result;	
	}

	private static void checkMethodInType(IType destinationType, RefactoringStatus result, IFunction method) throws JavaScriptModelException {
		IFunction[] destinationTypeMethods= destinationType.getFunctions();
		IFunction found= findMethod(method, destinationTypeMethods);
		if (found != null){
			RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), found.getSourceRange());
			String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_signature_exists, 
					new String[]{method.getElementName(), JavaModelUtil.getFullyQualifiedName(destinationType)});
			result.addError(message, context);
		} else {
			IFunction similar= Checks.findMethod(method, destinationType);
			if (similar != null){
				String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_same_param_count,
						 new String[]{method.getElementName(), JavaModelUtil.getFullyQualifiedName(destinationType)});
				RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), similar.getSourceRange());
				result.addWarning(message, context);
			}										
		}	
	}
	
	private static void checkFieldInType(IType destinationType, RefactoringStatus result, IField field) throws JavaScriptModelException {
		IField destinationTypeField= destinationType.getField(field.getElementName());	
		if (! destinationTypeField.exists())
			return;
		String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_field_exists, 
				new String[]{field.getElementName(), JavaModelUtil.getFullyQualifiedName(destinationType)});
		RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), destinationTypeField.getSourceRange());
		result.addError(message, context);
	}
	
	private static void checkTypeInType(IType destinationType, RefactoringStatus result, IType type) throws JavaScriptModelException {
		String typeName= type.getElementName();
		IType destinationTypeType= destinationType.getType(typeName);
		if (destinationTypeType.exists()){
			String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_type_name_conflict0,  
					new String[]{typeName, JavaModelUtil.getFullyQualifiedName(destinationType)});
			RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), destinationTypeType.getNameRange());
			result.addError(message, context);
		} else {
			//need to check the hierarchy of enclosing and enclosed types
			if (destinationType.getElementName().equals(typeName)){
				String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_type_name_conflict1,  
						new String[]{JavaModelUtil.getFullyQualifiedName(type)});
				RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), destinationType.getNameRange());
				result.addError(message, context);
			}
			if (typeNameExistsInEnclosingTypeChain(destinationType, typeName)){
				String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_type_name_conflict2,  
						new String[]{JavaModelUtil.getFullyQualifiedName(type)});
				RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), destinationType.getNameRange());
				result.addError(message, context);
			}
			checkHierarchyOfEnclosedTypes(destinationType, result, type);
		}
	}

	private static void checkHierarchyOfEnclosedTypes(IType destinationType, RefactoringStatus result, IType type) throws JavaScriptModelException {
		IType[] enclosedTypes= getAllEnclosedTypes(type);
		for (int i= 0; i < enclosedTypes.length; i++) {
			IType enclosedType= enclosedTypes[i];
			if (destinationType.getElementName().equals(enclosedType.getElementName())){
				String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_type_name_conflict3,  
						new String[]{JavaModelUtil.getFullyQualifiedName(enclosedType), JavaModelUtil.getFullyQualifiedName(type)});
				RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), destinationType.getNameRange());
				result.addError(message, context);
			}
			if (typeNameExistsInEnclosingTypeChain(destinationType, enclosedType.getElementName())){
				String message= Messages.format(RefactoringCoreMessages.MemberCheckUtil_type_name_conflict4,  
						new String[]{JavaModelUtil.getFullyQualifiedName(enclosedType), JavaModelUtil.getFullyQualifiedName(type)});
				RefactoringStatusContext context= JavaStatusContext.create(destinationType.getJavaScriptUnit(), destinationType.getNameRange());
				result.addError(message, context);
			}
		}
	}
	
	private static IType[] getAllEnclosedTypes(IType type) throws JavaScriptModelException {
		List result= new ArrayList(2);
		IType[] directlyEnclosed= type.getTypes();
		result.addAll(Arrays.asList(directlyEnclosed));
		for (int i= 0; i < directlyEnclosed.length; i++) {
			IType enclosedType= directlyEnclosed[i];
			result.addAll(Arrays.asList(getAllEnclosedTypes(enclosedType)));
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private static boolean typeNameExistsInEnclosingTypeChain(IType type, String typeName){
		IType enclosing= type.getDeclaringType();
		while (enclosing != null){
			if (enclosing.getElementName().equals(typeName))
				return true;
			enclosing= enclosing.getDeclaringType();
		}
		return false;
	}
	
	/**
	 * Finds a method in a list of methods. Compares methods by signature
	 * (only SimpleNames of types), and not by the declaring type.
	 * @return The found method or <code>null</code>, if nothing found
	 */
	public static IFunction findMethod(IFunction method, IFunction[] allMethods) throws JavaScriptModelException {
		String name= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		boolean isConstructor= method.isConstructor();

		for (int i= 0; i < allMethods.length; i++) {
			if (JavaModelUtil.isSameMethodSignature(name, paramTypes, isConstructor, allMethods[i]))
				return allMethods[i];
		}
		return null;
	}
}
