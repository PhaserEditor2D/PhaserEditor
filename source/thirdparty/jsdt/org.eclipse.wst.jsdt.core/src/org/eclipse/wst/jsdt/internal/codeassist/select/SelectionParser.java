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
package org.eclipse.wst.jsdt.internal.codeassist.select;

/*
 * Parser able to build specific completion parse nodes, given a cursorLocation.
 *
 * Cursor location denotes the position of the last character behind which completion
 * got requested:
 *  -1 means completion at the very beginning of the source
 *	0  means completion behind the first character
 *  n  means completion behind the n-th character
 */


import org.eclipse.wst.jsdt.internal.codeassist.impl.AssistParser;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.parser.JavadocParser;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveredType;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;

public class SelectionParser extends AssistParser {
	// OWNER
	protected static final int SELECTION_PARSER = 1024;
	protected static final int SELECTION_OR_ASSIST_PARSER = ASSIST_PARSER + SELECTION_PARSER;

	// KIND : all values known by SelectionParser are between 1025 and 1549
	protected static final int K_BETWEEN_CASE_AND_COLON = SELECTION_PARSER + 1; // whether we are inside a block

	public ASTNode assistNodeParent; // the parent node of assist node

	/* public fields */

	public int selectionStart, selectionEnd;

	public static final char[] THIS = "this".toCharArray(); //$NON-NLS-1$

public SelectionParser(ProblemReporter problemReporter) {
	super(problemReporter);
	this.javadocParser.checkDocComment = true;
}
public char[] assistIdentifier(){
	return ((SelectionScanner)scanner).selectionIdentifier;
}
protected void attachOrphanCompletionNode(){
	if (isOrphanCompletionNode){
		ASTNode orphan = this.assistNode;
		isOrphanCompletionNode = false;


		/* if in context of a type, then persists the identifier into a fake field return type */
		if (currentElement instanceof RecoveredType){
			RecoveredType recoveredType = (RecoveredType)currentElement;
			/* filter out cases where scanner is still inside type header */
			if (recoveredType.foundOpeningBrace) {
				/* generate a pseudo field with a completion on type reference */
				if (orphan instanceof TypeReference){
					currentElement = currentElement.add(new SelectionOnFieldType((TypeReference)orphan), 0);
					return;
				}
			}
		}

		if (orphan instanceof Expression) {
			buildMoreCompletionContext((Expression)orphan);
		} else {
			Statement statement = (Statement) orphan;
			currentElement = currentElement.add(statement, 0);
		}
		currentToken = 0; // given we are not on an eof, we do not want side effects caused by looked-ahead token
	}
}
private void buildMoreCompletionContext(Expression expression) {
	ASTNode parentNode = null;

	int kind = topKnownElementKind(SELECTION_OR_ASSIST_PARSER);
	if(kind != 0) {
//		int info = topKnownElementInfo(SELECTION_OR_ASSIST_PARSER);
		switch (kind) {
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
					parentNode = switchStatement;
					this.assistNodeParent = parentNode;
				}
				break;
		}
	}
	if(parentNode != null) {
		currentElement = currentElement.add((Statement)parentNode, 0);
	} else {
		currentElement = currentElement.add((Statement)wrapWithExplicitConstructorCallIfNeeded(expression), 0);
		if(lastCheckPoint < expression.sourceEnd) {
			lastCheckPoint = expression.sourceEnd + 1;
		}
	}
}
private boolean checkRecoveredType() {
	if (currentElement instanceof RecoveredType){
		/* check if current awaiting identifier is the completion identifier */
		if (this.indexOfAssistIdentifier() < 0) return false;

		if ((lastErrorEndPosition >= selectionStart)
			&& (lastErrorEndPosition <= selectionEnd+1)){
			return false;
		}
		RecoveredType recoveredType = (RecoveredType)currentElement;
		/* filter out cases where scanner is still inside type header */
		if (recoveredType.foundOpeningBrace) {
			this.assistNode = this.getTypeReference(0);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		}
	}
	return false;
}
protected void classInstanceCreation(boolean hasClassBody, boolean isShort) {

	// ClassInstanceCreationExpression ::= 'new' ClassType '(' ArgumentListopt ')' ClassBodyopt

	// ClassBodyopt produces a null item on the astStak if it produces NO class body
	// An empty class body produces a 0 on the length stack.....


//	if ((astLengthStack[astLengthPtr] == 1)
//		&& (astStack[astPtr] == null)) {


//		int index;
		int argsLength= isShort ? 0 : expressionLengthStack[expressionLengthPtr];
		if (!(this.expressionStack[this.expressionPtr-argsLength] instanceof SelectionOnSingleNameReference))
		{
//
//
//		if ((index = this.indexOfAssistIdentifier()) < 0) {
//			super.classInstanceCreation(hasClassBody, isShort);
//			return;
//		} else if(this.identifierLengthPtr > -1 &&
//					(this.identifierLengthStack[this.identifierLengthPtr] - 1) != index) {
			super.classInstanceCreation(hasClassBody, isShort);
			return;
		}
		QualifiedAllocationExpression alloc;
//		astPtr--;
//		astLengthPtr--;
		alloc = new SelectionOnQualifiedAllocationExpression();
		alloc.sourceEnd = endPosition; //the position has been stored explicitly

		if (!isShort)
		{
			int length;
			if ((length = expressionLengthStack[expressionLengthPtr--]) != 0) {
				expressionPtr -= length;
				System.arraycopy(
						expressionStack,
						expressionPtr + 1,
						alloc.arguments = new Expression[length],
						0,
						length);
			}
		}
		else
			alloc.arguments=new Expression[0];
		// trick to avoid creating a selection on type reference
		char [] oldIdent = this.assistIdentifier();
		this.setAssistIdentifier(null);
//		alloc.type = getTypeReference(0);
		alloc.member = this.expressionStack[this.expressionPtr--];
		this.expressionLengthPtr--;

		this.setAssistIdentifier(oldIdent);

		//the default constructor with the correct number of argument
		//will be created and added by the TC (see createsInternalConstructorWithBinding)
		alloc.sourceStart = intStack[intPtr--];
		pushOnExpressionStack(alloc);

		this.assistNode = alloc;
		this.lastCheckPoint = alloc.sourceEnd + 1;
		if (!diet){
			this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
			this.lastIgnoredToken = -1;
		}
		this.isOrphanCompletionNode = true;
//	} else {
//		super.classInstanceCreation(hasClassBody, isShort);
//	}
}
protected void consumeEnterVariable() {
	// EnterVariable ::= $empty
	// do nothing by default

	super.consumeEnterVariable();

//	AbstractVariableDeclaration variable = (AbstractVariableDeclaration) astStack[astPtr];
//	if (variable.type == assistNode){
//		if (!diet){
//			this.restartRecovery	= true;	// force to restart in recovery mode
//			this.lastIgnoredToken = -1;
//		}
//		isOrphanCompletionNode = false; // already attached inside variable decl
//	}
}

