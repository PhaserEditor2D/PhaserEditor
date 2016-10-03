/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.VariableDeclarationRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ASTResolving;

public class VariableDeclarationFix extends AbstractFix {
	
	private static class WrittenNamesFinder extends GenericVisitor {
		
		private final HashMap fResult;
	
		public WrittenNamesFinder(HashMap result) {
			fResult= result;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(SimpleName node) {	
			if (node.getParent() instanceof VariableDeclarationFragment)
				return super.visit(node);
			if (node.getParent() instanceof SingleVariableDeclaration)
				return super.visit(node);

			IBinding binding= node.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return super.visit(node);
			
			binding= ((IVariableBinding)binding).getVariableDeclaration();
			if (ASTResolving.isWriteAccess(node)) {
				List list;
				if (fResult.containsKey(binding)) {
					list= (List)fResult.get(binding);
				} else {
					list= new ArrayList();
				}
				list.add(node);
				fResult.put(binding, list);
			}
			
			return super.visit(node);
		}
	}

	private static class VariableDeclarationFinder extends GenericVisitor {
		
		private final JavaScriptUnit fCompilationUnit;
		private final List fResult;
		private final HashMap fWrittenVariables;
		private final boolean fAddFinalFields;
		private final boolean fAddFinalParameters;
		private final boolean fAddFinalLocals;
		
