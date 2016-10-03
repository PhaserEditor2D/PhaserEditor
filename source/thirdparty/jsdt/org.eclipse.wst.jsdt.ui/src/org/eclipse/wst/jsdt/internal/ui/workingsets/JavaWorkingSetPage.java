/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rodrigo Kumpera <kumpera AT gmail.com> - bug 95232
 *     
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.filters.EmptyInnerPackageFilter;
import org.eclipse.wst.jsdt.internal.ui.util.JavaUIHelp;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * The Java working set page allows the user to create
 * and edit a Java working set.
 * <p>
 * Working set elements are presented as a Java element tree.
 * </p>
 * 
 * 
 */
public class JavaWorkingSetPage extends WizardPage implements IWorkingSetPage {

	final private static String PAGE_TITLE= WorkingSetMessages.JavaWorkingSetPage_title; 
	final private static String PAGE_ID= "javaWorkingSetPage"; //$NON-NLS-1$
	
	private Text fWorkingSetName;
	private CheckboxTreeViewer fTree;
	private ITreeContentProvider fTreeContentProvider;
	
	private boolean fFirstCheck;
	private IWorkingSet fWorkingSet;

	/**
	 * Default constructor.
	 */
	public JavaWorkingSetPage() {
		super(PAGE_ID, PAGE_TITLE, JavaPluginImages.DESC_WIZBAN_JAVA_WORKINGSET);
		setDescription(WorkingSetMessages.JavaWorkingSetPage_workingSet_description); 
		fFirstCheck= true;
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		Label label= new Label(composite, SWT.WRAP);
		label.setText(WorkingSetMessages.JavaWorkingSetPage_workingSet_name); 
		GridData gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);

