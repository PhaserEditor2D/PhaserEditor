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

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.MissingAssetException;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.core.WorldModel.ZOperation;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.inspect.core.InspectCore;

/**
 * @author arian
 *
 */
@SuppressWarnings("synthetic-access")
public abstract class BaseObjectControl<T extends BaseObjectModel> {
	private T _model;
	private Node _node;
	private IObjectNode _inode;
	private ObjectCanvas _canvas;
	private PGridModel _propModel;
	private PGridNumberProperty _x_property;
	private PGridNumberProperty _y_property;
	private PGridNumberProperty _angle_property;
	private PGridNumberProperty _scale_x_property;
	private PGridNumberProperty _scale_y_property;
	private PGridNumberProperty _pivot_x_property;
	private PGridNumberProperty _pivot_y_property;
	private PGridBooleanProperty _editorPick_property;

	public BaseObjectControl(ObjectCanvas canvas, T model) {
		_canvas = canvas;
		_model = model;
		_inode = createNode();
		_node = _inode.getNode();
		_propModel = new PGridModel();

		initPGridModel(_propModel);
		updateFromModel();
	}

	public String getId() {
		return _model.getId();
	}

	public BaseObjectControl<?> findById(String id) {
		if (getId().equals(id)) {
			return this;
		}
		return null;
	}

	public int getDepthLevel() {
		GroupNode group = getGroup();

		if (group == null) {
			return 0;
		}

		return group.getControl().getDepthLevel() + 1;
	}

	public boolean isWorld() {
		return !(_node.getParent() instanceof GroupNode);
	}

	public GroupNode getGroup() {
		if (_node.getParent() instanceof GroupNode) {
			return (GroupNode) _node.getParent();
		}
		return null;
	}

	public void updateFromModel() {
		_node.setId(_model.getEditorName());
		_node.setLayoutX(_model.getX());
		_node.setLayoutY(_model.getY());

		if (_model.isEditorShow() != _node.isVisible()) {
			_node.setVisible(_model.isEditorShow());
		}

		ObservableList<Transform> transforms = _node.getTransforms();
		transforms.clear();

		updateTransforms(transforms);
	}

	public void updateTransforms(ObservableList<Transform> transforms) {
		{
			// rotation
			double a = _model.getAngle();
			Transform rotation = Transform.rotate(a, 0, 0);
			transforms.add(rotation);
		}

		{
			// pivot
			double px = _model.getPivotX();
			double py = _model.getPivotY();
			Translate pivot = Transform.translate(-px * _model.getScaleX(), -py * _model.getScaleY());
			transforms.add(pivot);
		}

		{
			// scale
			Transform scale = Transform.scale(_model.getScaleX(), _model.getScaleY());
			transforms.add(scale);
		}
	}

	public T getModel() {
		return _model;
	}

	public PGridModel getPropertyModel() {
		return _propModel;
	}

	protected static String help(String member) {
		return (member + "\n\n" + InspectCore.getPhaserHelp().getMemberHelp(member)).trim();
	}

	protected static String help(String member, String arg) {
		return (member + "(...," + arg + ",...)" + "\n\n" + InspectCore.getPhaserHelp().getMethodArgHelp(member, arg))
				.trim();
	}

	protected void initPGridModel(PGridModel propModel) {
		PGridSection editorSection = new PGridSection("Editor");
		initEditorPGridModel(propModel, editorSection);

		PGridSection displaySection = new PGridSection("Display");

		propModel.getSections().add(displaySection);

		_x_property = new PGridNumberProperty(getId(), "x", help("Phaser.Sprite.x")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getX());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setX(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getX() != 0;
			}
		};

