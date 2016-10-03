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
package org.eclipse.wst.jsdt.internal.ui.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;

/**
 * This scanner recognizes the JSDoc comments, multi line comments, single line comments,
 * strings, characters, and regular expressions.
 */
public class FastJavaPartitionScanner implements IPartitionTokenScanner, IJavaScriptPartitions {

	// states
	private static final int JAVASCRIPT= 0;
	private static final int SINGLE_LINE_COMMENT= 1;
	private static final int MULTI_LINE_COMMENT= 2;
	private static final int JSDOC= 3;
	private static final int CHARACTER= 4;
	private static final int STRING= 5;
	private static final int REGULAR_EXPRESSION = 6;
	private static final int SHEBANG_LINE= 7;

	// beginning of prefixes and postfixes
	private static final int NONE= 0;
	private static final int BACKSLASH= 1; // postfix for STRING and CHARACTER
	private static final int SLASH= 2; // prefix for SINGLE_LINE or MULTI_LINE or JSDOC
	private static final int SLASH_STAR= 3; // prefix for MULTI_LINE_COMMENT or JSDOC
	private static final int SLASH_STAR_STAR= 4; // prefix for MULTI_LINE_COMMENT or JSDOC
	private static final int STAR= 5; // postfix for MULTI_LINE_COMMENT or JSDOC
	private static final int CARRIAGE_RETURN=6; // postfix for STRING, CHARACTER and SINGLE_LINE_COMMENT
	private static final int REGULAR_EXPRESSION_END=7;
	private static final int BACKSLASH_CARRIAGE_RETURN = 8; // anti-postfix for STRING, CHARACTER
	private static final int HASH= 9; // prefix for SHEBANG

	/** The scanner. */
	private final BufferedDocumentScanner fScanner= new BufferedDocumentScanner(1000);	// faster implementation

	/** The offset of the last returned token. */
	private int fTokenOffset;
	/** The length of the last returned token. */
	private int fTokenLength;

	/** The state of the scanner. */
	private int fState;
	/** The last significant characters read. */
	private int fLast;
	/** The amount of characters already read on first call to nextToken(). */
	private int fPrefixLength;

	private final IToken[] fTokens= new IToken[] {
		new Token(null),
		new Token(JAVA_SINGLE_LINE_COMMENT),
		new Token(JAVA_MULTI_LINE_COMMENT),
		new Token(JAVA_DOC),
		new Token(JAVA_CHARACTER),
		new Token(JAVA_STRING),
		new Token(JAVA_STRING),	// regular expression same as string
		new Token(JAVA_SHEBANG_LINE)
	};

	public FastJavaPartitionScanner() {
	    // create the scanner
	}

