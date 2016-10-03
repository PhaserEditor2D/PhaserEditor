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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class BuildpathDelta {
	
	private final String fOperationDescription;
	private CPListElement[] fNewEntries;
	private final List fCreatedResources;
	private final List fDeletedResources;
	private final List fAddedEntries;
	private final ArrayList fRemovedEntries;

	public BuildpathDelta(String operationDescription) {
		fOperationDescription= operationDescription;
		
		fCreatedResources= new ArrayList();
		fDeletedResources= new ArrayList();
		fAddedEntries= new ArrayList();
		fRemovedEntries= new ArrayList();
    }

	public String getOperationDescription() {
		return fOperationDescription;
	}
	
	public CPListElement[] getNewEntries() {
		return fNewEntries;
	}
	
	public IResource[] getCreatedResources() {
		return (IResource[])fCreatedResources.toArray(new IResource[fCreatedResources.size()]);
	}
	
	public IResource[] getDeletedResources() {
		return (IResource[])fDeletedResources.toArray(new IResource[fDeletedResources.size()]);
	}

	public void setNewEntries(CPListElement[] newEntries) {
		fNewEntries= newEntries;
    }

	public void addCreatedResource(IResource resource) {
		fCreatedResources.add(resource);
    }

	public void addDeletedResource(IResource resource) {
		fDeletedResources.add(resource);
    }

    public List getAddedEntries() {
	    return fAddedEntries;
    }

    public void addEntry(CPListElement entry) {
    	fAddedEntries.add(entry);
    }
    
    public List getRemovedEntries() {
    	return fRemovedEntries;
    }

    public void removeEntry(CPListElement entry) {
    	fRemovedEntries.add(entry);
    }
}
