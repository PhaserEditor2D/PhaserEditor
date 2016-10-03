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

package org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDescriptorRegistry;

public class VariableNamesProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {

	private String fErrorMessage;
	
	private String[] fTempNameProposals;
	
	private ImageDescriptorRegistry fImageRegistry;
	private ImageDescriptor fProposalImageDescriptor;

	public VariableNamesProcessor(String[] tempNameProposals) {
		fTempNameProposals= (String[]) tempNameProposals.clone();
		Arrays.sort(fTempNameProposals);
		fImageRegistry= JavaScriptPlugin.getImageDescriptorRegistry();
		fProposalImageDescriptor= JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE;

	}
	
	public void setProposalImageDescriptor(ImageDescriptor proposalImageDescriptor) {
		fProposalImageDescriptor= proposalImageDescriptor;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.contentassist.IContentAssistProcessorExtension#computeContextInformation(org.eclipse.jface.contentassist.IContentAssistSubject, int)
	 */
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.contentassist.IContentAssistProcessorExtension#computeCompletionProposals(org.eclipse.jface.contentassist.IContentAssistSubject, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		if (fTempNameProposals.length == 0)
			return null;
		String input= contentAssistSubject.getDocument().get();
		
		ArrayList proposals= new ArrayList();
		String prefix= input.substring(0, documentOffset);
		Image image= fImageRegistry.get(fProposalImageDescriptor);
		for (int i= 0; i < fTempNameProposals.length; i++) {
			String tempName= fTempNameProposals[i];
			if (tempName.length() == 0 || ! tempName.startsWith(prefix))
				continue;
			JavaCompletionProposal proposal= new JavaCompletionProposal(tempName, 0, input.length(), image, tempName, 0);
			proposals.add(proposal);
		}
		fErrorMessage= proposals.size() > 0 ? null : JavaUIMessages.JavaEditor_codeassist_noCompletions; 
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

}
