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

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
	private PhysicsType _physicsBodyType;
	private PhysicsSortDirection _physicsSortDirection;

	public GroupModel(GroupModel parent, JSONObject data) {
		super(parent, TYPE_NAME, data);
	}

	public GroupModel(GroupModel parent) {
		super(parent, "group");
		_children = new ArrayList<>();
		_physicsBodyType = PhysicsType.ARCADE;
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

		if (isPrefabInstance()) {
			return true;
		}

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

	public PhysicsType getPhysicsBodyType() {
		return _physicsBodyType;
	}

	public void setPhysicsBodyType(PhysicsType physicsBodyType) {
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
	public void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_editorClosed = jsonInfo.optBoolean("editorClosed", false);

		_physicsGroup = jsonInfo.optBoolean("physicsGroup", false);
		{
			String name = jsonInfo.optString("physicsBodyType", PhysicsType.ARCADE.name());
			_physicsBodyType = PhysicsType.valueOf(name);
		}
		{
			String name = jsonInfo.optString("physicsSortDirection", PhysicsSortDirection.NULL.name());
			_physicsSortDirection = PhysicsSortDirection.valueOf(name);
		}
		_children = new ArrayList<>();

		try {
			JSONArray modelList = jsonInfo.optJSONArray("children");
			if (modelList != null) {
				for (int i = 0; i < modelList.length(); i++) {
					JSONObject jsonModel = modelList.getJSONObject(i);

					try {
						BaseObjectModel model = CanvasModelFactory.createModel(this, jsonModel);

						if (model != null) {
							_children.add(model);
						}
					} catch (MissingAssetException e) {
						out.println("Cannot open asset " + e.getData().toString(2));
						MissingAssetSpriteModel missingModel = new MissingAssetSpriteModel(this, e.getData());
						_children.add(missingModel);
					} catch (MissingPrefabException e) {
						out.println("Cannot open prefab " + e.getData().toString(2));
						MissingPrefabModel missingModel = new MissingPrefabModel(this, e.getData());
						_children.add(missingModel);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo, boolean saving) {
		super.writeInfo(jsonInfo, saving);

		jsonInfo.put("editorClosed", _editorClosed, false);

		if (isOverriding(BaseSpriteModel.PROPSET_PHYSICS)) {
			jsonInfo.put("physicsGroup", _physicsGroup, false);
			jsonInfo.put("physicsBodyType", _physicsBodyType, PhysicsType.ARCADE);
			jsonInfo.put("physicsSortDirection", _physicsSortDirection, PhysicsSortDirection.NULL);
		}

		if (isPrefabInstance() && saving) {
			// we are not going to save the children of prefabs cause we are not
			// allowing to change them in the editor. Only properties can be
			// edited in prefabs.
			//
		} else {
			JSONArray childrenData = new JSONArray();
			for (BaseObjectModel model : _children) {
				JSONObject data = new JSONObject();
				childrenData.put(data);
				model.write(data, saving);
			}
			jsonInfo.put("children", childrenData);
		}
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
		for (BaseObjectModel child : _children) {
			child.build();
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

	public void walk(Function<BaseObjectModel, Boolean> visitor) {
		walk2(visitor);
	}

	private boolean walk2(Function<BaseObjectModel, Boolean> visitor) {
		Boolean b = visitor.apply(this);
		if (b == null || !b.booleanValue()) {
			return false;
		}

		for (BaseObjectModel child : getChildren()) {
			if (child instanceof GroupModel) {
				if (!((GroupModel) child).walk2(visitor)) {
					return false;
				}
			} else {
				b = visitor.apply(child);
				if (b == null || !b.booleanValue()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean hasErrors() {
		for (BaseObjectModel child : _children) {
			if (child.hasErrors()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 */
	public void trim() {
		// remove the empty space from the left and top.
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		for (BaseObjectModel model : _children) {
			minx = Math.min(model.getX(), minx);
			miny = Math.min(model.getY(), miny);
		}

		for (BaseObjectModel model : _children) {
			model.setX(model.getX() - minx);
			model.setY(model.getY() - miny);
		}
	}
}
