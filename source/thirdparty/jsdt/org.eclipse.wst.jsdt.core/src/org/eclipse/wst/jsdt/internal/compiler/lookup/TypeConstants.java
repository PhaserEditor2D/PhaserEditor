/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

// TODO should rename into TypeNames (once extracted last non name constants)
public interface TypeConstants {

	char[] PROTOTYPE = "prototype".toCharArray(); //$NON-NLS-1$
	char[] JAVA = "java".toCharArray(); //$NON-NLS-1$
	char[] SYSTEMJS = "system.js".toCharArray(); //$NON-NLS-1$
	char[] LANG = "lang".toCharArray(); //$NON-NLS-1$
	char[] UTIL = "util".toCharArray(); //$NON-NLS-1$
	char[] REFLECT = "reflect".toCharArray(); //$NON-NLS-1$
	char[] LENGTH = "length".toCharArray(); //$NON-NLS-1$
	char[] GETCLASS = "getClass".toCharArray(); //$NON-NLS-1$
	char[] OBJECT = "Object".toCharArray(); //$NON-NLS-1$
	char[] READRESOLVE = "readResolve".toCharArray(); //$NON-NLS-1$
	char[] WRITEREPLACE = "writeReplace".toCharArray(); //$NON-NLS-1$
	char[] READOBJECT = "readObject".toCharArray(); //$NON-NLS-1$
	char[] WRITEOBJECT = "writeObject".toCharArray(); //$NON-NLS-1$
	char[] CharArray_JAVA_LANG_OBJECT = "java.lang.Object".toCharArray(); //$NON-NLS-1$
	char[] ANONYM_PREFIX = "new ".toCharArray(); //$NON-NLS-1$
	char[] ANONYM_SUFFIX = "(){}".toCharArray(); //$NON-NLS-1$
	char[] SHORT = "short".toCharArray(); //$NON-NLS-1$
	char[] INT = "int".toCharArray(); //$NON-NLS-1$
	char[] LONG = "long".toCharArray(); //$NON-NLS-1$
	char[] FLOAT = "float".toCharArray(); //$NON-NLS-1$
	char[] DOUBLE = "double".toCharArray(); //$NON-NLS-1$
	char[] CHAR = "char".toCharArray(); //$NON-NLS-1$
	char[] BOOLEAN = "boolean".toCharArray(); //$NON-NLS-1$
	char[] NULL = "null".toCharArray(); //$NON-NLS-1$
	char[] VOID = "void".toCharArray(); //$NON-NLS-1$
    char[] VALUE = "value".toCharArray(); //$NON-NLS-1$
    char[] VALUES = "values".toCharArray(); //$NON-NLS-1$
    char[] VALUEOF = "valueOf".toCharArray(); //$NON-NLS-1$
    char[] UPPER_SOURCE = "SOURCE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_CLASS = "CLASS".toCharArray(); //$NON-NLS-1$
    char[] UPPER_RUNTIME = "RUNTIME".toCharArray(); //$NON-NLS-1$
    char[] TYPE = "TYPE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_FIELD = "FIELD".toCharArray(); //$NON-NLS-1$
    char[] UPPER_METHOD = "METHOD".toCharArray(); //$NON-NLS-1$
    char[] UPPER_PARAMETER = "PARAMETER".toCharArray(); //$NON-NLS-1$
    char[] UPPER_CONSTRUCTOR = "CONSTRUCTOR".toCharArray(); //$NON-NLS-1$
    char[] UPPER_LOCAL_VARIABLE = "LOCAL_VARIABLE".toCharArray(); //$NON-NLS-1$
    char[] UPPER_PACKAGE = "PACKAGE".toCharArray(); //$NON-NLS-1$
	char[] UNDEFINED = "undefined".toCharArray(); //$NON-NLS-1$
	char[] ANY = "any".toCharArray(); //$NON-NLS-1$

	// Constant compound names
	char[][] JAVA_LANG = {JAVA, LANG};
	char[][] JAVA_LANG_ASSERTIONERROR = {JAVA, LANG, "AssertionError".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_CLASS = {JAVA, LANG, "Class".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_EXCEPTION = {JAVA, LANG, "Exception".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ERROR = {JAVA, LANG, "Error".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ILLEGALARGUMENTEXCEPTION = {JAVA, LANG, "IllegalArgumentException".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_ITERABLE = {JAVA, LANG, "Iterable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_OBJECT = {/*JAVA, LANG, SYSTEMJS, */OBJECT};
	char[][] JAVA_LANG_STRING = {/*JAVA, LANG, SYSTEMJS,*/ "String".toCharArray()}; //$NON-NLS-1$
	char[][] NUMBER = {/*JAVA, LANG, SYSTEMJS,*/  "Number".toCharArray()}; //$NON-NLS-1$
	char[][] FUNCTION = {/*JAVA, LANG, SYSTEMJS,*/  "Function".toCharArray()}; //$NON-NLS-1$
	char[][] BOOLEAN_OBJECT = {/*JAVA, LANG, SYSTEMJS,*/  "Boolean".toCharArray()}; //$NON-NLS-1$
	char[][] ARRAY = {/*JAVA, LANG, SYSTEMJS, */ "Array".toCharArray()}; //$NON-NLS-1$
	char[][] REGEXP = {/*JAVA, LANG, SYSTEMJS, */ "RegExp".toCharArray()}; //$NON-NLS-1$
	char[][] ERROR = {/*JAVA, LANG, SYSTEMJS,*/  "Error".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_SYSTEM = {JAVA, LANG, "System".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_RUNTIMEEXCEPTION = {JAVA, LANG, "RuntimeException".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_THROWABLE = {JAVA, LANG, "Throwable".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_SHORT = {JAVA, LANG, "Short".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_CHARACTER = {JAVA, LANG, "Character".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_INTEGER = {JAVA, LANG, "Integer".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_LONG = {JAVA, LANG, "Long".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_FLOAT = {JAVA, LANG, "Float".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_DOUBLE = {JAVA, LANG, "Double".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_BOOLEAN = {JAVA, LANG, "Boolean".toCharArray()}; //$NON-NLS-1$
	char[][] JAVA_LANG_VOID = {JAVA, LANG, "Void".toCharArray()}; //$NON-NLS-1$

	// Constraints for generic type argument inference
    int CONSTRAINT_EQUAL = 0;		// Actual = Formal
    int CONSTRAINT_EXTENDS = 1;	// Actual << Formal
    int CONSTRAINT_SUPER = 2;		// Actual >> Formal

	// Constants used to perform bound checks
	int OK = 0;
	int UNCHECKED = 1;
	int MISMATCH = 2;

	// Synthetics
	char[] INIT = "<init>".toCharArray(); //$NON-NLS-1$
	char[] CLINIT = "<clinit>".toCharArray(); //$NON-NLS-1$

	// synthetic package-info name
	public static final char[] PACKAGE_INFO_NAME = "package-info".toCharArray(); //$NON-NLS-1$
}
