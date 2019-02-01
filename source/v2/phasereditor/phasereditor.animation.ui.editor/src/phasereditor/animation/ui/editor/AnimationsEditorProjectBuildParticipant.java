package phasereditor.animation.ui.editor;

import org.eclipse.core.resources.IFile;

import phasereditor.project.ui.build.BaseEditorBuildParticipant;

public class AnimationsEditorProjectBuildParticipant extends BaseEditorBuildParticipant<AnimationsEditor> {

	public AnimationsEditorProjectBuildParticipant() {
		super(AnimationsEditor.ID);
	}

	@Override
	protected void buildEditor(AnimationsEditor editor) {
		editor.build();
	}

	@Override
	protected IFile getEditorFile(AnimationsEditor editor) {
		return editor.getEditorInput().getFile();
	}

	@Override
	protected void reloadEditorFile(AnimationsEditor editor) {
		editor.reloadFile();
	}

}
