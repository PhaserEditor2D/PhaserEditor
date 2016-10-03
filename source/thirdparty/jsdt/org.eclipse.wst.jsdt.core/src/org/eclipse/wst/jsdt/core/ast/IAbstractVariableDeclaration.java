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

package org.eclipse.wst.jsdt.core.ast;

import org.eclipse.wst.jsdt.core.infer.InferredType;
/**
 * <p>
 * Abstract representation of a var.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAbstractVariableDeclaration extends IStatement{
	/**
	 * Set the inferred type of the var
	 * @param inferred type
	 */
	public void setInferredType(InferredType type);
	/**
	 * Get the inferred type of the var
	 * @return inferred type
	 */
	public InferredType getInferredType();
	/**
	 * get the var name
	 * @return name
	 */
	public char[] getName();
	/**
	 * Get the initialization expression of the var
	 * @return initialization expression
	 */
	public IExpression getInitialization();
	/**
	 * get the JSDoc for the var
	 * @return jsdoc
	 */
	public IJsDoc getJsDoc();
	
	/**
	 * @param isType
	 *            <code>true</code> if this variable declaration is actually a
	 *            reference to a type, rather then the instance of a type.
	 *            <code>false</code> if this variable is a reference to an
	 *            instance of a type rather then the type itself.
	 */
	public void setIsType(boolean isType);

	/**
	 * @return <code>true</code> if this variable declaration is actually a
	 *         reference to a type, rather then the instance of a type.
	 *         <code>false</code> if this variable is a reference to an
	 *         instance of a type rather then the type itself.
	 */
	public boolean isType();
}