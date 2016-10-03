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

package org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.StubTypeContext;


public abstract class CompletionContextRequestor {
	
	public abstract StubTypeContext getStubTypeContext();
	
	public IJavaScriptUnit getOriginalCu() {
		return getStubTypeContext().getCuHandle();
	}

	public String getBeforeString() {
		return getStubTypeContext().getBeforeString();
	}

	public String getAfterString() {
		return getStubTypeContext().getAfterString();
	}
	
}
