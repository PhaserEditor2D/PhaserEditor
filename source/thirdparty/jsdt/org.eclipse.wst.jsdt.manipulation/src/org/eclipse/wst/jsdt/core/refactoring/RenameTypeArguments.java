/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.refactoring;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
 * Rename type arguments describe the data that a rename type processor
 * provides to its rename type participants.
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class RenameTypeArguments extends RenameArguments {

	private boolean updateSimilarDeclarations;
	private IJavaScriptElement[] similarDeclarations;
	
	/**
	 * Creates new rename type arguments.
	 * 
	 * @param newName the new name of the element to be renamed
	 * @param updateReferences <code>true</code> if reference
	 *  updating is requested; <code>false</code> otherwise
	 * @param updateSimilarDeclarations <code>true</code> if similar
	 *  declaration updating is requested; <code>false</code> otherwise
	 * @param similarDeclarations the similar declarations that will be 
	 *  updated or <code>null</code> if similar declaration updating is
	 *  not requested
	 */
	public RenameTypeArguments(String newName, boolean updateReferences, boolean updateSimilarDeclarations,
			IJavaScriptElement[] similarDeclarations) {
		super(newName, updateReferences);
		if (updateSimilarDeclarations) {
			Assert.isNotNull(similarDeclarations);
		}
		this.updateSimilarDeclarations= updateSimilarDeclarations;
		this.similarDeclarations= similarDeclarations;
	}
	
	/**
	 * Returns whether similar declaration updating is requested or not.
	 * 
	 * @return returns <code>true</code> if similar declaration
	 *  updating is requested; <code>false</code> otherwise
	 */
	public boolean getUpdateSimilarDeclarations() {
		return updateSimilarDeclarations;
	}
	
	/**
	 * Returns the similar declarations that get updated. Returns
	 * <code>null</code> if similar declaration updating is not
	 * requested.
	 * 
	 * @return the similar elements that get updated
	 */
	public IJavaScriptElement[] getSimilarDeclarations() {
		return similarDeclarations;
	}
	
	/* (non-Javadoc)
	 * @see RefactoringArguments#toString()
	 */
	public String toString() {
		return super.toString()
				+ (updateSimilarDeclarations ? " (update derived elements)" : " (don't update derived elements)"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
