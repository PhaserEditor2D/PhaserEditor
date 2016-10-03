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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;

/**
 * 
 * Wizard page for displaying a tree of similarly named elements renamed along with a
 * type.
 * 
 * 
 * 
 */
class RenameTypeWizardSimilarElementsPage extends UserInputWizardPage {

	public static class EditElementDialog extends StatusDialog implements IDialogFieldListener {

		private StringDialogField fNameField;
		private IJavaScriptElement fElementToEdit;

		public EditElementDialog(Shell parent, IJavaScriptElement elementToEdit, String newName) {
			super(parent);
			setTitle(RefactoringMessages.RenameTypeWizardSimilarElementsPage_change_element_name);
			setShellStyle(getShellStyle() | SWT.RESIZE);

			fElementToEdit= elementToEdit;

			fNameField= new StringDialogField();
			fNameField.setDialogFieldListener(this);
			fNameField.setLabelText(RefactoringMessages.RenameTypeWizardSimilarElementsPage_enter_new_name);

			fNameField.setText(newName);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			final Composite composite= (Composite) super.createDialogArea(parent);
			LayoutUtil.doDefaultLayout(composite, new DialogField[] { fNameField }, true, SWT.DEFAULT, SWT.DEFAULT);
			fNameField.postSetFocusOnDialogField(parent.getDisplay());
			
			LayoutUtil.setWidthHint(fNameField.getLabelControl(null), convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH));
			Text text= fNameField.getTextControl(null);
			LayoutUtil.setHorizontalGrabbing(text);
			TextFieldNavigationHandler.install(text);

