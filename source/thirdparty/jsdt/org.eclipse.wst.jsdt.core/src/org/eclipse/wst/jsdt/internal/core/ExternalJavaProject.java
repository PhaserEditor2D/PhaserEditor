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
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

public class ExternalJavaProject extends JavaProject {

	/*
	 * Note this name can be surfaced in the UI (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=128258)
	 */
	public static final String EXTERNAL_PROJECT_NAME = " "; //$NON-NLS-1$

	public ExternalJavaProject(IIncludePathEntry[] rawClasspath) {
		super(ResourcesPlugin.getWorkspace().getRoot().getProject(EXTERNAL_PROJECT_NAME), JavaModelManager.getJavaModelManager().getJavaModel());
		try {
			getPerProjectInfo().setClasspath(rawClasspath, defaultOutputLocation(), JavaModelStatus.VERIFIED_OK/*no .classpath format problem*/, null/*no resolved claspath*/, null/*no reverse map*/, null/*no resolve entry map*/, null/*no resolved status*/);
		} catch (JavaScriptModelException e) {
			// getPerProjectInfo() never throws JavaScriptModelException for an ExternalJavaProject
		}
	}

	public boolean equals(Object o) {
		return this == o;
	}

	public boolean exists() {
		// external project never exists
		return false;
	}

	public String getOption(String optionName, boolean inheritJavaCoreOptions) {
		if (JavaScriptCore.COMPILER_PB_FORBIDDEN_REFERENCE.equals(optionName)
				|| JavaScriptCore.COMPILER_PB_DISCOURAGED_REFERENCE.equals(optionName))
			return JavaScriptCore.IGNORE;
		return super.getOption(optionName, inheritJavaCoreOptions);
	}

	public boolean isOnIncludepath(IJavaScriptElement element) {
		// since project is external, no element is on classpath (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=61013#c16)
		return false;
	}

	public boolean isOnIncludepath(IResource resource) {
		// since project is external, no resource is on classpath (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=61013#c16)
		return false;
	}

}
