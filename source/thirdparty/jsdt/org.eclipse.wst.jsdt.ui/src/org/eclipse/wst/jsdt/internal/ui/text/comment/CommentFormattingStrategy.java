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

package org.eclipse.wst.jsdt.internal.ui.text.comment;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;

/**
 * Formatting strategy for general source code comments.
 *
 * 
 */
public class CommentFormattingStrategy extends ContextBasedFormattingStrategy {

	/** Documents to be formatted by this strategy */
	private final LinkedList fDocuments= new LinkedList();

	/** Partitions to be formatted by this strategy */
	private final LinkedList fPartitions= new LinkedList();

	/** Last formatted document's hash-code. */
	private int fLastDocumentHash;

	/** Last formatted document header's hash-code. */
	private int fLastHeaderHash;

	/** End of the first class or interface token in the last document. */
	private int fLastMainTokenEnd= -1;

	/** End of the header in the last document. */
	private int fLastDocumentsHeaderEnd;


	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#format()
	 */
	public void format() {
		
		final IDocument document= (IDocument) fDocuments.getFirst();

		TextEdit edit= calculateTextEdit();
		if (edit == null)
			return;
		
		try {
			edit.apply(document);
		} catch (MalformedTreeException x) {
			JavaScriptPlugin.log(x);
		} catch (BadLocationException x) {
			JavaScriptPlugin.log(x);
		}
	}

