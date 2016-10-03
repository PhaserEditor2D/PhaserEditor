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
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Refactoring descriptor for the use supertype refactoring.
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
public final class UseSupertypeDescriptor extends JavaScriptRefactoringDescriptor {

	/** The instanceof attribute */
	private static final String ATTRIBUTE_INSTANCEOF= "instanceof"; //$NON-NLS-1$

	/** The instanceof attribute */
	private boolean fInstanceof= false;

	/** The subtype attribute */
	private IType fSubType= null;

	/** The supertype attribute */
	private IType fSupertype= null;

	/**
	 * Creates a new refactoring descriptor.
	 */
	public UseSupertypeDescriptor() {
		super(IJavaScriptRefactorings.USE_SUPER_TYPE);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void populateArgumentMap() {
		super.populateArgumentMap();
		fArguments.put(ATTRIBUTE_INSTANCEOF, Boolean.valueOf(fInstanceof).toString());
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_INPUT, elementToHandle(getProject(), fSubType));
		fArguments.put(JavaScriptRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1, elementToHandle(getProject(), fSupertype));
	}

	/**
	 * Determines whether 'instanceof' statements are considered as candidates
	 * to replace the subtype occurrence by one of its supertypes.
	 * <p>
	 * The default is to not replace the subtype occurrence.
	 * </p>
	 * 
	 * @param replace
	 *            <code>true</code> to replace subtype occurrences in
	 *            'instanceof' statements, <code>false</code> otherwise
	 */
	public void setReplaceInstanceof(final boolean replace) {
		fInstanceof= replace;
	}

	/**
	 * Sets the subtype of the refactoring.
	 * <p>
	 * Occurrences of the subtype are replaced by the supertype set by
	 * {@link #setSupertype(IType)} where possible.
	 * </p>
	 * 
	 * @param type
	 *            the subtype to set
	 */
	public void setSubtype(final IType type) {
		Assert.isNotNull(type);
		fSubType= type;
	}

	/**
	 * Sets the supertype of the refactoring.
	 * <p>
	 * Occurrences of the subtype set by {@link #setSubtype(IType)} are replaced
	 * by the supertype where possible.
	 * </p>
	 * 
	 * @param type
	 *            the supertype to set
	 */
	public void setSupertype(final IType type) {
		Assert.isNotNull(type);
		fSupertype= type;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus validateDescriptor() {
		RefactoringStatus status= super.validateDescriptor();
		if (fSubType == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.UseSupertypeDescriptor_no_subtype));
		if (fSupertype == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.UseSupertypeDescriptor_no_supertype));
		return status;
	}
}
