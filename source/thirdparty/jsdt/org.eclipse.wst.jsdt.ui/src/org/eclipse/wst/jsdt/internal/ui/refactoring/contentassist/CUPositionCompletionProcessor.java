/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalComparator;


public class CUPositionCompletionProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {
	
	private static final ImageDescriptorRegistry IMAGE_DESC_REGISTRY= JavaScriptPlugin.getImageDescriptorRegistry();
	
	private String fErrorMessage;
	private char[] fProposalAutoActivationSet;
	private CompletionProposalComparator fComparator;
	
	private CompletionContextRequestor fCompletionContextRequestor;

	private CUPositionCompletionRequestor fCompletionRequestor;

	/**
	 * Creates a <code>CUPositionCompletionProcessor</code>.
	 * The completion context must be set via {@link #setCompletionContext(IJavaScriptUnit,String,String)}.
	 * @param completionRequestor the completion requestor
	 */
	public CUPositionCompletionProcessor(CUPositionCompletionRequestor completionRequestor) {
		fCompletionRequestor= completionRequestor;
		
		fComparator= new CompletionProposalComparator();
		IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	/**
	 * @param cuHandle the {@link IJavaScriptUnit} in whose context codeComplete will be invoked.
	 * 		The given cu doesn't have to exist (and if it exists, it will not be modified).
	 * 		An independent working copy consisting of
	 * 		<code>beforeString</code> + ${current_input} + <code>afterString</code> will be used.
	 * @param beforeString the string before the input position
	 * @param afterString the string after the input position
	 */
	public void setCompletionContext(final IJavaScriptUnit cuHandle, final String beforeString, final String afterString) {
		fCompletionContextRequestor= new CompletionContextRequestor() {
			final StubTypeContext fStubTypeContext= new StubTypeContext(cuHandle, beforeString, afterString);
			public StubTypeContext getStubTypeContext() {
				return fStubTypeContext;
			}
		};
		if (cuHandle != null)
			fCompletionRequestor.setJavaProject(cuHandle.getJavaScriptProject());
	}
	
	public void setCompletionContextRequestor(CompletionContextRequestor completionContextRequestor) {
		fCompletionContextRequestor= completionContextRequestor;
	}

	/**
	 * Computing proposals on a <code>ITextViewer</code> is not supported.
	 * @see #computeCompletionProposals(IContentAssistSubjectControl, int)
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}
	
	/**
	 * Computing context information on a <code>ITextViewer</code> is not supported.
	 * @see #computeContextInformation(IContentAssistSubjectControl, int)
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return fProposalAutoActivationSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeContextInformation(IContentAssistSubjectControl, int)
	 */
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		return null;
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeCompletionProposals(IContentAssistSubjectControl, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		if (fCompletionContextRequestor.getOriginalCu() == null)
			return null;
		String input= contentAssistSubjectControl.getDocument().get();
		if (documentOffset == 0)
			return null;
		ICompletionProposal[] proposals= internalComputeCompletionProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	private ICompletionProposal[] internalComputeCompletionProposals(int documentOffset, String input) {
		String cuString= fCompletionContextRequestor.getBeforeString() + input + fCompletionContextRequestor.getAfterString();
		IJavaScriptUnit cu= null;
		try {
			/*
			 * Explicitly create a new non-shared working copy.
			 * 
			 * The WorkingCopy cannot easily be shared between calls, since IContentAssistProcessor
			 * has no dispose() lifecycle method. A workaround could be to pass in a WorkingCopyOwner
			 * and dispose the owner's working copies from the caller's dispose().
			 */
			cu= fCompletionContextRequestor.getOriginalCu().getWorkingCopy(new WorkingCopyOwner() {/*subclass*/}, new NullProgressMonitor());
			cu.getBuffer().setContents(cuString);
			int cuPrefixLength= fCompletionContextRequestor.getBeforeString().length();
			fCompletionRequestor.setOffsetReduction(cuPrefixLength);
			cu.codeComplete(cuPrefixLength + documentOffset, fCompletionRequestor);
			
			JavaCompletionProposal[] proposals= fCompletionRequestor.getResults();
			if (proposals.length == 0) {
				String errorMsg= fCompletionRequestor.getErrorMessage();
				if (errorMsg == null || errorMsg.trim().length() == 0)
					errorMsg= RefactoringMessages.JavaTypeCompletionProcessor_no_completion;  
				fErrorMessage= errorMsg;
			} else {
				fErrorMessage= fCompletionRequestor.getErrorMessage();
			}
			return proposals;
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			return null;
		} finally {
			try {
				if (cu != null)
					cu.discardWorkingCopy();
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
	}

	protected abstract static class CUPositionCompletionRequestor extends CompletionRequestor {
		public static final char[] TRIGGER_CHARACTERS= new char[] { '.' };
		
		private int fOffsetReduction;
		private IJavaScriptProject fJavaProject;
		
		private List fProposals;
		private String fErrorMessage2;

		public IJavaScriptProject getJavaProject() {
			return fJavaProject;
		}
		
		private void setJavaProject(IJavaScriptProject javaProject) {
			fJavaProject= javaProject;
		}
		
		private void setOffsetReduction(int offsetReduction) {
			fOffsetReduction= offsetReduction;
			fProposals= new ArrayList();
		}
		
		public final void completionFailure(IProblem error) {
			fErrorMessage2= error.getMessage();
		}

		public final JavaCompletionProposal[] getResults() {
			return (JavaCompletionProposal[]) fProposals.toArray(new JavaCompletionProposal[fProposals.size()]);
		}
		
		public final String getErrorMessage() {
			return fErrorMessage2;
		}
		
		protected final void addAdjustedCompletion(String name, String completion,
				int start, int end, int relevance, ImageDescriptor descriptor) {
			JavaCompletionProposal javaCompletionProposal= new JavaCompletionProposal(completion, start - fOffsetReduction, end - start,
					getImage(descriptor), name, relevance);
			javaCompletionProposal.setTriggerCharacters(TRIGGER_CHARACTERS);
			fProposals.add(javaCompletionProposal);
		}
		
		protected final void addAdjustedTypeCompletion(String name, String completion,
				int start, int end, int relevance, ImageDescriptor descriptor, String fullyQualifiedName) {
			JavaTypeCompletionProposal javaCompletionProposal= new JavaTypeCompletionProposal(
					fullyQualifiedName == null ? completion : fullyQualifiedName, null,
					fullyQualifiedName == null ? start - fOffsetReduction : 0,
					end - start, getImage(descriptor), name, relevance, completion);
			javaCompletionProposal.setTriggerCharacters(TRIGGER_CHARACTERS);
			fProposals.add(javaCompletionProposal);
		}

		private static Image getImage(ImageDescriptor descriptor) {
			return (descriptor == null) ? null : CUPositionCompletionProcessor.IMAGE_DESC_REGISTRY.get(descriptor);
		}
	}
}
