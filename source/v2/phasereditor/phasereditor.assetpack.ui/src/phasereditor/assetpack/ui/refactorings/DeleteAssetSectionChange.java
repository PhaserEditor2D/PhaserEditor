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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor2;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class DeleteAssetSectionChange extends Change {

	private IFile _file;
	private String _sectionName;
	private boolean _doDelete;

	public DeleteAssetSectionChange(IFile file, String sectionName) {
		this(file, sectionName, true);
	}

	private DeleteAssetSectionChange(IFile file, String sectionName, boolean delete) {
		super();
		_file = file;
		_sectionName = sectionName;
		_doDelete = delete;
	}

	@Override
	public String getName() {
		if (_doDelete) {
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
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		deleteInFile(pm);

		deleteInEditors();

		return new DeleteAssetSectionChange(_file, _sectionName, !_doDelete);
	}

	private void deleteInEditors() {
		Display.getDefault().asyncExec(() -> {
			List<IEditorPart> editors = PhaserEditorUI.findOpenFileEditors(_file);
			for (IEditorPart editor : editors) {
				if (editor instanceof AssetPackEditor2) {
					var packEditor = (AssetPackEditor2) editor;
					AssetPackModel pack = packEditor.getModel();
					if (_doDelete) {
						AssetSectionModel section = pack.findSection(_sectionName);
						pack.removeSection(section, false);
					} else {
						pack.addSection(new AssetSectionModel(_sectionName, pack), false);
					}
					packEditor.refresh();
				}
			}
		});
	}

	private void deleteInFile(IProgressMonitor pm) {
		try {
			AssetPackModel pack = new AssetPackModel(_file);

			if (_doDelete) {
				AssetSectionModel section = pack.findSection(_sectionName);
				pack.removeSection(section, false);
			} else {
				pack.addSection(new AssetSectionModel(_sectionName, pack), false);
			}

			pack.save(pm);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object getModifiedElement() {
		return _file;
	}

}
