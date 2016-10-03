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
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.wst.jsdt.core.BindingKey;


public final class StandardType extends HierarchyType {
	
	private static final String OBJECT_KEY= BindingKey.createTypeBindingKey("java.lang.Object");   //$NON-NLS-1$
	private static final String CLONEABLE_KEY= BindingKey.createTypeBindingKey("java.lang.Cloneable"); //$NON-NLS-1$
	private static final String SERIALIZABLE_KEY= BindingKey.createTypeBindingKey("java.io.Serializable"); //$NON-NLS-1$

	protected StandardType(TypeEnvironment environment) {
		super(environment);
	}
	
	public int getKind() {
		return STANDARD_TYPE;
	}
	
	public boolean isJavaLangObject() {
		return OBJECT_KEY.equals(getBindingKey());
	}
	
	public boolean isJavaLangCloneable() {
		return CLONEABLE_KEY.equals(getBindingKey());
	}
	
	public boolean isJavaIoSerializable() {
		return SERIALIZABLE_KEY.equals(getBindingKey());
	}
	
	public boolean doEquals(TType type) {
		return getJavaElementType().equals(((StandardType)type).getJavaElementType());
	}
	
	public int hashCode() {
		return getJavaElementType().hashCode();
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		switch (lhs.getKind()) {
			case NULL_TYPE: return false;
			case VOID_TYPE: return false;
			case PRIMITIVE_TYPE: return canAssignToPrimitive((PrimitiveType)lhs);
			
			case ARRAY_TYPE: return false;
			
			case STANDARD_TYPE: return canAssignToStandardType((StandardType)lhs); 
			case GENERIC_TYPE: return false;
			case PARAMETERIZED_TYPE: return isSubType((HierarchyType)lhs);
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

	private boolean canAssignToPrimitive(PrimitiveType type) {
		PrimitiveType source= getEnvironment().createUnBoxed(this);
		return source != null && source.canAssignTo(type);
	}

	public String getName() {
		return getJavaElementType().getElementName();
	}
	
	protected String getPlainPrettySignature() {
		return getJavaElementType().getFullyQualifiedName('.');
	}
}
