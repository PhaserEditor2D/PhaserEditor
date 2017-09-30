// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.core;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.json.JSONObject;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * @author arian
 *
 */
public class TextStyle {
	private String _font;
	private int _fontSize;
	// like italic
	private FontPosture _fontStyle;
	// fontVariant

	public TextStyle() {
		_font = "Arial";
		_fontSize = 20;
		_fontStyle = FontPosture.REGULAR;
	}

	public String getFont() {
		return _font;
	}

	public void setFont(String font) {
		_font = font;
	}

	public int getFontSize() {
		return _fontSize;
	}

	public void setFontSize(int fontSize) {
		_fontSize = fontSize;
	}

	public FontPosture getFontStyle() {
		return _fontStyle;
	}

	public void setFontStyle(FontPosture fontStyle) {
		_fontStyle = fontStyle;
	}

	public Font buildFont() {
		Font font = Font.font(_font, FontWeight.NORMAL, _fontStyle, _fontSize);
		return font;
	}

	public JSONObject writeJSON(JSONObject data) {
		data.put("font", _font);
		data.put("fontSize", _fontSize);
		data.put("fontStyle", _fontStyle.name());
		return data;
	}

	public void readJSON(JSONObject data) {
		_font = data.getString("font");
		_fontSize = data.getInt("fontSize");
		_fontStyle = FontPosture.valueOf(data.optString("fontStyle", FontPosture.REGULAR.name()));
	}

	public static TextStyle createFromFontData(FontData fd) {
		TextStyle style = new TextStyle();
		style.setFont(fd.getName());
		style.setFontSize(fd.getHeight());
		switch (fd.getStyle()) {
		case SWT.NORMAL:
			style.setFontStyle(FontPosture.REGULAR);
			break;
		case SWT.ITALIC:
			style.setFontStyle(FontPosture.ITALIC);
			break;
		default:
			break;
		}
		return style;
	}

	@Override
	public String toString() {
		JSONObject data = new JSONObject();
		writeJSON(data);
		return data.toString();
	}

	public JSONObject toPhaserStyleObject() {
		JSONObject data = new JSONObject();
		data.put("font", _fontSize + "px" + " " + _font);
		return data;
	}

	public FontData buildFontData() {
		int fdStyle = SWT.NORMAL;

		switch (_fontStyle) {
		case ITALIC:
			fdStyle = fdStyle | SWT.ITALIC;
			break;

		default:
			break;
		}

		FontData fd = new FontData(_font, _fontSize, fdStyle);
		return fd;
	}
}
