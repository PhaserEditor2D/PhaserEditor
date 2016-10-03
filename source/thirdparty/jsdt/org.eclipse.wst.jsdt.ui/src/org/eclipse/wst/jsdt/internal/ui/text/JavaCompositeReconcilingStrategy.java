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
package org.eclipse.wst.jsdt.internal.ui.text;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.java.IProblemRequestorExtension;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaReconcilingStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.spelling.JavaSpellingReconcileStrategy;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;

/**
 * Reconciling strategy for Java code. This is a composite strategy containing the
 * regular java model reconciler and the comment spelling strategy.
 */
public class JavaCompositeReconcilingStrategy  extends CompositeReconcilingStrategy {

	private ITextEditor fEditor;
	private JavaReconcilingStrategy fJavaStrategy;

	/**
	 * Creates a new Java reconciling strategy.
	 *
	 * @param viewer the source viewer
	 * @param editor the editor of the strategy's reconciler
	 * @param documentPartitioning the document partitioning this strategy uses for configuration
	 */
	public JavaCompositeReconcilingStrategy(ISourceViewer viewer, ITextEditor editor, String documentPartitioning) {
		fEditor= editor;
		fJavaStrategy= new JavaReconcilingStrategy(editor);
		setReconcilingStrategies(new IReconcilingStrategy[] {
			fJavaStrategy,
			new JavaSpellingReconcileStrategy(viewer, EditorsUI.getSpellingService(), IJavaScriptPartitions.JAVA_PARTITIONING)
		});
	}

	/**
	 * Returns the problem requestor for the editor's input element.
	 *
	 * @return the problem requestor for the editor's input element
	 */
	private IProblemRequestorExtension getProblemRequestorExtension() {
		IDocumentProvider p= fEditor.getDocumentProvider();
		if (p == null) {
			// work around for https://bugs.eclipse.org/bugs/show_bug.cgi?id=51522
			p= JavaScriptPlugin.getDefault().getCompilationUnitDocumentProvider();
		}
		IAnnotationModel m= p.getAnnotationModel(fEditor.getEditorInput());
		if (m instanceof IProblemRequestorExtension)
			return (IProblemRequestorExtension) m;
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.CompositeReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		IProblemRequestorExtension e= getProblemRequestorExtension();
		if (e != null) {
			try {
				e.beginReportingSequence();
				super.reconcile(dirtyRegion, subRegion);
			} finally {
				e.endReportingSequence();
			}
		} else {
			super.reconcile(dirtyRegion, subRegion);
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.CompositeReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		IProblemRequestorExtension e= getProblemRequestorExtension();
		if (e != null) {
			try {
				e.beginReportingSequence();
				super.reconcile(partition);
			} finally {
				e.endReportingSequence();
			}
		} else {
			super.reconcile(partition);
		}
	}

	/**
	 * Tells this strategy whether to inform its listeners.
	 *
	 * @param notify <code>true</code> if listeners should be notified
	 */
	public void notifyListeners(boolean notify) {
		fJavaStrategy.notifyListeners(notify);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.CompositeReconcilingStrategy#initialReconcile()
	 */
	public void initialReconcile() {
		IProblemRequestorExtension e= getProblemRequestorExtension();
		if (e != null) {
			try {
				e.beginReportingSequence();
				super.initialReconcile();
			} finally {
				e.endReportingSequence();
			}
		} else {
			super.initialReconcile();
		}
	}

	/**
	 * Called before reconciling is started.
	 *
	 * 
	 */
	public void aboutToBeReconciled() {
		fJavaStrategy.aboutToBeReconciled();

	}
}
