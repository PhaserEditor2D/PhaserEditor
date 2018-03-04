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

import static java.lang.System.out;

import java.beans.PropertyChangeEvent;
import java.security.InvalidParameterException;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbench;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridProperty;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 */
public class UpdateBehavior {
	private ObjectCanvas _canvas;
	private PGrid _grid;
	private TreeViewer _outline;

	public UpdateBehavior(ObjectCanvas canvas, PGrid grid, TreeViewer outline) {
		super();
		_canvas = canvas;
		_grid = grid;
		_outline = outline;

		outline.addSelectionChangedListener(this::update_Grid_from_Selection);
		_canvas.getSelectionBehavior().addSelectionChangedListener(this::update_Grid_from_Selection);
		_canvas.getWorldModel().addPropertyChangeListener(WorldModel.PROP_STRUCTURE, this::modelStructuredChanged);

	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	public void dispose() {
		// nothing
	}

	public void rebuild() {
		out.println("Rebuild canvas (in editor) " + _canvas.getWorldModel().getFile().getLocation());

		List<String> selection = _canvas.getSelectionBehavior().getSelectionNodeIds();

		GroupControl control = _canvas.getWorldNode().getControl();

		if (control.rebuild()) {
			_outline.setInput(getCanvas());
		}

		_canvas.getPalette().rebuild();

		control.updateAllFromModel();
		_canvas.getZoomBehavior().updateZoomAndPan();

		// needed for text controls
		_canvas.getDisplay().asyncExec(() -> {
			_canvas.getSelectionBehavior().setSelection(selection);
		});
	}

	public void singleRebuildFromPrefab(BaseObjectControl<?> control) {
		List<IObjectNode> sel = _canvas.getSelectionBehavior().getSelectedNodes();
		boolean selected = sel.contains(control.getIObjectNode());

		BaseObjectControl<?> newControl = control.rebuildFromPrefab();

		_canvas.getZoomBehavior().updateZoomAndPan();

		if (selected) {
			_canvas.getSelectionBehavior().setSelection(new StructuredSelection(newControl.getNode()));
		} else {
			_canvas.getSelectionBehavior().updateSelectedNodes();
		}
	}

	@SuppressWarnings("unused")
	private void modelStructuredChanged(PropertyChangeEvent evt) {
		_outline.refresh();
	}

	private void update_Grid_from_Selection(SelectionChangedEvent event) {
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();

		Object[] elements = sel.toArray();

		if (elements.length != 1) {

			if (_canvas.getEditor().getModel().getType() == CanvasType.SPRITE) {
				ObservableList<Node> children = _canvas.getWorldNode().getChildren();
				if (!children.isEmpty()) {
					IObjectNode sprite = (IObjectNode) children.get(0);
					_grid.setModel(sprite.getControl().getPropertyModel());
				}
				return;
			}

			_grid.setModel(null);
			return;
		}

		Object element = elements[0];

		PGridModel model = createGridModelFromElement(element);
		_grid.setModel(model);
	}

	public void update_Grid_from_PropertyChange(PGridProperty<?> prop) {
		_canvas.dirty();
		_grid.refresh(prop);
	}

	public void update_Grid() {
		_canvas.dirty();
		_grid.refresh();
	}

	public void update_Canvas_from_GridChange(BaseObjectControl<?> changedShape) {
		_canvas.dirty();
		changedShape.updateFromModel();
		_canvas.getSelectionBehavior().updateSelectedNodes();
	}

	private static PGridModel createGridModelFromElement(Object element) {

		if (element instanceof IObjectNode) {
			IObjectNode node = (IObjectNode) element;
			BaseObjectControl<?> control = node.getControl();
			return control.getPropertyModel();
		}

		throw new InvalidParameterException("All elements needs a property model.");
	}

	public void update_Outline(IObjectNode node) {
		_outline.refresh(node);
	}

	public void updateLocation_undoable(IObjectNode node, double x, double y) {
		BaseObjectControl<?> control = node.getControl();
		ChangePropertyOperation<?> changeX = new ChangePropertyOperation<>(control.getId(),
				control.getX_property().getName(), Double.valueOf(x));
		ChangePropertyOperation<?> changeY = new ChangePropertyOperation<>(control.getId(),
				control.getY_property().getName(), Double.valueOf(y));

		CompositeOperation change = new CompositeOperation(changeX, changeY);
		IWorkbench workbench = _canvas.getEditor().getSite().getWorkbenchWindow().getWorkbench();
		try {
			IOperationHistory history = workbench.getOperationSupport().getOperationHistory();
			history.execute(change, null, _canvas.getEditor());
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("static-method")
	public void addUpdateLocationOperation(CompositeOperation operations, IObjectNode node, double x, double y,
			boolean notify) {
		BaseObjectControl<?> control = node.getControl();
		ChangePropertyOperation<?> changeX = new ChangePropertyOperation<>(control.getId(),
				control.getX_property().getName(), Double.valueOf(x), notify);
		ChangePropertyOperation<?> changeY = new ChangePropertyOperation<>(control.getId(),
				control.getY_property().getName(), Double.valueOf(y), notify);

		operations.add(changeX);
		operations.add(changeY);
	}

	public void executeOperations(CompositeOperation group) {
		if (group.isEmpty()) {
			return;
		}

		CanvasEditor editor = _canvas.getEditor();
		group.addContext(editor.undoContext);
		IWorkbench workbench = editor.getSite().getWorkbenchWindow().getWorkbench();
		try {
			IOperationHistory history = workbench.getOperationSupport().getOperationHistory();
			history.execute(group, null, editor);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void fireWorldChanged() {
		_canvas.getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
	}

	public void updateFromSettings() {
		double width = _canvas.getSettingsModel().getSceneWidth();
		double height = _canvas.getSettingsModel().getSceneHeight();
		_canvas.getRootPane().setMinSize(width, height);
		_canvas.getRootPane().setMaxSize(width, height);
		GroupNode world = _canvas.getWorldNode();
		world.setMinSize(width, height);
		world.setMaxSize(width, height);
		_canvas.getSelectionPane().setMinSize(width, height);
		_canvas.getSelectionPane().setMaxSize(width, height);
		_canvas.getSelectionFrontPane().setMinSize(width, height);
		_canvas.getSelectionFrontPane().setMaxSize(width, height);
		_canvas.getFrontGridPane().repaint();
		_canvas.getBackGridPane().repaint();
	}
}
