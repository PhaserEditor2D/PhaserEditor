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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetNewWizard;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.ibm.icu.text.Collator;

public class SimpleWorkingSetSelectionDialog extends SelectionDialog {
	
	private static class WorkingSetLabelProvider extends LabelProvider {
		
		private Map fIcons;
		
		public WorkingSetLabelProvider() {
			fIcons= new Hashtable();
		}
		
		public void dispose() {
			Iterator iterator= fIcons.values().iterator();
			while (iterator.hasNext()) {
				Image icon= (Image)iterator.next();
				icon.dispose();
			}
			super.dispose();
		}
		
		public Image getImage(Object object) {
			Assert.isTrue(object instanceof IWorkingSet);
			IWorkingSet workingSet= (IWorkingSet)object;
			ImageDescriptor imageDescriptor= workingSet.getImageDescriptor();
			if (imageDescriptor == null)
				return null;
			
			Image icon= (Image)fIcons.get(imageDescriptor);
			if (icon == null) {
				icon= imageDescriptor.createImage();
				fIcons.put(imageDescriptor, icon);
			}
			
			return icon;
		}
		
		public String getText(Object object) {
			Assert.isTrue(object instanceof IWorkingSet);
			IWorkingSet workingSet= (IWorkingSet)object;
			return workingSet.getName();
		}
		
	}
	
	private static class Filter extends ViewerFilter {
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return isCompatible((IWorkingSet)element);
		}
				
