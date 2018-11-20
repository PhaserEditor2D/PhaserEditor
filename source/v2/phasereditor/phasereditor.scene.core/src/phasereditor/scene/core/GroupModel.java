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

/**
 * @author arian
 *
 */
public class GroupModel extends ParentModel implements

		GroupComponent

{

	private GroupsModel _groups;

	public GroupModel(GroupsModel groups) {
		super(null);

		_groups = groups;

		GroupComponent.init(this);
	}

	public GroupsModel getGroups() {
		return _groups;
	}

	@Override
	public void read(JSONObject data, IProject project) {
		super.read(data, project);

		GroupComponent.set_name(this, data.optString(name_name));
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		data.put(name_name, GroupComponent.get_name(this));
	}

	@Override
	protected void writeChild(ObjectModel obj, JSONObject data) {
		data.put("-id", obj.getId());
	}

	@Override
	protected ObjectModel readChild(IProject project, JSONObject childData) {

		var id = childData.getString("-id");

		var model = getGroups().getSceneModel().getDisplayList().findById(id);

		return model;
	}
}
