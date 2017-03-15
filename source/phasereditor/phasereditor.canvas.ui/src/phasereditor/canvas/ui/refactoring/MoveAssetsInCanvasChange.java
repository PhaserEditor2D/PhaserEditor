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
import java.util.function.BiConsumer;

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
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.refactorings.AssetMoveList;
import phasereditor.canvas.core.AssetSpriteModel;
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
public class MoveAssetsInCanvasChange extends Change {

	private AssetMoveList _list;

	public MoveAssetsInCanvasChange(AssetMoveList list) {
		super();
		_list = list;
	}

	@Override
	public String getName() {
		return "Update moved assets in canvas files";
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

		IProject project = _list.getPackFile().getProject();
		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);

		// update files

		for (CanvasFile cfile : cfiles) {
			try {
				moveInFile(cfile.getFile(), pm);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// update editors

		PhaserEditorUI.forEachEditor(editor -> {
			if (editor instanceof CanvasEditor) {
				try {
					moveInModel(((CanvasEditor) editor).getModel());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		return new MoveAssetsInCanvasChange(_list.reverse());
	}

	private void moveInFile(IFile file, IProgressMonitor monitor) throws Exception {
		JSONObject data;

		try (InputStream contents = file.getContents()) {
			data = JSONObject.read(contents);
		}

		String packFile = _list.getPackFile().getProjectRelativePath().toPortableString();

		CanvasCore.forEachJSONReference(data, ref -> {
			for (int i = 0; i < _list.size(); i++) {
				String asset = _list.getAssetName(i);
				String section = _list.getInitialSectionName(i);
				String newSection = _list.getDestinySectionName(i);

				String refAsset = ref.getString("asset");
				String refSection = ref.getString("section");
				String refFile = ref.getString("file");

				if (refAsset.equals(asset) && refSection.equals(section) && refFile.equals(packFile)) {
					ref.put("section", newSection);
				}

			}
		});

		try (ByteArrayInputStream source = new ByteArrayInputStream(data.toString(2).getBytes())) {
			file.setContents(source, false, false, monitor);
		}
	}

	private void moveInModel(CanvasModel canvasModel) throws Exception {
		WorldModel world = canvasModel.getWorld();

		AssetPackModel pack = AssetPackCore.getAssetPackModel(_list.getPackFile());
		String packName = pack.getFile().getProjectRelativePath().toPortableString();

		for (int i = 0; i < _list.size(); i++) {
			AssetSectionModel section = pack.findSection(_list.getInitialSectionName(i));
			AssetModel asset = section.findAsset(_list.getAssetName(i));
			AssetSectionModel newSection = pack.findSection(_list.getDestinySectionName(i));

			CanvasCore.forEachAssetKeyInModelContent(world, new BiConsumer<IAssetKey, AssetSpriteModel<IAssetKey>>() {

				@Override
				public void accept(IAssetKey key, AssetSpriteModel<IAssetKey> sprite) {
					AssetModel spriteAsset = key.getAsset();
					String spritePackName = spriteAsset.getPack().getFile().getProjectRelativePath().toPortableString();
					String spriteSection = spriteAsset.getSection().getKey();
					String spriteAssetName = spriteAsset.getKey();
					//@formatter:off
					if (
							spritePackName.equals(packName)
							&& spriteSection.equals(section.getKey())
							&& spriteAssetName.equals(asset.getKey())) 
					{
					//@formatter:on	
						spriteAsset.getSection().removeAsset(spriteAsset);
						newSection.addAsset(asset, false);
					}

				}
			});
		}
	}

	@Override
	public Object getModifiedElement() {
		return _list;
	}

}
