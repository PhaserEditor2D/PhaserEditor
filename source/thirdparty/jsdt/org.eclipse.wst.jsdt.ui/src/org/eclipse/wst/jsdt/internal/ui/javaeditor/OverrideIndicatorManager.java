/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.java.IJavaReconcilingListener;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Manages the override and overwrite indicators for
 * the given Java element and annotation model.
 *
 * 
 */
class OverrideIndicatorManager implements IJavaReconcilingListener {

	/**
	 * Overwrite and override indicator annotation.
	 *
	 * 
	 */
	class OverrideIndicator extends Annotation {

		private boolean fIsOverwriteIndicator;
		private String fAstNodeKey;

		/**
		 * Creates a new override annotation.
		 *
		 * @param isOverwriteIndicator <code>true</code> if this annotation is
		 *            an overwrite indicator, <code>false</code> otherwise
		 * @param text the text associated with this annotation
		 * @param key the method binding key
		 * 
		 */
		OverrideIndicator(boolean isOverwriteIndicator, String text, String key) {
			super(ANNOTATION_TYPE, false, text);
			fIsOverwriteIndicator= isOverwriteIndicator;
			fAstNodeKey= key;
		}

		/**
		 * Tells whether this is an overwrite or an override indicator.
		 *
		 * @return <code>true</code> if this is an overwrite indicator
		 */
		public boolean isOverwriteIndicator() {
			return fIsOverwriteIndicator;
		}

		/**
		 * Opens and reveals the defining method.
		 */
		public void open() {
			JavaScriptUnit ast= ASTProvider.getASTProvider().getAST(fJavaElement, ASTProvider.WAIT_ACTIVE_ONLY, null);
			if (ast != null) {
				ASTNode node= ast.findDeclaringNode(fAstNodeKey);
				if (node instanceof FunctionDeclaration) {
					try {
						IFunctionBinding methodBinding= ((FunctionDeclaration)node).resolveBinding();
						IFunctionBinding definingMethodBinding= Bindings.findOverriddenMethod(methodBinding, true);
						if (definingMethodBinding != null) {
							IJavaScriptElement definingMethod= definingMethodBinding.getJavaElement();
							if (definingMethod != null) {
								JavaScriptUI.openInEditor(definingMethod, true, true);
								return;
							}
						}
					} catch (CoreException e) {
						ExceptionHandler.handle(e, JavaEditorMessages.OverrideIndicatorManager_open_error_title, JavaEditorMessages.OverrideIndicatorManager_open_error_messageHasLogEntry);
						return;
					}
				}
			}
			String title= JavaEditorMessages.OverrideIndicatorManager_open_error_title;
			String message= JavaEditorMessages.OverrideIndicatorManager_open_error_message;
			MessageDialog.openError(JavaScriptPlugin.getActiveWorkbenchShell(), title, message);
		}
	}

	static final String ANNOTATION_TYPE= "org.eclipse.wst.jsdt.ui.overrideIndicator"; //$NON-NLS-1$

	private IAnnotationModel fAnnotationModel;
	private Object fAnnotationModelLockObject;
	private Annotation[] fOverrideAnnotations;
	private IJavaScriptElement fJavaElement;


	public OverrideIndicatorManager(IAnnotationModel annotationModel, IJavaScriptElement javaElement, JavaScriptUnit ast) {
		Assert.isNotNull(annotationModel);
		Assert.isNotNull(javaElement);

		fJavaElement= javaElement;
		fAnnotationModel=annotationModel;
		fAnnotationModelLockObject= getLockObject(fAnnotationModel);

		updateAnnotations(ast, new NullProgressMonitor());
	}

	/**
	 * Returns the lock object for the given annotation model.
	 *
	 * @param annotationModel the annotation model
	 * @return the annotation model's lock object
	 * 
	 */
	private Object getLockObject(IAnnotationModel annotationModel) {
		if (annotationModel instanceof ISynchronizable) {
			Object lock= ((ISynchronizable)annotationModel).getLockObject();
			if (lock != null)
				return lock;
		}
		return annotationModel;
	}

	/**
	 * Updates the override and implements annotations based
	 * on the given AST.
	 *
	 * @param ast the compilation unit AST
	 * @param progressMonitor the progress monitor
	 * 
	 */
	protected void updateAnnotations(JavaScriptUnit ast, IProgressMonitor progressMonitor) {

		if (ast == null || progressMonitor.isCanceled())
			return;

		final Map annotationMap= new HashMap(50);

		ast.accept(new ASTVisitor(false) {
			/*
			 * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionDeclaration)
			 */
			public boolean visit(FunctionDeclaration node) {
				IFunctionBinding binding= node.resolveBinding();
				if (binding != null) {
					IFunctionBinding definingMethod= Bindings.findOverriddenMethod(binding, true);
					if (definingMethod != null) {

						ITypeBinding definingType= definingMethod.getDeclaringClass();
						String qualifiedMethodName= definingType.getQualifiedName() + "." + binding.getName(); //$NON-NLS-1$

						boolean isImplements= JdtFlags.isAbstract(definingMethod);
						String text;
						if (isImplements)
							text= Messages.format(JavaEditorMessages.OverrideIndicatorManager_implements, qualifiedMethodName);
						else
							text= Messages.format(JavaEditorMessages.OverrideIndicatorManager_overrides, qualifiedMethodName);

						SimpleName name= node.getName();
						Position position= name != null ?
									new Position(name.getStartPosition(), name.getLength()) :
										new Position(node.getStartPosition());

						annotationMap.put(
								new OverrideIndicator(isImplements, text, binding.getKey()), 
								position);

					}
				}
				return true;
			}
		});

		if (progressMonitor.isCanceled())
			return;

		synchronized (fAnnotationModelLockObject) {
			if (fAnnotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(fOverrideAnnotations, annotationMap);
			} else {
				removeAnnotations();
				Iterator iter= annotationMap.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry mapEntry= (Map.Entry)iter.next();
					fAnnotationModel.addAnnotation((Annotation)mapEntry.getKey(), (Position)mapEntry.getValue());
				}
			}
			fOverrideAnnotations= (Annotation[])annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
		}
	}

	/**
	 * Removes all override indicators from this manager's annotation model.
	 */
	void removeAnnotations() {
		if (fOverrideAnnotations == null)
			return;

		synchronized (fAnnotationModelLockObject) {
			if (fAnnotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(fOverrideAnnotations, null);
			} else {
				for (int i= 0, length= fOverrideAnnotations.length; i < length; i++)
					fAnnotationModel.removeAnnotation(fOverrideAnnotations[i]);
			}
			fOverrideAnnotations= null;
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	public void aboutToBeReconciled() {
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(JavaScriptUnit, boolean, IProgressMonitor)
	 */
	public void reconciled(JavaScriptUnit ast, boolean forced, IProgressMonitor progressMonitor) {
		updateAnnotations(ast, progressMonitor);
	}
}

