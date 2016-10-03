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
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;


public class MonitoringNewNameQueries implements INewNameQueries {
	private INewNameQueries fDelegate;
	private ReorgExecutionLog fExecutionLog;
	public MonitoringNewNameQueries(INewNameQueries delegate, ReorgExecutionLog log) {
		fDelegate= delegate;
		fExecutionLog= log;
	}
	public INewNameQuery createNewCompilationUnitNameQuery(final IJavaScriptUnit cu, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() throws OperationCanceledException {
				String result= fDelegate.createNewCompilationUnitNameQuery(cu, initialSuggestedName).getNewName();
				String newName= JavaModelUtil.getRenamedCUName(cu, result);
				fExecutionLog.setNewName(cu, newName);
				ResourceMapping mapping= JavaElementResourceMapping.create(cu);
				if (mapping != null) {
					fExecutionLog.setNewName(mapping, newName);
				}
				return result;
			}
		};
	}
	public INewNameQuery createNewPackageFragmentRootNameQuery(final IPackageFragmentRoot root, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() throws OperationCanceledException {
				String result= fDelegate.createNewPackageFragmentRootNameQuery(root, initialSuggestedName).getNewName();
				fExecutionLog.setNewName(root, result);
				ResourceMapping mapping= JavaElementResourceMapping.create(root);
				if (mapping != null) {
					fExecutionLog.setNewName(mapping, result);
				}
				return result;
			}
		};
	}
	public INewNameQuery createNewPackageNameQuery(final IPackageFragment pack, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() throws OperationCanceledException {
				String result= fDelegate.createNewPackageNameQuery(pack, initialSuggestedName).getNewName();
				fExecutionLog.setNewName(pack, result);
				ResourceMapping mapping= JavaElementResourceMapping.create(pack);
				if (mapping != null) {
					int index= result.lastIndexOf('.');
					String newFolderName= index == -1 ? result : result.substring(index + 1);
					fExecutionLog.setNewName(mapping, newFolderName);
				}
				return result;
			}
		};
	}
	public INewNameQuery createNewResourceNameQuery(final IResource res, final String initialSuggestedName) {
		return new INewNameQuery() {
			public String getNewName() throws OperationCanceledException {
				String result= fDelegate.createNewResourceNameQuery(res, initialSuggestedName).getNewName();
				fExecutionLog.setNewName(res, result);
				return result;
			}
		};
	}
	public INewNameQuery createNullQuery() {
		return fDelegate.createNullQuery();
	}
	public INewNameQuery createStaticQuery(String newName) {
		return fDelegate.createStaticQuery(newName);
	}
}
