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

package org.eclipse.wst.jsdt.internal.corext.refactoring;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;

/**
 * 
 */
public abstract class RefactoringElementFilter {

	/**
	 * @param cu the IJavaScriptUnit under test
	 * @return <code>true</code> iff the given IJavaScriptUnit should be
	 *         filtered (i.e., refactorings should not touch the compilation
	 *         unit)
	 */
	public abstract boolean filter(IJavaScriptUnit cu);
}
