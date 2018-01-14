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

import static java.lang.System.out;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import phasereditor.canvas.ui.Activator;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridProperty;
import phasereditor.canvas.ui.shapes.BaseObjectControl;

/**
 * @author arian
 *
 */
public class ChangePropertyOperation<T> extends AbstractNodeOperation {
	private String _propId;
	private T _value;
	private T _undoValue;
	private boolean _notify;
	private PGridProperty<T> _property;

	/**
	 * Just use in case the property does not belong to a Canvas Object.
	 * 
	 * @param property
	 * @param value
	 * @param notify
	 */
	public ChangePropertyOperation(PGridProperty<T> property, T value, boolean notify) {
		super("ChangePropertyOperation", null);
		_property = property;
		_propId = property.getName();
		_value = value;
		_notify = notify;
	}

	public ChangePropertyOperation(String controlId, String propId, T value, boolean notify) {
		super("ChangePropertyOperation", controlId);
		_propId = propId;
		_value = value;
		_notify = notify;
	}

	public ChangePropertyOperation(String controlId, String propId, T value) {
		this(controlId, propId, value, true);
	}

	private IStatus setValue(T value, IAdaptable info) {
		try {
			out.println("setting value " + value);
			PGridProperty<T> prop = findProperty(info);
			_undoValue = prop.getValue();
			out.println("undo value " + _undoValue + " " + isAttachedToControl());
			prop.setValue(value, _notify);

			if (isAttachedToControl()) {
				BaseObjectControl<?> control = findControl(info);

				control.updateFromModel();

				CanvasEditor editor = info.getAdapter(CanvasEditor.class);

				editor.getCanvas().getUpdateBehavior().update_Grid_from_PropertyChange(prop);
				editor.getCanvas().getSelectionBehavior().updateSelectedNodes();
			}

		} catch (IllegalStateException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
		}
		return Status.OK_STATUS;
	}
	
	private boolean isAttachedToControl() {
		return _property == null;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
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

	@SuppressWarnings("unchecked")
	private PGridProperty<T> findProperty(IAdaptable info) {
		PGridProperty<T> prop;

		if (_property != null) {
			prop = _property;
		} else {

			BaseObjectControl<?> control = findControl(info);

			if (control == null) {
				throw new IllegalStateException("Cannot find control " + _controlId);
			}

			PGridModel propModel = control.getPropertyModel();
			prop = (PGridProperty<T>) propModel.findById(_propId);
		}

		if (prop == null) {
			throw new IllegalStateException("Cannot find property " + _propId);
		}
		return prop;
	}
}
