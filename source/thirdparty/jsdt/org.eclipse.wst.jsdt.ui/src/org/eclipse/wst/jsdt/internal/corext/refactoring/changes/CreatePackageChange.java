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

package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;

public class CreatePackageChange extends JDTChange {
	
	private IPackageFragment fPackageFragment;
	
	public CreatePackageChange(IPackageFragment pack) {
		fPackageFragment= pack;
	}

	public RefactoringStatus isValid(IProgressMonitor pm) {
		// Don't do any checking. Peform handles the case
		// that the package already exists. Furthermore
		// create package change isn't used as a undo
		// redo change right now
		return new RefactoringStatus();
	}
	
	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.CreatePackageChange_Creating_package, 1); 

			if (fPackageFragment.exists()) {
				return new NullChange();	
			} else {
				IPackageFragmentRoot root= (IPackageFragmentRoot) fPackageFragment.getParent();
				root.createPackageFragment(fPackageFragment.getElementName(), false, pm);
				
				return new DeleteFolderChange((IFolder) fPackageFragment.getResource(), true); 
			}		
		} finally {
			pm.done();
		}
	}

	public String getName() {
		return RefactoringCoreMessages.CreatePackageChange_Create_package; 
	}

	public Object getModifiedElement() {
		return fPackageFragment;
	}
}
