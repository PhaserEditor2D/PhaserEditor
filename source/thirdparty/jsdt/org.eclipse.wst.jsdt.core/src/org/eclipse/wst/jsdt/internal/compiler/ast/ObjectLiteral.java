/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;


public class ObjectLiteral extends Expression implements IObjectLiteral {

	public ObjectLiteralField [] fields;
	public InferredType inferredType;
	
	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (fields==null || fields.length==0)
		{
			output.append("{}"); //$NON-NLS-1$
		}
		else
		{
			output.append("{\n"); //$NON-NLS-1$
			printIndent(indent+1, output);
			for (int i = 0; i < fields.length; i++) {
				if (i>0)
				{
					output.append(",\n"); //$NON-NLS-1$
					printIndent(indent+1, output);
				}
				fields[i].printExpression(indent, output);
			}
			output.append("\n"); //$NON-NLS-1$
			printIndent(indent, output);
			output.append("}"); //$NON-NLS-1$
		}
		return output;
	}
	
	public InferredType getInferredType() {
		return this.inferredType;
	}
	
	public void setInferredType(InferredType type) {
		this.inferredType=type;
	}
	
	public IObjectLiteralField[] getFields() {
		return this.fields;
	}
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (fields!=null)
				for (int i = 0; i < fields.length; i++) {
					fields[i].traverse(visitor, scope);
				}
		}
		visitor.endVisit(this, scope);
	}


	public TypeBinding resolveType(BlockScope scope) {
		this.constant=Constant.NotAConstant;
		if (this.fields!=null) {
			for (int i = 0; i < this.fields.length; i++) {
				this.fields[i].resolveType(scope);
			}
		}
		
		if(inferredType != null) {
			//build the type if it is not yet built
			if(inferredType.binding == null) {
				inferredType.resolveType(scope, this);
			}
			
			if(inferredType.binding != null) {
				return inferredType.binding;
			}
		}

		return TypeBinding.ANY;
	}

	public int nullStatus(FlowInfo flowInfo) {
			return FlowInfo.NON_NULL; // constant expression cannot be null
	}

	public FlowInfo analyseCode(
			BlockScope classScope,
			FlowContext initializationContext,
			FlowInfo flowInfo) {
		if (this.fields!=null)
			for (int i = 0; i < this.fields.length; i++) {
				flowInfo=this.fields[i].analyseCode(classScope,initializationContext, flowInfo);
			}

		return flowInfo;
	}
	public int getASTType() {
		return IASTNode.OBJECT_LITERAL;
	
	}
}
