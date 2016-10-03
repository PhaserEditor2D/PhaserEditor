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
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

public class JavadocTreeWizardPage extends JavadocWizardPage {

	private CheckboxTreeAndListGroup fInputGroup;

	private Text fDestinationText;
	private Combo fJavadocCommandText;
	private Text fDocletText;
	private Text fDocletTypeText;
	private Button fStandardButton;
	private Button fDestinationBrowserButton;
	private Button fCustomButton;
	private Button fPrivateVisibility;
	private Button fProtectedVisibility;
	private Button fPackageVisibility;
	private Button fPublicVisibility;
	private Label fDocletLabel;
	private Label fDocletTypeLabel;
	private Label fDestinationLabel;
	private CLabel fDescriptionLabel;
	
	private String fVisibilitySelection;

	private JavadocOptionsManager fStore;

	private StatusInfo fJavadocStatus;
	private StatusInfo fDestinationStatus;
	private StatusInfo fDocletStatus;
	private StatusInfo fTreeStatus;
	private StatusInfo fPreferenceStatus;
	private StatusInfo fWizardStatus;

	private final int PREFERENCESTATUS= 0;
	private final int CUSTOMSTATUS= 1;
	private final int STANDARDSTATUS= 2;
	private final int TREESTATUS= 3;
	private final int JAVADOCSTATUS= 4;

	/**
	 * Constructor for JavadocTreeWizardPage.
	 * @param pageName
	 */
	protected JavadocTreeWizardPage(String pageName, JavadocOptionsManager store) {
		super(pageName);
		setDescription(JavadocExportMessages.JavadocTreeWizardPage_javadoctreewizardpage_description); 

		fStore= store;

		// Status variables
		fJavadocStatus= new StatusInfo();
		fDestinationStatus= new StatusInfo();
		fDocletStatus= new StatusInfo();
		fTreeStatus= new StatusInfo();
		fPreferenceStatus= new StatusInfo();
		fWizardStatus= store.getWizardStatus();
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 6;
		composite.setLayout(layout);

		createJavadocCommandSet(composite);
		createInputGroup(composite);
		createVisibilitySet(composite);
		createOptionsSet(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.JAVADOC_TREE_PAGE);
	}
	
	protected void createJavadocCommandSet(Composite composite) {
		
		final int numColumns= 2;
		
		GridLayout layout= createGridLayout(numColumns);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		Composite group = new Composite(composite, SWT.NONE);
		group.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		group.setLayout(layout);

		createLabel(group, SWT.NONE, JavadocExportMessages.JavadocTreeWizardPage_javadoccommand_label, createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, numColumns, 0)); 
		fJavadocCommandText= createCombo(group, SWT.NONE, null, createGridData(GridData.FILL_HORIZONTAL, numColumns - 1, 0));

