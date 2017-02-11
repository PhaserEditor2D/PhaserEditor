package phasereditor.canvas.ui.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasType;

public class CanvasFileMoveParticipant extends MoveParticipant {

	private CanvasType _canvasType;
	private IFile _file;

	public CanvasFileMoveParticipant() {
		// TODO Auto-generated constructor stub
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
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

}
