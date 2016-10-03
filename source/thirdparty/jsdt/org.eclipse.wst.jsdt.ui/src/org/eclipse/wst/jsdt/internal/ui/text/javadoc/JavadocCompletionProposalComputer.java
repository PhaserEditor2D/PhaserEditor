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
package org.eclipse.wst.jsdt.internal.ui.text.javadoc;

import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposalComputer;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * 
 * 
 */
public class JavadocCompletionProposalComputer extends JavaCompletionProposalComputer {
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposalComputer#createCollector(org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector= super.createCollector(context);
		collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
		collector.setIgnored(CompletionProposal.FIELD_REF, false);
		collector.setIgnored(CompletionProposal.KEYWORD, true);
		collector.setIgnored(CompletionProposal.LABEL_REF, true);
		collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
		collector.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
		collector.setIgnored(CompletionProposal.METHOD_REF, false);
		collector.setIgnored(CompletionProposal.PACKAGE_REF, true);
		collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
		collector.setIgnored(CompletionProposal.JSDOC_TYPE_REF, false);
		collector.setIgnored(CompletionProposal.JSDOC_FIELD_REF, false);
		collector.setIgnored(CompletionProposal.JSDOC_METHOD_REF, false);
		collector.setIgnored(CompletionProposal.JSDOC_PARAM_REF, false);
		collector.setIgnored(CompletionProposal.TYPE_REF, false);
		return collector;
	}
}
