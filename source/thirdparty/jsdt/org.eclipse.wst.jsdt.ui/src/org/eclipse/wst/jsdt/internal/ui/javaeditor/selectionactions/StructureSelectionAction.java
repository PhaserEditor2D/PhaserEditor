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
package org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

public abstract class StructureSelectionAction extends Action {

	public static final String NEXT= "SelectNextElement"; //$NON-NLS-1$
	public static final String PREVIOUS= "SelectPreviousElement"; //$NON-NLS-1$
	public static final String ENCLOSING= "SelectEnclosingElement"; //$NON-NLS-1$
	public static final String HISTORY= "RestoreLastSelection"; //$NON-NLS-1$

	private JavaEditor fEditor;
	private SelectionHistory fSelectionHistory;

	protected StructureSelectionAction(String text, JavaEditor editor, SelectionHistory history) {
		super(text);
		Assert.isNotNull(editor);
		Assert.isNotNull(history);
		fEditor= editor;
		fSelectionHistory= history;
	}

	/*
	 * This constructor is for testing purpose only.
	 */
	protected StructureSelectionAction() {
		super(""); //$NON-NLS-1$
	}

	/*
	 * Method declared in IAction.
	 */
	public final  void run() {
		IJavaScriptElement inputElement= EditorUtility.getEditorInputJavaElement(fEditor, false);
		if (!(inputElement instanceof ISourceReference && inputElement.exists()))
			return;

		ISourceReference source= (ISourceReference)inputElement;
		ISourceRange sourceRange;
		try {
			sourceRange= source.getSourceRange();
			if (sourceRange == null || sourceRange.getLength() == 0) {
				MessageDialog.openInformation(fEditor.getEditorSite().getShell(),
					SelectionActionMessages.StructureSelect_error_title,
					SelectionActionMessages.StructureSelect_error_message);
				return;
			}
		} catch (JavaScriptModelException e) {
		}
		ITextSelection selection= getTextSelection();
		ISourceRange newRange= getNewSelectionRange(createSourceRange(selection), source);
		// Check if new selection differs from current selection
		if (selection.getOffset() == newRange.getOffset() && selection.getLength() == newRange.getLength())
			return;
		fSelectionHistory.remember(new SourceRange(selection.getOffset(), selection.getLength()));
		try {
			fSelectionHistory.ignoreSelectionChanges();
			fEditor.selectAndReveal(newRange.getOffset(), newRange.getLength());
		} finally {
			fSelectionHistory.listenToSelectionChanges();
		}
	}

	public final ISourceRange getNewSelectionRange(ISourceRange oldSourceRange, ISourceReference sr) {
		try{
			JavaScriptUnit root= getAST(sr);
			if (root == null)
				return oldSourceRange;
			Selection selection= Selection.createFromStartLength(oldSourceRange.getOffset(), oldSourceRange.getLength());
			SelectionAnalyzer selAnalyzer= new SelectionAnalyzer(selection, true);
			root.accept(selAnalyzer);
			return internalGetNewSelectionRange(oldSourceRange, sr, selAnalyzer);
	 	}	catch (JavaScriptModelException e){
	 		JavaScriptPlugin.log(e); //dialog would be too heavy here
	 		return new SourceRange(oldSourceRange.getOffset(), oldSourceRange.getLength());
	 	}
	}

	/**
	 * Subclasses determine the actual new selection.
	 */
	abstract ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ISourceReference sr, SelectionAnalyzer selAnalyzer) throws JavaScriptModelException;

	protected final ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}
	
	// -- helper methods for subclasses to fit a node range into the source range

	protected static ISourceRange getLastCoveringNodeRange(ISourceRange oldSourceRange, ISourceReference sr, SelectionAnalyzer selAnalyzer) throws JavaScriptModelException {
		if (selAnalyzer.getLastCoveringNode() == null)
			return oldSourceRange;
		else
			return getSelectedNodeSourceRange(sr, selAnalyzer.getLastCoveringNode());
	}

	protected static ISourceRange getSelectedNodeSourceRange(ISourceReference sr, ASTNode nodeToSelect) throws JavaScriptModelException {
		int offset= nodeToSelect.getStartPosition();
		int end= Math.min(sr.getSourceRange().getLength(), nodeToSelect.getStartPosition() + nodeToSelect.getLength() - 1);
		return createSourceRange(offset, end);
	}

	//-- private helper methods

	private static ISourceRange createSourceRange(ITextSelection ts){
		return new SourceRange(ts.getOffset(), ts.getLength());
	}

	private static JavaScriptUnit getAST(ISourceReference sr) {
		return ASTProvider.getASTProvider().getAST((IJavaScriptElement) sr, ASTProvider.WAIT_YES, null);
	}

	//-- helper methods for this class and subclasses

	static ISourceRange createSourceRange(int offset, int end){
		int length= end - offset + 1;
		if (length == 0) //to allow 0-length selection
			length= 1;
		return new SourceRange(Math.max(0, offset), length);
	}

	static ASTNode[] getSiblingNodes(ASTNode node) {
		ASTNode parent= node.getParent();
		StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
		if (locationInParent.isChildListProperty()) {
			List siblings= (List) parent.getStructuralProperty(locationInParent);
			return (ASTNode[]) siblings.toArray(new ASTNode[siblings.size()]);
		}
		return null;
	}

	static int findIndex(Object[] array, Object o){
		for (int i= 0; i < array.length; i++) {
			Object object= array[i];
			if (object == o)
				return i;
		}
		return -1;
	}

}
