/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

public class CommentFormatCleanUp extends AbstractCleanUp {
	
	public CommentFormatCleanUp(Map options) {
		super(options);
	}
	
	public CommentFormatCleanUp() {
		super();
	}
	
	public IFix createFix(IJavaScriptUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		if (!isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			return null;
		
		HashMap preferences= new HashMap(compilationUnit.getJavaScriptProject().getOptions(true));
		
		boolean singleLineComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT));
		boolean blockComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT));
		boolean javaDoc= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT));

		return CommentFormatFix.createCleanUp(compilationUnit, singleLineComment, blockComment, javaDoc, preferences);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(JavaScriptUnit compilationUnit) throws CoreException {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(IJavaScriptUnit unit) throws CoreException {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(JavaScriptUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return null;
	}
	
	public Map getRequiredOptions() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		return null;
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" *A JSDoc comment\n"); //$NON-NLS-1$
		buf.append("* \n"); //$NON-NLS-1$
		buf.append(" */\n"); //$NON-NLS-1$
		
		return buf.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(JavaScriptUnit compilationUnit) {
		return -1;
	}
	
	public boolean canFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
		return false;
	}
	
}
