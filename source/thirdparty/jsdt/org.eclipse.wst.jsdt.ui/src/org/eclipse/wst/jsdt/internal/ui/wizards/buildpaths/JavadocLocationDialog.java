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

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavadocConfigurationBlock;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;

/**
 * Dialog to configure a Javadoc location
 */
public class JavadocLocationDialog extends StatusDialog {

	private JavadocConfigurationBlock fJavadocConfigurationBlock;
	
	/**
	 * Shows the UI for configuring a javadoc location.
	 * Use {@link org.eclipse.wst.jsdt.ui.JavaScriptUI} to access and configure Javadoc locations.
	 * 
	 * @param parent The parent shell for the dialog.
	 * @param libraryName Name of of the library to which configured javadoc location belongs.
	 * @param initialURL The initial URL or <code>null</code>.
	 */
	public JavadocLocationDialog(Shell parent, String libraryName, URL initialURL) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};	
		
		setTitle(Messages.format(NewWizardMessages.LibrariesWorkbookPage_JavadocPropertyDialog_title, libraryName)); 
		fJavadocConfigurationBlock= new JavadocConfigurationBlock(parent, listener, initialURL, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		Control inner= fJavadocConfigurationBlock.createContents(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);		
		return composite;
	}
	
	/**
	 * Returns the configured Javadoc location. The result is only valid after the dialog
	 * has been opened and has not been cancelled by the user.
	 * @return The configured javadoc location
	 */
	public URL getResult() {
		return fJavadocConfigurationBlock.getJavadocLocation();
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.JAVADOC_PROPERTY_DIALOG);
	}
}
