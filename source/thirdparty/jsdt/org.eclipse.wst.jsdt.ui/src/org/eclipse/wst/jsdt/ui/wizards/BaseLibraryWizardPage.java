/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.wizards;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;

/**
 * Wizard page for adding base library support to the project.
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public class BaseLibraryWizardPage extends NewElementWizardPage implements IJsGlobalScopeContainerPage, IJsGlobalScopeContainerPageExtension, IJsGlobalScopeContainerPageExtension2  {
	
	private static final String CONTAINER_ID="org.eclipse.wst.jsdt.launching.baseBrowserLibrary"; //$NON-NLS-1$
	
	public BaseLibraryWizardPage() {
		super("BaseicLibraryWizzardPage"); //$NON-NLS-1$
		setTitle(NewWizardMessages.BaseLibraryWizardPage_title);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_ADD_LIBRARY);
	}

	public boolean finish() {
		return true;
	}

	public IIncludePathEntry getSelection() {
		System.out.println("Unimplemented method:BaseLibraryWizardPage.getSelection"); //$NON-NLS-1$
		return null;
	}

	public void setSelection(IIncludePathEntry containerEntry) {}

	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		DialogField field = new DialogField();
		
		field.setLabelText(NewWizardMessages.BaseLibraryWizardPage_DefaultBrowserLibraryAdded);
		LayoutUtil.doDefaultLayout(composite, new DialogField[] {field }, false, SWT.DEFAULT, SWT.DEFAULT);
		Dialog.applyDialogFont(composite);
		setControl(composite);
		setDescription(NewWizardMessages.BaseLibraryWizardPage_WebBrowserSupport);
	}

	public void initialize(IJavaScriptProject project, IIncludePathEntry[] currentEntries) {
		// nothing to initialize
	}

	public IIncludePathEntry[] getNewContainers() {
		IIncludePathEntry library = JavaScriptCore.newContainerEntry( new Path(CONTAINER_ID));
		return new IIncludePathEntry[] {library};
	}
}
