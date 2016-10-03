/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.text.spelling;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.core.DocumentAdapter;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IQuickFixProcessor;

/**
 * Provides a JSDT IQuickFixProcessor for SpellingAnnotations
 */
public class SpellingQuickFixProcessor implements IQuickFixProcessor {
	private static class SpellingProposal implements IJavaCompletionProposal {
		ICompletionProposal fProposal;

		SpellingProposal(ICompletionProposal spellingProposal) {
			super();
			fProposal = spellingProposal;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal#getRelevance
		 * ()
		 */
		public int getRelevance() {
			return 50;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.text.contentassist.ICompletionProposal#apply(
		 * org.eclipse.jface.text.IDocument)
		 */
		public void apply(IDocument document) {
			fProposal.apply(document);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
		 * getAdditionalProposalInfo()
		 */
		public String getAdditionalProposalInfo() {
			return fProposal.getAdditionalProposalInfo();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
		 * getContextInformation()
		 */
		public IContextInformation getContextInformation() {
			return fProposal.getContextInformation();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
		 * getDisplayString()
		 */
		public String getDisplayString() {
			return fProposal.getDisplayString();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
		 */
		public Image getImage() {
			return fProposal.getImage();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection
		 * (org.eclipse.jface.text.IDocument)
		 */
		public Point getSelection(IDocument document) {
			return fProposal.getSelection(document);
		}
	}

	static final class SpellingProblemCollector implements ISpellingProblemCollector {
		IQuickAssistInvocationContext fContext = null;

		SpellingProblemCollector(final IInvocationContext context) {
			fContext = new IQuickAssistInvocationContext() {
				public ISourceViewer getSourceViewer() {
					return null;
				}

				public int getOffset() {
					return context.getSelectionOffset();
				}

				public int getLength() {
					return context.getSelectionLength();
				}
			};
		}

		private List fProposals = new ArrayList();

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept
		 * (org.eclipse.ui.texteditor.spelling.SpellingProblem)
		 */
		public void accept(SpellingProblem problem) {
			ICompletionProposal[] proposals = problem.getProposals(fContext);
			for (int i = 0; i < proposals.length; i++) {
				fProposals.add(new SpellingProposal(proposals[i]));
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#
		 * beginCollecting()
		 */
		public void beginCollecting() {
			fProposals.clear();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#
		 * endCollecting()
		 */
		public void endCollecting() {
		}

		IJavaCompletionProposal[] getProposals() {
			return (IJavaCompletionProposal[]) fProposals.toArray(new IJavaCompletionProposal[fProposals.size()]);
		}
	}

	/**
	 * 
	 */
	public SpellingQuickFixProcessor() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.jsdt.ui.text.java.IQuickFixProcessor#hasCorrections
	 * (org.eclipse.wst.jsdt.core.IJavaScriptUnit, int)
	 */
	public boolean hasCorrections(IJavaScriptUnit unit, int problemId) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.jsdt.ui.text.java.IQuickFixProcessor#getCorrections
	 * (org.eclipse.wst.jsdt.ui.text.java.IInvocationContext,
	 * org.eclipse.wst.jsdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		List regions = new ArrayList();
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].getMarkerType() == SpellingAnnotation.TYPE) {
				regions.add(new Region(locations[i].getOffset(), locations[i].getLength()));
			}
		}
		SpellingProblemCollector collector = new SpellingProblemCollector(context);
		if (!regions.isEmpty()) {
			SpellingContext spellingContext = new SpellingContext();
			spellingContext.setContentType(Platform.getContentTypeManager().getContentType(JavaScriptCore.JAVA_SOURCE_CONTENT_TYPE));
			EditorsUI.getSpellingService().check(new DocumentAdapter(context.getCompilationUnit().getBuffer()), (IRegion[]) regions.toArray(new IRegion[regions.size()]), spellingContext, collector, new NullProgressMonitor());
		}
		return collector.getProposals();
	}

}
