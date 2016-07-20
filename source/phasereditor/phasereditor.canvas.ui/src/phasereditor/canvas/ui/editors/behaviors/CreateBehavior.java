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
package phasereditor.canvas.ui.editors.behaviors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.CanvasObjectFactory;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class CreateBehavior {
	private ObjectCanvas _canvas;

	public CreateBehavior(ObjectCanvas canvas) {
		_canvas = canvas;
	}

	public void dropAssets(IStructuredSelection selection, DragEvent event) {
		double sceneX = event.getSceneX();
		double sceneY = event.getSceneY();
		dropAssets(selection, sceneX, sceneY, CanvasModelFactory::createModel);
	}

	public void dropAssets(IStructuredSelection selection, BiFunction<GroupModel, IAssetKey, BaseSpriteModel> factory) {
		dropAssets(selection, _canvas.getScene().getWidth() / 2, _canvas.getScene().getHeight() / 2, factory);
	}

	public void dropAssets(IStructuredSelection selection, double sceneX, double sceneY,
			BiFunction<GroupModel, IAssetKey, BaseSpriteModel> factory) {
		Object[] elems = selection.toArray();
		int i = 0;
		CompositeOperation operations = new CompositeOperation();
		List<String> selectionIds = new ArrayList<>();
		for (Object elem : elems) {
			if (elem instanceof IAssetKey) {
				// TODO: for now get as parent the world
				WorldModel worldModel = _canvas.getWorldModel();
				BaseSpriteModel model = factory.apply(worldModel, (IAssetKey) elem);
				if (model != null) {
					String newname = worldModel.createName(model.getEditorName());
					model.setEditorName(newname);
					BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(_canvas, model);
					if (control != null) {
						selectionIds.add(control.getModel().getId());
						double x = sceneX + i * 20;
						double y = sceneY + i * 20;
						_canvas.dropToWorld(operations, control, x, y);
						i++;
					}
				}
			}
		}
		if (!operations.isEmpty()) {
			operations.add(new SelectOperation(selectionIds));
			_canvas.getUpdateBehavior().executeOperations(operations);
		}

		// do not add it to palette.
		// _palette.drop(elems);
	}

	public String makeGroup(CompositeOperation operations, Object... elems) {
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

		// reverse it because in selection it is reversed

		Collections.reverse(children);

		// remove selected nodes

		for (IObjectNode child : children) {
			// GroupNode group = child.getControl().getGroup();
			// group.getControl().removeChild(child);

			operations.add(new DeleteNodeOperation(child.getModel().getId()));

		}

		// create the new group

		@SuppressWarnings("null")
		BaseObjectControl<?> parentControl = parent.getControl();
		GroupModel parentModel = (GroupModel) parentControl.getModel();
		JSONObject groupData = new JSONObject();
		String newGroupId;
		{
			GroupModel model = new GroupModel(null);
			model.setEditorName(_canvas.getWorldModel().createName("group"));
			newGroupId = model.getId();
			model.write(groupData);
		}
		// add new group
		operations.add(new AddNodeOperation(groupData, -1, 0, 0, parentModel.getId()));

		// add children

		int i = 0;
		for (IObjectNode node : children) {
			JSONObject data = new JSONObject();
			BaseObjectModel model = node.getModel();
			model.write(data);
			operations.add(new AddNodeOperation(data, i, model.getX(), model.getY(), newGroupId));
			i++;
		}
		operations.add(new SelectOperation(newGroupId));

		return newGroupId;
	}

	public void paste(Object[] data) {
		GroupControl worldControl = _canvas.getWorldNode().getControl();
		GroupControl parent = worldControl;

		{
			List<IObjectNode> selnodes = _canvas.getSelectionBehavior().getSelectedNodes();
			if (selnodes.size() == 1) {
				IObjectNode node = selnodes.get(0);
				if (node instanceof GroupNode) {
					boolean ok = true;
					for (Object obj : data) {
						if (obj == node) {
							ok = false;
							break;
						}
					}
					if (ok) {
						parent = (GroupControl) node.getControl();
					}
				}
			}
		}

		double x = _canvas.getScene().getWidth() / 2 + 50 - Math.random() * 100;
		double y = _canvas.getScene().getHeight() / 2 + 50 - Math.random() * 100;
		{
			Point2D p = _canvas.getWorldNode().sceneToLocal(x, y);
			x = p.getX();
			y = p.getY();
		}

		CompositeOperation operations = new CompositeOperation();

		int i = 0;

		List<String> selection = new ArrayList<>();

		for (Object elem : data) {
			if (elem instanceof IObjectNode) {
				IObjectNode node = (IObjectNode) elem;

				BaseObjectModel copy = node.getModel().copy(false);
				selection.add(copy.getId());

				double x2 = x + i * 20;
				double y2 = y + i * 20;

				int index = parent.getNode().getChildren().indexOf(node);

				AddNodeOperation op = new AddNodeOperation(copy.toJSON(), index, x2, y2, parent.getId());
				operations.add(op);

				i++;
			}
		}
		operations.add(new SelectOperation(selection));
		_canvas.getUpdateBehavior().executeOperations(operations);
	}
}
