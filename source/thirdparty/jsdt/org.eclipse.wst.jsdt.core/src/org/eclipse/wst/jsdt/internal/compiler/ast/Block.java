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

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IBlock;
import org.eclipse.wst.jsdt.core.ast.IStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;

public class Block extends Statement implements IBlock {

	public Statement[] statements;
	public int explicitDeclarations;
	// the number of explicit declaration , used to create scope
	public BlockScope scope;

	public Block(int explicitDeclarations) {
		this.explicitDeclarations = explicitDeclarations;
	}
	
	public IStatement[] getStatements() {
		return statements;
	}
	
	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		// empty block
		if (statements == null)	return flowInfo;
		boolean didAlreadyComplain = false;
		for (int i = 0, max = statements.length; i < max; i++) {
			Statement stat = statements[i];
			if (!stat.complainIfUnreachable(flowInfo, scope, didAlreadyComplain)) {
				flowInfo = stat.analyseCode(scope, flowContext, flowInfo);
			} else {
				didAlreadyComplain = true;
			}
		}
		return flowInfo;
	}
	public boolean isEmptyBlock() {

		return statements == null;
	}

	public StringBuffer printBody(int indent, StringBuffer output) {

		if (this.statements == null) return output;
		for (int i = 0; i < statements.length; i++) {
			statements[i].printStatement(indent + 1, output);
			output.append('\n');
		}
		return output;
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		printIndent(indent, output);
		output.append("{\n"); //$NON-NLS-1$
		printBody(indent, output);
		return printIndent(indent, output).append('}');
	}

	public void resolve(BlockScope upperScope) {

		if ((this.bits & UndocumentedEmptyBlock) != 0) {
			upperScope.problemReporter().undocumentedEmptyBlock(this.sourceStart, this.sourceEnd);
		}
		if (statements != null) {
			scope =
				(!JavaScriptCore.IS_ECMASCRIPT4 || explicitDeclarations == 0)
					? upperScope
					: new BlockScope(upperScope, explicitDeclarations);
			for (int i = 0, length = statements.length; i < length; i++) {
				statements[i].resolve(scope);
			}
		}
	}

	public void resolveUsing(BlockScope givenScope) {

		if ((this.bits & UndocumentedEmptyBlock) != 0) {
			givenScope.problemReporter().undocumentedEmptyBlock(this.sourceStart, this.sourceEnd);
		}
		// this optimized resolve(...) is sent only on none empty blocks
		scope = givenScope;
		if (statements != null) {
			for (int i = 0, length = statements.length; i < length; i++) {
				statements[i].resolve(scope);
			}
		}
	}

	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		BlockScope visitScope=(scope!=null) ? scope:blockScope;
		if (visitor.visit(this, blockScope)) {
			if (statements != null) {
				for (int i = 0, length = statements.length; i < length; i++)
					statements[i].traverse(visitor, visitScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.BLOCK;
	
	}
}
