// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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
public abstract class BaseSpriteModel extends BaseObjectModel {
	private double _anchorX;
	private double _anchorY;
	private String _tint;

	public BaseSpriteModel(GroupModel parent, String typeName, JSONObject obj) {
		this(parent, typeName);
		read(obj);
	}

	public BaseSpriteModel(GroupModel parent, String typeName) {
		super(parent, typeName);
		_anchorX = 0;
		_anchorY = 0;
		_tint = null;
	}
	
	@Override
	public String getLabel() {
		return "[spr] " + getEditorName();
	}

	public double getAnchorX() {
		return _anchorX;
	}

	public void setAnchorX(double anchorX) {
		_anchorX = anchorX;
	}

	public double getAnchorY() {
		return _anchorY;
	}

	public void setAnchorY(double anchorY) {
		_anchorY = anchorY;
	}
	
	public String getTint() {
		return _tint;
	}
	
	public void setTint(String tint) {
		_tint = tint;
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo) {
		super.writeInfo(jsonInfo);
		jsonInfo.put("anchor.x", _anchorX);
		jsonInfo.put("anchor.y", _anchorY);
		jsonInfo.put("tint", _tint);
	}

	@Override
	protected void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);
		_anchorX = jsonInfo.optDouble("anchor.x", 0);
		_anchorY = jsonInfo.optDouble("anchor.y", 0);
		_tint = jsonInfo.optString("tint", null);
	}
}
