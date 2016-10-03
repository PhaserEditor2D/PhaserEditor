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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.CopyProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;

public final class JavaCopyProcessor extends CopyProcessor implements IReorgDestinationValidator, IScriptableRefactoring, ICommentProvider {

	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.CopyProcessor"; //$NON-NLS-1$

	private String fComment;

	private ICopyPolicy fCopyPolicy;

	private ReorgExecutionLog fExecutionLog;

	private INewNameQueries fNewNameQueries;

	private IReorgQueries fReorgQueries;

	public JavaCopyProcessor(ICopyPolicy copyPolicy) {
		fCopyPolicy= copyPolicy;
	}

	public boolean canChildrenBeDestinations(IJavaScriptElement javaElement) {
		return fCopyPolicy.canChildrenBeDestinations(javaElement);
	}

	public boolean canChildrenBeDestinations(IResource resource) {
		return fCopyPolicy.canChildrenBeDestinations(resource);
	}

	public boolean canElementBeDestination(IJavaScriptElement javaElement) {
		return fCopyPolicy.canElementBeDestination(javaElement);
	}

	public boolean canElementBeDestination(IResource resource) {
		return fCopyPolicy.canElementBeDestination(resource);
	}

	public boolean canEnableComment() {
		return true;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		Assert.isNotNull(fNewNameQueries, "Missing new name queries"); //$NON-NLS-1$
		Assert.isNotNull(fReorgQueries, "Missing reorg queries"); //$NON-NLS-1$
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= fCopyPolicy.checkFinalConditions(new SubProgressMonitor(pm, 1), context, fReorgQueries);
		result.merge(context.check(new SubProgressMonitor(pm, 1)));
		return result;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(fCopyPolicy.getResources()))));
		IResource[] javaResources= ReorgUtils.getResources(fCopyPolicy.getJavaElements());
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(javaResources))));
		return result;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		Assert.isNotNull(fNewNameQueries);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() == null || fCopyPolicy.getResourceDestination() == null);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() != null || fCopyPolicy.getResourceDestination() != null);
		try {
			final DynamicValidationStateChange result= new DynamicValidationStateChange(getChangeName()) {

				public Object getAdapter(Class adapter) {
					if (ReorgExecutionLog.class.equals(adapter))
						return fExecutionLog;
					return super.getAdapter(adapter);
				}

				public ChangeDescriptor getDescriptor() {
					return fCopyPolicy.getDescriptor();
				}

				public Change perform(IProgressMonitor pm2) throws CoreException {
					try {
						super.perform(pm2);
					} catch (OperationCanceledException e) {
						fExecutionLog.markAsCanceled();
						throw e;
					}
					return null;
				}
			};
			Change change= fCopyPolicy.createChange(pm, new MonitoringNewNameQueries(fNewNameQueries, fExecutionLog));
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
		String[] jNatures= JavaProcessors.computeAffectedNaturs(fCopyPolicy.getJavaElements());
		String[] rNatures= ResourceProcessors.computeAffectedNatures(fCopyPolicy.getResources());
		Set result= new HashSet();
		result.addAll(Arrays.asList(jNatures));
		result.addAll(Arrays.asList(rNatures));
		return (String[]) result.toArray(new String[result.size()]);
	}

	private String getChangeName() {
		return RefactoringCoreMessages.JavaCopyProcessor_changeName;
	}

	public String getComment() {
		return fComment;
	}

	public Object getCommonParentForInputElements() {
		return new ParentChecker(fCopyPolicy.getResources(), fCopyPolicy.getJavaElements()).getCommonParent();
	}

	public Object[] getElements() {
		IJavaScriptElement[] jElements= fCopyPolicy.getJavaElements();
		IResource[] resources= fCopyPolicy.getResources();
		List result= new ArrayList(jElements.length + resources.length);
		result.addAll(Arrays.asList(jElements));
		result.addAll(Arrays.asList(resources));
		return result.toArray();
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public IJavaScriptElement[] getJavaElements() {
		return fCopyPolicy.getJavaElements();
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.JavaCopyProcessor_processorName;
	}

	public IResource[] getResources() {
		return fCopyPolicy.getResources();
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		setReorgQueries(new NullReorgQueries());
		final RefactoringStatus status= new RefactoringStatus();
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			fCopyPolicy= ReorgPolicyFactory.createCopyPolicy(status, extended);
			if (fCopyPolicy != null && !status.hasFatalError()) {
				status.merge(fCopyPolicy.initialize(arguments));
				if (!status.hasFatalError()) {
					final ReorgExecutionLog log= ReorgPolicyFactory.loadReorgExecutionLog(status, extended);
					if (log != null && !status.hasFatalError())
						setNewNameQueries(new LoggedNewNameQueries(log));
				}
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return status;
	}

	public boolean isApplicable() throws CoreException {
		return fCopyPolicy.canEnable();
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		RefactoringParticipant[] result= fCopyPolicy.loadParticipants(status, this, getAffectedProjectNatures(), sharedParticipants);
		fExecutionLog= fCopyPolicy.getReorgExecutionLog();
		return result;
	}

	public void setComment(String comment) {
		fComment= comment;
	}

	public RefactoringStatus setDestination(IJavaScriptElement destination) throws JavaScriptModelException {
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaScriptModelException {
		return fCopyPolicy.setDestination(destination);
	}

	public void setNewNameQueries(INewNameQueries newNameQueries) {
		Assert.isNotNull(newNameQueries);
		fNewNameQueries= newNameQueries;
	}

	public void setReorgQueries(IReorgQueries queries) {
		Assert.isNotNull(queries);
		fReorgQueries= queries;
	}
}
