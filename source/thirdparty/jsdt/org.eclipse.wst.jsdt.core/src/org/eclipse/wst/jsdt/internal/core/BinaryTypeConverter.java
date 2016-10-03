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

import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Converter from a binary type to an AST type declaration.
 */
public class BinaryTypeConverter {

	/**
	 * Convert a binary type into an AST type declaration and put it in the given compilation unit.
	 */
	public static TypeDeclaration buildTypeDeclaration(IType type, CompilationUnitDeclaration compilationUnit, CompilationResult compilationResult)  throws JavaScriptModelException {
		PackageFragment pkg = (PackageFragment) type.getPackageFragment();
		char[][] packageName = Util.toCharArrays(pkg.names);

		if (packageName.length > 0) {
			compilationUnit.currentPackage = new ImportReference(packageName, new long[]{0}, false);
		}

		/* convert type */
		TypeDeclaration typeDeclaration = convert(type, null, null, compilationResult);

		IType alreadyComputedMember = type;
		IType parent = type.getDeclaringType();
		TypeDeclaration previousDeclaration = typeDeclaration;
		while(parent != null) {
			TypeDeclaration declaration = convert(parent, alreadyComputedMember, previousDeclaration, compilationResult);

			alreadyComputedMember = parent;
			previousDeclaration = declaration;
			parent = parent.getDeclaringType();
		}

		compilationUnit.types = new TypeDeclaration[]{previousDeclaration};

		return typeDeclaration;
	}

	private static FieldDeclaration convert(IField field, IType type) throws JavaScriptModelException {

		FieldDeclaration fieldDeclaration = new FieldDeclaration();

		fieldDeclaration.name = field.getElementName().toCharArray();
		fieldDeclaration.type = createTypeReference(Signature.toString(field.getTypeSignature()).toCharArray());
		fieldDeclaration.modifiers = field.getFlags();

		return fieldDeclaration;
	}

	private static AbstractMethodDeclaration convert(IFunction method, IType type, CompilationResult compilationResult) throws JavaScriptModelException {

		AbstractMethodDeclaration methodDeclaration;

		if (method.isConstructor()) {
			ConstructorDeclaration decl = new ConstructorDeclaration(compilationResult);
			decl.isDefaultConstructor = false;
			methodDeclaration = decl;
		} else {
			MethodDeclaration decl = new MethodDeclaration(compilationResult);
			/* convert return type */
			decl.returnType = createTypeReference(Signature.toString(method.getReturnType()).toCharArray());
			methodDeclaration = decl;
		}
		methodDeclaration.setSelector(method.getElementName().toCharArray());
		int flags = method.getFlags();
		boolean isVarargs = Flags.isVarargs(flags);
		methodDeclaration.modifiers = flags & ~Flags.AccVarargs;

		/* convert arguments */
		String[] argumentTypeNames = method.getParameterTypes();
		String[] argumentNames = method.getParameterNames();
		int argumentCount = argumentTypeNames == null ? 0 : argumentTypeNames.length;
		methodDeclaration.arguments = new Argument[argumentCount];
		for (int i = 0; i < argumentCount; i++) {
			String argumentTypeName = argumentTypeNames[i];
			TypeReference typeReference = createTypeReference(Signature.toString(argumentTypeName).toCharArray());
			if (isVarargs && i == argumentCount-1) {
				typeReference.bits |= ASTNode.IsVarArgs;
			}
			methodDeclaration.arguments[i] = new Argument(
				argumentNames[i].toCharArray(),
				0,
				typeReference,
				ClassFileConstants.AccDefault);
			// do not care whether was final or not
		}

		return methodDeclaration;
	}

