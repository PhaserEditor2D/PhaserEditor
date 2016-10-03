/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.wst.jsdt.ui.IJavaScriptElementSearchConstants;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;


/**
 * Configures the Content Assist > Static Members preference page.
 * 
 * 
 */
class CodeAssistFavoritesConfigurationBlock extends OptionsConfigurationBlock {
	
	
	private static class FavoriteStaticMemberInputDialog extends StatusDialog {
		
		private class StringButtonAdapter implements IDialogFieldListener, IStringButtonAdapter {
			/*
			 * @see IDialogFieldListener#dialogFieldChanged(DialogField)
			 */
			public void dialogFieldChanged(DialogField field) {
				doDialogFieldChanged(field);
			}			

			/*
			 * @see IStringButtonAdapter#changeControlPressed(DialogField)
			 */
			public void changeControlPressed(DialogField field) {
				doBrowseTypes();
			}
		}
		
		private StringButtonDialogField fNameDialogField;
		private List fExistingEntries;
		private final boolean fIsEditingMember;
			
		public FavoriteStaticMemberInputDialog(Shell parent, List existingEntries, boolean isEditingMember) {
			super(parent);
			fIsEditingMember= isEditingMember;
			setShellStyle(getShellStyle() | SWT.RESIZE);
			
			fExistingEntries= existingEntries;
			
			String label, title;
			if (isEditingMember) {
				title= PreferencesMessages.FavoriteStaticMemberInputDialog_member_title; 
				label= PreferencesMessages.FavoriteStaticMemberInputDialog_member_labelText; 
			} else {
				title= PreferencesMessages.FavoriteStaticMemberInputDialog_type_title; 
				label= PreferencesMessages.FavoriteStaticMemberInputDialog_type_labelText;
			}
			setTitle(title);

			StringButtonAdapter adapter= new StringButtonAdapter();

			fNameDialogField= new StringButtonDialogField(adapter);
			fNameDialogField.setLabelText(label); 
			fNameDialogField.setButtonLabel(PreferencesMessages.FavoriteStaticMemberInputDialog_browse_button); 
			fNameDialogField.setDialogFieldListener(adapter);
			fNameDialogField.setText(""); //$NON-NLS-1$
		}
			
		public void setInitialSelection(String editedEntry) {
			Assert.isNotNull(editedEntry);
			if (editedEntry.length() == 0)
				fNameDialogField.setText(""); //$NON-NLS-1$
			else
				fNameDialogField.setText(editedEntry);
		}
		
		public String getResult() {
			String val= fNameDialogField.getText();
			if (!fIsEditingMember)
				val= val + WILDCARD;
			return val;
		}
			
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			initializeDialogUnits(parent);
			
			GridLayout layout= (GridLayout) composite.getLayout();
			layout.numColumns= 2;
			
			fNameDialogField.doFillIntoGrid(composite, 3);
			
			fNameDialogField.getChangeControl(null).setVisible(!fIsEditingMember);
			
			LayoutUtil.setHorizontalSpan(fNameDialogField.getLabelControl(null), 2);
			
			int fieldWidthHint= convertWidthInCharsToPixels(60);
			Text text= fNameDialogField.getTextControl(null);
			LayoutUtil.setWidthHint(text, fieldWidthHint);
			LayoutUtil.setHorizontalGrabbing(text);
			LayoutUtil.setHorizontalSpan(text, fIsEditingMember ? 2 : 1);
			TextFieldNavigationHandler.install(text);
			
			DialogField.createEmptySpace(composite, 1);
			
			fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
			
			
			applyDialogFont(composite);		
			return composite;
		}
		
		private void doBrowseTypes() {		
			IRunnableContext context= new BusyIndicatorRunnableContext();
			IJavaScriptSearchScope scope= SearchEngine.createWorkspaceScope();
			int style= IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES;
			try {
				SelectionDialog dialog= JavaScriptUI.createTypeDialog(getShell(), context, scope, style, false, fNameDialogField.getText());
				dialog.setTitle(PreferencesMessages.FavoriteStaticMemberInputDialog_ChooseTypeDialog_title); 
				dialog.setMessage(PreferencesMessages.FavoriteStaticMemberInputDialog_ChooseTypeDialog_description); 
				if (dialog.open() == Window.OK) {
					IType res= (IType) dialog.getResult()[0];
					fNameDialogField.setText(res.getFullyQualifiedName('.'));
				}
			} catch (JavaScriptModelException e) {
				ExceptionHandler.handle(e, getShell(), PreferencesMessages.FavoriteStaticMemberInputDialog_ChooseTypeDialog_title, PreferencesMessages.FavoriteStaticMemberInputDialog_ChooseTypeDialog_error_message);  
			}
		}
		
