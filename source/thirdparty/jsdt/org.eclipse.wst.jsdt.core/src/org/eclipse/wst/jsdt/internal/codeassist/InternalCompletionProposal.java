/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist;

import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.core.BinaryType;
import org.eclipse.wst.jsdt.internal.core.NameLookup;

/**
 * Internal completion proposal
 * @since 3.1
 */
public class InternalCompletionProposal {
	private static Object NO_ATTACHED_SOURCE = new Object();

	static final char[] ARG = "arg".toCharArray();  //$NON-NLS-1$
	static final char[] ARG0 = "arg0".toCharArray();  //$NON-NLS-1$
	static final char[] ARG1 = "arg1".toCharArray();  //$NON-NLS-1$
	static final char[] ARG2 = "arg2".toCharArray();  //$NON-NLS-1$
	static final char[] ARG3 = "arg3".toCharArray();  //$NON-NLS-1$
	static final char[][] ARGS1 = new char[][]{ARG0};
	static final char[][] ARGS2 = new char[][]{ARG0, ARG1};
	static final char[][] ARGS3 = new char[][]{ARG0, ARG1, ARG2};
	static final char[][] ARGS4 = new char[][]{ARG0, ARG1, ARG2, ARG3};

	protected CompletionEngine completionEngine;
	protected NameLookup nameLookup;

	protected char[] declarationPackageName;
	protected char[] declarationTypeName;
	protected char[] returnQualification;
	protected char[] returnSimpleName;
	protected char[][] parameterTypeNames;

	protected char[] originalSignature;

	protected int accessibility = IAccessRule.K_ACCESSIBLE;

	protected boolean isConstructor = false;

	protected char[][] createDefaultParameterNames(int length) {
		char[][] parameterNames;
		switch (length) {
			case 0 :
				parameterNames = new char[length][];
				break;
			case 1 :
				parameterNames = ARGS1;
				break;
			case 2 :
				parameterNames = ARGS2;
				break;
			case 3 :
				parameterNames = ARGS3;
				break;
			case 4 :
				parameterNames = ARGS4;
				break;
			default :
				parameterNames = new char[length][];
				for (int i = 0; i < length; i++) {
					parameterNames[i] = CharOperation.concat(ARG, String.valueOf(i).toCharArray());
				}
				break;
		}
		return parameterNames;
	}
	protected char[][] findMethodParameterNames(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] paramTypeNames){
		if(paramTypeNames == null || declaringTypeName == null) return null;

		char[][] parameterNames = null;
		int length = paramTypeNames.length;

		char[] tName = CharOperation.concat(declaringTypePackageName,declaringTypeName,'.');
		Object cachedType = this.completionEngine.typeCache.get(tName);

		IType type = null;
		if(cachedType != null) {
			if(cachedType != NO_ATTACHED_SOURCE && cachedType instanceof BinaryType) {
				type = (BinaryType)cachedType;
			}
		} else {
			// TODO (david) shouldn't it be NameLookup.ACCEPT_ALL ?
			NameLookup.Answer answer = this.nameLookup.findType(new String(tName),
				false,
				NameLookup.ACCEPT_CLASSES & NameLookup.ACCEPT_INTERFACES,
				true/* consider secondary types */,
				false/* do NOT wait for indexes */,
				false/*don't check restrictions*/,
				null);
			type = answer == null ? null : answer.type;
			if(type instanceof BinaryType){
				this.completionEngine.typeCache.put(tName, type);
			} else {
				type = null;
			}
		}

		if(type != null) {
			String[] args = new String[length];
			for(int i = 0;	i< length ; i++){
				args[i] = new String(paramTypeNames[i]);
			}
			IFunction method = type.getFunction(new String(selector),args);
			try{
				parameterNames = new char[length][];
				String[] params = method.getParameterNames();
				for(int i = 0;	i< length ; i++){
					parameterNames[i] = params[i].toCharArray();
				}
			} catch(JavaScriptModelException e){
				parameterNames = null;
			}
		}

		// default parameters name
		if(parameterNames == null) {
			parameterNames = createDefaultParameterNames(length);
		}

		return parameterNames;
	}

	protected char[] getDeclarationPackageName() {
		return this.declarationPackageName;
	}

	protected char[] getDeclarationTypeName() {
		return this.declarationTypeName;
	}

	protected char[] getReturnQualification() {
		return this.returnQualification;
	}

	protected char[] getReturnSimpleName() {
		return this.returnSimpleName;
	}

	protected char[][] getParameterTypeNames() {
		return this.parameterTypeNames;
	}

	protected void setDeclarationPackageName(char[] declarationPackageName) {
		this.declarationPackageName = declarationPackageName;
	}

	protected void setDeclarationTypeName(char[] declarationTypeName) {
		this.declarationTypeName = declarationTypeName;
	}

	protected void setReturnQualification(char[] packageName) {
		this.returnQualification = packageName;
	}

	protected void setReturnSimpleName(char[] typeName) {
		this.returnSimpleName = typeName;
	}

	protected void setParameterTypeNames(char[][] parameterTypeNames) {
		this.parameterTypeNames = parameterTypeNames;
	}

	protected void setAccessibility(int kind) {
		this.accessibility = kind;
	}

	protected void setIsContructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
	}
	public void setOriginalSignature(char[] originalSignature) {
		this.originalSignature = originalSignature;
	}

	public IJavaScriptProject getJavaProject() {
		return completionEngine.javaProject;
	}

	public NameLookup getNameLookup() {
		return nameLookup;
	}
}
