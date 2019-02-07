package phasereditor.animation.ui.editor.refactorings;

import org.eclipse.core.resources.IFile;

import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.ui.refactorings.RenameEditorInput;

public class RenameAnimationsEditorInput extends RenameEditorInput<AnimationsEditor> {

	@Override
	protected String getEditorId() {
		return AnimationsEditor.ID;
	}

	@Override
	protected void handleFileMoved(AnimationsEditor editor, IFile newFile) {
		editor.handleFileMoved(newFile);
	}

	@Override
	protected IFile getEditorInputFile(AnimationsEditor editor) {
		return editor.getEditorInput().getFile();
	}


}
