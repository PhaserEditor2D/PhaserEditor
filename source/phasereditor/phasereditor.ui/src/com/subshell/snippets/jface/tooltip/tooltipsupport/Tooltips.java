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

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.Control;

public class Tooltips {

	private Tooltips() {
		// nothing to do
	}

	/**
	 * Installs tooltip support on a control.
	 * 
	 * @param control
	 *            the control to install tooltip support on
	 * @param provider
	 *            information provider to get information about elements
	 * @param controlCreators
	 *            information control creators to create the tooltips
	 * @param takeFocusWhenVisible
	 *            set to <code>true</code> if the information control should
	 *            take focus when made visible
	 */
	public static void install(Control control, IInformationProvider provider,
			List<ICustomInformationControlCreator> controlCreators, boolean takeFocusWhenVisible) {
		final InformationControlManager informationControlManager = new InformationControlManager(provider,
				controlCreators, takeFocusWhenVisible);
		informationControlManager.setSizeConstraints(100, 12, false, true);
		informationControlManager.install(control);

		// MouseListener to show the information when the user hovers a table
		// item
		control.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent event) {
				informationControlManager.showInformation();
			}
		});

		// DisposeListener to uninstall the information control manager
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				informationControlManager.dispose();
			}
		});
	}
}
