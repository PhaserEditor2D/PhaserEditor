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

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.IColorManager;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;


public class JavaCorrectionAssistant extends QuickAssistAssistant {

	private ITextViewer fViewer;
	private ITextEditor fEditor;
	private Position fPosition;
	private Annotation[] fCurrentAnnotations;

	private QuickAssistLightBulbUpdater fLightBulbUpdater;


	/**
	 * Constructor for JavaCorrectionAssistant.
	 */
	public JavaCorrectionAssistant(ITextEditor editor) {
		super();
		Assert.isNotNull(editor);
		fEditor= editor;

		JavaCorrectionProcessor processor= new JavaCorrectionProcessor(this);

		setQuickAssistProcessor(processor);

		setInformationControlCreator(getInformationControlCreator());

		JavaScriptTextTools textTools= JavaScriptPlugin.getDefault().getJavaTextTools();
		IColorManager manager= textTools.getColorManager();

		IPreferenceStore store=  JavaScriptPlugin.getDefault().getPreferenceStore();

		Color c= getColor(store, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, manager);
		setProposalSelectorForeground(c);

		c= getColor(store, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, manager);
		setProposalSelectorBackground(c);
	}

	public IEditorPart getEditor() {
		return fEditor;
	}


	private IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, new HTMLTextPresenter());
			}
		};
	}

	private static Color getColor(IPreferenceStore store, String key, IColorManager manager) {
		RGB rgb= PreferenceConverter.getColor(store, key);
		return manager.getColor(rgb);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistant#install(org.eclipse.jface.text.ITextViewer)
	 */
	public void install(ISourceViewer sourceViewer) {
		super.install(sourceViewer);
		fViewer= sourceViewer;

		fLightBulbUpdater= new QuickAssistLightBulbUpdater(fEditor, sourceViewer);
		fLightBulbUpdater.install();
	}



	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ContentAssistant#uninstall()
	 */
	public void uninstall() {
		if (fLightBulbUpdater != null) {
			fLightBulbUpdater.uninstall();
			fLightBulbUpdater= null;
		}
		super.uninstall();
	}

	/*
	 * @see org.eclipse.jface.text.quickassist.QuickAssistAssistant#showPossibleQuickAssists()
	 * 
	 */

	/**
	 * Show completions at caret position. If current
	 * position does not contain quick fixes look for
	 * next quick fix on same line by moving from left
	 * to right and restarting at end of line if the
	 * beginning of the line is reached.
	 *
	 * @see org.eclipse.jface.text.quickassist.IQuickAssistAssistant#showPossibleQuickAssists()
	 */
	public String showPossibleQuickAssists() {
		fPosition= null;
		fCurrentAnnotations= null;
		
		if (fViewer == null || fViewer.getDocument() == null)
			// Let superclass deal with this
			return super.showPossibleQuickAssists();


		ArrayList resultingAnnotations= new ArrayList(20);
		try {
			Point selectedRange= fViewer.getSelectedRange();
			int currOffset= selectedRange.x;
			int currLength= selectedRange.y;
			boolean goToClosest= (currLength == 0);
			
			int newOffset= collectQuickFixableAnnotations(fEditor, currOffset, goToClosest, resultingAnnotations);
			if (newOffset != currOffset) {
				storePosition(currOffset, currLength);
				fViewer.setSelectedRange(newOffset, 0);
				fViewer.revealRange(newOffset, 0);
			}
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
		fCurrentAnnotations= (Annotation[]) resultingAnnotations.toArray(new Annotation[resultingAnnotations.size()]);

		return super.showPossibleQuickAssists();
	}
	
	
	private static IRegion getRegionOfInterest(ITextEditor editor, int invocationLocation) throws BadLocationException {
		IDocumentProvider documentProvider= editor.getDocumentProvider();
		if (documentProvider == null) {
			return null;
		}
		IDocument document= documentProvider.getDocument(editor.getEditorInput());
		if (document == null) {
			return null;
		}
		return document.getLineInformationOfOffset(invocationLocation);
	}
	
	public static int collectQuickFixableAnnotations(ITextEditor editor, int invocationLocation, boolean goToClosest, ArrayList resultingAnnotations) throws BadLocationException {
		IAnnotationModel model= JavaScriptUI.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		if (model == null) {
			return invocationLocation;
		}
		
		ensureUpdatedAnnotations(editor);
		
		Iterator iter= model.getAnnotationIterator();
		if (goToClosest) {
			IRegion lineInfo= getRegionOfInterest(editor, invocationLocation);
			if (lineInfo == null) {
				return invocationLocation;
			}
			int rangeStart= lineInfo.getOffset();
			int rangeEnd= rangeStart + lineInfo.getLength();
			
			ArrayList allAnnotations= new ArrayList();
			ArrayList allPositions= new ArrayList();
			int bestOffset= Integer.MAX_VALUE;
			while (iter.hasNext()) {
				Annotation annot= (Annotation) iter.next();
				if (JavaCorrectionProcessor.isQuickFixableType(annot)) {
					Position pos= model.getPosition(annot);
					if (pos != null && isInside(pos.offset, rangeStart, rangeEnd)) { // inside our range?
						allAnnotations.add(annot);
						allPositions.add(pos);
						bestOffset= processAnnotation(annot, pos, invocationLocation, bestOffset);
					}
				}
			}
			if (bestOffset == Integer.MAX_VALUE) {
				return invocationLocation;
			}
			for (int i= 0; i < allPositions.size(); i++) {
				Position pos= (Position) allPositions.get(i);
				if (isInside(bestOffset, pos.offset, pos.offset + pos.length)) {
					resultingAnnotations.add(allAnnotations.get(i));
				}
			}
			return bestOffset;
		} else {
			while (iter.hasNext()) {
				Annotation annot= (Annotation) iter.next();
				if (JavaCorrectionProcessor.isQuickFixableType(annot)) {
					Position pos= model.getPosition(annot);
					if (pos != null && isInside(invocationLocation, pos.offset, pos.offset + pos.length)) {
						resultingAnnotations.add(annot);
					}
				}
			}
			return invocationLocation;
		}
	}

	private static void ensureUpdatedAnnotations(ITextEditor editor) {
		Object inputElement= editor.getEditorInput().getAdapter(IJavaScriptElement.class);
		if (inputElement instanceof IJavaScriptUnit) {
			JavaScriptPlugin.getDefault().getASTProvider().getAST((IJavaScriptUnit) inputElement, ASTProvider.WAIT_ACTIVE_ONLY, null);
		}
	}

	private static int processAnnotation(Annotation annot, Position pos, int invocationLocation, int bestOffset) {
		int posBegin= pos.offset;
		int posEnd= posBegin + pos.length;
		if (isInside(invocationLocation, posBegin, posEnd)) { // covers invocation location?
			return invocationLocation;
		} else if (bestOffset != invocationLocation) {
			int newClosestPosition= computeBestOffset(posBegin, invocationLocation, bestOffset);
			if (newClosestPosition != -1) { 
				if (newClosestPosition != bestOffset) { // new best
					if (JavaCorrectionProcessor.hasCorrections(annot)) { // only jump to it if there are proposals
						return newClosestPosition;
					}
				}
			}
		}
		return bestOffset;
	}


	private static boolean isInside(int offset, int start, int end) {
		return offset == start || offset == end || (offset > start && offset < end); // make sure to handle 0-length ranges
	}

	/**
	 * Computes and returns the invocation offset given a new
	 * position, the initial offset and the best invocation offset
	 * found so far.
	 * <p>
	 * The closest offset to the left of the initial offset is the
	 * best. If there is no offset on the left, the closest on the
	 * right is the best.</p>
	 * @return -1 is returned if the given offset is not closer or the new best offset
	 */
	private static int computeBestOffset(int newOffset, int invocationLocation, int bestOffset) {
		if (newOffset <= invocationLocation) {
			if (bestOffset > invocationLocation) {
				return newOffset; // closest was on the right, prefer on the left
			} else if (bestOffset <= newOffset) {
				return newOffset; // we are closer or equal
			}
			return -1; // further away
		}

		if (newOffset <= bestOffset)
			return newOffset; // we are closer or equal

		return -1; // further away
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ContentAssistant#possibleCompletionsClosed()
	 */
	protected void possibleCompletionsClosed() {
		super.possibleCompletionsClosed();
		restorePosition();
	}

	private void storePosition(int currOffset, int currLength) {
		fPosition= new Position(currOffset, currLength);
	}

	private void restorePosition() {
		if (fPosition != null && !fPosition.isDeleted() && fViewer.getDocument() != null) {
			fViewer.setSelectedRange(fPosition.offset, fPosition.length);
			fViewer.revealRange(fPosition.offset, fPosition.length);
		}
		fPosition= null;
	}

	/**
	 * Returns true if the last invoked completion was called with an updated offset.
	 */
	public boolean isUpdatedOffset() {
		return fPosition != null;
	}

	/**
	 * Returns the annotations at the current offset
	 */
	public Annotation[] getAnnotationsAtOffset() {
		return fCurrentAnnotations;
	}
}
