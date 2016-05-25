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
import org.eclipse.swt.widgets.Composite;

import javafx.embed.swt.FXCanvas;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ObjectCanvas extends FXCanvas {
	private CreateBehavior _createBehaviors;
	private Pane _selectionPane;
	private SelectionBehavior _selectionBehavior;
	private DragBehavior _dragBehavior;
	private WorldModel _model;
	private PGrid _grid;
	private UpdateChangeBehavior _updateBehavior;
	private GroupControl _worldControl;
	private TreeViewer _outline;
	private Group _root;
	private ZoomBehavior _zoomBehavior;

	public ObjectCanvas(Composite parent, int style) {
		super(parent, style);
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
		_zoomBehavior = new ZoomBehavior(this);
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

	public ZoomBehavior getZoomBehavior() {
		return _zoomBehavior;
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
				ObjectCanvas.this.setFocus();
			}
		});

		getScene().setOnDragDropped(event -> {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			List<Node> newnodes = _createBehaviors.dropAssets((IStructuredSelection) selection, event);
			_selectionBehavior.setSelection(new StructuredSelection(newnodes.toArray()));
		});
	}

	private void createScene() {
		_selectionPane = new Pane();
		_selectionPane.setId("__selection-pane__");

		_worldControl = new GroupControl(this, _model);

		GroupNode world = _worldControl.getNode();

		world.setStyle("-fx-background-color:white;-fx-border-color:darkGray;border-style:solid;");

		_root = new Group(world, _selectionPane);
		int width = _model.getWorldWidth();
		int height = _model.getWorldHeight();
		world.setMinSize(width, height);
		world.setMaxSize(width, height);
		_selectionPane.setMinSize(width, height);
		_selectionPane.setMaxSize(width, height);

		setScene(new Scene(_root));
	}

	public Group getRoot() {
		return _root;
	}

	public GroupNode getWorldNode() {
		return _worldControl.getNode();
	}

	public Pane getSelectionPane() {
		return _selectionPane;
	}

	public void dropToWorld(BaseObjectControl<?> control, double sceneX, double sceneY) {
		IObjectNode node = control.getIObjectNode();
		GroupNode worldNode = getWorldNode();

		double invScale = 1 / _zoomBehavior.getScale();
		Point2D translate = _zoomBehavior.getTranslate();

		double x = (sceneX - translate.getX()) * invScale;
		double y = (sceneY - translate.getY()) * invScale;

		double w = control.getWidth() / 2;
		double h = control.getHeight() / 2;

		BaseObjectModel model = control.getModel();
		model.setX(x - w);
		model.setY(y - h);

		control.updateFromModel();

		worldNode.getControl().addChild(node);

		getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
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

			IObjectNode inode = (IObjectNode) elem;
			GroupNode parent = inode.getGroup();
			parent.getControl().removeChild(inode);
			_selectionBehavior.removeNodeFromSelection((Node) elem);
		}

		getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
	}
}
