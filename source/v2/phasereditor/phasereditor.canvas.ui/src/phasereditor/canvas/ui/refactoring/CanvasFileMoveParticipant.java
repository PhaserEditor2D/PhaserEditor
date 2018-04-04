package phasereditor.canvas.ui.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.resource.MoveResourceChange;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.CanvasUI.FindPrefabReferencesResult;

public class CanvasFileMoveParticipant extends MoveParticipant {

	private CanvasType _canvasType;
	private IFile _file;

	public CanvasFileMoveParticipant() {
	}

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
		return "Move Canvas";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		if (!_canvasType.isPrefab()) {
			return status;
		}

		FindPrefabReferencesResult result = CanvasUI.findAllPrefabReferences(new Prefab(_file, _canvasType), pm);

		for (IFile file : result.getFiles()) {
			List<PrefabReference> refs = result.getReferencesOf(file);

			String filepath = file.getProjectRelativePath().toString();

			status.addWarning("The canvas file '" + filepath + "' has " + refs.size() + " prefab '" + _file.getName()
					+ "' instances.", new CanvasFileRefactoringStatusContext(file));
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		Set<Object> elements = new HashSet<>(Arrays.asList(getProcessor().getElements()));

		List<IFile> files = CanvasCore.getCanvasDereivedFiles(_file);

		MoveArguments args = getArguments();

		IContainer dst = (IContainer) args.getDestination();

		CompositeChange changes = new CompositeChange("Move " + _file.getName());

		for (IFile file : files) {
			if (!elements.contains(file)) {
				changes.add(new MoveResourceChange(file, dst));
			}
		}

		if (_canvasType.isPrefab()) {
			IPath srcPath = _file.getProjectRelativePath();
			IPath dstPath = dst.getProjectRelativePath().append(_file.getName());

			changes.add(new UpdatePrefabReferencesChange(_file.getProject(), srcPath, dstPath));
		}

		return changes;
	}

}
