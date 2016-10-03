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
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.wst.jsdt.internal.ui.text.correction.JavadocTagsSubProcessor;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
 * Fix which removes unused code.
 */
public class UnusedCodeFix extends AbstractFix {
	
	private static class SideEffectFinder extends ASTVisitor {

		private final ArrayList fSideEffectNodes;

		public SideEffectFinder(ArrayList res) {
			fSideEffectNodes= res;
		}

		public boolean visit(Assignment node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PostfixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PrefixExpression node) {
			Object operator= node.getOperator();
			if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
				fSideEffectNodes.add(node);
			}
			return false;
		}

		public boolean visit(FunctionInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(ClassInstanceCreation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(SuperMethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}
	}
	
	private static class RemoveImportOperation extends AbstractFixRewriteOperation {

		private final ImportDeclaration fImportDeclaration;
		
		public RemoveImportOperation(ImportDeclaration importDeclaration) {
			fImportDeclaration= importDeclaration;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ImportDeclaration node= fImportDeclaration;
			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveImport_description);
			cuRewrite.getASTRewrite().remove(node, group);
			textEditGroups.add(group);
		}
		
	}
	
	private static class RemoveUnusedMemberOperation extends AbstractFixRewriteOperation {

		private final SimpleName[] fUnusedNames;
		private boolean fForceRemove;
		private int fRemovedAssignmentsCount;
		private int fAlteredAssignmentsCount;
		
		public RemoveUnusedMemberOperation(SimpleName[] unusedNames, boolean forceRemoveInitializer) {
			fUnusedNames= unusedNames;
			fForceRemove=forceRemoveInitializer;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			for (int i= 0; i < fUnusedNames.length; i++) {
				removeUnusedName(cuRewrite.getASTRewrite(), fUnusedNames[i], cuRewrite.getRoot(), textEditGroups);	
			}
		}
		
		private void removeUnusedName(ASTRewrite rewrite, SimpleName simpleName, JavaScriptUnit completeRoot, List groups) {
			IBinding binding= simpleName.resolveBinding();
			JavaScriptUnit root= (JavaScriptUnit) simpleName.getRoot();
			String displayString= getDisplayString(binding);
			TextEditGroup group= createTextEditGroup(displayString);
			groups.add(group);
			if (binding.getKind() == IBinding.METHOD) {
				IFunctionBinding decl= ((IFunctionBinding) binding).getMethodDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				rewrite.remove(declaration, group);
			} else if (binding.getKind() == IBinding.TYPE) {
				ITypeBinding decl= ((ITypeBinding) binding).getTypeDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				if (declaration.getParent() instanceof TypeDeclarationStatement) {
					declaration= declaration.getParent();
				}
				rewrite.remove(declaration, group);
			} else if (binding.getKind() == IBinding.VARIABLE) {
				SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
				SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
				for (int i= 0; i < references.length; i++) {
					removeVariableReferences(rewrite, references[i], group);
				}

				IVariableBinding bindingDecl= ((IVariableBinding) nameNode.resolveBinding()).getVariableDeclaration();
				ASTNode declaringNode= completeRoot.findDeclaringNode(bindingDecl);
				if (declaringNode instanceof SingleVariableDeclaration) {
					removeParamTag(rewrite, (SingleVariableDeclaration) declaringNode, group);
				}
			} else {
				// unexpected
			}
		}
		
		private String getDisplayString(IBinding binding) {
			switch (binding.getKind()) {
				case IBinding.TYPE:
					return FixMessages.UnusedCodeFix_RemoveUnusedType_description;
				case IBinding.METHOD:
					if (((IFunctionBinding) binding).isConstructor()) {
						return FixMessages.UnusedCodeFix_RemoveUnusedConstructor_description;
					} else {
						return FixMessages.UnusedCodeFix_RemoveUnusedPrivateMethod_description;
					}
				case IBinding.VARIABLE:
					if (((IVariableBinding) binding).isField()) {
						return FixMessages.UnusedCodeFix_RemoveUnusedField_description;
					} else {
						return FixMessages.UnusedCodeFix_RemoveUnusedVariabl_description;
					}
				default:
					return ""; //$NON-NLS-1$
			}
		}

		private void removeParamTag(ASTRewrite rewrite, SingleVariableDeclaration varDecl, TextEditGroup group) {
			if (varDecl.getParent() instanceof FunctionDeclaration) {
				JSdoc javadoc= ((FunctionDeclaration) varDecl.getParent()).getJavadoc();
				if (javadoc != null) {
					TagElement tagElement= JavadocTagsSubProcessor.findParamTag(javadoc, varDecl.getName().getIdentifier());
					if (tagElement != null) {
						rewrite.remove(tagElement, group);
					}
				}
			}
		}
		
