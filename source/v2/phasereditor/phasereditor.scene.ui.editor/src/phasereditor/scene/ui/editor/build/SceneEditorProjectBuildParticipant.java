package phasereditor.scene.ui.editor.build;

import org.eclipse.core.resources.IFile;

import phasereditor.project.ui.build.BaseEditorBuildParticipant;
import phasereditor.scene.ui.editor.SceneEditor;

public class SceneEditorProjectBuildParticipant extends BaseEditorBuildParticipant<SceneEditor> {

	public SceneEditorProjectBuildParticipant() {
		super(SceneEditor.ID);
	}

	@Override
	protected void buildEditor(SceneEditor editor) {
		editor.build();
	}

	@Override
	protected IFile getEditorFile(SceneEditor editor) {
		return editor.getEditorInput().getFile();
	}

	@Override
	protected void reloadEditorFile(SceneEditor editor) {
		// TODO Auto-generated method stub
		
	}

}
