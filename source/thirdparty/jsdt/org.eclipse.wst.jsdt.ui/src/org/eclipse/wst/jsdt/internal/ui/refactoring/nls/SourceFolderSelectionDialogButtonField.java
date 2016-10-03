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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaSourcePackageFragmentRootCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

class SourceFolderSelectionDialogButtonField extends StringButtonDialogField implements IDialogFieldListener {

	private IPackageFragmentRoot fRoot;
	private SourceChangeListener fListener;
	private IDialogFieldListener fUpdateListener;

	public SourceFolderSelectionDialogButtonField(String descriptionLabel, String browseLabel, IStringButtonAdapter adapter) {
		super(adapter);
		setContentAssistProcessor(new JavaSourcePackageFragmentRootCompletionProcessor());
		setLabelText(descriptionLabel);
		setButtonLabel(browseLabel);
		setDialogFieldListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField)
	 */
	public void dialogFieldChanged(DialogField field) {
		// propagate a textchange to the fragment root of this
		setRoot(getRootFromString(getText()));
	}

	public void setUpdateListener(IDialogFieldListener updateListener) {
		fUpdateListener= updateListener;
	}

	public Control[] doFillIntoGrid(Composite parent, int nColumns, int textWidth) {
		Control[] res= super.doFillIntoGrid(parent, nColumns);

		final Text text= getTextControl(null);
		LayoutUtil.setWidthHint(text, textWidth);
		LayoutUtil.setHorizontalGrabbing(text);

		return res;
	}

	public void setSourceChangeListener(SourceChangeListener listener) {
		fListener= listener;
	}

	/**
	 * tries to build a packagefragmentroot out of a string and sets the string into this
	 * packagefragmentroot.
	 * 
	 * @param rootString
	 */
	private IPackageFragmentRoot getRootFromString(String rootString) {
		if (rootString.length() == 0) {
			return null;
		}
		IPath path= new Path(rootString);
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		IResource res= workspaceRoot.findMember(path);
		if (res == null) {
			return null;
		}
		int resType= res.getType();
		if (resType == IResource.PROJECT || resType == IResource.FOLDER) {
			IProject proj= res.getProject();
			if (!proj.isOpen()) {
				return null;
			}				
			IJavaScriptProject jproject= JavaScriptCore.create(proj);
			IPackageFragmentRoot root= jproject.getPackageFragmentRoot(res);
			if (root.exists()) {
				return root;
			}
		}
		return null;
	}

	public void setRoot(IPackageFragmentRoot root) {
		fRoot= root;

		if (fRoot != null) {
			String str= getRootString();
			if (!getText().equals(str)) {
				setText(str);
			}
		} else {
			// dont ripple if the root is not a real root
		}

		fListener.sourceRootChanged(fRoot);
		if (fUpdateListener != null) {
			fUpdateListener.dialogFieldChanged(this);
		}
	}

	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	private String getRootString() {
		return (fRoot == null) ? "" : fRoot.getPath().makeRelative().toString(); //$NON-NLS-1$
	}


}
