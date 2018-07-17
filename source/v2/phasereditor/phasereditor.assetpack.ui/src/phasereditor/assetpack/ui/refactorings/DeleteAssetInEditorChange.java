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
package phasereditor.assetpack.ui.refactorings;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;

/**
 * @author arian
 *
 */
public class DeleteAssetInEditorChange extends Change {

	private AssetModel _asset;
	private int _index;
	private AssetPackEditor _editor;

	public DeleteAssetInEditorChange(AssetModel asset, AssetPackEditor editor) {
		_asset = asset;
		_editor = editor;
	}

	@Override
	public String getName() {
		return "Delete asset pack entry '" + _asset.getKey() + "'.";
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		// nothing
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		if (_editor != null) {

			boolean visible = isEditorVisible();

			if (!visible) {
				status.addError("The asset pack editor is not open.");
			}
		}

		return status;
	}

	private boolean isEditorVisible() {
		return _editor.getEditorSite().getWorkbenchWindow().getActivePage().isPartVisible(_editor);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		AssetSectionModel section = _asset.getPack().findSection(_asset.getSection().getKey());
		_index = section.getAssets().indexOf(_asset);

		boolean[] reveal = { false };

		Display.getDefault().syncExec(() -> {
			TreeViewer viewer = _editor.getAssetsComp().getViewer();
			List<Object> expanded = Arrays.asList(viewer.getExpandedElements());
			reveal[0] = expanded.contains(_asset.getGroup());
			section.removeAsset(_asset, true);
			_editor.refresh();
		});

		return new AddAssetInEditorChange(_asset, reveal[0], _index, _editor);
	}

	@Override
	public Object getModifiedElement() {
		return _asset;
	}

}
