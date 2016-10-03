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
import org.eclipse.wst.jsdt.core.ast.IInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;

public class Initializer extends FieldDeclaration implements IInitializer {

	public Block block;
	public int lastVisibleFieldID;
	public int bodyStart;
	public int bodyEnd;

	public boolean errorInSignature = false;

	public Initializer(Block block, int modifiers) {
		this.block = block;
		this.modifiers = modifiers;

		declarationSourceStart = sourceStart = block.sourceStart;
	}

	public FlowInfo analyseCode(
		MethodScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		return block.analyseCode(currentScope, flowContext, flowInfo);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	public int getKind() {
		return AbstractVariableDeclaration.INITIALIZER;
	}

	public boolean isStatic() {

		return (this.modifiers & ClassFileConstants.AccStatic) != 0;
	}

	public void parseStatements(
		Parser parser,
		TypeDeclaration typeDeclaration,
		CompilationUnitDeclaration unit) {

		//fill up the method body with statement
		parser.parse(this, typeDeclaration, unit);
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		if (modifiers != 0) {
			printIndent(indent, output);
			printModifiers(modifiers, output);
			output.append("{\n"); //$NON-NLS-1$
			block.printBody(indent, output);
			printIndent(indent, output).append('}');
			return output;
		} else {
			return block.printStatement(indent, output);
		}
	}

	public void resolve(MethodScope scope) {

	    FieldBinding previousField = scope.initializedField;
		int previousFieldID = scope.lastVisibleFieldID;
		try {
		    scope.initializedField = null;
			scope.lastVisibleFieldID = lastVisibleFieldID;
			block.resolve(scope);
		} finally {
		    scope.initializedField = previousField;
			scope.lastVisibleFieldID = previousFieldID;
		}
	}

	public void traverse(ASTVisitor visitor, MethodScope scope) {

		if (visitor.visit(this, scope)) {
			block.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.INITIALIZER;
	
	}
}
