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
package org.eclipse.wst.jsdt.core.util;

/**
 * Definition of the modifier constants.
 *
 * This interface is not intended to be implemented by clients.
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IModifierConstants {

	int ACC_PUBLIC       = 0x0001;
	int ACC_PRIVATE      = 0x0002;
	int ACC_PROTECTED    = 0x0004;
	int ACC_STATIC       = 0x0008;
	int ACC_FINAL        = 0x0010;
	int ACC_SUPER        = 0x0020;

	/**
	 * Indicates a variable arity method (added in J2SE 1.5).
	 *  
	 */
	int ACC_VARARGS      = 0x0080;
	int ACC_NATIVE       = 0x0100;
	int ACC_ABSTRACT     = 0x0400;
	int ACC_STRICT       = 0x0800;
}
