/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Eicher (Avaloq Evolution AG) - block selection mode
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IndentUtil.IndentResult;

/**
 * Action for moving selected lines in a Java editor.
 * 
 */
public class JavaMoveLinesAction extends TextEditorAction {

	/**
	 * State shared by the Move / Copy lines action quadruple.
	 * 
	 */
	private static final class SharedState {
		/** The compilation unit editor that all four actions operate on. */
		public CompilationUnitEditor fEditor;
		/**
		 * The indent token shared by all four actions.
		 */
		public IndentResult fResult= null;
		/**
		 * Set to true before modifying the document, to false after.
		 */
		boolean fIsChanging= false;
		
		/** <code>true</code> if a compound move / copy is going on. */
		private boolean fEditInProgress= false;
		/** The exit strategy that will detect the ending of a compound edit */
		private final CompoundEditExitStrategy fExitStrategy;
		
		public SharedState(CompilationUnitEditor editor) {
			fEditor= editor;
			fExitStrategy= new CompoundEditExitStrategy(new String[] {ITextEditorActionDefinitionIds.MOVE_LINES_UP, ITextEditorActionDefinitionIds.MOVE_LINES_DOWN, ITextEditorActionDefinitionIds.COPY_LINES_UP, ITextEditorActionDefinitionIds.COPY_LINES_DOWN});
			fExitStrategy.addCompoundListener(new ICompoundEditListener() {
				public void endCompoundEdit() {
					SharedState.this.endCompoundEdit();
				}
			});
		}
		
		/**
		 * Ends the compound change.
		 */
		public void beginCompoundEdit() {
			if (fEditInProgress || fEditor == null)
				return;

			fEditInProgress= true;

			fExitStrategy.arm(fEditor.getViewer());

			IRewriteTarget target= (IRewriteTarget)fEditor.getAdapter(IRewriteTarget.class);
			if (target != null) {
				target.beginCompoundChange();
			}
		}
		/**
		 * Ends the compound change.
		 */
		public void endCompoundEdit() {
			if (!fEditInProgress || fEditor == null)
				return;

			fExitStrategy.disarm();

			IRewriteTarget target= (IRewriteTarget)fEditor.getAdapter(IRewriteTarget.class);
			if (target != null) {
				target.endCompoundChange();
			}

			fResult= null;
			fEditInProgress= false;
		}
	}

	/* keys */

	/** Key for status message upon illegal move. <p>Value {@value}</p> */

	/* state variables - define what this action does */

	/** <code>true</code> if lines are shifted upwards, <code>false</code> otherwise. */
	private final boolean fUpwards;
	/** <code>true</code> if lines are to be copied instead of moved. */
	private final boolean fCopy;
	/** The shared state of the move/copy action quadruple. */
	private final SharedState fSharedState;

	/**
	 * Creates the quadruple of move and copy actions. The returned array contains
	 * the actions in the following order:
	 * [0] move up
	 * [1] move down
	 * [2] copy up (duplicate)
	 * [3] copy down (duplicate & select)
	 * @param bundle the resource bundle
	 * @param editor the editor
	 * @return the quadruple of actions
	 */
	public static JavaMoveLinesAction[] createMoveCopyActionSet(ResourceBundle bundle, CompilationUnitEditor editor) {
		SharedState state= new SharedState(editor);
		JavaMoveLinesAction[] actions= new JavaMoveLinesAction[4];
		actions[0]= new JavaMoveLinesAction(bundle, "Editor.MoveLinesUp.", true, false, state); //$NON-NLS-1$
		actions[1]= new JavaMoveLinesAction(bundle, "Editor.MoveLinesDown.", false, false, state); //$NON-NLS-1$
		actions[2]= new JavaMoveLinesAction(bundle, "Editor.CopyLineUp.", true, true, state); //$NON-NLS-1$
		actions[3]= new JavaMoveLinesAction(bundle, "Editor.CopyLineDown.", false, true, state); //$NON-NLS-1$
		return actions;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.TextEditorAction#setEditor(org.eclipse.ui.texteditor.ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		Assert.isTrue(editor instanceof CompilationUnitEditor);
		super.setEditor(editor);
		if (fSharedState != null)
			fSharedState.fEditor= (CompilationUnitEditor) editor;
	}

