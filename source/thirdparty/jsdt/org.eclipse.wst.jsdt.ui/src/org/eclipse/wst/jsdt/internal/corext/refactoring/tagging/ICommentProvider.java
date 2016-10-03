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
package org.eclipse.wst.jsdt.internal.corext.refactoring.tagging;

/**
 * Interface for refactorings which provide a comment for the history.
 * 
 * 
 */
public interface ICommentProvider {

	/**
	 * Performs a dynamic check whether this refactoring object is capable of
	 * accepting user comments to be stored in the refactoring history. The
	 * return value of this method may change according to the state of the
	 * refactoring.
	 */
	public boolean canEnableComment();

	/**
	 * If <code>canEnableComment</code> returns <code>true</code>, then
	 * this method is used to ask the refactoring object for the comment
	 * associated with the refactoring. This call can be ignored if
	 * <code>canEnableComment</code> returns <code>false</code>.
	 * 
	 * @return the comment, or <code>null</code>
	 */
	public String getComment();

	/**
	 * If <code>canEnableComment</code> returns <code>true</code>, then
	 * this method may be called to set the comment associated with the
	 * refactoring. This call can be ignored if <code>canEnableComment</code>
	 * returns <code>false</code>.
	 * 
	 * @param comment
	 *            the comment to set, or <code>null</code>
	 */
	public void setComment(String comment);
}
