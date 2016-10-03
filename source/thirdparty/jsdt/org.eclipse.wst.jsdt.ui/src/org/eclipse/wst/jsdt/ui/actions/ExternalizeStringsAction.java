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
package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSScanner;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.ListDialog;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.wst.jsdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

/**
 * Externalizes the strings of a compilation unit or find all strings
 * in a package or project that are not externalized yet. Opens a wizard that
 * gathers additional information to externalize the strings.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IJavaScriptUnit</code>, <code>IType</code>, <code>IJavaScriptProject</code>,
 * <code>IPackageFragment</code>, and <code>IPackageFragmentRoot</code>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class ExternalizeStringsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	private NonNLSElement[] fElements;

	/**
	 * Creates a new <code>ExternalizeStringsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ExternalizeStringsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.ExternalizeStringsAction_label); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.EXTERNALIZE_STRINGS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public ExternalizeStringsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(fEditor != null && SelectionConverter.canOperateOn(fEditor));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isExternalizeStringsAvailable(selection));
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);//no UI - happens on selection changes
		}
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		IJavaScriptElement element= SelectionConverter.getInput(fEditor);
		if (!(element instanceof IJavaScriptUnit))
			return;
		run((IJavaScriptUnit)element);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		IJavaScriptUnit unit= getCompilationUnit(selection);
		if (unit != null) {//run on cu 
			run(unit);
		} else {
			//run on multiple
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, createRunnable(selection));
			} catch(InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), 
					ActionMessages.ExternalizeStringsAction_dialog_title, 
					ActionMessages.FindStringsToExternalizeAction_error_message); 
				return;
			} catch(InterruptedException e) {
				// OK
				return;
			}
			showResults();	
		}
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 */
	public void run(final IJavaScriptUnit unit) {
		if (!ActionUtil.isEditable(fEditor, getShell(), unit))
			return;
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					if (unit != null && unit.exists()) {
						NLSRefactoring refactoring= NLSRefactoring.create(unit);
						if (refactoring != null)
							new RefactoringStarter().activate(refactoring, new ExternalizeWizard(refactoring), getShell(), ActionMessages.ExternalizeStringsAction_dialog_title, RefactoringSaveHelper.SAVE_NON_JAVA_UPDATES); 
					}
				} catch(JavaScriptModelException e) {
					ExceptionHandler.handle(e, getShell(), ActionMessages.ExternalizeStringsAction_dialog_title, ActionMessages.ExternalizeStringsAction_dialog_message); 
				}
			}
		});
	}

	private static IJavaScriptUnit getCompilationUnit(IStructuredSelection selection) {
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (first instanceof IJavaScriptUnit) 
			return (IJavaScriptUnit) first;
		if (first instanceof IType)
			return ((IType) first).getJavaScriptUnit();
		return null;
	}
	
	private IRunnableWithProgress createRunnable(final IStructuredSelection selection) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					fElements= doRun(selection, pm);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
	}

	private NonNLSElement[] doRun(IStructuredSelection selection, IProgressMonitor pm) throws CoreException {
		List elements= getSelectedElementList(selection);
		if (elements == null || elements.isEmpty())
			return new NonNLSElement[0];

		pm.beginTask(ActionMessages.FindStringsToExternalizeAction_find_strings, elements.size()); 
					
		try{
			List result= new ArrayList();	
			for (Iterator iter= elements.iterator(); iter.hasNext();) {
				Object obj= iter.next();
				if (obj instanceof IJavaScriptElement) {
					IJavaScriptElement element= (IJavaScriptElement) obj;
					int elementType= element.getElementType();
					
					if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT) {
						result.addAll(analyze((IPackageFragment) element, new SubProgressMonitor(pm, 1)));
					} else if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT) {
						IPackageFragmentRoot root= (IPackageFragmentRoot)element;
						if (!root.isExternal() && !ReorgUtils.isClassFolder(root)) {
							result.addAll(analyze((IPackageFragmentRoot) element, new SubProgressMonitor(pm, 1)));
						} else {
							pm.worked(1);
						}
					} else if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT) {
						result.addAll(analyze((IJavaScriptProject) element, new SubProgressMonitor(pm, 1)));
					} else if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
						IJavaScriptUnit cu= (IJavaScriptUnit)element;
						if (cu.exists()) {
							NonNLSElement nlsElement= analyze(cu);
							if (nlsElement != null) {
								result.add(nlsElement);
							}							
						}
						pm.worked(1);
					} else if (elementType == IJavaScriptElement.TYPE) {
						IType type= (IType)element;
						IJavaScriptUnit cu= type.getJavaScriptUnit();
						if (cu != null && cu.exists()) {
							NonNLSElement nlsElement= analyze(cu);
							if (nlsElement != null) {
								result.add(nlsElement);
							}							
						}
						pm.worked(1);
					} else {
						pm.worked(1);
					}
				} else {
					pm.worked(1);
				}
			}
			return (NonNLSElement[]) result.toArray(new NonNLSElement[result.size()]);
		} finally{
			pm.done();
		}
	}

	private void showResults() {
		if (noStrings())
			MessageDialog.openInformation(getShell(), ActionMessages.ExternalizeStringsAction_dialog_title, ActionMessages.FindStringsToExternalizeAction_noStrings); 
		else
			new NonNLSListDialog(getShell(), fElements, countStrings()).open();
	}

	private boolean noStrings() {
		if (fElements != null) {
			for (int i= 0; i < fElements.length; i++) {
				if (fElements[i].count != 0)
					return false;
			}
		}
		return true;
	}

	/*
	 * returns List of Strings
	 */
	private List analyze(IPackageFragment pack, IProgressMonitor pm) throws CoreException {
		try{
			if (pack == null)
				return new ArrayList(0);
				
			IJavaScriptUnit[] cus= pack.getJavaScriptUnits();
	
			pm.beginTask("", cus.length); //$NON-NLS-1$
			pm.setTaskName(pack.getElementName());
			
			List l= new ArrayList(cus.length);
			for (int i= 0; i < cus.length; i++){
				pm.subTask(cus[i].getElementName());
				NonNLSElement element= analyze(cus[i]);
				if (element != null)
					l.add(element);
				pm.worked(1);
				if (pm.isCanceled())
					throw new OperationCanceledException();
			}	
			return l;					
		} finally {
			pm.done();
		}	
	}

	/*
	 * returns List of Strings
	 */	
	private List analyze(IPackageFragmentRoot sourceFolder, IProgressMonitor pm) throws CoreException {
		try{
			IJavaScriptElement[] children= sourceFolder.getChildren();
			pm.beginTask("", children.length); //$NON-NLS-1$
			pm.setTaskName(sourceFolder.getElementName());
			List result= new ArrayList();
			for (int i= 0; i < children.length; i++) {
				IJavaScriptElement iJavaElement= children[i];
				if (iJavaElement.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT){
					IPackageFragment pack= (IPackageFragment)iJavaElement;
					if (! pack.isReadOnly())
						result.addAll(analyze(pack, new SubProgressMonitor(pm, 1)));
					else
						pm.worked(1);	
				} else	
					pm.worked(1);
			}
			return result;
		} finally{
			pm.done();
		}	
	}
	
	/*
	 * returns List of Strings
	 */
	private List analyze(IJavaScriptProject project, IProgressMonitor pm) throws CoreException {
		try{
			IPackageFragment[] packs= project.getPackageFragments();
			pm.beginTask("", packs.length); //$NON-NLS-1$
			List result= new ArrayList();
			for (int i= 0; i < packs.length; i++) {
				if (! packs[i].isReadOnly())
					result.addAll(analyze(packs[i], new SubProgressMonitor(pm, 1)));
				else 
					pm.worked(1);	
			}
			return result;		
		} finally{
			pm.done();
		}	
	}

	private int countStrings() {
		int found= 0;
		if (fElements != null) {
			for (int i= 0; i < fElements.length; i++)
				found+= fElements[i].count;
		}
		return found;
	} 

	private NonNLSElement analyze(IJavaScriptUnit cu) throws CoreException {
		int count= countNonExternalizedStrings(cu);
		if (count == 0)
			return null;
		else	
			return new NonNLSElement(cu, count);
	}
	
	private int countNonExternalizedStrings(IJavaScriptUnit cu) throws CoreException {
		try{
			NLSLine[] lines= NLSScanner.scan(cu);
			int result= 0;
			for (int i= 0; i < lines.length; i++) {
				result += countNonExternalizedStrings(lines[i]);
			}
			return result;
		} catch (InvalidInputException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR,
				Messages.format(ActionMessages.FindStringsToExternalizeAction_error_cannotBeParsed, cu.getElementName()), 
				e));
		}	
	}

	private int countNonExternalizedStrings(NLSLine line){
		int result= 0;
		NLSElement[] elements= line.getElements();
		for (int i= 0; i < elements.length; i++){
			if (! elements[i].hasTag())
				result++;
		}
		return result;
	}

	/**
	 * returns <code>List</code> of <code>IPackageFragments</code>,  <code>IPackageFragmentRoots</code> or 
	 * <code>IJavaProjects</code> (all entries are of the same kind)
	 */
	private static List getSelectedElementList(IStructuredSelection selection) {
		if (selection == null)
			return null;
			
		return selection.toList();
	}
			
	//-------private classes --------------
		
	private static class NonNLSListDialog extends ListDialog {
		
		private static final int OPEN_BUTTON_ID= IDialogConstants.CLIENT_ID + 1;
		
		private Button fOpenButton;
		
		NonNLSListDialog(Shell parent, NonNLSElement[] input, int count) {
			super(parent);
			setInput(Arrays.asList(input));
			setTitle(ActionMessages.ExternalizeStringsAction_dialog_title);  
			setMessage(Messages.format(ActionMessages.FindStringsToExternalizeAction_non_externalized, new Object[] {Integer.valueOf(count)} )); 
			setContentProvider(new ArrayContentProvider());
			setLabelProvider(createLabelProvider());
		}

		public void create() {
			setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN);
			super.create();
		}

		protected Point getInitialSize() {
			return getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		}

		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			getTableViewer().addSelectionChangedListener(new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event){
					if (fOpenButton != null){
						fOpenButton.setEnabled(! getTableViewer().getSelection().isEmpty());
					}
				}
			});
			getTableViewer().getTable().addSelectionListener(new SelectionAdapter(){
				public void widgetDefaultSelected(SelectionEvent e) {
					NonNLSElement element= (NonNLSElement)e.item.getData();
					openWizard(element.cu);
				}
			});
			getTableViewer().getTable().setFocus();
			applyDialogFont(result);		
			return result;
		}
		
		protected void createButtonsForButtonBar(Composite parent) {
			fOpenButton= createButton(parent, OPEN_BUTTON_ID, ActionMessages.FindStringsToExternalizeAction_button_label, true); 
			fOpenButton.setEnabled(false);
			
			//looks like a 'close' but it a 'cancel'
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		}

		protected void buttonPressed(int buttonId) {
			if (buttonId != OPEN_BUTTON_ID){
				super.buttonPressed(buttonId);
				return;
			}	
			ISelection s= getTableViewer().getSelection();
			if (s instanceof IStructuredSelection){
				IStructuredSelection ss= (IStructuredSelection)s;
				if (ss.getFirstElement() instanceof NonNLSElement)
					openWizard(((NonNLSElement)ss.getFirstElement()).cu);
			}
		}

		private void openWizard(IJavaScriptUnit unit) {
			try {
				if (unit != null && unit.exists()) {
					NLSRefactoring refactoring= NLSRefactoring.create(unit);
					if (refactoring != null)
						new RefactoringStarter().activate(refactoring, new ExternalizeWizard(refactoring), getShell(), ActionMessages.ExternalizeStringsAction_dialog_title, RefactoringSaveHelper.SAVE_NON_JAVA_UPDATES); 
				}
			} catch (JavaScriptModelException e) {
				ExceptionHandler.handle(e, 
					ActionMessages.ExternalizeStringsAction_dialog_title, 
					ActionMessages.FindStringsToExternalizeAction_error_message); 
			}
		}
		
		private static LabelProvider createLabelProvider() {
			return new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT){ 
				public String getText(Object element) {
					NonNLSElement nlsel= (NonNLSElement)element;
					String elementName= nlsel.cu.getResource().getFullPath().toString();
					return Messages.format(
						ActionMessages.FindStringsToExternalizeAction_foundStrings, 
						new Object[] {Integer.valueOf(nlsel.count), elementName} );
				}		
				public Image getImage(Object element) {
					return super.getImage(((NonNLSElement)element).cu);
				}
			};
		}
		
		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.NONNLS_DIALOG);		
		}


	}
		
	private static class NonNLSElement{
		IJavaScriptUnit cu;
		int count;
		NonNLSElement(IJavaScriptUnit cu, int count){
			this.cu= cu;
			this.count= count;
		}
	}	

}
