package phasereditor.ui.refactorings;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

public abstract class MoveEditorInput<T extends EditorPart> extends MoveParticipant {

	private IFile _file;

	public MoveEditorInput() {
	}

	@Override
	protected boolean initialize(Object element) {
		var file = (IFile) element;
		_file = file;
		return true;
	}

	@Override
	public String getName() {
		return "Update editor input.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		var folder = (IContainer) getArguments().getDestination();

		var newScenePath = folder.getFullPath().append(_file.getName());

		return new UpdateEditorInputChange(_file.getFullPath(), newScenePath);
	}

	protected abstract String getEditorId();

	protected abstract void handleFileMoved(T editor, IFile newFile);

	protected abstract IFile getEditorInputFile(T editor);

	private class UpdateEditorInputChange extends Change {

		private IPath _oldFilePath;
		private IPath _newFilePath;

		public UpdateEditorInputChange(IPath oldFilePath, IPath newFilePath) {
			super();

			_oldFilePath = oldFilePath;
			_newFilePath = newFilePath;

		}

		@Override
		public String getName() {
			return "Rename Asset Pack editors.";
		}

		@Override
		public void initializeValidationData(IProgressMonitor pm) {
			//
		}

		@Override
		public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {

			var status = new RefactoringStatus();

			var file = getOldFile();

			if (!file.exists()) {
				status.addError("The file '" + _oldFilePath + "' does not exist.");
			}

			return status;
		}

		private IFile getOldFile() {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(_oldFilePath);
		}

		private IFile getNewFile() {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(_newFilePath);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Change perform(IProgressMonitor pm) throws CoreException {

			var newFile = getNewFile();

			for (var window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				for (var page : window.getPages()) {
					for (var ref : page.getEditorReferences()) {
						if (ref.getId().equals(getEditorId())) {
							var editor = ref.getEditor(false);
							if (editor != null) {
								var curFile = getEditorInputFile((T) editor);
								var curPath = curFile.getFullPath();

								if (curPath.equals(_oldFilePath)) {
									handleFileMoved((T) editor, newFile);
								}
							}
						}
					}
				}
			}

			return new UpdateEditorInputChange(_newFilePath, _oldFilePath);
		}

		@Override
		public Object getModifiedElement() {
			return _oldFilePath;
		}
	}

}