protected void consumeExitVariableWithInitialization() {
	super.consumeExitVariableWithInitialization();

	// does not keep the initialization if selection is not inside
	AbstractVariableDeclaration variable = (AbstractVariableDeclaration) astStack[astPtr];
	int start = variable.initialization.sourceStart;
	int end =  variable.initialization.sourceEnd;
	if ((selectionStart < start) &&  (selectionEnd < start) ||
			(selectionStart > end) && (selectionEnd > end)) {
		if (STOP_AT_CURSOR)
			variable.initialization = null;
	}

}
protected void consumeCallExpressionWithSimpleName() {
	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeCallExpressionWithSimpleName();
		return;
	}
	FieldReference fieldReference =
		new SelectionOnFieldReference(
			identifierStack[identifierPtr],
			identifierPositionStack[identifierPtr--]);
	identifierLengthPtr--;
		if ((fieldReference.receiver = expressionStack[expressionPtr]).isThis()) { //fieldReferenceerence begins at the this
			fieldReference.sourceStart = fieldReference.receiver.sourceStart;
		}
		expressionStack[expressionPtr] = fieldReference;
	assistNode = fieldReference;
	this.lastCheckPoint = fieldReference.sourceEnd + 1;
	if (!diet){
		this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
		this.lastIgnoredToken = -1;
	}
	this.isOrphanCompletionNode = true;

}
protected void consumeMemberExpressionWithSimpleName() {
	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeMemberExpressionWithSimpleName();
		return;
	}
	FieldReference fieldReference =
		new SelectionOnFieldReference(
			identifierStack[identifierPtr],
			identifierPositionStack[identifierPtr--]);
	identifierLengthPtr--;
		if ((fieldReference.receiver = expressionStack[expressionPtr]).isThis()) { //fieldReferenceerence begins at the this
			fieldReference.sourceStart = fieldReference.receiver.sourceStart;
		}
		expressionStack[expressionPtr] = fieldReference;
	assistNode = fieldReference;
	this.lastCheckPoint = fieldReference.sourceEnd + 1;
	if (!diet){
		this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
		this.lastIgnoredToken = -1;
	}
	this.isOrphanCompletionNode = true;

}
protected void consumeFormalParameter(boolean isVarArgs) {
	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeFormalParameter(isVarArgs);
//		if((!diet || dietInt != 0) && astPtr > -1) {
//			Argument argument = (Argument) astStack[astPtr];
//			if(argument.type == assistNode) {
//				isOrphanCompletionNode = true;
//				this.restartRecovery	= true;	// force to restart in recovery mode
//				this.lastIgnoredToken = -1;
//			}
//		}
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
//		int modifierPositions = intStack[intPtr--];
//		intPtr--;
		int modifierPositions=(int) (namePositions >>> 32);
		Argument arg =
			new SelectionOnArgumentName(
				identifierName,
				namePositions,
				null,
				 ClassFileConstants.AccDefault);
//				intStack[intPtr + 1] & ~ClassFileConstants.AccDeprecated); // modifiers
		arg.declarationSourceStart = modifierPositions;
		pushOnAstStack(arg);

		assistNode = arg;
		this.lastCheckPoint = (int) namePositions;
		isOrphanCompletionNode = true;

		if (!diet){
			this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
			this.lastIgnoredToken = -1;
		}

		/* if incomplete method header, listLength counter will not have been reset,
			indicating that some arguments are available on the stack */
		listLength++;
	}
}
protected void consumeLocalVariableDeclarationStatement() {
	super.consumeLocalVariableDeclarationStatement();

	// force to restart in recovery mode if the declaration contains the selection
	if (!this.diet) {
		LocalDeclaration localDeclaration = (LocalDeclaration) this.astStack[this.astPtr];
		if ((this.selectionStart >= localDeclaration.sourceStart)
				&&  (this.selectionEnd <= localDeclaration.sourceEnd)) {
			this.restartRecovery	= AssistParser.STOP_AT_CURSOR;
			this.lastIgnoredToken = -1;
		}
	}
}

