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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.Message;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.CodeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/* package */ class ExtractMethodAnalyzer extends CodeAnalyzer {

	public static final int ERROR=					-2;
	public static final int UNDEFINED=				-1;
	public static final int NO=						0;
	public static final int EXPRESSION=				1;
	public static final int ACCESS_TO_LOCAL=		2;
	public static final int RETURN_STATEMENT_VOID=	3;
	public static final int RETURN_STATEMENT_VALUE=	4;
	public static final int MULTIPLE=				5;

	/** This is either a method declaration or an initializer */
	private BodyDeclaration fEnclosingBodyDeclaration;
	private IFunctionBinding fEnclosingMethodBinding;
	private int fMaxVariableId;

	private int fReturnKind;
	private Type fReturnType;
	
	private FlowInfo fInputFlowInfo;
	private FlowContext fInputFlowContext;
	
	private IVariableBinding[] fArguments;
	private IVariableBinding[] fMethodLocals;
	private ITypeBinding[] fTypeVariables;
	
	private IVariableBinding fReturnValue;
	private IVariableBinding[] fCallerLocals;
	private IVariableBinding fReturnLocal;
	
	private ITypeBinding[] fAllExceptions;
	private ITypeBinding fExpressionBinding;

	private boolean fForceStatic;
	private boolean fIsLastStatementSelected;
	
	public ExtractMethodAnalyzer(IJavaScriptUnit unit, Selection selection) throws JavaScriptModelException {
		super(unit, selection, false);
	}
	
	public BodyDeclaration getEnclosingBodyDeclaration() {
		return fEnclosingBodyDeclaration;
	}
	
	public int getReturnKind() {
		return fReturnKind;
	}
	
	public boolean extractsExpression() {
		return fReturnKind == EXPRESSION;
	}
	
	public Type getReturnType() {
		return fReturnType;
	}

	public boolean generateImport() {
		switch (fReturnKind) {
			case EXPRESSION:
				return true;
			default:
				return false;
		}
	}
	
	public IVariableBinding[] getArguments() {
		return fArguments;
	}
	
	public IVariableBinding[] getMethodLocals() {
		return fMethodLocals;
	}
	
	public IVariableBinding getReturnValue() {
		return fReturnValue;
	}
	
	public IVariableBinding[] getCallerLocals() {
		return fCallerLocals;
	}
	
	public IVariableBinding getReturnLocal() {
		return fReturnLocal;
	}
	
	public ITypeBinding getExpressionBinding() {
		return fExpressionBinding;
	}
	
	public boolean getForceStatic() {
		return fForceStatic;
	}
	
	public ITypeBinding[] getTypeVariables() {
		return fTypeVariables;
	}
	
	//---- Activation checking ---------------------------------------------------------------------------
	
	public RefactoringStatus checkInitialConditions(ImportRewrite rewriter) {
		RefactoringStatus result= getStatus();
		checkExpression(result);
		if (result.hasFatalError())
			return result;
			
		fReturnKind= UNDEFINED;
		fMaxVariableId= LocalVariableIndex.perform(fEnclosingBodyDeclaration);
		if (analyzeSelection(result).hasFatalError())
			return result;

		int returns= fReturnKind == NO ? 0 : 1;
		if (fReturnValue != null) {
			fReturnKind= ACCESS_TO_LOCAL;
			returns++;
		}
		if (isExpressionSelected()) {
			fReturnKind= EXPRESSION;
			returns++;
		}
			
		if (returns > 1) {
			result.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_ambiguous_return_value, JavaStatusContext.create(fCUnit, getSelection())); 
			fReturnKind= MULTIPLE;
			return result;
		}
		initReturnType(rewriter);
		return result;
	}
	
	private void checkExpression(RefactoringStatus status) {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes != null && nodes.length == 1) {
			ASTNode node= nodes[0];
			if (node instanceof Type) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference, JavaStatusContext.create(fCUnit, node)); 
			} else if (node.getLocationInParent() == SwitchCase.EXPRESSION_PROPERTY) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_switch_case, JavaStatusContext.create(fCUnit, node)); 
			}
		}
	}
	
	private void initReturnType(ImportRewrite rewriter) {
		AST ast= fEnclosingBodyDeclaration.getAST();
		fReturnType= null;
		switch (fReturnKind) {
			case ACCESS_TO_LOCAL:
				VariableDeclaration declaration= ASTNodes.findVariableDeclaration(fReturnValue, fEnclosingBodyDeclaration);
				fReturnType= ASTNodeFactory.newType(ast, declaration);
				break;
			case EXPRESSION:
				Expression expression= (Expression)getFirstSelectedNode();
				if (expression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
					fExpressionBinding= ((ClassInstanceCreation)expression).getType().resolveBinding();
				} else {
					fExpressionBinding= expression.resolveTypeBinding();
				}
				if (fExpressionBinding != null) {
					if (fExpressionBinding.isNullType()) {
						getStatus().addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_null_type, JavaStatusContext.create(fCUnit, expression)); 
					} else {
						ITypeBinding normalizedBinding= Bindings.normalizeForDeclarationUse(fExpressionBinding, ast);
						if (normalizedBinding != null) {
							fReturnType= rewriter.addImport(normalizedBinding, ast);
						}
					}
				} else {
					fReturnType= ast.newPrimitiveType(PrimitiveType.VOID);
					getStatus().addError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_determine_return_type, JavaStatusContext.create(fCUnit, expression)); 
				}
				break;	
			case RETURN_STATEMENT_VALUE:
				if (fEnclosingBodyDeclaration.getNodeType() == ASTNode.FUNCTION_DECLARATION)
					fReturnType= ((FunctionDeclaration)fEnclosingBodyDeclaration).getReturnType2();
				break;
			default:
				fReturnType= ast.newPrimitiveType(PrimitiveType.VOID);
		}
		if (fReturnType == null)
			fReturnType= ast.newPrimitiveType(PrimitiveType.VOID);
	}
	
	//	 !!! -- +/- same as in ExtractTempRefactoring
	public boolean isLiteralNodeSelected() {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes.length != 1)
			return false;
		ASTNode node= nodes[0];
		switch (node.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL :
			case ASTNode.CHARACTER_LITERAL :
			case ASTNode.NULL_LITERAL :
			case ASTNode.NUMBER_LITERAL :
			case ASTNode.UNDEFINED_LITERAL :
			case ASTNode.REGULAR_EXPRESSION_LITERAL :
				return true;
			
			default :
				return false;
		}
	}

	//---- Input checking -----------------------------------------------------------------------------------
		
	public void checkInput(RefactoringStatus status, String methodName, AST ast) {
		ITypeBinding[] arguments= getArgumentTypes();
		ITypeBinding type= ASTNodes.getEnclosingType(fEnclosingBodyDeclaration);
		status.merge(Checks.checkMethodInType(type, methodName, arguments));
		status.merge(Checks.checkMethodInHierarchy(type.getSuperclass(), methodName, null, arguments));
	}
	
	private ITypeBinding[] getArgumentTypes() {
		ITypeBinding[] result= new ITypeBinding[fArguments.length];
		for (int i= 0; i < fArguments.length; i++) {
			result[i]= fArguments[i].getType();
		}
		return result;
	}
	
	private RefactoringStatus analyzeSelection(RefactoringStatus status) {
		fInputFlowContext= new FlowContext(0, fMaxVariableId + 1);
		fInputFlowContext.setConsiderAccessMode(true);
		fInputFlowContext.setComputeMode(FlowContext.ARGUMENTS);
		
		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(fInputFlowContext);
		fInputFlowInfo= flowAnalyzer.perform(getSelectedNodes());
		
		if (fInputFlowInfo.branches()) {
			status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_branch_mismatch, JavaStatusContext.create(fCUnit, getSelection())); 
			fReturnKind= ERROR;
			return status;
		}
		if (fInputFlowInfo.isValueReturn()) {
			fReturnKind= RETURN_STATEMENT_VALUE;
		} else  if (fInputFlowInfo.isVoidReturn() || (fInputFlowInfo.isPartialReturn() && isVoidMethod() && isLastStatementSelected())) {
			fReturnKind= RETURN_STATEMENT_VOID;
		} else if (fInputFlowInfo.isNoReturn() || fInputFlowInfo.isThrow() || fInputFlowInfo.isUndefined()) {
			fReturnKind= NO;
		}
		
		if (fReturnKind == UNDEFINED) {
			status.addError(RefactoringCoreMessages.FlowAnalyzer_execution_flow, JavaStatusContext.create(fCUnit, getSelection())); 
			fReturnKind= NO;
		}
		computeInput();
		computeExceptions();
		computeOutput(status);
		if (status.hasFatalError())
			return status;
		
		adjustArgumentsAndMethodLocals();
		compressArrays();
		return status;
	}
	
	private boolean isVoidMethod() {
		// if we have an initializer
		if (fEnclosingMethodBinding == null)
			return true;
		ITypeBinding binding= fEnclosingMethodBinding.getReturnType();
		if (fEnclosingBodyDeclaration.getAST().resolveWellKnownType("void").equals(binding)) //$NON-NLS-1$
			return true;
		return false;
	}
	
	public boolean isLastStatementSelected() {
		return fIsLastStatementSelected;
	}

	private void computeLastStatementSelected() {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes.length == 0) {
			fIsLastStatementSelected= false;
		} else {
			Block body= null;
			if (fEnclosingBodyDeclaration instanceof FunctionDeclaration) {
				body= ((FunctionDeclaration) fEnclosingBodyDeclaration).getBody();
			} else if (fEnclosingBodyDeclaration instanceof Initializer) {
				body= ((Initializer) fEnclosingBodyDeclaration).getBody();
			}
			if (body != null) {
				List statements= body.statements();
				fIsLastStatementSelected= nodes[nodes.length - 1] == statements.get(statements.size() - 1);
			}
		}
	}

	private void computeInput() {
		int argumentMode= FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN;
		fArguments= removeSelectedDeclarations(fInputFlowInfo.get(fInputFlowContext, argumentMode));
		fMethodLocals= removeSelectedDeclarations(fInputFlowInfo.get(fInputFlowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL));
		fTypeVariables= computeTypeVariables(fInputFlowInfo.getTypeVariables());
	}
	
	private IVariableBinding[] removeSelectedDeclarations(IVariableBinding[] bindings) {
		List result= new ArrayList(bindings.length);
		Selection selection= getSelection();
		for (int i= 0; i < bindings.length; i++) {
			ASTNode decl= ((JavaScriptUnit)fEnclosingBodyDeclaration.getRoot()).findDeclaringNode(bindings[i]);
			if (!selection.covers(decl))
				result.add(bindings[i]);
		}
		return (IVariableBinding[])result.toArray(new IVariableBinding[result.size()]);
	}
	
	private ITypeBinding[] computeTypeVariables(ITypeBinding[] bindings) {
		Selection selection= getSelection();
		Set result= new HashSet();
		// first remove all type variables that come from outside of the method
		// or are covered by the selection
		JavaScriptUnit compilationUnit= (JavaScriptUnit)fEnclosingBodyDeclaration.getRoot();
		for (int i= 0; i < bindings.length; i++) {
			ASTNode decl= compilationUnit.findDeclaringNode(bindings[i]);
			if (decl == null || (!selection.covers(decl) && decl.getParent() instanceof FunctionDeclaration))
				result.add(bindings[i]);
		}
		// all all type variables which are needed since a local variable uses it
//		for (int i= 0; i < fArguments.length; i++) {
//			IVariableBinding arg= fArguments[i];
//		}
		return (ITypeBinding[])result.toArray(new ITypeBinding[result.size()]);
	}
	
	private void computeOutput(RefactoringStatus status) {
		// First find all writes inside the selection.
		FlowContext flowContext= new FlowContext(0, fMaxVariableId + 1);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.RETURN_VALUES);
		FlowInfo returnInfo= new InOutFlowAnalyzer(flowContext).perform(getSelectedNodes());
		IVariableBinding[] returnValues= returnInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);
		
		// Compute a selection that exactly covers the selected nodes
		IRegion region= getSelectedNodeRange();
		Selection selection= Selection.createFromStartLength(region.getOffset(), region.getLength());
		
		int counter= 0;
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InputFlowAnalyzer(flowContext, selection, true).perform(fEnclosingBodyDeclaration);
		IVariableBinding[] reads= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.UNKNOWN);
		outer: for (int i= 0; i < returnValues.length && counter <= 1; i++) {
			IVariableBinding binding= returnValues[i];
			for (int x= 0; x < reads.length; x++) {
				if (reads[x] == binding) {
					counter++;
					fReturnValue= binding;
					continue outer;
				}
			}
		}
		switch (counter) {
			case 0:
				fReturnValue= null;
				break;
			case 1:
				break;
			default:
				fReturnValue= null;
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_assignments_to_local, JavaStatusContext.create(fCUnit, getSelection())); 
				return;
		}
		List callerLocals= new ArrayList(5);
		FlowInfo localInfo= new InputFlowAnalyzer(flowContext, selection, false).perform(fEnclosingBodyDeclaration);
		IVariableBinding[] writes= localInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);
		for (int i= 0; i < writes.length; i++) {
			IVariableBinding write= writes[i];
			if (getSelection().covers(ASTNodes.findDeclaration(write, fEnclosingBodyDeclaration)))
				callerLocals.add(write);
		}
		fCallerLocals= (IVariableBinding[])callerLocals.toArray(new IVariableBinding[callerLocals.size()]);
		if (fReturnValue != null && getSelection().covers(ASTNodes.findDeclaration(fReturnValue, fEnclosingBodyDeclaration)))
			fReturnLocal= fReturnValue;
	}
	
	private void adjustArgumentsAndMethodLocals() {
		for (int i= 0; i < fArguments.length; i++) {
			IVariableBinding argument= fArguments[i];
			// Both arguments and locals consider FlowInfo.WRITE_POTENTIAL. But at the end a variable
			// can either be a local of an argument. Fix this based on the compute return type which
			// didn't exist when we computed the locals and arguments (see computeInput())
			if (fInputFlowInfo.hasAccessMode(fInputFlowContext, argument, FlowInfo.WRITE_POTENTIAL)) {
				if (argument != fReturnValue)
					fArguments[i]= null;
				// We didn't remove the argument. So we have to remove the local declaration
				if (fArguments[i] != null) {
					for (int l= 0; l < fMethodLocals.length; l++) {
						if (fMethodLocals[l] == argument)
							fMethodLocals[l]= null;						
					}
				}
			}
		}
	}
	
	private void compressArrays() {
		fArguments= compressArray(fArguments);
		fCallerLocals= compressArray(fCallerLocals);
		fMethodLocals= compressArray(fMethodLocals);
	}
	
	private IVariableBinding[] compressArray(IVariableBinding[] array) {
		if (array == null)
			return null;
		int size= 0;
		for (int i= 0; i < array.length; i++) {
			if (array[i] != null)
				size++;	
		}
		if (size == array.length)
			return array;
		IVariableBinding[] result= new IVariableBinding[size];
		for (int i= 0, r= 0; i < array.length; i++) {
			if (array[i] != null)
				result[r++]= array[i];		
		}
		return result;
	}
	
	//---- Change creation ----------------------------------------------------------------------------------
	
	public void aboutToCreateChange() {
	}

	//---- Exceptions -----------------------------------------------------------------------------------------
	
	public ITypeBinding[] getExceptions(boolean includeRuntimeExceptions, AST ast) {
		if (includeRuntimeExceptions)
			return fAllExceptions;
		List result= new ArrayList(fAllExceptions.length);
		for (int i= 0; i < fAllExceptions.length; i++) {
			ITypeBinding exception= fAllExceptions[i];
			if (!includeRuntimeExceptions && Bindings.isRuntimeException(exception))
				continue;
			result.add(exception);
		}
		return (ITypeBinding[]) result.toArray(new ITypeBinding[result.size()]);
	}
	
	private void computeExceptions() {
		fAllExceptions= ExceptionAnalyzer.perform(getSelectedNodes());
	}
	
	//---- Special visitor methods ---------------------------------------------------------------------------

	protected void handleNextSelectedNode(ASTNode node) {
		super.handleNextSelectedNode(node);
		checkParent(node);
	}
	
	protected boolean handleSelectionEndsIn(ASTNode node) {
		invalidSelection(RefactoringCoreMessages.StatementAnalyzer_doesNotCover, JavaStatusContext.create(fCUnit, node)); 
		return super.handleSelectionEndsIn(node);
	}
		
	private void checkParent(ASTNode node) {
		ASTNode firstParent= getFirstSelectedNode().getParent();
		do {
			node= node.getParent();
			if (node == firstParent)
				return;
		} while (node != null);
		invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_parent_mismatch); 
	}
	
	public void endVisit(JavaScriptUnit node) {
		RefactoringStatus status= getStatus();
		superCall: {
			if (status.hasFatalError())
				break superCall;
			if (!hasSelectedNodes()) {
				ASTNode coveringNode= getLastCoveringNode();
				if (coveringNode instanceof Block && coveringNode.getParent() instanceof FunctionDeclaration) {
					FunctionDeclaration methodDecl= (FunctionDeclaration)coveringNode.getParent();
					Message[] messages= ASTNodes.getMessages(methodDecl, ASTNodes.NODE_ONLY);
					if (messages.length > 0) {
						status.addFatalError(Messages.format(
							RefactoringCoreMessages.ExtractMethodAnalyzer_compile_errors, 
							methodDecl.getName().getIdentifier()), JavaStatusContext.create(fCUnit, methodDecl));
						break superCall;
					}
				}
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_only_method_body); 
				break superCall;
			}
			fEnclosingBodyDeclaration= (BodyDeclaration)ASTNodes.getParent(getFirstSelectedNode(), BodyDeclaration.class);
			if (fEnclosingBodyDeclaration == null || 
					(fEnclosingBodyDeclaration.getNodeType() != ASTNode.FUNCTION_DECLARATION && 
					 fEnclosingBodyDeclaration.getNodeType() != ASTNode.INITIALIZER)) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_only_method_body); 
				break superCall;
			} else if (ASTNodes.getEnclosingType(fEnclosingBodyDeclaration) == null) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_compile_errors_no_parent_binding);
				break superCall;
			} else if (fEnclosingBodyDeclaration.getNodeType() == ASTNode.FUNCTION_DECLARATION) {
				fEnclosingMethodBinding= ((FunctionDeclaration)fEnclosingBodyDeclaration).resolveBinding();
			}
			if (!isSingleExpressionOrStatementSet()) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_single_expression_or_set); 
				break superCall;
			}
			if (isExpressionSelected()) {
				ASTNode expression= getFirstSelectedNode();
				if (expression instanceof Name) {
					Name name= (Name)expression;
					if (name.resolveBinding() instanceof ITypeBinding) {
						status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference); 
						break superCall;
					}
					if (name.resolveBinding() instanceof IFunctionBinding) {
						status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_method_name_reference); 
					}
					if (name.isSimpleName() && ((SimpleName)name).isDeclaration()) {
						status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_name_in_declaration); 
						break superCall;
					}
				}
				fForceStatic= 
					ASTNodes.getParent(expression, ASTNode.SUPER_CONSTRUCTOR_INVOCATION) != null ||
					ASTNodes.getParent(expression, ASTNode.CONSTRUCTOR_INVOCATION) != null;
			}
			status.merge(LocalTypeAnalyzer.perform(fEnclosingBodyDeclaration, getSelection()));
			computeLastStatementSelected();
		}
		super.endVisit(node);
	}
	
	public boolean visit(AnonymousClassDeclaration node) {
		boolean result= super.visit(node);
		if (isFirstSelectedNode(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_anonymous_type, JavaStatusContext.create(fCUnit, node)); 
			return false;
		}
		return result;
	}
	
	public boolean visit(Assignment node) {
		boolean result= super.visit(node);
		if (getSelection().getVisitSelectionMode(node.getLeftHandSide()) == Selection.SELECTED) {
			invalidSelection(
				RefactoringCoreMessages.ExtractMethodAnalyzer_leftHandSideOfAssignment,  
				JavaStatusContext.create(fCUnit, node));
			return false;
		}
		return result;
	}

	public boolean visit(DoStatement node) {
		boolean result= super.visit(node);
		
		try {
			int actionStart= getTokenScanner().getTokenEndOffset(ITerminalSymbols.TokenNamedo, node.getStartPosition());
			if (getSelection().getOffset() == actionStart) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_after_do_keyword, JavaStatusContext.create(fCUnit, getSelection())); 
				return false;
			}
		} catch (CoreException e) {
			// ignore
		}
		
		return result;
	}

	public boolean visit(FunctionDeclaration node) {
		Block body= node.getBody();
		if (body == null)
			return false;
		Selection selection= getSelection();
		int nodeStart= body.getStartPosition();
		int nodeExclusiveEnd= nodeStart + body.getLength();
		// if selection node inside of the method body ignore method
		if (!(nodeStart < selection.getOffset() && selection.getExclusiveEnd() < nodeExclusiveEnd))
			return false;
		return super.visit(node);
	}
	
	public boolean visit(ConstructorInvocation node) {
		return visitConstructorInvocation(node, super.visit(node));
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		return visitConstructorInvocation(node, super.visit(node));
	}
	
	private boolean visitConstructorInvocation(ASTNode node, boolean superResult) {
		if (getSelection().getVisitSelectionMode(node) == Selection.SELECTED) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_super_or_this, JavaStatusContext.create(fCUnit, node)); 
			return false;
		}
		return superResult;
	}
	
	public void endVisit(ForInStatement node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (node.getIterationVariable().equals(getFirstSelectedNode())) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer, JavaStatusContext.create(fCUnit, getSelection())); 
