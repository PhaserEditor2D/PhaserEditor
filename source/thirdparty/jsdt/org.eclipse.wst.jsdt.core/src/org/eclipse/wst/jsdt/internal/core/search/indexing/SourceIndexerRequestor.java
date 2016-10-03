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
package org.eclipse.wst.jsdt.internal.core.search.indexing;

import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.core.search.processing.JobManager;

/**
 * This class is used by the JavaParserIndexer. When parsing the java file, the requestor
 * recognises the java elements (methods, fields, ...) and add them to an index.
 */
public class SourceIndexerRequestor implements ISourceElementRequestor, IIndexConstants {
	SourceIndexer indexer;

	char[] packageName = CharOperation.NO_CHAR;
	char[][] enclosingTypeNames = new char[5][];
	int depth = 0;
	int methodDepth = 0;

public SourceIndexerRequestor(SourceIndexer indexer) {
	this.indexer = indexer;
}
/**
 * @see ISourceElementRequestor#acceptConstructorReference(char[], int, int)
 */
public void acceptConstructorReference(char[] typeName, int argCount, int sourcePosition) {
	this.indexer.addConstructorReference(typeName, argCount);
	int lastDot = CharOperation.lastIndexOf('.', typeName);
	if (lastDot != -1) {
		char[][] qualification = CharOperation.splitOn('.', CharOperation.subarray(typeName, 0, lastDot));
		for (int i = 0, length = qualification.length; i < length; i++) {
			this.indexer.addNameReference(qualification[i]);
		}
	}
}
/**
 * @see ISourceElementRequestor#acceptFieldReference(char[], int)
 */
public void acceptFieldReference(char[] fieldName, int sourcePosition) {
	this.indexer.addFieldReference(fieldName);
}
/**
 * @see ISourceElementRequestor#acceptImport(int, int, char[][], boolean, int)
 */
public void acceptImport(int declarationStart, int declarationEnd, char[][] tokens, boolean onDemand) {
	// imports have already been reported while creating the ImportRef node (see SourceElementParser#comsume*ImportDeclarationName() methods)
}
/**
 * @see ISourceElementRequestor#acceptLineSeparatorPositions(int[])
 */
public void acceptLineSeparatorPositions(int[] positions) {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#acceptMethodReference(char[], int, int)
 */
public void acceptMethodReference(char[] methodName, int sourcePosition) {
	this.indexer.addMethodReference(methodName);
}
/**
 * @see ISourceElementRequestor#acceptPackage(int, int, char[])
 */
public void acceptPackage(int declarationStart, int declarationEnd, char[] name) {
	this.packageName = name;
}
/**
 * @see ISourceElementRequestor#acceptProblem(CategorizedProblem)
 */
public void acceptProblem(CategorizedProblem problem) {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#acceptTypeReference(char[][], int, int)
 */
public void acceptTypeReference(char[][] typeName, int sourceStart, int sourceEnd) {
	int length = typeName.length;
	for (int i = 0; i < length - 1; i++)
		acceptUnknownReference(typeName[i], 0); // ?
	acceptTypeReference(typeName[length - 1], 0);
}
/**
 * @see ISourceElementRequestor#acceptTypeReference(char[], int)
 */
public void acceptTypeReference(char[] simpleTypeName, int sourcePosition) {
	this.indexer.addTypeReference(simpleTypeName);
}
/**
 * @see ISourceElementRequestor#acceptUnknownReference(char[][], int, int)
 */
public void acceptUnknownReference(char[][] name, int sourceStart, int sourceEnd) {
	for (int i = 0; i < name.length; i++) {
		acceptUnknownReference(name[i], 0);
	}
}
/**
 * @see ISourceElementRequestor#acceptUnknownReference(char[], int)
 */
public void acceptUnknownReference(char[] name, int sourcePosition) {
	this.indexer.addNameReference(name);
}
/*
 * Rebuild the proper qualification for the current source type:
 *
 * java.lang.Object ---> null
 * java.util.Hashtable$Entry --> [Hashtable]
 * x.y.A$B$C --> [A, B]
 */
public char[][] enclosingTypeNames(){

	if (depth == 0) return null;

	char[][] qualification = new char[this.depth][];
	System.arraycopy(this.enclosingTypeNames, 0, qualification, 0, this.depth);
	return qualification;
}

private void enterClass(TypeInfo typeInfo) {
	/* do not enter the "fake" GLOBAL type, otherwise
	 * would have to build every file when doing global searches
	 */
	if(typeInfo.name == IIndexConstants.GLOBAL_SYMBOL) {
		return;
	}
	
	if(typeInfo.anonymousMember && !typeInfo.isIndexed) {
		this.pushTypeName(typeInfo.name);
		return;
	}
	// eliminate possible qualifications, given they need to be fully resolved again
	if (typeInfo.superclass != null) {
		//typeInfo.superclass = getSimpleName(typeInfo.superclass);

		// add implicit constructor reference to default constructor
		this.indexer.addConstructorReference(typeInfo.superclass, 0);
	}
	char[][] typeNames;
	if (this.methodDepth > 0) {
		// set specific ['0'] value for local and anonymous to be able to filter them
		typeNames = ONE_ZERO_CHAR;
	} else {
		typeNames = this.enclosingTypeNames();
	}
	char [] typeName=typeInfo.name;
	char [] pkgName=this.packageName;
	int index;
	if ( (index=CharOperation.lastIndexOf('.',typeName)) >0) {
		pkgName=CharOperation.subarray(typeName, 0, index);
		typeName=CharOperation.subarray(typeName, index+1, typeName.length);
	}
	this.indexer.addClassDeclaration(typeInfo.modifiers, pkgName, typeName, typeNames, typeInfo.superclass, typeInfo.secondary, typeInfo.synonyms);
	this.pushTypeName(typeInfo.name);
}
/**
 * @see ISourceElementRequestor#enterCompilationUnit()
 */
public void enterCompilationUnit() {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#enterConstructor(MethodInfo)
 */
public void enterConstructor(MethodInfo methodInfo) {
	this.indexer.addConstructorDeclaration(methodInfo.name, methodInfo.parameterTypes, methodInfo.parameterNames, methodInfo.modifiers);
	this.methodDepth++;
}
/**
 * @see ISourceElementRequestor#enterField(FieldInfo)
 */
public void enterField(FieldInfo fieldInfo) {
	boolean isVar=depth==0;
	this.indexer.addFieldDeclaration(fieldInfo.type, fieldInfo.name, fieldInfo.declaringType, fieldInfo.modifiers, 
				isVar);

	this.methodDepth++;
}
/**
 * @see ISourceElementRequestor#enterInitializer(int, int)
 */
public void enterInitializer(int declarationSourceStart, int modifiers) {
	this.methodDepth++;
}
/**
 * @see ISourceElementRequestor#enterMethod(MethodInfo)
 */
public void enterMethod(MethodInfo methodInfo) {
	boolean isFunction=this.depth==0;
	this.indexer.addMethodDeclaration(methodInfo.name, methodInfo.parameterTypes,
			methodInfo.parameterNames, methodInfo.returnType, methodInfo.declaringType,
			isFunction, methodInfo.modifiers);
	this.methodDepth++;
}
/**
 * @see ISourceElementRequestor#enterType(TypeInfo)
 */
public void enterType(TypeInfo typeInfo) {
	// TODO (jerome) might want to merge the 4 methods
	switch (TypeDeclaration.kind(typeInfo.modifiers)) {
		case TypeDeclaration.CLASS_DECL:
			enterClass(typeInfo);
			break;
	}
}

/**
 * @see ISourceElementRequestor#exitCompilationUnit(int)
 */
public void exitCompilationUnit(int declarationEnd) {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#exitConstructor(int)
 */
public void exitConstructor(int declarationEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitField(int, int, int)
 */
public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitInitializer(int)
 */
public void exitInitializer(int declarationEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitMethod(int, int, int)
 */
public void exitMethod(int declarationEnd, int defaultValueStart, int defaultValueEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitType(int)
 */
public void exitType(int declarationEnd) {
	popTypeName();
}
/*
 * Returns the unqualified name without parameters from the given type name.
 */
private char[] getSimpleName(char[] typeName) {
	int lastDot = -1, lastGenericStart = -1;
	int depthCount = 0;
	int length = typeName.length;
	lastDotLookup: for (int i = length -1; i >= 0; i--) {
		switch (typeName[i]) {
			case '.':
				if (depthCount == 0) {
					lastDot = i;
					break lastDotLookup;
				}
				break;
			case '<':
				depthCount--;
				if (depthCount == 0) lastGenericStart = i;
				break;
			case '>':
				depthCount++;
				break;
		}
	}
	if (lastGenericStart < 0) {
		if (lastDot < 0) {
			return typeName;
		}
		return  CharOperation.subarray(typeName, lastDot + 1, length);
	}
	return  CharOperation.subarray(typeName, lastDot + 1, lastGenericStart);
}
public void popTypeName() {
	if (depth > 0) {
		enclosingTypeNames[--depth] = null;
	} else if (JobManager.VERBOSE) {
		// dump a trace so it can be tracked down
		try {
			enclosingTypeNames[-1] = null;
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
}
public void pushTypeName(char[] typeName) {
	if (depth == enclosingTypeNames.length)
		System.arraycopy(enclosingTypeNames, 0, enclosingTypeNames = new char[depth*2][], 0, depth);
	enclosingTypeNames[depth++] = typeName;
}
}
