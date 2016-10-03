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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;

class NameCollector extends GenericVisitor {
	private List names= new ArrayList();
	private Selection fSelection;
	public NameCollector(ASTNode node) {
		fSelection= Selection.createFromStartLength(node.getStartPosition(), node.getLength());
	}
	protected boolean visitNode(ASTNode node) {
		if (node.getStartPosition() > fSelection.getInclusiveEnd())
			return true;
		if (fSelection.coveredBy(node))
			return true;
		return false;
	}
	public boolean visit(SimpleName node) {
		names.add(node.getIdentifier());
		return super.visit(node);
	}
	public boolean visit(VariableDeclarationStatement node) {
		return true;
	}
	public boolean visit(VariableDeclarationFragment node) {
		boolean result= super.visit(node);
		if (!result)
			names.add(node.getName().getIdentifier());
		return result;
	}
	public boolean visit(SingleVariableDeclaration node) {
		boolean result= super.visit(node);
		if (!result)
			names.add(node.getName().getIdentifier());
		return result;
	}
	public boolean visit(TypeDeclarationStatement node) {
		names.add(node.getDeclaration().getName().getIdentifier());
		return false;
	}

    List getNames() {
        return names;
    }
}
