/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContextType;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class CodeGeneration {

	
	/**
	 * Constant ID for the type kind to be used in {@link #getTypeBody(String, IJavaScriptUnit, String, String)} to get the code template used
	 * for a new class type body.
	 * 
	 */
	public static final String CLASS_BODY_TEMPLATE_ID= CodeTemplateContextType.CLASSBODY_ID;
	
	private CodeGeneration() {
	}
	
	/**
	 * Returns the content for a new compilation unit using the 'new JavaScript file' code template.
	 * @param cu The compilation unit to create the source for. The compilation unit does not need to exist.
	 * @param typeComment The comment for the type to be created. Used when the code template contains a <i>${typecomment}</i> variable. Can be <code>null</code> if
	 * no comment should be added.
	 * @param typeContent The code of the type, including type declaration and body.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the template is undefined or empty.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 */
	public static String getCompilationUnitContent(IJavaScriptUnit cu, String typeComment, String typeContent, String lineDelimiter) throws CoreException {	
		return getCompilationUnitContent(cu, getFileComment(cu, lineDelimiter), typeComment, typeContent, lineDelimiter);
	}
	
	/**
	 * Returns the content for a new compilation unit using the 'new JavaScript file' code template.
	 * @param cu The compilation unit to create the source for. The compilation unit does not need to exist.
	 * 	@param fileComment The file comment to be used when the code template contains a <i>${filecomment}</i> variable. Can be <code>null</code> if
	 * no comment should be added.
	 * @param typeComment The comment for the type to be created. Used when the code template contains a <i>${typecomment}</i> variable. Can be <code>null</code> if
	 * no comment should be added.
	 * @param typeContent The code of the type, including type declaration and body.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the template is undefined or empty.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */
	public static String getCompilationUnitContent(IJavaScriptUnit cu, String fileComment, String typeComment, String typeContent, String lineDelimiter) throws CoreException {	
		return StubUtility.getCompilationUnitContent(cu, fileComment, typeComment, typeContent, lineDelimiter);
	}
	
	/**
	 * Returns the content for a new file comment using the 'file comment' code template. The returned content is unformatted and is not indented.
	 * @param cu The compilation unit to add the comment to. The compilation unit does not need to exist.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the code template is undefined or empty. The returned content is unformatted and is not indented.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */	
	public static String getFileComment(IJavaScriptUnit cu, String lineDelimiter) throws CoreException {
		return StubUtility.getFileComment(cu, lineDelimiter);
	}
	
	/**
	 * Returns the content for a new type comment using the 'type comment' code template. The returned content is unformatted and is not indented.
	 * @param cu The compilation unit where the type is contained. The compilation unit does not need to exist.
	 * @param typeQualifiedName The name of the type to which the comment is added. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the code template is undefined or empty. The returned content is unformatted and is not indented.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 */	
	public static String getTypeComment(IJavaScriptUnit cu, String typeQualifiedName, String lineDelimiter) throws CoreException {
		return StubUtility.getTypeComment(cu, typeQualifiedName, lineDelimiter);
	}
		
	/**
	 * Returns the content of a new new type body using the 'type body' code templates. The returned content is unformatted and is not indented.
	 * @param typeKind The type kind ID of the body template. Valid values are {@link #CLASS_BODY_TEMPLATE_ID}, {@link #INTERFACE_BODY_TEMPLATE_ID},
	 * {@link #ENUM_BODY_TEMPLATE_ID} and {@link #ANNOTATION_BODY_TEMPLATE_ID}.
	 * @param cu The compilation unit where the type is contained. The compilation unit does not need to exist.
	 * @param typeName The name of the type(for embedding in the template as a user variable).
	 * @param lineDelim The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the code template is undefined or empty. The returned content is unformatted and is not indented.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */	
	public static String getTypeBody(String typeKind, IJavaScriptUnit cu, String typeName, String lineDelim) throws CoreException {
		return StubUtility.getTypeBody(typeKind, cu, typeName, lineDelim);
	}

	/**
	 * Returns the content for a new field comment using the 'field comment' code template. The returned content is unformatted and is not indented.
	 * @param cu The compilation unit where the field is contained. The compilation unit does not need to exist.
	 * @param typeName The name of the field declared type.
	 * @param fieldName The name of the field to which the comment is added.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the code template is undefined or empty. The returned content is unformatted and is not indented.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */	
	public static String getFieldComment(IJavaScriptUnit cu, String typeName, String fieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getFieldComment(cu, typeName, fieldName, lineDelimiter);
	}	
	
	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param decl The FunctionDeclaration AST node that will be added as new
	 * method. The node does not need to exist in an AST (no parent needed) and does not need to resolve.
	 * See {@link org.eclipse.wst.jsdt.core.dom.AST#newFunctionDeclaration()} for how to create such a node.
	 * @param overridden The binding of the method to which to add an "@see" link or 
	 * <code>null</code> if no link should be created.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the generated method comment or <code>null</code> if the
	 * code template is empty. The returned content is unformatted and not indented (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 */
	public static String getMethodComment(IJavaScriptUnit cu, String declaringTypeName, FunctionDeclaration decl, IFunctionBinding overridden, String lineDelimiter) throws CoreException {
		if (overridden != null) {
			overridden= overridden.getMethodDeclaration();
			String declaringClassQualifiedName= overridden.getDeclaringClass().getQualifiedName();
			String linkToMethodName= overridden.getName();
			String[] parameterTypesQualifiedNames= StubUtility.getParameterTypeNamesForSeeTag(overridden);
			return StubUtility.getMethodComment(cu, declaringTypeName, decl, overridden.isDeprecated(), linkToMethodName, declaringClassQualifiedName, parameterTypesQualifiedNames, false, lineDelimiter);
		} else {
			return StubUtility.getMethodComment(cu, declaringTypeName, decl, false, null, null, null, false, lineDelimiter);
		}
	}

	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * <p>Exception types and return type are in signature notation. e.g. a source method declared as <code>public void foo(String text, int length)</code>
	 * would return the array <code>{"QString;","I"}</code> as parameter types. See {@link org.eclipse.wst.jsdt.core.Signature}.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param methodName Name of the method.
	 * @param paramNames Names of the parameters for the method.
	 * @param excTypeSig Thrown exceptions (Signature notation).
	 * @param retTypeSig Return type (Signature notation) or <code>null</code>
	 * for constructors.
	 * @param overridden The method that will be overridden by the created method or
	 * <code>null</code> for non-overriding methods. If not <code>null</code>, the method must exist.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the constructed comment or <code>null</code> if
	 * the comment code template is empty. The returned content is unformatted and not indented (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 */
	public static String getMethodComment(IJavaScriptUnit cu, String declaringTypeName, String methodName, String[] paramNames, String[] excTypeSig, String retTypeSig, IFunction overridden, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodComment(cu, declaringTypeName, methodName, paramNames, excTypeSig, retTypeSig, overridden, false, lineDelimiter);
	}
		
	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param method The method to be documented. The method must exist.
	 * @param overridden The method that will be overridden by the created method or
	 * <code>null</code> for non-overriding methods. If not <code>null</code>, the method must exist.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the constructed comment or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 */
	public static String getMethodComment(IFunction method, IFunction overridden, String lineDelimiter) throws CoreException {
		String retType= method.getReturnType();
		String[] paramNames= method.getParameterNames();
		
		String typeName = (method.getDeclaringType()!=null) ? method.getDeclaringType().getElementName() : ""; //$NON-NLS-1$
		return StubUtility.getMethodComment(method.getJavaScriptUnit(), typeName,
			method.getElementName(), paramNames, new String[0], retType, overridden, false, lineDelimiter);
	}
	
	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.

	 * @param decl The FunctionDeclaration AST node that will be added as new
	 * method. The node does not need to exist in an AST (no parent needed) and does not need to resolve.
	 * See {@link org.eclipse.wst.jsdt.core.dom.AST#newFunctionDeclaration()} for how to create such a node.
	 * @param isDeprecated If set, the method is deprecated
	 * @param overriddenMethodName If a method is overridden, the simple name of the overridden method, or <code>null</code> if no method is overridden.
	 * @param overriddenMethodDeclaringTypeName If a method is overridden, the fully qualified type name of the overridden method's declaring type,
	 * or <code>null</code> if no method is overridden.
	 * @param overriddenMethodParameterTypeNames If a method is overridden, the fully qualified parameter type names of the overridden method,
	 * or <code>null</code> if no method is overridden.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the constructed comment or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */

	public static String getMethodComment(IJavaScriptUnit cu, String declaringTypeName, FunctionDeclaration decl, boolean isDeprecated, String overriddenMethodName, String overriddenMethodDeclaringTypeName, String[] overriddenMethodParameterTypeNames, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodComment(cu, declaringTypeName, decl, isDeprecated, overriddenMethodName, overriddenMethodDeclaringTypeName, overriddenMethodParameterTypeNames, false, lineDelimiter);
	}

	/**
	 * Returns the content of the body for a method or constructor using the method body templates.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param methodName Name of the method.
	 * @param isConstructor Defines if the created body is for a constructor.
	 * @param bodyStatement The code to be entered at the place of the variable ${body_statement}. 
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the constructed body content or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 */	
	public static String getMethodBodyContent(IJavaScriptUnit cu, String declaringTypeName, String methodName, boolean isConstructor, String bodyStatement, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodBodyContent(isConstructor, cu.getJavaScriptProject(), declaringTypeName, methodName, bodyStatement, lineDelimiter);
	}
	
	/**
	 * Returns the content of body for a getter method using the getter method body template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param methodName The name of the getter method.
	 * @param fieldName The name of the field to get in the getter method, corresponding to the template variable for ${field}. 
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the constructed body content or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */	
	public static String getGetterMethodBodyContent(IJavaScriptUnit cu, String declaringTypeName, String methodName, String fieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getGetterMethodBodyContent(cu.getJavaScriptProject(), declaringTypeName, methodName, fieldName, lineDelimiter);
	}
	
	/**
	 * Returns the content of body for a setter method using the setter method body template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param methodName The name of the setter method.
	 * @param fieldName The name of the field to be set in the setter method, corresponding to the template variable for ${field}.
	 * @param paramName The name of the parameter passed to the setter method, corresponding to the template variable for $(param).
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the constructed body content or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */	
	public static String getSetterMethodBodyContent(IJavaScriptUnit cu, String declaringTypeName, String methodName, String fieldName, String paramName, String lineDelimiter) throws CoreException {
		return StubUtility.getSetterMethodBodyContent(cu.getJavaScriptProject(), declaringTypeName, methodName, fieldName, paramName, lineDelimiter);
	}
	
	/**
	 * Returns the comment for a getter method using the getter comment template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param methodName Name of the method.
	 * @param fieldName Name of the field to get.
	 * @param fieldType The type of the field to get.
	 * @param bareFieldName The field name without prefix or suffix.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the generated getter comment or <code>null</code> if the
	 * code template is empty. The returned content is not indented.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */
	public static String getGetterComment(IJavaScriptUnit cu, String declaringTypeName, String methodName, String fieldName, String fieldType, String bareFieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getGetterComment(cu, declaringTypeName, methodName, fieldName, fieldType, bareFieldName, lineDelimiter);
	}
	
	/**
	 * Returns the comment for a setter method using the setter method body template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.wst.jsdt.core.IType#getTypeQualifiedName(char)}.
	 * @param methodName Name of the method.
	 * @param fieldName Name of the field that is set.
	 * @param fieldType The type of the field that is to set.
	 * @param paramName The name of the parameter that used to set.
	 * @param bareFieldName The field name without prefix or suffix.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the generated setter comment or <code>null</code> if the
	 * code template is empty. The returned comment is not indented.
	 * @throws CoreException Thrown when the evaluation of the code template fails.
	 * 
	 */
	public static String getSetterComment(IJavaScriptUnit cu, String declaringTypeName, String methodName, String fieldName, String fieldType, String paramName, String bareFieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getSetterComment(cu, declaringTypeName, methodName, fieldName, fieldType, paramName, bareFieldName, lineDelimiter);
	}		
}
