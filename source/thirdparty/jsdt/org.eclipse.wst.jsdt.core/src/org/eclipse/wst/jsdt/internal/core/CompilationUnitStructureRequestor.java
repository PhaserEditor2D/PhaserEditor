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
package org.eclipse.wst.jsdt.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.core.util.ReferenceInfoAdapter;

/**
 * A requestor for the fuzzy parser, used to compute the children of an IJavaScriptUnit.
 */
public class CompilationUnitStructureRequestor extends ReferenceInfoAdapter implements ISourceElementRequestor {

	/**
	 * The handle to the compilation unit being parsed
	 */
	protected IJavaScriptElement unit;

	/**
	 * The info object for the compilation unit being parsed
	 */
	protected CompilationUnitElementInfo unitInfo;

	/**
	 * The import container info - null until created
	 */
	protected JavaElementInfo importContainerInfo = null;

	/**
	 * Hashtable of children elements of the compilation unit.
	 * Children are added to the table as they are found by
	 * the parser. Keys are handles, values are corresponding
	 * info objects.
	 */
	protected Map newElements;

	/**
	 * Stack of parent scope info objects. The info on the
	 * top of the stack is the parent of the next element found.
	 * For example, when we locate a method, the parent info object
	 * will be the type the method is contained in.
	 */
	protected Stack infoStack;

	/*
	 * Map from JavaElementInfo to of ArrayList of IJavaScriptElement representing the children
	 * of the given info.
	 */
	protected HashMap children;

	/**
	 * Stack of parent handles, corresponding to the info stack. We
	 * keep both, since info objects do not have back pointers to
	 * handles.
	 */
	protected Stack handleStack;

	/**
	 * The number of references reported thus far. Used to
	 * expand the arrays of reference kinds and names.
	 */
	protected int referenceCount= 0;

	/**
	 * Problem requestor which will get notified of discovered problems
	 */
	protected boolean hasSyntaxErrors = false;

	/*
	 * The parser this requestor is using.
	 */
	protected Parser parser;

	/**
	 * Empty collections used for efficient initialization
	 */
	protected static byte[] NO_BYTES= new byte[]{};