protected void consumeMethodHeaderName(boolean isAnonymous) {
	MethodDeclaration md = null;
	if (this.indexOfAssistIdentifier() < 0) {
		md = new MethodDeclaration(this.compilationUnit.compilationResult);
	}
	else { 
		md = new SelectionOnMethodName(this.compilationUnit.compilationResult);
	}

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
	//		md.returnType = getTypeReference(this.intStack[this.intPtr--]);
	//modifiers
	int functionPos = this.intStack[this.intPtr--];
	int modifierPos = this.intStack[this.intPtr--];
	md.declarationSourceStart = (functionPos>modifierPos)? modifierPos:functionPos;
	md.modifiers = this.intStack[this.intPtr--];
	// consume annotations
	//		int length;
	//		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
	//			System.arraycopy(
	//				this.expressionStack,
	//				(this.expressionPtr -= length) + 1,
	//				md.annotations = new Annotation[length],
	//				0,
	//				length);
	//		}
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

//  Nothing here applicable to javascript
//protected void consumeMethodInvocationPrimary() {
//	//optimize the push/pop
//	//FunctionInvocation ::= Primary '.' 'Identifier' '(' ArgumentListopt ')'
//
//	char[] selector = identifierStack[identifierPtr];
//	int accessMode;
//	if(selector == this.assistIdentifier()) {
//		if(CharOperation.equals(selector, SUPER)) {
//			accessMode = ExplicitConstructorCall.Super;
//		} else if(CharOperation.equals(selector, THIS)) {
//			accessMode = ExplicitConstructorCall.This;
//		} else {
//			super.consumeMethodInvocationPrimary();
//			return;
//		}
//	} else {
//		super.consumeMethodInvocationPrimary();
//		return;
//	}
//
//	final ExplicitConstructorCall constructorCall = new SelectionOnExplicitConstructorCall(accessMode);
//	constructorCall.sourceEnd = rParenPos;
//	int length;
//	if ((length = expressionLengthStack[expressionLengthPtr--]) != 0) {
//		expressionPtr -= length;
//		System.arraycopy(expressionStack, expressionPtr + 1, constructorCall.arguments = new Expression[length], 0, length);
//	}
//	constructorCall.qualification = expressionStack[expressionPtr--];
//	constructorCall.sourceStart = constructorCall.qualification.sourceStart;
//
//	if (!diet){
//		pushOnAstStack(constructorCall);
//		this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
//		this.lastIgnoredToken = -1;
//	} else {
//		pushOnExpressionStack(new Expression(){
//			public TypeBinding resolveType(BlockScope scope) {
//				constructorCall.resolve(scope);
//				return null;
//			}
//			public StringBuffer printExpression(int indent, StringBuffer output) {
//				return output;
//			}
//		});
//	}
//
//	this.assistNode = constructorCall;
//	this.lastCheckPoint = constructorCall.sourceEnd + 1;
//	this.isOrphanCompletionNode = true;
//}
protected void consumeToken(int token) {
	super.consumeToken(token);

	// if in a method or if in a field initializer
	if (isInsideMethod() || isInsideFieldInitialization()) {
		switch (token) {
			case TokenNamecase :
				pushOnElementStack(K_BETWEEN_CASE_AND_COLON);
				break;
			case TokenNameCOLON:
				if(topKnownElementKind(SELECTION_OR_ASSIST_PARSER) == K_BETWEEN_CASE_AND_COLON) {
					popElement(K_BETWEEN_CASE_AND_COLON);
				}
				break;
		}
	}
}
public ImportReference createAssistImportReference(char[][] tokens, long[] positions){
	return new SelectionOnImportReference(tokens, positions);
}
protected JavadocParser createJavadocParser() {
	return new SelectionJavadocParser(this);
}
protected LocalDeclaration createLocalDeclaration(char[] assistName,int sourceStart,int sourceEnd) {
	if (this.indexOfAssistIdentifier() < 0) {
		return super.createLocalDeclaration(assistName, sourceStart, sourceEnd);
	} else {
		SelectionOnLocalName local = new SelectionOnLocalName(assistName, sourceStart, sourceEnd);
		this.assistNode = local;
		this.lastCheckPoint = sourceEnd + 1;
		return local;
	}
}
public NameReference createQualifiedAssistNameReference(char[][] previousIdentifiers, char[] assistName, long[] positions){
	return new SelectionOnQualifiedNameReference(
					previousIdentifiers,
					assistName,
					positions);
}
public TypeReference createQualifiedAssistTypeReference(char[][] previousIdentifiers, char[] assistName, long[] positions){
	return new SelectionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions);
}
public NameReference createSingleAssistNameReference(char[] assistName, long position) {
	return new SelectionOnSingleNameReference(assistName, position);
}
public TypeReference createSingleAssistTypeReference(char[] assistName, long position) {
	return new SelectionOnSingleTypeReference(assistName, position);
}
public CompilationUnitDeclaration dietParse(ICompilationUnit sourceUnit, CompilationResult compilationResult, int start, int end) {

	this.selectionStart = start;
	this.selectionEnd = end;
	SelectionScanner selectionScanner = (SelectionScanner)this.scanner;
	selectionScanner.selectionIdentifier = null;
	selectionScanner.selectionStart = start;
	selectionScanner.selectionEnd = end;
	return this.dietParse(sourceUnit, compilationResult);
}
protected NameReference getUnspecifiedReference() {
	/* build a (unspecified) NameReference which may be qualified*/

	int completionIndex;

	/* no need to take action if not inside completed identifiers */
	if ((completionIndex = indexOfAssistIdentifier()) < 0) {
		return super.getUnspecifiedReference();
	}

	int length = identifierLengthStack[identifierLengthPtr];
	NameReference nameReference;
	/* retrieve identifiers subset and whole positions, the completion node positions
		should include the entire replaced source. */
	char[][] subset = identifierSubSet(completionIndex);
	identifierLengthPtr--;
	identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(
		identifierPositionStack,
		identifierPtr + 1,
		positions,
		0,
		length);
	/* build specific completion on name reference */
	if (completionIndex == 0) {
		/* completion inside first identifier */
		nameReference = this.createSingleAssistNameReference(assistIdentifier(), positions[0]);
	} else {
		/* completion inside subsequent identifier */
		nameReference = this.createQualifiedAssistNameReference(subset, assistIdentifier(), positions);
	}
	assistNode = nameReference;
	this.lastCheckPoint = nameReference.sourceEnd + 1;
	if (!diet){
		this.restartRecovery	= true;	// force to restart in recovery mode
		this.lastIgnoredToken = -1;
	}
	this.isOrphanCompletionNode = true;
	return nameReference;
}
/*
 * Copy of code from superclass with the following change:
 * In the case of qualified name reference if the cursor location is on the
 * qualified name reference, then create a CompletionOnQualifiedNameReference
 * instead.
 */
