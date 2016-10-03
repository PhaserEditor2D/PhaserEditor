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
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class LinkedFix extends AbstractFix {
	
	public static abstract class AbstractLinkedFixRewriteOperation extends AbstractFixRewriteOperation implements ILinkedFixRewriteOperation {

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			rewriteAST(cuRewrite, textEditGroups, new LinkedProposalModel());
		}

	}
	
	private final IFixRewriteOperation[] fFixRewrites;
	private final JavaScriptUnit fCompilationUnit;
	private final LinkedProposalModel fLinkedProposalModel;

	protected LinkedFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewrites) {
		super(name, compilationUnit, fixRewrites);
		fCompilationUnit= compilationUnit;
		fFixRewrites= fixRewrites;
		fLinkedProposalModel= new LinkedProposalModel();
	}
		
	public LinkedProposalModel getLinkedPositions() {
		return fLinkedProposalModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		if (fFixRewrites == null || fFixRewrites.length == 0)
			return null;

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite((IJavaScriptUnit)fCompilationUnit.getJavaElement(), fCompilationUnit);
	
		List/*<TextEditGroup>*/ groups= new ArrayList();

		fLinkedProposalModel.clear();
		
		for (int i= 0; i < fFixRewrites.length; i++) {
			IFixRewriteOperation operation= fFixRewrites[i];
			if (operation instanceof ILinkedFixRewriteOperation) {
				ILinkedFixRewriteOperation linkedOperation= (ILinkedFixRewriteOperation)operation;
				linkedOperation.rewriteAST(cuRewrite, groups, fLinkedProposalModel);
			} else {
				operation.rewriteAST(cuRewrite, groups);
			}
		}
		
		CompilationUnitChange result= cuRewrite.createChange(getDescription(), true, null);
		
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
		return result;
	}
}
