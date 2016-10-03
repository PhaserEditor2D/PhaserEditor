/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist.complete;

/*
 * Parser able to build specific completion parse nodes, given a cursorLocation.
 *
 * Cursor location denotes the position of the last character behind which completion
 * got requested:
 *  -1 means completion at the very beginning of the source
 *	0  means completion behind the first character
 *  n  means completion behind the n-th character
 */

import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFieldReference;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IThisReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.codeassist.impl.AssistParser;
import org.eclipse.wst.jsdt.internal.codeassist.impl.Keywords;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.parser.JavadocParser;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredBlock;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredElement;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredField;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredInitializer;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredLocalVariable;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredMethod;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredType;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class CompletionParser extends AssistParser {
	// OWNER
	protected static final int COMPLETION_PARSER = 1024;
	protected static final int COMPLETION_OR_ASSIST_PARSER = ASSIST_PARSER + COMPLETION_PARSER;

	// KIND : all values known by CompletionParser are between 1025 and 1549
	protected static final int K_BLOCK_DELIMITER = COMPLETION_PARSER + 1; // whether we are inside a block
	protected static final int K_SELECTOR_INVOCATION_TYPE = COMPLETION_PARSER + 2; // whether we are inside a message send
	protected static final int K_SELECTOR_QUALIFIER = COMPLETION_PARSER + 3; // whether we are inside a message send
	protected static final int K_BETWEEN_CATCH_AND_RIGHT_PAREN = COMPLETION_PARSER + 4; // whether we are between the keyword 'catch' and the following ')'
	protected static final int K_NEXT_TYPEREF_IS_CLASS = COMPLETION_PARSER + 5; // whether the next type reference is a class
	protected static final int K_NEXT_TYPEREF_IS_INTERFACE = COMPLETION_PARSER + 6; // whether the next type reference is an interface
	protected static final int K_NEXT_TYPEREF_IS_EXCEPTION = COMPLETION_PARSER + 7; // whether the next type reference is an exception
	protected static final int K_BETWEEN_NEW_AND_LEFT_BRACKET = COMPLETION_PARSER + 8; // whether we are between the keyword 'new' and the following left braket, ie. '[', '(' or '{'
	protected static final int K_INSIDE_THROW_STATEMENT = COMPLETION_PARSER + 9; // whether we are between the keyword 'throw' and the end of a throw statement
	protected static final int K_INSIDE_RETURN_STATEMENT = COMPLETION_PARSER + 10; // whether we are between the keyword 'return' and the end of a return statement
	protected static final int K_CAST_STATEMENT = COMPLETION_PARSER + 11; // whether we are between ')' and the end of a cast statement
	protected static final int K_LOCAL_INITIALIZER_DELIMITER = COMPLETION_PARSER + 12;
	protected static final int K_ARRAY_INITIALIZER = COMPLETION_PARSER + 13;
	protected static final int K_ARRAY_CREATION = COMPLETION_PARSER + 14;
	protected static final int K_UNARY_OPERATOR = COMPLETION_PARSER + 15;
	protected static final int K_BINARY_OPERATOR = COMPLETION_PARSER + 16;
	protected static final int K_ASSISGNMENT_OPERATOR = COMPLETION_PARSER + 17;
	protected static final int K_CONDITIONAL_OPERATOR = COMPLETION_PARSER + 18;
	protected static final int K_BETWEEN_IF_AND_RIGHT_PAREN = COMPLETION_PARSER + 19;
	protected static final int K_BETWEEN_WHILE_AND_RIGHT_PAREN = COMPLETION_PARSER + 20;
	protected static final int K_BETWEEN_FOR_AND_RIGHT_PAREN = COMPLETION_PARSER + 21;
	protected static final int K_BETWEEN_SWITCH_AND_RIGHT_PAREN = COMPLETION_PARSER + 22;
	protected static final int K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN = COMPLETION_PARSER + 23;
	protected static final int K_INSIDE_ASSERT_STATEMENT = COMPLETION_PARSER + 24;
	protected static final int K_SWITCH_LABEL= COMPLETION_PARSER + 25;
	protected static final int K_BETWEEN_CASE_AND_COLON = COMPLETION_PARSER + 26;
	protected static final int K_BETWEEN_DEFAULT_AND_COLON = COMPLETION_PARSER + 27;
	protected static final int K_BETWEEN_LEFT_AND_RIGHT_BRACKET = COMPLETION_PARSER + 28;
	protected static final int K_EXTENDS_KEYWORD = COMPLETION_PARSER + 29;
	protected static final int K_PARAMETERIZED_METHOD_INVOCATION = COMPLETION_PARSER + 30;
	protected static final int K_PARAMETERIZED_ALLOCATION = COMPLETION_PARSER + 31;
	protected static final int K_PARAMETERIZED_CAST = COMPLETION_PARSER + 32;
	protected static final int K_BETWEEN_ANNOTATION_NAME_AND_RPAREN = COMPLETION_PARSER + 33;
	protected static final int K_INSIDE_BREAK_STATEMENT = COMPLETION_PARSER + 34;
	protected static final int K_INSIDE_CONTINUE_STATEMENT = COMPLETION_PARSER + 35;
	protected static final int K_LABEL = COMPLETION_PARSER + 36;
	protected static final int K_MEMBER_VALUE_ARRAY_INITIALIZER = COMPLETION_PARSER + 37;

	public final static char[] FAKE_TYPE_NAME = new char[]{' '};
	public final static char[] FAKE_METHOD_NAME = new char[]{' '};
	public final static char[] FAKE_ARGUMENT_NAME = new char[]{' '};
	public final static char[] VALUE = new char[]{'v', 'a', 'l', 'u', 'e'};

	/* public fields */

	public int cursorLocation;
	public ASTNode assistNodeParent; // the parent node of assist node
	/* the following fields are internal flags */

	// block kind
	static final int IF = 1;
	static final int TRY = 2;
	static final int CATCH = 3;
	static final int WHILE = 4;
	static final int SWITCH = 5;
	static final int FOR = 6;
	static final int DO = 7;
	static final int SYNCHRONIZED = 8;

	// label kind
	static final int DEFAULT = 1;

	// invocation type constants
	static final int EXPLICIT_RECEIVER = 0;
	static final int NO_RECEIVER = -1;
	static final int SUPER_RECEIVER = -2;
	static final int NAME_RECEIVER = -3;
	static final int ALLOCATION = -4;
	static final int QUALIFIED_ALLOCATION = -5;

	static final int QUESTION = 1;
	static final int COLON = 2;

	// K_BETWEEN_ANNOTATION_NAME_AND_RPAREN arguments
	static final int LPAREN_NOT_CONSUMED = 1;
	static final int LPAREN_CONSUMED = 2;
	static final int ANNOTATION_NAME_COMPLETION = 4;

	// K_PARAMETERIZED_METHOD_INVOCATION arguments
	static final int INSIDE_NAME = 1;

	// the type of the current invocation (one of the invocation type constants)
	int invocationType;

	// a pointer in the expression stack to the qualifier of a invocation
	int qualifier;

	// last modifiers info
	int lastModifiers = ClassFileConstants.AccDefault;
	int lastModifiersStart = -1;

	// depth of '(', '{' and '[]'
	int bracketDepth;

	// show if the current token can be an explicit constructor
	int canBeExplicitConstructor = NO;
	static final int NO = 0;
	static final int NEXTTOKEN = 1;
	static final int YES = 2;

	protected static final int LabelStackIncrement = 10;
	char[][] labelStack = new char[LabelStackIncrement][];
	int labelPtr = -1;

	boolean isAlreadyAttached;
	public boolean record = false;
	public boolean skipRecord = false;
	public int recordFrom;
	public int recordTo;
	public int potentialVariableNamesPtr;
	public char[][] potentialVariableNames;
	public int[] potentialVariableNameStarts;
	public int[] potentialVariableNameEnds;

public CompletionParser(ProblemReporter problemReporter) {
	super(problemReporter);
	this.reportSyntaxErrorIsRequired = false;
	this.javadocParser.checkDocComment = true;
}
private void addPotentialName(char[] potentialVariableName, int start, int end) {
	int length = this.potentialVariableNames.length;
	if (this.potentialVariableNamesPtr >= length - 1) {
		System.arraycopy(
				this.potentialVariableNames,
				0,
				this.potentialVariableNames = new char[length * 2][],
				0,
				length);
		System.arraycopy(
				this.potentialVariableNameStarts,
				0,
				this.potentialVariableNameStarts = new int[length * 2],
				0,
				length);
		System.arraycopy(
				this.potentialVariableNameEnds,
				0,
				this.potentialVariableNameEnds = new int[length * 2],
				0,
				length);
	}
	this.potentialVariableNames[++this.potentialVariableNamesPtr] = potentialVariableName;
	this.potentialVariableNameStarts[this.potentialVariableNamesPtr] = start;
	this.potentialVariableNameEnds[this.potentialVariableNamesPtr] = end;
}
public void startRecordingIdentifiers(int from, int to) {
	this.record = true;
	this.skipRecord = false;
	this.recordFrom = from;
	this.recordTo = to;

	this.potentialVariableNamesPtr = -1;
	this.potentialVariableNames = new char[10][];
	this.potentialVariableNameStarts = new int[10];
	this.potentialVariableNameEnds = new int[10];
}
public void stopRecordingIdentifiers() {
	this.record = true;
	this.skipRecord = false;
}

public char[] assistIdentifier(){
	return ((CompletionScanner)scanner).completionIdentifier;
}
protected void attachOrphanCompletionNode(){
	if(assistNode == null || this.isAlreadyAttached) return;


	if (STOP_AT_CURSOR)
		this.isAlreadyAttached = true;
	if (this.isOrphanCompletionNode) {
		if (!STOP_AT_CURSOR)
			this.isAlreadyAttached = true;
		ASTNode orphan = this.assistNode;
		this.isOrphanCompletionNode = false;

		if (currentElement instanceof RecoveredUnit){
			if (orphan instanceof ImportReference){
				currentElement.add((ImportReference)orphan, 0);
			}
		}

		/* if in context of a type, then persists the identifier into a fake field return type */
		if (currentElement instanceof RecoveredType){
			RecoveredType recoveredType = (RecoveredType)currentElement;
			/* filter out cases where scanner is still inside type header */
			if (recoveredType.foundOpeningBrace) {
				/* generate a pseudo field with a completion on type reference */
				if (orphan instanceof TypeReference){
					TypeReference fieldType;

					int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
					int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
					if(kind == K_BINARY_OPERATOR && info == LESS && this.identifierPtr > -1) {
						this.pushOnGenericsStack(orphan);
						fieldType = getTypeReference(0);
						this.assistNodeParent = fieldType;
					} else {
						fieldType = (TypeReference)orphan;
					}

					CompletionOnFieldType fieldDeclaration = new CompletionOnFieldType(fieldType, false);

					// retrieve available modifiers if any
					if (intPtr >= 2 && intStack[intPtr-1] == this.lastModifiersStart && intStack[intPtr-2] == this.lastModifiers){
						fieldDeclaration.modifiersSourceStart = intStack[intPtr-1];
						fieldDeclaration.modifiers = intStack[intPtr-2];
					}

					currentElement = currentElement.add(fieldDeclaration, 0);
					return;
				}
			}
		}
		/* if in context of a method, persists if inside arguments as a type */
		if (currentElement instanceof RecoveredMethod){
			RecoveredMethod recoveredMethod = (RecoveredMethod)currentElement;
			/* only consider if inside method header */
			if (!recoveredMethod.foundOpeningBrace) {
				//if (rParenPos < lParenPos){ // inside arguments
				if (orphan instanceof TypeReference){
					currentElement = currentElement.parent.add(
						new CompletionOnFieldType((TypeReference)orphan, true), 0);
					return;
				}
			}
		}

		if ((topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CATCH_AND_RIGHT_PAREN)) {
			if (this.assistNode instanceof CompletionOnSingleTypeReference &&
					((CompletionOnSingleTypeReference)this.assistNode).isException()) {
				buildMoreTryStatementCompletionContext((TypeReference)this.assistNode);
				return;
			} else if (this.assistNode instanceof CompletionOnQualifiedTypeReference &&
					((CompletionOnQualifiedTypeReference)this.assistNode).isException()) {
				buildMoreTryStatementCompletionContext((TypeReference)this.assistNode);
				return;
			}
		}		// add the completion node to the method declaration or constructor declaration
		if (orphan instanceof Statement) {
			/* check for completion at the beginning of method body
				behind an invalid signature
			 */
			RecoveredMethod method = currentElement.enclosingMethod();
			if (method != null){
				AbstractMethodDeclaration methodDecl = method.methodDeclaration;
				if ((methodDecl.bodyStart == methodDecl.sourceEnd+1) // was missing opening brace
						&& (Util.getLineNumber(orphan.sourceStart, scanner.lineEnds, 0, scanner.linePtr)
								== Util.getLineNumber(methodDecl.sourceEnd, scanner.lineEnds, 0, scanner.linePtr))){
					return;
				}
			}
			// add the completion node as a statement to the list of block statements
			currentElement = currentElement.add((Statement)orphan, 0);
			return;
		}
	}

	if (this.isInsideAnnotation()) {
		// push top expression on ast stack if it contains the completion node
		Expression expression;
		if (this.expressionPtr > -1) {
			expression = this.expressionStack[this.expressionPtr];
			if(expression == assistNode) {
				if (this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_MEMBER_VALUE_ARRAY_INITIALIZER ) {
					ArrayInitializer arrayInitializer = new ArrayInitializer();
					arrayInitializer.expressions = new Expression[]{expression};
				} else if(this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN) {
					if (expression instanceof SingleNameReference) {
						SingleNameReference nameReference = (SingleNameReference) expression;
						return;
					} else if (expression instanceof QualifiedNameReference) {
					}
				} else {
					int index;
					if((index = lastIndexOfElement(K_ATTRIBUTE_VALUE_DELIMITER)) != -1) {
						int attributeIndentifierPtr = this.elementInfoStack[index];
						int identLengthPtr = this.identifierLengthPtr;
						int identPtr = this.identifierPtr;
						while (attributeIndentifierPtr < identPtr) {
							identPtr -= this.identifierLengthStack[identLengthPtr--];
						}

						if(attributeIndentifierPtr != identPtr) return;

						this.identifierLengthPtr = identLengthPtr;
						this.identifierPtr = identPtr;

						this.identifierLengthPtr--;
						this.identifierPtr--;

						return;
					}
				}
			} else {
				CompletionNodeDetector detector =  new CompletionNodeDetector(this.assistNode, expression);
				if(detector.containsCompletionNode()) {
				}
			}
		}
	}

	if(this.currentElement instanceof RecoveredType || this.currentElement instanceof RecoveredMethod) {
		if(this.currentElement instanceof RecoveredType) {
			RecoveredType recoveredType = (RecoveredType)this.currentElement;
		}

		if ((!isInsideMethod() && !isInsideFieldInitialization())) {
			if(this.genericsPtr > -1 && this.genericsLengthPtr > -1 && this.genericsIdentifiersLengthPtr > -1) {
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
				int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
				int genPtr = this.genericsPtr;
				done : for(int i = 0; i <= this.identifierLengthPtr && numberOfIdentifiers > 0; i++){
					int identifierLength = this.identifierLengthStack[this.identifierLengthPtr - i];
					int length = this.genericsLengthStack[this.genericsLengthPtr - i];
					for(int j = 0; j < length; j++) {
						ASTNode node = this.genericsStack[genPtr - j];
						CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, node);
						if(detector.containsCompletionNode()) {
							if(node == this.assistNode){
								if(this.identifierLengthPtr > -1 &&	this.identifierLengthStack[this.identifierLengthPtr]!= 0) {
									TypeReference ref = this.getTypeReference(0);
									this.assistNodeParent = ref;
								}
							} else {
								this.assistNodeParent = detector.getCompletionNodeParent();
							}
							break done;
						}
					}
					genPtr -= length;
					numberOfIdentifiers -= identifierLength;
				}
				if(this.assistNodeParent != null && this.assistNodeParent instanceof TypeReference) {
					if(this.currentElement instanceof RecoveredType) {
						this.currentElement = this.currentElement.add(new CompletionOnFieldType((TypeReference)this.assistNodeParent, false), 0);
					} else {
						this.currentElement = this.currentElement.add((TypeReference)this.assistNodeParent, 0);
					}
				}
			}
		}
	}

	// the following code applies only in methods, constructors or initializers
	if ((!isInsideMethod() && !isInsideFieldInitialization() && !isInsideAttributeValue())) {
		return;
	}

	if(this.genericsPtr > -1) {
		ASTNode node = this.genericsStack[this.genericsPtr];
		CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, node);
		if(detector.containsCompletionNode()) {
			/* check for completion at the beginning of method body
				behind an invalid signature
			 */
			RecoveredMethod method = this.currentElement.enclosingMethod();
			if (method != null){
				AbstractMethodDeclaration methodDecl = method.methodDeclaration;
				if ((methodDecl.bodyStart == methodDecl.sourceEnd+1) // was missing opening brace
						&& (Util.getLineNumber(node.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
								== Util.getLineNumber(methodDecl.sourceEnd, this.scanner.lineEnds, 0, this.scanner.linePtr))){
					return;
				}
			}
			if(node == this.assistNode){
				buildMoreGenericsCompletionContext(node,true);
			}
		}
	}

	// push top expression on ast stack if it contains the completion node
	Expression expression;
	if (this.expressionPtr > -1) {
		expression = this.expressionStack[this.expressionPtr];
		CompletionNodeDetector detector = new CompletionNodeDetector(assistNode, expression);
		if(detector.containsCompletionNode()) {
			/* check for completion at the beginning of method body
				behind an invalid signature
			 */
			RecoveredMethod method = currentElement.enclosingMethod();
			if (method != null){
				AbstractMethodDeclaration methodDecl = method.methodDeclaration;
				if ((methodDecl.bodyStart == methodDecl.sourceEnd+1) // was missing opening brace
						&& (Util.getLineNumber(expression.sourceStart, scanner.lineEnds, 0, scanner.linePtr)
								== Util.getLineNumber(methodDecl.sourceEnd, scanner.lineEnds, 0, scanner.linePtr))){
					return;
				}
			}
			if(expression == assistNode
				|| (expression instanceof AllocationExpression
					&& ((AllocationExpression)expression).type == assistNode)){
				buildMoreCompletionContext(expression);
			} else {
				assistNodeParent = detector.getCompletionNodeParent();
				if(assistNodeParent != null) {
					currentElement = currentElement.add((Statement)assistNodeParent, 0);
				} else {
					currentElement = currentElement.add(expression, 0);
				}
			}
		}
	}
}

