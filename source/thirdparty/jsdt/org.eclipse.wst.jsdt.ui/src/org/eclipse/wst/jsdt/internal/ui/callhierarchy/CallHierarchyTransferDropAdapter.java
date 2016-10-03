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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.SelectionUtil;

class CallHierarchyTransferDropAdapter extends SelectionTransferDropAdapter {

	private static final int OPERATION = DND.DROP_LINK;
	private CallHierarchyViewPart fCallHierarchyViewPart;

	public CallHierarchyTransferDropAdapter(CallHierarchyViewPart viewPart, StructuredViewer viewer) {
		super(viewer);
		setFullWidthMatchesItem(false);
		fCallHierarchyViewPart= viewPart;
	}

	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		initializeSelection();
		if (target != null){
			super.validateDrop(target, event, operation);
			return;
		}	
		if (getInputElement(getSelection()) != null) 
			event.detail= OPERATION;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.packageview.SelectionTransferDropAdapter#isEnabled(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public boolean isEnabled(DropTargetEvent event) {
		return true;
	}

	public void drop(Object target, DropTargetEvent event) {
		if (target != null || event.detail != OPERATION){
			super.drop(target, event);
			return;
		}	
		IFunction input= getInputElement(getSelection());
		fCallHierarchyViewPart.setMethod(input);
	}
	
	private static IFunction getInputElement(ISelection selection) {
		Object single= SelectionUtil.getSingleElement(selection);
		if (single == null)
			return null;
		return getCandidate(single);
	}
    
    /**
     * Converts the input to a possible input candidates
     */ 
    public static IFunction getCandidate(Object input) {
        if (!(input instanceof IFunction)) {
            return null;
        }
        return (IFunction) input;
    }
}
