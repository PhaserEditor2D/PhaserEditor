/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.wst.jsdt.core.IMember;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class MethodCall {
    private IMember fMember;
    private List fCallLocations;

    /**
     * @param enclosingElement
     */
    public MethodCall(IMember enclosingElement) {
        this.fMember = enclosingElement;
    }

    /**
     *
     */
    public Collection getCallLocations() {
        return fCallLocations;
    }

    public CallLocation getFirstCallLocation() {
        if ((fCallLocations != null) && !fCallLocations.isEmpty()) {
            return (CallLocation) fCallLocations.get(0);
        } else {
            return null;
        }
    }

    public boolean hasCallLocations() {
        return fCallLocations != null && fCallLocations.size() > 0;
    }
    
    /**
     * @return Object
     */
    public Object getKey() {
        return getMember().getHandleIdentifier();
    }

    /**
     *
     */
    public IMember getMember() {
        return fMember;
    }

    /**
     * @param location
     */
    public void addCallLocation(CallLocation location) {
        if (fCallLocations == null) {
            fCallLocations = new ArrayList();
        }

        fCallLocations.add(location);
    }
}
