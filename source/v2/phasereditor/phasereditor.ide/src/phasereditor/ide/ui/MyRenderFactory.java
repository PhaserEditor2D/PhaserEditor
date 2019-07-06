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
package phasereditor.ide.ui;

import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.renderers.swt.TrimmedPartLayout;
import org.eclipse.e4.ui.workbench.renderers.swt.WBWRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.WorkbenchRendererFactory;
import org.eclipse.swt.widgets.Shell;

import phasereditor.ide.ui.toolbar.HugeToolbar;

/**
 * @author arian
 *
 */
public class MyRenderFactory extends WorkbenchRendererFactory {
	private MyWBWRenderer _winRenderer;

	@Override
	public AbstractPartRenderer getRenderer(MUIElement uiElement, Object parent) {
		if (uiElement instanceof MWindow) {
			if (_winRenderer == null) {
				_winRenderer = new MyWBWRenderer();
				initRenderer(_winRenderer);
			}
			return _winRenderer;
		}
		return super.getRenderer(uiElement, parent);
	}

}

class MyWBWRenderer extends WBWRenderer {

	public static int GUTTER_TOP;

	@Override
	public Object createWidget(MUIElement element, Object parent) {
		var widget = super.createWidget(element, parent);

		var shell = (Shell) widget;

		var layout = (TrimmedPartLayout) shell.getLayout();

		var toolbar = new HugeToolbar(shell);

		layout.gutterTop = toolbar.getBounds().height;
		GUTTER_TOP = layout.gutterTop;

		return widget;
	}

}
