/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;


public class JavaMethodCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for method proposals without parameters. Do not modify. */
	protected final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[' };
	/** Triggers for method proposals. Do not modify. */
	protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	/** Triggers for method name proposals (static imports). Do not modify. */
	protected final static char[] METHOD_NAME_TRIGGERS= new char[] { ';' };
	
	private boolean fHasParameters;
	private boolean fHasParametersComputed= false;
	private FormatterPrefs fFormatterPrefs;

	public JavaMethodCompletionProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context) {
		super(proposal, context);
	}

	public void apply(IDocument document, char trigger, int offset) {
		if (trigger == ' ' || trigger == '(')
			trigger= '\0';
		super.apply(document, trigger, offset);
		if (needsLinkedMode()) {
			setUpLinkedMode(document, ')');
		}
	}

	protected boolean needsLinkedMode() {
		return hasArgumentList() && hasParameters();
	}
	
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		if (hasArgumentList()) {
			String completion= String.valueOf(fProposal.getName());
			if (isCamelCaseMatching()) {
				String prefix= getPrefix(document, completionOffset);
				return getCamelCaseCompound(prefix, completion);
			}

			return completion;
		}
		return super.getPrefixCompletionText(document, completionOffset);
	}
	
	protected IContextInformation computeContextInformation() {
		// no context information for METHOD_NAME_REF proposals (e.g. for static imports)
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=94654
		if (fProposal.getKind() == CompletionProposal.METHOD_REF &&  hasParameters() && (getReplacementString().endsWith(RPAREN) || getReplacementString().length() == 0)) {
			ProposalContextInformation contextInformation= new ProposalContextInformation(fProposal);
			if (fContextInformationPosition != 0 && fProposal.getCompletion().length == 0)
				contextInformation.setContextInformationPosition(fContextInformationPosition);
			return contextInformation;
		}
		return super.computeContextInformation();
	}
	
	protected char[] computeTriggerCharacters() {
		if (fProposal.getKind() == CompletionProposal.METHOD_NAME_REFERENCE ||
				fProposal.getKind() == CompletionProposal.METHOD_REF ||
				fProposal.getKind() == CompletionProposal.CONSTRUCTOR_INVOCATION)
			return METHOD_NAME_TRIGGERS;
		if (hasParameters())
			return METHOD_WITH_ARGUMENTS_TRIGGERS;
		return METHOD_TRIGGERS;
	}
	
	/**
	 * Returns <code>true</code> if the method being inserted has at least one parameter. Note
	 * that this does not say anything about whether the argument list should be inserted. This
	 * depends on the position in the document and the kind of proposal; see
	 * {@link #hasArgumentList() }.
	 * 
	 * @return <code>true</code> if the method has any parameters, <code>false</code> if it has
	 *         no parameters
	 */
	protected final boolean hasParameters() {
		if (!fHasParametersComputed) {
			fHasParametersComputed= true;
			fHasParameters= computeHasParameters();
		}
		return fHasParameters;
	}

	private boolean computeHasParameters() throws IllegalArgumentException {
		return fProposal.hasParameters() || Signature.getParameterCount(fProposal.getSignature()) > 0;
	}

	/**
	 * Returns <code>true</code> if the argument list should be inserted by the proposal,
	 * <code>false</code> if not.
	 * 
	 * @return <code>true</code> when the proposal is not in javadoc nor within an import and comprises the
	 *         parameter list
	 */
	protected boolean hasArgumentList() {
		if (CompletionProposal.METHOD_NAME_REFERENCE == fProposal.getKind())
			return false;
		IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
		boolean noOverwrite= preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION) ^ isToggleEating();
		char[] completion= fProposal.getCompletion();
		return !isInJavadoc() && completion.length > 0 && (noOverwrite  || completion[completion.length - 1] == ')');
	}

	/**
	 * Returns the method formatter preferences.
	 * 
	 * @return the formatter settings
	 */
	protected final FormatterPrefs getFormatterPrefs() {
		if (fFormatterPrefs == null)
			fFormatterPrefs= new FormatterPrefs(fInvocationContext.getProject());
		return fFormatterPrefs;
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.LazyJavaCompletionProposal#computeReplacementString()
	 */
	protected String computeReplacementString() {
		if (!hasArgumentList())
			return super.computeReplacementString();
		
		// we're inserting a method plus the argument list - respect formatter preferences
		StringBuffer buffer= new StringBuffer();
		buffer.append(fProposal.getName());

		FormatterPrefs prefs= getFormatterPrefs();
		if (prefs.beforeOpeningParen)
			buffer.append(SPACE);
		buffer.append(LPAREN);
		
		if (hasParameters()) {
			setCursorPosition(buffer.length());
			
			if (prefs.afterOpeningParen)
				buffer.append(SPACE);
			

			// don't add the trailing space, but let the user type it in himself - typing the closing paren will exit
//			if (prefs.beforeClosingParen)
//				buffer.append(SPACE);
		} else {
			if (prefs.inEmptyList)
				buffer.append(SPACE);
		}

		buffer.append(RPAREN);

		return buffer.toString();

	}
	
	protected ProposalInfo computeProposalInfo() {
		IJavaScriptProject project= fInvocationContext.getProject();
		if (project != null)
			return new MethodProposalInfo(project, fProposal);
		return super.computeProposalInfo();
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.LazyJavaCompletionProposal#computeSortString()
	 */
	protected String computeSortString() {
		/*
		 * Lexicographical sort order:
		 * 1) by relevance (done by the proposal sorter)
		 * 2) by method name
		 * 3) by parameter count
		 * 4) by parameter type names
		 */
		char[] name= fProposal.getName();
		char[] signature = fProposal.getSignature();
		char[] parameterList;
		int parameterCount;
		if(signature != null) {
			parameterList= Signature.toCharArray(fProposal.getSignature(), null, null, false, false);
			parameterCount= Signature.getParameterCount(fProposal.getSignature()) % 10; // we don't care about insane methods with >9 parameters
		} else {
			char[][] params = this.fProposal.getParamaterNames();
			if(params != null) {
				parameterList = CharOperation.concatWith(params, ',');
				parameterCount = params.length % 10; // we don't care about insane methods with >9 parameters
			} else {
				parameterList = new char[0];
				parameterCount = 0;
			}
		}
		
		StringBuffer buf= new StringBuffer(name.length + 2 + parameterList.length);
		
		buf.append(name);
		buf.append('\0'); // separator
		buf.append(parameterCount);
		buf.append(parameterList);
		return buf.toString();
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.AbstractJavaCompletionProposal#isValidPrefix(java.lang.String)
	 */
	public boolean isValidPrefix(String prefix) {
		if (super.isValidPrefix(prefix))
			return true;
		
		// match on last part of the proposal
		String name = new String(fProposal.getName());
		int lastSeperatorIndex = name.lastIndexOf('.');
		if(lastSeperatorIndex != -1) {
			name = name.substring(lastSeperatorIndex+1);
			name = name.toLowerCase();
			if(name.indexOf(prefix.toLowerCase()) == 0) {
				return true;
			}
		}
		
		String word= getDisplayString();
		if (isInJavadoc()) {
			int idx = word.indexOf("{@link "); //$NON-NLS-1$
			if (idx==0) {
				word = word.substring(7);
			} else {
				idx = word.indexOf("{@value "); //$NON-NLS-1$
				if (idx==0) {
					word = word.substring(8);
				}
			}
		}
		return isPrefix(prefix, word);
	}
}
