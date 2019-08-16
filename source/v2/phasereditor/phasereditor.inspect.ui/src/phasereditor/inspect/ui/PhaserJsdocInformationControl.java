// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.inspect.ui;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;
import phasereditor.inspect.core.jsdoc.JsdocRenderer;
import phasereditor.ui.IBrowser;
import phasereditor.ui.info.BaseInformationControl;

/**
 * @author arian
 *
 */
public class PhaserJsdocInformationControl extends BaseInformationControl {

	public PhaserJsdocInformationControl(Shell parentShell) {
		super(parentShell, true);
	}

	@Override
	protected Control createContent2(Composite parentComp) {
		return IBrowser.create(parentComp, SWT.NONE).getControl();
	}

	@Override
	protected void updateContent(Control control, Object model) {
		IJsdocProvider provider = Adapters.adapt(model, IJsdocProvider.class);
		var browser = IBrowser.get(control);
		browser.setText(JsdocRenderer.wrapDocBody(provider.getJsdoc()));
	}

	@Override
	public Point computeSizeHint() {
		return new Point(255 * 2, 200 * 2);
	}

}
