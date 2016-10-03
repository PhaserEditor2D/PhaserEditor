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

public interface TypeIds {

    //base type void null undefined Object String
	//should have an id that is 0<= id <= 15
    // The IDs below must be representable using 4 bits so as to fit in operator signatures.
	final int T_undefined = 0; // should not be changed
	final int T_JavaLangObject = 1;
	final int T_char = 2;
	final int T_short = 4;
	final int T_boolean = 5;
	final int T_void = 6;
	final int T_long = 7;
	final int T_double = 8;
	final int T_float = 9;
	final int T_int = 10;
	final int T_JavaLangString = 11;
	final int T_null = 12;
	final int T_any = 13;
	final int T_function = 14;

	final int T_last_basic = 14;


	//=========end of 4 bits constraint===========

	// well-known exception types
	final int T_JavaLangClass = 16;
	final int T_JavaLangStringBuffer = 17;
	final int T_JavaLangSystem = 18;
	final int T_JavaLangError = 19;
	final int T_JavaLangThrowable = 21;
	final int T_JavaLangNoClassDefError = 22;
	final int T_JavaLangClassNotFoundException = 23;
	final int T_JavaLangRuntimeException = 24;
	final int T_JavaLangException = 25;

	// wrapper types
	final int T_JavaLangShort = 27;
	final int T_JavaLangCharacter = 28;
	final int T_JavaLangInteger = 29;
	final int T_JavaLangLong = 30;
	final int T_JavaLangFloat = 31;
	final int T_JavaLangDouble = 32;
	final int T_JavaLangBoolean = 33;
	final int T_JavaLangVoid = 34;

	// 1.4 features
	final int T_JavaLangAssertionError = 35;

	// 1.5 features
	final int T_JavaLangIterable = 38;
	final int T_JavaUtilIterator = 39;
	final int T_JavaLangStringBuilder = 40;
	final int T_JavaLangIllegalArgumentException = 42;
	final int T_JavaLangDeprecated = 44;
	final int T_JavaLangOverride = 47;
	final int T_JavaLangSuppressWarnings = 49;

	final int NoId = Integer.MAX_VALUE;

	public static final int IMPLICIT_CONVERSION_MASK = 0xFF;
	public static final int COMPILE_TYPE_MASK = 0xF;

	// implicit conversions: <compileType> to <runtimeType>  (note: booleans are integers at runtime)
	final int Boolean2Int = T_boolean + (T_int << 4);
	final int Boolean2String = T_boolean + (T_JavaLangString << 4);
	final int Boolean2Boolean = T_boolean + (T_boolean << 4);
	final int Short2Short = T_short + (T_short << 4);
	final int Short2Char = T_short + (T_char << 4);
	final int Short2Int = T_short + (T_int << 4);
	final int Short2Long = T_short + (T_long << 4);
	final int Short2Float = T_short + (T_float << 4);
	final int Short2Double = T_short + (T_double << 4);
	final int Short2String = T_short + (T_JavaLangString << 4);
	final int Char2Short = T_char + (T_short << 4);
	final int Char2Char = T_char + (T_char << 4);
	final int Char2Int = T_char + (T_int << 4);
	final int Char2Long = T_char + (T_long << 4);
	final int Char2Float = T_char + (T_float << 4);
	final int Char2Double = T_char + (T_double << 4);
	final int Char2String = T_char + (T_JavaLangString << 4);
	final int Int2Short = T_int + (T_short << 4);
	final int Int2Char = T_int + (T_char << 4);
	final int Int2Int = T_int + (T_int << 4);
	final int Int2Long = T_int + (T_long << 4);
	final int Int2Float = T_int + (T_float << 4);
	final int Int2Double = T_int + (T_double << 4);
	final int Int2String = T_int + (T_JavaLangString << 4);
	final int Long2Short = T_long + (T_short << 4);
	final int Long2Char = T_long + (T_char << 4);
	final int Long2Int = T_long + (T_int << 4);
	final int Long2Long = T_long + (T_long << 4);
	final int Long2Float = T_long + (T_float << 4);
	final int Long2Double = T_long + (T_double << 4);
	final int Long2String = T_long + (T_JavaLangString << 4);
	final int Float2Short = T_float + (T_short << 4);
	final int Float2Char = T_float + (T_char << 4);
	final int Float2Int = T_float + (T_int << 4);
	final int Float2Long = T_float + (T_long << 4);
	final int Float2Float = T_float + (T_float << 4);
	final int Float2Double = T_float + (T_double << 4);
	final int Float2String = T_float + (T_JavaLangString << 4);
	final int Double2Short = T_double + (T_short << 4);
	final int Double2Char = T_double + (T_char << 4);
	final int Double2Int = T_double + (T_int << 4);
	final int Double2Long = T_double + (T_long << 4);
	final int Double2Float = T_double + (T_float << 4);
	final int Double2Double = T_double + (T_double << 4);
	final int Double2String = T_double + (T_JavaLangString << 4);
	final int String2String = T_JavaLangString + (T_JavaLangString << 4);
	final int Object2String = T_JavaLangObject + (T_JavaLangString << 4);
	final int Null2String = T_null + (T_JavaLangString << 4);
	final int Object2Object = T_JavaLangObject + (T_JavaLangObject << 4);
	final int BOXING = 0x200;
	final int UNBOXING = 0x400;
}
