/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.wst.jsdt.internal.ui.util.CoreUtility;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDisposer;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage.NewSourceContainerWorkbookPage;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

public class BuildPathsBlock {

	private CheckedListDialogField fClassPathList;
	
	private StatusInfo fClassPathStatus;
	private StatusInfo fBuildPathStatus;

	private IJavaScriptProject fCurrJSProject;
	
	private IStatusChangeListener fContext;
	private Control fSWTWidget;	
	private TabFolder fTabFolder;
	
	private int fPageIndex;
	
	private BuildPathBasePage fSourceContainerPage;
	private ProjectsWorkbookPage fProjectsPage;
	private LibrariesWorkbookPage fLibrariesPage;
	
	private BuildPathBasePage fCurrPage;
	
	private String fUserSettingsTimeStamp;
	private long fFileTimeStamp;
    
    private IRunnableContext fRunnableContext;
    private boolean fUseNewPage;
    private ClasspathOrderingWorkbookPage ordpage;

	private final IWorkbenchPreferenceContainer fPageContainer; // null when invoked from a non-property page context
	
	private final static int IDX_UP= 0;
	private final static int IDX_DOWN= 1;
	private final static int IDX_TOP= 3;
	private final static int IDX_BOTTOM= 4;
	private final static int IDX_SELECT_ALL= 6;
	private final static int IDX_UNSELECT_ALL= 7;
	
	public BuildPathsBlock(IRunnableContext runnableContext, IStatusChangeListener context, int pageToShow, boolean useNewPage, IWorkbenchPreferenceContainer pageContainer) {
		fPageContainer= pageContainer;
		fContext= context;
		fUseNewPage= useNewPage;
		
		fPageIndex= pageToShow;
		
		fSourceContainerPage= null;
		fLibrariesPage= null;
		fProjectsPage= null;
		fCurrPage= null;
        fRunnableContext= runnableContext;
				
		BuildPathAdapter adapter= new BuildPathAdapter();			
	
		String[] buttonLabels= new String[] {
			/* IDX_UP */ NewWizardMessages.BuildPathsBlock_classpath_up_button, 
			/* IDX_DOWN */ NewWizardMessages.BuildPathsBlock_classpath_down_button, 
			/* 2 */ null,
			/* IDX_TOP */ NewWizardMessages.BuildPathsBlock_classpath_top_button, 
			/* IDX_BOTTOM */ NewWizardMessages.BuildPathsBlock_classpath_bottom_button, 
			/* 5 */ null,
			/* IDX_SELECT_ALL */ NewWizardMessages.BuildPathsBlock_classpath_checkall_button, 
			/* IDX_UNSELECT_ALL */ NewWizardMessages.BuildPathsBlock_classpath_uncheckall_button
		
		};
		
		fClassPathList= new CheckedListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fClassPathList.setDialogFieldListener(adapter);
		fClassPathList.setLabelText(NewWizardMessages.BuildPathsBlock_classpath_label);  
		fClassPathList.setUpButtonIndex(IDX_UP);
		fClassPathList.setDownButtonIndex(IDX_DOWN);
		fClassPathList.setCheckAllButtonIndex(IDX_SELECT_ALL);
		fClassPathList.setUncheckAllButtonIndex(IDX_UNSELECT_ALL);	
	
		fBuildPathStatus= new StatusInfo();
		fClassPathStatus= new StatusInfo();
		
		fCurrJSProject= null;
	}
	
	// -------- UI creation ---------
	
