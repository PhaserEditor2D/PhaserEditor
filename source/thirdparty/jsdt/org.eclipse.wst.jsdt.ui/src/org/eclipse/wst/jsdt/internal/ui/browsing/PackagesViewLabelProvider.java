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

package org.eclipse.wst.jsdt.internal.ui.browsing;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Label provider for the Packages view.
 */
class PackagesViewLabelProvider extends AppearanceAwareLabelProvider {

	static final int HIERARCHICAL_VIEW_STATE= 0;
	static final int FLAT_VIEW_STATE= 1;

	private int fViewState;

	private ImageDescriptorRegistry fRegistry;
	private TreeHierarchyLayoutProblemsDecorator fDecorator;

	PackagesViewLabelProvider(int state) {
		this(state, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaScriptElementLabels.P_COMPRESSED, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);
	}

	PackagesViewLabelProvider(int state, long textFlags, int imageFlags) {
		super(textFlags, imageFlags);

		Assert.isTrue(isValidState(state));
		fViewState= state;
		fRegistry= JavaScriptPlugin.getImageDescriptorRegistry();

		fDecorator= new TreeHierarchyLayoutProblemsDecorator(isFlatView());
		addLabelDecorator(fDecorator);
	}

	private boolean isValidState(int state) {
		return state == FLAT_VIEW_STATE || state == HIERARCHICAL_VIEW_STATE;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof LogicalPackage) {
			LogicalPackage cp= (LogicalPackage) element;
			return getLogicalPackageImage(cp);
		}
		return super.getImage(element);
	}

	/*
	 * Decoration is only concerned with error ticks
	 */
	private Image getLogicalPackageImage(LogicalPackage cp) {
		IPackageFragment[] fragments= cp.getFragments();
		for (int i= 0; i < fragments.length; i++) {
			IPackageFragment fragment= fragments[i];
			if(!isEmpty(fragment)) {
				return decorateCompoundElement(JavaPluginImages.DESC_OBJS_LOGICAL_PACKAGE, cp);
			}
		}
		return decorateCompoundElement(JavaPluginImages.DESC_OBJS_EMPTY_LOGICAL_PACKAGE, cp);
	}


	private Image decorateCompoundElement(ImageDescriptor imageDescriptor, LogicalPackage cp) {
		Image image= fRegistry.get(imageDescriptor);
		return decorateImage(image, cp);
	}

	private boolean isEmpty(IPackageFragment fragment) {
		try {
			return (fragment.getJavaScriptUnits().length == 0) && (fragment.getClassFiles().length == 0);
		} catch (JavaScriptModelException e) {
			// ignore
		}
		return false;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof IPackageFragment)
			return getText((IPackageFragment)element);
		else if (element instanceof LogicalPackage)
			return getText((LogicalPackage)element);
		else
			return super.getText(element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider#getRichTextLabel(java.lang.Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		if (element instanceof IPackageFragment || element instanceof LogicalPackage)
			return new ColoredString(getText(element));
		return super.getRichTextLabel(element);
	}
	

	private String getText(IPackageFragment fragment) {
		if (isFlatView())
			return getFlatText(fragment);
		else
			return getHierarchicalText(fragment);
	}

	private String getText(LogicalPackage logicalPackage) {
		IPackageFragment[] fragments= logicalPackage.getFragments();
		return getText(fragments[0]);
	}

	private String getFlatText(IPackageFragment fragment) {
		return super.getText(fragment);
	}

	private boolean isFlatView() {
		return fViewState==FLAT_VIEW_STATE;
	}

	private String getHierarchicalText(IPackageFragment fragment) {
		if (fragment.isDefaultPackage()) {
			return super.getText(fragment);
		}
		IResource res= fragment.getResource();
		if(res != null && !(res.getType() == IResource.FILE))
			return decorateText(res.getName(), fragment);
		else
			return decorateText(calculateName(fragment), fragment);
	}

	private String calculateName(IPackageFragment fragment) {

		String name= fragment.getElementName();
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(name.lastIndexOf(".") + 1); //$NON-NLS-1$
		return name;

	}
}
