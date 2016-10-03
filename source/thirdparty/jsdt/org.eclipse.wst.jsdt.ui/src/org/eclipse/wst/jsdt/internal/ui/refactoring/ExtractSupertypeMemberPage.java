/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ExtractSupertypeRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Wizard page for the extract supertype refactoring, which, apart from pull up
 * facilities, also allows to specify the types where to extract the supertype.
 * 
 * 
 */
public final class ExtractSupertypeMemberPage extends PullUpMemberPage {

	/** Dialog to select supertypes */
	private static class SupertypeSelectionDialog extends SelectionDialog {

		/** The table viewer */
		private TableViewer fViewer;

		/**
		 * Creates a new supertype selection dialog.
		 * 
		 * @param shell
		 *            the parent shell
		 */
		public SupertypeSelectionDialog(final Shell shell) {
			super(shell);
		}

		/**
		 * {@inheritDoc}
		 */
		public void create() {
			setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
			super.create();
			getShell().setText(RefactoringMessages.ExtractSupertypeMemberPage_choose_type_caption);
		}

		/**
		 * {@inheritDoc}
		 */
		protected Control createDialogArea(final Composite composite) {
			Assert.isNotNull(composite);
			setMessage(RefactoringMessages.ExtractSupertypeMemberPage_choose_type_message);
			final Composite control= (Composite) super.createDialogArea(composite);
			createMessageArea(control);
			fViewer= new TableViewer(control, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			fViewer.setLabelProvider(createLabelProvider());
			fViewer.setContentProvider(new ArrayContentProvider());
			fViewer.setComparator(new SupertypeSelectionViewerSorter());
			fViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					setSelectionResult(((IStructuredSelection) fViewer.getSelection()).toArray());
				}
			});
			fViewer.addDoubleClickListener(new IDoubleClickListener() {

				public void doubleClick(final DoubleClickEvent event) {
					setSelectionResult(((IStructuredSelection) fViewer.getSelection()).toArray());
					close();
				}
			});
			final GridData data= new GridData(GridData.FILL_BOTH);
			data.heightHint= convertHeightInCharsToPixels(15);
			data.widthHint= convertWidthInCharsToPixels(55);
			fViewer.getTable().setLayoutData(data);
			applyDialogFont(control);
			return control;
		}

		/**
		 * Sets the input of this dialog.
		 * 
		 * @param object
		 *            the input
		 */
		public void setInput(final Object object) {
			fViewer.setInput(object);
		}
	}

	/** The label provider */
	private static class SupertypeSelectionLabelProvider extends AppearanceAwareLabelProvider implements ITableLabelProvider {

		/**
		 * Creates a new supertype selection label provider.
		 * 
		 * @param textFlags
		 *            the text flags
		 * @param imageFlags
		 *            the image flags
		 */
		public SupertypeSelectionLabelProvider(final long textFlags, final int imageFlags) {
			super(textFlags, imageFlags);
		}

		/**
		 * {@inheritDoc}
		 */
		public Image getColumnImage(final Object element, final int index) {
			return getImage(element);
		}

		/**
		 * {@inheritDoc}
		 */
		public String getColumnText(final Object element, final int index) {
			return getText(element);
		}
	}

	/** The viewer sorter */
	private static class SupertypeSelectionViewerSorter extends ViewerComparator {

		/**
		 * {@inheritDoc}
		 */
		public int compare(final Viewer viewer, final Object first, final Object second) {
			final IType predecessor= (IType) first;
			final IType successor= (IType) second;
			return getComparator().compare(predecessor.getElementName(), successor.getElementName());
		}
	}

	/**
	 * Creates a label provider for a type list.
	 * 
	 * @return a label provider
	 */
	private static ILabelProvider createLabelProvider() {
		return new SupertypeSelectionLabelProvider(JavaScriptElementLabels.T_TYPE_PARAMETERS | JavaScriptElementLabels.T_POST_QUALIFIED, JavaElementImageProvider.OVERLAY_ICONS);
	}

	/** The supertype name field */
	private Text fNameField;

	/** The table viewer */
	private TableViewer fTableViewer;

	/** The types to extract */
	private final Set fTypesToExtract= new HashSet(2);

	/**
	 * Creates a new extract supertype member page.
	 * 
	 * @param name
	 *            the page name
	 * @param page
	 *            the method page
	 */
	public ExtractSupertypeMemberPage(final String name, final ExtractSupertypeMethodPage page) {
		super(name, page);
		setDescription(RefactoringMessages.ExtractSupertypeMemberPage_page_title);
		METHOD_LABELS[PULL_UP_ACTION]= RefactoringMessages.ExtractSupertypeMemberPage_extract;
		METHOD_LABELS[DECLARE_ABSTRACT_ACTION]= RefactoringMessages.ExtractSupertypeMemberPage_declare_abstract;
		TYPE_LABELS[PULL_UP_ACTION]= RefactoringMessages.ExtractSupertypeMemberPage_extract;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void checkPageCompletionStatus(final boolean display) {
		final RefactoringStatus status= getProcessor().checkExtractedCompilationUnit();
		setMessage(null);
		if (display)
			setPageComplete(status);
		else
			setPageComplete(!status.hasFatalError());
		fSuccessorPage.fireSettingsChanged();
	}

	/**
	 * Computes the candidate types.
	 * 
	 * @throws InterruptedException
	 *             if the computation has been interrupted
	 */
	protected void computeCandidateTypes() throws InterruptedException {
		if (fCandidateTypes != null && fCandidateTypes.length > 0)
			return;
		try {
			getWizard().getContainer().run(true, true, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) throws InvocationTargetException {
					try {
						fCandidateTypes= getProcessor().getCandidateTypes(new RefactoringStatus(), monitor);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, getShell(), RefactoringMessages.ExtractSupertypeMemberPage_extract_supertype, RefactoringMessages.PullUpInputPage_exception);
		} catch (InterruptedException exception) {
			fCandidateTypes= new IType[0];
			throw new InterruptedException();
		} catch (OperationCanceledException exception) {
			fCandidateTypes= new IType[0];
		}
	}

	/**
	 * Creates the button composite.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createButtonComposite(final Composite parent) {
		final Composite buttons= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		final Button addButton= new Button(buttons, SWT.PUSH);
		addButton.setText(RefactoringMessages.ExtractSupertypeMemberPage_add_button_label);
		addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(addButton);
		addButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				try {
					computeCandidateTypes();
				} catch (InterruptedException exception) {
					return;
				}
				final LinkedList list= new LinkedList(Arrays.asList(fCandidateTypes));
				for (final Iterator outer= list.iterator(); outer.hasNext();) {
					final IType first= (IType) outer.next();
					for (final Iterator inner= fTypesToExtract.iterator(); inner.hasNext();) {
						final IType second= (IType) inner.next();
						if (second.getFullyQualifiedName().equals(first.getFullyQualifiedName()))
							outer.remove();
					}
				}
				final SupertypeSelectionDialog dialog= new SupertypeSelectionDialog(getShell());
				dialog.create();
				dialog.setInput(list.toArray());
				final int result= dialog.open();
				if (result == Window.OK) {
					final Object[] objects= dialog.getResult();
					if (objects != null && objects.length > 0) {
						for (int index= 0; index < objects.length; index++) {
							fTypesToExtract.add(objects[index]);
						}
						fTableViewer.setInput(fTypesToExtract.toArray());
						handleTypesChanged();
					}
				}
			}
		});

		final Button removeButton= new Button(buttons, SWT.PUSH);
		removeButton.setText(RefactoringMessages.ExtractSupertypeMemberPage_remove_button_label);
		removeButton.setEnabled(fCandidateTypes.length > 0 && !fTableViewer.getSelection().isEmpty());
		removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(removeButton);
		removeButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				final IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();
				if (!selection.isEmpty()) {
					final IType declaring= getDeclaringType();
					for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
						final Object element= iterator.next();
						if (!declaring.equals(element))
							fTypesToExtract.remove(element);
					}
					fTableViewer.setInput(fTypesToExtract.toArray());
					handleTypesChanged();
				}
			}
		});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(final SelectionChangedEvent event) {
				final IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();
				if (selection.isEmpty()) {
					removeButton.setEnabled(false);
					return;
				} else {
					final Object[] elements= selection.toArray();
					if (elements.length == 1 && elements[0].equals(getDeclaringType())) {
						removeButton.setEnabled(false);
						return;
					}
				}
				removeButton.setEnabled(true);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public void createControl(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);
		createSuperTypeField(composite);
		createSpacer(composite);
		createSuperTypeCheckbox(composite);
		createInstanceOfCheckbox(composite, layout.marginWidth);
		createStubCheckbox(composite);
		createSuperTypeControl(composite);
		createSpacer(composite);
		createMemberTableLabel(composite);
		createMemberTableComposite(composite);
		createStatusLine(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		initializeEnablement();
		initializeCheckboxes();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_SUPERTYPE_WIZARD_PAGE);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void createSuperTypeControl(final Composite parent) {
		try {
			createSuperTypeList(parent);
		} catch (JavaScriptModelException exception) {
			ExceptionHandler.handle(exception, getShell(), RefactoringMessages.ExtractSupertypeMemberPage_extract_supertype, RefactoringMessages.PullUpInputPage_exception);
		}
	}

	/**
	 * Creates the super type field.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createSuperTypeField(final Composite parent) {
		final Label label= new Label(parent, SWT.NONE);
		label.setText(RefactoringMessages.ExtractSupertypeMemberPage_name_label);
		label.setLayoutData(new GridData());

		fNameField= new Text(parent, SWT.BORDER);
		fNameField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				handleNameChanged(fNameField.getText());
			}
		});
		fNameField.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		TextFieldNavigationHandler.install(fNameField);
	}

	/**
	 * Creates the super type list.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createSuperTypeList(final Composite parent) throws JavaScriptModelException {
		createSpacer(parent);

		final Label label= new Label(parent, SWT.NONE);
		label.setText(RefactoringMessages.ExtractSupertypeMemberPage_types_list_caption);
		GridData data= new GridData();
		data.horizontalSpan= 2;
		label.setLayoutData(data);

		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
		data= new GridData(GridData.FILL_BOTH);
		data.horizontalSpan= 2;
		composite.setLayoutData(data);

		fTableViewer= new TableViewer(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		data= new GridData(GridData.FILL_BOTH);
		data.heightHint= SWTUtil.getTableHeightHint(fTableViewer.getTable(), 3);
		fTableViewer.getTable().setLayoutData(data);
		fTableViewer.setLabelProvider(createLabelProvider());
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setComparator(new JavaScriptElementComparator());
		fTypesToExtract.add(getDeclaringType());
		fTableViewer.setInput(fTypesToExtract.toArray());

		createButtonComposite(composite);
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getCreateStubsButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_create_stubs_label;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getDeclareAbstractActionLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_declare_abstract;
	}

	/**
	 * Returns the declaring type.
	 * 
	 * @return the declaring type
	 */
	public IType getDeclaringType() {
		return getProcessor().getDeclaringType();
	}

	/**
	 * {@inheritDoc}
	 */
	public IType getDestinationType() {
		return getProcessor().computeExtractedType(fNameField.getText());
	}

	/**
	 * Returns the extract supertype refactoring.
	 */
	public ExtractSupertypeRefactoring getExtractSuperTypeRefactoring() {
		return (ExtractSupertypeRefactoring) getRefactoring();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getInstanceofButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_use_supertype_label;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getNoMembersMessage() {
		return RefactoringMessages.ExtractSupertypeMemberPage_no_members_selected;
	}

	/**
	 * Returns the refactoring processor.
	 * 
	 * @return the refactoring processor
	 */
	protected ExtractSupertypeProcessor getProcessor() {
		return getExtractSuperTypeRefactoring().getExtractSupertypeProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getPullUpActionLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_extract;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getReplaceButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_use_instanceof_label;
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getTableRowCount() {
		return 6;
	}

	/**
	 * Handles the name changed event.
	 * 
	 * @param name
	 *            the name
	 */
	protected void handleNameChanged(final String name) {
		if (name != null)
			getProcessor().setTypeName(name);
		checkPageCompletionStatus(true);
	}

	/**
	 * Handles the types changed event.
	 */
	protected void handleTypesChanged() {
		getProcessor().setTypesToExtract((IType[]) fTypesToExtract.toArray(new IType[fTypesToExtract.size()]));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fNameField.setFocus();
			getProcessor().resetChanges();
		}
	}
}
