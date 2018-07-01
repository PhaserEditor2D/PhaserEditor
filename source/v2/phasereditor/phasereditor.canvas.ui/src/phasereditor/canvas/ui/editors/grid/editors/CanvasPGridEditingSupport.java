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
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import phasereditor.canvas.core.GroupModel.SetAllData;
import phasereditor.canvas.core.PhysicsSortDirection;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.CanvasNumberCellEditor;
import phasereditor.canvas.ui.editors.grid.PGridAnimationsProperty;
import phasereditor.canvas.ui.editors.grid.PGridBitmapTextFontProperty;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridLoadPackProperty;
import phasereditor.canvas.ui.editors.grid.PGridOverrideProperty;
import phasereditor.canvas.ui.editors.grid.PGridSetAllProperty;
import phasereditor.canvas.ui.editors.grid.PGridSpriteProperty;
import phasereditor.canvas.ui.editors.grid.PGridTilemapIndexesProperty;
import phasereditor.canvas.ui.editors.grid.PGridUserCodeProperty;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.project.core.codegen.SourceLang;
import phasereditor.ui.ComboBoxViewerCellEditor2;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.properties.PGridEditingSupport;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridProperty;
import phasereditor.ui.properties.PGridStringProperty;
import phasereditor.ui.properties.TextDialog;

/**
 * @author arian
 *
 */
public class CanvasPGridEditingSupport extends PGridEditingSupport {

	private ObjectCanvas _canvas;

	public CanvasPGridEditingSupport(ColumnViewer viewer, boolean supportUndoRedo) {
		super(viewer, supportUndoRedo);
	}

	public void setCanvas(ObjectCanvas canvas) {
		_canvas = canvas;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		Composite parent = (Composite) getViewer().getControl();

		if (element instanceof PGridNumberProperty) {
			return createCanvasNumberEditor(parent);
		} else if (element instanceof PGridSpriteProperty) {
			return createSpriteEditor(element, parent);
		} else if (element instanceof PGridFrameProperty) {
			return createTextureFrameEditor(element, parent);
		} else if (element instanceof PGridAnimationsProperty) {
			return createAnimationsEditor(element, parent);
		} else if (element instanceof PGridOverrideProperty) {
			return createOverrideListEditor(element, parent);
		} else if (element instanceof PGridUserCodeProperty) {
			return createUserCodeEditor(element, parent);
		} else if (element instanceof PGridLoadPackProperty) {
			return createLoadPackEditor(element, parent);
		} else if (element instanceof PGridSetAllProperty) {
			return createSetAllPropertyEditor(element, parent);
		} else if (element instanceof PGridBitmapTextFontProperty) {
			return createBitmapTextFontEditor(element, parent);
		} else if (element instanceof PGridTilemapIndexesProperty) {
			return createTilemapCollisionIndexesEditor(element, parent);
		}

		return super.getCellEditor(element);
	}

