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
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.ui.refactorings.AssetMoveList;
import phasereditor.assetpack.ui.refactorings.MoveAssetsArguments;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;

/**
 * @author arian
 *
 */
public class AssetMoveInCanvasParticipant extends MoveParticipant {
	private Set<IFile> _usedFiles;
	private AssetMoveList _list;

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof AssetMoveList) {
			_list = (AssetMoveList) element;
			AssetPackModel pack = AssetPackCore.getAssetPackModel(_list.getPackFile(), false);

			List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(pack.getFile().getProject());

			_usedFiles = new HashSet<>();

			for (CanvasFile cfile : cfiles) {
				try {
					JSONObject data = cfile.readData();
					CanvasCore.forEachJSONReference(data, ref -> {
						for (int i = 0; i < _list.size(); i++) {
							String asset = _list.getAssetName(i);
							String section = _list.getInitialSectionName(i);
							String refSection = ref.getString("section");
							String refAsset = ref.getString("asset");

							if (refSection.equals(section) && refAsset.equals(asset)) {
								_usedFiles.add(cfile.getFile());
								return;
							}
						}
					});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return "Move assets";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		for (IFile file : _usedFiles) {
			status.addWarning("The moving assets are used by the Canvas file '" + file.getName() + "'.",
					new CanvasFileRefactoringStatusContext(file));
		}

		MoveAssetsArguments args = (MoveAssetsArguments) getArguments();

		if (args.isInDirtyEditor()) {
			status.addFatalError("The asset is open in a dirty editor. Save before to move.");
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		MoveAssetsInCanvasChange change = new MoveAssetsInCanvasChange(_list);
		return change;
	}

}
