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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PushDownRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor.MemberActionInfo;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

public final class PushDownWizard extends RefactoringWizard {

	private static class PushDownInputPage extends UserInputWizardPage {

		private static class MemberActionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {

			private static String getActionLabel(final int action) {
				switch (action) {
					case MemberActionInfo.NO_ACTION:
						return ""; //$NON-NLS-1$
					case MemberActionInfo.PUSH_ABSTRACT_ACTION:
						return RefactoringMessages.PushDownInputPage_leave_abstract;
					case MemberActionInfo.PUSH_DOWN_ACTION:
						return RefactoringMessages.PushDownInputPage_push_down;
					default:
						Assert.isTrue(false);
						return null;
				}
			}

			private static String[] getAvailableActionLabels(final MemberActionInfo info) {
				final int[] actions= info.getAvailableActions();
				final String[] result= new String[actions.length];
				for (int index= 0; index < actions.length; index++) {
					result[index]= getActionLabel(actions[index]);
				}
				return result;
			}

			private final ILabelProvider fLabelProvider= new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT | JavaScriptElementLabelProvider.SHOW_SMALL_ICONS);

			public void dispose() {
				fLabelProvider.dispose();
				super.dispose();
			}

			public Image getColumnImage(final Object element, final int index) {
				final MemberActionInfo info= (MemberActionInfo) element;
				switch (index) {
					case MEMBER_COLUMN:
						return fLabelProvider.getImage(info.getMember());
					case ACTION_COLUMN:
						return null;
					default:
						Assert.isTrue(false);
						return null;
				}
			}

			public String getColumnText(final Object element, final int index) {
				final MemberActionInfo info= (MemberActionInfo) element;
				switch (index) {
					case MEMBER_COLUMN:
						return fLabelProvider.getText(info.getMember());
					case ACTION_COLUMN:
						return getActionLabel(info.getAction());
					default:
						Assert.isTrue(false);
						return null;
				}
			}
		}

		private class PushDownCellModifier implements ICellModifier {

			public boolean canModify(final Object element, final String property) {
				if (!ACTION_PROPERTY.equals(property))
					return false;
				return ((MemberActionInfo) element).isEditable();
			}

			public Object getValue(final Object element, final String property) {
				if (!ACTION_PROPERTY.equals(property))
					return null;

				final MemberActionInfo info= (MemberActionInfo) element;
				return Integer.valueOf(info.getAction());
			}

			public void modify(final Object element, final String property, final Object value) {
				if (!ACTION_PROPERTY.equals(property))
					return;

				final int action= ((Integer) value).intValue();
				MemberActionInfo info;
				if (element instanceof Item) {
					info= (MemberActionInfo) ((Item) element).getData();
				} else
					info= (MemberActionInfo) element;
				if (!canModify(info, property))
					return;
				info.setAction(action);
				PushDownInputPage.this.updateWizardPage(null, true);
			}
		}

		private static final int ACTION_COLUMN= 1;

		private final static String ACTION_PROPERTY= "action"; //$NON-NLS-1$	

		private static final int MEMBER_COLUMN= 0;

		private final static String MEMBER_PROPERTY= "member"; //$NON-NLS-1$	

		private static final String PAGE_NAME= "PushDownInputPage"; //$NON-NLS-1$

		private static final int ROW_COUNT= 10;

		private static int countEditableInfos(final MemberActionInfo[] infos) {
			int result= 0;
			for (int index= 0; index < infos.length; index++) {
				if (infos[index].isEditable())
					result++;
			}
			return result;
		}

		private static void setInfoAction(final MemberActionInfo[] infos, final int action) {
			for (int index= 0; index < infos.length; index++) {
				infos[index].setAction(action);
			}
		}

		private Button fDeselectAllButton;

		private Button fEditButton;

		private Button fSelectAllButton;

		private Label fStatusLine;

		private PullPushCheckboxTableViewer fTableViewer;

		public PushDownInputPage() {
			super(PAGE_NAME);
		}

		private boolean areAllElementsMarkedAsNoAction() {
			return countInfosForAction(MemberActionInfo.NO_ACTION) == ((MemberActionInfo[]) fTableViewer.getInput()).length;
		}

		private boolean areAllElementsMarkedAsPushDownAction() {
			return countInfosForAction(MemberActionInfo.PUSH_DOWN_ACTION) == ((MemberActionInfo[]) fTableViewer.getInput()).length;
		}

