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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/*
 * The page for setting the organize import settings
 */
public class ImportOrganizeConfigurationBlock extends OptionsConfigurationBlock {

	private static final Key PREF_IMPORTORDER= getJDTUIKey(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
	private static final Key PREF_ONDEMANDTHRESHOLD= getJDTUIKey(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
	private static final Key PREF_IGNORELOWERCASE= getJDTUIKey(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE);
	private static final Key PREF_STATICONDEMANDTHRESHOLD= getJDTUIKey(PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD);
	
	private static final String DIALOGSETTING_LASTLOADPATH= JavaScriptUI.ID_PLUGIN + ".importorder.loadpath"; //$NON-NLS-1$
	private static final String DIALOGSETTING_LASTSAVEPATH= JavaScriptUI.ID_PLUGIN + ".importorder.savepath"; //$NON-NLS-1$

	private static Key[] getAllKeys() {
		return new Key[] {
			PREF_IMPORTORDER, PREF_ONDEMANDTHRESHOLD, PREF_STATICONDEMANDTHRESHOLD, PREF_IGNORELOWERCASE
		};	
	}
	
	public static class ImportOrderEntry {
		
		public final String name;
		public final boolean isStatic;
		
		public ImportOrderEntry(String name, boolean isStatic) {
			this.name= name;
			this.isStatic= isStatic;
		}
		
		public String serialize() {
			return isStatic ? '#' + name : name;
		}
		
		public static ImportOrderEntry fromSerialized(String str) {
			if (str.length() > 0 && str.charAt(0) == '#') {
				return new ImportOrderEntry(str.substring(1), true);
			}
			return new ImportOrderEntry(str, false);
		}
		
	}
	
	
	private static class ImportOrganizeLabelProvider extends LabelProvider {
		
		private final Image PCK_ICON;
		private final Image STATIC_CLASS_ICON;

		public ImportOrganizeLabelProvider() {
			PCK_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKAGE);
			STATIC_CLASS_ICON= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, JavaScriptElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
		}
		
		public Image getImage(Object element) {
			return ((ImportOrderEntry) element).isStatic ? STATIC_CLASS_ICON : PCK_ICON;
		}

		public String getText(Object element) {
			ImportOrderEntry entry= (ImportOrderEntry) element;
			String name= entry.name;
			if (name.length() > 0) {
				return name;
			}
			if (entry.isStatic) {
				return PreferencesMessages.ImportOrganizeConfigurationBlock_other_static; 
			}
			return PreferencesMessages.ImportOrganizeConfigurationBlock_other_normal; 
		}
	}
	
	private class ImportOrganizeAdapter implements IListAdapter, IDialogFieldListener {

		private boolean canEdit(ListDialogField field) {
			List selected= field.getSelectedElements();
			return selected.size() == 1;
		}

        public void customButtonPressed(ListDialogField field, int index) {
        	doButtonPressed(index);
        }

        public void selectionChanged(ListDialogField field) {
			fOrderListField.enableButton(IDX_EDIT, canEdit(field));
        }

        public void dialogFieldChanged(DialogField field) {
        	doDialogFieldChanged(field);
        }
        
        public void doubleClicked(ListDialogField field) {
        	if (canEdit(field)) {
				doButtonPressed(IDX_EDIT);
        	}
        }
	}
	
	private static final int IDX_ADD= 0;
	private static final int IDX_ADD_STATIC= 1;
	private static final int IDX_EDIT= 2;
	private static final int IDX_REMOVE= 3;
	private static final int IDX_UP= 5;
	private static final int IDX_DOWN= 6;

	private ListDialogField fOrderListField;
	private StringDialogField fThresholdField;
	private StringDialogField fStaticThresholdField;
	private SelectionButtonDialogField fIgnoreLowerCaseTypesField;
	private SelectionButtonDialogField fExportButton;
	private SelectionButtonDialogField fImportButton;
	
	private PixelConverter fPixelConverter;
	
	public ImportOrganizeConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getAllKeys(), container);
	
