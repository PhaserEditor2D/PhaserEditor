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
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.VariableDeclarationRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.surround.SurroundWithAnalyzer;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;

public abstract class SurroundWith {

	private static final class SplitSelectedOperator implements ISplitOperation {
		
		private List fAccessedInside;
		private List fStatements;
		private List fAccessedAfter;
		private ASTRewrite fRewrite;
		private ListRewrite fBlockRewrite;
		private VariableDeclarationStatement fLastStatement= null;

		public SplitSelectedOperator(List inside, List after, ListRewrite blockRewrite, ASTRewrite rewrite, List statements) {
			super();
			fAccessedInside= inside;
			fStatements= statements;
			fAccessedAfter= after;
			fRewrite= rewrite;
			fBlockRewrite= blockRewrite;
		}

		public boolean needsSplit(VariableDeclarationFragment last, VariableDeclarationFragment current) {
			return fAccessedInside.contains(last) != fAccessedInside.contains(current) || fAccessedAfter.contains(last) != fAccessedAfter.contains(current);
		}

		public void initializeStatement(VariableDeclarationStatement statement, VariableDeclarationFragment current) {
			if (fAccessedAfter.contains(current)) {
				if (fAccessedInside.contains(current))
					makeFinal(statement, fRewrite);
				handleInitializer(current);

				if (fLastStatement != null) {
					fBlockRewrite.insertAfter(statement, fLastStatement, null);
				}
				fLastStatement= statement;
			} else {
				if (fLastStatement != null) {
					handleNewStatement(statement);
				} else {
					handleStatement(statement);
					fLastStatement= statement;
				}
			}
		}

		protected void handleStatement(Statement statement) {
			fStatements.add(fRewrite.createMoveTarget(statement));
		}

		protected void handleNewStatement(Statement statement) {
			fStatements.add(statement);
		}

		protected void handleInitializer(VariableDeclarationFragment fragment) {
			splitOffInitializer(fStatements, fragment, fRewrite);
		}

	}

	private static final class SplitUnselectedOperator implements ISplitOperation {

		private List fAccessedInside;
		private ListRewrite fBlockRewrite;
		private ASTRewrite fRewrite;
		private VariableDeclarationStatement fLastStatement;

		private SplitUnselectedOperator(List accessedInside, ListRewrite blockRewrite, ASTRewrite rewrite) {
			super();
			fAccessedInside= accessedInside;
			fBlockRewrite= blockRewrite;
			fRewrite= rewrite;
			fLastStatement= null;
		}

		public boolean needsSplit(VariableDeclarationFragment last, VariableDeclarationFragment current) {
			return fAccessedInside.contains(last) != fAccessedInside.contains(current);
		}

		public void initializeStatement(VariableDeclarationStatement statement, VariableDeclarationFragment current) {
			if (fAccessedInside.contains(current))
				makeFinal(statement, fRewrite);

			if (fLastStatement != null)
				fBlockRewrite.insertAfter(statement, fLastStatement, null);
			fLastStatement= statement;
		}
	}

	protected interface ISplitOperation {
		boolean needsSplit(VariableDeclarationFragment last, VariableDeclarationFragment current);
		void initializeStatement(VariableDeclarationStatement statement, VariableDeclarationFragment current);
	}

	private final JavaScriptUnit fRootNode;
	private final Statement[] fSelectedStatements;
	private boolean fIsNewContext;
	private ITrackedNodePosition fFirstInsertedPosition;
	private ITrackedNodePosition fLastInsertedPosition;
	
	public SurroundWith(JavaScriptUnit root, Statement[] selectedStatements) {
		fRootNode= root;
		fSelectedStatements= selectedStatements;
	}
	

	public static boolean isApplicable(IInvocationContext context) throws JavaScriptModelException {
		IJavaScriptUnit unit= context.getCompilationUnit();
		JavaScriptUnit ast= ASTProvider.getASTProvider().getAST(unit, ASTProvider.WAIT_NO, null);
		if (ast == null)
			return true;
		
		Selection selection= Selection.createFromStartLength(context.getSelectionOffset(), context.getSelectionLength());
		SurroundWithAnalyzer analyzer= new SurroundWithAnalyzer(unit, selection);
		context.getASTRoot().accept(analyzer);
	
		return analyzer.getStatus().isOK() && analyzer.hasSelectedNodes();
    }

