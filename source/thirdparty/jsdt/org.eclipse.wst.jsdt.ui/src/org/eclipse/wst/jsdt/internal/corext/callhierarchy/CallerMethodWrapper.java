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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
class CallerMethodWrapper extends MethodWrapper {
    public CallerMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        super(parent, methodCall);
    }

    protected IJavaScriptSearchScope getSearchScope() {
        return CallHierarchy.getDefault().getSearchScope();
    }

    protected String getTaskName() {
        return CallHierarchyMessages.CallerMethodWrapper_taskname; 
    }

    /* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper#createMethodWrapper(org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodCall)
	 */
	protected MethodWrapper createMethodWrapper(MethodCall methodCall) {
        return new CallerMethodWrapper(this, methodCall);
    }

	/**
	 * @return The result of the search for children
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper#findChildren(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected Map findChildren(IProgressMonitor progressMonitor) {
		try {
			MethodReferencesSearchRequestor searchRequestor= new MethodReferencesSearchRequestor();
			SearchEngine searchEngine= new SearchEngine();

			IProgressMonitor monitor= new SubProgressMonitor(progressMonitor, 95, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
			IJavaScriptSearchScope defaultSearchScope= getSearchScope();
			boolean isWorkspaceScope= SearchEngine.createWorkspaceScope().equals(defaultSearchScope);

			for (Iterator iter= getMembers().iterator(); iter.hasNext();) {
				checkCanceled(progressMonitor);

				IMember member= (IMember) iter.next();
				SearchPattern pattern= SearchPattern.createPattern(member, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
				IJavaScriptSearchScope searchScope= isWorkspaceScope ? getAccurateSearchScope(defaultSearchScope, member) : defaultSearchScope;
				searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, searchScope, searchRequestor,
						monitor);
			}
			return searchRequestor.getCallers();
			
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
			return new HashMap(0);
		}
	}

	private IJavaScriptSearchScope getAccurateSearchScope(IJavaScriptSearchScope defaultSearchScope, IMember member) throws JavaScriptModelException {
		if (! JdtFlags.isPrivate(member))
			return defaultSearchScope;
		
		if (member.getJavaScriptUnit() != null) {
			return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member.getJavaScriptUnit() });
		} else if (member.getClassFile() != null) {
			// member could be called from an inner class-> search
			// package fragment (see also bug 109053):
			return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT) });
		} else {
			return defaultSearchScope;
		}
	}

    /**
     * Returns a collection of IMember instances representing what to search for 
     */
    private Collection getMembers() {
        Collection result = new ArrayList();

        result.add(getMember());

        return result;
    }
}
