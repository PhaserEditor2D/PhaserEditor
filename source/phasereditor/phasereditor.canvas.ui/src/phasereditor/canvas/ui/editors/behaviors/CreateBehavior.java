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
import java.util.UUID;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;

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
import phasereditor.canvas.ui.editors.palette.PaletteComp;
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
	private PaletteComp _palette;

	public CreateBehavior(ObjectCanvas canvas, PaletteComp palette) {
		_canvas = canvas;
		_palette = palette;
	}

	public List<Node> dropAssets(IStructuredSelection selection, DragEvent event) {
		Object[] elems = selection.toArray();
		List<Node> _newnodes = new ArrayList<>();
		int i = 0;
		for (Object elem : elems) {
			if (elem instanceof IAssetKey) {
				// TODO: for now get as parent the world
				WorldModel worldModel = _canvas.getWorldModel();
				BaseSpriteModel model = CanvasModelFactory.createModel(worldModel, (IAssetKey) elem);
				if (model != null) {
					String newname = worldModel.createName(model.getEditorName());
					model.setEditorName(newname);
					BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(_canvas, model);
					if (control != null) {
						_newnodes.add(control.getNode());
						double x = event.getSceneX() + i * 20;
						double y = event.getSceneY() + i * 20;
						dropSprite(control, x, y);
						i++;
					}
				}
			}
		}

		_palette.drop(elems);

		return _newnodes;
	}

	public void dropSprite(BaseObjectControl<?> control, double sceneX, double sceneY) {
		_canvas.dropToWorld(control, sceneX, sceneY);
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

		CompositeOperation operations = new CompositeOperation();

		int i = 0;

		List<String> selection = new ArrayList<>();

		for (Object elem : data) {
			if (elem instanceof IObjectNode) {
				IObjectNode node = (IObjectNode) elem;
				// assign new id for pasted nodes!
				String id = UUID.randomUUID().toString();
				node.getModel().setId(id);
				selection.add(id);

				JSONObject copyJson = new JSONObject();
				node.getModel().write(copyJson);

				double x2 = x + i * 20;
				double y2 = y + i * 20;

				int index = parent.getNode().getChildren().indexOf(node);

				AddNodeOperation op = new AddNodeOperation(copyJson, index, x2, y2, parent.getId());
				operations.add(op);

				i++;
			}
		}
		operations.add(new SelectOperation(selection));
		_canvas.getUpdateBehavior().executeOperations(operations);
	}
}
