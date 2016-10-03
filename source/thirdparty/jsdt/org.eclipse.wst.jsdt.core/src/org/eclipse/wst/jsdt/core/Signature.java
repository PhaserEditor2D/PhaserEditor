/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added J2SE 1.5 support
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.wst.jsdt.internal.core.util.Util;


/**
 * Provides methods for encoding and decoding type and method signature strings.
 * <p>
 * This class provides static methods and constants only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class Signature {

	/**
	 * Character constant indicating the semicolon in a signature.
	 * Value is <code>';'</code>.
	 */
	public static final char C_SEMICOLON 			= ';';

	/**
	 * Character constant indicating the colon in a signature.
	 * Value is <code>':'</code>.
	 *   3.0
	 */
	public static final char C_COLON 			= ':';

	/**
	 * Character constant indicating result type void in a signature.
	 * Value is <code>'V'</code>.
	 */
	public static final char C_VOID			= 'V';


	/**
	 * Character constant indicating any type in a signature.
	 * Value is <code>'A'</code>.
	 */
	public static final char C_ANY			= 'A';

	/**
	 * Character constant indicating the dot in a signature.
	 * Value is <code>'.'</code>.
	 */
	public static final char C_DOT			= '.';

	/**
	 * Character constant indicating the dollar in a signature.
	 * Value is <code>'$'</code>.
	 */
	public static final char C_DOLLAR			= '$';

	/**
	 * Character constant indicating an array type in a signature.
	 * Value is <code>'['</code>.
	 */
	public static final char C_ARRAY		= '[';

	/**
	 * Character constant indicating the start of a resolved, named type in a
	 * signature. Value is <code>'L'</code>.
	 */
	public static final char C_RESOLVED		= 'L';


	/**
	 * Character constant indicating a compilation unit.
	 *  Value is <code>'X'</code>.
	 */
	public static final char C_COMPILATION_UNIT		= 'X';


	/**
	 * Character constant indicating the start of an unresolved, named type in a
	 * signature. Value is <code>'Q'</code>.
	 */
	public static final char C_UNRESOLVED	= 'Q';

	/**
	 * Character constant indicating the end of a named type in a signature.
	 * Value is <code>';'</code>.
	 */
	public static final char C_NAME_END		= ';';

	/**
	 * Character constant indicating the start of a parameter type list in a
	 * signature. Value is <code>'('</code>.
	 */
	public static final char C_PARAM_START	= '(';

	/**
	 * Character constant indicating the end of a parameter type list in a
	 * signature. Value is <code>')'</code>.
	 */
	public static final char C_PARAM_END	= ')';

	/** String constant for the signature of result type void.
	 * Value is <code>"V"</code>.
	 */
	public static final String SIG_VOID			= "V"; //$NON-NLS-1$

	public static final String SIG_ANY		= "A"; //$NON-NLS-1$

	public static final String SIG_COMPILATION_UNIT			= "X"; //$NON-NLS-1$

	/**
	 * Kind constant for a class type signature.
	 * @see #getTypeSignatureKind(String)
	 *  
	 */
	public static final int CLASS_TYPE_SIGNATURE = 1;

	/**
	 * Kind constant for a base (primitive or void) type signature.
	 * @see #getTypeSignatureKind(String)
	 *  
	 */
	public static final int BASE_TYPE_SIGNATURE = 2;

	/**
	 * Kind constant for an array type signature.
	 * @see #getTypeSignatureKind(String)
	 *  
	 */
	public static final int ARRAY_TYPE_SIGNATURE = 4;

	public static final char[] VOID = "void".toCharArray(); //$NON-NLS-1$
	public static final char[] ANY = "any".toCharArray(); //$NON-NLS-1$

	private static final Map BASE_TYPES = new HashMap();
	static {
		// B -> ???
		BASE_TYPES.put('C', String.valueOf(TypeBinding.CHAR.readableName()));		// C -> char
		BASE_TYPES.put('D', String.valueOf(TypeBinding.DOUBLE.readableName()));		// D -> double
		BASE_TYPES.put('F', String.valueOf(TypeBinding.FLOAT.readableName()));		// F -> float
		BASE_TYPES.put('I', String.valueOf(TypeBinding.INT.readableName()));		// I -> int
		BASE_TYPES.put('J', String.valueOf(TypeBinding.LONG.readableName()));		// J -> long
		BASE_TYPES.put('S', String.valueOf(TypeBinding.SHORT.readableName()));		// S -> short
		BASE_TYPES.put('Z', String.valueOf(TypeBinding.BOOLEAN.readableName()));	// Z -> boolean
	}
	
private Signature() {
	// Not instantiable
}

private static int checkName(char[] name, char[] typeName, int pos, int length) {
    if (CharOperation.fragmentEquals(name, typeName, pos, true)) {
        pos += name.length;
        if (pos == length) return pos;
        char currentChar = typeName[pos];
        switch (currentChar) {
            case ' ' :
            case '.' :
            case '[' :
            case ',' :
                return pos;
			default:
			    if (ScannerHelper.isWhitespace(currentChar))
			    	return pos;

        }
    }
    return -1;
}

/**
 * Creates a new type signature with the given amount of array nesting added
 * to the given type signature.
 *
 * @param typeSignature the type signature
 * @param arrayCount the desired number of levels of array nesting
 * @return the encoded array type signature
 *
 *  
 */
public static char[] createArraySignature(char[] typeSignature, int arrayCount) {
	if (arrayCount == 0) return typeSignature;
	int sigLength = typeSignature.length;
	char[] result = new char[arrayCount + sigLength];
	for (int i = 0; i < arrayCount; i++) {
		result[i] = C_ARRAY;
	}
	System.arraycopy(typeSignature, 0, result, arrayCount, sigLength);
	return result;
}
/**
 * Creates a new type signature with the given amount of array nesting added
 * to the given type signature.
 *
 * @param typeSignature the type signature
 * @param arrayCount the desired number of levels of array nesting
 * @return the encoded array type signature
 */
public static String createArraySignature(String typeSignature, int arrayCount) {
	return new String(createArraySignature(typeSignature.toCharArray(), arrayCount));
}

/**
 * Creates a method signature from the given parameter and return type
 * signatures. The encoded method signature is dot-based.
 *
 * @param parameterTypes the list of parameter type signatures
 * @param returnType the return type signature
 * @return the encoded method signature
 *
 *  
 */
