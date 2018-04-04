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
package com.subshell.snippets.jface.internal.tooltip;

import java.net.URL;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link IInformationControl} that displays HTML information in a
 * {@link org.eclipse.swt.browser.Browser} widget. Expects a {@link URL} to be
 * passed as the input.
 *
 */
class WebBrowserInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

	/** The control's browser widget */
	private Browser browser;

	/** Tells whether the browser has content */
	private boolean browserHasContent;

	/**
	 * <code>true</code> iff the browser has completed loading of the last input
	 * set via {@link #setInformation(String)}.
	 */
	private boolean isCompleted = false;

	WebBrowserInformationControl(Shell parent, boolean resizable) {
		super(parent, resizable);
		create();
	}

	@Override
	protected void createContent(Composite parent) {
		browser = new Browser(parent, SWT.NONE);
		browser.setJavascriptEnabled(true);

		Display display = getShell().getDisplay();
		browser.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		browser.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		browser.addProgressListener(new ProgressAdapter() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void completed(ProgressEvent event) {
				isCompleted = true;
			}
		});

		browser.addOpenWindowListener(new OpenWindowListener() {
			@Override
			public void open(WindowEvent event) {
				event.required = true; // Cancel opening of new windows
			}
		});

		// Replace browser's built-in context menu with none
		browser.setMenu(new Menu(getShell(), SWT.NONE));
	}

	@Override
	public void setInput(Object input) {
		setUrl((URL) input);
	}

	private void setUrl(URL url) {
		isCompleted = false;

		browserHasContent = url != null;
		if (url == null) {
			browser.setText("<html><body>No information available</body></html>");
		} else {
			browser.setUrl(url.toString());
		}
	}

	@Override
	public boolean hasContents() {
		return browserHasContent;
	}

	@Override
	public void setVisible(boolean visible) {
		Shell shell = getShell();
		if (shell.isVisible() == visible) {
			return;
		}

		if (!visible) {
			super.setVisible(false);
			setInformation(null);
			return;
		}

		/*
		 * The Browser widget flickers when made visible while it is not
		 * completely loaded. The fix is to delay the call to setVisible until
		 * either loading is completed (see ProgressListener in constructor), or
		 * a timeout has been reached.
		 */
		final Display display = shell.getDisplay();

		// Make sure the display wakes from sleep after timeout:
		display.timerExec(100, new Runnable() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				isCompleted = true;
			}
		});

		while (!isCompleted) {
			// Drive the event loop to process the events required to load the
			// browser widget's contents:
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		shell = getShell();
		if (shell == null || shell.isDisposed()) {
			return;
		}

		/*
		 * Avoids flickering when replacing hovers, especially on Vista in
		 * ON_CLICK mode. Causes flickering on GTK. Carbon does not care.
		 */
		if ("win32".equals(SWT.getPlatform())) { //$NON-NLS-1$
			shell.moveAbove(null);
		}

		super.setVisible(true);
	}

	@Override
	public void setSize(int width, int height) {
		browser.setRedraw(false); // avoid flickering
		try {
			super.setSize(width, height);
		} finally {
			browser.setRedraw(true);
		}
	}

	@Override
	protected void handleDispose() {
		browser = null;
		super.handleDispose();
	}

}
