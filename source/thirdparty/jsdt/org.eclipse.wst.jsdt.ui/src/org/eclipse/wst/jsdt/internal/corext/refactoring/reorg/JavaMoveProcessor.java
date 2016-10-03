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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;

public final class JavaMoveProcessor extends MoveProcessor implements IScriptableRefactoring, ICommentProvider, IQualifiedNameUpdating, IReorgDestinationValidator {

	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.MoveProcessor"; //$NON-NLS-1$

	private String fComment;

	private ICreateTargetQueries fCreateTargetQueries;

	private IMovePolicy fMovePolicy;

	private IReorgQueries fReorgQueries;

	private boolean fWasCanceled;

	public JavaMoveProcessor(IMovePolicy policy) {
		fMovePolicy= policy;
	}

	public boolean canChildrenBeDestinations(IJavaScriptElement javaElement) {
		return fMovePolicy.canChildrenBeDestinations(javaElement);
	}

	public boolean canChildrenBeDestinations(IResource resource) {
		return fMovePolicy.canChildrenBeDestinations(resource);
	}

	public boolean canElementBeDestination(IJavaScriptElement javaElement) {
		return fMovePolicy.canElementBeDestination(javaElement);
	}

	public boolean canElementBeDestination(IResource resource) {
		return fMovePolicy.canElementBeDestination(resource);
	}

	public boolean canEnableComment() {
		return true;
	}

	public boolean canEnableQualifiedNameUpdating() {
		return fMovePolicy.canEnableQualifiedNameUpdating();
	}

	public boolean canUpdateQualifiedNames() {
		return fMovePolicy.canUpdateQualifiedNames();
	}

