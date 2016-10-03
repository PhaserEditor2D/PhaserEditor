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
package org.eclipse.wst.jsdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.wst.jsdt.ui.text.java.IJavadocCompletionProcessor;

/**
 *  (renamed from JavaDocCompletionEvaluator which got introduced in 2.0)
 */
public class HTMLTagCompletionProposalComputer implements IJavaCompletionProposalComputer {

	private static final String[] fgHTMLProposals= new String[IHtmlTagConstants.HTML_GENERAL_TAGS.length * 2];
	{
		String tag= null;

		int index= 0;
		int offset= 0;

		while (index < fgHTMLProposals.length) {

			tag= IHtmlTagConstants.HTML_GENERAL_TAGS[offset];
			fgHTMLProposals[index++]= IHtmlTagConstants.HTML_TAG_PREFIX + tag + IHtmlTagConstants.HTML_TAG_POSTFIX;
			fgHTMLProposals[index++]= IHtmlTagConstants.HTML_CLOSE_PREFIX + tag + IHtmlTagConstants.HTML_TAG_POSTFIX;
			offset++;
		}
	}

	private IDocument fDocument;
	private int fCurrentPos;
	private int fCurrentLength;
	private String fErrorMessage;
	private List fResult;

	private boolean fRestrictToMatchingCase;

	public HTMLTagCompletionProposalComputer() {
	}

	private static boolean isWordPart(char ch) {
		return Character.isJavaIdentifierPart(ch) || (ch == '#') || (ch == '.') || (ch == '/');
	}

	private static int findCharBeforeWord(IDocument doc, int lineBeginPos, int pos) {
		int currPos= pos - 1;
		if (currPos > lineBeginPos) {
			try {
				while (currPos > lineBeginPos && isWordPart(doc.getChar(currPos))) {
					currPos--;
				}
				return currPos;
			} catch (BadLocationException e) {
				// ignore
			}
		}
		return pos;
	}

	private static int findClosingCharacter(IDocument doc, int pos, int end, char endChar) throws BadLocationException {
		int curr= pos;
		while (curr < end && (doc.getChar(curr) != endChar)) {
			curr++;
		}
		if (curr < end) {
			return curr + 1;
		}
		return pos;
	}

	private static int findReplaceEndPos(IDocument doc, String newText, String oldText, int pos) {
		if (oldText.length() == 0 || oldText.equals(newText)) {
			return pos;
		}

		try {
			IRegion lineInfo= doc.getLineInformationOfOffset(pos);
			int end= lineInfo.getOffset() + lineInfo.getLength();

			// for html, search the tag end character
			return findClosingCharacter(doc, pos, end, '>');
		} catch (BadLocationException e) {
			// ignore
		}
		return pos;
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#computeCompletionProposals(org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 * 
	 */
	public List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (!(context instanceof JavadocContentAssistInvocationContext))
			return Collections.EMPTY_LIST;
		
		JavadocContentAssistInvocationContext docContext= (JavadocContentAssistInvocationContext) context;
		int flags= docContext.getFlags();
		fCurrentPos= docContext.getInvocationOffset();
		fCurrentLength= docContext.getSelectionLength();
		fRestrictToMatchingCase= (flags & IJavadocCompletionProcessor.RESTRICT_TO_MATCHING_CASE) != 0;

		IJavaScriptUnit cu= docContext.getCompilationUnit();
		if (cu == null)
			return Collections.EMPTY_LIST;
		IEditorInput editorInput= new FileEditorInput((IFile) cu.getResource());
		fDocument= JavaScriptUI.getDocumentProvider().getDocument(editorInput);
		if (fDocument == null) {
			return Collections.EMPTY_LIST;
		}

		try {
			fResult= new ArrayList(100);
			evalProposals();
			return fResult;
		} catch (JavaScriptModelException e) {
			fErrorMessage= e.getLocalizedMessage();
		} finally {
			fResult= null;
		}
		return null;
	}

	private void evalProposals() throws JavaScriptModelException {
		try {

			IRegion info= fDocument.getLineInformationOfOffset(fCurrentPos);
			int lineBeginPos= info.getOffset();

			int word1Begin= findCharBeforeWord(fDocument, lineBeginPos, fCurrentPos);
			if (word1Begin == fCurrentPos)
				return;
			
			char firstChar= fDocument.getChar(word1Begin);
			if (firstChar == '<') {
				String prefix= fDocument.get(word1Begin, fCurrentPos - word1Begin);
				addProposals(prefix, fgHTMLProposals, JavaPluginImages.IMG_OBJS_HTMLTAG);
				return;
			} else if (!Character.isWhitespace(firstChar)) {
				return;
			}

			// TODO really show all tags when there is no prefix?
			// TODO find any unclosed open tag and offer the corresponding close tag
			String prefix= fDocument.get(word1Begin + 1, fCurrentPos - word1Begin - 1);
			addAllTags(prefix);
		} catch (BadLocationException e) {
			// ignore
		}
	}

	private boolean prefixMatches(String prefix, String proposal) {
		if (fRestrictToMatchingCase) {
			return proposal.startsWith(prefix);
		} else if (proposal.length() >= prefix.length()) {
			return prefix.equalsIgnoreCase(proposal.substring(0, prefix.length()));
		}
		return false;
	}

	private void addAllTags(String prefix) {
		String htmlPrefix= "<" + prefix; //$NON-NLS-1$
		for (int i= 0; i < fgHTMLProposals.length; i++) {
			String curr= fgHTMLProposals[i];
			if (prefixMatches(htmlPrefix, curr)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_HTMLTAG), 0));
			}
		}
	}

	private void addProposals(String prefix, String[] choices, String imageName) {
		for (int i= 0; i < choices.length; i++) {
			String curr= choices[i];
			if (prefixMatches(prefix, curr)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(imageName), 0));
			}
		}
	}

	private JavaCompletionProposal createCompletion(String newText, String oldText, String labelText, Image image, int severity) {
		int offset= fCurrentPos - oldText.length();
		int length= fCurrentLength + oldText.length();
		if (fCurrentLength == 0)
			length= findReplaceEndPos(fDocument, newText, oldText, fCurrentPos) - offset;

		// bump opening over closing tags
		if (!newText.startsWith(IHtmlTagConstants.HTML_CLOSE_PREFIX))
			severity++;
		JavaCompletionProposal proposal= new JavaCompletionProposal(newText, offset, length, image, labelText, severity, true);
		proposal.setTriggerCharacters( new char[] { '>' });
		return proposal;
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#computeContextInformation(org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 * 
	 */
	public List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return Collections.EMPTY_LIST;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#getErrorMessage()
	 * 
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
     * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
     * 
     */
    public void sessionEnded() {
    	fErrorMessage= null;
    }

	/*
     * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
     * 
     */
    public void sessionStarted() {
    	fErrorMessage= null;
    }
}
