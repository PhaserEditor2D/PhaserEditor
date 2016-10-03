/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.HierarchyProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.IMemberActionInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

/**
 * Wizard page for pull up refactoring wizards which allows to specify the
 * actions on the members to pull up.
 * 
 * 
 */
public class PullUpMemberPage extends UserInputWizardPage {

	private class MemberActionCellModifier implements ICellModifier {

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
			Assert.isTrue(info.isMethodInfo());
			info.setAction(action);
			updateWizardPage(null, true);
		}
	}

	private class MemberActionInfo implements IMemberActionInfo {

		private static final int NO_ACTION= 2;

		private int fAction;

		private final IMember fMember;

		public MemberActionInfo(final IMember member, final int action) {
			Assert.isTrue((member instanceof IFunction) || (member instanceof IField) || (member instanceof IType));
			assertAction(member, action);
			fMember= member;
			fAction= action;
		}

		private void assertAction(final IMember member, final int action) {
			if (member instanceof IFunction) {
				try {
					Assert.isTrue(action != DECLARE_ABSTRACT_ACTION || !JdtFlags.isStatic(member));
				} catch (JavaScriptModelException e) {
					JavaScriptPlugin.log(e);
				}
				Assert.isTrue(action == NO_ACTION || action == DECLARE_ABSTRACT_ACTION || action == PULL_UP_ACTION);
			} else {
				Assert.isTrue(action == NO_ACTION || action == PULL_UP_ACTION);
			}
		}

		public int getAction() {
			return fAction;
		}

		public String getActionLabel() {
			switch (fAction) {
				case PULL_UP_ACTION:
					return getPullUpActionLabel();
				case DECLARE_ABSTRACT_ACTION:
					return getDeclareAbstractActionLabel();
				case NO_ACTION:
					return ""; //$NON-NLS-1$
				default:
					Assert.isTrue(false);
					return null;
			}
		}

		public String[] getAllowedLabels() {
			if (isFieldInfo())
				return new String[] { ""}; //$NON-NLS-1$
			else if (isMethodInfo())
				return METHOD_LABELS;
			else if (isTypeInfo())
				return TYPE_LABELS;
			else {
				Assert.isTrue(false);
				return null;
			}
		}

		public IMember getMember() {
			return fMember;
		}

		public boolean isActive() {
			return getAction() != NO_ACTION;
		}

		public boolean isEditable() {
			if (fAction == NO_ACTION)
				return false;
			if (!isMethodInfo())
				return false;
			final IFunction method= (IFunction) fMember;
			try {
				return !JdtFlags.isStatic(method);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
				return false;
			}
		}

		public boolean isFieldInfo() {
			return getMember() instanceof IField;
		}

		public boolean isMethodInfo() {
			return getMember() instanceof IFunction;
		}

		public boolean isTypeInfo() {
			return getMember() instanceof IType;
		}

		public void setAction(final int action) {
			assertAction(fMember, action);
			fAction= action;
		}
	}

	private static class MemberActionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {

		private final ILabelProvider fLabelProvider= new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT | JavaScriptElementLabelProvider.SHOW_SMALL_ICONS);

		public void dispose() {
			super.dispose();
			fLabelProvider.dispose();
		}

		public Image getColumnImage(final Object element, final int columnIndex) {
			final MemberActionInfo info= (MemberActionInfo) element;
			switch (columnIndex) {
				case MEMBER_COLUMN:
					return fLabelProvider.getImage(info.getMember());
				case ACTION_COLUMN:
					return null;
				default:
					Assert.isTrue(false);
					return null;
			}
		}

		public String getColumnText(final Object element, final int columnIndex) {
			final MemberActionInfo info= (MemberActionInfo) element;
			switch (columnIndex) {
				case MEMBER_COLUMN:
					return fLabelProvider.getText(info.getMember());
				case ACTION_COLUMN:
					return info.getActionLabel();
				default:
					Assert.isTrue(false);
					return null;
			}
		}
	}

	private static final int ACTION_COLUMN= 1;

	private static final String ACTION_PROPERTY= "action"; //$NON-NLS-1$	

	protected static final int DECLARE_ABSTRACT_ACTION= 1;

	private static final int MEMBER_COLUMN= 0;

	private static final String MEMBER_PROPERTY= "member"; //$NON-NLS-1$	

	protected static final int PULL_UP_ACTION= 0;

	private static final String SETTING_INSTANCEOF= "InstanceOf"; //$NON-NLS-1$

	private static final String SETTING_REPLACE= "Replace"; //$NON-NLS-1$

	private static int getEditableCount(final MemberActionInfo[] infos) {
		int result= 0;
		for (int i= 0; i < infos.length; i++) {
			final MemberActionInfo info= infos[i];
			if (info.isEditable())
				result++;
		}
		return result;
	}

	private static void putToStringMapping(final Map result, final String[] actionLabels, final int actionIndex) {
		result.put(actionLabels[actionIndex], Integer.valueOf(actionIndex));
	}

	private static void setActionForInfos(final MemberActionInfo[] infos, final int action) {
		for (int i= 0; i < infos.length; i++) {
			infos[i].setAction(action);
		}
	}

	private Button fAddButton;

	protected IType[] fCandidateTypes= {};

	private Button fCreateStubsButton;

	private Button fDeselectAllButton;

	private Button fEditButton;

	private Button fInstanceofButton;

	private Label fLabel;

	private Button fReplaceButton;

	private Button fSelectAllButton;

	private Label fStatusLine;

	protected final PullUpMethodPage fSuccessorPage;

	private Combo fSuperTypesCombo;

	private CheckboxTableViewer fTableViewer;

	protected final String[] METHOD_LABELS;

	protected final String[] TYPE_LABELS;

	public PullUpMemberPage(final String name, final PullUpMethodPage page) {
		super(name);
		fSuccessorPage= page;
		setDescription(RefactoringMessages.PullUpInputPage1_page_message);
		METHOD_LABELS= new String[2];
		METHOD_LABELS[PULL_UP_ACTION]= RefactoringMessages.PullUpInputPage1_pull_up;
		METHOD_LABELS[DECLARE_ABSTRACT_ACTION]= RefactoringMessages.PullUpInputPage1_declare_abstract;

		TYPE_LABELS= new String[1];
		TYPE_LABELS[PULL_UP_ACTION]= RefactoringMessages.PullUpInputPage1_pull_up;
	}

	private boolean areAllMembersMarkedAsPullUp() {
		return getMembersForAction(PULL_UP_ACTION).length == getTableInput().length;
	}

	protected boolean areAllMembersMarkedAsWithNoAction() {
		return getMembersForAction(MemberActionInfo.NO_ACTION).length == getTableInput().length;
	}

	private MemberActionInfo[] asMemberActionInfos() {
		final PullUpRefactoringProcessor processor= getPullUpRefactoring().getPullUpProcessor();
		final List toPullUp= Arrays.asList(processor.getMembersToMove());
		final IMember[] members= processor.getPullableMembersOfDeclaringType();
		final MemberActionInfo[] result= new MemberActionInfo[members.length];
		for (int i= 0; i < members.length; i++) {
			final IMember member= members[i];
			if (toPullUp.contains(member))
				result[i]= new MemberActionInfo(member, PULL_UP_ACTION);
			else
				result[i]= new MemberActionInfo(member, MemberActionInfo.NO_ACTION);
		}
		return result;
	}

	public boolean canFlipToNextPage() {
		if (getMethodsForAction(PULL_UP_ACTION).length == 0)
			return isPageComplete();
		return super.canFlipToNextPage();
	}

	private void checkAdditionalRequired() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

				public void run(final IProgressMonitor pm) throws InvocationTargetException {
					try {
						checkPullUp(getPullUpRefactoring().getPullUpProcessor().getAdditionalRequiredMembersToPullUp(pm), true);
					} catch (JavaScriptModelException e) {
						throw new InvocationTargetException(e);
					} finally {
						pm.done();
					}
				}
			});
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.PullUpInputPage_pull_Up, RefactoringMessages.PullUpInputPage_exception);
		} catch (InterruptedException e) {
			Assert.isTrue(false);
		}
	}

	protected void checkPageCompletionStatus(final boolean displayErrors) {
		if (areAllMembersMarkedAsWithNoAction()) {
			if (displayErrors)
				setErrorMessage(getNoMembersMessage());
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
		fSuccessorPage.fireSettingsChanged();
	}

	private void checkPullUp(final IMember[] elements, final boolean displayErrors) {
		setActionForMembers(elements, PULL_UP_ACTION);
		updateWizardPage(null, displayErrors);
	}

	private void createButtonComposite(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		final GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		composite.setLayout(gl);

		fSelectAllButton= new Button(composite, SWT.PUSH);
		fSelectAllButton.setText(RefactoringMessages.PullUpWizard_select_all_label);
		fSelectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fSelectAllButton.setEnabled(true);
		SWTUtil.setButtonDimensionHint(fSelectAllButton);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				final IMember[] members= getMembers();
				setActionForMembers(members, PULL_UP_ACTION);
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
		fEditButton.setText(RefactoringMessages.PullUpInputPage1_Edit);

		final GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.verticalIndent= new PixelConverter(parent).convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		fEditButton.setLayoutData(data);
		fEditButton.setEnabled(false);
		SWTUtil.setButtonDimensionHint(fEditButton);
		fEditButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				editSelectedMembers();
			}
		});

		fAddButton= new Button(composite, SWT.PUSH);
		fAddButton.setText(RefactoringMessages.PullUpInputPage1_Add_Required);
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				checkAdditionalRequired();
			}
		});
	}

	public void createControl(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);

		createSuperTypeControl(composite);
		createSpacer(composite);
		createSuperTypeCheckbox(composite);
		createInstanceOfCheckbox(composite, layout.marginWidth);
		createStubCheckbox(composite);
		createSpacer(composite);
		createMemberTableLabel(composite);
		createMemberTableComposite(composite);
		createStatusLine(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		initializeEnablement();
		initializeCheckboxes();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);
	}

	protected void createInstanceOfCheckbox(final Composite result, final int margin) {
		final HierarchyProcessor processor= getPullUpRefactoring().getPullUpProcessor();
		fInstanceofButton= new Button(result, SWT.CHECK);
		fInstanceofButton.setSelection(false);
		final GridData gd= new GridData();
		gd.horizontalIndent= (margin + fInstanceofButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
		gd.horizontalSpan= 2;
		fInstanceofButton.setLayoutData(gd);
		fInstanceofButton.setText(getInstanceofButtonLabel());
		processor.setInstanceOf(fInstanceofButton.getSelection());
		fInstanceofButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				processor.setInstanceOf(fInstanceofButton.getSelection());
			}
		});
		fReplaceButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				fInstanceofButton.setEnabled(fReplaceButton.getSelection());
			}
		});
	}

	private void createMemberTable(final Composite parent) {
		final TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(60, true));
		layouter.addColumnData(new ColumnWeightData(40, true));

		final Table table= new Table(layouter, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		final GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= SWTUtil.getTableHeightHint(table, getTableRowCount());
		gd.widthHint= convertWidthInCharsToPixels(30);
		layouter.setLayoutData(gd);

		final TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);

		final TableColumn column0= new TableColumn(table, SWT.NONE);
		column0.setText(RefactoringMessages.PullUpInputPage1_Member);

		final TableColumn column1= new TableColumn(table, SWT.NONE);
		column1.setText(RefactoringMessages.PullUpInputPage1_Action);

		fTableViewer= new PullPushCheckboxTableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setLabelProvider(new MemberActionInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(final SelectionChangedEvent event) {
				updateButtonEnablement(event.getSelection());
			}
		});
		fTableViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(final CheckStateChangedEvent event) {
				final boolean checked= event.getChecked();
				final MemberActionInfo info= (MemberActionInfo) event.getElement();
				if (checked)
					info.setAction(PULL_UP_ACTION);
				else
					info.setAction(MemberActionInfo.NO_ACTION);
				updateWizardPage(null, true);
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {
				editSelectedMembers();
			}
		});

		setTableInput();
		checkPullUp(getPullUpRefactoring().getPullUpProcessor().getMembersToMove(), false);
		setupCellEditors(table);
	}

	protected void createMemberTableComposite(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		final GridData data= new GridData(GridData.FILL_BOTH);
		data.horizontalSpan= 2;
		composite.setLayoutData(data);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		createMemberTable(composite);
		createButtonComposite(composite);
	}

	protected void createMemberTableLabel(final Composite parent) {
		fLabel= new Label(parent, SWT.NONE);
		fLabel.setText(RefactoringMessages.PullUpInputPage1_Specify_actions);
		final GridData data= new GridData();
		data.horizontalSpan= 2;
		fLabel.setLayoutData(data);
	}

	protected void createSpacer(final Composite parent) {
		final Label label= new Label(parent, SWT.NONE);
		final GridData data= new GridData();
		data.horizontalSpan= 2;
		data.heightHint= convertHeightInCharsToPixels(1) / 2;
		label.setLayoutData(data);
	}

	protected void createStatusLine(final Composite composite) {
		fStatusLine= new Label(composite, SWT.NONE);
		final GridData data= new GridData();
		data.horizontalSpan= 2;
		updateStatusLine();
		fStatusLine.setLayoutData(data);
	}

	// String -> Integer
	private Map createStringMappingForSelectedMembers() {
		final Map result= new HashMap();
		putToStringMapping(result, METHOD_LABELS, PULL_UP_ACTION);
		putToStringMapping(result, METHOD_LABELS, DECLARE_ABSTRACT_ACTION);
		return result;
	}

	protected void createStubCheckbox(final Composite parent) {
		fCreateStubsButton= new Button(parent, SWT.CHECK);
		fCreateStubsButton.setText(getCreateStubsButtonLabel());
		final GridData data= new GridData();
		data.horizontalSpan= 2;
		fCreateStubsButton.setLayoutData(data);
		fCreateStubsButton.setEnabled(false);
		fCreateStubsButton.setSelection(getPullUpRefactoring().getPullUpProcessor().getCreateMethodStubs());
	}

	protected void createSuperTypeCheckbox(final Composite parent) {
		fReplaceButton= new Button(parent, SWT.CHECK);
		fReplaceButton.setText(getReplaceButtonLabel());
		final GridData data= new GridData();
		data.horizontalSpan= 2;
		fReplaceButton.setLayoutData(data);
		fReplaceButton.setEnabled(true);
		fReplaceButton.setSelection(getPullUpRefactoring().getPullUpProcessor().isReplace());
	}

	private void createSuperTypeCombo(final IProgressMonitor pm, final Composite parent) throws JavaScriptModelException {
		final Label label= new Label(parent, SWT.NONE);
		label.setText(RefactoringMessages.PullUpInputPage1_Select_destination);
		label.setLayoutData(new GridData());

		fSuperTypesCombo= new Combo(parent, SWT.READ_ONLY);
		fCandidateTypes= getPullUpRefactoring().getPullUpProcessor().getCandidateTypes(new RefactoringStatus(), pm);
		if (fCandidateTypes.length > 0) {
			for (int i= 0; i < fCandidateTypes.length; i++) {
				final String comboLabel= JavaModelUtil.getFullyQualifiedName(fCandidateTypes[i]);
				fSuperTypesCombo.add(comboLabel);
			}
			fSuperTypesCombo.select(fCandidateTypes.length - 1);
			fSuperTypesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
	}

	protected void createSuperTypeControl(final Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) throws InvocationTargetException {
					try {
						createSuperTypeCombo(monitor, parent);
					} catch (JavaScriptModelException exception) {
						throw new InvocationTargetException(exception);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, getShell(), RefactoringMessages.PullUpInputPage_pull_Up, RefactoringMessages.PullUpInputPage_exception);
		} catch (InterruptedException exception) {
			Assert.isTrue(false);
		}
	}

	public void dispose() {
		fInstanceofButton= null;
		fReplaceButton= null;
		fTableViewer= null;
		super.dispose();
	}

	private void editSelectedMembers() {
		if (!fEditButton.isEnabled())
			return;

		final ISelection preserved= fTableViewer.getSelection();
		try {
			final String shellTitle= RefactoringMessages.PullUpInputPage1_Edit_members;
			final String labelText= RefactoringMessages.PullUpInputPage1_Mark_selected_members;
			final Map stringMapping= createStringMappingForSelectedMembers();
			final String[] keys= (String[]) stringMapping.keySet().toArray(new String[stringMapping.keySet().size()]);
			Arrays.sort(keys);
			final int initialSelectionIndex= getInitialSelectionIndexForEditDialog(stringMapping, keys);
			final ComboSelectionDialog dialog= new ComboSelectionDialog(getShell(), shellTitle, labelText, keys, initialSelectionIndex);
			dialog.setBlockOnOpen(true);
			if (dialog.open() == Window.CANCEL)
				return;
			final int action= ((Integer) stringMapping.get(dialog.getSelectedString())).intValue();
			setActionForInfos(getSelectedMembers(), action);
		} finally {
			updateWizardPage(preserved, true);
		}
	}

	private boolean enableEditButton(final IStructuredSelection ss) {
		if (ss.isEmpty() || ss.size() == 0)
			return false;
		return ss.size() == getEditableCount(getSelectedMembers());
	}

	private MemberActionInfo[] getActiveInfos() {
		final MemberActionInfo[] infos= getTableInput();
		final List result= new ArrayList(infos.length);
		for (int i= 0; i < infos.length; i++) {
			final MemberActionInfo info= infos[i];
			if (info.isActive())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private int getCommonActionCodeForSelectedInfos() {
		final MemberActionInfo[] infos= getSelectedMembers();
		if (infos.length == 0)
			return -1;

		final int code= infos[0].getAction();
		for (int i= 0; i < infos.length; i++) {
			if (code != infos[i].getAction())
				return -1;
		}
		return code;
	}

	protected String getCreateStubsButtonLabel() {
		return RefactoringMessages.PullUpInputPage1_Create_stubs;
	}

	protected String getDeclareAbstractActionLabel() {
		return RefactoringMessages.PullUpInputPage1_declare_abstract;
	}

	public IType getDestinationType() {
		final int index= fSuperTypesCombo.getSelectionIndex();
		if (index >= 0)
			return fCandidateTypes[index];
		return null;
	}

	private int getInitialSelectionIndexForEditDialog(final Map stringMapping, final String[] keys) {
		final int commonActionCode= getCommonActionCodeForSelectedInfos();
		if (commonActionCode == -1)
			return 0;
		for (final Iterator iter= stringMapping.keySet().iterator(); iter.hasNext();) {
			final String key= (String) iter.next();
			final int action= ((Integer) stringMapping.get(key)).intValue();
			if (commonActionCode == action) {
				for (int i= 0; i < keys.length; i++) {
					if (key.equals(keys[i]))
						return i;
				}
				Assert.isTrue(false);
			}
		}
		return 0;
	}

	protected String getInstanceofButtonLabel() {
		return RefactoringMessages.PullUpInputPage1_label_use_in_instanceof;
	}

	private IMember[] getMembers() {
		final MemberActionInfo[] infos= getTableInput();
		final List result= new ArrayList(infos.length);
		for (int index= 0; index < infos.length; index++) {
			result.add(infos[index].getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private IMember[] getMembersForAction(final int action) {
		final MemberActionInfo[] infos= getTableInput();
		final List result= new ArrayList(infos.length);
		for (int index= 0; index < infos.length; index++) {
			if (infos[index].getAction() == action)
				result.add(infos[index].getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private IFunction[] getMethodsForAction(final int action) {
		final MemberActionInfo[] infos= getTableInput();
		final List list= new ArrayList(infos.length);
		for (int index= 0; index < infos.length; index++) {
			if (infos[index].isMethodInfo() && infos[index].getAction() == action) {
				list.add(infos[index].getMember());
			}
		}
		return (IFunction[]) list.toArray(new IFunction[list.size()]);
	}

	public IWizardPage getNextPage() {
		initializeRefactoring();
		storeDialogSettings();
		if (getMethodsForAction(PULL_UP_ACTION).length == 0)
			return computeSuccessorPage();
		
		return super.getNextPage();
	}

	protected String getNoMembersMessage() {
		return RefactoringMessages.PullUpInputPage1_Select_members_to_pull_up;
	}

	protected String getPullUpActionLabel() {
		return RefactoringMessages.PullUpInputPage1_pull_up;
	}

	private PullUpRefactoring getPullUpRefactoring() {
		return (PullUpRefactoring) getRefactoring();
	}

	protected String getReplaceButtonLabel() {
		return RefactoringMessages.PullUpInputPage1_label_use_destination;
	}

	private MemberActionInfo[] getSelectedMembers() {
		Assert.isTrue(fTableViewer.getSelection() instanceof IStructuredSelection);
		final IStructuredSelection structured= (IStructuredSelection) fTableViewer.getSelection();
		final List result= structured.toList();
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private MemberActionInfo[] getTableInput() {
		return (MemberActionInfo[]) fTableViewer.getInput();
	}

	protected int getTableRowCount() {
		return 10;
	}

	private void initializeCheckBox(final Button checkbox, final String property, final boolean def) {
		final String s= JavaScriptPlugin.getDefault().getDialogSettings().get(property);
		if (s != null)
			checkbox.setSelection(new Boolean(s).booleanValue());
		else
			checkbox.setSelection(def);
	}

	protected void initializeCheckboxes() {
		initializeCheckBox(fReplaceButton, SETTING_REPLACE, true);
		initializeCheckBox(fInstanceofButton, SETTING_INSTANCEOF, false);
	}

	protected void initializeEnablement() {
		MemberActionInfo[] infos= asMemberActionInfos();
		final boolean enabled= infos.length > 0;
		fTableViewer.getTable().setEnabled(enabled);
		fStatusLine.setEnabled(enabled);
		fAddButton.setEnabled(enabled);
		fLabel.setEnabled(enabled);
	}

	private void initializeRefactoring() {
		final PullUpRefactoringProcessor processor= getPullUpRefactoring().getPullUpProcessor();
		processor.setMembersToMove(getMembersForAction(PULL_UP_ACTION));
		processor.setAbstractMethods(getMethodsForAction(DECLARE_ABSTRACT_ACTION));
		final IType destination= getDestinationType();
		if (destination != null)
			processor.setDestinationType(destination);
		processor.setCreateMethodStubs(fCreateStubsButton.getSelection());
		processor.setReplace(fReplaceButton.getSelection());
		processor.setInstanceOf(fInstanceofButton.getSelection());
		processor.setDeletedMethods(getMethodsForAction(PULL_UP_ACTION));
	}

	protected boolean performFinish() {
		initializeRefactoring();
		storeDialogSettings();
		return super.performFinish();
	}

	private void setActionForMembers(final IMember[] members, final int action) {
		final MemberActionInfo[] infos= getTableInput();
		for (int i= 0; i < members.length; i++) {
			for (int j= 0; j < infos.length; j++) {
				if (infos[j].getMember().equals(members[i]))
					infos[j].setAction(action);
			}
		}
	}

	private void setTableInput() {
		fTableViewer.setInput(asMemberActionInfos());
	}

	private void setupCellEditors(final Table table) {
		final ComboBoxCellEditor editor= new ComboBoxCellEditor();
		editor.setStyle(SWT.READ_ONLY);
		fTableViewer.setCellEditors(new CellEditor[] { null, editor});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(final SelectionChangedEvent event) {
				if (editor.getControl() == null & !table.isDisposed())
					editor.create(table);
				final ISelection sel= event.getSelection();
				if (!(sel instanceof IStructuredSelection))
					return;
				final IStructuredSelection structured= (IStructuredSelection) sel;
				if (structured.size() != 1)
					return;
				final MemberActionInfo info= (MemberActionInfo) structured.getFirstElement();
				editor.setItems(info.getAllowedLabels());
				editor.setValue(Integer.valueOf(info.getAction()));
			}
		});

		final ICellModifier cellModifier= new MemberActionCellModifier();
		fTableViewer.setCellModifier(cellModifier);
		fTableViewer.setColumnProperties(new String[] { MEMBER_PROPERTY, ACTION_PROPERTY});
	}

	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible) {
			try {
				getPullUpRefactoring().getPullUpProcessor().resetEnvironment();
			} finally {
				fTableViewer.setSelection(new StructuredSelection(getActiveInfos()), true);
				fTableViewer.getControl().setFocus();
			}
		}
	}

	private void storeDialogSettings() {
		final IDialogSettings settings= JavaScriptPlugin.getDefault().getDialogSettings();
		settings.put(SETTING_REPLACE, fReplaceButton.getSelection());
		settings.put(SETTING_INSTANCEOF, fInstanceofButton.getSelection());
	}

	private void updateButtonEnablement(final ISelection selection) {
		if (fEditButton != null)
			fEditButton.setEnabled(enableEditButton((IStructuredSelection) selection));
		fCreateStubsButton.setEnabled(getMethodsForAction(DECLARE_ABSTRACT_ACTION).length != 0);
		fInstanceofButton.setEnabled(fReplaceButton.getSelection());
		if (fSelectAllButton != null)
			fSelectAllButton.setEnabled(!areAllMembersMarkedAsPullUp());
		if (fDeselectAllButton != null)
			fDeselectAllButton.setEnabled(!areAllMembersMarkedAsWithNoAction());
	}

	private void updateStatusLine() {
		if (fStatusLine == null)
			return;
		final int selected= fTableViewer.getCheckedElements().length;
		final String[] keys= { String.valueOf(selected)};
		final String msg= Messages.format(RefactoringMessages.PullUpInputPage1_status_line, keys);
		fStatusLine.setText(msg);
	}

	private void updateWizardPage(final ISelection selection, final boolean displayErrors) {
		fTableViewer.refresh();
		if (selection != null) {
			fTableViewer.getControl().setFocus();
			fTableViewer.setSelection(selection);
		}
		checkPageCompletionStatus(displayErrors);
		updateButtonEnablement(fTableViewer.getSelection());
		updateStatusLine();
	}
}
