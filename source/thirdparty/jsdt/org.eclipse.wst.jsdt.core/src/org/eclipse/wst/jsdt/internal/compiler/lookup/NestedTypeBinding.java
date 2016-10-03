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
package org.eclipse.wst.jsdt.internal.compiler.lookup;

public class NestedTypeBinding extends SourceTypeBinding {

	public SourceTypeBinding enclosingType;

	public int enclosingInstancesSlotSize; // amount of slots used by synthetic enclosing instances
	public int outerLocalVariablesSlotSize; // amount of slots used by synthetic outer local variables

	public NestedTypeBinding(char[][] typeName, ClassScope scope, SourceTypeBinding enclosingType) {
		super(typeName, enclosingType.fPackage, scope);
		this.tagBits |= TagBits.IsNestedType;
		this.enclosingType = enclosingType;
	}

	/* Answer the receiver's enclosing type... null if the receiver is a top level type.
	*/
	public ReferenceBinding enclosingType() {

		return enclosingType;
	}
}
