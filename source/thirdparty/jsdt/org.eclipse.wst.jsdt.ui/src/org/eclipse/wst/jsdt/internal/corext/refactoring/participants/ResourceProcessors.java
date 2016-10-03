/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.participants;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ResourceProcessors {
	
	public static String[] computeAffectedNatures(IResource resource) throws CoreException {
		IProject project= resource.getProject();
		Set result= new HashSet();
		Set visitedProjects= new HashSet();
		computeNatures(result, visitedProjects, project);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public static String[] computeAffectedNatures(IResource[] resources) throws CoreException {
		Set result= new HashSet();
		Set visitedProjects= new HashSet();
		for (int i= 0; i < resources.length; i++) {
			computeNatures(result, visitedProjects, resources[i].getProject());
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	private static void computeNatures(Set result, Set visitedProjects, IProject focus) throws CoreException {
		if (visitedProjects.contains(focus))
			return;
		String[] pns= focus.getDescription().getNatureIds();
		for (int p = 0; p < pns.length; p++) {
			result.add(pns[p]);
		}
		visitedProjects.add(focus);
		IProject[] referencing= focus.getReferencingProjects();
		for (int i= 0; i < referencing.length; i++) {
			computeNatures(result, visitedProjects, referencing[i]);
		}
	}
}