		_y_property = new PGridNumberProperty(getId(), "y", help("Phaser.Sprite.y")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getY());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setY(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getY() != 0;
			}
		};

		_angle_property = new PGridNumberProperty(getId(), "angle", help("Phaser.Sprite.angle")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getAngle());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setAngle(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getAngle() != 0;
			}
		};

		_scale_x_property = new PGridNumberProperty(getId(), "scale.x", help("Phaser.Sprite.scale")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getScaleX());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setScaleX(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getScaleX() != 1;
			}

		};

		_scale_y_property = new PGridNumberProperty(getId(), "scale.y", help("Phaser.Sprite.scale")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getScaleY());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setScaleY(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getScaleY() != 1;
			}

		};

		_pivot_x_property = new PGridNumberProperty(getId(), "pivot.x", help("Phaser.Sprite.pivot")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getPivotX());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setPivotX(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getPivotX() != 0;
			}
		};

		_pivot_y_property = new PGridNumberProperty(getId(), "pivot.y", help("Phaser.Sprite.pivot")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getPivotY());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setPivotY(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getPivotY() != 0;
			}
		};

		displaySection.add(_x_property);
		displaySection.add(_y_property);
		displaySection.add(_angle_property);
		displaySection.add(_scale_x_property);
		displaySection.add(_scale_y_property);
		displaySection.add(_pivot_x_property);
		displaySection.add(_pivot_y_property);

	}

	protected void initEditorPGridModel(PGridModel propModel, PGridSection section) {
		propModel.getSections().add(section);

		section.add(new PGridStringProperty(getId(), "name", "The name of the object. Used to generate the var name.") {

			@Override
			public String getValue() {
				return _model.getEditorName();
			}

			@Override
			public void setValue(String value, boolean notify) {
				_model.setEditorName(value);
				_canvas.getUpdateBehavior().update_Outline(_inode);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		PGridBooleanProperty editorPublic_property = new PGridBooleanProperty(getId(), "public",
				"If true the object is set 'public' in the generated code.") {
			@Override
			public Boolean getValue() {
				return Boolean.valueOf(getModel().isEditorPublic());
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getModel().setEditorPublic(value.booleanValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().isEditorPublic() != BaseObjectModel.DEF_EDITOR_PUBLIC;
			}
		};
		section.add(editorPublic_property);

		_editorPick_property = new PGridBooleanProperty(getId(), "pick",
				"Enable the ability to pick the object in the editor,\nDoes not affect the game.") {

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(_model.isEditorPick());
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				_model.setEditorPick(value.booleanValue());

				if (notify) {
					updateFromPropertyChange();
					_canvas.getUpdateBehavior().update_Outline(getIObjectNode());
				}
			}

			@Override
			public boolean isModified() {
				return !_model.isEditorPick();
			}
		};
		section.add(_editorPick_property);

		section.add(new PGridBooleanProperty(getId(), "generate",
				"If set to false the code generator ignores this object.") {

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(getModel().isEditorGenerate());
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getModel().setEditorGenerate(value.booleanValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return !getModel().isEditorGenerate();
			}
		});

		section.add(new PGridBooleanProperty(getId(), "show",
				"Set to false to hides the object in the editor.\nYou can use this in background objects, etc..\nThis value does not affect the game.") {

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(getModel().isEditorShow());
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getModel().setEditorShow(value.booleanValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return !getModel().isEditorShow();
			}
		});
	}

	public void updateFromPropertyChange() {
		UpdateBehavior update = getCanvas().getUpdateBehavior();
		update.update_Canvas_from_GridChange(this);
		update.fireWorldChanged();
	}

	public PGridNumberProperty getX_property() {
		return _x_property;
	}

	public PGridNumberProperty getY_property() {
		return _y_property;
	}

	public PGridBooleanProperty getEditorPick_property() {
		return _editorPick_property;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	protected abstract IObjectNode createNode();

	public Node getNode() {
		return _node;
	}

	public IObjectNode getIObjectNode() {
		return (IObjectNode) _node;
	}

	public void removeme() {
		getGroup().getControl().removeChild(this._inode);
	}

	protected static Display getDisplay() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay();
	}

	public double getX() {
		return _model.getX();
	}

	public double getY() {
		return _model.getY();
	}

	public abstract double getTextureWidth();

	public abstract double getTextureHeight();

	public double getTextureLeft() {
		T model = getModel();
		return model.getX();
	}

	public double getTextureTop() {
		T model = getModel();
		return model.getY();
	}

	public double getTextureRight() {
		return getX() + getTextureWidth();
	}

	public double getTextureBottom() {
		return getY() + getTextureHeight();
	}

	public void applyZOperation(ZOperation op) {
		GroupNode parent = (GroupNode) _node.getParent();
		op.perform(parent.getChildren(), _node);
		op.perform(parent.getModel().getChildren(), getIObjectNode().getModel());
	}

	/**
	 * Rebuild the model and nodes.
	 * 
	 * @return If the structure changed. Needed for example t refresh the
	 *         outline view.
	 */
	public boolean rebuild() {
		rebuildFromPrefab();

		getModel().build();

		return false;
	}

	protected void rebuildFromPrefab() {
		T model = getModel();

		if (!model.isPrefabInstance()) {
			return;
		}

		if (model.isPrefabInstance()) {
			Prefab prefab = model.getPrefab();

			JSONObject data = model.toJSON(true);

			if (!prefab.getFile().exists()) {
				// TODO: throw a missing prefab exception
				throw new MissingAssetException(data);
			}

			BaseObjectModel newModel = CanvasModelFactory.createModel(model.getParent(), data);
			BaseObjectControl<?> newControl = CanvasObjectFactory.createObjectControl(_canvas, newModel);
			GroupControl parentControl = getGroup().getControl();
			int i = parentControl.removeChild(getIObjectNode());
			parentControl.addChild(i, newControl.getIObjectNode());
		}

	}
}
