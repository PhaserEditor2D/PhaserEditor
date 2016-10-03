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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.NamingConventions;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class GetterSetterUtil {
	
	private static final String[] EMPTY= new String[0];
	
	//no instances
	private GetterSetterUtil(){
	}
	
	public static String getGetterName(IField field, String[] excludedNames) throws JavaScriptModelException {
		boolean useIs= StubUtility.useIsForBooleanGetters(field.getJavaScriptProject());
		return getGetterName(field, excludedNames, useIs);
	}
	
	private static String getGetterName(IField field, String[] excludedNames, boolean useIsForBoolGetters) throws JavaScriptModelException {
		if (excludedNames == null) {
			excludedNames= EMPTY;
		}
		return getGetterName(field.getJavaScriptProject(), field.getElementName(), field.getFlags(), useIsForBoolGetters && JavaModelUtil.isBoolean(field), excludedNames);
	}
	
	public static String getGetterName(IVariableBinding variableType, IJavaScriptProject project, String[] excludedNames, boolean isBoolean) {
		boolean useIs= StubUtility.useIsForBooleanGetters(project) && isBoolean;
		return getGetterName(project, variableType.getName(), variableType.getModifiers(), useIs, excludedNames);
	}
	
	public static String getGetterName(IJavaScriptProject project, String fieldName, int flags, boolean isBoolean, String[] excludedNames){
		return NamingConventions.suggestGetterName(project, fieldName, flags, isBoolean, excludedNames);	
	}

	public static String getSetterName(IVariableBinding variableType, IJavaScriptProject project, String[] excludedNames, boolean isBoolean) {
		return getSetterName(project, variableType.getName(), variableType.getModifiers(), isBoolean, excludedNames);
	}
	
	public static String getSetterName(IJavaScriptProject project, String fieldName, int flags, boolean isBoolean, String[] excludedNames){
		return NamingConventions.suggestSetterName(project, fieldName, flags, isBoolean, excludedNames);	
	}

	public static String getSetterName(IField field, String[] excludedNames) throws JavaScriptModelException {
		if (excludedNames == null) {
			excludedNames= EMPTY;
		}		
		return NamingConventions.suggestSetterName(field.getJavaScriptProject(), field.getElementName(), field.getFlags(), JavaModelUtil.isBoolean(field), excludedNames);
	}	

	public static IFunction getGetter(IField field) throws JavaScriptModelException{
		if (field.getDeclaringType()==null)
			return null;
		String getterName= getGetterName(field, EMPTY, true);
		IFunction primaryCandidate= JavaModelUtil.findMethod(getterName, new String[0], false, field.getDeclaringType());
		if (! JavaModelUtil.isBoolean(field) || (primaryCandidate != null && primaryCandidate.exists()))
			return primaryCandidate;
		//bug 30906 describes why we need to look for other alternatives here (try with get... for booleans)
		String secondCandidateName= getGetterName(field, EMPTY, false);
		return JavaModelUtil.findMethod(secondCandidateName, new String[0], false, field.getDeclaringType());
	}
	
	public static IFunction getSetter(IField field) throws JavaScriptModelException{
		if (field.getDeclaringType()==null)
			return null;
		String[] args= new String[] { field.getTypeSignature() };	
		return JavaModelUtil.findMethod(getSetterName(field, EMPTY), args, false, field.getDeclaringType());
	}
	
	/**
	 * Create a stub for a getter of the given field using getter/setter templates. The resulting code
	 * has to be formatted and indented.
	 * @param field The field to create a getter for
	 * @param setterName The chosen name for the setter
	 * @param addComments If <code>true</code>, comments will be added.
	 * @param flags The flags signaling visibility, if static, synchronized or final
	 * @return Returns the generated stub.
	 * @throws CoreException
	 */
	public static String getSetterStub(IField field, String setterName, boolean addComments, int flags) throws CoreException {
		
		String fieldName= field.getElementName();
		IType parentType= field.getDeclaringType();
		
		String returnSig= field.getTypeSignature();
		String typeName= Signature.toString(returnSig);
		
		IJavaScriptProject project= field.getJavaScriptProject();

		String accessorName = NamingConventions.removePrefixAndSuffixForFieldName(project, fieldName, field.getFlags());
		String argname= StubUtility.suggestArgumentName(project, accessorName, EMPTY);

		boolean isStatic= Flags.isStatic(flags);

		String lineDelim= "\n"; // Use default line delimiter, as generated stub has to be formatted anyway //$NON-NLS-1$
		StringBuffer buf= new StringBuffer();
		if (addComments) {
			String comment= CodeGeneration.getSetterComment(field.getJavaScriptUnit(), parentType.getTypeQualifiedName('.'), setterName, field.getElementName(), typeName, argname, accessorName, lineDelim);
			if (comment != null) {
				buf.append(comment);
				buf.append(lineDelim);
			}
		}
		buf.append(JdtFlags.getVisibilityString(flags));
		buf.append(' ');	
		if (isStatic)
			buf.append("static "); //$NON-NLS-1$
			
		buf.append("void "); //$NON-NLS-1$
		buf.append(setterName);
		buf.append('('); 
		buf.append(typeName); 
		buf.append(' '); 
		buf.append(argname); 
		buf.append(") {"); //$NON-NLS-1$
		buf.append(lineDelim);
		
		boolean useThis= StubUtility.useThisForFieldAccess(project);
		if (argname.equals(fieldName) || (useThis && !isStatic)) {
			if (isStatic)
				fieldName= parentType.getElementName() + '.' + fieldName;
			else
				fieldName= "this." + fieldName; //$NON-NLS-1$
		}
		String body= CodeGeneration.getSetterMethodBodyContent(field.getJavaScriptUnit(), parentType.getTypeQualifiedName('.'), setterName, fieldName, argname, lineDelim);
		if (body != null) {
			buf.append(body);
		}
		buf.append("}"); //$NON-NLS-1$
		buf.append(lineDelim);
		return buf.toString();
	}
	
	/**
	 * Create a stub for a getter of the given field using getter/setter templates. The resulting code
	 * has to be formatted and indented.
	 * @param field The field to create a getter for
	 * @param getterName The chosen name for the getter
	 * @param addComments If <code>true</code>, comments will be added.
	 * @param flags The flags signaling visibility, if static, synchronized or final
	 * @return Returns the generated stub.
	 * @throws CoreException
	 */
	public static String getGetterStub(IField field, String getterName, boolean addComments, int flags) throws CoreException {
		String fieldName= field.getElementName();
		IType parentType= field.getDeclaringType();
		
		boolean isStatic= Flags.isStatic(flags);
		
		String typeName= Signature.toString(field.getTypeSignature());
		String accessorName = NamingConventions.removePrefixAndSuffixForFieldName(field.getJavaScriptProject(), fieldName, field.getFlags());

		String lineDelim= "\n"; // Use default line delimiter, as generated stub has to be formatted anyway //$NON-NLS-1$
		StringBuffer buf= new StringBuffer();
		if (addComments) {
			String comment= CodeGeneration.getGetterComment(field.getJavaScriptUnit(), parentType.getTypeQualifiedName('.'), getterName, field.getElementName(), typeName, accessorName, lineDelim);
			if (comment != null) {
				buf.append(comment);
				buf.append(lineDelim);
			}					
		}
		
		buf.append(JdtFlags.getVisibilityString(flags));
		buf.append(' ');			
		if (isStatic)
			buf.append("static "); //$NON-NLS-1$
			
		buf.append(typeName);
		buf.append(' ');
		buf.append(getterName);
		buf.append("() {"); //$NON-NLS-1$
		buf.append(lineDelim);
		
		boolean useThis= StubUtility.useThisForFieldAccess(field.getJavaScriptProject());
		if (useThis && !isStatic) {
			fieldName= "this." + fieldName; //$NON-NLS-1$
		}
		
		String body= CodeGeneration.getGetterMethodBodyContent(field.getJavaScriptUnit(), parentType.getTypeQualifiedName('.'), getterName, fieldName, lineDelim);
		if (body != null) {
			buf.append(body);
		}
		buf.append("}"); //$NON-NLS-1$
		buf.append(lineDelim);
		return buf.toString(); 
	}


}