		public VariableDeclarationFinder(boolean addFinalFields, 
				boolean addFinalParameters, 
				boolean addFinalLocals, 
				final JavaScriptUnit compilationUnit, final List result, final HashMap writtenNames) {
			
			super();
			fAddFinalFields= addFinalFields;
			fAddFinalParameters= addFinalParameters;
			fAddFinalLocals= addFinalLocals;
			fCompilationUnit= compilationUnit;
			fResult= result;
			fWrittenVariables= writtenNames;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(FieldDeclaration node) {
			if (fAddFinalFields)
				return handleFragments(node.fragments(), node);
			
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(VariableDeclarationStatement node) {
			if (fAddFinalLocals)
				return handleFragments(node.fragments(), node);
			
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(VariableDeclarationExpression node) {
			if (fAddFinalLocals && node.fragments().size() == 1) {
				SimpleName name= ((VariableDeclarationFragment)node.fragments().get(0)).getName();
				
				IBinding binding= name.resolveBinding();
				if (binding == null)
					return false;
				
				if (fWrittenVariables.containsKey(binding))
					return false;
				
				ModifierChangeOperation op= createAddFinalOperation(name, fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			}
			return false;
		}
		
		private boolean handleFragments(List list, ASTNode declaration) {
			List toChange= new ArrayList();
			
			for (Iterator iter= list.iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
				SimpleName name= fragment.getName();
				IBinding resolveBinding= name.resolveBinding();
				if (canAddFinal(resolveBinding, name, declaration)) {
					IVariableBinding varbinding= (IVariableBinding)resolveBinding;
					if (varbinding.isField()) {
						if (fieldCanBeFinal(fragment, varbinding))
							toChange.add(fragment);
					} else {
						if (!fWrittenVariables.containsKey(resolveBinding))
							toChange.add(fragment);
					}
				}
			}
			
			if (toChange.size() == 0)
				return false;
			
			ModifierChangeOperation op= new ModifierChangeOperation(declaration, toChange, Modifier.FINAL, Modifier.VOLATILE);
			fResult.add(op);
			return false;
		}
		
		private boolean fieldCanBeFinal(VariableDeclarationFragment fragment, IVariableBinding binding) {
			if (Modifier.isStatic(((FieldDeclaration)fragment.getParent()).getModifiers()))
				return false;

			if (!fWrittenVariables.containsKey(binding)) {
				//variable is not written
				if (fragment.getInitializer() == null) {//variable is not initialized
					return false;
				} else {
					return true;
				}
			}
			
			if (fragment.getInitializer() != null)//variable is initialized and written
				return false;
			
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (declaringClass == null)
				return false;
			
			ArrayList writes= (ArrayList)fWrittenVariables.get(binding);
			if (!isWrittenInTypeConstructors(binding, writes, declaringClass))
				return false;
				
			HashSet writingConstructorBindings= new HashSet();
			ArrayList writingConstructors= new ArrayList();
			for (int i= 0; i < writes.size(); i++) {
	            SimpleName name= (SimpleName)writes.get(i);
	            FunctionDeclaration constructor= getWritingConstructor(name);
	            if (writingConstructors.contains(constructor))//variable is written twice or more in constructor
	            	return false;
	            
	            writingConstructors.add(constructor);
	            IFunctionBinding constructorBinding= constructor.resolveBinding();
	            if (constructorBinding == null)
	            	return false;
	            
				writingConstructorBindings.add(constructorBinding);
            }
			
			for (int i= 0; i < writingConstructors.size(); i++) {
	            FunctionDeclaration constructor= (FunctionDeclaration)writingConstructors.get(i);
	            if (callsWrittingConstructor(constructor, writingConstructorBindings))//writing constructor calls other writing constructor
	            	return false;
            }
			
			FunctionDeclaration constructor= (FunctionDeclaration)writingConstructors.get(0);
			TypeDeclaration typeDecl= (TypeDeclaration)ASTNodes.getParent(constructor, TypeDeclaration.class);
			if (typeDecl == null)
				return false;
			
			FunctionDeclaration[] methods= typeDecl.getMethods();
			for (int i= 0; i < methods.length; i++) {
	            if (methods[i].isConstructor()) {
	            	IFunctionBinding methodBinding= methods[i].resolveBinding();
	            	if (methodBinding == null)
	            		return false;
	            	
	            	if (!writingConstructorBindings.contains(methodBinding)) {
	            		if (!callsWrittingConstructor(methods[i], writingConstructorBindings))//non writing constructor does not call a writing constructor
	            			return false;
	            	}
	            }
            }
			
	        return true;
        }

		private boolean callsWrittingConstructor(FunctionDeclaration methodDeclaration, HashSet writingConstructorBindings) {
			Block body= methodDeclaration.getBody();
			if (body == null)
				return false;
			
			List statements= body.statements();
			if (statements.size() == 0)
				return false;
			
			Statement statement= (Statement)statements.get(0);
			if (!(statement instanceof ConstructorInvocation))
				return false;
			
			ConstructorInvocation invocation= (ConstructorInvocation)statement;
			IFunctionBinding constructorBinding= invocation.resolveConstructorBinding();
			if (constructorBinding == null)
				return false;
			
			if (writingConstructorBindings.contains(constructorBinding)) {
				return true;
			} else {
				ASTNode declaration= ASTNodes.findDeclaration(constructorBinding, methodDeclaration.getParent());
				if (!(declaration instanceof FunctionDeclaration))
					return false;
				
				return callsWrittingConstructor((FunctionDeclaration)declaration, writingConstructorBindings);
			}
        }

		private boolean isWrittenInTypeConstructors(IVariableBinding binding, ArrayList writes, ITypeBinding declaringClass) {
			
			for (int i= 0; i < writes.size(); i++) {
	            SimpleName name= (SimpleName)writes.get(i);
	            
	            FunctionDeclaration methodDeclaration= getWritingConstructor(name);
	            if (methodDeclaration == null)
	            	return false;
	            
	            if (!methodDeclaration.isConstructor())
	            	return false;
	            
	            IFunctionBinding constructor= methodDeclaration.resolveBinding();
	            if (constructor == null)
	            	return false;
	            
	            ITypeBinding declaringClass2= constructor.getDeclaringClass();
	            if (!declaringClass.equals(declaringClass2))
	            	return false;
            }
			
	        return true;
        }

		private FunctionDeclaration getWritingConstructor(SimpleName name) {
			Assignment assignement= (Assignment)ASTNodes.getParent(name, Assignment.class);
			if (assignement == null)
				return null;
			
			ASTNode expression= assignement.getParent();
			if (!(expression instanceof ExpressionStatement))
				return null;
			
			ASTNode block= expression.getParent();
			if (!(block instanceof Block))
				return null;
			
			ASTNode methodDeclaration= block.getParent();
			if (!(methodDeclaration instanceof FunctionDeclaration))
				return null;
			
	        return (FunctionDeclaration)methodDeclaration;
        }

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(VariableDeclarationFragment node) {
			SimpleName name= node.getName();
			
			IBinding binding= name.resolveBinding();
			if (binding == null)
				return false;
			
			if (fWrittenVariables.containsKey(binding))
				return false;
			
			ModifierChangeOperation op= createAddFinalOperation(name, fCompilationUnit, node);
			if (op == null)
				return false;
			
			fResult.add(op);
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(SingleVariableDeclaration node) {
			SimpleName name= node.getName();

			IBinding binding= name.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return false;

			IVariableBinding varBinding= (IVariableBinding)binding;
			if (fWrittenVariables.containsKey(varBinding))
				return false;
			
			if (fAddFinalParameters && fAddFinalLocals) {
				
				ModifierChangeOperation op= createAddFinalOperation(name, fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			} else if (fAddFinalParameters) {	
				if (!varBinding.isParameter())
					return false;
					
				ModifierChangeOperation op= createAddFinalOperation(name, fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			} else if (fAddFinalLocals) {
				if (varBinding.isParameter())
					return false;
				
				ModifierChangeOperation op= createAddFinalOperation(name, fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			}
			return false;
		}
	}
	
	private static class ModifierChangeOperation extends AbstractFixRewriteOperation {
		
		private final ASTNode fDeclaration;
		private final List fToChange;
		private final int fIncludedModifiers;
		private final int fExcludedModifiers;

		public ModifierChangeOperation(ASTNode declaration, List toChange, int includedModifiers, int excludedModifiers) {
			fDeclaration= declaration;
			fToChange= toChange;
			fIncludedModifiers= includedModifiers;
			fExcludedModifiers= excludedModifiers;	
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			
			TextEditGroup group= createTextEditGroup(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description);
			textEditGroups.add(group);
			
			if (fDeclaration instanceof VariableDeclarationStatement) {
				VariableDeclarationFragment[] toChange= (VariableDeclarationFragment[])fToChange.toArray(new VariableDeclarationFragment[fToChange.size()]);
				VariableDeclarationRewrite.rewriteModifiers((VariableDeclarationStatement)fDeclaration, toChange, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof FieldDeclaration) {
				VariableDeclarationFragment[] toChange= (VariableDeclarationFragment[])fToChange.toArray(new VariableDeclarationFragment[fToChange.size()]);
				VariableDeclarationRewrite.rewriteModifiers((FieldDeclaration)fDeclaration, toChange, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof SingleVariableDeclaration) {
				VariableDeclarationRewrite.rewriteModifiers((SingleVariableDeclaration)fDeclaration, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof VariableDeclarationExpression) {
				VariableDeclarationRewrite.rewriteModifiers((VariableDeclarationExpression)fDeclaration, fIncludedModifiers, fExcludedModifiers, rewrite, group);	
			}
		}
	}
	
	public static IFix createChangeModifierToFinalFix(final JavaScriptUnit compilationUnit, ASTNode[] selectedNodes) {
		HashMap writtenNames= new HashMap(); 
		WrittenNamesFinder finder= new WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);
		List ops= new ArrayList();
		VariableDeclarationFinder visitor= new VariableDeclarationFinder(true, true, true, compilationUnit, ops, writtenNames);
		if (selectedNodes.length == 1) {
			if (selectedNodes[0] instanceof SimpleName) {
				selectedNodes[0]= selectedNodes[0].getParent();
			}
			selectedNodes[0].accept(visitor);
		} else {
			for (int i= 0; i < selectedNodes.length; i++) {
				ASTNode selectedNode= selectedNodes[i];
				selectedNode.accept(visitor);
			}
		}
		if (ops.size() == 0)
			return null;
		
		IFixRewriteOperation[] result= (IFixRewriteOperation[])ops.toArray(new IFixRewriteOperation[ops.size()]);
		String label;
		if (result.length == 1) {
			label= FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description;
		} else {
			label= FixMessages.VariableDeclarationFix_ChangeMidifiersToFinalWherPossible_description;
		}
		return new VariableDeclarationFix(label, compilationUnit, result);
	}
	
	public static IFix createCleanUp(JavaScriptUnit compilationUnit,
			boolean addFinalFields, boolean addFinalParameters, boolean addFinalLocals) {
		
		if (!addFinalFields && !addFinalParameters && !addFinalLocals)
			return null;
		
		HashMap writtenNames= new HashMap(); 
		WrittenNamesFinder finder= new WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);
		
		List operations= new ArrayList();
		VariableDeclarationFinder visitor= new VariableDeclarationFinder(addFinalFields, addFinalParameters, addFinalLocals, compilationUnit, operations, writtenNames);
		compilationUnit.accept(visitor);
		
		if (operations.isEmpty())
			return null;
			
		return new VariableDeclarationFix(FixMessages.VariableDeclarationFix_add_final_change_name, compilationUnit, (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]));
	}
	
	private static ModifierChangeOperation createAddFinalOperation(SimpleName name, JavaScriptUnit compilationUnit, ASTNode decl) {
		if (decl == null)
			return null;
		
		IBinding binding= name.resolveBinding();
		if (!canAddFinal(binding, name, decl))
			return null;
		
		if (decl instanceof SingleVariableDeclaration) {
			return new ModifierChangeOperation(decl, new ArrayList(), Modifier.FINAL, Modifier.VOLATILE);
		} else if (decl instanceof VariableDeclarationExpression) {
			return new ModifierChangeOperation(decl, new ArrayList(), Modifier.FINAL, Modifier.VOLATILE);
		} else if (decl instanceof VariableDeclarationFragment){
			VariableDeclarationFragment frag= (VariableDeclarationFragment)decl;
			decl= decl.getParent();
			if (decl instanceof FieldDeclaration || decl instanceof VariableDeclarationStatement) {
				List list= new ArrayList();
				list.add(frag);
				return new ModifierChangeOperation(decl, list, Modifier.FINAL, Modifier.VOLATILE);
			} else if (decl instanceof VariableDeclarationExpression) {
				return new ModifierChangeOperation(decl, new ArrayList(), Modifier.FINAL, Modifier.VOLATILE);
			}
		}
		
		return null;
	}

	private static boolean canAddFinal(IBinding binding, SimpleName name, ASTNode declNode) {
		if (!(binding instanceof IVariableBinding))
			return false;

		IVariableBinding varbinding= (IVariableBinding)binding;
		if (Modifier.isFinal(varbinding.getModifiers()))
			return false;
		
		ASTNode parent= ASTNodes.getParent(declNode, VariableDeclarationExpression.class);
		if (parent != null && ((VariableDeclarationExpression)parent).fragments().size() > 1)
			return false;
		
		if (varbinding.isField() && !Modifier.isPrivate(varbinding.getModifiers())) 
			return false;
		
		if (varbinding.isParameter()) {
			ASTNode varDecl= declNode.getParent();
			if (varDecl instanceof FunctionDeclaration) {
				FunctionDeclaration declaration= (FunctionDeclaration)varDecl;
				if (declaration.getBody() == null)
					return false;
			}
		}
		
		return true;
	}

	protected VariableDeclarationFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
