/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.eval;

/**
 * A global variable declared in an evaluation context.
 * <p>
 * This interface is not intended to be implemented by clients.
 * <code>IEvaluationContext.newVariable</code> can be used to obtain an instance.
 * </p>
 *
 * @see IEvaluationContext#newVariable(String, String, String)
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IGlobalVariable {
/**
 * Returns the initializer of this global variable.
 * The syntax for an initializer corresponds to VariableInitializer (JLS2 8.3).
 *
 * @return the initializer expression, or <code>null</code> if this global does
 *    not have an initializer
 */
public String getInitializer();
/**
 * Returns the name of this global variable.
 *
 * @return the name of the global variable
 */
public String getName();
/**
 * Returns the fully qualified name of the type of this global
 * variable, or its simple representation if it is a primitive type
 * (<code>int</code>, <code>boolean</code>, etc.).
 * <p>
 * The syntax for a type name corresponds to Type in Field Declaration (JLS2 8.3).
 * </p>
 * @return the type name
 */
public String getTypeName();
}
