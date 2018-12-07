package phasereditor.assetpack.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.ui.editor.AssetPackUIEditor;

public class RenameAssetHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		AssetPackUIEditor.launchRenameWizard(HandlerUtil.getCurrentStructuredSelection(event).getFirstElement());
		
		return null;
	}

}
