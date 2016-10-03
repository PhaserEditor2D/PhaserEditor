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

import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;

public interface IParameterListChangeListener {

	/**
	 * Gets fired when the given parameter has changed
	 * @param parameter the parameter that has changed.
	 */
	public void parameterChanged(ParameterInfo parameter);

	/**
	 * Gets fired when the given parameter has been added
	 * @param parameter the parameter that has been added.
	 */
	public void parameterAdded(ParameterInfo parameter);
	
	
	/**
	 * Gets fired if the parameter list got modified by reordering or removing 
	 * parameters (note that adding is handled by <code>parameterAdded</code>))
	 */
	public void parameterListChanged();
}
