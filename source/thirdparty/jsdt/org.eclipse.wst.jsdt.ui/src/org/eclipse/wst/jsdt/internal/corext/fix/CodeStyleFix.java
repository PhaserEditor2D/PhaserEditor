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
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
 * A fix which fixes code style issues.
 */
public class CodeStyleFix extends AbstractFix {
	
	private final static class CodeStyleVisitor extends GenericVisitor {
		
		private final List/*<IFixRewriteOperation>*/ fResult;
		private final boolean fFindUnqualifiedAccesses;
		private final boolean fFindUnqualifiedStaticAccesses;
		private final boolean fFindUnqualifiedMethodAccesses;
		private final boolean fFindUnqualifiedStaticMethodAccesses;
		
		public CodeStyleVisitor(JavaScriptUnit compilationUnit, 
				boolean findUnqualifiedAccesses, 
				boolean findUnqualifiedStaticAccesses,
				boolean findUnqualifiedMethodAccesses,
				boolean findUnqualifiedStaticMethodAccesses,
				List resultingCollection) throws CoreException {
			
			fFindUnqualifiedAccesses= findUnqualifiedAccesses;
			fFindUnqualifiedStaticAccesses= findUnqualifiedStaticAccesses;
			fFindUnqualifiedMethodAccesses= findUnqualifiedMethodAccesses;
			fFindUnqualifiedStaticMethodAccesses= findUnqualifiedStaticMethodAccesses;
			//fImportRewrite= StubUtility.createImportRewrite(compilationUnit, true);
			fResult= resultingCollection;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(TypeDeclaration node) {
			return super.visit(node);
		}

		public boolean visit(QualifiedName node) {
			if (fFindUnqualifiedAccesses || fFindUnqualifiedStaticAccesses) {
				ASTNode simpleName= node;
				while (simpleName instanceof QualifiedName) {
					simpleName= ((QualifiedName) simpleName).getQualifier();
				}
				if (simpleName instanceof SimpleName) {
					handleSimpleName((SimpleName)simpleName);
				}
			}
			return false;
		}

		public boolean visit(SimpleName node) {
			if (fFindUnqualifiedAccesses || fFindUnqualifiedStaticAccesses) {
				handleSimpleName(node);
			}
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(FunctionInvocation node) {
			if (!fFindUnqualifiedMethodAccesses && !fFindUnqualifiedStaticMethodAccesses)
				return true;
			
			if (node.getExpression() != null)
				return true;
			
			SimpleName name = node.getName();
			if (name!=null)
			{
				IBinding binding= name.resolveBinding();
				if (!(binding instanceof IFunctionBinding))
					return true;
				handleMethod(name, (IFunctionBinding)binding);
			}
			
			return true;
		}

		private void handleSimpleName(SimpleName node) {
			ASTNode firstExpression= node.getParent();
			if (firstExpression instanceof FieldAccess) {
				while (firstExpression instanceof FieldAccess) {
					firstExpression= ((FieldAccess)firstExpression).getExpression();
				}
				if (!(firstExpression instanceof SimpleName))
					return;
				
				node= (SimpleName)firstExpression;
			} else if (firstExpression instanceof SuperFieldAccess)
				return;
			
			StructuralPropertyDescriptor parentDescription= node.getLocationInParent();
			if (parentDescription == VariableDeclarationFragment.NAME_PROPERTY || parentDescription == SwitchCase.EXPRESSION_PROPERTY)
				return;
			
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return;
			
			handleVariable(node, (IVariableBinding) binding);
		}

		private void handleVariable(SimpleName node, IVariableBinding varbinding) {
			if (!varbinding.isField())
				return;

			ITypeBinding declaringClass= varbinding.getDeclaringClass();
			if (Modifier.isStatic(varbinding.getModifiers())) {
				if (fFindUnqualifiedStaticAccesses) {
					Initializer initializer= (Initializer) ASTNodes.getParent(node, Initializer.class);
					//Do not qualify assignments to static final fields in static initializers (would result in compile error)
					StructuralPropertyDescriptor parentDescription= node.getLocationInParent();
					if (initializer != null && Modifier.isStatic(initializer.getModifiers())
							&& Modifier.isFinal(varbinding.getModifiers()) && parentDescription == Assignment.LEFT_HAND_SIDE_PROPERTY)
						return;
						
					//Do not qualify static fields if defined inside an anonymous class
					if (declaringClass.isAnonymous())
						return;

					fResult.add(new AddStaticQualifierOperation(declaringClass, node));
				}
			} 
//			else if (fFindUnqualifiedAccesses){
//				String qualifier= getNonStaticQualifier(declaringClass, fImportRewrite, node);
//				if (qualifier == null)
//					return;
//
//				fResult.add(new AddThisQualifierOperation(qualifier, node));
//			}
		}		

		private void handleMethod(SimpleName node, IFunctionBinding binding) {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (Modifier.isStatic(binding.getModifiers())) {
				if (fFindUnqualifiedStaticMethodAccesses) {
					//Do not qualify static fields if defined inside an anonymous class
					if (declaringClass.isAnonymous())
						return;

					fResult.add(new AddStaticQualifierOperation(declaringClass, node));
				}
			} else {
//				if (fFindUnqualifiedMethodAccesses) {
//					String qualifier= getNonStaticQualifier(declaringClass, fImportRewrite, node);
//					if (qualifier == null)
//						return;
//
//					fResult.add(new AddThisQualifierOperation(qualifier, node));
//				}
			}
		}
	}
	
//	private static class ThisQualifierVisitor extends GenericVisitor {
//		
//		private final JavaScriptUnit fCompilationUnit;
//		private final List fOperations;
//		private final boolean fRemoveFieldQualifiers;
//		private final boolean fRemoveMethodQualifiers;
//		
//		public ThisQualifierVisitor(boolean removeFieldQualifiers,
//									boolean removeMethodQualifiers,
//									JavaScriptUnit compilationUnit,
//									List result) {
//			fRemoveFieldQualifiers= removeFieldQualifiers;
//			fRemoveMethodQualifiers= removeMethodQualifiers;
//			fCompilationUnit= compilationUnit;
//			fOperations= result;
//		}
//		
//		/**
//		 * {@inheritDoc}
//		 */
//		public boolean visit(final FieldAccess node) {
//			if (!fRemoveFieldQualifiers)
//				return true;
//			
//			Expression expression= node.getExpression();
//			if (!(expression instanceof ThisExpression))
//				return true;
//			
//			final SimpleName name= node.getName();
//			if (hasConflict(expression.getStartPosition(), name, ScopeAnalyzer.VARIABLES))
//				return true;
//			
//			fOperations.add(new AbstractFixRewriteOperation() {
//				public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
//					ASTRewrite rewrite= cuRewrite.getASTRewrite();
//					
//					TextEditGroup group= createTextEditGroup(FixMessages.CodeStyleFix_removeThis_groupDescription);
//					textEditGroups.add(group);
//					
//					rewrite.replace(node, rewrite.createCopyTarget(name), group);
//				}
//			});
//			return super.visit(node);
//		}
//		
//		/**
//		 * {@inheritDoc}
//		 */
//		public boolean visit(final FunctionInvocation node) {
//			if (!fRemoveMethodQualifiers)
//				return true;
//			
//			Expression expression= node.getExpression();
//			if (!(expression instanceof ThisExpression)) 
//				return true;
//			
//			final SimpleName name= node.getName();
//			if (name!=null && name.resolveBinding() == null)
//				return true;
//			
//			if (hasConflict(expression.getStartPosition(), name, ScopeAnalyzer.METHODS))
//				return true;
//			
//			Name qualifier= ((ThisExpression)expression).getQualifier();
//			if (qualifier != null) {
//				ITypeBinding declaringClass= ((IFunctionBinding)name.resolveBinding()).getDeclaringClass();
//				if (declaringClass == null)
//					return true;
//				
//				ITypeBinding caller= getDeclaringType(node);
//				if (caller == null)
//					return true;
//				
//				ITypeBinding callee= (ITypeBinding)qualifier.resolveBinding();
//				if (callee == null)
//					return true;
//				
//				if (callee.isAssignmentCompatible(declaringClass) && caller.isAssignmentCompatible(declaringClass))
//					return true;
//			}
//			
//			fOperations.add(new AbstractFixRewriteOperation() {
//				public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
//					ASTRewrite rewrite= cuRewrite.getASTRewrite();
//					
//					TextEditGroup group= createTextEditGroup(FixMessages.CodeStyleFix_removeThis_groupDescription);
//					textEditGroups.add(group);
//					
//					rewrite.remove(node.getExpression(), group);
//				}
//			});
//			return super.visit(node);
//		}
//		
//		private ITypeBinding getDeclaringType(FunctionInvocation node) {
//			ASTNode p= node;
//			while (p != null) {
//				p= p.getParent();
//				if (p instanceof AbstractTypeDeclaration) {
//					return ((AbstractTypeDeclaration)p).resolveBinding();
//				}
//			}
//			return null;
//        }
//
//		private boolean hasConflict(int startPosition, SimpleName name, int flag) {
//			ScopeAnalyzer analyzer= new ScopeAnalyzer(fCompilationUnit);
//			IBinding[] declarationsInScope= analyzer.getDeclarationsInScope(startPosition, flag);
//			for (int i= 0; i < declarationsInScope.length; i++) {
//				IBinding decl= declarationsInScope[i];
//				if (decl.getName().equals(name.getIdentifier()) && name.resolveBinding() != decl)
//					return true;
//			}
//			return false;
//		}
//	}

//	private final static class AddThisQualifierOperation extends AbstractFixRewriteOperation {
//
//		private final String fQualifier;
//		private final SimpleName fName;
//
//		public AddThisQualifierOperation(String qualifier, SimpleName name) {
//			fQualifier= qualifier;
//			fName= name;
//		}
//		
//		public String getDescription() {
//			return Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {fName.getIdentifier(), fQualifier});
//		}
//
//		/* (non-Javadoc)
//		 * @see org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
//		 */
//		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
//			ASTRewrite rewrite= cuRewrite.getASTRewrite();
//			TextEditGroup group;
//			if (fName.resolveBinding() instanceof IFunctionBinding) {
//				group= createTextEditGroup(FixMessages.CodeStyleFix_QualifyMethodWithThis_description);
//			} else {
//				group= createTextEditGroup(FixMessages.CodeStyleFix_QualifyFieldWithThis_description);
//			}
//			textEditGroups.add(group);
//			rewrite.replace(fName, rewrite.createStringPlaceholder(fQualifier  + '.' + fName.getIdentifier(), ASTNode.SIMPLE_NAME), group);
//		}		
//	}
	
	private final static class AddStaticQualifierOperation extends AbstractFixRewriteOperation {

		private final SimpleName fName;
		private final ITypeBinding fDeclaringClass;
		
		public AddStaticQualifierOperation(ITypeBinding declaringClass, SimpleName name) {
			super();
			fDeclaringClass= declaringClass;
			fName= name;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			JavaScriptUnit compilationUnit= cuRewrite.getRoot();
			importType(fDeclaringClass, fName, cuRewrite.getImportRewrite(), compilationUnit);
			TextEditGroup group;
			if (fName.resolveBinding() instanceof IFunctionBinding) {
				group= createTextEditGroup(FixMessages.CodeStyleFix_QualifyMethodWithDeclClass_description);
			} else {
				group= createTextEditGroup(FixMessages.CodeStyleFix_QualifyFieldWithDeclClass_description);
			}
			textEditGroups.add(group);
			IJavaScriptElement javaElement= fDeclaringClass.getJavaElement();
			if (javaElement instanceof IType) {
				Name qualifierName= compilationUnit.getAST().newName(((IType)javaElement).getElementName());
				SimpleName simpleName= (SimpleName)rewrite.createMoveTarget(fName);
				QualifiedName qualifiedName= compilationUnit.getAST().newQualifiedName(qualifierName, simpleName);
				rewrite.replace(fName, qualifiedName, group);
			}
		}
		
	}
	
	private final static class ToStaticAccessOperation extends AbstractFixRewriteOperation {

		private final ITypeBinding fDeclaringTypeBinding;
		private final Expression fQualifier;

		public ToStaticAccessOperation(ITypeBinding declaringTypeBinding, Expression qualifier) {
			super();
			fDeclaringTypeBinding= declaringTypeBinding;
			fQualifier= qualifier;
		}
		
		public String getAccessorName() {
			return fDeclaringTypeBinding.getName();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			Type type= importType(fDeclaringTypeBinding, fQualifier, cuRewrite.getImportRewrite(), cuRewrite.getRoot());
			TextEditGroup group= createTextEditGroup(FixMessages.CodeStyleFix_ChangeAccessUsingDeclaring_description);
			textEditGroups.add(group);
			cuRewrite.getASTRewrite().replace(fQualifier, type, group);
		}
	}
	
	public static CodeStyleFix[] createNonStaticAccessFixes(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (!isNonStaticAccess(problem))
			return null;
		
		ToStaticAccessOperation operations[]= createToStaticAccessOperations(compilationUnit, problem);
		if (operations == null)
			return null;

		String label1= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, operations[0].getAccessorName());
		CodeStyleFix fix1= new CodeStyleFix(label1, compilationUnit, new IFixRewriteOperation[] {operations[0]});

		if (operations.length > 1) {
			String label2= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStaticUsingInstanceType_description, operations[1].getAccessorName());
			CodeStyleFix fix2= new CodeStyleFix(label2, compilationUnit, new IFixRewriteOperation[] {operations[1]});
			return new CodeStyleFix[] {fix1, fix2};
		}
		return new CodeStyleFix[] {fix1};
	}
	
//	public static CodeStyleFix createAddFieldQualifierFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
//		if (IProblem.UnqualifiedFieldAccess != problem.getProblemId())
//			return null;
//		
//		AddThisQualifierOperation operation= getUnqualifiedFieldAccessResolveOperation(compilationUnit, problem);
//		if (operation == null)
//			return null;
//
//		String groupName= operation.getDescription();
//		return new CodeStyleFix(groupName, compilationUnit, new IFixRewriteOperation[] {operation});
//	}
	
