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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;

/**
 * Infrastructure to share an AST for editor post selection listeners.
 */
public class SelectionListenerWithASTManager {
	
	private static SelectionListenerWithASTManager fgDefault;
	
	/**
	 * @return Returns the default manager instance.
	 */
	public static SelectionListenerWithASTManager getDefault() {
		if (fgDefault == null) {
			fgDefault= new SelectionListenerWithASTManager();
		}
		return fgDefault;
	}
	
	
	private final static class PartListenerGroup {
		private ITextEditor fPart;
		private ISelectionListener fPostSelectionListener;
		private ISelectionChangedListener fSelectionListener;
		private Job fCurrentJob;
		private ListenerList fAstListeners;
		/**
		 * Lock to avoid having more than one calculateAndInform job in parallel.
		 * Only jobs may synchronize on this as otherwise deadlocks are possible.
		 */
		private final Object fJobLock= new Object();
		
		public PartListenerGroup(ITextEditor editorPart) {
			fPart= editorPart;
			fCurrentJob= null;
			fAstListeners= new ListenerList(ListenerList.IDENTITY);
			
			fSelectionListener= new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					ISelection selection= event.getSelection();
					if (selection instanceof ITextSelection) {
						fireSelectionChanged((ITextSelection) selection);
					}
				}
			};
			
			fPostSelectionListener= new ISelectionListener() {
				public void selectionChanged(IWorkbenchPart part, ISelection selection) {
					if (part == fPart && selection instanceof ITextSelection)
						firePostSelectionChanged((ITextSelection) selection);
				}
			};
		}

		public boolean isEmpty() {
			return fAstListeners.isEmpty();
		}

		public void install(ISelectionListenerWithAST listener) {
			if (isEmpty()) {
				fPart.getEditorSite().getPage().addPostSelectionListener(fPostSelectionListener);
				ISelectionProvider selectionProvider= fPart.getSelectionProvider();
				if (selectionProvider != null)
						selectionProvider.addSelectionChangedListener(fSelectionListener);
			}
			fAstListeners.add(listener);
		}
		
		public void uninstall(ISelectionListenerWithAST listener) {
			fAstListeners.remove(listener);
			if (isEmpty()) {
				fPart.getEditorSite().getPage().removePostSelectionListener(fPostSelectionListener);
				ISelectionProvider selectionProvider= fPart.getSelectionProvider();
				if (selectionProvider != null)
					selectionProvider.removeSelectionChangedListener(fSelectionListener);
			}
		}
		
		public void fireSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
		}
		
		public void firePostSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
			final IJavaScriptElement input= EditorUtility.getEditorInputJavaElement(fPart, false);
			if (input == null) {
				return;
			}
			
			fCurrentJob= new Job(JavaUIMessages.SelectionListenerWithASTManager_job_title) { 
				public IStatus run(IProgressMonitor monitor) {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					synchronized (fJobLock) {
						return calculateASTandInform(input, selection, monitor);
					}
				}
			};
			fCurrentJob.setPriority(Job.DECORATE);
			fCurrentJob.setSystem(true);
			fCurrentJob.schedule();
		}
		
		protected IStatus calculateASTandInform(IJavaScriptElement input, ITextSelection selection, IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// create AST
			try {
				JavaScriptUnit astRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(input, ASTProvider.WAIT_ACTIVE_ONLY, monitor);
			
				if (astRoot != null && !monitor.isCanceled()) {
					Object[] listeners;
					synchronized (PartListenerGroup.this) {
						listeners= fAstListeners.getListeners();
					}
					for (int i= 0; i < listeners.length; i++) {
						((ISelectionListenerWithAST) listeners[i]).selectionChanged(fPart, selection, astRoot);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
					return Status.OK_STATUS;
				}
			} catch (OperationCanceledException e) {
				// thrown when canceling the AST creation
			}
			return Status.CANCEL_STATUS;
		}
	}
	
		
	private Map fListenerGroups;
	
	private SelectionListenerWithASTManager() {
		fListenerGroups= new HashMap();
	}
	
	/**
	 * Registers a selection listener for the given editor part.
	 * @param part The editor part to listen to.
	 * @param listener The listener to register.
	 */
	public void addListener(ITextEditor part, ISelectionListenerWithAST listener) {
		synchronized (this) {
			PartListenerGroup partListener= (PartListenerGroup) fListenerGroups.get(part);
			if (partListener == null) {
				partListener= new PartListenerGroup(part);
				fListenerGroups.put(part, partListener);
			}
			partListener.install(listener);
		}
	}

	/**
	 * Unregisters a selection listener.
	 * @param part The editor part the listener was registered.
	 * @param listener The listener to unregister.
	 */
	public void removeListener(ITextEditor part, ISelectionListenerWithAST listener) {
		synchronized (this) {
			PartListenerGroup partListener= (PartListenerGroup) fListenerGroups.get(part);
			if (partListener != null) {
				partListener.uninstall(listener);
				if (partListener.isEmpty()) {
					fListenerGroups.remove(part);
				}
			}
		}
	}
}
