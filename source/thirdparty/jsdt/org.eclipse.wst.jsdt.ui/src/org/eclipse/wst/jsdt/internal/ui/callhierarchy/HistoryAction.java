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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

/**
 * Action used for the type hierarchy forward / backward buttons
 */
class HistoryAction extends Action {
    private static JavaScriptElementLabelProvider fLabelProvider = new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_POST_QUALIFIED |
            JavaScriptElementLabelProvider.SHOW_PARAMETERS |
            JavaScriptElementLabelProvider.SHOW_RETURN_TYPE);
    private CallHierarchyViewPart fView;
    private IFunction fMethod;

    public HistoryAction(CallHierarchyViewPart viewPart, IFunction element) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        fView = viewPart;
        fMethod = element;

        String elementName = getElementLabel(element);
        setText(elementName);
        setImageDescriptor(getImageDescriptor(element));

        setDescription(Messages.format(CallHierarchyMessages.HistoryAction_description, elementName)); 
        setToolTipText(Messages.format(CallHierarchyMessages.HistoryAction_tooltip, elementName)); 
        
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_HISTORY_ACTION);
    }

    private ImageDescriptor getImageDescriptor(IJavaScriptElement elem) {
        JavaElementImageProvider imageProvider = new JavaElementImageProvider();
        ImageDescriptor desc = imageProvider.getBaseImageDescriptor(elem, 0);
        imageProvider.dispose();

        return desc;
    }

    /*
     * @see Action#run()
     */
    public void run() {
        fView.gotoHistoryEntry(fMethod);
    }

    /**
     * @param element
     * @return String
     */
    private String getElementLabel(IJavaScriptElement element) {
        Assert.isNotNull(element);
        return fLabelProvider.getText(element);
    }
}