		String[] buttonLabels= new String[] { 
			PreferencesMessages.ImportOrganizeConfigurationBlock_order_add_button, 
			PreferencesMessages.ImportOrganizeConfigurationBlock_order_add_static_button, 
			PreferencesMessages.ImportOrganizeConfigurationBlock_order_edit_button, 
			PreferencesMessages.ImportOrganizeConfigurationBlock_order_remove_button, 
			/* 4 */  null,
			PreferencesMessages.ImportOrganizeConfigurationBlock_order_up_button, 
			PreferencesMessages.ImportOrganizeConfigurationBlock_order_down_button, 
		};
				
		ImportOrganizeAdapter adapter= new ImportOrganizeAdapter();
		
		fOrderListField= new ListDialogField(adapter, buttonLabels, new ImportOrganizeLabelProvider());
		fOrderListField.setDialogFieldListener(adapter);
		fOrderListField.setLabelText(PreferencesMessages.ImportOrganizeConfigurationBlock_order_label); 
		fOrderListField.setUpButtonIndex(IDX_UP);
		fOrderListField.setDownButtonIndex(IDX_DOWN);
		fOrderListField.setRemoveButtonIndex(IDX_REMOVE);
		
		fOrderListField.enableButton(IDX_EDIT, false);
		
		fImportButton= new SelectionButtonDialogField(SWT.PUSH);
		fImportButton.setDialogFieldListener(adapter);
		fImportButton.setLabelText(PreferencesMessages.ImportOrganizeConfigurationBlock_order_load_button); 
		
		fExportButton= new SelectionButtonDialogField(SWT.PUSH);
		fExportButton.setDialogFieldListener(adapter);
		fExportButton.setLabelText(PreferencesMessages.ImportOrganizeConfigurationBlock_order_save_button); 
		
		fThresholdField= new StringDialogField();
		fThresholdField.setDialogFieldListener(adapter);
		fThresholdField.setLabelText(PreferencesMessages.ImportOrganizeConfigurationBlock_threshold_label); 

		fStaticThresholdField= new StringDialogField();
		fStaticThresholdField.setDialogFieldListener(adapter);
		fStaticThresholdField.setLabelText(PreferencesMessages.ImportOrganizeConfigurationBlock_staticthreshold_label); 

		fIgnoreLowerCaseTypesField= new SelectionButtonDialogField(SWT.CHECK);
		fIgnoreLowerCaseTypesField.setDialogFieldListener(adapter);
		fIgnoreLowerCaseTypesField.setLabelText(PreferencesMessages.ImportOrganizeConfigurationBlock_ignoreLowerCase_label); 
	
