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
import javafx.scene.text.TextAlignment;

/**
 * @author arian
 *
 */
public class TextModel extends BaseSpriteModel {
	public static final TextAlignment DEF_STYLE_ALIGN = TextAlignment.LEFT;
	private static final FontPosture DEF_STYLE_FONT_STYLE = FontPosture.REGULAR;
	public static final int DEF_STYLE_FONT_SIZE = 20;
	public static final String DEF_STYLE_FONT = "Arial";
	public static final FontWeight DEF_STYLE_FONT_WEIGHT = FontWeight.BOLD;
	public static final String DEF_STYLE_FILL = "#000000";
	public static final String DEF_STYLE_BACKGROUND_COLOR = null;
	public static final String DEF_STYLE_STROKE = "#000000";

	public static final String TYPE_NAME = "text";
	public static final String PROPSET_TEXT = "text";
	public static final String PROPSET_TEXT_STYLE = "textStyle";
	public static final int DEF_STYLE_STROKE_THICKNESS = 0;

	private String _text;
	private String _styleFont;
	private int _styleFontSize;
	private FontPosture _styleFontStyle;
	private FontWeight _styleFontWeight;
	private String _styleBackgroundColor;
	private String _styleFill;
	private TextAlignment _styleAlign;
	private String _styleStroke;
	private int _styleStrokeThickness;

	public TextModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}

	public TextModel(GroupModel parent) {
		super(parent, TYPE_NAME);

		_text = "";

		_styleFont = DEF_STYLE_FONT;
		_styleFontSize = 20;
		_styleFontWeight = DEF_STYLE_FONT_WEIGHT;
		_styleFontStyle = DEF_STYLE_FONT_STYLE;
		_styleBackgroundColor = null;
		_styleFill = DEF_STYLE_FILL;
		_styleAlign = DEF_STYLE_ALIGN;
		_styleStroke = DEF_STYLE_STROKE;
		_styleStrokeThickness = DEF_STYLE_STROKE_THICKNESS;
	}

	public String getText() {
		return _text;
	}

	public void setText(String text) {
		_text = text;
	}

	public String getStyleFont() {
		return _styleFont;
	}

	public void setStyleFont(String font) {
		_styleFont = font;
	}

	public int getStyleFontSize() {
		return _styleFontSize;
	}

	public void setStyleFontSize(int fontSize) {
		_styleFontSize = fontSize;
	}

	public FontPosture getStyleFontStyle() {
		return _styleFontStyle;
	}

	public void setStyleFontStyle(FontPosture fontStyle) {
		_styleFontStyle = fontStyle;
	}

	public FontWeight getStyleFontWeight() {
		return _styleFontWeight;
	}

	public void setStyleFontWeight(FontWeight fontWeight) {
		_styleFontWeight = fontWeight;
	}

	public String getStyleBackgroundColor() {
		return _styleBackgroundColor;
	}

	public void setStyleBackgroundColor(String backgroundColor) {
		_styleBackgroundColor = backgroundColor;
	}

	public String getStyleFill() {
		return _styleFill;
	}

	public void setStyleFill(String fill) {
		_styleFill = fill;
	}

	public TextAlignment getStyleAlign() {
		return _styleAlign;
	}

	public void setStyleAlign(TextAlignment styleAlign) {
		_styleAlign = styleAlign;
	}

	public String getStyleStroke() {
		return _styleStroke;
	}

	public void setStyleStroke(String styleStroke) {
		_styleStroke = styleStroke;
	}

	public int getStyleStrokeThickness() {
		return _styleStrokeThickness;
	}

	public void setStyleStrokeThickness(int styleStrokeThickness) {
		_styleStrokeThickness = styleStrokeThickness;
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
				jsonInfo.put("style.font", _styleFont);
				jsonInfo.put("style.fontSize", _styleFontSize);
				jsonInfo.put("style.fontWeight", _styleFontWeight.name());
				jsonInfo.put("style.fontStyle", _styleFontStyle.name());
				jsonInfo.put("style.fill", _styleFill);
				jsonInfo.put("style.stroke", _styleStroke);
				jsonInfo.put("style.strokeThickness", _styleStrokeThickness);
				jsonInfo.put("style.backgroundColor", _styleBackgroundColor);
				jsonInfo.put("style.align", _styleAlign.name());
			} else {
				jsonInfo.put("style.font", _styleFont, DEF_STYLE_FONT);
				jsonInfo.put("style.fontSize", _styleFontSize, DEF_STYLE_FONT_SIZE);
				jsonInfo.put("style.fontWeight", _styleFontWeight.name(), DEF_STYLE_FONT_WEIGHT.name());
				jsonInfo.put("style.fontStyle", _styleFontStyle.name(), DEF_STYLE_FONT_STYLE.name());
				jsonInfo.put("style.fill", _styleFill, DEF_STYLE_FILL);
				jsonInfo.put("style.stroke", _styleStroke, DEF_STYLE_STROKE);
				jsonInfo.put("style.strokeThickness", _styleStrokeThickness, DEF_STYLE_STROKE_THICKNESS);
				jsonInfo.put("style.backgroundColor", _styleBackgroundColor, DEF_STYLE_BACKGROUND_COLOR);
				jsonInfo.put("style.align", _styleAlign.name(), DEF_STYLE_ALIGN.name());
			}
		}

	}

	@Override
	public void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_text = jsonInfo.optString("text", "");

		_styleFont = jsonInfo.optString("style.font", DEF_STYLE_FONT);
		_styleFontSize = jsonInfo.optInt("style.fontSize", DEF_STYLE_FONT_SIZE);
		_styleFontWeight = FontWeight.valueOf(jsonInfo.optString("style.fontWeight", DEF_STYLE_FONT_WEIGHT.name()));
		_styleFontStyle = FontPosture.valueOf(jsonInfo.optString("style.fontStyle", DEF_STYLE_FONT_STYLE.name()));

		_styleFill = jsonInfo.optString("style.fill", DEF_STYLE_FILL);
		_styleBackgroundColor = jsonInfo.optString("style.backgroundColor", DEF_STYLE_BACKGROUND_COLOR);
		_styleStroke = jsonInfo.optString("style.stroke", DEF_STYLE_STROKE);
		_styleStrokeThickness = jsonInfo.optInt("style.strokeThickness", DEF_STYLE_STROKE_THICKNESS);

		_styleAlign = TextAlignment.valueOf(jsonInfo.optString("style.align", DEF_STYLE_ALIGN.name()));
	}

	@Override
	protected List<AnimationModel> readAnimations(JSONArray array) {
		// not supported in Text objects
		return Collections.emptyList();
	}

	public JSONObject getPhaserStyleObject() {
		JSONObject data = new JSONObject();

		StringBuilder sb = new StringBuilder();

		if (_styleFontStyle == FontPosture.ITALIC) {
			sb.append("italic ");
		}

		if (_styleFontWeight == FontWeight.BOLD) {
			sb.append("bold ");
		}

		sb.append(_styleFontSize + "pt ");
		sb.append(_styleFont);

		data.put("font", sb.toString());
		data.put("fill", _styleFill, DEF_STYLE_FILL);
		data.put("stroke", _styleStroke, DEF_STYLE_STROKE);
		data.put("strokeThickness", _styleStrokeThickness, DEF_STYLE_STROKE_THICKNESS);
		data.put("backgroundColor", _styleBackgroundColor, DEF_STYLE_BACKGROUND_COLOR);
		data.put("align", _styleAlign.name().toLowerCase(), DEF_STYLE_ALIGN.name().toLowerCase());

		return data;
	}

}
