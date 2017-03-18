// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.OutlineContentProvider;
import phasereditor.canvas.ui.editors.OutlineLabelProvider;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class SpriteCellEditor extends DialogCellEditor {
	 static int NONE_CODE = IDialogConstants.CLIENT_ID + 1;
	private ObjectCanvas _canvas;
	private String _current;

	public SpriteCellEditor(Composite parent, ObjectCanvas canvas, String current) {
		super(parent);
		_canvas = canvas;
		_current = current;
	}

	@Override
	protected Object openDialogBox(Control window) {

		ElementTreeSelectionDialog dlg = new ElementTreeSelectionDialog(window.getShell(), new OutlineLabelProvider(),
				new OutlineContentProvider(false)) {

					@Override
					protected void buttonPressed(int buttonId) {
						if (buttonId == NONE_CODE) {
							setReturnCode(NONE_CODE);
							close();
						}
						super.buttonPressed(buttonId);
					}
			
			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				createButton(parent, IDialogConstants.CLIENT_ID + 1, "None", false);
				super.createButtonsForButtonBar(parent);
			}
		};
		dlg.setTitle("Select Object");
		dlg.setInput(_canvas);

		if (_current != null) {
			BaseObjectModel sel = _canvas.getWorldModel().findById(_current);
			if (sel != null) {
				dlg.setInitialSelection(sel);
			}
		}

		int code = dlg.open();
		if (code == Window.OK) {
			IObjectNode result = (IObjectNode) dlg.getFirstResult();
			if (result != null) {
				return result.getModel().getId();
			}
		} else if (code == NONE_CODE) {
			return "";
		}

		return _current;
	}

	@Override
	protected void updateContents(Object value) {
		String id = (String) value;
		if (id == null) {
			super.updateContents("");
		} else {
			BaseObjectModel model = _canvas.getWorldModel().findById(id);

			if (model == null) {
				super.updateContents(id);
			} else {
				super.updateContents(model.getEditorName());
			}
		}
	}

}
