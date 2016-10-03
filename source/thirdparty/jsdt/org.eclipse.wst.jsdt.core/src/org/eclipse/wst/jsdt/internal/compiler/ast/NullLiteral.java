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
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.INullLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class NullLiteral extends MagicLiteral implements INullLiteral {

	static final char[] source = {'n' , 'u' , 'l' , 'l'};

	public NullLiteral(int s , int e) {

		super(s,e);
	}

	public void computeConstant() {

		constant = Constant.NotAConstant;
	}

	public TypeBinding literalType(BlockScope scope) {
		return TypeBinding.NULL;
	}

	public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.NULL;
	}

	public Object reusableJSRTarget() {
		return TypeBinding.NULL;
	}

	public char[] source() {
		return source;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.NULL_LITERAL;
	
	}
}
