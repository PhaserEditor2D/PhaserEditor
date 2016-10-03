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
package org.eclipse.wst.jsdt.internal.corext.refactoring.participants;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;

/**
 * Utility class to deal with Java element processors.
 */
public class JavaProcessors {

	public static String[] computeAffectedNatures(IJavaScriptElement element) throws CoreException {
		if (element instanceof IMember) {
			IMember member= (IMember)element;
			if (JdtFlags.isPrivate(member)) {
				return element.getJavaScriptProject().getProject().getDescription().getNatureIds();
			}
		}
		IJavaScriptProject project= element.getJavaScriptProject();
		return ResourceProcessors.computeAffectedNatures(project.getProject());
	}
	
	public static String[] computeAffectedNaturs(IJavaScriptElement[] elements) throws CoreException {
		Set result= new HashSet();
		for (int i= 0; i < elements.length; i++) {
			String[] natures= computeAffectedNatures(elements[i]);
			for (int j= 0; j < natures.length; j++) {
				result.add(natures[j]);
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
}
