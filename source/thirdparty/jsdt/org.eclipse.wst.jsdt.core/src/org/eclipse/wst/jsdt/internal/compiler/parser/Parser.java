/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *       	 Etienne Pfister <epfister@hsr.ch> bug 224333
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.parser;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ast.IDoStatement;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.infer.IInferEngineExtension;
import org.eclipse.wst.jsdt.core.infer.InferOptions;
import org.eclipse.wst.jsdt.core.infer.InferrenceManager;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DebuggerStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.EmptyExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForInStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteralMinValue;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectGetterSetterField;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.WithStatement;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.parser.diagnose.DiagnoseParser;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.wst.jsdt.internal.compiler.util.Messages;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class Parser implements  ParserBasicInformation, TerminalTokens, OperatorIds, TypeIds {

	private static final String PERFORMANCE__INFER_TYPES = "org.eclipse.wst.jsdt.core/perf/Parser/inferTypes"; //$NON-NLS-1$
	private static final boolean REPORT_PERFORMANCE__INFER_TYPES = Boolean.valueOf(Platform.getDebugOption("org.eclipse.wst.jsdt.core/perfReport/Parser/inferTypes")).booleanValue(); //$NON-NLS-1$

	public static final boolean DO_DIET_PARSE=false;

	protected static final int THIS_CALL = ExplicitConstructorCall.This;

	public static char asb[] = null;
	public static char asr[] = null;
	//ast stack
	protected final static int AstStackIncrement = 100;
	public static char base_action[] = null;
	public static final int BracketKinds = 3;

	public static short check_table[] = null;
	public static final int CurlyBracket = 2;
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_AUTOMATON = false;
	private static final String EOF_TOKEN = "$eof" ; //$NON-NLS-1$
	private static final String ERROR_TOKEN = "$error" ; //$NON-NLS-1$
	//expression stack
	protected final static int ExpressionStackIncrement = 100;

	protected final static int GenericsStackIncrement = 10;

	private final static String FILEPREFIX = "parser"; //$NON-NLS-1$
    public static char in_symb[] = null;
	private static final String INVALID_CHARACTER = "Invalid Character" ; //$NON-NLS-1$
	public static char lhs[] =  null;

	public static String name[] = null;
	public static char nasb[] = null;
	public static char nasr[] = null;
	public static char non_terminal_index[] = null;
	private final static String READABLE_NAMES_FILE = "readableNames"; //$NON-NLS-1$
	private final static String READABLE_NAMES_FILE_NAME =
		"org.eclipse.wst.jsdt.internal.compiler.parser." + READABLE_NAMES_FILE; //$NON-NLS-1$
	public static String readableName[] = null;

	public static byte rhs[] = null;

	public static int[] reverse_index = null;
	public static char[] recovery_templates_index = null;
	public static char[] recovery_templates = null;
	public static char[] statements_recovery_filter = null;
	public static byte[] state_flags = null;

	public static long rules_compliance[] =  null;

	public static final int RoundBracket = 0;

    public static byte scope_la[] = null;
    public static char scope_lhs[] = null;

	public static char scope_prefix[] = null;
    public static char scope_rhs[] = null;
    public static char scope_state[] = null;

    public static char scope_state_set[] = null;
    public static char scope_suffix[] = null;
	public static final int SquareBracket = 1;

	//internal data for the automat
	protected final static int StackIncrement = 255;

	public static char term_action[] = null;
	public static byte term_check[] = null;

	public static char terminal_index[] = null;

	private static final String UNEXPECTED_EOF = "Unexpected End Of File" ; //$NON-NLS-1$
	public static boolean VERBOSE_RECOVERY = false;


	static boolean optionalSemicolonState[] =null;


	protected int astLengthPtr;
	protected int[] astLengthStack;
	protected int astPtr;
	protected ASTNode[] astStack = new ASTNode[AstStackIncrement];
	public CompilationUnitDeclaration compilationUnit; /*the result from parse()*/
	protected RecoveredElement currentElement;
	public int currentToken;
	protected boolean diet = false; //tells the scanner to jump over some parts of the code/expressions like method bodies
	protected int dietInt = 0; // if > 0 force the none-diet-parsing mode (even if diet if requested) [field parsing with anonymous inner classes...]
	protected int endPosition; //accurate only when used ! (the start position is pushed into intStack while the end the current one)
	protected int endStatementPosition;
	protected int expressionLengthPtr;
	protected int[] expressionLengthStack;
	protected int expressionPtr;
	protected Expression[] expressionStack = new Expression[ExpressionStackIncrement];
	public int firstToken ; // handle for multiple parsing goals

	// generics management
	protected int genericsIdentifiersLengthPtr;
	protected int[] genericsIdentifiersLengthStack = new int[GenericsStackIncrement];
	protected int genericsLengthPtr;
	protected int[] genericsLengthStack = new int[GenericsStackIncrement];
	protected int genericsPtr;
	protected ASTNode[] genericsStack = new ASTNode[GenericsStackIncrement];

	protected boolean hasError;
	protected boolean hasReportedError;

	//identifiers stacks
	protected int identifierLengthPtr;
	protected int[] identifierLengthStack;
	protected long[] identifierPositionStack;
	protected int identifierPtr;
	protected char[][] identifierStack;

	protected boolean ignoreNextOpeningBrace;
	//positions , dimensions , .... (int stacks)
	protected int intPtr;
	protected int[] intStack;
	public int lastAct ; //handle for multiple parsing goals

	//error recovery management
	protected int lastCheckPoint;
	protected int lastErrorEndPosition;
	protected int lastErrorEndPositionBeforeRecovery = -1;
	protected int lastIgnoredToken, nextIgnoredToken;
	protected int listLength; // for recovering some incomplete list (interfaces, throws or parameters)
	protected int listTypeParameterLength; // for recovering some incomplete list (type parameters)
	protected int lParenPos,rParenPos; //accurate only when used !
	protected int modifiers;
	protected int modifiersSourceStart;
	protected int[] nestedMethod; //the ptr is nestedType
	protected int nestedType, dimensions;
	ASTNode [] noAstNodes = new ASTNode[AstStackIncrement];
	Expression [] noExpressions = new Expression[ExpressionStackIncrement];
	//modifiers dimensions nestedType etc.......
	protected boolean optimizeStringLiterals =true;
	protected CompilerOptions options;
	protected ProblemReporter problemReporter;
	protected int rBraceStart, rBraceEnd, rBraceSuccessorStart; //accurate only when used !
	protected int realBlockPtr;
	protected int[] realBlockStack;
	protected int recoveredStaticInitializerStart;
	public ReferenceContext referenceContext;
	public boolean reportOnlyOneSyntaxError = false;
	public boolean reportSyntaxErrorIsRequired = true;
	protected boolean restartRecovery;

	// statement recovery
//	public boolean statementRecoveryEnabled = true;
	public boolean methodRecoveryActivated = false;
	protected boolean statementRecoveryActivated = false;
	protected TypeDeclaration[] recoveredTypes;
	protected int recoveredTypePtr;
	protected int nextTypeStart;
	protected TypeDeclaration pendingRecoveredType;

	public RecoveryScanner recoveryScanner;

	//scanner token
	public Scanner scanner;
	protected int[] stack = new int[StackIncrement];
	protected int stateStackTop;
//	protected int synchronizedBlockSourceStart;
	protected int[] variablesCounter;

	protected boolean checkExternalizeStrings;
	protected boolean recordStringLiterals;

	// javadoc
	public Javadoc javadoc;
	public JavadocParser javadocParser;
	// used for recovery
	protected int lastJavadocEnd;

	private boolean enteredRecoverStatements;

	private int insertedSemicolonPosition=-1;

	private Set errorAction = new HashSet();
	
	private static final int  UNCONSUMED_LIT_ELEMENT=0x4;
	private static final int  UNCONSUMED_ELISION=0x2;
	private static final int WAS_ARRAY_LIT_ELEMENT=0x1;

	public static final byte FLAG_EMPTY_STATEMENT = 1;

	public IInferEngine[] inferenceEngines;
	
	static {
		try{
			initTables();
		} catch(java.io.IOException ex){
			throw new ExceptionInInitializerError(ex.getMessage());
		}
	}
public static int asi(int state) {

	return asb[original_state(state)];
}
public final static short base_check(int i) {
	return check_table[i - (NUM_RULES + 1)];
}
private final static void buildFile(String filename, List listToDump) {
	BufferedWriter writer = null;
	try {
		writer = new BufferedWriter(new FileWriter(filename));
    	for (Iterator iterator = listToDump.iterator(); iterator.hasNext(); ) {
    		writer.write(String.valueOf(iterator.next()));
    	}
    	writer.flush();
	} catch(IOException e) {
		// ignore
	} finally {
		if (writer != null) {
        	try {
				writer.close();
			} catch (IOException e1) {
				// ignore
			}
		}
	}
	System.out.println(filename + " creation complete"); //$NON-NLS-1$
}
private final static String[] buildFileForName(String filename, String contents) {
	String[] result = new String[contents.length()];
	result[0] = null;
	int resultCount = 1;

	StringBuffer buffer = new StringBuffer();

	int start = contents.indexOf("name[]"); //$NON-NLS-1$
	start = contents.indexOf('\"', start);
	int end = contents.indexOf("};", start); //$NON-NLS-1$

	contents = contents.substring(start, end);

	boolean addLineSeparator = false;
	int tokenStart = -1;
	StringBuffer currentToken = new StringBuffer();
	for (int i = 0; i < contents.length(); i++) {
		char c = contents.charAt(i);
		if(c == '\"') {
			if(tokenStart == -1) {
				tokenStart = i + 1;
			} else {
				if(addLineSeparator) {
					buffer.append('\n');
					result[resultCount++] = currentToken.toString();
					currentToken = new StringBuffer();
				}
				String token = contents.substring(tokenStart, i);
				if(token.equals(ERROR_TOKEN)){
					token = INVALID_CHARACTER;
				} else if(token.equals(EOF_TOKEN)) {
					token = UNEXPECTED_EOF;
				}
				buffer.append(token);
				currentToken.append(token);
				addLineSeparator = true;
				tokenStart = -1;
			}
		}
		if(tokenStart == -1 && c == '+'){
			addLineSeparator = false;
		}
	}
	if(currentToken.length() > 0) {
		result[resultCount++] = currentToken.toString();
	}

	buildFileForTable(filename, buffer.toString().toCharArray());

	System.arraycopy(result, 0, result = new String[resultCount], 0, resultCount);
	return result;
}
private static void buildFileForReadableName(
	String file,
	char[] newLhs,
	char[] newNonTerminalIndex,
	String[] newName,
	String[] tokens) {

	ArrayList entries = new ArrayList();

	boolean[] alreadyAdded = new boolean[newName.length];

	for (int i = 0; i < tokens.length; i = i + 3) {
		if("1".equals(tokens[i])) { //$NON-NLS-1$
			int index = newNonTerminalIndex[newLhs[Integer.parseInt(tokens[i + 1])]];
			StringBuffer buffer = new StringBuffer();
			if(!alreadyAdded[index]) {
				alreadyAdded[index] = true;
				buffer.append(newName[index]);
				buffer.append('=');
				buffer.append(tokens[i+2].trim());
				buffer.append('\n');
				entries.add(String.valueOf(buffer));
			}
		}
	}
	int i = 1;
	while(!INVALID_CHARACTER.equals(newName[i])) i++;
	i++;
	for (; i < alreadyAdded.length; i++) {
		if(!alreadyAdded[i]) {
			System.out.println(newName[i] + " has no readable name"); //$NON-NLS-1$
		}
	}
	Collections.sort(entries);
	buildFile(file, entries);
}
private static void buildFilesForRecoveryTemplates(
	String indexFilename,
	String templatesFilename,
	char[] newTerminalIndex,
	char[] newNonTerminalIndex,
	String[] newName,
	char[] newLhs,
	String[] tokens) {

	int[] newReverse = computeReverseTable(newTerminalIndex, newNonTerminalIndex, newName);

	char[] newRecoveyTemplatesIndex = new char[newNonTerminalIndex.length];
	char[] newRecoveyTemplates = new char[newNonTerminalIndex.length];
	int newRecoveyTemplatesPtr = 0;

	for (int i = 0; i < tokens.length; i = i + 3) {
		if("3".equals(tokens[i])) { //$NON-NLS-1$
			int length = newRecoveyTemplates.length;
			if(length == newRecoveyTemplatesPtr + 1) {
				System.arraycopy(newRecoveyTemplates, 0, newRecoveyTemplates = new char[length * 2], 0, length);
			}
			newRecoveyTemplates[newRecoveyTemplatesPtr++] = 0;

			int index = newLhs[Integer.parseInt(tokens[i + 1])];

			newRecoveyTemplatesIndex[index] = (char)newRecoveyTemplatesPtr;

			String token = tokens[i + 2].trim();
			java.util.StringTokenizer st = new java.util.StringTokenizer(new String(token), " ");  //$NON-NLS-1$
			String[] terminalNames = new String[st.countTokens()];
			int t = 0;
			while (st.hasMoreTokens()) {
				terminalNames[t++] = st.nextToken();
			}

			for (int j = 0; j < terminalNames.length; j++) {
				int symbol = getSymbol(terminalNames[j], newName, newReverse);
				if(symbol > -1) {
					length = newRecoveyTemplates.length;
					if(length == newRecoveyTemplatesPtr + 1) {
						System.arraycopy(newRecoveyTemplates, 0, newRecoveyTemplates = new char[length * 2], 0, length);
					}
					newRecoveyTemplates[newRecoveyTemplatesPtr++] = (char)symbol;
				}
			}
		}
	}
	newRecoveyTemplates[newRecoveyTemplatesPtr++] = 0;
	System.arraycopy(newRecoveyTemplates, 0, newRecoveyTemplates = new char[newRecoveyTemplatesPtr], 0, newRecoveyTemplatesPtr);

	buildFileForTable(indexFilename, newRecoveyTemplatesIndex);
	buildFileForTable(templatesFilename, newRecoveyTemplates);
}
private static void buildFilesForStatementsRecoveryFilter(
		String filename,
		char[] newNonTerminalIndex,
		char[] newLhs,
		String[] tokens) {

		char[] newStatementsRecoveryFilter = new char[newNonTerminalIndex.length];

		for (int i = 0; i < tokens.length; i = i + 3) {
			if("4".equals(tokens[i])) { //$NON-NLS-1$
				int index = newLhs[Integer.parseInt(tokens[i + 1])];

				newStatementsRecoveryFilter[index] = 1;
			}
		}
		buildFileForTable(filename, newStatementsRecoveryFilter);
	}

private static void buildFilesForFlags(
		String filename,
		int size,
		String[] tokens) {

		byte[] flags = new byte[size];

		for (int i = 0; i < tokens.length; i = i + 3) {
			if("5".equals(tokens[i])) { //$NON-NLS-1$
				int index = Integer.parseInt(tokens[i + 1]);
				byte value =(byte) Integer.parseInt(tokens[i + 2].trim());
				flags[index]=value;
			}
		}
		buildFileForTable(filename, flags);
	}

private static void buildFileForCompliance(
		String file,
		int length,
		String[] tokens) {

		byte[] result = new byte[length * 8];

		for (int i = 0; i < tokens.length; i = i + 3) {
			if("2".equals(tokens[i])) { //$NON-NLS-1$
				int index = Integer.parseInt(tokens[i + 1]);
				String token = tokens[i + 2].trim();
				long compliance = 0;
				if("1.4".equals(token)) { //$NON-NLS-1$
					compliance = ClassFileConstants.JDK1_4;
				} else if("1.5".equals(token)) { //$NON-NLS-1$
					compliance = ClassFileConstants.JDK1_5;
				} else if("recovery".equals(token)) { //$NON-NLS-1$
					compliance = ClassFileConstants.JDK_DEFERRED;
				}

				int j = index * 8;
				result[j] = 	(byte)(compliance >>> 56);
				result[j + 1] = (byte)(compliance >>> 48);
				result[j + 2] = (byte)(compliance >>> 40);
				result[j + 3] = (byte)(compliance >>> 32);
				result[j + 4] = (byte)(compliance >>> 24);
				result[j + 5] = (byte)(compliance >>> 16);
				result[j + 6] = (byte)(compliance >>> 8);
				result[j + 7] = (byte)(compliance);
			}
		}

		buildFileForTable(file, result);
	}
private final static void buildFileForTable(String filename, byte[] bytes) {
	 java.io.FileOutputStream stream = null;
		try {
			stream = new java.io.FileOutputStream(filename);
			stream.write(bytes);
		} catch(IOException e) {
		// ignore
	} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e1) {
					// ignore
				}
			}
		}
		System.out.println(filename + " creation complete"); //$NON-NLS-1$
	}
	private final static void buildFileForTable(String filename, char[] chars) {
		byte[] bytes = new byte[chars.length * 2];
		for (int i = 0; i < chars.length; i++) {
			bytes[2 * i] = (byte) (chars[i] >>> 8);
			bytes[2 * i + 1] = (byte) (chars[i] & 0xFF);
		}

	 java.io.FileOutputStream stream = null;
		try {
			stream = new java.io.FileOutputStream(filename);
			stream.write(bytes);
		} catch(IOException e) {
		// ignore
	} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e1) {
					// ignore
				}
			}
		}
		System.out.println(filename + " creation complete"); //$NON-NLS-1$
	}
	private final static byte[] buildFileOfByteFor(String filename, String tag, String[] tokens) {

		//transform the String tokens into chars before dumping then into file

		int i = 0;
		//read upto the tag
		while (!tokens[i++].equals(tag)){/*empty*/}
		//read upto the }

		byte[] bytes = new byte[tokens.length]; //can't be bigger
		int ic = 0;
		String token;
		while (!(token = tokens[i++]).equals("}")) { //$NON-NLS-1$
			int c = Integer.parseInt(token);
			bytes[ic++] = (byte) c;
		}

		//resize
		System.arraycopy(bytes, 0, bytes = new byte[ic], 0, ic);

		buildFileForTable(filename, bytes);
		return bytes;
	}
	private final static char[] buildFileOfIntFor(String filename, String tag, String[] tokens) {

		//transform the String tokens into chars before dumping then into file

		int i = 0;
		//read upto the tag
		while (!tokens[i++].equals(tag)){/*empty*/}
		//read upto the }

		char[] chars = new char[tokens.length]; //can't be bigger
		int ic = 0;
		String token;
		while (!(token = tokens[i++]).equals("}")) { //$NON-NLS-1$
			int c = Integer.parseInt(token);
			chars[ic++] = (char) c;
		}

		//resize
		System.arraycopy(chars, 0, chars = new char[ic], 0, ic);

		buildFileForTable(filename, chars);
		return chars;
	}
	private final static void buildFileOfShortFor(String filename, String tag, String[] tokens) {

		//transform the String tokens into chars before dumping then into file

		int i = 0;
		//read upto the tag
		while (!tokens[i++].equals(tag)){/*empty*/}
		//read upto the }

		char[] chars = new char[tokens.length]; //can't be bigger
		int ic = 0;
		String token;
		while (!(token = tokens[i++]).equals("}")) { //$NON-NLS-1$
			int c = Integer.parseInt(token);
			chars[ic++] = (char) (c + 32768);
		}

		//resize
		System.arraycopy(chars, 0, chars = new char[ic], 0, ic);

		buildFileForTable(filename, chars);
	}
