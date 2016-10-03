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
package org.eclipse.wst.jsdt.internal.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.wizards.NewClassWizardPage;

public class NewClassCreationWizard extends NewElementWizard {

	private NewClassWizardPage fPage;
    private boolean fOpenEditorOnFinish;
	
	public NewClassCreationWizard(NewClassWizardPage page, boolean openEditorOnFinish) {
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWCLASS);
		setDialogSettings(JavaScriptPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.NewClassCreationWizard_title);
		
		fPage= page;
		fOpenEditorOnFinish= openEditorOnFinish;
	}
	
	public NewClassCreationWizard() {
		this(null, true);
	}
	
	/*
	 * @see Wizard#createPages
	 */	
	public void addPages() {
		super.addPages();
		if (fPage == null) {
			fPage= new NewClassWizardPage();
			fPage.init(getSelection());
		}
		addPage(fPage);
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.NewElementWizard#canRunForked()
	 */
	protected boolean canRunForked() {
		return !fPage.isEnclosingTypeSelected();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fPage.createType(monitor); // use the full progress monitor
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		warnAboutTypeCommentDeprecation();
		boolean res= super.performFinish();
		if (res) {
			IResource resource= fPage.getModifiedResource();
			if (resource != null) {
				selectAndReveal(resource);
				if (fOpenEditorOnFinish) {
					openResource((IFile) resource);
				}
			}	
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
	 */
	public IJavaScriptElement getCreatedElement() {
		return fPage.getCreatedType();
	}

}
