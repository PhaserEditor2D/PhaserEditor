package phasereditor.assetpack.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.ui.AssetPackUI;

public class RenameAssetHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		AssetPackUI.launchRenameWizard(HandlerUtil.getCurrentStructuredSelection(event).getFirstElement());
		
		return null;
	}

}
