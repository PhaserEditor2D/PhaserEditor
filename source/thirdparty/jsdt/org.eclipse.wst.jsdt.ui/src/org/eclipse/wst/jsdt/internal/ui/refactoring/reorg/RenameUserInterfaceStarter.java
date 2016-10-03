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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.wst.jsdt.internal.ui.refactoring.UserInterfaceStarter;

public class RenameUserInterfaceStarter extends UserInterfaceStarter {
	
	public boolean activate(Refactoring refactoring, Shell parent, int saveMode) throws CoreException {
		RenameProcessor processor= (RenameProcessor)refactoring.getAdapter(RenameProcessor.class);
		Object[] elements= processor.getElements();
		RenameSelectionState state= elements.length == 1 ? new RenameSelectionState(elements[0]) : null;
		boolean executed= super.activate(refactoring, parent, saveMode);
		INameUpdating nameUpdating= (INameUpdating)refactoring.getAdapter(INameUpdating.class);
		if (executed && nameUpdating != null && state != null) {
			Object newElement= nameUpdating.getNewElement();
			if (newElement != null) {
				state.restore(newElement);
			}			
		}
		return executed;
	}
}
