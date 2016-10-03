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
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public abstract class CleanUpAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	public CleanUpAction(IWorkbenchSite site) {
		super(site);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * 
	 * @param editor
	 *            the Java editor
	 */
	public CleanUpAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getCompilationUnit(fEditor) != null);
	}

	/**
	 * @return the name of this action, not <b>null</b>
	 */
	protected abstract String getActionName();

	/**
	 * @param units
	 *            the units to clean up
	 * @return the clean ups to be performed or <b>null</b> if none to be
	 *         performed
	 */
	protected abstract ICleanUp[] createCleanUps(IJavaScriptUnit[] units);

	/**
	 * @param units
	 *            to clean up
	 * @param cleanUps
	 *            clean ups to execute on units
	 * 
	 * @throws JavaScriptModelException
	 * @throws InvocationTargetException
	 */
	protected void performRefactoring(IJavaScriptUnit[] units, ICleanUp[] cleanUps) throws JavaScriptModelException, InvocationTargetException {
		RefactoringExecutionStarter.startCleanupRefactoring(units, cleanUps, getShell(), false, getActionName());
	}

	public void run(ITextSelection selection) {
		IJavaScriptUnit cu= getCompilationUnit(fEditor);
		if (cu != null) {
			run(cu);
		}
	}

	public void run(IStructuredSelection selection) {
		IJavaScriptUnit[] cus= getCompilationUnits(selection);
		if (cus.length == 0) {
			MessageDialog.openInformation(getShell(), getActionName(), ActionMessages.CleanUpAction_EmptySelection_description);
		} else if (cus.length == 1) {
			run(cus[0]);
		} else {
			runOnMultiple(cus);
		}
	}

	public void selectionChanged(ITextSelection selection) {
		setEnabled(getCompilationUnit(fEditor) != null);
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(isEnabled(selection));
	}

	private boolean isEnabled(IStructuredSelection selection) {
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaScriptElement) {
					IJavaScriptElement elem= (IJavaScriptElement)selected[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaScriptElement.TYPE:
								return elem.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT; // for browsing perspective
							case IJavaScriptElement.JAVASCRIPT_UNIT:
								return true;
							case IJavaScriptElement.IMPORT_CONTAINER:
								return true;
							case IJavaScriptElement.PACKAGE_FRAGMENT:
							case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
								IPackageFragmentRoot root= (IPackageFragmentRoot)elem.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
								return (root.getKind() == IPackageFragmentRoot.K_SOURCE);
							case IJavaScriptElement.JAVASCRIPT_PROJECT:
								// https://bugs.eclipse.org/bugs/show_bug.cgi?id=65638
								return true;
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					return true;
				}
			} catch (JavaScriptModelException e) {
				if (!e.isDoesNotExist()) {
					JavaScriptPlugin.log(e);
				}
			}
		}
		return false;
	}

	private void run(IJavaScriptUnit cu) {
		if (!ActionUtil.isEditable(fEditor, getShell(), cu))
			return;

		ICleanUp[] cleanUps= createCleanUps(new IJavaScriptUnit[] {
			cu
		});
		if (cleanUps == null)
			return;

		if (!ElementValidator.check(cu, getShell(), getActionName(), fEditor != null))
			return;

		try {
			performRefactoring(new IJavaScriptUnit[] {
				cu
			}, cleanUps);
		} catch (InvocationTargetException e) {
			JavaScriptPlugin.log(e);
			if (e.getCause() instanceof CoreException)
				showUnexpectedError((CoreException)e.getCause());
		} catch (JavaScriptModelException e) {
			showUnexpectedError(e);
		}
	}

	private void runOnMultiple(final IJavaScriptUnit[] cus) {
		ICleanUp[] cleanUps= createCleanUps(cus);
		if (cleanUps == null)
			return;

		MultiStatus status= new MultiStatus(JavaScriptUI.ID_PLUGIN, IStatus.OK, ActionMessages.CleanUpAction_MultiStateErrorTitle, null);
		for (int i= 0; i < cus.length; i++) {
			IJavaScriptUnit cu= cus[i];

			if (!ActionUtil.isOnBuildPath(cu)) {
				String cuLocation= cu.getPath().makeRelative().toString();
				String message= Messages.format(ActionMessages.CleanUpAction_CUNotOnBuildpathMessage, cuLocation);
				status.add(new Status(IStatus.INFO, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, message, null));
			}
		}
		if (!status.isOK()) {
			ErrorDialog.openError(getShell(), getActionName(), null, status);
			return;
		}

		try {
			performRefactoring(cus, cleanUps);
		} catch (InvocationTargetException e) {
			JavaScriptPlugin.log(e);
			if (e.getCause() instanceof CoreException)
				showUnexpectedError((CoreException)e.getCause());
		} catch (JavaScriptModelException e) {
			showUnexpectedError(e);
		}
	}

	private void showUnexpectedError(CoreException e) {
		String message2= Messages.format(ActionMessages.CleanUpAction_UnexpectedErrorMessage, e.getStatus().getMessage());
		IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, message2, null);
		ErrorDialog.openError(getShell(), getActionName(), null, status);
	}

	public IJavaScriptUnit[] getCompilationUnits(IStructuredSelection selection) {
		HashSet result= new HashSet();
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaScriptElement) {
					IJavaScriptElement elem= (IJavaScriptElement)selected[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaScriptElement.TYPE:
								if (elem.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT) {
									result.add(elem.getParent());
								}
								break;
							case IJavaScriptElement.JAVASCRIPT_UNIT:
								result.add(elem);
								break;
							case IJavaScriptElement.IMPORT_CONTAINER:
								result.add(elem.getParent());
								break;
							case IJavaScriptElement.PACKAGE_FRAGMENT:
								collectCompilationUnits((IPackageFragment)elem, result);
								break;
							case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
								collectCompilationUnits((IPackageFragmentRoot)elem, result);
								break;
							case IJavaScriptElement.JAVASCRIPT_PROJECT:
								IPackageFragmentRoot[] roots= ((IJavaScriptProject)elem).getPackageFragmentRoots();
								for (int k= 0; k < roots.length; k++) {
									collectCompilationUnits(roots[k], result);
								}
								break;
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					IPackageFragment[] packageFragments= ((LogicalPackage)selected[i]).getFragments();
					for (int k= 0; k < packageFragments.length; k++) {
						IPackageFragment pack= packageFragments[k];
						if (pack.exists()) {
							collectCompilationUnits(pack, result);
						}
					}
				}
			} catch (JavaScriptModelException e) {
				if (JavaModelUtil.isExceptionToBeLogged(e))
					JavaScriptPlugin.log(e);
			}
		}
		return (IJavaScriptUnit[])result.toArray(new IJavaScriptUnit[result.size()]);
	}

	private void collectCompilationUnits(IPackageFragment pack, Collection result) throws JavaScriptModelException {
		result.addAll(Arrays.asList(pack.getJavaScriptUnits()));
	}

	private void collectCompilationUnits(IPackageFragmentRoot root, Collection result) throws JavaScriptModelException {
		if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
			IJavaScriptElement[] children= root.getChildren();
			for (int i= 0; i < children.length; i++) {
				collectCompilationUnits((IPackageFragment)children[i], result);
			}
		}
	}

	private static IJavaScriptUnit getCompilationUnit(JavaEditor editor) {
		IJavaScriptElement element= JavaScriptUI.getEditorInputJavaElement(editor.getEditorInput());
		if (!(element instanceof IJavaScriptUnit))
			return null;

		return (IJavaScriptUnit)element;
	}

}
