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
package org.eclipse.wst.jsdt.internal.corext.refactoring.surround;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

public class SurroundWithTryCatchAnalyzer extends SurroundWithAnalyzer {


	private ISurroundWithTryCatchQuery fQuery;
	private ITypeBinding[] fExceptions;

	public SurroundWithTryCatchAnalyzer(IJavaScriptUnit unit, Selection selection, ISurroundWithTryCatchQuery query) throws JavaScriptModelException {
		super(unit, selection);
		fQuery= query;
	}
	
	public ITypeBinding[] getExceptions() {
		return fExceptions;
	}

	public void endVisit(JavaScriptUnit node) {
		BodyDeclaration enclosingNode= null;
		if (!getStatus().hasFatalError() && hasSelectedNodes())
			enclosingNode= (BodyDeclaration)ASTNodes.getParent(getFirstSelectedNode(), BodyDeclaration.class);

		super.endVisit(node);
		if (enclosingNode != null && !getStatus().hasFatalError()) {
			fExceptions= ExceptionAnalyzer.perform(enclosingNode, getSelection());
			if (fExceptions == null || fExceptions.length == 0) {
				if (fQuery == null) {
					invalidSelection(RefactoringCoreMessages.SurroundWithTryCatchAnalyzer_noUncaughtExceptions); 
				} else if (fQuery.catchRuntimeException()) {
					fExceptions= new ITypeBinding[] {node.getAST().resolveWellKnownType("java.lang.RuntimeException")}; //$NON-NLS-1$
				}
			}
		}
	}
}
