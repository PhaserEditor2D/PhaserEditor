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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import phasereditor.canvas.core.BodyModel;
import phasereditor.canvas.ui.Activator;
import phasereditor.canvas.ui.shapes.BaseSpriteControl;

/**
 * @author arian
 *
 */
public class ChangeBodyOperation extends AbstractNodeOperation {
	private BodyModel _value;
	private BodyModel _undoValue;

	public ChangeBodyOperation(String controlId, BodyModel value) {
		super("ChangePropertyOperation", controlId);
		_value = value;
	}

	private IStatus setValue(BodyModel value, IAdaptable info) {
		try {
			BaseSpriteControl<?> control = (BaseSpriteControl<?>) findControl(info);
			_undoValue = control.getModel().getBody();
			control.getModel().setBody(value);
			
			// the body does not affect sprite appearance
			// control.updateFromModel();
		} catch (IllegalStateException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseSpriteControl<?> control = (BaseSpriteControl<?>) findControl(info);
		_undoValue = control.getModel().getBody();
		return setValue(_value, info);
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return setValue(_value, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return setValue(_undoValue, info);
	}
}
