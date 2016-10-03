/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import java.util.ArrayList;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.Compiler;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class BindingKeyResolver extends BindingKeyParser {
	Compiler compiler;
	Binding compilerBinding;

	char[][] compoundName;
	int dimension;
	LookupEnvironment environment;
	ReferenceBinding genericType;
	MethodBinding methodBinding;

	char[] secondarySimpleName;
	CompilationUnitDeclaration parsedUnit;
	BlockScope scope;
	TypeBinding typeBinding;
	TypeDeclaration typeDeclaration;
	ArrayList types = new ArrayList();
	int rank = 0;

	int wildcardRank;

	CompilationUnitDeclaration outerMostParsedUnit;

	private BindingKeyResolver(BindingKeyParser parser, Compiler compiler, LookupEnvironment environment, int wildcardRank, CompilationUnitDeclaration outerMostParsedUnit) {
		super(parser);
		this.compiler = compiler;
		this.environment = environment;
		this.wildcardRank = wildcardRank;
		this.outerMostParsedUnit = outerMostParsedUnit;
	}

	public BindingKeyResolver(String key) {
		this(key, null, null);
	}

	public BindingKeyResolver(String key, Compiler compiler, LookupEnvironment environment) {
		super(key);
		this.compiler = compiler;
		this.environment = environment;
	}

	/*
	 * If not already cached, computes and cache the compound name (pkg name + top level name) of this key.
	 * Returns the package name if key is a pkg key.
	 * Returns an empty array if malformed.
	 * This key's scanner should be positioned on the package or type token.
	 */
	public char[][] compoundName() {
		return this.compoundName;
	}

	public void consumeArrayDimension(char[] brakets) {
		this.dimension = brakets.length;
	}

	public void consumeBaseType(char[] baseTypeSig) {
		this.compoundName = new char[][] {getKey().toCharArray()};
		TypeBinding baseTypeBinding = getBaseTypeBinding(baseTypeSig);
		if (baseTypeBinding != null) {
			this.typeBinding = baseTypeBinding;
		}
	}

	public void consumeException() {
		this.types = new ArrayList();
	}

	public void consumeField(char[] fieldName) {
		FieldBinding[] fields = ((ReferenceBinding) this.typeBinding).availableFields(); // resilience
	 	for (int i = 0, length = fields.length; i < length; i++) {
			FieldBinding field = fields[i];
			if (CharOperation.equals(fieldName, field.name)) {
				this.typeBinding = null;
				this.compilerBinding = field;
				return;
			}
		}
	}

	public void consumeLocalType(char[] uniqueKey) {
 		LocalTypeBinding[] localTypeBindings  = this.parsedUnit.localTypes;
 		for (int i = 0; i < this.parsedUnit.localTypeCount; i++)
 			if (CharOperation.equals(uniqueKey, localTypeBindings[i].computeUniqueKey(false/*not a leaf*/))) {
 				this.typeBinding = localTypeBindings[i];
 				return;
 			}
	}

	public void consumeLocalVar(char[] varName) {
		if (this.scope == null) {
			this.scope = this.methodBinding.sourceMethod().getScope();
		}
	 	for (int i = 0; i < this.scope.localIndex; i++) {
			LocalVariableBinding local = this.scope.locals[i];
			if (CharOperation.equals(varName, local.name)) {
				this.methodBinding = null;
				this.compilerBinding = local;
				return;
			}
		}
	}

	public void consumeMethod(char[] selector, char[] signature) {
		MethodBinding[] methods = ((ReferenceBinding) this.typeBinding).availableMethods(); // resilience
	 	for (int i = 0, methodLength = methods.length; i < methodLength; i++) {
			MethodBinding method = methods[i];
			if (CharOperation.equals(selector, method.selector) || (selector.length == 0 && method.isConstructor())) {
				char[] methodSignature = method.signature();
				if (CharOperation.equals(signature, methodSignature)) {
					this.typeBinding = null;
					this.methodBinding = method;
					this.compilerBinding = this.methodBinding;
					return;
				}
			}
		}
	}

	public void consumeMemberType(char[] simpleTypeName) {
		this.typeBinding = getTypeBinding(simpleTypeName);
	}

	public void consumePackage(char[] pkgName) {
		this.compoundName = CharOperation.splitOn('/', pkgName);
		this.compilerBinding = new PackageBinding(this.compoundName, null, this.environment);
	}

	public void consumeParser(BindingKeyParser parser) {
		this.types.add(parser);
	}

	public void consumeScope(int scopeNumber) {
		if (this.scope == null) {
			this.scope = this.methodBinding.sourceMethod().getScope();
		}
		if (scopeNumber >= this.scope.subscopeCount)
			return; // malformed key
		this.scope = (BlockScope) this.scope.subscopes[scopeNumber];
	}

	public void consumeSecondaryType(char[] simpleTypeName) {
		this.secondarySimpleName = simpleTypeName;
	}

	public void consumeFullyQualifiedName(char[] fullyQualifiedName) {
		this.compoundName = CharOperation.splitOn('/', fullyQualifiedName);
	}

	public void consumeTopLevelType() {
		this.parsedUnit = getCompilationUnitDeclaration();
		if (this.parsedUnit != null && this.compiler != null) {
			this.compiler.process(this.parsedUnit, this.compiler.totalUnits+1); // noop if unit has already been resolved
		}
		if (this.parsedUnit == null) {
			this.typeBinding = getBinaryBinding();
		} else {
			char[] typeName = this.secondarySimpleName == null ? this.compoundName[this.compoundName.length-1] : this.secondarySimpleName;
			this.typeBinding = getTypeBinding(typeName);
		}
	}

	public void consumeKey() {
		if (this.typeBinding != null) {
			this.typeBinding = getArrayBinding(this.dimension, this.typeBinding);
			this.compilerBinding = this.typeBinding;
		}
	}

	public void consumeTypeWithCapture() {
		BindingKeyResolver resolver = (BindingKeyResolver) this.types.get(0);
		this.typeBinding =(TypeBinding) resolver.compilerBinding;
	}

	/*
	 * If the given dimension is greater than 0 returns an array binding for the given type binding.
	 * Otherwise return the given type binding.
	 * Returns null if the given type binding is null.
	 */
	private TypeBinding getArrayBinding(int dim, TypeBinding binding) {
		if (binding == null) return null;
		if (dim == 0) return binding;
		return this.environment.createArrayType(binding, dim);
	}

	private TypeBinding getBaseTypeBinding(char[] signature) {
		switch (signature[0]) {
			case 'I' :
				return TypeBinding.INT;
			case 'Z' :
				return TypeBinding.BOOLEAN;
			case 'V' :
				return TypeBinding.VOID;
			case 'C' :
				return TypeBinding.CHAR;
			case 'D' :
				return TypeBinding.DOUBLE;
			case 'F' :
				return TypeBinding.FLOAT;
			case 'J' :
				return TypeBinding.LONG;
			case 'S' :
				return TypeBinding.SHORT;
			case 'N':
				return TypeBinding.NULL;
			default :
				return null;
		}
	}

	/*
	 * Returns a binary binding corresonding to this key's compound name.
	 * Returns null if not found.
	 */
	private TypeBinding getBinaryBinding() {
		if (this.compoundName.length == 0) return null;
		return this.environment.getType(this.compoundName);
	}

	/*
	 * Finds the compilation unit declaration corresponding to the key in the given lookup environment.
	 * Returns null if no compilation unit declaration could be found.
	 * This key's scanner should be positioned on the package token.
	 */
	public CompilationUnitDeclaration getCompilationUnitDeclaration() {
		char[][] name = this.compoundName;
		if (name.length == 0) return null;
		if (this.environment == null) return null;
		ReferenceBinding binding = this.environment.getType(name);
		if (!(binding instanceof SourceTypeBinding)) {
			if (this.secondarySimpleName == null)
				return null;
			// case of a secondary type with no primary type (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=177115)
			int length = name.length;
			System.arraycopy(name, 0, name = new char[length][], 0, length-1);
			name[length-1] = this.secondarySimpleName;
			binding = this.environment.getType(name);
			if (!(binding instanceof SourceTypeBinding))
				return null;
		}
		SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) binding;
		if (sourceTypeBinding.scope == null)
			return null;
		return sourceTypeBinding.scope.compilationUnitScope().referenceContext;
	}

	/*
	 * Returns the compiler binding corresponding to this key.
	 * Returns null is malformed.
	 * This key's scanner should be positioned on the package token.
	 */
	public Binding getCompilerBinding() {
		try {
			parse();
			return this.compilerBinding;
		} catch (RuntimeException e) {
			Util.log(e, "Could not create binding from binding key: " + getKey()); //$NON-NLS-1$
			return null;
		}
	}

	private TypeBinding getTypeBinding(char[] simpleTypeName) {
		if (this.typeBinding instanceof BinaryTypeBinding) {
			return ((BinaryTypeBinding) this.typeBinding).getMemberType(simpleTypeName);
		} else {
			TypeDeclaration[] typeDeclarations =
				this.typeDeclaration == null ?
					(this.parsedUnit == null ? null : this.parsedUnit.types) :
					this.typeDeclaration.memberTypes;
			if (typeDeclarations == null) return null;
			for (int i = 0, length = typeDeclarations.length; i < length; i++) {
				TypeDeclaration declaration = typeDeclarations[i];
				if (CharOperation.equals(simpleTypeName, declaration.name)) {
					this.typeDeclaration = declaration;
					return declaration.binding;
				}
			}
		}
		return null;
	}

	private TypeBinding[] getTypeBindingArguments() {
		int size = this.types.size();
		TypeBinding[] arguments = new TypeBinding[size];
		for (int i = 0; i < size; i++) {
			BindingKeyResolver resolver = (BindingKeyResolver) this.types.get(i);
			TypeBinding compilerBinding2 = (TypeBinding) resolver.compilerBinding;
			if (compilerBinding2 == null) {
				throw new IllegalArgumentException();
			}
			arguments[i] = compilerBinding2;
		}
		this.types = new ArrayList();
		return arguments;
	}

	public void malformedKey() {
		this.compoundName = CharOperation.NO_CHAR_CHAR;
	}

	public BindingKeyParser newParser() {
		return new BindingKeyResolver(this, this.compiler, this.environment, this.rank, this.outerMostParsedUnit == null ? this.parsedUnit : this.outerMostParsedUnit);
	}

	public String toString() {
		return getKey();
	}

}
