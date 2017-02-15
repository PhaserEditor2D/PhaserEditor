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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.CanvasUI;

/**
 * @author arian
 *
 */
public class CanvasFileRenameParticipant extends RenameParticipant {

	private IFile _file;
	private CanvasType _canvasType;

	@Override
	protected boolean initialize(Object element) {
		if (!(element instanceof IFile)) {
			return false;
		}

		IFile file = (IFile) element;

		if (!CanvasCore.isCanvasFile(file)) {
			return false;
		}

		_file = file;

		_canvasType = CanvasCore.getCanvasType(file);

		return true;
	}

	@Override
	public String getName() {
		return "Rename Canvas";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		if (!_canvasType.isPrefab()) {
			return status;
		}

		Map<IFile, List<PrefabReference>> refMap = CanvasUI.findPrefabReferences(new Prefab(_file, _canvasType));

		for (Entry<IFile, List<PrefabReference>> entry : refMap.entrySet()) {
			IFile file = entry.getKey();
			List<PrefabReference> refs = entry.getValue();

			String filepath = file.getProjectRelativePath().toString();

			status.addWarning("The canvas file '" + filepath + "' has " + refs.size() + " prefab '" + _file.getName()
					+ "' instances.", new CanvasFileRefactoringStatusContext(file));
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		List<IFile> files = CanvasCore.getCanvasDereivedFiles(_file);

		RenameArguments args = getArguments();

		CompositeChange changes = new CompositeChange("Rename " + _file.getName());

		String newName = args.getNewName();

		for (IFile file : files) {
			IPath path = file.getFullPath();
			String ext = path.getFileExtension();
			String derivedNewName = new Path(newName).removeFileExtension().addFileExtension(ext).toPortableString();
			changes.add(new RenameResourceChange(path, derivedNewName));
		}

		if (_canvasType.isPrefab()) {
			IPath srcPath = _file.getProjectRelativePath();
			IPath dstPath = srcPath.removeLastSegments(1).append(newName);

			changes.add(new UpdatePrefabReferencesChange(_file.getProject(), srcPath, dstPath));
		}

		return changes;
	}

}
