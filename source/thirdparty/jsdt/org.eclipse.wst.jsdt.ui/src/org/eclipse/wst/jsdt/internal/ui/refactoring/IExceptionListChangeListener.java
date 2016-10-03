/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring;


public interface IExceptionListChangeListener {

	/**
	* Gets fired if the exception list got modified by adding or removing exceptions
	*/
	public void exceptionListChanged();
	
	
//	/**
//	 * Gets fired when the given parameter has changed
//	 * @param parameter the parameter that has changed.
//	 */
//	public void exceptionChanged(ExceptionInfo exception);
//
//	/**
//	 * Gets fired when the given exception has been added
//	 * @param exception the exception that has been added.
//	 */
//	public void exceptionAdded(ExceptionInfo exception);
//	
//	
//	/**
//	 * Gets fired if the exception list got modified by reordering or removing 
//	 * exceptions (note that adding is handled by <code>exceptionAdded</code>))
//	 */
//	public void exceptionListChanged();
}
