/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.buildpath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class CPJavaProject {

	public static CPJavaProject createFromExisting(IJavaScriptProject javaProject) throws CoreException {
		List classpathEntries= ClasspathModifier.getExistingEntries(javaProject);
		return new CPJavaProject(classpathEntries);
    }

    private final List fCPListElements;

	public CPJavaProject(List cpListElements) {
		fCPListElements= cpListElements;
    }

    public CPJavaProject createWorkingCopy() {
    	List newList= new ArrayList(fCPListElements.size());
    	for (Iterator iterator= fCPListElements.iterator(); iterator.hasNext();) {
	        CPListElement element= (CPListElement)iterator.next();
	        newList.add(element.copy());
        }
	    return new CPJavaProject(newList);
    }

    public CPListElement get(int index) {
    	return (CPListElement)fCPListElements.get(index);
    }

    public IIncludePathEntry[] getIncludePathEntries() {
    	IIncludePathEntry[] result= new IIncludePathEntry[fCPListElements.size()];
    	int i= 0;
    	for (Iterator iterator= fCPListElements.iterator(); iterator.hasNext();) {
	        CPListElement element= (CPListElement)iterator.next();
	        result[i]= element.getClasspathEntry();
	        i++;
        }
	    return result;
    }

    public CPListElement getCPElement(CPListElement element) {
		return ClasspathModifier.getClasspathEntry(fCPListElements, element);
    }

    public List getCPListElements() {
	    return fCPListElements;
    }
    
    public IJavaScriptProject getJavaProject() {
	    return ((CPListElement)fCPListElements.get(0)).getJavaProject();
    }

    public int indexOf(CPListElement element) {
		return fCPListElements.indexOf(element);
    }
}
