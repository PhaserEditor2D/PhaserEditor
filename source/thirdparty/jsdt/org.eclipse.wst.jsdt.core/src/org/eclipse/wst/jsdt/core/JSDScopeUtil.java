/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.internal.core.ClassFile;

/**
 *  (mostly) static methods to figure out includepath entries and container initializers *
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */

public class JSDScopeUtil {





	public static JsGlobalScopeContainerInitializer getContainerInitializer(IPath classPathEntry) {
		if(classPathEntry==null ) return null;
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(classPathEntry.segment(0));
		return initializer ;
	}

	public IIncludePathEntry[] getIncludepathEntries(IJsGlobalScopeContainer container) {


		if(container!=null) return	container.getIncludepathEntries();

		return new IIncludePathEntry[0];
	}

	public  IJsGlobalScopeContainer getLibraryContainer(IPath cpEntry, IJavaScriptProject javaProject) {
		IJsGlobalScopeContainer container=null;
		try {
			container = JavaScriptCore.getJsGlobalScopeContainer(cpEntry, javaProject);
		} catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		return	container;
	}

	public static JsGlobalScopeContainerInitializer findLibraryInitializer(IPath compUnitPath, IJavaScriptProject javaProject) {
		IPackageFragmentRoot[] roots = new IPackageFragmentRoot[0];
		try {
			roots = javaProject.getAllPackageFragmentRoots();
		} catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		for (int i = 0;i<roots.length;i++) {
			IPackageFragment frag = roots[i].getPackageFragment(""); //$NON-NLS-1$

			try {
				IClassFile classfile = frag.getClassFile(compUnitPath.toString());
				if(classfile.exists()) {
					return ((ClassFile)classfile).getContainerInitializer();
				}
			} catch (Exception ex) {
				// Do nothing since CU may be invalid and thats what w're tryingto figure out.
				// TODO Auto-generated catch block
			//	ex.printStackTrace();
			}


		}

		return null;
	}
}
