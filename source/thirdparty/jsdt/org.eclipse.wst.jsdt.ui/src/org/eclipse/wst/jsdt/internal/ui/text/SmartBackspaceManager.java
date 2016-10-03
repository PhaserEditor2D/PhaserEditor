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
package org.eclipse.wst.jsdt.internal.ui.text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.internal.ui.text.TypingRun.ChangeType;



/**
 * Installs as a verify key listener on a viewer and overwrites the behavior
 * of the backspace key. Clients may register undo specifications for certain
 * offsets in a document. The <code>SmartBackspaceManager</code> will manage the
 * specifications and execute the contained <code>TextEdit</code>s when backspace
 * is pressed at the given offset and the specification is still valid.
 * <p>
 * Undo specifications are removed after a number of typing runs.
 * </p>
 *
 * 
 */
public class SmartBackspaceManager {
	/* independent of JDT - may be moved to jface.text */

	/**
	 * An undo specification describes the change that should be executed if
	 * backspace is pressed at its trigger offset.
	 *
	 * 
	 */
	public static final class UndoSpec {
		private final int triggerOffset;
		private final IRegion selection;
		private final TextEdit[] undoEdits;
		private final UndoSpec child;
		int lives;

		/**
		 * Creates a new spec. A specification consists of a number of
		 * <code>TextEdit</code>s that will be executed when backspace is
		 * pressed at <code>triggerOffset</code>. The spec will be removed
		 * when it is executed, or if more than <code>lives</code>
		 * <code>TypingRun</code>s have ended after registering the spec.
		 * <p>
		 * Optionally, a child specification can be registered. After executing
		 * the spec, the child spec will be registered with the manager. This allows
		 * to create chains of <code>UndoSpec</code>s that will be executed upon
		 * repeated pressing of backspace.
		 * </p>
		 *
		 * @param triggerOffset the offset where this spec is active
		 * @param selection the selection after executing the undo spec
		 * @param edits the <code>TextEdit</code>s to perform when executing
		 *        the spec
		 * @param lives the number of <code>TypingRun</code>s before removing
		 *        the spec
		 * @param child a child specification that will be registered after
		 *        executing this spec, or <code>null</code>
		 */
		public UndoSpec(int triggerOffset, IRegion selection, TextEdit[] edits, int lives, UndoSpec child) {
			Assert.isLegal(triggerOffset >= 0);
			Assert.isLegal(selection != null);
			Assert.isLegal(lives >= 0);
			Assert.isLegal(edits != null);
			Assert.isLegal(edits.length > 0);
			for (int i= 0; i < edits.length; i++) {
				Assert.isLegal(edits[i] != null);
			}

			this.triggerOffset= triggerOffset;
			this.selection= selection;
			this.undoEdits= edits;
			this.lives= lives;
			this.child= child;
		}
	}


	private class BackspaceListener implements VerifyKeyListener {

		/*
		 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {
			if (fViewer != null && isBackspace(event)) {
				int offset= getCaretOffset();
				UndoSpec spec= removeEdit(offset);
				if (spec != null) {
					try {
						beginChange();
						for (int i= 0; i < spec.undoEdits.length; i++) {
							spec.undoEdits[i].apply(getDocument(), TextEdit.UPDATE_REGIONS);
						}
						fViewer.setSelectedRange(spec.selection.getOffset(), spec.selection.getLength());
						if (spec.child != null)
							register(spec.child);
					} catch (MalformedTreeException e) {
						// fall back to standard bs
						return;
					} catch (BadLocationException e) {
						// fall back to standard bs
						return;
					} finally {
						endChange();
					}
					event.doit= false;
				}

			}
		}

		private void beginChange() {
			ITextViewer viewer= fViewer;
			if (viewer instanceof TextViewer) {
				TextViewer v= (TextViewer) viewer;
				v.getRewriteTarget().beginCompoundChange();
			}
		}

		private void endChange() {
			ITextViewer viewer= fViewer;
			if (viewer instanceof TextViewer) {
				TextViewer v= (TextViewer) viewer;
				v.getRewriteTarget().endCompoundChange();
			}
		}

		private boolean isBackspace(VerifyEvent event) {
			return event.doit == true && event.character == SWT.BS && event.stateMask == 0;
		}

		private int getCaretOffset() {
			ITextViewer viewer= fViewer;
			Point point= viewer.getSelectedRange();
			return point.x;
		}

	}

	private ITextViewer fViewer;
	private BackspaceListener fBackspaceListener;
	private Map fSpecs;
	private TypingRunDetector fRunDetector;
	private ITypingRunListener fRunListener;

	/**
	 * Registers an undo specification with this manager.
	 *
	 * @param spec the specification to register
	 * @throws IllegalStateException if the manager is not installed
	 */
	public void register(UndoSpec spec) {
		if (fViewer == null)
			throw new IllegalStateException();

		ensureListenerInstalled();
		addEdit(spec);
	}

