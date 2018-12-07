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
package phasereditor.assetpack.ui.editor.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editor.AssetPackEditor;

/**
 * @author arian
 *
 */
public class DeleteAssetSectionInEditorChange extends Change {

	private int _index;
	private AssetPackEditor _editor;
	private boolean _delete;
	private String _sectionName;

	public DeleteAssetSectionInEditorChange(String sectionName, AssetPackEditor editor) {
		this(sectionName, editor, true, 0);
	}

	private DeleteAssetSectionInEditorChange(String sectionName, AssetPackEditor editor, boolean delete, int index) {
		_sectionName = sectionName;
		_editor = editor;
		_delete = delete;
		_index = index;
	}

	@Override
	public String getName() {
		if (_delete) {
			return "Delete section '" + _sectionName + "'.";
		}
		return "Add section '" + _sectionName + "'.";
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
		AssetPackModel pack = _editor.getModel();

		int[] index = { 0 };

		Display.getDefault().syncExec(() -> {
			if (_delete) {
				AssetSectionModel section = pack.findSection(_sectionName);
				index[0] = pack.getSections().indexOf(section);
				pack.removeSection(section, true);
			} else {
				pack.addSection(_index, new AssetSectionModel(_sectionName, pack), true);
			}
			_editor.refresh();
		});

		return new DeleteAssetSectionInEditorChange(_sectionName, _editor, !_delete, index[0]);
	}

	@Override
	public Object getModifiedElement() {
		return _editor;
	}

}