	protected HashtableOfObject fieldRefCache;
	protected HashtableOfObject messageRefCache;
	protected HashtableOfObject typeRefCache;
	protected HashtableOfObject unknownRefCache;

protected CompilationUnitStructureRequestor(IJavaScriptElement unit, CompilationUnitElementInfo unitInfo, Map newElements) {
	this.unit = unit;
	this.unitInfo = unitInfo;
	this.newElements = newElements;
}
/**
 * @see ISourceElementRequestor
 */
public void acceptImport(int declarationStart, int declarationEnd, char[][] tokens, boolean onDemand) {
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	if (!(parentHandle.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT)) {
		Assert.isTrue(false); // Should not happen
	}

	IJavaScriptUnit parentCU= (IJavaScriptUnit)parentHandle;
	//create the import container and its info
	ImportContainer importContainer= (ImportContainer)parentCU.getImportContainer();
	if (this.importContainerInfo == null) {
		this.importContainerInfo = new JavaElementInfo();
		JavaElementInfo parentInfo = (JavaElementInfo) this.infoStack.peek();
		addToChildren(parentInfo, importContainer);
		this.newElements.put(importContainer, this.importContainerInfo);
	}

	String elementName = JavaModelManager.getJavaModelManager().intern(new String(CharOperation.concatWith(tokens, '.')));
	ImportDeclaration handle = new ImportDeclaration(importContainer, elementName, onDemand);
	resolveDuplicates(handle);

	ImportDeclarationElementInfo info = new ImportDeclarationElementInfo();
	info.setSourceRangeStart(declarationStart);
	info.setSourceRangeEnd(declarationEnd);

	addToChildren(this.importContainerInfo, handle);
	this.newElements.put(handle, info);
}
/*
 * Table of line separator position. This table is passed once at the end
 * of the parse action, so as to allow computation of normalized ranges.
 *
 * A line separator might corresponds to several characters in the source,
 *
 */
public void acceptLineSeparatorPositions(int[] positions) {
	// ignore line separator positions
}
public void acceptProblem(CategorizedProblem problem) {
	if ((problem.getID() & IProblem.Syntax) != 0){
		this.hasSyntaxErrors = true;
	}
}
private void addToChildren(JavaElementInfo parentInfo, JavaElement handle) {
	ArrayList childrenList = (ArrayList) this.children.get(parentInfo);
	if (childrenList == null)
		this.children.put(parentInfo, childrenList = new ArrayList());
	childrenList.add(handle);
}
/**
 * Convert these type names to signatures.
 * @see Signature
 */
/* default */ static String[] convertTypeNamesToSigs(char[][] typeNames) {
	if (typeNames == null)
		return CharOperation.NO_STRINGS;
	int n = typeNames.length;
	if (n == 0)
		return CharOperation.NO_STRINGS;
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	String[] typeSigs = new String[n];
	for (int i = 0; i < n; ++i) {
		typeSigs[i] = manager.intern(Signature.createTypeSignature(typeNames[i], false));
	}
	return typeSigs;
}
/**
 * @see ISourceElementRequestor
 */
public void enterCompilationUnit() {
	this.infoStack = new Stack();
	this.children = new HashMap();
	this.handleStack= new Stack();
	this.infoStack.push(this.unitInfo);
	this.handleStack.push(this.unit);
}
/**
 * @see ISourceElementRequestor
 */
public void enterConstructor(MethodInfo methodInfo) {
	enterMethod(methodInfo);
}
/**
 * @see ISourceElementRequestor
 */
public void enterField(FieldInfo fieldInfo) {

	JavaElementInfo parentInfo = (JavaElementInfo) this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	SourceField handle = null;
	if (parentHandle.getElementType() == IJavaScriptElement.TYPE
			|| parentHandle.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT
			|| parentHandle.getElementType() == IJavaScriptElement.CLASS_FILE
			|| parentHandle.getElementType() == IJavaScriptElement.METHOD
			) {
		String fieldName = JavaModelManager.getJavaModelManager().intern(new String(fieldInfo.name));
		handle = new SourceField(parentHandle, fieldName);
	}
	else {
		Assert.isTrue(false); // Should not happen
	}
	resolveDuplicates(handle);

	SourceFieldElementInfo info = new SourceFieldElementInfo();
	info.setNameSourceStart(fieldInfo.nameSourceStart);
	info.setNameSourceEnd(fieldInfo.nameSourceEnd);
	info.setSourceRangeStart(fieldInfo.declarationStart);
	info.setFlags(fieldInfo.modifiers);
	if (fieldInfo.type!=null)
	{
	  char[] typeName = JavaModelManager.getJavaModelManager().intern(fieldInfo.type);
	  info.setTypeName(typeName);
	}

	addToChildren(parentInfo, handle);
	if (parentInfo instanceof CompilationUnitElementInfo) {
		CompilationUnitElementInfo compilationUnitInfo = (CompilationUnitElementInfo) parentInfo;
		compilationUnitInfo.addCategories(handle, fieldInfo.categories);
	}
	this.newElements.put(handle, info);

	this.infoStack.push(info);
	this.handleStack.push(handle);
}
/**
 * @see ISourceElementRequestor
 */
public void enterInitializer(
	int declarationSourceStart,
	int modifiers) {
		JavaElementInfo parentInfo = (JavaElementInfo) this.infoStack.peek();
		JavaElement parentHandle= (JavaElement) this.handleStack.peek();
		Initializer handle = null;

		if (parentHandle.getElementType() == IJavaScriptElement.TYPE) {
			handle = new Initializer(parentHandle, 1);
		}
		else {
			Assert.isTrue(false); // Should not happen
		}
		resolveDuplicates(handle);

		InitializerElementInfo info = new InitializerElementInfo();
		info.setSourceRangeStart(declarationSourceStart);
		info.setFlags(modifiers);

		addToChildren(parentInfo, handle);
		this.newElements.put(handle, info);

		this.infoStack.push(info);
		this.handleStack.push(handle);
}
/**
 * @see ISourceElementRequestor
 */
public void enterMethod(MethodInfo methodInfo) {

	JavaElementInfo parentInfo = (JavaElementInfo) this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	SourceMethod handle = null;

	// translate nulls to empty arrays
	if (methodInfo.parameterTypes == null) {
		methodInfo.parameterTypes= CharOperation.NO_CHAR_CHAR;
	}
	if (methodInfo.parameterNames == null) {
		methodInfo.parameterNames= CharOperation.NO_CHAR_CHAR;
	}

	String[] parameterTypeSigs = convertTypeNamesToSigs(methodInfo.parameterTypes);
	if (parentHandle.getElementType() == IJavaScriptElement.TYPE
			|| parentHandle.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT
			|| parentHandle.getElementType() == IJavaScriptElement.CLASS_FILE
			|| parentHandle.getElementType() == IJavaScriptElement.METHOD
			|| parentHandle.getElementType() == IJavaScriptElement.FIELD
			) {

		char[] cs = methodInfo.name!=null ? methodInfo.name: CharOperation.concat(IInferEngine.ANONYMOUS_PREFIX, IInferEngine.ANONYMOUS_CLASS_ID);

		String selector = JavaModelManager.getJavaModelManager().intern(new String(cs));
		handle = new SourceMethod(parentHandle, selector, parameterTypeSigs);
	}
	else {
		Assert.isTrue(false); // Should not happen
	}
	resolveDuplicates(handle);

	SourceMethodElementInfo info;
	if (methodInfo.isConstructor)
		info = new SourceConstructorInfo();
	else
		info = new SourceMethodInfo();
	info.setSourceRangeStart(methodInfo.declarationStart);
	int flags = methodInfo.modifiers;
	info.setNameSourceStart(methodInfo.nameSourceStart);
	info.setNameSourceEnd(methodInfo.nameSourceEnd);
	info.setFlags(flags);
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	char[][] parameterNames = methodInfo.parameterNames;
	for (int i = 0, length = parameterNames.length; i < length; i++)
		parameterNames[i] = manager.intern(parameterNames[i]);
	info.setArgumentNames(parameterNames);
	char[] returnType = methodInfo.returnType == null ? null : manager.intern(methodInfo.returnType);
	info.setReturnType(returnType);
	addToChildren(parentInfo, handle);
	if (parentInfo instanceof CompilationUnitElementInfo) {
		CompilationUnitElementInfo compilationUnitInfo = (CompilationUnitElementInfo) parentInfo;
		compilationUnitInfo.addCategories(handle, methodInfo.categories);
	}
	this.newElements.put(handle, info);
	this.infoStack.push(info);
	this.handleStack.push(handle);
}
/**
 * @see ISourceElementRequestor
 */
public void enterType(TypeInfo typeInfo) {

	JavaElementInfo parentInfo = (JavaElementInfo) this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	String nameString= new String(typeInfo.name);
	
	// if the type is a global type (isIndexed) then make sure its parent is a IJavaScriptUnit
	while(typeInfo.isIndexed && !(parentHandle instanceof IJavaScriptUnit)) {
		parentHandle = parentHandle.parent;
		
		// just in case something is messed up with the parenting, it will still get a value set
		if(parentHandle == null) {
			parentHandle= (JavaElement) this.handleStack.peek();
			break;
		}
	}

	SourceType handle = new SourceType(parentHandle, nameString, typeInfo.anonymousMember);  //NB: occurenceCount is computed in resolveDuplicates

	resolveDuplicates(handle);

	SourceTypeElementInfo info = 
			new SourceTypeElementInfo( parentHandle instanceof ClassFile , typeInfo.anonymousMember);
	info.setHandle(handle);
	info.setSourceRangeStart(typeInfo.declarationStart);
	info.setFlags(typeInfo.modifiers);
	info.setNameSourceStart(typeInfo.nameSourceStart);
	info.setNameSourceEnd(typeInfo.nameSourceEnd);
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	char[] superclass = typeInfo.superclass;
	info.setSuperclassName(superclass == null ? null : manager.intern(superclass));
	info.addCategories(handle, typeInfo.categories);
	if (parentHandle.getElementType() == IJavaScriptElement.TYPE)
		((SourceTypeElementInfo) parentInfo).addCategories(handle, typeInfo.categories);
	addToChildren(parentInfo, handle);
	this.newElements.put(handle, info);
	this.infoStack.push(info);
	this.handleStack.push(handle);
}
/**
 * @see ISourceElementRequestor
 */
public void exitCompilationUnit(int declarationEnd) {
	// set import container children
	if (this.importContainerInfo != null) {
		setChildren(this.importContainerInfo);
	}

	// set children
	setChildren(this.unitInfo);

	this.unitInfo.setSourceLength(declarationEnd + 1);

	// determine if there were any parsing errors
	this.unitInfo.setIsStructureKnown(!this.hasSyntaxErrors);
}
/**
 * @see ISourceElementRequestor
 */
public void exitConstructor(int declarationEnd) {
	exitMember(declarationEnd);
}
/**
 * @see ISourceElementRequestor
 */
public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
	SourceFieldElementInfo info = (SourceFieldElementInfo) this.infoStack.pop();
	info.setSourceRangeEnd(declarationSourceEnd);
	setChildren(info);

