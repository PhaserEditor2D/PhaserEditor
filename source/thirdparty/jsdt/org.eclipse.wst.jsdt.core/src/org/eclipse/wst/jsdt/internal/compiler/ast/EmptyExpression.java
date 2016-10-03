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
import org.eclipse.wst.jsdt.core.ast.IEmptyExpression;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class EmptyExpression extends Expression implements IEmptyExpression {

	
	
	public EmptyExpression(int startPosition, int endPosition) {
		this.sourceStart = startPosition;
		this.sourceEnd = endPosition;
	}

	
	public StringBuffer printExpression(int indent, StringBuffer output) {
		return output;
	}

	public void resolve(BlockScope scope) {
	}
	
	public TypeBinding resolveType(BlockScope scope) {
		return TypeBinding.ANY;
	}
	public int getASTType() {
		return IASTNode.EMPTY_EXPRESSION;
	
	}
}
