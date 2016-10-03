/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.wst.jsdt.internal.ui.text.Symbols;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * 
 * 
 */
public class JavaNoTypeCompletionProposalComputer extends JavaCompletionProposalComputer {
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposalComputer#createCollector(org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector= super.createCollector(context);
		collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, false);
		collector.setIgnored(CompletionProposal.FIELD_REF, false);
		collector.setIgnored(CompletionProposal.KEYWORD, false);
		collector.setIgnored(CompletionProposal.LABEL_REF, false);
		collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
		collector.setIgnored(CompletionProposal.METHOD_DECLARATION, false);
		collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, false);
		collector.setIgnored(CompletionProposal.METHOD_REF, false);
		collector.setIgnored(CompletionProposal.PACKAGE_REF, true);
		collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, false);
		collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, false);
		
		collector.setIgnored(CompletionProposal.JSDOC_BLOCK_TAG, true);
		collector.setIgnored(CompletionProposal.JSDOC_FIELD_REF, true);
		collector.setIgnored(CompletionProposal.JSDOC_INLINE_TAG, true);
		collector.setIgnored(CompletionProposal.JSDOC_METHOD_REF, true);
		collector.setIgnored(CompletionProposal.JSDOC_PARAM_REF, true);
		collector.setIgnored(CompletionProposal.JSDOC_TYPE_REF, true);
		
		collector.setIgnored(CompletionProposal.TYPE_REF, false);
		return collector;
	}
	
	protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
		final int contextPosition= context.getInvocationOffset();
		
		IDocument document= context.getDocument();
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		int bound= Math.max(-1, contextPosition - 200);
		
		// try the innermost scope of parentheses that looks like a method call
		int pos= contextPosition - 1;
		do {
			int paren= scanner.findOpeningPeer(pos, bound, '(', ')');
			if (paren == JavaHeuristicScanner.NOT_FOUND)
				break;
			int token= scanner.previousToken(paren - 1, bound);
			// next token must be a method name (identifier) or the closing angle of a
			// constructor call of a parameterized type.
			if (token == Symbols.TokenIDENT || token == Symbols.TokenGREATERTHAN)
				return paren + 1;
			pos= paren - 1;
		} while (true);
		
		return super.guessContextInformationPosition(context);
	}
}
