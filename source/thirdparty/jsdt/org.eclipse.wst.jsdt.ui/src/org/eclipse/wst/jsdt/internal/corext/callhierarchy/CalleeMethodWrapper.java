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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
class CalleeMethodWrapper extends MethodWrapper {
    private Comparator fMethodWrapperComparator = new MethodWrapperComparator();

    private static class MethodWrapperComparator implements Comparator {
        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            MethodWrapper m1 = (MethodWrapper) o1;
            MethodWrapper m2 = (MethodWrapper) o2;

            CallLocation callLocation1 = m1.getMethodCall().getFirstCallLocation();
            CallLocation callLocation2 = m2.getMethodCall().getFirstCallLocation();

            if ((callLocation1 != null) && (callLocation2 != null)) {
                if (callLocation1.getStart() == callLocation2.getStart()) {
                    return callLocation1.getEnd() - callLocation2.getEnd();
                }

                return callLocation1.getStart() - callLocation2.getStart();
            }

            return 0;
        }
    }

    /**
     * Constructor for CalleeMethodWrapper.
     */
    public CalleeMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        super(parent, methodCall);
    }

	/* Returns the calls sorted after the call location
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper#getCalls()
     */
    public MethodWrapper[] getCalls(IProgressMonitor progressMonitor) {
        MethodWrapper[] result = super.getCalls(progressMonitor);
        Arrays.sort(result, fMethodWrapperComparator);

        return result;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper#getTaskName()
     */
    protected String getTaskName() {
        return CallHierarchyMessages.CalleeMethodWrapper_taskname; 
    }

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper#createMethodWrapper(org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodCall)
     */
    protected MethodWrapper createMethodWrapper(MethodCall methodCall) {
        return new CalleeMethodWrapper(this, methodCall);
    }

	/**
     * Find callees called from the current method.
	 * @see org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper#findChildren(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected Map findChildren(IProgressMonitor progressMonitor) {
    	if (getMember().exists() && getMember().getElementType() == IJavaScriptElement.METHOD) {
        	JavaScriptUnit cu= CallHierarchy.getCompilationUnitNode(getMember(), true);
            if (progressMonitor != null) {
                progressMonitor.worked(5);
            }

        	if (cu != null) {
        		CalleeAnalyzerVisitor visitor = new CalleeAnalyzerVisitor((IFunction) getMember(),
        				cu, progressMonitor);
        
        		cu.accept(visitor);
        		return visitor.getCallees();
        	}
        }
        return new HashMap(0);
    }
}
