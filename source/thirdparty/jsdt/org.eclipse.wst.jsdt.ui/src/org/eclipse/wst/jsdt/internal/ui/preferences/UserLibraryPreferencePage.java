/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.IUIConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.AccessRulesDialog;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElementSorter;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPUserLibraryElement;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.ClasspathAttributeConfigurationDescriptors;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.TreeListDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UserLibraryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID= "org.eclipse.wst.jsdt.ui.preferences.UserLibraryPreferencePage"; //$NON-NLS-1$
	public static final String DATA_DO_CREATE= "do_create"; //$NON-NLS-1$
	public static final String DATA_LIBRARY_TO_SELECT= "select_library"; //$NON-NLS-1$
	
	public static class LibraryNameDialog extends StatusDialog implements IDialogFieldListener {

		private StringDialogField fNameField;
		
		private CPUserLibraryElement fElementToEdit;
		private List fExistingLibraries;

		public LibraryNameDialog(Shell parent, CPUserLibraryElement elementToEdit, List existingLibraries) {
			super(parent);
			if (elementToEdit == null) { 
				setTitle(PreferencesMessages.UserLibraryPreferencePage_LibraryNameDialog_new_title); 
			} else {
				setTitle(PreferencesMessages.UserLibraryPreferencePage_LibraryNameDialog_edit_title); 
			}
			
			fElementToEdit= elementToEdit;
			fExistingLibraries= existingLibraries;
			
			fNameField= new StringDialogField();
			fNameField.setDialogFieldListener(this);
			fNameField.setLabelText(PreferencesMessages.UserLibraryPreferencePage_LibraryNameDialog_name_label); 
			
			if (elementToEdit != null) {
				fNameField.setText(elementToEdit.getName());
			} else {
				fNameField.setText(""); //$NON-NLS-1$
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			LayoutUtil.doDefaultLayout(composite, new DialogField[] { fNameField }, true, SWT.DEFAULT, SWT.DEFAULT);
			LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
			fNameField.postSetFocusOnDialogField(parent.getDisplay());
			
			Dialog.applyDialogFont(composite);
			
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.CP_EDIT_USER_LIBRARY);
			
			return composite;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fNameField) {
				updateStatus(validateSettings());
			}
		}
		
		private IStatus validateSettings() {
			String name= fNameField.getText();
			if (name.length() == 0) {
				return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LibraryNameDialog_name_error_entername); 
			}
			for (int i= 0; i < fExistingLibraries.size(); i++) {
				CPUserLibraryElement curr= (CPUserLibraryElement) fExistingLibraries.get(i);
				if (curr != fElementToEdit && name.equals(curr.getName())) {
					return new StatusInfo(IStatus.ERROR, Messages.format(PreferencesMessages.UserLibraryPreferencePage_LibraryNameDialog_name_error_exists, name)); 
				}
			}
			IStatus status= ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
			if (status.matches(IStatus.ERROR)) {
				return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LibraryNameDialog_name_error_invalid);
			}
			return StatusInfo.OK_STATUS;
		}
		
		public CPUserLibraryElement getNewLibrary() {
			CPListElement[] entries= null;
			if (fElementToEdit != null) {
				entries= fElementToEdit.getChildren();
			}
			return new CPUserLibraryElement(fNameField.getText(), entries);
		}
		
	}
	
	public static class LoadSaveDialog extends StatusDialog implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		
		private static final String VERSION1= "1"; //$NON-NLS-1$ // using OS strings for archive path and source attachment
		private static final String CURRENT_VERSION= "2"; //$NON-NLS-1$

		private static final String TAG_ROOT= "eclipse-userlibraries"; //$NON-NLS-1$
		private static final String TAG_VERSION= "version"; //$NON-NLS-1$
		private static final String TAG_LIBRARY= "library"; //$NON-NLS-1$
		private static final String TAG_ARCHIVE_PATH= "path"; //$NON-NLS-1$
		private static final String TAG_ARCHIVE= "archive"; //$NON-NLS-1$
		private static final String TAG_NAME= "name"; //$NON-NLS-1$
		private static final String TAG_JSDOC= "jsdoc"; //$NON-NLS-1$
		private static final String TAG_ACCESSRULES= "accessrules"; //$NON-NLS-1$
		private static final String TAG_ACCESSRULE= "accessrule"; //$NON-NLS-1$
		private static final String TAG_RULE_KIND= "kind"; //$NON-NLS-1$
		private static final String TAG_RULE_PATTERN= "pattern"; //$NON-NLS-1$

		private static final String PREF_LASTPATH= JavaScriptUI.ID_PLUGIN + ".lastuserlibrary"; //$NON-NLS-1$
		private static final String PREF_USER_LIBRARY_LOADSAVE_SIZE= "UserLibraryLoadSaveDialog.size"; //$NON-NLS-1$
		
		private List fExistingLibraries;
		private IDialogSettings fSettings;
		
		private File fLastFile;
		
		private StringButtonDialogField fLocationField;
		private CheckedListDialogField fExportImportList;
		private Point fInitialSize;
		private final boolean fIsSave;

		public LoadSaveDialog(Shell shell, boolean isSave, List existingLibraries, IDialogSettings dialogSettings) {
			super(shell);
			initializeDialogUnits(shell);
			
			setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
			
			fExistingLibraries= existingLibraries;
			fSettings= dialogSettings;
			fLastFile= null;
			fIsSave= isSave;
			
			int defaultWidth= convertWidthInCharsToPixels(80);
			int defaultHeigth= convertHeightInCharsToPixels(34);
			String lastSize= fSettings.get(PREF_USER_LIBRARY_LOADSAVE_SIZE);
			if (lastSize != null) {
				fInitialSize= StringConverter.asPoint(lastSize, new Point(defaultWidth, defaultHeigth));
			} else {
				fInitialSize= new Point(defaultWidth, defaultHeigth);
			}
		
			if (isSave()) {
				setTitle(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_save_title); 
			} else {
				setTitle(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_title);				 
			}
			
			fLocationField= new StringButtonDialogField(this);
			fLocationField.setLabelText(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_location_label); 
			fLocationField.setButtonLabel(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_location_button); 
			fLocationField.setDialogFieldListener(this);
			
			String[] buttonNames= new String[] {
					PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_list_selectall_button, 
					PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_list_deselectall_button
			};
			fExportImportList= new CheckedListDialogField(this, buttonNames, new CPListLabelProvider());
			fExportImportList.setCheckAllButtonIndex(0);
			fExportImportList.setUncheckAllButtonIndex(1);
			fExportImportList.setViewerComparator(new CPListElementSorter());
			fExportImportList.setDialogFieldListener(this);
			if (isSave()) {
				fExportImportList.setLabelText(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_list_save_label); 
				fExportImportList.setElements(fExistingLibraries);
				fExportImportList.checkAll(true);
			} else {
				fExportImportList.setLabelText(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_list_load_label); 
			}
			String lastPath= fSettings.get(PREF_LASTPATH);
			if (lastPath != null) {
				fLocationField.setText(lastPath);
			} else {
				fLocationField.setText(""); //$NON-NLS-1$
			}
		}
				
		protected Point getInitialSize() {
			return fInitialSize;
		}
		
		private boolean isSave() {
			return fIsSave;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			DialogField[] fields;
			if (isSave()) {
				fields= new DialogField[] { fExportImportList, fLocationField };
			} else {
				fields= new DialogField[] { fLocationField, fExportImportList };
			}
			LayoutUtil.doDefaultLayout(composite, fields, true, SWT.DEFAULT, SWT.DEFAULT);
			fExportImportList.getListControl(null).setLayoutData(new GridData(GridData.FILL_BOTH));
			
			fLocationField.postSetFocusOnDialogField(parent.getDisplay());
			
			Dialog.applyDialogFont(composite);
			
			if (isSave()) {
				PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.CP_EXPORT_USER_LIBRARY);
			} else {
				PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.CP_IMPORT_USER_LIBRARY);
			}
			
			return composite;
		}
		

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			String label= isSave() ? PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_filedialog_save_title : PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_filedialog_load_title; 
			FileDialog dialog= new FileDialog(getShell(), isSave() ? SWT.SAVE : SWT.OPEN);
			dialog.setText(label);
			dialog.setFilterExtensions(new String[] {"*.userlibraries", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			String lastPath= fLocationField.getText();
			if (lastPath.length() == 0 || !new File(lastPath).exists()) {
				lastPath= fSettings.get(PREF_LASTPATH);
			}
			if (lastPath != null) {
				dialog.setFileName(lastPath);
			}
			String fileName= dialog.open();
			if (fileName != null) {
				fSettings.put(PREF_LASTPATH, fileName);
				fLocationField.setText(fileName);
			}
		}

		private IStatus updateShownLibraries(IStatus status) {
			if (!status.isOK()) {
				fExportImportList.removeAllElements();
				fExportImportList.setEnabled(false);
				fLastFile= null;
			} else {
				File file= new File(fLocationField.getText());
				if (!file.equals(fLastFile)) {
					fLastFile= file;
					try {
						List elements= loadLibraries(file);
						fExportImportList.setElements(elements);
						fExportImportList.checkAll(true);
						fExportImportList.setEnabled(true);
						if (elements.isEmpty()) {
							return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_error_empty); 
						}
					} catch (IOException e) {
						fExportImportList.removeAllElements();
						fExportImportList.setEnabled(false);
						return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_error_invalidfile); 
					}
				}
			}
			return status;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fLocationField) {
				IStatus status= validateSettings();
				if (!isSave()) {
					status= updateShownLibraries(status);
				}
				updateStatus(status);
			} else if (field == fExportImportList) {
				updateStatus(validateSettings());
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		public void customButtonPressed(ListDialogField field, int index) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void selectionChanged(ListDialogField field) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void doubleClicked(ListDialogField field) {
			List selectedElements= fExportImportList.getSelectedElements();
			if (selectedElements.size() == 1) {
				Object elem= selectedElements.get(0);
				fExportImportList.setChecked(elem, !fExportImportList.isChecked(elem));
			}
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
		 */
		protected void okPressed() {
			if (isSave()) {
				final File file= new File(fLocationField.getText());
				if (file.exists()) {
					String title= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_overwrite_title; 
					String message= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_overwrite_message; 
					if (!MessageDialog.openQuestion(getShell(), title, message)) {
						return;
					}
				}
				try {
					String encoding= "UTF-8"; //$NON-NLS-1$
					IPath filePath= Path.fromOSString(file.getCanonicalPath());
					final IPath workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation();
					if (filePath.matchingFirstSegments(workspacePath) == workspacePath.segmentCount()) {
						IPath path= filePath.removeFirstSegments(workspacePath.segmentCount());
						path= path.makeRelative();
						final IFile result= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
						try {
							encoding= result.getCharset(true);
						} catch (CoreException exception) {
							JavaScriptPlugin.log(exception);
						}
					}
					final List elements= fExportImportList.getCheckedElements();
					final String charset= encoding;
					IRunnableContext context= PlatformUI.getWorkbench().getProgressService();
					try {
						context.run(true, true, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {
									saveLibraries(elements, file, charset, monitor);
								} catch (IOException e) {
									throw new InvocationTargetException(e);
								}
							}
						});
						fSettings.put(PREF_LASTPATH, file.getPath());
					} catch (InvocationTargetException e) {
						String errorTitle= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_save_errordialog_title; 
						String errorMessage= Messages.format(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_save_errordialog_message, e.getMessage()); 
						ExceptionHandler.handle(e, getShell(), errorTitle, errorMessage);
						return;
					} catch (InterruptedException e) {
						// cancelled
						return;
					}
					String savedTitle= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_save_ok_title; 
					String savedMessage= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_save_ok_message; 
					MessageDialog.openInformation(getShell(), savedTitle, savedMessage);
				} catch (IOException exception) {
					JavaScriptPlugin.log(exception);
				}
			} else {
				HashSet map= new HashSet(fExistingLibraries.size());
				for (int k= 0; k < fExistingLibraries.size(); k++) {
					CPUserLibraryElement elem= (CPUserLibraryElement) fExistingLibraries.get(k);
					map.add(elem.getName());
				}
				int nReplaced= 0;
				List elements= getLoadedLibraries();
				for (int i= 0; i < elements.size(); i++) {
					CPUserLibraryElement curr= (CPUserLibraryElement) elements.get(i);
					if (map.contains(curr.getName())) {
						nReplaced++;
					}
				}
				if (nReplaced > 0) {
					String replaceTitle= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_replace_title; 
					String replaceMessage;
					if (nReplaced == 1) {
						replaceMessage= PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_replace_message; 
					} else {
						replaceMessage= Messages.format(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_replace_multiple_message, String.valueOf(nReplaced)); 
					}
					if (!MessageDialog.openConfirm(getShell(), replaceTitle, replaceMessage)) {
						return;
					}
				}
			}
			super.okPressed();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#close()
		 */
		public boolean close() {
			Point point= getShell().getSize();
			fSettings.put(PREF_USER_LIBRARY_LOADSAVE_SIZE, StringConverter.asString(point));
			return super.close();
		}
		
		private IStatus validateSettings() {
			String name= fLocationField.getText();
			fLastFile= null;
			if (isSave()) {
				if (name.length() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_location_error_save_enterlocation); 
				}
				File file= new File(name);
				if (file.isDirectory()) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_location_error_save_invalid); 
				}
				if (fExportImportList.getCheckedSize() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_list_error_save_nothingselected); 
				}
				fLastFile= file;
			} else {
				if (name.length() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_location_error_load_enterlocation); 
				}
				if (!new File(name).isFile()) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_location_error_load_invalid); 
				}
				if (fExportImportList.getSize() > 0 && fExportImportList.getCheckedSize() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_list_error_load_nothingselected); 
				}
			}
			return new StatusInfo();
		}
		
		protected static void saveLibraries(List libraries, File file, String encoding, IProgressMonitor monitor) throws IOException {
			OutputStream stream= new FileOutputStream(file);
			try {
				DocumentBuilder docBuilder= null;
				DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				docBuilder= factory.newDocumentBuilder();
				Document document= docBuilder.newDocument();
	
				// Create the document
				Element rootElement= document.createElement(TAG_ROOT);
				document.appendChild(rootElement);
	
				rootElement.setAttribute(TAG_VERSION, CURRENT_VERSION); 
				
				for (int i= 0; i < libraries.size(); i++) {
					Element libraryElement= document.createElement(TAG_LIBRARY);
					rootElement.appendChild(libraryElement);
					
					CPUserLibraryElement curr= (CPUserLibraryElement) libraries.get(i);
					libraryElement.setAttribute(TAG_NAME, curr.getName());
					
					CPListElement[] children= curr.getChildren();
					for (int k= 0; k < children.length; k++) {
						CPListElement child= children[k];
						
						Element childElement= document.createElement(TAG_ARCHIVE); 
						libraryElement.appendChild(childElement);
						
						childElement.setAttribute(TAG_ARCHIVE_PATH, child.getPath().toPortableString());
						
						String javadocLocation= (String) child.getAttribute(CPListElement.JAVADOC);
						if (javadocLocation != null) {
							childElement.setAttribute(TAG_JSDOC, javadocLocation);
						}

						IAccessRule[] accessRules= (IAccessRule[]) child.getAttribute(CPListElement.ACCESSRULES);
						if (accessRules != null && accessRules.length > 0) {
							Element rulesElement= document.createElement(TAG_ACCESSRULES); 
							childElement.appendChild(rulesElement);
							for (int n= 0; n < accessRules.length; n++) {
								IAccessRule rule= accessRules[n];
								Element ruleElement= document.createElement(TAG_ACCESSRULE); 
								rulesElement.appendChild(ruleElement);
								ruleElement.setAttribute(TAG_RULE_KIND, String.valueOf(rule.getKind()));
								ruleElement.setAttribute(TAG_RULE_PATTERN, rule.getPattern().toPortableString());
							}
						}
					}
				}
	
				// Write the document to the stream
				Transformer transformer=TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
				transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","4"); //$NON-NLS-1$ //$NON-NLS-2$

				DOMSource source = new DOMSource(document);
				StreamResult result = new StreamResult(stream);
				transformer.transform(source, result);	
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} catch (TransformerException e) {
				throw new IOException(e.getMessage());			
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
				if (monitor != null) {
					monitor.done();
				}
			}
		}
				
		private static List loadLibraries(File file) throws IOException {
			InputStream stream= new FileInputStream(file);
			Element cpElement;
			try {
				DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				cpElement = parser.parse(new InputSource(stream)).getDocumentElement();
			} catch (SAXException e) {
				throw new IOException(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_badformat); 
			} catch (ParserConfigurationException e) {
				throw new IOException(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_badformat); 
			} finally {
				stream.close();
			}
			
			if (!cpElement.getNodeName().equalsIgnoreCase(TAG_ROOT)) {
				throw new IOException(PreferencesMessages.UserLibraryPreferencePage_LoadSaveDialog_load_badformat); 
			}
			
			String version= cpElement.getAttribute(TAG_VERSION); 
			
			NodeList libList= cpElement.getElementsByTagName(TAG_LIBRARY);
			int length = libList.getLength();
			
			ArrayList result= new ArrayList(length);
			for (int i= 0; i < length; i++) {
				Node lib= libList.item(i);
				if (!(lib instanceof Element)) {
					continue;
				}
				Element libElement= (Element) lib;
				String name= libElement.getAttribute(TAG_NAME);
				
				CPUserLibraryElement newLibrary= new CPUserLibraryElement(name, null);
				result.add(newLibrary);
				
				NodeList archiveList= libElement.getElementsByTagName(TAG_ARCHIVE);
				for (int k= 0; k < archiveList.getLength(); k++) {
					Node archiveNode= archiveList.item(k);
					if (!(archiveNode instanceof Element)) {
						continue;
					}
					Element archiveElement= (Element) archiveNode;
					
					String pathString= archiveElement.getAttribute(TAG_ARCHIVE_PATH);
					IPath path= version.equals(VERSION1) ? Path.fromOSString(pathString) : Path.fromPortableString(pathString);
					CPListElement newArchive= new CPListElement(newLibrary, null, IIncludePathEntry.CPE_LIBRARY, path, null);
					newLibrary.add(newArchive);
					
					if (archiveElement.hasAttribute(TAG_JSDOC)) {
						String javadoc= archiveElement.getAttribute(TAG_JSDOC);
						newArchive.setAttribute(CPListElement.JAVADOC, javadoc);
					}
					
					NodeList rulesParentNodes= archiveElement.getElementsByTagName(TAG_ACCESSRULES);
					if (rulesParentNodes.getLength() > 0 && rulesParentNodes.item(0) instanceof Element) {
						Element ruleParentElement= (Element) rulesParentNodes.item(0); // take first, ignore others
						NodeList ruleElements= ruleParentElement.getElementsByTagName(TAG_ACCESSRULE);
						int nRuleElements= ruleElements.getLength();
						if (nRuleElements > 0) {
							ArrayList resultingRules= new ArrayList(nRuleElements);
							for (int n= 0; n < nRuleElements; n++) {
								Node node= ruleElements.item(n);
								if (node instanceof Element) {
									Element ruleElement= (Element) node;
									try {
										int kind= Integer.parseInt(ruleElement.getAttribute(TAG_RULE_KIND));
										IPath pattern= Path.fromPortableString(ruleElement.getAttribute(TAG_RULE_PATTERN));
										resultingRules.add(JavaScriptCore.newAccessRule(pattern, kind));
									} catch (NumberFormatException e) {
										// ignore
									}
								}
							}
							newArchive.setAttribute(CPListElement.ACCESSRULES, resultingRules.toArray(new IAccessRule[resultingRules.size()]));
						}
					}					
				}
			}
			return result;
		}

		public List getLoadedLibraries() {
			return fExportImportList.getCheckedElements();
		}
	}
	
	private IDialogSettings fDialogSettings;
	private TreeListDialogField fLibraryList;
	private IJavaScriptProject fDummyProject;
	private ClasspathAttributeConfigurationDescriptors fAttributeDescriptors;
		
	private static final int IDX_NEW= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_ADD_FILE= 2;
	private static final int IDX_ADD_FOLDER=3;
	private static final int IDX_REMOVE= 4;
	private static final int IDX_LOAD= 5;
	private static final int IDX_SAVE= 6;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public UserLibraryPreferencePage() {
		setPreferenceStore(JavaScriptPlugin.getDefault().getPreferenceStore());
		fDummyProject= createPlaceholderProject();
		
		fAttributeDescriptors= JavaScriptPlugin.getDefault().getClasspathAttributeConfigurationDescriptors();
	
		// title only used when page is shown programatically
		setTitle(PreferencesMessages.UserLibraryPreferencePage_title); 
		setDescription(PreferencesMessages.UserLibraryPreferencePage_description); 
		noDefaultAndApplyButton();

		fDialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		
		UserLibraryAdapter adapter= new UserLibraryAdapter();
		String[] buttonLabels= new String[] {
				PreferencesMessages.UserLibraryPreferencePage_libraries_new_button, 
				PreferencesMessages.UserLibraryPreferencePage_libraries_edit_button, 
				PreferencesMessages.UserLibraryPreferencePage_libraries_addjar_button, 
				PreferencesMessages.UserLibraryPreferencePage_libraries_addfolder_button,
				PreferencesMessages.UserLibraryPreferencePage_libraries_remove_button, 
				PreferencesMessages.UserLibraryPreferencePage_libraries_load_button, 
				PreferencesMessages.UserLibraryPreferencePage_libraries_save_button
		};
		
		fLibraryList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fLibraryList.setLabelText(PreferencesMessages.UserLibraryPreferencePage_libraries_label);
		
		String[] names= JavaScriptCore.getUserLibraryNames();
		ArrayList elements= new ArrayList();
		
		for (int i= 0; i < names.length; i++) {
			IPath path= new Path(JavaScriptCore.USER_LIBRARY_CONTAINER_ID).append(names[i]);
			try {
				IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(path, fDummyProject);
				elements.add(new CPUserLibraryElement(names[i], container, fDummyProject));
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
				// ignore
			}
		}
		fLibraryList.setElements(elements);
		fLibraryList.setViewerComparator(new CPListElementSorter());
		
		doSelectionChanged(fLibraryList); //update button enable state 
	}
	
	private static IJavaScriptProject createPlaceholderProject() {
		StringBuilder name= new StringBuilder("####jsdtinternal"); //$NON-NLS-1$
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		while (true) {
			IProject project= root.getProject(name.toString());
			if (!project.exists()) {
				return JavaScriptCore.create(project);
			}
			name.append('1');
		}		
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
	 */
	public void applyData(Object data) {
		if (data instanceof Map) {
			Map map= (Map) data;
			Object selectedLibrary= map.get(DATA_LIBRARY_TO_SELECT);
			boolean createIfNotExists= Boolean.TRUE.equals(map.get(DATA_DO_CREATE));
			if (selectedLibrary instanceof String) {
				int nElements= fLibraryList.getSize();
				for (int i= 0; i < nElements; i++) {
					CPUserLibraryElement curr= (CPUserLibraryElement) fLibraryList.getElement(i);
					if (curr.getName().equals(selectedLibrary)) {
						fLibraryList.selectElements(new StructuredSelection(curr));
						fLibraryList.expandElement(curr, 1);
						break;
					}
				}
				if (createIfNotExists) {
					CPUserLibraryElement elem= new CPUserLibraryElement((String) selectedLibrary, null, createPlaceholderProject());
					fLibraryList.addElement(elem);
					fLibraryList.selectElements(new StructuredSelection(elem));
				}
			}
		}
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.CP_USERLIBRARIES_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibraryList }, true);
		LayoutUtil.setHorizontalGrabbing(fLibraryList.getTreeControl(null));
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
	}

	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() { 
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						if (monitor != null) {
							monitor= new NullProgressMonitor();
						}
						
						updateUserLibararies(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title= PreferencesMessages.UserLibraryPreferencePage_config_error_title; 
			String message= PreferencesMessages.UserLibraryPreferencePage_config_error_message; 
			ExceptionHandler.handle(e, getShell(), title, message);
		}
		return true;
	}
	
	
	private void updateUserLibararies(IProgressMonitor monitor) throws CoreException {
		List list= fLibraryList.getElements();
		HashSet oldNames= new HashSet(Arrays.asList(JavaScriptCore.getUserLibraryNames()));
		int nExisting= list.size();
		
		HashSet newEntries= new HashSet(list.size());
		for (int i= 0; i < nExisting; i++) {
			CPUserLibraryElement element= (CPUserLibraryElement) list.get(i);
			boolean contained= oldNames.remove(element.getName());
			if (!contained) {
				newEntries.add(element);
			}
		}	
		
		int len= nExisting + oldNames.size();
		monitor.beginTask(PreferencesMessages.UserLibraryPreferencePage_operation, len); 
		MultiStatus multiStatus= new MultiStatus(JavaScriptUI.ID_PLUGIN, IStatus.OK, PreferencesMessages.UserLibraryPreferencePage_operation_error, null); 
		
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(JavaScriptCore.USER_LIBRARY_CONTAINER_ID);
		IJavaScriptProject jproject= fDummyProject;
				
		for (int i= 0; i < nExisting; i++) {
			CPUserLibraryElement element= (CPUserLibraryElement) list.get(i);
			IPath path= element.getPath();
			if (newEntries.contains(element) || element.hasChanges(JavaScriptCore.getJsGlobalScopeContainer(path, jproject))) {
				IJsGlobalScopeContainer updatedContainer= element.getUpdatedContainer();
				try {
					initializer.requestJsGlobalScopeContainerUpdate(path, jproject, updatedContainer);
				} catch (CoreException e) {
					multiStatus.add(e.getStatus());
				}
			}
			monitor.worked(1);
		}
		
		Iterator iter= oldNames.iterator();
		while (iter.hasNext()) {
			String name= (String) iter.next();
			
			IPath path= new Path(JavaScriptCore.USER_LIBRARY_CONTAINER_ID).append(name);
			try {
				initializer.requestJsGlobalScopeContainerUpdate(path, jproject, null);
			} catch (CoreException e) {
				multiStatus.add(e.getStatus());
			}
			monitor.worked(1);
		}
		
		if (!multiStatus.isOK()) {
			throw new CoreException(multiStatus);
		}
	}
	
	private CPUserLibraryElement getSingleSelectedLibrary(List selected) {
		if (selected.size() == 1 && selected.get(0) instanceof CPUserLibraryElement) {
			return (CPUserLibraryElement) selected.get(0);
		}
		return null;
	}
	
	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		CPListElement selElement= elem.getParent();
		if (key.equals(CPListElement.ACCESSRULES)) {
			AccessRulesDialog dialog= new AccessRulesDialog(getShell(), selElement, null, false);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.ACCESSRULES, dialog.getAccessRules());
				fLibraryList.refresh(elem);
				fLibraryList.expandElement(elem, 2);
			}
		} else if (!elem.isBuiltIn()) {
			ClasspathAttributeConfiguration config= fAttributeDescriptors.get(key);
			if (config != null) {
				IIncludePathAttribute result= config.performEdit(getShell(), elem.getClasspathAttributeAccess());
				if (result != null) {
					elem.setValue(result.getValue());
					fLibraryList.refresh(elem);
				}
			}
		}
	}
	
	protected void doSelectionChanged(TreeListDialogField field) {
		List list= field.getSelectedElements();
		field.enableButton(IDX_REMOVE, canRemove(list));
		field.enableButton(IDX_EDIT, canEdit(list));
		field.enableButton(IDX_ADD_FILE, canAdd(list));
		field.enableButton(IDX_ADD_FOLDER, canAdd(list));
		field.enableButton(IDX_SAVE, field.getSize() > 0);	
	}
	
	protected void doCustomButtonPressed(TreeListDialogField field, int index) {
		if (index == IDX_NEW) {
			editUserLibraryElement(null);
		} else if (index == IDX_ADD_FILE) {
			doAdd(field.getSelectedElements());
		} else if (index == IDX_ADD_FOLDER) {
			doAddFolder(field.getSelectedElements());
		} else if (index == IDX_REMOVE) {
			doRemove(field.getSelectedElements());
		} else if (index == IDX_EDIT) {
			doEdit(field.getSelectedElements());
		} else if (index == IDX_SAVE) {
			doSave();
		} else if (index == IDX_LOAD) {
			doLoad();
		}
	}
	
	protected void doDoubleClicked(TreeListDialogField field) {
		List selected= field.getSelectedElements();
		if (canEdit(selected)) {
			doEdit(field.getSelectedElements());
		}
	}
	
	protected void doKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			List selection= field.getSelectedElements();
			if (canRemove(selection)) {
				doRemove(selection);
			}
		}
	}
	
	private void doEdit(List selected) {
		if (selected.size() == 1) {
			Object curr= selected.get(0);
			if (curr instanceof CPListElementAttribute) {
				editAttributeEntry((CPListElementAttribute) curr);
			} else if (curr instanceof CPUserLibraryElement) {
				editUserLibraryElement((CPUserLibraryElement) curr);
			} else if (curr instanceof CPListElement) {
				CPListElement elem= (CPListElement) curr;
				if(elem.getPath().lastSegment().endsWith(".js")) { //$NON-NLS-1$
					editArchiveElement(elem, (CPUserLibraryElement) elem.getParentContainer(), false);
				} else {
					editArchiveElement(elem, (CPUserLibraryElement) elem.getParentContainer(), true);
				}
			}
			doSelectionChanged(fLibraryList);
		}
	}

	private void editUserLibraryElement(CPUserLibraryElement element) {
		LibraryNameDialog dialog= new LibraryNameDialog(getShell(), element, fLibraryList.getElements());
		if (dialog.open() == Window.OK) {
			CPUserLibraryElement newLibrary= dialog.getNewLibrary();
			if (element != null) {
				fLibraryList.replaceElement(element, newLibrary);
			} else {
				fLibraryList.addElement(newLibrary);
			}
			fLibraryList.expandElement(newLibrary, AbstractTreeViewer.ALL_LEVELS);
			fLibraryList.selectElements(new StructuredSelection(newLibrary));
		}
	}
	
	private void editArchiveElement(CPListElement existingElement, CPUserLibraryElement parent, boolean isFolder) {
		CPListElement[] elements= null;
		if(isFolder) {
			elements = openExtJsFolderDialog(parent);
		} else {
			elements = openExtJSFileDialog(existingElement, parent);
		}
		if (elements != null) {
			for (int i= 0; i < elements.length; i++) {
				if (existingElement != null) {
					parent.replace(existingElement, elements[i]);
				} else {
					parent.add(elements[i]);
				}
			}
			fLibraryList.refresh(parent);
			fLibraryList.selectElements(new StructuredSelection(Arrays.asList(elements)));
			fLibraryList.expandElement(parent, 2);
		}
	}


	private void doRemove(List selected) {
		Object selectionAfter= null;
		for (int i= 0; i < selected.size(); i++) {
			Object curr= selected.get(i);
			if (curr instanceof CPUserLibraryElement) {
				fLibraryList.removeElement(curr);
			} else if (curr instanceof CPListElement) {
				Object parent= ((CPListElement) curr).getParentContainer();
				if (parent instanceof CPUserLibraryElement) {
					CPUserLibraryElement elem= (CPUserLibraryElement) parent;
					elem.remove((CPListElement) curr);
					fLibraryList.refresh(elem);
					selectionAfter= parent;
				}
			} else if (curr instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) curr;
				if (attrib.isBuiltIn()) {
					Object value= null;
					String key= attrib.getKey();
					if (key.equals(CPListElement.ACCESSRULES)) {
						value= new IAccessRule[0];
					}
					attrib.getParent().setAttribute(key, value);
					fLibraryList.refresh(attrib);
				} else {
					ClasspathAttributeConfiguration config= fAttributeDescriptors.get(attrib.getKey());
					if (config != null) {
						IIncludePathAttribute result= config.performRemove(attrib.getClasspathAttributeAccess());
						if (result != null) {
							attrib.setValue(result.getValue());
							fLibraryList.refresh(attrib);
						}
					}
				}
			}
		}
		if (fLibraryList.getSelectedElements().isEmpty()) {
			if (selectionAfter != null) {
				fLibraryList.selectElements(new StructuredSelection(selectionAfter));
			} else {
				fLibraryList.selectFirstElement();	
			}
		} else {
			doSelectionChanged(fLibraryList);
		}
	}

	private void doAdd(List list) {
		if (canAdd(list)) {
			CPUserLibraryElement element= getSingleSelectedLibrary(list);
			editArchiveElement(null, element, false);
		}
	}
	
	private void doAddFolder(List list) {
		if (canAdd(list)) {
			CPUserLibraryElement element= getSingleSelectedLibrary(list);
			editArchiveElement(null, element, true);
		}
	}
	
	private void doLoad() {
		List existing= fLibraryList.getElements();
		LoadSaveDialog dialog= new LoadSaveDialog(getShell(), false, existing, fDialogSettings);
		if (dialog.open() == Window.OK) {
			HashMap map= new HashMap(existing.size());
			for (int k= 0; k < existing.size(); k++) {
				CPUserLibraryElement elem= (CPUserLibraryElement) existing.get(k);
				map.put(elem.getName(), elem);
			}
			
			List list= dialog.getLoadedLibraries();
			for (int i= 0; i < list.size(); i++) {
				CPUserLibraryElement elem= (CPUserLibraryElement) list.get(i);
				CPUserLibraryElement found= (CPUserLibraryElement) map.get(elem.getName());
				if (found == null) {
					existing.add(elem);
					map.put(elem.getName(), elem);
				} else {
					existing.set(existing.indexOf(found), elem); // replace
				}
			}
			fLibraryList.setElements(existing);
			fLibraryList.selectElements(new StructuredSelection(list));
		}
	}
	
	private void doSave() {
		LoadSaveDialog dialog= new LoadSaveDialog(getShell(), true, fLibraryList.getElements(), fDialogSettings);
		dialog.open();
	}
	
	private boolean canAdd(List list) {
		return getSingleSelectedLibrary(list) != null;
	}

	private boolean canEdit(List list) {
		if (list.size() != 1)
			return false;
		
		Object firstElement= list.get(0);
		if (firstElement instanceof IAccessRule)
			return false;
		if (firstElement instanceof CPListElementAttribute) {
			CPListElementAttribute attrib= (CPListElementAttribute) firstElement;
			if (!attrib.isBuiltIn()) {
				ClasspathAttributeConfiguration config= fAttributeDescriptors.get(attrib.getKey());
				return config != null && config.canEdit(attrib.getClasspathAttributeAccess());
			}
		}
		return true;
	}

	private boolean canRemove(List list) { 
		if (list.size() == 0) {
			return false;
		}
		for (int i= 0; i < list.size(); i++) {
			Object elem= list.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				if (attrib.isNonModifiable()) {
					return false;
				}
				if (attrib.isBuiltIn()) {
					if (attrib.getKey().equals(CPListElement.ACCESSRULES)) {
						return ((IAccessRule[]) attrib.getValue()).length > 0;
					}
					if (attrib.getValue() == null) {
						return false;
					}
				} else {
					ClasspathAttributeConfiguration config= fAttributeDescriptors.get(attrib.getKey());
					if (config == null || !config.canRemove(attrib.getClasspathAttributeAccess()) ) {
						return false;
					}
				}
			} else if (elem instanceof CPListElement) {
				// ok to remove
			} else if (elem instanceof CPUserLibraryElement) {
				// ok to remove
			} else { // unknown element
				return false;
			}
		}		
		return true;
	}

	private CPListElement[] openExtJSFileDialog(CPListElement existing, Object parent) {
		String lastUsedPath;
		if (existing != null) {
			lastUsedPath= existing.getPath().removeLastSegments(1).toOSString();
		} else {
			lastUsedPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (lastUsedPath == null) {
				lastUsedPath= ""; //$NON-NLS-1$
			}
		}
		String title= (existing == null) ? PreferencesMessages.UserLibraryPreferencePage_browsejar_new_title : PreferencesMessages.UserLibraryPreferencePage_browsejar_edit_title; 
		
		FileDialog dialog= new FileDialog(getShell(), existing == null ? SWT.MULTI : SWT.SINGLE);
		dialog.setText(title);
		dialog.setFilterExtensions(new String[] {"*.js"}); //$NON-NLS-1$
		dialog.setFilterPath(lastUsedPath);
		if (existing != null) {
			dialog.setFileName(existing.getPath().lastSegment());
		}
		
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
			
		IPath filterPath= Path.fromOSString(dialog.getFilterPath());
		CPListElement[] elems= new CPListElement[nChosen];
		for (int i= 0; i < nChosen; i++) {
			IPath path= filterPath.append(fileNames[i]).makeAbsolute();	
			CPListElement curr= new CPListElement(parent, null, IIncludePathEntry.CPE_LIBRARY, path, null);
			curr.setAttribute(CPListElement.JAVADOC, BuildPathSupport.guessJavadocLocation(curr));
			elems[i]= curr;
		}
		fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
		
		return elems;
	}
	
	private CPListElement[] openExtJsFolderDialog(final Object parent) {
		String lastUsedPath = fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
		if (lastUsedPath == null) {
			lastUsedPath= ""; //$NON-NLS-1$
		}
		
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
		dialog.setText(PreferencesMessages.UserLibraryPreferencePage_browsefolder_new_title);
		dialog.setMessage(PreferencesMessages.UserLibraryPreferencePage_browsefolder_new_title);
		dialog.setFilterPath(lastUsedPath);
		
		String result= dialog.open();
		if (result == null) {
			return null;
		}
		IPath filterPath= Path.fromOSString(dialog.getFilterPath());
		CPListElement[] elems= new CPListElement[1];
		CPListElement curr= new CPListElement(parent, null, IIncludePathEntry.CPE_LIBRARY, filterPath.makeAbsolute(), null);
		curr.setAttribute(CPListElement.JAVADOC, BuildPathSupport.guessJavadocLocation(curr));
		elems[0] = curr;
		
		fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, result);
				
		return elems;
	}

	
	private class UserLibraryAdapter implements ITreeListAdapter {
		
		private final Object[] EMPTY= new Object[0];

		public void customButtonPressed(TreeListDialogField field, int index) {
			doCustomButtonPressed(field, index);
		}

		public void selectionChanged(TreeListDialogField field) {
			doSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			doDoubleClicked(field);
		}

		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			doKeyPressed(field, event);
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof CPUserLibraryElement) {
				CPUserLibraryElement elem= (CPUserLibraryElement) element;
				return elem.getChildren();
			} else if (element instanceof CPListElement) {
				return ((CPListElement)element).getChildren();
			} else if (element instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute) element;
				if (CPListElement.ACCESSRULES.equals(attribute.getKey())) {
					return (IAccessRule[]) attribute.getValue();
				}
			}
			return EMPTY;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof CPListElementAttribute) {
				return ((CPListElementAttribute) element).getParent();
			} else if (element instanceof CPListElement) {
				return ((CPListElement) element).getParentContainer();
			} 
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return getChildren(field, element).length > 0;
		}
				
	}
}
