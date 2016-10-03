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
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Comment;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;

/**
 * Type constraints creator to determine the necessary constraints to replace type occurrences by a given super type.
 * 
 * 
 */
public final class SuperTypeConstraintsCreator extends HierarchicalASTVisitor {

	/** The constraint variable property */
	private static final String PROPERTY_CONSTRAINT_VARIABLE= "cv"; //$NON-NLS-1$

	/**
	 * Returns the original methods of the method hierarchy of the specified method.
	 * 
	 * @param binding the method binding
	 * @param type the current type
	 * @param originals the original methods which have already been found (element type: <code>IFunctionBinding</code>)
	 * @param implementations <code>true</code> to favor implementation methods, <code>false</code> otherwise
	 */
	private static void getOriginalMethods(final IFunctionBinding binding, final ITypeBinding type, final Collection originals, final boolean implementations) {
		final ITypeBinding ancestor= type.getSuperclass();
		if (!implementations) {
			if (ancestor != null)
				getOriginalMethods(binding, ancestor, originals, implementations);
		}
		if (implementations && ancestor != null)
			getOriginalMethods(binding, ancestor, originals, implementations);
		final IFunctionBinding[] methods= type.getDeclaredMethods();
		IFunctionBinding method= null;
		for (int index= 0; index < methods.length; index++) {
			method= methods[index];
			if (!binding.getKey().equals(method.getKey())) {
				boolean match= false;
				IFunctionBinding current= null;
				for (final Iterator iterator= originals.iterator(); iterator.hasNext();) {
					current= (IFunctionBinding) iterator.next();
					if (Bindings.areOverriddenMethods(method, current))
						match= true;
				}
				if (!match && Bindings.areOverriddenMethods(binding, method))
					originals.add(method);
			}
		}
	}

	/** The current method declarations being processed (element type: <code>FunctionDeclaration</code>) */
	private final Stack fCurrentMethods= new Stack();

	/** Should instanceof expressions be rewritten? */
	private final boolean fInstanceOf;

	/** The type constraint model to solve */
	private final SuperTypeConstraintsModel fModel;

