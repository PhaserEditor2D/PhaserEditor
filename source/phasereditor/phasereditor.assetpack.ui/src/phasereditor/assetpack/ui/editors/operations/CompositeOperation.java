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
package phasereditor.assetpack.ui.editors.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;

import phasereditor.assetpack.ui.editors.AssetPackEditor;

/**
 * @author arian
 *
 */
public class CompositeOperation extends AssetPackOperation {

	private List<AssetPackOperation> _operations;

	/**
	 * @param label
	 */
	public CompositeOperation() {
		super("CompositeOperation");
		_operations = new ArrayList<>();
	}

	public void add(AssetPackOperation op) {
		_operations.add(op);
	}

	public boolean isEmpty() {
		return _operations.isEmpty();
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		AssetPackEditor editor = getEditor(info);
		TreeViewer viewer = editor.getViewer();
		Tree tree = viewer.getTree();
		tree.setRedraw(false);

		for (AssetPackOperation op : _operations) {
			op.execute(monitor, info);
		}

		viewer.refresh();
		tree.setRedraw(true);

		editor.getModel().setDirty(true);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		AssetPackEditor editor = getEditor(info);
		TreeViewer viewer = editor.getViewer();
		Tree tree = viewer.getTree();
		tree.setRedraw(false);

		for (AssetPackOperation op : _operations) {
			op.redo(monitor, info);
		}

		viewer.refresh();
		tree.setRedraw(true);

		editor.getModel().setDirty(true);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		AssetPackEditor editor = getEditor(info);
		TreeViewer viewer = editor.getViewer();
		Tree tree = viewer.getTree();
		tree.setRedraw(false);

		for (int i = _operations.size() - 1; i >= 0; i--) {
			AssetPackOperation op = _operations.get(i);
			op.undo(monitor, info);
		}

		viewer.refresh();
		tree.setRedraw(true);

		editor.getModel().setDirty(true);

		return Status.OK_STATUS;
	}

}