	public static CodeStyleFix createIndirectAccessToStaticFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (!isIndirectStaticAccess(problem))
			return null;
		
		ToStaticAccessOperation operations[]= createToStaticAccessOperations(compilationUnit, problem);
		if (operations == null)
			return null;

		String label= Messages.format(FixMessages.CodeStyleFix_ChangeStaticAccess_description, operations[0].getAccessorName());
		return new CodeStyleFix(label, compilationUnit, new IFixRewriteOperation[] {operations[0]});
	}
	
	public static CodeStyleFix createCleanUp(JavaScriptUnit compilationUnit, 
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic, 
			boolean qualifyStaticFieldAccess,
			boolean changeIndirectStaticAccessToDirect,
			boolean qualifyMethodAccess,
			boolean qualifyStaticMethodAccess,
			boolean removeFieldQualifier,
			boolean removeMethodQualifier) throws CoreException {
		
		if (!addThisQualifier && !changeNonStaticAccessToStatic && !qualifyStaticFieldAccess && !changeIndirectStaticAccessToDirect && !qualifyMethodAccess && !qualifyStaticMethodAccess && !removeFieldQualifier && !removeMethodQualifier)
			return null;

		List/*<IFixRewriteOperation>*/ operations= new ArrayList(); 
		if (addThisQualifier || qualifyStaticFieldAccess || qualifyMethodAccess || qualifyStaticMethodAccess) {
			CodeStyleVisitor codeStyleVisitor= new CodeStyleVisitor(compilationUnit, addThisQualifier, qualifyStaticFieldAccess, qualifyMethodAccess, qualifyStaticMethodAccess, operations);
			compilationUnit.accept(codeStyleVisitor);
		}
		
		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
	        locations[i]= new ProblemLocation(problems[i]);
        }
		addToStaticAccessOperations(compilationUnit, locations, changeNonStaticAccessToStatic, changeIndirectStaticAccessToDirect, operations);
		