private void buildMoreCompletionContext(Expression expression) {
	Statement statement = expression;
	int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
	if(kind != 0) {
		int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
		nextElement : switch (kind) {
			case K_SELECTOR_QUALIFIER :
				int selector = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 2);
				if(false){// Not Possible selector == THIS_CONSTRUCTOR || selector == SUPER_CONSTRUCTOR) {
					ExplicitConstructorCall call = new ExplicitConstructorCall(ExplicitConstructorCall.This);
					call.arguments = new Expression[] {expression};
					call.sourceStart = expression.sourceStart;
					call.sourceEnd = expression.sourceEnd;
					assistNodeParent = call;
				} else {
					int invocType = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER,1);
					int qualifierExprPtr = info;

					// find arguments
					int length = expressionLengthStack[expressionLengthPtr];

					// search previous arguments if missing
					if(this.expressionPtr > 0 && this.expressionLengthPtr > 0 && length == 1) {
						int start = 0;
						if (invocType != ALLOCATION && invocType != QUALIFIED_ALLOCATION)
						  start=(int) (identifierPositionStack[selector] >>> 32);
						else
							start=expressionStack[info].sourceStart;

						if(this.expressionStack[expressionPtr-1] != null && this.expressionStack[expressionPtr-1].sourceStart > start) {
							length += expressionLengthStack[expressionLengthPtr-1];
						}

					}

					Expression[] arguments = null;
					if (length != 0) {
						arguments = new Expression[length];
						expressionPtr -= length;
						System.arraycopy(expressionStack, expressionPtr + 1, arguments, 0, length-1);
						arguments[length-1] = expression;
					}

					if(invocType != ALLOCATION && invocType != QUALIFIED_ALLOCATION) {
						MessageSend messageSend = new MessageSend();
						messageSend.selector = identifierStack[selector];
						messageSend.arguments = arguments;

						// find receiver
						switch (invocType) {
							case NO_RECEIVER:
								messageSend.receiver = ThisReference.implicitThis();
								break;
							case NAME_RECEIVER:
								// remove special flags for primitive types
								while (this.identifierLengthPtr >= 0 && this.identifierLengthStack[this.identifierLengthPtr] < 0) {
									this.identifierLengthPtr--;
								}

								// remove selector
								this.identifierPtr--;
								if(this.genericsPtr > -1 && this.genericsLengthPtr > -1 && this.genericsLengthStack[this.genericsLengthPtr] > 0) {
									// is inside a paremeterized method: bar.<X>.foo
									this.identifierLengthPtr--;
								} else {
									this.identifierLengthStack[this.identifierLengthPtr]--;
								}
								// consume the receiver
								int identifierLength = this.identifierLengthStack[this.identifierLengthPtr];
								if(this.identifierPtr > -1 && identifierLength > 0 && this.identifierPtr + 1 >= identifierLength) {
									messageSend.receiver = this.getUnspecifiedReference();
								} else {
									messageSend = null;
								}
								break;
							case SUPER_RECEIVER:
								messageSend.receiver = new SuperReference(0, 0);
								break;
							case EXPLICIT_RECEIVER:
								messageSend.receiver = this.expressionStack[qualifierExprPtr];
								break;
							default :
								messageSend.receiver = ThisReference.implicitThis();
								break;
						}
						assistNodeParent = messageSend;
					} else {
						if(invocType == ALLOCATION) {
							AllocationExpression allocationExpr = new AllocationExpression();
							allocationExpr.arguments = arguments;
//							pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
//							pushOnGenericsLengthStack(0);
//							allocationExpr.type = getTypeReference(0);
							allocationExpr.member=expressionStack[info];
							assistNodeParent = allocationExpr;
						} else {
							QualifiedAllocationExpression allocationExpr = new QualifiedAllocationExpression();
							allocationExpr.enclosingInstance = this.expressionStack[qualifierExprPtr];
							allocationExpr.arguments = arguments;
							pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
							pushOnGenericsLengthStack(0);

							allocationExpr.type = getTypeReference(0);
							assistNodeParent = allocationExpr;
						}
					}
				}
				break nextElement;
			case K_INSIDE_RETURN_STATEMENT :
				if(info == bracketDepth) {
					ReturnStatement returnStatement = new ReturnStatement(expression, expression.sourceStart, expression.sourceEnd);
					assistNodeParent = returnStatement;
				}
				break nextElement;
			case K_UNARY_OPERATOR :
				if(expressionPtr > -1) {
					Expression operatorExpression = null;
					switch (info) {
						case PLUS_PLUS :
							operatorExpression = new PrefixExpression(expression,IntLiteral.getOne(), PLUS, expression.sourceStart);
							break;
						case MINUS_MINUS :
							operatorExpression = new PrefixExpression(expression,IntLiteral.getOne(), MINUS, expression.sourceStart);
							break;
						default :
							operatorExpression = new UnaryExpression(expression, info);
							break;
					}
					assistNodeParent = operatorExpression;
				}
				break nextElement;
			case K_BINARY_OPERATOR :
				if(expressionPtr > -1) {
					Expression operatorExpression = null;
					Expression left = null;
					if(expressionPtr == 0) {
						// it is  a ***_NotName rule
						if(this.identifierPtr > -1) {
							left = getUnspecifiedReferenceOptimized();
						}
					} else {
						left = this.expressionStack[expressionPtr-1];
						// is it a ***_NotName rule ?
						if(this.identifierPtr > -1) {
							int start = (int) (identifierPositionStack[this.identifierPtr] >>> 32);
							if(left.sourceStart < start) {
								left = getUnspecifiedReferenceOptimized();
							}
						}
					}

					if(left != null) {
						switch (info) {
							case AND_AND :
								operatorExpression = new AND_AND_Expression(left, expression, info);
								break;
							case OR_OR :
								operatorExpression = new OR_OR_Expression(left, expression, info);
								break;
							case EQUAL_EQUAL :
							case NOT_EQUAL :
								operatorExpression = new EqualExpression(left, expression, info);
								break;
							default :
								operatorExpression = new BinaryExpression(left, expression, info);
								break;
						}
					}
					if(operatorExpression != null) {
						assistNodeParent = operatorExpression;
					}
				}
				break nextElement;
			case K_ARRAY_INITIALIZER :
				ArrayInitializer arrayInitializer = new ArrayInitializer();
				arrayInitializer.expressions = new Expression[]{expression};
				expressionPtr -= expressionLengthStack[expressionLengthPtr--];

				if(expressionLengthPtr > -1
					&& expressionPtr > -1
					&& this.expressionStack[expressionPtr] != null
					&& this.expressionStack[expressionPtr].sourceStart > info) {
					expressionLengthPtr--;
				}

				lastCheckPoint = scanner.currentPosition;

				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_ARRAY_CREATION) {
					ArrayAllocationExpression allocationExpression = new ArrayAllocationExpression();
					pushOnGenericsLengthStack(0);
					pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
					allocationExpression.type = getTypeReference(0);
					allocationExpression.type.bits |= ASTNode.IgnoreRawTypeCheck; // no need to worry about raw type usage
					int length = expressionLengthStack[expressionLengthPtr];
					allocationExpression.dimensions = new Expression[length];

					allocationExpression.initializer = arrayInitializer;
					assistNodeParent = allocationExpression;
				} else if(currentElement instanceof RecoveredField && !(currentElement instanceof RecoveredInitializer)) {
					RecoveredField recoveredField = (RecoveredField) currentElement;
					if(recoveredField.fieldDeclaration.type.dimensions() == 0) {
						Block block = new Block(0);
						block.sourceStart = info;
						currentElement = currentElement.add(block, 1);
					} else {
						statement = arrayInitializer;
					}
				} else if(currentElement instanceof RecoveredLocalVariable) {
					RecoveredLocalVariable recoveredLocalVariable = (RecoveredLocalVariable) currentElement;
					if(recoveredLocalVariable.localDeclaration.type.dimensions() == 0) {
						Block block = new Block(0);
						block.sourceStart = info;
						currentElement = currentElement.add(block, 1);
					} else {
						statement = arrayInitializer;
					}
				} else {
					statement = arrayInitializer;
				}
				break nextElement;
			case K_ARRAY_CREATION :
				ArrayAllocationExpression allocationExpression = new ArrayAllocationExpression();
				allocationExpression.type = getTypeReference(0);
				allocationExpression.dimensions = new Expression[]{expression};

				assistNodeParent = allocationExpression;
				break nextElement;
			case K_ASSISGNMENT_OPERATOR :
				if(expressionPtr > 0 && expressionStack[expressionPtr - 1] != null) {
					Assignment assignment;
					if(info == EQUAL) {
						assignment = new Assignment(
							expressionStack[expressionPtr - 1],
							expression,
							expression.sourceEnd
						);
					} else {
						assignment = new CompoundAssignment(
							expressionStack[expressionPtr - 1],
							expression,
							info,
							expression.sourceEnd
						);
					}
					assistNodeParent = assignment;
				}
				break nextElement;
			case K_CONDITIONAL_OPERATOR :
				if(info == QUESTION) {
					if(expressionPtr > 0) {
						expressionPtr--;
						expressionLengthPtr--;
						expressionStack[expressionPtr] = expressionStack[expressionPtr+1];
						popElement(K_CONDITIONAL_OPERATOR);
						buildMoreCompletionContext(expression);
						return;
					}
				} else {
					if(expressionPtr > 1) {
						expressionPtr = expressionPtr - 2;
						expressionLengthPtr = expressionLengthPtr - 2;
						expressionStack[expressionPtr] = expressionStack[expressionPtr+2];
						popElement(K_CONDITIONAL_OPERATOR);
						buildMoreCompletionContext(expression);
						return;
					}
				}
				break nextElement;
			case K_BETWEEN_LEFT_AND_RIGHT_BRACKET :
				ArrayReference arrayReference;
				if(identifierPtr < 0 && expressionPtr > 0 && expressionStack[expressionPtr] == expression) {
					arrayReference =
						new ArrayReference(
							expressionStack[expressionPtr-1],
							expression);
				} else {
					arrayReference =
						new ArrayReference(
							getUnspecifiedReferenceOptimized(),
							expression);
				}
				assistNodeParent = arrayReference;
				break;
			case K_BETWEEN_CASE_AND_COLON :
				if(this.expressionPtr > 0) {
					SwitchStatement switchStatement = new SwitchStatement();
					switchStatement.expression = this.expressionStack[this.expressionPtr - 1];
					if(this.astLengthPtr > -1 && this.astPtr > -1) {
						int length = this.astLengthStack[this.astLengthPtr];
						int newAstPtr = this.astPtr - length;
						ASTNode firstNode = this.astStack[newAstPtr + 1];
						if(length != 0 && firstNode.sourceStart > switchStatement.expression.sourceEnd) {
							switchStatement.statements = new Statement[length + 1];
							System.arraycopy(
								this.astStack,
								newAstPtr + 1,
								switchStatement.statements,
								0,
								length);
						}
					}
					CaseStatement caseStatement = new CaseStatement(expression, expression.sourceStart, expression.sourceEnd);
					if(switchStatement.statements == null) {
						switchStatement.statements = new Statement[]{caseStatement};
					} else {
						switchStatement.statements[switchStatement.statements.length - 1] = caseStatement;
					}
					assistNodeParent = switchStatement;
				}
				break;
		}
	}
	if(assistNodeParent != null) {
		currentElement = currentElement.add((Statement)assistNodeParent, 0);
	} else {
		if(currentElement instanceof RecoveredField && !(currentElement instanceof RecoveredInitializer)
			&& ((RecoveredField) currentElement).fieldDeclaration.initialization == null) {

			assistNodeParent = ((RecoveredField) currentElement).fieldDeclaration;
			currentElement = currentElement.add(statement, 0);
		} else if(currentElement instanceof RecoveredLocalVariable
			&& ((RecoveredLocalVariable) currentElement).localDeclaration.initialization == null) {

			assistNodeParent = ((RecoveredLocalVariable) currentElement).localDeclaration;
			currentElement = currentElement.add(statement, 0);
		} else {
			currentElement = currentElement.add(expression, 0);
		}
	}
}
private void buildMoreGenericsCompletionContext(ASTNode node, boolean consumeTypeArguments) {
	int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
	if(kind != 0) {
		int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
		nextElement : switch (kind) {
			case K_BINARY_OPERATOR :
				int prevKind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1);
				switch (prevKind) {
					case K_PARAMETERIZED_ALLOCATION :
						if(this.invocationType == ALLOCATION || this.invocationType == QUALIFIED_ALLOCATION) {
							currentElement = currentElement.add((TypeReference)node, 0);
						}
						break nextElement;
					case K_PARAMETERIZED_METHOD_INVOCATION :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == 0) {
							currentElement = currentElement.add((TypeReference)node, 0);
							break nextElement;
						}
				}
				if(info == LESS && node instanceof TypeReference) {
					if(this.identifierLengthPtr > -1 && this.identifierLengthStack[this.identifierLengthPtr]!= 0) {
						TypeReference ref = this.getTypeReference(0);
						if(currentElement instanceof RecoveredType) {
							currentElement = currentElement.add(new CompletionOnFieldType(ref, false), 0);
						} else {
							currentElement = currentElement.add(ref, 0);
						}
					} else if (currentElement.enclosingMethod().methodDeclaration.isConstructor()) {
						currentElement = currentElement.add((TypeReference)node, 0);
					}
				}
				break;
		}
	}
}
private void buildMoreTryStatementCompletionContext(TypeReference exceptionRef) {
	if (this.astLengthPtr > -1 &&
			this.astPtr > 1 &&
			this.astStack[this.astPtr] instanceof Block &&
			this.astStack[this.astPtr - 1] instanceof Argument) {
		TryStatement tryStatement = new TryStatement();

		int newAstPtr = this.astPtr;

		int length = this.astLengthStack[this.astLengthPtr];
		Block[] bks = (tryStatement.catchBlocks = new Block[length + 1]);
		Argument[] args = (tryStatement.catchArguments = new Argument[length + 1]);
		if (length != 0) {
			while (length-- > 0) {
				bks[length] = (Block) this.astStack[newAstPtr--];
				bks[length].statements = null; // statements of catch block won't be used
				args[length] = (Argument) this.astStack[newAstPtr--];
			}
		}

		bks[bks.length - 1] = new Block(0);
		args[args.length - 1] = new Argument(FAKE_ARGUMENT_NAME,0,exceptionRef,0);

		tryStatement.tryBlock = (Block) this.astStack[newAstPtr--];

		assistNodeParent = tryStatement;

		currentElement.add(tryStatement, 0);
	} else if (this.astLengthPtr > -1 &&
			this.astPtr > -1 &&
			this.astStack[this.astPtr] instanceof Block) {
		TryStatement tryStatement = new TryStatement();

		int newAstPtr = this.astPtr;

		Block[] bks = (tryStatement.catchBlocks = new Block[1]);
		Argument[] args = (tryStatement.catchArguments = new Argument[1]);

		bks[0] = new Block(0);
		args[0] = new Argument(FAKE_ARGUMENT_NAME,0,exceptionRef,0);

		tryStatement.tryBlock = (Block) this.astStack[newAstPtr--];

		assistNodeParent = tryStatement;

		currentElement.add(tryStatement, 0);
	}else {
		currentElement = currentElement.add(exceptionRef, 0);
	}
}

