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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class CallLocation implements IAdaptable {
    public static final int UNKNOWN_LINE_NUMBER= -1;
    private IMember fMember;
    private IMember fCalledMember;
    private int fStart;
    private int fEnd;

    private String fCallText;
    private int fLineNumber;

    public CallLocation(IMember member, IMember calledMember, int start, int end, int lineNumber) {
        this.fMember = member;
        this.fCalledMember = calledMember;
        this.fStart = start;
        this.fEnd = end;
        this.fLineNumber= lineNumber;
    }

    /**
     * @return IFunction
     */
    public IMember getCalledMember() {
        return fCalledMember;
    }

    /**
     *
     */
    public int getEnd() {
        return fEnd;
    }

    public IMember getMember() {
        return fMember;
    }

    /**
     *
     */
    public int getStart() {
        return fStart;
    }

    public int getLineNumber() {
    	initCallTextAndLineNumber();
        return fLineNumber;
    }
    
    public String getCallText() {
    	initCallTextAndLineNumber();
        return fCallText;
    }

    private void initCallTextAndLineNumber() {
    	if (fCallText != null)
    		return;
    	
        IBuffer buffer= getBufferForMember();
        if (buffer == null || buffer.getLength() < fEnd) { //binary, without source attachment || buffer contents out of sync (bug 121900)
        	fCallText= ""; //$NON-NLS-1$
        	fLineNumber= UNKNOWN_LINE_NUMBER;
        	return;
        }

        fCallText= buffer.getText(fStart, (fEnd - fStart));
        
        if (fLineNumber == UNKNOWN_LINE_NUMBER) {
            Document document= new Document(buffer.getContents());
            try {
                fLineNumber= document.getLineOfOffset(fStart) + 1;
            } catch (BadLocationException e) {
                JavaScriptPlugin.log(e);
            }
        }
    }
    
    /**
     * Returns the IBuffer for the IMember represented by this CallLocation.
     * 
     * @return IBuffer for the IMember or null if the member doesn't have a buffer (for
     *          example if it is a binary file without source attachment).
     */
    private IBuffer getBufferForMember() {
        IBuffer buffer = null;
        try {
            IOpenable openable = fMember.getOpenable();
            if (openable != null && fMember.exists()) {
                buffer = openable.getBuffer();
            }
        } catch (JavaScriptModelException e) {
            JavaScriptPlugin.log(e);
        }
        return buffer;
    }

    public String toString() {
        return getCallText();
    }
    
    public Object getAdapter(Class adapter) {
        if (IJavaScriptElement.class.isAssignableFrom(adapter)) {
            return getMember();
        }

        return null;
    }
}
