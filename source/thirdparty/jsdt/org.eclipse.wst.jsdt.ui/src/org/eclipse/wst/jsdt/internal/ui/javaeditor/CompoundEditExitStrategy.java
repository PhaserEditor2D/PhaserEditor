/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Exit strategy for commands that want to fold repeated execution into one compound edit. See
 * {@link org.eclipse.jface.text.IRewriteTarget#endCompoundChange() IRewriteTarget.endCompoundChange}.
 * As long as a strategy is installed on an {@link ITextViewer}, it will detect the end of a
 * compound operation when any of the following conditions becomes true:
 * <ul>
 * <li>the viewer's text widget loses the keyboard focus</li>
 * <li>the mouse is clicked or double clicked inside the viewer's widget</li>
 * <li>a command other than the ones specified is executed</li>
 * <li>the viewer receives any key events that are not modifier combinations</li>
 * </ul>
 * <p>
 * If the end of a compound edit is detected, any registered {@link ICompoundEditListener}s are
 * notified and the strategy is disarmed (spring-loaded).
 * </p>
 * 
 * 
 */
public final class CompoundEditExitStrategy {
	/**
	 * Listens for events that may trigger the end of a compound edit.
	 */
	private final class EventListener implements MouseListener, FocusListener, VerifyKeyListener, IExecutionListener {

		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent e) {
			// mouse actions end the compound change
			fireEndCompoundEdit();
		}

		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDown(MouseEvent e) {
			// mouse actions end the compound change
			fireEndCompoundEdit();
		}

		public void mouseUp(MouseEvent e) {}

		public void focusGained(FocusEvent e) {}

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusLost(FocusEvent e) {
			// losing focus ends the change
			fireEndCompoundEdit();
		}

		public void notHandled(String commandId, NotHandledException exception) {}

		public void postExecuteFailure(String commandId, ExecutionException exception) {}

		public void postExecuteSuccess(String commandId, Object returnValue) {}

		/*
		 * @see org.eclipse.core.commands.IExecutionListener#preExecute(java.lang.String, org.eclipse.core.commands.ExecutionEvent)
		 */
		public void preExecute(String commandId, ExecutionEvent event) {
			// any command other than the known ones end the compound change
			for (int i= 0; i < fCommandIds.length; i++) {
				if (commandId.equals(fCommandIds[i]))
					return;
			}
			fireEndCompoundEdit();
		}

		/*
		 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {
			// any key press that is not a modifier combo ends the compound change
			final int maskWithoutShift= SWT.MODIFIER_MASK & ~SWT.SHIFT;
			if ((event.keyCode & SWT.MODIFIER_MASK) == 0 && (event.stateMask & maskWithoutShift) == 0)
				fireEndCompoundEdit();
		}
		
	}

	private final String[] fCommandIds;
	private final EventListener fEventListener= new EventListener();
	private final ListenerList fListenerList= new ListenerList(ListenerList.IDENTITY);

	private ITextViewer fViewer;
	private StyledText fWidgetEventSource;

	/**
	 * Creates a new strategy, equivalent to calling
	 * {@linkplain #CompoundEditExitStrategy(String[]) CompoundEditExitStrategy(new String[] &#x7b; commandId &#x7d;)}.
	 * 
	 * @param commandId the command id of the repeatable command
	 */
	public CompoundEditExitStrategy(String commandId) {
		if (commandId == null)
			throw new NullPointerException("commandId"); //$NON-NLS-1$
		fCommandIds= new String[] {commandId};
	}
	
	/**
	 * Creates a new strategy, ending upon execution of any command other than the ones
	 * specified.
	 * 
	 * @param commandIds the ids of the repeatable commands
	 */
	public CompoundEditExitStrategy(String[] commandIds) {
		for (int i= 0; i < commandIds.length; i++) {
			if (commandIds[i] == null)
				throw new NullPointerException("commandIds[" + i + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fCommandIds= new String[commandIds.length];
		System.arraycopy(commandIds, 0, fCommandIds, 0, commandIds.length);
	}
	
	/**
	 * Installs the receiver on <code>viewer</code> and arms it. After this call returns, any
	 * registered listeners will be notified if a compound edit ends.
	 * 
	 * @param viewer the viewer to install on
	 */
	public void arm(ITextViewer viewer) {
		disarm();
		if (viewer == null)
			throw new NullPointerException("editor"); //$NON-NLS-1$
		fViewer= viewer;
		addListeners(fViewer);
	}
	
	/**
	 * Disarms the receiver. After this call returns, any registered listeners will be not be
	 * notified any more until <code>install</code> is called again. Note that the listeners are
	 * not removed.
	 * <p>
	 * Note that the receiver is automatically disarmed when the end of a compound edit has
	 * been detected and before the listeners are notified.
	 * </p>
	 */
	public void disarm() {
		if (isInstalled()) {
			removeListeners(fViewer);
			fViewer= null;
		}
	}

	private void addListeners(ITextViewer viewer) {
		fWidgetEventSource= viewer.getTextWidget();
		if (fWidgetEventSource != null) {
			fWidgetEventSource.addVerifyKeyListener(fEventListener);
			fWidgetEventSource.addMouseListener(fEventListener);
			fWidgetEventSource.addFocusListener(fEventListener);
		}

		ICommandService commandService= (ICommandService)PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		if (commandService != null)
			commandService.addExecutionListener(fEventListener);
	}
	
	private void removeListeners(ITextViewer editor) {
		ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		if (commandService != null)
			commandService.removeExecutionListener(fEventListener);
		
		if (fWidgetEventSource != null) {
			fWidgetEventSource.removeFocusListener(fEventListener);
			fWidgetEventSource.removeMouseListener(fEventListener);
			fWidgetEventSource.removeVerifyKeyListener(fEventListener);
			fWidgetEventSource= null;
		}
	}

	private boolean isInstalled() {
		return fViewer != null;
	}
	
	private void fireEndCompoundEdit() {
		disarm();
		Object[] listeners= fListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			ICompoundEditListener listener= (ICompoundEditListener) listeners[i];
			try {
				listener.endCompoundEdit();
			} catch (Exception e) {
				JavaScriptPlugin.log(e);
			}
		}
	}
	
	/**
	 * Adds a compound edit listener. Multiple registration is possible. Note that the receiver is
	 * automatically disarmed before the listeners are notified.
	 * 
	 * @param listener the new listener
	 */
	public void addCompoundListener(ICompoundEditListener listener) {
		fListenerList.add(listener);
	}
	
	/**
	 * Removes a compound edit listener. If <code>listener</code> is registered multiple times, an
	 * arbitrary instance is removed. If <code>listener</code> is not currently registered,
	 * nothing happens.
	 * 
	 * @param listener the listener to be removed.
	 */
	public void removeCompoundListener(ICompoundEditListener listener) {
		fListenerList.remove(listener);
	}
	
}
