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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.wst.jsdt.internal.ui.text.TypingRun.ChangeType;


/**
 * When connected to a text viewer, a <code>TypingRunDetector</code> observes
 * <code>TypingRun</code> events. A typing run is a sequence of similar text
 * modifications, such as inserting or deleting single characters.
 * <p>
 * Listeners are informed about the start and end of a <code>TypingRun</code>.
 * </p>
 *
 * 
 */
public class TypingRunDetector {
	/*
	 * Implementation note: This class is independent of JDT and may be pulled
	 * up to jface.text if needed.
	 */

	/** Debug flag. */
	private static final boolean DEBUG= false;

	/**
	 * Instances of this class abstract a text modification into a simple
	 * description. Typing runs consists of a sequence of one or more modifying
	 * changes of the same type. Every change records the type of change
	 * described by a text modification, and an offset it can be followed by
	 * another change of the same run.
	 */
	private static final class Change {
		private ChangeType fType;
		private int fNextOffset;

		/**
		 * Creates a new change of type <code>type</code>.
		 *
		 * @param type the <code>ChangeType</code> of the new change
		 * @param nextOffset the offset of the next change in a typing run
		 */
		public Change(ChangeType type, int nextOffset) {
			fType= type;
			fNextOffset= nextOffset;
		}

		/**
		 * Returns <code>true</code> if the receiver can extend the typing run
		 * the last change of which is described by <code>change</code>.
		 *
		 * @param change the last change in a typing run
		 * @return <code>true</code> if the receiver is a valid extension to
		 *         <code>change</code>, <code>false</code> otherwise
		 */
		public boolean canFollow(Change change) {
			if (fType == TypingRun.NO_CHANGE)
				return true;
			if (fType.equals(TypingRun.UNKNOWN))
				return false;
			if (fType.equals(change.fType)) {
				if (fType == TypingRun.DELETE)
					return fNextOffset == change.fNextOffset - 1;
				else if (fType == TypingRun.INSERT)
					return fNextOffset == change.fNextOffset + 1;
				else if (fType == TypingRun.OVERTYPE)
					return fNextOffset == change.fNextOffset + 1;
				else if (fType == TypingRun.SELECTION)
					return true;
			}
			return false;
		}

		/**
		 * Returns <code>true</code> if the receiver describes a text
		 * modification, <code>false</code> if it describes a focus /
		 * selection change.
		 *
		 * @return <code>true</code> if the receiver is a text modification
		 */
		public boolean isModification() {
			return fType.isModification();
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return fType.toString() + "@" + fNextOffset; //$NON-NLS-1$
		}

		/**
		 * Returns the change type of this change.
		 *
		 * @return the change type of this change
		 */
		public ChangeType getType() {
			return fType;
		}
	}

	/**
	 * Observes any events that modify the content of the document displayed in
	 * the editor. Since text events may start a new run, this listener is
	 * always registered if the detector is connected.
	 */
	private class TextListener implements ITextListener {

		/*
		 * @see org.eclipse.jface.text.ITextListener#textChanged(org.eclipse.jface.text.TextEvent)
		 */
		public void textChanged(TextEvent event) {
			handleTextChanged(event);
		}
	}

	/**
	 * Observes non-modifying events that will end a run, such as clicking into
	 * the editor, moving the caret, and the editor losing focus. These events
	 * can never start a run, therefore this listener is only registered if
	 * there is an ongoing run.
	 */
	private class SelectionListener implements MouseListener, KeyListener, FocusListener {

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusGained(FocusEvent e) {
			handleSelectionChanged();
		}

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusLost(FocusEvent e) {
		}

		/*
		 * @see MouseListener#mouseDoubleClick
		 */
		public void mouseDoubleClick(MouseEvent e) {
		}

		/*
		 * If the right mouse button is pressed, the current editing command is closed
		 * @see MouseListener#mouseDown
		 */
		public void mouseDown(MouseEvent e) {
			if (e.button == 1)
				handleSelectionChanged();
		}

