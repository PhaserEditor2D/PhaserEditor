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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RenameSourceFolderChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;

public final class RenameSourceFolderProcessor extends JavaRenameProcessor {
	
	private static final String ATTRIBUTE_PATH= "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$
	
	private IPackageFragmentRoot fSourceFolder;

	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.renameSourceFolderProcessor"; //$NON-NLS-1$

	/**
	 * Creates a new rename source folder processor.
	 * @param root the package fragment root, or <code>null</code> if invoked by scripting
	 */
	public RenameSourceFolderProcessor(IPackageFragmentRoot root) {
		fSourceFolder= root;
		if (root != null)
			setNewElementName(root.getElementName());
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fSourceFolder);
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.RenameSourceFolderRefactoring_rename;
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fSourceFolder);
	}
	
	public Object[] getElements() {
		return new Object[] {fSourceFolder};
	}

	public Object getNewElement() throws CoreException {
		IPackageFragmentRoot[] roots= fSourceFolder.getJavaScriptProject().getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].getElementName().equals(getNewElementName()))
				return roots[i];	
		}
		return null;
	}
	
	public int getSaveMode() {
		return RefactoringSaveHelper.SAVE_ALL;
	}
	
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fSourceFolder, new RenameArguments(getNewElementName(), getUpdateReferences()));
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		return new IFile[0];
	}
	
	//---- IRenameProcessor ----------------------------------------------
	
	public String getCurrentElementName() {
		return fSourceFolder.getElementName();
	}
			
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}

	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		if (! newName.trim().equals(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_blank); 
		
		IContainer c= 	fSourceFolder.getResource().getParent();
		if (! c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_invalid_name); 
		
		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, IResource.FOLDER));
		if (result.hasFatalError())
			return result;		
				
		result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), IResource.FOLDER)));		
		if (result.hasFatalError())
			return result;
			
		IJavaScriptProject project= fSourceFolder.getJavaScriptProject();
		IPath p= project.getProject().getFullPath().append(newName);
		if (project.findPackageFragmentRoot(p) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_already_exists); 
		
		if (project.getProject().findMember(new Path(newName)) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_alread_exists); 
		return result;		
	}
	
	private String createNewPath(String newName) {
		return fSourceFolder.getPath().removeLastSegments(1).append(newName).toString();
	}

	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}		
	}
	
	public boolean getUpdateReferences() {
		return true;
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(RefactoringCoreMessages.RenameTypeRefactoring_creating_change, 1);
		try {
			final IResource resource= fSourceFolder.getResource();
			final String project= resource.getProject().getName();
			final String newName= getNewElementName();
			final String description= Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_descriptor_description_short, fSourceFolder.getElementName());
			final String header= Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_descriptor_description, new String[] { resource.getFullPath().toString(), newName});
			final String comment= new JDTRefactoringDescriptorComment(project, this, header).asString();
			final RenameJavaScriptElementDescriptor descriptor= new RenameJavaScriptElementDescriptor(IJavaScriptRefactorings.RENAME_SOURCE_FOLDER);
			descriptor.setProject(project);
			descriptor.setDescription(description);
			descriptor.setComment(comment);
			descriptor.setFlags(RefactoringDescriptor.NONE);
			descriptor.setJavaElement(fSourceFolder);
			descriptor.setNewName(newName);
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.RenameSourceFolderRefactoring_rename, new Change[] { new RenameSourceFolderChange(fSourceFolder, newName)});
		} finally {
			monitor.done();
		}
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments generic= (JavaRefactoringArguments) arguments;
			final String path= generic.getAttribute(ATTRIBUTE_PATH);
			if (path != null) {
				final IResource resource= ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(path));
				if (resource == null || !resource.exists())
					return ScriptableRefactoring.createInputFatalStatus(resource, getRefactoring().getName(), IJavaScriptRefactorings.RENAME_SOURCE_FOLDER);
				else
					fSourceFolder= (IPackageFragmentRoot) JavaScriptCore.create(resource);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PATH));
			final String name= generic.getAttribute(ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_NAME));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
