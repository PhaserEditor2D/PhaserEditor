/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
class CallSearchResultCollector {
    private Map fCalledMembers;

    public CallSearchResultCollector() {
        this.fCalledMembers = createCalledMethodsData();
    }

    public Map getCallers() {
        return fCalledMembers;
    }

    protected void addMember(IMember member, IMember calledMember, int start, int end) {
        addMember(member, calledMember, start, end, CallLocation.UNKNOWN_LINE_NUMBER);
    }

    protected void addMember(IMember member, IMember calledMember, int start, int end, int lineNumber) {
        if ((member != null) && (calledMember != null)) {
            if (!isIgnored(calledMember)) {
                MethodCall methodCall = (MethodCall) fCalledMembers.get(calledMember.getHandleIdentifier());

                if (methodCall == null) {
                    methodCall = new MethodCall(calledMember);
                    fCalledMembers.put(calledMember.getHandleIdentifier(), methodCall);
                }

                methodCall.addCallLocation(new CallLocation(member, calledMember, start,
                        end, lineNumber));
            }
        }
    }

    protected Map createCalledMethodsData() {
        return new HashMap();
    }

    /**
     * Method isIgnored.
     * @param enclosingElement
     * @return boolean
     */
    private boolean isIgnored(IMember enclosingElement) {
    	IType type = getTypeOfElement(enclosingElement);
        String fullyQualifiedName = (type!=null)? type.getFullyQualifiedName() :
        	enclosingElement.getJavaScriptUnit().getElementName();

        return CallHierarchy.getDefault().isIgnored(fullyQualifiedName);
    }

    private IType getTypeOfElement(IMember element) {
        if (element.getElementType() == IJavaScriptElement.TYPE) {
            return (IType) element;
        }

        return element.getDeclaringType();
    }
}
