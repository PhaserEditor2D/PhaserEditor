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

/**
 * @author arian
 *
 */
public class TextModel extends BaseSpriteModel {
	public static final String TYPE_NAME = "text";
	public static final String PROPSET_TEXT = "text";
	public static final String PROPSET_TEXT_STYLE = "textStyle";

	private String _text;
	private TextStyle _style;

	public TextModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}

	public TextModel(GroupModel parent) {
		super(parent, TYPE_NAME);

		_text = "";
		_style = new TextStyle();
	}

	public String getText() {
		return _text;
	}

	public void setText(String text) {
		_text = text;
	}

	public TextStyle getStyle() {
		return _style;
	}

	public void setStyle(TextStyle style) {
		_style = style;
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
			JSONObject styleData = new JSONObject();
			_style.writeJSON(styleData);
			jsonInfo.put("style", styleData);
		}

	}

	@Override
	public void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_text = jsonInfo.optString("text", "");

		_style = new TextStyle();
		_style.readJSON(jsonInfo.getJSONObject("style"));
	}

	@Override
	protected List<AnimationModel> readAnimations(JSONArray array) {
		// not supported in Text objects
		return Collections.emptyList();
	}

}
