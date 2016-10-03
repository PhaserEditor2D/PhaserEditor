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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class JavaImplementorFinder implements IImplementorFinder {
    /* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.IImplementorFinder#findImplementingTypes(org.eclipse.wst.jsdt.core.IType, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Collection findImplementingTypes(IType type, IProgressMonitor progressMonitor) {
        ITypeHierarchy typeHierarchy;

        try {
            typeHierarchy = type.newTypeHierarchy(progressMonitor);

            IType[] implementingTypes = typeHierarchy.getAllClasses();
            HashSet result = new HashSet(Arrays.asList(implementingTypes));

            return result;
        } catch (JavaScriptModelException e) {
            JavaScriptPlugin.log(e);
        }

        return null;
    }
}
