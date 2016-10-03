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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaCopyRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;

public class ReorgCopyStarter {

	public static ReorgCopyStarter create(IJavaScriptElement[] javaElements, IResource[] resources, IJavaScriptElement destination) throws JavaScriptModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements);
		if (!copyPolicy.canEnable())
			return null;
		JavaCopyProcessor copyProcessor= new JavaCopyProcessor(copyPolicy);
		if (!copyProcessor.setDestination(destination).isOK())
			return null;
		return new ReorgCopyStarter(copyProcessor);
	}

	public static ReorgCopyStarter create(IJavaScriptElement[] javaElements, IResource[] resources, IResource destination) throws JavaScriptModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements);
		if (!copyPolicy.canEnable())
			return null;
		JavaCopyProcessor copyProcessor= new JavaCopyProcessor(copyPolicy);
		if (!copyProcessor.setDestination(destination).isOK())
			return null;
		return new ReorgCopyStarter(copyProcessor);
	}

	private final JavaCopyProcessor fCopyProcessor;

	private ReorgCopyStarter(JavaCopyProcessor copyProcessor) {
		Assert.isNotNull(copyProcessor);
		fCopyProcessor= copyProcessor;
	}

	public void run(Shell parent) throws InterruptedException, InvocationTargetException {
		IRunnableContext context= new ProgressMonitorDialog(parent);
		fCopyProcessor.setNewNameQueries(new NewNameQueries(parent));
		fCopyProcessor.setReorgQueries(new ReorgQueries(parent));
		new RefactoringExecutionHelper(new JavaCopyRefactoring(fCopyProcessor), RefactoringCore.getConditionCheckingFailedSeverity(), RefactoringSaveHelper.SAVE_NOTHING, parent, context).perform(false, false);
	}
}
