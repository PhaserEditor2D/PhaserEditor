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
public class RenameAssetInEditorChange extends Change {

	private AssetModel _asset;
	private AssetSectionModel _section;
	private final String _oldName;
	private final String _newName;
	private final Object _element;
	private final AssetPackEditor _editor;

	public RenameAssetInEditorChange(Object element, String newName, AssetPackEditor editor) {
		_element = element;
		_newName = newName;
		_editor = editor;

		if (element instanceof AssetModel) {
			_asset = (AssetModel) element;
			_oldName = _asset.getKey();
		} else {
			_section = (AssetSectionModel) element;
			_oldName = _section.getKey();
		}
	}

	@Override
	public String getName() {
		if (_asset != null) {
			return "Rename asset entry '" + _asset.getKey() + "'";
		}
		return "Rename section '" + _section.getKey() + "'";
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		// nothing
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		if (_editor != null) {
			
			boolean visible = _editor.getEditorSite().getWorkbenchWindow().getActivePage().isPartVisible(_editor);
			
			if (!visible) {
				status.addFatalError("The editor is not open.");
			}
		}

		return status;
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		Display.getDefault().syncExec(() -> {

			if (_asset == null) {
				renameSection();
			} else {
				renameAsset();
			}
		});

		return new RenameAssetInEditorChange(_element, _oldName, _editor);
	}

	private void renameSection() {
		AssetSectionModel section = _editor.getModel().findSection(_oldName);
		if (section != null) {
			section.setKey(_newName, true);
			TreeViewer viewer = _editor.getViewer();
			viewer.refresh();
			_editor.updateAssetEditor();
		}
	}

	private void renameAsset() {
		AssetSectionModel section = _editor.getModel().findSection(_asset.getSection().getKey());
		if (section != null) {
			AssetModel asset = section.findAsset(_oldName);
			if (asset != null) {
				asset.setKey(_newName, true);
				TreeViewer viewer = _editor.getViewer();
				viewer.refresh();
				_editor.updateAssetEditor();
			}
		}
	}

	@Override
	public Object getModifiedElement() {
		return _element;
	}

}
