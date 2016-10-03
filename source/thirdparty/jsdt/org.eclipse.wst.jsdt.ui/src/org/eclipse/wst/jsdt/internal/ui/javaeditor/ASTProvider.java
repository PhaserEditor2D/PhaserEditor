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

package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;


/**
 * Provides a shared AST for clients. The shared AST is
 * the AST of the active Java editor's input element.
 *
 * 
 */
public final class ASTProvider {

	/**
	 * Wait flag.
	 *
	 * 
	 */
	public static final class WAIT_FLAG {

		String fName;

		private WAIT_FLAG(String name) {
			fName= name;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return fName;
		}
	}

	/**
	 * Wait flag indicating that a client requesting an AST
	 * wants to wait until an AST is ready.
	 * <p>
	 * An AST will be created by this AST provider if the shared
	 * AST is not for the given java element.
	 * </p>
	 *
	 * 
	 */
	public static final WAIT_FLAG WAIT_YES= new WAIT_FLAG("wait yes"); //$NON-NLS-1$

	/**
	 * Wait flag indicating that a client requesting an AST
	 * only wants to wait for the shared AST of the active editor.
	 * <p>
	 * No AST will be created by the AST provider.
	 * </p>
	 *
	 * 
	 */
	public static final WAIT_FLAG WAIT_ACTIVE_ONLY= new WAIT_FLAG("wait active only"); //$NON-NLS-1$

	/**
	 * Wait flag indicating that a client requesting an AST
	 * only wants the already available shared AST.
	 * <p>
	 * No AST will be created by the AST provider.
	 * </p>
	 *
	 * 
	 */
	public static final WAIT_FLAG WAIT_NO= new WAIT_FLAG("don't wait"); //$NON-NLS-1$


	/**
	 * Tells whether this class is in debug mode.
	 * 
	 */
	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.jsdt.ui/debug/ASTProvider"));  //$NON-NLS-1$//$NON-NLS-2$


	/**
	 * Internal activation listener.
	 *
	 * 
	 */
	private class ActivationListener implements IPartListener2, IWindowListener {


		/*
		 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partActivated(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partBroughtToTop(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partClosed(IWorkbenchPartReference ref) {
			if (isActiveEditor(ref)) {
				if (DEBUG)
					System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "closed active editor: " + ref.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$

				activeJavaEditorChanged(null);
			}
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partDeactivated(IWorkbenchPartReference ref) {
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partOpened(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partHidden(IWorkbenchPartReference ref) {
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partVisible(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partInputChanged(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowActivated(IWorkbenchWindow window) {
			IWorkbenchPartReference ref= window.getPartService().getActivePartReference();
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowDeactivated(IWorkbenchWindow window) {
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowClosed(IWorkbenchWindow window) {
			if (fActiveEditor != null && fActiveEditor.getSite() != null && window == fActiveEditor.getSite().getWorkbenchWindow()) {
				if (DEBUG)
					System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "closed active editor: " + fActiveEditor.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$

				activeJavaEditorChanged(null);
			}
			window.getPartService().removePartListener(this);
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowOpened(IWorkbenchWindow window) {
			window.getPartService().addPartListener(this);
		}

		private boolean isActiveEditor(IWorkbenchPartReference ref) {
			return ref != null && isActiveEditor(ref.getPart(false));
		}

		private boolean isActiveEditor(IWorkbenchPart part) {
			return part != null && (part == fActiveEditor);
		}

		private boolean isJavaEditor(IWorkbenchPartReference ref) {
			if (ref == null)
				return false;

			String id= ref.getId();

			// The instanceof check is not need but helps clients, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=84862
			return JavaScriptUI.ID_CF_EDITOR.equals(id) || JavaScriptUI.ID_CU_EDITOR.equals(id) || ref.getPart(false) instanceof JavaEditor;
		}
	}

	public static final int SHARED_AST_LEVEL= AST.JLS3;
	public static final boolean SHARED_AST_STATEMENT_RECOVERY= true;
	public static final boolean SHARED_BINDING_RECOVERY= true;

	private static final String DEBUG_PREFIX= "ASTProvider > "; //$NON-NLS-1$


	private IJavaScriptElement fReconcilingJavaElement;
	private IJavaScriptElement fActiveJavaElement;
	private JavaScriptUnit fAST;
	private ActivationListener fActivationListener;
	private Object fReconcileLock= new Object();
	private Object fWaitLock= new Object();
	private boolean fIsReconciling;
	private IWorkbenchPart fActiveEditor;

	
	/**
	 * Returns the Java plug-in's AST provider.
	 * 
	 * @return the AST provider
	 * 
	 */
	public static ASTProvider getASTProvider() {
		return JavaScriptPlugin.getDefault().getASTProvider();
	}
	