public int bodyEnd(AbstractMethodDeclaration method){
	return cursorLocation;
}
public int bodyEnd(Initializer initializer){
	return cursorLocation;
}
/**
 * Checks if the completion is on the exception type of a catch clause.
 * Returns whether we found a completion node.
 */
private boolean checkCatchClause() {
	if ((topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CATCH_AND_RIGHT_PAREN) && this.identifierPtr > -1) {
		// NB: if the cursor is on the variable, then it has been reduced (so identifierPtr is -1),
		//     thus this can only be a completion on the type of the catch clause
		pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
		this.assistNode = getTypeReference(0);
		popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
		this.lastCheckPoint = this.assistNode.sourceEnd + 1;
		this.isOrphanCompletionNode = true;
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on the type following a 'new'.
 * Returns whether we found a completion node.
 */
private boolean checkClassInstanceCreation() {
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_NEW_AND_LEFT_BRACKET) {
		int length = identifierLengthStack[identifierLengthPtr];
		int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
		if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
			// no class instance creation with a parameterized type
			return true;
		}

		// completion on type inside an allocation expression

		TypeReference type;
		if (this.invocationType == ALLOCATION) {
			// non qualified allocation expression
			AllocationExpression allocExpr = new AllocationExpression();
			if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_INSIDE_THROW_STATEMENT
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == this.bracketDepth) {
				pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
				type = getTypeReference(0);
				popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
			} else {
				type = getTypeReference(0);
			}
			if(type instanceof CompletionOnSingleTypeReference) {
				((CompletionOnSingleTypeReference)type).isConstructorType = true;
			}
			allocExpr.type = type;
			allocExpr.sourceStart = type.sourceStart;
			allocExpr.sourceEnd = type.sourceEnd;
			pushOnExpressionStack(allocExpr);
			this.isOrphanCompletionNode = false;
		} else {
			// qualified allocation expression
			QualifiedAllocationExpression allocExpr = new QualifiedAllocationExpression();
			pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
			pushOnGenericsLengthStack(0);
			if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_INSIDE_THROW_STATEMENT
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == this.bracketDepth) {
				pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
				type = getTypeReference(0);
				popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
			} else {
				type = getTypeReference(0);
			}
			allocExpr.type = type;
			allocExpr.enclosingInstance = this.expressionStack[this.qualifier];
			allocExpr.sourceStart = this.intStack[this.intPtr--];
			allocExpr.sourceEnd = type.sourceEnd;
			this.expressionStack[this.qualifier] = allocExpr; // attach it now (it replaces the qualifier expression)
			this.isOrphanCompletionNode = false;
		}
		this.assistNode = type;
		this.lastCheckPoint = type.sourceEnd + 1;

		popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on the dot following an array type,
 * a primitive type or an primitive array type.
 * Returns whether we found a completion node.
 */