		private void checkPageCompletionStatus(final boolean displayErrorMessage) {
			if (areAllElementsMarkedAsNoAction()) {
				if (displayErrorMessage)
					setErrorMessage(RefactoringMessages.PushDownInputPage_Select_members_to_push_down);
				setPageComplete(false);
			} else {
				setErrorMessage(null);
				setPageComplete(true);
			}
		}

		private int countInfosForAction(final int action) {
			final MemberActionInfo[] infos= (MemberActionInfo[]) fTableViewer.getInput();
			int count= 0;
			for (int index= 0; index < infos.length; index++) {
				final MemberActionInfo info= infos[index];
				if (info.getAction() == action)
					count++;
			}
			return count;
		}

		private void createButtonComposite(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
			final GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			composite.setLayout(layout);

			fSelectAllButton= new Button(composite, SWT.PUSH);
			fSelectAllButton.setText(RefactoringMessages.PullUpWizard_select_all_label);
			fSelectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fSelectAllButton.setEnabled(true);
			SWTUtil.setButtonDimensionHint(fSelectAllButton);
			fSelectAllButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					final IMember[] members= getMembers();
					setActionForMembers(members, MemberActionInfo.PUSH_DOWN_ACTION);
					updateWizardPage(null, true);
				}
			});

			fDeselectAllButton= new Button(composite, SWT.PUSH);
			fDeselectAllButton.setText(RefactoringMessages.PullUpWizard_deselect_all_label);
			fDeselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectAllButton.setEnabled(false);
			SWTUtil.setButtonDimensionHint(fDeselectAllButton);
			fDeselectAllButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					final IMember[] members= getMembers();
					setActionForMembers(members, MemberActionInfo.NO_ACTION);
					updateWizardPage(null, true);
				}
			});

			fEditButton= new Button(composite, SWT.PUSH);
			fEditButton.setText(RefactoringMessages.PushDownInputPage_Edit);
			final GridData data= new GridData(GridData.FILL_HORIZONTAL);
			data.verticalIndent= new PixelConverter(parent).convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			fEditButton.setLayoutData(data);
			fEditButton.setEnabled(false);
			SWTUtil.setButtonDimensionHint(fEditButton);
			fEditButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					PushDownInputPage.this.editSelectedMembers();
				}
			});

			final Button addButton= new Button(composite, SWT.PUSH);
			addButton.setText(RefactoringMessages.PushDownInputPage_Add_Required);
			addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			SWTUtil.setButtonDimensionHint(addButton);
			addButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					PushDownInputPage.this.markAdditionalRequiredMembersAsMembersToPushDown();
				}
			});
		}

		public void createControl(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());

			createMemberTableLabel(composite);
			createMemberTableComposite(composite);
			createStatusLine(composite);

			setControl(composite);
			Dialog.applyDialogFont(composite);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.PUSH_DOWN_WIZARD_PAGE);
		}

		private void createMemberTable(final Composite parent) {
			final TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NONE);
			layouter.addColumnData(new ColumnWeightData(60, true));
			layouter.addColumnData(new ColumnWeightData(40, true));

			final Table table= new Table(layouter, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
			table.setHeaderVisible(true);
			table.setLinesVisible(true);

			final GridData data= new GridData(GridData.FILL_BOTH);
			data.heightHint= SWTUtil.getTableHeightHint(table, ROW_COUNT);
			data.widthHint= convertWidthInCharsToPixels(30);
			layouter.setLayoutData(data);

			final TableLayout layout= new TableLayout();
			table.setLayout(layout);

			final TableColumn first= new TableColumn(table, SWT.NONE);
			first.setText(RefactoringMessages.PushDownInputPage_Member);

			final TableColumn second= new TableColumn(table, SWT.NONE);
			second.setText(RefactoringMessages.PushDownInputPage_Action);

			fTableViewer= new PullPushCheckboxTableViewer(table);
			fTableViewer.setUseHashlookup(true);
			fTableViewer.setContentProvider(new ArrayContentProvider());
			fTableViewer.setLabelProvider(new MemberActionInfoLabelProvider());
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					PushDownInputPage.this.updateButtonEnablementState((IStructuredSelection) event.getSelection());
				}
			});
			fTableViewer.addCheckStateListener(new ICheckStateListener() {

				public void checkStateChanged(final CheckStateChangedEvent event) {
					final boolean checked= event.getChecked();
					final MemberActionInfo info= (MemberActionInfo) event.getElement();
					if (checked)
						info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
					else
						info.setAction(MemberActionInfo.NO_ACTION);
					updateWizardPage(null, true);
				}
			});
			fTableViewer.addDoubleClickListener(new IDoubleClickListener() {

				public void doubleClick(final DoubleClickEvent event) {
					PushDownInputPage.this.editSelectedMembers();
				}
			});

			fTableViewer.setInput(getPushDownRefactoring().getPushDownProcessor().getMemberActionInfos());
			updateWizardPage(null, false);
			setupCellEditors(table);
		}

		private void createMemberTableComposite(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			final GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			composite.setLayout(layout);

			createMemberTable(composite);
			createButtonComposite(composite);
		}

		private void createMemberTableLabel(final Composite parent) {
			final Label label= new Label(parent, SWT.NONE);
			label.setText(RefactoringMessages.PushDownInputPage_Specify_actions);
			label.setLayoutData(new GridData());
		}

		private void createStatusLine(final Composite composite) {
			fStatusLine= new Label(composite, SWT.NONE);
			final GridData data= new GridData();
			data.horizontalSpan= 2;
			updateStatusLine();
			fStatusLine.setLayoutData(data);
		}

		// String -> Integer
		private Map createStringMappingForSelectedElements() {
			final Map result= new HashMap();
			int action= MemberActionInfo.PUSH_DOWN_ACTION;
			result.put(MemberActionInfoLabelProvider.getActionLabel(action), Integer.valueOf(action));
			int action1= MemberActionInfo.PUSH_ABSTRACT_ACTION;
			result.put(MemberActionInfoLabelProvider.getActionLabel(action1), Integer.valueOf(action1));
			return result;
		}

		private void editSelectedMembers() {
			if (!fEditButton.isEnabled())
				return;
			final ISelection preserved= fTableViewer.getSelection();
			try {
				final Map stringMapping= createStringMappingForSelectedElements();
				final String[] keys= (String[]) stringMapping.keySet().toArray(new String[stringMapping.keySet().size()]);
				Arrays.sort(keys);
				final int initialSelectionIndex= getInitialSelectionIndexForEditDialog(stringMapping, keys);

				final ComboSelectionDialog dialog= new ComboSelectionDialog(getShell(), RefactoringMessages.PushDownInputPage_Edit_members, RefactoringMessages.PushDownInputPage_Mark_selected_members, keys, initialSelectionIndex);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.CANCEL)
					return;
				final int action= ((Integer) stringMapping.get(dialog.getSelectedString())).intValue();
				setInfoAction(getSelectedMemberActionInfos(), action);
			} finally {
				updateWizardPage(preserved, true);
			}
		}

		private boolean enableEditButton(final IStructuredSelection selection) {
			if (selection.isEmpty() || selection.size() == 0)
				return false;
			return selection.size() == countEditableInfos(getSelectedMemberActionInfos());
		}

		private MemberActionInfo[] getActiveInfos() {
			final MemberActionInfo[] infos= getPushDownRefactoring().getPushDownProcessor().getMemberActionInfos();
			final List result= new ArrayList(infos.length);
			for (int index= 0; index < infos.length; index++) {
				final MemberActionInfo info= infos[index];
				if (info.isActive())
					result.add(info);
			}
			return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
		}

		private int getCommonActionCodeForSelectedInfos() {
			final MemberActionInfo[] infos= getSelectedMemberActionInfos();
			if (infos.length == 0)
				return -1;

			final int code= infos[0].getAction();
			for (int index= 0; index < infos.length; index++) {
				if (code != infos[index].getAction())
					return -1;
			}
			return code;
		}

		private int getInitialSelectionIndexForEditDialog(final Map mapping, final String[] keys) {
			final int commonActionCode= getCommonActionCodeForSelectedInfos();
			if (commonActionCode == -1)
				return 0;
			for (final Iterator iterator= mapping.keySet().iterator(); iterator.hasNext();) {
				final String key= (String) iterator.next();
				final int action= ((Integer) mapping.get(key)).intValue();
				if (commonActionCode == action) {
					for (int index= 0; index < keys.length; index++) {
						if (key.equals(keys[index]))
							return index;
					}
					Assert.isTrue(false);// there's no way
				}
			}
			return 0;
		}

		private IMember[] getMembers() {
			final MemberActionInfo[] infos= (MemberActionInfo[]) fTableViewer.getInput();
			final List result= new ArrayList(infos.length);
			for (int index= 0; index < infos.length; index++) {
				result.add(infos[index].getMember());
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		}

		private PushDownRefactoring getPushDownRefactoring() {
			return (PushDownRefactoring) getRefactoring();
		}

		private MemberActionInfo[] getSelectedMemberActionInfos() {
			Assert.isTrue(fTableViewer.getSelection() instanceof IStructuredSelection);
			final List result= ((IStructuredSelection) fTableViewer.getSelection()).toList();
			return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
		}

		public void markAdditionalRequiredMembersAsMembersToPushDown() {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

					public void run(final IProgressMonitor pm) throws InvocationTargetException {
						try {
							getPushDownRefactoring().getPushDownProcessor().computeAdditionalRequiredMembersToPushDown(pm);
							updateWizardPage(null, true);
						} catch (final JavaScriptModelException e) {
							throw new InvocationTargetException(e);
						} finally {
							pm.done();
						}
					}
				});
			} catch (final InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.PushDownInputPage_Push_Down, RefactoringMessages.PushDownInputPage_Internal_Error);
			} catch (final InterruptedException e) {
				Assert.isTrue(false);// not cancelable
			}
		}

		private void setActionForMembers(final IMember[] members, final int action) {
			final MemberActionInfo[] infos= (MemberActionInfo[]) fTableViewer.getInput();
			for (int offset= 0; offset < members.length; offset++) {
				for (int index= 0; index < infos.length; index++) {
					if (infos[index].getMember().equals(members[offset]))
						infos[index].setAction(action);
				}
			}
		}

		private void setupCellEditors(final Table table) {
			final ComboBoxCellEditor comboBoxCellEditor= new ComboBoxCellEditor();
			comboBoxCellEditor.setStyle(SWT.READ_ONLY);
			fTableViewer.setCellEditors(new CellEditor[] { null, comboBoxCellEditor});
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					if (comboBoxCellEditor.getControl() == null & !table.isDisposed())
						comboBoxCellEditor.create(table);
					Assert.isTrue(event.getSelection() instanceof IStructuredSelection);
					final IStructuredSelection ss= (IStructuredSelection) event.getSelection();
					if (ss.size() != 1)
						return;
					final MemberActionInfo mac= (MemberActionInfo) ss.getFirstElement();
					comboBoxCellEditor.setItems(MemberActionInfoLabelProvider.getAvailableActionLabels(mac));
					comboBoxCellEditor.setValue(Integer.valueOf(mac.getAction()));
				}
			});

			final ICellModifier cellModifier= new PushDownCellModifier();
			fTableViewer.setCellModifier(cellModifier);
			fTableViewer.setColumnProperties(new String[] { MEMBER_PROPERTY, ACTION_PROPERTY});
		}

		public void setVisible(final boolean visible) {
			super.setVisible(visible);
			if (visible) {
				fTableViewer.setSelection(new StructuredSelection(getActiveInfos()), true);
				fTableViewer.getControl().setFocus();
			}
		}

		private void updateButtonEnablementState(final IStructuredSelection tableSelection) {
			if (tableSelection == null || fEditButton == null)
				return;
			fEditButton.setEnabled(enableEditButton(tableSelection));
			if (fSelectAllButton != null)
				fSelectAllButton.setEnabled(!areAllElementsMarkedAsPushDownAction());
			if (fDeselectAllButton != null)
				fDeselectAllButton.setEnabled(!areAllElementsMarkedAsNoAction());
		}

		private void updateStatusLine() {
			if (fStatusLine == null)
				return;
			final int selected= fTableViewer.getCheckedElements().length;
			final String[] keys= { String.valueOf(selected)};
			final String msg= Messages.format(RefactoringMessages.PushDownInputPage_status_line, keys);
			fStatusLine.setText(msg);
		}

		private void updateWizardPage(final ISelection preserved, final boolean displayErrorMessage) {
			fTableViewer.refresh();
			if (preserved != null) {
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(preserved);
			}
			checkPageCompletionStatus(displayErrorMessage);
			updateButtonEnablementState(((IStructuredSelection) fTableViewer.getSelection()));
			updateStatusLine();
		}
	}

	public PushDownWizard(final PushDownRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.PushDownWizard_defaultPageTitle);
	}

	protected void addUserInputPages() {
		addPage(new PushDownInputPage());
	}
}