	/**
	 * Creates a new AST provider.
	 */
	public ASTProvider() {
		install();
	}

	/**
	 * Installs this AST provider.
	 */
	void install() {
		// Create and register activation listener
		fActivationListener= new ActivationListener();
		PlatformUI.getWorkbench().addWindowListener(fActivationListener);

		// Ensure existing windows get connected
		IWorkbenchWindow[] windows= PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i= 0, length= windows.length; i < length; i++)
			windows[i].getPartService().addPartListener(fActivationListener);
	}

	private void activeJavaEditorChanged(IWorkbenchPart editor) {

		IJavaScriptElement javaElement= null;
		if (editor instanceof JavaEditor)
			javaElement= ((JavaEditor)editor).getInputJavaElement();

		synchronized (this) {
			fActiveEditor= editor;
			fActiveJavaElement= javaElement;
			cache(null, javaElement);
		}

		if (DEBUG)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "active editor is: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$

		synchronized (fReconcileLock) {
			if (fIsReconciling && (fReconcilingJavaElement == null || !fReconcilingJavaElement.equals(javaElement))) {
				fIsReconciling= false;
				fReconcilingJavaElement= null;
			} else if (javaElement == null) {
				fIsReconciling= false;
				fReconcilingJavaElement= null;
			}
		}
	}

	/**
	 * Returns whether the given compilation unit AST is
	 * cached by this AST provided.
	 *
	 * @param ast the compilation unit AST
	 * @return <code>true</code> if the given AST is the cached one
	 */
	public boolean isCached(JavaScriptUnit ast) {
		return ast != null && fAST == ast;
	}

	/**
	 * Returns whether this AST provider is active on the given
	 * compilation unit.
	 *
	 * @param cu the compilation unit
	 * @return <code>true</code> if the given compilation unit is the active one
	 * 
	 */
	public boolean isActive(IJavaScriptUnit cu) {
		return cu != null && cu.equals(fActiveJavaElement);
	}

	/**
	 * Informs that reconciling for the given element is about to be started.
	 *
	 * @param javaElement the Java element
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	void aboutToBeReconciled(IJavaScriptElement javaElement) {

		if (javaElement == null)
			return;

		if (DEBUG)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "about to reconcile: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$

		synchronized (fReconcileLock) {
			fIsReconciling= true;
			fReconcilingJavaElement= javaElement;
		}
		cache(null, javaElement);
	}

	/**
	 * Disposes the cached AST.
	 */
	private synchronized void disposeAST() {

		if (fAST == null)
			return;

		if (DEBUG)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "disposing AST: " + toString(fAST) + " for: " + toString(fActiveJavaElement)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		fAST= null;

		cache(null, null);
	}

	/**
	 * Returns a string for the given Java element used for debugging.
	 *
	 * @param javaElement the compilation unit AST
	 * @return a string used for debugging
	 */
	private String toString(IJavaScriptElement javaElement) {
		if (javaElement == null)
			return "null"; //$NON-NLS-1$
		else
			return javaElement.getElementName();

	}

