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
 package org.eclipse.wst.jsdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.core.search.TypeNameRequestor;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.OpenTypeHistory;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.dialogs.TypeSelectionExtension;

/**
 * @deprecated use {@link FilteredTypesSelectionDialog}
 */
public class TypeSelectionDialog2 extends SelectionStatusDialog {

	private String fTitle;
	
	private boolean fMultipleSelection;
	private IRunnableContext fRunnableContext;
	private IJavaScriptSearchScope fScope;
	private int fElementKind;
	
	private String fInitialFilter;
	private int fSelectionMode;
	private ISelectionStatusValidator fValidator;
	private TypeSelectionComponent fContent;
	private TypeSelectionExtension fExtension;
	
	public static final int NONE= TypeSelectionComponent.NONE;
	public static final int CARET_BEGINNING= TypeSelectionComponent.CARET_BEGINNING;
	public static final int FULL_SELECTION= TypeSelectionComponent.FULL_SELECTION;
	
	private static boolean fgFirstTime= true; 
	
	private class TitleLabel implements TypeSelectionComponent.ITitleLabel {
		public void setText(String text) {
			if (text == null || text.length() == 0) {
				getShell().setText(fTitle);
			} else {
				getShell().setText(Messages.format(
					JavaUIMessages.TypeSelectionDialog2_title_format,
					new String[] { fTitle, text}));
			}
		}
	}
	