		fWorkingSetName= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fWorkingSetName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateInput();
				}
			}
		);
		fWorkingSetName.setFocus();
		
		label= new Label(composite, SWT.WRAP);
		label.setText(WorkingSetMessages.JavaWorkingSetPage_workingSet_content); 
		gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);

		fTree= new CheckboxTreeViewer(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		ColoredViewersManager.install(fTree);
		gd= new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		gd.heightHint= convertHeightInCharsToPixels(15);
		fTree.getControl().setLayoutData(gd);
		
		fTreeContentProvider= new JavaWorkingSetPageContentProvider();
		fTree.setContentProvider(fTreeContentProvider);
		
		AppearanceAwareLabelProvider fJavaElementLabelProvider= 
			new AppearanceAwareLabelProvider(
				AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaScriptElementLabels.P_COMPRESSED,
				AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS
			);
		
		fTree.setLabelProvider(new DecoratingJavaLabelProvider(fJavaElementLabelProvider));
		fTree.setComparator(new JavaScriptElementComparator());
		fTree.addFilter(new EmptyInnerPackageFilter());
		fTree.setUseHashlookup(true);
		
		fTree.setInput(JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot()));

		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				handleCheckStateChange(event);
			}
		});

		fTree.addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}
			public void treeExpanded(TreeExpansionEvent event) {
				final Object element= event.getElement();
				if (fTree.getGrayed(element) == false)
					BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
					public void run() {
						setSubtreeChecked(element, fTree.getChecked(element), false);
					}
				});
			}
		});

		// Add select / deselect all buttons for bug 46669
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0; layout.marginHeight= 0;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText(WorkingSetMessages.JavaWorkingSetPage_selectAll_label);
		selectAllButton.setToolTipText(WorkingSetMessages.JavaWorkingSetPage_selectAll_toolTip);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTree.setCheckedElements(fTreeContentProvider.getElements(fTree.getInput()));
				validateInput();
			}
		});
		selectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(selectAllButton);

		Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
		deselectAllButton.setText(WorkingSetMessages.JavaWorkingSetPage_deselectAll_label);
		deselectAllButton.setToolTipText(WorkingSetMessages.JavaWorkingSetPage_deselectAll_toolTip);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTree.setCheckedElements(new Object[0]);
				validateInput();
			}
		});
		deselectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(deselectAllButton);
		
		if (fWorkingSet != null)
			fWorkingSetName.setText(fWorkingSet.getName());
		initializeCheckedState();
		validateInput();

		Dialog.applyDialogFont(composite);
		// Set help for the page 
		JavaUIHelp.setHelp(fTree, IJavaHelpContextIds.JAVA_WORKING_SET_PAGE);
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public void setSelection(IWorkingSet workingSet) {
		Assert.isNotNull(workingSet, "Working set must not be null"); //$NON-NLS-1$
		fWorkingSet= workingSet;
		if (getContainer() != null && getShell() != null && fWorkingSetName != null) {
			fFirstCheck= false;
			fWorkingSetName.setText(fWorkingSet.getName());
			initializeCheckedState();
			validateInput();
		}
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public void finish() {
		String workingSetName= fWorkingSetName.getText();
		ArrayList elements= new ArrayList(10);
		findCheckedElements(elements, fTree.getInput());
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet= workingSetManager.createWorkingSet(workingSetName, (IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
		} else {
			// Add inaccessible resources
			IAdaptable[] oldItems= fWorkingSet.getElements();
			HashSet closedWithChildren= new HashSet(elements.size());
			for (int i= 0; i < oldItems.length; i++) {
				IResource oldResource= null;
				if (oldItems[i] instanceof IResource) {
					oldResource= (IResource)oldItems[i];
				} else {
					oldResource= (IResource)oldItems[i].getAdapter(IResource.class);
				}
				if (oldResource != null && oldResource.isAccessible() == false) {
					IProject project= oldResource.getProject();
					if (closedWithChildren.contains(project) || elements.contains(project)) {
						elements.add(oldItems[i]);
						elements.remove(project);
						closedWithChildren.add(project);
					}
				}
			}
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements((IAdaptable[]) elements.toArray(new IAdaptable[elements.size()]));
		}
	}

	private void validateInput() {
		String errorMessage= null; 
		String infoMessage= null;
		String newText= fWorkingSetName.getText();

		if (newText.equals(newText.trim()) == false)
			errorMessage = WorkingSetMessages.JavaWorkingSetPage_warning_nameWhitespace; 
		if (newText.equals("")) { //$NON-NLS-1$
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck= false;
				return;
			}
			else				
				errorMessage= WorkingSetMessages.JavaWorkingSetPage_warning_nameMustNotBeEmpty; 
		}

		fFirstCheck= false;

		if (errorMessage == null && (fWorkingSet == null || newText.equals(fWorkingSet.getName()) == false)) {
			IWorkingSet[] workingSets= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
			for (int i= 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage= WorkingSetMessages.JavaWorkingSetPage_warning_workingSetExists; 
				}
			}
		}
		
		if (!hasCheckedElement())
			infoMessage= WorkingSetMessages.JavaWorkingSetPage_warning_resourceMustBeChecked;

		setMessage(infoMessage, INFORMATION);
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	private boolean hasCheckedElement() {
		TreeItem[] items= fTree.getTree().getItems();
		for (int i= 0; i < items.length; i++) {
			if (items[i].getChecked())
				return true;
		}
		return false;
	}
	
	private void findCheckedElements(List checkedResources, Object parent) {
		Object[] children= fTreeContentProvider.getChildren(parent);
		for (int i= 0; i < children.length; i++) {
			if (fTree.getGrayed(children[i]))
				findCheckedElements(checkedResources, children[i]);
			else if (fTree.getChecked(children[i]))
				checkedResources.add(children[i]);
		}
	}

	void handleCheckStateChange(final CheckStateChangedEvent event) {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				IAdaptable element= (IAdaptable)event.getElement();
				boolean state= event.getChecked();		
				fTree.setGrayed(element, false);
				if (isExpandable(element))
					setSubtreeChecked(element, state, state); // only check subtree if state is set to true
					
				updateParentState(element, state);
				validateInput();
			}
		});
	}

	private void setSubtreeChecked(Object parent, boolean state, boolean checkExpandedState) {
		if (!(parent instanceof IAdaptable))
			return;
		IContainer container= (IContainer)((IAdaptable)parent).getAdapter(IContainer.class);
		if ((!fTree.getExpandedState(parent) && checkExpandedState) || (container != null && !container.isAccessible()))
			return;
		
		Object[] children= fTreeContentProvider.getChildren(parent);
		for (int i= children.length - 1; i >= 0; i--) {
			Object element= children[i];
			if (state) {
				fTree.setChecked(element, true);
				fTree.setGrayed(element, false);
			}
			else
				fTree.setGrayChecked(element, false);
			if (isExpandable(element))
				setSubtreeChecked(element, state, true);
		}
	}

	private void updateParentState(Object child, boolean baseChildState) {
		if (child == null)
			return;
		if (child instanceof IAdaptable) {
			IResource resource= (IResource)((IAdaptable)child).getAdapter(IResource.class);
			if (resource != null && !resource.isAccessible())
				return;
		}
		Object parent= fTreeContentProvider.getParent(child);
		if (parent == null)
			return;
		
		updateObjectState(parent, baseChildState);
	}

	private void updateObjectState(Object element, boolean baseChildState) {		

		boolean allSameState= true;
		Object[] children= fTreeContentProvider.getChildren(element);

		for (int i= children.length -1; i >= 0; i--) {
			if (fTree.getChecked(children[i]) != baseChildState || fTree.getGrayed(children[i])) {
				allSameState= false;
				break;
			}
		}
	
		fTree.setGrayed(element, !allSameState);
		fTree.setChecked(element, !allSameState || baseChildState);
		
		updateParentState(element, baseChildState);
	}

	private void initializeCheckedState() {

		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				Object[] elements;
				if (fWorkingSet == null) {
					// Use current part's selection for initialization
					IWorkbenchPage page= JavaScriptPlugin.getActivePage();
					if (page == null)
						return;
					
					IWorkbenchPart part= JavaScriptPlugin.getActivePage().getActivePart();
					if (part == null)
						return;
					
					try {
						elements= SelectionConverter.getStructuredSelection(part).toArray();
						for (int i= 0; i < elements.length; i++) {
							if (elements[i] instanceof IResource) {
								IJavaScriptElement je= (IJavaScriptElement)((IResource)elements[i]).getAdapter(IJavaScriptElement.class);
								if (je != null && je.exists() &&  je.getJavaScriptProject().isOnIncludepath((IResource)elements[i]))
									elements[i]= je;
							}
						}
					} catch (JavaScriptModelException e) {
						return;
					}
				}
				else
					elements= fWorkingSet.getElements();

				// Use closed project for elements in closed project
				for (int i= 0; i < elements.length; i++) {
					Object element= elements[i];
					if (element instanceof IResource) {
						IProject project= ((IResource)element).getProject();
						if (!project.isAccessible())
							elements[i]= project;
					}
					if (element instanceof IJavaScriptElement) {
						IJavaScriptProject jProject= ((IJavaScriptElement)element).getJavaScriptProject();
						if (jProject != null && !jProject.getProject().isAccessible()) 
							elements[i]= jProject.getProject();
					}
				}

				fTree.setCheckedElements(elements);
				HashSet parents = new HashSet();
				for (int i= 0; i < elements.length; i++) {
					Object element= elements[i];
					if (isExpandable(element))
						setSubtreeChecked(element, true, true);
						
					if (element instanceof IAdaptable) {
						IResource resource= (IResource) ((IAdaptable)element).getAdapter(IResource.class);
						if (resource != null && !resource.isAccessible())
							continue;
					}
					Object parent= fTreeContentProvider.getParent(element);
					if (parent != null)
						parents.add(parent);
				}
				
				for (Iterator i = parents.iterator(); i.hasNext();)
					updateObjectState(i.next(), true);
			}
		});
	}
	
	private boolean isExpandable(Object element) {
		return (
			element instanceof IJavaScriptProject
			||
			element instanceof IPackageFragmentRoot
			||
			element instanceof IPackageFragment
			||
			element instanceof IJavaScriptModel
			||
			element instanceof IContainer);
	}
}