	private void addEdit(UndoSpec spec) {
		Integer i= Integer.valueOf(spec.triggerOffset);
		fSpecs.put(i, spec);
	}

	private UndoSpec removeEdit(int offset) {
		Integer i= Integer.valueOf(offset);
		UndoSpec spec= (UndoSpec) fSpecs.remove(i);
		return spec;
	}

	private void ensureListenerInstalled() {
		if (fBackspaceListener == null) {
			fBackspaceListener= new BackspaceListener();
			ITextViewer viewer= fViewer;
			if (viewer instanceof ITextViewerExtension)
				((ITextViewerExtension) viewer).prependVerifyKeyListener(fBackspaceListener);
			else
				viewer.getTextWidget().addVerifyKeyListener(fBackspaceListener);
		}
	}

	private void ensureListenerRemoved() {
		if (fBackspaceListener != null) {
			ITextViewer viewer= fViewer;
			if (viewer instanceof ITextViewerExtension)
				((ITextViewerExtension) viewer).removeVerifyKeyListener(fBackspaceListener);
			else
				viewer.getTextWidget().removeVerifyKeyListener(fBackspaceListener);
			fBackspaceListener= null;
		}
	}

	private IDocument getDocument() {
		return fViewer.getDocument();
	}

	/**
	 * Installs the receiver on a text viewer.
	 *
	 * @param viewer
	 */
	public void install(ITextViewer viewer) {
		Assert.isLegal(viewer != null);

		fViewer= viewer;
		fSpecs= new HashMap();
		fRunDetector= new TypingRunDetector();
		fRunDetector.install(viewer);
		fRunListener= new ITypingRunListener() {

			/*
			 * @see org.eclipse.jface.text.TypingRunDetector.ITypingRunListener#typingRunStarted(org.eclipse.jface.text.TypingRunDetector.TypingRun)
			 */
			public void typingRunStarted(TypingRun run) {
			}

			/*
			 * @see org.eclipse.jface.text.TypingRunDetector.ITypingRunListener#typingRunEnded(org.eclipse.jface.text.TypingRunDetector.TypingRun)
			 */
			public void typingRunEnded(TypingRun run, ChangeType reason) {
				if (reason == TypingRun.SELECTION)
					fSpecs.clear();
				else
					prune();
			}
		};
		fRunDetector.addTypingRunListener(fRunListener);
	}

	private void prune() {
		for (Iterator it= fSpecs.values().iterator(); it.hasNext();) {
			UndoSpec spec= (UndoSpec) it.next();
			if (--spec.lives < 0)
				it.remove();
		}
	}

	/**
	 * Uninstalls the receiver. No undo specifications may be registered on an
	 * uninstalled manager.
	 */
	public void uninstall() {
		if (fViewer != null) {
			fRunDetector.removeTypingRunListener(fRunListener);
			fRunDetector.uninstall();
			fRunDetector= null;
			ensureListenerRemoved();
			fViewer= null;
		}
	}
}
