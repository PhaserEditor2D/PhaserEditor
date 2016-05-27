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

import java.beans.PropertyChangeEvent;
import java.security.InvalidParameterException;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;

import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridProperty;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * A class to update the grid, from canvas changes, and update the canvas, from
 * grid changes.
 * 
 * @author arian
 *
 */
public class UpdateChangeBehavior {
	private ObjectCanvas _canvas;
	private PGrid _grid;
	private TreeViewer _outline;

	public UpdateChangeBehavior(ObjectCanvas canvas, PGrid grid, TreeViewer outline) {
		super();
		_canvas = canvas;
		_grid = grid;
		_outline = outline;

		outline.addSelectionChangedListener(this::update_Grid_from_Selection);
		canvas.getSelectionBehavior().addSelectionChangedListener(this::update_Grid_from_Selection);
		canvas.getWorldModel().addPropertyChangeListener(WorldModel.PROP_STRUCTURE, this::modelStructuredChanged);
	}

	@SuppressWarnings("unused")
	private void modelStructuredChanged(PropertyChangeEvent evt) {
		_outline.refresh();
	}

	private void update_Grid_from_Selection(SelectionChangedEvent event) {
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();

		if (sel.isEmpty()) {
			// TODO: implement support for no selection.
			return;
		}

		Object[] elements = sel.toArray();
		if (elements.length > 1) {
			// TODO: implement support for multiple elements.
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

}
