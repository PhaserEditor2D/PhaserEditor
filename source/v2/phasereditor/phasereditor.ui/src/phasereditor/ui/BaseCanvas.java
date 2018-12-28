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
package phasereditor.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * @author arian
 *
 */
public class BaseCanvas extends Canvas {

	public BaseCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);

		setData("org.eclipse.e4.ui.css.CssClassName", "Canvas");

		parent.setBackgroundMode(SWT.INHERIT_FORCE);

		PhaserEditorUI.redrawCanvasWhenPreferencesChange(this);
	}

	public static void prepareGC(GC gc) {
		if (!PhaserEditorUI.get_pref_Preview_Anitialias()) {
			gc.setAntialias(SWT.OFF);
			gc.setInterpolation(SWT.OFF);
		}
	}

	public static void prepareGC(Graphics2D g2) {
		if (PhaserEditorUI.get_pref_Preview_Anitialias()) {
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
	}

}
