/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj_eclipse@ihug.com.au) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=20644
 *     Brock Janiczak (brockj_eclipse@ihug.com.au) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=83607
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLPrinter;
import org.eclipse.wst.jsdt.internal.ui.text.html.SubstitutionTextReader;
import org.eclipse.wst.jsdt.core.dom.TagElement;


/**
 * Processes JavaDoc tags.
 */
public class JavaDoc2HTMLTextReader extends SubstitutionTextReader {


	static private class Pair {
		String fTag;
		String fContent;

		Pair(String tag, String content) {
			fTag= tag;
			fContent= content;
		}
	}

	private List fParameters;
	private String fReturn;
	private List fExceptions;
	private List fAuthors;
	private List fSees;
	private List fSince;
	private List fRest; // list of Pair objects

	public JavaDoc2HTMLTextReader(Reader reader) {
		super(reader);
		setSkipWhitespace(false);
	}

	private int getTag(StringBuffer buffer) throws IOException {
		int c= nextChar();
		while (c == '.' || c != -1 && Character.isLetter((char) c)) {
			buffer.append((char) c);
			c= nextChar();
		}
		return c;
	}

	private int getContent(StringBuffer buffer, char stopChar) throws IOException {
		int c= nextChar();
		while (c != -1 && c != stopChar) {
			buffer.append((char) c);
			c= nextChar();
		}
		return c;
	}

	private int getContentUntilNextTag(StringBuffer buffer) throws IOException {
		int c= nextChar();
        boolean blockStartRead= false;
		while (c != -1) {
			if (c == '@') {
				int index= buffer.length();
				while (--index >= 0 && Character.isWhitespace(buffer.charAt(index))) {
					switch (buffer.charAt(index)) {
					case '\n':
					case '\r':
						return c;
					}
					if (index <= 0) {
						return c;
					}
				}
			}
            if (blockStartRead) {
                buffer.append(processBlockTag());
                blockStartRead= false;
            } else {
                buffer.append((char) c);
            }

			c= nextChar();
            blockStartRead= c == '{';
		}
		return c;
	}

	private String substituteQualification(String qualification) {
		String result= qualification.replace('#', '.');
		if (result.startsWith(".")) { //$NON-NLS-1$
			result= result.substring(1);
		}
		return result;
	}

	private void printDefinitions(StringBuffer buffer, List list, boolean firstword) {
		Iterator e= list.iterator();
		while (e.hasNext()) {
			String s= (String) e.next();
			buffer.append("<dd>"); //$NON-NLS-1$
			if (!firstword)
				buffer.append(s);
			else {
				buffer.append("<b>"); //$NON-NLS-1$

				int i= getParamEndOffset(s);
				if (i <= s.length()) {
					buffer.append(HTMLPrinter.convertToHTMLContent(s.substring(0, i)));
					buffer.append("</b>"); //$NON-NLS-1$
					buffer.append(s.substring(i));
				} else {
					buffer.append("</b>"); //$NON-NLS-1$
				}
			}
			buffer.append("</dd>"); //$NON-NLS-1$
		}
	}

	private int getParamEndOffset(String s) {
		int i= 0;
		final int length= s.length();
		// \s*
		while (i < length && Character.isWhitespace(s.charAt(i)))
			++i;
		if (i < length && s.charAt(i) == '<') {
			// generic type parameter
			// read <\s*\w*\s*>
			while (i < length && Character.isWhitespace(s.charAt(i)))
				++i;
			while (i < length && Character.isJavaIdentifierPart(s.charAt(i)))
				++i;
			while (i < length && s.charAt(i) != '>')
				++i;
		} else {
			// simply read an identifier
			while (i < length && Character.isJavaIdentifierPart(s.charAt(i)))
				++i;
		}

		return i;
	}

	private void print(StringBuffer buffer, String tag, List elements, boolean firstword) {
		if ( !elements.isEmpty()) {
			buffer.append("<dt>"); //$NON-NLS-1$
			buffer.append(tag);
			buffer.append("</dt>"); //$NON-NLS-1$
			printDefinitions(buffer, elements, firstword);
		}
	}

	private void print(StringBuffer buffer, String tag, String content) {
		if  (content != null) {
			buffer.append("<dt>"); //$NON-NLS-1$
			buffer.append(tag);
			buffer.append("</dt>"); //$NON-NLS-1$
			buffer.append("<dd>"); //$NON-NLS-1$
			buffer.append(content);
			buffer.append("</dd>"); //$NON-NLS-1$
		}
	}

	private void printRest(StringBuffer buffer) {
		if ( !fRest.isEmpty()) {
			Iterator e= fRest.iterator();
			while (e.hasNext()) {
				Pair p= (Pair) e.next();
				buffer.append("<dt>"); //$NON-NLS-1$
				if (p.fTag != null)
					buffer.append(p.fTag);
				buffer.append("</dt>"); //$NON-NLS-1$
				buffer.append("<dd>"); //$NON-NLS-1$
				if (p.fContent != null)
					buffer.append(p.fContent);
				buffer.append("</dd>"); //$NON-NLS-1$
			}
		}
	}

