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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalComparator;

public class JavaPackageCompletionProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {
	
	private IPackageFragmentRoot fPackageFragmentRoot;
	private CompletionProposalComparator fComparator;
	private ILabelProvider fLabelProvider;

	private char[] fProposalAutoActivationSet;

	/**
	 * Creates a <code>JavaPackageCompletionProcessor</code>.
	 * The completion context must be set via {@link #setPackageFragmentRoot(IPackageFragmentRoot)}.
	 */
	public JavaPackageCompletionProcessor() {
	    this(new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_SMALL_ICONS));
	}
	
    /**
     * Creates a <code>JavaPackageCompletionProcessor</code>.
     * The Processor uses the given <code>ILabelProvider</code> to show text and icons for the 
     * possible completions.
     * @param labelProvider Used for the popups.
     */
	public JavaPackageCompletionProcessor(ILabelProvider labelProvider) {
		fComparator= new CompletionProposalComparator();
		fLabelProvider= labelProvider;

		IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/* (non-Javadoc)
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
		return fProposalAutoActivationSet;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeContextInformation(IContentAssistSubjectControl, int)
	 */
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl,
			int documentOffset) {
		return null;
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeCompletionProposals(IContentAssistSubjectControl, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		if (fPackageFragmentRoot == null)
			return null;
		String input= contentAssistSubjectControl.getDocument().get();
		ICompletionProposal[] proposals= createPackagesProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
	}
	
	public void setPackageFragmentRoot(IPackageFragmentRoot packageFragmentRoot) {
		fPackageFragmentRoot= packageFragmentRoot;
	}

	private ICompletionProposal[] createPackagesProposals(int documentOffset, String input) {
		ArrayList proposals= new ArrayList();
		String prefix= input.substring(0, documentOffset);
		try {
			IJavaScriptElement[] packageFragments= fPackageFragmentRoot.getChildren();
			for (int i= 0; i < packageFragments.length; i++) {
				IPackageFragment pack= (IPackageFragment) packageFragments[i];
				String packName= pack.getElementName();
				if (packName.length() == 0 || ! packName.startsWith(prefix))
					continue;
				Image image= fLabelProvider.getImage(pack);
				JavaCompletionProposal proposal= new JavaCompletionProposal(packName, 0, input.length(), image, fLabelProvider.getText(pack), 0);
				proposals.add(proposal);
			}
		} catch (JavaScriptModelException e) {
			//fPackageFragmentRoot is not a proper root -> no proposals
		}
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
}
