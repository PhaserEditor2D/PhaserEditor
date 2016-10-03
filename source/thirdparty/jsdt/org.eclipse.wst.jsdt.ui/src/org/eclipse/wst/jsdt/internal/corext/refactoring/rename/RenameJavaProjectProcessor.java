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
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RenameJavaProjectChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;

public final class RenameJavaProjectProcessor extends JavaRenameProcessor implements IReferenceUpdating {

	private IJavaScriptProject fProject;
	private boolean fUpdateReferences;

	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.renameJavaProjectProcessor"; //$NON-NLS-1$

	/**
	 * Creates a new rename java project processor.
	 * @param project the java project, or <code>null</code> if invoked by scripting
	 */
	public RenameJavaProjectProcessor(IJavaScriptProject project) {
		fProject= project;
		if (fProject != null)
			setNewElementName(fProject.getElementName());
		fUpdateReferences= true;
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fProject);
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.RenameJavaProjectRefactoring_rename;
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fProject);
	}
	
	public Object[] getElements() {
		return new Object[] {fProject};
	}

	public Object getNewElement() throws CoreException {
		IPath newPath= fProject.getPath().removeLastSegments(1).append(getNewElementName());
		return JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot().findMember(newPath));
	}
	
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fProject, new RenameArguments(getNewElementName(), getUpdateReferences()));
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		IFile projectFile= fProject.getProject().getFile(".project"); //$NON-NLS-1$
		if (projectFile != null && projectFile.exists()) {
			return new IFile[] {projectFile};
		}
		return new IFile[0];
	}
	
	public int getSaveMode() {
		return RefactoringSaveHelper.SAVE_ALL;
	}
	
	//---- IReferenceUpdating --------------------------------------
		
	public boolean canEnableUpdateReferences() {
		return true;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}

	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}
		
	//---- IRenameProcessor ----------------------------------------------
	
	public String getCurrentElementName() {
		return fProject.getElementName();
	}
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= RefactoringStatus.create(ResourcesPlugin.getWorkspace().validateName(newName, IResource.PROJECT));
		if (result.hasFatalError())
			return result;
		
		if (projectNameAlreadyExists(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameJavaProjectRefactoring_already_exists); 
		if (projectFolderAlreadyExists(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameJavaProjectProcessor_folder_already_exists); 
		
		return new RefactoringStatus();
	}
	
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			if (isReadOnly()){
				String message= Messages.format(RefactoringCoreMessages.RenameJavaProjectRefactoring_read_only, 
									fProject.getElementName());
				return RefactoringStatus.createErrorStatus(message);
			}
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	private boolean isReadOnly() {
		return Resources.isReadOnly(fProject.getResource());
	}
	
	private boolean projectNameAlreadyExists(String newName){
		return ResourcesPlugin.getWorkspace().getRoot().getProject(newName).exists();
	}

	private boolean projectFolderAlreadyExists(String newName) throws CoreException {
		boolean isNotInWorkpace= fProject.getProject().getDescription().getLocationURI() != null;
		if (isNotInWorkpace)
			return false; // projects outside of the workspace are not renamed
		URI locationURI= fProject.getProject().getLocationURI();
		IFileStore projectStore= EFS.getStore(locationURI);
		IFileStore newProjectStore= projectStore.getParent().getChild(newName);
		return newProjectStore.fetchInfo().exists();
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			final String description= Messages.format(RefactoringCoreMessages.RenameJavaProjectProcessor_descriptor_description_short, fProject.getElementName());
			final String header= Messages.format(RefactoringCoreMessages.RenameJavaProjectChange_descriptor_description, new String[] { fProject.getElementName(), getNewElementName()});
			final String comment= new JDTRefactoringDescriptorComment(null, this, header).asString();
			final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE;
			final RenameJavaScriptElementDescriptor descriptor= new RenameJavaScriptElementDescriptor(IJavaScriptRefactorings.RENAME_JAVA_PROJECT);
			descriptor.setProject(null);
			descriptor.setDescription(description);
			descriptor.setComment(comment);
			descriptor.setFlags(flags);
			descriptor.setJavaElement(fProject);
			descriptor.setNewName(getNewElementName());
			descriptor.setUpdateReferences(fUpdateReferences);
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.RenameJavaProjectRefactoring_rename, new Change[] { new RenameJavaProjectChange(fProject, getNewElementName(), fUpdateReferences)});
		} finally {
			monitor.done();
		}
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.JAVASCRIPT_PROJECT)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.RENAME_JAVA_PROJECT);
				else
					fProject= (IJavaScriptProject) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String references= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES);
			if (references != null) {
				fUpdateReferences= Boolean.valueOf(references).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
