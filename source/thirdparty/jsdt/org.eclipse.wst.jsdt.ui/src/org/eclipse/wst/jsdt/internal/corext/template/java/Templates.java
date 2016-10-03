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
package org.eclipse.wst.jsdt.internal.corext.template.java;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * <code>Templates</code> gives access to the available templates.
 * 
 * @deprecated As of 3.0, replaced by {@link org.eclipse.jface.text.templates.persistence.TemplateStore}
 */
public class Templates extends org.eclipse.wst.jsdt.internal.corext.template.java.TemplateSet {

	private static final String TEMPLATE_FILE= "templates.xml"; //$NON-NLS-1$

	/** Singleton. */
	private static Templates fgTemplates;

	/**
	 * Returns an instance of templates.
	 * 
	 * @return an instance of templates
	 * @deprecated As of 3.0, replaced by
	 *             {@link org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin#getTemplateStore()}
	 */
	public static Templates getInstance() {
		if (fgTemplates == null)
			fgTemplates= new Templates();
		
		return fgTemplates;
	}
	
	public Templates() {
		super("template", JavaScriptPlugin.getDefault().getTemplateContextRegistry()); //$NON-NLS-1$
		create();
	}
	

	private void create() {

		try {
			File templateFile= getTemplateFile();
			if (templateFile.exists()) {
				addFromFile(templateFile, false);
			}

		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
			clear();
		}

	}	
	
	/**
	 * Resets the template set.
	 * 
	 * @throws CoreException in case the reset operation fails
	 */
	public void reset() throws CoreException {
	}

	/**
	 * Resets the template set with the default templates.
	 * 
	 * @throws CoreException in case the restore operation fails
	 */
	public void restoreDefaults() throws CoreException {
	}

	/**
	 * Saves the template set.
	 * 
	 * @throws CoreException in case the save operation fails
	 */
	public void save() throws CoreException {					
	}

	private static File getTemplateFile() {
		IPath path= JavaScriptPlugin.getDefault().getStateLocation();
		path= path.append(TEMPLATE_FILE);
		
		return path.toFile();
	}
}