	/**
	 * Creates and initializes the action for the given text editor.
	 * The action configures its visual representation from the given resource
	 * bundle.
	 *
	 * @param bundle the resource bundle
	 * @param prefix a prefix to be prepended to the various resource keys
	 *   (described in <code>ResourceAction</code> constructor), or  <code>null</code> if none
	 * @param upwards <code>true</code>if the selected lines should be moved upwards,
	 * <code>false</code> if downwards
	 * @param copy if <code>true</code>, the action will copy lines instead of moving them
	 * @param state the shared state
	 * @see TextEditorAction#TextEditorAction(ResourceBundle, String, ITextEditor)
	 */
	private JavaMoveLinesAction(ResourceBundle bundle, String prefix, boolean upwards, boolean copy, SharedState state) {
		super(bundle, prefix, state.fEditor);
		fUpwards= upwards;
		fCopy= copy;
		fSharedState= state;
		update();
	}

	/**
	 * Checks if <code>selection</code> is contained by the visible region of <code>viewer</code>.
	 * As a special case, a selection is considered contained even if it extends over the visible
	 * region, but the extension stays on a partially contained line and contains only white space.
	 *
	 * @param selection the selection to be checked
	 * @param viewer the viewer displaying a visible region of <code>selection</code>'s document.
	 * @return <code>true</code>, if <code>selection</code> is contained, <code>false</code> otherwise.
	 */
	private boolean containedByVisibleRegion(ITextSelection selection, ISourceViewer viewer) {
		int min= selection.getOffset();
		int max= min + selection.getLength();
		IDocument document= viewer.getDocument();

		IRegion visible;
		if (viewer instanceof ITextViewerExtension5)
			visible= ((ITextViewerExtension5) viewer).getModelCoverage();
		else
			visible= viewer.getVisibleRegion();

		int visOffset= visible.getOffset();
		try {
			if (visOffset > min) {
				if (document.getLineOfOffset(visOffset) != selection.getStartLine())
					return false;
				if (!isWhitespace(document.get(min, visOffset - min))) {
					showStatus();
					return false;
				}
			}
			int visEnd= visOffset + visible.getLength();
			if (visEnd < max) {
				if (document.getLineOfOffset(visEnd) != selection.getEndLine())
					return false;
				if (!isWhitespace(document.get(visEnd, max - visEnd))) {
					showStatus();
					return false;
				}
			}
			return true;
		} catch (BadLocationException e) {
		}
		return false;
	}

	/**
	 * Given a selection on a document, computes the lines fully or partially covered by
	 * <code>selection</code>. A line in the document is considered covered if
	 * <code>selection</code> comprises any characters on it, including the terminating delimiter.
	 * <p>Note that the last line in a selection is not considered covered if the selection only
	 * comprises the line delimiter at its beginning (that is considered part of the second last
	 * line).
	 * As a special case, if the selection is empty, a line is considered covered if the caret is
	 * at any position in the line, including between the delimiter and the start of the line. The
	 * line containing the delimiter is not considered covered in that case.
	 * </p>
	 *
	 * @param document the document <code>selection</code> refers to
	 * @param selection a selection on <code>document</code>
	 * @param viewer the <code>ISourceViewer</code> displaying <code>document</code>
	 * @return a selection describing the range of lines (partially) covered by
	 * <code>selection</code>, without any terminating line delimiters
	 * @throws BadLocationException if the selection is out of bounds (when the underlying document has changed during the call)
	 */
	private ITextSelection getMovingSelection(IDocument document, ITextSelection selection, ISourceViewer viewer) throws BadLocationException {
		int low= document.getLineOffset(selection.getStartLine());
		int endLine= selection.getEndLine();
		int high= document.getLineOffset(endLine) + document.getLineLength(endLine);

		// get everything up to last line without its delimiter
		String delim= document.getLineDelimiter(endLine);
		if (delim != null)
			high -= delim.length();

		return new TextSelection(document, low, high - low);
	}

