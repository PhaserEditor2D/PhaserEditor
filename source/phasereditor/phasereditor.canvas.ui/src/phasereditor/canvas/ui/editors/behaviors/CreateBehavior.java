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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
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
import phasereditor.canvas.core.Prefab;
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

		// check the elements come from the same project

		IProject dstProject = _canvas.getEditor().getEditorInputFile().getProject();
		for (Object elem : elems) {
			if (elem instanceof IAssetKey) {
				IProject srcProject = ((IAssetKey) elem).getAsset().getPack().getFile().getProject();
				if (!srcProject.equals(dstProject)) {
					MessageDialog.openInformation(_canvas.getShell(), "Canvas",
							"Cannot paste assets from other projects.");
					return;
				}
			}
		}

		int i = 0;
		CompositeOperation operations = new CompositeOperation();

		List<String> selectionIds = new ArrayList<>();

		WorldModel worldModel = _canvas.getWorldModel();

		for (Object elem : elems) {
			BaseObjectControl<?> control = null;
			BaseObjectModel model = null;

			if (elem instanceof IAssetKey) {
				// TODO: for now get as parent the world
				model = factory.apply(worldModel, (IAssetKey) elem);
			} else if (elem instanceof Prefab) {
				Prefab prefab = (Prefab) elem;
				model = CanvasModelFactory.createModel(worldModel, prefab);
			}

			if (model != null) {
				String newname = worldModel.createName(model.getEditorName());
				model.setEditorName(newname);
				control = CanvasObjectFactory.createObjectControl(_canvas, model);
			}

			if (control != null) {
				selectionIds.add(control.getModel().getId());
				double x = sceneX + i * 20;
				double y = sceneY + i * 20;
				_canvas.dropToWorld(operations, control, x, y);
				i++;
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

		// sort it by the display order

		children.sort(IObjectNode.DISPLAY_ORDER_COMPARATOR);

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
			model.write(groupData, true);
		}
		// add new group
		operations.add(new AddNodeOperation(groupData, -1, 0, 0, parentModel.getId()));

		// add children

		int i = 0;
		for (IObjectNode node : children) {
			JSONObject data = new JSONObject();
			BaseObjectModel model = node.getModel();
			model.write(data, true);
			operations.add(new AddNodeOperation(data, i, model.getX(), model.getY(), newGroupId));
			i++;
		}
		operations.add(new SelectOperation(newGroupId));

		return newGroupId;
	}

	public void paste(Object[] data) {
		List<IObjectNode> filtered = new ArrayList<>();

		for (Object elem : data) {
			if (elem instanceof IObjectNode) {
				filtered.add((IObjectNode) elem);
			}
		}

		filtered.sort(IObjectNode.DISPLAY_ORDER_COMPARATOR);

		// we use the table only if the pasting nodes comes from the same
		// canvas.
		// boolean saving = filtered.size() > 0 &&
		// filtered.get(0).getNode().getScene() == _canvas.getScene();

		// is better to do not use the table, it was introducing bugs
		boolean saving = false;

		GroupControl worldControl = _canvas.getWorldNode().getControl();
		GroupControl pasteIntoThis = worldControl;

		{
			List<IObjectNode> selnodes = _canvas.getSelectionBehavior().getSelectedNodes();
			if (selnodes.size() == 1) {
				IObjectNode node = selnodes.get(0);

				if (node instanceof GroupNode) {
					boolean ok = true;
					for (Object obj : filtered) {
						if (obj == node) {
							ok = false;
							break;
						}
					}
					if (ok) {
						pasteIntoThis = (GroupControl) node.getControl();
					}
				}
			}
		}

		double x;
		double y;
		MouseBehavior mouse = _canvas.getMouseBehavior();
		{
			Point2D pos = mouse.getMousePosition();
			if (pos == null) {
				x = _canvas.getScene().getWidth() / 2;
				y = _canvas.getScene().getHeight() / 2;
			} else {
				x = pos.getX();
				y = pos.getY();
			}
		}
		{
			Point2D p = pasteIntoThis.getNode().sceneToLocal(x, y);
			x = p.getX();
			y = p.getY();
		}

		CompositeOperation operations = new CompositeOperation();

		List<BaseObjectModel> copies = new ArrayList<>();
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		{
			for (IObjectNode node : filtered) {
				BaseObjectModel copy = node.getModel().copy(false);
				copies.add(copy);
				minx = Math.min(minx, copy.getX());
				miny = Math.min(miny, copy.getY());
			}

			for (BaseObjectModel copy : copies) {
				copy.setX(copy.getX() - minx);
				copy.setY(copy.getY() - miny);
			}

		}

		List<String> selection = new ArrayList<>();
		int i = pasteIntoThis.getNode().getChildren().size();
		for (BaseObjectModel copy : copies) {
			selection.add(copy.getId());
			double x2 = mouse.stepX(x + copy.getX(), false);
			double y2 = mouse.stepY(y + copy.getY(), false);
			AddNodeOperation op = new AddNodeOperation(copy.toJSON(saving), i, x2, y2, pasteIntoThis.getId());
			operations.add(op);
			i++;
		}
		operations.add(new SelectOperation(selection));
		_canvas.getUpdateBehavior().executeOperations(operations);
	}
}
