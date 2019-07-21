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

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.ui.editor.AssetPackEditor;

/**
 * @author arian
 *
 */
public class AssetsOperation extends AbstractOperation {

	public static class AssetState {
		JSONObject state;
		private int index;

		public AssetState(AssetModel asset) {
			super();
			this.index = asset.getSection().getAssets().indexOf(asset);
			this.state = asset.toJSON();
		}

	}

	public static List<AssetState> readState(List<AssetModel> assets) {
		return assets.stream().map(a -> new AssetState(a)).collect(toList());
	}

	private List<AssetState> _before;
	private List<AssetState> _after;

	public AssetsOperation(String label, List<AssetState> before, List<AssetState> after) {
		super(label);
		_before = before;
		_after = after;
	}

	private static IStatus readState(List<AssetState> state, IAdaptable info) {

		var editor = info.getAdapter(AssetPackEditor.class);
		var section = editor.getModel().getSections().get(0);
		for (var assetState : state) {
			var asset = section.getAssets().get(assetState.index);
			asset.setKey(assetState.state.getString("key"));
			asset.readInfo(assetState.state);
			asset.build(null);
		}

		editor.refresh();
		editor.updatePropertyPages();

		editor.setDirty(true);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		var editor = info.getAdapter(AssetPackEditor.class);

		editor.setDirty(true);

		editor.refresh();
		editor.updatePropertyPages();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return readState(_after, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return readState(_before, info);
	}

}