public static char[] createMethodSignature(char[][] parameterTypes, char[] returnType) {
	int parameterTypesLength = parameterTypes.length;
	int parameterLength = 0;
	for (int i = 0; i < parameterTypesLength; i++) {
		parameterLength += parameterTypes[i].length;

	}
	int returnTypeLength = returnType.length;
	char[] result = new char[1 + parameterLength + 1 + returnTypeLength];
	result[0] = C_PARAM_START;
	int index = 1;
	for (int i = 0; i < parameterTypesLength; i++) {
		char[] parameterType = parameterTypes[i];
		int length = parameterType.length;
		System.arraycopy(parameterType, 0, result, index, length);
		index += length;
	}
	result[index] = C_PARAM_END;
	System.arraycopy(returnType, 0, result, index+1, returnTypeLength);
	return result;
}

/**
 * Creates a method signature from the given parameter and return type
 * signatures. The encoded method signature is dot-based. This method
 * is equivalent to
 * <code>createMethodSignature(parameterTypes, returnType)</code>.
 *
 * @param parameterTypes the list of parameter type signatures
 * @param returnType the return type signature
 * @return the encoded method signature
 * @see Signature#createMethodSignature(char[][], char[])
 */
public static String createMethodSignature(String[] parameterTypes, String returnType) {
	int parameterTypesLenth = parameterTypes.length;
	char[][] parameters = new char[parameterTypesLenth][];
	for (int i = 0; i < parameterTypesLenth; i++) {
		parameters[i] = parameterTypes[i].toCharArray();
	}
	return new String(createMethodSignature(parameters, returnType.toCharArray()));
}

/**
 * Creates a new type signature from the given type name encoded as a character
 * array. The type name may contain primitive types, array types or parameterized types.
 * This method is equivalent to
 * <code>createTypeSignature(new String(typeName),isResolved)</code>, although
 * more efficient for callers with character arrays rather than strings. If the
 * type name is qualified, then it is expected to be dot-based.
 *
 * @param typeName the possibly qualified type name
 * @param isResolved <code>true</code> if the type name is to be considered
 *   resolved (for example, a type name from a binary class file), and
 *   <code>false</code> if the type name is to be considered unresolved
 *   (for example, a type name found in source code)
 * @return the encoded type signature
 * @see #createTypeSignature(java.lang.String,boolean)
 */
public static String createTypeSignature(char[] typeName, boolean isResolved) {
	return new String(createCharArrayTypeSignature(typeName, isResolved));
}

/**
 * Creates a new type signature from the given type name encoded as a character
 * array. The type name may contain primitive types or array types or parameterized types.
 * This method is equivalent to
 * <code>createTypeSignature(new String(typeName),isResolved).toCharArray()</code>,
 * although more efficient for callers with character arrays rather than strings.
 * If the type name is qualified, then it is expected to be dot-based.
 *
 * @param typeName the possibly qualified type name
 * @param isResolved <code>true</code> if the type name is to be considered
 *   resolved (for example, a type name from a binary class file), and
 *   <code>false</code> if the type name is to be considered unresolved
 *   (for example, a type name found in source code)
 * @return the encoded type signature
 * @see #createTypeSignature(java.lang.String,boolean)
 *
 *  
 */
public static char[] createCharArrayTypeSignature(char[] typeName, boolean isResolved) {
	if (typeName == null || typeName.length == 0)
	{
		return new char[]{C_ANY};
	}

	int length = typeName.length;
	StringBuffer buffer = new StringBuffer(5);
	int pos = encodeTypeSignature(typeName, 0, isResolved, length, buffer);
	pos = consumeWhitespace(typeName, pos, length);
	if (pos < length) throw new IllegalArgumentException(new String(typeName));
	char[] result = new char[length = buffer.length()];
	buffer.getChars(0, length, result, 0);
	return result;
}
private static int consumeWhitespace(char[] typeName, int pos, int length) {
    while (pos < length) {
        char currentChar = typeName[pos];
        if (currentChar != ' ' && !CharOperation.isWhitespace(currentChar)) {
            break;
        }
        pos++;
    }
    return pos;
}
private static int encodeQualifiedName(char[] typeName, int pos, int length, StringBuffer buffer) {
    int count = 0;
    char lastAppendedChar = 0;
    nameLoop: while (pos < length) {
	    char currentChar = typeName[pos];
		switch (currentChar) {
		    case '[' :
		    case ',' :
		        break nameLoop;
			case '.' :
			    buffer.append(C_DOT);
				lastAppendedChar = C_DOT;
			    count++;
			    break;
			default:
			    if (currentChar == ' ' || ScannerHelper.isWhitespace(currentChar)) {
			        if (lastAppendedChar == C_DOT) { // allow spaces after a dot
			            pos = consumeWhitespace(typeName, pos, length) - 1; // will be incremented
			            break;
			        }
			        // allow spaces before a dot
				    int checkPos = checkNextChar(typeName, '.', pos, length, true);
				    if (checkPos > 0) {
				        buffer.append(C_DOT);			// process dot immediately to avoid one iteration
				        lastAppendedChar = C_DOT;
				        count++;
				        pos = checkPos;
				        break;
				    }
				    break nameLoop;
			    }
			    buffer.append(currentChar);
			    lastAppendedChar = currentChar;
				count++;
			    break;
		}
	    pos++;
    }
    if (count == 0) throw new IllegalArgumentException(new String(typeName));
	return pos;
}

private static int encodeArrayDimension(char[] typeName, int pos, int length, StringBuffer buffer) {
    int checkPos;
    while (pos < length && (checkPos = checkNextChar(typeName, '[', pos, length, true)) > 0) {
        pos = checkNextChar(typeName, ']', checkPos, length, false);
        buffer.append(C_ARRAY);
    }
    return pos;
}
private static int checkArrayDimension(char[] typeName, int pos, int length) {
    int genericBalance = 0;
    while (pos < length) {
		switch(typeName[pos]) {
		    case ',' :
			    if (genericBalance == 0) return -1;
			    break;
			case '[':
			    if (genericBalance == 0) {
			        return pos;
			    }
		}
		pos++;
    }
    return -1;
}
private static int checkNextChar(char[] typeName, char expectedChar, int pos, int length, boolean isOptional) {
    pos = consumeWhitespace(typeName, pos, length);
    if (pos < length && typeName[pos] == expectedChar)
        return pos + 1;
    if (!isOptional) throw new IllegalArgumentException(new String(typeName));
    return -1;
}

