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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.json.JSONObject;

import phasereditor.lic.LicCore;
import phasereditor.ui.ColorUtil;

/**
 * @author arian
 *
 */
public class SceneModel {

	private static final int VERSION = 1;

	public static final String[] GAME_OBJECT_TYPES = {

			SpriteModel.TYPE,

			ImageModel.TYPE,

			TileSpriteModel.TYPE,

			BitmapTextModel.TYPE,

			DynamicBitmapTextModel.TYPE

	};

	private ParentModel _rootObject;

	private boolean _snapEnabled;
	private int _snapWidth;
	private int _snapHeight;

	private RGB _backgroundColor;
	private RGB _foregroundColor;
	private static final RGB DEF_FG_RGB = ColorUtil.WHITESMOKE.rgb;
	private static final RGB DEF_BG_RGB = ColorUtil.LIGHTGRAY.rgb;

	public SceneModel() {
		_rootObject = new WorldModel();

		_snapEnabled = false;
		_snapWidth = 16;
		_snapHeight = 16;

		_backgroundColor = DEF_BG_RGB;
		_foregroundColor = DEF_FG_RGB;
	}

	public RGB getBackgroundColor() {
		return _backgroundColor;
	}

	public void setBackgroundColor(RGB backgroundColor) {
		_backgroundColor = backgroundColor;
	}

	public RGB getForegroundColor() {
		return _foregroundColor;
	}

	public void setForegroundColor(RGB foregroundColor) {
		_foregroundColor = foregroundColor;
	}

	public boolean isSnapEnabled() {
		return _snapEnabled;
	}

	public void setSnapEnabled(boolean snapEnabled) {
		_snapEnabled = snapEnabled;
	}

	public int getSnapWidth() {
		return _snapWidth;
	}

	public void setSnapWidth(int snapWidth) {
		_snapWidth = snapWidth;
	}

	public int getSnapHeight() {
		return _snapHeight;
	}

	public void setSnapHeight(int snapHeight) {
		_snapHeight = snapHeight;
	}

	public ParentModel getRootObject() {
		return _rootObject;
	}

	public void write(JSONObject data) {
		data.put("-app", "Scene Editor - " + LicCore.PRODUCT_NAME);
		data.put("-version", VERSION);

		{
			JSONObject rootData;
			if (_rootObject == null) {
				rootData = null;
			} else {
				rootData = new JSONObject();
				_rootObject.write(rootData);
			}

			data.put("root", rootData);
		}

		writeProperties(data);
	}

	public void read(JSONObject data, IProject project) {
		var rootData = data.optJSONObject("root");

		var type = rootData.getString("-type");

		ObjectModel model = createModel(type);

		if (model != null) {
			model.read(rootData, project);
			_rootObject = (ParentModel) model;
		}

		readProperties(data);
	}

	public void writeProperties(JSONObject data) {
		{
			data.put("snapEnabled", _snapEnabled, false);
			data.put("snapWidth", _snapWidth, 16);
			data.put("snapHeight", _snapHeight, 16);
		}
		{
			data.put("backgroundColor", asString(_backgroundColor), asString(DEF_BG_RGB));
			data.put("foregroundColor", asString(_foregroundColor), asString(DEF_FG_RGB));
		}
	}

	public void readProperties(JSONObject data) {
		{
			_snapEnabled = data.optBoolean("snapEnabled", false);
			_snapWidth = data.optInt("snapWidth", 16);
			_snapHeight = data.optInt("snapHeight", 16);
		}
		{
			_backgroundColor = asRGB(data.optString("backgroundColor", asString(DEF_BG_RGB)));
			_foregroundColor = asRGB(data.optString("foregroundColor", asString(DEF_FG_RGB)));
		}
	}

	private static String asString(RGB color) {
		return StringConverter.asString(color);
	}

	private static RGB asRGB(String color) {
		return StringConverter.asRGB(color);
	}

	@SuppressWarnings("incomplete-switch")
	public static ObjectModel createModel(String type) {

		switch (type) {

		case WorldModel.TYPE:
			return new WorldModel();

		case SpriteModel.TYPE:
			return new SpriteModel();

		case ImageModel.TYPE:
			return new ImageModel();

		case TileSpriteModel.TYPE:
			return new TileSpriteModel();

		case BitmapTextModel.TYPE:
			return new BitmapTextModel();

		case DynamicBitmapTextModel.TYPE:
			return new DynamicBitmapTextModel();
		}

		return null;
	}
}
