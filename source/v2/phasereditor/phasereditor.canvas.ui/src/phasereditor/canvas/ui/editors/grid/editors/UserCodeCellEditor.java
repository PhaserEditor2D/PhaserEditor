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

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.canvas.core.EditorSettings_UserCode;

/**
 * @author arian
 *
 */
public class UserCodeCellEditor extends DialogCellEditor {

	private EditorSettings_UserCode _userCode;

	public UserCodeCellEditor(Composite parent, EditorSettings_UserCode userCode) {
		super(parent);
		_userCode = userCode;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {

		UserCodeDialog dlg = new UserCodeDialog(cellEditorWindow.getShell());

		EditorSettings_UserCode copy = _userCode.copy();
		dlg.setUserCode(copy);

		if (dlg.open() == Window.OK) {
			if (!copy.toJSON().toString().equals(_userCode.toJSON().toString())) {
				return copy;
			}
		}
		return _userCode;
	}

}
