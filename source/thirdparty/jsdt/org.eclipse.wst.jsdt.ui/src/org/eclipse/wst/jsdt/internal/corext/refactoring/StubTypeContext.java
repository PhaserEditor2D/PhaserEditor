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

package org.eclipse.wst.jsdt.internal.corext.refactoring;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;


public class StubTypeContext {
	private String fBeforeString;
	private String fAfterString;
	private final IJavaScriptUnit fCuHandle;
	
	public StubTypeContext(IJavaScriptUnit cuHandle, String beforeString, String afterString) {
		fCuHandle= cuHandle;
		fBeforeString= beforeString;
		fAfterString= afterString;
	}
	
	public IJavaScriptUnit getCuHandle() {
		return fCuHandle;
	}
	
	public String getBeforeString() {
		return fBeforeString;
	}
	
	public String getAfterString() {
		return fAfterString;
	}
}
