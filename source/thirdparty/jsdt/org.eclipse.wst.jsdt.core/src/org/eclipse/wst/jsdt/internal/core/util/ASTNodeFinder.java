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
package org.eclipse.wst.jsdt.internal.core.util;

import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.core.SourceRefElement;
import org.eclipse.wst.jsdt.internal.core.SourceType;

/**
 * Finds an ASTNode given an IJavaScriptElement in a CompilationUnitDeclaration
 */
public class ASTNodeFinder {
	private CompilationUnitDeclaration unit;

	public ASTNodeFinder(CompilationUnitDeclaration unit) {
		this.unit = unit;
	}

	/*
	 * Finds the FieldDeclaration in the given ast corresponding to the given field handle.
	 * Returns null if not found.
	 */
	public FieldDeclaration findField(IField fieldHandle) {
		TypeDeclaration typeDecl = findType((IType)fieldHandle.getParent());
		if (typeDecl == null) return null;
		FieldDeclaration[] fields = typeDecl.fields;
		if (fields != null) {
			char[] fieldName = fieldHandle.getElementName().toCharArray();
			for (int i = 0, length = fields.length; i < length; i++) {
				FieldDeclaration field = fields[i];
				if (CharOperation.equals(fieldName, field.name)) {
					return field;
				}
			}
		}
		return null;
	}

	/*
	 * Finds the Initializer in the given ast corresponding to the given initializer handle.
	 * Returns null if not found.
	 */
	public Initializer findInitializer(IInitializer initializerHandle) {
		TypeDeclaration typeDecl = findType((IType)initializerHandle.getParent());
		if (typeDecl == null) return null;
		FieldDeclaration[] fields = typeDecl.fields;
		if (fields != null) {
			int occurenceCount = ((SourceRefElement)initializerHandle).occurrenceCount;
			for (int i = 0, length = fields.length; i < length; i++) {
				FieldDeclaration field = fields[i];
				if (field instanceof Initializer && --occurenceCount == 0) {
					return (Initializer)field;
				}
			}
		}
		return null;
	}

	/*
	 * Finds the AbstractMethodDeclaration in the given ast corresponding to the given method handle.
	 * Returns null if not found.
	 */
	public AbstractMethodDeclaration findMethod(IFunction methodHandle) {
		TypeDeclaration typeDecl = findType((IType)methodHandle.getParent());
		if (typeDecl == null) return null;
		AbstractMethodDeclaration[] methods = typeDecl.methods;
		if (methods != null) {
			char[] selector = methodHandle.getElementName().toCharArray();
			String[] parameterTypeSignatures = methodHandle.getParameterTypes();
			int parameterCount = parameterTypeSignatures.length;
			nextMethod: for (int i = 0, length = methods.length; i < length; i++) {
				AbstractMethodDeclaration method = methods[i];
				if (CharOperation.equals(selector, method.getName())) {
					Argument[] args = method.arguments;
					int argsLength = args == null ? 0 : args.length;
					if (argsLength == parameterCount) {
						for (int j = 0; j < parameterCount; j++) {
							TypeReference type = args[j].type;
							String signature = Util.typeSignature(type);
							if (!signature.equals(parameterTypeSignatures[j])) {
								continue nextMethod;
							}
						}
						return method;
					}
				}
			}
		}
		return null;
	}

	/*
	 * Finds the TypeDeclaration in the given ast corresponding to the given type handle.
	 * Returns null if not found.
	 */
	public TypeDeclaration findType(IType typeHandle) {
		if (!JavaScriptCore.IS_ECMASCRIPT4)
			return null;
		IJavaScriptElement parent = typeHandle.getParent();
		final char[] typeName = typeHandle.getElementName().toCharArray();
		final int occurenceCount = ((SourceType)typeHandle).occurrenceCount;
		final boolean findAnonymous = typeName.length == 0;
		class Visitor extends ASTVisitor {
			TypeDeclaration result;
			int count = 0;
			public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
				if (result != null) return false;
				if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
					if (findAnonymous && ++count == occurenceCount) {
						result = typeDeclaration;
					}
				} else {
					if (!findAnonymous && CharOperation.equals(typeName, typeDeclaration.name)) {
						result = typeDeclaration;
					}
				}
				return false; // visit only one level
			}
		}
		switch (parent.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				TypeDeclaration[] types = this.unit.types;
				if (types != null) {
					for (int i = 0, length = types.length; i < length; i++) {
						TypeDeclaration type = types[i];
						if (CharOperation.equals(typeName, type.name)) {
							return type;
						}
					}
				}
				break;
			case IJavaScriptElement.TYPE:
				TypeDeclaration parentDecl = findType((IType)parent);
				if (parentDecl == null) return null;
				types = parentDecl.memberTypes;
				if (types != null) {
					for (int i = 0, length = types.length; i < length; i++) {
						TypeDeclaration type = types[i];
						if (CharOperation.equals(typeName, type.name)) {
							return type;
						}
					}
				}
				break;
			case IJavaScriptElement.FIELD:
				FieldDeclaration fieldDecl = findField((IField)parent);
				if (fieldDecl == null) return null;
				Visitor visitor = new Visitor();
				fieldDecl.traverse(visitor, null);
				return visitor.result;
			case IJavaScriptElement.INITIALIZER:
				Initializer initializer = findInitializer((IInitializer)parent);
				if (initializer == null) return null;
				visitor = new Visitor();
				initializer.traverse(visitor, null);
				return visitor.result;
			case IJavaScriptElement.METHOD:
				AbstractMethodDeclaration methodDecl = findMethod((IFunction)parent);
				if (methodDecl == null) return null;
				visitor = new Visitor();
				methodDecl.traverse(visitor, (ClassScope)null);
				return visitor.result;
		}
		return null;
	}
	public InferredType findInferredType(IType typeHandle)
	{
		final char[] typeName = typeHandle.getElementName().toCharArray();
		final int occurenceCount = ((SourceType)typeHandle).occurrenceCount;
		final boolean findAnonymous = typeName.length == 0;
		int count = 0;
		for (int i = 0; i < this.unit.numberInferredTypes; i++) {
			InferredType inferredType = this.unit.inferredTypes[i];
			if (!inferredType.isDefinition())
				continue;

			if (inferredType.isAnonymous) {
				if (findAnonymous && ++count == occurenceCount) {
					return inferredType;
				}
			} else {
				if (!findAnonymous && CharOperation.equals(inferredType.getName(),typeName)) {
					return inferredType;
				}
			}
		}
		return null;
	}
}
