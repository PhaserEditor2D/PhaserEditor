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

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.WorldModel.ZOperation;
import phasereditor.canvas.ui.editors.ObjectCanvas;
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

		updateFromModel();
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

		ObservableList<Transform> transforms = _node.getTransforms();
		transforms.clear();

		updateTransforms(transforms);
	}

	protected void updateTransforms(ObservableList<Transform> transforms) {
		double px = _model.getPivotX();
		double py = _model.getPivotY();

		{
			// rotation
			double a = _model.getAngle();
			Transform rotation = Transform.rotate(a, 0, 0);
			transforms.add(rotation);
		}

		{
			// pivot
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
		if (_propModel == null) {
			_propModel = new PGridModel();
			initPGridModel(_propModel);
		}
		return _propModel;
	}

	protected static String help(String member) {
		return member + "\n\n" + InspectCore.getPhaserHelp().getMemberHelp(member);
	}

	protected static String help(String member, String arg) {
		return member + "(...," + arg + ",...)" + "\n\n" + InspectCore.getPhaserHelp().getMethodArgHelp(member, arg);
	}

	protected void initPGridModel(PGridModel propModel) {
		PGridSection editorSec = new PGridSection("Editor");
		initEditorPGridModel(propModel, editorSec);

		PGridSection objectSec = new PGridSection("Display");

		if (!isWorld()) {
			propModel.getSections().add(objectSec);
		}

		_x_property = new PGridNumberProperty("x") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getX());
			}

			@Override
			public void setValue(Double value) {
				getModel().setX(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getX() != 0;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.x");
			}
		};

		_y_property = new PGridNumberProperty("y") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getY());
			}

			@Override
			public void setValue(Double value) {
				getModel().setY(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getY() != 0;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.y");
			}
		};

		_angle_property = new PGridNumberProperty("angle") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getAngle());
			}

			@Override
			public void setValue(Double value) {
				getModel().setAngle(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getAngle() != 0;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.angle");
			}
		};

		_scale_x_property = new PGridNumberProperty("scale.x") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getScaleX());
			}

			@Override
			public void setValue(Double value) {
				getModel().setScaleX(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getScaleX() != 1;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.scale");
			}
		};

		_scale_y_property = new PGridNumberProperty("scale.y") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getScaleY());
			}

			@Override
			public void setValue(Double value) {
				getModel().setScaleY(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getScaleY() != 1;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.scale");
			}
		};

		_pivot_x_property = new PGridNumberProperty("pivot.x") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getPivotX());
			}

			@Override
			public void setValue(Double value) {
				getModel().setPivotX(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getPivotX() != 0;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.pivot");
			}
		};

		_pivot_y_property = new PGridNumberProperty("pivot.y") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getPivotY());
			}

			@Override
			public void setValue(Double value) {
				getModel().setPivotY(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getPivotY() != 0;
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.pivot");
			}
		};

		objectSec.add(_x_property);
		objectSec.add(_y_property);
		objectSec.add(_angle_property);
		objectSec.add(_scale_x_property);
		objectSec.add(_scale_y_property);
		objectSec.add(_pivot_x_property);
		objectSec.add(_pivot_y_property);

	}

	protected void initEditorPGridModel(PGridModel propModel, PGridSection section) {
		propModel.getSections().add(section);

		section.add(new PGridStringProperty("name") {

			@Override
			public String getValue() {
				return _model.getEditorName();
			}

			@Override
			public void setValue(String value) {
				_model.setEditorName(value);
				_canvas.getUpdateBehavior().update_Outline(_inode);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		_editorPick_property = new PGridBooleanProperty("pick") {

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(_model.isEditorPick());
			}

			@Override
			public void setValue(Boolean value) {
				_model.setEditorPick(value.booleanValue());
				updateGridChange();
				_canvas.getUpdateBehavior().update_Outline(getIObjectNode());
			}

			@Override
			public boolean isModified() {
				return !_model.isEditorPick();
			}
		};
		section.add(_editorPick_property);

	}

	protected void updateGridChange() {
		getCanvas().getUpdateBehavior().update_Canvas_from_GridChange(this);
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

	public void sendNodeTo(ZOperation op) {
		GroupNode parent = (GroupNode) _node.getParent();
		op.perform(parent.getChildren(), _node);
		_canvas.getWorldModel().sendTo(getModel(), op);
	}

	public void rebuild() {
		T model = getModel();
		model.rebuild();
	}
}
