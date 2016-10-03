/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.compare;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.corext.dom.TokenScanner;


/**
 * A comparator for Java tokens.
 */
public class JavaTokenComparator implements ITokenComparator {
	
	/**
	 * Factory to create text token comparators.
	 * This is a workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=183224 .
	 */
	public static interface ITokenComparatorFactory {
		/**
		 * @param text text to be tokenized
		 * @return a token comparator
		 */
		public ITokenComparator createTokenComparator(String text);
	}
		
	private static final boolean DEBUG= false;
	
	private final String fText;
	private final ITokenComparatorFactory fTextTokenComparatorFactory;
	private int fCount;
	private int[] fStarts;
	private int[] fLengths;

	/**
	 * Creates a token comparator for the given string.
	 * 
	 * @param text the text to be tokenized
	 */
	public JavaTokenComparator(String text) {
		this(text, null);
	}
	
	/**
	 * Creates a token comparator for the given string.
	 * 
	 * @param text the text to be tokenized
	 * @param textTokenComparatorFactory a factory to create text token comparators
	 */
	public JavaTokenComparator(String text, ITokenComparatorFactory textTokenComparatorFactory) {
		
		fTextTokenComparatorFactory= textTokenComparatorFactory;
		Assert.isLegal(text != null);
		
		fText= text;
		
		int length= fText.length();
		fStarts= new int[length];
		fLengths= new int[length];
		fCount= 0;
		
		IScanner scanner= ToolFactory.createScanner(true, true, false, false); // returns comments & whitespace
		scanner.setSource(fText.toCharArray());
		int endPos= 0;
		try {
			int tokenType;
			while ((tokenType= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				int start= scanner.getCurrentTokenStartPosition();
				int end= scanner.getCurrentTokenEndPosition()+1;
				// Comments are treated as a single token (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=78063)
				if (TokenScanner.isComment(tokenType) || tokenType == ITerminalSymbols.TokenNameStringLiteral) {
					int dl= fTextTokenComparatorFactory == null ? getCommentStartTokenLength(tokenType) : 0;
					recordTokenRange(start, dl);
					parseText(start + dl, text.substring(start + dl, end));
				} else {
					recordTokenRange(start, end - start);
				}
				endPos= end;
			}
		} catch (InvalidInputException ex) {
			// We couldn't parse part of the input. Fall through and make the rest a single token
		}
		// Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=13907
		if (endPos < length) {
			recordTokenRange(endPos, length - endPos);
		}
	}

	/**
	 * Records the given token range.
	 * 
	 * @param start of the token
	 * @param length length of the token
	 * 
	 */
	private void recordTokenRange(int start, int length) {
		fStarts[fCount]= start;
		fLengths[fCount]= length;
		fCount++;
		if (DEBUG)
			System.out.println(fText.substring(start, start + length));
	}

	private void parseText(int start, String text) {
		ITokenComparator subTokenizer= fTextTokenComparatorFactory == null
				? new JavaTokenComparator(text)
				: fTextTokenComparatorFactory.createTokenComparator(text);
		int count= subTokenizer.getRangeCount();
		for (int i= 0; i < count; i++) {
			int subStart= subTokenizer.getTokenStart(i);
			int subLength= subTokenizer.getTokenLength(i);
			recordTokenRange(start + subStart, subLength);
		}
	}

	/**
	 * Returns the length of the token that
	 * initiates the given comment type. 
	 * 
	 * @param tokenType
	 * @return the length of the token that start a comment
	 * 
	 */
	private static int getCommentStartTokenLength(int tokenType) {
		if (tokenType == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
			return 3;
		} else if (tokenType == ITerminalSymbols.TokenNameStringLiteral) {
			return 1;
		} else {
			return 2;
		}
	}

	/**
	 * Returns the number of tokens in the string.
	 *
	 * @return number of token in the string
	 */
	public int getRangeCount() {
		return fCount;
	}

	/* (non Javadoc)
	 * see ITokenComparator.getTokenStart
	 */
	public int getTokenStart(int index) {
		if (index >= 0 && index < fCount)
			return fStarts[index];
		if (fCount > 0)
			return fStarts[fCount-1] + fLengths[fCount-1];
		return 0;
	}

	/* (non Javadoc)
	 * see ITokenComparator.getTokenLength
	 */
	public int getTokenLength(int index) {
		if (index < fCount)
			return fLengths[index];
		return 0;
	}
	
	/**
	 * Returns <code>true</code> if a token given by the first index
	 * matches a token specified by the other <code>IRangeComparator</code> and index.
	 *
	 * @param thisIndex	the number of the token within this range comparator
	 * @param other the range comparator to compare this with
	 * @param otherIndex the number of the token within the other comparator
	 * @return <code>true</code> if the token are equal
	 */
	public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
		if (other != null && getClass() == other.getClass()) {
			JavaTokenComparator tc= (JavaTokenComparator) other;	// safe cast
			int thisLen= getTokenLength(thisIndex);
			int otherLen= tc.getTokenLength(otherIndex);
			if (thisLen == otherLen)
				return fText.regionMatches(false, getTokenStart(thisIndex), tc.fText, tc.getTokenStart(otherIndex), thisLen);
		}
		return false;
	}

	/**
	 * Aborts the comparison if the number of tokens is too large.
	 *
	 * @param length a number on which to base the decision whether to return
	 * 	<code>true</code> or <code>false</code>
	 * @param maxLength another number on which to base the decision whether to return
	 *	<code>true</code> or <code>false</code>
	 * @param other the other <code>IRangeComparator</code> to compare with
	 * @return <code>true</code> to abort a token comparison
	 */
	public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {

		if (getRangeCount() < 50 || other.getRangeCount() < 50)
			return false;

		if (maxLength < 100)
			return false;

		if (length < 100)
			return false;

		if (maxLength > 800)
			return true;

		if (length < maxLength / 4)
			return false;

		return true;
	}
}
