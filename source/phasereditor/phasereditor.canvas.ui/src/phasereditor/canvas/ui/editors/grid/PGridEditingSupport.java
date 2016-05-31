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
package phasereditor.canvas.ui.editors.grid;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import phasereditor.canvas.ui.editors.grid.editors.FrameCellEditor;
import phasereditor.canvas.ui.editors.grid.editors.RGBCellEditor;

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
			return new FrameCellEditor(parent, prop.getControl());
		} else if (element instanceof PGridColorProperty) {
			return new RGBCellEditor(parent, ((PGridColorProperty) element).getDefaultRGB());
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

	@Override
	protected void setValue(Object element, Object value) {
		if (element instanceof PGridNumberProperty) {
			((PGridNumberProperty) element).setValue((Double) value);
		} else if (element instanceof PGridStringProperty) {
			((PGridStringProperty) element).setValue((String) value);
		} else if (element instanceof PGridBooleanProperty) {
			((PGridBooleanProperty) element).setValue((Boolean) value);
		} else if (element instanceof PGridFrameProperty) {
			((PGridFrameProperty) element).setValue((Integer) value);
		} else if (element instanceof PGridColorProperty) {
			((PGridColorProperty) element).setValue((RGB) value);
		}

		getViewer().refresh(element);
	}
}