//		if (removeFieldQualifier || removeMethodQualifier) {
//			ThisQualifierVisitor visitor= new ThisQualifierVisitor(removeFieldQualifier, removeMethodQualifier, compilationUnit, operations);
//			compilationUnit.accept(visitor);
//		}

		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new CodeStyleFix(FixMessages.CodeStyleFix_change_name, compilationUnit, operationsArray);
	}
	
	public static CodeStyleFix createCleanUp(JavaScriptUnit compilationUnit, IProblemLocation[] problems, 
			boolean addThisQualifier, 
			boolean changeNonStaticAccessToStatic,
			boolean changeIndirectStaticAccessToDirect) throws CoreException {
		
		if (!addThisQualifier && !changeNonStaticAccessToStatic && !changeIndirectStaticAccessToDirect)
			return null;
				
		List/*<IFixRewriteOperation>*/ operations= new ArrayList(); 
//		if (addThisQualifier) {
//			for (int i= 0; i < problems.length; i++) {
//				IProblemLocation problem= problems[i];
//				if (problem.getProblemId() == IProblem.UnqualifiedFieldAccess) {
//					AddThisQualifierOperation operation= getUnqualifiedFieldAccessResolveOperation(compilationUnit, problem);
//					if (operation != null)
//						operations.add(operation);
//				}
//			}
//		}

		addToStaticAccessOperations(compilationUnit, problems, changeNonStaticAccessToStatic, changeIndirectStaticAccessToDirect, operations);

		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new CodeStyleFix(FixMessages.CodeStyleFix_change_name, compilationUnit, operationsArray);
	}
	
	private static void addToStaticAccessOperations(JavaScriptUnit compilationUnit, IProblemLocation[] problems, boolean changeNonStaticAccessToStatic, boolean changeIndirectStaticAccessToDirect, List result) {
	    Hashtable nonStaticAccessOps= new Hashtable();
		if (changeNonStaticAccessToStatic || changeIndirectStaticAccessToDirect) {
			for (int i= 0; i < problems.length; i++) {
				IProblemLocation problem= problems[i];
				boolean isNonStaticAccess= changeNonStaticAccessToStatic && isNonStaticAccess(problem);
				boolean isIndirectStaticAccess= changeIndirectStaticAccessToDirect && isIndirectStaticAccess(problem);
				if (isNonStaticAccess || isIndirectStaticAccess) {
					ToStaticAccessOperation[] nonStaticAccessInformation= createToStaticAccessOperations(compilationUnit, problem);
					if (nonStaticAccessInformation != null) {
						ToStaticAccessOperation op= nonStaticAccessInformation[0];
						nonStaticAccessOps.put(op.fQualifier, op);
					}
				}
			}
		}
		for (Iterator iter= nonStaticAccessOps.values().iterator(); iter.hasNext();) {
			ToStaticAccessOperation op= (ToStaticAccessOperation)iter.next();
			if (!nonStaticAccessOps.containsKey(op.fQualifier.getParent()))
				result.add(op);
		}
	}

	private static boolean isIndirectStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.IndirectAccessToStaticField
				|| problem.getProblemId() == IProblem.IndirectAccessToStaticMethod);
	}
	
	private static boolean isNonStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.NonStaticAccessToStaticField
				|| problem.getProblemId() == IProblem.NonStaticAccessToStaticMethod);
	}
	
	private static ToStaticAccessOperation[] createToStaticAccessOperations(JavaScriptUnit astRoot, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return null;
		}

		Expression qualifier= null;
		IBinding accessBinding= null;

        if (selectedNode instanceof QualifiedName) {
        	QualifiedName name= (QualifiedName) selectedNode;
        	qualifier= name.getQualifier();
        	accessBinding= name.resolveBinding();
        } else if (selectedNode instanceof SimpleName) {
        	ASTNode parent= selectedNode.getParent();
        	if (parent instanceof FieldAccess) {
        		FieldAccess fieldAccess= (FieldAccess) parent;
        		qualifier= fieldAccess.getExpression();
        		accessBinding= fieldAccess.getName().resolveBinding();
        	} else if (parent instanceof QualifiedName) {
        		QualifiedName qualifiedName= (QualifiedName) parent;
        		qualifier= qualifiedName.getQualifier();
        		accessBinding= qualifiedName.getName().resolveBinding();
        	}
        } else if (selectedNode instanceof FunctionInvocation) {
        	FunctionInvocation methodInvocation= (FunctionInvocation) selectedNode;
        	qualifier= methodInvocation.getExpression();
        	SimpleName name = methodInvocation.getName();
        	if (name!=null)
        		accessBinding= name.resolveBinding();
        } else if (selectedNode instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess) selectedNode;
			qualifier= fieldAccess.getExpression();
			accessBinding= fieldAccess.getName().resolveBinding();
		}
        
		if (accessBinding != null && qualifier != null) {
			ToStaticAccessOperation declaring= null;
			ITypeBinding declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
				
				declaring= new ToStaticAccessOperation(declaringTypeBinding, qualifier);
			}
			ToStaticAccessOperation instance= null;
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null) {
				instanceTypeBinding= instanceTypeBinding.getTypeDeclaration();  // use generic to avoid any type arguments
				if (instanceTypeBinding.getTypeDeclaration() != declaringTypeBinding) {
					instance= new ToStaticAccessOperation(instanceTypeBinding, qualifier);
				}
			}
			if (declaring != null && instance != null) {
				return new ToStaticAccessOperation[] {declaring, instance};
			} else {
				return new ToStaticAccessOperation[] {declaring};
			}
		}
		return null;
	}
	
	private static ITypeBinding getDeclaringTypeBinding(IBinding accessBinding) {
		if (accessBinding instanceof IFunctionBinding) {
			return ((IFunctionBinding) accessBinding).getDeclaringClass();
		} else if (accessBinding instanceof IVariableBinding) {
			return ((IVariableBinding) accessBinding).getDeclaringClass();
		}
		return null;
	}
		
