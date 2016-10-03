/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IJsDoc;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ImportBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

/**
 * Node representing a structured Javadoc comment
 */
public class Javadoc extends ASTNode implements IJsDoc {

	public JavadocSingleNameReference[] paramReferences; // @param
	public TypeReference[] exceptionReferences; // @throws, @exception
	public JavadocReturnStatement returnStatement; // @return, @returns
	public Expression[] seeReferences; // @see
	public long inheritedPositions = -1;
	// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51600
	// Store param references for tag with invalid syntax
	public JavadocSingleNameReference[] invalidParameters; // @param
	// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=153399
	// Store value tag positions
	public long valuePositions = -1;
	public int modifiers=0;

	public TypeReference namespace=null;
	public TypeReference memberOf=null;
	public TypeReference returnType=null;
	public TypeReference extendsType=null;

	public TypeReference classDef=null;
	public TypeReference methodDef=null;
	public JavadocSingleNameReference property=null;
	public boolean isConstructor;


	public Javadoc(int sourceStart, int sourceEnd) {
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
	}
	/**
	 * Returns whether a type can be seen at a given visibility level or not.
	 *
	 * @param visibility Level of visiblity allowed to see references
	 * @param modifiers modifiers of java element to be seen
	 * @return true if the type can be seen, false otherwise
	 */
	boolean canBeSeen(int visibility, int modifiers) {
		if (modifiers < 0) return true;
		switch (modifiers & ExtraCompilerModifiers.AccVisibilityMASK) {
			case ClassFileConstants.AccPublic :
				return true;
			case ClassFileConstants.AccProtected:
				return (visibility != ClassFileConstants.AccPublic);
			case ClassFileConstants.AccDefault:
				return (visibility == ClassFileConstants.AccDefault || visibility == ClassFileConstants.AccPrivate);
			case ClassFileConstants.AccPrivate:
				return (visibility == ClassFileConstants.AccPrivate);
		}
		return true;
	}

