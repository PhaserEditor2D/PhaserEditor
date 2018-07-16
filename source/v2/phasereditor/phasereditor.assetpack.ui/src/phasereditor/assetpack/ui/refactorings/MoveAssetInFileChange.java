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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.editors.AssetPackEditor2;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class MoveAssetInFileChange extends Change {
	private AssetMoveList _list;

	public MoveAssetInFileChange(AssetMoveList list) {
		_list = list;
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

		try {
			AssetPackModel pack = new AssetPackModel(_list.getPackFile());

			for (int i = 0; i < _list.size(); i++) {
				String sectionName = _list.getInitialSectionName(i);
				String assetName = _list.getAssetName(i);
				
				AssetSectionModel srcSection = pack.findSection(sectionName);
				AssetModel asset = srcSection.findAsset(assetName);
				srcSection.removeAsset(asset);
				
				AssetSectionModel dstSection = pack.findSection(_list.getDestinySectionName(i));
				dstSection.addAsset(asset, false);
			}

			pack.save(pm);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, AssetPackUI.PLUGIN_ID, e.getMessage(), e));
		}
		
		Display.getDefault().syncExec(() -> {
			List<IEditorPart> editors = PhaserEditorUI.findOpenFileEditors(_list.getPackFile());
			for (IEditorPart editor : editors) {
				if (editor instanceof AssetPackEditor2) {
					var packEditor = (AssetPackEditor2) editor;
					moveAsset(packEditor);
				}
			}
		});

		return new MoveAssetInFileChange(_list.reverse());
	}
	
	private void moveAsset(AssetPackEditor2 editor) {
		AssetPackModel pack = editor.getModel();
		
		for(int i = 0; i < _list.size(); i++) {
			AssetSectionModel srcSection = pack.findSection(_list.getInitialSectionName(i));
			AssetSectionModel dstSection = pack.findSection(_list.getDestinySectionName(i));
			AssetModel asset = srcSection.findAsset(_list.getAssetName(i));
			srcSection.removeAsset(asset, false);
			dstSection.addAsset(asset, false);
		}
		
		editor.refresh();
		editor.updateAssetEditor();
	}

	@Override
	public Object getModifiedElement() {
		return _list.getPackFile();
	}
}
