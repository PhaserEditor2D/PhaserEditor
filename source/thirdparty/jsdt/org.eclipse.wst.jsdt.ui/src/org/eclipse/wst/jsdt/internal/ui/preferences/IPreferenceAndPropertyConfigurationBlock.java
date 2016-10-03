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
package org.eclipse.wst.jsdt.internal.ui.preferences;

/**
 * Interface for preference and property configuration blocks which can either be
 * wrapped by a {@link org.eclipse.wst.jsdt.internal.ui.preferences.AbstractConfigurationBlockPreferenceAndPropertyPage}
 * or be included some preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * 
 */
public interface IPreferenceAndPropertyConfigurationBlock extends IPreferenceConfigurationBlock {

	/**
	 * Disable project specific settings for the settings configured by this block.
	 */
	public abstract void disableProjectSettings();

	/**
	 * Enabled project specific settings for the settings configured by this block.
	 */
	public abstract void enableProjectSettings();

}
