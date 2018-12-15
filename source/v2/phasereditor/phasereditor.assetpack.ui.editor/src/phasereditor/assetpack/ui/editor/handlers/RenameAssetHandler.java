package phasereditor.assetpack.ui.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.assetpack.ui.editor.AssetPackUIEditor;

public class RenameAssetHandler extends RefactoringHandler {

	@Override
	protected Object execute(ExecutionEvent event, AssetPackEditor editor) {
		AssetPackUIEditor.launchRenameWizard(HandlerUtil.getCurrentStructuredSelection(event).getFirstElement(),
				editor);

		return null;
	}

}