private boolean checkClassLiteralAccess() {
	if (this.identifierLengthPtr >= 1 && this.previousToken == TokenNameDOT) { // (NB: the top id length is 1 and it is for the completion identifier)
		int length;
		// if the penultimate id length is negative,
		// the completion is after a primitive type or a primitive array type
		if ((length = this.identifierLengthStack[this.identifierLengthPtr-1]) < 0) {
			// build the primitive type node
			int dim = this.isAfterArrayType() ? this.intStack[this.intPtr--] : 0;
			SingleTypeReference typeRef = (SingleTypeReference)TypeReference.baseTypeReference(-length, dim);
			typeRef.sourceStart = this.intStack[this.intPtr--];
			if (dim == 0) {
				typeRef.sourceEnd = this.intStack[this.intPtr--];
			} else {
				this.intPtr--;
				typeRef.sourceEnd = this.endPosition;
			}
			//typeRef.sourceEnd = typeRef.sourceStart + typeRef.token.length; // NB: It's ok to use the length of the token since it doesn't contain any unicode

			// find the completion identifier and its source positions
			char[] source = identifierStack[identifierPtr];
			long pos = this.identifierPositionStack[this.identifierPtr--];
			this.identifierLengthPtr--; // it can only be a simple identifier (so its length is one)

			// build the completion on class literal access node
			CompletionOnClassLiteralAccess access = new CompletionOnClassLiteralAccess(pos, typeRef);
			access.completionIdentifier = source;
			this.identifierLengthPtr--; // pop the length that was used to say it is a primitive type
			this.assistNode = access;
			this.isOrphanCompletionNode = true;
			return true;
		}

		// if the completion is after a regular array type
		if (isAfterArrayType()) {
			// find the completion identifier and its source positions
			char[] source = identifierStack[identifierPtr];
			long pos = this.identifierPositionStack[this.identifierPtr--];
			this.identifierLengthPtr--; // it can only be a simple identifier (so its length is one)

			// get the type reference
			pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
			pushOnGenericsLengthStack(0);

			TypeReference typeRef = getTypeReference(this.intStack[this.intPtr--]);

			// build the completion on class literal access node
			CompletionOnClassLiteralAccess access = new CompletionOnClassLiteralAccess(pos, typeRef);
			access.completionIdentifier = source;
			this.assistNode = access;
			this.isOrphanCompletionNode = true;
			return true;
		}

	}
	return false;
}
private boolean checkKeyword() {
	if (currentElement instanceof RecoveredUnit) {
//		RecoveredUnit unit = (RecoveredUnit) currentElement;
		int index = -1;
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;

			char[] ident = identifierStack[ptr];
			long pos = identifierPositionStack[ptr];

			char[][] keywords = new char[Keywords.COUNT][];
			int count = 0;
//			if(unit.typeCount == 0
//				&& lastModifiers == ClassFileConstants.AccDefault) {
//				keywords[count++] = Keywords.IMPORT;
//			}
//			if(unit.typeCount == 0
//				&& unit.importCount == 0
//				&& lastModifiers == ClassFileConstants.AccDefault
//				&& compilationUnit.currentPackage == null) {
//				keywords[count++] = Keywords.PACKAGE;
//			}
//			if((lastModifiers & ClassFileConstants.AccPublic) == 0) {
//				boolean hasNoPublicType = true;
//				for (int i = 0; i < unit.typeCount; i++) {
//					if((unit.types[i].typeDeclaration.modifiers & ClassFileConstants.AccPublic) != 0) {
//						hasNoPublicType = false;
//					}
//				}
//				if(hasNoPublicType) {
//					keywords[count++] = Keywords.PUBLIC;
//				}
//			}
//			if((lastModifiers & ClassFileConstants.AccAbstract) == 0
//				&& (lastModifiers & ClassFileConstants.AccFinal) == 0) {
//				keywords[count++] = Keywords.ABSTRACT;
//			}
//			if((lastModifiers & ClassFileConstants.AccAbstract) == 0
//				&& (lastModifiers & ClassFileConstants.AccFinal) == 0) {
//				keywords[count++] = Keywords.FINAL;
//			}
//
//			keywords[count++] = Keywords.CLASS;
//
//			if((lastModifiers & ClassFileConstants.AccFinal) == 0) {
//				keywords[count++] = Keywords.INTERFACE;
//			}
			if(count != 0) {
				System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);

				this.assistNode = new CompletionOnKeyword2(ident, pos, keywords);
				this.lastCheckPoint = assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private boolean checkInstanceofKeyword() {
	if(isInsideMethod()) {
		int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
		int index;
		if(kind != K_BLOCK_DELIMITER
			&& (index = indexOfAssistIdentifier()) > -1
			&& expressionPtr > -1
			&& expressionLengthStack[expressionPtr] == 1) {

			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			if(identifierStack[ptr].length > 0 && CharOperation.prefixEquals(identifierStack[ptr], Keywords.INSTANCEOF)) {
				this.assistNode = new CompletionOnKeyword3(
						identifierStack[ptr],
						identifierPositionStack[ptr],
						Keywords.INSTANCEOF);
				this.lastCheckPoint = assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
			if(identifierStack[ptr].length > 0 && CharOperation.prefixEquals(identifierStack[ptr], Keywords.TYPEOF)) {
				this.assistNode = new CompletionOnKeyword3(
						identifierStack[ptr],
						identifierPositionStack[ptr],
						Keywords.TYPEOF);
				this.lastCheckPoint = assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
/**
 * Checks if the completion is inside a method invocation or a constructor invocation.
 * Returns whether we found a completion node.
 */
private boolean checkInvocation() {
	Expression topExpression = this.expressionPtr >= 0 ?
		this.expressionStack[this.expressionPtr] :
		null;
	boolean isEmptyNameCompletion = false;
	boolean isEmptyAssistIdentifier = false;
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR_QUALIFIER
		&& ((isEmptyNameCompletion = topExpression == this.assistNode && this.isEmptyNameCompletion()) // eg. it is something like "this.fred([cursor]" but it is not something like "this.fred(1 + [cursor]"
			|| (isEmptyAssistIdentifier = this.indexOfAssistIdentifier() >= 0 && this.identifierStack[this.identifierPtr].length == 0))) { // eg. it is something like "this.fred(1 [cursor]"

		// pop empty name completion
		if (isEmptyNameCompletion) {
			this.expressionPtr--;
			this.expressionLengthStack[this.expressionLengthPtr]--;
		} else if (isEmptyAssistIdentifier) {
			this.identifierPtr--;
			this.identifierLengthPtr--;
		}

		// find receiver and qualifier
		int invocType = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1);
		int qualifierExprPtr = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);

		// find arguments
		int numArgs = this.expressionPtr - qualifierExprPtr;
		int argStart = qualifierExprPtr + 1;
		Expression[] arguments = null;
		if (numArgs > 0) {
			// remember the arguments
			arguments = new Expression[numArgs];
			System.arraycopy(this.expressionStack, argStart, arguments, 0, numArgs);

			// consume the expression arguments
			this.expressionPtr -= numArgs;
			int count = numArgs;
			while (count > 0) {
				count -= this.expressionLengthStack[this.expressionLengthPtr--];
			}
		}

		// build ast node
		if (invocType != ALLOCATION && invocType != QUALIFIED_ALLOCATION) {
			// creates completion on message send
			CompletionOnMessageSend messageSend = new CompletionOnMessageSend();
			messageSend.arguments = arguments;
			switch (invocType) {
				case NO_RECEIVER:
					// implicit this
					messageSend.receiver = ThisReference.implicitThis();
					break;
				case NAME_RECEIVER:
					// remove special flags for primitive types
					while (this.identifierLengthPtr >= 0 && this.identifierLengthStack[this.identifierLengthPtr] < 0) {
						this.identifierLengthPtr--;
					}

					// remove selector
					this.identifierPtr--;
					if(this.genericsPtr > -1 && this.genericsLengthPtr > -1 && this.genericsLengthStack[this.genericsLengthPtr] > 0) {
						// is inside a paremeterized method: bar.<X>.foo
						this.identifierLengthPtr--;
					} else {
						this.identifierLengthStack[this.identifierLengthPtr]--;
					}
					// consume the receiver
					messageSend.receiver = this.getUnspecifiedReference();
					break;
				case SUPER_RECEIVER:
					messageSend.receiver = new SuperReference(0, 0);
					break;
				case EXPLICIT_RECEIVER:
					messageSend.receiver = this.expressionStack[qualifierExprPtr];
			}

			// set selector
			int selectorPtr = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 2);
			messageSend.selector = this.identifierStack[selectorPtr];
			// remove selector
			if (this.identifierLengthPtr >=0 && this.identifierLengthStack[this.identifierLengthPtr] == 1) {
				this.identifierPtr--;
				this.identifierLengthPtr--;
			}

			// the entire message may be replaced in case qualification is needed
			messageSend.sourceStart = (int)(this.identifierPositionStack[selectorPtr] >> 32); //this.cursorLocation + 1;
			messageSend.sourceEnd = this.cursorLocation;

			// remember the message send as an orphan completion node
			this.assistNode = messageSend;
			this.lastCheckPoint = messageSend.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		} else {
			int selectorPtr = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 2);
			if (selectorPtr == THIS_CONSTRUCTOR || selectorPtr == SUPER_CONSTRUCTOR) {
				// creates an explicit constructor call
				CompletionOnExplicitConstructorCall call = new CompletionOnExplicitConstructorCall(ExplicitConstructorCall.This);
				call.arguments = arguments;
				if (invocType == QUALIFIED_ALLOCATION) {
					call.qualification = this.expressionStack[qualifierExprPtr];
				}

				// no source is going to be replaced
				call.sourceStart = this.cursorLocation + 1;
				call.sourceEnd = this.cursorLocation;

				// remember the explicit constructor call as an orphan completion node
				this.assistNode = call;
				this.lastCheckPoint = call.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			} else {
				// creates an allocation expression
				CompletionOnQualifiedAllocationExpression allocExpr = new CompletionOnQualifiedAllocationExpression();
				allocExpr.arguments = arguments;
				if(this.genericsLengthPtr < 0) {
					pushOnGenericsLengthStack(0);
					pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
				}
				allocExpr.type = super.getTypeReference(0); // we don't want a completion node here, so call super
				if (invocType == QUALIFIED_ALLOCATION) {
					allocExpr.enclosingInstance = this.expressionStack[qualifierExprPtr];
				}
				// no source is going to be replaced
				allocExpr.sourceStart = this.cursorLocation + 1;
				allocExpr.sourceEnd = this.cursorLocation;

				// remember the allocation expression as an orphan completion node
				this.assistNode = allocExpr;
				this.lastCheckPoint = allocExpr.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private boolean checkLabelStatement() {
	if(isInsideMethod() || isInsideFieldInitialization()) {

		int kind = this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
		if(kind != K_INSIDE_BREAK_STATEMENT && kind != K_INSIDE_CONTINUE_STATEMENT) return false;

		if (indexOfAssistIdentifier() != 0) return false;

		char[][] labels = new char[this.labelPtr + 1][];
		int labelCount = 0;

		int labelKind = kind;
		int index = 1;
		while(labelKind != 0 && labelKind != K_METHOD_DELIMITER) {
			labelKind = this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, index);
			if(labelKind == K_LABEL) {
				int ptr = this.topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, index);
				labels[labelCount++] = this.labelStack[ptr];
			}
			index++;
		}
		System.arraycopy(labels, 0, labels = new char[labelCount][], 0, labelCount);

		long position = this.identifierPositionStack[this.identifierPtr];
		CompletionOnBrankStatementLabel statementLabel =
			new CompletionOnBrankStatementLabel(
					kind == K_INSIDE_BREAK_STATEMENT ? CompletionOnBrankStatementLabel.BREAK : CompletionOnBrankStatementLabel.CONTINUE,
					this.identifierStack[this.identifierPtr--],
					(int) (position >>> 32),
					(int)position,
					labels);

		this.assistNode = statementLabel;
		this.lastCheckPoint = this.assistNode.sourceEnd + 1;
		this.isOrphanCompletionNode = true;
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on a member access (ie. in an identifier following a dot).
 * Returns whether we found a completion node.
 */
private boolean checkMemberAccess() {
	if (this.previousToken == TokenNameDOT && this.qualifier > -1 && this.expressionPtr == this.qualifier) {
		if (this.identifierLengthPtr > 1 && this.identifierLengthStack[this.identifierLengthPtr - 1] < 0) {
			// its not a  member access because the receiver is a base type
			// fix for bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=137623
			return false;
		}
		// the receiver is an expression
		pushCompletionOnMemberAccessOnExpressionStack(false);
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on a name reference.
 * Returns whether we found a completion node.
 */
private boolean checkNameCompletion() {
	/*
		We didn't find any other completion, but the completion identifier is on the identifier stack,
		so it can only be a completion on name.
		Note that we allow the completion on a name even if nothing is expected (eg. foo() b[cursor] would
		be a completion on 'b'). This policy gives more to the user than he/she would expect, but this
		simplifies the problem. To fix this, the recovery must be changed to work at a 'statement' granularity
		instead of at the 'expression' granularity as it does right now.
	*/

	// NB: at this point the completion identifier is on the identifier stack
	this.assistNode = getUnspecifiedReferenceOptimized();
	this.lastCheckPoint = this.assistNode.sourceEnd + 1;
	this.isOrphanCompletionNode = true;
	return true;
}
/**
 * Checks if the completion is in the context of a method and on the type of one of its arguments
 * Returns whether we found a completion node.
 */
private boolean checkRecoveredMethod() {
	if (currentElement instanceof RecoveredMethod){
		/* check if current awaiting identifier is the completion identifier */
		if (this.indexOfAssistIdentifier() < 0) return false;

		/* check if on line with an error already - to avoid completing inside
			illegal type names e.g.  int[<cursor> */
		if (lastErrorEndPosition <= cursorLocation+1
				&& Util.getLineNumber(lastErrorEndPosition, scanner.lineEnds, 0, scanner.linePtr)
				== Util.getLineNumber(((CompletionScanner)scanner).completedIdentifierStart, scanner.lineEnds, 0, scanner.linePtr)){
			return false;
		}
 		RecoveredMethod recoveredMethod = (RecoveredMethod)currentElement;
		/* only consider if inside method header */
		if (!recoveredMethod.foundOpeningBrace
			&& lastIgnoredToken == -1) {
			//if (rParenPos < lParenPos){ // inside arguments
			this.assistNode = this.getTypeReference(0);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		}
	}
	return false;
}
/**
 * Checks if the completion is in the context of a type and on a type reference in this type.
 * Persists the identifier into a fake field return type
 * Returns whether we found a completion node.
 */
private boolean checkRecoveredType() {
	if (currentElement instanceof RecoveredType){
		/* check if current awaiting identifier is the completion identifier */
		if (this.indexOfAssistIdentifier() < 0) return false;

		/* check if on line with an error already - to avoid completing inside
			illegal type names e.g.  int[<cursor> */
		if ((lastErrorEndPosition <= cursorLocation+1)
				&& Util.getLineNumber(lastErrorEndPosition, scanner.lineEnds, 0, scanner.linePtr)
				== Util.getLineNumber(((CompletionScanner)scanner).completedIdentifierStart, scanner.lineEnds, 0, scanner.linePtr)){
			return false;
		}
		RecoveredType recoveredType = (RecoveredType)currentElement;
		/* filter out cases where scanner is still inside type header */
		if (recoveredType.foundOpeningBrace) {
			// complete generics stack if necessary
			if((this.genericsIdentifiersLengthPtr < 0 && this.identifierPtr > -1)
					|| (this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr] <= this.identifierPtr)) {
				pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
				pushOnGenericsLengthStack(0); // handle type arguments
			}
			this.assistNode = this.getTypeReference(0);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		} else {
			if(recoveredType.typeDeclaration.superclass == null &&
					this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_EXTENDS_KEYWORD) {
				this.consumeClassOrInterfaceName();
				this.pushOnElementStack(K_NEXT_TYPEREF_IS_CLASS);
				this.assistNode = this.getTypeReference(0);
				this.popElement(K_NEXT_TYPEREF_IS_CLASS);
				this.lastCheckPoint = this.assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private void classHeaderExtendsOrImplements(boolean isInterface) {
	if (currentElement != null
			&& currentToken == TokenNameIdentifier
			&& this.cursorLocation+1 >= scanner.startPosition
			&& this.cursorLocation < scanner.currentPosition){
			this.pushIdentifier();
		int index = -1;
		/* check if current awaiting identifier is the completion identifier */
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			RecoveredType recoveredType = (RecoveredType)currentElement;
			/* filter out cases where scanner is still inside type header */
			if (!recoveredType.foundOpeningBrace) {
				TypeDeclaration type = recoveredType.typeDeclaration;
				if(!isInterface) {
					char[][] keywords = new char[Keywords.COUNT][];
					int count = 0;


					if(type.superclass == null) {
						keywords[count++] = Keywords.EXTENDS;
					}
					keywords[count++] = Keywords.IMPLEMENTS;

					System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);

					if(count > 0) {
						CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
							identifierStack[ptr],
							identifierPositionStack[ptr],
							keywords);
						completionOnKeyword.canCompleteEmptyToken = true;
						type.superclass = completionOnKeyword;
						type.superclass.bits |= ASTNode.IsSuperType;
						this.assistNode = completionOnKeyword;
						this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
					}
				} else {
					CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
						identifierStack[ptr],
						identifierPositionStack[ptr],
						Keywords.EXTENDS);
					completionOnKeyword.canCompleteEmptyToken = true;
					this.assistNode = completionOnKeyword;
					this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
				}
			}
		}
	}
}
/*
 * Check whether about to shift beyond the completion token.
 * If so, depending on the context, a special node might need to be created
 * and attached to the existing recovered structure so as to be remember in the
 * resulting parsed structure.
 */
public void completionIdentifierCheck(){
	//if (assistNode != null) return;

	if (checkKeyword()) return;
	if (checkRecoveredType()) return;
	if (checkRecoveredMethod()) return;

	// if not in a method in non diet mode and if not inside a field initializer, only record references attached to types
//	if (!(isInsideMethod() && !this.diet)
//		&& !isIndirectlyInsideFieldInitialization()
//		&& !isInsideAttributeValue()) return;

	/*
	 	In some cases, the completion identifier may not have yet been consumed,
	 	e.g.  int.[cursor]
	 	This is because the grammar does not allow any (empty) identifier to follow
	 	a base type. We thus have to manually force the identifier to be consumed
	 	(that is, pushed).
	 */
	if (assistIdentifier() == null && this.currentToken == TokenNameIdentifier) { // Test below copied from CompletionScanner.getCurrentIdentifierSource()
		if (cursorLocation < this.scanner.startPosition && this.scanner.currentPosition == this.scanner.startPosition){ // fake empty identifier got issued
			this.pushIdentifier();
		} else if (cursorLocation+1 >= this.scanner.startPosition && cursorLocation < this.scanner.currentPosition){
			this.pushIdentifier();
		}
	}

	// check for different scenarii
	// no need to go further if we found a non empty completion node
	// (we still need to store labels though)
	if (this.assistNode != null) {
		// however inside an invocation, the completion identifier may already have been consumed into an empty name
		// completion, so this check should be before we check that we are at the cursor location
		if (!isEmptyNameCompletion() || checkInvocation()) return;
	}

	// no need to check further if we are not at the cursor location
	if (this.indexOfAssistIdentifier() < 0) return;

	if (checkClassInstanceCreation()) return;
	if (checkCatchClause()) return;
	if (checkMemberAccess()) return;
	if (checkClassLiteralAccess()) return;
	if (checkInstanceofKeyword()) return;

	// if the completion was not on an empty name, it can still be inside an invocation (eg. this.fred("abc"[cursor])
	// (NB: Put this check before checkNameCompletion() because the selector of the invocation can be on the identifier stack)
	if (checkInvocation()) return;

	if (checkLabelStatement()) return;
	if (checkNameCompletion()) return;
}
protected void consumeArrayCreationHeader() {
	// nothing to do
}
protected void consumeAssignment() {
	popElement(K_ASSISGNMENT_OPERATOR);
	super.consumeAssignment();
}
protected void consumeAssignmentOperator(int pos) {
	super.consumeAssignmentOperator(pos);
	pushOnElementStack(K_ASSISGNMENT_OPERATOR, pos);
}
protected void consumeBinaryExpression(int op) {
	super.consumeBinaryExpression(op);
	popElement(K_BINARY_OPERATOR);

	if(expressionStack[expressionPtr] instanceof BinaryExpression) {
		BinaryExpression exp = (BinaryExpression) expressionStack[expressionPtr];
		if(assistNode != null && exp.right == assistNode) {
			assistNodeParent = exp;
		}
	}
}
protected void consumeCaseLabel() {
	super.consumeCaseLabel();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_SWITCH_LABEL) {
		pushOnElementStack(K_SWITCH_LABEL);
	}
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeCompilationUnit()
 */
protected void consumeCompilationUnit() {
	this.javadoc = null;
	checkComment();
	if (this.javadoc != null && this.cursorLocation > this.javadoc.sourceStart && this.cursorLocation < this.javadoc.sourceEnd) {
		// completion is in an orphan javadoc comment => replace compilation unit one to allow completion resolution
		compilationUnit.javadoc = this.javadoc;
		// create a fake interface declaration to allow resolution
		if (this.compilationUnit.types == null) {
			this.compilationUnit.types = new TypeDeclaration[1];
			TypeDeclaration declaration = new TypeDeclaration(compilationUnit.compilationResult);
			declaration.name = FAKE_TYPE_NAME;
			declaration.modifiers = ClassFileConstants.AccDefault;
			this.compilationUnit.types[0] = declaration;
		}
	}
	super.consumeCompilationUnit();
}

protected void consumeConditionalExpression(int op) {
	popElement(K_CONDITIONAL_OPERATOR);
	super.consumeConditionalExpression(op);
}
protected void consumeDefaultLabel() {
	super.consumeDefaultLabel();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL) {
		popElement(K_SWITCH_LABEL);
	}
	pushOnElementStack(K_SWITCH_LABEL, DEFAULT);
}
protected void consumeDimWithOrWithOutExpr() {
	// DimWithOrWithOutExpr ::= '[' ']'
	pushOnExpressionStack(null);
}
protected void consumeEnterVariable() {
	identifierPtr--;
	identifierLengthPtr--;

	boolean isLocalDeclaration = true;//nestedMethod[nestedType] != 0;
	int variableIndex = variablesCounter[nestedType];
	int extendedDimension = intStack[intPtr + 1];

	if(isLocalDeclaration || indexOfAssistIdentifier() < 0 || variableIndex != 0 || extendedDimension != 0) {
		identifierPtr++;
		identifierLengthPtr++;
		super.consumeEnterVariable();
	} else {
		restartRecovery = AssistParser.STOP_AT_CURSOR;

		// recovery
		if (currentElement != null) {
			if(!checkKeyword() && !(currentElement instanceof RecoveredUnit && ((RecoveredUnit)currentElement).statementCount == 0)) {
				int nameSourceStart = (int)(identifierPositionStack[identifierPtr] >>> 32);
				intPtr--;
//				pushOnGenericsIdentifiersLengthStack(identifierLengthStack[identifierLengthPtr]);
//				pushOnGenericsLengthStack(0);
				TypeReference type = getTypeReference(intStack[intPtr--]);
				intPtr--;

				if (!(currentElement instanceof RecoveredType)
					&& (currentToken == TokenNameDOT
							|| (Util.getLineNumber(type.sourceStart, scanner.lineEnds, 0, scanner.linePtr)
									!= Util.getLineNumber(nameSourceStart, scanner.lineEnds, 0, scanner.linePtr)))){
					lastCheckPoint = nameSourceStart;
					restartRecovery = true;
					return;
				}

				FieldDeclaration completionFieldDecl = new CompletionOnFieldType(type, false);
				completionFieldDecl.modifiers = intStack[intPtr--];
				assistNode = completionFieldDecl;
				lastCheckPoint = type.sourceEnd + 1;
				currentElement = currentElement.add(completionFieldDecl, 0);
				lastIgnoredToken = -1;
			}
		}
	}
}
protected void consumeEqualityExpression(int op) {
	super.consumeEqualityExpression(op);
	popElement(K_BINARY_OPERATOR);

	BinaryExpression exp = (BinaryExpression) expressionStack[expressionPtr];
	if(assistNode != null && exp.right == assistNode) {
		assistNodeParent = exp;
	}
}
protected void consumeExitVariableWithInitialization() {
	super.consumeExitVariableWithInitialization();

	// does not keep the initialization if completion is not inside
	AbstractVariableDeclaration variable = (AbstractVariableDeclaration) astStack[astPtr];
	if (cursorLocation + 1 < variable.initialization.sourceStart ||
		cursorLocation > variable.initialization.sourceEnd) {
		if (STOP_AT_CURSOR)
			variable.initialization = null;
	} else if (assistNode != null && assistNode == variable.initialization) {
		assistNodeParent = variable;
	}
}
protected void consumeCallExpressionWithSimpleName() {
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeCallExpressionWithSimpleName();
	} else {
		this.pushCompletionOnMemberAccessOnExpressionStack(false);
	}
}
protected void consumeMemberExpressionWithSimpleName() {
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeMemberExpressionWithSimpleName();
	} else {
		this.pushCompletionOnMemberAccessOnExpressionStack(false);
	}
}
protected void consumeForceNoDiet() {
	super.consumeForceNoDiet();
	if (isInsideMethod()) {
		pushOnElementStack(K_LOCAL_INITIALIZER_DELIMITER);
	}
}
protected void consumeFormalParameter(boolean isVarArgs) {
	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeFormalParameter(isVarArgs);
	} else {

		identifierLengthPtr--;
		char[] identifierName = identifierStack[identifierPtr];
		long namePositions = identifierPositionStack[identifierPtr--];
//		int extendedDimensions = this.intStack[this.intPtr--];
//		int endOfEllipsis = 0;
//		if (isVarArgs) {
//			endOfEllipsis = this.intStack[this.intPtr--];
//		}
//		int firstDimensions = this.intStack[this.intPtr--];
//		final int typeDimensions = firstDimensions + extendedDimensions;
//		TypeReference type = getTypeReference(typeDimensions);
//		if (isVarArgs) {
//			type = copyDims(type, typeDimensions + 1);
//			if (extendedDimensions == 0) {
//				type.sourceEnd = endOfEllipsis;
//			}
//			type.bits |= ASTNode.IsVarArgs; // set isVarArgs
//		}
		intPtr -= 2;
		CompletionOnArgumentName arg =
			new CompletionOnArgumentName(
				identifierName,
				namePositions,
				null,
				intStack[intPtr + 1] & ~ClassFileConstants.AccDeprecated); // modifiers
		// consume annotations
//		int length;
//		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
//			System.arraycopy(
//				this.expressionStack,
//				(this.expressionPtr -= length) + 1,
//				arg.annotations = new Annotation[length],
//				0,
//				length);
//		}

		arg.isCatchArgument = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CATCH_AND_RIGHT_PAREN;
		pushOnAstStack(arg);

		assistNode = arg;
		this.lastCheckPoint = (int) namePositions;
		isOrphanCompletionNode = true;

		/* if incomplete method header, listLength counter will not have been reset,
			indicating that some arguments are available on the stack */
		listLength++;
	}
}
protected void consumeInsideCastExpression() {
	int end = intStack[intPtr--];
	boolean isParameterized =(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_CAST);
	if(isParameterized) {
		popElement(K_PARAMETERIZED_CAST);

		if(this.identifierLengthStack[this.identifierLengthPtr] > 0) {
			pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
		}
	} else {
		if(this.identifierLengthStack[this.identifierLengthPtr] > 0) {
			pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
			pushOnGenericsLengthStack(0);
		}
	}
	Expression castType = getTypeReference(intStack[intPtr--]);
	if(isParameterized) {
		intPtr--;
	}
	castType.sourceEnd = end - 1;
	castType.sourceStart = intStack[intPtr--] + 1;
	pushOnExpressionStack(castType);

	pushOnElementStack(K_CAST_STATEMENT);
}
protected void consumeCallExpressionWithArguments() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeCallExpressionWithArguments();
}
protected void consumeMethodHeaderName(boolean isAnnotationMethod) {
	if(this.indexOfAssistIdentifier() < 0) {
		identifierPtr--;
		identifierLengthPtr--;
		if(this.indexOfAssistIdentifier() != 0 ||
			this.identifierLengthStack[this.identifierLengthPtr] != this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr]) {
			identifierPtr++;
			identifierLengthPtr++;
			super.consumeMethodHeaderName(isAnnotationMethod);
		} else {
			restartRecovery = AssistParser.STOP_AT_CURSOR;

			// recovery
			if (currentElement != null) {
				//name
//				char[] selector = identifierStack[identifierPtr + 1];
//				long selectorSource = identifierPositionStack[identifierPtr + 1];

//				//type
//				TypeReference type = getTypeReference(intStack[intPtr--]);
//				((CompletionOnSingleTypeReference)type).isCompletionNode = false;
//				//modifiers
//				int declarationSourceStart = intStack[intPtr--];
//				int mod = intStack[intPtr--];

//				if(Util.getLineNumber(type.sourceStart, scanner.lineEnds, 0, scanner.linePtr)
//						!= Util.getLineNumber((int) (selectorSource >>> 32), scanner.lineEnds, 0, scanner.linePtr)) {
//					FieldDeclaration completionFieldDecl = new CompletionOnFieldType(type, false);
//					// consume annotations
//					int length;
//					if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
//						System.arraycopy(
//							this.expressionStack,
//							(this.expressionPtr -= length) + 1,
//							completionFieldDecl.annotations = new Annotation[length],
//							0,
//							length);
//					}
//					completionFieldDecl.modifiers = mod;
//					assistNode = completionFieldDecl;
//					lastCheckPoint = type.sourceEnd + 1;
//					currentElement = currentElement.add(completionFieldDecl, 0);
//					lastIgnoredToken = -1;
//				} else {
//					CompletionOnMethodReturnType md = new CompletionOnMethodReturnType(type, this.compilationUnit.compilationResult);
//					// consume annotations
//					int length;
//					if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
//						System.arraycopy(
//							this.expressionStack,
//							(this.expressionPtr -= length) + 1,
//							md.annotations = new Annotation[length],
//							0,
//							length);
//					}
//					md.selector = selector;
//					md.declarationSourceStart = declarationSourceStart;
//					md.modifiers = mod;
//					md.bodyStart = lParenPos+1;
//					listLength = 0; // initialize listLength before reading parameters/throws
//					assistNode = md;
//					this.lastCheckPoint = md.bodyStart;
//					currentElement = currentElement.add(md, 0);
//					lastIgnoredToken = -1;
//					// javadoc
//					md.javadoc = this.javadoc;
//					this.javadoc = null;
//				}
			}
		}
	} else {
		// MethodHeaderName ::= Modifiersopt Type 'Identifier' '('
		CompletionOnMethodName md = new CompletionOnMethodName(this.compilationUnit.compilationResult);

		//name
		md.setSelector(identifierStack[identifierPtr]);
		long selectorSource = identifierPositionStack[identifierPtr--];
		identifierLengthPtr--;
		//type
		md.returnType = getTypeReference(intStack[intPtr--]);
		//modifiers
		md.declarationSourceStart = intStack[intPtr--];
		md.modifiers = intStack[intPtr--];
		this.expressionLengthPtr--;
		// javadoc
		md.javadoc = this.javadoc;
		this.javadoc = null;

		//highlight starts at selector start
		md.sourceStart = (int) (selectorSource >>> 32);
		md.selectorEnd = (int) selectorSource;
		pushOnAstStack(md);
		md.sourceEnd = lParenPos;
		md.bodyStart = lParenPos+1;
		listLength = 0; // initialize listLength before reading parameters/throws

		this.assistNode = md;
		this.lastCheckPoint = md.sourceEnd;
		// recovery
		if (currentElement != null){
			if (currentElement instanceof RecoveredType
				//|| md.modifiers != 0
					|| (Util.getLineNumber(md.returnType.sourceStart, scanner.lineEnds, 0, scanner.linePtr)
							== Util.getLineNumber(md.sourceStart, scanner.lineEnds, 0, scanner.linePtr))){
				lastCheckPoint = md.bodyStart;
				currentElement = currentElement.add(md, 0);
				lastIgnoredToken = -1;
			} else {
				lastCheckPoint = md.sourceStart;
				restartRecovery = AssistParser.STOP_AT_CURSOR;
			}
		}
	}
}
protected void consumeMethodHeaderRightParen() {
	super.consumeMethodHeaderRightParen();

	if (currentElement != null
		&& currentToken == TokenNameIdentifier
		&& this.cursorLocation+1 >= scanner.startPosition
		&& this.cursorLocation < scanner.currentPosition){
		this.pushIdentifier();

		int index = -1;
		/* check if current awaiting identifier is the completion identifier */
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			if (currentElement instanceof RecoveredMethod){
				RecoveredMethod recoveredMethod = (RecoveredMethod)currentElement;
				/* filter out cases where scanner is still inside type header */
				if (!recoveredMethod.foundOpeningBrace) {
					CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
						identifierStack[ptr],
						identifierPositionStack[ptr],
						Keywords.THROWS);
					recoveredMethod.foundOpeningBrace = true;
					this.assistNode = completionOnKeyword;
					this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
				}
			}
		}
	}
}
protected void consumeLabel() {
	super.consumeLabel();
	this.pushOnLabelStack(this.identifierStack[this.identifierPtr]);
	this.pushOnElementStack(K_LABEL, this.labelPtr);
}
protected void consumeMethodBody() {
	popElement(K_BLOCK_DELIMITER);
	super.consumeMethodBody();
}
protected void consumeMethodHeader() {
	super.consumeMethodHeader();
	pushOnElementStack(K_BLOCK_DELIMITER);
}
protected void consumeRestoreDiet() {
	super.consumeRestoreDiet();
	if (isInsideMethod()) {
		popElement(K_LOCAL_INITIALIZER_DELIMITER);
	}
}
protected void consumeStatementBreakWithLabel() {
	super.consumeStatementBreakWithLabel();
	if (this.record) {
		ASTNode breakStatement = this.astStack[this.astPtr];
		if (!isAlreadyPotentialName(breakStatement.sourceStart)) {
			this.addPotentialName(null, breakStatement.sourceStart, breakStatement.sourceEnd);
		}
	}

}

