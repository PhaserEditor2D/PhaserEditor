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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import phasereditor.canvas.core.PhysicsSortDirection;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.NumberCellEditor;
import phasereditor.canvas.ui.editors.grid.PGridAnimationsProperty;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridOverrideProperty;
import phasereditor.canvas.ui.editors.grid.PGridProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;

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

	@Override
	protected CellEditor getCellEditor(Object element) {
		Composite parent = (Composite) getViewer().getControl();

		if (element instanceof PGridNumberProperty) {
			return new NumberCellEditor(parent);
		} else if (element instanceof PGridStringProperty) {
			PGridStringProperty longStrProp = (PGridStringProperty) element;
			if (longStrProp.isLongText()) {
				return new DialogCellEditor(parent) {

					@Override
					protected Object openDialogBox(Control cellEditorWindow) {
						TextDialog dlg = new TextDialog(cellEditorWindow.getShell());
						dlg.setInitialText(longStrProp.getValue());
						dlg.setTitle("data");
						dlg.setMessage("Write a valid JSON string. It will be verbatim generated.");
						if (dlg.open() == Window.OK) {
							return dlg.getResult();
						}
						return null;
					}
				};
			}
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
		} else if (element instanceof PGridEnumProperty) {
			ComboBoxViewerCellEditor editor = new ComboBoxViewerCellEditor(parent, SWT.READ_ONLY);
			editor.setContentProvider(new ArrayContentProvider());
			editor.setInput(((PGridEnumProperty<?>) element).getValues());
			editor.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object obj) {
					if (obj instanceof PhysicsType) {
						return ((PhysicsType) obj).getPhaserName();
					}

					if (obj instanceof PhysicsSortDirection) {
						return ((PhysicsSortDirection) obj).getPhaserName();
					}
					return super.getText(obj);
				}
			});
			return editor;
		} else if (element instanceof PGridOverrideProperty) {
			PGridOverrideProperty prop = (PGridOverrideProperty) element;
			return new DialogCellEditor(parent) {

				@Override
				protected Object openDialogBox(Control cellEditorWindow) {
					ListSelectionDialog dlg = new ListSelectionDialog(cellEditorWindow.getShell(),
							prop.getValidProperties(), new ArrayContentProvider(), new LabelProvider(),
							"Select the properties to override in this prefab instance:");
					List<String> initialValue = prop.getValue();
					dlg.setInitialSelections(initialValue.toArray());
					dlg.setTitle("Prefab Override");
					if (dlg.open() == Window.OK) {
						Object[] result = dlg.getResult();
						return new ArrayList<>(Arrays.asList(result));
					}

					return initialValue;
				}
			};
		}

		return null;
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof PGridSection) {
			return false;
		} else if (element instanceof PGridProperty) {
			return !((PGridProperty<?>) element).isReadOnly();
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

				ChangePropertyOperation<? extends Object> op = new ChangePropertyOperation<>(prop.getNodeId(),
						prop.getName(), value);
				CanvasEditor editor = (CanvasEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getActiveEditor();
				editor.getCanvas().getUpdateBehavior().executeOperations(new CompositeOperation(op));

			} else {
				prop.setValue(value, true);
				if (_onChanged != null) {
					_onChanged.run();
				}
			}
		}

		getViewer().refresh(element);
	}
}
