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
package org.eclipse.wst.jsdt.core.dom;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Literal;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;

/**
 * Internal class for resolving bindings using old ASTs.
 * <p>
 * IMPORTANT: The methods on this class are synchronized. This is required
 * because there may be multiple clients in separate threads concurrently
 * reading an AST and asking for bindings for its nodes. These requests all
 * end up invoking instance methods on this class. There are various internal
 * tables and caches which are built and maintained in the course of looking
 * up bindings. To ensure that they remain coherent in the presence of multiple
 * threads, the methods are synchronized on the DefaultBindingResolver instance.
 * </p>
  *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
*/
class DefaultBindingResolver extends BindingResolver {

	/*
	 * Holds on binding tables that can be shared by several ASTs.
	 */
	static class BindingTables {

		/**
		 * This map is used to get a binding from its binding key.
		 */
		Map bindingKeysToBindings;
		/**
		 * This map is used to keep the correspondance between new bindings and the
		 * validator bindings to their internal counterpart.
		 * This is an identity map. We should only create one object for one binding.
		 */
		Map compilerBindingsToASTBindings;

		BindingTables() {
			this.compilerBindingsToASTBindings = new HashMap();
			this.bindingKeysToBindings = new HashMap();
		}

	}
	/**
	 * This map is used to retrieve the corresponding block scope for a ast node
	 */
	Map astNodesToBlockScope;

	/**
	 * This map is used to get an ast node from its binding (new binding) or DOM
	 */
	Map bindingsToAstNodes;

	/*
	 * The shared binding tables accros ASTs.
	 */
	BindingTables bindingTables;

	/**
	 * This map is used to retrieve an old ast node using the new ast node. This is not an
	 * identity map.
	 */
	Map newAstToOldAst;

	/**
	 * JavaScript unit scope
	 */
	private CompilationUnitScope scope;

	/**
	 * The working copy owner that defines the context in which this resolver is creating the bindings.
	 */
	WorkingCopyOwner workingCopyOwner;
	boolean isRecoveredBinding;


	/**
	 * Constructor for DefaultBindingResolver.
	 */
	DefaultBindingResolver(CompilationUnitScope scope, WorkingCopyOwner workingCopyOwner, BindingTables bindingTables, boolean isRecoveredBinding) {
		this.newAstToOldAst = new HashMap();
		this.astNodesToBlockScope = new HashMap();
		this.bindingsToAstNodes = new HashMap();
		this.bindingTables = bindingTables;
		this.scope = scope;
		this.workingCopyOwner = workingCopyOwner;
		this.isRecoveredBinding = isRecoveredBinding;
	}