protected void consumeStatementLabel() {
	this.popElement(K_LABEL);
	super.consumeStatementLabel();
}
protected void consumeStatementSwitch() {
	super.consumeStatementSwitch();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL) {
		popElement(K_SWITCH_LABEL);
		popElement(K_BLOCK_DELIMITER);
	}
}
protected void consumeNestedMethod() {
	super.consumeNestedMethod();
	if(!(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER)) pushOnElementStack(K_BLOCK_DELIMITER);
}
protected void consumePushPosition() {
	super.consumePushPosition();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BINARY_OPERATOR) {
		int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
		popElement(K_BINARY_OPERATOR);
		pushOnElementStack(K_UNARY_OPERATOR, info);
	}
}
protected void consumeToken(int token) {
	if(isFirst) {
		super.consumeToken(token);
		return;
	}
	if(canBeExplicitConstructor == NEXTTOKEN) {
		canBeExplicitConstructor = YES;
	} else {
		canBeExplicitConstructor = NO;
	}

	int previous = this.previousToken;
	int prevIdentifierPtr = this.previousIdentifierPtr;

	if (isInsideMethod() || isInsideFieldInitialization() || isInsideAnnotation()) {
		switch(token) {
			case TokenNameLPAREN:
				if(previous == TokenNameIdentifier &&
						topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_METHOD_INVOCATION) {
					popElement(K_PARAMETERIZED_METHOD_INVOCATION);
				} else {
					popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
				}
				break;
			case TokenNameLBRACE:
				popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
				break;
			case TokenNameLBRACKET:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_NEW_AND_LEFT_BRACKET) {
					popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
					pushOnElementStack(K_ARRAY_CREATION);
				}
				break;
			case TokenNameRBRACE:
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				switch (kind) {
					case K_BLOCK_DELIMITER:
						popElement(K_BLOCK_DELIMITER);
						break;
					case K_MEMBER_VALUE_ARRAY_INITIALIZER:
						popElement(K_MEMBER_VALUE_ARRAY_INITIALIZER);
						break;
					default:
						popElement(K_ARRAY_INITIALIZER);
						break;
				}
				break;
			case TokenNameRBRACKET:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_LEFT_AND_RIGHT_BRACKET) {
					popElement(K_BETWEEN_LEFT_AND_RIGHT_BRACKET);
				}
				break;

		}
	}
	super.consumeToken(token);

	// if in field initializer (directly or not), on the completion identifier and not in recovery mode yet
	// then position end of file at cursor location (so that we have the same behavior as
	// in method bodies)
	if (token == TokenNameIdentifier
			&& this.identifierStack[this.identifierPtr] == assistIdentifier()
			&& this.currentElement == null
			&& this.isIndirectlyInsideFieldInitialization()) {
//		this.scanner.eofPosition = cursorLocation < Integer.MAX_VALUE ? cursorLocation+1 : cursorLocation;
	}

	// if in a method or if in a field initializer
	if (isInsideMethod() || isInsideFieldInitialization() || isInsideAttributeValue()) {
		switch (token) {
			case TokenNameDOT:
				switch (previous) {
					case TokenNamethis: // eg. this[.]fred()
						this.invocationType = EXPLICIT_RECEIVER;
						break;
					case TokenNamesuper: // eg. super[.]fred()
						this.invocationType = SUPER_RECEIVER;
						break;
					case TokenNameIdentifier: // eg. bar[.]fred()
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_BETWEEN_NEW_AND_LEFT_BRACKET) {
							if (this.identifierPtr != prevIdentifierPtr) { // if identifier has been consumed, eg. this.x[.]fred()
								this.invocationType = EXPLICIT_RECEIVER;
							} else {
								this.invocationType = NAME_RECEIVER;
							}
						}
						break;
				}
				break;
			case TokenNameIdentifier:
				if (previous == TokenNameDOT) { // eg. foo().[fred]()
					if (this.invocationType != SUPER_RECEIVER // eg. not super.[fred]()
						&& this.invocationType != NAME_RECEIVER // eg. not bar.[fred]()
						&& this.invocationType != ALLOCATION // eg. not new foo.[Bar]()
						&& this.invocationType != QUALIFIED_ALLOCATION) { // eg. not fred().new foo.[Bar]()

						this.invocationType = EXPLICIT_RECEIVER;
						this.qualifier = this.expressionPtr;
					}
				}
				break;
			case TokenNamenew:
				pushOnElementStack(K_BETWEEN_NEW_AND_LEFT_BRACKET);
				this.qualifier = this.expressionPtr; // NB: even if there is no qualification, set it to the expression ptr so that the number of arguments are correctly computed
				if (previous == TokenNameDOT) { // eg. fred().[new] X()
					this.invocationType = QUALIFIED_ALLOCATION;
				} else { // eg. [new] X()
					this.invocationType = ALLOCATION;
				}
				break;
			case TokenNamethis:
				if (previous == TokenNameDOT) { // eg. fred().[this]()
					this.invocationType = QUALIFIED_ALLOCATION;
					this.qualifier = this.expressionPtr;
				}
				break;
			case TokenNamesuper:
				if (previous == TokenNameDOT) { // eg. fred().[super]()
					this.invocationType = QUALIFIED_ALLOCATION;
					this.qualifier = this.expressionPtr;
				}
				break;
			case TokenNamecatch:
				pushOnElementStack(K_BETWEEN_CATCH_AND_RIGHT_PAREN);
				break;
			case TokenNameLPAREN:
				if (this.invocationType == NO_RECEIVER || this.invocationType == NAME_RECEIVER || this.invocationType == SUPER_RECEIVER) {
					this.qualifier = this.expressionPtr; // remenber the last expression so that arguments are correctly computed
				}
				switch (previous) {
					case TokenNameIdentifier: // eg. fred[(]) or foo.fred[(])
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							int info = 0;
							if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER,1) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN &&
									(info=topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER,1) & LPAREN_NOT_CONSUMED) != 0) {
								this.popElement(K_SELECTOR);
								this.popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
								if ((info & ANNOTATION_NAME_COMPLETION) != 0) {
									this.pushOnElementStack(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN, LPAREN_CONSUMED | ANNOTATION_NAME_COMPLETION);
								} else {
								this.pushOnElementStack(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN, LPAREN_CONSUMED);
								}							} else {
								this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, this.invocationType);
								int selectorQualifier=(this.invocationType==ALLOCATION)?this.expressionPtr:this.qualifier;
								this.pushOnElementStack(K_SELECTOR_QUALIFIER, selectorQualifier);
							}
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
					case TokenNamethis: // explicit constructor invocation, eg. this[(]1, 2)
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, (this.invocationType == QUALIFIED_ALLOCATION) ? QUALIFIED_ALLOCATION : ALLOCATION);
							this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
					case TokenNamesuper: // explicit constructor invocation, eg. super[(]1, 2)
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, (this.invocationType == QUALIFIED_ALLOCATION) ? QUALIFIED_ALLOCATION : ALLOCATION);
							this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
					case TokenNameGREATER: // explicit constructor invocation, eg. Fred<X>[(]1, 2)
					case TokenNameRIGHT_SHIFT: // or fred<X<X>>[(]1, 2)
					case TokenNameUNSIGNED_RIGHT_SHIFT: //or Fred<X<X<X>>>[(]1, 2)
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_BINARY_OPERATOR &&
									topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == GREATER) {
								// it's not a selector invocation
								popElement(K_SELECTOR);
							} else {
							this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, (this.invocationType == QUALIFIED_ALLOCATION) ? QUALIFIED_ALLOCATION : ALLOCATION);
							this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
							}
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
				}
				break;
			case TokenNameLBRACE:
				this.bracketDepth++;
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				if(kind == K_FIELD_INITIALIZER_DELIMITER
					|| kind == K_LOCAL_INITIALIZER_DELIMITER
					|| kind == K_ARRAY_CREATION) {
					pushOnElementStack(K_ARRAY_INITIALIZER, endPosition);
				} else if (kind == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN) {
					pushOnElementStack(K_MEMBER_VALUE_ARRAY_INITIALIZER, endPosition);
				} else {
					switch(previous) {
						case TokenNameRPAREN :
							switch(previousKind) {
								case K_BETWEEN_IF_AND_RIGHT_PAREN :
									pushOnElementStack(K_BLOCK_DELIMITER, IF);
									break;
								case K_BETWEEN_CATCH_AND_RIGHT_PAREN :
									pushOnElementStack(K_BLOCK_DELIMITER, CATCH);
									break;
								case K_BETWEEN_WHILE_AND_RIGHT_PAREN :
									pushOnElementStack(K_BLOCK_DELIMITER, WHILE);
									break;
								case K_BETWEEN_SWITCH_AND_RIGHT_PAREN :
									pushOnElementStack(K_BLOCK_DELIMITER, SWITCH);
									break;
								case K_BETWEEN_FOR_AND_RIGHT_PAREN :
									pushOnElementStack(K_BLOCK_DELIMITER, FOR);
									break;
								case K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN :
									pushOnElementStack(K_BLOCK_DELIMITER, SYNCHRONIZED);
									break;
								default :
									pushOnElementStack(K_BLOCK_DELIMITER);
									break;
							}
							break;
						case TokenNametry :
							pushOnElementStack(K_BLOCK_DELIMITER, TRY);
							break;
						case TokenNamedo:
							pushOnElementStack(K_BLOCK_DELIMITER, DO);
							break;
						default :
							pushOnElementStack(K_BLOCK_DELIMITER);
							break;
					}
				}
				break;
			case TokenNameLBRACKET:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_ARRAY_CREATION) {
					pushOnElementStack(K_BETWEEN_LEFT_AND_RIGHT_BRACKET);
				} else {
					if(previous == TokenNameIdentifier) {
						invocationType = NO_RECEIVER;
						qualifier = -1;
					}
				}
				this.bracketDepth++;
				break;
			case TokenNameRBRACE:
				this.bracketDepth--;
				break;
			case TokenNameRBRACKET:
				this.bracketDepth--;
				break;
			case TokenNameRPAREN:
				switch(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					case K_BETWEEN_CATCH_AND_RIGHT_PAREN :
						popElement(K_BETWEEN_CATCH_AND_RIGHT_PAREN);
						break;
					case K_BETWEEN_IF_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == bracketDepth) {
							popElement(K_BETWEEN_IF_AND_RIGHT_PAREN);
						}
						break;
					case K_BETWEEN_WHILE_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == bracketDepth) {
							popElement(K_BETWEEN_WHILE_AND_RIGHT_PAREN);
						}
						break;
					case K_BETWEEN_FOR_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == bracketDepth) {
							popElement(K_BETWEEN_FOR_AND_RIGHT_PAREN);
						}
						break;
					case K_BETWEEN_SWITCH_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == bracketDepth) {
							popElement(K_BETWEEN_SWITCH_AND_RIGHT_PAREN);
						}
						break;
					case K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == bracketDepth) {
							popElement(K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN);
						}
						break;
				}
				break;
			case TokenNamethrow:
				pushOnElementStack(K_INSIDE_THROW_STATEMENT, bracketDepth);
				break;
			case TokenNameSEMICOLON:
				switch(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					case K_INSIDE_THROW_STATEMENT :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_THROW_STATEMENT);
						}
						break;
					case K_INSIDE_RETURN_STATEMENT :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_RETURN_STATEMENT);
						}
						break;
					case K_INSIDE_ASSERT_STATEMENT :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_ASSERT_STATEMENT);
						}
						break;
					case K_INSIDE_BREAK_STATEMENT:
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_BREAK_STATEMENT);
						}
						break;
					case K_INSIDE_CONTINUE_STATEMENT:
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_CONTINUE_STATEMENT);
						}
						break;
				}
				break;
			case TokenNamereturn:
				pushOnElementStack(K_INSIDE_RETURN_STATEMENT, this.bracketDepth);
				break;
			case TokenNameMULTIPLY:
				pushOnElementStack(K_BINARY_OPERATOR, MULTIPLY);
				break;
			case TokenNameDIVIDE:
				pushOnElementStack(K_BINARY_OPERATOR, DIVIDE);
				break;
			case TokenNameREMAINDER:
				pushOnElementStack(K_BINARY_OPERATOR, REMAINDER);
				break;
			case TokenNamePLUS:
				pushOnElementStack(K_BINARY_OPERATOR, PLUS);
				break;
			case TokenNameMINUS:
				pushOnElementStack(K_BINARY_OPERATOR, MINUS);
				break;
			case TokenNameLEFT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, LEFT_SHIFT);
				break;
			case TokenNameRIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, RIGHT_SHIFT);
				break;
			case TokenNameUNSIGNED_RIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, UNSIGNED_RIGHT_SHIFT);
				break;
			case TokenNameLESS:
				switch(previous) {
					case TokenNameDOT :
						pushOnElementStack(K_PARAMETERIZED_METHOD_INVOCATION);
						break;
					case TokenNamenew :
						pushOnElementStack(K_PARAMETERIZED_ALLOCATION);
						break;
				}
				pushOnElementStack(K_BINARY_OPERATOR, LESS);
				break;
			case TokenNameGREATER:
				pushOnElementStack(K_BINARY_OPERATOR, GREATER);
				break;
			case TokenNameLESS_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, LESS_EQUAL);
				break;
			case TokenNameGREATER_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, GREATER_EQUAL);
				break;
			case TokenNameAND:
				pushOnElementStack(K_BINARY_OPERATOR, AND);
				break;
			case TokenNameXOR:
				pushOnElementStack(K_BINARY_OPERATOR, XOR);
				break;
			case TokenNameOR:
				pushOnElementStack(K_BINARY_OPERATOR, OR);
				break;
			case TokenNameAND_AND:
				pushOnElementStack(K_BINARY_OPERATOR, AND_AND);
				break;
			case TokenNameOR_OR:
				pushOnElementStack(K_BINARY_OPERATOR, OR_OR);
				break;
			case TokenNamePLUS_PLUS:
				pushOnElementStack(K_UNARY_OPERATOR, PLUS_PLUS);
				break;
			case TokenNameMINUS_MINUS:
				pushOnElementStack(K_UNARY_OPERATOR, MINUS_MINUS);
				break;
			case TokenNameTWIDDLE:
				pushOnElementStack(K_UNARY_OPERATOR, TWIDDLE);
				break;
			case TokenNameNOT:
				pushOnElementStack(K_UNARY_OPERATOR, NOT);
				break;
			case TokenNameEQUAL_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, EQUAL_EQUAL);
				break;
			case TokenNameNOT_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, NOT_EQUAL);
				break;
			case TokenNameinstanceof:
				pushOnElementStack(K_BINARY_OPERATOR, INSTANCEOF);
				break;
			case TokenNameQUESTION:
				if(previous != TokenNameLESS) {
					pushOnElementStack(K_CONDITIONAL_OPERATOR, QUESTION);
				}
				break;
			case TokenNameCOLON:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONDITIONAL_OPERATOR
					&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == QUESTION) {
					popElement(K_CONDITIONAL_OPERATOR);
					pushOnElementStack(K_CONDITIONAL_OPERATOR, COLON);
				} else {
					if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CASE_AND_COLON) {
						popElement(K_BETWEEN_CASE_AND_COLON);
					} else {
						popElement(K_BETWEEN_DEFAULT_AND_COLON);
					}
				}
				break;
			case TokenNameif:
				pushOnElementStack(K_BETWEEN_IF_AND_RIGHT_PAREN, bracketDepth);
				break;
			case TokenNamewhile:
				pushOnElementStack(K_BETWEEN_WHILE_AND_RIGHT_PAREN, bracketDepth);
				break;
			case TokenNamefor:
				pushOnElementStack(K_BETWEEN_FOR_AND_RIGHT_PAREN, bracketDepth);
				break;
			case TokenNameswitch:
				pushOnElementStack(K_BETWEEN_SWITCH_AND_RIGHT_PAREN, bracketDepth);
				break;
			case TokenNamesynchronized:
				pushOnElementStack(K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN, bracketDepth);
				break;