	/*
	 * Search node with a given staring position in javadoc objects arrays.
	 */
	public ASTNode getNodeStartingAt(int start) {
		int length = 0;
		// parameters array
		if (this.paramReferences != null) {
			length = this.paramReferences.length;
			for (int i=0; i<length; i++) {
				JavadocSingleNameReference param = this.paramReferences[i];
				if (param.sourceStart==start) {
					return param;
				}
			}
		}
		// array of invalid syntax tags parameters
		if (this.invalidParameters != null) {
			length = this.invalidParameters.length;
			for (int i=0; i<length; i++) {
				JavadocSingleNameReference param = this.invalidParameters[i];
				if (param.sourceStart==start) {
					return param;
				}
			}
		}
		
		// thrown exception array
		if (this.exceptionReferences != null) {
			length = this.exceptionReferences.length;
			for (int i=0; i<length; i++) {
				TypeReference typeRef = this.exceptionReferences[i];
				if (typeRef.sourceStart==start) {
					return typeRef;
				}
			}
		}
		// references array
		if (this.seeReferences != null) {
			length = this.seeReferences.length;
			for (int i=0; i<length; i++) {
				org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = this.seeReferences[i];
				if (expression.sourceStart==start) {
					return expression;
				} else if (expression instanceof JavadocAllocationExpression) {
					JavadocAllocationExpression allocationExpr = (JavadocAllocationExpression) this.seeReferences[i];
					// if binding is valid then look at arguments
					if (allocationExpr.binding != null && allocationExpr.binding.isValidBinding()) {
						if (allocationExpr.arguments != null) {
							for (int j=0, l=allocationExpr.arguments.length; j<l; j++) {
								if (allocationExpr.arguments[j].sourceStart == start) {
									return allocationExpr.arguments[j];
								}
							}
						}
					}
				} else if (expression instanceof JavadocMessageSend) {
					JavadocMessageSend messageSend = (JavadocMessageSend) this.seeReferences[i];
					// if binding is valid then look at arguments
					if (messageSend.binding != null && messageSend.binding.isValidBinding()) {
						if (messageSend.arguments != null) {
							for (int j=0, l=messageSend.arguments.length; j<l; j++) {
								if (messageSend.arguments[j].sourceStart == start) {
									return messageSend.arguments[j];
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#print(int, java.lang.StringBuffer)
	 */
	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output).append("/**\n"); //$NON-NLS-1$
		if (this.paramReferences != null) {
			for (int i = 0, length = this.paramReferences.length; i < length; i++) {
				printIndent(indent + 1, output).append(" * @param "); //$NON-NLS-1$
				this.paramReferences[i].print(indent, output).append('\n');
			}
		}
		if (this.returnStatement != null) {
			printIndent(indent + 1, output).append(" * @"); //$NON-NLS-1$
			this.returnStatement.print(indent, output).append('\n');
		}
		if (this.exceptionReferences != null) {
			for (int i = 0, length = this.exceptionReferences.length; i < length; i++) {
				printIndent(indent + 1, output).append(" * @throws "); //$NON-NLS-1$
				this.exceptionReferences[i].print(indent, output).append('\n');
			}
		}
		if (this.seeReferences != null) {
			for (int i = 0, length = this.seeReferences.length; i < length; i++) {
				printIndent(indent + 1, output).append(" * @see "); //$NON-NLS-1$
				this.seeReferences[i].print(indent, output).append('\n');
			}
		}

		if (this.returnType!=null)
		{
			printIndent(indent + 1, output).append(" * @type "); //$NON-NLS-1$
			this.returnType.print(indent, output).append('\n');

		}
		if (this.memberOf!=null)
		{
			printIndent(indent + 1, output).append(" * @member "); //$NON-NLS-1$
			this.memberOf.print(indent, output).append('\n');

		}
		if (this.extendsType!=null)
		{
			printIndent(indent + 1, output).append(" * @extends "); //$NON-NLS-1$
			this.extendsType.print(indent, output).append('\n');

		}
		if (this.isConstructor)
			printIndent(indent + 1, output).append(" * @constructor\n"); //$NON-NLS-1$
		if ((this.modifiers & ClassFileConstants.AccPrivate) != 0)
			printIndent(indent + 1, output).append(" * @private\n"); //$NON-NLS-1$
		if ((this.modifiers & ClassFileConstants.AccFinal) != 0)
			printIndent(indent + 1, output).append(" * @final\n"); //$NON-NLS-1$


		printIndent(indent, output).append(" */\n"); //$NON-NLS-1$
		return output;
	}

	/*
	 * Resolve type javadoc
	 */
	public void resolve(ClassScope scope) {

		// @param tags
		int paramTagsSize = this.paramReferences == null ? 0 : this.paramReferences.length;
		for (int i = 0; i < paramTagsSize; i++) {
			JavadocSingleNameReference param = this.paramReferences[i];
			scope.problemReporter().javadocUnexpectedTag(param.tagSourceStart, param.tagSourceEnd);
		}

		// @return tags
		if (this.returnStatement != null) {
			scope.problemReporter().javadocUnexpectedTag(this.returnStatement.sourceStart, this.returnStatement.sourceEnd);
		}

		// @throws/@exception tags
		int throwsTagsLength = this.exceptionReferences == null ? 0 : this.exceptionReferences.length;
		for (int i = 0; i < throwsTagsLength; i++) {
			TypeReference typeRef = this.exceptionReferences[i];
			int start, end;
			if (typeRef instanceof JavadocSingleTypeReference) {
				JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) typeRef;
				start = singleRef.tagSourceStart;
				end = singleRef.tagSourceEnd;
			} else if (typeRef instanceof JavadocQualifiedTypeReference) {
				JavadocQualifiedTypeReference qualifiedRef = (JavadocQualifiedTypeReference) typeRef;
				start = qualifiedRef.tagSourceStart;
				end = qualifiedRef.tagSourceEnd;
			} else {
				start = typeRef.sourceStart;
				end = typeRef.sourceEnd;
			}
			scope.problemReporter().javadocUnexpectedTag(start, end);
		}

		// @see tags
		int seeTagsLength = this.seeReferences == null ? 0 : this.seeReferences.length;
		for (int i = 0; i < seeTagsLength; i++) {
			resolveReference(this.seeReferences[i], scope);
		}

		// @value tag
		boolean source15 = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		if (!source15 && this.valuePositions != -1) {
			scope.problemReporter().javadocUnexpectedTag((int)(this.valuePositions>>>32), (int) this.valuePositions);
		}
	}

	/*
	 * Resolve compilation unit javadoc
	 */
	public void resolve(CompilationUnitScope unitScope) {
		// do nothing
	}

	/*
	 * Resolve method javadoc
	 */
	public void resolve(MethodScope methScope) {

		// get method declaration
		AbstractMethodDeclaration methDecl = methScope.referenceMethod();
		boolean overriding = methDecl == null /* field declaration */ || methDecl.binding == null /* compiler error */
			? false :
			!methDecl.binding.isStatic() && ((methDecl.binding.modifiers & (ExtraCompilerModifiers.AccImplementing | ExtraCompilerModifiers.AccOverriding)) != 0);

		// @see tags
		int seeTagsLength = this.seeReferences == null ? 0 : this.seeReferences.length;
		boolean superRef = false;
		for (int i = 0; i < seeTagsLength; i++) {

			// Resolve reference
			resolveReference(this.seeReferences[i], methScope);

			if (methDecl != null && (methDecl.isConstructor() || overriding) && !superRef) {
				if (this.seeReferences[i] instanceof JavadocMessageSend) {
					JavadocMessageSend messageSend = (JavadocMessageSend) this.seeReferences[i];
					// if binding is valid then look if we have a reference to an overriden method/constructor
					if (messageSend.binding != null && messageSend.binding.isValidBinding() && messageSend.actualReceiverType instanceof ReferenceBinding) {
						ReferenceBinding methodReceiverType = (ReferenceBinding) messageSend.actualReceiverType;
						if ((methodReceiverType.isSuperclassOf(methDecl.binding.declaringClass)) &&
							CharOperation.equals(messageSend.selector, methDecl.getName()) &&
							(methDecl.binding.returnType.isCompatibleWith(messageSend.binding.returnType))) {
							if (messageSend.arguments == null && methDecl.arguments == null) {
								superRef = true;
							}
							else if (messageSend.arguments != null && methDecl.arguments != null) {
								superRef = methDecl.binding.areParametersEqual(messageSend.binding);
							}
						}
					}
				}
				else if (this.seeReferences[i] instanceof JavadocAllocationExpression) {
					JavadocAllocationExpression allocationExpr = (JavadocAllocationExpression) this.seeReferences[i];
					// if binding is valid then look if we have a reference to an overriden method/constructor
					if (allocationExpr.binding != null && allocationExpr.binding.isValidBinding()) {
						if (methDecl.binding.declaringClass.isCompatibleWith(allocationExpr.resolvedType)) {
							if (allocationExpr.arguments == null && methDecl.arguments == null) {
								superRef = true;
							}
							else if (allocationExpr.arguments != null && methDecl.arguments != null) {
								superRef = methDecl.binding.areParametersCompatibleWith(allocationExpr.binding.parameters);
							}
						}
					}
				}
			}
		}

		// Store if a reference exists to an overriden method/constructor or the method is in a local type,
		boolean reportMissing = methDecl == null || !((overriding && this.inheritedPositions != -1) || superRef || (methDecl.binding != null && methDecl.binding.declaringClass != null && methDecl.binding.declaringClass.isLocalType()));
		if (!overriding && this.inheritedPositions != -1) {
			int start = (int) (this.inheritedPositions >>> 32);
			int end = (int) this.inheritedPositions;
			methScope.problemReporter().javadocUnexpectedTag(start, end);
		}

		// @param tags
		boolean considerParamRefAsUsage = methScope.compilerOptions().reportUnusedParameterIncludeDocCommentReference;
		resolveParamTags(methScope, reportMissing,considerParamRefAsUsage);

		// @return tags
		if (this.returnStatement == null) {
			if (reportMissing && methDecl != null) {
				if (methDecl.isMethod()) {
					MethodDeclaration meth = (MethodDeclaration) methDecl;
					if (meth.binding != null && meth.binding.returnType != TypeBinding.VOID && !meth.binding.isConstructor()) {
						// method with return should have @return tag
						methScope.problemReporter().javadocMissingReturnTag(meth.declarationSourceStart, meth.declarationSourceEnd, methDecl.binding.modifiers);
					}
				}
			}
		} else {
			this.returnStatement.resolve(methScope);
		}

		// @throws/@exception tags
		resolveThrowsTags(methScope, reportMissing);

		// @value tag
		boolean source15 = methScope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		if (!source15 && methDecl != null && this.valuePositions != -1) {
			methScope.problemReporter().javadocUnexpectedTag((int)(this.valuePositions>>>32), (int) this.valuePositions);
		}

		// Resolve param tags with invalid syntax
		int length = this.invalidParameters == null ? 0 : this.invalidParameters.length;
		for (int i = 0; i < length; i++) {
			this.invalidParameters[i].resolve(methScope, false, false);
		}
	}

	private void resolveReference(Expression reference, Scope scope) {

		// Perform resolve
		int problemCount = scope.referenceContext().compilationResult().problemCount;
		switch (scope.kind) {
			case Scope.METHOD_SCOPE:
				reference.resolveType((MethodScope)scope);
				break;
			case Scope.CLASS_SCOPE:
				reference.resolveType((ClassScope)scope);
				break;
		}
		boolean hasProblems = scope.referenceContext().compilationResult().problemCount > problemCount;

		// Verify field references
		boolean source15 = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		int scopeModifiers = -1;
		if (reference instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) reference;

			// Verify if this is a method reference
			// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51911
			if (fieldRef.methodBinding != null) {
				if (fieldRef.receiverType != null) {
					if (scope.enclosingSourceType().isCompatibleWith(fieldRef.receiverType)) {
							fieldRef.bits |= ASTNode.SuperAccess;
						}
					fieldRef.methodBinding = scope.findMethod((ReferenceBinding)fieldRef.receiverType, fieldRef.token, new TypeBinding[0], fieldRef);
				}
			}

			// Verify type references
			if (!hasProblems && fieldRef.binding != null && fieldRef.binding.isValidBinding() && fieldRef.receiverType instanceof ReferenceBinding) {
				ReferenceBinding resolvedType = (ReferenceBinding) fieldRef.receiverType;
				verifyTypeReference(fieldRef, fieldRef.receiver, scope, source15, resolvedType, fieldRef.binding.modifiers);
			}

			// That's it for field references
			return;
		}

		// Verify type references
		if (!hasProblems && (reference instanceof JavadocSingleTypeReference || reference instanceof JavadocQualifiedTypeReference) && reference.resolvedType instanceof ReferenceBinding) {
			ReferenceBinding resolvedType = (ReferenceBinding) reference.resolvedType;
			verifyTypeReference(reference, reference, scope, source15, resolvedType, resolvedType.modifiers);
		}

		// Verify that message reference are not used for @value tags
		if (reference instanceof JavadocMessageSend) {
			JavadocMessageSend msgSend = (JavadocMessageSend) reference;

			// Verify type references
			if (!hasProblems && msgSend.binding != null && msgSend.binding.isValidBinding() && msgSend.actualReceiverType instanceof ReferenceBinding) {
				ReferenceBinding resolvedType = (ReferenceBinding) msgSend.actualReceiverType;
				verifyTypeReference(msgSend, msgSend.receiver, scope, source15, resolvedType, msgSend.binding.modifiers);
			}
		}

		// Verify that constructor reference are not used for @value tags
		else if (reference instanceof JavadocAllocationExpression) {
			JavadocAllocationExpression alloc = (JavadocAllocationExpression) reference;

			// Verify type references
			if (!hasProblems && alloc.binding != null && alloc.binding.isValidBinding() && alloc.resolvedType instanceof ReferenceBinding) {
				ReferenceBinding resolvedType = (ReferenceBinding) alloc.resolvedType;
				verifyTypeReference(alloc, alloc.type, scope, source15, resolvedType, alloc.binding.modifiers);
			}
		}
	}

	/*
	 * Resolve @param tags while method scope
	 */
	private void resolveParamTags(MethodScope scope, boolean reportMissing, boolean considerParamRefAsUsage) {
		AbstractMethodDeclaration methodDecl = scope.referenceMethod();
		int paramTagsSize = this.paramReferences == null ? 0 : this.paramReferences.length;

		// If no referenced method (field initializer for example) then report a problem for each param tag
		if (methodDecl == null) {
			for (int i = 0; i < paramTagsSize; i++) {
				JavadocSingleNameReference param = this.paramReferences[i];
				scope.problemReporter().javadocUnexpectedTag(param.tagSourceStart, param.tagSourceEnd);
			}
			return;
		}

		// If no param tags then report a problem for each method argument
		int argumentsSize = methodDecl.arguments == null ? 0 : methodDecl.arguments.length;
		if (paramTagsSize == 0) {
			if (reportMissing) {
				for (int i = 0; i < argumentsSize; i++) {
					Argument arg = methodDecl.arguments[i];
					scope.problemReporter().javadocMissingParamTag(arg.name, arg.sourceStart, arg.sourceEnd, methodDecl.binding == null ? 0 : methodDecl.binding.modifiers);
				}
			}
		} else {
			LocalVariableBinding[] bindings = new LocalVariableBinding[paramTagsSize];
			int maxBindings = 0;

			// Scan all @param tags
			for (int i = 0; i < paramTagsSize; i++) {
				JavadocSingleNameReference param = this.paramReferences[i];
				param.resolve(scope, true, considerParamRefAsUsage);
				if (param.binding != null && param.binding.isValidBinding()) {
					// Verify duplicated tags
					boolean found = false;
					for (int j = 0; j < maxBindings && !found; j++) {
						if (bindings[j] == param.binding) {
							scope.problemReporter().javadocDuplicatedParamTag(param.token, param.sourceStart, param.sourceEnd, methodDecl.binding.modifiers);
							found = true;
						}
					}
					if (!found) {
						bindings[maxBindings++] = (LocalVariableBinding) param.binding;
					}
				}
			}

			// Look for undocumented arguments
			if (reportMissing) {
				for (int i = 0; i < argumentsSize; i++) {
					Argument arg = methodDecl.arguments[i];
					boolean found = false;
					for (int j = 0; j < maxBindings && !found; j++) {
						LocalVariableBinding binding = bindings[j];
						if (arg.binding == binding) {
							found = true;
						}
					}
					if (!found) {
						scope.problemReporter().javadocMissingParamTag(arg.name, arg.sourceStart, arg.sourceEnd, methodDecl.binding.modifiers);
					}
				}
			}
		}
	}

	/*
	 * Resolve @throws/@exception tags while method scope
	 */
	private void resolveThrowsTags(MethodScope methScope, boolean reportMissing) {
		AbstractMethodDeclaration md = methScope.referenceMethod();
		int throwsTagsLength = this.exceptionReferences == null ? 0 : this.exceptionReferences.length;

		// If no referenced method (field initializer for example) then report a problem for each throws tag
		if (md == null) {
			for (int i = 0; i < throwsTagsLength; i++) {
				TypeReference typeRef = this.exceptionReferences[i];
				int start = typeRef.sourceStart;
				int end = typeRef.sourceEnd;
				if (typeRef instanceof JavadocQualifiedTypeReference) {
					start = ((JavadocQualifiedTypeReference) typeRef).tagSourceStart;
					end = ((JavadocQualifiedTypeReference) typeRef).tagSourceEnd;
				} else if (typeRef instanceof JavadocSingleTypeReference) {
					start = ((JavadocSingleTypeReference) typeRef).tagSourceStart;
					end = ((JavadocSingleTypeReference) typeRef).tagSourceEnd;
				}
				methScope.problemReporter().javadocUnexpectedTag(start, end);
			}
			return;
		}

		// If no throws tags then report a problem for each method thrown exception
		if (throwsTagsLength == 0) {
		} else {
			int maxRef = 0;
			TypeReference[] typeReferences = new TypeReference[throwsTagsLength];

			// Scan all @throws tags
			for (int i = 0; i < throwsTagsLength; i++) {
				TypeReference typeRef = this.exceptionReferences[i];
				typeRef.resolve(methScope);
				TypeBinding typeBinding = typeRef.resolvedType;

				if (typeBinding != null && typeBinding.isValidBinding() && typeBinding.isClass()) {
					// accept only valid class binding
					typeReferences[maxRef++] = typeRef;
				}
			}
		}
	}

	private void verifyTypeReference(Expression reference, Expression typeReference, Scope scope, boolean source15, ReferenceBinding resolvedType, int modifiers) {
		if (resolvedType.isValidBinding()) {
			int scopeModifiers = -1;

			// reference must have enough visibility to be used
			if (!canBeSeen(scope.problemReporter().options.reportInvalidJavadocTagsVisibility, modifiers)) {
				scope.problemReporter().javadocHiddenReference(typeReference.sourceStart, reference.sourceEnd, scope, modifiers);
				return;
			}

			// type reference must have enough visibility to be used
			if (reference != typeReference) {
				if (!canBeSeen(scope.problemReporter().options.reportInvalidJavadocTagsVisibility, resolvedType.modifiers)) {
					scope.problemReporter().javadocHiddenReference(typeReference.sourceStart, typeReference.sourceEnd, scope, resolvedType.modifiers);
					return;
				}
			}

			// member types
			if (resolvedType.isMemberType()) {
				ReferenceBinding topLevelType = resolvedType;
				// rebuild and store (in reverse order) compound name to handle embedded inner class
				int packageLength = topLevelType.fPackage.compoundName.length;
				int depth = resolvedType.depth();
				int idx = depth + packageLength;
				char[][] computedCompoundName = new char[idx+1][];
				computedCompoundName[idx] = topLevelType.sourceName;
				while (topLevelType.enclosingType() != null) {
					topLevelType = topLevelType.enclosingType();
					computedCompoundName[--idx] = topLevelType.sourceName;
				}

				// add package information
				for (int i = packageLength; --i >= 0;) {
					computedCompoundName[--idx] = topLevelType.fPackage.compoundName[i];
				}

				ClassScope topLevelScope = scope.classScope();
				// when scope is not on compilation unit type, then inner class may not be visible...
				if (topLevelScope.parent.kind != Scope.COMPILATION_UNIT_SCOPE ||
					!CharOperation.equals(topLevelType.sourceName, topLevelScope.referenceContext.name)) {
					topLevelScope = topLevelScope.outerMostClassScope();
					if (typeReference instanceof JavadocSingleTypeReference) {
						// inner class single reference can only be done in same unit
						if ((!source15 && depth == 1) || topLevelType != topLevelScope.referenceContext.binding) {
							// search for corresponding import
							boolean hasValidImport = false;
							if (source15) {
								CompilationUnitScope unitScope = topLevelScope.compilationUnitScope();
								ImportBinding[] imports = unitScope.imports;
								int length = imports == null ? 0 : imports.length;
								mainLoop: for (int i=0; i<length; i++) {
									char[][] compoundName = imports[i].compoundName;
									int compoundNameLength = compoundName.length;
									if ((imports[i].onDemand && compoundNameLength == computedCompoundName.length-1) ||
										(compoundNameLength == computedCompoundName.length))
									{
										for (int j = compoundNameLength; --j >= 0;) {
											if (CharOperation.equals(imports[i].compoundName[j], computedCompoundName[j])) {
												if (j == 0) {
													hasValidImport = true;
													break mainLoop;
												}
											} else {
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.paramReferences != null) {
				for (int i = 0, length = this.paramReferences.length; i < length; i++) {
					this.paramReferences[i].traverse(visitor, scope);
				}
			}
			if (this.returnStatement != null) {
				this.returnStatement.traverse(visitor, scope);
			}
			if (this.exceptionReferences != null) {
				for (int i = 0, length = this.exceptionReferences.length; i < length; i++) {
					this.exceptionReferences[i].traverse(visitor, scope);
				}
			}
			if (this.seeReferences != null) {
				for (int i = 0, length = this.seeReferences.length; i < length; i++) {
					this.seeReferences[i].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.paramReferences != null) {
				for (int i = 0, length = this.paramReferences.length; i < length; i++) {
					this.paramReferences[i].traverse(visitor, scope);
				}
			}
			if (this.returnStatement != null) {
				this.returnStatement.traverse(visitor, scope);
			}
			if (this.exceptionReferences != null) {
				for (int i = 0, length = this.exceptionReferences.length; i < length; i++) {
					this.exceptionReferences[i].traverse(visitor, scope);
				}
			}
			if (this.seeReferences != null) {
				for (int i = 0, length = this.seeReferences.length; i < length; i++) {
					this.seeReferences[i].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	public JavadocSingleNameReference findParam(char [] name)
	{
		if (this.paramReferences!=null)
			for (int i = 0; i < this.paramReferences.length; i++) {
				if (this.paramReferences[i] != null && CharOperation.equals(name, this.paramReferences[i].token))
					return this.paramReferences[i];
			}
		return null;
	}
	public int getASTType() {
		return IASTNode.JSDOC;
	
	}
}
