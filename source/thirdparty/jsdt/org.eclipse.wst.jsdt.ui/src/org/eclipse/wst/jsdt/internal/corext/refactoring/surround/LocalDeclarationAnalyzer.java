/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;


public class LocalDeclarationAnalyzer extends ASTVisitor {

	private Selection fSelection;
	private List fAffectedLocals;

	public static VariableDeclaration[] perform(BodyDeclaration parent, Selection selection) {
		LocalDeclarationAnalyzer analyzer= new LocalDeclarationAnalyzer(selection);
		parent.accept(analyzer);
		return (VariableDeclaration[]) analyzer.fAffectedLocals.toArray(new VariableDeclaration[analyzer.fAffectedLocals.size()]);
	}

	private LocalDeclarationAnalyzer(Selection selection) {
		fSelection= selection;
		fAffectedLocals= new ArrayList(1);
	}
	
	public boolean visit(SimpleName node) {
		IVariableBinding binding= null; 
		if (node.isDeclaration() || !considerNode(node) || (binding= ASTNodes.getLocalVariableBinding(node)) == null)
			return false;
		handleReferenceToLocal(node, binding);
		return true;
	}	
	
	private boolean considerNode(ASTNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.AFTER;
	}
	
	private void handleReferenceToLocal(SimpleName node, IVariableBinding binding) {
		VariableDeclaration declaration= ASTNodes.findVariableDeclaration(binding, node);
		if (declaration != null && fSelection.covers(declaration))
			addLocalDeclaration(declaration);
	}
	
	private void addLocalDeclaration(VariableDeclaration declaration) {
		if (!fAffectedLocals.contains(declaration))
			fAffectedLocals.add(declaration);
	}
}
