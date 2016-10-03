// COPIED FROM org.eclipse.jface.internal.text.html
// to get around "discouraged access" errors
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
package org.eclipse.wst.jsdt.internal.ui.text.html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;


import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;

import com.ibm.icu.text.BreakIterator;


/**
 * <p>
 * Moved into this package from <code>org.eclipse.jface.internal.text.revisions</code>.</p>
 */
public class HTMLTextPresenter implements DefaultInformationControl.IInformationPresenter, DefaultInformationControl.IInformationPresenterExtension {

	public class LineBreakingReader {

		private BufferedReader fReader;
		private GC fGC;
		private int fMaxWidth;

		private String fLine;
		private int fOffset;

		private BreakIterator fLineBreakIterator;
		private boolean fBreakWords;

		/**
		 * Creates a reader that breaks an input text to fit in a given width.
		 * 
		 * @param reader Reader of the input text
		 * @param gc The graphic context that defines the currently used font sizes
		 * @param maxLineWidth The max width (pixels) where the text has to fit in
		 */
		public LineBreakingReader(Reader reader, GC gc, int maxLineWidth) {
			fReader= new BufferedReader(reader);
			fGC= gc;
			fMaxWidth= maxLineWidth;
			fOffset= 0;
			fLine= null;
			fLineBreakIterator= BreakIterator.getLineInstance();
			fBreakWords= true;
		}

		public boolean isFormattedLine() {
			return fLine != null;
		}

		/**
		 * Reads the next line. The lengths of the line will not exceed the given maximum
		 * width.
		 * 
		 * @return the next line 
		 * @throws IOException 
		 */
		public String readLine() throws IOException {
			if (fLine == null) {
				String line= fReader.readLine();
				if (line == null)
					return null;

				int lineLen= fGC.textExtent(line).x;
				if (lineLen < fMaxWidth) {
					return line;
				}
				fLine= line;
				fLineBreakIterator.setText(line);
				fOffset= 0;
			}
			int breakOffset= findNextBreakOffset(fOffset);
			String res;
			if (breakOffset != BreakIterator.DONE) {
				res= fLine.substring(fOffset, breakOffset);
				fOffset= findWordBegin(breakOffset);
				if (fOffset == fLine.length()) {
					fLine= null;
				}
			} else {
				res= fLine.substring(fOffset);
				fLine= null;
			}
			return res;
		}

		private int findNextBreakOffset(int currOffset) {
			int currWidth= 0;
			int nextOffset= fLineBreakIterator.following(currOffset);
			while (nextOffset != BreakIterator.DONE) {
				String word= fLine.substring(currOffset, nextOffset);
				int wordWidth= fGC.textExtent(word).x;
				int nextWidth= wordWidth + currWidth;
				if (nextWidth > fMaxWidth) {
					if (currWidth > 0)
						return currOffset;

					if (!fBreakWords)
						return nextOffset;

					// need to fit into fMaxWidth
					int length= word.length();
					while (length >= 0) {
						length--;
						word= word.substring(0, length);
						wordWidth= fGC.textExtent(word).x;
						if (wordWidth + currWidth < fMaxWidth)
							return currOffset + length;
					}
					return nextOffset;
				}
				currWidth= nextWidth;
				currOffset= nextOffset;
				nextOffset= fLineBreakIterator.next();
			}
			return nextOffset;
		}

		private int findWordBegin(int idx) {
			while (idx < fLine.length() && Character.isWhitespace(fLine.charAt(idx))) {
				idx++;
			}
			return idx;
		}
	}
	private static final String LINE_DELIM= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	private int fCounter;
	private boolean fEnforceUpperLineLimit;

	public HTMLTextPresenter(boolean enforceUpperLineLimit) {
		super();
		fEnforceUpperLineLimit= enforceUpperLineLimit;
	}

	public HTMLTextPresenter() {
		this(true);
	}

	protected Reader createReader(String hoverInfo, TextPresentation presentation) {
		return new HTML2TextReader(new StringReader(hoverInfo), presentation);
	}

