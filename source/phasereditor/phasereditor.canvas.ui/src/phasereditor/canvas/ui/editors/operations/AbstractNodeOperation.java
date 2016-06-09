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

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.BaseObjectControl;

/**
 * @author arian
 *
 */
public abstract class AbstractNodeOperation extends AbstractOperation {

	protected String _controlId;

	public AbstractNodeOperation(String label, String controlId) {
		super(label);
		_controlId = controlId;
	}

	protected BaseObjectControl<?> findControl(IAdaptable info) {
		return findControl(info, _controlId);
	}

	protected static BaseObjectControl<?> findControl(IAdaptable info, String id) {
		CanvasEditor editor = info.getAdapter(CanvasEditor.class);
		return editor.getCanvas().getWorldNode().getControl().findById(id);
	}
}