		/**
		 * Remove the field or variable declaration including the initializer.
		 * @param rewrite the AST rewriter to use
		 * @param reference a reference to the variable to remove
		 * @param group the text edit group to use
		 */
		private void removeVariableReferences(ASTRewrite rewrite, SimpleName reference, TextEditGroup group) {
			ASTNode parent= reference.getParent();
			while (parent instanceof QualifiedName) {
				parent= parent.getParent();
			}
			if (parent instanceof FieldAccess) {
				parent= parent.getParent();
			}

			int nameParentType= parent.getNodeType();
			if (nameParentType == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				Expression rightHand= assignment.getRightHandSide();

				ASTNode assignParent= assignment.getParent();
				if (assignParent.getNodeType() == ASTNode.EXPRESSION_STATEMENT && rightHand.getNodeType() != ASTNode.ASSIGNMENT) {
					removeVariableWithInitializer(rewrite, rightHand, assignParent, group);
				}	else {
					rewrite.replace(assignment, rewrite.createCopyTarget(rightHand), group);
				}
			} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
				rewrite.remove(parent, group);
			} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
				ASTNode varDecl= frag.getParent();
				List fragments;
				if (varDecl instanceof VariableDeclarationExpression) {
					fragments= ((VariableDeclarationExpression) varDecl).fragments();
				} else if (varDecl instanceof FieldDeclaration) {
					fragments= ((FieldDeclaration) varDecl).fragments();
				} else {
					fragments= ((VariableDeclarationStatement) varDecl).fragments();
				}
				Expression initializer = frag.getInitializer();
				boolean sideEffectInitializer = initializer instanceof FunctionInvocation || initializer instanceof ClassInstanceCreation;
				if (fragments.size() == fUnusedNames.length) {
					if (fForceRemove) {
						rewrite.remove(varDecl, group);
						return;
					}
					if (parent.getParent() instanceof FieldDeclaration) {
						rewrite.remove(varDecl, group);
						return;
					}
					if (sideEffectInitializer){
						Expression movedInit = (Expression) rewrite.createMoveTarget(initializer);
						ExpressionStatement wrapped = rewrite.getAST().newExpressionStatement(movedInit);
						rewrite.replace(varDecl, wrapped, group);
					} else {
						rewrite.remove(varDecl, group);
					}
				} else {
					if (fForceRemove) {
						rewrite.remove(frag, group);
						return;
					}
					//multiple declarations in one line
					ASTNode declaration = parent.getParent();
					if (declaration instanceof FieldDeclaration) {
						rewrite.remove(frag, group);
						return;
					}
					if (declaration instanceof VariableDeclarationStatement) {
						ASTNode lst = declaration.getParent();
						if (lst instanceof Block)
							splitUpDeclarations(rewrite, group, frag, lst, (VariableDeclarationStatement) declaration);
						rewrite.remove(frag, group);
						return;
					}
					if (declaration instanceof VariableDeclarationExpression) {
						//keep constructors and method invocations
						if (!sideEffectInitializer){
							rewrite.remove(frag, group);
						}
					}
				}
			}
		}

		private void splitUpDeclarations(ASTRewrite rewrite, TextEditGroup group, VariableDeclarationFragment frag, ASTNode block, VariableDeclarationStatement originalStatement) {
			Expression initializer = frag.getInitializer();
			//keep constructors and method invocations
			if (initializer instanceof FunctionInvocation || initializer instanceof ClassInstanceCreation){
				Expression movedInitializer= (Expression) rewrite.createMoveTarget(initializer);
				ListRewrite statementRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ExpressionStatement newInitializer= rewrite.getAST().newExpressionStatement( movedInitializer);
				statementRewrite.insertAfter(newInitializer, originalStatement, group);

				VariableDeclarationStatement newDeclaration= null;
				List fragments= originalStatement.fragments();
				int fragIndex= fragments.indexOf(frag);
				ListIterator fragmentIterator= fragments.listIterator(fragIndex+1);
				while (fragmentIterator.hasNext()) {
					VariableDeclarationFragment currentFragment= (VariableDeclarationFragment) fragmentIterator.next();
					VariableDeclarationFragment movedFragment= (VariableDeclarationFragment) rewrite.createMoveTarget(currentFragment);
					if (newDeclaration == null) {
						newDeclaration= rewrite.getAST().newVariableDeclarationStatement(movedFragment);
						Type copiedType= (Type) rewrite.createCopyTarget(originalStatement.getType());
						newDeclaration.setType(copiedType);
					} else
						newDeclaration.fragments().add(movedFragment);
				}
				if (newDeclaration != null){
					statementRewrite.insertAfter(newDeclaration, newInitializer, group);
				}
				if (originalStatement.fragments().size() == newDeclaration.fragments().size() + 1){
					rewrite.remove(originalStatement, group);
				}
			}
		}

		private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode, TextEditGroup group) {
			boolean performRemove= fForceRemove;
			if (!performRemove) {
				ArrayList sideEffectNodes= new ArrayList();
				initializerNode.accept(new SideEffectFinder(sideEffectNodes));
				performRemove= sideEffectNodes.isEmpty();
			}
			if (performRemove) {
				if (ASTNodes.isControlStatementBody(statementNode.getLocationInParent())) {
					rewrite.replace(statementNode, rewrite.getAST().newBlock(), group);
				} else {
					rewrite.remove(statementNode, group);
				}
				fRemovedAssignmentsCount++;
			} else {
				ASTNode initNode = rewrite.createMoveTarget(initializerNode);
				ExpressionStatement statement = rewrite.getAST().newExpressionStatement((Expression) initNode);
				rewrite.replace(statementNode, statement, null);
				fAlteredAssignmentsCount++;
			}
		}

		public String getAdditionalInfo() {
			StringBuffer sb=new StringBuffer();
			if (fRemovedAssignmentsCount>0){
				sb.append(Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_RemovedAssignments_preview,String.valueOf(fRemovedAssignmentsCount)));
			}
			if (fAlteredAssignmentsCount>0){
				sb.append(Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_AlteredAssignments_preview,String.valueOf(fAlteredAssignmentsCount)));
			}
			if (sb.length()>0) {
				return sb.toString();
			} else
				return null;
		}
	}
	
	public static UnusedCodeFix createRemoveUnusedImportFix(JavaScriptUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
		    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound) {
			
			ImportDeclaration node= getImportDeclaration(problem, compilationUnit);
			if (node != null) {
				String label= FixMessages.UnusedCodeFix_RemoveImport_description;
				RemoveImportOperation operation= new RemoveImportOperation(node);
				Map options= new Hashtable();
				options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpConstants.TRUE);
				return new UnusedCodeFix(label, compilationUnit, new IFixRewriteOperation[] {operation}, options);
			}
		}
		return null;
	}
	
	public static UnusedCodeFix createUnusedMemberFix(JavaScriptUnit compilationUnit, IProblemLocation problem, boolean forceInitializerRemoval) {
		int id= problem.getProblemId();
		if (id == IProblem.UnusedPrivateMethod || id == IProblem.UnusedPrivateConstructor || id == IProblem.UnusedPrivateField ||
		    id == IProblem.UnusedPrivateType || id == IProblem.LocalVariableIsNeverUsed || id == IProblem.ArgumentIsNeverUsed) {
			
			SimpleName name= getUnusedName(compilationUnit, problem);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					if (isFormalParameterInEnhancedForStatement(name))
						return null;
						
					String label= getDisplayString(name, binding, forceInitializerRemoval);
					RemoveUnusedMemberOperation operation= new RemoveUnusedMemberOperation(new SimpleName[] {name}, forceInitializerRemoval);
					return new UnusedCodeFix(label, compilationUnit, new IFixRewriteOperation[] {operation}, getCleanUpOptions(binding));
				}
			}
		}
		return null;
	}
	
	public static IFix createCleanUp(JavaScriptUnit compilationUnit, 
			boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, 
			boolean removeUnusedPrivateFields, 
			boolean removeUnusedPrivateTypes, 
			boolean removeUnusedLocalVariables, 
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		
		return createCleanUp(compilationUnit, locations, 
				removeUnusedPrivateMethods, 
				removeUnusedPrivateConstructors, 
				removeUnusedPrivateFields, 
				removeUnusedPrivateTypes, 
				removeUnusedLocalVariables, 
				removeUnusedImports,
				removeUnusedCast);
	}
	
	public static IFix createCleanUp(JavaScriptUnit compilationUnit, IProblemLocation[] problems, 
			boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, 
			boolean removeUnusedPrivateFields, 
			boolean removeUnusedPrivateTypes, 
			boolean removeUnusedLocalVariables, 
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		List/*<IFixRewriteOperation>*/ result= new ArrayList();
		Hashtable/*<ASTNode, List>*/ variableDeclarations= new Hashtable();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= problems[i];
			int id= problem.getProblemId();
			
			if (removeUnusedImports && (id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
				    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound)) 
			{
				ImportDeclaration node= UnusedCodeFix.getImportDeclaration(problem, compilationUnit);
				if (node != null) {
					result.add(new RemoveImportOperation(node));
				}
			}

			if ((removeUnusedPrivateMethods && id == IProblem.UnusedPrivateMethod) || (removeUnusedPrivateConstructors && id == IProblem.UnusedPrivateConstructor) ||
			    (removeUnusedPrivateTypes && id == IProblem.UnusedPrivateType)) {
				
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null) {
						result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}, false));
					}
				}
			}
			
			if ((removeUnusedLocalVariables && id == IProblem.LocalVariableIsNeverUsed) ||  (removeUnusedPrivateFields && id == IProblem.UnusedPrivateField)) {
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null && !isFormalParameterInEnhancedForStatement(name) && isSideEffectFree(name, compilationUnit)) {
						VariableDeclarationFragment parent= (VariableDeclarationFragment)ASTNodes.getParent(name, VariableDeclarationFragment.class);
						if (parent != null) {
							ASTNode varDecl= parent.getParent();
							if (!variableDeclarations.containsKey(varDecl)) {
								variableDeclarations.put(varDecl, new ArrayList());
							}
							((List)variableDeclarations.get(varDecl)).add(name);
						} else {
							result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}, false));
						}
					}
				}
			}
		}
		for (Iterator iter= variableDeclarations.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode)iter.next();
			List names= (List)variableDeclarations.get(node);
			result.add(new RemoveUnusedMemberOperation((SimpleName[])names.toArray(new SimpleName[names.size()]), false));
		}
		
		if (result.size() == 0)
			return null;
		
		return new UnusedCodeFix(FixMessages.UnusedCodeFix_change_name, compilationUnit, (IFixRewriteOperation[])result.toArray(new IFixRewriteOperation[result.size()]));
	}
	
	private static boolean isFormalParameterInEnhancedForStatement(SimpleName name) {
		return name.getParent() instanceof SingleVariableDeclaration && name.getParent().getLocationInParent() == EnhancedForStatement.PARAMETER_PROPERTY;
	}
	
	private static boolean isSideEffectFree(SimpleName simpleName, JavaScriptUnit completeRoot) {
		SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
		SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
		for (int i= 0; i < references.length; i++) {
			if (hasSideEffect(references[i]))
				return false;
		}
		return true;
	}

	private static boolean hasSideEffect(SimpleName reference) {
		ASTNode parent= reference.getParent();
		while (parent instanceof QualifiedName) {
			parent= parent.getParent();
		}
		if (parent instanceof FieldAccess) {
			parent= parent.getParent();
		}

		ASTNode node= null;
		int nameParentType= parent.getNodeType();
		if (nameParentType == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) parent;
			node= assignment.getRightHandSide();
		} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration)parent;
			node= decl.getInitializer();
			if (node == null)
				return false;
		} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= parent;
		} else {
			return false;
		}		

		ArrayList sideEffects= new ArrayList();
		node.accept(new SideEffectFinder(sideEffects));
		return sideEffects.size() > 0;
	}

	private static SimpleName getUnusedName(JavaScriptUnit compilationUnit, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		if (selectedNode instanceof FunctionDeclaration) {
			return ((FunctionDeclaration) selectedNode).getName();
		} else if (selectedNode instanceof SimpleName) {
			return (SimpleName) selectedNode;
		}
		
		return null;
	}
	
	private static String getDisplayString(SimpleName simpleName, IBinding binding, boolean forceRemoveInitializer) {
		String name= simpleName.getIdentifier();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return Messages.format(FixMessages.UnusedCodeFix_RemoveType_description, name);
			case IBinding.METHOD:
				if (((IFunctionBinding) binding).isConstructor()) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveConstructor_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveMethod_description, name);
				}
			case IBinding.VARIABLE:
				if (forceRemoveInitializer) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocalWithInitializer_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				}
			default:
				return ""; //$NON-NLS-1$
		}
	}
	
	private static Map getCleanUpOptions(IBinding binding) {
		Map result= new Hashtable();
		
		result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpConstants.TRUE);		
		switch (binding.getKind()) {
			case IBinding.TYPE:
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpConstants.TRUE);
				break;
			case IBinding.METHOD:
				if (((IFunctionBinding) binding).isConstructor()) {
					result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpConstants.TRUE);
				} else {
					result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpConstants.TRUE);
				}
				break;
			case IBinding.VARIABLE:
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpConstants.TRUE);
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpConstants.TRUE);
				break;
		}

		return result;
	}
	
	private static ImportDeclaration getImportDeclaration(IProblemLocation problem, JavaScriptUnit compilationUnit) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode != null) {
			ASTNode node= ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (node instanceof ImportDeclaration) {
				return (ImportDeclaration)node;
			}
		}
		return null;
	}
	
	private final Map fCleanUpOptions;
	
	private UnusedCodeFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		this(name, compilationUnit, fixRewriteOperations, null);
	}
	
	private UnusedCodeFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations, Map options) {
		super(name, compilationUnit, fixRewriteOperations);
		if (options == null) {
			fCleanUpOptions= new Hashtable();			
		} else {
			fCleanUpOptions= options;
		}
	}

	public UnusedCodeCleanUp getCleanUp() {
		return new UnusedCodeCleanUp(fCleanUpOptions);
	}

}
