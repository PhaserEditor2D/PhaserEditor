// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.assetpack.ui.editor.undo;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.assetpack.ui.editor.AssetPackUIEditor;

/**
 * @author arian
 *
 */
public class GlobalOperation extends AbstractOperation {

	private JSONObject _before;
	private JSONObject _after;

	public GlobalOperation(String label, JSONObject before, JSONObject after) {
		super(label);
		_before = before;
		_after = after;
	}

	public static JSONObject readState(AssetPackEditor editor) {
		var state = new JSONObject();

		var data = editor.getModel().toJSON();
		var selection = (IStructuredSelection) editor.getSite().getSelectionProvider().getSelection();
		var selKeys = Arrays.stream(selection.toArray())

				.filter(o -> o instanceof AssetModel)

				.map(o -> ((AssetModel) o).getKey())

				.toArray();

		state.put("data", data);
		state.put("selKeys", selKeys);

		return state;
	}

	private static void loadState(JSONObject state, IAdaptable info) {
		var editor = info.getAdapter(AssetPackEditor.class);
		var model = editor.getModel();

		// var selectionIds = editor.getSelectionIdList();
		//
		try {
			var data = state.getJSONObject("data");
			model.read(data);
			model.build();

			var selKeys = (Object[]) state.get("selKeys");
			var section = model.getSections().get(0);
			var sel = Arrays.stream(selKeys)

					.map(o -> section.findAsset((String) o))

					.filter(a -> a != null)

					.toArray();

			editor.getSite().getSelectionProvider().setSelection(new StructuredSelection(sel));
		} catch (Exception e) {
			AssetPackUIEditor.logError(e);
			throw new RuntimeException(e);
		}

		editor.refresh();

		//
		// editor.refreshOutline_basedOnId();
		//
		// editor.setSelectionFromIdList(selectionIds);
		//
		editor.setDirty(true);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		var editor = info.getAdapter(AssetPackEditor.class);

		editor.getModel().build();

		editor.refresh();

		editor.setDirty(true);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		loadState(_after, info);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		loadState(_before, info);
		return Status.OK_STATUS;
	}
}
