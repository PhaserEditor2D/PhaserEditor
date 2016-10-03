/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.classfmt;

import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;

public interface ClassFileConstants {

	int AccDefault = 0;
	/*
	 * Modifiers
	 */
	int AccPublic       = 0x0001;
	int AccPrivate      = 0x0002;
	int AccProtected    = 0x0004;
	int AccStatic       = 0x0008;
	int AccFinal        = 0x0010;
	int AccBridge       = 0x0040;
	int AccVarargs      = 0x0080;
	int AccNative       = 0x0100;
	int AccAbstract     = 0x0400;
	int AccStrictfp     = 0x0800;

	/**
	 * Other VM flags.
	 */
	int AccSuper = 0x0020;

	/**
	 * Extra flags for types and members attributes.
	 */
	int AccAnnotationDefault = ASTNode.Bit18; // indicate presence of an attribute  "DefaultValue" (annotation method)
	int AccDeprecated = ASTNode.Bit21; // indicate presence of an attribute "Deprecated"

	int Utf8Tag = 1;
	int IntegerTag = 3;
	int FloatTag = 4;
	int LongTag = 5;
	int DoubleTag = 6;
	int ClassTag = 7;
	int StringTag = 8;
	int FieldRefTag = 9;
	int MethodRefTag = 10;
	int InterfaceMethodRefTag = 11;
	int NameAndTypeTag = 12;

	int ConstantMethodRefFixedSize = 5;
	int ConstantClassFixedSize = 3;
	int ConstantDoubleFixedSize = 9;
	int ConstantFieldRefFixedSize = 5;
	int ConstantFloatFixedSize = 5;
	int ConstantIntegerFixedSize = 5;
	int ConstantInterfaceMethodRefFixedSize = 5;
	int ConstantLongFixedSize = 9;
	int ConstantStringFixedSize = 3;
	int ConstantUtf8FixedSize = 3;
	int ConstantNameAndTypeFixedSize = 5;

	int MAJOR_VERSION_0_0 = 00;
	int MAJOR_VERSION_1_1 = 45;
	int MAJOR_VERSION_1_2 = 46;
	int MAJOR_VERSION_1_3 = 47;
	int MAJOR_VERSION_1_4 = 48;
	int MAJOR_VERSION_1_5 = 49;
	int MAJOR_VERSION_1_6 = 50;
	int MAJOR_VERSION_1_7 = 51;

	int MINOR_VERSION_0 = 0;
	int MINOR_VERSION_1 = 1;
	int MINOR_VERSION_2 = 2;
	int MINOR_VERSION_3 = 3;

	
	// A special version number that is to be used to skip any parsing/type inference
	long JDK0_0 = ((long)ClassFileConstants.MAJOR_VERSION_0_0 << 16) + ClassFileConstants.MINOR_VERSION_0; // 1.1. is 45.3
	// JDK 1.1 -> 1.7, comparable value allowing to check both major/minor version at once 1.4.1 > 1.4.0
	// 16 unsigned bits for major, then 16 bits for minor
	long JDK1_1 = ((long)ClassFileConstants.MAJOR_VERSION_1_1 << 16) + ClassFileConstants.MINOR_VERSION_3; // 1.1. is 45.3
	long JDK1_2 =  ((long)ClassFileConstants.MAJOR_VERSION_1_2 << 16) + ClassFileConstants.MINOR_VERSION_0;
	long JDK1_3 =  ((long)ClassFileConstants.MAJOR_VERSION_1_3 << 16) + ClassFileConstants.MINOR_VERSION_0;
	long JDK1_4 = ((long)ClassFileConstants.MAJOR_VERSION_1_4 << 16) + ClassFileConstants.MINOR_VERSION_0;
	long JDK1_5 = ((long)ClassFileConstants.MAJOR_VERSION_1_5 << 16) + ClassFileConstants.MINOR_VERSION_0;
	long JDK1_6 = ((long)ClassFileConstants.MAJOR_VERSION_1_6 << 16) + ClassFileConstants.MINOR_VERSION_0;
	long JDK1_7 = ((long)ClassFileConstants.MAJOR_VERSION_1_7 << 16) + ClassFileConstants.MINOR_VERSION_0;

	// jdk level used to denote future releases: optional behavior is not enabled for now, but may become so. In order to enable these,
	// search for references to this constant, and change it to one of the official JDT constants above.
	long JDK_DEFERRED = Long.MAX_VALUE;

	int INT_ARRAY = 10;
	int BYTE_ARRAY = 8;
	int BOOLEAN_ARRAY = 4;
	int SHORT_ARRAY = 9;
	int CHAR_ARRAY = 5;
	int LONG_ARRAY = 11;
	int FLOAT_ARRAY = 6;
	int DOUBLE_ARRAY = 7;

	// Debug attributes
	int ATTR_SOURCE = 1; // SourceFileAttribute
	int ATTR_LINES = 2; // LineNumberAttribute
	int ATTR_VARS = 4; // LocalVariableTableAttribute
	int ATTR_STACK_MAP = 8; // Stack map table attribute
}
