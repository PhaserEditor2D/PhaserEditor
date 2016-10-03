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
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Image provider for {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.OverrideIndicatorManager.OverrideIndicator} annotations.
 *
 * 
 */
public class OverrideIndicatorImageProvider implements IAnnotationImageProvider {

	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationImageProvider#getManagedImage(org.eclipse.jface.text.source.Annotation)
	 */
	private static final String OVERRIDE_IMG_DESC_ID= "JavaPluginImages.DESC_OBJ_OVERRIDES"; //$NON-NLS-1$
	private static final String OVERWRITE_IMG_DESC_ID= "JavaPluginImages.DESC_OBJ_IMPLEMENTS"; //$NON-NLS-1$
	public Image getManagedImage(Annotation annotation) {
		return null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationImageProvider#getImageDescriptorId(org.eclipse.jface.text.source.Annotation)
	 */
	public String getImageDescriptorId(Annotation annotation) {
		if (!isImageProviderFor(annotation))
			return null;

		if (isOverwriting(annotation))
			return OVERWRITE_IMG_DESC_ID;
		else
			return OVERRIDE_IMG_DESC_ID;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationImageProvider#getImageDescriptor(java.lang.String)
	 */
	public ImageDescriptor getImageDescriptor(String imageDescritporId) {
		if (OVERWRITE_IMG_DESC_ID.equals(imageDescritporId))
			return JavaPluginImages.DESC_OBJ_IMPLEMENTS;
		else if (OVERRIDE_IMG_DESC_ID.equals(imageDescritporId))
			return JavaPluginImages.DESC_OBJ_OVERRIDES;

		return null;
	}

	private boolean isImageProviderFor(Annotation annotation) {
		return annotation != null && OverrideIndicatorManager.ANNOTATION_TYPE.equals(annotation.getType());
	}

	private boolean isOverwriting(Annotation annotation) {
		return ((OverrideIndicatorManager.OverrideIndicator)annotation).isOverwriteIndicator();
	}
}
