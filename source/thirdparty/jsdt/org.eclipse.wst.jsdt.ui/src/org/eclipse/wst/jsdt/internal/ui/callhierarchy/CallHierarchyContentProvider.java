/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

public class CallHierarchyContentProvider implements ITreeContentProvider {
    private final static Object[] EMPTY_ARRAY = new Object[0];

    private DeferredTreeContentManager fManager;
    private CallHierarchyViewPart fPart;
    
    private class MethodWrapperRunnable implements IRunnableWithProgress {
        private MethodWrapper fMethodWrapper;
        private MethodWrapper[] fCalls= null;

        MethodWrapperRunnable(MethodWrapper methodWrapper) {
            fMethodWrapper= methodWrapper;
        }
                
        public void run(IProgressMonitor pm) {
        	fCalls= fMethodWrapper.getCalls(pm);
        }
        
        MethodWrapper[] getCalls() {
            if (fCalls != null) {
                return fCalls;
            }
            return new MethodWrapper[0];
        }
    }

    public CallHierarchyContentProvider(CallHierarchyViewPart part) {
        super();
        fPart= part;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof TreeRoot) {
            TreeRoot dummyRoot = (TreeRoot) parentElement;

            return new Object[] { dummyRoot.getRoot() };
        } else if (parentElement instanceof MethodWrapper) {
            MethodWrapper methodWrapper = ((MethodWrapper) parentElement);

            if (shouldStopTraversion(methodWrapper)) {
                return EMPTY_ARRAY;
            } else {
                if (fManager != null) {
                    Object[] children = fManager.getChildren(new DeferredMethodWrapper(this, methodWrapper));
                    if (children != null)
                        return children;
                }
                return fetchChildren(methodWrapper);            
            }
        }

        return EMPTY_ARRAY;
    }

    protected Object[] fetchChildren(MethodWrapper methodWrapper) {
        IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
        MethodWrapperRunnable runnable= new MethodWrapperRunnable(methodWrapper);
        try {
            context.run(true, true, runnable);
        } catch (InvocationTargetException e) {
            ExceptionHandler.handle(e, CallHierarchyMessages.CallHierarchyContentProvider_searchError_title, CallHierarchyMessages.CallHierarchyContentProvider_searchError_message);  
            return EMPTY_ARRAY;
        } catch (InterruptedException e) {
            return new Object[] { TreeTermination.SEARCH_CANCELED };
        }
        
        return runnable.getCalls();
    }

    private boolean shouldStopTraversion(MethodWrapper methodWrapper) {
        return (methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth()) || methodWrapper.isRecursive();
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        if (element instanceof MethodWrapper) {
            return ((MethodWrapper) element).getParent();
        }

        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
        // Nothing to dispose
    }

    /**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element == TreeRoot.EMPTY_ROOT || element == TreeTermination.SEARCH_CANCELED) {
			return false;
		}

		// Only methods can have subelements, so there's no need to fool the
		// user into believing that there is more
		if (element instanceof MethodWrapper) {
			MethodWrapper methodWrapper= (MethodWrapper) element;
			if (methodWrapper.getMember().getElementType() != IJavaScriptElement.METHOD) {
				return false;
			}
			if (shouldStopTraversion(methodWrapper)) {
				return false;
			}
			return true;
		} else if (element instanceof TreeRoot) {
			return true;
		} else if (element instanceof DeferredMethodWrapper) {
			// Err on the safe side by returning true even though
			// we don't know for sure that there are children.
			return true;
		}

		return false; // the "Update ..." placeholder has no children
	}

    /**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    	if (oldInput instanceof TreeRoot) {
    		Object root = ((TreeRoot) oldInput).getRoot();
    		if (root instanceof MethodWrapper) {
    			cancelJobs((MethodWrapper) root);
    		}
    	}
        if (viewer instanceof AbstractTreeViewer) {
            fManager = new DeferredTreeContentManager(this, (AbstractTreeViewer) viewer, fPart.getSite());
        }
    }

    /**
     * Cancel all current jobs. 
     */
    void cancelJobs(MethodWrapper wrapper) {
        if (fManager != null && wrapper != null) {
			fManager.cancel(wrapper);
            if (fPart != null) {
                fPart.setCancelEnabled(false);
            }
        }
    }

    /**
     * 
     */
    public void doneFetching() {
        if (fPart != null) {
            fPart.setCancelEnabled(false);
        }
    }

    /**
     * 
     */
    public void startFetching() {
        if (fPart != null) {
            fPart.setCancelEnabled(true);
        }
    }
}
