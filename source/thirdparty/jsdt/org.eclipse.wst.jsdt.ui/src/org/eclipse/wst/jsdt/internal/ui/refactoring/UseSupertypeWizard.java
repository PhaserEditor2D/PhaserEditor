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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.UseSuperTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.UseSuperTypeRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class UseSupertypeWizard extends RefactoringWizard{

	/* package */ static final String DIALOG_SETTING_SECTION= "UseSupertypeWizard"; //$NON-NLS-1$
	
	public UseSupertypeWizard(UseSuperTypeRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.UseSupertypeWizard_Use_Super_Type_Where_Possible); 
	}

	protected void addUserInputPages(){
		addPage(new UseSupertypeInputPage());
	}

	private static class UseSupertypeInputPage extends UserInputWizardPage{

		private class UseSupertypeContentProvider implements ITreeContentProvider {

			private ITypeHierarchy fHierarchy;
			
			public Object[] getChildren(Object element) {
				if (element instanceof ITypeHierarchy)
					return getElements(element);
				return getDirectSuperTypes((IType)element).toArray();
			}

			public Set/*<IType>*/ getDirectSuperTypes(IType type){
				Set/*<IType>*/ result= new HashSet();
				final IType superclass= fHierarchy.getSuperclass(type);
				if (superclass != null) {
					result.add(superclass);
				}	
				return result;
			}	

			public Object[] getElements(Object element) {
				if (element instanceof ITypeHierarchy)
					return getChildren(((ITypeHierarchy) element).getType());
				return new Object[0];
			}

			public boolean hasChildren(Object element) {
				return getChildren(element).length > 0;
			}

			public Object getParent(Object element) {
				return null;
			}

			public void dispose() {
				// Do nothing
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof ITypeHierarchy)
					fHierarchy= (ITypeHierarchy) newInput;
				else
					fHierarchy= null;
			}
		}

		private static final String REWRITE_INSTANCEOF= "rewriteInstanceOf";  //$NON-NLS-1$
		public static final String PAGE_NAME= "UseSupertypeInputPage";//$NON-NLS-1$
		private TreeViewer fTreeViewer; 
		private final Map fFileCount;  //IType -> Integer
		private final static String MESSAGE= RefactoringMessages.UseSupertypeInputPage_Select_supertype; 
		private JavaScriptElementLabelProvider fLabelProvider;
		private IDialogSettings fSettings;
		
		public UseSupertypeInputPage() {
			super(PAGE_NAME);
			fFileCount= new HashMap(2);
			setMessage(MESSAGE);
		}

		private void loadSettings() {
			fSettings= getDialogSettings().getSection(UseSupertypeWizard.DIALOG_SETTING_SECTION);
			if (fSettings == null) {
				fSettings= getDialogSettings().addNewSection(UseSupertypeWizard.DIALOG_SETTING_SECTION);
				fSettings.put(REWRITE_INSTANCEOF, false);
			}
			getUseSupertypeProcessor().setInstanceOf(fSettings.getBoolean(REWRITE_INSTANCEOF));
		}

		private UseSuperTypeProcessor getUseSupertypeProcessor() {
			return getUseSupertypeRefactoring().getUseSuperTypeProcessor();
		}

		private UseSuperTypeRefactoring getUseSupertypeRefactoring() {
			return ((UseSuperTypeRefactoring)getRefactoring());
		}	

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			loadSettings();
			Composite composite= new Composite(parent, SWT.NONE);
			setControl(composite);
			composite.setLayout(new GridLayout());

			Label label= new Label(composite, SWT.NONE);
			label.setText(Messages.format(
					RefactoringMessages.UseSupertypeInputPage_Select_supertype_to_use, 
					JavaScriptElementLabels.getElementLabel(getUseSupertypeProcessor().getSubType(), JavaScriptElementLabels.T_FULLY_QUALIFIED)));
			label.setLayoutData(new GridData());
		
			addTreeViewer(composite);

			final Button checkbox= new Button(composite, SWT.CHECK);
			checkbox.setText(RefactoringMessages.UseSupertypeInputPage_Use_in_instanceof); 
			checkbox.setLayoutData(new GridData());
			checkbox.setSelection(getUseSupertypeProcessor().isInstanceOf());
			checkbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getUseSupertypeProcessor().setInstanceOf(checkbox.getSelection());
					fSettings.put(REWRITE_INSTANCEOF, checkbox.getSelection());
					setMessage(MESSAGE);
					setPageComplete(true);
					fFileCount.clear();
					fTreeViewer.refresh();
				}
			});

			Dialog.applyDialogFont(composite);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.USE_SUPERTYPE_WIZARD_PAGE);
		}

		private void addTreeViewer(Composite composite) {
			fTreeViewer= new TreeViewer(composite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			final Tree tree= fTreeViewer.getTree();
			final GridData data= new GridData(GridData.FILL_BOTH);
			data.heightHint= convertHeightInCharsToPixels(12);
			tree.setLayoutData(data);
			fLabelProvider= new UseSupertypeLabelProvider(fFileCount);
			fTreeViewer.setLabelProvider(fLabelProvider);
			fTreeViewer.setContentProvider(new UseSupertypeContentProvider());
			fTreeViewer.setComparator(new ViewerComparator() {
							
				public boolean isSorterProperty(Object element, String property) {
					return true;
				}
			
				public int compare(Viewer viewer, Object first, Object second) {
					final IType type1= (IType)first;
					final IType type2= (IType)second;
					
					return getComparator().compare(type1.getElementName(), type2.getElementName());
				}
			});
			fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection ss= (IStructuredSelection)event.getSelection();
					if (Integer.valueOf(0).equals(fFileCount.get(ss.getFirstElement()))){
						setMessage(RefactoringMessages.UseSupertypeInputPage_No_updates, IMessageProvider.INFORMATION); 
						setPageComplete(false);
					} else {
						setMessage(MESSAGE);
						setPageComplete(true);
					}
					fTreeViewer.refresh();
				}
			});
			try {
				fTreeViewer.setInput(SuperTypeHierarchyCache.getTypeHierarchy(getUseSupertypeProcessor().getSubType()));
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			fTreeViewer.expandAll();
			final TreeItem[] items= tree.getItems();
			if (items.length > 0)
				tree.setSelection(new TreeItem[] {items[0]});
		}

		public IWizardPage getNextPage() {
			initializeRefactoring();
			IWizardPage nextPage= super.getNextPage();
			updateUpdateLabels();
			return nextPage;
		}

		private void updateUpdateLabels() {
			IType selectedType= getSelectedSupertype();
			final int count= getUseSupertypeProcessor().getChanges();
			fFileCount.put(selectedType, Integer.valueOf(count));
			if (count == 0) {
				setMessage(RefactoringMessages.UseSupertypeInputPage_No_updates, IMessageProvider.INFORMATION); 
				setPageComplete(false);
			}
			fTreeViewer.refresh();
			if (noSupertypeCanBeUsed()){
				setMessage(RefactoringMessages.UseSupertypeWizard_10, IMessageProvider.INFORMATION); 
				setPageComplete(false);	
			}
		}

		private boolean noSupertypeCanBeUsed() {
			return fTreeViewer.getTree().getItemCount() == countFilesWithValue(0);
		}

		private int countFilesWithValue(int i) {
			int count= 0;
			for (Iterator iter= fFileCount.keySet().iterator(); iter.hasNext();) {
				if (((Integer)fFileCount.get(iter.next())).intValue() == i)
					count++;
			}
			return count;
		}

		private IType getSelectedSupertype() {
			IStructuredSelection ss= (IStructuredSelection)fTreeViewer.getSelection();
			return (IType)ss.getFirstElement();
		}

		public boolean performFinish(){
			initializeRefactoring();
			boolean superFinish= super.performFinish();
			if (! superFinish)
				return false;
			final int count= getUseSupertypeProcessor().getChanges();
			if (count == 0) {
				updateUpdateLabels();
				return false;
			}
			return superFinish;
		}

		private void initializeRefactoring() {
			IStructuredSelection ss= (IStructuredSelection)fTreeViewer.getSelection();
			getUseSupertypeProcessor().setSuperType((IType)ss.getFirstElement());
		}

		public void dispose() {
			fTreeViewer= null;
			fFileCount.clear();
			fLabelProvider= null;
			super.dispose();
		}
	
		private static class UseSupertypeLabelProvider extends JavaScriptElementLabelProvider{
			private final Map fFileCount;
			private UseSupertypeLabelProvider(Map fileCount){
				fFileCount= fileCount;
			}
			public String getText(Object element) {
				String superText= super.getText(element);
				if  (! fFileCount.containsKey(element))
					return superText;
				int count= ((Integer)fFileCount.get(element)).intValue();
				if (count == 0){
					String[] keys= {superText};
					return Messages.format(RefactoringMessages.UseSupertypeInputPage_no_possible_updates, keys); 
				} else if (count == 1){
					String [] keys= {superText};
					return Messages.format(RefactoringMessages.UseSupertypeInputPage_updates_possible_in_file, keys); 
				}	else {
					String[] keys= {superText, String.valueOf(count)};
					return Messages.format(RefactoringMessages.UseSupertypeInputPage_updates_possible_in_files, keys); 
				}	
			}
		}

		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible && fTreeViewer != null)
				fTreeViewer.getTree().setFocus();
		}
	}
}
