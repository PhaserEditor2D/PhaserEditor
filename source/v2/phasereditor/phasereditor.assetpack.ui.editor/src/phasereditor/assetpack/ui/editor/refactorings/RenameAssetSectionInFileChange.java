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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class RenameAssetSectionInFileChange extends Change {

	private final String _initialName;
	private final String _newName;
	private final IFile _file;

	public RenameAssetSectionInFileChange(IFile file, String initialName, String newName) {
		_file = file;
		_initialName = initialName;
		_newName = newName;
	}

	@Override
	public String getName() {
		return "Rename asset section '" + _initialName + "'";
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
		try {
			AssetPackModel pack = new AssetPackModel(_file);
			AssetSectionModel section = pack.findSection(_initialName);
			section.setKey(_newName, false);
			pack.save(pm);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, AssetPackCore.PLUGIN_ID, e.getMessage(), e));
		}

		Display.getDefault().syncExec(() -> {
			List<IEditorPart> editors = PhaserEditorUI.findOpenFileEditors(_file);
			for (IEditorPart editor : editors) {
				if (editor instanceof AssetPackEditor) {
					var packEditor = (AssetPackEditor) editor;
					renameSection(packEditor);
				}
			}
		});

		return new RenameAssetSectionInFileChange(_file, _newName, _initialName);
	}

	private void renameSection(AssetPackEditor editor) {
		AssetSectionModel section = editor.getModel().findSection(_initialName);
		if (section != null) {
			section.setKey(_newName, false);
			editor.refresh();
			editor.updateAssetEditor();
		}
	}

	@Override
	public Object getModifiedElement() {
		return _file;
	}

}