protected NameReference getUnspecifiedReferenceOptimized() {

	int index = indexOfAssistIdentifier();
	NameReference reference = super.getUnspecifiedReferenceOptimized();

	if (index >= 0){
		if (!diet){
			this.restartRecovery	= AssistParser.STOP_AT_CURSOR;	// force to restart in recovery mode
			this.lastIgnoredToken = -1;
		}
		this.isOrphanCompletionNode = true;
	}
	return reference;
}
public void initializeScanner(){
	this.scanner = new SelectionScanner(this.options.sourceLevel);
}
protected MessageSend newMessageSend() {
	// '(' ArgumentListopt ')'
	// the arguments are on the expression stack



	int numArgs=expressionLengthStack[expressionLengthPtr];
	Expression receiver = expressionStack[expressionPtr-numArgs];
//	char[] selector = identifierStack[identifierPtr];
//	if (selector != this.assistIdentifier()){

	if (!(receiver instanceof SelectionOnSingleNameReference || receiver instanceof SelectionOnFieldReference))
	{
		return super.newMessageSend();
	}
	MessageSend messageSend = new SelectionOnMessageSend();
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
public CompilationUnitDeclaration parse(ICompilationUnit sourceUnit, CompilationResult compilationResult, int start, int end) {

	if (end == -1) return super.parse(sourceUnit, compilationResult, start, end);

	this.selectionStart = start;
	this.selectionEnd = end;
	SelectionScanner selectionScanner = (SelectionScanner)this.scanner;
	selectionScanner.selectionIdentifier = null;
	selectionScanner.selectionStart = start;
	selectionScanner.selectionEnd = end;
	return super.parse(sourceUnit, compilationResult, -1, -1/*parse without reseting the scanner*/);
}
/*
 * Reset context so as to resume to regular parse loop
 * If unable to reset for resuming, answers false.
 *
 * Move checkpoint location, reset internal stacks and
 * decide which grammar goal is activated.
 */
protected boolean resumeAfterRecovery() {

	/* if reached assist node inside method body, but still inside nested type,
		should continue in diet mode until the end of the method body */
	if (this.assistNode != null
		&& !(referenceContext instanceof CompilationUnitDeclaration)){
		currentElement.preserveEnclosingBlocks();
		if (currentElement.enclosingType() == null) {
			if(!(currentElement instanceof RecoveredType)) {
				this.resetStacks();
				return false;
			}

			RecoveredType recoveredType = (RecoveredType)currentElement;
			if(recoveredType.typeDeclaration != null && recoveredType.typeDeclaration.allocation == this.assistNode){
				this.resetStacks();
				return false;
			}
		}
	}
	return super.resumeAfterRecovery();
}

public void selectionIdentifierCheck(){
	if (checkRecoveredType()) return;
}
public void setAssistIdentifier(char[] assistIdent){
	((SelectionScanner)scanner).selectionIdentifier = assistIdent;
}
/*
 * Update recovery state based on current parser/scanner state
 */
protected void updateRecoveryState() {

	/* expose parser state to recovery state */
	currentElement.updateFromParserState();

	/* may be able to retrieve completionNode as an orphan, and then attach it */
	this.selectionIdentifierCheck();
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
}

public  String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("elementKindStack : int[] = {"); //$NON-NLS-1$
	for (int i = 0; i <= elementPtr; i++) {
		sb.append(elementKindStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');
	
	sb.append("elementInfoStack : int[] = {"); //$NON-NLS-1$
	for (int i = 0; i <= elementPtr; i++) {
		sb.append(elementInfoStack[i]);
		sb.append(',');
	}
	sb.append('}').append('\n');
	sb.append(super.toString());
	return sb.toString();
}
public int getCursorLocation() {
	return this.selectionStart;
}

public void createAssistTypeForAllocation(AllocationExpression expression) {

}
}
