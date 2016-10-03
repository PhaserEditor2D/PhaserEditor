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
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.ExpressionsFix;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

public class ExpressionsCleanUp extends AbstractCleanUp {
		
	public ExpressionsCleanUp(Map options) {
		super(options);
	}
	
	public ExpressionsCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(IJavaScriptUnit unit) throws CoreException {
		boolean usePrentheses= isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		if (!usePrentheses)
			return false;
		
		return isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS) ||
		       isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);
	}
	
	public IFix createFix(JavaScriptUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		boolean usePrentheses= isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		if (!usePrentheses)
			return null;
		
		return ExpressionsFix.createCleanUp(compilationUnit, 
				isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS),
				isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(JavaScriptUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return createFix(compilationUnit);
	}

	public Map getRequiredOptions() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS)) 
			result.add(MultiFixMessages.ExpressionsCleanUp_addParanoiac_description);
		
		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER)) 
			result.add(MultiFixMessages.ExpressionsCleanUp_removeUnnecessary_description);
		
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS)) {
			buf.append("var b= (((i > 0) && (i < 10)) || (i == 50));\n"); //$NON-NLS-1$
		} else if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER)) {
			buf.append("var b= i > 0 && i < 10 || i == 50;\n"); //$NON-NLS-1$
		} else {
			buf.append("var b= (i > 0 && i < 10 || i == 50);\n"); //$NON-NLS-1$
		}
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS)) {
			IFix fix= ExpressionsFix.createAddParanoidalParenthesisFix(compilationUnit, new ASTNode[] {problem.getCoveredNode(compilationUnit)});
			if (fix != null)
				return true;
		}
		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER)) {
			IFix fix= ExpressionsFix.createRemoveUnnecessaryParenthesisFix(compilationUnit, new ASTNode[] {problem.getCoveredNode(compilationUnit)});
			if (fix != null)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(JavaScriptUnit compilationUnit) {
		return -1;
	}
}
