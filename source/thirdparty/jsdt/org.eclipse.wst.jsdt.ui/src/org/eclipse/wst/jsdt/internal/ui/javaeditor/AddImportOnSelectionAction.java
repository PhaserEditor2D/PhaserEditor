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

package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddImportsOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddImportsOperation.IChooseImportQuery;
import org.eclipse.wst.jsdt.internal.corext.util.History;
import org.eclipse.wst.jsdt.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.TypeNameMatchLabelProvider;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;

import com.ibm.icu.text.Collator;


public class AddImportOnSelectionAction extends Action implements IUpdate {
	
	private static final AddImportComparator ADD_IMPORT_COMPARATOR= new AddImportComparator();
	
	private static final class AddImportComparator implements Comparator {
		
		public int compare(Object o1, Object o2) {
			if (((String)o1).equals(o2))
				return 0;
			
			History history= QualifiedTypeNameHistory.getDefault();
			
			int pos1= history.getPosition(o1);
			int pos2= history.getPosition(o2);
			
			if (pos1 == pos2)
				return Collator.getInstance().compare(o1, o2);
			
			if (pos1 > pos2) {
				return -1;
			} else {
				return 1;
			}
		}
		
	}

	private CompilationUnitEditor fEditor;

	public AddImportOnSelectionAction(CompilationUnitEditor editor) {
		super(JavaEditorMessages.AddImportOnSelection_label);
		setToolTipText(JavaEditorMessages.AddImportOnSelection_tooltip);
		setDescription(JavaEditorMessages.AddImportOnSelection_description);
		fEditor= editor;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_IMPORT_ON_SELECTION_ACTION);
		setEnabled(getCompilationUnit() != null);
	}

	public void update() {
		setEnabled(fEditor != null && getCompilationUnit() != null);
	}

	private IJavaScriptUnit getCompilationUnit () {
		if (fEditor == null) {
			return null;
		}
		IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
		return manager.getWorkingCopy(fEditor.getEditorInput());
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		final IJavaScriptUnit cu= getCompilationUnit();
		if (cu == null || fEditor == null)
			return;
		if (!ElementValidator.checkValidateEdit(cu, getShell(), JavaEditorMessages.AddImportOnSelection_error_title))
			return;
		if (!ActionUtil.isEditable(fEditor))
			return;

		ISelection selection= fEditor.getSelectionProvider().getSelection();
		if (selection instanceof ITextSelection) {
			final ITextSelection textSelection= (ITextSelection) selection;
			AddImportOnSelectionAction.SelectTypeQuery query= new SelectTypeQuery(getShell());
			AddImportsOperation op= new AddImportsOperation(cu, textSelection.getOffset(), textSelection.getLength(), query, false);
			IEditingSupport helper= createViewerHelper(textSelection, query);
			try {
				registerHelper(helper);
				IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
				progressService.runInUI(fEditor.getSite().getWorkbenchWindow(), new WorkbenchRunnableAdapter(op, op.getScheduleRule()), op.getScheduleRule());
				IStatus status= op.getStatus();
				if (!status.isOK()) {
					IStatusLineManager manager= getStatusLineManager();
					if (manager != null) {
						manager.setMessage(status.getMessage());
					}
				}
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), JavaEditorMessages.AddImportOnSelection_error_title, null);
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled.
			} finally {
				deregisterHelper(helper);
			}
		}
	}

	private IEditingSupport createViewerHelper(final ITextSelection selection, final SelectTypeQuery query) {
		return new IEditingSupport() {

			public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
				return subjectRegion.getOffset() <= selection.getOffset() + selection.getLength() &&  selection.getOffset() <= subjectRegion.getOffset() + subjectRegion.getLength();
			}

			public boolean ownsFocusShell() {
				return query.isShowing();
			}

		};
	}

	private void registerHelper(IEditingSupport helper) {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.register(helper);
		}
	}

	private void deregisterHelper(IEditingSupport helper) {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.unregister(helper);
		}
	}

	private Shell getShell() {
		return fEditor.getSite().getShell();
	}

	private static class SelectTypeQuery implements IChooseImportQuery {

		private final Shell fShell;
		private boolean fIsShowing;

		public SelectTypeQuery(Shell shell) {
			fShell= shell;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddImportsOperation.IChooseImportQuery#chooseImport(org.eclipse.wst.jsdt.internal.corext.util.TypeInfo[], java.lang.String)
		 */
		public TypeNameMatch chooseImport(TypeNameMatch[] results, String containerName) {
			int nResults= results.length;

			if (nResults == 0) {
				return null;
			} else if (nResults == 1) {
				return results[0];
			}

			if (containerName.length() != 0) {
				for (int i= 0; i < nResults; i++) {
					TypeNameMatch curr= results[i];
					if (containerName.equals(curr.getTypeContainerName())) {
						return curr;
					}
				}
			}
			fIsShowing= true;
			ElementListSelectionDialog dialog= new ElementListSelectionDialog(fShell, new TypeNameMatchLabelProvider(TypeNameMatchLabelProvider.SHOW_FULLYQUALIFIED)) {
				protected FilteredList createFilteredList(Composite parent) {
					FilteredList filteredList= super.createFilteredList(parent);
					filteredList.setComparator(ADD_IMPORT_COMPARATOR);
					return filteredList;
				}
			};
			dialog.setTitle(JavaEditorMessages.AddImportOnSelection_dialog_title);
			dialog.setMessage(JavaEditorMessages.AddImportOnSelection_dialog_message);
			dialog.setElements(results);
			if (dialog.open() == Window.OK) {
				fIsShowing= false;
				TypeNameMatch result= (TypeNameMatch) dialog.getFirstResult();
				QualifiedTypeNameHistory.remember(result.getFullyQualifiedName());
				return result;
			}
			fIsShowing= false;
			return null;
		}

		boolean isShowing() {
			return fIsShowing;
		}
	}

	private IStatusLineManager getStatusLineManager() {
		return fEditor.getEditorSite().getActionBars().getStatusLineManager();
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}
