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
 * Internal field structure for parsing recovery
 */
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;

public class RecoveredUnit extends RecoveredElement {

	public CompilationUnitDeclaration unitDeclaration;

//	public RecoveredImport[] imports;
//	public int importCount;
//	public RecoveredType[] types;
//	public int typeCount;

	public RecoveredElement[] statements;
	public int statementCount;

	public RecoveredUnit(CompilationUnitDeclaration unitDeclaration, int bracketBalance, Parser parser){
	super(null, bracketBalance, parser);
	this.unitDeclaration = unitDeclaration;
}


/*
 *	Record a method declaration: should be attached to last type
 */
public RecoveredElement add(AbstractMethodDeclaration methodDeclaration, int bracketBalanceValue) {


	RecoveredMethod element = new RecoveredMethod(methodDeclaration, this, bracketBalanceValue, this.recoveringParser);
	addStatement(element);


	/* consider that if the opening brace was not found, it is there */
	if (!foundOpeningBrace){
		foundOpeningBrace = true;
		this.bracketBalance++;
	}
	/* if method not finished, then method becomes current */
	if (methodDeclaration.declarationSourceEnd == 0) return element;
	return this;
}
/*
 *	Record a field declaration: should be attached to last type
 */
public RecoveredElement add(FieldDeclaration fieldDeclaration, int bracketBalanceValue) {

	RecoveredField element;
			element = new RecoveredField(fieldDeclaration, this, bracketBalanceValue);
	addStatement(element);

	/* consider that if the opening brace was not found, it is there */
	if (!foundOpeningBrace){
		foundOpeningBrace = true;
		this.bracketBalance++;
	}
	/* if field not finished, then field becomes current */
	if (fieldDeclaration.declarationSourceEnd == 0) return element;
	return this;
}

public RecoveredElement add(LocalDeclaration localDeclaration, int bracketBalanceValue) {
	RecoveredLocalVariable element;
	element = new RecoveredLocalVariable(localDeclaration, this, bracketBalanceValue);
addStatement(element);

/* consider that if the opening brace was not found, it is there */
if (!foundOpeningBrace){
foundOpeningBrace = true;
this.bracketBalance++;
}
/* if field not finished, then field becomes current */
if (localDeclaration.declarationSourceEnd == 0) return element;
return this;
}



public RecoveredElement add(ImportReference importReference, int bracketBalanceValue) {
//	if (this.imports == null) {
//		this.imports = new RecoveredImport[5];
//		this.importCount = 0;
//	} else {
//		if (this.importCount == this.imports.length) {
//			System.arraycopy(
//				this.imports,
//				0,
//				(this.imports = new RecoveredImport[2 * this.importCount]),
//				0,
//				this.importCount);
//		}
//	}
//	RecoveredImport element = new RecoveredImport(importReference, this, bracketBalanceValue);
//	this.imports[this.importCount++] = element;
//
//	/* if import not finished, then import becomes current */
//	if (importReference.declarationSourceEnd == 0) return element;
	return this;
}
public RecoveredElement add(TypeDeclaration typeDeclaration, int bracketBalanceValue) {

//	if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0){
//		if (this.typeCount > 0) {
//			// add it to the last type
//			RecoveredType lastType = this.types[this.typeCount-1];
//			lastType.bodyEnd = 0; // reopen type
//			lastType.typeDeclaration.bodyEnd = 0; // reopen type
//			lastType.typeDeclaration.declarationSourceEnd = 0; // reopen type
//			lastType.bracketBalance++; // expect one closing brace
//			return lastType.add(typeDeclaration, bracketBalanceValue);
//		}
//	}
//	if (this.types == null) {
//		this.types = new RecoveredType[5];
//		this.typeCount = 0;
//	} else {
//		if (this.typeCount == this.types.length) {
//			System.arraycopy(
//				this.types,
//				0,
//				(this.types = new RecoveredType[2 * this.typeCount]),
//				0,
//				this.typeCount);
//		}
//	}
//	RecoveredType element = new RecoveredType(typeDeclaration, this, bracketBalanceValue);
//	this.types[this.typeCount++] = element;
//
//	/* if type not finished, then type becomes current */
//	if (typeDeclaration.declarationSourceEnd == 0) return element;
	return this;
}

private void addStatement(RecoveredElement statement)
{
	if (this.statements == null) {
		this.statements = new RecoveredElement[5];
		this.statementCount = 0;
	} else {
		if (this.statementCount == this.statements.length) {
			System.arraycopy(
				this.statements,
				0,
				(this.statements = new RecoveredElement[2 * this.statementCount]),
				0,
				this.statementCount);
		}
	}
	this.statements[this.statementCount++] = statement;
}
/*
 * Answer the associated parsed structure
 */
public ASTNode parseTree(){
	return this.unitDeclaration;
}
/*
 * Answer the very source end of the corresponding parse node
 */
public int sourceEnd(){
	return this.unitDeclaration.sourceEnd;
}
public String toString(int tab) {
	StringBuffer result = new StringBuffer(tabString(tab));
	result.append("Recovered unit: [\n"); //$NON-NLS-1$
	this.unitDeclaration.print(tab + 1, result);
	result.append(tabString(tab + 1));
	result.append("]"); //$NON-NLS-1$
//	if (this.imports != null) {
//		for (int i = 0; i < this.importCount; i++) {
//			result.append("\n"); //$NON-NLS-1$
//			result.append(this.imports[i].toString(tab + 1));
//		}
//	}
	if (this.statements != null) {
		for (int i = 0; i < this.statementCount; i++) {
			result.append("\n"); //$NON-NLS-1$
			result.append(this.statements[i].toString(tab + 1));
		}
	}
	return result.toString();
}
public CompilationUnitDeclaration updatedCompilationUnitDeclaration(){

	/* update imports */
//	if (this.importCount > 0){
//		ImportReference[] importRefences = new ImportReference[this.importCount];
//		for (int i = 0; i < this.importCount; i++){
//			importRefences[i] = this.imports[i].updatedImportReference();
//		}
//		this.unitDeclaration.imports = importRefences;
//	}
	/* update types */
	int sourceEnd=(this.unitDeclaration.sourceEnd>0)?this.unitDeclaration.sourceEnd:this.parser().scanner.eofPosition;
	if (this.statementCount > 0){
		int existingCount = this.unitDeclaration.statements == null ? 0 : this.unitDeclaration.statements.length;
		ProgramElement[] stmts = new ProgramElement[existingCount + this.statementCount];
		if (existingCount > 0){
			System.arraycopy(this.unitDeclaration.statements, 0, stmts, 0, existingCount);
		}
		ASTNode astNode = this.statements[this.statementCount - 1].parseTree();
		// may need to update the declarationSourceEnd of the last type
		if (astNode.sourceEnd == 0){
			astNode.sourceEnd= sourceEnd;
			if (astNode instanceof Assignment)
			{
				Assignment assign=(Assignment)astNode;
				if (assign.expression instanceof FunctionExpression)
				{
					FunctionExpression functionExpression=(FunctionExpression)assign.expression;
					functionExpression.sourceEnd=astNode.sourceEnd;
					functionExpression.methodDeclaration.bodyEnd=
					functionExpression.methodDeclaration.sourceEnd=astNode.sourceEnd;
				}


			}

//			this.statements[this.statementCount - 1].updateSourceEndIfNecessary(sourceEnd)typeDeclaration.bodyEnd = this.unitDeclaration.sourceEnd;
		}
	    if (astNode instanceof AbstractMethodDeclaration && ((AbstractMethodDeclaration)astNode).bodyEnd<=0)
			 ((AbstractMethodDeclaration)astNode).bodyEnd=this.unitDeclaration.sourceEnd;
		int actualCount = existingCount;
		for (int i = 0; i < this.statementCount; i++){
			 ProgramElement updatedASTNode = this.statements[i].updatedASTNode();
			 if (updatedASTNode!=null && updatedASTNode.sourceEnd<=0 )
			 {
				 updatedASTNode.sourceEnd=this.unitDeclaration.sourceEnd;
			 }
			 if (updatedASTNode instanceof AbstractMethodDeclaration && ((AbstractMethodDeclaration)updatedASTNode).bodyEnd<=0 )
				 ((AbstractMethodDeclaration)updatedASTNode).bodyEnd=this.unitDeclaration.sourceEnd;
			 else if (updatedASTNode instanceof AbstractVariableDeclaration && ((AbstractVariableDeclaration)updatedASTNode).declarationSourceEnd<=0 )
				 ((AbstractVariableDeclaration)updatedASTNode).declarationSourceEnd=this.unitDeclaration.sourceEnd;

				//			  this.statements[i].updateParseTree();
			// filter out local types (12454)
//			if ((typeDecl.bits & ASTNode.IsLocalType) == 0){
				stmts[actualCount++] = updatedASTNode;
//			}
		}
//		if (actualCount != this.statementCount){
//			System.arraycopy(
//					stmts,
//				0,
//				stmts = new ProgramElement[existingCount+actualCount],
//				0,
//				existingCount+actualCount);
//		}
		this.unitDeclaration.statements = stmts;
	}
	else if (this.unitDeclaration.statements==null)
		this.unitDeclaration.statements=new ProgramElement[0];
	return this.unitDeclaration;
}
public RecoveredElement add(Block nestedBlockDeclaration, int bracketBalanceValue) {
	RecoveredBlock element = new RecoveredBlock(nestedBlockDeclaration, this, bracketBalanceValue);

	// if we have a pending Argument, promote it into the new block
//	if (this.pendingArgument != null){
//		element.attach(this.pendingArgument);
//		this.pendingArgument = null;
//	}
	if(this.parser().statementRecoveryActivated) {
		this.addBlockStatement(element);
	}
	addStatement(element);
	if (nestedBlockDeclaration.sourceEnd == 0) return element;
	return this;
}
public RecoveredElement add(Statement statement, int bracketBalanceValue) {
	RecoveredStatement element = new RecoveredStatement(statement, this, bracketBalanceValue);
	addStatement(element);
	if (statement.sourceEnd == 0) return element;
	return this;
}


public void updateParseTree(){
	this.updatedCompilationUnitDeclaration();
}
/*
 * Update the sourceEnd of the corresponding parse node
 */
public void updateSourceEndIfNecessary(int bodyStart, int bodyEnd){
	if (this.unitDeclaration.sourceEnd == 0)
		this.unitDeclaration.sourceEnd = bodyEnd;
}


public ProgramElement updatedASTNode() {
	//TODO: implement  SHOULD NOT GET HERE
	throw new org.eclipse.wst.jsdt.core.UnimplementedException();
}


public void updateFromParserState() {
		if (parser().astPtr>=0)
		{
		}
}
}