	/**
	 * Selected nodes in <code>context</code> under <code>selection</code> or null if no valid selection.
	 * @param context The context in which the proposal is applyed.
	 * @return Selected nodes or null if no valid selection.
	 * @throws JavaScriptModelException
	 */
	public static Statement[] getSelectedStatements(IInvocationContext context) throws JavaScriptModelException {
		Selection selection= Selection.createFromStartLength(context.getSelectionOffset(), context.getSelectionLength());
		SurroundWithAnalyzer analyzer= new SurroundWithAnalyzer(context.getCompilationUnit(), selection);
		context.getASTRoot().accept(analyzer);
	
		if (!analyzer.getStatus().isOK() || !analyzer.hasSelectedNodes()) {
			return null;
		} else {
			return analyzer.getSelectedStatements();
		}
	}
	
	public int getBodyStart() {
		return fFirstInsertedPosition.getStartPosition();
	}

	public int getBodyLength() {
		return fLastInsertedPosition.getStartPosition() + fLastInsertedPosition.getLength() - getBodyStart();
	}

	/**
	 * Returns the rewriter to be used.
	 * @return Returns the rewriter to be used.
	 * @throws CoreException A core exception is thrown when the could not be created.
	 */
	public ASTRewrite getRewrite() throws CoreException {
		Statement[] selectedStatements= fSelectedStatements;
		AST ast= getAst();
		
		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		BodyDeclaration enclosingBodyDeclaration= (BodyDeclaration)ASTNodes.getParent(selectedStatements[0], BodyDeclaration.class);
		int maxVariableId= LocalVariableIndex.perform(enclosingBodyDeclaration) + 1;
		
		fIsNewContext= isNewContext();
		
		List accessedAfter= getVariableDeclarationsAccessedAfter(selectedStatements[selectedStatements.length - 1], maxVariableId);
		List readInside;
		readInside= getVariableDeclarationReadsInside(selectedStatements, maxVariableId);
		
		List inserted= new ArrayList();
		moveToBlock(selectedStatements, inserted, accessedAfter, readInside, rewrite);
		if (fIsNewContext) {
			ImportRewrite importRewrite= StubUtility.createImportRewrite((JavaScriptUnit)selectedStatements[0].getRoot(), false);
			for (int i= 0; i < selectedStatements.length; i++) {
				qualifyThisExpressions(selectedStatements[i], rewrite, importRewrite);
			}
		}
		
		if (selectedStatements.length == 1 && ASTNodes.isControlStatementBody(selectedStatements[0].getLocationInParent())) {
			Block wrap= ast.newBlock();
			rewrite.replace(selectedStatements[0], wrap, null);
			ListRewrite listRewrite= rewrite.getListRewrite(wrap, Block.STATEMENTS_PROPERTY);
			
			for (Iterator iterator= inserted.iterator(); iterator.hasNext();) {
				ASTNode node= (ASTNode)iterator.next();
				listRewrite.insertLast(node, null);
			}
			
		} else {
			ListRewrite listRewrite= getListRewrite(selectedStatements[0], rewrite);
			
			ASTNode current= selectedStatements[selectedStatements.length - 1];
			for (Iterator iterator= inserted.iterator(); iterator.hasNext();) {
				ASTNode node= (ASTNode)iterator.next();
				listRewrite.insertAfter(node, current, null);
				current= node;
			}
		}
		
		fFirstInsertedPosition= rewrite.track((ASTNode)inserted.get(0));
		fLastInsertedPosition= rewrite.track((ASTNode)inserted.get(inserted.size() - 1));
		
		return rewrite;
	}
	
	
	/**
	 * @return true if the code will be moved to a new context?
	 */
	protected abstract boolean isNewContext();

