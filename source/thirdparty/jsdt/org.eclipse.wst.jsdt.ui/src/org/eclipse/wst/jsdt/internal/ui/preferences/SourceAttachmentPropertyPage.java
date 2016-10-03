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
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

/**
 * Property page to configure a archive's JARs source attachment
 */
public class SourceAttachmentPropertyPage extends PropertyPage implements IStatusChangeListener {

	private SourceAttachmentBlock fSourceAttachmentBlock;
	private IPackageFragmentRoot fRoot;
	private IPath fContainerPath;
	private IIncludePathEntry fEntry;
	public static final String PROP_ID= "org.eclipse.wst.jsdt.ui.propertyPages.SourceAttachmentPage" ; //$NON-NLS-1$
	
	public SourceAttachmentPropertyPage() {
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.SOURCE_ATTACHMENT_PROPERTY_PAGE);
	}		
	
	/*
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite composite) {
		initializeDialogUnits(composite);
		Control result= createPageContent(composite);
		Dialog.applyDialogFont(result);
		return result;
	}
	
	private Control createPageContent(Composite composite) {
		try {
			fContainerPath= null;
			fEntry= null;
			fRoot= getJARPackageFragmentRoot();
			if (fRoot == null || fRoot.getKind() != IPackageFragmentRoot.K_BINARY) {
				return createMessageContent(composite, PreferencesMessages.SourceAttachmentPropertyPage_noarchive_message);  
			}
	
			IPath containerPath= null;
			IJavaScriptProject jproject= fRoot.getJavaScriptProject();
			IIncludePathEntry entry= fRoot.getRawIncludepathEntry();
			if (entry == null) {
				// use a dummy entry to use for initialization
				entry= JavaScriptCore.newLibraryEntry(fRoot.getPath(), null, null);
			} else {
				if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
					containerPath= entry.getPath();
					JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(containerPath.segment(0));
					IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, jproject);
					if (initializer == null || container == null) {
						return createMessageContent(composite, Messages.format(PreferencesMessages.SourceAttachmentPropertyPage_invalid_container, containerPath.toString()));  
					}
					String containerName= container.getDescription();

					IStatus status= initializer.getSourceAttachmentStatus(containerPath, jproject);
					if (status.getCode() == JsGlobalScopeContainerInitializer.ATTRIBUTE_NOT_SUPPORTED) {
						return createMessageContent(composite, Messages.format(PreferencesMessages.SourceAttachmentPropertyPage_not_supported, containerName));  
					}
					if (status.getCode() == JsGlobalScopeContainerInitializer.ATTRIBUTE_READ_ONLY) {
						return createMessageContent(composite, Messages.format(PreferencesMessages.SourceAttachmentPropertyPage_read_only, containerName));  
					}
					entry= JavaModelUtil.findEntryInContainer(container, fRoot.getPath());
					Assert.isNotNull(entry);
				}
			}
			fContainerPath= containerPath;
			fEntry= entry;
			
			fSourceAttachmentBlock= new SourceAttachmentBlock(this, entry);
			return fSourceAttachmentBlock.createControl(composite);				
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
			return createMessageContent(composite, PreferencesMessages.SourceAttachmentPropertyPage_noarchive_message);  
		}
	}
	
	
	private Control createMessageContent(Composite composite, String message) {
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;		
		inner.setLayout(layout);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint= convertWidthInCharsToPixels(80);
		
		Label label= new Label(inner, SWT.LEFT + SWT.WRAP);
		label.setText(message);
		label.setLayoutData(gd);
		return inner;
	}
	

	/*
	 * @see IPreferencePage#performOk
	 */
	public boolean performOk() {
		if (fSourceAttachmentBlock != null) {
			try {
				IIncludePathEntry entry= fSourceAttachmentBlock.getNewEntry();
				if (entry.equals(fEntry)) {
					return true; // no change
				}
				
				IRunnableWithProgress runnable= SourceAttachmentBlock.getRunnable(getShell(), entry, fRoot.getJavaScriptProject(), fContainerPath);		
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
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (fSourceAttachmentBlock != null) {
			fSourceAttachmentBlock.setDefaults();
		}
		super.performDefaults();
	}	
				
	private IPackageFragmentRoot getJARPackageFragmentRoot() throws CoreException {
		// try to find it as Java element (needed for external jars)
		IAdaptable adaptable= getElement();
		IJavaScriptElement elem= (IJavaScriptElement) adaptable.getAdapter(IJavaScriptElement.class);
		if (elem instanceof IPackageFragmentRoot) {
			return (IPackageFragmentRoot) elem;
		}
		// not on classpath or not in a java project
		IResource resource= (IResource) adaptable.getAdapter(IResource.class);
		if (resource instanceof IFile) {
			IProject proj= resource.getProject();
			if (proj.hasNature(JavaScriptCore.NATURE_ID)) {
				IJavaScriptProject jproject= JavaScriptCore.create(proj);
				return jproject.getPackageFragmentRoot(resource);
			}
		}
		return null;		
	}
		

	/*
	 * @see IStatusChangeListener#statusChanged
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}	



}
