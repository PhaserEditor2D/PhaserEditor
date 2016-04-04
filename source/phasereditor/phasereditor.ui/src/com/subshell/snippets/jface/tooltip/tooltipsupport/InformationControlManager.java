// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.subshell.snippets.jface.tooltip.tooltipsupport;

import java.util.List;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Information control manager. Refactored from
 * {@link org.eclipse.jface.text.information.InformationPresenter} for general
 * usage instead of {@link ITextViewer}.
 */
class InformationControlManager extends AbstractInformationControlManager {

	/**
	 * Internal information control closer. Listens to several events issued by
	 * its subject control and closes the information control when necessary.
	 */
	@SuppressWarnings("synthetic-access")
	class InformationControlCloser implements IInformationControlCloser, ControlListener, MouseListener, FocusListener,
			KeyListener, MouseMoveListener {

		/** The subject control. */
		private Control subjectControl;
		/** The information control. */
		private IInformationControl informationControlToClose;
		/** Indicates whether this closer is active. */
		private boolean isActive = false;

		@Override
		public void setSubjectControl(Control control) {
			subjectControl = control;
		}

		@Override
		public void setInformationControl(IInformationControl control) {
			informationControlToClose = control;
		}

		@Override
		public void start(Rectangle informationArea) {
			if (isActive) {
				return;
			}
			isActive = true;

			if (subjectControl != null && !subjectControl.isDisposed()) {
				subjectControl.addControlListener(this);
				subjectControl.addMouseListener(this);
				subjectControl.addFocusListener(this);
				subjectControl.addKeyListener(this);
				subjectControl.addMouseMoveListener(this);
			}

			if (informationControlToClose != null) {
				informationControlToClose.addFocusListener(this);
			}
		}

		@Override
		public void stop() {
			if (!isActive) {
				return;
			}
			isActive = false;

			if (informationControlToClose != null) {
				informationControlToClose.removeFocusListener(this);
			}

			if (subjectControl != null && !subjectControl.isDisposed()) {
				subjectControl.removeControlListener(this);
				subjectControl.removeMouseListener(this);
				subjectControl.removeFocusListener(this);
				subjectControl.removeKeyListener(this);
				subjectControl.removeMouseMoveListener(this);
			}
		}

		@Override
		public void controlResized(ControlEvent e) {
			hideInformationControl();
		}

		@Override
		public void controlMoved(ControlEvent e) {
			hideInformationControl();
		}

		@Override
		public void mouseDown(MouseEvent e) {
			hideInformationControl();
		}

		@Override
		public void mouseUp(MouseEvent e) {
			hideInformationControl();
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			hideInformationControl();
		}

		@Override
		public void mouseMove(MouseEvent e) {
			hideInformationControl();
		}

		@Override
		public void focusGained(FocusEvent e) {
			// nothing to do
		}

		@Override
		public void focusLost(FocusEvent e) {
			Display d = subjectControl.getDisplay();
			d.asyncExec(new Runnable() {
				// Without the asyncExec, mouse clicks to the workbench window
				// are swallowed.
				@Override
				public void run() {
					if (informationControlToClose == null || !informationControlToClose.isFocusControl()) {
						hideInformationControl();
					}
				}
			});
		}

		@Override
		public void keyPressed(KeyEvent e) {
			hideInformationControl();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// nothing to do
		}
	}

	private static final IInformationControlCreator DEFAULT_INFORMATION_CONTROL_CREATOR = new ICustomInformationControlCreator() {

		@Override
		public IInformationControl createInformationControl(Shell parent) {
			return new AbstractInformationControl(parent, false) {

				@Override
				public boolean hasContents() {
					return false;
				}

				@Override
				protected void createContent(Composite parentComp) {
					// nothing
				}
			};
		}

		@Override
		public boolean isSupported(Object info) {
			return false;
		}
	};

	private final IInformationProvider informationProvider;
	private final List<ICustomInformationControlCreator> customControlCreators;

	/**
	 * Creates a new information control manager that uses the given information
	 * provider and control creators. The manager is not installed on any
	 * control yet. By default, an information control closer is set that closes
	 * the information control in the event of key strokes, resizing, moves,
	 * focus changes, mouse clicks, and disposal - all of those applied to the
	 * information control's parent control. Optionally, the setup ensures that
	 * the information control when made visible will request the focus.
	 *
	 * @param informationProvider
	 *            the information provider to be used
	 * @param customControlCreators
	 *            the control creators to be used
	 * @param takeFocusWhenVisible
	 *            set to <code>true</code> if the information control should
	 *            take focus when made visible
	 */
	InformationControlManager(IInformationProvider informationProvider,
			List<ICustomInformationControlCreator> customControlCreators, boolean takeFocusWhenVisible) {
		super(DEFAULT_INFORMATION_CONTROL_CREATOR);
		this.informationProvider = informationProvider;
		this.customControlCreators = customControlCreators;

		setCloser(new InformationControlCloser());
		takesFocusWhenVisible(takeFocusWhenVisible);
	}

	@Override
	protected void computeInformation() {
		Display display = getSubjectControl().getDisplay();
		Point mouseLocation = display.getCursorLocation();
		mouseLocation = getSubjectControl().toControl(mouseLocation);

		// Compute information input
		Object info = informationProvider.getInformation(mouseLocation);

		// Find an information control creator for the computed information
		// input
		IInformationControlCreator customControlCreator = null;
		for (ICustomInformationControlCreator controlCreator : customControlCreators) {
			if (controlCreator.isSupported(info)) {
				customControlCreator = controlCreator;
				break;
			}
		}
		setCustomInformationControlCreator(customControlCreator);

		// Convert to String for default TextLabelInformationControl
		// (Fallback, if no custom control creator has been found)
		if (info != null && customControlCreator == null) {
			info = info.toString();
		}

		// Trigger the presentation of the computed information
		Rectangle area = informationProvider.getArea(mouseLocation);
		setInformation(info, area);
	}

	@Override
	protected Point computeLocation(Rectangle subjectArea, Point controlSize, Anchor anchor) {
		Point location = super.computeLocation(subjectArea, controlSize, anchor);
		location.x += 20;
		return location;
	}
}