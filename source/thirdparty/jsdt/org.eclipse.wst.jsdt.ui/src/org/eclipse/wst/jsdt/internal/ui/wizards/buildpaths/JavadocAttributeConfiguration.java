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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration;

public class JavadocAttributeConfiguration extends ClasspathAttributeConfiguration {

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#getImageDescriptor(org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public ImageDescriptor getImageDescriptor(ClasspathAttributeAccess attribute) {
		return JavaPluginImages.DESC_OBJS_JAVADOC_LOCATION_ATTRIB;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#getNameLabel(org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public String getNameLabel(ClasspathAttributeAccess attribute) {
		return NewWizardMessages.CPListLabelProvider_javadoc_location_label;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#getValueLabel(org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public String getValueLabel(ClasspathAttributeAccess access) {
		String arg= null;
		String str= access.getClasspathAttribute().getValue();
		if (str != null) {
			String prefix= JavaDocLocations.ARCHIVE_PREFIX;
			if (str.startsWith(prefix)) {
				int sepIndex= str.lastIndexOf("!/"); //$NON-NLS-1$
				if (sepIndex == -1) {
					arg= str.substring(prefix.length());
				} else {
					String archive= str.substring(prefix.length(), sepIndex);
					String root= str.substring(sepIndex + 2);
					if (root.length() > 0) {
						arg= Messages.format(NewWizardMessages.CPListLabelProvider_twopart, new String[] { archive, root }); 
					} else {
						arg= archive;
					}
				}
			} else {
				arg= str;
			}
		} else {
			arg= NewWizardMessages.CPListLabelProvider_none;
		}
		return arg;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#performEdit(org.eclipse.swt.widgets.Shell, org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public IIncludePathAttribute performEdit(Shell shell, ClasspathAttributeAccess attribute) {
		String initialLocation= attribute.getClasspathAttribute().getValue();
		String elementName= attribute.getParentClasspassEntry().getPath().lastSegment();
		try {
			URL locationURL= initialLocation != null ? new URL(initialLocation) : null;
			URL[] result= BuildPathDialogAccess.configureJavadocLocation(shell, elementName, locationURL);
			if (result != null) {
				URL newURL= result[0];
				String string= newURL != null ? newURL.toExternalForm() : null;
				return JavaScriptCore.newIncludepathAttribute(IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME, string);
			}
		} catch (MalformedURLException e) {
			// todo
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#performRemove(org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public IIncludePathAttribute performRemove(ClasspathAttributeAccess attribute) {
		return JavaScriptCore.newIncludepathAttribute(IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#canEdit(org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public boolean canEdit(ClasspathAttributeAccess attribute) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration#canRemove(org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess)
	 */
	public boolean canRemove(ClasspathAttributeAccess attribute) {
		return attribute.getClasspathAttribute().getValue() != null;
	}



}
