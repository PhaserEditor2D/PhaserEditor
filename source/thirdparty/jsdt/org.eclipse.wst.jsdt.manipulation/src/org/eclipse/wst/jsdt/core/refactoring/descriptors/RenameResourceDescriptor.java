/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.refactoring.descriptors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Refactoring descriptor for the rename resource refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link org.eclipse.ltk.core.refactoring.RefactoringContribution#createDescriptor()} on a refactoring
 * contribution requested by invoking
 * {@link org.eclipse.ltk.core.refactoring.RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class RenameResourceDescriptor extends JavaScriptRefactoringDescriptor {

	/** The name attribute */
	private String fName= null;

	/** The resource attribute */
	private IResource fResource= null;

	/**
	 * Creates a new refactoring descriptor.
	 */
	public RenameResourceDescriptor() {
		super(IJavaScriptRefactorings.RENAME_RESOURCE);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void populateArgumentMap() {
		super.populateArgumentMap();
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_INPUT, resourceToHandle(getProject(), fResource));
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_NAME, fName);
	}

	/**
	 * Sets the new name to rename the resource to.
	 * 
	 * @param name
	 *            the non-empty new name to set
	 */
	public void setNewName(final String name) {
		Assert.isNotNull(name);
		Assert.isLegal(!"".equals(name), "Name must not be empty"); //$NON-NLS-1$//$NON-NLS-2$
		fName= name;
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * Note: If the resource to be renamed is of type {@link IResource#PROJECT},
	 * clients are required to to set the project name to <code>null</code>.
	 * </p>
	 * <p>
	 * The default is to associate the refactoring with the workspace.
	 * </p>
	 * 
	 * @param project
	 *            the non-empty project name to set, or <code>null</code> for
	 *            the workspace
	 * 
	 * @see #getProject()
	 */
	public void setProject(final String project) {
		super.setProject(project);
	}

	/**
	 * Sets the resource to be renamed.
	 * <p>
	 * Note: If the resource to be renamed is of type {@link IResource#PROJECT},
	 * clients are required to to set the project name to <code>null</code>.
	 * </p>
	 * 
	 * @param resource
	 *            the resource to be renamed
	 */
	public void setResource(final IResource resource) {
		Assert.isNotNull(resource);
		fResource= resource;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus validateDescriptor() {
		RefactoringStatus status= super.validateDescriptor();
		if (fResource == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_resource));
		if (fName == null || "".equals(fName)) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_new_name));
		if (fResource instanceof IProject && getProject() != null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_project_constraint));
		return status;
	}
}
