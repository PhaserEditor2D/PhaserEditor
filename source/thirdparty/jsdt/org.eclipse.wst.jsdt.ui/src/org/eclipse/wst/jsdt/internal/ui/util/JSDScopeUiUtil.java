/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 *
 */
package org.eclipse.wst.jsdt.internal.ui.util;



import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;

import org.eclipse.wst.jsdt.core.JSDScopeUtil;

import org.eclipse.wst.jsdt.internal.ui.IJsGlobalScopeContainerInitializerExtension;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;



/**
 * @author childsb
 *
 */

/* (mostly) static methods to figure out classpath entries and container initializers *
 *
 */
public class JSDScopeUiUtil {

	private static final String CLASS="class"; //$NON-NLS-1$
	private static final String ID="id"; //$NON-NLS-1$
	
	public static IJsGlobalScopeContainerInitializerExtension findLibraryUiInitializer(IPath compUnitPath, IJavaScriptProject javaProject) {
		System.out.println("public static IJsGlobalScopeContainerInitializerExtension findLibraryInitializer("); //$NON-NLS-1$
		JsGlobalScopeContainerInitializer init =  JSDScopeUtil.findLibraryInitializer(compUnitPath,javaProject);
			return (IJsGlobalScopeContainerInitializerExtension)init;
	}

	public static IJsGlobalScopeContainerInitializerExtension getContainerUiInitializer(IPath compUnitPath) {
		try {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
		    IExtensionPoint extensionPoint =  registry.getExtensionPoint("org.eclipse.wst.jsdt.ui.JsGlobalScopeUIInitializer"); //$NON-NLS-1$
		    IConfigurationElement points[] = extensionPoint.getConfigurationElements();
		 //   int[] priorities = new int[points.length];
		   
		    
		    
		    for(int i = 0;i < points.length;i++){
		    	String id = points[i].getAttribute(ID);
		    	if(id!=null && compUnitPath.equals(new Path(id))){
		    		Object o =  points[i].createExecutableExtension(CLASS);
		    		return (IJsGlobalScopeContainerInitializerExtension)o;
		    	}
		       
		    }
		    
		}catch(Exception e) {
			JavaScriptPlugin.log( e);
		}
		return null;
		//IJsGlobalScopeContainerInitializer init = JSDScopeUtil.getContainerInitializer(compUnitPath);
		//System.out.println("public static IJsGlobalScopeContainerInitializerExtension getContainerInitializer(");
	//	return (IJsGlobalScopeContainerInitializerExtension)init;
		
	}
}
