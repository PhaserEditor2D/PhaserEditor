/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;
import org.eclipse.wst.jsdt.ui.JSdocContentAccess;


public class ProposalInfo {

	private boolean fJavadocResolved= false;
	private String fJavadoc= null;

	protected IJavaScriptElement fElement;

	public ProposalInfo(IMember member) {
		fElement= member;
	}
	
	protected ProposalInfo() {
		fElement= null;
	}

	public IJavaScriptElement getJavaElement() throws JavaScriptModelException {
		return fElement;
	}

	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 *
	 * @param monitor a progress monitor
	 * @return the additional info text
	 */
	public final String getInfo(IProgressMonitor monitor) {
		if (!fJavadocResolved) {
			fJavadocResolved= true;
			fJavadoc= computeInfo(monitor);
		}
		return fJavadoc;
	}

	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 *
	 * @param monitor a progress monitor
	 * @return the additional info text
	 */
	private String computeInfo(IProgressMonitor monitor) {
		try {
			final IJavaScriptElement javaElement= getJavaElement();
			return extractJavadoc(javaElement, monitor);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		} catch (IOException e) {
			JavaScriptPlugin.log(e);
		}
		return null;
	}

	/**
	 * Extracts the javadoc for the given <code>IMember</code> and returns it
	 * as HTML.
	 *
	 * @param element the member to get the documentation for
	 * @param monitor a progress monitor
	 * @return the javadoc for <code>member</code> or <code>null</code> if
	 *         it is not available
	 * @throws JavaScriptModelException if accessing the javadoc fails
	 * @throws IOException if reading the javadoc fails
	 */
	private String extractJavadoc(IJavaScriptElement element, IProgressMonitor monitor) throws JavaScriptModelException, IOException {
		if (element != null) {
			Reader reader =  getHTMLContentReader(element, monitor);
			if (reader != null)
				return getString(reader);
		}
		return null;
	}

	private Reader getHTMLContentReader(IJavaScriptElement element, IProgressMonitor monitor) throws JavaScriptModelException {
		Reader contentReader= JSdocContentAccess.getHTMLContentReader(element, true, true);
        if (contentReader != null) {
        	return contentReader;
        }

        contentReader= JSdocContentAccess.getContentReader(element, true);
        if (contentReader != null) {
        	return new JavaDoc2HTMLTextReader(contentReader);
        }
	        
        if (element.getOpenable().getBuffer() == null) { // only if no source available
        	String s= element.getAttachedJavadoc(monitor);
        	if (s != null)
        		return new StringReader(s);
        }
        return null;
    }
	
	/**
	 * Gets the reader content as a String
	 */
	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}
}
