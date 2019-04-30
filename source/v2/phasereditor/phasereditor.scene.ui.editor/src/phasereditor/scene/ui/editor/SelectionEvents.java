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
package phasereditor.scene.ui.editor;

import java.util.ArrayList;

import phasereditor.scene.core.ObjectModel;

/**
 * @author arian
 *
 */
public class SelectionEvents {

	private SceneEditor _editor;

	public SelectionEvents(SceneEditor editor) {
		super();
		_editor = editor;
	}

	public void updateSelection(ObjectModel clickedObject, boolean controlPressed) {
		
		var fireUpdateSelection = false;

		var list = new ArrayList<>(_editor.getSelectionList());

		if (clickedObject == null) {
			fireUpdateSelection = !list.isEmpty();
			list = new ArrayList<>();
		} else {
			if (controlPressed) {
				if (list.contains(clickedObject)) {
					list.remove(clickedObject);
				} else {
					list.add(clickedObject);
				}
			} else {
				list = new ArrayList<>();
				list.add(clickedObject);
			}

			fireUpdateSelection = true;
		}

		if (fireUpdateSelection) {
			_editor.setSelection(list);
		}
	}
}
