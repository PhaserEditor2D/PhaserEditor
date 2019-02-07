package phasereditor.atlas.ui.editor.refactorings;

import org.eclipse.core.resources.IFile;

import phasereditor.atlas.ui.editor.TexturePackerEditor;
import phasereditor.ui.refactorings.MoveEditorInput;

public class MoveTexturePackerEditorInput extends MoveEditorInput<TexturePackerEditor> {

	@Override
	protected String getEditorId() {
		return TexturePackerEditor.ID;
	}

	@Override
	protected void handleFileMoved(TexturePackerEditor editor, IFile newFile) {
		editor.handleFileMoved(newFile);
	}

	@Override
	protected IFile getEditorInputFile(TexturePackerEditor editor) {
		return editor.getEditorInputFile();
	}

}
