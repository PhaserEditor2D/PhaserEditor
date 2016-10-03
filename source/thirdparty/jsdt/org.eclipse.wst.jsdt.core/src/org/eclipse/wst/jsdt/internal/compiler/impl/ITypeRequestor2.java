/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.impl;

import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;

public interface ITypeRequestor2 extends ITypeRequestor {

	/**
	 * Accept the requested type's compilation unit, building and
	 * completing type bindings only for the given type names.
	 */
	void accept(ICompilationUnit unit, char[][] typeNames, AccessRestriction accessRestriction);

}
