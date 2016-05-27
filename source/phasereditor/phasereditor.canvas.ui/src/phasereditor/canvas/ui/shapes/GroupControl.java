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

import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridSection;

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
	protected final IObjectNode createShapeNode() {
		GroupNode group = createGroupNode();
		for (BaseObjectModel child : getModel().getChildren()) {
			BaseObjectControl<?> control = ShapeFactory.createShapeControl(getCanvas(), child);
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
	public double getWidth() {
		if (getModel() instanceof WorldModel) {
			return getModel().getWorld().getWorldWidth();
		}
		return getNode().getBoundsInLocal().getWidth();
	}

	@Override
	public double getHeight() {
		if (getModel() instanceof WorldModel) {
			return getModel().getWorld().getWorldHeight();
		}
		return getNode().getBoundsInLocal().getHeight();
	}

	public PGridBooleanProperty getClosed_property() {
		return _closed_property;
	}

	@Override
	protected void initEditorPGridModel(PGridModel propModel, PGridSection section) {
		super.initEditorPGridModel(propModel, section);

		GroupModel model = getModel();

		_closed_property = new PGridBooleanProperty("closed") {

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(model.isEditorClosed());
			}

			@Override
			public void setValue(Boolean value) {
				model.setEditorClosed(value.booleanValue());
				getCanvas().getUpdateBehavior().update_Outline(getNode());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return model.isEditorClosed();
			}
		};
		section.add(_closed_property);
	}

	public void removeChild(IObjectNode inode) {
		getModel().removeChild(inode.getModel());
		getNode().getChildren().remove(inode.getNode());
	}

	public void addChild(IObjectNode inode) {
		addChild(getNode().getChildren().size(), inode);
	}

	public void addChild(int i, IObjectNode inode) {
		getModel().addChild(i, inode.getModel());
		getNode().getChildren().add(i, inode.getNode());
	}

	/**
	 * 
	 */
	public void trim() {
		// remove the empty space from the left and top.
		GroupNode group = getNode();
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		for (Node node : group.getChildren()) {
			BaseObjectModel model = ((IObjectNode) node).getModel();
			minx = Math.min(model.getX(), minx);
			miny = Math.min(model.getY(), miny);
		}

		GroupModel groupModel = group.getModel();
		groupModel.setX(groupModel.getX() + minx);
		groupModel.setY(groupModel.getY() + miny);

		for (Node node : group.getChildren()) {
			SpriteNode sprite = (SpriteNode) node;
			BaseObjectModel model = sprite.getModel();
			model.setX(model.getX() - minx);
			model.setY(model.getY() - miny);
			sprite.getControl().updateFromModel();
		}

		group.getControl().updateFromModel();
	}
}