		fJavadocCommandText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(JAVADOCSTATUS);
			}
		});

		final Button javadocCommandBrowserButton= createButton(group, SWT.PUSH, JavadocExportMessages.JavadocTreeWizardPage_javadoccommand_button_label, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0)); 
		SWTUtil.setButtonDimensionHint(javadocCommandBrowserButton);

		javadocCommandBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				browseForJavadocCommand();
			}
		});
	}
	

	
	
	protected void createInputGroup(Composite composite) {

		createLabel(composite, SWT.NONE, JavadocExportMessages.JavadocTreeWizardPage_checkboxtreeandlistgroup_label, createGridData(6)); 
		Composite c= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		
		ITreeContentProvider treeContentProvider= new JavadocProjectContentProvider();
		ITreeContentProvider listContentProvider= new JavadocMemberContentProvider();
		fInputGroup= new CheckboxTreeAndListGroup(c, this, treeContentProvider, new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT), listContentProvider, new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT), SWT.NONE, convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(10));

		fInputGroup.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doValidation(TREESTATUS);
			}
		});
		fInputGroup.setTreeComparator(new JavaScriptElementComparator());
		
		IJavaScriptElement[] elements= fStore.getInitialElements();
		setTreeChecked(elements);
		if (elements.length > 0) {
			fInputGroup.setTreeSelection(new StructuredSelection(elements[0].getJavaScriptProject()));
		}

		fInputGroup.aboutToOpen();
	}

	private void createVisibilitySet(Composite composite) {

		GridLayout visibilityLayout= createGridLayout(4);
		visibilityLayout.marginHeight= 0;
		visibilityLayout.marginWidth= 0;
		Composite visibilityGroup= new Composite(composite, SWT.NONE);
		visibilityGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		visibilityGroup.setLayout(visibilityLayout);

		createLabel(visibilityGroup, SWT.NONE, JavadocExportMessages.JavadocTreeWizardPage_visibilitygroup_label, createGridData(GridData.FILL_HORIZONTAL, 4, 0)); 
		fPrivateVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.JavadocTreeWizardPage_privatebutton_label, createGridData(GridData.FILL_HORIZONTAL, 1, 0)); 
		fPackageVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.JavadocTreeWizardPage_packagebutton_label, createGridData(GridData.FILL_HORIZONTAL, 1, 0)); 
		fProtectedVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.JavadocTreeWizardPage_protectedbutton_label, createGridData(GridData.FILL_HORIZONTAL, 1, 0)); 
		fPublicVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.JavadocTreeWizardPage_publicbutton_label, createGridData(GridData.FILL_HORIZONTAL, 1, 0)); 

		fDescriptionLabel= new CLabel(visibilityGroup, SWT.LEFT);
		fDescriptionLabel.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, convertWidthInCharsToPixels(3) -  3)); // INDENT of CLabel

		fPrivateVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PRIVATE;
					fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_privatevisibilitydescription_label); 
				}
			}
		});
		fPackageVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PACKAGE;
					fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_packagevisibledescription_label); 
				}
			}
		});
		fProtectedVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PROTECTED;
					fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_protectedvisibilitydescription_label); 
				}
			}
		});

		fPublicVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PUBLIC;
					fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_publicvisibilitydescription_label); 
				}
			}
		});

		setVisibilitySettings();

	}

	protected void setVisibilitySettings() {
		fVisibilitySelection= fStore.getAccess();
		fPrivateVisibility.setSelection(fVisibilitySelection.equals(fStore.PRIVATE));
		if (fPrivateVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_privatevisibilitydescription_label); 

		fProtectedVisibility.setSelection(fVisibilitySelection.equals(fStore.PROTECTED));
		if (fProtectedVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_protectedvisibilitydescription_label); 

		fPackageVisibility.setSelection(fVisibilitySelection.equals(fStore.PACKAGE));
		if (fPackageVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_packagevisibledescription_label); 

		fPublicVisibility.setSelection(fVisibilitySelection.equals(fStore.PUBLIC));
		if (fPublicVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.JavadocTreeWizardPage_publicvisibilitydescription_label); 
	}

	private void createOptionsSet(Composite composite) {
		
		final int numColumns= 4;

		final GridLayout layout= createGridLayout(numColumns);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		Composite group= new Composite(composite, SWT.NONE);
		group.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		group.setLayout(layout);

		fStandardButton= createButton(group, SWT.RADIO, JavadocExportMessages.JavadocTreeWizardPage_standarddocletbutton_label, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns, 0)); 

		fDestinationLabel= createLabel(group, SWT.NONE, JavadocExportMessages.JavadocTreeWizardPage_destinationfield_label, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3))); 
		fDestinationText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, numColumns - 2, 0));
		((GridData) fDestinationText.getLayoutData()).widthHint= 0;
		fDestinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});

		fDestinationBrowserButton= createButton(group, SWT.PUSH, JavadocExportMessages.JavadocTreeWizardPage_destinationbrowse_label, createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); 
		SWTUtil.setButtonDimensionHint(fDestinationBrowserButton);

		//Option to use custom doclet
		fCustomButton= createButton(group, SWT.RADIO, JavadocExportMessages.JavadocTreeWizardPage_customdocletbutton_label, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns, 0)); 
		
		//For Entering location of custom doclet
		fDocletTypeLabel= createLabel(group, SWT.NONE, JavadocExportMessages.JavadocTreeWizardPage_docletnamefield_label, createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); 
		fDocletTypeText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns - 1, 0));
		((GridData) fDocletTypeText.getLayoutData()).widthHint= 0;
		
		
		fDocletTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}
		});

		fDocletLabel= createLabel(group, SWT.NONE, JavadocExportMessages.JavadocTreeWizardPage_docletpathfield_label, createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); 
		fDocletText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns - 1, 0));
		((GridData) fDocletText.getLayoutData()).widthHint= 0;
		
		fDocletText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}

		});

		//Add Listeners
		fCustomButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }, new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }));
		fCustomButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(CUSTOMSTATUS);
			}
		});
		fStandardButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }, new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }));
		fStandardButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});
		fDestinationBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				String text= handleFolderBrowseButtonPressed(fDestinationText.getText(), JavadocExportMessages.JavadocTreeWizardPage_destinationbrowsedialog_title, 
				   		JavadocExportMessages.JavadocTreeWizardPage_destinationbrowsedialog_label); 
				fDestinationText.setText(text);
			}
		});

		setOptionSetSettings();
	}

	public boolean getCustom() {
		return fCustomButton.getSelection();
	}

	private void setOptionSetSettings() {

		if (!fStore.isFromStandard()) {
			fCustomButton.setSelection(true);
			fDocletText.setText(fStore.getDocletPath());
			fDocletTypeText.setText(fStore.getDocletName());
			fDestinationText.setText(fStore.getDestination());
			fDestinationText.setEnabled(false);
			fDestinationBrowserButton.setEnabled(false);
			fDestinationLabel.setEnabled(false);
			
		} else {
			fStandardButton.setSelection(true);
			fDestinationText.setText(fStore.getDestination());
			fDocletText.setText(fStore.getDocletPath());
			fDocletTypeText.setText(fStore.getDocletName());
			fDocletText.setEnabled(false);
			fDocletLabel.setEnabled(false);
			fDocletTypeText.setEnabled(false);
			fDocletTypeLabel.setEnabled(false);
		}
		
		fJavadocCommandText.setItems(fStore.getJavadocCommandHistory());
		fJavadocCommandText.select(0);
	}

	/**
	 * Receives of list of elements selected by the user and passes them
	 * to the CheckedTree. List can contain multiple projects and elements from
	 * different projects. If the list of seletected elements is empty a default
	 * project is selected.
	 */
	private void setTreeChecked(IJavaScriptElement[] sourceElements) {
		for (int i= 0; i < sourceElements.length; i++) {
			IJavaScriptElement curr= sourceElements[i];
			if (curr instanceof IJavaScriptUnit) {
				fInputGroup.initialCheckListItem(curr);
			} else if (curr instanceof IPackageFragment) {
				fInputGroup.initialCheckTreeItem(curr);
			} else if (curr instanceof IJavaScriptProject) {
				fInputGroup.initialCheckTreeItem(curr);
			} else if (curr instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) curr;
				if (!root.isArchive())
					fInputGroup.initialCheckTreeItem(curr);
			}
		}
	}

	private IPath[] getSourcePath(IJavaScriptProject[] projects) {
		HashSet res= new HashSet();
		//loops through all projects and gets a list if of their source paths
		for (int k= 0; k < projects.length; k++) {
			IJavaScriptProject iJavaProject= projects[k];

			try {
				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot curr= roots[i];
					if (curr.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IResource resource= curr.getResource();
						if (resource != null) {
							// Using get location is OK here. If the source folder
							// isn't local we can't create Javadoc for it.
							IPath p= resource.getLocation();
							if (p != null) {
								res.add(p);
							}
						}
					}
				}
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	private IPath[] getClassPath(IJavaScriptProject[] javaProjects) {
		HashSet res= new HashSet();

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (int j= 0; j < javaProjects.length; j++) {
			IJavaScriptProject curr= javaProjects[j];
			try {
				IPath outputLocation= null;
				
				// Not really clear yet what to do here for EFS. See bug
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=113233.

				String[] classPath= JavaRuntime.computeDefaultRuntimeClassPath(curr);
				for (int i= 0; i < classPath.length; i++) {
					IPath path= Path.fromOSString(classPath[i]);
					if (!path.equals(outputLocation)) {
						res.add(path);
					}
				}
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	/**
	 * Gets a list of elements to generated javadoc for from each project. 
	 * Javadoc can be generated for either a IPackageFragment or a IJavaScriptUnit.
	 */
	private IJavaScriptElement[] getSourceElements(IJavaScriptProject[] projects) {
		ArrayList res= new ArrayList();
		try {
			Set allChecked= fInputGroup.getAllCheckedTreeItems();

			Set incompletePackages= new HashSet();
			for (int h= 0; h < projects.length; h++) {
				IJavaScriptProject iJavaProject= projects[h];

				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot root= roots[i];
					if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IPath rootLocation= root.getResource().getLocation();
						IJavaScriptElement[] packs= root.getChildren();
						for (int k= 0; k < packs.length; k++) {
							IJavaScriptElement curr= packs[k];
							if (curr.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
								// default packages are always incomplete
								if (curr.getElementName().length() == 0 || !allChecked.contains(curr)
										|| fInputGroup.isTreeItemGreyChecked(curr) || !isAccessibleLocation(curr.getResource().getLocation(), rootLocation)) {
									incompletePackages.add(curr.getElementName());
								}
							}
						}
					}
				}
			}

			Iterator checkedElements= fInputGroup.getAllCheckedListItems();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IJavaScriptUnit) {
					IJavaScriptUnit unit= (IJavaScriptUnit) element;
					if (incompletePackages.contains(unit.getParent().getElementName())) {
						res.add(unit);
					}
				}
			}

			Set addedPackages= new HashSet();

			checkedElements= allChecked.iterator();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IPackageFragment) {
					IPackageFragment fragment= (IPackageFragment) element;
					String name= fragment.getElementName();
					if (!incompletePackages.contains(name) && !addedPackages.contains(name)) {
						res.add(fragment);
						addedPackages.add(name);
					}
				}
			}

		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		return (IJavaScriptElement[]) res.toArray(new IJavaScriptElement[res.size()]);
	}

	private boolean isAccessibleLocation(IPath packageLocation, IPath rootLocation) {
		return rootLocation != null && packageLocation != null && rootLocation.isPrefixOf(packageLocation);
	}

	protected void updateStore(IJavaScriptProject[] checkedProjects) {

		if (fCustomButton.getSelection()) {
			fStore.setDocletName(fDocletTypeText.getText());
			fStore.setDocletPath(fDocletText.getText());
			fStore.setFromStandard(false);
		}
		if (fStandardButton.getSelection()) {
			fStore.setFromStandard(true);
			//the destination used in javadoc generation
			fStore.setDestination(fDestinationText.getText());
		}

		fStore.setSourcepath(getSourcePath(checkedProjects));
		fStore.setClasspath(getClassPath(checkedProjects));
		fStore.setAccess(fVisibilitySelection);
		fStore.setSelectedElements(getSourceElements(checkedProjects));
		
		ArrayList commands= new ArrayList();
		commands.add(fJavadocCommandText.getText()); // must be first
		String[] items= fJavadocCommandText.getItems();
		for (int i= 0; i < items.length; i++) {
			String curr= items[i];
			if (!commands.contains(curr)) {
				commands.add(curr);
			}
		}
		fStore.setJavadocCommandHistory((String[]) commands.toArray(new String[commands.size()]));
	}

	public IJavaScriptProject[] getCheckedProjects() {
		ArrayList res= new ArrayList();
		TreeItem[] treeItems= fInputGroup.getTree().getItems();
		for (int i= 0; i < treeItems.length; i++) {
			if (treeItems[i].getChecked()) {
				Object curr= treeItems[i].getData();
				if (curr instanceof IJavaScriptProject) {
					res.add(curr);
				}
			}
		}
		return (IJavaScriptProject[]) res.toArray(new IJavaScriptProject[res.size()]);
	}
	
	protected void doValidation(int validate) {

		
		switch (validate) {
			case PREFERENCESTATUS :
				fPreferenceStatus= new StatusInfo();
				fDocletStatus= new StatusInfo();
				updateStatus(findMostSevereStatus());
				break;
			case CUSTOMSTATUS :

				if (fCustomButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					String doclet= fDocletTypeText.getText();
					String docletPath= fDocletText.getText();
					if (doclet.length() == 0) {
						fDocletStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_nodocletname_error); 

					} else if (JavaScriptConventions.validateJavaScriptTypeName(doclet).matches(IStatus.ERROR)) {
						fDocletStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_invaliddocletname_error); 
					} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
						fDocletStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_invaliddocletpath_error); 
					}
					updateStatus(findMostSevereStatus());
				}
				break;

			case STANDARDSTATUS :
				if (fStandardButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					String dest= fDestinationText.getText();
					if (dest.length() == 0) {
						fDestinationStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_nodestination_error); 
					}
					File file= new File(dest);
					if (!Path.ROOT.isValidPath(dest) || file.isFile()) {
						fDestinationStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_invaliddestination_error); 
					}
					if (new File(dest, "package-list").exists() || new File(dest, "index.html").exists()) //$NON-NLS-1$//$NON-NLS-2$
						fDestinationStatus.setWarning(JavadocExportMessages.JavadocTreeWizardPage_warning_mayoverwritefiles); 
					updateStatus(findMostSevereStatus());
				}
				break;

			case TREESTATUS :

				fTreeStatus= new StatusInfo();

				if (!fInputGroup.getAllCheckedListItems().hasNext())
					fTreeStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_invalidtreeselection_error); 
				updateStatus(findMostSevereStatus());

				break;
				
			case JAVADOCSTATUS:
				fJavadocStatus= new StatusInfo();
				String text= fJavadocCommandText.getText();
				if (text.length() == 0) {
					fJavadocStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_javadoccmd_error_enterpath);  
				} else {
					File file= new File(text);
					if (!file.isFile()) {
						fJavadocStatus.setError(JavadocExportMessages.JavadocTreeWizardPage_javadoccmd_error_notexists);  
					}
				}
				updateStatus(findMostSevereStatus());
				break;
		} //end switch
		
		
	}
	
	protected void browseForJavadocCommand() {
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(JavadocExportMessages.JavadocTreeWizardPage_javadoccmd_dialog_title); 
		String dirName= fJavadocCommandText.getText();
		dialog.setFileName(dirName);
		String selectedDirectory= dialog.open();
		if (selectedDirectory != null) {
			ArrayList newItems= new ArrayList();
			String[] items= fJavadocCommandText.getItems();
			newItems.add(selectedDirectory);
			for (int i= 0; i < items.length && newItems.size() < 5; i++) { // only keep the last 5 entries
				String curr= items[i];
				if (!newItems.contains(curr)) {
					newItems.add(curr);
				}
			}
			fJavadocCommandText.setItems((String[]) newItems.toArray(new String[newItems.size()]));
			fJavadocCommandText.select(0);
		}
	}
	

	private boolean validDocletPath(String docletPath) {
		StringTokenizer tokens= new StringTokenizer(docletPath, ";"); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			File file= new File(tokens.nextToken());
			if (!file.exists())
				return false;
		}
		return true;
	}

	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fJavadocStatus, fPreferenceStatus, fDestinationStatus, fDocletStatus, fTreeStatus, fWizardStatus });
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	public void setVisible(boolean visible) {
		if (visible) {
			doValidation(STANDARDSTATUS);
			doValidation(CUSTOMSTATUS);
			doValidation(TREESTATUS);
			doValidation(PREFERENCESTATUS);
			doValidation(JAVADOCSTATUS);
		}
		super.setVisible(visible);
	}

}
