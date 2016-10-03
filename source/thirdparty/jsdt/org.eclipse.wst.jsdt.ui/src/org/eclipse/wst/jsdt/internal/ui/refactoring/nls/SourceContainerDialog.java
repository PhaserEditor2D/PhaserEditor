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

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.wst.jsdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider;

public class SourceContainerDialog extends ElementTreeSelectionDialog {

	private class PackageAndProjectSelectionValidator extends TypedElementSelectionValidator {

		public PackageAndProjectSelectionValidator() {
			super(new Class[]{IPackageFragmentRoot.class},false);
		}

		public boolean isSelectedValid(Object element) {
			try {
				if (element instanceof IJavaScriptProject) {
					IJavaScriptProject jproject= (IJavaScriptProject) element;
					IPath path= jproject.getProject().getFullPath();
					return (jproject.findPackageFragmentRoot(path) != null);
				} else
					if (element instanceof IPackageFragmentRoot) {
						return (((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE);
					}
				return true;
			} catch (JavaScriptModelException e) {
				// fall through returning false
			}
			return false;
		}
	}
	
	/**
	 * A TypedViewerFilter that accepts only PackageFragments and JavaProjects.
	 * PackageFragments are only accepted if they are of the kind K_SOURCE.
	 */
	private class JavaTypedViewerFilter extends TypedViewerFilter {

		public JavaTypedViewerFilter() {
			super(new Class[]{IPackageFragmentRoot.class, IJavaScriptProject.class});
		}

		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot fragmentRoot= (IPackageFragmentRoot)element;
				try {
					return (fragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE);
				} catch (JavaScriptModelException e) {
					return false;
				}
			}
			return super.select(viewer, parent, element);
		}
	}

	private SourceContainerDialog(Shell shell) {
		super(shell,new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT),new StandardJavaScriptElementContentProvider());
		setValidator(new PackageAndProjectSelectionValidator());
		setComparator(new JavaScriptElementComparator());
		setTitle(NewWizardMessages.NewContainerWizardPage_ChooseSourceContainerDialog_title); 
		setMessage(NewWizardMessages.NewContainerWizardPage_ChooseSourceContainerDialog_description); 
		addFilter(new JavaTypedViewerFilter());
	}

	public static IPackageFragmentRoot getSourceContainer(Shell shell, IWorkspaceRoot workspaceRoot, IJavaScriptElement initElement) {
		SourceContainerDialog dialog= new SourceContainerDialog(shell);
		dialog.setInput(JavaScriptCore.create(workspaceRoot));
		dialog.setInitialSelection(initElement);

		if (dialog.open() == Window.OK) {
			Object element= dialog.getFirstResult();
			if (element instanceof IJavaScriptProject) {
				IJavaScriptProject jproject= (IJavaScriptProject) element;
				return jproject.getPackageFragmentRoot(jproject.getProject());
			} else
				if (element instanceof IPackageFragmentRoot) {
					return (IPackageFragmentRoot) element;
				}
			return null;
		}
		return null;
	}
}
