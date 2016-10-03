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

import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression.Operator;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;


public class ConvertForLoopOperation extends ConvertLoopOperation {
	
	private static final String LENGTH_QUERY= "length"; //$NON-NLS-1$
	private static final String LITERAL_0= "0"; //$NON-NLS-1$
	private static final String LITERAL_1= "1"; //$NON-NLS-1$
	private static final class InvalidBodyError extends Error {
		private static final long serialVersionUID= 1L;
	}
	
	private IVariableBinding fIndexBinding;
	private IVariableBinding fLengthBinding;
	private IBinding fArrayBinding;
	private Expression fArrayAccess;
	private VariableDeclarationFragment fElementDeclaration;
	private final boolean fMakeFinal;
	
	public ConvertForLoopOperation(ForStatement forStatement) {
		this(forStatement, new String[0], false);
	}
	
	public ConvertForLoopOperation(ForStatement forStatement, String[] usedNames, boolean makeFinal) {
		super(forStatement, usedNames);
		fMakeFinal= makeFinal;
	}
	
	public IStatus satisfiesPreconditions() {
		ForStatement statement= getForStatement();
		JavaScriptUnit ast= (JavaScriptUnit)statement.getRoot();
		
		IJavaScriptElement javaElement= ast.getJavaElement();
		if (javaElement == null)
			return ERROR_STATUS;
		
		if (!JavaModelUtil.is50OrHigher(javaElement.getJavaScriptProject()))
			return ERROR_STATUS;
		
		if (!validateInitializers(statement))
			return ERROR_STATUS;
		
		if (!validateExpression(statement))
			return ERROR_STATUS;
		
		if (!validateUpdaters(statement))
			return ERROR_STATUS;
		
		if (!validateBody(statement))
			return ERROR_STATUS;
		
		return Status.OK_STATUS;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>int [result]= 0;</li>
	 * <li>int [result]= 0, [lengthBinding]= [arrayBinding].length;</li>
	 * <li>int , [result]= 0;</li>
	 * </ul>
	 */
	private boolean validateInitializers(ForStatement statement) {
		List initializers= statement.initializers();
		if (initializers.size() != 1)
			return false;
		
		Expression expression= (Expression)initializers.get(0);
		if (!(expression instanceof VariableDeclarationExpression))
			return false;
		
		VariableDeclarationExpression declaration= (VariableDeclarationExpression)expression;
		ITypeBinding declarationBinding= declaration.resolveTypeBinding();
		if (declarationBinding == null)
			return false;
		
		if (!declarationBinding.isPrimitive())
			return false;
		
		if (!PrimitiveType.INT.toString().equals(declarationBinding.getQualifiedName()))
			return false;
		
		List fragments= declaration.fragments();
		if (fragments.size() == 1) {
			IVariableBinding indexBinding= getIndexBindingFromFragment((VariableDeclarationFragment)fragments.get(0));
			if (indexBinding == null)
				return false;
			
			fIndexBinding= indexBinding;
			return true;
		} else if (fragments.size() == 2) {
			IVariableBinding indexBinding= getIndexBindingFromFragment((VariableDeclarationFragment)fragments.get(0));
			if (indexBinding == null) {
				indexBinding= getIndexBindingFromFragment((VariableDeclarationFragment)fragments.get(1));
				if (indexBinding == null)
					return false;
				
				if (!validateLengthFragment((VariableDeclarationFragment)fragments.get(0)))
					return false;
			} else {
				if (!validateLengthFragment((VariableDeclarationFragment)fragments.get(1)))
					return false;
			}
			
			fIndexBinding= indexBinding;
			return true;
		}
		return false;
	}
	
	/**
	 * [lengthBinding]= [arrayBinding].length
	 */
	private boolean validateLengthFragment(VariableDeclarationFragment fragment) {
		Expression initializer= fragment.getInitializer();
		if (initializer == null)
			return false;
		
		if (!validateLengthQuery(initializer))
			return false;
		
		IVariableBinding lengthBinding= (IVariableBinding)fragment.getName().resolveBinding();
		if (lengthBinding == null)
			return false;
		fLengthBinding= lengthBinding;
		
		return true;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[result]= 0</li>
	 * </ul>
	 */
	private IVariableBinding getIndexBindingFromFragment(VariableDeclarationFragment fragment) {
		Expression initializer= fragment.getInitializer();
		if (!(initializer instanceof NumberLiteral))
			return null;
		
		NumberLiteral number= (NumberLiteral)initializer;
		if (!LITERAL_0.equals(number.getToken()))
			return null;
		
		return (IVariableBinding)fragment.getName().resolveBinding();
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[indexBinding] < [result].length;</li>
	 * <li>[result].length > [indexBinding];</li>
	 * <li>[indexBinding] < [lengthBinding];</li>
	 * <li>[lengthBinding] > [indexBinding];</li>
	 * </ul>
	 */
	private boolean validateExpression(ForStatement statement) {
		Expression expression= statement.getExpression();
		if (!(expression instanceof InfixExpression))
			return false;
		
		InfixExpression infix= (InfixExpression)expression;
		
		Expression left= infix.getLeftOperand();
		Expression right= infix.getRightOperand();
		if (left instanceof SimpleName && right instanceof SimpleName) {
			IVariableBinding lengthBinding= fLengthBinding;
			if (lengthBinding == null)
				return false;
			
			IBinding leftBinding= ((SimpleName)left).resolveBinding();
			IBinding righBinding= ((SimpleName)right).resolveBinding();
			
			if (fIndexBinding.equals(leftBinding)) {
				return lengthBinding.equals(righBinding);
			} else if (fIndexBinding.equals(righBinding)) {
				return lengthBinding.equals(leftBinding);
			}
			
			return false;
		} else if (left instanceof SimpleName) {
			if (!fIndexBinding.equals(((SimpleName)left).resolveBinding()))
				return false;
			
			if (!Operator.LESS.equals(infix.getOperator()))
				return false;
			
			return validateLengthQuery(right);
		} else if (right instanceof SimpleName) {
			if (!fIndexBinding.equals(((SimpleName)right).resolveBinding()))
				return false;
			
			if (!Operator.GREATER.equals(infix.getOperator()))
				return false;
			
			return validateLengthQuery(left);
		}
		
		return false;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[result].length</li>
	 * </ul>
	 */
	private boolean validateLengthQuery(Expression lengthQuery) {
		if (lengthQuery instanceof QualifiedName) {
			QualifiedName qualifiedName= (QualifiedName)lengthQuery;
			SimpleName name= qualifiedName.getName();
			if (!LENGTH_QUERY.equals(name.getIdentifier()))
				return false;
			
			Name arrayAccess= qualifiedName.getQualifier();
			ITypeBinding accessType= arrayAccess.resolveTypeBinding();
			if (accessType == null)
				return false;
			
			if (!accessType.isArray())
				return false;
			
			IBinding arrayBinding= arrayAccess.resolveBinding();
			if (arrayBinding == null)
				return false;
			
			fArrayBinding= arrayBinding;
			fArrayAccess= arrayAccess;
			return true;
		} else if (lengthQuery instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess)lengthQuery;
			SimpleName name= fieldAccess.getName();
			if (!LENGTH_QUERY.equals(name.getIdentifier()))
				return false;
			
			Expression arrayAccess= fieldAccess.getExpression();
			ITypeBinding accessType= arrayAccess.resolveTypeBinding();
			if (accessType == null)
				return false;
			
			if (!accessType.isArray())
				return false;
			
			IBinding arrayBinding= getBinding(arrayAccess);
			if (arrayBinding == null)
				return false;
			
			fArrayBinding= arrayBinding;
			fArrayAccess= arrayAccess;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[indexBinding]++</li>
	 * <li>[indexBinding]+= 1</li>
	 * <li>[indexBinding]= [indexBinding] + 1</li>
	 * <li>[indexBinding]= 1 + [indexBinding]</li>
	 * <ul>
	 */
	private boolean validateUpdaters(ForStatement statement) {
		List updaters= statement.updaters();
		if (updaters.size() != 1)
			return false;
		
		Expression updater= (Expression)updaters.get(0);
		if (updater instanceof PostfixExpression) {
			PostfixExpression postfix= (PostfixExpression)updater;
			
			if (!PostfixExpression.Operator.INCREMENT.equals(postfix.getOperator()))
				return false;
			
			IBinding binding= getBinding(postfix.getOperand());
			if (!fIndexBinding.equals(binding))
				return false;
			
			return true;
		} else if (updater instanceof Assignment) {
			Assignment assignment= (Assignment)updater;
			Expression left= assignment.getLeftHandSide();
			IBinding binding= getBinding(left);
			if (!fIndexBinding.equals(binding))
				return false;
			
			if (Assignment.Operator.PLUS_ASSIGN.equals(assignment.getOperator())) {
				return isOneLiteral(assignment.getRightHandSide());
			} else if (Assignment.Operator.ASSIGN.equals(assignment.getOperator())) {
				Expression right= assignment.getRightHandSide();
				if (!(right instanceof InfixExpression))
					return false;
				
				InfixExpression infixExpression= (InfixExpression)right;
				Expression leftOperand= infixExpression.getLeftOperand();
				IBinding leftBinding= getBinding(leftOperand);
				Expression rightOperand= infixExpression.getRightOperand();
				IBinding rightBinding= getBinding(rightOperand);
				
				if (fIndexBinding.equals(leftBinding)) {
					return isOneLiteral(rightOperand);
				} else if (fIndexBinding.equals(rightBinding)) {
					return isOneLiteral(leftOperand);
				}
			}
		}
		return false;
	}
	
	private boolean isOneLiteral(Expression expression) {
		if (!(expression instanceof NumberLiteral))
			return false;
		
		NumberLiteral literal= (NumberLiteral)expression;
		return LITERAL_1.equals(literal.getToken());
	}
	
	/**
	 * returns false iff
	 * <ul>
	 * <li><code>indexBinding</code> is used for anything else then accessing
	 * an element of <code>arrayBinding</code></li>
	 * <li><code>arrayBinding</code> is assigned</li>
	 * <li>an element of <code>arrayBinding</code> is assigned</li>
	 * <li><code>lengthBinding</code> is referenced</li>
	 * </ul>
	 * within <code>body</code>
	 */
	private boolean validateBody(ForStatement statement) {
		Statement body= statement.getBody();
		try {
			body.accept(new GenericVisitor() {
				/**
				 * {@inheritDoc}
				 */
				protected boolean visitNode(ASTNode node) {
					if (node instanceof Name) {
						Name name= (Name)node;
						IBinding nameBinding= name.resolveBinding();
						if (nameBinding == null)
							throw new InvalidBodyError();
						
						if (nameBinding.equals(fIndexBinding)) {
							if (node.getLocationInParent() != ArrayAccess.INDEX_PROPERTY)
								throw new InvalidBodyError();
							
							ArrayAccess arrayAccess= (ArrayAccess)node.getParent();
							Expression array= arrayAccess.getArray();
							
							IBinding binding= getBinding(array);
							if (binding == null)
								throw new InvalidBodyError();
							
							if (!fArrayBinding.equals(binding))
								throw new InvalidBodyError();
							
						} else if (nameBinding.equals(fArrayBinding)) {
							ASTNode current= node;
							while (current != null && !(current instanceof Statement)) {
								if (current.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY)
									throw new InvalidBodyError();
								
								if (current instanceof PrefixExpression)
									throw new InvalidBodyError();
								
								if (current instanceof PostfixExpression)
									throw new InvalidBodyError();
								
								current= current.getParent();
							}
						} else if (nameBinding.equals(fLengthBinding)) {
							throw new InvalidBodyError();
						}
					}
					
					return true;
				}
				
				public boolean visit(ArrayAccess node) {
					if (fElementDeclaration != null)
						return super.visit(node);
					
					IBinding binding= getBinding(node.getArray());
					if (fArrayBinding.equals(binding)) {
						IBinding index= getBinding(node.getIndex());
						if (fIndexBinding.equals(index)) {
							if (node.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
								fElementDeclaration= (VariableDeclarationFragment)node.getParent();
							}
						}
					}
					return super.visit(node);
				}
				
			});
		} catch (InvalidBodyError e) {
			return false;
		}
		
		return true;
	}
	
	private static IBinding getBinding(Expression expression) {
		if (expression instanceof FieldAccess) {
			return ((FieldAccess)expression).resolveFieldBinding();
		} else if (expression instanceof Name) {
			return ((Name)expression).resolveBinding();
		}
		
		return null;
	}
	
	public String getIntroducedVariableName() {
		if (fElementDeclaration != null) {
			return fElementDeclaration.getName().getIdentifier();
		} else {
			ForStatement forStatement= getForStatement();
			IJavaScriptProject javaProject= ((JavaScriptUnit)forStatement.getRoot()).getJavaElement().getJavaScriptProject();
			String[] proposals= getVariableNameProposals(fArrayAccess.resolveTypeBinding(), javaProject);
			return proposals[0];
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.fix.LinkedFix.ILinkedFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List, java.util.List)
	 */
	public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups, LinkedProposalModel positionGroups) throws CoreException {
		TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description);
		textEditGroups.add(group);
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		
		TightSourceRangeComputer rangeComputer;
		if (rewrite.getExtendedSourceRangeComputer() instanceof TightSourceRangeComputer) {
			rangeComputer= (TightSourceRangeComputer)rewrite.getExtendedSourceRangeComputer();
		} else {
			rangeComputer= new TightSourceRangeComputer();			
		}
		rangeComputer.addTightSourceNode(getForStatement());
		rewrite.setTargetSourceRangeComputer(rangeComputer);
		
		Statement statement= convert(cuRewrite, group, positionGroups);
		rewrite.replace(getForStatement(), statement, group);
	}
	
	protected Statement convert(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModel positionGroups) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		
		ForStatement forStatement= getForStatement();
		
		IJavaScriptProject javaProject= ((JavaScriptUnit)forStatement.getRoot()).getJavaElement().getJavaScriptProject();
		String[] proposals= getVariableNameProposals(fArrayAccess.resolveTypeBinding(), javaProject);
		
		String parameterName;
		if (fElementDeclaration != null) {
			parameterName= fElementDeclaration.getName().getIdentifier();
		} else {
			parameterName= proposals[0];
		}
		
		LinkedProposalPositionGroup pg= positionGroups.getPositionGroup(parameterName, true);
		if (fElementDeclaration != null)
			pg.addProposal(parameterName, null, 10);
		for (int i= 0; i < proposals.length; i++) {
			pg.addProposal(proposals[i], null, 10);
		}
		
		AST ast= forStatement.getAST();
		EnhancedForStatement result= ast.newEnhancedForStatement();
		
		SingleVariableDeclaration parameterDeclaration= createParameterDeclaration(parameterName, fElementDeclaration, fArrayAccess, forStatement, importRewrite, rewrite, group, pg, fMakeFinal);
		result.setParameter(parameterDeclaration);
		
		result.setExpression((Expression)rewrite.createCopyTarget(fArrayAccess));
		
		convertBody(forStatement.getBody(), fIndexBinding, fArrayBinding, parameterName, rewrite, group, pg);
		result.setBody(getBody(cuRewrite, group, positionGroups));
		
		positionGroups.setEndPosition(rewrite.track(result));
		
		return result;
	}
	
	private void convertBody(Statement body, final IBinding indexBinding, final IBinding arrayBinding, final String parameterName, final ASTRewrite rewrite, final TextEditGroup editGroup, final LinkedProposalPositionGroup pg) {
		final AST ast= body.getAST();
		
		final HashSet assignedBindings= new HashSet();
		
		body.accept(new GenericVisitor() {
			public boolean visit(ArrayAccess node) {
				IBinding binding= getBinding(node.getArray());
				if (arrayBinding.equals(binding)) {
					IBinding index= getBinding(node.getIndex());
					if (indexBinding.equals(index)) {
						replaceAccess(node);
					}
				}
				
				return super.visit(node);
			}
			
			public boolean visit(SimpleName node) {
				if (assignedBindings.contains(node.resolveBinding())) {
					replaceAccess(node);
				}
				return super.visit(node);
			}
			
			private void replaceAccess(ASTNode node) {
				if (node.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)node.getParent();
					IBinding targetBinding= fragment.getName().resolveBinding();
					if (targetBinding != null) {
						assignedBindings.add(targetBinding);
						
						VariableDeclarationStatement statement= (VariableDeclarationStatement)fragment.getParent();
						
						if (statement.fragments().size() == 1) {
							rewrite.remove(statement, editGroup);
						} else {
							ListRewrite listRewrite= rewrite.getListRewrite(statement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
							listRewrite.remove(fragment, editGroup);
						}
						
					} else {
						SimpleName name= ast.newSimpleName(parameterName);
						rewrite.replace(node, name, editGroup);
						pg.addPosition(rewrite.track(name), true);
					}
				} else {
					SimpleName name= ast.newSimpleName(parameterName);
					rewrite.replace(node, name, editGroup);
					pg.addPosition(rewrite.track(name), true);
				}
			}
		});
	}
	
	private SingleVariableDeclaration createParameterDeclaration(String parameterName, VariableDeclarationFragment fragement, Expression arrayAccess, ForStatement statement, ImportRewrite importRewrite, ASTRewrite rewrite, TextEditGroup group, LinkedProposalPositionGroup pg, boolean makeFinal) {
		JavaScriptUnit compilationUnit= (JavaScriptUnit)arrayAccess.getRoot();
		AST ast= compilationUnit.getAST();
		
		SingleVariableDeclaration result= ast.newSingleVariableDeclaration();
		
		SimpleName name= ast.newSimpleName(parameterName);
		pg.addPosition(rewrite.track(name), true);
		result.setName(name);
		
		ITypeBinding arrayTypeBinding= arrayAccess.resolveTypeBinding();
		Type type= importType(arrayTypeBinding.getElementType(), statement, importRewrite, compilationUnit);
		if (arrayTypeBinding.getDimensions() != 1) {
			type= ast.newArrayType(type, arrayTypeBinding.getDimensions() - 1);
		}
		result.setType(type);
		
		if (fragement != null) {
			VariableDeclarationStatement declaration= (VariableDeclarationStatement)fragement.getParent();
			ModifierRewrite.create(rewrite, result).copyAllModifiers(declaration, group);
		}
		if (makeFinal) {
			ModifierRewrite.create(rewrite, result).setModifiers(Modifier.FINAL, 0, group);
		}
		
		return result;
	}
	
	private String[] getVariableNameProposals(ITypeBinding arrayTypeBinding, IJavaScriptProject project) {
		String[] variableNames= getUsedVariableNames();
		String[] elementSuggestions= StubUtility.getLocalNameSuggestions(project, FOR_LOOP_ELEMENT_IDENTIFIER, 0, variableNames);
		
		String type= arrayTypeBinding.getElementType().getName();
		String[] typeSuggestions= StubUtility.getLocalNameSuggestions(project, type, arrayTypeBinding.getDimensions() - 1, variableNames);
		
		String[] result= new String[elementSuggestions.length + typeSuggestions.length];
		System.arraycopy(elementSuggestions, 0, result, 0, elementSuggestions.length);
		System.arraycopy(typeSuggestions, 0, result, elementSuggestions.length, typeSuggestions.length);
		return result;
	}
	
}
