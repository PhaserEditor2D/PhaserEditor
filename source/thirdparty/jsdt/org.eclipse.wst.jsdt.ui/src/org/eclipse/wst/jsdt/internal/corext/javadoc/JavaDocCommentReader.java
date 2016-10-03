/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.javadoc;

import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.formatter.IndentManipulation;
import org.eclipse.wst.jsdt.internal.ui.text.html.SingleCharReader;


/**
 * Reads a java doc comment from a java doc comment. Skips star-character
 * on begin of line
 */
public class JavaDocCommentReader extends SingleCharReader {

	private IBuffer fBuffer;
	
	private int fCurrPos;
	private int fStartPos;
	private int fEndPos;
	
	private boolean fWasNewLine;
		
	public JavaDocCommentReader(IBuffer buf, int start, int end) {
		fBuffer= buf;
		fStartPos= start + 3;
		fEndPos= end - 2;
		
		reset();
	}
		
	public JavaDocCommentReader(IBuffer buf, int end) {
		fBuffer= buf;
		fStartPos = fEndPos = end;
		
		fStartPos = rewind();
		if (fStartPos >= 0)
			reset();
		else
			fCurrPos = fEndPos;
	}
		
	/**
	 * @see java.io.Reader#read()
	 */
	public int read() {
		if (fCurrPos < fEndPos) {
			char ch;
			if (fWasNewLine) {
				do {
					ch= fBuffer.getChar(fCurrPos++);
				} while (fCurrPos < fEndPos && Character.isWhitespace(ch));
				if (ch == '*') {
					if (fCurrPos < fEndPos) {
						do {
							ch= fBuffer.getChar(fCurrPos++);
						} while (ch == '*');
					} else {
						return -1;
					}
				}
			} else {
				ch= fBuffer.getChar(fCurrPos++);
			}
			fWasNewLine= IndentManipulation.isLineDelimiterChar(ch);
			
			return ch;
		}
		return -1;
	}
		
	/**
	 * @see java.io.Reader#close()
	 */		
	public void close() {
		fBuffer= null;
	}
	
	/**
	 * @see java.io.Reader#reset()
	 */		
	public void reset() {
		fCurrPos= fStartPos;
		fWasNewLine= true;
	}
	
	private int rewind() {
		if (fEndPos > 4) {
			char ch;
			// skip whitespace before the name
			do {
				ch = fBuffer.getChar(fStartPos--);
			}
			while (fStartPos > 4 && Character.isWhitespace(ch));

			// skip keyword if present
			if (ch != 'r' && ch != '/')
				return -1;
			ch = fBuffer.getChar(fStartPos--);
			if (ch != 'a' && ch != '*')
				return -1;
			if (ch == '*')
				fStartPos += 2;
			else {
				ch = fBuffer.getChar(fStartPos--);
				if (ch != 'v')
					return -1;
			}
			// skip before any trailing whitespace
			do {
				ch = fBuffer.getChar(fStartPos--);
			}
			while (fStartPos > 4 && Character.isWhitespace(ch));
			// found a possible block comment end
			if (fStartPos > 4) {
				if (ch == '/' && fBuffer.getChar(fStartPos) == '*') {
					fEndPos = fStartPos - 1;
					while (fStartPos > 2 && (fBuffer.getChar(fStartPos - 1) != '/' || fBuffer.getChar(fStartPos) != '*')) {
						fStartPos--;
					}
					if (fBuffer.getChar(fStartPos - 1) == '/' || fBuffer.getChar(fStartPos) == '*') {
						return fStartPos;
					}
				}
			}
		}
		return -1;
	}
			
	/**
	 * Returns the offset of the last read character in the passed buffer.
	 */
	public int getOffset() {
		return fCurrPos;
	}
		
		
}
