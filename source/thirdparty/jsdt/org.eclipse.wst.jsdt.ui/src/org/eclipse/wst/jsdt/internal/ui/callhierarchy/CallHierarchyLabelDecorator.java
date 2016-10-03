/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * LabelDecorator that decorates an method's image with recursion overlays.
 * The viewer using this decorator is responsible for updating the images on element changes.
 */
public class CallHierarchyLabelDecorator implements ILabelDecorator {

    /**
     * Creates a decorator. The decorator creates an own image registry to cache
     * images. 
     */
    public CallHierarchyLabelDecorator() {
        // Do nothing
    }   

    /* (non-Javadoc)
     * @see ILabelDecorator#decorateText(String, Object)
     */
    public String decorateText(String text, Object element) {
        return text;
    }   

    /* (non-Javadoc)
     * @see ILabelDecorator#decorateImage(Image, Object)
     */
    public Image decorateImage(Image image, Object element) {
        int adornmentFlags= computeAdornmentFlags(element);
        if (adornmentFlags != 0) {
            ImageDescriptor baseImage= new ImageImageDescriptor(image);
            Rectangle bounds= image.getBounds();
            return JavaScriptPlugin.getImageDescriptorRegistry().get(new CallHierarchyImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
        }
        return image;
    }
    
    /**
     * Note: This method is for internal use only. Clients should not call this method.
     */
    private int computeAdornmentFlags(Object element) {
        int flags= 0;
        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper= (MethodWrapper) element;
            if (methodWrapper.isRecursive()) {
                flags= CallHierarchyImageDescriptor.RECURSIVE;
            }
            if (isMaxCallDepthExceeded(methodWrapper)) {
                flags|= CallHierarchyImageDescriptor.MAX_LEVEL;
            } 
        }
        return flags;
    }

    private boolean isMaxCallDepthExceeded(MethodWrapper methodWrapper) {
        return methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth();
    }
    
    /* (non-Javadoc)
     * @see IBaseLabelProvider#addListener(ILabelProviderListener)
     */
    public void addListener(ILabelProviderListener listener) {
        // Do nothing
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#dispose()
     */
    public void dispose() {
        // Nothing to dispose
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#isLabelProperty(Object, String)
     */
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
     */
    public void removeListener(ILabelProviderListener listener) {
        // Do nothing
    }
}
