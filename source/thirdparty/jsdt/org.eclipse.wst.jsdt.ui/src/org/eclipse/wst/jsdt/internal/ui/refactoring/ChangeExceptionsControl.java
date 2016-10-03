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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ExceptionInfo;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;

/**
 * A special control to add and remove thrown exceptions.
 */
public class ChangeExceptionsControl extends Composite {
//TODO: cleanup, adapt NLS strings

	private static class ExceptionInfoContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return removeMarkedAsDeleted((List) inputElement);
		}
		private ExceptionInfo[] removeMarkedAsDeleted(List exceptionInfos){
			List result= new ArrayList(exceptionInfos.size());
			for (Iterator iter= exceptionInfos.iterator(); iter.hasNext();) {
				ExceptionInfo info= (ExceptionInfo) iter.next();
				if (! info.isDeleted())
					result.add(info);
			}
			return (ExceptionInfo[]) result.toArray(new ExceptionInfo[result.size()]);
		}
		public void dispose() {
			// do nothing
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	private static class ExceptionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
		private Image fInterfaceImage;
			
		public ExceptionInfoLabelProvider() {
			super();
			fInterfaceImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS);
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return fInterfaceImage;
		}
		public String getColumnText(Object element, int columnIndex) {
			ExceptionInfo info= (ExceptionInfo) element;
			return info.getType().getFullyQualifiedName();
		}
	}

	private final IExceptionListChangeListener fListener;
	private final IJavaScriptProject fProject;

	private TableViewer fTableViewer;
	private Button fRemoveButton;
	private List fExceptionInfos;

	public ChangeExceptionsControl(Composite parent, int style, IExceptionListChangeListener listener, IJavaScriptProject project) {
		super(parent, style);
		Assert.isNotNull(listener);
		fListener= listener;
		Assert.isNotNull(project);
		fProject= project;
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		setLayout(layout);

		createExceptionList(this);
		createButtonComposite(this);
	}

	public void setInput(List exceptionInfos) {
		Assert.isNotNull(exceptionInfos);
		fExceptionInfos= exceptionInfos;
		fTableViewer.setInput(fExceptionInfos);
		if (fExceptionInfos.size() > 0)
			fTableViewer.setSelection(new StructuredSelection(fExceptionInfos.get(0)));
	}

	private void createExceptionList(Composite parent) {
		final Table table= new Table(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		fTableViewer= new TableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new ExceptionInfoContentProvider());
		fTableViewer.setLabelProvider(new ExceptionInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsEnabledState();
			}
		});
	}

	private ExceptionInfo[] getSelectedItems() {
		ISelection selection= fTableViewer.getSelection();
		if (selection == null)
			return new ExceptionInfo[0];

		if (!(selection instanceof IStructuredSelection))
			return new ExceptionInfo[0];

		List selected= ((IStructuredSelection) selection).toList();
		return (ExceptionInfo[]) selected.toArray(new ExceptionInfo[selected.size()]);
	}

	// ---- Button bar --------------------------------------------------------------------------------------

	private void createButtonComposite(Composite parent) {
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComposite.setLayout(gl);

		createAddButton(buttonComposite);	
		fRemoveButton= createRemoveButton(buttonComposite);
		updateButtonsEnabledState();
	}

	private void updateButtonsEnabledState() {
		if (fRemoveButton != null)
			fRemoveButton.setEnabled(getTableSelectionCount() != 0);
	}

	private int getTableSelectionCount() {
		return getTable().getSelectionCount();
	}

	private int getTableItemCount() {
		return getTable().getItemCount();
	}

	private Table getTable() {
		return fTableViewer.getTable();
	}
	
	private Button createAddButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.ChangeExceptionsControl_buttons_add); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.setEnabled(true);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doAddException();
			}
		});	
		return button;
	}

	private void doAddException() {
		IType newException= chooseException();
		if (newException == null)
			return;
		
		ExceptionInfo info= findExceptionInfo(newException);
		if (info != null) {
			if (info.isDeleted()) {
				info.markAsOld();
				fTableViewer.refresh();
			}
			fListener.exceptionListChanged();
			fTableViewer.getControl().setFocus();
			fTableViewer.setSelection(new StructuredSelection(info), true);
			return;
		}
		
		info= ExceptionInfo.createInfoForAddedException(newException);
		fExceptionInfos.add(info);
		fListener.exceptionListChanged();
		fTableViewer.refresh();
		fTableViewer.getControl().setFocus();
		int row= getTableItemCount() - 1;
		getTable().setSelection(row);
		updateButtonsEnabledState();

	}
	
	private IType chooseException() {
		IJavaScriptElement[] elements= new IJavaScriptElement[] { fProject.getJavaScriptProject() };
		final IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		
		FilteredTypesSelectionDialog dialog= new FilteredTypesSelectionDialog(getShell(), false,
				PlatformUI.getWorkbench().getProgressService(), scope, IJavaScriptSearchConstants.CLASS);
		dialog.setTitle(RefactoringMessages.ChangeExceptionsControl_choose_title); 
		dialog.setMessage(RefactoringMessages.ChangeExceptionsControl_choose_message); 
		dialog.setInitialPattern("*Exception*"); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection.length == 0)
					return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
				try {
					return checkException((IType)selection[0]);
				} catch (JavaScriptModelException e) {
					JavaScriptPlugin.log(e);
					return StatusInfo.OK_STATUS;
				}
			}
		});
		
		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}
	
	private IStatus checkException(final IType type) throws JavaScriptModelException {
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(new NullProgressMonitor());
		IType curr= type;
		while (curr != null) {
			String name= curr.getFullyQualifiedName();
			if ("java.lang.Throwable".equals(name)) //$NON-NLS-1$
				return StatusInfo.OK_STATUS;
			curr= hierarchy.getSuperclass(curr);
		}
		return JavaUIStatus.createError(IStatus.ERROR,
				RefactoringMessages.ChangeExceptionsControl_not_exception, null); 
	}
	
	private ExceptionInfo findExceptionInfo(IType exception) {
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext(); ) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (info.getType().equals(exception))
				return info;
		}
		return null;
	}

	private Button createRemoveButton(Composite buttonComposite) {
		final Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.ChangeExceptionsControl_buttons_remove); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index= getTable().getSelectionIndices()[0];
				ExceptionInfo[] selected= getSelectedItems();
				for (int i= 0; i < selected.length; i++) {
					if (selected[i].isAdded())
						fExceptionInfos.remove(selected[i]);
					else
						selected[i].markAsDeleted();	
				}
				restoreSelection(index);
			}
			private void restoreSelection(int index) {
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				int itemCount= getTableItemCount();
				if (itemCount != 0) {
					if (index >= itemCount)
						index= itemCount - 1;
					getTable().setSelection(index);
				}
				fListener.exceptionListChanged();
				updateButtonsEnabledState();
			}
		});	
		return button;
	}

}
