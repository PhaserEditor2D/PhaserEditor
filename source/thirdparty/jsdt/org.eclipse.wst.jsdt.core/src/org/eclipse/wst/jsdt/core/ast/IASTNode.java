/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.ast;

/**
 *  Abstract base class for AST nodes.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */

public interface IASTNode {
	
	
	//public static final int AST_FUNCTION = 1;
	//public static final int AST_ABSTRACT_METHOD_DECLARATION = 0;
	public static final int AST_NODE=0;
	public static final int ABSTRACT_FUNCTION_DECLARATION=1;
	public static final int ABSTRACT_VARIABLE_DECLARATION=2;
	public static final int ALLOCATION_EXPRESSION=3;
	public static final int AND_AND_EXPRESSION=4;
	public static final int ARGUMENT=7;
	public static final int ARRAY_ALLOCATION_EXPRESSION=8;
	public static final int ARRAY_INITIALIZER=9;
	public static final int ARRAY_QUALIFIED_TYPE_REFERENCE=10;
	public static final int ARRAY_REFERENCE=11;
	public static final int ARRAY_TYPE_REFERENCE=12;
	public static final int ASSIGNMENT=14;
	public static final int BINARY_EXPRESSION=15;
	public static final int BLOCK=16;
	public static final int BRANCH_STATEMENT=17;
	public static final int BREAK_STATEMENT=18;
	public static final int CASE_STATEMENT=19;
	public static final int CHAR_LITERAL=21;
	public static final int COMBINED_BINARY_EXPRESSION=22;
	public static final int COMPOUND_ASSIGNMENT=23;
	public static final int CONDITIONAL_EXPRESSION=24;
	public static final int CONSTRUCTOR_DECLARATION=25;
	public static final int CONTINUE_STATEMENT=26;
	public static final int DO_STATEMENT=27;
	public static final int DOUBLE_LITERAL=28;
	public static final int EMPTY_EXPRESSION=29;
	public static final int EMPTY_STATEMENT=30;
	public static final int EQUAL_EXPRESSION=31;
	public static final int EXPLICIT_CONSTRUCTOR_CALL=32;
	public static final int EXPRESSION=33;
	public static final int EXTENDED_STRING_LITERAL=34;
	public static final int FALSE_LITERAL=35;
	public static final int FIELD_DECLARATION=36;
	public static final int FIELD_REFERENCE=37;
	public static final int FLOAT_LITERAL=38;
	public static final int FOR_EACH_STATEMENT=39;
	public static final int FOR_IN_STATEMENT=40;
	public static final int FOR_STATEMENT=41;
	public static final int FUNCTION_CALL=42;
	public static final int FUNCTION_DECLARATION=43;
	public static final int FUNCTION_EXPRESSION=44;
	public static final int IF_STATEMENT=45;
	public static final int IMPORT_REFERENCE=46;
	public static final int INITIALIZER=47;
	public static final int INSTANCEOF_EXPRESSION=48;
	public static final int INT_LITERAL=49;
	public static final int INT_LITERAL_MIN_VALUE=50;
	public static final int JSDOC=51;
	public static final int JSDOC_ALLOCATION_EXPRESSION=52;
	public static final int JSDOC_ARGUMENTEXPRESSION=53;
	public static final int JSDOC_ARRAY_QUALIFIED_TYPE_REFERENCE=54;
	public static final int JSDOC_ARRAY_SINGLE_TYPE_REFERENCE=55;
	public static final int JSDOC_FIELD_REFERENCE=56;
	public static final int JSDOC_IMPLICIT_TYPE_REFERENCE=57;
	public static final int JSDOC_MESSAGE_SEND=58;
	public static final int JSDOC_QUALIFIED_TYPE_REFERENCE=59;
	public static final int JSDOC_RETURN_STATEMENT=60;
	public static final int JSDOC_SINGLE_NAME_REFERENCE=61;
	public static final int JSDOC_SINGLE_TYPE_REFERENCE=62;
	public static final int LABELED_STATEMENT=63;
	public static final int LIST_EXPRESSION=64;
	public static final int LITERAL=65;
	public static final int LOCAL_DECLARATION=66;
	public static final int LONG_LITERAL=67;
	public static final int LONG_LITERAL_MIN_VALUE=68;
	public static final int MAGIC_LITERAL=69;
	public static final int NAME_REFERENCE=72;
	public static final int NULL_LITERAL=74;
	public static final int NUMBER_LITERAL=75;
	public static final int OBJECT_LITERAL=76;
	public static final int OBJECT_LITERAL_FIELD=77;
	public static final int OPERATOR_EXPRESSION=78;
	public static final int OR_OR_EXPRESSION=79;
	public static final int PARAMETERIZED_QUALIFIED_TYPE_REFERENCE=80;
	public static final int PARAMETERIZED_SINGLE_TYPE_REFERENCE=81;
	public static final int POSTFIX_EXPRESSION=82;
	public static final int PREFIX_EXPRESSION=83;
	public static final int PROGRAM_ELEMENT=84;
	public static final int QUALIFIED_ALLOCATION_EXPRESSION=85;
	public static final int QUALIFIED_NAME_REFERENCE=86;
	public static final int QUALIFIED_SUPER_REFERENCE=87;
	public static final int QUALIFIED_THIS_REFERENCE=88;
	public static final int QUALIFIED_TYPE_REFERENCE=89;
	public static final int REFERENCE=90;
	public static final int REG_EX_LITERAL=91;
	public static final int RETURN_STATEMENT=92;
	public static final int SCRIPT_FILE_DECLARATION=93;
	public static final int SINGLE_NAME_REFERENCE=95;
	public static final int SINGLE_TYPE_REFERENCE=96;
	public static final int STATEMENT=97;
	public static final int STRING_LITERAL=98;
	public static final int STRING_LITERAL_CONCATENATION=99;
	public static final int SUB_ROUTINE_STATEMENT=100;
	public static final int SUPER_REFERENCE=101;
	public static final int SWITCH_STATEMENT=102;
	public static final int THIS_REFERENCE=103;
	public static final int THROW_STATEMENT=104;
	public static final int TRUE_LITERAL=105;
	public static final int TRY_STATEMENT=106;
	public static final int TYPE_DECLARATION=107;
	public static final int TYPE_PARAMETER=108;
	public static final int TYPE_REFERENCE=109;
	public static final int UNARY_EXPRESSION=110;
	public static final int UNDEFINED_LITERAL=111;
	public static final int WHILE_STATEMENT=112;
	public static final int WITH_STATEMENT=114;
	public static final int CLASS_LITERAL_ACCESS=115;
	public static final int CL_INIT=116;
	public static final int OBJECT_GETTER_SETTER_FIELD=117;
	public int sourceStart() ;
	public int sourceEnd();
	public int getASTType();
	
	public void traverse(ASTVisitor visitor);

}