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

package org.eclipse.wst.jsdt.internal.ui.text.spelling;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;

/**
 * Reconcile strategy for spell checking comments.
 * 
 */
public class JavaSpellingReconcileStrategy extends SpellingReconcileStrategy {

	public JavaSpellingReconcileStrategy(ISourceViewer viewer, SpellingService spellingService, String partitioning) {
		super(viewer, spellingService);
		fPartitioning = partitioning;
	}

	/**
	 * Spelling problem collector that forwards {@link SpellingProblem}s as
	 * {@link org.eclipse.wst.jsdt.core.compiler.IProblem}s to the
	 * {@link org.eclipse.wst.jsdt.core.compiler.IProblemRequestor}.
	 */
	private class JSSpellingProblemCollector implements ISpellingProblemCollector {
		private ISpellingProblemCollector fParentCollector;

		public JSSpellingProblemCollector(ISpellingProblemCollector parentCollector) {
			fParentCollector = parentCollector;
		}

		/*
		 * @see
		 * org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept
		 * (org.eclipse.ui.texteditor.spelling.SpellingProblem)
		 */
		public void accept(SpellingProblem problem) {
			try {
				String type = ((IDocumentExtension3) getDocument()).getPartition(fPartitioning, problem.getOffset(), false).getType();
				if (IJavaScriptPartitions.JAVA_DOC.equals(type) || IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT.equals(type) || IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type))
					fParentCollector.accept(problem);
			}
			catch (BadLocationException e) {
				fParentCollector.accept(problem);
			}
			catch (BadPartitioningException e) {
				fParentCollector.accept(problem);
			}
		}

		/*
		 * @seeorg.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#
		 * beginCollecting()
		 */
		public void beginCollecting() {
			fParentCollector.beginCollecting();
		}

		/*
		 * @seeorg.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#
		 * endCollecting()
		 */
		public void endCollecting() {
			fParentCollector.endCollecting();
		}
	}


	/** The id of the problem */
	public static final int SPELLING_PROBLEM_ID = 0x80000000;
	private String fPartitioning;

	protected ISpellingProblemCollector createSpellingProblemCollector() {
		return new JSSpellingProblemCollector(super.createSpellingProblemCollector());
	}
}
