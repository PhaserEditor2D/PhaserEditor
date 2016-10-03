/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core;

/**
 * Abstract base implementation of all includepath variable initializers.
 * Includepath variable initializers are used in conjunction with the
 * "org.eclipse.wst.jsdt.core.JsGlobalScopeVariableInitializer" extension point.
 * <p>
 * Clients should subclass this class to implement a specific includepath
 * variable initializer. The subclass must have a public 0-argument
 * constructor and a concrete implementation of <code>initialize</code>.
 *
 * @see IIncludePathEntry
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class JsGlobalScopeVariableInitializer {

    /**
     * Creates a new includepath variable initializer.
     */
    public JsGlobalScopeVariableInitializer() {
    	// a includepath variable initializer must have a public 0-argument constructor
    }

    /**
     * Binds a value to the workspace includepath variable with the given name,
     * or fails silently if this cannot be done.
     * <p>
     * A variable initializer is automatically activated whenever a variable value
     * is needed and none has been recorded so far. The implementation of
     * the initializer can set the corresponding variable using
     * <code>JavaScriptCore#setClasspathVariable</code>.
     *
     * @param variable the name of the workspace includepath variable
     *    that requires a binding
     *
     * @see JavaScriptCore#getIncludepathVariable(String)
     * @see JavaScriptCore#setIncludepathVariable(String, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
     * @see JavaScriptCore#setIncludepathVariables(String[], org.eclipse.core.runtime.IPath[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public abstract void initialize(String variable);
}
