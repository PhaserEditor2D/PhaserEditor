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

package org.eclipse.wst.jsdt.core.ast;

import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;

/**
 * Abstract representation of a Function declaration.  
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */

public interface IAbstractFunctionDeclaration extends IStatement{

	/**
	 * Set the function arguments
	 * @param args IArgument[]
	 */
	public void setArguments( IArgument[] args);

	/**
	 * Get the function arguments
	 * @return arguments
	 */
	public IArgument[] getArguments();
	
	/**
	 * Get the function jsdoc
	 * @return jsdoc
	 */
	IJsDoc getJsDoc();

	/**
	 * Get the function statements
	 * @return statements
	 */
	IProgramElement[] getStatements();

	/**
	 * Get the function name
	 * 
	 * @return name
	 */
	char[] getName();

	/**
	 * Set the inferred return type
	 * 
	 * @param inferred return type
	 */
	void setInferredType(InferredType type);
	
	/**
	 * Get the inferred return type for the function
	 * @return inferred type
	 */
	InferredType getInferredType();

	/**
	 * Get the Inferred method associated with this function
	 * @return
	 */
	InferredMethod getInferredMethod();
}