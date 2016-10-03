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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;

public interface IReorgPolicy extends IReferenceUpdating, IQualifiedNameUpdating, IScriptableRefactoring {

	public ChangeDescriptor getDescriptor();

	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor, CheckConditionsContext context, IReorgQueries queries) throws CoreException;
	public RefactoringStatus setDestination(IResource resource) throws JavaScriptModelException;
	public RefactoringStatus setDestination(IJavaScriptElement javaElement) throws JavaScriptModelException;
	
	public boolean canEnable() throws JavaScriptModelException;
	public boolean canChildrenBeDestinations(IResource resource);
	public boolean canChildrenBeDestinations(IJavaScriptElement javaElement);
	public boolean canElementBeDestination(IResource resource);
	public boolean canElementBeDestination(IJavaScriptElement javaElement);
	
	public IResource[] getResources();
	public IJavaScriptElement[] getJavaElements();
	
	public IResource getResourceDestination();
	public IJavaScriptElement getJavaElementDestination();
	
	public boolean hasAllInputSet();

	public boolean canUpdateReferences();
	public boolean canUpdateQualifiedNames();
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException;
	
	public String getPolicyId();

	public static interface ICopyPolicy extends IReorgPolicy{
		public Change createChange(IProgressMonitor monitor, INewNameQueries queries) throws JavaScriptModelException;
		public ReorgExecutionLog getReorgExecutionLog();
	}
	public static interface IMovePolicy extends IReorgPolicy{
		public Change createChange(IProgressMonitor monitor) throws JavaScriptModelException;
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor monitor) throws CoreException;
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries);
		public boolean isTextualMove();
		public CreateTargetExecutionLog getCreateTargetExecutionLog();
		public void setDestinationCheck(boolean check);
	}
}
