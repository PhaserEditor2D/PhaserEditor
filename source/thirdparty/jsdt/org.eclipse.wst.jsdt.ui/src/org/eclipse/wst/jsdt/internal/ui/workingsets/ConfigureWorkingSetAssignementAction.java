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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public final class ConfigureWorkingSetAssignementAction extends SelectionDispatchAction {
	
	private static final class GrayedCheckedModel {

		private GrayedCheckedModelElement[] fElements;
		private final Hashtable fLookup;
		
		public GrayedCheckedModel(GrayedCheckedModelElement[] elements) {
			fElements= elements;
			fLookup= new Hashtable();
			for (int i= 0; i < elements.length; i++) {
				fLookup.put(elements[i].getWorkingSet(), elements[i]);
			}
		}
		
		public void addElement(GrayedCheckedModelElement element) {
			ArrayList list= new ArrayList(Arrays.asList(fElements));
			list.add(element);
			fElements= (GrayedCheckedModelElement[])list.toArray(new GrayedCheckedModelElement[list.size()]);
			
			fLookup.put(element.getWorkingSet(), element);
		}

		public IWorkingSet[] getAll() {
			IWorkingSet[] result= new IWorkingSet[fElements.length];
			for (int i= 0; i < fElements.length; i++) {
				result[i]= fElements[i].getWorkingSet();
			}
			return result;
		}
		
		public GrayedCheckedModelElement getModelElement(IWorkingSet element) {
			return (GrayedCheckedModelElement)fLookup.get(element);
		}

		public IWorkingSet[] getChecked() {
			ArrayList result= new ArrayList();
			for (int i= 0; i < fElements.length; i++) {
				if (fElements[i].isChecked())
					result.add(fElements[i].getWorkingSet());
			}
			return (IWorkingSet[])result.toArray(new IWorkingSet[result.size()]);
		}

		public IWorkingSet[] getGrayed() {
			ArrayList result= new ArrayList();
			for (int i= 0; i < fElements.length; i++) {
				if (fElements[i].isGrayed())
					result.add(fElements[i].getWorkingSet());
			}
			return (IWorkingSet[])result.toArray(new IWorkingSet[result.size()]);
		}

		public void selectAll() {
			for (int i= 0; i < fElements.length; i++) {
				fElements[i].select();
			}
		}

		public void deselectAll() {
			for (int i= 0; i < fElements.length; i++) {
				fElements[i].deselect();
			}
		}
		
	}

	private final static class GrayedCheckedModelElement {
		
		private final IWorkingSet fWorkingSet;
		private final int fElementCount;
		private int fCheckCount;
		
		public GrayedCheckedModelElement(IWorkingSet workingSet, int checkCount, int elementCount) {
			fWorkingSet= workingSet;
			fCheckCount= checkCount;
			fElementCount= elementCount;
		}
		
		public IWorkingSet getWorkingSet() {
			return fWorkingSet;
		}
		
		public int getCheckCount() {
			return fCheckCount;
		}
		
		public boolean isGrayed() {
			return isChecked() && fCheckCount < fElementCount;
		}
		
		public boolean isChecked() {
			return fCheckCount > 0;
		}

		public void deselect() {
			fCheckCount= 0;
		}
		
		public void select() {
			fCheckCount= fElementCount;
		}
		
	}
	
	private final class WorkingSetModelAwareSelectionDialog extends SimpleWorkingSetSelectionDialog {
		
		private CheckboxTableViewer fTableViewer;
		private boolean fShowVisibleOnly;
		private GrayedCheckedModel fModel;
		private final IAdaptable[] fElements;
		
		private WorkingSetModelAwareSelectionDialog(Shell shell, GrayedCheckedModel model, IAdaptable[] elements) {
			super(shell, VALID_WORKING_SET_IDS, model.getChecked());
			setTitle(WorkingSetMessages.ConfigureWorkingSetAssignementAction_WorkingSetAssignments_title);
			fModel= model;
			fShowVisibleOnly= true;
			fElements= elements;
		}
		
		public IWorkingSet[] getGrayed() {
			return fModel.getGrayed();
		}
		
		public IWorkingSet[] getSelection() {
			return fModel.getChecked();
		}

		protected CheckboxTableViewer createTableViewer(Composite parent) {
			fTableViewer= super.createTableViewer(parent);
			fTableViewer.setGrayedElements(fModel.getGrayed());
			fTableViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					IWorkingSet element= (IWorkingSet)event.getElement();
					fTableViewer.setGrayed(element, false);
					GrayedCheckedModelElement modelElement= fModel.getModelElement(element);
					if (event.getChecked()) {
						modelElement.select();
					} else {
						modelElement.deselect();
					}
					fTableViewer.update(element, null);
				}
			});
			
			createShowVisibleOnly(parent);
			
			return fTableViewer;
		}
		
		protected void selectAll() {
			super.selectAll();
			fModel.selectAll();
			fTableViewer.setGrayedElements(new Object[0]);
			fTableViewer.refresh();
		}

		protected void deselectAll() {
			super.deselectAll();
			fModel.deselectAll();
			fTableViewer.setGrayedElements(new Object[0]);
			fTableViewer.refresh();
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected void addNewWorkingSet(IWorkingSet workingSet) {
			if (fWorkingSetModel != null) {
				IWorkingSet[] workingSets= fWorkingSetModel.getActiveWorkingSets();
				IWorkingSet[] activeWorkingSets= new IWorkingSet[workingSets.length+ 1];
				activeWorkingSets[0]= workingSet;
				System.arraycopy(workingSets, 0, activeWorkingSets, 1, workingSets.length);
				fWorkingSetModel.setActiveWorkingSets(activeWorkingSets);
			}
			
			int checkCount= 0;
			for (int i= 0; i < fElements.length; i++) {
				IAdaptable adapted= adapt(workingSet, fElements[i]);
				if (adapted != null && contains(workingSet, adapted)) {
					checkCount++;
				}
			}
			
			fModel.addElement(new GrayedCheckedModelElement(workingSet, checkCount, fElements.length));
			
			fTableViewer.setInput(fModel.getAll());
			fTableViewer.refresh();
			
			fTableViewer.setCheckedElements(fModel.getChecked());
			fTableViewer.setGrayedElements(fModel.getGrayed());
			
			fTableViewer.setSelection(new StructuredSelection(workingSet));
		}

		protected ViewerFilter createTableFilter() {
			final ViewerFilter superFilter= super.createTableFilter();
			return new ViewerFilter() {
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (!superFilter.select(viewer, parentElement, element))
						return false;
					
					IWorkingSet set= (IWorkingSet)element;
					if (!isValidWorkingSet(set))
						return false;
					
					if (fWorkingSetModel == null)
						return true;
					
					if (fShowVisibleOnly) {
						if (!fWorkingSetModel.isActiveWorkingSet(set))
							return false;
						
						return true;						
					} else {
						return true;
					}
				}					
			};
		}
		
		protected ViewerSorter createTableSorter() {
			final ViewerSorter superSorter= super.createTableSorter();
			return new ViewerSorter() {
				public int compare(Viewer viewer, Object e1, Object e2) {
					if (fWorkingSetModel == null)
						return superSorter.compare(viewer, e1, e2);
					
					IWorkingSet[] activeWorkingSets= fWorkingSetModel.getActiveWorkingSets();
					for (int i= 0; i < activeWorkingSets.length; i++) {
						IWorkingSet active= activeWorkingSets[i];
						if (active == e1) {
							return -1;
						} else if (active == e2) {
							return 1;
						}
					}
					
					return superSorter.compare(viewer, e1, e2);
				}
			};
		}
		
		protected LabelProvider createTableLabelProvider() {
			final LabelProvider superLabelProvider= super.createTableLabelProvider();
			return new LabelProvider() {
				public String getText(Object element) {
					String superText= superLabelProvider.getText(element);
					if (superText == null)
						return null;
					
					GrayedCheckedModelElement modelElement= fModel.getModelElement((IWorkingSet)element);
					if (!modelElement.isGrayed())
						return superText;
					
					return superText + Messages.format(WorkingSetMessages.ConfigureWorkingSetAssignementAction_XofY_label, new Object[] {Integer.valueOf(modelElement.getCheckCount()), Integer.valueOf(fElements.length)});
				}
			};
		}	
	
		private void createShowVisibleOnly(Composite parent) {
			if (fWorkingSetModel == null)
				return;
			
			Composite bar= new Composite(parent, SWT.NONE);
			bar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			GridLayout gridLayout= new GridLayout(2, false);
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			bar.setLayout(gridLayout);
			
			final Button showVisibleOnly= new Button(bar, SWT.CHECK);
			showVisibleOnly.setSelection(fShowVisibleOnly);
			showVisibleOnly.setLayoutData(new GridData(SWT.LEAD, SWT.FILL, false, true));
			showVisibleOnly.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fShowVisibleOnly= showVisibleOnly.getSelection();
					
					fTableViewer.refresh();
					
					fTableViewer.setCheckedElements(fModel.getChecked());
					fTableViewer.setGrayedElements(fModel.getGrayed());
				}
			});
			
			Link ppwsLink= new Link(bar, SWT.NONE);
			ppwsLink.setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_OnlyShowVisible_link);
			ppwsLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			ppwsLink.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					
					List workingSets= new ArrayList(Arrays.asList(fWorkingSetModel.getAllWorkingSets()));
					IWorkingSet[] activeWorkingSets= fWorkingSetModel.getActiveWorkingSets();
					WorkingSetConfigurationDialog dialog= new WorkingSetConfigurationDialog(
						getShell(), 
						(IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]),
						activeWorkingSets); 
					dialog.setSelection(activeWorkingSets);
					if (dialog.open() == IDialogConstants.OK_ID) {
						IWorkingSet[] selection= dialog.getSelection();
						fWorkingSetModel.setActiveWorkingSets(selection);
					}
					
					recalculateCheckedState();
				}
			});
		}
		
		private void recalculateCheckedState() {
			fModel= createGrayedCheckedModel(fElements, getAllWorkingSets());
			
			fTableViewer.setInput(fModel.getAll());
			fTableViewer.refresh();
			fTableViewer.setCheckedElements(fModel.getChecked());
			fTableViewer.setGrayedElements(fModel.getGrayed());
		}
	}

	private static final String[] VALID_WORKING_SET_IDS= new String[] {
			JavaWorkingSetUpdater.ID,
			"org.eclipse.ui.resourceWorkingSetPage" //$NON-NLS-1$
	};
	
	private WorkingSetModel fWorkingSetModel;
	private final IWorkbenchSite fSite;

	public ConfigureWorkingSetAssignementAction(IWorkbenchSite site) {
		super(site);
		fSite= site;
		setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_WorkingSets_actionLabel);
		setEnabled(false);
	}
	
	public void setWorkingSetModel(WorkingSetModel workingSetModel) {
		fWorkingSetModel= workingSetModel;
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
	}

	private boolean canEnable(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		
		List list= selection.toList();
		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
			Object object= iterator.next();
			if (!(object instanceof IResource) && !(object instanceof IJavaScriptElement))
				return false;
		}
		
		return true;
	}

	private IAdaptable[] getSelectedElements(IStructuredSelection selection) {
		ArrayList result= new ArrayList();
		
		List list= selection.toList();
		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
			Object object= iterator.next();
			if (object instanceof IResource || object instanceof IJavaScriptElement) {
				result.add(object);
			}
		}
		
		return (IAdaptable[])result.toArray(new IAdaptable[result.size()]);
	}
	
	public void run(IStructuredSelection selection) {
		IAdaptable[] elements= getSelectedElements(selection);
		GrayedCheckedModel model= createGrayedCheckedModel(elements, getAllWorkingSets());
		WorkingSetModelAwareSelectionDialog dialog= new WorkingSetModelAwareSelectionDialog(fSite.getShell(), model, elements);
		
		if (elements.length == 1) {
			IAdaptable element= elements[0];
			String elementName;
			if (element instanceof IResource) {
				elementName= ((IResource)element).getName();
			} else {
				elementName= JavaScriptElementLabels.getElementLabel((IJavaScriptElement)element, JavaScriptElementLabels.ALL_DEFAULT);
			}
			dialog.setMessage(Messages.format(WorkingSetMessages.ConfigureWorkingSetAssignementAction_DialogMessage_specific, elementName));
		} else {
			dialog.setMessage(Messages.format(WorkingSetMessages.ConfigureWorkingSetAssignementAction_DialogMessage_multi, Integer.valueOf(elements.length)));
		}
		if (dialog.open() == Window.OK) {
			updateWorkingSets(dialog.getSelection(), dialog.getGrayed(), elements);
			selectAndReveal(elements);
		}
	}
	
	private static GrayedCheckedModel createGrayedCheckedModel(IAdaptable[] elements, IWorkingSet[] workingSets) {
		GrayedCheckedModelElement[] result= new GrayedCheckedModelElement[workingSets.length];
		
		for (int i= 0; i < workingSets.length; i++) {
			IWorkingSet set= workingSets[i];
			
			int checkCount= 0;
			for (int j= 0; j < elements.length; j++) {
				IAdaptable adapted= adapt(set, elements[j]);
				if (adapted != null && contains(set, adapted)) {
					checkCount++;
				}
			}
			
			result[i]= new GrayedCheckedModelElement(set, checkCount, elements.length);
		}
		
		return new GrayedCheckedModel(result);
	}

	private void updateWorkingSets(IWorkingSet[] newWorkingSets, IWorkingSet[] grayedWorkingSets, IAdaptable[] elements) {
		HashSet selectedSets= new HashSet(Arrays.asList(newWorkingSets));
		HashSet grayedSets= new HashSet(Arrays.asList(grayedWorkingSets));
		IWorkingSet[] workingSets= getAllWorkingSets();
		
		for (int i= 0; i < workingSets.length; i++) {
			IWorkingSet workingSet= workingSets[i];
			if (isValidWorkingSet(workingSet) && !selectedSets.contains(workingSet) && !grayedSets.contains(workingSet)) {
				for (int j= 0; j < elements.length; j++) {							
					IAdaptable adapted= adapt(workingSet, elements[j]);
					if (adapted != null && contains(workingSet, adapted)) {
						remove(workingSet, adapted);
					}
				}
			}
		}

		for (int i= 0; i < newWorkingSets.length; i++) {
			IWorkingSet set= newWorkingSets[i];
			if (isValidWorkingSet(set) && !grayedSets.contains(set)) {
				for (int j= 0; j < elements.length; j++) {						
					IAdaptable adapted= adapt(set, elements[j]);
					if (adapted != null && !contains(set, adapted)) {
						add(set, adapted);
					}
				}
			}
		}
	}
	
	private IWorkingSet[] getAllWorkingSets() {
		if (fWorkingSetModel != null) {
			return fWorkingSetModel.getAllWorkingSets();
		} else {
			return PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
		}
	}

	private static boolean isValidWorkingSet(IWorkingSet set) {
		if (set.isAggregateWorkingSet() || !set.isSelfUpdating())
			return false;
		
		if (!set.isVisible())
			return false;
		
		if (!set.isEditable())
			return false;

		for (int i= 0; i < VALID_WORKING_SET_IDS.length; i++) {
			if (VALID_WORKING_SET_IDS[i].equals(set.getId()))
				return true;
		}

		return false;
	}

	private static IAdaptable adapt(IWorkingSet set, IAdaptable element) {
		IAdaptable[] adaptedElements= set.adaptElements(new IAdaptable[] {
			element
		});
		if (adaptedElements.length != 1)
			return null;

		return adaptedElements[0];
	}

	private static boolean contains(IWorkingSet set, IAdaptable adaptedElement) {
		IAdaptable[] elements= set.getElements();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i].equals(adaptedElement))
				return true;
		}

		return false;
	}

	private static void remove(IWorkingSet workingSet, IAdaptable adaptedElement) {
		HashSet set= new HashSet(Arrays.asList(workingSet.getElements()));
		set.remove(adaptedElement);
		workingSet.setElements((IAdaptable[])set.toArray(new IAdaptable[set.size()]));
	}

	private static void add(IWorkingSet workingSet, IAdaptable adaptedElement) {
		IAdaptable[] elements= workingSet.getElements();
		IAdaptable[] newElements= new IAdaptable[elements.length + 1];
		System.arraycopy(elements, 0, newElements, 0, elements.length);
		newElements[elements.length]= adaptedElement;
		workingSet.setElements(newElements);
	}
	
	private void selectAndReveal(IAdaptable[] elements) {
		PackageExplorerPart explorer= getActivePackageExplorer();
		if (explorer != null)
			explorer.selectReveal(new StructuredSelection(elements));
	}
	
	private PackageExplorerPart getActivePackageExplorer() {
		IWorkbenchPage page= JavaScriptPlugin.getActivePage();
		if (page != null) {
			IWorkbenchPart activePart= page.getActivePart();
			if (activePart instanceof PackageExplorerPart) {
				return (PackageExplorerPart) activePart;
			}
		}
		return null;
	}

}
