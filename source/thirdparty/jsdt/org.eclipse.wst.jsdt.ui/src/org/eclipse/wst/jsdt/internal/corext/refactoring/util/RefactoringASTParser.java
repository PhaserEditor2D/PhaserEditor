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
package org.eclipse.wst.jsdt.internal.corext.refactoring.util;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;

public class RefactoringASTParser {

	private ASTParser fParser;
	
	public RefactoringASTParser(int level) {
		fParser= ASTParser.newParser(level);
	}
	
	public JavaScriptUnit parse(ITypeRoot typeRoot, boolean resolveBindings) {
		return parse(typeRoot, resolveBindings, null);
	}

	public JavaScriptUnit parse(ITypeRoot typeRoot, boolean resolveBindings, IProgressMonitor pm) {
		return parse(typeRoot, null, resolveBindings, pm);
	}

	public JavaScriptUnit parse(ITypeRoot typeRoot, WorkingCopyOwner owner, boolean resolveBindings, IProgressMonitor pm) {
		return parse(typeRoot, owner, resolveBindings, false, false, pm);
	}

	public JavaScriptUnit parse(ITypeRoot typeRoot, WorkingCopyOwner owner, boolean resolveBindings, boolean statementsRecovery, boolean bindingsRecovery, IProgressMonitor pm) {
		fParser.setResolveBindings(resolveBindings);
		fParser.setStatementsRecovery(statementsRecovery);
		fParser.setBindingsRecovery(bindingsRecovery);
		fParser.setSource(typeRoot);
		if (owner != null)
			fParser.setWorkingCopyOwner(owner);
		fParser.setCompilerOptions(getCompilerOptions(typeRoot));
		JavaScriptUnit result= (JavaScriptUnit) fParser.createAST(pm);
		return result;
	}

	/**
	 * @param newCuSource the source
	 * @param originalCu the compilation unit to get the name and project from
	 * @param resolveBindings whether bindings are to be resolved
	 * @param statementsRecovery whether statements recovery should be enabled
	 * @param pm an {@link IProgressMonitor}, or <code>null</code>
	 * @return the parsed JavaScriptUnit
	 */
	public JavaScriptUnit parse(String newCuSource, IJavaScriptUnit originalCu, boolean resolveBindings, boolean statementsRecovery, IProgressMonitor pm) {
		fParser.setResolveBindings(resolveBindings);
		fParser.setStatementsRecovery(statementsRecovery);
		fParser.setSource(newCuSource.toCharArray());
		fParser.setUnitName(originalCu.getElementName());
		fParser.setProject(originalCu.getJavaScriptProject());
		fParser.setCompilerOptions(getCompilerOptions(originalCu));
		JavaScriptUnit newCUNode= (JavaScriptUnit) fParser.createAST(pm);
		return newCUNode;
	}
	
	/**
	 * @param newCfSource the source
	 * @param originalCf the class file to get the name and project from
	 * @param resolveBindings whether bindings are to be resolved
	 * @param statementsRecovery whether statements recovery should be enabled
	 * @param pm an {@link IProgressMonitor}, or <code>null</code>
	 * @return the parsed JavaScriptUnit
	 */
	public JavaScriptUnit parse(String newCfSource, IClassFile originalCf, boolean resolveBindings, boolean statementsRecovery, IProgressMonitor pm) {
		fParser.setResolveBindings(resolveBindings);
		fParser.setStatementsRecovery(statementsRecovery);
		fParser.setSource(newCfSource.toCharArray());
		String cfName= originalCf.getElementName();
		fParser.setUnitName(cfName.substring(0, cfName.length() - 6) + JavaModelUtil.DEFAULT_CU_SUFFIX);
		fParser.setProject(originalCf.getJavaScriptProject());
		fParser.setCompilerOptions(getCompilerOptions(originalCf));
		JavaScriptUnit newCUNode= (JavaScriptUnit) fParser.createAST(pm);
		return newCUNode;
	}
	
	/**
	 * Tries to get the shared AST from the ASTProvider.
	 * If the shared AST is not available, parses the type root with a
	 * RefactoringASTParser that uses settings similar to the ASTProvider.
	 * 
	 * @param typeRoot the type root
	 * @param resolveBindings TODO
	 * @param pm an {@link IProgressMonitor}, or <code>null</code>
	 * @return the parsed JavaScriptUnit
	 */
	public static JavaScriptUnit parseWithASTProvider(ITypeRoot typeRoot, boolean resolveBindings, IProgressMonitor pm) {
		JavaScriptUnit cuNode= ASTProvider.getASTProvider().getAST(typeRoot, ASTProvider.WAIT_ACTIVE_ONLY, pm);
		if (cuNode != null) {
			return cuNode;
		} else {
			return new RefactoringASTParser(ASTProvider.SHARED_AST_LEVEL).parse(typeRoot, null, resolveBindings, ASTProvider.SHARED_AST_STATEMENT_RECOVERY, ASTProvider.SHARED_BINDING_RECOVERY, pm);
		}
	}

	public static IJavaScriptUnit getCompilationUnit(ASTNode node) {
		ASTNode root= node.getRoot();
		if (root instanceof JavaScriptUnit) {
			IJavaScriptElement cu= ((JavaScriptUnit) root).getJavaElement();
			if (cu instanceof IJavaScriptUnit)
				return (IJavaScriptUnit) cu;
		}
		return null;
	}
	
	public static Map getCompilerOptions(IJavaScriptElement element) {
		IJavaScriptProject project= element.getJavaScriptProject();
		Map options= project.getOptions(true);
		// turn all errors and warnings into ignore. The customizable set of compiler
		// options only contains additional Eclipse options. The standard JDK compiler
		// options can't be changed anyway.
		for (Iterator iter= options.keySet().iterator(); iter.hasNext();) {
			String key= (String)iter.next();
			String value= (String)options.get(key);
			if ("error".equals(value) || "warning".equals(value)) {  //$NON-NLS-1$//$NON-NLS-2$
				// System.out.println("Ignoring - " + key);
				options.put(key, "ignore"); //$NON-NLS-1$
			}
		}
		options.put(JavaScriptCore.COMPILER_TASK_TAGS, ""); //$NON-NLS-1$		
		return options;
	}
}
