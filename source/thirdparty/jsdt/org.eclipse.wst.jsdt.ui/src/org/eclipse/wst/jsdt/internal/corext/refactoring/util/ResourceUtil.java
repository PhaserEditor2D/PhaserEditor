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
package org.eclipse.wst.jsdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IOpenable;


public class ResourceUtil {
	
	private ResourceUtil(){
	}
	
	public static IFile[] getFiles(IJavaScriptUnit[] cus) {
		List files= new ArrayList(cus.length);
		for (int i= 0; i < cus.length; i++) {
			IResource resource= cus[i].getResource();
			if (resource != null && resource.getType() == IResource.FILE)
				files.add(resource);
		}
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}

	public static IFile getFile(IJavaScriptUnit cu) {
		IResource resource= cu.getResource();
		if (resource != null && resource.getType() == IResource.FILE)
			return (IFile)resource;
		else
			return null;
	}

	//----- other ------------------------------
			
	public static IResource getResource(Object o){
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IJavaScriptElement)
			return getResource((IJavaScriptElement)o);
		return null;	
	}

	private static IResource getResource(IJavaScriptElement element){
		if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT)
			return ((IJavaScriptUnit) element).getResource();
		else if (element instanceof IOpenable) 
			return element.getResource();
		else	
			return null;	
	}
}
