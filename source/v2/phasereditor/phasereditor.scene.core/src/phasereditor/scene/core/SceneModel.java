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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.lic.LicCore;

/**
 * @author arian
 *
 */
public class SceneModel {
	private static final int VERSION = 1;

	private List<ObjectModel> _objects;

	public SceneModel() {
		_objects = new ArrayList<>();
	}

	public List<ObjectModel> getObjects() {
		return _objects;
	}

	public void write(JSONObject data) {
		data.put("-app", "Scene Editor - " + LicCore.PRODUCT_NAME);
		data.put("-version", VERSION);
		var list = new JSONArray();
		data.put("objects", list);
		for (var obj : _objects) {
			var objData = new JSONObject();
			list.put(objData);
			obj.write(objData);
		}
	}

	public void read(JSONObject data, IProject project) {
		var objects = new ArrayList<ObjectModel>();

		var listData = data.getJSONArray("objects");

		for (int i = 0; i < listData.length(); i++) {
			var objData = listData.getJSONObject(i);

			var type = objData.getString("-type");

			ObjectModel model = createModel(type);

			if (model != null) {
				model.read(objData, project);
				objects.add(model);
			}
		}

		_objects = objects;
	}

	@SuppressWarnings("incomplete-switch")
	public static ObjectModel createModel(String type) {

		switch (type) {

		case SpriteModel.TYPE:
			return new SpriteModel();

		}

		return null;
	}
}
