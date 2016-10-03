/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     jens.lukowski@gmx.de - contributed code to convert prefix and postfix 
 *       expressions into a combination of setter and getter calls.
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;

/**
 * Analyzer to find all references to the field and to determine how to convert 
 * them into setter or getter calls.
 */
class AccessAnalyzer extends ASTVisitor {

	private IJavaScriptUnit fCUnit;
	private IVariableBinding fFieldBinding;
	private ITypeBinding fDeclaringClassBinding;	
	private String fGetter;
	private String fSetter;
	private ASTRewrite fRewriter;
	private ImportRewrite fImportRewriter;
	private List fGroupDescriptions;
	private RefactoringStatus fStatus;
	private boolean fSetterMustReturnValue;
	private boolean fEncapsulateDeclaringClass;
	private boolean fIsFieldFinal;
	
	private boolean fRemoveStaticImport;
	private boolean fReferencingGetter;
	private boolean fReferencingSetter;

	private static final String READ_ACCESS= RefactoringCoreMessages.SelfEncapsulateField_AccessAnalyzer_encapsulate_read_access; 
	private static final String WRITE_ACCESS= RefactoringCoreMessages.SelfEncapsulateField_AccessAnalyzer_encapsulate_write_access; 
	private static final String PREFIX_ACCESS= RefactoringCoreMessages.SelfEncapsulateField_AccessAnalyzer_encapsulate_prefix_access; 
	private static final String POSTFIX_ACCESS= RefactoringCoreMessages.SelfEncapsulateField_AccessAnalyzer_encapsulate_postfix_access; 
		
	public AccessAnalyzer(SelfEncapsulateFieldRefactoring refactoring, IJavaScriptUnit unit, IVariableBinding field, ITypeBinding declaringClass, ASTRewrite rewriter, ImportRewrite importRewrite) {
		Assert.isNotNull(refactoring);
		Assert.isNotNull(unit);
		Assert.isNotNull(field);
		Assert.isNotNull(declaringClass);
		Assert.isNotNull(rewriter);
		Assert.isNotNull(importRewrite);
		fCUnit= unit;
		fFieldBinding= field.getVariableDeclaration();
		fDeclaringClassBinding= declaringClass;
		fRewriter= rewriter;
		fImportRewriter= importRewrite;
		fGroupDescriptions= new ArrayList();
		fGetter= refactoring.getGetterName();
		fSetter= refactoring.getSetterName();
		fEncapsulateDeclaringClass= refactoring.getEncapsulateDeclaringClass();
		fIsFieldFinal= false;
		
		fStatus= new RefactoringStatus();
	}

	public boolean getSetterMustReturnValue() {
		return fSetterMustReturnValue;
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	public List getGroupDescriptions() {
		return fGroupDescriptions;
	}
	
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		if (!considerBinding(resolveBinding(lhs), lhs))
			return true;
			
		checkParent(node);
		if (!fIsFieldFinal) {
			// Write access.
			AST ast= node.getAST();
			FunctionInvocation invocation= ast.newFunctionInvocation();
			invocation.setName(ast.newSimpleName(fSetter));
			fReferencingSetter= true;
			Expression receiver= getReceiver(lhs);
			if (receiver != null)
				invocation.setExpression((Expression)fRewriter.createCopyTarget(receiver));
			List arguments= invocation.arguments();
			if (node.getOperator() == Assignment.Operator.ASSIGN) {
				arguments.add(fRewriter.createCopyTarget(node.getRightHandSide()));
			} else {
				// This is the compound assignment case: field+= 10;
				boolean needsParentheses= ASTNodes.needsParentheses(node.getRightHandSide());
				InfixExpression exp= ast.newInfixExpression();
				exp.setOperator(ASTNodes.convertToInfixOperator(node.getOperator()));
				FunctionInvocation getter= ast.newFunctionInvocation();
				getter.setName(ast.newSimpleName(fGetter));
				fReferencingGetter= true;
				if (receiver != null)
					getter.setExpression((Expression)fRewriter.createCopyTarget(receiver));
				exp.setLeftOperand(getter);
				Expression rhs= (Expression)fRewriter.createCopyTarget(node.getRightHandSide());
				if (needsParentheses) {
					ParenthesizedExpression p= ast.newParenthesizedExpression();
					p.setExpression(rhs);
					rhs= p;
				}
				exp.setRightOperand(rhs);
				arguments.add(exp);
			}
			fRewriter.replace(node, invocation, createGroupDescription(WRITE_ACCESS));
		}
		node.getRightHandSide().accept(this);
		return false;
	}

	public boolean visit(SimpleName node) {
		if (!node.isDeclaration() && considerBinding(node.resolveBinding(), node)) {
			fReferencingGetter= true;
			fRewriter.replace(
				node, 
				fRewriter.createStringPlaceholder(fGetter + "()", ASTNode.FUNCTION_INVOCATION), //$NON-NLS-1$
				createGroupDescription(READ_ACCESS));
		}
		return true;
	}
	
	public boolean visit(ImportDeclaration node) {
		if (considerBinding(node.resolveBinding(), node)) {
			fRemoveStaticImport= true;
		}
		return false;
	}
	
