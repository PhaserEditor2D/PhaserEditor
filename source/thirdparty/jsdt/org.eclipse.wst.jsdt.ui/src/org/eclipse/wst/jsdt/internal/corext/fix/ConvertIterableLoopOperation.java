/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;

/**
 * Operation to convert for loops over iterables to enhanced for loops.
 * 
 * 
 */
public final class ConvertIterableLoopOperation extends ConvertLoopOperation {
	
	/**
	 * Returns the supertype of the given type with the qualified name.
	 * 
	 * @param binding
	 *            the binding of the type
	 * @param name
	 *            the qualified name of the supertype
	 * @return the supertype, or <code>null</code>
	 */
	private static ITypeBinding getSuperType(final ITypeBinding binding, final String name) {
		
		if (binding.isArray() || binding.isPrimitive())
			return null;
		
		if (binding.getQualifiedName().startsWith(name))
			return binding;
		
		final ITypeBinding type= binding.getSuperclass();
		if (type != null) {
			final ITypeBinding result= getSuperType(type, name);
			if (result != null)
				return result;
		}
		return null;
	}
	
	/** Has the element variable been assigned outside the for statement? */
	private boolean fAssigned= false;
	
	/** The binding of the element variable */
	private IBinding fElement= null;
	
	/** The node of the iterable object used in the expression */
	private Expression fExpression= null;
	
	/** The binding of the iterable object */
	private IBinding fIterable= null;
	
	/** Is the iterator method invoked on <code>this</code>? */
	private boolean fThis= false;
	
	/** The binding of the iterator variable */
	private IVariableBinding fIterator= null;
	
	/** The nodes of the element variable occurrences */
	private final List fOccurrences= new ArrayList(2);
	
	private EnhancedForStatement fEnhancedForLoop;

	private final boolean fMakeFinal;
	
	public ConvertIterableLoopOperation(ForStatement statement) {
		this(statement, new String[0], false);
	}
	
	public ConvertIterableLoopOperation(ForStatement statement, String[] usedNames, boolean makeFinal) {
		super(statement, usedNames);
		fMakeFinal= makeFinal;
	}
	
	public String getIntroducedVariableName() {
		if (fElement != null) {
			return fElement.getName();
		} else {
			return getVariableNameProposals()[0];
		}
	}
	
	private String[] getVariableNameProposals() {
		
		String[] variableNames= getUsedVariableNames();
		String[] elementSuggestions= StubUtility.getLocalNameSuggestions(getJavaProject(), FOR_LOOP_ELEMENT_IDENTIFIER, 0, variableNames);
		
		return elementSuggestions;
	}
	
	private IJavaScriptProject getJavaProject() {
		return getRoot().getJavaElement().getJavaScriptProject();
	}
	
	private JavaScriptUnit getRoot() {
		return (JavaScriptUnit)getForStatement().getRoot();
	}
	
	/**
	 * Returns the expression for the enhanced for statement.
	 * 
	 * @param rewrite
	 *            the AST rewrite to use
	 * @return the expression node, or <code>null</code>
	 */
	private Expression getExpression(final ASTRewrite rewrite) {
		if (fThis)
			return rewrite.getAST().newThisExpression();
		if (fExpression instanceof FunctionInvocation)
			return (FunctionInvocation)rewrite.createMoveTarget(fExpression);
		return (Expression)ASTNode.copySubtree(rewrite.getAST(), fExpression);
	}
	
	/**
	 * Returns the iterable type from the iterator type binding.
	 * 
	 * @param iterator
	 *            the iterator type binding, or <code>null</code>
	 * @return the iterable type
	 */
	private ITypeBinding getIterableType(final ITypeBinding iterator) {
		return getRoot().getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.fix.LinkedFix.ILinkedFixRewriteOperation#rewriteAST(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List, java.util.List)
	 */
	public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups, final LinkedProposalModel positionGroups) throws CoreException {
		final TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description);
		textEditGroups.add(group);
		
		final ASTRewrite astRewrite= cuRewrite.getASTRewrite();
		
		TightSourceRangeComputer rangeComputer;
		if (astRewrite.getExtendedSourceRangeComputer() instanceof TightSourceRangeComputer) {
			rangeComputer= (TightSourceRangeComputer)astRewrite.getExtendedSourceRangeComputer();
		} else {
			rangeComputer= new TightSourceRangeComputer();			
		}
		rangeComputer.addTightSourceNode(getForStatement());
		astRewrite.setTargetSourceRangeComputer(rangeComputer);
		