	public boolean canUpdateReferences() {
		return fMovePolicy.canUpdateReferences();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try {
			Assert.isNotNull(fReorgQueries);
			fWasCanceled= false;
			return fMovePolicy.checkFinalConditions(pm, context, fReorgQueries);
		} catch (OperationCanceledException e) {
			fWasCanceled= true;
			throw e;
		}
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();
			result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(fMovePolicy.getResources()))));
			IResource[] javaResources= ReorgUtils.getResources(fMovePolicy.getJavaElements());
			result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(javaResources))));
			return result;
		} finally {
			pm.done();
		}
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		Assert.isTrue(fMovePolicy.getJavaElementDestination() == null || fMovePolicy.getResourceDestination() == null);
		Assert.isTrue(fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null);
		try {
			final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.JavaMoveProcessor_change_name) {

				public ChangeDescriptor getDescriptor() {
					return fMovePolicy.getDescriptor();
				}

				public Change perform(IProgressMonitor pm2) throws CoreException {
					Change change= super.perform(pm2);
					Change[] changes= getChildren();
					for (int index= 0; index < changes.length; index++) {
						if (!(changes[index] instanceof TextEditBasedChange))
							return null;
					}
					return change;
				}
			};
			CreateTargetExecutionLog log= null;
			if (fCreateTargetQueries instanceof MonitoringCreateTargetQueries) {
				final MonitoringCreateTargetQueries queries= (MonitoringCreateTargetQueries) fCreateTargetQueries;
				final ICreateTargetQueries delegate= queries.getDelegate();
				if (delegate instanceof LoggedCreateTargetQueries)
					log= queries.getCreateTargetExecutionLog();
			}
			if (log != null) {
				final Object[] selected= log.getSelectedElements();
				for (int index= 0; index < selected.length; index++) {
					result.add(new LoggedCreateTargetChange(selected[index], fCreateTargetQueries));
				}
			}
			Change change= fMovePolicy.createChange(pm);
			if (change instanceof CompositeChange) {
				CompositeChange subComposite= (CompositeChange) change;
				result.merge(subComposite);
			} else {
				result.add(change);
			}
			return result;
		} finally {
			pm.done();
		}
	}

	private String[] getAffectedProjectNatures() throws CoreException {
		String[] jNatures= JavaProcessors.computeAffectedNaturs(fMovePolicy.getJavaElements());
		String[] rNatures= ResourceProcessors.computeAffectedNatures(fMovePolicy.getResources());
		Set result= new HashSet();
		result.addAll(Arrays.asList(jNatures));
		result.addAll(Arrays.asList(rNatures));
		return (String[]) result.toArray(new String[result.size()]);
	}

	public String getComment() {
		return fComment;
	}

	public Object getCommonParentForInputElements() {
		return new ParentChecker(fMovePolicy.getResources(), fMovePolicy.getJavaElements()).getCommonParent();
	}

	public ICreateTargetQuery getCreateTargetQuery() {
		return fMovePolicy.getCreateTargetQuery(fCreateTargetQueries);
	}

	protected Object getDestination() {
		IJavaScriptElement je= fMovePolicy.getJavaElementDestination();
		if (je != null)
			return je;
		return fMovePolicy.getResourceDestination();
	}

	public Object[] getElements() {
		List result= new ArrayList();
		result.addAll(Arrays.asList(fMovePolicy.getJavaElements()));
		result.addAll(Arrays.asList(fMovePolicy.getResources()));
		return result.toArray();
	}

	public String getFilePatterns() {
		return fMovePolicy.getFilePatterns();
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public IJavaScriptElement[] getJavaElements() {
		return fMovePolicy.getJavaElements();
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.MoveRefactoring_0;
	}

	public IResource[] getResources() {
		return fMovePolicy.getResources();
	}

	public boolean getUpdateQualifiedNames() {
		return fMovePolicy.getUpdateQualifiedNames();
	}

	public boolean getUpdateReferences() {
		if (!canUpdateReferences())
			return false;
		return fMovePolicy.getUpdateReferences();
	}

	public boolean hasAllInputSet() {
		return fMovePolicy.hasAllInputSet();
	}

	public boolean hasDestinationSet() {
		return fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null;
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		setReorgQueries(new NullReorgQueries());
		final RefactoringStatus status= new RefactoringStatus();
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			fMovePolicy= ReorgPolicyFactory.createMovePolicy(status, arguments);
			if (fMovePolicy != null && !status.hasFatalError()) {
				final CreateTargetExecutionLog log= ReorgPolicyFactory.loadCreateTargetExecutionLog(status, extended);
				if (log != null && !status.hasFatalError()) {
					fMovePolicy.setDestinationCheck(false);
					fCreateTargetQueries= new MonitoringCreateTargetQueries(new LoggedCreateTargetQueries(log), log);
				}
				status.merge(fMovePolicy.initialize(arguments));
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return status;
	}

	public boolean isApplicable() throws CoreException {
		return fMovePolicy.canEnable();
	}

	public boolean isTextualMove() {
		return fMovePolicy.isTextualMove();
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		return fMovePolicy.loadParticipants(status, this, getAffectedProjectNatures(), shared);
	}

	public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
		return fMovePolicy.postCreateChange(participantChanges, pm);
	}

	public void setComment(String comment) {
		fComment= comment;
	}

	public void setCreateTargetQueries(ICreateTargetQueries queries) {
		Assert.isNotNull(queries);
		fCreateTargetQueries= new MonitoringCreateTargetQueries(queries, fMovePolicy.getCreateTargetExecutionLog());
	}

	public RefactoringStatus setDestination(IJavaScriptElement destination) throws JavaScriptModelException {
		return fMovePolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaScriptModelException {
		return fMovePolicy.setDestination(destination);
	}

	public void setFilePatterns(String patterns) {
		fMovePolicy.setFilePatterns(patterns);
	}

	public void setReorgQueries(IReorgQueries queries) {
		Assert.isNotNull(queries);
		fReorgQueries= queries;
	}

	public void setUpdateQualifiedNames(boolean update) {
		fMovePolicy.setUpdateQualifiedNames(update);
	}

	public void setUpdateReferences(boolean update) {
		fMovePolicy.setUpdateReferences(update);
	}

	public boolean wasCanceled() {
		return fWasCanceled;
	}
}
