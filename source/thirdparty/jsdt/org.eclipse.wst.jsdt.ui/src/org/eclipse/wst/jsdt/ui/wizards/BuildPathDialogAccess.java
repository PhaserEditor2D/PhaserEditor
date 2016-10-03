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
package org.eclipse.wst.jsdt.ui.wizards;

import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.ui.IUIConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.wst.jsdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.JavadocLocationDialog;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.JsGlobalScopeContainerWizard;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.MultipleFolderSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.SourceAttachmentDialog;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Class that gives access to dialogs used by the JavaScript build path page to configure classpath entries
 * and properties of classpath entries.
 * Static methods are provided to show dialogs for:
 * <ul>
 *  <li> configuration of source attachments</li>
 *  <li> configuration of Javadoc locations</li>
 *  <li> configuration and selection of classpath variable entries</li>
 *  <li> configuration and selection of classpath container entries</li>
 *  <li> configuration and selection of JAR and external JAR entries</li>
 *  <li> selection of class and source folders</li>
 * </ul>
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public final class BuildPathDialogAccess {

	private BuildPathDialogAccess() {
		// do not instantiate
	}
	
	/**
	 * Shows the UI for configuring source attachments. <code>null</code> is returned
	 * if the user cancels the dialog. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog
	 * @param initialEntry The entry to edit. The kind of the classpath entry must be either
	 * <code>IIncludePathEntry.CPE_LIBRARY</code> or <code>IIncludePathEntry.CPE_VARIABLE</code>.
	 * @return Returns the resulting classpath entry containing a potentially modified source attachment path and
	 * source attachment root. The resulting entry can be used to replace the original entry on the classpath.
	 * Note that the dialog does not make any changes on the passed entry nor on the classpath that
	 * contains it.
	 */
	public static IIncludePathEntry configureSourceAttachment(Shell shell, IIncludePathEntry initialEntry) {
		if (initialEntry == null) {
			throw new IllegalArgumentException();
		}
		int entryKind= initialEntry.getEntryKind();
		if (entryKind != IIncludePathEntry.CPE_LIBRARY && entryKind != IIncludePathEntry.CPE_VARIABLE) {
			throw new IllegalArgumentException();
		}
		
		SourceAttachmentDialog dialog=  new SourceAttachmentDialog(shell, initialEntry);
		if (dialog.open() == Window.OK) {
			return dialog.getResult();
		}
		return null;
	}
		
	/**
	 * Shows the UI for configuring a javadoc location. <code>null</code> is returned
	 * if the user cancels the dialog. If OK is pressed, an array of length 1 containing the configured URL is
	 * returned. Note that the configured URL can be <code>null</code> when the user
	 * wishes to have no URL location specified. The dialog does not apply any changes.
	 * Use {@link org.eclipse.wst.jsdt.ui.JavaScriptUI} to access and configure
	 * Javadoc locations.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param libraryName Name of of the library to which configured javadoc location belongs.
	 * @param initialURL The initial URL or <code>null</code>.
	 * @return Returns an array of size 1 that contains the resulting javadoc location or
	 * <code>null</code> if the dialog has been canceled. Note that the configured URL can be <code>null</code> when the user
	 * wishes to have no URL location specified.
	 */
	public static URL[] configureJavadocLocation(Shell shell, String libraryName, URL initialURL) {
		if (libraryName == null) {
			throw new IllegalArgumentException();
		}
		
		JavadocLocationDialog dialog=  new JavadocLocationDialog(shell, libraryName, initialURL);
		if (dialog.open() == Window.OK) {
			return new URL[] { dialog.getResult() };
		}
		return null;
	}
	
	/**
	 * Shows the UI for configuring a javadoc location attribute of the classpath entry. <code>null</code> is returned
	 * if the user cancels the dialog. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialEntry The entry to edit. The kind of the classpath entry must be either
	 * <code>IIncludePathEntry.CPE_LIBRARY</code> or <code>IIncludePathEntry.CPE_VARIABLE</code>.
	 * @return Returns the resulting classpath entry containing a potentially modified javadoc location attribute 
	 * The resulting entry can be used to replace the original entry on the classpath.
	 * Note that the dialog does not make any changes on the passed entry nor on the classpath that
	 * contains it.
	 * 
	 * 
	 */
	public static IIncludePathEntry configureJavadocLocation(Shell shell, IIncludePathEntry initialEntry) {
		if (initialEntry == null) {
			throw new IllegalArgumentException();
		}
		int entryKind= initialEntry.getEntryKind();
		if (entryKind != IIncludePathEntry.CPE_LIBRARY && entryKind != IIncludePathEntry.CPE_VARIABLE) {
			throw new IllegalArgumentException();
		}
		
		URL location= JavaScriptUI.getLibraryJSdocLocation(initialEntry);
		JavadocLocationDialog dialog=  new JavadocLocationDialog(shell, initialEntry.getPath().toString(), location);
		if (dialog.open() == Window.OK) {
			CPListElement element= CPListElement.createFromExisting(initialEntry, null);
			URL res= dialog.getResult();
			element.setAttribute(CPListElement.JAVADOC, res != null ? res.toExternalForm() : null);
			return element.getClasspathEntry();
		}
		return null;
	}
	
	/**
	 * Shows the UI to configure a classpath container classpath entry. See {@link IIncludePathEntry#CPE_CONTAINER} for
	 * details about container classpath entries.
	 * The dialog returns the configured classpath entry or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialEntry The initial classpath container entry.
	 * @param project The project the entry belongs to. The project does not have to exist and can also be <code>null</code>.
	 * @param currentClasspath The class path entries currently selected to be set as the projects classpath. This can also
	 * include the entry to be edited. The dialog uses these entries as information only (e.g. to avoid duplicate entries); The user still can make changes after the
	 * the classpath container dialog has been closed. See {@link IJsGlobalScopeContainerPageExtension} for
	 * more information.
	 * @return Returns the configured classpath container entry or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IIncludePathEntry configureContainerEntry(Shell shell, IIncludePathEntry initialEntry, IJavaScriptProject project, IIncludePathEntry[] currentClasspath) {
		if (initialEntry == null || currentClasspath == null) {
			throw new IllegalArgumentException();
		}
		
		JsGlobalScopeContainerWizard wizard= new JsGlobalScopeContainerWizard(initialEntry, project, currentClasspath);
		if (JsGlobalScopeContainerWizard.openWizard(shell, wizard) == Window.OK) {
			IIncludePathEntry[] created= wizard.getNewEntries();
			if (created != null && created.length == 1) {
				return created[0];
			}
		}
		return null;
	}
	
	/**
	 * Shows the UI to choose new classpath container classpath entries. See {@link IIncludePathEntry#CPE_CONTAINER} for
	 * details about container classpath entries.
	 * The dialog returns the selected classpath entries or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param project The project the entry belongs to. The project does not have to exist and
	 * can also be <code>null</code>.
	 * @param currentClasspath The class path entries currently selected to be set as the projects classpath. This can also
	 * include the entry to be edited. The dialog uses these entries as information only; The user still can make changes after the
	 * the classpath container dialog has been closed. See {@link IJsGlobalScopeContainerPageExtension} for
	 * more information.
	 * @return Returns the selected classpath container entries or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IIncludePathEntry[] chooseContainerEntries(Shell shell, IJavaScriptProject project, IIncludePathEntry[] currentClasspath) {
		if (currentClasspath == null) {
			throw new IllegalArgumentException();
		}
		
		JsGlobalScopeContainerWizard wizard= new JsGlobalScopeContainerWizard((IIncludePathEntry) null, project, currentClasspath);
		if (JsGlobalScopeContainerWizard.openWizard(shell, wizard) == Window.OK) {
			return wizard.getNewEntries();
		}
		return null;
	}
	
	
	/**
	 * Shows the UI to configure a JAR or ZIP archive located in the workspace.
	 * The dialog returns the configured classpath entry path or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialEntry The path of the initial archive entry 
	 * @param usedEntries An array of paths that are already on the classpath and therefore should not be
	 * selected again.
	 * @return Returns the configured classpath container entry path or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IPath configureJAREntry(Shell shell, IPath initialEntry, IPath[] usedEntries) {
		if (initialEntry == null || usedEntries == null) {
			throw new IllegalArgumentException();
		}
		
		Class[] acceptedClasses= new Class[] { IFile.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		
		ArrayList usedJars= new ArrayList(usedEntries.length);
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (int i= 0; i < usedEntries.length; i++) {
			IPath curr= usedEntries[i];
			if (!curr.equals(initialEntry)) {
				IResource resource= root.findMember(usedEntries[i]);
				if (resource instanceof IFile) {
					usedJars.add(resource);
				}
			}
		}
		
		IResource existing= root.findMember(initialEntry);
		
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setValidator(validator);
		dialog.setTitle(NewWizardMessages.BuildPathDialogAccess_JARArchiveDialog_edit_title); 
		dialog.setMessage(NewWizardMessages.BuildPathDialogAccess_JARArchiveDialog_edit_description); 
		dialog.addFilter(new ArchiveFileFilter(usedJars, true));
		dialog.setInput(root);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setInitialSelection(existing);

		if (dialog.open() == Window.OK) {
			IResource element= (IResource) dialog.getFirstResult();
			return element.getFullPath();
		}
		return null;
	}
	
	/**
	 * Shows the UI to select new JAR or ZIP archive entries located in the workspace.
	 * The dialog returns the selected entries or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialSelection The path of the element (container or archive) to initially select or <code>null</code> to not select an entry. 
	 * @param usedEntries An array of paths that are already on the classpath and therefore should not be
	 * selected again.
	 * @return Returns the new classpath container entry paths or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IPath[] chooseJAREntries(Shell shell, IPath initialSelection, IPath[] usedEntries) {
		if (usedEntries == null) {
			throw new IllegalArgumentException();
		}
		
		Class[] acceptedClasses= new Class[] { IFile.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
		ArrayList usedJars= new ArrayList(usedEntries.length);
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (int i= 0; i < usedEntries.length; i++) {
			IResource resource= root.findMember(usedEntries[i]);
			if (resource instanceof IFile) {
				usedJars.add(resource);
			}
		}
		IResource focus= initialSelection != null ? root.findMember(initialSelection) : null;
		
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setHelpAvailable(false);
		dialog.setValidator(validator);
		dialog.setTitle(NewWizardMessages.BuildPathDialogAccess_JARArchiveDialog_new_title); 
		dialog.setMessage(NewWizardMessages.BuildPathDialogAccess_JARArchiveDialog_new_description); 
		dialog.addFilter(new ArchiveFileFilter(usedJars, true));
		dialog.setInput(root);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setInitialSelection(focus);

		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();
			IPath[] res= new IPath[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource)elements[i];
				res[i]= elem.getFullPath();
			}
			return res;
		}
		return null;
	}
	
	
	
	
	public static LibrarySuperType chooseSuperType(Shell shell, CPListElement[] cpEntries, LibrarySuperType initialSelection, IJavaScriptProject project) {
		if (cpEntries == null) {
			throw new IllegalArgumentException();
		}
		
//		Class[] acceptedClasses= new Class[] { IFile.class };
//		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
//		ArrayList usedJars= new ArrayList(usedEntries.length);
//		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
//		for (int i= 0; i < usedEntries.length; i++) {
//			IResource resource= root.findMember(usedEntries[i]);
//			if (resource instanceof IFile) {
//				usedJars.add(resource);
//			}
//		}
//		IResource focus= initialSelection != null ? root.findMember(initialSelection) : null;
		ArrayList allLibsSuper = new ArrayList();
		
		for(int i = 0;i<cpEntries.length;i++) {
			LibrarySuperType libSuperParent = new LibrarySuperType(cpEntries[i].getPath(), cpEntries[i].getJavaProject());
			if(libSuperParent.hasChildren()) {
				allLibsSuper.add(libSuperParent);
			}
			//allLibsSuper.addAll(Arrays.asList(libSupers));
		}
		
		boolean currentIsValid = initialSelection!=null && allLibsSuper.contains(initialSelection.getParent());
		
		LibrarySuperType[] libSupers = (LibrarySuperType[])allLibsSuper.toArray(new LibrarySuperType[allLibsSuper.size()]);
		
		
		
		class LibrarySuperTypeContentProvider implements ITreeContentProvider, 
														  ILabelProvider,
														  ISelectionStatusValidator{
			public Object[] getChildren(Object parentElement) {
				if(parentElement==null) return null;
				return ((LibrarySuperType)parentElement).getChildren();
			
			}
			public Object getParent(Object element) {
				if(element==null) return null;
				return ((LibrarySuperType)element).getParent();
			}
			
			public boolean hasChildren(Object element) {
				if(element==null) return false;
				return ((LibrarySuperType)element).hasChildren();
			}
			public Object[] getElements(Object inputElement) {
				if(inputElement instanceof Object[]) return (Object[])inputElement;
				return new Object[] {inputElement}	;
			}
			public void dispose() {}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			public Image getImage(Object element) {
				return null;
//				if(element==null) return null;
//				return ((LibrarySuperType)element).toImage();
			}
			
			public String getText(Object element) {
				if(element==null) return null;
				
				String superTypeName = ((LibrarySuperType)element).getSuperTypeName();
				String libraryName = ((LibrarySuperType)element).getLibraryName();
				if(superTypeName!=null) return superTypeName;
				return libraryName;
			}
			
			public void addListener(ILabelProviderListener listener) {}
			public boolean isLabelProperty(Object element, String property) {return false;}
			public void removeListener(ILabelProviderListener listener) {}
			
			public IStatus validate(Object[] selection) {
				if(selection==null || selection.length!=1) { 
					return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, null, null);
				}else if( ! (selection[0]  instanceof LibrarySuperType)     ){
					return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR,null, null);
				}else if(((LibrarySuperType)selection[0]).isParent()) {
					return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, null, null);
				}
				return new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
			}
			
		}
		LibrarySuperTypeContentProvider libValidator = new LibrarySuperTypeContentProvider();
		ElementTreeSelectionDialog dialog =  new ElementTreeSelectionDialog(shell,libValidator , libValidator);
		dialog.setAllowMultiple(false);
		
		dialog.setHelpAvailable(false);
		dialog.setInput(libSupers);
		dialog.setValidator(libValidator);
		dialog.setTitle(NewWizardMessages.BuildPathDialogAccess_SuperTypeSelection); 
		dialog.setMessage(NewWizardMessages.BuildPathDialogAccess_SelectSupertType); 
		//dialog.addFilter(new ArchiveFileFilter(usedJars, true));
		if(currentIsValid) dialog.setInitialSelection(initialSelection);
	//	dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		//dialog.setInitialSelection(focus);

		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();
			if(elements==null || elements.length==0 || !(elements[0] instanceof LibrarySuperType) ) return null;
			return (LibrarySuperType)elements[0];
			                
//			IPath[] res= new IPath[elements.length];
//			for (int i= 0; i < res.length; i++) {
//				IResource elem= (IResource)elements[i];
//				res[i]= elem.getFullPath();
//			}
//			return res;
		}
		return null;
	}
	
	
	
	
	
	/**
	 * Shows the UI to configure an external JAR or ZIP archive.
	 * The dialog returns the configured or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialEntry The path of the initial archive entry.
	 * @return Returns the configured classpath container entry path or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IPath configureExternalJAREntry(Shell shell, IPath initialEntry) {
		if (initialEntry == null) {
			throw new IllegalArgumentException();
		}
		
		String lastUsedPath= initialEntry.removeLastSegments(1).toOSString();
		
		FileDialog dialog= new FileDialog(shell, SWT.SINGLE);
		dialog.setText(NewWizardMessages.BuildPathDialogAccess_ExtJARArchiveDialog_edit_title); 
		dialog.setFilterExtensions(ArchiveFileFilter.FILTER_EXTENSIONS);
		dialog.setFilterPath(lastUsedPath);
		dialog.setFileName(initialEntry.lastSegment());
		
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		JavaScriptPlugin.getDefault().getDialogSettings().put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());

		return Path.fromOSString(res).makeAbsolute();	
	}
	
	/**
	 * Shows the UI to select new external JAR or ZIP archive entries.
	 * The dialog returns the selected entry paths or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @return Returns the new classpath container entry paths or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IPath[] chooseExternalJAREntries(Shell shell) {
		String lastUsedPath= JavaScriptPlugin.getDefault().getDialogSettings().get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
		if (lastUsedPath == null) {
			lastUsedPath= ""; //$NON-NLS-1$
		}
		FileDialog dialog= new FileDialog(shell, SWT.MULTI);
		dialog.setText(NewWizardMessages.BuildPathDialogAccess_ExtJARArchiveDialog_new_title); 
		dialog.setFilterExtensions(ArchiveFileFilter.FILTER_EXTENSIONS);
		dialog.setFilterPath(lastUsedPath);
		
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
			
		IPath filterPath= Path.fromOSString(dialog.getFilterPath());
		IPath[] elems= new IPath[nChosen];
		for (int i= 0; i < nChosen; i++) {
			elems[i]= filterPath.append(fileNames[i]).makeAbsolute();	
		}
		JavaScriptPlugin.getDefault().getDialogSettings().put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
		
		return elems;
	}
		
	/**
	 * Shows the UI to select new class folders.
	 * The dialog returns the selected classpath entry paths or <code>null</code> if the dialog has
	 * been canceled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialSelection The path of the element to initially select or <code>null</code>.
	 * @param usedEntries An array of paths that are already on the classpath and therefore should not be
	 * selected again.
	 * @return Returns the configured classpath container entry path or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IPath[] chooseClassFolderEntries(Shell shell, IPath initialSelection, IPath[] usedEntries) {
		if (usedEntries == null) {
			throw new IllegalArgumentException();
		}
		String title= NewWizardMessages.BuildPathDialogAccess_ExistingClassFolderDialog_new_title; 
		String message= NewWizardMessages.BuildPathDialogAccess_ExistingClassFolderDialog_new_description; 
		return internalChooseFolderEntry(shell, initialSelection, usedEntries, title, message);
	}
	
	/**
	 * Shows the UI to select new source folders.
	 * The dialog returns the selected classpath entry paths or <code>null</code> if the dialog has
	 * been canceled The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialSelection The path of the element to initially select or <code>null</code>
	 * @param usedEntries An array of paths that are already on the classpath and therefore should not be
	 * selected again.
	 * @return Returns the configured classpath container entry path or <code>null</code> if the dialog has
	 * been canceled by the user.
	 */
	public static IPath[] chooseSourceFolderEntries(Shell shell, IPath initialSelection, IPath[] usedEntries) {
		if (usedEntries == null) {
			throw new IllegalArgumentException();
		}
		String title= NewWizardMessages.BuildPathDialogAccess_ExistingSourceFolderDialog_new_title; 
		String message= NewWizardMessages.BuildPathDialogAccess_ExistingSourceFolderDialog_new_description; 
		return internalChooseFolderEntry(shell, initialSelection, usedEntries, title, message);
	}
	
		
	private static IPath[] internalChooseFolderEntry(Shell shell, IPath initialSelection, IPath[] usedEntries, String title, String message) {	
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };

		ArrayList usedContainers= new ArrayList(usedEntries.length);
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (int i= 0; i < usedEntries.length; i++) {
			IResource resource= root.findMember(usedEntries[i]);
			if (resource instanceof IContainer) {
				usedContainers.add(resource);
			}
		}
		
		IResource focus= initialSelection != null ? root.findMember(initialSelection) : null;
		Object[] used= usedContainers.toArray();
		
		MultipleFolderSelectionDialog dialog= new MultipleFolderSelectionDialog(shell, new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setExisting(used);
		dialog.setTitle(title); 
		dialog.setMessage(message); 
		dialog.setHelpAvailable(false);
		dialog.addFilter(new TypedViewerFilter(acceptedClasses, used));
		dialog.setInput(root);
		dialog.setInitialFocus(focus);
		
		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();
			IPath[] res= new IPath[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource) elements[i];
				res[i]= elem.getFullPath();
			}
			return res;
		}
		return null;		
	}
}
