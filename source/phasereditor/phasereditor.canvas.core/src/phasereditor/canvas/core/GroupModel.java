// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class GroupModel extends BaseObjectModel {

	public static final String TYPE_NAME = "group";
	private List<BaseObjectModel> _children;
	private boolean _editorClosed;
	private boolean _physicsGroup;
	private PhysicsBodyType _physicsBodyType;
	private PhysicsSortDirection _physicsSortDirection;

	public GroupModel(GroupModel parent, JSONObject data) {
		super(parent, TYPE_NAME, data);
	}

	public GroupModel(GroupModel parent) {
		super(parent, "group");
		_children = new ArrayList<>();
		_physicsBodyType = PhysicsBodyType.ARCADE;
		_physicsSortDirection = PhysicsSortDirection.NULL;
	}

	@Override
	public void resetId() {
		super.resetId();
		for (BaseObjectModel child : _children) {
			child.resetId();
		}
	}

	@SuppressWarnings("static-method")
	public boolean isWorldModel() {
		return false;
	}

	public boolean isEditorClosed() {
		return _editorClosed;
	}

	public void setEditorClosed(boolean editorClosed) {
		_editorClosed = editorClosed;
	}

	public boolean isPhysicsGroup() {
		return _physicsGroup;
	}

	public void setPhysicsGroup(boolean physicsGroup) {
		_physicsGroup = physicsGroup;
	}

	public PhysicsBodyType getPhysicsBodyType() {
		return _physicsBodyType;
	}

	public void setPhysicsBodyType(PhysicsBodyType physicsBodyType) {
		_physicsBodyType = physicsBodyType;
	}

	public PhysicsSortDirection getPhysicsSortDirection() {
		return _physicsSortDirection;
	}

	public void setPhysicsSortDirection(PhysicsSortDirection physicsSortDirection) {
		_physicsSortDirection = physicsSortDirection;
	}

	@Override
	public String getLabel() {
		return "[grp] " + getEditorName();
	}

	public List<BaseObjectModel> getChildren() {
		return _children;
	}

	public BaseObjectModel findByName(String name) {
		if (getEditorName().equals(name)) {
			return this;
		}

		for (BaseObjectModel model : _children) {
			if (model instanceof GroupModel) {
				BaseObjectModel found = ((GroupModel) model).findByName(name);
				if (found != null) {
					return found;
				}
			} else if (model.getEditorName().equals(name)) {
				return model;
			}
		}
		return null;
	}

	@Override
	protected void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_editorClosed = jsonInfo.optBoolean("editorClosed", false);

		_physicsGroup = jsonInfo.optBoolean("physicsGroup", false);
		{
			String name = jsonInfo.optString("physicsBodyType", PhysicsBodyType.ARCADE.name());
			_physicsBodyType = PhysicsBodyType.valueOf(name);
		}
		{
			String name = jsonInfo.optString("physicsSortDirection", PhysicsSortDirection.NULL.name());
			_physicsSortDirection = PhysicsSortDirection.valueOf(name);
		}
		_children = new ArrayList<>();

		try {
			JSONArray modelList = jsonInfo.getJSONArray("children");
			for (int i = 0; i < modelList.length(); i++) {
				JSONObject jsonModel = modelList.getJSONObject(i);
				BaseObjectModel model = CanvasModelFactory.createModel(this, jsonModel);

				if (model != null) {
					_children.add(model);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo) {
		super.writeInfo(jsonInfo);

		jsonInfo.put("editorClosed", _editorClosed);

		jsonInfo.put("physicsGroup", _physicsGroup);
		jsonInfo.put("physicsBodyType", _physicsBodyType, PhysicsBodyType.ARCADE);
		jsonInfo.put("physicsSortDirection", _physicsSortDirection, PhysicsSortDirection.NULL);

		JSONArray childrenData = new JSONArray();

		for (BaseObjectModel model : _children) {
			JSONObject data = new JSONObject();
			childrenData.put(data);
			model.write(data);
		}

		jsonInfo.put("children", childrenData);
	}

	public void addChild(BaseObjectModel model) {
		addChild(_children.size(), model);
	}

	public void addChild(int i, BaseObjectModel model) {
		model.getParent()._children.remove(model);
		_children.add(i, model);
	}

	public void removeChild(BaseObjectModel model) {
		_children.remove(model);
	}

	@Override
	public void build() {
		for (BaseObjectModel model : _children) {
			model.build();
		}
	}

	public void walk(Consumer<BaseObjectModel> visitor) {
		visitor.accept(this);
		for (BaseObjectModel child : getChildren()) {
			if (child instanceof GroupModel) {
				((GroupModel) child).walk(visitor);
			} else {
				visitor.accept(child);
			}
		}
	}
}