		updateControls();
	}
	
	protected Control createContents(Composite parent) {
		setShell(parent.getShell());
		
		fPixelConverter= new PixelConverter(parent);
	
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		
		composite.setLayout(layout);
		
		fOrderListField.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fOrderListField.getLabelControl(null), 2);
		LayoutUtil.setWidthHint(fOrderListField.getLabelControl(null), fPixelConverter.convertWidthInCharsToPixels(60));
		LayoutUtil.setHorizontalGrabbing(fOrderListField.getListControl(null));
		
		Composite importExportComp= new Composite(composite, SWT.NONE);
		importExportComp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		
		importExportComp.setLayout(layout);
		
		fImportButton.doFillIntoGrid(importExportComp, 1);
		fExportButton.doFillIntoGrid(importExportComp, 1);
		
		fThresholdField.doFillIntoGrid(composite, 2);
		((GridData) fThresholdField.getTextControl(null).getLayoutData()).grabExcessHorizontalSpace= false;
		fStaticThresholdField.doFillIntoGrid(composite, 2);
		fIgnoreLowerCaseTypesField.doFillIntoGrid(composite, 2);
		
		Dialog.applyDialogFont(composite);

		return composite;
	}
	
	private boolean doThresholdChanged(String thresholdString) {
		StatusInfo status= new StatusInfo();
		try {
			int threshold= Integer.parseInt(thresholdString);
			if (threshold < 0) {
				status.setError(PreferencesMessages.ImportOrganizeConfigurationBlock_error_invalidthreshold); 
			}
		} catch (NumberFormatException e) {
			status.setError(PreferencesMessages.ImportOrganizeConfigurationBlock_error_invalidthreshold); 
		}
		updateStatus(status);
		return status.isOK();
	}
		
	private void doButtonPressed(int index) {
		if (index == IDX_ADD || index == IDX_ADD_STATIC) { // add new
			List existing= fOrderListField.getElements();
			ImportOrganizeInputDialog dialog= new ImportOrganizeInputDialog(getShell(), existing, index == IDX_ADD_STATIC);
			if (dialog.open() == Window.OK) {
				List selectedElements= fOrderListField.getSelectedElements();
				if (selectedElements.size() == 1) {
					int insertionIndex= fOrderListField.getIndexOfElement(selectedElements.get(0)) + 1;
					fOrderListField.addElement(dialog.getResult(), insertionIndex);
				} else {
					fOrderListField.addElement(dialog.getResult());
				}
			}
		} else if (index == IDX_EDIT) { // edit
			List selected= fOrderListField.getSelectedElements();
			if (selected.isEmpty()) {
				return;
			}
			ImportOrderEntry editedEntry= (ImportOrderEntry) selected.get(0);
			
			List existing= fOrderListField.getElements();
			existing.remove(editedEntry);
			
			ImportOrganizeInputDialog dialog= new ImportOrganizeInputDialog(getShell(), existing, editedEntry.isStatic);
			dialog.setInitialSelection(editedEntry);
			if (dialog.open() == Window.OK) {
				fOrderListField.replaceElement(editedEntry, dialog.getResult());
			}
		}
	}
	
	
	/*
	 * The import order file is a property file. The keys are
	 * "0", "1" ... last entry. The values must be valid package names.
	 */
	private List loadFromProperties(Properties properties) {
		ArrayList res= new ArrayList();
		int nEntries= properties.size();
		for (int i= 0 ; i < nEntries; i++) {
			String curr= properties.getProperty(String.valueOf(i));
			if (curr != null) {
				ImportOrderEntry entry= ImportOrderEntry.fromSerialized(curr);
				if (!JavaScriptConventions.validatePackageName(entry.name, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_5).matches(IStatus.ERROR)) {
					res.add(entry);
				} else {
					return null;
				}
			} else {
				return res;
			}
		}
		return res;
	}
	
	private List loadImportOrder() {
		IDialogSettings dialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		
		FileDialog dialog= new FileDialog(getShell(), SWT.OPEN);
		dialog.setText(PreferencesMessages.ImportOrganizeConfigurationBlock_loadDialog_title); 
		dialog.setFilterExtensions(new String[] {"*.importorder", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		String lastPath= dialogSettings.get(DIALOGSETTING_LASTLOADPATH);
		if (lastPath != null) {
			dialog.setFilterPath(lastPath);
		}
		String fileName= dialog.open();
		if (fileName != null) {
			dialogSettings.put(DIALOGSETTING_LASTLOADPATH, dialog.getFilterPath());
					
			Properties properties= new Properties();
			FileInputStream fis= null;
			try {
				fis= new FileInputStream(fileName);
				properties.load(fis);
				List res= loadFromProperties(properties);
				if (res != null) {
					return res;
				}
			} catch (IOException e) {
				JavaScriptPlugin.log(e);
			} finally {
				if (fis != null) {
					try { fis.close(); } catch (IOException e) {}
				}
			}
			String title= PreferencesMessages.ImportOrganizeConfigurationBlock_loadDialog_error_title; 
			String message= PreferencesMessages.ImportOrganizeConfigurationBlock_loadDialog_error_message; 
			MessageDialog.openError(getShell(), title, message);
		}
		return null;
	}
	
	private void saveImportOrder(List elements) {
		IDialogSettings dialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		
		FileDialog dialog= new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(PreferencesMessages.ImportOrganizeConfigurationBlock_saveDialog_title); 
		dialog.setFilterExtensions(new String[] {"*.importorder", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFileName("example.importorder"); //$NON-NLS-1$
		String lastPath= dialogSettings.get(DIALOGSETTING_LASTSAVEPATH);
		if (lastPath != null) {
			dialog.setFilterPath(lastPath);
		}
		String fileName= dialog.open();
		if (fileName != null) {
			dialogSettings.put(DIALOGSETTING_LASTSAVEPATH, dialog.getFilterPath());
			
			Properties properties= new Properties();
			for (int i= 0; i < elements.size(); i++) {
				ImportOrderEntry entry= (ImportOrderEntry) elements.get(i);
				properties.setProperty(String.valueOf(i), entry.serialize());
			}
			FileOutputStream fos= null;
			try {
				fos= new FileOutputStream(fileName);
				properties.store(fos, "Organize Import Order"); //$NON-NLS-1$
			} catch (IOException e) {
				JavaScriptPlugin.log(e);
				String title= PreferencesMessages.ImportOrganizeConfigurationBlock_saveDialog_error_title; 
				String message= PreferencesMessages.ImportOrganizeConfigurationBlock_saveDialog_error_message; 
				MessageDialog.openError(getShell(), title, message);				
			} finally {
				if (fos != null) {
					try { fos.close(); } catch (IOException e) {}
				}
			}
		}
	}

	private void updateStatus(IStatus status) {
		fContext.statusChanged(status);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(java.lang.String, java.lang.String)
	 */
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// no validation
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#updateControls()
	 */
	protected void updateControls() {
		ImportOrderEntry[] importOrder= getImportOrderPreference();
		int threshold= getImportNumberThreshold(PREF_ONDEMANDTHRESHOLD);
		int staticThreshold= getImportNumberThreshold(PREF_STATICONDEMANDTHRESHOLD);
		boolean ignoreLowerCase= Boolean.valueOf(getValue(PREF_IGNORELOWERCASE)).booleanValue();
		
		fOrderListField.removeAllElements();
		for (int i= 0; i < importOrder.length; i++) {
			fOrderListField.addElement(importOrder[i]);
		}
		fThresholdField.setText(String.valueOf(threshold));
		fStaticThresholdField.setText(String.valueOf(staticThreshold));
		fIgnoreLowerCaseTypesField.setSelection(ignoreLowerCase);
	}	
	
	
	protected final void doDialogFieldChanged(DialogField field) {
		// set values in working copy
		if (field == fOrderListField) {
	  		setValue(PREF_IMPORTORDER, packOrderList(fOrderListField.getElements()));
		} else if (field == fThresholdField) {
	  		if (doThresholdChanged(fThresholdField.getText())) {
		  		setValue(PREF_ONDEMANDTHRESHOLD, fThresholdField.getText());
	  		}
		} else if (field == fStaticThresholdField) {
	  		if (doThresholdChanged(fStaticThresholdField.getText())) {
		  		setValue(PREF_STATICONDEMANDTHRESHOLD, fStaticThresholdField.getText());
	  		}
		} else if (field == fIgnoreLowerCaseTypesField) {
	  		setValue(PREF_IGNORELOWERCASE, fIgnoreLowerCaseTypesField.isSelected());
		} else if (field == fImportButton) {
			List order= loadImportOrder();
			if (order != null) {
				fOrderListField.setElements(order);
			}
		} else if (field == fExportButton) {
			saveImportOrder(fOrderListField.getElements());
		}
	}
	
		
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null; // no build required
	}

	private static ImportOrderEntry[] unpackOrderList(String str) {
		ArrayList res= new ArrayList();
		int start= 0;
		do {
			int end= str.indexOf(';', start);
			if (end == -1) {
				end= str.length();
			}
			res.add(ImportOrderEntry.fromSerialized(str.substring(start, end)));
			start= end + 1;
		} while (start < str.length());
		
		return (ImportOrderEntry[]) res.toArray(new ImportOrderEntry[res.size()]);
	}
	
	private static String packOrderList(List orderList) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < orderList.size(); i++) {
			ImportOrderEntry entry= (ImportOrderEntry) orderList.get(i);
			buf.append(entry.serialize());
			buf.append(';');
		}
		return buf.toString();
	}
	
	private ImportOrderEntry[] getImportOrderPreference() {
		String str= getValue(PREF_IMPORTORDER);
		if (str != null) {
			return unpackOrderList(str);
		}
		return new ImportOrderEntry[0];
	}
	
	private int getImportNumberThreshold(Key key) {
		String thresholdStr= getValue(key);
		try {
			int threshold= Integer.parseInt(thresholdStr);
			if (threshold < 0) {
				threshold= Integer.MAX_VALUE;
			}
			return threshold;
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

}


