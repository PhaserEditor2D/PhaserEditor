/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ViewerPane;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;

public class OverrideMethodDialog extends SourceActionDialog {

	private static final boolean SHOW_ONLY_SUPER = true;
	
	
	private class OverrideFlatTreeAction extends Action {

		private boolean fToggle;
		
		
		public OverrideFlatTreeAction() {
			setToolTipText(JavaUIMessages.OverrideMethodDialog_groupMethodsByTypes); 

			JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

			fToggle= getOverrideContentProvider().isShowTypes();
			setChecked(fToggle);
		}

		private OverrideMethodContentProvider getOverrideContentProvider() {
			return (OverrideMethodContentProvider) getContentProvider();
		}

		public void run() {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=39264
			Object[] elementList= getOverrideContentProvider().getViewer().getCheckedElements();
			fToggle= !fToggle;
			setChecked(fToggle);
			getOverrideContentProvider().setShowTypes(fToggle);
			getOverrideContentProvider().getViewer().setCheckedElements(elementList);
		}

	}

	private static class OverrideMethodContentProvider implements ITreeContentProvider {

		private final Object[] fEmpty= new Object[0];

		private IFunction[] fMethods;

		private IDialogSettings fSettings;

		private boolean fShowTypes;

	//	private Object[] fTypes;

		private ContainerCheckedTreeViewer fViewer;

		private final String SETTINGS_SECTION= "OverrideMethodDialog"; //$NON-NLS-1$

		private final String SETTINGS_SHOWTYPES= "showtypes"; //$NON-NLS-1$

		/**
		 * Constructor for OverrideMethodContentProvider.
		 */
		public OverrideMethodContentProvider() {
			IDialogSettings dialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
			fSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fSettings == null) {
				fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fSettings.put(SETTINGS_SHOWTYPES, true);
			}
			fShowTypes= fSettings.getBoolean(SETTINGS_SHOWTYPES);
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IJavaScriptUnit) {
				ArrayList result= new ArrayList(fMethods.length);
				for (int index= 0; index < fMethods.length; index++) {
					if (fMethods[index].getJavaScriptUnit() == parentElement)
						result.add(fMethods[index]);
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return  fMethods;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IFunction) {
				return ((IFunction) element).getJavaScriptUnit();
			}
			return null;
		}

		public ContainerCheckedTreeViewer getViewer() {
			return fViewer;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public void init(IFunction[] methods) {
			fMethods= methods;
			//fTypes= types;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fViewer= (ContainerCheckedTreeViewer) viewer;
		}

		public boolean isShowTypes() {
			return fShowTypes;
		}

		public void setShowTypes(boolean showTypes) {
			if (fShowTypes != showTypes) {
				fShowTypes= showTypes;
				fSettings.put(SETTINGS_SHOWTYPES, showTypes);
				if (fViewer != null)
					fViewer.refresh();
			}
		}
	}

	private static class OverrideMethodComparator extends ViewerComparator {
		public OverrideMethodComparator() {
		}

		/*
		 * @see ViewerSorter#compare(Viewer, Object, Object)
		 */
		public int compare(Viewer viewer, Object first, Object second) {
			if (first instanceof ITypeBinding && second instanceof ITypeBinding) {
				final ITypeBinding left= (ITypeBinding) first;
				final ITypeBinding right= (ITypeBinding) second;
				if (right.getQualifiedName().equals("java.lang.Object")) //$NON-NLS-1$
					return -1;
				if (left.isEqualTo(right))
					return 0;
				if (Bindings.isSuperType(left, right))
					return +1;
				else if (Bindings.isSuperType(right, left))
					return -1;
				return 0;
			} else
				return super.compare(viewer, first, second);
		}
	}

	private static class OverrideMethodValidator implements ISelectionStatusValidator {

		private static int fNumMethods;

