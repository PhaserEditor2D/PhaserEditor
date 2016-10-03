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

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Refactoring descriptor for the rename local variable refactoring.
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
public final class RenameLocalVariableDescriptor extends JavaScriptRefactoringDescriptor {

	/** The name attribute */
	private String fName= null;

	/** The references attribute */
	private boolean fReferences= false;

	/** The selection attribute */
	private ISourceRange fSelection= null;

	/** The compilation unit attribute */
	private IJavaScriptUnit fUnit= null;

	/**
	 * Creates a new refactoring descriptor.
	 */
	public RenameLocalVariableDescriptor() {
		super(IJavaScriptRefactorings.RENAME_LOCAL_VARIABLE);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void populateArgumentMap() {
		super.populateArgumentMap();
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_NAME, fName);
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_INPUT, elementToHandle(getProject(), fUnit));
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelection.getOffset()).toString() + " " + Integer.valueOf(fSelection.getLength()).toString()); //$NON-NLS-1$
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_REFERENCES, Boolean.toString(fReferences));
	}

	/**
	 * Sets the compilation unit which contains the local variable.
	 * 
	 * @param unit
	 *            the compilation unit to set
	 */
	public void setCompilationUnit(final IJavaScriptUnit unit) {
		Assert.isNotNull(unit);
		fUnit= unit;
	}

	/**
	 * Sets the new name to rename the local variable to.
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
	 * Sets the selection within the compilation unit which references the local
	 * variable to rename.
	 * 
	 * @param selection
	 *            the selection to set
	 */
	public void setSelection(final ISourceRange selection) {
		Assert.isNotNull(selection);
		fSelection= selection;
	}

	/**
	 * Determines whether references to the local variable should be renamed.
	 * <p>
	 * The default is to not update references.
	 * </p>
	 * 
	 * @param update
	 *            <code>true</code> to update references, <code>false</code>
	 *            otherwise
	 */
	public void setUpdateReferences(final boolean update) {
		fReferences= update;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus validateDescriptor() {
		RefactoringStatus status= super.validateDescriptor();
		if (fName == null || "".equals(fName)) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_new_name));
		if (fUnit == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameLocalVariableDescriptor_no_compilation_unit));
		if (fSelection == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameLocalVariableDescriptor_no_selection));
		return status;
	}
}
