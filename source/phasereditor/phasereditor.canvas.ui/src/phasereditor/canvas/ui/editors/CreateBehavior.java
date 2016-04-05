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
package phasereditor.canvas.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import javafx.scene.input.DragEvent;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteShapeModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ObjectModelFactory;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.BaseObjectNode;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ShapeFactory;

/**
 * @author arian
 *
 */
public class CreateBehavior {
	private ShapeCanvas _canvas;

	public CreateBehavior(ShapeCanvas canvas) {
		_canvas = canvas;
	}

	public List<BaseObjectNode> dropAssets(IStructuredSelection selection, DragEvent event) {
		Object[] elems = selection.toArray();
		List<BaseObjectNode> _newnodes = new ArrayList<>();
		if (elems.length == 1) {
			Object elem = elems[0];
			if (elem instanceof IAssetElementModel || elem instanceof AssetModel) {
				// TODO: for now get as parent the world
				WorldModel worldModel = _canvas.getWorldModel();
				BaseSpriteShapeModel model = ObjectModelFactory.createModel(worldModel, elem);
				if (model != null) {
					String newname = worldModel.createName(model.getEditorName());
					model.setEditorName(newname);
					BaseObjectControl<?> control = ShapeFactory.createShapeControl(_canvas, model);
					if (control != null) {
						_newnodes.add(control.getNode());
						dropShape(control, event);
					}
				}
			}
		}
		return _newnodes;
	}

	public void dropShape(BaseObjectControl<?> control, DragEvent event) {
		_canvas.dropToWorld(control, event.getSceneX(), event.getSceneY());
	}

	/**
	 * @param elems
	 */
	public void makeGroup(Object... elems) {
		List<BaseObjectNode> children = new ArrayList<>();

		Set<Object> used = new HashSet<>();

		GroupNode parent = null;

		for (Object elem : elems) {
			BaseObjectNode node = (BaseObjectNode) elem;

			// skip nodes under used groups
			GroupNode group = node.getGroup();

			if (group != null) {
				if (used.contains(group)) {
					continue;
				}
				if (parent == null || group.getDepthLevel() < parent.getDepthLevel()) {
					parent = group;
				}
			}

			children.add(node);
		}

		// remove selected nodes
		for (BaseObjectNode child : children) {
			child.getGroup().getChildren().remove(child);
		}

		// reverse it because in selection it is reversed
		Collections.reverse(children);

		@SuppressWarnings("null")
		BaseObjectControl<?> parentControl = parent.getControl();
		GroupModel parentModel = (GroupModel) parentControl.getModel();
		GroupModel groupModel = new GroupModel(parentModel);
		groupModel.setEditorName(_canvas.getWorldModel().createName("group"));

		for (BaseObjectNode node : children) {
			BaseObjectModel model = node.getControl().getModel();
			groupModel.addChild(model);
		}

		ShapeCanvas canvas = parentControl.getCanvas();
		GroupControl groupControl = new GroupControl(canvas, groupModel);
		GroupNode group = groupControl.getGroupNode();

		parentModel.addChild(groupModel);

		parent.getChildren().add(group);

		canvas.getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);

		canvas.getSelectionBehavior().setSelection(new StructuredSelection(group));
	}
}
