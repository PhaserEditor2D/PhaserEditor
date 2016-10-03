/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mickael Istria (Red Hat Inc.) - 426209 Java 6 + Warnings cleanup
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Property page used to set the project's Javadoc location for sources
 */
public class JavadocConfigurationPropertyPage extends PropertyPage implements IStatusChangeListener {

	public static final String PROP_ID= "org.eclipse.wst.jsdt.ui.propertyPages.JavadocConfigurationPropertyPage"; //$NON-NLS-1$
	
	private JavadocConfigurationBlock fJavadocConfigurationBlock;
	private boolean fIsValidElement;
	
	private IPath fContainerPath;
	private IIncludePathEntry fEntry;
	private URL fInitalLocation;

	public JavadocConfigurationPropertyPage() {
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		IJavaScriptElement elem= getJavaElement();
		try {
			if (elem instanceof IPackageFragmentRoot && ((IPackageFragmentRoot) elem).getKind() == IPackageFragmentRoot.K_BINARY) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) elem;
				
				IIncludePathEntry entry= root.getRawIncludepathEntry();
				if (entry == null) {
					fIsValidElement= false;
					setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsIncorrectElement_description);
				} else {
					if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
						fContainerPath= entry.getPath();
						fEntry= handleContainerEntry(fContainerPath, elem.getJavaScriptProject(), root.getPath());
						fIsValidElement= fEntry != null;
					} else {
						fContainerPath= null;
						fEntry= entry;
						fIsValidElement= true;
						setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsPackageFragmentRoot_description); 
					}
				}

			} else if (elem instanceof IJavaScriptProject) {
				fIsValidElement= true;
				setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsJavaProject_description); 
			} else {
				fIsValidElement= false;
				setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsIncorrectElement_description);
			}
		} catch (JavaScriptModelException e) {
			fIsValidElement= false;
			setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsIncorrectElement_description);
		}
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.JAVADOC_CONFIGURATION_PROPERTY_PAGE);
	}
	
	private IIncludePathEntry handleContainerEntry(IPath containerPath, IJavaScriptProject jproject, IPath jarPath) throws JavaScriptModelException {
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(containerPath.segment(0));
		IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, jproject);
		if (initializer == null || container == null) {
			setDescription(Messages.format(PreferencesMessages.JavadocConfigurationPropertyPage_invalid_container, containerPath.toString()));
			return null;
		}
		String containerName= container.getDescription();
		IStatus status= initializer.getAttributeStatus(containerPath, jproject, IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME);
		if (status.getCode() == JsGlobalScopeContainerInitializer.ATTRIBUTE_NOT_SUPPORTED) {
			setDescription(Messages.format(PreferencesMessages.JavadocConfigurationPropertyPage_not_supported, containerName));
			return null;
		}
		if (status.getCode() == JsGlobalScopeContainerInitializer.ATTRIBUTE_READ_ONLY) {
			setDescription(Messages.format(PreferencesMessages.JavadocConfigurationPropertyPage_read_only, containerName));
			return null;
		}
		IIncludePathEntry entry= JavaModelUtil.findEntryInContainer(container, jarPath);
		Assert.isNotNull(entry);
		setDescription(PreferencesMessages.JavadocConfigurationPropertyPage_IsPackageFragmentRoot_description); 
		return entry;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		if (!fIsValidElement) {
			return new Composite(parent, SWT.NONE);
		}
		
		IJavaScriptElement elem= getJavaElement();
		fInitalLocation= null;
		if (elem != null) {
			try {
				fInitalLocation= JavaScriptUI.getJSdocBaseLocation(elem);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		
		boolean isProject= (elem instanceof IJavaScriptProject);
		fJavadocConfigurationBlock= new JavadocConfigurationBlock(getShell(), this, fInitalLocation, isProject);
		Control control= fJavadocConfigurationBlock.createContents(parent);
		control.setVisible(elem != null);

		Dialog.applyDialogFont(control);
		return control;
	}

	private IJavaScriptElement getJavaElement() {
		IAdaptable adaptable= getElement();
		IJavaScriptElement elem= (IJavaScriptElement) adaptable.getAdapter(IJavaScriptElement.class);
		if (elem == null) {

			IResource resource= (IResource) adaptable.getAdapter(IResource.class);
			//special case when the .jar is a file
			try {
				if (resource instanceof IFile && ArchiveFileFilter.isArchivePath(resource.getFullPath())) {
					IProject proj= resource.getProject();
					if (proj.hasNature(JavaScriptCore.NATURE_ID)) {
						IJavaScriptProject jproject= JavaScriptCore.create(proj);
						elem= jproject.getPackageFragmentRoot(resource); // create a handle
					}
				}
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return elem;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (fJavadocConfigurationBlock != null) {
			fJavadocConfigurationBlock.performDefaults();
		}
		super.performDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (fJavadocConfigurationBlock != null) {
			URL javadocLocation= fJavadocConfigurationBlock.getJavadocLocation();
			if ((javadocLocation == null && fInitalLocation == null) ||
				(javadocLocation != null && fInitalLocation != null && javadocLocation.toString().equals(fInitalLocation.toString()))) {
				return true; // no change
			}
			
			
			IJavaScriptElement elem= getJavaElement();
			try {
				IRunnableWithProgress runnable= getRunnable(getShell(), elem, javadocLocation, fEntry, fContainerPath);
				PlatformUI.getWorkbench().getProgressService().run(true, true, runnable);
			} catch (InvocationTargetException e) {
				String title= PreferencesMessages.SourceAttachmentPropertyPage_error_title; 
				String message= PreferencesMessages.SourceAttachmentPropertyPage_error_message; 
				ExceptionHandler.handle(e, getShell(), title, message);
				return false;
			} catch (InterruptedException e) {
				// cancelled
				return false;
			}
		}
		return true;
	}
	
	
	private static IRunnableWithProgress getRunnable(final Shell shell, final IJavaScriptElement elem, final URL javadocLocation, final IIncludePathEntry entry, final IPath containerPath) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {				
				try {
					IJavaScriptProject project= elem.getJavaScriptProject();
					if (elem instanceof IPackageFragmentRoot) {
						CPListElement cpElem= CPListElement.createFromExisting(entry, project);
						String loc= javadocLocation != null ? javadocLocation.toExternalForm() : null;
						cpElem.setAttribute(CPListElement.JAVADOC, loc);
						IIncludePathEntry newEntry= cpElem.getClasspathEntry();
						String[] changedAttributes= { CPListElement.JAVADOC };
						BuildPathSupport.modifyClasspathEntry(shell, newEntry, changedAttributes, project, containerPath, monitor);
					} else {
						JavaScriptUI.setProjectJSdocLocation(project, javadocLocation);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
	}


	/**
	 * @see IStatusChangeListener#statusChanged(IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

}
