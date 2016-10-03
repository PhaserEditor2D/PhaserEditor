/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Simple LineReader Helper. Returns lines including "line-break" characters.
 */
public class SimpleLineReader {

    private IDocument fInput;
    private int fCurrLine;

    public SimpleLineReader(IDocument input) {
        fInput = input;
        fCurrLine= 0;
    }    
    
    public String readLine() {
    	int nLines= fInput.getNumberOfLines();
    	if (fCurrLine >= nLines) {
    		return null;
    	}
    	
    	try {
			IRegion region= fInput.getLineInformation(fCurrLine++);
			String content= fInput.get(region.getOffset(), region.getLength());
			
			int start= region.getOffset();
			  	
			boolean continuesOnNext= content.endsWith("\\") && !isCommentOrWhiteSpace(content); //$NON-NLS-1$
			
			while (continuesOnNext && fCurrLine < nLines) {
				region= fInput.getLineInformation(fCurrLine++);
				content= fInput.get(region.getOffset(), region.getLength());
				continuesOnNext= content.endsWith("\\") && !isCommentOrWhiteSpace(content); //$NON-NLS-1$
			}
			int end;
			if (fCurrLine < nLines) {
				end= fInput.getLineOffset(fCurrLine); // beginning of next
			} else {
				end= fInput.getLength();
				if (end == start) {
					return null; // nd of file, empty line -> null
				}
			}
			return fInput.get(start, end - start);
		} catch (BadLocationException e) {
			// should not happen
			JavaScriptPlugin.log(e);
		}
		return null;
     }
    
    public static boolean isCommentOrWhiteSpace(String line) {
        line = line.trim();
        return (line.length() == 0) || line.charAt(0) == '!' || line.charAt(0) == '#';
    }   
}
