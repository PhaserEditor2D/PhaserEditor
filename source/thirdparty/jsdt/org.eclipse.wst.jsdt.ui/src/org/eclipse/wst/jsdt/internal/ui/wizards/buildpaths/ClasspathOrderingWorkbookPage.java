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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ObjectStringStatusButtonDialogField;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;


public class ClasspathOrderingWorkbookPage extends BuildPathBasePage {
	
	private ListDialogField fClassPathList;
	
	private List allCpElements;
	
	private static final boolean HIDE_ALL_READONLY_CONTAINERS = true;
	private ObjectStringStatusButtonDialogField superTypeField;
	private IJavaScriptProject fJavaProject;
	private Control fSWTControl;
	
	
	public ClasspathOrderingWorkbookPage(ListDialogField classPathList) {
		fClassPathList= classPathList;
		superTypeField = new ObjectStringStatusButtonDialogField(new OrderingWorkbookPageAdapter());
		
	}
	
	public ObjectStringStatusButtonDialogField getSuperField() {
		return superTypeField;
	}
	class OrderingWorkbookPageAdapter implements IStringButtonAdapter{
		public void changeControlPressed(DialogField field) {
			if(field==superTypeField) {
				CPListElement elements[] = (CPListElement[])allCpElements.toArray(new CPListElement[allCpElements.size()]);
				Object prevSuperTypeObject = superTypeField.getValue();
				LibrarySuperType oldSuper = null;
				
				if(prevSuperTypeObject!=null) {
					oldSuper = (LibrarySuperType)prevSuperTypeObject;
				}
				
				
				LibrarySuperType superType = openSuperTypeSelectionDialog(elements,oldSuper);
				
				if(superType!=null && superType!=oldSuper) {
					superTypeField.setValue(superType);
					//List reOrder = fClassPathList.getElements();
					IPath cpEntryPath = superType.getRawContainerPath();
					
					Iterator listItt = allCpElements.iterator();
					CPListElement found = null;
					int foundIndex = -1;
					
					while(listItt.hasNext()) {
						foundIndex++;
						CPListElement o = (CPListElement)listItt.next();
						if(o.getPath().equals(cpEntryPath)) {
							found = o;
							break;
						}
					}
					
					if(found!=null) {
						allCpElements.add(0,allCpElements.remove(foundIndex));
					}
					
					
					fClassPathList.setElements(filterNodes( allCpElements));
					
				}
			}
			
		}
		
	}
	
//	private String[] popupFieldSelectionDialog() {
//		return new String[] {"Window","Basic Browser Library"};
//	}
	
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		superTypeField.setButtonLabel(NewWizardMessages.ClasspathOrderingWorkbookPage_SelectReorder); 
		//superTypeField.setDialogFieldListener(null);
		
		superTypeField.setLabelText(NewWizardMessages.ClasspathOrderingWorkbookPage_SuperType); 

		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fClassPathList, superTypeField }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fClassPathList.getListControl(null));
		//superTypeField.setTextFieldEditable(false);
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fClassPathList.setButtonsMinWidth(buttonBarWidth);
		fSWTControl = composite;
		return composite;
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fClassPathList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements, boolean expand) {
		allCpElements = selElements;
		fClassPathList.selectElements(new StructuredSelection(allCpElements));
	}
	
	private List filterNodes(List elements) {
		ArrayList filter = new ArrayList();
		
		Iterator itt = elements.iterator();
		
		while(itt.hasNext()) {
			Object next = itt.next();
			if(((next instanceof CPListElement) && ((CPListElement)next).isJRE() )) {
				// dont add
			}else if(HIDE_ALL_READONLY_CONTAINERS && (next instanceof CPListElement) && ((CPListElement)next).isInNonModifiableContainer()) {
				// dont add
			}else {
				filter.add(next);
			}
		}
		
		return filter;
		
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#init(org.eclipse.wst.jsdt.core.IJavaScriptProject)
	 */
	public void init(IJavaScriptProject javaProject) {
		fJavaProject = javaProject;
	}

	/**
     * {@inheritDoc}
     */
    public void setFocus() {
    	
    	fClassPathList.setFocus();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#aboutToDispose()
	 */
	public void aboutToDispose() {
		fClassPathList.setElements(allCpElements);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#aboutToShow()
	 */
	public void aboutToShow() {
		allCpElements = fClassPathList.getElements();
		fClassPathList.setElements(filterNodes( fClassPathList.getElements()));
		superTypeField.setText(superTypeField.getValue().toString());
	}
	
	private LibrarySuperType openSuperTypeSelectionDialog(CPListElement[] existingCp, LibrarySuperType existingSuper) {
		LibrarySuperType newLib = BuildPathDialogAccess.chooseSuperType(getShell(), existingCp, existingSuper, fJavaProject);
		return newLib;
	}
	
//	private CPListElement[] openContainerSelectionDialog(CPListElement existing) {
//		if (existing == null) {
//			IIncludePathEntry[] created= BuildPathDialogAccess.chooseContainerEntries(getShell(), fCurrJProject, getRawClasspath());
//			if (created != null) {
//				CPListElement[] res= new CPListElement[created.length];
//				for (int i= 0; i < res.length; i++) {
//					//res[i]= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_CONTAINER, created[i].getPath(), null);
//					res[i]= new CPListElement(fCurrJProject, created[i].getEntryKind(), created[i].getPath(), null);
//				}
//				return res;
//			}
//		} else {
//			IIncludePathEntry created= BuildPathDialogAccess.configureContainerEntry(getShell(), existing.getClasspathEntry(), fCurrJProject, getRawClasspath());
//			if (created != null) {
//				//CPListElement elem= new CPListElement(fCurrJProject, IIncludePathEntry.CPE_CONTAINER, created.getPath(), null);
//				CPListElement elem= new CPListElement(fCurrJProject, created.getEntryKind(), created.getPath(), null);
//				return new CPListElement[] { elem };
//			}
//		}		
//		return null;
//	}

	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaScriptPlugin.getActiveWorkbenchShell();
	}
}
