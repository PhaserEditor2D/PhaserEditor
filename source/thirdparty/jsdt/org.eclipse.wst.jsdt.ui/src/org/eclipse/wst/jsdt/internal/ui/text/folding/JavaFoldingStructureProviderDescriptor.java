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
package org.eclipse.wst.jsdt.internal.ui.text.folding;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.wst.jsdt.ui.text.folding.IJavaFoldingPreferenceBlock;
import org.eclipse.wst.jsdt.ui.text.folding.IJavaFoldingStructureProvider;

/**
 * Describes a contribution to the folding provider extension point.
 *
 * 
 */
public final class JavaFoldingStructureProviderDescriptor {

	/* extension point attribute names */

	private static final String PREFERENCES_CLASS= "preferencesClass"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	private static final String NAME= "name"; //$NON-NLS-1$
	private static final String ID= "id"; //$NON-NLS-1$

	/** The identifier of the extension. */
	private String fId;
	/** The name of the extension. */
	private String fName;
	/** The class name of the provided <code>IJavaFoldingStructureProvider</code>. */
	private String fClass;
	/**
	 * <code>true</code> if the extension specifies a custom
	 * <code>IJavaFoldingPreferenceBlock</code>.
	 */
	private boolean fHasPreferences;
	/** The configuration element of this extension. */
	private IConfigurationElement fElement;

	/**
	 * Creates a new descriptor.
	 *
	 * @param element the configuration element to read
	 */
	JavaFoldingStructureProviderDescriptor(IConfigurationElement element) {
		fElement= element;
		fId= element.getAttribute(ID);
		Assert.isLegal(fId != null);

		fName= element.getAttribute(NAME);
		if (fName == null)
			fName= fId;

		fClass= element.getAttribute(CLASS);
		Assert.isLegal(fClass != null);

		if (element.getAttribute(PREFERENCES_CLASS) == null)
			fHasPreferences= false;
		else
			fHasPreferences= true;
	}

	/**
	 * Creates a folding provider as described in the extension's xml.
	 *
	 * @return a new instance of the folding provider described by this
	 *         descriptor
	 * @throws CoreException if creation fails
	 */
	public IJavaFoldingStructureProvider createProvider() throws CoreException {
		IJavaFoldingStructureProvider prov= (IJavaFoldingStructureProvider) fElement.createExecutableExtension(CLASS);
		return prov;
	}

	/**
	 * Creates a preferences object as described in the extension's xml.
	 *
	 * @return a new instance of the reference provider described by this
	 *         descriptor
	 * @throws CoreException if creation fails
	 */
	public IJavaFoldingPreferenceBlock createPreferences() throws CoreException {
		if (fHasPreferences) {
			IJavaFoldingPreferenceBlock prefs= (IJavaFoldingPreferenceBlock) fElement.createExecutableExtension(PREFERENCES_CLASS);
			return prefs;
		} else {
			return new EmptyJavaFoldingPreferenceBlock();
		}
	}

	/**
	 * Returns the identifier of the described extension.
	 *
	 * @return Returns the id
	 */
	public String getId() {
		return fId;
	}

	/**
	 * Returns the name of the described extension.
	 *
	 * @return Returns the name
	 */
	public String getName() {
		return fName;
	}
}
