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
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.wst.jsdt.internal.corext.util.JavaElementResourceMapping;

abstract class CompilationUnitReorgChange extends JDTChange {

	private String fCuHandle;
	private String fOldPackageHandle;
	private String fNewPackageHandle;

	private INewNameQuery fNewNameQuery;

	CompilationUnitReorgChange(IJavaScriptUnit cu, IPackageFragment dest, INewNameQuery newNameQuery) {
		fCuHandle= cu.getHandleIdentifier();
		fNewPackageHandle= dest.getHandleIdentifier();
		fNewNameQuery= newNameQuery;
		fOldPackageHandle= cu.getParent().getHandleIdentifier();
	}

	CompilationUnitReorgChange(IJavaScriptUnit cu, IPackageFragment dest) {
		this(cu, dest, null);
	}

	CompilationUnitReorgChange(String oldPackageHandle, String newPackageHandle, String cuHandle) {
		fOldPackageHandle= oldPackageHandle;
		fNewPackageHandle= newPackageHandle;
		fCuHandle= cuHandle;
	}

	public final Change perform(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask(getName(), 1);
		try {
			IJavaScriptUnit unit= getCu();
			ResourceMapping mapping= JavaElementResourceMapping.create(unit);
			Change result= doPerformReorg(new SubProgressMonitor(pm, 1));
			markAsExecuted(unit, mapping);
			return result;
		} finally {
			pm.done();
		}
	}

	abstract Change doPerformReorg(IProgressMonitor pm) throws CoreException, OperationCanceledException;

	public Object getModifiedElement() {
		return getCu();
	}

	IJavaScriptUnit getCu() {
		return (IJavaScriptUnit)JavaScriptCore.create(fCuHandle);
	}

	IPackageFragment getOldPackage() {
		return (IPackageFragment)JavaScriptCore.create(fOldPackageHandle);
	}

	IPackageFragment getDestinationPackage() {
		return (IPackageFragment)JavaScriptCore.create(fNewPackageHandle);
	}

	String getNewName() throws OperationCanceledException {
		if (fNewNameQuery == null)
			return null;
		return fNewNameQuery.getNewName();
	}

	static String getPackageName(IPackageFragment pack) {
		if (pack.isDefaultPackage())
			return RefactoringCoreMessages.MoveCompilationUnitChange_default_package; 
		else
			return pack.getElementName();
	}

	private void markAsExecuted(IJavaScriptUnit unit, ResourceMapping mapping) {
		ReorgExecutionLog log= (ReorgExecutionLog)getAdapter(ReorgExecutionLog.class);
		if (log != null) {
			log.markAsProcessed(unit);
			log.markAsProcessed(mapping);
		}
	}
}
