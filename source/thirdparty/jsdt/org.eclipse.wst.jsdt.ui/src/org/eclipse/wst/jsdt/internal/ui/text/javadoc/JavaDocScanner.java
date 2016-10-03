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
package org.eclipse.wst.jsdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.wst.jsdt.internal.ui.text.CombinedWordRule;
import org.eclipse.wst.jsdt.internal.ui.text.JavaCommentScanner;
import org.eclipse.wst.jsdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.wst.jsdt.internal.ui.text.CombinedWordRule.CharacterBuffer;
import org.eclipse.wst.jsdt.internal.ui.text.CombinedWordRule.WordMatcher;
import org.eclipse.wst.jsdt.ui.text.IColorManager;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptColorConstants;

/**
 * A rule based JavaDoc scanner.
 */
public final class JavaDocScanner extends JavaCommentScanner {


	/**
	 * Detector for HTML comment delimiters.
	 */
	static class HTMLCommentDetector implements IWordDetector {

		/**
		 * @see IWordDetector#isWordStart(char)
		 */
		public boolean isWordStart(char c) {
			return (c == '<' || c == '-');
		}

		/**
		 * @see IWordDetector#isWordPart(char)
		 */
		public boolean isWordPart(char c) {
			return (c == '-' || c == '!' || c == '>');
		}
	}

	class TagRule extends SingleLineRule {

		/*
		 * @see SingleLineRule
		 */
		public TagRule(IToken token) {
			super("<", ">", token, (char) 0); //$NON-NLS-2$ //$NON-NLS-1$
		}

		/*
		 * @see SingleLineRule
		 */
		public TagRule(IToken token, char escapeCharacter) {
			super("<", ">", token, escapeCharacter); //$NON-NLS-2$ //$NON-NLS-1$
		}

		private IToken evaluateToken() {
			try {
				final String token= getDocument().get(getTokenOffset(), getTokenLength()) + "."; //$NON-NLS-1$

				int offset= 0;
				char character= token.charAt(++offset);

				if (character == '/')
					character= token.charAt(++offset);

				while (Character.isWhitespace(character))
					character= token.charAt(++offset);

				while (Character.isLetterOrDigit(character))
					character= token.charAt(++offset);

				while (Character.isWhitespace(character))
					character= token.charAt(++offset);

				if (offset >= 2 && token.charAt(offset) == fEndSequence[0])
					return fToken;

			} catch (BadLocationException exception) {
				// Do nothing
			}
			return getToken(IJavaScriptColorConstants.JAVADOC_DEFAULT);
		}

		/*
		 * @see PatternRule#evaluate(ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {
			IToken result= super.evaluate(scanner);
			if (result == fToken)
				return evaluateToken();
			return result;
		}
	}

	private static String[] fgTokenProperties= {
		IJavaScriptColorConstants.JAVADOC_KEYWORD,
		IJavaScriptColorConstants.JAVADOC_TAG,
		IJavaScriptColorConstants.JAVADOC_LINK,
		IJavaScriptColorConstants.JAVADOC_DEFAULT,
		TASK_TAG
	};


	public JavaDocScanner(IColorManager manager, IPreferenceStore store, Preferences coreStore) {
		super(manager, store, coreStore, IJavaScriptColorConstants.JAVADOC_DEFAULT, fgTokenProperties);
	}

	/**
	 * Initialize with the given arguments
	 * @param manager	Color manager
	 * @param store	Preference store
	 *
	 * 
	 */
	public JavaDocScanner(IColorManager manager, IPreferenceStore store) {
		this(manager, store, null);
	}

	public IDocument getDocument() {
		return fDocument;
	}

	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	protected List createRules() {

		List list= new ArrayList();

		// Add rule for tags.
		Token token= getToken(IJavaScriptColorConstants.JAVADOC_TAG);
		list.add(new TagRule(token));


		// Add rule for HTML comments
		WordRule wordRule= new WordRule(new HTMLCommentDetector(), token);
		wordRule.addWord("<!--", token); //$NON-NLS-1$
		wordRule.addWord("--!>", token); //$NON-NLS-1$
		list.add(wordRule);


		// Add rule for links.
		token= getToken(IJavaScriptColorConstants.JAVADOC_LINK);
		list.add(new SingleLineRule("{@link", "}", token)); //$NON-NLS-2$ //$NON-NLS-1$
		list.add(new SingleLineRule("{@value", "}", token)); //$NON-NLS-2$ //$NON-NLS-1$


		// Add generic whitespace rule.
		list.add(new WhitespaceRule(new JavaWhitespaceDetector()));


		list.addAll(super.createRules());
		return list;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.JavaCommentScanner#createMatchers()
	 */
	protected List createMatchers() {
		List list= super.createMatchers();

		// Add word rule for keywords.
		final IToken token= getToken(IJavaScriptColorConstants.JAVADOC_KEYWORD);
		WordMatcher matcher= new CombinedWordRule.WordMatcher() {
			public IToken evaluate(ICharacterScanner scanner, CharacterBuffer word) {
				int length= word.length();
				if (length > 1 && word.charAt(0) == '@') {
					int i= 0;
					try {
						for (; i <= length; i++)
							scanner.unread();
						int c= scanner.read();
						i--;
						if (c == '*' || Character.isWhitespace((char)c)) {
							scanner.unread();
							return token;
						}
					} finally {
						for (; i >= 0; i--)
							scanner.read();
					}
				}
				return Token.UNDEFINED;
			}
		};
		list.add(matcher);

		return list;
	}
}


