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
 *       o bug "inline method - doesn't handle implicit cast" (see
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *       o bug inline method: compile error (array related)
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38471)
 *       o inline call that is used in a field initializer 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *       o inline call a field initializer: could detect self reference 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=44417)
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public class CallInliner {

	private IJavaScriptUnit fCUnit;
	private ASTRewrite fRewrite;
	private ImportRewrite fImportRewrite;
	private ITextFileBuffer fBuffer;
	private SourceProvider fSourceProvider;
	private TypeEnvironment fTypeEnvironment;
	
	private BodyDeclaration fBodyDeclaration;
	private CodeScopeBuilder.Scope fRootScope;
	private int fNumberOfLocals;
	
	private ASTNode fInvocation;
	
	private int fInsertionIndex;
	private ListRewrite fListRewrite;
	
	private boolean fNeedsStatement;
	private ASTNode fTargetNode;
	private FlowContext fFlowContext;
	private FlowInfo fFlowInfo;
	private CodeScopeBuilder.Scope fInvocationScope;
	private boolean fFieldInitializer;
	private List fLocals;
	private CallContext fContext;
	
	private class InlineEvaluator extends HierarchicalASTVisitor {
		private ParameterData fFormalArgument;
		private boolean fResult;
		public InlineEvaluator(ParameterData argument) {
			fFormalArgument= argument;
		}
		public boolean getResult() {
			return fResult;
		}
		private boolean setResult(boolean result) {
			fResult= result;
			return false;
		}
		public boolean visit(Expression node) {
			int accessMode= fFormalArgument.getSimplifiedAccessMode();
			if (accessMode == FlowInfo.WRITE)
				return setResult(false);
			if (accessMode == FlowInfo.UNUSED)
				return setResult(true);
			if (ASTNodes.isLiteral(node))
				return setResult(true);
			return setResult(fFormalArgument.getNumberOfAccesses() <= 1);
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding) {
				int accessMode = fFormalArgument.getSimplifiedAccessMode();
				if (accessMode == FlowInfo.READ || accessMode == FlowInfo.UNUSED)
					return setResult(true);
				// from now on we only have write accesses.
				IVariableBinding vb= (IVariableBinding)binding;
				if (vb.isField())
					return setResult(false);
				return setResult(fFlowInfo.hasAccessMode(fFlowContext, vb, FlowInfo.UNUSED | FlowInfo.WRITE));
			}
			return setResult(false);
		}
		public boolean visit(FieldAccess node) {
			return visit(node.getName());
		}
		public boolean visit(SuperFieldAccess node) {
			return visit(node.getName());
		}
		public boolean visit(ThisExpression node) {
			int accessMode= fFormalArgument.getSimplifiedAccessMode();
			if (accessMode == FlowInfo.READ || accessMode == FlowInfo.UNUSED)
				return setResult(true);
			return setResult(false);
		}
	}

