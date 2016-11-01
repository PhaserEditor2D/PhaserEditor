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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;

/**
 * @author arian
 *
 */
public class RemoveAssetOperation extends AssetPackOperation {

	private AssetModel _asset;
	private int _index;
	private boolean _reveal;

	/**
	 * @param label
	 */
	public RemoveAssetOperation(AssetModel asset) {
		super("RemoveAssetOperation");
		_asset = asset;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		_index = _asset.getSection().getAssets().indexOf(_asset);
		List<Object> expanded = Arrays.asList(getEditor(info).getViewer().getExpandedElements());
		_reveal = expanded.contains(_asset.getGroup());
		_asset.getSection().removeAsset(_asset);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return execute(monitor, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		_asset.getSection().addAsset(_index, _asset, false);
		if (_reveal) {
			AssetPackEditor editor = getEditor(info);
			editor.refresh();
			editor.revealElement(_asset);
		}
		return Status.OK_STATUS;
	}

}
