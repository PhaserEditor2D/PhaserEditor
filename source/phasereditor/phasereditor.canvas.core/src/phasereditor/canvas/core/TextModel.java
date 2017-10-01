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

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * @author arian
 *
 */
public class TextModel extends BaseSpriteModel {
	/**
	 * 
	 */
	private static final FontPosture DEF_FONT_STYLE = FontPosture.REGULAR;
	public static final int DEF_FONT_SIZE = 20;
	public static final String DEF_FONT = "Arial";
	public static final FontWeight DEF_FONT_WEIGHT = FontWeight.BOLD;
	public static final String TYPE_NAME = "text";
	public static final String PROPSET_TEXT = "text";
	public static final String PROPSET_TEXT_STYLE = "textStyle";

	private String _text;
	private String _font;
	private int _fontSize;
	// like italic
	private FontPosture _fontStyle;
	// bold
	private FontWeight _fontWeight;
	// #000
	private String _backgroundColor;
	// #000
	private String _fill;

	public TextModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}

	public TextModel(GroupModel parent) {
		super(parent, TYPE_NAME);

		_text = "";

		_font = DEF_FONT;
		_fontSize = 20;
		_fontWeight = DEF_FONT_WEIGHT;
		_fontStyle = DEF_FONT_STYLE;
		_backgroundColor = null;
		_fill = "#000000";
	}

	public String getText() {
		return _text;
	}

	public void setText(String text) {
		_text = text;
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

	public FontWeight getFontWeight() {
		return _fontWeight;
	}

	public void setFontWeight(FontWeight fontWeight) {
		_fontWeight = fontWeight;
	}

	public String getBackgroundColor() {
		return _backgroundColor;
	}

	public void setBackgroundColor(String backgroundColor) {
		_backgroundColor = backgroundColor;
	}

	public String getFill() {
		return _fill;
	}

	public void setFill(String fill) {
		_fill = fill;
	}

	@Override
	public void build() {
		// nothing
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo, boolean saving) {
		super.writeInfo(jsonInfo, saving);

		boolean prefabInstance = isPrefabInstance();

		if (isOverriding(PROPSET_TEXT)) {
			if (prefabInstance) {
				jsonInfo.put("text", _text);
			} else {
				jsonInfo.put("text", _text, "");
			}
		}

		if (isOverriding(PROPSET_TEXT_STYLE)) {
			if (prefabInstance) {
				jsonInfo.put("font", _font);
				jsonInfo.put("fontSize", _fontSize);
				jsonInfo.put("fontWeight", _fontWeight.name());
				jsonInfo.put("fontStyle", _fontStyle.name());
			} else {
				jsonInfo.put("font", _font, DEF_FONT);
				jsonInfo.put("fontSize", _fontSize, DEF_FONT_SIZE);
				jsonInfo.put("fontWeight", _fontWeight.name(), DEF_FONT_WEIGHT.name());
				jsonInfo.put("fontStyle", _fontStyle.name(), DEF_FONT_STYLE.name());
			}
		}

	}

	@Override
	public void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_text = jsonInfo.optString("text", "");

		_font = jsonInfo.optString("font", DEF_FONT);
		_fontSize = jsonInfo.optInt("fontSize", DEF_FONT_SIZE);
		_fontWeight = FontWeight.valueOf(jsonInfo.optString("fontWeight", DEF_FONT_WEIGHT.name()));
		_fontStyle = FontPosture.valueOf(jsonInfo.optString("fontStyle", DEF_FONT_STYLE.name()));

	}

	@Override
	protected List<AnimationModel> readAnimations(JSONArray array) {
		// not supported in Text objects
		return Collections.emptyList();
	}

	public JSONObject getPhaserStyleObject() {
		JSONObject data = new JSONObject();

		StringBuilder sb = new StringBuilder();

		if (_fontStyle == FontPosture.ITALIC) {
			sb.append("italic ");
		}
		
		if (_fontWeight == FontWeight.BOLD) {
			sb.append("bold ");
		}
		
		sb.append(_fontSize + "px ");
		sb.append(_font);

		data.put("font", sb.toString());

		return data;
	}

}