//			case TokenNameassert:
//				pushOnElementStack(K_INSIDE_ASSERT_STATEMENT, this.bracketDepth);
//				break;
			case TokenNamecase :
				pushOnElementStack(K_BETWEEN_CASE_AND_COLON);
				break;
			case TokenNamedefault :
				pushOnElementStack(K_BETWEEN_DEFAULT_AND_COLON);
				break;
			case TokenNameextends:
				pushOnElementStack(K_EXTENDS_KEYWORD);
				break;
			case TokenNamebreak:
				pushOnElementStack(K_INSIDE_BREAK_STATEMENT, bracketDepth);
				break;
			case TokenNamecontinue:
				pushOnElementStack(K_INSIDE_CONTINUE_STATEMENT, bracketDepth);
				break;
		}
	} else {
		switch(token) {
			case TokenNameextends:
				pushOnElementStack(K_EXTENDS_KEYWORD);
				break;
			case TokenNameLESS:
				pushOnElementStack(K_BINARY_OPERATOR, LESS);
				break;
			case TokenNameGREATER:
				pushOnElementStack(K_BINARY_OPERATOR, GREATER);
				break;
			case TokenNameRIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, RIGHT_SHIFT);
				break;
			case TokenNameUNSIGNED_RIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, UNSIGNED_RIGHT_SHIFT);
				break;

		}
	}
}
protected void consumeRightParen() {
	super.consumeRightParen();
}
protected void consumeUnaryExpression(int op) {
	super.consumeUnaryExpression(op);
	popElement(K_UNARY_OPERATOR);

	if(expressionStack[expressionPtr] instanceof UnaryExpression) {
		UnaryExpression exp = (UnaryExpression) expressionStack[expressionPtr];
		if(assistNode != null && exp.expression == assistNode) {
			assistNodeParent = exp;
		}
	}
}
protected void consumeUnaryExpression(int op, boolean post) {
	super.consumeUnaryExpression(op, post);
	popElement(K_UNARY_OPERATOR);

	if(expressionStack[expressionPtr] instanceof UnaryExpression) {
		UnaryExpression exp = (UnaryExpression) expressionStack[expressionPtr];
		if(assistNode != null && exp.expression == assistNode) {
			assistNodeParent = exp;
		}
	}
}

