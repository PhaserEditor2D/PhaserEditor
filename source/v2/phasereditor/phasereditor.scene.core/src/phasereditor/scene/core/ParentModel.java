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

import org.eclipse.core.resources.IProject;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public abstract class ParentModel extends ObjectModel implements ParentComponent {
	public ParentModel() {

		ParentComponent.init(this);

	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		var children = new ArrayList<ObjectModel>();

		var childrenData = data.getJSONArray(children_name);

		for (int i = 0; i < childrenData.length(); i++) {
			var objData = childrenData.getJSONObject(i);

			var type = objData.getString("-type");

			ObjectModel model = SceneModel.createModel(type);

			if (model != null) {
				model.read(objData, project);
				children.add(model);
			}
		}

		ParentComponent.set_children(this, children);
		for(var child : children) {
			ParentComponent.set_parent(child, this);
		}
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		var childrenData = new JSONArray();

		data.put(children_name, childrenData);

		var children = ParentComponent.get_children(this);

		for (var obj : children) {
			var objData = new JSONObject();
			childrenData.put(objData);

			obj.write(objData);
		}
	}

}
