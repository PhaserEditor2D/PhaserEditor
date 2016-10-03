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
package org.eclipse.wst.jsdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.dom.TokenScanner;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;

/**
 * Analyzer to check if a selection covers a valid set of statements of an abstract syntax
 * tree. The selection is valid iff
 * <ul>
 * 	<li>it does not start or end in the middle of a comment.</li>
 * 	<li>no extract characters except the empty statement ";" is included in the selection.</li>
 * </ul>
 */
public class StatementAnalyzer extends SelectionAnalyzer {

	protected IJavaScriptUnit fCUnit;
	private TokenScanner fScanner;
	private RefactoringStatus fStatus;

	public StatementAnalyzer(IJavaScriptUnit cunit, Selection selection, boolean traverseSelectedNode) throws JavaScriptModelException {
		super(selection, traverseSelectedNode);
		Assert.isNotNull(cunit);
		fCUnit= cunit;
		fStatus= new RefactoringStatus();
		fScanner= new TokenScanner(fCUnit);
	}
	
	protected void checkSelectedNodes() {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes.length == 0)
			return;
		
		ASTNode node= nodes[0];
		int selectionOffset= getSelection().getOffset();
		try {
			int pos= fScanner.getNextStartOffset(selectionOffset, true);
			if (pos == node.getStartPosition()) {
				int lastNodeEnd= ASTNodes.getExclusiveEnd(nodes[nodes.length - 1]);
				
				pos= fScanner.getNextStartOffset(lastNodeEnd, true);
				int selectionEnd= getSelection().getInclusiveEnd();
				if (pos <= selectionEnd) {
					ISourceRange range= new SourceRange(lastNodeEnd, pos - lastNodeEnd);
					invalidSelection(RefactoringCoreMessages.StatementAnalyzer_end_of_selection, JavaStatusContext.create(fCUnit, range)); 
				}
				return; // success
			}
		} catch (CoreException e) {
			// fall through
		}
		ISourceRange range= new SourceRange(selectionOffset, node.getStartPosition() - selectionOffset + 1);
		invalidSelection(RefactoringCoreMessages.StatementAnalyzer_beginning_of_selection, JavaStatusContext.create(fCUnit, range));
	}
	
	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	protected IJavaScriptUnit getCompilationUnit() {
		return fCUnit;
	}
	
	protected TokenScanner getTokenScanner() {
		return fScanner;
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(JavaScriptUnit node) {
		if (!hasSelectedNodes()) {
			super.endVisit(node);
			return;
		}
		ASTNode selectedNode= getFirstSelectedNode();
		Selection selection= getSelection();
		if (node != selectedNode) {
			ASTNode parent= selectedNode.getParent();
			fStatus.merge(CommentAnalyzer.perform(selection, fScanner.getScanner(), parent.getStartPosition(), parent.getLength()));
		}
		if (!fStatus.hasFatalError())
			checkSelectedNodes();
		super.endVisit(node);
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(DoStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getBody()) && contains(selectedNodes, node.getExpression())) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_do_body_expression); 
			}
		}
		super.endVisit(node);
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(ForStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			boolean containsExpression= contains(selectedNodes, node.getExpression());
			boolean containsUpdaters= contains(selectedNodes, node.updaters());
			if (contains(selectedNodes, node.initializers()) && containsExpression) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_initializer_expression); 
			} else if (containsExpression && containsUpdaters) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_expression_updater); 
			} else if (containsUpdaters && contains(selectedNodes, node.getBody())) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_updater_body); 
			}
		}
		super.endVisit(node);
	}
	public void endVisit(ForInStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			boolean containsVar= contains(selectedNodes, node.getIterationVariable());
			boolean containsCollection= contains(selectedNodes, node.getCollection());
			  if (containsVar && containsCollection) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_expression_updater); 
			} else if (containsCollection && contains(selectedNodes, node.getBody())) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_updater_body); 
			}
		}
		super.endVisit(node);
	}

	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(SwitchStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			List cases= getSwitchCases(node);
			for (int i= 0; i < selectedNodes.length; i++) {
				ASTNode topNode= selectedNodes[i];
				if (cases.contains(topNode)) {
					invalidSelection(RefactoringCoreMessages.StatementAnalyzer_switch_statement); 
					break;
				}
			}
		}
		super.endVisit(node);
	}

	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(TryStatement node) {
		ASTNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (firstSelectedNode == node.getBody() || firstSelectedNode == node.getFinally()) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_try_statement); 
			} else {
				List catchClauses= node.catchClauses();
				for (Iterator iterator= catchClauses.iterator(); iterator.hasNext();) {
					CatchClause element= (CatchClause)iterator.next();
					if (element == firstSelectedNode || element.getBody() == firstSelectedNode) {
						invalidSelection(RefactoringCoreMessages.StatementAnalyzer_try_statement); 
					} else if (element.getException() == firstSelectedNode) {
						invalidSelection(RefactoringCoreMessages.StatementAnalyzer_catch_argument); 
					}
				}
			}
		}
		super.endVisit(node);
	}
	
	/* (non-Javadoc)
	 * Method declared in ASTVisitor
	 */
	public void endVisit(WhileStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getExpression()) && contains(selectedNodes, node.getBody())) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_while_expression_body); 
			}
		}
		super.endVisit(node);
	}	
	
	public void endVisit(WithStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getExpression()) && contains(selectedNodes, node.getBody())) {
				invalidSelection(RefactoringCoreMessages.StatementAnalyzer_while_expression_body); 
			}
		}
		super.endVisit(node);
	}	
	

	private boolean doAfterValidation(ASTNode node, ASTNode[] selectedNodes) {
		return selectedNodes.length > 0 && node == selectedNodes[0].getParent() && getSelection().getEndVisitSelectionMode(node) == Selection.AFTER;
	}
	
	protected void invalidSelection(String message) {
		fStatus.addFatalError(message);
		reset();
	}
	
	protected void invalidSelection(String message, RefactoringStatusContext context) {
		fStatus.addFatalError(message, context);
		reset();
	}
	
	private static List getSwitchCases(SwitchStatement node) {
		List result= new ArrayList();
		for (Iterator iter= node.statements().iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof SwitchCase)
				result.add(element);
		}
		return result;
	}
	
	protected static boolean contains(ASTNode[] nodes, ASTNode node) {
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] == node)
				return true;
		}
		return false;
	}	
	
	protected static boolean contains(ASTNode[] nodes, List list) {
		for (int i = 0; i < nodes.length; i++) {
			if (list.contains(nodes[i]))
				return true;
		}
		return false;
	}	
}
