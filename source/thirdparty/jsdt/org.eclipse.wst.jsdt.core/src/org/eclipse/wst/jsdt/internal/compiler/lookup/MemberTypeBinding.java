/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;

public final class MemberTypeBinding extends NestedTypeBinding {
public MemberTypeBinding(char[][] compoundName, ClassScope scope, SourceTypeBinding enclosingType) {
	super(compoundName, scope, enclosingType);
	this.tagBits |= TagBits.MemberTypeMask;
}
/* Answer the receiver's constant pool name.
*
* NOTE: This method should only be used during/after code gen.
*/

public char[] constantPoolName() /* java/lang/Object */ {
	if (constantPoolName != null)
		return constantPoolName;

	return constantPoolName = CharOperation.concat(enclosingType().constantPoolName(), sourceName, '.');
}

public String toString() {
	return "Member type : " + new String(sourceName()) + " " + super.toString(); //$NON-NLS-2$ //$NON-NLS-1$
}
}
