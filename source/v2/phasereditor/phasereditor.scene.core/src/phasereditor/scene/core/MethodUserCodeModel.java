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
package phasereditor.scene.core;

import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class MethodUserCodeModel {
	private String _methodName;
	private String _beforeCode;
	private String _afterCode;

	public MethodUserCodeModel(String methodName) {
		super();
		_methodName = methodName;
		_beforeCode = "";
		_afterCode = "";
	}

	public String getBeforeCode() {
		return _beforeCode;
	}

	public void setBeforeCode(String beforeCode) {
		_beforeCode = beforeCode;
	}

	public String getAfterCode() {
		return _afterCode;
	}

	public void setAfterCode(String afterCode) {
		_afterCode = afterCode;
	}

	public void write(JSONObject data) {
		var methData = new JSONObject();

		methData.put("before", _beforeCode, "");
		methData.put("after", _afterCode, "");

		data.put(_methodName, methData);
	}

	public void read(JSONObject data) {
		var methData = data.optJSONObject(_methodName);

		if (methData != null) {
			_beforeCode = methData.optString("before", "");
			_afterCode = methData.optString("after", "");
		}
	}

}
