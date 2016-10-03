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
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;


public final class GenericType extends HierarchyType {
	
	protected GenericType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, IType javaElementType) {
		Assert.isTrue(false);
		super.initialize(binding, javaElementType);
	}
	
	public int getKind() {
		return GENERIC_TYPE;
	}
	
	public boolean doEquals(TType type) {
		return getJavaElementType().equals(((GenericType)type).getJavaElementType());
	}
	
	public int hashCode() {
		return getJavaElementType().hashCode();
	}
	
	protected boolean doCanAssignTo(TType type) {
		return false;
	}
	
	protected boolean isTypeEquivalentTo(TType other) {
		int otherElementType= other.getKind();
		if (otherElementType == RAW_TYPE || otherElementType == PARAMETERIZED_TYPE)
			return getErasure().isTypeEquivalentTo(other.getErasure());
		return super.isTypeEquivalentTo(other);
	}
	
	public String getName() {
		return getJavaElementType().getElementName();
	}
	
	protected String getPlainPrettySignature() {
		StringBuffer result= new StringBuffer(getJavaElementType().getFullyQualifiedName('.'));
		result.append("<"); //$NON-NLS-1$
		result.append(">"); //$NON-NLS-1$
		return result.toString();
	}
}
