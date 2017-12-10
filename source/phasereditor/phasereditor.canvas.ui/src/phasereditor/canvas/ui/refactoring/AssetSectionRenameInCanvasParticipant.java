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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.refactorings.RenameAssetArguments;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class AssetSectionRenameInCanvasParticipant extends RenameParticipant {

	private AssetSectionModel _section;
	private Set<IFile> _affectedFiles;

	@Override
	protected boolean initialize(Object element) {
		try {

			if (element instanceof AssetSectionModel) {
				_section = ((AssetSectionModel) element);

				String initialName = _section.getKey();

				List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache()
						.getProjectData(_section.getPack().getFile().getProject());

				_affectedFiles = new HashSet<>();

				for (CanvasFile cfile : cfiles) {
					JSONObject data = cfile.readData();
					CanvasCore.forEachJSONReference(data, ref -> {
						String name = ref.getString("section");
						if (initialName.equals(name)) {
							_affectedFiles.add(cfile.getFile());
						}
					});

				}

				PhaserEditorUI.forEachEditor(editor -> {
					if (editor instanceof CanvasEditor) {
						WorldModel world = ((CanvasEditor) editor).getModel().getWorld();
						CanvasCore.forEachAssetKeyInModelContent(world, (key, sprite) -> {
							if (key.getAsset().getSection().getKey().equals(initialName)) {
								_affectedFiles.add(sprite.getWorld().getFile());
							}
						});
					}
				});

				if (_affectedFiles.isEmpty()) {
					return false;
				}

				return true;
			}

			return false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return "Rename assets section '" + _section.getKey() + "' in canvas files.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		String key = _section.getKey();

		for (IFile file : _affectedFiles) {
			status.addWarning("The canvas '" + file.getName() + "' uses the asset pack section '" + key + "'.",
					new CanvasFileRefactoringStatusContext(file));
		}

		RenameAssetArguments args = (RenameAssetArguments) getArguments();

		if (args.isInDirtyEditor()) {
			status.addFatalError("The asset is open in a dirty editor. Save before to rename.");
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		IFile file = _section.getPack().getFile();
		String initialName = _section.getKey();
		String newName = getArguments().getNewName();

		return new RenameAssetSectionInCanvasChange(file, initialName, newName);
	}

}
