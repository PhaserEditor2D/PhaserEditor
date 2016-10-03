/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import java.util.ArrayList;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;

/*
 * Converts a binding key into a signature
 */
public class KeyToSignature extends BindingKeyParser {

	public static final int SIGNATURE = 0;
	public static final int TYPE_ARGUMENTS = 1;
	public static final int DECLARING_TYPE = 2;
	public static final int THROWN_EXCEPTIONS = 3;

	public StringBuffer signature = new StringBuffer();
	private int kind;
	private ArrayList arguments = new ArrayList();
	private ArrayList thrownExceptions = new ArrayList();
	private int mainTypeStart = -1;
	private int mainTypeEnd;
	private int typeSigStart = -1;
	
	public static final char[] ObjectSignature = "Ljava/lang/Object;".toCharArray(); //$NON-NLS-1$


	public KeyToSignature(BindingKeyParser parser) {
		super(parser);
		this.kind = ((KeyToSignature) parser).kind;
	}

	public KeyToSignature(String key, int kind) {
		super(key);
		this.kind = kind;
	}

	public void consumeArrayDimension(char[] brakets) {
		this.signature.append(brakets);
	}

	public void consumeBaseType(char[] baseTypeSig) {
		this.typeSigStart = this.signature.length();
		this.signature.append(baseTypeSig);
	}

	public void consumeCapture(int position) {
		// behave as if it was a wildcard
		this.signature = ((KeyToSignature) this.arguments.get(0)).signature;
	}

	public void consumeLocalType(char[] uniqueKey) {
		this.signature = new StringBuffer();
		// remove trailing semi-colon as it is added later in comsumeType()
		uniqueKey = CharOperation.subarray(uniqueKey, 0, uniqueKey.length-1);
		CharOperation.replace(uniqueKey, '/', '.');
		this.signature.append(uniqueKey);
	}

	public void consumeMethod(char[] selector, char[] methodSignature) {
		this.arguments = new ArrayList();
		CharOperation.replace(methodSignature, '/', '.');
		switch(this.kind) {
			case SIGNATURE:
				this.signature = new StringBuffer();
				this.signature.append(methodSignature);
				break;
		}
	}

	public void consumeMemberType(char[] simpleTypeName) {
		this.signature.append('.');
		this.signature.append(simpleTypeName);
	}

	public void consumePackage(char[] pkgName) {
		this.signature.append(pkgName);
	}

	public void consumeParameterizedType(char[] simpleTypeName, boolean isRaw) {
		if (simpleTypeName != null) {
			// member type
			this.signature.append('.');
			this.signature.append(simpleTypeName);
		}
		if (!isRaw) {
			this.signature.append('<');
			int length = this.arguments.size();
			for (int i = 0; i < length; i++) {
				this.signature.append(((KeyToSignature) this.arguments.get(i)).signature);
			}
			this.signature.append('>');
			if (this.kind != TYPE_ARGUMENTS)
				this.arguments = new ArrayList();
		}
	}

	public void consumeParser(BindingKeyParser parser) {
		this.arguments.add(parser);
	}

	public void consumeField(char[] fieldName) {
		if (this.kind == SIGNATURE) {
			this.signature = ((KeyToSignature) this.arguments.get(0)).signature;
		}
	}

	public void consumeException() {
		int size = this.arguments.size();
		if (size > 0) {
			for (int i=0; i<size; i++) {
				this.thrownExceptions.add(((KeyToSignature) this.arguments.get(i)).signature.toString());
			}
			this.arguments = new ArrayList();
		}
	}

	public void consumeFullyQualifiedName(char[] fullyQualifiedName) {
		this.typeSigStart = this.signature.length();
		this.signature.append('L');
		this.signature.append(CharOperation.replaceOnCopy(fullyQualifiedName, '/', '.'));
	}

	public void consumeSecondaryType(char[] simpleTypeName) {
		this.signature.append('~');
		this.mainTypeStart = this.signature.lastIndexOf(".") + 1; //$NON-NLS-1$
		if (this.mainTypeStart == 0)
			this.mainTypeStart = 1; // default package
		this.mainTypeEnd = this.signature.length();
		this.signature.append(simpleTypeName);
	}

	public void consumeType() {
		// remove main type if needed
		if (this.mainTypeStart != -1) {
			this.signature.replace(this.mainTypeStart, this.mainTypeEnd, ""); //$NON-NLS-1$
		}
		this.signature.append(';');
	}

	public void consumeTypeVariable(char[] position, char[] typeVariableName) {
		this.signature = new StringBuffer();
		this.signature.append('T');
		this.signature.append(typeVariableName);
		this.signature.append(';');
	}

	public void consumeTypeWithCapture() {
		KeyToSignature keyToSignature = (KeyToSignature) this.arguments.get(0);
		this.signature = keyToSignature.signature;
		this.arguments = keyToSignature.arguments;
		this.thrownExceptions = keyToSignature.thrownExceptions;
	}

	public String[] getThrownExceptions() {
		int length = this.thrownExceptions.size();
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = (String) this.thrownExceptions.get(i);
		}
		return result;
	}

	public String[] getTypeArguments() {
		int length = this.arguments.size();
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = ((KeyToSignature) this.arguments.get(i)).signature.toString();
		}
		return result;
	}

	public BindingKeyParser newParser() {
		return new KeyToSignature(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.signature.toString();
	}

}