		public OverrideMethodValidator(int entries) {
			fNumMethods= entries;
		}

		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (int index= 0; index < selection.length; index++) {
				if (selection[index] instanceof IFunction)
					count++;
			}
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			return new StatusInfo(IStatus.INFO, Messages.format(JavaUIMessages.OverrideMethodDialog_selectioninfo_more, new String[] { String.valueOf(count), String.valueOf(fNumMethods)})); 
		}
	}

	private JavaScriptUnit fUnit= null;

	public OverrideMethodDialog(Shell shell, CompilationUnitEditor editor, IType type, boolean isSubType) throws JavaScriptModelException {
		super(shell, new BindingLabelProvider(), new OverrideMethodContentProvider(), editor, type, false);

//		IMethod[] methods = type.getMethods();
		String parentName = type.getSuperclassName();
		IType superType =  type.getJavaScriptProject().findType(parentName);
		IFunction pMethods[] = superType.getFunctions();
		IFunction tMethods[] = type.getFunctions();
		
		IFunction parentMethods[] = new IFunction[0];
		
		if(OverrideMethodDialog.SHOW_ONLY_SUPER){
			
			ArrayList show = new ArrayList();
			start:		for(int i = 0;pMethods!=null && i<pMethods.length;i++){
							for(int k=0;k<tMethods.length;k++){
								if(tMethods[k].getElementName().equals(pMethods[i].getElementName())){
									continue start;
								}
							}
							show.add(pMethods[i]);
				
							
						}
			parentMethods = (IFunction[]) show.toArray(new IFunction[show.size()]);
			
		}
		
		
		
		//IMethodBinding[] toImplementArray= (IMethodBinding[]) toImplement.toArray(new IMethodBinding[toImplement.size()]);
		setInitialSelections(parentMethods);

		HashSet expanded= new HashSet(parentMethods.length);
		for (int i= 0; i < parentMethods.length; i++) {
			expanded.add(parentMethods[i]);
		}

		HashSet types= new HashSet(parentMethods.length);
		for (int i= 0; i < parentMethods.length; i++) {
			types.add(parentMethods[i]);
		}

		IFunction[] typesArrays= (IFunction[]) types.toArray(new IFunction[types.size()]);
		ViewerComparator comparator= new OverrideMethodComparator();
		if (expanded.isEmpty() && typesArrays.length > 0) {
			comparator.sort(null, typesArrays);
			expanded.add(typesArrays[0]);
		}
		setExpandedElements(expanded.toArray());

		((OverrideMethodContentProvider) getContentProvider()).init(parentMethods);

		setTitle(JavaUIMessages.OverrideMethodDialog_dialog_title);
		setMessage(null);
		setValidator(new OverrideMethodValidator(parentMethods.length));
		setComparator(comparator);
		setContainerMode(true);
		setSize(60, 18);
		setInput(new Object());
	}

	public JavaScriptUnit getCompilationUnit() {
		return fUnit;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OVERRIDE_TREE_SELECTION_DIALOG);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createLinkControl(Composite composite) {
		Link link= new Link(composite, SWT.WRAP);
		link.setText(JavaUIMessages.OverrideMethodDialog_link_message); 
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openCodeTempatePage(CodeTemplateContextType.OVERRIDECOMMENT_ID);
			}
		});
		link.setToolTipText(JavaUIMessages.OverrideMethodDialog_link_tooltip); 
		
		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= convertWidthInCharsToPixels(40); // only expand further if anyone else requires it
		link.setLayoutData(gridData);
		return link;
	}

	/*
	 * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
	 */
	protected CheckboxTreeViewer createTreeViewer(Composite composite) {
		initializeDialogUnits(composite);
		ViewerPane pane= new ViewerPane(composite, SWT.BORDER | SWT.FLAT);
		pane.setText(JavaUIMessages.OverrideMethodDialog_dialog_description); 
		CheckboxTreeViewer treeViewer= super.createTreeViewer(pane);
		pane.setContent(treeViewer.getControl());
		GridLayout paneLayout= new GridLayout();
		paneLayout.marginHeight= 0;
		paneLayout.marginWidth= 0;
		paneLayout.numColumns= 1;
		pane.setLayout(paneLayout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(55);
		gd.heightHint= convertHeightInCharsToPixels(15);
		pane.setLayoutData(gd);
		ToolBarManager manager= pane.getToolBarManager();
		manager.add(new OverrideFlatTreeAction()); // create after tree is created
		manager.update(true);
		treeViewer.getTree().setFocus();
		return treeViewer;
	}

	public boolean hasMethodsToOverride() {
		return getContentProvider().getElements(null).length > 0;
	}

}