	/*
	 * @see org.eclipse.jface.text.rules.ITokenScanner#nextToken()
	 */
	public IToken nextToken() {
		fTokenOffset += fTokenLength;
		fTokenLength= fPrefixLength;
		
		int lastNonWhitespaceChar = NONE;
		int currentChar = NONE;
		boolean onCharList = false;
		while (true) {
			if (!Character.isWhitespace((char)currentChar))
				lastNonWhitespaceChar = currentChar;
			
			// read in the next char
			currentChar= fScanner.read();

			// characters
	 		switch (currentChar) {
	 		case ICharacterScanner.EOF:
		 		if (fTokenLength > 0) {
		 			fLast= NONE; // ignore last
		 			return preFix(fState, JAVASCRIPT, NONE, 0);

		 		} else {
		 			fLast= NONE;
		 			fPrefixLength= 0;
					return Token.EOF;
		 		}

	 		case '\r':
	 			if ((fState == STRING || fState == CHARACTER) && fLast == BACKSLASH) {
	 				fLast = BACKSLASH_CARRIAGE_RETURN;
	 				fTokenLength++;
	 				continue;
	 			}
	 			if (fLast != CARRIAGE_RETURN) {
						fLast= CARRIAGE_RETURN;
						fTokenLength++;
	 					continue;

	 			} else {
					switch (fState) {
					case SINGLE_LINE_COMMENT:
					case CHARACTER:
					case STRING:
					case REGULAR_EXPRESSION:
					case SHEBANG_LINE:
						if (fTokenLength > 0) {
							IToken token= fTokens[fState];
				 			
							fLast= CARRIAGE_RETURN;
							fPrefixLength= 1;

							fState= JAVASCRIPT;
							return token;

						} else {
							consume();
							continue;
						}

					default:
						consume();
						continue;
					}
	 			}

	 		case '\n':
	 		case '\u2028':
	 		case '\u2029':
				switch (fState) {
				case STRING:
				case CHARACTER:
					if(fLast == BACKSLASH || fLast == BACKSLASH_CARRIAGE_RETURN) {
						consume();
						continue;
					}
				//$FALL-THROUGH$
				case SINGLE_LINE_COMMENT:
				case REGULAR_EXPRESSION:
				case SHEBANG_LINE:
					return postFix(fState);

				default:
					consume();
					continue;
				}
				
			default:
				if (fLast == CARRIAGE_RETURN) {
					switch (fState) {
					case SINGLE_LINE_COMMENT:
					case REGULAR_EXPRESSION:
					case CHARACTER:
					case STRING:

						int last;
						int newState;
						switch (currentChar) {
						case '/':
							last= SLASH;
							newState= JAVASCRIPT;
							break;

						case '*':
							last= STAR;
							newState= JAVASCRIPT;
							break;

						case '\'':
							last= NONE;
							newState= CHARACTER;
							break;

						case '"':
							last= NONE;
							newState= STRING;
							break;

						case '\r':
							last= CARRIAGE_RETURN;
							newState= JAVASCRIPT;
							break;

						case '\\':
							last= BACKSLASH;
							newState= JAVASCRIPT;
							break;
							
						case '#':
							last= HASH;
							newState= JAVASCRIPT;
							break;
							
						default:
							last= NONE;
							newState= JAVASCRIPT;
							break;
						}

						fLast= NONE; // ignore fLast
						return preFix(fState, newState, last, 1);

					default:
						break;
					}
				}
			}

			// states
	 		switch (fState) {
	 		case JAVASCRIPT:
				switch (currentChar) {
				case '#':
					if (fLast == NONE) {
						fTokenLength++;
						fLast= HASH;
					}
					break;						
				case '!':
					if (fLast == HASH) {
						if (fTokenLength - getLastLength(fLast) > 0) {
							return preFix(JAVASCRIPT, SHEBANG_LINE, NONE, 2);
						} else {
							preFix(JAVASCRIPT, SHEBANG_LINE, NONE, 2);
							fTokenOffset += fTokenLength;
							fTokenLength= fPrefixLength;
						}
					} else {
						consume();
					}
					break;
				case '/':
					if (fLast == SLASH) {
						if (fTokenLength - getLastLength(fLast) > 0) {
							return preFix(JAVASCRIPT, SINGLE_LINE_COMMENT, NONE, 2);
						} else {
							preFix(JAVASCRIPT, SINGLE_LINE_COMMENT, NONE, 2);
							fTokenOffset += fTokenLength;
							fTokenLength= fPrefixLength;
							break;
						}

					} else {
						switch (lastNonWhitespaceChar)	//possible chars before regexp 
						{
						case 0: // No char before (the very beginning of a javascript
						case '(':
						case ',':
						case '=':
						case ':':
						case '[':
						case '!':
						case '|':
						case '&':
						case '?':
						case '{':
						case '}':
							int tempChar = fScanner.read();
							fScanner.unread();
							switch(tempChar) {
							case '/':
							case '*':
								break;
							default:
								//check if regexp
								fLast= NONE; // ignore fLast
								onCharList = false; //reset char list;
								if (fTokenLength > 0) {
									return preFix(JAVASCRIPT, REGULAR_EXPRESSION, NONE, 1);
								} else {
									preFix(JAVASCRIPT, REGULAR_EXPRESSION, NONE, 1);
									fTokenOffset += fTokenLength;
									fTokenLength= fPrefixLength;
									break;
								}
							}
							
						}
						fTokenLength++;
						fLast= SLASH;
						break;
					}

				case '*':
					if (fLast == SLASH) {
						if (fTokenLength - getLastLength(fLast) > 0)
							return preFix(JAVASCRIPT, MULTI_LINE_COMMENT, SLASH_STAR, 2);
						else {
							preFix(JAVASCRIPT, MULTI_LINE_COMMENT, SLASH_STAR, 2);
							fTokenOffset += fTokenLength;
							fTokenLength= fPrefixLength;
							break;
						}

					} else {
						consume();
						break;
					}

				case '\'':
					fLast= NONE; // ignore fLast
					if (fTokenLength > 0)
						return preFix(JAVASCRIPT, CHARACTER, NONE, 1);
					else {
						preFix(JAVASCRIPT, CHARACTER, NONE, 1);
						fTokenOffset += fTokenLength;
						fTokenLength= fPrefixLength;
						break;
					}

				case '"':
					fLast= NONE; // ignore fLast
					if (fTokenLength > 0)
						return preFix(JAVASCRIPT, STRING, NONE, 1);
					else {
						preFix(JAVASCRIPT, STRING, NONE, 1);
						fTokenOffset += fTokenLength;
						fTokenLength= fPrefixLength;
						break;
					}

				default:
					consume();
					break;
				}
				break;

	 		case SINGLE_LINE_COMMENT:
				consume();
				break;

	 		case JSDOC:
				switch (currentChar) {
				case '/':
					switch (fLast) {
					case SLASH_STAR_STAR:
						return postFix(MULTI_LINE_COMMENT);

					case STAR:
						return postFix(JSDOC);

					default:
						consume();
						break;
					}
					break;

				case '*':
					fTokenLength++;
					fLast= STAR;
					break;

				default:
					consume();
					break;
				}
				break;

	 		case MULTI_LINE_COMMENT:
				switch (currentChar) {
				case '*':
					if (fLast == SLASH_STAR) {
						fLast= SLASH_STAR_STAR;
						fTokenLength++;
						fState= JSDOC;
					} else {
						fTokenLength++;
						fLast= STAR;
					}
					break;

				case '/':
					if (fLast == STAR) {
						return postFix(MULTI_LINE_COMMENT);
					} else {
						consume();
						break;
					}

				default:
					consume();
					break;
				}
				break;

	 		case STRING:
	 			switch (currentChar) {
	 			case '\\':
					fLast= (fLast == BACKSLASH) ? NONE : BACKSLASH;
					fTokenLength++;
					break;

				case '\"':
	 				if (fLast != BACKSLASH) {
	 					return postFix(STRING);

		 			} else {
						consume();
						break;
	 				}

		 		default:
					consume();
	 				break;
	 			}
	 			break;

	 		case REGULAR_EXPRESSION:
	 			switch (currentChar) {

				case '\\':
					fLast= (fLast == BACKSLASH) ? NONE : BACKSLASH;
					fTokenLength++;
					break;
				case '[':
					onCharList = true;
					consume();
					break;
				case ']':
					onCharList = false;
					if (fLast==SLASH || fLast==REGULAR_EXPRESSION_END)
		 			{
		 				fTokenLength--;
		 				fScanner.unread();
		 				return postFix(REGULAR_EXPRESSION);
		 			}
					consume();
					break;
				case '/':
					if (!onCharList) {
						fLast= (fLast == BACKSLASH) ? NONE : SLASH;
						fTokenLength++;
					} else {
						consume();
					}
					break;	 				

				case 'g':
				case 'm':
				case 'i':
		 			if (fLast==SLASH || fLast==REGULAR_EXPRESSION_END)
		 			{
	 					  fLast=REGULAR_EXPRESSION_END;
	 						fTokenLength++;
		 			}
		 			else
						consume();
	 				break;
		 				
					

		 		default:
		 			if (fLast==SLASH || fLast==REGULAR_EXPRESSION_END)
		 			{
		 				fTokenLength--;
		 				fScanner.unread();
 					   return postFix(REGULAR_EXPRESSION);
		 			}
					consume();
	 				break;
	 			}
	 			break;

	 		case CHARACTER:
	 			switch (currentChar) {
				case '\\':
					fLast= (fLast == BACKSLASH) ? NONE : BACKSLASH;
					fTokenLength++;
					break;

	 			case '\'':
	 				if (fLast != BACKSLASH) {
	 					return postFix(CHARACTER);

	 				} else {
						consume();
		 				break;
	 				}

	 			default:
					consume();
	 				break;
	 			}
	 			break;
	 			
	 		case SHEBANG_LINE:
				consume();
				break;
	 		}
		}
 	}