	public Control createControl(Composite parent) {
		fSWTWidget= parent;
		
		Composite composite= new Composite(parent, SWT.NONE);	
		composite.setFont(parent.getFont());
		
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.numColumns= 1;		
		composite.setLayout(layout);
		
		TabFolder folder= new TabFolder(composite, SWT.NONE);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		folder.setFont(composite.getFont());
		
		TabItem item = null;

		// Libraries tab
        fLibrariesPage= new LibrariesWorkbookPage(fClassPathList, fPageContainer);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_tab_scriptimport); 
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_LIBRARY));
		item.setData(fLibrariesPage);
		item.setControl(fLibrariesPage.getControl(folder));
		
		
		// source folders tab
        if (fUseNewPage) {
        		fSourceContainerPage= new NewSourceContainerWorkbookPage(fClassPathList, fRunnableContext, this);
        } else {
			fSourceContainerPage= new SourceContainerWorkbookPage(fClassPathList);
        }
		item= new TabItem(folder, SWT.NONE);
        item.setText(NewWizardMessages.BuildPathsBlock_tab_source); 
        item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT));
		
        item.setData(fSourceContainerPage);     
        item.setControl(fSourceContainerPage.getControl(folder));
		
        
        // project dependency tab
		IWorkbench workbench= JavaScriptPlugin.getDefault().getWorkbench();	
		Image projectImage= workbench.getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		
		fProjectsPage= new ProjectsWorkbookPage(fClassPathList, fPageContainer);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_tab_projects); 
		item.setImage(projectImage);
		item.setData(fProjectsPage);
		item.setControl(fProjectsPage.getControl(folder));
		
		
		//global supertype tab
		Image cpoImage= JavaPluginImages.DESC_TOOL_CLASSPATH_ORDER.createImage();
		composite.addDisposeListener(new ImageDisposer(cpoImage));	
		
		ordpage= new ClasspathOrderingWorkbookPage(fClassPathList);
		
		/* init super type field with either default or the one defined for the project */
		ordpage.getSuperField().setValue(getProjectSuperType(fCurrJSProject));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_GlobalOrder); 
		item.setImage(cpoImage);
		item.setData(ordpage);
		item.setControl(ordpage.getControl(folder));

				
		 //a non shared image
		if (fCurrJSProject != null) {
			fSourceContainerPage.init(fCurrJSProject);
			fLibrariesPage.init(fCurrJSProject);
			fProjectsPage.init(fCurrJSProject);
			ordpage.init(fCurrJSProject);
			
		}
		
		if(fPageIndex < folder.getItems().length) {
			folder.setSelection(fPageIndex);
			fCurrPage= (BuildPathBasePage) folder.getItem(fPageIndex).getData();
		}
		folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tabChanged(e.item);
			}	
		});
		fTabFolder= folder;

		Dialog.applyDialogFont(composite);
		return composite;
	}
	
