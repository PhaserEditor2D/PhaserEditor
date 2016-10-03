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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.internal.ui.callhierarchy.MethodWrapperWorkbenchAdapter;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public abstract class MethodWrapper extends PlatformObject {
    private Map fElements = null;

    /*
     * A cache of previously found methods. This cache should be searched
     * before adding a "new" method object reference to the list of elements.
     * This way previously found methods won't be searched again.
     */
    private Map fMethodCache;
    private MethodCall fMethodCall;
    private MethodWrapper fParent;
    private int fLevel;

    /**
     * Constructor CallerElement.
     */
    public MethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        Assert.isNotNull(methodCall);

        if (parent == null) {
            setMethodCache(new HashMap());
            fLevel = 1;
        } else {
            setMethodCache(parent.getMethodCache());
            fLevel = parent.getLevel() + 1;
        }

        this.fMethodCall = methodCall;
        this.fParent = parent;
    }

    public Object getAdapter(Class adapter) {
		if (adapter == IJavaScriptElement.class) {
	        return getMember();
	    } else if (adapter == IWorkbenchAdapter.class){
	    	return new MethodWrapperWorkbenchAdapter(this);
	    } else {
	    	return null;
	    }
	}

    /**
     * @return the child caller elements of this element
     */
    public MethodWrapper[] getCalls(IProgressMonitor progressMonitor) {
        if (fElements == null) {
            doFindChildren(progressMonitor);
        }

        MethodWrapper[] result = new MethodWrapper[fElements.size()];
        int i = 0;

        for (Iterator iter = fElements.keySet().iterator(); iter.hasNext();) {
            MethodCall methodCall = getMethodCallFromMap(fElements, iter.next());
            result[i++] = createMethodWrapper(methodCall);
        }

        return result;
    }

    public int getLevel() {
        return fLevel;
    }

    public IMember getMember() {
        return getMethodCall().getMember();
    }

    public MethodCall getMethodCall() {
        return fMethodCall;
    }

    public String getName() {
        if (getMethodCall() != null) {
            return getMethodCall().getMember().getElementName();
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public MethodWrapper getParent() {
        return fParent;
    }

    public boolean equals(Object oth) {
        if (this == oth) {
            return true;
        }

        if (oth == null) {
            return false;
        }
        
        if (oth instanceof MethodWrapperWorkbenchAdapter) {
            //Note: A MethodWrapper is equal to a referring MethodWrapperWorkbenchAdapter and vice versa (bug 101677).
        	oth= ((MethodWrapperWorkbenchAdapter) oth).getMethodWrapper();
        }
        
        if (oth.getClass() != getClass()) {
            return false;
        }

        MethodWrapper other = (MethodWrapper) oth;

        if (this.fParent == null) {
            if (other.fParent != null) {
                return false;
            }
        } else {
            if (!this.fParent.equals(other.fParent)) {
                return false;
            }
        }

        if (this.getMethodCall() == null) {
            if (other.getMethodCall() != null) {
                return false;
            }
        } else {
            if (!this.getMethodCall().equals(other.getMethodCall())) {
                return false;
            }
        }

        return true;
    }

    public int hashCode() {
        final int PRIME = 1000003;
        int result = 0;

        if (fParent != null) {
            result = (PRIME * result) + fParent.hashCode();
        }

        if (getMethodCall() != null) {
            result = (PRIME * result) + getMethodCall().getMember().hashCode();
        }

        return result;
    }

    private void setMethodCache(Map methodCache) {
        fMethodCache = methodCache;
    }

    protected abstract String getTaskName();

    private void addCallToCache(MethodCall methodCall) {
        Map cachedCalls = lookupMethod(this.getMethodCall());
        cachedCalls.put(methodCall.getKey(), methodCall);
    }

    protected abstract MethodWrapper createMethodWrapper(MethodCall methodCall);

    private void doFindChildren(IProgressMonitor progressMonitor) {
        Map existingResults = lookupMethod(getMethodCall());

        if (existingResults != null) {
            fElements = new HashMap();
            fElements.putAll(existingResults);
        } else {
            initCalls();

            if (progressMonitor != null) {
                progressMonitor.beginTask(getTaskName(), 100);
            }

            try {
                performSearch(progressMonitor);
            } finally {
                if (progressMonitor != null) {
                    progressMonitor.done();
                }
            }

            //                ModalContext.run(getRunnableWithProgress(), true, getProgressMonitor(),
            //                    Display.getCurrent());
        }
    }

    /**
     * Determines if the method represents a recursion call (i.e. whether the
     * method call is already in the cache.)
     *
     * @return True if the call is part of a recursion
     */
    public boolean isRecursive() {
        MethodWrapper current = getParent();

        while (current != null) {
            if (getMember().getHandleIdentifier().equals(current.getMember()
                                                                        .getHandleIdentifier())) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    /**
     * This method finds the children of the current IFunction (either callers or
     * callees, depending on the concrete subclass.
     * @return The result of the search for children
     */
    protected abstract Map findChildren(IProgressMonitor progressMonitor);

    private Map getMethodCache() {
        return fMethodCache;
    }

    private void initCalls() {
        this.fElements = new HashMap();

        initCacheForMethod();
    }

    /**
     * Looks up a previously created search result in the "global" cache.
     * @return the List of previously found search results
     */
    private Map lookupMethod(MethodCall methodCall) {
        return (Map) getMethodCache().get(methodCall.getKey());
    }

    private void performSearch(IProgressMonitor progressMonitor) {
        fElements = findChildren(progressMonitor);

        for (Iterator iter = fElements.keySet().iterator(); iter.hasNext();) {
            checkCanceled(progressMonitor);

            MethodCall methodCall = getMethodCallFromMap(fElements, iter.next());
            addCallToCache(methodCall);
        }
    }

    private MethodCall getMethodCallFromMap(Map elements, Object key) {
        return (MethodCall) elements.get(key);
    }

    private void initCacheForMethod() {
        Map cachedCalls = new HashMap();
        getMethodCache().put(this.getMethodCall().getKey(), cachedCalls);
    }

    /**
     * Checks with the progress monitor to see whether the creation of the type hierarchy
     * should be canceled. Should be regularly called
     * so that the user can cancel.
     *
     * @exception OperationCanceledException if cancelling the operation has been requested
     * @see IProgressMonitor#isCanceled
     */
    protected void checkCanceled(IProgressMonitor progressMonitor) {
        if (progressMonitor != null && progressMonitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }
    
    /**
     * Allows a visitor to traverse the call hierarchy. The visiting is stopped when
     * a recursive node is reached.
     *  
     * @param visitor
     */
    public void accept(CallHierarchyVisitor visitor, IProgressMonitor progressMonitor) {
        if (getParent() != null && getParent().isRecursive()) {
            return;
        }
        checkCanceled(progressMonitor);
        
        visitor.preVisit(this);
        if (visitor.visit(this)) {
            MethodWrapper[] methodWrappers= getCalls(progressMonitor);
            for (int i= 0; i < methodWrappers.length; i++) {
                methodWrappers[i].accept(visitor, progressMonitor);
            }
        }
        visitor.postVisit(this);

        if (progressMonitor != null) {        
            progressMonitor.worked(1);
        }
    }
}
