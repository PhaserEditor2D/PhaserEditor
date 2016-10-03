/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;


public abstract class WildcardType extends TType {
	protected TType fBound;
	
	protected WildcardType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding) {
		Assert.isTrue(false);
		super.initialize(binding);
	}
	
	public TType getBound() {
		return fBound;
	}
	
	public TType[] getSubTypes() {
		throw new UnsupportedOperationException();
	}
	
	public boolean doEquals(TType type) {
		WildcardType other= (WildcardType)type;
		if (fBound == null)
			return other.fBound == null;
		return fBound.equals(other.fBound);
	}
	
	public int hashCode() {
		if (fBound == null)
			return super.hashCode();
		return fBound.hashCode() << WILDCARD_TYPE_SHIFT;
	}
	
	protected abstract boolean checkAssignmentBound(TType rhs);
	
	// protected abstract boolean checkTypeArgumentBound(TType rhs);
	
	protected String internalGetName(String keyword) {
		StringBuffer result= new StringBuffer("?"); //$NON-NLS-1$
		TType bound= getBound();
		if (bound != null) {
			result.append(" "); //$NON-NLS-1$
			result.append(keyword);
			result.append(" "); //$NON-NLS-1$
			result.append(bound.getName());
		}
		return result.toString();
	}
	
	protected String internalGetPrettySignature(String keyword) {
		StringBuffer result= new StringBuffer("?"); //$NON-NLS-1$
		TType bound= getBound();
		if (bound != null) {
			result.append(" "); //$NON-NLS-1$
			result.append(keyword);
			result.append(" "); //$NON-NLS-1$
			result.append(bound.getPlainPrettySignature());
		}
		return result.toString();
	}	
}