	/**
	 * List of VariableDeclaration of variables which are read in <code>selectedNodes</code>.
	 * 
	 * @param maxVariableId Maximum number of variable declarations block
	 * @param selectedNodes The selectedNodes
	 * @return	List of VariableDeclaration
	 */
	protected List getVariableDeclarationReadsInside(Statement[] selectedNodes, int maxVariableId) {
		ArrayList result= new ArrayList();
		if (!fIsNewContext)
			return result;
		
		IVariableBinding[] reads= getReads(selectedNodes, maxVariableId);
		for (int i= 0; i < reads.length; i++) {
			IVariableBinding read= reads[i];
			if (!read.isField()) {
				ASTNode readDecl= getRootNode().findDeclaringNode(read);
				if (readDecl instanceof VariableDeclaration) {
					result.add(readDecl);
				}
			}
		}
		
		return result;
	}

	/**
	 * List of VariableDeclarationFragments which are accessed after <code>startNode</code>.
	 * 
	 * @param startNode The node after to inspect
	 * @param maxVariableId The maximum number of variable declarations
	 * @return List of VariableDeclarationFragments which can't be moved to the new block
	 */
	protected List getVariableDeclarationsAccessedAfter(ASTNode startNode, int maxVariableId) {
	
		List statements;
		if (startNode.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY) {
			SwitchStatement block= (SwitchStatement)ASTNodes.getParent(startNode, SwitchStatement.class);
			statements= block.statements();
		} else {
			Block block= (Block)ASTNodes.getParent(startNode, Block.class);
			statements= block.statements();
		}
		List bodyAfterSelection= statements.subList(statements.indexOf(startNode) + 1, statements.size());
		
		List result= new ArrayList();
		if (!bodyAfterSelection.isEmpty()) {
		
			IVariableBinding[] accesses= getAccesses((ASTNode[]) bodyAfterSelection.toArray(new ASTNode[bodyAfterSelection.size()]), maxVariableId);
			
			for (int i= 0; i < accesses.length; i++) {
				IVariableBinding curVar= accesses[i];
				if (!curVar.isField()) {
					ASTNode readDecl= ASTNodes.findDeclaration(curVar, getRootNode());
					if (readDecl instanceof VariableDeclarationFragment) {
						result.add(readDecl);
					}
				}
			}
		}
		return result;
	}