		Statement statement= convert(cuRewrite, group, positionGroups);
		astRewrite.replace(getForStatement(), statement, group);
	}
	
	protected Statement convert(CompilationUnitRewrite cuRewrite, final TextEditGroup group, final LinkedProposalModel positionGroups) throws CoreException {
		final AST ast= cuRewrite.getAST();
		final ASTRewrite astRewrite= cuRewrite.getASTRewrite();
		final ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		final ImportRemover remover= cuRewrite.getImportRemover();
		
		fEnhancedForLoop= ast.newEnhancedForStatement();
		String[] names= getVariableNameProposals();
		
		String name;
		if (fElement != null) {
			name= fElement.getName();
		} else {
			name= names[0];
		}
		final LinkedProposalPositionGroup pg= positionGroups.getPositionGroup(name, true);
		if (fElement != null)
			pg.addProposal(name, null, 10);
		for (int i= 0; i < names.length; i++) {
			pg.addProposal(names[i], null, 10);
		}
		
		final Statement body= getForStatement().getBody();
		if (body != null) {
			final ListRewrite list;
			if (body instanceof Block) {
				list= astRewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				for (final Iterator iterator= fOccurrences.iterator(); iterator.hasNext();) {
					final Statement parent= (Statement)ASTNodes.getParent((ASTNode)iterator.next(), Statement.class);
					if (parent != null && list.getRewrittenList().contains(parent)) {
						list.remove(parent, null);
						remover.registerRemovedNode(parent);
					}
				}
			} else {
				list= null;
			}
			final String text= name;
			body.accept(new ASTVisitor() {
				
				private boolean replace(final Expression expression) {
					final SimpleName node= ast.newSimpleName(text);
					astRewrite.replace(expression, node, group);
					remover.registerRemovedNode(expression);
					pg.addPosition(astRewrite.track(node), false);
					return false;
				}
				
				public final boolean visit(final FunctionInvocation node) {
					final IFunctionBinding binding= node.resolveMethodBinding();
					if (binding != null && (binding.getName().equals("next") || binding.getName().equals("nextElement"))) { //$NON-NLS-1$ //$NON-NLS-2$
						final Expression expression= node.getExpression();
						if (expression instanceof Name) {
							final IBinding result= ((Name)expression).resolveBinding();
							if (result != null && result.equals(fIterator))
								return replace(node);
						} else if (expression instanceof FieldAccess) {
							final IBinding result= ((FieldAccess)expression).resolveFieldBinding();
							if (result != null && result.equals(fIterator))
								return replace(node);
						}
					}
					return super.visit(node);
				}
				
				public final boolean visit(final SimpleName node) {
					if (fElement != null) {
						final IBinding binding= node.resolveBinding();
						if (binding != null && binding.equals(fElement)) {
							final Statement parent= (Statement)ASTNodes.getParent(node, Statement.class);
							if (parent != null && (list == null || list.getRewrittenList().contains(parent)))
								pg.addPosition(astRewrite.track(node), false);
						}
					}
					return false;
				}
			});
			
			fEnhancedForLoop.setBody(getBody(cuRewrite, group, positionGroups));
		}
		final SingleVariableDeclaration declaration= ast.newSingleVariableDeclaration();
		final SimpleName simple= ast.newSimpleName(name);
		pg.addPosition(astRewrite.track(simple), true);
		declaration.setName(simple);
		final ITypeBinding iterable= getIterableType(fIterator.getType());
		declaration.setType(importType(iterable, getForStatement(), importRewrite, getRoot()));
		if (fMakeFinal) {
			ModifierRewrite.create(astRewrite, declaration).setModifiers(Modifier.FINAL, 0, group);
		}
		remover.registerAddedImport(iterable.getQualifiedName());
		fEnhancedForLoop.setParameter(declaration);
		fEnhancedForLoop.setExpression(getExpression(astRewrite));
		
		remover.registerRemovedNode(getForStatement().getExpression());
		for (Iterator iterator= getForStatement().initializers().iterator(); iterator.hasNext();) {
			ASTNode node= (ASTNode)iterator.next();
			remover.registerRemovedNode(node);			
		}
		for (Iterator iterator= getForStatement().updaters().iterator(); iterator.hasNext();) {
			ASTNode node= (ASTNode)iterator.next();
			remover.registerRemovedNode(node);						
		}
		
		return fEnhancedForLoop;
	}
	
	/**
	 * Is this proposal applicable?
	 * 
	 * @return A status with severity <code>IStatus.Error</code> if not
	 *         applicable
	 */
	public final IStatus satisfiesPreconditions() {
		IStatus resultStatus= StatusInfo.OK_STATUS;
		if (JavaModelUtil.is50OrHigher(getJavaProject())) {
			resultStatus= checkExpressionCondition();
			if (resultStatus.getSeverity() == IStatus.ERROR)
				return resultStatus;
			
			List updateExpressions= (List)getForStatement().getStructuralProperty(ForStatement.UPDATERS_PROPERTY);
			if (updateExpressions.size() == 1) {
				resultStatus= new StatusInfo(IStatus.WARNING, Messages.format(FixMessages.ConvertIterableLoopOperation_RemoveUpdateExpression_Warning, ((Expression)updateExpressions.get(0)).toString()));
			} else if (updateExpressions.size() > 1) {
				resultStatus= new StatusInfo(IStatus.WARNING, FixMessages.ConvertIterableLoopOperation_RemoveUpdateExpressions_Warning);
			}
			
			for (final Iterator outer= getForStatement().initializers().iterator(); outer.hasNext();) {
				final Expression initializer= (Expression)outer.next();
				if (initializer instanceof VariableDeclarationExpression) {
					final VariableDeclarationExpression declaration= (VariableDeclarationExpression)initializer;
					List fragments= declaration.fragments();
					if (fragments.size() != 1) {
						return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
					} else {
						final VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(0);
						fragment.accept(new ASTVisitor() {
							
							public final boolean visit(final FunctionInvocation node) {
								final IFunctionBinding binding= node.resolveMethodBinding();
								if (binding != null) {
									final ITypeBinding type= binding.getReturnType();
									if (type != null) {
										final String qualified= type.getQualifiedName();
										if (qualified.startsWith("java.util.Enumeration<") || qualified.startsWith("java.util.Iterator<")) { //$NON-NLS-1$ //$NON-NLS-2$
											final Expression qualifier= node.getExpression();
											if (qualifier != null) {
												final ITypeBinding resolved= qualifier.resolveTypeBinding();
												if (resolved != null) {
													final ITypeBinding iterable= getSuperType(resolved, "java.lang.Iterable"); //$NON-NLS-1$
													if (iterable != null) {
														fExpression= qualifier;
														if (qualifier instanceof Name) {
															final Name name= (Name)qualifier;
															fIterable= name.resolveBinding();
														} else if (qualifier instanceof FunctionInvocation) {
															final FunctionInvocation invocation= (FunctionInvocation)qualifier;
															fIterable= invocation.resolveMethodBinding();
														} else if (qualifier instanceof FieldAccess) {
															final FieldAccess access= (FieldAccess)qualifier;
															fIterable= access.resolveFieldBinding();
														} else if (qualifier instanceof ThisExpression)
															fIterable= resolved;
													}
												}
											} else {
												final ITypeBinding declaring= binding.getDeclaringClass();
												if (declaring != null) {
													final ITypeBinding superBinding= getSuperType(declaring, "java.lang.Iterable"); //$NON-NLS-1$
													if (superBinding != null) {
														fIterable= superBinding;
														fThis= true;
													}
												}
											}
										}
									}
								}
								return true;
							}
							
							public final boolean visit(final VariableDeclarationFragment node) {
								final IVariableBinding binding= node.resolveBinding();
								if (binding != null) {
									final ITypeBinding type= binding.getType();
									if (type != null) {
										ITypeBinding iterator= getSuperType(type, "java.util.Iterator"); //$NON-NLS-1$
										if (iterator != null)
											fIterator= binding;
										else {
											iterator= getSuperType(type, "java.util.Enumeration"); //$NON-NLS-1$
											if (iterator != null)
												fIterator= binding;
										}
									}
								}
								return true;
							}
						});
					}
				}
			}
			final Statement statement= getForStatement().getBody();
			final boolean[] otherInvocationThenNext= new boolean[] {false};
			final int[] nextInvocationCount= new int[] {0};
			if (statement != null && fIterator != null) {
				final ITypeBinding iterable= getIterableType(fIterator.getType());
				statement.accept(new ASTVisitor() {
					
					public final boolean visit(final Assignment node) {
						return visit(node.getLeftHandSide(), node.getRightHandSide());
					}
					
					private boolean visit(final Expression node) {
						if (node != null) {
							final ITypeBinding binding= node.resolveTypeBinding();
							if (binding != null && iterable.equals(binding)) {
								if (node instanceof Name) {
									final Name name= (Name)node;
									final IBinding result= name.resolveBinding();
									if (result != null) {
										fOccurrences.add(node);
										fElement= result;
										return false;
									}
								} else if (node instanceof FieldAccess) {
									final FieldAccess access= (FieldAccess)node;
									final IBinding result= access.resolveFieldBinding();
									if (result != null) {
										fOccurrences.add(node);
										fElement= result;
										return false;
									}
								}
							}
						}
						return true;
					}
					
					private boolean visit(final Expression left, final Expression right) {
						if (right instanceof FunctionInvocation) {
//							final FunctionInvocation invocation= (FunctionInvocation)right;
//							final IFunctionBinding binding= invocation.resolveMethodBinding();
//							if (binding != null && (binding.getName().equals("next") || binding.getName().equals("nextElement"))) { //$NON-NLS-1$ //$NON-NLS-2$
//								final Expression expression= invocation.getExpression();
//								if (expression instanceof Name) {
//									final Name qualifier= (Name)expression;
//									final IBinding result= qualifier.resolveBinding();
//									if (result != null && result.equals(fIterator)) {
//										nextInvocationCount[0]++;
//										return visit(left);
//									}
//								} else if (expression instanceof FieldAccess) {
//									final FieldAccess qualifier= (FieldAccess)expression;
//									final IBinding result= qualifier.resolveFieldBinding();
//									if (result != null && result.equals(fIterator)) {
//										nextInvocationCount[0]++;
//										return visit(left);
//									}
//								}
//							} else {
//								return visit(invocation);
//							}
						} else if (right instanceof NullLiteral || right instanceof UndefinedLiteral)
							return visit(left);
						return true;
					}
					
					/**
					 * {@inheritDoc}
					 */
					public boolean visit(FunctionInvocation invocation) {
						final IFunctionBinding binding= invocation.resolveMethodBinding();
						if (binding != null) {
							final Expression expression= invocation.getExpression();
							if (expression instanceof Name) {
								final Name qualifier= (Name)expression;
								final IBinding result= qualifier.resolveBinding();
								if (result != null && result.equals(fIterator)) {
//									if (!binding.getName().equals("next") && !binding.getName().equals("nextElement")) { //$NON-NLS-1$ //$NON-NLS-2$
//										otherInvocationThenNext[0]= true;
//									} else {
//										nextInvocationCount[0]++;
//									}
								}
							} else if (expression instanceof FieldAccess) {
								final FieldAccess qualifier= (FieldAccess)expression;
								final IBinding result= qualifier.resolveFieldBinding();
								if (result != null && result.equals(fIterator)) {
//									if (!binding.getName().equals("next") && !binding.getName().equals("nextElement")) { //$NON-NLS-1$ //$NON-NLS-2$
//										otherInvocationThenNext[0]= true;
//									} else {
//										nextInvocationCount[0]++;
//									}
								}
							}
						}
						return false;
					}
					
					public final boolean visit(final VariableDeclarationFragment node) {
						return visit(node.getName(), node.getInitializer());
					}
				});
				if (otherInvocationThenNext[0])
					return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
					
				if (nextInvocationCount[0] > 1)
					return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			}
			final ASTNode root= getForStatement().getRoot();
			if (root != null) {
				root.accept(new ASTVisitor() {
					
					public final boolean visit(final ForStatement node) {
						return false;
					}
					
					public final boolean visit(final SimpleName node) {
						final IBinding binding= node.resolveBinding();
						if (binding != null && binding.equals(fElement))
							fAssigned= true;
						return false;
					}
				});
			}
		}
		if ((fExpression != null || fThis) && fIterable != null && fIterator != null && !fAssigned) {
			return resultStatus;
		} else {
			return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
		}
	}
	
	private IStatus checkExpressionCondition() {
		String warningLable= FixMessages.ConvertIterableLoopOperation_semanticChangeWarning;
		
		Expression expression= getForStatement().getExpression();
		if (!(expression instanceof FunctionInvocation))
			return new StatusInfo(IStatus.WARNING, warningLable);
		
		FunctionInvocation invoc= (FunctionInvocation)expression;
		IFunctionBinding methodBinding= invoc.resolveMethodBinding();
		if (methodBinding == null)
			return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			
		ITypeBinding declaringClass= methodBinding.getDeclaringClass();
		if (declaringClass == null)
			return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			
//		String qualifiedName= declaringClass.getQualifiedName();
//		String methodName= invoc.getName().getIdentifier();
//		if (qualifiedName.startsWith("java.util.Enumeration")) { //$NON-NLS-1$
//			if (!methodName.equals("hasMoreElements")) //$NON-NLS-1$
//				return new StatusInfo(IStatus.WARNING, warningLable);
//		} else if (qualifiedName.startsWith("java.util.Iterator")) { //$NON-NLS-1$
//			if (!methodName.equals("hasNext")) //$NON-NLS-1$
//				return new StatusInfo(IStatus.WARNING, warningLable);
//		} else {
//			return new StatusInfo(IStatus.WARNING, warningLable);
//		}
		
		return StatusInfo.OK_STATUS;
	}
	
}