	private static CellEditor createTilemapCollisionIndexesEditor(Object element, Composite parent) {
		PGridTilemapIndexesProperty prop = (PGridTilemapIndexesProperty) element;
		return new DialogCellEditor(parent) {

			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				Shell shell = cellEditorWindow.getShell();
				return openTilemapIndexesDialog(prop, shell);
			}
		};
	}

	private static CellEditor createBitmapTextFontEditor(Object element, Composite parent) {
		return new BitmapTextFontCellEditor(parent, (PGridBitmapTextFontProperty) element);
	}

	private static CellEditor createSetAllPropertyEditor(Object element, Composite parent) {
		PGridSetAllProperty prop = (PGridSetAllProperty) element;
		return new DialogCellEditor(parent) {

			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				SetAllData initialValue = prop.getValue();
				SetAllDialog dlg = new SetAllDialog(cellEditorWindow.getShell());
				dlg.setSetAllData(initialValue);
				if (dlg.open() == Window.OK) {
					return dlg.getResult();
				}

				return initialValue;
			}
		};
	}

	private CellEditor createLoadPackEditor(Object element, Composite parent) {
		return new LoadPackCellEditor(parent, _canvas.getWorldModel().getProject(),
				((PGridLoadPackProperty) element).getValue());
	}

	private static CellEditor createUserCodeEditor(Object element, Composite parent) {
		PGridUserCodeProperty prop = (PGridUserCodeProperty) element;
		return new UserCodeCellEditor(parent, prop.getValue());
	}

	private static CellEditor createOverrideListEditor(Object element, Composite parent) {
		PGridOverrideProperty prop = (PGridOverrideProperty) element;
		return new DialogCellEditor(parent) {

			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				return openOverridePropertiesDialog(prop, cellEditorWindow.getShell());
			}
		};
	}

	@Override
	protected CellEditor createEnumEditor(Object element, Composite parent) {
		CellEditor editor = super.createEnumEditor(element, parent);
		((ComboBoxViewerCellEditor2) editor).setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object obj) {
				if (obj instanceof PhysicsType) {
					return ((PhysicsType) obj).getPhaserName();
				}

				if (obj instanceof PhysicsSortDirection) {
					return ((PhysicsSortDirection) obj).getPhaserName();
				}

				if (obj instanceof SourceLang) {
					return ((SourceLang) obj).getDisplayName();
				}

				return super.getText(obj);
			}
		});
		return editor;
	}

	private static CellEditor createAnimationsEditor(Object element, Composite parent) {
		return new AnimationsCellEditor(parent, (PGridAnimationsProperty) element);
	}

	private static CellEditor createTextureFrameEditor(Object element, Composite parent) {
		PGridFrameProperty prop = (PGridFrameProperty) element;
		return new FrameCellEditor(parent, prop);
	}

	private static CanvasNumberCellEditor createCanvasNumberEditor(Composite parent) {
		return new CanvasNumberCellEditor(parent);
	}

	private SpriteCellEditor createSpriteEditor(Object element, Composite parent) {
		return new SpriteCellEditor(parent, _canvas, ((PGridSpriteProperty) element).getValue());
	}

	public static Object openSelectFrameDialog(PGridFrameProperty prop, Shell shell) {
		FrameDialog dlg = new FrameDialog(shell);
		dlg.setAllowNull(prop.isAllowNull());
		dlg.setFrames(prop.getFrames());
		dlg.setSelectedItem(prop.getValue());
		if (dlg.open() == Window.OK) {
			return dlg.getResult();
		}
		return null;
	}

	public static List<Integer> openTilemapIndexesDialog(PGridTilemapIndexesProperty prop, Shell shell) {
		SelectTilemapIndexesDialog dlg = new SelectTilemapIndexesDialog(shell);
		dlg.setModel(prop.getSpriteModel());

		if (dlg.open() == Window.OK) {
			return dlg.getSelection();
		}

		return null;
	}

	public static Object openOverridePropertiesDialog(PGridOverrideProperty prop, Shell shell) {
		Object input = prop.getValidProperties();
		IStructuredContentProvider contentProvider = new ArrayContentProvider();
		ILabelProvider labelProvider = new LabelProvider();
		String message = "Select the properties to override in this prefab instance:";
		ListSelectionDialog dlg = PhaserEditorUI.createFilteredListSelectionDialog(shell, input, contentProvider,
				labelProvider, message);
		List<String> initialValue = prop.getValue();
		dlg.setInitialElementSelections(initialValue);
		dlg.setTitle("Prefab Override");
		if (dlg.open() == Window.OK) {
			Object[] result = dlg.getResult();
			return new ArrayList<>(Arrays.asList(result));
		}

		return initialValue;
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

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void executeChangePropertyValueOperation(Object value, PGridProperty prop) {
		ChangePropertyOperation<? extends Object> op = makeChangePropertyValueOperation(value, prop);
		_canvas.getUpdateBehavior().executeOperations(new CompositeOperation(op));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ChangePropertyOperation<? extends Object> makeChangePropertyValueOperation(Object value,
			PGridProperty prop) {
		ChangePropertyOperation<? extends Object> op;
		if (prop.getNodeId() == null) {
			op = new ChangePropertyOperation<>(prop, value, true);
		} else {
			op = new ChangePropertyOperation<>(prop.getNodeId(), prop.getName(), value);
		}
		return op;
	}
}
