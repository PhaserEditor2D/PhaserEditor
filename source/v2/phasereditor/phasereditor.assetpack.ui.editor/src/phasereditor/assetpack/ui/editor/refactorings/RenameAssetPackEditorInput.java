package phasereditor.assetpack.ui.editor.refactorings;

import org.eclipse.core.resources.IFile;

import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.ui.refactorings.RenameEditorInput;

public class RenameAssetPackEditorInput extends RenameEditorInput<AssetPackEditor>{

	@Override
	protected String getEditorId() {
		return AssetPackEditor.ID;
	}

	@Override
	protected void handleFileMoved(AssetPackEditor editor, IFile newFile) {
		editor.handleFileMoved(newFile);
	}

	@Override
	protected IFile getEditorInputFile(AssetPackEditor editor) {
		return editor.getEditorInput().getFile();
	}


	
}
