package phasereditor.assetpack.ui.editor.build;

import org.eclipse.core.resources.IFile;

import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.project.ui.build.BaseEditorBuildParticipant;

public class AssetPackEditorProjectBuildParticipant extends BaseEditorBuildParticipant<AssetPackEditor> {

	public AssetPackEditorProjectBuildParticipant() {
		super(AssetPackEditor.ID);
	}

	@Override
	protected void buildEditor(AssetPackEditor editor) {
		editor.build();
	}

	@Override
	protected IFile getEditorFile(AssetPackEditor editor) {
		return editor.getEditorInput().getFile();
	}

	@Override
	protected void reloadEditorFile(AssetPackEditor editor) {
		editor.reloadFile();
	}


}
