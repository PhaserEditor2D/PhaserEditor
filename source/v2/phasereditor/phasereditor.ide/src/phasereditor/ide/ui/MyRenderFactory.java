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
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.renderers.swt.TrimBarLayout;
import org.eclipse.e4.ui.workbench.renderers.swt.TrimBarRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.TrimmedPartLayout;
import org.eclipse.e4.ui.workbench.renderers.swt.WBWRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.WorkbenchRendererFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * @author arian
 *
 */
public class MyRenderFactory extends WorkbenchRendererFactory {
	private MyWBWRenderer _winRenderer;
	private MyTrimBarRenderer _trimBarRenderer;

	@Override
	public AbstractPartRenderer getRenderer(MUIElement uiElement, Object parent) {
		if (uiElement instanceof MWindow) {
			if (_winRenderer == null) {
				_winRenderer = new MyWBWRenderer();
				initRenderer(_winRenderer);
			}
			return _winRenderer;
		} else if (uiElement instanceof MTrimBar) {
			if (_trimBarRenderer == null) {
				_trimBarRenderer = new MyTrimBarRenderer();
				initRenderer(_trimBarRenderer);
			}
			return _trimBarRenderer;
		}

		return super.getRenderer(uiElement, parent);
	}

}

class MyTrimBarRenderer extends TrimBarRenderer {
	@Override
	public Object createWidget(MUIElement element, Object parent) {
		var widget = super.createWidget(element, parent);
		if (widget != null) {
			Composite parentComp = (Composite) parent;
			final MTrimBar trimModel = (MTrimBar) element;
			if (parentComp.getLayout() instanceof TrimmedPartLayout) {
				TrimmedPartLayout tpl = (TrimmedPartLayout) parentComp.getLayout();

				if (trimModel.getSide().getValue() == SideValue.BOTTOM_VALUE) {
					var comp = (Composite) widget;
					var layout = (TrimBarLayout) comp.getLayout();
					layout.marginTop = 50;
					comp.addPaintListener(e -> {
						e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_DARK_GRAY));
						e.gc.fillOval(e.width / 2 - 15, 5, 30, 30);
					});
				}
			}
		}

		return widget;
	}
}

class MyWBWRenderer extends WBWRenderer {
	@Override
	public Object createWidget(MUIElement element, Object parent) {
		var widget = super.createWidget(element, parent);
		var shell = (Shell) widget;
		//
		// out.println(shell.getLayout());
		// var layout = (TrimmedPartLayout) shell.getLayout();
		// layout.clientArea.dispose();
		//
		// shell.setLayout(new GridLayout(1, false));
		// var topComp = Pepe.createButtons(shell);
		// topComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		//
		// var centerComp = new Composite(shell, 0);
		// centerComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		// layout = new TrimmedPartLayout(centerComp);
		// centerComp.setLayout(layout);

		return widget;
	}

}

class Pepe {
	public static Composite createButtons(Composite compToHack) {
		compToHack.setLayout(new FillLayout());
		var comp2 = new Composite(compToHack, 0);
		comp2.setLayout(new RowLayout());
		for (int i = 0; i < 10; i++) {
			var btn = new Button(comp2, SWT.PUSH);
			btn.setText("Button " + i);
		}
		return comp2;
	}
}