	public TypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, 
			IJavaScriptSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}
	
	public TypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, 
			IJavaScriptSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fMultipleSelection= multi;
		fRunnableContext= context;
		fScope= scope;
		fElementKind= elementKinds;
		fSelectionMode= NONE;
		fExtension= extension;
		if (fExtension != null) {
			fValidator= fExtension.getSelectionValidator();
		}
	}
	
	public void setFilter(String filter) {
		setFilter(filter, FULL_SELECTION);
	}
	
	public void setFilter(String filter, int selectionMode) {
		fInitialFilter= filter;
		fSelectionMode= selectionMode;
	}
	
	public void setValidator(ISelectionStatusValidator validator) {
		fValidator= validator;
	}
	
	protected TypeNameMatch[] getSelectedTypes() {
		if (fContent == null || fContent.isDisposed())
			return null;
		return fContent.getSelection();
	}
	
	public void create() {
		super.create();
		fContent.populate(fSelectionMode);
		getOkButton().setEnabled(fContent.getSelection().length > 0);
	}
	
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.TYPE_SELECTION_DIALOG2);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite area= (Composite)super.createDialogArea(parent);
		fContent= new TypeSelectionComponent(area, SWT.NONE, getMessage(), 
			fMultipleSelection, fScope, fElementKind, fInitialFilter,
			new TitleLabel(), fExtension);
		GridData gd= new GridData(GridData.FILL_BOTH);
		fContent.setLayoutData(gd);
		fContent.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDefaultSelected(fContent.getSelection());
			}
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected(fContent.getSelection());
			}
		});
		return area;
	}
	
	protected void handleDefaultSelected(TypeNameMatch[] selection) {
		if (selection.length == 0)
			return;
		okPressed();
	}
	
	protected void handleWidgetSelected(TypeNameMatch[] selection) {
		IStatus status= null;
		if (selection.length == 0) {
	    	status= new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, "",null); //$NON-NLS-1$
	    } else {
		    if (fValidator != null) {
				List jElements= new ArrayList();
				for (int i= 0; i < selection.length; i++) {
					IType type= selection[i].getType();
					if (type != null) {
						jElements.add(type);
					} else {
			    		status= new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR,
			    			Messages.format(JavaUIMessages.TypeSelectionDialog_error_type_doesnot_exist, selection[i].getFullyQualifiedName()),
			    			null);
			    		break;
					}
				}
				if (status == null) {
					status= fValidator.validate(jElements.toArray());
				}
			} else {
				status= new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), IStatus.OK, "",null); //$NON-NLS-1$
			}
	    }
    	updateStatus(status);
	}
	
	public int open() {
		try {
			ensureConsistency();
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, JavaUIMessages.TypeSelectionDialog_error3Title, JavaUIMessages.TypeSelectionDialog_error3Message); 
			return CANCEL;
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}
		if (fInitialFilter == null) {
			IWorkbenchWindow window= JavaScriptPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				ISelection selection= window.getSelectionService().getSelection();
				if (selection instanceof ITextSelection) {
					String text= ((ITextSelection)selection).getText();
					if (text != null) {
						text= text.trim();
						if (text.length() > 0 && JavaScriptConventions.validateJavaScriptTypeName(text, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3).isOK()) {
							fInitialFilter= text;
							fSelectionMode= FULL_SELECTION;
						}
					}
				}
			}
		}
		return super.open();
	}
	
	public boolean close() {
		boolean result;
		try {
			if (getReturnCode() == OK) {
				OpenTypeHistory.getInstance().save();
			}
		} finally {
			result= super.close();
		}
		return result;
	}
	
	public void setTitle(String title) {
		super.setTitle(title);
		fTitle= title;
	}
	
	protected void computeResult() {
		TypeNameMatch[] selected= fContent.getSelection();
		if (selected == null || selected.length == 0) {
			setResult(null);
			return;
		}
		
		// If the scope is null then it got computed by the type selection component.
		if (fScope == null) {
			fScope= fContent.getScope();
		}
		
		OpenTypeHistory history= OpenTypeHistory.getInstance();
		List result= new ArrayList(selected.length);
		for (int i= 0; i < selected.length; i++) {
			TypeNameMatch typeInfo= selected[i];
			IType type= typeInfo.getType();
			if (!type.exists()) {
				String title= JavaUIMessages.TypeSelectionDialog_errorTitle; 
				IPackageFragmentRoot root= typeInfo.getPackageFragmentRoot();
				String containerName= JavaScriptElementLabels.getElementLabel(root, JavaScriptElementLabels.ROOT_QUALIFIED);
				String message= Messages.format(JavaUIMessages.TypeSelectionDialog_dialogMessage, new String[] { typeInfo.getFullyQualifiedName(), containerName }); 
				MessageDialog.openError(getShell(), title, message);
				history.remove(typeInfo);
				setResult(null);
			} else {
				history.accessed(typeInfo);
				result.add(type);
			}
		}
		setResult(result);
	}
	
	private void ensureConsistency() throws InvocationTargetException, InterruptedException {
		// we only have to ensure history consistency here since the search engine
		// takes care of working copies.
		class ConsistencyRunnable implements IRunnableWithProgress {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (fgFirstTime) {
					// Join the initialize after load job.
					IJobManager manager= Job.getJobManager();
					manager.join(JavaScriptUI.ID_PLUGIN, monitor);
				}
				OpenTypeHistory history= OpenTypeHistory.getInstance();
				if (fgFirstTime || history.isEmpty()) {
					monitor.beginTask(JavaUIMessages.TypeSelectionDialog_progress_consistency, 100);
					if (history.needConsistencyCheck()) {
						refreshSearchIndices(new SubProgressMonitor(monitor, 90));
						history.checkConsistency(new SubProgressMonitor(monitor, 10));
					} else {
						refreshSearchIndices(monitor);
					}
					monitor.done();
					fgFirstTime= false;
				} else {
					history.checkConsistency(monitor);
				}
			}
			public boolean needsExecution() {
				OpenTypeHistory history= OpenTypeHistory.getInstance();
				return fgFirstTime || history.isEmpty() || history.needConsistencyCheck(); 
			}
			private void refreshSearchIndices(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					new SearchEngine().searchAllTypeNames(
						null,
						0,
						// make sure we search a concrete name. This is faster according to Kent  
						"_______________".toCharArray(), //$NON-NLS-1$
						SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, 
						IJavaScriptSearchConstants.ENUM,
						SearchEngine.createWorkspaceScope(), 
						new TypeNameRequestor() {}, 
						IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
						monitor);
				} catch (JavaScriptModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		}
		ConsistencyRunnable runnable= new ConsistencyRunnable();
		if (!runnable.needsExecution())
			return;
		IRunnableContext context= fRunnableContext != null 
			? fRunnableContext 
			: PlatformUI.getWorkbench().getProgressService();
		context.run(true, true, runnable);
	}
}
