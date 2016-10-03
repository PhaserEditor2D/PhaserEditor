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
 *  
 * Representation of a return statement.
 * <p>
 * @noimplement This interface is not intended to be implemented by clients.
 * </p>
 */

public interface IReturnStatement extends IStatement {

	IExpression getExpression();
	
	/**
	 * @param type
	 *            {@link InferredType} returned by this return statement
	 */
	void setInferredType(InferredType type);

	/**
	 * @return {@link InferredType} returned by this return statement
	 */
	InferredType getInferredType();

	/**
	 * @param isType
	 *            <code>true</code> if this return statement is actually
	 *            returning a type, rather then the instance of a type.
	 *            <code>false</code> if this return statement is returning an
	 *            instance of a type rather then the type itself.
	 */
	public void setIsType(boolean isType);

	/**
	 * @return <code>true</code> if this return statement is actually
	 *         returning a type, rather then the instance of a type.
	 *         <code>false</code> if this return statement is returning an
	 *         instance of a type rather then the type itself.
	 */
	public boolean isType();
}
