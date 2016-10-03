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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.GlobalBuildAction;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.ListDialog;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

public class RefactoringSaveHelper {

	private boolean fFilesSaved;
	private final int fSaveMode;

	/**
	 * Save mode to save all dirty editors (always ask).
	 */
	public static final int SAVE_ALL_ALWAYS_ASK= 1;
	
	/**
	 * Save mode to save all dirty editors.
	 */
	public static final int SAVE_ALL= 2;
	
	/**
	 * Save mode to save all unknown editors, i.e. those that don't work on
	 * resources, don't use file buffers, or are otherwise suspect.
	 * 
	 * Used for refactorings with participants or qualified name updating.
	 */
	public static final int SAVE_NON_JAVA_UPDATES= 3;
	
	/**
	 * Save mode to save only dirty editors on compilation units that are not in
	 * working copy mode.
	 * 
	 * Used for refactorings without participants or qualified name updating.
	 */
	public static final int SAVE_JAVA_ONLY_UPDATES= 4;
	
	/**
	 * Save mode to not save save any editors.
	 */
	public static final int SAVE_NOTHING= 5;
	
	/**
	 * @param saveMode one of the SAVE_* constants
	 */
	public RefactoringSaveHelper(int saveMode) {
		Assert.isTrue(saveMode == SAVE_ALL_ALWAYS_ASK
				|| saveMode == SAVE_ALL
				|| saveMode == SAVE_NON_JAVA_UPDATES
				|| saveMode == SAVE_JAVA_ONLY_UPDATES
				|| saveMode == SAVE_NOTHING);
		fSaveMode= saveMode;
	}
	
	/**
	 * @param shell
	 * @return <code>true</code> if save was successful and refactoring can proceed;
	 * 		false if the refactoring must be cancelled
	 */
	public boolean saveEditors(Shell shell) {
		final IEditorPart[] dirtyEditors;
		switch (fSaveMode) {
			case SAVE_ALL_ALWAYS_ASK:
			case SAVE_ALL:
				dirtyEditors= EditorUtility.getDirtyEditors();
				break;
				
			case SAVE_NON_JAVA_UPDATES:
				dirtyEditors= EditorUtility.getDirtyEditorsToSave(false); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=175495
				break;
				
			case SAVE_JAVA_ONLY_UPDATES:
				dirtyEditors= EditorUtility.getDirtyEditorsToSave(false);
				break;
				
			case SAVE_NOTHING:
				return true;
				
			default:
				throw new IllegalStateException(Integer.toString(fSaveMode));
		}
		if (dirtyEditors.length == 0)
			return true;
		if (! askSaveAllDirtyEditors(shell, dirtyEditors))
			return false;
		try {
			// Save isn't cancelable.
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description= workspace.getDescription();
			boolean autoBuild= description.isAutoBuilding();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
			try {
				if (fSaveMode == SAVE_ALL_ALWAYS_ASK || fSaveMode == SAVE_ALL
						|| RefactoringSavePreferences.getSaveAllEditors()) {
					if (!JavaScriptPlugin.getActiveWorkbenchWindow().getWorkbench().saveAllEditors(false))
						return false;
				} else {
					IRunnableWithProgress runnable= new IRunnableWithProgress() {
						public void run(IProgressMonitor pm) throws InterruptedException {
							int count= dirtyEditors.length;
							pm.beginTask("", count); //$NON-NLS-1$
							for (int i= 0; i < count; i++) {
								IEditorPart editor= dirtyEditors[i];
								editor.doSave(new SubProgressMonitor(pm, 1));
								if (pm.isCanceled())
									throw new InterruptedException();
							}
							pm.done();
						}
					};
					try {
						PlatformUI.getWorkbench().getProgressService().runInUI(JavaScriptPlugin.getActiveWorkbenchWindow(), runnable, null);
					} catch (InterruptedException e) {
						return false;
					} catch (InvocationTargetException e) {
						ExceptionHandler.handle(e, shell, 
								RefactoringMessages.RefactoringStarter_saving, RefactoringMessages.RefactoringStarter_unexpected_exception);
						return false;
					}
				}
				fFilesSaved= true;
			} finally {
				description.setAutoBuilding(autoBuild);
				workspace.setDescription(description);
			}
			return true;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, 
				RefactoringMessages.RefactoringStarter_saving, RefactoringMessages.RefactoringStarter_unexpected_exception);  
			return false;
		}
	}

	public void triggerBuild() {
		if (fFilesSaved && ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding()) {
			new GlobalBuildAction(JavaScriptPlugin.getActiveWorkbenchWindow(), IncrementalProjectBuilder.INCREMENTAL_BUILD).run();
		}
	}
	
	private boolean askSaveAllDirtyEditors(Shell shell, IEditorPart[] dirtyEditors) {
		final boolean canSaveAutomatically= fSaveMode != SAVE_ALL_ALWAYS_ASK;
		if (canSaveAutomatically && RefactoringSavePreferences.getSaveAllEditors()) //must save everything
			return true;
		ListDialog dialog= new ListDialog(shell) {
			{
				setShellStyle(getShellStyle() | SWT.APPLICATION_MODAL);
			}
			protected Control createDialogArea(Composite parent) {
				Composite result= (Composite) super.createDialogArea(parent);
				if (canSaveAutomatically) {
					final Button check= new Button(result, SWT.CHECK);
					check.setText(RefactoringMessages.RefactoringStarter_always_save); 
					check.setSelection(RefactoringSavePreferences.getSaveAllEditors());
					check.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							RefactoringSavePreferences.setSaveAllEditors(check.getSelection());
						}
					});
					applyDialogFont(result);
				}
				return result;
			}
		};
		dialog.setTitle(RefactoringMessages.RefactoringStarter_save_all_resources); 
		dialog.setAddCancelButton(true);
		dialog.setLabelProvider(createDialogLabelProvider());
		dialog.setMessage(RefactoringMessages.RefactoringStarter_must_save); 
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setInput(Arrays.asList(dirtyEditors));
		return dialog.open() == Window.OK;
	}
	
	public boolean hasFilesSaved() {
		return fFilesSaved;
	}
	
	private ILabelProvider createDialogLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}	
}