			Dialog.applyDialogFont(composite);
			return composite;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fNameField)
				updateStatus(validateSettings());
		}

		private IStatus validateSettings() {
			final String name= fNameField.getText();
			if (name.length() == 0) {
				return new StatusInfo(IStatus.ERROR, RefactoringMessages.RenameTypeWizardSimilarElementsPage_name_empty);
			}
			IStatus status= JavaScriptConventions.validateIdentifier(name);
			if (status.matches(IStatus.ERROR))
				return status;
			if (!Checks.startsWithLowerCase(name))
				return new StatusInfo(IStatus.WARNING, RefactoringMessages.RenameTypeWizardSimilarElementsPage_name_should_start_lowercase);

			if (fElementToEdit instanceof IMember && ((IMember) fElementToEdit).getDeclaringType() != null) {
				IType type= ((IMember) fElementToEdit).getDeclaringType();
				if (fElementToEdit instanceof IField) {
					final IField f= type.getField(name);
					if (f.exists())
						return new StatusInfo(IStatus.ERROR, RefactoringMessages.RenameTypeWizardSimilarElementsPage_field_exists);
				}
				if (fElementToEdit instanceof IFunction) {
					final IFunction m= type.getFunction(name, ((IFunction) fElementToEdit).getParameterTypes());
					if (m.exists())
						return new StatusInfo(IStatus.ERROR, RefactoringMessages.RenameTypeWizardSimilarElementsPage_method_exists);
				}
			}
			
			// cannot check local variables; no .getLocalVariable(String) in IMember

			return StatusInfo.OK_STATUS;
		}

		public String getNewName() {
			return fNameField.getText();
		}
	}

	private static class SimilarElementTreeContentProvider implements ITreeContentProvider {

		private Map/* <IJavaScriptElement,Set<IJavaScriptElement>> */fTreeElementMap;
		private Set/* <IJavaScriptUnit> */fTopLevelElements;

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			final Set children= (Set) fTreeElementMap.get(parentElement);
			if (children != null)
				return children.toArray();
			else
				return new Object[0];
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMember || element instanceof ILocalVariable) {
				return ((IJavaScriptElement) element).getParent();
			}
			if (element instanceof IJavaScriptUnit)
				return null;
			Assert.isTrue(false, "Should not get here"); //$NON-NLS-1$
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return fTreeElementMap.containsKey(element);
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			Assert.isTrue(inputElement == null || inputElement instanceof Map);
			return fTopLevelElements.toArray();
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fTreeElementMap.clear();
			fTreeElementMap= null;
			fTopLevelElements= null;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			Assert.isTrue(newInput == null || newInput instanceof Map);
			if (newInput == null)
				return;
			final Map similarElementsMap= (Map) newInput;
			final IJavaScriptElement[] similarElements= (IJavaScriptElement[]) similarElementsMap.keySet().toArray(new IJavaScriptElement[0]);
			fTreeElementMap= new HashMap();
			fTopLevelElements= new HashSet();
			for (int i= 0; i < similarElements.length; i++) {
				final IType declaring= (IType) similarElements[i].getAncestor(IJavaScriptElement.TYPE);
				if (similarElements[i] instanceof IMember) {
					// methods, fields, initializers, inner types
					addToMap(declaring, similarElements[i]);
				} else {
					// local variables
					final IJavaScriptElement parent= similarElements[i].getParent();
					if (parent instanceof IMember) {
						// parent is a method or an initializer
						addToMap(parent, similarElements[i]);
						addToMap(declaring, parent);
					}
				}
				handleDeclaring(declaring);
			}
		}

		private void handleDeclaring(final IJavaScriptElement someType) {

			final IJavaScriptElement enclosing= someType.getParent();
			if (enclosing instanceof IJavaScriptUnit) {
				fTopLevelElements.add(someType.getParent());
				addToMap(someType.getParent(), someType);
			} else {
				addToMap(enclosing, someType);
				handleDeclaring(enclosing);
			}
		}

		private void addToMap(final IJavaScriptElement key, final IJavaScriptElement element) {
			Set elements= (Set) fTreeElementMap.get(key);
			if (elements == null) {
				elements= new HashSet();
				fTreeElementMap.put(key, elements);
			}
			elements.add(element);
		}

	}

	private static class SimilarLabelProvider extends JavaScriptElementLabelProvider {

		private Map fDescriptorImageMap= new HashMap();
		private Map fElementToNewName;

		public SimilarLabelProvider() {
			super(JavaScriptElementLabelProvider.SHOW_DEFAULT | JavaScriptElementLabelProvider.SHOW_SMALL_ICONS);
		}

		public void initialize(Map elementToNewName) {
			this.fElementToNewName= elementToNewName;
		}

		public void dispose() {
			for (Iterator iter= fDescriptorImageMap.values().iterator(); iter.hasNext();) {
				Image image= (Image) iter.next();
				image.dispose();
			}
			super.dispose();
		}

		private Image manageImageDescriptor(ImageDescriptor descriptor) {
			Image image= (Image) fDescriptorImageMap.get(descriptor);
			if (image == null) {
				image= descriptor.createImage();
				fDescriptorImageMap.put(descriptor, image);
			}
			return image;
		}

		public Image getImage(Object element) {
			if (isSimilarElement(element))
				return manageImageDescriptor(JavaPluginImages.DESC_OBJS_DEFAULT_CHANGE);
			return super.getImage(element);
		}

		public Image getJavaImage(Object element) {
			return super.getImage(element);
		}

		public String getText(Object element) {
			if (isSimilarElement(element)) {
				return Messages.format(RefactoringMessages.RenameTypeWizardSimilarElementsPage_rename_to, new String[] { super.getText(element), (String)fElementToNewName.get(element) } ); 
			}
			return super.getText(element);
		}
		
		private boolean isSimilarElement(Object element) {
			return fElementToNewName.containsKey(element);
		}
		
	}
	
	private static class SimilarElementComparator extends JavaScriptElementComparator {

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.ui.JavaElementSorter#category(java.lang.Object)
		 */
		public int category(Object element) {

			/*
			 * We'd like to present the elements in the same order as they
			 * appear in the source. This can be achieved by assigning a
			 * distinct category to every element; the category being derived
			 * from the source position of the element.
			 */

			ISourceRange sourceRange= null;
			if (element instanceof IMember) {
				IMember member= (IMember) element;
				try {
					sourceRange= member.getNameRange();
				} catch (JavaScriptModelException e) {
					// fall through
				}
			}
			if (element instanceof ILocalVariable) {
				ILocalVariable var= (ILocalVariable) element;
				sourceRange= var.getNameRange();
			}

			if (sourceRange != null)
				return 100 + sourceRange.getOffset(); // +100: safe distance from all other categories.

			return super.category(element);
		}
	}

	public static final String PAGE_NAME= "SimilarElementSelectionPage"; //$NON-NLS-1$

	private final long LABEL_FLAGS= JavaScriptElementLabels.DEFAULT_QUALIFIED | JavaScriptElementLabels.ROOT_POST_QUALIFIED | JavaScriptElementLabels.APPEND_ROOT_PATH | JavaScriptElementLabels.M_PARAMETER_TYPES
			| JavaScriptElementLabels.M_PARAMETER_NAMES | JavaScriptElementLabels.M_APP_RETURNTYPE | JavaScriptElementLabels.M_EXCEPTIONS | JavaScriptElementLabels.F_APP_TYPE_SIGNATURE | JavaScriptElementLabels.T_TYPE_PARAMETERS;

	private Label fSimilarElementsLabel;
	private SourceViewer fSourceViewer;
	private ContainerCheckedTreeViewer fTreeViewer;
	private SimilarLabelProvider fTreeViewerLabelProvider;
	private Map fSimilarElementsToNewName;
	private Button fEditElementButton;
	private boolean fWasInitialized;
	private CLabel fCurrentElementLabel;

	public RenameTypeWizardSimilarElementsPage() {
		super(PAGE_NAME);
	}

	// --- UI creation

	public void createControl(Composite parent) {

		ViewForm viewForm= new ViewForm(parent, SWT.BORDER | SWT.FLAT);

		Composite inner= new Composite(viewForm, SWT.NULL);
		GridLayout layout= new GridLayout();
		inner.setLayout(layout);

		createTreeAndSourceViewer(inner);
		createButtonComposite(inner);
		viewForm.setContent(inner);

		setControl(viewForm);

		Dialog.applyDialogFont(viewForm);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE);
	}

	private void createTreeAndSourceViewer(Composite superComposite) {
		SashForm composite= new SashForm(superComposite, SWT.HORIZONTAL);
		initializeDialogUnits(superComposite);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(20);
		gd.widthHint= convertWidthInCharsToPixels(10);
		composite.setLayoutData(gd);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		createSimilarElementTreeComposite(composite);
		createSourceViewerComposite(composite);
		composite.setWeights(new int[] { 50, 50 });
	}

	private void createSimilarElementTreeComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		createTypeHierarchyLabel(composite);
		createTreeViewer(composite);
	}

	private void createTypeHierarchyLabel(Composite composite) {
		fSimilarElementsLabel= new Label(composite, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= JavaElementImageProvider.SMALL_SIZE.x;
		fSimilarElementsLabel.setLayoutData(gd);
		fSimilarElementsLabel.setText(RefactoringMessages.RenameTypeWizardSimilarElementsPage_review_similar_elements);
	}

	private void createTreeViewer(Composite composite) {
		Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTreeViewer= new ContainerCheckedTreeViewer(tree);
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setComparator(new SimilarElementComparator());
		fTreeViewer.setContentProvider(new SimilarElementTreeContentProvider());
		fTreeViewerLabelProvider= new SimilarLabelProvider();
		fTreeViewer.setLabelProvider(fTreeViewerLabelProvider);
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				RenameTypeWizardSimilarElementsPage.this.treeViewerSelectionChanged(event);
			}
		});
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				RenameTypeWizardSimilarElementsPage.this.editCurrentElement();
			}
		});
	}

	private void createSourceViewerComposite(Composite parent) {
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);

		createSourceViewerLabel(c);
		createSourceViewer(c);
	}

	private void createSourceViewerLabel(Composite c) {
		fCurrentElementLabel= new CLabel(c, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= JavaElementImageProvider.SMALL_SIZE.x;
		fCurrentElementLabel.setText(RefactoringMessages.RenameTypeWizardSimilarElementsPage_select_element_to_view_source);
		fCurrentElementLabel.setLayoutData(gd);
	}

	private void createSourceViewer(Composite c) {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		fSourceViewer= new JavaSourceViewer(c, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION, store);
		fSourceViewer.configure(new JavaScriptSourceViewerConfiguration(getJavaTextTools().getColorManager(), store, null, null));
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		Document document= new Document();
		getJavaTextTools().setupJavaDocumentPartitioner(document);
		fSourceViewer.setDocument(document);
	}

	private static JavaScriptTextTools getJavaTextTools() {
		return JavaScriptPlugin.getDefault().getJavaTextTools();
	}

	private void createButtonComposite(Composite superComposite) {
		Composite buttonComposite= new Composite(superComposite, SWT.NONE);
		buttonComposite.setLayoutData(new GridData());
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		buttonComposite.setLayout(layout);

		Button returnToDefaults= new Button(buttonComposite, SWT.PUSH);
		returnToDefaults.setText(RefactoringMessages.RenameTypeWizardSimilarElementsPage_restore_defaults);
		returnToDefaults.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(returnToDefaults);
		returnToDefaults.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardSimilarElementsPage.this.resetDataInRefAndUI();
			}
		});
		fEditElementButton= new Button(buttonComposite, SWT.PUSH);
		fEditElementButton.setText(RefactoringMessages.RenameTypeWizardSimilarElementsPage_change_name);
		fEditElementButton.setLayoutData(new GridData());
		fEditElementButton.setEnabled(false);
		SWTUtil.setButtonDimensionHint(fEditElementButton);
		fEditElementButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardSimilarElementsPage.this.editCurrentElement();
			}
		});
	}


	// ---------------------- Acting

	public void setVisible(boolean visible) {
		if (visible) {
			initializeUIFromRef();
		}
		super.setVisible(visible);
		selectFirstElement();
	}

	private void selectFirstElement() {
		if (fTreeViewer.getTree().getItemCount() > 0) {
			TreeItem item= fTreeViewer.getTree().getItem(0);
			if (item.getData() != null) {
				fTreeViewer.reveal(item.getData());
				Object data= getFirstSimilarElement(item);
				if (data != null) {
					fTreeViewer.setSelection(new StructuredSelection(data));
				}
			}
		}
		fTreeViewer.getTree().setFocus();
	}
	
	private Object getFirstSimilarElement(TreeItem item) {
		Object data= item.getData();
		if (isSimilarElement(data)) {
			return data;
		} else {
			TreeItem[] children= item.getItems();
			for (int i= 0; i < children.length; i++) {
				Object childData= getFirstSimilarElement(children[i]);
				if (childData != null)
					return childData;
			}
		}
		return null;
	}

	private void initializeUIFromRef() {
		// Get data from the refactoring
		final Map elementsToNewNames= getRenameTypeProcessor().getSimilarElementsToNewNames();
		try {
			// To prevent flickering, stop redrawing
			getShell().setRedraw(false);
			if (fSimilarElementsToNewName == null || elementsToNewNames != fSimilarElementsToNewName) {
				fSimilarElementsToNewName= elementsToNewNames;
				fTreeViewerLabelProvider.initialize(fSimilarElementsToNewName);
				fTreeViewer.setInput(fSimilarElementsToNewName);
			}
			fTreeViewer.expandAll();
			restoreSelectionAndNames(getRenameTypeProcessor().getSimilarElementsToSelection());
		} finally {
			getShell().setRedraw(true);
		}		
		fWasInitialized= true;
	}

	private void initializeRefFromUI() {
		IJavaScriptElement[] selected= getCheckedSimilarElements();
		Map selection= getRenameTypeProcessor().getSimilarElementsToSelection();
		for (Iterator iter= selection.keySet().iterator(); iter.hasNext();) {
			IJavaScriptElement element= (IJavaScriptElement) iter.next();
			selection.put(element, Boolean.FALSE);
		}
		for (int i= 0; i < selected.length; i++)
			selection.put(selected[i], Boolean.TRUE);

	}

	private void resetDataInRefAndUI() {
		getRenameTypeProcessor().resetSelectedSimilarElements();
		restoreSelectionAndNames(getRenameTypeProcessor().getSimilarElementsToSelection());
	}

	protected void editCurrentElement() {
		IStructuredSelection selection= (IStructuredSelection) fTreeViewer.getSelection();
		if ( (selection != null) && isSimilarElement(selection.getFirstElement())) {
			IJavaScriptElement element= (IJavaScriptElement) selection.getFirstElement();
			String newName= (String) fSimilarElementsToNewName.get(element);
			if (newName == null)
				return;
			EditElementDialog dialog= new EditElementDialog(getShell(), element, newName);
			if (dialog.open() == Window.OK) {
				String changedName= dialog.getNewName();
				if (!changedName.equals(newName)) {
					fSimilarElementsToNewName.put(element, changedName);
					fTreeViewer.update(element, null);
				}
			}
		}
	}

	private void restoreSelectionAndNames(final Map selection) {
		final Map selectedElements= selection;
		for (Iterator iter= selectedElements.keySet().iterator(); iter.hasNext();) {
			IJavaScriptElement element= (IJavaScriptElement) iter.next();
			boolean isSelected= ((Boolean) selectedElements.get(element)).booleanValue();
			fTreeViewer.setChecked(element, isSelected);
			fTreeViewer.update(element, null);
		}
	}

	// ------------ Navigation

	/*
	 * @see IWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		if (fWasInitialized)
			initializeRefFromUI();
		// computes the input successor page
		// (=create changes)
		IWizardPage nextPage= super.getNextPage();
		if (nextPage != this) // if user pressed cancel, then the next page is the current page
			nextPage.setPreviousPage(this);
		return nextPage;
	}


	/*
	 * @see IWizardPage#getPreviousPage()
	 */
	public IWizardPage getPreviousPage() {
		if (fWasInitialized)
			initializeRefFromUI();
		return super.getPreviousPage();
	}

	/*
	 * @see RefactoringWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		initializeRefFromUI();
		return super.performFinish();
	}

	// ------------ Helper

	private boolean isSimilarElement(Object element) {
		if (!fWasInitialized)
			return false;
		
		return fSimilarElementsToNewName.containsKey(element);
	}

	private void treeViewerSelectionChanged(SelectionChangedEvent event) {
		try {
			final IJavaScriptElement selection= getFirstSelectedSourceReference(event);
			setSourceViewerContents(selection);
			fEditElementButton.setEnabled(selection != null && (isSimilarElement(selection)));
			fCurrentElementLabel.setText(selection != null ? JavaScriptElementLabels.getElementLabel(selection, LABEL_FLAGS) : RefactoringMessages.RenameTypeWizardSimilarElementsPage_select_element_to_view_source);
			fCurrentElementLabel.setImage(selection != null ? fTreeViewerLabelProvider.getJavaImage(selection) : null);
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.RenameTypeWizard_defaultPageTitle, RefactoringMessages.RenameTypeWizard_unexpected_exception);
		}
	}

	private IJavaScriptElement getFirstSelectedSourceReference(SelectionChangedEvent event) {
		ISelection s= event.getSelection();
		if (! (s instanceof IStructuredSelection))
			return null;
		IStructuredSelection strSel= (IStructuredSelection) s;
		if (strSel.size() != 1)
			return null;
		Object first= strSel.getFirstElement();
		if (! (first instanceof IJavaScriptElement))
			return null;
		return (IJavaScriptElement) first;
	}

	private void setSourceViewerContents(IJavaScriptElement el) throws JavaScriptModelException {
		String EMPTY= ""; //$NON-NLS-1$
		if (el == null) {
			fSourceViewer.getDocument().set(EMPTY);
			return;
		}
		IJavaScriptUnit element= (IJavaScriptUnit) el.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (element == null) {
			fSourceViewer.getDocument().set(EMPTY);
			return;
		}
		
		String contents= element.getSource();
		try {
			fSourceViewer.setRedraw(false);
			fSourceViewer.getDocument().set(contents == null ? EMPTY : contents);
			ISourceRange sr= getNameRange(el);
			if (sr != null) {
				fSourceViewer.setSelectedRange(sr.getOffset(), sr.getLength());
			}
		} finally {
			fSourceViewer.setRedraw(true);
		}		
	}

	private ISourceRange getNameRange(IJavaScriptElement element) throws JavaScriptModelException {
		if (element instanceof IMember)
			return ((IMember) element).getNameRange();
		else if (element instanceof ILocalVariable)
			return ((ILocalVariable) element).getNameRange();
		else
			return null;
	}

	private IJavaScriptElement[] getCheckedSimilarElements() {
		Object[] checked= fTreeViewer.getCheckedElements();
		List elements= new ArrayList(checked.length);
		for (int i= 0; i < checked.length; i++) {
			if (isSimilarElement(checked[i]))
				elements.add(checked[i]);
		}
		return (IJavaScriptElement[]) elements.toArray(new IJavaScriptElement[elements.size()]);
	}

	public RenameTypeProcessor getRenameTypeProcessor() {
		RefactoringProcessor proc= ((RenameRefactoring) getRefactoring()).getProcessor();
		if (proc instanceof RenameTypeProcessor)
			return (RenameTypeProcessor) proc;
		else if (proc instanceof RenameCompilationUnitProcessor) {
			RenameCompilationUnitProcessor rcu= (RenameCompilationUnitProcessor) proc;
			return rcu.getRenameTypeProcessor();
		}
		Assert.isTrue(false); // Should never get here
		return null;
	}
}
