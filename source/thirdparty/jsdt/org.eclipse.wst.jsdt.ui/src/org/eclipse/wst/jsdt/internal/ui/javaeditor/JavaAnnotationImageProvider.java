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
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * Image provider for annotations based on Java problem markers.
 *
 * 
 */
public class JavaAnnotationImageProvider implements IAnnotationImageProvider {

	private final static int NO_IMAGE= 0;
	private final static int GRAY_IMAGE= 1;
	private final static int OVERLAY_IMAGE= 2;
	private final static int QUICKFIX_IMAGE= 3;
	private final static int QUICKFIX_ERROR_IMAGE= 4;


	private static Image fgQuickFixImage;
	private static Image fgQuickFixErrorImage;
	private static ImageRegistry fgImageRegistry;

	private boolean fShowQuickFixIcon;
	private int fCachedImageType;
	private Image fCachedImage;


	public JavaAnnotationImageProvider() {
		fShowQuickFixIcon= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CORRECTION_INDICATION);
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getManagedImage(org.eclipse.jface.text.source.Annotation)
	 */
	public Image getManagedImage(Annotation annotation) {
		if (annotation instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
			int imageType= getImageType(javaAnnotation);
			return getImage(javaAnnotation, imageType, Display.getCurrent());
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getImageDescriptorId(org.eclipse.jface.text.source.Annotation)
	 */
	public String getImageDescriptorId(Annotation annotation) {
		// unmanaged images are not supported
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getImageDescriptor(java.lang.String)
	 */
	public ImageDescriptor getImageDescriptor(String symbolicName) {
		// unmanaged images are not supported
		return null;
	}


	private boolean showQuickFix(IJavaAnnotation annotation) {
		return fShowQuickFixIcon && annotation.isProblem() && JavaCorrectionProcessor.hasCorrections((Annotation) annotation);
	}

	private Image getQuickFixImage() {
		if (fgQuickFixImage == null)
			fgQuickFixImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
		return fgQuickFixImage;
	}

	private Image getQuickFixErrorImage() {
		if (fgQuickFixErrorImage == null)
			fgQuickFixErrorImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
		return fgQuickFixErrorImage;
	}

	private ImageRegistry getImageRegistry(Display display) {
		if (fgImageRegistry == null)
			fgImageRegistry= new ImageRegistry(display);
		return fgImageRegistry;
	}

	private int getImageType(IJavaAnnotation annotation) {
		int imageType= NO_IMAGE;
		if (annotation.hasOverlay())
			imageType= OVERLAY_IMAGE;
		else if (!annotation.isMarkedDeleted()) {
			if (showQuickFix(annotation))
				imageType= JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(annotation.getType()) ? QUICKFIX_ERROR_IMAGE : QUICKFIX_IMAGE;
		} else {
			imageType= GRAY_IMAGE;
		}
		return imageType;
	}

	private Image getImage(IJavaAnnotation annotation, int imageType, Display display) {
		if ((imageType == QUICKFIX_IMAGE || imageType == QUICKFIX_ERROR_IMAGE) && fCachedImageType == imageType)
			return fCachedImage;

		Image image= null;
		switch (imageType) {
			case OVERLAY_IMAGE:
				IJavaAnnotation overlay= annotation.getOverlay();
				image= getManagedImage((Annotation) overlay);
				fCachedImageType= -1;
				break;
			case QUICKFIX_IMAGE:
				image= getQuickFixImage();
				fCachedImageType= imageType;
				fCachedImage= image;
				break;
			case QUICKFIX_ERROR_IMAGE:
				image= getQuickFixErrorImage();
				fCachedImageType= imageType;
				fCachedImage= image;
				break;
			case GRAY_IMAGE: {
				ISharedImages sharedImages= PlatformUI.getWorkbench().getSharedImages();
				String annotationType= annotation.getType();
				if (JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(annotationType)) {
					image= sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
				} else if (JavaMarkerAnnotation.WARNING_ANNOTATION_TYPE.equals(annotationType)) {
					image= sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
				} else if (JavaMarkerAnnotation.INFO_ANNOTATION_TYPE.equals(annotationType)) {
					image= sharedImages.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
				}
				if (image != null) {
					ImageRegistry registry= getImageRegistry(display);
					String key= Integer.toString(image.hashCode());
					Image grayImage= registry.get(key);
					if (grayImage == null) {
						grayImage= new Image(display, image, SWT.IMAGE_GRAY);
						registry.put(key, grayImage);
					}
					image= grayImage;
				}
				fCachedImageType= -1;
				break;
			}
		}

		return image;
	}
}
