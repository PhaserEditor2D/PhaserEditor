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
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class RenameAssetSectionInCanvasChange extends Change {

	private String _newName;
	private String _initialName;
	private IFile _file;

	public RenameAssetSectionInCanvasChange(IFile file, String initialName, String newName) {
		super();
		_file = file;
		_initialName = initialName;
		_newName = newName;
	}

	@Override
	public String getName() {
		return "Rename asset section '" + _initialName + "' in canvas files.";
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

		return new RenameAssetSectionInCanvasChange(_file, _newName, _initialName);
	}

	private void renameInFile(IFile file, IProgressMonitor monitor) throws Exception {
		JSONObject data;

		try (InputStream contents = file.getContents()) {
			data = JSONObject.read(contents);
		}

		CanvasCore.forEachJSONReference(data, ref -> {
			String name = ref.getString("section");
			if (_initialName.equals(name)) {
				ref.put("section", _newName);
			}
		});

		try (ByteArrayInputStream source = new ByteArrayInputStream(data.toString(2).getBytes())) {
			file.setContents(source, false, false, monitor);
		}
	}

	private void renameInModel(CanvasModel canvasModel) throws Exception {
		WorldModel world = canvasModel.getWorld();
		
		CanvasCore.forEachAssetKeyInModelContent(world, (assetKey, sprite) -> {
			AssetModel asset = assetKey.getAsset();
			AssetSectionModel section = asset.getSection();
			if (section.getKey().equals(_initialName)) {
				AssetSectionModel section2 = section.copy();
				section2.setKey(_newName);
				AssetModel asset2 = asset.copy(section2);

				if (sprite.getAssetKey() instanceof AssetModel) {
					sprite.setAssetKey(asset2);
				} else {
					String frameKey = sprite.getAssetKey().getKey();
					IAssetElementModel frame = asset2.getSubElements().stream()
							.filter(elem -> elem.getKey().equals(frameKey)).findFirst().get();
					sprite.setAssetKey(frame);
				}
			}
		});
	}

	@Override
	public Object getModifiedElement() {
		return _file;
	}

}
