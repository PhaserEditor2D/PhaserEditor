/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.model;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.ltk.ui.refactoring.model.AbstractSynchronizationLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator;

/**
 * Java-aware synchronization label provider.
 * 
 * 
 */
public final class JavaSynchronizationLabelProvider extends AbstractSynchronizationLabelProvider implements ITreePathLabelProvider{

	/** The delegate label provider, or <code>null</code> */
	private ILabelProvider fLabelProvider= null;

	/** The model root, or <code>null</code> */
	private Object fModelRoot= null;

	/** The package image, or <code>null</code> */
	private Image fPackageImage= null;

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (fPackageImage != null && !fPackageImage.isDisposed())
			fPackageImage.dispose();
		if (fLabelProvider != null)
			fLabelProvider.dispose();
		super.dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getDelegateImage(final Object element) {
		if (element instanceof IPackageFragment) {
			final IPackageFragment fragment= (IPackageFragment) element;
			final IResource resource= fragment.getResource();
			if (resource == null || !resource.exists()) {
				if (fPackageImage == null)
					fPackageImage= JavaPluginImages.DESC_OBJS_PACKAGE.createImage();
				return fPackageImage;
			}
		}
		return super.getDelegateImage(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected ILabelProvider getDelegateLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider= new DecoratingLabelProvider(new JavaModelLabelProvider(ModelMessages.JavaModelLabelProvider_project_preferences_label, ModelMessages.JavaModelLabelProvider_refactorings_label), new ProblemsLabelDecorator(null));
		return fLabelProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	protected IDiff getDiff(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null) {
			final IDiff[] diff= JavaSynchronizationContentProvider.getDiffs(context, element);
			for (int index= 0; index < diff.length; index++) {
				if (context.getDiffTree().getResource(diff[index]).equals(resource))
					return diff[index];
			}
		}
		return super.getDiff(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getMarkerSeverity(final Object element) {
		// Decoration label provider is handling this
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object getModelRoot() {
		if (fModelRoot == null)
			fModelRoot= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
		return fModelRoot;
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean hasDecendantConflicts(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null)
			return context.getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS);
		return super.hasDecendantConflicts(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isBusy(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null)
			return context.getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_BUSY_HINT);
		return super.isBusy(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isIncludeOverlays() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreePathLabelProvider#updateLabel(org.eclipse.jface.viewers.ViewerLabel, org.eclipse.jface.viewers.TreePath)
	 */
	public void updateLabel(ViewerLabel label, TreePath elementPath) {
		Object firstSegment = elementPath.getFirstSegment();
		if (firstSegment instanceof IProject && elementPath.getSegmentCount() == 2) {
			IProject project = (IProject) firstSegment;
			Object lastSegment = elementPath.getLastSegment();
			if (lastSegment instanceof IFolder) {
				IFolder folder = (IFolder) lastSegment;
				if (!folder.getParent().equals(project)) {
					// This means that a folder that is not a direct child of the project
					// is a child in the tree. Therefore, the resource content provider
					// must be active and in compress folder mode so we will leave
					// it to the resource provider to provide the proper label.
					// We need to do this because of bug 153912
					return;
				}
			}
		}
		label.setImage(getImage(elementPath.getLastSegment()));
		label.setText(getText(elementPath.getLastSegment()));
		Font f = getFont(elementPath.getLastSegment());
		if (f != null)
			label.setFont(f);
	}
}