	protected void adaptTextPresentation(TextPresentation presentation, int offset, int insertLength) {

		int yoursStart= offset;
		int yoursEnd=   offset + insertLength -1;
		yoursEnd= Math.max(yoursStart, yoursEnd);

		Iterator e= presentation.getAllStyleRangeIterator();
		while (e.hasNext()) {

			StyleRange range= (StyleRange) e.next();

			int myStart= range.start;
			int myEnd=   range.start + range.length -1;
			myEnd= Math.max(myStart, myEnd);

			if (myEnd < yoursStart)
				continue;

			if (myStart < yoursStart)
				range.length += insertLength;
			else
				range.start += insertLength;
		}
	}

	private void append(StringBuffer buffer, String string, TextPresentation presentation) {

		int length= string.length();
		buffer.append(string);

		if (presentation != null)
			adaptTextPresentation(presentation, fCounter, length);

		fCounter += length;
	}

	private String getIndent(String line) {
		int length= line.length();

		int i= 0;
		while (i < length && Character.isWhitespace(line.charAt(i))) ++i;

		return (i == length ? line : line.substring(0, i)) + " "; //$NON-NLS-1$
	}

	/*
	 * @see IHoverInformationPresenter#updatePresentation(Display display, String, TextPresentation, int, int)
	 */
	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
		return updatePresentation((Drawable)display, hoverInfo, presentation, maxWidth, maxHeight);
	}

	/*
	 * @see IHoverInformationPresenterExtension#updatePresentation(Drawable drawable, String, TextPresentation, int, int)
	 * 
	 */
	public String updatePresentation(Drawable drawable, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {

		if (hoverInfo == null)
			return null;

		GC gc= new GC(drawable);
		try {

			StringBuffer buffer= new StringBuffer();
			int maxNumberOfLines= Math.round(maxHeight / gc.getFontMetrics().getHeight());

			fCounter= 0;
			LineBreakingReader reader= new LineBreakingReader(createReader(hoverInfo, presentation), gc, maxWidth);

			boolean lastLineFormatted= false;
			String lastLineIndent= null;

			String line=reader.readLine();
			boolean lineFormatted= reader.isFormattedLine();
			boolean firstLineProcessed= false;

			while (line != null) {

				if (fEnforceUpperLineLimit && maxNumberOfLines <= 0)
					break;

				if (firstLineProcessed) {
					if (!lastLineFormatted)
						append(buffer, LINE_DELIM, null);
					else {
						append(buffer, LINE_DELIM, presentation);
						if (lastLineIndent != null)
							append(buffer, lastLineIndent, presentation);
					}
				}

				append(buffer, line, null);
				firstLineProcessed= true;

				lastLineFormatted= lineFormatted;
				if (!lineFormatted)
					lastLineIndent= null;
				else if (lastLineIndent == null)
					lastLineIndent= getIndent(line);

				line= reader.readLine();
				lineFormatted= reader.isFormattedLine();

				maxNumberOfLines--;
			}

			if (line != null) {
				append(buffer, LINE_DELIM, lineFormatted ? presentation : null);
				append(buffer, HTMLMessages.getString("HTMLTextPresenter.ellipse"), presentation); //$NON-NLS-1$
			}

			return trim(buffer, presentation);

		} catch (IOException e) {

			// ignore TODO do something else?
			return null;

		} finally {
			gc.dispose();
		}
	}

	private String trim(StringBuffer buffer, TextPresentation presentation) {

		int length= buffer.length();

		int end= length -1;
		while (end >= 0 && Character.isWhitespace(buffer.charAt(end)))
			-- end;

		if (end == -1)
			return ""; //$NON-NLS-1$

		if (end < length -1)
			buffer.delete(end + 1, length);
		else
			end= length;

		int start= 0;
		while (start < end && Character.isWhitespace(buffer.charAt(start)))
			++ start;

		buffer.delete(0, start);
		presentation.setResultWindow(new Region(start, buffer.length()));
		return buffer.toString();
	}
}

