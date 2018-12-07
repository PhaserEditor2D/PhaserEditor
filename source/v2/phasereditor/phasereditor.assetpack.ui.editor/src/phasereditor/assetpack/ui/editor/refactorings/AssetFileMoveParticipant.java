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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.ui.editor.AssetPackUIEditor;
import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class AssetFileMoveParticipant extends MoveParticipant {

	private IFile _file;
	private List<AssetModel> _result;

	@Override
	protected boolean initialize(Object element) {
		if (!(element instanceof IFile)) {
			return false;
		}

		IFile file = (IFile) element;

		if (!ProjectCore.isWebContentFile(file)) {
			return false;
		}

		_file = file;

		_result = AssetPackUIEditor.findAssetResourceReferences(file);

		return !_result.isEmpty();
	}

	@Override
	public String getName() {
		return "Move asset resource.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		for (AssetModel asset : _result) {
			String packname = asset.getPack().getFile().getName();
			status.addInfo("The asset pack entry '" + asset.getKey() + "' in '" + packname + "' will be updated.");
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		CompositeChange change = new CompositeChange("Update asset packs from rename.");

		List<AssetPackModel> packs = new ArrayList<>();

		IContainer dstFolder = (IContainer) getArguments().getDestination();
		IFile newFile = dstFolder.getFile(new Path(_file.getName()));

		for (AssetModel asset : _result) {
			packs.add(asset.getPack());
		}

		for (AssetPackModel pack : packs) {
			change.add(new AssetFileInPackChange(pack, _file, newFile));
		}

		return change;
	}

}