	/**
	 * @param region The region to inspect
	 * @param maxVariableId Max number of variables in region
	 * @return All variables with read access in region
	 */
	private IVariableBinding[] getReads(ASTNode[] region, int maxVariableId) {
		FlowContext flowContext= new FlowContext(0, maxVariableId);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InOutFlowAnalyzer(flowContext).perform(region);
		IVariableBinding[] reads= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.UNKNOWN);
		return reads;
	}

	/**
	 * @param region The region to inspect
	 * @param maxVariableId Max number of variables in region
	 * @return All variables with read or write access in region
	 */
	private IVariableBinding[] getAccesses(ASTNode[] region, int maxVariableId) {
		FlowContext flowContext= new FlowContext(0, maxVariableId);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InOutFlowAnalyzer(flowContext).perform(region);
		IVariableBinding[] varsAccessedAfter= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL  | FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);
		return varsAccessedAfter;
	}

	/**
	 * Moves the nodes in toMove to <code>block</block> except the VariableDeclarationFragments
	 * in <code>accessedAfter</code>. The initializers (if any) of variable declarations
	 * in <code>accessedAfter</code> are moved to the block if the variable declaration is
	 * part of <code>toMove</code>. VariableDeclarations in <code>accessedInside</code> are 
	 * made final unless they are moved to <code>block</code>.
	 * 
	 * i.e. (if <code>i</code> is element of <code>accessedAfter</code>):
	 * <code>int i= 10;</code> ---> <code>int i;</code> and <code>{i= 10;}</code>
	 * 
	 * Declarations with more then one fragments are splited if required. i.e.:
	 * <code>int i,j,k;</code> ---> <code>int i,j; final int k;</code> 
	 * 
	 * @param toMove Nodes to be moved to block
	 * @param statements List to move to.
	 * @param accessedAfter VariableDeclarationFragments which can not be moved to block
	 * @param accessedInside VariableDeclaration which can be made final
	 * @param rewrite The rewrite to use.
	 */
	private final void moveToBlock(Statement[] toMove, List statements, final List/*<VariableDeclarationFragment>*/ accessedAfter, final List/*<VariableDeclaration>*/ accessedInside, final ASTRewrite rewrite) {

		for (int i= 0; i < toMove.length; i++) {
			ASTNode node= toMove[i];
			if (node instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement statement= (VariableDeclarationStatement)node;
				final ListRewrite blockRewrite= getListRewrite(statement, rewrite);
				
				splitVariableDeclarationStatement(statement, createSplitSelectedOperator(accessedAfter, accessedInside, rewrite, statements, blockRewrite), rewrite);
				
				for (Iterator iter= statement.fragments().iterator(); iter.hasNext();) {
					accessedInside.remove(iter.next());
				}
			} else {
				insertNodeAtEnd(rewrite, statements, node);
			}
		}
		
		while (!accessedInside.isEmpty()) {
			VariableDeclaration variableDeclaration= (VariableDeclaration)accessedInside.get(0);
			if (variableDeclaration instanceof SingleVariableDeclaration) {
				if (ASTNodes.findModifierNode(Modifier.FINAL, ASTNodes.getModifiers(variableDeclaration)) == null) {
					ModifierRewrite.create(rewrite, variableDeclaration).setModifiers(Modifier.FINAL, Modifier.NONE, null);
				}
				accessedInside.remove(0);
			} else if (variableDeclaration.getParent() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement statement= (VariableDeclarationStatement)variableDeclaration.getParent();
				final ListRewrite blockRewrite= getListRewrite(statement, rewrite);
				
				splitVariableDeclarationStatement(statement, createSplitUnselectedOperator(accessedInside, rewrite, blockRewrite), rewrite);
				
				for (Iterator iter= statement.fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
					accessedInside.remove(fragment);
				}
			} else if (variableDeclaration.getParent() instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression expression= (VariableDeclarationExpression)variableDeclaration.getParent();
				
				VariableDeclarationRewrite.rewriteModifiers(expression, Modifier.FINAL, 0, rewrite, null);
				
				for (Iterator iter= expression.fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
					accessedInside.remove(fragment);
				}
			}
		}
	}

	private void insertNodeAtEnd(final ASTRewrite rewrite, final List statements, ASTNode node) {
		statements.add(rewrite.createMoveTarget(node));
	}

	protected ISplitOperation createSplitUnselectedOperator(List accessedInside, ASTRewrite rewrite, ListRewrite blockRewrite) {
		return new SplitUnselectedOperator(accessedInside, blockRewrite, rewrite);
	}

	protected ISplitOperation createSplitSelectedOperator(List accessedAfter, List accessedInside, ASTRewrite rewrite, List statements, ListRewrite blockRewrite) {
		return new SplitSelectedOperator(accessedInside, accessedAfter, blockRewrite, rewrite, statements);
	}

	/**
	 * Split the fragments in <code>statement</code> to multiple VariableDeclarationStatements whenever
	 * <code>splitOperator.needsSplit</code> returns <code>true</code>.
	 * i.e.:
	 * int i, j; ---> int i; int j; (if splitOperator.needsSplit(i, j) == true)
	 * 
	 * @param statement The VariableDeclarationStatement to split
	 * @param splitOperator The operator to use to split
	 * @param rewrite The rewriter to use to generate new VariableDeclarationStatements.
	 */
	private void splitVariableDeclarationStatement(VariableDeclarationStatement statement, ISplitOperation splitOperator, ASTRewrite rewrite) {
		
		List fragments= statement.fragments();
		Iterator iter= fragments.iterator();
		VariableDeclarationFragment lastFragment= (VariableDeclarationFragment)iter.next();
		VariableDeclarationStatement lastStatement= statement;
		
		splitOperator.initializeStatement(lastStatement, lastFragment);
		
		ListRewrite fragmentsRewrite= null;
		while (iter.hasNext()) {
			VariableDeclarationFragment currentFragment= (VariableDeclarationFragment)iter.next();
			
			if (splitOperator.needsSplit(lastFragment, currentFragment)) {
					
					VariableDeclarationStatement newStatement= getAst().newVariableDeclarationStatement((VariableDeclarationFragment)rewrite.createMoveTarget(currentFragment));
					
					ListRewrite modifierRewrite= rewrite.getListRewrite(newStatement, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
					for (Iterator iterator= statement.modifiers().iterator(); iterator.hasNext();) {
						modifierRewrite.insertLast(rewrite.createCopyTarget((ASTNode)iterator.next()), null);
					}
					
					newStatement.setType((Type)rewrite.createCopyTarget(statement.getType()));
					
					splitOperator.initializeStatement(newStatement, currentFragment);
					
					fragmentsRewrite= rewrite.getListRewrite(newStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
					
					lastStatement= newStatement;
			} else if (fragmentsRewrite != null) {
				ASTNode fragment0= rewrite.createMoveTarget(currentFragment);
				fragmentsRewrite.insertLast(fragment0, null);
			}
			lastFragment= currentFragment;
		}
	}

	/**
	 * Make statement final
	 * @param statement
	 * @param rewrite
	 */
	protected static void makeFinal(VariableDeclarationStatement statement, ASTRewrite rewrite) {
		VariableDeclaration fragment= (VariableDeclaration)statement.fragments().get(0);
		if (ASTNodes.findModifierNode(Modifier.FINAL, ASTNodes.getModifiers(fragment)) == null) {
			ModifierRewrite.create(rewrite, statement).setModifiers(Modifier.FINAL, Modifier.NONE, null);
		}
	}
	
	private void qualifyThisExpressions(ASTNode node, final ASTRewrite rewrite, final ImportRewrite importRewrite) {
		node.accept(new GenericVisitor() {
			/**
			 * {@inheritDoc}
			 */
			public boolean visit(ThisExpression thisExpr) {
				if (thisExpr.getQualifier() == null) {
					ITypeBinding typeBinding= thisExpr.resolveTypeBinding();
					if (typeBinding != null) {
						IJavaScriptElement javaElement= typeBinding.getJavaElement();
						if (javaElement instanceof IType) {
							String typeName= ((IType)javaElement).getElementName();
							SimpleName simpleName= thisExpr.getAST().newSimpleName(typeName);
							rewrite.set(thisExpr, ThisExpression.QUALIFIER_PROPERTY, simpleName, null);	
						}
					}
				}
				return super.visit(thisExpr);
			}
		});
	}
	
	/**
	 * Split off initializer in <code>fragment</code> (if any) and add it as a new expression at the end of <code>statements</code>.
	 * @param statements The home of the new expression.
	 * @param fragment The fragment to split.
	 * @param rewrite The rewrite to use.
	 */
	protected static void splitOffInitializer(List statements, VariableDeclarationFragment fragment, ASTRewrite rewrite) {
		Expression initializer= fragment.getInitializer();
		if (initializer != null) {
			AST ast= rewrite.getAST();
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide((Expression)rewrite.createCopyTarget(fragment.getName()));
			assignment.setRightHandSide((Expression)rewrite.createMoveTarget(initializer));
			statements.add(ast.newExpressionStatement(assignment));
		}
	}

	/**
	 * Get a list rewrite for statement sequence node is element
	 * @param node
	 * @param rewrite
	 * @return The list rewrite
	 */
	private ListRewrite getListRewrite(ASTNode node, ASTRewrite rewrite) {
		if (node.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY) {
			ASTNode block= ASTNodes.getParent(node, SwitchStatement.class);
			return rewrite.getListRewrite(block, SwitchStatement.STATEMENTS_PROPERTY);
		} else {
			ASTNode block= ASTNodes.getParent(node, Block.class);
			return rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		}
	}

	protected final AST getAst() {
		return getRootNode().getAST();
	}

	protected final Statement[] getSelectedStatements() {
		return fSelectedStatements;
	}

	private JavaScriptUnit getRootNode() {
		if (fSelectedStatements.length > 0)
			return (JavaScriptUnit)fSelectedStatements[0].getRoot();
		return fRootNode;
	}

}
