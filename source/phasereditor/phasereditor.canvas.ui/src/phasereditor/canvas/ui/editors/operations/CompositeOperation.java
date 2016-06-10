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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;

import phasereditor.canvas.ui.editors.CanvasEditor;

/**
 * @author arian
 *
 */
public class CompositeOperation extends AbstractOperation {

	private List<IUndoableOperation> _operations;

	public CompositeOperation(IUndoableOperation... operations) {
		super("CompositeChangePropertyOperation");
		_operations = new ArrayList<>(Arrays.asList(operations));
		addContext(CanvasEditor.UNDO_CONTEXT);
	}

	public boolean isEmpty() {
		return _operations.isEmpty();
	}

	public int getSize() {
		return _operations.size();
	}

	public void add(IUndoableOperation operation) {
		_operations.add(operation);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		IStatus status = Status.OK_STATUS;

		if (getSize() > 2) {
			try {
				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(false, false, monitor2 -> {
					try {
						monitor2.beginTask("Undo operation", getSize());

						for (IUndoableOperation op : _operations) {
							op.execute(monitor, info);
							monitor2.worked(1);
						}

					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					monitor2.done();
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			for (IUndoableOperation op : _operations) {
				op.execute(monitor, info);
			}
		}

		return status;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		IStatus status = Status.OK_STATUS;

		if (getSize() > 2) {
			try {
				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(false, false, monitor2 -> {
					try {
						monitor2.beginTask("Undo operation", getSize());
						for (IUndoableOperation op : _operations) {
							op.redo(monitor, info);
							monitor2.worked(1);
						}
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					monitor2.done();
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			for (int i = _operations.size() - 1; i >= 0; i--) {
				IUndoableOperation op = _operations.get(i);
				status = op.redo(monitor, info);
			}
		}

		return status;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		IStatus status = Status.OK_STATUS;
		if (getSize() > 2) {
			try {
				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(false, false, monitor2 -> {
					try {
						monitor2.beginTask("Undo operation", getSize());
						for (int i = _operations.size() - 1; i >= 0; i--) {
							IUndoableOperation op = _operations.get(i);
							op.undo(monitor2, info);
							monitor2.worked(1);
						}
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					monitor2.done();
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			for (int i = _operations.size() - 1; i >= 0; i--) {
				IUndoableOperation op = _operations.get(i);
				status = op.undo(monitor, info);
			}
		}

		return status;
	}

}
