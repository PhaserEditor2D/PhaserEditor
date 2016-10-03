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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;

public interface INewNameQueries {
	public INewNameQuery createNewCompilationUnitNameQuery(IJavaScriptUnit cu, String initialSuggestedName) throws OperationCanceledException;
	public INewNameQuery createNewResourceNameQuery(IResource res, String initialSuggestedName) throws OperationCanceledException;
	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack, String initialSuggestedName) throws OperationCanceledException;
	public INewNameQuery createNewPackageFragmentRootNameQuery(IPackageFragmentRoot root, String initialSuggestedName) throws OperationCanceledException;
	public INewNameQuery createNullQuery();
	public INewNameQuery createStaticQuery(String newName) throws OperationCanceledException;
}