//			} else if (node.updaters().contains(getLastSelectedNode())) {
//				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_updater, JavaStatusContext.create(fCUnit, getSelection())); 
			}
		}
		super.endVisit(node);
	}		
	 
	
	public boolean visit(VariableDeclarationFragment node) {
		boolean result= super.visit(node);
		if (isFirstSelectedNode(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment, JavaStatusContext.create(fCUnit, node)); 
			return false;
		}
		return result;
	}
	
	public void endVisit(ForStatement node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (node.initializers().contains(getFirstSelectedNode())) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer, JavaStatusContext.create(fCUnit, getSelection())); 
			} else if (node.updaters().contains(getLastSelectedNode())) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_updater, JavaStatusContext.create(fCUnit, getSelection())); 
			}
		}
		super.endVisit(node);
	}		
	
	public void endVisit(VariableDeclarationExpression node) {
		checkTypeInDeclaration(node.getType());
		super.endVisit(node);		
	}
			
	public void endVisit(VariableDeclarationStatement node) {
		checkTypeInDeclaration(node.getType());
		super.endVisit(node);		
	}
			
	private boolean isFirstSelectedNode(ASTNode node) {
		return getSelection().getVisitSelectionMode(node) == Selection.SELECTED && getFirstSelectedNode() == node;
	}
	
	private void checkTypeInDeclaration(Type node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED && getFirstSelectedNode() == node) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration, JavaStatusContext.create(fCUnit, getSelection())); 
		}
	}
	
	private boolean isSingleExpressionOrStatementSet() {
		ASTNode first= getFirstSelectedNode();
		if (first == null)
			return true;
		if (first instanceof Expression && getSelectedNodes().length != 1)
			return false;
		return true;
	}
}

