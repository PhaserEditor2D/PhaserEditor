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
package org.eclipse.wst.jsdt.ui.text.folding;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 * Contributors to the <code>org.eclipse.wst.jsdt.ui.foldingStructureProvider</code> extension point
 * can specify an implementation of this interface to be displayed on the JavaScript &gt; Editor &gt; Folding
 * preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IJavaFoldingPreferenceBlock {

	/**
	 * Creates the control that will be displayed on the JavaScript &gt; Editor &gt; Folding
	 * preference page.
	 *
	 * @param parent the parent composite to which to add the preferences control
	 * @return the control that was added to <code>parent</code>
	 */
	Control createControl(Composite parent);

	/**
	 * Called after creating the control. Implementations should load the
	 * preferences values and update the controls accordingly.
	 */
	void initialize();

	/**
	 * Called when the <code>OK</code> button is pressed on the preference
	 * page. Implementations should commit the configured preference settings
	 * into their form of preference storage.
	 */
	void performOk();

	/**
	 * Called when the <code>Defaults</code> button is pressed on the
	 * preference page. Implementation should reset any preference settings to
	 * their default values and adjust the controls accordingly.
	 */
	void performDefaults();

	/**
	 * Called when the preference page is being disposed. Implementations should
	 * free any resources they are holding on to.
	 */
	void dispose();

}
