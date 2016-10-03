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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;


public class CallHierarchyImageDescriptor extends CompositeImageDescriptor {
    
    /** Flag to render the recursive adornment */
    public final static int RECURSIVE=       0x001;
    
    /** Flag to render the callee adornment */
    public final static int MAX_LEVEL=       0x002;

    private ImageDescriptor fBaseImage;
    private int fFlags;
    private Point fSize;

    /**
     * Creates a new CallHierarchyImageDescriptor.
     * 
     * @param baseImage an image descriptor used as the base image
     * @param flags flags indicating which adornments are to be rendered. See <code>setAdornments</code>
     *  for valid values.
     * @param size the size of the resulting image
     * @see #setAdornments(int)
     */
    public CallHierarchyImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
        fBaseImage= baseImage;
        Assert.isNotNull(fBaseImage);
        fFlags= flags;
        Assert.isTrue(fFlags >= 0);
        fSize= size;
        Assert.isNotNull(fSize);
    }
    
    /**
     * Sets the descriptors adornments. Valid values are: <code>RECURSIVE</code>, <code>CALLER</code>,
     * <code>CALLEE</code>, <code>MAX_LEVEL</code>, or any combination of those.
     * 
     * @param adornments the image descritpors adornments
     */
    public void setAdornments(int adornments) {
        Assert.isTrue(adornments >= 0);
        fFlags= adornments;
    }

    /**
     * Returns the current adornments.
     * 
     * @return the current adornments
     */
    public int getAdronments() {
        return fFlags;
    }

    /**
     * Sets the size of the image created by calling <code>createImage()</code>.
     * 
     * @param size the size of the image returned from calling <code>createImage()</code>
     * @see ImageDescriptor#createImage()
     */
    public void setImageSize(Point size) {
        Assert.isNotNull(size);
        Assert.isTrue(size.x >= 0 && size.y >= 0);
        fSize= size;
    }
    
    /**
     * Returns the size of the image created by calling <code>createImage()</code>.
     * 
     * @return the size of the image created by calling <code>createImage()</code>
     * @see ImageDescriptor#createImage()
     */
    public Point getImageSize() {
        return new Point(fSize.x, fSize.y);
    }
    
    /* (non-Javadoc)
     * Method declared in CompositeImageDescriptor
     */
    protected Point getSize() {
        return fSize;
    }
    
    /* (non-Javadoc)
     * Method declared on Object.
     */
    public boolean equals(Object object) {
        if (object == null || !CallHierarchyImageDescriptor.class.equals(object.getClass()))
            return false;
            
        CallHierarchyImageDescriptor other= (CallHierarchyImageDescriptor)object;
        return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
    }
    
    /* (non-Javadoc)
     * Method declared on Object.
     */
    public int hashCode() {
        return fBaseImage.hashCode() | fFlags | fSize.hashCode();
    }
    
    /* (non-Javadoc)
     * Method declared in CompositeImageDescriptor
     */
    protected void drawCompositeImage(int width, int height) {
        ImageData bg= getImageData(fBaseImage);
            
        drawImage(bg, 0, 0);
        drawBottomLeft();
    }  
    
	private ImageData getImageData(ImageDescriptor descriptor) {
		ImageData data= descriptor.getImageData(); // see bug 51965: getImageData can return null
		if (data == null) {
			data= DEFAULT_IMAGE_DATA;
			JavaScriptPlugin.logErrorMessage("Image data not available: " + descriptor.toString()); //$NON-NLS-1$
		}
		return data;
	}
    
    private void drawBottomLeft() {
        Point size= getSize();
        int x= 0;
        ImageData data= null;
        if ((fFlags & RECURSIVE) != 0) {
            data= getImageData(JavaPluginImages.DESC_OVR_RECURSIVE);
            drawImage(data, x, size.y - data.height);
            x+= data.width;
        }
        if ((fFlags & MAX_LEVEL) != 0) {
            data= getImageData(JavaPluginImages.DESC_OVR_MAX_LEVEL);
            drawImage(data, x, size.y - data.height);
            x+= data.width;
        }
    }       
}
