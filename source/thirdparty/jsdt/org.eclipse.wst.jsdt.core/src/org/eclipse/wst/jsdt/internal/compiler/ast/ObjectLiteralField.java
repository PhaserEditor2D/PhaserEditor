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
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IJsDoc;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ObjectLiteralField extends Expression implements IObjectLiteralField {

	public Expression fieldName;
	public Expression initializer;
	public Javadoc  javaDoc;

	public ObjectLiteralField(Expression field, Expression value, int start, int end) {

		this.fieldName=field;
		this.initializer=value;
		this.sourceEnd=end;
		this.sourceStart=start;
	}
	
	public IExpression getFieldName() {
		return fieldName;
	}
	
	public IExpression getInitializer() {
		return initializer;
	}

	
	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.javaDoc!=null)
			this.javaDoc.print(indent, output);
		fieldName.printExpression(indent, output);
		output.append(" : "); //$NON-NLS-1$
		initializer.printExpression(indent, output) ;
		return output;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {

			if (javaDoc!=null)
				javaDoc.traverse(visitor, scope);
			if (fieldName!=null)
				fieldName.traverse(visitor, scope);
			if (initializer!=null)
				initializer.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}


	public TypeBinding resolveType(BlockScope scope) {
		return initializer.resolveType(scope);
	}


	public FlowInfo analyseCode(
			BlockScope classScope,
			FlowContext initializationContext,
			FlowInfo flowInfo) {
			flowInfo=initializer.analyseCode(classScope,initializationContext, flowInfo);

		return flowInfo;
	}
	public int getASTType() {
		return IASTNode.OBJECT_LITERAL_FIELD;
	
	}

	public IJsDoc getJsDoc() {
		return this.javaDoc;
	}
}