private static int encodeTypeSignature(char[] typeName, int start, boolean isResolved, int length, StringBuffer buffer) {
    int pos = start;
    pos = consumeWhitespace(typeName, pos, length);
    if (pos >= length) throw new IllegalArgumentException(new String(typeName));
    int checkPos;
    char currentChar = typeName[pos];
    switch (currentChar) {
		// primitive type?
		case 'v':
		    checkPos = checkName(VOID, typeName, pos, length);
		    if (checkPos > 0) {
		        pos = encodeArrayDimension(typeName, checkPos, length, buffer);
			    buffer.append(C_VOID);
			    return pos;
			}
		    break;
    }
    // non primitive type
    checkPos = checkArrayDimension(typeName, pos, length);
	int end;
	if (checkPos > 0) {
	    end = encodeArrayDimension(typeName, checkPos, length, buffer);
	} else {
	    end = -1;
	}
	buffer.append(isResolved ? C_RESOLVED : C_UNRESOLVED);
	while (true) { // loop on qualifiedName[<args>][.qualifiedName[<args>]*
	    pos = encodeQualifiedName(typeName, pos, length, buffer);
		checkPos = checkNextChar(typeName, '.', pos, length, true);
		if (checkPos > 0) {
			buffer.append(C_DOT);
			pos = checkPos;
		} else {
			break;
		}
	}
	buffer.append(C_NAME_END);
	if (end > 0) pos = end; // skip array dimension which were preprocessed
    return pos;
}

/**
 * Creates a new type signature from the given type name. If the type name is qualified,
 * then it is expected to be dot-based. The type name may contain primitive
 * types or array types. However, parameterized types are not supported.
 * <p>
 * For example:
 * <pre>
 * <code>
 * createTypeSignature("int", hucairz) -> "I"
 * createTypeSignature("java.lang.String", true) -> "Ljava.lang.String;"
 * createTypeSignature("String", false) -> "QString;"
 * createTypeSignature("java.lang.String", false) -> "Qjava.lang.String;"
 * createTypeSignature("int []", false) -> "[I"
 * </code>
 * </pre>
 * </p>
 *
 * @param typeName the possibly qualified type name
 * @param isResolved <code>true</code> if the type name is to be considered
 *   resolved (for example, a type name from a binary class file), and
 *   <code>false</code> if the type name is to be considered unresolved
 *   (for example, a type name found in source code)
 * @return the encoded type signature
 */
public static String createTypeSignature(String typeName, boolean isResolved) {
	return createTypeSignature(typeName == null ? null : typeName.toCharArray(), isResolved);
}

/**
 * Returns the array count (array nesting depth) of the given type signature.
 *
 * @param typeSignature the type signature
 * @return the array nesting depth, or 0 if not an array
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 *
 *  
 */
public static int getArrayCount(char[] typeSignature) throws IllegalArgumentException {
	try {
		int count = 0;
		while (typeSignature[count] == C_ARRAY) {
			++count;
		}
		return count;
	} catch (ArrayIndexOutOfBoundsException e) { // signature is syntactically incorrect if last character is C_ARRAY
		throw new IllegalArgumentException();
	}
}
/**
 * Returns the array count (array nesting depth) of the given type signature.
 *
 * @param typeSignature the type signature
 * @return the array nesting depth, or 0 if not an array
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 */
public static int getArrayCount(String typeSignature) throws IllegalArgumentException {
	return getArrayCount(typeSignature.toCharArray());
}
/**
 * Returns the type signature without any array nesting.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getElementType({'[', '[', 'I'}) --> {'I'}.
 * </code>
 * </pre>
 * </p>
 *
 * @param typeSignature the type signature
 * @return the type signature without arrays
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 *
 *  
 */
public static char[] getElementType(char[] typeSignature) throws IllegalArgumentException {
	int count = getArrayCount(typeSignature);
	if (count == 0) return typeSignature;
	int length = typeSignature.length;
	char[] result = new char[length-count];
	System.arraycopy(typeSignature, count, result, 0, length-count);
	return result;
}
/**
 * Returns the type signature without any array nesting.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getElementType("[[I") --> "I".
 * </code>
 * </pre>
 * </p>
 *
 * @param typeSignature the type signature
 * @return the type signature without arrays
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 */
public static String getElementType(String typeSignature) throws IllegalArgumentException {
	return new String(getElementType(typeSignature.toCharArray()));
}
/**
 * Returns the number of parameter types in the given method signature.
 *
 * @param methodSignature the method signature
 * @return the number of parameters
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 *  
 */
public static int getParameterCount(char[] methodSignature) throws IllegalArgumentException {
	if (methodSignature==null)
		return 0;
	try {
		int count = 0;
		int i = CharOperation.indexOf(C_PARAM_START, methodSignature);
		if (i < 0) {
			throw new IllegalArgumentException();
		} else {
			i++;
		}
		for (;;) {
			if (methodSignature[i] == C_PARAM_END) {
				return count;
			}
			int e= Util.scanTypeSignature(methodSignature, i);
			if (e < 0) {
				throw new IllegalArgumentException();
			} else {
				i = e + 1;
			}
			count++;
		}
	} catch (ArrayIndexOutOfBoundsException e) {
		throw new IllegalArgumentException();
	}
}

/**
 * Returns the kind of type signature encoded by the given string.
 *
 * @param typeSignature the type signature string
 * @return the kind of type signature; one of the kind constants:
 * {@link #ARRAY_TYPE_SIGNATURE}, {@link #CLASS_TYPE_SIGNATURE},
 * {@link #BASE_TYPE_SIGNATURE}, or {@link #TYPE_VARIABLE_SIGNATURE},
 * or {@link #CAPTURE_TYPE_SIGNATURE}
 * @exception IllegalArgumentException if this is not a type signature
 *  
 */
public static int getTypeSignatureKind(char[] typeSignature) {
	// need a minimum 1 char
	if (typeSignature.length < 1) {
		// uknown return type
		throw new IllegalArgumentException();
	}
	char c = typeSignature[0];
	switch (c) {
		case C_ARRAY :
			return ARRAY_TYPE_SIGNATURE;
		case C_RESOLVED :
		case C_UNRESOLVED :
			return CLASS_TYPE_SIGNATURE;
		default :
			if ("BCDFIJSVZA".indexOf(c) >= 0) { //$NON-NLS-1$
				return BASE_TYPE_SIGNATURE;
			}			
			throw new IllegalArgumentException(String.valueOf(typeSignature));
	}
}

/**
 * Returns the kind of type signature encoded by the given string.
 *
 * @param typeSignature the type signature string
 * @return the kind of type signature; one of the kind constants:
 * {@link #ARRAY_TYPE_SIGNATURE}, {@link #CLASS_TYPE_SIGNATURE},
 * {@link #BASE_TYPE_SIGNATURE}, or {@link #TYPE_VARIABLE_SIGNATURE},
 * or {@link #CAPTURE_TYPE_SIGNATURE}
 * @exception IllegalArgumentException if this is not a type signature
 *  
 */
