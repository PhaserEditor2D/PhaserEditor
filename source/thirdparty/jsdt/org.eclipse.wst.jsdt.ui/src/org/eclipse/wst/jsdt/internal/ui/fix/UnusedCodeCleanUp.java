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
import org.eclipse.wst.jsdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can remove unused code
 * @see org.eclipse.wst.jsdt.internal.corext.fix.UnusedCodeFix
 *
 */
public class UnusedCodeCleanUp extends AbstractCleanUp {
		
	public UnusedCodeCleanUp(Map options) {
		super(options);
	}
	
	public UnusedCodeCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(IJavaScriptUnit unit) throws CoreException {
		boolean removeUnuseMembers= isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		
		return removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS) ||
//				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS) ||
				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS) ||
//				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES) || 
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES) ;
//				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS) && !isEnabled(CleanUpConstants.ORGANIZE_IMPORTS);
	}

	public IFix createFix(JavaScriptUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		boolean removeUnuseMembers= isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		
		return UnusedCodeFix.createCleanUp(compilationUnit,
				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS),
				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS),
				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS), 
				removeUnuseMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES), 
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES),
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS) && !isEnabled(CleanUpConstants.ORGANIZE_IMPORTS),
				false);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(JavaScriptUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		boolean removeMembers= isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		return UnusedCodeFix.createCleanUp(compilationUnit, problems,
				removeMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS), 
				removeMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS), 
				removeMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS),
				removeMembers && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES),
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES), 
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS) && !isEnabled(CleanUpConstants.ORGANIZE_IMPORTS),
				false);
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS) && !isEnabled(CleanUpConstants.ORGANIZE_IMPORTS))
			options.put(JavaScriptCore.COMPILER_PB_UNUSED_IMPORT, JavaScriptCore.WARNING);

		boolean removeMembers= isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		if (removeMembers && (
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS) ||
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS) || 
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS) ||
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES)))
			options.put(JavaScriptCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaScriptCore.WARNING);
		
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES))
			options.put(JavaScriptCore.COMPILER_PB_UNUSED_LOCAL, JavaScriptCore.WARNING);

		return options;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
//		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS))
//			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedImport_description);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedMethod_description);
//		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS))
//			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedConstructor_description);
//		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES))
//			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedType_description);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedField_description);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedVariable_description);
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
////		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS)) {
////		} else {
////			buf.append("import pack.Bar;\n"); //$NON-NLS-1$
////		}
////		buf.append("class Example {\n"); //$NON-NLS-1$
//		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES)) {
//		} else {
//			buf.append("    private class Sub {}\n"); //$NON-NLS-1$
//		}
//		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS)) {
//		} else {
//			buf.append("    function Example() {}\n"); //$NON-NLS-1$
//		}
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS)) {
		} else {
			buf.append("    var fField;\n"); //$NON-NLS-1$
		}
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS)) {
		} else {
			buf.append("    function foo() {}\n"); //$NON-NLS-1$
		}
		buf.append("    function bar() {\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES)) {
		} else {
			buf.append("        var i= 10;\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
//		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS)) {
			UnusedCodeFix fix= UnusedCodeFix.createRemoveUnusedImportFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS) ||
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS) ||
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES) ||
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS) ||
				isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES)) 
		{
			UnusedCodeFix fix= UnusedCodeFix.createUnusedMemberFix(compilationUnit, problem, false);
			if (fix != null)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(JavaScriptUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS) && !isEnabled(CleanUpConstants.ORGANIZE_IMPORTS)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
					    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound)
					result++;
			}
		}
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateMethod);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateConstructor);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateType);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS) && isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateField);
		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES))
			result+= getNumberOfProblems(problems, IProblem.LocalVariableIsNeverUsed);
		return result;
	}
}
