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
package org.eclipse.wst.jsdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;

public abstract class AbstractCleanUp implements ICleanUp {
	
	private Map fOptions;
	private final boolean fCanReinitialize;
	
	public AbstractCleanUp() {
		this(null);
	}
	
	public AbstractCleanUp(Map options) {
		fOptions= options;
		fCanReinitialize= options == null;
	}
	
	protected int getNumberOfProblems(IProblem[] problems, int problemId) {
		int result= 0;
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].getID() == problemId)
				result++;
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkPreConditions(IJavaScriptProject project, IJavaScriptUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		if (monitor != null)
			monitor.done();
		return new RefactoringStatus();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		if (monitor != null)
			monitor.done();
		//Default do nothing
		return new RefactoringStatus();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initialize(Map settings) throws CoreException {
		if (fCanReinitialize)
			fOptions= settings;
	}
	
	protected boolean isEnabled(String key) {
		Assert.isNotNull(key);
		
		Object value= fOptions.get(key);
		return CleanUpConstants.TRUE == value || CleanUpConstants.TRUE.equals(value);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean needsFreshAST(JavaScriptUnit compilationUnit) {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(IJavaScriptUnit unit) throws CoreException {
		return null;
	}
}
