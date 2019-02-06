package phasereditor.scene.ui.editor.refactorings;

import org.eclipse.core.resources.IFile;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.refactorings.MoveEditorInput;

public class MoveSceneEditorInput extends MoveEditorInput<SceneEditor> {

	@Override
	protected String getEditorId() {
		return SceneEditor.ID;
	}

	@Override
	protected void handleFileMoved(SceneEditor editor, IFile newFile) {
		editor.handleFileMoved(newFile);
	}

	@Override
	protected IFile getEditorInputFile(SceneEditor editor) {
		return editor.getEditorInput().getFile();
	}

}
