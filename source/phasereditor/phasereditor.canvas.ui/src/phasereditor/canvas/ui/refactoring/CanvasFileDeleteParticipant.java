package phasereditor.canvas.ui.refactoring;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.CanvasUI;

public class CanvasFileDeleteParticipant extends DeleteParticipant {

	private IFile _file;
	private CanvasType _canvasType;

	public CanvasFileDeleteParticipant() {
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
		return "Delete Canvas";
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

		CompositeChange change = new CompositeChange("Delete " + _file.getName());

		for (IFile file : files) {
			change.add(new DeleteResourceChange(file.getFullPath(), true));
		}

		return change;
	}

}