	/**
	 * Returns a string for the given AST used for debugging.
	 *
	 * @param ast the compilation unit AST
	 * @return a string used for debugging
	 */
	private String toString(JavaScriptUnit ast) {
		if (ast == null)
			return "null"; //$NON-NLS-1$

		List types= ast.types();
		if (types != null && types.size() > 0)
			return ((AbstractTypeDeclaration)types.get(0)).getName().getIdentifier();
		else
			return "AST without any type"; //$NON-NLS-1$
	}

	/**
	 * Caches the given compilation unit AST for the given Java element.
	 *
	 * @param ast
	 * @param javaElement
	 */
	private synchronized void cache(JavaScriptUnit ast, IJavaScriptElement javaElement) {

		if (fActiveJavaElement != null && !fActiveJavaElement.equals(javaElement)) {
			if (DEBUG && javaElement != null) // don't report call from disposeAST()
				System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "don't cache AST for inactive: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		if (DEBUG && (javaElement != null || ast != null)) // don't report call from disposeAST()
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "caching AST: " + toString(ast) + " for: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (fAST != null)
			disposeAST();

		fAST= ast;

		// Signal AST change
		synchronized (fWaitLock) {
			fWaitLock.notifyAll();
		}
	}

	/**
	 * Returns a shared compilation unit AST for the given
	 * Java element.
	 * <p>
	 * Clients are not allowed to modify the AST and must
	 * synchronize all access to its nodes.
	 * </p>
	 *
	 * @param je				the Java element
	 * @param waitFlag			{@link #WAIT_YES}, {@link #WAIT_NO} or {@link #WAIT_ACTIVE_ONLY}
	 * @param progressMonitor	the progress monitor or <code>null</code>
	 * @return					the AST or <code>null</code> if the AST is not available
	 */
	public JavaScriptUnit getAST(IJavaScriptElement je, WAIT_FLAG waitFlag, IProgressMonitor progressMonitor) {
		if (je == null)
			return null;
		
		Assert.isTrue(je.getElementType() == IJavaScriptElement.CLASS_FILE || je.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT);

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		boolean isActiveElement;
		synchronized (this) {
			isActiveElement= je.equals(fActiveJavaElement);
			if (isActiveElement) {
				if (fAST != null) {
					if (DEBUG)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "returning cached AST:" + toString(fAST) + " for: " + je.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

					return fAST;
				}
				if (waitFlag == WAIT_NO) {
					if (DEBUG)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "returning null (WAIT_NO) for: " + je.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$

					return null;

				}
			}
		}
		if (isActiveElement && isReconciling(je)) {
			try {
				final IJavaScriptElement activeElement= fReconcilingJavaElement;

				// Wait for AST
				synchronized (fWaitLock) {
					if (DEBUG)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "waiting for AST for: " + je.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$

					fWaitLock.wait();
				}

				// Check whether active element is still valid
				synchronized (this) {
					if (activeElement == fActiveJavaElement && fAST != null) {
						if (DEBUG)
							System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "...got AST for: " + je.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$

						return fAST;
					}
				}
				return getAST(je, waitFlag, progressMonitor);
			} catch (InterruptedException e) {
				return null; // thread has been interrupted don't compute AST
			}
		} else if (waitFlag == WAIT_NO || (waitFlag == WAIT_ACTIVE_ONLY && !(isActiveElement && fAST == null)))
			return null;

		if (isActiveElement)
			aboutToBeReconciled(je);

		JavaScriptUnit ast= null;
		try {
			ast= createAST(je, progressMonitor);
			if (progressMonitor != null && progressMonitor.isCanceled()) {
				ast= null;
				if (DEBUG)
					System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "Ignore created AST for: " + je.getElementName() + " - operation has been cancelled"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} finally {
			if (isActiveElement) {
				if (fAST != null) {
					if (DEBUG)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "Ignore created AST for " + je.getElementName() + " - AST from reconciler is newer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					reconciled(fAST, je, null);
				} else
					reconciled(ast, je, null);
			}
		}

		return ast;
	}

	/**
	 * Tells whether the given Java element is the one
	 * reported as currently being reconciled.
	 *
	 * @param javaElement the Java element
	 * @return <code>true</code> if reported as currently being reconciled
	 */
	private boolean isReconciling(IJavaScriptElement javaElement) {
		synchronized (fReconcileLock) {
			return javaElement != null && javaElement.equals(fReconcilingJavaElement) && fIsReconciling;
		}
	}

	/**
	 * Creates a new compilation unit AST.
	 *
	 * @param je the Java element for which to create the AST
	 * @param progressMonitor the progress monitor
	 * @return AST
	 */
	private JavaScriptUnit createAST(final IJavaScriptElement je, final IProgressMonitor progressMonitor) {
		if (!hasSource(je))
			return null;
		
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		final ASTParser parser = ASTParser.newParser(SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(SHARED_AST_STATEMENT_RECOVERY);
		parser.setBindingsRecovery(SHARED_BINDING_RECOVERY);

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		if (je.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT)
			parser.setSource((IJavaScriptUnit)je);
		else if (je.getElementType() == IJavaScriptElement.CLASS_FILE)
			parser.setSource((IClassFile)je);

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		final JavaScriptUnit root[]= new JavaScriptUnit[1]; 
		
		SafeRunner.run(new ISafeRunnable() {
			public void run() {
				try {
					if (progressMonitor != null && progressMonitor.isCanceled())
						return;
					if (DEBUG)
						System.err.println(getThreadName() + " - " + DEBUG_PREFIX + "creating AST for: " + je.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$
					root[0]= (JavaScriptUnit)parser.createAST(progressMonitor);
				} catch (OperationCanceledException ex) {
					return;
				}
			}
			public void handleException(Throwable ex) {
				IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.OK, "Error in JSDT Core during AST creation", ex);  //$NON-NLS-1$
				JavaScriptPlugin.getDefault().getLog().log(status);
			}
		});
		
		// mark as unmodifiable
		if (root[0] != null)
			ASTNodes.setFlagsToAST(root[0], ASTNode.PROTECT);
		
		return root[0];
	}
	
	/**
	 * Checks whether the given Java element has accessible source.
	 * 
	 * @param je the Java element to test
	 * @return <code>true</code> if the element has source
	 * 
	 */
	private boolean hasSource(IJavaScriptElement je) {
		if (je == null || !je.exists())
			return false;
		
		try {
			return je instanceof ITypeRoot && ((ITypeRoot)je).getBuffer() != null;
		} catch (JavaScriptModelException ex) {
			IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.OK, "Error in JSDT Core during AST creation", ex);  //$NON-NLS-1$
			JavaScriptPlugin.getDefault().getLog().log(status);
		}
		return false;
	}
	
	/**
	 * Disposes this AST provider.
	 */
	public void dispose() {

		// Dispose activation listener
		PlatformUI.getWorkbench().removeWindowListener(fActivationListener);
		fActivationListener= null;

		disposeAST();

		synchronized (fWaitLock) {
			fWaitLock.notifyAll();
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(org.eclipse.wst.jsdt.core.dom.JavaScriptUnit)
	 */
	void reconciled(JavaScriptUnit ast, IJavaScriptElement javaElement, IProgressMonitor progressMonitor) {

		if (DEBUG)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "reconciled: " + toString(javaElement) + ", AST: " + toString(ast)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		synchronized (fReconcileLock) {

			fIsReconciling= progressMonitor != null && progressMonitor.isCanceled();
			if (javaElement == null || !javaElement.equals(fReconcilingJavaElement)) {

				if (DEBUG)
					System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "  ignoring AST of out-dated editor"); //$NON-NLS-1$ //$NON-NLS-2$

				// Signal - threads might wait for wrong element
				synchronized (fWaitLock) {
					fWaitLock.notifyAll();
				}

				return;
			}

			cache(ast, javaElement);
		}
	}

	private String getThreadName() {
		String name= Thread.currentThread().getName();
		if (name != null)
			return name;
		else
			return Thread.currentThread().toString();
	}
	
}