public static int getTypeSignatureKind(String typeSignature) {
	return getTypeSignatureKind(typeSignature.toCharArray());
}

/**
 * Returns the number of parameter types in the given method signature.
 *
 * @param methodSignature the method signature
 * @return the number of parameters
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 */
public static int getParameterCount(String methodSignature) throws IllegalArgumentException {
	return getParameterCount(methodSignature.toCharArray());
}

/**
 * Extracts the parameter type signatures from the given method signature.
 * The method signature is expected to be dot-based.
 *
 * @param methodSignature the method signature
 * @return the list of parameter type signatures
 * @exception IllegalArgumentException if the signature is syntactically
 *   incorrect
 *
 *  
 */
public static char[][] getParameterTypes(char[] methodSignature) throws IllegalArgumentException {
	try {
		int count = getParameterCount(methodSignature);
		char[][] result = new char[count][];
		if (count == 0) {
			return result;
		}
		int i = CharOperation.indexOf(C_PARAM_START, methodSignature);
		if (i < 0) {
			throw new IllegalArgumentException();
		} else {
			i++;
		}
		int t = 0;
		for (;;) {
			if (methodSignature[i] == C_PARAM_END) {
				return result;
			}
			int e = Util.scanTypeSignature(methodSignature, i);
			if (e < 0) {
				throw new IllegalArgumentException();
			}
			result[t] = CharOperation.subarray(methodSignature, i, e + 1);
			t++;
			i = e + 1;
		}
	} catch (ArrayIndexOutOfBoundsException e) {
		throw new IllegalArgumentException();
	}
}

/**
 * Extracts the parameter type signatures from the given method signature.
 * The method signature is expected to be dot-based.
 *
 * @param methodSignature the method signature
 * @return the list of parameter type signatures
 * @exception IllegalArgumentException if the signature is syntactically
 *   incorrect
 */
public static String[] getParameterTypes(String methodSignature) throws IllegalArgumentException {
	char[][] parameterTypes = getParameterTypes(methodSignature.toCharArray());
	return CharOperation.toStrings(parameterTypes);
}

/**
 * Extracts the type variable name from the given formal type parameter
 * signature. The signature is expected to be dot-based.
 *
 * @param formalTypeParameterSignature the formal type parameter signature
 * @return the name of the type variable
 * @exception IllegalArgumentException if the signature is syntactically
 *   incorrect
 *  
 */
public static String getTypeVariable(String formalTypeParameterSignature) throws IllegalArgumentException {
	return new String(getTypeVariable(formalTypeParameterSignature.toCharArray()));
}

/**
 * Extracts the type variable name from the given formal type parameter
 * signature. The signature is expected to be dot-based.
 *
 * @param formalTypeParameterSignature the formal type parameter signature
 * @return the name of the type variable
 * @exception IllegalArgumentException if the signature is syntactically
 *   incorrect
 *  
 */
public static char[] getTypeVariable(char[] formalTypeParameterSignature) throws IllegalArgumentException {
	int p = CharOperation.indexOf(C_COLON, formalTypeParameterSignature);
	if (p < 0) {
		// no ":" means can't be a formal type parameter signature
		throw new IllegalArgumentException();
	}
	return CharOperation.subarray(formalTypeParameterSignature, 0, p);
}

/**
 * Returns a char array containing all but the last segment of the given
 * dot-separated qualified name. Returns the empty char array if it is not qualified.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getQualifier({'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'O', 'b', 'j', 'e', 'c', 't'}) -> {'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g'}
 * getQualifier({'O', 'u', 't', 'e', 'r', '.', 'I', 'n', 'n', 'e', 'r'}) -> {'O', 'u', 't', 'e', 'r'}
 * getQualifier({'j', 'a', 'v', 'a', '.', 'u', 't', 'i', 'l', '.', 'L', 'i', 's', 't', '<', 'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g', '>'}) -> {'j', 'a', 'v', 'a', '.', 'u', 't', 'i', 'l'}
 * </code>
 * </pre>
 * </p>
 *
 * @param name the name
 * @return the qualifier prefix, or the empty char array if the name contains no
 *   dots
 * @exception NullPointerException if name is null
 *  
 */
public static char[] getQualifier(char[] name) {
	int lastDot = CharOperation.lastIndexOf(C_DOT, name, 0, name.length-1);
	if (lastDot == -1) {
		return CharOperation.NO_CHAR;
	}
	return CharOperation.subarray(name, 0, lastDot);
}
/**
 * Returns a string containing all but the last segment of the given
 * dot-separated qualified name. Returns the empty string if it is not qualified.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getQualifier("java.lang.Object") -&gt; "java.lang"
 * getQualifier("Outer.Inner") -&gt; "Outer"
 * getQualifier("java.util.List&lt;java.lang.String&gt;") -&gt; "java.util"
 * </code>
 * </pre>
 * </p>
 *
 * @param name the name
 * @return the qualifier prefix, or the empty string if the name contains no
 *   dots
 * @exception NullPointerException if name is null
 */
public static String getQualifier(String name) {
	char[] qualifier = getQualifier(name.toCharArray());
	if (qualifier.length == 0) return org.eclipse.wst.jsdt.internal.compiler.util.Util.EMPTY_STRING;
	return new String(qualifier);
}
/**
 * Extracts the return type from the given method signature. The method signature is
 * expected to be dot-based.
 *
 * @param methodSignature the method signature
 * @return the type signature of the return type
 * @exception IllegalArgumentException if the signature is syntactically
 *   incorrect
 *
 *  
 */
public static char[] getReturnType(char[] methodSignature) throws IllegalArgumentException {
	// skip type parameters
	if (methodSignature==null)
		return CharOperation.NO_CHAR;
	int paren = CharOperation.lastIndexOf(C_PARAM_END, methodSignature);
	if (paren == -1) {
		// could not be determined
		return CharOperation.NO_CHAR;
	}
	// there could be thrown exceptions behind, thus scan one type exactly
	int last = Util.scanTypeSignature(methodSignature, paren+1);
	return CharOperation.subarray(methodSignature, paren + 1, last+1);
}
/**
 * Extracts the return type from the given method signature. The method signature is
 * expected to be dot-based.
 *
 * @param methodSignature the method signature
 * @return the type signature of the return type
 * @exception IllegalArgumentException if the signature is syntactically
 *   incorrect
 */
