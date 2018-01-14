// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.canvas.ui.editors.grid.PGridBitmapTextFontProperty;

/**
 * @author arian
 *
 */
public class BitmapTextFontCellEditor extends DialogCellEditor {

	private PGridBitmapTextFontProperty _prop;

	public BitmapTextFontCellEditor(Composite parent, PGridBitmapTextFontProperty prop) {
		super(parent);
		_prop = prop;
	}

	@Override
	protected void updateContents(Object value) {
		super.updateContents(value);

		if (value == null) {
			getDefaultLabel().setText("");
			return;
		}

		getDefaultLabel().setText(" " + ((BitmapFontAssetModel) value).getKey());
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		BitmapTextFontDialog dlg = new BitmapTextFontDialog(cellEditorWindow.getShell());

		dlg.setProject(_prop.getValue().getPack().getFile().getProject());
		
		dlg.setSelectedFont(_prop.getValue());
		
		dlg.setInitialText(_prop.getModel().getText());
		
		if (dlg.open() == Window.OK) {
			return dlg.getSelectedFont();
		}

		return null;
	}
}
