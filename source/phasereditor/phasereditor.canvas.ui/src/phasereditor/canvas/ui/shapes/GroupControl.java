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
package phasereditor.canvas.ui.shapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.GroupModel.SetAllData;
import phasereditor.canvas.core.MissingAssetException;
import phasereditor.canvas.core.MissingAssetSpriteModel;
import phasereditor.canvas.core.MissingPrefabException;
import phasereditor.canvas.core.MissingPrefabModel;
import phasereditor.canvas.core.PhysicsSortDirection;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridSetAllProperty;

/**
 * @author arian
 *
 */
public class GroupControl extends BaseObjectControl<GroupModel> {

	private PGridBooleanProperty _closed_property;

	public GroupControl(ObjectCanvas canvas, GroupModel model) {
		super(canvas, model);
	}

	@Override
	public BaseObjectControl<?> findById(String id) {
		if (getId().equals(id)) {
			return this;
		}

		for (Node node : getNode().getChildren()) {
			BaseObjectControl<?> control = ((IObjectNode) node).getControl();
			BaseObjectControl<?> result = control.findById(id);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	protected final IObjectNode createNode() {
		GroupNode group = createGroupNode();
		for (BaseObjectModel child : getModel().getChildren()) {
			BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(getCanvas(), child);
			group.getChildren().add(control.getNode());
		}
		return group;
	}

	protected GroupNode createGroupNode() {
		return new GroupNode(this);
	}

	@Override
	public GroupNode getNode() {
		return (GroupNode) super.getNode();
	}

	@Override
	public double getTextureLeft() {
		if (getModel() instanceof WorldModel) {
			return 0;
		}

		ObservableList<Node> list = getNode().getChildren();
		if (list.isEmpty()) {
			return 0;
		}

		double modelx = getModel().getX();
		double x = Double.MAX_VALUE;

		for (Object obj : getNode().getChildren()) {
			IObjectNode node = (IObjectNode) obj;
			x = Math.min(modelx + node.getControl().getTextureLeft(), x);
		}

		return x;
	}

	@Override
	public double getTextureTop() {
		if (getModel() instanceof WorldModel) {
			return 0;
		}

		ObservableList<Node> list = getNode().getChildren();
		if (list.isEmpty()) {
			return 0;
		}

		double modely = getModel().getY();
		double y = Double.MAX_VALUE;

		for (Object obj : list) {
			IObjectNode node = (IObjectNode) obj;
			y = Math.min(modely + node.getControl().getTextureTop(), y);
		}
		return y;
	}

	@Override
	public double getTextureWidth() {
		if (getModel() instanceof WorldModel) {
			return getCanvas().getSettingsModel().getSceneWidth();
		}

		ObservableList<Node> list = getNode().getChildren();
		if (list.isEmpty()) {
			return 0;
		}

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		for (Object obj : list) {
			IObjectNode node = (IObjectNode) obj;
			min = Math.min(node.getControl().getTextureLeft(), min);
			max = Math.max(node.getControl().getTextureRight(), max);
		}

		return max - min;
	}

	@Override
	public double getTextureHeight() {
		if (getModel() instanceof WorldModel) {
			return getCanvas().getSettingsModel().getSceneHeight();
		}

		ObservableList<Node> list = getNode().getChildren();
		if (list.isEmpty()) {
			return 0;
		}

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		for (Object obj : list) {
			IObjectNode node = (IObjectNode) obj;
			min = Math.min(node.getControl().getTextureTop(), min);
			max = Math.max(node.getControl().getTextureBottom(), max);
		}

		return max - min;
	}

	public PGridBooleanProperty getClosed_property() {
		return _closed_property;
	}

	@Override
	protected void initEditorPGridModel(PGridModel propModel, PGridSection section) {
		super.initEditorPGridModel(propModel, section);

		GroupModel model = getModel();

		_closed_property = new PGridBooleanProperty(getId(), "closed",
				"If true and you pick on a child the whole group is selected. Useful for selecting, dragging.\nThis value does not affect the game.") {

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(model.isEditorClosed());
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				model.setEditorClosed(value.booleanValue());
				getCanvas().getUpdateBehavior().update_Outline(getNode());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return model.isEditorClosed();
			}
		};

		// all prefabs are closed
		if (!model.isPrefabInstance()) {
			section.add(_closed_property);
		}

	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridSection section = new PGridSection("Group");
		section.add(new PGridBooleanProperty(getId(), "physicsGroup", help("Phaser.GameObjectFactory.physicsGroup")) {

			@Override
			public void setValue(Boolean value, boolean notify) {
				getModel().setPhysicsGroup(value.booleanValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(getModel().isPhysicsGroup());
			}

			@Override
			public boolean isModified() {
				return getModel().isPhysicsGroup();
			}

			@Override
			public boolean isReadOnly() {
				return getModel().isPrefabReadOnly("physics");
			}

		});

		section.add(new PGridEnumProperty<PhysicsType>(getId(), "physicsBodyType", help("Phaser.Group.physicsBodyType"),
				PhysicsType.VALUES_WITHOUT_NONE) {

			@Override
			public PhysicsType getValue() {
				return getModel().getPhysicsBodyType();
			}

			@Override
			public void setValue(PhysicsType value, boolean notify) {
				getModel().setPhysicsBodyType(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getPhysicsBodyType() != PhysicsType.ARCADE;
			}

			@Override
			public boolean isReadOnly() {
				return getModel().isPrefabReadOnly("physics");
			}
		});

		section.add(new PGridEnumProperty<PhysicsSortDirection>(getId(), "physicsSortDirection",
				help("Phaser.Group.physicsSortDirection"), PhysicsSortDirection.values()) {

			@Override
			public PhysicsSortDirection getValue() {
				return getModel().getPhysicsSortDirection();
			}

			@Override
			public void setValue(PhysicsSortDirection value, boolean notify) {
				getModel().setPhysicsSortDirection(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getPhysicsSortDirection() != PhysicsSortDirection.NULL;
			}

			@Override
			public boolean isReadOnly() {
				return getModel().isPrefabReadOnly(BaseSpriteModel.PROPSET_PHYSICS);
			}
		});

		section.add(new PGridSetAllProperty(getModel()) {

			@Override
			public void setValue(SetAllData value, boolean notify) {
				super.setValue(value, notify);

				if (notify) {
					updateFromPropertyChange();
				}
			}
			
		});

		propModel.getSections().add(section);

	}

	@Override
	protected void initPrefabPGridModel(List<String> validProperties) {
		super.initPrefabPGridModel(validProperties);
		validProperties.addAll(Arrays.asList(
		//@formatter:off
				BaseSpriteModel.PROPSET_PHYSICS,
				GroupModel.PROPSET_SET_ALL
				//@formatter:on
		));
	}

	public int removeChild(IObjectNode childNode) {
		GroupModel groupModel = getModel();
		BaseObjectModel childModel = childNode.getModel();

		int i = groupModel.getChildren().indexOf(childModel);

		groupModel.removeChild(childModel);
		getNode().getChildren().remove(childNode.getNode());

		return i;
	}

	public void addChild(IObjectNode inode) {
		addChild(getNode().getChildren().size(), inode);
	}

	public void addChild(int i, IObjectNode inode) {
		if (i == -1) {
			getModel().addChild(inode.getModel());
			getNode().getChildren().add(inode.getNode());
		} else {
			getModel().addChild(i, inode.getModel());
			getNode().getChildren().add(i, inode.getNode());
		}
	}

	private static class MissingRecord {
		int index;
		JSONObject data;
		IObjectNode node;
		boolean missingPrefab;

		public MissingRecord(int index, JSONObject data, IObjectNode node, boolean missingPrefab) {
			this.index = index;
			this.data = data;
			this.node = node;
			this.missingPrefab = missingPrefab;
		}
	}

	@Override
	public boolean rebuild() {
		rebuildFromPrefab();

		List<MissingRecord> missing = new ArrayList<>();

		boolean changed = false;
		int i = 0;
		ArrayList<Node> childrenCopy = new ArrayList<>(getNode().getChildren());
		for (Node node : childrenCopy) {
			IObjectNode inode = (IObjectNode) node;
			try {
				changed = inode.getControl().rebuild() || changed;
			} catch (MissingAssetException e) {
				missing.add(new MissingRecord(i, e.getData(), inode, false));
			} catch (MissingPrefabException e) {
				missing.add(new MissingRecord(i, e.getData(), inode, true));
			}
			i++;
		}

		if (!missing.isEmpty()) {
			changed = true;
			for (MissingRecord r : missing) {
				removeChild(r.node);
				BaseObjectControl<?> newControl;
				if (r.missingPrefab) {
					MissingPrefabModel newModel = new MissingPrefabModel(getModel(), r.data);
					newControl = new MissingPrefabControl(getCanvas(), newModel);
				} else {
					MissingAssetSpriteModel newModel = new MissingAssetSpriteModel(getModel(), r.data);
					newControl = new MissingAssetControl(getCanvas(), newModel);
				}
				addChild(r.index, newControl.getIObjectNode());
			}
		}

		return changed;
	}

	public void updateAllFromModel() {
		for (Node node : getNode().getChildren()) {
			IObjectNode inode = (IObjectNode) node;
			BaseObjectControl<?> control = inode.getControl();
			if (control instanceof GroupControl) {
				((GroupControl) control).updateAllFromModel();
			} else {
				control.updateFromModel();
			}
		}

		updateFromModel();
	}

	public void updateStructureFromModel() {
		List<Node> list = new ArrayList<>();
		for (BaseObjectModel model : getModel().getChildren()) {
			BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(getCanvas(), model);
			list.add(control.getNode());
		}
		getNode().getChildren().setAll(list);
		updateFromModel();
	}
}
