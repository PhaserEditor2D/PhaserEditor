/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ObjectGetterSetterField extends ObjectLiteralField implements IExpression {

	public Statement[] statements;
	public Javadoc javaDoc;
	public boolean isSetter;
	public Expression varName;

	public ObjectGetterSetterField(Expression field, Statement[] statements, Expression varName, int start, int end) {
		super(field, null, start, end);
		this.statements = statements;
		this.isSetter = varName != null;
		this.varName = varName;
	}
	
	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.javaDoc!=null) {
			this.javaDoc.print(indent, output);
		}
		printIndent(indent, output);
		if (this.isSetter) {
			output.append("set "); //$NON-NLS-1$
		} else {
			output.append("get "); //$NON-NLS-1$
		}
		this.fieldName.printExpression(indent, output);
		output.append("("); //$NON-NLS-1$
		if (this.isSetter) {
			this.varName.printExpression(indent, output);
		}
		output.append(") {\n"); //$NON-NLS-1$
		for (int i = 0, max = this.statements.length; i < max; i++) {
			printIndent(indent + 1, output);
			this.statements[i].printStatement(indent + 1, output);
			output.append("\n"); //$NON-NLS-1$
		}
		printIndent(indent + 1, output);
		output.append("}"); //$NON-NLS-1$
		return output;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (javaDoc!=null)
				javaDoc.traverse(visitor, scope);
			if (fieldName!=null)
				fieldName.traverse(visitor, scope);
			if (this.varName != null) {
				this.varName.traverse(visitor, scope);
			}
			for (int i = 0, max = this.statements.length; i < max; i++) {
				this.statements[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}


	public TypeBinding resolveType(BlockScope scope) {
		// TODO to be completed
		return null;
	}


	public FlowInfo analyseCode(
			BlockScope classScope,
			FlowContext initializationContext,
			FlowInfo flowInfo) {
		// TODO to be completed

		return flowInfo;
	}
	public int getASTType() {
		return IASTNode.OBJECT_GETTER_SETTER_FIELD;
	
	}

	public IJsDoc getJsDoc() {
		return this.javaDoc;
	}
}