		/*
		 * @see MouseListener#mouseUp
		 */
		public void mouseUp(MouseEvent e) {
		}

		/*
		 * @see KeyListener#keyPressed
		 */
		public void keyReleased(KeyEvent e) {
		}

		/*
		 * On cursor keys, the current editing command is closed
		 * @see KeyListener#keyPressed
		 */
		public void keyPressed(KeyEvent e) {
			switch (e.keyCode) {
				case SWT.ARROW_UP:
				case SWT.ARROW_DOWN:
				case SWT.ARROW_LEFT:
				case SWT.ARROW_RIGHT:
				case SWT.END:
				case SWT.HOME:
				case SWT.PAGE_DOWN:
				case SWT.PAGE_UP:
					handleSelectionChanged();
					break;
			}
		}
	}

	/** The listeners. */
	private final Set fListeners= new HashSet();
	/**
	 * The viewer we work upon. Set to <code>null</code> in
	 * <code>uninstall</code>.
	 */
	private ITextViewer fViewer;
	/** The text event listener. */
	private final TextListener fTextListener= new TextListener();
	/**
	 * The selection listener. Set to <code>null</code> when no run is active.
	 */
	private SelectionListener fSelectionListener;

	/* state variables */

	/** The most recently observed change. Never <code>null</code>. */
	private Change fLastChange;
	/** The current run, or <code>null</code> if there is none. */
	private TypingRun fRun;

	/**
	 * Installs the receiver with a text viewer.
	 *
	 * @param viewer the viewer to install on
	 */
	public void install(ITextViewer viewer) {
		Assert.isLegal(viewer != null);
		fViewer= viewer;
		connect();
	}

	/**
	 * Initializes the state variables and registers any permanent listeners.
	 */
	private void connect() {
		if (fViewer != null) {
			fLastChange= new Change(TypingRun.UNKNOWN, -1);
			fRun= null;
			fSelectionListener= null;
			fViewer.addTextListener(fTextListener);
		}
	}

	/**
	 * Uninstalls the receiver and removes all listeners. <code>install()</code>
	 * must be called for events to be generated.
	 */
	public void uninstall() {
		if (fViewer != null) {
			fListeners.clear();
			disconnect();
			fViewer= null;
		}
	}

	/**
	 * Disconnects any registered listeners.
	 */
	private void disconnect() {
		fViewer.removeTextListener(fTextListener);
		ensureSelectionListenerRemoved();
	}

	/**
	 * Adds a listener for <code>TypingRun</code> events. Repeatedly adding
	 * the same listener instance has no effect. Listeners may be added even
	 * if the receiver is neither connected nor installed.
	 *
	 * @param listener the listener add
	 */
	public void addTypingRunListener(ITypingRunListener listener) {
		Assert.isLegal(listener != null);
		fListeners.add(listener);
		if (fListeners.size() == 1)
			connect();
	}

	/**
	 * Removes the listener from this manager. If <code>listener</code> is not
	 * registered with the receiver, nothing happens.
	 *
	 * @param listener the listener to remove, or <code>null</code>
	 */
	public void removeTypingRunListener(ITypingRunListener listener) {
		fListeners.remove(listener);
		if (fListeners.size() == 0)
			disconnect();
	}

	/**
	 * Handles an incoming text event.
	 *
	 * @param event the text event that describes the text modification
	 */
	void handleTextChanged(TextEvent event) {
		Change type= computeChange(event);
		handleChange(type);
	}

	/**
	 * Computes the change abstraction given a text event.
	 *
	 * @param event the text event to analyze
	 * @return a change object describing the event
	 */
	private Change computeChange(TextEvent event) {
		DocumentEvent e= event.getDocumentEvent();
		if (e == null)
			return new Change(TypingRun.NO_CHANGE, -1);

		int start= e.getOffset();
		int end= e.getOffset() + e.getLength();
		String newText= e.getText();
		if (newText == null)
			newText= new String();

		if (start == end) {
			// no replace / delete / overwrite
			if (newText.length() == 1)
				return new Change(TypingRun.INSERT, end + 1);
		} else if (start == end - 1) {
			if (newText.length() == 1)
				return new Change(TypingRun.OVERTYPE, end);
			if (newText.length() == 0)
				return new Change(TypingRun.DELETE, start);
		}

		return new Change(TypingRun.UNKNOWN, -1);
	}

