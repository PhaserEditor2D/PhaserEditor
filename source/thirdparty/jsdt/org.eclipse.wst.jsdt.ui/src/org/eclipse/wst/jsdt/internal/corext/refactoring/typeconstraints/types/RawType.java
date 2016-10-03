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


public final class RawType extends HierarchyType {
	
	private HierarchyType fTypeDeclaration;
	
	protected RawType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, IType javaElementType) {
		Assert.isTrue(false);
		super.initialize(binding, javaElementType);
		TypeEnvironment environment= getEnvironment();
		fTypeDeclaration= (HierarchyType)environment.create(binding.getTypeDeclaration());
	}
	
	public int getKind() {
		return RAW_TYPE;
	}
	
	public boolean doEquals(TType type) {
		return getJavaElementType().equals(((RawType)type).getJavaElementType());
	}
	
	public int hashCode() {
		return getJavaElementType().hashCode();
	}
	
	public TType getTypeDeclaration() {
		return fTypeDeclaration;
	}
	
	public TType getErasure() {
		return fTypeDeclaration;
	}
	
	/*package*/ HierarchyType getHierarchyType() {
		return fTypeDeclaration;
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		int targetType= lhs.getKind();
		switch (targetType) {
			case NULL_TYPE: return false;
			case VOID_TYPE: return false;
			case PRIMITIVE_TYPE: return false;
			
			case ARRAY_TYPE: return false;
			
			case STANDARD_TYPE: return canAssignToStandardType((StandardType)lhs); 
			case GENERIC_TYPE: return false;
			case RAW_TYPE: return isSubType((HierarchyType)lhs);
			
			case UNBOUND_WILDCARD_TYPE:
			case SUPER_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE: 
				return ((WildcardType)lhs).checkAssignmentBound(this);
			
			case TYPE_VARIABLE: return false;
			case CAPTURE_TYPE:
				return ((CaptureType)lhs).checkLowerBound(this);
		}
		return false;
	}

	protected boolean isTypeEquivalentTo(TType other) {
		int otherElementType= other.getKind();
		if (otherElementType == PARAMETERIZED_TYPE || otherElementType == GENERIC_TYPE)
			return getErasure().isTypeEquivalentTo(other.getErasure());
		return super.isTypeEquivalentTo(other);
	}

	public String getName() {
		return getJavaElementType().getElementName();
	}
	
	protected String getPlainPrettySignature() {
		return getJavaElementType().getFullyQualifiedName('.');
	}
}
