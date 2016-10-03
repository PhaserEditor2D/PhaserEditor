/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.ITrueLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class TrueLiteral extends MagicLiteral implements ITrueLiteral {
	static final char[] source = {'t' , 'r' , 'u' , 'e'};
public TrueLiteral(int s , int e) {
	super(s,e);
}
public void computeConstant() {
	this.constant = BooleanConstant.fromValue(true);
}
public TypeBinding literalType(BlockScope scope) {
	return scope.getJavaLangBoolean();
}
/**
 *
 */
public char[] source() {
	return source;
}
public void traverse(ASTVisitor visitor, BlockScope scope) {
	visitor.visit(this, scope);
	visitor.endVisit(this, scope);
}
public int getASTType() {
	return IASTNode.TRUE_LITERAL;

}
}