	/**
	 * Creates a new super type constraints creator.
	 * 
	 * @param model the model to create the type constraints for
	 * @param instanceofs <code>true</code> to rewrite instanceof expressions, <code>false</code> otherwise
	 */
	public SuperTypeConstraintsCreator(final SuperTypeConstraintsModel model, final boolean instanceofs) {
		Assert.isNotNull(model);

		fModel= model;
		fInstanceOf= instanceofs;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ArrayAccess)
	 */
	public final void endVisit(final ArrayAccess node) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, node.getArray().getProperty(PROPERTY_CONSTRAINT_VARIABLE));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ArrayCreation)
	 */
	public final void endVisit(final ArrayCreation node) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) node.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
		final ArrayInitializer initializer= node.getInitializer();
		if (initializer != null) {
			final ConstraintVariable2 descendant= (ConstraintVariable2) initializer.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			if (descendant != null)
				fModel.createSubtypeConstraint(descendant, ancestor);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ArrayInitializer)
	 */
	public final void endVisit(final ArrayInitializer node) {
		final ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null && binding.isArray()) {
			final ConstraintVariable2 ancestor= fModel.createIndependentTypeVariable(binding.getElementType());
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
			Expression expression= null;
			ConstraintVariable2 descendant= null;
			final List expressions= node.expressions();
			for (int index= 0; index < expressions.size(); index++) {
				expression= (Expression) expressions.get(index);
				descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ArrayType)
	 */
	public final void endVisit(final ArrayType node) {
		ArrayType array= null;
		Type component= node.getComponentType();
		while (component instanceof ArrayType) {
			array= (ArrayType) component;
			component= array.getComponentType();
		}
		final ConstraintVariable2 variable= fModel.createTypeVariable(component);
		if (variable != null) {
			component.setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.Assignment)
	 */
	public final void endVisit(final Assignment node) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) node.getLeftHandSide().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 descendant= (ConstraintVariable2) node.getRightHandSide().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
		if (ancestor != null && descendant != null)
			fModel.createSubtypeConstraint(descendant, ancestor);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.CatchClause)
	 */
	public final void endVisit(final CatchClause node) {
		final SingleVariableDeclaration declaration= node.getException();
		if (declaration != null) {
			final ConstraintVariable2 descendant= (ConstraintVariable2) declaration.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			if (descendant != null) {
				final ITypeBinding binding= node.getAST().resolveWellKnownType("java.lang.Throwable"); //$NON-NLS-1$
				if (binding != null) {
					final ConstraintVariable2 ancestor= fModel.createImmutableTypeVariable(binding);
					if (ancestor != null)
						fModel.createSubtypeConstraint(descendant, ancestor);
				}
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation)
	 */
	public final void endVisit(final ClassInstanceCreation node) {
		final IFunctionBinding binding= node.resolveConstructorBinding();
		if (binding != null) {
			endVisit(node.arguments(), binding);
			ConstraintVariable2 variable= null;
			final AnonymousClassDeclaration declaration= node.getAnonymousClassDeclaration();
			if (declaration != null) {
				final ITypeBinding type= declaration.resolveBinding();
				if (type != null)
					variable= fModel.createImmutableTypeVariable(type);
			} else {
				final ITypeBinding type= node.resolveTypeBinding();
				if (type != null)
					variable= fModel.createImmutableTypeVariable(type);
			}
			if (variable != null)
				node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ConditionalExpression)
	 */
	public final void endVisit(final ConditionalExpression node) {
		ConstraintVariable2 thenVariable= null;
		ConstraintVariable2 elseVariable= null;
		final Expression thenExpression= node.getThenExpression();
		if (thenExpression != null)
			thenVariable= (ConstraintVariable2) thenExpression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final Expression elseExpression= node.getElseExpression();
		if (elseExpression != null)
			elseVariable= (ConstraintVariable2) elseExpression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null) {
			if (binding.isArray())
				binding= binding.getElementType();
			final ConstraintVariable2 ancestor= fModel.createIndependentTypeVariable(binding);
			if (ancestor != null) {
				node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
				if (thenVariable != null)
					fModel.createSubtypeConstraint(thenVariable, ancestor);
				if (elseVariable != null)
					fModel.createSubtypeConstraint(elseVariable, ancestor);
				if (thenVariable != null && elseVariable != null)
					fModel.createConditionalTypeConstraint(ancestor, thenVariable, elseVariable);
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ConstructorInvocation)
	 */
	public final void endVisit(final ConstructorInvocation node) {
		final IFunctionBinding binding= node.resolveConstructorBinding();
		if (binding != null)
			endVisit(node.arguments(), binding);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.FieldAccess)
	 */
	public final void endVisit(final FieldAccess node) {
		final IVariableBinding binding= node.resolveFieldBinding();
		if (binding != null)
			endVisit(binding, node.getExpression(), node);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.FieldDeclaration)
	 */
	public final void endVisit(final FieldDeclaration node) {
		endVisit(node.fragments(), node.getType(), node);
	}

	/**
	 * End of visit the specified method declaration.
	 * 
	 * @param binding the method binding
	 */
	private void endVisit(final IFunctionBinding binding) {
		IFunctionBinding method= null;
		ConstraintVariable2 ancestor= null;
		final ConstraintVariable2 descendant= fModel.createReturnTypeVariable(binding);
		if (descendant != null) {
			final Collection originals= getOriginalMethods(binding);
			for (final Iterator iterator= originals.iterator(); iterator.hasNext();) {
				method= (IFunctionBinding) iterator.next();
				if (!method.getKey().equals(binding.getKey())) {
					ancestor= fModel.createReturnTypeVariable(method);
					if (ancestor != null)
						fModel.createCovariantTypeConstraint(descendant, ancestor);
				}
			}
		}
	}

	/**
	 * End of visit the specified method invocation.
	 * 
	 * @param binding the method binding
	 * @param descendant the constraint variable of the invocation expression
	 */
	private void endVisit(final IFunctionBinding binding, final ConstraintVariable2 descendant) {
		ITypeBinding declaring= null;
		IFunctionBinding method= null;
		final Collection originals= getOriginalMethods(binding);
		for (final Iterator iterator= originals.iterator(); iterator.hasNext();) {
			method= (IFunctionBinding) iterator.next();
			declaring= method.getDeclaringClass();
			if (declaring != null) {
				final ConstraintVariable2 ancestor= fModel.createDeclaringTypeVariable(declaring);
				if (ancestor != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
			}
		}
	}

	/**
	 * End of visit the thrown exception
	 * 
	 * @param binding the type binding of the thrown exception
	 * @param node the exception name node
	 */
	private void endVisit(final ITypeBinding binding, final Name node) {
		final ConstraintVariable2 variable= fModel.createExceptionVariable(node);
		if (variable != null)
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
	}

	/**
	 * End of visit the field access.
	 * 
	 * @param binding the variable binding
	 * @param qualifier the qualifier expression, or <code>null</code>
	 * @param access the access expression
	 */
	private void endVisit(final IVariableBinding binding, final Expression qualifier, final Expression access) {
		access.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createVariableVariable(binding));
		if (qualifier != null) {
			final ITypeBinding type= binding.getDeclaringClass();
			if (type != null) {
				// array.length does not have a declaring class
				final ConstraintVariable2 ancestor= fModel.createDeclaringTypeVariable(type);
				if (ancestor != null) {
					final ConstraintVariable2 descendant= (ConstraintVariable2) qualifier.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
					if (descendant != null)
						fModel.createSubtypeConstraint(descendant, ancestor);
				}
			}
		}
	}

	/**
	 * End of visit the method argument list.
	 * 
	 * @param arguments the arguments (element type: <code>Expression</code>)
	 * @param binding the method binding
	 */
	private void endVisit(final List arguments, final IFunctionBinding binding) {
		Expression expression= null;
		ConstraintVariable2 ancestor= null;
		ConstraintVariable2 descendant= null;
		for (int index= 0; index < arguments.size(); index++) {
			expression= (Expression) arguments.get(index);
			descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			ancestor= fModel.createMethodParameterVariable(binding, index);
			if (ancestor != null && descendant != null)
				fModel.createSubtypeConstraint(descendant, ancestor);
		}
	}

	/**
	 * End of visit the variable declaration fragment list.
	 * 
	 * @param fragments the fragments (element type: <code>VariableDeclarationFragment</code>)
	 * @param type the type of the fragments
	 * @param parent the parent of the fragment list
	 */
	private void endVisit(final List fragments, final Type type, final ASTNode parent) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) type.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (ancestor != null) {
			IVariableBinding binding= null;
			ConstraintVariable2 descendant= null;
			VariableDeclarationFragment fragment= null;
			for (int index= 0; index < fragments.size(); index++) {
				fragment= (VariableDeclarationFragment) fragments.get(index);
				descendant= (ConstraintVariable2) fragment.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
				binding= fragment.resolveBinding();
				if (binding != null) {
					descendant= fModel.createVariableVariable(binding);
					if (descendant != null)
						fModel.createEqualityConstraint(ancestor, descendant);
				}
			}
			parent.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.FunctionDeclaration)
	 */
	public final void endVisit(final FunctionDeclaration node) {
		fCurrentMethods.pop();
		final IFunctionBinding binding= node.resolveBinding();
		if (binding != null) {
			if (!binding.isConstructor()) {
				final Type type= node.getReturnType2();
				if (type != null) {
					final ConstraintVariable2 first= fModel.createReturnTypeVariable(binding);
					final ConstraintVariable2 second= (ConstraintVariable2) type.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
					if (first != null) {
						if (second != null)
							fModel.createEqualityConstraint(first, second);
						endVisit(binding);
					}
				}
			}
			ConstraintVariable2 ancestor= null;
			ConstraintVariable2 descendant= null;
			IVariableBinding variable= null;
			final List parameters= node.parameters();
			if (!parameters.isEmpty()) {
				final Collection originals= getOriginalMethods(binding);
				SingleVariableDeclaration declaration= null;
				for (int index= 0; index < parameters.size(); index++) {
					declaration= (SingleVariableDeclaration) parameters.get(index);
					ancestor= fModel.createMethodParameterVariable(binding, index);
					if (ancestor != null) {
						descendant= (ConstraintVariable2) declaration.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
						if (descendant != null)
							fModel.createEqualityConstraint(descendant, ancestor);
						variable= declaration.resolveBinding();
						if (variable != null) {
							descendant= fModel.createVariableVariable(variable);
							if (descendant != null)
								fModel.createEqualityConstraint(ancestor, descendant);
						}
						IFunctionBinding method= null;
						for (final Iterator iterator= originals.iterator(); iterator.hasNext();) {
							method= (IFunctionBinding) iterator.next();
							if (!method.getKey().equals(binding.getKey())) {
								descendant= fModel.createMethodParameterVariable(method, index);
								if (descendant != null)
									fModel.createEqualityConstraint(ancestor, descendant);
							}
						}
					}
				}
			}
			final List exceptions= node.thrownExceptions();
			if (!exceptions.isEmpty()) {
				final ITypeBinding throwable= node.getAST().resolveWellKnownType("java.lang.Throwable"); //$NON-NLS-1$
				if (throwable != null) {
					ancestor= fModel.createImmutableTypeVariable(throwable);
					if (ancestor != null) {
						Name exception= null;
						for (int index= 0; index < exceptions.size(); index++) {
							exception= (Name) exceptions.get(index);
							descendant= (ConstraintVariable2) exception.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
							if (descendant != null)
								fModel.createSubtypeConstraint(descendant, ancestor);
						}
					}
				}
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.FunctionInvocation)
	 */
	public final void endVisit(final FunctionInvocation node) {
		final IFunctionBinding binding= node.resolveMethodBinding();
		if (binding != null) {
			endVisit(node, binding);
			endVisit(node.arguments(), binding);
			final Expression expression= node.getExpression();
			if (expression != null) {
				final ConstraintVariable2 descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					endVisit(binding, descendant);
			}
		}
	}

	/**
	 * End of visit the return type of a method invocation.
	 * 
	 * @param invocation the method invocation
	 * @param binding the method binding
	 */
	private void endVisit(final FunctionInvocation invocation, final IFunctionBinding binding) {
		if (!binding.isConstructor()) {
			final ConstraintVariable2 variable= fModel.createReturnTypeVariable(binding);
			if (variable != null)
				invocation.setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.NullLiteral)
	 */
	public final void endVisit(final NullLiteral node) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createImmutableTypeVariable(node.resolveTypeBinding()));
	}
	public final void endVisit(final UndefinedLiteral node) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createImmutableTypeVariable(node.resolveTypeBinding()));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression)
	 */
	public final void endVisit(final ParenthesizedExpression node) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, node.getExpression().getProperty(PROPERTY_CONSTRAINT_VARIABLE));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.QualifiedName)
	 */
	public final void endVisit(final QualifiedName node) {
		final ASTNode parent= node.getParent();
		final Name qualifier= node.getQualifier();
		IBinding binding= qualifier.resolveBinding();
		if (binding instanceof ITypeBinding) {
			final ConstraintVariable2 variable= fModel.createTypeVariable((ITypeBinding) binding, new CompilationUnitRange(RefactoringASTParser.getCompilationUnit(node), new SourceRange(qualifier.getStartPosition(), qualifier.getLength())));
			if (variable != null)
				qualifier.setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
		}
		binding= node.getName().resolveBinding();
		if (binding instanceof IVariableBinding && !(parent instanceof ImportDeclaration))
			endVisit((IVariableBinding) binding, qualifier, node);
		else if (binding instanceof ITypeBinding && parent instanceof FunctionDeclaration)
			endVisit((ITypeBinding) binding, node);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ReturnStatement)
	 */
	public final void endVisit(final ReturnStatement node) {
		final Expression expression= node.getExpression();
		if (expression != null) {
			final ConstraintVariable2 descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			if (descendant != null) {
				final FunctionDeclaration declaration= (FunctionDeclaration) fCurrentMethods.peek();
				if (declaration != null) {
					final IFunctionBinding binding= declaration.resolveBinding();
					if (binding != null) {
						final ConstraintVariable2 ancestor= fModel.createReturnTypeVariable(binding);
						if (ancestor != null) {
							node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
							fModel.createSubtypeConstraint(descendant, ancestor);
						}
					}
				}
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.SimpleName)
	 */
	public final void endVisit(final SimpleName node) {
		final ASTNode parent= node.getParent();
		if (!(parent instanceof ImportDeclaration) && !(parent instanceof PackageDeclaration) && !(parent instanceof AbstractTypeDeclaration)) {
			final IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding && !(parent instanceof FunctionDeclaration))
				endVisit((IVariableBinding) binding, null, node);
			else if (binding instanceof ITypeBinding && parent instanceof FunctionDeclaration)
				endVisit((ITypeBinding) binding, node);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration)
	 */
	public final void endVisit(final SingleVariableDeclaration node) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) node.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (ancestor != null) {
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
			final Expression expression= node.getInitializer();
			if (expression != null) {
				final ConstraintVariable2 descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation)
	 */
	public final void endVisit(final SuperConstructorInvocation node) {
		final IFunctionBinding binding= node.resolveConstructorBinding();
		if (binding != null)
			endVisit(node.arguments(), binding);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.SuperFieldAccess)
	 */
	public final void endVisit(final SuperFieldAccess node) {
		final Name name= node.getName();
		final IBinding binding= name.resolveBinding();
		if (binding instanceof IVariableBinding)
			endVisit((IVariableBinding) binding, null, node);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation)
	 */
	public final void endVisit(final SuperMethodInvocation node) {
		final IFunctionBinding superBinding= node.resolveMethodBinding();
		if (superBinding != null) {
			endVisit(node.arguments(), superBinding);
			final FunctionDeclaration declaration= (FunctionDeclaration) fCurrentMethods.peek();
			if (declaration != null) {
				final IFunctionBinding subBinding= declaration.resolveBinding();
				if (subBinding != null) {
					final ConstraintVariable2 ancestor= fModel.createReturnTypeVariable(superBinding);
					if (ancestor != null) {
						node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
						final ConstraintVariable2 descendant= fModel.createReturnTypeVariable(subBinding);
						if (descendant != null)
							fModel.createEqualityConstraint(descendant, ancestor);
					}
				}
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.ThisExpression)
	 */
	public final void endVisit(final ThisExpression node) {
		final ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null)
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createDeclaringTypeVariable(binding));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.Type)
	 */
	public final void endVisit(final Type node) {
		final ASTNode parent= node.getParent();
		if (!(parent instanceof AbstractTypeDeclaration) && !(parent instanceof ClassInstanceCreation) && !(parent instanceof TypeLiteral) && (!(parent instanceof InstanceofExpression) || fInstanceOf))
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createTypeVariable(node));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression)
	 */
	public final void endVisit(final VariableDeclarationExpression node) {
		endVisit(node.fragments(), node.getType(), node);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment)
	 */
	public final void endVisit(final VariableDeclarationFragment node) {
		final Expression initializer= node.getInitializer();
		if (initializer != null)
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, initializer.getProperty(PROPERTY_CONSTRAINT_VARIABLE));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement)
	 */
	public final void endVisit(final VariableDeclarationStatement node) {
		endVisit(node.fragments(), node.getType(), node);
	}

	/**
	 * Returns the original methods of the method hierarchy of the specified method.
	 * 
	 * @param binding the method binding
	 * @return the original methods (element type: <code>IFunctionBinding</code>)
	 */
	private Collection getOriginalMethods(final IFunctionBinding binding) {
		final Collection originals= new ArrayList();
		final ITypeBinding type= binding.getDeclaringClass();
		getOriginalMethods(binding, type, originals, false);
		getOriginalMethods(binding, type, originals, true);
		if (originals.isEmpty())
			originals.add(binding);
		return originals;
	}



	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Comment)
	 */
	public final boolean visit(final Comment node) {
		return false;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ImportDeclaration)
	 */
	public final boolean visit(final ImportDeclaration node) {
		return false;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionDeclaration)
	 */
	public final boolean visit(final FunctionDeclaration node) {
		fCurrentMethods.push(node);
		return super.visit(node);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.PackageDeclaration)
	 */
	public final boolean visit(final PackageDeclaration node) {
		return false;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ThisExpression)
	 */
	public final boolean visit(final ThisExpression node) {
		return false;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.Type)
	 */
	public final boolean visit(final Type node) {
		return false;
	}
}
