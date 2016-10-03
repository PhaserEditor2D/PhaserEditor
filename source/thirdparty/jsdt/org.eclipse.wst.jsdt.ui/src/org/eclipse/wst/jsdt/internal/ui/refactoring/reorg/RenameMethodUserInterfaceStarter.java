/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public class RenameMethodUserInterfaceStarter extends RenameUserInterfaceStarter {
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameUserInterfaceStarter#activate(org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring, org.eclipse.swt.widgets.Shell)
	 */
	public boolean activate(Refactoring refactoring, Shell parent, int saveMode) throws CoreException {
		RenameVirtualMethodProcessor processor= (RenameVirtualMethodProcessor)refactoring.getAdapter(RenameVirtualMethodProcessor.class);
		if (processor != null) {
			RefactoringStatus status= processor.checkInitialConditions(new NullProgressMonitor());
			if (!status.hasFatalError()) {
				IFunction method= processor.getMethod();
				if (!method.equals(processor.getOriginalMethod())) {
					String message= null;
					message= Messages.format(
						RefactoringCoreMessages.MethodChecks_overrides, 
						new String[]{
							JavaElementUtil.createMethodSignature(method), 
							JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
					
					message= Messages.format(
						ReorgMessages.RenameMethodUserInterfaceStarter_message,  
						message);
					if (!MessageDialog.openQuestion(parent, 
							ReorgMessages.RenameMethodUserInterfaceStarter_name,  
							message)) {
						return false;
					}
				}
			}
		}
		return super.activate(refactoring, parent, saveMode);
	}
}
