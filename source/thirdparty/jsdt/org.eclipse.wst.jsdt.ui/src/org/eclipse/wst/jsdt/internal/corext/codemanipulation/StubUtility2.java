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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.NamingConventions;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.IPackageBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.ui.CodeGeneration;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public final class StubUtility2 {

	public static FunctionDeclaration createConstructorStub(IJavaScriptUnit unit, ASTRewrite rewrite, ImportRewrite imports, IFunctionBinding binding, String type, int modifiers, boolean omitSuperForDefConst, boolean todo, CodeGenerationSettings settings) throws CoreException {
		AST ast= rewrite.getAST();
		FunctionDeclaration decl= ast.newFunctionDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE));
		decl.setName(ast.newSimpleName(type));
		decl.setConstructor(true);

		List parameters= createParameters(unit, imports, ast, binding, decl, null);

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);
		String bodyStatement= ""; //$NON-NLS-1$
		if (!omitSuperForDefConst || !parameters.isEmpty()) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			bodyStatement= ASTNodes.asFormattedString(invocation, 0, delimiter, unit.getJavaScriptProject().getOptions(true));
		}

		if (todo) {
			String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), true, bodyStatement, delimiter);
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		} else {
			ASTNode statementNode= rewrite.createStringPlaceholder(bodyStatement, ASTNode.RETURN_STATEMENT);
			body.statements().add(statementNode);
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static FunctionDeclaration createConstructorStub(IJavaScriptUnit unit, ASTRewrite rewrite, ImportRewrite imports, ITypeBinding typeBinding, AST ast, IFunctionBinding superConstructor, IVariableBinding[] variableBindings, int modifiers, CodeGenerationSettings settings) throws CoreException {

		FunctionDeclaration decl= ast.newFunctionDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE));
		decl.setName(ast.newSimpleName(typeBinding.getName()));
		decl.setConstructor(true);

		List parameters= decl.parameters();
		if (superConstructor != null) {

			createParameters(unit, imports, ast, superConstructor, decl, null);
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		if (superConstructor != null) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			body.statements().add(invocation);
		}

		List prohibited= new ArrayList();
		for (final Iterator iterator= parameters.iterator(); iterator.hasNext();)
			prohibited.add(((SingleVariableDeclaration) iterator.next()).getName().getIdentifier());
		String param= null;
		List list= new ArrayList(prohibited);
		String[] excluded= null;
		for (int i= 0; i < variableBindings.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			var.setType(imports.addImport(variableBindings[i].getType(), ast));
			excluded= new String[list.size()];
			list.toArray(excluded);
			param= getParameterName(unit, variableBindings[i], excluded);
			list.add(param);
			var.setName(ast.newSimpleName(param));
			parameters.add(var);
		}

		list= new ArrayList(prohibited);
		for (int i= 0; i < variableBindings.length; i++) {
			excluded= new String[list.size()];
			list.toArray(excluded);
			final String paramName= getParameterName(unit, variableBindings[i], excluded);
			list.add(paramName);
			final String fieldName= variableBindings[i].getName();
			Expression expression= null;
			if (paramName.equals(fieldName) || settings.useKeywordThis) {
				FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(fieldName));
				expression= access;
			} else
				expression= ast.newSimpleName(fieldName);
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide(expression);
			assignment.setRightHandSide(ast.newSimpleName(paramName));
			assignment.setOperator(Assignment.Operator.ASSIGN);
			body.statements().add(ast.newExpressionStatement(assignment));
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, typeBinding.getName(), decl, superConstructor, delimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static FunctionDeclaration createDelegationStub(IJavaScriptUnit unit, ASTRewrite rewrite, ImportRewrite imports, AST ast, IBinding[] bindings, CodeGenerationSettings settings) throws CoreException {
		Assert.isNotNull(bindings);
		Assert.isNotNull(settings);
		Assert.isTrue(bindings.length == 2);
		Assert.isTrue(bindings[0] instanceof IVariableBinding);
		Assert.isTrue(bindings[1] instanceof IFunctionBinding);

		IVariableBinding variableBinding= (IVariableBinding) bindings[0];
		IFunctionBinding methodBinding= (IFunctionBinding) bindings[1];

		FunctionDeclaration decl= ast.newFunctionDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, methodBinding.getModifiers() & ~Modifier.SYNCHRONIZED & ~Modifier.ABSTRACT & ~Modifier.NATIVE));

		decl.setName(ast.newSimpleName(methodBinding.getName()));
		decl.setConstructor(false);

		decl.setReturnType2(imports.addImport(methodBinding.getReturnType(), ast));

		List parameters= decl.parameters();
		ITypeBinding[] params= methodBinding.getParameterTypes();
		String[] paramNames= StubUtility.suggestArgumentNames(unit.getJavaScriptProject(), methodBinding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration varDecl= ast.newSingleVariableDeclaration();
			
			if (methodBinding.isVarargs() && params[i].isArray() && i == params.length - 1) {
				StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType()));
				for (int dim= 1; dim < params[i].getDimensions(); dim++)
					buffer.append("[]"); //$NON-NLS-1$
				varDecl.setType(ASTNodeFactory.newType(ast, buffer.toString()));
				varDecl.setVarargs(true);
			} else
				varDecl.setType(imports.addImport(params[i], ast));
			
			varDecl.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(varDecl);
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		Statement statement= null;
		FunctionInvocation invocation= ast.newFunctionInvocation();
		invocation.setName(ast.newSimpleName(methodBinding.getName()));
		List arguments= invocation.arguments();
		for (int i= 0; i < params.length; i++)
			arguments.add(ast.newSimpleName(paramNames[i]));
		if (settings.useKeywordThis) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName(variableBinding.getName()));
			invocation.setExpression(access);
		} else
			invocation.setExpression(ast.newSimpleName(variableBinding.getName()));
		if (methodBinding.getReturnType().isPrimitive() && methodBinding.getReturnType().getName().equals("void")) {//$NON-NLS-1$
			statement= ast.newExpressionStatement(invocation);
		} else {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(invocation);
			statement= returnStatement;
		}
		body.statements().add(statement);

		ITypeBinding declaringType= variableBinding.getDeclaringClass();
		if (declaringType == null) { // can be null for
			return decl;
		}

		String qualifiedName= declaringType.getQualifiedName();
		IPackageBinding packageBinding= declaringType.getPackage();
		if (packageBinding != null) {
			if (packageBinding.getName().length() > 0 && qualifiedName.startsWith(packageBinding.getName()))
				qualifiedName= qualifiedName.substring(packageBinding.getName().length());
		}

		if (settings.createComments) {
			/*
			 * TODO: have API for delegate method comments This is an inlined
			 * version of
			 * {@link CodeGeneration#getMethodComment(IJavaScriptUnit, String, FunctionDeclaration, IFunctionBinding, String)}
			 */
			methodBinding= methodBinding.getMethodDeclaration();
			String declaringClassQualifiedName= methodBinding.getDeclaringClass().getQualifiedName();
			String linkToMethodName= methodBinding.getName();
			String[] parameterTypesQualifiedNames= StubUtility.getParameterTypeNamesForSeeTag(methodBinding);
			String string= StubUtility.getMethodComment(unit, qualifiedName, decl, methodBinding.isDeprecated(), linkToMethodName, declaringClassQualifiedName, parameterTypesQualifiedNames, true, delimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static FunctionDeclaration createImplementationStub(IJavaScriptUnit unit, ASTRewrite rewrite, ImportRewrite imports, AST ast, IFunctionBinding binding, String type, CodeGenerationSettings settings, boolean deferred, ImportRewriteContext context) throws CoreException {

		FunctionDeclaration decl= ast.newFunctionDeclaration();
		decl.modifiers().addAll(getImplementationModifiers(ast, binding, deferred));

		decl.setName(ast.newSimpleName(binding.getName()));
		decl.setConstructor(false);

		decl.setReturnType2(imports.addImport(binding.getReturnType(), ast, context));

		List parameters= createParameters(unit, imports, ast, binding, decl, context);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);
		if (!deferred) {
			Map options= unit.getJavaScriptProject().getOptions(true);

			Block body= ast.newBlock();
			decl.setBody(body);

			String bodyStatement= ""; //$NON-NLS-1$
			if (Modifier.isAbstract(binding.getModifiers())) {
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			} else {
				SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
				invocation.setName(ast.newSimpleName(binding.getName()));
				SingleVariableDeclaration varDecl= null;
				for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
					varDecl= (SingleVariableDeclaration) iterator.next();
					invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
				}
				Expression expression= invocation;
				Type returnType= decl.getReturnType2();
				if (returnType != null && (returnType instanceof PrimitiveType) && ((PrimitiveType) returnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
					bodyStatement= ASTNodes.asFormattedString(ast.newExpressionStatement(expression), 0, delimiter, options);
				} else {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			}

			String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), false, bodyStatement, delimiter);
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		}

		if (settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}

		return decl;
	}

	public static FunctionDeclaration createImplementationStub(IJavaScriptUnit unit, ASTRewrite rewrite, ImportRewrite importRewrite, IFunctionBinding binding, String type, boolean deferred, CodeGenerationSettings settings) throws CoreException {
		AST ast= rewrite.getAST();
		FunctionDeclaration decl= ast.newFunctionDeclaration();
		decl.modifiers().addAll(getImplementationModifiers(ast, binding, deferred));

		decl.setName(ast.newSimpleName(binding.getName()));
		decl.setConstructor(false);

		decl.setReturnType2(createTypeNode(importRewrite, binding.getReturnType(), ast));

		List parameters= createParameters(unit, importRewrite, ast, binding, decl);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);
		if (!deferred) {
			Map options= unit.getJavaScriptProject().getOptions(true);
			
			Block body= ast.newBlock();
			decl.setBody(body);

			String bodyStatement= ""; //$NON-NLS-1$
			if (Modifier.isAbstract(binding.getModifiers())) {
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			} else {
				SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
				invocation.setName(ast.newSimpleName(binding.getName()));
				SingleVariableDeclaration varDecl= null;
				for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
					varDecl= (SingleVariableDeclaration) iterator.next();
					invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
				}
				Expression expression= invocation;
				Type returnType= decl.getReturnType2();
				if (returnType instanceof PrimitiveType && ((PrimitiveType) returnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
					bodyStatement= ASTNodes.asFormattedString(ast.newExpressionStatement(expression), 0, delimiter, options);
				} else {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			}

			String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), false, bodyStatement, delimiter);
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	private static List createParameters(IJavaScriptUnit unit, ImportRewrite imports, AST ast, IFunctionBinding binding, FunctionDeclaration decl, ImportRewriteContext context) {
		List parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		String[] paramNames= StubUtility.suggestArgumentNames(unit.getJavaScriptProject(), binding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			if (binding.isVarargs() && params[i].isArray() && i == params.length - 1) {
				StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType(), context));
				for (int dim= 1; dim < params[i].getDimensions(); dim++)
					buffer.append("[]"); //$NON-NLS-1$
				var.setType(ASTNodeFactory.newType(ast, buffer.toString()));
				var.setVarargs(true);
			} else
				var.setType(imports.addImport(params[i], ast, context));
			var.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(var);
		}
		return parameters;
	}

	private static List createParameters(IJavaScriptUnit unit, ImportRewrite imports, AST ast, IFunctionBinding binding, FunctionDeclaration decl) {
		List parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		String[] paramNames= StubUtility.suggestArgumentNames(unit.getJavaScriptProject(), binding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			if (binding.isVarargs() && params[i].isArray() && i == params.length - 1) {
				final ITypeBinding elementType= params[i].getElementType();
				StringBuffer buffer= new StringBuffer(imports != null ? imports.addImport(elementType) : elementType.getQualifiedName());
				for (int dim= 1; dim < params[i].getDimensions(); dim++)
					buffer.append("[]"); //$NON-NLS-1$
				var.setType(ASTNodeFactory.newType(ast, buffer.toString()));
				var.setVarargs(true);
			} else
				var.setType(createTypeNode(imports, params[i], ast));
			var.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(var);
		}
		return parameters;
	}

	private static Type createTypeNode(ImportRewrite importRewrite, ITypeBinding binding, AST ast) {
		if (importRewrite != null)
			return importRewrite.addImport(binding, ast);
		return createTypeNode(binding, ast);
	}

	private static Type createTypeNode(ITypeBinding binding, AST ast) {
		if (binding.isPrimitive())
			return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
		ITypeBinding normalized= Bindings.normalizeTypeBinding(binding);
		if (normalized == null)
			return ast.newSimpleType(ast.newSimpleName("invalid")); //$NON-NLS-1$
		else if (normalized.isArray())
			return ast.newArrayType(createTypeNode(normalized.getElementType(), ast), normalized.getDimensions());
		String qualified= Bindings.getRawQualifiedName(normalized);
		if (qualified.length() > 0) {
			return ast.newSimpleType(ASTNodeFactory.newName(ast, qualified));
		}
		return ast.newSimpleType(ASTNodeFactory.newName(ast, Bindings.getRawName(normalized)));
	}

	private static IFunctionBinding findMethodBinding(IFunctionBinding method, List allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IFunctionBinding curr= (IFunctionBinding) allMethods.get(i);
			if (Bindings.isSubsignature(method, curr)) {
				return curr;
			}
		}
		return null;
	}

	private static IFunctionBinding findOverridingMethod(IFunctionBinding method, List allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IFunctionBinding curr= (IFunctionBinding) allMethods.get(i);
			if (Bindings.areOverriddenMethods(curr, method) || Bindings.isSubsignature(curr, method))
				return curr;
		}
		return null;
	}

	public static IBinding[][] getDelegatableMethods(AST ast, ITypeBinding binding) {
		final List tuples= new ArrayList();
		final List declared= new ArrayList();
		IFunctionBinding[] typeMethods= binding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++)
			declared.add(typeMethods[index]);
		IVariableBinding[] typeFields= binding.getDeclaredFields();
		for (int index= 0; index < typeFields.length; index++) {
			IVariableBinding fieldBinding= typeFields[index];
			if (fieldBinding.isField())
				getDelegatableMethods(ast, tuples, new ArrayList(declared), fieldBinding, fieldBinding.getType(), binding);
		}
		// list of tuple<IVariableBinding, IFunctionBinding>
		return (IBinding[][]) tuples.toArray(new IBinding[tuples.size()][2]);
	}

	private static void getDelegatableMethods(AST ast, List tuples, List methods, IVariableBinding fieldBinding, ITypeBinding typeBinding, ITypeBinding binding) {
		boolean match= false;
		IFunctionBinding[] candidates= getDelegateCandidates(typeBinding, binding);
		for (int index= 0; index < candidates.length; index++) {
			match= false;
			final IFunctionBinding methodBinding= candidates[index];
			for (int offset= 0; offset < methods.size() && !match; offset++) {
				if (Bindings.areOverriddenMethods((IFunctionBinding) methods.get(offset), methodBinding))
					match= true;
			}
			if (!match) {
				tuples.add(new IBinding[] { fieldBinding, methodBinding });
				methods.add(methodBinding);
			}
		}
		final ITypeBinding superclass= typeBinding.getSuperclass();
		if (superclass != null)
			getDelegatableMethods(ast, tuples, methods, fieldBinding, superclass, binding);
	}

	private static IFunctionBinding[] getDelegateCandidates(ITypeBinding binding, ITypeBinding hierarchy) {
		List allMethods= new ArrayList();
		IFunctionBinding[] typeMethods= binding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++) {
			final int modifiers= typeMethods[index].getModifiers();
			if (!typeMethods[index].isConstructor() && !Modifier.isStatic(modifiers) && (Modifier.isPublic(modifiers))) {
//				IFunctionBinding result= Bindings.findOverriddenMethodInHierarchy(hierarchy, typeMethods[index]);
//				ITypeBinding[] parameterBindings= typeMethods[index].getParameterTypes();
				boolean upper= false;
				if (!upper)
					allMethods.add(typeMethods[index]);
			}
		}
		return (IFunctionBinding[]) allMethods.toArray(new IFunctionBinding[allMethods.size()]);
	}

	private static List getImplementationModifiers(AST ast, IFunctionBinding method, boolean deferred) {
		int modifiers= method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.PRIVATE;
		if (deferred) {
			modifiers= modifiers & ~Modifier.PROTECTED;
			modifiers= modifiers | Modifier.PUBLIC;
		}
		return ASTNodeFactory.newModifiers(ast, modifiers);
	}

	public static IFunctionBinding[] getOverridableMethods(AST ast, ITypeBinding typeBinding, boolean isSubType) {
		List allMethods= new ArrayList();
		IFunctionBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++) {
			final int modifiers= typeMethods[index].getModifiers();
			if (!typeMethods[index].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers))
				allMethods.add(typeMethods[index]);
		}
		ITypeBinding clazz= typeBinding.getSuperclass();
		while (clazz != null) {
			IFunctionBinding[] methods= clazz.getDeclaredMethods();
			for (int offset= 0; offset < methods.length; offset++) {
				final int modifiers= methods[offset].getModifiers();
				if (!methods[offset].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
					if (findOverridingMethod(methods[offset], allMethods) == null)
						allMethods.add(methods[offset]);
				}
			}
			clazz= clazz.getSuperclass();
		}
		clazz= typeBinding;
		while (clazz != null) {
			clazz= clazz.getSuperclass();
		}
		if (!isSubType)
			allMethods.removeAll(Arrays.asList(typeMethods));
		int modifiers= 0;
		
		for (int index= allMethods.size() - 1; index >= 0; index--) {
			IFunctionBinding method= (IFunctionBinding) allMethods.get(index);
			modifiers= method.getModifiers();
			if (Modifier.isFinal(modifiers))
				allMethods.remove(index);
		}
		
		return (IFunctionBinding[]) allMethods.toArray(new IFunctionBinding[allMethods.size()]);
	}

