// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scene.ui.editor.refactorings;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.PlatformUI;

import phasereditor.scene.ui.editor.SceneEditor;

/**
 * @author arian
 *
 */
public class RenameSceneEditorChange extends Change {

	private IPath _originalFilePath;
	private String _newName;

	public RenameSceneEditorChange(IPath originalFilePath, String newName) {
		super();
		_originalFilePath = originalFilePath;
		_newName = newName;
	}

	@Override
	public String getName() {
		return "Rename Scene editors.";
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
			status.addError("The file '" + _originalFilePath + "' does not exist.");
		}

		return status;
	}

	private IFile getOldFile() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(_originalFilePath);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {

		var oldFile = getOldFile();
		var newFile = oldFile.getParent().getFile(new Path(_newName));

		for (var window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (var page : window.getPages()) {
				for (var ref : page.getEditorReferences()) {
					if (ref.getId().equals(SceneEditor.ID)) {
						var editor = (SceneEditor) ref.getEditor(false);
						if (editor != null) {
							var curFile = editor.getEditorInput().getFile();
							if (curFile.getFullPath().equals(oldFile.getFullPath())) {
								editor.handleFileMoved(newFile);
							}
						}
					}
				}
			}
		}

		return new RenameSceneEditorChange(newFile.getFullPath(), oldFile.getName());
	}

	@Override
	public Object getModifiedElement() {
		return _originalFilePath;
	}

}