public final static void buildFilesFromLPG(String dataFilename, String dataFilename2)	{

	//RUN THIS METHOD TO GENERATE PARSER*.RSC FILES

	//build from the lpg javadcl.java files that represents the parser tables
	//lhs check_table asb asr symbol_index

	//[org.eclipse.wst.jsdt.internal.compiler.parser.Parser.buildFilesFromLPG("d:/leapfrog/grammar/javadcl.java")]
	char[] contents = CharOperation.NO_CHAR;
	try {
		contents = Util.getFileCharContent(new File(dataFilename), null);
	} catch (IOException ex) {
		System.out.println(Messages.parser_incorrectPath);
		return;
	}
	java.util.StringTokenizer st =
		new java.util.StringTokenizer(new String(contents), " \t\n\r[]={,;");  //$NON-NLS-1$
	String[] tokens = new String[st.countTokens()];
	int j = 0;
	while (st.hasMoreTokens()) {
		tokens[j++] = st.nextToken();
	}
	final String prefix = FILEPREFIX;
	int i = 0;

	char[] newLhs = buildFileOfIntFor(prefix + (++i) + ".rsc", "lhs", tokens); //$NON-NLS-1$ //$NON-NLS-2$
	buildFileOfShortFor(prefix + (++i) + ".rsc", "check_table", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "asb", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "asr", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "nasb", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "nasr", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	char[] newTerminalIndex = buildFileOfIntFor(prefix + (++i) + ".rsc", "terminal_index", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	char[] newNonTerminalIndex = buildFileOfIntFor(prefix + (++i) + ".rsc", "non_terminal_index", tokens); //$NON-NLS-1$ //$NON-NLS-2$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "term_action", tokens); //$NON-NLS-2$ //$NON-NLS-1$

	buildFileOfIntFor(prefix + (++i) + ".rsc", "scope_prefix", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "scope_suffix", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "scope_lhs", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "scope_state_set", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "scope_rhs", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "scope_state", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfIntFor(prefix + (++i) + ".rsc", "in_symb", tokens); //$NON-NLS-2$ //$NON-NLS-1$

	byte[] newRhs = buildFileOfByteFor(prefix + (++i) + ".rsc", "rhs", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfByteFor(prefix + (++i) + ".rsc", "term_check", tokens); //$NON-NLS-2$ //$NON-NLS-1$
	buildFileOfByteFor(prefix + (++i) + ".rsc", "scope_la", tokens); //$NON-NLS-2$ //$NON-NLS-1$

	String[] newName = buildFileForName(prefix + (++i) + ".rsc", new String(contents)); //$NON-NLS-1$

	contents = CharOperation.NO_CHAR;
	try {
		contents = Util.getFileCharContent(new File(dataFilename2), null);
	} catch (IOException ex) {
		System.out.println(Messages.parser_incorrectPath);
		return;
	}
	st = new java.util.StringTokenizer(new String(contents), "\t\n\r#");  //$NON-NLS-1$
	tokens = new String[st.countTokens()];
	j = 0;
	while (st.hasMoreTokens()) {
		tokens[j++] = st.nextToken();
	}

	buildFileForCompliance(prefix + (++i) + ".rsc", newRhs.length, tokens);//$NON-NLS-1$
	buildFileForReadableName(READABLE_NAMES_FILE+".properties", newLhs, newNonTerminalIndex, newName, tokens);//$NON-NLS-1$

	buildFilesForRecoveryTemplates(
			prefix + (++i) + ".rsc", //$NON-NLS-1$
			prefix + (++i) + ".rsc", //$NON-NLS-1$
			newTerminalIndex,
			newNonTerminalIndex,
			newName,
			newLhs,
			tokens);

	buildFilesForStatementsRecoveryFilter(
			prefix + (++i) + ".rsc", //$NON-NLS-1$
			newNonTerminalIndex,
			newLhs,
			tokens);

	buildFilesForFlags(
			prefix + (++i) + ".rsc", //$NON-NLS-1$
			rhs.length,
			tokens);


	System.out.println(Messages.parser_moveFiles);
}
public static int in_symbol(int state) {
	return in_symb[original_state(state)];
}
public final static void initTables() throws java.io.IOException {

	final String prefix = FILEPREFIX;
	int i = 0;
	lhs = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	char[] chars = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	check_table = new short[chars.length];
	for (int c = chars.length; c-- > 0;) {
		check_table[c] = (short) (chars[c] - 32768);
	}
	asb = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	asr = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	nasb = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	nasr = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	terminal_index = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	non_terminal_index = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	term_action = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	scope_prefix = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	scope_suffix = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	scope_lhs = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	scope_state_set = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	scope_rhs = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	scope_state = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	in_symb = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	rhs = readByteTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	term_check = readByteTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	scope_la = readByteTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	name = readNameTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	rules_compliance = readLongTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	readableName = readReadableNameTable(READABLE_NAMES_FILE_NAME);

	reverse_index = computeReverseTable(terminal_index, non_terminal_index, name);

	recovery_templates_index = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	recovery_templates = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	statements_recovery_filter = readTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$
	state_flags = readByteTable(prefix + (++i) + ".rsc"); //$NON-NLS-1$

	base_action = lhs;

    optionalSemicolonState =new boolean[base_action.length];
	for ( i = 0; i < optionalSemicolonState.length; i++) {
		if (base_action[i]+TokenNameSEMICOLON<term_check.length
				&& (term_check[base_action[i]+TokenNameSEMICOLON] == TokenNameSEMICOLON))
		{
			int act=term_action[base_action[i]+TokenNameSEMICOLON];
			if (act>ERROR_ACTION)
				act-=ERROR_ACTION;
			boolean isEmptyStatementsState = false;
			if (act<NUM_RULES)
				isEmptyStatementsState = (state_flags[act]&FLAG_EMPTY_STATEMENT)>0;
			if (!isEmptyStatementsState)
				optionalSemicolonState[i]=true;
		}
	}

}
public static int nasi(int state) {
	return nasb[original_state(state)];
}
public static int ntAction(int state, int sym) {
	return base_action[state + sym];
}
protected static int original_state(int state) {
	return -base_check(state);
}
protected static int[] computeReverseTable(char[] newTerminalIndex, char[] newNonTerminalIndex, String[] newName) {
	int[] newReverseTable = new int[newName.length];
	for (int j = 0; j < newName.length; j++) {
		found : {
			for (int k = 0; k < newTerminalIndex.length; k++) {
				if(newTerminalIndex[k] == j) {
					newReverseTable[j] = k;
					break found;
				}
			}
			for (int k = 0; k < newNonTerminalIndex.length; k++) {
				if(newNonTerminalIndex[k] == j) {
					newReverseTable[j] = -k;
					break found;
				}
			}
		}
	}
	return newReverseTable;
}

private static int getSymbol(String terminalName, String[] newName, int[] newReverse) {
	for (int j = 0; j < newName.length; j++) {
		if(terminalName.equals(newName[j])) {
			return newReverse[j];
		}
	}
	return -1;
}

protected static byte[] readByteTable(String filename) throws java.io.IOException {

	//files are located at Parser.class directory

	InputStream stream = Parser.class.getResourceAsStream(filename);
	if (stream == null) {
		throw new java.io.IOException(Messages.bind(Messages.parser_missingFile, filename));
	}
	byte[] bytes = null;
	try {
		stream = new BufferedInputStream(stream);
		bytes = Util.getInputStreamAsByteArray(stream, -1);
	} finally {
		try {
			stream.close();
		} catch (IOException e) {
			// ignore
		}
	}
	return bytes;
}

protected static String[] readNameTable(String filename) throws java.io.IOException {
	char[] contents = readTable(filename);
	char[][] nameAsChar = CharOperation.splitOn('\n', contents);

	String[] result = new String[nameAsChar.length + 1];
	result[0] = null;
	for (int i = 0; i < nameAsChar.length; i++) {
		result[i + 1] = new String(nameAsChar[i]);
	}

	return result;
}
protected static String[] readReadableNameTable(String filename) {
	String[] result = new String[name.length];

	ResourceBundle bundle;
	try {
		bundle = ResourceBundle.getBundle(filename, Locale.getDefault());
	} catch(MissingResourceException e) {
		System.out.println("Missing resource : " + filename.replace('.', '/') + ".properties for locale " + Locale.getDefault()); //$NON-NLS-1$//$NON-NLS-2$
		throw e;
	}
	for (int i = 0; i < NT_OFFSET + 1; i++) {
		result[i] = name[i];
	}
	for (int i = NT_OFFSET; i < name.length; i++) {
		try {
			String n = bundle.getString(name[i]);
			if(n != null && n.length() > 0) {
				result[i] = n;
			} else {
				result[i] = name[i];
			}
		} catch(MissingResourceException e) {
			result[i] = name[i];
		}
	}
	return result;
}
protected static char[] readTable(String filename) throws java.io.IOException {

	//files are located at Parser.class directory

	InputStream stream = Parser.class.getResourceAsStream(filename);
	if (stream == null) {
		throw new java.io.IOException(Messages.bind(Messages.parser_missingFile, filename));
	}
	byte[] bytes = null;
	try {
		stream = new BufferedInputStream(stream);
		bytes = Util.getInputStreamAsByteArray(stream, -1);
	} finally {
		try {
			stream.close();
		} catch (IOException e) {
			// ignore
		}
	}

	//minimal integrity check (even size expected)
	int length = bytes.length;
	if ((length & 1) != 0)
		throw new java.io.IOException(Messages.bind(Messages.parser_corruptedFile, filename));

	// convert bytes into chars
	char[] chars = new char[length / 2];
	int i = 0;
	int charIndex = 0;

	while (true) {
		chars[charIndex++] = (char) (((bytes[i++] & 0xFF) << 8) + (bytes[i++] & 0xFF));
		if (i == length)
			break;
	}
	return chars;
}
protected static long[] readLongTable(String filename) throws java.io.IOException {

	//files are located at Parser.class directory

	InputStream stream = Parser.class.getResourceAsStream(filename);
	if (stream == null) {
		throw new java.io.IOException(Messages.bind(Messages.parser_missingFile, filename));
	}
	byte[] bytes = null;
	try {
		stream = new BufferedInputStream(stream);
		bytes = Util.getInputStreamAsByteArray(stream, -1);
	} finally {
		try {
			stream.close();
		} catch (IOException e) {
			// ignore
		}
	}

	//minimal integrity check (even size expected)
	int length = bytes.length;
	if (length % 8 != 0)
		throw new java.io.IOException(Messages.bind(Messages.parser_corruptedFile, filename));

	// convert bytes into longs
	long[] longs = new long[length / 8];
	int i = 0;
	int longIndex = 0;

	while (true) {
		longs[longIndex++] =
		  (((long) (bytes[i++] & 0xFF)) << 56)
		+ (((long) (bytes[i++] & 0xFF)) << 48)
		+ (((long) (bytes[i++] & 0xFF)) << 40)
		+ (((long) (bytes[i++] & 0xFF)) << 32)
		+ (((long) (bytes[i++] & 0xFF)) << 24)
		+ (((long) (bytes[i++] & 0xFF)) << 16)
		+ (((long) (bytes[i++] & 0xFF)) << 8)
		+ (bytes[i++] & 0xFF);

		if (i == length)
			break;
	}
	return longs;
}
public static int tAction(int state, int sym) {
	return term_action[term_check[base_action[state]+sym] == sym ? base_action[state] + sym : base_action[state]];
}

public Parser(ProblemReporter problemReporter, boolean optimizeStringLiterals) {

	this.problemReporter = problemReporter;
	this.options = problemReporter.options;
	this.optimizeStringLiterals = optimizeStringLiterals;
	this.initializeScanner();
	this.astLengthStack = new int[50];
	this.expressionLengthStack = new int[30];
	this.intStack = new int[50];
	this.identifierStack = new char[30][];
	this.identifierLengthStack = new int[30];
	this.nestedMethod = new int[30];
	this.realBlockStack = new int[30];
	this.identifierPositionStack = new long[30];
	this.variablesCounter = new int[30];

	// javadoc support
	this.javadocParser = createJavadocParser();
	
}
protected void annotationRecoveryCheckPoint(int start, int end) {
	if(this.lastCheckPoint > start && this.lastCheckPoint < end) {
		this.lastCheckPoint = end + 1;
	}
}
public void arrayInitializer(int length) {
	//length is the size of the array Initializer
	//expressionPtr points on the last elt of the arrayInitializer,
	// in other words, it has not been decremented yet.

	ArrayInitializer ai = new ArrayInitializer();
	if (length != 0) {
		this.expressionPtr -= length;
		System.arraycopy(this.expressionStack, this.expressionPtr + 1, ai.expressions = new Expression[length], 0, length);
	}
	pushOnExpressionStack(ai);
	//positionning
	ai.sourceEnd = this.endStatementPosition;
	ai.sourceStart = this.intStack[this.intPtr--];
}
protected void blockReal() {
	// See consumeLocalVariableDeclarationStatement in case of change: duplicated code
	// increment the amount of declared variables for this block
	this.realBlockStack[this.realBlockPtr]++;
}
/*
 * Build initial recovery state.
 * Recovery state is inferred from the current state of the parser (reduced node stack).
 */
public RecoveredElement buildInitialRecoveryState(){

	/* initialize recovery by retrieving available reduced nodes
	 * also rebuild bracket balance
	 */
	this.lastCheckPoint = 0;
	this.lastErrorEndPositionBeforeRecovery = this.scanner.currentPosition;

	RecoveredElement element = null;
	if (this.referenceContext instanceof CompilationUnitDeclaration){
		if (!DO_DIET_PARSE)
		{
			this.methodRecoveryActivated=true;
//			this.statementRecoveryActivated=true;
		}
		element = new RecoveredUnit(this.compilationUnit, 0, this);

		/* ignore current stack state, since restarting from the beginnning
		   since could not trust simple brace count */
		if (false){ // experimenting restart recovery from scratch
			this.compilationUnit.currentPackage = null;
			this.compilationUnit.imports = null;
			this.compilationUnit.types = null;
			this.compilationUnit.statements = null;
			this.currentToken = 0;
			this.listLength = 0;
			this.listTypeParameterLength = 0;
			this.endPosition = 0;
			this.endStatementPosition = 0;
			return element;
		}
		else
		{
			this.currentToken = 0;
//			if (this.astPtr<0&&this.compilationUnit.sourceEnd>0)
//				this.lastCheckPoint = this.compilationUnit.sourceEnd;

		}

		if (this.compilationUnit.currentPackage != null){
			this.lastCheckPoint = this.compilationUnit.currentPackage.declarationSourceEnd+1;
		}
		if (this.compilationUnit.imports != null){
			this.lastCheckPoint = this.compilationUnit.imports[this.compilationUnit.imports.length -1].declarationSourceEnd+1;
		}
	} else {
		if (this.referenceContext instanceof AbstractMethodDeclaration){
			element = new RecoveredMethod((AbstractMethodDeclaration) this.referenceContext, null, 0, this);
			this.lastCheckPoint = ((AbstractMethodDeclaration) this.referenceContext).bodyStart;
			if(this.statementRecoveryActivated) {
				element = element.add(new Block(0), 0);
			}
		} else {
			/* Initializer bodies are parsed in the context of the type declaration, we must thus search it inside */
			if (this.referenceContext instanceof TypeDeclaration){
				TypeDeclaration type = (TypeDeclaration) this.referenceContext;
				for (int i = 0; i < type.fields.length; i++){
					FieldDeclaration field = type.fields[i];
					if (field != null
						&& field.getKind() == AbstractVariableDeclaration.INITIALIZER
						&& field.declarationSourceStart <= this.scanner.initialPosition
						&& this.scanner.initialPosition <= field.declarationSourceEnd
						&& this.scanner.eofPosition <= field.declarationSourceEnd+1){
						element = new RecoveredInitializer(field, null, 1, this);
						this.lastCheckPoint = field.declarationSourceStart;
						break;
					}
				}
			}
		}
	}

	if (element == null) return element;

	element = recoverAST(element);
	if (this.statementRecoveryActivated) {
		if (this.pendingRecoveredType != null &&
				this.scanner.startPosition - 1 <= this.pendingRecoveredType.declarationSourceEnd) {
			// Add the pending type to the AST if this type isn't already added in the AST.
			element = element.add(this.pendingRecoveredType, 0);
			this.lastCheckPoint = this.pendingRecoveredType.declarationSourceEnd + 1;
			this.pendingRecoveredType = null;
		}
	}
	ProgramElement[] compUnitStatments = this.compilationUnit.statements;
	if (compUnitStatments!=null && compUnitStatments.length>0 &&
			this.lastCheckPoint<compUnitStatments[compUnitStatments.length-1].sourceEnd)
	{
		ProgramElement programElement = compUnitStatments[compUnitStatments.length-1];

		this.lastCheckPoint=((programElement instanceof Expression) ? ((Expression)programElement).statementEnd : programElement.sourceEnd)+1;
	}
	return element;
}

public RecoveredElement recoverAST(RecoveredElement element) {
	for(int i = 0; i <= this.astPtr; i++){
		ASTNode node = this.astStack[i];
		if (node instanceof AbstractMethodDeclaration){
			AbstractMethodDeclaration method = (AbstractMethodDeclaration) node;
			if (method.getName()!=null)
			{
				if (method.declarationSourceEnd == 0){
					element = element.add(method, 0);
					this.lastCheckPoint = method.bodyStart;
				} else {
					element = element.add(method, 0);
					this.lastCheckPoint = method.declarationSourceEnd + 1;
				}
			}
			else
				element = recoverFunctionExpression(element,method);
			continue;
		}
		if (node instanceof Initializer){
			Initializer initializer = (Initializer) node;
			if (initializer.declarationSourceEnd == 0){
				element = element.add(initializer, 1);
				this.lastCheckPoint = initializer.sourceStart;
			} else {
				element = element.add(initializer, 0);
				this.lastCheckPoint = initializer.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof FieldDeclaration){
			FieldDeclaration field = (FieldDeclaration) node;
			if (field.declarationSourceEnd == 0){
				element = element.add(field, 0);
				if (field.initialization == null){
					this.lastCheckPoint = field.sourceEnd + 1;
				} else {
					this.lastCheckPoint = field.initialization.sourceEnd + 1;
				}
			} else {
				element = element.add(field, 0);
				this.lastCheckPoint = field.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof LocalDeclaration){
			LocalDeclaration localDecl = (LocalDeclaration) node;
			if (localDecl.declarationSourceEnd == 0){
				element = element.add(localDecl, 0);
				if (localDecl.initialization == null){
					this.lastCheckPoint = localDecl.sourceEnd + 1;
				} else {
					this.lastCheckPoint = localDecl.initialization.sourceEnd + 1;
				}
			} else {
				element = element.add(localDecl, 0);
				this.lastCheckPoint = localDecl.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof TypeDeclaration){
			TypeDeclaration type = (TypeDeclaration) node;
			if (type.declarationSourceEnd == 0){
				element = element.add(type, 0);
				this.lastCheckPoint = type.bodyStart;
			} else {
				element = element.add(type, 0);
				this.lastCheckPoint = type.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof Statement){
			Statement statement = (Statement) node;
			if (statement.sourceEnd == 0){
				element = element.add(statement, 1);
				this.lastCheckPoint = statement.sourceStart;
			} else {
				element = element.add(statement, 0);
				this.lastCheckPoint = statement.sourceEnd + 1;
			}
			continue;
		}
		if (node instanceof ImportReference){
			ImportReference importRef = (ImportReference) node;
			element = element.add(importRef, 0);
			this.lastCheckPoint = importRef.declarationSourceEnd + 1;
		}
		if(this.statementRecoveryActivated) {
			if(node instanceof Block) {
				Block block = (Block) node;
				element = element.add(block, 0);
				this.lastCheckPoint = block.sourceEnd + 1;
			} else if(node instanceof LocalDeclaration) {
				LocalDeclaration statement = (LocalDeclaration) node;
				element = element.add(statement, 0);
				this.lastCheckPoint = statement.declarationSourceEnd + 1;
			} else if(node instanceof Expression) {
				if(node instanceof Assignment ||
						node instanceof PrefixExpression ||
						node instanceof PostfixExpression ||
						node instanceof MessageSend ||
						node instanceof AllocationExpression) {
					// recover only specific expressions
					Expression statement = (Expression) node;
					element = element.add(statement, 0);
					if(statement.statementEnd != -1) {
						this.lastCheckPoint = statement.statementEnd + 1;
					} else {
						this.lastCheckPoint = statement.sourceEnd + 1;
					}
				}
			} else if(node instanceof Statement) {
				Statement statement = (Statement) node;
				element = element.add(statement, 0);
				this.lastCheckPoint = statement.sourceEnd + 1;
			}
		}
	}
	return element;
}

protected RecoveredElement recoverFunctionExpression(RecoveredElement element, AbstractMethodDeclaration method) {
	int start = method.exprStackPtr;
//	int end=this.expressionPtr;
	boolean isAssignment=true;
	Statement expression=null;

    FunctionExpression funcExpr=new FunctionExpression((MethodDeclaration)method);
    funcExpr.sourceEnd=method.declarationSourceEnd;
    funcExpr.sourceStart=method.sourceStart;


	if (isAssignment && start>=0)
	{
		expression=new Assignment(this.expressionStack[start],funcExpr,method.sourceEnd);
	}
	if (expression!=null)
	{
		element = element.add(expression, 1);
		if (method.declarationSourceEnd == 0){
			element = element.add(method, 0);
			this.lastCheckPoint = method.bodyStart;
		} else {
			element = element.add(method, 0);
			this.lastCheckPoint = method.declarationSourceEnd + 1;
		}
		if (element instanceof RecoveredMethod)
			element.add(new Block(0), 0);
	}

	return element;
}
protected void checkAndSetModifiers(int flag){
	/*modify the current modifiers buffer.
	When the startPosition of the modifiers is 0
	it means that the modifier being parsed is the first
	of a list of several modifiers. The startPosition
	is zeroed when a copy of modifiers-buffer is push
	onto the this.astStack. */

	if ((this.modifiers & flag) != 0){ // duplicate modifier
		this.modifiers |= ExtraCompilerModifiers.AccAlternateModifierProblem;
	}
	this.modifiers |= flag;

	if (this.modifiersSourceStart < 0) this.modifiersSourceStart = this.scanner.startPosition;
}
public void checkComment() {

	// discard obsolete comments while inside methods or fields initializer (see bug 74369)
	// don't discard if the expression being worked on is an ObjectLiteral (see bug 322412 )
		if (!(this.diet && this.dietInt == 0) && this.scanner.commentPtr >= 0 && !(expressionPtr >= 0 && expressionStack[expressionPtr] instanceof ObjectLiteral)) {
			flushCommentsDefinedPriorTo(this.endStatementPosition);
	}

	int lastComment = this.scanner.commentPtr;

//	if (this.modifiersSourceStart >= 0) {
//		// eliminate comments located after modifierSourceStart if positionned
//		while (lastComment >= 0 && this.scanner.commentStarts[lastComment] > this.modifiersSourceStart) lastComment--;
//	}
	if (lastComment >= 0) {
		// consider all remaining leading comments to be part of current declaration
		this.modifiersSourceStart = this.scanner.commentStarts[0];

		// check deprecation in last comment if javadoc (can be followed by non-javadoc comments which are simply ignored)
		while (lastComment >= 0 && this.scanner.commentStops[lastComment] < 0) lastComment--; // non javadoc comment have negative end positions
		if (lastComment >= 0 && this.javadocParser != null) {
			int commentEnd = this.scanner.commentStops[lastComment] - 1; //stop is one over,
			// do not report problem before last parsed comment while recovering code...
			this.javadocParser.reportProblems = this.currentElement == null || commentEnd > this.lastJavadocEnd;
			if (this.javadocParser.checkDeprecation(lastComment)) {
				checkAndSetModifiers(ClassFileConstants.AccDeprecated);
			}
			this.javadoc = this.javadocParser.docComment;	// null if check javadoc is not activated
			if (currentElement == null) this.lastJavadocEnd = commentEnd;
		}
	}
}
protected void checkNonNLSAfterBodyEnd(int declarationEnd){
	if(this.scanner.currentPosition - 1 <= declarationEnd) {
		this.scanner.eofPosition = declarationEnd < Integer.MAX_VALUE ? declarationEnd + 1 : declarationEnd;
		try {
			while(this.scanner.getNextToken() != TokenNameEOF){/*empty*/}
		} catch (InvalidInputException e) {
			// Nothing to do
		}
	}
}
protected void classInstanceCreation(boolean isQualified, boolean isShort) {
	// ClassInstanceCreationExpression ::= 'new' ClassType '(' ArgumentListopt ')' ClassBodyopt

	// ClassBodyopt produces a null item on the astStak if it produces NO class body
	// An empty class body produces a 0 on the length stack.....

	AllocationExpression alloc = new AllocationExpression();
	int length;
//	if (((length = this.astLengthStack[this.astLengthPtr--]) == 1)
//		&& (this.astStack[this.astPtr] == null)) {
//		//NO ClassBody
//		this.astPtr--;
//		if (isQualified) {
//			alloc = new QualifiedAllocationExpression();
//		} else {
			alloc = new AllocationExpression();
//		}

		alloc.isShort=isShort;
		if (!isShort)
		{
			alloc.sourceEnd = this.intStack[this.intPtr--]; //the position has been stored explicitly
			if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
				this.expressionPtr -= length;
				System.arraycopy(
					this.expressionStack,
					this.expressionPtr + 1,
					alloc.arguments = new Expression[length],
					0,
					length);
			}

		}

		alloc.member = this.expressionStack[this.expressionPtr--];
		this.expressionLengthPtr--;

		//the default constructor with the correct number of argument
		//will be created and added by the TC (see createsInternalConstructorWithBinding)
		alloc.sourceStart = this.intStack[this.intPtr--];
		if (isShort)
			alloc.sourceEnd=alloc.member.sourceEnd;
		pushOnExpressionStack(alloc);
//	} else {
//		dispatchDeclarationInto(length);
//		TypeDeclaration anonymousTypeDeclaration = (TypeDeclaration)this.astStack[this.astPtr];
//		anonymousTypeDeclaration.declarationSourceEnd = this.endStatementPosition;
//		anonymousTypeDeclaration.bodyEnd = this.endStatementPosition;
//		if (anonymousTypeDeclaration.allocation != null) {
//			anonymousTypeDeclaration.allocation.sourceEnd = this.endStatementPosition;
//		}
//		if (length == 0 && !containsComment(anonymousTypeDeclaration.bodyStart, anonymousTypeDeclaration.bodyEnd)) {
//			anonymousTypeDeclaration.bits |= ASTNode.UndocumentedEmptyBlock;
//		}
//		this.astPtr--;
//		this.astLengthPtr--;
//
//		// mark initializers with local type mark if needed
//		markInitializersWithLocalType(anonymousTypeDeclaration);
//	}
}
protected void concatExpressionLists() {
	this.expressionLengthStack[--this.expressionLengthPtr]++;
}
protected void concatNodeLists() {
	/*
	 * This is a case where you have two sublists into the this.astStack that you want
	 * to merge in one list. There is no action required on the this.astStack. The only
	 * thing you need to do is merge the two lengths specified on the astStackLength.
	 * The top two length are for example:
	 * ... p   n
	 * and you want to result in a list like:
	 * ... n+p
	 * This means that the p could be equals to 0 in case there is no astNode pushed
	 * on the this.astStack.
	 * Look at the InterfaceMemberDeclarations for an example.
	 */

	this.astLengthStack[this.astLengthPtr - 1] += this.astLengthStack[this.astLengthPtr--];
}
protected void consumeAnnotationAsModifier() {
	Expression expression = this.expressionStack[this.expressionPtr];
	int sourceStart = expression.sourceStart;
	if (this.modifiersSourceStart < 0) {
		this.modifiersSourceStart = sourceStart;
	}
}
protected void consumeArgumentList() {
	// ArgumentList ::= ArgumentList ',' Expression
	concatExpressionLists();
}
protected void consumeArguments() {
	// Arguments ::= '(' ArgumentListopt ')'
	// nothing to do, the expression stack is already updated
	pushOnIntStack(rParenPos);
}
protected void consumeAssignment() {
	// Assignment ::= LeftHandSide AssignmentOperator AssignmentExpression
	//optimize the push/pop

	int op = this.intStack[this.intPtr--] ; //<--the encoded operator

	this.expressionPtr -- ; this.expressionLengthPtr -- ;
	checkComment();

	if(op != EQUAL) {
		CompoundAssignment compoundAssignment = new CompoundAssignment(
			this.expressionStack[this.expressionPtr] ,
			this.expressionStack[this.expressionPtr+1],
			op,
			this.scanner.startPosition - 1);
		if (this.javadoc != null) {
			compoundAssignment.javadoc = this.javadoc;
			this.javadoc = null;
		}
		this.expressionStack[this.expressionPtr] = compoundAssignment;
	}
	else {
		Assignment assignment = new Assignment(
			this.expressionStack[this.expressionPtr] ,
			this.expressionStack[this.expressionPtr+1],
			this.scanner.startPosition - 1);
		if (this.javadoc != null) {
			assignment.javadoc = this.javadoc;
			this.javadoc = null;
		}
		this.expressionStack[this.expressionPtr] = assignment;
	}	

				if (this.pendingRecoveredType != null) {
					// Used only in statements recovery.
					// This is not a real assignment but a placeholder for an existing anonymous type.
					// The assignment must be replace by the anonymous type.
					if (this.pendingRecoveredType.allocation != null &&
							this.scanner.startPosition - 1 <= this.pendingRecoveredType.declarationSourceEnd) {
						this.expressionStack[this.expressionPtr] = this.pendingRecoveredType.allocation;
						this.pendingRecoveredType = null;
						return;
					}
					this.pendingRecoveredType = null;
				}
}
protected void consumeAssignmentOperator(int pos) {
	// AssignmentOperator ::= '='
	// AssignmentOperator ::= '*='
	// AssignmentOperator ::= '/='
	// AssignmentOperator ::= '%='
	// AssignmentOperator ::= '+='
	// AssignmentOperator ::= '-='
	// AssignmentOperator ::= '<<='
	// AssignmentOperator ::= '>>='
	// AssignmentOperator ::= '>>>='
	// AssignmentOperator ::= '&='
	// AssignmentOperator ::= '^='
	// AssignmentOperator ::= '|='

	pushOnIntStack(pos);
}
protected void consumeBinaryExpression(int op) {
	// MultiplicativeExpression ::= MultiplicativeExpression '*' UnaryExpression
	// MultiplicativeExpression ::= MultiplicativeExpression '/' UnaryExpression
	// MultiplicativeExpression ::= MultiplicativeExpression '%' UnaryExpression
	// AdditiveExpression ::= AdditiveExpression '+' MultiplicativeExpression
	// AdditiveExpression ::= AdditiveExpression '-' MultiplicativeExpression
	// ShiftExpression ::= ShiftExpression '<<'  AdditiveExpression
	// ShiftExpression ::= ShiftExpression '>>'  AdditiveExpression
	// ShiftExpression ::= ShiftExpression '>>>' AdditiveExpression
	// RelationalExpression ::= RelationalExpression '<'  ShiftExpression
	// RelationalExpression ::= RelationalExpression '>'  ShiftExpression
	// RelationalExpression ::= RelationalExpression '<=' ShiftExpression
	// RelationalExpression ::= RelationalExpression '>=' ShiftExpression
	// AndExpression ::= AndExpression '&' EqualityExpression
	// ExclusiveOrExpression ::= ExclusiveOrExpression '^' AndExpression
	// InclusiveOrExpression ::= InclusiveOrExpression '|' ExclusiveOrExpression
	// ConditionalAndExpression ::= ConditionalAndExpression '&&' InclusiveOrExpression
	// ConditionalOrExpression ::= ConditionalOrExpression '||' ConditionalAndExpression

	//optimize the push/pop

	this.expressionPtr--;
	this.expressionLengthPtr--;
	Expression expr1 = this.expressionStack[this.expressionPtr];
	Expression expr2 = this.expressionStack[this.expressionPtr + 1];
	switch(op) {
		case OR_OR :
			this.expressionStack[this.expressionPtr] =
				new OR_OR_Expression(
					expr1,
					expr2,
					op);
			break;
		case AND_AND :
			this.expressionStack[this.expressionPtr] =
				new AND_AND_Expression(
					expr1,
					expr2,
					op);
			break;
		case PLUS :
			// look for "string1" + "string2"
			// look for "string1" + "string2"
			if (this.optimizeStringLiterals) {
				if (expr1 instanceof StringLiteral) {
					if (expr2 instanceof StringLiteral) { //string+string
						this.expressionStack[this.expressionPtr] =
							((StringLiteral) expr1).extendWith((StringLiteral) expr2);
					} else {
						this.expressionStack[this.expressionPtr] = new BinaryExpression(expr1, expr2, PLUS);
					}
				} else if (expr1 instanceof CombinedBinaryExpression) {
					CombinedBinaryExpression cursor;
					// left branch is comprised of PLUS BEs
					// cursor is shifted upwards, while needed BEs are added
					// on demand; past the arityMax-th
					// consecutive BE, a CBE is inserted that holds a
					// full-fledged references table
					if ((cursor = (CombinedBinaryExpression)expr1).arity <
								cursor.arityMax) {
						cursor.left = new BinaryExpression(cursor.left,
								cursor.right, PLUS);
						cursor.arity++;
					} else {
						cursor.left = new CombinedBinaryExpression(cursor.left,
								cursor.right, PLUS, cursor.arity);
						cursor.arity = 0;
						cursor.tuneArityMax();
					}
					cursor.right = expr2;
					cursor.sourceEnd = expr2.sourceEnd;
					this.expressionStack[this.expressionPtr] = cursor;
					// BE_INSTRUMENTATION: neutralized in the released code
//					cursor.depthTracker = ((BinaryExpression)cursor.left).
//						depthTracker + 1;
				} else if (expr1 instanceof BinaryExpression &&
							// single out the a + b case, which is a BE
							// instead of a CBE (slightly more than a half of
							// strings concatenation are one-deep binary
							// expressions)
						((expr1.bits & ASTNode.OperatorMASK) >>
							ASTNode.OperatorSHIFT) == OperatorIds.PLUS) {
					this.expressionStack[this.expressionPtr] =
						new CombinedBinaryExpression(expr1, expr2, PLUS, 1);
				} else {
					this.expressionStack[this.expressionPtr] =
						new BinaryExpression(expr1, expr2, PLUS);
				}
			} else if (expr1 instanceof StringLiteral) {
				if (expr2 instanceof StringLiteral) {
					// string + string
					this.expressionStack[this.expressionPtr] =
						((StringLiteral) expr1).extendsWith((StringLiteral) expr2);
				} else {
					// single out the a + b case
					this.expressionStack[this.expressionPtr] =
						new BinaryExpression(expr1, expr2, PLUS);
				}
			} else if (expr1 instanceof CombinedBinaryExpression) {
					CombinedBinaryExpression cursor;
					// shift cursor; create BE/CBE as needed
					if ((cursor = (CombinedBinaryExpression)expr1).arity <
								cursor.arityMax) {
						cursor.left = new BinaryExpression(cursor.left,
								cursor.right, PLUS);
						cursor.arity++;
					} else {
						cursor.left = new CombinedBinaryExpression(cursor.left,
								cursor.right, PLUS, cursor.arity);
						cursor.arity = 0;
						cursor.tuneArityMax();
					}
					cursor.right = expr2;
					cursor.sourceEnd = expr2.sourceEnd;
					// BE_INSTRUMENTATION: neutralized in the released code
//					cursor.depthTracker = ((BinaryExpression)cursor.left).
//						depthTracker + 1;
					this.expressionStack[this.expressionPtr] = cursor;
			} else if (expr1 instanceof BinaryExpression &&
							// single out the a + b case
						((expr1.bits & ASTNode.OperatorMASK) >>
							ASTNode.OperatorSHIFT) == OperatorIds.PLUS) {
				this.expressionStack[this.expressionPtr] =
					new CombinedBinaryExpression(expr1, expr2, PLUS, 1);
			} else {
				this.expressionStack[this.expressionPtr] =
					new BinaryExpression(expr1, expr2, PLUS);
			}
			break;
		case LESS :
			this.intPtr--;
			this.expressionStack[this.expressionPtr] =
				new BinaryExpression(
					expr1,
					expr2,
					op);
			break;
		default :
			this.expressionStack[this.expressionPtr] =
				new BinaryExpression(
					expr1,
					expr2,
					op);
	}
}
protected void consumeBlock() {
	// Block ::= OpenBlock '{' BlockStatementsopt '}'
	// simpler action for empty blocks

	int statementsLength = this.astLengthStack[this.astLengthPtr--];
	Block block;
	if (statementsLength == 0) { // empty block
		block = new Block(0);
		block.sourceStart = this.intStack[this.intPtr--];
		block.sourceEnd = this.endStatementPosition;
		// check whether this block at least contains some comment in it
		if (!containsComment(block.sourceStart, block.sourceEnd)) {
			block.bits |= ASTNode.UndocumentedEmptyBlock;
		}
		this.realBlockPtr--; // still need to pop the block variable counter
	} else {
		block = new Block(this.realBlockStack[this.realBlockPtr--]);
		this.astPtr -= statementsLength;
		System.arraycopy(
			this.astStack,
			this.astPtr + 1,
			block.statements = new Statement[statementsLength],
			0,
			statementsLength);
		block.sourceStart = this.intStack[this.intPtr--];
		block.sourceEnd = this.endStatementPosition;
	}
	pushOnAstStack(block);
}
protected void consumeBlockStatements() {
	// BlockStatements ::= BlockStatements BlockStatement
	concatNodeLists();
}
protected void consumeProgramElements() {
	// BlockStatements ::= BlockStatements BlockStatement
	concatNodeLists();
}
protected void consumeCallExpressionWithArguments() {
	//optimize the push/pop
	//FunctionInvocation ::= Primary '.' 'Identifier' '(' ArgumentListopt ')'

	MessageSend m = newMessageSend();
//	m.sourceStart =
//		(int) ((m.nameSourcePosition = this.identifierPositionStack[this.identifierPtr]) >>> 32);
//	m.selector = this.identifierStack[this.identifierPtr--];
//	this.identifierLengthPtr--;

	Expression receiver = this.expressionStack[this.expressionPtr];
	m.sourceStart = receiver.sourceStart;
	if (receiver instanceof SingleNameReference)
	{
		SingleNameReference singleNameReference = (SingleNameReference)receiver;
		m.selector=singleNameReference.token;
		 m.nameSourcePosition = (((long) singleNameReference.sourceStart) << 32)+(singleNameReference.sourceStart+m.selector.length-1);
		 receiver=null;

	} else if (receiver instanceof FieldReference) {
		FieldReference fieldReference = (FieldReference) receiver;
		m.selector=fieldReference.token;
		m.nameSourcePosition= (((long) (fieldReference.sourceEnd-(m.selector.length-1))) << 32)+(fieldReference.sourceEnd);
		receiver=fieldReference.receiver;
	}


	m.receiver = receiver;
	m.sourceEnd = this.intStack[this.intPtr--];
	this.expressionStack[this.expressionPtr] = m;
}
protected void consumeCallExpressionWithArrayReference() {
	this.expressionPtr--;
	this.expressionLengthPtr--;
	Expression exp =
		this.expressionStack[this.expressionPtr] =
			new ArrayReference(
				this.expressionStack[this.expressionPtr],
				this.expressionStack[this.expressionPtr + 1]);
	exp.sourceEnd = this.endPosition;
}
protected void consumeCallExpressionWithSimpleName() {
	FieldReference fr =
			new FieldReference(
				this.identifierStack[this.identifierPtr],
				this.identifierPositionStack[this.identifierPtr--]);
		this.identifierLengthPtr--;
			//optimize push/pop
		fr.receiver = this.expressionStack[this.expressionPtr];
		//fieldreference begins at the receiver
		fr.sourceStart = fr.receiver.sourceStart;
		this.expressionStack[this.expressionPtr] = fr;
}
protected void consumeCaseLabel() {
	// SwitchLabel ::= 'case' ConstantExpression ':'
	this.expressionLengthPtr--;
	Expression expression = this.expressionStack[this.expressionPtr--];
	pushOnAstStack(new CaseStatement(expression, expression.sourceEnd, this.intStack[this.intPtr--]));
}
protected void consumeCatches() {
	// Catches ::= Catches CatchClause
	optimizedConcatNodeLists();
}
protected void consumeCatchHeader() {
	// CatchDeclaration ::= 'catch' '(' FormalParameter ')' '{'

	if (this.currentElement == null){
		return; // should never occur, this consumeRule is only used in recovery mode
	}
	// current element should be a block due to the presence of the opening brace
	if (!(this.currentElement instanceof RecoveredBlock)){
		if(!(this.currentElement instanceof RecoveredMethod)) {
			return;
		}
		RecoveredMethod rMethod = (RecoveredMethod) this.currentElement;
		if(!(rMethod.methodBody == null && rMethod.bracketBalance > 0)) {
			return;
		}
	}

	Argument arg = (Argument)this.astStack[this.astPtr--];
	// convert argument to local variable
	LocalDeclaration localDeclaration = new LocalDeclaration(arg.name, arg.sourceStart, arg.sourceEnd);
	localDeclaration.type = arg.type;
	localDeclaration.declarationSourceStart = arg.declarationSourceStart;
	localDeclaration.declarationSourceEnd = arg.declarationSourceEnd;

	this.currentElement = this.currentElement.add(localDeclaration, 0);
	this.lastCheckPoint = this.scanner.startPosition; // force to restart at this exact position
	this.restartRecovery = true; // request to restart from here on
	this.lastIgnoredToken = -1;
}
protected void consumeClassOrInterfaceName() {
	pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
	pushOnGenericsLengthStack(0); // handle type arguments
}
protected void consumeCompilationUnit() {
	// JavaScriptUnit ::= EnterCompilationUnit InternalCompilationUnit
	// do nothing by default
}
protected void consumeConditionalExpression(int op) {
	// ConditionalExpression ::= ConditionalOrExpression '?' Expression ':' ConditionalExpression
	//optimize the push/pop
	this.intPtr -= 2;//consume position of the question mark
	this.expressionPtr -= 2;
	this.expressionLengthPtr -= 2;
	this.expressionStack[this.expressionPtr] =
		new ConditionalExpression(
			this.expressionStack[this.expressionPtr],
			this.expressionStack[this.expressionPtr + 1],
			this.expressionStack[this.expressionPtr + 2]);
}
protected void consumeDefaultLabel() {
	// SwitchLabel ::= 'default' ':'
	pushOnAstStack(new CaseStatement(null, this.intStack[this.intPtr--], this.intStack[this.intPtr--]));
}
protected void consumeDefaultModifiers() {
	checkComment(); // might update modifiers with AccDeprecated
	pushOnIntStack(this.modifiers); // modifiers
	pushOnIntStack(
		this.modifiersSourceStart >= 0 ? this.modifiersSourceStart : this.scanner.startPosition);
	resetModifiers();
//	pushOnExpressionStackLengthStack(0); // no annotation
}
protected void consumeDiet() {
	// Diet ::= $empty
	checkComment();
	pushOnIntStack(this.modifiersSourceStart); // push the start position of a javadoc comment if there is one
	resetModifiers();
	jumpOverMethodBody();
}
protected void consumeDebuggerStatement() {
	pushOnAstStack(new DebuggerStatement(this.intStack[this.intPtr--], this.endStatementPosition));
}
protected void consumeEmptyArgumentListopt() {
	// ArgumentListopt ::= $empty
	pushOnExpressionStackLengthStack(0);
}
protected void consumeEmptyArguments() {
	// Argumentsopt ::= $empty
	final FieldDeclaration fieldDeclaration = (FieldDeclaration) this.astStack[this.astPtr];
	pushOnIntStack(fieldDeclaration.sourceEnd);
	pushOnExpressionStackLengthStack(0);
}
protected void consumeEmptyBlockStatementsopt() {
	// BlockStatementsopt ::= $empty
	pushOnAstLengthStack(0);
}
protected void consumeEmptyCatchesopt() {
	// Catchesopt ::= $empty
	pushOnAstLengthStack(0);
}
protected void consumeEmptyExpression() {
	// Expressionopt ::= $empty
	pushOnExpressionStackLengthStack(0);
}
protected void consumeEmptyForInitopt() {
	// ForInitopt ::= $empty
	pushOnAstLengthStack(0);
}
protected void consumeEmptyForUpdateopt() {
	// ForUpdateopt ::= $empty
	pushOnExpressionStackLengthStack(0);
}
protected void consumeEmptyInternalCompilationUnit() {
	// InternalCompilationUnit ::= $empty
	// nothing to do by default
	if (this.compilationUnit.isPackageInfo()) {
		this.compilationUnit.types = new TypeDeclaration[1];
		// create a fake interface declaration
		TypeDeclaration declaration = new TypeDeclaration(compilationUnit.compilationResult);
		declaration.name = TypeConstants.PACKAGE_INFO_NAME;
		declaration.modifiers = ClassFileConstants.AccDefault;
		this.compilationUnit.types[0] = declaration;
		declaration.javadoc = this.compilationUnit.javadoc;
	}
}
protected void consumeEmptyProgramElements() {
	pushOnAstLengthStack(0);
}
protected void consumeEmptyObjectLiteral() {
	ObjectLiteral objectLiteral = new ObjectLiteral();
	objectLiteral.sourceEnd = this.endStatementPosition;
	objectLiteral.sourceStart = this.intStack[this.intPtr--];

	pushOnExpressionStack(objectLiteral);
}
protected void consumeEmptyPropertySetParameterList() {
	pushOnExpressionStackLengthStack(0);
}
protected void consumeEmptyStatement() {
	this.intPtr--;
	// EmptyStatement ::= ';'
	char[] source = this.scanner.source;
	if (this.endStatementPosition >= source.length) {
		// this would be inserted as a fake empty statement
		pushOnAstStack(new EmptyStatement(this.endStatementPosition, this.endStatementPosition));
		return;
	}
	int sourceStart = this.endStatementPosition;
	
	if (source[this.endStatementPosition] != ';') {
		if(source.length > 5 && this.endStatementPosition >= 4) {
			int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
			int pos = this.endStatementPosition - 4;
			while (source[pos] == 'u') {
				pos--;
			}
			if (source[pos] == '\\' &&
					!((c1 = ScannerHelper.getNumericValue(source[this.endStatementPosition - 3])) > 15
						|| c1 < 0
						|| (c2 = ScannerHelper.getNumericValue(source[this.endStatementPosition - 2])) > 15
						|| c2 < 0
						|| (c3 = ScannerHelper.getNumericValue(source[this.endStatementPosition - 1])) > 15
						|| c3 < 0
						|| (c4 = ScannerHelper.getNumericValue(source[this.endStatementPosition])) > 15
						|| c4 < 0) &&
					((char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4)) == ';'){
				// we have a Unicode for the ';' (/u003B)
				sourceStart = pos;
			}
		}
	}
	if (this.astPtr > -1) {
		if (this.astStack[this.astPtr] instanceof IDoStatement) {
			ASTNode node = this.astStack[this.astPtr];
			node.setSourceEnd(this.endStatementPosition);
			pushOnAstLengthStack(0);
			return;
		}
	}
	pushOnAstStack(new EmptyStatement(sourceStart, this.endStatementPosition));
}
protected void consumeEmptySwitchBlock() {
	// SwitchBlock ::= '{' '}'
	pushOnAstLengthStack(0);
}
protected void consumeEnterCompilationUnit() {
	// EnterCompilationUnit ::= $empty
	// do nothing by default
}
protected void consumeEnterVariable() {
	// EnterVariable ::= $empty
	// do nothing by default
	checkComment();
   resetModifiers();
	
	char[] identifierName = this.identifierStack[this.identifierPtr];
	long namePosition = this.identifierPositionStack[this.identifierPtr];
//	int extendedDimension = this.intStack[this.intPtr--];
	AbstractVariableDeclaration declaration;
	// create the ast node
//	boolean isLocalDeclaration = this.nestedMethod[this.nestedType] != 0;
//	if (isLocalDeclaration) {
//		// create the local variable declarations
		declaration =
			this.createLocalDeclaration(identifierName, (int) (namePosition >>> 32), (int) namePosition);
//	} else {
		// create the field declaration
//		declaration =
//			this.createFieldDeclaration(identifierName, (int) (namePosition >>> 32), (int) namePosition);
//	}

	this.identifierPtr--;
	this.identifierLengthPtr--;
//	TypeReference type;
	int variableIndex = this.variablesCounter[this.nestedType];
//	int typeDim = 0;
	if (variableIndex == 0) {
		// first variable of the declaration (FieldDeclaration or LocalDeclaration)
//		if (isLocalDeclaration) {
//			declaration.declarationSourceStart = this.intStack[this.intPtr--];
//			declaration.modifiers = this.intStack[this.intPtr--];
//			// consume annotations
//			int length;
//			if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
//				System.arraycopy(
//					this.expressionStack,
//					(this.expressionPtr -= length) + 1,
//					declaration.annotations = new Annotation[length],
//					0,
//					length);
//			}
//			type = getTypeReference(typeDim = this.intStack[this.intPtr--]); // type dimension
//			if (declaration.declarationSourceStart == -1) {
//				// this is true if there is no modifiers for the local variable declaration
//				declaration.declarationSourceStart = type.sourceStart;
//			}
//			pushOnAstStack(type);
//		} else {
//			type = getTypeReference(typeDim = this.intStack[this.intPtr--]); // type dimension
//			pushOnAstStack(type);
			int modifiersStart = this.intStack[this.intPtr--];
			declaration.modifiers = this.intStack[this.intPtr--];
			int varPosition = this.intStack[this.intPtr--];
			declaration.declarationSourceStart=(modifiersStart>=0)?modifiersStart:varPosition;
			this.expressionLengthPtr--;
			
			// Store javadoc only on first declaration as it is the same for all ones
			LocalDeclaration fieldDeclaration = (LocalDeclaration) declaration;
			fieldDeclaration.javadoc = this.javadoc;
			this.javadoc = null;
//		}
	} else {
//		type = (TypeReference) this.astStack[this.astPtr - variableIndex];
//		typeDim = type.dimensions();
		AbstractVariableDeclaration previousVariable =
			(AbstractVariableDeclaration) this.astStack[this.astPtr];
		declaration.declarationSourceStart = previousVariable.declarationSourceStart;
		declaration.modifiers = previousVariable.modifiers;
//		final Annotation[] annotations = previousVariable.annotations;
//		if (annotations != null) {
//			final int annotationsLength = annotations.length;
//			System.arraycopy(annotations, 0, declaration.annotations = new Annotation[annotationsLength], 0, annotationsLength);
//		}
	}

//	if (extendedDimension == 0) {
//		declaration.type = type;
//	} else {
//		int dimension = typeDim + extendedDimension;
//		declaration.type = this.copyDims(type, dimension);
//	}
	this.variablesCounter[this.nestedType]++;
	pushOnAstStack(declaration);
	// recovery
	if (this.currentElement != null) {
		if (!(this.currentElement instanceof RecoveredUnit)
			&& (this.currentToken == TokenNameDOT
				//|| declaration.modifiers != 0
				|| (Util.getLineNumber(declaration.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
						!= Util.getLineNumber((int) (namePosition >>> 32), this.scanner.lineEnds, 0, this.scanner.linePtr)))){
			this.lastCheckPoint = (int) (namePosition >>> 32);
			this.restartRecovery = true;
			return;
		}
//		if (isLocalDeclaration){
//			LocalDeclaration localDecl = (LocalDeclaration) this.astStack[this.astPtr];
//			this.lastCheckPoint = localDecl.sourceEnd + 1;
//			this.currentElement = this.currentElement.add(localDecl, 0);
//		} else {
			LocalDeclaration fieldDecl = (LocalDeclaration) this.astStack[this.astPtr];
			this.lastCheckPoint = fieldDecl.sourceEnd + 1;
			this.currentElement = this.currentElement.add(fieldDecl, 0);
//		}
		this.lastIgnoredToken = -1;
	}
}
protected void consumeEqualityExpression(int op) {
	// EqualityExpression ::= EqualityExpression '==' RelationalExpression
	// EqualityExpression ::= EqualityExpression '!=' RelationalExpression

	//optimize the push/pop

	this.expressionPtr--;
	this.expressionLengthPtr--;
	this.expressionStack[this.expressionPtr] =
		new EqualExpression(
			this.expressionStack[this.expressionPtr],
			this.expressionStack[this.expressionPtr + 1],
			op);
}
protected void consumeExitTryBlock() {
	//ExitTryBlock ::= $empty
	if(this.currentElement != null) {
		this.restartRecovery = true;
	}
}
protected void consumeExitVariableWithInitialization() {
	// ExitVariableWithInitialization ::= $empty
	// do nothing by default
	this.expressionLengthPtr--;
	AbstractVariableDeclaration variableDecl = (AbstractVariableDeclaration) this.astStack[this.astPtr];
	variableDecl.initialization = this.expressionStack[this.expressionPtr--];
	// we need to update the declarationSourceEnd of the local variable declaration to the
	// source end position of the initialization expression
	variableDecl.declarationSourceEnd = variableDecl.initialization.sourceEnd;
	variableDecl.declarationEnd = variableDecl.initialization.sourceEnd;

	this.recoveryExitFromVariable();
}
protected void consumeExitVariableWithoutInitialization() {
	// ExitVariableWithoutInitialization ::= $empty
	// do nothing by default

	AbstractVariableDeclaration variableDecl = (AbstractVariableDeclaration) this.astStack[this.astPtr];
	variableDecl.declarationSourceEnd = variableDecl.declarationEnd;
	if(this.currentElement != null && this.currentElement instanceof RecoveredField) {
		if(this.endStatementPosition > variableDecl.sourceEnd) {
			this.currentElement.updateSourceEndIfNecessary(this.endStatementPosition);
		}
	}
	this.recoveryExitFromVariable();
}
protected void consumeExpressionStatement() {
	// ExpressionStatement ::= StatementExpression ';'
	this.expressionLengthPtr--;
	Expression expression = this.expressionStack[this.expressionPtr--];
	expression.statementEnd = this.endStatementPosition;
	pushOnAstStack(expression);
}
protected void consumeForceNoDiet() {
	// ForceNoDiet ::= $empty
	this.dietInt++;
}
protected void consumeForInit() {
	// ForInit ::= StatementExpressionList
	pushOnAstLengthStack(-1);
}
protected void consumeFormalParameter(boolean isVarArgs) {
	// FormalParameter ::= Type VariableDeclaratorId ==> false
	// FormalParameter ::= Modifiers Type VariableDeclaratorId ==> true
	/*
	this.astStack :
	this.identifierStack : type identifier
	this.intStack : dim dim
	 ==>
	this.astStack : Argument
	this.identifierStack :
	this.intStack :
	*/

	this.identifierLengthPtr--;
	char[] identifierName = this.identifierStack[this.identifierPtr];
	long namePositions = this.identifierPositionStack[this.identifierPtr--];
//	int extendedDimensions = this.intStack[this.intPtr--];
//	int endOfEllipsis = 0;
//	if (isVarArgs) {
//		endOfEllipsis = this.intStack[this.intPtr--];
//	}
//	int firstDimensions = this.intStack[this.intPtr--];
//	final int typeDimensions = firstDimensions + extendedDimensions;
//	TypeReference type = getTypeReference(typeDimensions);
//	if (isVarArgs) {
//		type = copyDims(type, typeDimensions + 1);
//		if (extendedDimensions == 0) {
//			type.sourceEnd = endOfEllipsis;
//		}
//		type.bits |= ASTNode.IsVarArgs; // set isVarArgs
//	}
//	int modifierPositions = this.intStack[this.intPtr--];
//	this.intPtr--;
	int modifierPositions=(int) (namePositions >>> 32);
	Argument arg =
		new Argument(
			identifierName,
			namePositions,
			null,
			 ClassFileConstants.AccDefault);
//	this.intStack[this.intPtr + 1] & ~ClassFileConstants.AccDeprecated); // modifiers
	arg.declarationSourceStart = modifierPositions;
	// consume annotations
//	int length;
//	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
//		System.arraycopy(
//			this.expressionStack,
//			(this.expressionPtr -= length) + 1,
//			arg.annotations = new Annotation[length],
//			0,
//			length);
//	}
	if (this.options.inferOptions.saveArgumentComments)
	{
		handleArgumentComment(arg);
	}
	
	
	pushOnAstStack(arg);

	/* if incomplete method header, this.listLength counter will not have been reset,
		indicating that some arguments are available on the stack */
	this.listLength++;
}

protected void handleArgumentComment(Argument arg)
{
	int lastComment = this.scanner.commentPtr;

//	if (this.modifiersSourceStart >= 0) {
//		// eliminate comments located after modifierSourceStart if positionned
//		while (lastComment >= 0 && this.scanner.commentStarts[lastComment] > this.modifiersSourceStart) lastComment--;
//	}
	if (lastComment >= 0 && this.scanner.commentStops[0]<0) {
		// consider all remaining leading comments to be part of current declaration
		int start=this.scanner.commentStarts[0];
		int end=this.scanner.commentStops[0];
		arg.comment=CharOperation.subarray(this.scanner.source, start+2, (-end)-2);
		this.scanner.commentPtr=-1;
	}
}
protected void consumeFormalParameterList() {
	// FormalParameterList ::= FormalParameterList ',' FormalParameter
	optimizedConcatNodeLists();
}
protected void consumeFormalParameterListopt() {
	// FormalParameterListopt ::= $empty
	pushOnAstLengthStack(0);
}
protected void consumeGetSetPropertyAssignment(boolean isSetter) {
	// remove two expressions property name/remove an optional property set parameter list
	// remove all statement from function body
	this.intPtr -= 2; // int pushed by consumeNestedMethod() and consumeOpenBlock() (called inside consumeMethodBody())
	int length = this.astLengthStack[this.astLengthPtr--];
	Statement[] statements = new Statement[length];
	this.astPtr -= length;
	System.arraycopy(
			this.astStack,
			this.astPtr + 1,
			statements,
			0,
			length);
	Expression varName = null;
	if (isSetter) {
		this.expressionLengthPtr--;
		varName = this.expressionStack[this.expressionPtr--];
	}
	// property name
	this.expressionLengthPtr--;
	Expression propertyName = this.expressionStack[this.expressionPtr--];
	// set or get
	this.expressionLengthPtr--;
	Expression expression = this.expressionStack[this.expressionPtr--];
	int end = this.endStatementPosition;
	int start = expression.sourceStart;

	if (expression instanceof SingleNameReference) {
		SingleNameReference reference = (SingleNameReference) expression;
		if (isSetter) {
			if (!CharOperation.equals(reference.token, "set".toCharArray())) {
				// report error
				//this.problemReporter().invalidValueForGetterSetter(expression, true);
			}
		} else {
			if (!CharOperation.equals(reference.token, "get".toCharArray())) {
				// report error
				//this.problemReporter().invalidValueForGetterSetter(expression, false);
			}
		}
	}
	ObjectGetterSetterField getterSetterField = new ObjectGetterSetterField(propertyName, statements, varName, start, end);
	pushOnExpressionStack(getterSetterField);
}
protected void consumeInternalCompilationUnitWithTypes() {
	// InternalCompilationUnit ::= PackageDeclaration ImportDeclarations ReduceImports TypeDeclarations
	// InternalCompilationUnit ::= PackageDeclaration TypeDeclarations
	// InternalCompilationUnit ::= TypeDeclarations
	// InternalCompilationUnit ::= ImportDeclarations ReduceImports TypeDeclarations
	// consume type declarations
	int length;
	if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
			this.compilationUnit.statements = new ProgramElement[length];
			this.astPtr -= length;
			System.arraycopy(this.astStack, this.astPtr + 1, this.compilationUnit.statements, 0, length);
	}
}
protected void consumeLabel() {
	// Do nothing
}
protected void consumeLeftParen() {
	// PushLPAREN ::= '('
	pushOnIntStack(this.lParenPos);
}
protected void consumeLocalVariableDeclaration() {
	// LocalVariableDeclaration ::= Modifiers Type VariableDeclarators ';'

	/*
	this.astStack :
	this.expressionStack: Expression Expression ...... Expression
	this.identifierStack : type  identifier identifier ...... identifier
	this.intStack : typeDim      dim        dim               dim
	 ==>
	this.astStack : FieldDeclaration FieldDeclaration ...... FieldDeclaration
	this.expressionStack :
	this.identifierStack :
	this.intStack :

	*/
//	int variableDeclaratorsCounter = this.astLengthStack[this.astLengthPtr];
//
//	// update the this.astStack, this.astPtr and this.astLengthStack
//	int startIndex = this.astPtr - this.variablesCounter[this.nestedType] + 1;
//	System.arraycopy(
//		this.astStack,
//		startIndex,
//		this.astStack,
//		startIndex - 1,
//		variableDeclaratorsCounter);
//	this.astPtr--; // remove the type reference
//	this.astLengthStack[--this.astLengthPtr] = variableDeclaratorsCounter;
	this.variablesCounter[this.nestedType] = 0;
}
protected void consumeLocalVariableDeclarationStatement() {
	// LocalVariableDeclarationStatement ::= LocalVariableDeclaration ';'
	// see blockReal in case of change: duplicated code
	// increment the amount of declared variables for this block
	this.realBlockStack[this.realBlockPtr]++;

	// update source end to include the semi-colon
	int variableDeclaratorsCounter = this.astLengthStack[this.astLengthPtr];
	AbstractVariableDeclaration nextDeclaration =null;
	for (int i = 0; i<variableDeclaratorsCounter; i++) {
		AbstractVariableDeclaration localDeclaration = (AbstractVariableDeclaration) this.astStack[this.astPtr - i];
		localDeclaration.declarationSourceEnd = this.endStatementPosition;
		localDeclaration.declarationEnd = this.endStatementPosition;	// semi-colon included
		localDeclaration.nextLocal=nextDeclaration;
		nextDeclaration=localDeclaration;
	}
	this.astPtr-=variableDeclaratorsCounter-1;
	this.astLengthStack[this.astLengthPtr]=1;
	this.lastCheckPoint = endStatementPosition+1;

}
protected void consumeMemberExpressionWithArrayReference() {
	this.expressionPtr--;
	this.expressionLengthPtr--;
	Expression exp =
		this.expressionStack[this.expressionPtr] =
			new ArrayReference(
				this.expressionStack[this.expressionPtr],
				this.expressionStack[this.expressionPtr + 1]);
	exp.sourceEnd = this.endPosition;
}
protected void consumeMemberExpressionWithSimpleName() {
	FieldReference fr =
		new FieldReference(
			this.identifierStack[this.identifierPtr],
			this.identifierPositionStack[this.identifierPtr--]);
	this.identifierLengthPtr--;
		//optimize push/pop
	fr.receiver = this.expressionStack[this.expressionPtr];
	//fieldreference begins at the receiver
	fr.sourceStart = fr.receiver.sourceStart;
	this.expressionStack[this.expressionPtr] = fr;
}
protected void consumeMethodBody() {
	// MethodBody ::= NestedMethod '{' BlockStatementsopt '}'
	this.nestedMethod[this.nestedType] --;
}
protected void consumeMethodDeclaration(boolean isNotAbstract) {
	// FunctionDeclaration ::= MethodHeader MethodBody
	// AbstractMethodDeclaration ::= MethodHeader ';'

	/*
	this.astStack : modifiers arguments throws statements
	this.identifierStack : type name
	this.intStack : dim dim dim
	 ==>
	this.astStack : FunctionDeclaration
	this.identifierStack :
	this.intStack :
	*/


	this.nestedType--;
	int length;
	if (isNotAbstract) {
		// pop the position of the {  (body of the method) pushed in block decl
		this.intPtr--;
		this.intPtr--;
	}

	int explicitDeclarations = 0;
	Statement[] statements = null;
	if (isNotAbstract) {
		//statements
		explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
		if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
			System.arraycopy(
				this.astStack,
				(this.astPtr -= length) + 1,
				statements = new Statement[length],
				0,
				length);
		}
	}

	// now we know that we have a method declaration at the top of the ast stack
	MethodDeclaration md = (MethodDeclaration) this.astStack[this.astPtr];
	md.statements = statements;
	md.explicitDeclarations = explicitDeclarations;

	// cannot be done in consumeMethodHeader because we have no idea whether or not there
	// is a body when we reduce the method header
	if (!isNotAbstract) { //remember the fact that the method has a semicolon body
		md.modifiers |= ExtraCompilerModifiers.AccSemicolonBody;
	} else if (!(this.diet && this.dietInt == 0) && statements == null && !containsComment(md.bodyStart, this.endPosition)) {
		md.bits |= ASTNode.UndocumentedEmptyBlock;
	}
	// store the this.endPosition (position just before the '}') in case there is
	// a trailing comment behind the end of the method
	md.bodyEnd = this.endPosition;
	md.sourceEnd = this.endPosition;
	md.declarationSourceEnd = flushCommentsDefinedPriorTo(this.endStatementPosition);
}
protected void consumeMethodHeader() {
	// MethodHeader ::= MethodHeaderName MethodHeaderParameters MethodHeaderExtendedDims ThrowsClauseopt
	// AnnotationMethodHeader ::= AnnotationMethodHeaderName FormalParameterListopt MethodHeaderRightParen MethodHeaderExtendedDims AnnotationMethodHeaderDefaultValueopt
	// RecoveryMethodHeader ::= RecoveryMethodHeaderName FormalParameterListopt MethodHeaderRightParen MethodHeaderExtendedDims AnnotationMethodHeaderDefaultValueopt
	// RecoveryMethodHeader ::= RecoveryMethodHeaderName FormalParameterListopt MethodHeaderRightParen MethodHeaderExtendedDims MethodHeaderThrowsClause

	// retrieve end position of method declarator
	AbstractMethodDeclaration method = (AbstractMethodDeclaration)this.astStack[this.astPtr];

	if (this.currentToken == TokenNameLBRACE){
		method.bodyStart = this.scanner.currentPosition;
	}
	 else if (currentToken != TokenNameSEMICOLON) { // insert semicolon
			currentToken = TokenNameSEMICOLON;
			scanner.pushBack();
		}
	// recovery
	if (this.currentElement != null){
//		if(method.isAnnotationMethod()) {
//			method.modifiers |= AccSemicolonBody;
//			method.declarationSourceEnd = this.scanner.currentPosition-1;
//			method.bodyEnd = this.scanner.currentPosition-1;
//			this.currentElement = this.currentElement.parent;
//		} else
		if (this.currentToken == TokenNameSEMICOLON /*&& !method.isAnnotationMethod()*/){
			method.modifiers |= ExtraCompilerModifiers.AccSemicolonBody;
			method.declarationSourceEnd = this.scanner.currentPosition-1;
			method.bodyEnd = this.scanner.currentPosition-1;
//			if (this.currentElement.parseTree() == method && this.currentElement.parent != null) {
//				this.currentElement = this.currentElement.parent;
//			}
		} else if(this.currentToken == TokenNameLBRACE) {
			if (this.currentElement instanceof RecoveredMethod &&
					((RecoveredMethod)this.currentElement).methodDeclaration != method) {
				this.ignoreNextOpeningBrace = true;
				this.currentElement.bracketBalance++;
		}			}
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}
protected void consumeMethodHeaderName(boolean isAnonymous) {
	// MethodHeaderName ::= Modifiersopt Type 'Identifier' '('
	// AnnotationMethodHeaderName ::= Modifiersopt Type 'Identifier' '('
	// RecoveryMethodHeaderName ::= Modifiersopt Type 'Identifier' '('
	MethodDeclaration md = null;
//	if(isAnnotationMethod) {
//		md = new AnnotationMethodDeclaration(this.compilationUnit.compilationResult);
//		this.recordStringLiterals = false;
//	} else {
		md = new MethodDeclaration(this.compilationUnit.compilationResult);
//	}

		md.exprStackPtr=this.expressionPtr;
	//name
		 long selectorSource =-1;
	if (!isAnonymous)
	{
	  md.setSelector(this.identifierStack[this.identifierPtr]);
	   selectorSource = this.identifierPositionStack[this.identifierPtr--];
	  this.identifierLengthPtr--;
	}


	if (this.nestedType>0)
		markEnclosingMemberWithLocalType();

	//type
//	md.returnType = getTypeReference(this.intStack[this.intPtr--]);
	//modifiers
	int functionPos = this.intStack[this.intPtr--];
	int modifierPos = this.intStack[this.intPtr--];
	md.declarationSourceStart = (functionPos>modifierPos)? modifierPos:functionPos;
	md.modifiers = this.intStack[this.intPtr--];
	// consume annotations
//	int length;
//	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
//		System.arraycopy(
//			this.expressionStack,
//			(this.expressionPtr -= length) + 1,
//			md.annotations = new Annotation[length],
//			0,
//			length);
//	}
	// javadoc
	md.javadoc = this.javadoc;
	this.javadoc = null;

	//highlight starts at selector start
	if (selectorSource>=0)
		md.sourceStart = (int) (selectorSource >>> 32);
	else
		md.sourceStart=md.declarationSourceStart;
	pushOnAstStack(md);
	md.sourceEnd = this.lParenPos;
	md.bodyStart = this.lParenPos+1;
	this.listLength = 0; // initialize this.listLength before reading parameters/throws




	incrementNestedType();

	// recovery
	if (this.currentElement != null){
		if (this.currentElement instanceof RecoveredType
			//|| md.modifiers != 0
			|| true/* (this.scanner.getLineNumber(md.returnType.sourceStart)
					== this.scanner.getLineNumber(md.sourceStart))*/){
			this.lastCheckPoint = md.bodyStart;
			this.currentElement = this.currentElement.add(md, 0);
			this.lastIgnoredToken = -1;
		} else {
			this.lastCheckPoint = md.sourceStart;
			this.restartRecovery = true;
		}
	}
}
protected void consumeMethodHeaderRightParen() {
	// MethodHeaderParameters ::= FormalParameterListopt ')'
	int length = this.astLengthStack[this.astLengthPtr--];
	this.astPtr -= length;
	AbstractMethodDeclaration md = (AbstractMethodDeclaration) this.astStack[this.astPtr];
	md.sourceEnd = 	this.rParenPos;
	//arguments
	if (length != 0) {
		System.arraycopy(
			this.astStack,
			this.astPtr + 1,
			md.arguments = new Argument[length],
			0,
			length);
	}
	md.bodyStart = this.rParenPos+1;
	this.listLength = 0; // reset this.listLength after having read all parameters
	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = md.bodyStart;
		if (this.currentElement.parseTree() == md) return;

		// might not have been attached yet - in some constructor scenarii
		if (md.isConstructor()){
			if ((length != 0)
				|| (this.currentToken == TokenNameLBRACE)
				|| (this.currentToken == TokenNamethrows)){
				this.currentElement = this.currentElement.add(md, 0);
				this.lastIgnoredToken = -1;
			}
		}
	}
}
protected void consumeModifiers2() {
	this.expressionLengthStack[this.expressionLengthPtr - 1] += this.expressionLengthStack[this.expressionLengthPtr--];
}
protected void consumeNestedMethod() {
	// NestedMethod ::= $empty
	jumpOverMethodBody();
	this.nestedMethod[this.nestedType] ++;
	pushOnIntStack(this.scanner.currentPosition);
	consumeOpenBlock();
}
protected void consumeNestedType() {
	// NestedType ::= $empty
	incrementNestedType();
}
protected void incrementNestedType() {
	int length = this.nestedMethod.length;
	if (++this.nestedType >= length) {
		System.arraycopy(
			this.nestedMethod, 0,
			this.nestedMethod = new int[length + 30], 0,
			length);
		// increase the size of the variablesCounter as well. It has to be consistent with the size of the nestedMethod collection
		System.arraycopy(
			this.variablesCounter, 0,
			this.variablesCounter = new int[length + 30], 0,
			length);
	}
	this.nestedMethod[this.nestedType] = 0;
	this.variablesCounter[this.nestedType] = 0;
}
protected void consumeNewExpression() {
	classInstanceCreation(false, true);
}
protected void consumeNewMemberExpressionWithArguments() {
	classInstanceCreation(false, false);
}
protected void consumeOpenBlock() {
	// OpenBlock ::= $empty

	pushOnIntStack(this.scanner.startPosition);
	int stackLength = this.realBlockStack.length;
	if (++this.realBlockPtr >= stackLength) {
		System.arraycopy(
			this.realBlockStack, 0,
			this.realBlockStack = new int[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.realBlockStack[this.realBlockPtr] = 0;
}
protected void consumePostfixExpression() {
	// PostfixExpression ::= Name
	pushOnExpressionStack(getUnspecifiedReferenceOptimized());
}
protected void consumePrimaryNoNewArray() {
	// PrimaryNoNewArray ::=  PushLPAREN Expression PushRPAREN
	final Expression parenthesizedExpression = this.expressionStack[this.expressionPtr];
	updateSourcePosition(parenthesizedExpression);
	int numberOfParenthesis = (parenthesizedExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
	parenthesizedExpression.bits &= ~ASTNode.ParenthesizedMASK;
	parenthesizedExpression.bits |= (numberOfParenthesis + 1) << ASTNode.ParenthesizedSHIFT;
}
protected void consumePrimaryNoNewArrayThis() {
	// PrimaryNoNewArray ::= 'this'
	pushOnExpressionStack(new ThisReference(this.intStack[this.intPtr--], this.endPosition));
}
protected void consumePrimarySimpleName() {
	// PrimaryNoNewArray ::= SimpleName
	pushOnExpressionStack(getUnspecifiedReferenceOptimized());
}
protected void consumePropertyAssignment() {
	// MemberValuePair ::= SimpleName '=' MemberValue
	this.modifiersSourceStart=-1;
	this.checkComment();
	this.resetModifiers();

	Expression value = this.expressionStack[this.expressionPtr--];
	this.expressionLengthPtr--;

	Expression field = this.expressionStack[this.expressionPtr--];
	this.expressionLengthPtr--;
	int end = value.sourceEnd;
	int start = field.sourceStart;

	ObjectLiteralField literalField = new ObjectLiteralField(field, value, start, end);
	pushOnExpressionStack(literalField);

	if (this.javadoc!=null) {
		literalField.javaDoc = this.javadoc;
	}
	else if (value instanceof FunctionExpression)
	{
		MethodDeclaration methodDeclaration = ((FunctionExpression)value).methodDeclaration;
		literalField.javaDoc=methodDeclaration.javadoc;
		methodDeclaration.javadoc=null;
	}
	this.javadoc = null;

	// discard obsolete comments while inside methods or fields initializer (see bug 74369)
	if (!(this.diet && this.dietInt==0) && this.scanner.commentPtr >= 0) {
		flushCommentsDefinedPriorTo(literalField.sourceEnd);
	}
	resetModifiers();
}
protected void consumePropertyName() {
	pushOnExpressionStack(getUnspecifiedReferenceOptimized());
}
protected void consumePropertyNameAndValueList() {
	concatExpressionLists();
}
protected void consumePropertySetParameterList() {
	pushOnExpressionStack(getUnspecifiedReferenceOptimized());
}
protected void consumePushLeftBrace() {
	pushOnIntStack(this.endPosition); // modifiers
}

protected void consumeArrayLiteralHeader() {
	pushOnIntStack(this.endPosition); // modifiers
	pushOnIntStack(0); // numExprs
}
protected void consumePushModifiers() {
	checkComment(); // might update modifiers with AccDeprecated
	pushOnIntStack(this.modifiers); // modifiers
	pushOnIntStack(this.modifiersSourceStart);
	resetModifiers();
	pushOnExpressionStackLengthStack(0);
}
protected void consumePushPosition() {
	// for source managment purpose
	// PushPosition ::= $empty
	pushOnIntStack(this.endPosition);
}
protected void consumeQualifiedName() {
	// QualifiedName ::= Name '.' SimpleName
	/*back from the recursive loop of QualifiedName.
	Updates identifier length into the length stack*/

	this.identifierLengthStack[--this.identifierLengthPtr]++;
}
protected void consumeRecoveryMethodHeaderName() {
	// this method is call only inside recovery
//	boolean isAnnotationMethod = false;
//	if(this.currentElement instanceof RecoveredType) {
//		isAnnotationMethod = (((RecoveredType)this.currentElement).typeDeclaration.modifiers & ClassFileConstants.AccAnnotation) != 0;
//	} else {
//		RecoveredType recoveredType = this.currentElement.enclosingType();
//		if(recoveredType != null) {
//			isAnnotationMethod = (recoveredType.typeDeclaration.modifiers & ClassFileConstants.AccAnnotation) != 0;
//		}
//	}
	this.consumeMethodHeaderName(false);
}
protected void consumeRestoreDiet() {
	// RestoreDiet ::= $empty
	this.dietInt--;
}
protected void consumeRightParen() {
	// PushRPAREN ::= ')'
	pushOnIntStack(this.rParenPos);
}
private void consumeFunctionExpression() {

    consumeMethodDeclaration(true);
	MethodDeclaration md = (MethodDeclaration) this.astStack[this.astPtr--];
   this.astLengthPtr--;
    FunctionExpression funcExpr=new FunctionExpression(md);
    funcExpr.sourceEnd=md.declarationSourceEnd;
    funcExpr.sourceStart=md.declarationSourceStart;
    pushOnExpressionStack(funcExpr);
}

private void consumeStatementForIn() {
//	int length;
	Expression collection = null;
	Statement iteratorVar;
	boolean scope = true;

	//statements
	this.astLengthPtr--;
	Statement statement = (Statement) this.astStack[this.astPtr--];

 	if (this.expressionLengthStack[this.expressionLengthPtr--] != 0)
 		collection = this.expressionStack[this.expressionPtr--];

	this.astLengthPtr--;
 	iteratorVar = (Statement) this.astStack[this.astPtr--];
	pushOnAstStack(
		new ForInStatement(
				iteratorVar,
				collection,
			statement,
			scope,
			this.intStack[this.intPtr--],
			this.endStatementPosition));

}

private void consumeArrayLiteralList() {
	concatExpressionLists();
    this.intStack[this.intPtr]&= ~(UNCONSUMED_ELISION|UNCONSUMED_LIT_ELEMENT);
}
private void consumeArrayLiteralListOne() {
	    if ( (this.intStack[this.intPtr]&UNCONSUMED_ELISION)!=0)
	    {
		   concatExpressionLists();
		   this.intStack[this.intPtr]&= ~(UNCONSUMED_ELISION|UNCONSUMED_LIT_ELEMENT);
	    }


}

private void consumeListExpression() {
	this.expressionPtr--;
	this.expressionLengthPtr--;
	Expression expr1 = this.expressionStack[this.expressionPtr];
	Expression expr2 = this.expressionStack[this.expressionPtr + 1];
			this.expressionStack[this.expressionPtr] =
				new ListExpression(
					expr1,
					expr2);

}

protected void consumePostDoc() {
	
	if (this.options.inferOptions.docLocation==InferOptions.DOC_LOCATION_AFTER)
	{
		
	}
}

//This method is part of an automatic generation : do NOT edit-modify  
protected void consumeRule(int act) {
switch ( act ) {
 case 23 : if (DEBUG) { System.out.println("CompilationUnit ::= EnterCompilationUnit..."); }  //$NON-NLS-1$
		    consumeCompilationUnit();  
			break;

 case 24 : if (DEBUG) { System.out.println("InternalCompilationUnit ::= ProgramElements"); }  //$NON-NLS-1$
		    consumeInternalCompilationUnitWithTypes();  
			break;

 case 25 : if (DEBUG) { System.out.println("InternalCompilationUnit ::="); }  //$NON-NLS-1$
		    consumeEmptyInternalCompilationUnit();  
			break;

 case 26 : if (DEBUG) { System.out.println("EnterCompilationUnit ::="); }  //$NON-NLS-1$
		    consumeEnterCompilationUnit();  
			break;

 case 30 : if (DEBUG) { System.out.println("CatchHeader ::= catch LPAREN FormalParameter RPAREN..."); }  //$NON-NLS-1$
		    consumeCatchHeader();  
			break;

 case 32 : if (DEBUG) { System.out.println("VariableDeclarators ::= VariableDeclarators COMMA..."); }  //$NON-NLS-1$
		    consumeVariableDeclarators();  
			break;

 case 34 : if (DEBUG) { System.out.println("VariableDeclaratorsNoIn ::= VariableDeclaratorsNoIn COMMA"); }  //$NON-NLS-1$
		    consumeVariableDeclarators();  
			break;

 case 39 : if (DEBUG) { System.out.println("EnterVariable ::="); }  //$NON-NLS-1$
		    consumeEnterVariable();  
			break;

 case 40 : if (DEBUG) { System.out.println("ExitVariableWithInitialization ::="); }  //$NON-NLS-1$
		    consumeExitVariableWithInitialization();  
			break;

 case 41 : if (DEBUG) { System.out.println("ExitVariableWithoutInitialization ::="); }  //$NON-NLS-1$
		    consumeExitVariableWithoutInitialization();  
			break;

 case 42 : if (DEBUG) { System.out.println("ForceNoDiet ::="); }  //$NON-NLS-1$
		    consumeForceNoDiet();  
			break;

 case 43 : if (DEBUG) { System.out.println("RestoreDiet ::="); }  //$NON-NLS-1$
		    consumeRestoreDiet();  
			break;

 case 47 : if (DEBUG) { System.out.println("FunctionExpression ::= FunctionExpressionHeader..."); }  //$NON-NLS-1$
		    // set to true to consume a method with a body
consumeFunctionExpression();   
			break;

 case 48 : if (DEBUG) { System.out.println("FunctionExpressionHeader ::= FunctionExpressionHeaderName"); }  //$NON-NLS-1$
		    consumeMethodHeader();  
			break;

 case 49 : if (DEBUG) { System.out.println("FunctionExpressionHeaderName ::= Modifiersopt function..."); }  //$NON-NLS-1$
		    consumeMethodHeaderName(false);  
			break;

 case 50 : if (DEBUG) { System.out.println("FunctionExpressionHeaderName ::= Modifiersopt function..."); }  //$NON-NLS-1$
		    consumeMethodHeaderName(true);  
			break;

 case 52 : if (DEBUG) { System.out.println("MethodDeclaration ::= MethodHeader MethodBody"); }  //$NON-NLS-1$
		    // set to true to consume a method with a body
consumeMethodDeclaration(true);   
			break;

 case 53 : if (DEBUG) { System.out.println("AbstractMethodDeclaration ::= MethodHeader SEMICOLON"); }  //$NON-NLS-1$
		    // set to false to consume a method without body
consumeMethodDeclaration(false);  
			break;

 case 54 : if (DEBUG) { System.out.println("MethodHeader ::= MethodHeaderName FormalParameterListopt"); }  //$NON-NLS-1$
		    consumeMethodHeader();  
			break;

 case 55 : if (DEBUG) { System.out.println("MethodHeaderName ::= Modifiersopt function Identifier..."); }  //$NON-NLS-1$
		    consumeMethodHeaderName(false);  
			break;

 case 56 : if (DEBUG) { System.out.println("MethodHeaderRightParen ::= RPAREN"); }  //$NON-NLS-1$
		    consumeMethodHeaderRightParen();  
			break;

 case 58 : if (DEBUG) { System.out.println("FormalParameterList ::= FormalParameterList COMMA..."); }  //$NON-NLS-1$
		    consumeFormalParameterList();  
			break;

 case 59 : if (DEBUG) { System.out.println("FormalParameter ::= VariableDeclaratorId"); }  //$NON-NLS-1$
		    consumeFormalParameter(false);  
			break;

 case 60 : if (DEBUG) { System.out.println("MethodBody ::= NestedMethod LBRACE PostDoc..."); }  //$NON-NLS-1$
		    consumeMethodBody();  
			break;

 case 61 : if (DEBUG) { System.out.println("NestedMethod ::="); }  //$NON-NLS-1$
		    consumeNestedMethod();  
			break;

 case 62 : if (DEBUG) { System.out.println("PostDoc ::="); }  //$NON-NLS-1$
		    consumePostDoc();  
			break;

 case 63 : if (DEBUG) { System.out.println("PushLeftBraceObjectLiteral ::="); }  //$NON-NLS-1$
		    consumePushLeftBrace();  
			break;

 case 64 : if (DEBUG) { System.out.println("Block ::= OpenBlock LBRACE BlockStatementsopt RBRACE"); }  //$NON-NLS-1$
		    consumeBlock();  
			break;

 case 65 : if (DEBUG) { System.out.println("OpenBlock ::="); }  //$NON-NLS-1$
		    consumeOpenBlock() ;  
			break;

 case 67 : if (DEBUG) { System.out.println("ProgramElements ::= ProgramElements ProgramElement"); }  //$NON-NLS-1$
		    consumeProgramElements() ;  
			break;

 case 70 : if (DEBUG) { System.out.println("BlockStatements ::= BlockStatements BlockStatement"); }  //$NON-NLS-1$
		    consumeBlockStatements() ;  
			break;

 case 74 : if (DEBUG) { System.out.println("LocalVariableDeclarationStatement ::=..."); }  //$NON-NLS-1$
		    consumeLocalVariableDeclarationStatement();  
			break;

 case 75 : if (DEBUG) { System.out.println("LocalVariableDeclaration ::= var PushModifiers..."); }  //$NON-NLS-1$
		    consumeLocalVariableDeclaration();  
			break;

 case 76 : if (DEBUG) { System.out.println("LocalVariableDeclarationNoIn ::= var PushModifiers..."); }  //$NON-NLS-1$
		    consumeLocalVariableDeclaration();  
			break;

 case 77 : if (DEBUG) { System.out.println("PushModifiers ::="); }  //$NON-NLS-1$
		    consumePushModifiers();  
			break;

 case 102 : if (DEBUG) { System.out.println("EmptyStatement ::= PushPosition SEMICOLON"); }  //$NON-NLS-1$
		    consumeEmptyStatement();  
			break;

 case 103 : if (DEBUG) { System.out.println("LabeledStatement ::= Label COLON Statement"); }  //$NON-NLS-1$
		    consumeStatementLabel() ;  
			break;

 case 104 : if (DEBUG) { System.out.println("LabeledStatementNoShortIf ::= Label COLON..."); }  //$NON-NLS-1$
		    consumeStatementLabel() ;  
			break;

 case 105 : if (DEBUG) { System.out.println("Label ::= Identifier"); }  //$NON-NLS-1$
		    consumeLabel() ;  
			break;

  case 106 : if (DEBUG) { System.out.println("ExpressionStatement ::= StatementExpression SEMICOLON"); }  //$NON-NLS-1$
		    consumeExpressionStatement();  
			break;

 case 108 : if (DEBUG) { System.out.println("IfThenStatement ::= if LPAREN Expression RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementIfNoElse();  
			break;

 case 109 : if (DEBUG) { System.out.println("IfThenElseStatement ::= if LPAREN Expression RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementIfWithElse();  
			break;

 case 110 : if (DEBUG) { System.out.println("IfThenElseStatementNoShortIf ::= if LPAREN Expression..."); }  //$NON-NLS-1$
		    consumeStatementIfWithElse();  
			break;

 case 111 : if (DEBUG) { System.out.println("IfThenElseStatement ::= if LPAREN Expression RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementIfWithElse();  
			break;

 case 112 : if (DEBUG) { System.out.println("IfThenElseStatementNoShortIf ::= if LPAREN Expression..."); }  //$NON-NLS-1$
		    consumeStatementIfWithElse();  
			break;

 case 113 : if (DEBUG) { System.out.println("SwitchStatement ::= switch LPAREN Expression RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementSwitch() ;  
			break;

 case 114 : if (DEBUG) { System.out.println("SwitchBlock ::= LBRACE RBRACE"); }  //$NON-NLS-1$
		    consumeEmptySwitchBlock() ;  
			break;

 case 117 : if (DEBUG) { System.out.println("SwitchBlock ::= LBRACE SwitchBlockStatements..."); }  //$NON-NLS-1$
		    consumeSwitchBlock() ;  
			break;

 case 119 : if (DEBUG) { System.out.println("SwitchBlockStatements ::= SwitchBlockStatements..."); }  //$NON-NLS-1$
		    consumeSwitchBlockStatements() ;  
			break;

 case 120 : if (DEBUG) { System.out.println("SwitchBlockStatement ::= SwitchLabels BlockStatements"); }  //$NON-NLS-1$
		    consumeSwitchBlockStatement() ;  
			break;

 case 122 : if (DEBUG) { System.out.println("SwitchLabels ::= SwitchLabels SwitchLabel"); }  //$NON-NLS-1$
		    consumeSwitchLabels() ;  
			break;

  case 123 : if (DEBUG) { System.out.println("SwitchLabel ::= case ConstantExpression COLON"); }  //$NON-NLS-1$
		    consumeCaseLabel();  
			break;

  case 124 : if (DEBUG) { System.out.println("SwitchLabel ::= default COLON"); }  //$NON-NLS-1$
		    consumeDefaultLabel();  
			break;

 case 125 : if (DEBUG) { System.out.println("WhileStatement ::= while LPAREN Expression RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementWhile() ;  
			break;

 case 126 : if (DEBUG) { System.out.println("WhileStatementNoShortIf ::= while LPAREN Expression..."); }  //$NON-NLS-1$
		    consumeStatementWhile() ;  
			break;

 case 127 : if (DEBUG) { System.out.println("WithStatement ::= with LPAREN Expression RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementWith() ;  
			break;

 case 128 : if (DEBUG) { System.out.println("WithStatementNoShortIf ::= with LPAREN Expression RPAREN"); }  //$NON-NLS-1$
		    consumeStatementWith() ;  
			break;

 case 129 : if (DEBUG) { System.out.println("DoStatement ::= do Statement while LPAREN Expression..."); }  //$NON-NLS-1$
		    consumeStatementDo() ;  
			break;

 case 130 : if (DEBUG) { System.out.println("ForStatement ::= for LPAREN ForInitopt SEMICOLON..."); }  //$NON-NLS-1$
		    consumeStatementFor() ;  
			break;

 case 131 : if (DEBUG) { System.out.println("ForStatement ::= for LPAREN ForInInit in Expression..."); }  //$NON-NLS-1$
		    consumeStatementForIn() ;  
			break;

 case 132 : if (DEBUG) { System.out.println("ForStatementNoShortIf ::= for LPAREN ForInitopt..."); }  //$NON-NLS-1$
		    consumeStatementFor() ;  
			break;

 case 133 : if (DEBUG) { System.out.println("ForStatementNoShortIf ::= for LPAREN ForInInit in..."); }  //$NON-NLS-1$
		    consumeStatementForIn() ;  
			break;

 case 134 : if (DEBUG) { System.out.println("ForInInit ::= LeftHandSideExpression"); }  //$NON-NLS-1$
		    consumeForInInit() ;  
			break;

 case 136 : if (DEBUG) { System.out.println("ForInit ::= ExpressionNoIn"); }  //$NON-NLS-1$
		    consumeForInit() ;  
			break;

 case 140 : if (DEBUG) { System.out.println("StatementExpressionList ::= StatementExpressionList..."); }  //$NON-NLS-1$
		    consumeStatementExpressionList() ;  
			break;

 case 141 : if (DEBUG) { System.out.println("BreakStatement ::= break SEMICOLON"); }  //$NON-NLS-1$
		    consumeStatementBreak() ;  
			break;

 case 142 : if (DEBUG) { System.out.println("BreakStatement ::= break Identifier SEMICOLON"); }  //$NON-NLS-1$
		    consumeStatementBreakWithLabel() ;  
			break;

 case 143 : if (DEBUG) { System.out.println("ContinueStatement ::= continue SEMICOLON"); }  //$NON-NLS-1$
		    consumeStatementContinue() ;  
			break;

 case 144 : if (DEBUG) { System.out.println("ContinueStatement ::= continue Identifier SEMICOLON"); }  //$NON-NLS-1$
		    consumeStatementContinueWithLabel() ;  
			break;

 case 145 : if (DEBUG) { System.out.println("ReturnStatement ::= return Expressionopt SEMICOLON"); }  //$NON-NLS-1$
		    consumeStatementReturn() ;  
			break;

 case 146 : if (DEBUG) { System.out.println("ThrowStatement ::= throw Expression SEMICOLON"); }  //$NON-NLS-1$
		    consumeStatementThrow();  
			break;

 case 147 : if (DEBUG) { System.out.println("TryStatement ::= try TryBlock Catches"); }  //$NON-NLS-1$
		    consumeStatementTry(false);  
			break;

 case 148 : if (DEBUG) { System.out.println("TryStatement ::= try TryBlock Catchesopt Finally"); }  //$NON-NLS-1$
		    consumeStatementTry(true);  
			break;

 case 150 : if (DEBUG) { System.out.println("ExitTryBlock ::="); }  //$NON-NLS-1$
		    consumeExitTryBlock();  
			break;

 case 152 : if (DEBUG) { System.out.println("Catches ::= Catches CatchClause"); }  //$NON-NLS-1$
		    consumeCatches();  
			break;

 case 153 : if (DEBUG) { System.out.println("CatchClause ::= catch LPAREN FormalParameter RPAREN..."); }  //$NON-NLS-1$
		    consumeStatementCatch() ;  
			break;

 case 155 : if (DEBUG) { System.out.println("DebuggerStatement ::= debugger SEMICOLON"); }  //$NON-NLS-1$
		    consumeDebuggerStatement() ;  
			break;

 case 156 : if (DEBUG) { System.out.println("PushLPAREN ::= LPAREN"); }  //$NON-NLS-1$
		    consumeLeftParen();  
			break;

 case 157 : if (DEBUG) { System.out.println("PushRPAREN ::= RPAREN"); }  //$NON-NLS-1$
		    consumeRightParen();  
			break;

 case 162 : if (DEBUG) { System.out.println("PrimaryNoNewArray ::= SimpleName"); }  //$NON-NLS-1$
		    consumePrimarySimpleName();  
			break;

 case 163 : if (DEBUG) { System.out.println("PrimaryNoNewArray ::= this"); }  //$NON-NLS-1$
		    consumePrimaryNoNewArrayThis();  
			break;

 case 164 : if (DEBUG) { System.out.println("PrimaryNoNewArray ::= PushLPAREN Expression PushRPAREN"); }  //$NON-NLS-1$
		    consumePrimaryNoNewArray();  
			break;

 case 165 : if (DEBUG) { System.out.println("ObjectLiteral ::= LBRACE PushLeftBraceObjectLiteral..."); }  //$NON-NLS-1$
		    consumeEmptyObjectLiteral();  
			break;

 case 166 : if (DEBUG) { System.out.println("ObjectLiteral ::= LBRACE PushLeftBraceObjectLiteral..."); }  //$NON-NLS-1$
		    consumeObjectLiteral();  
			break;

 case 167 : if (DEBUG) { System.out.println("ObjectLiteral ::= LBRACE PushLeftBraceObjectLiteral..."); }  //$NON-NLS-1$
		    consumeObjectLiteral();  
			break;

 case 169 : if (DEBUG) { System.out.println("PropertyNameAndValueList ::= PropertyNameAndValueList..."); }  //$NON-NLS-1$
		    consumePropertyNameAndValueList();  
			break;

 case 170 : if (DEBUG) { System.out.println("PropertyAssignment ::= PropertyName COLON..."); }  //$NON-NLS-1$
		    consumePropertyAssignment();  
			break;

 case 171 : if (DEBUG) { System.out.println("PropertyAssignment ::= PropertyName PropertyName LPAREN"); }  //$NON-NLS-1$
		    consumeGetSetPropertyAssignment(false);  
			break;

 case 172 : if (DEBUG) { System.out.println("PropertyAssignment ::= PropertyName PropertyName LPAREN"); }  //$NON-NLS-1$
		    consumeGetSetPropertyAssignment(true);  
			break;

 case 173 : if (DEBUG) { System.out.println("PropertySetParameterList ::= SimpleName"); }  //$NON-NLS-1$
		    consumePropertySetParameterList();  
			break;

 case 174 : if (DEBUG) { System.out.println("FunctionBody ::= NestedMethod LBRACE PostDoc..."); }  //$NON-NLS-1$
		    consumeMethodBody();  
			break;

 case 175 : if (DEBUG) { System.out.println("ProgramElementsopt ::="); }  //$NON-NLS-1$
		    consumeEmptyProgramElements();  
			break;

 case 177 : if (DEBUG) { System.out.println("PropertyName ::= SimpleName"); }  //$NON-NLS-1$
		    consumePropertyName();  
			break;

 case 181 : if (DEBUG) { System.out.println("ArrayLiteral ::= ArrayLiteralHeader ElisionOpt RBRACKET"); }  //$NON-NLS-1$
		    consumeArrayLiteral(false);  
			break;

 case 182 : if (DEBUG) { System.out.println("ArrayLiteral ::= ArrayLiteralHeader..."); }  //$NON-NLS-1$
		    consumeArrayLiteral(false);  
			break;

 case 183 : if (DEBUG) { System.out.println("ArrayLiteral ::= ArrayLiteralHeader..."); }  //$NON-NLS-1$
		    consumeArrayLiteral(true);  
			break;

 case 184 : if (DEBUG) { System.out.println("ArrayLiteralHeader ::= LBRACKET"); }  //$NON-NLS-1$
		    consumeArrayLiteralHeader();  
			break;

 case 185 : if (DEBUG) { System.out.println("ElisionOpt ::="); }  //$NON-NLS-1$
		    consumeElisionEmpty();  
			break;

 case 187 : if (DEBUG) { System.out.println("Elision ::= COMMA"); }  //$NON-NLS-1$
		    consumeElisionOne();  
			break;

 case 188 : if (DEBUG) { System.out.println("Elision ::= Elision COMMA"); }  //$NON-NLS-1$
		    consumeElisionList();  
			break;

 case 189 : if (DEBUG) { System.out.println("ArrayLiteralElementList ::= ElisionOpt..."); }  //$NON-NLS-1$
		    consumeArrayLiteralListOne();  
			break;

 case 190 : if (DEBUG) { System.out.println("ArrayLiteralElementList ::= ArrayLiteralElementList..."); }  //$NON-NLS-1$
		    consumeArrayLiteralList();  
			break;

 case 191 : if (DEBUG) { System.out.println("ArrayLiteralElement ::= AssignmentExpression"); }  //$NON-NLS-1$
		    consumeArrayLiteralElement();  
			break;

 case 194 : if (DEBUG) { System.out.println("MemberExpression ::= MemberExpression LBRACKET..."); }  //$NON-NLS-1$
		    consumeMemberExpressionWithArrayReference();  
			break;

 case 195 : if (DEBUG) { System.out.println("MemberExpression ::= MemberExpression DOT SimpleName"); }  //$NON-NLS-1$
		    consumeMemberExpressionWithSimpleName();  
			break;

 case 196 : if (DEBUG) { System.out.println("MemberExpression ::= new MemberExpression Arguments"); }  //$NON-NLS-1$
		    consumeNewMemberExpressionWithArguments();  
			break;

 case 198 : if (DEBUG) { System.out.println("NewExpression ::= new NewExpression"); }  //$NON-NLS-1$
		    consumeNewExpression();  
			break;

 case 199 : if (DEBUG) { System.out.println("CallExpression ::= MemberExpression Arguments"); }  //$NON-NLS-1$
		    consumeCallExpressionWithArguments();  
			break;

 case 200 : if (DEBUG) { System.out.println("CallExpression ::= CallExpression Arguments"); }  //$NON-NLS-1$
		    consumeCallExpressionWithArguments();  
			break;

 case 201 : if (DEBUG) { System.out.println("CallExpression ::= CallExpression LBRACKET Expression..."); }  //$NON-NLS-1$
		    consumeCallExpressionWithArrayReference();  
			break;

 case 202 : if (DEBUG) { System.out.println("CallExpression ::= CallExpression DOT SimpleName"); }  //$NON-NLS-1$
		    consumeCallExpressionWithSimpleName();  
			break;

 case 206 : if (DEBUG) { System.out.println("PostfixExpression ::= LeftHandSideExpression PLUS_PLUS"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.PLUS, true);  
			break;

 case 207 : if (DEBUG) { System.out.println("PostfixExpression ::= LeftHandSideExpression MINUS_MINUS"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.MINUS, true);  
			break;

 case 209 : if (DEBUG) { System.out.println("ListExpression ::= ListExpression COMMA..."); }  //$NON-NLS-1$
		    consumeListExpression();  
			break;

 case 211 : if (DEBUG) { System.out.println("ListExpressionNoIn ::= ListExpressionNoIn COMMA..."); }  //$NON-NLS-1$
		    consumeListExpression();  
			break;

 case 213 : if (DEBUG) { System.out.println("ListExpressionStmt ::= ListExpressionStmt COMMA..."); }  //$NON-NLS-1$
		    consumeListExpression();  
			break;

 case 215 : if (DEBUG) { System.out.println("ArgumentList ::= ArgumentList COMMA AssignmentExpression"); }  //$NON-NLS-1$
		    consumeArgumentList();  
			break;

 case 216 : if (DEBUG) { System.out.println("PushPosition ::="); }  //$NON-NLS-1$
		    consumePushPosition();  
			break;

 case 219 : if (DEBUG) { System.out.println("UnaryExpression ::= PLUS PushPosition UnaryExpression"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.PLUS);  
			break;

 case 220 : if (DEBUG) { System.out.println("UnaryExpression ::= MINUS PushPosition UnaryExpression"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.MINUS);  
			break;

 case 222 : if (DEBUG) { System.out.println("PreIncrementExpression ::= PLUS_PLUS PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.PLUS, false);  
			break;

 case 223 : if (DEBUG) { System.out.println("PreDecrementExpression ::= MINUS_MINUS PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.MINUS, false);  
			break;

 case 225 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinus ::= TWIDDLE PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.TWIDDLE);  
			break;

 case 226 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinus ::= NOT PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.NOT);  
			break;

 case 227 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinus ::= delete PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.DELETE);  
			break;

 case 228 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinus ::= void PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.VOID);  
			break;

 case 229 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinus ::= typeof PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.TYPEOF);  
			break;

 case 231 : if (DEBUG) { System.out.println("MultiplicativeExpression ::= MultiplicativeExpression..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.MULTIPLY);  
			break;

 case 232 : if (DEBUG) { System.out.println("MultiplicativeExpression ::= MultiplicativeExpression..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.DIVIDE);  
			break;

 case 233 : if (DEBUG) { System.out.println("MultiplicativeExpression ::= MultiplicativeExpression..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.REMAINDER);  
			break;

 case 235 : if (DEBUG) { System.out.println("AdditiveExpression ::= AdditiveExpression PLUS..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.PLUS);  
			break;

 case 236 : if (DEBUG) { System.out.println("AdditiveExpression ::= AdditiveExpression MINUS..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.MINUS);  
			break;

 case 238 : if (DEBUG) { System.out.println("ShiftExpression ::= ShiftExpression LEFT_SHIFT..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LEFT_SHIFT);  
			break;

 case 239 : if (DEBUG) { System.out.println("ShiftExpression ::= ShiftExpression RIGHT_SHIFT..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.RIGHT_SHIFT);  
			break;

 case 240 : if (DEBUG) { System.out.println("ShiftExpression ::= ShiftExpression UNSIGNED_RIGHT_SHIFT"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.UNSIGNED_RIGHT_SHIFT);  
			break;

 case 242 : if (DEBUG) { System.out.println("RelationalExpression ::= RelationalExpression LESS..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LESS);  
			break;

 case 243 : if (DEBUG) { System.out.println("RelationalExpression ::= RelationalExpression GREATER..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.GREATER);  
			break;

 case 244 : if (DEBUG) { System.out.println("RelationalExpression ::= RelationalExpression LESS_EQUAL"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LESS_EQUAL);  
			break;

 case 245 : if (DEBUG) { System.out.println("RelationalExpression ::= RelationalExpression..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.GREATER_EQUAL);  
			break;

 case 246 : if (DEBUG) { System.out.println("RelationalExpression ::= RelationalExpression instanceof"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.INSTANCEOF);  
			break;

 case 247 : if (DEBUG) { System.out.println("RelationalExpression ::= RelationalExpression in..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.IN);  
			break;

 case 249 : if (DEBUG) { System.out.println("RelationalExpressionNoIn ::= RelationalExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LESS);  
			break;

 case 250 : if (DEBUG) { System.out.println("RelationalExpressionNoIn ::= RelationalExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.GREATER);  
			break;

 case 251 : if (DEBUG) { System.out.println("RelationalExpressionNoIn ::= RelationalExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LESS_EQUAL);  
			break;

 case 252 : if (DEBUG) { System.out.println("RelationalExpressionNoIn ::= RelationalExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.GREATER_EQUAL);  
			break;

 case 253 : if (DEBUG) { System.out.println("RelationalExpressionNoIn ::= RelationalExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.INSTANCEOF);  
			break;

 case 255 : if (DEBUG) { System.out.println("EqualityExpression ::= EqualityExpression EQUAL_EQUAL..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.EQUAL_EQUAL);  
			break;

 case 256 : if (DEBUG) { System.out.println("EqualityExpression ::= EqualityExpression NOT_EQUAL..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.NOT_EQUAL);  
			break;

 case 257 : if (DEBUG) { System.out.println("EqualityExpression ::= EqualityExpression..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.EQUAL_EQUAL_EQUAL);  
			break;

 case 258 : if (DEBUG) { System.out.println("EqualityExpression ::= EqualityExpression..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.NOT_EQUAL_EQUAL);  
			break;

 case 260 : if (DEBUG) { System.out.println("EqualityExpressionNoIn ::= EqualityExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.EQUAL_EQUAL);  
			break;

 case 261 : if (DEBUG) { System.out.println("EqualityExpressionNoIn ::= EqualityExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.NOT_EQUAL);  
			break;

 case 262 : if (DEBUG) { System.out.println("EqualityExpressionNoIn ::= EqualityExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.EQUAL_EQUAL_EQUAL);  
			break;

 case 263 : if (DEBUG) { System.out.println("EqualityExpressionNoIn ::= EqualityExpressionNoIn..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.NOT_EQUAL_EQUAL);  
			break;

 case 265 : if (DEBUG) { System.out.println("AndExpression ::= AndExpression AND EqualityExpression"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.AND);  
			break;

 case 267 : if (DEBUG) { System.out.println("AndExpressionNoIn ::= AndExpressionNoIn AND..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.AND);  
			break;

 case 269 : if (DEBUG) { System.out.println("ExclusiveOrExpression ::= ExclusiveOrExpression XOR..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.XOR);  
			break;

 case 271 : if (DEBUG) { System.out.println("ExclusiveOrExpressionNoIn ::= ExclusiveOrExpressionNoIn"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.XOR);  
			break;

 case 273 : if (DEBUG) { System.out.println("InclusiveOrExpression ::= InclusiveOrExpression OR..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.OR);  
			break;

 case 275 : if (DEBUG) { System.out.println("InclusiveOrExpressionNoIn ::= InclusiveOrExpressionNoIn"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.OR);  
			break;

 case 277 : if (DEBUG) { System.out.println("ConditionalAndExpression ::= ConditionalAndExpression..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.AND_AND);  
			break;

 case 279 : if (DEBUG) { System.out.println("ConditionalAndExpressionNoIn ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.AND_AND);  
			break;

 case 281 : if (DEBUG) { System.out.println("ConditionalOrExpression ::= ConditionalOrExpression..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.OR_OR);  
			break;

 case 283 : if (DEBUG) { System.out.println("ConditionalOrExpressionNoIn ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.OR_OR);  
			break;

 case 285 : if (DEBUG) { System.out.println("ConditionalExpression ::= ConditionalOrExpression..."); }  //$NON-NLS-1$
		    consumeConditionalExpression(OperatorIds.QUESTIONCOLON);  
			break;

 case 287 : if (DEBUG) { System.out.println("ConditionalExpressionNoIn ::=..."); }  //$NON-NLS-1$
		    consumeConditionalExpression(OperatorIds.QUESTIONCOLON);  
			break;

 case 292 : if (DEBUG) { System.out.println("Assignment ::= PostfixExpression AssignmentOperator..."); }  //$NON-NLS-1$
		    consumeAssignment();  
			break;

 case 293 : if (DEBUG) { System.out.println("AssignmentNoIn ::= PostfixExpression AssignmentOperator"); }  //$NON-NLS-1$
		    consumeAssignment();  
			break;

 case 294 : if (DEBUG) { System.out.println("AssignmentOperator ::= EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(EQUAL);  
			break;

 case 295 : if (DEBUG) { System.out.println("AssignmentOperator ::= MULTIPLY_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(MULTIPLY);  
			break;

 case 296 : if (DEBUG) { System.out.println("AssignmentOperator ::= DIVIDE_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(DIVIDE);  
			break;

 case 297 : if (DEBUG) { System.out.println("AssignmentOperator ::= REMAINDER_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(REMAINDER);  
			break;

 case 298 : if (DEBUG) { System.out.println("AssignmentOperator ::= PLUS_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(PLUS);  
			break;

 case 299 : if (DEBUG) { System.out.println("AssignmentOperator ::= MINUS_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(MINUS);  
			break;

 case 300 : if (DEBUG) { System.out.println("AssignmentOperator ::= LEFT_SHIFT_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(LEFT_SHIFT);  
			break;

 case 301 : if (DEBUG) { System.out.println("AssignmentOperator ::= RIGHT_SHIFT_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(RIGHT_SHIFT);  
			break;

 case 302 : if (DEBUG) { System.out.println("AssignmentOperator ::= UNSIGNED_RIGHT_SHIFT_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(UNSIGNED_RIGHT_SHIFT);  
			break;

 case 303 : if (DEBUG) { System.out.println("AssignmentOperator ::= AND_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(AND);  
			break;

 case 304 : if (DEBUG) { System.out.println("AssignmentOperator ::= XOR_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(XOR);  
			break;

 case 305 : if (DEBUG) { System.out.println("AssignmentOperator ::= OR_EQUAL"); }  //$NON-NLS-1$
		    consumeAssignmentOperator(OR);  
			break;

 case 308 : if (DEBUG) { System.out.println("Expressionopt ::="); }  //$NON-NLS-1$
		    consumeEmptyExpression();  
			break;

 case 314 : if (DEBUG) { System.out.println("PrimaryNoNewArrayStmt ::= SimpleName"); }  //$NON-NLS-1$
		    consumePrimarySimpleName();  
			break;

 case 315 : if (DEBUG) { System.out.println("PrimaryNoNewArrayStmt ::= this"); }  //$NON-NLS-1$
		    consumePrimaryNoNewArrayThis();  
			break;

 case 316 : if (DEBUG) { System.out.println("PrimaryNoNewArrayStmt ::= PushLPAREN Expression..."); }  //$NON-NLS-1$
		    consumePrimaryNoNewArray();  
			break;

 case 318 : if (DEBUG) { System.out.println("MemberExpressionStmt ::= MemberExpressionStmt LBRACKET"); }  //$NON-NLS-1$
		    consumeMemberExpressionWithArrayReference();  
			break;

 case 319 : if (DEBUG) { System.out.println("MemberExpressionStmt ::= MemberExpressionStmt DOT..."); }  //$NON-NLS-1$
		    consumeMemberExpressionWithSimpleName();  
			break;

 case 320 : if (DEBUG) { System.out.println("MemberExpressionStmt ::= new MemberExpression Arguments"); }  //$NON-NLS-1$
		    consumeNewMemberExpressionWithArguments();  
			break;

 case 322 : if (DEBUG) { System.out.println("NewExpressionStmt ::= new NewExpression"); }  //$NON-NLS-1$
		    consumeNewExpression();  
			break;

 case 323 : if (DEBUG) { System.out.println("CallExpressionStmt ::= MemberExpressionStmt Arguments"); }  //$NON-NLS-1$
		    consumeCallExpressionWithArguments();  
			break;

 case 324 : if (DEBUG) { System.out.println("CallExpressionStmt ::= CallExpressionStmt Arguments"); }  //$NON-NLS-1$
		    consumeCallExpressionWithArguments();  
			break;

 case 325 : if (DEBUG) { System.out.println("CallExpressionStmt ::= CallExpressionStmt LBRACKET..."); }  //$NON-NLS-1$
		    consumeCallExpressionWithArrayReference();  
			break;

 case 326 : if (DEBUG) { System.out.println("CallExpressionStmt ::= CallExpressionStmt DOT SimpleName"); }  //$NON-NLS-1$
		    consumeCallExpressionWithSimpleName();  
			break;

 case 327 : if (DEBUG) { System.out.println("Arguments ::= LPAREN ArgumentListopt RPAREN"); }  //$NON-NLS-1$
		    consumeArguments();  
			break;

 case 331 : if (DEBUG) { System.out.println("PostfixExpressionStmt ::= LeftHandSideExpressionStmt..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.PLUS, true);  
			break;

 case 332 : if (DEBUG) { System.out.println("PostfixExpressionStmt ::= LeftHandSideExpressionStmt..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.MINUS, true);  
			break;

 case 333 : if (DEBUG) { System.out.println("PreIncrementExpressionStmt ::= PLUS_PLUS PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.PLUS, false);  
			break;

 case 334 : if (DEBUG) { System.out.println("PreDecrementExpressionStmt ::= MINUS_MINUS PushPosition"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.MINUS, false);  
			break;

 case 337 : if (DEBUG) { System.out.println("UnaryExpressionStmt ::= PLUS PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.PLUS);  
			break;

 case 338 : if (DEBUG) { System.out.println("UnaryExpressionStmt ::= MINUS PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.MINUS);  
			break;

 case 341 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinusStmt ::= TWIDDLE PushPosition"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.TWIDDLE);  
			break;

 case 342 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinusStmt ::= NOT PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.NOT);  
			break;

 case 343 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinusStmt ::= delete PushPosition"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.DELETE);  
			break;

 case 344 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinusStmt ::= void PushPosition..."); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.VOID);  
			break;

 case 345 : if (DEBUG) { System.out.println("UnaryExpressionNotPlusMinusStmt ::= typeof PushPosition"); }  //$NON-NLS-1$
		    consumeUnaryExpression(OperatorIds.TYPEOF);  
			break;

 case 347 : if (DEBUG) { System.out.println("MultiplicativeExpressionStmt ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.MULTIPLY);  
			break;

 case 348 : if (DEBUG) { System.out.println("MultiplicativeExpressionStmt ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.DIVIDE);  
			break;

 case 349 : if (DEBUG) { System.out.println("MultiplicativeExpressionStmt ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.REMAINDER);  
			break;

 case 351 : if (DEBUG) { System.out.println("AdditiveExpressionStmt ::= AdditiveExpressionStmt PLUS"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.PLUS);  
			break;

 case 352 : if (DEBUG) { System.out.println("AdditiveExpressionStmt ::= AdditiveExpressionStmt MINUS"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.MINUS);  
			break;

 case 354 : if (DEBUG) { System.out.println("ShiftExpressionStmt ::= ShiftExpressionStmt LEFT_SHIFT"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LEFT_SHIFT);  
			break;

 case 355 : if (DEBUG) { System.out.println("ShiftExpressionStmt ::= ShiftExpressionStmt RIGHT_SHIFT"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.RIGHT_SHIFT);  
			break;

 case 356 : if (DEBUG) { System.out.println("ShiftExpressionStmt ::= ShiftExpressionStmt..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.UNSIGNED_RIGHT_SHIFT);  
			break;

 case 358 : if (DEBUG) { System.out.println("RelationalExpressionStmt ::= RelationalExpressionStmt..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LESS);  
			break;

 case 359 : if (DEBUG) { System.out.println("RelationalExpressionStmt ::= RelationalExpressionStmt..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.GREATER);  
			break;

 case 360 : if (DEBUG) { System.out.println("RelationalExpressionStmt ::= RelationalExpressionStmt..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.LESS_EQUAL);  
			break;

 case 361 : if (DEBUG) { System.out.println("RelationalExpressionStmt ::= RelationalExpressionStmt..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.GREATER_EQUAL);  
			break;

 case 362 : if (DEBUG) { System.out.println("RelationalExpressionStmt ::= RelationalExpressionStmt..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.INSTANCEOF);  
			break;

 case 363 : if (DEBUG) { System.out.println("RelationalExpressionStmt ::= RelationalExpressionStmt in"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.IN);  
			break;

 case 365 : if (DEBUG) { System.out.println("EqualityExpressionStmt ::= EqualityExpressionStmt..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.EQUAL_EQUAL);  
			break;

 case 366 : if (DEBUG) { System.out.println("EqualityExpressionStmt ::= EqualityExpressionStmt..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.NOT_EQUAL);  
			break;

 case 367 : if (DEBUG) { System.out.println("EqualityExpressionStmt ::= EqualityExpressionStmt..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.EQUAL_EQUAL_EQUAL);  
			break;

 case 368 : if (DEBUG) { System.out.println("EqualityExpressionStmt ::= EqualityExpressionStmt..."); }  //$NON-NLS-1$
		    consumeEqualityExpression(OperatorIds.NOT_EQUAL_EQUAL);  
			break;

 case 370 : if (DEBUG) { System.out.println("AndExpressionStmt ::= AndExpressionStmt AND..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.AND);  
			break;

 case 372 : if (DEBUG) { System.out.println("ExclusiveOrExpressionStmt ::= ExclusiveOrExpressionStmt"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.XOR);  
			break;

 case 374 : if (DEBUG) { System.out.println("InclusiveOrExpressionStmt ::= InclusiveOrExpressionStmt"); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.OR);  
			break;

 case 376 : if (DEBUG) { System.out.println("ConditionalAndExpressionStmt ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.AND_AND);  
			break;

 case 378 : if (DEBUG) { System.out.println("ConditionalOrExpressionStmt ::=..."); }  //$NON-NLS-1$
		    consumeBinaryExpression(OperatorIds.OR_OR);  
			break;

 case 380 : if (DEBUG) { System.out.println("ConditionalExpressionStmt ::=..."); }  //$NON-NLS-1$
		    consumeConditionalExpression(OperatorIds.QUESTIONCOLON) ;  
			break;

 case 383 : if (DEBUG) { System.out.println("AssignmentStmt ::= PostfixExpressionStmt..."); }  //$NON-NLS-1$
		    consumeAssignment();  
			break;

  case 384 : if (DEBUG) { System.out.println("Modifiersopt ::="); }  //$NON-NLS-1$
		    consumeDefaultModifiers();  
			break;

 case 385 : if (DEBUG) { System.out.println("BlockStatementsopt ::="); }  //$NON-NLS-1$
		    consumeEmptyBlockStatementsopt();  
			break;

  case 387 : if (DEBUG) { System.out.println("ArgumentListopt ::="); }  //$NON-NLS-1$
		    consumeEmptyArgumentListopt();  
			break;

 case 389 : if (DEBUG) { System.out.println("FormalParameterListopt ::="); }  //$NON-NLS-1$
		    consumeFormalParameterListopt();  
			break;

  case 391 : if (DEBUG) { System.out.println("ForInitopt ::="); }  //$NON-NLS-1$
		    consumeEmptyForInitopt();  
			break;

  case 393 : if (DEBUG) { System.out.println("ForUpdateopt ::="); }  //$NON-NLS-1$
		    consumeEmptyForUpdateopt();  
			break;

  case 395 : if (DEBUG) { System.out.println("Catchesopt ::="); }  //$NON-NLS-1$
		    consumeEmptyCatchesopt();  
			break;

 case 397 : if (DEBUG) { System.out.println("RecoveryMethodHeaderName ::= Modifiersopt function..."); }  //$NON-NLS-1$
		    consumeRecoveryMethodHeaderName();  
			break;

 case 398 : if (DEBUG) { System.out.println("RecoveryMethodHeader ::= RecoveryMethodHeaderName..."); }  //$NON-NLS-1$
		    consumeMethodHeader();  
			break;

	}
}

private void consumeElisionList() {
	int flag=this.intStack[this.intPtr];
	if ((flag&UNCONSUMED_ELISION)!=0)
	{
		pushOnExpressionStack(new EmptyExpression(this.endPosition,this.endPosition));
	}
	concatExpressionLists();
//    this.intStack[this.intPtr]&= ~(UNCONSUMED_ELISION|UNCONSUMED_LIT_ELEMENT);
}
private void consumeElisionOne() {
	pushOnExpressionStack(new EmptyExpression(this.endPosition,this.endPosition));
    if ( (this.intStack[this.intPtr]&UNCONSUMED_LIT_ELEMENT)!=0 || (this.intStack[this.intPtr]&WAS_ARRAY_LIT_ELEMENT)!=0)
		   concatExpressionLists();
	this.intStack[this.intPtr]|=(WAS_ARRAY_LIT_ELEMENT|UNCONSUMED_ELISION) ;

}
private void consumeArrayLiteralElement() {
	this.intStack[this.intPtr]|= (WAS_ARRAY_LIT_ELEMENT|UNCONSUMED_LIT_ELEMENT);
}
private void consumeElisionEmpty() {
}
private void consumeForInInit() {
	Expression expression = this.expressionStack[this.expressionPtr--];
	this.expressionLengthPtr--;
	Statement var = expression;
	pushOnAstStack(var);

}
private void consumeStatementWith() {
	this.expressionLengthPtr--;
	Statement statement = (Statement) this.astStack[this.astPtr];
	this.astStack[this.astPtr] =
		new WithStatement(
			this.expressionStack[this.expressionPtr--],
			statement,
			this.intStack[this.intPtr--],
			this.endStatementPosition);
}

private void consumeArrayLiteral(boolean addElision) {
	int flag=this.intStack[this.intPtr--];
	if (addElision || (flag&UNCONSUMED_ELISION)!=0)
	{
		pushOnExpressionStack(new EmptyExpression(this.endPosition,this.endPosition));
		concatExpressionLists();
	}
	int length = ((flag&WAS_ARRAY_LIT_ELEMENT)>0)? this.expressionLengthStack[this.expressionLengthPtr--] : 0;
	arrayInitializer(length);

}
private void consumeObjectLiteral() {
	ObjectLiteral objectLiteral = new ObjectLiteral();
	int length;
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
		this.expressionPtr -= length;
		System.arraycopy(
			this.expressionStack,
			this.expressionPtr + 1,
			objectLiteral.fields = new ObjectLiteralField[length],
			0,
			length);
	}
	objectLiteral.sourceEnd = this.endStatementPosition;
	objectLiteral.sourceStart = this.intStack[this.intPtr--];

	pushOnExpressionStack(objectLiteral);
}
protected void consumeStatementBreak() {
	// BreakStatement ::= 'break' ';'
	// break pushs a position on this.intStack in case there is no label

	pushOnAstStack(new BreakStatement(null, this.intStack[this.intPtr--], this.endStatementPosition));
	if (this.pendingRecoveredType != null) {
		// Used only in statements recovery.
		// This is not a real break statement but a placeholder for an existing local type.
		// The break statement must be replace by the local type.
		if (this.pendingRecoveredType.allocation == null &&
				this.endPosition <= this.pendingRecoveredType.declarationSourceEnd) {
			this.astStack[this.astPtr] = this.pendingRecoveredType;
			this.pendingRecoveredType = null;
			return;
		}
		this.pendingRecoveredType = null;
	}
}
protected void consumeStatementBreakWithLabel() {
	// BreakStatement ::= 'break' Identifier ';'
	// break pushs a position on this.intStack in case there is no label

	pushOnAstStack(
		new BreakStatement(
			this.identifierStack[this.identifierPtr--],
			this.intStack[this.intPtr--],
			this.endStatementPosition));
	this.identifierLengthPtr--;
}
protected void consumeStatementCatch() {
	// CatchClause ::= 'catch' '(' FormalParameter ')'    Block

	//catch are stored directly into the Try
	//has they always comes two by two....
	//we remove one entry from the astlengthPtr.
	//The construction of the try statement must
	//then fetch the catches using  2*i and 2*i + 1

	this.astLengthPtr--;
	this.listLength = 0; // reset formalParameter counter (incremented for catch variable)
}
protected void consumeStatementContinue() {
	// ContinueStatement ::= 'continue' ';'
	// continue pushs a position on this.intStack in case there is no label

	pushOnAstStack(
		new ContinueStatement(
			null,
			this.intStack[this.intPtr--],
			this.endStatementPosition));
}
protected void consumeStatementContinueWithLabel() {
	// ContinueStatement ::= 'continue' Identifier ';'
	// continue pushs a position on this.intStack in case there is no label

	pushOnAstStack(
		new ContinueStatement(
			this.identifierStack[this.identifierPtr--],
			this.intStack[this.intPtr--],
			this.endStatementPosition));
	this.identifierLengthPtr--;
}
protected void consumeStatementDo() {
	// DoStatement ::= 'do' Statement 'while' '(' Expression ')' ';'

	//the 'while' pushes a value on this.intStack that we need to remove
	this.intPtr--;

	Statement statement = (Statement) this.astStack[this.astPtr];
	this.expressionLengthPtr--;
	this.astStack[this.astPtr] =
		new DoStatement(
			this.expressionStack[this.expressionPtr--],
			statement,
			this.intStack[this.intPtr--],
			this.endStatementPosition);
}
protected void consumeStatementExpressionList() {
	// StatementExpressionList ::= StatementExpressionList ',' StatementExpression
	concatExpressionLists();
}
protected void consumeStatementFor() {
	// ForStatement ::= 'for' '(' ForInitopt ';' Expressionopt ';' ForUpdateopt ')' Statement
	// ForStatementNoShortIf ::= 'for' '(' ForInitopt ';' Expressionopt ';' ForUpdateopt ')' StatementNoShortIf

	int length;
	Expression cond = null;
	Statement[] inits, updates;
	boolean scope = true;

	//statements
	this.astLengthPtr--;
	Statement statement = (Statement) this.astStack[this.astPtr--];

	//updates are on the expresion stack
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) == 0) {
		updates = null;
	} else {
		this.expressionPtr -= length;
		System.arraycopy(
			this.expressionStack,
			this.expressionPtr + 1,
			updates = new Statement[length],
			0,
			length);
	}

	if (this.expressionLengthStack[this.expressionLengthPtr--] != 0)
		cond = this.expressionStack[this.expressionPtr--];

	//inits may be on two different stacks
	if ((length = this.astLengthStack[this.astLengthPtr--]) == 0) {
		inits = null;
		scope = false;
	} else {
		if (length == -1) { //on this.expressionStack
			scope = false;
			length = this.expressionLengthStack[this.expressionLengthPtr--];
			this.expressionPtr -= length;
			System.arraycopy(
				this.expressionStack,
				this.expressionPtr + 1,
				inits = new Statement[length],
				0,
				length);
		} else { //on this.astStack
			this.astPtr -= length;
			System.arraycopy(
				this.astStack,
				this.astPtr + 1,
				inits = new Statement[length],
				0,
				length);
		}
	}
	pushOnAstStack(
		new ForStatement(
			inits,
			cond,
			updates,
			statement,
			scope,
			this.intStack[this.intPtr--],
			this.endStatementPosition));
}
protected void consumeStatementIfNoElse() {
	// IfThenStatement ::=  'if' '(' Expression ')' Statement

	//optimize the push/pop
	this.expressionLengthPtr--;
	Statement thenStatement = (Statement) this.astStack[this.astPtr];
	this.astStack[this.astPtr] =
		new IfStatement(
			this.expressionStack[this.expressionPtr--],
			thenStatement,
			this.intStack[this.intPtr--],
			this.endStatementPosition);
}
protected void consumeStatementIfWithElse() {
	// IfThenElseStatement ::=  'if' '(' Expression ')' StatementNoShortIf 'else' Statement
	// IfThenElseStatementNoShortIf ::=  'if' '(' Expression ')' StatementNoShortIf 'else' StatementNoShortIf

	this.expressionLengthPtr--;

	// optimized {..., Then, Else } ==> {..., If }
	this.astLengthPtr--;

	//optimize the push/pop
	this.astStack[--this.astPtr] =
		new IfStatement(
			this.expressionStack[this.expressionPtr--],
			(Statement) this.astStack[this.astPtr],
			(Statement) this.astStack[this.astPtr + 1],
			this.intStack[this.intPtr--],
			this.endStatementPosition);
}
protected void consumeStatementLabel() {
	// LabeledStatement ::= 'Identifier' ':' Statement
	// LabeledStatementNoShortIf ::= 'Identifier' ':' StatementNoShortIf

	//optimize push/pop
	Statement statement = (Statement) this.astStack[this.astPtr];
	this.astStack[this.astPtr] =
		new LabeledStatement(
			this.identifierStack[this.identifierPtr],
			statement,
			this.identifierPositionStack[this.identifierPtr--],
			this.endStatementPosition);
	this.identifierLengthPtr--;
}
protected void consumeStatementReturn() {
	// ReturnStatement ::= 'return' Expressionopt ';'
	// return pushs a position on this.intStack in case there is no expression

	if (this.expressionLengthStack[this.expressionLengthPtr--] != 0) {
		pushOnAstStack(
			new ReturnStatement(
				this.expressionStack[this.expressionPtr--],
				this.intStack[this.intPtr--],
				this.endStatementPosition)
		);
	} else {
		pushOnAstStack(new ReturnStatement(null, this.intStack[this.intPtr--], this.endStatementPosition));
	}
}
protected void consumeStatementSwitch() {
	// SwitchStatement ::= 'switch' OpenBlock '(' Expression ')' SwitchBlock

	//OpenBlock just makes the semantic action blockStart()
	//the block is inlined but a scope need to be created
	//if some declaration occurs.

	int length;
	SwitchStatement switchStatement = new SwitchStatement();
	this.expressionLengthPtr--;
	switchStatement.expression = this.expressionStack[this.expressionPtr--];
	if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		this.astPtr -= length;
		System.arraycopy(
			this.astStack,
			this.astPtr + 1,
			switchStatement.statements = new Statement[length],
			0,
			length);
	}
	switchStatement.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	pushOnAstStack(switchStatement);
	switchStatement.blockStart = this.intStack[this.intPtr--];
	switchStatement.sourceStart = this.intStack[this.intPtr--];
	switchStatement.sourceEnd = this.endStatementPosition;
	if (length == 0 && !containsComment(switchStatement.blockStart, switchStatement.sourceEnd)) {
		switchStatement.bits |= ASTNode.UndocumentedEmptyBlock;
	}
}

protected void consumeStatementThrow() {
	// ThrowStatement ::= 'throw' Expression ';'
	this.expressionLengthPtr--;
	pushOnAstStack(new ThrowStatement(this.expressionStack[this.expressionPtr--], this.intStack[this.intPtr--], this.endStatementPosition));
}
protected void consumeStatementTry(boolean withFinally) {
	//TryStatement ::= 'try'  Block Catches
	//TryStatement ::= 'try'  Block Catchesopt Finally

	int length;
	TryStatement tryStmt = new TryStatement();
	//finally
	if (withFinally) {
		this.astLengthPtr--;
		tryStmt.finallyBlock = (Block) this.astStack[this.astPtr--];
	}
	//catches are handle by two <argument-block> [see statementCatch]
	if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		if (length == 1) {
			tryStmt.catchBlocks = new Block[] {(Block) this.astStack[this.astPtr--]};
			tryStmt.catchArguments = new Argument[] {(Argument) this.astStack[this.astPtr--]};
		} else {
			Block[] bks = (tryStmt.catchBlocks = new Block[length]);
			Argument[] args = (tryStmt.catchArguments = new Argument[length]);
			while (length-- > 0) {
				bks[length] = (Block) this.astStack[this.astPtr--];
				args[length] = (Argument) this.astStack[this.astPtr--];
			}
		}
	}
	//try
	this.astLengthPtr--;
	tryStmt.tryBlock = (Block) this.astStack[this.astPtr--];

	//positions
	tryStmt.sourceEnd = this.endStatementPosition;
	tryStmt.sourceStart = this.intStack[this.intPtr--];
	pushOnAstStack(tryStmt);
}
protected void consumeStatementWhile() {
	// WhileStatement ::= 'while' '(' Expression ')' Statement
	// WhileStatementNoShortIf ::= 'while' '(' Expression ')' StatementNoShortIf

	this.expressionLengthPtr--;
	Statement statement = (Statement) this.astStack[this.astPtr];
	this.astStack[this.astPtr] =
		new WhileStatement(
			this.expressionStack[this.expressionPtr--],
			statement,
			this.intStack[this.intPtr--],
			this.endStatementPosition);
}

protected void consumeSwitchBlock() {
	// SwitchBlock ::= '{' SwitchBlockStatements SwitchLabels '}'
	concatNodeLists();
}
protected void consumeSwitchBlockStatement() {
	// SwitchBlockStatement ::= SwitchLabels BlockStatements
	concatNodeLists();
}
protected void consumeSwitchBlockStatements() {
	// SwitchBlockStatements ::= SwitchBlockStatements SwitchBlockStatement
	concatNodeLists();
}
protected void consumeSwitchLabels() {
	// SwitchLabels ::= SwitchLabels SwitchLabel
	optimizedConcatNodeLists();
}
protected void consumeToken(int type) {
	/* remember the last consumed value */
	/* try to minimize the number of build values */
//	// clear the commentPtr of the scanner in case we read something different from a modifier
//	switch(type) {
//		case TokenNameabstract :
//		case TokenNamestrictfp :
//		case TokenNamefinal :
//		case TokenNamenative :
//		case TokenNameprivate :
//		case TokenNameprotected :
//		case TokenNamepublic :
//		case TokenNametransient :
//		case TokenNamevolatile :
//		case TokenNamestatic :
//		case TokenNamesynchronized :
//			break;
//		default:
//			this.scanner.commentPtr = -1;
//	}
	//System.out.println(this.scanner.toStringAction(type));
	switch (type) {
		case TokenNameIdentifier :
			pushIdentifier();
			break;
		case TokenNameinterface :
			//'class' is pushing two int (positions) on the stack ==> 'interface' needs to do it too....
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNameabstract :
			checkAndSetModifiers(ClassFileConstants.AccAbstract);
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNamefinal :
			checkAndSetModifiers(ClassFileConstants.AccFinal);
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNamenative :
			checkAndSetModifiers(ClassFileConstants.AccNative);
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNameprivate :
			checkAndSetModifiers(ClassFileConstants.AccPrivate);
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNameprotected :
			checkAndSetModifiers(ClassFileConstants.AccProtected);
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNamepublic :
			checkAndSetModifiers(ClassFileConstants.AccPublic);
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNametransient :
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNamevolatile :
			pushOnExpressionStackLengthStack(0);
			break;
		case TokenNamestatic :
			checkAndSetModifiers(ClassFileConstants.AccStatic);
			pushOnExpressionStackLengthStack(0);
			break;
//		case TokenNamesynchronized :
//			this.synchronizedBlockSourceStart = this.scanner.startPosition;
//			checkAndSetModifiers(ClassFileConstants.AccSynchronized);
//			pushOnExpressionStackLengthStack(0);
//			break;
			//==============================
//		case TokenNamevoid :
//			pushIdentifier(-T_void);
//			pushOnIntStack(this.scanner.currentPosition - 1);
//			pushOnIntStack(this.scanner.startPosition);
//			break;
			//push a default dimension while void is not part of the primitive
			//declaration baseType and so takes the place of a type without getting into
			//regular type parsing that generates a dimension on this.intStack
		case TokenNameboolean :
			pushIdentifier(-T_boolean);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNamechar :
			pushIdentifier(-T_char);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNamedouble :
			pushIdentifier(-T_double);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNamefloat :
			pushIdentifier(-T_float);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNameint :
			pushIdentifier(-T_int);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNamelong :
			pushIdentifier(-T_long);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNameshort :
			pushIdentifier(-T_short);
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
			//==============================
		case TokenNameIntegerLiteral :
			pushOnExpressionStack(
				new IntLiteral(
					this.scanner.getCurrentTokenSource(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1));
			break;
		case TokenNameLongLiteral :
		case TokenNameFloatingPointLiteral :
		case TokenNameDoubleLiteral :
			pushOnExpressionStack(
				new DoubleLiteral(
					this.scanner.getCurrentTokenSource(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1));
			break;
		case TokenNameCharacterLiteral :
			StringLiteral stringLiteral;
			if (this.recordStringLiterals && this.checkExternalizeStrings && !this.statementRecoveryActivated) {
				stringLiteral = this.createStringLiteral(
					this.scanner.getCurrentTokenSourceString(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1,
					Util.getLineNumber(this.scanner.startPosition, this.scanner.lineEnds, 0, this.scanner.linePtr));
				this.compilationUnit.recordStringLiteral(stringLiteral);
			} else {
				stringLiteral = this.createStringLiteral(
					this.scanner.getCurrentTokenSourceString(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1,
					0);
			}
			pushOnExpressionStack(stringLiteral);
			break;
		case TokenNameRegExLiteral :
			pushOnExpressionStack(
				new RegExLiteral(
					this.scanner.getCurrentTokenSource(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1));
			break;

		case TokenNameStringLiteral :
			if (this.recordStringLiterals && this.checkExternalizeStrings && !this.statementRecoveryActivated) {
				stringLiteral = this.createStringLiteral(
					this.scanner.getCurrentTokenSourceString(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1,
					Util.getLineNumber(this.scanner.startPosition, this.scanner.lineEnds, 0, this.scanner.linePtr));
				this.compilationUnit.recordStringLiteral(stringLiteral);
			} else {
				stringLiteral = this.createStringLiteral(
					this.scanner.getCurrentTokenSourceString(),
					this.scanner.startPosition,
					this.scanner.currentPosition - 1,
					0);
			}
			pushOnExpressionStack(stringLiteral);
			break;
		case TokenNamefalse :
			pushOnExpressionStack(
				new FalseLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
			break;
		case TokenNametrue :
			pushOnExpressionStack(
				new TrueLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
			break;
		case TokenNamenull :
			pushOnExpressionStack(
				new NullLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
			break;
		case TokenNameundefined :
			pushOnExpressionStack(
				new UndefinedLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
			break;
			//============================
		case TokenNamesuper :
		case TokenNamethis :
			this.endPosition = this.scanner.currentPosition - 1;
			pushOnIntStack(this.scanner.startPosition);
			break;
//		case TokenNameassert :
		case TokenNameimport :
		case TokenNamepackage :
		case TokenNamethrow :
		case TokenNamedo :
		case TokenNameif :
		case TokenNamefor :
		case TokenNameswitch :
		case TokenNametry :
		case TokenNamewhile :
		case TokenNamebreak :
		case TokenNamecontinue :
		case TokenNamereturn :
		case TokenNamecase :
		case TokenNamedebugger :
		case TokenNameexport :
		case TokenNamefunction :
		case TokenNamevar :
//		case TokenNamein :
//		case TokenNameinfinity :
		case TokenNamewith :
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNamenew :
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40954
			resetModifiers();
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNameclass :
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNameenum :
			pushOnIntStack(this.scanner.currentPosition - 1);
			pushOnIntStack(this.scanner.startPosition);
			break;
		case TokenNamedefault :
			pushOnIntStack(this.scanner.startPosition);
			pushOnIntStack(this.scanner.currentPosition - 1);
			break;
			//let extra semantic action decide when to push
		case TokenNameRBRACKET :
			this.endPosition = this.scanner.startPosition;
			this.endStatementPosition = this.scanner.currentPosition - 1;
			break;
		case TokenNameLBRACKET :
			this.endPosition = this.scanner.startPosition;
			break;
		case TokenNameLBRACE :
			this.endStatementPosition = this.scanner.currentPosition - 1;
		case TokenNamePLUS :
		case TokenNameMINUS :
		case TokenNameNOT :
		case TokenNameTWIDDLE :
		case TokenNamedelete :
		case TokenNamevoid :
		case TokenNametypeof :
			this.endPosition = this.scanner.startPosition;
			break;
		case TokenNamePLUS_PLUS :
		case TokenNameMINUS_MINUS :
			this.endPosition = this.scanner.startPosition;
			this.endStatementPosition = this.scanner.currentPosition - 1;
			break;
		case TokenNameSEMICOLON :
			if (this.insertedSemicolonPosition>0)
			{
				if (this.insertedSemicolonPosition>=this.scanner.source.length)
					this.insertedSemicolonPosition--;
				this.endStatementPosition = this.insertedSemicolonPosition;
				this.endPosition = this.insertedSemicolonPosition;
				this.insertedSemicolonPosition=-1;
				this.problemReporter().missingSemiColon(null, this.endPosition-1,this.endPosition);
				break;
			}// else fallthru
		case TokenNameRBRACE:
			this.endStatementPosition = this.scanner.currentPosition - 1;
			this.endPosition = this.scanner.startPosition - 1;
			//the item is not part of the potential future expression/statement
			break;
		case TokenNameRPAREN :
			// in order to handle ( expression) ////// (cast)expression///// foo(x)
			this.rParenPos = this.scanner.currentPosition - 1; // position of the end of right parenthesis (in case of unicode \u0029) lex00101
			break;
		case TokenNameLPAREN :
			this.lParenPos = this.scanner.startPosition;
			break;
		case TokenNameQUESTION  :
			pushOnIntStack(this.scanner.startPosition);
			pushOnIntStack(this.scanner.currentPosition - 1);
			break;
		case TokenNameLESS :
			pushOnIntStack(this.scanner.startPosition);
			break;
//		case TokenNameELLIPSIS :
//			pushOnIntStack(this.scanner.currentPosition - 1);
//			break;
			//  case TokenNameCOMMA :
			//  case TokenNameCOLON  :
			//  case TokenNameEQUAL  :
			//  case TokenNameLBRACKET  :
			//  case TokenNameDOT :
			//  case TokenNameERROR :
			//  case TokenNameEOF  :
			//  case TokenNamecase  :
			//  case TokenNamecatch  :
			//  case TokenNameelse  :
			//  case TokenNameextends  :
			//  case TokenNamefinally  :
			//  case TokenNameimplements  :
			//  case TokenNamethrows  :
			//  case TokenNameinstanceof  :
			//  case TokenNameEQUAL_EQUAL  :
			//  case TokenNameLESS_EQUAL  :
			//  case TokenNameGREATER_EQUAL  :
			//  case TokenNameNOT_EQUAL  :
			//  case TokenNameLEFT_SHIFT  :
			//  case TokenNameRIGHT_SHIFT  :
			//  case TokenNameUNSIGNED_RIGHT_SHIFT :
			//  case TokenNamePLUS_EQUAL  :
			//  case TokenNameMINUS_EQUAL  :
			//  case TokenNameMULTIPLY_EQUAL  :
			//  case TokenNameDIVIDE_EQUAL  :
			//  case TokenNameAND_EQUAL  :
			//  case TokenNameOR_EQUAL  :
			//  case TokenNameXOR_EQUAL  :
			//  case TokenNameREMAINDER_EQUAL  :
			//  case TokenNameLEFT_SHIFT_EQUAL  :
			//  case TokenNameRIGHT_SHIFT_EQUAL  :
			//  case TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL  :
			//  case TokenNameOR_OR  :
			//  case TokenNameAND_AND  :
			//  case TokenNameREMAINDER :
			//  case TokenNameXOR  :
			//  case TokenNameAND  :
			//  case TokenNameMULTIPLY :
			//  case TokenNameOR  :
			//  case TokenNameDIVIDE :
			//  case TokenNameGREATER  :
	}
}
protected void consumeUnaryExpression(int op) {
	// UnaryExpression ::= '+' PushPosition UnaryExpression
	// UnaryExpression ::= '-' PushPosition UnaryExpression
	// UnaryExpressionNotPlusMinus ::= '~' PushPosition UnaryExpression
	// UnaryExpressionNotPlusMinus ::= '!' PushPosition UnaryExpression

	//optimize the push/pop

	//handle manually the -2147483648 while it is not a real
	//computation of an - and 2147483648 (notice that 2147483648
	//is Integer.MAX_VALUE+1.....)
	//Same for -9223372036854775808L ............

	//this.intStack have the position of the operator

	Expression r, exp = this.expressionStack[this.expressionPtr];
	if (op == MINUS) {
		if ((exp instanceof IntLiteral) && (((IntLiteral) exp).mayRepresentMIN_VALUE())) {
			r = this.expressionStack[this.expressionPtr] = new IntLiteralMinValue();
		} else {
			r = this.expressionStack[this.expressionPtr] = new UnaryExpression(exp, op);
		}
	} else {
		r = this.expressionStack[this.expressionPtr] = new UnaryExpression(exp, op);
	}
	r.sourceStart = this.intStack[this.intPtr--];
	r.sourceEnd = exp.sourceEnd;
}
protected void consumeUnaryExpression(int op, boolean post) {
	// PreIncrementExpression ::= '++' PushPosition UnaryExpression
	// PreDecrementExpression ::= '--' PushPosition UnaryExpression

	// ++ and -- operators
	//optimize the push/pop

	//this.intStack has the position of the operator when prefix

	Expression leftHandSide = this.expressionStack[this.expressionPtr];
	if (leftHandSide instanceof Reference) {
		// ++foo()++ is unvalid
		if (post) {
			this.expressionStack[this.expressionPtr] =
				new PostfixExpression(
					leftHandSide,
					IntLiteral.getOne(),
					op,
					this.endStatementPosition);
		} else {
			this.expressionStack[this.expressionPtr] =
				new PrefixExpression(
					leftHandSide,
					IntLiteral.getOne(),
					op,
					this.intStack[this.intPtr--]);
		}
	} else {
		//the ++ or the -- is NOT taken into account if code gen proceeds
		if (!post) {
			this.intPtr--;
		}
		if(!this.statementRecoveryActivated) problemReporter().invalidUnaryExpression(leftHandSide);
	}
}
protected void consumeVariableDeclarators() {
	// VariableDeclarators ::= VariableDeclarators ',' VariableDeclarator
	optimizedConcatNodeLists();
}
protected void consumeVariableInitializers() {
	// VariableInitializers ::= VariableInitializers ',' VariableInitializer
	concatExpressionLists();
}
/**
 * Given the current comment stack, answer whether some comment is available in a certain exclusive range
 *
 * @param sourceStart int
 * @param sourceEnd int
 * @return boolean
 */
public boolean containsComment(int sourceStart, int sourceEnd) {
	int iComment = this.scanner.commentPtr;
	for (; iComment >= 0; iComment--) {
		int commentStart = this.scanner.commentStarts[iComment];
		// ignore comments before start
		if (commentStart < sourceStart) continue;
		// ignore comments after end
		if (commentStart > sourceEnd) continue;
		return true;
	}
	return false;
}
public MethodDeclaration convertToMethodDeclaration(ConstructorDeclaration c, CompilationResult compilationResult) {
	MethodDeclaration m = new MethodDeclaration(compilationResult);
	m.sourceStart = c.sourceStart;
	m.sourceEnd = c.sourceEnd;
	m.bodyStart = c.bodyStart;
	m.bodyEnd = c.bodyEnd;
	m.declarationSourceEnd = c.declarationSourceEnd;
	m.declarationSourceStart = c.declarationSourceStart;
	m.setSelector(c.getName());
	m.statements = c.statements;
	m.modifiers = c.modifiers;
	m.arguments = c.arguments;
	m.explicitDeclarations = c.explicitDeclarations;
	m.returnType = null;
	m.javadoc = c.javadoc;
	return m;
}
protected TypeReference copyDims(TypeReference typeRef, int dim) {
	return typeRef.copyDims(dim);
}
protected FieldDeclaration createFieldDeclaration(char[] fieldDeclarationName, int sourceStart, int sourceEnd) {
	return new FieldDeclaration(fieldDeclarationName, sourceStart, sourceEnd);
}
protected JavadocParser createJavadocParser() {
	return new JavadocParser(this);
}
protected LocalDeclaration createLocalDeclaration(char[] localDeclarationName, int sourceStart, int sourceEnd) {
	return new LocalDeclaration(localDeclarationName, sourceStart, sourceEnd);
}
protected StringLiteral createStringLiteral(char[] token, int start, int end, int lineNumber) {
	return new StringLiteral(token, start, end, lineNumber);
}

	protected RecoveredType currentRecoveryType() {
	if(this.currentElement != null) {
		if(this.currentElement instanceof RecoveredType) {
			return (RecoveredType) this.currentElement;
		} else {
			return this.currentElement.enclosingType();
		}
	}
	return null;
}
public CompilationUnitDeclaration dietParse(ICompilationUnit sourceUnit, CompilationResult compilationResult) {

	CompilationUnitDeclaration parsedUnit;
	boolean old = this.diet;
	try {
		this.diet = DO_DIET_PARSE;
		parsedUnit = parse(sourceUnit, compilationResult);
	} finally {
		this.diet = old;
	}
	return parsedUnit;
}
protected void dispatchDeclarationInto(int length) {
	/* they are length on this.astStack that should go into
	   methods fields constructors lists of the typeDecl

	   Return if there is a constructor declaration in the methods declaration */


	// Looks for the size of each array .

	if (length == 0)
		return;
	int[] flag = new int[length + 1]; //plus one -- see <HERE>
	int size1 = 0, size2 = 0, size3 = 0;
	boolean hasAbstractMethods = false;
	for (int i = length - 1; i >= 0; i--) {
		ASTNode astNode = this.astStack[this.astPtr--];
		if (astNode instanceof AbstractMethodDeclaration) {
			//methods and constructors have been regrouped into one single list
			flag[i] = 2;
			size2++;
			if (((AbstractMethodDeclaration) astNode).isAbstract()) {
				hasAbstractMethods = true;
			}
		} else if (astNode instanceof TypeDeclaration) {
			flag[i] = 3;
			size3++;
		} else {
			//field
			flag[i] = 1;
			size1++;
		}
	}

	//arrays creation
	TypeDeclaration typeDecl = (TypeDeclaration) this.astStack[this.astPtr];
	if (size1 != 0) {
		typeDecl.fields = new FieldDeclaration[size1];
	}
	if (size2 != 0) {
		typeDecl.methods = new AbstractMethodDeclaration[size2];
		if (hasAbstractMethods) typeDecl.bits |= ASTNode.HasAbstractMethods;
	}
	if (size3 != 0) {
		typeDecl.memberTypes = new TypeDeclaration[size3];
	}

	//arrays fill up
	size1 = size2 = size3 = 0;
	int flagI = flag[0], start = 0;
	int length2;
	for (int end = 0; end <= length; end++) //<HERE> the plus one allows to
		{
		if (flagI != flag[end]) //treat the last element as a ended flag.....
			{ //array copy
			switch (flagI) {
				case 1 :
					size1 += (length2 = end - start);
					System.arraycopy(
						this.astStack,
						this.astPtr + start + 1,
						typeDecl.fields,
						size1 - length2,
						length2);
					break;
				case 2 :
					size2 += (length2 = end - start);
					System.arraycopy(
						this.astStack,
						this.astPtr + start + 1,
						typeDecl.methods,
						size2 - length2,
						length2);
					break;
				case 3 :
					size3 += (length2 = end - start);
					System.arraycopy(
						this.astStack,
						this.astPtr + start + 1,
						typeDecl.memberTypes,
						size3 - length2,
						length2);
					break;
			}
			flagI = flag[start = end];
		}
	}

	if (typeDecl.memberTypes != null) {
		for (int i = typeDecl.memberTypes.length - 1; i >= 0; i--) {
			typeDecl.memberTypes[i].enclosingType = typeDecl;
		}
	}
}
protected void dispatchDeclarationIntoEnumDeclaration(int length) {

	if (length == 0)
      return;
   int[] flag = new int[length + 1]; //plus one -- see <HERE>
   int size1 = 0, size2 = 0, size3 = 0;
   TypeDeclaration enumDeclaration = (TypeDeclaration) this.astStack[this.astPtr - length];
   boolean hasAbstractMethods = false;
   for (int i = length - 1; i >= 0; i--) {
      ASTNode astNode = this.astStack[this.astPtr--];
      if (astNode instanceof AbstractMethodDeclaration) {
         //methods and constructors have been regrouped into one single list
         flag[i] = 2;
         size2++;
		if (((AbstractMethodDeclaration) astNode).isAbstract()) {
			hasAbstractMethods = true;
		}
      } else if (astNode instanceof TypeDeclaration) {
         flag[i] = 3;
         size3++;
      } else if (astNode instanceof FieldDeclaration) {
         flag[i] = 1;
         size1++;
//         if(astNode instanceof EnumConstant) {
//            EnumConstant constant = (EnumConstant) astNode;
//            ((AllocationExpression)constant.initialization).type = new SingleTypeReference(enumDeclaration.name,
//                  (((long) enumDeclaration.sourceStart) << 32) + enumDeclaration.sourceEnd);
//         }
      }
   }

   //arrays creation
   if (size1 != 0) {
      enumDeclaration.fields = new FieldDeclaration[size1];
   }
   if (size2 != 0) {
      enumDeclaration.methods = new AbstractMethodDeclaration[size2];
      if (hasAbstractMethods) enumDeclaration.bits |= ASTNode.HasAbstractMethods;
   }
   if (size3 != 0) {
      enumDeclaration.memberTypes = new TypeDeclaration[size3];
   }

   //arrays fill up
   size1 = size2 = size3 = 0;
   int flagI = flag[0], start = 0;
   int length2;
   for (int end = 0; end <= length; end++) //<HERE> the plus one allows to
      {
      if (flagI != flag[end]) //treat the last element as a ended flag.....
         { //array copy
         switch (flagI) {
            case 1 :
               size1 += (length2 = end - start);
               System.arraycopy(
                  this.astStack,
                  this.astPtr + start + 1,
                  enumDeclaration.fields,
                  size1 - length2,
                  length2);
               break;
            case 2 :
               size2 += (length2 = end - start);
               System.arraycopy(
                  this.astStack,
                  this.astPtr + start + 1,
                  enumDeclaration.methods,
                  size2 - length2,
                  length2);
               break;
            case 3 :
               size3 += (length2 = end - start);
               System.arraycopy(
                  this.astStack,
                  this.astPtr + start + 1,
                  enumDeclaration.memberTypes,
                  size3 - length2,
                  length2);
               break;
         }
         flagI = flag[start = end];
      }
   }

   if (enumDeclaration.memberTypes != null) {
      for (int i = enumDeclaration.memberTypes.length - 1; i >= 0; i--) {
         enumDeclaration.memberTypes[i].enclosingType = enumDeclaration;
      }
   }}
protected CompilationUnitDeclaration endParse(int act) {

	this.lastAct = act;

	if(this.statementRecoveryActivated ) {
		RecoveredElement recoveredElement = this.buildInitialRecoveryState();
		recoveredElement.topElement().updateParseTree();
		if(this.hasError) this.resetStacks();
	} else if (this.currentElement != null){
		if (VERBOSE_RECOVERY){
			System.out.print(Messages.parser_syntaxRecovery);
			System.out.println("--------------------------");		 //$NON-NLS-1$
			System.out.println(this.compilationUnit);
			System.out.println("----------------------------------"); //$NON-NLS-1$
		}
		recoverAST(this.currentElement);
		this.currentElement.topElement().updateParseTree();
	} else {
		if (this.diet & VERBOSE_RECOVERY){
			System.out.print(Messages.parser_regularParse);
			System.out.println("--------------------------");	 //$NON-NLS-1$
			System.out.println(this.compilationUnit);
			System.out.println("----------------------------------"); //$NON-NLS-1$
		}
	}
	persistLineSeparatorPositions();
	for (int i = 0; i < this.scanner.foundTaskCount; i++){
		if(!this.statementRecoveryActivated) problemReporter().task(
			new String(this.scanner.foundTaskTags[i]),
			new String(this.scanner.foundTaskMessages[i]),
			this.scanner.foundTaskPriorities[i] == null ? null : new String(this.scanner.foundTaskPriorities[i]),
			this.scanner.foundTaskPositions[i][0],
			this.scanner.foundTaskPositions[i][1]);
	}
	if (this.compilationUnit.statements==null)
		this.compilationUnit.statements=new ProgramElement[0];
	return this.compilationUnit;
}
/*
 * Flush comments defined prior to a given positions.
 *
 * Note: comments are stacked in syntactical order
 *
 * Either answer given <position>, or the end position of a comment line
 * immediately following the <position> (same line)
 *
 * e.g.
 * void foo(){
 * } // end of method foo
 */

public int flushCommentsDefinedPriorTo(int position) {

	int lastCommentIndex = this.scanner.commentPtr;
	if (lastCommentIndex < 0) return position; // no comment

	// compute the index of the first obsolete comment
	int index = lastCommentIndex;
	int validCount = 0;
	while (index >= 0){
		int commentEnd = this.scanner.commentStops[index];
		if (commentEnd < 0) commentEnd = -commentEnd; // negative end position for non-javadoc comments
		if (commentEnd <= position){
			break;
		}
		index--;
		validCount++;
	}
	// if the source at <position> is immediately followed by a line comment, then
	// flush this comment and shift <position> to the comment end.
	if (validCount > 0){
		int immediateCommentEnd = -this.scanner.commentStops[index+1]; //non-javadoc comment end positions are negative
		if (immediateCommentEnd > 0){ // only tolerating non-javadoc comments
			// is there any line break until the end of the immediate comment ? (thus only tolerating line comment)
			immediateCommentEnd--; // comment end in one char too far
			if (Util.getLineNumber(position, this.scanner.lineEnds, 0, this.scanner.linePtr)
					== Util.getLineNumber(immediateCommentEnd, this.scanner.lineEnds, 0, this.scanner.linePtr)){
				position = immediateCommentEnd;
				validCount--; // flush this comment
				index++;
			}
		}
	}

	if (index < 0) return position; // no obsolete comment

	switch (validCount) {
		case 0:
			// do nothing
			break;
		// move valid comment infos, overriding obsolete comment infos
		case 2:
			this.scanner.commentStarts[0] = this.scanner.commentStarts[index+1];
			this.scanner.commentStops[0] = this.scanner.commentStops[index+1];
			this.scanner.commentTagStarts[0] = this.scanner.commentTagStarts[index+1];
			this.scanner.commentStarts[1] = this.scanner.commentStarts[index+2];
			this.scanner.commentStops[1] = this.scanner.commentStops[index+2];
			this.scanner.commentTagStarts[1] = this.scanner.commentTagStarts[index+2];
			break;
		case 1:
			this.scanner.commentStarts[0] = this.scanner.commentStarts[index+1];
			this.scanner.commentStops[0] = this.scanner.commentStops[index+1];
			this.scanner.commentTagStarts[0] = this.scanner.commentTagStarts[index+1];
			break;
		default:
			System.arraycopy(this.scanner.commentStarts, index + 1, this.scanner.commentStarts, 0, validCount);
			System.arraycopy(this.scanner.commentStops, index + 1, this.scanner.commentStops, 0, validCount);
			System.arraycopy(this.scanner.commentTagStarts, index + 1, this.scanner.commentTagStarts, 0, validCount);
	}
	this.scanner.commentPtr = validCount - 1;
	return position;
}
public int getFirstToken() {
	// the first token is a virtual token that
	// allows the parser to parse several goals
	// even if they aren't LALR(1)....
	// Goal ::= '++' JavaScriptUnit
	// Goal ::= '--' MethodBody
	// Goal ::= '==' ConstructorBody
	// -- Initializer
	// Goal ::= '>>' StaticInitializer
	// Goal ::= '>>' Block
	// -- error recovery
	// Goal ::= '>>>' Headers
	// Goal ::= '*' BlockStatements
	// Goal ::= '*' MethodPushModifiersHeader
	// -- JDOM
	// Goal ::= '&&' FieldDeclaration
	// Goal ::= '||' ImportDeclaration
	// Goal ::= '?' PackageDeclaration
	// Goal ::= '+' TypeDeclaration
	// Goal ::= '/' GenericMethodDeclaration
	// Goal ::= '&' ClassBodyDeclaration
	// -- code snippet
	// Goal ::= '%' Expression
	// -- completion parser
	// Goal ::= '!' ConstructorBlockStatementsopt
	// Goal ::= '~' BlockStatementsopt

	return this.firstToken;
}
/*
 * Answer back an array of sourceStart/sourceEnd positions of the available JavaDoc comments.
 * The array is a flattened structure: 2*n entries with consecutives start and end positions.
 *
 * If no JavaDoc is available, then null is answered instead of an empty array.
 *
 * e.g. { 10, 20, 25, 45 }  --> javadoc1 from 10 to 20, javadoc2 from 25 to 45
 */
public int[] getJavaDocPositions() {

	int javadocCount = 0;
	for (int i = 0, max = this.scanner.commentPtr; i <= max; i++){
		// javadoc only (non javadoc comment have negative end positions.)
		if (this.scanner.commentStops[i] > 0){
			javadocCount++;
		}
	}
	if (javadocCount == 0) return null;

	int[] positions = new int[2*javadocCount];
	int index = 0;
	for (int i = 0, max = this.scanner.commentPtr; i <= max; i++){
		// javadoc only (non javadoc comment have negative end positions.)
		if (this.scanner.commentStops[i] > 0){
			positions[index++] = this.scanner.commentStarts[i];
			positions[index++] = this.scanner.commentStops[i]-1; //stop is one over
		}
	}
	return positions;
}
	public void getMethodBodies(CompilationUnitDeclaration unit) {
		//fill the methods bodies in order for the code to be generated

		if (unit == null) return;

		if (unit.ignoreMethodBodies) {
			unit.ignoreFurtherInvestigation = true;
			return;
			// if initial diet parse did not work, no need to dig into method bodies.
		}

		if ((unit.bits & ASTNode.HasAllMethodBodies) != 0)
			return; //work already done ...

		// save existing values to restore them at the end of the parsing process
		// see bug 47079 for more details
		int[] oldLineEnds = this.scanner.lineEnds;
		int oldLinePtr = this.scanner.linePtr;

		//real parse of the method....
		CompilationResult compilationResult = unit.compilationResult;
		char[] contents = compilationResult.compilationUnit.getContents();
		this.scanner.setSource(contents, compilationResult);

		if (this.javadocParser != null && this.javadocParser.checkDocComment) {
			this.javadocParser.scanner.setSource(contents);
		}
		if (unit.types != null) {
			for (int i = unit.types.length; --i >= 0;)
				unit.types[i].parseMethod(this, unit);
		}

		// tag unit has having read bodies
		unit.bits |= ASTNode.HasAllMethodBodies;

		// this is done to prevent any side effects on the compilation unit result
		// line separator positions array.
		this.scanner.lineEnds = oldLineEnds;
		this.scanner.linePtr = oldLinePtr;
	}
protected char getNextCharacter(char[] comment, int[] index) {
	char nextCharacter = comment[index[0]++];
	switch(nextCharacter) {
		case '\\' :
			int c1, c2, c3, c4;
			index[0]++;
			while (comment[index[0]] == 'u') index[0]++;
			if (!(((c1 = ScannerHelper.getNumericValue(comment[index[0]++])) > 15
				|| c1 < 0)
				|| ((c2 = ScannerHelper.getNumericValue(comment[index[0]++])) > 15 || c2 < 0)
				|| ((c3 = ScannerHelper.getNumericValue(comment[index[0]++])) > 15 || c3 < 0)
				|| ((c4 = ScannerHelper.getNumericValue(comment[index[0]++])) > 15 || c4 < 0))) {
					nextCharacter = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
			}
			break;
	}
	return nextCharacter;
}
protected Expression getTypeReference(Expression exp) {

	exp.bits &= ~ASTNode.RestrictiveFlagMASK;
	exp.bits |= Binding.TYPE;
	return exp;
}
protected TypeReference getTypeReference(int dim) {
	/* build a Reference on a variable that may be qualified or not
	 This variable is a type reference and dim will be its dimensions*/

	TypeReference ref;
	int length = this.identifierLengthStack[this.identifierLengthPtr--];
	if (length < 0) { //flag for precompiled type reference on base types
		ref = TypeReference.baseTypeReference(-length, dim);
		ref.sourceStart = this.intStack[this.intPtr--];
		if (dim == 0) {
			ref.sourceEnd = this.intStack[this.intPtr--];
		} else {
			this.intPtr--;
			ref.sourceEnd = this.endPosition;
		}
	} else {
		int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr--];
		if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
			ref = null;
		} else if (length == 1) {
			// single variable reference
			this.genericsLengthPtr--; // pop the 0
			if (dim == 0) {
				ref =
					new SingleTypeReference(
						this.identifierStack[this.identifierPtr],
						this.identifierPositionStack[this.identifierPtr--]);
			} else {
				ref =
					new ArrayTypeReference(
						this.identifierStack[this.identifierPtr],
						dim,
						this.identifierPositionStack[this.identifierPtr--]);
				ref.sourceEnd = this.endPosition;
			}
		} else {
			this.genericsLengthPtr--;
			//Qualified variable reference
			char[][] tokens = new char[length][];
			this.identifierPtr -= length;
			long[] positions = new long[length];
			System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
			System.arraycopy(
				this.identifierPositionStack,
				this.identifierPtr + 1,
				positions,
				0,
				length);
			if (dim == 0) {
				ref = new QualifiedTypeReference(tokens, positions);
			} else {
				ref = new ArrayQualifiedTypeReference(tokens, dim, positions);
				ref.sourceEnd = this.endPosition;
			}
		}
	}
	return ref;
}
protected NameReference getUnspecifiedReference() {
	/* build a (unspecified) NameReference which may be qualified*/

	int length;
	NameReference ref;
	if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1)
		// single variable reference
		ref =
			new SingleNameReference(
				this.identifierStack[this.identifierPtr],
				this.identifierPositionStack[this.identifierPtr--]);
	else
		//Qualified variable reference
		{
		char[][] tokens = new char[length][];
		this.identifierPtr -= length;
		System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
		long[] positions = new long[length];
		System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
		ref =
			new QualifiedNameReference(tokens,
				positions,
				(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32), // sourceStart
				(int) this.identifierPositionStack[this.identifierPtr + length]); // sourceEnd
	}
	return ref;
}
protected NameReference getUnspecifiedReferenceOptimized() {
	/* build a (unspecified) NameReference which may be qualified
	The optimization occurs for qualified reference while we are
	certain in this case the last item of the qualified name is
	a field access. This optimization is IMPORTANT while it results
	that when a NameReference is build, the type checker should always
	look for that it is not a type reference */

	int length;
	NameReference ref;
	if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1) {
		// single variable reference
		ref =
			new SingleNameReference(
				this.identifierStack[this.identifierPtr],
				this.identifierPositionStack[this.identifierPtr--]);
		ref.bits &= ~ASTNode.RestrictiveFlagMASK;
		ref.bits |= Binding.LOCAL | Binding.FIELD;
		return ref;
	}

	//Qualified-variable-reference
	//In fact it is variable-reference DOT field-ref , but it would result in a type
	//conflict tha can be only reduce by making a superclass (or inetrface ) between
	//nameReference and FiledReference or putting FieldReference under NameReference
	//or else..........This optimisation is not really relevant so just leave as it is

	char[][] tokens = new char[length][];
	this.identifierPtr -= length;
	System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
	long[] positions = new long[length];
	System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
	ref = new QualifiedNameReference(
			tokens,
			positions,
			(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32), // sourceStart
			(int) this.identifierPositionStack[this.identifierPtr + length]); // sourceEnd
	ref.bits &= ~ASTNode.RestrictiveFlagMASK;
	ref.bits |= Binding.LOCAL | Binding.FIELD;
	return ref;
}
public void goForBlockStatementsopt() {
	//tells the scanner to go for block statements opt parsing

	this.firstToken = TokenNameTWIDDLE;
	this.scanner.recordLineSeparator = false;
}
public void goForProgramElements() {
	//tells the scanner to go for block statements opt parsing

	this.firstToken = TokenNamePLUS;
	this.scanner.recordLineSeparator = true;
}

public void goForBlockStatementsOrCatchHeader() {
	//tells the scanner to go for block statements or method headers parsing

	this.firstToken = TokenNameMULTIPLY;
	this.scanner.recordLineSeparator = false;
}
public void goForClassBodyDeclarations() {
	//tells the scanner to go for any body declarations parsing

//	this.firstToken = TokenNameAND;
	this.firstToken = TokenNamePLUS;
	this.scanner.recordLineSeparator = true;
}
public void goForCompilationUnit(){
	//tells the scanner to go for compilation unit parsing

	this.firstToken = TokenNamePLUS_PLUS ;
	this.scanner.foundTaskCount = 0;
	this.scanner.recordLineSeparator = true;
}
public void goForExpression() {
	//tells the scanner to go for an expression parsing

	this.firstToken = TokenNameREMAINDER;
	this.scanner.recordLineSeparator = true; // recovery goals must record line separators
}
public void goForFieldDeclaration(){
	//tells the scanner to go for field declaration parsing

	this.firstToken = TokenNameAND_AND ;
	this.scanner.recordLineSeparator = true;
}
public void goForHeaders(){
	//tells the scanner to go for headers only parsing
//	RecoveredType currentType = this.currentRecoveryType();
//	if(currentType != null && currentType.insideEnumConstantPart) {
//		this.firstToken = TokenNameNOT;
//	} else {
		this.firstToken = TokenNameUNSIGNED_RIGHT_SHIFT;
//	}
	this.scanner.recordLineSeparator = true; // recovery goals must record line separators
}
public void goForInitializer(){
	//tells the scanner to go for initializer parsing

	this.firstToken = TokenNameRIGHT_SHIFT ;
	this.scanner.recordLineSeparator = false;
}
public void goForMemberValue() {
	//tells the scanner to go for a member value parsing

	this.firstToken = TokenNameOR_OR;
	this.scanner.recordLineSeparator = true; // recovery goals must record line separators
}
public void goForMethodBody(){
	//tells the scanner to go for method body parsing

	this.firstToken = TokenNameMINUS_MINUS ;
	this.scanner.recordLineSeparator = false;
}
public void goForTypeDeclaration() {
	//tells the scanner to go for type (interface or class) declaration parsing

	this.firstToken = TokenNamePLUS;
	this.scanner.recordLineSeparator = true;
}
protected void ignoreExpressionAssignment() {
	// Assignment ::= InvalidArrayInitializerAssignement
	// encoded operator would be: this.intStack[this.intPtr]
	this.intPtr--;
	ArrayInitializer arrayInitializer = (ArrayInitializer) this.expressionStack[this.expressionPtr--];
	this.expressionLengthPtr -- ;
}
public void initialize() {
	this.initialize(false);
}
public void initialize(boolean initializeNLS) {
	//positionning the parser for a new compilation unit
	//avoiding stack reallocation and all that....
	this.astPtr = -1;
	this.astLengthPtr = -1;
	this.expressionPtr = -1;
	this.expressionLengthPtr = -1;
	this.identifierPtr = -1;
	this.identifierLengthPtr	= -1;
	this.intPtr = -1;
	this.nestedMethod[this.nestedType = 0] = 0; // need to reset for further reuse
	this.variablesCounter[this.nestedType] = 0;
	this.dimensions = 0 ;
	this.realBlockPtr = 0;
	this.compilationUnit = null;
	this.referenceContext = null;
	this.endStatementPosition = 0;

	//remove objects from stack too, while the same parser/compiler couple is
	//re-used between two compilations ....

	int astLength = this.astStack.length;
	if (this.noAstNodes.length < astLength){
		this.noAstNodes = new ASTNode[astLength];
		//System.out.println("Resized AST stacks : "+ astLength);

	}
	System.arraycopy(this.noAstNodes, 0, this.astStack, 0, astLength);

	int expressionLength = this.expressionStack.length;
	if (this.noExpressions.length < expressionLength){
		this.noExpressions = new Expression[expressionLength];
		//System.out.println("Resized EXPR stacks : "+ expressionLength);
	}
	System.arraycopy(this.noExpressions, 0, this.expressionStack, 0, expressionLength);

	// reset this.scanner state
	this.scanner.commentPtr = -1;
	this.scanner.foundTaskCount = 0;
	this.scanner.eofPosition = Integer.MAX_VALUE;
	this.recordStringLiterals = true;
	final boolean checkNLS = this.options.getSeverity(CompilerOptions.NonExternalizedString) != ProblemSeverities.Ignore;
	this.checkExternalizeStrings = checkNLS;
	this.scanner.checkNonExternalizedStringLiterals = initializeNLS && checkNLS;

	resetModifiers();

	// recovery
	this.lastCheckPoint = -1;
	this.currentElement = null;
	this.restartRecovery = false;
	this.hasReportedError = false;
	this.recoveredStaticInitializerStart = 0;
	this.lastIgnoredToken = -1;
	this.lastErrorEndPosition = -1;
	this.lastErrorEndPositionBeforeRecovery = -1;
	this.lastJavadocEnd = -1;
	this.listLength = 0;
	this.listTypeParameterLength = 0;

	this.rBraceStart = 0;
	this.rBraceEnd = 0;
	this.rBraceSuccessorStart = 0;

	this.genericsIdentifiersLengthPtr = -1;
	this.genericsLengthPtr = -1;
	this.genericsPtr = -1;

	this.errorAction= new HashSet(); 
	
}
public void initializeScanner(){
	this.scanner = new Scanner(
		false /*comment*/,
		false /*whitespace*/,
		false, /* will be set in initialize(boolean) */
		this.options.sourceLevel /*sourceLevel*/,
		this.options.complianceLevel /*complianceLevel*/,
		this.options.taskTags/*taskTags*/,
		this.options.taskPriorites/*taskPriorities*/,
		this.options.isTaskCaseSensitive/*taskCaseSensitive*/);
}
public void jumpOverMethodBody() {
	//on diet parsing.....do not buffer method statements

	//the scanner.diet is reinitialized to false
	//automatically by the scanner once it has jumped over
	//the statements

	if (this.diet && (this.dietInt == 0))
		this.scanner.diet = true;
}
private void jumpOverType(){
	if (this.recoveredTypes != null && this.nextTypeStart > -1 && this.nextTypeStart < this.scanner.currentPosition) {

		if (DEBUG_AUTOMATON) {
			System.out.println("Jump         -"); //$NON-NLS-1$
		}

		TypeDeclaration typeDeclaration = this.recoveredTypes[this.recoveredTypePtr];
		boolean isAnonymous = typeDeclaration.allocation != null;

		int end = this.scanner.eofPosition;
		this.scanner.resetTo(typeDeclaration.declarationSourceEnd + 1, end  - 1);
		if(!isAnonymous) {
			((RecoveryScanner)this.scanner).setPendingTokens(new int[]{TokenNameSEMICOLON, TokenNamebreak});
		} else {
			((RecoveryScanner)this.scanner).setPendingTokens(new int[]{TokenNameIdentifier, TokenNameEQUAL, TokenNameIdentifier});
		}

		this.pendingRecoveredType = typeDeclaration;

		try {
			this.currentToken = this.scanner.getNextToken();
		} catch(InvalidInputException e){
			// it's impossible because we added pending tokens before
		}

		if(++this.recoveredTypePtr < this.recoveredTypes.length) {
			TypeDeclaration nextTypeDeclaration = this.recoveredTypes[this.recoveredTypePtr];
			this.nextTypeStart =
				nextTypeDeclaration.allocation == null
					? nextTypeDeclaration.declarationSourceStart
							: nextTypeDeclaration.allocation.sourceStart;
		} else {
			this.nextTypeStart = Integer.MAX_VALUE;
		}
	}
}
protected void markEnclosingMemberWithLocalType() {
		if (this.currentElement != null) return; // this is already done in the recovery code
		for (int i = this.astPtr; i >= 0; i--) {
			ASTNode node = this.astStack[i];
			if (node instanceof AbstractMethodDeclaration
					|| node instanceof FieldDeclaration
					|| (node instanceof TypeDeclaration // mark type for now: all initializers will be marked when added to this type
							// and enclosing type must not be closed (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=147485)
							&& ((TypeDeclaration) node).declarationSourceEnd == 0)) {
				node.bits |= ASTNode.HasLocalType;
				return;
			}
		}
		// default to reference context (case of parse method body)
		if (this.referenceContext instanceof AbstractMethodDeclaration
				|| this.referenceContext instanceof TypeDeclaration) {
			((ASTNode)this.referenceContext).bits |= ASTNode.HasLocalType;
		}
}
protected void markInitializersWithLocalType(TypeDeclaration type) {
	if (type.fields == null || (type.bits & ASTNode.HasLocalType) == 0) return;
	for (int i = 0, length = type.fields.length; i < length; i++) {
		FieldDeclaration field = type.fields[i];
		if (field instanceof Initializer) {
			field.bits |= ASTNode.HasLocalType;
		}
	}
}

/*
 * Move checkpoint location (current implementation is moving it by one token)
 *
 * Answers true if successfully moved checkpoint (in other words, it did not attempt to move it
 * beyond end of file).
 */
protected boolean moveRecoveryCheckpoint() {

	int pos = this.lastCheckPoint;
	/* reset this.scanner, and move checkpoint by one token */
	this.scanner.startPosition = pos;
	this.scanner.currentPosition = pos;
	this.scanner.currentToken=this.scanner.currentNonWhitespaceToken=TokenNameUNKNOWN;
	this.scanner.diet = false; // quit jumping over method bodies

	/* if about to restart, then no need to shift token */
	if (this.restartRecovery){
		this.lastIgnoredToken = -1;
		this.scanner.insideRecovery = true;
		return true;
	}

	/* protect against shifting on an invalid token */
	this.lastIgnoredToken = this.nextIgnoredToken;
	this.nextIgnoredToken = -1;
	do {
		try {
			this.nextIgnoredToken = this.scanner.getNextToken();
			if(this.scanner.currentPosition == this.scanner.startPosition){
				this.scanner.currentPosition++; // on fake completion identifier
				this.nextIgnoredToken = -1;
			}

		} catch(InvalidInputException e){
			pos = this.scanner.currentPosition;
		}
	} while (this.nextIgnoredToken < 0);

	if (this.nextIgnoredToken == TokenNameEOF) { // no more recovery after this point
		if (this.currentToken == TokenNameEOF) { // already tried one iteration on EOF
			return false;
		}
	}
	this.lastCheckPoint = this.scanner.currentPosition;

	/* reset this.scanner again to previous checkpoint location*/
	this.scanner.startPosition = pos;
	this.scanner.currentPosition = pos;
	this.scanner.currentToken=TokenNameUNKNOWN;
	this.scanner.commentPtr = -1;
	this.scanner.foundTaskCount = 0;
	return true;

/*
 	The following implementation moves the checkpoint location by one line:

	int pos = this.lastCheckPoint;
	// reset this.scanner, and move checkpoint by one token
	this.scanner.startPosition = pos;
	this.scanner.currentPosition = pos;
	this.scanner.diet = false; // quit jumping over method bodies

	// if about to restart, then no need to shift token
	if (this.restartRecovery){
		this.lastIgnoredToken = -1;
		return true;
	}

	// protect against shifting on an invalid token
	this.lastIgnoredToken = this.nextIgnoredToken;
	this.nextIgnoredToken = -1;

	boolean wasTokenizingWhiteSpace = this.scanner.tokenizeWhiteSpace;
	this.scanner.tokenizeWhiteSpace = true;
	checkpointMove:
		do {
			try {
				this.nextIgnoredToken = this.scanner.getNextToken();
				switch(this.nextIgnoredToken){
					case Scanner.TokenNameWHITESPACE :
						if(this.scanner.getLineNumber(this.scanner.startPosition)
							== this.scanner.getLineNumber(this.scanner.currentPosition)){
							this.nextIgnoredToken = -1;
							}
						break;
					case TokenNameSEMICOLON :
					case TokenNameLBRACE :
					case TokenNameRBRACE :
						break;
					case TokenNameIdentifier :
						if(this.scanner.currentPosition == this.scanner.startPosition){
							this.scanner.currentPosition++; // on fake completion identifier
						}
					default:
						this.nextIgnoredToken = -1;
						break;
					case TokenNameEOF :
						break checkpointMove;
				}
			} catch(InvalidInputException e){
				pos = this.scanner.currentPosition;
			}
		} while (this.nextIgnoredToken < 0);
	this.scanner.tokenizeWhiteSpace = wasTokenizingWhiteSpace;

	if (this.nextIgnoredToken == TokenNameEOF) { // no more recovery after this point
		if (this.currentToken == TokenNameEOF) { // already tried one iteration on EOF
			return false;
		}
	}
	this.lastCheckPoint = this.scanner.currentPosition;

	// reset this.scanner again to previous checkpoint location
	this.scanner.startPosition = pos;
	this.scanner.currentPosition = pos;
	this.scanner.commentPtr = -1;

	return true;
*/
}
protected MessageSend newMessageSend() {
	// '(' ArgumentListopt ')'
	// the arguments are on the expression stack

	MessageSend m = new MessageSend();
	int length;
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
		this.expressionPtr -= length;
		System.arraycopy(
			this.expressionStack,
			this.expressionPtr + 1,
			m.arguments = new Expression[length],
			0,
			length);
	}
	return m;
}
protected void optimizedConcatNodeLists() {
	/*back from a recursive loop. Virtualy group the
	astNode into an array using this.astLengthStack*/

	/*
	 * This is a case where you have two sublists into the this.astStack that you want
	 * to merge in one list. There is no action required on the this.astStack. The only
	 * thing you need to do is merge the two lengths specified on the astStackLength.
	 * The top two length are for example:
	 * ... p   n
	 * and you want to result in a list like:
	 * ... n+p
	 * This means that the p could be equals to 0 in case there is no astNode pushed
	 * on the this.astStack.
	 * Look at the InterfaceMemberDeclarations for an example.
	 * This case optimizes the fact that p == 1.
	 */

	this.astLengthStack[--this.astLengthPtr]++;
}

protected boolean isErrorState(int act) {
 	int stackTop=this.stateStackTop;
 	int [] tempStack=new int[this.stack.length+2];
 	System.arraycopy(this.stack, 0, tempStack, 0, this.stack.length);
	boolean first=true;
	int currentAction = act;
	ProcessTerminals : for (;;) {
		int stackLength = tempStack.length;
		if (!first)
		{
		  if (++stackTop >= stackLength) {
			System.arraycopy(
					tempStack, 0,
					tempStack = new int[stackLength + StackIncrement], 0,
				stackLength);
 		  }
		  tempStack[stackTop] = currentAction;
		}
	  first=false;
	  currentAction = tAction(currentAction, this.currentToken);
		if (currentAction == ERROR_ACTION) {
			return true;
		}
		if (currentAction <= NUM_RULES) {
			stackTop--;

		} else if (currentAction > ERROR_ACTION) { /* shift-reduce */
			if (DEBUG) System.out.println("<<shift-reduce consume Token: "+scanner.toStringAction(this.currentToken)); //$NON-NLS-1$
			return false;
//			consumeToken(this.currentToken);
//			if (this.currentElement != null) this.recoveryTokenCheck();
//			try {
//				prevPos = scanner.currentPosition;
//				prevToken = currentToken;
//				insertedSemicolon = false;
//				this.currentToken = this.scanner.getNextToken();
//				if (DEBUG) System.out.println(">>shift-reduce Next Token: "+scanner.dumpCurrent());
//			} catch(InvalidInputException e){
//				if (!this.hasReportedError){
//					this.problemReporter().scannerError(this, e.getMessage());
//					this.hasReportedError = true;
//				}
//				this.lastCheckPoint = this.scanner.currentPosition;
//				this.restartRecovery = true;
//			}
//			if(this.statementRecoveryActivated) {
//				jumpOverTypeAfterReduce = true;
//			}
//			act -= ERROR_ACTION;

		} else {
		    if (currentAction < ACCEPT_ACTION) { /* shift */
				if (DEBUG) System.out.println("<<shift consume Token: "+scanner.toStringAction(this.currentToken)); //$NON-NLS-1$
				return false;
//				consumeToken(this.currentToken);
//				if (this.currentElement != null) this.recoveryTokenCheck();
//				try{
//					prevPos = scanner.currentPosition;
//					prevToken = currentToken;
//					insertedSemicolon = false;
//					this.currentToken = this.scanner.getNextToken();
//					if (DEBUG) System.out.println(">>shift next Token: "+scanner.dumpCurrent());
//				} catch(InvalidInputException e){
//					if (!this.hasReportedError){
//						this.problemReporter().scannerError(this, e.getMessage());
//						this.hasReportedError = true;
//					}
//					this.lastCheckPoint = this.scanner.currentPosition;
//					this.restartRecovery = true;
//				}
//				if(this.statementRecoveryActivated) {
//					this.jumpOverType();
//				}
//				continue ProcessTerminals;
			}
			break ProcessTerminals;
		}

		// ProcessNonTerminals :
		do { /* reduce */
			stackTop -= (rhs[currentAction] - 1);
			currentAction = ntAction(tempStack[stackTop], lhs[currentAction]);
		} while (currentAction <= NUM_RULES);
	}
return false;
}


/*main loop of the automat
When a rule is reduced, the method consumeRule(int) is called with the number
of the consumed rule. When a terminal is consumed, the method consumeToken(int) is
called in order to remember (when needed) the consumed token */
// (int)asr[asi(act)]
// name[symbol_index[currentKind]]
protected void parse() {
	if (DEBUG) System.out.println("-- ENTER INSIDE PARSE METHOD --");  //$NON-NLS-1$

	if (DEBUG_AUTOMATON) {
		System.out.println("- Start --------------------------------");  //$NON-NLS-1$
	}

	boolean isDietParse = this.diet;
	int oldFirstToken = getFirstToken();
	this.hasError = false;

	this.hasReportedError = false;
	boolean insertedSemicolon = false;
	int prevAct = START_STATE,
		prevToken = getFirstToken(),
		prevPos = scanner.startPosition;

	int act = START_STATE;
	this.stateStackTop = -1;
	this.currentToken = getFirstToken();
	ProcessTerminals : for (;;) {
		int stackLength = this.stack.length;
		if (++this.stateStackTop >= stackLength) {
			System.arraycopy(
				this.stack, 0,
				this.stack = new int[stackLength + StackIncrement], 0,
				stackLength);
		}
		this.stack[this.stateStackTop] = act;

		if (DEBUG)
			System.out.println("action="+act+ ((term_check[base_action[act]+this.currentToken] == this.currentToken)?"":" - take default") ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		prevAct=act;

		if (optionalSemicolonState[act])
		{
			if (term_check[base_action[act]+this.currentToken] != this.currentToken)
			{
				if (isErrorState(act))
				{
				  if (!insertedSemicolon && shouldInsertSemicolon(prevPos, prevToken) )
				  {
					currentToken = TokenNameSEMICOLON;
					this.insertedSemicolonPosition=prevPos;
					scanner.pushBack();
					insertedSemicolon = true;

				  }
				}
			}
		}

		act = tAction(act, this.currentToken);
		if (act == ERROR_ACTION || this.restartRecovery) {
//			if ( act == ERROR_ACTION &&
//					!insertedSemicolon &&
//					shouldInsertSemicolon(prevPos, prevToken)
//					) {
//					act = prevAct;
//					--stateStackTop;
//					currentToken = TokenNameSEMICOLON;
//					scanner.pushBack();
//					insertedSemicolon = true;
//					continue ProcessTerminals;
//				}

			if (DEBUG_AUTOMATON) {
				if (this.restartRecovery) {
					System.out.println("Restart      - "); //$NON-NLS-1$
				} else {
					System.out.println("Error        - "); //$NON-NLS-1$
				}
			}

			int errorPos = this.scanner.currentPosition;
			if (!this.hasReportedError) {
				this.hasError = true;
			}
 			if (resumeOnSyntaxError()) {
				if (act == ERROR_ACTION) this.lastErrorEndPosition = errorPos;
				act = START_STATE;
				this.stateStackTop = -1;
				this.currentToken = getFirstToken();
				if (DEBUG) System.out.println("!! Resume on syntax error"); //$NON-NLS-1$
				continue ProcessTerminals;
			}
			act = ERROR_ACTION;
			break ProcessTerminals;
		}
		if (act <= NUM_RULES) {
			insertedSemicolon = false;
			this.stateStackTop--;

			if (DEBUG_AUTOMATON) {
				System.out.print("Reduce       - "); //$NON-NLS-1$
			}

		} else if (act > ERROR_ACTION) { /* shift-reduce */
			if (DEBUG) System.out.println("<<shift-reduce consume Token: "+scanner.toStringAction(this.currentToken)); //$NON-NLS-1$
			consumeToken(this.currentToken);
			if (this.currentElement != null) this.recoveryTokenCheck();
			try {
				prevPos = scanner.currentPosition;
				prevToken = currentToken;
				insertedSemicolon = false;
				this.currentToken = this.scanner.getNextToken();
				if (DEBUG) System.out.println(">>shift-reduce Next Token: "+scanner.dumpCurrent()); //$NON-NLS-1$
			} catch(InvalidInputException e){
				if (!this.hasReportedError){
					this.problemReporter().scannerError(this, e.getMessage());
					this.hasReportedError = true;
				}
				this.lastCheckPoint = this.scanner.currentPosition;
				this.restartRecovery = true;
			}
			if(this.statementRecoveryActivated) {
				this.jumpOverType();
			}
			act -= ERROR_ACTION;

			if (DEBUG_AUTOMATON) {
				System.out.print("Shift/Reduce - (" + name[terminal_index[this.currentToken]]+") ");  //$NON-NLS-1$  //$NON-NLS-2$
			}

		} else {
		    if (act < ACCEPT_ACTION) { /* shift */
				if (DEBUG) System.out.println("<<shift consume Token: "+scanner.toStringAction(this.currentToken)); //$NON-NLS-1$
				consumeToken(this.currentToken);
				if (this.currentElement != null) this.recoveryTokenCheck();
				try{
					prevPos = scanner.currentPosition;
					prevToken = currentToken;
					insertedSemicolon = false;
					this.currentToken = this.scanner.getNextToken();
					if (DEBUG) System.out.println(">>shift next Token: "+scanner.dumpCurrent()); //$NON-NLS-1$
				} catch(InvalidInputException e){
					if (!this.hasReportedError){
						this.problemReporter().scannerError(this, e.getMessage());
						this.hasReportedError = true;
					}
					this.lastCheckPoint = this.scanner.currentPosition;
					this.restartRecovery = true;
				}
				if(this.statementRecoveryActivated) {
					this.jumpOverType();
				}
				if (DEBUG_AUTOMATON) {
					System.out.println("Shift        - (" + name[terminal_index[this.currentToken]]+")");  //$NON-NLS-1$  //$NON-NLS-2$
				}
				continue ProcessTerminals;
			}
			break ProcessTerminals;
		}

		// ProcessNonTerminals :
		do { /* reduce */

			if (DEBUG_AUTOMATON) {
				System.out.println(name[non_terminal_index[lhs[act]]]);
			}

			consumeRule(act);
			this.stateStackTop -= (rhs[act] - 1);
			act = ntAction(this.stack[this.stateStackTop], lhs[act]);
			if (DEBUG_AUTOMATON && act <= NUM_RULES) {
				System.out.print("             - ");  //$NON-NLS-1$
				}

		} while (act <= NUM_RULES);
		if (DEBUG_AUTOMATON) {
			System.out.println("----------------------------------------");  //$NON-NLS-1$
		}
	}
	if (DEBUG_AUTOMATON) {
		System.out.println("- End ----------------------------------");  //$NON-NLS-1$
	}

	endParse(act);
	// record all nls tags in the corresponding compilation unit
	final NLSTag[] tags = this.scanner.getNLSTags();
	if (tags != null) {
		this.compilationUnit.nlsTags = tags;
	}

	this.scanner.checkNonExternalizedStringLiterals = false;
	if (this.reportSyntaxErrorIsRequired && this.hasError && !(this.statementRecoveryActivated && DO_DIET_PARSE)) {
		if(!this.options.performStatementsRecovery) {
			reportSyntaxErrors(isDietParse, oldFirstToken);
		} else {
			RecoveryScannerData data = this.referenceContext.compilationResult().recoveryScannerData;

			if(this.recoveryScanner == null) {
				this.recoveryScanner = new RecoveryScanner(this.scanner, data);
			} else {
				this.recoveryScanner.setData(data);
			}

			this.recoveryScanner.setSource(scanner.source);
			this.recoveryScanner.lineEnds = this.scanner.lineEnds;
			this.recoveryScanner.linePtr = this.scanner.linePtr;

			reportSyntaxErrors(isDietParse, oldFirstToken);

			if(data == null) {
				this.referenceContext.compilationResult().recoveryScannerData =
					this.recoveryScanner.getData();
			}

			if (this.methodRecoveryActivated && this.options.performStatementsRecovery && !this.enteredRecoverStatements) {
				this.methodRecoveryActivated = false;
				this.enteredRecoverStatements=true;
				this.recoverStatements();
				this.methodRecoveryActivated = true;

				this.lastAct = ERROR_ACTION;
			}
		}
	}

	if (DEBUG) System.out.println("-- EXIT FROM PARSE METHOD --");  //$NON-NLS-1$
}
public void parse(ConstructorDeclaration cd, CompilationUnitDeclaration unit) {
	parse(cd, unit, false);
}
public void parse(ConstructorDeclaration cd, CompilationUnitDeclaration unit, boolean recordLineSeparator) {
	//only parse the method body of cd
	//fill out its statements

	//convert bugs into parse error

	boolean oldMethodRecoveryActivated = this.methodRecoveryActivated;
	if(this.options.performMethodsFullRecovery) {
		this.methodRecoveryActivated = true;
	}

	initialize();
	goForBlockStatementsopt();
	if (recordLineSeparator) {
		this.scanner.recordLineSeparator = true;
	}
	this.nestedMethod[this.nestedType]++;
	pushOnRealBlockStack(0);

	this.referenceContext = cd;
	this.compilationUnit = unit;

	this.scanner.resetTo(cd.bodyStart, cd.bodyEnd);
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
		if(this.options.performStatementsRecovery) {
			this.methodRecoveryActivated = oldMethodRecoveryActivated;
		}
	}

	checkNonNLSAfterBodyEnd(cd.declarationSourceEnd);

	if (this.lastAct == ERROR_ACTION) {
		initialize();
		return;
	}

	//statements
	cd.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	int length;
	if (astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		this.astPtr -= length;
		if (this.astStack[this.astPtr + 1] instanceof ExplicitConstructorCall)
			//avoid a isSomeThing that would only be used here BUT what is faster between two alternatives ?
			{
			System.arraycopy(
				this.astStack,
				this.astPtr + 2,
				cd.statements = new Statement[length - 1],
				0,
				length - 1);
			cd.constructorCall = (ExplicitConstructorCall) this.astStack[this.astPtr + 1];
		} else { //need to add explicitly the super();
			System.arraycopy(
				this.astStack,
				this.astPtr + 1,
				cd.statements = new Statement[length],
				0,
				length);
			cd.constructorCall = SuperReference.implicitSuperConstructorCall();
		}
	} else {
		cd.constructorCall = SuperReference.implicitSuperConstructorCall();
		if (!containsComment(cd.bodyStart, cd.bodyEnd)) {
			cd.bits |= ASTNode.UndocumentedEmptyBlock;
		}
	}

	if (cd.constructorCall.sourceEnd == 0) {
		cd.constructorCall.sourceEnd = cd.sourceEnd;
		cd.constructorCall.sourceStart = cd.sourceStart;
	}
}
// A P I

public void parse(
	FieldDeclaration field,
	TypeDeclaration type,
	CompilationUnitDeclaration unit,
	char[] initializationSource) {
	//only parse the initializationSource of the given field

	//convert bugs into parse error

	initialize();
	goForExpression();
	this.nestedMethod[this.nestedType]++;

	this.referenceContext = type;
	this.compilationUnit = unit;

	this.scanner.setSource(initializationSource);
	this.scanner.resetTo(0, initializationSource.length-1);
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
	}

	if (this.lastAct == ERROR_ACTION) {
		return;
	}

	field.initialization = this.expressionStack[this.expressionPtr];

	// mark field with local type if one was found during parsing
	if ((type.bits & ASTNode.HasLocalType) != 0) {
		field.bits |= ASTNode.HasLocalType;
	}
}
// A P I

public CompilationUnitDeclaration parse(
	ICompilationUnit sourceUnit,
	CompilationResult compilationResult) {
	// parses a compilation unit and manages error handling (even bugs....)

	return parse(sourceUnit, compilationResult, -1, -1/*parse without reseting the scanner*/);
}
// A P I

public CompilationUnitDeclaration parse(
	ICompilationUnit sourceUnit,
	CompilationResult compilationResult,
	int start,
	int end) {
	// parses a compilation unit and manages error handling (even bugs....)

	CompilationUnitDeclaration unit;
	try {
		/* automaton initialization */
		initialize(true);
		goForCompilationUnit();

		/* unit creation */
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					this.problemReporter,
					compilationResult,
					0);

		initializeInferenceEngine(this.compilationUnit);

		/* scanners initialization */
		char[] contents;
		try {
			contents = sourceUnit.getContents();
		} catch(AbortCompilationUnit abortException) {
			this.problemReporter().cannotReadSource(this.compilationUnit, abortException, this.options.verbose);
			contents = CharOperation.NO_CHAR; // pretend empty from thereon
		}
		this.scanner.setSource(contents);
		this.compilationUnit.sourceEnd = this.scanner.source.length - 1;
		if (end != -1) this.scanner.resetTo(start, end);
		if (this.javadocParser != null && this.javadocParser.checkDocComment) {
			this.javadocParser.scanner.setSource(contents);
			if (end != -1) {
				this.javadocParser.scanner.resetTo(start, end);
			}
		}
		/* run automaton */
		if (false)
			System.out.println("parsing "+new String(sourceUnit.getFileName())); //$NON-NLS-1$
		if (options.complianceLevel > ClassFileConstants.JDK0_0) {
			parse();
		}
	} finally {
		unit = this.compilationUnit;
		this.compilationUnit = null; // reset parser
		// tag unit has having read bodies
		if (!this.diet) unit.bits |= ASTNode.HasAllMethodBodies;
	}
	return unit;
}

public void initializeInferenceEngine(CompilationUnitDeclaration compilationUnitDeclaration) {
	if (this.inferenceEngines==null)
		this.inferenceEngines =  InferrenceManager.getInstance().getInferenceEngines(compilationUnitDeclaration);
	for (int i = 0; i <  this.inferenceEngines.length; i++) {
		this.inferenceEngines[i].initializeOptions(this.options.inferOptions);
	}
}

public void parse(
	Initializer initializer,
	TypeDeclaration type,
	CompilationUnitDeclaration unit) {
	//only parse the method body of md
	//fill out method statements

	//convert bugs into parse error

	boolean oldMethodRecoveryActivated = this.methodRecoveryActivated;
	if(this.options.performMethodsFullRecovery) {
		this.methodRecoveryActivated = true;
	}

	initialize();
	goForBlockStatementsopt();
	this.nestedMethod[this.nestedType]++;
	pushOnRealBlockStack(0);

	this.referenceContext = type;
	this.compilationUnit = unit;

	this.scanner.resetTo(initializer.bodyStart, initializer.bodyEnd); // just on the beginning {
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
		if(this.options.performStatementsRecovery) {
			this.methodRecoveryActivated = oldMethodRecoveryActivated;
		}
	}

	checkNonNLSAfterBodyEnd(initializer.declarationSourceEnd);

	if (this.lastAct == ERROR_ACTION) {
		return;
	}

	//refill statements
	initializer.block.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	int length;
	if (astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) > 0) {
		System.arraycopy(this.astStack, (this.astPtr -= length) + 1, initializer.block.statements = new Statement[length], 0, length);
	} else {
		// check whether this block at least contains some comment in it
		if (!containsComment(initializer.block.sourceStart, initializer.block.sourceEnd)) {
			initializer.block.bits |= ASTNode.UndocumentedEmptyBlock;
		}
	}

	// mark initializer with local type if one was found during parsing
	if ((type.bits & ASTNode.HasLocalType) != 0) {
		initializer.bits |= ASTNode.HasLocalType;
	}
}
// A P I
public void parse(MethodDeclaration md, CompilationUnitDeclaration unit) {
	//only parse the method body of md
	//fill out method statements

	//convert bugs into parse error

	if (md.isAbstract())
		return;
	if ((md.modifiers & ExtraCompilerModifiers.AccSemicolonBody) != 0)
		return;

	boolean oldMethodRecoveryActivated = this.methodRecoveryActivated;
	if(this.options.performMethodsFullRecovery) {
		this.methodRecoveryActivated = true;
		this.rParenPos = md.sourceEnd;
	}
	initialize();
	goForBlockStatementsopt();
	this.nestedMethod[this.nestedType]++;
	pushOnRealBlockStack(0);

	this.referenceContext = md;
	this.compilationUnit = unit;

	this.scanner.resetTo(md.bodyStart, md.bodyEnd);
	// reset the scanner to parser from { down to }
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
		if(this.options.performStatementsRecovery) {
			this.methodRecoveryActivated = oldMethodRecoveryActivated;
		}
	}

	checkNonNLSAfterBodyEnd(md.declarationSourceEnd);

	if (this.lastAct == ERROR_ACTION) {
		return;
	}

	//refill statements
	md.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	int length;
	if (astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		System.arraycopy(
			this.astStack,
			(this.astPtr -= length) + 1,
			md.statements = new Statement[length],
			0,
			length);
	} else {
		if (!containsComment(md.bodyStart, md.bodyEnd)) {
			md.bits |= ASTNode.UndocumentedEmptyBlock;
		}
	}
}
public ASTNode[] parseClassBodyDeclarations(char[] source, int offset, int length, CompilationUnitDeclaration unit) {
	/* automaton initialization */
	initialize();
	goForClassBodyDeclarations();
	/* scanner initialization */
	this.scanner.setSource(source);
	this.scanner.resetTo(offset, offset + length - 1);
	if (this.javadocParser != null && this.javadocParser.checkDocComment) {
		this.javadocParser.scanner.setSource(source);
		this.javadocParser.scanner.resetTo(offset, offset + length - 1);
	}

	/* type declaration should be parsed as member type declaration */
	this.nestedType = 1;

	/* unit creation */
	this.referenceContext = unit;
	this.compilationUnit = unit;

	/* run automaton */
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	}

	if (this.lastAct == ERROR_ACTION || this.hasError) {
		return null;
	}
	int astLength;
	if (astLengthPtr > -1 && (astLength = this.astLengthStack[this.astLengthPtr--]) != 0) {
		ASTNode[] result = new ASTNode[astLength];
		this.astPtr -= astLength;
		System.arraycopy(this.astStack, this.astPtr + 1, result, 0, astLength);
		return result;
	}
	return null;
}
public Expression parseExpression(char[] source, int offset, int length, CompilationUnitDeclaration unit) {

	initialize();
	goForExpression();
	this.nestedMethod[this.nestedType]++;

	this.referenceContext = unit;
	this.compilationUnit = unit;

	this.scanner.setSource(source);
	this.scanner.resetTo(offset, offset + length - 1);
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
	}

	if (this.lastAct == ERROR_ACTION) {
		return null;
	}

	return this.expressionStack[this.expressionPtr];
}
public Expression parseMemberValue(char[] source, int offset, int length, CompilationUnitDeclaration unit) {

	initialize();
	goForMemberValue();
	this.nestedMethod[this.nestedType]++;

	this.referenceContext = unit;
	this.compilationUnit = unit;

	this.scanner.setSource(source);
	this.scanner.resetTo(offset, offset + length - 1);
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
	}

	if (this.lastAct == ERROR_ACTION) {
		return null;
	}

	return this.expressionStack[this.expressionPtr];
}
public void parseStatements(ReferenceContext rc, int start, int end, TypeDeclaration[] types, CompilationUnitDeclaration unit) {
	boolean oldStatementRecoveryEnabled = this.statementRecoveryActivated;
	this.statementRecoveryActivated = true;

	initialize();

	if (rc instanceof CompilationUnitDeclaration)
		goForCompilationUnit();
	else
		goForBlockStatementsopt();
	this.nestedMethod[this.nestedType]++;
	pushOnRealBlockStack(0);

	//pushOnAstLengthStack(0);

	this.referenceContext = rc;
	this.compilationUnit = unit;

	this.pendingRecoveredType = null;

	if(types != null && types.length > 0) {
		this.recoveredTypes = types;
		this.recoveredTypePtr = 0;
		this.nextTypeStart =
			this.recoveredTypes[0].allocation == null
				? this.recoveredTypes[0].declarationSourceStart
						: this.recoveredTypes[0].allocation.sourceStart;
	} else {
		this.recoveredTypes = null;
		this.recoveredTypePtr = -1;
		this.nextTypeStart = -1;
	}

	this.scanner.resetTo(start, end);
	// reset the scanner to parser from { down to }

	this.lastCheckPoint = this.scanner.initialPosition;


	this.stateStackTop = -1;

	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
		this.recoveredTypes = null;
		this.statementRecoveryActivated = oldStatementRecoveryEnabled;
	}

	checkNonNLSAfterBodyEnd(end);
}
public void persistLineSeparatorPositions() {
	if (this.scanner.recordLineSeparator) {
		this.compilationUnit.compilationResult.lineSeparatorPositions = this.scanner.getLineEnds();
	}
}
/*
 * Prepares the state of the parser to go for BlockStatements.
 */
protected void prepareForBlockStatements() {
	this.nestedMethod[this.nestedType = 0] = 1;
	this.variablesCounter[this.nestedType] = 0;
	this.realBlockStack[this.realBlockPtr = 1] = 0;
}
/**
 * Returns this parser's problem reporter initialized with its reference context.
 * Also it is assumed that a problem is going to be reported, so initializes
 * the compilation result's line positions.
 *
 * @return ProblemReporter
 */
public ProblemReporter problemReporter(){
	if (this.scanner.recordLineSeparator) {
		if (this.compilationUnit!=null)
			this.compilationUnit.compilationResult.lineSeparatorPositions = this.scanner.getLineEnds();
	}
	this.problemReporter.referenceContext = this.referenceContext;
	return this.problemReporter;
}
protected void pushIdentifier() {
	/*push the consumeToken on the identifier stack.
	Increase the total number of identifier in the stack.
	identifierPtr points on the next top */

	int stackLength = this.identifierStack.length;
	if (++this.identifierPtr >= stackLength) {
		System.arraycopy(
			this.identifierStack, 0,
			this.identifierStack = new char[stackLength + 20][], 0,
			stackLength);
		System.arraycopy(
			this.identifierPositionStack, 0,
			this.identifierPositionStack = new long[stackLength + 20], 0,
			stackLength);
	}
	this.identifierStack[this.identifierPtr] = this.scanner.getCurrentIdentifierSource();
	this.identifierPositionStack[this.identifierPtr] =
		(((long) this.scanner.startPosition) << 32) + (this.scanner.currentPosition - 1);

	stackLength = this.identifierLengthStack.length;
	if (++this.identifierLengthPtr >= stackLength) {
		System.arraycopy(
			this.identifierLengthStack, 0,
			this.identifierLengthStack = new int[stackLength + 10], 0,
			stackLength);
	}
	this.identifierLengthStack[this.identifierLengthPtr] = 1;
}
protected void pushIdentifier(int flag) {
	/*push a special flag on the stack :
	-zero stands for optional Name
	-negative number for direct ref to base types.
	identifierLengthPtr points on the top */

	int stackLength = this.identifierLengthStack.length;
	if (++this.identifierLengthPtr >= stackLength) {
		System.arraycopy(
			this.identifierLengthStack, 0,
			this.identifierLengthStack = new int[stackLength + 10], 0,
			stackLength);
	}
	this.identifierLengthStack[this.identifierLengthPtr] = flag;
}
protected void pushOnAstLengthStack(int pos) {

	int stackLength = this.astLengthStack.length;
	if (++this.astLengthPtr >= stackLength) {
		System.arraycopy(
			this.astLengthStack, 0,
			this.astLengthStack = new int[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.astLengthStack[this.astLengthPtr] = pos;
}
protected void pushOnAstStack(ASTNode node) {
	/*add a new obj on top of the ast stack
	astPtr points on the top*/

	int stackLength = this.astStack.length;
	if (++this.astPtr >= stackLength) {
		System.arraycopy(
			this.astStack, 0,
			this.astStack = new ASTNode[stackLength + AstStackIncrement], 0,
			stackLength);
		this.astPtr = stackLength;
	}
	this.astStack[this.astPtr] = node;

	stackLength = this.astLengthStack.length;
	if (++this.astLengthPtr >= stackLength) {
		System.arraycopy(
			this.astLengthStack, 0,
			this.astLengthStack = new int[stackLength + AstStackIncrement], 0,
			stackLength);
	}
	this.astLengthStack[this.astLengthPtr] = 1;
}
protected void pushOnExpressionStack(Expression expr) {

	int stackLength = this.expressionStack.length;
	if (++this.expressionPtr >= stackLength) {
		System.arraycopy(
			this.expressionStack, 0,
			this.expressionStack = new Expression[stackLength + ExpressionStackIncrement], 0,
			stackLength);
	}
	this.expressionStack[this.expressionPtr] = expr;

	stackLength = this.expressionLengthStack.length;
	if (++this.expressionLengthPtr >= stackLength) {
		System.arraycopy(
			this.expressionLengthStack, 0,
			this.expressionLengthStack = new int[stackLength + ExpressionStackIncrement], 0,
			stackLength);
	}
	this.expressionLengthStack[this.expressionLengthPtr] = 1;
}
protected void pushOnExpressionStackLengthStack(int pos) {

	int stackLength = this.expressionLengthStack.length;
	if (++this.expressionLengthPtr >= stackLength) {
		System.arraycopy(
			this.expressionLengthStack, 0,
			this.expressionLengthStack = new int[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.expressionLengthStack[this.expressionLengthPtr] = pos;
}
protected void pushOnGenericsStack(ASTNode node) {
	/*add a new obj on top of the generics stack
	genericsPtr points on the top*/

	int stackLength = this.genericsStack.length;
	if (++this.genericsPtr >= stackLength) {
		System.arraycopy(
			this.genericsStack, 0,
			this.genericsStack = new ASTNode[stackLength + GenericsStackIncrement], 0,
			stackLength);
	}
	this.genericsStack[this.genericsPtr] = node;

	stackLength = this.genericsLengthStack.length;
	if (++this.genericsLengthPtr >= stackLength) {
		System.arraycopy(
			this.genericsLengthStack, 0,
			this.genericsLengthStack = new int[stackLength + GenericsStackIncrement], 0,
			stackLength);
	}
	this.genericsLengthStack[this.genericsLengthPtr] = 1;
}
protected void pushOnGenericsIdentifiersLengthStack(int pos) {
	int stackLength = this.genericsIdentifiersLengthStack.length;
	if (++this.genericsIdentifiersLengthPtr >= stackLength) {
		System.arraycopy(
			this.genericsIdentifiersLengthStack, 0,
			this.genericsIdentifiersLengthStack = new int[stackLength + GenericsStackIncrement], 0,
			stackLength);
	}
	this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr] = pos;
}
protected void pushOnGenericsLengthStack(int pos) {
	int stackLength = this.genericsLengthStack.length;
	if (++this.genericsLengthPtr >= stackLength) {
		System.arraycopy(
			this.genericsLengthStack, 0,
			this.genericsLengthStack = new int[stackLength + GenericsStackIncrement], 0,
			stackLength);
	}
	this.genericsLengthStack[this.genericsLengthPtr] = pos;
}
protected void pushOnIntStack(int pos) {

	int stackLength = this.intStack.length;
	if (++this.intPtr >= stackLength) {
		System.arraycopy(
			this.intStack, 0,
			this.intStack = new int[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.intStack[this.intPtr] = pos;
}
protected void pushOnRealBlockStack(int i){

	int stackLength = this.realBlockStack.length;
	if (++this.realBlockPtr >= stackLength) {
		System.arraycopy(
			this.realBlockStack, 0,
			this.realBlockStack = new int[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.realBlockStack[this.realBlockPtr] = i;
}
protected void recoverStatements() {
	class MethodVisitor extends ASTVisitor {
		public ASTVisitor typeVisitor;

		TypeDeclaration enclosingType; // used only for initializer

		TypeDeclaration[] types = new TypeDeclaration[0];
		int typePtr = -1;
		public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
			typePtr = -1;
			return true;
		}
		public boolean visit(Initializer initializer, MethodScope scope) {
			typePtr = -1;
			return true;
		}
		public boolean visit(MethodDeclaration methodDeclaration,Scope scope) {
			typePtr = -1;
			return true;
		}
		public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
			return this.visit(typeDeclaration);
		}
		public boolean visit(TypeDeclaration typeDeclaration, Scope scope) {
			return this.visit(typeDeclaration);
		}
		private boolean visit(TypeDeclaration typeDeclaration) {
			if(this.types.length <= ++this.typePtr) {
				int length = this.typePtr;
				System.arraycopy(this.types, 0, this.types = new TypeDeclaration[length * 2 + 1], 0, length);
			}
			this.types[this.typePtr] = typeDeclaration;
			return false;
		}
		public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
			this.endVisitMethod(constructorDeclaration, scope);
		}
		public void endVisit(MethodDeclaration methodDeclaration, Scope scope) {
			this.endVisitMethod(methodDeclaration, scope);
		}
		private void endVisitMethod(AbstractMethodDeclaration methodDeclaration, Scope scope) {
			TypeDeclaration[] foundTypes = null;
//			int length = 0;
//			if(this.typePtr > -1) {
//				length = this.typePtr + 1;
//				foundTypes = new TypeDeclaration[length];
//				System.arraycopy(this.types, 0, foundTypes, 0, length);
//			}
			ReferenceContext oldContext = Parser.this.referenceContext;
			Parser.this.recoveryScanner.resetTo(methodDeclaration.bodyStart, methodDeclaration.bodyEnd);
			Scanner oldScanner = Parser.this.scanner;
			Parser.this.scanner = Parser.this.recoveryScanner;
			Parser.this.parseStatements(
					methodDeclaration,
					methodDeclaration.bodyStart,
					methodDeclaration.bodyEnd,
					foundTypes,
					compilationUnit);
			Parser.this.scanner = oldScanner;
			Parser.this.referenceContext = oldContext;

//			for (int i = 0; i < length; i++) {
//				foundTypes[i].traverse(typeVisitor, scope);
//			}
		}
		public void endVisit(Initializer initializer, MethodScope scope) {
			TypeDeclaration[] foundTypes = null;
			int length = 0;
			if(this.typePtr > -1) {
				length = this.typePtr + 1;
				foundTypes = new TypeDeclaration[length];
				System.arraycopy(this.types, 0, foundTypes, 0, length);
			}
			ReferenceContext oldContext = Parser.this.referenceContext;
			Parser.this.recoveryScanner.resetTo(initializer.bodyStart, initializer.bodyEnd);
			Scanner oldScanner = Parser.this.scanner;
			Parser.this.scanner = Parser.this.recoveryScanner;
			Parser.this.parseStatements(
					this.enclosingType,
					initializer.bodyStart,
					initializer.bodyEnd,
					foundTypes,
					compilationUnit);
			Parser.this.scanner = oldScanner;
			Parser.this.referenceContext = oldContext;

			for (int i = 0; i < length; i++) {
				foundTypes[i].traverse(typeVisitor, scope);
			}
		}
	}
	class TypeVisitor extends ASTVisitor {
		public MethodVisitor methodVisitor;

		TypeDeclaration[] types = new TypeDeclaration[0];
		int typePtr = -1;

		public void endVisit(TypeDeclaration typeDeclaration, BlockScope scope) {
			endVisitType();
		}
		public void endVisit(TypeDeclaration typeDeclaration, ClassScope scope) {
			endVisitType();
		}
		private void endVisitType() {
			this.typePtr--;
		}
		public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
			return this.visit(typeDeclaration);
		}
		public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
			return this.visit(typeDeclaration);
		}
		private boolean visit(TypeDeclaration typeDeclaration) {
			if(this.types.length <= ++this.typePtr) {
				int length = this.typePtr;
				System.arraycopy(this.types, 0, this.types = new TypeDeclaration[length * 2 + 1], 0, length);
			}
			this.types[this.typePtr] = typeDeclaration;
			return true;
		}
		public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
			if(constructorDeclaration.isDefaultConstructor()) return false;

			constructorDeclaration.traverse(methodVisitor, scope);
			return false;
		}
		public boolean visit(Initializer initializer, MethodScope scope) {
			methodVisitor.enclosingType = this.types[typePtr];
			initializer.traverse(methodVisitor, scope);
			return false;
		}
		public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
			methodDeclaration.traverse(methodVisitor, scope);
			return false;
		}
	}

	if (false)
	{
	MethodVisitor methodVisitor = new MethodVisitor();
	TypeVisitor typeVisitor = new TypeVisitor();
	methodVisitor.typeVisitor = typeVisitor;
	typeVisitor.methodVisitor = methodVisitor;

	if(this.referenceContext instanceof AbstractMethodDeclaration) {
		((AbstractMethodDeclaration)this.referenceContext).traverse(methodVisitor, (Scope)null);
	}else if(this.referenceContext instanceof CompilationUnitDeclaration) {
		CompilationUnitDeclaration compilationUnitDeclaration=(CompilationUnitDeclaration)this.referenceContext;
		if (compilationUnitDeclaration.statements!=null)
			for (int i = 0; i < compilationUnitDeclaration.statements.length; i++) {
				if( compilationUnitDeclaration.statements[i] instanceof AbstractMethodDeclaration)
					((AbstractMethodDeclaration)compilationUnitDeclaration.statements[i] ).traverse(methodVisitor, (Scope)null);
			}
	} else if(this.referenceContext instanceof TypeDeclaration) {
		TypeDeclaration typeContext = (TypeDeclaration)this.referenceContext;

		int length = typeContext.fields.length;
		for (int i = 0; i < length; i++) {
			final FieldDeclaration fieldDeclaration = typeContext.fields[i];
			switch(fieldDeclaration.getKind()) {
				case AbstractVariableDeclaration.INITIALIZER:
					methodVisitor.enclosingType = typeContext;
					((Initializer) fieldDeclaration).traverse(methodVisitor, (MethodScope)null);
					break;
			}
		}
	}
	}
	else
	{
		CompilationUnitDeclaration compilationUnitDeclaration=(CompilationUnitDeclaration)this.referenceContext;

		ReferenceContext oldContext = Parser.this.referenceContext;
		int start = compilationUnitDeclaration.sourceStart;
		int end = compilationUnitDeclaration.sourceEnd;
		Parser.this.recoveryScanner.resetTo(start, end);
		Scanner oldScanner = Parser.this.scanner;
		Parser.this.scanner = Parser.this.recoveryScanner;
		/* unit creation */
		this.referenceContext =
			this.compilationUnit = compilationUnitDeclaration=
				new CompilationUnitDeclaration(
					this.problemReporter,
					compilationUnitDeclaration.compilationResult,
					end);

		Parser.this.parseStatements(
				compilationUnitDeclaration,
				start,
				end,
				null,
				compilationUnit);
		Parser.this.scanner = oldScanner;
		Parser.this.referenceContext = oldContext;
	}


}

public void recoveryExitFromVariable() {
	if(this.currentElement != null && this.currentElement.parent != null) {
		if(this.currentElement instanceof RecoveredLocalVariable) {

			int end = ((RecoveredLocalVariable)this.currentElement).localDeclaration.sourceEnd;
			this.currentElement.updateSourceEndIfNecessary(end);
			this.currentElement = this.currentElement.parent;
		} else if(this.currentElement instanceof RecoveredField
			&& !(this.currentElement instanceof RecoveredInitializer)) {

			int end = ((RecoveredField)this.currentElement).fieldDeclaration.sourceEnd;
			this.currentElement.updateSourceEndIfNecessary(end);
			this.currentElement = this.currentElement.parent;
		}
	}
}
/* Token check performed on every token shift once having entered
 * recovery mode.
 */
public void recoveryTokenCheck() {
	switch (this.currentToken) {
		case TokenNameLBRACE :
			RecoveredElement newElement = null;
			if(!this.ignoreNextOpeningBrace) {
				newElement = this.currentElement.updateOnOpeningBrace(this.scanner.startPosition - 1, this.scanner.currentPosition - 1);
			}
			this.lastCheckPoint = this.scanner.currentPosition;
			if (newElement != null){ // null means nothing happened
				this.restartRecovery = true; // opening brace detected
				this.currentElement = newElement;
			}
			break;

		case TokenNameRBRACE :
			this.rBraceStart = this.scanner.startPosition - 1;
			this.rBraceEnd = this.scanner.currentPosition - 1;
			this.endPosition = this.flushCommentsDefinedPriorTo(this.rBraceEnd);
			newElement =
				this.currentElement.updateOnClosingBrace(this.scanner.startPosition, this.rBraceEnd);
				this.lastCheckPoint = this.scanner.currentPosition;
			if (newElement != this.currentElement){
				this.currentElement = newElement;
//				if (newElement instanceof RecoveredField && this.dietInt <= 0) {
//					if (((RecoveredField)newElement).fieldDeclaration.type == null) { // enum constant
//						this.isInsideEnumConstantPart = true; // restore status
//					}
//				}
			}
			break;
		case TokenNameSEMICOLON :
			this.endStatementPosition = this.scanner.currentPosition - 1;
			this.endPosition = this.scanner.startPosition - 1;
			this.lastCheckPoint=this.scanner.currentPosition;
//			RecoveredType currentType = this.currentRecoveryType();
//			if(currentType != null) {
//				currentType.insideEnumConstantPart = false;
//			}
			// fall through
		default : {
			if (this.rBraceEnd > this.rBraceSuccessorStart && this.scanner.currentPosition != this.scanner.startPosition){
				this.rBraceSuccessorStart = this.scanner.startPosition;
			}
			break;
		}
	}
	this.ignoreNextOpeningBrace = false;
}

protected boolean shouldInsertSemicolon(int prevpos, int prevtoken) {
	Integer position = Integer.valueOf(prevpos);
	if (this.errorAction.contains(position)) {
		// should not insert a semi-colon at a location that has already be tried
		return false;
	}
	this.errorAction.add(position);
	return this.currentToken == TokenNameRBRACE
			|| scanner.getLineNumber(scanner.currentPosition) > scanner.getLineNumber(prevpos)
			|| this.currentToken==TokenNameEOF;
}

// A P I
protected void reportSyntaxErrors(boolean isDietParse, int oldFirstToken) {
	if(this.referenceContext instanceof MethodDeclaration) {
		MethodDeclaration methodDeclaration = (MethodDeclaration) this.referenceContext;
		if(methodDeclaration.errorInSignature){
			return;
		}
	}
	this.compilationUnit.compilationResult.lineSeparatorPositions = this.scanner.getLineEnds();
	this.scanner.recordLineSeparator = false;

	int start = this.scanner.initialPosition;
	int end = this.scanner.eofPosition == Integer.MAX_VALUE ? this.scanner.eofPosition : this.scanner.eofPosition - 1;
	if(isDietParse) {
		ProgramElement[] statements = this.compilationUnit.statements;
//		TypeDeclaration[] types = this.compilationUnit.types;
		int[][] intervalToSkip = org.eclipse.wst.jsdt.internal.compiler.parser.diagnose.RangeUtil.computeDietRange(statements);
//		int[][] intervalToSkip = org.eclipse.wst.jsdt.internal.compiler.parser.diagnose.RangeUtil.computeDietRange(types);
		DiagnoseParser diagnoseParser = new DiagnoseParser(this, oldFirstToken, start, end, intervalToSkip[0], intervalToSkip[1], intervalToSkip[2], this.options);
		diagnoseParser.diagnoseParse(false);

		reportSyntaxErrorsForSkippedMethod(statements);
//		reportSyntaxErrorsForSkippedMethod(types);
		this.scanner.resetTo(start, end);
	} else {
		DiagnoseParser diagnoseParser = new DiagnoseParser(this, oldFirstToken, start, end, this.options);
		diagnoseParser.diagnoseParse(this.options.performStatementsRecovery);
	}
}
private void reportSyntaxErrorsForSkippedMethod(ProgramElement[] statements){
	if(statements != null) {
		for (int i = 0; i < statements.length; i++) {
//			TypeDeclaration[] memberTypes = types[i].memberTypes;
//			if(memberTypes != null) {
//				reportSyntaxErrorsForSkippedMethod(memberTypes);
//			}
//
//			AbstractMethodDeclaration[] methods = types[i].methods;
//			if(methods != null) {
//				for (int j = 0; j < methods.length; j++) {
			if (statements[i] instanceof AbstractMethodDeclaration )
			{
				AbstractMethodDeclaration method = (AbstractMethodDeclaration)statements[i] ;
					if(method.errorInSignature) {
						DiagnoseParser diagnoseParser = new DiagnoseParser(this, TokenNameDIVIDE, method.declarationSourceStart, method.declarationSourceEnd, this.options);
						diagnoseParser.diagnoseParse(this.options.performStatementsRecovery);
					}
//				}
			}
			else if (statements[i] instanceof FieldDeclaration )
			{
//			  FieldDeclaration   field =(FieldDeclaration) statements[i] ;
//			if (fields != null) {
//				int length = fields.length;
//				for (int j = 0; j < length; j++) {
//					if (fields[j] instanceof Initializer) {
//						Initializer initializer = (Initializer)fields[j];
//						if(initializer.errorInSignature){
//							DiagnoseParser diagnoseParser = new DiagnoseParser(this, TokenNameRIGHT_SHIFT, initializer.declarationSourceStart, initializer.declarationSourceEnd, this.options);
//							diagnoseParser.diagnoseParse(this.options.performStatementsRecovery);
//						}
//					}
//				}
			}
		}
	}
}
protected void resetModifiers() {
	this.modifiers = ClassFileConstants.AccDefault;
	this.modifiersSourceStart = -1; // <-- see comment into modifiersFlag(int)
	this.scanner.commentPtr = -1;
}
/*
 * Reset context so as to resume to regular parse loop
 */
protected void resetStacks() {

	this.astPtr = -1;
	this.astLengthPtr = -1;
	this.expressionPtr = -1;
	this.expressionLengthPtr = -1;
	this.identifierPtr = -1;
	this.identifierLengthPtr	= -1;
	this.intPtr = -1;
	this.nestedMethod[this.nestedType = 0] = 0; // need to reset for further reuse
	this.variablesCounter[this.nestedType] = 0;
	this.dimensions = 0 ;
	this.realBlockStack[this.realBlockPtr = 0] = 0;
	this.recoveredStaticInitializerStart = 0;
	this.listLength = 0;
	this.listTypeParameterLength = 0;

	this.genericsIdentifiersLengthPtr = -1;
	this.genericsLengthPtr = -1;
	this.genericsPtr = -1;
	this.errorAction = new HashSet();
}
/*
 * Reset context so as to resume to regular parse loop
 * If unable to reset for resuming, answers false.
 *
 * Move checkpoint location, reset internal stacks and
 * decide which grammar goal is activated.
 */
protected boolean resumeAfterRecovery() {
	if(!this.methodRecoveryActivated && !this.statementRecoveryActivated) {

		// reset internal stacks
		this.resetStacks();
		this.resetModifiers();

		/* attempt to move checkpoint location */
		if (!this.moveRecoveryCheckpoint()) {
			return false;
		}

		// only look for headers
		if (this.referenceContext instanceof CompilationUnitDeclaration){
			if (DO_DIET_PARSE)
			{
				goForHeaders();
				this.diet = true; // passed this point, will not consider method bodies
			}
			else
				goForProgramElements();
			return true;
		}

		// does not know how to restart
		return false;
	} else if(!this.statementRecoveryActivated || !DO_DIET_PARSE) {

		// reset internal stacks
		this.resetStacks();
		this.resetModifiers();

		/* attempt to move checkpoint location */
		if (!this.moveRecoveryCheckpoint()) {
			return false;
		}

		// only look for headers
		if (DO_DIET_PARSE)
			goForHeaders();
		else
			goForProgramElements();
		return true;
	} else {
		return false;
	}
}
protected boolean resumeOnSyntaxError() {
	this.checkExternalizeStrings = false;
	this.scanner.checkNonExternalizedStringLiterals = false;
	/* request recovery initialization */
	if (this.currentElement == null){
		// Reset javadoc before restart parsing after recovery
		this.javadoc = null;

		// do not investigate deeper in statement recovery
		if (this.statementRecoveryActivated) return false;

		// build some recovered elements
		this.currentElement = buildInitialRecoveryState();
	}
	/* do not investigate deeper in recovery when no recovered element */
	if (this.currentElement == null) return false;

	/* manual forced recovery restart - after headers */
	if (this.restartRecovery){
		this.restartRecovery = false;
	}
	/* update recovery state with current error state of the parser */
	this.updateRecoveryState();

	/* attempt to reset state in order to resume to parse loop */
	return this.resumeAfterRecovery();
}
public void setMethodsFullRecovery(boolean enabled) {
	this.options.performMethodsFullRecovery = enabled;
}
public void setStatementsRecovery(boolean enabled) {
	if(enabled) this.options.performMethodsFullRecovery = true;
	this.options.performStatementsRecovery = enabled;
}
public String toString() {

	StringBuilder sb = new StringBuilder();
	sb.append("lastCheckpoint : int = "); //$NON-NLS-1$
	sb.append(this.lastCheckPoint);
	sb.append('\n');
	sb.append("identifierStack : char[");//$NON-NLS-1$
	sb.append(this.identifierPtr + 1);
	sb.append("][] = {");//$NON-NLS-1$
	for (int i = 0; i <= this.identifierPtr; i++) {
		sb.append('\"');
		sb.append(this.identifierStack[i]);
		sb.append('\"').append(',');
	}
	sb.append('}').append('\n');

	sb.append("identifierLengthStack : int["); //$NON-NLS-1$
	sb.append(this.identifierLengthPtr + 1);
	sb.append("] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.identifierLengthPtr; i++) {
		sb.append(this.identifierLengthStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');

	sb.append("astLengthStack : int["); //$NON-NLS-1$
	sb.append(this.astLengthPtr + 1);
	sb.append("] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.astLengthPtr; i++) {
		sb.append(this.astLengthStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');
	sb.append("astPtr : int = "); //$NON-NLS-1$
	sb.append(this.astPtr);
	sb.append('\n');

	sb.append("intStack : int["); //$NON-NLS-1$
	sb.append(this.intPtr + 1);
	sb.append("] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.intPtr; i++) {
		sb.append(this.intStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');
	sb.append("intPtr : int = "); //$NON-NLS-1$
	sb.append(this.intPtr);
	sb.append('\n');

	sb.append("expressionLengthStack : int["); //$NON-NLS-1$
	sb.append(this.expressionLengthPtr + 1);
	sb.append("] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.expressionLengthPtr; i++) {
		sb.append(this.expressionLengthStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');

	sb.append("expressionPtr : int = "); //$NON-NLS-1$
	sb.append(this.expressionPtr);
	sb.append('\n');

	sb.append("genericsIdentifiersLengthStack : int["); //$NON-NLS-1$
	sb.append(this.genericsIdentifiersLengthPtr + 1);
	sb.append("] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.genericsIdentifiersLengthPtr; i++) {
		sb.append(this.genericsIdentifiersLengthStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');

	sb.append("genericsLengthStack : int["); //$NON-NLS-1$
	sb.append(this.genericsLengthPtr + 1);
	sb.append("] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.genericsLengthPtr; i++) {
		sb.append(this.genericsLengthStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');

	sb.append("genericsPtr : int = "); //$NON-NLS-1$
	sb.append(this.genericsPtr);
	sb.append('\n');

	sb.append("\n\n\n----------------Scanner--------------\n"); //$NON-NLS-1$
	sb.append(this.scanner);
	return sb.toString();

}
/*
 * Update recovery state based on current parser/scanner state
 */
protected void updateRecoveryState() {

	/* expose parser state to recovery state */
	this.currentElement.updateFromParserState();

	/* check and update recovered state based on current token,
		this action is also performed when shifting token after recovery
		got activated once.
	*/
	this.recoveryTokenCheck();
}
protected void updateSourceDeclarationParts(int variableDeclaratorsCounter) {
	//fields is a definition of fields that are grouped together like in
	//public int[] a, b[], c
	//which results into 3 fields.

	FieldDeclaration field;
	int endTypeDeclarationPosition =
		-1 + this.astStack[this.astPtr - variableDeclaratorsCounter + 1].sourceStart;
	for (int i = 0; i < variableDeclaratorsCounter - 1; i++) {
		//last one is special(see below)
		field = (FieldDeclaration) this.astStack[this.astPtr - i - 1];
		field.endPart1Position = endTypeDeclarationPosition;
		field.endPart2Position = -1 + this.astStack[this.astPtr - i].sourceStart;
	}
	//last one
	(field = (FieldDeclaration) this.astStack[this.astPtr]).endPart1Position =
		endTypeDeclarationPosition;
	field.endPart2Position = field.declarationSourceEnd;

}
protected void updateSourcePosition(Expression exp) {
	//update the source Position of the expression

	//this.intStack : int int
	//-->
	//this.intStack :

	exp.sourceEnd = this.intStack[this.intPtr--];
	exp.sourceStart = this.intStack[this.intPtr--];
}

public void inferTypes(CompilationUnitDeclaration parsedUnit, CompilerOptions compileOptions) {
	if (parsedUnit.typesHaveBeenInferred)
		return;
	if (compileOptions==null)
		compileOptions=this.options;
	
	if (this.inferenceEngines==null)
		initializeInferenceEngine(parsedUnit);
//	InferEngine inferEngine=compileOptions.inferOptions.createEngine();
	for (int i=0;i<this.inferenceEngines.length;i++)
	{
		IInferEngine engine=this.inferenceEngines[i];
		PerformanceStats stats= PerformanceStats.getStats(PERFORMANCE__INFER_TYPES, engine);
		try {
			stats.startRun(new String(parsedUnit.getFileName()));

			engine.initialize();
			if (engine instanceof IInferEngineExtension)
				((IInferEngineExtension) engine).setCompilationUnit(parsedUnit, this.scanner.getSource());
			else
				engine.setCompilationUnit(parsedUnit);

			engine.doInfer();
		} catch (RuntimeException e) {
			org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "error during type inferencing"); //$NON-NLS-1$
		}
		finally {
			stats.endRun();
			if (REPORT_PERFORMANCE__INFER_TYPES && stats.isFailure()) {
				IStatus status = new Status(IStatus.WARNING, JavaScriptCore.PLUGIN_ID, IStatus.OK, "Inference Engine took too long: " + engine, null); //$NON-NLS-1$
				Platform.getLog(Platform.getBundle(JavaScriptCore.PLUGIN_ID)).log(status);
			}
		}
	}
	parsedUnit.typesHaveBeenInferred=true;
}


}
