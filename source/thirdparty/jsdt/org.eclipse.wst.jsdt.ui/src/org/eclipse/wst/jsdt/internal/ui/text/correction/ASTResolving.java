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

package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
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
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType.Code;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.dom.TypeBindingVisitor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class ASTResolving {

	public static ITypeBinding guessBindingForReference(ASTNode node) {
		return Bindings.normalizeTypeBinding(getPossibleReferenceBinding(node));
	}

	private static ITypeBinding getPossibleReferenceBinding(ASTNode node) {
		ASTNode parent= node.getParent();
		switch (parent.getNodeType()) {
		case ASTNode.ASSIGNMENT:
			Assignment assignment= (Assignment) parent;
			if (node.equals(assignment.getLeftHandSide())) {
				// field write access: xx= expression
				return assignment.getRightHandSide().resolveTypeBinding();
			}
			// read access
			return assignment.getLeftHandSide().resolveTypeBinding();
		case ASTNode.INFIX_EXPRESSION:
			InfixExpression infix= (InfixExpression) parent;
			InfixExpression.Operator op= infix.getOperator();
			if (op == InfixExpression.Operator.CONDITIONAL_AND || op == InfixExpression.Operator.CONDITIONAL_OR) {
				// boolean operation
				return infix.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			} else if (op == InfixExpression.Operator.LEFT_SHIFT || op == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED || op == InfixExpression.Operator.RIGHT_SHIFT_SIGNED) {
				// asymmetric operation
				return infix.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			if (node.equals(infix.getLeftOperand())) {
				//	xx operation expression
				ITypeBinding rigthHandBinding= infix.getRightOperand().resolveTypeBinding();
				if (rigthHandBinding != null) {
					return rigthHandBinding;
				}
			} else {
				// expression operation xx
				ITypeBinding leftHandBinding= infix.getLeftOperand().resolveTypeBinding();
				if (leftHandBinding != null) {
					return leftHandBinding;
				}
			}
			if (op != InfixExpression.Operator.EQUALS && op != InfixExpression.Operator.NOT_EQUALS) {
				return infix.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			break;
		case ASTNode.INSTANCEOF_EXPRESSION:
			InstanceofExpression instanceofExpression= (InstanceofExpression) parent;
			return instanceofExpression.getRightOperand().resolveBinding();
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
			if (frag.getInitializer().equals(node)) {
				return frag.getName().resolveTypeBinding();
			}
			break;
		case ASTNode.SUPER_METHOD_INVOCATION:
			SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) parent;
			IFunctionBinding superMethodBinding= ASTNodes.getMethodBinding(superMethodInvocation.getName());
			if (superMethodBinding != null) {
				return getParameterTypeBinding(node, superMethodInvocation.arguments(), superMethodBinding);
			}
			break;
		case ASTNode.FUNCTION_INVOCATION:
			FunctionInvocation methodInvocation= (FunctionInvocation) parent;
			IFunctionBinding methodBinding= methodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				return getParameterTypeBinding(node, methodInvocation.arguments(), methodBinding);
			}
			break;
		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION: {
			SuperConstructorInvocation superInvocation= (SuperConstructorInvocation) parent;
			IFunctionBinding superBinding= superInvocation.resolveConstructorBinding();
			if (superBinding != null) {
				return getParameterTypeBinding(node, superInvocation.arguments(), superBinding);
			}
			break;
		}
		case ASTNode.CONSTRUCTOR_INVOCATION: {
			ConstructorInvocation constrInvocation= (ConstructorInvocation) parent;
			IFunctionBinding constrBinding= constrInvocation.resolveConstructorBinding();
			if (constrBinding != null) {
				return getParameterTypeBinding(node, constrInvocation.arguments(), constrBinding);
			}
			break;
		}
		case ASTNode.CLASS_INSTANCE_CREATION: {
			ClassInstanceCreation creation= (ClassInstanceCreation) parent;
			IFunctionBinding creationBinding= creation.resolveConstructorBinding();
			if (creationBinding != null) {
				return getParameterTypeBinding(node, creation.arguments(), creationBinding);
			}
			break;
		}
		case ASTNode.PARENTHESIZED_EXPRESSION:
			return guessBindingForReference(parent);
		case ASTNode.ARRAY_ACCESS:
			if (((ArrayAccess) parent).getIndex().equals(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			} else {
				return getPossibleReferenceBinding(parent);
			}
		case ASTNode.ARRAY_CREATION:
			if (((ArrayCreation) parent).dimensions().contains(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			break;
		case ASTNode.ARRAY_INITIALIZER:
			ASTNode initializerParent= parent.getParent();
			int dim= 1;
			while (initializerParent instanceof ArrayInitializer) {
				initializerParent= initializerParent.getParent();
				dim++;
			}
			Type creationType= null;
			if (initializerParent instanceof ArrayCreation) {
				creationType= ((ArrayCreation) initializerParent).getType();
			} else if (initializerParent instanceof VariableDeclaration) {
				VariableDeclaration varDecl= (VariableDeclaration) initializerParent;
				creationType= ASTNodes.getType(varDecl);
				dim-= varDecl.getExtraDimensions();
			}
			if (creationType != null) {
				while ((creationType instanceof ArrayType) && dim > 0) {
					creationType= ((ArrayType) creationType).getComponentType();
					dim--;
				}
				return creationType.resolveBinding();
			}
			break;
		case ASTNode.CONDITIONAL_EXPRESSION:
			ConditionalExpression expression= (ConditionalExpression) parent;
			if (node.equals(expression.getExpression())) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
			if (node.equals(expression.getElseExpression())) {
				return expression.getThenExpression().resolveTypeBinding();
			}
			return expression.getElseExpression().resolveTypeBinding();
		case ASTNode.POSTFIX_EXPRESSION:
			return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
		case ASTNode.PREFIX_EXPRESSION:
			if (((PrefixExpression) parent).getOperator() == PrefixExpression.Operator.NOT) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
			return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
		case ASTNode.IF_STATEMENT:
		case ASTNode.WHILE_STATEMENT:
		case ASTNode.DO_STATEMENT:
			if (node instanceof Expression) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
			break;
		case ASTNode.SWITCH_STATEMENT:
			if (((SwitchStatement) parent).getExpression().equals(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			break;
		case ASTNode.RETURN_STATEMENT:
			FunctionDeclaration decl= ASTResolving.findParentMethodDeclaration(parent);
			if (decl != null && !decl.isConstructor()) {
				return decl.getReturnType2() != null ? decl.getReturnType2().resolveBinding() : null;
			}
			break;
		case ASTNode.THROW_STATEMENT:
		case ASTNode.CATCH_CLAUSE:
            return parent.getAST().resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
		case ASTNode.FIELD_ACCESS:
			if (node.equals(((FieldAccess) parent).getName())) {
				return getPossibleReferenceBinding(parent);
			}
			break;
		case ASTNode.SUPER_FIELD_ACCESS:
			return getPossibleReferenceBinding(parent);
		case ASTNode.QUALIFIED_NAME:
			if (node.equals(((QualifiedName) parent).getName())) {
				return getPossibleReferenceBinding(parent);
			}
			break;
		case ASTNode.SWITCH_CASE:
			if (node.equals(((SwitchCase) parent).getExpression()) && parent.getParent() instanceof SwitchStatement) {
				return ((SwitchStatement) parent.getParent()).getExpression().resolveTypeBinding();
			}
			break;
		default:
			// do nothing
		}

		return null;
	}
	
	public static Type guessTypeForReference(AST ast, ASTNode node) {
		ASTNode parent= node.getParent();
		while (parent != null) {
			switch (parent.getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					if (((VariableDeclarationFragment) parent).getInitializer() == node) {
						return ASTNodeFactory.newType(ast, (VariableDeclaration) parent);
					}
					return null;
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					if (((VariableDeclarationFragment) parent).getInitializer() == node) {
						return ASTNodeFactory.newType(ast, (VariableDeclaration) parent);
					}
					return null;
				case ASTNode.ARRAY_ACCESS:
					if (!((ArrayAccess) parent).getIndex().equals(node)) {
						Type type= guessTypeForReference(ast, parent);
						if (type != null) {
							return ast.newArrayType(type);
						}
					}
					return null;
				case ASTNode.FIELD_ACCESS:
					if (node.equals(((FieldAccess) parent).getName())) {
						node= parent;
						parent= parent.getParent();
					} else {
						return null;
					}
					break;
				case ASTNode.SUPER_FIELD_ACCESS:
				case ASTNode.PARENTHESIZED_EXPRESSION:
					node= parent;
					parent= parent.getParent();
					break;
				case ASTNode.QUALIFIED_NAME:
					if (node.equals(((QualifiedName) parent).getName())) {
						node= parent;
						parent= parent.getParent();
					} else {
						return null;
					}
					break;
				default:
					return null;
			}
		}
		return null;
	}

	private static ITypeBinding getParameterTypeBinding(ASTNode node, List args, IFunctionBinding binding) {
		ITypeBinding[] paramTypes= binding.getParameterTypes();
		int index= args.indexOf(node);
		if (binding.isVarargs() && index >= paramTypes.length - 1) {
			return paramTypes[paramTypes.length - 1].getComponentType();
		}
		if (index >= 0 && index < paramTypes.length) {
			return paramTypes[index];
		}
		return null;
	}

    public static ITypeBinding guessBindingForTypeReference(ASTNode node) {
    	StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
    	if (locationInParent == QualifiedName.QUALIFIER_PROPERTY) {
    		return null; // can't guess type for X.A
    	}
    	if (locationInParent == SimpleType.NAME_PROPERTY) {
    		node= node.getParent();
    	}
    	ITypeBinding binding= Bindings.normalizeTypeBinding(getPossibleTypeBinding(node));
    	return binding;
    }

	private static ITypeBinding getPossibleTypeBinding(ASTNode node) {
		ASTNode parent= node.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.ARRAY_TYPE: {
				int dim= 1;
				while (parent.getParent() instanceof ArrayType) {
					parent= parent.getParent();
					dim++;
				}
				ITypeBinding parentBinding= getPossibleTypeBinding(parent);
				if (parentBinding != null && parentBinding.getDimensions() == dim) {
					return parentBinding.getElementType();
				}
				return null;
			}
			case ASTNode.QUALIFIED_TYPE: {
				ITypeBinding parentBinding= getPossibleTypeBinding(parent);
				if (parentBinding == null || !parentBinding.isMember()) {
					return null;
				}
				if (node.getLocationInParent() == QualifiedType.QUALIFIER_PROPERTY) {
					return parentBinding.getDeclaringClass();
				}
				return parentBinding;
			}
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				return guessVariableType(((VariableDeclarationStatement) parent).fragments());
			case ASTNode.FIELD_DECLARATION:
				return guessVariableType(((FieldDeclaration) parent).fragments());
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				return guessVariableType(((VariableDeclarationExpression) parent).fragments());
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				SingleVariableDeclaration varDecl= (SingleVariableDeclaration) parent;
				if (varDecl.getInitializer() != null) {
					return Bindings.normalizeTypeBinding(varDecl.getInitializer().resolveTypeBinding());
				}
				break;
			case ASTNode.ARRAY_CREATION:
				ArrayCreation creation= (ArrayCreation) parent;
				if (creation.getInitializer() != null) {
					return creation.getInitializer().resolveTypeBinding();
				}
				return getPossibleReferenceBinding(parent);
			case ASTNode.TYPE_LITERAL:
				return ((TypeLiteral) parent).getType().resolveBinding();
			case ASTNode.CLASS_INSTANCE_CREATION:
				return getPossibleReferenceBinding(parent);
			case ASTNode.TAG_ELEMENT:
				TagElement tagElement= (TagElement) parent;
				if (TagElement.TAG_THROWS.equals(tagElement.getTagName()) || TagElement.TAG_EXCEPTION.equals(tagElement.getTagName())) {
					ASTNode methNode= tagElement.getParent().getParent();
					if (methNode instanceof FunctionDeclaration) {
						List thrownExcpetions= ((FunctionDeclaration) methNode).thrownExceptions();
						if (thrownExcpetions.size() == 1) {
							return ((Name) thrownExcpetions.get(0)).resolveTypeBinding();
						}
					}
				}
				break;
		}
		return null;
	}

   	private static ITypeBinding guessVariableType(List fragments) {
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) iter.next();
			if (frag.getInitializer() != null) {
				return Bindings.normalizeTypeBinding(frag.getInitializer().resolveTypeBinding());
			}
		}
		return null;
	}

   	/**
   	 * Finds all type bindings that contain a method of a given signature
   	 * @param searchRoot the ast node to start the search from
   	 * @param selector the method name
   	 * @param arguments the method arguments
   	 * @param context the context in which the method would be called
   	 * @return returns all types known in the AST that have a method with a given name
   	 */
	public static ITypeBinding[] getQualifierGuess(ASTNode searchRoot, final String selector, List arguments, final IBinding context) {
		final int nArgs= arguments.size();
		final ArrayList result= new ArrayList();
		
		// test if selector is a object method
		ITypeBinding binding= searchRoot.getAST().resolveWellKnownType("Object"); //$NON-NLS-1$
		IFunctionBinding[] objectMethods= binding.getDeclaredMethods();
		for (int i= 0; i < objectMethods.length; i++) {
			IFunctionBinding meth= objectMethods[i];
			if (meth.getName().equals(selector) && meth.getParameterTypes().length == nArgs) {
				return new ITypeBinding[] { binding };
			}
		}

		visitAllBindings(searchRoot, new TypeBindingVisitor() {
			private HashSet fVisitedBindings= new HashSet(100);

			public boolean visit(ITypeBinding node) {
				node= Bindings.normalizeTypeBinding(node);
				if (node == null) {
					return true;
				}
				
				if (!fVisitedBindings.add(node.getKey())) {
					return true;
				}
				if (context != null && !isUseableTypeInContext(node, context, false)) {
					return true;
				}
				
				IFunctionBinding[] methods= node.getDeclaredMethods();
				for (int i= 0; i < methods.length; i++) {
					IFunctionBinding meth= methods[i];
					if (meth.getName().equals(selector) && meth.getParameterTypes().length == nArgs) {
						result.add(node);
					}
				}
				return true;
			}
		});
		return (ITypeBinding[]) result.toArray(new ITypeBinding[result.size()]);
	}
	
	public static void visitAllBindings(ASTNode astRoot, TypeBindingVisitor visitor) {
		try {
			astRoot.accept(new AllBindingsVisitor(visitor));
		} catch (AllBindingsVisitor.VisitCancelledException e) {
		}
	}
	
	private static class AllBindingsVisitor extends GenericVisitor {
		private final TypeBindingVisitor fVisitor;
		
		private static class VisitCancelledException extends RuntimeException {
			private static final long serialVersionUID= 1L;
		}
		public AllBindingsVisitor(TypeBindingVisitor visitor) {
			super(true);
			fVisitor= visitor;
		}
		public boolean visit(SimpleName node) {
			ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null) {
				boolean res= fVisitor.visit(binding);
				if (res) {
					res= Bindings.visitHierarchy(binding, fVisitor);
				}
				if (!res) {
					throw new VisitCancelledException();
				}
			}
			return false;
		}
	}


	public static IBinding getParentMethodOrTypeBinding(ASTNode node) {
		do {
			if (node instanceof FunctionDeclaration) {
				return ((FunctionDeclaration) node).resolveBinding();
			} else if (node instanceof AbstractTypeDeclaration) {
				return ((AbstractTypeDeclaration) node).resolveBinding();
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration) node).resolveBinding();
			}
			node= node.getParent();
		} while (node != null);
		
		return null;
	}
	
	public static BodyDeclaration findParentBodyDeclaration(ASTNode node) {
		while ((node != null) && (!(node instanceof BodyDeclaration))) {
			node= node.getParent();
		}
		return (BodyDeclaration) node;
	}
	
	public static ASTNode findParentBodyDeclaration(ASTNode node, boolean treatModifiersOutside) {
		StructuralPropertyDescriptor lastLocation= null;
		
		while (node != null) {
			if (node instanceof BodyDeclaration) {
				BodyDeclaration decl= (BodyDeclaration) node;
				if (!treatModifiersOutside || lastLocation != decl.getModifiersProperty()) {
					return decl;
				}
				treatModifiersOutside= false;
			}
			else if (node instanceof JavaScriptUnit)
				return node;
			lastLocation= node.getLocationInParent();
			node= node.getParent();
		}
		return  node;
	}
	

	public static JavaScriptUnit findParentCompilationUnit(ASTNode node) {
		return (JavaScriptUnit) findAncestor(node, ASTNode.JAVASCRIPT_UNIT);
	}

	/**
	 * Finds the parent type of a node.
	 * 
	 * @param node the node inside the type to find
	 * @param treatModifiersOutside if set, modifiers are not part of their type, but of the type's parent
	 * @return returns either a AbstractTypeDeclaration or an AnonymousTypeDeclaration 
	 */
	public static ASTNode findParentType(ASTNode node, boolean treatModifiersOutside) {
		StructuralPropertyDescriptor lastLocation= null;

		while (node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration decl= (AbstractTypeDeclaration) node;
				if (!treatModifiersOutside || lastLocation != decl.getModifiersProperty()) {
					return decl;
				}
			} else if (node instanceof AnonymousClassDeclaration) {
				return node;
			}
			lastLocation= node.getLocationInParent();
			node= node.getParent();
		}
		return null;
	}
	
	public static ASTNode findParentType(ASTNode node) {
		return findParentType(node, false);
	}

	
	public static ASTNode findParent(ASTNode node) {
		while (node != null) {
			
		 if (node instanceof JavaScriptUnit) {
			return node;
		}
		 else	if (node instanceof AbstractTypeDeclaration) {
				return node;
	 
			} else if (node instanceof AnonymousClassDeclaration) {
				return node;
			}
			node= node.getParent();
		}
		return null;
	}

	/**
	 * Returns the method binding of the node's parent method declaration or <code>null</code> if the node
	 * is not inside a method
	 * @param node
	 * @return JavaScriptUnit
	 */
	public static FunctionDeclaration findParentMethodDeclaration(ASTNode node) {
		while (node != null) {
			if (node.getNodeType() == ASTNode.FUNCTION_DECLARATION) {
				return (FunctionDeclaration) node;
			}
			if (node instanceof AbstractTypeDeclaration || node instanceof AnonymousClassDeclaration) {
				return null;
			}
			node= node.getParent();
		}
		return null;
	}

	public static ASTNode findAncestor(ASTNode node, int nodeType) {
		while ((node != null) && (node.getNodeType() != nodeType)) {
			node= node.getParent();
		}
		return node;
	}

	public static Statement findParentStatement(ASTNode node) {
		while ((node != null) && (!(node instanceof Statement))) {
			node= node.getParent();
			if (node instanceof BodyDeclaration) {
				return null;
			}
		}
		return (Statement) node;
	}

	public static TryStatement findParentTryStatement(ASTNode node) {
		while ((node != null) && (!(node instanceof TryStatement))) {
			node= node.getParent();
			if (node instanceof BodyDeclaration) {
				return null;
			}
		}
		return (TryStatement) node;
	}

	public static boolean isInsideConstructorInvocation(FunctionDeclaration methodDeclaration, ASTNode node) {
		if (methodDeclaration.isConstructor()) {
			Statement statement= ASTResolving.findParentStatement(node);
			if (statement instanceof ConstructorInvocation || statement instanceof SuperConstructorInvocation) {
				return true; // argument in a this or super call
			}
		}
		return false;
	}
	
	public static boolean isInsideModifiers(ASTNode node) {
		while (node != null && !(node instanceof BodyDeclaration)) {
			node= node.getParent();
		}
		return false;
	}

	public static boolean isInStaticContext(ASTNode selectedNode) {
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof FunctionDeclaration) {
			if (isInsideConstructorInvocation((FunctionDeclaration) decl, selectedNode)) {
				return true;
			}
			return Modifier.isStatic(decl.getModifiers());
		} else if (decl instanceof Initializer) {
			return Modifier.isStatic(((Initializer)decl).getModifiers());
		} else if (decl instanceof FieldDeclaration) {
			return Modifier.isStatic(((FieldDeclaration)decl).getModifiers());
		}
		return false;
	}

	public static boolean isWriteAccess(Name selectedNode) {
		ASTNode curr= selectedNode;
		ASTNode parent= curr.getParent();
		while (parent != null) {
			switch (parent.getNodeType()) {
				case ASTNode.QUALIFIED_NAME:
					if (((QualifiedName) parent).getQualifier() == curr) {
						return false;
					}
					break;
				case ASTNode.FIELD_ACCESS:
					if (((FieldAccess) parent).getExpression() == curr) {
						return false;
					}
					break;
				case ASTNode.SUPER_FIELD_ACCESS:
					break;
				case ASTNode.ASSIGNMENT:
					return ((Assignment) parent).getLeftHandSide() == curr;
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					return ((VariableDeclaration) parent).getName() == curr;
				case ASTNode.POSTFIX_EXPRESSION:
				case ASTNode.PREFIX_EXPRESSION:
					return true;
				default:
					return false;
			}

			curr= parent;
			parent= curr.getParent();
		}
		return false;
	}

	public static int getPossibleTypeKinds(ASTNode node, boolean is50OrHigher) {
		int kinds= internalGetPossibleTypeKinds(node);
		if (!is50OrHigher) {
			kinds &= (SimilarElementsRequestor.INTERFACES | SimilarElementsRequestor.CLASSES);
		}
		return kinds;
	}
	
	
	private static int internalGetPossibleTypeKinds(ASTNode node) {
		int kind= SimilarElementsRequestor.ALL_TYPES;

		int mask= SimilarElementsRequestor.ALL_TYPES | SimilarElementsRequestor.VOIDTYPE;
		
		ASTNode parent= node.getParent();
		while (parent instanceof QualifiedName) {
			if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY) {
				return SimilarElementsRequestor.REF_TYPES;
			}
			node= parent;
			parent= parent.getParent();
			mask= SimilarElementsRequestor.REF_TYPES;
		}
		while (parent instanceof Type) {
			if (parent instanceof QualifiedType) {
				if (node.getLocationInParent() == QualifiedType.QUALIFIER_PROPERTY) {
					return mask & (SimilarElementsRequestor.REF_TYPES);
				}
				mask&= SimilarElementsRequestor.REF_TYPES;
			}
			node= parent;
			parent= parent.getParent();
		}

		switch (parent.getNodeType()) {
			case ASTNode.TYPE_DECLARATION:
				if (node.getLocationInParent() == TypeDeclaration.SUPERCLASS_TYPE_PROPERTY) {
					kind= SimilarElementsRequestor.CLASSES;
				}
				break;
			case ASTNode.FUNCTION_DECLARATION:
				if (node.getLocationInParent() == FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
					kind= SimilarElementsRequestor.CLASSES;
				} else if (node.getLocationInParent() == FunctionDeclaration.RETURN_TYPE2_PROPERTY) {
					kind= SimilarElementsRequestor.ALL_TYPES | SimilarElementsRequestor.VOIDTYPE;
				}
				break;
			case ASTNode.INSTANCEOF_EXPRESSION:
				kind= SimilarElementsRequestor.REF_TYPES;
				break;
			case ASTNode.THROW_STATEMENT:
				kind= SimilarElementsRequestor.CLASSES;
				break;
			case ASTNode.CLASS_INSTANCE_CREATION:
				if (((ClassInstanceCreation) parent).getAnonymousClassDeclaration() == null) {
					kind= SimilarElementsRequestor.CLASSES;
				} else {
					kind= SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES;
				}
				break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				int superParent= parent.getParent().getNodeType();
				if (superParent == ASTNode.CATCH_CLAUSE) {
					kind= SimilarElementsRequestor.CLASSES;
				}
				break;
			case ASTNode.TAG_ELEMENT:
				kind= SimilarElementsRequestor.REF_TYPES;
				break;
			case ASTNode.TYPE_LITERAL:
				kind= SimilarElementsRequestor.REF_TYPES;
				break;
			default:
		}
		return kind & mask;
	}

	public static String getFullName(Name name) {
		return name.getFullyQualifiedName();
	}

	public static IJavaScriptUnit findCompilationUnitForBinding(IJavaScriptUnit cu, JavaScriptUnit astRoot, ITypeBinding binding) throws JavaScriptModelException {
		if (binding == null || !binding.isFromSource()) {
			return null;
		}
		ASTNode node= astRoot.findDeclaringNode(binding.getTypeDeclaration());
		if (node == null) {
			IJavaScriptUnit targetCU= Bindings.findCompilationUnit(binding, cu.getJavaScriptProject());
			if (targetCU != null) {
				return targetCU;
			}
			return null;
		} else if (node instanceof JavaScriptUnit || node instanceof AbstractTypeDeclaration || node instanceof AnonymousClassDeclaration) {
			return cu;
		}

		
		return null;
	}


	private static final Code[] CODE_ORDER= { PrimitiveType.CHAR, PrimitiveType.SHORT, PrimitiveType.INT, PrimitiveType.LONG, PrimitiveType.FLOAT, PrimitiveType.DOUBLE };

	public static ITypeBinding[] getNarrowingTypes(AST ast, ITypeBinding type) {
		ArrayList res= new ArrayList();
		res.add(type);
		if (type.isPrimitive()) {
			Code code= PrimitiveType.toCode(type.getName());
			for (int i= 0; i < CODE_ORDER.length && code != CODE_ORDER[i]; i++) {
				String typeName= CODE_ORDER[i].toString();
				res.add(ast.resolveWellKnownType(typeName));
			}
		}
		return (ITypeBinding[]) res.toArray(new ITypeBinding[res.size()]);
	}
	
	public static ITypeBinding[] getRelaxingTypes(AST ast, ITypeBinding type) {
		ArrayList res= new ArrayList();
		res.add(type);
		if (type.isArray()) {
			res.add(ast.resolveWellKnownType("Object")); //$NON-NLS-1$
//			res.add(ast.resolveWellKnownType("java.io.Serializable")); //$NON-NLS-1$ 
//			res.add(ast.resolveWellKnownType("java.lang.Cloneable")); //$NON-NLS-1$
		} else if (type.isPrimitive()) {
			Code code= PrimitiveType.toCode(type.getName());
			boolean found= false;
			for (int i= 0; i < CODE_ORDER.length; i++) {
				if (found) {
					String typeName= CODE_ORDER[i].toString();
					res.add(ast.resolveWellKnownType(typeName));
				}
				if (code == CODE_ORDER[i]) {
					found= true;
				}
			}
		} else {
			collectRelaxingTypes(res, type);
		}
		return (ITypeBinding[]) res.toArray(new ITypeBinding[res.size()]);
	}

	private static void collectRelaxingTypes(Collection res, ITypeBinding type) {
		ITypeBinding binding= type.getSuperclass();
		if (binding != null) {
			if (!res.contains(binding)) {
				res.add(binding);
			}
			collectRelaxingTypes(res, binding);
		}
	}
	
	public static String[] getUsedVariableNames(ASTNode node) {
		JavaScriptUnit root= (JavaScriptUnit) node.getRoot();
		Collection res= (new ScopeAnalyzer(root)).getUsedVariableNames(node.getStartPosition(), node.getLength());
		return (String[]) res.toArray(new String[res.size()]);
	}

	public static boolean isUseableTypeInContext(ITypeBinding[] binding, IBinding context, boolean noWildcards) {
		for (int i= 0; i < binding.length; i++) {
			if (!isUseableTypeInContext(binding[i], context, noWildcards)) {
				return false;
			}
		}
		return true;
	}


	public static boolean isUseableTypeInContext(ITypeBinding type, IBinding context, boolean noWildcards) {
		if (type.isArray()) {
			type= type.getElementType();
		}
		if (type.isAnonymous()) {
			return false;
		}
		if (type.isPrimitive()) {
			return true;
		}
		return true;
	}

	// pretty signatures

	public static String getTypeSignature(ITypeBinding type) {
		return BindingLabelProvider.getBindingLabel(type, BindingLabelProvider.DEFAULT_TEXTFLAGS);
	}

	public static String getMethodSignature(IFunctionBinding binding, boolean inOtherCU) {
		StringBuffer buf= new StringBuffer();
		if (inOtherCU && !binding.isConstructor()) {
			buf.append(binding.getDeclaringClass().getTypeDeclaration().getName()).append('.'); // simple type name
		}
		return BindingLabelProvider.getBindingLabel(binding, BindingLabelProvider.DEFAULT_TEXTFLAGS);
	}

	public static String getMethodSignature(String name, ITypeBinding[] params, boolean isVarArgs) {
		StringBuffer buf= new StringBuffer();
		buf.append(name).append('(');
		for (int i= 0; i < params.length; i++) {
			if (i > 0) {
				buf.append(JavaScriptElementLabels.COMMA_STRING);
			}
			if (isVarArgs && i == params.length - 1) {
				buf.append(getTypeSignature(params[i].getElementType()));
				buf.append("..."); //$NON-NLS-1$
			} else {
				buf.append(getTypeSignature(params[i]));
			}
		}
		buf.append(')');
		return buf.toString();
	}

	public static JavaScriptUnit createQuickFixAST(IJavaScriptUnit compilationUnit, IProgressMonitor monitor) {
		ASTParser astParser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		astParser.setSource(compilationUnit);
		astParser.setResolveBindings(true);
		astParser.setStatementsRecovery(ASTProvider.SHARED_AST_STATEMENT_RECOVERY);
		astParser.setBindingsRecovery(ASTProvider.SHARED_BINDING_RECOVERY);
		return (JavaScriptUnit) astParser.createAST(monitor);
	}

}
