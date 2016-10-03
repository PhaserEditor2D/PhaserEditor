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

import java.util.Iterator;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.compare.JavaTokenComparator;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;

/**
 * A proposal for quick fixes and quick assist that work on a single compilation unit.
 * Either a {@link TextChange text change} is directly passed in the constructor or method
 * {@link #addEdits(IDocument, TextEdit)} is overridden to provide the text edits that are
 * applied to the document when the proposal is evaluated.
 * <p>
 * The proposal takes care of the preview of the changes as proposal information.
 * </p>
 * 
 */
public class CUCorrectionProposal extends ChangeCorrectionProposal  {

	private IJavaScriptUnit fCompilationUnit;
	private LinkedProposalModel fLinkedProposalModel;


	/**
	 * Constructs a correction proposal working on a compilation unit with a given text change
	 * 
	 * @param name the name that is displayed in the proposal selection dialog.
	 * @param cu the compilation unit on that the change works.
	 * @param change the change that is executed when the proposal is applied or <code>null</code>
	 * if implementors override {@link #addEdits(IDocument, TextEdit)} to provide
	 * the text edits or {@link #createTextChange()} to provide a text change.
	 * @param relevance the relevance of this proposal.
	 * @param image the image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public CUCorrectionProposal(String name, IJavaScriptUnit cu, TextChange change, int relevance, Image image) {
		super(name, change, relevance, image);
		if (cu == null) {
			throw new IllegalArgumentException("Compilation unit must not be null"); //$NON-NLS-1$
		}
		fCompilationUnit= cu;
		fLinkedProposalModel= null;
	}
	
	/**
	 * Constructs a correction proposal working on a compilation unit.
	 * <p>Users have to override {@link #addEdits(IDocument, TextEdit)} to provide
	 * the text edits or {@link #createTextChange()} to provide a text change.
	 * </p>
	 * 
	 * @param name The name that is displayed in the proposal selection dialog.
	 * @param cu The compilation unit on that the change works.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	protected CUCorrectionProposal(String name, IJavaScriptUnit cu, int relevance, Image image) {
		this(name, cu, null, relevance, image);
	}

	/**
	 * Called when the {@link CompilationUnitChange} is initialized. Subclasses can override to
	 * add text edits to the root edit of the change. Implementors must not access the proposal,
	 * e.g getting the change.
	 * <p>The default implementation does not add any edits</p>
	 * 
	 * @param document content of the underlying compilation unit. To be accessed read only.
	 * @param editRoot The root edit to add all edits to
	 * @throws CoreException can be thrown if adding the edits is failing.
	 */
	protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
		if (false) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, "Implementors can throw an exception", null)); //$NON-NLS-1$
		}
	}
	
	protected LinkedProposalModel getLinkedProposalModel() {
		if (fLinkedProposalModel == null) {
			fLinkedProposalModel= new LinkedProposalModel();
		}
		return fLinkedProposalModel;
	}
	
	protected void setLinkedProposalModel(LinkedProposalModel model) {
		fLinkedProposalModel= model;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();

		try {
			TextChange change= getTextChange();

			IDocument previewContent= change.getPreviewDocument(new NullProgressMonitor());
			String currentConentString= change.getCurrentContent(new NullProgressMonitor());

			/*
			 * Do not change the type of those local variables. We use Object
			 * here in order to prevent loading of the Compare plug-in at load
			 * time of this class.
			 */
			Object leftSide= new JavaTokenComparator(previewContent.get());
			Object rightSide= new JavaTokenComparator(currentConentString);

			RangeDifference[] differences= RangeDifferencer.findRanges((IRangeComparator)leftSide, (IRangeComparator)rightSide);
			for (int i= 0; i < differences.length; i++) {
				RangeDifference curr= differences[i];
				int start= ((JavaTokenComparator)leftSide).getTokenStart(curr.leftStart());
				int end= ((JavaTokenComparator)leftSide).getTokenStart(curr.leftEnd());
				if (curr.kind() == RangeDifference.CHANGE && curr.leftLength() > 0) {
					buf.append("<b>"); //$NON-NLS-1$
					appendContent(previewContent, start, end, buf, false);
					buf.append("</b>"); //$NON-NLS-1$
				} else if (curr.kind() == RangeDifference.NOCHANGE) {
					appendContent(previewContent, start, end, buf, true);
				}
			}
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
		return buf.toString();
	}

	private final int surroundLines= 1;

	private void appendContent(IDocument text, int startOffset, int endOffset, StringBuffer buf, boolean surroundLinesOnly) throws BadLocationException {
		int startLine= text.getLineOfOffset(startOffset);
		int endLine= text.getLineOfOffset(endOffset);

		boolean dotsAdded= false;
		if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
			startLine= Math.max(endLine - surroundLines, 0);
			buf.append("...<br>"); //$NON-NLS-1$
			dotsAdded= true;
		}

		for (int i= startLine; i <= endLine; i++) {
			if (surroundLinesOnly) {
				if ((i - startLine > surroundLines) && (endLine - i > surroundLines)) {
					if (!dotsAdded) {
						buf.append("...<br>"); //$NON-NLS-1$
						dotsAdded= true;
					} else if (endOffset == text.getLength()) {
						return; // no surround lines for the bottom no-change range
					}
					continue;
				}
			}

			IRegion lineInfo= text.getLineInformation(i);
			int start= lineInfo.getOffset();
			int end= start + lineInfo.getLength();

			int from= Math.max(start, startOffset);
			int to= Math.min(end, endOffset);
			String content= text.get(from, to - from);
			if (surroundLinesOnly && (from == start) && Strings.containsOnlyWhitespaces(content)) {
				continue; // ignore empty lines except when range started in the middle of a line
			}
			for (int k= 0; k < content.length(); k++) {
				char ch= content.charAt(k);
				if (ch == '<') {
					buf.append("&lt;"); //$NON-NLS-1$
				} else if (ch == '>') {
					buf.append("&gt;"); //$NON-NLS-1$
				} else {
					buf.append(ch);
				}
			}
			if (to == end && to != endOffset) { // new line when at the end of the line, and not end of range
				buf.append("<br>"); //$NON-NLS-1$
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {
		try {
			IJavaScriptUnit unit= getCompilationUnit();
			IEditorPart part= null;
			if (unit.getResource().exists()) {
				boolean canEdit= performValidateEdit(unit);
				if (!canEdit) {
					return;
				}
				part= EditorUtility.isOpenInEditor(unit);
				if (part == null) {
					part= JavaScriptUI.openInEditor(unit);
					if (part != null) {
						document= JavaScriptUI.getDocumentProvider().getDocument(part.getEditorInput());
					}
				}
				IWorkbenchPage page= JavaScriptPlugin.getActivePage();
				if (page != null && part != null) {
					page.bringToTop(part);
				}
				if (part != null) {
					part.setFocus();
				}
			}
			performChange(part, document);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, CorrectionMessages.CUCorrectionProposal_error_title, CorrectionMessages.CUCorrectionProposal_error_message);
		}
	}

	private boolean performValidateEdit(IJavaScriptUnit unit) {
		IStatus status= Resources.makeCommittable(unit.getResource(), JavaScriptPlugin.getActiveWorkbenchShell());
		if (!status.isOK()) {
			String label= CorrectionMessages.CUCorrectionProposal_error_title;
			String message= CorrectionMessages.CUCorrectionProposal_error_message;
			ErrorDialog.openError(JavaScriptPlugin.getActiveWorkbenchShell(), label, message, status);
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeCorrectionProposal#performChange(org.eclipse.jface.text.IDocument, org.eclipse.ui.IEditorPart)
	 */
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		try {
			super.performChange(part, document);
			if (part == null) {
				return;
			}

			if (fLinkedProposalModel != null) {
				if (fLinkedProposalModel.hasLinkedPositions() && part instanceof JavaEditor) {
					// enter linked mode
					ITextViewer viewer= ((JavaEditor) part).getViewer();
					enterLinkedMode(viewer, part);
				} else if (part instanceof ITextEditor) {
					LinkedProposalPositionGroup.PositionInformation endPosition= fLinkedProposalModel.getEndPosition();
					if (endPosition != null) {
						// select a result
						int pos= endPosition.getOffset() + endPosition.getLength();
						((ITextEditor) part).selectAndReveal(pos, 0);
					}
				}
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}

	}

	private void enterLinkedMode(ITextViewer viewer, IEditorPart editor) throws BadLocationException {
		IDocument document= viewer.getDocument();

		LinkedModeModel model= new LinkedModeModel();
		boolean added= false;
		
		Iterator iterator= fLinkedProposalModel.getPositionGroupIterator();
		while (iterator.hasNext()) {
			LinkedProposalPositionGroup curr= (LinkedProposalPositionGroup) iterator.next();
			
			LinkedPositionGroup group= new LinkedPositionGroup();
			
			LinkedProposalPositionGroup.PositionInformation[] positions= curr.getPositions();
			if (positions.length > 0) {
				LinkedProposalPositionGroup.Proposal[] linkedModeProposals= curr.getProposals();
				if (linkedModeProposals.length <= 1) {
					for (int i= 0; i < positions.length; i++) {
						LinkedProposalPositionGroup.PositionInformation pos= positions[i];
						if (pos.getOffset() != -1) {
							group.addPosition(new LinkedPosition(document, pos.getOffset(), pos.getLength(), pos.getSequenceRank()));
						}
					}
				} else {
					LinkedPositionProposalImpl[] proposalImpls= new LinkedPositionProposalImpl[linkedModeProposals.length];
					for (int i= 0; i < linkedModeProposals.length; i++) {
						proposalImpls[i]= new LinkedPositionProposalImpl(linkedModeProposals[i], model);
					}
					
					for (int i= 0; i < positions.length; i++) {
						LinkedProposalPositionGroup.PositionInformation pos= positions[i];
						if (pos.getOffset() != -1) {
							group.addPosition(new ProposalPosition(document, pos.getOffset(), pos.getLength(), pos.getSequenceRank(), proposalImpls));
						}
					}
				}
				model.addGroup(group);
				added= true;
			}
		}

		model.forceInstall();

		if (editor instanceof JavaEditor) {
			model.addLinkingListener(new EditorHighlightingSynchronizer((JavaEditor) editor));
		}

		if (added) { // only set up UI if there are any positions set
			LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
			LinkedProposalPositionGroup.PositionInformation endPosition= fLinkedProposalModel.getEndPosition();
			if (endPosition != null && endPosition.getOffset() != -1) {
				ui.setExitPosition(viewer, endPosition.getOffset() + endPosition.getLength(), 0, Integer.MAX_VALUE);
			} else {
				int cursorPosition= viewer.getSelectedRange().x;
				if (cursorPosition != 0) {
					ui.setExitPosition(viewer, cursorPosition, 0, Integer.MAX_VALUE);
				}
			}
			ui.setExitPolicy(new LinkedModeExitPolicy());
			ui.enter();

			IRegion region= ui.getSelectedRegion();
			viewer.setSelectedRange(region.getOffset(), region.getLength());
			viewer.revealRange(region.getOffset(), region.getLength());
		}
	}

	
	/**
	 * Creates the text change for this proposal.
	 * This method is only called once and only when no text change has been passed in
	 * {@link #CUCorrectionProposal(String, IJavaScriptUnit, TextChange, int, Image)}.
	 * 
	 * @return returns the created text change.
	 * @throws CoreException thrown if the creation of the text change failed.
	 */
	protected TextChange createTextChange() throws CoreException {
		IJavaScriptUnit cu= getCompilationUnit();
		String name= getName();
		TextChange change;
		if (!cu.getResource().exists()) {
			String source;
			try {
				source= cu.getSource();
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
				source= new String(); // empty
			}
			Document document= new Document(source);
			document.setInitialLineDelimiter(StubUtility.getLineDelimiterUsed(cu));
			change= new DocumentChange(name, document);
		} else {
			CompilationUnitChange cuChange = new CompilationUnitChange(name, cu);
			cuChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
			change= cuChange;
		}
		TextEdit rootEdit= new MultiTextEdit();
		change.setEdit(rootEdit);

		// initialize text change
		IDocument document= change.getCurrentDocument(new NullProgressMonitor());
		addEdits(document, rootEdit);
		return change;
	}
		
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeCorrectionProposal#createChange()
	 */
	protected final Change createChange() throws CoreException {
		return createTextChange(); // make sure that only text changes are allowed here
	}
	
	/**
	 * Gets the text change that is invoked when the change is applied.
	 * 
	 * @return returns the text change that is invoked when the change is applied.
	 * @throws CoreException throws an exception if accessing the change failed
	 */
	public final TextChange getTextChange() throws CoreException {
		return (TextChange) getChange();
	}

	/**
	 * The compilation unit on that the change works.
	 * 
	 * @return the compilation unit on that the change works.
	 */
	public final IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Creates a preview of the content of the compilation unit after applying the change.
	 * 
	 * @return returns the preview of the changed compilation unit.
	 * @throws CoreException thrown if the creation of the change failed.
	 */
	public String getPreviewContent() throws CoreException {
		return getTextChange().getPreviewContent(new NullProgressMonitor());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		try {
			return getPreviewContent();
		} catch (CoreException e) {
		}
		return super.toString();
	}
		

	private static class LinkedModeExitPolicy implements LinkedModeUI.IExitPolicy {
		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			if (event.character  == '=') {
				return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
			}
			return null;
		}
	}
	
	private static class LinkedPositionProposalImpl implements ICompletionProposalExtension2, IJavaCompletionProposal {

		private final LinkedProposalPositionGroup.Proposal fProposal;
		private final LinkedModeModel fLinkedPositionModel;
		

		public LinkedPositionProposalImpl(LinkedProposalPositionGroup.Proposal proposal, LinkedModeModel model) {
			fProposal= proposal;
			fLinkedPositionModel= model;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
		 */
		public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
			IDocument doc= viewer.getDocument();
			LinkedPosition position= fLinkedPositionModel.findPosition(new LinkedPosition(doc, offset, 0));
			if (position != null) {
				try {
					try {
						TextEdit edit= fProposal.computeEdits(offset, position, trigger, stateMask, fLinkedPositionModel);
						if (edit != null) {
							edit.apply(position.getDocument(), 0);
						}
					} catch (MalformedTreeException e) {
						throw new CoreException(new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, "Unexpected exception applying edit", e)); //$NON-NLS-1$
					} catch (BadLocationException e) {
						throw new CoreException(new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, "Unexpected exception applying edit", e)); //$NON-NLS-1$
					}
				} catch (CoreException e) {
					JavaScriptPlugin.log(e);
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
		 */
		public String getDisplayString() {
			return fProposal.getDisplayString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
		 */
		public Image getImage() {
			return fProposal.getImage();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal#getRelevance()
		 */
		public int getRelevance() {
			return fProposal.getRelevance();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
		 */
		public void apply(IDocument document) {
			// not called
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
		 */
		public String getAdditionalProposalInfo() {
			return fProposal.getAdditionalProposalInfo();
		}

		public Point getSelection(IDocument document) { return null; }
		public IContextInformation getContextInformation() { return null; }
		public void selected(ITextViewer viewer, boolean smartToggle) {}
		public void unselected(ITextViewer viewer) {}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
		 */
		public boolean validate(IDocument document, int offset, DocumentEvent event) {
			// ignore event
			String insert= getDisplayString();

			int off;
			LinkedPosition pos= fLinkedPositionModel.findPosition(new LinkedPosition(document, offset, 0));
			if (pos != null) {
				off= pos.getOffset();
			} else {
				off= Math.max(0, offset - insert.length());
			}
			int length= offset - off;

			if (offset <= document.getLength()) {
				try {
					String content= document.get(off, length);
					if (insert.startsWith(content))
						return true;
				} catch (BadLocationException e) {
					JavaScriptPlugin.log(e);
					// and ignore and return false
				}
			}
			return false;
		}
	}
}
