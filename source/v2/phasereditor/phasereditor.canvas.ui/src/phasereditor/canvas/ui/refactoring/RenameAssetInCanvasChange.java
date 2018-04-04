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
package phasereditor.canvas.ui.refactoring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.AssetInCanvasReference;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class RenameAssetInCanvasChange extends Change {

	private String _newName;
	private String _initialName;
	private IFile _file;
	private String _sectionKey;

	public RenameAssetInCanvasChange(IFile file, String sectionKey, String initialName, String newName) {
		super();
		_file = file;
		_initialName = initialName;
		_sectionKey = sectionKey;
		_newName = newName;
	}

	@Override
	public String getName() {
		return "Rename asset entry '" + _initialName + "' in canvas files";
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

		IProject project = _file.getProject();
		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);

		// update files

		for (CanvasFile cfile : cfiles) {
			try {
				renameInFile(cfile.getFile(), pm);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// update editors

		PhaserEditorUI.forEachEditor(editor -> {
			if (editor instanceof CanvasEditor) {
				try {
					renameInModel(((CanvasEditor) editor).getModel());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		return new RenameAssetInCanvasChange(_file, _sectionKey, _newName, _initialName);
	}

	private void renameInFile(IFile file, IProgressMonitor monitor) throws Exception {
		JSONObject data;

		try (InputStream contents = file.getContents()) {
			data = JSONObject.read(contents);
		}

		CanvasCore.forEachJSONReference(data, ref -> {
			String name = ref.getString("asset");
			if (_initialName.equals(name)) {
				ref.put("asset", _newName);
			}
		});

		try (ByteArrayInputStream source = new ByteArrayInputStream(data.toString(2).getBytes())) {
			file.setContents(source, false, false, monitor);
		}
	}

	private void renameInModel(CanvasModel canvasModel) throws Exception {
		WorldModel world = canvasModel.getWorld();

		AssetPackModel pack = AssetPackCore.getAssetPackModel(_file);
		AssetSectionModel section = pack.findSection(_sectionKey);
		AssetModel asset = section.findAsset(_initialName);

		List<IAssetReference> refs = CanvasCore.findAssetReferenceInModelContent(asset, world);
		for (IAssetReference ref : refs) {
			AssetModel asset2 = ref.getAssetKey().getAsset().copy(section);

			AssetSpriteModel<IAssetKey> model = ((AssetInCanvasReference) ref).getModel();
			if (model.getAssetKey() instanceof AssetModel) {
				model.setAssetKey(asset2);
			} else {
				String frameKey = model.getAssetKey().getKey();
				IAssetElementModel frame = asset2.getSubElements().stream()
						.filter(elem -> elem.getKey().equals(frameKey)).findFirst().get();
				model.setAssetKey(frame);
			}

			// we set the name at the end because image frames gets the name
			// directly from the image asset.
			asset2.setKey(_newName);
		}
	}

	@Override
	public Object getModifiedElement() {
		return _file;
	}

}
