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
package org.eclipse.wst.jsdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.fix.StringFix;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can solve problems in connection with Strings
 * @see org.eclipse.wst.jsdt.internal.corext.fix.StringFix
 *
 */
public class StringCleanUp extends AbstractCleanUp {
	
	public StringCleanUp(Map options) {
		super(options);
	}
	
	public StringCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(IJavaScriptUnit unit) throws CoreException {
	    return isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS) || 
		       isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);
	}

	public IFix createFix(JavaScriptUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;

		return StringFix.createCleanUp(compilationUnit, 
				isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS), 
				isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(JavaScriptUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return StringFix.createCleanUp(compilationUnit, problems,
				isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS), 
				isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS));
	}

	public Map getRequiredOptions() {
		Map result= new Hashtable();
		
		if (isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS) || isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS))
			result.put(JavaScriptCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaScriptCore.WARNING);
		
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS))
			result.add(MultiFixMessages.StringMultiFix_AddMissingNonNls_description);
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS))
			result.add(MultiFixMessages.StringMultiFix_RemoveUnnecessaryNonNls_description);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS)) {
			buf.append("var s=\"\";"); //$NON-NLS-1$
		} else {
			buf.append("var s=\"\"; //$NON-NLS-1$"); //$NON-NLS-1$
		}
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 * @throws CoreException 
	 */
	public boolean canFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
		return StringFix.createFix(compilationUnit, problem, isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS), isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS)) != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(JavaScriptUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.ADD_MISSING_NLS_TAGS))
			result+= getNumberOfProblems(problems, IProblem.NonExternalizedStringLiteral);
		
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS))
			result+= getNumberOfProblems(problems, IProblem.UnnecessaryNLSTag);
		
		return result;
	}	
}
