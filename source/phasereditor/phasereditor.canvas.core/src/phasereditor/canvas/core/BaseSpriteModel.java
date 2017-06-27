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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.canvas.core.BodyModel.BodyType;

/**
 * @author arian
 *
 */
public abstract class BaseSpriteModel extends BaseObjectModel {
	private static final String TINT_0X000000 = "0x000000";
	public static final double DEF_ANCHOR_X = 0;
	public static final double DEF_ANCHOR_Y = 0;
	public static final String DEF_TINT = null;

	public static final String PROPSET_TEXTURE = "texture";
	public static final String PROPSET_ANCHOR = "anchor";
	public static final String PROPSET_TINT = "tint";
	public static final String PROPSET_ANIMATIONS = "animations";
	public static final String PROPSET_PHYSICS = "physics";
	public static final String PROPSET_DATA = "data";

	private double _anchorX;
	private double _anchorY;
	private String _tint;
	private List<AnimationModel> _animations;
	private String _data;
	private BodyModel _body;

	public BaseSpriteModel(GroupModel parent, String typeName, JSONObject obj) {
		this(parent, typeName);
		read(obj);
	}

	public BaseSpriteModel(GroupModel parent, String typeName) {
		super(parent, typeName);
		_anchorX = DEF_ANCHOR_X;
		_anchorY = DEF_ANCHOR_Y;
		_tint = DEF_TINT;
		_animations = new ArrayList<>();
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
		if (TINT_0X000000.equals(tint)) {
			_tint = null;
		} else {
			_tint = tint;
		}
	}

	public List<AnimationModel> getAnimations() {
		return _animations;
	}

	public void setAnimations(List<AnimationModel> animations) {
		_animations = animations;
	}

	public String getData() {
		return _data;
	}

	public void setData(String data) {
		_data = data;
	}

	public BodyModel getBody() {
		return _body;
	}

	public void setBody(BodyModel body) {
		_body = body;
	}

	public ArcadeBodyModel getArcadeBody() {
		if (_body != null && _body instanceof ArcadeBodyModel) {
			return (ArcadeBodyModel) _body;
		}

		return null;
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo, boolean saving) {
		super.writeInfo(jsonInfo, saving);

		boolean prefabInstance = isPrefabInstance();

		if (isOverriding(PROPSET_ANCHOR)) {
			if (prefabInstance) {
				jsonInfo.put("anchor.x", _anchorX);
				jsonInfo.put("anchor.y", _anchorY);
			} else {
				jsonInfo.put("anchor.x", _anchorX, DEF_ANCHOR_X);
				jsonInfo.put("anchor.y", _anchorY, DEF_ANCHOR_Y);
			}
		}

		if (isOverriding(PROPSET_TINT)) {
			if (prefabInstance) {
				jsonInfo.put("tint", _tint);
			} else {
				jsonInfo.put("tint", _tint, DEF_TINT);
			}
		}

		if (isOverriding(PROPSET_DATA)) {
			if (prefabInstance) {
				jsonInfo.put("data", _data);
			} else {
				jsonInfo.put("data", _data, null);
			}
		}

		if (isOverriding(PROPSET_ANIMATIONS)) {
			if (!_animations.isEmpty() || prefabInstance) {
				JSONArray array = new JSONArray();
				jsonInfo.put("animations", array);
				for (AnimationModel model : _animations) {
					JSONObject obj = new JSONObject();
					model.write(obj);
					array.put(obj);
				}
			}
		}

		if (isOverriding(PROPSET_PHYSICS)) {
			if (_body != null || prefabInstance) {
				jsonInfo.put("body", _body == null ? null : _body.toJSON());
			}
		}
	}

	@Override
	public void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);
		_anchorX = jsonInfo.optDouble("anchor.x", DEF_ANCHOR_X);
		_anchorY = jsonInfo.optDouble("anchor.y", DEF_ANCHOR_Y);
		_tint = jsonInfo.optString("tint", DEF_TINT);
		_data = jsonInfo.optString("data", null);

		_animations = new ArrayList<>();

		if (jsonInfo.has("animations")) {
			JSONArray array = jsonInfo.getJSONArray("animations");
			_animations = readAnimations(array);
		}

		{
			JSONObject bodyData = jsonInfo.optJSONObject("body");
			if (bodyData == null) {
				_body = null;
			} else {
				String type = bodyData.getString("type");
				BodyType bodyType = BodyType.valueOf(type);
				_body = bodyType.createModel();
				_body.readJSON(bodyData);
			}
		}
	}

	protected abstract List<AnimationModel> readAnimations(JSONArray array);
}
