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
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalComparator;

/**
 * Simple completion processor that completes package fragment roots that are source
 * folders.
 */
public class JavaSourcePackageFragmentRootCompletionProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {

	private char[] fProposalAutoActivationSet;
	private IJavaScriptModel fRoot;
	private CompletionProposalComparator fComparator;
	private JavaScriptElementLabelProvider fLabelProvider;

	public JavaSourcePackageFragmentRootCompletionProcessor() {
		fRoot= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet= triggers.toCharArray();
		fComparator= new CompletionProposalComparator();
		fLabelProvider= new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_SMALL_ICONS);
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return fProposalAutoActivationSet;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		if (fRoot == null) {
			return null;
		}

		String input= contentAssistSubject.getDocument().get();
		String prefix= input.substring(0, documentOffset);

		ICompletionProposal[] proposals= createSourcePackageFragmentRootProposals(prefix, input.length());
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	/**
	 * @param prefix prefixString with thatthe sourcepackagefragmentroots must begin
	 * @param replacementLength length of the text to replace
	 * @return array with sourcepackagefragmentroots
	 */
	private ICompletionProposal[] createSourcePackageFragmentRootProposals(String prefix, int replacementLength) {
		List proposals= new ArrayList();
		try {
			IJavaScriptProject[] projects= fRoot.getJavaScriptProjects();
			for (int i= 0; i < projects.length; i++) {
				IJavaScriptProject project= projects[i];
				IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
				for (int j= 0; j < roots.length; j++) {
					IPackageFragmentRoot root= roots[j];
					if (root.exists() && (root.getKind() == IPackageFragmentRoot.K_SOURCE)) {
						String name= root.getPath().toString();
						if (name.length() > 1) {
							name= name.substring(1);
						}
						if (name.startsWith(prefix)) {
							Image image= fLabelProvider.getImage(root);
							JavaCompletionProposal proposal= new JavaCompletionProposal(name, 0, replacementLength, image,
								name, 0);
							proposals.add(proposal);
						}
					}
				}
			}
		} catch (JavaScriptModelException e) {
			// nothing to do
		}

		return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		return null;
	}

}
