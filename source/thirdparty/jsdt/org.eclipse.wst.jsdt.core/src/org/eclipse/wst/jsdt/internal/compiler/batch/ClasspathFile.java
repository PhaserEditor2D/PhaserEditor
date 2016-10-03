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
package org.eclipse.wst.jsdt.internal.compiler.batch;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.eclipse.wst.jsdt.core.UnimplementedException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;

public class ClasspathFile extends ClasspathLocation {

protected File file;
protected Hashtable packageCache;
String packageName;
protected char[] normalizedPath;
String encoding;
HashtableOfObject definedItems[] = new HashtableOfObject[Binding.NUMBER_BASIC_BINDING];

NameEnvironmentAnswer foundAnswer;

public ClasspathFile(File file, String encoding,
		AccessRuleSet accessRuleSet, String destinationPath) {
	super(accessRuleSet,destinationPath);
	this.file = file;
	this.packageName=file.getName();
	this.encoding=encoding;
}

public NameEnvironmentAnswer findBinding(char[] typeName, String qualifiedPackageName, int type, ITypeRequestor requestor) {
//	if (!qualifiedPackageName.equals(this.packageName))
	//	return null;
	if (foundAnswer==null)
	{
		parseFile(requestor);
	}
	NameEnvironmentAnswer answer=null;
	if ((type&(Binding.VARIABLE|Binding.FIELD))!=0)
	{
		answer= (NameEnvironmentAnswer)definedItems[Binding.VARIABLE|Binding.FIELD].get(typeName);
		if (answer!=null)
			return answer;
	}
	if ((type&Binding.TYPE)!=0)
	{
		answer= (NameEnvironmentAnswer)definedItems[Binding.TYPE].get(typeName);
		if (answer!=null)
			return answer;
	}
	if ((type&Binding.METHOD)!=0)
	{
		answer= (NameEnvironmentAnswer)definedItems[Binding.METHOD].get(typeName);
		if (answer!=null)
			return answer;
	}
	return null;
	}

private void parseFile(ITypeRequestor requestor) {
	CompilationUnit compilationUnit = new CompilationUnit(null,
			file.getAbsolutePath(), this.encoding);
	compilationUnit.packageName=new char [][]{packageName.toCharArray()};
	for (int i = 0; i < definedItems.length; i++) {
		definedItems[i]=new HashtableOfObject();
	}


		foundAnswer =
		 new NameEnvironmentAnswer(compilationUnit,
			fetchAccessRestriction(file.getAbsolutePath()));

		if (requestor!=null)
	{
		CompilationUnitDeclaration declaration = requestor.doParse(compilationUnit,null);
		for (int i = 0; i < declaration.statements.length; i++) {
			if (declaration.statements[i] instanceof AbstractMethodDeclaration) {
				AbstractMethodDeclaration method = (AbstractMethodDeclaration) declaration.statements[i];
				definedItems[Binding.METHOD].put(method.getName(), foundAnswer);
			}
			else if (declaration.statements[i] instanceof AbstractVariableDeclaration) {
				AbstractVariableDeclaration var = (AbstractVariableDeclaration) declaration.statements[i];
				definedItems[Binding.VARIABLE].put(var.name, foundAnswer);

			}
		}
		for (int inx=0;inx<declaration.numberInferredTypes;inx++) {
			InferredType inferredType = declaration.inferredTypes[inx];
			if (inferredType.isDefinition())
				definedItems[Binding.TYPE].put(inferredType.getName(), foundAnswer);
		}

	}
	else
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();

}

public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String qualifiedBinaryFileName) {
	return findClass(typeName,qualifiedPackageName,qualifiedBinaryFileName,false);
}
public NameEnvironmentAnswer findClass(char[] typeName,
			String qualifiedPackageName, String qualifiedBinaryFileName,
			boolean asBinaryOnly) {
	if (!isPackage(qualifiedPackageName))
		return null; // most common case
	throw new org.eclipse.wst.jsdt.core.UnimplementedException();

//	return null;
}
public void initialize() throws IOException {
}
public boolean isPackage(String qualifiedPackageName) {
		return packageName.equals(qualifiedPackageName);
}
public void reset() {

	this.packageCache = null;
}
public String toString() {
	return "Classpath for file " + this.file.getPath(); //$NON-NLS-1$
}
public char[] normalizedPath() {
	if (this.normalizedPath == null) {
		char[] rawName = this.file.getPath().toCharArray();
		if (File.separatorChar == '\\') {
			CharOperation.replace(rawName, '\\', '/');
		}
		this.normalizedPath = CharOperation.subarray(rawName, 0, CharOperation.lastIndexOf('.', rawName));
	}
	return this.normalizedPath;
}
public String getPath(){
	return this.file.getPath();
}

public char[][][] findTypeNames(String qualifiedPackageName) {
	throw new UnimplementedException("implement"); //$NON-NLS-1$
}
}