	public boolean visit(PrefixExpression node) {
		Expression operand= node.getOperand();
		if (!considerBinding(resolveBinding(operand), operand))
			return true;
		
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator != PrefixExpression.Operator.INCREMENT && operator != PrefixExpression.Operator.DECREMENT)
			return true;
			
		checkParent(node);
		
		fRewriter.replace(node, 
			createInvocation(node.getAST(), node.getOperand(), node.getOperator().toString()), 
			createGroupDescription(PREFIX_ACCESS));
		return false;
	}
	
	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		if (!considerBinding(resolveBinding(operand), operand))
			return true;

		ASTNode parent= node.getParent();
		if (!(parent instanceof ExpressionStatement)) {
			fStatus.addError(RefactoringCoreMessages.SelfEncapsulateField_AccessAnalyzer_cannot_convert_postfix_expression,  
				JavaStatusContext.create(fCUnit, new SourceRange(node)));
			return false;
		}
		fRewriter.replace(node, 
			createInvocation(node.getAST(), node.getOperand(), node.getOperator().toString()), 
			createGroupDescription(POSTFIX_ACCESS));
		return false;
	}
	
	public boolean visit(FunctionDeclaration node) {
		String name= node.getName().getIdentifier();
		if (name.equals(fGetter) || name.equals(fSetter))
			return false;
		return true;
	}
	
	public void endVisit(JavaScriptUnit node) {
		// If we don't had a static import to the field we don't
		// have to add any, even if we generated a setter or 
		// getter access.
		if (!fRemoveStaticImport)
			return;
		
		ITypeBinding type= fFieldBinding.getDeclaringClass();
		String fieldName= fFieldBinding.getName();
		String typeName= type.getQualifiedName();
		if (fRemoveStaticImport) {
			fImportRewriter.removeStaticImport(typeName + "." + fieldName); //$NON-NLS-1$
		}
		if (fReferencingGetter) {
			fImportRewriter.addStaticImport(typeName, fGetter, false);
		}
		if (fReferencingSetter) {
			fImportRewriter.addStaticImport(typeName, fSetter, false);
		}
	}
	
	private boolean considerBinding(IBinding binding, ASTNode node) {
		if (!(binding instanceof IVariableBinding))
			return false;
		boolean result= Bindings.equals(fFieldBinding, ((IVariableBinding)binding).getVariableDeclaration());
		if (!result || fEncapsulateDeclaringClass)
			return result;
			
		if (binding instanceof IVariableBinding) {
			AbstractTypeDeclaration type= (AbstractTypeDeclaration)ASTNodes.getParent(node, AbstractTypeDeclaration.class);
			if (type != null) {
				ITypeBinding declaringType= type.resolveBinding();
				return !Bindings.equals(fDeclaringClassBinding, declaringType);
			}
		}
		return true;
	}
	
	private void checkParent(ASTNode node) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof ExpressionStatement))
			fSetterMustReturnValue= true;
	}
		
	private IBinding resolveBinding(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression).resolveBinding();
		else if (expression instanceof QualifiedName)
			return ((QualifiedName)expression).resolveBinding();
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName().resolveBinding();
		return null;
	}
	
	private Expression getReceiver(Expression expression) {
		int type= expression.getNodeType();
		switch(type) {
			case ASTNode.SIMPLE_NAME:
				return null;
			case ASTNode.QUALIFIED_NAME:
				return ((QualifiedName)expression).getQualifier();
			case ASTNode.FIELD_ACCESS:
				return ((FieldAccess)expression).getExpression();
		}
		return null;
	}
		
	private FunctionInvocation createInvocation(AST ast, Expression operand, String operator) {
		Expression receiver= getReceiver(operand);
		FunctionInvocation invocation= ast.newFunctionInvocation();
		invocation.setName(ast.newSimpleName(fSetter));
		if (receiver != null)
			invocation.setExpression((Expression)fRewriter.createCopyTarget(receiver));
		InfixExpression argument= ast.newInfixExpression();
		invocation.arguments().add(argument);
		if ("++".equals(operator)) { //$NON-NLS-1$
			argument.setOperator(InfixExpression.Operator.PLUS);
		} else if ("--".equals(operator)) { //$NON-NLS-1$
			argument.setOperator(InfixExpression.Operator.MINUS);
		} else {
			Assert.isTrue(false, "Should not happen"); //$NON-NLS-1$
		}
		FunctionInvocation getter= ast.newFunctionInvocation();
		getter.setName(ast.newSimpleName(fGetter));
		if (receiver != null)
			getter.setExpression((Expression)fRewriter.createCopyTarget(receiver));
		argument.setLeftOperand(getter);
		argument.setRightOperand(ast.newNumberLiteral("1")); //$NON-NLS-1$

		fReferencingGetter= true;
		fReferencingSetter= true;
		
		return invocation;
	}
	
	private TextEditGroup createGroupDescription(String name) {
		TextEditGroup result= new TextEditGroup(name);
		fGroupDescriptions.add(result);
		return result;
	}
}

