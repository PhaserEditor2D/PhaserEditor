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

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

class LocationLabelProvider extends LabelProvider implements ITableLabelProvider {
    private static final int COLUMN_ICON= 0;
    private static final int COLUMN_LINE= 1;
    private static final int COLUMN_INFO= 2;
        
    LocationLabelProvider() {
        // Do nothing
    }
            
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
        return getColumnText(element, COLUMN_INFO);
    }

    public Image getImage(Object element) {
        return getColumnImage(element, COLUMN_ICON);
    }
    
    private String removeWhitespaceOutsideStringLiterals(CallLocation callLocation) {
        StringBuffer buf = new StringBuffer();
        boolean withinString = false;

        String s= callLocation.getCallText();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (ch == '"') {
                withinString = !withinString;
            }

            if (withinString) {
                buf.append(ch);
            } else if (Character.isWhitespace(ch)) {
                if ((buf.length() == 0) ||
                            !Character.isWhitespace(buf.charAt(buf.length() - 1))) {
                    if (ch != ' ') {
                        ch = ' ';
                    }

                    buf.append(ch);
                }
            } else {
                buf.append(ch);
            }
        }

        return buf.toString();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
        if (columnIndex == COLUMN_ICON) {
            return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    public String getColumnText(Object element, int columnIndex) {
        if (element instanceof CallLocation) {
            CallLocation callLocation= (CallLocation) element;
            
            switch (columnIndex) {
                case COLUMN_LINE:
                    int lineNumber= callLocation.getLineNumber();
                    if (lineNumber == CallLocation.UNKNOWN_LINE_NUMBER) {
						return CallHierarchyMessages.LocationLabelProvider_unknown;
                    } else {
                    	return String.valueOf(lineNumber);
                    }
                case COLUMN_INFO:
                    return removeWhitespaceOutsideStringLiterals(callLocation);
            }
        }

        return ""; //$NON-NLS-1$        
    }
}
