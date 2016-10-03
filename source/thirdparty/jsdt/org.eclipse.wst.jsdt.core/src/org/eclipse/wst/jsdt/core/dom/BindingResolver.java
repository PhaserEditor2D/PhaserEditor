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

package org.eclipse.wst.jsdt.core.dom;

import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;

/**
 * A binding resolver is an internal mechanism for figuring out the binding
 * for a major declaration, type, or name reference. 
 * <p>
 * The default implementation serves as the default binding resolver
 * that does no resolving whatsoever. Internal subclasses do all the real work.
 * </p>
 *
 * @see AST#getBindingResolver
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
class BindingResolver {

	/**
	 * Creates a binding resolver.
	 */
	BindingResolver() {
		// default implementation: do nothing
	}

	/**
	 * Finds the corresponding AST node from which the given binding originated.
	 * Returns <code>null</code> if the binding does not correspond to any node
	 * in the javaScript unit.
	 * <p>
	 * The following table indicates the expected node type for the various
	 * different kinds of bindings:
	 * <ul>
	 * <li></li>
	 * <li>var/field - a <code>VariableDeclarationFragment</code> in a
	 *    <code>FieldDeclaration</code> </li>
	 * <li>local variable - a <code>SingleVariableDeclaration</code>, or
	 *    a <code>VariableDeclarationFragment</code> in a
	 *    <code>VariableDeclarationStatement</code> or
	 *    <code>VariableDeclarationExpression</code></li>
	 * <li>function/method - a <code>FunctionDeclaration</code> </li>
	 * </ul>
	 * <ul>
	 * </p>
	 * <p>
	 * The implementation of <code>JavaScriptUnit.findDeclaringNode</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param binding the binding
	 * @return the corresponding node where the bindings is declared,
	 *    or <code>null</code> if none
	 */
	ASTNode findDeclaringNode(IBinding binding) {
		return null;
	}

	/**
	 * Finds the corresponding AST node from which the given binding key originated.
	 *
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param bindingKey the binding key
	 * @return the corresponding node where the bindings is declared,
	 *    or <code>null</code> if none
	 */
	ASTNode findDeclaringNode(String bindingKey) {
		return null;
	}

	/**
	 * Allows the user to get information about the given old/new pair of
	 * AST nodes.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param currentNode the new node
	 * @return org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode
	 */
	org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode getCorrespondingNode(ASTNode currentNode) {
		return null;
	}

	/**
	 * Returns the new method binding corresponding to the given old method binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param methodBinding the old method binding
	 * @return the new method binding
	 */
	IFunctionBinding getMethodBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding) {
		return null;
	}

	/*
	 * Returns the new package binding corresponding to the given old package binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param packageBinding the old package binding
	 * @return the new package binding
	 */
	IPackageBinding getPackageBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding packageBinding) {
		return null;
	}

	/**
	 * Returns the new type binding corresponding to the given old type binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param referenceBinding the old type binding
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding referenceBinding) {
		return null;
	}


	/**
	 * Returns the new type binding corresponding to the given variableDeclaration.
	 * This is used for recovered binding only.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param variableDeclaration the given variable declaration
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(VariableDeclaration variableDeclaration) {
		return null;
	}

	/**
	 * Returns the new type binding corresponding to the given type. This is used for recovered binding
	 * only.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the given type
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(Type type) {
		return null;
	}

	/**
	 * Returns the new type binding corresponding to the given recovered type binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param recoveredTypeBinding the recovered type binding
	 * @param dimensions the dimensions to add the to given type binding dimensions
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(RecoveredTypeBinding recoveredTypeBinding, int dimensions) {
		return null;
	}

	/**
	 * Returns the new variable binding corresponding to the given old variable binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param binding the old variable binding
	 * @return the new variable binding
	 */
	IVariableBinding getVariableBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding binding) {
		return null;
	}


	/**
	 * Return the working copy owner for the receiver.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 * @return the working copy owner for the receiver
	 */
	public WorkingCopyOwner getWorkingCopyOwner() {
		return null;
	}

	boolean isResolvedTypeInferredFromExpectedType(FunctionInvocation methodInvocation) {
		return false;
	}

	boolean isResolvedTypeInferredFromExpectedType(SuperMethodInvocation methodInvocation) {
		return false;
	}

	/**
	 * Returns the validator lookup environment used by this binding resolver.
	 * Returns <code>null</code> if none.
	 *
	 * @return the lookup environment used by this resolver, or <code>null</code> if none.
	 */
	LookupEnvironment lookupEnvironment() {
		return null;
	}

	/**
	 * This method is used to record the scope and its corresponding node.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 * @param astNode
	 */
	void recordScope(ASTNode astNode, BlockScope blockScope) {
		// default implementation: do nothing
	}

	/**
	 * Returns whether this expression node is the site of a boxing
	 * conversion (JLS3 5.1.7). This information is available only
	 * when bindings are requested when the AST is being built.
	 *
	 * @return <code>true</code> if this expression is the site of a
	 * boxing conversion, or <code>false</code> if either no boxing conversion
	 * is involved or if bindings were not requested when the AST was created
	 */
	boolean resolveBoxing(Expression expression) {
		return false;
	}

	/**
	 * Returns whether this expression node is the site of an unboxing
	 * conversion (JLS3 5.1.8). This information is available only
	 * when bindings are requested when the AST is being built.
	 *
	 * @return <code>true</code> if this expression is the site of an
	 * unboxing conversion, or <code>false</code> if either no unboxing
	 * conversion is involved or if bindings were not requested when the
	 * AST was created
	 */
	boolean resolveUnboxing(Expression expression) {
		return false;
	}

	/**
	 * Resolves and returns the compile-time constant expression value as
	 * specified in JLS2 15.28, if this expression has one. Constant expression
	 * values are unavailable unless bindings are requested when the AST is
	 * being built. If the type of the value is a primitive type, the result
	 * is the boxed equivalent (i.e., int returned as an <code>Integer</code>);
	 * if the type of the value is <code>String</code>, the result is the string
	 * itself. If the expression does not have a compile-time constant expression
	 * value, the result is <code>null</code>.
	 * <p>
	 * Resolving constant expressions takes into account the value of simple
	 * and qualified names that refer to constant variables (JLS2 4.12.4).
	 * </p>
	 * <p>
	 * Note 1: enum constants are not considered constant expressions either.
	 * The result is always <code>null</code> for these.
	 * </p>
	 * <p>
	 * Note 2: Compile-time constant expressions cannot denote <code>null</code>.
	 * So technically {@link NullLiteral} nodes are not constant expressions.
	 * The result is <code>null</code> for these nonetheless.
	 * </p>
	 *
	 * @return the constant expression value, or <code>null</code> if this
	 * expression has no constant expression value or if bindings were not
	 * requested when the AST was created
	 */
	Object resolveConstantExpressionValue(Expression expression) {
		return null;
	}

	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>ClassInstanceCreation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IFunctionBinding resolveConstructor(ClassInstanceCreation expression) {
		return null;
	}

	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>ConstructorInvocation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IFunctionBinding resolveConstructor(ConstructorInvocation expression) {
		return null;
	}

	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>SuperConstructorInvocation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IFunctionBinding resolveConstructor(SuperConstructorInvocation expression) {
		return null;
	}
	/**
	 * Resolves the type of the given expression and returns the type binding
	 * for it.
	 * <p>
	 * The implementation of <code>Expression.resolveTypeBinding</code>
	 * forwards to this method. The result is often a function of the context
	 * in which the expression node is embedded as well as the expression
	 * subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression whose type is of interest
	 * @return the binding for the type of the given expression, or
	 *    <code>null</code> if no binding is available
	 */
	ITypeBinding resolveExpressionType(Expression expression) {
		return null;
	}

	/**
	 * Resolves the given field access and returns the binding for it.
	 * <p>
	 * The implementation of <code>FieldAccess.resolveFieldBinding</code>
	 * forwards to this method. How the field resolves is often a function of
	 * the context in which the field access node is embedded as well as
	 * the field access subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param fieldAccess the field access of interest
	 * @return the binding for the given field access, or
	 *    <code>null</code> if no binding is available
	 */
	IVariableBinding resolveField(FieldAccess fieldAccess) {
		return null;
	}

	/**
	 * Resolves the given super field access and returns the binding for it.
	 * <p>
	 * The implementation of <code>SuperFieldAccess.resolveFieldBinding</code>
	 * forwards to this method. How the field resolves is often a function of
	 * the context in which the super field access node is embedded as well as
	 * the super field access subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param fieldAccess the super field access of interest
	 * @return the binding for the given field access, or
	 *    <code>null</code> if no binding is available
	 */
	IVariableBinding resolveField(SuperFieldAccess fieldAccess) {
		return null;
	}

	/**
	 * Resolves the given import declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>ImportDeclaration.resolveBinding</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param importDeclaration the import declaration of interest
	 * @return the binding for the given package declaration, or
	 *         the package binding (for on-demand imports) or type binding
	 *         (for single-type imports), or <code>null</code> if no binding is
	 *         available
	 */
	IBinding resolveImport(ImportDeclaration importDeclaration) {
		return null;
	}

	/**
	 * Resolves the given method declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>FunctionDeclaration.resolveBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method declaration node is embedded as well as
	 * the method declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param method the method or constructor declaration of interest
	 * @return the binding for the given method declaration, or
	 *    <code>null</code> if no binding is available
	 */
	IFunctionBinding resolveMethod(FunctionDeclaration method) {
		return null;
	}

	/**
	 * Resolves the given method invocation and returns the binding for it.
	 * <p>
	 * The implementation of <code>FunctionInvocation.resolveMethodBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method invocation node is embedded as well as
	 * the method invocation subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param method the method invocation of interest
	 * @return the binding for the given method invocation, or
	 *    <code>null</code> if no binding is available
	 */
	IFunctionBinding resolveMethod(FunctionInvocation method) {
		return null;
	}

	/**
	 * Resolves the given method invocation and returns the binding for it.
	 * <p>
	 * The implementation of <code>FunctionInvocation.resolveMethodBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method invocation node is embedded as well as
	 * the method invocation subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param method the method invocation of interest
	 * @return the binding for the given method invocation, or
	 *    <code>null</code> if no binding is available
	 */
	IFunctionBinding resolveMethod(SuperMethodInvocation method) {
		return null;
	}

	/**
	 * Resolves the given name and returns the type binding for it.
	 * <p>
	 * The implementation of <code>Name.resolveBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param name the name of interest
	 * @return the binding for the name, or <code>null</code> if no binding is
	 *    available
	 */
	IBinding resolveName(Name name) {
		return null;
	}

	/**
	 * Resolves the given package declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>PackageDeclaration.resolveBinding</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param pkg the package declaration of interest
	 * @return the binding for the given package declaration, or
	 *    <code>null</code> if no binding is available
	 */
	IPackageBinding resolvePackage(PackageDeclaration pkg) {
		return null;
	}

	/**
	 * Resolves the given reference and returns the binding for it.
	 * <p>
	 * The implementation of <code>MemberRef.resolveBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param ref the reference of interest
	 * @return the binding for the reference, or <code>null</code> if no binding is
	 *    available
	 */
	IBinding resolveReference(MemberRef ref) {
		return null;
	}

	/**
	 * Resolves the given reference and returns the binding for it.
	 * <p>
	 * The implementation of <code>FunctionRef.resolveBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param ref the reference of interest
	 * @return the binding for the reference, or <code>null</code> if no binding is
	 *    available
	 */
	IBinding resolveReference(FunctionRef ref) {
		return null;
	}

	/**
	 * Resolves the given anonymous class declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>AnonymousClassDeclaration.resolveBinding</code>
	 * forwards to this method. How the declaration resolves is often a
	 * function of the context in which the declaration node is embedded as well
	 * as the declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the anonymous class declaration of interest
	 * @return the binding for the given class declaration, or <code>null</code>
	 *    if no binding is available
	 */
	ITypeBinding resolveType(AnonymousClassDeclaration type) {
		return null;
	}

	/**
	 * Resolves the given type and returns the type binding for it.
	 * <p>
	 * The implementation of <code>Type.resolveBinding</code>
	 * forwards to this method. How the type resolves is often a function
	 * of the context in which the type node is embedded as well as the type
	 * subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the type of interest
	 * @return the binding for the given type, or <code>null</code>
	 *    if no binding is available
	 */
	ITypeBinding resolveType(Type type) {
		return null;
	}

	/**
	 * Resolves the given class or interface declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>TypeDeclaration.resolveBinding</code>
	 * (and <code>TypeDeclarationStatement.resolveBinding</code>) forwards
	 * to this method. How the type declaration resolves is often a function of
	 * the context in which the type declaration node is embedded as well as the
	 * type declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the class or interface declaration of interest
	 * @return the binding for the given type declaration, or <code>null</code>
	 *    if no binding is available
	 */
	ITypeBinding resolveType(TypeDeclaration type) {
		return null;
	}


	ITypeBinding resolveType(JavaScriptUnit compilationUnit) {
		return null;
	}

	/**
	 * Resolves the given variable declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>VariableDeclaration.resolveBinding</code>
	 * forwards to this method. How the variable declaration resolves is often
	 * a function of the context in which the variable declaration node is
	 * embedded as well as the variable declaration subtree itself. VariableDeclaration
	 * declarations used as local variable, formal parameter and exception
	 * variables resolve to local variable bindings; variable declarations
	 * used to declare fields resolve to field bindings.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param variable the variable declaration of interest
	 * @return the binding for the given variable declaration, or
	 *    <code>null</code> if no binding is available
	 */
	IVariableBinding resolveVariable(VariableDeclaration variable) {
		return null;
	}

	IVariableBinding resolveVariable(VariableDeclarationStatement variable) {
		return null;
	}

	/**
	 * Resolves the given well known type by name and returns the type binding
	 * for it.
	 * <p>
	 * The implementation of <code>AST.resolveWellKnownType</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param name the name of a well known type
	 * @return the corresponding type binding, or <code>null<code> if the
	 *   named type is not considered well known or if no binding can be found
	 *   for it
	 */
	ITypeBinding resolveWellKnownType(String name) {
		return null;
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
		return null;
	}

	/**
	 * Returns the javaScript unit scope used by this binding resolver.
	 * Returns <code>null</code> if none.
	 *
	 * @return the javaScript unit scope by this resolver, or <code>null</code> if none.
	 */
	public CompilationUnitScope scope() {
		return null;
	}

	/**
	 * Allows the user to store information about the given old/new pair of
	 * AST nodes.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param newNode the new AST node
	 * @param oldASTNode the old AST node
	 */
	void store(ASTNode newNode, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode oldASTNode) {
		// default implementation: do nothing
	}

	/**
	 * Allows the user to update information about the given old/new pair of
	 * AST nodes.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param node the old AST node
	 * @param newNode the new AST node
	 */
	void updateKey(ASTNode node, ASTNode newNode) {
		// default implementation: do nothing
	}
}