//	private static AddThisQualifierOperation getUnqualifiedFieldAccessResolveOperation(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
//		SimpleName name= getName(compilationUnit, problem);
//		if (name == null)
//			return null;
//		
//		IBinding binding= name.resolveBinding();
//		if (binding == null || binding.getKind() != IBinding.VARIABLE)
//			return null;
//		
//		ImportRewrite imports= StubUtility.createImportRewrite(compilationUnit, true);
//		
//		String replacement= getQualifier((IVariableBinding)binding, imports, name);
//		if (replacement == null)
//			return null;
//		
//		return new AddThisQualifierOperation(replacement, name);
//	}
//	
//	private static String getQualifier(IVariableBinding binding, ImportRewrite imports, SimpleName name) {
//		ITypeBinding declaringClass= binding.getDeclaringClass();
//		if (Modifier.isStatic(binding.getModifiers())) {
//			IJavaScriptElement javaElement= declaringClass.getJavaElement();
//			if (javaElement instanceof IType) {
//				return ((IType)javaElement).getElementName();
//			}
//		} else {
//			return getNonStaticQualifier(declaringClass, imports, name);
//		}
//
//		return null;
//	}

//	private static String getNonStaticQualifier(ITypeBinding declaringClass, ImportRewrite imports, SimpleName name) {
//		ITypeBinding parentType= Bindings.getBindingOfParentType(name);
//		ITypeBinding currType= parentType;
//		while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
//			currType= currType.getDeclaringClass();
//		}
//		if (currType == null) {
//			declaringClass= declaringClass.getTypeDeclaration();
//			currType= parentType;
//			while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
//				currType= currType.getDeclaringClass();
//			}
//		}
//		if (currType != parentType) {
//			if (currType == null)
//				return null;
//			
//			if (currType.isAnonymous())
//				//If we access a field of a super class of an anonymous class
//				//then we can only qualify with 'this' but not with outer.this
//				//see bug 115277
//				return null;
//			
//			String outer= imports.addImport(currType);
//			return outer + ".this"; //$NON-NLS-1$
//		} else {
//			return "this"; //$NON-NLS-1$
//		}
//	}
	
//	private static SimpleName getName(JavaScriptUnit compilationUnit, IProblemLocation problem) {
//		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
//		
//		while (selectedNode instanceof QualifiedName) {
//			selectedNode= ((QualifiedName) selectedNode).getQualifier();
//		}
//		if (!(selectedNode instanceof SimpleName)) {
//			return null;
//		}
//		return (SimpleName) selectedNode;
//	}

	private CodeStyleFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
