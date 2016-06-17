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
package phasereditor.canvas.ui.editors.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;

import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class SelectOperation extends AbstractNodeOperation {

	private List<String> _beforeSelection;
	private List<String> _selection;

	public SelectOperation(String... selectionIds) {
		this(new ArrayList<>(Arrays.asList(selectionIds)));
	}

	public SelectOperation(List<String> selection) {
		super("SelectOperation", null);
		_selection = selection;
	}

	public void add(String id) {
		_selection.add(id);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		ObjectCanvas canvas = getCanvas(info);
		_beforeSelection = new ArrayList<>();

		SelectionBehavior behavior = canvas.getSelectionBehavior();

		for (IObjectNode node : behavior.getSelectedNodes()) {
			_beforeSelection.add(node.getModel().getId());
		}

		List<IObjectNode> selection = new ArrayList<>();
		for (String id : _selection) {
			BaseObjectControl<?> control = findControl(info, id);
			selection.add(control.getIObjectNode());
		}

		canvas.getUpdateBehavior().fireWorldChanged();
		behavior.setSelection(new StructuredSelection(selection));

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return updateSelection(info, _selection);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return updateSelection(info, _beforeSelection);
	}

	private static IStatus updateSelection(IAdaptable info, List<String> selectionIds) {
		ObjectCanvas canvas = getCanvas(info);
		canvas.getUpdateBehavior().fireWorldChanged();

		List<IObjectNode> selection = new ArrayList<>();
		for (String id : selectionIds) {
			BaseObjectControl<?> control = findControl(info, id);
			// is possible the node does not exist, like in operations of create
			// then select, then in the redo
			if (control != null) {
				selection.add(control.getIObjectNode());
			}
		}

		canvas.getSelectionBehavior().setSelection(new StructuredSelection(selection));

		return Status.OK_STATUS;
	}

}
