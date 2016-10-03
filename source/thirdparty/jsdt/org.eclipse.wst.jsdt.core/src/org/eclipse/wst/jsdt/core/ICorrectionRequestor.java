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
package org.eclipse.wst.jsdt.core;

/**
 * A callback interface for receiving javaScript problem correction.
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface ICorrectionRequestor {
/*
 * Notification of a class correction.
 *
 * @param packageName Declaring package name of the class.
 * @param className Name of the class.
 * @param correctionName The correction for the class.
 * @param modifiers The modifiers of the class.
 * @param correctionStart The start position of insertion of the correction of the class.
 * @param correctionEnd The end position of insertion of the correction of the class.
 *
 * NOTE - All package and type names are presented in their readable form:
 *    Package names are in the form "a.b.c".
 *    Nested type names are in the qualified form "A.M".
 *    The default package is represented by an empty array.
 */
void acceptClass(
	char[] packageName,
	char[] className,
	char[] correctionName,
	int modifiers,
	int correctionStart,
	int correctionEnd);
/**
 * Notification of a field/var correction.
 *
 * @param declaringTypePackageName Name of the package in which the type that contains this field is declared.
 * @param declaringTypeName Name of the type declaring this field.
 * @param name Name of the field.
 * @param typePackageName Name of the package in which the type of this field is declared.
 * @param typeName Name of the type of this field.
 * @param correctionName The correction for the field.
 * @param modifiers The modifiers of this field.
 * @param correctionStart The start position of insertion of the correction of this field.
 * @param correctionEnd The end position of insertion of the correction of this field.
 *
 */
void acceptField(
	char[] declaringTypePackageName,
	char[] declaringTypeName,
	char[] name,
	char[] typePackageName,
	char[] typeName,
	char[] correctionName,
	int modifiers,
	int correctionStart,
	int correctionEnd);
/**
 * Notification of a local variable correction.
 *
 * @param name Name of the local variable.
 * @param typePackageName Name of the package in which the type of this local variable is declared.
 * @param typeName Name of the type of this local variable.
 * @param modifiers The modifiers of this local variable.
 * @param correctionStart The start position of insertion of the correction of this local variable.
 * @param correctionEnd The end position of insertion of the correction of this local variable.
 *
 */
void acceptLocalVariable(
	char[] name,
	char[] typePackageName,
	char[] typeName,
	int modifiers,
	int correctionStart,
	int correctionEnd);
/**
 * Notification of a method correction.
 *
 * @param declaringTypePackageName Name of the package in which the type that contains this method is declared.
 * @param declaringTypeName Name of the type declaring this method.
 * @param selector Name of the method.
 * @param parameterPackageNames Names of the packages in which the parameter types are declared.
 *    Should contain as many elements as parameterTypeNames.
 * @param parameterTypeNames Names of the parameter types.
 *    Should contain as many elements as parameterPackageNames.
 * @param parameterNames Names of the parameters.
 *    Should contain as many elements as parameterPackageNames.
 * @param returnTypePackageName Name of the package in which the return type is declared.
 * @param returnTypeName Name of the return type of this method, should be <code>null</code> for a constructor.
 * @param correctionName The correction for the method.
 *   Can include zero, one or two brackets. If the closing bracket is included, then the cursor should be placed before it.
 * @param modifiers The modifiers of this method.
 * @param correctionStart The start position of insertion of the correction of this method.
 * @param correctionEnd The end position of insertion of the correction of this method.
 *
 * NOTE: parameter names can be retrieved from the source model after the user selects a specific method.
 */
void acceptMethod(
	char[] declaringTypePackageName,
	char[] declaringTypeName,
	char[] selector,
	char[][] parameterPackageNames,
	char[][] parameterTypeNames,
	char[][] parameterNames,
	char[] returnTypePackageName,
	char[] returnTypeName,
	char[] correctionName,
	int modifiers,
	int correctionStart,
	int correctionEnd);
/**
 * Notification of a package correction.
 *
 * @param packageName The package name.
 * @param correctionName The correction for the package.
 *   Can include '.*;' for imports.
 * @param correctionStart The start position of insertion of the correction of this package.
 * @param correctionEnd The end position of insertion of the correction of this package.
 *
 * <b>This Method only applies to ECMAScript 4 which is not yet supported</b>
 */
void acceptPackage(
	char[] packageName,
	char[] correctionName,
	int correctionStart,
	int correctionEnd);
}
