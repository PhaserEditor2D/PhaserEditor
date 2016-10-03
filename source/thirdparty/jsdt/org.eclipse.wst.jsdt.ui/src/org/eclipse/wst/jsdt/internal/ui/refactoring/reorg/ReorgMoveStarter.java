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
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.JavaMoveRefactoring;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;


public class ReorgMoveStarter {
	private final JavaMoveProcessor fMoveProcessor;

	private ReorgMoveStarter(JavaMoveProcessor moveProcessor) {
		Assert.isNotNull(moveProcessor);
		fMoveProcessor= moveProcessor;
	}
	
	public static ReorgMoveStarter create(IJavaScriptElement[] javaElements, IResource[] resources, IJavaScriptElement destination) throws JavaScriptModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
		if (!policy.canEnable())
			return null;
		JavaMoveProcessor processor= new JavaMoveProcessor(policy);
		if (! processor.setDestination(destination).isOK())
			return null;
		return new ReorgMoveStarter(processor);
	}

	public static ReorgMoveStarter create(IJavaScriptElement[] javaElements, IResource[] resources, IResource destination) throws JavaScriptModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
		if (!policy.canEnable())
			return null;
		JavaMoveProcessor processor= new JavaMoveProcessor(policy);
		if (! processor.setDestination(destination).isOK())
			return null;
		return new ReorgMoveStarter(processor);
	}
	
	public void run(Shell parent) throws InterruptedException, InvocationTargetException {
		try {
			JavaMoveRefactoring ref= new JavaMoveRefactoring(fMoveProcessor);
			if (fMoveProcessor.hasAllInputSet()) {
				IRunnableContext context= new ProgressMonitorDialog(parent);
				fMoveProcessor.setCreateTargetQueries(new CreateTargetQueries(parent));
				fMoveProcessor.setReorgQueries(new ReorgQueries(parent));
				new RefactoringExecutionHelper(ref, RefactoringCore.getConditionCheckingFailedSeverity(), RefactoringSaveHelper.SAVE_ALL, parent, context).perform(false, false);
			} else  {
				RefactoringWizard wizard= new ReorgMoveWizard(ref);
				/*
				 * We want to get the shell from the refactoring dialog but it's not known at this point, 
				 * so we pass the wizard and then, once the dialog is open, we will have access to its shell.
				 */
				fMoveProcessor.setCreateTargetQueries(new CreateTargetQueries(wizard));
				fMoveProcessor.setReorgQueries(new ReorgQueries(wizard));
				new RefactoringStarter().activate(ref, wizard, parent, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_ALL); 
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}
}
