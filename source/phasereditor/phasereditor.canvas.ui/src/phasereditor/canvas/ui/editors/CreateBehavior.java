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

import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.canvas.core.BaseSpriteShapeModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ObjectModelFactory;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ShapeFactory;

/**
 * @author arian
 *
 */
public class CreateBehavior {
	private ObjectCanvas _canvas;

	public CreateBehavior(ObjectCanvas canvas) {
		_canvas = canvas;
	}

	public List<Node> dropAssets(IStructuredSelection selection, DragEvent event) {
		Object[] elems = selection.toArray();
		List<Node> _newnodes = new ArrayList<>();
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

	public GroupNode makeGroup(Object... elems) {
		List<IObjectNode> children = new ArrayList<>();

		Set<Object> used = new HashSet<>();

		GroupNode parent = null;

		for (Object elem : elems) {
			Node node = (Node) elem;

			// skip nodes under used groups
			GroupNode group = ((IObjectNode) node).getControl().getGroup();

			if (group != null) {
				if (used.contains(group)) {
					continue;
				}
				if (parent == null || group.getControl().getDepthLevel() < parent.getControl().getDepthLevel()) {
					parent = group;
				}
			}

			children.add((IObjectNode) node);
		}

		// remove selected nodes

		for (IObjectNode child : children) {
			GroupNode group = child.getControl().getGroup();
			group.getControl().removeChild(child);
		}

		// reverse it because in selection it is reversed

		Collections.reverse(children);

		// create the new group

		@SuppressWarnings("null")
		BaseObjectControl<?> parentControl = parent.getControl();
		GroupModel parentModel = (GroupModel) parentControl.getModel();
		GroupControl newGroupControl = new GroupControl(_canvas, new GroupModel(parentModel));

		// init model

		newGroupControl.getModel().setEditorName(_canvas.getWorldModel().createName("group"));

		// add children

		for (IObjectNode node : children) {
			newGroupControl.addChild(node);
		}

		// add to the parent

		parent.getControl().addChild(newGroupControl.getNode());

		// notify a world change

		_canvas.getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);

		// select the new group

		_canvas.getSelectionBehavior().setSelection(new StructuredSelection(newGroupControl.getNode()));

		return newGroupControl.getNode();
	}

	public void makeEmptyGroup(GroupNode parent) {
		GroupControl parentControl = parent.getControl();

		GroupModel newModel = new GroupModel(parentControl.getModel());
		newModel.setEditorName(_canvas.getWorldModel().createName("group"));

		GroupControl newControl = new GroupControl(_canvas, newModel);
		parentControl.addChild(newControl.getNode());

		_canvas.getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
		_canvas.getSelectionBehavior().setSelection(new StructuredSelection(newControl.getNode()));

	}
}
