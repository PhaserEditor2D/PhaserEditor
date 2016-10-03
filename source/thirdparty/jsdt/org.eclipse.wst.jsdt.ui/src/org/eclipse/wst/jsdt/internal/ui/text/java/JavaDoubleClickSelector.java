/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.text.ISourceVersionDependent;
import org.eclipse.wst.jsdt.internal.ui.text.JavaPairMatcher;

/**
 * Double click strategy aware of Java identifier syntax rules.
 */
public class JavaDoubleClickSelector implements ITextDoubleClickStrategy, ISourceVersionDependent {

	/**
	 * Detects java words depending on the source level. In 1.4 mode, detects
	 * <code>[[:ID:]]*</code>. In 1.5 mode, it also detects
	 * <code>@\s*[[:IDS:]][[:ID:]]*</code>.
	 *
	 * Character class definitions:
	 * <dl>
	 * <dt>[[:IDS:]]</dt><dd>a java identifier start character</dd>
	 * <dt>[[:ID:]]</dt><dd>a java identifier part character</dd>
	 * <dt>\s</dt><dd>a white space character</dd>
	 * <dt>@</dt><dd>the at symbol</dd>
	 * </dl>
	 *
	 * 
	 */
	private static final class AtJavaIdentifierDetector implements ISourceVersionDependent {

		private boolean fSelectAnnotations;

		private static final int UNKNOWN= -1;

		/* states */
		private static final int WS= 0;
		private static final int ID= 1;
		private static final int IDS= 2;
		private static final int AT= 3;

		/* directions */
		private static final int FORWARD= 0;
		private static final int BACKWARD= 1;

		/** The current state. */
		private int fState;
		/**
		 * The state at the anchor (if already detected by going the other way),
		 * or <code>UNKNOWN</code>.
		 */
		private int fAnchorState;
		/** The current direction. */
		private int fDirection;
		/** The start of the detected word. */
		private int fStart;
		/** The end of the word. */
		private int fEnd;

		/**
		 * Initializes the detector at offset <code>anchor</code>.
		 *
		 * @param anchor the offset of the double click
		 */
		private void setAnchor(int anchor) {
			fState= UNKNOWN;
			fAnchorState= UNKNOWN;
			fDirection= UNKNOWN;
			fStart= anchor;
			fEnd= anchor - 1;
		}

		private boolean isAt(char c) {
			return fSelectAnnotations && c == '@';
		}

		private boolean isIdentifierStart(char c) {
			return Character.isJavaIdentifierStart(c);
		}

		private boolean isIdentifierPart(char c) {
			return Character.isJavaIdentifierPart(c);
		}