	/**
	 * Calculates the <code>TextEdit</code> used to format the region with the 
	 * properties indicated in the formatting context previously supplied by 
	 * <code>formatterStarts(IFormattingContext)</code>.
	 * 
	 * @see CommentFormattingStrategy#format()
	 * @return A <code>TextEdit</code>, or <code>null</code> if no formating is required
	 * 
	 */
	public TextEdit calculateTextEdit() {
		super.format();

		final IDocument document= (IDocument) fDocuments.removeFirst();
		final TypedPosition position= (TypedPosition)fPartitions.removeFirst();
		if (document == null || position == null)
			return null;

		Map preferences= getPreferences();
		final boolean isFormattingHeader= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER));
		int documentsHeaderEnd= computeHeaderEnd(document, preferences);

		TextEdit edit= null;		
		if (position.offset >= documentsHeaderEnd) {
			// not a header
			try {
				// compute offset in document of region passed to the formatter
				int sourceOffset= document.getLineOffset(document.getLineOfOffset(position.getOffset()));

				// format region
				int partitionOffset= position.getOffset() - sourceOffset;
				int sourceLength= partitionOffset + position.getLength();
				String source= document.get(sourceOffset, sourceLength);
				CodeFormatter commentFormatter= ToolFactory.createCodeFormatter(preferences, ToolFactory.M_FORMAT_EXISTING);
				int indentationLevel= inferIndentationLevel(source.substring(0, partitionOffset), getTabSize(preferences), getIndentSize(preferences));
				edit= commentFormatter.format(getKindForPartitionType(position.getType()), source, partitionOffset, position.getLength(), indentationLevel, TextUtilities.getDefaultLineDelimiter(document));

				// move edit offset to match document
				if (edit != null)
					edit.moveTree(sourceOffset);
			} catch (BadLocationException x) {
				JavaScriptPlugin.log(x);
			}
		} else if (isFormattingHeader) {
			boolean wasJavaDoc= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT));
			if (!wasJavaDoc)
				preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			
			boolean wasBlockComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT));
			if (!wasBlockComment)
				preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, DefaultCodeFormatterConstants.TRUE);
			
			boolean wasLineComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT));
			if (!wasLineComment)
				preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT, DefaultCodeFormatterConstants.TRUE);
			
			try {
				// compute offset in document of region passed to the formatter
				int sourceOffset= document.getLineOffset(document.getLineOfOffset(position.getOffset()));

				// format region
				int partitionOffset= position.getOffset() - sourceOffset;
				int sourceLength= partitionOffset + position.getLength();
				String source= document.get(sourceOffset, sourceLength);
				CodeFormatter commentFormatter= ToolFactory.createCodeFormatter(preferences);
				int indentationLevel= inferIndentationLevel(source.substring(0, partitionOffset), getTabSize(preferences), getIndentSize(preferences));
				edit= commentFormatter.format(getKindForPartitionType(position.getType()), source, partitionOffset, position.getLength(), indentationLevel, TextUtilities.getDefaultLineDelimiter(document));

				// move edit offset to match document
				if (edit != null)
					edit.moveTree(sourceOffset);
			} catch (BadLocationException x) {
				JavaScriptPlugin.log(x);
			} finally {
				if (!wasJavaDoc)
					preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.FALSE);
				if (!wasBlockComment)
					preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, DefaultCodeFormatterConstants.FALSE);
				if (!wasLineComment)
					preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT, DefaultCodeFormatterConstants.FALSE);
			}

		}
		return edit;		
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#formatterStarts(org.eclipse.jface.text.formatter.IFormattingContext)
	 */
	public void formatterStarts(IFormattingContext context) {
		super.formatterStarts(context);

		fPartitions.addLast(context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
		fDocuments.addLast(context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#formatterStops()
	 */
	public void formatterStops() {
		fPartitions.clear();
		fDocuments.clear();

		super.formatterStops();
	}

	/**
	 * Map from {@link IJavaScriptPartitions}comment partition types to
	 * {@link CodeFormatter}code snippet kinds.
	 *
	 * @param type the partition type
	 * @return the code snippet kind
	 * 
	 */
	private static int getKindForPartitionType(String type) {
		if (IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type))
				return CodeFormatter.K_SINGLE_LINE_COMMENT;
		if (IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT.equals(type))
				return CodeFormatter.K_MULTI_LINE_COMMENT;
		if (IJavaScriptPartitions.JAVA_DOC.equals(type))
				return CodeFormatter.K_JAVA_DOC;
		return CodeFormatter.K_UNKNOWN;
	}

	/**
	 * Infer the indentation level based on the given reference indentation
	 * and tab size.
	 *
	 * @param reference the reference indentation
	 * @param tabSize the tab size
	 * @param indentSize the indent size in space equivalents
	 * @return the inferred indentation level
	 * 
	 */
	private int inferIndentationLevel(String reference, int tabSize, int indentSize) {
		StringBuffer expanded= expandTabs(reference, tabSize);

		int referenceWidth= expanded.length();
		if (tabSize == 0)
			return referenceWidth;

		int level= referenceWidth / indentSize;
		if (referenceWidth % indentSize > 0)
			level++;
		return level;
	}

	/**
	 * Expands the given string's tabs according to the given tab size.
	 *
	 * @param string the string
	 * @param tabSize the tab size
	 * @return the expanded string
	 * 
	 */
	private static StringBuffer expandTabs(String string, int tabSize) {
		StringBuffer expanded= new StringBuffer();
		for (int i= 0, n= string.length(), chars= 0; i < n; i++) {
			char ch= string.charAt(i);
			if (ch == '\t') {
				for (; chars < tabSize; chars++)
					expanded.append(' ');
				chars= 0;
			} else {
				expanded.append(ch);
				chars++;
				if (chars >= tabSize)
					chars= 0;
			}

		}
		return expanded;
	}

	/**
	 * Returns the visual tab size.
	 *
	 * @param preferences the preferences
	 * @return the visual tab size
	 * 
	 */
	private static int getTabSize(Map preferences) {
		/*
		 * If the tab-char is SPACE, FORMATTER_INDENTATION_SIZE is not used
		 * by the core formatter.
		 * We piggy back the visual tab length setting in that preference in
		 * that case. See CodeFormatterUtil.
		 */
		String key;
		if (JavaScriptCore.SPACE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
			key= DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
		else
			key= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;

		if (preferences.containsKey(key))
			try {
				return Integer.parseInt((String) preferences.get(key));
			} catch (NumberFormatException e) {
				// use default
			}
		return 4;
	}
	
	/**
	 * Returns the indentation size in space equivalents.
	 *
	 * @param preferences the preferences
	 * @return the indentation size in space equivalents
	 * 
	 */
	private static int getIndentSize(Map preferences) {
		/*
		 * FORMATTER_INDENTATION_SIZE is only used if FORMATTER_TAB_CHAR is MIXED. Otherwise, the
		 * indentation size is in FORMATTER_TAB_CHAR. See CodeFormatterUtil.
		 */
		String key;
		if (DefaultCodeFormatterConstants.MIXED.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
			key= DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
		else
			key= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
		
		if (preferences.containsKey(key))
			try {
				return Integer.parseInt((String) preferences.get(key));
			} catch (NumberFormatException e) {
				// use default
			}
			return 4;
	}

	/**
	 * Returns the end offset for the document's header.
	 *
	 * @param document the document
	 * @param preferences the given preferences to format
	 * @return the header's end offset
	 */
	private int computeHeaderEnd(IDocument document, Map preferences) {
		if (document == null)
			return -1;

		try {
			if (fLastMainTokenEnd >= 0 && document.hashCode() == fLastDocumentHash && fLastMainTokenEnd < document.getLength() && document.get(0, fLastMainTokenEnd).hashCode() == fLastHeaderHash)
				return fLastDocumentsHeaderEnd;
		} catch (BadLocationException e) {
			// should not happen -> recompute
		}

		IScanner scanner= ToolFactory.createScanner(true, false, false, (String) preferences.get(JavaScriptCore.COMPILER_SOURCE), (String) preferences.get(JavaScriptCore.COMPILER_COMPLIANCE));
		scanner.setSource(document.get().toCharArray());

		try {
			int offset= -1;
			boolean foundComment= false;
			int terminal= scanner.getNextToken();
			while (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || terminal== ITerminalSymbols.TokenNameWHITESPACE || 
					terminal == ITerminalSymbols.TokenNameCOMMENT_LINE || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK)
			   {

				if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC)
					offset= scanner.getCurrentTokenStartPosition();

				foundComment= terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK;

				terminal= scanner.getNextToken();
			}

			int mainTokenEnd= scanner.getCurrentTokenEndPosition();
			if (terminal != ITerminalSymbols.TokenNameEOF) {
				mainTokenEnd++;
				if (offset == -1 || (foundComment && (terminal == ITerminalSymbols.TokenNameimport || terminal == ITerminalSymbols.TokenNamepackage)))
					offset= scanner.getCurrentTokenStartPosition();
			} else
				offset= -1;

			try {
				fLastHeaderHash= document.get(0, mainTokenEnd).hashCode();
			} catch (BadLocationException e) {
				// should not happen -> recompute next time
				mainTokenEnd= -1;
			}

			fLastDocumentHash= document.hashCode();
			fLastMainTokenEnd= mainTokenEnd;
			fLastDocumentsHeaderEnd= offset;
			return offset;

		} catch (InvalidInputException ex) {
			// enable formatting
			return -1;
		}
	}
}
