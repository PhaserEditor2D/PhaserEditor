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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls.search;

import java.util.Set;

import org.eclipse.core.runtime.Assert;



class Properties extends java.util.Properties {
	
	private static final long serialVersionUID= 1L;
	
	private Set fDuplicateKeys;

	public Properties() {
	}
	
	public Properties(Set duplicateKeys) {
		super();
		Assert.isNotNull(duplicateKeys);
		fDuplicateKeys= duplicateKeys;
	}
	
	public Properties (Properties properties, Set duplicateKeys) {
		super(properties);
		Assert.isNotNull(duplicateKeys);
		fDuplicateKeys= duplicateKeys;
	}
	/*
	 * @see java.util.Map#put(Object, Object)
	 */
	public Object put(Object arg0, Object arg1) {
		if (arg0 != null && containsKey(arg0))
			fDuplicateKeys.add(arg0);
		return super.put(arg0, arg1);
	}
}
