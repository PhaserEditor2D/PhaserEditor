/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.wst.jsdt.internal.core.util.KeyToSignature;

/**
 * Utility class to decode or create a binding key.
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 *
 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getKey()
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class BindingKey {

	private String key;

	/**
	 * Creates a new binding key.
	 *
	 * @param key the key to decode
	 */
	public BindingKey(String key) {
		this.key = key;
	}

	/**
	 * Creates a new array type binding key from the given type binding key and the given array dimension.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createArrayTypeBindingKey("LObject;", 1) -> "[LObject;"
	 * </code>
	 * </pre>
	 * </p>
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param typeKey the binding key of the given type
	 * @param arrayDimension the given array dimension
	 * @return a new array type binding key
	 */
	public static String createArrayTypeBindingKey(String typeKey, int arrayDimension) {
		// Note this implementation is heavily dependent on ArrayTypeBinding#computeUniqueKey()
		StringBuffer buffer = new StringBuffer();
		while (arrayDimension-- > 0)
			buffer.append('[');
		buffer.append(typeKey);
		return buffer.toString();
	}

	/**
	 * Creates a new type binding key from the given type name. 
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createTypeBindingKey("String") -> "LString;"
	 * </code>
	 * </pre>
	 * </p>
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param typeName the possibly qualified type name
	 * @return a new type binding key
	 */
	public static String createTypeBindingKey(String typeName) {
		// Note this implementation is heavily dependent on TypeBinding#computeUniqueKey() and its subclasses
		return Signature.createTypeSignature(typeName.replace('.', '/'), true/*resolved*/);
	}

	/**
	 * Transforms this binding key into a resolved signature.
	 * If this binding key represents a field, the returned signature is
	 * the declaring type's signature.
	 *
	 * @return the resolved signature for this binding key
	 * @see Signature
	 */
	public String toSignature() {
		KeyToSignature keyToSignature = new KeyToSignature(this.key, KeyToSignature.SIGNATURE);
		keyToSignature.parse();
		return keyToSignature.signature.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.key;
	}
}
