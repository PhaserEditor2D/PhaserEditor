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
package phasereditor.canvas.ui.editors.grid.editors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.NumberCellEditor;
import phasereditor.canvas.ui.editors.grid.PGridAnimationsProperty;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;

/**
 * @author arian
 *
 */
public class PGridEditingSupport extends EditingSupport {

	public PGridEditingSupport(ColumnViewer viewer) {
		super(viewer);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		Composite parent = (Composite) getViewer().getControl();

		if (element instanceof PGridNumberProperty) {
			return new NumberCellEditor(parent);
		} else if (element instanceof PGridStringProperty) {
			return new TextCellEditor(parent);
		} else if (element instanceof PGridBooleanProperty) {
			return new CheckboxCellEditor(parent);
		} else if (element instanceof PGridFrameProperty) {
			PGridFrameProperty prop = (PGridFrameProperty) element;
			return new FrameCellEditor(parent, prop);
		} else if (element instanceof PGridColorProperty) {
			return new RGBCellEditor(parent, ((PGridColorProperty) element).getDefaultRGB());
		} else if (element instanceof PGridAnimationsProperty) {
			return new AnimationsCellEditor(parent, (PGridAnimationsProperty) element);
		}

		return null;
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof PGridSection) {
			return false;
		}
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		if (element instanceof PGridProperty<?>) {
			return ((PGridProperty<?>) element).getValue();
		}
		return null;
	}

	@SuppressWarnings("null")
	@Override
	protected void setValue(Object element, Object value) {
		PGridProperty<?> prop = (PGridProperty<?>) element;

		Object old = prop.getValue();

		boolean changed = false;

		if (old == null && value == null) {
			return;
		}

		if (old == null && value != null) {
			changed = true;
		}

		if (old != null && value == null) {
			changed = true;
		}

		if (!changed) {
			changed = !old.equals(value);
		}

		if (changed) {
			ChangePropertyOperation<? extends Object> op = new ChangePropertyOperation<>(prop.getNodeId(),
					prop.getName(), value);
			IOperationHistory history = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			try {
				CanvasEditor editor = (CanvasEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getActiveEditor();
				history.execute(op, null, editor);
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		// if (element instanceof PGridNumberProperty) {
		// ((PGridNumberProperty) element).setUndoableValue((Double) value);
		// } else if (element instanceof PGridStringProperty) {
		// ((PGridStringProperty) element).setUndoableValue((String) value);
		// } else if (element instanceof PGridBooleanProperty) {
		// ((PGridBooleanProperty) element).setValue((Boolean) value);
		// } else if (element instanceof PGridFrameProperty) {
		// ((PGridFrameProperty) element).setUndoableValue((IAssetFrameModel)
		// value);
		// } else if (element instanceof PGridColorProperty) {
		// ((PGridColorProperty) element).setUndoableValue((RGB) value);
		// }

		getViewer().refresh(element);
	}
}
