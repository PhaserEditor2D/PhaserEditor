/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.internal.ui.navigator.ContainerFolder;
import org.eclipse.wst.jsdt.internal.ui.navigator.deferred.LoadingModelNode;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Provides the labels for the Package Explorer.
 * <p>
 * It provides labels for the packages in hierarchical layout and in all
 * other cases delegates it to its super class.
 * </p>
 * 
 */
public class PackageExplorerLabelProvider extends AppearanceAwareLabelProvider {
	
	private PackageExplorerContentProvider fContentProvider;
	private Map fWorkingSetImages;
	
	private boolean fIsFlatLayout;
	private PackageExplorerProblemsDecorator fProblemDecorator;

	public PackageExplorerLabelProvider(PackageExplorerContentProvider cp) {
		super(DEFAULT_TEXTFLAGS | JavaScriptElementLabels.P_COMPRESSED | JavaScriptElementLabels.ALL_CATEGORY, DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);
		
		fProblemDecorator= new PackageExplorerProblemsDecorator();
		addLabelDecorator(fProblemDecorator);
		Assert.isNotNull(cp);
		fContentProvider= cp;
		fWorkingSetImages= null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider#getRichTextLabel(java.lang.Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		String text= getSpecificText(element);
		if (text != null) {
			return new ColoredString(decorateText(text, element));
		}
		return super.getRichTextLabel(element);
	}
	
	private String getSpecificText(Object element) {
		if (!fIsFlatLayout && element instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) element;
			Object parent= fContentProvider.getHierarchicalPackageParent(fragment);
			if (parent instanceof IPackageFragment) {
				return getNameDelta((IPackageFragment) parent, fragment);
			} else if (parent instanceof IFolder) { // bug 152735
				return getNameDelta((IFolder) parent, fragment);
			}
		} else if (element instanceof IWorkingSet) {
			return ((IWorkingSet) element).getLabel();
		}
		return null;
	}
	
	public String getText(Object element) {
		if(element instanceof ContainerFolder) {
			return ((ContainerFolder)element).toString();
		} else if (element instanceof LoadingModelNode) {
			return ((LoadingModelNode)element).getText();
		}
		String text= getSpecificText(element);
		if (text != null) {
			return decorateText(text, element);
		}
		text = super.getText(element);
		if (!isFlatLayout() && element instanceof IJavaScriptElement) {
			switch (((IJavaScriptElement) element).getElementType()) {
				case IJavaScriptElement.TYPE :
				case IJavaScriptElement.METHOD :
				case IJavaScriptElement.FIELD :
				case IJavaScriptElement.LOCAL_VARIABLE :
				case IJavaScriptElement.INITIALIZER : {
					int groupEnd = text.lastIndexOf('.');
					if (groupEnd > 0 && groupEnd < text.length() - 1) {
						text = text.substring(groupEnd + 1);
					}
					return text;
				}
			}
		}

		if(element instanceof IClassFile) {
//			text = ((IClassFile)element).getPath().lastSegment();
		}else if (element instanceof IJavaScriptUnit) {
			text = ((IJavaScriptUnit)element).getPath().lastSegment();
		}
		
		return text;
		
	}
	
	private String getNameDelta(IPackageFragment parent, IPackageFragment fragment) {
		String prefix= parent.getElementName() + '/';
		String fullName= fragment.getElementName();
		if (fullName.startsWith(prefix)) {
			return fullName.substring(prefix.length());
		}
		return fullName;
	}
	
	public boolean isFlatLayout() {
		return fIsFlatLayout;
	}
	
	private String getNameDelta(IFolder parent, IPackageFragment fragment) {
		IPath prefix= parent.getFullPath();
		IPath fullPath= fragment.getPath();
		if (prefix.isPrefixOf(fullPath)) {
			StringBuffer buf= new StringBuffer();
			for (int i= prefix.segmentCount(); i < fullPath.segmentCount(); i++) {
				if (buf.length() > 0)
					buf.append('.');
				buf.append(fullPath.segment(i));
			}
			return buf.toString();
		}
		return fragment.getElementName();
	}
	
	public Image getImage(Object element) {
		
		if(element instanceof ContainerFolder) {
			return super.getImage(((ContainerFolder)element).getParentObject());
		} else if (element instanceof LoadingModelNode) {
			return ((LoadingModelNode)element).getImage();
		}
		
		if (element instanceof IWorkingSet) {
			ImageDescriptor image= ((IWorkingSet)element).getImageDescriptor();
			if (fWorkingSetImages == null) {
				fWorkingSetImages= new HashMap();
			}
				
			Image result= (Image) fWorkingSetImages.get(image);
			if (result == null) {
				result= image.createImage();
				fWorkingSetImages.put(image, result);
			}
			return decorateImage(result, element);
		}
		return super.getImage(element);
	}
	
	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
		fProblemDecorator.setIsFlatLayout(state);
	}
	
	public void dispose() {
		if (fWorkingSetImages != null) {
			for (Iterator iter= fWorkingSetImages.values().iterator(); iter.hasNext();) {
				((Image)iter.next()).dispose();
			}
		}
		super.dispose();
	}
}
