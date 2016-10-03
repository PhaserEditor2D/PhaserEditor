/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;

public class MissingBinaryTypeBinding extends SourceTypeBinding {

/**
 * Special constructor for constructing proxies of missing binary types (114349)
 * @param packageBinding
 * @param compoundName
 * @param environment
 */
public MissingBinaryTypeBinding(PackageBinding packageBinding, char[][] compoundName, LookupEnvironment environment, Scope parentScope) {
	super(compoundName,packageBinding,new ClassScope(parentScope,new InferredType(null)));
	this.compoundName = compoundName;
	computeId();
	this.tagBits |= TagBits.IsBinaryBinding | TagBits.HierarchyHasProblems;
//	this.environment = environment;
	this.fPackage = packageBinding;
	this.fileName = CharOperation.concatWith(compoundName, '/');
	this.sourceName = compoundName[compoundName.length - 1]; // [java][util][Map$Entry]
	this.modifiers = ClassFileConstants.AccPublic;
	this.setSuperBinding(null); // will be fixed up using #setMissingSuperclass(...)
	this.memberTypes = Binding.NO_MEMBER_TYPES;
	this.fields = Binding.NO_FIELDS;
	this.methods = Binding.NO_METHODS;
}

/**
 * Missing binary type will answer <code>false</code> to #isValidBinding()
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Binding#problemId()
 */
public int problemId() {
	return ProblemReasons.NotFound;
}

/**
 * Only used to fixup the superclass hierarchy of proxy binary types
 * @param missingSuperclass
 * @see LookupEnvironment#cacheMissingBinaryType(char[][], org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration)
 */
void setMissingSuperclass(ReferenceBinding missingSuperclass) {
	this.setSuperBinding(missingSuperclass);
}
}