public static String getReturnType(String methodSignature) throws IllegalArgumentException {
	return new String(getReturnType(methodSignature.toCharArray()));
}
/**
 * Returns package fragment of a type signature. The package fragment separator must be '.'
 * and the type fragment separator must be '$'.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSignatureQualifier({'L', 'j', 'a', 'v', 'a', '.', 'u', 't', 'i', 'l', '.', 'M', 'a', 'p', '$', 'E', 'n', 't', 'r', 'y', ';'}) -> {'j', 'a', 'v', 'a', '.', 'u', 't', 'i', 'l'}
 * </code>
 * </pre>
 * </p>
 *
 * @param typeSignature the type signature
 * @return the package fragment (separators are '.')
 *  
 */
public static char[] getSignatureQualifier(char[] typeSignature) {
	if(typeSignature == null) return CharOperation.NO_CHAR;

	char[] qualifiedType = Signature.toCharArray(typeSignature);

	int dotCount = 0;
	indexFound: for(int i = 0; i < typeSignature.length; i++) {
		switch(typeSignature[i]) {
			case C_DOT:
				dotCount++;
				break;
			case C_DOLLAR:
				break indexFound;
		}
	}

	if(dotCount > 0) {
		for(int i = 0; i < qualifiedType.length; i++) {
			if(qualifiedType[i] == '.') {
				dotCount--;
			}
			if(dotCount <= 0) {
				return CharOperation.subarray(qualifiedType, 0, i);
			}
		}
	}
	return CharOperation.NO_CHAR;
}
/**
 * Returns package fragment of a type signature. The package fragment separator must be '.'
 * and the type fragment separator must be '$'.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSignatureQualifier("Ljava.util.Map$Entry") -> "java.util"
 * </code>
 * </pre>
 * </p>
 *
 * @param typeSignature the type signature
 * @return the package fragment (separators are '.')
 *  
 */
public static String getSignatureQualifier(String typeSignature) {
	return new String(getSignatureQualifier(typeSignature == null ? null : typeSignature.toCharArray()));
}
/**
 * Returns type fragment of a type signature. The package fragment separator must be '.'
 * and the type fragment separator must be '$'.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSignatureSimpleName({'L', 'j', 'a', 'v', 'a', '.', 'u', 't', 'i', 'l', '.', 'M', 'a', 'p', '$', 'E', 'n', 't', 'r', 'y', ';'}) -> {'M', 'a', 'p', '.', 'E', 'n', 't', 'r', 'y'}
 * </code>
 * </pre>
 * </p>
 *
 * @param typeSignature the type signature
 * @return the type fragment (separators are '.')
 *  
 */
public static char[] getSignatureSimpleName(char[] typeSignature) {
	if(typeSignature == null) return CharOperation.NO_CHAR;

	char[] qualifiedType = Signature.toCharArray(typeSignature);

//	int dotCount = 0;
//	indexFound: for(int i = 0; i < typeSignature.length; i++) {
//		switch(typeSignature[i]) {
//			case C_DOT:
//				dotCount++;
//				break;
//			case C_GENERIC_START:
//				break indexFound;
//			case C_DOLLAR:
//				break indexFound;
//		}
//	}
//
//	if(dotCount > 0) {
//		for(int i = 0; i < qualifiedType.length; i++) {
//			if(qualifiedType[i] == '.') {
//				dotCount--;
//			}
//			if(dotCount <= 0) {
//				return CharOperation.subarray(qualifiedType, i + 1, qualifiedType.length);
//			}
//		}
//	}
	return qualifiedType;
}
/**
 * Returns type fragment of a type signature. The package fragment separator must be '.'
 * and the type fragment separator must be '$'.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSignatureSimpleName("Ljava.util.Map$Entry") -> "Map.Entry"
 * </code>
 * </pre>
 * </p>
 *
 * @param typeSignature the type signature
 * @return the type fragment (separators are '.')
 *  
 */
public static String getSignatureSimpleName(String typeSignature) {
	return new String(getSignatureSimpleName(typeSignature == null ? null : typeSignature.toCharArray()));
}

/**
 * Returns the last segment of the given dot-separated qualified name.
 * Returns the given name if it is not qualified.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSimpleName({'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'O', 'b', 'j', 'e', 'c', 't'}) -> {'O', 'b', 'j', 'e', 'c', 't'}
 * </code>
 * </pre>
 * </p>
 *
 * @param name the name
 * @return the last segment of the qualified name
 * @exception NullPointerException if name is null
 *  
 */
public static char[] getSimpleName(char[] name) {

	int lastDot = -1;
	int depth = 0;
	int length = name.length;
	lastDotLookup: for (int i = length -1; i >= 0; i--) {
		switch (name[i]) {
			case '.':
				if (depth == 0) {
					lastDot = i;
					break lastDotLookup;
				}
				break;
		}
	}
	
	if (lastDot < 0) {
		return name;
	}
	return  CharOperation.subarray(name, lastDot + 1, length);
}
/**
 * Returns the last segment of the given dot-separated qualified name.
 * Returns the given name if it is not qualified.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSimpleName("java.lang.Object") -&gt; "Object"
 * </code>
 * <code>
 * getSimpleName("java.util.Map&lt;java.lang.String, java.lang.Object&gt;") -&gt; "Map&lt;String,Object&gt;"
 * </code>
 * </pre>
 * </p>
 *
 * @param name the name
 * @return the last segment of the qualified name
 * @exception NullPointerException if name is null
 */
public static String getSimpleName(String name) {
	int lastDot = -1;
	int depth = 0;
	int length = name.length();
	lastDotLookup: for (int i = length -1; i >= 0; i--) {
		switch (name.charAt(i)) {
			case '.':
				if (depth == 0) {
					lastDot = i;
					break lastDotLookup;
				}
				break;
		}
	}
	if (lastDot < 0) {
		return name;
	}
	return name.substring(lastDot + 1, length);
}

/**
 * Returns all segments of the given dot-separated qualified name.
 * Returns an array with only the given name if it is not qualified.
 * Returns an empty array if the name is empty.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSimpleNames({'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'O', 'b', 'j', 'e', 'c', 't'}) -> {{'j', 'a', 'v', 'a'}, {'l', 'a', 'n', 'g'}, {'O', 'b', 'j', 'e', 'c', 't'}}
 * getSimpleNames({'O', 'b', 'j', 'e', 'c', 't'}) -> {{'O', 'b', 'j', 'e', 'c', 't'}}
 * getSimpleNames({}) -> {}
 * getSimpleNames({'j', 'a', 'v', 'a', '.', 'u', 't', 'i', 'l', '.', 'L', 'i', 's', 't', '<', 'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g', '>'}) -> {{'j', 'a', 'v', 'a'}, {'l', 'a', 'n', 'g'}, {'L', 'i', 's', 't', '<', 'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g'}}
 * </code>
 * </pre>
 *
 * @param name the name
 * @return the list of simple names, possibly empty
 * @exception NullPointerException if name is null
 *  
 */
