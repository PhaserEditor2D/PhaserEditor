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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public class MoveResourceChange extends ResourceReorgChange {
	
	public MoveResourceChange(IResource res, IContainer dest){
		super(res, dest, null);
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// We already present a dialog to the user if he
		// moves read-only resources. Since moving a resource
		// doesn't do a validate edit (it actually doesn't
		// change the content we can't check for READ only
		// here.
		return super.isValid(pm, DIRTY);
	}
	
	/* non java-doc
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected Change doPerformReorg(IPath path, IProgressMonitor pm) throws CoreException{
		getResource().move(path, getReorgFlags(), pm);
		return null;
	}
	public String getName() {
		return Messages.format(RefactoringCoreMessages.MoveResourceChange_move, 
			new String[]{getResource().getFullPath().toString(), getDestination().getName()});
	}
}

