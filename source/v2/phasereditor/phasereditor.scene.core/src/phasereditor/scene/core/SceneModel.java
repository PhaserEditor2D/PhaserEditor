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
import org.json.JSONObject;

import phasereditor.lic.LicCore;

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

	public SceneModel() {
		_rootObject = new WorldModel();
		
		_snapEnabled = false;
		_snapWidth = 64;
		_snapHeight = 64;
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
	}

	public void read(JSONObject data, IProject project) {
		var rootData = data.optJSONObject("root");

		var type = rootData.getString("-type");

		ObjectModel model = createModel(type);

		if (model != null) {
			model.read(rootData, project);
			_rootObject = (ParentModel) model;
		}
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
