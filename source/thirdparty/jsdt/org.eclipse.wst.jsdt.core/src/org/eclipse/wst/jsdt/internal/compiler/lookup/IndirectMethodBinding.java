/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

public class IndirectMethodBinding extends MethodBinding {
	TypeBinding receiverType;

	public IndirectMethodBinding(int modifiers, TypeBinding receiverType, TypeBinding[] parameters,ReferenceBinding declaringClass)
	{
		super(modifiers,null,TypeBinding.UNKNOWN,parameters,declaringClass);
		this.receiverType=receiverType;
	}

}
