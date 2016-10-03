/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 * 	     o bug "Inline refactoring showed bogus error" (see bugzilla
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=42753)
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.ImportReferencesCollector;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;

class SourceAnalyzer  {
	
	public static class NameData {
		private String fName;
		private List fReferences;
		public NameData(String n) {
			fName= n;
			fReferences= new ArrayList(2);
		}
		public String getName() {
			return fName;
		}
		public void addReference(SimpleName ref) {
			fReferences.add(ref);
		}
		public List references() {
			return fReferences;
		}
	}

	private class ActivationAnalyzer extends ASTVisitor {
		public RefactoringStatus status= new RefactoringStatus();
		private ASTNode fLastNode= getLastNode();
		private IFunctionBinding fBinding= getBinding();
		public boolean visit(ReturnStatement node) {
			if (node != fLastNode) {
				fInterruptedExecutionFlow= true;
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			return false;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}
		public boolean visit(FunctionInvocation node) {
			IFunctionBinding methodBinding= node.resolveMethodBinding();
			if (methodBinding != null)
				methodBinding.getMethodDeclaration();
			if (fBinding != null && methodBinding != null && fBinding.isEqualTo(methodBinding) && !status.hasFatalError()) {
				status.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_recursive_call); 
				return false;
			}
			return true;
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding == null && !status.hasFatalError()) {
				// fixes bug #42753
				if (!ASTNodes.isLabel(node)) {
					status.addFatalError(
						RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_declaration_has_errors, 
						JavaStatusContext.create(fTypeRoot, fDeclaration));
					return false;
				}
			}
			return true;
		}
		public boolean visit(ThisExpression node) {
			if (node.getQualifier() != null) {
				status.addFatalError(
					RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_qualified_this_expressions, 
					JavaStatusContext.create(fTypeRoot, node));
				return false;
			}
			return true;
		}
		private ASTNode getLastNode() {
			List statements= fDeclaration.getBody().statements();
			if (statements.size() == 0)
				return null;
			return (ASTNode)statements.get(statements.size() - 1);
		}
		private IFunctionBinding getBinding() {
			IFunctionBinding result= fDeclaration.resolveBinding();
			if (result != null)
				return result.getMethodDeclaration();
			return result;
		}
	}
	
	private class UpdateCollector extends ASTVisitor {
		private int fTypeCounter;
		public boolean visit(TypeDeclaration node) {
			return visitType(node);
		}
		public void endVisit(TypeDeclaration node) {
			fTypeCounter--;
		}
		private boolean visitType(AbstractTypeDeclaration node) {
			if (fTypeCounter++ == 0) {
				addNameReference(node.getName());
			}
			return true;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			fTypeCounter++;
			return true;
		}
		public void endVisit(AnonymousClassDeclaration node) {
			fTypeCounter--;
		}
		public boolean visit(FieldAccess node) {
			// only visit the expression and not the simple name
			node.getExpression().accept(this);
			addReferencesToName(node.getName());
			return false;
		}
		public boolean visit(FunctionDeclaration node) {
			if (node.isConstructor()) {
				AbstractTypeDeclaration decl= (AbstractTypeDeclaration) ASTNodes.getParent(node, AbstractTypeDeclaration.class);
				NameData name= (NameData)fNames.get(decl.getName().resolveBinding());
				if (name != null) {
					name.addReference(node.getName());
				}
			}
			return true;
		}
		public boolean visit(FunctionInvocation node) {
			if (fTypeCounter == 0) {
				Expression receiver= node.getExpression();
				if (receiver == null && !isStaticallyImported(node.getName())) {
					fImplicitReceivers.add(node);
				}
			}
			return true;
		}
		public boolean visit(SuperMethodInvocation node) {
			if (fTypeCounter == 0) {
				fHasSuperMethodInvocation= true;
			}
			return true;
		}
		public boolean visist(SuperConstructorInvocation node) {
			if (fTypeCounter == 0) {
				fHasSuperMethodInvocation= true;
			}
			return true;
		}
		public boolean visit(ClassInstanceCreation node) {
			if (fTypeCounter == 0) {
				Expression receiver= node.getExpression();
				if (receiver == null) {
					if (node.resolveTypeBinding().isLocal())
						fImplicitReceivers.add(node);
				}
			}
			return true;
		}
		public boolean visit(SingleVariableDeclaration node) {
			if (fTypeCounter == 0)
				addNameReference(node.getName());
			return true;
		}
		public boolean visit(VariableDeclarationFragment node) {
			if (fTypeCounter == 0)
				addNameReference(node.getName());
			return true;
		}
		public boolean visit(SimpleName node) {
			addReferencesToName(node);
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)binding;
				if (vb.isField() && ! isStaticallyImported(node)) {
					Name topName= ASTNodes.getTopMostName(node);
					if (node == topName || node == ASTNodes.getLeftMostSimpleName(topName)) {
						StructuralPropertyDescriptor location= node.getLocationInParent();
						if (location != SingleVariableDeclaration.NAME_PROPERTY 
							&& location != VariableDeclarationFragment.NAME_PROPERTY) {
							fImplicitReceivers.add(node);
						}
					}
				} else if (!vb.isField()) {
					// we have a local. Check if it is a parameter.
					ParameterData data= (ParameterData)fParameters.get(binding);
					if (data != null) {
						ASTNode parent= node.getParent();
						if (parent instanceof Expression) {
							int precedence= OperatorPrecedence.getValue((Expression)parent);
							if (precedence != -1) {
								data.setOperatorPrecedence(node, precedence);
							}
						}
					}
				}
			}
			return true;
		}
		public boolean visit(ThisExpression node) {
			if (fTypeCounter == 0) {
				fImplicitReceivers.add(node);
			}
			return true;
		}
		private void addReferencesToName(SimpleName node) {
			IBinding binding= node.resolveBinding();
			ParameterData data= (ParameterData)fParameters.get(binding);
			if (data != null)
				data.addReference(node);
				
			NameData name= (NameData)fNames.get(binding);
			if (name != null)
				name.addReference(node);
		}
		private void addNameReference(SimpleName name) {
			fNames.put(name.resolveBinding(), new NameData(name.getIdentifier()));
		}
		private void addTypeVariableReference(ITypeBinding variable, SimpleName name) {
			NameData data= (NameData)fTypeParameterMapping.get(variable);
			if (data == null) {
				data= (NameData)fMethodTypeParameterMapping.get(variable);
			}
			data.addReference(name);
		}
		private boolean isStaticallyImported(Name name) {
			return fStaticsToImport.contains(name);
		}
	}
	
	private class VarargAnalyzer extends ASTVisitor {
		private IBinding fParameter;
		public VarargAnalyzer(IBinding parameter) {
			fParameter= parameter;
		}
		public boolean visit(ArrayAccess node) {
			Expression array= node.getArray();
			if (array instanceof SimpleName && fParameter.isEqualTo(((SimpleName)array).resolveBinding())) {
				fArrayAccess= true;
			}
			return true;
		}
	}

	private ITypeRoot fTypeRoot;
	private FunctionDeclaration fDeclaration;
	private Map fParameters;
	private Map fNames;
	private List fImplicitReceivers;
	
	private boolean fArrayAccess;
	private boolean fHasSuperMethodInvocation;
	
	private List/*<Name>*/ fTypesToImport;
	private List/*<Name>*/ fStaticsToImport;
	
	private List/*<NameData>*/ fTypeParameterReferences;
	private Map/*<ITypeBinding, NameData>*/ fTypeParameterMapping;
	
	private List/*<NameData>*/ fMethodTypeParameterReferences;
	private Map/*<ITypeBinding, NameData>*/ fMethodTypeParameterMapping;
	
	private boolean fInterruptedExecutionFlow;

	public SourceAnalyzer(ITypeRoot typeRoot, FunctionDeclaration declaration) {
		super();
		fTypeRoot= typeRoot;
		fDeclaration= declaration;
	}
	
	public boolean isExecutionFlowInterrupted() {
		return fInterruptedExecutionFlow;
	}
	
	public RefactoringStatus checkActivation() throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (!fTypeRoot.isStructureKnown()) {
			result.addFatalError(		
				RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_syntax_errors, 
				JavaStatusContext.create(fTypeRoot));		
			return result;
		}
		IProblem[] problems= ASTNodes.getProblems(fDeclaration, ASTNodes.NODE_ONLY, ASTNodes.ERROR);
		if (problems.length > 0) {
			result.addFatalError(		
				RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_declaration_has_errors, 
				JavaStatusContext.create(fTypeRoot, fDeclaration));		
			return result;
		}
		final IFunctionBinding declarationBinding= fDeclaration.resolveBinding();
		if (declarationBinding != null) {
			final int modifiers= declarationBinding.getModifiers();
			if (Modifier.isAbstract(modifiers)) {
				result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_abstract_methods, JavaStatusContext.create(fTypeRoot, fDeclaration));
				return result;
			} else if (Modifier.isNative(modifiers)) {
				result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_native_methods, JavaStatusContext.create(fTypeRoot, fDeclaration));
				return result;
			}
		} else {
			result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_methoddeclaration_has_errors, JavaStatusContext.create(fTypeRoot));
			return result;
		}
		ActivationAnalyzer analyzer= new ActivationAnalyzer();
		fDeclaration.accept(analyzer);
		result.merge(analyzer.status);
		if (!result.hasFatalError()) {
			List parameters= fDeclaration.parameters();
			fParameters= new HashMap(parameters.size() * 2);
			for (Iterator iter= parameters.iterator(); iter.hasNext();) {
				SingleVariableDeclaration element= (SingleVariableDeclaration) iter.next();
				IVariableBinding binding= element.resolveBinding();
				if (binding == null) {
					result.addFatalError(
						RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_declaration_has_errors, 
						JavaStatusContext.create(fTypeRoot, fDeclaration));
					return result;
				}
				fParameters.put(binding, element.getProperty(ParameterData.PROPERTY));
			}
			fNames= new HashMap();
			fImplicitReceivers= new ArrayList(2);
			
			fTypeParameterReferences= new ArrayList(0);
			fTypeParameterMapping= new HashMap();
			ITypeBinding declaringType= declarationBinding.getDeclaringClass();
			if (declaringType == null) {
				result.addFatalError(
					RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_typedeclaration_has_errors, 
					JavaStatusContext.create(fTypeRoot));
				return result;
			}
			
			fMethodTypeParameterReferences= new ArrayList(0);
			fMethodTypeParameterMapping= new HashMap();		
		}
		if (fDeclaration.isVarargs()) {
			List parameters= fDeclaration.parameters();
			VarargAnalyzer vAnalyzer= new VarargAnalyzer(
				((SingleVariableDeclaration)parameters.get(parameters.size() - 1)).getName().resolveBinding());
			fDeclaration.getBody().accept(vAnalyzer);
		}
		return result;
	}

	public void initialize() {
		Block body= fDeclaration.getBody();
		// first collect the static imports. This is necessary to not mark
		// static imported fields and methods as implicit visible.
		fTypesToImport= new ArrayList();
		fStaticsToImport= new ArrayList();
		ImportReferencesCollector collector= new ImportReferencesCollector(
			fTypeRoot.getJavaScriptProject(), null, fTypesToImport, fStaticsToImport);
		body.accept(collector);
		
		// Now collect implicit references and name references
		body.accept(new UpdateCollector());
		
		int numberOfLocals= LocalVariableIndex.perform(fDeclaration);
		FlowContext context= new FlowContext(0, numberOfLocals + 1);
		context.setConsiderAccessMode(true);
		context.setComputeMode(FlowContext.MERGE);
		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(context);
		FlowInfo info= flowAnalyzer.perform(getStatements());
		
		for (Iterator iter= fDeclaration.parameters().iterator(); iter.hasNext();) {
			SingleVariableDeclaration element= (SingleVariableDeclaration) iter.next();
			IVariableBinding binding= element.resolveBinding();
			ParameterData data= (ParameterData)element.getProperty(ParameterData.PROPERTY);
			data.setAccessMode(info.getAccessMode(context, binding));
		}
	}
	
	public Collection getUsedNames() {
		return fNames.values();
	}
	
	public List getImplicitReceivers() {
		return fImplicitReceivers;
	}
	
	public List getTypesToImport() {
		return fTypesToImport;
	}
	
	public List getStaticsToImport() {
		return fStaticsToImport;
	}
	
	public List getTypeParameterReferences() {
		return fTypeParameterReferences;
	}
	
	public List getMethodTypeParameterReferences() {
		return fMethodTypeParameterReferences;
	}
	
	public boolean hasArrayAccess() {
		return fArrayAccess;
	}
	
	public boolean hasSuperMethodInvocation() {
		return fHasSuperMethodInvocation;
	}
	
	private ASTNode[] getStatements() {
		List statements= fDeclaration.getBody().statements();
		return (ASTNode[]) statements.toArray(new ASTNode[statements.size()]);
	}

}
