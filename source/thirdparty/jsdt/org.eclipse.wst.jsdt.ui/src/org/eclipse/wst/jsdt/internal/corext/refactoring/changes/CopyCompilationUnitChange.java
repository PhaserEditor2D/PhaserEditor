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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public class CopyCompilationUnitChange extends CompilationUnitReorgChange {
	
	public CopyCompilationUnitChange(IJavaScriptUnit cu, IPackageFragment dest, INewNameQuery newNameQuery){
		super(cu, dest, newNameQuery);
	}
		
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// Copy compilation unit change isn't undoable and isn't used
		// as a redo/undo change right now. Furthermore the current 
		// implementation allows copying dirty files. In this case only 
		// the content on disk is copied.
		return super.isValid(pm, NONE);
	}
	
	Change doPerformReorg(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		getCu().copy(getDestinationPackage(), null, getNewName(), true, pm);
		return null;
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.CopyCompilationUnitChange_copy, 
			new String[]{getCu().getElementName(), getPackageName(getDestinationPackage())});
	}
}
