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
 * Representation of an assignment expression
 * </p>
 *  
 *@noimplement This interface is not intended to be implemented by clients.
 */
public interface IAssignment extends IExpression {

	/**
	 * get the expression being assigned
	 * @return expression
	 */
	IExpression getExpression();

	/**
	 * The assignment target
	 * @return
	 */
	IExpression getLeftHandSide();
	
	IJsDoc getJsDoc();
	
	/**
	 * Set the inferred type of the assignment
	 * @param inferred type
	 */
	public void setInferredType(InferredType type);
	/**
	 * Get the inferred type of the assignment
	 * @return inferred type
	 */
	public InferredType getInferredType();
	
	/**
	 * @param isType
	 *            <code>true</code> if this assignment is actually a assigning
	 *            a type, rather then the instance of a type.
	 *            <code>false</code> if this assignment is a assigning an
	 *            instance of a type rather then the type itself.
	 */
	public void setIsType(boolean isType);

	/**
	 * @return <code>true</code> if this assignment is actually a assigning a
	 *         type, rather then the instance of a type. <code>false</code> if
	 *         this assignment is a assigning an instance of a type rather
	 *         then the type itself.
	 */
	public boolean isType();
}