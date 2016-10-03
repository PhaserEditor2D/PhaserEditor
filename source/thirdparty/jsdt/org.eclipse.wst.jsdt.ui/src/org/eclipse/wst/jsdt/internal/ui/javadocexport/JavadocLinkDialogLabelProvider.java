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
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.ISharedImages;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;


public class JavadocLinkDialogLabelProvider extends LabelProvider {

	public String getText(Object element) {
		if (element instanceof JavadocLinkRef) {
			JavadocLinkRef ref= (JavadocLinkRef) element;
			URL url= ref.getURL();
			String text= ref.getFullPath().lastSegment();
			if (url != null) {
				Object[] args= new Object[] { text, url.toExternalForm() };
				return Messages.format(JavadocExportMessages.JavadocLinkDialogLabelProvider_configuredentry, args); 
			} else {
				return Messages.format(JavadocExportMessages.JavadocLinkDialogLabelProvider_notconfiguredentry, text); 
			}
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element) {
		if (element instanceof JavadocLinkRef) {
			JavadocLinkRef ref= (JavadocLinkRef) element;
			ImageDescriptor desc;
			if (ref.isProjectRef()) {
				desc= PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
			} else {
				desc= JavaScriptUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_JAR);
			}
			if (ref.getURL() == null) {
				return JavaScriptPlugin.getImageDescriptorRegistry().get(new JavaScriptElementImageDescriptor(desc, JavaScriptElementImageDescriptor.WARNING, JavaElementImageProvider.SMALL_SIZE));
			}
			return JavaScriptPlugin.getImageDescriptorRegistry().get(desc);
		}
		return null;
	}

}
