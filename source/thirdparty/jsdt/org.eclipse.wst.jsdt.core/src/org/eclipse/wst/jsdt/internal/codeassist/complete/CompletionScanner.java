/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 * Scanner aware of a cursor location so as to discard trailing portions of identifiers
 * containing the cursor location.
 *
 * Cursor location denotes the position of the last character behind which completion
 * got requested:
 *  -1 means completion at the very beginning of the source
 *	0  means completion behind the first character
 *  n  means completion behind the n-th character
 */
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.codeassist.impl.AssistParser;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;

public class CompletionScanner extends Scanner {


	public char[] completionIdentifier;
	public int cursorLocation;
	public int endOfEmptyToken = -1;

	/* Source positions of the completedIdentifier
	 * if inside actual identifier, end goes to the actual identifier
	 * end, in other words, beyond cursor location
	 */
	public int completedIdentifierStart = 0;
	public int completedIdentifierEnd = -1;
	public int unicodeCharSize;


	public static final char[] EmptyCompletionIdentifier = {};

public CompletionScanner(long sourceLevel) {
	super(
		false /*comment*/,
		false /*whitespace*/,
		false /*nls*/,
		sourceLevel,
		null /*taskTags*/,
		null/*taskPriorities*/,
		true/*taskCaseSensitive*/);
}

/*
 * Truncate the current identifier if it is containing the cursor location. Since completion is performed
 * on an identifier prefix.
 *
 */
public char[] getCurrentIdentifierSource() {

	if (this.completionIdentifier == null){
		if (this.cursorLocation < this.startPosition && this.currentPosition == this.startPosition){ // fake empty identifier got issued
			// remember actual identifier positions
			this.completedIdentifierStart = this.startPosition;
			this.completedIdentifierEnd = this.completedIdentifierStart - 1;
			return this.completionIdentifier = EmptyCompletionIdentifier;
		}
		if (this.cursorLocation+1 >= this.startPosition && this.cursorLocation < this.currentPosition){
			// remember actual identifier positions
			this.completedIdentifierStart = this.startPosition;
			this.completedIdentifierEnd = this.currentPosition - 1;
			if (this.withoutUnicodePtr != 0){			// check unicode scenario
				int length = this.cursorLocation + 1 - this.startPosition - this.unicodeCharSize;
				System.arraycopy(this.withoutUnicodeBuffer, 1, this.completionIdentifier = new char[length], 0, length);
			} else {
				// no char[] sharing around completionIdentifier, we want it to be unique so as to use identity checks
				int length = this.cursorLocation + 1 - this.startPosition;
				System.arraycopy(this.source, this.startPosition, (this.completionIdentifier = new char[length]), 0, length);
			}
			return this.completionIdentifier;
		}
	}
	return super.getCurrentIdentifierSource();
}

public char[] getCurrentTokenSourceString() {
	if (this.completionIdentifier == null){
		if (this.cursorLocation+1 >= this.startPosition && this.cursorLocation < this.currentPosition){
			// remember actual identifier positions
			this.completedIdentifierStart = this.startPosition;
			this.completedIdentifierEnd = this.currentPosition - 1;
			if (this.withoutUnicodePtr != 0){			// check unicode scenario
				int length = this.cursorLocation - this.startPosition - this.unicodeCharSize;
				System.arraycopy(this.withoutUnicodeBuffer, 2, this.completionIdentifier = new char[length], 0, length);
			} else {
				// no char[] sharing around completionIdentifier, we want it to be unique so as to use identity checks
				int length = this.cursorLocation - this.startPosition;
				System.arraycopy(this.source, this.startPosition + 1, (this.completionIdentifier = new char[length]), 0, length);
			}
			return this.completionIdentifier;
		}
	}
	return super.getCurrentTokenSourceString();
}
public int getNextToken() throws InvalidInputException {
	if ( pushedBack ) {
		pushedBack = false;
		return currentToken;
	}
	int previousToken = this.currentToken;
	this.wasAcr = false;
	this.unicodeCharSize = 0;
	if (this.diet) {
		jumpOverMethodBody();
		this.diet = false;
		return (currentToken=this.currentPosition > this.eofPosition ? TokenNameEOF : TokenNameRBRACE);
	}
	int whiteStart = 0;
	try {
		while (true) { //loop for jumping over comments
			this.withoutUnicodePtr = 0;
			//start with a new token (even comment written with unicode )

			// ---------Consume white space and handles start position---------
			whiteStart = this.currentPosition;
			boolean isWhiteSpace, hasWhiteSpaces = false;
			int offset = 0;
			do {
				this.startPosition = this.currentPosition;
				boolean checkIfUnicode = false;
				try {
					checkIfUnicode = ((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
						&& (this.source[this.currentPosition] == 'u');
				} catch(IndexOutOfBoundsException e) {
					if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
						// reposition scanner in case we are interested by spaces as tokens
						this.currentPosition--;
						this.startPosition = whiteStart;
						return (currentToken=TokenNameWHITESPACE);
					}
					if (this.currentPosition > this.eofPosition) {
						/* might be completing at eof (e.g. behind a dot) */
						if (this.completionIdentifier == null &&
							this.startPosition == this.cursorLocation + 1){
							this.currentPosition = this.startPosition; // for being detected as empty free identifier
							return TokenNameIdentifier;
						}
						return (currentToken=TokenNameEOF);
					}
				}
				if (checkIfUnicode) {
					isWhiteSpace = jumpOverUnicodeWhiteSpace();
					offset = 6;
				} else {
					offset = 1;
					if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
						//checkNonExternalizedString();
						if (this.recordLineSeparator) {
							pushLineSeparator();
						}
					}
					isWhiteSpace =
						(this.currentCharacter == ' ') || CharOperation.isWhitespace(this.currentCharacter);
				}
				if (isWhiteSpace) {
					hasWhiteSpaces = true;
				}
				/* completion requesting strictly inside blanks */
				if ((whiteStart != this.currentPosition)
					//&& (previousToken == TokenNameDOT)
					&& (/* !AssistParser.STOP_AT_CURSOR || */ this.completionIdentifier == null)
					&& (whiteStart <= this.cursorLocation+1)
					&& (this.cursorLocation < this.startPosition)
					&& !ScannerHelper.isJavaIdentifierStart(this.currentCharacter)){
//					if (AssistParser.STOP_AT_CURSOR)
						this.currentPosition = this.startPosition; // for next token read
					return (currentToken=TokenNameIdentifier);
				}
			} while (isWhiteSpace);
			if (this.tokenizeWhiteSpace && hasWhiteSpaces) {
				// reposition scanner in case we are interested by spaces as tokens
				this.currentPosition-=offset;
				this.startPosition = whiteStart;
				return (currentToken=TokenNameWHITESPACE);
			}
			//little trick to get out in the middle of a source computation
			if (this.currentPosition > this.eofPosition ||(!AssistParser.STOP_AT_CURSOR && this.currentPosition > this.cursorLocation)){
				/* might be completing at eof (e.g. behind a dot) */
				if (this.completionIdentifier == null &&
					this.startPosition == this.cursorLocation + 1){
					// compute end of empty identifier.
					// if the empty identifier is at the start of a next token the end of
					// empty identifier is the end of the next token (eg. "<empty token>next").
					int temp = this.eofPosition;
					this.eofPosition = this.source.length;
				 	while(getNextCharAsJavaIdentifierPart()){/*empty*/}
				 	this.eofPosition = temp;
				 	this.endOfEmptyToken = this.currentPosition - 1;
//					this.currentPosition = this.startPosition; // for being detected as empty free identifier
					return (currentToken=TokenNameIdentifier);
				}
				if (AssistParser.STOP_AT_CURSOR)
					return (currentToken=TokenNameEOF);
			}

			// ---------Identify the next token-------------

			switch (this.currentCharacter) {
				case '(' :
					return (currentToken=TokenNameLPAREN);
				case ')' :
					return (currentToken=TokenNameRPAREN);
				case '{' :
					return (currentToken=TokenNameLBRACE);
				case '}' :
					return (currentToken=TokenNameRBRACE);
				case '[' :
					return (currentToken=TokenNameLBRACKET);
				case ']' :
					return (currentToken=TokenNameRBRACKET);
				case ';' :
					return (currentToken=TokenNameSEMICOLON);
				case ',' :
					return (currentToken=TokenNameCOMMA);
				case '.' :
					if (this.startPosition <= this.cursorLocation
					    && this.cursorLocation < this.currentPosition){
					    	return (currentToken=TokenNameDOT); // completion inside .<|>12
				    }
					if (getNextCharAsDigit()) {
						return (currentToken=scanNumber(true));
					}
					int temp = this.currentPosition;
					if (getNextChar('.')) {
//						if (getNextChar('.')) {
//							return (currentToken=TokenNameELLIPSIS);
//						} else {
							this.currentPosition = temp;
							return (currentToken=TokenNameDOT);
//						}
					} else {
						this.currentPosition = temp;
						return (currentToken=TokenNameDOT);
					}
				case '+' :
					{
						int test;
						if ((test = getNextChar('+', '=')) == 0)
							return (currentToken=TokenNamePLUS_PLUS);
						if (test > 0)
							return (currentToken=TokenNamePLUS_EQUAL);
						return (currentToken=TokenNamePLUS);
					}
				case '-' :
					{
						int test;
						if ((test = getNextChar('-', '=')) == 0)
							return (currentToken=TokenNameMINUS_MINUS);
						if (test > 0)
							return (currentToken=TokenNameMINUS_EQUAL);
						return (currentToken=TokenNameMINUS);
					}
				case '~' :
					return (currentToken=TokenNameTWIDDLE);
				case '!' :
					if (getNextChar('='))
					{
						if (getNextChar('='))
						{
							currentToken=TokenNameNOT_EQUAL_EQUAL;
							return currentToken;
						}
						currentToken=TokenNameNOT_EQUAL;
						return currentToken;
					}
					currentToken=TokenNameNOT;
					return currentToken;
				case '*' :
					if (getNextChar('='))
						return (currentToken=TokenNameMULTIPLY_EQUAL);
					return (currentToken=TokenNameMULTIPLY);
				case '%' :
					if (getNextChar('='))
						return (currentToken=TokenNameREMAINDER_EQUAL);
					return (currentToken=TokenNameREMAINDER);
				case '<' :
					{
						int test;
						if ((test = getNextChar('=', '<')) == 0)
							return (currentToken=TokenNameLESS_EQUAL);
						if (test > 0) {
							if (getNextChar('='))
								return (currentToken=TokenNameLEFT_SHIFT_EQUAL);
							return (currentToken=TokenNameLEFT_SHIFT);
						}
						return (currentToken=TokenNameLESS);
					}
				case '>' :
					{
						int test;
						if (this.returnOnlyGreater) {
							return (currentToken=TokenNameGREATER);
						}
						if ((test = getNextChar('=', '>')) == 0)
							return (currentToken=TokenNameGREATER_EQUAL);
						if (test > 0) {
							if ((test = getNextChar('=', '>')) == 0)
								return (currentToken=TokenNameRIGHT_SHIFT_EQUAL);
							if (test > 0) {
								if (getNextChar('='))
									return (currentToken=TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL);
								return (currentToken=TokenNameUNSIGNED_RIGHT_SHIFT);
							}
							return (currentToken=TokenNameRIGHT_SHIFT);
						}
						return (currentToken=TokenNameGREATER);
					}
				case '=' :
					if (getNextChar('='))
					{
						if (getNextChar('='))
						{
							currentToken=TokenNameEQUAL_EQUAL_EQUAL;
							return currentToken;
						}
						currentToken=TokenNameEQUAL_EQUAL;
						return currentToken;
					}
					currentToken=TokenNameEQUAL;
					return currentToken;
				case '&' :
					{
						int test;
						if ((test = getNextChar('&', '=')) == 0)
							return (currentToken=TokenNameAND_AND);
						if (test > 0)
							return (currentToken=TokenNameAND_EQUAL);
						return (currentToken=TokenNameAND);
					}
				case '|' :
					{
						int test;
						if ((test = getNextChar('|', '=')) == 0)
							return (currentToken=TokenNameOR_OR);
						if (test > 0)
							return (currentToken=TokenNameOR_EQUAL);
						return (currentToken=TokenNameOR);
					}
				case '^' :
					if (getNextChar('='))
						return (currentToken=TokenNameXOR_EQUAL);
					return (currentToken=TokenNameXOR);
				case '?' :
					return (currentToken=TokenNameQUESTION);
				case ':' :
					return (currentToken=TokenNameCOLON);
/*				case '\'' :
					{
						int test;
						if ((test = getNextChar('\n', '\r')) == 0) {
							throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
						}
						if (test > 0) {
							// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
							for (int lookAhead = 0; lookAhead < 3; lookAhead++) {
								if (this.currentPosition + lookAhead == this.eofPosition)
									break;
								if (this.source[this.currentPosition + lookAhead] == '\n')
									break;
								if (this.source[this.currentPosition + lookAhead] == '\'') {
									this.currentPosition += lookAhead + 1;
									break;
								}
							}
							throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
						}
					}
					if (getNextChar('\'')) {
						// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
						for (int lookAhead = 0; lookAhead < 3; lookAhead++) {
							if (this.currentPosition + lookAhead == this.eofPosition)
								break;
							if (this.source[this.currentPosition + lookAhead] == '\n')
								break;
							if (this.source[this.currentPosition + lookAhead] == '\'') {
								this.currentPosition += lookAhead + 1;
								break;
							}
						}
						throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
					}
					if (getNextChar('\\')) {
						if (this.unicodeAsBackSlash) {
							// consume next character
							this.unicodeAsBackSlash = false;
							if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
								getNextUnicodeChar();
							} else {
								if (this.withoutUnicodePtr != 0) {
									unicodeStore();
								}
							}
						} else {
							this.currentCharacter = this.source[this.currentPosition++];
						}
						scanEscapeCharacter();
					} else { // consume next character
						this.unicodeAsBackSlash = false;
						boolean checkIfUnicode = false;
						try {
							checkIfUnicode = ((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
							&& (this.source[this.currentPosition] == 'u');
						} catch(IndexOutOfBoundsException e) {
							this.currentPosition--;
							throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
						}
						if (checkIfUnicode) {
							getNextUnicodeChar();
						} else {
							if (this.withoutUnicodePtr != 0) {
							    this.unicodeStore();
							}
						}
					}
					if (getNextChar('\''))
						return (currentToken=TokenNameCharacterLiteral);
					// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
					for (int lookAhead = 0; lookAhead < 20; lookAhead++) {
						if (this.currentPosition + lookAhead == this.eofPosition)
							break;
						if (this.source[this.currentPosition + lookAhead] == '\n')
							break;
						if (this.source[this.currentPosition + lookAhead] == '\'') {
							this.currentPosition += lookAhead + 1;
							break;
						}
					}
					throw new InvalidInputException(INVALID_CHARACTER_CONSTANT); */
				case '\'' :
				case '"' :
					char character = this.currentCharacter;
					try {
						// consume next character
						this.unicodeAsBackSlash = false;
						boolean isUnicode = false;
						if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
							&& (this.source[this.currentPosition] == 'u')) {
							getNextUnicodeChar();
							isUnicode = true;
						} else {
							if (this.withoutUnicodePtr != 0) {
							    this.unicodeStore();
							}
						}

						while ((this.currentCharacter !=  character) || ((this.currentCharacter ==  character) && (isUnicode == true))) {
							if ((this.currentCharacter == '\n' && !isUnicode) || (this.currentCharacter == '\r' && !isUnicode)) {
								this.currentPosition--; // set current position on new line character
								if(this.startPosition <= this.cursorLocation
										&& this.cursorLocation <= this.currentPosition-1) {
									// complete inside a string literal
									return (currentToken=TokenNameStringLiteral);
								}
								throw new InvalidInputException(INVALID_CHAR_IN_STRING);
							}
							if (this.currentCharacter == '\\') {
								if (this.unicodeAsBackSlash) {
									this.withoutUnicodePtr--;
									// consume next character
									this.unicodeAsBackSlash = false;
									if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
										getNextUnicodeChar();
										isUnicode = true;
										this.withoutUnicodePtr--;
									} else {
										isUnicode = false;
									}
								} else {
									if (this.withoutUnicodePtr == 0) {
										unicodeInitializeBuffer(this.currentPosition - this.startPosition);
									}
									this.withoutUnicodePtr --;
									this.currentCharacter = this.source[this.currentPosition++];
								}
								// we need to compute the escape character in a separate buffer
								if (scanEscapeCharacter() && this.withoutUnicodePtr != 0) {
									unicodeStore();
								}
							}
							// consume next character
							this.unicodeAsBackSlash = false;
							if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\')
								&& (this.source[this.currentPosition] == 'u')) {
								getNextUnicodeChar();
								isUnicode = true;
							} else {
								isUnicode = false;
								if (this.withoutUnicodePtr != 0) {
								    this.unicodeStore();
								}
							}

						}
					} catch (IndexOutOfBoundsException e) {
						this.currentPosition--;
						if(this.startPosition <= this.cursorLocation
							&& this.cursorLocation < this.currentPosition) {
							// complete inside a string literal
							return (currentToken=TokenNameStringLiteral);
						}
						throw new InvalidInputException(UNTERMINATED_STRING);
					} catch (InvalidInputException e) {
						if (e.getMessage().equals(INVALID_ESCAPE)) {
							// relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
							for (int lookAhead = 0; lookAhead < 50; lookAhead++) {
								if (this.currentPosition + lookAhead == this.eofPosition)
									break;
								if (this.source[this.currentPosition + lookAhead] == '\n')
									break;
								if (this.source[this.currentPosition + lookAhead] == character) {
									this.currentPosition += lookAhead + 1;
									break;
								}
							}

						}
						throw e; // rethrow
					}
					if (character == '\'') {
						return (currentToken=TokenNameCharacterLiteral);
					} else {
						return (currentToken=TokenNameStringLiteral);
					}


				case '/' :
					{
						int test;
						if ((test = getNextChar('/', '*')) == 0) { //line comment
							this.lastCommentLinePosition = this.currentPosition;
							try { //get the next char
								this.currentCharacter = this.source[this.currentPosition++];
								while (this.currentCharacter != '\r' && this.currentCharacter != '\n') {
									this.currentCharacter = this.source[this.currentPosition++];
								}
								/*
								 * We need to completely consume the line break
								 */
								boolean isUnicode = false;
								if (this.currentCharacter == '\r'
								   && this.eofPosition > this.currentPosition) {
								   	if (this.source[this.currentPosition] == '\n') {
										this.currentPosition++;
										this.currentCharacter = '\n';
								   	} else if ((this.source[this.currentPosition] == '\\')
										&& (this.source[this.currentPosition + 1] == 'u')) {
										isUnicode = true;
										char unicodeChar;
										int index = this.currentPosition + 1;
										index++;
										while (this.source[index] == 'u') {
											index++;
										}
										//-------------unicode traitement ------------
										int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
										if ((c1 = ScannerHelper.getNumericValue(this.source[index++])) > 15
											|| c1 < 0
											|| (c2 = ScannerHelper.getNumericValue(this.source[index++])) > 15
											|| c2 < 0
											|| (c3 = ScannerHelper.getNumericValue(this.source[index++])) > 15
											|| c3 < 0
											|| (c4 = ScannerHelper.getNumericValue(this.source[index++])) > 15
											|| c4 < 0) {
											this.currentPosition = index;
											throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
										} else {
											unicodeChar = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
										}
										if (unicodeChar == '\n') {
											this.currentPosition = index;
											this.currentCharacter = '\n';
										}
									}
							   	}
								recordComment(TokenNameCOMMENT_LINE);
								if (this.startPosition <= this.cursorLocation && this.cursorLocation < this.currentPosition-1){
									throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_COMMENT);
								}
								if (this.taskTags != null) checkTaskTag(this.startPosition, this.currentPosition);
								if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
									//checkNonExternalizedString();
									if (this.recordLineSeparator) {
										if (isUnicode) {
											pushUnicodeLineSeparator();
										} else {
											pushLineSeparator();
										}
									}
								}
								if (this.tokenizeComments) {
									return (currentToken=TokenNameCOMMENT_LINE);
								}
							} catch (IndexOutOfBoundsException e) {
								this.currentPosition--;
								recordComment(TokenNameCOMMENT_LINE);
								if (this.taskTags != null) checkTaskTag(this.startPosition, this.currentPosition);
								if (this.tokenizeComments) {
									return (currentToken=TokenNameCOMMENT_LINE);
								} else {
									this.currentPosition++;
								}
							}
							break;
						}
						if (test > 0) { //traditional and javadoc comment
							try { //get the next char
								boolean isJavadoc = false, star = false;
								boolean isUnicode = false;
								// consume next character
								this.currentCharacter = this.source[this.currentPosition++];

								if (this.currentCharacter == '*') {
									isJavadoc = true;
									star = true;
								}
								if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
									//checkNonExternalizedString();
									if (this.recordLineSeparator) {
										if (!isUnicode) {
											pushLineSeparator();
										}
									}
								}
								isUnicode = false;
								this.currentCharacter = this.source[this.currentPosition++];
								// empty comment is not a javadoc /**/
								if (this.currentCharacter == '/') {
									isJavadoc = false;
								}
								//loop until end of comment */
								while ((this.currentCharacter != '/') || (!star)) {
									if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
										//checkNonExternalizedString();
										if (this.recordLineSeparator) {
											if (!isUnicode) {
												pushLineSeparator();
											}
										}
									}
									star = this.currentCharacter == '*';
									//get next char
									this.currentCharacter = this.source[this.currentPosition++];
								}
								int token = isJavadoc ? TokenNameCOMMENT_JAVADOC : TokenNameCOMMENT_BLOCK;
								recordComment(token);
								if (!isJavadoc && this.startPosition <= this.cursorLocation && this.cursorLocation < this.currentPosition-1){
									throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_COMMENT);
								}
								if (this.taskTags != null) checkTaskTag(this.startPosition, this.currentPosition);
								if (this.tokenizeComments) {
									/*
									if (isJavadoc)
										return TokenNameCOMMENT_JAVADOC;
									return TokenNameCOMMENT_BLOCK;
									*/
									return (currentToken=token);
								}
							} catch (IndexOutOfBoundsException e) {
								this.currentPosition--;
								throw new InvalidInputException(UNTERMINATED_COMMENT);
							}
							break;
						}
						if (checkIfDivide(previousToken)){
							if (getNextChar('='))
							{
								currentToken=TokenNameDIVIDE_EQUAL;
								return currentToken;
							}
							currentToken=TokenNameDIVIDE;
							return currentToken;
						}

						// check if regular expression
						if (checkIfRegExp()) {
							currentToken = TokenNameRegExLiteral;
							return currentToken;
						}
					}
				case '\u001a' :
					if (atEnd())
						return (currentToken=TokenNameEOF);
					//the atEnd may not be <this.currentPosition == this.eofPosition> if source is only some part of a real (external) stream
					throw new InvalidInputException("Ctrl-Z"); //$NON-NLS-1$

				default :
					if (ScannerHelper.isJavaIdentifierStart(this.currentCharacter))
						return (currentToken=scanIdentifierOrKeyword());
					if (ScannerHelper.isDigit(this.currentCharacter)) {
						return (currentToken=scanNumber(false));
					}
					return (currentToken=TokenNameERROR);
			}
		}
	} //-----------------end switch while try--------------------
	catch (IndexOutOfBoundsException e) {
		if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
			// reposition scanner in case we are interested by spaces as tokens
			this.currentPosition--;
			this.startPosition = whiteStart;
			return (currentToken=TokenNameWHITESPACE);
		}
	}
	/* might be completing at very end of file (e.g. behind a dot) */
	if (this.completionIdentifier == null &&
		this.startPosition == this.cursorLocation + 1){
		this.currentPosition = this.startPosition; // for being detected as empty free identifier
		return (currentToken=TokenNameIdentifier);
	}
	return (currentToken=TokenNameEOF);
}
public final void getNextUnicodeChar() throws InvalidInputException {
	int temp = this.currentPosition; // the \ is already read
	super.getNextUnicodeChar();
	if(this.cursorLocation > temp) {
		this.unicodeCharSize += (this.currentPosition - temp);
	}
	if (temp < this.cursorLocation && this.cursorLocation < this.currentPosition-1){
		throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_UNICODE);
	}
}
public final void jumpOverBlock() {
	this.jumpOverMethodBody();
}