		private boolean isWhitespace(char c) {
			return fSelectAnnotations && Character.isWhitespace(c);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.text.ISourceVersionDependent#setSourceVersion(java.lang.String)
		 */
		public void setSourceVersion(String version) {
			if (JavaScriptCore.VERSION_1_5.compareTo(version) <= 0)
				fSelectAnnotations= true;
			else
				fSelectAnnotations= false;
		}

		/**
		 * Try to add a character to the word going backward. Only call after
		 * forward calls!
		 *
		 * @param c the character to add
		 * @param offset the offset of the character
		 * @return <code>true</code> if further characters may be added to the
		 *         word
		 */
		private boolean backward(char c, int offset) {
			checkDirection(BACKWARD);
			switch (fState) {
				case AT:
					return false;
				case IDS:
					if (isAt(c)) {
						fStart= offset;
						fState= AT;
						return false;
					}
					if (isWhitespace(c)) {
						fState= WS;
						return true;
					}
				//$FALL-THROUGH$
				case ID:
					if (isIdentifierStart(c)) {
						fStart= offset;
						fState= IDS;
						return true;
					}
					if (isIdentifierPart(c)) {
						fStart= offset;
						fState= ID;
						return true;
					}
					return false;
				case WS:
					if (isWhitespace(c)) {
						return true;
					}
					if (isAt(c)) {
						fStart= offset;
						fState= AT;
						return false;
					}
					return false;
				default:
					return false;
			}
		}

		/**
		 * Try to add a character to the word going forward.
		 *
		 * @param c the character to add
		 * @param offset the offset of the character
		 * @return <code>true</code> if further characters may be added to the
		 *         word
		 */
		private boolean forward(char c, int offset) {
			checkDirection(FORWARD);
			switch (fState) {
				case WS:
				case AT:
					if (isWhitespace(c)) {
						fState= WS;
						return true;
					}
					if (isIdentifierStart(c)) {
						fEnd= offset;
						fState= IDS;
						return true;
					}
					return false;
				case IDS:
				case ID:
					if (isIdentifierStart(c)) {
						fEnd= offset;
						fState= IDS;
						return true;
					}
					if (isIdentifierPart(c)) {
						fEnd= offset;
						fState= ID;
						return true;
					}
					return false;
				case UNKNOWN:
					if (isIdentifierStart(c)) {
						fEnd= offset;
						fState= IDS;
						fAnchorState= fState;
						return true;
					}
					if (isIdentifierPart(c)) {
						fEnd= offset;
						fState= ID;
						fAnchorState= fState;
						return true;
					}
					if (isWhitespace(c)) {
						fState= WS;
						fAnchorState= fState;
						return true;
					}
					if (isAt(c)) {
						fStart= offset;
						fState= AT;
						fAnchorState= fState;
						return true;
					}
					return false;
				default:
					return false;
			}
		}

		/**
		 * If the direction changes, set state to be the previous anchor state.
		 *
		 * @param direction the new direction
		 */
		private void checkDirection(int direction) {
			if (fDirection == direction)
				return;

			if (direction == FORWARD) {
				if (fStart <= fEnd)
					fState= fAnchorState;
				else
					fState= UNKNOWN;
			} else if (direction == BACKWARD) {
				if (fEnd >= fStart)
					fState= fAnchorState;
				else
					fState= UNKNOWN;
			}

			fDirection= direction;
		}

		/**
		 * Returns the region containing <code>anchor</code> that is a java
		 * word.
		 *
		 * @param document the document from which to read characters
		 * @param anchor the offset around which to select a word
		 * @return the region describing a java word around <code>anchor</code>
		 */
		public IRegion getWordSelection(IDocument document, int anchor) {

			try {

				final int min= 0;
				final int max= document.getLength();
				setAnchor(anchor);

				char c;

				int offset= anchor;
				while (offset < max) {
					c= document.getChar(offset);
					if (!forward(c, offset))
						break;
					++offset;
				}

				offset= anchor; // use to not select the previous word when right behind it
//				offset= anchor - 1; // use to select the previous word when right behind it
				while (offset >= min) {
					c= document.getChar(offset);
					if (!backward(c, offset))
						break;
					--offset;
				}

				return new Region(fStart, fEnd - fStart + 1);

			} catch (BadLocationException x) {
				return new Region(anchor, 0);
			}
		}

	}

	protected static final char[] BRACKETS= {'{', '}', '(', ')', '[', ']', '<', '>' };
	protected JavaPairMatcher fPairMatcher= new JavaPairMatcher(BRACKETS);
	protected final AtJavaIdentifierDetector fWordDetector= new AtJavaIdentifierDetector();


	public JavaDoubleClickSelector() {
		super();
	}

	/**
	 * @see ITextDoubleClickStrategy#doubleClicked
	 */
	public void doubleClicked(ITextViewer textViewer) {

		int offset= textViewer.getSelectedRange().x;

		if (offset < 0)
			return;

		IDocument document= textViewer.getDocument();

		IRegion region= fPairMatcher.match(document, offset);
		if (region != null && region.getLength() >= 2) {
			textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
		} else {
			region= selectWord(document, offset);
			textViewer.setSelectedRange(region.getOffset(), region.getLength());
		}
	}

	protected IRegion selectWord(IDocument document, int anchor) {
		return fWordDetector.getWordSelection(document, anchor);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.ISourceVersionDependent#setSourceVersion(java.lang.String)
	 */
	public void setSourceVersion(String version) {
		fPairMatcher.setSourceVersion(version);
		fWordDetector.setSourceVersion(version);
	}
}
