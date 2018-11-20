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
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public abstract class ParentModel extends ObjectModel implements ParentComponent {

	public ParentModel(String type) {
		super(type);

		ParentComponent.init(this);

	}

	public ObjectModel findById(String id) {
		if (getId().equals(id)) {
			return this;
		}

		for (var child : ParentComponent.get_children(this)) {

			if (child instanceof ParentModel) {
				var found = ((ParentModel) child).findById(id);
				if (found != null) {
					return found;
				}
			} else if (child.getId().equals(id)) {
				return child;
			}
		}
		return null;
	}

	public List<ObjectModel> findByIds(List<String> ids) {
		var result = new ArrayList<ObjectModel>();

		for (var id : ids) {

			var obj = findById(id);

			if (obj != null) {
				result.add(obj);
			}
		}

		return result;
	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		var children = new ArrayList<ObjectModel>();

		var childrenData = data.getJSONArray(children_name);

		for (int i = 0; i < childrenData.length(); i++) {
			var objData = childrenData.getJSONObject(i);

			var childModel = readChild(project, objData);

			if (childModel != null) {
				children.add(childModel);
			}
		}

		ParentComponent.set_children(this, children);
		for (var child : children) {
			ParentComponent.set_parent(child, this);
		}
	}

	@SuppressWarnings("static-method")
	protected ObjectModel readChild(IProject project, JSONObject childData) {
		var type = childData.getString("-type");

		ObjectModel childModel = SceneModel.createModel(type);

		if (childModel != null) {
			childModel.read(childData, project);
		}

		return childModel;

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

			writeChild(obj, objData);
		}
	}

	@SuppressWarnings("static-method")
	protected void writeChild(ObjectModel obj, JSONObject data) {
		obj.write(data);
	}

	public void visitChildren(Consumer<ObjectModel> visitor) {
		var children = ParentComponent.get_children(this);

		for (var child : children) {
			child.visit(visitor);
		}
	}

}
