/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.IVisibilityChangeListener;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;

public class GenerateConstructorUsingFieldsSelectionDialog extends SourceActionDialog {

	class GenerateConstructorUsingFieldsTreeViewerAdapter implements ISelectionChangedListener, IDoubleClickListener {

		public void doubleClick(DoubleClickEvent event) {
			// Do nothing
		}

		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();

			List selectedList= selection.toList();
			GenerateConstructorUsingFieldsContentProvider cp= (GenerateConstructorUsingFieldsContentProvider) getContentProvider();

			fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX].setEnabled(cp.canMoveUp(selectedList));
			fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX].setEnabled(cp.canMoveDown(selectedList));
		}
	}

	private static final int DOWN_BUTTON= IDialogConstants.CLIENT_ID + 2;

	private static final int UP_BUTTON= IDialogConstants.CLIENT_ID + 1;

	protected Button[] fButtonControls;

	boolean[] fButtonsEnabled;

	IDialogSettings fGenConstructorSettings;

	int fHeight= 18;

	boolean fOmitSuper;

	Button fOmitSuperButton;

	IFunctionBinding[] fSuperConstructors;

	int fSuperIndex;

	GenerateConstructorUsingFieldsTreeViewerAdapter fTreeViewerAdapter;

	int fWidth= 60;

	final String OMIT_SUPER= "OmitCallToSuper"; //$NON-NLS-1$

	final String SETTINGS_SECTION= "GenerateConstructorUsingFieldsSelectionDialog"; //$NON-NLS-1$

	private static final int DOWN_INDEX= 1;

	private static final int UP_INDEX= 0;

	public GenerateConstructorUsingFieldsSelectionDialog(Shell parent, ILabelProvider labelProvider, GenerateConstructorUsingFieldsContentProvider contentProvider, CompilationUnitEditor editor, IType type, IFunctionBinding[] superConstructors) throws JavaScriptModelException {
		super(parent, labelProvider, contentProvider, editor, type, true);
		fTreeViewerAdapter= new GenerateConstructorUsingFieldsTreeViewerAdapter();

		fSuperConstructors= superConstructors;

		IDialogSettings dialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		fGenConstructorSettings= dialogSettings.getSection(SETTINGS_SECTION);
		if (fGenConstructorSettings == null) {
			fGenConstructorSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
			fGenConstructorSettings.put(OMIT_SUPER, false); 
		}

		fOmitSuper= fGenConstructorSettings.getBoolean(OMIT_SUPER);
	}

	Composite addSuperClassConstructorChoices(Composite composite) {
		Label label= new Label(composite, SWT.NONE);
		label.setText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_sort_constructor_choices_label); 
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);

		BindingLabelProvider provider= new BindingLabelProvider();
		final Combo combo= new Combo(composite, SWT.READ_ONLY);
		for (int i= 0; i < fSuperConstructors.length; i++) {
			combo.add(provider.getText(fSuperConstructors[i]));
		}

		// TODO: Can we be a little more intelligent about guessing the super() ?
		combo.setText(combo.getItem(0));
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				fSuperIndex= combo.getSelectionIndex();
				// Disable omit super checkbox unless default constructor
				fOmitSuperButton.setEnabled(getSuperConstructorChoice().getParameterTypes().length == 0);
				updateOKStatus();
			}
		});

		return composite;
	}

	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		switch (buttonId) {
			case UP_BUTTON: {
				GenerateConstructorUsingFieldsContentProvider contentProvider= (GenerateConstructorUsingFieldsContentProvider) getTreeViewer().getContentProvider();
				contentProvider.up(getElementList(), getTreeViewer());
				updateOKStatus();
				break;
			}
			case DOWN_BUTTON: {
				GenerateConstructorUsingFieldsContentProvider contentProvider= (GenerateConstructorUsingFieldsContentProvider) getTreeViewer().getContentProvider();
				contentProvider.down(getElementList(), getTreeViewer());
				updateOKStatus();
				break;
			}
		}
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.GENERATE_CONSTRUCTOR_USING_FIELDS_SELECTION_DIALOG);
	}
	
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		GridData gd= null;

		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);

		Composite classConstructorComposite= addSuperClassConstructorChoices(composite);
		gd= new GridData(GridData.FILL_BOTH);
		classConstructorComposite.setLayoutData(gd);

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout innerLayout= new GridLayout();
		innerLayout.numColumns= 2;
		innerLayout.marginHeight= 0;
		innerLayout.marginWidth= 0;
		inner.setLayout(innerLayout);

		Label messageLabel= createMessageArea(inner);
		if (messageLabel != null) {
			gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan= 2;
			messageLabel.setLayoutData(gd);
		}

		CheckboxTreeViewer treeViewer= createTreeViewer(inner);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(fWidth);
		gd.heightHint= convertHeightInCharsToPixels(fHeight);
		treeViewer.getControl().setLayoutData(gd);
		treeViewer.addSelectionChangedListener(fTreeViewerAdapter);

		Composite buttonComposite= createSelectionButtons(inner);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		buttonComposite.setLayoutData(gd);

		gd= new GridData(GridData.FILL_BOTH);
		inner.setLayoutData(gd);

		Composite entryComposite= createInsertPositionCombo(composite);
		entryComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite commentComposite= createCommentSelection(composite);
		commentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite overrideSuperComposite= createOmitSuper(composite);
		overrideSuperComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control linkControl= createLinkControl(composite);
		if (linkControl != null)
			linkControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		gd= new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		applyDialogFont(composite);

		return composite;
	}

	protected Composite createInsertPositionCombo(Composite composite) {
		Composite entryComposite= super.createInsertPositionCombo(composite);
		addVisibilityAndModifiersChoices(entryComposite);
		return entryComposite;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createLinkControl(Composite composite) {
		Link link= new Link(composite, SWT.WRAP);
		link.setText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_template_link_message); 
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openCodeTempatePage(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID);
			}
		});
		link.setToolTipText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_template_link_tooltip); 
		
		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= convertWidthInCharsToPixels(40); // only expand further if anyone else requires it
		link.setLayoutData(gridData);
		return link;
	}

	protected Composite createOmitSuper(Composite composite) {
		Composite omitSuperComposite= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		omitSuperComposite.setLayout(layout);

		fOmitSuperButton= new Button(omitSuperComposite, SWT.CHECK);
		fOmitSuperButton.setText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_omit_super); 
		fOmitSuperButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		fOmitSuperButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				boolean isSelected= (((Button) e.widget).getSelection());
				setOmitSuper(isSelected);
			}
		});
		fOmitSuperButton.setSelection(isOmitSuper());
		
		// Disable omit super checkbox unless default constructor and enum
		final boolean hasContructor= getSuperConstructorChoice().getParameterTypes().length == 0;
		fOmitSuperButton.setEnabled(hasContructor);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		fOmitSuperButton.setLayoutData(gd);

		return omitSuperComposite;
	}

	protected Composite createSelectionButtons(Composite composite) {
		Composite buttonComposite= super.createSelectionButtons(composite);

		GridLayout layout= new GridLayout();
		buttonComposite.setLayout(layout);

		createUpDownButtons(buttonComposite);

		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;

		return buttonComposite;
	}

	void createUpDownButtons(Composite buttonComposite) {
		int numButtons= 2; // up, down
		fButtonControls= new Button[numButtons];
		fButtonsEnabled= new boolean[numButtons];
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX]= createButton(buttonComposite, UP_BUTTON, ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_up_button, false); 
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX]= createButton(buttonComposite, DOWN_BUTTON, ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_down_button, false); 
		boolean defaultState= false;
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX].setEnabled(defaultState);
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX].setEnabled(defaultState);
		fButtonsEnabled[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX]= defaultState;
		fButtonsEnabled[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX]= defaultState;
	}

	protected Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
		int[] visibilities= availableVisibilities;
		return createVisibilityControl(parent, visibilityChangeListener, visibilities, correctVisibility);
	}

	List getElementList() {
		IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();
		List elements= selection.toList();
		ArrayList elementList= new ArrayList();

		for (int i= 0; i < elements.size(); i++) {
			elementList.add(elements.get(i));
		}
		return elementList;
	}

	public IFunctionBinding getSuperConstructorChoice() {
		return fSuperConstructors[fSuperIndex];
	}

	public boolean isOmitSuper() {
		return fOmitSuper;
	}

	public void setOmitSuper(boolean omitSuper) {
		if (fOmitSuper != omitSuper) {
			fOmitSuper= omitSuper;
			fGenConstructorSettings.put(OMIT_SUPER, omitSuper);
		}
	}
}