public ImportReference createAssistImportReference(char[][] tokens, long[] positions){
	return new CompletionOnImportReference(tokens, positions);
}
public NameReference createQualifiedAssistNameReference(char[][] previousIdentifiers, char[] assistName, long[] positions){
	return new CompletionOnQualifiedNameReference(
					previousIdentifiers,
					assistName,
					positions,
					isInsideAttributeValue());
}
public TypeReference createQualifiedAssistTypeReference(char[][] previousIdentifiers, char[] assistName, long[] positions){
	switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
		case K_NEXT_TYPEREF_IS_EXCEPTION :
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions,
					CompletionOnQualifiedTypeReference.K_EXCEPTION);
		case K_NEXT_TYPEREF_IS_CLASS :
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions,
					CompletionOnQualifiedTypeReference.K_CLASS);
		case K_NEXT_TYPEREF_IS_INTERFACE :
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions,
					CompletionOnQualifiedTypeReference.K_INTERFACE);
		default :
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions);
	}
}
public NameReference createSingleAssistNameReference(char[] assistName, long position) {
	int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
	if(false){//!isInsideMethod()) {
		if (isInsideFieldInitialization()) {
			return new CompletionOnSingleNameReference(
					assistName,
					position,
					new char[][]{Keywords.FALSE, Keywords.TRUE},
					false,
					isInsideAttributeValue());
		}
		return new CompletionOnSingleNameReference(assistName, position, isInsideAttributeValue());
	} else {
		boolean canBeExplicitConstructorCall = false;
		if(kind == K_BLOCK_DELIMITER
			&& previousKind == K_BLOCK_DELIMITER
			&& previousInfo == DO) {
			return new CompletionOnKeyword3(assistName, position, Keywords.WHILE);
		} else if(kind == K_BLOCK_DELIMITER
			&& previousKind == K_BLOCK_DELIMITER
			&& previousInfo == TRY) {
			return new CompletionOnKeyword3(assistName, position, new char[][]{Keywords.CATCH, Keywords.FINALLY});
		} else if(kind == K_BLOCK_DELIMITER
			&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == SWITCH) {
			return new CompletionOnKeyword3(assistName, position, new char[][]{Keywords.CASE, Keywords.DEFAULT});
		} else {
			char[][] keywords = new char[Keywords.COUNT][];
			int count = 0;

//			if((lastModifiers & ClassFileConstants.AccStatic) == 0) {
//				keywords[count++]= Keywords.SUPER;
				keywords[count++]= Keywords.THIS;
//			}
			keywords[count++]= Keywords.NEW;

			if(kind == K_BLOCK_DELIMITER || kind==0) {
				if(canBeExplicitConstructor == YES) {
					canBeExplicitConstructorCall = true;
				}

//				keywords[count++]= Keywords.ASSERT;
				keywords[count++]= Keywords.DO;
				keywords[count++]= Keywords.FOR;
				keywords[count++]= Keywords.IF;
				keywords[count++]= Keywords.RETURN;
				keywords[count++]= Keywords.SWITCH;
//				keywords[count++]= Keywords.SYNCHRONIZED;
				keywords[count++]= Keywords.THROW;
				keywords[count++]= Keywords.TRY;
				keywords[count++]= Keywords.WHILE;
				keywords[count++]= Keywords.VAR;
				keywords[count++]= Keywords.FUNCTION;
				keywords[count++]= Keywords.DELETE;
				keywords[count++]= Keywords.TYPEOF;

//				keywords[count++]= Keywords.FINAL;
//				keywords[count++]= Keywords.CLASS;

				if(previousKind == K_BLOCK_DELIMITER) {
					switch (previousInfo) {
						case IF :
							keywords[count++]= Keywords.ELSE;
							break;
						case CATCH :
							keywords[count++]= Keywords.CATCH;
							keywords[count++]= Keywords.FINALLY;
							break;
					}
				}
				if(isInsideLoop()) {
					keywords[count++]= Keywords.CONTINUE;
				}
				if(isInsideBreakable()) {
					keywords[count++]= Keywords.BREAK;
				}
			} else if(kind != K_BETWEEN_CASE_AND_COLON && kind != K_BETWEEN_DEFAULT_AND_COLON) {
				keywords[count++]= Keywords.TRUE;
				keywords[count++]= Keywords.FALSE;
				keywords[count++]= Keywords.NULL;
				keywords[count++]= Keywords.UNDEFINED;
				keywords[count++]= Keywords.FUNCTION;

				if(kind == K_SWITCH_LABEL) {
					if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) != DEFAULT) {
						keywords[count++]= Keywords.DEFAULT;
					}
					keywords[count++]= Keywords.BREAK;
					keywords[count++]= Keywords.CASE;
					keywords[count++]= Keywords.DO;
					keywords[count++]= Keywords.FOR;
					keywords[count++]= Keywords.IF;
					keywords[count++]= Keywords.RETURN;
					keywords[count++]= Keywords.SWITCH;
//					keywords[count++]= Keywords.SYNCHRONIZED;
					keywords[count++]= Keywords.THROW;
					keywords[count++]= Keywords.TRY;
					keywords[count++]= Keywords.WHILE;
					keywords[count++]= Keywords.VAR;
					keywords[count++]= Keywords.FUNCTION;
					keywords[count++]= Keywords.DELETE;
					keywords[count++]= Keywords.TYPEOF;
					if(isInsideLoop()) {
						keywords[count++]= Keywords.CONTINUE;
				}							}
			}
			System.arraycopy(keywords, 0 , keywords = new char[count][], 0, count);

			return new CompletionOnSingleNameReference(assistName, position, keywords, canBeExplicitConstructorCall, isInsideAttributeValue());
		}
	}
}
public TypeReference createSingleAssistTypeReference(char[] assistName, long position) {
	switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
		case K_NEXT_TYPEREF_IS_EXCEPTION :
			return new CompletionOnSingleTypeReference(assistName, position, CompletionOnSingleTypeReference.K_EXCEPTION) ;
		case K_NEXT_TYPEREF_IS_CLASS :
			return new CompletionOnSingleTypeReference(assistName, position, CompletionOnSingleTypeReference.K_CLASS);
		case K_NEXT_TYPEREF_IS_INTERFACE :
			return new CompletionOnSingleTypeReference(assistName, position, CompletionOnSingleTypeReference.K_INTERFACE);
		default :
			return new CompletionOnSingleTypeReference(assistName, position);
	}
}
public TypeReference createParameterizedSingleAssistTypeReference(TypeReference[] typeArguments, char[] assistName, long position) {
	return this.createSingleAssistTypeReference(assistName, position);
}
protected StringLiteral createStringLiteral(char[] token, int start, int end, int lineNumber) {
	if (start <= this.cursorLocation && this.cursorLocation <= end){
		char[] source = this.scanner.source;

		int contentStart = start;
		int contentEnd = end;

		// " could be as unicode \u0022
		int pos = contentStart;
		if(source[pos] == '\"') {
			contentStart = pos + 1;
		} else if(source[pos] == '\\' && source[pos+1] == 'u') {
			pos += 2;
			while (source[pos] == 'u') {
				pos++;
			}
			if(source[pos] == 0 && source[pos + 1] == 0 && source[pos + 2] == 2 && source[pos + 3] == 2) {
				contentStart = pos + 4;
			}
		}

		pos = contentEnd;
		if(source[pos] == '\"') {
			contentEnd = pos - 1;
		} else if(source.length > 5 && source[pos-4] == 'u') {
			if(source[pos - 3] == 0 && source[pos - 2] == 0 && source[pos - 1] == 2 && source[pos] == 2) {
				pos -= 5;
				while (pos > -1 && source[pos] == 'u') {
					pos--;
				}
				if(pos > -1 && source[pos] == '\\') {
					contentEnd = pos - 1;
				}
			}
		}

		if(contentEnd < start) {
			contentEnd = end;
		}

		if(this.cursorLocation != end || end == contentEnd) {
			CompletionOnStringLiteral stringLiteral = new CompletionOnStringLiteral(
					token,
					start,
					end,
					contentStart,
					contentEnd,
					lineNumber);

			this.assistNode = stringLiteral;
			this.restartRecovery = AssistParser.STOP_AT_CURSOR;
			this.lastCheckPoint = end;

			return stringLiteral;
		}
	}
	return super.createStringLiteral(token, start, end, lineNumber);
}
protected TypeReference copyDims(TypeReference typeRef, int dim) {
	if (this.assistNode == typeRef) {
		return typeRef;
	}
	TypeReference result = super.copyDims(typeRef, dim);
	if (this.assistNodeParent == typeRef) {
		this.assistNodeParent = result;
	}
	return result;
}
public CompilationUnitDeclaration dietParse(ICompilationUnit sourceUnit, CompilationResult compilationResult, int cursorLoc) {

	this.cursorLocation = cursorLoc;
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	completionScanner.completionIdentifier = null;
	completionScanner.cursorLocation = cursorLoc;
	return this.dietParse(sourceUnit, compilationResult);
}
/*
 * Flush parser/scanner state regarding to code assist
 */
public void flushAssistState() {

	super.flushAssistState();
	this.isOrphanCompletionNode = false;
	this.isAlreadyAttached = false;
	assistNodeParent = null;
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	completionScanner.completedIdentifierStart = 0;
	completionScanner.completedIdentifierEnd = -1;
}
protected NameReference getUnspecifiedReference() {
	NameReference nameReference = super.getUnspecifiedReference();
	if (this.record) {
		recordReference(nameReference);
	}
	return nameReference;
}
protected NameReference getUnspecifiedReferenceOptimized() {
	if (this.identifierLengthStack[this.identifierLengthPtr] > 1) { // reducing a qualified name
		// potential receiver is being poped, so reset potential receiver
		this.invocationType = NO_RECEIVER;
		this.qualifier = -1;
	}
	NameReference nameReference = super.getUnspecifiedReferenceOptimized();
	if (this.record) {
		recordReference(nameReference);
	}
	return nameReference;
}
private boolean isAlreadyPotentialName(int identifierStart) {
	if (this.potentialVariableNamesPtr < 0) return false;

	return identifierStart <= this.potentialVariableNameEnds[this.potentialVariableNamesPtr];
}
protected int indexOfAssistIdentifier(boolean useGenericsStack) {
	if (this.record) return -1; // when names are recorded there is no assist identifier
	return super.indexOfAssistIdentifier(useGenericsStack);
}
public void initialize() {
	super.initialize();
	this.labelPtr = -1;
	this.initializeForBlockStatements();
}
public void initialize(boolean initializeNLS) {
	super.initialize(initializeNLS);
	this.labelPtr = -1;
	this.initializeForBlockStatements();
}
/*
 * Initializes the state of the parser that is about to go for BlockStatements.
 */
private void initializeForBlockStatements() {
	this.previousToken = -1;
	this.previousIdentifierPtr = -1;
	this.bracketDepth = 0;
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;
	popUntilElement(K_SWITCH_LABEL);
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_SWITCH_LABEL) {
		this.popUntilElement(K_BLOCK_DELIMITER);
	}
}
public void initializeScanner(){
	this.scanner = new CompletionScanner(this.options.sourceLevel);
}
/**
 * Returns whether the completion is just after an array type
 * eg. String[].[cursor]
 */
private boolean isAfterArrayType() {
	// TBD: The following relies on the fact that array dimensions are small: it says that if the
	//      top of the intStack is less than 11, then it must be a dimension
	//      (smallest position of array type in a compilation unit is 11 as in "class X{Y[]")
	if ((this.intPtr > -1) && (this.intStack[this.intPtr] < 11)) {
		return true;
	}
	return false;
}
private boolean isEmptyNameCompletion() {
	return
		this.assistNode != null &&
		this.assistNode instanceof CompletionOnSingleNameReference &&
		(((CompletionOnSingleNameReference)this.assistNode).token.length == 0);
}
protected boolean isInsideAnnotation() {
	int i = elementPtr;
	while(i > -1) {
		if(elementKindStack[i] == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN)
			return true;
		i--;
	}
	return false;
}
protected boolean isIndirectlyInsideBlock(){
	int i = elementPtr;
	while(i > -1) {
		if(elementKindStack[i] == K_BLOCK_DELIMITER)
			return true;
		i--;
	}
	return false;
}

