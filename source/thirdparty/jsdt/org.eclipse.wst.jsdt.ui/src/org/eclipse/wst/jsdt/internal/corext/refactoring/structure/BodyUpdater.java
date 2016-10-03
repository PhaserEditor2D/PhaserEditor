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

package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;


public abstract class BodyUpdater {

	/**
	 * Updates the body of a method declaration. This method is called by the
	 * {@link ChangeSignatureRefactoring} and allows implementors to refactor the body
	 * of the given method declaration.
	 * 
	 * @param methodDeclaration
	 * @param cuRewrite
	 * @param result
	 * @throws CoreException 
	 */
	public abstract void updateBody(FunctionDeclaration methodDeclaration, CompilationUnitRewrite cuRewrite, RefactoringStatus result) throws CoreException;

	/**
	 * Returns whether {@link ChangeSignatureRefactoring} should check if
	 * deleted parameters are currently used in the method body.
	 * 
	 * @return <code>true</code> by default, subclasses can override
	 */
	public boolean needsParameterUsedCheck() {
		return true;
	}

}
