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

import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.BaseObjectNode;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.WorldControl;
import phasereditor.canvas.ui.shapes.WorldNode;

/**
 * @author arian
 *
 */
public class ShapeCanvas extends FXCanvas {
	private CreateBehavior _createBehaviors;
	private BorderPane _rootPane;
	private SelectionBehavior _selectionBehavior;
	private DragBehavior _dragBehavior;
	private WorldModel _model;
	private PGrid _grid;
	private UpdateChangeBehavior _updateBehavior;
	private WorldControl _worldControl;
	private TreeViewer _outline;

	public ShapeCanvas(Composite parent) {
		super(parent, SWT.NONE);
	}

	public void init(WorldModel model, PGrid grid, TreeViewer outline) {
		_model = model;
		_grid = grid;
		_outline = outline;

		createScene();

		initDrop();

		_createBehaviors = new CreateBehavior(this);
		_selectionBehavior = new SelectionBehavior(this);
		_dragBehavior = new DragBehavior(this);
		_updateBehavior = new UpdateChangeBehavior(this, _grid, outline);
	}

	public TreeViewer getOutline() {
		return _outline;
	}

	public PGrid getGrid() {
		return _grid;
	}

	public WorldModel getWorldModel() {
		return _model;
	}

	public CreateBehavior getCreateBehaviors() {
		return _createBehaviors;
	}

	public SelectionBehavior getSelectionBehavior() {
		return _selectionBehavior;
	}

	public DragBehavior getDragBehavior() {
		return _dragBehavior;
	}

	public UpdateChangeBehavior getUpdateBehavior() {
		return _updateBehavior;
	}

	private void initDrop() {
		getScene().setOnDragOver(event -> {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			if (selection == null) {
				event.consume();
			} else {
				event.acceptTransferModes(TransferMode.ANY);
			}
		});

		getScene().setOnDragDropped(event -> {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			List<BaseObjectNode> newnodes = _createBehaviors.dropAssets((IStructuredSelection) selection, event);
			_selectionBehavior.setSelection(new StructuredSelection(newnodes.toArray()));
		});
	}

	private void createScene() {
		_rootPane = new BorderPane();

		_worldControl = new WorldControl(this, _model);

		_rootPane.setCenter(_worldControl.getNode());

		Scene scene = new Scene(_rootPane);
		setScene(scene);
	}

	public WorldNode getWorldNode() {
		return _worldControl.getNode();
	}

	public void dropToWorld(BaseObjectControl<?> control, double sceneX, double sceneY) {
		BaseObjectNode node = control.getNode();
		WorldNode worldNode = getWorldNode();
		double x = sceneX - worldNode.getLayoutX();
		double y = sceneY - worldNode.getLayoutY();

		node.setLayoutX(x - control.getWidth() / 2);
		node.setLayoutY(y - control.getHeight() / 2);

		worldNode.getChildren().add(node);

		_model.addChild(control.getModel());
	}

	public void dirty() {
		getWorldModel().setDirty(true);
	}

	public void deleteSelected() {
		Object[] sel = _selectionBehavior.getSelection().toArray();
		for (Object elem : sel) {
			if (elem == getWorldNode()) {
				continue;
			}

			BaseObjectNode node = (BaseObjectNode) elem;
			GroupNode parent = (GroupNode) node.getParent();
			parent.getChildren().remove(node);
			GroupModel groupModel = parent.getControl().getModel();
			groupModel.removeChild(node.getControl().getModel());
			getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
		}
	}
}