	/**
	 * Computes the region of the skipped line given the text block to be moved. If
	 * <code>fUpwards</code> is <code>true</code>, the line above <code>selection</code>
	 * is selected, otherwise the line below.
	 *
	 * @param document the document <code>selection</code> refers to
	 * @param selection the selection on <code>document</code> that will be moved.
	 * @return the region comprising the line that <code>selection</code> will be moved over, without its terminating delimiter.
	 */
	private ITextSelection getSkippedLine(IDocument document, ITextSelection selection) {
		int skippedLineN= (fUpwards ? selection.getStartLine() - 1 : selection.getEndLine() + 1);
		if (skippedLineN > document.getNumberOfLines() || (!fCopy && (skippedLineN < 0 ||  skippedLineN == document.getNumberOfLines())))
			return null;
		try {
			if (fCopy && skippedLineN == -1)
				skippedLineN= 0;
			IRegion line= document.getLineInformation(skippedLineN);
			return new TextSelection(document, line.getOffset(), line.getLength());
		} catch (BadLocationException e) {
			// only happens on concurrent modifications
			return null;
		}
	}

	/**
	 * Checks for white space in a string.
	 *
	 * @param string the string to be checked or <code>null</code>
	 * @return <code>true</code> if <code>string</code> contains only white space or is
	 * <code>null</code>, <code>false</code> otherwise
	 */
	private boolean isWhitespace(String string) {
		return string == null ? true : string.trim().length() == 0;
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void runWithEvent(Event event) {

		// get involved objects
		if (fSharedState.fEditor == null)
			return;

		if (!validateEditorInputState())
			return;

		ISourceViewer viewer= fSharedState.fEditor.getViewer();
		if (viewer == null)
			return;

		IDocument document= viewer.getDocument();
		if (document == null)
			return;

		StyledText widget= viewer.getTextWidget();
		if (widget == null)
			return;

		// get selection
		ITextSelection sel= (ITextSelection) viewer.getSelectionProvider().getSelection();
		if (sel.isEmpty())
			return;

		ITextSelection skippedLine= getSkippedLine(document, sel);
		if (skippedLine == null)
			return;

		try {

			ITextSelection movingArea= getMovingSelection(document, sel, viewer);

			// if either the skipped line or the moving lines are outside the widget's
			// visible area, bail out
			if (!containedByVisibleRegion(movingArea, viewer) || !containedByVisibleRegion(skippedLine, viewer))
				return;

			// get the content to be moved around: the moving (selected) area and the skipped line
			String moving= movingArea.getText();
			String skipped= skippedLine.getText();
			if (moving == null || skipped == null || document.getLength() == 0)
				return;

			String delim;
			String insertion;
			int offset;
			if (fUpwards) {
				delim= document.getLineDelimiter(skippedLine.getEndLine());
				if (fCopy) {
					delim= TextUtilities.getDefaultLineDelimiter(document);
					insertion= moving + delim;
					offset= movingArea.getOffset();
				} else {
					Assert.isNotNull(delim);
					insertion= moving + delim + skipped;
					offset= skippedLine.getOffset();
				}
			} else {
				delim= document.getLineDelimiter(movingArea.getEndLine());
				if (fCopy) {
					if (delim == null) {
						delim= TextUtilities.getDefaultLineDelimiter(document);
						insertion= delim + moving;
					} else
						insertion= moving + delim;
					offset= skippedLine.getOffset();
				} else {
					Assert.isNotNull(delim);
					insertion= skipped + delim + moving;
					offset= movingArea.getOffset();
				}
			}
			int lenght= fCopy ? 0 : insertion.length();

			// modify the document
			ILineRange selectionBefore= getLineRange(document, movingArea);
			
			if (fCopy)
				fSharedState.endCompoundEdit();
			fSharedState.beginCompoundEdit();
			fSharedState.fIsChanging= true;
			
			document.replace(offset, lenght, insertion);
			
			ILineRange selectionAfter;
			if (fUpwards && fCopy)
				selectionAfter= selectionBefore;
			else if (fUpwards)
				selectionAfter= new LineRange(selectionBefore.getStartLine() - 1, selectionBefore.getNumberOfLines());
			else if (fCopy)
				selectionAfter= new LineRange(selectionBefore.getStartLine() + selectionBefore.getNumberOfLines(), selectionBefore.getNumberOfLines());
			else
				selectionAfter= new LineRange(selectionBefore.getStartLine() + 1, selectionBefore.getNumberOfLines());
			
			fSharedState.fResult= IndentUtil.indentLines(document, selectionAfter, getProject(), fSharedState.fResult);
			
			// move the selection along
			IRegion region= getRegion(document, selectionAfter);
			selectAndReveal(viewer, region.getOffset(), region.getLength());
			
		} catch (BadLocationException x) {
			// won't happen without concurrent modification - bail out
			return;
		} finally {
			fSharedState.fIsChanging= false;
			if (fCopy)
				fSharedState.endCompoundEdit();
		}
	}

	private IJavaScriptProject getProject() {
		IEditorInput editorInput= fSharedState.fEditor.getEditorInput();
		IJavaScriptUnit unit= JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);
		if (unit != null)
			return unit.getJavaScriptProject();
		return null;
	}

