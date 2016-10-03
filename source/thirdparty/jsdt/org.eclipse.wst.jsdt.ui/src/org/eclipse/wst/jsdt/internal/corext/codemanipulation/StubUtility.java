/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.NamingConventions;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.formatter.IndentManipulation;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ProjectTemplateStore;
import org.eclipse.wst.jsdt.ui.CodeStyleConfiguration;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class StubUtility {
	
	private static final String[] EMPTY= new String[0];
	
	private static final Set VALID_TYPE_BODY_TEMPLATES;
	static {
		VALID_TYPE_BODY_TEMPLATES= new HashSet();
		VALID_TYPE_BODY_TEMPLATES.add(CodeTemplateContextType.CLASSBODY_ID);
	}
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getMethodBodyContent(boolean isConstructor, IJavaScriptProject project, String destTypeName, String methodName, String bodyStatement, String lineDelimiter) throws CoreException {
		String templateName= isConstructor ? CodeTemplateContextType.CONSTRUCTORSTUB_ID : CodeTemplateContextType.METHODSTUB_ID;
		Template template= getCodeTemplate(templateName, project);
		if (template == null) {
			return bodyStatement;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.BODY_STATEMENT, bodyStatement);
		String str= evaluateTemplate(context, template, new String[] { CodeTemplateContextType.BODY_STATEMENT });
		if (str == null && !Strings.containsOnlyWhitespaces(bodyStatement)) {
			return bodyStatement;
		}
		return str;
	}
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getGetterMethodBodyContent(IJavaScriptProject project, String destTypeName, String methodName, String fieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.GETTERSTUB_ID;
		Template template= getCodeTemplate(templateName, project);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		
		return evaluateTemplate(context, template);
	}
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getSetterMethodBodyContent(IJavaScriptProject project, String destTypeName, String methodName, String fieldName, String paramName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.SETTERSTUB_ID;
		Template template= getCodeTemplate(templateName, project);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, fieldName);
		context.setVariable(CodeTemplateContextType.PARAM, paramName);
		
		return evaluateTemplate(context, template);
	}
	
	public static String getCatchBodyContent(IJavaScriptUnit cu, String exceptionType, String variableName, ASTNode locationInAST, String lineDelimiter) throws CoreException {
		String enclosingType= ""; //$NON-NLS-1$
		String enclosingMethod= ""; //$NON-NLS-1$
			
		if (locationInAST != null) {
			FunctionDeclaration parentMethod= ASTResolving.findParentMethodDeclaration(locationInAST);
			if (parentMethod != null) {
				enclosingMethod= parentMethod.getName().getIdentifier();
				locationInAST= parentMethod;
			}
			ASTNode parentType= ASTResolving.findParentType(locationInAST);
			if (parentType instanceof AbstractTypeDeclaration) {
				enclosingType= ((AbstractTypeDeclaration) parentType).getName().getIdentifier();
			}
		}
		return getCatchBodyContent(cu, exceptionType, variableName, enclosingType, enclosingMethod, lineDelimiter);
	}
	
	
	public static String getCatchBodyContent(IJavaScriptUnit cu, String exceptionType, String variableName, String enclosingType, String enclosingMethod, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}

		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, enclosingType);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, enclosingMethod); 
		context.setVariable(CodeTemplateContextType.EXCEPTION_TYPE, exceptionType);
		context.setVariable(CodeTemplateContextType.EXCEPTION_VAR, variableName); 
		return evaluateTemplate(context, template);
	}
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getCompilationUnitContent(IJavaScriptUnit, String, String, String, String)
	 */	
	public static String getCompilationUnitContent(IJavaScriptUnit cu, String fileComment, String typeComment, String typeContent, String lineDelimiter) throws CoreException {
		IPackageFragment pack= (IPackageFragment) cu.getParent();
		String packDecl= pack.isDefaultPackage() ? "" : "package " + pack.getElementName() + ';'; //$NON-NLS-1$ //$NON-NLS-2$
		return getCompilationUnitContent(cu, packDecl, fileComment, typeComment, typeContent, lineDelimiter);
	}
	
	public static String getCompilationUnitContent(IJavaScriptUnit cu, String packDecl, String fileComment, String typeComment, String typeContent, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}
		
		IJavaScriptProject project= cu.getJavaScriptProject();
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.PACKAGE_DECLARATION, packDecl);
		context.setVariable(CodeTemplateContextType.TYPE_COMMENT, typeComment != null ? typeComment : ""); //$NON-NLS-1$
		context.setVariable(CodeTemplateContextType.FILE_COMMENT, fileComment != null ? fileComment : ""); //$NON-NLS-1$
		context.setVariable(CodeTemplateContextType.TYPE_DECLARATION, typeContent);
		context.setVariable(CodeTemplateContextType.TYPENAME, JavaScriptCore.removeJavaScriptLikeExtension(cu.getElementName()));
		
		String[] fullLine= { CodeTemplateContextType.PACKAGE_DECLARATION, CodeTemplateContextType.FILE_COMMENT, CodeTemplateContextType.TYPE_COMMENT };
		return evaluateTemplate(context, template, fullLine);
	}
	
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getFileComment(IJavaScriptUnit, String)
	 */	
	public static String getFileComment(IJavaScriptUnit cu, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}
		
		IJavaScriptProject project= cu.getJavaScriptProject();
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.TYPENAME, JavaScriptCore.removeJavaScriptLikeExtension(cu.getElementName()));
		return evaluateTemplate(context, template);
	}	

	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getTypeComment(IJavaScriptUnit, String, String[], String)
	 */		
	public static String getTypeComment(IJavaScriptUnit cu, String typeQualifiedName, String lineDelim) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelim);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, Signature.getQualifier(typeQualifiedName));
		context.setVariable(CodeTemplateContextType.TYPENAME, Signature.getSimpleName(typeQualifiedName));

		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		
		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
		
		IDocument document= new Document(str);
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(document, tagOffsets[i], position.getLength(), EMPTY, EMPTY, null, false, lineDelim);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		return document.get();
	}

	/*
	 * Returns the parameters type names used in see tags. Currently, these are always fully qualified.
	 */
	public static String[] getParameterTypeNamesForSeeTag(IFunctionBinding binding) {
		ITypeBinding[] typeBindings= binding.getParameterTypes();
		String[] result= new String[typeBindings.length];
		for (int i= 0; i < result.length; i++) {
			ITypeBinding curr= typeBindings[i];
			curr= curr.getTypeDeclaration(); // no parameterized types
			result[i]= curr.getQualifiedName();
		}
		return result;
	}
	
	/*
	 * Returns the parameters type names used in see tags. Currently, these are always fully qualified.
	 */
	private static String[] getParameterTypeNamesForSeeTag(IFunction overridden) {
		try {
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(overridden.getJavaScriptProject());
			IBinding[] bindings= parser.createBindings(new IJavaScriptElement[] { overridden }, null);
			if (bindings.length == 1 && bindings[0] instanceof IFunctionBinding) {
				return getParameterTypeNamesForSeeTag((IFunctionBinding) bindings[0]);
			}
		} catch (IllegalStateException e) {
			// method does not exist
		}
		// fall back code. Not good for generic methods!
		String[] paramTypes= overridden.getParameterTypes();
		String[] paramTypeNames= new String[paramTypes.length];
		for (int i= 0; i < paramTypes.length; i++) {
			paramTypeNames[i]= Signature.toString(paramTypes[i]);
		}
		return paramTypeNames;
	}

	private static String getSeeTag(String declaringClassQualifiedName, String methodName, String[] parameterTypesQualifiedNames) {
		StringBuffer buf= new StringBuffer();
		buf.append("@see "); //$NON-NLS-1$
		buf.append(declaringClassQualifiedName);
		buf.append('#'); 
		buf.append(methodName);
		buf.append('(');
		for (int i= 0; i < parameterTypesQualifiedNames.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(parameterTypesQualifiedNames[i]);
		}
		buf.append(')');
		return buf.toString();
	}

	/**
     * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getTypeBody(String, IJavaScriptUnit, String, String)
	 */		
	public static String getTypeBody(String templateID, IJavaScriptUnit cu, String typeName, String lineDelim) throws CoreException {
		if ( !VALID_TYPE_BODY_TEMPLATES.contains(templateID)) {
			throw new IllegalArgumentException("Invalid code template ID: " + templateID);  //$NON-NLS-1$
		}
		
		Template template= getCodeTemplate(templateID, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelim);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.TYPENAME, typeName);

		return evaluateTemplate(context, template);
	}
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getMethodComment(IJavaScriptUnit, String, String, String[], String[], String, String[], IFunction, String)
	 */
	public static String getMethodComment(IJavaScriptUnit cu, String typeName, String methodName, String[] paramNames, String[] excTypeSig, String retTypeSig, IFunction target, boolean delegate, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.METHODCOMMENT_ID;
		if (retTypeSig == null) {
			templateName= CodeTemplateContextType.CONSTRUCTORCOMMENT_ID;
		} else if (target != null) {
			if (delegate)
				templateName= CodeTemplateContextType.DELEGATECOMMENT_ID;
			else
				templateName= CodeTemplateContextType.OVERRIDECOMMENT_ID;
		}
		Template template= getCodeTemplate(templateName, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
				
		if (retTypeSig != null) {
			context.setVariable(CodeTemplateContextType.RETURN_TYPE, Signature.toString(retTypeSig));
		}
		if (target != null && target.getDeclaringType()!=null) {
			String targetTypeName= target.getDeclaringType().getFullyQualifiedName('.');
			String[] targetParamTypeNames= getParameterTypeNamesForSeeTag(target);
			if (delegate)
				context.setVariable(CodeTemplateContextType.SEE_TO_TARGET_TAG, getSeeTag(targetTypeName, methodName, targetParamTypeNames));
			else
				context.setVariable(CodeTemplateContextType.SEE_TO_OVERRIDDEN_TAG, getSeeTag(targetTypeName, methodName, targetParamTypeNames));
		}
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null) {
			return null;
		}
		
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
			
		IDocument document= new Document(str);
		String[] exceptionNames= new String[excTypeSig.length];
		for (int i= 0; i < excTypeSig.length; i++) {
			exceptionNames[i]= Signature.toString(excTypeSig[i]);
		}
		String returnType= retTypeSig != null ? Signature.toString(retTypeSig) : null;
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(document, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, false, lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		return document.get();
	}
	
	// remove lines for empty variables
	private static String fixEmptyVariables(TemplateBuffer buffer, String[] variables) throws MalformedTreeException, BadLocationException {
		IDocument doc= new Document(buffer.getString());
		int nLines= doc.getNumberOfLines();
		MultiTextEdit edit= new MultiTextEdit();
		HashSet removedLines= new HashSet();
		for (int i= 0; i < variables.length; i++) {
			TemplateVariable position= findVariable(buffer, variables[i]); // look if Javadoc tags have to be added
			if (position == null || position.getLength() > 0) {
				continue;
			}
			int[] offsets= position.getOffsets();
			for (int k= 0; k < offsets.length; k++) {
				int line= doc.getLineOfOffset(offsets[k]);
				IRegion lineInfo= doc.getLineInformation(line);
				int offset= lineInfo.getOffset();
				String str= doc.get(offset, lineInfo.getLength());
				if (Strings.containsOnlyWhitespaces(str) && nLines > line + 1 && removedLines.add(Integer.valueOf(line))) {
					int nextStart= doc.getLineOffset(line + 1);
					edit.addChild(new DeleteEdit(offset, nextStart - offset));
				}
			}
		}
		edit.apply(doc, 0);
		return doc.get();
	}
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getFieldComment(IJavaScriptUnit cu, String typeName, String fieldName, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		
		return evaluateTemplate(context, template);
	}	
	
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getSetterComment(IJavaScriptUnit, String, String, String, String, String, String, String)
	 */
	public static String getSetterComment(IJavaScriptUnit cu, String typeName, String methodName, String fieldName, String fieldType, String paramName, String bareFieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.SETTERCOMMENT_ID;
		Template template= getCodeTemplate(templateName, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}
		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, fieldType);
		context.setVariable(CodeTemplateContextType.BARE_FIELD_NAME, bareFieldName);
		context.setVariable(CodeTemplateContextType.PARAM, paramName);

		return evaluateTemplate(context, template);
	}	
	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getGetterComment(IJavaScriptUnit, String, String, String, String, String, String)
	 */
	public static String getGetterComment(IJavaScriptUnit cu, String typeName, String methodName, String fieldName, String fieldType, String bareFieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.GETTERCOMMENT_ID;
		Template template= getCodeTemplate(templateName, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, fieldType);
		context.setVariable(CodeTemplateContextType.BARE_FIELD_NAME, bareFieldName);

		return evaluateTemplate(context, template);
	}
	
	private static String evaluateTemplate(CodeTemplateContext context, Template template) throws CoreException {
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null)
			return null;
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		return str;
	}
	
	private static String evaluateTemplate(CodeTemplateContext context, Template template, String[] fullLineVariables) throws CoreException {
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
			if (buffer == null)
				return null;
			String str= fixEmptyVariables(buffer, fullLineVariables);
			if (Strings.containsOnlyWhitespaces(str)) {
				return null;
			}
			return str;
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}

	
	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see org.eclipse.wst.jsdt.ui.CodeGeneration#getMethodComment(IJavaScriptUnit, String, FunctionDeclaration, boolean, String, String[], String)
	 */
	public static String getMethodComment(IJavaScriptUnit cu, String typeName, FunctionDeclaration decl, boolean isDeprecated, String targetName, String targetMethodDeclaringTypeName, String[] targetMethodParameterTypeNames, boolean delegate, String lineDelimiter) throws CoreException {
		if (typeName==null)
			typeName=""; //$NON-NLS-1$
		boolean needsTarget= targetMethodDeclaringTypeName != null && targetMethodParameterTypeNames != null;
		String templateName= CodeTemplateContextType.METHODCOMMENT_ID;
		if (decl.isConstructor()) {
			templateName= CodeTemplateContextType.CONSTRUCTORCOMMENT_ID;
		} else if (needsTarget) {
			if (delegate)
				templateName= CodeTemplateContextType.DELEGATECOMMENT_ID;
			else
				templateName= CodeTemplateContextType.OVERRIDECOMMENT_ID;
		}
		Template template= getCodeTemplate(templateName, cu.getJavaScriptProject());
		if (template == null) {
			return null;
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaScriptProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, decl.getName().getIdentifier());
		if (!decl.isConstructor() &&getReturnType(decl)!=null) {
			context.setVariable(CodeTemplateContextType.RETURN_TYPE, ASTNodes.asString(getReturnType(decl)));
		}
		if (needsTarget) {
			if (delegate)
				context.setVariable(CodeTemplateContextType.SEE_TO_TARGET_TAG, getSeeTag(targetMethodDeclaringTypeName, targetName, targetMethodParameterTypeNames));
			else
				context.setVariable(CodeTemplateContextType.SEE_TO_OVERRIDDEN_TAG, getSeeTag(targetMethodDeclaringTypeName, targetName, targetMethodParameterTypeNames));
		}
		
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null)
			return null;
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS);  // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
			
		IDocument textBuffer= new Document(str);
		List params= decl.parameters();
		String[] paramNames= new String[params.size()];
		for (int i= 0; i < paramNames.length; i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
			paramNames[i]= elem.getName().getIdentifier();
		}
		List exceptions= decl.thrownExceptions();
		String[] exceptionNames= new String[exceptions.size()];
		for (int i= 0; i < exceptionNames.length; i++) {
			exceptionNames[i]= ASTNodes.getSimpleNameIdentifier((Name) exceptions.get(i));
		}
		
		String returnType= null;
		if (!decl.isConstructor()) {
			returnType= ASTNodes.asString(getReturnType(decl));
		}
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(textBuffer, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, isDeprecated, lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		return textBuffer.get();
	}
	
	/**
	 * @deprecated Deprecated to avoid deprecated warnings
	 */
	private static ASTNode getReturnType(FunctionDeclaration decl) {
		// used from API, can't eliminate
		return (decl.getAST().apiLevel() == AST.JLS2) ? decl.getReturnType() : decl.getReturnType2();
	}
	
	
	private static TemplateVariable findVariable(TemplateBuffer buffer, String variable) {
		TemplateVariable[] positions= buffer.getVariables();
		for (int i= 0; i < positions.length; i++) {
			TemplateVariable curr= positions[i];
			if (variable.equals(curr.getType())) {
				return curr;
			}
		}
		return null;		
	}	
	
	private static void insertTag(IDocument textBuffer, int offset, int length, String[] paramNames, String[] exceptionNames, String returnType, boolean isDeprecated, String lineDelimiter) throws BadLocationException {
		IRegion region= textBuffer.getLineInformationOfOffset(offset);
		if (region == null) {
			return;
		}
		String lineStart= textBuffer.get(region.getOffset(), offset - region.getOffset());
		
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < paramNames.length; i++) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@param ").append(paramNames[i]); //$NON-NLS-1$
		}
		if (returnType != null && !returnType.equals("void")) { //$NON-NLS-1$
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}			
			buf.append("@returns"); //$NON-NLS-1$
			if(!returnType.equals("any")) { //$NON-NLS-1$
				buf.append(" {" + returnType + "}"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (exceptionNames != null) {
			for (int i= 0; i < exceptionNames.length; i++) {
				if (buf.length() > 0) {
					buf.append(lineDelimiter).append(lineStart);
				}
				buf.append("@throws ").append(exceptionNames[i]); //$NON-NLS-1$
			}
		}		
		if (isDeprecated) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@deprecated"); //$NON-NLS-1$
		}
		if (buf.length() == 0 && isAllCommentWhitespace(lineStart)) {
			int prevLine= textBuffer.getLineOfOffset(offset) -1;
			if (prevLine > 0) {
				IRegion prevRegion= textBuffer.getLineInformation(prevLine);
				int prevLineEnd= prevRegion.getOffset() + prevRegion.getLength();
				// clear full line
				textBuffer.replace(prevLineEnd, offset + length - prevLineEnd, ""); //$NON-NLS-1$
				return;
			}
		}
		textBuffer.replace(offset, length, buf.toString());
	}
	
	private static boolean isAllCommentWhitespace(String lineStart) {
		for (int i= 0; i < lineStart.length(); i++) {
			char ch= lineStart.charAt(i);
			if (!Character.isWhitespace(ch) && ch != '*') {
				return false;
			}
		}
		return true;
	}
		
	/**
	 * Returns the line delimiter which is used in the specified project.
	 * 
	 * @param project the java project, or <code>null</code>
	 * @return the used line delimiter
	 */
	public static String getLineDelimiterUsed(IJavaScriptProject project) {
		return getProjectLineDelimiter(project);
	}

	private static String getProjectLineDelimiter(IJavaScriptProject javaProject) {
		IProject project= null;
		if (javaProject != null)
			project= javaProject.getProject();
		
		String lineDelimiter= getLineDelimiterPreference(project);
		if (lineDelimiter != null)
			return lineDelimiter;
		
		return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static String getLineDelimiterPreference(IProject project) {
		IScopeContext[] scopeContext;
		if (project != null) {
			// project preference
			scopeContext= new IScopeContext[] { new ProjectScope(project) };
			String lineDelimiter= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineDelimiter != null)
				return lineDelimiter;
		}
		// workspace preference
		scopeContext= new IScopeContext[] { new InstanceScope() };
		String platformDefault= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, platformDefault, scopeContext);
	}
	
	/**
	 * Examines a string and returns the first line delimiter found.
	 */
	public static String getLineDelimiterUsed(IJavaScriptElement elem) {
		while (elem != null && !(elem instanceof IOpenable)) {
			elem= elem.getParent();
		}
		if (elem != null) {
			try {
				return ((IOpenable) elem).findRecommendedLineSeparator();
			} catch (JavaScriptModelException exception) {
				// Use project setting
			}
		}
		return getProjectLineDelimiter(null);
	}

	/**
	 * Evaluates the indentation used by a Java element. (in tabulators)
	 */	
	public static int getIndentUsed(IJavaScriptElement elem) throws JavaScriptModelException {
		if (elem instanceof ISourceReference) {
			IJavaScriptUnit cu= (IJavaScriptUnit) elem.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null) {
				IBuffer buf= cu.getBuffer();
				int offset= ((ISourceReference)elem).getSourceRange().getOffset();
				int i= offset;
				// find beginning of line
				while (i > 0 && !IndentManipulation.isLineDelimiterChar(buf.getChar(i - 1)) ){
					i--;
				}
				return Strings.computeIndentUnits(buf.getText(i, offset - i), elem.getJavaScriptProject());
			}
		}
		return 0;
	}
		
	/**
	 * Returns the element after the give element.
	 */
	public static IJavaScriptElement findNextSibling(IJavaScriptElement member) throws JavaScriptModelException {
		IJavaScriptElement parent= member.getParent();
		if (parent instanceof IParent) {
			IJavaScriptElement[] elements= ((IParent)parent).getChildren();
			for (int i= elements.length - 2; i >= 0 ; i--) {
				if (member.equals(elements[i])) {
					return elements[i+1];
				}
			}
		}
		return null;
	}
	
	public static String getTodoTaskTag(IJavaScriptProject project) {
		String markers= null;
		if (project == null) {
			markers= JavaScriptCore.getOption(JavaScriptCore.COMPILER_TASK_TAGS);
		} else {
			markers= project.getOption(JavaScriptCore.COMPILER_TASK_TAGS, true);
		}
		
		if (markers != null && markers.length() > 0) {
			int idx= markers.indexOf(',');
			if (idx == -1) {
				return markers;
			} else {
				return markers.substring(0, idx);
			}
		}
		return null;
	}
	
	private static String removeTypeArguments(String baseName) {
		int idx= baseName.indexOf('<');
		if (idx != -1) {
			return baseName.substring(0, idx);
		}
		return baseName;
	}
	
	
	// --------------------------- name suggestions --------------------------
	
	public static final int STATIC_FIELD= 1;
	public static final int INSTANCE_FIELD= 2;
	public static final int CONSTANT_FIELD= 3;
	public static final int PARAMETER= 4;
	public static final int LOCAL= 5;
	
	public static String[] getVariableNameSuggestions(int variableKind, IJavaScriptProject project, ITypeBinding expectedType, Expression assignedExpression, Collection excluded) {
		LinkedHashSet res= new LinkedHashSet(); // avoid duplicates but keep order

		if (assignedExpression != null) {
			String nameFromExpression= getBaseNameFromExpression(project, assignedExpression, variableKind);
			if (nameFromExpression != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromExpression, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}
		if (expectedType != null) {
			expectedType= Bindings.normalizeTypeBinding(expectedType);
			if (expectedType != null) {
				int dim= 0;
				if (expectedType.isArray()) {
					dim= expectedType.getDimensions();
					expectedType= expectedType.getElementType();
				}
				String typeName= expectedType.getQualifiedName();
				if (typeName.length() > 0) {
					String[] names= getVariableNameSuggestions(variableKind, project, typeName, dim, excluded, false);
					for (int i= 0; i < names.length; i++) {
						res.add(names[i]);
					}
				}
			}
		}
		if (assignedExpression != null) {
			// add at end, less important
			String nameFromParent= getBaseNameFromLocationInParent(project, assignedExpression);
			if (nameFromParent != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromParent, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}
		if (res.isEmpty()) {
			return getDefaultVariableNameSuggestions(variableKind, excluded);
		}
		return (String[]) res.toArray(new String[res.size()]);
	}
	
	public static String[] getVariableNameSuggestions(int variableKind, IJavaScriptProject project, Type expectedType, Expression assignedExpression, Collection excluded) {
		LinkedHashSet res= new LinkedHashSet(); // avoid duplicates but keep order

		if (assignedExpression != null) {
			String nameFromExpression= getBaseNameFromExpression(project, assignedExpression, variableKind);
			if (nameFromExpression != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromExpression, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}
		if (expectedType != null) {			
			int dim= 0;
			if (expectedType.isArrayType()) {
				ArrayType arrayType= (ArrayType) expectedType;
				dim= arrayType.getDimensions();
				expectedType= arrayType.getElementType();
			}

			String typeName= ASTNodes.asString(expectedType);
			
			if (typeName.length() > 0) {
				String[] names= getVariableNameSuggestions(variableKind, project, typeName, dim, excluded, false);
				for (int i= 0; i < names.length; i++) {
					res.add(names[i]);
				}
			}
		}
		if (assignedExpression != null) {
			// add at end, less important
			String nameFromParent= getBaseNameFromLocationInParent(project, assignedExpression);
			if (nameFromParent != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromParent, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}
		if (res.isEmpty()) {
			return getDefaultVariableNameSuggestions(variableKind, excluded);
		}
		return (String[]) res.toArray(new String[res.size()]);
	}
	
	private static String[] getDefaultVariableNameSuggestions(int variableKind, Collection excluded) {
		String prop= variableKind == CONSTANT_FIELD ? "X" : "x";  //$NON-NLS-1$//$NON-NLS-2$
		String name= prop;
		int i= 1;
		while (excluded.contains(name)) {
			name= prop + i++;
		}
		return new String[] { name };
	}
	
	/**
	 * Returns variable name suggestions for the given base name. This is a layer over the JDT.Core NamingConventions API to fix its shortcomings. JDT UI code should only use this
	 * API.
	 * @param variableKind Specifies what type the variable is: {@link #LOCAL}, {@link #PARAMETER}, {@link #STATIC_FIELD}, {@link #INSTANCE_FIELD} or {@link #CONSTANT_FIELD}.
 	 * @param project the current project
 	 * @param baseName the base name to make a suggestion on. the base name is expected to be a name without any pre- or suffixes in singular form. Type name are accepted as well.
 	 * @param dimensions if greater than 0, the resulting name will be in plural form
 	 * @param excluded a collection containing all excluded names or <code>null</code> if no names are excluded
 	 * @param evaluateDefault if set, the result is guaranteed to contain at least one result. If not, the result can be an empty array. 
	 * 
	 * @return returns the name suggestions sorted by relevance (best proposal first). If <code>evaluateDefault</code> is set to true, the returned array is never empty.
	 * If <code>evaluateDefault</code> is set to false, an empty array is returned if there is no good suggestion for the given base name.
	 */
	public static String[] getVariableNameSuggestions(int variableKind, IJavaScriptProject project, String baseName, int dimensions, Collection excluded, boolean evaluateDefault) {
		String name= workaround38111(baseName);
		name= removeTypeArguments(name);
		String packageName= new String(); // not used, so don't compute for now
		String[] result= null;
		
		switch (variableKind) {
			case CONSTANT_FIELD:
				result= getConstantSuggestions(project, packageName, name, dimensions, excluded);
				break;
			case STATIC_FIELD:
				result= sortByLength(NamingConventions.suggestFieldNames(project, packageName, name, dimensions, Flags.AccStatic, getExcludedArray(excluded)));
				break;
			case INSTANCE_FIELD:
				result= sortByLength(NamingConventions.suggestFieldNames(project, packageName, name, dimensions, 0, getExcludedArray(excluded)));
				break;
			case PARAMETER:
				result= sortByLength(NamingConventions.suggestArgumentNames(project, packageName, name, dimensions, getExcludedArray(excluded)));
				break;
			case LOCAL:
				result= sortByLength(NamingConventions.suggestLocalVariableNames(project, packageName, name, dimensions, getExcludedArray(excluded)));
				break;
		}
		if (evaluateDefault) {
			if (result.length == 0) {
				result= getDefaultVariableNameSuggestions(variableKind, excluded);
			}
		} else if (variableKind != CONSTANT_FIELD) {
			 // see 166464 API DCR: specify if naming convention should return default value or not
			String defaultValue= "NAME"; // default as chosen by jdt.core //$NON-NLS-1$
			if (!name.toUpperCase().endsWith(defaultValue) && result[0].toUpperCase().endsWith(defaultValue)) {
				return new String[0];
			}
		}
		return result;
	}
	
	private static String[] getExcludedArray(Collection excluded) {
		if (excluded == null) {
			return null;
		} else if (excluded instanceof ExcludedCollection) {
			return ((ExcludedCollection) excluded).getExcludedArray();
		}
		return (String[]) excluded.toArray(new String[excluded.size()]);
	}
	
	
	private static final String[] KNOWN_METHOD_NAME_PREFIXES= { "get", "is", "to"}; //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-1$
	
	
	private static void add(String[] names, Set result) {
		for (int i= 0; i < names.length; i++) {
			result.add(names[i]);
		}
	}
	
	private static String getBaseNameFromExpression(IJavaScriptProject project, Expression assignedExpression, int variableKind) {
		String name= null;

		if (assignedExpression instanceof Name) {
			Name simpleNode= (Name) assignedExpression;
			IBinding binding= simpleNode.resolveBinding();
			if (binding instanceof IVariableBinding)
				return removePrefixAndSuffixForVariable(project, (IVariableBinding) binding);
			
			return ASTNodes.getSimpleNameIdentifier(simpleNode);
		} else if (assignedExpression instanceof FunctionInvocation) {
			SimpleName name2 = ((FunctionInvocation) assignedExpression).getName();
			if (name2!=null)
				name= name2.getIdentifier();
		} else if (assignedExpression instanceof SuperMethodInvocation) {
			name= ((SuperMethodInvocation) assignedExpression).getName().getIdentifier();
		} else if (assignedExpression instanceof FieldAccess) {
			return ((FieldAccess) assignedExpression).getName().getIdentifier();
		} else if (variableKind == CONSTANT_FIELD && (assignedExpression instanceof StringLiteral || assignedExpression instanceof NumberLiteral)) {
			String string= assignedExpression instanceof StringLiteral ? ((StringLiteral) assignedExpression).getLiteralValue() : ((NumberLiteral) assignedExpression).getToken();
			StringBuffer res= new StringBuffer();
			boolean needsUnderscore= false;
			for (int i= 0; i < string.length(); i++) {
				char ch= string.charAt(i);
				if (Character.isJavaIdentifierPart(ch)) {
					if (res.length() == 0 && !Character.isJavaIdentifierStart(ch) || needsUnderscore) {
						res.append('_');
					}
					res.append(ch);
					needsUnderscore= false;
				} else {
					needsUnderscore= res.length() > 0;
				}
			}
			if (res.length() > 0) {
				return res.toString();
			}
		}
		if (name != null) {
			for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
				String curr= KNOWN_METHOD_NAME_PREFIXES[i];
				if (name.startsWith(curr)) {
					if (name.equals(curr)) {
						return null; // don't suggest 'get' as variable name
					} else if (Character.isUpperCase(name.charAt(curr.length()))) {
						return name.substring(curr.length());
					}
				}
			}
		}
		return name;
	}
	
	private static String getBaseNameFromLocationInParent(IJavaScriptProject project, Expression assignedExpression) {
		StructuralPropertyDescriptor location= assignedExpression.getLocationInParent();
		if (location == FunctionInvocation.ARGUMENTS_PROPERTY) {
			FunctionInvocation parent= (FunctionInvocation) assignedExpression.getParent();
			IFunctionBinding binding= parent.resolveMethodBinding();
			int index= parent.arguments().indexOf(assignedExpression);
			if (binding != null && index != -1) {
				return getParameterName(binding, index);
			}
		} else if (location == ClassInstanceCreation.ARGUMENTS_PROPERTY) {
			ClassInstanceCreation parent= (ClassInstanceCreation) assignedExpression.getParent();
			IFunctionBinding binding= parent.resolveConstructorBinding();
			int index= parent.arguments().indexOf(assignedExpression);
			if (binding != null && index != -1) {
				return getParameterName(binding, index);
			}
		} else if (location == SuperMethodInvocation.ARGUMENTS_PROPERTY) {
			SuperMethodInvocation parent= (SuperMethodInvocation) assignedExpression.getParent();
			IFunctionBinding binding= parent.resolveMethodBinding();
			int index= parent.arguments().indexOf(assignedExpression);
			if (binding != null && index != -1) {
				return getParameterName(binding, index);
			}
		} else if (location == ConstructorInvocation.ARGUMENTS_PROPERTY) {
			ConstructorInvocation parent= (ConstructorInvocation) assignedExpression.getParent();
			IFunctionBinding binding= parent.resolveConstructorBinding();
			int index= parent.arguments().indexOf(assignedExpression);
			if (binding != null && index != -1) {
				return getParameterName(binding, index);
			}
		} else if (location == SuperConstructorInvocation.ARGUMENTS_PROPERTY) {
			SuperConstructorInvocation parent= (SuperConstructorInvocation) assignedExpression.getParent();
			IFunctionBinding binding= parent.resolveConstructorBinding();
			int index= parent.arguments().indexOf(assignedExpression);
			if (binding != null && index != -1) {
				return getParameterName(binding, index);
			}
		}
		return null;
	}
	
	private static String getParameterName(IFunctionBinding binding, int index) {
		try {
			IJavaScriptElement javaElement= binding.getJavaElement();
			if (javaElement instanceof IFunction) {
				IFunction method= (IFunction) javaElement;
				if (method.getOpenable().getBuffer() != null) { // avoid dummy names and lookup from Javadoc
					String[] parameterNames= method.getParameterNames();
					if (index < parameterNames.length) {
						return NamingConventions.removePrefixAndSuffixForArgumentName(method.getJavaScriptProject(), parameterNames[index]);
					}
				}
			}
		} catch (JavaScriptModelException e) {
			// ignore
		}
		return null;
	}
	
	public static String[] getArgumentNameSuggestions(IType type,IJavaScriptUnit compUnit, String[] excluded) {
		String baseName= (type!=null)?JavaModelUtil.getFullyQualifiedName(type) : compUnit.getElementName();
		return getVariableNameSuggestions(PARAMETER, compUnit.getJavaScriptProject(),baseName, 0, new ExcludedCollection(excluded), true);
	}
	
	public static String[] getArgumentNameSuggestions(IType type, String[] excluded) {
		return getVariableNameSuggestions(PARAMETER, type.getJavaScriptProject(), JavaModelUtil.getFullyQualifiedName(type), 0, new ExcludedCollection(excluded), true);
	}
	
	public static String[] getArgumentNameSuggestions(IJavaScriptProject project, Type type, String[] excluded) {
		int dim= 0;
		if (type.isArrayType()) {
			ArrayType arrayType= (ArrayType) type;
			dim= arrayType.getDimensions();
			type= arrayType.getElementType();
		}

		return getVariableNameSuggestions(PARAMETER, project, ASTNodes.asString(type), dim, new ExcludedCollection(excluded), true);
	}
	
	public static String[] getArgumentNameSuggestions(IJavaScriptProject project, ITypeBinding binding, String[] excluded) {
		return getVariableNameSuggestions(PARAMETER, project, binding, null, new ExcludedCollection(excluded));
	}
		
	public static String[] getArgumentNameSuggestions(IJavaScriptProject project, String baseName, int dimensions, String[] excluded) {
		return getVariableNameSuggestions(PARAMETER, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}
	
	public static String[] getFieldNameSuggestions(IType type, int fieldModifiers, String[] excluded) {		
		return getFieldNameSuggestions(type.getJavaScriptProject(), JavaModelUtil.getFullyQualifiedName(type), 0, fieldModifiers, excluded);
	}
		 
	public static String[] getFieldNameSuggestions(IJavaScriptProject project, String baseName, int dimensions, int modifiers, String[] excluded) {
		if (Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(STATIC_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		}
		return getVariableNameSuggestions(INSTANCE_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}

	private static String[] getConstantSuggestions(IJavaScriptProject project, String packageName, String typeName, int dimensions, Collection excluded) {
		//TODO: workaround JDT/Core bug 85946
		
		String string= Signature.getSimpleName(typeName);

		StringBuffer buf= new StringBuffer();
		boolean wasUpperCase= true;
		for (int i= 0; i < string.length() ; i++) {
			char ch= string.charAt(i);
			if (Character.isUpperCase(ch)) {
				if (!wasUpperCase) {
					buf.append('_');
				}
				buf.append(ch);
			} else {
				buf.append(Character.toUpperCase(ch));
				wasUpperCase= ch == '_'; // avoid duplicate underscores
			}
		}
		ArrayList res= new ArrayList();
		String sourceLevel= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		String complianceLevel= project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
		
		boolean nameStarts= true;
		for (int i= 0; i < buf.length(); i++) {
			if (nameStarts) {
				String prop= buf.substring(i);
				if (!excluded.contains(prop) && JavaScriptConventions.validateFieldName(prop, sourceLevel, complianceLevel).isOK()) {
					res.add(prop);
				}
			}
			char ch= buf.charAt(i);
			nameStarts= ch == '_';
		}
		return (String[]) res.toArray(new String[res.size()]);
	}
	
	private static String getCamelCaseFromUpper(String string) {
		StringBuffer result= new StringBuffer();
		boolean lastWasUnderscore= false;
		for (int i= 0; i < string.length(); i++) {
			char ch= string.charAt(i);
			if (Character.isUpperCase(ch)) {
				if (!lastWasUnderscore) {
					ch= Character.toLowerCase(ch);
				}
				result.append(ch);
				lastWasUnderscore= false;
			} else if (ch == '_') {
				lastWasUnderscore= true;
			} else {
				return string; // abort
			}
		}
		return result.toString();
	}
	
	public static String[] getLocalNameSuggestions(IJavaScriptProject project, String baseName, int dimensions, String[] excluded) {
		return getVariableNameSuggestions(LOCAL, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}
	
	private static String[] sortByLength(String[] proposals) {
		Arrays.sort(proposals, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((String) o2).length() - ((String) o1).length();
			}
		});
		return proposals;
	}
	
	private static String workaround38111(String baseName) {
		if (BASE_TYPES.contains(baseName))
			return baseName;
		return Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
	}
	
	private static final List BASE_TYPES= Arrays.asList(
			new String[] {"boolean", "byte", "char", "double", "float", "int", "long", "short"});  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	public static String suggestArgumentName(IJavaScriptProject project, String baseName, String[] excluded) {
		return suggestVariableName(PARAMETER, project, baseName, 0, excluded);
	}
	
	private static String suggestVariableName(int varKind, IJavaScriptProject project, String baseName, int dimension, String[] excluded) {
		return getVariableNameSuggestions(varKind, project, baseName, dimension, new ExcludedCollection(excluded), true)[0];
	}
	
	
	public static String[][] suggestArgumentNamesWithProposals(IJavaScriptProject project, String[] paramNames) {
		String[][] newNames= new String[paramNames.length][];
		ArrayList takenNames= new ArrayList();
		
		// Ensure that the code generation preferences are respected
		for (int i= 0; i < paramNames.length; i++) {
			String curr= paramNames[i];
			String baseName= NamingConventions.removePrefixAndSuffixForArgumentName(project, curr);

			String[] proposedNames= getVariableNameSuggestions(PARAMETER, project, curr, 0, takenNames, true);
			if (!curr.equals(baseName)) {
				// make the existing name to favourite
				LinkedHashSet updatedNames= new LinkedHashSet();
				updatedNames.add(curr);
				for (int k= 0; k < proposedNames.length; k++) {
					updatedNames.add(proposedNames[k]);
				}
				proposedNames= (String[]) updatedNames.toArray(new String[updatedNames.size()]);
			}
			newNames[i]= proposedNames;
			takenNames.add(proposedNames[0]);
		}
		return newNames;
	}
	
	public static String[][] suggestArgumentNamesWithProposals(IJavaScriptProject project, IFunctionBinding binding) {
		int nParams= binding.getParameterTypes().length;
		if (nParams > 0) {
			try {
				IFunction method= (IFunction) binding.getMethodDeclaration().getJavaElement();
				if (method != null) {
					return suggestArgumentNamesWithProposals(project, method.getParameterNames());
				}
			} catch (JavaScriptModelException e) {
				// ignore
			}
		}
		String[][] names= new String[nParams][];
		for (int i= 0; i < names.length; i++) {
			names[i]= new String[] { "arg" + i }; //$NON-NLS-1$
		}
		return names;
	}
	
	
	public static String[] suggestArgumentNames(IJavaScriptProject project, IFunctionBinding binding) {
		int nParams= binding.getParameterTypes().length;

		if (nParams > 0) {
			try {
				IFunction method= (IFunction) binding.getMethodDeclaration().getJavaElement();
				if (method != null) {
					String[] paramNames= method.getParameterNames();
					String[] namesArray= new String[0];
					ArrayList newNames= new ArrayList(paramNames.length);
					// Ensure that the code generation preferences are respected
					for (int i= 0; i < paramNames.length; i++) {
						String curr= paramNames[i];
						String baseName= NamingConventions.removePrefixAndSuffixForArgumentName(project, curr);
						if (!curr.equals(baseName)) {
							// make the existing name the favourite
							newNames.add(curr);
						} else {
							newNames.add(suggestArgumentName(project, curr, namesArray));
						}
						namesArray= (String[]) newNames.toArray(new String[newNames.size()]);
					}
					return namesArray;
				}
			} catch (JavaScriptModelException e) {
				// ignore
			}
		}
		String[] names= new String[nParams];
		for (int i= 0; i < names.length; i++) {
			names[i]= "arg" + i; //$NON-NLS-1$
		}
		return names;
	}
	
	public static String removePrefixAndSuffixForVariable(IJavaScriptProject project, IVariableBinding binding) {
		if (binding.isField()) {
			if (Modifier.isStatic(binding.getModifiers()) && Modifier.isFinal(binding.getModifiers())) {
				return getCamelCaseFromUpper(binding.getName());
			} else {
				return NamingConventions.removePrefixAndSuffixForFieldName(project, binding.getName(), binding.getModifiers());
			}
		} else if (binding.isParameter()) {
			return NamingConventions.removePrefixAndSuffixForArgumentName(project, binding.getName());
		} else {
			return NamingConventions.removePrefixAndSuffixForLocalVariableName(project, binding.getName());
		}
	}
	
    private static class ExcludedCollection extends AbstractList {
		private String[] fExcluded;
		public ExcludedCollection(String[] excluded) {
			fExcluded = excluded;
		}
		public String[] getExcludedArray() {
			return fExcluded;
		}
		public int size() {
			return fExcluded.length;
		}
		public Object get(int index) {
			return fExcluded[index];
		}
		public int indexOf(Object o) {
			if (o instanceof String) {
				for (int i= 0; i < fExcluded.length; i++) {
			         if (o.equals(fExcluded[i]))
			             return i;
				}
			}
			return -1;
		}
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}
    }
	
	
	public static boolean hasFieldName(IJavaScriptProject project, String name) {
		String prefixes= project.getOption(JavaScriptCore.CODEASSIST_FIELD_PREFIXES, true);
		String suffixes= project.getOption(JavaScriptCore.CODEASSIST_FIELD_SUFFIXES, true);
		String staticPrefixes= project.getOption(JavaScriptCore.CODEASSIST_STATIC_FIELD_PREFIXES, true);
		String staticSuffixes= project.getOption(JavaScriptCore.CODEASSIST_STATIC_FIELD_SUFFIXES, true);
		
		
		return hasPrefixOrSuffix(prefixes, suffixes, name) 
			|| hasPrefixOrSuffix(staticPrefixes, staticSuffixes, name);
	}
	
	public static boolean hasParameterName(IJavaScriptProject project, String name) {
		String prefixes= project.getOption(JavaScriptCore.CODEASSIST_ARGUMENT_PREFIXES, true);
		String suffixes= project.getOption(JavaScriptCore.CODEASSIST_ARGUMENT_SUFFIXES, true);
		return hasPrefixOrSuffix(prefixes, suffixes, name);
	}
	
	public static boolean hasLocalVariableName(IJavaScriptProject project, String name) {
		String prefixes= project.getOption(JavaScriptCore.CODEASSIST_LOCAL_PREFIXES, true);
		String suffixes= project.getOption(JavaScriptCore.CODEASSIST_LOCAL_SUFFIXES, true);
		return hasPrefixOrSuffix(prefixes, suffixes, name);
	}
	
	public static boolean hasConstantName(String name) {
		return Character.isUpperCase(name.charAt(0));
	}
	
	
	private static boolean hasPrefixOrSuffix(String prefixes, String suffixes, String name) {
		final String listSeparartor= ","; //$NON-NLS-1$

		StringTokenizer tok= new StringTokenizer(prefixes, listSeparartor);
		while (tok.hasMoreTokens()) {
			String curr= tok.nextToken();
			if (name.startsWith(curr)) {
				return true;
			}
		}

		tok= new StringTokenizer(suffixes, listSeparartor);
		while (tok.hasMoreTokens()) {
			String curr= tok.nextToken();
			if (name.endsWith(curr)) {
				return true;
			}
		}
		return false;
	}
	
	// -------------------- preference access -----------------------
	
	public static boolean useThisForFieldAccess(IJavaScriptProject project) {
		return Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_KEYWORD_THIS, project)).booleanValue(); 
	}
	
	public static boolean useIsForBooleanGetters(IJavaScriptProject project) {
		return Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, project)).booleanValue(); 
	}
	
	public static String getExceptionVariableName(IJavaScriptProject project) {
		return PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_EXCEPTION_VAR_NAME, project); 
	}
	
	public static boolean doAddComments(IJavaScriptProject project) {
		return Boolean.valueOf(PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_ADD_COMMENTS, project)).booleanValue(); 
	}
	
	public static void setCodeTemplate(String templateId, String pattern, IJavaScriptProject project) {
		TemplateStore codeTemplateStore= JavaScriptPlugin.getDefault().getCodeTemplateStore();
		TemplatePersistenceData data= codeTemplateStore.getTemplateData(templateId);
		Template orig= data.getTemplate();
		Template copy= new Template(orig.getName(), orig.getDescription(), orig.getContextTypeId(), pattern, true);
		data.setTemplate(copy);
	}
	
	private static Template getCodeTemplate(String id, IJavaScriptProject project) {
		if (project == null)
			return JavaScriptPlugin.getDefault().getCodeTemplateStore().findTemplateById(id);
		ProjectTemplateStore projectStore= new ProjectTemplateStore(project.getProject());
		try {
			projectStore.load();
		} catch (IOException e) {
			JavaScriptPlugin.log(e);
		}
		return projectStore.findTemplateById(id);
	}
	

	public static ImportRewrite createImportRewrite(IJavaScriptUnit cu, boolean restoreExistingImports) throws JavaScriptModelException {
		return CodeStyleConfiguration.createImportRewrite(cu, restoreExistingImports);
	}
	
	public static ImportRewrite createImportRewrite(JavaScriptUnit astRoot, boolean restoreExistingImports) {
		return CodeStyleConfiguration.createImportRewrite(astRoot, restoreExistingImports);
	}
	
}
