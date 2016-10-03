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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

public class LocalTypeAnalyzer extends ASTVisitor {

	private Selection fSelection;
	private List fTypeDeclarationsBefore= new ArrayList(2);
	private List fTypeDeclarationsSelected= new ArrayList(2);
	private String fBeforeTypeReferenced;
	private String fSelectedTypeReferenced;

	//---- Analyzing statements ----------------------------------------------------------------
	
	public static RefactoringStatus perform(BodyDeclaration declaration, Selection selection) {
		LocalTypeAnalyzer analyzer= new LocalTypeAnalyzer(selection);
		declaration.accept(analyzer);
		RefactoringStatus result= new RefactoringStatus();
		analyzer.check(result);
		return result;
	}

	private LocalTypeAnalyzer(Selection selection) {
		fSelection= selection;
	}

	public boolean visit(SimpleName node) {
		if (node.isDeclaration())
			return true;
		IBinding binding= node.resolveBinding();
		if (binding instanceof ITypeBinding)
			processLocalTypeBinding((ITypeBinding) binding, fSelection.getVisitSelectionMode(node));

		return true;
	}

	public boolean visit(TypeDeclaration node) {
		return visitType(node);
	}

	private boolean visitType(AbstractTypeDeclaration node) {
		int mode= fSelection.getVisitSelectionMode(node);
		switch (mode) {
			case Selection.BEFORE:
				fTypeDeclarationsBefore.add(node);
				break;
			case Selection.SELECTED:
				fTypeDeclarationsSelected.add(node);
				break;
		}
		return true;
	}

	private void processLocalTypeBinding(ITypeBinding binding, int mode) {
		switch (mode) {
			case Selection.SELECTED:
				if (fBeforeTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsBefore, binding))
					fBeforeTypeReferenced= RefactoringCoreMessages.LocalTypeAnalyzer_local_type_from_outside; 
				break;
			case Selection.AFTER:
				if (fSelectedTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsSelected, binding))
					fSelectedTypeReferenced= RefactoringCoreMessages.LocalTypeAnalyzer_local_type_referenced_outside; 
				break;
		}
	}
	
	private boolean checkBinding(List declarations, ITypeBinding binding) {
		for (Iterator iter= declarations.iterator(); iter.hasNext();) {
			AbstractTypeDeclaration declaration= (AbstractTypeDeclaration)iter.next();
			if (declaration.resolveBinding() == binding) {
				return true;
			}
		}
		return false;
	}
	
	private void check(RefactoringStatus status) {
		if (fBeforeTypeReferenced != null)
			status.addFatalError(fBeforeTypeReferenced);
		if (fSelectedTypeReferenced != null)
			status.addFatalError(fSelectedTypeReferenced);
	}
}
