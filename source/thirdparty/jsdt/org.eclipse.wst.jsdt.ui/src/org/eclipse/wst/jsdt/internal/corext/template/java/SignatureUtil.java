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
package org.eclipse.wst.jsdt.internal.corext.template.java;

import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;

/**
 * Utilities for Signature operations.
 * 
 * @see Signature
 * 
 */
public final class SignatureUtil {
	
	/**
	 * The signature of <code>java.lang.Object</code> ({@value}).
	 */
	private static final String OBJECT_SIGNATURE= "Ljava.lang.Object;"; //$NON-NLS-1$
	
	/**
	 * The signature of <code>Boolean</code> ({@value}).
	 */
	public static final String BOOLEAN_SIGNATURE= "LBoolean;"; //$NON-NLS-1$

	private SignatureUtil() {
		// do not instantiate
	}
	
	/**
	 * Returns <code>true</code> if <code>signature</code> is the
	 * signature of the <code>java.lang.Object</code> type.
	 * 
	 * @param signature the signature
	 * @return <code>true</code> if <code>signature</code> is the
	 *         signature of the <code>java.lang.Object</code> type,
	 *         <code>false</code> otherwise
	 */
	public static boolean isJavaLangObject(String signature) {
		return OBJECT_SIGNATURE.equals(signature);
	}

	/**
	 * Returns the fully qualified type name of the given signature, with any
	 * type parameters and arrays erased.
	 * 
	 * @param signature the signature
	 * @return the fully qualified type name of the signature
	 */
	public static String stripSignatureToFQN(String signature) throws IllegalArgumentException {
		signature= Signature.getElementType(signature);
		return Signature.toString(signature);
	}
	
	/**
	 * Returns the qualified signature corresponding to
	 * <code>signature</code>.
	 * 
	 * @param signature the signature to qualify
	 * @param context the type inside which an unqualified type will be
	 *        resolved to find the qualifier, or <code>null</code> if no
	 *        context is available
	 * @return the qualified signature
	 */
	public static String qualifySignature(final String signature, final IType context) {
		if (context == null)
			return signature;
		
		String qualifier= Signature.getSignatureQualifier(signature);
		if (qualifier.length() > 0)
			return signature;

		String elementType= Signature.getElementType(signature);
		String erasure= elementType;
		String simpleName= Signature.getSignatureSimpleName(erasure);
		String genericSimpleName= Signature.getSignatureSimpleName(elementType);
		
		int dim= Signature.getArrayCount(signature);
		
		try {
			String[][] strings= context.resolveType(simpleName);
			if (strings != null && strings.length > 0)
				qualifier= strings[0][0];
		} catch (JavaScriptModelException e) {
			// ignore - not found
		}
		
		if (qualifier.length() == 0)
			return signature;
		
		String qualifiedType= Signature.toQualifiedName(new String[] {qualifier, genericSimpleName});
		String qualifiedSignature= Signature.createTypeSignature(qualifiedType, true);
		String newSignature= Signature.createArraySignature(qualifiedSignature, dim);
		
		return newSignature;
	}
	
	/**
	 * Takes a method signature
	 * <code>[&lt; typeVariableName : formalTypeDecl &gt;] ( paramTypeSig1* ) retTypeSig</code>
	 * and returns it with any parameter signatures filtered through
	 * <code>getLowerBound</code> and the return type filtered through
	 * <code>getUpperBound</code>. Any preceding formal type variable
	 * declarations are removed.
	 * <p>
	 * TODO this is a temporary workaround for
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=83600
	 * </p>
	 * 
	 * @param signature the method signature to convert
	 * @return the signature with no bounded types
	 */
	public static char[] unboundedSignature(char[] signature) {
		if (signature == null || signature.length < 2)
			return signature;

		StringBuffer res= new StringBuffer("("); //$NON-NLS-1$
		char[][] parameters= Signature.getParameterTypes(signature);
		for (int i= 0; i < parameters.length; i++) {
			char[] param= parameters[i];
			res.append(param);
		}
		res.append(')');
		res.append(Signature.getReturnType(signature));
		return res.toString().toCharArray();
	}

	private static int typeEnd(char[] signature, int pos) {
		int depth= 0;
		while (pos < signature.length) {
			switch (signature[pos]) {
				case Signature.C_SEMICOLON:
					if (depth == 0)
						return pos + 1;
					break;
			}
			pos++;
		}
		return pos + 1;
	}
}
