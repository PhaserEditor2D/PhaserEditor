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

import java.util.Map;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
class MethodReferencesSearchRequestor extends SearchRequestor {
    private CallSearchResultCollector fSearchResults;
    private boolean fRequireExactMatch = true;

    MethodReferencesSearchRequestor() {
        fSearchResults = new CallSearchResultCollector();
    }

    public Map getCallers() {
        return fSearchResults.getCallers();
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.wst.jsdt.core.search.SearchMatch)
     */
    public void acceptSearchMatch(SearchMatch match) {
        if (fRequireExactMatch && (match.getAccuracy() != SearchMatch.A_ACCURATE)) {
            return;
        }
        
        if (match.isInsideDocComment()) {
            return;
        }

        if (match.getElement() != null && match.getElement() instanceof IMember) {
            IMember member= (IMember) match.getElement();
            switch (member.getElementType()) {
                case IJavaScriptElement.METHOD:
                case IJavaScriptElement.TYPE:
                case IJavaScriptElement.FIELD:
                case IJavaScriptElement.INITIALIZER:
                    fSearchResults.addMember(member, member, match.getOffset(), match.getOffset()+match.getLength());
                    break;
            }
        }
    }
}
