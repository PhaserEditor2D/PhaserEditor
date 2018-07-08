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
package phasereditor.ui.properties;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import phasereditor.ui.ComboBoxViewerCellEditor2;

/**
 * @author arian
 *
 */
public class PGridEditingSupport extends EditingSupport {

	private boolean _supportUndoRedo;
	private Runnable _onChanged;

	public PGridEditingSupport(ColumnViewer viewer, boolean supportUndoRedo) {
		super(viewer);
		_supportUndoRedo = supportUndoRedo;
	}

	public void setOnChanged(Runnable onChanged) {
		_onChanged = onChanged;
	}

	public Runnable getOnChanged() {
		return _onChanged;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected CellEditor getCellEditor(Object element) {
		Composite parent = (Composite) getViewer().getControl();

		if (element instanceof PGridProperty) {
			CellEditor editor = ((PGridProperty) element).createCellEditor(parent, element);
			if (editor != null) {
				return editor;
			}
		}

		if (element instanceof PGridNumberProperty) {
			return createNumberEditor(parent);
		} else if (element instanceof PGridStringProperty) {
			return createTextEditor(element, parent);
		} else if (element instanceof PGridBooleanProperty) {
			return createBooleanEditor(parent);
		} else if (element instanceof PGridColorProperty) {
			return createRGBEditor(element, parent);
		} else if (element instanceof PGridEnumProperty) {
			return createEnumEditor(element, parent);
		}

		return null;
	}

	@SuppressWarnings("static-method")
	protected CellEditor createEnumEditor(Object element, Composite parent) {
		ComboBoxViewerCellEditor2 editor = new ComboBoxViewerCellEditor2(parent, SWT.READ_ONLY);
		editor.setContentProvider(new ArrayContentProvider());
		editor.setInput(((PGridEnumProperty<?>) element).getValues());
		editor.setLabelProvider(new LabelProvider());
		return editor;
	}

	private static CellEditor createRGBEditor(Object element, Composite parent) {
		return new RGBCellEditor(parent, ((PGridColorProperty) element).getDefaultRGB());
	}

	private static CheckboxCellEditor createBooleanEditor(Composite parent) {
		return new CheckboxCellEditor(parent);
	}

	private static NumberCellEditor createNumberEditor(Composite parent) {
		return new NumberCellEditor(parent);
	}

	private static CellEditor createTextEditor(Object element, Composite parent) {
		PGridStringProperty prop = (PGridStringProperty) element;
		if (prop.isLongText()) {
			return new DialogCellEditor(parent) {

				@Override
				protected Object openDialogBox(Control cellEditorWindow) {
					Shell shell = cellEditorWindow.getShell();
					return openLongStringDialog(prop, shell);
				}
			};
		}
		return new TextCellEditor(parent);
	}

	public static String openLongStringDialog(PGridStringProperty prop, Shell shell) {
		TextDialog dlg = new TextDialog(shell);
		dlg.setInitialText(prop.getValue());
		dlg.setTitle(prop.getName());
		dlg.setMessage(prop.getMessage());
		if (dlg.open() == Window.OK) {
			return dlg.getResult();
		}
		return null;
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof PGridSection) {
			return false;
		} else if (element instanceof PGridProperty) {
			PGridProperty<?> prop = (PGridProperty<?>) element;
			boolean readonly = prop.isReadOnly();
			return !readonly;
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

	@SuppressWarnings({ "null", "rawtypes", "unchecked" })
	@Override
	protected void setValue(Object element, Object value) {
		PGridProperty prop = (PGridProperty) element;

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
			if (_supportUndoRedo) {

				executeChangePropertyValueOperation(value, prop);

			} else {
				prop.setValue(value, true);
				if (_onChanged != null) {
					_onChanged.run();
				}
			}
		}

		getViewer().refresh(element);
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	protected void executeChangePropertyValueOperation(Object value, PGridProperty prop) {
		// nothing in the default implementation
	}

}
