/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Olivier Oeuillot <oeuillot@free.fr>  - bug 244411
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.parser;

import java.util.List;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArraySingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

/**
 * Parser specialized for decoding javadoc comments
 */
public class JavadocParser extends AbstractCommentParser {

	// Public fields
	public Javadoc docComment;

	// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51600
	// Store param references for tag with invalid syntax
	private int invalidParamReferencesPtr = -1;
	private ASTNode[] invalidParamReferencesStack;

	// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=153399
	// Store value tag positions
	private long validValuePositions, invalidValuePositions;

	private int flags=0;

	private TypeReference namespace=null;
	private TypeReference memberOf=null;
	private TypeReference returnType=null;
	private TypeReference extendsType=null;

	private TypeReference classDef=null;
	private TypeReference methodDef=null;
	JavadocSingleNameReference property=null;
	private boolean isConstructor;



	protected boolean commentParse() {
		this.isConstructor=false;
		this.namespace=null;
		this.flags=0;

		this.namespace=null;
		this.memberOf=null;
		this.returnType=null;
		this.extendsType=null;
		this.property=null;
		this.classDef=null;
		this.methodDef=null;
		// TODO Auto-generated method stub
		boolean result= super.commentParse();
	return result;
	}

	public JavadocParser(Parser sourceParser) {
		super(sourceParser);
		this.kind = COMPIL_PARSER | TEXT_VERIF;
	}

