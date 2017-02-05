package phasereditor.canvas.ui.refactoring;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.CanvasUI;

public class CanvasFileDeleteParticipant extends DeleteParticipant {

	private IFile _file;

	public CanvasFileDeleteParticipant() {
	}

	@Override
	protected boolean initialize(Object element) {
		if (!(element instanceof IFile)) {
			return false;
		}

		IFile file = (IFile) element;

		if (!CanvasCore.isPrefabFile(file)) {
			return false;
		}

		_file = file;

		return true;
	}

	@Override
	public String getName() {
		return "Delete Canvas";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		List<PrefabReference> refs = CanvasUI.findPrefabReferences(new Prefab(_file));

		RefactoringStatus status = new RefactoringStatus();

		for (PrefabReference ref : refs) {
			status.addWarning("The canvas file '" + ref.getFile().getProjectRelativePath().toString()
					+ "' is using the prefab '" + _file.getName() + "'",
					new UsingPrefabRefactoringStatusContext(ref));
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// here we should add a delete-resource-change of the derived JS file.
		// return new DeleteResourceChange(_file.getFullPath(), false);
		return new NullChange();
	}

}