//	private static class AmbiguousMethodAnalyzer implements TypeBindingVisitor {
//		private TypeEnvironment fTypeEnvironment;
//		private TType[] fTypes;
//		private IFunctionBinding fOriginal;
//
//		public AmbiguousMethodAnalyzer(TypeEnvironment typeEnvironment, IFunctionBinding original, TType[] types) {
//			fTypeEnvironment= typeEnvironment;
//			fOriginal= original;
//			fTypes= types;
//		}
//		public boolean visit(ITypeBinding node) {
//			IFunctionBinding[] methods= node.getDeclaredMethods();
//			for (int i= 0; i < methods.length; i++) {
//				IFunctionBinding candidate= methods[i];
//				if (candidate == fOriginal) {
//					continue;
//				}
//				if (fOriginal.getName().equals(candidate.getName())) {
//					if (canImplicitlyCall(candidate)) {
//						return false;
//					}
//				}
//			}
//			return true;
//		}
//		/**
//		 * Returns <code>true</code> if the method can be called without explicit casts; 
//		 * otherwise <code>false</code>.
//		 */
//		private boolean canImplicitlyCall(IFunctionBinding candidate) {
//			ITypeBinding[] parameters= candidate.getParameterTypes();
//			if (parameters.length != fTypes.length) {
//				return false;
//			}
//			for (int i= 0; i < parameters.length; i++) {
//				if (!fTypes[i].canAssignTo(fTypeEnvironment.create(parameters[i]))) {
//					return false;
//				}
//			}
//			return true;
//		}
//	}

	public CallInliner(IJavaScriptUnit unit, JavaScriptUnit targetAstRoot, SourceProvider provider) throws CoreException {
		super();
		fCUnit= unit;
		fBuffer= RefactoringFileBuffers.acquire(fCUnit);
		fSourceProvider= provider;
		fImportRewrite= StubUtility.createImportRewrite(targetAstRoot, true);
		fLocals= new ArrayList(3);
		fRewrite= ASTRewrite.create(targetAstRoot.getAST());
		fRewrite.setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());
		fTypeEnvironment= new TypeEnvironment();
	}

	public void dispose() {
		try {
			RefactoringFileBuffers.release(fCUnit);
		} catch (CoreException exception) {
			JavaScriptPlugin.log(exception);
		}
	}
	
	
	public ImportRewrite getImportEdit() {
		return fImportRewrite;
	}
	
	public ASTNode getTargetNode() {
		return fTargetNode;
	}
	
	public void initialize(BodyDeclaration declaration) {
		fBodyDeclaration= declaration;
		fRootScope= CodeScopeBuilder.perform(declaration, fSourceProvider.getDeclaration().resolveBinding());
		fNumberOfLocals= 0;
		switch (declaration.getNodeType()) {
			case ASTNode.FUNCTION_DECLARATION:
			case ASTNode.INITIALIZER:
				fNumberOfLocals= LocalVariableIndex.perform(declaration);
				break;
		}
	}

	public RefactoringStatus initialize(ASTNode invocation, int severity) {
		RefactoringStatus result= new RefactoringStatus();
		fInvocation= invocation;
		fLocals= new ArrayList(3);
		
		checkMethodDeclaration(result, severity);
		if (result.getSeverity() >= severity)
			return result;
		
		initializeRewriteState();
		initializeTargetNode();
		flowAnalysis();
		
		fContext= new CallContext(fInvocation, fInvocationScope, fTargetNode.getNodeType(), fImportRewrite);
		
		try {
			computeRealArguments();
			computeReceiver();
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		checkInvocationContext(result, severity);
		
		return result;
	}

	private void initializeRewriteState() {
		// field initializer can be inside of a block if used in a local class
		// but block can't be a child of field initializer
		if(ASTNodes.getParent(fInvocation, ASTNode.FIELD_DECLARATION) != null) {
			fFieldInitializer= true;
		}
	}

	private void initializeTargetNode() {
		ASTNode parent= fInvocation.getParent();
		int nodeType= parent.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT || nodeType == ASTNode.RETURN_STATEMENT) {
			fTargetNode= parent;
		} else {
			fTargetNode= fInvocation;
		}
	}

	// the checks depend on invocation context and therefore can't be done in SourceAnalyzer
	private void checkMethodDeclaration(RefactoringStatus result, int severity) {
		FunctionDeclaration methodDeclaration= fSourceProvider.getDeclaration();
		// it is not allowed to inline constructor invocation only if it is used for class instance creation
		// if constructor is invoked from another constructor then we can inline such invocation
		if (fInvocation.getNodeType() != ASTNode.CONSTRUCTOR_INVOCATION && methodDeclaration.isConstructor()) {
			result.addEntry(new RefactoringStatusEntry(
				severity,  
				RefactoringCoreMessages.CallInliner_constructors, 
				JavaStatusContext.create(fCUnit, fInvocation)));
		}
		if (fSourceProvider.hasSuperMethodInvocation() && fInvocation.getNodeType() == ASTNode.FUNCTION_INVOCATION) {
			Expression receiver= ((FunctionInvocation)fInvocation).getExpression();
			if (receiver instanceof ThisExpression) {
				result.addEntry(new RefactoringStatusEntry(
					severity,
					RefactoringCoreMessages.CallInliner_super_into_this_expression,
					JavaStatusContext.create(fCUnit, fInvocation)));
			}
		}
	}

	private void checkInvocationContext(RefactoringStatus result, int severity) {
		if (fInvocation.getNodeType() == ASTNode.FUNCTION_INVOCATION) {
			Expression exp= ((FunctionInvocation)fInvocation).getExpression();
			if (exp != null && exp.resolveTypeBinding() == null) {
				addEntry(result, RefactoringCoreMessages.CallInliner_receiver_type, 
					RefactoringStatusCodes.INLINE_METHOD_NULL_BINDING, severity);
				return;
			}
		}
		int nodeType= fTargetNode.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT) {
			if (fSourceProvider.isExecutionFlowInterrupted()) {
				addEntry(result, RefactoringCoreMessages.CallInliner_execution_flow,  
					RefactoringStatusCodes.INLINE_METHOD_EXECUTION_FLOW, severity);
				return;
			}
		} else if (nodeType == ASTNode.FUNCTION_INVOCATION) {
			ASTNode parent= fTargetNode.getParent();
			if (isReturnStatement(parent)) {
				//support inlining even if the execution flow is interrupted
				return;
			}
			if (fSourceProvider.isExecutionFlowInterrupted()) {
				addEntry(result, RefactoringCoreMessages.CallInliner_execution_flow,  
					RefactoringStatusCodes.INLINE_METHOD_EXECUTION_FLOW, severity);
				return;
			}
			if (isAssignment(parent) || isSingleDeclaration(parent)) {
				// we support inlining expression in assigment and initializers as
				// long as the execution flow isn't interrupted.
				return;
			} else {
				boolean isFieldDeclaration= ASTNodes.getParent(fInvocation, FieldDeclaration.class) != null;
				if (!fSourceProvider.isSimpleFunction()) {
					if (isMultiDeclarationFragment(parent)) {
						addEntry(result, RefactoringCoreMessages.CallInliner_multiDeclaration, 
							RefactoringStatusCodes.INLINE_METHOD_INITIALIZER_IN_FRAGEMENT, severity);
					} else if (isFieldDeclaration) {
						addEntry(result,
							RefactoringCoreMessages.CallInliner_field_initializer_simple, 
							RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
					} else {
						addEntry(result, RefactoringCoreMessages.CallInliner_simple_functions, 
							RefactoringStatusCodes.INLINE_METHOD_ONLY_SIMPLE_FUNCTIONS, severity);
					}
					return;
				}
				if (isFieldDeclaration) {
					int argumentsCount= fContext.arguments.length;
					for (int i= 0; i < argumentsCount; i++) {
						ParameterData parameter= fSourceProvider.getParameterData(i);
						if(parameter.isWrite()) {
							addEntry(result,
								RefactoringCoreMessages.CallInliner_field_initialize_write_parameter, 
								RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
							return;
						}
					}
					if(fLocals.size() > 0) {
						addEntry(result,
							RefactoringCoreMessages.CallInliner_field_initialize_new_local, 
							RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
						return;
					}
					// verify that the field is not referenced by the initializer method 
					VariableDeclarationFragment variable= (VariableDeclarationFragment)ASTNodes.getParent(fInvocation, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
					if(fSourceProvider.isVariableReferenced(variable.resolveBinding())) {
						addEntry(result,
							RefactoringCoreMessages.CallInliner_field_initialize_self_reference, 
							RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
						return;
					}
				}
			}
		}		
	}

	private static boolean isAssignment(ASTNode node) {
		return node instanceof Assignment;
	}
	
	private static boolean isReturnStatement(ASTNode node) {
		return node instanceof ReturnStatement;
	}
	
	private static boolean isSingleDeclaration(ASTNode node) {
		int type= node.getNodeType();
		if (type == ASTNode.SINGLE_VARIABLE_DECLARATION)
			return true;
		if (type == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= node.getParent();
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)node;
				return vs.fragments().size() == 1;
			}
		}
		return false;
	}
	
	private static boolean isMultiDeclarationFragment(ASTNode node) {
		int nodeType= node.getNodeType();
		if (nodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= node.getParent();
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)node;
				return vs.fragments().size() > 1;
			}
		}
		return false;
	}
	
	private void addEntry(RefactoringStatus result, String message, int code, int severity) {
		result.addEntry(new RefactoringStatusEntry(
			severity, message, 
			JavaStatusContext.create(fCUnit, fInvocation),
			Corext.getPluginId(),
			code, null));
	}

	private void flowAnalysis() {
		fInvocationScope= fRootScope.findScope(fTargetNode.getStartPosition(), fTargetNode.getLength());
		fInvocationScope.setCursor(fTargetNode.getStartPosition());
		fFlowContext= new FlowContext(0, fNumberOfLocals + 1);
		fFlowContext.setConsiderAccessMode(true);
		fFlowContext.setComputeMode(FlowContext.ARGUMENTS);
		Selection selection= Selection.createFromStartLength(fInvocation.getStartPosition(), fInvocation.getLength());
		switch (fBodyDeclaration.getNodeType()) {
			case ASTNode.INITIALIZER:
			case ASTNode.FIELD_DECLARATION:
			case ASTNode.FUNCTION_DECLARATION:
				fFlowInfo= new InputFlowAnalyzer(fFlowContext, selection, true).perform(fBodyDeclaration);
				break;
			default:
				Assert.isTrue(false, "Should not happen");			 //$NON-NLS-1$
		}
	}
	
	public RefactoringStatus perform(TextEditGroup textEditGroup) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		String[] blocks= fSourceProvider.getCodeBlocks(fContext);
		if(!fFieldInitializer) {
			initializeInsertionPoint(fSourceProvider.getNumberOfStatements() + fLocals.size());
		}
		
		addNewLocals(textEditGroup);
		replaceCall(result, blocks, textEditGroup);
		return result;
	}
	
	public TextEdit getModifications() {
		return fRewrite.rewriteAST(fBuffer.getDocument(), fCUnit.getJavaScriptProject().getOptions(true));
	}

	private void computeRealArguments() throws BadLocationException {
		List arguments= Invocations.getArguments(fInvocation);
		Set canNotInline= crossCheckArguments(arguments);
		boolean needsVarargBoxing= needsVarargBoxing(arguments);
		int varargIndex= fSourceProvider.getVarargIndex();
		String[] realArguments= new String[needsVarargBoxing ? varargIndex + 1 : arguments.size()];
		for (int i= 0; i < (needsVarargBoxing ? varargIndex : arguments.size()); i++) {
			Expression expression= (Expression)arguments.get(i);
			ParameterData parameter= fSourceProvider.getParameterData(i);
			if (canInline(expression, parameter) && !canNotInline.contains(expression)) {
				realArguments[i] = getContent(expression);
				// fixes bugs #35905, #38471
				if (argumentNeedsParenthesis(expression, parameter)) {
					realArguments[i] = "(" + realArguments[i] + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				String name= fInvocationScope.createName(parameter.getName(), true);
				realArguments[i]= name;
				fLocals.add(createLocalDeclaration(
					parameter.getTypeBinding(), name, 
					(Expression)fRewrite.createCopyTarget(expression)));
			}
		}
		if (needsVarargBoxing) {
			ParameterData parameter= fSourceProvider.getParameterData(varargIndex);
			String name= fInvocationScope.createName(parameter.getName(), true);
			realArguments[varargIndex]= name;
			AST ast= fInvocation.getAST();
			Type type= fImportRewrite.addImport(parameter.getTypeBinding(), ast);
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setName(ast.newSimpleName(name));
			ArrayInitializer initializer= ast.newArrayInitializer();
			for (int i= varargIndex; i < arguments.size(); i++) {
				initializer.expressions().add(fRewrite.createCopyTarget((ASTNode)arguments.get(i)));
			}
			fragment.setInitializer(initializer);
			VariableDeclarationStatement decl= ast.newVariableDeclarationStatement(fragment);
			decl.setType(type);
			fLocals.add(decl);
		}
		fContext.arguments= realArguments;
	}
	
	private boolean needsVarargBoxing(List arguments) {
		if (!fSourceProvider.isVarargs())
			return false;
		/*
		if (!fSourceProvider.hasArrayAccess())
			return false;
		*/
		int index= fSourceProvider.getVarargIndex();
		// we have varags but the call doesn't pass any arguments
		if (index >= arguments.size())
			return true;
		// parameter is array type
		// one arg
		if (index == arguments.size() - 1) {
			ITypeBinding argument= ((Expression)arguments.get(index)).resolveTypeBinding();
			if (argument == null)
				return false;
			ITypeBinding parameter= fSourceProvider.getParameterData(index).getTypeBinding();
			return !fTypeEnvironment.create(argument).canAssignTo(fTypeEnvironment.create(parameter));
		}
		return true;
	}
	
	private boolean argumentNeedsParenthesis(Expression expression, ParameterData param) {
		if (expression instanceof ArrayCreation)
			return true;
		int argPrecedence= OperatorPrecedence.getValue(expression);
		int paramPrecedence= param.getOperatorPrecedence();
		if (argPrecedence != -1 && paramPrecedence != -1)
			return argPrecedence < paramPrecedence;
		
		return false;
	}
	
	private void computeReceiver() throws BadLocationException {
		Expression receiver= Invocations.getExpression(fInvocation);
		if (receiver == null)
			return;
		final boolean isName= receiver instanceof Name;
		if (isName)
			fContext.receiverIsStatic= ((Name)receiver).resolveBinding() instanceof ITypeBinding;
		if (ASTNodes.isLiteral(receiver) || isName || receiver instanceof ThisExpression) {
			fContext.receiver= fBuffer.getDocument().get(receiver.getStartPosition(), receiver.getLength());
			return;
		}
		switch(fSourceProvider.getReceiversToBeUpdated()) {
			case 0:
				// Make sure we evaluate the current receiver. Best is to assign to
				// local.
				fLocals.add(createLocalDeclaration(
					receiver.resolveTypeBinding(), 
					fInvocationScope.createName("r", true),  //$NON-NLS-1$
					(Expression)fRewrite.createCopyTarget(receiver)));
				return;
			case 1:
				fContext.receiver= fBuffer.getDocument().get(receiver.getStartPosition(), receiver.getLength());
				return;
			default:
				String local= fInvocationScope.createName("r", true); //$NON-NLS-1$
					fLocals.add(createLocalDeclaration(
					receiver.resolveTypeBinding(), 
					local, 
					(Expression)fRewrite.createCopyTarget(receiver)));
				fContext.receiver= local;
				return;
		}
	}

	private void addNewLocals(TextEditGroup textEditGroup) {
		if (fLocals.isEmpty())
			return;
		for (Iterator iter= fLocals.iterator(); iter.hasNext();) {
			ASTNode element= (ASTNode)iter.next();
			fListRewrite.insertAt(element, fInsertionIndex++, textEditGroup);
		}
	}

	private void replaceCall(RefactoringStatus status, String[] blocks, TextEditGroup textEditGroup) {
		// Inline empty body
		if (blocks.length == 0) {
			if (fNeedsStatement) {
				fRewrite.replace(fTargetNode, fTargetNode.getAST().newEmptyStatement(), textEditGroup);
			} else {
				fRewrite.remove(fTargetNode, textEditGroup);
			}
		} else {
			ASTNode node= null;
			for (int i= 0; i < blocks.length - 1; i++) {
				node= fRewrite.createStringPlaceholder(blocks[i], ASTNode.RETURN_STATEMENT);
				fListRewrite.insertAt(node, fInsertionIndex++, textEditGroup);
			}
			String block= blocks[blocks.length - 1];
			// We can inline a call where the declaration is a function and the call itself
			// is a statement. In this case we have to create a temporary variable if the
			// returned expression must be evaluated.
			if (fContext.callMode == ASTNode.EXPRESSION_STATEMENT && fSourceProvider.hasReturnValue()) {
				if (fSourceProvider.mustEvaluateReturnedExpression()) {
					if (fSourceProvider.returnValueNeedsLocalVariable()) {
						IFunctionBinding invocation= Invocations.resolveBinding(fInvocation);
						node= createLocalDeclaration(
							invocation.getReturnType(), 
							fInvocationScope.createName(fSourceProvider.getMethodName(), true), 
							(Expression)fRewrite.createStringPlaceholder(block, ASTNode.FUNCTION_INVOCATION));
					} else {
						node= fTargetNode.getAST().newExpressionStatement(
							(Expression)fRewrite.createStringPlaceholder(block, ASTNode.FUNCTION_INVOCATION));
					}
				} else {
					node= null;
				}
			} else if (fTargetNode instanceof Expression) {
				node= fRewrite.createStringPlaceholder(block, ASTNode.FUNCTION_INVOCATION);
				
				if (needsParenthesis()) {
					ParenthesizedExpression pExp= fTargetNode.getAST().newParenthesizedExpression();
					pExp.setExpression((Expression)node);
					node= pExp;
				}
			} else {
				node= fRewrite.createStringPlaceholder(block, ASTNode.RETURN_STATEMENT);
			}
			
			// Now replace the target node with the source node
			if (node != null) {
				if (fTargetNode == null) {
					fListRewrite.insertAt(node, fInsertionIndex++, textEditGroup);
				} else {
					fRewrite.replace(fTargetNode, node, textEditGroup);
				}
			} else {
				if (fTargetNode != null) {
					fRewrite.remove(fTargetNode, textEditGroup);
				}
			}
		}
	}
	private boolean needsParenthesis() {
		if (!fSourceProvider.needsReturnedExpressionParenthesis())
			return false;
		ASTNode parent= fTargetNode.getParent();
		int type= parent.getNodeType();
		return 
			type == ASTNode.FUNCTION_INVOCATION || 
			(parent instanceof Expression && type != ASTNode.ASSIGNMENT) ||
			(fSourceProvider.returnsConditionalExpression() &&
				type == ASTNode.VARIABLE_DECLARATION_FRAGMENT &&  
				((VariableDeclarationFragment)parent).getInitializer() == fTargetNode);
	}
	
	private VariableDeclarationStatement createLocalDeclaration(ITypeBinding type, String name, Expression initializer) {
		String typeName= fImportRewrite.addImport(type);
		VariableDeclarationStatement decl= (VariableDeclarationStatement)ASTNodeFactory.newStatement(
			fInvocation.getAST(), typeName + " " + name + ";"); //$NON-NLS-1$ //$NON-NLS-2$
		((VariableDeclarationFragment)decl.fragments().get(0)).setInitializer(initializer);
		return decl;
	}

    /**
     * Checks whether arguments are passed to the method which do some assignments
     * inside the expression. If so these arguments can't be inlined into the
     * calling method since the assignments might be reorder. An example is:
     * <code>
     *   add((field=args).length,field.hashCode());
     * </code>
     * Field might not be initialized when the arguments are reorder in the called
     * method.
     */
	private Set crossCheckArguments(List arguments) {
		final Set assigned= new HashSet();
		final Set result= new HashSet();
		for (Iterator iter= arguments.iterator(); iter.hasNext();) {
			final Expression expression= (Expression) iter.next();
			expression.accept(new ASTVisitor() {
				public boolean visit(Assignment node) {
					Expression lhs= node.getLeftHandSide();
					if (lhs instanceof Name) {
						IBinding binding= ((Name)lhs).resolveBinding();
						if (binding instanceof IVariableBinding) {
							assigned.add(binding);
							result.add(expression);
						}
					}
					return true;
				}
			});
		}
		for (Iterator iter= arguments.iterator(); iter.hasNext();) {
			final Expression expression= (Expression) iter.next();
			if (!result.contains(expression)) {
				expression.accept(new HierarchicalASTVisitor() {
					public boolean visit(Name node) {
						IBinding binding= node.resolveBinding();
						if (binding != null && assigned.contains(binding))
							result.add(expression);
						return false;
					}
				});
			}
		}
		return result;
	}
	
	private boolean canInline(Expression actualParameter, ParameterData formalParameter) {
		InlineEvaluator evaluator= new InlineEvaluator(formalParameter);
		actualParameter.accept(evaluator);
		return evaluator.getResult();
	}
	
	private void initializeInsertionPoint(int nos) {
		fInsertionIndex= -1;
		fNeedsStatement= false;
		// if we have a constructor invocation the invocation itself is already a statement
		ASTNode parentStatement= fInvocation instanceof Statement 
			? fInvocation 
			: ASTNodes.getParent(fInvocation, Statement.class);
		ASTNode container= parentStatement.getParent();
		int type= container.getNodeType();
		if (type == ASTNode.BLOCK) { 
			Block block= (Block)container;
			fListRewrite= fRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			fInsertionIndex= fListRewrite.getRewrittenList().indexOf(parentStatement);
		} else if (type == ASTNode.SWITCH_STATEMENT) {
			SwitchStatement switchStatement= (SwitchStatement)container;
			fListRewrite= fRewrite.getListRewrite(switchStatement, SwitchStatement.STATEMENTS_PROPERTY);
			fInsertionIndex= fListRewrite.getRewrittenList().indexOf(parentStatement);
		} else if (isControlStatement(container) || type == ASTNode.LABELED_STATEMENT) {
			fNeedsStatement= true;
			if (nos > 1 || needsBlockAroundDanglingIf()) {
				Block block= fInvocation.getAST().newBlock();
				fInsertionIndex= 0;
				Statement currentStatement= null;
				switch(type) {
					case ASTNode.LABELED_STATEMENT:
						currentStatement= ((LabeledStatement)container).getBody();
						break;
					case ASTNode.FOR_STATEMENT:
						currentStatement= ((ForStatement)container).getBody();
						break;
					case ASTNode.FOR_IN_STATEMENT:
						currentStatement= ((ForInStatement)container).getBody(); 
						break;
					case ASTNode.ENHANCED_FOR_STATEMENT:
						currentStatement= ((EnhancedForStatement)container).getBody();
						break;
					case ASTNode.WHILE_STATEMENT:
						currentStatement= ((WhileStatement)container).getBody();
						break;
					case ASTNode.WITH_STATEMENT:
						currentStatement= ((WithStatement)container).getBody();
						break;
					case ASTNode.DO_STATEMENT:
						currentStatement= ((DoStatement)container).getBody();
						break;
					case ASTNode.IF_STATEMENT:
						IfStatement node= (IfStatement)container;
						Statement thenPart= node.getThenStatement();
						if (fTargetNode == thenPart || ASTNodes.isParent(fTargetNode, thenPart)) {
							currentStatement= thenPart;
						} else {
							currentStatement= node.getElseStatement();
						}
						break;
				}
				Assert.isNotNull(currentStatement);
				fRewrite.replace(currentStatement, block, null);
				fListRewrite= fRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				// The method to be inlined is not the body of the control statement.
				if (currentStatement != fTargetNode) {
					fListRewrite.insertLast(fRewrite.createCopyTarget(currentStatement), null);
				} else {
					// We can't replace a copy with something else. So we
					// have to insert all statements to be inlined.
					fTargetNode= null;
				}
			}
		}
		// We only insert one new statement or we delete the existing call. 
		// So there is no need to have an insertion index.
	}

	private boolean needsBlockAroundDanglingIf() {
		/* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=169331
		 * 
		 * Situation:
		 * boolean a, b;
		 * void toInline() {
		 *     if (a)
		 *         hashCode();
		 * }
		 * void m() {
		 *     if (b)
		 *         toInline();
		 *     else
		 *         toString();
		 * }
		 * => needs block around inlined "if (a)..." to avoid attaching else to wrong if.
		 */
		return fTargetNode.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY
				&& fTargetNode.getParent().getStructuralProperty(IfStatement.ELSE_STATEMENT_PROPERTY) != null
				&& fSourceProvider.isDangligIf();
	}

	private String getContent(ASTNode node) throws BadLocationException {
		return fBuffer.getDocument().get(node.getStartPosition(), node.getLength());
	}

	private boolean isControlStatement(ASTNode node) {
		int type= node.getNodeType();
		return type == ASTNode.IF_STATEMENT || type == ASTNode.FOR_STATEMENT || type == ASTNode.ENHANCED_FOR_STATEMENT ||
		        type == ASTNode.WHILE_STATEMENT || type == ASTNode.DO_STATEMENT;
	}
}