	private ILineRange getLineRange(IDocument document, ITextSelection selection) throws BadLocationException {
		final int offset= selection.getOffset();
		int startLine= document.getLineOfOffset(offset);
		int endOffset= offset + selection.getLength();
		int endLine= document.getLineOfOffset(endOffset);
		final int nLines= endLine - startLine + 1;
		return new LineRange(startLine, nLines);
	}
	
	private IRegion getRegion(IDocument document, ILineRange lineRange) throws BadLocationException {
		final int startLine= lineRange.getStartLine();
		int offset= document.getLineOffset(startLine);
		final int numberOfLines= lineRange.getNumberOfLines();
		if (numberOfLines < 1)
			return new Region(offset, 0);
		int endLine = startLine + numberOfLines - 1;
		int endOffset;
		if (fSharedState.fEditor.isBlockSelectionModeEnabled()) {
			// in column mode, don't select the last delimiter as we count an
			// empty selected line
			IRegion endLineInfo = document.getLineInformation(endLine);
			endOffset = endLineInfo.getOffset() + endLineInfo.getLength();
		}
		else {
			endOffset = document.getLineOffset(endLine) + document.getLineLength(endLine);
		}
		return new Region(offset, endOffset - offset);
	}

	/**
	 * Performs similar to AbstractTextEditor.selectAndReveal, but does not update
	 * the viewers highlight area.
	 *
	 * @param viewer the viewer that we want to select on
	 * @param offset the offset of the selection
	 * @param length the length of the selection
	 */
	private void selectAndReveal(ITextViewer viewer, int offset, int length) {
		// invert selection to avoid jumping to the end of the selection in st.showSelection()
		viewer.setSelectedRange(offset + length, -length);
		//viewer.revealRange(offset, length); // will trigger jumping
		StyledText st= viewer.getTextWidget();
		if (st != null)
			st.showSelection(); // only minimal scrolling
	}

	/**
	 * Displays information in the status line why a line move is not possible
	 */
	private void showStatus() {
		IEditorStatusLine status= (IEditorStatusLine) fSharedState.fEditor.getAdapter(IEditorStatusLine.class);
		if (status == null)
			return;
		status.setMessage(false, JavaEditorMessages.Editor_MoveLines_IllegalMove_status, null);
	}

	/*
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		super.update();

		if (isEnabled())
			setEnabled(canModifyEditor());

	}
}