	/* (non-Javadoc)
	 * Returns true if tag @deprecated is present in javadoc comment.
	 *
	 * If javadoc checking is enabled, will also construct an Javadoc node, which will be stored into Parser.javadoc
	 * slot for being consumed later on.
	 */
	public boolean checkDeprecation(int commentPtr) {

		// Store javadoc positions
		this.javadocStart = this.sourceParser.scanner.commentStarts[commentPtr];
		this.javadocEnd = this.sourceParser.scanner.commentStops[commentPtr]-1;
		this.firstTagPosition = this.sourceParser.scanner.commentTagStarts[commentPtr];
		this.validValuePositions = -1;
		this.invalidValuePositions = -1;

		// Init javadoc if necessary
		if (this.checkDocComment) {
			this.docComment = new Javadoc(javadocStart, javadocEnd);
		} else {
			this.docComment = null;
		}

		// If there's no tag in javadoc, return without parsing it
		if (this.firstTagPosition == 0) {
			switch (this.kind & PARSER_KIND) {
				case COMPIL_PARSER:
				case SOURCE_PARSER:
					return false;
			}
		}

		// Parse
		try {
			this.source = this.sourceParser.scanner.source;
			if (this.checkDocComment) {
				// Initialization
				this.scanner.lineEnds = this.sourceParser.scanner.lineEnds;
				this.scanner.linePtr = this.sourceParser.scanner.linePtr;
				this.lineEnds = this.scanner.lineEnds;
				commentParse();
			} else {

				// Parse comment
				Scanner sourceScanner = this.sourceParser.scanner;
				int firstLineNumber = Util.getLineNumber(javadocStart, sourceScanner.lineEnds, 0, sourceScanner.linePtr);
				int lastLineNumber = Util.getLineNumber(javadocEnd, sourceScanner.lineEnds, 0, sourceScanner.linePtr);
				this.index = javadocStart +3;

				// scan line per line, since tags must be at beginning of lines only
				this.deprecated = false;
				nextLine : for (int line = firstLineNumber; line <= lastLineNumber; line++) {
					int lineStart = line == firstLineNumber
							? javadocStart + 3 // skip leading /**
							: this.sourceParser.scanner.getLineStart(line);
					this.index = lineStart;
					this.lineEnd = line == lastLineNumber
							? javadocEnd - 2 // remove trailing * /
							: this.sourceParser.scanner.getLineEnd(line);
					nextCharacter : while (this.index < this.lineEnd) {
						char c = readChar(); // consider unicodes
						switch (c) {
							case '*' :
							case '\t' :			/* HORIZONTAL TABULATION   */
							case '\u000b' :		/* FORM FEED               */
							case '\u000c' :		/* VERTICAL TAB            */
							case ' ' :			/* SPACE                   */
							case '\u00a0' :		/* NO-BREAK SPACE          */
							case '\n' :			/* LINE FEED   */
							case '\r' :			/* CR */
								// do nothing for space or '*' characters
						        continue nextCharacter;
						    case '@' :
						    	parseSimpleTag();
						    	if (this.tagValue == TAG_DEPRECATED_VALUE) {
						    		if (this.abort) break nextCharacter;
						    	}
						}
			        	continue nextLine;
					}
				}
				return this.deprecated;
			}
		} finally {
			this.source = null; // release source as soon as finished
		}
		return this.deprecated;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.parser.AbstractCommentParser#createArgumentReference(char[], java.lang.Object, int)
	 */
	protected Object createArgumentReference(char[] name, int dim, boolean isVarargs, Object typeRef, long[] dimPositions, long argNamePos) throws InvalidInputException {
		try {
			TypeReference argTypeRef = (TypeReference) typeRef;
			if (dim > 0) {
				long pos = (((long) argTypeRef.sourceStart) << 32) + argTypeRef.sourceEnd;
				if (typeRef instanceof JavadocSingleTypeReference) {
					JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) typeRef;
					argTypeRef = new JavadocArraySingleTypeReference(singleRef.token, dim, pos);
				} else {
					JavadocQualifiedTypeReference qualifRef = (JavadocQualifiedTypeReference) typeRef;
					argTypeRef = new JavadocArrayQualifiedTypeReference(qualifRef, dim);
				}
			}
			int argEnd = argTypeRef.sourceEnd;
			if (dim > 0) {
				argEnd = (int) dimPositions[dim-1];
				if (isVarargs) {
					argTypeRef.bits |= ASTNode.IsVarArgs; // set isVarArgs
				}
			}
			if (argNamePos >= 0) argEnd = (int) argNamePos;
			return new JavadocArgumentExpression(name, argTypeRef.sourceStart, argEnd, argTypeRef);
		}
		catch (ClassCastException ex) {
			throw new InvalidInputException();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.parser.AbstractCommentParser#createFieldReference()
	 */
	protected Object createFieldReference(Object receiver) throws InvalidInputException {
		try {
			// Get receiver type
			TypeReference typeRef = (TypeReference) receiver;
			if (typeRef == null) {
				char[] name = this.sourceParser.compilationUnit.getMainTypeName();
				typeRef = new JavadocImplicitTypeReference(name, this.memberStart);
			}
			// Create field
			JavadocFieldReference field = new JavadocFieldReference(this.identifierStack[0], this.identifierPositionStack[0]);
			field.receiver = typeRef;
			field.tagSourceStart = this.tagSourceStart;
			field.tagSourceEnd = this.tagSourceEnd;
			field.tagValue = this.tagValue;
			return field;
		}
		catch (ClassCastException ex) {
			throw new InvalidInputException();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.parser.AbstractCommentParser#createMethodReference(java.lang.Object[])
	 */
	protected Object createMethodReference(Object receiver, List arguments) throws InvalidInputException {
		try {
			// Get receiver type
			TypeReference typeRef = (TypeReference) receiver;
			// Decide whether we have a constructor or not
			boolean isConstructor = false;
			int length = this.identifierLengthStack[0];	// may be > 0 for member class constructor reference
			if (typeRef == null) {
				char[] name = this.sourceParser.compilationUnit.getMainTypeName();
				TypeDeclaration typeDecl = getParsedTypeDeclaration();
				if (typeDecl != null) {
					name = typeDecl.name;
				}
				isConstructor = CharOperation.equals(this.identifierStack[length-1], name);
				typeRef = new JavadocImplicitTypeReference(name, this.memberStart);
			} else {
				if (typeRef instanceof JavadocSingleTypeReference) {
					char[] name = ((JavadocSingleTypeReference)typeRef).token;
					isConstructor = CharOperation.equals(this.identifierStack[length-1], name);
				} else if (typeRef instanceof JavadocQualifiedTypeReference) {
					char[][] tokens = ((JavadocQualifiedTypeReference)typeRef).tokens;
					int last = tokens.length-1;
					isConstructor = CharOperation.equals(this.identifierStack[length-1], tokens[last]);
					if (isConstructor) {
						boolean valid = true;
						if (valid) {
							for (int i=0; i<length-1 && valid; i++) {
								valid = CharOperation.equals(this.identifierStack[i], tokens[i]);
							}
						}
						if (!valid) {
							return null;
						}
					}
				} else {
					throw new InvalidInputException();
				}
			}
			// Create node
			if (arguments == null) {
				if (isConstructor) {
					JavadocAllocationExpression allocation = new JavadocAllocationExpression(this.identifierPositionStack[length-1]);
					allocation.type = typeRef;
					allocation.tagValue = this.tagValue;
					allocation.sourceEnd = this.scanner.getCurrentTokenEndPosition();
					if (length == 1) {
						allocation.qualification = new char[][] { this.identifierStack[0] };
					} else {
						System.arraycopy(this.identifierStack, 0, allocation.qualification = new char[length][], 0, length);
						allocation.sourceStart = (int) (this.identifierPositionStack[0] >>> 32);
					}
					allocation.memberStart = this.memberStart;
					return allocation;
				} else {
					JavadocMessageSend msg = new JavadocMessageSend(this.identifierStack[length-1], this.identifierPositionStack[length-1]);
					msg.receiver = typeRef;
					msg.tagValue = this.tagValue;
					msg.sourceEnd = this.scanner.getCurrentTokenEndPosition();
					return msg;
				}
			} else {
				JavadocArgumentExpression[] expressions = new JavadocArgumentExpression[arguments.size()];
				arguments.toArray(expressions);
				if (isConstructor) {
					JavadocAllocationExpression allocation = new JavadocAllocationExpression(this.identifierPositionStack[length-1]);
					allocation.arguments = expressions;
					allocation.type = typeRef;
					allocation.tagValue = this.tagValue;
					allocation.sourceEnd = this.scanner.getCurrentTokenEndPosition();
					if (length == 1) {
						allocation.qualification = new char[][] { this.identifierStack[0] };
					} else {
						System.arraycopy(this.identifierStack, 0, allocation.qualification = new char[length][], 0, length);
						allocation.sourceStart = (int) (this.identifierPositionStack[0] >>> 32);
					}
					allocation.memberStart = this.memberStart;
					return allocation;
				} else {
					JavadocMessageSend msg = new JavadocMessageSend(this.identifierStack[length-1], this.identifierPositionStack[length-1], expressions);
					msg.receiver = typeRef;
					msg.tagValue = this.tagValue;
					msg.sourceEnd = this.scanner.getCurrentTokenEndPosition();
					return msg;
				}
			}
		}
		catch (ClassCastException ex) {
			throw new InvalidInputException();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.parser.AbstractCommentParser#createReturnStatement()
	 */
	protected Object createReturnStatement() {
		return new JavadocReturnStatement(this.scanner.getCurrentTokenStartPosition(),
					this.scanner.getCurrentTokenEndPosition());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.parser.AbstractCommentParser#parseTagName()
	 */
	protected void createTag() {
		this.tagValue = TAG_OTHERS_VALUE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.parser.AbstractCommentParser#createTypeReference()
	 */
	protected Object createTypeReference(int primitiveToken) {
		TypeReference typeRef = null;
		int size = this.identifierLengthStack[this.identifierLengthPtr];
		if (size == 1) { // Single Type ref
			typeRef = new JavadocSingleTypeReference(
						this.identifierStack[this.identifierPtr],
						this.identifierPositionStack[this.identifierPtr],
						this.tagSourceStart,
						this.tagSourceEnd);
		} else if (size > 1) { // Qualified Type ref
			char[][] tokens = new char[size][];
			System.arraycopy(this.identifierStack, this.identifierPtr - size + 1, tokens, 0, size);
			long[] positions = new long[size];
			System.arraycopy(this.identifierPositionStack, this.identifierPtr - size + 1, positions, 0, size);
			typeRef = new JavadocQualifiedTypeReference(tokens, positions, this.tagSourceStart, this.tagSourceEnd);
		}
		return typeRef;
	}

	/*
	 * Get current parsed type declaration.
	 */
	protected TypeDeclaration getParsedTypeDeclaration() {
		int ptr = this.sourceParser.astPtr;
		while (ptr >= 0) {
			Object node = this.sourceParser.astStack[ptr];
			if (node instanceof TypeDeclaration) {
				TypeDeclaration typeDecl = (TypeDeclaration) node;
				if (typeDecl.bodyEnd == 0) { // type declaration currenly parsed
					return typeDecl;
				}
			}
			ptr--;
		}
		return null;
	}

	/*
	 * Parse @return tag declaration
	 */
	protected boolean parseReturn() {
		if (this.returnStatement == null) {
			Object[] type = parseTypeReference();
			this.returnType = (TypeReference) (type != null && type.length > 0 ? type[0] : null);
			this.returnStatement = createReturnStatement();
			if((this.kind & TEXT_VERIF) != 0 && type != null && type.length > 0 && type[0] != null)
				refreshReturnStatement();
			return true;
		}
		if (this.reportProblems) {
			this.sourceParser.problemReporter().javadocDuplicatedReturnTag(
				this.scanner.getCurrentTokenStartPosition(),
				this.scanner.getCurrentTokenEndPosition());
		}
		return false;
	}


	protected void parseSimpleTag() {

		// Read first char
		// readChar() code is inlined to balance additional method call in checkDeprectation(int)
		char first = this.source[this.index++];
		if (first == '\\' && this.source[this.index] == 'u') {
			int c1, c2, c3, c4;
			int pos = this.index;
			this.index++;
			while (this.source[this.index] == 'u')
				this.index++;
			if (!(((c1 = ScannerHelper.getNumericValue(this.source[this.index++])) > 15 || c1 < 0)
					|| ((c2 = ScannerHelper.getNumericValue(this.source[this.index++])) > 15 || c2 < 0)
					|| ((c3 = ScannerHelper.getNumericValue(this.source[this.index++])) > 15 || c3 < 0) || ((c4 = ScannerHelper.getNumericValue(this.source[this.index++])) > 15 || c4 < 0))) {
				first = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
			} else {
				this.index = pos;
			}
		}

		// switch on first tag char
		switch (first) {
			case 'd':
		        if ((readChar() == 'e') &&
						(readChar() == 'p') && (readChar() == 'r') &&
						(readChar() == 'e') && (readChar() == 'c') &&
						(readChar() == 'a') && (readChar() == 't') &&
						(readChar() == 'e') && (readChar() == 'd')) {
					// ensure the tag is properly ended: either followed by a space, a tab, line end or asterisk.
					char c = readChar();
					if (ScannerHelper.isWhitespace(c) || c == '*') {
						this.abort = true;
			    		this.deprecated = true;
						this.tagValue = TAG_DEPRECATED_VALUE;
					}
		        }
				break;
		}
	}

	protected boolean parseTag(int previousPosition) throws InvalidInputException {
		boolean valid = false;

		// Read tag name
		int currentPosition = this.index;
		int token = readTokenAndConsume();
		if (currentPosition != this.scanner.startPosition) {
			this.tagSourceStart = previousPosition;
			this.tagSourceEnd = currentPosition;
			if (this.reportProblems) this.sourceParser.problemReporter().javadocInvalidTag(this.tagSourceStart, this.tagSourceEnd);
			return false;
		}
		if (this.index >= this.scanner.eofPosition) {
			this.tagSourceStart = previousPosition;
			this.tagSourceEnd = this.tokenPreviousPosition;
			if (this.reportProblems) this.sourceParser.problemReporter().javadocInvalidTag(this.tagSourceStart, this.tagSourceEnd);
			return false;
		}
		this.tagSourceStart = this.scanner.getCurrentTokenStartPosition();
		this.tagSourceEnd = this.scanner.getCurrentTokenEndPosition();
		char[] tagName = this.scanner.getCurrentIdentifierSource();

		// Try to get tag name other than java identifier
		// (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51660)
		if (this.scanner.currentCharacter != ' ' && !ScannerHelper.isWhitespace(this.scanner.currentCharacter)) {
			boolean validTag = true;
			tagNameToken: while (token != TerminalTokens.TokenNameEOF && this.index < this.scanner.eofPosition) {
				int length = tagName.length;
				// !, ", #, %, &, ', -, :, <, >, * chars and spaces are not allowed in tag names
				switch (this.scanner.currentCharacter) {
					case '}':
					case '*': // break for '*' as this is perhaps the end of comment (bug 65288)
						break tagNameToken;
					case '!':
					case '#':
					case '%':
					case '&':
					case '\'':
					case '"':
					case ':':
					case '<':
					case '>':
					case '@':
						validTag = false;
						this.tagSourceEnd = this.scanner.getCurrentTokenEndPosition();
						this.index = this.scanner.currentPosition;
						break;
					case '-': // allowed in tag names as this character is often used in doclets (bug 68087)
						System.arraycopy(tagName, 0, tagName = new char[length+1], 0, length);
						tagName[length] = this.scanner.currentCharacter;
						this.tagSourceEnd = this.scanner.getCurrentTokenEndPosition();
						this.index = this.scanner.currentPosition;
						break;
					default:
						if (this.scanner.currentCharacter == ' ' || ScannerHelper.isWhitespace(this.scanner.currentCharacter)) {
							break tagNameToken;
						}
						token = readTokenAndConsume();
						char[] ident = this.scanner.getCurrentIdentifierSource();
						System.arraycopy(tagName, 0, tagName = new char[length+ident.length], 0, length);
						System.arraycopy(ident, 0, tagName, length, ident.length);
						this.tagSourceEnd = this.scanner.getCurrentTokenEndPosition();
						break;
				}
				this.scanner.getNextChar();
			}
			if (!validTag) {
				if (this.reportProblems) this.sourceParser.problemReporter().javadocInvalidTag(this.tagSourceStart, this.tagSourceEnd);
				return false;
			}
		}
		int length = tagName.length;
		if (length == 0) return false; // may happen for some parser (completion for example)
		this.index = this.tagSourceEnd+1;
		this.scanner.currentPosition = this.tagSourceEnd+1;

		// Decide which parse to perform depending on tag name
		this.tagValue = NO_TAG_VALUE;
		switch (token) {
			case TerminalTokens.TokenNameIdentifier :
				switch (tagName[0]) { 
				case 'a':
					if (length == TAG_ADDON_LENGTH && CharOperation.equals(TAG_ADDON, tagName)) {
						this.tagValue = TAG_ADDON_VALUE;
						valid=true;
					} else
						if (length == TAG_ALIAS_LENGTH && CharOperation.equals(TAG_ALIAS, tagName)) {
							this.tagValue = TAG_ALIAS_VALUE;
							valid=true;
						}
						else if (length == TAG_ARGUMENT_LENGTH && CharOperation.equals(TAG_ARGUMENT, tagName)) {
						this.tagValue = TAG_ARGUMENT_VALUE;
						valid=parseParam();
					}
				break;
				case 'b':
					if (length == TAG_BASE_LENGTH && CharOperation.equals(TAG_BASE, tagName)) {
						this.tagValue = TAG_BASE_VALUE;
						valid=parseExtends();
					}
				break;
					case 'c':
						if (length == TAG_CLASSDECRIPTION_LENGTH && CharOperation.equals(TAG_CLASSDECRIPTION, tagName)) {
							this.tagValue = TAG_CLASSDECRIPTION_VALUE;
							valid =true;
						} else	if (length == TAG_CLASS_LENGTH && CharOperation.equals(TAG_CLASS, tagName)) {
							this.tagValue = TAG_CLASS_VALUE;
							valid =parseClass() ;
						} else
								if (length == TAG_CONSTRUCTOR_LENGTH && CharOperation.equals(TAG_CONSTRUCTOR, tagName)) {
									this.tagValue = TAG_CONSTRUCTOR_VALUE;
									this.isConstructor=true;
									valid =true;
								}
					break;
					case 'd':
						if (length == TAG_DEPRECATED_LENGTH && CharOperation.equals(TAG_DEPRECATED, tagName)) {
							this.deprecated = true;
							valid = true;
							this.tagValue = TAG_DEPRECATED_VALUE;
						}
						break;
					case 'e':
						if (length == TAG_EXCEPTION_LENGTH && CharOperation.equals(TAG_EXCEPTION, tagName)) {
							this.tagValue = TAG_EXCEPTION_VALUE;
							valid = parseThrows();
						} else
							if (length == TAG_EXTENDS_LENGTH && CharOperation.equals(TAG_EXTENDS, tagName)) {
								this.tagValue = TAG_EXTENDS_VALUE;
								valid = parseExtends();
							} else
								if (length == TAG_EXEC_LENGTH && CharOperation.equals(TAG_EXEC, tagName)) {
									this.tagValue = TAG_EXEC_VALUE;
									valid = true;
								}

						break;
					case 'f':
						if (length == TAG_FILEOVERVIEW_LENGTH && CharOperation.equals(TAG_FILEOVERVIEW, tagName)) {
							this.tagValue = TAG_FILEOVERVIEW_VALUE;
							valid = true;
						} else
							if (length == TAG_FINAL_LENGTH && CharOperation.equals(TAG_FINAL, tagName)) {
								this.tagValue = TAG_FINAL_VALUE;
								this.flags|=ClassFileConstants.AccFinal;
								valid = true;
							} 
						break;
					case 'i':
						if (length == TAG_ID_LENGTH && CharOperation.equals(TAG_ID, tagName)) {
							this.tagValue = TAG_ID_VALUE;
							valid = true;
						}
						else if (length == TAG_IGNORE_LENGTH && CharOperation.equals(TAG_IGNORE, tagName)) {
							this.tagValue = TAG_IGNORE_VALUE;
							valid = true;
						}
						break;
					case 'l':
						if (length == TAG_LINK_LENGTH && CharOperation.equals(TAG_LINK, tagName)) {
							this.tagValue = TAG_LINK_VALUE;
							if (this.inlineTagStarted || (this.kind & COMPLETION_PARSER) != 0) {
								valid= parseReference();
							} else {
								// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53290
								// Cannot have @link outside inline comment
								valid = false;
								if (this.reportProblems) {
									this.sourceParser.problemReporter().javadocUnexpectedTag(this.tagSourceStart, this.tagSourceEnd);
								}
							}
						}
						break;
					case 'm':
						if (length == TAG_MEMBER_LENGTH && CharOperation.equals(TAG_MEMBER, tagName)) {
							this.tagValue = TAG_MEMBER_VALUE;
							valid = parseMember();
						}
						else if (length == TAG_MEMBEROF_LENGTH && CharOperation.equals(TAG_MEMBEROF, tagName)) {
							this.tagValue = TAG_MEMBEROF_VALUE;
							valid = parseMember();
						}
						else if (length == TAG_METHOD_LENGTH && CharOperation.equals(TAG_METHOD, tagName)) {
							this.tagValue = TAG_METHOD_VALUE;
							valid = parseMethod();
						}
						break;
					case 'n':
						if (length == TAG_NAMESPACE_LENGTH && CharOperation.equals(TAG_NAMESPACE, tagName)) {
							this.tagValue = TAG_NAMESPACE_VALUE;
							valid = parseNamespace();
						}
						break;
					case 'p':
						if (length == TAG_PARAM_LENGTH && CharOperation.equals(TAG_PARAM, tagName)) {
							this.tagValue = TAG_PARAM_VALUE;
							valid = parseParam();
						}
						else if (length == TAG_PRIVATE_LENGTH && CharOperation.equals(TAG_PRIVATE, tagName)) {
							this.tagValue = TAG_PRIVATE_VALUE;
							this.flags|=ClassFileConstants.AccPrivate;
							valid = true;
						}
						else if (length == TAG_PROJECT_DESCRIPTION_LENGTH && CharOperation.equals(TAG_PROJECT_DESCRIPTION, tagName)) {
							this.tagValue = TAG_PROJECT_DESCRIPTION_VALUE;
							valid = true;
						}
						else if (length == TAG_PROPERTY_LENGTH && CharOperation.equals(TAG_PROPERTY, tagName)) {
							this.tagValue = TAG_PROPERTY_VALUE;
							valid = parseProperty();
						}	
						else if (length == TAG_PROTECTED_LENGTH && CharOperation.equals(TAG_PROTECTED, tagName)) {
							this.tagValue = TAG_PRIVATE_VALUE;
							this.flags|=ClassFileConstants.AccProtected;
							valid = true;
						}
						break;
					case 'r':
						if (length == TAG_RETURNS_LENGTH && CharOperation.equals(TAG_RETURNS, tagName)) {
							this.tagValue = TAG_RETURNS_VALUE;
							valid = parseReturn();
						} else if (length == TAG_RETURN_LENGTH && CharOperation.equals(TAG_RETURN, tagName)) {
							this.tagValue = TAG_RETURNS_VALUE;
							valid = parseReturn();
						}
						else if (length == TAG_REQUIRES_LENGTH && CharOperation.equals(TAG_REQUIRES, tagName)) {
							this.tagValue = TAG_REQUIRES_VALUE;
							valid = parseRequires();
						}
						break;
					case 's':
						if (length == TAG_SEE_LENGTH && CharOperation.equals(TAG_SEE, tagName)) {
							if (this.inlineTagStarted) {
								// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53290
								// Cannot have @see inside inline comment
								valid = false;
								if (this.reportProblems) {
									this.sourceParser.problemReporter().javadocUnexpectedTag(this.tagSourceStart, this.tagSourceEnd);
								}
							} else {
								this.tagValue = TAG_SEE_VALUE;
								valid = parseReference();
							}
						}
						else if (length == TAG_SDOC_LENGTH && CharOperation.equals(TAG_SDOC, tagName)) {
							this.tagValue = TAG_SDOC_VALUE;
							valid = true;
						}
						break;

					case 't':
						if (length == TAG_TYPE_LENGTH && CharOperation.equals(TAG_TYPE, tagName)) {
							this.tagValue = TAG_TYPE_VALUE;
							valid=parseType();
						}
						break;
					default:
						createTag();
						break;
				}
				break;
			case TerminalTokens.TokenNamereturn :
				this.tagValue = TAG_RETURN_VALUE;
				valid = parseReturn();
				/* verify characters after return tag (we're expecting text description)
				if(!verifyCharsAfterReturnTag(this.index)) {
					if (this.sourceParser != null) {
						int end = this.starPosition == -1 || this.lineEnd<this.starPosition ? this.lineEnd : this.starPosition;
						this.sourceParser.problemReporter().javadocEmptyReturnTag(this.tagSourceStart, end);
					}
				}
				*/
				break;
			case TerminalTokens.TokenNamethrows :
				this.tagValue = TAG_THROWS_VALUE;
				valid = parseThrows();
				break;
			case TerminalTokens.TokenNameclass :
				this.tagValue = TAG_CLASS_VALUE;
				valid=true;
				break;
			case TerminalTokens.TokenNameextends :
				this.tagValue = TAG_EXTENDS_VALUE;
				valid=parseExtends();
				break;
			case TerminalTokens.TokenNameprivate :
				this.tagValue = TAG_PRIVATE_VALUE;
				this.flags|=ClassFileConstants.AccPrivate;
				valid=true;
				break;
			case TerminalTokens.TokenNameprotected :
				this.tagValue = TAG_PROTECTED_VALUE;
				this.flags|=ClassFileConstants.AccProtected;
				valid=true;
				break;
			case TerminalTokens.TokenNamefinal :
				this.tagValue = TAG_FINAL_VALUE;
				this.flags|=ClassFileConstants.AccFinal;
				valid=true;
				break;
		}
		this.textStart = this.index;
		return valid;
	}

	private boolean parseExtends() throws InvalidInputException {
		this.extendsType=(TypeReference)parseQualifiedName(true);
		return this.extendsType!=null;
	}

	private boolean parseType() throws InvalidInputException {
		this.returnType=(TypeReference)parseQualifiedName(true);
		return this.returnType!=null;
	}

	private boolean parseNamespace() throws InvalidInputException {
		this.namespace=(TypeReference) parseQualifiedName(true);
		return this.namespace!=null;
	}

    private boolean parseProperty() throws InvalidInputException {
		// Store current state
		int start = this.tagSourceStart;
		int end = this.tagSourceEnd;
		boolean tokenWhiteSpace = this.scanner.tokenizeWhiteSpace;
		this.scanner.tokenizeWhiteSpace = true;
		Object []typeReference=null;

	 
		// Get first non whitespace token
		this.identifierPtr = -1;
		this.identifierLengthPtr = -1;
		boolean hasMultiLines = this.scanner.currentPosition > (this.lineEnd+1);
		boolean valid = true, empty = true;
		boolean isParmType=false;
		int token = -1;
		nextToken: while (true) {
			this.currentTokenType = -1;
			try {
				token = readToken();
			} catch (InvalidInputException e) {
				valid = false;
			}
			switch (token) {
				case TerminalTokens.TokenNameIdentifier :
					if (valid) {
						// store param name id
						pushIdentifier(true, false);
						start = this.scanner.getCurrentTokenStartPosition();
						end = hasMultiLines ? this.lineEnd: this.scanner.getCurrentTokenEndPosition();
						break nextToken;
					}

					// fall through next case to report error
				default:
					if (valid && !hasMultiLines) start = this.scanner.getCurrentTokenStartPosition();
					valid = false;
					if (!hasMultiLines) {
						empty = false;
						end = hasMultiLines ? this.lineEnd: this.scanner.getCurrentTokenEndPosition();
						break;
					}
					end = this.lineEnd;
					// when several lines, fall through next case to report problem immediately
				case TerminalTokens.TokenNameWHITESPACE:
					if (this.scanner.currentPosition > (this.lineEnd+1)) hasMultiLines = true;
					if (valid) break;
					// if not valid fall through next case to report error
				case TerminalTokens.TokenNameEOF:
					if (this.reportProblems)
						if (empty)
							this.sourceParser.problemReporter().javadocMissingParamName(start, end, this.sourceParser.modifiers);
						else
							this.sourceParser.problemReporter().javadocInvalidParamTagName(start, end);

					this.currentTokenType = -1;
					this.scanner.tokenizeWhiteSpace = tokenWhiteSpace;
					return false;
				case TerminalTokens.TokenNameLBRACE:
					this.scanner.tokenizeWhiteSpace = false;
					  typeReference=parseTypeReference();
					  isParmType=true;
						this.identifierPtr = -1;
						this.identifierLengthPtr = -1;
						this.scanner.tokenizeWhiteSpace = true;
					break;

			}
		}


		// Verify that tag name is well followed by white spaces
		if (valid) {
			this.currentTokenType = -1;
			int restart = this.scanner.currentPosition;
			try {
				token = readToken();
			} catch (InvalidInputException e) {
				valid = false;
			}
			if (token == TerminalTokens.TokenNameWHITESPACE) {
				this.scanner.currentPosition = restart;
				this.index = restart;
				this.scanner.tokenizeWhiteSpace = tokenWhiteSpace;
				valid= pushParamName(false);
				if (valid  )
				{
					  JavadocSingleNameReference nameRef=(JavadocSingleNameReference)this.astStack[this.astPtr];
					  TypeReference [] refs=null;
					  if (typeReference!=null && isParmType)
					  {
						  refs = new TypeReference[typeReference.length];
						  System.arraycopy(typeReference, 0, refs, 0, typeReference.length);
					  }
					  nameRef.types=refs;
					  this.property=nameRef;
				}
				  return valid;
			}
		}


		// Report problem
		this.currentTokenType = -1;
 
		end = hasMultiLines ? this.lineEnd: this.scanner.getCurrentTokenEndPosition();
		while ((token=readToken()) != TerminalTokens.TokenNameWHITESPACE && token != TerminalTokens.TokenNameEOF) {
			this.currentTokenType = -1;
			end = hasMultiLines ? this.lineEnd: this.scanner.getCurrentTokenEndPosition();
		}
		if (this.reportProblems)
				this.sourceParser.problemReporter().javadocInvalidParamTagName(start, end);
		this.scanner.currentPosition = start;
		this.index = start;
		this.currentTokenType = -1;
		this.scanner.tokenizeWhiteSpace = tokenWhiteSpace;
		return false;

	}
	
	private boolean parseClass() throws InvalidInputException {
		this.classDef=(TypeReference) parseQualifiedName(true);
		return this.classDef!=null;
	}
	
	private boolean parseMethod() throws InvalidInputException {
		this.methodDef=(TypeReference) parseQualifiedName(true);
		return this.methodDef!=null;
	}
	
	private boolean parseMember() throws InvalidInputException {
		this.memberOf=(TypeReference) parseQualifiedName(true);
		return this.memberOf!=null;
	}

	/*
	 * Push a param name in ast node stack.
	 */
	protected boolean pushParamName(boolean isTypeParam) {
		// Create param reference
		ASTNode nameRef = null;
		if (isTypeParam) {
			JavadocSingleTypeReference ref = new JavadocSingleTypeReference(this.identifierStack[1],
				this.identifierPositionStack[1],
				this.tagSourceStart,
				this.tagSourceEnd);
			nameRef = ref;
		} else {
			JavadocSingleNameReference ref = new JavadocSingleNameReference(this.identifierStack[0],
				this.identifierPositionStack[0],
				this.tagSourceStart,
				this.tagSourceEnd);
			nameRef = ref;
		}
		// Push ref on stack
		if (this.astLengthPtr == -1) { // First push
			pushOnAstStack(nameRef, true);
		} else {
			// Verify that no @throws has been declared before
			
			for (int i=THROWS_TAG_EXPECTED_ORDER; i<=this.astLengthPtr; i+=ORDERED_TAGS_NUMBER) {
				if (this.astLengthStack[i] != 0) {
					if (this.reportProblems) this.sourceParser.problemReporter().javadocUnexpectedTag(this.tagSourceStart, this.tagSourceEnd);
					// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51600
					// store invalid param references in specific array
					if (this.invalidParamReferencesPtr == -1l) {
						this.invalidParamReferencesStack = new JavadocSingleNameReference[10];
					}
					int stackLength = this.invalidParamReferencesStack.length;
					if (++this.invalidParamReferencesPtr >= stackLength) {
						System.arraycopy(
							this.invalidParamReferencesStack, 0,
							this.invalidParamReferencesStack = new JavadocSingleNameReference[stackLength + AST_STACK_INCREMENT], 0,
							stackLength);
					}
					this.invalidParamReferencesStack[this.invalidParamReferencesPtr] = nameRef;
					return false;
				}
			}
		
			switch (this.astLengthPtr % ORDERED_TAGS_NUMBER) {
				case PARAM_TAG_EXPECTED_ORDER :
					// previous push was a @param tag => push another param name
					pushOnAstStack(nameRef, false);
					break;
				case SEE_TAG_EXPECTED_ORDER :
					// previous push was a @see tag => push new param name
					pushOnAstStack(nameRef, true);
					break;
				default:
					return false;
			}
		}
		return true;
	}

	/*
	 * Push a reference statement in ast node stack.
	 */
	protected boolean pushSeeRef(Object statement) {
		if (this.astLengthPtr == -1) { // First push
			pushOnAstStack(null, true);
			pushOnAstStack(null, true);
			pushOnAstStack(statement, true);
		} else {
			switch (this.astLengthPtr % ORDERED_TAGS_NUMBER) {
				case PARAM_TAG_EXPECTED_ORDER :
					// previous push was a @param tag => push empty @throws tag and new @see tag
					pushOnAstStack(null, true);
					pushOnAstStack(statement, true);
					break;
				case THROWS_TAG_EXPECTED_ORDER :
					// previous push was a @throws tag => push new @see tag
					pushOnAstStack(statement, true);
					break;
				case SEE_TAG_EXPECTED_ORDER :
					// previous push was a @see tag => push another @see tag
					pushOnAstStack(statement, false);
					break;
				default:
					return false;
			}
		}
		return true;
	}

	/*
	 * Push a throws type ref in ast node stack.
	 */
	protected boolean pushThrowName(Object typeRef) {
		if (this.astLengthPtr == -1) { // First push
			pushOnAstStack(null, true);
			pushOnAstStack(typeRef, true);
		} else {
			switch (this.astLengthPtr % ORDERED_TAGS_NUMBER) {
				case PARAM_TAG_EXPECTED_ORDER :
					// previous push was a @param tag => push new @throws tag
					pushOnAstStack(typeRef, true);
					break;
				case THROWS_TAG_EXPECTED_ORDER :
					// previous push was a @throws tag => push another @throws tag
					pushOnAstStack(typeRef, false);
					break;
				case SEE_TAG_EXPECTED_ORDER :
					// previous push was a @see tag => push empty @param and new @throws tags
					pushOnAstStack(null, true);
					pushOnAstStack(typeRef, true);
					break;
				default:
					return false;
			}
		}
		return true;
	}

	/*
	 * Refresh return statement
	 */
	protected void refreshReturnStatement() {
		((JavadocReturnStatement) this.returnStatement).bits &= ~ASTNode.Empty;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("check javadoc: ").append(this.checkDocComment).append("\n");	//$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("javadoc: ").append(this.docComment).append("\n");	//$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(super.toString());
		return buffer.toString();
	}

	/*
	 * Fill associated comment fields with ast nodes information stored in stack.
	 */
	protected void updateDocComment() {

		this.docComment.modifiers=this.flags;


		this.docComment.namespace=this.namespace;
		this.docComment.memberOf=this.memberOf;
		this.docComment.returnType=this.returnType;
		this.docComment.extendsType=this.extendsType;
		this.docComment.classDef=this.classDef;
		this.docComment.methodDef=this.methodDef;
		this.docComment.property=this.property;

		this.docComment.isConstructor=this.isConstructor;

		// Set positions
		this.docComment.inheritedPositions = this.inheritedPositions;
		this.docComment.valuePositions = this.validValuePositions != -1 ? this.validValuePositions : this.invalidValuePositions;

		// Set return node if present
		if (this.returnStatement != null) {
			this.docComment.returnStatement = (JavadocReturnStatement) this.returnStatement;
		}

		// Copy array of invalid syntax param tags
		if (this.invalidParamReferencesPtr >= 0) {
			this.docComment.invalidParameters = new JavadocSingleNameReference[this.invalidParamReferencesPtr+1];
			System.arraycopy(this.invalidParamReferencesStack, 0, this.docComment.invalidParameters, 0, this.invalidParamReferencesPtr+1);
		}

		// If no nodes stored return
		if (this.astLengthPtr == -1) {
			return;
		}

		// Initialize arrays
		int[] sizes = new int[ORDERED_TAGS_NUMBER];
		for (int i=0; i<=this.astLengthPtr; i++) {
			sizes[i%ORDERED_TAGS_NUMBER] += this.astLengthStack[i];
		}
		this.docComment.seeReferences = new Expression[sizes[SEE_TAG_EXPECTED_ORDER]];
		this.docComment.exceptionReferences = new TypeReference[sizes[THROWS_TAG_EXPECTED_ORDER]];
		int paramRefPtr = sizes[PARAM_TAG_EXPECTED_ORDER];
		this.docComment.paramReferences = new JavadocSingleNameReference[paramRefPtr];

		// Store nodes in arrays
		while (this.astLengthPtr >= 0) {
			int ptr = this.astLengthPtr % ORDERED_TAGS_NUMBER;
			// Starting with the stack top, so get references (eg. Expression) coming from @see declarations
			switch(ptr) {
				case SEE_TAG_EXPECTED_ORDER:
					int size = this.astLengthStack[this.astLengthPtr--];
					for (int i=0; i<size; i++) {
						this.docComment.seeReferences[--sizes[ptr]] = (Expression) this.astStack[this.astPtr--];
					}
					break;

				// Then continuing with class names (eg. TypeReference) coming from @throw/@exception declarations
				case THROWS_TAG_EXPECTED_ORDER:
					size = this.astLengthStack[this.astLengthPtr--];
					for (int i=0; i<size; i++) {
						this.docComment.exceptionReferences[--sizes[ptr]] = (TypeReference) this.astStack[this.astPtr--];
					}
					break;

				// Finally, finishing with parameters nales (ie. Argument) coming from @param declaration
				case PARAM_TAG_EXPECTED_ORDER:
					size = this.astLengthStack[this.astLengthPtr--];
					for (int i=0; i<size; i++) {
						Expression reference = (Expression) this.astStack[this.astPtr--];
						if (reference instanceof JavadocSingleNameReference)
							this.docComment.paramReferences[--paramRefPtr] = (JavadocSingleNameReference) reference;
					}
					break;
			}
		}

		// Resize param tag references arrays
		if (paramRefPtr != 0) { // there's no type parameters references
		// there both of references => resize arrays
			int size = sizes[PARAM_TAG_EXPECTED_ORDER];
			System.arraycopy(this.docComment.paramReferences, paramRefPtr, this.docComment.paramReferences = new JavadocSingleNameReference[size - paramRefPtr], 0, size - paramRefPtr);
		}
	}

	protected void createParamType(Object[] typeReference) {
		  JavadocSingleNameReference nameRef=(JavadocSingleNameReference)this.astStack[this.astPtr];
		  TypeReference [] refs=null;
		  if (typeReference!=null)
		  {
			  refs = new TypeReference[typeReference.length];
			  System.arraycopy(typeReference, 0, refs, 0, typeReference.length);
		  }
		  nameRef.types=refs;
	}
	
	protected boolean parseRequires()
	{
		return true;
	}
}
