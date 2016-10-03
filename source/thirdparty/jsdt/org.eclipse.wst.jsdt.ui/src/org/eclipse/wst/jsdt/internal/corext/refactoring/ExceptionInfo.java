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
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;


public class ExceptionInfo {
	private final IType fType;
	private final ITypeBinding fTypeBinding;
	private int fKind;

	public static final int OLD= 0;
	public static final int ADDED= 1;
	public static final int DELETED= 2;
	
	public ExceptionInfo(IType type, int kind, ITypeBinding binding) {
		Assert.isNotNull(type);
		fType= type;
		fKind= kind;
		fTypeBinding= binding;
	}

	public static ExceptionInfo createInfoForOldException(IType type, ITypeBinding binding){
		return new ExceptionInfo(type, OLD, binding);
	}
	public static ExceptionInfo createInfoForAddedException(IType type){
		return new ExceptionInfo(type, ADDED, null);
	}
	
	public void markAsDeleted(){
		Assert.isTrue(! isAdded());//added exception infos should be simply removed from the list
		fKind= DELETED;
	}
	
	public void markAsOld(){
		Assert.isTrue(isDeleted());
		fKind= OLD;
	}
	
	public boolean isAdded(){
		return fKind == ADDED;
	}
	
	public boolean isDeleted(){
		return fKind == DELETED;
	}
	
	public boolean isOld(){
		return fKind == OLD;
	}
	
	public IType getType() {
		return fType;
	}
	
	public int getKind() {
		return fKind;
	}
	
	/**
	 * @return ITypeBinding the typeBinding (for OLD and DELETED exceptions) or <code>null</code>
	 */
	public ITypeBinding getTypeBinding() {
		return fTypeBinding;
	}
	
	public String toString() {
		StringBuffer result= new StringBuffer();
		switch (fKind) {
			case OLD : result.append("OLD: "); break; //$NON-NLS-1$
			case ADDED : result.append("ADDED: "); break; //$NON-NLS-1$
			case DELETED : result.append("DELETED: "); break; //$NON-NLS-1$
		}
		if (fType == null)
			result.append("null"); //$NON-NLS-1$
		else
			result.append(fType.toString());
		return result.toString();
	}
}
