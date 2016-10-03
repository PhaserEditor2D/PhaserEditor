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
import org.eclipse.wst.jsdt.core.ast.IIntLiteralMinValue;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.impl.IntConstant;

public class IntLiteralMinValue extends IntLiteral implements IIntLiteralMinValue {

	final static char[] CharValue = new char[]{'-','2','1','4','7','4','8','3','6','4','8'};
	final static Constant MIN_VALUE = IntConstant.fromValue(Integer.MIN_VALUE) ;

public IntLiteralMinValue() {
	super(CharValue,0,0,Integer.MIN_VALUE);
	constant = MIN_VALUE;
}
public void computeConstant(){

	/*precomputed at creation time*/ 
}
public int getASTType() {
	return IASTNode.INT_LITERAL_MIN_VALUE;

}
}
