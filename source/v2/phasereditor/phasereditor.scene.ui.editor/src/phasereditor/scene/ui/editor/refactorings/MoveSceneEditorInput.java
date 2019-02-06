package phasereditor.scene.ui.editor.refactorings;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

public class MoveSceneEditorInput extends MoveParticipant {

	private IFile _sceneFile;

	public MoveSceneEditorInput() {
	}

	@Override
	protected boolean initialize(Object element) {
		var file = (IFile) element;
		_sceneFile = file;
		return true;
	}

	@Override
	public String getName() {
		return "Update Scene file related stuff.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		var folder = (IContainer) getArguments().getDestination();
		
		var newScenePath = folder.getFullPath().append(_sceneFile.getName());

		return new UpdateSceneEditorInputChange(_sceneFile.getFullPath(), newScenePath);
	}

}