	private String printSimpleTag() {
		StringBuffer buffer= new StringBuffer();
		buffer.append("<dl>"); //$NON-NLS-1$
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_see_section, fSees, false);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_parameters_section, fParameters, true);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_returns_section, fReturn);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_throws_section, fExceptions, false);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_author_section, fAuthors, false);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_since_section, fSince, false);
		printRest(buffer);
		buffer.append("</dl>"); //$NON-NLS-1$

		return buffer.toString();
	}

	private void handleTag(String tag, String tagContent) {

		tagContent= tagContent.trim();

		if (TagElement.TAG_PARAM.equals(tag))
			fParameters.add(tagContent);
		else if (TagElement.TAG_RETURN.equals(tag)) 
			fReturn= tagContent;
		else if (TagElement.TAG_EXCEPTION.equals(tag)) 
			fExceptions.add(tagContent);
		else if (TagElement.TAG_THROWS.equals(tag)) 
			fExceptions.add(tagContent);
		else if (TagElement.TAG_AUTHOR.equals(tag)) 
			fAuthors.add(substituteQualification(tagContent));
		else if (TagElement.TAG_SEE.equals(tag)) 
			fSees.add(substituteQualification(tagContent));
		else if (TagElement.TAG_SINCE.equals(tag)) 
			fSince.add(substituteQualification(tagContent));
		else if (tagContent != null)
			fRest.add(new Pair(tag, tagContent));
	}

	/*
	 * A '@' has been read. Process a javadoc tag
	 */
	private String processSimpleTag() throws IOException {

		fParameters= new ArrayList();
		fExceptions= new ArrayList();
		fAuthors= new ArrayList();
		fSees= new ArrayList();
		fSince= new ArrayList();
		fRest= new ArrayList();

		StringBuffer buffer= new StringBuffer();
		int c= '@';
		while (c != -1) {

			buffer.setLength(0);
			buffer.append((char) c);
			c= getTag(buffer);
			String tag= buffer.toString();

			buffer.setLength(0);
			if (c != -1) {
				c= getContentUntilNextTag(buffer);
			}

			handleTag(tag, buffer.toString());
		}

		return printSimpleTag();
	}

	private String printBlockTag(String tag, String tagContent) {

		if (TagElement.TAG_LINK.equals(tag) || TagElement.TAG_LINKPLAIN.equals(tag)) {

			char[] contentChars= tagContent.toCharArray();
			boolean inParentheses= false;
			int labelStart= 0;

			for (int i= 0; i < contentChars.length; i++) {
				char nextChar= contentChars[i];

				// tagContent always has a leading space
				if (i == 0 && Character.isWhitespace(nextChar)) {
					labelStart= 1;
					continue;
				}

				if (nextChar == '(') {
					inParentheses= true;
					continue;
				}

				if (nextChar == ')') {
					inParentheses= false;
					continue;
				}

				// Stop at first whitespace that is not in parentheses
				if (!inParentheses && Character.isWhitespace(nextChar)) {
					labelStart= i+1;
					break;
				}
			}
			if (TagElement.TAG_LINK.equals(tag))
				return "<code>" + substituteQualification(tagContent.substring(labelStart)) + "</code>";  //$NON-NLS-1$//$NON-NLS-2$
			else
				return substituteQualification(tagContent.substring(labelStart));
			
		} else if (TagElement.TAG_LITERAL.equals(tag)) {
			return printLiteral(tagContent);
			
		} else if (TagElement.TAG_CODE.equals(tag)) {
			return "<code>" + printLiteral(tagContent) + "</code>"; //$NON-NLS-1$//$NON-NLS-2$
		}

		// If something went wrong at least replace the {} with the content
		return substituteQualification(tagContent);
	}

	private String printLiteral(String tagContent) {
		int contentStart= 0;
		for (int i= 0; i < tagContent.length(); i++) {
			if (! Character.isWhitespace(tagContent.charAt(i))) {
				contentStart= i;
				break;
			}
		}
		return HTMLPrinter.convertToHTMLContent(tagContent.substring(contentStart));
	}

	/*
	 * A '{' has been read. Process a block tag
	 */
	private String processBlockTag() throws IOException {

		int c= nextChar();

		if (c != '@') {
			StringBuffer buffer= new StringBuffer();
			buffer.append('{');
			buffer.append((char) c);
			return buffer.toString();
		}

		StringBuffer buffer= new StringBuffer();
		if (c != -1) {

			buffer.setLength(0);
			buffer.append((char) c);

			c= getTag(buffer);
			String tag= buffer.toString();

			buffer.setLength(0);
			if (c != -1 && c != '}') {
				buffer.append((char) c);
				c= getContent(buffer, '}');
			}

			return printBlockTag(tag, buffer.toString());
		}

		return null;
	}

	/*
	 * @see SubstitutionTextReaderr#computeSubstitution(int)
	 */
	protected String computeSubstitution(int c) throws IOException {
		if (c == '@' && fWasWhiteSpace)
			return processSimpleTag();

		if (c == '{')
			return processBlockTag();

		return null;
	}
}