	this.handleStack.pop();
}
/**
 * @see ISourceElementRequestor
 */
public void exitInitializer(int declarationEnd) {
	exitMember(declarationEnd);
}
/**
 * common processing for classes and interfaces
 */
protected void exitMember(int declarationEnd) {
	SourceRefElementInfo info = (SourceRefElementInfo) this.infoStack.pop();
	info.setSourceRangeEnd(declarationEnd);
	setChildren(info);
	this.handleStack.pop();
}
/**
 * @see ISourceElementRequestor
 */
public void exitMethod(int declarationEnd, int defaultValueStart, int defaultValueEnd) {
	SourceMethodElementInfo info = (SourceMethodElementInfo) this.infoStack.pop();
	info.setSourceRangeEnd(declarationEnd);
	setChildren(info);
	this.handleStack.pop();
}
/**
 * @see ISourceElementRequestor
 */
public void exitType(int declarationEnd) {

	exitMember(declarationEnd);
}
/**
 * Resolves duplicate handles by incrementing the occurrence count
 * of the handle being created until there is no conflict.
 */
protected void resolveDuplicates(SourceRefElement handle) {
	while (this.newElements.containsKey(handle)) {
		handle.occurrenceCount++;
	}
}
private void setChildren(JavaElementInfo info) {
	ArrayList childrenList = (ArrayList) this.children.get(info);
	if (childrenList != null) {
		int length = childrenList.size();
		IJavaScriptElement[] elements = new IJavaScriptElement[length];
		childrenList.toArray(elements);
		info.children = elements;
	}
}
}