	private static final int getLastLength(int last) {
		switch (last) {
		default:
			return -1;

		case NONE:
			return 0;

		case CARRIAGE_RETURN:
		case BACKSLASH:
		case SLASH:
		case STAR:
		case HASH:
			return 1;

		case SLASH_STAR:
		case SHEBANG_LINE:
			return 2;

		case SLASH_STAR_STAR:
			return 3;
		}
	}

	private final void consume() {
		fTokenLength++;
		fLast= NONE;
	}

	private final IToken postFix(int state) {
		fTokenLength++;
		fLast= NONE;
		fState= JAVASCRIPT;
		fPrefixLength= 0;
		return fTokens[state];
	}

	private final IToken preFix(int state, int newState, int last, int prefixLength) {
		fTokenLength -= getLastLength(fLast);
		fLast= last;
		fPrefixLength= prefixLength;
		IToken token= fTokens[state];
		fState= newState;
		return token;
	}

	private static int getState(String contentType) {

		if (contentType == null)
			return JAVASCRIPT;

		else if (contentType.equals(JAVA_SINGLE_LINE_COMMENT))
			return SINGLE_LINE_COMMENT;

		else if (contentType.equals(JAVA_MULTI_LINE_COMMENT))
			return MULTI_LINE_COMMENT;

		else if (contentType.equals(JAVA_DOC))
			return JSDOC;

		else if (contentType.equals(JAVA_STRING))
			return STRING;

		else if (contentType.equals(JAVA_CHARACTER))
			return CHARACTER;

		else
			return JAVASCRIPT;
	}

	/*
	 * @see IPartitionTokenScanner#setPartialRange(IDocument, int, int, String, int)
	 */
	public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {

		fScanner.setRange(document, offset, length);
		fTokenOffset= partitionOffset;
		fTokenLength= 0;
		fPrefixLength= offset - partitionOffset;
		fLast= NONE;

		if (offset == partitionOffset) {
			// restart at beginning of partition
			fState= JAVASCRIPT;
		} else {
			fState= getState(contentType);
		}
	}

	/*
	 * @see ITokenScanner#setRange(IDocument, int, int)
	 */
	public void setRange(IDocument document, int offset, int length) {

		fScanner.setRange(document, offset, length);
		fTokenOffset= offset;
		fTokenLength= 0;
		fPrefixLength= 0;
		fLast= NONE;
		fState= JAVASCRIPT;
	}

	/*
	 * @see ITokenScanner#getTokenLength()
	 */
	public int getTokenLength() {
		return fTokenLength;
	}

	/*
	 * @see ITokenScanner#getTokenOffset()
	 */
	public int getTokenOffset() {
		return fTokenOffset;
	}

}
