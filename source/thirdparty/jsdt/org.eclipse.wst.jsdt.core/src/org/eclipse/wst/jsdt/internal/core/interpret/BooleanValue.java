/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.interpret;

public class BooleanValue extends Value {

	boolean value;
	public BooleanValue(boolean value) {
		super(BOOLEAN);
		this.value=value;
	}
	public boolean booleanValue() {
		return value;
	}
	public int numberValue() {
		return value? 1 : 0;
	}
	public String stringValue() {
		return value?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