///*
// * In case we actually read a keyword, but the cursor is located inside,
// * we pretend we read an identifier.
// */
public int scanIdentifierOrKeyword() {

	int id = super.scanIdentifierOrKeyword();

	if (this.startPosition <= this.cursorLocation+1
			&& this.cursorLocation < this.currentPosition){

		// extends the end of the completion token even if the end is after eofPosition
		if (this.cursorLocation+1 == this.eofPosition) {
			int temp = this.eofPosition;
			this.eofPosition = this.source.length;
		 	while(getNextCharAsJavaIdentifierPart()){/*empty*/}
			this.eofPosition = temp;
		}
		// convert completed keyword into an identifier
		return TokenNameIdentifier;
	}
	return id;
}


public int scanNumber(boolean dotPrefix) throws InvalidInputException {

	int token = super.scanNumber(dotPrefix);

	// consider completion just before a number to be ok, will insert before it
	if (this.startPosition <= this.cursorLocation && this.cursorLocation < this.currentPosition){
		throw new InvalidCursorLocation(InvalidCursorLocation.NO_COMPLETION_INSIDE_NUMBER);
	}
	return token;
}

public void setSource(char[] sourceString) {
	super.setSource(sourceString);
	if (!Parser.DO_DIET_PARSE && AssistParser.STOP_AT_CURSOR)
		this.eofPosition=this.cursorLocation+1;
}

}
