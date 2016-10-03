/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;


/**
 * 
 * This represents an inferred attribute.
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class InferredAttribute extends InferredMember{

	public FieldBinding binding;
	public int initializationStart=-1;
	
	/**
	 * The type of this attribute
	 */
	public InferredType type;
	public ASTNode node;
	public int modifiers;
	
	/**
	 * <p>
	 * <code>true</code> if this attribute is actually a type,
	 * rather then the instance of a type. <code>false</code> if this attribute is
	 * an instance of a type rather then the type itself.
	 * </p>
	 */
	private boolean fIsType;

	/**
	 * Creates an attribute with this name in the given inferred type. This
	 * method is <b>discouraged</b> in favor of supplying the ASTNode which declared
	 * the attribute.
	 */
	public InferredAttribute(char [] name, InferredType inType, int start, int end)
	{
		this.name=name;
		this.inType = inType;
		this.sourceStart=start;
		this.sourceEnd=end;
	}


	/**
	 * @param name
	 * @param inferredType the type to which this attribute belongs
	 * @param definer
	 */
	public InferredAttribute(char[] name, InferredType inferredType, IASTNode definer) {
		this(name, inferredType, definer.sourceStart(), definer.sourceEnd());
		node = (ASTNode) definer;
	}


	public StringBuffer print(int indent, StringBuffer output)
	{
		String modifier=(isStatic)? "static ":""; //$NON-NLS-1$ //$NON-NLS-2$
		printIndent(indent, output).append(modifier);
	   if (type!=null)
		   type.dumpReference(output);
	   else
		   output.append("??"); //$NON-NLS-1$
	   output.append(" ").append(name); //$NON-NLS-1$
	   return output;
	}
	
	/**
	 * @param isType
	 *            <code>true</code> if this attribute is actually a type,
	 *            rather then the instance of a type. <code>false</code> if
	 *            this attribute is an instance of a type rather then the type
	 *            itself.
	 */
	public void setIsType(boolean isType) {
		this.fIsType = isType;
	}

	/**
	 * @return <code>true</code> if this attribute is actually a type, rather
	 *         then the instance of a type. <code>false</code> if this
	 *         attribute is an instance of a type rather then the type itself.
	 */
	public boolean isType() {
		return this.fIsType;
	}
}