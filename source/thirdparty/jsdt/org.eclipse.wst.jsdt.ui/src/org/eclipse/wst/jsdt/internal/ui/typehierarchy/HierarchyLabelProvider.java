/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Label provider for the hierarchy viewers. Types in the hierarchy that are not belonging to the
 * input scope are rendered differently.
  */
public class HierarchyLabelProvider extends AppearanceAwareLabelProvider {

	private static class FocusDescriptor extends CompositeImageDescriptor {
		private ImageDescriptor fBase;
		public FocusDescriptor(ImageDescriptor base) {
			fBase= base;
		}
		protected void drawCompositeImage(int width, int height) {
			drawImage(getImageData(fBase), 0, 0);
			drawImage(getImageData(JavaPluginImages.DESC_OVR_FOCUS), 0, 0);
		}
		
		private ImageData getImageData(ImageDescriptor descriptor) {
			ImageData data= descriptor.getImageData(); // see bug 51965: getImageData can return null
			if (data == null) {
				data= DEFAULT_IMAGE_DATA;
				JavaScriptPlugin.logErrorMessage("Image data not available: " + descriptor.toString()); //$NON-NLS-1$
			}
			return data;
		}
		
		protected Point getSize() {
			return JavaElementImageProvider.BIG_SIZE;
		}
		public int hashCode() {
			return fBase.hashCode();
		}
		public boolean equals(Object object) {
			return object != null && FocusDescriptor.class.equals(object.getClass()) && ((FocusDescriptor)object).fBase.equals(fBase);
		}		
	}

	private Color fSpecialColor;

	private ViewerFilter fFilter;
	
	private TypeHierarchyLifeCycle fHierarchy;
	
	public HierarchyLabelProvider(TypeHierarchyLifeCycle lifeCycle) {
		super(DEFAULT_TEXTFLAGS | JavaScriptElementLabels.USE_RESOLVED, DEFAULT_IMAGEFLAGS);
		
		fHierarchy= lifeCycle;
		fFilter= null;
	}

	/**
	 * @return Returns the filter.
	 */
	public ViewerFilter getFilter() {
		return fFilter;
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(ViewerFilter filter) {
		fFilter= filter;
	}

	protected boolean isDifferentScope(IType type) {
		if (fFilter != null && !fFilter.select(null, null, type)) {
			return true;
		}
		
		IJavaScriptElement input= fHierarchy.getInputElement();
		if (input == null || input.getElementType() == IJavaScriptElement.TYPE) {
			return false;
		}
			
		IJavaScriptElement parent= type.getAncestor(input.getElementType());
		if (input.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
			if (parent == null || parent.getElementName().equals(input.getElementName())) {
				return false;
			}
		} else if (input.equals(parent)) {
			return false;
		}
		return true;
	}
		
	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */ 
	public Image getImage(Object element) {
		Image result= null;
		if (element instanceof IType) {
			ImageDescriptor desc= getTypeImageDescriptor((IType) element);
			if (desc != null) {
				if (element.equals(fHierarchy.getInputElement())) {
					desc= new FocusDescriptor(desc);
				}
				result= JavaScriptPlugin.getImageDescriptorRegistry().get(desc);
			}
		} else {
			result= fImageLabelProvider.getImageLabel(element, evaluateImageFlags(element));
		}
		return decorateImage(result, element);
	}

	private ImageDescriptor getTypeImageDescriptor(IType type) {
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return new JavaScriptElementImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.BIG_SIZE);
		}
		
		int flags= hierarchy.getCachedFlags(type);
		if (flags == -1) {
			return new JavaScriptElementImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.BIG_SIZE);
		}
		
		boolean isInner= (type.getDeclaringType() != null);
		
		ImageDescriptor desc= JavaElementImageProvider.getTypeImageDescriptor(isInner, false, flags, isDifferentScope(type));

		int adornmentFlags= 0;
		if (Flags.isAbstract(flags)) {
			adornmentFlags |= JavaScriptElementImageDescriptor.ABSTRACT;
		}
		if (Flags.isStatic(flags)) {
			adornmentFlags |= JavaScriptElementImageDescriptor.STATIC;
		}
		
		return new JavaScriptElementImageDescriptor(desc, adornmentFlags, JavaElementImageProvider.BIG_SIZE);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (element instanceof IFunction) {
			if (fSpecialColor == null) {
				fSpecialColor= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
			}
			return fSpecialColor;
		} else if (element instanceof IType && isDifferentScope((IType) element)) {
			return JFaceResources.getColorRegistry().get(ColoredViewersManager.QUALIFIER_COLOR_NAME);
		}
		return null;
	}	
	
	

}
