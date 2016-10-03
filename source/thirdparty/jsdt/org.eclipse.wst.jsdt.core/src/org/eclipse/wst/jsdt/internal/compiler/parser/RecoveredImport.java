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
 * Internal import structure for parsing recovery
 */
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;

public class RecoveredImport extends RecoveredElement {

	public ImportReference importReference;
public RecoveredImport(ImportReference importReference, RecoveredElement parent, int bracketBalance){
	super(parent, bracketBalance);
	this.importReference = importReference;
}
/*
 * Answer the associated parsed structure
 */
public ASTNode parseTree(){
	return importReference;
}
/*
 * Answer the very source end of the corresponding parse node
 */
public int sourceEnd(){
	return this.importReference.declarationSourceEnd;
}
public String toString(int tab) {
	return tabString(tab) + "Recovered import: " + importReference.toString(); //$NON-NLS-1$
}
public ImportReference updatedImportReference(){

	return importReference;
}
public void updateParseTree(){
	this.updatedImportReference();
}
/*
 * Update the declarationSourceEnd of the corresponding parse node
 */
public void updateSourceEndIfNecessary(int bodyStart, int bodyEnd){
	if (this.importReference.declarationSourceEnd == 0) {
		this.importReference.declarationSourceEnd = bodyEnd;
		this.importReference.declarationEnd = bodyEnd;
	}
}
public ProgramElement updatedASTNode() {
	// TODO Auto-generated method stub
	return null;
}
}
