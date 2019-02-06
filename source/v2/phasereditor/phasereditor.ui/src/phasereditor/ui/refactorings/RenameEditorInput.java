package phasereditor.ui.refactorings;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

public abstract class RenameEditorInput<T extends EditorPart> extends RenameParticipant {

	public RenameEditorInput() {
	}

	private IFile _file;

	@Override
	protected boolean initialize(Object element) {

		_file = (IFile) element;

		return true;
	}

	@Override
	public String getName() {
		return "Rename Asset Pack editors.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		IPath oldPath = _file.getFullPath();
		IPath newPath = oldPath.removeLastSegments(1).append(getArguments().getNewName());

		return new UpdateEditorInputChange(oldPath, newPath);
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
			return "Rename editor input.";
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
