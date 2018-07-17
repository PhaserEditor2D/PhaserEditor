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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;

/**
 * @author arian
 *
 */
public class MoveAssetInEditorChange extends Change {

	private AssetMoveList _list;
	private AssetPackEditor _editor;

	public MoveAssetInEditorChange(AssetMoveList list, AssetPackEditor editor) {
		_list = list;
		_editor = editor;
	}

	@Override
	public String getName() {
		return "Move assets";
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		// nothing
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {

		Display.getDefault().syncExec(() -> {

			AssetPackModel pack = _editor.getModel();
			AssetModel asset = null;

			for (int i = 0; i < _list.size(); i++) {
				AssetSectionModel srcSection = pack.findSection(_list.getInitialSectionName(i));
				AssetSectionModel dstSection = pack.findSection(_list.getDestinySectionName(i));
				asset = srcSection.findAsset(_list.getAssetName(i));
				srcSection.removeAsset(asset, false);
				dstSection.addAsset(asset, false);
			}

			_editor.refresh();
			_editor.updateAssetEditor();
			_editor.revealElement(asset);
		});

		return new MoveAssetInEditorChange(_list.reverse(), _editor);
	}

	@Override
	public Object getModifiedElement() {
		return _list.getPackFile();
	}

}