	DefaultBindingResolver(LookupEnvironment lookupEnvironment, WorkingCopyOwner workingCopyOwner, BindingTables bindingTables, boolean isRecoveredBinding) {
		this.newAstToOldAst = new HashMap();
		this.astNodesToBlockScope = new HashMap();
		this.bindingsToAstNodes = new HashMap();
		this.bindingTables = bindingTables;
		this.scope = new CompilationUnitScope(new CompilationUnitDeclaration(null, null, -1), lookupEnvironment);
		this.workingCopyOwner = workingCopyOwner;
		this.isRecoveredBinding = isRecoveredBinding;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized ASTNode findDeclaringNode(IBinding binding) {
		if (binding == null) {
			return null;
		}
		if (binding instanceof IFunctionBinding) {
			IFunctionBinding methodBinding = (IFunctionBinding) binding;
			return (ASTNode) this.bindingsToAstNodes.get(methodBinding.getMethodDeclaration());
		} else if (binding instanceof ITypeBinding) {
			ITypeBinding typeBinding = (ITypeBinding) binding;
			return (ASTNode) this.bindingsToAstNodes.get(typeBinding.getTypeDeclaration());
		} else if (binding instanceof IVariableBinding) {
			IVariableBinding variableBinding = (IVariableBinding) binding;
			return (ASTNode) this.bindingsToAstNodes.get(variableBinding.getVariableDeclaration());
		}
		return (ASTNode) this.bindingsToAstNodes.get(binding);
	}

	synchronized ASTNode findDeclaringNode(String bindingKey) {
		if (bindingKey == null) {
			return null;
		}
		Object binding = this.bindingTables.bindingKeysToBindings.get(bindingKey);
		if (binding == null)
			return null;
		return (ASTNode) this.bindingsToAstNodes.get(binding);
	}

	IBinding getBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.Binding binding) {
		switch (binding.kind()) {
			case Binding.PACKAGE:
				return getPackageBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) binding);
			case Binding.TYPE:
			case Binding.BASE_TYPE:
				return getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) binding);
			case Binding.ARRAY_TYPE:
				return new TypeBinding(this, (org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) binding);
			case Binding.METHOD:
				return getMethodBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding) binding);
			case Binding.FIELD:
			case Binding.LOCAL:
				return getVariableBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding) binding);
		}
		return null;
	}

	synchronized org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode getCorrespondingNode(ASTNode currentNode) {
		return (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(currentNode);
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IFunctionBinding getMethodBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding) {
 		if (methodBinding != null && !methodBinding.isValidBinding()) {
			org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding problemMethodBinding =
				(org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding) methodBinding;
			methodBinding = problemMethodBinding.closestMatch;
 		}

		if (methodBinding != null) {
			IFunctionBinding binding = (IFunctionBinding) this.bindingTables.compilerBindingsToASTBindings.get(methodBinding);
			if (binding != null) {
				return binding;
			}
			binding = new FunctionBinding(this, methodBinding);
			this.bindingTables.compilerBindingsToASTBindings.put(methodBinding, binding);
			return binding;
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IPackageBinding getPackageBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding packageBinding) {
		if (packageBinding == null || !packageBinding.isValidBinding()) {
			return null;
		}
		IPackageBinding binding = (IPackageBinding) this.bindingTables.compilerBindingsToASTBindings.get(packageBinding);
		if (binding != null) {
			return binding;
		}
		binding = new PackageBinding(packageBinding, this);
		this.bindingTables.compilerBindingsToASTBindings.put(packageBinding, binding);
		return binding;
	}

	/**
	 * Returns the new type binding corresponding to the given variable declaration.
	 * This is used for recovered binding only.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param variableDeclaration the given variable declaration
	 * @return the new type binding
	 */
	synchronized ITypeBinding getTypeBinding(VariableDeclaration variableDeclaration) {
		ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(variableDeclaration);
		if (binding != null) {
			return binding;
		}
		binding = new RecoveredTypeBinding(this, variableDeclaration);
		this.bindingTables.compilerBindingsToASTBindings.put(variableDeclaration, binding);
		return binding;
	}

	/**
	 * Returns the new type binding corresponding to the given type.
	 * This is used for recovered binding only.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the given type
	 * @return the new type binding
	 */
	synchronized ITypeBinding getTypeBinding(Type type) {
		ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(type);
		if (binding != null) {
			return binding;
		}
		binding = new RecoveredTypeBinding(this, type);
		this.bindingTables.compilerBindingsToASTBindings.put(type, binding);
		return binding;
	}


	/*
	 * Method declared on BindingResolver.
	 */
	synchronized ITypeBinding getTypeBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding referenceBinding) {
		if (referenceBinding == null) {
			return null;
		} else if (!referenceBinding.isValidBinding()) {
			switch(referenceBinding.problemId()) {
				case ProblemReasons.NotVisible :
				case ProblemReasons.NonStaticReferenceInStaticContext :
					if (referenceBinding instanceof ProblemReferenceBinding) {
						ProblemReferenceBinding problemReferenceBinding = (ProblemReferenceBinding) referenceBinding;
						ReferenceBinding binding2 = problemReferenceBinding.closestMatch();
						ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(binding2);
						if (binding != null) {
							return binding;
						}
						binding = new TypeBinding(this, binding2);
						this.bindingTables.compilerBindingsToASTBindings.put(binding2, binding);
						return binding;
					}
					break;
				case ProblemReasons.NotFound :
					if (this.isRecoveredBinding) {
						ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(referenceBinding);
						if (binding != null) {
							return binding;
						}
						binding = new RecoveredTypeBinding(this, referenceBinding);
						this.bindingTables.compilerBindingsToASTBindings.put(referenceBinding, binding);
						return binding;
					}
			}
			return null;
		} else {
			ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(referenceBinding);
			if (binding != null) {
				return binding;
			}
			if (referenceBinding instanceof CompilationUnitBinding)
				binding = new org.eclipse.wst.jsdt.core.dom.JavaScriptUnitBinding(this, referenceBinding);
			else
				binding = new TypeBinding(this, referenceBinding);
			this.bindingTables.compilerBindingsToASTBindings.put(referenceBinding, binding);
			return binding;
		}
	}

	synchronized ITypeBinding getTypeBinding(RecoveredTypeBinding recoveredTypeBinding, int dimensions) {
		if (recoveredTypeBinding== null) {
			return null;
		}
		return new RecoveredTypeBinding(this, recoveredTypeBinding, dimensions);
	}

	synchronized IVariableBinding getVariableBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding variableBinding, VariableDeclaration variableDeclaration) {
		if (this.isRecoveredBinding) {
			if (variableBinding != null) {
				if (variableBinding.isValidBinding()) {
					IVariableBinding binding = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(variableBinding);
					if (binding != null) {
						return binding;
					}
					if (variableBinding.type != null) {
						binding = new VariableBinding(this, variableBinding);
					} else {
						binding = new RecoveredVariableBinding(this, variableDeclaration);
					}
					this.bindingTables.compilerBindingsToASTBindings.put(variableBinding, binding);
					return binding;
				} else {
					/*
					 * http://dev.eclipse.org/bugs/show_bug.cgi?id=24449
					 */
					if (variableBinding instanceof ProblemFieldBinding) {
						ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) variableBinding;
						switch(problemFieldBinding.problemId()) {
							case ProblemReasons.NotVisible :
							case ProblemReasons.NonStaticReferenceInStaticContext :
							case ProblemReasons.NonStaticReferenceInConstructorInvocation :
								ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
								FieldBinding exactBinding = declaringClass.getField(problemFieldBinding.name, true /*resolve*/);
								if (exactBinding != null) {
									IVariableBinding variableBinding2 = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
									if (variableBinding2 != null) {
										return variableBinding2;
									}
									variableBinding2 = new VariableBinding(this, exactBinding);
									this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding2);
									return variableBinding2;
								}
								break;
						}
					}
				}
			}
			return null;
		}
		return this.getVariableBinding(variableBinding);
	}

	public WorkingCopyOwner getWorkingCopyOwner() {
		return this.workingCopyOwner;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IVariableBinding getVariableBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding variableBinding) {
 		if (variableBinding != null) {
	 		if (variableBinding.isValidBinding()) {
	 			if (variableBinding.type != null) {
					IVariableBinding binding = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(variableBinding);
					if (binding != null) {
						return binding;
					}
					binding = new VariableBinding(this, variableBinding);
					this.bindingTables.compilerBindingsToASTBindings.put(variableBinding, binding);
					return binding;
	 			}
	 		} else {
				/*
				 * http://dev.eclipse.org/bugs/show_bug.cgi?id=24449
				 */
				if (variableBinding instanceof ProblemFieldBinding) {
					ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) variableBinding;
					switch(problemFieldBinding.problemId()) {
						case ProblemReasons.NotVisible :
						case ProblemReasons.NonStaticReferenceInStaticContext :
						case ProblemReasons.NonStaticReferenceInConstructorInvocation :
							ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
							FieldBinding exactBinding = declaringClass.getField(problemFieldBinding.name, true /*resolve*/);
							if (exactBinding != null) {
								IVariableBinding variableBinding2 = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
								if (variableBinding2 != null) {
									return variableBinding2;
								}
								variableBinding2 = new VariableBinding(this, exactBinding);
								this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding2);
								return variableBinding2;
							}
							break;
					}
				}
	 		}
 		}
		return null;
	}

	boolean isResolvedTypeInferredFromExpectedType(FunctionInvocation methodInvocation) {
		Object oldNode = this.newAstToOldAst.get(methodInvocation);
		if (oldNode instanceof MessageSend) {
//			MessageSend messageSend = (MessageSend) oldNode;
//			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding = messageSend.binding;
//			if (methodBinding instanceof ParameterizedGenericMethodBinding) {
//				ParameterizedGenericMethodBinding genericMethodBinding = (ParameterizedGenericMethodBinding) methodBinding;
//				return genericMethodBinding.inferredReturnType;
//			}
		}
		return false;
	}

	boolean isResolvedTypeInferredFromExpectedType(SuperMethodInvocation superMethodInvocation) {
		Object oldNode = this.newAstToOldAst.get(superMethodInvocation);
		if (oldNode instanceof MessageSend) {
//			MessageSend messageSend = (MessageSend) oldNode;
//			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding = messageSend.binding;
//			if (methodBinding instanceof ParameterizedGenericMethodBinding) {
//				ParameterizedGenericMethodBinding genericMethodBinding = (ParameterizedGenericMethodBinding) methodBinding;
//				return genericMethodBinding.inferredReturnType;
//			}
		}
		return false;
	}


	/*
	 * Method declared on BindingResolver.
	 */
	LookupEnvironment lookupEnvironment() {
		return this.scope.environment();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.dom.BindingResolver#recordScope(ASTNode, BlockScope)
	 */
	synchronized void recordScope(ASTNode astNode, BlockScope blockScope) {
		this.astNodesToBlockScope.put(astNode, blockScope);
	}

	/*
	 * @see BindingResolver#resolveConstantExpressionValue(Expression)
	 */
	Object resolveConstantExpressionValue(Expression expression) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node != null && (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Expression)) {
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression compilerExpression = (org.eclipse.wst.jsdt.internal.compiler.ast.Expression) node;
			Constant constant = compilerExpression.constant;
			if (constant != null && constant != Constant.NotAConstant) {
				switch (constant.typeID()) {
					case TypeIds.T_int : return Integer.valueOf(constant.intValue());
					case TypeIds.T_short : return Short.valueOf(constant.shortValue());
					case TypeIds.T_char : return Character.valueOf(constant.charValue());
					case TypeIds.T_float : return new Float(constant.floatValue());
					case TypeIds.T_double : return new Double(constant.doubleValue());
					case TypeIds.T_boolean : return constant.booleanValue() ? Boolean.TRUE : Boolean.FALSE;
					case TypeIds.T_long : return Long.valueOf(constant.longValue());
					case TypeIds.T_JavaLangString : return constant.stringValue();
				}
				return null;
			}
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveConstructor(ClassInstanceCreation)
	 */
	synchronized IFunctionBinding resolveConstructor(ClassInstanceCreation expression) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node != null && (node.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.IsAnonymousType) != 0) {
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration anonymousLocalTypeDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) node;
			return this.getMethodBinding(anonymousLocalTypeDeclaration.allocation.binding);
		} else if (node instanceof AllocationExpression) {
			return this.getMethodBinding(((AllocationExpression)node).binding);
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveConstructor(ConstructorInvocation)
	 */
	synchronized IFunctionBinding resolveConstructor(ConstructorInvocation expression) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node instanceof ExplicitConstructorCall) {
			ExplicitConstructorCall explicitConstructorCall = (ExplicitConstructorCall) node;
			return this.getMethodBinding(explicitConstructorCall.binding);
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveConstructor(SuperConstructorInvocation)
	 */
	synchronized IFunctionBinding resolveConstructor(SuperConstructorInvocation expression) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node instanceof ExplicitConstructorCall) {
			ExplicitConstructorCall explicitConstructorCall = (ExplicitConstructorCall) node;
			return this.getMethodBinding(explicitConstructorCall.binding);
		}
		return null;
	}
	/*
	 * Method declared on BindingResolver.
	 */
	synchronized ITypeBinding resolveExpressionType(Expression expression) {
		try {
			switch(expression.getNodeType()) {
				case ASTNode.CLASS_INSTANCE_CREATION :
					org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode astNode = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
					if (astNode instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
						// anonymous type case
						org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) astNode;
						ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
						if (typeBinding != null) {
							return typeBinding;
						}
					} else {
						// should be an AllocationExpression
						AllocationExpression allocationExpression = (AllocationExpression) astNode;
						return this.getTypeBinding(allocationExpression.resolvedType);
					}
					break;
				case ASTNode.SIMPLE_NAME :
				case ASTNode.QUALIFIED_NAME :
					return this.resolveTypeBindingForName((Name) expression);
				case ASTNode.ARRAY_INITIALIZER :
				case ASTNode.ARRAY_CREATION :
				case ASTNode.ASSIGNMENT :
				case ASTNode.POSTFIX_EXPRESSION :
				case ASTNode.PREFIX_EXPRESSION :
				case ASTNode.TYPE_LITERAL :
				case ASTNode.INFIX_EXPRESSION :
				case ASTNode.INSTANCEOF_EXPRESSION :
				case ASTNode.FIELD_ACCESS :
				case ASTNode.SUPER_FIELD_ACCESS :
				case ASTNode.ARRAY_ACCESS :
				case ASTNode.FUNCTION_INVOCATION :
				case ASTNode.SUPER_METHOD_INVOCATION :
				case ASTNode.CONDITIONAL_EXPRESSION :
					org.eclipse.wst.jsdt.internal.compiler.ast.Expression compilerExpression = (org.eclipse.wst.jsdt.internal.compiler.ast.Expression) this.newAstToOldAst.get(expression);
					if (compilerExpression != null) {
						return this.getTypeBinding(compilerExpression.resolvedType);
					}
					break;
				case ASTNode.STRING_LITERAL :
					if (this.scope != null) {
						return this.getTypeBinding(this.scope.getJavaLangString());
					}
					break;
				case ASTNode.BOOLEAN_LITERAL :
				case ASTNode.NULL_LITERAL :
				case ASTNode.UNDEFINED_LITERAL :
				case ASTNode.CHARACTER_LITERAL :
				case ASTNode.REGULAR_EXPRESSION_LITERAL :
				case ASTNode.NUMBER_LITERAL :
					Literal literal = (Literal) this.newAstToOldAst.get(expression);
					return this.getTypeBinding(literal.literalType(this.scope));
				case ASTNode.THIS_EXPRESSION :
					ThisReference thisReference = (ThisReference) this.newAstToOldAst.get(expression);
					BlockScope blockScope = (BlockScope) this.astNodesToBlockScope.get(expression);
					if (blockScope != null) {
						return this.getTypeBinding(thisReference.resolveType(blockScope));
					}
					break;
				case ASTNode.PARENTHESIZED_EXPRESSION :
					ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
					return this.resolveExpressionType(parenthesizedExpression.getExpression());
				case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) expression;
					Type type = variableDeclarationExpression.getType();
					if (type != null) {
						return type.resolveBinding();
					}
					break;
			}
		} catch (AbortCompilation e) {
			// handle missing types
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveField(FieldAccess)
	 */
	synchronized IVariableBinding resolveField(FieldAccess fieldAccess) {
		Object oldNode = this.newAstToOldAst.get(fieldAccess);
		if (oldNode instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) oldNode;
			return this.getVariableBinding(fieldReference.binding);
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveField(SuperFieldAccess)
	 */
	synchronized IVariableBinding resolveField(SuperFieldAccess fieldAccess) {
		Object oldNode = this.newAstToOldAst.get(fieldAccess);
		if (oldNode instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) oldNode;
			return this.getVariableBinding(fieldReference.binding);
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveImport(ImportDeclaration)
	 */
	synchronized IBinding resolveImport(ImportDeclaration importDeclaration) {
		if (this.scope == null) return null;
		try {
			org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(importDeclaration);
			if (node instanceof ImportReference) {
				ImportReference importReference = (ImportReference) node;
				if ((importReference.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OnDemand) != 0) {
					Binding binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, importReference.tokens.length), true);
					if (binding != null) {
						if ((binding.kind() & Binding.PACKAGE) != 0) {
							IPackageBinding packageBinding = this.getPackageBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) binding);
							if (packageBinding == null) {
								return null;
							}
							return packageBinding;
						} else {
							// if it is not a package, it has to be a type
							ITypeBinding typeBinding = this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) binding);
							if (typeBinding == null) {
								return null;
							}
							return typeBinding;
						}
					}
				} else {
					Binding binding = this.scope.getImport(importReference.tokens, false);
					if (binding != null) {
						if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
							ITypeBinding typeBinding = this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) binding);
							return typeBinding == null ? null : typeBinding;
						}
					}
				}
			}
		} catch(AbortCompilation e) {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IFunctionBinding resolveMethod(FunctionDeclaration method) {
		Object oldNode = this.newAstToOldAst.get(method);
		if (oldNode instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) oldNode;
			IFunctionBinding methodBinding = this.getMethodBinding(methodDeclaration.getBinding());
			if (methodBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(methodBinding, method);
			String key = methodBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, methodBinding);
			}
			return methodBinding;
		}
		return null;
	}
	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IFunctionBinding resolveMethod(FunctionInvocation method) {
		Object oldNode = this.newAstToOldAst.get(method);
		if (oldNode instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) oldNode;
			return this.getMethodBinding(messageSend.binding);
		}
		return null;
	}
	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IFunctionBinding resolveMethod(SuperMethodInvocation method) {
		Object oldNode = this.newAstToOldAst.get(method);
		if (oldNode instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) oldNode;
			return this.getMethodBinding(messageSend.binding);
		}
		return null;
	}

	synchronized ITypeBinding resolveTypeBindingForName(Name name) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(name);
		int index = name.index;
		if (node instanceof QualifiedNameReference) {
			QualifiedNameReference qualifiedNameReference = (QualifiedNameReference) node;
			final char[][] tokens = qualifiedNameReference.tokens;
			if (tokens.length == index) {
				return this.getTypeBinding(qualifiedNameReference.resolvedType);
			}
			int indexOfFirstFieldBinding = qualifiedNameReference.indexOfFirstFieldBinding; // one-based
			if (index < indexOfFirstFieldBinding) {
				// an extra lookup is required
				BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
				Binding binding = null;
				try {
					if (internalScope == null) {
						if (this.scope == null) return null;
						binding = this.scope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					} else {
						binding = internalScope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					}
				} catch (AbortCompilation e) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
				}
				if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) {
					return null;
				} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
					// it is a type
					return this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)binding);
				}
			} else if (index == indexOfFirstFieldBinding) {
				if (qualifiedNameReference.isTypeReference()) {
					return this.getTypeBinding(qualifiedNameReference.resolvedType);
				} else {
					// in this case we want to get the next field declaring's class
					if (qualifiedNameReference.otherBindings == null) {
						return null;
					}
					FieldBinding fieldBinding = qualifiedNameReference.otherBindings[0];
					if (fieldBinding == null) return null;
					org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding type = fieldBinding.declaringClass;
					if (type == null) { // array length scenario
						// use type from first binding (no capture needed for array type)
						switch (qualifiedNameReference.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.RestrictiveFlagMASK) {
							case Binding.FIELD:
								type = ((FieldBinding) qualifiedNameReference.binding).type;
								break;
							case Binding.LOCAL:
								type = ((LocalVariableBinding) qualifiedNameReference.binding).type;
								break;
						}
					}
					return this.getTypeBinding(type);
				}
			} else {
				/* This is the case for a name which is part of a qualified name that
				 * cannot be resolved. See PR 13063.
				 */
				if (qualifiedNameReference.otherBindings == null) return null;
				final int otherBindingsLength = qualifiedNameReference.otherBindings.length;
				if (otherBindingsLength == (index - indexOfFirstFieldBinding)) {
					return this.getTypeBinding(qualifiedNameReference.resolvedType);
				}
				FieldBinding fieldBinding = qualifiedNameReference.otherBindings[index - indexOfFirstFieldBinding];
				if (fieldBinding == null) return null;
				org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding type = fieldBinding.declaringClass;
				if (type == null) { // array length scenario
					// use type from previous binding (no capture needed for array type)
					fieldBinding = qualifiedNameReference.otherBindings[index - indexOfFirstFieldBinding - 1];
					if (fieldBinding == null) return null;
					type = fieldBinding.type;
				}
				return this.getTypeBinding(type);
			}
		} else if (node instanceof QualifiedTypeReference) {
			QualifiedTypeReference qualifiedTypeReference = (QualifiedTypeReference) node;
			if (qualifiedTypeReference.resolvedType == null) {
				return null;
			}
			if (index == qualifiedTypeReference.tokens.length) {
				if (!qualifiedTypeReference.resolvedType.isValidBinding() && qualifiedTypeReference instanceof JavadocQualifiedTypeReference) {
					JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) node;
					if (typeRef.packageBinding != null) {
						return null;
					}
				}
				return this.getTypeBinding(qualifiedTypeReference.resolvedType.leafComponentType());
			} else {
				if (index >= 0) {
					BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
					Binding binding = null;
					try {
						if (internalScope == null) {
							if (this.scope == null) return null;
							binding = this.scope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						} else {
							binding = internalScope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						}
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
					if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) {
						return null;
					} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)binding);
					} else {
						return null;
					}
				}
			}
		} else if (node instanceof ImportReference) {
			ImportReference importReference = (ImportReference) node;
			int importReferenceLength = importReference.tokens.length;
			if (index >= 0) {
				Binding binding = null;
				if (this.scope == null) return null;
				if (importReferenceLength == index) {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), (importReference.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OnDemand) != 0);
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				} else {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), true);
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				}
				if (binding != null) {
					if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)binding);
					}
					return null;
				}
			}
		} else if (node instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) node;
			IFunctionBinding method = this.getMethodBinding(methodDeclaration.getBinding());
			if (method == null) return null;
			return method.getReturnType();
		} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding != null) {
				return typeBinding;
			}
		} if (node instanceof JavadocSingleNameReference) {
			JavadocSingleNameReference singleNameReference = (JavadocSingleNameReference) node;
			LocalVariableBinding localVariable = (LocalVariableBinding)singleNameReference.binding;
			if (localVariable != null) {
				return this.getTypeBinding(localVariable.type);
			}
		} if (node instanceof SingleNameReference) {
			SingleNameReference singleNameReference = (SingleNameReference) node;
			return this.getTypeBinding(singleNameReference.resolvedType);
		} else if (node instanceof LocalDeclaration) {
			IVariableBinding variable = this.getVariableBinding(((LocalDeclaration)node).binding);
			if (variable == null) return null;
			return variable.getType();
		} else if (node instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) node;
			if (fieldRef.methodBinding != null) {
				return getMethodBinding(fieldRef.methodBinding).getReturnType();
			}
			return getTypeBinding(fieldRef.resolvedType);
		} else if (node instanceof FieldReference) {
			return getTypeBinding(((FieldReference) node).resolvedType);
		} else if (node instanceof SingleTypeReference) {
			SingleTypeReference singleTypeReference = (SingleTypeReference) node;
			org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding = singleTypeReference.resolvedType;
			if (binding != null) {
				return this.getTypeBinding(binding.leafComponentType());
			}
		} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) node;
			IVariableBinding field = this.getVariableBinding(fieldDeclaration.binding);
			if (field == null) return null;
			return field.getType();
		} else if (node instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) node;
			IFunctionBinding method = getMethodBinding(messageSend.binding);
			if (method == null) return null;
			return method.getReturnType();
		} else if (node instanceof AllocationExpression) {
			AllocationExpression allocation = (AllocationExpression) node;
			return getTypeBinding(allocation.resolvedType);
		} else if (node instanceof JavadocImplicitTypeReference) {
			JavadocImplicitTypeReference implicitRef = (JavadocImplicitTypeReference) node;
			return getTypeBinding(implicitRef.resolvedType);
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IBinding resolveName(Name name) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(name);
		int index = name.index;
		if (node instanceof QualifiedNameReference) {
			QualifiedNameReference qualifiedNameReference = (QualifiedNameReference) node;
			final char[][] tokens = qualifiedNameReference.tokens;
			int indexOfFirstFieldBinding = qualifiedNameReference.indexOfFirstFieldBinding; // one-based
			if (index < indexOfFirstFieldBinding) {
				// an extra lookup is required
				BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
				Binding binding = null;
				try {
					if (internalScope == null) {
						if (this.scope == null) return null;
						binding = this.scope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					} else {
						binding = internalScope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					}
				} catch (AbortCompilation e) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
				}
				if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) {
					return this.getPackageBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding)binding);
				} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
					// it is a type
					return this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)binding);
				}
			} else if (index == indexOfFirstFieldBinding) {
				if (qualifiedNameReference.isTypeReference()) {
					return this.getTypeBinding(qualifiedNameReference.resolvedType);
				} else {
					Binding binding = qualifiedNameReference.binding;
					if (binding != null) {
						if (binding.isValidBinding()) {
							return this.getVariableBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding) binding);
						} else  if (binding instanceof ProblemFieldBinding) {
							ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) binding;
							switch(problemFieldBinding.problemId()) {
								case ProblemReasons.NotVisible :
								case ProblemReasons.NonStaticReferenceInStaticContext :
									ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
									if (declaringClass != null) {
										FieldBinding exactBinding = declaringClass.getField(tokens[tokens.length - 1], true /*resolve*/);
										if (exactBinding != null) {
											if (exactBinding.type != null) {
												IVariableBinding variableBinding = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
												if (variableBinding != null) {
													return variableBinding;
												}
												variableBinding = new VariableBinding(this, exactBinding);
												this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding);
												return variableBinding;
											}
										}
									}
									break;
							}
						}
					}
				}
			} else {
				/* This is the case for a name which is part of a qualified name that
				 * cannot be resolved. See PR 13063.
				 */
				if (qualifiedNameReference.otherBindings == null || (index - indexOfFirstFieldBinding - 1) < 0) {
					return null;
				} else {
					return this.getVariableBinding(qualifiedNameReference.otherBindings[index - indexOfFirstFieldBinding - 1]);
				}
			}
		} else if (node instanceof QualifiedTypeReference) {
			QualifiedTypeReference qualifiedTypeReference = (QualifiedTypeReference) node;
			if (qualifiedTypeReference.resolvedType == null) {
				return null;
			}
			if (index == qualifiedTypeReference.tokens.length) {
				if (!qualifiedTypeReference.resolvedType.isValidBinding() && qualifiedTypeReference instanceof JavadocQualifiedTypeReference) {
					JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) node;
					if (typeRef.packageBinding != null) {
						return getPackageBinding(typeRef.packageBinding);
					}
				}
				return this.getTypeBinding(qualifiedTypeReference.resolvedType.leafComponentType());
			} else {
				if (index >= 0) {
					BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
					Binding binding = null;
					try {
						if (internalScope == null) {
							if (this.scope == null) return null;
							binding = this.scope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						} else {
							binding = internalScope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						}
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
					if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) {
						return this.getPackageBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding)binding);
					} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)binding);
					} else {
						return null;
					}
				}
			}
		} else if (node instanceof ImportReference) {
			ImportReference importReference = (ImportReference) node;
			int importReferenceLength = importReference.tokens.length;
			if (index >= 0) {
				Binding binding = null;
				if (this.scope == null) return null;
				if (importReferenceLength == index) {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), (importReference.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OnDemand) != 0);
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				} else {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), true);
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				}
				if (binding != null) {
					if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) {
						return this.getPackageBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding)binding);
					} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)binding);
					} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding) {
						// it is a type
						return this.getVariableBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding)binding);
					} else if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding) {
						// it is a type
						return this.getMethodBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding)binding);
					} else {
						return null;
					}
				}
			}
		} else if (node instanceof CompilationUnitDeclaration) {
			CompilationUnitDeclaration compilationUnitDeclaration = (CompilationUnitDeclaration) node;
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
			if (types == null || types.length == 0) {
				return null;
			}
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration type = types[0];
			if (type != null) {
				ITypeBinding typeBinding = this.getTypeBinding(type.binding);
				if (typeBinding != null) {
					return typeBinding.getPackage();
				}
			}
		} else if (node instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) node;
			IFunctionBinding methodBinding = this.getMethodBinding(methodDeclaration.getBinding());
			if (methodBinding != null) {
				return methodBinding;
			}
		} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding != null) {
				return typeBinding;
			}
		} if (node instanceof SingleNameReference) {
			SingleNameReference singleNameReference = (SingleNameReference) node;
			if (singleNameReference.isTypeReference()) {
				return this.getTypeBinding(singleNameReference.resolvedType);
			} else {
				// this is a variable or a field
				Binding binding = singleNameReference.binding;
				if (binding != null) {
					if (binding.isValidBinding()) {
						if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding)
							return this.getVariableBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding) binding);
						if (binding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding)
								return this.getMethodBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding) binding);
					} else {
						/*
						 * http://dev.eclipse.org/bugs/show_bug.cgi?id=24449
						 */
						if (binding instanceof ProblemFieldBinding) {
							ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) binding;
							switch(problemFieldBinding.problemId()) {
								case ProblemReasons.NotVisible :
								case ProblemReasons.NonStaticReferenceInStaticContext :
								case ProblemReasons.NonStaticReferenceInConstructorInvocation :
									ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
									FieldBinding exactBinding = declaringClass.getField(problemFieldBinding.name, true /*resolve*/);
									if (exactBinding != null) {
										if (exactBinding.type != null) {
											IVariableBinding variableBinding2 = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
											if (variableBinding2 != null) {
												return variableBinding2;
											}
											variableBinding2 = new VariableBinding(this, exactBinding);
											this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding2);
											return variableBinding2;
										}
									}
									break;
							}
						}
					}
	 			}
			}
		} else if (node instanceof LocalDeclaration) {
			return this.getVariableBinding(((LocalDeclaration)node).binding);
		} else if (node instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) node;
			if (fieldRef.methodBinding != null) {
				return getMethodBinding(fieldRef.methodBinding);
			}
			return getVariableBinding(fieldRef.binding);
		} else if (node instanceof FieldReference) {
			return getVariableBinding(((FieldReference) node).binding);
		} else if (node instanceof SingleTypeReference) {
			SingleTypeReference singleTypeReference = (SingleTypeReference) node;
			org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding = singleTypeReference.resolvedType;
			if (binding != null) {
				if (!binding.isValidBinding() && node instanceof JavadocSingleTypeReference) {
					JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) node;
					if (typeRef.packageBinding != null) {
						return getPackageBinding(typeRef.packageBinding);
					}
				}
				return this.getTypeBinding(binding.leafComponentType());
			}
		} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) node;
			return this.getVariableBinding(fieldDeclaration.binding);
		} else if (node instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) node;
			return getMethodBinding(messageSend.binding);
		} else if (node instanceof AllocationExpression) {
			AllocationExpression allocation = (AllocationExpression) node;
			return getMethodBinding(allocation.binding);
		} else if (node instanceof JavadocImplicitTypeReference) {
			JavadocImplicitTypeReference implicitRef = (JavadocImplicitTypeReference) node;
			return getTypeBinding(implicitRef.resolvedType);
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolvePackage(PackageDeclaration)
	 */
	synchronized IPackageBinding resolvePackage(PackageDeclaration pkg) {
		if (this.scope == null) return null;
		try {
			org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(pkg);
			if (node instanceof ImportReference) {
				ImportReference importReference = (ImportReference) node;
				Binding binding = this.scope.getTypeOrPackage(CharOperation.subarray(importReference.tokens, 0, importReference.tokens.length));
				if ((binding != null) && (binding.isValidBinding())) {
					IPackageBinding packageBinding = this.getPackageBinding((org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding) binding);
					if (packageBinding == null) {
						return null;
					}
					this.bindingsToAstNodes.put(packageBinding, pkg);
					String key = packageBinding.getKey();
					if (key != null) {
						this.bindingTables.bindingKeysToBindings.put(key, packageBinding);
					}
					return packageBinding;
				}
			}
		} catch (AbortCompilation e) {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see BindingResolver#resolveReference(MemberRef)
     *  
	 */
	synchronized IBinding resolveReference(MemberRef ref) {
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = (org.eclipse.wst.jsdt.internal.compiler.ast.Expression) this.newAstToOldAst.get(ref);
		if (expression instanceof TypeReference) {
			return getTypeBinding(expression.resolvedType);
		}
		else if (expression instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) expression;
			if (fieldRef.methodBinding != null) {
				return getMethodBinding(fieldRef.methodBinding);
			}
			return getVariableBinding(fieldRef.binding);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see BindingResolver#resolveReference(FunctionRef)
     *  
	 */
	synchronized IBinding resolveReference(FunctionRef ref) {
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = (org.eclipse.wst.jsdt.internal.compiler.ast.Expression) this.newAstToOldAst.get(ref);
		if (expression instanceof JavadocMessageSend) {
			return this.getMethodBinding(((JavadocMessageSend)expression).binding);
		}
		else if (expression instanceof JavadocAllocationExpression) {
			return this.getMethodBinding(((JavadocAllocationExpression)expression).binding);
		}
		return null;
	}

	/*
	 * @see BindingResolver#resolveType(AnonymousClassDeclaration)
	 */
	synchronized ITypeBinding resolveType(AnonymousClassDeclaration type) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(type);
		if (node != null && (node.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.IsAnonymousType) != 0) {
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration anonymousLocalTypeDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(anonymousLocalTypeDeclaration.binding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized ITypeBinding resolveType(Type type) {
		// retrieve the old ast node
		org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = (org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(type);
		org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding = null;
		if (node != null) {
            if (node instanceof TypeReference) {
				TypeReference typeReference = (TypeReference) node;
				binding = typeReference.resolvedType;
			} else if (node instanceof SingleNameReference && ((SingleNameReference)node).isTypeReference()) {
				binding = (((SingleNameReference)node).resolvedType);
			} else if (node instanceof QualifiedNameReference && ((QualifiedNameReference)node).isTypeReference()) {
				binding = (((QualifiedNameReference)node).resolvedType);
			} else if (node instanceof ArrayAllocationExpression) {
				binding = ((ArrayAllocationExpression) node).resolvedType;
			}
			if (binding != null) {
				if (type.isArrayType()) {
					ArrayType arrayType = (ArrayType) type;
					if (this.scope == null) return null;
					if (binding.isArrayType()) {
						ArrayBinding arrayBinding = (ArrayBinding) binding;
						return getTypeBinding(this.scope.createArrayType(arrayBinding.leafComponentType, arrayType.getDimensions()));
					} else {
						return getTypeBinding(this.scope.createArrayType(binding, arrayType.getDimensions()));
					}
				} else {
					if (binding.isArrayType()) {
						ArrayBinding arrayBinding = (ArrayBinding) binding;
						return getTypeBinding(arrayBinding.leafComponentType);
					} else {
						return getTypeBinding(binding);
					}
				}
			}
		} else if (type.isPrimitiveType()) {
			/* Handle the void primitive type returned by getReturnType for a method declaration
			 * that is a constructor declaration. It prevents null from being returned
			 */
			if (((PrimitiveType) type).getPrimitiveTypeCode() == PrimitiveType.VOID) {
				return this.getTypeBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.VOID);
			}
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized ITypeBinding resolveType(TypeDeclaration type) {
		final Object node = this.newAstToOldAst.get(type);
		if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	ITypeBinding resolveType(JavaScriptUnit compilationUnit) {
		final Object node = this.newAstToOldAst.get(compilationUnit);
		if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration compilationUnitDeclaration =
				(org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(compilationUnitDeclaration.compilationUnitBinding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, compilationUnit);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized IVariableBinding resolveVariable(VariableDeclaration variable) {
		final Object node = this.newAstToOldAst.get(variable);
		if (node instanceof AbstractVariableDeclaration) {
			AbstractVariableDeclaration abstractVariableDeclaration = (AbstractVariableDeclaration) node;
			if (abstractVariableDeclaration instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) {
				org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) abstractVariableDeclaration;
				IVariableBinding variableBinding = this.getVariableBinding(fieldDeclaration.binding, variable);
				if (variableBinding == null) {
					return null;
				}
				this.bindingsToAstNodes.put(variableBinding, variable);
				String key = variableBinding.getKey();
				if (key != null) {
					this.bindingTables.bindingKeysToBindings.put(key, variableBinding);
				}
				return variableBinding;
			}
			IVariableBinding variableBinding = this.getVariableBinding(((LocalDeclaration) abstractVariableDeclaration).binding, variable);
			if (variableBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(variableBinding, variable);
			String key = variableBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, variableBinding);
			}
			return variableBinding;
		}
		return null;
	}

	synchronized IVariableBinding resolveVariable(VariableDeclarationStatement variable) {
		final Object node = this.newAstToOldAst.get(variable);
		if (node instanceof AbstractVariableDeclaration) {
			AbstractVariableDeclaration abstractVariableDeclaration = (AbstractVariableDeclaration) node;
			if (abstractVariableDeclaration instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) {
				org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) abstractVariableDeclaration;
				IVariableBinding variableBinding = this.getVariableBinding(fieldDeclaration.binding);
				if (variableBinding == null) {
					return null;
				}
				this.bindingsToAstNodes.put(variableBinding, variable);
				String key = variableBinding.getKey();
				if (key != null) {
					this.bindingTables.bindingKeysToBindings.put(key, variableBinding);
				}
				return variableBinding;
			}
			IVariableBinding variableBinding = this.getVariableBinding(((LocalDeclaration) abstractVariableDeclaration).binding);
			if (variableBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(variableBinding, variable);
			String key = variableBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, variableBinding);
			}
			return variableBinding;
		}
		return null;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized ITypeBinding resolveWellKnownType(String name) {
		if (this.scope == null) return null;
		try {
			// possible called by flow info
//			if (("boolean".equals(name))//$NON-NLS-1$
//				|| ("char".equals(name))//$NON-NLS-1$
//				|| ("byte".equals(name))//$NON-NLS-1$
//				|| ("short".equals(name))//$NON-NLS-1$
//				|| ("int".equals(name))//$NON-NLS-1$
//				|| ("long".equals(name))//$NON-NLS-1$
//				|| ("float".equals(name))//$NON-NLS-1$
//				|| ("double".equals(name))//$NON-NLS-1$
//				|| ("void".equals(name))) {//$NON-NLS-1$
//				return this.getTypeBinding(Scope.getBaseType(name.toCharArray()));
//			} else
			if ("Object".equals(name)) {//$NON-NLS-1$
				return this.getTypeBinding(this.scope.getJavaLangObject());
			} else if ("String".equals(name)) {//$NON-NLS-1$
				return this.getTypeBinding(this.scope.getJavaLangString());
			} else if ("Number".equals(name)) {//$NON-NLS-1$
				return this.getTypeBinding(this.scope.getJavaLangNumber());
			} else if ("Function".equals(name)) {//$NON-NLS-1$
				return this.getTypeBinding(this.scope.getJavaLangFunction());
			} else if ("Boolean".equals(name)) {//$NON-NLS-1$
				return this.getTypeBinding(this.scope.getJavaLangBoolean());
//			} else if ("java.lang.StringBuffer".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_STRINGBUFFER, 3));
//			} else if ("java.lang.Throwable".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getJavaLangThrowable());
//			} else if ("java.lang.Exception".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_EXCEPTION, 3));
//			} else if ("java.lang.RuntimeException".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION, 3));
//			} else if ("java.lang.Error".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_ERROR, 3));
//			} else if ("java.lang.Class".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getJavaLangClass());
//			} else if ("java.lang.Cloneable".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getJavaLangCloneable());
//			} else if ("java.io.Serializable".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getJavaIoSerializable());
//			} else if ("java.lang.Boolean".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_BOOLEAN, 3));
//			} else if ("java.lang.Byte".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_BYTE, 3));
//			} else if ("java.lang.Character".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_CHARACTER, 3));
//			} else if ("java.lang.Double".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_DOUBLE, 3));
//			} else if ("java.lang.Float".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_FLOAT, 3));
//			} else if ("java.lang.Integer".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_INTEGER, 3));
//			} else if ("java.lang.Long".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_LONG, 3));
//			} else if ("java.lang.Short".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_SHORT, 3));
//			} else if ("java.lang.Void".equals(name)) {//$NON-NLS-1$
//				return this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_VOID, 3));
			}
		} catch (AbortCompilation e) {
			// ignore missing types
		}
		return null;
	}


	/*
	 * Method declared on BindingResolver.
	 */
	public CompilationUnitScope scope() {
		return this.scope;
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized void store(ASTNode node, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode oldASTNode) {
		this.newAstToOldAst.put(node, oldASTNode);
	}

	/*
	 * Method declared on BindingResolver.
	 */
	synchronized void updateKey(ASTNode node, ASTNode newNode) {
		Object astNode = this.newAstToOldAst.remove(node);
		if (astNode != null) {
			this.newAstToOldAst.put(newNode, astNode);
		}
	}

	/**
	 * Answer an array type binding with the given type binding and the given
	 * dimensions.
	 *
	 * <p>If the given type binding is an array binding, then the resulting dimensions is the given dimensions
	 * plus the existing dimensions of the array binding. Otherwise the resulting dimensions is the given
	 * dimensions.</p>
	 *
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param typeBinding the given type binding
	 * @param dimensions the given dimensions
	 * @return an array type binding with the given type binding and the given
	 * dimensions
	 * @throws IllegalArgumentException if the type binding represents the <code>void</code> type binding
	 */
	ITypeBinding resolveArrayType(ITypeBinding typeBinding, int dimensions) {
		if (typeBinding.isRecovered()) throw new IllegalArgumentException("Cannot be called on a recovered type binding"); //$NON-NLS-1$
		ITypeBinding leafComponentType = typeBinding;
		int actualDimensions = dimensions;
		if (typeBinding.isArray()) {
			leafComponentType = typeBinding.getElementType();
			actualDimensions += typeBinding.getDimensions();
		}
 		org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding leafTypeBinding = null;
 		if (leafComponentType.isPrimitive()) {
 	 		String name = leafComponentType.getBinaryName();
			switch(name.charAt(0)) {
				case 'I' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.INT;
					break;
				case 'Z' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.BOOLEAN;
					break;
				case 'C' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.CHAR;
					break;
				case 'J' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.LONG;
					break;
				case 'S' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.SHORT;
					break;
				case 'D' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.DOUBLE;
					break;
				case 'F' :
					leafTypeBinding = org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.FLOAT;
					break;
				case 'V' :
					throw new IllegalArgumentException();
			}
 		} else {
 			leafTypeBinding = ((TypeBinding) leafComponentType).binding;
 		}
		if (!(leafComponentType instanceof TypeBinding)) return null;
		return this.getTypeBinding(this.lookupEnvironment().createArrayType(leafTypeBinding, actualDimensions));
	}
}
