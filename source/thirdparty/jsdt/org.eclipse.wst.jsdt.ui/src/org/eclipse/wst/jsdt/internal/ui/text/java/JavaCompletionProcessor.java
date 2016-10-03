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
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * Java completion processor.
 */
public class JavaCompletionProcessor extends ContentAssistProcessor {

	private final static String VISIBILITY= JavaScriptCore.CODEASSIST_VISIBILITY_CHECK;
	private final static String ENABLED= "enabled"; //$NON-NLS-1$
	private final static String DISABLED= "disabled"; //$NON-NLS-1$
	
	private IContextInformationValidator fValidator;
	protected final IEditorPart fEditor;

	public JavaCompletionProcessor(IEditorPart editor, ContentAssistant assistant, String partition) {
		super(assistant, partition);
		fEditor= editor;
	}

	/**
	 * Tells this processor to restrict its proposal to those element
	 * visible in the actual invocation context.
	 *
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToVisibility(boolean restrict) {
		Hashtable options= JavaScriptCore.getOptions();
		Object value= options.get(VISIBILITY);
		if (value instanceof String) {
			String newValue= restrict ? ENABLED : DISABLED;
			if ( !newValue.equals(value)) {
				options.put(VISIBILITY, newValue);
				JavaScriptCore.setOptions(options);
			}
		}
	}

	/**
	 * Tells this processor to restrict is proposals to those
	 * starting with matching cases.
	 *
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToMatchingCases(boolean restrict) {
		// not yet supported
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		if (fValidator == null)
			fValidator= new JavaParameterListValidator();
		return fValidator;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.ContentAssistProcessor#filterAndSort(java.util.List, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected List filterAndSortProposals(List proposals, IProgressMonitor monitor, ContentAssistInvocationContext context) {
		ProposalSorterRegistry.getDefault().getCurrentSorter().sortProposals(context, proposals);
		return proposals;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.ContentAssistProcessor#createContext(org.eclipse.jface.text.ITextViewer, int)
	 */
	protected ContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
		return new JavaContentAssistInvocationContext(viewer, offset, fEditor);
	}
}
