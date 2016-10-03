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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;


public class ReturnTypeInfo {
	
	private final String fOldTypeName;
	private String fNewTypeName;
	private ITypeBinding fNewTypeBinding;
	
	public ReturnTypeInfo(String returnType) {
		fOldTypeName= returnType;
		fNewTypeName= returnType;
	}

	public String getOldTypeName() {
		return fOldTypeName;
	}
	
	public String getNewTypeName() {
		return fNewTypeName;
	}
	
	public void setNewTypeName(String type){
		Assert.isNotNull(type);
		fNewTypeName= type;
	}

	public ITypeBinding getNewTypeBinding() {
		return fNewTypeBinding;
	}
	
	public void setNewTypeBinding(ITypeBinding typeBinding){
		fNewTypeBinding= typeBinding;
	}

	public boolean isTypeNameChanged() {
		return !fOldTypeName.equals(fNewTypeName);
	}
	
	public String toString() {
		return fOldTypeName + " -> " + fNewTypeName; //$NON-NLS-1$
	}
}
