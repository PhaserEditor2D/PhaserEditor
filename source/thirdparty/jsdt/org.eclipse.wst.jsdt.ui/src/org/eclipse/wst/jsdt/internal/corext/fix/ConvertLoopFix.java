/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 *
 **/
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;

public class ConvertLoopFix extends LinkedFix {
	
	private final static class ControlStatementFinder extends GenericVisitor {
		
		//private final List/*<IFixRewriteOperation>*/fResult;
		private final Hashtable fUsedNames;
		private final boolean fFindForLoopsToConvert;
		private final boolean fConvertIterableForLoops;
		//private final boolean fMakeFinal;
		
		public ControlStatementFinder(boolean findForLoopsToConvert, boolean convertIterableForLoops, boolean makeFinal, List resultingCollection) {
			fFindForLoopsToConvert= findForLoopsToConvert;
			fConvertIterableForLoops= convertIterableForLoops;
			//fMakeFinal= makeFinal;
			//fResult= resultingCollection;
			fUsedNames= new Hashtable();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.wst.jsdt.core.dom.ForStatement)
		 */

		
		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ForStatement)
		 */
		public void endVisit(ForStatement node) {
			if (fFindForLoopsToConvert || fConvertIterableForLoops) {
				fUsedNames.remove(node);
			}
			super.endVisit(node);
		}
		
	}
	
	public static IFix createCleanUp(JavaScriptUnit compilationUnit, boolean convertForLoops, boolean convertIterableForLoops, boolean makeFinal) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaScriptProject()))
			return null;
		
		if (!convertForLoops && !convertIterableForLoops)
			return null;
		
		List operations= new ArrayList();
		ControlStatementFinder finder= new ControlStatementFinder(convertForLoops, convertIterableForLoops, makeFinal, operations);
		compilationUnit.accept(finder);
		
		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new ConvertLoopFix(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops);
	}
	

	public static IFix createConvertIterableLoopToEnhancedFix(JavaScriptUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(loop);
		IStatus status= loopConverter.satisfiesPreconditions();
		if (status.getSeverity() == IStatus.ERROR)
			return null;
		
		ConvertLoopFix result= new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
		result.setStatus(status);
		return result;
	}
	
	protected ConvertLoopFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
	
}
