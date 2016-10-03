/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Toggles the call direction of the call hierarchy (i.e. toggles between showing callers and callees.)
 */
class ToggleCallModeAction extends Action {

    private CallHierarchyViewPart fView;    
    private int fMode;
    
    public ToggleCallModeAction(CallHierarchyViewPart v, int mode) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        if (mode == CallHierarchyViewPart.CALL_MODE_CALLERS) {
            setText(CallHierarchyMessages.ToggleCallModeAction_callers_label); 
            setDescription(CallHierarchyMessages.ToggleCallModeAction_callers_description); 
            setToolTipText(CallHierarchyMessages.ToggleCallModeAction_callers_tooltip); 
            JavaPluginImages.setLocalImageDescriptors(this, "ch_callers.gif"); //$NON-NLS-1$
        } else if (mode == CallHierarchyViewPart.CALL_MODE_CALLEES) {
            setText(CallHierarchyMessages.ToggleCallModeAction_callees_label); 
            setDescription(CallHierarchyMessages.ToggleCallModeAction_callees_description); 
            setToolTipText(CallHierarchyMessages.ToggleCallModeAction_callees_tooltip); 
            JavaPluginImages.setLocalImageDescriptors(this, "ch_callees.gif"); //$NON-NLS-1$
        } else {
            Assert.isTrue(false);
        }
        fView= v;
        fMode= mode;
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_CALL_MODE_ACTION);
    }
    
    public int getMode() {
        return fMode;
    }   
    
    /*
     * @see Action#actionPerformed
     */     
    public void run() {
        fView.setCallMode(fMode); // will toggle the checked state
    }
    
}