		protected boolean isCompatible(IWorkingSet set) {
			if (set.isAggregateWorkingSet() || !set.isSelfUpdating())
				return false;
			
			if (!set.isVisible())
				return false;
			
			if (!set.isEditable())
				return false;
			
			return true;
		}
		
	}
	
	private static class JSFilter extends Filter {
		
		private String[] jsWorkingSetIds = null;
		
		public void setJSWorkingSetIds(String[] workingSetIds) {
			jsWorkingSetIds = workingSetIds;
		}
		
		protected boolean isCompatible(IWorkingSet set) {
			if (!super.isCompatible(set))
				return false;
			
			for (int i = 0; jsWorkingSetIds != null && i < jsWorkingSetIds.length; i++) {
				if (jsWorkingSetIds[i].equals(set.getId()))
					return true;
			}
			
			return false;
		}
		
	}
	
	private static final Filter[] ALL_WORKINGSETS = {new Filter()};
	private static final JSFilter[] JS_ONLY_WORKINGSETS = {new JSFilter()};
		
	private final IWorkingSet[] fWorkingSets;
	private final IWorkingSet[] fInitialSelection;
	private final ArrayList fCreatedWorkingSets;
	
	private CheckboxTableViewer fTableViewer;
	private IWorkingSet[] fCheckedElements;
	
	private boolean fShowOnlyJSEnabled;

	private Button fSelectAll;
	private Button fDeselectAll;
	private Button fNewWorkingSet;
	
	private Button fShowOnlyJSWorkingSets;

	public SimpleWorkingSetSelectionDialog(Shell shell, String[] workingSetIds, IWorkingSet[] initialSelection) {
		this(shell, workingSetIds, initialSelection, false);
	}
	
	public SimpleWorkingSetSelectionDialog(Shell shell, String[] workingSetIds, IWorkingSet[] initialSelection, boolean showOnlyJSEnabled) {
		super(shell);
		
		setTitle(WorkingSetMessages.SimpleWorkingSetSelectionDialog_SimpleSelectWorkingSetDialog_title);
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.RESIZE);

		JS_ONLY_WORKINGSETS[0].setJSWorkingSetIds(workingSetIds);
		fWorkingSets= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
		fInitialSelection= initialSelection;
		fCheckedElements= fInitialSelection;
		fCreatedWorkingSets= new ArrayList();
		fShowOnlyJSEnabled = showOnlyJSEnabled;
	}
	
	protected final Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		composite.setFont(parent.getFont());

		createMessageArea(composite);
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		inner.setLayout(layout);
		
		Composite tableComposite= new Composite(inner, SWT.NONE);
		tableComposite.setFont(composite.getFont());
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		tableComposite.setLayout(layout);
		
		fTableViewer= createTableViewer(tableComposite);
		fTableViewer.setFilters(fShowOnlyJSEnabled ? JS_ONLY_WORKINGSETS : ALL_WORKINGSETS); 
		
		fShowOnlyJSWorkingSets= new Button(composite, SWT.CHECK);
		fShowOnlyJSWorkingSets.setText(WorkingSetMessages.SimpleWorkingSetSelectionDialog_show_only_js_working_sets_button);
		fShowOnlyJSWorkingSets.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, false));
		fShowOnlyJSWorkingSets.setSelection(fShowOnlyJSEnabled);
		fShowOnlyJSWorkingSets.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fShowOnlyJSEnabled= fShowOnlyJSWorkingSets.getSelection();
				fTableViewer.setFilters(fShowOnlyJSEnabled ? JS_ONLY_WORKINGSETS : ALL_WORKINGSETS); 
				checkedStateChanged();
			}
		});

		createRightButtonBar(inner);
		
		createBottomButtonBar(composite);
		
		return composite;
	}

	public IWorkingSet[] getSelection() {
		return fCheckedElements;
	}
	
	public boolean isShowOnlyJSWorkingSetsEnabled() {
		return fShowOnlyJSEnabled;
	}
	
	protected CheckboxTableViewer createTableViewer(Composite parent) {
		CheckboxTableViewer result= CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
		result.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkedStateChanged();
			}
		});
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint= convertHeightInCharsToPixels(20);
		data.widthHint= convertWidthInCharsToPixels(50);
		result.getTable().setLayoutData(data);
		result.getTable().setFont(parent.getFont());

		result.addFilter(createTableFilter());
		result.setLabelProvider(createTableLabelProvider());
		result.setSorter(createTableSorter());
		result.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object element) {
				return (Object[])element;
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		
		result.setInput(fWorkingSets);
		result.setCheckedElements(fInitialSelection);
		
		return result;
	}

	protected ViewerSorter createTableSorter() {
		return new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IWorkingSet w1= (IWorkingSet)e1;
				IWorkingSet w2= (IWorkingSet)e2;
				return Collator.getInstance().compare(w1.getLabel(), w2.getLabel());
			}
		};
	}

	protected LabelProvider createTableLabelProvider() {
		return new WorkingSetLabelProvider();
	}

	protected ViewerFilter createTableFilter() {
		return new Filter();
	}
	
	protected void createRightButtonBar(Composite parent) {
		Composite buttons= new Composite(parent, SWT.NONE);
		buttons.setFont(parent.getFont());
		buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);

		createButtonsForRightButtonBar(buttons);
	}

	protected void createButtonsForRightButtonBar(Composite bar) {
		fSelectAll= new Button(bar, SWT.PUSH);
		fSelectAll.setText(WorkingSetMessages.SimpleWorkingSetSelectionDialog_SelectAll_button); 
		fSelectAll.setFont(bar.getFont());
		setButtonLayoutData(fSelectAll);
		fSelectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll();
			}
		});
		
		fDeselectAll= new Button(bar, SWT.PUSH);
		fDeselectAll.setText(WorkingSetMessages.SimpleWorkingSetSelectionDialog_DeselectAll_button); 
		fDeselectAll.setFont(bar.getFont());
		setButtonLayoutData(fDeselectAll);
		fDeselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deselectAll();
			}
		});
		
		new Label(bar, SWT.NONE);
		
		fNewWorkingSet= new Button(bar, SWT.PUSH);
		fNewWorkingSet.setText(WorkingSetMessages.SimpleWorkingSetSelectionDialog_New_button); 
		fNewWorkingSet.setFont(bar.getFont());
		setButtonLayoutData(fNewWorkingSet);
		fNewWorkingSet.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IWorkingSet workingSet= newWorkingSet();
				if (workingSet != null) {
					
				}
			}
		});
	}
	
	protected void createBottomButtonBar(Composite parent) {
	}
	
	protected void checkedStateChanged() {
		List elements= Arrays.asList(fTableViewer.getCheckedElements());
		fCheckedElements= (IWorkingSet[])elements.toArray(new IWorkingSet[elements.size()]);
	}
	
	protected void selectAll() {
		fTableViewer.setAllChecked(true);
		checkedStateChanged();
	}
	
	protected void deselectAll() {
		fTableViewer.setAllChecked(false);
		checkedStateChanged();
	}
	
	protected IWorkingSet newWorkingSet() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		
		//can only allow to create java working sets at the moment, see bug 186762
//		IWorkingSetNewWizard wizard= manager.createWorkingSetNewWizard(fWorkingSetIds);
//		if (wizard == null)
//			return;
		
		IWorkingSetNewWizard wizard= manager.createWorkingSetNewWizard(new String[] {JavaWorkingSetUpdater.ID});
		
		WizardDialog dialog= new WizardDialog(getShell(), wizard);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IWorkingSet workingSet= wizard.getSelection();
			Filter filter= new Filter();
			if (filter.select(null, null, workingSet)) {
				addNewWorkingSet(workingSet);
				checkedStateChanged();
				manager.addWorkingSet(workingSet);
				fCreatedWorkingSets.add(workingSet);
				return workingSet;
			}
		}
		
		return null;
	}

	protected void addNewWorkingSet(IWorkingSet workingSet) {
		fTableViewer.add(workingSet);
		fTableViewer.setSelection(new StructuredSelection(workingSet), true);
		fTableViewer.setChecked(workingSet, true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void cancelPressed() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		for (int i= 0; i < fCreatedWorkingSets.size(); i++) {
			manager.removeWorkingSet((IWorkingSet)fCreatedWorkingSets.get(i));
		}
		
		super.cancelPressed();
	}
}