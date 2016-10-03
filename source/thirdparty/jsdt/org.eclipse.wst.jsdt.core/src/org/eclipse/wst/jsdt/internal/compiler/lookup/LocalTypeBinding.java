/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;

public final class LocalTypeBinding extends NestedTypeBinding {
	final static char[] LocalTypePrefix = { '$', 'L', 'o', 'c', 'a', 'l', '$' };

	private InnerEmulationDependency[] dependents;
	public ArrayBinding[] localArrayBindings; // used to cache array bindings of various dimensions for this local type
	public CaseStatement enclosingCase; // from 1.4 on, local types should not be accessed across switch case blocks (52221)
	public int sourceStart; // used by computeUniqueKey to uniquely identify this binding
	public MethodBinding enclosingMethod;

public LocalTypeBinding(ClassScope scope, SourceTypeBinding enclosingType, CaseStatement switchCase) {
	super(
		new char[][] {CharOperation.concat(LocalTypePrefix, scope.referenceContext.name)},
		scope,
		enclosingType);

	if (this.sourceName == CharOperation.NO_CHAR)
		this.tagBits |= TagBits.AnonymousTypeMask;
	else
		this.tagBits |= TagBits.LocalTypeMask;
	this.enclosingCase = switchCase;
	this.sourceStart = scope.referenceContext.sourceStart;
	MethodScope methodScope = scope.enclosingMethodScope();
	AbstractMethodDeclaration declaration = methodScope.referenceMethod();
	if (declaration != null) {
		this.enclosingMethod = declaration.getBinding();
	}
}
/* Record a dependency onto a source target type which may be altered
* by the end of the innerclass emulation. Later on, we will revisit
* all its dependents so as to update them (see updateInnerEmulationDependents()).
*/

public void addInnerEmulationDependent(BlockScope dependentScope, boolean wasEnclosingInstanceSupplied) {
	int index;
	if (dependents == null) {
		index = 0;
		dependents = new InnerEmulationDependency[1];
	} else {
		index = dependents.length;
		for (int i = 0; i < index; i++)
			if (dependents[i].scope == dependentScope)
				return; // already stored
		System.arraycopy(dependents, 0, (dependents = new InnerEmulationDependency[index + 1]), 0, index);
	}
	dependents[index] = new InnerEmulationDependency(dependentScope, wasEnclosingInstanceSupplied);
	//  System.out.println("Adding dependency: "+ new String(scope.enclosingType().readableName()) + " --> " + new String(this.readableName()));
}
public char[] computeUniqueKey(boolean isLeaf) {
	char[] outerKey = outermostEnclosingType().computeUniqueKey(isLeaf);
	int semicolon = CharOperation.lastIndexOf(';', outerKey);

	StringBuffer sig = new StringBuffer();
	sig.append(outerKey, 0, semicolon);

	// insert $sourceStart
	sig.append('$');
	sig.append(String.valueOf(this.sourceStart));

	// insert $LocalName if local
	if (!isAnonymousType()) {
		sig.append('$');
		sig.append(this.sourceName);
	}

	// insert remaining from outer key
	sig.append(outerKey, semicolon, outerKey.length-semicolon);

	int sigLength = sig.length();
	char[] uniqueKey = new char[sigLength];
	sig.getChars(0, sigLength, uniqueKey, 0);
	return uniqueKey;
}

public char[] constantPoolName() /* java/lang/Object */ {
	return constantPoolName;
}

ArrayBinding createArrayType(int dimensionCount, LookupEnvironment lookupEnvironment) {
	if (localArrayBindings == null) {
		localArrayBindings = new ArrayBinding[] {new ArrayBinding(this, dimensionCount, lookupEnvironment)};
		return localArrayBindings[0];
	}

	// find the cached array binding for this dimensionCount (if any)
	int length = localArrayBindings.length;
	for (int i = 0; i < length; i++)
		if (localArrayBindings[i].dimensions == dimensionCount)
			return localArrayBindings[i];

	// no matching array
	System.arraycopy(localArrayBindings, 0, localArrayBindings = new ArrayBinding[length + 1], 0, length);
	return localArrayBindings[length] = new ArrayBinding(this, dimensionCount, lookupEnvironment);
}

public char[] readableName() /*java.lang.Object,  p.X<T> */ {
    char[] readableName;
	if (isAnonymousType()) {
		readableName = CharOperation.concat(TypeConstants.ANONYM_PREFIX, this.getSuperBinding0().readableName(), TypeConstants.ANONYM_SUFFIX);
	} else if (isMemberType()) {
		readableName = CharOperation.concat(enclosingType().readableName(), this.sourceName, '.');
	} else {
		readableName = this.sourceName;
	}
	return readableName;
}

public char[] shortReadableName() /*Object*/ {
    char[] shortReadableName;
	if (isAnonymousType()) {
		shortReadableName = CharOperation.concat(TypeConstants.ANONYM_PREFIX, this.getSuperBinding0().shortReadableName(), TypeConstants.ANONYM_SUFFIX);
	} else if (isMemberType()) {
		shortReadableName = CharOperation.concat(enclosingType().shortReadableName(), sourceName, '.');
	} else {
		shortReadableName = sourceName;
	}
	return shortReadableName;
}

// Record that the type is a local member type
public void setAsMemberType() {
	this.tagBits |= TagBits.MemberTypeMask;
}

public void setConstantPoolName(char[] computedConstantPoolName) /* java/lang/Object */ {
	this.constantPoolName = computedConstantPoolName;
}
/*
 * Overriden for code assist. In this case, the constantPoolName() has not been computed yet.
 * Slam the source name so that the signature is syntactically correct.
 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=102284)
 */
public char[] signature() {
	if (this.signature == null && constantPoolName() == null) {
		if (isAnonymousType())
			setConstantPoolName(getSuperBinding().sourceName());
		else
			setConstantPoolName(sourceName());
	}
	return super.signature();
}
public char[] sourceName() {
	if (isAnonymousType()) {
		return CharOperation.concat(TypeConstants.ANONYM_PREFIX, this.getSuperBinding0().sourceName(), TypeConstants.ANONYM_SUFFIX);
	} else
		return sourceName;
}
public String toString() {
	if (isAnonymousType())
		return "Anonymous type : " + super.toString(); //$NON-NLS-1$
	if (isMemberType())
		return "Local member type : " + new String(sourceName()) + " " + super.toString(); //$NON-NLS-2$ //$NON-NLS-1$
	return "Local type : " + new String(sourceName()) + " " + super.toString(); //$NON-NLS-2$ //$NON-NLS-1$
}
}
