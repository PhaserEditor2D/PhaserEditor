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


import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.WorkingCopyManager;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class JavaReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {


	private ITextEditor fEditor;

	private WorkingCopyManager fManager;
	private IDocumentProvider fDocumentProvider;
	private IProgressMonitor fProgressMonitor;
	private boolean fNotify= true;

	private IJavaReconcilingListener fJavaReconcilingListener;
	private boolean fIsJavaReconcilingListener;


	public JavaReconcilingStrategy(ITextEditor editor) {
		fEditor= editor;
		fManager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
		fDocumentProvider= JavaScriptPlugin.getDefault().getCompilationUnitDocumentProvider();
		fIsJavaReconcilingListener= fEditor instanceof IJavaReconcilingListener;
		if (fIsJavaReconcilingListener)
			fJavaReconcilingListener= (IJavaReconcilingListener)fEditor;
	}

	private IProblemRequestorExtension getProblemRequestorExtension() {
		IAnnotationModel model= fDocumentProvider.getAnnotationModel(fEditor.getEditorInput());
		if (model instanceof IProblemRequestorExtension)
			return (IProblemRequestorExtension) model;
		return null;
	}

	private void reconcile(final boolean initialReconcile) {
		final JavaScriptUnit[] ast= new JavaScriptUnit[1];
		try {
			final IJavaScriptUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput(), false);
			if (unit != null) {
				SafeRunner.run(new ISafeRunnable() {
					public void run() {
						try {
							
							/* fix for missing cancel flag communication */
							IProblemRequestorExtension extension= getProblemRequestorExtension();
							if (extension != null) {
								extension.setProgressMonitor(fProgressMonitor);
								extension.setIsActive(true);
							}
							
							try {
								boolean isASTNeeded= initialReconcile || JavaScriptPlugin.getDefault().getASTProvider().isActive(unit);
								// reconcile
								if (fIsJavaReconcilingListener && isASTNeeded) {
									int reconcileFlags= IJavaScriptUnit.FORCE_PROBLEM_DETECTION 
										| (ASTProvider.SHARED_AST_STATEMENT_RECOVERY ? IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY : 0)
										| (ASTProvider.SHARED_BINDING_RECOVERY ? IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY : 0);
											
									ast[0]= unit.reconcile(ASTProvider.SHARED_AST_LEVEL, reconcileFlags, null, fProgressMonitor);
									if (ast[0] != null) {
										// mark as unmodifiable
										ASTNodes.setFlagsToAST(ast[0], ASTNode.PROTECT);
									}
								} else
									unit.reconcile(IJavaScriptUnit.NO_AST, true, null, fProgressMonitor);
							} catch (OperationCanceledException ex) {
								Assert.isTrue(fProgressMonitor == null || fProgressMonitor.isCanceled());
								ast[0]= null;
							} finally {
								/* fix for missing cancel flag communication */
								if (extension != null) {
									extension.setProgressMonitor(null);
									extension.setIsActive(false);
								}
							}
							
						} catch (JavaScriptModelException ex) {
							handleException(ex);
						}
					}
					public void handleException(Throwable ex) {
						IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.OK, "Error in JSDT Core during reconcile", ex);  //$NON-NLS-1$
						JavaScriptPlugin.getDefault().getLog().log(status);
					}
				});
				
			}
		} finally {
			// Always notify listeners, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=55969 for the final solution
			try {
				if (fIsJavaReconcilingListener) {
					IProgressMonitor pm= fProgressMonitor;
					if (pm == null)
						pm= new NullProgressMonitor();
					fJavaReconcilingListener.reconciled(ast[0], !fNotify, pm);
				}
			} finally {
				fNotify= true;
			}
		}
	}

	/*
	 * @see IReconcilingStrategy#reconcile(IRegion)
	 */
	public void reconcile(IRegion partition) {
		reconcile(false);
	}

	/*
	 * @see IReconcilingStrategy#reconcile(DirtyRegion, IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile(false);
	}

	/*
	 * @see IReconcilingStrategy#setDocument(IDocument)
	 */
	public void setDocument(IDocument document) {
	}

	/*
	 * @see IReconcilingStrategyExtension#setProgressMonitor(IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;
	}

	/*
	 * @see IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile() {
		reconcile(true);
	}

	/**
	 * Tells this strategy whether to inform its listeners.
	 *
	 * @param notify <code>true</code> if listeners should be notified
	 */
	public void notifyListeners(boolean notify) {
		fNotify= notify;
	}

	/**
	 * Called before reconciling is started.
	 *
	 * 
	 */
	public void aboutToBeReconciled() {
		if (fIsJavaReconcilingListener)
			fJavaReconcilingListener.aboutToBeReconciled();
	}
}
