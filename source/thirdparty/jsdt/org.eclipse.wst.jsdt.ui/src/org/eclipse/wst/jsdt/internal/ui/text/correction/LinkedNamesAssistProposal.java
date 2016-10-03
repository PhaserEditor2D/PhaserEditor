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

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;

/**
 * A template proposal.
 */
public class LinkedNamesAssistProposal implements IJavaCompletionProposal, ICompletionProposalExtension2, ICommandAccess {

	/**
	 * An exit policy that skips Backspace and Delete at the beginning and at the end
	 * of a linked position, respectively.
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=183925 .  
	 */
	public static class DeleteBlockingExitPolicy implements IExitPolicy {
		private IDocument fDocument;

		public DeleteBlockingExitPolicy(IDocument document) {
			fDocument= document;
		}

		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			if (length == 0 && (event.character == SWT.BS || event.character == SWT.DEL)) {
				LinkedPosition position= model.findPosition(new LinkedPosition(fDocument, offset, 0, LinkedPositionGroup.NO_STOP));
				if (position != null) {
					if (event.character == SWT.BS) {
						if (offset - 1 < position.getOffset()) {
							//skip backspace at beginning of linked position
							event.doit= false;
						}
					} else /* event.character == SWT.DEL */ {
						if (offset + 1 > position.getOffset() + position.getLength()) {
							//skip delete at end of linked position
							event.doit= false;
						}
					}
				}
			}
			
			return null; // don't change behavior
		}
	}

	
	public static final String ASSIST_ID= "org.eclipse.wst.jsdt.ui.correction.renameInFile.assist"; //$NON-NLS-1$
	
	private SimpleName fNode;
	private IJavaScriptUnit fCompilationUnit;
	private String fLabel;
	private String fValueSuggestion;
	private int fRelevance;

	public LinkedNamesAssistProposal(IJavaScriptUnit cu, SimpleName node) {
		this(CorrectionMessages.LinkedNamesAssistProposal_description, cu, node, null);
		fNode= node;
		fCompilationUnit= cu;
		fRelevance= 8;
	}

	public LinkedNamesAssistProposal(String label, IJavaScriptUnit cu, SimpleName node, String valueSuggestion) {
		fLabel= label;
		fNode= node;
		fCompilationUnit= cu;
		fValueSuggestion= valueSuggestion;
		fRelevance= 8;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		try {
			Point seletion= viewer.getSelectedRange();

			// get full ast
			JavaScriptUnit root= JavaScriptPlugin.getDefault().getASTProvider().getAST(fCompilationUnit, ASTProvider.WAIT_YES, null);

			ASTNode nameNode= NodeFinder.perform(root, fNode.getStartPosition(), fNode.getLength());
			final int pos= fNode.getStartPosition();

			ASTNode[] sameNodes;
			if (nameNode instanceof SimpleName) {
				sameNodes= LinkedNodeFinder.findByNode(root, (SimpleName) nameNode);
			} else {
				sameNodes= new ASTNode[] { nameNode };
			}

			// sort for iteration order, starting with the node @ offset
			Arrays.sort(sameNodes, new Comparator() {

				public int compare(Object o1, Object o2) {
					return rank((ASTNode) o1) - rank((ASTNode) o2);
				}

				/**
				 * Returns the absolute rank of an <code>ASTNode</code>. Nodes
				 * preceding <code>offset</code> are ranked last.
				 *
				 * @param node the node to compute the rank for
				 * @return the rank of the node with respect to the invocation offset
				 */
				private int rank(ASTNode node) {
					int relativeRank= node.getStartPosition() + node.getLength() - pos;
					if (relativeRank < 0)
						return Integer.MAX_VALUE + relativeRank;
					else
						return relativeRank;
				}

			});

			IDocument document= viewer.getDocument();
			LinkedPositionGroup group= new LinkedPositionGroup();
			for (int i= 0; i < sameNodes.length; i++) {
				ASTNode elem= sameNodes[i];
				group.addPosition(new LinkedPosition(document, elem.getStartPosition(), elem.getLength(), i));
			}

			LinkedModeModel model= new LinkedModeModel();
			model.addGroup(group);
			model.forceInstall();
			JavaEditor editor= getJavaEditor();
			if (editor != null) {
				model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
			}

			LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
			ui.setExitPolicy(new DeleteBlockingExitPolicy(document));
			ui.setExitPosition(viewer, offset, 0, LinkedPositionGroup.NO_STOP);
			ui.enter();

			if (fValueSuggestion != null) {
				document.replace(nameNode.getStartPosition(), nameNode.getLength(), fValueSuggestion);
				IRegion selectedRegion= ui.getSelectedRegion();
				seletion= new Point(selectedRegion.getOffset(), fValueSuggestion.length());
			}
			
			viewer.setSelectedRange(seletion.x, seletion.y); // by default full word is selected, restore original selection

		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
	}

	/**
	 * Returns the currently active java editor, or <code>null</code> if it
	 * cannot be determined.
	 *
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaScriptPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		// can't do anything
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return null;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return CorrectionMessages.LinkedNamesAssistProposal_proposalinfo;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		String shortCutString= CorrectionCommandHandler.getShortCutString(getCommandId());
		if (shortCutString != null) {
			return Messages.format(CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut, new String[] { fLabel, shortCutString });
		}
		return fLabel;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LINKED_RENAME);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return fRelevance;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
	 */
	public void selected(ITextViewer textViewer, boolean smartToggle) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(ITextViewer textViewer) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.IShortcutProposal#getProposalId()
	 */
	public String getCommandId() {
		return ASSIST_ID;
	}

	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

}