public static char[][] getSimpleNames(char[] name) {
	int length = name == null ? 0 : name.length;
	if (length == 0)
		return CharOperation.NO_CHAR_CHAR;

	int wordCount = 1;
	for (int i = 0; i < length; i++)
		switch(name[i]) {
			case C_DOT:
				wordCount++;
				break;
		}
	char[][] split = new char[wordCount][];
	int last = 0, currentWord = 0;
	for (int i = 0; i < length; i++) {
		if (name[i] == C_DOT) {
			split[currentWord] = new char[i - last];
			System.arraycopy(
				name,
				last,
				split[currentWord++],
				0,
				i - last);
			last = i + 1;
		}
	}
	split[currentWord] = new char[length - last];
	System.arraycopy(name, last, split[currentWord], 0, length - last);
	return split;
}
/**
 * Returns all segments of the given dot-separated qualified name.
 * Returns an array with only the given name if it is not qualified.
 * Returns an empty array if the name is empty.
 * <p>
 * For example:
 * <pre>
 * <code>
 * getSimpleNames("java.lang.Object") -&gt; {"java", "lang", "Object"}
 * getSimpleNames("Object") -&gt; {"Object"}
 * getSimpleNames("") -&gt; {}
 * getSimpleNames("java.util.List&lt;java.lang.String&gt;") -&gt;
 *   {"java", "util", "List&lt;java.lang.String&gt;"}
 * </code>
 * </pre>
 *
 * @param name the name
 * @return the list of simple names, possibly empty
 * @exception NullPointerException if name is null
 */
public static String[] getSimpleNames(String name) {
	return CharOperation.toStrings(getSimpleNames(name.toCharArray()));
}

/**
 * Converts the given method signature to a readable form. The method signature is expected to
 * be dot-based.
 * <p>
 * For example:
 * <pre>
 * <code>
 * toString("([Ljava.lang.String;)V", "main", new String[] {"args"}, false, true) -> "void main(String[] args)"
 * </code>
 * </pre>
 * </p>
 *
 * @param methodSignature the method signature to convert
 * @param methodName the name of the method to insert in the result, or
 *   <code>null</code> if no method name is to be included
 * @param parameterNames the parameter names to insert in the result, or
 *   <code>null</code> if no parameter names are to be included; if supplied,
 *   the number of parameter names must match that of the method signature
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param includeReturnType <code>true</code> if the return type is to be
 *   included
 * @return the char array representation of the method signature
 *
 *  
 */
public static char[] toCharArray(char[] methodSignature, char[] methodName, char[][] parameterNames, boolean fullyQualifyTypeNames, boolean includeReturnType) {
	return toCharArray(methodSignature, methodName, parameterNames, fullyQualifyTypeNames, includeReturnType, false);
}
/**
 * Converts the given method signature to a readable form. The method signature is expected to
 * be dot-based.
 * <p>
 * For example:
 * <pre>
 * <code>
 * toString("([Ljava.lang.String;)V", "main", new String[] {"args"}, false, true) -> "void main(String[] args)"
 * </code>
 * </pre>
 * </p>
 *
 * @param methodSignature the method signature to convert
 * @param methodName the name of the method to insert in the result, or
 *   <code>null</code> if no method name is to be included
 * @param parameterNames the parameter names to insert in the result, or
 *   <code>null</code> if no parameter names are to be included; if supplied,
 *   the number of parameter names must match that of the method signature
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param includeReturnType <code>true</code> if the return type is to be
 *   included
 * @param isVargArgs <code>true</code> if the last argument should be displayed as a
 * variable argument,  <code>false</code> otherwise.
 * @return the char array representation of the method signature
 *
 *  
 */
public static char[] toCharArray(char[] methodSignature, char[] methodName, char[][] parameterNames, boolean fullyQualifyTypeNames, boolean includeReturnType, boolean isVargArgs) {
	int firstParen = CharOperation.indexOf(C_PARAM_START, methodSignature);
	if (firstParen == -1) {
		throw new IllegalArgumentException();
	}

	StringBuffer buffer = new StringBuffer(methodSignature.length + 10);

	// return type
	if (includeReturnType) {
		char[] rts = getReturnType(methodSignature);
		appendTypeSignature(rts, 0 , fullyQualifyTypeNames, buffer);
		buffer.append(' ');
	}

	// selector
	if (methodName != null) {
		buffer.append(methodName);
	}

	// parameters
	buffer.append('(');
	char[][] pts = getParameterTypes(methodSignature);
	for (int i = 0, max = pts.length; i < max; i++) {
		if (i == max - 1) {
			appendTypeSignature(pts[i], 0 , fullyQualifyTypeNames, buffer, isVargArgs);
		} else {
			appendTypeSignature(pts[i], 0 , fullyQualifyTypeNames, buffer);
		}
		if (parameterNames != null) {
			buffer.append(' ');
			buffer.append(parameterNames[i]);
		}
		if (i != pts.length - 1) {
			buffer.append(',');
			buffer.append(' ');
		}
	}
	buffer.append(')');
	char[] result = new char[buffer.length()];
	buffer.getChars(0, buffer.length(), result, 0);
	return result;
}
/**
 * Converts the given type signature to a readable string. The signature is expected to
 * be dot-based.
 *
 * <p>
 * For example:
 * <pre>
 * <code>
 * toString({'[', 'L', 'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g', ';'}) -> {'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g', '[', ']'}
 * toString({'I'}) -> {'i', 'n', 't'}
 * toString({'+', 'L', 'O', 'b', 'j', 'e', 'c', 't', ';'}) -> {'?', ' ', 'e', 'x', 't', 'e', 'n', 'd', 's', ' ', 'O', 'b', 'j', 'e', 'c', 't'}
 * </code>
 * </pre>
 * </p>
 * <p>
 * Note: This method assumes that a type signature containing a <code>'$'</code>
 * is an inner type signature. While this is correct in most cases, someone could
 * define a non-inner type name containing a <code>'$'</code>. Handling this
 * correctly in all cases would have required resolving the signature, which
 * generally not feasible.
 * </p>
 *
 * @param signature the type signature
 * @return the string representation of the type
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 *
 *  
 */
public static char[] toCharArray(char[] signature) throws IllegalArgumentException {
		int sigLength = signature.length;
		if (sigLength == 0 || signature[0] == C_PARAM_START) {
			return toCharArray(signature, CharOperation.NO_CHAR, null, true, true);
		}

		StringBuffer buffer = new StringBuffer(signature.length + 10);
		appendTypeSignature(signature, 0, true, buffer);
		char[] result = new char[buffer.length()];
		buffer.getChars(0, buffer.length(), result, 0);
		return result;
}

