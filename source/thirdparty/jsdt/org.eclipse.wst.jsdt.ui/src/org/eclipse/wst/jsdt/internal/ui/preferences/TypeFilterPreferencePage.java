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
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/*
 * The page for setting the type filters
 */
public class TypeFilterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String TYPE_FILTER_PREF_PAGE_ID= "org.eclipse.wst.jsdt.ui.preferences.TypeFilterPreferencePage"; //$NON-NLS-1$
	
	private static final String PREF_FILTER_ENABLED= PreferenceConstants.TYPEFILTER_ENABLED;
	private static final String PREF_FILTER_DISABLED= PreferenceConstants.TYPEFILTER_DISABLED;
	
	private static String[] unpackOrderList(String str) {
		StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
	
	private static String packOrderList(List orderList) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < orderList.size(); i++) {
			buf.append((String) orderList.get(i));
			buf.append(';');
		}
		return buf.toString();
	}	
		
	private class TypeFilterAdapter implements IListAdapter, IDialogFieldListener {

		private boolean canEdit(ListDialogField field) {
			return field.getSelectedElements().size() == 1;
		}

        public void customButtonPressed(ListDialogField field, int index) {
        	doButtonPressed(index);
        }

        public void selectionChanged(ListDialogField field) {
			fFilterListField.enableButton(IDX_EDIT, canEdit(field));
        }

        public void dialogFieldChanged(DialogField field) {
        }
        
        public void doubleClicked(ListDialogField field) {
        	if (canEdit(field)) {
				doButtonPressed(IDX_EDIT);
        	}
        }
	}
	
	private static final int IDX_ADD= 0;
	private static final int IDX_ADD_PACKAGE= 1;
	private static final int IDX_EDIT= 2;
	private static final int IDX_REMOVE= 3;
	private static final int IDX_SELECT= 5;
	private static final int IDX_DESELECT= 6;

	private CheckedListDialogField fFilterListField;
	
	public TypeFilterPreferencePage() {
		super();
		setPreferenceStore(JavaScriptPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.TypeFilterPreferencePage_description); 
	
		String[] buttonLabels= new String[] { 
			PreferencesMessages.TypeFilterPreferencePage_add_button, 
			PreferencesMessages.TypeFilterPreferencePage_addpackage_button, 
			PreferencesMessages.TypeFilterPreferencePage_edit_button, 
			PreferencesMessages.TypeFilterPreferencePage_remove_button, 
			/* 4 */  null,
			PreferencesMessages.TypeFilterPreferencePage_selectall_button, 
			PreferencesMessages.TypeFilterPreferencePage_deselectall_button, 
		};
				
		TypeFilterAdapter adapter= new TypeFilterAdapter();
		
		fFilterListField= new CheckedListDialogField(adapter, buttonLabels, new LabelProvider());
		fFilterListField.setDialogFieldListener(adapter);
		fFilterListField.setLabelText(PreferencesMessages.TypeFilterPreferencePage_list_label); 
		fFilterListField.setCheckAllButtonIndex(IDX_SELECT);
		fFilterListField.setUncheckAllButtonIndex(IDX_DESELECT);
		fFilterListField.setRemoveButtonIndex(IDX_REMOVE);
		
		fFilterListField.enableButton(IDX_EDIT, false);
		
		initialize(false);
	}
	
	/*
	 * @see PreferencePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.TYPE_FILTER_PREFERENCE_PAGE);
	}
	
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
	
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		
		composite.setLayout(layout);
		
		fFilterListField.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fFilterListField.getLabelControl(null), 2);
		LayoutUtil.setWidthHint(fFilterListField.getLabelControl(null), convertWidthInCharsToPixels(40));
		LayoutUtil.setHorizontalGrabbing(fFilterListField.getListControl(null));
		
		fFilterListField.getTableViewer().setComparator(new ViewerComparator());
		
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	private void initialize(boolean fromDefault) {
		IPreferenceStore store= getPreferenceStore();
		
		String enabled= fromDefault ? store.getDefaultString(PREF_FILTER_ENABLED) : store.getString(PREF_FILTER_ENABLED);
		String disabled= fromDefault ? store.getDefaultString(PREF_FILTER_DISABLED) : store.getString(PREF_FILTER_DISABLED);
		
		ArrayList res= new ArrayList();
		
		String[] enabledEntries= unpackOrderList(enabled);
		for (int i= 0; i < enabledEntries.length; i++) {
			res.add(enabledEntries[i]);
		}
		String[] disabledEntries= unpackOrderList(disabled);
		for (int i= 0; i < disabledEntries.length; i++) {
			res.add(disabledEntries[i]);
		}
		
		fFilterListField.setElements(res);
		fFilterListField.setCheckedElements(Arrays.asList(enabledEntries));
	}
	
	
	private void doButtonPressed(int index) {
		if (index == IDX_ADD) { // add new
			List existing= fFilterListField.getElements();
			TypeFilterInputDialog dialog= new TypeFilterInputDialog(getShell(), existing);
			if (dialog.open() == Window.OK) {
				Object res= dialog.getResult();
				fFilterListField.addElement(res);
				fFilterListField.setChecked(res, true);
			}
		} else if (index == IDX_ADD_PACKAGE) { // add packages
			String[] res= choosePackage();
			if (res != null) {
				fFilterListField.addElements(Arrays.asList(res));
				for (int i= 0; i < res.length; i++) {
					fFilterListField.setChecked(res[i], true);
				}
			}
			
		} else if (index == IDX_EDIT) { // edit
			List selected= fFilterListField.getSelectedElements();
			if (selected.isEmpty()) {
				return;
			}
			String editedEntry= (String) selected.get(0);
			
			List existing= fFilterListField.getElements();
			existing.remove(editedEntry);
			
			TypeFilterInputDialog dialog= new TypeFilterInputDialog(getShell(), existing);
			dialog.setInitialString(editedEntry);
			if (dialog.open() == Window.OK) {
				fFilterListField.replaceElement(editedEntry, dialog.getResult());
			}
		}	
	}
	
	private String[] choosePackage() {
		IJavaScriptSearchScope scope= SearchEngine.createWorkspaceScope();
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		int flags= PackageSelectionDialog.F_SHOW_PARENTS | PackageSelectionDialog.F_HIDE_DEFAULT_PACKAGE | PackageSelectionDialog.F_REMOVE_DUPLICATES;
		PackageSelectionDialog dialog = new PackageSelectionDialog(getShell(), context, flags , scope);
		dialog.setTitle(PreferencesMessages.TypeFilterPreferencePage_choosepackage_label); 
		dialog.setMessage(PreferencesMessages.TypeFilterPreferencePage_choosepackage_description); 
		dialog.setMultipleSelection(true);
		if (dialog.open() == IDialogConstants.OK_ID) {
			Object[] fragments= dialog.getResult();
			String[] res= new String[fragments.length];
			for (int i= 0; i < res.length; i++) {
				res[i]= ((IPackageFragment) fragments[i]).getElementName() + ".*"; //$NON-NLS-1$
			}
			return res;
		}
		return null;
	}
	
	
	public void init(IWorkbench workbench) {
	}
		
    /*
     * @see PreferencePage#performDefaults()
     */
    protected void performDefaults() {
    	initialize(true);
		
		super.performDefaults();	
    }


    /*
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
  		IPreferenceStore prefs= JavaScriptPlugin.getDefault().getPreferenceStore();
  		
  		List checked= fFilterListField.getCheckedElements();
  		List unchecked= fFilterListField.getElements();
  		unchecked.removeAll(checked);
  		
  		prefs.setValue(PREF_FILTER_ENABLED, packOrderList(checked));
  		prefs.setValue(PREF_FILTER_DISABLED, packOrderList(unchecked));
  		JavaScriptPlugin.getDefault().savePluginPreferences();
        return true;
    }


}