	private static TypeDeclaration convert(IType type, IType alreadyComputedMember,TypeDeclaration alreadyComputedMemberDeclaration, CompilationResult compilationResult) throws JavaScriptModelException {
		/* create type declaration - can be member type */
		TypeDeclaration typeDeclaration = new TypeDeclaration(compilationResult);

		if (type.getDeclaringType() != null) {
			typeDeclaration.bits |= ASTNode.IsMemberType;
		}
		typeDeclaration.name = type.getElementName().toCharArray();
		typeDeclaration.modifiers = type.getFlags();


		/* set superclass and superinterfaces */
		if (type.getSuperclassName() != null) {
			typeDeclaration.superclass = createTypeReference(type.getSuperclassName().toCharArray());
			typeDeclaration.superclass.bits |= ASTNode.IsSuperType;
		}

		/* convert member types */
		IType[] memberTypes = type.getTypes();
		int memberTypeCount =	memberTypes == null ? 0 : memberTypes.length;
		typeDeclaration.memberTypes = new TypeDeclaration[memberTypeCount];
		for (int i = 0; i < memberTypeCount; i++) {
			if(alreadyComputedMember != null && alreadyComputedMember.getFullyQualifiedName().equals(memberTypes[i].getFullyQualifiedName())) {
				typeDeclaration.memberTypes[i] = alreadyComputedMemberDeclaration;
			} else {
				typeDeclaration.memberTypes[i] = convert(memberTypes[i], null, null, compilationResult);
			}
		}

		/* convert fields */
		IField[] fields = type.getFields();
		int fieldCount = fields == null ? 0 : fields.length;
		typeDeclaration.fields = new FieldDeclaration[fieldCount];
		for (int i = 0; i < fieldCount; i++) {
			typeDeclaration.fields[i] = convert(fields[i], type);
		}

		/* convert methods - need to add default constructor if necessary */
		IFunction[] methods = type.getFunctions();
		int methodCount = methods == null ? 0 : methods.length;

		/* source type has a constructor ?           */
		/* by default, we assume that one is needed. */
		int neededCount = 1;
		for (int i = 0; i < methodCount; i++) {
			if (methods[i].isConstructor()) {
				neededCount = 0;
				// Does not need the extra constructor since one constructor already exists.
				break;
			}
		}

		typeDeclaration.methods = new AbstractMethodDeclaration[methodCount + neededCount];
		if (neededCount != 0) { // add default constructor in first position
			typeDeclaration.methods[0] = typeDeclaration.createDefaultConstructor(false, false);
		}
		boolean hasAbstractMethods = false;
		for (int i = 0; i < methodCount; i++) {
			AbstractMethodDeclaration method =convert(methods[i], type, compilationResult);
			boolean isAbstract = method.isAbstract();
			if (isAbstract) { // fix-up flag
				method.modifiers |= ExtraCompilerModifiers.AccSemicolonBody;
			}
			if (isAbstract) {
				hasAbstractMethods = true;
			}
			typeDeclaration.methods[neededCount + i] = method;
		}
		if (hasAbstractMethods) {
			typeDeclaration.bits |= ASTNode.HasAbstractMethods;
		}
		return typeDeclaration;
	}

	private static TypeReference createTypeReference(char[] type) {
		/* count identifiers and dimensions */
		int max = type.length;
		int dimStart = max;
		int dim = 0;
		int identCount = 1;
		for (int i = 0; i < max; i++) {
			switch (type[i]) {
				case '[' :
					if (dim == 0)
						dimStart = i;
					dim++;
					break;
				case '.' :
					identCount++;
					break;
			}
		}
		/* rebuild identifiers and dimensions */
		if (identCount == 1) { // simple type reference
			if (dim == 0) {
				return new SingleTypeReference(type, 0);
			} else {
				char[] identifier = new char[dimStart];
				System.arraycopy(type, 0, identifier, 0, dimStart);
				return new ArrayTypeReference(identifier, dim, 0);
			}
		} else { // qualified type reference
			char[][] identifiers =	CharOperation.splitOn('.', type, 0, dimStart);
			if (dim == 0) {
				return new QualifiedTypeReference(identifiers, new long[identifiers.length]);
			} else {
				return new ArrayQualifiedTypeReference(identifiers, dim, new long[identifiers.length]);
			}
		}
	}
}
