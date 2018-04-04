// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.grid.editors;

import java.awt.Color;

import javax.swing.JColorChooser;

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.ui.ColorButtonSupport;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class RGBCellEditor extends DialogCellEditor {

	private RGB _defaultRGB;

	public RGBCellEditor(Composite parent, RGB defaultRGB) {
		super(parent);
		_defaultRGB = defaultRGB;
	}

	public RGB getDefaultRGB() {
		return _defaultRGB;
	}

	public void setDefaultRGB(RGB defaultRGB) {
		_defaultRGB = defaultRGB;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {

		RGB rgb = null;
		Object value = getValue();
		if (value != null) {
			rgb = (RGB) value;
		}

		// TODO: we should find the way this work on Mac!
		if (!PhaserEditorUI.isMacPlatform()) {
			if (PhaserEditorUI.get_pref_Dialog_Color_Java()) {

				Color color = null;

				if (rgb != null) {
					color = new Color(rgb.red, rgb.green, rgb.blue);
				}

				color = JColorChooser.showDialog(null, "Color Dialog", color);
				if (color != null) {
					rgb = new RGB(color.getRed(), color.getGreen(), color.getBlue());
				}

				return rgb;
			}
		}

		ColorDialog dlg = new ColorDialog(cellEditorWindow.getShell());

		if (rgb != null) {
			dlg.setRGB(rgb);
		}
		value = dlg.open();
		return dlg.getRGB();

	}

	@Override
	protected void updateContents(Object value) {
		super.updateContents(value);

		if (value != null) {
			RGB rgb = (RGB) value;
			getDefaultLabel().setText("  " + getRGBString(rgb));
		}
	}

	private String getRGBString(RGB rgb) {
		if (_defaultRGB != null && _defaultRGB.equals(rgb)) {
			return "(default)";
		}

		return ColorButtonSupport.getHexString(rgb);
	}

}
