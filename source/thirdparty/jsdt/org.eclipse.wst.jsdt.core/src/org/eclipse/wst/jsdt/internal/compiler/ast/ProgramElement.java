/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;

public abstract class ProgramElement extends ASTNode implements IProgramElement {

	public abstract StringBuffer printStatement(int indent, StringBuffer output);

	public void resolve(BlockScope scope)
	{
		if (this instanceof AbstractMethodDeclaration)
			((AbstractMethodDeclaration)this).resolve((Scope)scope);
		else
			//TODO: implement
			throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}
	public int getASTType() {
		return IASTNode.PROGRAM_ELEMENT;
	
	}

}
