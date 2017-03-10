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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class RenameAssetChange extends Change {

	private AssetModel _asset;
	private AssetSectionModel _section;
	private final String _oldName;
	private final String _newName;
	private final AssetPackModel _pack;
	private Object _element;

	public RenameAssetChange(Object element, String newName) {
		_element = element;
		if (element instanceof AssetModel) {
			_asset = (AssetModel) element;
			_oldName = _asset.getKey();
			_pack = _asset.getPack();
		} else {
			_section = (AssetSectionModel) element;
			_oldName = _section.getKey();
			_pack = _section.getPack();
		}
		_newName = newName;
	}

	@Override
	public String getName() {
		return "Rename Asset";
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
		if (_asset != null) {
			_asset.setKey(_newName);
		} else {
			_section.setKey(_newName);
		}

		_pack.save(pm);

		Display.getDefault().asyncExec(() -> {
			List<IEditorPart> editors = PhaserEditorUI.findOpenFileEditors(_pack.getFile());
			for (IEditorPart editor : editors) {
				if (editor instanceof AssetPackEditor) {
					AssetPackEditor packEditor = (AssetPackEditor) editor;

					if (_asset == null) {
						renameSection(packEditor);
					} else {
						renameAsset(packEditor);
					}
				}
			}
		});

		return new RenameAssetChange(_element, _oldName);
	}

	private void renameSection(AssetPackEditor editor) {
		AssetSectionModel section = editor.getModel().findSection(_oldName);
		if (section != null) {
			section.setKey(_newName, false);
			TreeViewer viewer = editor.getViewer();
			viewer.refresh();
			editor.updateAssetEditor();
		}
	}

	private void renameAsset(AssetPackEditor editor) {
		AssetSectionModel section = editor.getModel().findSection(_asset.getSection().getKey());
		if (section != null) {
			AssetModel asset = section.findAsset(_oldName);
			if (asset != null) {
				asset.setKey(_newName, false);
				TreeViewer viewer = editor.getViewer();
				viewer.refresh();
				editor.updateAssetEditor();
			}
		}
	}

	@Override
	public Object getModifiedElement() {
		return _element;
	}

}