//	private Shell getShell() {
//		if (fSWTWidget != null) {
//			return fSWTWidget.getShell();
//		}
//		return JavaScriptPlugin.getActiveWorkbenchShell();
//	}
	
	/**
	 * Initializes the classpath for the given project. Multiple calls to init are allowed,
	 * but all existing settings will be cleared and replace by the given or default paths.
	 * @param jproject The java project to configure. Does not have to exist.
	 * @param outputLocation The output location to be set in the page. If <code>null</code>
	 * is passed, jdt default settings are used, or - if the project is an existing Java project- the
	 * output location of the existing project 
	 * @param classpathEntries The classpath entries to be set in the page. If <code>null</code>
	 * is passed, jdt default settings are used, or - if the project is an existing Java project - the
	 * classpath entries of the existing project
	 */	
	// public void init(IJavaScriptProject jproject, IPath outputLocation, IIncludePathEntry[] classpathEntries) {
	public void init(IJavaScriptProject jproject,IIncludePathEntry[] classpathEntries) {
		fCurrJSProject= jproject;
		boolean projectExists= false;
		List newClassPath= null;
		IProject project= fCurrJSProject.getProject();
		projectExists= (project.exists() && jproject.getJSDTScopeFile().exists()); //$NON-NLS-1$
		if  (projectExists) {
//			if (outputLocation == null) {
//				outputLocation=  fCurrJProject.readOutputLocation();
//			}
			if (classpathEntries == null) {
				classpathEntries=  fCurrJSProject.readRawIncludepath();
			}
		}
////		if (outputLocation == null) {
////			outputLocation= getDefaultOutputLocation(jproject);
////		}			
//
		if (classpathEntries != null) {
			newClassPath= getExistingEntries(classpathEntries);
		}
		if (newClassPath == null) {
			newClassPath= getDefaultClassPath(jproject);
		}
		
		List exportedEntries = new ArrayList();
		for (int i= 0; i < newClassPath.size(); i++) {
			CPListElement curr= (CPListElement) newClassPath.get(i);
			if (curr.isExported() || curr.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
				exportedEntries.add(curr);
			}
		}
		
		// inits the dialog field
		//fBuildPathDialogField.setText(outputLocation.makeRelative().toString());
		//fBuildPathDialogField.enableButton(project.exists());
		
		fClassPathList.setElements(newClassPath);
		fClassPathList.setCheckedElements(exportedEntries);
		
		fClassPathList.selectFirstElement();
		
		if (fSourceContainerPage != null) {
			fSourceContainerPage.init(fCurrJSProject);
			fProjectsPage.init(fCurrJSProject);
			fLibrariesPage.init(fCurrJSProject);
		}
		
		initializeTimeStamps();
		updateUI();
	}
	
	protected void updateUI() {
		if (fSWTWidget == null || fSWTWidget.isDisposed()) {
			return;
		}
		
		if (Display.getCurrent() != null) {
			doUpdateUI();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (fSWTWidget == null || fSWTWidget.isDisposed()) {
						return;
					}
					doUpdateUI();
				}
			});
		}
	}

	public void setSuperType(LibrarySuperType type) {
		ordpage.getSuperField().setValue(type);
		
	}
	
	protected void doUpdateUI() {
	//	fBuildPathDialogField.refresh();
		fClassPathList.refresh();
	
		doStatusLineUpdate();
	}
	
	private String getEncodedSettings() {
		StringBuffer buf= new StringBuffer();	
		//CPListElement.appendEncodePath(fOutputLocationPath, buf).append(';');

		int nElements= fClassPathList.getSize();
		buf.append('[').append(nElements).append(']');
		for (int i= 0; i < nElements; i++) {
			CPListElement elem= (CPListElement) fClassPathList.getElement(i);
			elem.appendEncodedSettings(buf);
		}
		return buf.toString();
	}
	
	public boolean hasChangesInSuper() {
		LibrarySuperType savedSuperType = getProjectSuperType(fCurrJSProject);
		
		Object o = ordpage.getSuperField().getValue();
		
		if(o!=null && !o.equals(savedSuperType)) return true;
		return false;
	}
	
	public boolean hasChangesInDialog() {
		
		
		
		String currSettings= getEncodedSettings();
		return !currSettings.equals(fUserSettingsTimeStamp);
	}
	
	public void aboutToDispose() {
		if(fCurrPage!=null) fCurrPage.aboutToDispose();
	}
	
	public void aboutToShow() {
		if(fCurrPage!=null) fCurrPage.aboutToShow();
	}
	public boolean hasChangesInClasspathFile() {
		IFile file= fCurrJSProject.getJSDTScopeFile(); //$NON-NLS-1$
		return fFileTimeStamp != file.getModificationStamp();
	}
	
	public boolean isClassfileMissing() {
		return !fCurrJSProject.getJSDTScopeFile().exists(); //$NON-NLS-1$
	}
	
	public void initializeTimeStamps() {
		IFile file= fCurrJSProject.getJSDTScopeFile(true); //$NON-NLS-1$
		fFileTimeStamp= file.getModificationStamp();
		fUserSettingsTimeStamp= getEncodedSettings();
	}

	private ArrayList getExistingEntries(IIncludePathEntry[] classpathEntries) {
		ArrayList newClassPath= new ArrayList();
		for (int i= 0; i < classpathEntries.length; i++) {
			IIncludePathEntry curr= classpathEntries[i];
			newClassPath.add(CPListElement.createFromExisting(curr, fCurrJSProject));
		}
		return newClassPath;
	}
	
	// -------- public api --------
	
	/**
	 * @return Returns the Java project. Can return <code>null<code> if the page has not
	 * been initialized.
	 */
	public IJavaScriptProject getJavaProject() {
		return fCurrJSProject;
	}
	
	/**
	 *  @return Returns the current output location. Note that the path returned must not be valid.
	 */	
	public IPath getOutputLocation() {
		//return new Path(fBuildPathDialogField.getText()).makeAbsolute();
		return new Path(""); //$NON-NLS-1$
	}
	
	/**
	 *  @return Returns the current class path (raw). Note that the entries returned must not be valid.
	 */	
	public IIncludePathEntry[] getRawClassPath() {
		List elements=  fClassPathList.getElements();
		int nElements= elements.size();
		IIncludePathEntry[] entries= new IIncludePathEntry[elements.size()];

		for (int i= 0; i < nElements; i++) {
			CPListElement currElement= (CPListElement) elements.get(i);
			entries[i]= currElement.getClasspathEntry();
		}
		return entries;
	}
	
	public int getPageIndex() {
		return fPageIndex;
	}
	
	
	// -------- evaluate default settings --------
	private List getDefaultClassPath(IJavaScriptProject jproj) {
		List list= new ArrayList();
		IResource srcFolder;
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		String sourceFolderName= store.getString(PreferenceConstants.SRCBIN_SRCNAME);
		if (store.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ) && sourceFolderName.length() > 0) {
			srcFolder= jproj.getProject().getFolder(sourceFolderName);
		} else {
			srcFolder= jproj.getProject();
		}

		list.add(new CPListElement(jproj, IIncludePathEntry.CPE_SOURCE, srcFolder.getFullPath(), srcFolder));

		IIncludePathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
		list.addAll(getExistingEntries(jreEntries));
		return list;
	}	
