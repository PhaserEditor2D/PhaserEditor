/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IArrayTypeReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ArrayTypeReference extends SingleTypeReference implements IArrayTypeReference {
	public int dimensions;
	public int originalSourceEnd;

	/**
	 * ArrayTypeReference constructor comment.
	 * @param source char[]
	 * @param dimensions int
	 * @param pos int
	 */
	public ArrayTypeReference(char[] source, int dimensions, long pos) {

		super(source, pos);
		this.originalSourceEnd = this.sourceEnd;
		this.dimensions = dimensions ;
	}

	public int dimensions() {

		return dimensions;
	}
	/**
	 * @return char[][]
	 */
	public char [][] getParameterizedTypeName(){
		int dim = this.dimensions;
		char[] dimChars = new char[dim*2];
		for (int i = 0; i < dim; i++) {
			int index = i*2;
			dimChars[index] = '[';
			dimChars[index+1] = ']';
		}
		return new char[][]{ CharOperation.concat(token, dimChars) };
	}
	protected TypeBinding getTypeBinding(Scope scope) {

		if (this.resolvedType != null) return this.resolvedType;
		TypeBinding leafComponentType = scope.getType(token);
		return scope.createArrayType(leafComponentType, dimensions);

	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		super.printExpression(indent, output);
		if ((this.bits & IsVarArgs) != 0) {
			for (int i= 0 ; i < dimensions - 1; i++) {
				output.append("[]"); //$NON-NLS-1$
			}
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i= 0 ; i < dimensions; i++) {
				output.append("[]"); //$NON-NLS-1$
			}
		}
		return output;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {

		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {

		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.ARRAY_TYPE_REFERENCE;
	
	}
}
