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
import org.eclipse.wst.jsdt.core.ast.IStatement;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public abstract class Statement extends ProgramElement implements IStatement {

	public abstract FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo);


	// Report an error if necessary
	public boolean complainIfUnreachable(FlowInfo flowInfo, BlockScope scope, boolean didAlreadyComplain) {

		if ((flowInfo.reachMode() & FlowInfo.UNREACHABLE) != 0) {
			this.bits &= ~ASTNode.IsReachable;
			boolean reported = flowInfo == FlowInfo.DEAD_END;
			if (!didAlreadyComplain && reported) {
				scope.problemReporter().unreachableCode(this);
			}
			return reported; // keep going for fake reachable
		}
		return false;
	}


	public boolean isEmptyBlock() {
		return false;
	}

	public boolean isValidJavaStatement() {
		//the use of this method should be avoid in most cases
		//and is here mostly for documentation purpose.....
		//while the parser is responsable for creating
		//welled formed expression statement, which results
		//in the fact that java-non-semantic-expression-used-as-statement
		//should not be parsable...thus not being built.
		//It sounds like the java grammar as help the compiler job in removing
		//-by construction- some statement that would have no effect....
		//(for example all expression that may do side-effects are valid statement
		// -this is an appromative idea.....-)

		return true;
	}

	public StringBuffer print(int indent, StringBuffer output) {
		return printStatement(indent, output);
	}
//	public abstract StringBuffer printStatement(int indent, StringBuffer output);

	public abstract void resolve(BlockScope scope);

	/**
	 * Returns case constant associated to this statement (NotAConstant if none)
	 */
	public Constant resolveCase(BlockScope scope, TypeBinding testType, SwitchStatement switchStatement) {
		// statement within a switch that are not case are treated as normal statement....

		resolve(scope);
		return Constant.NotAConstant;
	}
	public int getASTType() {
		return IASTNode.STATEMENT;
	
	}

}
