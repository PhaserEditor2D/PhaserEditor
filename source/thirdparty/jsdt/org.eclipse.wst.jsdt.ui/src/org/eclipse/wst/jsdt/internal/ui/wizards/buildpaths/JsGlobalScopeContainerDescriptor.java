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

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.CoreUtility;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.wizards.IJsGlobalScopeContainerPage;

/**
  */
public class JsGlobalScopeContainerDescriptor {

	private IConfigurationElement fConfigElement;
	private IJsGlobalScopeContainerPage fPage;

	private static final String ATT_EXTENSION = "JsGlobalScopeContainerPage"; //$NON-NLS-1$

	private static final String ATT_ID = "id"; //$NON-NLS-1$
	private static final String ATT_NAME = "name"; //$NON-NLS-1$
	private static final String ATT_PAGE_CLASS = "class"; //$NON-NLS-1$	
	private static final String ATT_ALLOW_MULTI = "allowMulti"; //$NON-NLS-1$

	public JsGlobalScopeContainerDescriptor(IConfigurationElement configElement) throws CoreException {
		super();
		fConfigElement = configElement;
		fPage= null;

		String id = fConfigElement.getAttribute(ATT_ID);
		String name = configElement.getAttribute(ATT_NAME);
		String pageClassName = configElement.getAttribute(ATT_PAGE_CLASS);

		if (name == null) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, 0, "Invalid extension (missing name): " + id, null)); //$NON-NLS-1$
		}
		if (pageClassName == null) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, 0, "Invalid extension (missing page class name): " + id, null)); //$NON-NLS-1$
		}
	}

	public IJsGlobalScopeContainerPage createPage() throws CoreException  {
		if (fPage == null) {
			Object elem= CoreUtility.createExtension(fConfigElement, ATT_PAGE_CLASS);
			if (elem instanceof IJsGlobalScopeContainerPage) {
				fPage= (IJsGlobalScopeContainerPage) elem;
			} else {
				String id= fConfigElement.getAttribute(ATT_ID);
				throw new CoreException(new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, 0, "Invalid extension (page not of type IJsGlobalScopeContainerPage): " + id, null)); //$NON-NLS-1$
			}
		}
		return fPage;
	}
	
	public IJsGlobalScopeContainerPage getPage() {
		return fPage;
	}
	
	public void setPage(IJsGlobalScopeContainerPage page) {
		fPage= page;
	}
	
	public void dispose() {
		if (fPage != null) {
			fPage.dispose();
			fPage= null;
		}
	}

	public String getName() {
		return fConfigElement.getAttribute(ATT_NAME);
	}
	
	public String getPageClass() {
		return fConfigElement.getAttribute(ATT_PAGE_CLASS);
	}	

	public boolean canEdit(IIncludePathEntry entry) {
		String id = fConfigElement.getAttribute(ATT_ID);
		if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER && !entry.getPath().isEmpty()) {
			String type = entry.getPath().segment(0);
			String multi = fConfigElement.getAttribute(ATT_ALLOW_MULTI);
			return type.equals(id) && (multi == null || !Boolean.valueOf(multi).booleanValue());
		}
		return false;
	}

	public static JsGlobalScopeContainerDescriptor[] getDescriptors() {
		ArrayList containers= new ArrayList();
		
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(JavaScriptUI.ID_PLUGIN, ATT_EXTENSION);
		if (extensionPoint != null) {
			JsGlobalScopeContainerDescriptor defaultPage= null;
			String defaultPageName= JsGlobalScopeContainerDefaultPage.class.getName();
			
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
			for (int i = 0; i < elements.length; i++) {
				try {
					JsGlobalScopeContainerDescriptor curr= new JsGlobalScopeContainerDescriptor(elements[i]);					
					if (defaultPageName.equals(curr.getPageClass())) {
						defaultPage= curr;
					} else {
						containers.add(curr);
					}
				} catch (CoreException e) {
					JavaScriptPlugin.log(e);
				}
			}
			if (defaultPageName != null && containers.isEmpty()) {
				// default page only added of no other extensions found
				containers.add(defaultPage);
			}
		}
		return (JsGlobalScopeContainerDescriptor[]) containers.toArray(new JsGlobalScopeContainerDescriptor[containers.size()]);
	}

}