/**
 * Scans the given string for a type signature starting at the given
 * index and appends it to the given buffer, and returns the index of the last
 * character.
 *
 * @param string the signature string
 * @param start the 0-based character index of the first character
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param buffer the string buffer to append to
 * @return the 0-based character index of the last character
 * @exception IllegalArgumentException if this is not a type signature
 * @see Util#scanTypeSignature(char[], int)
 */
private static int appendTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
	return appendTypeSignature(string, start, fullyQualifyTypeNames, buffer, false);
}
/**
 * Scans the given string for a type signature starting at the given
 * index and appends it to the given buffer, and returns the index of the last
 * character.
 *
 * @param string the signature string
 * @param start the 0-based character index of the first character
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param buffer the string buffer to append to
 * @param isVarArgs <code>true</code> if the type must be displayed as a
 * variable argument, <code>false</code> otherwise. In this case, the type must be an array type
 * @return the 0-based character index of the last character
 * @exception IllegalArgumentException if this is not a type signature, or if isVarArgs is <code>true</code>,
 * and the type is not an array type signature.
 * @see Util#scanTypeSignature(char[], int)
 */
private static int appendTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer, boolean isVarArgs) {
	// need a minimum 1 char
	if (start >= string.length) {
		throw new IllegalArgumentException();
	}
	char c = string[start];
	if (isVarArgs) {
		switch (c) {
			case C_ARRAY :
				return appendArrayTypeSignature(string, start, fullyQualifyTypeNames, buffer, true);
			case C_RESOLVED :
			case C_UNRESOLVED :
			case C_VOID :
			default:
				throw new IllegalArgumentException(); // a var args is an array type
		}
	} else {
		switch (c) {
			case C_ARRAY :
				return appendArrayTypeSignature(string, start, fullyQualifyTypeNames, buffer);
			case C_RESOLVED :
			case C_UNRESOLVED :
				return appendClassTypeSignature(string, start, fullyQualifyTypeNames, buffer);
			case C_COMPILATION_UNIT :
				return appendCompilationUnitSignature(string, start, fullyQualifyTypeNames, buffer);
			case C_ANY :
				buffer.append(ANY);
				return start;
			case C_VOID :
				buffer.append(VOID);
				return start;
			default :
				int result = appendBaseTypeSignature(string, start, fullyQualifyTypeNames, buffer);
				if (result == -1) {
					/* either the string is not formated as a signature, or we do not know
					 * how to handle it, so just return it, this is preferable to throwing
					 * an unnecessary exception
					 */
					buffer.append(string);
				}
				return start;
		}
	}
}

private static int appendBaseTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
	// Need a minimum one char 
	if (start >= string.length) { // Do not throw unnecessary exception here
		return -1;
	}
	// Base type should be exactly one character lenght
	if (start + 1 < string.length && Character.isJavaIdentifierPart(string[start + 1])) {
		return -1;
	}
	
	// must start in any of 'B'(?), 'C', 'D', 'F', 'I', 'J', 'S', 'Z'
	char c = string[start];
	String typeName = (String)BASE_TYPES.get(c);
	if (typeName == null)
		return -1;
	buffer.append(typeName);
	return start;
}


private static int appendCompilationUnitSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
	// need a minimum 3 chars "Lx;"
	if (start >= string.length - 2) {
		throw new IllegalArgumentException();
	}
	// must start in "L" or "Q"
	char c = string[start];
	if (c != C_COMPILATION_UNIT) {
		throw new IllegalArgumentException();
	}
	int p = start + 1;
	while (true) {
		if (p >= string.length) {
			throw new IllegalArgumentException();
		}
		c = string[p];
		switch(c) {
			case C_SEMICOLON :
				// all done
				return p;
			 default :
				buffer.append(c);
		}
		p++;
	}
}



/**
 * Scans the given string for an array type signature starting at the given
 * index and appends it to the given buffer, and returns the index of the last
 * character.
 *
 * @param string the signature string
 * @param start the 0-based character index of the first character
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @return the 0-based character index of the last character
 * @exception IllegalArgumentException if this is not an array type signature
 * @see Util#scanArrayTypeSignature(char[], int)
 */
private static int appendArrayTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
	return appendArrayTypeSignature(string, start, fullyQualifyTypeNames, buffer, false);
}

/**
 * Scans the given string for an array type signature starting at the given
 * index and appends it to the given buffer, and returns the index of the last
 * character.
 *
 * @param string the signature string
 * @param start the 0-based character index of the first character
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param isVarArgs <code>true</code> if the array type must be displayed as a
 * variable argument, <code>false</code> otherwise
 * @return the 0-based character index of the last character
 * @exception IllegalArgumentException if this is not an array type signature
 * @see Util#scanArrayTypeSignature(char[], int)
 */
private static int appendArrayTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer, boolean isVarArgs) {
	int length = string.length;
	// need a minimum 2 char
	if (start >= length - 1) {
		throw new IllegalArgumentException();
	}
	char c = string[start];
	if (c != C_ARRAY) {
		throw new IllegalArgumentException();
	}

	int index = start;
	c = string[++index];
	while(c == C_ARRAY) {
		// need a minimum 2 char
		if (index >= length - 1) {
			throw new IllegalArgumentException();
		}
		c = string[++index];
	}

	int e = appendTypeSignature(string, index, fullyQualifyTypeNames, buffer);

	for(int i = 1, dims = index - start; i < dims; i++) {
		buffer.append('[').append(']');
	}

	if (isVarArgs) {
		buffer.append('.').append('.').append('.');
	} else {
		buffer.append('[').append(']');
	}
	return e;
}
/**
 * Scans the given string for a class type signature starting at the given
 * index and appends it to the given buffer, and returns the index of the last
 * character.
 *
 * @param string the signature string
 * @param start the 0-based character index of the first character
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param buffer the string buffer to append to
 * @return the 0-based character index of the last character
 * @exception IllegalArgumentException if this is not a class type signature
 * @see Util#scanClassTypeSignature(char[], int)
 */
