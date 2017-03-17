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

import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class EditorSettings_UserCode {

	private String _create_before = "";
	private String _create_after = "";
	private String _state_constructor_before = "";
	private String _state_init_before = "";
	private String _state_init_after = "";
	private String _state_preload_before = "";
	private String _state_preload_after = "";
	private String _state_constructor_after = "";
	private CanvasModel _canvasModel;

	public EditorSettings_UserCode(CanvasModel canvasModel) {
		_canvasModel = canvasModel;
	}

	public CanvasModel getCanvasModel() {
		return _canvasModel;
	}

	public String getCreate_before() {
		return _create_before;
	}

	public void setCreate_before(String create_before) {
		_create_before = create_before;
	}

	public String getCreate_after() {
		return _create_after;
	}

	public void setCreate_after(String create_after) {
		_create_after = create_after;
	}

	public String getState_constructor_before() {
		return _state_constructor_before;
	}

	public void setState_constructor_before(String state_constructor_before) {
		_state_constructor_before = state_constructor_before;
	}

	public String getState_constructor_after() {
		return _state_constructor_after;
	}

	public void setState_constructor_after(String state_constructor_after) {
		_state_constructor_after = state_constructor_after;
	}

	public String getState_init_before() {
		return _state_init_before;
	}

	public void setState_init_before(String state_init_before) {
		_state_init_before = state_init_before;
	}

	public String getState_init_after() {
		return _state_init_after;
	}

	public void setState_init_after(String state_init_after) {
		_state_init_after = state_init_after;
	}

	public String getState_preload_before() {
		return _state_preload_before;
	}

	public void setState_preload_before(String state_preload_before) {
		_state_preload_before = state_preload_before;
	}

	public String getState_preload_after() {
		return _state_preload_after;
	}

	public void setState_preload_after(String state_preload_after) {
		_state_preload_after = state_preload_after;
	}

	public EditorSettings_UserCode copy() {
		JSONObject data = toJSON();
		EditorSettings_UserCode code = new EditorSettings_UserCode(_canvasModel);
		code.read(data);
		return code;
	}
	
	public void write(JSONObject data) {
		data.put("create_before", _create_before, "");
		data.put("create_after", _create_after, "");

		if (_canvasModel.getType() == CanvasType.STATE) {
			data.put("state_constructor_before", _state_constructor_before, "");
			data.put("state_constructor_after", _state_constructor_after, "");

			data.put("state_init_before", _state_init_before, "");
			data.put("state_init_after", _state_init_after, "");

			data.put("state_preload_before", _state_preload_before, "");
			data.put("state_preload_after", _state_preload_after, "");
		}
	}

	public void read(JSONObject data) {
		_create_before = data.optString("create_before", "");
		_create_after = data.optString("create_after", "");

		if (_canvasModel.getType() == CanvasType.STATE) {
			_state_constructor_before = data.optString("state_constructor_before", "");
			_state_constructor_after = data.optString("state_constructor_after", "");

			_state_init_before = data.optString("state_init_before", "");
			_state_init_after = data.optString("state_init_after", "");

			_state_preload_before = data.optString("state_preload_before", "");
			_state_preload_after = data.optString("state_preload_after", "");
		}
	}
	
	public JSONObject toJSON() {
		JSONObject data = new JSONObject();
		write(data);
		return data;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (_state_constructor_before.length() > 0 || _state_constructor_after.length() > 0) {
			sb.append("constructor,");
		}

		if (_state_init_before.length() > 0 || _state_init_after.length() > 0) {
			sb.append("init,");
		}

		if (_state_preload_before.length() > 0 || _state_preload_after.length() > 0) {
			sb.append("preload,");
		}

		if (_create_before.length() > 0 || _create_after.length() > 0) {
			sb.append("create,");
		}

		String str = sb.toString();

		if (str.length() > 0) {
			str = str.substring(0, str.length() - 1);
		}

		return "[" + str + "]";
	}

}