protected boolean isInsideBlock(){
	int i = elementPtr;
	while(i > -1) {
		switch (elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_BLOCK_DELIMITER : return true;
		}
		i--;
	}
	return false;
}
protected boolean isInsideBreakable(){
	int i = elementPtr;
	while(i > -1) {
		switch (elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_SWITCH_LABEL : return true;
			case K_BLOCK_DELIMITER :
				switch(elementInfoStack[i]) {
					case FOR :
					case DO :
					case WHILE :
						return true;
				}
		}
		i--;
	}
	return false;
}
protected boolean isInsideLoop(){
	int i = elementPtr;
	while(i > -1) {
		switch (elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_BLOCK_DELIMITER :
				switch(elementInfoStack[i]) {
					case FOR :
					case DO :
					case WHILE :
						return true;
				}
		}
		i--;
	}
	return false;
}
protected boolean isInsideReturn(){
	int i = elementPtr;
	while(i > -1) {
		switch (elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_BLOCK_DELIMITER : return false;
			case K_INSIDE_RETURN_STATEMENT : return true;
		}
		i--;
	}
	return false;
}
public CompilationUnitDeclaration parse(ICompilationUnit sourceUnit, CompilationResult compilationResult, int cursorLoc) {

	this.cursorLocation = cursorLoc;
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	completionScanner.completionIdentifier = null;
	completionScanner.cursorLocation = cursorLoc;
	return this.parse(sourceUnit, compilationResult);
}
public void parseBlockStatements(
	ConstructorDeclaration cd,
	CompilationUnitDeclaration unit) {
	canBeExplicitConstructor = 1;
	super.parseBlockStatements(cd, unit);
}
public MethodDeclaration parseSomeStatements(int start, int end, int fakeBlocksCount, CompilationUnitDeclaration unit) {
	this.methodRecoveryActivated = true;

	initialize();

	// simulate goForMethodBody except that we don't want to balance brackets because they are not going to be balanced
	goForBlockStatementsopt();

	MethodDeclaration fakeMethod = new MethodDeclaration(unit.compilationResult());
	fakeMethod.setSelector(FAKE_METHOD_NAME);
	fakeMethod.bodyStart = start;
	fakeMethod.bodyEnd = end;
	fakeMethod.declarationSourceStart = start;
	fakeMethod.declarationSourceEnd = end;
	fakeMethod.sourceStart = start;
	fakeMethod.sourceEnd = start; //fake method must ignore the method header

	referenceContext = fakeMethod;
	compilationUnit = unit;
	this.diet = false;
	this.restartRecovery = true;


	scanner.resetTo(start, end);
	consumeNestedMethod();
	for (int i = 0; i < fakeBlocksCount; i++) {
		consumeOpenFakeBlock();
	}
	try {
		parse();
	} catch (AbortCompilation ex) {
		lastAct = ERROR_ACTION;
	} finally {
		nestedMethod[nestedType]--;
	}
	if (!this.hasError) {
		int length;
		if (astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) != 0) {
			System.arraycopy(
				this.astStack,
				(this.astPtr -= length) + 1,
				fakeMethod.statements = new Statement[length],
				0,
				length);
		}
	}

	return fakeMethod;
}
protected void popUntilCompletedAnnotationIfNecessary() {
	if(elementPtr < 0) return;

	int i = elementPtr;
	while(i > -1 &&
			(elementKindStack[i] != K_BETWEEN_ANNOTATION_NAME_AND_RPAREN ||
					(elementInfoStack[i] & ANNOTATION_NAME_COMPLETION) == 0)) {
		i--;
	}

	if(i >= 0) {
		previousKind = elementKindStack[i];
		previousInfo = elementInfoStack[i];
		elementPtr = i - 1;
	}
}

/*
 * Prepares the state of the parser to go for BlockStatements.
 */
protected void prepareForBlockStatements() {
	this.nestedMethod[this.nestedType = 0] = 1;
	this.variablesCounter[this.nestedType] = 0;
	this.realBlockStack[this.realBlockPtr = 1] = 0;

	this.initializeForBlockStatements();
}
protected void pushOnLabelStack(char[] label){
	if (this.labelPtr < -1) return;

	int stackLength = this.labelStack.length;
	if (++this.labelPtr >= stackLength) {
		System.arraycopy(
			this.labelStack, 0,
			this.labelStack = new char[stackLength + LabelStackIncrement][], 0,
			stackLength);
	}
	this.labelStack[this.labelPtr] = label;
}
/**
 * Creates a completion on member access node and push it
 * on the expression stack.
 */
private void pushCompletionOnMemberAccessOnExpressionStack(boolean isSuperAccess) {
	char[] source = identifierStack[identifierPtr];
	long pos = identifierPositionStack[identifierPtr--];
	CompletionOnMemberAccess fr = new CompletionOnMemberAccess(source, pos, isInsideAnnotation());
	this.assistNode = fr;
	this.lastCheckPoint = fr.sourceEnd + 1;
	identifierLengthPtr--;
	if (isSuperAccess) { //considerates the fieldReference beginning at the 'super' ....
		fr.sourceStart = intStack[intPtr--];
		fr.receiver = new SuperReference(fr.sourceStart, endPosition);
		pushOnExpressionStack(fr);
	} else { //optimize push/pop
		if ((fr.receiver = expressionStack[expressionPtr]).isThis()) { //fieldreference begins at the this
			fr.sourceStart = fr.receiver.sourceStart;
		}
		expressionStack[expressionPtr] = fr;
	}
}
public void recordCompletionOnReference(){

	if (currentElement instanceof RecoveredType){
		RecoveredType recoveredType = (RecoveredType)currentElement;

		/* filter out cases where scanner is still inside type header */
		if (!recoveredType.foundOpeningBrace) return;

		/* generate a pseudo field with a completion on type reference */
		currentElement.add(
			new CompletionOnFieldType(this.getTypeReference(0), false), 0);
		return;
	}
	if (!diet) return; // only record references attached to types

}
private void recordReference(NameReference nameReference) {
	if (!this.skipRecord &&
			this.recordFrom <= nameReference.sourceStart &&
			nameReference.sourceEnd <= this.recordTo &&
			!isAlreadyPotentialName(nameReference.sourceStart)) {
		char[] token;
		if (nameReference instanceof SingleNameReference) {
			token = ((SingleNameReference) nameReference).token;
		} else {
			token = ((QualifiedNameReference) nameReference).tokens[0];
		}

		// Most of the time a name which start with an uppercase is a type name.
		// As we don't want to resolve names to avoid to slow down performances then this name will be ignored
		if (Character.isUpperCase(token[0])) return;

		addPotentialName(token, nameReference.sourceStart, nameReference.sourceEnd);
	}
}
public void recoveryExitFromVariable() {
	if(currentElement != null && currentElement instanceof RecoveredLocalVariable) {
		RecoveredElement oldElement = currentElement;
		super.recoveryExitFromVariable();
		if(oldElement != currentElement) {
			popElement(K_LOCAL_INITIALIZER_DELIMITER);
		}
	} else {
		super.recoveryExitFromVariable();
	}
}
public void recoveryTokenCheck() {
	RecoveredElement oldElement = currentElement;
	switch (currentToken) {
		case TokenNameLBRACE :
			super.recoveryTokenCheck();
			break;
		case TokenNameRBRACE :
			super.recoveryTokenCheck();
			if(currentElement != oldElement && oldElement instanceof RecoveredBlock) {
				popElement(K_BLOCK_DELIMITER);
			}
			break;
		case TokenNamecase :
			super.recoveryTokenCheck();
			if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == SWITCH) {
				pushOnElementStack(K_SWITCH_LABEL);
			}
			break;
		case TokenNamedefault :
			super.recoveryTokenCheck();
			if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == SWITCH) {
				pushOnElementStack(K_SWITCH_LABEL, DEFAULT);
			} else if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL) {
				popElement(K_SWITCH_LABEL);
				pushOnElementStack(K_SWITCH_LABEL, DEFAULT);
			}
			break;
		default :
			super.recoveryTokenCheck();
			break;
	}
}
/*
 * Reset internal state after completion is over
 */

public void reset() {
	super.reset();
	this.cursorLocation = 0;
}
/*
 * Reset internal state after completion is over
 */

public void resetAfterCompletion() {
	this.cursorLocation = 0;
	this.flushAssistState();
}
/*
 * Reset context so as to resume to regular parse loop
 * If unable to reset for resuming, answers false.
 *
 * Move checkpoint location, reset internal stacks and
 * decide which grammar goal is activated.
 */
protected boolean resumeAfterRecovery() {
	if (this.assistNode != null) {
		/* if reached [eof] inside method body, but still inside nested type,
			or inside a field initializer, should continue in diet mode until
			the end of the method body or compilation unit */
		if ((scanner.eofPosition == cursorLocation+1)
			&& (!(referenceContext instanceof CompilationUnitDeclaration)
			|| isIndirectlyInsideFieldInitialization()
			|| assistNodeParent instanceof FieldDeclaration && !(assistNodeParent instanceof Initializer))) {

			/*	disabled since does not handle possible field/message refs, that is, Obj[ASSIST HERE]ect.registerNatives()
			// consume extra tokens which were part of the qualified reference
			//   so that the replaced source comprises them as well
			if (this.assistNode instanceof NameReference){
				int oldEof = scanner.eofPosition;
				scanner.eofPosition = currentElement.topElement().sourceEnd()+1;
				scanner.currentPosition = this.cursorLocation+1;
				int token = -1;
				try {
					do {
						// first token might not have to be a dot
						if (token >= 0 || !this.completionBehindDot){
							if ((token = scanner.getNextToken()) != TokenNameDOT) break;
						}
						if ((token = scanner.getNextToken()) != TokenNameIdentifier) break;
						this.assistNode.sourceEnd = scanner.currentPosition - 1;
					} while (token != TokenNameEOF);
				} catch (InvalidInputException e){
				} finally {
					scanner.eofPosition = oldEof;
				}
			}
			*/
			/* restart in diet mode for finding sibling constructs */
			if (currentElement instanceof RecoveredType
				|| currentElement.enclosingType() != null){

				if(lastCheckPoint <= this.assistNode.sourceEnd) {
					lastCheckPoint = this.assistNode.sourceEnd+1;
				}
				int end = currentElement.topElement().sourceEnd();
				scanner.eofPosition = end < Integer.MAX_VALUE ? end + 1 : end;
			} else {
				this.resetStacks();
				return false;
			}
		}
	}
	return super.resumeAfterRecovery();
}
public void setAssistIdentifier(char[] assistIdent){
	((CompletionScanner)scanner).completionIdentifier = assistIdent;
}
public  String toString() {
	StringBuffer buffer = new StringBuffer();
	buffer.append("elementKindStack : int[] = {"); //$NON-NLS-1$
	for (int i = 0; i <= elementPtr; i++) {
		buffer.append(String.valueOf(elementKindStack[i])).append(',');
	}
	buffer.append("}\n"); //$NON-NLS-1$
	buffer.append("elementInfoStack : int[] = {"); //$NON-NLS-1$
	for (int i = 0; i <= elementPtr; i++) {
		buffer.append(String.valueOf(elementInfoStack[i])).append(',');
	}
	buffer.append("}\n"); //$NON-NLS-1$
	buffer.append(super.toString());
	return String.valueOf(buffer);
}

/*
 * Update recovery state based on current parser/scanner state
 */
protected void updateRecoveryState() {

	/* expose parser state to recovery state */
	currentElement.updateFromParserState();

	/* may be able to retrieve completionNode as an orphan, and then attach it */
	this.completionIdentifierCheck();
	this.attachOrphanCompletionNode();

	// if an assist node has been found and a recovered element exists,
	// mark enclosing blocks as to be preserved
	if (this.assistNode != null && this.currentElement != null) {
		currentElement.preserveEnclosingBlocks();
	}

	/* check and update recovered state based on current token,
		this action is also performed when shifting token after recovery
		got activated once.
	*/
	this.recoveryTokenCheck();

	this.recoveryExitFromVariable();
}

protected LocalDeclaration createLocalDeclaration(char[] assistName, int sourceStart, int sourceEnd) {
	if (this.indexOfAssistIdentifier() < 0) {
		return super.createLocalDeclaration(assistName, sourceStart, sourceEnd);
	} else {
		CompletionOnLocalName local = new CompletionOnLocalName(assistName, sourceStart, sourceEnd);
		this.assistNode = local;
		this.lastCheckPoint = sourceEnd + 1;
		return local;
	}
}

protected FieldDeclaration createFieldDeclaration(char[] assistName, int sourceStart, int sourceEnd) {
	if (this.indexOfAssistIdentifier() < 0 || (currentElement instanceof RecoveredUnit && ((RecoveredUnit)currentElement).statementCount == 0)) {
		return super.createFieldDeclaration(assistName, sourceStart, sourceEnd);
	} else {
		CompletionOnFieldName field = new CompletionOnFieldName(assistName, sourceStart, sourceEnd);
		this.assistNode = field;
		this.lastCheckPoint = sourceEnd + 1;
		return field;
	}
}
protected void classInstanceCreation(boolean isQualified, boolean isShort) {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.classInstanceCreation(isQualified, isShort);
}
public int getCursorLocation() {
	return this.cursorLocation;
}


protected MessageSend newMessageSend() {
	if (AssistParser.STOP_AT_CURSOR)
		return super.newMessageSend();
	// '(' ArgumentListopt ')'
	// the arguments are on the expression stack

	int numArgs=expressionLengthStack[expressionLengthPtr];
	Expression receiver = expressionStack[expressionPtr-numArgs];

//	char[] selector = identifierStack[identifierPtr];
//	if (selector != this.assistIdentifier()){
	if (!(receiver instanceof CompletionOnMemberAccess || receiver instanceof CompletionOnSingleNameReference))
	{
		return super.newMessageSend();
	}
	MessageSend messageSend = new CompletionOnMessageSend();
	int length;
	if ((length = expressionLengthStack[expressionLengthPtr--]) != 0) {
		expressionPtr -= length;
		System.arraycopy(
			expressionStack,
			expressionPtr + 1,
			messageSend.arguments = new Expression[length],
			0,
			length);
	}
	assistNode = messageSend;
	if (!diet){
		this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
		this.lastIgnoredToken = -1;
	}

	this.isOrphanCompletionNode = true;
	return messageSend;
}

protected JavadocParser createJavadocParser() {
	return new CompletionJavadocParser(this);
}

	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.impl.AssistParser#createAssistTypeForAllocation(org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression)
	 */
	public void createAssistTypeForAllocation(AllocationExpression expression) {
		Expression member = expression.member;
		
		/* create a CompletionOnSingleTypeReference from the existing
		 * member expression for the given allocation expression
		 */
		if (member instanceof SingleNameReference) {
			SingleNameReference snr = (SingleNameReference) member;
			long position =  (((long)snr.sourceStart)<<32)+snr.sourceEnd;
			expression.member= new CompletionOnSingleTypeReference(snr.token,position);
			((CompletionOnSingleTypeReference)expression.member).isConstructorType = true;
		}
		else if(member instanceof CompletionOnMemberAccess) {
			CompletionOnMemberAccess memberAccess = (CompletionOnMemberAccess) member;
			
			//iterate over the receivers to build the token and find the start of the expression
			IExpression receiver = memberAccess.getReceiver();
			String token = new String(memberAccess.getToken());
			int start = memberAccess.sourceStart();
			while(receiver != null) {
				start = receiver.sourceStart();
				if(receiver instanceof IFieldReference) {
					IFieldReference ref = (IFieldReference)receiver;
					token = new String(ref.getToken()) + "." + token;
					receiver = ref.getReceiver();
				} else if(receiver instanceof ISingleNameReference) {
					ISingleNameReference ref = (ISingleNameReference)receiver;
					token = new String(ref.getToken()) + "." + token;
					receiver = null;
				} else if(receiver instanceof IThisReference) {
					IThisReference ref = (IThisReference)receiver;
					token =  "this." + token;
					receiver = null;
				}
			}
			
			//create and set the CompletionOnSingleTypeReference
			long position =  (((long)start)<<32)+memberAccess.sourceEnd;
			expression.member = new CompletionOnSingleTypeReference(token.toCharArray(), position);
			((CompletionOnSingleTypeReference)expression.member).isConstructorType = true;
		}
	}

}
