// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.ui;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author arian
 *
 */
public interface IBrowser {

	public Control getControl();

	public void setUrl(String url);

	public static IBrowser create(Composite parent, int style) {
		if (PhaserEditorUI.isUsingChromium()) {
			return createChromiumBrowser(parent, style);
		}

		return createDefaultBrowser(parent, style);
	}

	private static IBrowser createDefaultBrowser(Composite parent, int style) {
		var browser = new Browser(parent, style);
		return new IBrowser() {
			@Override
			public Control getControl() {
				return browser;
			}

			@Override
			public void setUrl(String url) {
				browser.setUrl(url);
			}
		};
	}

	private static IBrowser createChromiumBrowser(Composite parent, int style) {
		var browser = new org.eclipse.swt.chromium.Browser(parent, style);
		return new IBrowser() {
			@Override
			public Control getControl() {
				return browser;
			}

			@Override
			public void setUrl(String url) {
				browser.setUrl(url);
			}
		};
	}
}