//	private static void getOverridableMethods(AST ast, ITypeBinding superBinding, List allMethods) {
//		IFunctionBinding[] methods= superBinding.getDeclaredMethods();
//		for (int offset= 0; offset < methods.length; offset++) {
//			final int modifiers= methods[offset].getModifiers();
//			if (!methods[offset].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
//				if (findOverridingMethod(methods[offset], allMethods) == null && !Modifier.isStatic(modifiers))
//					allMethods.add(methods[offset]);
//			}
//		}
//	}

	private static String getParameterName(IJavaScriptUnit unit, IVariableBinding binding, String[] excluded) {
		final String name= NamingConventions.removePrefixAndSuffixForFieldName(unit.getJavaScriptProject(), binding.getName(), binding.getModifiers());
		return StubUtility.suggestArgumentName(unit.getJavaScriptProject(), name, excluded);
	}

	public static IFunctionBinding[] getUnimplementedMethods(ITypeBinding typeBinding) {
		ArrayList allMethods= new ArrayList();
		ArrayList toImplement= new ArrayList();

		IFunctionBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (int i= 0; i < typeMethods.length; i++) {
			IFunctionBinding curr= typeMethods[i];
			int modifiers= curr.getModifiers();
			if (!curr.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
				allMethods.add(curr);
			}
		}

		ITypeBinding superClass= typeBinding.getSuperclass();
		while (superClass != null) {
			typeMethods= superClass.getDeclaredMethods();
			for (int i= 0; i < typeMethods.length; i++) {
				IFunctionBinding curr= typeMethods[i];
				int modifiers= curr.getModifiers();
				if (!curr.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
					if (findMethodBinding(curr, allMethods) == null) {
						allMethods.add(curr);
					}
				}
			}
			superClass= superClass.getSuperclass();
		}

		for (int i= 0; i < allMethods.size(); i++) {
			IFunctionBinding curr= (IFunctionBinding) allMethods.get(i);
			int modifiers= curr.getModifiers();
			if ((Modifier.isAbstract(modifiers)) && (typeBinding != curr.getDeclaringClass())) {
				// implement all abstract methods
				toImplement.add(curr);
			}
		}

		ITypeBinding curr= typeBinding;
		while (curr != null) {
			curr= curr.getSuperclass();
		}

		return (IFunctionBinding[]) toImplement.toArray(new IFunctionBinding[toImplement.size()]);
	}

	public static IFunctionBinding[] getVisibleConstructors(ITypeBinding binding, boolean accountExisting, boolean proposeDefault) {
		List constructorMethods= new ArrayList();
		List existingConstructors= null;
		ITypeBinding superType= binding.getSuperclass();
		if (superType == null)
			return new IFunctionBinding[0];
		if (accountExisting) {
			IFunctionBinding[] methods= binding.getDeclaredMethods();
			existingConstructors= new ArrayList(methods.length);
			for (int index= 0; index < methods.length; index++) {
				IFunctionBinding method= methods[index];
				if (method.isConstructor() && !method.isDefaultConstructor())
					existingConstructors.add(method);
			}
		}
		if (existingConstructors != null)
			constructorMethods.addAll(existingConstructors);
		IFunctionBinding[] methods= binding.getDeclaredMethods();
		IFunctionBinding[] superMethods= superType.getDeclaredMethods();
		for (int index= 0; index < superMethods.length; index++) {
			IFunctionBinding method= superMethods[index];
			if (method.isConstructor()) {
				if (Bindings.isVisibleInHierarchy(method, binding.getPackage()) && (!accountExisting || !Bindings.containsSignatureEquivalentConstructor(methods, method)))
					constructorMethods.add(method);
			}
		}
		if (existingConstructors != null)
			constructorMethods.removeAll(existingConstructors);
		if (constructorMethods.isEmpty()) {
			superType= binding;
			while (superType.getSuperclass() != null)
				superType= superType.getSuperclass();
			IFunctionBinding method= Bindings.findMethodInType(superType, "Object", new ITypeBinding[0]); //$NON-NLS-1$
			if (method != null) {
				if ((proposeDefault || (!accountExisting || (existingConstructors == null || existingConstructors.isEmpty()))) && (!accountExisting || !Bindings.containsSignatureEquivalentConstructor(methods, method)))
					constructorMethods.add(method);
			}
		}
		return (IFunctionBinding[]) constructorMethods.toArray(new IFunctionBinding[constructorMethods.size()]);
	}



	/**
	 * Creates a new stub utility.
	 */
	private StubUtility2() {
		// Not for instantiation
	}
}
