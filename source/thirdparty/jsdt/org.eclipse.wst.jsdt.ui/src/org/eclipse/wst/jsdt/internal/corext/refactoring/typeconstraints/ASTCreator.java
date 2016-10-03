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
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;


public class ASTCreator {

	public static final String CU_PROPERTY= "org.eclipse.wst.jsdt.ui.refactoring.cu"; //$NON-NLS-1$

	private ASTCreator() {
		//private
	}
	
	public static JavaScriptUnit createAST(IJavaScriptUnit cu, WorkingCopyOwner workingCopyOwner) {
		JavaScriptUnit cuNode= getCuNode(workingCopyOwner, cu);
		cuNode.setProperty(CU_PROPERTY, cu);
		return cuNode;
	}

	private static JavaScriptUnit getCuNode(WorkingCopyOwner workingCopyOwner, IJavaScriptUnit cu) {
		ASTParser p = ASTParser.newParser(AST.JLS3);
		p.setSource(cu);
		p.setResolveBindings(true);
		p.setWorkingCopyOwner(workingCopyOwner);
		p.setCompilerOptions(RefactoringASTParser.getCompilerOptions(cu));
		return (JavaScriptUnit) p.createAST(null);
	}

	public static IJavaScriptUnit getCu(ASTNode node) {
		Object property= node.getRoot().getProperty(CU_PROPERTY);
		if (property instanceof IJavaScriptUnit)
			return (IJavaScriptUnit)property;
		return null;
	}
}