private static int appendClassTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
	// need a minimum 3 chars "Lx;"
	if (start >= string.length - 2) {
		throw new IllegalArgumentException();
	}
	// must start in "L" or "Q"
	char c = string[start];
	if (c != C_RESOLVED && c != C_UNRESOLVED) {
		throw new IllegalArgumentException();
	}
	boolean resolved = (c == C_RESOLVED);
	boolean removePackageQualifiers = !fullyQualifyTypeNames;
	if (!resolved) {
		// keep everything in an unresolved name
		removePackageQualifiers = false;
	}
	int p = start + 1;
	int checkpoint = buffer.length();
	int innerTypeStart = -1;
	boolean inAnonymousType = false;
	while (true) {
		if (p >= string.length) {
			throw new IllegalArgumentException();
		}
		c = string[p];
		switch(c) {
			case C_SEMICOLON :
				// all done
				return p;
			case C_DOT :
				if (removePackageQualifiers) {
					// erase package prefix
					buffer.setLength(checkpoint);
				} else {
					buffer.append('.');
				}
				break;
			 case '/' :
				if (removePackageQualifiers) {
					// erase package prefix
					buffer.setLength(checkpoint);
				} else {
					buffer.append('/');
				}
				break;
			 default :
				if (innerTypeStart != -1 && !inAnonymousType && Character.isDigit(c)) {
					inAnonymousType = true;
					buffer.setLength(innerTypeStart); // remove '.'
					buffer.insert(checkpoint, "new "); //$NON-NLS-1$
					buffer.append("(){}"); //$NON-NLS-1$
				}
			 	if (!inAnonymousType)
					buffer.append(c);
				innerTypeStart = -1;
		}
		p++;
	}
}

/**
 * Converts the given array of qualified name segments to a qualified name.
 * <p>
 * For example:
 * <pre>
 * <code>
 * toQualifiedName({{'j', 'a', 'v', 'a'}, {'l', 'a', 'n', 'g'}, {'O', 'b', 'j', 'e', 'c', 't'}}) -> {'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'O', 'b', 'j', 'e', 'c', 't'}
 * toQualifiedName({{'O', 'b', 'j', 'e', 'c', 't'}}) -> {'O', 'b', 'j', 'e', 'c', 't'}
 * toQualifiedName({{}}) -> {}
 * </code>
 * </pre>
 * </p>
 *
 * @param segments the list of name segments, possibly empty
 * @return the dot-separated qualified name, or the empty string
 *
 *  
 */
public static char[] toQualifiedName(char[][] segments) {
	int length = segments.length;
	if (length == 0) return CharOperation.NO_CHAR;
	if (length == 1) return segments[0];

	int resultLength = 0;
	for (int i = 0; i < length; i++) {
		resultLength += segments[i].length+1;
	}
	resultLength--;
	char[] result = new char[resultLength];
	int index = 0;
	for (int i = 0; i < length; i++) {
		char[] segment = segments[i];
		int segmentLength = segment.length;
		System.arraycopy(segment, 0, result, index, segmentLength);
		index += segmentLength;
		if (i != length-1) {
			result[index++] = C_DOT;
		}
	}
	return result;
}
/**
 * Converts the given array of qualified name segments to a qualified name.
 * <p>
 * For example:
 * <pre>
 * <code>
 * toQualifiedName(new String[] {"java", "lang", "Object"}) -> "java.lang.Object"
 * toQualifiedName(new String[] {"Object"}) -> "Object"
 * toQualifiedName(new String[0]) -> ""
 * </code>
 * </pre>
 * </p>
 *
 * @param segments the list of name segments, possibly empty
 * @return the dot-separated qualified name, or the empty string
 */
public static String toQualifiedName(String[] segments) {
	int length = segments.length;
	char[][] charArrays = new char[length][];
	for (int i = 0; i < length; i++) {
		charArrays[i] = segments[i].toCharArray();
	}
	return new String(toQualifiedName(charArrays));
}
/**
 * Converts the given type signature to a readable string. The signature is expected to
 * be dot-based.
 *
 * <p>
 * For example:
 * <pre>
 * <code>
 * toString("[Ljava.lang.String;") -> "java.lang.String[]"
 * toString("I") -> "int"
 * toString("+QObject;") -> "? extends Object"
 * </code>
 * </pre>
 * </p>
 * <p>
 * Note: This method assumes that a type signature containing a <code>'$'</code>
 * is an inner type signature. While this is correct in most cases, someone could
 * define a non-inner type name containing a <code>'$'</code>. Handling this
 * correctly in all cases would have required resolving the signature, which
 * generally not feasible.
 * </p>
 *
 * @param signature the type signature
 * @return the string representation of the type
 * @exception IllegalArgumentException if the signature is not syntactically
 *   correct
 */
public static String toString(String signature) throws IllegalArgumentException {
	return new String(toCharArray(signature.toCharArray()));
}
/**
 * Converts the given method signature to a readable string. The method signature is expected to
 * be dot-based.
 *
 * @param methodSignature the method signature to convert
 * @param methodName the name of the method to insert in the result, or
 *   <code>null</code> if no method name is to be included
 * @param parameterNames the parameter names to insert in the result, or
 *   <code>null</code> if no parameter names are to be included; if supplied,
 *   the number of parameter names must match that of the method signature
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param includeReturnType <code>true</code> if the return type is to be
 *   included
 * @see #toCharArray(char[], char[], char[][], boolean, boolean)
 * @return the string representation of the method signature
 */
public static String toString(String methodSignature, String methodName, String[] parameterNames, boolean fullyQualifyTypeNames, boolean includeReturnType) {
	return toString(methodSignature, methodName, parameterNames, fullyQualifyTypeNames, includeReturnType, false);
}
/**
 * Converts the given method signature to a readable string. The method signature is expected to
 * be dot-based.
 *
 * @param methodSignature the method signature to convert
 * @param methodName the name of the method to insert in the result, or
 *   <code>null</code> if no method name is to be included
 * @param parameterNames the parameter names to insert in the result, or
 *   <code>null</code> if no parameter names are to be included; if supplied,
 *   the number of parameter names must match that of the method signature
 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
 *   qualified, and <code>false</code> to use only simple names
 * @param includeReturnType <code>true</code> if the return type is to be
 *   included
 * @param isVarArgs <code>true</code> if the last argument should be displayed as a
 * variable argument, <code>false</code> otherwise
 * @see #toCharArray(char[], char[], char[][], boolean, boolean)
 * @return the string representation of the method signature
 *
 *  
 */
public static String toString(String methodSignature, String methodName, String[] parameterNames, boolean fullyQualifyTypeNames, boolean includeReturnType, boolean isVarArgs) {
	char[][] params;
	if (parameterNames == null) {
		params = null;
	} else {
		int paramLength = parameterNames.length;
		params = new char[paramLength][];
		for (int i = 0; i < paramLength; i++) {
			params[i] = parameterNames[i].toCharArray();
		}
	}
	return new String(toCharArray(methodSignature.toCharArray(), methodName == null ? null : methodName.toCharArray(), params, fullyQualifyTypeNames, includeReturnType, isVarArgs));
}
}
