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

import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;

public abstract class VariableBinding extends Binding {

	public int modifiers;
	public TypeBinding type;
	public char[] name;
	public int id; // for flow-analysis (position in flowInfo bit vector)
	public long tagBits;

	public VariableBinding(char[] name, TypeBinding type, int modifiers) {
		this.name = name;
		this.type = type;
		this.modifiers = modifiers;
	}

	public char[] readableName() {
		return name;
	}

	public String toString() {
		String s = (type != null) ? type.debugName() : "UNDEFINED TYPE"; //$NON-NLS-1$
		s += " "; //$NON-NLS-1$
		s += (name != null) ? new String(name) : "UNNAMED FIELD"; //$NON-NLS-1$
		return s;
	}

	public abstract boolean isFor(
			AbstractVariableDeclaration variableDeclaration);
}