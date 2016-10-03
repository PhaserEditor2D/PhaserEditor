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
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.List;

import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class BodyDeclarationRewrite {

	private ASTNode fTypeNode;
	private ListRewrite fListRewrite;
	
	public static BodyDeclarationRewrite create(ASTRewrite rewrite, ASTNode typeNode) {
		return new BodyDeclarationRewrite(rewrite, typeNode);
	}
	
	private BodyDeclarationRewrite(ASTRewrite rewrite, ASTNode typeNode) {
		ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(typeNode);
		fTypeNode= typeNode;
		fListRewrite= rewrite.getListRewrite(typeNode, property);
	}
	
	public void insert(BodyDeclaration decl, TextEditGroup description) {
		List container= ASTNodes.getBodyDeclarations(fTypeNode);
		int index= ASTNodes.getInsertionIndex(decl, container);
		fListRewrite.insertAt(decl, index, description);
	}
}
