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

import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameLocalVariableProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameResourceProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameSourceFolderProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.UserInterfaceManager;

public class RenameUserInterfaceManager extends UserInterfaceManager {
	private static final UserInterfaceManager fgInstance= new RenameUserInterfaceManager();
	
	public static UserInterfaceManager getDefault() {
		return fgInstance;
	}
	
	private RenameUserInterfaceManager() {
		put(RenameResourceProcessor.class, RenameUserInterfaceStarter.class, RenameResourceWizard.class);
		
		put(RenameJavaProjectProcessor.class, RenameUserInterfaceStarter.class, RenameJavaProjectWizard.class);
		put(RenameSourceFolderProcessor.class, RenameUserInterfaceStarter.class, RenameSourceFolderWizard.class);
		put(RenamePackageProcessor.class, RenameUserInterfaceStarter.class, RenamePackageWizard.class);
		put(RenameCompilationUnitProcessor.class, RenameUserInterfaceStarter.class, RenameCuWizard.class);
		put(RenameTypeProcessor.class, RenameUserInterfaceStarter.class, RenameTypeWizard.class);
		put(RenameFieldProcessor.class, RenameUserInterfaceStarter.class, RenameFieldWizard.class);
		put(RenameNonVirtualMethodProcessor.class, RenameMethodUserInterfaceStarter.class, RenameMethodWizard.class);
		put(RenameVirtualMethodProcessor.class, RenameMethodUserInterfaceStarter.class, RenameMethodWizard.class);
		put(RenameLocalVariableProcessor.class, RenameUserInterfaceStarter.class, RenameLocalVariableWizard.class);
	}
}
