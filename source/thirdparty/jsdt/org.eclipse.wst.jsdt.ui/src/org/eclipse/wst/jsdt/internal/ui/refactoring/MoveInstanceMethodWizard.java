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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refactoring wizard for the 'move instance method' refactoring.
 */
public final class MoveInstanceMethodWizard extends RefactoringWizard {

	/**
	 * The input wizard page of the 'move instance method' refactoring.
	 */
	public final class MoveInstanceMethodPage extends UserInputWizardPage {

		/** The page name */
		protected static final String PAGE_NAME= "MoveInstanceMethodPage"; //$NON-NLS-1$

		/** The "leave delegate" checkbox */
		protected Button fLeaveDelegateCheckBox= null;

		/** The "deprecate delegate" checkbox */
		protected Button fDeprecateDelegateCheckBox= null;

		/** The method name text field */
		protected Text fMethodNameField= null;

		/** The current method name status */
		protected RefactoringStatus fMethodNameStatus= new RefactoringStatus();

		/** The target name text field */
		protected Text fTargetNameField= null;

		/** The target name label */
		protected Label fTargetNameLabel= null;

		/** The current target name status */
		protected RefactoringStatus fTargetNameStatus= new RefactoringStatus();

		/** The current target type status */
		protected RefactoringStatus fTargetTypeStatus= new RefactoringStatus();

		/**
		 * Creates a new move instance method page.
		 */
		public MoveInstanceMethodPage() {
			super(PAGE_NAME);
		}

		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(final Composite parent) {
			Assert.isNotNull(parent);
			final Composite control= new Composite(parent, SWT.NONE);
			setControl(control);

			final GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			control.setLayout(layout);

			Label label= new Label(control, SWT.SINGLE);
			label.setText(Messages.format(RefactoringMessages.MoveInstanceMethodPage_New_receiver, JavaScriptElementLabels.getElementLabel(fProcessor.getMethod(), JavaScriptElementLabels.ALL_DEFAULT | JavaScriptElementLabels.M_PRE_RETURNTYPE | JavaScriptElementLabels.M_PRE_TYPE_PARAMETERS | JavaScriptElementLabels.M_PARAMETER_NAMES))); 

			GridData data= new GridData();
			data.horizontalSpan= 2;
			label.setLayoutData(data);

			final TableLayoutComposite composite= new TableLayoutComposite(control, SWT.NULL);
			composite.addColumnData(new ColumnWeightData(40, true));
			composite.addColumnData(new ColumnWeightData(60, true));

			final Table table= new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
			table.setHeaderVisible(true);
			table.setLinesVisible(false);

			TableColumn column= new TableColumn(table, SWT.NONE);
			column.setText(RefactoringMessages.MoveInstanceMethodPage_Name); 
			column.setResizable(true);

			column= new TableColumn(table, SWT.NONE);
			column.setText(RefactoringMessages.MoveInstanceMethodPage_Type); 
			column.setResizable(true);

			final TableViewer viewer= new TableViewer(table);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new TargetLabelProvider());

			final IVariableBinding[] candidateTargets= fProcessor.getCandidateTargets();
			viewer.setInput(candidateTargets);
			final IVariableBinding[] possibleTargets= fProcessor.getPossibleTargets();
			viewer.setSelection(new StructuredSelection(new Object[] { possibleTargets[0]}));

			viewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public final void selectionChanged(final SelectionChangedEvent event) {
					final Object element= ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (element instanceof IVariableBinding) {
						final IVariableBinding target= (IVariableBinding) element;
						final IVariableBinding[] targets= fProcessor.getPossibleTargets();
						boolean success= false;
						for (int index= 0; index < targets.length; index++) {
							if (Bindings.equals(target, targets[index])) {
								handleTargetChanged(target);
								success= true;
								break;
							}
						}
						if (!success)
							fTargetTypeStatus= RefactoringStatus.createWarningStatus(Messages.format(RefactoringMessages.MoveInstanceMethodPage_invalid_target, target.getName())); 
						else
							fTargetTypeStatus= new RefactoringStatus();
						handleStatusChanged();
					}
				}
			});

			data= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			data.heightHint= SWTUtil.getTableHeightHint(table, 7);
			data.horizontalSpan= 2;
			composite.setLayoutData(data);

			label= new Label(control, SWT.SINGLE);
			label.setText(RefactoringMessages.MoveInstanceMethodPage_Method_name); 
			label.setLayoutData(new GridData());

			fMethodNameField= new Text(control, SWT.SINGLE | SWT.BORDER);
			fMethodNameField.setText(fProcessor.getMethodName());
			fMethodNameField.selectAll();
			fMethodNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fMethodNameField.setFocus();
			fMethodNameField.addModifyListener(new ModifyListener() {

				public final void modifyText(final ModifyEvent event) {
					fMethodNameStatus= fProcessor.setMethodName(fMethodNameField.getText());
					handleStatusChanged();
				}
			});
			TextFieldNavigationHandler.install(fMethodNameField);

			fTargetNameLabel= new Label(control, SWT.SINGLE);
			fTargetNameLabel.setText(RefactoringMessages.MoveInstanceMethodPage_Target_name); 
			fTargetNameLabel.setLayoutData(new GridData());

			fTargetNameField= new Text(control, SWT.SINGLE | SWT.BORDER);
			final String name= fProcessor.getTargetName();
			if (name != null && name.length() > 0)
				fTargetNameField.setText(fProcessor.getTargetName());
			else {
				setPageComplete(RefactoringStatus.createInfoStatus(RefactoringCoreMessages.Checks_Choose_name)); 
				setPageComplete(false);
			}
			fTargetNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fTargetNameField.addModifyListener(new ModifyListener() {

				public final void modifyText(final ModifyEvent event) {
					fTargetNameStatus= fProcessor.setTargetName(fTargetNameField.getText());
					handleStatusChanged();
				}
			});
			TextFieldNavigationHandler.install(fTargetNameField);

			label= new Label(control, SWT.NONE);

			data= new GridData();
			data.horizontalSpan= 2;
			label.setLayoutData(data);

			fLeaveDelegateCheckBox= DelegateUIHelper.generateLeaveDelegateCheckbox(control, getRefactoring(), false);
			if (fLeaveDelegateCheckBox != null) {
				fDeprecateDelegateCheckBox= new Button(control, SWT.CHECK);
				data= new GridData();
				data.horizontalAlignment= GridData.FILL;
				data.horizontalIndent= (layout.marginWidth + fDeprecateDelegateCheckBox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
				data.horizontalSpan= 2;
				fDeprecateDelegateCheckBox.setLayoutData(data);
				fDeprecateDelegateCheckBox.setText(DelegateUIHelper.getDeprecateDelegateCheckBoxTitle());
				fDeprecateDelegateCheckBox.setSelection(DelegateUIHelper.loadDeprecateDelegateSetting(fProcessor));
				fProcessor.setDeprecateDelegates(fDeprecateDelegateCheckBox.getSelection());
				fDeprecateDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fProcessor.setDeprecateDelegates(fDeprecateDelegateCheckBox.getSelection());
					}
				});
				fDeprecateDelegateCheckBox.setEnabled(fLeaveDelegateCheckBox.getSelection());
				fLeaveDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fDeprecateDelegateCheckBox.setEnabled(fLeaveDelegateCheckBox.getSelection());
					}
				});
			}

			fProcessor.setInlineDelegator(!fLeaveDelegateCheckBox.getSelection());
			fProcessor.setRemoveDelegator(!fLeaveDelegateCheckBox.getSelection());

			handleTargetChanged(possibleTargets[0]);

			Dialog.applyDialogFont(control);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);
		}
		
		public void dispose() {
			DelegateUIHelper.saveLeaveDelegateSetting(fLeaveDelegateCheckBox);
			DelegateUIHelper.saveDeprecateDelegateSetting(fDeprecateDelegateCheckBox);
			super.dispose();
		}

		/**
		 * Handles the status changed event.
		 */
		protected final void handleStatusChanged() {
			final RefactoringStatus status= new RefactoringStatus();
			status.merge(fMethodNameStatus);
			status.merge(fTargetNameStatus);
			status.merge(fTargetTypeStatus);
			if (!fTargetTypeStatus.isOK())
				setPageComplete(false);
			else
				setPageComplete(status);
		}

		/**
		 * Handles the target changed event.
		 * 
		 * @param target the changed target
		 */
		protected final void handleTargetChanged(final IVariableBinding target) {
			Assert.isNotNull(target);
			fProcessor.setTarget(target);
			fTargetNameField.setEnabled(fProcessor.needsTargetNode());
			fTargetNameLabel.setEnabled(fProcessor.needsTargetNode());
		}
	}

	/**
	 * Table label provider for the target selection table.
	 */
	public static class TargetLabelProvider extends BindingLabelProvider implements ITableLabelProvider {

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(final Object element, final int column) {
			if (column == 0)
				return getImage(element);
			return null;
		}

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(final Object element, final int column) {
			final IVariableBinding binding= (IVariableBinding) element;
			switch (column) {
				case 0:
					return getText(binding);
				case 1:
					return getText(binding.getType());
				default:
					return null;
			}
		}
	}

	/** The default create delegator setting */
	protected static boolean DEFAULT_CREATE_DELEGATOR_SETTING= false;

	/** The default deprecate delegator setting */
	protected static boolean DEFAULT_DEPRECATE_DELEGATOR_SETTING= false;

	/** The associated move instance method processor */
	protected final MoveInstanceMethodProcessor fProcessor;

	/**
	 * Creates a new move instance method wizard.
	 * 
	 * @param refactoring the refactoring to host
	 */
	public MoveInstanceMethodWizard(final MoveInstanceMethodRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		fProcessor= refactoring.getMoveMethodProcessor();
		setDefaultPageTitle(RefactoringMessages.MoveInstanceMethodWizard_Move_Method); 
	}

	/*
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {
		addPage(new MoveInstanceMethodPage());
	}
}
