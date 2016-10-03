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

public final class UnboundWildcardType extends WildcardType {

	protected UnboundWildcardType(TypeEnvironment environment) {
		super(environment);
	}

	public int getKind() {
		return UNBOUND_WILDCARD_TYPE;
	}
	
	public TType getErasure() {
		return getEnvironment().getJavaLangObject();
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		switch(lhs.getKind()) {
			case STANDARD_TYPE:
				return ((StandardType)lhs).isJavaLangObject();
			case UNBOUND_WILDCARD_TYPE:
				return true;
			case SUPER_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE:
				return ((WildcardType)lhs).getBound().isJavaLangObject();
			case CAPTURE_TYPE:
				return ((CaptureType)lhs).checkLowerBound(this);
			default:
				return false;
		}
	}
	
	protected boolean checkTypeArgument(TType rhs) {
		switch(rhs.getKind()) {
			case ARRAY_TYPE:
			case STANDARD_TYPE:
			case PARAMETERIZED_TYPE:
			case RAW_TYPE:
			case UNBOUND_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE: 
			case SUPER_WILDCARD_TYPE:
			case TYPE_VARIABLE:
			case CAPTURE_TYPE:
				return true;
			default:
				return false;
		}
	}
	
	protected boolean checkAssignmentBound(TType rhs) {
		// unbound equals ? extends Object.
		return rhs.isNullType();
	}
	
	public String getName() {
		return "?"; //$NON-NLS-1$
	}
	
	protected String getPlainPrettySignature() {
		return getName();
	}
}