		void doDialogFieldChanged(DialogField field) {
			doValidation();
		}
		
		private void doValidation() {
			StatusInfo status= new StatusInfo();
			String newText= fNameDialogField.getText();
			if (newText.length() == 0) {
				status.setError(""); //$NON-NLS-1$
			} else {
				IStatus val= JavaScriptConventions.validateJavaScriptTypeName(newText, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3);
				if (val.matches(IStatus.ERROR)) {
					if (fIsEditingMember) 
						status.setError(PreferencesMessages.FavoriteStaticMemberInputDialog_error_invalidMemberName);
					else
						status.setError(PreferencesMessages.FavoriteStaticMemberInputDialog_error_invalidTypeName);
				} else {
					if (doesExist(newText, fIsEditingMember)) {
						status.setError(PreferencesMessages.FavoriteStaticMemberInputDialog_error_entryExists); 
					}
				}
			}
			updateStatus(status);
		}
		
		private boolean doesExist(String name, boolean isStatic) {
			for (int i= 0; i < fExistingEntries.size(); i++) {
				String entry= (String) fExistingEntries.get(i);
				if (name.equals(entry)) {
					return true;
				}
			}
			return false;
		}
		

		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE);
		}

	}

	
	private static class ListLabelProvider extends LabelProvider {
		
		public final Image MEMBER_ICON;
		private final Image CLASS_ICON;

		public ListLabelProvider() {
			MEMBER_ICON= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, 0, JavaElementImageProvider.SMALL_SIZE);
			CLASS_ICON= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.SMALL_SIZE);
		}
		
		public Image getImage(Object element) {
			return ((String)element).endsWith(WILDCARD) ? CLASS_ICON : MEMBER_ICON;
		}

		public String getText(Object element) {
			return (String)element;
		}
	}
	
	
	private class ListAdapter implements IListAdapter, IDialogFieldListener {

		private boolean canEdit(ListDialogField field) {
			List selected= field.getSelectedElements();
			return selected.size() == 1;
		}

        public void customButtonPressed(ListDialogField field, int index) {
        	doButtonPressed(index);
        }

        public void selectionChanged(ListDialogField field) {
			fList.enableButton(IDX_EDIT, canEdit(field));
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
	
	
	private static final Key PREF_CODEASSIST_FAVORITE_STATIC_MEMBERS= getJDTUIKey(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
	
	private static final String WILDCARD= ".*"; //$NON-NLS-1$

	private static final int IDX_NEW_TYPE= 0;
	private static final int IDX_NEW_MEMBER= 1;
	private static final int IDX_EDIT= 2;
	private static final int IDX_REMOVE= 3;

	private ListDialogField fList;
	

	private static Key[] getAllKeys() {
		return new Key[] {
				PREF_CODEASSIST_FAVORITE_STATIC_MEMBERS
		};	
	}
	

	public CodeAssistFavoritesConfigurationBlock(IStatusChangeListener statusListener, IWorkbenchPreferenceContainer workbenchcontainer) {
		super(statusListener, null, getAllKeys(), workbenchcontainer);
	}

	protected Control createContents(Composite parent) {
		ScrolledPageContent scrolled= new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite control= new Composite(scrolled, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		control.setLayout(layout);
		
		createFavoriteList(control);
		
		initialize();
		
		scrolled.setContent(control);
		final Point size= control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(size.x, size.y);

		Dialog.applyDialogFont(scrolled);
		
		return scrolled;
	}
	
	private void createFavoriteList(Composite parent) {
		String[] buttonLabels= new String[] { 
				PreferencesMessages.CodeAssistStaticMembersConfigurationBlock_newType_button, 
				PreferencesMessages.CodeAssistStaticMembersConfigurationBlock_newMember_button, 
				PreferencesMessages.CodeAssistStaticMembersConfigurationBlock_edit_button, 
				PreferencesMessages.CodeAssistStaticMembersConfigurationBlock_remove_button
		};

		ListAdapter adapter= new ListAdapter();

		fList= new ListDialogField(adapter, buttonLabels, new ListLabelProvider());
		fList.setDialogFieldListener(adapter);
		fList.setLabelText(PreferencesMessages.CodeAssistStaticMembersConfigurationBlock_description); 
		fList.setRemoveButtonIndex(IDX_REMOVE);
		fList.enableButton(IDX_EDIT, false);
		fList.setViewerComparator(new ViewerComparator());
		
		PixelConverter pixelConverter= new PixelConverter(parent);
		
		fList.doFillIntoGrid(parent, 3);
		LayoutUtil.setHorizontalSpan(fList.getLabelControl(null), 2);
		LayoutUtil.setWidthHint(fList.getLabelControl(null), pixelConverter.convertWidthInCharsToPixels(60));
		LayoutUtil.setHorizontalGrabbing(fList.getListControl(null));
		
		Control listControl= fList.getListControl(null);
		GridData gd= (GridData)listControl.getLayoutData();
		gd.verticalAlignment= GridData.BEGINNING;
		gd.heightHint= pixelConverter.convertHeightInCharsToPixels(7);
	}
	
	public void initialize() {
		initializeFields();
	}

	private void initializeFields() {
		fList.setElements(Arrays.asList(getFavoriteStaticMembersPreference()));
 	}
	
    public void performDefaults() {
		super.performDefaults();
		initializeFields();
	}
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}
	
	protected void validateSettings(Key key, String oldValue, String newValue) {
		// no validation
	}

	protected void setControlEnabled(Key key, boolean enabled) {
		Control control= getControl(key);
		control.setEnabled(enabled);
		Label label= (Label) fLabels.get(control);
		if (label != null)
			label.setEnabled(enabled);
	}

	private Control getControl(Key key) {
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fTextBoxes.get(i);
			Key currKey= (Key) curr.getData();
			if (key.equals(currKey)) {
				return curr;
			}
		}
		return null;		
	}
	
	private void doButtonPressed(int index) {
		if (index == IDX_NEW_TYPE || index == IDX_NEW_MEMBER) { // add new
			List existing= fList.getElements();
			FavoriteStaticMemberInputDialog dialog= new FavoriteStaticMemberInputDialog(getShell(), existing, index == IDX_NEW_MEMBER);
			if (dialog.open() == Window.OK) {
				fList.addElement(dialog.getResult());
			}
		} else if (index == IDX_EDIT) { // edit
			List selected= fList.getSelectedElements();
			if (selected.isEmpty())
				return;

			String editedEntry= (String)selected.get(0);
			
			List existing= fList.getElements();
			existing.remove(editedEntry);
			boolean isType= editedEntry.endsWith(WILDCARD);
			FavoriteStaticMemberInputDialog dialog= new FavoriteStaticMemberInputDialog(getShell(), existing, !isType);
			if (isType)
				dialog.setInitialSelection(editedEntry.substring(0, editedEntry.length() - 2));
			else
				dialog.setInitialSelection(editedEntry);
			
			if (dialog.open() == Window.OK) {
				fList.replaceElement(editedEntry, dialog.getResult());
			}
		}
	}
	
	protected final void doDialogFieldChanged(DialogField field) {
		// set values in working copy
		if (field == fList)
	  		setValue(PREF_CODEASSIST_FAVORITE_STATIC_MEMBERS, serializeFavorites(fList.getElements()));
	}

	private String[] getFavoriteStaticMembersPreference() {
		String str= getValue(PREF_CODEASSIST_FAVORITE_STATIC_MEMBERS);
		if (str != null && str.length() > 0)
			return deserializeFavorites(str);
		return new String[0];
	}

	private static String[] deserializeFavorites(String str) {
		return str.split(";"); //$NON-NLS-1$
	}
	
	private static String serializeFavorites(List favorites) {
		int size= favorites.size();
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < size; i++) {
			buf.append((String)favorites.get(i));
			if (i < size -1)
				buf.append(';');
		}
		return buf.toString();
	}

}
