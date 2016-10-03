/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.parser;

/**
 * Internal statement structure for parsing recovery
 */
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;

public class RecoveredStatement extends RecoveredElement {

	public Statement statement;

	public RecoveredElement[] childStatements;
	public int childCount;

	public RecoveredStatement(Statement statement, RecoveredElement parent, int bracketBalance){
	super(parent, bracketBalance);
	this.statement = statement;

}
/*
 * Answer the associated parsed structure
 */
public ASTNode parseTree(){
	return statement;
}
/*
 * Answer the very source end of the corresponding parse node
 */
public int sourceEnd(){
	return this.statement.sourceEnd;
}
public String toString(int tab){
	return tabString(tab) + "Recovered statement:\n" + statement.print(tab + 1, new StringBuffer(10)); //$NON-NLS-1$
}
public Statement updatedStatement(){
	for (int i = 0; i < childCount ; i++) {
		childStatements[i].updatedASTNode();
	}
	return statement;
}
public void updateParseTree(){
	this.updatedStatement();
}
/*
 * Update the declarationSourceEnd of the corresponding parse node
 */
public void updateSourceEndIfNecessary(int bodyStart, int bodyEnd){
	if (this.statement.sourceEnd == 0)
		this.statement.sourceEnd = bodyEnd;
}
public ProgramElement updatedASTNode() {
	return updatedStatement();
}


public RecoveredElement add(AbstractMethodDeclaration methodDeclaration, int bracketBalanceValue) {


	RecoveredMethod element = new RecoveredMethod(methodDeclaration, this, bracketBalanceValue, this.recoveringParser);
	addChild(element);


	/* consider that if the opening brace was not found, it is there */
	if (!foundOpeningBrace){
		foundOpeningBrace = true;
		this.bracketBalance++;
	}
	/* if method not finished, then method becomes current */
	if (methodDeclaration.declarationSourceEnd == 0) return element;
	return this;
}

private void addChild(RecoveredElement statement)
{
	if (this.childStatements == null) {
		this.childStatements = new RecoveredElement[5];
		this.childCount = 0;
	} else {
		if (this.childCount == this.childStatements.length) {
			System.arraycopy(
				this.childStatements,
				0,
				(this.childStatements = new RecoveredElement[2 * this.childCount]),
				0,
				this.childCount);
		}
	}
	this.childStatements[this.childCount++] = statement;
}
}