//	private List getDefaultClassPath(IJavaScriptProject jproj) {
//		List list= new ArrayList();
//
//
//		
//
//		IIncludePathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
//		list.addAll(getExistingEntries(jreEntries));
//		CPListElement projectSourceRoot = new CPListElement(jproj, IIncludePathEntry.CPE_SOURCE,jproj.getProject().getFullPath(),jproj.getProject());
//				
//		
//		projectSourceRoot.setAttribute(CPListElement.EXCLUSION, (new IPath[] {new Path("*/*/**")}));
//		
//		
//		
//		list.add(projectSourceRoot);
//		return list;
//	}
//	
	
	public static LibrarySuperType getProjectSuperType(IJavaScriptProject jproj) {
		if(jproj==null) {
			return getDefaultSuperType(jproj);
		}
		JavaProject javaProject = ((JavaProject)jproj);
		
		//String superTypeName =null;
		//String superTypeContainer =null;	
		LibrarySuperType projectSuperType = null;
	//	try {
			projectSuperType = javaProject.getCommonSuperType();
			//superTypeName = javaProject.getSharedProperty(LibrarySuperType.SUPER_TYPE_NAME);
			//superTypeContainer = javaProject.getSharedProperty(LibrarySuperType.SUPER_TYPE_CONTAINER);
	//	} catch (CoreException ex) {
			// TODO Auto-generated catch block
		//	ex.printStackTrace();
		//}
//	/	IPreferenceStore store= PreferenceConstants.getPreferenceStore();
//		String superTypeContainerPath= store.getString(PreferenceConstants.SUPER_TYPE_CONTAINER);
//		String superTypeName= store.getString(PreferenceConstants.SUPER_TYPE_NAME);
		//if(superTypeName==null || superTypeContainer==null || superTypeName.equals("") ) {
		if(projectSuperType==null) {
			LibrarySuperType defaultSt =getDefaultSuperType(jproj); 
			setProjectSuperType(jproj, defaultSt);
			return defaultSt;
		}
		return projectSuperType;
		//return new LibrarySuperType(new Path(superTypeContainer),jproj, superTypeName);
//		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
//		if (store.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ)) {
//			String outputLocationName= store.getString(PreferenceConstants.SRCBIN_BINNAME);
//			return jproj.getProject().getFullPath().append(outputLocationName);
//		} else {
//			return jproj.getProject().getFullPath();
//		}
	}	
	
	public static LibrarySuperType getDefaultSuperType(IJavaScriptProject jproj) {
		IPath JREPath = new Path(JavaRuntime.DEFAULT_SUPER_TYPE_LIBRARY);
		String superTypeName = JavaRuntime.DEFAULT_SUPER_TYPE;
		
		return new LibrarySuperType(JREPath, jproj, superTypeName);
	}	
	
	public static void setProjectSuperType(IJavaScriptProject jproj, LibrarySuperType superType) {
		JavaProject javaScriptProject = ((JavaProject)jproj);
		javaScriptProject.setCommonSuperType(superType);	
	}	

	
	private class BuildPathAdapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {

		// -------- IStringButtonAdapter --------
		public void changeControlPressed(DialogField field) {
			buildPathChangeControlPressed(field);
		}
		
		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			buildPathDialogFieldChanged(field);
		}

		// ---------- IListAdapter --------
		public void customButtonPressed(ListDialogField field, int index) {
			buildPathCustomButtonPressed(field, index);
		}

		public void doubleClicked(ListDialogField field) {
		}

		public void selectionChanged(ListDialogField field) {
			List selected = field.getSelectedElements();
			if(selected==null) {
				 enableButtons();
				return;
			}
			
			if(selected.size()!=1 ) {
				disableButtons();
				return;
			}
			
			Object selection = selected.get(0);
			int selctedIndext = field.getIndexOfElement(selection);
			
			if(selctedIndext == 0) {
				disableButtons();
				return;
			}else {
				enableButtons();
			}
			
			
			updateTopButtonEnablement();
		}
	}
	
	public void disableButtons() {
		fClassPathList.enableButton(IDX_BOTTOM, false);
		fClassPathList.enableButton(IDX_TOP, false);
		fClassPathList.enableButton(IDX_UP, false);
		fClassPathList.enableButton(IDX_DOWN, false);
	
	}
	
	public void enableButtons() {
		fClassPathList.enableButton(IDX_BOTTOM, fClassPathList.canMoveDown());
		fClassPathList.enableButton(IDX_TOP, fClassPathList.canMoveUp());
		fClassPathList.enableButton(IDX_UP, true);
		fClassPathList.enableButton(IDX_DOWN, true);
		
	}
	
	private void buildPathChangeControlPressed(DialogField field) {
//		if (field == fBuildPathDialogField) {
//			IContainer container= chooseContainer();
//			if (container != null) {
//				fBuildPathDialogField.setText(container.getFullPath().toString());
//			}
//		}
	}
	
	public void updateTopButtonEnablement() {
		fClassPathList.enableButton(IDX_BOTTOM, fClassPathList.canMoveDown());
		fClassPathList.enableButton(IDX_TOP, fClassPathList.canMoveUp());
		
	}

	public void buildPathCustomButtonPressed(ListDialogField field, int index) {
		List elems= field.getSelectedElements();
		field.removeElements(elems);
		if (index == IDX_BOTTOM) {
			field.addElements(elems);
		} else if (index == IDX_TOP) {
			field.addElements(elems, 0);
		}
	}

	private void buildPathDialogFieldChanged(DialogField field) {
		if (field == fClassPathList) {
			updateClassPathStatus();
			updateTopButtonEnablement();
		}
//		else if (field == fBuildPathDialogField) {
//			updateOutputLocationStatus();
//		}
		doStatusLineUpdate();
	}	
	

	
	// -------- verification -------------------------------
	
	private void doStatusLineUpdate() {
		if (Display.getCurrent() != null) {
			IStatus res= findMostSevereStatus();
			fContext.statusChanged(res);
		}
	}
	
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fClassPathStatus, fBuildPathStatus });
	}
	
	
	/**
	 * Validates the build path.
	 */
	public void updateClassPathStatus() {
		fClassPathStatus.setOK();
		
		List elements= fClassPathList.getElements();
	
		CPListElement entryMissing= null;
		CPListElement entryDeprecated= null;
		int nEntriesMissing= 0;
		IIncludePathEntry[] entries= new IIncludePathEntry[elements.size()];

		for (int i= elements.size()-1 ; i >= 0 ; i--) {
			CPListElement currElement= (CPListElement)elements.get(i);
			boolean isChecked= fClassPathList.isChecked(currElement);
			if (currElement.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
				if (!isChecked) {
					fClassPathList.setCheckedWithoutUpdate(currElement, true);
				}
				if (!fClassPathList.isGrayed(currElement)) {
					fClassPathList.setGrayedWithoutUpdate(currElement, true);
				}
			} else {
				currElement.setExported(isChecked);
			}

			entries[i]= currElement.getClasspathEntry();
			if (currElement.isMissing()) {
				nEntriesMissing++;
				if (entryMissing == null) {
					entryMissing= currElement;
				}
			}
			if (entryDeprecated == null & currElement.isDeprecated()) {
				entryDeprecated= currElement;
			}
		}
				
		if (nEntriesMissing > 0) {
			if (nEntriesMissing == 1) {
				fClassPathStatus.setWarning(Messages.format(NewWizardMessages.BuildPathsBlock_warning_EntryMissing, entryMissing.getPath().toString())); 
			} else {
				fClassPathStatus.setWarning(Messages.format(NewWizardMessages.BuildPathsBlock_warning_EntriesMissing, String.valueOf(nEntriesMissing))); 
			}
		} else if (entryDeprecated != null) {
			fClassPathStatus.setInfo(entryDeprecated.getDeprecationMessage());
		}
				
/*		if (fCurrJProject.hasClasspathCycle(entries)) {
			fClassPathStatus.setWarning(NewWizardMessages.getString("BuildPathsBlock.warning.CycleInClassPath")); //$NON-NLS-1$
		}
*/		
		updateBuildPathStatus();
	}
		
	private void updateBuildPathStatus() {
		List elements= fClassPathList.getElements();
		IIncludePathEntry[] entries= new IIncludePathEntry[elements.size()];
	
		for (int i= elements.size()-1 ; i >= 0 ; i--) {
			CPListElement currElement= (CPListElement)elements.get(i);
			entries[i]= currElement.getClasspathEntry();
		}
		
		IJavaScriptModelStatus status= JavaScriptConventions.validateClasspath(fCurrJSProject, entries);
		if (!status.isOK()) {
			fBuildPathStatus.setError(status.getMessage());
			return;
		}
		fBuildPathStatus.setOK();
	}
	
	// -------- creation -------------------------------
	
	public static void createProject(IProject project, URI locationURI, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}				
		monitor.beginTask(NewWizardMessages.BuildPathsBlock_operationdesc_project, 10); 

		// create the project
		try {
			if (!project.exists()) {
				IProjectDescription desc= project.getWorkspace().newProjectDescription(project.getName());
				if (locationURI != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(locationURI)) {
					locationURI= null;
				}
				desc.setLocationURI(locationURI);
				project.create(desc, monitor);
				monitor= null;
			}
			if (!project.isOpen()) {
				project.open(monitor);
				monitor= null;
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	public static void addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (!project.hasNature(JavaScriptCore.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures= description.getNatureIds();
			String[] newNatures= new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length]= JavaScriptCore.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		} else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}

	
	public void configureJavaProject(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		aboutToDispose();
		flush(fClassPathList.getElements(),  getJavaProject(), getSuperType(), monitor);
		initializeTimeStamps();
		
		updateUI();
	}
    
	public LibrarySuperType getSuperType() {

		Object o = ordpage.getSuperField().getValue();
		
		return (LibrarySuperType)o;
	}
	
	/*
	 * Creates the Java project and sets the configured build path.
	 * If the project already exists only build paths are updated.
	 */
	public static void flush(List classPathEntries, IJavaScriptProject javaScriptProject, LibrarySuperType superType, IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if(superType==null) {
			System.out.println("---------------------------------- NULL SUPER TYPE -------------------------"); //$NON-NLS-1$
		}
		if (superType != null) {
			setProjectSuperType(javaScriptProject, superType);
		}
		
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.setTaskName(NewWizardMessages.BuildPathsBlock_operationdesc_java); 
		monitor.beginTask("", classPathEntries.size() * 4 + 4); //$NON-NLS-1$
		
		try {
			IProject project= javaScriptProject.getProject();
			IPath projPath= project.getFullPath();
			
			monitor.worked(1);
			
			//IWorkspaceRoot fWorkspaceRoot= JavaScriptPlugin.getWorkspace().getRoot();
			
			monitor.worked(1);
			
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			
			IIncludePathEntry[] classpath= new IIncludePathEntry[classPathEntries.size()];
			
			int i= 0;
			for (Iterator iter= classPathEntries.iterator(); iter.hasNext();) {
				CPListElement entry= (CPListElement)iter.next();
				classpath[i]= entry.getClasspathEntry();
				i++;
				
				IResource res= entry.getResource();
				//1 tick
				if (res instanceof IFolder && entry.getLinkTarget() == null && !res.exists()) {
					CoreUtility.createFolder((IFolder)res, true, true, new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				
				//3 ticks
				if (entry.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
					monitor.worked(1);
					
					IPath path= entry.getPath();
					if (projPath.equals(path)) {
						monitor.worked(2);
						continue;	
					}
					
					if (projPath.isPrefixOf(path)) {
						path= path.removeFirstSegments(projPath.segmentCount());
					}
					IFolder folder= project.getFolder(path);
					IPath orginalPath= entry.getOrginalPath();
					if (orginalPath == null) {
						if (!folder.exists()) {
							//New source folder needs to be created
							if (entry.getLinkTarget() == null) {
								CoreUtility.createFolder(folder, true, true, new SubProgressMonitor(monitor, 2));
							} else {
								folder.createLink(entry.getLinkTarget(), IResource.ALLOW_MISSING_LOCAL, new SubProgressMonitor(monitor, 2));
							}
						}
					} else {
						if (projPath.isPrefixOf(orginalPath)) {
							orginalPath= orginalPath.removeFirstSegments(projPath.segmentCount());
						}
						IFolder orginalFolder= project.getFolder(orginalPath);
						if (entry.getLinkTarget() == null) {
							if (!folder.exists()) {
								//Source folder was edited, move to new location
								IPath parentPath= entry.getPath().removeLastSegments(1);
								if (projPath.isPrefixOf(parentPath)) {
									parentPath= parentPath.removeFirstSegments(projPath.segmentCount());
								}
								if (parentPath.segmentCount() > 0) {
									IFolder parentFolder= project.getFolder(parentPath);
									if (!parentFolder.exists()) {
										CoreUtility.createFolder(parentFolder, true, true, new SubProgressMonitor(monitor, 1));
									} else {
										monitor.worked(1);
									}
								} else {
									monitor.worked(1);
								}
								orginalFolder.move(entry.getPath(), true, true, new SubProgressMonitor(monitor, 1));
							}
						} else {
							if (!folder.exists() || !entry.getLinkTarget().equals(entry.getOrginalLinkTarget())) {
								orginalFolder.delete(true, new SubProgressMonitor(monitor, 1));
								folder.createLink(entry.getLinkTarget(), IResource.ALLOW_MISSING_LOCAL, new SubProgressMonitor(monitor, 1));
							}
						}
					}
				} else {
					monitor.worked(3);
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			
			javaScriptProject.setRawIncludepath(classpath, new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
	}
	
	// -------- tab switching ----------
	
	private void tabChanged(Widget widget) {
		if (widget instanceof TabItem) {
			TabItem tabItem= (TabItem) widget;
			BuildPathBasePage newPage= (BuildPathBasePage) tabItem.getData();
			if (fCurrPage != null) {
				List selection= fCurrPage.getSelection();
				if (!selection.isEmpty()) {
					newPage.setSelection(selection, false);
				}
				fCurrPage.aboutToDispose();
				newPage.aboutToShow();
			}
			
			fCurrPage= newPage;
			fPageIndex= tabItem.getParent().getSelectionIndex();
		}
	}
	
	private int getPageIndex(int entryKind) {
		switch (entryKind) {
			case IIncludePathEntry.CPE_CONTAINER:
			case IIncludePathEntry.CPE_LIBRARY:
			case IIncludePathEntry.CPE_VARIABLE:
				return 2;
			case IIncludePathEntry.CPE_PROJECT:
				return 1;
			case IIncludePathEntry.CPE_SOURCE:
				return 0;
		}
		return 0;
	}
	
	private CPListElement findElement(IIncludePathEntry entry) {
		for (int i= 0, len= fClassPathList.getSize(); i < len; i++) {
			CPListElement curr= (CPListElement) fClassPathList.getElement(i);
			if (curr.getEntryKind() == entry.getEntryKind() && curr.getPath().equals(entry.getPath())) {
				return curr;
			}
		}
		return null;
	}
	
	public void setElementToReveal(IIncludePathEntry entry, String attributeKey) {
		int pageIndex= getPageIndex(entry.getEntryKind());
		if (fTabFolder == null) {
			fPageIndex= pageIndex;
		} else {
			fTabFolder.setSelection(pageIndex);
			CPListElement element= findElement(entry);
			if (element != null) {
				Object elementToSelect= element;
				
				if (attributeKey != null) {
					Object attrib= element.findAttributeElement(attributeKey);
					if (attrib != null) {
						elementToSelect= attrib;
					}
				}
				BuildPathBasePage page= (BuildPathBasePage) fTabFolder.getItem(pageIndex).getData();
				List selection= new ArrayList(1);
				selection.add(elementToSelect);
				page.setSelection(selection, true);
			}	
		}
	}
	
	public void showPage(int pageIndex) {
		if (fTabFolder == null) {
			fPageIndex= pageIndex;
		} else {
			fTabFolder.setSelection(pageIndex);
			fCurrPage= (BuildPathBasePage)fTabFolder.getItem(pageIndex).getData();
			fCurrPage.aboutToShow();
				//BuildPathBasePage page= (BuildPathBasePage) fTabFolder.getItem(pageIndex).getData();
			}	
		updateUI();
	}
	
	
	public void addElement(IIncludePathEntry entry) {
		int pageIndex= getPageIndex(entry.getEntryKind());
		if (fTabFolder == null) {
			fPageIndex= pageIndex;
		} else {
			fTabFolder.setSelection(pageIndex);

			Object page=  fTabFolder.getItem(pageIndex).getData();
			if (page instanceof LibrariesWorkbookPage) {
				CPListElement element= CPListElement.createFromExisting(entry, fCurrJSProject);
				((LibrariesWorkbookPage) page).addElement(element);
			}
		}
	}

	public void dispose() {
		if (fSourceContainerPage instanceof NewSourceContainerWorkbookPage) {
			((NewSourceContainerWorkbookPage)fSourceContainerPage).dispose();
			fSourceContainerPage= null;
		}
    }

	public boolean isOKStatus() {
	    return findMostSevereStatus().isOK();
    }

	public void setFocus() {
		fSourceContainerPage.setFocus();
    }
}
