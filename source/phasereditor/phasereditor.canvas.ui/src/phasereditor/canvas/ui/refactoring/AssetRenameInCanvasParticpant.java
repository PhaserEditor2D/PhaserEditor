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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.ui.refactorings.RenameAssetArguments;
import phasereditor.canvas.ui.CanvasUI;

/**
 * @author arian
 *
 */
public class AssetRenameInCanvasParticpant extends RenameParticipant {

	private AssetModel _asset;
	private FindAssetReferencesResult _refs;

	@Override
	protected boolean initialize(Object element) {

		if (element instanceof AssetModel) {
			_asset = ((AssetModel) element);

			_refs = CanvasUI.findAllAssetReferences(_asset, new NullProgressMonitor());

			if (_refs.getFiles().isEmpty()) {
				return false;
			}

			return true;
		}

		return false;
	}

	@Override
	public String getName() {
		return "Rename asset '" + _asset.getKey() + "' in canvas files.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		String key = _asset.getKey();

		for (IFile file : _refs.getFiles()) {
			status.addWarning("The canvas '" + file.getName() + "' uses the asset pack entry '" + key + "'.",
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

		IFile file = _asset.getPack().getFile();
		String sectionKey = _asset.getSection().getKey();
		String initialName = _asset.getKey();
		String newName = getArguments().getNewName();

		return new RenameAssetInCanvasChange(file, sectionKey, initialName, newName);
	}

}
