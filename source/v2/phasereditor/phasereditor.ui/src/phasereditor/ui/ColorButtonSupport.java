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
package phasereditor.ui;

import java.util.function.Consumer;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Control;

/**
 * @author arian
 *
 */
public abstract class ColorButtonSupport {
	private static final int DEFAULT_EXTENT = 16;
	private Button _button;
	Image _image;
	private Consumer<RGB> _callback;

	public static ColorButtonSupport createDefault(Button btn, Consumer<RGB> callback) {
		return new ColorButtonSupport(btn, callback) {
			private RGB _color;

			@Override
			public RGB getColor() {
				return _color;
			}

			@Override
			public void setColor(RGB color) {
				_color = color;
			}

		};
	}

	public ColorButtonSupport(Button button, Consumer<RGB> callback) {
		super();
		_button = button;
		_callback = callback;

		_button.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (_image.isDisposed()) {
					return;
				}
				_image.dispose();
			}
		});

		_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ColorDialog dlg = new ColorDialog(e.display.getActiveShell());
				RGB rgb = dlg.open();
				if (rgb != null) {
					setColor(rgb);
					callback.accept(rgb);
				}
				updateProvider();
			}
		});

		updateProvider();
	}

	public void updateProvider() {
		RGB color = getColor();

		if (_image != null && !_image.isDisposed()) {
			_image.dispose();
		}

		ImageData id = createColorImage(_button, color);
		ImageData mask = id.getTransparencyMask();
		_image = new Image(_button.getDisplay(), id, mask);
		_button.setImage(_image);
		_button.setText(color == null ? "NULL" : ColorButtonSupport.getHexString(color));
	}

	private static ImageData createColorImage(Control w, RGB color) {
		GC gc = new GC(w);
		FontMetrics fm = gc.getFontMetrics();
		int size = fm.getAscent();
		gc.dispose();

		int indent = 0;
		int extent = DEFAULT_EXTENT;

		if (size > extent) {
			size = extent;
		}

		int width = indent + size;
		int height = extent;

		int xoffset = indent;
		int yoffset = (height - size) / 2;

		RGB black = new RGB(0, 0, 0);
		PaletteData dataPalette = new PaletteData(
				color == null ? new RGB[] { black, black } : new RGB[] { black, black, color });
		ImageData data = new ImageData(width, height, 4, dataPalette);
		data.transparentPixel = 0;

		int end = size - 1;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (x == 0 || y == 0 || x == end || y == end) {
					data.setPixel(x + xoffset, y + yoffset, 1);
				} else {
					if (color != null) {
						data.setPixel(x + xoffset, y + yoffset, 2);
					}
				}
			}
		}

		return data;
	}

	public static String getHexString(RGB rgb) {
		return "#" + toHexString(rgb.red) + toHexString(rgb.green) + toHexString(rgb.blue);
	}

	public static String getHexString2(RGB rgb) {
		return "0x" + toHexString(rgb.red) + toHexString(rgb.green) + toHexString(rgb.blue);
	}

	public static String toHexString(int n) {
		String s = Integer.toHexString(n);
		return (s.length() == 1 ? "0" : "") + s;
	}

	public static String getRGBString(RGB rgb) {
		return "rgb(" + rgb.red + "," + rgb.green + "," + rgb.blue + ")";
	}

	public void clearColor() {
		setColor(null);
		_callback.accept(null);
	}

	public abstract RGB getColor();

	public abstract void setColor(RGB color);

}