	/**
	 * Handles an incoming selection event.
	 */
	void handleSelectionChanged() {
		handleChange(new Change(TypingRun.SELECTION, -1));
	}

	/**
	 * State machine. Changes state given the current state and the incoming
	 * change.
	 *
	 * @param change the incoming change
	 */
	private void handleChange(Change change) {
		if (change.getType() == TypingRun.NO_CHANGE)
			return;

		if (DEBUG)
			System.err.println("Last change: " + fLastChange); //$NON-NLS-1$

		if (!change.canFollow(fLastChange))
			endIfStarted(change);
		fLastChange= change;
		if (change.isModification())
			startOrContinue();

		if (DEBUG)
			System.err.println("New change: " + change); //$NON-NLS-1$
	}

	/**
	 * Starts a new run if there is none and informs all listeners. If there
	 * already is a run, nothing happens.
	 */
	private void startOrContinue() {
		if (!hasRun()) {
			if (DEBUG)
				System.err.println("+Start run"); //$NON-NLS-1$
			fRun= new TypingRun(fLastChange.getType());
			ensureSelectionListenerAdded();
			fireRunBegun(fRun);
		}
	}

	/**
	 * Returns <code>true</code> if there is an active run, <code>false</code>
	 * otherwise.
	 *
	 * @return <code>true</code> if there is an active run, <code>false</code>
	 *         otherwise
	 */
	private boolean hasRun() {
		return fRun != null;
	}

	/**
	 * Ends any active run and informs all listeners. If there is none, nothing
	 * happens.
	 *
	 * @param change the change that triggered ending the active run
	 */
	private void endIfStarted(Change change) {
		if (hasRun()) {
			ensureSelectionListenerRemoved();
			if (DEBUG)
				System.err.println("-End run"); //$NON-NLS-1$
			fireRunEnded(fRun, change.getType());
			fRun= null;
		}
	}

	/**
	 * Adds the selection listener to the text widget underlying the viewer, if
	 * not already done.
	 */
	private void ensureSelectionListenerAdded() {
		if (fSelectionListener == null) {
			fSelectionListener= new SelectionListener();
			StyledText textWidget= fViewer.getTextWidget();
			textWidget.addFocusListener(fSelectionListener);
			textWidget.addKeyListener(fSelectionListener);
			textWidget.addMouseListener(fSelectionListener);
		}
	}

	/**
	 * If there is a selection listener, it is removed from the text widget
	 * underlying the viewer.
	 */
	private void ensureSelectionListenerRemoved() {
		if (fSelectionListener != null) {
			StyledText textWidget= fViewer.getTextWidget();
			textWidget.removeFocusListener(fSelectionListener);
			textWidget.removeKeyListener(fSelectionListener);
			textWidget.removeMouseListener(fSelectionListener);
			fSelectionListener= null;
		}
	}

	/**
	 * Informs all listeners about a newly started <code>TypingRun</code>.
	 *
	 * @param run the new run
	 */
	private void fireRunBegun(TypingRun run) {
		List listeners= new ArrayList(fListeners);
		for (Iterator it= listeners.iterator(); it.hasNext();) {
			ITypingRunListener listener= (ITypingRunListener) it.next();
			listener.typingRunStarted(fRun);
		}
	}

	/**
	 * Informs all listeners about an ended <code>TypingRun</code>.
	 *
	 * @param run the previously active run
	 * @param reason the type of change that caused the run to be ended
	 */
	private void fireRunEnded(TypingRun run, ChangeType reason) {
		List listeners= new ArrayList(fListeners);
		for (Iterator it= listeners.iterator(); it.hasNext();) {
			ITypingRunListener listener= (ITypingRunListener) it.next();
			listener.typingRunEnded(fRun, reason);
		}
	}
}
