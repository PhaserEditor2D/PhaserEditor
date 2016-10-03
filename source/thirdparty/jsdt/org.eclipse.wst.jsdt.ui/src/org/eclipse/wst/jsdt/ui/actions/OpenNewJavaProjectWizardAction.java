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

package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.JavaProjectWizard;

/**
 * <p>Action that opens the new JavaScript project wizard. The action initializes the wizard with the
 * selection as configured by {@link #setSelection(org.eclipse.jface.viewers.IStructuredSelection)} or the selection of
 * the active workbench window.</p>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *  
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class OpenNewJavaProjectWizardAction extends AbstractOpenWizardAction {
	
	/**
	 * Creates an instance of the <code>OpenNewJavaProjectWizardAction</code>.
	 */
	public OpenNewJavaProjectWizardAction() {
		setText(ActionMessages.OpenNewJavaProjectWizardAction_text); 
		setDescription(ActionMessages.OpenNewJavaProjectWizardAction_description); 
		setToolTipText(ActionMessages.OpenNewJavaProjectWizardAction_tooltip); 
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_PROJECT_WIZARD_ACTION);
		setShell(JavaScriptPlugin.getActiveWorkbenchShell());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.actions.AbstractOpenWizardAction#createWizard()
	 */
	protected final INewWizard createWizard() throws CoreException {
		return new JavaProjectWizard();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.actions.AbstractOpenWizardAction#doCreateProjectFirstOnEmptyWorkspace(Shell)
	 */
	protected boolean doCreateProjectFirstOnEmptyWorkspace(Shell shell) {
		return true; // can work on an empty workspace
	}
}
