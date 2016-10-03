/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.callhierarchy;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class Implementors {
    private static Implementors fgInstance;

    /**
     * Returns the shared instance.
     */
    public static Implementors getInstance() {
        if (fgInstance == null) {
            fgInstance = new Implementors();
        }

        return fgInstance;
    }

    /**
     * Searches for implementors of the specified Java elements. Currently, only IFunction
     * instances are searched for. Also, only the first element of the elements
     * parameter is taken into consideration.
     *
     * @param elements
     *
     * @return An array of found implementing Java elements (currently only IFunction
     *         instances)
     */
    public IJavaScriptElement[] searchForImplementors(IJavaScriptElement[] elements,
        IProgressMonitor progressMonitor) {
        return null;
    }
}
