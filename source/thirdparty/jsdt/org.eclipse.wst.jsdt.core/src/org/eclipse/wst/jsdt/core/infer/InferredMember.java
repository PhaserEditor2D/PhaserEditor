/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;

import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;


/**
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class InferredMember extends ASTNode{

	/**
	 * The name of this member
	 */
	public char [] name;
	/**
	 * The type to which this member belongs
	 */
	public InferredType inType;
	/**
	 * The source offset at which the name of this member begins
	 */
	public int nameStart;
	public boolean isStatic = false;

	public boolean isInferred()
	{
		return true;
	}
}
