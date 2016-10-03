/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * Computes Java completion proposals and context infos.
 * 
 * 
 */
public class JavaCompletionProposalComputer implements IJavaCompletionProposalComputer {

	private static final class ContextInformationWrapper implements IContextInformation, IContextInformationExtension {

		private final IContextInformation fContextInformation;
		private int fPosition;

		public ContextInformationWrapper(IContextInformation contextInformation) {
			fContextInformation= contextInformation;
		}

		/*
		 * @see IContextInformation#getContextDisplayString()
		 */
		public String getContextDisplayString() {
			return fContextInformation.getContextDisplayString();
		}

			/*
		 * @see IContextInformation#getImage()
		 */
		public Image getImage() {
			return fContextInformation.getImage();
		}

		/*
		 * @see IContextInformation#getInformationDisplayString()
		 */
		public String getInformationDisplayString() {
			return fContextInformation.getInformationDisplayString();
		}

		/*
		 * @see IContextInformationExtension#getContextInformationPosition()
		 */
		public int getContextInformationPosition() {
			return fPosition;
		}

		public void setContextInformationPosition(int position) {
			fPosition= position;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformation#equals(java.lang.Object)
		 */
		public boolean equals(Object object) {
			if (object instanceof ContextInformationWrapper)
				return fContextInformation.equals(((ContextInformationWrapper) object).fContextInformation);
			else
				return fContextInformation.equals(object);
		}
	}
	
	private String fErrorMessage;
	
	public JavaCompletionProposalComputer() {
	}

	protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
		return context.getInvocationOffset();
	}

	private List addContextInformations(JavaContentAssistInvocationContext context, int offset, IProgressMonitor monitor) {
		List proposals= internalComputeCompletionProposals(offset, context, monitor);
		List result= new ArrayList(proposals.size());

		for (Iterator it= proposals.iterator(); it.hasNext();) {
			ICompletionProposal proposal= (ICompletionProposal) it.next();
			IContextInformation contextInformation= proposal.getContextInformation();
			if (contextInformation != null) {
				ContextInformationWrapper wrapper= new ContextInformationWrapper(contextInformation);
				wrapper.setContextInformationPosition(offset);
				result.add(wrapper);
			}
		}
		return result;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
			
			int contextInformationPosition= guessContextInformationPosition(javaContext);
			List result= addContextInformations(javaContext, contextInformationPosition, monitor);
			return result;
		}
		return Collections.EMPTY_LIST;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
			return internalComputeCompletionProposals(context.getInvocationOffset(), javaContext, monitor);
		}
		return Collections.EMPTY_LIST;
	}

	private List internalComputeCompletionProposals(int offset, JavaContentAssistInvocationContext context, IProgressMonitor monitor) {
		IJavaScriptUnit unit= context.getCompilationUnit();
		if (unit == null)
			return Collections.EMPTY_LIST;
		
		ITextViewer viewer= context.getViewer();
		
		CompletionProposalCollector collector= createCollector(context);
		collector.setInvocationContext(context);
		
		// Allow completions for unresolved types - since 3.3
		collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF, true);
		collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT, true);
		collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT, true);
		
		collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF, true);
		collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT, true);
		collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT, true);

		collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);
		
		// Set the favorite list to propose static members - since 3.3 
		collector.setFavoriteReferences(getFavoriteStaticMembers());

		try {
			Point selection= viewer.getSelectedRange();
			if (selection.y > 0)
				collector.setReplacementLength(selection.y);
			
				unit.codeComplete(offset, collector);
		} catch (JavaScriptModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			if (x.isDoesNotExist() && !unit.getJavaScriptProject().isOnIncludepath(unit))
				MessageDialog.openInformation(shell, JavaTextMessages.CompletionProcessor_error_notOnBuildPath_title, JavaTextMessages.CompletionProcessor_error_notOnBuildPath_message);
			else
				ErrorDialog.openError(shell, JavaTextMessages.CompletionProcessor_error_accessing_title, JavaTextMessages.CompletionProcessor_error_accessing_message, x.getStatus());
		}

		ICompletionProposal[] javaProposals= collector.getJavaCompletionProposals();
		int contextInformationOffset= guessContextInformationPosition(context);
		if (contextInformationOffset != offset) {
			for (int i= 0; i < javaProposals.length; i++) {
				if (javaProposals[i] instanceof JavaMethodCompletionProposal) {
					JavaMethodCompletionProposal jmcp= (JavaMethodCompletionProposal) javaProposals[i];
					jmcp.setContextInformationPosition(contextInformationOffset);
				}
			}
		}
		
		List proposals= new ArrayList(Arrays.asList(javaProposals));
		if (proposals.size() == 0) {
			String error= collector.getErrorMessage();
			if (error.length() > 0)
				fErrorMessage= error;
		}
		return proposals;
	}

	/**
	 * Returns the array with favorite static members.
	 * 
	 * @return the <code>String</code> array with with favorite static members
	 * @see org.eclipse.wst.jsdt.core.CompletionRequestor#setFavoriteReferences(String[])
	 * 
	 */
	private String[] getFavoriteStaticMembers() {
		String serializedFavorites= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
		if (serializedFavorites != null && serializedFavorites.length() > 0)
			return serializedFavorites.split(";"); //$NON-NLS-1$
		return new String[0];
	}

	/**
	 * Creates the collector used to get proposals from core.
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector = new CompletionProposalCollector(context.getCompilationUnit());
		collector.setInvocationContext(context);
		return collector;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	public void sessionStarted() {
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
	 */
	public void sessionEnded() {
		fErrorMessage= null;
	}
}
