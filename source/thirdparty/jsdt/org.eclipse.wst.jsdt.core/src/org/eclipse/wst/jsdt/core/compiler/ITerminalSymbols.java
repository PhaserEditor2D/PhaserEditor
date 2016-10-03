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

package org.eclipse.wst.jsdt.core.compiler;

/**
 * Maps each terminal symbol in the javaScript-grammar into a unique integer.
 * This integer is used to represent the terminal when computing a parsing action.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see IScanner
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface ITerminalSymbols {

	int TokenNameWHITESPACE = 1000;
	int TokenNameCOMMENT_LINE = 1001;
	int TokenNameCOMMENT_BLOCK = 1002;
	int TokenNameCOMMENT_JAVADOC = 1003;
	int TokenNameSHEBANG_LINE = 1004;

	int TokenNameIdentifier = 5;
	int TokenNameabstract = 98;

	int TokenNameassert = 118;
	int TokenNameboolean = 18;
	int TokenNamebreak = 119;
	int TokenNamebyte = 19;
	int TokenNamecase = 211;
	int TokenNamecatch = 225;
	int TokenNamechar = 20;
	int TokenNameclass = 165;
	int TokenNamecontinue = 120;
	int TokenNamedefault = 212;
	int TokenNamedo = 121;
	int TokenNamedouble = 21;
	int TokenNameelse = 213;
	int TokenNameextends = 243;
	int TokenNamefalse = 37;
	int TokenNamefinal = 99;
	int TokenNamefinally = 226;
	int TokenNamefloat = 22;
	int TokenNamefor = 122;
	int TokenNameif = 123;
	int TokenNameimplements = 268;
	int TokenNameimport = 191;
	int TokenNameinstanceof = 65;
	int TokenNameint = 23;
	int TokenNameinterface = 180;
	int TokenNamelong = 24;
	int TokenNamenative = 100;
	int TokenNamenew = 32;
	int TokenNamenull = 38;
	int TokenNamepackage = 214;
	int TokenNameprivate = 101;
	int TokenNameprotected = 102;
	int TokenNamepublic = 103;
	int TokenNamereturn = 124;
	int TokenNameshort = 25;
	int TokenNamestatic = 94;
	int TokenNamestrictfp = 104;
	int TokenNamesuper = 33;
	int TokenNameswitch = 125;
	int TokenNamesynchronized = 85;
	int TokenNamethis = 34;
	int TokenNamethrow = 126;
	int TokenNamethrows = 227;
	int TokenNametransient = 105;
	int TokenNametrue = 39;
	int TokenNametry = 127;
	int TokenNamevoid = 26;
	int TokenNamevolatile = 106;
	int TokenNamewhile = 117;
	int TokenNameIntegerLiteral = 40;
	int TokenNameLongLiteral = 41;
	int TokenNameFloatingPointLiteral = 42;
	int TokenNameDoubleLiteral = 43;
	int TokenNameCharacterLiteral = 44;
	int TokenNameStringLiteral = 45;
	int TokenNameRegExLiteral = 46;
	int TokenNamePLUS_PLUS = 1;
	int TokenNameMINUS_MINUS = 2;
	int TokenNameEQUAL_EQUAL = 35;
	int TokenNameLESS_EQUAL = 66;
	int TokenNameGREATER_EQUAL = 67;
	int TokenNameNOT_EQUAL = 36;
	int TokenNameLEFT_SHIFT = 14;
	int TokenNameRIGHT_SHIFT = 11;
	int TokenNameUNSIGNED_RIGHT_SHIFT = 12;
	int TokenNamePLUS_EQUAL = 168;
	int TokenNameMINUS_EQUAL = 169;
	int TokenNameMULTIPLY_EQUAL = 170;
	int TokenNameDIVIDE_EQUAL = 171;
	int TokenNameAND_EQUAL = 172;
	int TokenNameOR_EQUAL = 173;
	int TokenNameXOR_EQUAL = 174;
	int TokenNameREMAINDER_EQUAL = 175;
	int TokenNameLEFT_SHIFT_EQUAL = 176;
	int TokenNameRIGHT_SHIFT_EQUAL = 177;
	int TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL = 178;
	int TokenNameOR_OR = 80;
	int TokenNameAND_AND = 79;
	int TokenNamePLUS = 3;
	int TokenNameMINUS = 4;
	int TokenNameNOT = 71;
	int TokenNameREMAINDER = 9;
	int TokenNameXOR = 63;
	int TokenNameAND = 62;
	int TokenNameMULTIPLY = 8;
	int TokenNameOR = 70;
	int TokenNameTWIDDLE = 72;
	int TokenNameDIVIDE = 10;
	int TokenNameGREATER = 68;
	int TokenNameLESS = 69;
	int TokenNameLPAREN = 7;
	int TokenNameRPAREN = 86;
	int TokenNameLBRACE = 110;
	int TokenNameRBRACE = 95;
	int TokenNameLBRACKET = 15;
	int TokenNameRBRACKET = 166;
	int TokenNameSEMICOLON = 64;
	int TokenNameQUESTION = 81;
	int TokenNameCOLON = 154;
	int TokenNameCOMMA = 90;
	int TokenNameDOT = 6;
	int TokenNameEQUAL = 167;
	int TokenNameEOF = 158;
	int TokenNameERROR = 309;

	int TokenNameenum = 400;

	int TokenNameAT = 401;

	int TokenNameELLIPSIS = 402;

	int TokenNameconst = 403;

	int TokenNamegoto = 404;

	int TokenNameNOT_EQUAL_EQUAL=450;
	int TokenNameEQUAL_EQUAL_EQUAL=451;
	int TokenNamedelete=452;
	int TokenNamedebugger=453;
	int TokenNameexport=454;
	int TokenNamefunction=455;
	int TokenNamein=456;
	int TokenNameinfinity=457;
	int TokenNametypeof=458;
	int TokenNameundefined=459;
	int TokenNamevar=460;
	int TokenNamewith=461;

}
