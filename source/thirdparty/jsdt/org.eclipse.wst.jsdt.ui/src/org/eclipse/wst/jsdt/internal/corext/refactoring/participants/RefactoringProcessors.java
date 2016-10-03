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
import org.eclipse.core.runtime.CoreException;

public class RefactoringProcessors {
	
	public static String[] getNatures(IProject[] projects) throws CoreException {
		Set result= new HashSet();
		for (int i= 0; i < projects.length; i++) {
			String[] pns= projects[i].getDescription().getNatureIds();
			for (int p = 0; p < pns.length; p++) {
				result.add(pns[p]);
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
}
